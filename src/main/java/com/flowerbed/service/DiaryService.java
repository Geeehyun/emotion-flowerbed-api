package com.flowerbed.service;

import com.flowerbed.domain.Diary;
import com.flowerbed.domain.Emotion;
import com.flowerbed.domain.User;
import com.flowerbed.dto.*;
import com.flowerbed.exception.BusinessException;
import com.flowerbed.exception.DiaryNotFoundException;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.repository.DiaryRepository;
import com.flowerbed.repository.FlowerRepository;
import com.flowerbed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
    private final FlowerRepository flowerRepository;
    private final DiaryEmotionService emotionService;  // 실제 Claude API 분석
    private final DiaryEmotionTestService emotionTestService;  // 테스트용 랜덤 분석

    /**
     * 일기 작성 (감정 분석 X, 내용만 저장)
     */
    @Transactional
    public DiaryResponse createDiary(Long userId, DiaryCreateRequest request) {

        // 일기 내용 유효성 검사
        validateDiaryContent(request.getContent());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 같은 날짜에 일기가 이미 있는지 확인
        diaryRepository.findByUserUserIdAndDiaryDateAndDeletedAtIsNull(userId, request.getDiaryDate())
                .ifPresent(diary -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_DIARY_DATE,
                            "해당 날짜에 이미 일기가 존재합니다");
                });

        // 일기 생성
        Diary diary = new Diary(user, request.getDiaryDate(), request.getContent());

        try {
            Diary savedDiary = diaryRepository.save(diary);
            log.info("Diary created: diaryId={}, userId={}, date={}",
                    savedDiary.getDiaryId(), userId, request.getDiaryDate());

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
        if (!diary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // 감정 분석 수행
        DiaryEmotionResponse emotionResponse = emotionService.analyzeDiary(diary.getContent());

        // 분석 결과 저장
        List<Diary.EmotionPercent> emotionsJson = emotionResponse.getEmotions().stream()
                .map(e -> new Diary.EmotionPercent(e.getEmotion(), e.getPercent()))
                .collect(Collectors.toList());

        // 감정 코드 검증 (DB에 존재하는 감정 코드인지 확인)
        String emotionCode = emotionResponse.getCoreEmotion();
        boolean emotionExists = flowerRepository.findById(emotionCode).isPresent();

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

        return convertToResponse(diary);
    }

    /**
     * 일기 감정 분석 (테스트 모드 - API 비용 없음)
     * - emotionTestService.analyzeForTest() 호출하여 랜덤 생성
     * - DB의 emotions 테이블에서 랜덤 선택
     */
    @Transactional
    public DiaryResponse analyzeDiaryEmotionTest(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // 테스트 모드 감정 분석 (랜덤 생성)
        DiaryEmotionResponse emotionResponse = emotionTestService.analyzeForTest(diary.getContent());

        // 분석 결과 저장
        List<Diary.EmotionPercent> emotionsJson = emotionResponse.getEmotions().stream()
                .map(e -> new Diary.EmotionPercent(e.getEmotion(), e.getPercent()))
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

        log.info("Diary emotion analyzed (TEST MODE): diaryId={}, coreEmotionCode={}",
                diaryId, emotionCode);

        return convertToResponse(diary);
    }

    /**
     * 일기 상세 조회
     */
    public DiaryResponse getDiary(Long userId, Long diaryId) {

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(DiaryNotFoundException::new);

        // 권한 확인
        if (!diary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        return convertToResponse(diary);
    }

    /**
     * 특정 날짜 일기 조회
     */
    public DiaryResponse getDiaryByDate(Long userId, LocalDate date) {

        Diary diary = diaryRepository.findByUserUserIdAndDiaryDateAndDeletedAtIsNull(userId, date)
                .orElseThrow(DiaryNotFoundException::new);

        return convertToResponse(diary);
    }

    /**
     * 월별 일기 목록 조회
     */
    public MonthlyDiariesResponse getMonthlyDiaries(Long userId, String yearMonth) {

        YearMonth ym = YearMonth.parse(yearMonth);
        int year = ym.getYear();
        int month = ym.getMonthValue();

        List<Diary> diaries = diaryRepository.findByUserIdAndYearMonth(userId, year, month);

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
        if (!diary.getUser().getUserId().equals(userId)) {
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
        if (!diary.getUser().getUserId().equals(userId)) {
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
     * Entity -> Response 변환
     */
    private DiaryResponse convertToResponse(Diary diary) {
        List<EmotionPercent> emotions = null;
        if (diary.getEmotionsJson() != null) {
            emotions = diary.getEmotionsJson().stream()
                    .map(e -> new EmotionPercent(e.getEmotion(), e.getPercent()))
                    .collect(Collectors.toList());
        }

        // 꽃 상세정보 조회 (coreEmotionCode로 조회)
        DiaryResponse.FlowerDetail flowerDetail = null;
        if (diary.getCoreEmotionCode() != null) {
            flowerDetail = flowerRepository.findById(diary.getCoreEmotionCode())
                    .map(this::convertToDiaryFlowerDetail)
                    .orElse(null);
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
                    .map(e -> new EmotionPercent(e.getEmotion(), e.getPercent()))
                    .collect(Collectors.toList());
        }

        // 꽃 상세 정보 조회 (coreEmotionCode로 조회)
        MonthlyDiariesResponse.FlowerDetail flowerDetail = null;
        if (diary.getCoreEmotionCode() != null) {
            flowerDetail = flowerRepository.findById(diary.getCoreEmotionCode())
                    .map(this::convertToFlowerDetail)
                    .orElse(null);
        }

        return MonthlyDiariesResponse.DiaryListItem.builder()
                .id(diary.getDiaryId())
                .date(diary.getDiaryDate())
                .content(diary.getContent())
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
}
