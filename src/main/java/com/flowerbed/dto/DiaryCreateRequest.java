package com.flowerbed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaryCreateRequest {

    @NotNull(message = "일기 날짜는 필수입니다")
    private LocalDate diaryDate;

    @NotBlank(message = "일기 내용은 필수입니다")
    @Size(min = 10, max = 5000, message = "일기 내용은 10자 이상 5000자 이하여야 합니다")
    private String content;
}
