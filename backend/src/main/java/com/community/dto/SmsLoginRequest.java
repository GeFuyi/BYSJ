package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SmsLoginRequest {

    private String schemeName;

    @Pattern(regexp = "^\\d{1,6}$", message = "格式不正确")
    private String countryCode;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "格式不正确")
    private String phoneNumber;

    private String outId;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^[0-9A-Za-z]{4,8}$", message = "格式不正确")
    private String verifyCode;

    @Pattern(regexp = "^(?i)(1|2|IGNORE_CASE|CASE_INSENSITIVE|STRICT|CASE_SENSITIVE)$", message = "格式不正确")
    private String caseAuthPolicy;
}
