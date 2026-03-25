package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ServiceAuditRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    @Size(max = 255, message = "审核原因最多255字符")
    private String reason;
}

