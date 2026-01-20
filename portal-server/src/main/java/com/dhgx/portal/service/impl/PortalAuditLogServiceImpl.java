package com.dhgx.portal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dhgx.portal.entity.PortalAuditLog;
import com.dhgx.portal.mapper.PortalAuditLogMapper;
import com.dhgx.portal.service.PortalAuditLogService;
import org.springframework.stereotype.Service;

@Service
/**
 * PortalAuditLogServiceImpl。
 */
public class PortalAuditLogServiceImpl extends ServiceImpl<PortalAuditLogMapper, PortalAuditLog>
        implements PortalAuditLogService {
}
