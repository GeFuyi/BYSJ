package com.community.mapper;

import com.community.entity.FriendRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FriendRequestMapper {

    int insert(FriendRequest request);

    FriendRequest selectById(@Param("id") Long id);

    FriendRequest selectPending(@Param("requesterId") Long requesterId,
                                @Param("targetUserId") Long targetUserId);

    List<FriendRequest> selectPendingByTarget(@Param("targetUserId") Long targetUserId);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("handledBy") Long handledBy);
}
