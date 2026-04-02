package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ServiceListItemResponse {

    private Long id;
    private Long providerId;
    private String providerName;
    private String providerAvatarPath;
    private String name;
    private String categoryCode;
    private String categoryName;
    private String summary;
    private String contactName;
    private String contactPhone;
    private String coverImagePath;
    private String serviceStatus;
    private String serviceStatusLabel;
    private String auditStatus;
    private String auditStatusLabel;
    private String auditReason;
    private Integer maxCapacity;
    private Integer currentBooked;
    private BigDecimal avgScore;
    private Integer scoreCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;
}
