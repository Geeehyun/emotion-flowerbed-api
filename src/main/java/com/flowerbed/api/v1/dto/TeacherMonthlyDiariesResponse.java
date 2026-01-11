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
public class TeacherMonthlyDiariesResponse {

    private String yearMonth;
    private List<EmotionListItem> emotions;
    private Integer totalCount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionListItem {
        private Long id;
        private LocalDate date;
        private Boolean isAnalyzed;
        private String coreEmotionCode;
        private List<EmotionPercent> emotions;
        private EmotionDetail coreEmotionDetail;  // 꽃 상세 정보
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionDetail {
        private String emotionCode;
        private String emotionNameKr;
        private String emotionNameEn;
        private String emotionArea;
        private String flowerNameKr;
        private String flowerNameEn;
        private String flowerMeaning;
        private String imageFile3d;
    }
}
