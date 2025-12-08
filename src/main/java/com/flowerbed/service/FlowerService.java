package com.flowerbed.service;

import com.flowerbed.domain.Diary;
import com.flowerbed.domain.Flower;
import com.flowerbed.dto.FlowerResponse;
import com.flowerbed.dto.UserEmotionFlowerResponse;
import com.flowerbed.exception.BusinessException;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.repository.DiaryRepository;
import com.flowerbed.repository.FlowerRepository;
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

    private final FlowerRepository flowerRepository;
    private final DiaryRepository diaryRepository;

    /**
     * 꽃 이름으로 상세 정보 조회
     */
    public FlowerResponse getFlowerByName(String flowerName) {
        Flower flower = flowerRepository.findByFlowerNameKr(flowerName)
                .orElseThrow(() -> new BusinessException(ErrorCode.FLOWER_NOT_FOUND,
                        "꽃을 찾을 수 없습니다: " + flowerName));

        return convertToResponse(flower);
    }

    /**
     * 일기 ID로 해당 일기의 꽃 상세 정보 조회
     */
    public FlowerResponse getFlowerByDiaryId(Long userId, Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));

        // 권한 확인
        if (!diary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
        }

        // 분석되지 않은 일기인 경우
        if (!diary.getIsAnalyzed() || diary.getFlowerName() == null) {
            throw new BusinessException(ErrorCode.DIARY_NOT_ANALYZED,
                    "아직 감정 분석이 완료되지 않은 일기입니다");
        }

        Flower flower = flowerRepository.findByFlowerNameKr(diary.getFlowerName())
                .orElseThrow(() -> new BusinessException(ErrorCode.FLOWER_NOT_FOUND,
                        "꽃 정보를 찾을 수 없습니다: " + diary.getFlowerName()));

        return convertToResponse(flower);
    }

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
     * Flower Entity -> FlowerResponse 변환
     */
    private FlowerResponse convertToResponse(Flower flower) {
        return FlowerResponse.builder()
                .flowerId(flower.getFlowerId())
                .emotion(flower.getEmotion())
                .flowerNameKr(flower.getFlowerNameKr())
                .flowerNameEn(flower.getFlowerNameEn())
                .flowerMeaning(flower.getFlowerMeaning())
                .flowerColor(flower.getFlowerColor())
                .flowerColorCodes(flower.getFlowerColorCodes())
                .flowerOrigin(flower.getFlowerOrigin())
                .flowerBloomingSeason(flower.getFlowerBloomingSeason())
                .flowerFragrance(flower.getFlowerFragrance())
                .flowerMeaningOrigin(flower.getFlowerMeaningOrigin())
                .flowerFunFact(flower.getFlowerFunFact())
                .imageFile3d(flower.getImageFile3d())
                .imageFileRealistic(flower.getImageFileRealistic())
                .isPositive(flower.getIsPositive())
                .displayOrder(flower.getDisplayOrder())
                .createdAt(flower.getCreatedAt())
                .updatedAt(flower.getUpdatedAt())
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
