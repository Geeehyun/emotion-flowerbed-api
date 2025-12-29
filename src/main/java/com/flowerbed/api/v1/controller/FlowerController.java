package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.AllEmotionsResponse;
import com.flowerbed.api.v1.dto.UserEmotionFlowerResponse;
import com.flowerbed.api.v1.service.FlowerService;
import com.flowerbed.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 꽃/감정 정보 관련 API Controller
 *
 * 감정-꽃 매핑 정보를 조회하는 기능을 제공합니다.
 * emotions 테이블의 마스터 데이터를 기반으로 합니다.
 *
 * API 종류:
 * 1. /my-emotions - 사용자가 작성한 일기에서 나타난 감정 통계 (개인화 데이터)
 * 2. /all-emotions - 전체 감정-꽃 매핑 정보 (마스터 데이터)
 */
@Tag(name = "Flower", description = "꽃 정보 API")
@RestController
@RequestMapping("/v1/flowers")
@RequiredArgsConstructor
public class FlowerController {

    private final FlowerService flowerService;

    /**
     * 사용자의 감정-꽃 통계 조회
     *
     * 사용자가 작성한 일기들을 분석하여 어떤 감정이 얼마나 나타났는지 통계를 제공합니다.
     * 감정별로 그룹화하여 나타난 횟수, 날짜 목록, 꽃 상세 정보를 반환합니다.
     *
     * @return UserEmotionFlowerResponse (감정별 통계 + 꽃 상세정보)
     *
     * Response 구조:
     * - items: 감정별 통계 배열
     *   - emotionCode: 감정 코드 (예: "JOYFUL")
     *   - flowerName: 꽃 이름 (예: "해바라기")
     *   - flowerMeaning: 꽃말
     *   - count: 해당 감정이 나타난 횟수
     *   - dates: 해당 감정의 일기 날짜 목록 ["2025-12-01", "2025-12-15"]
     *   - flowerDetail: 꽃 상세 정보 (색상, 원산지, 이미지 등)
     * - totalCount: 전체 감정 종류 개수
     *
     * 비즈니스 로직:
     * 1. 사용자의 분석 완료된 일기만 조회 (isAnalyzed=true)
     * 2. 감정 코드별로 그룹화
     * 3. 각 감정의 횟수, 날짜 집계
     * 4. emotions 테이블에서 꽃 상세 정보 조회
     *
     * 사용 예시:
     * - 감정 통계 대시보드
     * - 내가 느낀 감정 분포 시각화
     * - 꽃 컬렉션 표시
     */
    @Operation(summary = "사용자의 감정&꽃 리스트 조회", description = "사용자가 작성한 일기에서 나타난 감정과 꽃 리스트를 조회합니다")
    @GetMapping("/my-emotions")
    public ResponseEntity<UserEmotionFlowerResponse> getUserEmotionFlowers() {
        Long userSn = SecurityUtil.getCurrentUserSn();
        UserEmotionFlowerResponse response = flowerService.getUserEmotionFlowers(userSn);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 감정-꽃 매핑 정보 조회
     *
     * 시스템에 등록된 모든 감정-꽃 매핑 정보를 조회합니다.
     * emotions 테이블의 전체 마스터 데이터를 display_order 순으로 정렬하여 반환합니다.
     *
     * @return AllEmotionsResponse (전체 감정-꽃 매핑 정보)
     *
     * Response 구조:
     * - emotions: 감정-꽃 정보 배열
     *   - emotionCode: 감정 코드 (예: "JOYFUL")
     *   - emotionNameKr: 감정명 한글 (예: "기쁨")
     *   - emotionNameEn: 감정명 영어 (예: "Joyful")
     *   - flowerNameKr: 꽃 이름 한글 (예: "해바라기")
     *   - flowerNameEn: 꽃 이름 영어 (예: "Sunflower")
     *   - flowerMeaning: 꽃말
     *   - flowerMeaningStory: 꽃말 유래
     *   - flowerColor: 꽃 색상 텍스트
     *   - flowerColorCodes: 색상 HEX 코드 (쉼표로 구분)
     *   - flowerOrigin: 원산지
     *   - flowerFragrance: 향기 특성
     *   - flowerFunFact: 꽃 관련 재미있는 이야기
     *   - imageFile3d: 3D 이미지 파일명
     *   - imageFileRealistic: 실사 이미지 파일명
     *   - area: 감정 영역 (red/yellow/green/blue)
     *   - displayOrder: 표시 순서
     * - totalCount: 전체 감정 개수
     *
     * 사용 예시:
     * - 감정 선택 UI (드롭다운, 카드 리스트 등)
     * - 꽃 도감/백과사전
     * - 초기 데이터 로딩
     *
     * !! 참고 !!
     * - 인증 없이 조회 가능 (공개 마스터 데이터)
     * - display_order에 따라 정렬되어 있음
     * - DB에 감정 데이터 추가 시 자동으로 반영됨
     */
    @Operation(summary = "전체 감정-꽃 정보 조회", description = "시스템에 등록된 모든 감정-꽃 매핑 정보를 조회합니다")
    @GetMapping("/all-emotions")
    public ResponseEntity<AllEmotionsResponse> getAllEmotions() {
        AllEmotionsResponse response = flowerService.getAllEmotions();
        return ResponseEntity.ok(response);
    }
}
