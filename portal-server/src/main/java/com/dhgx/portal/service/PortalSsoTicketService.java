package com.dhgx.portal.service;

import com.dhgx.common.portal.dto.PortalSsoTicketLoginResponse;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.common.SsoTicketUtils;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.entity.PortalUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Portal SSO ticket 业务服务。
 */
@Service
public class PortalSsoTicketService {
    private static final Logger log = LoggerFactory.getLogger(PortalSsoTicketService.class);
    private static final DateTimeFormatter LOGIN_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter EXPIRE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern TICKET_ALLOWED_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");
    private static final String GSESSION_PREFIX = "PORTAL:GSESSION:";
    private static final String LOGOUT_TOKEN_PREFIX = "PORTAL:LOGOUT_TOKEN:";
    private static final long DEFAULT_GSESSION_TTL_SECONDS = 8 * 60 * 60;

    private static final RedisScript<List> VERIFY_SCRIPT = buildVerifyScript();

    private final StringRedisTemplate stringRedisTemplate;
    private final PortalUserService portalUserService;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    public PortalSsoTicketService(StringRedisTemplate stringRedisTemplate,
                                  PortalUserService portalUserService,
                                  AuthClient authClient,
                                  ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.portalUserService = portalUserService;
        this.authClient = authClient;
        this.objectMapper = objectMapper;
    }

    public VerifyResult verifyAndConsume(String systemCode, String ticket, String redirectUri) {
        return verifyAndConsume(systemCode, ticket, redirectUri, null);
    }

    public VerifyResult verifyAndConsume(String systemCode, String ticket, String redirectUri, String state) {
        if (!StringUtils.hasText(systemCode) || !StringUtils.hasText(ticket) || !StringUtils.hasText(redirectUri)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_INVALID);
        }
        if (!isValidTicket(ticket)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_INVALID);
        }
        if (isRateLimited(systemCode)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_RATE_LIMITED);
        }
        String redirectUriHash = SsoTicketUtils.hashRedirectUri(redirectUri);
        if (!StringUtils.hasText(redirectUriHash)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_INVALID);
        }
        String stateHash = StringUtils.hasText(state) ? SsoTicketUtils.hashValue(state) : "";
        List result = stringRedisTemplate.execute(VERIFY_SCRIPT,
                Collections.singletonList(SsoTicketUtils.buildTicketKey(ticket)),
                systemCode,
                redirectUriHash,
                stateHash);
        if (result == null || result.size() < 2) {
            log.warn("SSO ticket verify script returned empty. ticket={}", SsoTicketUtils.maskTicket(ticket));
            return VerifyResult.failure(ErrorCode.SSO_TICKET_SYSTEM_ERROR);
        }
        String successFlag = String.valueOf(result.get(0));
        String code = result.size() > 1 ? String.valueOf(result.get(1)) : "";
        if (!"1".equals(successFlag)) {
            return VerifyResult.failure(mapErrorCode(code));
        }
        String payloadJson = result.size() > 2 ? String.valueOf(result.get(2)) : "";
        Map<String, Object> payload = parsePayload(payloadJson);
        String userId = payload == null ? null : String.valueOf(payload.get("userId"));
        String tokenValue = payload == null ? null : String.valueOf(payload.get("tokenValue"));
        if (!StringUtils.hasText(userId) || "null".equalsIgnoreCase(userId)
                || !StringUtils.hasText(tokenValue) || "null".equalsIgnoreCase(tokenValue)) {
            log.warn("SSO ticket payload missing userId. ticket={}", SsoTicketUtils.maskTicket(ticket));
            return VerifyResult.failure(ErrorCode.SSO_TICKET_SYSTEM_ERROR);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            log.warn("SSO ticket user not found. userId={}, ticket={}", userId, SsoTicketUtils.maskTicket(ticket));
            return VerifyResult.failure(ErrorCode.SSO_TICKET_SYSTEM_ERROR);
        }
        String gSessionId = buildGSessionId();
        String logoutToken = buildLogoutToken();
        long ttlSeconds = resolveGlobalSessionTtlSeconds();
        writeGlobalSessionMapping(gSessionId, tokenValue, systemCode, logoutToken, ttlSeconds);
        String expireAt = LocalDateTime.now().plusSeconds(ttlSeconds).format(EXPIRE_TIME_FORMATTER);

        PortalSsoTicketLoginResponse response = new PortalSsoTicketLoginResponse(
                user.getId(),
                user.getUsername(),
                user.getMobile(),
                user.getRealName(),
                LOGIN_TIME_FORMATTER.format(Instant.now()),
                gSessionId,
                logoutToken,
                expireAt);
        return VerifyResult.success(response);
    }

    public VerifyResult logoutByGlobalSession(String systemCode, String gSessionId, String logoutToken) {
        if (!StringUtils.hasText(systemCode) || !StringUtils.hasText(gSessionId) || !StringUtils.hasText(logoutToken)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_INVALID);
        }
        String mappingKey = buildGSessionKey(gSessionId);
        String mappingValue = stringRedisTemplate.opsForValue().get(mappingKey);
        if (!StringUtils.hasText(mappingValue)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_INVALID);
        }
        String[] values = mappingValue.split("\\|", 2);
        if (values.length < 2) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_SYSTEM_ERROR);
        }
        String tokenValue = values[0];
        String expectedSystemCode = values[1];
        if (!systemCode.equals(expectedSystemCode)) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_CLIENT_MISMATCH);
        }
        String expectedHash = stringRedisTemplate.opsForValue().get(buildLogoutTokenKey(gSessionId));
        if (!StringUtils.hasText(expectedHash) || !expectedHash.equals(hashToken(logoutToken))) {
            return VerifyResult.failure(ErrorCode.SSO_TICKET_INVALID);
        }
        ApiResponse<Void> logoutResponse = authClient.logoutByTokenValue(tokenValue);
        if (logoutResponse == null || logoutResponse.getCode() != 0) {
            log.warn("SSO global logout failed at auth-server. gSessionId={}, systemCode={}, message={}",
                    gSessionId, systemCode, logoutResponse == null ? null : logoutResponse.getMessage());
            return VerifyResult.failure(ErrorCode.SSO_TICKET_SYSTEM_ERROR);
        }
        stringRedisTemplate.delete(buildGSessionKey(gSessionId));
        stringRedisTemplate.delete(buildLogoutTokenKey(gSessionId));
        return VerifyResult.logoutSuccess();
    }

    private boolean isValidTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return false;
        }
        String trimmed = ticket.trim();
        return TICKET_ALLOWED_PATTERN.matcher(trimmed).matches()
                || isHex(trimmed);
    }

    private boolean isHex(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.length() < 16 || trimmed.length() > 128) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!Character.isDigit(ch)
                    && (Character.toLowerCase(ch) < 'a' || Character.toLowerCase(ch) > 'f')) {
                return false;
            }
        }
        return true;
    }

    private boolean isRateLimited(String systemCode) {
        return false;
    }

    private String buildGSessionId() {
        return "G-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildLogoutToken() {
        return "L-" + UUID.randomUUID().toString().replace("-", "");
    }

    private void writeGlobalSessionMapping(String gSessionId,
                                           String tokenValue,
                                           String systemCode,
                                           String logoutToken,
                                           long ttlSeconds) {
        String mappingValue = tokenValue + "|" + systemCode;
        stringRedisTemplate.opsForValue().set(buildGSessionKey(gSessionId), mappingValue, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(buildLogoutTokenKey(gSessionId), hashToken(logoutToken), ttlSeconds, TimeUnit.SECONDS);
    }

    private long resolveGlobalSessionTtlSeconds() {
        return DEFAULT_GSESSION_TTL_SECONDS;
    }

    private String buildGSessionKey(String gSessionId) {
        return GSESSION_PREFIX + gSessionId;
    }

    private String buildLogoutTokenKey(String gSessionId) {
        return LOGOUT_TOKEN_PREFIX + gSessionId;
    }

    private String hashToken(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("hash logout token failed", ex);
        }
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return null;
        }
    }

    private ErrorCode mapErrorCode(String code) {
        if (!StringUtils.hasText(code)) {
            return ErrorCode.SSO_TICKET_INVALID;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "CLIENT_MISMATCH":
                return ErrorCode.SSO_TICKET_CLIENT_MISMATCH;
            case "REDIRECT_URI_MISMATCH":
                return ErrorCode.SSO_TICKET_REDIRECT_URI_MISMATCH;
            case "STATE_MISMATCH":
                return ErrorCode.SSO_TICKET_STATE_MISMATCH;
            case "RATE_LIMITED":
                return ErrorCode.SSO_TICKET_RATE_LIMITED;
            case "SYSTEM_ERROR":
                return ErrorCode.SSO_TICKET_SYSTEM_ERROR;
            case "INVALID_TICKET":
            default:
                return ErrorCode.SSO_TICKET_INVALID;
        }
    }

    private static RedisScript<List> buildVerifyScript() {
        String script = ""
                + "local key = KEYS[1]\n"
                + "local expectedSystemCode = ARGV[1]\n"
                + "local expectedRedirectHash = ARGV[2]\n"
                + "local expectedStateHash = ARGV[3]\n"
                + "if redis.call('EXISTS', key) == 0 then\n"
                + "  return {0,'INVALID_TICKET',''}\n"
                + "end\n"
                + "local systemCode = redis.call('HGET', key, 'systemCode')\n"
                + "if systemCode ~= expectedSystemCode then\n"
                + "  redis.call('DEL', key)\n"
                + "  return {0,'CLIENT_MISMATCH',''}\n"
                + "end\n"
                + "local redirectHash = redis.call('HGET', key, 'redirectUriHash')\n"
                + "if redirectHash ~= expectedRedirectHash then\n"
                + "  redis.call('DEL', key)\n"
                + "  return {0,'REDIRECT_URI_MISMATCH',''}\n"
                + "end\n"
                + "local stateHash = redis.call('HGET', key, 'stateHash')\n"
                + "if stateHash and stateHash ~= '' and expectedStateHash ~= '' and stateHash ~= expectedStateHash then\n"
                + "  return {0,'STATE_MISMATCH',''}\n"
                + "end\n"
                + "local userId = redis.call('HGET', key, 'userId')\n"
                + "local issuedAt = redis.call('HGET', key, 'issuedAt')\n"
                + "local tokenValue = redis.call('HGET', key, 'tokenValue')\n"
                + "redis.call('DEL', key)\n"
                + "return {1,'OK', cjson.encode({userId=userId, systemCode=systemCode, issuedAt=issuedAt, tokenValue=tokenValue})}\n";
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(List.class);
        redisScript.setScriptText(script);
        return redisScript;
    }

    public static class VerifyResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final PortalSsoTicketLoginResponse loginResponse;

        private VerifyResult(boolean success, ErrorCode errorCode, PortalSsoTicketLoginResponse loginResponse) {
            this.success = success;
            this.errorCode = errorCode;
            this.loginResponse = loginResponse;
        }

        public static VerifyResult success(PortalSsoTicketLoginResponse response) {
            return new VerifyResult(true, null, response);
        }

        public static VerifyResult logoutSuccess() {
            return new VerifyResult(true, null, null);
        }

        public static VerifyResult failure(ErrorCode errorCode) {
            return new VerifyResult(false, errorCode, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public PortalSsoTicketLoginResponse getLoginResponse() {
            return loginResponse;
        }
    }
}
