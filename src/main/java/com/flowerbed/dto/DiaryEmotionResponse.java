package com.flowerbed.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiaryEmotionResponse {

    // LLM이 일기를 분석할 수 없다고 판단한 경우
    private Boolean error;
    private String message;

    // 분석 성공 시
    private String summary;
    private List<EmotionPercent> emotions;
    private String coreEmotion;
    private String reason;
    private String flower;
    private String floriography;
}
