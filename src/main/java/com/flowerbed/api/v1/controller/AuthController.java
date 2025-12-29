package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.LoginRequest;
import com.flowerbed.api.v1.dto.LoginResponse;
import com.flowerbed.api.v1.dto.RefreshRequest;
import com.flowerbed.api.v1.service.AuthService;
import com.flowerbed.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증/인가 API Controller
 *
 * 로그인, 로그아웃, 토큰 갱신 기능을 제공합니다.
 * JWT 기반 인증을 사용하며, RefreshToken은 Redis에 저장됩니다.
 *
 * API 종류:
 * 1. POST /v1/auth/login - 로그인
 * 2. POST /v1/auth/logout - 로그아웃
 * 3. POST /v1/auth/refresh - Access Token 갱신
 */
@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인
     *
     * 사용자 인증 후 JWT 토큰을 발급합니다.
     * - AccessToken: 1일 유효
     * - RefreshToken: 1년 유효 (Redis에 저장)
     *
     * @param request 로그인 요청 (userId, password)
     * @return LoginResponse (accessToken, refreshToken, 사용자 정보)
     *
     * Response 구조:
     * - accessToken: API 요청 시 사용할 토큰
     * - refreshToken: AccessToken 갱신 시 사용할 토큰
     * - userSn: 사용자 일련번호
     * - userId: 로그인 ID
     * - name: 이름
     * - userTypeCd: 사용자 유형 코드 (STUDENT/TEACHER/ADMIN)
     * - emotionControlCd: 감정 제어 활동 코드 (DEEP_BREATHING/WALK/DRAW/TALK)
     *
     * 비즈니스 로직:
     * 1. userId로 사용자 조회
     * 2. 비밀번호 검증 (BCrypt)
     * 3. JWT 토큰 생성 (AccessToken + RefreshToken)
     * 4. RefreshToken을 Redis에 저장
     * 5. 응답 반환
     *
     * 사용 예시:
     * ```
     * POST /v1/auth/login
     * {
     *   "userId": "student1",
     *   "password": "1234"
     * }
     * ```
     *
     * !! 주의 !!
     * - DB에 저장된 비밀번호는 BCrypt로 암호화되어 있어야 합니다
     * - 테스트 시 평문 비밀번호를 BCrypt로 암호화하여 DB 업데이트 필요
     */
    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급합니다")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     *
     * 사용자의 토큰을 무효화합니다.
     * - AccessToken을 블랙리스트에 추가
     * - RefreshToken을 Redis에서 삭제
     *
     * @param authorization Authorization 헤더 (Bearer {accessToken})
     * @return 성공 메시지
     *
     * 비즈니스 로직:
     * 1. Authorization 헤더에서 AccessToken 추출
     * 2. SecurityContext에서 인증된 사용자 정보 조회
     * 3. AccessToken을 블랙리스트에 추가 (만료 시간까지)
     * 4. RefreshToken을 Redis에서 삭제
     *
     * 사용 예시:
     * ```
     * POST /v1/auth/logout
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * ```
     *
     * !! 주의 !!
     * - 로그아웃 후 AccessToken은 만료 시간까지 블랙리스트에 유지됩니다
     * - JwtAuthenticationFilter에서 블랙리스트 토큰을 차단합니다
     */
    @Operation(summary = "로그아웃", description = "사용자의 토큰을 무효화합니다")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authorization) {

        // Bearer 토큰 추출
        String accessToken = authorization.substring(7);

        // SecurityContext에서 인증된 사용자 정보 조회
        String userSn = String.valueOf(SecurityUtil.getCurrentUserSn());

        // 로그아웃 처리
        authService.logout(accessToken, userSn);

        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다"));
    }

    /**
     * Access Token 갱신 (Refresh Token Rotation)
     *
     * RefreshToken을 사용하여 새로운 AccessToken과 RefreshToken을 발급합니다.
     * AccessToken 만료 시 호출하여 재인증 없이 토큰을 갱신할 수 있습니다.
     *
     * @param request RefreshToken
     * @return 새로운 AccessToken + RefreshToken
     *
     * Response 구조:
     * - accessToken: 새로 발급된 AccessToken
     * - refreshToken: 새로 발급된 RefreshToken (보안 강화)
     *
     * 비즈니스 로직:
     * 1. RefreshToken 유효성 검증
     * 2. RefreshToken에서 userSn 추출
     * 3. Redis에 저장된 RefreshToken과 비교
     * 4. 일치하면 새로운 AccessToken + RefreshToken 발급
     * 5. 기존 RefreshToken 무효화 (일회용)
     *
     * 사용 예시:
     * ```
     * POST /v1/auth/refresh
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * ```
     *
     * !! 중요 !!
     * - Refresh Token Rotation 방식으로 보안 강화
     * - 기존 RefreshToken은 사용 후 무효화됨 (한 번만 사용 가능)
     * - 새로 발급된 RefreshToken을 반드시 저장해야 함
     * - RefreshToken이 만료되었거나 Redis에 없으면 실패합니다
     * - 로그아웃한 사용자는 RefreshToken이 삭제되어 갱신할 수 없습니다
     */
    @Operation(summary = "토큰 갱신", description = "RefreshToken을 사용하여 새로운 AccessToken과 RefreshToken을 발급합니다 (Refresh Token Rotation)")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest request) {
        Map<String, String> tokens = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(tokens);
    }
}
