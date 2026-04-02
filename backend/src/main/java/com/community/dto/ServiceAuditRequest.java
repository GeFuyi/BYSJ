package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ServiceAuditRequest {

    @NotBlank(message = "不能为空")
    private String action;

    @Size(max = 255, message = "长度不合法")
    private String reason;
}
