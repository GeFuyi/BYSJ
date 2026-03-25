package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RepairOrderStatusUpdateRequest {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}

