package com.dhgx.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dhgx.portal.entity.AppUserRole;
import com.dhgx.portal.mapper.AppUserRoleMapper;
import com.dhgx.portal.service.AppUserRoleService;
import org.springframework.stereotype.Service;

@Service
/**
 * AppUserRoleServiceImpl。
 */
public class AppUserRoleServiceImpl extends ServiceImpl<AppUserRoleMapper, AppUserRole> implements AppUserRoleService {
}
