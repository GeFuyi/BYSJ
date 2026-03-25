package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ServiceAuditLogResponse {

    private Long id;
    private String fromAuditStatus;
    private String fromAuditStatusLabel;
    private String toAuditStatus;
    private String toAuditStatusLabel;
    private String action;
    private String reason;
    private Long reviewerId;
    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}

