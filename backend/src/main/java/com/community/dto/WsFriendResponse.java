package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class WsFriendResponse {

    private Long relationId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarPath;
    private String role;
    private Boolean online;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
