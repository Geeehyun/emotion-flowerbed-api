package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.WeeklyReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주간 리포트 상세 조회 응답 DTO (학생용)
 * - 학생이 자신의 주간 리포트를 상세 조회할 때 사용
 * - studentReport, studentEncouragement만 포함
 * - teacherReport, teacherTalkTip은 제외
 */
@Getter
@Builder
public class WeeklyReportDetailResponse {

    private Long reportId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer diaryCount;
    private Boolean isAnalyzed;
    private Boolean readYn;
    private String studentReport;
    private String studentEncouragement;
    private List<EmotionStatDto> emotionStats;
    private List<DiaryDetailDto> weeklyDiaryDetails;
    private LocalDateTime createdAt;

    /**
     * 감정 통계 DTO
     */
    @Getter
    @Builder
    public static class EmotionStatDto {
        private String emotion;  // 감정 코드
        private String emotionNameKr;  // 감정 한글 이름
        private Integer count;  // 출현 횟수
        private Double percentage;  // 비율
    }

    /**
     * 주간 일기 상세 DTO
     * - 프론트에서 날짜별 감정 조회용
     */
    @Getter
    @Builder
    public static class DiaryDetailDto {
        private Long diaryId;
        private LocalDate diaryDate;
        private String coreEmotion;
        private String emotionNameKr;
        private String flowerNameKr;
        private String flowerMeaning;
    }

    /**
     * Entity -> Response DTO 변환 (학생용)
     */
    public static WeeklyReportDetailResponse from(WeeklyReport report) {
        List<EmotionStatDto> emotionStats = null;
        if (report.getEmotionStats() != null) {
            emotionStats = report.getEmotionStats().stream()
                    .map(stat -> EmotionStatDto.builder()
                            .emotion(stat.getEmotion())
                            .emotionNameKr(stat.getEmotionNameKr())
                            .count(stat.getCount())
                            .percentage(stat.getPercentage())
                            .build())
                    .collect(Collectors.toList());
        }

        List<DiaryDetailDto> diaryDetails = null;
        if (report.getWeeklyDiaryDetails() != null) {
            diaryDetails = report.getWeeklyDiaryDetails().stream()
                    .map(detail -> DiaryDetailDto.builder()
                            .diaryId(detail.getDiaryId())
                            .diaryDate(detail.getDiaryDate())
                            .coreEmotion(detail.getCoreEmotion())
                            .emotionNameKr(detail.getEmotionNameKr())
                            .flowerNameKr(detail.getFlowerNameKr())
                            .flowerMeaning(detail.getFlowerMeaning())
                            .build())
                    .collect(Collectors.toList());
        }

        return WeeklyReportDetailResponse.builder()
                .reportId(report.getReportId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .diaryCount(report.getDiaryCount())
                .isAnalyzed(report.getIsAnalyzed())
                .readYn(report.getReadYn())
                .studentReport(report.getStudentReport())
                .studentEncouragement(report.getStudentEncouragement())
                .emotionStats(emotionStats)
                .weeklyDiaryDetails(diaryDetails)
                .createdAt(report.getCreatedAt())
                .build();
    }
}
