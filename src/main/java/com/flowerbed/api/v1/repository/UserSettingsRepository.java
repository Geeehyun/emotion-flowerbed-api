package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    /**
     * userSn으로 설정 조회
     */
    Optional<UserSettings> findByUserSn(Long userSn);
}
