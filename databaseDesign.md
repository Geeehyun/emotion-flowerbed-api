# 데이터베이스 설계 문서

## 1. users (회원)

사용자 계정 정보를 관리하는 테이블입니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| user_id | bigint(20) | PK, AUTO_INCREMENT | 회원 고유 ID |
| email | varchar(255) | NOT NULL, UNIQUE | 이메일 (로그인 ID) |
| password | varchar(255) | NOT NULL | 암호화된 비밀번호 |
| nickname | varchar(50) | NOT NULL | 닉네임 |
| profile_image | varchar(255) | NULL | 프로필 이미지 URL |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 가입일시 |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| deleted_at | datetime | NULL | 탈퇴일시 (Soft Delete) |

### 인덱스
- PRIMARY KEY: `user_id`
- UNIQUE KEY: `email`
- INDEX: `idx_email` (이메일 조회 최적화)
- INDEX: `idx_created_at` (가입일 기준 조회 최적화)

### DDL
```mysql
-- users DDL
CREATE TABLE `users` (
`user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
`email` varchar(255) NOT NULL COMMENT '이메일 (로그인 ID)',
`password` varchar(255) NOT NULL COMMENT '암호화된 비밀번호',
`nickname` varchar(50) NOT NULL COMMENT '닉네임',
`profile_image` varchar(255) DEFAULT NULL COMMENT '프로필 이미지 URL',
`created_at` datetime DEFAULT current_timestamp() COMMENT '가입일시',
`updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
`deleted_at` datetime DEFAULT NULL COMMENT '탈퇴일시 (soft delete)',
PRIMARY KEY (`user_id`),
UNIQUE KEY `email` (`email`),
KEY `idx_email` (`email`),
KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원'
;
```

### JPA Entity
```java
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@Where(clause = "deleted_at IS NULL")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diary> diaries = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

---

## 2. diaries (일기)

사용자가 작성한 일기와 AI 감정 분석 결과를 저장하는 테이블입니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| diary_id | bigint(20) | PK, AUTO_INCREMENT | 일기 고유 ID |
| user_id | bigint(20) | FK, NOT NULL | 작성자 ID (users.user_id) |
| diary_date | date | NOT NULL | 일기 날짜 |
| content | text | NOT NULL | 일기 내용 |
| summary | text | NULL | AI 생성 일기 요약 |
| core_emotion | varchar(20) | NULL | 대표 감정 (한글명) |
| core_emotion_code | varchar(20) | NULL | 대표 감정 코드 (영문) |
| emotion_reason | text | NULL | 대표 감정 선택 이유 |
| flower_name | varchar(50) | NULL | 감정에 매칭되는 꽃 이름 |
| flower_meaning | varchar(100) | NULL | 꽃말 |
| emotions_json | longtext | NULL | 전체 감정 배열 JSON |
| is_analyzed | tinyint(1) | DEFAULT 0 | 감정 분석 완료 여부 |
| analyzed_at | datetime | NULL | 분석 완료 일시 |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 작성일시 |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| deleted_at | datetime | NULL | 삭제일시 (Soft Delete) |
| is_active | tinyint(1) | GENERATED COLUMN | 활성 상태 (deleted_at IS NULL) |

### 제약조건
- **FOREIGN KEY**: `user_id` → `users.user_id` (ON DELETE CASCADE)
- **UNIQUE KEY**: `uk_user_date_active` (user_id, diary_date, is_active)
  - 같은 사용자가 같은 날짜에 여러 일기를 작성할 수 없도록 제한
  - is_active를 포함하여 soft delete된 일기는 제약에서 제외

### 인덱스
- PRIMARY KEY: `diary_id`
- UNIQUE KEY: `uk_user_date_active` (user_id, diary_date, is_active)
- INDEX: `idx_user_date` (사용자별 날짜 조회)
- INDEX: `idx_user_created` (사용자별 최신순 조회)
- INDEX: `idx_core_emotion` (감정별 조회)
- INDEX: `idx_analyzed` (분석 상태별 조회)

### emotions_json 구조
```json
[
  {"emotion": "기쁨", "percent": 40},
  {"emotion": "설렘", "percent": 30},
  {"emotion": "평온", "percent": 20},
  {"emotion": "감사", "percent": 10}
]
```

### DDL
```mysql
-- diaries DDL
CREATE TABLE `diaries` (
  `diary_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '일기 ID',
  `user_id` bigint(20) NOT NULL COMMENT '작성자 ID',
  `diary_date` date NOT NULL COMMENT '일기 날짜',
  `content` text NOT NULL COMMENT '일기 내용',
  `summary` text DEFAULT NULL COMMENT '일기 요약',
  `core_emotion` varchar(20) DEFAULT NULL COMMENT '대표 감정',
  `core_emotion_code` varchar(20) DEFAULT NULL,
  `emotion_reason` text DEFAULT NULL COMMENT '대표 감정 선택 이유',
  `flower_name` varchar(50) DEFAULT NULL COMMENT '꽃 이름',
  `flower_meaning` varchar(100) DEFAULT NULL COMMENT '꽃말',
  `emotions_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '전체 감정 배열 JSON' CHECK (json_valid(`emotions_json`)),
  `is_analyzed` tinyint(1) DEFAULT 0 COMMENT '분석 완료 여부',
  `analyzed_at` datetime DEFAULT NULL COMMENT '분석 완료 일시',
  `created_at` datetime DEFAULT current_timestamp() COMMENT '작성일시',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
  `deleted_at` datetime DEFAULT NULL COMMENT '삭제일시 (soft delete)',
  `is_active` tinyint(1) GENERATED ALWAYS AS (case when `deleted_at` is null then 1 else NULL end) STORED,
  PRIMARY KEY (`diary_id`),
  UNIQUE KEY `uk_user_date_active` (`user_id`,`diary_date`,`is_active`),
  KEY `idx_user_date` (`user_id`,`diary_date`),
  KEY `idx_user_created` (`user_id`,`created_at` DESC),
  KEY `idx_core_emotion` (`core_emotion`),
  KEY `idx_analyzed` (`is_analyzed`,`analyzed_at`),
  CONSTRAINT `diaries_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일기'
```

### JPA Entity
```java
@Entity
@Table(name = "diaries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_date", columnNames = {"user_id", "diary_date"})
})
@SQLDelete(sql = "UPDATE diaries SET deleted_at = NOW() WHERE diary_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Diary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "core_emotion", length = 20)
    private String coreEmotion;

    @Column(name = "core_emotion_code", length = 20)
    private String coreEmotionCode;

    @Column(name = "emotion_reason", columnDefinition = "TEXT")
    private String emotionReason;

    @Column(name = "flower_name", length = 50)
    private String flowerName;

    @Column(name = "flower_meaning", length = 100)
    private String flowerMeaning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emotions_json", columnDefinition = "LONGTEXT")
    private List<EmotionPercent> emotionsJson;

    @Column(name = "is_analyzed", nullable = false)
    private Boolean isAnalyzed = false;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

---

## 3. emotions (감정-꽃 매핑)

감정과 꽃을 매핑하는 마스터 데이터 테이블입니다. 각 감정별로 대응되는 꽃과 꽃말, 상세 정보를 저장합니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| emotion_code | varchar(20) | PK | 감정 코드 (영문, 예: JOY, EXCITEMENT) |
| emotion_name_kr | varchar(20) | NOT NULL | 감정명 (한글, 예: 기쁨, 설렘) |
| emotion_name_en | varchar(20) | NOT NULL | 감정명 (영문) |
| flower_name_kr | varchar(50) | NOT NULL | 꽃 이름 (한글) |
| flower_name_en | varchar(50) | NULL | 꽃 이름 (영문) |
| flower_meaning | varchar(100) | NOT NULL | 꽃말 |
| flower_meaning_story | varchar(1000) | NULL | 꽃말 유래 |
| flower_color | varchar(50) | NULL | 꽃 색상 텍스트 (예: 연한 분홍색) |
| flower_color_codes | varchar(500) | NULL | 꽃 색상 HEX 코드 (,로 구분) |
| flower_origin | varchar(100) | NULL | 꽃 원산지 |
| flower_fragrance | varchar(50) | NULL | 꽃 향기 특성 |
| flower_fun_fact | varchar(1000) | NULL | 꽃 관련 흥미로운 이야기 |
| image_file_3d | varchar(100) | NOT NULL | 3D 이미지 파일명 |
| image_file_realistic | varchar(100) | NOT NULL | 실사 이미지 파일명 |
| is_positive | tinyint(1) | NOT NULL | 긍정 감정 여부 (1: 긍정, 0: 부정) |
| display_order | int(11) | NOT NULL | 화면 표시 순서 |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |

### 인덱스
- PRIMARY KEY: `emotion_code`
- INDEX: `idx_display_order` (순서 기반 조회)

### 데이터 예시
```sql
INSERT INTO emotions (emotion_code, emotion_name_kr, emotion_name_en,
                      flower_name_kr, flower_meaning,
                      image_file_3d, image_file_realistic,
                      is_positive, display_order)
VALUES
('JOY', '기쁨', 'Joy', '해바라기', '당신만을 바라봅니다',
 'sunflower_3d.png', 'sunflower_real.jpg', 1, 1),
('EXCITEMENT', '설렘', 'Excitement', '튤립', '사랑의 고백',
 'tulip_3d.png', 'tulip_real.jpg', 1, 2);
```

### DDL
```mysql
CREATE TABLE `emotions` (
  `emotion_code` varchar(20) NOT NULL COMMENT '감정 코드 (영문)',
  `emotion_name_kr` varchar(20) NOT NULL COMMENT '감정명 (한글)',
  `emotion_name_en` varchar(20) NOT NULL COMMENT '감정명 (영문)',
  `flower_name_kr` varchar(50) NOT NULL COMMENT '꽃 이름',
  `flower_name_en` varchar(50) DEFAULT NULL,
  `flower_meaning` varchar(100) NOT NULL COMMENT '꽃말',
  `flower_meaning_story` varchar(1000) DEFAULT NULL COMMENT '꽃말 유래',
  `flower_color` varchar(50) DEFAULT NULL COMMENT '꽃 색상 텍스트',
  `flower_color_codes` varchar(500) DEFAULT NULL COMMENT '꽃 색상 코드 (, 로 구분)',
  `flower_origin` varchar(100) DEFAULT NULL COMMENT '꽃 원산지',
  `flower_fragrance` varchar(50) DEFAULT NULL COMMENT '꽃 향기',
  `flower_fun_fact` varchar(1000) DEFAULT NULL COMMENT '꽃과 관련된 재밌는 이야기',
  `image_file_3d` varchar(100) NOT NULL COMMENT '3D 이미지',
  `image_file_realistic` varchar(100) NOT NULL COMMENT '실사 이미지',
  `is_positive` tinyint(1) NOT NULL COMMENT '긍정 여부',
  `display_order` int(11) NOT NULL COMMENT '표시 순서',
  `created_at` datetime DEFAULT current_timestamp(),
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`emotion_code`),
  KEY `idx_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

### JPA Entity
```java
@Entity
@Table(name = "emotions")
public class Emotion {
    @Id
    @Column(name = "emotion_code", length = 20)
    private String emotionCode;

    @Column(name = "emotion_name_kr", nullable = false, length = 20)
    private String emotionNameKr;

    @Column(name = "emotion_name_en", nullable = false, length = 20)
    private String emotionNameEn;

    @Column(name = "flower_name_kr", nullable = false, length = 50)
    private String flowerNameKr;

    @Column(name = "flower_name_en", length = 50)
    private String flowerNameEn;

    @Column(name = "flower_meaning", nullable = false, length = 100)
    private String flowerMeaning;

    @Column(name = "flower_meaning_story", length = 1000)
    private String flowerMeaningStory;

    @Column(name = "flower_color", length = 50)
    private String flowerColor;

    @Column(name = "flower_color_codes", length = 500)
    private String flowerColorCodes;

    @Column(name = "flower_origin", length = 100)
    private String flowerOrigin;

    @Column(name = "flower_fragrance", length = 50)
    private String flowerFragrance;

    @Column(name = "flower_fun_fact", length = 1000)
    private String flowerFunFact;

    @Column(name = "image_file_3d", nullable = false, length = 100)
    private String imageFile3d;

    @Column(name = "image_file_realistic", nullable = false, length = 100)
    private String imageFileRealistic;

    @Column(name = "is_positive", nullable = false)
    private Boolean isPositive;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

## 테이블 관계도

```
users (1) ----< (N) diaries
               |
               | (조회)
               v
           emotions (마스터 데이터)
```

### 관계 설명
1. **users ↔ diaries**: 1:N 관계
   - 한 사용자는 여러 일기를 작성할 수 있음
   - FK: `diaries.user_id` → `users.user_id` (ON DELETE CASCADE)

2. **diaries ↔ emotions**: 참조 관계 (FK 없음)
   - 일기의 `core_emotion_code`가 `emotions.emotion_code`를 참조
   - 일기의 `flower_name`이 `emotions.flower_name_kr`를 참조
   - 마스터 데이터이므로 FK 제약조건 없이 조회만 수행

---

## 주요 비즈니스 로직

### 1. 일기 작성 플로우
1. 사용자가 일기 내용 작성
2. 일기 저장 (`is_analyzed = false`)
3. Claude API 호출하여 감정 분석
4. 분석 결과를 일기에 업데이트
   - `summary`, `core_emotion`, `core_emotion_code`
   - `flower_name`, `flower_meaning`
   - `emotions_json`, `is_analyzed = true`

### 2. 월별 일기 조회
- 특정 연월의 모든 일기 조회
- 각 일기의 `flower_name`으로 `emotions` 테이블 조인
- 꽃 상세 정보 포함하여 응답

### 3. Soft Delete 정책
- 모든 테이블에서 `deleted_at`을 사용한 Soft Delete 적용
- JPA `@SQLDelete`, `@Where` 애노테이션 활용
- 실제 데이터는 삭제되지 않고 `deleted_at`에 삭제 시각 기록
