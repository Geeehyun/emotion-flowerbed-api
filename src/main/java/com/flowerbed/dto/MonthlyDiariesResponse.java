package com.flowerbed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDiariesResponse {

    private String yearMonth;
    private List<DiaryListItem> diaries;
    private Integer totalCount;
    private Boolean hasNextMonth;
    private Boolean hasPrevMonth;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryListItem {
        private Long id;
        private LocalDate date;
        private String content;
        private String coreEmotion;
        private String flower;
        private String floriography;
        private String summary;
        private List<EmotionPercent> emotions;
        private String reason;
        private FlowerDetail flowerDetail;  // 꽃 상세 정보
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlowerDetail {
        private String emotionCode;
        private String emotionNameKr;
        private String emotionNameEn;
        private String flowerNameKr;
        private String flowerNameEn;
        private String flowerMeaning;
        private String flowerMeaningStory;
        private String flowerColor;
        private String flowerColorCodes;
        private String flowerOrigin;
        private String flowerFragrance;
        private String flowerFunFact;
        private String imageFile3d;
        private String imageFileRealistic;
        private Boolean isPositive;
    }
}
