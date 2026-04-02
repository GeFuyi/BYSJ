package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class RepairOrderCreateRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 100, message = "长度不合法")
    private String title;

    @NotBlank(message = "不能为空")
    @Size(max = 2000, message = "长度不合法")
    private String description;

    @Pattern(regexp = "^\\d{11}$", message = "格式不正确")
    private String contactPhone;

    private List<@Size(max = 255, message = "长度不合法") String> imagePaths;
}
