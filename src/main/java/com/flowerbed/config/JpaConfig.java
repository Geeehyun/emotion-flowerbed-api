package com.flowerbed.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정
 *
 * @EnableJpaAuditing - JPA Auditing 활성화
 *   - @CreatedDate, @LastModifiedDate 자동 설정
 *   - @CreatedBy, @LastModifiedBy 자동 설정 (AuditorAware 사용)
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }
}

