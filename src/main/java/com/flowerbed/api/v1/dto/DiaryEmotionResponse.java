package com.flowerbed.api.v1.dto;

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
    private List<String> keywords;  // 핵심 감정 관련 키워드 (최대 3개)

    // 위험도 분석 (2단계 - 키워드 탐지)
    private String riskLevel;  // normal, caution, danger
    private String riskReason;  // 위험도 판정 사유
    private List<String> concernKeywords;  // 탐지된 우려 키워드
}
