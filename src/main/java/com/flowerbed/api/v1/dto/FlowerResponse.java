package com.flowerbed.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowerResponse {

    private Integer flowerId;
    private String emotion;
    private String flowerNameKr;
    private String flowerNameEn;
    private String flowerMeaning;
    private String flowerColor;
    private String flowerColorCodes;
    private String flowerOrigin;
    private String flowerBloomingSeason;
    private String flowerFragrance;
    private String flowerMeaningOrigin;
    private String flowerFunFact;
    private String imageFile3d;
    private String imageFileRealistic;
    private String area;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
