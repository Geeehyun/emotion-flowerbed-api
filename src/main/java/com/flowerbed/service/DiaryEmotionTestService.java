package com.flowerbed.service;

import com.flowerbed.dto.DiaryEmotionResponse;
import com.flowerbed.dto.EmotionPercent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryEmotionTestService {

    private static final Map<String, FlowerInfo> EMOTION_FLOWER_MAP = Map.ofEntries(
            Map.entry("JOY", new FlowerInfo("해바라기", "당신을 보면 행복해요")),
            Map.entry("HAPPINESS", new FlowerInfo("코스모스", "평화로운 사랑")),
            Map.entry("GRATITUDE", new FlowerInfo("핑크 장미", "감사, 존경")),
            Map.entry("EXCITEMENT", new FlowerInfo("프리지아", "순수한 마음")),
            Map.entry("PEACE", new FlowerInfo("은방울꽃", "행복의 재림")),
            Map.entry("ACHIEVEMENT", new FlowerInfo("노란 튤립", "성공, 명성")),
            Map.entry("LOVE", new FlowerInfo("빨간 장미", "사랑, 애정")),
            Map.entry("HOPE", new FlowerInfo("데이지", "희망, 순수")),
            Map.entry("VITALITY", new FlowerInfo("거베라", "희망, 도전")),
            Map.entry("FUN", new FlowerInfo("스위트피", "즐거운 추억")),
            Map.entry("SADNESS", new FlowerInfo("파란 수국", "진심, 이해")),
            Map.entry("LONELINESS", new FlowerInfo("물망초", "나를 잊지 말아요")),
            Map.entry("ANXIETY", new FlowerInfo("라벤더", "침묵, 의심")),
            Map.entry("ANGER", new FlowerInfo("노란 카네이션", "경멸, 거절")),
            Map.entry("FATIGUE", new FlowerInfo("민트", "휴식, 상쾌함")),
            Map.entry("REGRET", new FlowerInfo("보라색 팬지", "생각, 추억")),
            Map.entry("LETHARGY", new FlowerInfo("백합", "순수, 재생")),
            Map.entry("CONFUSION", new FlowerInfo("아네모네", "기대, 진실")),
            Map.entry("DISAPPOINTMENT", new FlowerInfo("노란 수선화", "불확실한 사랑")),
            Map.entry("BOREDOM", new FlowerInfo("흰 카모마일", "역경 속의 평온"))
    );

    private static final List<String> ALL_EMOTIONS = new ArrayList<>(EMOTION_FLOWER_MAP.keySet());
    private final Random random = new Random();

    /**
     * 테스트용 감정 분석 (Claude API 호출 없이 랜덤 생성)
     */
    public DiaryEmotionResponse analyzeForTest(String diaryContent) {

        log.info("Test mode: Generating random emotion analysis");

        // 1. Summary: 일기 내용 앞 10글자
        String summary = diaryContent.length() > 10
                ? diaryContent.substring(0, 10) + "..."
                : diaryContent;

        // 2. 랜덤하게 3개 감정 선택
        List<String> selectedEmotions = getRandomEmotions(3);

        // 3. 랜덤 퍼센트 생성 (합계 100)
        List<Integer> percentages = generateRandomPercentages(3);

        // 4. Emotions 리스트 생성 (퍼센트 내림차순 정렬)
        List<EmotionPercent> emotions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            emotions.add(new EmotionPercent(selectedEmotions.get(i), percentages.get(i)));
        }
        emotions.sort((a, b) -> b.getPercent().compareTo(a.getPercent()));

        // 5. CoreEmotion: 가장 높은 퍼센트의 감정
        String coreEmotion = emotions.get(0).getEmotion();

        // 6. Flower & Floriography
        FlowerInfo flowerInfo = EMOTION_FLOWER_MAP.get(coreEmotion);

        // 7. Response 생성
        DiaryEmotionResponse response = new DiaryEmotionResponse();
        response.setSummary(summary);
        response.setEmotions(emotions);
        response.setCoreEmotion(coreEmotion);
        response.setReason("테스트 모드: 랜덤으로 생성된 감정 분석 결과입니다");
        response.setFlower(flowerInfo.flower);
        response.setFloriography(flowerInfo.floriography);

        return response;
    }

    /**
     * 랜덤하게 N개의 감정 선택
     */
    private List<String> getRandomEmotions(int count) {
        List<String> shuffled = new ArrayList<>(ALL_EMOTIONS);
        Collections.shuffle(shuffled, random);
        return shuffled.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * 합계 100이 되는 랜덤 퍼센트 생성
     */
    private List<Integer> generateRandomPercentages(int count) {
        List<Integer> percentages = new ArrayList<>();
        int remaining = 100;

        for (int i = 0; i < count - 1; i++) {
            int maxForThis = remaining - (count - i - 1); // 남은 항목에 최소 1%씩 보장
            int percent = random.nextInt(maxForThis - 10) + 10; // 최소 10%
            percentages.add(percent);
            remaining -= percent;
        }

        percentages.add(remaining); // 마지막은 남은 값

        return percentages;
    }

    private record FlowerInfo(String flower, String floriography) {}
}
