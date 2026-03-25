package com.community.mapper;

import com.community.entity.RepairOrderFlow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RepairOrderFlowMapper {

    int insert(RepairOrderFlow flow);

    List<RepairOrderFlow> selectByOrderId(@Param("orderId") Long orderId);
}

