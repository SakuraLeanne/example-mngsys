package com.dhgx.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dhgx.portal.entity.AppMenuResource;
import com.dhgx.portal.mapper.AppMenuResourceMapper;
import com.dhgx.portal.service.AppMenuResourceService;
import org.springframework.stereotype.Service;

@Service
/**
 * AppMenuResourceServiceImpl。
 */
public class AppMenuResourceServiceImpl extends ServiceImpl<AppMenuResourceMapper, AppMenuResource>
        implements AppMenuResourceService {
}
