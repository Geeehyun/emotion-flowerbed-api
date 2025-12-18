package com.flowerbed.api.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerbed.config.AnthropicConfig;
import com.flowerbed.exception.business.LlmAnalysisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Claude API 호출 클라이언트
 * - Anthropic API와 HTTP 통신
 * - 프롬프트 전송 및 응답 파싱
 * - AnthropicConfig에서 모델/토큰/온도 설정 로드
 * - llm.provider=claude일 때만 활성화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "llm.provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeApiClient implements LlmApiClient {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final AnthropicConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Claude API 호출
     */
    public String call(String prompt) {
        try {
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(prompt);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling Claude API with model: {}", config.getModel());

            ResponseEntity<String> response = restTemplate.exchange(
                    ANTHROPIC_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return extractContent(response.getBody());

        } catch (Exception e) {
            log.error("Claude API call failed", e);
            throw new LlmAnalysisException("Claude API 호출에 실패했습니다", e);
        }
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", config.getKey());
        headers.set("anthropic-version", ANTHROPIC_VERSION);
        return headers;
    }

    /**
     * 요청 바디 생성
     */
    private Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "model", config.getModel(),
                "max_tokens", config.getMaxTokens(),
                "temperature", config.getTemperature(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );
    }

    /**
     * 응답에서 content 추출
     */
    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content").get(0).path("text");

            if (content.isMissingNode()) {
                throw new LlmAnalysisException("Claude API 응답에서 content를 찾을 수 없습니다");
            }

            return content.asText();

        } catch (Exception e) {
            log.error("Failed to parse Claude API response", e);
            throw new LlmAnalysisException("Claude API 응답 파싱에 실패했습니다", e);
        }
    }
}
