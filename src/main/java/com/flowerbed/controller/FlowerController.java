package com.flowerbed.controller;

import com.flowerbed.dto.FlowerResponse;
import com.flowerbed.dto.UserEmotionFlowerResponse;
import com.flowerbed.service.FlowerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Flower", description = "꽃 정보 API")
@RestController
@RequestMapping("/flowers")
@RequiredArgsConstructor
public class FlowerController {

    private final FlowerService flowerService;

    @Operation(summary = "꽃 이름으로 상세 정보 조회", description = "꽃 이름(한글)으로 꽃의 상세 정보를 조회합니다")
    @GetMapping("/{flowerName}")
    public ResponseEntity<FlowerResponse> getFlowerByName(
            @PathVariable String flowerName
    ) {
        FlowerResponse response = flowerService.getFlowerByName(flowerName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "일기 번호로 꽃 상세 정보 조회", description = "일기 ID로 해당 일기에 매칭된 꽃의 상세 정보를 조회합니다")
    @GetMapping("/diary/{diaryId}")
    public ResponseEntity<FlowerResponse> getFlowerByDiaryId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long diaryId
    ) {
        FlowerResponse response = flowerService.getFlowerByDiaryId(userId, diaryId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "사용자의 감정&꽃 리스트 조회", description = "사용자가 작성한 일기에서 나타난 감정과 꽃 리스트를 조회합니다")
    @GetMapping("/my-emotions")
    public ResponseEntity<UserEmotionFlowerResponse> getUserEmotionFlowers(
            @RequestHeader("X-User-Id") Long userId
    ) {
        UserEmotionFlowerResponse response = flowerService.getUserEmotionFlowers(userId);
        return ResponseEntity.ok(response);
    }
}
