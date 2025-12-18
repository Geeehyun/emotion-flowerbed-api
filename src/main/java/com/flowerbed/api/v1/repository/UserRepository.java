package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * userId(로그인 ID)로 사용자 조회
     */
    Optional<User> findByUserId(String userId);
}
