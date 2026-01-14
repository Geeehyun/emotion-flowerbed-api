package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 위험 학생 리스트 응답 DTO
 * - 선생님이 CAUTION/DANGER 상태 학생들을 조회할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtRiskStudentsResponse {

    /**
     * 전체 위험 학생 수
     */
    private Integer totalCount;

    /**
     * DANGER 학생 수
     */
    private Integer dangerCount;

    /**
     * CAUTION 학생 수
     */
    private Integer cautionCount;

    /**
     * 위험 학생 목록
     */
    private List<AtRiskStudentInfo> students;

    /**
     * 위험 학생 상세 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtRiskStudentInfo {

        /**
         * 학생 일련번호
         */
        private Long userSn;

        /**
         * 학생 이름
         */
        private String name;

        /**
         * 위험도 레벨 (CAUTION, DANGER)
         */
        private String riskLevel;

        /**
         * 위험도 판정 사유
         */
        private String riskReason;

        /**
         * 연속된 감정 영역 (red, yellow, blue, green)
         */
        private String riskContinuousArea;

        /**
         * 연속 일수
         */
        private Integer riskContinuousDays;

        /**
         * 위험도 분석 실행 날짜 (LocalDate.now())
         */
        private java.time.LocalDate riskLastCheckedDate;

        /**
         * 위험도 분석 대상 일기 날짜
         */
        private java.time.LocalDate riskTargetDiaryDate;

        /**
         * 위험도 분석 대상 일기 SN
         */
        private Long riskTargetDiarySn;

        /**
         * 위험도 갱신 시각
         */
        private LocalDateTime riskUpdatedAt;

        /**
         * DANGER 해제한 선생님 user_sn
         */
        private Long dangerResolvedBy;

        /**
         * DANGER 해제 시각
         */
        private LocalDateTime dangerResolvedAt;

        /**
         * DANGER 해제 사유
         */
        private String dangerResolveMemo;

        /**
         * User 엔티티를 AtRiskStudentInfo로 변환
         */
        public static AtRiskStudentInfo from(User user) {
            return AtRiskStudentInfo.builder()
                    .userSn(user.getUserSn())
                    .name(user.getName())
                    .riskLevel(user.getRiskLevel())
                    .riskReason(user.getRiskReason())
                    .riskContinuousArea(user.getRiskContinuousArea())
                    .riskContinuousDays(user.getRiskContinuousDays())
                    .riskLastCheckedDate(user.getRiskLastCheckedDate())
                    .riskTargetDiaryDate(user.getRiskTargetDiaryDate())
                    .riskTargetDiarySn(user.getRiskTargetDiarySn())
                    .riskUpdatedAt(user.getRiskUpdatedAt())
                    .dangerResolvedBy(user.getDangerResolvedBy())
                    .dangerResolvedAt(user.getDangerResolvedAt())
                    .dangerResolveMemo(user.getDangerResolveMemo())
                    .build();
        }
    }
}
