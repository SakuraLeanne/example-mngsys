package com.dhgx.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhgx.api.notify.core.EventNotifyPublisher;
import com.dhgx.portal.client.AuthClient;
import com.dhgx.portal.common.api.ErrorCode;
import com.dhgx.portal.controller.AdminUserController;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.entity.PortalUserAuthState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * PortalAdminUserService。
 * <p>
 * 提供管理端用户的查询、禁用/启用等业务能力，并负责同步鉴权缓存。
 * </p>
 */
@Service
public class PortalAdminUserService {

    /** 用户启用状态码。 */
    private static final int STATUS_ENABLED = 1;
    /** 用户禁用状态码。 */
    private static final int STATUS_DISABLED = 0;
    /** 用户禁用事件类型。 */
    private static final String EVENT_USER_DISABLED = "portal:events:USER_DISABLED";
    /** 用户启用事件类型。 */
    private static final String EVENT_USER_ENABLED = "portal:events:USER_ENABLED";

    /** 门户用户服务。 */
    private final PortalUserService portalUserService;
    /** 用户鉴权状态服务。 */
    private final PortalUserAuthStateService portalUserAuthStateService;
    /** 用户鉴权缓存服务。 */
    private final UserAuthCacheService userAuthCacheService;
    /** 认证中心客户端。 */
    private final AuthClient authClient;
    /** 事件发布器。 */
    private final EventNotifyPublisher eventNotifyPublisher;
    /** 角色权限服务。 */
    private final RolePermissionService rolePermissionService;

    /**
     * 构造函数，注入依赖。
     */
    public PortalAdminUserService(PortalUserService portalUserService,
                                  PortalUserAuthStateService portalUserAuthStateService,
                                  UserAuthCacheService userAuthCacheService,
                                  AuthClient authClient,
                                  EventNotifyPublisher eventNotifyPublisher,
                                  RolePermissionService rolePermissionService) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.authClient = authClient;
        this.eventNotifyPublisher = eventNotifyPublisher;
        this.rolePermissionService = rolePermissionService;
    }

    /**
     * 分页查询用户列表。
     *
     * @param page    页码（从 1 开始）
     * @param size    每页数量
     * @param keyword 搜索关键字
     * @param status  用户状态
     * @return 分页结果
     */
    public Page<PortalUser> listUsers(int page, int size, String keyword, Integer status, String requesterId) {
        int pageIndex = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        if (rolePermissionService.isPortalAdmin(requesterId)) {
            Page<PortalUser> query = new Page<>(pageIndex, pageSize);
            LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(keyword)) {
                wrapper.and(inner -> inner.like(PortalUser::getUsername, keyword)
                        .or()
                        .like(PortalUser::getRealName, keyword)
                        .or()
                        .like(PortalUser::getMobile, keyword)
                        .or()
                        .like(PortalUser::getNickName, keyword));
            }
            if (status != null) {
                wrapper.eq(PortalUser::getStatus, status);
            }
            wrapper.orderByDesc(PortalUser::getCreateTime);
            return portalUserService.page(query, wrapper);
        }
        Page<PortalUser> result = new Page<>(pageIndex, pageSize);
        PortalUser user = portalUserService.getById(requesterId);
        if (user == null) {
            result.setTotal(0);
            result.setRecords(Collections.emptyList());
            return result;
        }
        result.setTotal(1);
        result.setRecords(Collections.singletonList(user));
        return result;
    }

    /**
     * 查询用户详情。
     *
     * @param userId 用户 ID
     * @return 包含详情的结果
     */
    public UserDetailResult getUserDetail(String userId, String requesterId) {
        if (!StringUtils.hasText(userId)) {
            return UserDetailResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        if (!rolePermissionService.isPortalAdmin(requesterId) && !userId.equals(requesterId)) {
            return UserDetailResult.failure(ErrorCode.FORBIDDEN);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return UserDetailResult.failure(ErrorCode.NOT_FOUND);
        }
        return UserDetailResult.success(user);
    }

    /**
     * 更新用户启用状态并同步缓存与事件。
     */
    @Transactional
    public ActionResult updateUserStatus(String userId, boolean enabled, String operatorId) {
        if (!StringUtils.hasText(userId)) {
            return ActionResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        if (!rolePermissionService.isPortalAdmin(operatorId)) {
            return ActionResult.failure(ErrorCode.FORBIDDEN);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ActionResult.failure(ErrorCode.NOT_FOUND);
        }
        int status = enabled ? STATUS_ENABLED : STATUS_DISABLED;
        user.setStatus(status);
        user.setRemark(null);
        portalUserService.updateById(user);

        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        if (!enabled) {
            state.setLastDisableTime(LocalDateTime.now());
        }
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, status, nextAuthVersion, state.getProfileVersion());

        authClient.kick(userId);
        if (enabled) {
            publishEnabled(userId, nextAuthVersion, operatorId);
        } else {
            publishDisabled(userId, nextAuthVersion, operatorId);
        }
        return ActionResult.success();
    }

    /**
     * 加载或初始化用户鉴权状态。
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
     * 发布用户禁用事件。
     * operatorId 操作人ID
     */
    private void publishDisabled(String userId, Long authVersion, String operatorId) {
        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("authVersion", authVersion.toString());
        message.put("operatorId", operatorId);
        message.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        eventNotifyPublisher.publish(EVENT_USER_DISABLED, message);
    }

    /**
     * 发布用户启用事件。
     */
    private void publishEnabled(String userId, Long authVersion, String operatorId) {
        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("authVersion", authVersion.toString());
        message.put("operatorId", operatorId);
        message.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        eventNotifyPublisher.publish(EVENT_USER_ENABLED, message);
    }


    /**
     * 用户列表分页结果。
     */
    public static class PageResult {
        /** 总记录数。 */
        private final long total;
        /** 当前页码。 */
        private final int page;
        /** 每页大小。 */
        private final int size;
        /** 用户摘要列表。 */
        private final List<AdminUserController.UserSummary> users;

        /**
         * 构造函数。
         */
        public PageResult(long total, int page, int size,
                          List<AdminUserController.UserSummary> users) {
            this.total = total;
            this.page = page;
            this.size = size;
            this.users = users;
        }

        /** 获取总记录数。 */
        public long getTotal() {
            return total;
        }

        /** 获取当前页码。 */
        public int getPage() {
            return page;
        }

        /** 获取每页大小。 */
        public int getSize() {
            return size;
        }

        /** 获取用户列表。 */
        public List<AdminUserController.UserSummary> getUsers() {
            return users;
        }
    }

    /**
     * 用户详情查询结果。
     */
    public static class UserDetailResult {
        /** 是否成功。 */
        private final boolean success;
        /** 错误码。 */
        private final ErrorCode errorCode;
        /** 详情数据。 */
        private final PortalUser detail;

        private UserDetailResult(boolean success, ErrorCode errorCode,
                                 PortalUser detail) {
            this.success = success;
            this.errorCode = errorCode;
            this.detail = detail;
        }

        /** 构建成功结果。 */
        public static UserDetailResult success(PortalUser detail) {
            return new UserDetailResult(true, null, detail);
        }

        /** 构建失败结果。 */
        public static UserDetailResult failure(ErrorCode errorCode) {
            return new UserDetailResult(false, errorCode, null);
        }

        /** 是否成功。 */
        public boolean isSuccess() {
            return success;
        }

        /** 获取错误码。 */
        public ErrorCode getErrorCode() {
            return errorCode;
        }

        /** 获取详情。 */
        public PortalUser getDetail() {
            return detail;
        }
    }

    /**
     * 通用操作结果。
     */
    public static class ActionResult {
        /** 是否成功。 */
        private final boolean success;
        /** 错误码。 */
        private final ErrorCode errorCode;

        private ActionResult(boolean success, ErrorCode errorCode) {
            this.success = success;
            this.errorCode = errorCode;
        }

        /** 构建成功结果。 */
        public static ActionResult success() {
            return new ActionResult(true, null);
        }

        /** 构建失败结果。 */
        public static ActionResult failure(ErrorCode errorCode) {
            return new ActionResult(false, errorCode);
        }

        /** 是否成功。 */
        public boolean isSuccess() {
            return success;
        }

        /** 获取错误码。 */
        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

}
