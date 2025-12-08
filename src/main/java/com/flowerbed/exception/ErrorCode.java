package com.flowerbed.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력 값이 올바르지 않습니다"),
    INVALID_DIARY_CONTENT(HttpStatus.BAD_REQUEST, "INVALID_DIARY_CONTENT", "일기 내용이 분석 불가능합니다"),
    DUPLICATE_DIARY_DATE(HttpStatus.BAD_REQUEST, "DUPLICATE_DIARY_DATE", "해당 날짜에 이미 일기가 존재합니다"),

    // 404 Not Found
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_NOT_FOUND", "일기를 찾을 수 없습니다"),
    DIARY_NOT_ANALYZED(HttpStatus.NOT_FOUND, "DIARY_NOT_ANALYZED", "일기가 아직 분석되지 않았습니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    FLOWER_NOT_FOUND(HttpStatus.NOT_FOUND, "FLOWER_NOT_FOUND", "꽃 정보를 찾을 수 없습니다"),

    // 500 Internal Server Error
    LLM_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_ANALYSIS_FAILED", "AI 감정 분석에 실패했습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
