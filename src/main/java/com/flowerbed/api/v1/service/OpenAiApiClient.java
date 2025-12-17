package com.flowerbed.api.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerbed.config.OpenAiConfig;
import com.flowerbed.exception.LlmAnalysisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI API 호출 클라이언트
 * - OpenAI Chat Completions API와 HTTP 통신
 * - 프롬프트 전송 및 응답 파싱
 * - OpenAiConfig에서 모델/토큰/온도 설정 로드
 * - llm.provider=openai일 때만 활성화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiApiClient implements LlmApiClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final OpenAiConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * OpenAI API 호출
     */
    @Override
    public String call(String prompt) {
        try {
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(prompt);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Calling OpenAI API with model: {}", config.getModel());

            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return extractContent(response.getBody());

        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new LlmAnalysisException("OpenAI API 호출에 실패했습니다", e);
        }
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getKey());
        return headers;
    }

    /**
     * 요청 바디 생성
     */
    private Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "model", config.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens()
        );
    }

    /**
     * 응답에서 content 추출
     */
    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").get(0).path("message").path("content");

            if (content.isMissingNode()) {
                throw new LlmAnalysisException("OpenAI API 응답에서 content를 찾을 수 없습니다");
            }

            return content.asText();

        } catch (Exception e) {
            log.error("Failed to parse OpenAI API response", e);
            throw new LlmAnalysisException("OpenAI API 응답 파싱에 실패했습니다", e);
        }
    }
}
