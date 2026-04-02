package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ServiceBookingResponse {

    private Long id;
    private Long serviceId;
    private Long userId;
    private String userNickname;
    private String userAvatarPath;
    private String serviceName;
    private String contactName;
    private String contactPhone;
    private String remark;
    private String status;
    private String statusLabel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
