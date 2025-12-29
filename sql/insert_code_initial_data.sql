-- ========================================
-- 공통 코드 초기 데이터 입력 스크립트
-- ========================================

-- 1. 코드 그룹 데이터
INSERT INTO code_groups (group_code, group_name, description, is_editable, display_order, created_by)
VALUES
    ('USER_TYPE', '사용자 유형', '사용자 유형 구분 (학생/교사/관리자)', 0, 1, 'SYSTEM'),
    ('EMOTION_CONTROL', '감정 제어 활동', '감정 조절을 위한 활동 유형', 1, 2, 'SYSTEM');

-- 2. USER_TYPE 코드 데이터
INSERT INTO codes (group_code, code, code_name, description, is_active, display_order, created_by)
VALUES
    ('USER_TYPE', 'STUDENT', '학생', '일반 학생 사용자', 1, 1, 'SYSTEM'),
    ('USER_TYPE', 'TEACHER', '교사', '교사 사용자 (학생 관리 가능)', 1, 2, 'SYSTEM'),
    ('USER_TYPE', 'ADMIN', '관리자', '시스템 관리자 (모든 권한)', 1, 3, 'SYSTEM');

-- 3. EMOTION_CONTROL 코드 데이터
-- extra_value1: 권장 소요 시간 (분)
-- extra_value2: 난이도 (easy, medium, hard)
-- extra_value3: 카테고리 (physical, mental, creative, social)
INSERT INTO codes (group_code, code, code_name, description, is_active, display_order, extra_value1, extra_value2, extra_value3, created_by)
VALUES
    ('EMOTION_CONTROL', 'DEEP_BREATHING', '심호흡하기', '깊게 숨을 들이마시고 천천히 내쉬며 마음을 진정시킵니다', 1, 1, '5', 'easy', 'mental', 'SYSTEM'),
    ('EMOTION_CONTROL', 'WALK', '산책하기', '주변을 천천히 걸으며 생각을 정리합니다', 1, 2, '15', 'easy', 'physical', 'SYSTEM'),
    ('EMOTION_CONTROL', 'DRAW', '그림 그리기', '감정을 그림으로 표현해봅니다', 1, 3, '20', 'medium', 'creative', 'SYSTEM'),
    ('EMOTION_CONTROL', 'TALK', '친구와 대화하기', '신뢰하는 사람과 이야기를 나눕니다', 1, 4, '30', 'medium', 'social', 'SYSTEM');

-- ========================================
-- 적용 후 확인
-- ========================================
-- SELECT * FROM code_groups ORDER BY display_order;
-- SELECT * FROM codes WHERE group_code = 'USER_TYPE' ORDER BY display_order;
-- SELECT * FROM codes WHERE group_code = 'EMOTION_CONTROL' ORDER BY display_order;
