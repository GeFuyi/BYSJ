package com.community.mapper;

import com.community.entity.ServiceAuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceAuditLogMapper {

    int insert(ServiceAuditLog log);

    List<ServiceAuditLog> selectByServiceId(@Param("serviceId") Long serviceId);
}

