package com.flowerbed.repository;

import com.flowerbed.domain.Diary;
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
    Optional<Diary> findByUserUserIdAndDiaryDate(Long userId, LocalDate diaryDate);

    // 월별 일기 조회
    @Query("SELECT d FROM Diary d WHERE d.user.userId = :userId " +
            "AND YEAR(d.diaryDate) = :year AND MONTH(d.diaryDate) = :month " +
            "ORDER BY d.diaryDate DESC")
    List<Diary> findByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 특정 월에 일기가 있는지 확인
    @Query("SELECT COUNT(d) > 0 FROM Diary d WHERE d.user.userId = :userId " +
            "AND YEAR(d.diaryDate) = :year AND MONTH(d.diaryDate) = :month")
    boolean existsByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 사용자의 최근 일기 조회
    List<Diary> findByUserUserIdOrderByDiaryDateDesc(Long userId);
}
