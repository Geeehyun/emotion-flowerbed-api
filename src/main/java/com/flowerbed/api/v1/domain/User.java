package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_sn = ?")
@Where(clause = "deleted_at IS NULL")
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_sn")
    private Long userSn;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "user_type_cd", length = 50)
    private String userTypeCd;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "school_nm")
    private String schoolNm;

    @Column(name = "class_code")
    private String classCode;

    @Column(name = "emotion_control_cd")
    private String emotionControlCd;

    @Column(name = "risk_level", length = 20)
    private String riskLevel = "NORMAL";

    @Column(name = "risk_continuous_area", length = 20)
    private String riskContinuousArea;

    @Column(name = "risk_continuous_days")
    private Integer riskContinuousDays = 0;

    @Column(name = "risk_reason", columnDefinition = "TEXT")
    private String riskReason;

    @Column(name = "risk_last_checked_date")
    private java.time.LocalDate riskLastCheckedDate;

    @Column(name = "risk_updated_at")
    private java.time.LocalDateTime riskUpdatedAt;

    @Column(name = "danger_resolved_by")
    private Long dangerResolvedBy;

    @Column(name = "danger_resolved_at")
    private java.time.LocalDateTime dangerResolvedAt;

    @Column(name = "danger_resolve_memo", columnDefinition = "TEXT")
    private String dangerResolveMemo;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diary> diaries = new ArrayList<>();

    public User(String userId, String password, String name) {
        this.userId = userId;
        this.password = password;
        this.name = name;
    }

    public User(String userId, String password, String name, String userTypeCd) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.userTypeCd = userTypeCd;
    }

    /**
     * 위험도 상태 업데이트
     */
    public void updateRiskStatus(String riskLevel, String continuousArea, Integer continuousDays, String reason) {
        this.riskLevel = riskLevel;
        this.riskContinuousArea = continuousArea;
        this.riskContinuousDays = continuousDays;
        this.riskReason = reason;
        this.riskLastCheckedDate = java.time.LocalDate.now();
        this.riskUpdatedAt = java.time.LocalDateTime.now();
    }

    /**
     * DANGER 상태 해제 처리
     */
    public void resolveDanger(Long teacherUserSn, String memo) {
        this.dangerResolvedBy = teacherUserSn;
        this.dangerResolvedAt = java.time.LocalDateTime.now();
        this.dangerResolveMemo = memo;
    }

    /**
     * DANGER 해제 정보 초기화 (실제 NORMAL/CAUTION으로 변경 시)
     */
    public void clearDangerResolveInfo() {
        this.dangerResolvedBy = null;
        this.dangerResolvedAt = null;
        this.dangerResolveMemo = null;
    }
}
