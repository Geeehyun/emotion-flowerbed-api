-- =====================================================
-- 학생 위험도 분석 시스템 테이블 생성 및 컬럼 추가
-- =====================================================

-- 1. users 테이블에 위험도 관련 컬럼 추가
ALTER TABLE users
ADD COLUMN risk_level VARCHAR(20) DEFAULT 'NORMAL' COMMENT '위험도 (NORMAL, CAUTION, DANGER)',
ADD COLUMN risk_continuous_area VARCHAR(20) COMMENT '연속된 감정 영역 (red, yellow, blue, green)',
ADD COLUMN risk_continuous_days INT DEFAULT 0 COMMENT '연속 일수',
ADD COLUMN risk_reason TEXT COMMENT '위험도 판정 사유',
ADD COLUMN risk_last_checked_date DATE COMMENT '마지막 위험도 체크 날짜',
ADD COLUMN risk_updated_at DATETIME COMMENT '위험도 변경 시각',
ADD COLUMN danger_resolved_by BIGINT COMMENT '위험 상태 해제한 선생님 user_sn',
ADD COLUMN danger_resolved_at DATETIME COMMENT '위험 상태 해제 시각',
ADD COLUMN danger_resolve_memo TEXT COMMENT '위험 해제 사유 (선생님 메모)';

-- 2. 위험도 조회 성능을 위한 인덱스 추가
CREATE INDEX idx_users_risk_level ON users(risk_level);
CREATE INDEX idx_users_risk_checked ON users(risk_last_checked_date);

-- 3. student_risk_history 테이블 생성 (위험도 변화 이력)
CREATE TABLE student_risk_history (
  history_id BIGINT PRIMARY KEY AUTO_INCREMENT,

  -- 학생 정보
  user_sn BIGINT NOT NULL COMMENT '학생 user_sn',

  -- 위험도 변화
  previous_level VARCHAR(20) COMMENT '이전 위험도 (NORMAL, CAUTION, DANGER)',
  new_level VARCHAR(20) NOT NULL COMMENT '새 위험도',

  -- 위험 유형 및 사유
  risk_type VARCHAR(50) NOT NULL COMMENT '위험 유형 (CONTINUOUS_SAME_AREA, CONTINUOUS_RED_BLUE, KEYWORD_DETECTED 등)',
  risk_reason TEXT COMMENT '위험도 판정 상세 사유',

  -- 연속 영역 관련
  continuous_area VARCHAR(20) COMMENT '연속된 감정 영역',
  continuous_days INT COMMENT '연속 일수',

  -- LLM 분석 결과 (2단계 확장용)
  concern_keywords JSON COMMENT '우려되는 키워드 목록 ["혼자", "외로워"]',

  -- 선생님 확인 (나중 확장용)
  is_confirmed BOOLEAN DEFAULT FALSE COMMENT '선생님 확인 여부',
  confirmed_by BIGINT COMMENT '확인한 선생님 user_sn',
  confirmed_at DATETIME COMMENT '확인 시각',
  teacher_memo TEXT COMMENT '선생님 메모',

  -- 생성 시각
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '이력 생성 시각',

  -- 외래키
  CONSTRAINT fk_risk_history_user FOREIGN KEY (user_sn) REFERENCES users(user_sn),

  -- 인덱스
  INDEX idx_user_created (user_sn, created_at DESC),
  INDEX idx_new_level (new_level),
  INDEX idx_confirmed (is_confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='학생 위험도 변화 이력';

-- =====================================================
-- 실행 순서
-- =====================================================
-- 1. 백업 먼저 수행
-- 2. 이 스크립트 실행
-- 3. 기존 학생들은 기본값 NORMAL, 0일로 초기화됨
-- =====================================================
