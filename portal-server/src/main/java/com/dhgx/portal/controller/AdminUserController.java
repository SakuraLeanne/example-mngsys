package com.dhgx.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dhgx.common.api.ActionResponse;
import com.dhgx.common.api.PageResponse;
import com.dhgx.portal.common.api.ApiResponse;
import com.dhgx.portal.common.context.RequestContext;
import com.dhgx.portal.entity.PortalUser;
import com.dhgx.portal.security.AdminRequired;
import com.dhgx.portal.service.LegacyUserSyncService;
import com.dhgx.portal.service.PortalAdminUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端用户控制器，提供用户查询、启用/禁用等接口。
 */
@RestController
@RequestMapping("/admin/users")
@Validated
public class AdminUserController {

    /**
     * 管理端用户服务，处理用户相关业务。
     */
    private final PortalAdminUserService portalAdminUserService;
    private final LegacyUserSyncService legacyUserSyncService;

    /**
     * 构造函数，注入用户服务。
     *
     * @param portalAdminUserService 用户业务服务
     */
    public AdminUserController(PortalAdminUserService portalAdminUserService,
                               LegacyUserSyncService legacyUserSyncService) {
        this.portalAdminUserService = portalAdminUserService;
        this.legacyUserSyncService = legacyUserSyncService;
    }

    /**
     * 分页查询用户列表。
     *
     * @param page    页码
     * @param size    页大小
     * @param username 用户名
     * @param mobile  手机号
     * @param status  用户状态
     * @return 用户分页数据
     */
    @GetMapping
    public ApiResponse<PageResponse<PortalUser>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) String username,
                                                           @RequestParam(required = false) String mobile,
                                                           @RequestParam(required = false) Integer status) {
        String requesterId = RequestContext.getUserId();
        Page<PortalUser> result = portalAdminUserService.listUsers(page, size, username, mobile, status, requesterId);
        PageResponse<PortalUser> response = new PageResponse<>(
                result.getTotal(),
                result.getCurrent(),
                result.getRecords().size(),
                result.getRecords()
        );
        return ApiResponse.success(response);
    }

    /**
     * 查询用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详细信息
     */
    @GetMapping("/{userId}")
    public ApiResponse<PortalUser> getUser(@PathVariable String userId) {
        String requesterId = RequestContext.getUserId();
        PortalAdminUserService.UserDetailResult result = portalAdminUserService.getUserDetail(userId, requesterId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(result.getDetail());
    }

    /**
     * 更新指定用户启用状态。
     *
     * @param userId  用户 ID
     * @param enabled 禁用false，启用true
     * @return 操作结果
     */
    @GetMapping("/status")
    @AdminRequired(scope = "portal")
    public ApiResponse<ActionResponse> updateUserStatus(@RequestParam String userId, @RequestParam @NotNull(message = "参数enabled不能为空") Boolean enabled) {
        String operatorId = RequestContext.getUserId();
        PortalAdminUserService.ActionResult result = portalAdminUserService.updateUserStatus(
                userId,
                enabled,
                operatorId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 手动触发外部系统用户同步。
     *
     * @param start    增量开始时间，可空，格式 yyyy-MM-ddTHH:mm:ss
     * @param end      增量结束时间，可空，格式 yyyy-MM-ddTHH:mm:ss
     * @param pageSize 每页数量，可空
     * @return 同步统计结果
     */
    @PostMapping("/sync/manual")
    @AdminRequired(scope = "portal")
    public ApiResponse<Map<String, Integer>> manualSyncUsers(@RequestParam(required = false) String start,
                                                             @RequestParam(required = false) String end,
                                                             @RequestParam(required = false) Integer pageSize) {
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        LegacyUserSyncService.SyncResult syncResult = legacyUserSyncService.syncUsers(startTime, endTime, pageSize);
        Map<String, Integer> result = new HashMap<>();
        result.put("apiTotal", syncResult.getApiTotal());
        result.put("apiSuccess", syncResult.getApiSuccess());
        result.put("apiFailed", syncResult.getApiFailed());
        result.put("tbTotal", syncResult.getTbTotal());
        result.put("tbInserted", syncResult.getTbInserted());
        result.put("tbUpdated", syncResult.getTbUpdated());
        result.put("tbFailed", syncResult.getTbFailed());
        result.put("tbSkippedDuplicate", syncResult.getTbSkippedDuplicate());
        result.put("tbSkippedNoMobile", syncResult.getTbSkippedNoMobile());
        result.put("empCalls", syncResult.getEmpCalls());
        result.put("mobileOverwritten", syncResult.getMobileOverwritten());
        return ApiResponse.success(result);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(value.trim());
    }

    /**
     * 用户状态更新请求体。
     */
    public static class StatusRequest {
        /**
         * 是否启用。
         */
        @NotNull(message = "enabled 不能为空")
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 用户概要信息。
     */
    public static class UserSummary {
        /**
         * 用户 ID。
         */
        private final String id;
        /**
         * 用户名。
         */
        private final String username;
        /**
         * 真实姓名。
         */
        private final String realName;
        /**
         * 手机号。
         */
        private final String mobile;
        /**
         * 邮箱。
         */
        private final String email;
        /**
         * 用户状态。
         */
        private final Integer status;
        /** 备注信息。 */
        private final String remark;

        public UserSummary(String id, String username, String realName, String mobile, String email,
                           Integer status, String remark) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
            this.remark = remark;
        }

        public String getId() {
            return id;
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

        public String getRemark() {
            return remark;
        }
    }

    /**
     * 用户详情信息。
     */
    public static class UserDetail {
        /**
         * 用户 ID。
         */
        private final String id;
        /**
         * 用户名。
         */
        private final String username;
        /**
         * 真实姓名。
         */
        private final String realName;
        /**
         * 手机号。
         */
        private final String mobile;
        /**
         * 邮箱。
         */
        private final String email;
        /**
         * 用户状态。
         */
        private final Integer status;
        /** 备注信息（包含禁用原因等）。 */
        private final String remark;
        /**
         * 创建时间。
         */
        private final LocalDateTime createTime;

        public UserDetail(String id, String username, String realName, String mobile, String email,
                          Integer status, String remark,
                          LocalDateTime createTime) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
            this.remark = remark;
            this.createTime = createTime;
        }

        public String getId() {
            return id;
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

        public String getRemark() {
            return remark;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }
}
