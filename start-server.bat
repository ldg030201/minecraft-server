@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 최신 버전 확인 중...
docker compose pull
echo 서버 시작 중...
docker compose up -d
echo 완료! 서버가 시작되었습니다.
echo 대시보드: http://localhost:8080
pause
