package com.flowerbed.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 캐시 설정
 *
 * Spring Cache를 활성화하고 Redis 기반 캐시 매니저를 설정합니다.
 * 자주 변경되지 않는 데이터는 캐싱하여 성능을 향상시킵니다.
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * 애플리케이션 시작 시 Spring Cache만 선택적으로 삭제
     * - JWT 토큰, Blacklist 등 다른 Redis 데이터는 유지
     * - 캐시 직렬화 방식 변경 시 기존 데이터와 충돌 방지
     */
    @PostConstruct
    public void clearSpringCachesOnStartup() {
        try {
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.afterPropertiesSet();

            // Spring Cache에서 사용하는 캐시만 삭제
            String[] cacheNames = {"emotion", "codeGroups", "codeGroup", "codes", "code", "weeklyReport"};

            for (String cacheName : cacheNames) {
                // Spring Cache는 "cacheName::*" 패턴으로 키 생성
                var keys = redisTemplate.keys(cacheName + "::*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Cleared cache: {} ({} keys)", cacheName, keys.size());
                }
            }

            log.info("Spring Cache cleared on application startup (JWT tokens preserved)");
        } catch (Exception e) {
            log.error("Failed to clear Spring caches on startup", e);
        }
    }

    /**
     * Redis 기반 캐시 매니저 설정
     *
     * 캐시 종류:
     * - codeGroups: 전체 코드 그룹 목록
     * - codeGroup: 특정 코드 그룹 (key: groupCode)
     * - codes: 특정 그룹의 코드 목록 (key: groupCode)
     * - code: 특정 코드 (key: groupCode_code)
     * - emotion: 감정/꽃 마스터 데이터 (TTL: 24시간)
     * - weeklyReport: 주간 리포트 상세 (TTL: 7일)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // ObjectMapper 설정 (Java 8 날짜/시간 타입 + 타입 정보 저장)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 타입 정보를 JSON에 포함하여 역직렬화 시 정확한 타입으로 복원
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .entryTtl(Duration.ofHours(1)); // 기본 TTL: 1시간

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 공통 코드: 1시간
        cacheConfigurations.put("codeGroups", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("codeGroup", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("codes", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("code", defaultConfig.entryTtl(Duration.ofHours(1)));

        // 감정/꽃 마스터 데이터: 24시간 (거의 변경되지 않음)
        cacheConfigurations.put("emotion", defaultConfig.entryTtl(Duration.ofHours(24)));

        // 주간 리포트 상세: 7일 (한번 생성되면 변경되지 않음)
        cacheConfigurations.put("weeklyReport", defaultConfig.entryTtl(Duration.ofDays(7)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
