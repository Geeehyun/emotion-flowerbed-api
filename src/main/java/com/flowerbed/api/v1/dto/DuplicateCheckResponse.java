package com.flowerbed.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ID 중복 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ID 중복 조회 응답")
public class DuplicateCheckResponse {

    @Schema(description = "조회한 ID", example = "student1")
    private String userId;

    @Schema(description = "중복 여부 (true: 중복, false: 사용 가능)", example = "false")
    private boolean isDuplicate;
}
