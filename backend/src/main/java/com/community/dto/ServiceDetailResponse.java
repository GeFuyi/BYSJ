package com.community.dto;

import lombok.Data;

import java.util.List;

@Data
public class ServiceDetailResponse extends ServiceListItemResponse {

    private String description;
    private String address;
    private List<String> imagePaths;
    private List<ServiceReviewResponse> reviews;
    private List<ServiceAuditLogResponse> auditLogs;
}

