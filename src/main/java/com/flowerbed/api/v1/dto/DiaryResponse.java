package com.flowerbed.api.v1.dto;

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

    // 감정 조절 팁 관련 (분석 API에서만 제공)
    private Boolean showEmotionControlTip;  // 감정 조절 팁 표시 여부
    private Integer consecutiveSameAreaDays;  // 연속된 같은 영역 일수 (3 또는 5)
    private String repeatedEmotionArea;  // 반복된 감정 영역 (red, yellow, blue, green)

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
