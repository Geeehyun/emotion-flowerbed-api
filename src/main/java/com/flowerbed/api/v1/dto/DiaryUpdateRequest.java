package com.flowerbed.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryUpdateRequest {

    @NotBlank(message = "일기 내용은 필수입니다")
    @Size(min = 10, max = 5000, message = "일기 내용은 10자 이상 5000자 이하여야 합니다")
    private String content;
}
