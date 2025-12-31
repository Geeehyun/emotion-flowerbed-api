package com.flowerbed.security;

import com.flowerbed.api.v1.domain.User;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.exception.business.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 관련 유틸리티 클래스
 * - SecurityContext에서 인증된 사용자 정보를 추출합니다.
 */
public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 User 객체를 가져옵니다.
     *
     * @return User 인증된 사용자 엔티티
     * @throws BusinessException 인증되지 않은 경우
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "인증되지 않은 사용자입니다");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof User)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "인증 정보가 올바르지 않습니다");
        }

        return (User) principal;
    }

    /**
     * 현재 인증된 사용자의 userSn을 가져옵니다.
     *
     * @return userSn 사용자 일련번호
     * @throws BusinessException 인증되지 않은 경우
     */
    public static Long getCurrentUserSn() {
        return getCurrentUser().getUserSn();
    }

    /**
     * 현재 인증된 사용자의 userId를 가져옵니다.
     *
     * @return userId 사용자 로그인 ID
     * @throws BusinessException 인증되지 않은 경우
     */
    public static String getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    /**
     * 현재 인증된 사용자가 관리자인지 확인합니다.
     *
     * @return true: 관리자, false: 일반 사용자
     */
    public static boolean isAdmin() {
        User user = getCurrentUser();
        return "ADMIN".equals(user.getUserTypeCd());
    }

    /**
     * 현재 인증된 사용자가 학생인지 확인합니다.
     *
     * @return true: 학생, false: 그 외
     */
    public static boolean isStudent() {
        User user = getCurrentUser();
        return "STUDENT".equals(user.getUserTypeCd());
    }

    /**
     * 현재 인증된 사용자가 선생님인지 확인합니다.
     *
     * @return true: 선생님, false: 그 외
     */
    public static boolean isTeacher() {
        User user = getCurrentUser();
        return "TEACHER".equals(user.getUserTypeCd());
    }

    /**
     * 현재 인증된 사용자가 관리자인지 확인하고, 아니면 예외를 발생시킵니다.
     *
     * @throws BusinessException 관리자가 아닌 경우
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }

    /**
     * 현재 인증된 사용자가 학생인지 확인하고, 아니면 예외를 발생시킵니다.
     *
     * @throws BusinessException 학생이 아닌 경우
     */
    public static void requireStudent() {
        if (!isStudent()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "학생 권한이 필요합니다.");
        }
    }

    /**
     * 현재 인증된 사용자가 선생님인지 확인하고, 아니면 예외를 발생시킵니다.
     *
     * @throws BusinessException 선생님이 아닌 경우
     */
    public static void requireTeacher() {
        if (!isTeacher()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "선생님 권한이 필요합니다.");
        }
    }
}
