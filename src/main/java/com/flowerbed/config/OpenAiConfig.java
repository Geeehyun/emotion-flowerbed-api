package com.flowerbed.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI API 설정
 * - application.yml의 openai.api 속성 매핑
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "openai.api")
public class OpenAiConfig {
    private String key;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
