package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.StudentRiskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudentRiskHistoryRepository extends JpaRepository<StudentRiskHistory, Long> {

    /**
     * 특정 학생의 위험도 변화 이력 조회 (최근순)
     */
    List<StudentRiskHistory> findByUserUserSnOrderByCreatedAtDesc(Long userSn);

    /**
     * 특정 학생의 최근 N개 이력 조회
     */
    @Query("SELECT h FROM StudentRiskHistory h WHERE h.user.userSn = :userSn " +
            "ORDER BY h.createdAt DESC")
    List<StudentRiskHistory> findRecentHistories(
            @Param("userSn") Long userSn,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * 특정 기간의 위험도 변화 이력 조회 (통계용)
     */
    @Query("SELECT h FROM StudentRiskHistory h WHERE h.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY h.createdAt DESC")
    List<StudentRiskHistory> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 미확인 이력 조회 (선생님 확인 대기)
     */
    List<StudentRiskHistory> findByIsConfirmedFalseOrderByCreatedAtDesc();

    /**
     * 특정 위험도 레벨의 이력 조회
     */
    List<StudentRiskHistory> findByNewLevelOrderByCreatedAtDesc(String newLevel);
}
