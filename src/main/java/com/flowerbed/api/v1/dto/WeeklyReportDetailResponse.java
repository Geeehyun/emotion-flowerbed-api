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
    private List<String> mindGardeningTip;  // 마음 가드닝 팁 (학생용, 2~3개)
    private List<String> weekKeywords;  // 주간 핵심 키워드 (최대 5개)
    private List<EmotionStatDto> emotionStats;
    private List<DiaryDetailDto> weeklyDiaryDetails;
    private HighlightsDto highlights;
    private LocalDateTime createdAt;

    /**
     * 감정 통계 DTO
     */
    @Getter
    @Builder
    public static class EmotionStatDto {
        private String emotion;  // 감정 코드
        private String emotionNameKr;  // 감정 한글 이름
        private String color;  // 감정 색상 (HEX)
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
        private String imageFile3d;
    }

    /**
     * 하이라이트 DTO
     */
    @Getter
    @Builder
    public static class HighlightsDto {
        private FlowerOfTheWeekDto flowerOfTheWeek;
        private QuickStatsDto quickStats;
        private GardenDiversityDto gardenDiversity;
    }

    /**
     * 이번 주 대표 꽃 DTO
     */
    @Getter
    @Builder
    public static class FlowerOfTheWeekDto {
        private String emotion;
        private String emotionNameKr;
        private String flowerNameKr;
        private String flowerMeaning;
        private String imageFile3d;
        private Integer count;
    }

    /**
     * 숫자로 보는 한 주 DTO
     */
    @Getter
    @Builder
    public static class QuickStatsDto {
        private Integer totalDiaries;
        private Integer emotionVariety;
        private String dominantArea;
        private String dominantAreaNameKr;
    }

    /**
     * 감정 정원 다양성 DTO
     */
    @Getter
    @Builder
    public static class GardenDiversityDto {
        private Integer score;
        private String level;
        private String description;
        private Integer emotionVariety;
        private Integer areaVariety;
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
                            .color(stat.getColor())
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
                            .imageFile3d(detail.getImageFile3d())
                            .build())
                    .collect(Collectors.toList());
        }

        HighlightsDto highlightsDto = null;
        if (report.getHighlights() != null) {
            WeeklyReport.Highlights highlights = report.getHighlights();

            FlowerOfTheWeekDto flowerDto = null;
            if (highlights.getFlowerOfTheWeek() != null) {
                WeeklyReport.FlowerOfTheWeek flower = highlights.getFlowerOfTheWeek();
                flowerDto = FlowerOfTheWeekDto.builder()
                        .emotion(flower.getEmotion())
                        .emotionNameKr(flower.getEmotionNameKr())
                        .flowerNameKr(flower.getFlowerNameKr())
                        .flowerMeaning(flower.getFlowerMeaning())
                        .imageFile3d(flower.getImageFile3d())
                        .count(flower.getCount())
                        .build();
            }

            QuickStatsDto quickStatsDto = null;
            if (highlights.getQuickStats() != null) {
                WeeklyReport.QuickStats quickStats = highlights.getQuickStats();
                quickStatsDto = QuickStatsDto.builder()
                        .totalDiaries(quickStats.getTotalDiaries())
                        .emotionVariety(quickStats.getEmotionVariety())
                        .dominantArea(quickStats.getDominantArea())
                        .dominantAreaNameKr(quickStats.getDominantAreaNameKr())
                        .build();
            }

            GardenDiversityDto gardenDto = null;
            if (highlights.getGardenDiversity() != null) {
                WeeklyReport.GardenDiversity garden = highlights.getGardenDiversity();
                gardenDto = GardenDiversityDto.builder()
                        .score(garden.getScore())
                        .level(garden.getLevel())
                        .description(garden.getDescription())
                        .emotionVariety(garden.getEmotionVariety())
                        .areaVariety(garden.getAreaVariety())
                        .build();
            }

            highlightsDto = HighlightsDto.builder()
                    .flowerOfTheWeek(flowerDto)
                    .quickStats(quickStatsDto)
                    .gardenDiversity(gardenDto)
                    .build();
        }

        // 주간 키워드를 쉼표로 구분된 문자열에서 List로 변환
        List<String> weekKeywords = null;
        if (report.getWeekKeywords() != null && !report.getWeekKeywords().isEmpty()) {
            weekKeywords = List.of(report.getWeekKeywords().split(","));
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
                .mindGardeningTip(report.getMindGardeningTip())
                .weekKeywords(weekKeywords)
                .emotionStats(emotionStats)
                .weeklyDiaryDetails(diaryDetails)
                .highlights(highlightsDto)
                .createdAt(report.getCreatedAt())
                .build();
    }
}
