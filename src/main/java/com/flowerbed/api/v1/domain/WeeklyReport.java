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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "highlights", columnDefinition = "LONGTEXT")
    private Highlights highlights;

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
                       Highlights highlights,
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
        this.highlights = highlights;
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
        private String color;  // 감정 색상 (HEX)
        private Integer count;  // 출현 횟수
        private Double percentage;  // 비율

        @Builder
        public EmotionStat(String emotion, String emotionNameKr, String color, Integer count, Double percentage) {
            this.emotion = emotion;
            this.emotionNameKr = emotionNameKr;
            this.color = color;
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
        private String imageFile3d;  // 3D 이미지 파일명

        @Builder
        public DiaryDetail(Long diaryId, LocalDate diaryDate, String coreEmotion,
                          String emotionNameKr, String flowerNameKr, String flowerMeaning, String imageFile3d) {
            this.diaryId = diaryId;
            this.diaryDate = diaryDate;
            this.coreEmotion = coreEmotion;
            this.emotionNameKr = emotionNameKr;
            this.flowerNameKr = flowerNameKr;
            this.flowerMeaning = flowerMeaning;
            this.imageFile3d = imageFile3d;
        }
    }

    /**
     * 주간 리포트 하이라이트 정보를 JSON으로 저장하기 위한 내부 클래스
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Highlights {
        private FlowerOfTheWeek flowerOfTheWeek;  // 이번 주 대표 꽃
        private QuickStats quickStats;  // 숫자로 보는 한 주
        private GardenDiversity gardenDiversity;  // 감정 정원 다양성

        @Builder
        public Highlights(FlowerOfTheWeek flowerOfTheWeek, QuickStats quickStats, GardenDiversity gardenDiversity) {
            this.flowerOfTheWeek = flowerOfTheWeek;
            this.quickStats = quickStats;
            this.gardenDiversity = gardenDiversity;
        }
    }

    /**
     * 이번 주 대표 꽃
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FlowerOfTheWeek {
        private String emotion;  // 감정 코드
        private String emotionNameKr;  // 감정 한글 이름
        private String flowerNameKr;  // 꽃 한글 이름
        private String flowerMeaning;  // 꽃말
        private String imageFile3d;  // 3D 이미지 파일명
        private Integer count;  // 출현 횟수

        @Builder
        public FlowerOfTheWeek(String emotion, String emotionNameKr, String flowerNameKr,
                              String flowerMeaning, String imageFile3d, Integer count) {
            this.emotion = emotion;
            this.emotionNameKr = emotionNameKr;
            this.flowerNameKr = flowerNameKr;
            this.flowerMeaning = flowerMeaning;
            this.imageFile3d = imageFile3d;
            this.count = count;
        }
    }

    /**
     * 숫자로 보는 한 주
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class QuickStats {
        private Integer totalDiaries;  // 총 일기 개수
        private Integer emotionVariety;  // 경험한 감정 종류 수
        private String dominantArea;  // 가장 많이 느낀 감정 영역
        private String dominantAreaNameKr;  // 감정 영역 한글명

        @Builder
        public QuickStats(Integer totalDiaries, Integer emotionVariety,
                         String dominantArea, String dominantAreaNameKr) {
            this.totalDiaries = totalDiaries;
            this.emotionVariety = emotionVariety;
            this.dominantArea = dominantArea;
            this.dominantAreaNameKr = dominantAreaNameKr;
        }
    }

    /**
     * 감정 정원 다양성
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class GardenDiversity {
        private Integer score;  // 0-100점
        private String level;  // 레벨명
        private String description;  // 설명 메시지
        private Integer emotionVariety;  // 감정 종류 수
        private Integer areaVariety;  // 영역 종류 수

        @Builder
        public GardenDiversity(Integer score, String level, String description,
                              Integer emotionVariety, Integer areaVariety) {
            this.score = score;
            this.level = level;
            this.description = description;
            this.emotionVariety = emotionVariety;
            this.areaVariety = areaVariety;
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
                                     Highlights highlights, Integer diaryCount) {
        this.studentReport = studentReport;
        this.studentEncouragement = studentEncouragement;
        this.teacherReport = teacherReport;
        this.teacherTalkTip = teacherTalkTip;
        this.emotionStats = emotionStats;
        this.weeklyDiaryDetails = weeklyDiaryDetails;
        this.highlights = highlights;
        this.diaryCount = diaryCount;
        this.isAnalyzed = true;  // 분석 완료 상태로 변경
    }
}
