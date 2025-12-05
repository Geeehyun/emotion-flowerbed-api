package com.flowerbed.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "anthropic.api")
public class AnthropicConfig {
    private String key;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
