package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.dto.WeeklyReportDetailResponse;
import com.flowerbed.api.v1.dto.WeeklyReportListItemResponse;
import com.flowerbed.api.v1.dto.WeeklyReportStatusResponse;
import com.flowerbed.security.SecurityUtil;
import com.flowerbed.service.WeeklyReportService;
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
     * GET /api/v1/weekly-reports/list?status=all|read|unread
     * @param status all(전체), read(읽음), unread(안읽음) - 기본값: all
     */
    @GetMapping("/list")
    public ResponseEntity<List<WeeklyReportListItemResponse>> getWeeklyReportList(
            @RequestParam(defaultValue = "all") String status
    ) {
        Long userSn = SecurityUtil.getCurrentUserSn();
        List<WeeklyReport> reports = weeklyReportService.getReportsByStatus(userSn, status);
        List<WeeklyReportListItemResponse> response = reports.stream()
                .map(WeeklyReportListItemResponse::from)
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
        return ResponseEntity.ok(WeeklyReportDetailResponse.from(report));
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
     * 수동으로 주간 리포트 생성 - 단일 사용자 (학생 전용)
     * POST /api/v1/weekly-reports/generate?startDate=2025-01-06&endDate=2025-01-12
     *
     * ⚠️ 학생 권한 필수 (userTypeCd = 'STUDENT')
     * - 선생님, 관리자는 주간 리포트를 생성할 수 없습니다.
     *
     * 사용 시나리오:
     * - 학생이 수동으로 자신의 주간 리포트 생성
     * - 테스트용
     */
    @PostMapping("/generate")
    public ResponseEntity<WeeklyReportDetailResponse> generateWeeklyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // 학생 권한 체크
        SecurityUtil.requireStudent();

        Long userSn = SecurityUtil.getCurrentUserSn();
        WeeklyReport report = weeklyReportService.generateReport(userSn, startDate, endDate);

        if (report == null) {
            throw new IllegalArgumentException("일기가 3개 미만이어서 주간 리포트를 생성할 수 없습니다.");
        }

        return ResponseEntity.ok(WeeklyReportDetailResponse.from(report));
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

    // ========================================
    // TODO: 선생님용 API (프론트 화면 준비 후 구현)
    // ========================================
    // GET /api/v1/teachers/students/{studentSn}/weekly-reports/list
    // - 선생님이 담당 학생의 주간 리포트 목록 조회
    // - teacherReport, teacherTalkTip 포함
    //
    // GET /api/v1/teachers/students/{studentSn}/weekly-reports/{reportId}
    // - 선생님이 담당 학생의 주간 리포트 상세 조회
    // - teacherReport, teacherTalkTip 포함
    // ========================================
}
