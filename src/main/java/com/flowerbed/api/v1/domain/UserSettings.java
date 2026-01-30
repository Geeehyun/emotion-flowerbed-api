package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings {

    @Id
    @Column(name = "user_sn")
    private Long userSn;

    @Column(name = "theme_color", length = 20)
    private String themeColor;

    @Column(name = "theme_garden_bg", length = 50)
    private String themeGardenBg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_sn")
    private User user;

    /**
     * 기본 설정으로 생성
     */
    public static UserSettings createDefault(User user) {
        UserSettings settings = new UserSettings();
        settings.user = user;
        settings.userSn = user.getUserSn();
        settings.themeColor = "yellow";
        settings.themeGardenBg = "default";
        settings.createdAt = LocalDateTime.now();
        settings.createdBy = user.getUserId();
        settings.updatedAt = LocalDateTime.now();
        return settings;
    }

    /**
     * 테마 색상 변경
     */
    public void updateThemeColor(String themeColor) {
        this.themeColor = themeColor;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 정원 배경 테마 변경
     */
    public void updateThemeGardenBg(String themeGardenBg) {
        this.themeGardenBg = themeGardenBg;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 설정 일괄 변경
     */
    public void updateSettings(String themeColor, String themeGardenBg) {
        this.themeColor = themeColor;
        this.themeGardenBg = themeGardenBg;
        this.updatedAt = LocalDateTime.now();
    }
}
