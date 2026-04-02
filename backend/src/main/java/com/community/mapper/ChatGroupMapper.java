package com.community.mapper;

import com.community.entity.ChatGroup;
import org.apache.ibatis.annotations.Param;

public interface ChatGroupMapper {

    int insert(ChatGroup group);

    ChatGroup selectById(@Param("id") Long id);

    int updateOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    int updateAnnouncement(@Param("id") Long id,
                           @Param("announcement") String announcement,
                           @Param("announcementVersion") Long announcementVersion);

    int deleteById(@Param("id") Long id);
}
