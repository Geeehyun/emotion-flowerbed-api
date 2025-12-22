package com.flowerbed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerbed.api.v1.domain.Diary;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.repository.DiaryRepository;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.api.v1.repository.WeeklyReportRepository;
import com.flowerbed.api.v1.service.LlmApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 주간 리포트 서비스
 * - 주간 일기 분석 및 리포트 생성
 * - LLM API를 통한 감정 트렌드 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final LlmApiClient llmApiClient;
    private final ObjectMapper objectMapper;

    /**
     * 비동기로 사용자별 주간 리포트 생성
     */
    @Async
    @Transactional
    public CompletableFuture<WeeklyReport> generateReportAsync(Long userSn, LocalDate startDate, LocalDate endDate) {
        try {
            WeeklyReport report = generateReport(userSn, startDate, endDate);
            return CompletableFuture.completedFuture(report);
        } catch (Exception e) {
            log.error("Failed to generate weekly report for user: {}", userSn, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 주간 리포트 생성 (동기)
     */
    @Transactional
    public WeeklyReport generateReport(Long userSn, LocalDate startDate, LocalDate endDate) {

        // 이미 생성된 리포트가 있는지 확인
        if (weeklyReportRepository.existsByUserUserSnAndStartDateAndDeletedAtIsNull(userSn, startDate)) {
            log.info("Weekly report already exists for user: {}, week: {}", userSn, startDate);
            return weeklyReportRepository.findByUserUserSnAndStartDateAndDeletedAtIsNull(userSn, startDate)
                    .orElseThrow();
        }

        // 사용자 조회
        User user = userRepository.findById(userSn)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userSn));

        // 해당 기간의 일기 조회
        List<Diary> diaries = diaryRepository.findByUserSnAndDateBetween(userSn, startDate, endDate);

        if (diaries.size() < 3) {
            log.warn("User {} has less than 3 diaries in this week. Skip generating report.", userSn);
            throw new IllegalArgumentException("3일 이상 일기를 작성해야 주간 리포트를 생성할 수 있습니다.");
        }

        // LLM API 호출하여 분석
        String analysisText = callLlmForAnalysis(diaries);

        // JSON 파싱 및 WeeklyReport 엔티티 생성
        WeeklyReport report = parseAndCreateReport(user, startDate, endDate, diaries.size(), analysisText);

        WeeklyReport saved = weeklyReportRepository.save(report);

        log.info("Weekly report generated for user: {}, reportId: {}, week: {}",
                userSn, saved.getReportId(), startDate);

        return saved;
    }

    /**
     * LLM API 호출하여 주간 일기 분석
     */
    private String callLlmForAnalysis(List<Diary> diaries) {

        // 일기 내용을 하나의 텍스트로 결합
        String diaryContents = diaries.stream()
                .map(d -> String.format("[%s] %s", d.getDiaryDate(), d.getContent()))
                .collect(Collectors.joining("\n\n"));

        // 프롬프트 생성
        String prompt = buildWeeklyReportPrompt(diaryContents, diaries.size());

        // LLM API 호출
        try {
            return llmApiClient.call(prompt);
        } catch (Exception e) {
            log.error("Failed to call LLM API for weekly report analysis", e);
            throw new RuntimeException("주간 리포트 분석 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 주간 리포트 분석 프롬프트 생성
     */
    private String buildWeeklyReportPrompt(String diaryContents, int diaryCount) {
        return String.format("""
                당신은 감정 일기 분석 전문가입니다.
                사용자가 이번 주에 작성한 %d개의 일기를 분석하여 주간 리포트를 작성해주세요.

                <일기 내용>
                %s
                </일기 내용>

                다음 형식의 JSON으로 응답해주세요:
                {
                    "weekSummary": "한 주를 한 문장으로 요약 (최대 100자)",
                    "emotionalJourney": "감정의 흐름과 변화 분석 (200-300자)",
                    "emotionStats": [
                        {"emotion": "기쁨", "count": 3, "percentage": 42.5},
                        {"emotion": "슬픔", "count": 2, "percentage": 28.5}
                    ],
                    "highlights": [
                        "이번 주 가장 의미있었던 순간 3가지"
                    ],
                    "growthInsight": "성장과 변화에 대한 통찰 (200-300자)"
                }

                **중요**: 응답은 반드시 유효한 JSON 형식이어야 하며, 다른 텍스트는 포함하지 마세요.
                JSON만 출력하고, 마크다운 코드 블록(```json)이나 설명 문구는 제외해주세요.
                """, diaryCount, diaryContents);
    }

    /**
     * LLM 응답 파싱 및 WeeklyReport 엔티티 생성
     */
    private WeeklyReport parseAndCreateReport(User user, LocalDate startDate, LocalDate endDate,
                                             int diaryCount, String llmResponse) {
        try {
            // JSON 파싱
            WeeklyReport.AnalysisResult analysisResult = objectMapper.readValue(
                    llmResponse, WeeklyReport.AnalysisResult.class);

            // WeeklyReport 엔티티 생성
            return WeeklyReport.builder()
                    .user(user)
                    .startDate(startDate)
                    .endDate(endDate)
                    .diaryCount(diaryCount)
                    .summary(analysisResult.getWeekSummary())
                    .emotionTrend(analysisResult.getEmotionalJourney())
                    .recommendations(analysisResult.getGrowthInsight())
                    .analysisJson(analysisResult)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response: {}", llmResponse, e);

            // 파싱 실패 시 기본 리포트 생성
            return WeeklyReport.builder()
                    .user(user)
                    .startDate(startDate)
                    .endDate(endDate)
                    .diaryCount(diaryCount)
                    .summary("이번 주 " + diaryCount + "일의 일기를 작성했습니다.")
                    .emotionTrend("분석 중 오류가 발생했습니다.")
                    .recommendations("다음에 다시 시도해주세요.")
                    .build();
        }
    }

    /**
     * 특정 사용자의 주간 리포트 조회
     */
    public WeeklyReport getReport(Long userSn, LocalDate startDate) {
        return weeklyReportRepository.findByUserUserSnAndStartDateAndDeletedAtIsNull(userSn, startDate)
                .orElseThrow(() -> new IllegalArgumentException("주간 리포트를 찾을 수 없습니다."));
    }

    /**
     * 특정 사용자의 모든 주간 리포트 조회
     */
    public List<WeeklyReport> getAllReports(Long userSn) {
        return weeklyReportRepository.findByUserUserSnAndDeletedAtIsNullOrderByStartDateDesc(userSn);
    }

    /**
     * 특정 사용자의 최근 N개 리포트 조회
     */
    public List<WeeklyReport> getRecentReports(Long userSn, int limit) {
        return weeklyReportRepository.findRecentReports(userSn, limit);
    }
}
