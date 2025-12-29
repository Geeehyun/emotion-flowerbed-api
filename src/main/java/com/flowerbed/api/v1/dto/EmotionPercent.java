package com.flowerbed.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 감정 퍼센트 DTO
 * - 일기 분석 결과에서 각 감정의 비율과 색상 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmotionPercent {
    /**
     * 감정 코드 (예: JOYFUL, SAD, ANGRY)
     */
    private String emotion;

    /**
     * 감정 비율 (0-100)
     */
    private Integer percent;

    /**
     * 감정 색상 (HEX 코드, 예: #FF5733)
     */
    private String color;

    /**
     * color 없이 생성하는 생성자 (하위 호환성)
     */
    public EmotionPercent(String emotion, Integer percent) {
        this.emotion = emotion;
        this.percent = percent;
    }
}
