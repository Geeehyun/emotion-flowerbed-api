package com.flowerbed.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 설정 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 설정 응답")
public class UserSettingsResponse {

    @Schema(description = "테마 색상", example = "yellow")
    private String themeColor;

    @Schema(description = "정원 배경 테마", example = "default")
    private String themeGardenBg;
}
