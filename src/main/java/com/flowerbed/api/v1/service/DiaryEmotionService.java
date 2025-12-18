package com.flowerbed.api.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.dto.DiaryEmotionResponse;
import com.flowerbed.api.v1.dto.EmotionPercent;
import com.flowerbed.exception.business.InvalidDiaryContentException;
import com.flowerbed.api.v1.repository.FlowerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM API를 이용한 일기 감정 분석 서비스
 * - 실제 LLM 호출하여 감정 분석 (Claude 또는 OpenAI)
 * - 프롬프트 관리, 응답 파싱/검증
 * - llm.provider 설정에 따라 사용할 LLM 자동 선택
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryEmotionService {

    private final LlmApiClient llmApiClient;  // LLM API 호출 (Claude 또는 OpenAI)
    private final FlowerRepository flowerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // DB에서 조회한 유효한 감정 코드 목록 (초기화 시 캐싱)
    private Set<String> validEmotions;

    // emotion-analysis-prompt.txt 파일 로드
    private static String promptTemplate;

    static {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/emotion-analysis-prompt.txt");
            promptTemplate = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt template", e);
        }
    }

    /**
     * 서비스 초기화 시 DB에서 유효한 감정 코드 목록 조회하여 캐싱
     */
    @PostConstruct
    public void init() {
        validEmotions = flowerRepository.findAll().stream()
                .map(Emotion::getEmotionCode)
                .collect(Collectors.toSet());
        log.info("Loaded {} valid emotion codes from database", validEmotions.size());
    }

    /**
     * 일기 감정 분석 (LLM API 사용)
     * - 프롬프트 생성 → LLM 호출 → 응답 파싱/검증
     */
    public DiaryEmotionResponse analyzeDiary(String diaryContent) {

        // 1. 일기 내용 기본 검증
        validateDiaryContent(diaryContent);

        // 2. 프롬프트 생성
        String prompt = buildPrompt(diaryContent);

        // 3. LLM API 호출
        String llmResponse = llmApiClient.call(prompt);

        // 4. 응답 파싱 및 검증
        return parseAndValidateResponse(llmResponse);
    }

    /**
     * 일기 내용 기본 검증
     */
    private void validateDiaryContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidDiaryContentException("일기 내용이 비어있습니다");
        }

        if (content.length() < 10) {
            throw new InvalidDiaryContentException("일기 내용이 너무 짧습니다. 최소 10자 이상 작성해주세요");
        }

        if (content.length() > 5000) {
            throw new InvalidDiaryContentException("일기 내용이 너무 깁니다. 최대 5000자까지 가능합니다");
        }
    }

    /**
     * LLM API 프롬프트 생성
     */
    private String buildPrompt(String diaryContent) {
        return promptTemplate.replace("{DIARY_CONTENT}", diaryContent);
    }

    /**
     * LLM 응답 파싱 및 검증
     */
    private DiaryEmotionResponse parseAndValidateResponse(String llmResponse) {
        try {
            // JSON 파싱
            DiaryEmotionResponse response = objectMapper.readValue(llmResponse, DiaryEmotionResponse.class);

            // LLM이 일기를 분석할 수 없다고 판단한 경우
            if (Boolean.TRUE.equals(response.getError())) {
                log.warn("LLM rejected diary content: {}", response.getMessage());
                throw new InvalidDiaryContentException(
                        response.getMessage() != null ? response.getMessage() : "일기 내용을 분석할 수 없습니다"
                );
            }

            // 필수 필드 검증
            if (response.getCoreEmotion() == null || !validEmotions.contains(response.getCoreEmotion())) {
                log.error("Invalid emotion in LLM response: {}", response.getCoreEmotion());
                return getDefaultResponse();
            }

            if (response.getFlower() == null || response.getSummary() == null) {
                log.error("Missing required fields in LLM response");
                return getDefaultResponse();
            }

            return response;

        } catch (InvalidDiaryContentException e) {
            throw e; // 재전파
        } catch (Exception e) {
            log.error("Failed to parse LLM response", e);
            return getDefaultResponse();
        }
    }

    /**
     * 기본 응답 (분석 실패 시)
     */
    private DiaryEmotionResponse getDefaultResponse() {
        DiaryEmotionResponse response = new DiaryEmotionResponse();
        response.setSummary("일기 분석에 실패했지만 괜찮아요. 오늘도 수고하셨습니다.");
        response.setCoreEmotion("PEACE");
        response.setFlower("은방울꽃");
        response.setFloriography("행복의 재림");
        response.setReason("감정 분석에 실패하여 기본값으로 설정되었습니다.");

        EmotionPercent emotion = new EmotionPercent("PEACE", 100);
        response.setEmotions(List.of(emotion));

        return response;
    }
}
