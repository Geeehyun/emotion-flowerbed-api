package com.flowerbed.security;

import com.flowerbed.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * JWT 토큰 생성 - 로그인 (앱용)
     * @param userId
     * @return
     */
    public Map<String, String> generateToken(String userId) {
        Map<String, String> token = new HashMap<>();
        String accessToken = createAccessToken(userId);
        String refreshToken = createRefreshToken(userId);
        token.put("accessToken", accessToken);
        token.put("refreshToken", refreshToken);
        log.debug("[JwtUtils.generateToken] token : {}", token);
        return token;
    }

    /**
     * AccessToken 생성
     * @param userId
     * @return
     */
    public String createAccessToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim("type", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getACCESS_TOKEN_EXP()))
                .signWith(jwtConfig.getKey())
                .compact();
    }

    /**
     * RefreshToken 생성
     * @param userId
     * @return
     */
    public String createRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getREFRESH_TOKEN_EXP()))
                .signWith(jwtConfig.getKey())
                .compact();
    }

    /**
     * JWT 유효성 검증 및 사용자 정보 추출
     * @param token
     * @return
     */
    public Claims extractClaims(String token) {
        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(jwtConfig.getKey())
                .build()
                .parseSignedClaims(token);
        return claimsJws.getPayload();
    }

    /**
     * JWT 유효성 검사
     * @param token
     * @return
     */
    public boolean isTokenValid(String token) {
        try {
            return extractClaims(token).getExpiration().after(new Date());
        } catch(Exception e) {
            return false;
        }
    }

    /** RefreshToken 유효기간
     * @return long
     */
    public long getRefreshTokenExp() {
        return jwtConfig.getREFRESH_TOKEN_EXP();
    }
}
