package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.PortalUser;
import com.example.mngsys.portal.mapper.PortalUserMapper;
import com.example.mngsys.portal.service.PortalUserService;
import org.springframework.stereotype.Service;

@Service
/**
 * PortalUserServiceImpl。
 */
public class PortalUserServiceImpl extends ServiceImpl<PortalUserMapper, PortalUser> implements PortalUserService {
}
