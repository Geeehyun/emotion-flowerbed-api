#!/bin/bash
#
# Git Hooks 설치 스크립트
# 민감정보 검증을 위한 pre-commit hook을 설치합니다.
#

echo "🔧 Git Hooks 설치 중..."

# Git 저장소 확인
if [ ! -d ".git" ]; then
    echo "❌ Git 저장소가 아닙니다. Git 저장소 루트에서 실행하세요."
    exit 1
fi

# hooks 디렉토리 생성
mkdir -p .git/hooks

# pre-commit hook 복사
cp scripts/pre-commit .git/hooks/pre-commit

# 실행 권한 부여
chmod +x .git/hooks/pre-commit

echo "✅ Git Hooks 설치 완료!"
echo ""
echo "📌 설치된 Hook:"
echo "  - pre-commit: 커밋 전 민감정보 검증"
echo ""
echo "💡 사용 방법:"
echo "  - 일반 커밋: git commit -m \"메시지\""
echo "  - 검증 무시: git commit --no-verify (권장하지 않음)"
echo ""
