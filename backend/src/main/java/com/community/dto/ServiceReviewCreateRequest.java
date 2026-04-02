package com.community.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class ServiceReviewCreateRequest {

    @NotNull(message = "不能为空")
    @Min(value = 1, message = "数值过小")
    @Max(value = 5, message = "数值过大")
    private Integer rating;

    @Size(max = 500, message = "长度不合法")
    private String content;
}
