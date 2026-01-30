package com.flowerbed.api.v1.controller;

import com.flowerbed.api.v1.dto.UserInfoResponse;
import com.flowerbed.api.v1.service.UserService;
import com.flowerbed.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 정보 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 정보 API")
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     *
     * 현재 로그인한 사용자의 정보를 조회합니다.
     * 로그인 응답과 동일한 사용자 정보를 반환합니다 (토큰 제외).
     *
     * @return UserInfoResponse (사용자 정보)
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
    public ResponseEntity<UserInfoResponse> getMyInfo() {
        Long userSn = SecurityUtil.getCurrentUserSn();
        UserInfoResponse response = userService.getMyInfo(userSn);
        return ResponseEntity.ok(response);
    }
}
