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
public class UserEmotionFlowerResponse {

    private List<EmotionFlowerItem> items;
    private Integer totalCount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionFlowerItem {
        private String emotion;
        private String emotionCode;
        private String flowerName;
        private String flowerMeaning;
        private Integer count;  // 해당 감정이 나타난 횟수
        private List<String> dates;  // 해당 감정의 일기 날짜 목록 (YYYY-MM-DD)
        private FlowerDetail flowerDetail;  // 꽃 상세정보
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
        private String area;
    }
}
