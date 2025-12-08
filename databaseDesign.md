# Database 설계 문서

## 개요
- **Database명**: flowerbed
- **DBMS**: MariaDB 10.x
- **문자셋**: utf8mb4_unicode_ci (이모지 지원)
- **엔진**: InnoDB (트랜잭션 지원)

---

## ERD (Entity Relationship Diagram)

```
┌─────────────────┐
│     users       │
│─────────────────│
│ user_id (PK)    │
│ email (UQ)      │
│ password        │
│ nickname        │
│ profile_image   │
│ created_at      │
│ updated_at      │
│ deleted_at      │
└─────────────────┘
        │
        │ 1:N
        ↓
┌──────────────────┐         ┌───────────────────┐
│    diaries       │         │    emotions       │
│──────────────────│         │───────────────────│
│ diary_id (PK)    │         │ emotion_code (PK) │
│ user_id (FK)     │ N:1     │ emotion_name_kr   │
│ diary_date       │←────────│ emotion_name_en   │
│ content          │         │ flower_name_kr    │
│ summary          │         │ flower_name_en    │
│ core_emotion─────┼────────→│ flower_meaning    │
│ core_emotion_code│         │ flower_...        │
│ emotion_reason   │         │ image_file_3d     │
│ flower_name      │         │ image_file_real   │
│ flower_meaning   │         │ is_positive       │
│ emotions_json    │         │ display_order     │
│ is_analyzed      │         │ created_at        │
│ analyzed_at      │         │ updated_at        │
│ created_at       │         └───────────────────┘
│ updated_at       │
│ deleted_at       │
└──────────────────┘
```

---

## 테이블 상세 설명

### 1. users (회원)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| user_id | BIGINT | PK, AUTO_INCREMENT | 회원 고유 ID |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 이메일 (로그인 ID) |
| password | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 (BCrypt 등) |
| nickname | VARCHAR(50) | NOT NULL | 닉네임 |
| profile_image | VARCHAR(255) | NULL | 프로필 이미지 URL |
| created_at | DATETIME | DEFAULT NOW | 가입일시 |
| updated_at | DATETIME | ON UPDATE NOW | 최종 수정일시 |
| deleted_at | DATETIME | NULL | 탈퇴일시 (Soft Delete) |

**인덱스**:
- `idx_email`: 로그인 시 이메일 조회
- `idx_created_at`: 가입일 기준 정렬

**비즈니스 규칙**:
- 이메일 중복 불가
- 탈퇴 시 deleted_at에 일시 기록 (실제 삭제 X)
- 탈퇴 후에도 일기 데이터는 CASCADE로 삭제됨

---

### 2. diaries (일기)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| diary_id | BIGINT | PK, AUTO_INCREMENT | 일기 고유 ID |
| user_id | BIGINT | FK, NOT NULL | 작성자 ID |
| diary_date | DATE | NOT NULL | 일기 날짜 |
| content | TEXT | NOT NULL | 일기 내용 (최대 5000자) |
| summary | TEXT | NULL | AI 생성 요약 |
| core_emotion | VARCHAR(20) | NULL | 대표 감정 (한글) |
| core_emotion_code | VARCHAR(20) | NULL | 대표 감정 코드 (영문) |
| emotion_reason | TEXT | NULL | 대표 감정 선택 이유 |
| flower_name | VARCHAR(50) | NULL | 꽃 이름 |
| flower_meaning | VARCHAR(100) | NULL | 꽃말 |
| emotions_json | LONGTEXT | NULL | 전체 감정 배열 JSON |
| is_analyzed | BOOLEAN | DEFAULT FALSE | 분석 완료 여부 |
| analyzed_at | DATETIME | NULL | 분석 완료 일시 |
| created_at | DATETIME | DEFAULT NOW | 작성일시 |
| updated_at | DATETIME | ON UPDATE NOW | 수정일시 |
| deleted_at | DATETIME | NULL | 삭제일시 (Soft Delete) |
| is_active | TINYINT(1) | GENERATED | deleted_at IS NULL ? 1 : NULL |

**인덱스**:
- `uk_user_date_active`: (user_id, diary_date, is_active) UNIQUE - 하루 1개 일기만
- `idx_user_date`: (user_id, diary_date) - 특정 날짜 조회
- `idx_user_created`: (user_id, created_at DESC) - 최신 일기 목록
- `idx_core_emotion`: (core_emotion) - 감정별 통계
- `idx_analyzed`: (is_analyzed, analyzed_at) - 분석 대기 목록

**emotions_json 예시**:
```json
[
  {"emotion": "기쁨", "percent": 60},
  {"emotion": "슬픔", "percent": 30},
  {"emotion": "혼란", "percent": 10}
]
```

**비즈니스 규칙**:
- 한 사용자는 하루에 일기 1개만 작성 가능 (uk_user_date_active)
- Soft Delete된 일기는 is_active가 NULL이 되어 UNIQUE 제약조건에서 제외됨
- 일기 작성 직후 is_analyzed=FALSE
- AI 분석 완료 후 is_analyzed=TRUE, analyzed_at 갱신

---

### 3. emotions (감정-꽃 마스터)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| emotion_code | VARCHAR(20) | PK | 감정 코드 (영문, 예: JOY) |
| emotion_name_kr | VARCHAR(20) | NOT NULL | 감정명 (한글, 예: 기쁨) |
| emotion_name_en | VARCHAR(20) | NOT NULL | 감정명 (영문, 예: Joy) |
| flower_name_kr | VARCHAR(50) | NOT NULL | 꽃 이름 (한글) |
| flower_name_en | VARCHAR(50) | NULL | 꽃 이름 (영문) |
| flower_meaning | VARCHAR(100) | NOT NULL | 꽃말 |
| flower_meaning_story | VARCHAR(1000) | NULL | 꽃말 유래 |
| flower_color | VARCHAR(50) | NULL | 꽃 색상 텍스트 |
| flower_color_codes | VARCHAR(500) | NULL | 꽃 색상 코드 (콤마 구분) |
| flower_origin | VARCHAR(100) | NULL | 꽃 원산지 |
| flower_fragrance | VARCHAR(50) | NULL | 꽃 향기 설명 |
| flower_blooming_season | VARCHAR(100) | NULL | 개화 시기 |
| flower_fun_fact | VARCHAR(1000) | NULL | 재미있는 사실 |
| image_file_3d | VARCHAR(100) | NOT NULL | 3D 이미지 파일명 |
| image_file_realistic | VARCHAR(100) | NOT NULL | 실사 이미지 파일명 |
| is_positive | BOOLEAN | NOT NULL | 긍정 감정 여부 |
| display_order | INT | NOT NULL | 표시 순서 |
| created_at | DATETIME | DEFAULT NOW | 생성일시 |
| updated_at | DATETIME | ON UPDATE NOW | 수정일시 |

**비즈니스 규칙**:
- 20개 고정 데이터 (초기 데이터 INSERT)
- 관리자 페이지에서 꽃말/이미지 수정 가능
- emotion_code는 diaries.core_emotion_code와 매칭

**초기 데이터**: 20개 감정 (기쁨, 행복, 감사, 설렘, 평온, 성취, 사랑, 희망, 활력, 재미, 슬픔, 외로움, 불안, 분노, 피로, 후회, 무기력, 혼란, 실망, 지루함)

---

## 주요 쿼리 패턴

### 1. 일기 작성 플로우

```sql
-- 1) 일기 작성
INSERT INTO diaries (user_id, diary_date, content)
VALUES (?, ?, ?);

-- 2) 작성된 일기 ID 반환
SELECT LAST_INSERT_ID();

-- 3) AI 분석 호출 (백엔드)

-- 4) 분석 결과 저장
UPDATE diaries
SET summary = ?,
    core_emotion = ?,
    core_emotion_code = ?,
    emotion_reason = ?,
    flower_name = ?,
    flower_meaning = ?,
    emotions_json = ?,
    is_analyzed = TRUE,
    analyzed_at = NOW()
WHERE diary_id = ?;
```

### 2. 월별 일기 목록 (꽃 정보 포함)

```sql
SELECT
    d.diary_id,
    d.diary_date,
    d.content,
    d.summary,
    d.core_emotion,
    d.core_emotion_code,
    d.flower_name,
    d.flower_meaning,
    d.emotions_json,
    d.emotion_reason,
    e.emotion_code,
    e.emotion_name_kr,
    e.emotion_name_en,
    e.flower_name_kr,
    e.flower_name_en,
    e.flower_color,
    e.flower_color_codes,
    e.flower_origin,
    e.flower_blooming_season,
    e.flower_fragrance,
    e.flower_meaning_story,
    e.flower_fun_fact,
    e.image_file_3d,
    e.image_file_realistic,
    e.is_positive
FROM diaries d
LEFT JOIN emotions e ON d.flower_name = e.flower_name_kr
WHERE d.user_id = ?
  AND YEAR(d.diary_date) = ?
  AND MONTH(d.diary_date) = ?
  AND d.deleted_at IS NULL
ORDER BY d.diary_date DESC;
```

### 3. 특정 날짜 일기 조회

```sql
SELECT d.*, e.*
FROM diaries d
LEFT JOIN emotions e ON d.flower_name = e.flower_name_kr
WHERE d.user_id = ?
  AND d.diary_date = ?
  AND d.deleted_at IS NULL;
```

### 4. 감정 통계 (최근 30일)

```sql
SELECT
    d.core_emotion,
    d.core_emotion_code,
    d.flower_name,
    e.is_positive,
    COUNT(*) as count,
    MAX(d.diary_date) as last_date
FROM diaries d
LEFT JOIN emotions e ON d.flower_name = e.flower_name_kr
WHERE d.user_id = ?
  AND d.deleted_at IS NULL
  AND d.is_analyzed = TRUE
  AND d.diary_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY d.core_emotion, d.core_emotion_code, d.flower_name, e.is_positive
ORDER BY count DESC;
```

---

## JPA Entity 매핑

### User Entity
```java
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@Where(clause = "deleted_at IS NULL")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String nickname;
    private String profileImage;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
```

### Diary Entity
```java
@Entity
@Table(name = "diaries")
@SQLDelete(sql = "UPDATE diaries SET deleted_at = NOW() WHERE diary_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate diaryDate;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String coreEmotion;
    private String coreEmotionCode;

    @Column(columnDefinition = "TEXT")
    private String emotionReason;

    private String flowerName;
    private String flowerMeaning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<EmotionPercent> emotionsJson;

    private Boolean isAnalyzed = false;
    private LocalDateTime analyzedAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
```

### Flower Entity (테이블명: emotions)
```java
@Entity
@Table(name = "emotions")
public class Flower {
    @Id
    @Column(name = "emotion_code")
    private String emotionCode;

    private String emotionNameKr;
    private String emotionNameEn;
    private String flowerNameKr;
    private String flowerNameEn;
    private String flowerMeaning;
    private String flowerMeaningStory;
    private String flowerColor;
    private String flowerColorCodes;
    private String flowerOrigin;
    private String flowerFragrance;
    private String flowerBloomingSeason;
    private String flowerFunFact;
    private String imageFile3d;
    private String imageFileRealistic;
    private Boolean isPositive;
    private Integer displayOrder;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## 성능 최적화

### 1. 인덱스 전략
- **복합 인덱스**: (user_id, diary_date) - 가장 빈번한 조회 패턴
- **커버링 인덱스**: 목록 조회 시 INDEX ONLY SCAN 가능
- **JSON 인덱스**: MariaDB 10.5+ 지원, emotions_json 검색 시 활용

### 2. 캐싱 전략
- **Redis 캐싱**:
    - emotions 테이블 전체 (변경 거의 없음)
    - 최근 일기 목록 (TTL: 5분)
    - 감정 통계 (TTL: 1시간)

---

## 백업 및 보안

### 1. 일일 백업
```bash
mysqldump -u root -p --single-transaction flowerbed > backup_$(date +%Y%m%d).sql
```

### 2. 보안
- BCrypt 비밀번호 암호화
- PreparedStatement 사용 (JPA 자동)
- 애플리케이션 전용 DB 계정 사용

---

## FAQ

### Q1. 왜 테이블명이 emotions인가요?
A. 이 테이블은 감정과 꽃을 매핑하는 마스터 데이터입니다. 엔티티명은 Flower이지만, 테이블명은 emotions입니다.

### Q2. core_emotion과 core_emotion_code의 차이는?
A. core_emotion은 한글 감정명("기쁨"), core_emotion_code는 영문 코드("JOY")입니다. emotions 테이블의 emotion_code와 조인할 때 사용합니다.

### Q3. is_active 컬럼은 왜 GENERATED인가요?
A. Soft Delete 시 UNIQUE 제약조건을 우회하기 위함입니다. deleted_at이 NULL이 아니면 is_active도 NULL이 되어 UNIQUE 체크에서 제외됩니다.
