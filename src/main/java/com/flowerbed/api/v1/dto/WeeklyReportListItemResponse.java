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
    private Integer diaryCount;
    private Boolean isAnalyzed;
    private Boolean readYn;
    private LocalDateTime createdAt;

    /**
     * Entity -> Response DTO 변환
     */
    public static WeeklyReportListItemResponse from(WeeklyReport report) {
        return WeeklyReportListItemResponse.builder()
                .reportId(report.getReportId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .diaryCount(report.getDiaryCount())
                .isAnalyzed(report.getIsAnalyzed())
                .readYn(report.getReadYn())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
