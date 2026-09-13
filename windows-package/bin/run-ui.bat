@echo off
REM Excel to Anki - GUI Launcher for Windows
REM This script launches the GUI application with bundled Java

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Package root sits one level above bin\
set PACKAGE_DIR=%SCRIPT_DIR%..

REM Set paths for bundled JRE and Python
set JAVA_HOME=%PACKAGE_DIR%\jre
set PYTHON_HOME=%PACKAGE_DIR%\python
set PATH=%JAVA_HOME%\bin;%PYTHON_HOME%;%PYTHON_HOME%\Scripts;%PATH%

REM Path to the UI JAR
set UI_JAR=%PACKAGE_DIR%\lib\excel-to-anki-ui.jar
set PYTHON_EXE=%PYTHON_HOME%\python.exe
set PIP_EXE=%PYTHON_HOME%\Scripts\pip.exe

REM Check if JAR exists
if not exist "%UI_JAR%" (
    echo Error: Excel to Anki UI JAR not found at %UI_JAR%
    echo Please ensure the application is properly installed.
    pause
    exit /b 1
)

REM Check if JRE exists
if not exist "%JAVA_HOME%\java.exe" (
    echo Error: Java Runtime Environment not found at %JAVA_HOME%
    echo Please run setup.bat to install dependencies.
    pause
    exit /b 1
)

if not exist "%PYTHON_EXE%" (
    echo Error: Python not found at %PYTHON_EXE%
    echo Please run setup.bat to install dependencies.
    pause
    exit /b 1
)

REM Ensure genanki is installed with the bundled Python
"%PYTHON_EXE%" -c "import genanki" >nul 2>&1
if errorlevel 1 (
    echo Installing genanki...
    if exist "%PIP_EXE%" (
        "%PIP_EXE%" install --upgrade genanki
    ) else (
        "%PYTHON_EXE%" -m pip install --upgrade genanki
    )
    if errorlevel 1 (
        echo Error: Failed to install genanki
        pause
        exit /b 1
    )
)

REM Launch the GUI application
echo Starting Excel to Anki Converter...
cd /d "%SCRIPT_DIR%"
"%JAVA_HOME%\java.exe" -jar "%UI_JAR%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo Error: Failed to start application
    echo Error Code: %ERRORLEVEL%
    pause
)
