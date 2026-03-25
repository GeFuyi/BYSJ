package com.community.mapper;

import com.community.entity.ServiceImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceImageMapper {

    int deleteByServiceId(@Param("serviceId") Long serviceId);

    int insertBatch(@Param("serviceId") Long serviceId,
                    @Param("imagePaths") List<String> imagePaths);

    List<ServiceImage> selectByServiceId(@Param("serviceId") Long serviceId);
}

