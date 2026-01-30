package com.flowerbed.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 설정 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 설정 수정 요청")
public class UserSettingsUpdateRequest {

    @Size(max = 20, message = "테마 색상은 20자 이하여야 합니다")
    @Schema(description = "테마 색상", example = "blue")
    private String themeColor;

    @Size(max = 50, message = "정원 배경 테마는 50자 이하여야 합니다")
    @Schema(description = "정원 배경 테마", example = "spring")
    private String themeGardenBg;
}
