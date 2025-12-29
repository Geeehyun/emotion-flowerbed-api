package com.flowerbed.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정
 *
 * Spring Cache를 활성화하고 캐시 매니저를 설정합니다.
 * 공통 코드 같이 자주 변경되지 않는 데이터는 캐싱하여 성능을 향상시킵니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 매니저 설정
     *
     * 현재는 간단한 ConcurrentMapCacheManager를 사용합니다.
     * 향후 Redis 등 분산 캐시로 변경 가능합니다.
     *
     * 캐시 종류:
     * - codeGroups: 전체 코드 그룹 목록
     * - codeGroup: 특정 코드 그룹 (key: groupCode)
     * - codes: 특정 그룹의 코드 목록 (key: groupCode)
     * - code: 특정 코드 (key: groupCode_code)
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "codeGroups",
                "codeGroup",
                "codes",
                "code"
        );
    }
}
