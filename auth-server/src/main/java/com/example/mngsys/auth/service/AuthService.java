package com.example.mngsys.auth.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final Map<String, User> users;

    public AuthService() {
        Map<String, User> seed = new HashMap<>();
        seed.put("admin", new User(1L, "admin", "admin"));
        seed.put("user", new User(1001L, "user", "user"));
        this.users = Collections.unmodifiableMap(seed);
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null || !user.getPassword().equals(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }

    public static class User {
        private final Long userId;
        private final String username;
        private final String password;

        public User(Long userId, String username, String password) {
            this.userId = userId;
            this.username = username;
            this.password = password;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
