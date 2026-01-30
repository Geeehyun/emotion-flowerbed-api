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
import java.util.Optional;
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

    // emotion-analysis-prompt.txt 템플릿
    private String promptTemplateRaw;

    // DB 감정 정보가 주입된 최종 프롬프트 템플릿
    private String promptTemplate;

    /**
     * 서비스 초기화
     * 1. 프롬프트 템플릿 파일 로드
     * 2. DB에서 활성화된 감정 정보 조회
     * 3. 프롬프트에 감정 정보 주입
     */
    @PostConstruct
    public void init() {
        try {
            // 1. 프롬프트 템플릿 파일 로드
            ClassPathResource resource = new ClassPathResource("prompts/emotion-analysis-prompt.txt");
            promptTemplateRaw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 2. DB에서 활성화된 감정 정보만 조회 (AI 분석용)
            List<Emotion> activeEmotions = flowerRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
            validEmotions = activeEmotions.stream()
                    .map(Emotion::getEmotionCode)
                    .collect(Collectors.toSet());

            // 3. 감정 정보표 생성 (영역별로 그룹핑) - 활성화된 감정만
            String emotionMappings = buildEmotionMappings(activeEmotions);

            // 4. 프롬프트에 주입
            promptTemplate = promptTemplateRaw.replace("{EMOTION_MAPPINGS}", emotionMappings);

            log.info("DiaryEmotionService 초기화 완료: {} 개 활성화된 감정 로드", validEmotions.size());

        } catch (IOException e) {
            throw new RuntimeException("감정 분석 프롬프트 초기화 실패", e);
        }
    }

    /**
     * DB 감정 정보를 바탕으로 감정 정보표 텍스트 생성
     * - 꽃 정보 제외, 감정 설명 포함
     */
    private String buildEmotionMappings(List<Emotion> emotions) {
        StringBuilder sb = new StringBuilder();

        // 영역별로 그룹핑
        String[] areas = {"YELLOW", "GREEN", "BLUE", "RED"};
        String[] areaNames = {"노랑 영역 (활기찬 감정)", "초록 영역 (평온한 감정)",
                             "파랑 영역 (차분한 감정)", "빨강 영역 (강한 감정)"};

        for (int i = 0; i < areas.length; i++) {
            String area = areas[i];
            String areaName = areaNames[i];

            List<Emotion> areaEmotions = emotions.stream()
                    .filter(e -> area.equalsIgnoreCase(e.getArea()))
                    .collect(Collectors.toList());

            if (!areaEmotions.isEmpty()) {
                sb.append("\n").append(areaName).append("\n");
                for (Emotion emotion : areaEmotions) {
                    // 감정 설명이 있으면 포함, 없으면 생략
                    String description = emotion.getEmotionDescription();
                    if (description != null && !description.isEmpty()) {
                        sb.append(String.format("- %s (%s): %s\n",
                                emotion.getEmotionCode(),
                                emotion.getEmotionNameKr(),
                                description));
                    } else {
                        sb.append(String.format("- %s (%s)\n",
                                emotion.getEmotionCode(),
                                emotion.getEmotionNameKr()));
                    }
                }
            }
        }

        return sb.toString();
    }

    /**
     * 일기 감정 분석 (LLM API 사용)
     * - 프롬프트 생성 → LLM 호출 → 응답 파싱/검증
     *
     * @param diaryContent 일기 내용
     * @param needRiskAnalysis 위험도 분석 필요 여부 (최신 일기일 경우만 true)
     */
    public DiaryEmotionResponse analyzeDiary(String diaryContent, boolean needRiskAnalysis) {

        // 1. 일기 내용 기본 검증
        validateDiaryContent(diaryContent);

        // 2. 프롬프트 생성
        String prompt = buildPrompt(diaryContent, needRiskAnalysis);
        log.info("[DiaryEmotionService - analyzeDiary] needRiskAnalysis={}, prompt : {}", needRiskAnalysis, prompt);

        // 3. LLM API 호출
        String llmResponse = llmApiClient.call(prompt);

        log.info("[DiaryEmotionService - analyzeDiary] llmResponse : {}", llmResponse);

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
     *
     * @param diaryContent 일기 내용
     * @param needRiskAnalysis 위험도 분석 필요 여부
     */
    private String buildPrompt(String diaryContent, boolean needRiskAnalysis) {
        String prompt = promptTemplate.replace("{DIARY_CONTENT}", diaryContent);

        // 위험도 분석이 필요하지 않은 경우, 해당 부분 제거
        if (!needRiskAnalysis) {
            prompt = removeRiskAnalysisSection(prompt);
        }

        return prompt;
    }

    /**
     * 프롬프트에서 위험도 분석 섹션 제거
     * - [위험도 분석 규칙] 섹션 전체 제거
     * - 응답 형식에서 riskLevel, riskReason, concernKeywords 필드 제거
     */
    private String removeRiskAnalysisSection(String prompt) {
        // [위험도 분석 규칙] 섹션 제거 (섹션 시작부터 [응답 형식]까지)
        prompt = prompt.replaceAll("\\[위험도 분석 규칙\\][\\s\\S]*?(?=\\[응답 형식\\])", "");

        // 응답 형식에서 위험도 관련 필드 제거
        prompt = prompt.replaceAll(",?\\s*\"riskLevel\":[^,\\n]*", "");
        prompt = prompt.replaceAll(",?\\s*\"riskReason\":[^,\\n]*", "");
        prompt = prompt.replaceAll(",?\\s*\"concernKeywords\":[^\\]]*\\][^,\\n]*", "");

        return prompt;
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

            if (response.getSummary() == null) {
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
        response.setReason("감정 분석에 실패하여 기본값으로 설정되었습니다.");

        EmotionPercent emotion = new EmotionPercent("PEACE", 100);
        response.setEmotions(List.of(emotion));

        return response;
    }
}
