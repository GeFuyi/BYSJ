package com.community.dto;

import lombok.Data;

@Data
public class WsOnlineUserResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarPath;
    private String role;
}
