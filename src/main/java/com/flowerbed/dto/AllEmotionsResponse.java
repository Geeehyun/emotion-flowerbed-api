package com.flowerbed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllEmotionsResponse {

    private List<EmotionItem> emotions;
    private Integer totalCount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionItem {
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
        private String area;
        private Integer displayOrder;
    }
}
