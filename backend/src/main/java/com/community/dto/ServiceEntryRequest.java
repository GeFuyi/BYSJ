package com.community.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class ServiceEntryRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 100, message = "长度不合法")
    private String name;

    @NotBlank(message = "不能为空")
    @Size(max = 32, message = "长度不合法")
    private String categoryCode;

    @NotBlank(message = "不能为空")
    @Size(max = 255, message = "长度不合法")
    private String summary;

    @NotBlank(message = "不能为空")
    @Size(max = 5000, message = "长度不合法")
    private String description;

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不合法")
    private String contactName;

    @NotBlank(message = "不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "格式不正确")
    private String contactPhone;

    @Size(max = 255, message = "长度不合法")
    private String address;

    @Size(max = 255, message = "长度不合法")
    private String coverImagePath;

    private List<@Size(max = 255, message = "长度不合法") String> imagePaths;

    @Min(value = 1, message = "数值过小")
    @Max(value = 9999, message = "数值过大")
    private Integer maxCapacity;
}
