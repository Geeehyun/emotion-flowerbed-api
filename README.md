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

### 1. 사용자 인증 (JWT)
- 로그인 / 로그아웃
- Access Token (1일 유효) + Refresh Token (1년 유효)
- Redis 기반 토큰 관리 및 블랙리스트

### 2. 일기 관리
- 일기 작성 (하루 1개 제한)
- 일기 수정 / 삭제 (Soft Delete)
- 특정 날짜 일기 조회
- 월별 일기 목록 조회

### 3. AI 감정 분석
- Claude Haiku 모델 사용 (비용 최적화)
- 20개 감정 분류 체계
- 감정별 백분율 분석
- 감정에 맞는 꽃 & 꽃말 매칭
- **감정 조절 팁 자동 제공** (연속 3일/5일 같은 영역 감정 시)

### 4. 꽃 정보 제공
- 월별 일기 조회 시 꽃 상세정보 포함
- 감정 코드별 꽃 데이터 (한글/영문 이름, 색상, 원산지, 향기, 재밌는 이야기 등)
- 사용자의 감정&꽃 통계

### 5. 주간 리포트
- 주간 감정 패턴 분석
- AI 기반 인사이트 제공

### 6. 공통 코드 관리
- 사용자 유형 코드 (학생/교사/관리자)
- 감정 제어 활동 코드
- 감정 조절 팁 코드 (영역별/일수별)

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

**Base URL**: `/api/v1`

**인증**: 로그인 제외한 모든 API는 `Authorization: Bearer {accessToken}` 헤더 필요

### API 개요

| 카테고리 | 설명 | 상세 문서 |
|---------|------|----------|
| 인증 | 로그인, 로그아웃, 토큰 갱신 (3개 API) | [auth.md](./docs/api/auth.md) |
| 학생 | 일기 CRUD, 감정 분석, 주간 리포트, 감정 통계 (13개 API) | [student.md](./docs/api/student.md) |
| 선생님 | 학생 관리, 감정 모니터링, 위험 학생 관리 (9개 API) | [teacher.md](./docs/api/teacher.md) |
| 공통 에러 | 모든 API 에러 코드 및 처리 가이드 | [error.md](./docs/api/error.md) |

> 💡 **상세한 API 명세는 [docs/api](./docs/api) 디렉토리를 참조하세요.**
> 각 API의 요청/응답 구조, 에러 응답, 사용 예시 등이 포함되어 있습니다.

---

## 주요 기능 설명

### 1. 로그인 (JWT 기반)
- Access Token (1일 유효) + Refresh Token (1년 유효) 발급
- Refresh Token Rotation 방식으로 보안 강화
- Redis 기반 토큰 관리 및 블랙리스트

> 상세 명세: [docs/api/auth.md](./docs/api/auth.md)

### 2. 일기 작성 및 감정 분석
- 하루 1개 일기 작성 가능 (10자 이상, 5000자 이하)
- AI 기반 감정 분석 (Claude Haiku 모델)
- 20개 감정 분류 체계 및 감정별 꽃 매칭
- 연속 3일/5일 같은 영역 감정 시 자동으로 감정 조절 팁 제공

**비용 최적화:**
- Claude Haiku 모델 사용 (Sonnet 대비 20배 저렴)
- 테스트 모드 제공 (랜덤 분석, API 호출 비용 없음)

> 상세 명세: [docs/api/student.md](./docs/api/student.md)

### 3. 주간 리포트
- 주간 감정 패턴 분석
- AI 기반 인사이트 제공 (학생용/선생님용)
- 감정 다양성 점수 및 대표 꽃 선정

> 상세 명세: [docs/api/student.md](./docs/api/student.md)

### 4. 선생님 기능
- 담당 학생 목록 및 감정 현황 조회
- 날짜별/월별 학급 감정 분포 조회
- 위험 학생 관리 (CAUTION/DANGER 상태)
- 학생별 주간 리포트 및 위험도 변화 이력

> 상세 명세: [docs/api/teacher.md](./docs/api/teacher.md)

---

## 프로젝트 구조

```
emotion-flowerbed-api/
├── src/main/java/com/flowerbed/
│   ├── config/
│   │   ├── AnthropicConfig.java          # Claude API 설정
│   │   ├── JpaConfig.java                # JPA Auditing 설정
│   │   ├── JwtConfig.java                # JWT 설정
│   │   ├── RedisConfig.java              # Redis 설정
│   │   ├── SecurityConfig.java           # Spring Security 설정
│   │   ├── SwaggerConfig.java            # Swagger 설정
│   │   ├── CacheConfig.java              # 캐시 설정
│   │   └── WebConfig.java                # CORS 설정
│   ├── security/
│   │   ├── JwtUtil.java                  # JWT 토큰 생성/검증
│   │   └── SecurityUtil.java             # 현재 인증 사용자 정보 조회
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java  # JWT 인증 필터
│   ├── controller/
│   │   ├── AuthController.java           # 인증 API (로그인/로그아웃/토큰갱신)
│   │   ├── DiaryController.java          # 일기 API
│   │   ├── FlowerController.java         # 꽃 정보 API
│   │   ├── CodeController.java           # 공통 코드 API
│   │   └── WeeklyReportController.java   # 주간 리포트 API
│   ├── service/
│   │   ├── AuthService.java              # 인증 서비스
│   │   ├── RedisService.java             # Redis 서비스 (토큰 관리)
│   │   ├── DiaryService.java             # 일기 비즈니스 로직
│   │   ├── DiaryEmotionService.java      # 감정 분석 (Claude API)
│   │   ├── DiaryEmotionTestService.java  # 테스트 모드 (랜덤)
│   │   ├── FlowerService.java            # 꽃 정보 서비스
│   │   ├── CodeService.java              # 공통 코드 서비스
│   │   ├── WeeklyReportService.java      # 주간 리포트 서비스
│   │   └── ClaudeApiClient.java          # Claude API 클라이언트
│   ├── domain/
│   │   ├── User.java                     # 회원 엔티티
│   │   ├── Diary.java                    # 일기 엔티티
│   │   ├── Emotion.java                  # 감정-꽃 엔티티
│   │   ├── CodeGroup.java                # 코드 그룹 엔티티
│   │   ├── Code.java                     # 코드 엔티티
│   │   ├── WeeklyReport.java             # 주간 리포트 엔티티
│   │   └── BaseAuditEntity.java          # 공통 Audit 엔티티
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RefreshRequest.java
│   │   ├── DiaryCreateRequest.java
│   │   ├── DiaryUpdateRequest.java
│   │   ├── DiaryResponse.java
│   │   ├── DiaryEmotionResponse.java
│   │   ├── MonthlyDiariesResponse.java
│   │   ├── AllEmotionsResponse.java
│   │   ├── UserEmotionFlowerResponse.java
│   │   ├── CodeGroupResponse.java
│   │   ├── CodeResponse.java
│   │   ├── WeeklyReportResponse.java
│   │   └── EmotionPercent.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── DiaryRepository.java
│   │   ├── FlowerRepository.java
│   │   ├── CodeGroupRepository.java
│   │   ├── CodeRepository.java
│   │   └── WeeklyReportRepository.java
│   └── exception/
│       ├── ErrorCode.java
│       ├── BusinessException.java
│       ├── DiaryNotFoundException.java
│       ├── InvalidTokenExceptionCustom.java
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

**HTTP 테스트 파일** (IntelliJ IDEA, VS Code REST Client):

`http/` 디렉토리에 업무별로 분리된 테스트 파일이 있습니다:

| 파일 | 설명 |
|------|------|
| `auth.http` | 인증 API (로그인, 로그아웃, 토큰 갱신) |
| `diary.http` | 일기 API + 감정 조절 팁 테스트 |
| `flower.http` | 꽃 정보 API |
| `teacher.http` | 선생님 API (학생 목록 조회) |
| `code.http` | 공통 코드 API |
| `weekly-report.http` | 주간 리포트 API |

**사용 방법**:
1. `auth.http` 파일에서 로그인 실행 → `accessToken` 발급
2. 발급받은 토큰은 **전역 변수**로 관리되어 모든 파일에서 자동 사용
3. 원하는 API 파일 열고 테스트 실행

**예시**:
```http
# 1. auth.http에서 로그인
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json
{ "userId": "student2", "password": "1234" }

# 2. diary.http에서 바로 사용 가능
POST http://localhost:8080/api/v1/diaries
Authorization: Bearer {{accessToken}}  # 자동으로 인식됨
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
    max-tokens: 10000
    temperature: 0.3
```

---

## 에러 코드

모든 API는 실패 시 동일한 형식의 에러 응답을 반환합니다.

### 주요 에러 코드

| HTTP Status | Error Code | 설명 |
|-------------|------------|------|
| 400 | INVALID_INPUT | 입력 값 검증 실패 |
| 401 | INVALID_TOKEN | 토큰이 유효하지 않거나 만료됨 |
| 401 | INVALID_PASSWORD | 비밀번호 불일치 |
| 403 | FORBIDDEN | 접근 권한 없음 |
| 404 | DIARY_NOT_FOUND | 일기를 찾을 수 없음 |
| 404 | USER_NOT_FOUND | 사용자를 찾을 수 없음 |
| 500 | LLM_ANALYSIS_FAILED | AI 분석 실패 |

> 💡 **전체 에러 코드 및 처리 가이드는 [docs/api/error.md](./docs/api/error.md)를 참조하세요.**

---

## 개발 현황

### ✅ 완료된 기능
- [x] Spring Boot 프로젝트 초기 설정
- [x] MariaDB 연동 및 Entity 설계
- [x] **JWT 기반 인증/인가 시스템** (Access Token + Refresh Token)
- [x] **Redis 기반 토큰 관리** (블랙리스트, RefreshToken 저장)
- [x] **SecurityUtil을 통한 인증 정보 조회**
- [x] 일기 CRUD API 구현
- [x] Claude API 감정 분석 (실제 + 테스트 모드)
- [x] **감정 조절 팁 자동 제공** (연속 3일/5일 같은 영역 감정 시)
- [x] 월별 일기 목록 조회 (꽃 상세정보 포함)
- [x] 감정&꽃 통계 API
- [x] **주간 리포트 기능** (AI 기반 인사이트)
- [x] **공통 코드 관리 시스템** (코드 그룹/코드)
- [x] 예외 처리 및 에러 핸들링
- [x] Swagger UI 설정
- [x] 유효성 검사 (최소/최대 길이)
- [x] Soft Delete 구현

### 📝 추후 개선 사항

**보안 & 성능**
- [ ] Rate Limiting (API 호출 제한)
- [ ] 프롬프트 인젝션 방지 (Role 기반 프롬프트 분리)
  - 현재: 규칙 + 사용자 입력을 하나의 프롬프트로 전송 (조작 가능)
  - 개선: System Role(규칙) / User Role(일기 내용) 분리
  - Claude API `system` 파라미터 활용
  - 사용자 입력으로 인한 프롬프트 조작 원천 차단

**주간 리포트 기능 개선**
- [ ] 학생용 주간 리포트 생성/수정 기능
  - 이미 리포트가 있고 `isAnalyzed=false`인 경우 UPDATE로 재생성
  - 일기 3개 이상일 때만 분석 수행
  - `generateReport()` 메서드 로직 개선 필요
- [ ] 선생님용 학생 주간 리포트 생성/수정 API 추가
  - 같은 학교/반 학생의 리포트만 생성 가능
  - 학생용과 동일한 로직 (일기 3개 이상, UPDATE 지원)
  - TeacherService 및 TeacherController에 구현 필요


---

## 참고 문서

- [Database 설계 문서](./databaseDesign.md)
- [Claude API 문서](https://docs.anthropic.com/)
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)

---