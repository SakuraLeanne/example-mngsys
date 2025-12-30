package com.example.mngsys.auth.service;

import com.example.mngsys.auth.config.AuthProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * AuthService。
 */
public class AuthService {

    private final Map<String, User> users;
    private final AuthProperties authProperties;

    public AuthService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        Map<String, User> seed = new ConcurrentHashMap<>();
        seed.put("13800000001", new User("u-admin-0001", "admin", "13800000001"));
        seed.put("13800000002", new User("u-user-0002", "user", "13800000002"));
        this.users = seed;
    }

    public User authenticateByMobile(String mobile) {
        User user = users.get(mobile);
        if (user != null) {
            return user;
        }
        if (!authProperties.isAutoCreateUser()) {
            throw new IllegalArgumentException("用户不存在");
        }
        return users.computeIfAbsent(mobile, this::createUser);
    }

    private User createUser(String mobile) {
        String userId = "u-" + mobile;
        return new User(userId, mobile, mobile);
    }

    public static class User {
        private final String userId;
        private final String username;
        private final String mobile;

        public User(String userId, String username, String mobile) {
            this.userId = userId;
            this.username = username;
            this.mobile = mobile;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getMobile() {
            return mobile;
        }
    }
}
