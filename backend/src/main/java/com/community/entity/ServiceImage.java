package com.community.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long serviceId;
    private String imagePath;
    private Integer sortNo;
}

