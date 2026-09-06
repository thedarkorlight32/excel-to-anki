@echo off
REM Excel to Anki - Setup Script for Windows
REM This script downloads and configures Java and Python for standalone use

setlocal enabledelayedexpansion

color 0A
cls

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║     Excel to Anki - Windows Setup & Configuration             ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

set SCRIPT_DIR=%~dp0
set JRE_DIR=%SCRIPT_DIR%jre
set PYTHON_DIR=%SCRIPT_DIR%python
set BIN_DIR=%SCRIPT_DIR%bin

echo [*] Installation Directory: %SCRIPT_DIR%
echo.

REM Check if already installed
if exist "%JRE_DIR%\bin\java.exe" (
    echo [✓] Java Runtime Environment already installed
) else (
    echo [!] Java Runtime Environment not found
    echo.
    echo To complete the setup manually:
    echo.
    echo 1. Download JRE 21 (Windows x64) from:
    echo    https://www.oracle.com/java/technologies/downloads/
    echo.
    echo 2. Extract the JRE to: %JRE_DIR%
    echo    (So the structure is: %JRE_DIR%\bin\java.exe)
    echo.
    echo 3. Run this script again
    echo.
    pause
)

if exist "%PYTHON_DIR%\python.exe" (
    echo [✓] Python Runtime already installed
) else (
    echo [!] Python Runtime not found
    echo.
    echo To complete the setup manually:
    echo.
    echo 1. Download Embedded Python 3.11+ (Windows x64) from:
    echo    https://www.python.org/downloads/windows/
    echo    (Look for "Windows embeddable package")
    echo.
    echo 2. Extract the Python to: %PYTHON_DIR%
    echo    (So the structure is: %PYTHON_DIR%\python.exe)
    echo.
    echo 3. Install genanki by running in Command Prompt:
    echo    %PYTHON_DIR%\python.exe -m pip install genanki
    echo.
    echo 4. Run this script again
    echo.
    pause
)

echo.
echo Checking installation...
echo.

if exist "%JRE_DIR%\bin\java.exe" (
    for /f "tokens=*" %%i in ('"%JRE_DIR%\bin\java.exe" -version 2^>^&1 ^| find /v ""') do (
        echo [✓] %%i
    )
) else (
    echo [✗] Java not found - please install JRE
)

if exist "%PYTHON_DIR%\python.exe" (
    echo [✓] Python is installed
    
    REM Try to install genanki if not present
    echo [*] Checking Python packages...
    "%PYTHON_DIR%\python.exe" -c "import genanki" >nul 2>&1
    if errorlevel 1 (
        echo [!] Installing genanki...
        "%PYTHON_DIR%\python.exe" -m pip install --quiet genanki
        if !ERRORLEVEL! equ 0 (
            echo [✓] genanki installed successfully
        ) else (
            echo [✗] Failed to install genanki
            echo    Please run manually:
            echo    %PYTHON_DIR%\python.exe -m pip install genanki
        )
    ) else (
        echo [✓] genanki is installed
    )
) else (
    echo [✗] Python not found - please install Python
)

echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║                   Setup Complete!                              ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.
echo You can now run:
echo.
echo   GUI:  %BIN_DIR%\run-ui.bat
echo   CLI:  %BIN_DIR%\run-cli.bat ^<excel-file^>
echo.
echo.
pause
