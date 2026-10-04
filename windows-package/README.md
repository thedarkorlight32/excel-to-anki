# Excel to Anki - Windows Standalone Package

This package is now **Java-only**.

## Package Structure

```
excel-to-anki-windows/
├── bin/
│   ├── run-ui.bat          # Launch GUI application
│   └── run-cli.bat         # Launch CLI application
├── lib/
│   ├── excel-to-anki-ui.jar
│   └── excel-to-anki-cli.jar
├── jre/                    # Java Runtime Environment (bundled or installed by setup)
├── setup.bat
├── setup.ps1
└── README.md
```

## Setup

1. Run `setup.bat` (or `setup.ps1`) to install/check JRE 21 in the `jre` folder.
2. Launch:
   - GUI: `bin\run-ui.bat`
   - CLI: `bin\run-cli.bat <excel-file>`

## CLI Usage

```batch
bin\run-cli.bat vocabulary.xlsx
bin\run-cli.bat vocabulary.xlsx English German en de
```

## Troubleshooting

### Java not found

Make sure `jre\bin\java.exe` exists.

### JAR not found

Make sure both files exist:

```
lib\excel-to-anki-ui.jar
lib\excel-to-anki-cli.jar
```

If needed, rebuild and copy:

```bash
mvn clean package
```

Copy from:

- `excel-to-anki-ui/target/excel-to-anki-ui.jar`
- `excel-to-anki-core/target/excel-to-anki-cli.jar`
