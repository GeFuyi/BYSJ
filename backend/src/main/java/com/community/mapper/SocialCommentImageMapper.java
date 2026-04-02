package com.community.mapper;

import com.community.entity.SocialCommentImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SocialCommentImageMapper {

    int insertBatch(@Param("commentId") Long commentId, @Param("imagePaths") List<String> imagePaths);

    List<SocialCommentImage> selectByCommentId(@Param("commentId") Long commentId);

    List<SocialCommentImage> selectByCommentIds(@Param("commentIds") List<Long> commentIds);
}

