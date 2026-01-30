package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Emotion extends BaseAuditEntity {

    @Id
    @Column(name = "emotion_code", length = 20)
    private String emotionCode;

    @Column(name = "emotion_name_kr", nullable = false, length = 20)
    private String emotionNameKr;

    @Column(name = "emotion_name_en", nullable = false, length = 20)
    private String emotionNameEn;

    @Column(name = "emotion_description", columnDefinition = "TEXT")
    private String emotionDescription;  // 감정 설명 (정의, 상황 예시, 대처법 포함)

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

    @Column(name = "color", length = 8)
    private String color;  // 감정 색상 (HEX 코드, 예: #FF5733)

    @Column(name = "x")
    private Integer x;  // 감정 무드미터 그래프 X 좌표

    @Column(name = "y")
    private Integer y;  // 감정 무드미터 그래프 Y 좌표

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;  // 활성화 여부 (비활성화된 감정은 AI 분석에서 제외)

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
