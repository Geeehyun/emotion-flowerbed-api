package com.flowerbed.api.v1.dto;

import com.flowerbed.api.v1.domain.CodeGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 코드 그룹 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코드 그룹 응답")
public class CodeGroupResponse {

    @Schema(description = "그룹 코드", example = "USER_TYPE")
    private String groupCode;

    @Schema(description = "그룹명", example = "사용자 유형")
    private String groupName;

    @Schema(description = "설명", example = "사용자 유형 구분 (학생/교사/관리자)")
    private String description;

    @Schema(description = "수정 가능 여부", example = "false")
    private Boolean isEditable;

    @Schema(description = "표시 순서", example = "1")
    private Integer displayOrder;

    @Schema(description = "코드 목록")
    private List<CodeResponse> codes;

    public static CodeGroupResponse from(CodeGroup codeGroup) {
        return CodeGroupResponse.builder()
                .groupCode(codeGroup.getGroupCode())
                .groupName(codeGroup.getGroupName())
                .description(codeGroup.getDescription())
                .isEditable(codeGroup.getIsEditable())
                .displayOrder(codeGroup.getDisplayOrder())
                .codes(codeGroup.getCodes().stream()
                        .filter(code -> code.getIsActive())
                        .map(CodeResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    public static CodeGroupResponse fromWithoutCodes(CodeGroup codeGroup) {
        return CodeGroupResponse.builder()
                .groupCode(codeGroup.getGroupCode())
                .groupName(codeGroup.getGroupName())
                .description(codeGroup.getDescription())
                .isEditable(codeGroup.getIsEditable())
                .displayOrder(codeGroup.getDisplayOrder())
                .build();
    }
}
