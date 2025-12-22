package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.WeeklyReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WeeklyReportResponse {

    private Long reportId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer diaryCount;
    private String summary;
    private String emotionTrend;
    private String recommendations;
    private AnalysisDetail analysisDetail;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class AnalysisDetail {
        private String weekSummary;
        private String emotionalJourney;
        private List<EmotionStat> emotionStats;
        private List<String> highlights;
        private String growthInsight;
    }

    @Getter
    @Builder
    public static class EmotionStat {
        private String emotion;
        private Integer count;
        private Double percentage;
    }

    /**
     * Entity -> Response DTO 변환
     */
    public static WeeklyReportResponse from(WeeklyReport report) {

        AnalysisDetail analysisDetail = null;
        if (report.getAnalysisJson() != null) {
            WeeklyReport.AnalysisResult json = report.getAnalysisJson();

            List<EmotionStat> emotionStats = null;
            if (json.getEmotionStats() != null) {
                emotionStats = json.getEmotionStats().stream()
                        .map(e -> EmotionStat.builder()
                                .emotion(e.getEmotion())
                                .count(e.getCount())
                                .percentage(e.getPercentage())
                                .build())
                        .toList();
            }

            analysisDetail = AnalysisDetail.builder()
                    .weekSummary(json.getWeekSummary())
                    .emotionalJourney(json.getEmotionalJourney())
                    .emotionStats(emotionStats)
                    .highlights(json.getHighlights())
                    .growthInsight(json.getGrowthInsight())
                    .build();
        }

        return WeeklyReportResponse.builder()
                .reportId(report.getReportId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .diaryCount(report.getDiaryCount())
                .summary(report.getSummary())
                .emotionTrend(report.getEmotionTrend())
                .recommendations(report.getRecommendations())
                .analysisDetail(analysisDetail)
                .createdAt(report.getCreatedAt())
                .build();
    }
}
