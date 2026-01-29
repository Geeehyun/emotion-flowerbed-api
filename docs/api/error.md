# 공통 에러 응답 명세서

## 개요
API 호출 시 발생할 수 있는 모든 에러에 대한 명세입니다.

모든 API는 실패 시 동일한 형식의 에러 응답을 반환합니다.

---

## 에러 응답 구조

### Error Response Format

모든 에러는 다음과 같은 JSON 형식으로 반환됩니다.

```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_INPUT",
  "message": "입력 값이 올바르지 않습니다",
  "path": "/api/v1/diaries"
}
```

### Response Fields

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| timestamp | String | O | 에러 발생 시각 (ISO 8601 형식) |
| status | Integer | O | HTTP 상태 코드 |
| error | String | O | HTTP 상태 메시지 (예: Bad Request, Unauthorized) |
| code | String | O | 에러 코드 (비즈니스 에러 식별자) |
| message | String | O | 에러 메시지 (사용자에게 표시할 내용) |
| path | String | O | 요청 경로 |

---

## HTTP Status Code별 에러 코드

### 400 Bad Request
잘못된 요청 파라미터, 입력 값 검증 실패, 비즈니스 규칙 위반

| 코드 | 메시지 | 설명 |
|-----|--------|------|
| INVALID_INPUT | 입력 값이 올바르지 않습니다 | 필수 파라미터 누락, 형식 오류, 검증 실패 |
| DUPLICATE_USER_ID | 이미 사용 중인 아이디입니다 | 회원가입 시 중복된 ID 사용 시도 |
| INVALID_DIARY_CONTENT | 일기 내용이 분석 불가능합니다 | 일기 내용이 10자 미만이거나 5000자 초과 |
| DUPLICATE_DIARY_DATE | 해당 날짜에 이미 일기가 존재합니다 | 같은 날짜에 중복 일기 작성 시도 |
| NO_SCHOOL_INFO | 학교 정보가 올바르지 않습니다. | 학교 코드 또는 반 코드 미설정 |
| INVALID_RISK_LEVEL | 위험 레벨이 올바르지 않습니다. | 지원하지 않는 위험 레벨 값 |

### 401 Unauthorized
인증 실패, 토큰 오류

| 코드 | 메시지 | 설명 |
|-----|--------|------|
| INVALID_TOKEN | 유효하지 않은 토큰 입니다. | 토큰이 유효하지 않거나 만료됨 |
| INVALID_PASSWORD | 비밀번호가 일치하지 않습니다 | 로그인 시 비밀번호 불일치 |

### 403 Forbidden
권한 없음, 접근 제한

| 코드 | 메시지 | 설명 |
|-----|--------|------|
| FORBIDDEN | 접근 권한이 없는 사용자입니다. | 사용자 유형 또는 소속 반 불일치로 접근 불가 |

### 404 Not Found
리소스를 찾을 수 없음

| 코드 | 메시지 | 설명 |
|-----|--------|------|
| USER_NOT_FOUND | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 ID |
| DIARY_NOT_FOUND | 일기를 찾을 수 없습니다 | 존재하지 않는 일기 ID 또는 삭제된 일기 |
| DIARY_NOT_ANALYZED | 일기가 아직 분석되지 않았습니다 | 분석되지 않은 일기의 분석 결과 조회 시도 |
| WEEKLY_REPORT_NOT_ANALYZED | 주간 리포트가 아직 분석되지 않았습니다 | 분석되지 않은 주간 리포트 조회 시도 |
| NO_STUDENTS_FOUND | 담당 학생이 없습니다 | 선생님의 담당 학생이 없음 |
| FLOWER_NOT_FOUND | 꽃 정보를 찾을 수 없습니다 | 존재하지 않는 감정 코드 |
| CODE_NOT_FOUND | 코드 정보를 찾을 수 없습니다 | 존재하지 않는 공통 코드 |

### 500 Internal Server Error
서버 내부 오류

| 코드 | 메시지 | 설명 |
|-----|--------|------|
| LLM_ANALYSIS_FAILED | AI 감정 분석에 실패했습니다 | AI API 호출 실패 또는 분석 처리 중 오류 |
| INTERNAL_SERVER_ERROR | 서버 내부 오류가 발생했습니다 | 예기치 않은 서버 오류 |

---

## 에러 코드 상세 설명

### INVALID_INPUT

**HTTP Status:** 400 Bad Request

**발생 상황:**
- 필수 파라미터 누락
- 잘못된 데이터 형식
- Validation 검증 실패

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_INPUT",
  "message": "일기 내용은 10자 이상, 5000자 이하여야 합니다",
  "path": "/api/v1/diaries"
}
```

**해결 방법:**
- 요청 파라미터 확인
- API 명세서의 필드 제약사항 확인

---

### INVALID_TOKEN

**HTTP Status:** 401 Unauthorized

**발생 상황:**
- AccessToken이 유효하지 않음
- AccessToken이 만료됨
- AccessToken이 블랙리스트에 등록됨 (로그아웃된 토큰)
- RefreshToken이 유효하지 않거나 만료됨

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰 입니다.",
  "path": "/api/v1/diaries"
}
```

**해결 방법:**
- AccessToken 만료 시: `/v1/auth/refresh` 호출하여 토큰 갱신
- RefreshToken 만료 시: 재로그인 필요 (`/v1/auth/login`)
- 블랙리스트 토큰: 재로그인 필요

---

### INVALID_PASSWORD

**HTTP Status:** 401 Unauthorized

**발생 상황:**
- 로그인 시 비밀번호 불일치

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_PASSWORD",
  "message": "비밀번호가 일치하지 않습니다",
  "path": "/api/v1/auth/login"
}
```

**해결 방법:**
- 올바른 비밀번호 입력

---

### FORBIDDEN

**HTTP Status:** 403 Forbidden

**발생 상황:**
- 사용자 유형이 API 권한과 맞지 않음 (예: 학생이 선생님 API 호출)
- 다른 반 학생의 정보 조회 시도
- 본인 소유가 아닌 리소스 수정/삭제 시도

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 403,
  "error": "Forbidden",
  "code": "FORBIDDEN",
  "message": "접근 권한이 없는 사용자입니다.",
  "path": "/api/v1/teachers/students"
}
```

**해결 방법:**
- 사용자 권한 확인
- 올바른 API 엔드포인트 사용

---

### DUPLICATE_DIARY_DATE

**HTTP Status:** 400 Bad Request

**발생 상황:**
- 같은 날짜에 이미 일기가 존재하는 상태에서 새 일기 작성 시도

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "DUPLICATE_DIARY_DATE",
  "message": "해당 날짜에 이미 일기가 존재합니다",
  "path": "/api/v1/diaries"
}
```

**해결 방법:**
- 기존 일기 수정 (`PUT /v1/diaries/{diaryId}`)
- 기존 일기 삭제 후 새 일기 작성

---

### DIARY_NOT_FOUND

**HTTP Status:** 404 Not Found

**발생 상황:**
- 존재하지 않는 일기 ID로 조회/수정/삭제 시도
- 삭제된 일기에 접근 시도
- 다른 사용자의 일기 접근 시도

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 404,
  "error": "Not Found",
  "code": "DIARY_NOT_FOUND",
  "message": "일기를 찾을 수 없습니다",
  "path": "/api/v1/diaries/999"
}
```

**해결 방법:**
- 올바른 일기 ID 확인
- 일기 목록에서 존재하는 일기만 조회

---

### DIARY_NOT_ANALYZED

**HTTP Status:** 404 Not Found

**발생 상황:**
- 아직 분석되지 않은 일기의 분석 결과 조회 시도

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 404,
  "error": "Not Found",
  "code": "DIARY_NOT_ANALYZED",
  "message": "일기가 아직 분석되지 않았습니다",
  "path": "/api/v1/diaries/123"
}
```

**해결 방법:**
- 먼저 `/v1/diaries/{diaryId}/analyze` 또는 `/analyze-test` 호출
- 분석 완료 후 다시 조회

---

### WEEKLY_REPORT_NOT_ANALYZED

**HTTP Status:** 404 Not Found

**발생 상황:**
- 일기가 3개 미만인 주간 리포트 조회 시도
- 아직 분석되지 않은 주간 리포트 조회 시도

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 404,
  "error": "Not Found",
  "code": "WEEKLY_REPORT_NOT_ANALYZED",
  "message": "주간 리포트가 아직 분석되지 않았습니다",
  "path": "/api/v1/weekly-reports/101"
}
```

**해결 방법:**
- 해당 주에 일기 3개 이상 작성 후 대기
- 자동 생성 스케줄러 실행 대기 또는 수동 생성

---

### LLM_ANALYSIS_FAILED

**HTTP Status:** 500 Internal Server Error

**발생 상황:**
- AI API 호출 실패
- AI 응답 파싱 오류
- AI 분석 처리 중 예외 발생

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 500,
  "error": "Internal Server Error",
  "code": "LLM_ANALYSIS_FAILED",
  "message": "AI 감정 분석에 실패했습니다",
  "path": "/api/v1/diaries/123/analyze"
}
```

**해결 방법:**
- 잠시 후 다시 시도
- 개발 환경: `/analyze-test` 사용 (테스트용 랜덤 분석)
- 지속 발생 시 서버 관리자에게 문의

---

### USER_NOT_FOUND

**HTTP Status:** 404 Not Found

**발생 상황:**
- 존재하지 않는 사용자 ID로 로그인 시도
- 존재하지 않는 학생 조회 시도

**예시:**
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 404,
  "error": "Not Found",
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다",
  "path": "/api/v1/auth/login"
}
```

**해결 방법:**
- 올바른 사용자 ID 입력
- 회원가입 필요 (`/v1/auth/signup`)

---

### DUPLICATE_USER_ID

**HTTP Status:** 400 Bad Request

**발생 상황:**
- 회원가입 시 이미 존재하는 ID 사용 시도

**예시:**
```json
{
  "timestamp": "2026-01-29T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "DUPLICATE_USER_ID",
  "message": "이미 사용 중인 아이디입니다",
  "path": "/api/v1/auth/signup"
}
```

**해결 방법:**
- ID 중복 조회 API로 사전 확인 (`/v1/auth/check-duplicate`)
- 다른 ID 사용

---

## 에러 처리 가이드

### 클라이언트 에러 처리 권장 사항

#### 1. 400 Bad Request
```javascript
if (error.code === 'INVALID_INPUT') {
  // 입력 값 검증 실패 - 사용자에게 메시지 표시
  showValidationError(error.message);
} else if (error.code === 'DUPLICATE_DIARY_DATE') {
  // 중복 일기 - 수정 또는 삭제 선택지 제공
  showDuplicateDiaryDialog();
}
```

#### 2. 401 Unauthorized
```javascript
if (error.code === 'INVALID_TOKEN') {
  // 토큰 만료 - 토큰 갱신 시도
  try {
    const tokens = await refreshToken();
    // 갱신 성공 - 원래 요청 재시도
    retryOriginalRequest(tokens.accessToken);
  } catch (refreshError) {
    // 갱신 실패 - 로그인 페이지로 이동
    redirectToLogin();
  }
}
```

#### 3. 403 Forbidden
```javascript
if (error.code === 'FORBIDDEN') {
  // 권한 없음 - 사용자에게 알림 후 이전 페이지로 이동
  showErrorMessage('접근 권한이 없습니다');
  goBack();
}
```

#### 4. 404 Not Found
```javascript
if (error.code === 'DIARY_NOT_FOUND') {
  // 리소스 없음 - 목록 페이지로 이동
  redirectToDiaryList();
} else if (error.code === 'DIARY_NOT_ANALYZED') {
  // 분석 안 됨 - 분석 요청 버튼 표시
  showAnalyzeButton();
}
```

#### 5. 500 Internal Server Error
```javascript
if (error.code === 'LLM_ANALYSIS_FAILED') {
  // AI 분석 실패 - 재시도 또는 테스트 모드 제안
  showRetryDialog('AI 분석에 실패했습니다. 다시 시도하시겠습니까?');
} else {
  // 기타 서버 오류 - 일반 에러 메시지
  showErrorMessage('일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
}
```

---

## 에러 응답 예시 모음

### Validation 에러 (여러 필드 검증 실패)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_INPUT",
  "message": "아이디를 입력해주세요, 비밀번호를 입력해주세요",
  "path": "/api/v1/auth/login"
}
```

### 권한 부족 (다른 반 학생 조회)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 403,
  "error": "Forbidden",
  "code": "FORBIDDEN",
  "message": "다른 반 학생의 정보는 조회할 수 없습니다",
  "path": "/api/v1/teachers/students/999/weekly-reports"
}
```

### 리소스 없음 (특정 날짜 일기 조회)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 404,
  "error": "Not Found",
  "code": "DIARY_NOT_FOUND",
  "message": "일기를 찾을 수 없습니다",
  "path": "/api/v1/diaries/date/2026-01-12"
}
```

---

## 버전 히스토리

### v1.1.0 (2026-01-29)
- DUPLICATE_USER_ID 에러 코드 추가

### v1.0.0 (2026-01-12)
- 초기 버전 작성
- 전체 에러 코드 및 응답 형식 문서화
- HTTP Status Code별 에러 분류
- 에러 처리 가이드 추가
