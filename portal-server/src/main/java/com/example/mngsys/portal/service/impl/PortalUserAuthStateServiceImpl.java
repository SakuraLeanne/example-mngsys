package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.entity.PortalUserAuthState;
import com.example.mngsys.portal.mapper.PortalUserAuthStateMapper;
import com.example.mngsys.portal.service.PortalUserAuthStateService;
import com.example.mngsys.portal.service.PortalUserService;
import com.example.mngsys.portal.service.UserAuthCacheService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PortalUserAuthStateServiceImpl extends ServiceImpl<PortalUserAuthStateMapper, PortalUserAuthState>
        implements PortalUserAuthStateService {

    private final UserAuthCacheService userAuthCacheService;
    private final PortalUserService portalUserService;

    public PortalUserAuthStateServiceImpl(UserAuthCacheService userAuthCacheService,
                                          PortalUserService portalUserService) {
        this.userAuthCacheService = userAuthCacheService;
        this.portalUserService = portalUserService;
    }

    @Override
    public void recordPasswordChange(Long userId) {
        if (userId == null) {
            return;
        }
        PortalUserAuthState state = loadOrInit(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setLastPwdChangeTime(LocalDateTime.now());
        if (saveOrUpdate(state)) {
            Integer status = resolveStatus(userId);
            userAuthCacheService.updateUserAuthCache(userId, status, nextAuthVersion, state.getProfileVersion());
        }
    }

    @Override
    public void recordProfileUpdate(Long userId) {
        if (userId == null) {
            return;
        }
        PortalUserAuthState state = loadOrInit(userId);
        Long nextProfileVersion = nextVersion(state.getProfileVersion());
        state.setProfileVersion(nextProfileVersion);
        state.setLastProfileUpdateTime(LocalDateTime.now());
        if (saveOrUpdate(state)) {
            Integer status = resolveStatus(userId);
            userAuthCacheService.updateUserAuthCache(userId, status, state.getAuthVersion(), nextProfileVersion);
        }
    }

    @Override
    public void recordStatusChange(Long userId, Integer status) {
        if (userId == null) {
            return;
        }
        PortalUserAuthState state = loadOrInit(userId);
        Long nextAuthVersion = nextVersion(state.getAuthVersion());
        state.setAuthVersion(nextAuthVersion);
        state.setLastDisableTime(LocalDateTime.now());
        if (saveOrUpdate(state)) {
            userAuthCacheService.updateUserAuthCache(userId, status, nextAuthVersion, state.getProfileVersion());
        }
    }

    private PortalUserAuthState loadOrInit(Long userId) {
        PortalUserAuthState state = getById(userId);
        if (state == null) {
            state = new PortalUserAuthState();
            state.setUserId(userId);
            state.setAuthVersion(1L);
            state.setProfileVersion(1L);
        }
        return state;
    }

    private Long nextVersion(Long current) {
        long base = current == null ? 1L : current;
        return base + 1;
    }

    private Integer resolveStatus(Long userId) {
        PortalUser user = portalUserService.getById(userId);
        return user == null ? null : user.getStatus();
    }
}
