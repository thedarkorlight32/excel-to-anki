@echo off
REM Excel to Anki CLI - Command line interface for Windows
REM Usage: run-cli.bat <excel-file> [sourceColumn] [targetColumn] [sourceLang] [targetLang]

setlocal enabledelayedexpansion
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%excel-to-anki-core\target\excel-to-anki-cli.jar

if not exist "%JAR_PATH%" (
    echo Error: CLI JAR not found at %JAR_PATH%
    echo Please build the project first: mvn clean package
    exit /b 1
)

java -jar "%JAR_PATH%" %*
