package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.dto.DuplicateCheckResponse;
import com.flowerbed.api.v1.dto.LoginRequest;
import com.flowerbed.api.v1.dto.LoginResponse;
import com.flowerbed.api.v1.dto.SignUpRequest;
import com.flowerbed.api.v1.dto.SignUpResponse;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 인증/인가 서비스
 * - 로그인, 로그아웃, 토큰 갱신
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     * - userId 중복 체크
     * - 비밀번호 암호화
     * - 사용자 생성
     */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        // 1. userId 중복 체크
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID, "이미 사용 중인 아이디입니다");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 사용자 생성
        User user = new User(
                request.getUserId(),
                encodedPassword,
                request.getName(),
                request.getUserTypeCd(),
                request.getSchoolCode(),
                request.getSchoolNm(),
                request.getClassCode()
        );

        User savedUser = userRepository.save(user);

        log.info("User signed up: userId={}, userTypeCd={}", savedUser.getUserId(), savedUser.getUserTypeCd());

        // 4. 응답 생성
        return SignUpResponse.builder()
                .userSn(savedUser.getUserSn())
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .userTypeCd(savedUser.getUserTypeCd())
                .build();
    }

    /**
     * ID 중복 조회
     * - userId 존재 여부 확인
     */
    public DuplicateCheckResponse checkDuplicateUserId(String userId) {
        boolean isDuplicate = userRepository.existsByUserId(userId);

        return DuplicateCheckResponse.builder()
                .userId(userId)
                .isDuplicate(isDuplicate)
                .build();
    }

    /**
     * 로그인
     * - userId, password 검증
     * - AccessToken + RefreshToken 발급
     * - RefreshToken을 Redis에 저장
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "아이디 또는 비밀번호가 일치하지 않습니다"));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD,
                    "아이디 또는 비밀번호가 일치하지 않습니다");
        }

        // 3. JWT 토큰 생성 (AccessToken + RefreshToken)
        Map<String, String> tokens = jwtUtil.generateToken(String.valueOf(user.getUserSn()));

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        // 4. RefreshToken을 Redis에 저장
        redisService.saveRefreshToken(user.getUserId(), refreshToken);

        log.info("User logged in: userId={}", user.getUserId());

        // 5. 응답 생성
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userSn(user.getUserSn())
                .userId(user.getUserId())
                .name(user.getName())
                .userTypeCd(user.getUserTypeCd())
                .schoolCode(user.getSchoolCode())
                .schoolNm(user.getSchoolNm())
                .classCode(user.getClassCode())
                .build();
    }

    /**
     * 로그아웃
     * - AccessToken을 블랙리스트에 추가
     * - RefreshToken을 Redis에서 삭제
     */
    public void logout(String accessToken, String userId) {
        // 1. AccessToken을 블랙리스트에 추가 (만료 시간까지)
        Claims claims = jwtUtil.extractClaims(accessToken);
        long expirationMillis = claims.getExpiration().getTime() - System.currentTimeMillis();

        if (expirationMillis > 0) {
            redisService.addToBlacklist(accessToken, expirationMillis);
        }

        // 2. RefreshToken 삭제
        redisService.deleteRefreshToken(userId);

        log.info("User logged out: userId={}", userId);
    }

    /**
     * Access Token 갱신 (Refresh Token Rotation 방식)
     * - RefreshToken 검증
     * - Redis에 저장된 RefreshToken과 비교
     * - 새로운 AccessToken + RefreshToken 발급
     * - 기존 RefreshToken 무효화 및 새로운 RefreshToken 저장
     *
     * 보안 강화:
     * - RefreshToken도 함께 재발급하여 일회용으로 만듦
     * - 탈취된 RefreshToken은 한 번만 사용 가능
     */
    public Map<String, String> refreshAccessToken(String refreshToken) {
        // 1. RefreshToken 유효성 검증
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다");
        }

        // 2. RefreshToken에서 userSn 추출
        Claims claims = jwtUtil.extractClaims(refreshToken);
        String userSn = claims.getSubject();

        // 3. userSn으로 userId 조회
        User user = userRepository.findById(Long.parseLong(userSn))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

        // 4. Redis에 저장된 RefreshToken과 비교
        String storedRefreshToken = redisService.getRefreshToken(user.getUserId());
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "유효하지 않은 Refresh Token입니다");
        }

        // 5. 새로운 AccessToken + RefreshToken 발급
        Map<String, String> newTokens = jwtUtil.generateToken(userSn);
        String newAccessToken = newTokens.get("accessToken");
        String newRefreshToken = newTokens.get("refreshToken");

        // 6. 기존 RefreshToken 삭제 후 새로운 RefreshToken 저장 (Rotation)
        redisService.deleteRefreshToken(user.getUserId());
        redisService.saveRefreshToken(user.getUserId(), newRefreshToken);

        log.info("Tokens refreshed (Refresh Token Rotation): userId={}", user.getUserId());

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }
}
