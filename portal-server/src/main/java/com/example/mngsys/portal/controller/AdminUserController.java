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

@RestController
@RequestMapping("/portal/api/admin/users")
@Validated
@AdminRequired
/**
 * AdminUserController。
 */
public class AdminUserController {

    private final PortalAdminUserService portalAdminUserService;

    public AdminUserController(PortalAdminUserService portalAdminUserService) {
        this.portalAdminUserService = portalAdminUserService;
    }

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

    @GetMapping("/{userId}")
    public ApiResponse<UserDetail> getUser(@PathVariable Long userId) {
        PortalAdminUserService.UserDetailResult result = portalAdminUserService.getUserDetail(userId);
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(result.getDetail());
    }

    @PostMapping("/{userId}/disable")
    public ApiResponse<ActionResponse> disableUser(@PathVariable Long userId,
                                                   @Valid @RequestBody DisableRequest request,
                                                   HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
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

    @PostMapping("/{userId}/enable")
    public ApiResponse<ActionResponse> enableUser(@PathVariable Long userId,
                                                  HttpServletRequest httpServletRequest) {
        Long operatorId = RequestContext.getUserId();
        PortalAdminUserService.ActionResult result = portalAdminUserService.enableUser(
                userId,
                operatorId,
                resolveIp(httpServletRequest));
        if (!result.isSuccess()) {
            return ApiResponse.failure(result.getErrorCode());
        }
        return ApiResponse.success(new ActionResponse(true));
    }

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

    public static class DisableRequest {
        @NotBlank(message = "reason 不能为空")
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ActionResponse {
        private final boolean success;

        public ActionResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    public static class PageResponse<T> {
        private final long total;
        private final int page;
        private final int size;
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

    public static class UserSummary {
        private final Long id;
        private final String username;
        private final String realName;
        private final String mobile;
        private final String email;
        private final Integer status;
        private final LocalDateTime disableTime;

        public UserSummary(Long id, String username, String realName, String mobile, String email,
                           Integer status, LocalDateTime disableTime) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
            this.disableTime = disableTime;
        }

        public Long getId() {
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

        public LocalDateTime getDisableTime() {
            return disableTime;
        }
    }

    public static class UserDetail {
        private final Long id;
        private final String username;
        private final String realName;
        private final String mobile;
        private final String email;
        private final Integer status;
        private final String disableReason;
        private final LocalDateTime disableTime;
        private final LocalDateTime createTime;

        public UserDetail(Long id, String username, String realName, String mobile, String email,
                          Integer status, String disableReason, LocalDateTime disableTime,
                          LocalDateTime createTime) {
            this.id = id;
            this.username = username;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.status = status;
            this.disableReason = disableReason;
            this.disableTime = disableTime;
            this.createTime = createTime;
        }

        public Long getId() {
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

        public String getDisableReason() {
            return disableReason;
        }

        public LocalDateTime getDisableTime() {
            return disableTime;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }
    }
}
