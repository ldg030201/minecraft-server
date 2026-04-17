@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 서버 종료 중...
docker compose down
echo 종료 완료.
pause
