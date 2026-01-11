-- 일기 키워드 및 마음 가드닝 팁 기능 추가
-- 작성일: 2026-01-11

-- 1. diary 테이블에 keywords 컬럼 추가
ALTER TABLE diaries
ADD COLUMN keywords VARCHAR(500) NULL COMMENT '일기 핵심 키워드 (최대 3개, 쉼표로 구분)';

-- 2. weekly_report 테이블에 mind_gardening_tip 컬럼 추가
ALTER TABLE weekly_reports
ADD COLUMN mind_gardening_tip TEXT NULL COMMENT '마음 가드닝 팁 (LLM 생성, 학생용)';

-- 3. weekly_report 테이블에 week_keywords 컬럼 추가
ALTER TABLE weekly_reports
ADD COLUMN week_keywords VARCHAR(500) NULL COMMENT '주간 통합 핵심 키워드 (최대 5개, 쉼표로 구분)';
