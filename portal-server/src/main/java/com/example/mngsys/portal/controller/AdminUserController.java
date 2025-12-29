package com.example.mngsys.portal.controller;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.common.context.RequestContext;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户控制器，提供用户查询、启用/禁用等接口。
 */
@RestController
@RequestMapping("/portal/api/admin/users")
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
    public ApiResponse<PageResponse<UserSummary>> listUsers(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Integer status) {
        PortalAdminUserService.PageResult result = portalAdminUserService.listUsers(page, size, keyword, status);
        PageResponse<UserSummary> response = new PageResponse<>(result.getTotal(), result.getPage(), result.getSize(),
                result.getUsers());
        return ApiResponse.success(response);
    }

    /**
     * 查询用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详细信息
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserDetail> getUser(@PathVariable String userId) {
        PortalAdminUserService.UserDetailResult result = portalAdminUserService.getUserDetail(userId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(result.getDetail());
    }

    /**
     * 禁用指定用户。
     *
     * @param userId             用户 ID
     * @param request            禁用原因请求体
     * @param httpServletRequest HTTP 请求，用于解析操作者 IP
     * @return 操作结果
     */
    @PostMapping("/{userId}/disable")
    public ApiResponse<ActionResponse> disableUser(@PathVariable String userId,
                                                   @Valid @RequestBody DisableRequest request,
                                                   HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminUserService.ActionResult result = portalAdminUserService.disableUser(
                userId,
                request.getReason(),
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 启用指定用户。
     *
     * @param userId             用户 ID
     * @param httpServletRequest HTTP 请求，用于解析操作者 IP
     * @return 操作结果
     */
    @PostMapping("/{userId}/enable")
    public ApiResponse<ActionResponse> enableUser(@PathVariable String userId,
                                                  HttpServletRequest httpServletRequest) {
        String operatorId = RequestContext.getUserId();
        PortalAdminUserService.ActionResult result = portalAdminUserService.enableUser(
                userId,
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

    /**
     * 从请求中解析操作者 IP。
     *
     * @param request HTTP 请求
     * @return IP 地址
     */
    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 禁用请求体，包含禁用原因。
     */
    public static class DisableRequest {
        /**
         * 禁用原因。
         */
        @NotBlank(message = "reason 不能为空")
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
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
     * 分页响应包装，包含记录总数和数据列表。
     *
     * @param <T> 数据类型
     */
    public static class PageResponse<T> {
        /**
         * 总记录数。
         */
        private final long total;
        /**
         * 当前页码。
         */
        private final int page;
        /**
         * 每页大小。
         */
        private final int size;
        /**
         * 当前页记录列表。
         */
        private final List<T> records;

        public PageResponse(long total, int page, int size, List<T> records) {
            this.total = total;
            this.page = page;
            this.size = size;
            this.records = records;
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

        public List<T> getRecords() {
            return records;
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
