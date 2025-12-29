package com.example.mngsys.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public AuthService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        Map<String, User> seed = new HashMap<>();
        String demoHash = "{bcrypt}$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5Cw5IV/pY5PaaC2l5x4pnW5sA8vz";
        seed.put("admin", new User("u-admin-0001", "admin", demoHash));
        seed.put("user", new User("u-user-0002", "user", demoHash));
        this.users = Collections.unmodifiableMap(seed);
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }

    public static class User {
        private final String userId;
        private final String username;
        private final String password;

        public User(String userId, String username, String password) {
            this.userId = userId;
            this.username = username;
            this.password = password;
        }

        public String getUserId() {
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
