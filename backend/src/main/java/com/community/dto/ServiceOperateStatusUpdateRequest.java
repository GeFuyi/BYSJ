package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ServiceOperateStatusUpdateRequest {

    @NotBlank(message = "服务状态不能为空")
    private String serviceStatus;
}

