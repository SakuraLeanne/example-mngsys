package com.example.mngsys.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mngsys.auth.config.AuthProperties;
import com.example.mngsys.auth.entity.AuthUser;
import com.example.mngsys.auth.mapper.AuthUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
/**
 * AuthService。
 */
public class AuthService {

    private final AuthUserMapper authUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public AuthService(AuthUserMapper authUserMapper, PasswordEncoder passwordEncoder, AuthProperties authProperties) {
        this.authUserMapper = authUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        initializeBuiltinUsersSafely();
    }

    public User authenticateByMobile(String mobile) {
        AuthUser authUser = findByMobile(mobile);
        if (authUser != null) {
            validateUserStatus(authUser);
            return toUser(authUser);
        }
        if (!authProperties.isAutoCreateUser()) {
            throw new IllegalArgumentException("用户不存在");
        }
        authUser = createAndPersistUser(mobile);
        return toUser(authUser);
    }

    public User authenticateByUsernameAndPassword(String username, String password) {
        AuthUser authUser = findByUsername(username);
        if (authUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        validateUserStatus(authUser);
        if (!StringUtils.hasText(authUser.getPassword()) || !passwordEncoder.matches(password, authUser.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return toUser(authUser);
    }

    private AuthUser findByMobile(String mobile) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getMobile, mobile).last("LIMIT 1");
        return authUserMapper.selectOne(wrapper);
    }

    private AuthUser findByUsername(String username) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getUsername, username).last("LIMIT 1");
        return authUserMapper.selectOne(wrapper);
    }

    private AuthUser createAndPersistUser(String mobile) {
        AuthUser authUser = newUserWithDefaults(mobile, mobile);
        authUser.setPassword(generateRandomPassword());
        authUserMapper.insert(authUser);
        return authUser;
    }

    private String generateRandomPassword() {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    private AuthUser newUserWithDefaults(String username, String mobile) {
        AuthUser authUser = new AuthUser();
        authUser.setUsername(username);
        authUser.setMobile(mobile);
        authUser.setMobileVerified(1);
        authUser.setEmailVerified(0);
        authUser.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        authUser.setCreateTime(now);
        authUser.setUpdateTime(now);
        return authUser;
    }

    private void initializeBuiltinUsers() {
        registerBuiltinUser("admin", "13800000001", "admin123456");
        registerBuiltinUser("user", "13800000002", "user123456");
    }

    private void initializeBuiltinUsersSafely() {
        try {
            initializeBuiltinUsers();
        } catch (Exception ex) {
            log.warn("Skip initializing builtin users because database is unavailable: {}", ex.getMessage());
        }
    }

    private void registerBuiltinUser(String username, String mobile, String rawPassword) {
        if (findByUsername(username) != null || findByMobile(mobile) != null) {
            return;
        }
        AuthUser authUser = newUserWithDefaults(username, mobile);
        authUser.setPassword(passwordEncoder.encode(rawPassword));
        authUserMapper.insert(authUser);
    }

    private void validateUserStatus(AuthUser authUser) {
        Integer status = authUser.getStatus();
        if (status == null || status == 0) {
            throw new IllegalArgumentException("用户已禁用");
        }
        if (status == 2) {
            throw new IllegalArgumentException("用户已冻结");
        }
    }

    private User toUser(AuthUser authUser) {
        return new User(
                authUser.getId(),
                authUser.getUsername(),
                authUser.getMobile(),
                authUser.getPassword(),
                authUser.getRealName(),
                authUser.getStatus());
    }

    public static class User {
        private final String userId;
        private final String username;
        private final String mobile;
        private final String password;
        private final String realName;
        private final Integer status;

        public User(String userId, String username, String mobile, String password, String realName, Integer status) {
            this.userId = userId;
            this.username = username;
            this.mobile = mobile;
            this.password = password;
            this.realName = realName;
            this.status = status;
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

        public String getRealName() {
            return realName;
        }

        public Integer getStatus() {
            return status;
        }

        public boolean passwordMatches(String rawPassword, PasswordEncoder passwordEncoder) {
            return StringUtils.hasText(password) && passwordEncoder.matches(rawPassword, password);
        }
    }
}
