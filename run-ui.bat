@echo off
REM Excel to Anki UI - Graphical user interface for Windows
REM Usage: run-ui.bat

setlocal enabledelayedexpansion
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%excel-to-anki-ui\target\excel-to-anki-ui.jar

if not exist "%JAR_PATH%" (
    echo Error: UI JAR not found at %JAR_PATH%
    echo Please build the project first: mvn clean package
    exit /b 1
)

java -jar "%JAR_PATH%"
