package com.example.mngsys.portal.client;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.config.AuthClientProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;
import feign.FeignException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.function.Supplier;

@Component
/**
 * AuthClient。
 */
public class AuthClient {

    private final AuthFeignClient authFeignClient;
    private final AuthClientProperties authClientProperties;
    private final ObjectMapper objectMapper;

    public AuthClient(AuthFeignClient authFeignClient, AuthClientProperties authClientProperties, ObjectMapper objectMapper) {
        this.authFeignClient = authFeignClient;
        this.authClientProperties = authClientProperties;
        this.objectMapper = objectMapper;
    }

    public ApiResponse<LoginResponse> login(String mobile, String code) {
        LoginRequest request = new LoginRequest(mobile, code);
        return authFeignClient.login(request);
    }

    public ResponseEntity<ApiResponse> loginWithResponse(String mobile, String code) {
        LoginRequest request = new LoginRequest(mobile, code);
        return exchangeSafely(() -> authFeignClient.login(request));
    }

    public ApiResponse<Void> logout() {
        return authFeignClient.logout(null);
    }

    public ApiResponse<Void> sendLoginSms(String mobile) {
        SmsSendRequest request = new SmsSendRequest(mobile);
        return authFeignClient.sendSms(request);
    }

    public ResponseEntity<ApiResponse> logoutWithResponse(String cookie) {
        return exchangeSafely(() -> authFeignClient.logout(cookie));
    }

    public ApiResponse<SessionResponse> sessionMe() {
        return authFeignClient.sessionMe(null);
    }

    public ResponseEntity<ApiResponse> sessionMe(String cookie) {
        return exchangeSafely(() -> authFeignClient.sessionMe(cookie));
    }

    public ApiResponse<Void> kick(String userId) {
        KickRequest request = new KickRequest(userId);
        return authFeignClient.kick(authClientProperties.getInternalToken(), request);
    }

    private ResponseEntity<ApiResponse> exchangeSafely(Supplier<ApiResponse> supplier) {
        try {
            ApiResponse response = supplier.get();
            return ResponseEntity.ok(response);
        } catch (FeignException ex) {
            ApiResponse response = parseErrorResponse(ex.contentUTF8());
            return ResponseEntity.status(ex.status()).body(response);
        }
    }

    private ApiResponse parseErrorResponse(String body) {
        if (!StringUtils.hasText(body)) {
            return ApiResponse.failure(com.example.mngsys.portal.common.api.ErrorCode.INTERNAL_ERROR, "调用鉴权服务失败");
        }
        try {
            return objectMapper.readValue(body, ApiResponse.class);
        } catch (JsonProcessingException ex) {
            return ApiResponse.failure(com.example.mngsys.portal.common.api.ErrorCode.INTERNAL_ERROR, "调用鉴权服务失败");
        }
    }

    public static class LoginRequest {
        private String mobile;
        private String code;

        public LoginRequest() {
        }

        public LoginRequest(String mobile, String code) {
            this.mobile = mobile;
            this.code = code;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class LoginResponse {
        private String userId;
        private String username;
        private String mobile;
        private String realName;
        private String satoken;
        private Long loginTime;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getSatoken() {
            return satoken;
        }

        public void setSatoken(String satoken) {
            this.satoken = satoken;
        }

        public Long getLoginTime() {
            return loginTime;
        }

        public void setLoginTime(Long loginTime) {
            this.loginTime = loginTime;
        }
    }

    public static class SessionResponse {
        private String userId;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    public static class KickRequest {
        private String userId;

        public KickRequest() {
        }

        public KickRequest(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }

    public static class SmsSendRequest {
        private String mobile;

        public SmsSendRequest() {
        }

        public SmsSendRequest(String mobile) {
            this.mobile = mobile;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
    }
}
