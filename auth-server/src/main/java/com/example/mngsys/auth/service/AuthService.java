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

    private final Map<String, User> usersByMobile;
    private final Map<String, User> usersByUsername;
    private final AuthProperties authProperties;

    public AuthService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        Map<String, User> mobileIndex = new ConcurrentHashMap<>();
        Map<String, User> usernameIndex = new ConcurrentHashMap<>();
        registerUser(mobileIndex, usernameIndex, new User("u-admin-0001", "admin", "13800000001", "admin123456"));
        registerUser(mobileIndex, usernameIndex, new User("u-user-0002", "user", "13800000002", "user123456"));
        this.usersByMobile = mobileIndex;
        this.usersByUsername = usernameIndex;
    }

    public User authenticateByMobile(String mobile) {
        User user = usersByMobile.get(mobile);
        if (user != null) {
            return user;
        }
        if (!authProperties.isAutoCreateUser()) {
            throw new IllegalArgumentException("用户不存在");
        }
        return usersByMobile.computeIfAbsent(mobile, key -> registerUser(createUser(key)));
    }

    public User authenticateByUsernameAndPassword(String username, String password) {
        User user = usersByUsername.get(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!user.passwordMatches(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }

    private User createUser(String mobile) {
        String userId = "u-" + mobile;
        return new User(userId, mobile, mobile, null);
    }

    private User registerUser(User user) {
        return registerUser(usersByMobile, usersByUsername, user);
    }

    private User registerUser(Map<String, User> mobileIndex, Map<String, User> usernameIndex, User user) {
        mobileIndex.put(user.getMobile(), user);
        usernameIndex.put(user.getUsername(), user);
        return user;
    }

    public static class User {
        private final String userId;
        private final String username;
        private final String mobile;
        private final String password;

        public User(String userId, String username, String mobile, String password) {
            this.userId = userId;
            this.username = username;
            this.mobile = mobile;
            this.password = password;
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

        public String getPassword() {
            return password;
        }

        public boolean passwordMatches(String rawPassword) {
            if (password == null) {
                return false;
            }
            return password.equals(rawPassword);
        }
    }
}
