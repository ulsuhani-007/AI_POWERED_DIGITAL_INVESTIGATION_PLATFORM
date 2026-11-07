@echo off
setlocal
cd /d "%~dp0"
echo ====================================================================
echo   AI-Powered Digital Investigation Platform - Portable Launcher
echo ====================================================================
echo Launching start-platform.ps1...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-platform.ps1" %*
endlocal

