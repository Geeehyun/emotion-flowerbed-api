package com.flowerbed.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 응답")
public class SignUpResponse {

    @Schema(description = "사용자 일련번호", example = "1")
    private Long userSn;

    @Schema(description = "로그인 ID", example = "student1")
    private String userId;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "사용자 유형 코드", example = "STUDENT")
    private String userTypeCd;
}
