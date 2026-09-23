@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0bootstrap-gradle.ps1"
if errorlevel 1 (
  echo.
  echo Bootstrap failed. Copy the error text and send it to ChatGPT.
  pause
  exit /b 1
)
echo.
echo Wrapper generated successfully.
pause
