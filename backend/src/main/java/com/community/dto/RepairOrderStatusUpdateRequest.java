package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class RepairOrderStatusUpdateRequest {

    @NotBlank(message = "不能为空")
    private String targetStatus;

    @Size(max = 255, message = "长度不合法")
    private String remark;

    private List<String> imagePaths;
}
