package com.community.mapper;

import com.community.entity.ServiceReview;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceReviewMapper {

    int insert(ServiceReview review);

    ServiceReview selectByServiceAndUser(@Param("serviceId") Long serviceId, @Param("userId") Long userId);

    List<ServiceReview> selectByServiceId(@Param("serviceId") Long serviceId);
}

