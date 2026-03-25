package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ServiceBookingCreateRequest {

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人最多50字符")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Size(max = 255, message = "预约备注最多255字符")
    private String remark;
}

