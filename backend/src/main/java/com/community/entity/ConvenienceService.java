package com.community.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class ConvenienceService implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long providerId;
    private String name;
    private String categoryCode;
    private String summary;
    private String description;
    private String contactName;
    private String contactPhone;
    private String address;
    private String coverImagePath;
    private String serviceStatus;
    private String auditStatus;
    private String auditReason;
    private Long reviewedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedAt;

    private Integer maxCapacity;
    private Integer currentBooked;
    private BigDecimal avgScore;
    private Integer scoreCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;
}

