package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.domain.UserSettings;
import com.flowerbed.api.v1.dto.UserSettingsResponse;
import com.flowerbed.api.v1.dto.UserSettingsUpdateRequest;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.api.v1.repository.UserSettingsRepository;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 설정 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 설정 조회
     */
    public UserSettingsResponse getSettings(Long userSn) {
        UserSettings settings = userSettingsRepository.findByUserSn(userSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자 설정을 찾을 수 없습니다"));

        return UserSettingsResponse.builder()
                .themeColor(settings.getThemeColor())
                .themeGardenBg(settings.getThemeGardenBg())
                .build();
    }

    /**
     * 사용자 설정 수정
     */
    @Transactional
    public UserSettingsResponse updateSettings(Long userSn, UserSettingsUpdateRequest request) {
        User user = userRepository.findById(userSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

        UserSettings settings = userSettingsRepository.findByUserSn(userSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자 설정을 찾을 수 없습니다"));

        // 설정 업데이트
        settings.updateSettings(request.getThemeColor(), request.getThemeGardenBg());

        log.info("User settings updated: userSn={}, themeColor={}, themeGardenBg={}",
                userSn, request.getThemeColor(), request.getThemeGardenBg());

        return UserSettingsResponse.builder()
                .themeColor(settings.getThemeColor())
                .themeGardenBg(settings.getThemeGardenBg())
                .build();
    }
}
