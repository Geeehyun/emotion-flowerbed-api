package com.flowerbed.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class SignUpRequest {

    @NotBlank(message = "아이디를 입력해주세요")
    @Schema(description = "로그인 ID", example = "student1")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Schema(description = "비밀번호", example = "1234")
    private String password;

    @NotBlank(message = "이름을 입력해주세요")
    @Schema(description = "이름", example = "홍길동")
    private String name;

    @NotBlank(message = "사용자 유형을 선택해주세요")
    @Pattern(regexp = "^(STUDENT|TEACHER)$", message = "사용자 유형은 STUDENT 또는 TEACHER만 가능합니다")
    @Schema(description = "사용자 유형 코드 (STUDENT/TEACHER)", example = "STUDENT")
    private String userTypeCd;

    @NotBlank(message = "학교 코드를 입력해주세요")
    @Schema(description = "학교 코드", example = "1111")
    private String schoolCode;

    @NotBlank(message = "학교명을 입력해주세요")
    @Schema(description = "학교명", example = "예시초등학교")
    private String schoolNm;

    @NotBlank(message = "학급 코드를 입력해주세요")
    @Schema(description = "학급 코드", example = "301")
    private String classCode;
}
