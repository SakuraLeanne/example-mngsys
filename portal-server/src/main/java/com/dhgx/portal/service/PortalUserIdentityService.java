package com.dhgx.portal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dhgx.portal.entity.PortalUserIdentity;

/**
 * PortalUserIdentityService。
 */
public interface PortalUserIdentityService extends IService<PortalUserIdentity> {

    PortalUserIdentity findMiniProgramOpenId(String openId);

    /**
     * 按 provider/type/key 查询已绑定身份。
     */
    PortalUserIdentity findByIdentity(String provider, String identityType, String identityKey);
}
