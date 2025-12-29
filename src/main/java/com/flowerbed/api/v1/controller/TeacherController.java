package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.StudentResponse;
import com.flowerbed.api.v1.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 선생님 API
 * - 학생 목록 조회
 */
@Tag(name = "Teacher", description = "선생님 API")
@Slf4j
@RestController
@RequestMapping("/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    /**
     * 내 학생 목록 조회
     *
     * 선생님이 자신이 담당하는 반의 학생 목록을 조회합니다.
     * - 같은 학교(school_code), 같은 반(class_code)의 학생만 조회
     * - 이름 오름차순으로 정렬
     *
     * @return 학생 목록
     *
     * Response 구조:
     * - userSn: 학생 일련번호
     * - userId: 로그인 ID
     * - name: 이름
     * - schoolCode: 학교 코드
     * - schoolNm: 학교명
     * - classCode: 반 코드
     * - emotionControlCd: 감정 제어 활동 코드
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 사용자 정보 조회
     * 2. 사용자 타입이 TEACHER인지 확인
     * 3. 선생님의 학교 코드, 반 코드 조회
     * 4. 같은 학교, 같은 반의 STUDENT 타입 회원 조회
     * 5. 학생 정보 목록 반환 (이름 오름차순)
     *
     * 사용 예시:
     * ```
     * GET /v1/teachers/students
     * Authorization: Bearer {accessToken}
     * ```
     *
     * !! 주의 !!
     * - TEACHER 타입만 접근 가능
     * - 학교 코드 또는 반 코드가 없으면 오류 발생
     * - 다른 반 학생은 조회 불가
     */
    @Operation(summary = "내 학생 목록 조회", description = "선생님이 담당하는 반의 학생 목록을 조회합니다")
    @GetMapping("/students")
    public ResponseEntity<List<StudentResponse>> getMyStudents() {
        List<StudentResponse> students = teacherService.getMyStudents();
        return ResponseEntity.ok(students);
    }
}
