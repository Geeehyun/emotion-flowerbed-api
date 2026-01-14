package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Diary;
import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.domain.StudentRiskHistory;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.repository.DiaryRepository;
import com.flowerbed.api.v1.repository.FlowerRepository;
import com.flowerbed.api.v1.repository.StudentRiskHistoryRepository;
import com.flowerbed.api.v1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 학생 위험도 분석 서비스
 * - 일기 분석 후 위험도 체크
 * - 연속 영역 계산
 * - 위험도 레벨 판정 (NORMAL, CAUTION, DANGER)
 * - 이력 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskAnalysisService {

    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final EmotionCacheService emotionCacheService;
    private final StudentRiskHistoryRepository riskHistoryRepository;

    /**
     * 위험도 체크 및 업데이트 (LLM 분석 결과 포함)
     * - 일기 분석 완료 후 호출
     * - 최신 일기일 경우만 위험도 분석 진행
     * - 최근 7일 일기를 조회하여 연속 영역 계산
     * - LLM 키워드 탐지 결과 반영
     * - 위험도 레벨 판정 및 업데이트
     *
     * @param userSn 학생 user_sn
     * @param diaryDate 분석 대상 일기 날짜
     * @param diarySn 분석 대상 일기 SN
     * @param llmRiskLevel LLM이 분석한 위험도 (normal, caution, danger)
     * @param llmRiskReason LLM이 분석한 위험도 사유
     * @param concernKeywords LLM이 탐지한 우려 키워드
     */
    @Transactional
    public void checkAndUpdateRiskLevel(Long userSn, LocalDate diaryDate, Long diarySn,
                                       String llmRiskLevel, String llmRiskReason,
                                       List<String> concernKeywords) {
        User student = userRepository.findById(userSn)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다: " + userSn));

        // STUDENT 타입만 위험도 체크
        if (!"STUDENT".equals(student.getUserTypeCd())) {
            log.debug("STUDENT 타입이 아니므로 위험도 체크 생략: userSn={}", userSn);
            return;
        }

        // 최신 일기일 경우만 위험도 분석 진행
        // 기존 분석 기준 일기 날짜가 있고, 현재 일기 날짜가 더 과거이면 분석 생략
        if (student.getRiskTargetDiaryDate() != null && diaryDate.isBefore(student.getRiskTargetDiaryDate())) {
            log.debug("과거 일기이므로 위험도 분석 생략: userSn={}, diaryDate={}, riskTargetDiaryDate={}",
                    userSn, diaryDate, student.getRiskTargetDiaryDate());
            return;
        }

        // 1. 최근 7일 분석된 일기 조회 (분석 대상 일기 날짜 기준)
        List<Diary> recentDiaries = diaryRepository.findRecentAnalyzedDiaries(
                userSn,
                diaryDate,
                PageRequest.of(0, 7)
        );

        if (recentDiaries.isEmpty()) {
            log.debug("분석된 일기가 없어 위험도 체크 생략: userSn={}", userSn);
            return;
        }

        // 2. 연속 영역 계산
        ContinuousAreaInfo continuousInfo = calculateContinuousArea(recentDiaries);

        // 3. 새 위험도 계산 (LLM 결과 + 연속 일수)
        RiskLevelInfo riskInfo = calculateRiskLevelWithLLM(continuousInfo, llmRiskLevel, llmRiskReason, concernKeywords);
        String newLevel = riskInfo.getLevel();
        String reason = riskInfo.getReason();

        // 4. DANGER 해제 방지 체크
        String currentLevel = student.getRiskLevel() != null ? student.getRiskLevel() : "NORMAL";
        if ("DANGER".equals(currentLevel) && !"DANGER".equals(newLevel)) {
            // 선생님이 해제하지 않았으면 DANGER 유지
            if (student.getDangerResolvedAt() == null) {
                log.warn("DANGER 상태는 선생님이 직접 해제해야 합니다: userSn={}", userSn);
                newLevel = "DANGER";
                reason = student.getRiskReason();  // 기존 사유 유지
            }
        }

        // 5. 상태 변경 시에만 업데이트 및 이력 기록
        LocalDate checkedDate = LocalDate.now();
        if (!newLevel.equals(currentLevel)) {
            updateRiskStatus(student, newLevel, continuousInfo, reason, checkedDate, diaryDate, diarySn);
            saveRiskHistory(student, currentLevel, newLevel, continuousInfo, reason, concernKeywords, diaryDate, diarySn);

            log.info("위험도 변경: userSn={}, {} → {}, reason={}, diaryDate={}",
                    userSn, currentLevel, newLevel, reason, diaryDate);
        } else {
            // 레벨은 같지만 연속 일수나 사유가 바뀔 수 있음
            student.updateRiskStatus(newLevel, continuousInfo.getArea(),
                    continuousInfo.getDays(), reason, checkedDate, diaryDate, diarySn);
            log.debug("위험도 정보 갱신: userSn={}, level={}, days={}, diaryDate={}",
                    userSn, newLevel, continuousInfo.getDays(), diaryDate);
        }
    }

    /**
     * LLM 분석 결과와 연속 일수를 종합하여 위험도 계산
     * - LLM이 danger 탐지 → 즉시 DANGER
     * - LLM이 caution 탐지 → CAUTION (단, 연속 7일 red/blue면 DANGER)
     * - 연속 7일 이상 red/blue → DANGER
     * - 연속 7일 이상 같은 영역 → CAUTION
     */
    private RiskLevelInfo calculateRiskLevelWithLLM(ContinuousAreaInfo continuousInfo,
                                                    String llmRiskLevel, String llmRiskReason,
                                                    List<String> concernKeywords) {
        String level;
        String reason;

        // LLM이 극단적 표현 탐지 → 최우선 DANGER
        if ("danger".equalsIgnoreCase(llmRiskLevel)) {
            level = "DANGER";
            reason = llmRiskReason;
        }
        // 연속 7일 이상 red/blue → DANGER
        else if (continuousInfo.getDays() >= 7) {
            boolean isRedOrBlue = "red".equals(continuousInfo.getArea()) || "blue".equals(continuousInfo.getArea());
            if (isRedOrBlue) {
                level = "DANGER";
                String continuousReason = String.format("%d일 연속 %s 영역 감정 (%s). 강도 높은 감정이 지속되어 주의가 필요합니다.",
                        continuousInfo.getDays(), continuousInfo.getArea(), getAreaName(continuousInfo.getArea()));

                // LLM 사유가 있으면 병합
                if (llmRiskReason != null && !llmRiskReason.isEmpty()) {
                    reason = continuousReason + "\n추가: " + llmRiskReason;
                } else {
                    reason = continuousReason;
                }
            } else {
                // 연속 7일 같은 영역 (red/blue 아님) → CAUTION
                level = "CAUTION";
                reason = String.format("%d일 연속 %s 영역 감정 (%s)",
                        continuousInfo.getDays(), continuousInfo.getArea(), getAreaName(continuousInfo.getArea()));

                if (llmRiskReason != null && !llmRiskReason.isEmpty()) {
                    reason += "\n추가: " + llmRiskReason;
                }
            }
        }
        // LLM이 caution 탐지
        else if ("caution".equalsIgnoreCase(llmRiskLevel)) {
            level = "CAUTION";
            reason = llmRiskReason;
        }
        // 정상
        else {
            level = "NORMAL";
            reason = null;
        }

        return new RiskLevelInfo(level, reason);
    }

    /**
     * 연속 영역 계산
     * - 최근 일기부터 역순으로 확인
     * - 같은 area가 연속되는 일수 카운트
     */
    private ContinuousAreaInfo calculateContinuousArea(List<Diary> recentDiaries) {
        if (recentDiaries.isEmpty()) {
            return new ContinuousAreaInfo(null, 0);
        }

        // 가장 최근 일기의 area
        String baseArea = getEmotionArea(recentDiaries.get(0));
        if (baseArea == null) {
            return new ContinuousAreaInfo(null, 0);
        }

        int continuousDays = 1;

        // 두 번째 일기부터 비교 (날짜 역순이므로)
        for (int i = 1; i < recentDiaries.size(); i++) {
            Diary prevDiary = recentDiaries.get(i - 1);
            Diary currDiary = recentDiaries.get(i);

            // 날짜가 연속인지 확인 (하루 차이)
            LocalDate expectedDate = prevDiary.getDiaryDate().minusDays(1);
            if (!expectedDate.equals(currDiary.getDiaryDate())) {
                break;  // 연속 끊김
            }

            // area가 같은지 확인
            String currArea = getEmotionArea(currDiary);
            if (!baseArea.equals(currArea)) {
                break;  // area 다름
            }

            continuousDays++;
        }

        return new ContinuousAreaInfo(baseArea, continuousDays);
    }

    /**
     * 일기에서 감정 영역 조회 (캐싱 적용)
     */
    private String getEmotionArea(Diary diary) {
        if (diary.getCoreEmotionCode() == null) {
            return null;
        }

        Emotion emotion = emotionCacheService.getEmotion(diary.getCoreEmotionCode());
        return emotion != null ? emotion.getArea().toLowerCase() : null;
    }

    /**
     * 위험도 레벨 계산
     */
    private String calculateRiskLevel(ContinuousAreaInfo info) {
        if (info.getDays() < 7) {
            return "NORMAL";
        }

        // 7일 이상 연속
        boolean isRedOrBlue = "red".equals(info.getArea()) || "blue".equals(info.getArea());
        return isRedOrBlue ? "DANGER" : "CAUTION";
    }

    /**
     * 위험도 사유 생성
     */
    private String generateRiskReason(ContinuousAreaInfo info, String level) {
        if ("NORMAL".equals(level)) {
            return null;
        }

        String areaName = getAreaName(info.getArea());
        String reason = String.format("%d일 연속 %s 영역 감정 (%s)",
                info.getDays(), info.getArea(), areaName);

        if ("DANGER".equals(level)) {
            reason += ". 강도 높은 감정이 지속되어 주의가 필요합니다.";
        }

        return reason;
    }

    /**
     * 영역 이름 조회
     */
    private String getAreaName(String area) {
        switch (area) {
            case "red": return "강한 감정";
            case "yellow": return "활기찬 감정";
            case "blue": return "차분한 감정";
            case "green": return "평온한 감정";
            default: return "";
        }
    }

    /**
     * 위험도 상태 업데이트
     */
    private void updateRiskStatus(User student, String newLevel,
                                  ContinuousAreaInfo info, String reason,
                                  LocalDate checkedDate, LocalDate targetDiaryDate, Long targetDiarySn) {
        student.updateRiskStatus(newLevel, info.getArea(), info.getDays(), reason,
                checkedDate, targetDiaryDate, targetDiarySn);

        // DANGER에서 벗어났으면 해제 정보 초기화
        if (!"DANGER".equals(newLevel) && student.getDangerResolvedAt() != null) {
            student.clearDangerResolveInfo();
        }
    }

    /**
     * 위험도 변화 이력 저장
     */
    private void saveRiskHistory(User student, String previousLevel, String newLevel,
                                 ContinuousAreaInfo info, String reason, List<String> concernKeywords,
                                 LocalDate targetDiaryDate, Long targetDiarySn) {
        String riskType = determineRiskType(info, newLevel, concernKeywords);

        // 자동 해제 여부 확인 (CAUTION/DANGER → NORMAL)
        boolean isAutoResolved = "RESOLVED".equals(riskType)
                && ("CAUTION".equals(previousLevel) || "DANGER".equals(previousLevel))
                && "NORMAL".equals(newLevel);

        // 자동 해제인 경우 사유 간단히 명시
        String finalReason = reason;
        if (isAutoResolved && (reason == null || reason.isEmpty())) {
            finalReason = "자동 해제";
        }

        StudentRiskHistory history = StudentRiskHistory.builder()
                .user(student)
                .previousLevel(previousLevel)
                .newLevel(newLevel)
                .riskType(riskType)
                .riskReason(finalReason)
                .continuousArea(info.getArea())
                .continuousDays(info.getDays())
                .concernKeywords(concernKeywords)
                .targetDiaryDate(targetDiaryDate)
                .targetDiarySn(targetDiarySn)
                .build();

        // 자동 해제인 경우 SYSTEM으로 처리
        if (isAutoResolved) {
            history.confirmBySystem();
        }

        riskHistoryRepository.save(history);
    }

    /**
     * 위험 유형 결정
     */
    private String determineRiskType(ContinuousAreaInfo info, String level, List<String> concernKeywords) {
        // 키워드가 있으면 키워드 탐지 우선
        if (concernKeywords != null && !concernKeywords.isEmpty()) {
            return "KEYWORD_DETECTED";
        }

        if ("DANGER".equals(level)) {
            return "CONTINUOUS_RED_BLUE";
        } else if ("CAUTION".equals(level)) {
            return "CONTINUOUS_SAME_AREA";
        }
        return "RESOLVED";
    }

    /**
     * 위험도 레벨 및 사유 정보
     */
    private static class RiskLevelInfo {
        private final String level;
        private final String reason;

        public RiskLevelInfo(String level, String reason) {
            this.level = level;
            this.reason = reason;
        }

        public String getLevel() {
            return level;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 연속 영역 정보
     */
    private static class ContinuousAreaInfo {
        private final String area;
        private final int days;

        public ContinuousAreaInfo(String area, int days) {
            this.area = area;
            this.days = days;
        }

        public String getArea() {
            return area;
        }

        public int getDays() {
            return days;
        }
    }
}
