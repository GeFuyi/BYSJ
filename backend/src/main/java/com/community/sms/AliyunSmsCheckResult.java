package com.community.sms;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AliyunSmsCheckResult {

    private String code;
    private String message;
    private Boolean success;
    private String outId;
    private String verifyResult;
}
