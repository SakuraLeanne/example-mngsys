package com.dhgx.portal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dhgx.portal.entity.PortalUserIdentity;

/**
 * PortalUserIdentityService。
 */
public interface PortalUserIdentityService extends IService<PortalUserIdentity> {

    PortalUserIdentity findMiniProgramOpenId(String openId);
}
