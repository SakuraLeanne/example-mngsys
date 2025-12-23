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

    public PortalPasswordService(PortalUserService portalUserService,
                                 PortalUserAuthStateService portalUserAuthStateService,
                                 UserAuthCacheService userAuthCacheService,
                                 StringRedisTemplate stringRedisTemplate,
                                 ObjectMapper objectMapper,
                                 AuthClient authClient,
                                 EventPublisher eventPublisher) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.authClient = authClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ChangeResult changePassword(String oldPassword, String newPassword, String ptk) {
        if (!StringUtils.hasText(oldPassword)) {
            throw new IllegalArgumentException("oldPassword 不能为空");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new IllegalArgumentException("newPassword 长度至少 8 位");
        }
        Long userId = resolveUserId(ptk);
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
        LocalDateTime now = LocalDateTime.now();
        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setLastPwdChangeTime(now);
        state.setUpdateTime(now);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, status, nextAuthVersion, state.getProfileVersion());
        authClient.kick(userId);
        deletePtk(ptk);
        publishPasswordChanged(userId, nextAuthVersion);
        return ChangeResult.success(userId, nextAuthVersion);
    }

    private Long resolveUserId(String ptk) {
        Long ptkUserId = loadUserIdFromPtk(ptk);
        if (ptkUserId != null) {
            return ptkUserId;
        }
        return RequestContext.getUserId();
    }

    private Long loadUserIdFromPtk(String ptk) {
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
            return parseLong(data.get("userId"));
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private PortalUserAuthState loadOrInitAuthState(Long userId) {
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

    private void publishPasswordChanged(Long userId, Long authVersion) {
        EventMessage message = new EventMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setEventType(EVENT_TYPE_PASSWORD_CHANGED);
        message.setUserId(userId);
        message.setAuthVersion(authVersion);
        message.setTs(System.currentTimeMillis());
        eventPublisher.publish(message);
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static class ChangeResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final Long userId;
        private final Long authVersion;

        private ChangeResult(boolean success, ErrorCode errorCode, Long userId, Long authVersion) {
            this.success = success;
            this.errorCode = errorCode;
            this.userId = userId;
            this.authVersion = authVersion;
        }

        public static ChangeResult success(Long userId, Long authVersion) {
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

        public Long getUserId() {
            return userId;
        }

        public Long getAuthVersion() {
            return authVersion;
        }
    }
}
