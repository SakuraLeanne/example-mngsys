package com.example.mngsys.portal.service;

import com.example.mngsys.api.notify.core.EventNotifyPublisher;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.entity.PortalUserAuthState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
/**
 * PortalProfileService。
 */
public class PortalProfileService {

    private static final String PTK_PREFIX = "portal:ptk:";
    private static final String EVENT_TYPE_PROFILE_UPDATED = "portal:events:USER_PROFILE_UPDATED";

    private final PortalUserService portalUserService;
    private final PortalUserAuthStateService portalUserAuthStateService;
    private final UserAuthCacheService userAuthCacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final EventNotifyPublisher eventNotifyPublisher;

    public PortalProfileService(PortalUserService portalUserService,
                                PortalUserAuthStateService portalUserAuthStateService,
                                UserAuthCacheService userAuthCacheService,
                                StringRedisTemplate stringRedisTemplate,
                                ObjectMapper objectMapper,
                                EventNotifyPublisher eventNotifyPublisher) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.eventNotifyPublisher = eventNotifyPublisher;
    }

    public ProfileResult getProfile(String ptk) {
        String userId = resolveUserId(ptk);
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
        return ProfileResult.success(user.getId(), user.getUsername(), user.getRealName(), user.getMobile(), user.getEmail(), status);
    }

    @Transactional
    public UpdateResult updateProfile(String ptk, String realName, String mobile, String email) {
        String userId = resolveUserId(ptk);
        if (userId == null) {
            return UpdateResult.failure(ErrorCode.UNAUTHENTICATED);
        }
        if (!StringUtils.hasText(mobile)) {
            return UpdateResult.failure(ErrorCode.INVALID_ARGUMENT);
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
        try {
            portalUserService.updateById(user);
        } catch (IllegalArgumentException ex) {
            return UpdateResult.failure(ErrorCode.INVALID_ARGUMENT);
        }

        LocalDateTime now = LocalDateTime.now();
        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextProfileVersion = nextVersion(state.getProfileVersion());
        state.setProfileVersion(nextProfileVersion);
        state.setLastProfileUpdateTime(now);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, status, null, nextProfileVersion);
        deletePtk(ptk);
        publishProfileUpdated(userId, nextProfileVersion, changedFields);
        return UpdateResult.success(userId, nextProfileVersion);
    }

    private String resolveUserId(String ptk) {
        String ptkUserId = loadUserIdFromPtk(ptk);
        String sessionUserId = RequestContext.getUserId();
        if (ptkUserId != null && sessionUserId != null && !ptkUserId.equals(sessionUserId)) {
            return null;
        }
        return ptkUserId != null ? ptkUserId : sessionUserId;
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

    private void publishProfileUpdated(String userId, Long profileVersion, Map<String, Object> changedFields) {
        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("profileVersion", profileVersion.toString());
        if (changedFields != null && !changedFields.isEmpty()) {
            try {
                message.put("changedFields", objectMapper.writeValueAsString(changedFields));
            } catch (JsonProcessingException ex) {
                message.put("changedFields", null);
            }
        }
        message.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        eventNotifyPublisher.publish(EVENT_TYPE_PROFILE_UPDATED, message);
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

    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return StringUtils.hasText(text) ? text : null;
    }

    public static class ProfileResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String userId;
        private final String username;
        private final String realName;
        private final String mobile;
        private final String email;
        private final Integer status;

        private ProfileResult(boolean success, ErrorCode errorCode, String userId, String username,
                              String realName, String mobile, String email, Integer status) {
            this.success = success;
            this.errorCode = errorCode;
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
        }

        public static ProfileResult success(String userId, String username, String realName, String mobile, String email, Integer status) {
            return new ProfileResult(true, null, userId, username, realName, mobile, email, status);
        }

        public static ProfileResult failure(ErrorCode errorCode) {
            return new ProfileResult(false, errorCode, null, null, null, null, null, null);
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

        public String getUsername() {
            return username;
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

        public Integer getStatus() {
            return status;
        }
    }

    public static class UpdateResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String userId;
        private final Long profileVersion;

        private UpdateResult(boolean success, ErrorCode errorCode, String userId, Long profileVersion) {
            this.success = success;
            this.errorCode = errorCode;
            this.userId = userId;
            this.profileVersion = profileVersion;
        }

        public static UpdateResult success(String userId, Long profileVersion) {
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

        public String getUserId() {
            return userId;
        }

        public Long getProfileVersion() {
            return profileVersion;
        }
    }
}
