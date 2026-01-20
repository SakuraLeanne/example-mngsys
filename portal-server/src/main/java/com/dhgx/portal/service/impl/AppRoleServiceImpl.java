package com.dhgx.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dhgx.portal.entity.AppRole;
import com.dhgx.portal.mapper.AppRoleMapper;
import com.dhgx.portal.service.AppRoleService;
import org.springframework.stereotype.Service;

@Service
/**
 * AppRoleServiceImpl。
 */
public class AppRoleServiceImpl extends ServiceImpl<AppRoleMapper, AppRole> implements AppRoleService {
}
