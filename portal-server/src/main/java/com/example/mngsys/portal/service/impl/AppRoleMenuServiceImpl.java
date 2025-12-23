package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.AppRoleMenu;
import com.example.mngsys.portal.mapper.AppRoleMenuMapper;
import com.example.mngsys.portal.service.AppRoleMenuService;
import org.springframework.stereotype.Service;

@Service
public class AppRoleMenuServiceImpl extends ServiceImpl<AppRoleMenuMapper, AppRoleMenu> implements AppRoleMenuService {
}
