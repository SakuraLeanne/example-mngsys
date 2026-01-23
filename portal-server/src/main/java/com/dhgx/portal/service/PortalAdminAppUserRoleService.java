package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dhgx.api.notify.core.EventNotifyPublisher;
import com.dhgx.common.redis.RedisKeys;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.entity.AppRole;
import com.dhgx.portal.entity.AppUserRole;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
/**
 * PortalAdminAppUserRoleService。
 */
public class PortalAdminAppUserRoleService {

    private static final String MENU_CACHE_PREFIX = "app:menu:user:";
    private static final String EVENT_TOKEN_VERSION_UPDATED = "portal:events:USER_TOKEN_VERSION_UPDATED";
    private static final DateTimeFormatter EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppUserRoleService appUserRoleService;
    private final AppRoleService appRoleService;
    private final StringRedisTemplate stringRedisTemplate;
    private final EventNotifyPublisher eventNotifyPublisher;
    private final RolePermissionService rolePermissionService;

    public PortalAdminAppUserRoleService(AppUserRoleService appUserRoleService,
                                         AppRoleService appRoleService,
                                         StringRedisTemplate stringRedisTemplate,
                                         EventNotifyPublisher eventNotifyPublisher,
                                         RolePermissionService rolePermissionService) {
        this.appUserRoleService = appUserRoleService;
        this.appRoleService = appRoleService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.eventNotifyPublisher = eventNotifyPublisher;
        this.rolePermissionService = rolePermissionService;
    }

    public Result<List<AppRole>> listUserRoles(String userId, String operatorId) {
        if (!StringUtils.hasText(userId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "用户ID不能为空");
        }
        if (!StringUtils.hasText(operatorId)) {
            return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
        }
        boolean selfRequest = userId.equals(operatorId);
        Set<String> adminAppCodes = rolePermissionService.listAppAdminAppCodes(operatorId);
        if (!selfRequest && CollectionUtils.isEmpty(adminAppCodes)) {
            return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
        }
        List<AppUserRole> relations = appUserRoleService.list(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Long> roleIds = relations.stream().map(AppUserRole::getRoleId).distinct().collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        LambdaQueryWrapper<AppRole> wrapper = new LambdaQueryWrapper<AppRole>()
                .in(AppRole::getId, roleIds);
        if (!CollectionUtils.isEmpty(adminAppCodes) && !selfRequest) {
            wrapper.in(AppRole::getAppCode, adminAppCodes);
        }
        List<AppRole> roles = appRoleService.list(wrapper);
        return Result.success(roles);
    }

    public Result<List<AppRole>> listRolesByIds(List<Long> roleIds) {
        List<Long> normalized = roleIds == null ? new ArrayList<>() : roleIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(normalized)) {
            return Result.success(new ArrayList<>());
        }
        List<AppRole> roles = appRoleService.list(new LambdaQueryWrapper<AppRole>()
                .in(AppRole::getId, normalized));
        if (roles.size() != normalized.size()) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色不存在");
        }
        return Result.success(roles);
    }

    @Transactional
    public Result<Void> grantRoles(String userId, List<Long> roleIds, String operatorId) {
        if (!StringUtils.hasText(userId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "用户ID不能为空");
        }
        if (CollectionUtils.isEmpty(rolePermissionService.listAppAdminAppCodes(operatorId))) {
            return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
        }
        List<Long> normalized = roleIds == null ? new ArrayList<>() : roleIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(normalized)) {
            List<AppRole> roles = appRoleService.list(new LambdaQueryWrapper<AppRole>()
                    .in(AppRole::getId, normalized));
            if (roles.size() != normalized.size()) {
                return Result.failure(ErrorCode.INVALID_ARGUMENT, "角色不存在");
            }
            Set<String> adminAppCodes = rolePermissionService.listAppAdminAppCodes(operatorId);
            boolean appMismatch = roles.stream()
                    .map(AppRole::getAppCode)
                    .anyMatch(appCode -> !adminAppCodes.contains(appCode));
            if (appMismatch) {
                return Result.failure(ErrorCode.FORBIDDEN, "权限不足，请联系管理员");
            }
        }
        appUserRoleService.remove(new LambdaQueryWrapper<AppUserRole>()
                .eq(AppUserRole::getUserId, userId));
        if (!CollectionUtils.isEmpty(normalized)) {
            List<AppUserRole> relations = normalized.stream().map(roleId -> {
                AppUserRole relation = new AppUserRole();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                relation.setCreateTime(LocalDateTime.now());
                return relation;
            }).collect(Collectors.toList());
            appUserRoleService.saveBatch(relations);
        }
        /*stringRedisTemplate.delete(buildMenuCacheKey(userId));
        Long tokenVersion = bumpTokenVersion(userId);
        publishTokenVersionUpdated(userId, tokenVersion, operatorId);*/
        return Result.success(null);
    }

    private String buildMenuCacheKey(String userId) {
        return MENU_CACHE_PREFIX + userId;
    }

    private Long bumpTokenVersion(String userId) {
        return stringRedisTemplate.opsForValue().increment(RedisKeys.tokenVersion(userId));
    }

    private void publishTokenVersionUpdated(String userId, Long tokenVersion, String operatorId) {
        if (tokenVersion == null) {
            return;
        }
        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("tokenVersion", tokenVersion.toString());
        message.put("operatorId", operatorId);
        message.put("time", LocalDateTime.now().format(EVENT_TIME_FORMATTER));
        eventNotifyPublisher.publish(EVENT_TOKEN_VERSION_UPDATED, message);
    }

    public static class Result<T> {
        private final boolean success;
        private final ErrorCode errorCode;
        private final String message;
        private final T data;

        private Result(boolean success, ErrorCode errorCode, String message, T data) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
            this.data = data;
        }

        public static <T> Result<T> success(T data) {
            return new Result<>(true, null, null, data);
        }

        public static <T> Result<T> failure(ErrorCode errorCode, String message) {
            return new Result<>(false, errorCode, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }
}
