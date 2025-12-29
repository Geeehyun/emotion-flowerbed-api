package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.dto.StudentResponse;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 선생님 관련 비즈니스 로직
 * - 학생 목록 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final UserRepository userRepository;

    /**
     * 내 학생 목록 조회
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 STUDENT 타입 회원 조회
     *
     * @return 학생 목록
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 사용자 정보 조회
     * 2. 사용자 타입이 TEACHER인지 확인
     * 3. 학교 코드, 반 코드가 있는지 확인
     * 4. 같은 학교, 같은 반의 STUDENT 타입 회원 조회
     * 5. StudentResponse로 변환하여 반환
     *
     * 예외:
     * - TEACHER 타입이 아니면 FORBIDDEN 에러
     * - 학교 코드 또는 반 코드가 없으면 BAD_REQUEST 에러
     */
    public List<StudentResponse> getMyStudents() {
        // 1. 현재 로그인한 사용자 조회
        User teacher = SecurityUtil.getCurrentUser();

        // 2. TEACHER 타입인지 확인
        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 학생 목록을 조회할 수 있습니다");
        }

        // 3. 학교 코드, 반 코드 확인
        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 4. 같은 학교, 같은 반의 학생 목록 조회 (이름 오름차순)
        List<User> students = userRepository.findBySchoolCodeAndClassCodeAndUserTypeCdOrderByNameAsc(
                teacher.getSchoolCode(),
                teacher.getClassCode(),
                "STUDENT"
        );

        log.info("Teacher {} retrieved {} students from school={}, class={}",
                teacher.getUserId(),
                students.size(),
                teacher.getSchoolCode(),
                teacher.getClassCode());

        // 5. StudentResponse로 변환
        return students.stream()
                .map(StudentResponse::from)
                .collect(Collectors.toList());
    }
}
