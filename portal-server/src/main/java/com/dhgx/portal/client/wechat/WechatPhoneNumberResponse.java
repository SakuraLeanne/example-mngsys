package com.dhgx.portal.client.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 微信手机号换取接口返回体。
 */
public class WechatPhoneNumberResponse {

    private Integer errcode;
    private String errmsg;
    @JsonProperty("phone_info")
    private PhoneInfo phoneInfo;

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public PhoneInfo getPhoneInfo() {
        return phoneInfo;
    }

    public void setPhoneInfo(PhoneInfo phoneInfo) {
        this.phoneInfo = phoneInfo;
    }

    public static class PhoneInfo {
        private String phoneNumber;
        private String purePhoneNumber;
        private String countryCode;
        private Watermark watermark;

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getPurePhoneNumber() {
            return purePhoneNumber;
        }

        public void setPurePhoneNumber(String purePhoneNumber) {
            this.purePhoneNumber = purePhoneNumber;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public Watermark getWatermark() {
            return watermark;
        }

        public void setWatermark(Watermark watermark) {
            this.watermark = watermark;
        }
    }

    public static class Watermark {
        private String appid;
        private Long timestamp;

        public String getAppid() {
            return appid;
        }

        public void setAppid(String appid) {
            this.appid = appid;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
