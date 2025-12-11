package com.flowerbed.controller;

import com.flowerbed.dto.AllEmotionsResponse;
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

    // TODO: 회원가입/로그인 기능 추가 후 실제 userId 사용하도록 수정 필요
    private static final Long DEFAULT_USER_ID = 1L;

    @Operation(summary = "사용자의 감정&꽃 리스트 조회", description = "사용자가 작성한 일기에서 나타난 감정과 꽃 리스트를 조회합니다")
    @GetMapping("/my-emotions")
    public ResponseEntity<UserEmotionFlowerResponse> getUserEmotionFlowers() {
        // TODO: 회원가입/로그인 기능 추가 후 실제 userId를 파라미터로 받도록 수정
        UserEmotionFlowerResponse response = flowerService.getUserEmotionFlowers(DEFAULT_USER_ID);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전체 감정-꽃 정보 조회", description = "시스템에 등록된 모든 감정-꽃 매핑 정보를 조회합니다")
    @GetMapping("/all-emotions")
    public ResponseEntity<AllEmotionsResponse> getAllEmotions() {
        AllEmotionsResponse response = flowerService.getAllEmotions();
        return ResponseEntity.ok(response);
    }
}
