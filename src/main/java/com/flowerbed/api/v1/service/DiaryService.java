package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Diary;
import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.dto.*;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.exception.business.DiaryNotFoundException;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.api.v1.repository.DiaryRepository;
import com.flowerbed.api.v1.repository.FlowerRepository;
import com.flowerbed.api.v1.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;

/**
 * 일기 비즈니스 로직 처리
 * - 일기 CRUD
 * - 감정 분석 호출 및 결과 저장
 * - 꽃 정보 조회하여 응답에 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final EmotionCacheService emotionCacheService;
    private final DiaryEmotionService emotionService;  // 실제 Claude API 분석
    private final DiaryEmotionTestService emotionTestService;  // 테스트용 랜덤 분석
    private final RiskAnalysisService riskAnalysisService;  // 위험도 분석

    /**
     * 일기 작성 (감정 분석 X, 내용만 저장)
     */
    @Transactional
    public DiaryResponse createDiary(Long userSn, DiaryCreateRequest request) {

        // 일기 내용 유효성 검사
        validateDiaryContent(request.getContent());

        // 사용자 조회
        User user = userRepository.findById(userSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 같은 날짜에 일기가 이미 있는지 확인
        diaryRepository.findByUserUserSnAndDiaryDateAndDeletedAtIsNull(userSn, request.getDiaryDate())
                .ifPresent(diary -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_DIARY_DATE,
                            "해당 날짜에 이미 일기가 존재합니다");
                });

        // 일기 생성
        Diary diary = new Diary(user, request.getDiaryDate(), request.getContent());

        try {
            Diary savedDiary = diaryRepository.save(diary);
            log.info("Diary created: diaryId={}, userId={}, date={}",
                    savedDiary.getDiaryId(), userSn, request.getDiaryDate());

            return convertToResponse(savedDiary);

        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_DIARY_DATE,
                    "해당 날짜에 이미 일기가 존재합니다");
        }
    }

    /**
     * 일기 감정 분석 (실제 Claude API 사용)
     * - emotionService.analyzeDiary() 호출하여 LLM 분석
     * - 분석 결과를 일기에 저장
     */
    @Transactional
    public DiaryResponse analyzeDiaryEmotion(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserSn().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // 감정 분석 수행
        DiaryEmotionResponse emotionResponse = emotionService.analyzeDiary(diary.getContent());

        // 분석 결과 저장 (color, emotionNameKr 포함)
        List<Diary.EmotionPercent> emotionsJson = emotionResponse.getEmotions().stream()
                .map(e -> {
                    // 감정 코드로 색상 및 이름 조회
                    Emotion emotion = emotionCacheService.getEmotion(e.getEmotion());
                    String color = emotion != null ? emotion.getColor() : null;
                    String emotionNameKr = emotion != null ? emotion.getEmotionNameKr() : null;
                    return new Diary.EmotionPercent(e.getEmotion(), e.getPercent(), color, emotionNameKr);
                })
                .collect(Collectors.toList());

        // 감정 코드 검증 (DB에 존재하는 감정 코드인지 확인)
        String emotionCode = emotionResponse.getCoreEmotion();
        boolean emotionExists = emotionCacheService.getEmotion(emotionCode) != null;

        if(!emotionExists) {
            log.error("Invalid emotion code from LLM: diaryId={}, coreEmotionCode={}",
                    diaryId, emotionCode);
            return null;
            // TODO LLM 응답 이상 (존재하지 않는 감정 코드) 재요청 1회 필요
        }

        diary.updateAnalysis(
                emotionResponse.getSummary(),
                emotionCode,
                emotionResponse.getReason(),
                emotionResponse.getFlower(),
                emotionResponse.getFloriography(),
                emotionsJson
        );

        log.info("Diary emotion analyzed: diaryId={}, coreEmotionCode={}",
                diaryId, emotionResponse.getCoreEmotion());

        // 감정 조절 팁 체크 (오늘 날짜이고 연속 3일 이상인 경우)
        EmotionControlTipInfo tipInfo = checkEmotionControlTip(userId, diary);

        // 위험도 체크 (7일 연속 같은 영역, red/blue 체크 + LLM 키워드 탐지)
        riskAnalysisService.checkAndUpdateRiskLevel(
                userId,
                diary.getDiaryDate(),
                emotionResponse.getRiskLevel(),
                emotionResponse.getRiskReason(),
                emotionResponse.getConcernKeywords()
        );

        return convertToResponse(diary, tipInfo);
    }

    /**
     * 일기 감정 분석 (테스트 모드 - API 비용 없음)
     * - emotionTestService.analyzeForTest() 호출하여 랜덤 생성
     * - DB의 emotions 테이블에서 랜덤 선택
     * - area 파라미터로 특정 감정 영역 지정 가능 (연속 감정 팁 테스트 시 유용)
     */
    @Transactional
    public DiaryResponse analyzeDiaryEmotionTest(Long userId, Long diaryId, String area) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserSn().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // 테스트 모드 감정 분석 (랜덤 생성, area 지정 가능)
        DiaryEmotionResponse emotionResponse = emotionTestService.analyzeForTest(diary.getContent(), area);

        // 분석 결과 저장 (color, emotionNameKr 포함)
        List<Diary.EmotionPercent> emotionsJson = emotionResponse.getEmotions().stream()
                .map(e -> {
                    // 감정 코드로 색상 및 이름 조회
                    Emotion emotion = emotionCacheService.getEmotion(e.getEmotion());
                    String color = emotion != null ? emotion.getColor() : null;
                    String emotionNameKr = emotion != null ? emotion.getEmotionNameKr() : null;
                    return new Diary.EmotionPercent(e.getEmotion(), e.getPercent(), color, emotionNameKr);
                })
                .collect(Collectors.toList());

        // 테스트 모드에서는 감정 코드 검증 생략
        String emotionCode = emotionResponse.getCoreEmotion();

        diary.updateAnalysis(
                emotionResponse.getSummary(),
                emotionCode,
                emotionResponse.getReason(),
                emotionResponse.getFlower(),
                emotionResponse.getFloriography(),
                emotionsJson
        );

        log.info("Diary emotion analyzed (TEST MODE): diaryId={}, coreEmotionCode={}, area={}",
                diaryId, emotionCode, area);

        // 감정 조절 팁 체크 (오늘 날짜이고 연속 3일 이상인 경우)
        EmotionControlTipInfo tipInfo = checkEmotionControlTip(userId, diary);

        // 위험도 체크 (7일 연속 같은 영역, red/blue 체크 + LLM 키워드 탐지)
        // 테스트 모드는 LLM 분석 없으므로 null 전달
        riskAnalysisService.checkAndUpdateRiskLevel(
                userId,
                diary.getDiaryDate(),
                null,
                null,
                null
        );

        return convertToResponse(diary, tipInfo);
    }

    /**
     * 일기 상세 조회
     */
    public DiaryResponse getDiary(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserSn().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        return convertToResponse(diary);
    }

    /**
     * 특정 날짜 일기 조회
     */
    public DiaryResponse getDiaryByDate(Long userSn, LocalDate date) {

        Diary diary = diaryRepository.findByUserUserSnAndDiaryDateAndDeletedAtIsNull(userSn, date)
                .orElseThrow(DiaryNotFoundException::new);

        return convertToResponse(diary);
    }

    /**
     * 월별 일기 목록 조회
     */
    public MonthlyDiariesResponse getMonthlyDiaries(Long userSn, String yearMonth) {

        YearMonth ym = YearMonth.parse(yearMonth);
        int year = ym.getYear();
        int month = ym.getMonthValue();

        List<Diary> diaries = diaryRepository.findByUserSnAndYearMonth(userSn, year, month);

        List<MonthlyDiariesResponse.DiaryListItem> items = diaries.stream()
                .map(this::convertToListItem)
                .collect(Collectors.toList());

        return MonthlyDiariesResponse.builder()
                .yearMonth(yearMonth)
                .diaries(items)
                .totalCount(items.size())
                .build();
    }

    /**
     * 일기 수정
     */
    @Transactional
    public DiaryResponse updateDiary(Long userId, Long diaryId, DiaryUpdateRequest request) {

        // 일기 내용 유효성 검사
        validateDiaryContent(request.getContent());

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserSn().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // 내용 수정 (분석 정보는 초기화)
        diary.updateContent(request.getContent());

        log.info("Diary updated: diaryId={}", diaryId);

        return convertToResponse(diary);
    }

    /**
     * 일기 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserSn().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        diaryRepository.delete(diary); // Soft Delete (@SQLDelete 적용)

        log.info("Diary deleted: diaryId={}", diaryId);
    }

    /**
     * 일기 내용 유효성 검사
     */
    private void validateDiaryContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_DIARY_CONTENT,
                    "일기 내용이 비어있습니다");
        }

        if (content.length() < 10) {
            throw new BusinessException(ErrorCode.INVALID_DIARY_CONTENT,
                    "일기 내용이 너무 짧습니다. 최소 10자 이상 작성해주세요");
        }

        if (content.length() > 5000) {
            throw new BusinessException(ErrorCode.INVALID_DIARY_CONTENT,
                    "일기 내용이 너무 깁니다. 최대 5000자까지 가능합니다");
        }
    }

    /**
     * Entity -> Response 변환 (조회 API용 - 감정 조절 팁 정보 없음)
     */
    private DiaryResponse convertToResponse(Diary diary) {
        return convertToResponse(diary, null);
    }

    /**
     * Entity -> Response 변환 (분석 API용 - 감정 조절 팁 정보 포함 가능)
     */
    private DiaryResponse convertToResponse(Diary diary, EmotionControlTipInfo tipInfo) {
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

        // 꽃 상세정보 조회 (coreEmotionCode로 조회, 캐싱 적용)
        DiaryResponse.FlowerDetail flowerDetail = null;
        if (diary.getCoreEmotionCode() != null) {
            Emotion emotion = emotionCacheService.getEmotion(diary.getCoreEmotionCode());
            flowerDetail = emotion != null ? convertToDiaryFlowerDetail(emotion) : null;
        }

        // 감정 조절 팁 정보 설정 (분석 API에서만 제공)
        Boolean showTip = null;
        Integer consecutiveDays = null;
        String repeatedArea = null;
        String tipCode = null;

        if (tipInfo != null) {
            showTip = tipInfo.isShow();
            consecutiveDays = tipInfo.getConsecutiveDays();
            repeatedArea = tipInfo.getArea();
            tipCode = tipInfo.getTipCode();
        }

        return DiaryResponse.builder()
                .diaryId(diary.getDiaryId())
                .diaryDate(diary.getDiaryDate())
                .content(diary.getContent())
                .summary(diary.getSummary())
                .coreEmotionCode(diary.getCoreEmotionCode())
                .emotionReason(diary.getEmotionReason())
                .flowerName(diary.getFlowerName())
                .flowerMeaning(diary.getFlowerMeaning())
                .emotions(emotions)
                .isAnalyzed(diary.getIsAnalyzed())
                .analyzedAt(diary.getAnalyzedAt())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .flowerDetail(flowerDetail)
                .showEmotionControlTip(showTip)
                .consecutiveSameAreaDays(consecutiveDays)
                .repeatedEmotionArea(repeatedArea)
                .emotionControlTipCode(tipCode)
                .build();
    }

    /**
     * Emotion Entity -> DiaryResponse.FlowerDetail DTO 변환
     */
    private DiaryResponse.FlowerDetail convertToDiaryFlowerDetail(Emotion emotion) {
        return DiaryResponse.FlowerDetail.builder()
                .emotionCode(emotion.getEmotionCode())
                .emotionNameKr(emotion.getEmotionNameKr())
                .emotionNameEn(emotion.getEmotionNameEn())
                .flowerNameKr(emotion.getFlowerNameKr())
                .flowerNameEn(emotion.getFlowerNameEn())
                .flowerMeaning(emotion.getFlowerMeaning())
                .flowerMeaningStory(emotion.getFlowerMeaningStory())
                .flowerColor(emotion.getFlowerColor())
                .flowerColorCodes(emotion.getFlowerColorCodes())
                .flowerOrigin(emotion.getFlowerOrigin())
                .flowerFragrance(emotion.getFlowerFragrance())
                .flowerFunFact(emotion.getFlowerFunFact())
                .imageFile3d(emotion.getImageFile3d())
                .imageFileRealistic(emotion.getImageFileRealistic())
                .area(emotion.getArea())
                .build();
    }

    /**
     * Entity -> ListItem 변환
     */
    private MonthlyDiariesResponse.DiaryListItem convertToListItem(Diary diary) {
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

        // 꽃 상세 정보 조회 (coreEmotionCode로 조회)
        MonthlyDiariesResponse.FlowerDetail flowerDetail = null;
        if (diary.getCoreEmotionCode() != null) {
            Emotion emotion = emotionCacheService.getEmotion(diary.getCoreEmotionCode());
            flowerDetail = emotion != null ? convertToFlowerDetail(emotion) : null;
        }

        return MonthlyDiariesResponse.DiaryListItem.builder()
                .id(diary.getDiaryId())
                .date(diary.getDiaryDate())
                .content(diary.getContent())
                .isAnalyzed(diary.getIsAnalyzed())
                .coreEmotionCode(diary.getCoreEmotionCode())
                .flower(diary.getFlowerName())
                .floriography(diary.getFlowerMeaning())
                .summary(diary.getSummary())
                .emotions(emotions)
                .reason(diary.getEmotionReason())
                .flowerDetail(flowerDetail)
                .build();
    }

    /**
     * Emotion Entity -> FlowerDetail DTO 변환
     */
    private MonthlyDiariesResponse.FlowerDetail convertToFlowerDetail(Emotion emotion) {
        return MonthlyDiariesResponse.FlowerDetail.builder()
                .emotionCode(emotion.getEmotionCode())
                .emotionNameKr(emotion.getEmotionNameKr())
                .emotionNameEn(emotion.getEmotionNameEn())
                .flowerNameKr(emotion.getFlowerNameKr())
                .flowerNameEn(emotion.getFlowerNameEn())
                .flowerMeaning(emotion.getFlowerMeaning())
                .flowerMeaningStory(emotion.getFlowerMeaningStory())
                .flowerColor(emotion.getFlowerColor())
                .flowerColorCodes(emotion.getFlowerColorCodes())
                .flowerOrigin(emotion.getFlowerOrigin())
                .flowerFragrance(emotion.getFlowerFragrance())
                .flowerFunFact(emotion.getFlowerFunFact())
                .imageFile3d(emotion.getImageFile3d())
                .imageFileRealistic(emotion.getImageFileRealistic())
                .area(emotion.getArea())
                .build();
    }

    /**
     * 감정 조절 팁 표시 여부 체크
     *
     * 비즈니스 로직:
     * 1. 현재 일기의 감정 영역 조회
     * 2. 최근 7일간의 분석된 일기 조회 (현재 일기 기준 이전)
     * 3. 연속으로 같은 감정 영역(area)이 나온 일수 체크
     * 4. 3일 이상 또는 5일 이상이면 감정 조절 팁 표시
     *
     * 주의:
     * - 날짜가 연속이어야 함 (하루라도 끊기면 연속 중단)
     * - area가 같아야 함 (red, yellow, blue, green)
     * - 과거 일기를 나중에 작성해도 패턴 인식하여 팁 제공
     *
     * 사용자 경험 개선:
     * - 오늘 날짜 체크 제거: 언제 일기를 쓰든 연속 패턴 기준으로 팁 제공
     * - 며칠치 일기를 모아서 써도 패턴 인식 가능
     * - 과거 일기 작성 시에도 "그때 이런 패턴이었구나" 인식 가능
     *
     * @param userSn 사용자 일련번호
     * @param currentDiary 현재 분석한 일기
     * @return EmotionControlTipInfo (표시 여부, 연속 일수, 영역)
     */
    private EmotionControlTipInfo checkEmotionControlTip(Long userSn, Diary currentDiary) {
        // 1. 현재 일기의 감정 영역 조회
        String currentArea = getEmotionArea(currentDiary.getCoreEmotionCode());
        if (currentArea == null) {
            return new EmotionControlTipInfo(false, null, null, null);
        }

        // 2. 최근 7일간의 분석된 일기 조회 (현재 일기 포함, 날짜 역순)
        List<Diary> recentDiaries = diaryRepository.findRecentAnalyzedDiaries(
                userSn,
                currentDiary.getDiaryDate(),
                PageRequest.of(0, 7)
        );

        // 3. 연속된 같은 영역 일수 체크
        int consecutiveDays = 1;  // 현재 일기 포함

        for (int i = 1; i < recentDiaries.size(); i++) {
            Diary prevDiary = recentDiaries.get(i - 1);
            Diary currDiary = recentDiaries.get(i);

            // 날짜가 연속인지 확인 (하루 차이)
            LocalDate expectedDate = prevDiary.getDiaryDate().minusDays(1);
            if (!expectedDate.equals(currDiary.getDiaryDate())) {
                break;  // 연속 끊김
            }

            // area가 같은지 확인
            String area = getEmotionArea(currDiary.getCoreEmotionCode());
            if (area == null || !area.equals(currentArea)) {
                break;  // area 다름
            }

            consecutiveDays++;
        }

        // 4. 3일 이상이면 감정 조절 팁 표시
        if (consecutiveDays >= 3) {
            // 실제 연속 일수를 그대로 반환 (3일, 4일, 5일, 6일... 등)
            // 팁 코드 생성: 3~4일은 _3, 5일 이상은 _5
            int tipLevel = consecutiveDays >= 5 ? 5 : 3;
            String tipCode = currentArea.toUpperCase() + "_" + tipLevel;
            return new EmotionControlTipInfo(true, consecutiveDays, currentArea, tipCode);
        }

        return new EmotionControlTipInfo(false, null, null, null);
    }

    /**
     * 감정 코드로부터 영역(area) 조회
     *
     * @param emotionCode 감정 코드
     * @return area (red, yellow, blue, green) 또는 null
     */
    private String getEmotionArea(String emotionCode) {
        if (emotionCode == null) {
            return null;
        }

        Emotion emotion = emotionCacheService.getEmotion(emotionCode);
        return emotion != null ? emotion.getArea() : null;
    }

    /**
     * 감정 조절 팁 정보를 담는 내부 클래스
     */
    @Getter
    @AllArgsConstructor
    private static class EmotionControlTipInfo {
        private boolean show;  // 팁 표시 여부
        private Integer consecutiveDays;  // 연속 일수 (3, 4, 5, 6... 등 실제 연속 일수)
        private String area;  // 감정 영역 (red, yellow, blue, green)
        private String tipCode;  // 감정 조절 팁 코드 (RED_3, YELLOW_5 등)
    }
}
