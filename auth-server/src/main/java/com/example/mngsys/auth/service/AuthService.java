package com.example.mngsys.auth.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
/**
 * AuthService。
 */
public class AuthService {

    private final Map<String, User> users;

    public AuthService() {
        Map<String, User> seed = new HashMap<>();
        seed.put("13800000001", new User("u-admin-0001", "admin", "13800000001"));
        seed.put("13800000002", new User("u-user-0002", "user", "13800000002"));
        this.users = Collections.unmodifiableMap(seed);
    }

    public User authenticateByMobile(String mobile) {
        User user = users.get(mobile);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
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
