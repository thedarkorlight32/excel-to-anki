# Excel to Anki - Automated Setup Script for Windows
# Installs Java 21 JRE and Python 3 locally into this package folder

param(
    [switch]$SkipJava,
    [switch]$SkipPython
)

$ErrorActionPreference = "Stop"

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

# Get script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JreDir = Join-Path $ScriptDir "jre"
$PythonDir = Join-Path $ScriptDir "python"
$LibDir = Join-Path $ScriptDir "lib"
$TempDir = Join-Path $env:TEMP "excel-to-anki-setup"

# Download locations
$JavaJreUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse"
$PythonVersion = "3.12.10"
$PythonInstallerUrl = "https://www.python.org/ftp/python/$PythonVersion/python-$PythonVersion-amd64.exe"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗"
Write-Host "║     Excel to Anki - Automated Windows Setup                   ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"
Write-Host ""

Write-Info "Installation Directory: $ScriptDir"
Write-Info "JRE Directory: $JreDir"
Write-Info "Python Directory: $PythonDir"
Write-Host ""

if (Test-Path $TempDir) {
    Remove-Item -Path $TempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $TempDir | Out-Null

try {
    # Java Setup
    if (-not $SkipJava) {
        Write-Host ""
        Write-Info "Checking Java 21 JRE..."

        $javaExe = Join-Path $JreDir "bin\java.exe"
        if (Test-Path $javaExe) {
            Write-Success "Java Runtime Environment already installed"
            & $javaExe -version 2>&1 | ForEach-Object { Write-Info $_ }
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
            & $javaExe -version 2>&1 | ForEach-Object { Write-Info $_ }
        }
    }

    # Python Setup
    if (-not $SkipPython) {
        Write-Host ""
        Write-Info "Checking Python..."

        $pythonExe = Join-Path $PythonDir "python.exe"
        if (Test-Path $pythonExe) {
            Write-Success "Python already installed"
            & $pythonExe --version 2>&1 | ForEach-Object { Write-Info $_ }
        } else {
            Write-Info "Installing Python $PythonVersion (x64)..."
            if (Test-Path $PythonDir) {
                Remove-Item -Path $PythonDir -Recurse -Force
            }
            New-Item -ItemType Directory -Path $PythonDir | Out-Null

            $pythonInstaller = Join-Path $TempDir "python-installer.exe"
            Invoke-DownloadFile -Url $PythonInstallerUrl -DestinationPath $pythonInstaller

            $pythonArgs = @(
                "/quiet",
                "InstallAllUsers=0",
                "TargetDir=$PythonDir",
                "Include_pip=1",
                "PrependPath=0",
                "Include_test=0",
                "Include_launcher=0",
                "InstallLauncherAllUsers=0",
                "Shortcuts=0"
            )

            $installProc = Start-Process -FilePath $pythonInstaller -ArgumentList $pythonArgs -Wait -PassThru -NoNewWindow
            if ($installProc.ExitCode -ne 0) {
                throw "Python installer failed with exit code $($installProc.ExitCode)"
            }

            if (-not (Test-Path $pythonExe)) {
                throw "Python install completed but $pythonExe is missing"
            }
            Write-Success "Python installed"
            & $pythonExe --version 2>&1 | ForEach-Object { Write-Info $_ }
        }

        # Create python3 command for compatibility with the app's ProcessBuilder("python3", ...)
        $python3Shim = Join-Path $PythonDir "python3.cmd"
        Set-Content -Path $python3Shim -Encoding ASCII -Value "@echo off`r`n`"%~dp0python.exe`" %*`r`n"
        Write-Success "Created python3 command shim"

        # Ensure pip and install dependencies
        & $pythonExe -m pip --version *> $null
        if ($LASTEXITCODE -ne 0) {
            Write-Warning_ "pip not detected, running ensurepip..."
            & $pythonExe -m ensurepip --upgrade
            if ($LASTEXITCODE -ne 0) {
                throw "Failed to bootstrap pip"
            }
        }

        Write-Info "Installing Python package: genanki"
        & $pythonExe -m pip install --upgrade pip
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to upgrade pip"
        }

        & $pythonExe -m pip install genanki
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to install genanki"
        }

        & $pythonExe -c "import genanki"
        if ($LASTEXITCODE -ne 0) {
            throw "genanki import check failed after installation"
        }
        Write-Success "genanki is installed"
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

    $javaOk = Test-Path (Join-Path $JreDir "bin\java.exe")
    $pythonOk = Test-Path (Join-Path $PythonDir "python.exe")

    if ($javaOk -and $pythonOk) {
        Write-Success "All dependencies installed!"
        Write-Host ""
        Write-Info "You can now run:"
        Write-Host "  GUI:  $ScriptDir\bin\run-ui.bat"
        Write-Host "  CLI:  $ScriptDir\bin\run-cli.bat [excel-file]"
        Write-Host ""
        exit 0
    } else {
        Write-Error_ "Some dependencies are missing"
        if (-not $javaOk) {
            Write-Warning_ "Java must be installed"
        }
        if (-not $pythonOk) {
            Write-Warning_ "Python must be installed"
        }
        exit 1
    }
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
