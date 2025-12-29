package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.DiaryCreateRequest;
import com.flowerbed.api.v1.dto.DiaryResponse;
import com.flowerbed.api.v1.dto.DiaryUpdateRequest;
import com.flowerbed.api.v1.dto.MonthlyDiariesResponse;
import com.flowerbed.api.v1.service.DiaryService;
import com.flowerbed.security.SecurityUtil;
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
@RequestMapping("/v1/diaries")
@RequiredArgsConstructor
@Tag(name = "Diary", description = "일기 API")
public class DiaryController {

    private final DiaryService diaryService;

    /**
     * 일기 작성
     *
     * 새로운 일기를 작성합니다. 이 시점에는 일기 내용만 저장되고 감정 분석은 수행되지 않습니다.
     * 감정 분석은 별도로 /analyze 엔드포인트를 호출해야 합니다.
     *
     * @param request 일기 작성 요청 (diaryDate, content)
     * @return DiaryResponse (생성된 일기 정보, isAnalyzed=false 상태)
     *
     * 비즈니스 로직:
     * 1. JWT 토큰에서 인증된 사용자 정보 조회
     * 2. 같은 날짜에 이미 일기가 있는지 확인 (하루에 하나만 작성 가능)
     * 3. 일기 내용 유효성 검증 (10자 이상, 5000자 이하)
     * 4. DB에 저장 (isAnalyzed=false, 감정 정보 null)
     */
    @PostMapping
    @Operation(summary = "일기 작성", description = "새로운 일기를 작성합니다")
    public ResponseEntity<DiaryResponse> createDiary(
            @Valid @RequestBody DiaryCreateRequest request) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        DiaryResponse response = diaryService.createDiary(userSn, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 일기 감정 분석 (실제 Claude API 사용)
     *
     * 작성된 일기를 AI로 분석하여 감정 정보를 추출합니다.
     *
     * @param diaryId 분석할 일기 ID
     * @return DiaryResponse (감정 분석 결과 포함, isAnalyzed=true)
     *
     * 비즈니스 로직:
     * 1. 일기 존재 여부 및 권한 확인
     * 2. Claude API or OpenAI API 호출하여 감정 분석 수행
     * 3. 분석 결과를 일기에 저장:
     *    - coreEmotionCode: 대표 감정 코드 (예: "JOYFUL")
     *    - emotionsJson: 전체 감정 리스트 [{emotion, percent}, ...]
     *    - flowerName, flowerMeaning: 감정에 매칭된 꽃 정보
     *    - summary: AI가 생성한 일기 요약
     *    - reason: 대표 감정 선택 이유
     * 4. isAnalyzed=true, analyzedAt=현재시각 업데이트
     *
     * !! 주의 !!
     * - API 호출 비용이 발생합니다
     * - 테스트 시에는 /analyze-test 사용 권장
     */
    @PostMapping("/{diaryId}/analyze")
    @Operation(summary = "일기 감정 분석", description = "작성된 일기의 감정을 AI로 분석합니다")
    public ResponseEntity<DiaryResponse> analyzeDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        DiaryResponse response = diaryService.analyzeDiaryEmotion(userSn, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 감정 분석 (테스트 모드 - API 호출 없음)
     *
     * AI API를 호출하지 않고 DB의 emotions 테이블에서 랜덤하게 선택하여
     * 감정 분석 결과를 생성합니다. 개발/테스트 용도로 사용합니다.
     *
     * @param diaryId 분석할 일기 ID
     * @return DiaryResponse (랜덤 생성된 감정 분석 결과)
     *
     * 비즈니스 로직:
     * 1. DB에서 3개의 랜덤 감정 선택
     * 2. 랜덤 퍼센트 생성 (합계 100%)
     * 3. 가장 높은 퍼센트의 감정을 대표 감정으로 설정
     * 4. 대표 감정에 매칭되는 꽃 정보 조회
     *
     * !! 장점 !!
     * - API 호출 비용 없음
     * - 빠른 테스트 가능
     * - 실제 데이터 구조와 동일한 결과 반환
     */
    @PostMapping("/{diaryId}/analyze-test")
    @Operation(summary = "일기 감정 분석 (테스트)",
               description = "Claude API 호출 없이 랜덤으로 감정 분석 결과를 생성합니다 (테스트용)")
    public ResponseEntity<DiaryResponse> analyzeDiaryTest(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        DiaryResponse response = diaryService.analyzeDiaryEmotionTest(userSn, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 상세 조회
     *
     * 특정 일기의 전체 정보를 조회합니다. 감정 분석이 완료된 경우 분석 결과도 함께 반환됩니다.
     *
     * @param diaryId 조회할 일기 ID
     * @return DiaryResponse (일기 정보 + 감정 분석 결과 + 꽃 상세정보)
     *
     * Response 구조:
     * - 기본 정보: diaryId, diaryDate, content, createdAt, updatedAt
     * - 분석 정보: summary, coreEmotionCode, emotionReason, emotions (퍼센트 리스트)
     * - 꽃 정보: flowerName, flowerMeaning
     * - 꽃 상세정보: flowerDetail (emotionNameKr, flowerColor, flowerOrigin, imageFile3d 등)
     *
     * !! 주의 !!
     * - 다른 사용자의 일기는 조회 불가 (권한 체크)
     * - 삭제된 일기는 조회 불가 (Soft Delete)
     */
    @GetMapping("/{diaryId}")
    @Operation(summary = "일기 상세 조회", description = "일기의 상세 정보를 조회합니다")
    public ResponseEntity<DiaryResponse> getDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        DiaryResponse response = diaryService.getDiary(userSn, diaryId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 날짜 일기 조회
     *
     * 지정한 날짜의 일기를 조회합니다. 하루에 하나의 일기만 작성 가능하므로
     * 해당 날짜의 일기가 있으면 반환하고, 없으면 404 에러를 반환합니다.
     *
     * @param date 조회할 날짜 (YYYY-MM-DD 형식)
     * @return DiaryResponse (해당 날짜의 일기 정보)
     *
     * 사용 예시:
     * - GET /diaries/date/2025-12-17
     * - 달력 UI에서 특정 날짜를 클릭했을 때 사용
     *
     * !! 참고 !!
     * - 날짜 형식이 잘못되면 400 Bad Request
     * - 해당 날짜에 일기가 없으면 404 Not Found
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "특정 날짜 일기 조회", description = "특정 날짜의 일기를 조회합니다")
    public ResponseEntity<DiaryResponse> getDiaryByDate(
            @Parameter(description = "날짜 (YYYY-MM-DD)")
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        DiaryResponse response = diaryService.getDiaryByDate(userSn, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 월별 일기 목록 조회
     *
     * 특정 월의 모든 일기를 목록으로 조회합니다. 달력 UI에 표시할 때 사용합니다.
     *
     * @param yearMonth 조회할 년월 (YYYY-MM 형식, 예: "2025-12")
     * @return MonthlyDiariesResponse (해당 월의 일기 목록)
     *
     * Response 구조:
     * - yearMonth: 요청한 년월
     * - diaries: 일기 목록 배열
     *   - id, date, content, summary, coreEmotionCode
     *   - flower, floriography (꽃 이름, 꽃말)
     *   - emotions (감정 퍼센트 배열)
     *   - flowerDetail (꽃 상세 정보 - 색상, 이미지, 원산지 등)
     * - totalCount: 일기 개수
     *
     * 사용 예시:
     * - GET /diaries?yearMonth=2025-12
     * - 달력에서 해당 월의 모든 일기를 표시할 때 사용
     */
    @GetMapping
    @Operation(summary = "월별 일기 목록 조회", description = "특정 월의 일기 목록을 조회합니다")
    public ResponseEntity<MonthlyDiariesResponse> getMonthlyDiaries(
            @Parameter(description = "년월 (YYYY-MM)", example = "2025-12")
            @RequestParam String yearMonth) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        MonthlyDiariesResponse response = diaryService.getMonthlyDiaries(userSn, yearMonth);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 수정
     *
     * 일기의 내용을 수정합니다. 수정 시 감정 분석 정보는 초기화됩니다.
     *
     * @param diaryId 수정할 일기 ID
     * @param request 수정할 내용 (content)
     * @return DiaryResponse (수정된 일기 정보, isAnalyzed=false)
     *
     * 비즈니스 로직:
     * 1. 일기 존재 여부 및 권한 확인
     * 2. 내용 유효성 검증 (10자 이상, 5000자 이하)
     * 3. content 업데이트
     * 4. 감정 분석 정보 초기화:
     *    - isAnalyzed = false
     *    - summary, coreEmotionCode, emotions 등 = null
     *
     * !! 주의 !!
     * - 수정 후 다시 /analyze를 호출해야 감정 분석 결과가 생성됩니다
     * - 다른 사용자의 일기는 수정 불가
     */
    @PutMapping("/{diaryId}")
    @Operation(summary = "일기 수정", description = "일기 내용을 수정합니다")
    public ResponseEntity<DiaryResponse> updateDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId,
            @Valid @RequestBody DiaryUpdateRequest request) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        DiaryResponse response = diaryService.updateDiary(userSn, diaryId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 일기 삭제 (Soft Delete)
     *
     * 일기를 삭제합니다. 실제로 DB에서 삭제되지 않고 deleted_at에 삭제 시각이 기록됩니다.
     *
     * @param diaryId 삭제할 일기 ID
     * @return 204 No Content
     *
     * 비즈니스 로직:
     * 1. 일기 존재 여부 및 권한 확인
     * 2. deleted_at = NOW() 업데이트 (Soft Delete)
     * 3. 이후 조회 시 제외됨 (@Where clause)
     *
     * !! Soft Delete 장점 !!
     * - 실제 데이터는 보존되어 복구 가능
     * - 통계/분석 시 데이터 활용 가능
     * - 데이터 정합성 유지
     *
     * !! 주의 !!
     * - 다른 사용자의 일기는 삭제 불가
     * - 같은 날짜에 새 일기 작성 가능 (deleted_at이 NULL인 것만 UNIQUE 제약)
     */
    @DeleteMapping("/{diaryId}")
    @Operation(summary = "일기 삭제", description = "일기를 삭제합니다 (Soft Delete)")
    public ResponseEntity<Void> deleteDiary(
            @Parameter(description = "일기 ID") @PathVariable Long diaryId) {

        Long userSn = SecurityUtil.getCurrentUserSn();
        diaryService.deleteDiary(userSn, diaryId);
        return ResponseEntity.noContent().build();
    }
}
