package com.flowerbed.api.v1.service;

/**
 * LLM API 클라이언트 인터페이스
 * - Claude, OpenAI 등 다양한 LLM 제공자를 추상화
 * - 구현체: ClaudeApiClient, OpenAiApiClient
 * - application.yml의 llm.provider 설정으로 전환 가능
 */
public interface LlmApiClient {

    /**
     * LLM API 호출하여 응답 받기
     * @param prompt 전송할 프롬프트
     * @return LLM 응답 텍스트
     */
    String call(String prompt);
}
