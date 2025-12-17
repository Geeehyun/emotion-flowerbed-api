package com.flowerbed.service;

import com.flowerbed.domain.Diary;
import com.flowerbed.domain.Emotion;
import com.flowerbed.dto.AllEmotionsResponse;
import com.flowerbed.dto.UserEmotionFlowerResponse;
import com.flowerbed.repository.DiaryRepository;
import com.flowerbed.repository.FlowerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 꽃/감정 정보 조회 서비스
 * - 사용자별 감정 통계 (일기 기반)
 * - 전체 감정-꽃 매핑 정보 (마스터 데이터)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlowerService {

    private final DiaryRepository diaryRepository;
    private final FlowerRepository flowerRepository;

    /**
     * 사용자의 감정 통계 조회
     * - 분석 완료된 일기들을 감정별로 그룹화
     * - 감정별 횟수, 날짜 목록, 꽃 상세정보 반환
     */
    public UserEmotionFlowerResponse getUserEmotionFlowers(Long userId) {
        // 사용자의 분석된 일기 중 핵심 감정별로 그룹화
        List<Diary> analyzedDiaries = diaryRepository.findByUserUserIdAndIsAnalyzed(userId, true);

        if (analyzedDiaries.isEmpty()) {
            return UserEmotionFlowerResponse.builder()
                    .items(List.of())
                    .totalCount(0)
                    .build();
        }

        // 감정별로 데이터 집계 (순서 유지를 위해 LinkedHashMap 사용)
        Map<String, EmotionFlowerData> emotionMap = new LinkedHashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Diary diary : analyzedDiaries) {
            String emotionCode = diary.getCoreEmotionCode();
            if (emotionCode != null) {
                emotionMap.computeIfAbsent(emotionCode, k -> new EmotionFlowerData(
                        emotionCode,
                        diary.getFlowerName(),
                        diary.getFlowerMeaning()
                )).addDate(diary.getDiaryDate().format(dateFormatter));
            }
        }

        // EmotionFlowerItem으로 변환
        List<UserEmotionFlowerResponse.EmotionFlowerItem> items = emotionMap.values().stream()
                .map(data -> {
                    // 꽃 상세정보 조회 (emotionCode로 조회)
                    UserEmotionFlowerResponse.FlowerDetail flowerDetail = null;
                    if (data.emotionCode != null) {
                        flowerDetail = flowerRepository.findById(data.emotionCode)
                                .map(this::convertToFlowerDetail)
                                .orElse(null);
                    }

                    return UserEmotionFlowerResponse.EmotionFlowerItem.builder()
                            .emotionCode(data.emotionCode)
                            .flowerName(data.flowerName)
                            .flowerMeaning(data.flowerMeaning)
                            .count(data.count)
                            .dates(data.dates)
                            .flowerDetail(flowerDetail)
                            .build();
                })
                .collect(Collectors.toList());

        return UserEmotionFlowerResponse.builder()
                .items(items)
                .totalCount(items.size())
                .build();
    }

    /**
     * Emotion Entity -> FlowerDetail DTO 변환
     */
    private UserEmotionFlowerResponse.FlowerDetail convertToFlowerDetail(Emotion emotion) {
        return UserEmotionFlowerResponse.FlowerDetail.builder()
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
     * 전체 감정-꽃 매핑 정보 조회
     * - emotions 테이블 전체 조회
     * - display_order 순으로 정렬
     */
    public AllEmotionsResponse getAllEmotions() {
        List<Emotion> emotions = flowerRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"));

        List<AllEmotionsResponse.EmotionItem> items = emotions.stream()
                .map(emotion -> AllEmotionsResponse.EmotionItem.builder()
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
                        .displayOrder(emotion.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return AllEmotionsResponse.builder()
                .emotions(items)
                .totalCount(items.size())
                .build();
    }

    /**
     * 감정별 꽃 데이터 집계용 내부 클래스
     */
    private static class EmotionFlowerData {
        String emotionCode;
        String flowerName;
        String flowerMeaning;
        int count = 0;
        List<String> dates = new ArrayList<>();

        EmotionFlowerData(String emotionCode, String flowerName, String flowerMeaning) {
            this.emotionCode = emotionCode;
            this.flowerName = flowerName;
            this.flowerMeaning = flowerMeaning;
        }

        void addDate(String date) {
            this.dates.add(date);
            this.count++;
        }
    }
}
