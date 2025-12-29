package com.example.mngsys.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.entity.AppUserRole;
import com.example.mngsys.portal.entity.PortalAuditLog;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
/**
 * PortalAdminAppUserRoleService。
 */
public class PortalAdminAppUserRoleService {

    private static final String MENU_CACHE_PREFIX = "app:menu:user:";

    private final AppUserRoleService appUserRoleService;
    private final AppRoleService appRoleService;
    private final PortalAuditLogService portalAuditLogService;
    private final StringRedisTemplate stringRedisTemplate;

    public PortalAdminAppUserRoleService(AppUserRoleService appUserRoleService,
                                         AppRoleService appRoleService,
                                         PortalAuditLogService portalAuditLogService,
                                         StringRedisTemplate stringRedisTemplate) {
        this.appUserRoleService = appUserRoleService;
        this.appRoleService = appRoleService;
        this.portalAuditLogService = portalAuditLogService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Result<List<AppRole>> listUserRoles(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "用户ID不能为空");
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
        List<AppRole> roles = appRoleService.list(new LambdaQueryWrapper<AppRole>()
                .in(AppRole::getId, roleIds));
        return Result.success(roles);
    }

    @Transactional
    public Result<Void> grantRoles(String userId, List<Long> roleIds, String operatorId, String ip) {
        if (!StringUtils.hasText(userId)) {
            return Result.failure(ErrorCode.INVALID_ARGUMENT, "用户ID不能为空");
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
        stringRedisTemplate.delete(buildMenuCacheKey(userId));
        writeAuditLog(operatorId, "GRANT_USER_ROLES", String.valueOf(userId), normalized.toString(), ip);
        return Result.success(null);
    }

    private String buildMenuCacheKey(String userId) {
        return MENU_CACHE_PREFIX + userId;
    }

    private void writeAuditLog(String operatorId, String action, String resource, String detail, String ip) {
        PortalAuditLog log = new PortalAuditLog();
        log.setUserId(operatorId);
        log.setAction(action);
        log.setResource(resource);
        log.setDetail(detail);
        log.setIp(ip);
        log.setStatus(1);
        log.setCreateTime(LocalDateTime.now());
        portalAuditLogService.save(log);
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
