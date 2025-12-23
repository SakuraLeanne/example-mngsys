package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.AppUserRole;
import com.example.mngsys.portal.mapper.AppUserRoleMapper;
import com.example.mngsys.portal.service.AppUserRoleService;
import org.springframework.stereotype.Service;

@Service
public class AppUserRoleServiceImpl extends ServiceImpl<AppUserRoleMapper, AppUserRole> implements AppUserRoleService {
}
