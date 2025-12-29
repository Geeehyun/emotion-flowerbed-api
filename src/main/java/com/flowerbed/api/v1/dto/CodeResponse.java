package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.Code;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 코드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코드 응답")
public class CodeResponse {

    @Schema(description = "코드 ID", example = "1")
    private Long codeId;

    @Schema(description = "그룹 코드", example = "USER_TYPE")
    private String groupCode;

    @Schema(description = "코드값", example = "STUDENT")
    private String code;

    @Schema(description = "코드명", example = "학생")
    private String codeName;

    @Schema(description = "설명", example = "일반 학생 사용자")
    private String description;

    @Schema(description = "활성 여부", example = "true")
    private Boolean isActive;

    @Schema(description = "표시 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "확장 필드1", example = "5")
    private String extraValue1;

    @Schema(description = "확장 필드2", example = "easy")
    private String extraValue2;

    @Schema(description = "확장 필드3", example = "mental")
    private String extraValue3;

    public static CodeResponse from(Code code) {
        return CodeResponse.builder()
                .codeId(code.getCodeId())
                .groupCode(code.getCodeGroup().getGroupCode())
                .code(code.getCode())
                .codeName(code.getCodeName())
                .description(code.getDescription())
                .isActive(code.getIsActive())
                .displayOrder(code.getDisplayOrder())
                .extraValue1(code.getExtraValue1())
                .extraValue2(code.getExtraValue2())
                .extraValue3(code.getExtraValue3())
                .build();
    }
}
