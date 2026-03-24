package com.community.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SmsCodeRequest {

    private String schemeName;

    @Pattern(regexp = "^\\d{1,6}$", message = "国家编码格式不正确")
    private String countryCode;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    private String phoneNumber;

    private String signName;
    private String templateCode;
    private String templateParam;
    private String smsUpExtendCode;
    private String outId;

    @Min(value = 4, message = "验证码长度最小为4")
    @Max(value = 8, message = "验证码长度最大为8")
    private Long codeLength;

    @Min(value = 60, message = "验证码有效时长最小为60秒")
    @Max(value = 1800, message = "验证码有效时长最大为1800秒")
    private Long validTime;

    private Long duplicatePolicy;

    @Min(value = 0, message = "发送间隔不能小于0秒")
    @Max(value = 600, message = "发送间隔不能超过600秒")
    private Long interval;

    private Long codeType;
    private Boolean returnVerifyCode;
    private Boolean autoRetry;
}
