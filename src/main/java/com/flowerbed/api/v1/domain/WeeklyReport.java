package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

/**
 * 주간 리포트 엔티티
 * - 매주 월요일 00시에 지난 주(월~일) 일기 분석 결과 저장
 * - 3일 이상 일기를 쓴 사용자만 생성
 */
@Entity
@Table(name = "weekly_reports", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_week", columnNames = {"user_sn", "start_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE weekly_reports SET deleted_at = NOW() WHERE report_id = ?")
@Where(clause = "deleted_at IS NULL")
public class WeeklyReport extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_sn", nullable = false, foreignKey = @ForeignKey(name = "weekly_reports_users_user_sn_fk"))
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "diary_count", nullable = false)
    private Integer diaryCount;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "emotion_trend", columnDefinition = "TEXT")
    private String emotionTrend;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_json", columnDefinition = "LONGTEXT")
    private AnalysisResult analysisJson;

    @Builder
    public WeeklyReport(User user, LocalDate startDate, LocalDate endDate,
                       Integer diaryCount, String summary, String emotionTrend,
                       String recommendations, AnalysisResult analysisJson) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.diaryCount = diaryCount;
        this.summary = summary;
        this.emotionTrend = emotionTrend;
        this.recommendations = recommendations;
        this.analysisJson = analysisJson;
    }

    /**
     * AI 분석 결과를 JSON으로 저장하기 위한 내부 클래스
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AnalysisResult {
        private String weekSummary;
        private String emotionalJourney;
        private List<EmotionStat> emotionStats;
        private List<String> highlights;
        private String growthInsight;

        @Builder
        public AnalysisResult(String weekSummary, String emotionalJourney,
                            List<EmotionStat> emotionStats, List<String> highlights,
                            String growthInsight) {
            this.weekSummary = weekSummary;
            this.emotionalJourney = emotionalJourney;
            this.emotionStats = emotionStats;
            this.highlights = highlights;
            this.growthInsight = growthInsight;
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class EmotionStat {
        private String emotion;
        private Integer count;
        private Double percentage;

        @Builder
        public EmotionStat(String emotion, Integer count, Double percentage) {
            this.emotion = emotion;
            this.count = count;
            this.percentage = percentage;
        }
    }
}
