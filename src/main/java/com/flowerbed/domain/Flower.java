package com.flowerbed.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "flowers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Flower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flower_id")
    private Integer flowerId;

    @Column(unique = true, nullable = false, length = 20)
    private String emotion;

    @Column(name = "flower_name", nullable = false, length = 50)
    private String flowerName;

    @Column(name = "flower_meaning", nullable = false, length = 100)
    private String flowerMeaning;

    @Column(name = "image_file_3d", nullable = false, length = 100)
    private String imageFile3d;

    @Column(name = "image_file_realistic", nullable = false, length = 100)
    private String imageFileRealistic;

    @Column(name = "is_positive", nullable = false)
    private Boolean isPositive;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Flower(String emotion, String flowerName, String flowerMeaning,
                  String imageFile3d, String imageFileRealistic,
                  Boolean isPositive, Integer displayOrder) {
        this.emotion = emotion;
        this.flowerName = flowerName;
        this.flowerMeaning = flowerMeaning;
        this.imageFile3d = imageFile3d;
        this.imageFileRealistic = imageFileRealistic;
        this.isPositive = isPositive;
        this.displayOrder = displayOrder;
    }

    public void updateFlowerInfo(String flowerName, String flowerMeaning) {
        this.flowerName = flowerName;
        this.flowerMeaning = flowerMeaning;
    }

    public void updateImages(String imageFile3d, String imageFileRealistic) {
        this.imageFile3d = imageFile3d;
        this.imageFileRealistic = imageFileRealistic;
    }
}
