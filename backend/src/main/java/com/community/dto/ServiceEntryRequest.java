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

    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称最多100字符")
    private String name;

    @NotBlank(message = "服务分类不能为空")
    @Size(max = 32, message = "服务分类编码过长")
    private String categoryCode;

    @NotBlank(message = "服务简介不能为空")
    @Size(max = 255, message = "服务简介最多255字符")
    private String summary;

    @NotBlank(message = "服务详情不能为空")
    @Size(max = 5000, message = "服务详情最多5000字符")
    private String description;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 50, message = "联系人最多50字符")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Size(max = 255, message = "服务地址最多255字符")
    private String address;

    @Size(max = 255, message = "封面路径过长")
    private String coverImagePath;

    private List<@Size(max = 255, message = "图片路径过长") String> imagePaths;

    @Min(value = 1, message = "可预约名额最小为1")
    @Max(value = 9999, message = "可预约名额最大为9999")
    private Integer maxCapacity;
}

