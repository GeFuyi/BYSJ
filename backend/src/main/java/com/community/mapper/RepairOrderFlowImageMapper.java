package com.community.mapper;

import com.community.entity.RepairOrderFlowImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RepairOrderFlowImageMapper {

    int insertBatch(@Param("flowId") Long flowId, @Param("imagePaths") List<String> imagePaths);

    List<RepairOrderFlowImage> selectByFlowIds(@Param("flowIds") List<Long> flowIds);
}
