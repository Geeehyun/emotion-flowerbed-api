-- ========================================
-- 주간 리포트 테이블 구조 개선
-- ========================================
-- 작업 일자: 2025-12-30
-- 작업 내용:
-- 1. AI 응답 구조를 개별 컬럼으로 분리 (JSON 통합 → 개별 컬럼)
-- 2. 읽음 상태 관리 필드 추가 (read_yn, new_notification_sent)
-- 3. 분석 여부 필드 추가 (is_analyzed)
-- ========================================

-- 1. 새로운 컬럼 추가
ALTER TABLE weekly_reports
    ADD COLUMN student_report TEXT COMMENT '학생용 주간 일기 분석' AFTER diary_count,
    ADD COLUMN student_encouragement TEXT COMMENT '학생용 응원/칭찬/격려의 말 (1~2문장)' AFTER student_report,
    ADD COLUMN teacher_report TEXT COMMENT '교사용 주간 일기 분석' AFTER student_encouragement,
    ADD COLUMN teacher_talk_tip LONGTEXT COMMENT '교사용 학생 말걸기 TIP (JSON 배열)' AFTER teacher_report,
    ADD COLUMN emotion_stats LONGTEXT COMMENT '주간 감정 통계 (JSON 배열: emotion, emotionNameKr, count, percentage)' AFTER teacher_talk_tip,
    ADD COLUMN weekly_diary_details LONGTEXT COMMENT '주간 일기 상세 정보 (JSON 배열: diaryId, diaryDate, coreEmotion, emotionNameKr, flowerNameKr, flowerMeaning)' AFTER emotion_stats,
    ADD COLUMN is_analyzed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '분석 여부 (일기 3개 이상 시 1)' AFTER weekly_diary_details,
    ADD COLUMN read_yn TINYINT(1) NOT NULL DEFAULT 0 COMMENT '학생 읽음 여부 (0: 미읽음, 1: 읽음)' AFTER is_analyzed,
    ADD COLUMN new_notification_sent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '새 리포트 발행 알림 전송 여부 (1회만)' AFTER read_yn;

-- 2. 기존 컬럼 제거
ALTER TABLE weekly_reports
    DROP COLUMN summary,
    DROP COLUMN emotion_trend,
    DROP COLUMN recommendations,
    DROP COLUMN analysis_json;

-- ========================================
-- 변경 후 테이블 구조 확인
-- ========================================
-- DESC weekly_reports;

-- ========================================
-- 롤백 스크립트 (필요 시 사용)
-- ========================================
-- ALTER TABLE weekly_reports
--     ADD COLUMN summary TEXT COMMENT '한 주 요약',
--     ADD COLUMN emotion_trend TEXT COMMENT '감정 트렌드 분석',
--     ADD COLUMN recommendations TEXT COMMENT '추천 사항',
--     ADD COLUMN analysis_json LONGTEXT COMMENT 'AI 분석 결과 (JSON)',
--     DROP COLUMN student_report,
--     DROP COLUMN student_encouragement,
--     DROP COLUMN teacher_report,
--     DROP COLUMN teacher_talk_tip,
--     DROP COLUMN emotion_stats,
--     DROP COLUMN weekly_diary_details,
--     DROP COLUMN is_analyzed,
--     DROP COLUMN read_yn,
--     DROP COLUMN new_notification_sent;
