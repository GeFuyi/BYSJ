package com.community.dto;

import lombok.Data;

@Data
public class WsFriendAddResult {

    private String mode;
    private WsFriendResponse friend;
    private WsFriendRequestResponse request;
}
