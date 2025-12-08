package com.flowerbed.repository;

import com.flowerbed.domain.Flower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlowerRepository extends JpaRepository<Flower, String> {

    Optional<Flower> findByEmotionCode(String emotionCode);

    Optional<Flower> findByFlowerNameKr(String flowerNameKr);
}
