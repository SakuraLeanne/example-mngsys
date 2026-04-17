package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.common.feign.dto.AuthLoginResponse;
import com.dhgx.common.portal.dto.PortalMiniProgramBindRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramLoginRequest;
import com.dhgx.common.portal.dto.PortalMobileLoginResponse;
import com.dhgx.common.redis.RedisKeys;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.entity.PortalUserIdentity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PortalMobileAuthService。
 * <p>
 * 小程序登录编排服务：处理 openId 登录、手机号绑定、移动 token 签发/刷新/登出。
 * </p>
 */
@Service
public class PortalMobileAuthService {

    private static final Logger log = LoggerFactory.getLogger(PortalMobileAuthService.class);
    private static final long ACCESS_TOKEN_TTL_SECONDS = 2 * 60 * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 30L * 24 * 60 * 60;
    private static final long BIND_TOKEN_TTL_SECONDS = 5 * 60;
    private static final String CLIENT_TYPE_MINI_PROGRAM = "MINI_PROGRAM";

    private final PortalUserIdentityService portalUserIdentityService;
    private final PortalUserService portalUserService;
    private final AuthClient authClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public PortalMobileAuthService(PortalUserIdentityService portalUserIdentityService,
                                   PortalUserService portalUserService,
                                   AuthClient authClient,
                                   StringRedisTemplate stringRedisTemplate,
                                   ObjectMapper objectMapper) {
        this.portalUserIdentityService = portalUserIdentityService;
        this.portalUserService = portalUserService;
        this.authClient = authClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 小程序 openId 登录。已绑定直接登录，未绑定返回 bindToken。
     */
    public ApiResponse<PortalMobileLoginResponse> loginByMiniProgram(PortalMiniProgramLoginRequest request) {
        String openId = request.getOpenId().trim();
        PortalUserIdentity identity = portalUserIdentityService.findMiniProgramOpenId(openId);
        if (identity == null) {
            String bindToken = createBindToken(openId);
            log.info("mini program login requires binding, openId={}", mask(openId));
            return ApiResponse.success(PortalMobileLoginResponse.bindRequired(bindToken));
        }
        return doLogin(identity.getUserId(), "mini-program-openid");
    }

    /**
     * 绑定手机号并登录。
     */
    public ApiResponse<PortalMobileLoginResponse> bindMobileAndLogin(PortalMiniProgramBindRequest request) {
        String bindKey = RedisKeys.miniBindToken(request.getBindToken());
        String openId = stringRedisTemplate.opsForValue().get(bindKey);
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "绑定令牌无效或已过期");
        }

        ApiResponse<Void> smsVerifyResp = authClient.verifySms(request.getMobile(), request.getCode());
        if (smsVerifyResp == null || smsVerifyResp.getCode() != 0) {
            String msg = smsVerifyResp == null ? "短信验证码校验失败" : smsVerifyResp.getMessage();
            log.warn("mini program bind failed: sms verify failed, mobile={}, msg={}", mask(request.getMobile()), msg);
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, msg);
        }

        PortalUser user = portalUserService.getOne(new LambdaQueryWrapper<PortalUser>()
                .eq(PortalUser::getMobile, request.getMobile())
                .last("LIMIT 1"));
        if (user == null) {
            return ApiResponse.failure(ErrorCode.NOT_FOUND, "手机号未开通门户账号，请联系管理员");
        }

        PortalUserIdentity exists = portalUserIdentityService.findMiniProgramOpenId(openId);
        if (exists != null && !user.getId().equals(exists.getUserId())) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "该微信身份已绑定其他账号");
        }
        if (exists == null) {
            PortalUserIdentity identity = new PortalUserIdentity();
            identity.setUserId(user.getId());
            identity.setIdentityProvider("WECHAT_MINI_PROGRAM");
            identity.setIdentityType("OPENID");
            identity.setIdentityKey(openId);
            identity.setBindStatus(1);
            identity.setBindTime(LocalDateTime.now());
            portalUserIdentityService.save(identity);
            log.info("mini program identity bind success, userId={}, openId={}", user.getId(), mask(openId));
        }

        stringRedisTemplate.delete(bindKey);
        return doLogin(user.getId(), "mini-program-bind");
    }

    public ApiResponse<PortalMobileLoginResponse> refresh(String refreshToken) {
        String refreshKey = RedisKeys.mobileRefreshToken(refreshToken);
        String refreshPayload = stringRedisTemplate.opsForValue().get(refreshKey);
        if (!StringUtils.hasText(refreshPayload)) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, "refresh_token 无效或已过期");
        }
        try {
            RefreshTokenPayload payload = objectMapper.readValue(refreshPayload, RefreshTokenPayload.class);
            // 刷新时重新签发 token，并删除旧 refresh_token，避免重放。
            stringRedisTemplate.delete(refreshKey);
            return doLogin(payload.getUserId(), "mini-program-refresh");
        } catch (JsonProcessingException ex) {
            log.error("refresh token payload parse error", ex);
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "令牌解析失败");
        }
    }

    public ApiResponse<Void> logout(String accessToken) {
        TokenPayload payload = readAccessPayload(accessToken);
        if (payload != null) {
            stringRedisTemplate.delete(RedisKeys.mobileRefreshToken(payload.getRefreshToken()));
        }
        stringRedisTemplate.delete(RedisKeys.mobileAccessToken(accessToken));
        return ApiResponse.success(null);
    }

    public ApiResponse<TokenPayload> introspect(String accessToken) {
        TokenPayload payload = readAccessPayload(accessToken);
        if (payload == null) {
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, "access_token 无效或已过期");
        }
        ResponseEntity<ApiResponse<com.dhgx.common.feign.dto.AuthSessionResponse>> sessionResp = authClient.sessionMe("satoken=" + payload.getSaToken());
        ApiResponse<?> body = sessionResp == null ? null : sessionResp.getBody();
        if (body == null || body.getCode() != 0) {
            log.warn("introspect failed because satoken invalid, userId={}", payload.getUserId());
            stringRedisTemplate.delete(RedisKeys.mobileAccessToken(accessToken));
            stringRedisTemplate.delete(RedisKeys.mobileRefreshToken(payload.getRefreshToken()));
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, "登录态已失效");
        }
        return ApiResponse.success(payload);
    }

    private ApiResponse<PortalMobileLoginResponse> doLogin(String userId, String source) {
        ApiResponse<AuthLoginResponse> authResp = authClient.internalLoginByUserId(userId);
        if (authResp == null || authResp.getCode() != 0 || authResp.getData() == null) {
            String msg = authResp == null ? "鉴权服务无响应" : authResp.getMessage();
            log.warn("mini program {} login failed, userId={}, msg={}", source, userId, msg);
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, msg);
        }
        AuthLoginResponse loginResponse = authResp.getData();
        String accessToken = randomToken();
        String refreshToken = randomToken();
        TokenPayload accessPayload = new TokenPayload();
        accessPayload.setUserId(userId);
        accessPayload.setClientType(CLIENT_TYPE_MINI_PROGRAM);
        accessPayload.setSaToken(loginResponse.getSatoken());
        accessPayload.setRefreshToken(refreshToken);
        accessPayload.setExpireAt(Instant.now().plusSeconds(ACCESS_TOKEN_TTL_SECONDS).toEpochMilli());

        RefreshTokenPayload refreshPayload = new RefreshTokenPayload();
        refreshPayload.setUserId(userId);
        refreshPayload.setClientType(CLIENT_TYPE_MINI_PROGRAM);

        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.mobileAccessToken(accessToken),
                    objectMapper.writeValueAsString(accessPayload), ACCESS_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(RedisKeys.mobileRefreshToken(refreshToken),
                    objectMapper.writeValueAsString(refreshPayload), REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("mini program login success, userId={}, source={}", userId, source);
            return ApiResponse.success(PortalMobileLoginResponse.loginSuccess(
                    accessToken, refreshToken, ACCESS_TOKEN_TTL_SECONDS, userId));
        } catch (JsonProcessingException ex) {
            log.error("write mobile token payload failed, userId={}", userId, ex);
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "令牌签发失败");
        }
    }

    private TokenPayload readAccessPayload(String accessToken) {
        String raw = stringRedisTemplate.opsForValue().get(RedisKeys.mobileAccessToken(accessToken));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<TokenPayload>() {
            });
        } catch (JsonProcessingException ex) {
            log.warn("access token payload parse failed, token={}", mask(accessToken));
            return null;
        }
    }

    private String createBindToken(String openId) {
        String bindToken = randomToken();
        stringRedisTemplate.opsForValue().set(RedisKeys.miniBindToken(bindToken), openId, BIND_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        return bindToken;
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    /**
     * access_token 持久化内容。
     */
    public static class TokenPayload {
        private String userId;
        private String clientType;
        private String saToken;
        private String refreshToken;
        private Long expireAt;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getClientType() {
            return clientType;
        }

        public void setClientType(String clientType) {
            this.clientType = clientType;
        }

        public String getSaToken() {
            return saToken;
        }

        public void setSaToken(String saToken) {
            this.saToken = saToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public Long getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(Long expireAt) {
            this.expireAt = expireAt;
        }
    }

    private static class RefreshTokenPayload {
        private String userId;
        private String clientType;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getClientType() {
            return clientType;
        }

        public void setClientType(String clientType) {
            this.clientType = clientType;
        }
    }
}
