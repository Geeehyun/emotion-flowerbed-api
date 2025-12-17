package com.flowerbed.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Getter
@Configuration
public class JwtConfig {

    private final SecretKey key;
    private final long ACCESS_TOKEN_EXP = 1000L * 60 * 60 * 24 * 1; // 1일
    private final long REFRESH_TOKEN_EXP = 1000L * 60 * 60 * 24 * 365; // 1년

    public JwtConfig(@Value("${jwt.secret-key}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

}
