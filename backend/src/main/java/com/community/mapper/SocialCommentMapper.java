package com.community.mapper;

import com.community.entity.SocialComment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SocialCommentMapper {

    int insert(SocialComment comment);

    SocialComment selectById(@Param("id") Long id);

    List<SocialComment> selectByPostId(@Param("postId") Long postId);
}

