# 학생 API 명세서

## 개요
학생이 일기를 작성하고 감정 분석 결과를 조회하며, 주간 리포트를 확인하는 API입니다.

**공통 인증:** 모든 API는 JWT Bearer Token 인증이 필요합니다.
```
Authorization: Bearer {accessToken}
```

**공통 권한:** STUDENT 타입 사용자 접근 가능 (일부 API는 권한 제한 없음)

---

## API 그룹

### 일기 API (`/api/v1/diaries`)
- [일기 작성](#일기-api)
- [일기 감정 분석](#일기-감정-분석)
- [일기 조회](#일기-조회)
- [일기 수정/삭제](#일기-수정삭제)

### 주간 리포트 API (`/api/v1/weekly-reports`)
- [주간 리포트 조회](#주간-리포트-api)
- [읽음 상태 관리](#주간-리포트-읽음-상태-관리)

### 감정/꽃 정보 API (`/api/v1/flowers`)
- [나의 감정 통계](#감정꽃-정보-api)
- [전체 감정 정보](#전체-감정-꽃-정보-조회)

---

## 일기 API

### 1. 일기 작성

#### 기본 정보
```
POST /api/v1/diaries
```

새로운 일기를 작성합니다. 이 시점에는 일기 내용만 저장되고 감정 분석은 수행되지 않습니다.

#### 요청
##### Request Body
```json
{
  "diaryDate": "2026-01-10",
  "content": "오늘은 친구들과 즐거운 시간을 보냈다. 점심시간에 같이 놀면서 많이 웃었다..."
}
```

| 필드 | 타입 | 필수 | 설명 | 제약 |
|-----|------|------|------|------|
| diaryDate | String | O | 일기 날짜 (YYYY-MM-DD) | - |
| content | String | O | 일기 내용 | 10자 이상, 5000자 이하 |

#### 응답
##### Response Body
```json
{
  "diaryId": 123,
  "diaryDate": "2026-01-10",
  "content": "오늘은 친구들과 즐거운 시간을 보냈다...",
  "isAnalyzed": false,
  "summary": null,
  "coreEmotionCode": null,
  "emotionReason": null,
  "flowerName": null,
  "flowerMeaning": null,
  "keywords": null,
  "emotions": null,
  "flowerDetail": null,
  "createdAt": "2026-01-10T14:30:00",
  "updatedAt": "2026-01-10T14:30:00",
  "analyzedAt": null
}
```

#### 에러 응답
```json
{
  "code": "DUPLICATE_DIARY_DATE",
  "message": "해당 날짜에 이미 일기가 존재합니다"
}
```

```json
{
  "code": "INVALID_INPUT",
  "message": "일기 내용은 10자 이상, 5000자 이하여야 합니다"
}
```

---

### 2. 일기 감정 분석

#### 2-1. 실제 AI 분석 (운영 환경)

##### 기본 정보
```
POST /api/v1/diaries/{diaryId}/analyze
```

작성된 일기를 AI로 분석하여 감정 정보를 추출합니다.

##### 요청
```http
POST /api/v1/diaries/123/analyze
Authorization: Bearer {accessToken}
```

##### 응답
```json
{
  "diaryId": 123,
  "diaryDate": "2026-01-10",
  "content": "오늘은 친구들과 즐거운 시간을 보냈다...",
  "isAnalyzed": true,
  "summary": "친구들과 즐겁게 보낸 하루",
  "coreEmotionCode": "E001",
  "emotionReason": "친구들과의 즐거운 시간이 주된 감정입니다",
  "flowerName": "해바라기",
  "flowerMeaning": "긍정적인 에너지",
  "keywords": ["친구", "즐거움", "행복"],
  "emotions": [
    {
      "emotion": "E001",
      "percent": 60,
      "color": "#FFD700",
      "emotionNameKr": "기쁨"
    },
    {
      "emotion": "E002",
      "percent": 30,
      "color": "#FF6B6B",
      "emotionNameKr": "설렘"
    },
    {
      "emotion": "E003",
      "percent": 10,
      "color": "#4ECDC4",
      "emotionNameKr": "평온"
    }
  ],
  "flowerDetail": {
    "emotionCode": "E001",
    "emotionNameKr": "기쁨",
    "emotionNameEn": "Joy",
    "flowerNameKr": "해바라기",
    "flowerNameEn": "Sunflower",
    "flowerMeaning": "긍정적인 에너지와 희망",
    "flowerColor": "노란색",
    "flowerOrigin": "북아메리카",
    "imageFile3d": "sunflower_3d.png",
    "area": "YELLOW"
  },
  "analyzedAt": "2026-01-10T14:35:00"
}
```

#### 2-2. 테스트 분석 (개발/테스트 환경)

##### 기본 정보
```
POST /api/v1/diaries/{diaryId}/analyze-test?area=yellow
```

AI API를 호출하지 않고 랜덤으로 감정 분석 결과를 생성합니다. 개발/테스트 용도로 사용합니다.

##### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| area | String | X | 감정 영역 지정 | red/yellow/blue/green |

##### 장점
- API 호출 비용 없음
- 빠른 테스트 가능
- area 지정으로 특정 감정 영역 테스트 가능

---

### 3. 일기 조회

#### 3-1. 일기 상세 조회

##### 기본 정보
```
GET /api/v1/diaries/{diaryId}
```

특정 일기의 전체 정보를 조회합니다.

##### 요청
```http
GET /api/v1/diaries/123
Authorization: Bearer {accessToken}
```

##### 응답
일기 작성/분석 응답과 동일한 구조

#### 3-2. 특정 날짜 일기 조회

##### 기본 정보
```
GET /api/v1/diaries/date/{date}
```

지정한 날짜의 일기를 조회합니다. 하루에 하나의 일기만 작성 가능합니다.

##### 요청
```http
GET /api/v1/diaries/date/2026-01-10
Authorization: Bearer {accessToken}
```

##### 에러 응답
```json
{
  "code": "DIARY_NOT_FOUND",
  "message": "일기를 찾을 수 없습니다"
}
```

#### 3-3. 월별 일기 목록 조회

##### 기본 정보
```
GET /api/v1/diaries?yearMonth={YYYY-MM}
```

특정 월의 모든 일기를 목록으로 조회합니다.

##### 요청
```http
GET /api/v1/diaries?yearMonth=2026-01
Authorization: Bearer {accessToken}
```

##### 응답
```json
{
  "yearMonth": "2026-01",
  "totalCount": 5,
  "diaries": [
    {
      "id": 123,
      "date": "2026-01-10",
      "content": "오늘은 친구들과...",
      "summary": "친구들과 즐겁게 보낸 하루",
      "isAnalyzed": true,
      "coreEmotionCode": "E001",
      "flower": "해바라기",
      "floriography": "긍정적인 에너지",
      "keywords": ["친구", "즐거움", "행복"],
      "emotions": [
        {
          "emotion": "E001",
          "percent": 60,
          "color": "#FFD700",
          "emotionNameKr": "기쁨"
        }
      ],
      "flowerDetail": {
        "emotionNameKr": "기쁨",
        "flowerNameKr": "해바라기",
        "imageFile3d": "sunflower_3d.png"
      }
    }
  ]
}
```

---

### 4. 일기 수정/삭제

#### 4-1. 일기 수정

##### 기본 정보
```
PUT /api/v1/diaries/{diaryId}
```

일기의 내용을 수정합니다. **수정 시 감정 분석 정보는 초기화됩니다.**

##### 요청
```http
PUT /api/v1/diaries/123
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "content": "수정된 일기 내용..."
}
```

##### 응답
```json
{
  "diaryId": 123,
  "content": "수정된 일기 내용...",
  "isAnalyzed": false,
  "summary": null,
  "coreEmotionCode": null,
  "keywords": null,
  ...
}
```

**주의:** 수정 후 다시 `/analyze`를 호출해야 감정 분석 결과가 생성됩니다.

#### 4-2. 일기 삭제

##### 기본 정보
```
DELETE /api/v1/diaries/{diaryId}
```

일기를 삭제합니다. Soft Delete 방식으로 실제 데이터는 보존됩니다.

##### 요청
```http
DELETE /api/v1/diaries/123
Authorization: Bearer {accessToken}
```

##### 응답
```
204 No Content
```

**참고:**
- 실제로 DB에서 삭제되지 않고 `deleted_at`에 삭제 시각 기록
- 삭제 후 같은 날짜에 새 일기 작성 가능

---

## 주간 리포트 API

### 1. 안 읽은 리포트 확인

#### 기본 정보
```
GET /api/v1/weekly-reports/unread/exists
```

안 읽은 리포트가 있는지 확인합니다.

#### 응답
```json
{
  "hasUnread": true,
  "hasNew": null
}
```

---

### 2. 새 리포트 확인

#### 기본 정보
```
GET /api/v1/weekly-reports/new/exists
```

새로 생성된 리포트(알림 전송 안 된)가 있는지 확인합니다.

#### 응답
```json
{
  "hasUnread": null,
  "hasNew": true
}
```

---

### 3. 주간 리포트 목록 조회

#### 기본 정보
```
GET /api/v1/weekly-reports/list?status={status}
```

주간 리포트 목록을 필터링하여 조회합니다.

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| status | String | X | all(전체) / read(읽음) / unread(안읽음) / recent(최근 3개월) | all |

#### 응답
```json
[
  {
    "reportId": 101,
    "startDate": "2025-12-30",
    "endDate": "2026-01-05",
    "diaryCount": 5,
    "isAnalyzed": true,
    "readYn": false,
    "createdAt": "2026-01-06T00:00:00"
  }
]
```

---

### 4. 주간 리포트 상세 조회

#### 기본 정보
```
GET /api/v1/weekly-reports/{reportId}
```

주간 리포트의 상세 정보를 조회합니다.

#### 응답
```json
{
  "reportId": 101,
  "startDate": "2025-12-30",
  "endDate": "2026-01-05",
  "diaryCount": 5,
  "isAnalyzed": true,
  "studentReport": "이번 주 너는 친구들과 많은 시간을 보내며...",
  "studentEncouragement": "다음 주에도 긍정적인 마음으로 생활하길 바라!",
  "mindGardeningTip": [
    "친구 일로 마음이 아플 때는, 그 감정을 바로 해결하지 않아도 괜찮아요. 글이나 메모로 마음을 먼저 꺼내본 뒤, 이야기할지 말지를 천천히 정해 보세요.",
    "기분이 계속 좋을 때도 몸이 보내는 피곤 신호를 한 번 살펴봐도 좋아요. 충분히 쉬는 것도 마음을 잘 가꾸는 방법이에요."
  ],
  "weekKeywords": ["친구", "가족", "놀이", "학교"],
  "emotionStats": [
    {
      "emotion": "E001",
      "emotionNameKr": "기쁨",
      "color": "#FFD700",
      "count": 3,
      "percentage": 60.0
    }
  ],
  "weeklyDiaryDetails": [
    {
      "diaryId": 10,
      "diaryDate": "2025-12-30",
      "coreEmotion": "E001",
      "emotionNameKr": "기쁨",
      "flowerNameKr": "해바라기",
      "flowerMeaning": "긍정적인 에너지",
      "imageFile3d": "sunflower_3d.png"
    }
  ],
  "highlights": {
    "flowerOfTheWeek": {
      "emotion": "E001",
      "emotionNameKr": "기쁨",
      "flowerNameKr": "해바라기",
      "flowerMeaning": "긍정적인 에너지",
      "imageFile3d": "sunflower_3d.png",
      "count": 3
    },
    "quickStats": {
      "totalDiaries": 5,
      "emotionVariety": 2,
      "dominantArea": "YELLOW",
      "dominantAreaNameKr": "노랑 영역 (활기찬 감정)"
    },
    "gardenDiversity": {
      "score": 65,
      "level": "풍성한 정원",
      "description": "이번 주는 2가지 감정의 꽃이 피었어요...",
      "emotionVariety": 2,
      "areaVariety": 2
    }
  },
  "readYn": false,
  "createdAt": "2026-01-06T00:00:00"
}
```

#### 에러 응답
```json
{
  "code": "WEEKLY_REPORT_NOT_ANALYZED",
  "message": "주간 리포트가 아직 분석되지 않았습니다"
}
```

---

### 5. 주간 리포트 읽음 처리

#### 기본 정보
```
PUT /api/v1/weekly-reports/{reportId}/read
```

주간 리포트를 읽음으로 표시합니다.

#### 응답
```
200 OK
```

---

### 6. 주간 리포트 알림 확인 처리

#### 기본 정보
```
PUT /api/v1/weekly-reports/{reportId}/notification-sent
```

새 리포트 알림을 확인했음을 표시합니다.

#### 사용 시나리오
1. 학생이 앱을 열면 `GET /new/exists` 호출 → `hasNew: true`
2. 학생이 새 리포트 알림 확인 (리포트 목록 진입)
3. `PUT /{reportId}/notification-sent` 호출
4. `newNotificationSent`를 true로 변경
5. 다음부터 `GET /new/exists` → `hasNew: false`

---

### 7. 주간 리포트 수동 생성 (학생용)

#### 기본 정보
```
POST /api/v1/weekly-reports/generate?startDate={date}&endDate={date}
```

학생이 수동으로 자신의 주간 리포트를 생성합니다.

**권한:** STUDENT 타입 필수

#### 요청
```http
POST /api/v1/weekly-reports/generate?startDate=2025-12-30&endDate=2026-01-05
Authorization: Bearer {accessToken}
```

#### 응답
주간 리포트 상세 조회와 동일한 구조

#### 에러 응답
```json
{
  "message": "일기가 3개 미만이어서 주간 리포트를 생성할 수 없습니다."
}
```

---

## 감정/꽃 정보 API

### 1. 나의 감정 통계 조회

#### 기본 정보
```
GET /api/v1/flowers/my-emotions
```

사용자가 작성한 일기에서 나타난 감정 통계를 조회합니다.

#### 응답
```json
{
  "items": [
    {
      "emotionCode": "E001",
      "flowerName": "해바라기",
      "flowerMeaning": "긍정적인 에너지",
      "count": 5,
      "dates": ["2026-01-10", "2026-01-05", "2025-12-20"],
      "flowerDetail": {
        "emotionCode": "E001",
        "emotionNameKr": "기쁨",
        "flowerNameKr": "해바라기",
        "flowerColor": "노란색",
        "flowerOrigin": "북아메리카",
        "imageFile3d": "sunflower_3d.png",
        "area": "YELLOW"
      }
    }
  ],
  "totalCount": 3
}
```

**비즈니스 로직:**
- 분석 완료된 일기만 집계 (`isAnalyzed=true`)
- 감정 코드별로 그룹화
- 각 감정의 횟수, 날짜 목록 제공

---

### 2. 전체 감정-꽃 정보 조회

#### 기본 정보
```
GET /api/v1/flowers/all-emotions
```

시스템에 등록된 모든 감정-꽃 매핑 정보를 조회합니다.

**권한:** 인증 불필요 (공개 마스터 데이터)

#### 응답
```json
{
  "emotions": [
    {
      "emotionCode": "E001",
      "emotionNameKr": "기쁨",
      "emotionNameEn": "Joy",
      "flowerNameKr": "해바라기",
      "flowerNameEn": "Sunflower",
      "flowerMeaning": "긍정적인 에너지",
      "flowerMeaningStory": "해바라기는 항상 태양을 향해...",
      "flowerColor": "노란색",
      "flowerColorCodes": "#FFD700,#FFA500",
      "flowerOrigin": "북아메리카",
      "flowerFragrance": "은은한 향",
      "flowerFunFact": "해바라기는 하루에 15도씩 회전합니다",
      "imageFile3d": "sunflower_3d.png",
      "imageFileRealistic": "sunflower_real.jpg",
      "area": "YELLOW",
      "displayOrder": 1
    }
  ],
  "totalCount": 20
}
```

**사용 예시:**
- 감정 선택 UI
- 꽃 도감/백과사전
- 초기 데이터 로딩

---

## 공통 응답 필드

### DiaryResponse
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| diaryId | Long | O | 일기 ID |
| diaryDate | String | O | 일기 날짜 (YYYY-MM-DD) |
| content | String | O | 일기 내용 |
| isAnalyzed | Boolean | O | 분석 완료 여부 |
| summary | String | △ | AI 요약 (분석 완료 시) |
| coreEmotionCode | String | △ | 대표 감정 코드 (분석 완료 시) |
| emotionReason | String | △ | 대표 감정 선택 이유 (분석 완료 시) |
| flowerName | String | △ | 꽃 이름 (분석 완료 시) |
| flowerMeaning | String | △ | 꽃말 (분석 완료 시) |
| keywords | Array<String> | △ | 핵심 감정 관련 키워드 (분석 완료 시, 최대 3개) |
| emotions | Array | △ | 감정 분포 (분석 완료 시) |
| flowerDetail | Object | △ | 꽃 상세 정보 (분석 완료 시) |
| createdAt | String | O | 생성 시각 |
| updatedAt | String | O | 수정 시각 |
| analyzedAt | String | △ | 분석 완료 시각 |

### EmotionPercent
| 필드 | 타입 | 설명 |
|-----|------|------|
| emotion | String | 감정 코드 |
| percent | Integer | 비율 (0-100) |
| color | String | 색상 (HEX) |
| emotionNameKr | String | 감정 이름 (한글) |

### FlowerDetail
| 필드 | 타입 | 설명 |
|-----|------|------|
| emotionCode | String | 감정 코드 |
| emotionNameKr | String | 감정 이름 (한글) |
| emotionNameEn | String | 감정 이름 (영문) |
| flowerNameKr | String | 꽃 이름 (한글) |
| flowerNameEn | String | 꽃 이름 (영문) |
| flowerMeaning | String | 꽃말 |
| flowerColor | String | 꽃 색상 |
| flowerOrigin | String | 원산지 |
| imageFile3d | String | 3D 이미지 파일명 |
| area | String | 감정 영역 (RED/YELLOW/BLUE/GREEN) |

---

## 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|-----|-------------|------|
| INVALID_INPUT | 400 | 입력 값이 올바르지 않음 |
| DUPLICATE_DIARY_DATE | 400 | 해당 날짜에 이미 일기 존재 |
| DIARY_NOT_FOUND | 404 | 일기를 찾을 수 없음 |
| DIARY_NOT_ANALYZED | 404 | 일기가 아직 분석되지 않음 |
| WEEKLY_REPORT_NOT_ANALYZED | 404 | 주간 리포트가 아직 분석되지 않음 |
| FORBIDDEN | 403 | 접근 권한 없음 |
| LLM_ANALYSIS_FAILED | 500 | AI 감정 분석 실패 |

---

## 버전 히스토리

### v1.1.0 (2026-01-11)
- 일기 분석 API 응답에 `keywords` 필드 추가 (핵심 감정 관련 키워드 최대 3개)
- 주간 리포트 API 응답에 `mindGardeningTip` 필드를 배열로 변경 (2~3개)
- 주간 리포트 API 응답에 `weekKeywords` 필드 추가 (최대 5개)

### v1.0.0 (2026-01-11)
- 초기 버전 작성
- 일기 API, 주간 리포트 API, 감정/꽃 정보 API 문서화
