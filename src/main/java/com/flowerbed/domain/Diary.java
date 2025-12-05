package com.flowerbed.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "diaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_date", columnNames = {"user_id", "diary_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE diaries SET deleted_at = NOW() WHERE diary_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Diary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_diary_user"))
    private User user;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "core_emotion", length = 20)
    private String coreEmotion;

    @Column(name = "emotion_reason", columnDefinition = "TEXT")
    private String emotionReason;

    @Column(name = "flower_name", length = 50)
    private String flowerName;

    @Column(name = "flower_meaning", length = 100)
    private String flowerMeaning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emotions_json", columnDefinition = "JSON")
    private List<EmotionPercent> emotionsJson;

    @Column(name = "is_analyzed", nullable = false)
    private Boolean isAnalyzed = false;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Diary(User user, LocalDate diaryDate, String content) {
        this.user = user;
        this.diaryDate = diaryDate;
        this.content = content;
        this.isAnalyzed = false;
    }

    public void updateContent(String content) {
        this.content = content;
        this.isAnalyzed = false;
        this.analyzedAt = null;
    }

    public void updateAnalysis(String summary, String coreEmotion, String emotionReason,
                               String flowerName, String flowerMeaning,
                               List<EmotionPercent> emotionsJson) {
        this.summary = summary;
        this.coreEmotion = coreEmotion;
        this.emotionReason = emotionReason;
        this.flowerName = flowerName;
        this.flowerMeaning = flowerMeaning;
        this.emotionsJson = emotionsJson;
        this.isAnalyzed = true;
        this.analyzedAt = LocalDateTime.now();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class EmotionPercent {
        private String emotion;
        private Integer percent;

        public EmotionPercent(String emotion, Integer percent) {
            this.emotion = emotion;
            this.percent = percent;
        }
    }
}
