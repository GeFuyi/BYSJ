package com.community.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SmsCodeRequest {

    private String schemeName;

    @Pattern(regexp = "^\\d{1,6}$", message = "格式不正确")
    private String countryCode;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "格式不正确")
    private String phoneNumber;

    private String signName;
    private String templateCode;
    private String templateParam;
    private String smsUpExtendCode;
    private String outId;

    @Min(value = 4, message = "数值过小")
    @Max(value = 8, message = "数值过大")
    private Long codeLength;

    @Min(value = 60, message = "数值过小")
    @Max(value = 1800, message = "数值过大")
    private Long validTime;

    private Long duplicatePolicy;

    @Min(value = 0, message = "数值过小")
    @Max(value = 600, message = "数值过大")
    private Long interval;

    private Long codeType;
    private Boolean returnVerifyCode;
    private Boolean autoRetry;
}
