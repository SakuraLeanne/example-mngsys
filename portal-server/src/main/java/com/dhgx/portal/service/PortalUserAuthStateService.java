package com.dhgx.portal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dhgx.portal.entity.PortalUserAuthState;

/**
 * PortalUserAuthStateService。
 */
public interface PortalUserAuthStateService extends IService<PortalUserAuthState> {
    void recordPasswordChange(String userId);

    void recordProfileUpdate(String userId);

    void recordStatusChange(String userId, Integer status);
}
