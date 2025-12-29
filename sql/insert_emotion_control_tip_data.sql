-- ========================================
-- 감정 조절 팁 코드 데이터 추가 스크립트
-- ========================================
-- 감정 영역별로 연속 일수에 따른 팁 메시지를 관리합니다.
-- - 3일 이상 연속: 주의 단계
-- - 5일 이상 연속: 경고 단계
--
-- 감정 영역 (Emotion Area):
-- - RED: 빨강 영역 (강한 부정적 감정)
-- - YELLOW: 노랑 영역 (중간 부정적 감정)
-- - BLUE: 파랑 영역 (차분한 감정)
-- - GREEN: 초록 영역 (긍정적 감정)
-- ========================================

-- EMOTION_CONTROL 그룹에 감정 조절 팁 코드 추가
-- code_name: 프론트에서 표시할 팁 메시지
-- description: 팁에 대한 상세 설명
-- extra_value1: 영역 이름 (red, yellow, blue, green)
-- extra_value2: 단계 (warning=주의, alert=경고)
-- extra_value3: 연속 일수 (3, 5)

INSERT INTO codes (group_code, code, code_name, description, is_active, display_order, extra_value1, extra_value2, extra_value3, created_by)
VALUES
    -- RED 영역 (강한 부정적 감정)
    ('EMOTION_CONTROL', 'RED_3',
     '3일 연속 강한 감정이 감지되었어요. 잠시 멈추고 심호흡을 해보는 건 어떨까요?',
     'Red 영역 3일 연속 - 주의 단계 팁',
     1, 11, 'red', 'warning', '3', 'SYSTEM'),

    ('EMOTION_CONTROL', 'RED_5',
     '5일 이상 힘든 감정이 계속되고 있어요. 선생님이나 부모님과 이야기를 나눠보세요.',
     'Red 영역 5일 연속 - 경고 단계 팁',
     1, 12, 'red', 'alert', '5', 'SYSTEM'),

    -- YELLOW 영역 (중간 부정적 감정)
    ('EMOTION_CONTROL', 'YELLOW_3',
     '3일째 불편한 감정이 이어지고 있어요. 산책하며 기분 전환을 해보는 건 어떨까요?',
     'Yellow 영역 3일 연속 - 주의 단계 팁',
     1, 13, 'yellow', 'warning', '3', 'SYSTEM'),

    ('EMOTION_CONTROL', 'YELLOW_5',
     '5일 이상 마음이 편치 않은 것 같아요. 친구나 가족과 대화를 나눠보세요.',
     'Yellow 영역 5일 연속 - 경고 단계 팁',
     1, 14, 'yellow', 'alert', '5', 'SYSTEM'),

    -- BLUE 영역 (차분한 감정)
    ('EMOTION_CONTROL', 'BLUE_3',
     '3일째 조용한 감정이 계속되고 있어요. 좋아하는 활동을 해보는 건 어떨까요?',
     'Blue 영역 3일 연속 - 주의 단계 팁',
     1, 15, 'blue', 'warning', '3', 'SYSTEM'),

    ('EMOTION_CONTROL', 'BLUE_5',
     '5일 이상 무기력한 상태가 이어지고 있어요. 가벼운 운동이나 야외 활동을 시도해보세요.',
     'Blue 영역 5일 연속 - 경고 단계 팁',
     1, 16, 'blue', 'alert', '5', 'SYSTEM'),

    -- GREEN 영역 (긍정적 감정)
    ('EMOTION_CONTROL', 'GREEN_3',
     '3일째 좋은 감정이 이어지고 있어요! 이 기분을 계속 유지해보세요.',
     'Green 영역 3일 연속 - 긍정 강화 팁',
     1, 17, 'green', 'positive', '3', 'SYSTEM'),

    ('EMOTION_CONTROL', 'GREEN_5',
     '5일 이상 행복한 날들이 계속되고 있어요! 지금의 긍정적인 감정을 일기로 기록해보세요.',
     'Green 영역 5일 연속 - 긍정 강화 팁',
     1, 18, 'green', 'positive', '5', 'SYSTEM');

-- ========================================
-- 적용 후 확인
-- ========================================
-- 전체 EMOTION_CONTROL 코드 조회
-- SELECT * FROM codes WHERE group_code = 'EMOTION_CONTROL' ORDER BY display_order;

-- Red 영역 팁만 조회
-- SELECT * FROM codes WHERE group_code = 'EMOTION_CONTROL' AND extra_value1 = 'red';

-- 3일 연속 팁만 조회
-- SELECT * FROM codes WHERE group_code = 'EMOTION_CONTROL' AND extra_value3 = '3';
