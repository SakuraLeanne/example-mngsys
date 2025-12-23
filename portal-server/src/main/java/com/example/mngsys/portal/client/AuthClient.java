package com.example.mngsys.portal.client;

import com.example.mngsys.portal.common.api.ApiResponse;
import com.example.mngsys.portal.config.AuthClientProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthClient {

    private final RestTemplate restTemplate;
    private final AuthClientProperties authClientProperties;

    public AuthClient(RestTemplate restTemplate, AuthClientProperties authClientProperties) {
        this.restTemplate = restTemplate;
        this.authClientProperties = authClientProperties;
    }

    public ApiResponse<LoginResponse> login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        return restTemplate.postForEntity(buildUrl("/auth/api/login"), request, ApiResponse.class).getBody();
    }

    public ApiResponse<Void> logout() {
        return restTemplate.postForEntity(buildUrl("/auth/api/logout"), null, ApiResponse.class).getBody();
    }

    public ApiResponse<SessionResponse> sessionMe() {
        return restTemplate.getForEntity(buildUrl("/auth/api/session/me"), ApiResponse.class).getBody();
    }

    public ApiResponse<Void> kick(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Token", authClientProperties.getInternalToken());
        KickRequest request = new KickRequest(userId);
        HttpEntity<KickRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                buildUrl("/auth/api/session/kick"),
                HttpMethod.POST,
                entity,
                ApiResponse.class);
        return response.getBody();
    }

    private String buildUrl(String path) {
        String baseUrl = authClientProperties.getServerBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
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
        private Long userId;
        private String username;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
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
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }

    public static class KickRequest {
        private Long userId;

        public KickRequest() {
        }

        public KickRequest(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
