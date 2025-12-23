package com.example.mngsys.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.mngsys.portal.entity.PortalAuditLog;
import com.example.mngsys.portal.mapper.PortalAuditLogMapper;
import com.example.mngsys.portal.service.PortalAuditLogService;
import org.springframework.stereotype.Service;

@Service
public class PortalAuditLogServiceImpl extends ServiceImpl<PortalAuditLogMapper, PortalAuditLog>
        implements PortalAuditLogService {
}
