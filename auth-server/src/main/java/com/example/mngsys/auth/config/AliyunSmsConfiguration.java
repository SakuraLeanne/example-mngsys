package com.example.mngsys.auth.config;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AliyunSmsConfiguration。
 * <p>
 * 提供阿里云短信客户端 Bean。
 * </p>
 */
@Configuration
public class AliyunSmsConfiguration {

    @Bean
    public Client smsClient(AuthProperties authProperties) throws Exception {
        AuthProperties.SmsProperties sms = authProperties.getSms();
        Config config = new Config()
                .setAccessKeyId(sms.getAccessKeyId())
                .setAccessKeySecret(sms.getAccessKeySecret())
                .setEndpoint(sms.getEndpoint());
        return new Client(config);
    }
}
