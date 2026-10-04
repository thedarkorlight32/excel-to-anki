# Excel to Anki - Automated Setup Script for Windows
# Installs Java 21 JRE locally into this package folder

param(
    [switch]$SkipJava
)

$ErrorActionPreference = "Stop"

function Write-Success {
    Write-Host "[+] $args" -ForegroundColor Green
}

function Write-Error_ {
    Write-Host "[x] $args" -ForegroundColor Red
}

function Write-Info {
    Write-Host "[*] $args" -ForegroundColor Cyan
}

function Invoke-DownloadFile {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$DestinationPath
    )
    Write-Info "Downloading: $Url"
    Invoke-WebRequest -Uri $Url -OutFile $DestinationPath -MaximumRedirection 10
}

function Copy-DirectoryContents {
    param(
        [Parameter(Mandatory = $true)][string]$From,
        [Parameter(Mandatory = $true)][string]$To
    )
    if (-not (Test-Path $To)) {
        New-Item -ItemType Directory -Path $To | Out-Null
    }
    Get-ChildItem -LiteralPath $From -Force | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $To -Recurse -Force
    }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JreDir = Join-Path $ScriptDir "jre"
$LibDir = Join-Path $ScriptDir "lib"
$TempDir = Join-Path $env:TEMP "excel-to-anki-setup"
$JavaJreUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"

Write-Host ""
Write-Host "============================================================"
Write-Host "Excel to Anki - Automated Windows Setup"
Write-Host "============================================================"
Write-Host ""

if (Test-Path $TempDir) {
    Remove-Item -Path $TempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $TempDir | Out-Null

try {
    if (-not $SkipJava) {
        Write-Host ""
        Write-Info "Checking Java 21 JRE..."

        $javaExe = Join-Path $JreDir "bin\java.exe"
        if (Test-Path $javaExe) {
            Write-Success "Java Runtime Environment already installed"
        } else {
            Write-Info "Installing Java 21 JRE (Temurin x64)..."
            if (Test-Path $JreDir) {
                Remove-Item -Path $JreDir -Recurse -Force
            }
            New-Item -ItemType Directory -Path $JreDir | Out-Null

            $javaZip = Join-Path $TempDir "java-jre21.zip"
            $javaExtract = Join-Path $TempDir "java-extract"
            Invoke-DownloadFile -Url $JavaJreUrl -DestinationPath $javaZip
            Expand-Archive -Path $javaZip -DestinationPath $javaExtract -Force

            $javaRoot = Get-ChildItem -Path $javaExtract -Directory | Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } | Select-Object -First 1
            if (-not $javaRoot) {
                throw "Downloaded Java package does not contain bin\java.exe"
            }

            Copy-DirectoryContents -From $javaRoot.FullName -To $JreDir
            if (-not (Test-Path $javaExe)) {
                throw "Java install completed but $javaExe is missing"
            }
            Write-Success "Java 21 JRE installed"
        }
    }

    Write-Host ""
    Write-Info "Checking Application Files..."

    $uiJar = Join-Path $LibDir "excel-to-anki-ui.jar"

    $filesOk = $true
    if (Test-Path $uiJar) {
        Write-Success "UI JAR found"
    } else {
        Write-Error_ "UI JAR not found at: $uiJar"
        $filesOk = $false
    }

    $javaOk = Test-Path (Join-Path $JreDir "bin\java.exe")
    if (-not $javaOk) {
        Write-Error_ "Java is not installed"
    }

    Write-Host ""
    if ($javaOk -and $filesOk) {
        Write-Success "Setup complete"
        Write-Host ""
        Write-Info "You can now run:"
        Write-Host "  GUI:  $ScriptDir\bin\run-ui.bat"
        exit 0
    }

    Write-Error_ "Setup incomplete"
    exit 1
}
catch {
    Write-Host ""
    Write-Error_ $_.Exception.Message
    exit 1
}
finally {
    if (Test-Path $TempDir) {
        Remove-Item -Path $TempDir -Recurse -Force
    }
}
