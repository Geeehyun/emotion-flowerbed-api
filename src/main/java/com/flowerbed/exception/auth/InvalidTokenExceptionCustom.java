package com.flowerbed.exception.auth;

import com.flowerbed.exception.ErrorCode;

public class InvalidTokenExceptionCustom extends CustomAuthException {

    public InvalidTokenExceptionCustom() {
        super(ErrorCode.INVALID_TOKEN, "유효하지 않은 토큰 입니다.");
    }

    public InvalidTokenExceptionCustom(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }

    public InvalidTokenExceptionCustom(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
