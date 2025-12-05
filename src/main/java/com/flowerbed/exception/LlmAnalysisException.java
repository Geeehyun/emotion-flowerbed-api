package com.flowerbed.exception;

public class LlmAnalysisException extends BusinessException {

    public LlmAnalysisException() {
        super(ErrorCode.LLM_ANALYSIS_FAILED);
    }

    public LlmAnalysisException(String message) {
        super(ErrorCode.LLM_ANALYSIS_FAILED, message);
    }

    public LlmAnalysisException(String message, Throwable cause) {
        super(ErrorCode.LLM_ANALYSIS_FAILED, message);
        initCause(cause);
    }
}
