package com.flowerbed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyDiariesResponse {

    private String yearMonth;
    private List<DiaryListItem> diaries;
    private Integer totalCount;
    private Boolean hasNextMonth;
    private Boolean hasPrevMonth;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryListItem {
        private Long id;
        private LocalDate date;
        private String content;
        private String coreEmotion;
        private String flower;
        private String floriography;
        private String summary;
    }
}
