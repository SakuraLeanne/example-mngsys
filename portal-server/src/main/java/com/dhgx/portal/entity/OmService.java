package com.dhgx.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("tb_om_service")
public class OmService {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceId;
    private String serviceName;
    private String serviceNum;
    private String serviceChannel;
    private String serviceJumpAddress;
    private String status;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceNum() {
        return serviceNum;
    }

    public void setServiceNum(String serviceNum) {
        this.serviceNum = serviceNum;
    }

    public String getServiceChannel() {
        return serviceChannel;
    }

    public void setServiceChannel(String serviceChannel) {
        this.serviceChannel = serviceChannel;
    }

    public String getServiceJumpAddress() {
        return serviceJumpAddress;
    }

    public void setServiceJumpAddress(String serviceJumpAddress) {
        this.serviceJumpAddress = serviceJumpAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
