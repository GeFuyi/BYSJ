package com.community.mapper;

import com.community.entity.ServiceBooking;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceBookingMapper {

    int insert(ServiceBooking booking);

    ServiceBooking selectActiveByServiceAndUser(@Param("serviceId") Long serviceId, @Param("userId") Long userId);

    List<ServiceBooking> selectByUserId(@Param("userId") Long userId);
}

