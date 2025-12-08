package com.flowerbed.service;

import com.flowerbed.domain.Diary;
import com.flowerbed.dto.UserEmotionFlowerResponse;
import com.flowerbed.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlowerService {

    private final DiaryRepository diaryRepository;

    /**
     * 사용자의 감정&꽃 리스트 조회
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

        // 감정별로 카운트 (순서 유지를 위해 LinkedHashMap 사용)
        Map<String, EmotionFlowerData> emotionMap = new LinkedHashMap<>();

        for (Diary diary : analyzedDiaries) {
            String emotion = diary.getCoreEmotion();
            if (emotion != null) {
                emotionMap.computeIfAbsent(emotion, k -> new EmotionFlowerData(
                        emotion,
                        diary.getFlowerName(),
                        diary.getFlowerMeaning()
                )).incrementCount();
            }
        }

        List<UserEmotionFlowerResponse.EmotionFlowerItem> items = emotionMap.values().stream()
                .map(data -> UserEmotionFlowerResponse.EmotionFlowerItem.builder()
                        .emotion(data.emotion)
                        .flowerName(data.flowerName)
                        .flowerMeaning(data.flowerMeaning)
                        .count(data.count)
                        .build())
                .collect(Collectors.toList());

        return UserEmotionFlowerResponse.builder()
                .items(items)
                .totalCount(items.size())
                .build();
    }

    /**
     * 감정별 꽃 데이터 집계용 내부 클래스
     */
    private static class EmotionFlowerData {
        String emotion;
        String flowerName;
        String flowerMeaning;
        int count = 0;

        EmotionFlowerData(String emotion, String flowerName, String flowerMeaning) {
            this.emotion = emotion;
            this.flowerName = flowerName;
            this.flowerMeaning = flowerMeaning;
        }

        void incrementCount() {
            this.count++;
        }
    }
}
