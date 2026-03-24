package com.community.dto;

import lombok.Data;

@Data
public class SmsCodeSendResponse {

    private String schemeName;
    private String countryCode;
    private String phoneNumber;
    private String outId;
    private Integer codeLength;
    private Integer validMinutes;
    private String verifyCode;
    private String bizId;
    private String requestId;
    private String providerCode;
    private String providerMessage;
}
