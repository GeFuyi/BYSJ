package com.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class WsSocialPostResponse {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarPath;
    private String content;
    private List<String> imagePaths;
    private List<WsSocialCommentResponse> comments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}
