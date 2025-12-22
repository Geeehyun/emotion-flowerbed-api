-- ========================================
-- 공통 코드 테이블 생성 스크립트
-- ========================================
-- code_groups: 코드 그룹 테이블 (ROLE, EMOTION_CONTROL 등)
-- codes: 실제 코드 데이터 테이블
-- ========================================

-- 1. code_groups 테이블 생성
CREATE TABLE `code_groups` (
    `group_code` VARCHAR(50) NOT NULL COMMENT '그룹 코드 (PK)',
    `group_name` VARCHAR(100) NOT NULL COMMENT '그룹명',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '설명',
    `is_editable` TINYINT(1) DEFAULT 1 COMMENT '수정 가능 여부 (시스템 코드는 0)',
    `display_order` INT NOT NULL COMMENT '표시 순서',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP() COMMENT '생성일시',
    `created_by` VARCHAR(255) DEFAULT NULL COMMENT '생성자 사용자 ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP() COMMENT '수정일시',
    `updated_by` VARCHAR(255) DEFAULT NULL COMMENT '수정자 사용자 ID',
    PRIMARY KEY (`group_code`),
    KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='코드 그룹';

-- 2. codes 테이블 생성
CREATE TABLE `codes` (
    `code_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '코드 ID (PK)',
    `group_code` VARCHAR(50) NOT NULL COMMENT '그룹 코드 (FK)',
    `code` VARCHAR(50) NOT NULL COMMENT '코드값',
    `code_name` VARCHAR(100) NOT NULL COMMENT '코드명',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '설명',
    `is_active` TINYINT(1) DEFAULT 1 COMMENT '활성 여부',
    `display_order` INT NOT NULL COMMENT '표시 순서',
    `extra_value1` VARCHAR(200) DEFAULT NULL COMMENT '확장 필드1',
    `extra_value2` VARCHAR(200) DEFAULT NULL COMMENT '확장 필드2',
    `extra_value3` VARCHAR(200) DEFAULT NULL COMMENT '확장 필드3',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP() COMMENT '생성일시',
    `created_by` VARCHAR(255) DEFAULT NULL COMMENT '생성자 사용자 ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP() COMMENT '수정일시',
    `updated_by` VARCHAR(255) DEFAULT NULL COMMENT '수정자 사용자 ID',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '삭제일시 (soft delete)',
    `deleted_by` VARCHAR(255) DEFAULT NULL COMMENT '삭제자 사용자 ID',
    PRIMARY KEY (`code_id`),
    UNIQUE KEY `uk_group_code` (`group_code`, `code`),
    KEY `idx_group_code` (`group_code`),
    KEY `idx_is_active` (`is_active`),
    KEY `idx_display_order` (`display_order`),
    CONSTRAINT `fk_codes_group` FOREIGN KEY (`group_code`) REFERENCES `code_groups` (`group_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='코드';
