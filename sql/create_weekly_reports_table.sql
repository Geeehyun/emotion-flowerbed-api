-- 주간 리포트 테이블 생성
CREATE TABLE IF NOT EXISTS weekly_reports
(
    report_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '리포트 ID',
    user_sn        BIGINT       NOT NULL COMMENT '사용자 일련번호',
    start_date     DATE         NOT NULL COMMENT '주 시작일 (월요일)',
    end_date       DATE         NOT NULL COMMENT '주 종료일 (일요일)',
    diary_count    INT          NOT NULL COMMENT '작성한 일기 수',
    summary        TEXT COMMENT '한 주 요약',
    emotion_trend  TEXT COMMENT '감정 트렌드 분석',
    recommendations TEXT COMMENT '추천 사항',
    analysis_json  LONGTEXT COMMENT 'AI 분석 결과 (JSON)',

    -- 감사 정보
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    created_by     VARCHAR(100) COMMENT '생성자',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    updated_by     VARCHAR(100) COMMENT '수정자',
    deleted_at     DATETIME COMMENT '삭제일시 (Soft Delete)',

    -- 외래키 제약조건
    CONSTRAINT weekly_reports_users_user_sn_fk
        FOREIGN KEY (user_sn) REFERENCES users (user_sn)
            ON DELETE CASCADE,

    -- 유니크 제약조건 (사용자당 같은 주에 하나의 리포트만)
    CONSTRAINT uk_user_week UNIQUE (user_sn, start_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '주간 리포트';

-- 인덱스 생성
CREATE INDEX idx_weekly_reports_user_sn ON weekly_reports (user_sn);
CREATE INDEX idx_weekly_reports_start_date ON weekly_reports (start_date);
CREATE INDEX idx_weekly_reports_created_at ON weekly_reports (created_at);
