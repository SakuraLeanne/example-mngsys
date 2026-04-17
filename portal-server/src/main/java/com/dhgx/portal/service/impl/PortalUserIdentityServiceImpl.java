package com.dhgx.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dhgx.portal.entity.PortalUserIdentity;
import com.dhgx.portal.mapper.PortalUserIdentityMapper;
import com.dhgx.portal.service.PortalUserIdentityService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * PortalUserIdentityServiceImpl。
 */
@Service
public class PortalUserIdentityServiceImpl extends ServiceImpl<PortalUserIdentityMapper, PortalUserIdentity>
        implements PortalUserIdentityService {

    private static final String PROVIDER_WECHAT_MINI_PROGRAM = "WECHAT_MINI_PROGRAM";
    private static final String TYPE_OPENID = "OPENID";

    @Override
    public PortalUserIdentity findMiniProgramOpenId(String openId) {
        if (!StringUtils.hasText(openId)) {
            return null;
        }
        LambdaQueryWrapper<PortalUserIdentity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalUserIdentity::getIdentityProvider, PROVIDER_WECHAT_MINI_PROGRAM)
                .eq(PortalUserIdentity::getIdentityType, TYPE_OPENID)
                .eq(PortalUserIdentity::getIdentityKey, openId)
                .eq(PortalUserIdentity::getBindStatus, 1)
                .last("LIMIT 1");
        return getOne(wrapper, false);
    }
}
