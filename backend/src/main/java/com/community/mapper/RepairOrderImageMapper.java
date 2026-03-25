package com.community.mapper;

import com.community.entity.RepairOrderImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RepairOrderImageMapper {

    int insertBatch(@Param("orderId") Long orderId,
                    @Param("imagePaths") List<String> imagePaths);

    List<RepairOrderImage> selectByOrderId(@Param("orderId") Long orderId);
}

