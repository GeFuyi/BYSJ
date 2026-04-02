package com.community.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long senderId;
    private String receiverType;
    private Long receiverId;
    private String content;
    private String imagePaths;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;
}

