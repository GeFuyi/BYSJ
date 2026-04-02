package com.community.ws;

import lombok.Data;

@Data
public class WsSessionContext {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarPath;
    private String role;
    private String sessionId;
}
