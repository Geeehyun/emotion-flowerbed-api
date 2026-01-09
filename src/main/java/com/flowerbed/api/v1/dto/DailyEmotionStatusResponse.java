package com.flowerbed.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 날짜별 학생 감정 현황 응답 DTO
 * - 선생님이 특정 날짜의 반 학생들 감정 상태를 조회할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyEmotionStatusResponse {

    /**
     * 조회 날짜
     */
    private LocalDate date;

    /**
     * 전체 학생 수
     */
    private Integer totalCount;

    /**
     * 영역별 학생 수
     * - red: 빨강 영역 (강한 감정)
     * - yellow: 노랑 영역 (활기찬 감정)
     * - blue: 파랑 영역 (차분한 감정)
     * - green: 초록 영역 (평온한 감정)
     * - unanalyzed: 일기 작성했지만 분석 안됨
     * - none: 일기 미작성
     */
    private Map<String, Integer> area;

    /**
     * 학생별 감정 정보 리스트
     */
    private List<StudentEmotionInfo> students;

    /**
     * 학생 감정 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentEmotionInfo {

        /**
         * 학생 일련번호
         */
        private Long userSn;

        /**
         * 학생 이름
         */
        private String name;

        /**
         * 감정 영역
         * - red, yellow, blue, green: 분석된 감정 영역
         * - unanalyzed: 일기 있지만 분석 안됨
         * - none: 일기 없음
         */
        private String area;

        /**
         * 핵심 감정 코드
         * - 분석된 경우만 값 있음
         */
        private String coreEmotion;

        /**
         * 핵심 감정 한글명
         * - 분석된 경우만 값 있음
         */
        private String coreEmotionNameKr;

        /**
         * 일기 분석 여부
         */
        private Boolean isAnalyzed;
    }
}
