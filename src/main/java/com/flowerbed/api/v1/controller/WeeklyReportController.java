package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.dto.WeeklyReportResponse;
import com.flowerbed.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 주간 리포트 API
 * - 사용자별 주간 리포트 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/weekly-reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    /**
     * 특정 주의 리포트 조회
     * GET /api/v1/weekly-reports?startDate=2025-01-06
     */
    @GetMapping
    public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long userSn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        WeeklyReport report = weeklyReportService.getReport(userSn, startDate);
        return ResponseEntity.ok(WeeklyReportResponse.from(report));
    }

    /**
     * 모든 주간 리포트 조회 (최신순)
     * GET /api/v1/weekly-reports/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<WeeklyReportResponse>> getAllWeeklyReports(
            @AuthenticationPrincipal Long userSn
    ) {
        List<WeeklyReport> reports = weeklyReportService.getAllReports(userSn);
        List<WeeklyReportResponse> response = reports.stream()
                .map(WeeklyReportResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 N개 주간 리포트 조회
     * GET /api/v1/weekly-reports/recent?limit=5
     */
    @GetMapping("/recent")
    public ResponseEntity<List<WeeklyReportResponse>> getRecentWeeklyReports(
            @AuthenticationPrincipal Long userSn,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<WeeklyReport> reports = weeklyReportService.getRecentReports(userSn, limit);
        List<WeeklyReportResponse> response = reports.stream()
                .map(WeeklyReportResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 수동으로 주간 리포트 생성 (테스트용)
     * POST /api/v1/weekly-reports/generate?startDate=2025-01-06&endDate=2025-01-12
     */
    @PostMapping("/generate")
    public ResponseEntity<WeeklyReportResponse> generateWeeklyReport(
            @AuthenticationPrincipal Long userSn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        WeeklyReport report = weeklyReportService.generateReport(userSn, startDate, endDate);
        return ResponseEntity.ok(WeeklyReportResponse.from(report));
    }
}
