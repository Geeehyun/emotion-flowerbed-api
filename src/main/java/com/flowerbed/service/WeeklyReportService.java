package com.flowerbed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowerbed.api.v1.domain.Diary;
import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.repository.DiaryRepository;
import com.flowerbed.api.v1.repository.FlowerRepository;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.api.v1.repository.WeeklyReportRepository;
import com.flowerbed.api.v1.service.LlmApiClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 주간 리포트 서비스
 * - 주간 일기 분석 및 리포트 생성
 * - LLM API를 통한 감정 트렌드 분석
 * - 모든 사용자에 대해 레코드 생성 (일기 3개 미만이어도)
 * - 일기 3개 이상: AI 분석 수행 (isAnalyzed=true)
 * - 일기 3개 미만: 분석 미수행 (isAnalyzed=false)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final FlowerRepository flowerRepository;
    private final LlmApiClient llmApiClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${weekly-report.batch-size:100}")
    private int batchSize;

    @org.springframework.beans.factory.annotation.Value("${weekly-report.delay-between-batches:2000}")
    private long delayBetweenBatches;

    // 프롬프트 템플릿 (원본)
    private String promptTemplateRaw;

    // DB 감정 정보가 주입된 최종 프롬프트 템플릿
    private String promptTemplate;

    /**
     * 서비스 초기화
     * 1. 프롬프트 템플릿 파일 로드
     * 2. DB에서 감정 정보 조회
     * 3. 프롬프트에 감정 정보 주입
     */
    @PostConstruct
    public void initPrompt() {
        try {
            // 1. 프롬프트 템플릿 파일 로드
            ClassPathResource resource = new ClassPathResource("prompts/weekly-report-analysis-prompt.txt");
            promptTemplateRaw = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 2. DB에서 감정 정보 조회
            List<Emotion> emotions = flowerRepository.findAllByOrderByDisplayOrderAsc();

            // 3. 감정 영역 설명 생성
            String emotionAreas = buildEmotionAreas(emotions);

            // 4. 감정-꽃 매칭표 생성
            String emotionMappings = buildEmotionMappings(emotions);

            // 5. 프롬프트에 주입
            promptTemplate = promptTemplateRaw
                    .replace("{EMOTION_AREAS}", emotionAreas)
                    .replace("{EMOTION_MAPPINGS}", emotionMappings);

            log.info("WeeklyReportService 초기화 완료: {} 개 감정 로드", emotions.size());

        } catch (IOException e) {
            throw new RuntimeException("주간 리포트 프롬프트 초기화 실패", e);
        }
    }

    /**
     * 감정 영역 설명 텍스트 생성
     */
    private String buildEmotionAreas(List<Emotion> emotions) {
        Map<String, List<String>> areaEmotionNames = new HashMap<>();

        // 영역별로 감정명(한글) 수집
        for (Emotion emotion : emotions) {
            String area = emotion.getArea();
            areaEmotionNames.computeIfAbsent(area, k -> new ArrayList<>())
                    .add(emotion.getEmotionNameKr());
        }

        StringBuilder sb = new StringBuilder();

        // 영역별 설명 생성
        Map<String, String> areaDescriptions = Map.of(
                "RED", "빨강 영역: 강한 감정",
                "YELLOW", "노랑 영역: 활기찬 감정",
                "BLUE", "파랑 영역: 차분한 감정",
                "GREEN", "초록 영역: 평온한 감정"
        );

        String[] areaOrder = {"RED", "YELLOW", "BLUE", "GREEN"};
        for (String area : areaOrder) {
            if (areaEmotionNames.containsKey(area)) {
                List<String> emotionNames = areaEmotionNames.get(area);
                sb.append("- ").append(areaDescriptions.get(area))
                  .append(" (").append(String.join(", ", emotionNames)).append(" 등)\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * DB 감정 정보를 바탕으로 감정-꽃 매칭표 텍스트 생성
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
                    sb.append(String.format("- %s (%s): %s / %s\n",
                            emotion.getEmotionCode(),
                            emotion.getEmotionNameKr(),
                            emotion.getFlowerNameKr(),
                            emotion.getFlowerMeaning()));
                }
            }
        }

        return sb.toString();
    }

    /**
     * 비동기로 사용자별 주간 리포트 생성
     * @return CompletableFuture<WeeklyReport> (일기 3개 미만 시 null 포함)
     */
    @Async
    @Transactional
    public CompletableFuture<WeeklyReport> generateReportAsync(Long userSn, LocalDate startDate, LocalDate endDate) {
        try {
            WeeklyReport report = generateReport(userSn, startDate, endDate);
            // null일 수 있음 (일기 3개 미만)
            return CompletableFuture.completedFuture(report);
        } catch (Exception e) {
            log.error("Failed to generate weekly report for user: {}", userSn, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 주간 리포트 생성 (동기)
     *
     * 비즈니스 로직:
     * 1. 일기 3개 이상: AI 분석 수행, isAnalyzed=true
     * 2. 일기 3개 미만: 리포트 생성하지 않음 (null 반환)
     * 3. 분석 실패: 레코드 생성 + isAnalyzed=false (재시도 대상)
     *
     * @return WeeklyReport or null (일기 3개 미만 시)
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

        // 해당 기간의 분석된 일기만 조회
        List<Diary> diaries = diaryRepository.findByUserSnAndDateBetween(userSn, startDate, endDate).stream()
                .filter(Diary::getIsAnalyzed)  // 분석된 일기만
                .collect(Collectors.toList());

        int diaryCount = diaries.size();

        // 일기 3개 미만: 리포트 생성하지 않음
        if (diaryCount < 3) {
            log.info("User {} has less than 3 analyzed diaries. Skipping report generation.", userSn);
            return null;
        }

        // 일기 3개 이상: AI 분석 수행
        try {
            // 감정 통계 계산
            List<WeeklyReport.EmotionStat> emotionStats = calculateEmotionStats(diaries);

            // 주간 일기 상세 정보 생성
            List<WeeklyReport.DiaryDetail> weeklyDiaryDetails = buildWeeklyDiaryDetails(diaries);

            // LLM API 호출하여 분석
            Map<String, Object> analysisResult = callLlmForAnalysis(diaries);

            // WeeklyReport 엔티티 생성
            WeeklyReport report = WeeklyReport.builder()
                    .user(user)
                    .startDate(startDate)
                    .endDate(endDate)
                    .diaryCount(diaryCount)
                    .studentReport((String) analysisResult.get("studentReport"))
                    .studentEncouragement((String) analysisResult.get("studentEncouragement"))
                    .teacherReport((String) analysisResult.get("teacherReport"))
                    .teacherTalkTip((List<String>) analysisResult.get("teacherTalkTip"))
                    .emotionStats(emotionStats)
                    .weeklyDiaryDetails(weeklyDiaryDetails)
                    .isAnalyzed(true)
                    .readYn(false)
                    .newNotificationSent(false)
                    .build();

            WeeklyReport saved = weeklyReportRepository.save(report);

            log.info("Weekly report generated with analysis: reportId={}, user={}, week={}",
                    saved.getReportId(), userSn, startDate);

            return saved;

        } catch (Exception e) {
            log.error("Failed to analyze weekly report for user: {}", userSn, e);

            // 분석 실패 시에도 레코드 생성 (isAnalyzed=false)
            WeeklyReport report = WeeklyReport.builder()
                    .user(user)
                    .startDate(startDate)
                    .endDate(endDate)
                    .diaryCount(diaryCount)
                    .isAnalyzed(false)
                    .readYn(false)
                    .newNotificationSent(false)
                    .build();

            WeeklyReport saved = weeklyReportRepository.save(report);
            log.warn("Weekly report created without analysis due to error: reportId={}, user={}, week={}",
                    saved.getReportId(), userSn, startDate);
            return saved;
        }
    }

    /**
     * 주간 일기 상세 정보 생성
     * - 날짜별 일기의 감정 정보 (프론트에서 날짜별 조회용)
     */
    private List<WeeklyReport.DiaryDetail> buildWeeklyDiaryDetails(List<Diary> diaries) {
        return diaries.stream()
                .sorted((a, b) -> a.getDiaryDate().compareTo(b.getDiaryDate()))  // 날짜 오름차순
                .map(diary -> {
                    String emotionCode = diary.getCoreEmotionCode();

                    // DB에서 감정 정보 조회
                    Emotion emotion = flowerRepository.findById(emotionCode).orElse(null);

                    return WeeklyReport.DiaryDetail.builder()
                            .diaryId(diary.getDiaryId())
                            .diaryDate(diary.getDiaryDate())
                            .coreEmotion(emotionCode)
                            .emotionNameKr(emotion != null ? emotion.getEmotionNameKr() : emotionCode)
                            .flowerNameKr(emotion != null ? emotion.getFlowerNameKr() : null)
                            .flowerMeaning(emotion != null ? emotion.getFlowerMeaning() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 감정 통계 계산
     * - 해당 주의 모든 일기의 coreEmotion 집계
     * - 감정별 출현 횟수 및 비율 계산
     * - 출현 횟수 내림차순 정렬
     */
    private List<WeeklyReport.EmotionStat> calculateEmotionStats(List<Diary> diaries) {
        // 감정별 카운트
        Map<String, Long> emotionCounts = diaries.stream()
                .map(Diary::getCoreEmotionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        int totalCount = diaries.size();

        // EmotionStat 리스트 생성
        List<WeeklyReport.EmotionStat> stats = emotionCounts.entrySet().stream()
                .map(entry -> {
                    String emotionCode = entry.getKey();
                    int count = entry.getValue().intValue();
                    double percentage = (count * 100.0) / totalCount;

                    // DB에서 감정 한글 이름 조회
                    String emotionNameKr = flowerRepository.findById(emotionCode)
                            .map(Emotion::getEmotionNameKr)
                            .orElse(emotionCode);

                    return WeeklyReport.EmotionStat.builder()
                            .emotion(emotionCode)
                            .emotionNameKr(emotionNameKr)
                            .count(count)
                            .percentage(Math.round(percentage * 10.0) / 10.0)  // 소수점 첫째 자리 반올림
                            .build();
                })
                .sorted((a, b) -> b.getCount().compareTo(a.getCount()))  // 출현 횟수 내림차순
                .collect(Collectors.toList());

        return stats;
    }

    /**
     * LLM API 호출하여 주간 일기 분석
     */
    private Map<String, Object> callLlmForAnalysis(List<Diary> diaries) {

        // 일기 내용을 구조화된 형식으로 결합
        String diaryContents = diaries.stream()
                .map(d -> String.format("""
                        날짜: %s
                        내용: %s
                        핵심감정: %s
                        감정분포: %s
                        """,
                        d.getDiaryDate(),
                        d.getContent(),
                        d.getCoreEmotionCode(),
                        d.getEmotionsJson() != null ? toJsonOrNull(d.getEmotionsJson()) : "정보 없음"
                ))
                .collect(Collectors.joining("\n---\n\n"));

        // 프롬프트 생성
        String prompt = promptTemplate.replace("{DIARY_CONTENT}", diaryContents);
        log.info("[WeeklyReportService - callLlmForAnalysis] prompt : {}", prompt);

        // LLM API 호출
        try {
            String llmResponse = llmApiClient.call(prompt);
            return parseAnalysisResponse(llmResponse);
        } catch (Exception e) {
            log.error("Failed to call LLM API for weekly report analysis", e);
            throw new RuntimeException("주간 리포트 분석 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * LLM 응답 파싱 및 검증
     *
     * 예외 처리:
     * 1. JSON 파싱 실패
     * 2. 필수 필드 누락 (studentReport, studentEncouragement, teacherReport, teacherTalkTip)
     * 3. teacherTalkTip이 배열이 아닌 경우
     */
    private Map<String, Object> parseAnalysisResponse(String llmResponse) {
        try {
            log.debug("LLM 응답 파싱 시작. 응답 길이: {} 문자", llmResponse.length());

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(llmResponse);

            // 필수 필드 검증
            if (!jsonNode.has("studentReport") || jsonNode.get("studentReport").isNull()) {
                throw new IllegalArgumentException("studentReport 필드가 누락되었습니다.");
            }
            if (!jsonNode.has("studentEncouragement") || jsonNode.get("studentEncouragement").isNull()) {
                throw new IllegalArgumentException("studentEncouragement 필드가 누락되었습니다.");
            }
            if (!jsonNode.has("teacherReport") || jsonNode.get("teacherReport").isNull()) {
                throw new IllegalArgumentException("teacherReport 필드가 누락되었습니다.");
            }
            if (!jsonNode.has("teacherTalkTip") || !jsonNode.get("teacherTalkTip").isArray()) {
                throw new IllegalArgumentException("teacherTalkTip 필드가 배열 형식이 아닙니다.");
            }

            String studentReport = jsonNode.get("studentReport").asText();
            String studentEncouragement = jsonNode.get("studentEncouragement").asText();
            String teacherReport = jsonNode.get("teacherReport").asText();

            List<String> teacherTalkTip = new ArrayList<>();
            jsonNode.get("teacherTalkTip").forEach(node -> teacherTalkTip.add(node.asText()));

            // 빈 값 검증
            if (studentReport.trim().isEmpty()) {
                throw new IllegalArgumentException("studentReport가 비어있습니다.");
            }
            if (studentEncouragement.trim().isEmpty()) {
                throw new IllegalArgumentException("studentEncouragement가 비어있습니다.");
            }
            if (teacherReport.trim().isEmpty()) {
                throw new IllegalArgumentException("teacherReport가 비어있습니다.");
            }
            if (teacherTalkTip.isEmpty()) {
                throw new IllegalArgumentException("teacherTalkTip이 비어있습니다.");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("studentReport", studentReport);
            result.put("studentEncouragement", studentEncouragement);
            result.put("teacherReport", teacherReport);
            result.put("teacherTalkTip", teacherTalkTip);

            return result;

        } catch (JsonProcessingException e) {
            log.error("========== LLM 응답 JSON 파싱 실패 ==========");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("응답 길이: {} 문자", llmResponse.length());
            log.error("응답 내용 (처음 500자):");
            log.error("{}", llmResponse.substring(0, Math.min(500, llmResponse.length())));
            if (llmResponse.length() > 500) {
                log.error("... (총 {} 문자, 나머지 생략)", llmResponse.length());
            }
            log.error("응답 내용 (마지막 500자):");
            log.error("{}", llmResponse.substring(Math.max(0, llmResponse.length() - 500)));
            log.error("========================================");
            throw new RuntimeException("LLM 응답 파싱에 실패했습니다. 응답이 완전하지 않을 수 있습니다.", e);
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

    /**
     * 리포트 읽음 처리
     */
    @Transactional
    public void markAsRead(Long reportId, Long userSn) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("주간 리포트를 찾을 수 없습니다."));

        // 권한 확인
        if (!report.getUser().getUserSn().equals(userSn)) {
            throw new IllegalArgumentException("해당 리포트에 접근 권한이 없습니다.");
        }

        report.markAsRead();
        log.info("Weekly report marked as read: reportId={}, user={}", reportId, userSn);
    }

    /**
     * 새 리포트 알림 전송 완료 처리
     */
    @Transactional
    public void markNotificationSent(Long reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("주간 리포트를 찾을 수 없습니다."));

        report.markNotificationSent();
        log.info("Weekly report notification sent: reportId={}", reportId);
    }

    /**
     * 안 읽은 리포트 존재 여부 확인
     */
    public boolean hasUnreadReports(Long userSn) {
        return weeklyReportRepository.existsByUserUserSnAndReadYnFalseAndDeletedAtIsNull(userSn);
    }

    /**
     * 새 리포트 존재 여부 확인 (알림 전송 안 된 리포트)
     * - 분석 완료된 리포트 중 알림 전송 안 된 것만 체크
     */
    public boolean hasNewReports(Long userSn) {
        return weeklyReportRepository.existsByUserUserSnAndNewNotificationSentFalseAndIsAnalyzedTrueAndDeletedAtIsNull(userSn);
    }

    /**
     * 읽음 상태별 리포트 목록 조회
     * @param userSn 사용자 SN
     * @param status "all", "read", "unread"
     * @return 리포트 목록 (최신순)
     */
    public List<WeeklyReport> getReportsByStatus(Long userSn, String status) {
        if ("all".equalsIgnoreCase(status)) {
            return weeklyReportRepository.findByUserUserSnAndDeletedAtIsNullOrderByStartDateDesc(userSn);
        } else if ("read".equalsIgnoreCase(status)) {
            return weeklyReportRepository.findByUserUserSnAndReadYnAndDeletedAtIsNullOrderByStartDateDesc(userSn, true);
        } else if ("unread".equalsIgnoreCase(status)) {
            return weeklyReportRepository.findByUserUserSnAndReadYnAndDeletedAtIsNullOrderByStartDateDesc(userSn, false);
        } else {
            throw new IllegalArgumentException("잘못된 status 값입니다. (all, read, unread 중 선택)");
        }
    }

    /**
     * 리포트 상세 조회 (권한 체크 포함)
     */
    public WeeklyReport getReportDetail(Long reportId, Long userSn) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("주간 리포트를 찾을 수 없습니다."));

        // 권한 확인
        if (!report.getUser().getUserSn().equals(userSn)) {
            throw new IllegalArgumentException("해당 리포트에 접근 권한이 없습니다.");
        }

        return report;
    }

    /**
     * 전체 사용자 대상 주간 리포트 생성
     * - 스케줄러 및 수동 생성 API에서 사용
     * - 활동중인 학생(STUDENT)만 대상
     * - 배치 처리 방식 (메모리/API Rate Limit 문제 방지)
     *
     * @param startDate 시작일 (월요일)
     * @param endDate 종료일 (일요일)
     */
    public void generateReportsForAllUsers(LocalDate startDate, LocalDate endDate) {
        log.info("========== 전체 사용자 주간 리포트 생성 시작 ==========");
        log.info("분석 기간: {} ~ {}", startDate, endDate);
        log.info("배치 설정: 배치 크기={}, 배치 간 대기={}ms", batchSize, delayBetweenBatches);

        // 활동중인 학생만 조회 (deletedAt IS NULL은 @Where로 자동 필터링)
        List<Long> allUserSns = userRepository.findAll().stream()
                .filter(user -> "STUDENT".equals(user.getUserTypeCd()))
                .map(User::getUserSn)
                .toList();

        int totalUsers = allUserSns.size();
        log.info("주간 리포트 생성 대상 학생 수: {}", totalUsers);

        if (allUserSns.isEmpty()) {
            log.info("주간 리포트 생성 대상 사용자가 없습니다.");
            return;
        }

        // 배치 단위로 분할 처리
        int totalBatches = (int) Math.ceil((double) totalUsers / batchSize);
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int fromIndex = batchIndex * batchSize;
            int toIndex = Math.min((batchIndex + 1) * batchSize, totalUsers);
            List<Long> batchUserSns = allUserSns.subList(fromIndex, toIndex);

            log.info("========== 배치 {}/{} 처리 시작 ({} ~ {} / {} 명) ==========",
                    batchIndex + 1, totalBatches, fromIndex + 1, toIndex, totalUsers);

            // 현재 배치 처리
            BatchResult batchResult = processBatch(batchUserSns, startDate, endDate);
            successCount += batchResult.successCount;
            skipCount += batchResult.skipCount;
            failCount += batchResult.failCount;

            log.info("========== 배치 {}/{} 처리 완료 (성공: {}, 스킵: {}, 실패: {}) ==========",
                    batchIndex + 1, totalBatches, batchResult.successCount, batchResult.skipCount, batchResult.failCount);

            // 마지막 배치가 아니면 대기 (API Rate Limit 방지)
            if (batchIndex < totalBatches - 1) {
                try {
                    log.info("다음 배치 처리 전 {}ms 대기 중...", delayBetweenBatches);
                    Thread.sleep(delayBetweenBatches);
                } catch (InterruptedException e) {
                    log.warn("배치 간 대기 중 인터럽트 발생", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("========== 전체 사용자 주간 리포트 생성 완료 ==========");
        log.info("전체 결과: 총 {}명, 성공 {}명, 스킵 {}명 (일기 3개 미만), 실패 {}명",
                totalUsers, successCount, skipCount, failCount);
    }

    /**
     * 분석 실패한 리포트 재시도
     * - isAnalyzed=false인 모든 리포트에 대해 LLM 분석 재시도
     * - 관리자가 수동으로 실행하는 API
     *
     * 사용 시나리오:
     * - LLM API 일시적 장애 복구 후 재시도
     * - 토큰 제한으로 실패한 리포트 재분석
     */
    @Transactional
    public void retryFailedReports() {
        log.info("========== 분석 실패한 주간 리포트 재시도 시작 ==========");

        // isAnalyzed=false인 모든 리포트 조회
        List<WeeklyReport> failedReports = weeklyReportRepository.findByIsAnalyzedFalseAndDeletedAtIsNull();

        int totalCount = failedReports.size();
        log.info("재시도 대상 리포트 수: {}", totalCount);

        if (failedReports.isEmpty()) {
            log.info("재시도 대상 리포트가 없습니다.");
            return;
        }

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (WeeklyReport report : failedReports) {
            Long reportId = report.getReportId();
            Long userSn = report.getUser().getUserSn();
            LocalDate startDate = report.getStartDate();
            LocalDate endDate = report.getEndDate();

            log.info("리포트 재분석 시도: reportId={}, user={}, week={}", reportId, userSn, startDate);

            try {
                // 해당 기간의 분석된 일기 재조회
                List<Diary> diaries = diaryRepository.findByUserSnAndDateBetween(userSn, startDate, endDate).stream()
                        .filter(Diary::getIsAnalyzed)
                        .collect(Collectors.toList());

                int diaryCount = diaries.size();

                // 일기 3개 미만: 스킵 (이론적으로 발생하지 않아야 함)
                if (diaryCount < 3) {
                    log.warn("리포트 재분석 스킵: reportId={}, 일기 개수 부족 ({}개)", reportId, diaryCount);
                    skipCount++;
                    continue;
                }

                // 감정 통계 재계산
                List<WeeklyReport.EmotionStat> emotionStats = calculateEmotionStats(diaries);

                // 주간 일기 상세 정보 재생성
                List<WeeklyReport.DiaryDetail> weeklyDiaryDetails = buildWeeklyDiaryDetails(diaries);

                // LLM API 재호출
                Map<String, Object> analysisResult = callLlmForAnalysis(diaries);

                // 리포트 업데이트
                report.updateAnalysisResult(
                        (String) analysisResult.get("studentReport"),
                        (String) analysisResult.get("studentEncouragement"),
                        (String) analysisResult.get("teacherReport"),
                        (List<String>) analysisResult.get("teacherTalkTip"),
                        emotionStats,
                        weeklyDiaryDetails,
                        diaryCount
                );

                weeklyReportRepository.save(report);

                log.info("✓ 리포트 재분석 성공: reportId={}, user={}", reportId, userSn);
                successCount++;

            } catch (Exception e) {
                log.error("✗ 리포트 재분석 실패: reportId={}, user={}, error={}", reportId, userSn, e.getMessage(), e);
                failCount++;
            }
        }

        log.info("========== 분석 실패한 주간 리포트 재시도 완료 ==========");
        log.info("전체 결과: 총 {}개, 성공 {}개, 스킵 {}개, 실패 {}개", totalCount, successCount, skipCount, failCount);
    }

    /**
     * 배치 처리 결과
     */
    private static class BatchResult {
        int successCount = 0;  // 성공 (분석 완료 + 분석 실패했지만 레코드 생성)
        int skipCount = 0;     // 스킵 (일기 3개 미만)
        int failCount = 0;     // 실패 (예외 발생)
    }

    /**
     * 한 배치 처리 (비동기 병렬 처리)
     */
    private BatchResult processBatch(List<Long> userSns, LocalDate startDate, LocalDate endDate) {
        BatchResult result = new BatchResult();

        // 배치 내에서는 비동기 병렬 처리
        List<CompletableFuture<Void>> futures = userSns.stream()
                .map(userSn -> generateReportAsync(userSn, startDate, endDate)
                        .thenAccept(report -> {
                            if (report == null) {
                                // 일기 3개 미만으로 스킵
                                result.skipCount++;
                                log.debug("○ 주간 리포트 스킵 (일기 3개 미만): userId={}", userSn);
                            } else {
                                // 성공 (분석 완료 또는 분석 실패했지만 레코드 생성)
                                result.successCount++;
                                log.debug("✓ 주간 리포트 생성 완료: userId={}, reportId={}, isAnalyzed={}",
                                        userSn, report.getReportId(), report.getIsAnalyzed());
                            }
                        })
                        .exceptionally(ex -> {
                            result.failCount++;
                            log.error("✗ 주간 리포트 생성 실패: userId={}, error={}", userSn, ex.getMessage());
                            return null;
                        }))
                .toList();

        // 배치 내 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return result;
    }

    private String toJsonOrNull(Object value) {
        try {
            return value == null ? "정보 없음" : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "정보 없음";
        }
    }
}
