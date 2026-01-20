package com.dhgx.portal.service;

import com.dhgx.api.notify.core.EventNotifyPublisher;
import com.dhgx.common.security.PasswordCryptoService;
import com.dhgx.common.security.PasswordPolicyValidator;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.common.context.RequestContext;
import com.dhgx.portal.entity.PortalUserAuthState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * PortalPasswordService。
 * <p>
 * 处理门户用户密码修改逻辑，包含参数校验、密码复杂度校验、鉴权状态更新以及事件发布。
 * </p>
 */
@Service
public class PortalPasswordService {

    /** Portal Token 的缓存前缀。 */
    private static final String PTK_PREFIX = "portal:ptk:";
    /** 密码变更事件类型。 */
    private static final String EVENT_TYPE_PASSWORD_CHANGED = "portal:events:USER_PASSWORD_CHANGED";

    /** 用户服务。 */
    private final PortalUserService portalUserService;
    /** 鉴权状态服务。 */
    private final PortalUserAuthStateService portalUserAuthStateService;
    /** 鉴权缓存服务。 */
    private final UserAuthCacheService userAuthCacheService;
    /** Redis 模板。 */
    private final StringRedisTemplate stringRedisTemplate;
    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;
    /** 认证中心客户端。 */
    private final AuthClient authClient;
    /** 事件发布器。 */
    private final EventNotifyPublisher eventNotifyPublisher;
    /** 密码编码器。 */
    private final PasswordEncoder passwordEncoder;
    /** 密码解密服务。 */
    private final PasswordCryptoService passwordCryptoService;

    /**
     * 构造函数，注入依赖。
     */
    public PortalPasswordService(PortalUserService portalUserService,
                                 PortalUserAuthStateService portalUserAuthStateService,
                                 UserAuthCacheService userAuthCacheService,
                                 StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper,
                                 AuthClient authClient,
                                 EventNotifyPublisher eventNotifyPublisher,
                                 PasswordEncoder passwordEncoder,
                                 PasswordCryptoService passwordCryptoService) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.authClient = authClient;
        this.eventNotifyPublisher = eventNotifyPublisher;
        this.passwordEncoder = passwordEncoder;
        this.passwordCryptoService = passwordCryptoService;
    }

    /**
     * 修改密码流程。
     *
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @param ptk         Portal Token，用于校验操作合法性
     * @return 操作结果
     */
    @Transactional
    public ChangeResult changePassword(String encryptedOldPassword,
                                       String oldPassword,
                                       String encryptedNewPassword,
                                       String newPassword,
                                       String ptk) {
        String resolvedOldPassword = passwordCryptoService.decrypt(encryptedOldPassword, oldPassword);
        if (!StringUtils.hasText(resolvedOldPassword)) {
            return ChangeResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        String resolvedNewPassword = passwordCryptoService.decrypt(encryptedNewPassword, newPassword);
        if (!StringUtils.hasText(resolvedNewPassword)) {
            return ChangeResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        if (!PasswordPolicyValidator.isValid(resolvedNewPassword)) {
            return ChangeResult.failure(ErrorCode.NEW_PASSWORD_POLICY_VIOLATION);
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
        if (!matchesPassword(resolvedOldPassword, user.getPassword())) {
            return ChangeResult.failure(ErrorCode.OLD_PASSWORD_INCORRECT);
        }
        String encodedPassword = passwordEncoder.encode(resolvedNewPassword);
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

    /**
     * 对比原始密码与加密串。
     */
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

    /**
     * 根据 PTK 或上下文解析用户 ID。
     */
    private String resolveUserId(String ptk) {
        String ptkUserId = loadUserIdFromPtk(ptk);
        if (ptkUserId != null) {
            return ptkUserId;
        }
        return RequestContext.getUserId();
    }

    /**
     * 从 PTK 缓存中获取用户 ID。
     */
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

    /**
     * 获取或初始化鉴权状态。
     */
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

    /**
     * 生成下一个版本号。
     */
    private Long nextVersion(Long current) {
        long base = current == null ? 1L : current;
        return base + 1;
    }

    /**
     * 删除 PTK 缓存。
     */
    private void deletePtk(String ptk) {
        if (!StringUtils.hasText(ptk)) {
            return;
        }
        stringRedisTemplate.delete(PTK_PREFIX + ptk);
    }

    /**
     * 发布密码变更事件。
     */
    private void publishPasswordChanged(String userId, Long authVersion) {
        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("authVersion", authVersion.toString());
        message.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        eventNotifyPublisher.publish(EVENT_TYPE_PASSWORD_CHANGED, message);
    }

    /**
     * 安全解析字符串。
     */
    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? text : null;
    }

    /**
     * 密码修改结果。
     */
    public static class ChangeResult {
        /** 是否成功。 */
        private final boolean success;
        /** 错误码。 */
        private final ErrorCode errorCode;
        /** 用户 ID。 */
        private final String userId;
        /** 新的鉴权版本。 */
        private final Long authVersion;

        private ChangeResult(boolean success, ErrorCode errorCode, String userId, Long authVersion) {
            this.success = success;
            this.errorCode = errorCode;
            this.userId = userId;
            this.authVersion = authVersion;
        }

        /** 构建成功结果。 */
        public static ChangeResult success(String userId, Long authVersion) {
            return new ChangeResult(true, null, userId, authVersion);
        }

        /** 构建失败结果。 */
        public static ChangeResult failure(ErrorCode errorCode) {
            return new ChangeResult(false, errorCode, null, null);
        }

        /** 是否成功。 */
        public boolean isSuccess() {
            return success;
        }

        /** 获取错误码。 */
        public ErrorCode getErrorCode() {
            return errorCode;
        }

        /** 获取用户 ID。 */
        public String getUserId() {
            return userId;
        }

        /** 获取鉴权版本号。 */
        public Long getAuthVersion() {
            return authVersion;
        }
    }
}
