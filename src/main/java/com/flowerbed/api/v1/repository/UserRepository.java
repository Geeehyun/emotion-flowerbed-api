package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
