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
        if (request == null) {
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
        PortalUser merged = mergeForUpdate(user, request);
        merged.setId(userId);
        try {
            portalUserService.updateById(merged);
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
        putIfChanged(changed, "username", user.getUsername(), request.getUsername());
        putIfChanged(changed, "mobile", user.getMobile(), request.getMobile());
        putIfChanged(changed, "mobileVerified", user.getMobileVerified(), request.getMobileVerified());
        putIfChanged(changed, "email", user.getEmail(), request.getEmail());
        putIfChanged(changed, "emailVerified", user.getEmailVerified(), request.getEmailVerified());
        putIfChanged(changed, "password", user.getPassword(), request.getPassword());
        putIfChanged(changed, "status", user.getStatus(), request.getStatus());
        putIfChanged(changed, "realName", user.getRealName(), request.getRealName());
        putIfChanged(changed, "nickName", user.getNickName(), request.getNickName());
        putIfChanged(changed, "gender", user.getGender(), request.getGender());
        putIfChanged(changed, "birthday", user.getBirthday(), request.getBirthday());
        putIfChanged(changed, "companyName", user.getCompanyName(), request.getCompanyName());
        putIfChanged(changed, "department", user.getDepartment(), request.getDepartment());
        putIfChanged(changed, "position", user.getPosition(), request.getPosition());
        putIfChanged(changed, "tenantId", user.getTenantId(), request.getTenantId());
        putIfChanged(changed, "remark", user.getRemark(), request.getRemark());
        changed.put("updateTime", LocalDateTime.now());
        changed.put("updateBy", request.getId());
        /*putIfChanged(changed, "createTime", user.getCreateTime(), request.getCreateTime());
        putIfChanged(changed, "updateTime", user.getUpdateTime(), request.getUpdateTime());
        putIfChanged(changed, "createBy", user.getCreateBy(), request.getCreateBy());
        putIfChanged(changed, "updateBy", user.getUpdateBy(), request.getUpdateBy());*/
        return changed;
    }

    private void putIfChanged(Map<String, Object> changed, String field, Object existing, Object requestValue) {
        if (requestValue != null && !Objects.equals(existing, requestValue)) {
            changed.put(field, requestValue);
        }
    }

    private PortalUser mergeForUpdate(PortalUser existing, PortalUser request) {
        PortalUser merged = new PortalUser();
        merged.setId(existing.getId());
        merged.setUsername(existing.getUsername());
        merged.setMobile(existing.getMobile());
        merged.setMobileVerified(existing.getMobileVerified());
        merged.setEmail(existing.getEmail());
        merged.setEmailVerified(existing.getEmailVerified());
        merged.setPassword(existing.getPassword());
        merged.setStatus(existing.getStatus());
        merged.setRealName(existing.getRealName());
        merged.setNickName(existing.getNickName());
        merged.setGender(existing.getGender());
        merged.setBirthday(existing.getBirthday());
        merged.setCompanyName(existing.getCompanyName());
        merged.setDepartment(existing.getDepartment());
        merged.setPosition(existing.getPosition());
        merged.setTenantId(existing.getTenantId());
        merged.setRemark(existing.getRemark());
        merged.setCreateTime(existing.getCreateTime());
        merged.setUpdateTime(existing.getUpdateTime());
        merged.setCreateBy(existing.getCreateBy());
        merged.setUpdateBy(existing.getUpdateBy());

        if (request.getUsername() != null) {
            merged.setUsername(request.getUsername());
        }
        if (request.getMobile() != null) {
            merged.setMobile(request.getMobile());
        }
        if (request.getMobileVerified() != null) {
            merged.setMobileVerified(request.getMobileVerified());
        }
        if (request.getEmail() != null) {
            merged.setEmail(request.getEmail());
        }
        if (request.getEmailVerified() != null) {
            merged.setEmailVerified(request.getEmailVerified());
        }
        if (request.getPassword() != null) {
            merged.setPassword(request.getPassword());
        }
        if (request.getStatus() != null) {
            merged.setStatus(request.getStatus());
        }
        if (request.getRealName() != null) {
            merged.setRealName(request.getRealName());
        }
        if (request.getNickName() != null) {
            merged.setNickName(request.getNickName());
        }
        if (request.getGender() != null) {
            merged.setGender(request.getGender());
        }
        if (request.getBirthday() != null) {
            merged.setBirthday(request.getBirthday());
        }
        if (request.getCompanyName() != null) {
            merged.setCompanyName(request.getCompanyName());
        }
        if (request.getDepartment() != null) {
            merged.setDepartment(request.getDepartment());
        }
        if (request.getPosition() != null) {
            merged.setPosition(request.getPosition());
        }
        if (request.getTenantId() != null) {
            merged.setTenantId(request.getTenantId());
        }
        if (request.getRemark() != null) {
            merged.setRemark(request.getRemark());
        }
        if (request.getCreateTime() != null) {
            merged.setCreateTime(request.getCreateTime());
        }
        if (request.getUpdateTime() != null) {
            merged.setUpdateTime(request.getUpdateTime());
        }
        if (request.getCreateBy() != null) {
            merged.setCreateBy(request.getCreateBy());
        }
        if (request.getUpdateBy() != null) {
            merged.setUpdateBy(request.getUpdateBy());
        }
        return merged;
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
