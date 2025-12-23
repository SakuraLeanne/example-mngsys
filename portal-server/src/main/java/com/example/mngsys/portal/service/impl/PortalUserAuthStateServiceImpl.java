package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.PortalUserAuthState;
import com.example.mngsys.portal.mapper.PortalUserAuthStateMapper;
import com.example.mngsys.portal.service.PortalUserAuthStateService;
import org.springframework.stereotype.Service;

@Service
public class PortalUserAuthStateServiceImpl extends ServiceImpl<PortalUserAuthStateMapper, PortalUserAuthState>
        implements PortalUserAuthStateService {
}
