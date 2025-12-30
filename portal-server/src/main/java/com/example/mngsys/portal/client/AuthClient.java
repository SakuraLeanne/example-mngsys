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

    public ApiResponse<LoginResponse> login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        return authFeignClient.login(request);
    }

    public ResponseEntity<ApiResponse> loginWithResponse(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        return exchangeSafely(() -> authFeignClient.login(request));
    }

    public ApiResponse<Void> logout() {
        return authFeignClient.logout(null);
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
        private String username;
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class LoginResponse {
        private String userId;
        private String username;

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
}
