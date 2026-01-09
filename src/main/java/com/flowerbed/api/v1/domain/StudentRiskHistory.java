package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 학생 위험도 변화 이력
 * - 위험도가 변경될 때마다 이력 기록
 * - 통계 분석 및 감사 추적용
 */
@Entity
@Table(name = "student_risk_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentRiskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_sn", nullable = false, foreignKey = @ForeignKey(name = "fk_risk_history_user"))
    private User user;

    @Column(name = "previous_level", length = 20)
    private String previousLevel;

    @Column(name = "new_level", length = 20, nullable = false)
    private String newLevel;

    @Column(name = "risk_type", length = 50, nullable = false)
    private String riskType;

    @Column(name = "risk_reason", columnDefinition = "TEXT")
    private String riskReason;

    @Column(name = "continuous_area", length = 20)
    private String continuousArea;

    @Column(name = "continuous_days")
    private Integer continuousDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concern_keywords", columnDefinition = "LONGTEXT")
    private List<String> concernKeywords;

    @Column(name = "is_confirmed")
    private Boolean isConfirmed = false;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "teacher_memo", columnDefinition = "TEXT")
    private String teacherMemo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public StudentRiskHistory(User user, String previousLevel, String newLevel,
                              String riskType, String riskReason,
                              String continuousArea, Integer continuousDays,
                              List<String> concernKeywords) {
        this.user = user;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
        this.riskType = riskType;
        this.riskReason = riskReason;
        this.continuousArea = continuousArea;
        this.continuousDays = continuousDays;
        this.concernKeywords = concernKeywords;
        this.isConfirmed = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 선생님 확인 처리
     */
    public void confirmByTeacher(Long teacherUserSn, String memo) {
        this.isConfirmed = true;
        this.confirmedBy = teacherUserSn;
        this.confirmedAt = LocalDateTime.now();
        this.teacherMemo = memo;
    }

    /**
     * 시스템 자동 처리 (자동 해제)
     */
    public void confirmBySystem() {
        this.isConfirmed = true;
        this.confirmedBy = 0L;  // 0 = SYSTEM
        this.confirmedAt = LocalDateTime.now();
        this.teacherMemo = null;  // 시스템 처리는 메모 없음
    }
}
