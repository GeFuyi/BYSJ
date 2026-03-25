package com.community.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ServiceAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long serviceId;
    private String fromAuditStatus;
    private String toAuditStatus;
    private String action;
    private String reason;
    private Long reviewerId;
    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}

