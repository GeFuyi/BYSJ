package com.community.mapper;

import com.community.entity.SocialPostImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SocialPostImageMapper {

    int insertBatch(@Param("postId") Long postId, @Param("imagePaths") List<String> imagePaths);

    List<SocialPostImage> selectByPostId(@Param("postId") Long postId);

    List<SocialPostImage> selectByPostIds(@Param("postIds") List<Long> postIds);
}

