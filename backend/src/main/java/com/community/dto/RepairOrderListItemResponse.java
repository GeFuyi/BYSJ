package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RepairOrderListItemResponse {

    private Long id;
    private Long userId;
    private String username;
    private String userNickname;
    private Long handlerId;
    private String handlerName;
    private String title;
    private String description;
    private String contactPhone;
    private String status;
    private String statusLabel;
    private List<String> imagePaths;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;
}

