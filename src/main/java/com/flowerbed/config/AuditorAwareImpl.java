package com.flowerbed.config;

import com.flowerbed.api.v1.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA Auditing을 위한 AuditorAware 구현체
 *
 * SecurityContext에서 현재 인증된 사용자 정보를 가져와서
 * @CreatedBy, @LastModifiedBy에 자동으로 설정합니다.
 *
 * 인증되지 않은 요청(회원가입, 로그인 등)의 경우 "SYSTEM"을 반환합니다.
 */
@Slf4j
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않았거나 익명 사용자인 경우 "SYSTEM" 반환
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            log.debug("No authenticated user found, using SYSTEM as auditor");
            return Optional.of("SYSTEM");
        }

        // SecurityContext에서 User 객체 가져오기
        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            User user = (User) principal;
            log.debug("Current auditor: userId={}", user.getUserId());
            return Optional.of(user.getUserId());
        }

        log.warn("Unexpected principal type: {}", principal.getClass().getName());
        return Optional.of("SYSTEM");
    }
}
