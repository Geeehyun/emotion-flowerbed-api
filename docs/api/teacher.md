# 선생님 API 명세서

## 개요
선생님이 담당 반 학생들의 감정 상태를 모니터링하고 관리하는 API입니다.

**Base URL:** `/api/v1/teachers`

**공통 인증:** 모든 API는 JWT Bearer Token 인증이 필요합니다.
```
Authorization: Bearer {accessToken}
```

**공통 권한:** TEACHER 타입 사용자만 접근 가능

---

## 목차
1. [내 학생 목록 조회](#1-내-학생-목록-조회)
2. [날짜별 학생 감정 현황 조회](#2-날짜별-학생-감정-현황-조회)
3. [위험 학생 리스트 조회](#3-위험-학생-리스트-조회)
4. [DANGER 상태 해제](#4-danger-상태-해제)
5. [학생별 위험도 변화 이력 조회](#5-학생별-위험도-변화-이력-조회)
6. [학생별 주간 리포트 목록 조회](#6-학생별-주간-리포트-목록-조회)
7. [학생별 주간 리포트 상세 조회](#7-학생별-주간-리포트-상세-조회)
8. [학생 월별 감정 조회](#8-학생-월별-감정-조회)
9. [학급 월별 감정 분포 조회](#9-학급-월별-감정-분포-조회)

---

## 1. 내 학생 목록 조회

### 기본 정보
```
GET /v1/teachers/students
```

선생님이 담당하는 반의 학생 목록을 조회합니다.

### 요청
#### Headers
```
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
[
  {
    "userSn": 1,
    "userId": "student01",
    "name": "김학생",
    "schoolCode": "S001",
    "schoolNm": "행복초등학교",
    "classCode": "C101",
    "emotionControlCd": "EC01",
    "recentEmotionArea": "yellow",
    "recentCoreEmotionCd": "E001",
    "recentCoreEmotionNameKr": "기쁨",
    "recentCoreEmotionImage": "sunflower_3d.png"
  }
]
```

#### Response Fields
| 필드 | 타입 | 설명 |
|-----|------|------|
| userSn | Long | 학생 일련번호 |
| userId | String | 로그인 ID |
| name | String | 학생 이름 |
| schoolCode | String | 학교 코드 |
| schoolNm | String | 학교명 |
| classCode | String | 반 코드 |
| emotionControlCd | String | 감정 제어 활동 코드 |
| recentEmotionArea | String | 최근 감정 영역 (red/yellow/blue/green) |
| recentCoreEmotionCd | String | 최근 핵심 감정 코드 |
| recentCoreEmotionNameKr | String | 최근 핵심 감정 이름 (한글) |
| recentCoreEmotionImage | String | 최근 감정 3D 이미지 파일명 |

### 에러 응답
```json
{
  "code": "FORBIDDEN",
  "message": "선생님만 접근할 수 있습니다"
}
```

```json
{
  "code": "NO_SCHOOL_INFO",
  "message": "학교 코드 또는 반 코드가 설정되지 않았습니다"
}
```

---

## 2. 날짜별 학생 감정 현황 조회

### 기본 정보
```
GET /v1/teachers/daily-emotion-status
```

선생님이 담당하는 반의 특정 날짜 학생들의 감정 현황을 조회합니다.

### 요청
#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| date | String | X | 조회 날짜 (yyyy-MM-dd 형식, 미지정 시 오늘) | 2026-01-06 |

#### Request Example
```http
GET /v1/teachers/daily-emotion-status?date=2026-01-06
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
{
  "date": "2026-01-06",
  "totalCount": 25,
  "area": {
    "red": 2,
    "yellow": 8,
    "blue": 3,
    "green": 7,
    "unanalyzed": 2,
    "none": 3
  },
  "students": [
    {
      "userSn": 1,
      "name": "김학생",
      "area": "yellow",
      "coreEmotionCd": "E001",
      "coreEmotionNameKr": "기쁨",
      "coreEmotionImage": "sunflower_3d.png",
      "isAnalyzed": true
    },
    {
      "userSn": 2,
      "name": "이학생",
      "area": "none",
      "coreEmotionCd": null,
      "coreEmotionNameKr": null,
      "coreEmotionImage": null,
      "isAnalyzed": false
    }
  ]
}
```

#### Response Fields

**최상위**
| 필드 | 타입 | 설명 |
|-----|------|------|
| date | String | 조회 날짜 |
| totalCount | Integer | 전체 학생 수 |
| area | Object | 영역별 학생 수 집계 |
| students | Array | 학생별 감정 상세 정보 |

**area**
| 필드 | 타입 | 설명 |
|-----|------|------|
| red | Integer | 빨강 영역 (강한 감정) 학생 수 |
| yellow | Integer | 노랑 영역 (활기찬 감정) 학생 수 |
| blue | Integer | 파랑 영역 (차분한 감정) 학생 수 |
| green | Integer | 초록 영역 (평온한 감정) 학생 수 |
| unanalyzed | Integer | 일기 작성했지만 분석 안 됨 |
| none | Integer | 일기 미작성 |

**students[]**
| 필드 | 타입 | 설명 |
|-----|------|------|
| userSn | Long | 학생 일련번호 |
| name | String | 학생 이름 |
| area | String | 감정 영역 (red/yellow/blue/green/unanalyzed/none) |
| coreEmotionCd | String | 핵심 감정 코드 (분석 안 됐으면 null) |
| coreEmotionNameKr | String | 핵심 감정 이름 (한글) |
| coreEmotionImage | String | 감정 3D 이미지 파일명 |
| isAnalyzed | Boolean | 일기 분석 여부 |

---

## 3. 위험 학생 리스트 조회

### 기본 정보
```
GET /v1/teachers/students/at-risk
```

선생님이 담당하는 반의 CAUTION 또는 DANGER 상태 학생 목록을 조회합니다.

### 요청
#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| level | String | X | 위험 레벨 필터 (ALL/CAUTION/DANGER, 기본값: ALL) | DANGER |

#### Request Example
```http
GET /v1/teachers/students/at-risk?level=DANGER
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
{
  "totalCount": 5,
  "dangerCount": 2,
  "cautionCount": 3,
  "students": [
    {
      "userSn": 3,
      "name": "박학생",
      "riskLevel": "DANGER",
      "riskReason": "7일 연속 red 영역 감정 (강한 감정). 강도 높은 감정이 지속되어 주의가 필요합니다.",
      "riskContinuousArea": "red",
      "riskContinuousDays": 7,
      "riskUpdatedAt": "2026-01-06T10:30:00",
      "dangerResolvedBy": null,
      "dangerResolvedAt": null,
      "dangerResolveMemo": null
    },
    {
      "userSn": 5,
      "name": "최학생",
      "riskLevel": "CAUTION",
      "riskReason": "7일 연속 yellow 영역 감정 (활기찬 감정)",
      "riskContinuousArea": "yellow",
      "riskContinuousDays": 7,
      "riskUpdatedAt": "2026-01-05T14:20:00",
      "dangerResolvedBy": null,
      "dangerResolvedAt": null,
      "dangerResolveMemo": null
    }
  ]
}
```

#### Response Fields

**최상위**
| 필드 | 타입 | 설명 |
|-----|------|------|
| totalCount | Integer | 전체 위험 학생 수 |
| dangerCount | Integer | DANGER 학생 수 |
| cautionCount | Integer | CAUTION 학생 수 |
| students | Array | 위험 학생 목록 |

**students[]**
| 필드 | 타입 | 설명 |
|-----|------|------|
| userSn | Long | 학생 일련번호 |
| name | String | 학생 이름 |
| riskLevel | String | 위험도 레벨 (CAUTION/DANGER) |
| riskReason | String | 위험도 판정 사유 |
| riskContinuousArea | String | 연속된 감정 영역 (red/yellow/blue/green) |
| riskContinuousDays | Integer | 연속 일수 |
| riskUpdatedAt | String | 위험도 갱신 시각 (ISO 8601) |
| dangerResolvedBy | Long | DANGER 해제한 선생님 user_sn (해제 안 됐으면 null) |
| dangerResolvedAt | String | DANGER 해제 시각 (해제 안 됐으면 null) |
| dangerResolveMemo | String | DANGER 해제 사유 (해제 안 됐으면 null) |

### 참고사항
- DANGER 우선 정렬, 같은 레벨 내에서는 위험도 갱신 시각 최신순
- NORMAL 상태 학생은 포함되지 않음

---

## 4. DANGER 상태 해제

### 기본 정보
```
POST /v1/teachers/students/{studentUserSn}/resolve-danger
```

선생님이 DANGER 상태인 학생을 상담하고 위험 상태를 해제합니다.

### 요청
#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studentUserSn | Long | O | 학생 user_sn |

#### Request Body
```json
{
  "memo": "학생 상담 완료. 최근 상태 개선 확인."
}
```

| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| memo | String | O | 해제 사유 |

#### Request Example
```http
POST /v1/teachers/students/1/resolve-danger
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "memo": "학생 상담 완료. 최근 상태 개선 확인."
}
```

### 응답
#### Success Response
```
200 OK
```

### 에러 응답
```json
{
  "code": "NOT_DANGER_STATUS",
  "message": "DANGER 상태가 아닙니다"
}
```

```json
{
  "code": "FORBIDDEN",
  "message": "다른 반 학생의 상태는 변경할 수 없습니다"
}
```

### 참고사항
- DANGER 상태는 선생님만 해제 가능
- 해제 후에도 다음 일기 분석 시까지 DANGER 유지됨
- 다음 일기 분석에서 연속이 끊겼으면 자동으로 NORMAL/CAUTION으로 변경

---

## 5. 학생별 위험도 변화 이력 조회

### 기본 정보
```
GET /v1/teachers/students/{studentUserSn}/risk-history
```

선생님이 특정 학생의 위험도 변화 이력을 조회합니다.

### 요청
#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studentUserSn | Long | O | 학생 user_sn |

#### Request Example
```http
GET /v1/teachers/students/1/risk-history
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
{
  "userSn": 1,
  "name": "김학생",
  "totalCount": 5,
  "histories": [
    {
      "historyId": 15,
      "previousLevel": "CAUTION",
      "newLevel": "DANGER",
      "riskType": "CONTINUOUS_RED_BLUE",
      "riskReason": "7일 연속 red 영역 감정 (강한 감정). 강도 높은 감정이 지속되어 주의가 필요합니다.",
      "continuousArea": "red",
      "continuousDays": 7,
      "concernKeywords": [],
      "isConfirmed": false,
      "confirmedBy": null,
      "confirmedAt": null,
      "teacherMemo": null,
      "createdAt": "2026-01-06T10:30:00"
    },
    {
      "historyId": 14,
      "previousLevel": "NORMAL",
      "newLevel": "CAUTION",
      "riskType": "CONTINUOUS_SAME_AREA",
      "riskReason": "7일 연속 red 영역 감정 (강한 감정)",
      "continuousArea": "red",
      "continuousDays": 7,
      "concernKeywords": [],
      "isConfirmed": true,
      "confirmedBy": 100,
      "confirmedAt": "2026-01-05T15:00:00",
      "teacherMemo": "학생 상담 진행함",
      "createdAt": "2025-12-30T09:20:00"
    }
  ]
}
```

#### Response Fields

**최상위**
| 필드 | 타입 | 설명 |
|-----|------|------|
| userSn | Long | 학생 일련번호 |
| name | String | 학생 이름 |
| totalCount | Integer | 전체 이력 개수 |
| histories | Array | 위험도 변화 이력 목록 (최근순) |

**histories[]**
| 필드 | 타입 | 설명 |
|-----|------|------|
| historyId | Long | 이력 ID |
| previousLevel | String | 이전 위험도 레벨 (NORMAL/CAUTION/DANGER) |
| newLevel | String | 새 위험도 레벨 (NORMAL/CAUTION/DANGER) |
| riskType | String | 위험 유형 (KEYWORD_DETECTED/CONTINUOUS_RED_BLUE/CONTINUOUS_SAME_AREA/RESOLVED) |
| riskReason | String | 위험도 판정 사유 |
| continuousArea | String | 연속된 감정 영역 (red/yellow/blue/green) |
| continuousDays | Integer | 연속 일수 |
| concernKeywords | Array<String> | 탐지된 우려 키워드 목록 |
| isConfirmed | Boolean | 선생님 확인 여부 |
| confirmedBy | Long | 확인한 선생님 user_sn |
| confirmedAt | String | 확인 시각 (ISO 8601) |
| teacherMemo | String | 선생님 메모 |
| createdAt | String | 이력 생성 시각 (ISO 8601) |

---

## 6. 학생별 주간 리포트 목록 조회

### 기본 정보
```
GET /v1/teachers/students/{studentUserSn}/weekly-reports
```

선생님이 특정 학생의 주간 리포트 목록을 조회합니다.

### 요청
#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studentUserSn | Long | O | 학생 user_sn |

#### Request Example
```http
GET /v1/teachers/students/2/weekly-reports
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
[
  {
    "reportId": 101,
    "startDate": "2025-12-30",
    "endDate": "2026-01-05",
    "diaryCount": 5,
    "isAnalyzed": true,
    "readYn": true,
    "createdAt": "2026-01-06T00:00:00"
  },
  {
    "reportId": 100,
    "startDate": "2025-12-23",
    "endDate": "2025-12-29",
    "diaryCount": 2,
    "isAnalyzed": false,
    "readYn": false,
    "createdAt": "2025-12-30T00:00:00"
  }
]
```

#### Response Fields
| 필드 | 타입 | 설명 |
|-----|------|------|
| reportId | Long | 리포트 ID |
| startDate | String | 시작 날짜 (월요일, yyyy-MM-dd) |
| endDate | String | 종료 날짜 (일요일, yyyy-MM-dd) |
| diaryCount | Integer | 해당 주의 일기 개수 |
| isAnalyzed | Boolean | 분석 완료 여부 (일기 3개 이상일 때만 true) |
| readYn | Boolean | 읽음 여부 |
| createdAt | String | 생성 시각 (ISO 8601) |

### 참고사항
- 최근순으로 정렬되어 반환
- 분석 완료/미완료 모두 포함

---

## 7. 학생별 주간 리포트 상세 조회

### 기본 정보
```
GET /v1/teachers/students/{studentUserSn}/weekly-reports/{reportId}
```

선생님이 특정 학생의 주간 리포트 상세를 조회합니다.

### 요청
#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studentUserSn | Long | O | 학생 user_sn |
| reportId | Long | O | 주간 리포트 ID |

#### Request Example
```http
GET /v1/teachers/students/2/weekly-reports/1
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
{
  "reportId": 1,
  "studentUserSn": 2,
  "studentName": "김학생",
  "startDate": "2025-12-30",
  "endDate": "2026-01-05",
  "diaryCount": 5,
  "isAnalyzed": true,
  "teacherReport": "이번 주 김학생은 전반적으로 긍정적인 감정을 많이 느꼈습니다...",
  "teacherTalkTip": [
    "\"이번 주에 가장 기뻤던 일이 무엇인지 이야기해볼까요?\"",
    "\"친구들과 함께 한 활동 중 어떤 것이 가장 즐거웠나요?\""
  ],
  "emotionStats": [
    {
      "emotion": "E001",
      "emotionNameKr": "기쁨",
      "color": "#FFD700",
      "count": 3,
      "percentage": 60.0
    },
    {
      "emotion": "E005",
      "emotionNameKr": "평온",
      "color": "#4ECDC4",
      "count": 2,
      "percentage": 40.0
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
      "description": "이번 주는 2가지 감정의 꽃이 피었어요. 다양한 감정을 경험한 풍성한 한 주였네요! 🌸",
      "emotionVariety": 2,
      "areaVariety": 2
    }
  },
  "readYn": true,
  "createdAt": "2026-01-06T00:00:00"
}
```

#### Response Fields

**최상위**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| reportId | Long | O | 리포트 ID |
| studentUserSn | Long | O | 학생 user_sn |
| studentName | String | O | 학생 이름 |
| startDate | String | O | 시작 날짜 (월요일) |
| endDate | String | O | 종료 날짜 (일요일) |
| diaryCount | Integer | O | 일기 개수 |
| isAnalyzed | Boolean | O | 분석 완료 여부 |
| teacherReport | String | △ | 선생님용 리포트 (미분석 시 null) |
| teacherTalkTip | Array<String> | △ | 선생님 대화 팁 (미분석 시 null) |
| emotionStats | Array | △ | 감정 통계 (미분석 시 null) |
| weeklyDiaryDetails | Array | △ | 주간 일기 상세 (미분석 시 null) |
| highlights | Object | △ | 하이라이트 (미분석 시 null) |
| readYn | Boolean | O | 읽음 여부 |
| createdAt | String | O | 생성 시각 |

**emotionStats[]**
| 필드 | 타입 | 설명 |
|-----|------|------|
| emotion | String | 감정 코드 |
| emotionNameKr | String | 감정 이름 (한글) |
| color | String | 감정 색상 (HEX) |
| count | Integer | 출현 횟수 |
| percentage | Double | 비율 (%) |

**weeklyDiaryDetails[]**
| 필드 | 타입 | 설명 |
|-----|------|------|
| diaryId | Long | 일기 ID |
| diaryDate | String | 일기 날짜 |
| coreEmotion | String | 핵심 감정 코드 |
| emotionNameKr | String | 감정 이름 (한글) |
| flowerNameKr | String | 꽃 이름 (한글) |
| flowerMeaning | String | 꽃말/의미 |
| imageFile3d | String | 3D 이미지 파일명 |

**highlights**
| 필드 | 타입 | 설명 |
|-----|------|------|
| flowerOfTheWeek | Object | 이번 주 대표 꽃 |
| quickStats | Object | 숫자로 보는 한 주 |
| gardenDiversity | Object | 감정 정원 다양성 |

### 에러 응답
```json
{
  "code": "FORBIDDEN",
  "message": "다른 반 학생의 리포트는 조회할 수 없습니다"
}
```

```json
{
  "code": "WEEKLY_REPORT_NOT_FOUND",
  "message": "주간 리포트를 찾을 수 없습니다"
}
```

---

## 8. 학생 월별 감정 조회

### 기본 정보
```
GET /v1/teachers/students/{studentUserSn}/monthly-emotions
```

선생님이 특정 학생의 월별 일기 감정 정보를 조회합니다.
- 일기 내용은 포함하지 않고 감정 정보와 꽃 상세 정보만 제공
- 분석되지 않은 일기도 포함 (isAnalyzed=false, 감정 정보는 null)

### 요청
#### Path Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| studentUserSn | Long | O | 학생 user_sn |

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| yearMonth | String | O | 조회할 년월 (YYYY-MM 형식) | 2025-12 |

#### Request Example
```http
GET /v1/teachers/students/1/monthly-emotions?yearMonth=2025-12
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
{
  "yearMonth": "2025-12",
  "totalCount": 15,
  "emotions": [
    {
      "id": 123,
      "date": "2025-12-01",
      "isAnalyzed": true,
      "coreEmotionCode": "E001",
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
      "coreEmotionDetail": {
        "emotionCode": "E001",
        "emotionNameKr": "기쁨",
        "emotionNameEn": "Joy",
        "emotionArea": "yellow",
        "flowerNameKr": "해바라기",
        "flowerNameEn": "Sunflower",
        "flowerMeaning": "긍정적인 에너지와 희망",
        "imageFile3d": "sunflower_3d.png"
      }
    },
    {
      "id": 124,
      "date": "2025-12-02",
      "isAnalyzed": false,
      "coreEmotionCode": null,
      "emotions": null,
      "coreEmotionDetail": null
    }
  ]
}
```

#### Response Fields

**최상위**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| yearMonth | String | O | 조회한 년월 (YYYY-MM) |
| totalCount | Integer | O | 해당 월 일기 총 개수 |
| emotions | Array | O | 일기 감정 목록 (날짜 내림차순) |

**emotions[] (EmotionListItem)**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| id | Long | O | 일기 ID (diary_id) |
| date | String | O | 일기 날짜 (YYYY-MM-DD) |
| isAnalyzed | Boolean | O | 분석 완료 여부 |
| coreEmotionCode | String | △ | 핵심 감정 코드 (분석 안 됐으면 null) |
| emotions | Array | △ | 감정 분포 목록 (분석 안 됐으면 null) |
| coreEmotionDetail | Object | △ | 핵심 감정 상세 정보 (분석 안 됐으면 null) |

**emotions[].emotions[] (EmotionPercent)**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| emotion | String | O | 감정 코드 (E001, E002 등) |
| percent | Integer | O | 감정 비율 (0-100) |
| color | String | O | 감정 색상 (HEX, 예: #FFD700) |
| emotionNameKr | String | O | 감정 이름 (한글) |

**emotions[].coreEmotionDetail (EmotionDetail)**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| emotionCode | String | O | 감정 코드 |
| emotionNameKr | String | O | 감정 이름 (한글) |
| emotionNameEn | String | O | 감정 이름 (영문) |
| emotionArea | String | O | 감정 영역 (red / yellow / green / blue) |
| flowerNameKr | String | O | 꽃 이름 (한글) |
| flowerNameEn | String | O | 꽃 이름 (영문) |
| flowerMeaning | String | O | 꽃말/의미 |
| imageFile3d | String | O | 3D 이미지 파일명 |

### 에러 응답
```json
{
  "code": "FORBIDDEN",
  "message": "선생님만 학생 월별 감정 정보를 조회할 수 있습니다"
}
```

```json
{
  "code": "FORBIDDEN",
  "message": "다른 반 학생의 감정 정보는 조회할 수 없습니다"
}
```

```json
{
  "code": "USER_NOT_FOUND",
  "message": "학생을 찾을 수 없습니다"
}
```

```json
{
  "code": "BAD_REQUEST",
  "message": "yearMonth 형식이 올바르지 않습니다 (YYYY-MM)"
}
```

### 참고사항
1. **일기 순서**: 날짜 내림차순 (최신순)으로 정렬되어 제공
2. **미분석 일기**: `isAnalyzed=false`일 때 `coreEmotionCode`, `emotions`, `coreEmotionDetail` 모두 null
3. **감정 분포**: `emotions` 배열의 percent 합계는 100
4. **캐싱**: 감정/꽃 마스터 데이터는 Redis 캐싱으로 성능 최적화됨

---

## 9. 학급 월별 감정 분포 조회

### 기본 정보
```
GET /v1/teachers/class/monthly-emotion-distribution
```

선생님이 담당하는 반의 월별 일자별 감정 분포를 조회합니다.

### 요청
#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| yearMonth | String | O | 조회할 년월 (YYYY-MM 형식) | 2026-01 |

#### Request Example
```http
GET /v1/teachers/class/monthly-emotion-distribution?yearMonth=2026-01
Authorization: Bearer {accessToken}
```

### 응답
#### Response Body
```json
{
  "yearMonth": "2026-01",
  "totalStudents": 25,
  "dailyDistribution": [
    {
      "date": "2026-01-01",
      "dayOfWeek": "목요일",
      "area": {
        "red": 2,
        "yellow": 8,
        "blue": 3,
        "green": 7,
        "unanalyzed": 2,
        "none": 3
      }
    },
    {
      "date": "2026-01-02",
      "dayOfWeek": "금요일",
      "area": {
        "red": 1,
        "yellow": 10,
        "blue": 4,
        "green": 6,
        "unanalyzed": 1,
        "none": 3
      }
    }
  ]
}
```

#### Response Fields

**최상위**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| yearMonth | String | O | 조회한 년월 (YYYY-MM) |
| totalStudents | Integer | O | 전체 학생 수 |
| dailyDistribution | Array | O | 일자별 감정 분포 리스트 (날짜 오름차순) |

**dailyDistribution[] (DailyDistribution)**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| date | String | O | 날짜 (YYYY-MM-DD) |
| dayOfWeek | String | O | 요일 (월요일, 화요일, ...) |
| area | Object | O | 영역별 학생 수 |

**dailyDistribution[].area (AreaDistribution)**
| 필드 | 타입 | 필수 | 설명 |
|-----|------|------|------|
| red | Integer | O | 빨강 영역 (강한 감정) 학생 수 |
| yellow | Integer | O | 노랑 영역 (활기찬 감정) 학생 수 |
| blue | Integer | O | 파랑 영역 (차분한 감정) 학생 수 |
| green | Integer | O | 초록 영역 (평온한 감정) 학생 수 |
| unanalyzed | Integer | O | 일기 작성했지만 분석 안 됨 |
| none | Integer | O | 일기 미작성 |

### 에러 응답
```json
{
  "code": "FORBIDDEN",
  "message": "선생님만 학급 월별 감정 분포를 조회할 수 있습니다"
}
```

```json
{
  "code": "NO_SCHOOL_INFO",
  "message": "학교 코드 또는 반 코드가 설정되지 않았습니다"
}
```

```json
{
  "code": "NO_STUDENTS_FOUND",
  "message": "담당 학생이 없습니다"
}
```

```json
{
  "code": "BAD_REQUEST",
  "message": "yearMonth 형식이 올바르지 않습니다 (YYYY-MM)"
}
```

### 참고사항
1. **성능 최적화**: 해당 월의 모든 일기를 1번의 쿼리로 조회 후 메모리에서 처리
2. **영역 합계**: 각 날짜별 영역별 학생 수의 합은 `totalStudents`와 동일
3. **월 전체 조회**: 해당 월의 1일부터 마지막 날까지 모든 날짜 포함
4. **캐싱**: 감정/꽃 마스터 데이터는 Redis 캐싱으로 성능 최적화됨

---

## 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|-----|-------------|------|
| FORBIDDEN | 403 | 권한 없음 (TEACHER 타입 아님 또는 다른 반 학생) |
| USER_NOT_FOUND | 404 | 사용자를 찾을 수 없음 |
| NO_SCHOOL_INFO | 400 | 학교 코드 또는 반 코드 미설정 |
| NO_STUDENTS_FOUND | 404 | 담당 학생이 없음 |
| NOT_DANGER_STATUS | 400 | DANGER 상태가 아님 |
| WEEKLY_REPORT_NOT_FOUND | 404 | 주간 리포트를 찾을 수 없음 |
| BAD_REQUEST | 400 | 잘못된 요청 |

---

## 버전 히스토리

### v1.1.0 (2026-01-11)
- 학급 월별 감정 분포 조회 API 추가
- 학생 월별 감정 조회 API에 emotionArea 필드 추가

### v1.0.0 (2026-01-09)
- 초기 버전 작성
- 8개 API 명세 문서화
