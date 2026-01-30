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
 * - 주간 리포트 발행 횟수 제한
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BL:";
    private static final String WEEKLY_REPORT_LIMIT_PREFIX = "WRL:";

    /**
     * 일일 주간 리포트 발행 횟수 제한
     * - 추후 변경 시 이 상수만 수정
     */
    public static final int DAILY_WEEKLY_REPORT_LIMIT = 1;

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

    // ======================== 주간 리포트 발행 횟수 제한 ========================

    /**
     * 오늘 주간 리포트 발행 횟수 조회
     * @param userSn 사용자 일련번호
     * @return 오늘 발행 횟수 (없으면 0)
     */
    public int getWeeklyReportGenerateCount(Long userSn) {
        String key = buildWeeklyReportLimitKey(userSn);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * 주간 리포트 발행 가능 여부 확인
     * @param userSn 사용자 일련번호
     * @return 발행 가능하면 true
     */
    public boolean canGenerateWeeklyReport(Long userSn) {
        int currentCount = getWeeklyReportGenerateCount(userSn);
        return currentCount < DAILY_WEEKLY_REPORT_LIMIT;
    }

    /**
     * 주간 리포트 발행 횟수 증가
     * - 오늘 자정까지 TTL 설정
     * @param userSn 사용자 일련번호
     * @return 증가 후 횟수
     */
    public int incrementWeeklyReportGenerateCount(Long userSn) {
        String key = buildWeeklyReportLimitKey(userSn);

        // 증가
        Long newCount = redisTemplate.opsForValue().increment(key);

        // 첫 발행이면 자정까지 TTL 설정
        if (newCount != null && newCount == 1) {
            long secondsUntilMidnight = getSecondsUntilMidnight();
            redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
            log.debug("Weekly report limit key created with TTL {} seconds for userSn: {}", secondsUntilMidnight, userSn);
        }

        log.debug("Weekly report generate count incremented to {} for userSn: {}", newCount, userSn);
        return newCount != null ? newCount.intValue() : 1;
    }

    /**
     * 남은 발행 가능 횟수 조회
     * @param userSn 사용자 일련번호
     * @return 남은 횟수
     */
    public int getRemainingWeeklyReportGenerateCount(Long userSn) {
        int currentCount = getWeeklyReportGenerateCount(userSn);
        return Math.max(0, DAILY_WEEKLY_REPORT_LIMIT - currentCount);
    }

    /**
     * 주간 리포트 제한 키 생성
     * - 형식: WRL:{userSn}:{yyyy-MM-dd}
     */
    private String buildWeeklyReportLimitKey(Long userSn) {
        String today = java.time.LocalDate.now().toString();
        return WEEKLY_REPORT_LIMIT_PREFIX + userSn + ":" + today;
    }

    /**
     * 자정까지 남은 초 계산
     */
    private long getSecondsUntilMidnight() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, midnight).getSeconds();
    }
}
