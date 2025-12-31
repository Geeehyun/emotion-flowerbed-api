@echo off
REM Git Hooks 설치 스크립트 (Windows용)
REM 민감정보 검증을 위한 pre-commit hook을 설치합니다.

echo 🔧 Git Hooks 설치 중...
echo.

REM Git 저장소 확인
if not exist ".git" (
    echo ❌ Git 저장소가 아닙니다. Git 저장소 루트에서 실행하세요.
    exit /b 1
)

REM hooks 디렉토리 생성
if not exist ".git\hooks" mkdir ".git\hooks"

REM pre-commit hook 복사
copy /Y scripts\pre-commit .git\hooks\pre-commit >nul

echo ✅ Git Hooks 설치 완료!
echo.
echo 📌 설치된 Hook:
echo   - pre-commit: 커밋 전 민감정보 검증
echo.
echo 💡 사용 방법:
echo   - 일반 커밋: git commit -m "메시지"
echo   - 검증 무시: git commit --no-verify (권장하지 않음)
echo.

pause
