package com.flowerbed.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DANGER 상태 해제 요청 DTO
 * - 선생님이 위험 학생을 상담하고 해제할 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResolveDangerRequest {

    /**
     * 해제 사유 (필수)
     * 예시: "학생 상담 완료. 최근 일주일간 감정 상태 개선 확인. 학부모와 통화하여 가정 내 문제 해결 중임을 확인함."
     */
    @NotBlank(message = "해제 사유를 입력해주세요")
    private String memo;
}
