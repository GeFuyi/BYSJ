package com.community.mapper;

import com.community.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatGroupMemberMapper {

    int insert(ChatGroupMember member);

    ChatGroupMember selectByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    List<ChatGroupMember> selectByGroupId(@Param("groupId") Long groupId);

    List<ChatGroupMember> selectByUserId(@Param("userId") Long userId);

    int updateMuted(@Param("groupId") Long groupId,
                    @Param("userId") Long userId,
                    @Param("muted") Integer muted);

    int updateRole(@Param("groupId") Long groupId,
                   @Param("userId") Long userId,
                   @Param("role") String role);

    int deleteByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);

    int deleteByGroupId(@Param("groupId") Long groupId);

    int countByGroupId(@Param("groupId") Long groupId);

    ChatGroupMember selectFirstByGroupId(@Param("groupId") Long groupId);
}
