package com.flowerbed.api.v1.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 발행 가능한 주간 리포트 목록 응답 DTO
 * - 아직 주간 리포트가 생성되지 않은 주
 * - 분석된 일기가 3개 이상인 주
 */
@Getter
@Builder
public class GenerableWeekResponse {

    private LocalDate startDate;  // 주 시작일 (월요일)
    private LocalDate endDate;    // 주 종료일 (일요일)
    private Integer diaryCount;   // 해당 주의 분석된 일기 개수

    public static GenerableWeekResponse of(LocalDate startDate, LocalDate endDate, Integer diaryCount) {
        return GenerableWeekResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .diaryCount(diaryCount)
                .build();
    }
}
