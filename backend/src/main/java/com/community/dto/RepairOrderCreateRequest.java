package com.community.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class RepairOrderCreateRequest {

    @NotBlank(message = "报修标题不能为空")
    @Size(max = 100, message = "报修标题最多100个字符")
    private String title;

    @NotBlank(message = "报修描述不能为空")
    @Size(max = 2000, message = "报修描述最多2000个字符")
    private String description;

    @Pattern(regexp = "^\\d{11}$", message = "联系电话格式不正确")
    private String contactPhone;

    private List<@Size(max = 255, message = "图片路径过长") String> imagePaths;
}

