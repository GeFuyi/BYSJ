package com.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "service-platform")
public class ServicePlatformProperties {

    private String uploadDir = "uploads/service";
    private long maxFileSizeMb = 10;
}

