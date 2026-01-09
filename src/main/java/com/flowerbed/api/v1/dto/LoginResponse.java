package com.flowerbed.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponse {

    @Schema(description = "Access Token (1일 유효)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token (1년 유효)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "사용자 일련번호", example = "1")
    private Long userSn;

    @Schema(description = "로그인 ID", example = "student1")
    private String userId;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "사용자 유형 코드", example = "STUDENT")
    private String userTypeCd;

    @Schema(description = "학교 코드", example = "1111")
    private String schoolCode;

    @Schema(description = "학교명", example = "예시초등학교")
    private String schoolNm;

    @Schema(description = "학급 코드", example = "301")
    private String classCode;
}
