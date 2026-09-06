@echo off
REM Excel to Anki - CLI Launcher for Windows
REM This script launches the CLI application with bundled Java and Python

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0..

REM Set paths for bundled JRE and Python
set JAVA_HOME=%SCRIPT_DIR%\jre
set PYTHON_HOME=%SCRIPT_DIR%\python
set PATH=%JAVA_HOME%\bin;%PYTHON_HOME%;%PYTHON_HOME%\Scripts;%PATH%

REM Path to the CLI JAR
set CLI_JAR=%SCRIPT_DIR%\lib\excel-to-anki-cli.jar

REM Check if JAR exists
if not exist "%CLI_JAR%" (
    echo Error: Excel to Anki CLI JAR not found at %CLI_JAR%
    echo Please ensure the application is properly installed.
    exit /b 1
)

REM Check if JRE exists
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: Java Runtime Environment not found at %JAVA_HOME%
    echo Please run setup.bat to install dependencies.
    exit /b 1
)

REM Launch the CLI application with passed arguments
cd /d "%SCRIPT_DIR%"
"%JAVA_HOME%\bin\java.exe" -jar "%CLI_JAR%" %*

if %ERRORLEVEL% neq 0 (
    exit /b %ERRORLEVEL%
)
