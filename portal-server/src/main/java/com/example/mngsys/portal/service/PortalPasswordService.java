package com.example.mngsys.portal.service;

import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.entity.PortalUserAuthState;
import com.example.mngsys.redisevent.EventMessage;
import com.example.mngsys.redisevent.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
/**
 * PortalPasswordService。
 */
public class PortalPasswordService {

    private static final String PTK_PREFIX = "portal:ptk:";
    private static final String EVENT_TYPE_PASSWORD_CHANGED = "USER_PASSWORD_CHANGED";

    private final PortalUserService portalUserService;
    private final PortalUserAuthStateService portalUserAuthStateService;
    private final UserAuthCacheService userAuthCacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthClient authClient;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public PortalPasswordService(PortalUserService portalUserService,
                                 PortalUserAuthStateService portalUserAuthStateService,
                                 UserAuthCacheService userAuthCacheService,
                                 StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper,
                                 AuthClient authClient,
                                 EventPublisher eventPublisher,
                                 PasswordEncoder passwordEncoder) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.authClient = authClient;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ChangeResult changePassword(String oldPassword, String newPassword, String ptk) {
        if (!StringUtils.hasText(oldPassword)) {
            return ChangeResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8 || !isComplexEnough(newPassword)) {
            return ChangeResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        String userId = resolveUserId(ptk);
        if (userId == null) {
            return ChangeResult.failure(ErrorCode.UNAUTHENTICATED);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ChangeResult.failure(ErrorCode.NOT_FOUND);
        }
        Integer status = user.getStatus();
        if (status == null || status != 1) {
            return ChangeResult.failure(ErrorCode.USER_DISABLED);
        }
        if (!matchesPassword(oldPassword, user.getPassword())) {
            return ChangeResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        if (matchesPassword(newPassword, user.getPassword())) {
            return ChangeResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        portalUserService.updateById(user);
        LocalDateTime now = LocalDateTime.now();
        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setLastPwdChangeTime(now);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, status, nextAuthVersion, state.getProfileVersion());
        authClient.kick(userId);
        deletePtk(ptk);
        publishPasswordChanged(userId, nextAuthVersion);
        return ChangeResult.success(userId, nextAuthVersion);
    }

    private boolean isComplexEnough(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPassword(String rawPassword, String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return false;
        }
        if (encoded.startsWith("{")) {
            return passwordEncoder.matches(rawPassword, encoded);
        }
        BCryptPasswordEncoder fallback = new BCryptPasswordEncoder();
        if (fallback.matches(rawPassword, encoded)) {
            return true;
        }
        return passwordEncoder.matches(rawPassword, encoded);
    }

    private String resolveUserId(String ptk) {
        String ptkUserId = loadUserIdFromPtk(ptk);
        if (ptkUserId != null) {
            return ptkUserId;
        }
        return RequestContext.getUserId();
    }

    private String loadUserIdFromPtk(String ptk) {
        if (!StringUtils.hasText(ptk)) {
            return null;
        }
        String payload = stringRedisTemplate.opsForValue().get(PTK_PREFIX + ptk);
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            return parseString(data.get("userId"));
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private PortalUserAuthState loadOrInitAuthState(String userId) {
        PortalUserAuthState state = portalUserAuthStateService.getById(userId);
        if (state == null) {
            state = new PortalUserAuthState();
            state.setUserId(userId);
            state.setAuthVersion(1L);
            state.setProfileVersion(1L);
        }
        return state;
    }

    private Long nextVersion(Long current) {
        long base = current == null ? 1L : current;
        return base + 1;
    }

    private void deletePtk(String ptk) {
        if (!StringUtils.hasText(ptk)) {
            return;
        }
        stringRedisTemplate.delete(PTK_PREFIX + ptk);
    }

    private void publishPasswordChanged(String userId, Long authVersion) {
        EventMessage message = new EventMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setEventType(EVENT_TYPE_PASSWORD_CHANGED);
        message.setUserId(userId);
        message.setAuthVersion(authVersion);
        message.setTs(System.currentTimeMillis());
        eventPublisher.publish(message);
    }

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? text : null;
    }

    public static class ChangeResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String userId;
        private final Long authVersion;

        private ChangeResult(boolean success, ErrorCode errorCode, String userId, Long authVersion) {
            this.success = success;
            this.errorCode = errorCode;
            this.userId = userId;
            this.authVersion = authVersion;
        }

        public static ChangeResult success(String userId, Long authVersion) {
            return new ChangeResult(true, null, userId, authVersion);
        }

        public static ChangeResult failure(ErrorCode errorCode) {
            return new ChangeResult(false, errorCode, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getUserId() {
            return userId;
        }

        public Long getAuthVersion() {
            return authVersion;
        }
    }
}
