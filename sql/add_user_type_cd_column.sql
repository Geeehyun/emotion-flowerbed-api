-- ========================================
-- users 테이블에 user_type_cd 컬럼 추가
-- ========================================
-- codes 테이블의 USER_TYPE 그룹과 매핑되는 컬럼 추가
-- _cd suffix로 코드 참조 컬럼임을 명시
-- ========================================

ALTER TABLE users
    ADD COLUMN user_type_cd VARCHAR(50) DEFAULT NULL COMMENT '사용자 유형 코드 (STUDENT/TEACHER/ADMIN)' AFTER name;

-- ========================================
-- 적용 후 확인
-- ========================================
-- DESC users;

-- ========================================
-- 기존 데이터 업데이트 (필요시)
-- ========================================
-- UPDATE users SET user_type_cd = 'STUDENT' WHERE user_type_cd IS NULL;
