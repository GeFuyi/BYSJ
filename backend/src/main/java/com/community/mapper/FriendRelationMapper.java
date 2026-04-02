package com.community.mapper;

import com.community.entity.FriendRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FriendRelationMapper {

    int insert(FriendRelation relation);

    FriendRelation selectByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);

    List<FriendRelation> selectByUserId(@Param("userId") Long userId);

    int deleteByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
