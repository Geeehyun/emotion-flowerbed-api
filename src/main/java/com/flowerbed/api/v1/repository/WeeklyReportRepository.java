package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    // 특정 사용자의 특정 주 리포트 조회
    Optional<WeeklyReport> findByUserUserSnAndStartDateAndDeletedAtIsNull(Long userSn, LocalDate startDate);

    // 특정 사용자의 모든 주간 리포트 조회 (최신순)
    List<WeeklyReport> findByUserUserSnAndDeletedAtIsNullOrderByStartDateDesc(Long userSn);

    // 특정 기간의 리포트 존재 여부 확인
    boolean existsByUserUserSnAndStartDateAndDeletedAtIsNull(Long userSn, LocalDate startDate);

    // 특정 기간에 생성된 리포트 조회 (스케줄러용)
    @Query("SELECT w FROM WeeklyReport w WHERE w.startDate = :startDate AND w.endDate = :endDate")
    List<WeeklyReport> findByWeekPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 사용자의 최근 N개 리포트 조회
    @Query("SELECT w FROM WeeklyReport w WHERE w.user.userSn = :userSn " +
            "AND w.deletedAt IS NULL ORDER BY w.startDate DESC LIMIT :limit")
    List<WeeklyReport> findRecentReports(@Param("userSn") Long userSn, @Param("limit") int limit);

    // 안 읽은 리포트 존재 여부 확인
    boolean existsByUserUserSnAndReadYnFalseAndDeletedAtIsNull(Long userSn);

    // 새 리포트 존재 여부 확인 (알림 전송 안 된 리포트)
    boolean existsByUserUserSnAndNewNotificationSentFalseAndIsAnalyzedTrueAndDeletedAtIsNull(Long userSn);

    // 읽음 상태별 리포트 조회 (최신순)
    List<WeeklyReport> findByUserUserSnAndReadYnAndDeletedAtIsNullOrderByStartDateDesc(Long userSn, Boolean readYn);

    // 최근 3개월 리포트 조회 (startDate 기준 내림차순)
    List<WeeklyReport> findByUserUserSnAndStartDateGreaterThanEqualAndDeletedAtIsNullOrderByStartDateDesc(Long userSn, LocalDate threeMonthsAgo);

    // 분석 실패한 리포트 조회 (isAnalyzed=false)
    List<WeeklyReport> findByIsAnalyzedFalseAndDeletedAtIsNull();
}
