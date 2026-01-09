package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Diary;
import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.domain.StudentRiskHistory;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.domain.WeeklyReport;
import com.flowerbed.api.v1.dto.AtRiskStudentsResponse;
import com.flowerbed.api.v1.dto.DailyEmotionStatusResponse;
import com.flowerbed.api.v1.dto.StudentResponse;
import com.flowerbed.api.v1.dto.StudentRiskHistoryResponse;
import com.flowerbed.api.v1.dto.TeacherWeeklyReportDetailResponse;
import com.flowerbed.api.v1.dto.WeeklyReportListItemResponse;
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

        // 4-2. 즉시 위험도를 NORMAL로 변경
        student.updateRiskStatus("NORMAL", null, 0, null);

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

        // 4. WeeklyReportListItemResponse로 변환
        return reports.stream()
                .map(WeeklyReportListItemResponse::from)
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
        return TeacherWeeklyReportDetailResponse.from(report);
    }
}
