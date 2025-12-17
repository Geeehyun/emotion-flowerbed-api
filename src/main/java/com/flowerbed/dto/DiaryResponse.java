package com.flowerbed.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiaryResponse {

    private Long diaryId;
    private LocalDate diaryDate;
    private String content;
    private String summary;
    private String coreEmotionCode;
    private String emotionReason;
    private String flowerName;
    private String flowerMeaning;
    private List<EmotionPercent> emotions;
    private Boolean isAnalyzed;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private FlowerDetail flowerDetail;  // 꽃 상세정보

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
