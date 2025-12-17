package com.flowerbed.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "emotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Emotion {

    @Id
    @Column(name = "emotion_code", length = 20)
    private String emotionCode;

    @Column(name = "emotion_name_kr", nullable = false, length = 20)
    private String emotionNameKr;

    @Column(name = "emotion_name_en", nullable = false, length = 20)
    private String emotionNameEn;

    @Column(name = "flower_name_kr", nullable = false, length = 50)
    private String flowerNameKr;

    @Column(name = "flower_name_en", length = 50)
    private String flowerNameEn;

    @Column(name = "flower_meaning", nullable = false, length = 100)
    private String flowerMeaning;

    @Column(name = "flower_meaning_story", length = 1000)
    private String flowerMeaningStory;

    @Column(name = "flower_color", length = 50)
    private String flowerColor;

    @Column(name = "flower_color_codes", length = 500)
    private String flowerColorCodes;

    @Column(name = "flower_origin", length = 100)
    private String flowerOrigin;

    @Column(name = "flower_fragrance", length = 50)
    private String flowerFragrance;

    @Column(name = "flower_fun_fact", length = 1000)
    private String flowerFunFact;

    @Column(name = "image_file_3d", nullable = false, length = 100)
    private String imageFile3d;

    @Column(name = "image_file_realistic", nullable = false, length = 100)
    private String imageFileRealistic;

    @Column(name = "area", nullable = false, length = 10)
    private String area;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Emotion(String emotionCode, String emotionNameKr, String emotionNameEn,
                  String flowerNameKr, String flowerMeaning,
                  String imageFile3d, String imageFileRealistic,
                  String area, Integer displayOrder) {
        this.emotionCode = emotionCode;
        this.emotionNameKr = emotionNameKr;
        this.emotionNameEn = emotionNameEn;
        this.flowerNameKr = flowerNameKr;
        this.flowerMeaning = flowerMeaning;
        this.imageFile3d = imageFile3d;
        this.imageFileRealistic = imageFileRealistic;
        this.area = area;
        this.displayOrder = displayOrder;
    }

    public void updateBasicInfo(String emotionNameKr, String emotionNameEn,
                                  String flowerNameKr, String flowerNameEn, String flowerMeaning) {
        this.emotionNameKr = emotionNameKr;
        this.emotionNameEn = emotionNameEn;
        this.flowerNameKr = flowerNameKr;
        this.flowerNameEn = flowerNameEn;
        this.flowerMeaning = flowerMeaning;
    }

    public void updateDetailInfo(String flowerColor, String flowerColorCodes, String flowerOrigin,
                                  String flowerFragrance,
                                  String flowerMeaningStory, String flowerFunFact) {
        this.flowerColor = flowerColor;
        this.flowerColorCodes = flowerColorCodes;
        this.flowerOrigin = flowerOrigin;
        this.flowerFragrance = flowerFragrance;
        this.flowerMeaningStory = flowerMeaningStory;
        this.flowerFunFact = flowerFunFact;
    }

    public void updateImages(String imageFile3d, String imageFileRealistic) {
        this.imageFile3d = imageFile3d;
        this.imageFileRealistic = imageFileRealistic;
    }
}
