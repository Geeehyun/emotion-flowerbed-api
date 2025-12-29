package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.Diary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {

    // 특정 날짜 일기 조회 (하루 1개)
    Optional<Diary> findByUserUserSnAndDiaryDateAndDeletedAtIsNull(Long userSn, LocalDate diaryDate);

    // 월별 일기 조회
    @Query("SELECT d FROM Diary d WHERE d.user.userSn = :userSn " +
            "AND YEAR(d.diaryDate) = :year AND MONTH(d.diaryDate) = :month " +
            "ORDER BY d.diaryDate DESC")
    List<Diary> findByUserSnAndYearMonth(
            @Param("userSn") Long userSn,
            @Param("year") int year,
            @Param("month") int month
    );

    // 특정 월에 일기가 있는지 확인
    @Query("SELECT COUNT(d) > 0 FROM Diary d WHERE d.user.userSn = :userSn " +
            "AND YEAR(d.diaryDate) = :year AND MONTH(d.diaryDate) = :month")
    boolean existsByUserSnAndYearMonth(
            @Param("userSn") Long userSn,
            @Param("year") int year,
            @Param("month") int month
    );

    // 사용자의 최근 일기 조회
    List<Diary> findByUserUserSnOrderByDiaryDateDesc(Long userSn);

    // 사용자의 분석된 일기 조회
    List<Diary> findByUserUserSnAndIsAnalyzed(Long userSn, Boolean isAnalyzed);

    // 특정 기간의 일기 조회 (주간 리포트용)
    @Query("SELECT d FROM Diary d WHERE d.user.userSn = :userSn " +
            "AND d.diaryDate BETWEEN :startDate AND :endDate " +
            "ORDER BY d.diaryDate ASC")
    List<Diary> findByUserSnAndDateBetween(
            @Param("userSn") Long userSn,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 특정 기간에 3일 이상 일기를 쓴 사용자 조회
    @Query("SELECT d.user.userSn FROM Diary d " +
            "WHERE d.diaryDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.user.userSn " +
            "HAVING COUNT(d) >= 3")
    List<Long> findUserSnWithMinDiaryCount(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 특정 날짜 이전 최근 N일치 분석된 일기 조회 (날짜 역순, 감정 조절 팁 체크용)
    @Query("SELECT d FROM Diary d WHERE d.user.userSn = :userSn " +
            "AND d.diaryDate <= :baseDate " +
            "AND d.isAnalyzed = true " +
            "ORDER BY d.diaryDate DESC")
    List<Diary> findRecentAnalyzedDiaries(
            @Param("userSn") Long userSn,
            @Param("baseDate") LocalDate baseDate,
            org.springframework.data.domain.Pageable pageable
    );
}
