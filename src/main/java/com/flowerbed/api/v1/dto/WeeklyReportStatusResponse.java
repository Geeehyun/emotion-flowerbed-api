package com.flowerbed.api.v1.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 주간 리포트 상태 응답 DTO
 * - 안 읽은 리포트 / 새 리포트 존재 여부 확인용
 */
@Getter
@Builder
public class WeeklyReportStatusResponse {

    private Boolean hasUnread;  // 안 읽은 리포트가 있는지
    private Boolean hasNew;  // 새 리포트가 있는지 (알림 전송 안 된 리포트)

    /**
     * 정적 팩토리 메서드
     */
    public static WeeklyReportStatusResponse of(Boolean hasUnread, Boolean hasNew) {
        return WeeklyReportStatusResponse.builder()
                .hasUnread(hasUnread)
                .hasNew(hasNew)
                .build();
    }
}
