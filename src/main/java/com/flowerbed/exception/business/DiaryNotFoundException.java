package com.flowerbed.exception.business;

import com.flowerbed.exception.ErrorCode;

public class DiaryNotFoundException extends BusinessException {

    public DiaryNotFoundException() {
        super(ErrorCode.DIARY_NOT_FOUND);
    }

    public DiaryNotFoundException(String message) {
        super(ErrorCode.DIARY_NOT_FOUND, message);
    }
}
