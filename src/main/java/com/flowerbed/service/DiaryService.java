package com.flowerbed.service;

import com.flowerbed.domain.Diary;
import com.flowerbed.domain.User;
import com.flowerbed.dto.*;
import com.flowerbed.exception.BusinessException;
import com.flowerbed.exception.DiaryNotFoundException;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.repository.DiaryRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;
    private final DiaryEmotionService emotionService;
    private final DiaryEmotionTestService emotionTestService;

    /**
     * 일기 작성
     */
    @Transactional
    public DiaryResponse createDiary(Long userId, DiaryCreateRequest request) {

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 같은 날짜에 일기가 이미 있는지 확인
        diaryRepository.findByUserUserIdAndDiaryDate(userId, request.getDiaryDate())
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
     * 일기 감정 분석
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

        diary.updateAnalysis(
                emotionResponse.getSummary(),
                emotionResponse.getCoreEmotion(),
                emotionResponse.getReason(),
                emotionResponse.getFlower(),
                emotionResponse.getFloriography(),
                emotionsJson
        );

        log.info("Diary emotion analyzed: diaryId={}, coreEmotion={}",
                diaryId, emotionResponse.getCoreEmotion());

        return convertToResponse(diary);
    }

    /**
     * 일기 감정 분석 (테스트 모드 - Claude API 호출 없음)
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

        diary.updateAnalysis(
                emotionResponse.getSummary(),
                emotionResponse.getCoreEmotion(),
                emotionResponse.getReason(),
                emotionResponse.getFlower(),
                emotionResponse.getFloriography(),
                emotionsJson
        );

        log.info("Diary emotion analyzed (TEST MODE): diaryId={}, coreEmotion={}",
                diaryId, emotionResponse.getCoreEmotion());

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

        Diary diary = diaryRepository.findByUserUserIdAndDiaryDate(userId, date)
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

        // 이전/다음 달 존재 여부 확인
        YearMonth prevMonth = ym.minusMonths(1);
        YearMonth nextMonth = ym.plusMonths(1);

        boolean hasPrev = diaryRepository.existsByUserIdAndYearMonth(
                userId, prevMonth.getYear(), prevMonth.getMonthValue());
        boolean hasNext = diaryRepository.existsByUserIdAndYearMonth(
                userId, nextMonth.getYear(), nextMonth.getMonthValue());

        return MonthlyDiariesResponse.builder()
                .yearMonth(yearMonth)
                .diaries(items)
                .totalCount(items.size())
                .hasPrevMonth(hasPrev)
                .hasNextMonth(hasNext)
                .build();
    }

    /**
     * 일기 수정
     */
    @Transactional
    public DiaryResponse updateDiary(Long userId, Long diaryId, DiaryUpdateRequest request) {

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
     * Entity -> Response 변환
     */
    private DiaryResponse convertToResponse(Diary diary) {
        List<EmotionPercent> emotions = null;
        if (diary.getEmotionsJson() != null) {
            emotions = diary.getEmotionsJson().stream()
                    .map(e -> new EmotionPercent(e.getEmotion(), e.getPercent()))
                    .collect(Collectors.toList());
        }

        return DiaryResponse.builder()
                .diaryId(diary.getDiaryId())
                .diaryDate(diary.getDiaryDate())
                .content(diary.getContent())
                .summary(diary.getSummary())
                .coreEmotion(diary.getCoreEmotion())
                .emotionReason(diary.getEmotionReason())
                .flowerName(diary.getFlowerName())
                .flowerMeaning(diary.getFlowerMeaning())
                .emotions(emotions)
                .isAnalyzed(diary.getIsAnalyzed())
                .analyzedAt(diary.getAnalyzedAt())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> ListItem 변환
     */
    private MonthlyDiariesResponse.DiaryListItem convertToListItem(Diary diary) {
        return MonthlyDiariesResponse.DiaryListItem.builder()
                .id(diary.getDiaryId())
                .date(diary.getDiaryDate())
                .content(diary.getContent())
                .coreEmotion(diary.getCoreEmotion())
                .flower(diary.getFlowerName())
                .floriography(diary.getFlowerMeaning())
                .summary(diary.getSummary())
                .build();
    }
}
