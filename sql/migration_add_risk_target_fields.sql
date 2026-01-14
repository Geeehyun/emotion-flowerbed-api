-- 위험도 분석 대상 일기 정보 필드 추가
-- 작성일: 2026-01-13
-- 목적: 위험도 분석의 기준이 된 일기 날짜와 SN을 추적하기 위함

-- 1. users 테이블에 위험도 분석 대상 일기 정보 추가
ALTER TABLE users
ADD COLUMN risk_target_diary_date DATE COMMENT '위험도 분석 대상 일기 날짜',
ADD COLUMN risk_target_diary_sn BIGINT COMMENT '위험도 분석 대상 일기 SN';

-- 2. student_risk_history 테이블에 위험도 분석 기준 일기 정보 추가
ALTER TABLE student_risk_history
ADD COLUMN target_diary_date DATE COMMENT '위험도 분석 기준 일기 날짜',
ADD COLUMN target_diary_sn BIGINT COMMENT '위험도 분석 기준 일기 SN';

-- 필드 설명:
-- users.risk_last_checked_date: 위험도 분석을 실행한 날짜 (LocalDate.now())
-- users.risk_target_diary_date: 분석 대상이 된 일기의 날짜
-- users.risk_target_diary_sn: 분석 대상이 된 일기의 SN (일기 삭제 시 추적 가능)

-- student_risk_history.target_diary_date: 이력 시점의 기준 일기 날짜
-- student_risk_history.target_diary_sn: 이력 시점의 기준 일기 SN
