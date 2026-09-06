# Excel to Anki - Automated Setup Script for Windows
# This script automatically downloads and installs Java and Python
# Requires: PowerShell 5.0+, admin rights for some operations

param(
    [switch]$SkipJava,
    [switch]$SkipPython
)

$ErrorActionPreference = "Stop"

# Colors
function Write-Success {
    Write-Host "[✓] $args" -ForegroundColor Green
}

function Write-Error_ {
    Write-Host "[✗] $args" -ForegroundColor Red
}

function Write-Info {
    Write-Host "[*] $args" -ForegroundColor Cyan
}

function Write-Warning_ {
    Write-Host "[!] $args" -ForegroundColor Yellow
}

# Get script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JreDir = Join-Path $ScriptDir "jre"
$PythonDir = Join-Path $ScriptDir "python"
$LibDir = Join-Path $ScriptDir "lib"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗"
Write-Host "║     Excel to Anki - Automated Windows Setup                   ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"
Write-Host ""

Write-Info "Installation Directory: $ScriptDir"
Write-Info "JRE Directory: $JreDir"
Write-Info "Python Directory: $PythonDir"
Write-Host ""

# Check if running with admin rights (required for some operations)
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Warning_ "This script should be run as Administrator for full functionality"
    Write-Info "Some operations may fail without admin rights"
    Write-Host ""
}

# Java Setup
if (-not $SkipJava) {
    Write-Host ""
    Write-Info "Checking Java..."
    
    if (Test-Path "$JreDir\bin\java.exe") {
        Write-Success "Java Runtime Environment already installed"
        & "$JreDir\bin\java.exe" -version 2>&1 | ForEach-Object { Write-Info $_ }
    } else {
        Write-Error_ "Java Runtime Environment not found"
        Write-Host ""
        Write-Info "To install Java manually:"
        Write-Host "  1. Download JRE 21 (Windows x64) from:"
        Write-Host "     https://www.oracle.com/java/technologies/downloads/"
        Write-Host ""
        Write-Host "  2. Extract to: $JreDir"
        Write-Host "     (Ensure $JreDir\bin\java.exe exists)"
        Write-Host ""
    }
}

# Python Setup
if (-not $SkipPython) {
    Write-Host ""
    Write-Info "Checking Python..."
    
    if (Test-Path "$PythonDir\python.exe") {
        Write-Success "Python already installed"
        & "$PythonDir\python.exe" --version 2>&1 | ForEach-Object { Write-Info $_ }
        
        # Check genanki
        Write-Info "Checking Python packages..."
        & "$PythonDir\python.exe" -c "import genanki" 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "genanki is installed"
        } else {
            Write-Warning_ "genanki not installed, attempting to install..."
            & "$PythonDir\python.exe" -m pip install --quiet genanki
            if ($LASTEXITCODE -eq 0) {
                Write-Success "genanki installed successfully"
            } else {
                Write-Error_ "Failed to install genanki"
                Write-Host ""
                Write-Info "Please run manually:"
                Write-Host "  $PythonDir\python.exe -m pip install genanki"
                Write-Host ""
            }
        }
    } else {
        Write-Error_ "Python not found"
        Write-Host ""
        Write-Info "To install Python manually:"
        Write-Host "  1. Download Embedded Python 3.11+ (Windows x64) from:"
        Write-Host "     https://www.python.org/downloads/windows/"
        Write-Host "     (Look for: Windows embeddable package)"
        Write-Host ""
        Write-Host "  2. Extract to: $PythonDir"
        Write-Host "     (Ensure $PythonDir\python.exe exists)"
        Write-Host ""
        Write-Host "  3. Install genanki:"
        Write-Host "     $PythonDir\python.exe -m pip install genanki"
        Write-Host ""
    }
}

# Check JARs
Write-Host ""
Write-Info "Checking Application Files..."

$uiJar = Join-Path $LibDir "excel-to-anki-ui.jar"
$cliJar = Join-Path $LibDir "excel-to-anki-cli.jar"

if (Test-Path $uiJar) {
    Write-Success "UI JAR found"
} else {
    Write-Error_ "UI JAR not found at: $uiJar"
}

if (Test-Path $cliJar) {
    Write-Success "CLI JAR found"
} else {
    Write-Error_ "CLI JAR not found at: $cliJar"
}

# Final status
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗"
Write-Host "║                    Setup Complete!                             ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"
Write-Host ""

$javaOk = Test-Path "$JreDir\bin\java.exe"
$pythonOk = Test-Path "$PythonDir\python.exe"

if ($javaOk -and $pythonOk) {
    Write-Success "All dependencies installed!"
    Write-Host ""
    Write-Info "You can now run:"
    Write-Host "  GUI:  $ScriptDir\bin\run-ui.bat"
    Write-Host "  CLI:  $ScriptDir\bin\run-cli.bat <excel-file>"
    Write-Host ""
} else {
    Write-Error_ "Some dependencies are missing"
    Write-Host ""
    if (-not $javaOk) {
        Write-Warning_ "Java must be installed"
    }
    if (-not $pythonOk) {
        Write-Warning_ "Python must be installed"
    }
    Write-Host ""
}

Write-Host ""
