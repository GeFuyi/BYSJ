package com.community.mapper;

import com.community.entity.ServiceCategory;

import java.util.List;

public interface ServiceCategoryMapper {

    List<ServiceCategory> selectEnabled();

    ServiceCategory selectByCode(String code);
}

