package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.User;
import lombok.*;

/**
 * 학생 정보 응답 DTO
 * - 선생님이 학생 목록을 조회할 때 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    /**
     * 사용자 일련번호
     */
    private Long userSn;

    /**
     * 로그인 ID
     */
    private String userId;

    /**
     * 이름
     */
    private String name;

    /**
     * 학교 코드
     */
    private String schoolCode;

    /**
     * 학교명
     */
    private String schoolNm;

    /**
     * 반 코드
     */
    private String classCode;

    /**
     * 감정 제어 활동 코드
     * - DEEP_BREATHING: 심호흡
     * - WALK: 산책
     * - DRAW: 그림 그리기
     * - TALK: 대화하기
     */
    private String emotionControlCd;

    /**
     * 위험도 레벨 (NORMAL, CAUTION, DANGER)
     */
    private String riskLevel;

    /**
     * 위험도 판정 사유
     */
    private String riskReason;

    /**
     * 연속된 감정 영역
     */
    private String riskContinuousArea;

    /**
     * 연속 일수
     */
    private Integer riskContinuousDays;

    /**
     * 최근 감정 영역 (red, yellow, blue, green)
     */
    private String recentEmotionArea;

    /**
     * 최근 핵심 감정 코드
     */
    private String recentCoreEmotionCd;

    /**
     * 최근 핵심 감정 한글명
     */
    private String recentCoreEmotionNameKr;

    /**
     * 최근 핵심 감정 이미지 (3D 이미지)
     */
    private String recentCoreEmotionImage;

    /**
     * User 엔티티를 StudentResponse로 변환
     */
    public static StudentResponse from(User user) {
        return StudentResponse.builder()
                .userSn(user.getUserSn())
                .userId(user.getUserId())
                .name(user.getName())
                .schoolCode(user.getSchoolCode())
                .schoolNm(user.getSchoolNm())
                .classCode(user.getClassCode())
                .emotionControlCd(user.getEmotionControlCd())
                .riskLevel(user.getRiskLevel())
                .riskReason(user.getRiskReason())
                .riskContinuousArea(user.getRiskContinuousArea())
                .riskContinuousDays(user.getRiskContinuousDays())
                .build();
    }
}
