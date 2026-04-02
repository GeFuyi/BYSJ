package com.community.mapper;

import com.community.entity.SocialPost;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SocialPostMapper {

    int insert(SocialPost post);

    SocialPost selectById(@Param("id") Long id);

    List<SocialPost> selectFeed(@Param("beforeId") Long beforeId, @Param("limit") int limit);
}

