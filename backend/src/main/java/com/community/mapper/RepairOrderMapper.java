package com.community.mapper;

import com.community.entity.RepairOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RepairOrderMapper {

    int insert(RepairOrder order);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("handlerId") Long handlerId);

    RepairOrder selectById(@Param("id") Long id);

    List<RepairOrder> selectAll(@Param("status") String status);

    List<RepairOrder> selectByUserId(@Param("userId") Long userId,
                                     @Param("status") String status);
}

