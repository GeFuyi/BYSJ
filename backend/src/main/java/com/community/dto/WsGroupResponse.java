package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class WsGroupResponse {

    private Long groupId;
    private String name;
    private Long ownerId;
    private Boolean muted;
    private String announcement;
    private Long announcementVersion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date announcementUpdatedAt;

    private Boolean announcementAcked;
    private List<WsUserBriefResponse> members;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
