package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Emotion;
import com.flowerbed.api.v1.dto.DiaryEmotionResponse;
import com.flowerbed.api.v1.dto.EmotionPercent;
import com.flowerbed.api.v1.repository.FlowerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 테스트용 감정 분석 서비스 (API 비용 없음)
 * - DB의 emotions 테이블에서 랜덤 선택
 * - 실제 API와 동일한 응답 구조 반환
 * - 개발/테스트 단계에서 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryEmotionTestService {

    private final FlowerRepository flowerRepository;
    private final Random random = new Random();

    // DB에서 조회한 전체 감정 코드 리스트 (초기화 시 캐싱)
    private List<String> allEmotionCodes;

    // area별 감정 코드 리스트 (초기화 시 캐싱)
    private Map<String, List<String>> emotionCodesByArea;

    /**
     * 서비스 초기화 시 DB에서 전체 감정 코드 조회하여 캐싱
     */
    @PostConstruct
    public void init() {
        List<Emotion> allEmotions = flowerRepository.findAll();

        // 전체 감정 코드 리스트
        allEmotionCodes = allEmotions.stream()
                .map(Emotion::getEmotionCode)
                .collect(Collectors.toList());

        // area별 감정 코드 맵
        emotionCodesByArea = allEmotions.stream()
                .filter(e -> e.getArea() != null)
                .collect(Collectors.groupingBy(
                        Emotion::getArea,
                        Collectors.mapping(Emotion::getEmotionCode, Collectors.toList())
                ));

        log.info("Loaded {} emotion codes from database for test service", allEmotionCodes.size());
        log.info("Emotion codes by area: {}", emotionCodesByArea.keySet());
    }

    /**
     * 테스트용 감정 분석 (Claude API 호출 없이 랜덤 생성)
     *
     * @param diaryContent 일기 내용
     * @param area 감정 영역 (red/yellow/blue/green, null이면 전체)
     * @return 랜덤 생성된 감정 분석 결과
     */
    public DiaryEmotionResponse analyzeForTest(String diaryContent, String area) {

        log.info("Test mode: Generating random emotion analysis (area={})", area);

        // 1. Summary: 일기 내용 앞 10글자
        String summary = diaryContent.length() > 10
                ? diaryContent.substring(0, 10) + "..."
                : diaryContent;

        // 2. 랜덤하게 3개 감정 선택 (area 지정 시 해당 영역만)
        List<String> selectedEmotions = getRandomEmotions(3, area);

        // 3. 랜덤 퍼센트 생성 (합계 100)
        List<Integer> percentages = generateRandomPercentages(3);

        // 4. Emotions 리스트 생성 (퍼센트 내림차순 정렬)
        List<EmotionPercent> emotions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            emotions.add(new EmotionPercent(selectedEmotions.get(i), percentages.get(i)));
        }
        emotions.sort((a, b) -> b.getPercent().compareTo(a.getPercent()));

        // 5. CoreEmotion: 가장 높은 퍼센트의 감정
        String coreEmotionCode = emotions.get(0).getEmotion();

        // 6. DB에서 감정 정보 조회
        Emotion emotion = flowerRepository.findById(coreEmotionCode)
                .orElseThrow(() -> new IllegalStateException("감정 코드를 찾을 수 없습니다: " + coreEmotionCode));

        // 7. Response 생성 (coreEmotion은 영어 코드로 반환)
        DiaryEmotionResponse response = new DiaryEmotionResponse();
        response.setSummary(summary);
        response.setEmotions(emotions);
        response.setCoreEmotion(emotion.getEmotionCode());
        response.setReason(area != null
                ? "테스트 모드: " + area + " 영역에서 랜덤으로 생성된 감정 분석 결과입니다"
                : "테스트 모드: 랜덤으로 생성된 감정 분석 결과입니다");
        response.setFlower(emotion.getFlowerNameKr());
        response.setFloriography(emotion.getFlowerMeaning());

        return response;
    }

    /**
     * 랜덤하게 N개의 감정 선택
     *
     * @param count 선택할 감정 개수
     * @param area 감정 영역 (null이면 전체에서 선택)
     * @return 랜덤 선택된 감정 코드 리스트
     */
    private List<String> getRandomEmotions(int count, String area) {
        List<String> targetList;

        if (area != null && emotionCodesByArea.containsKey(area)) {
            // area가 지정되면 해당 영역의 감정만 선택
            targetList = emotionCodesByArea.get(area);
            log.debug("Selecting {} emotions from area: {} (total {} emotions)",
                    count, area, targetList.size());
        } else {
            // area가 없거나 유효하지 않으면 전체에서 선택
            targetList = allEmotionCodes;
            if (area != null) {
                log.warn("Invalid area: {}, falling back to all emotions", area);
            }
        }

        // 랜덤 선택
        List<String> shuffled = new ArrayList<>(targetList);
        Collections.shuffle(shuffled, random);
        return shuffled.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 합계 100이 되는 랜덤 퍼센트 생성
     */
    private List<Integer> generateRandomPercentages(int count) {
        List<Integer> percentages = new ArrayList<>();
        int remaining = 100;
        int minPercent = 10; // 각 감정의 최소 퍼센트

        for (int i = 0; i < count - 1; i++) {
            // 남은 항목들에게 최소 퍼센트를 보장해야 함
            int remainingItems = count - i - 1;
            int maxForThis = remaining - (remainingItems * minPercent);

            // maxForThis가 minPercent보다 작으면 minPercent로 제한
            if (maxForThis < minPercent) {
                maxForThis = minPercent;
            }

            int percent = random.nextInt(maxForThis - minPercent + 1) + minPercent;
            percentages.add(percent);
            remaining -= percent;
        }

        // 마지막은 남은 값 (음수가 될 수 없도록 최소값 보장)
        percentages.add(Math.max(remaining, minPercent));

        return percentages;
    }
}
