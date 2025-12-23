package com.example.mngsys.portal.service;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
/**
 * PortalProfileService。
 */
public class PortalProfileService {

    private static final String PTK_PREFIX = "portal:ptk:";
    private static final String EVENT_TYPE_PROFILE_UPDATED = "USER_PROFILE_UPDATED";

    private final PortalUserService portalUserService;
    private final PortalUserAuthStateService portalUserAuthStateService;
    private final UserAuthCacheService userAuthCacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    public PortalProfileService(PortalUserService portalUserService,
                                PortalUserAuthStateService portalUserAuthStateService,
                                UserAuthCacheService userAuthCacheService,
                                StringRedisTemplate stringRedisTemplate,
                                ObjectMapper objectMapper,
                                EventPublisher eventPublisher) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    public ProfileResult getProfile(String ptk) {
        Long userId = resolveUserId(ptk);
        if (userId == null) {
            return ProfileResult.failure(ErrorCode.UNAUTHENTICATED);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ProfileResult.failure(ErrorCode.NOT_FOUND);
        }
        Integer status = user.getStatus();
        if (status == null || status != 1) {
            return ProfileResult.failure(ErrorCode.USER_DISABLED);
        }
        return ProfileResult.success(user.getRealName(), user.getMobile(), user.getEmail());
    }

    @Transactional
    public UpdateResult updateProfile(String ptk, String realName, String mobile, String email) {
        Long userId = resolveUserId(ptk);
        if (userId == null) {
            return UpdateResult.failure(ErrorCode.UNAUTHENTICATED);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return UpdateResult.failure(ErrorCode.NOT_FOUND);
        }
        Integer status = user.getStatus();
        if (status == null || status != 1) {
            return UpdateResult.failure(ErrorCode.USER_DISABLED);
        }
        Map<String, Object> changedFields = resolveChangedFields(user, realName, mobile, email);
        user.setRealName(realName);
        user.setMobile(mobile);
        user.setEmail(email);
        user.setUpdateTime(LocalDateTime.now());
        portalUserService.updateById(user);

        LocalDateTime now = LocalDateTime.now();
        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextProfileVersion = nextVersion(state.getProfileVersion());
        state.setProfileVersion(nextProfileVersion);
        state.setLastProfileUpdateTime(now);
        state.setUpdateTime(now);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, status, null, nextProfileVersion);
        publishProfileUpdated(userId, nextProfileVersion, changedFields);
        return UpdateResult.success(userId, nextProfileVersion);
    }

    private Long resolveUserId(String ptk) {
        Long ptkUserId = loadUserIdFromPtk(ptk);
        Long sessionUserId = RequestContext.getUserId();
        if (ptkUserId != null && sessionUserId != null && !ptkUserId.equals(sessionUserId)) {
            return null;
        }
        return ptkUserId != null ? ptkUserId : sessionUserId;
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

    private void publishProfileUpdated(Long userId, Long profileVersion, Map<String, Object> changedFields) {
        EventMessage message = new EventMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setEventType(EVENT_TYPE_PROFILE_UPDATED);
        message.setUserId(userId);
        message.setProfileVersion(profileVersion);
        message.setTs(System.currentTimeMillis());
        if (changedFields != null && !changedFields.isEmpty()) {
            try {
                message.setPayload(objectMapper.writeValueAsString(changedFields));
            } catch (JsonProcessingException ex) {
                message.setPayload(null);
            }
        }
        eventPublisher.publish(message);
    }

    private Map<String, Object> resolveChangedFields(PortalUser user, String realName, String mobile, String email) {
        Map<String, Object> changed = new LinkedHashMap<>();
        if (!Objects.equals(user.getRealName(), realName)) {
            changed.put("realName", realName);
        }
        if (!Objects.equals(user.getMobile(), mobile)) {
            changed.put("mobile", mobile);
        }
        if (!Objects.equals(user.getEmail(), email)) {
            changed.put("email", email);
        }
        return changed;
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

    public static class ProfileResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String realName;
        private final String mobile;
        private final String email;

        private ProfileResult(boolean success, ErrorCode errorCode, String realName, String mobile, String email) {
            this.success = success;
            this.errorCode = errorCode;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
        }

        public static ProfileResult success(String realName, String mobile, String email) {
            return new ProfileResult(true, null, realName, mobile, email);
        }

        public static ProfileResult failure(ErrorCode errorCode) {
            return new ProfileResult(false, errorCode, null, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getRealName() {
            return realName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getEmail() {
            return email;
        }
    }

    public static class UpdateResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final Long userId;
        private final Long profileVersion;

        private UpdateResult(boolean success, ErrorCode errorCode, Long userId, Long profileVersion) {
            this.success = success;
            this.errorCode = errorCode;
            this.userId = userId;
            this.profileVersion = profileVersion;
        }

        public static UpdateResult success(Long userId, Long profileVersion) {
            return new UpdateResult(true, null, userId, profileVersion);
        }

        public static UpdateResult failure(ErrorCode errorCode) {
            return new UpdateResult(false, errorCode, null, null);
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

        public Long getProfileVersion() {
            return profileVersion;
        }
    }
}
