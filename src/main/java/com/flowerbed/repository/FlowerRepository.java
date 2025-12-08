package com.flowerbed.repository;

import com.flowerbed.domain.Emotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlowerRepository extends JpaRepository<Emotion, String> {

    Optional<Emotion> findByEmotionCode(String emotionCode);

    Optional<Emotion> findByFlowerNameKr(String flowerNameKr);
}
