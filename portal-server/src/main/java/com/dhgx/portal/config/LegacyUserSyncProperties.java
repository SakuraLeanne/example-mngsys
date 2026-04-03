package com.dhgx.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 外部账号/人员接口同步配置。
 */
@Component
@ConfigurationProperties(prefix = "portal.sync.user")
public class LegacyUserSyncProperties {

    private String baseUrl = "http://md.elht.com:9090";
    private String accountPath = "/default/public/api/rest/esbmule/services/query/DH_SELECT_ZH";
    private String empPath = "/default/public/api/rest/emp/queryByCode";
    private String accountUsercode = "dhgxjkyh";
    private String accountPassword = "rBZQ5*q6AV";
    private String empUsercode = "dhgxjkyh";
    private String empPassword = "aBZQ6@q6AV";
    private int pageSize = 50;
    private long empRequestIntervalMs = 300L;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAccountPath() {
        return accountPath;
    }

    public void setAccountPath(String accountPath) {
        this.accountPath = accountPath;
    }

    public String getEmpPath() {
        return empPath;
    }

    public void setEmpPath(String empPath) {
        this.empPath = empPath;
    }

    public String getAccountUsercode() {
        return accountUsercode;
    }

    public void setAccountUsercode(String accountUsercode) {
        this.accountUsercode = accountUsercode;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }

    public String getEmpUsercode() {
        return empUsercode;
    }

    public void setEmpUsercode(String empUsercode) {
        this.empUsercode = empUsercode;
    }

    public String getEmpPassword() {
        return empPassword;
    }

    public void setEmpPassword(String empPassword) {
        this.empPassword = empPassword;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getEmpRequestIntervalMs() {
        return empRequestIntervalMs;
    }

    public void setEmpRequestIntervalMs(long empRequestIntervalMs) {
        this.empRequestIntervalMs = empRequestIntervalMs;
    }
}
