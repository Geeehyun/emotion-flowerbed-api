package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.StudentRiskHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 학생 위험도 변화 이력 응답 DTO
 * - 선생님이 특정 학생의 위험도 변화 이력을 조회할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRiskHistoryResponse {

    /**
     * 학생 일련번호
     */
    private Long userSn;

    /**
     * 학생 이름
     */
    private String name;

    /**
     * 전체 이력 개수
     */
    private Integer totalCount;

    /**
     * 위험도 변화 이력 목록
     */
    private List<RiskHistoryItem> histories;

    /**
     * 위험도 변화 이력 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskHistoryItem {

        /**
         * 이력 ID
         */
        private Long historyId;

        /**
         * 이전 위험도 레벨
         */
        private String previousLevel;

        /**
         * 새 위험도 레벨
         */
        private String newLevel;

        /**
         * 위험 유형
         * - KEYWORD_DETECTED: 키워드 탐지
         * - CONTINUOUS_RED_BLUE: 연속 red/blue
         * - CONTINUOUS_SAME_AREA: 연속 같은 영역
         * - RESOLVED: 해제됨
         */
        private String riskType;

        /**
         * 위험도 판정 사유
         */
        private String riskReason;

        /**
         * 연속된 감정 영역
         */
        private String continuousArea;

        /**
         * 연속 일수
         */
        private Integer continuousDays;

        /**
         * 탐지된 우려 키워드 목록
         */
        private List<String> concernKeywords;

        /**
         * 위험도 분석 기준 일기 날짜
         */
        private java.time.LocalDate targetDiaryDate;

        /**
         * 위험도 분석 기준 일기 SN
         */
        private Long targetDiarySn;

        /**
         * 선생님 확인 여부
         */
        private Boolean isConfirmed;

        /**
         * 확인한 선생님 user_sn
         */
        private Long confirmedBy;

        /**
         * 확인 시각
         */
        private LocalDateTime confirmedAt;

        /**
         * 선생님 메모
         */
        private String teacherMemo;

        /**
         * 이력 생성 시각
         */
        private LocalDateTime createdAt;

        /**
         * StudentRiskHistory 엔티티를 RiskHistoryItem으로 변환
         */
        public static RiskHistoryItem from(StudentRiskHistory history) {
            return RiskHistoryItem.builder()
                    .historyId(history.getHistoryId())
                    .previousLevel(history.getPreviousLevel())
                    .newLevel(history.getNewLevel())
                    .riskType(history.getRiskType())
                    .riskReason(history.getRiskReason())
                    .continuousArea(history.getContinuousArea())
                    .continuousDays(history.getContinuousDays())
                    .concernKeywords(history.getConcernKeywords())
                    .targetDiaryDate(history.getTargetDiaryDate())
                    .targetDiarySn(history.getTargetDiarySn())
                    .isConfirmed(history.getIsConfirmed())
                    .confirmedBy(history.getConfirmedBy())
                    .confirmedAt(history.getConfirmedAt())
                    .teacherMemo(history.getTeacherMemo())
                    .createdAt(history.getCreatedAt())
                    .build();
        }
    }

    /**
     * StudentRiskHistory 목록을 StudentRiskHistoryResponse로 변환
     */
    public static StudentRiskHistoryResponse from(Long userSn, String name, List<StudentRiskHistory> histories) {
        List<RiskHistoryItem> historyItems = histories.stream()
                .map(RiskHistoryItem::from)
                .collect(Collectors.toList());

        return StudentRiskHistoryResponse.builder()
                .userSn(userSn)
                .name(name)
                .totalCount(histories.size())
                .histories(historyItems)
                .build();
    }
}
