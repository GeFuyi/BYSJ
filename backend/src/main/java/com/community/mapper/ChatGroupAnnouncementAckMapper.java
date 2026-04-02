package com.community.mapper;

import com.community.entity.ChatGroupAnnouncementAck;
import org.apache.ibatis.annotations.Param;

public interface ChatGroupAnnouncementAckMapper {

    int insert(ChatGroupAnnouncementAck ack);

    ChatGroupAnnouncementAck selectByGroupUserVersion(@Param("groupId") Long groupId,
                                                      @Param("userId") Long userId,
                                                      @Param("announcementVersion") Long announcementVersion);
}
