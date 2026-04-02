package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class RepairOrderFlowResponse {

    private Long id;
    private String fromStatus;
    private String fromStatusLabel;
    private String toStatus;
    private String toStatusLabel;
    private String remark;
    private Long operatorId;
    private String operatorName;
    private String operatorAvatarPath;
    private String operatorRole;
    private List<String> imagePaths;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
