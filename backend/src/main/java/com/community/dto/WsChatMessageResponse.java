package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class WsChatMessageResponse {

    private Long id;
    private String receiverType;
    private Long receiverId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatarPath;
    private String content;
    private List<String> imagePaths;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
