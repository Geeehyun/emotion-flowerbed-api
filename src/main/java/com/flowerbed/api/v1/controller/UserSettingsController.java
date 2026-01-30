package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.UserSettingsResponse;
import com.flowerbed.api.v1.dto.UserSettingsUpdateRequest;
import com.flowerbed.api.v1.service.UserSettingsService;
import com.flowerbed.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 설정 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/v1/students/settings")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "사용자 설정 API")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    /**
     * 내 설정 조회
     *
     * 현재 로그인한 사용자의 설정 정보를 조회합니다.
     *
     * @return UserSettingsResponse (themeColor, themeGardenBg)
     */
    @GetMapping
    @Operation(summary = "내 설정 조회", description = "현재 로그인한 사용자의 설정 정보를 조회합니다")
    public ResponseEntity<UserSettingsResponse> getSettings() {
        Long userSn = SecurityUtil.getCurrentUserSn();
        UserSettingsResponse response = userSettingsService.getSettings(userSn);
        return ResponseEntity.ok(response);
    }

    /**
     * 설정 수정
     *
     * 현재 로그인한 사용자의 설정 정보를 수정합니다.
     *
     * @param request 수정할 설정 (themeColor, themeGardenBg)
     * @return UserSettingsResponse (수정된 설정 정보)
     */
    @PutMapping
    @Operation(summary = "설정 수정", description = "현재 로그인한 사용자의 설정 정보를 수정합니다")
    public ResponseEntity<UserSettingsResponse> updateSettings(
            @Valid @RequestBody UserSettingsUpdateRequest request) {
        Long userSn = SecurityUtil.getCurrentUserSn();
        UserSettingsResponse response = userSettingsService.updateSettings(userSn, request);
        return ResponseEntity.ok(response);
    }
}
