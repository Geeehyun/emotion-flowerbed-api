package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.User;
import com.flowerbed.api.v1.domain.UserSettings;
import com.flowerbed.api.v1.dto.UserInfoResponse;
import com.flowerbed.api.v1.repository.UserRepository;
import com.flowerbed.api.v1.repository.UserSettingsRepository;
import com.flowerbed.exception.business.BusinessException;
import com.flowerbed.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    /**
     * 내 정보 조회
     */
    public UserInfoResponse getMyInfo(Long userSn) {
        User user = userRepository.findById(userSn)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

        // 학생인 경우에만 사용자 설정 조회
        UserSettings userSettings = null;
        if ("STUDENT".equals(user.getUserTypeCd())) {
            userSettings = userSettingsRepository.findByUserSn(userSn)
                    .orElse(null);
        }

        return UserInfoResponse.builder()
                .userSn(user.getUserSn())
                .userId(user.getUserId())
                .name(user.getName())
                .userTypeCd(user.getUserTypeCd())
                .schoolCode(user.getSchoolCode())
                .schoolNm(user.getSchoolNm())
                .classCode(user.getClassCode())
                .themeColor(userSettings != null ? userSettings.getThemeColor() : null)
                .themeGardenBg(userSettings != null ? userSettings.getThemeGardenBg() : null)
                .build();
    }
}
