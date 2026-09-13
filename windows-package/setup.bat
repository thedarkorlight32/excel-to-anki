@echo off
REM Excel to Anki - Setup Script for Windows
REM Runs the PowerShell installer flow for Java 21 JRE + Python

setlocal
set SCRIPT_DIR=%~dp0

powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%setup.ps1" %*
set EXIT_CODE=%ERRORLEVEL%

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [!] Setup failed with exit code %EXIT_CODE%
    pause
)

exit /b %EXIT_CODE%
