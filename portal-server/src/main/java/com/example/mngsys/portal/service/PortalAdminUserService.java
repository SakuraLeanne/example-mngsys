package com.example.mngsys.portal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mngsys.portal.client.AuthClient;
import com.example.mngsys.portal.common.api.ErrorCode;
import com.example.mngsys.portal.entity.PortalAuditLog;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.entity.PortalUserAuthState;
import com.example.mngsys.redisevent.EventMessage;
import com.example.mngsys.redisevent.EventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
/**
 * PortalAdminUserService。
 */
public class PortalAdminUserService {

    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final String EVENT_USER_DISABLED = "USER_DISABLED";
    private static final String EVENT_USER_ENABLED = "USER_ENABLED";

    private final PortalUserService portalUserService;
    private final PortalUserAuthStateService portalUserAuthStateService;
    private final UserAuthCacheService userAuthCacheService;
    private final PortalAuditLogService portalAuditLogService;
    private final AuthClient authClient;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public PortalAdminUserService(PortalUserService portalUserService,
                                  PortalUserAuthStateService portalUserAuthStateService,
                                  UserAuthCacheService userAuthCacheService,
                                  PortalAuditLogService portalAuditLogService,
                                  AuthClient authClient,
                                  EventPublisher eventPublisher,
                                  ObjectMapper objectMapper) {
        this.portalUserService = portalUserService;
        this.portalUserAuthStateService = portalUserAuthStateService;
        this.userAuthCacheService = userAuthCacheService;
        this.portalAuditLogService = portalAuditLogService;
        this.authClient = authClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

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
        wrapper.orderByDesc(PortalUser::getId);
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
                        user.getDisableTime()))
                .collect(Collectors.toList());
        return new PageResult(result.getTotal(), pageIndex, pageSize, users);
    }

    public UserDetailResult getUserDetail(Long userId) {
        if (userId == null) {
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
                        user.getDisableReason(),
                        user.getDisableTime(),
                        user.getCreateTime());
        return UserDetailResult.success(detail);
    }

    @Transactional
    public ActionResult disableUser(Long userId, String reason, Long operatorId, String ip) {
        if (userId == null || !StringUtils.hasText(reason)) {
            return ActionResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ActionResult.failure(ErrorCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        user.setStatus(STATUS_DISABLED);
        user.setDisableReason(reason);
        user.setDisableTime(now);
        user.setUpdateTime(now);
        portalUserService.updateById(user);

        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setLastDisableTime(now);
        state.setUpdateTime(now);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, STATUS_DISABLED, nextAuthVersion, state.getProfileVersion());

        authClient.kick(userId);
        publishDisabled(userId, nextAuthVersion, reason, operatorId);
        writeAuditLog(operatorId, "DISABLE_USER", String.valueOf(userId), reason, ip);
        return ActionResult.success();
    }

    @Transactional
    public ActionResult enableUser(Long userId, Long operatorId, String ip) {
        if (userId == null) {
            return ActionResult.failure(ErrorCode.INVALID_ARGUMENT);
        }
        PortalUser user = portalUserService.getById(userId);
        if (user == null) {
            return ActionResult.failure(ErrorCode.NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        user.setStatus(STATUS_ENABLED);
        user.setDisableReason(null);
        user.setDisableTime(null);
        user.setUpdateTime(now);
        portalUserService.updateById(user);

        PortalUserAuthState state = loadOrInitAuthState(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setUpdateTime(now);
        portalUserAuthStateService.saveOrUpdate(state);
        userAuthCacheService.updateUserAuthCache(userId, STATUS_ENABLED, nextAuthVersion, state.getProfileVersion());

        authClient.kick(userId);
        publishEnabled(userId, nextAuthVersion, operatorId);
        writeAuditLog(operatorId, "ENABLE_USER", String.valueOf(userId), null, ip);
        return ActionResult.success();
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

    private void publishDisabled(Long userId, Long authVersion, String reason, Long operatorId) {
        EventMessage message = new EventMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setEventType(EVENT_USER_DISABLED);
        message.setUserId(userId);
        message.setAuthVersion(authVersion);
        message.setOperatorId(operatorId);
        message.setTs(System.currentTimeMillis());
        if (StringUtils.hasText(reason)) {
            try {
                message.setPayload(objectMapper.writeValueAsString(new DisablePayload(reason)));
            } catch (JsonProcessingException ex) {
                message.setPayload(null);
            }
        }
        eventPublisher.publish(message);
    }

    private void publishEnabled(Long userId, Long authVersion, Long operatorId) {
        EventMessage message = new EventMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setEventType(EVENT_USER_ENABLED);
        message.setUserId(userId);
        message.setAuthVersion(authVersion);
        message.setOperatorId(operatorId);
        message.setTs(System.currentTimeMillis());
        eventPublisher.publish(message);
    }

    private void writeAuditLog(Long operatorId, String action, String resource, String detail, String ip) {
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

    public static class PageResult {
        private final long total;
        private final int page;
        private final int size;
        private final List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> users;

        public PageResult(long total, int page, int size,
                          List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> users) {
            this.total = total;
            this.page = page;
            this.size = size;
            this.users = users;
        }

        public long getTotal() {
            return total;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public List<com.example.mngsys.portal.controller.AdminUserController.UserSummary> getUsers() {
            return users;
        }
    }

    public static class UserDetailResult {
        private final boolean success;
        private final ErrorCode errorCode;
        private final com.example.mngsys.portal.controller.AdminUserController.UserDetail detail;

        private UserDetailResult(boolean success, ErrorCode errorCode,
                                 com.example.mngsys.portal.controller.AdminUserController.UserDetail detail) {
            this.success = success;
            this.errorCode = errorCode;
            this.detail = detail;
        }

        public static UserDetailResult success(
                com.example.mngsys.portal.controller.AdminUserController.UserDetail detail) {
            return new UserDetailResult(true, null, detail);
        }

        public static UserDetailResult failure(ErrorCode errorCode) {
            return new UserDetailResult(false, errorCode, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public com.example.mngsys.portal.controller.AdminUserController.UserDetail getDetail() {
            return detail;
        }
    }

    public static class ActionResult {
        private final boolean success;
        private final ErrorCode errorCode;

        private ActionResult(boolean success, ErrorCode errorCode) {
            this.success = success;
            this.errorCode = errorCode;
        }

        public static ActionResult success() {
            return new ActionResult(true, null);
        }

        public static ActionResult failure(ErrorCode errorCode) {
            return new ActionResult(false, errorCode);
        }

        public boolean isSuccess() {
            return success;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    private static class DisablePayload {
        private final String reason;

        private DisablePayload(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
