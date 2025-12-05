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
    private String coreEmotion;
    private String emotionReason;
    private String flowerName;
    private String flowerMeaning;
    private List<EmotionPercent> emotions;
    private Boolean isAnalyzed;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
