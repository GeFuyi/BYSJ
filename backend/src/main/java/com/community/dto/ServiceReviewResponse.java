package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ServiceReviewResponse {

    private Long id;
    private Long serviceId;
    private Long userId;
    private Integer rating;
    private String content;
    private String reviewerName;
    private String reviewerAvatarPath;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
