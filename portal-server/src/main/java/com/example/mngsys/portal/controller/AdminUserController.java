package com.example.mngsys.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mngsys.common.api.PageResponse;
import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.security.AdminRequired;
import com.example.mngsys.portal.service.PortalAdminUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 管理端用户控制器，提供用户查询、启用/禁用等接口。
 */
@RestController
@RequestMapping("/admin/users")
@Validated
@AdminRequired
public class AdminUserController {

    /**
     * 管理端用户服务，处理用户相关业务。
     */
    private final PortalAdminUserService portalAdminUserService;

    /**
     * 构造函数，注入用户服务。
     *
     * @param portalAdminUserService 用户业务服务
     */
    public AdminUserController(PortalAdminUserService portalAdminUserService) {
        this.portalAdminUserService = portalAdminUserService;
    }

    /**
     * 分页查询用户列表。
     *
     * @param page    页码
     * @param size    页大小
     * @param keyword 关键词
     * @param status  用户状态
     * @return 用户分页数据
     */
    @GetMapping
    public ApiResponse<PageResponse<PortalUser>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer status) {
        Page<PortalUser> result = portalAdminUserService.listUsers(page, size, keyword, status);
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
        PortalAdminUserService.UserDetailResult result = portalAdminUserService.getUserDetail(userId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(result.getDetail());
    }

    /**
     * 更新指定用户启用状态。
     *
     * @param userId  用户 ID
     * @param request 状态更新请求体
     * @return 操作结果
     */
    @PostMapping("/{userId}/status")
    public ApiResponse<ActionResponse> updateUserStatus(@PathVariable String userId,
                                                        @Valid @RequestBody StatusRequest request) {
        String operatorId = RequestContext.getUserId();
        PortalAdminUserService.ActionResult result = portalAdminUserService.updateUserStatus(
                userId,
                request.getEnabled(),
                operatorId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new ActionResponse(true));
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
     * 通用操作响应。
     */
    public static class ActionResponse {
        /**
         * 是否成功。
         */
        private final boolean success;

        public ActionResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
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
