# Git Hooks - 민감정보 검증

## 개요

커밋 전에 자동으로 민감정보 포함 여부를 검사하여 실수로 API 키, 비밀번호 등이 Git에 올라가는 것을 방지합니다.

## 설치 방법

### Windows
```bash
# Git Bash 또는 PowerShell에서
scripts\install-hooks.bat
```

### Mac/Linux
```bash
bash scripts/install-hooks.sh
```

## 검증 항목

✅ **자동으로 검증하는 민감정보:**

- Anthropic API Key (sk-ant-api03-...)
- OpenAI API Key (sk-...)
- AWS Access Key (AKIA...)
- Private Key (-----BEGIN PRIVATE KEY-----)
- 하드코딩된 비밀번호
- JWT Secret (base64)
- RDS 엔드포인트 (운영 DB 주소)
- Private IP (172.31.x.x)

## 사용 방법

### 정상적인 커밋
```bash
git add .
git commit -m "feat: 새 기능 추가"
# → 자동으로 민감정보 검증 실행
```

### 민감정보 발견 시
```
❌ src/main/resources/application.yml
   → 발견: Anthropic API Key

🚨 커밋 거부: 민감정보가 포함되어 있습니다!

해결 방법:
  1. 하드코딩된 민감정보를 환경변수 참조로 변경
     예: key: ${SPRING_ANTHROPIC_API_KEY}
```

### 검증 통과 시
```
✅ 민감정보 검증 통과!
```

### 검증 무시 (권장하지 않음)
```bash
git commit --no-verify -m "메시지"
```

## 예외 처리

다음 파일들은 자동으로 제외됩니다:
- `.gitignore`에 포함된 파일
- 삭제된 파일

## 문제 해결

### Hook이 실행되지 않는 경우
```bash
# 실행 권한 확인
ls -la .git/hooks/pre-commit

# 실행 권한 부여
chmod +x .git/hooks/pre-commit
```

### Windows에서 실행 오류
- Git Bash 사용 권장
- 또는 WSL 환경에서 실행

## 팀 협업

**프로젝트를 새로 clone한 팀원은 반드시 Hook을 설치해야 합니다:**

```bash
# 저장소 clone 후
git clone https://github.com/Geeehyun/emotion-flowerbed-api.git
cd emotion-flowerbed-api

# Hook 설치
bash scripts/install-hooks.sh
```

`.git/hooks` 디렉토리는 Git에 포함되지 않으므로, 각 개발자가 개별적으로 설치해야 합니다.
