package com.flowerbed.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 401 UnAuthorized
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰 입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다"),

    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력 값이 올바르지 않습니다"),
    DUPLICATE_USER_ID(HttpStatus.BAD_REQUEST, "DUPLICATE_USER_ID", "이미 사용 중인 아이디입니다"),
    INVALID_DIARY_CONTENT(HttpStatus.BAD_REQUEST, "INVALID_DIARY_CONTENT", "일기 내용이 분석 불가능합니다"),
    DUPLICATE_DIARY_DATE(HttpStatus.BAD_REQUEST, "DUPLICATE_DIARY_DATE", "해당 날짜에 이미 일기가 존재합니다"),
    NO_SCHOOL_INFO(HttpStatus.BAD_REQUEST, "DUPLICATE_DIARY_DATE", "학교 정보가 올바르지 않습니다."),
    INVALID_RISK_LEVEL(HttpStatus.BAD_REQUEST, "DUPLICATE_DIARY_DATE", "학교 정보가 올바르지 않습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없는 사용자입니다."),

    // 404 Not Found
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_NOT_FOUND", "일기를 찾을 수 없습니다"),
    DIARY_NOT_ANALYZED(HttpStatus.NOT_FOUND, "DIARY_NOT_ANALYZED", "일기가 아직 분석되지 않았습니다"),
    WEEKLY_REPORT_NOT_ANALYZED(HttpStatus.NOT_FOUND, "WEEKLY_REPORT_NOT_ANALYZED", "주간 리포트가 아직 분석되지 않았습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    NO_STUDENTS_FOUND(HttpStatus.NOT_FOUND, "NO_STUDENTS_FOUND", "담당 학생이 없습니다"),
    FLOWER_NOT_FOUND(HttpStatus.NOT_FOUND, "FLOWER_NOT_FOUND", "꽃 정보를 찾을 수 없습니다"),
    CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "CODE_NOT_FOUND", "코드 정보를 찾을 수 없습니다"),

    // 500 Internal Server Error
    LLM_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_ANALYSIS_FAILED", "AI 감정 분석에 실패했습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
