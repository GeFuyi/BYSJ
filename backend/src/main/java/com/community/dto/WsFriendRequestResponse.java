package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class WsFriendRequestResponse {

    private Long requestId;
    private Long requesterId;
    private String requesterUsername;
    private String requesterNickname;
    private String requesterAvatarPath;
    private String requesterRole;

    private Long targetUserId;
    private String targetUsername;
    private String targetNickname;
    private String targetAvatarPath;
    private String targetRole;

    private String status;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handledAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
