============================
-- Audit 컬럼 추가 스크립트
-- ========================================
-- 모든 테이블에 생성자/수정자/삭제자 정보를 추적하기 위한 컬럼 추가
-- - created_by: 생성자 사용자 ID
-- - updated_by: 수정자 사용자 ID
-- - deleted_by: 삭제자 사용자 ID
-- ========================================

-- 1. users 테이블에 audit 컬럼 추가
ALTER TABLE users
    ADD COLUMN created_by VARCHAR(255) COMMENT '생성자 사용자 ID' AFTER created_at,
    ADD COLUMN updated_by VARCHAR(255) COMMENT '수정자 사용자 ID' AFTER updated_at,
    ADD COLUMN deleted_by VARCHAR(255) COMMENT '삭제자 사용자 ID' AFTER deleted_at;

-- 2. diaries 테이블에 audit 컬럼 추가
ALTER TABLE diaries
    ADD COLUMN created_by VARCHAR(255) COMMENT '생성자 사용자 ID' AFTER created_at,
    ADD COLUMN updated_by VARCHAR(255) COMMENT '수정자 사용자 ID' AFTER updated_at,
    ADD COLUMN deleted_by VARCHAR(255) COMMENT '삭제자 사용자 ID' AFTER deleted_at;

-- 3. emotions 테이블에 audit 컬럼 추가
ALTER TABLE emotions
    ADD COLUMN created_by VARCHAR(255) COMMENT '생성자 사용자 ID' AFTER created_at,
    ADD COLUMN updated_by VARCHAR(255) COMMENT '수정자 사용자 ID' AFTER updated_at,
    ADD COLUMN deleted_by VARCHAR(255) COMMENT '삭제자 사용자 ID';

-- 참고: emotions 테이블은 deleted_at 컬럼이 없으므로 deleted_by는 마지막에 추가
-- 향후 emotions 테이블에 soft delete가 필요하면 deleted_at도 함께 추가 필요

-- ========================================
-- 적용 후 확인
-- ========================================
-- DESC users;
-- DESC diaries;
-- DESC emotions;
