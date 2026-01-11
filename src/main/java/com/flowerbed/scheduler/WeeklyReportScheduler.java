package com.flowerbed.scheduler;

import com.flowerbed.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 주간 리포트 스케줄러
 * - 매주 월요일 00시에 실행
 * - 모든 사용자에 대해 주간 리포트 레코드 생성
 * - 일기 3개 이상: AI 분석 수행 (isAnalyzed=true)
 * - 일기 3개 미만: 레코드만 생성, AI 분석 미수행 (isAnalyzed=false)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final WeeklyReportService weeklyReportService;

    /**
     * 매주 월요일 00시에 주간 리포트 생성
     * cron: "초 분 시 일 월 요일"
     * "0 0 0 * * MON" = 매주 월요일 00:00:00
     */
    @Scheduled(cron = "0 25 0 * * MON")
    public void generateWeeklyReports() {
        log.info("========== 주간 리포트 자동 생성 스케줄러 시작 ==========");

        // 지난 주의 시작일(월요일)과 종료일(일요일) 계산
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastSunday = lastMonday.plusDays(6);

        // Service 메서드 호출
        weeklyReportService.generateReportsForAllUsers(lastMonday, lastSunday);

        log.info("========== 주간 리포트 자동 생성 스케줄러 종료 ==========");
    }
}
