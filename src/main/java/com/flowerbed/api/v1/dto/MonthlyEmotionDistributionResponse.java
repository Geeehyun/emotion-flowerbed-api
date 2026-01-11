package com.flowerbed.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 선생님 학급 월별 감정 분포 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEmotionDistributionResponse {

    /**
     * 조회한 년월 (YYYY-MM)
     */
    private String yearMonth;

    /**
     * 전체 학생 수
     */
    private Integer totalStudents;

    /**
     * 영역별 핵심 키워드 (빈도 높은 순 최대 5개)
     */
    private AreaKeywords areaKeywords;

    /**
     * 일자별 감정 분포 리스트
     */
    private List<DailyDistribution> dailyDistribution;

    /**
     * 일자별 감정 분포
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyDistribution {
        /**
         * 날짜 (YYYY-MM-DD)
         */
        private String date;

        /**
         * 요일 (월요일, 화요일, ...)
         */
        private String dayOfWeek;

        /**
         * 영역별 학생 수
         */
        private AreaDistribution area;
    }

    /**
     * 영역별 학생 수
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaDistribution {
        /**
         * 빨강 영역 (강한 감정) 학생 수
         */
        private Integer red;

        /**
         * 노랑 영역 (활기찬 감정) 학생 수
         */
        private Integer yellow;

        /**
         * 파랑 영역 (차분한 감정) 학생 수
         */
        private Integer blue;

        /**
         * 초록 영역 (평온한 감정) 학생 수
         */
        private Integer green;

        /**
         * 일기 작성했지만 분석 안 됨
         */
        private Integer unanalyzed;

        /**
         * 일기 미작성
         */
        private Integer none;
    }

    /**
     * 영역별 핵심 키워드
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaKeywords {
        /**
         * 빨강 영역 키워드 (빈도 높은 순 최대 5개)
         */
        private List<String> red;

        /**
         * 노랑 영역 키워드 (빈도 높은 순 최대 5개)
         */
        private List<String> yellow;

        /**
         * 파랑 영역 키워드 (빈도 높은 순 최대 5개)
         */
        private List<String> blue;

        /**
         * 초록 영역 키워드 (빈도 높은 순 최대 5개)
         */
        private List<String> green;
    }
}
