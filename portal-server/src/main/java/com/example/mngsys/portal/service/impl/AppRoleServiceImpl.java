package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.AppRole;
import com.example.mngsys.portal.mapper.AppRoleMapper;
import com.example.mngsys.portal.service.AppRoleService;
import org.springframework.stereotype.Service;

@Service
public class AppRoleServiceImpl extends ServiceImpl<AppRoleMapper, AppRole> implements AppRoleService {
}
