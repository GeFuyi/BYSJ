package com.community.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
}

