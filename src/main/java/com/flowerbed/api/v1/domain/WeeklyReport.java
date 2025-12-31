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
 * - 매주 월요일 00시에 지난 주(월~일) 주간 리포트 레코드 생성 (모든 사용자)
 * - 일기 3개 이상: AI 분석 수행 (isAnalyzed=true)
 * - 일기 3개 미만: 분석 미수행 (isAnalyzed=false, 분석 필드 null)
 * - 학생과 담당 선생님만 조회 가능
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

    @Column(name = "student_report", columnDefinition = "TEXT")
    private String studentReport;

    @Column(name = "student_encouragement", columnDefinition = "TEXT")
    private String studentEncouragement;

    @Column(name = "teacher_report", columnDefinition = "TEXT")
    private String teacherReport;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teacher_talk_tip", columnDefinition = "LONGTEXT")
    private List<String> teacherTalkTip;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emotion_stats", columnDefinition = "LONGTEXT")
    private List<EmotionStat> emotionStats;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weekly_diary_details", columnDefinition = "LONGTEXT")
    private List<DiaryDetail> weeklyDiaryDetails;

    @Column(name = "is_analyzed", nullable = false)
    private Boolean isAnalyzed = false;

    @Column(name = "read_yn", nullable = false)
    private Boolean readYn = false;

    @Column(name = "new_notification_sent", nullable = false)
    private Boolean newNotificationSent = false;

    @Builder
    public WeeklyReport(User user, LocalDate startDate, LocalDate endDate,
                       Integer diaryCount, String studentReport, String studentEncouragement,
                       String teacherReport, List<String> teacherTalkTip,
                       List<EmotionStat> emotionStats, List<DiaryDetail> weeklyDiaryDetails,
                       Boolean isAnalyzed, Boolean readYn, Boolean newNotificationSent) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.diaryCount = diaryCount;
        this.studentReport = studentReport;
        this.studentEncouragement = studentEncouragement;
        this.teacherReport = teacherReport;
        this.teacherTalkTip = teacherTalkTip;
        this.emotionStats = emotionStats;
        this.weeklyDiaryDetails = weeklyDiaryDetails;
        this.isAnalyzed = isAnalyzed != null ? isAnalyzed : false;
        this.readYn = readYn != null ? readYn : false;
        this.newNotificationSent = newNotificationSent != null ? newNotificationSent : false;
    }

    /**
     * 감정 통계 정보를 JSON으로 저장하기 위한 내부 클래스
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class EmotionStat {
        private String emotion;  // 감정 코드 (영문)
        private String emotionNameKr;  // 감정 이름 (한글)
        private Integer count;  // 출현 횟수
        private Double percentage;  // 비율

        @Builder
        public EmotionStat(String emotion, String emotionNameKr, Integer count, Double percentage) {
            this.emotion = emotion;
            this.emotionNameKr = emotionNameKr;
            this.count = count;
            this.percentage = percentage;
        }
    }

    /**
     * 주간 일기 상세 정보를 JSON으로 저장하기 위한 내부 클래스
     * - 날짜별 일기의 감정 정보 (프론트에서 날짜별 조회용)
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class DiaryDetail {
        private Long diaryId;  // 일기 ID
        private LocalDate diaryDate;  // 일기 날짜
        private String coreEmotion;  // 핵심 감정 코드
        private String emotionNameKr;  // 감정 한글 이름
        private String flowerNameKr;  // 꽃 한글 이름
        private String flowerMeaning;  // 꽃말

        @Builder
        public DiaryDetail(Long diaryId, LocalDate diaryDate, String coreEmotion,
                          String emotionNameKr, String flowerNameKr, String flowerMeaning) {
            this.diaryId = diaryId;
            this.diaryDate = diaryDate;
            this.coreEmotion = coreEmotion;
            this.emotionNameKr = emotionNameKr;
            this.flowerNameKr = flowerNameKr;
            this.flowerMeaning = flowerMeaning;
        }
    }

    /**
     * 읽음 상태 업데이트
     */
    public void markAsRead() {
        this.readYn = true;
    }

    /**
     * 새 리포트 알림 전송 완료 처리
     */
    public void markNotificationSent() {
        this.newNotificationSent = true;
    }

    /**
     * 주간 리포트 분석 결과 업데이트 (재시도 시 사용)
     */
    public void updateAnalysisResult(String studentReport, String studentEncouragement,
                                     String teacherReport, List<String> teacherTalkTip,
                                     List<EmotionStat> emotionStats, List<DiaryDetail> weeklyDiaryDetails,
                                     Integer diaryCount) {
        this.studentReport = studentReport;
        this.studentEncouragement = studentEncouragement;
        this.teacherReport = teacherReport;
        this.teacherTalkTip = teacherTalkTip;
        this.emotionStats = emotionStats;
        this.weeklyDiaryDetails = weeklyDiaryDetails;
        this.diaryCount = diaryCount;
        this.isAnalyzed = true;  // 분석 완료 상태로 변경
    }
}
