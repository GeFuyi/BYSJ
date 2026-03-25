package com.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "repair")
public class RepairProperties {

    private String uploadDir = "uploads/repair";
    private long maxFileSizeMb = 10;
}

