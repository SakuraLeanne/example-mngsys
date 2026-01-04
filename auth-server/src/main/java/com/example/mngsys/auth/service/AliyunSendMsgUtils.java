package com.example.mngsys.auth.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teautil.Common;
import com.example.mngsys.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AliyunSendMsgUtils。
 * <p>
 * 通过配置的 AK/SK 创建的客户端下发短信，避免在代码中泄露密钥。
 * </p>
 */
@Component
public class AliyunSendMsgUtils {

    private static final Logger log = LoggerFactory.getLogger(AliyunSendMsgUtils.class);

    private final Client smsClient;
    private final AuthProperties.SmsProperties smsProperties;

    public AliyunSendMsgUtils(Client smsClient, AuthProperties authProperties) {
        this.smsClient = smsClient;
        this.smsProperties = authProperties.getSms();
    }

    /**
     * 发送短信验证码。
     *
     * @param mobile 接收手机号
     * @param code   验证码
     * @param templateCode 短信模板编码
     */
    public void sendCode(String mobile, String code, String templateCode) {
        Map<String, String> templateParams = new HashMap<>();
        templateParams.put("code", code);
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(mobile)
                    .setSignName(smsProperties.getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(Common.toJSONString(templateParams));
            smsClient.sendSms(request);
        } catch (Exception ex) {
            log.error("发送短信验证码失败，mobile={}", mobile, ex);
            throw new IllegalStateException("短信发送失败，请稍后重试");
        }
    }
}
