package com.example.mngsys.portal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.mngsys.portal.entity.PortalUserAuthState;

public interface PortalUserAuthStateService extends IService<PortalUserAuthState> {
    void recordPasswordChange(Long userId);

    void recordProfileUpdate(Long userId);

    void recordStatusChange(Long userId, Integer status);
}
