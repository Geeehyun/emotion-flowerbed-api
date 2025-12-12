# 일기 감정 분석 API - Emotion Flowerbed

AI 기반 일기 감정 분석 및 꽃 매칭 서비스

## 기술 스택

- **Java 21**
- **Spring Boot 3.3.5**
- **Spring Data JPA**
- **MariaDB 10.x**
- **Claude API (Anthropic Haiku)**
- **Lombok**
- **Swagger (SpringDoc OpenAPI)**

---

## 주요 기능

### 1. 일기 관리
- 일기 작성 (하루 1개 제한)
- 일기 수정 / 삭제 (Soft Delete)
- 특정 날짜 일기 조회
- 월별 일기 목록 조회

### 2. AI 감정 분석
- Claude Haiku 모델 사용 (비용 최적화)
- 20개 감정 분류 체계
- 감정별 백분율 분석
- 감정에 맞는 꽃 & 꽃말 매칭

### 3. 꽃 정보 제공
- 월별 일기 조회 시 꽃 상세정보 포함
- 감정 코드별 꽃 데이터 (한글/영문 이름, 색상, 원산지, 향기, 재밌는 이야기 등)
- 사용자의 감정&꽃 통계

---

## 데이터베이스 설계

### 테이블 구조

```
users (회원)
├─ user_id (PK)
├─ email (UNIQUE)
├─ password
├─ nickname
└─ deleted_at (Soft Delete)

diaries (일기)
├─ diary_id (PK)
├─ user_id (FK)
├─ diary_date (UNIQUE per user)
├─ content
├─ summary (AI 생성)
├─ core_emotion (한글)
├─ core_emotion_code (영문)
├─ flower_name
├─ flower_meaning
├─ emotions_json (JSON 배열)
├─ is_analyzed
└─ deleted_at (Soft Delete)

emotions (감정-꽃 마스터)
├─ emotion_code (PK, 영문)
├─ emotion_name_kr
├─ emotion_name_en
├─ flower_name_kr
├─ flower_name_en
├─ flower_meaning
├─ flower_meaning_story
├─ flower_color_codes
├─ flower_fragrance
├─ flower_fun_fact
├─ image_file_3d
└─ image_file_realistic
```

자세한 설계 문서는 [databaseDesign.md](./databaseDesign.md)를 참조하세요.

---

## API 엔드포인트

### Diary API

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/diaries` | 일기 작성 |
| POST | `/diaries/{id}/analyze` | 일기 감정 분석 (Claude API) |
| POST | `/diaries/{id}/analyze-test` | 일기 감정 분석 (테스트 모드, 랜덤) |
| GET | `/diaries/{id}` | 일기 상세 조회 |
| GET | `/diaries/date/{date}` | 특정 날짜 일기 조회 |
| GET | `/diaries?yearMonth=YYYY-MM` | 월별 일기 목록 조회 (꽃 상세정보 포함) |
| PUT | `/diaries/{id}` | 일기 수정 |
| DELETE | `/diaries/{id}` | 일기 삭제 (Soft Delete) |

**참고**: 현재는 회원가입/로그인 기능이 없어 모든 요청이 userId=1로 처리됩니다.

### Flower API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/flowers/my-emotions` | 사용자의 감정&꽃 통계 (날짜 목록 + 꽃 상세정보 포함) |
| GET | `/flowers/all-emotions` | 전체 감정-꽃 정보 조회 (display_order 순) |

---

## 주요 기능 설명

### 1. 일기 작성
```json
POST /api/diaries
Header: X-User-Id: 1

{
  "diaryDate": "2025-12-08",
  "content": "오늘 친구와 맛있는 저녁을 먹었다..."
}
```

**유효성 검사**:
- 최소 10자 이상
- 최대 5000자 이하
- 하루 1개 일기만 작성 가능

### 2. 감정 분석
```json
POST /api/diaries/1/analyze
Header: X-User-Id: 1

Response:
{
  "diaryId": 1,
  "summary": "친구와 저녁을 먹으며 즐거운 시간을 보냄",
  "coreEmotion": "기쁨",
  "coreEmotionCode": "JOY",
  "emotionReason": "친구와의 즐거운 시간이 강조됨",
  "flowerName": "해바라기",
  "flowerMeaning": "당신을 보면 행복해요",
  "emotions": [
    {"emotion": "기쁨", "percent": 70},
    {"emotion": "행복", "percent": 30}
  ],
  "isAnalyzed": true,
  "analyzedAt": "2025-12-08T10:30:00"
}
```

**비용 최적화**:
- Claude Haiku 모델 사용 (Sonnet 대비 20배 저렴)
- max_tokens: 500으로 제한
- temperature: 0.3 (일관된 결과)

### 3. 월별 일기 목록 (꽃 상세정보 포함)
```json
GET /api/diaries?yearMonth=2025-12
Header: X-User-Id: 1

Response:
{
  "yearMonth": "2025-12",
  "diaries": [
    {
      "id": 1,
      "date": "2025-12-08",
      "content": "오늘 친구와...",
      "coreEmotion": "기쁨",
      "flower": "해바라기",
      "floriography": "당신을 보면 행복해요",
      "summary": "친구와 저녁을...",
      "emotions": [
        {"emotion": "JOY", "percent": 70}
      ],
      "reason": "친구와의 즐거운 시간...",
      "flowerDetail": {
        "emotionCode": "JOY",
        "emotionNameKr": "기쁨",
        "emotionNameEn": "Joy",
        "flowerNameKr": "해바라기",
        "flowerNameEn": "Sunflower",
        "flowerMeaning": "당신을 보면 행복해요",
        "flowerMeaningStory": "해바라기는 해를 따라 고개를 돌리는 특성이 있어...",
        "flowerColor": "노란색",
        "flowerColorCodes": "#FFD700,#FFA500",
        "flowerOrigin": "북아메리카",
        "flowerFragrance": "은은한 풀향기",
        "flowerFunFact": "해바라기는 하루에 약 2리터의 물을 흡수합니다",
        "imageFile3d": "sunflower_3d.png",
        "imageFileRealistic": "sunflower_real.jpg",
        "isPositive": true
      }
    }
  ],
  "totalCount": 15,
  "hasNextMonth": true,
  "hasPrevMonth": true
}
```

---

## 프로젝트 구조

```
emotion-flowerbed-api/
├── src/main/java/com/flowerbed/
│   ├── config/
│   │   ├── AnthropicConfig.java          # Claude API 설정
│   │   ├── JpaConfig.java                # JPA Auditing 설정
│   │   ├── SwaggerConfig.java            # Swagger 설정
│   │   └── WebConfig.java                # CORS 설정 (application.yml 기반)
│   ├── controller/
│   │   ├── DiaryController.java          # 일기 API
│   │   └── FlowerController.java         # 꽃 정보 API
│   ├── service/
│   │   ├── DiaryService.java             # 일기 비즈니스 로직
│   │   ├── DiaryEmotionService.java      # 감정 분석 (Claude API)
│   │   ├── DiaryEmotionTestService.java  # 테스트 모드 (랜덤)
│   │   ├── FlowerService.java            # 꽃 정보 서비스
│   │   └── ClaudeApiClient.java          # Claude API 클라이언트
│   ├── domain/
│   │   ├── User.java                     # 회원 엔티티
│   │   ├── Diary.java                    # 일기 엔티티
│   │   └── Emotion.java                  # 감정-꽃 엔티티 (테이블명: emotions)
│   ├── dto/
│   │   ├── DiaryCreateRequest.java
│   │   ├── DiaryUpdateRequest.java
│   │   ├── DiaryResponse.java
│   │   ├── DiaryEmotionResponse.java
│   │   ├── MonthlyDiariesResponse.java
│   │   ├── AllEmotionsResponse.java      # 전체 감정-꽃 정보 응답
│   │   ├── UserEmotionFlowerResponse.java
│   │   └── EmotionPercent.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── DiaryRepository.java
│   │   └── FlowerRepository.java
│   └── exception/
│       ├── ErrorCode.java
│       ├── BusinessException.java
│       ├── DiaryNotFoundException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml                   # 기본 설정 (CORS, DB, API 기본값)
│   ├── application-local.yml.example     # 로컬 설정 템플릿 (Git 포함)
│   ├── application-local.yml             # 로컬 환경 설정 (gitignored)
│   └── prompts/
│       └── emotion-analysis-prompt.txt   # AI 프롬프트 템플릿
├── databaseDesign.md                     # 데이터베이스 설계 문서
├── api-test.http                         # API 테스트 파일 (REST Client)
├── README.md                             # 프로젝트 문서
├── .gitignore                            # Git 제외 파일 목록
└── build.gradle                          # Gradle 빌드 설정
```

---

## 환경 설정

### 1. 데이터베이스 생성
```sql
CREATE DATABASE flowerbed CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'flowerbed-api'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON flowerbed.* TO 'flowerbed-api'@'%';
FLUSH PRIVILEGES;
```

### 2. 테이블 생성
DDL은 [databaseDesign.md](./databaseDesign.md) 참조

### 3. application-local.yml 생성

**템플릿 파일 복사**:
```bash
# Windows (PowerShell)
Copy-Item src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# Mac/Linux
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

**설정 파일 수정** (`src/main/resources/application-local.yml`):
```yaml
spring:
  datasource:
    username: 'flowerbed-api'
    password: your_password_here  # 실제 DB 비밀번호 입력

cors:
  allowed-origins: http://localhost:3000,http://localhost:8080  # 필요시 추가

anthropic:
  api:
    key: sk-ant-api03-xxxxx  # Anthropic API Key 입력 (https://console.anthropic.com/)
```

**중요**:
- `application-local.yml`은 `.gitignore`에 포함되어 Git에 커밋되지 않습니다.
- `application-local.yml.example`은 템플릿 파일로 Git에 포함됩니다.

### 4. Anthropic API Key 발급
1. https://console.anthropic.com/ 접속
2. Settings → API Keys → Create Key
3. 발급받은 키를 `application-local.yml`에 입력

### 5. 애플리케이션 실행
```bash
# Gradle Wrapper 사용
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

### 6. API 테스트

**Swagger UI**:
```
http://localhost:8080/api/swagger-ui.html
```

**.http 파일** (IntelliJ IDEA, VS Code REST Client):
```
api-test.http 파일 참조
```

---

## 주요 설정

### JPA 설정
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 테이블 자동 수정 방지
```

**ddl-auto 옵션**:
- `validate`: 엔티티와 테이블 구조 일치 여부만 검증 (권장)
- `update`: 자동으로 테이블 수정 (위험)
- `create`: 매번 테이블 재생성 (개발 초기에만)
- `none`: 아무것도 하지 않음

### Claude API 설정
```yaml
anthropic:
  api:
    key: ${ANTHROPIC_API_KEY}
    model: claude-3-5-haiku-20241022  # 비용 최적화
    max-tokens: 500
    temperature: 0.3
```

---

## 에러 코드

| HTTP Status | Error Code | 설명 |
|-------------|------------|------|
| 400 | INVALID_INPUT | 입력 값 검증 실패 |
| 400 | INVALID_DIARY_CONTENT | 일기 내용이 분석 불가능 (10자 미만 또는 5000자 초과) |
| 400 | DUPLICATE_DIARY_DATE | 해당 날짜에 이미 일기가 존재 |
| 404 | DIARY_NOT_FOUND | 일기를 찾을 수 없음 |
| 404 | DIARY_NOT_ANALYZED | 일기가 아직 분석되지 않음 |
| 404 | USER_NOT_FOUND | 사용자를 찾을 수 없음 |
| 404 | FLOWER_NOT_FOUND | 꽃 정보를 찾을 수 없음 |
| 500 | LLM_ANALYSIS_FAILED | AI 분석 실패 |
| 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류 |

---

## 개발 현황

### ✅ 완료된 기능
- [x] Spring Boot 프로젝트 초기 설정
- [x] MariaDB 연동 및 Entity 설계
- [x] 일기 CRUD API 구현
- [x] Claude API 감정 분석 (실제 + 테스트 모드)
- [x] 월별 일기 목록 조회 (꽃 상세정보 포함)
- [x] 감정&꽃 통계 API
- [x] 예외 처리 및 에러 핸들링
- [x] Swagger UI 설정
- [x] 유효성 검사 (최소/최대 길이)
- [x] Soft Delete 구현

### 📝 추후 개선 사항

**인프라 & 보안**
- [ ] 사용자 인증/인가 (Spring Security + JWT)
- [ ] Rate Limiting (API 호출 제한)
- [ ] 감정 통계 시각화 데이터

**신규 기능**
- [ ] 감정 관리 미션 시스템
  - 감정 패턴 분석 (예: 부정 감정 3일 연속 시 자동 감지)
  - 감정별 맞춤 관리 미션 자동 생성 및 추천
  - 미션 진행 상태 추적 (진행중, 완료, 포기)
  - 미션 완료 시 리워드 시스템
- [ ] 감정 미션 활동 로그
  - 과거 미션 이행 기록 조회
  - 미션별 성공/실패 통계
  - 미션 달성률 및 성과 분석
- [ ] 주간 감정 분석 레터
  - 주간 감정 패턴 분석 및 요약
  - AI 기반 감정 인사이트 제공


---

## 참고 문서

- [Database 설계 문서](./databaseDesign.md)
- [Claude API 문서](https://docs.anthropic.com/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)

---