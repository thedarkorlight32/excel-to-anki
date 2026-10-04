@echo off
REM Excel to Anki - GUI Launcher for Windows
REM This script launches the GUI application with bundled Java

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Package root sits one level above bin\
set PACKAGE_DIR=%SCRIPT_DIR%..

REM Set path for bundled JRE
set JAVA_HOME=%PACKAGE_DIR%\jre
set PATH=%JAVA_HOME%\bin;%PATH%

REM Path to the UI JAR
set UI_JAR=%PACKAGE_DIR%\lib\excel-to-anki-ui.jar

REM Check if JAR exists
if not exist "%UI_JAR%" (
    echo Error: Excel to Anki UI JAR not found at %UI_JAR%
    echo Please ensure the application is properly installed.
    pause
    exit /b 1
)

REM Check if JRE exists
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: Java Runtime Environment not found at %JAVA_HOME%
    echo Please run setup.bat to install dependencies.
    pause
    exit /b 1
)

REM Launch the GUI application
echo Starting Excel to Anki Converter...
cd /d "%SCRIPT_DIR%"
"%JAVA_HOME%\bin\java.exe" -jar "%UI_JAR%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Error: Failed to start application
    echo Error Code: %ERRORLEVEL%
    pause
)
