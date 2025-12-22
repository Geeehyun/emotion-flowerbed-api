package com.flowerbed.scheduler;

import com.flowerbed.api.v1.repository.DiaryRepository;
import com.flowerbed.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 주간 리포트 스케줄러
 * - 매주 월요일 00시에 실행
 * - 지난 주(월~일) 일기를 3일 이상 쓴 사용자에게 주간 리포트 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final WeeklyReportService weeklyReportService;
    private final DiaryRepository diaryRepository;

    /**
     * 매주 월요일 00시에 주간 리포트 생성
     * cron: "초 분 시 일 월 요일"
     * "0 0 0 * * MON" = 매주 월요일 00:00:00
     */
    // @Scheduled(cron = "0 0 0 * * MON")
    public void generateWeeklyReports() {
        log.info("========== 주간 리포트 생성 스케줄러 시작 ==========");

        // 지난 주의 시작일(월요일)과 종료일(일요일) 계산
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastSunday = lastMonday.plusDays(6);

        log.info("분석 기간: {} ~ {}", lastMonday, lastSunday);

        // 3일 이상 일기를 쓴 사용자 조회
        List<Long> userSns = diaryRepository.findUserSnWithMinDiaryCount(lastMonday, lastSunday);

        log.info("주간 리포트 생성 대상 사용자 수: {}", userSns.size());

        if (userSns.isEmpty()) {
            log.info("주간 리포트 생성 대상 사용자가 없습니다.");
            return;
        }

        // 비동기로 각 사용자별 리포트 생성
        List<CompletableFuture<Void>> futures = userSns.stream()
                .map(userSn -> weeklyReportService.generateReportAsync(userSn, lastMonday, lastSunday)
                        .thenAccept(report -> log.info("✓ 주간 리포트 생성 완료: userId={}, reportId={}",
                                userSn, report.getReportId()))
                        .exceptionally(ex -> {
                            log.error("✗ 주간 리포트 생성 실패: userId={}, error={}", userSn, ex.getMessage());
                            return null;
                        }))
                .toList();

        // 모든 비동기 작업이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("========== 주간 리포트 생성 스케줄러 종료 ==========");
    }

    /**
     * 테스트용: 수동으로 주간 리포트 생성
     * 실제 운영 시에는 제거하거나 주석 처리
     */
    // @Scheduled(cron = "0 */5 * * * *") // 5분마다 실행 (테스트용)
    public void generateWeeklyReportsForTest() {
        log.info("========== [TEST] 주간 리포트 생성 스케줄러 시작 ==========");

        // 이번 주의 시작일(월요일)과 오늘까지 계산
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);

        log.info("[TEST] 분석 기간: {} ~ {}", thisMonday, today);

        List<Long> userSns = diaryRepository.findUserSnWithMinDiaryCount(thisMonday, today);

        log.info("[TEST] 주간 리포트 생성 대상 사용자 수: {}", userSns.size());

        if (userSns.isEmpty()) {
            log.info("[TEST] 주간 리포트 생성 대상 사용자가 없습니다.");
            return;
        }

        List<CompletableFuture<Void>> futures = userSns.stream()
                .map(userSn -> weeklyReportService.generateReportAsync(userSn, thisMonday, today)
                        .thenAccept(report -> log.info("[TEST] ✓ 주간 리포트 생성 완료: userId={}, reportId={}",
                                userSn, report.getReportId()))
                        .exceptionally(ex -> {
                            log.error("[TEST] ✗ 주간 리포트 생성 실패: userId={}, error={}", userSn, ex.getMessage());
                            return null;
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("========== [TEST] 주간 리포트 생성 스케줄러 종료 ==========");
    }
}
