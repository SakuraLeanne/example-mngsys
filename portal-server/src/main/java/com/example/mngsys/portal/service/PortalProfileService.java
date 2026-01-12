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
        return ProfileResult.success(user);
    }

    @Transactional
    public UpdateResult updateProfile(String ptk, PortalUser request) {
        String userId = resolveUserId(ptk);
        if (userId == null) {
            return UpdateResult.failure(ErrorCode.UNAUTHENTICATED);
        }
        if (request == null || !StringUtils.hasText(request.getMobile())) {
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
        Map<String, Object> changedFields = resolveChangedFields(user, request);
        request.setId(userId);
        try {
            portalUserService.updateById(request);
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

    private Map<String, Object> resolveChangedFields(PortalUser user, PortalUser request) {
        Map<String, Object> changed = new LinkedHashMap<>();
        if (!Objects.equals(user.getUsername(), request.getUsername())) {
            changed.put("username", request.getUsername());
        }
        if (!Objects.equals(user.getMobile(), request.getMobile())) {
            changed.put("mobile", request.getMobile());
        }
        if (!Objects.equals(user.getMobileVerified(), request.getMobileVerified())) {
            changed.put("mobileVerified", request.getMobileVerified());
        }
        if (!Objects.equals(user.getEmail(), request.getEmail())) {
            changed.put("email", request.getEmail());
        }
        if (!Objects.equals(user.getEmailVerified(), request.getEmailVerified())) {
            changed.put("emailVerified", request.getEmailVerified());
        }
        if (!Objects.equals(user.getPassword(), request.getPassword())) {
            changed.put("password", request.getPassword());
        }
        if (!Objects.equals(user.getStatus(), request.getStatus())) {
            changed.put("status", request.getStatus());
        }
        if (!Objects.equals(user.getRealName(), request.getRealName())) {
            changed.put("realName", request.getRealName());
        }
        if (!Objects.equals(user.getNickName(), request.getNickName())) {
            changed.put("nickName", request.getNickName());
        }
        if (!Objects.equals(user.getGender(), request.getGender())) {
            changed.put("gender", request.getGender());
        }
        if (!Objects.equals(user.getBirthday(), request.getBirthday())) {
            changed.put("birthday", request.getBirthday());
        }
        if (!Objects.equals(user.getCompanyName(), request.getCompanyName())) {
            changed.put("companyName", request.getCompanyName());
        }
        if (!Objects.equals(user.getDepartment(), request.getDepartment())) {
            changed.put("department", request.getDepartment());
        }
        if (!Objects.equals(user.getPosition(), request.getPosition())) {
            changed.put("position", request.getPosition());
        }
        if (!Objects.equals(user.getTenantId(), request.getTenantId())) {
            changed.put("tenantId", request.getTenantId());
        }
        if (!Objects.equals(user.getRemark(), request.getRemark())) {
            changed.put("remark", request.getRemark());
        }
        if (!Objects.equals(user.getCreateTime(), request.getCreateTime())) {
            changed.put("createTime", request.getCreateTime());
        }
        if (!Objects.equals(user.getUpdateTime(), request.getUpdateTime())) {
            changed.put("updateTime", request.getUpdateTime());
        }
        if (!Objects.equals(user.getCreateBy(), request.getCreateBy())) {
            changed.put("createBy", request.getCreateBy());
        }
        if (!Objects.equals(user.getUpdateBy(), request.getUpdateBy())) {
            changed.put("updateBy", request.getUpdateBy());
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
        private final PortalUser user;

        private ProfileResult(boolean success, ErrorCode errorCode, PortalUser user) {
            this.success = success;
            this.errorCode = errorCode;
            this.user = user;
        }

        public static ProfileResult success(PortalUser user) {
            return new ProfileResult(true, null, user);
        }

        public static ProfileResult failure(ErrorCode errorCode) {
            return new ProfileResult(false, errorCode, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public PortalUser getUser() {
            return user;
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
