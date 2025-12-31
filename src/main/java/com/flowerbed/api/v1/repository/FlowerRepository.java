package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlowerRepository extends JpaRepository<Emotion, String> {

    Optional<Emotion> findByEmotionCode(String emotionCode);

    Optional<Emotion> findByEmotionNameKr(String emotionNameKr);

    Optional<Emotion> findByFlowerNameKr(String flowerNameKr);

    /**
     * display_order 순서로 모든 감정 조회
     */
    List<Emotion> findAllByOrderByDisplayOrderAsc();
}
