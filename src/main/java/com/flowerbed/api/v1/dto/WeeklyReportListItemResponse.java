package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.WeeklyReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주간 리포트 리스트 아이템 응답 DTO
 * - 주간 리포트 목록 조회 시 사용
 * - 필수 정보만 포함 (분석 내용 제외)
 */
@Getter
@Builder
public class WeeklyReportListItemResponse {

    private Long reportId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer diaryCount;  // 리포트 생성 당시 일기 개수
    private Integer currentDiaryCount;  // 현재 시점 일기 개수
    private Boolean isAnalyzed;
    private Boolean isAnalyzable;  // 현재 분석 가능 여부 (분석된 일기 3개 이상)
    private Boolean readYn;
    private LocalDateTime createdAt;

    /**
     * Entity -> Response DTO 변환 (현재 일기 개수 포함)
     *
     * @param report 주간 리포트 엔티티
     * @param currentDiaryCount 현재 시점 해당 주의 분석된 일기 개수
     * @return 주간 리포트 리스트 아이템 응답 DTO
     */
    public static WeeklyReportListItemResponse from(WeeklyReport report, Integer currentDiaryCount) {
        // 현재 분석된 일기가 3개 이상이면 분석 가능
        boolean isAnalyzable = currentDiaryCount != null && currentDiaryCount >= 3;

        return WeeklyReportListItemResponse.builder()
                .reportId(report.getReportId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .diaryCount(report.getDiaryCount())
                .currentDiaryCount(currentDiaryCount)
                .isAnalyzed(report.getIsAnalyzed())
                .isAnalyzable(isAnalyzable)
                .readYn(report.getReadYn())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /**
     * Entity -> Response DTO 변환 (기존 메서드, 하위 호환성 유지)
     * currentDiaryCount와 isAnalyzable은 null로 설정됨
     */
    public static WeeklyReportListItemResponse from(WeeklyReport report) {
        return WeeklyReportListItemResponse.builder()
                .reportId(report.getReportId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .diaryCount(report.getDiaryCount())
                .currentDiaryCount(null)
                .isAnalyzed(report.getIsAnalyzed())
                .isAnalyzable(null)
                .readYn(report.getReadYn())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
