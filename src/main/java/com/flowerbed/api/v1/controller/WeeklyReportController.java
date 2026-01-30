package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.dto.GenerableWeekResponse;
import com.flowerbed.api.v1.dto.GenerableWeeksResponse;
import com.flowerbed.api.v1.dto.WeeklyReportDetailResponse;
import com.flowerbed.api.v1.dto.WeeklyReportListItemResponse;
import com.flowerbed.api.v1.dto.WeeklyReportStatusResponse;
import com.flowerbed.api.v1.service.EmotionCacheService;
import com.flowerbed.api.v1.service.RedisService;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.security.SecurityUtil;
import com.flowerbed.service.WeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주간 리포트 API (학생용)
 * - 학생이 자신의 주간 리포트 조회
 * - 안 읽은 리포트, 새 리포트 확인
 * - 읽음 상태 관리
 */
@Slf4j
@RestController
@RequestMapping("/v1/weekly-reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final EmotionCacheService emotionCacheService;
    private final RedisService redisService;

    /**
     * 안 읽은 리포트 존재 여부 확인
     * GET /api/v1/weekly-reports/unread/exists
     */
    @GetMapping("/unread/exists")
    public ResponseEntity<WeeklyReportStatusResponse> checkUnreadReports() {
        Long userSn = SecurityUtil.getCurrentUserSn();
        boolean hasUnread = weeklyReportService.hasUnreadReports(userSn);
        return ResponseEntity.ok(WeeklyReportStatusResponse.of(hasUnread, null));
    }

    /**
     * 새 리포트 존재 여부 확인 (알림 전송 안 된 리포트)
     * GET /api/v1/weekly-reports/new/exists
     */
    @GetMapping("/new/exists")
    public ResponseEntity<WeeklyReportStatusResponse> checkNewReports() {
        Long userSn = SecurityUtil.getCurrentUserSn();
        boolean hasNew = weeklyReportService.hasNewReports(userSn);
        return ResponseEntity.ok(WeeklyReportStatusResponse.of(null, hasNew));
    }

    /**
     * 주간 리포트 리스트 조회 (필터링)
     * GET /api/v1/weekly-reports/list?status=all&includeAnalyzable=true
     * @param status all(전체), read(읽음), unread(안읽음), recent(최근 3개월) - 기본값: all
     * @param includeAnalyzable true: 분석 가능한 미완료 리포트 포함, false: 분석 완료된 것만 (기본값: false)
     * @return 리포트 목록 (startDate 기준 내림차순)
     */
    @GetMapping("/list")
    public ResponseEntity<List<WeeklyReportListItemResponse>> getWeeklyReportList(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "false") boolean includeAnalyzable
    ) {
        Long userSn = SecurityUtil.getCurrentUserSn();
        List<WeeklyReport> reports = weeklyReportService.getReportsByStatus(userSn, status, includeAnalyzable);
        List<WeeklyReportListItemResponse> response = reports.stream()
                .map(report -> {
                    int currentCount = weeklyReportService.getCurrentDiaryCount(userSn, report.getStartDate(), report.getEndDate());
                    return WeeklyReportListItemResponse.from(report, currentCount);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * 주간 리포트 상세 조회
     * GET /api/v1/weekly-reports/{reportId}
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<WeeklyReportDetailResponse> getWeeklyReportDetail(
            @PathVariable Long reportId
    ) {
        Long userSn = SecurityUtil.getCurrentUserSn();
        WeeklyReport report = weeklyReportService.getReportDetail(reportId, userSn);
        return ResponseEntity.ok(WeeklyReportDetailResponse.from(report, emotionCacheService));
    }

    /**
     * 주간 리포트 읽음 처리
     * PUT /api/v1/weekly-reports/{reportId}/read
     */
    @PutMapping("/{reportId}/read")
    public ResponseEntity<Void> markReportAsRead(
            @PathVariable Long reportId
    ) {
        Long userSn = SecurityUtil.getCurrentUserSn();
        weeklyReportService.markAsRead(reportId, userSn);
        return ResponseEntity.ok().build();
    }

    /**
     * 발행 가능한 주 목록 조회 (학생 전용)
     * GET /api/v1/weekly-reports/generable
     *
     * 조건:
     * - 분석된 일기가 3개 이상인 주
     * - 아직 주간 리포트가 생성되지 않은 주
     * - 현재 진행 중인 주 제외 (완료된 주만)
     *
     * @return 발행 가능한 주 목록 + 발행 횟수 정보
     */
    @Operation(summary = "발행 가능한 주 목록 조회", description = "학생이 주간 리포트를 발행할 수 있는 주 목록과 발행 횟수 정보를 조회합니다.")
    @GetMapping("/generable")
    public ResponseEntity<GenerableWeeksResponse> getGenerableWeeks() {
        // 학생 권한 체크
        SecurityUtil.requireStudent();

        Long userSn = SecurityUtil.getCurrentUserSn();
        List<LocalDate[]> generableWeeks = weeklyReportService.getGenerableWeeks(userSn);

        List<GenerableWeekResponse> weeks = generableWeeks.stream()
                .map(week -> GenerableWeekResponse.of(
                        week[0],  // startDate
                        week[1],  // endDate
                        (int) week[2].toEpochDay()  // diaryCount
                ))
                .collect(Collectors.toList());

        // 발행 횟수 정보 조회
        int dailyLimit = RedisService.DAILY_WEEKLY_REPORT_LIMIT;
        int usedCount = redisService.getWeeklyReportGenerateCount(userSn);

        return ResponseEntity.ok(GenerableWeeksResponse.of(dailyLimit, usedCount, weeks));
    }

    /**
     * 수동으로 주간 리포트 생성 - 단일 사용자 (학생 전용)
     * POST /api/v1/weekly-reports/generate?startDate=2025-01-06&endDate=2025-01-12
     *
     * ⚠️ 학생 권한 필수 (userTypeCd = 'STUDENT')
     * - 선생님, 관리자는 주간 리포트를 생성할 수 없습니다.
     * - 일일 발행 횟수 제한: {@link RedisService#DAILY_WEEKLY_REPORT_LIMIT}
     *
     * 사용 시나리오:
     * - 학생이 수동으로 자신의 주간 리포트 생성
     */
    @Operation(summary = "주간 리포트 발행 신청", description = "학생이 특정 주의 주간 리포트 발행을 신청합니다. 일일 발행 횟수 제한이 있습니다.")
    @PostMapping("/generate")
    public ResponseEntity<WeeklyReportDetailResponse> generateWeeklyReport(
            @Parameter(description = "주 시작일 (월요일)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "주 종료일 (일요일)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // 학생 권한 체크
        SecurityUtil.requireStudent();

        Long userSn = SecurityUtil.getCurrentUserSn();

        // 일일 발행 횟수 제한 체크
        if (!redisService.canGenerateWeeklyReport(userSn)) {
            log.warn("일일 주간 리포트 발행 횟수 초과: userSn={}", userSn);
            throw new BusinessException(ErrorCode.WEEKLY_REPORT_LIMIT_EXCEEDED);
        }

        WeeklyReport report = weeklyReportService.generateReport(userSn, startDate, endDate);

        if (report == null) {
            throw new IllegalArgumentException("일기가 3개 미만이어서 주간 리포트를 생성할 수 없습니다.");
        }

        // 발행 성공 시 횟수 증가
        redisService.incrementWeeklyReportGenerateCount(userSn);
        log.info("주간 리포트 발행 완료: userSn={}, reportId={}, 남은 횟수={}",
                userSn, report.getReportId(), redisService.getRemainingWeeklyReportGenerateCount(userSn));

        return ResponseEntity.ok(WeeklyReportDetailResponse.from(report, emotionCacheService));
    }

    /**
     * 수동으로 주간 리포트 생성 - 전체 학생 (관리자 전용)
     * POST /api/v1/weekly-reports/generate-all?startDate=2025-01-06&endDate=2025-01-12
     *
     * ⚠️ 관리자 권한 필수 (userTypeCd = 'ADMIN')
     *
     * 사용 시나리오:
     * - 스케줄러 테스트
     * - 특정 기간 리포트 일괄 생성
     */
    @PostMapping("/generate-all")
    public ResponseEntity<Void> generateWeeklyReportsForAllUsers(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // 관리자 권한 체크
        SecurityUtil.requireAdmin();

        weeklyReportService.generateReportsForAllUsers(startDate, endDate);
        return ResponseEntity.ok().build();
    }

    /**
     * 분석 실패한 리포트 재시도 - 전체 (관리자 전용)
     * POST /api/v1/weekly-reports/retry-failed
     *
     * ⚠️ 관리자 권한 필수 (userTypeCd = 'ADMIN')
     *
     * 사용 시나리오:
     * - isAnalyzed=false인 리포트들을 다시 분석
     * - LLM API 장애 복구 후 재시도
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<Void> retryFailedReports() {
        // 관리자 권한 체크
        SecurityUtil.requireAdmin();

        weeklyReportService.retryFailedReports();
        return ResponseEntity.ok().build();
    }

    /**
     * 주간 리포트 알림 확인 처리
     * PUT /api/v1/weekly-reports/{reportId}/notification-sent
     *
     * 사용 시나리오:
     * 1. 학생이 앱을 열면 GET /new/exists 호출 → hasNew: true
     * 2. 학생이 새 리포트 알림을 확인 (리포트 목록 진입 등)
     * 3. PUT /{reportId}/notification-sent 호출
     * 4. newNotificationSent를 true로 변경
     * 5. 다음부터 GET /new/exists → hasNew: false
     */
    @Operation(summary = "주간 리포트 알림 확인 처리", description = "학생이 새 리포트 알림을 확인했을 때 호출하여 newNotificationSent를 true로 변경")
    @PutMapping("/{reportId}/notification-sent")
    public ResponseEntity<Void> markNotificationSent(
            @Parameter(description = "주간 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        Long userSn = SecurityUtil.getCurrentUserSn();
        weeklyReportService.markNotificationSentByUser(reportId, userSn);
        return ResponseEntity.ok().build();
    }
}
