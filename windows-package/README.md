# Excel to Anki - Windows Standalone Package

This directory contains everything needed to create a standalone Windows package that requires **no pre-installed Java or Python**.

## Package Structure

```
excel-to-anki-windows/
├── bin/
│   ├── run-ui.bat          # Launch GUI application
│   └── run-cli.bat         # Launch CLI application
├── lib/
│   ├── excel-to-anki-ui.jar    # GUI application JAR
│   └── excel-to-anki-cli.jar   # CLI application JAR
├── jre/                    # Java Runtime Environment (to be bundled)
├── python/                 # Python Runtime (to be bundled)
├── setup.bat              # Batch setup script
├── setup.ps1              # PowerShell setup script
└── README.md              # This file
```

## Setup Instructions

### Option 1: Automated Setup (PowerShell)

**Easiest Method** - Automatically checks and configures everything.

```powershell
# Run as Administrator
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process -Force
.\setup.ps1
```

### Option 2: Manual Batch Setup

Simple batch file for manual guidance:

```batch
setup.bat
```

### Option 3: Manual Setup

Follow these steps to manually set up:

#### Step 1: Install Java Runtime Environment (JRE)

1. Download **JRE 21** from: https://www.oracle.com/java/technologies/downloads/
   - Select: Windows x64 (jre-21_windows-x64_bin.zip)

2. Extract to the `jre` directory:
   ```
   jre/
   ├── bin/
   │   └── java.exe
   ├── lib/
   └── ...
   ```

   **Important:** The structure should be `jre\bin\java.exe`, not `jre\jre-21\bin\java.exe`

#### Step 2: Install Python Runtime

1. Download **Embedded Python** from: https://www.python.org/downloads/windows/
   - Look for: "Windows embeddable package (64-bit)"
   - Select Python 3.11 or later

2. Extract to the `python` directory:
   ```
   python/
   ├── python.exe
   ├── python311.dll
   └── ...
   ```

#### Step 3: Install Python Package Dependencies

Open Command Prompt and run:

```batch
python\python.exe -m pip install genanki
```

If you get an error about pip not being found, create `python\pyvenv.cfg`:

```
home = C:\path\to\excel-to-anki-windows\python
```

Then run:

```batch
python\python.exe -m pip install --upgrade pip
python\python.exe -m pip install genanki
```

## Usage

After setup is complete:

### Run GUI Application

Double-click `run-ui.bat` or open Command Prompt:

```batch
bin\run-ui.bat
```

The graphical interface will appear with:
- File upload button
- Language selection dropdowns
- Convert button
- Progress bar
- Status messages

### Run CLI Application

Open Command Prompt in the package directory:

```batch
REM Convert with defaults (English source, Dutch target)
bin\run-cli.bat vocabulary.xlsx

REM Specify source and target languages
bin\run-cli.bat vocabulary.xlsx English German en de

REM Get help
bin\run-cli.bat
```

## Creating a Portable ZIP Package

To create a distributable ZIP file:

### Windows (PowerShell)

```powershell
# From the windows-package directory
Compress-Archive -Path @("bin", "lib", "setup.bat", "setup.ps1", "README.md") `
                  -DestinationPath "excel-to-anki-windows.zip" `
                  -Force

# Users then need to:
# 1. Extract the ZIP
# 2. Run setup.bat or setup.ps1
```

### Windows (Command Prompt)

```batch
REM Requires 7-Zip, WinRAR, or built-in compression
REM Use Windows Explorer to create the ZIP, or use a tool like 7-Zip
```

## Creating a Windows Installer (NSIS)

For a professional installer, use NSIS (Nullsoft Scriptable Install System):

1. Install NSIS from: http://nsis.sourceforge.net/

2. Create `installer.nsi`:

```nsis
!include "MUI2.nsh"

Name "Excel to Anki"
OutFile "excel-to-anki-installer.exe"
InstallDir "$PROGRAMFILES\ExcelToAnki"

!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"

Section "Install"
  SetOutPath "$INSTDIR"
  File /r "bin\*.*"
  File /r "lib\*.*"
  File /r "jre\*.*"
  File /r "python\*.*"
  
  CreateDirectory "$SMPROGRAMS\Excel to Anki"
  CreateShortCut "$SMPROGRAMS\Excel to Anki\Excel to Anki.lnk" \
                 "$INSTDIR\bin\run-ui.bat"
  CreateShortCut "$SMPROGRAMS\Excel to Anki\Uninstall.lnk" \
                 "$INSTDIR\uninstall.exe"
SectionEnd

Section "Uninstall"
  RMDir /r "$INSTDIR"
  RMDir /r "$SMPROGRAMS\Excel to Anki"
SectionEnd
```

3. Compile with NSIS to create `excel-to-anki-installer.exe`

## Creating an EXE Wrapper (Launch4j)

For an even more user-friendly experience, wrap the JAR in an EXE:

1. Download Launch4j from: http://launch4j.sourceforge.net/

2. Configure for `excel-to-anki-ui.jar` with bundled JRE path

3. This creates a native Windows EXE that looks and feels like a regular application

## Troubleshooting

### Error: "Java not found"

**Solution:** Ensure Java is installed in the `jre` directory with this structure:
```
jre/
├── bin/
│   └── java.exe
└── lib/
```

### Error: "Python not found"

**Solution:** Ensure Python is installed in the `python` directory:
```
python/
├── python.exe
├── python311.dll (or similar)
└── Lib/
```

### Error: "create_apkg_from_csv.py not found"

**Solution:** Ensure the file exists in the package root directory. It should be located where `run-ui.bat` and `run-cli.bat` can find it.

### genanki Import Error

**Solution:** Install genanki in the bundled Python:

```batch
cd path\to\excel-to-anki-windows
python\python.exe -m pip install --upgrade genanki
```

### JAR Not Found

**Solution:** Ensure the JAR files exist in the `lib` directory:
```
lib/
├── excel-to-anki-ui.jar
└── excel-to-anki-cli.jar
```

Build them from source:
```bash
mvn clean package
```

Then copy from:
- `excel-to-anki-ui/target/excel-to-anki-ui.jar`
- `excel-to-anki-core/target/excel-to-anki-cli.jar`

## Distribution

### Recommended Approaches:

1. **Portable ZIP** (Easiest)
   - Users extract and run `setup.bat`
   - Minimal file size if JRE/Python not bundled
   - Users install Java and Python themselves

2. **Bundled ZIP** (User-Friendly)
   - Include JRE and Python in the ZIP
   - Large file size (~400-500 MB)
   - "Just extract and use" experience

3. **NSIS Installer** (Professional)
   - Professional installer interface
   - Registry entries and Start Menu shortcuts
   - Can include automatic dependency downloading

4. **Standalone EXE** (Premium)
   - Single executable file
   - Use Launch4j to wrap the JAR
   - Best user experience

## Building for Production

### Complete Package Builder Script

Create `build-windows-package.bat`:

```batch
@echo off
REM Build Windows standalone package

echo Building Excel to Anki JARs...
mvn clean package -q

echo Copying files to windows-package...
copy excel-to-anki-ui\target\excel-to-anki-ui.jar windows-package\lib\
copy excel-to-anki-core\target\excel-to-anki-cli.jar windows-package\lib\
copy create_apkg_from_csv.py windows-package\

echo Package ready at: windows-package\
echo Next steps:
echo 1. Run setup.bat to install Java and Python
echo 2. Distribute as ZIP or create NSIS installer
```

## Support & Resources

- **Java Downloads:** https://www.oracle.com/java/technologies/downloads/
- **Python Downloads:** https://www.python.org/downloads/
- **genanki Documentation:** https://github.com/kerrickstaley/genanki
- **Launch4j:** http://launch4j.sourceforge.net/
- **NSIS:** http://nsis.sourceforge.net/

## License

[Your License Here]

---

**Note:** Ensure all components (Java, Python, genanki) comply with their respective licenses when distributing.
