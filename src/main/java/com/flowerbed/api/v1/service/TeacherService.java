package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Diary;
import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.domain.StudentRiskHistory;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.dto.*;
import com.flowerbed.api.v1.repository.DiaryRepository;
import com.flowerbed.api.v1.repository.FlowerRepository;
import com.flowerbed.api.v1.repository.StudentRiskHistoryRepository;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.api.v1.repository.WeeklyReportRepository;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 선생님 관련 비즈니스 로직
 * - 학생 목록 조회
 * - 날짜별 감정 현황 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherService {

    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final EmotionCacheService emotionCacheService;
    private final StudentRiskHistoryRepository riskHistoryRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final com.flowerbed.service.WeeklyReportService weeklyReportService;

    /**
     * 내 학생 목록 조회
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 STUDENT 타입 회원 조회
     * - 각 학생의 최근 감정 정보 포함
     *
     * @return 학생 목록
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 사용자 정보 조회
     * 2. 사용자 타입이 TEACHER인지 확인
     * 3. 학교 코드, 반 코드가 있는지 확인
     * 4. 같은 학교, 같은 반의 STUDENT 타입 회원 조회
     * 5. 각 학생의 최근 분석된 일기 조회
     * 6. 최근 감정 정보와 함께 StudentResponse로 변환하여 반환
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

        // 5-6. 각 학생의 최근 감정 정보와 함께 StudentResponse로 변환
        return students.stream()
                .map(student -> {
                    StudentResponse response = StudentResponse.from(student);

                    // 최근 분석된 일기 조회 (1개만)
                    List<Diary> recentDiaries = diaryRepository.findRecentAnalyzedDiaries(
                            student.getUserSn(),
                            LocalDate.now(),
                            org.springframework.data.domain.PageRequest.of(0, 1)
                    );

                    if (!recentDiaries.isEmpty()) {
                        Diary recentDiary = recentDiaries.get(0);
                        String coreEmotionCode = recentDiary.getCoreEmotionCode();

                        if (coreEmotionCode != null) {
                            // 감정 정보 조회 (캐싱 적용)
                            Emotion emotion = emotionCacheService.getEmotion(coreEmotionCode);
                            if (emotion != null) {
                                response.setRecentEmotionArea(emotion.getArea() != null ? emotion.getArea().toLowerCase() : null);
                                response.setRecentCoreEmotionCd(coreEmotionCode);
                                response.setRecentCoreEmotionNameKr(emotion.getEmotionNameKr());
                                response.setRecentCoreEmotionImage(emotion.getImageFile3d());
                            }
                        }
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * 날짜별 학생 감정 현황 조회
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 학생들의 특정 날짜 감정 현황 조회
     *
     * @param date 조회 날짜 (null이면 오늘 날짜)
     * @return 날짜별 감정 현황
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 선생님 정보 조회 및 권한 확인
     * 2. 같은 학교, 같은 반의 모든 학생 목록 조회
     * 3. 특정 날짜의 학생들 일기 조회
     * 4. 학생별 감정 영역 분류:
     *    - 일기 있고 분석됨 → emotion의 area (red, yellow, blue, green)
     *    - 일기 있고 분석 안됨 → unanalyzed
     *    - 일기 없음 → none
     * 5. 영역별 집계
     * 6. 응답 DTO 생성
     */
    public DailyEmotionStatusResponse getDailyEmotionStatus(LocalDate date) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 감정 현황을 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 날짜가 없으면 오늘 날짜
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // 2. 같은 학교, 같은 반의 모든 학생 목록 조회
        List<User> students = userRepository.findBySchoolCodeAndClassCodeAndUserTypeCdOrderByNameAsc(
                teacher.getSchoolCode(),
                teacher.getClassCode(),
                "STUDENT"
        );

        if (students.isEmpty()) {
            log.warn("No students found for teacher={}, school={}, class={}",
                    teacher.getUserId(), teacher.getSchoolCode(), teacher.getClassCode());
        }

        // 3. 특정 날짜의 학생들 일기 조회
        List<Long> studentUserSnList = students.stream()
                .map(User::getUserSn)
                .collect(Collectors.toList());

        List<Diary> diaries = studentUserSnList.isEmpty()
                ? Collections.emptyList()
                : diaryRepository.findByUserSnListAndDate(studentUserSnList, targetDate);

        // 일기를 Map으로 변환 (userSn -> Diary)
        Map<Long, Diary> diaryMap = diaries.stream()
                .collect(Collectors.toMap(d -> d.getUser().getUserSn(), d -> d));

        // 4. 학생별 감정 영역 분류 및 응답 데이터 생성
        Map<String, Integer> areaCount = new HashMap<>();
        areaCount.put("red", 0);
        areaCount.put("yellow", 0);
        areaCount.put("blue", 0);
        areaCount.put("green", 0);
        areaCount.put("unanalyzed", 0);
        areaCount.put("none", 0);

        List<DailyEmotionStatusResponse.StudentEmotionInfo> studentInfoList = new ArrayList<>();

        for (User student : students) {
            Diary diary = diaryMap.get(student.getUserSn());

            String area;
            String coreEmotion = null;
            String coreEmotionNameKr = null;
            Boolean isAnalyzed = false;

            if (diary == null) {
                // 일기 없음
                area = "none";
                isAnalyzed = false;
            } else if (!diary.getIsAnalyzed()) {
                // 일기 있지만 분석 안됨
                area = "unanalyzed";
                isAnalyzed = false;
            } else {
                // 일기 있고 분석됨
                isAnalyzed = true;
                coreEmotion = diary.getCoreEmotionCode();

                // 감정 코드로 area와 emotionNameKr 조회 (캐싱 적용)
                Emotion emotion = emotionCacheService.getEmotion(coreEmotion);
                if (emotion != null) {
                    area = emotion.getArea().toLowerCase();
                    coreEmotionNameKr = emotion.getEmotionNameKr();
                } else {
                    area = "none";  // 감정 정보가 없으면 none
                }
            }

            // 영역별 카운트 증가
            areaCount.put(area, areaCount.get(area) + 1);

            // 학생 정보 추가
            studentInfoList.add(DailyEmotionStatusResponse.StudentEmotionInfo.builder()
                    .userSn(student.getUserSn())
                    .name(student.getName())
                    .area(area)
                    .coreEmotion    (coreEmotion)
                    .coreEmotionNameKr(coreEmotionNameKr)
                    .isAnalyzed(isAnalyzed)
                    .build());
        }

        log.info("Teacher {} retrieved daily emotion status: date={}, totalCount={}, areaCount={}",
                teacher.getUserId(), targetDate, students.size(), areaCount);

        // 6. 응답 DTO 생성
        return DailyEmotionStatusResponse.builder()
                .date(targetDate)
                .totalCount(students.size())
                .area(areaCount)
                .students(studentInfoList)
                .build();
    }

    /**
     * DANGER 상태 해제
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 학생만 해제 가능
     * - DANGER 상태인 학생만 해제 가능
     *
     * @param studentUserSn 학생 user_sn
     * @param memo 해제 사유
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 선생님 정보 조회 및 권한 확인
     * 2. 학생 조회 및 같은 학교, 같은 반 확인
     * 3. DANGER 상태 확인
     * 4. 해제 처리:
     *    - danger_resolved_by, danger_resolved_at, danger_resolve_memo 설정 (기록용)
     *    - 즉시 riskLevel을 NORMAL로 변경
     *    - StudentRiskHistory에 DANGER → NORMAL 이력 기록
     *
     * 예외:
     * - TEACHER 타입이 아니면 FORBIDDEN 에러
     * - 다른 학교/반 학생이면 FORBIDDEN 에러
     * - DANGER 상태가 아니면 BAD_REQUEST 에러
     */
    @Transactional
    public void resolveDangerStatus(Long studentUserSn, String memo) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 위험 상태를 해제할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. 학생 조회 및 같은 학교, 같은 반 확인
        User student = userRepository.findById(studentUserSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "학생을 찾을 수 없습니다"));

        if (!teacher.getSchoolCode().equals(student.getSchoolCode()) ||
            !teacher.getClassCode().equals(student.getClassCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "다른 반 학생의 위험 상태는 해제할 수 없습니다");
        }

        // 3. DANGER 상태 확인
        if (!"DANGER".equals(student.getRiskLevel())) {
            throw new BusinessException(ErrorCode.INVALID_RISK_LEVEL,
                    "DANGER 상태가 아닙니다");
        }

        String previousLevel = student.getRiskLevel();
        String previousArea = student.getRiskContinuousArea();
        Integer previousDays = student.getRiskContinuousDays();

        // 4. 해제 처리
        // 4-1. 해제 정보 설정 (기록용)
        student.resolveDanger(teacher.getUserSn(), memo);

        // 4-2. 즉시 위험도를 NORMAL로 변경 (수동 해제이므로 기준 일기 정보는 null)
        student.updateRiskStatus("NORMAL", null, 0, null,
                java.time.LocalDate.now(), null, null);

        // 4-3. 이력 기록
        StudentRiskHistory history = StudentRiskHistory.builder()
                .user(student)
                .previousLevel(previousLevel)
                .newLevel("NORMAL")
                .riskType("RESOLVED")
                .riskReason("수동 해제")
                .continuousArea(previousArea)
                .continuousDays(previousDays)
                .concernKeywords(null)
                .targetDiaryDate(null)
                .targetDiarySn(null)
                .build();

        // 선생님이 해제한 것이므로 즉시 확인 처리
        history.confirmByTeacher(teacher.getUserSn(), memo);

        riskHistoryRepository.save(history);

        log.info("DANGER 상태 해제 완료: student={}, teacher={}, memo={}, DANGER → NORMAL",
                student.getUserId(), teacher.getUserId(), memo);
    }

    /**
     * 위험 학생 리스트 조회 (CAUTION/DANGER)
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 CAUTION/DANGER 상태 학생 조회
     *
     * @param level 위험 레벨 필터 (ALL, CAUTION, DANGER)
     * @return 위험 학생 리스트
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 선생님 정보 조회 및 권한 확인
     * 2. level 파라미터에 따라 적절한 쿼리 실행
     *    - ALL: CAUTION + DANGER 모두 조회
     *    - CAUTION: CAUTION만 조회
     *    - DANGER: DANGER만 조회
     * 3. 결과를 AtRiskStudentsResponse로 변환
     *    - totalCount, dangerCount, cautionCount 계산
     *    - students 리스트 생성
     * 4. 정렬: DANGER 우선, 같은 레벨 내에서는 위험도 갱신 시각 최신순
     *
     * 예외:
     * - TEACHER 타입이 아니면 FORBIDDEN 에러
     * - 학교 코드 또는 반 코드가 없으면 BAD_REQUEST 에러
     */
    public AtRiskStudentsResponse getAtRiskStudents(String level) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 위험 학생 목록을 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. level에 따라 학생 목록 조회
        List<User> students;
        if (level == null || "ALL".equalsIgnoreCase(level)) {
            // CAUTION + DANGER 모두 조회
            students = userRepository.findAtRiskStudents(
                    teacher.getSchoolCode(),
                    teacher.getClassCode(),
                    "STUDENT"
            );
        } else if ("CAUTION".equalsIgnoreCase(level)) {
            // CAUTION만 조회
            students = userRepository.findAtRiskStudentsByLevel(
                    teacher.getSchoolCode(),
                    teacher.getClassCode(),
                    "STUDENT",
                    "CAUTION"
            );
        } else if ("DANGER".equalsIgnoreCase(level)) {
            // DANGER만 조회
            students = userRepository.findAtRiskStudentsByLevel(
                    teacher.getSchoolCode(),
                    teacher.getClassCode(),
                    "STUDENT",
                    "DANGER"
            );
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "유효하지 않은 level 파라미터입니다: " + level);
        }

        // 3. 집계 및 응답 생성
        int dangerCount = (int) students.stream()
                .filter(s -> "DANGER".equals(s.getRiskLevel()))
                .count();

        int cautionCount = (int) students.stream()
                .filter(s -> "CAUTION".equals(s.getRiskLevel()))
                .count();

        List<AtRiskStudentsResponse.AtRiskStudentInfo> studentInfoList = students.stream()
                .map(AtRiskStudentsResponse.AtRiskStudentInfo::from)
                .collect(Collectors.toList());

        log.info("Teacher {} retrieved at-risk students: level={}, totalCount={}, dangerCount={}, cautionCount={}",
                teacher.getUserId(), level, students.size(), dangerCount, cautionCount);

        return AtRiskStudentsResponse.builder()
                .totalCount(students.size())
                .dangerCount(dangerCount)
                .cautionCount(cautionCount)
                .students(studentInfoList)
                .build();
    }

    /**
     * 학생별 위험도 변화 이력 조회
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 학생만 조회 가능
     *
     * @param studentUserSn 학생 user_sn
     * @return 학생 위험도 변화 이력
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 선생님 정보 조회 및 권한 확인
     * 2. 학생 조회 및 같은 학교, 같은 반 확인
     * 3. 학생의 위험도 변화 이력 조회 (최근순)
     * 4. StudentRiskHistoryResponse로 변환하여 반환
     *
     * 예외:
     * - TEACHER 타입이 아니면 FORBIDDEN 에러
     * - 학생을 찾을 수 없으면 NOT_FOUND 에러
     * - 다른 학교/반 학생이면 FORBIDDEN 에러
     */
    public StudentRiskHistoryResponse getStudentRiskHistory(Long studentUserSn) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 학생 위험도 이력을 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. 학생 조회 및 같은 학교, 같은 반 확인
        User student = userRepository.findById(studentUserSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "학생을 찾을 수 없습니다"));

        if (!teacher.getSchoolCode().equals(student.getSchoolCode()) ||
            !teacher.getClassCode().equals(student.getClassCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "다른 반 학생의 위험도 이력은 조회할 수 없습니다");
        }

        // 3. 학생의 위험도 변화 이력 조회 (최근순)
        List<StudentRiskHistory> histories = riskHistoryRepository.findByUserUserSnOrderByCreatedAtDesc(studentUserSn);

        log.info("Teacher {} retrieved risk history for student {}: {} histories found",
                teacher.getUserId(), student.getUserId(), histories.size());

        // 4. StudentRiskHistoryResponse로 변환
        return StudentRiskHistoryResponse.from(student.getUserSn(), student.getName(), histories);
    }

    /**
     * 학생별 주간 리포트 조회
     * - 선생님만 접근 가능
     * - 같은 학교, 같은 반의 학생만 조회 가능
     *
     * @param studentUserSn 학생 user_sn
     * @return 학생의 주간 리포트 목록
     *
     * 비즈니스 로직:
     * 1. 현재 로그인한 선생님 정보 조회 및 권한 확인
     * 2. 학생 조회 및 같은 학교, 같은 반 확인
     * 3. 학생의 주간 리포트 조회 (최근순)
     * 4. WeeklyReportListItemResponse로 변환하여 반환
     *
     * 예외:
     * - TEACHER 타입이 아니면 FORBIDDEN 에러
     * - 학생을 찾을 수 없으면 NOT_FOUND 에러
     * - 다른 학교/반 학생이면 FORBIDDEN 에러
     */
    public List<WeeklyReportListItemResponse> getStudentWeeklyReports(Long studentUserSn) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 학생 주간 리포트를 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. 학생 조회 및 같은 학교, 같은 반 확인
        User student = userRepository.findById(studentUserSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "학생을 찾을 수 없습니다"));

        if (!teacher.getSchoolCode().equals(student.getSchoolCode()) ||
            !teacher.getClassCode().equals(student.getClassCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "다른 반 학생의 주간 리포트는 조회할 수 없습니다");
        }

        // 3. 학생의 주간 리포트 조회 (최근순, 분석 여부 무관)
        List<WeeklyReport> reports = weeklyReportRepository.findByUserUserSnAndDeletedAtIsNullOrderByStartDateDesc(studentUserSn);

        log.info("Teacher {} retrieved weekly reports for student {}: {} reports found (analyzed: {}, unanalyzed: {})",
                teacher.getUserId(), student.getUserId(), reports.size(),
                reports.stream().filter(WeeklyReport::getIsAnalyzed).count(),
                reports.stream().filter(r -> !r.getIsAnalyzed()).count());

        // 4. WeeklyReportListItemResponse로 변환 (현재 일기 개수 포함)
        return reports.stream()
                .map(report -> {
                    int currentCount = weeklyReportService.getCurrentDiaryCount(studentUserSn, report.getStartDate(), report.getEndDate());
                    return WeeklyReportListItemResponse.from(report, currentCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 학생별 주간 리포트 상세 조회
     * - 선생님이 특정 학생의 특정 주간 리포트 상세를 조회합니다
     * - 같은 학교, 같은 반의 학생만 조회 가능
     * - teacherReport, teacherTalkTip 포함
     *
     * @param studentUserSn 학생 user_sn
     * @param reportId 주간 리포트 ID
     * @return 주간 리포트 상세 (선생님용)
     */
    public TeacherWeeklyReportDetailResponse getStudentWeeklyReportDetail(Long studentUserSn, Long reportId) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 학생 주간 리포트를 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. 학생 조회 및 같은 학교, 같은 반 확인
        User student = userRepository.findById(studentUserSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "학생을 찾을 수 없습니다"));

        if (!teacher.getSchoolCode().equals(student.getSchoolCode()) ||
            !teacher.getClassCode().equals(student.getClassCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "다른 반 학생의 주간 리포트는 조회할 수 없습니다");
        }

        // 3. 주간 리포트 조회
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEEKLY_REPORT_NOT_ANALYZED,
                        "주간 리포트를 찾을 수 없습니다"));

        // 4. 리포트가 해당 학생의 것인지 확인
        if (!report.getUser().getUserSn().equals(studentUserSn)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "해당 리포트는 지정된 학생의 것이 아닙니다");
        }

        log.info("Teacher {} retrieved weekly report detail for student {}: reportId={}, isAnalyzed={}",
                teacher.getUserId(), student.getUserId(), reportId, report.getIsAnalyzed());

        // 6. TeacherWeeklyReportDetailResponse로 변환
        return TeacherWeeklyReportDetailResponse.from(report, emotionCacheService);
    }

    public TeacherMonthlyDiariesResponse getStudentMonthlyEmotions(Long studentUserSn, String yearMonth) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 학생 월별 감정 정보를 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. 학생 조회 및 같은 학교, 같은 반 확인
        User student = userRepository.findById(studentUserSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                        "학생을 찾을 수 없습니다"));

        if (!teacher.getSchoolCode().equals(student.getSchoolCode()) ||
                !teacher.getClassCode().equals(student.getClassCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "다른 반 학생의 감정 정보는 조회할 수 없습니다");
        }

        // 3. 월 데이터 파싱
        YearMonth ym = YearMonth.parse(yearMonth);
        int year = ym.getYear();
        int month = ym.getMonthValue();

        // 4. 월별 감정 정보 조회
        List<Diary> diaries = diaryRepository.findByUserSnAndYearMonth(studentUserSn, year, month);

        // 5. 선생님 Response 형태로 파싱
        List<TeacherMonthlyDiariesResponse.EmotionListItem> items = diaries.stream()
                .map(this::convertToListItem)
                .toList();

        return TeacherMonthlyDiariesResponse.builder()
                .yearMonth(yearMonth)
                .emotions(items)
                .totalCount(items.size())
                .build();
    }

    /**
     * Entity -> ListItem 변환
     */
    private TeacherMonthlyDiariesResponse.EmotionListItem convertToListItem(Diary diary) {
        List<EmotionPercent> emotions = null;

        if (diary.getEmotionsJson() != null) {
            emotions = diary.getEmotionsJson().stream()
                    .map(e -> {
                        String color = e.getColor();
                        String emotionNameKr = e.getEmotionNameKr();

                        // null이면 DB 조회해서 채움 (기존 데이터 대응)
                        if (color == null || emotionNameKr == null) {
                            Emotion emotion = emotionCacheService.getEmotion(e.getEmotion());
                            if (emotion != null) {
                                if (color == null) {
                                    color = emotion.getColor();
                                }
                                if (emotionNameKr == null) {
                                    emotionNameKr = emotion.getEmotionNameKr();
                                }
                            }
                        }

                        EmotionPercent ep = new EmotionPercent(e.getEmotion(), e.getPercent(), color);
                        ep.setEmotionNameKr(emotionNameKr);
                        return ep;
                    })
                    .collect(Collectors.toList());
        }

        // coreEmotion 상세 정보 조회
        TeacherMonthlyDiariesResponse.EmotionDetail coreEmotionDetail = null;
        if (diary.getCoreEmotionCode() != null) {
            Emotion coreEmotion = emotionCacheService.getEmotion(diary.getCoreEmotionCode());
            if (coreEmotion != null) {
                coreEmotionDetail = convertToEmotionDetail(coreEmotion);
            }
        }

        return TeacherMonthlyDiariesResponse.EmotionListItem.builder()
                .id(diary.getDiaryId())
                .date(diary.getDiaryDate())
                .isAnalyzed(diary.getIsAnalyzed())
                .coreEmotionCode(diary.getCoreEmotionCode())
                .emotions(emotions)
                .coreEmotionDetail(coreEmotionDetail)
                .build();
    }


    /**
     * Emotion Entity -> FlowerDetail DTO 변환
     */
    private TeacherMonthlyDiariesResponse.EmotionDetail convertToEmotionDetail(Emotion emotion) {
        return TeacherMonthlyDiariesResponse.EmotionDetail.builder()
                .emotionCode(emotion.getEmotionCode())
                .emotionNameKr(emotion.getEmotionNameKr())
                .emotionNameEn(emotion.getEmotionNameEn())
                .emotionArea(emotion.getArea())
                .flowerNameKr(emotion.getFlowerNameKr())
                .flowerNameEn(emotion.getFlowerNameEn())
                .flowerMeaning(emotion.getFlowerMeaning())
                .imageFile3d(emotion.getImageFile3d())
                .build();
    }

    /**
     * 학급 월별 감정 분포 조회
     * - 선생님이 담당하는 반의 월별 일자별 감정 분포를 조회
     * - 일기 미작성: none, 일기 작성했지만 분석 안됨: unanalyzed
     *
     * @param yearMonth 년월 (YYYY-MM)
     * @return 월별 일자별 감정 분포
     */
    public MonthlyEmotionDistributionResponse getMonthlyEmotionDistribution(String yearMonth) {
        // 1. 현재 로그인한 선생님 조회 및 권한 확인
        User teacher = SecurityUtil.getCurrentUser();

        if (!"TEACHER".equals(teacher.getUserTypeCd())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "선생님만 학급 월별 감정 분포를 조회할 수 있습니다");
        }

        if (teacher.getSchoolCode() == null || teacher.getClassCode() == null) {
            throw new BusinessException(ErrorCode.NO_SCHOOL_INFO,
                    "학교 코드 또는 반 코드가 설정되지 않았습니다");
        }

        // 2. 같은 학교, 같은 반의 모든 학생 조회
        List<User> students = userRepository.findBySchoolCodeAndClassCodeAndUserTypeCdOrderByNameAsc(
                teacher.getSchoolCode(),
                teacher.getClassCode(),
                "STUDENT"
        );

        // 담당 학생이 없는 경우 빈 응답 반환
        if (students.isEmpty()) {
            return MonthlyEmotionDistributionResponse.builder()
                    .yearMonth(yearMonth)
                    .totalStudents(0)
                    .areaKeywords(MonthlyEmotionDistributionResponse.AreaKeywords.builder()
                            .red(List.of())
                            .yellow(List.of())
                            .blue(List.of())
                            .green(List.of())
                            .build())
                    .dailyDistribution(List.of())
                    .build();
        }

        List<Long> studentSnList = students.stream()
                .map(User::getUserSn)
                .collect(Collectors.toList());

        int totalStudents = students.size();

        // 3. 월 범위 계산
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate startDate = ym.atDay(1);  // 월 첫째 날
        LocalDate endDate = ym.atEndOfMonth();  // 월 마지막 날

        // 4. 해당 월의 모든 학생 일기 한번에 조회
        List<Diary> allDiaries = diaryRepository.findByUserSnListAndDateBetween(
                studentSnList,
                startDate,
                endDate
        );

        // 5. 일기 데이터를 날짜별, 학생별로 그룹핑 (빠른 조회를 위해 Map 사용)
        Map<LocalDate, Map<Long, Diary>> diaryByDateAndStudent = allDiaries.stream()
                .collect(Collectors.groupingBy(
                        Diary::getDiaryDate,
                        Collectors.toMap(
                                d -> d.getUser().getUserSn(),
                                d -> d,
                                (d1, d2) -> d1  // 중복 시 첫 번째 선택 (일반적으로 발생하지 않음)
                        )
                ));

        // 6. 일자별 감정 분포 계산
        List<MonthlyEmotionDistributionResponse.DailyDistribution> dailyDistributions = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Map<Long, Diary> diariesOnDate = diaryByDateAndStudent.getOrDefault(date, new HashMap<>());

            // 영역별 카운트 초기화
            int red = 0, yellow = 0, blue = 0, green = 0, unanalyzed = 0, none = 0;

            // 각 학생별로 감정 영역 분류
            for (Long studentSn : studentSnList) {
                Diary diary = diariesOnDate.get(studentSn);

                if (diary == null) {
                    // 일기 없음
                    none++;
                } else if (!diary.getIsAnalyzed()) {
                    // 일기 있지만 분석 안됨
                    unanalyzed++;
                } else {
                    // 일기 있고 분석됨 → 감정 영역 조회
                    String coreEmotionCode = diary.getCoreEmotionCode();
                    if (coreEmotionCode != null) {
                        Emotion emotion = emotionCacheService.getEmotion(coreEmotionCode);
                        if (emotion != null) {
                            String area = emotion.getArea().toLowerCase();
                            switch (area) {
                                case "red" -> red++;
                                case "yellow" -> yellow++;
                                case "blue" -> blue++;
                                case "green" -> green++;
                                default -> none++;  // 예외 케이스
                            }
                        } else {
                            none++;  // 감정 정보 없음
                        }
                    } else {
                        unanalyzed++;  // 핵심 감정 없음
                    }
                }
            }

            // 일자별 분포 생성
            MonthlyEmotionDistributionResponse.AreaDistribution areaDistribution =
                    MonthlyEmotionDistributionResponse.AreaDistribution.builder()
                            .red(red)
                            .yellow(yellow)
                            .blue(blue)
                            .green(green)
                            .unanalyzed(unanalyzed)
                            .none(none)
                            .build();

            MonthlyEmotionDistributionResponse.DailyDistribution dailyDistribution =
                    MonthlyEmotionDistributionResponse.DailyDistribution.builder()
                            .date(date.toString())
                            .dayOfWeek(getDayOfWeekKorean(date))
                            .area(areaDistribution)
                            .build();

            dailyDistributions.add(dailyDistribution);
        }

        // 7. 영역별 키워드 집계
        MonthlyEmotionDistributionResponse.AreaKeywords areaKeywords = calculateAreaKeywords(allDiaries);

        // 8. 응답 생성
        return MonthlyEmotionDistributionResponse.builder()
                .yearMonth(yearMonth)
                .totalStudents(totalStudents)
                .areaKeywords(areaKeywords)
                .dailyDistribution(dailyDistributions)
                .build();
    }

    /**
     * LocalDate를 한글 요일로 변환
     */
    private String getDayOfWeekKorean(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    /**
     * 영역별 키워드 집계 (빈도 높은 순 최대 5개)
     */
    private MonthlyEmotionDistributionResponse.AreaKeywords calculateAreaKeywords(List<Diary> diaries) {
        // 영역별 키워드 빈도 Map
        Map<String, Map<String, Integer>> areaKeywordFreq = new HashMap<>();
        areaKeywordFreq.put("red", new HashMap<>());
        areaKeywordFreq.put("yellow", new HashMap<>());
        areaKeywordFreq.put("blue", new HashMap<>());
        areaKeywordFreq.put("green", new HashMap<>());

        // 분석된 일기에서 키워드 추출
        for (Diary diary : diaries) {
            if (!diary.getIsAnalyzed() || diary.getCoreEmotionCode() == null || diary.getKeywords() == null) {
                continue;
            }

            // 감정 영역 조회
            Emotion emotion = emotionCacheService.getEmotion(diary.getCoreEmotionCode());
            if (emotion == null) {
                continue;
            }

            String area = emotion.getArea().toLowerCase();
            if (!areaKeywordFreq.containsKey(area)) {
                continue;
            }

            // 키워드를 쉼표로 구분하여 파싱
            String[] keywords = diary.getKeywords().split(",");
            Map<String, Integer> keywordFreq = areaKeywordFreq.get(area);

            for (String keyword : keywords) {
                String trimmedKeyword = keyword.trim();
                if (!trimmedKeyword.isEmpty()) {
                    keywordFreq.put(trimmedKeyword, keywordFreq.getOrDefault(trimmedKeyword, 0) + 1);
                }
            }
        }

        // 각 영역별 상위 5개 키워드 선택
        List<String> redKeywords = getTopKeywords(areaKeywordFreq.get("red"), 5);
        List<String> yellowKeywords = getTopKeywords(areaKeywordFreq.get("yellow"), 5);
        List<String> blueKeywords = getTopKeywords(areaKeywordFreq.get("blue"), 5);
        List<String> greenKeywords = getTopKeywords(areaKeywordFreq.get("green"), 5);

        return MonthlyEmotionDistributionResponse.AreaKeywords.builder()
                .red(redKeywords)
                .yellow(yellowKeywords)
                .blue(blueKeywords)
                .green(greenKeywords)
                .build();
    }

    /**
     * 키워드 빈도 Map에서 상위 N개 키워드 추출
     */
    private List<String> getTopKeywords(Map<String, Integer> keywordFreq, int topN) {
        if (keywordFreq.isEmpty()) {
            return List.of();
        }

        return keywordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
