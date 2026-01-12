# 인증 API 명세서

## 개요
사용자 인증 및 토큰 관리를 위한 API입니다.

**Base URL:** `/api/v1/auth`

**인증 방식:** JWT (JSON Web Token)
- AccessToken: 1일 유효
- RefreshToken: 1년 유효 (Redis 저장)

**보안 기능:**
- Refresh Token Rotation (보안 강화)
- 로그아웃 시 토큰 블랙리스트 관리
- BCrypt 비밀번호 암호화

---

## 목차
1. [로그인](#1-로그인)
2. [로그아웃](#2-로그아웃)
3. [토큰 갱신](#3-토큰-갱신)

---

## 1. 로그인

### 기본 정보
```
POST /v1/auth/login
```

사용자 인증 후 JWT 토큰을 발급합니다.

**권한:** 인증 불필요 (공개 API)

### 요청
#### Request Body
```json
{
  "userId": "student1",
  "password": "1234"
}
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|-----|------|------|------|------|
| userId | String | O | 로그인 ID | student1 |
| password | String | O | 비밀번호 | 1234 |

### 응답
#### Success Response (200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userSn": 1,
  "userId": "student1",
  "name": "홍길동",
  "userTypeCd": "STUDENT",
  "schoolCode": "1111",
  "schoolNm": "예시초등학교",
  "classCode": "301"
}
```

#### Response Fields
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| accessToken | String | O | Access Token (API 요청 시 사용, 1일 유효) |
| refreshToken | String | O | Refresh Token (토큰 갱신 시 사용, 1년 유효) |
| userSn | Long | O | 사용자 일련번호 |
| userId | String | O | 로그인 ID |
| name | String | O | 이름 |
| userTypeCd | String | O | 사용자 유형 코드 (STUDENT/TEACHER/ADMIN) |
| schoolCode | String | △ | 학교 코드 (학생/선생님만) |
| schoolNm | String | △ | 학교명 (학생/선생님만) |
| classCode | String | △ | 학급 코드 (학생/선생님만) |

### 에러 응답
#### 사용자를 찾을 수 없음 (404)
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

#### 비밀번호 불일치 (401)
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

#### 입력 값 검증 실패 (400)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_INPUT",
  "message": "아이디를 입력해주세요",
  "path": "/api/v1/auth/login"
}
```

### 비즈니스 로직
1. userId로 사용자 조회
2. 비밀번호 검증 (BCrypt)
3. JWT 토큰 생성 (AccessToken + RefreshToken)
4. RefreshToken을 Redis에 저장
5. 응답 반환

### 사용 예시
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "userId": "student1",
  "password": "1234"
}
```

---

## 2. 로그아웃

### 기본 정보
```
POST /v1/auth/logout
```

사용자의 토큰을 무효화합니다.

**권한:** 인증 필요 (모든 사용자)

### 요청
#### Headers
```
Authorization: Bearer {accessToken}
```

#### Request Example
```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 응답
#### Success Response (200 OK)
```json
{
  "message": "로그아웃되었습니다"
}
```

### 에러 응답
#### 유효하지 않은 토큰 (401)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰 입니다.",
  "path": "/api/v1/auth/logout"
}
```

### 비즈니스 로직
1. Authorization 헤더에서 AccessToken 추출
2. SecurityContext에서 인증된 사용자 정보 조회
3. AccessToken을 블랙리스트에 추가 (만료 시간까지)
4. RefreshToken을 Redis에서 삭제

### 참고사항
- 로그아웃 후 AccessToken은 만료 시간까지 블랙리스트에 유지됩니다
- JwtAuthenticationFilter에서 블랙리스트 토큰을 차단합니다
- 로그아웃 후 RefreshToken으로 토큰 갱신 불가능

---

## 3. 토큰 갱신

### 기본 정보
```
POST /v1/auth/refresh
```

RefreshToken을 사용하여 새로운 AccessToken과 RefreshToken을 발급합니다.

**권한:** 인증 불필요 (RefreshToken 필요)

**보안:** Refresh Token Rotation 방식 적용

### 요청
#### Request Body
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| refreshToken | String | O | Refresh Token |

### 응답
#### Success Response (200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Response Fields
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| accessToken | String | O | 새로 발급된 Access Token (1일 유효) |
| refreshToken | String | O | 새로 발급된 Refresh Token (1년 유효) |

### 에러 응답
#### 유효하지 않은 토큰 (401)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 401,
  "error": "Unauthorized",
  "code": "INVALID_TOKEN",
  "message": "유효하지 않은 토큰 입니다.",
  "path": "/api/v1/auth/refresh"
}
```

#### 입력 값 검증 실패 (400)
```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_INPUT",
  "message": "Refresh Token을 입력해주세요",
  "path": "/api/v1/auth/refresh"
}
```

### 비즈니스 로직
1. RefreshToken 유효성 검증
2. RefreshToken에서 userSn 추출
3. Redis에 저장된 RefreshToken과 비교
4. 일치하면 새로운 AccessToken + RefreshToken 발급
5. 기존 RefreshToken 무효화 (일회용)
6. 새 RefreshToken을 Redis에 저장

### 사용 예시
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 중요 사항
- **Refresh Token Rotation 방식으로 보안 강화**
- 기존 RefreshToken은 사용 후 무효화됨 (한 번만 사용 가능)
- 새로 발급된 RefreshToken을 반드시 저장해야 함
- RefreshToken이 만료되었거나 Redis에 없으면 실패
- 로그아웃한 사용자는 RefreshToken이 삭제되어 갱신 불가능

---

## 공통 응답 형식

### 성공 응답
각 API별로 응답 형식이 다르므로 상단 API 명세 참조

### 에러 응답
모든 에러는 동일한 형식으로 반환됩니다.

```json
{
  "timestamp": "2026-01-12T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_INPUT",
  "message": "입력 값이 올바르지 않습니다",
  "path": "/api/v1/auth/login"
}
```

| 필드 | 타입 | 설명 |
|-----|------|------|
| timestamp | String | 에러 발생 시각 (ISO 8601) |
| status | Integer | HTTP 상태 코드 |
| error | String | HTTP 상태 메시지 |
| code | String | 에러 코드 |
| message | String | 에러 메시지 |
| path | String | 요청 경로 |

---

## 공통 에러 코드

| HTTP Status | 코드 | 메시지 | 설명 |
|-------------|------|--------|------|
| 400 | INVALID_INPUT | 입력 값이 올바르지 않습니다 | 요청 파라미터 검증 실패 |
| 401 | INVALID_TOKEN | 유효하지 않은 토큰 입니다. | 토큰이 유효하지 않거나 만료됨 |
| 401 | INVALID_PASSWORD | 비밀번호가 일치하지 않습니다 | 비밀번호 불일치 |
| 404 | USER_NOT_FOUND | 사용자를 찾을 수 없습니다 | 존재하지 않는 사용자 ID |
| 500 | INTERNAL_SERVER_ERROR | 서버 내부 오류가 발생했습니다 | 예기치 않은 서버 오류 |

전체 에러 코드는 [공통 에러 응답 명세](./error.md)를 참조하세요.

---

## 인증 흐름

### 1. 최초 로그인
```
1. POST /v1/auth/login
   → AccessToken + RefreshToken 발급
2. AccessToken을 헤더에 포함하여 API 요청
   Authorization: Bearer {accessToken}
```

### 2. AccessToken 만료 시
```
1. API 요청 → 401 Unauthorized (토큰 만료)
2. POST /v1/auth/refresh
   → 새 AccessToken + RefreshToken 발급
3. 새 AccessToken으로 다시 API 요청
```

### 3. 로그아웃
```
1. POST /v1/auth/logout
   → AccessToken 블랙리스트 등록
   → RefreshToken Redis에서 삭제
2. 재로그인 필요
```

---

## 보안 고려사항

### Refresh Token Rotation
- RefreshToken 사용 시마다 새로운 RefreshToken 발급
- 기존 RefreshToken은 즉시 무효화
- 탈취된 토큰의 재사용 방지

### 토큰 저장 위치
- **AccessToken**: 클라이언트 메모리 또는 secure storage
- **RefreshToken**: 클라이언트 secure storage (안전한 저장소)
- **주의**: LocalStorage에 저장 시 XSS 공격 위험

### 블랙리스트 관리
- 로그아웃한 AccessToken은 만료 시간까지 블랙리스트 유지
- Redis TTL을 토큰 만료 시간과 동일하게 설정하여 자동 삭제

---

## 버전 히스토리

### v1.0.0 (2026-01-12)
- 초기 버전 작성
- 로그인, 로그아웃, 토큰 갱신 API 문서화
- Refresh Token Rotation 방식 적용
