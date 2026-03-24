package com.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsLoginProperties {

    private String accessKeyId;
    private String accessKeySecret;

    private String region = "ap-southeast-1";
    private String endpoint = "dypnsapi.aliyuncs.com";

    private String schemeName = "测试方案";
    private String defaultCountryCode = "86";
    private String defaultPhoneNumber = "15138114047";

    private String signName;
    private String templateCode;
    private String templateParam = "{\"code\":\"##code##\",\"min\":\"5\"}";
    private String smsUpExtendCode;

    private long codeLength = 6;
    private long validTime = 300;
    private long duplicatePolicy = 1;
    private long intervalSeconds = 60;
    private long codeType = 1;
    private boolean returnVerifyCode = false;
    private long caseAuthPolicy = 1;
}
