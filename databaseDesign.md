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
┌─────────────────┐         ┌─────────────────┐
│    diaries      │         │    flowers      │
│─────────────────│         │─────────────────│
│ diary_id (PK)   │         │ flower_id (PK)  │
│ user_id (FK)    │ N:1     │ emotion (UQ)    │
│ diary_date      │←────────│ flower_name     │
│ content         │         │ flower_meaning  │
│ summary         │         │ image_file_3d   │
│ core_emotion────┼────────→│ image_file_real │
│ emotion_reason  │         │ is_positive     │
│ flower_name     │         │ display_order   │
│ flower_meaning  │         │ created_at      │
│ emotions_json   │         │ updated_at      │
│ is_analyzed     │         └─────────────────┘
│ analyzed_at     │
│ created_at      │
│ updated_at      │
│ deleted_at      │
└─────────────────┘
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
| core_emotion | VARCHAR(20) | NULL | 대표 감정 (20개 중 하나) |
| emotion_reason | TEXT | NULL | 대표 감정 선택 이유 |
| flower_name | VARCHAR(50) | NULL | 꽃 이름 |
| flower_meaning | VARCHAR(100) | NULL | 꽃말 |
| emotions_json | JSON | NULL | 전체 감정 배열 |
| is_analyzed | BOOLEAN | DEFAULT FALSE | 분석 완료 여부 |
| analyzed_at | DATETIME | NULL | 분석 완료 일시 |
| created_at | DATETIME | DEFAULT NOW | 작성일시 |
| updated_at | DATETIME | ON UPDATE NOW | 수정일시 |
| deleted_at | DATETIME | NULL | 삭제일시 (Soft Delete) |

**인덱스**:
- `uk_user_date`: (user_id, diary_date) 유니크 - 하루 1개 일기만
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
- 한 사용자는 하루에 일기 1개만 작성 가능 (uk_user_date)
- 일기 작성 직후 is_analyzed=FALSE
- AI 분석 완료 후 is_analyzed=TRUE, analyzed_at 갱신
- 삭제 시 deleted_at에 일시 기록

---

### 3. flowers (꽃 마스터)

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| flower_id | INT | PK, AUTO_INCREMENT | 꽃 고유 ID |
| emotion | VARCHAR(20) | UNIQUE, NOT NULL | 감정명 (20개) |
| flower_name | VARCHAR(50) | NOT NULL | 꽃 이름 |
| flower_meaning | VARCHAR(100) | NOT NULL | 꽃말 |
| image_file_3d | VARCHAR(100) | NOT NULL | 3D 이미지 파일명 |
| image_file_realistic | VARCHAR(100) | NOT NULL | 실사 이미지 파일명 |
| is_positive | BOOLEAN | NOT NULL | 긍정 감정 여부 |
| display_order | INT | NOT NULL | 표시 순서 |
| created_at | DATETIME | DEFAULT NOW | 생성일시 |
| updated_at | DATETIME | ON UPDATE NOW | 수정일시 |

**인덱스**:
- `idx_emotion`: 감정명으로 조회
- `idx_display_order`: 정렬용

**비즈니스 규칙**:
- 20개 고정 데이터 (초기 데이터 INSERT)
- 관리자 페이지에서 꽃말/이미지 수정 가능
- emotion은 diaries.core_emotion과 매칭

**초기 데이터**: 20개 감정 (기쁨, 행복, 감사... 지루함)

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
    emotion_reason = ?,
    flower_name = ?,
    flower_meaning = ?,
    emotions_json = ?,
    is_analyzed = TRUE,
    analyzed_at = NOW()
WHERE diary_id = ?;
```

### 2. 화단 화면 (일기 목록)

```sql
-- 최근 일기 20개 (페이징)
SELECT 
    d.diary_id,
    d.diary_date,
    d.summary,
    d.core_emotion,
    d.flower_name,
    f.image_file_3d,
    f.is_positive
FROM diaries d
LEFT JOIN flowers f ON d.core_emotion = f.emotion
WHERE d.user_id = ?
  AND d.deleted_at IS NULL
ORDER BY d.diary_date DESC
LIMIT 20 OFFSET ?;
```

### 3. 일기 상세 조회

```sql
SELECT 
    d.*,
    f.image_file_realistic,
    f.flower_meaning,
    f.is_positive
FROM diaries d
LEFT JOIN flowers f ON d.core_emotion = f.emotion
WHERE d.diary_id = ?
  AND d.user_id = ?
  AND d.deleted_at IS NULL;
```

### 4. 특정 날짜 일기 조회

```sql
SELECT d.*, f.image_file_3d
FROM diaries d
LEFT JOIN flowers f ON d.core_emotion = f.emotion
WHERE d.user_id = ?
  AND d.diary_date = ?
  AND d.deleted_at IS NULL;
```

### 5. 감정 통계 (최근 30일)

```sql
SELECT 
    d.core_emotion,
    f.flower_name,
    f.is_positive,
    COUNT(*) as count,
    MAX(d.diary_date) as last_date
FROM diaries d
LEFT JOIN flowers f ON d.core_emotion = f.emotion
WHERE d.user_id = ?
  AND d.deleted_at IS NULL
  AND d.diary_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY d.core_emotion, f.flower_name, f.is_positive
ORDER BY count DESC;
```

### 6. 월별 감정 트렌드

```sql
SELECT 
    DATE_FORMAT(diary_date, '%Y-%m') as year_month,
    core_emotion,
    COUNT(*) as count
FROM diaries
WHERE user_id = ?
  AND deleted_at IS NULL
  AND diary_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
GROUP BY year_month, core_emotion
ORDER BY year_month DESC;
```

---

## View 정의

### v_user_recent_diaries
사용자별 최근 일기 목록 (꽃 정보 포함)

```sql
CREATE OR REPLACE VIEW v_user_recent_diaries AS
SELECT 
    d.diary_id,
    d.user_id,
    d.diary_date,
    d.content,
    d.summary,
    d.core_emotion,
    d.flower_name,
    d.flower_meaning,
    d.created_at,
    f.image_file_3d,
    f.image_file_realistic,
    f.is_positive
FROM diaries d
LEFT JOIN flowers f ON d.core_emotion = f.emotion
WHERE d.deleted_at IS NULL
ORDER BY d.diary_date DESC;
```

### v_user_emotion_stats_30d
사용자별 감정 통계 (최근 30일)

```sql
CREATE OR REPLACE VIEW v_user_emotion_stats_30d AS
SELECT 
    d.user_id,
    d.core_emotion,
    f.flower_name,
    f.is_positive,
    COUNT(*) as count,
    MAX(d.diary_date) as last_date
FROM diaries d
LEFT JOIN flowers f ON d.core_emotion = f.emotion
WHERE d.deleted_at IS NULL
  AND d.diary_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY d.user_id, d.core_emotion, f.flower_name, f.is_positive;
```

---

## 성능 최적화

### 1. 인덱스 전략
- **복합 인덱스**: (user_id, diary_date) - 가장 빈번한 조회 패턴
- **커버링 인덱스**: 목록 조회 시 INDEX ONLY SCAN 가능
- **JSON 인덱스**: MariaDB 10.5+ 지원, emotions_json 검색 시 활용

### 2. 파티셔닝 (대용량 데이터 시)
```sql
-- diary_date 기준 월별 파티셔닝
ALTER TABLE diaries 
PARTITION BY RANGE (TO_DAYS(diary_date)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    ...
);
```

### 3. 캐싱 전략
- **Redis 캐싱**:
    - flowers 테이블 전체 (변경 거의 없음)
    - 최근 일기 목록 (TTL: 5분)
    - 감정 통계 (TTL: 1시간)

### 4. 읽기/쓰기 분리
- **Master**: 일기 작성, 수정, 삭제
- **Slave**: 일기 조회, 통계

---

## JPA Entity 예시

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
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, length = 50)
    private String nickname;
    
    @Column
    private String profileImage;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Diary> diaries = new ArrayList<>();
    
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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDate diaryDate;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(length = 20)
    private String coreEmotion;
    
    @Column(columnDefinition = "TEXT")
    private String emotionReason;
    
    @Column(length = 50)
    private String flowerName;
    
    @Column(length = 100)
    private String flowerMeaning;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "JSON")
    private List<EmotionPercent> emotionsJson;
    
    @Column(nullable = false)
    private Boolean isAnalyzed = false;
    
    private LocalDateTime analyzedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime deletedAt;
}
```

### Flower Entity
```java
@Entity
@Table(name = "flowers")
public class Flower {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer flowerId;
    
    @Column(unique = true, nullable = false, length = 20)
    private String emotion;
    
    @Column(nullable = false, length = 50)
    private String flowerName;
    
    @Column(nullable = false, length = 100)
    private String flowerMeaning;
    
    @Column(nullable = false, length = 100)
    private String imageFile3d;
    
    @Column(nullable = false, length = 100)
    private String imageFileRealistic;
    
    @Column(nullable = false)
    private Boolean isPositive;
    
    @Column(nullable = false)
    private Integer displayOrder;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

## 마이그레이션 전략

### 1. 초기 설치 (V1.0)
```sql
-- schema.sql 실행
-- 초기 flowers 데이터 INSERT
```

### 2. 향후 변경사항 예시

#### V1.1: 일기 공개 기능 추가
```sql
ALTER TABLE diaries 
ADD COLUMN is_public BOOLEAN DEFAULT FALSE AFTER analyzed_at,
ADD COLUMN published_at DATETIME NULL AFTER is_public,
ADD INDEX idx_public (is_public, published_at);
```

#### V1.2: 태그 기능 추가
```sql
CREATE TABLE tags (
    tag_id INT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE diary_tags (
    diary_id BIGINT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (diary_id, tag_id),
    FOREIGN KEY (diary_id) REFERENCES diaries(diary_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);
```

---

## 백업 전략

### 1. 일일 백업
```bash
# 전체 백업
mysqldump -u root -p --single-transaction flowerbed > backup_$(date +%Y%m%d).sql

# 테이블별 백업
mysqldump -u root -p --single-transaction flowerbed diaries > diaries_$(date +%Y%m%d).sql
```

### 2. 증분 백업
```sql
-- Binary Log 활성화
SET GLOBAL binlog_format = 'ROW';
```

### 3. 복구 테스트
- 매월 1회 백업 파일로 테스트 DB 복구 테스트

---

## 보안 고려사항

### 1. 비밀번호 암호화
- BCrypt, SCrypt 등 단방향 해시 사용
- Salt 적용

### 2. SQL Injection 방어
- PreparedStatement 사용 (JPA 자동 처리)
- 사용자 입력 검증

### 3. 접근 권한 관리
```sql
-- 애플리케이션 전용 계정 생성
CREATE USER 'flowerbed_app'@'%' IDENTIFIED BY 'strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON flowerbed.* TO 'flowerbed_app'@'%';
FLUSH PRIVILEGES;
```

### 4. 데이터 암호화
- 민감 정보(이메일, 일기 내용) 암호화 고려
- TDE (Transparent Data Encryption) 활용

---

## 모니터링 지표

### 1. 쿼리 성능
```sql
-- Slow Query Log 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;  -- 1초 이상 쿼리 로깅
```

### 2. 테이블 크기 모니터링
```sql
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS "Size (MB)"
FROM information_schema.TABLES
WHERE table_schema = 'flowerbed'
ORDER BY (data_length + index_length) DESC;
```

### 3. 인덱스 사용률
```sql
SHOW INDEX FROM diaries;
```

---

## FAQ

### Q1. 왜 emotions_json을 별도 테이블로 분리하지 않았나요?
A. 초기 단계에서는 JSON으로 충분합니다. 감정별 통계가 복잡해지면 그때 정규화하면 됩니다.

### Q2. 하루에 여러 일기를 쓰고 싶으면?
A. `uk_user_date` 제약조건을 제거하고, diary_datetime으로 변경하면 됩니다.

### Q3. 꽃 테이블 없이 Enum만 써도 되지 않나요?
A. 가능하지만, 꽃말이나 이미지 변경 시 재배포가 필요합니다. DB로 관리하면 유연합니다.

### Q4. deleted_at이 NULL이 아닌 경우는?
A. Soft Delete로, 실제 데이터는 남아있지만 논리적으로 삭제된 상태입니다.