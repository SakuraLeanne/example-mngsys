package com.example.mngsys.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mngsys.api.notify.core.EventNotifyPublisher;
import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.entity.PortalAuditLog;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.entity.PortalUserAuthState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PortalAdminUserService。
 * <p>
 * 提供管理端用户的查询、禁用/启用等业务能力，并负责同步鉴权缓存与审计日志。
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
    /** 审计日志服务。 */
    private final PortalAuditLogService portalAuditLogService;
    /** 认证中心客户端。 */
    private final AuthClient authClient;
    /** 事件发布器。 */
    private final EventNotifyPublisher eventNotifyPublisher;
    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入依赖。
     */
    public PortalAdminUserService(PortalUserService portalUserService,
                                  PortalUserAuthStateService portalUserAuthStateService,
                                  UserAuthCacheService userAuthCacheService,
                                  PortalAuditLogService portalAuditLogService,
                                  AuthClient authClient,
                                  EventNotifyPublisher eventNotifyPublisher,
                                  ObjectMapper objectMapper) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.portalAuditLogService = portalAuditLogService;
        this.authClient = authClient;
        this.eventNotifyPublisher = eventNotifyPublisher;
        this.objectMapper = objectMapper;
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
    public PageResult listUsers(int page, int size, String keyword, Integer status) {
        int pageIndex = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        Page<PortalUser> query = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<PortalUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(inner -> inner.like(PortalUser::getUsername, keyword)
                    .or()
                    .like(PortalUser::getRealName, keyword)
                    .or()
                    .like(PortalUser::getMobile, keyword)
                    .or()
                    .like(PortalUser::getEmail, keyword));
        }
        if (status != null) {
            wrapper.eq(PortalUser::getStatus, status);
        }
        wrapper.orderByDesc(PortalUser::getCreateTime);
        Page<PortalUser> result = portalUserService.page(query, wrapper);
        List<PortalUser> records = result.getRecords();
        List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> users = records.stream()
                .map(user -> new com.example.mngsys.portal.controller.AdminUserController.UserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getMobile(),
                        user.getEmail(),
                        user.getStatus(),
                        user.getRemark()))
                .collect(Collectors.toList());
        return new PageResult(result.getTotal(), pageIndex, pageSize, users);
    }

    /**
     * 查询用户详情。
     *
     * @param userId 用户 ID
     * @return 包含详情的结果
     */
    public UserDetailResult getUserDetail(String userId) {
        if (!StringUtils.hasText(userId)) {
            return UserDetailResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return UserDetailResult.failure(ErrorCode.NOT_FOUND);
        }
        com.example.mngsys.portal.controller.AdminUserController.UserDetail detail =
                new com.example.mngsys.portal.controller.AdminUserController.UserDetail(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getMobile(),
                        user.getEmail(),
                        user.getStatus(),
                        user.getRemark(),
                        user.getCreateTime());
        return UserDetailResult.success(detail);
    }

    /**
     * 禁用用户并同步缓存与事件。
     */
    @Transactional
    public ActionResult disableUser(String userId, String reason, String operatorId, String ip) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(reason)) {
            return ActionResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ActionResult.failure(ErrorCode.NOT_FOUND);
        }
        user.setStatus(STATUS_DISABLED);
        user.setRemark(reason);
        portalUserService.updateById(user);

        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setLastDisableTime(LocalDateTime.now());
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, STATUS_DISABLED, nextAuthVersion, state.getProfileVersion());

        authClient.kick(userId);
        publishDisabled(userId, nextAuthVersion, reason, operatorId);
        writeAuditLog(operatorId, "DISABLE_USER", String.valueOf(userId), reason, ip);
        return ActionResult.success();
    }

    /**
     * 启用用户并同步缓存与事件。
     */
    @Transactional
    public ActionResult enableUser(String userId, String operatorId, String ip) {
        if (!StringUtils.hasText(userId)) {
            return ActionResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ActionResult.failure(ErrorCode.NOT_FOUND);
        }
        user.setStatus(STATUS_ENABLED);
        user.setRemark(null);
        portalUserService.updateById(user);

        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, STATUS_ENABLED, nextAuthVersion, state.getProfileVersion());

        authClient.kick(userId);
        publishEnabled(userId, nextAuthVersion, operatorId);
        writeAuditLog(operatorId, "ENABLE_USER", String.valueOf(userId), null, ip);
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
    private void publishDisabled(String userId, Long authVersion, String reason, String operatorId) {
        Map<String, String> message = new HashMap<>();
        message.put("userId", userId);
        message.put("authVersion", authVersion.toString());
        message.put("operatorId", operatorId);
        if (StringUtils.hasText(reason)) {
            try {
                message.put("reason", objectMapper.writeValueAsString(new DisablePayload(reason)));
            } catch (JsonProcessingException ex) {
                message.put("reason", null);
            }
        }
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
     * 记录审计日志。
     */
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
        private final List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> users;

        /**
         * 构造函数。
         */
        public PageResult(long total, int page, int size,
                          List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> users) {
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
        public List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> getUsers() {
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
        private final com.example.mngsys.portal.controller.AdminUserController.UserDetail detail;

        private UserDetailResult(boolean success, ErrorCode errorCode,
                                 com.example.mngsys.portal.controller.AdminUserController.UserDetail detail) {
            this.success = success;
            this.errorCode = errorCode;
            this.detail = detail;
        }

        /** 构建成功结果。 */
        public static UserDetailResult success(
                com.example.mngsys.portal.controller.AdminUserController.UserDetail detail) {
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
        public com.example.mngsys.portal.controller.AdminUserController.UserDetail getDetail() {
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

    /**
     * 禁用事件负载。
     */
    private static class DisablePayload {
        /** 禁用原因。 */
        private final String reason;

        private DisablePayload(String reason) {
            this.reason = reason;
        }

        /** 获取禁用原因。 */
        public String getReason() {
            return reason;
        }
    }
}
