# 데이터베이스 설계 문서

## 1. users (회원)

사용자 계정 정보를 관리하는 테이블입니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| user_sn | bigint(20) | PK, AUTO_INCREMENT | 회원 일련번호 (내부 ID) |
| user_id | varchar(255) | NOT NULL, UNIQUE | 로그인 ID |
| password | varchar(255) | NOT NULL | 암호화된 비밀번호 |
| name | varchar(50) | NOT NULL | 이름 |
| user_type_cd | varchar(50) | NULL | 사용자 유형 코드 (codes.USER_TYPE) |
| school_code | varchar(50) | NULL | 학교 코드 |
| school_nm | varchar(100) | NULL | 학교명 |
| class_code | varchar(50) | NULL | 반 코드 |
| emotion_control_cd | varchar(50) | NULL | 감정 제어 활동 코드 (codes.EMOTION_CONTROL) |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 가입일시 |
| created_by | varchar(255) | NULL | 생성자 사용자 ID |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| updated_by | varchar(255) | NULL | 수정자 사용자 ID |
| deleted_at | datetime | NULL | 탈퇴일시 (Soft Delete) |
| deleted_by | varchar(255) | NULL | 삭제자 사용자 ID |

### 인덱스
- PRIMARY KEY: `user_sn`
- UNIQUE KEY: `user_id`
- INDEX: `idx_user_id` (로그인 ID 조회 최적화)
- INDEX: `idx_created_at` (가입일 기준 조회 최적화)

### DDL
```mysql
-- users DDL
CREATE TABLE `users` (
`user_sn` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '회원 일련번호',
`user_id` varchar(255) NOT NULL COMMENT '로그인 ID',
`password` varchar(255) NOT NULL COMMENT '암호화된 비밀번호',
`name` varchar(50) NOT NULL COMMENT '이름',
`user_type_cd` varchar(50) DEFAULT NULL COMMENT '사용자 유형 코드',
`school_code` varchar(50) DEFAULT NULL COMMENT '학교 코드',
`school_nm` varchar(100) DEFAULT NULL COMMENT '학교명',
`class_code` varchar(50) DEFAULT NULL COMMENT '반 코드',
`emotion_control_cd` varchar(50) DEFAULT NULL COMMENT '감정 제어 활동 코드',
`created_at` datetime DEFAULT current_timestamp() COMMENT '가입일시',
`created_by` varchar(255) DEFAULT NULL COMMENT '생성자 사용자 ID',
`updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
`updated_by` varchar(255) DEFAULT NULL COMMENT '수정자 사용자 ID',
`deleted_at` datetime DEFAULT NULL COMMENT '탈퇴일시 (soft delete)',
`deleted_by` varchar(255) DEFAULT NULL COMMENT '삭제자 사용자 ID',
PRIMARY KEY (`user_sn`),
UNIQUE KEY `user_id` (`user_id`),
KEY `idx_user_id` (`user_id`),
KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원'
;
```

### JPA Entity
```java
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_sn = ?")
@Where(clause = "deleted_at IS NULL")
public class User extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_sn")
    private Long userSn;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "user_type_cd", length = 50)
    private String userTypeCd;

    @Column(name = "school_code", length = 50)
    private String schoolCode;

    @Column(name = "school_nm", length = 100)
    private String schoolNm;

    @Column(name = "class_code", length = 50)
    private String classCode;

    @Column(name = "emotion_control_cd", length = 50)
    private String emotionControlCd;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diary> diaries = new ArrayList<>();

    // Audit 정보 (created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)는
    // BaseAuditEntity에서 상속
}
```

---

## 2. diaries (일기)

사용자가 작성한 일기와 AI 감정 분석 결과를 저장하는 테이블입니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| diary_id | bigint(20) | PK, AUTO_INCREMENT | 일기 고유 ID |
| user_sn | bigint(20) | FK, NOT NULL | 작성자 일련번호 (users.user_sn) |
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
| created_by | varchar(255) | NULL | 생성자 사용자 ID |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| updated_by | varchar(255) | NULL | 수정자 사용자 ID |
| deleted_at | datetime | NULL | 삭제일시 (Soft Delete) |
| deleted_by | varchar(255) | NULL | 삭제자 사용자 ID |
| is_active | tinyint(1) | GENERATED COLUMN | 활성 상태 (deleted_at IS NULL) |

### 제약조건
- **FOREIGN KEY**: `user_sn` → `users.user_sn` (ON DELETE CASCADE)
- **UNIQUE KEY**: `uk_user_date_active` (user_sn, diary_date, is_active)
  - 같은 사용자가 같은 날짜에 여러 일기를 작성할 수 없도록 제한
  - is_active를 포함하여 soft delete된 일기는 제약에서 제외

### 인덱스
- PRIMARY KEY: `diary_id`
- UNIQUE KEY: `uk_user_date_active` (user_sn, diary_date, is_active)
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
  `user_sn` bigint(20) NOT NULL COMMENT '작성자 일련번호',
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
  `created_by` varchar(255) DEFAULT NULL COMMENT '생성자 사용자 ID',
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
  `updated_by` varchar(255) DEFAULT NULL COMMENT '수정자 사용자 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '삭제일시 (soft delete)',
  `deleted_by` varchar(255) DEFAULT NULL COMMENT '삭제자 사용자 ID',
  `is_active` tinyint(1) GENERATED ALWAYS AS (case when `deleted_at` is null then 1 else NULL end) STORED,
  PRIMARY KEY (`diary_id`),
  UNIQUE KEY `uk_user_date_active` (`user_sn`,`diary_date`,`is_active`),
  KEY `idx_user_date` (`user_sn`,`diary_date`),
  KEY `idx_user_created` (`user_sn`,`created_at` DESC),
  KEY `idx_core_emotion` (`core_emotion`),
  KEY `idx_analyzed` (`is_analyzed`,`analyzed_at`),
  CONSTRAINT `diaries_ibfk_1` FOREIGN KEY (`user_sn`) REFERENCES `users` (`user_sn`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일기'
```

### JPA Entity
```java
@Entity
@Table(name = "diaries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_date", columnNames = {"user_sn", "diary_date"})
})
@SQLDelete(sql = "UPDATE diaries SET deleted_at = NOW() WHERE diary_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Diary extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long diaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_sn", nullable = false)
    private User user;

    @Column(name = "diary_date", nullable = false)
    private LocalDate diaryDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String summary;

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

    // Audit 정보 (created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)는
    // BaseAuditEntity에서 상속
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
| area | varchar(10) | NOT NULL | 영역 (red/yellow/green/blue) |
| display_order | int(11) | NOT NULL | 화면 표시 순서 |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| created_by | varchar(255) | NULL | 생성자 사용자 ID |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| updated_by | varchar(255) | NULL | 수정자 사용자 ID |

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
create table emotions
(
    emotion_code         varchar(20)                          not null comment '감정 코드 (영문)'
        primary key,
    emotion_name_kr      varchar(20)                          not null comment '감정명 (한글)',
    emotion_name_en      varchar(20)                          not null comment '감정명 (영문)',
    flower_name_kr       varchar(50)                          not null comment '꽃 이름',
    flower_name_en       varchar(50)                          null,
    flower_meaning       varchar(100)                         not null comment '꽃말',
    flower_meaning_story varchar(1000)                        null comment '꽃말 유래',
    flower_color         varchar(50)                          null comment '꽃 색상 텍스트',
    flower_color_codes   varchar(500)                         null comment '꽃 색상 코드 (, 로 구분)',
    flower_origin        varchar(100)                         null comment '꽃 원산지',
    flower_fragrance     varchar(50)                          null comment '꽃 향기',
    flower_fun_fact      varchar(1000)                        null comment '꽃과 관련된 재밌는 이야기',
    image_file_3d        varchar(100)                         not null comment '3D 이미지',
    image_file_realistic varchar(100)                         not null comment '실사 이미지',
    area                 varchar(10)                          not null comment '영역 (red/yellow/green/blue)',
    display_order        int                                  not null comment '표시 순서',
    created_at           datetime default current_timestamp() null,
    created_by           varchar(255)                         null comment '생성자 사용자 ID',
    updated_at           datetime default current_timestamp() null on update current_timestamp(),
    updated_by           varchar(255)                         null comment '수정자 사용자 ID'
)
    comment '감종 - 꽃 마스터 테이블' collate = utf8mb4_unicode_ci;

create index idx_area
    on emotions (area);

create index idx_display_order
    on emotions (display_order);
```

### JPA Entity
```java
@Entity
@Table(name = "emotions")
public class Emotion extends BaseAuditEntity {
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

    @Column(name = "area", nullable = false, length = 10)
    private String area;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    // Audit 정보 (created_at, created_by, updated_at, updated_by)는
    // BaseAuditEntity에서 상속
    // emotions 테이블은 soft delete가 없으므로 deleted_at, deleted_by는 사용하지 않음
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
   - FK: `diaries.user_sn` → `users.user_sn` (ON DELETE CASCADE)

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

---

## 4. code_groups (코드 그룹)

공통 코드의 그룹을 관리하는 테이블입니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| group_code | varchar(50) | PK | 그룹 코드 |
| group_name | varchar(100) | NOT NULL | 그룹명 |
| description | varchar(500) | NULL | 설명 |
| is_editable | tinyint(1) | DEFAULT 1 | 수정 가능 여부 (시스템 코드는 0) |
| display_order | int(11) | NOT NULL | 표시 순서 |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| created_by | varchar(255) | NULL | 생성자 사용자 ID |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| updated_by | varchar(255) | NULL | 수정자 사용자 ID |

### 인덱스
- PRIMARY KEY: `group_code`
- INDEX: `idx_display_order` (순서 기반 조회)

### 데이터 예시
```sql
INSERT INTO code_groups (group_code, group_name, description, is_editable, display_order)
VALUES
('USER_TYPE', '사용자 유형', '사용자 유형 구분 (학생/교사/관리자)', 0, 1),
('EMOTION_CONTROL', '감정 제어 활동', '감정 조절을 위한 활동 유형', 1, 2);
```

### DDL
```mysql
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
```

### JPA Entity
```java
@Entity
@Table(name = "code_groups")
public class CodeGroup extends BaseAuditEntity {
    @Id
    @Column(name = "group_code", length = 50)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_editable", nullable = false)
    private Boolean isEditable = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "codeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Code> codes = new ArrayList<>();

    // Audit 정보는 BaseAuditEntity에서 상속
}
```

---

## 5. codes (코드)

실제 코드 데이터를 관리하는 테이블입니다.

### 테이블 구조

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|---------|------|
| code_id | bigint(20) | PK, AUTO_INCREMENT | 코드 ID |
| group_code | varchar(50) | FK, NOT NULL | 그룹 코드 (code_groups.group_code) |
| code | varchar(50) | NOT NULL | 코드값 |
| code_name | varchar(100) | NOT NULL | 코드명 |
| description | varchar(500) | NULL | 설명 |
| is_active | tinyint(1) | DEFAULT 1 | 활성 여부 |
| display_order | int(11) | NOT NULL | 표시 순서 |
| extra_value1 | varchar(200) | NULL | 확장 필드1 |
| extra_value2 | varchar(200) | NULL | 확장 필드2 |
| extra_value3 | varchar(200) | NULL | 확장 필드3 |
| created_at | datetime | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| created_by | varchar(255) | NULL | 생성자 사용자 ID |
| updated_at | datetime | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| updated_by | varchar(255) | NULL | 수정자 사용자 ID |
| deleted_at | datetime | NULL | 삭제일시 (Soft Delete) |
| deleted_by | varchar(255) | NULL | 삭제자 사용자 ID |

### 제약조건
- **FOREIGN KEY**: `group_code` → `code_groups.group_code`
- **UNIQUE KEY**: `uk_group_code` (group_code, code)

### 인덱스
- PRIMARY KEY: `code_id`
- UNIQUE KEY: `uk_group_code` (group_code, code)
- INDEX: `idx_group_code` (그룹별 조회)
- INDEX: `idx_is_active` (활성 상태 조회)
- INDEX: `idx_display_order` (순서 기반 조회)

### 데이터 예시
```sql
-- USER_TYPE 코드
INSERT INTO codes (group_code, code, code_name, description, is_active, display_order)
VALUES
('USER_TYPE', 'STUDENT', '학생', '일반 학생 사용자', 1, 1),
('USER_TYPE', 'TEACHER', '교사', '교사 사용자 (학생 관리 가능)', 1, 2),
('USER_TYPE', 'ADMIN', '관리자', '시스템 관리자 (모든 권한)', 1, 3);

-- EMOTION_CONTROL 코드 (extra_value 활용)
INSERT INTO codes (group_code, code, code_name, description, is_active, display_order,
                   extra_value1, extra_value2, extra_value3)
VALUES
('EMOTION_CONTROL', 'DEEP_BREATHING', '심호흡하기', '깊게 숨을 들이마시고 천천히 내쉬며 마음을 진정시킵니다',
 1, 1, '5', 'easy', 'mental'),
('EMOTION_CONTROL', 'WALK', '산책하기', '주변을 천천히 걸으며 생각을 정리합니다',
 1, 2, '15', 'easy', 'physical');
```

### extra_value 활용 예시 (EMOTION_CONTROL)
- `extra_value1`: 권장 소요 시간 (분)
- `extra_value2`: 난이도 (easy, medium, hard)
- `extra_value3`: 카테고리 (physical, mental, creative, social)

### DDL
```mysql
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
```

### JPA Entity
```java
@Entity
@Table(name = "codes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_group_code", columnNames = {"group_code", "code"})
})
@SQLDelete(sql = "UPDATE codes SET deleted_at = NOW() WHERE code_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Code extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long codeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_code", nullable = false)
    private CodeGroup codeGroup;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "code_name", nullable = false, length = 100)
    private String codeName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "extra_value1", length = 200)
    private String extraValue1;

    @Column(name = "extra_value2", length = 200)
    private String extraValue2;

    @Column(name = "extra_value3", length = 200)
    private String extraValue3;

    // Audit 정보는 BaseAuditEntity에서 상속
}
```

---

## 테이블 관계도 (업데이트)

```
users (1) ----< (N) diaries
  |            |
  |            | (조회)
  |            v
  |        emotions (마스터 데이터)
  |
  | (참조)
  v
codes (USER_TYPE)
  |
  | (FK)
  v
code_groups
```

### 관계 설명
1. **users ↔ diaries**: 1:N 관계
   - 한 사용자는 여러 일기를 작성할 수 있음
   - FK: `diaries.user_sn` → `users.user_sn` (ON DELETE CASCADE)

2. **users ↔ codes**: 참조 관계 (FK 없음)
   - 사용자의 `user_type_cd`가 `codes.code` (USER_TYPE 그룹)를 참조
   - 사용자의 `emotion_control_cd`가 `codes.code` (EMOTION_CONTROL 그룹)를 참조
   - 코드 테이블이므로 FK 제약조건 없이 조회만 수행
   - 예시:
     - user_type_cd = 'STUDENT' → codes.code = 'STUDENT' (group_code = 'USER_TYPE')
     - emotion_control_cd = 'DEEP_BREATHING' → codes.code = 'DEEP_BREATHING' (group_code = 'EMOTION_CONTROL')

3. **diaries ↔ emotions**: 참조 관계 (FK 없음)
   - 일기의 `core_emotion_code`가 `emotions.emotion_code`를 참조
   - 일기의 `flower_name`이 `emotions.flower_name_kr`를 참조
   - 마스터 데이터이므로 FK 제약조건 없이 조회만 수행

4. **code_groups ↔ codes**: 1:N 관계
   - 하나의 코드 그룹은 여러 코드를 가질 수 있음
   - FK: `codes.group_code` → `code_groups.group_code`
