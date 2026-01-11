package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.*;
import com.flowerbed.api.v1.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 선생님 API
 * - 학생 목록 조회
 * - 날짜별 감정 현황 조회
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

    /**
     * 날짜별 학생 감정 현황 조회
     *
     * 선생님이 담당하는 반의 학생들의 특정 날짜 감정 현황을 조회합니다.
     * - 같은 학교(school_code), 같은 반(class_code)의 학생들의 감정 상태 조회
     * - 날짜를 지정하지 않으면 오늘 날짜 기준으로 조회
     *
     * @param date 조회 날짜 (yyyy-MM-dd 형식, 선택)
     * @return 날짜별 감정 현황
     *
     * Response 구조:
     * - date: 조회 날짜
     * - totalCount: 전체 학생 수
     * - area: 영역별 학생 수
     *   - red: 빨강 영역 (강한 감정)
     *   - yellow: 노랑 영역 (활기찬 감정)
     *   - blue: 파랑 영역 (차분한 감정)
     *   - green: 초록 영역 (평온한 감정)
     *   - unanalyzed: 일기 작성했지만 분석 안됨
     *   - none: 일기 미작성
     * - students: 학생별 감정 정보 리스트
     *   - userSn: 학생 일련번호
     *   - name: 학생 이름
     *   - area: 감정 영역 (red, yellow, blue, green, unanalyzed, none)
     *   - coreEmotion: 핵심 감정 코드 (분석된 경우만)
     *   - isAnalyzed: 일기 분석 여부
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 사용자 정보 조회
     * 2. 사용자 타입이 TEACHER인지 확인
     * 3. 선생님의 학교 코드, 반 코드 조회
     * 4. 같은 학교, 같은 반의 모든 STUDENT 타입 회원 조회
     * 5. 특정 날짜의 학생들 일기 조회
     * 6. 학생별 감정 영역 분류 및 집계
     * 7. 감정 현황 정보 반환
     *
     * 사용 예시:
     * ```
     * GET /v1/teachers/daily-emotion-status?date=2026-01-06
     * GET /v1/teachers/daily-emotion-status  (오늘 날짜)
     * Authorization: Bearer {accessToken}
     * ```
     *
     * !! 주의 !!
     * - TEACHER 타입만 접근 가능
     * - 학교 코드 또는 반 코드가 없으면 오류 발생
     * - 다른 반 학생은 조회 불가
     */
    @Operation(summary = "날짜별 학생 감정 현황 조회", description = "선생님이 담당하는 반의 학생들의 특정 날짜 감정 현황을 조회합니다")
    @GetMapping("/daily-emotion-status")
    public ResponseEntity<DailyEmotionStatusResponse> getDailyEmotionStatus(
            @Parameter(description = "조회 날짜 (yyyy-MM-dd 형식, 미지정 시 오늘 날짜)", example = "2026-01-06")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        DailyEmotionStatusResponse response = teacherService.getDailyEmotionStatus(date);
        return ResponseEntity.ok(response);
    }

    /**
     * DANGER 상태 해제
     *
     * 선생님이 DANGER 상태인 학생을 상담하고 위험 상태를 해제합니다.
     * - DANGER 상태는 선생님만 해제 가능
     * - 해제 후에도 다음 일기 분석 시까지 DANGER 유지됨
     * - 다음 일기 분석에서 연속이 끊겼으면 자동으로 NORMAL/CAUTION으로 변경
     *
     * @param studentUserSn 학생 user_sn
     * @param request 해제 사유
     * @return 성공 응답
     *
     * Request Body:
     * - memo: 해제 사유 (필수)
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 선생님 정보 조회
     * 2. 학생 조회 및 같은 학교, 같은 반 확인
     * 3. DANGER 상태 확인
     * 4. 해제 처리 (danger_resolved_by, danger_resolved_at, danger_resolve_memo 설정)
     * 5. 다음 일기 분석 시 자동으로 위험도 재평가
     *
     * 사용 예시:
     * ```
     * POST /v1/teachers/students/1/resolve-danger
     * Authorization: Bearer {accessToken}
     * Content-Type: application/json
     *
     * {
     *   "memo": "학생 상담 완료. 최근 상태 개선 확인."
     * }
     * ```
     *
     * !! 주의 !!
     * - TEACHER 타입만 접근 가능
     * - 같은 학교, 같은 반의 학생만 해제 가능
     * - DANGER 상태가 아니면 오류 발생
     */
    @Operation(summary = "DANGER 상태 해제", description = "선생님이 DANGER 상태인 학생의 위험 상태를 해제합니다")
    @PostMapping("/students/{studentUserSn}/resolve-danger")
    public ResponseEntity<Void> resolveDangerStatus(
            @Parameter(description = "학생 user_sn", required = true)
            @PathVariable Long studentUserSn,
            @Valid @RequestBody ResolveDangerRequest request
    ) {
        teacherService.resolveDangerStatus(studentUserSn, request.getMemo());
        return ResponseEntity.ok().build();
    }

    /**
     * 위험 학생 리스트 조회 (CAUTION/DANGER)
     *
     * 선생님이 담당하는 반의 CAUTION 또는 DANGER 상태 학생 목록을 조회합니다.
     * - 같은 학교(school_code), 같은 반(class_code)의 위험 상태 학생만 조회
     * - level 파라미터로 필터링 가능 (ALL, CAUTION, DANGER)
     * - DANGER 우선 정렬, 같은 레벨 내에서는 위험도 갱신 시각 최신순
     *
     * @param level 위험 레벨 필터 (ALL, CAUTION, DANGER, 기본값: ALL)
     * @return 위험 학생 리스트
     *
     * Response 구조:
     * - totalCount: 전체 위험 학생 수
     * - dangerCount: DANGER 학생 수
     * - cautionCount: CAUTION 학생 수
     * - students: 위험 학생 목록
     *   - userSn: 학생 일련번호
     *   - name: 학생 이름
     *   - riskLevel: 위험도 레벨 (CAUTION, DANGER)
     *   - riskReason: 위험도 판정 사유
     *   - riskContinuousArea: 연속된 감정 영역 (red, yellow, blue, green)
     *   - riskContinuousDays: 연속 일수
     *   - riskUpdatedAt: 위험도 갱신 시각
     *   - dangerResolvedBy: DANGER 해제한 선생님 user_sn (해제된 경우만)
     *   - dangerResolvedAt: DANGER 해제 시각 (해제된 경우만)
     *   - dangerResolveMemo: DANGER 해제 사유 (해제된 경우만)
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 선생님 정보 조회
     * 2. level 파라미터에 따라 적절한 쿼리 실행
     *    - ALL: CAUTION + DANGER 모두 조회
     *    - CAUTION: CAUTION만 조회
     *    - DANGER: DANGER만 조회
     * 3. 집계 및 정렬 처리
     * 4. 위험 학생 정보 반환
     *
     * 사용 예시:
     * ```
     * GET /v1/teachers/students/at-risk?level=ALL
     * GET /v1/teachers/students/at-risk?level=DANGER
     * GET /v1/teachers/students/at-risk?level=CAUTION
     * GET /v1/teachers/students/at-risk  (level 미지정 시 ALL)
     * Authorization: Bearer {accessToken}
     * ```
     *
     * !! 주의 !!
     * - TEACHER 타입만 접근 가능
     * - 학교 코드 또는 반 코드가 없으면 오류 발생
     * - 다른 반 학생은 조회 불가
     * - NORMAL 상태 학생은 포함되지 않음
     */
    @Operation(summary = "위험 학생 리스트 조회", description = "선생님이 담당하는 반의 CAUTION/DANGER 상태 학생 목록을 조회합니다")
    @GetMapping("/students/at-risk")
    public ResponseEntity<AtRiskStudentsResponse> getAtRiskStudents(
            @Parameter(description = "위험 레벨 필터 (ALL, CAUTION, DANGER)", example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") String level
    ) {
        AtRiskStudentsResponse response = teacherService.getAtRiskStudents(level);
        return ResponseEntity.ok(response);
    }

    /**
     * 학생별 위험도 변화 이력 조회
     *
     * 선생님이 특정 학생의 위험도 변화 이력을 조회합니다.
     * - 같은 학교, 같은 반의 학생만 조회 가능
     * - 최근순으로 정렬되어 반환
     *
     * @param studentUserSn 학생 user_sn
     * @return 학생 위험도 변화 이력
     *
     * Response 구조:
     * - userSn: 학생 일련번호
     * - name: 학생 이름
     * - totalCount: 전체 이력 개수
     * - histories: 위험도 변화 이력 목록
     *   - historyId: 이력 ID
     *   - previousLevel: 이전 위험도 레벨
     *   - newLevel: 새 위험도 레벨
     *   - riskType: 위험 유형 (KEYWORD_DETECTED, CONTINUOUS_RED_BLUE, CONTINUOUS_SAME_AREA, RESOLVED)
     *   - riskReason: 위험도 판정 사유
     *   - continuousArea: 연속된 감정 영역
     *   - continuousDays: 연속 일수
     *   - concernKeywords: 탐지된 우려 키워드 목록
     *   - isConfirmed: 선생님 확인 여부
     *   - confirmedBy: 확인한 선생님 user_sn
     *   - confirmedAt: 확인 시각
     *   - teacherMemo: 선생님 메모
     *   - createdAt: 이력 생성 시각
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 선생님 정보 조회
     * 2. 학생 조회 및 같은 학교, 같은 반 확인
     * 3. 학생의 위험도 변화 이력 조회 (최근순)
     * 4. 이력 정보 반환
     *
     * 사용 예시:
     * ```
     * GET /v1/teachers/students/1/risk-history
     * Authorization: Bearer {accessToken}
     * ```
     *
     * !! 주의 !!
     * - TEACHER 타입만 접근 가능
     * - 같은 학교, 같은 반의 학생만 조회 가능
     * - 학생을 찾을 수 없으면 오류 발생
     */
    @Operation(summary = "학생별 위험도 변화 이력 조회", description = "선생님이 특정 학생의 위험도 변화 이력을 조회합니다")
    @GetMapping("/students/{studentUserSn}/risk-history")
    public ResponseEntity<StudentRiskHistoryResponse> getStudentRiskHistory(
            @Parameter(description = "학생 user_sn", required = true)
            @PathVariable Long studentUserSn
    ) {
        StudentRiskHistoryResponse response = teacherService.getStudentRiskHistory(studentUserSn);
        return ResponseEntity.ok(response);
    }

    /**
     * 학생별 주간 리포트 조회
     *
     * 선생님이 특정 학생의 주간 리포트 목록을 조회합니다.
     * - 같은 학교, 같은 반의 학생만 조회 가능
     * - 최근순으로 정렬되어 반환
     *
     * @param studentUserSn 학생 user_sn
     * @return 학생의 주간 리포트 목록
     *
     * Response 구조:
     * - reportId: 리포트 ID
     * - startDate: 시작 날짜 (월요일)
     * - endDate: 종료 날짜 (일요일)
     * - diaryCount: 해당 주의 일기 개수
     * - isAnalyzed: 분석 완료 여부
     * - readYn: 읽음 여부
     * - createdAt: 생성 시각
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 선생님 정보 조회
     * 2. 학생 조회 및 같은 학교, 같은 반 확인
     * 3. 학생의 주간 리포트 조회 (최근순)
     * 4. 리포트 목록 반환
     *
     * 사용 예시:
     * ```
     * GET /v1/teachers/students/2/weekly-reports
     * Authorization: Bearer {accessToken}
     * ```
     *
     * !! 주의 !!
     * - TEACHER 타입만 접근 가능
     * - 같은 학교, 같은 반의 학생만 조회 가능
     * - 분석 완료/미완료 모두 포함 (isAnalyzed 무관)
     * - 학생을 찾을 수 없으면 오류 발생
     */
    @Operation(summary = "학생별 주간 리포트 조회", description = "선생님이 특정 학생의 주간 리포트 목록을 조회합니다 (분석 완료/미완료 모두 포함)")
    @GetMapping("/students/{studentUserSn}/weekly-reports")
    public ResponseEntity<List<WeeklyReportListItemResponse>> getStudentWeeklyReports(
            @Parameter(description = "학생 user_sn", required = true)
            @PathVariable Long studentUserSn
    ) {
        List<WeeklyReportListItemResponse> response = teacherService.getStudentWeeklyReports(studentUserSn);
        return ResponseEntity.ok(response);
    }

    /**
     * 학생별 주간 리포트 상세 조회
     * GET /api/v1/teachers/students/{studentUserSn}/weekly-reports/{reportId}
     *
     * 선생님이 특정 학생의 특정 주간 리포트 상세를 조회합니다.
     * - TEACHER 타입만 접근 가능
     * - 같은 학교, 같은 반의 학생만 조회 가능
     * - teacherReport, teacherTalkTip 포함
     * - 분석 완료/미완료 모두 조회 가능 (isAnalyzed 무관)
     *   - isAnalyzed=false인 경우 일부 필드는 null일 수 있음
     */
    @Operation(summary = "학생별 주간 리포트 상세 조회", description = "선생님이 특정 학생의 주간 리포트 상세를 조회합니다 (분석 완료/미완료 모두 조회 가능)")
    @GetMapping("/students/{studentUserSn}/weekly-reports/{reportId}")
    public ResponseEntity<TeacherWeeklyReportDetailResponse> getStudentWeeklyReportDetail(
            @Parameter(description = "학생 user_sn", required = true)
            @PathVariable Long studentUserSn,
            @Parameter(description = "주간 리포트 ID", required = true)
            @PathVariable Long reportId
    ) {
        TeacherWeeklyReportDetailResponse response = teacherService.getStudentWeeklyReportDetail(studentUserSn, reportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/students/{studentUserSn}/monthly-emotions")
    public ResponseEntity<TeacherMonthlyDiariesResponse> getStudentMonthlyEmotions(
            @Parameter(description = "학생 user_sn", required = true)
            @PathVariable Long studentUserSn,
            @Parameter(description = "년월 (YYYY-MM)", example = "2025-12")
            @RequestParam String yearMonth
    ) {
        TeacherMonthlyDiariesResponse response = teacherService.getStudentMonthlyEmotions(studentUserSn, yearMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * 학급 월별 감정 분포 조회
     *
     * 선생님이 담당하는 반의 월별 일자별 감정 분포를 조회합니다.
     * - 일기 미작성: none
     * - 일기 작성했지만 분석 안됨: unanalyzed
     * - 감정 영역: red, yellow, blue, green
     *
     * @param yearMonth 년월 (YYYY-MM)
     * @return 월별 일자별 감정 분포
     */
    @Operation(summary = "학급 월별 감정 분포 조회", description = "선생님이 담당하는 반의 월별 일자별 감정 분포를 조회합니다")
    @GetMapping("/class/monthly-emotion-distribution")
    public ResponseEntity<MonthlyEmotionDistributionResponse> getMonthlyEmotionDistribution(
            @Parameter(description = "년월 (YYYY-MM)", example = "2026-01", required = true)
            @RequestParam String yearMonth
    ) {
        MonthlyEmotionDistributionResponse response = teacherService.getMonthlyEmotionDistribution(yearMonth);
        return ResponseEntity.ok(response);
    }
}
