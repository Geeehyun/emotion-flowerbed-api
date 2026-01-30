package com.flowerbed.api.v1.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 발행 가능한 주간 리포트 목록 응답 DTO (발행 횟수 정보 포함)
 */
@Getter
@Builder
public class GenerableWeeksResponse {

    private Integer dailyLimit;      // 일일 발행 가능 횟수
    private Integer usedCount;       // 오늘 발행한 횟수
    private Integer remainingCount;  // 남은 발행 가능 횟수

    private List<GenerableWeekResponse> weeks;  // 발행 가능한 주 목록

    public static GenerableWeeksResponse of(int dailyLimit, int usedCount, List<GenerableWeekResponse> weeks) {
        return GenerableWeeksResponse.builder()
                .dailyLimit(dailyLimit)
                .usedCount(usedCount)
                .remainingCount(Math.max(0, dailyLimit - usedCount))
                .weeks(weeks)
                .build();
    }
}
