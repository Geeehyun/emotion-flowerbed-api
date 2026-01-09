package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.repository.FlowerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 감정/꽃 마스터 데이터 캐싱 서비스
 * - 거의 변경되지 않는 마스터 데이터를 Redis에 캐싱
 * - 일기 분석, 주간 리포트 생성 시 DB 부하 감소
 * - TTL: 24시간
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmotionCacheService {

    private final FlowerRepository flowerRepository;

    /**
     * 감정 코드로 조회 (캐싱)
     * - 캐시 키: emotion::{emotionCode}
     * - TTL: 24시간
     *
     * @param emotionCode 감정 코드 (E001, E002 등)
     * @return Emotion 객체 또는 null
     */
    @Cacheable(value = "emotion", key = "#emotionCode", unless = "#result == null")
    public Emotion getEmotion(String emotionCode) {
        log.debug("Cache miss - Loading emotion from DB: {}", emotionCode);
        return flowerRepository.findById(emotionCode).orElse(null);
    }

    /**
     * 전체 감정 목록 조회 (캐싱)
     * - 캐시 키: emotion::all
     * - TTL: 24시간
     *
     * @return 전체 Emotion 리스트 (display_order 오름차순)
     */
    @Cacheable(value = "emotion", key = "'all'")
    public List<Emotion> getAllEmotions() {
        log.debug("Cache miss - Loading all emotions from DB");
        return flowerRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * 감정 캐시 전체 삭제
     * - 감정/꽃 마스터 데이터 업데이트 시 사용
     */
    @CacheEvict(value = "emotion", allEntries = true)
    public void evictAllEmotionCache() {
        log.info("All emotion cache evicted");
    }

    /**
     * 특정 감정 캐시 삭제
     *
     * @param emotionCode 감정 코드
     */
    @CacheEvict(value = "emotion", key = "#emotionCode")
    public void evictEmotionCache(String emotionCode) {
        log.info("Emotion cache evicted: {}", emotionCode);
    }
}
