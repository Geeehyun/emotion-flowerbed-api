# Emotion Flowerbed API 문서

감정 화단 API 문서 모음입니다.

## API 명세서

### 사용자 역할별 API
- [선생님 API](./api/teacher.md) - 담당 학생 관리 및 감정 모니터링
- [학생 API](./api/student.md) - 일기 작성, 감정 분석, 주간 리포트

### 향후 추가 예정
- 관리자 API
- 공통 API (인증, 코드 조회 등)

## 문서 버전
- 작성일: 2026-01-09
- API 버전: v1

## 참고사항
- 모든 API는 JWT Bearer Token 인증이 필요합니다
- Base URL: `/api/v1`
- 응답 형식: JSON
