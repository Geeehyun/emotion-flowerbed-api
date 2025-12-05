package com.flowerbed.exception;

public class InvalidDiaryContentException extends BusinessException {

    public InvalidDiaryContentException() {
        super(ErrorCode.INVALID_DIARY_CONTENT);
    }

    public InvalidDiaryContentException(String message) {
        super(ErrorCode.INVALID_DIARY_CONTENT, message);
    }
}
