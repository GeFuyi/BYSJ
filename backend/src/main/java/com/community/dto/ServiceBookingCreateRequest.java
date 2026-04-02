package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ServiceBookingCreateRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不合法")
    private String contactName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "格式不正确")
    private String contactPhone;

    @Size(max = 255, message = "长度不合法")
    private String remark;
}
