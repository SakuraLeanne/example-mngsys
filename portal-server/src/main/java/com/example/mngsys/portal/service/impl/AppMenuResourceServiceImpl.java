package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.AppMenuResource;
import com.example.mngsys.portal.mapper.AppMenuResourceMapper;
import com.example.mngsys.portal.service.AppMenuResourceService;
import org.springframework.stereotype.Service;

@Service
/**
 * AppMenuResourceServiceImpl。
 */
public class AppMenuResourceServiceImpl extends ServiceImpl<AppMenuResourceMapper, AppMenuResource>
        implements AppMenuResourceService {
}
