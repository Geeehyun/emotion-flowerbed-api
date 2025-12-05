package com.flowerbed.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerbed.dto.DiaryEmotionResponse;
import com.flowerbed.dto.EmotionPercent;
import com.flowerbed.exception.InvalidDiaryContentException;
import com.flowerbed.exception.LlmAnalysisException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryEmotionService {

    private final ClaudeApiClient claudeApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> VALID_EMOTIONS = Set.of(
            "JOY", "HAPPINESS", "GRATITUDE", "EXCITEMENT", "PEACE", "ACHIEVEMENT",
            "LOVE", "HOPE", "VITALITY", "FUN", "SADNESS", "LONELINESS",
            "ANXIETY", "ANGER", "FATIGUE", "REGRET", "LETHARGY", "CONFUSION",
            "DISAPPOINTMENT", "BOREDOM"
    );

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
     * 일기 감정 분석
     */
    public DiaryEmotionResponse analyzeDiary(String diaryContent) {

        // 1. 일기 내용 기본 검증
        validateDiaryContent(diaryContent);

        // 2. 프롬프트 생성
        String prompt = buildPrompt(diaryContent);

        // 3. Claude API 호출
        String llmResponse = claudeApiClient.call(prompt);

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
     * Claude API 프롬프트 생성
     */
    private String buildPrompt(String diaryContent) {
        return promptTemplate.replace("{DIARY_CONTENT}", diaryContent);
    }

    /**
     * Claude 응답 파싱 및 검증
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
            if (response.getCoreEmotion() == null || !VALID_EMOTIONS.contains(response.getCoreEmotion())) {
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
