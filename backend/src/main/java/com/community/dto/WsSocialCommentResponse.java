package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class WsSocialCommentResponse {

    private Long id;
    private Long postId;
    private Long parentId;
    private Long replyToUserId;
    private Long userId;
    private String nickname;
    private String avatarPath;
    private String replyToNickname;
    private String replyToAvatarPath;
    private String content;
    private List<String> imagePaths;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
