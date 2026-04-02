package com.community.mapper;

import com.community.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatMessageMapper {

    int insert(ChatMessage message);

    List<ChatMessage> selectPrivateHistory(@Param("userA") Long userA,
                                           @Param("userB") Long userB,
                                           @Param("limit") int limit);

    List<ChatMessage> selectGroupHistory(@Param("groupId") Long groupId,
                                         @Param("limit") int limit);
}

