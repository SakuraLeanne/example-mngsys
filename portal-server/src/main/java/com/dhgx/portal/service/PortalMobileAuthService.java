package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.common.feign.dto.AuthLoginResponse;
import com.dhgx.common.portal.dto.PortalAppWechatLoginRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramBindRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramLoginRequest;
import com.dhgx.common.portal.dto.PortalMiniProgramPhoneLoginRequest;
import com.dhgx.common.portal.dto.PortalMobileClientType;
import com.dhgx.common.portal.dto.PortalMobileLoginResponse;
import com.dhgx.common.portal.dto.PortalMobileSmsLoginRequest;
import com.dhgx.common.portal.dto.PortalMobileSmsSendRequest;
import com.dhgx.common.redis.RedisKeys;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.client.wechat.WechatAccessTokenResponse;
import com.dhgx.portal.client.wechat.WechatAppClient;
import com.dhgx.portal.client.wechat.WechatAppOauthResponse;
import com.dhgx.portal.client.wechat.WechatMiniProgramClient;
import com.dhgx.portal.client.wechat.WechatMiniProgramSessionResponse;
import com.dhgx.portal.client.wechat.WechatPhoneNumberResponse;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.config.AuthClientProperties;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.entity.PortalUserIdentity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PortalMobileAuthService。
 * <p>
 * 移动端登录编排服务：处理 APP/小程序微信登录、手机号绑定、移动 token 签发/刷新/登出。
 * </p>
 */
@Service
public class PortalMobileAuthService {

    private static final Logger log = LoggerFactory.getLogger(PortalMobileAuthService.class);
    private static final long ACCESS_TOKEN_TTL_SECONDS = 2 * 60 * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 30L * 24 * 60 * 60;
    private static final long BIND_TOKEN_TTL_SECONDS = 5 * 60;
    private static final long WECHAT_ACCESS_TOKEN_SKEW_SECONDS = 120;

    private static final String CLIENT_TYPE_MINI_PROGRAM = "MINI_PROGRAM";
    private static final String CLIENT_TYPE_APP = "APP";
    private static final String PROVIDER_WECHAT_MINI_PROGRAM = "WECHAT_MINI_PROGRAM";
    private static final String PROVIDER_WECHAT_APP = "WECHAT_APP";
    private static final String IDENTITY_TYPE_OPENID = "OPENID";
    private static final String IDENTITY_TYPE_UNIONID = "UNIONID";

    private final PortalUserIdentityService portalUserIdentityService;
    private final PortalUserService portalUserService;
    private final AuthClient authClient;
    private final WechatMiniProgramClient wechatMiniProgramClient;
    private final WechatAppClient wechatAppClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthClientProperties authClientProperties;
    private final PasswordEncoder passwordEncoder;

    public PortalMobileAuthService(PortalUserIdentityService portalUserIdentityService,
                                   PortalUserService portalUserService,
                                   AuthClient authClient,
                                   WechatMiniProgramClient wechatMiniProgramClient,
                                   WechatAppClient wechatAppClient,
                                   StringRedisTemplate stringRedisTemplate,
                                   ObjectMapper objectMapper,
                                   AuthClientProperties authClientProperties,
                                   PasswordEncoder passwordEncoder) {
        this.portalUserIdentityService = portalUserIdentityService;
        this.portalUserService = portalUserService;
        this.authClient = authClient;
        this.wechatMiniProgramClient = wechatMiniProgramClient;
        this.wechatAppClient = wechatAppClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.authClientProperties = authClientProperties;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 移动端发送短信验证码（APP/小程序共用）。
     */
    public ApiResponse<String> sendLoginSms(PortalMobileSmsSendRequest request) {
        String clientType = resolveClientType(request.getClientType());
        ApiResponse<String> resp = authClient.sendLoginSms(request.getMobile());
        if (resp == null) {
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "鉴权服务无响应");
        }
        if (resp.getCode() == 0) {
            log.info("mobile sms send success, clientType={}, mobile={}", clientType, mask(request.getMobile()));
        } else {
            log.warn("mobile sms send failed, clientType={}, mobile={}, msg={}", clientType, mask(request.getMobile()), resp.getMessage());
        }
        return resp;
    }

    /**
     * 移动端短信验证码登录（APP/小程序共用）。
     */
    public ApiResponse<PortalMobileLoginResponse> loginBySms(PortalMobileSmsLoginRequest request) {
        String clientType = resolveClientType(request.getClientType());
        ApiResponse<Void> smsVerifyResp = authClient.verifySms(request.getMobile(), request.getCode());
        if (smsVerifyResp == null || smsVerifyResp.getCode() != 0) {
            String msg = smsVerifyResp == null ? "短信验证码校验失败" : smsVerifyResp.getMessage();
            log.warn("mobile sms login failed: verify failed, clientType={}, mobile={}, msg={}",
                    clientType, mask(request.getMobile()), msg);
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, msg);
        }
        return loginByMobile(request.getMobile(), clientType, request.getDeviceId(), "sms-login");
    }

    /**
     * 小程序手机号授权登录：消费 bindgetphonenumber 的动态 code 换取手机号并登录。
     */
    public ApiResponse<PortalMobileLoginResponse> loginByMiniProgramPhoneCode(PortalMiniProgramPhoneLoginRequest request) {
        String accessToken;
        try {
            accessToken = resolveMiniProgramAccessToken();
        } catch (RuntimeException ex) {
            log.warn("mini program phone login failed: get access token error, msg={}", ex.getMessage());
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "获取微信访问令牌失败");
        }
        WechatPhoneNumberResponse phoneResponse;
        try {
            phoneResponse = wechatMiniProgramClient.getPhoneNumber(accessToken, request.getPhoneCode().trim());
        } catch (RuntimeException ex) {
            log.warn("mini program phone login failed: get phone number error, msg={}", ex.getMessage());
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "微信手机号获取失败，请重新点击授权");
        }
        if (phoneResponse.getErrcode() != null && phoneResponse.getErrcode() != 0) {
            log.warn("mini program phone login failed: wechat error, errcode={}, errmsg={}",
                    phoneResponse.getErrcode(), phoneResponse.getErrmsg());
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "微信手机号获取失败：" + phoneResponse.getErrmsg());
        }
        WechatPhoneNumberResponse.PhoneInfo phoneInfo = phoneResponse.getPhoneInfo();
        if (phoneInfo == null || !StringUtils.hasText(phoneInfo.getPhoneNumber())) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "微信手机号信息为空");
        }
        String configuredAppId = wechatMiniProgramClient.resolveConfiguredAppId();
        String responseAppId = phoneInfo.getWatermark() == null ? null : phoneInfo.getWatermark().getAppid();
        if (!StringUtils.hasText(configuredAppId) || !configuredAppId.equals(responseAppId)) {
            log.warn("mini program phone login failed: appId mismatch, expected={}, actual={}",
                    configuredAppId, responseAppId);
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "小程序应用标识校验失败");
        }
        return loginByMobile(phoneInfo.getPhoneNumber(), CLIENT_TYPE_MINI_PROGRAM, request.getDeviceId(), "mini-phone-code");
    }

    private ApiResponse<PortalMobileLoginResponse> loginByMobile(String mobile,
                                                                 String clientType,
                                                                 String deviceId,
                                                                 String source) {
        PortalUser user = portalUserService.getOne(new LambdaQueryWrapper<PortalUser>()
                .eq(PortalUser::getMobile, mobile)
                .last("LIMIT 1"));
        if (user == null) {
            if (!authClientProperties.isAutoCreateUser()) {
                return ApiResponse.failure(ErrorCode.NOT_FOUND, "手机号未开通门户账号，请联系管理员");
            }
            user = autoCreatePortalUser(mobile);
            log.info("auto created portal user for mobile login, clientType={}, userId={}, mobile={}",
                    clientType, user.getId(), mask(user.getMobile()));
        }
        return doLogin(user.getId(), clientType, deviceId, source);
    }

    /**
     * APP 微信授权登录：APP 传 code，门户后端换取 openId/unionId。
     */
    public ApiResponse<PortalMobileLoginResponse> loginByWechatApp(PortalAppWechatLoginRequest request) {
        WechatAppOauthResponse oauthResponse = wechatAppClient.code2Oauth(request.getCode().trim());
        String openId = oauthResponse.getOpenid();
        String unionId = oauthResponse.getUnionid();

        // APP 场景优先按 unionId 归一，找不到再按 openId。
        IdentityMatchResult matchResult = findIdentityWithConflictCheck(PROVIDER_WECHAT_APP, unionId, openId);
        if (matchResult.hasConflict()) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "微信身份冲突，请联系管理员处理");
        }
        PortalUserIdentity identity = matchResult.getMatchedIdentity();
        if (identity == null) {
            String bindToken = createBindToken(new BindContext(PROVIDER_WECHAT_APP, openId, unionId));
            log.info("app wechat login requires binding, openId={}, unionId={}", mask(openId), mask(unionId));
            return ApiResponse.success(PortalMobileLoginResponse.bindRequired(bindToken));
        }
        return doLogin(identity.getUserId(), CLIENT_TYPE_APP, request.getDeviceId(), "app-wechat");
    }

    /**
     * 小程序登录：小程序端传 code，门户后端调用 jscode2session 换 openId。
     */
    public ApiResponse<PortalMobileLoginResponse> loginByMiniProgram(PortalMiniProgramLoginRequest request) {
        WechatMiniProgramSessionResponse sessionResponse = wechatMiniProgramClient.code2Session(request.getCode().trim());
        String openId = sessionResponse.getOpenid();
        IdentityMatchResult matchResult = findIdentityWithConflictCheck(PROVIDER_WECHAT_MINI_PROGRAM, sessionResponse.getUnionid(), openId);
        if (matchResult.hasConflict()) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "微信身份冲突，请联系管理员处理");
        }
        PortalUserIdentity identity = matchResult.getMatchedIdentity();
        if (identity == null) {
            String bindToken = createBindToken(new BindContext(PROVIDER_WECHAT_MINI_PROGRAM, openId, sessionResponse.getUnionid()));
            log.info("mini program login requires binding, openId={}", mask(openId));
            return ApiResponse.success(PortalMobileLoginResponse.bindRequired(bindToken));
        }
        return doLogin(identity.getUserId(), CLIENT_TYPE_MINI_PROGRAM, request.getDeviceId(), "mini-program-openid");
    }

    /**
     * 绑定手机号并登录。
     */
    public ApiResponse<PortalMobileLoginResponse> bindMobileAndLogin(PortalMiniProgramBindRequest request) {
        BindContext bindContext = readBindContextAndConsume(request.getBindToken());
        if (bindContext == null || !StringUtils.hasText(bindContext.getOpenId())
                || !StringUtils.hasText(bindContext.getProvider())) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "绑定令牌无效或已过期");
        }

        ApiResponse<Void> smsVerifyResp = authClient.verifySms(request.getMobile(), request.getCode());
        if (smsVerifyResp == null || smsVerifyResp.getCode() != 0) {
            String msg = smsVerifyResp == null ? "短信验证码校验失败" : smsVerifyResp.getMessage();
            log.warn("wechat bind failed: sms verify failed, mobile={}, msg={}", mask(request.getMobile()), msg);
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, msg);
        }

        PortalUser user = portalUserService.getOne(new LambdaQueryWrapper<PortalUser>()
                .eq(PortalUser::getMobile, request.getMobile())
                .last("LIMIT 1"));
        if (user == null) {
            // 按 auth.auto-create-user 配置决定是否自动创建门户账号。
            if (!authClientProperties.isAutoCreateUser()) {
                return ApiResponse.failure(ErrorCode.NOT_FOUND, "手机号未开通门户账号，请联系管理员");
            }
            user = autoCreatePortalUser(request.getMobile());
            log.info("auto created portal user for wechat bind, userId={}, mobile={}", user.getId(), mask(user.getMobile()));
        }

        PortalUserIdentity existsOpenId = portalUserIdentityService.findByIdentity(
                bindContext.getProvider(), IDENTITY_TYPE_OPENID, bindContext.getOpenId());
        if (existsOpenId != null && !user.getId().equals(existsOpenId.getUserId())) {
            return ApiResponse.failure(ErrorCode.INVALID_ARGUMENT, "该微信身份已绑定其他账号");
        }

        saveIdentityIfAbsent(user.getId(), bindContext.getProvider(), IDENTITY_TYPE_OPENID, bindContext.getOpenId());
        saveIdentityIfAbsent(user.getId(), bindContext.getProvider(), IDENTITY_TYPE_UNIONID, bindContext.getUnionId());

        String clientType = PROVIDER_WECHAT_APP.equals(bindContext.getProvider()) ? CLIENT_TYPE_APP : CLIENT_TYPE_MINI_PROGRAM;
        return doLogin(user.getId(), clientType, request.getDeviceId(), "wechat-bind");
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
            return doLogin(payload.getUserId(), payload.getClientType(), payload.getDeviceId(), "mobile-refresh");
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


    private String resolveClientType(PortalMobileClientType clientType) {
        if (clientType == null) {
            throw new IllegalArgumentException("clientType 不能为空");
        }
        switch (clientType) {
            case APP:
                return CLIENT_TYPE_APP;
            case MINI_PROGRAM:
                return CLIENT_TYPE_MINI_PROGRAM;
            default:
                throw new IllegalArgumentException("不支持的客户端类型");
        }
    }

    /**
     * 根据手机号自动创建门户用户（当 auth.auto-create-user=true 时生效）。
     */
    private PortalUser autoCreatePortalUser(String mobile) {
        PortalUser portalUser = new PortalUser();
        portalUser.setUsername(mobile);
        portalUser.setMobile(mobile);
        portalUser.setMobileVerified(1);
        portalUser.setEmailVerified(0);
        portalUser.setStatus(1);
        // 仅作为占位密码，不会对外暴露。
        portalUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        portalUserService.save(portalUser);
        return portalUser;
    }

    private ApiResponse<PortalMobileLoginResponse> doLogin(String userId, String clientType, String deviceId, String source) {
        ApiResponse<AuthLoginResponse> authResp = authClient.internalLoginByUserId(userId);
        if (authResp == null || authResp.getCode() != 0 || authResp.getData() == null) {
            String msg = authResp == null ? "鉴权服务无响应" : authResp.getMessage();
            log.warn("{} login failed, userId={}, msg={}", source, userId, msg);
            return ApiResponse.failure(ErrorCode.UNAUTHENTICATED, msg);
        }
        AuthLoginResponse loginResponse = authResp.getData();
        String accessToken = randomToken();
        String refreshToken = randomToken();
        TokenPayload accessPayload = new TokenPayload();
        accessPayload.setUserId(userId);
        accessPayload.setClientType(clientType);
        accessPayload.setSaToken(loginResponse.getSatoken());
        accessPayload.setRefreshToken(refreshToken);
        accessPayload.setDeviceId(deviceId);
        accessPayload.setExpireAt(Instant.now().plusSeconds(ACCESS_TOKEN_TTL_SECONDS).toEpochMilli());

        RefreshTokenPayload refreshPayload = new RefreshTokenPayload();
        refreshPayload.setUserId(userId);
        refreshPayload.setClientType(clientType);
        refreshPayload.setDeviceId(deviceId);

        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.mobileAccessToken(accessToken),
                    objectMapper.writeValueAsString(accessPayload), ACCESS_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(RedisKeys.mobileRefreshToken(refreshToken),
                    objectMapper.writeValueAsString(refreshPayload), REFRESH_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("mobile login success, userId={}, source={}, clientType={}", userId, source, clientType);
            return ApiResponse.success(PortalMobileLoginResponse.loginSuccess(
                    accessToken, refreshToken, ACCESS_TOKEN_TTL_SECONDS, userId));
        } catch (JsonProcessingException ex) {
            log.error("write mobile token payload failed, userId={}", userId, ex);
            return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, "令牌签发失败");
        }
    }

    /**
     * 微信身份匹配规则：unionId 优先，openid 兜底；若两者命中不同用户视为冲突。
     */
    private IdentityMatchResult findIdentityWithConflictCheck(String provider, String unionId, String openId) {
        PortalUserIdentity byUnion = portalUserIdentityService.findByIdentity(provider, IDENTITY_TYPE_UNIONID, unionId);
        PortalUserIdentity byOpen = portalUserIdentityService.findByIdentity(provider, IDENTITY_TYPE_OPENID, openId);
        if (byUnion != null && byOpen != null && !byUnion.getUserId().equals(byOpen.getUserId())) {
            log.warn("wechat identity conflict detected, provider={}, unionId={}, openId={}", provider, mask(unionId), mask(openId));
            return IdentityMatchResult.conflict();
        }
        if (byUnion != null) {
            return IdentityMatchResult.matched(byUnion);
        }
        return IdentityMatchResult.matched(byOpen);
    }

    private String resolveAppIdByProvider(String provider) {
        if (PROVIDER_WECHAT_APP.equals(provider)) {
            return wechatAppClient.resolveConfiguredAppId();
        }
        if (PROVIDER_WECHAT_MINI_PROGRAM.equals(provider)) {
            return wechatMiniProgramClient.resolveConfiguredAppId();
        }
        return null;
    }

    private void saveIdentityIfAbsent(String userId, String provider, String identityType, String identityKey) {
        if (!StringUtils.hasText(identityKey)) {
            return;
        }
        PortalUserIdentity exists = portalUserIdentityService.findByIdentity(provider, identityType, identityKey);
        if (exists != null) {
            return;
        }
        PortalUserIdentity identity = new PortalUserIdentity();
        identity.setUserId(userId);
        identity.setIdentityProvider(provider);
        identity.setIdentityType(identityType);
        identity.setIdentityKey(identityKey);
        identity.setAppId(resolveAppIdByProvider(provider));
        identity.setBindStatus(1);
        identity.setBindTime(LocalDateTime.now());
        portalUserIdentityService.save(identity);
    }

    private String resolveMiniProgramAccessToken() {
        String appId = wechatMiniProgramClient.resolveConfiguredAppId();
        if (!StringUtils.hasText(appId)) {
            throw new IllegalStateException("微信小程序 appId 未配置");
        }
        String cacheKey = RedisKeys.wechatMiniAccessToken(appId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        WechatAccessTokenResponse accessTokenResponse = wechatMiniProgramClient.getAccessToken();
        if (accessTokenResponse == null || !StringUtils.hasText(accessTokenResponse.getAccessToken())) {
            throw new IllegalStateException("微信 access_token 为空");
        }
        long expiresIn = accessTokenResponse.getExpiresIn() == null ? 7200L : accessTokenResponse.getExpiresIn();
        long ttl = Math.max(60L, expiresIn - WECHAT_ACCESS_TOKEN_SKEW_SECONDS);
        stringRedisTemplate.opsForValue().set(cacheKey, accessTokenResponse.getAccessToken(), ttl, TimeUnit.SECONDS);
        return accessTokenResponse.getAccessToken();
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

    /**
     * 读取并立即消费 bindToken，防止重放。
     */
    private BindContext readBindContextAndConsume(String bindToken) {
        String raw = stringRedisTemplate.opsForValue().getAndDelete(RedisKeys.miniBindToken(bindToken));
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, BindContext.class);
        } catch (JsonProcessingException ex) {
            log.warn("bind token payload parse failed, bindToken={}", mask(bindToken));
            return null;
        }
    }

    private String createBindToken(BindContext bindContext) {
        String bindToken = randomToken();
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.miniBindToken(bindToken),
                    objectMapper.writeValueAsString(bindContext),
                    BIND_TOKEN_TTL_SECONDS,
                    TimeUnit.SECONDS);
            return bindToken;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生成绑定令牌失败", ex);
        }
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
        private String deviceId;
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

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
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
        private String deviceId;

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

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }
    }


    private static class IdentityMatchResult {
        private final PortalUserIdentity matchedIdentity;
        private final boolean conflict;

        private IdentityMatchResult(PortalUserIdentity matchedIdentity, boolean conflict) {
            this.matchedIdentity = matchedIdentity;
            this.conflict = conflict;
        }

        static IdentityMatchResult matched(PortalUserIdentity identity) {
            return new IdentityMatchResult(identity, false);
        }

        static IdentityMatchResult conflict() {
            return new IdentityMatchResult(null, true);
        }

        public PortalUserIdentity getMatchedIdentity() {
            return matchedIdentity;
        }

        public boolean hasConflict() {
            return conflict;
        }
    }

    private static class BindContext {
        private String provider;
        private String openId;
        private String unionId;

        public BindContext() {
        }

        public BindContext(String provider, String openId, String unionId) {
            this.provider = provider;
            this.openId = openId;
            this.unionId = unionId;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getUnionId() {
            return unionId;
        }

        public void setUnionId(String unionId) {
            this.unionId = unionId;
        }
    }
}
