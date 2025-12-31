-- ========================================
-- 주간 리포트 테이블에 highlights 컬럼 추가
-- ========================================
-- 작업 일자: 2025-12-31
-- 작업 내용:
-- - highlights 컬럼 추가 (JSON 형식)
--   - flowerOfTheWeek: 이번 주 대표 꽃
--   - quickStats: 숫자로 보는 한 주
--   - gardenDiversity: 감정 정원 다양성
-- ========================================

ALTER TABLE weekly_reports
    ADD COLUMN highlights LONGTEXT COMMENT '주간 리포트 하이라이트 정보 (JSON)' AFTER weekly_diary_details;

-- ========================================
-- 변경 후 테이블 구조 확인
-- ========================================
-- DESC weekly_reports;

-- ========================================
-- 롤백 스크립트 (필요 시 사용)
-- ========================================
-- ALTER TABLE weekly_reports DROP COLUMN highlights;
