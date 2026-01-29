package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * userId(로그인 ID)로 사용자 조회
     */
    Optional<User> findByUserId(String userId);

    /**
     * userId(로그인 ID) 중복 여부 확인
     */
    boolean existsByUserId(String userId);

    /**
     * 학교 코드, 반 코드, 사용자 타입으로 학생 목록 조회
     * - 선생님이 자신의 학생들을 조회할 때 사용
     * - 이름 오름차순 정렬
     *
     * @param schoolCode 학교 코드
     * @param classCode 반 코드
     * @param userTypeCd 사용자 타입 코드 (STUDENT)
     * @return 학생 목록
     */
    List<User> findBySchoolCodeAndClassCodeAndUserTypeCdOrderByNameAsc(
            String schoolCode,
            String classCode,
            String userTypeCd
    );

    /**
     * 위험 학생 목록 조회 (CAUTION/DANGER)
     * - 같은 학교, 같은 반의 STUDENT 타입 중 위험 상태인 학생 조회
     * - 정렬: DANGER 우선, 같은 레벨 내에서는 위험도 갱신 시각 최신순
     *
     * @param schoolCode 학교 코드
     * @param classCode 반 코드
     * @param userTypeCd 사용자 타입 코드 (STUDENT)
     * @return 위험 학생 목록
     */
    @Query("SELECT u FROM User u " +
            "WHERE u.schoolCode = :schoolCode " +
            "AND u.classCode = :classCode " +
            "AND u.userTypeCd = :userTypeCd " +
            "AND u.riskLevel IN ('CAUTION', 'DANGER') " +
            "ORDER BY " +
            "CASE u.riskLevel " +
            "  WHEN 'DANGER' THEN 1 " +
            "  WHEN 'CAUTION' THEN 2 " +
            "  ELSE 3 " +
            "END, " +
            "u.riskUpdatedAt DESC")
    List<User> findAtRiskStudents(
            @Param("schoolCode") String schoolCode,
            @Param("classCode") String classCode,
            @Param("userTypeCd") String userTypeCd
    );

    /**
     * 특정 위험 레벨 학생 목록 조회
     * - 같은 학교, 같은 반의 STUDENT 타입 중 특정 위험 레벨 학생 조회
     * - 정렬: 위험도 갱신 시각 최신순
     *
     * @param schoolCode 학교 코드
     * @param classCode 반 코드
     * @param userTypeCd 사용자 타입 코드 (STUDENT)
     * @param riskLevel 위험 레벨 (CAUTION 또는 DANGER)
     * @return 위험 학생 목록
     */
    @Query("SELECT u FROM User u " +
            "WHERE u.schoolCode = :schoolCode " +
            "AND u.classCode = :classCode " +
            "AND u.userTypeCd = :userTypeCd " +
            "AND u.riskLevel = :riskLevel " +
            "ORDER BY u.riskUpdatedAt DESC")
    List<User> findAtRiskStudentsByLevel(
            @Param("schoolCode") String schoolCode,
            @Param("classCode") String classCode,
            @Param("userTypeCd") String userTypeCd,
            @Param("riskLevel") String riskLevel
    );
}
