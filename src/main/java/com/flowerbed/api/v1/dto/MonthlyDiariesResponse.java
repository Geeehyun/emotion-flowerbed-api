package com.flowerbed.api.v1.dto;

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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryListItem {
        private Long id;
        private LocalDate date;
        private String content;
        private Boolean isAnalyzed;
        private String coreEmotionCode;
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
        private String emotionDescription;  // 감정 설명 (정의, 상황 예시, 대처법)
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
        private String area;
    }
}
