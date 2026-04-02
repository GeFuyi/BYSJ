package com.community.dto;

import lombok.Data;

import java.util.List;

@Data
public class WsInitPayload {

    private List<WsSocialPostResponse> feed;
    private List<WsFriendResponse> friends;
    private List<WsFriendRequestResponse> friendRequests;
    private List<WsGroupResponse> groups;
    private List<WsOnlineUserResponse> onlineUsers;
    private List<WsUserBriefResponse> userDirectory;
}
