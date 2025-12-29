package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.CodeGroupResponse;
import com.flowerbed.api.v1.dto.CodeResponse;
import com.flowerbed.api.v1.service.CodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공통 코드 API Controller
 *
 * 공통 코드 조회 기능을 제공합니다.
 * 프론트엔드에서 드롭다운, 선택 옵션 등에 사용할 코드 데이터를 제공합니다.
 *
 * API 종류:
 * 1. GET /v1/codes/groups - 모든 코드 그룹 조회 (코드 포함)
 * 2. GET /v1/codes/groups/{groupCode} - 특정 코드 그룹 조회 (코드 포함)
 * 3. GET /v1/codes/{groupCode} - 특정 그룹의 코드 목록 조회
 * 4. GET /v1/codes/{groupCode}/{code} - 특정 코드 조회
 * 5. GET /v1/codes/user-types - 사용자 유형 코드 목록
 * 6. GET /v1/codes/emotion-controls - 감정 제어 활동 코드 목록
 */
@Tag(name = "Code", description = "공통 코드 API")
@Slf4j
@RestController
@RequestMapping("/v1/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    /**
     * 모든 코드 그룹 조회 (코드 포함)
     *
     * 시스템의 모든 코드 그룹과 각 그룹에 속한 활성화된 코드들을 조회합니다.
     *
     * @return 코드 그룹 목록 (코드 포함)
     *
     * 사용 예시:
     * ```
     * GET /v1/codes/groups
     * Authorization: Bearer {accessToken}
     * ```
     */
    @Operation(summary = "모든 코드 그룹 조회", description = "모든 코드 그룹과 코드 목록을 조회합니다")
    @GetMapping("/groups")
    public ResponseEntity<List<CodeGroupResponse>> getAllCodeGroups() {
        List<CodeGroupResponse> codeGroups = codeService.getAllCodeGroups();
        return ResponseEntity.ok(codeGroups);
    }

    /**
     * 특정 코드 그룹 조회 (코드 포함)
     *
     * @param groupCode 그룹 코드 (예: USER_TYPE, EMOTION_CONTROL)
     * @return 코드 그룹 정보 (코드 포함)
     *
     * 사용 예시:
     * ```
     * GET /v1/codes/groups/USER_TYPE
     * Authorization: Bearer {accessToken}
     * ```
     */
    @Operation(summary = "특정 코드 그룹 조회", description = "특정 코드 그룹과 코드 목록을 조회합니다")
    @GetMapping("/groups/{groupCode}")
    public ResponseEntity<CodeGroupResponse> getCodeGroup(@PathVariable String groupCode) {
        CodeGroupResponse codeGroup = codeService.getCodeGroup(groupCode);
        return ResponseEntity.ok(codeGroup);
    }

    /**
     * 특정 그룹의 코드 목록 조회
     *
     * @param groupCode 그룹 코드 (예: USER_TYPE, EMOTION_CONTROL)
     * @return 활성화된 코드 목록
     *
     * 사용 예시:
     * ```
     * GET /v1/codes/USER_TYPE
     * Authorization: Bearer {accessToken}
     * ```
     */
    @Operation(summary = "특정 그룹의 코드 목록 조회", description = "특정 그룹에 속한 활성화된 코드 목록을 조회합니다")
    @GetMapping("/{groupCode}")
    public ResponseEntity<List<CodeResponse>> getCodesByGroup(@PathVariable String groupCode) {
        List<CodeResponse> codes = codeService.getCodesByGroup(groupCode);
        return ResponseEntity.ok(codes);
    }

    /**
     * 특정 코드 조회
     *
     * @param groupCode 그룹 코드
     * @param code 코드값
     * @return 코드 정보
     *
     * 사용 예시:
     * ```
     * GET /v1/codes/USER_TYPE/STUDENT
     * Authorization: Bearer {accessToken}
     * ```
     */
    @Operation(summary = "특정 코드 조회", description = "그룹 코드와 코드값으로 특정 코드를 조회합니다")
    @GetMapping("/{groupCode}/{code}")
    public ResponseEntity<CodeResponse> getCode(
            @PathVariable String groupCode,
            @PathVariable String code) {
        CodeResponse codeResponse = codeService.getCode(groupCode, code);
        return ResponseEntity.ok(codeResponse);
    }

    /**
     * 사용자 유형 코드 목록 조회
     *
     * USER_TYPE 그룹의 코드 목록을 조회합니다.
     * - STUDENT: 학생
     * - TEACHER: 교사
     * - ADMIN: 관리자
     *
     * @return 사용자 유형 코드 목록
     *
     * 사용 예시:
     * ```
     * GET /v1/codes/user-types
     * Authorization: Bearer {accessToken}
     * ```
     */
    @Operation(summary = "사용자 유형 코드 조회", description = "사용자 유형(STUDENT/TEACHER/ADMIN) 코드 목록을 조회합니다")
    @GetMapping("/user-types")
    public ResponseEntity<List<CodeResponse>> getUserTypes() {
        List<CodeResponse> userTypes = codeService.getUserTypes();
        return ResponseEntity.ok(userTypes);
    }

    /**
     * 감정 제어 활동 코드 목록 조회
     *
     * EMOTION_CONTROL 그룹의 코드 목록을 조회합니다.
     * - DEEP_BREATHING: 심호흡하기
     * - WALK: 산책하기
     * - DRAW: 그림 그리기
     * - TALK: 친구와 대화하기
     *
     * @return 감정 제어 활동 코드 목록
     *
     * 사용 예시:
     * ```
     * GET /v1/codes/emotion-controls
     * Authorization: Bearer {accessToken}
     * ```
     */
    @Operation(summary = "감정 제어 활동 코드 조회", description = "감정 제어 활동 코드 목록을 조회합니다")
    @GetMapping("/emotion-controls")
    public ResponseEntity<List<CodeResponse>> getEmotionControls() {
        List<CodeResponse> emotionControls = codeService.getEmotionControls();
        return ResponseEntity.ok(emotionControls);
    }
}
