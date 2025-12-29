package com.flowerbed.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.dto.ErrorResponse;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.exception.auth.InvalidTokenExceptionCustom;
import com.flowerbed.security.JwtUtil;
import com.flowerbed.api.v1.service.RedisService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 토큰 인증 Filter
 * - JWT 토큰 유효성 검사
 * - 블랙리스트 확인 (로그아웃된 토큰 차단)
 * - 사용자 정보 조회 후 SecurityContextHolder 삽입
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserRepository userRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.debug("=================== JwtAuthenticationFilter START");
        String token = request.getHeader("Authorization");
        log.debug("=================== token : {}", token);

        if(token != null && token.startsWith("Bearer")) {
            token = token.replace("Bearer", "").replace(" ", "");

            // 1. 블랙리스트 확인 (로그아웃된 토큰 차단)
            if(redisService.isBlacklisted(token)) {
                log.debug("=================== result : 블랙리스트 토큰 (로그아웃됨)");
                handleAuthenticationFailure(request, response, "로그아웃된 토큰입니다");
                return;
            }

            // 2. 토큰 유효성 검증
            if(jwtUtil.isTokenValid(token)) {
                // 3. 인증 성공 - 사용자 정보 조회
                log.debug("=================== result : 인증성공");
                Claims claims = jwtUtil.extractClaims(token);
                String userSn = claims.getSubject();

                // 4. DB에서 사용자 정보 조회
                User user = userRepository.findById(Long.parseLong(userSn)).orElse(null);

                if(user != null) {
                    // 5. SecurityContext에 사용자 정보 저장
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("=================== User authenticated: userId={}", user.getUserId());
                } else {
                    log.warn("=================== User not found: userSn={}", userSn);
                }
            } else {
                // 인증 실패
                log.debug("=================== result : 인증실패 (토큰 만료 또는 유효하지 않음)");
                handleAuthenticationFailure(request, response, "유효하지 않은 토큰입니다");
                return;
            }
        }

        log.debug("=================== JwtAuthenticationFilter END");
        filterChain.doFilter(request, response);
    }

    /**
     * 인증 실패 처리
     */
    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        InvalidTokenExceptionCustom ex = new InvalidTokenExceptionCustom();
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.getHttpStatus().value(),
                errorCode.getHttpStatus().getReasonPhrase(),
                errorCode.getCode(),
                message,
                request.getRequestURI()
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 인증 실패 에러로그 출력
        log.error("[JwtAuthenticationFilter] Authentication failed: {}", message);

        // 인증 실패 response return
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
