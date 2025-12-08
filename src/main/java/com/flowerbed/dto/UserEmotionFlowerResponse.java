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
        private String flowerName;
        private String flowerMeaning;
        private Integer count;  // 해당 감정이 나타난 횟수
    }
}
