package com.flowerbed.controller;

import com.flowerbed.dto.*;
import com.flowerbed.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/diaries")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 API")
public class DiaryController {

    private final DiaryService diaryService;

    // TODO: 회원가입/로그인 기능 추가 후 실제 인증된 사용자 ID 사용하도록 수정 필요 (Spring Security + JWT)
    private static final Long DEFAULT_USER_ID = 1L;

    @PostMapping
    @Operation(summary = "일기 작성", description = "새로운 일기를 작성합니다")
    public ResponseEntity<DiaryResponse> createDiary(
            @Valid @RequestBody DiaryCreateRequest request) {

        DiaryResponse response = diaryService.createDiary(DEFAULT_USER_ID, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{diaryId}/analyze")
    @Operation(summary = "일기 감정 분석", description = "작성된 일기의 감정을 AI로 분석합니다")
    public ResponseEntity<DiaryResponse> analyzeDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        DiaryResponse response = diaryService.analyzeDiaryEmotion(DEFAULT_USER_ID, diaryId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{diaryId}/analyze-test")
    @Operation(summary = "일기 감정 분석 (테스트)",
               description = "Claude API 호출 없이 랜덤으로 감정 분석 결과를 생성합니다 (테스트용)")
    public ResponseEntity<DiaryResponse> analyzeDiaryTest(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        DiaryResponse response = diaryService.analyzeDiaryEmotionTest(DEFAULT_USER_ID, diaryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{diaryId}")
    @Operation(summary = "일기 상세 조회", description = "일기의 상세 정보를 조회합니다")
    public ResponseEntity<DiaryResponse> getDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        DiaryResponse response = diaryService.getDiary(DEFAULT_USER_ID, diaryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "특정 날짜 일기 조회", description = "특정 날짜의 일기를 조회합니다")
    public ResponseEntity<DiaryResponse> getDiaryByDate(
            @Parameter(description = "날짜 (YYYY-MM-DD)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        DiaryResponse response = diaryService.getDiaryByDate(DEFAULT_USER_ID, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "월별 일기 목록 조회", description = "특정 월의 일기 목록을 조회합니다")
    public ResponseEntity<MonthlyDiariesResponse> getMonthlyDiaries(
            @Parameter(description = "년월 (YYYY-MM)", example = "2025-12")
            @RequestParam String yearMonth) {

        MonthlyDiariesResponse response = diaryService.getMonthlyDiaries(DEFAULT_USER_ID, yearMonth);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{diaryId}")
    @Operation(summary = "일기 수정", description = "일기 내용을 수정합니다")
    public ResponseEntity<DiaryResponse> updateDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request) {

        DiaryResponse response = diaryService.updateDiary(DEFAULT_USER_ID, diaryId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{diaryId}")
    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다 (Soft Delete)")
    public ResponseEntity<Void> deleteDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        diaryService.deleteDiary(DEFAULT_USER_ID, diaryId);
        return ResponseEntity.noContent().build();
    }
}
