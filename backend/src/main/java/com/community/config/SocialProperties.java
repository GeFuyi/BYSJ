package com.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "social")
public class SocialProperties {

    private String uploadDir = "uploads/social";
    private long maxFileSizeMb = 10;
}

