package com.flowerbed.api.v1.service;

import com.flowerbed.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 토큰 관리 서비스
 * - RefreshToken 저장/조회/삭제
 * - AccessToken 블랙리스트 관리 (로그아웃 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";

    /**
     * RefreshToken 저장
     * @param userId 사용자 ID
     * @param refreshToken Refresh Token
     */
    public void saveRefreshToken(String userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        long ttl = jwtConfig.getREFRESH_TOKEN_EXP();

        redisTemplate.opsForValue().set(key, refreshToken, ttl, TimeUnit.MILLISECONDS);
        log.debug("RefreshToken saved for userId: {}", userId);
    }

    /**
     * RefreshToken 조회
     * @param userId 사용자 ID
     * @return RefreshToken (없으면 null)
     */
    public String getRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * RefreshToken 삭제 (로그아웃 시)
     * @param userId 사용자 ID
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("RefreshToken deleted for userId: {}", userId);
    }

    /**
     * AccessToken을 블랙리스트에 추가 (로그아웃 시)
     * @param accessToken Access Token
     * @param expirationMillis 토큰 만료까지 남은 시간 (밀리초)
     */
    public void addToBlacklist(String accessToken, long expirationMillis) {
        String key = BLACKLIST_PREFIX + accessToken;

        // 토큰이 만료될 때까지만 블랙리스트에 보관
        redisTemplate.opsForValue().set(key, "logout", expirationMillis, TimeUnit.MILLISECONDS);
        log.debug("AccessToken added to blacklist");
    }

    /**
     * AccessToken이 블랙리스트에 있는지 확인
     * @param accessToken Access Token
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
