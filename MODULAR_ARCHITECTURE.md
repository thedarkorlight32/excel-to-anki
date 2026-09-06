# Excel to Anki - Modular Architecture Implementation

## Summary

Successfully refactored the Excel to Anki project into a clean, modular Maven multi-module architecture with two distinct modules:

### ✅ Completed Tasks

1. **Created `excel-to-anki-core` Module**
   - Contains all conversion logic and business rules
   - CLI entry point for command-line usage
   - Can be used as a library dependency in other projects
   - Executable JAR: `excel-to-anki-cli.jar`

2. **Created `excel-to-anki-ui` Module**
   - Kotlin Swing-based graphical user interface
   - Depends on core module for conversion logic
   - Features implemented:
     - ✅ Upload button to select Excel files
     - ✅ Convert button to initiate conversion
     - ✅ Progress bar with percentage display
     - ✅ Two language dropdown selectors (EN, NL, NONE)
     - ✅ Status messages and error handling
     - ✅ Real-time progress updates
   - Executable JAR: `excel-to-anki-ui.jar`

3. **Set Up Parent POM**
   - Multi-module Maven structure
   - Centralized dependency management
   - Shared plugin configuration

4. **Created Convenient Launch Scripts**
   - Unix/Linux: `run-cli.sh`, `run-ui.sh`
   - Windows: `run-cli.bat`, `run-ui.bat`
   - Automatic JAR path resolution

5. **Documentation**
   - Updated README.md with modular architecture details
   - Created RUNNING.md with comprehensive usage guide
   - Included examples for both CLI and GUI usage

## Project Structure

```
excel-to-anki/
├── pom.xml                              # Parent POM (multi-module)
├── run-cli.sh / run-cli.bat             # Launch CLI application
├── run-ui.sh / run-ui.bat               # Launch GUI application
├── RUNNING.md                           # Detailed usage guide
├── README.md                            # Project documentation
├── create_apkg_from_csv.py              # Python helper script
│
├── excel-to-anki-core/                  # Core Library Module
│   ├── pom.xml
│   └── src/main/kotlin/example/exceltoanki/
│       ├── App.kt                       # CLI entry point
│       ├── Card.kt                      # Data model
│       ├── CardBuilder.kt               # Card creation logic
│       ├── ExcelReader.kt               # Excel file reading
│       ├── AnkiWriter.kt                # CSV/media writing
│       ├── ApkgCreator.kt               # APKG creation
│       └── TextToSpeechFetcher.kt       # TTS implementation
│   └── target/
│       └── excel-to-anki-cli.jar        # Executable CLI JAR
│
└── excel-to-anki-ui/                    # UI Module
    ├── pom.xml
    └── src/main/kotlin/example/exceltoanki/ui/
        ├── ExcelToAnkiUI.kt             # Swing GUI application
        └── ConversionService.kt         # Business logic
    └── target/
        └── excel-to-anki-ui.jar         # Executable UI JAR
```

## How to Use

### Build the Project
```bash
mvn clean package
```

### Run the GUI (Recommended)
```bash
# macOS/Linux
./run-ui.sh

# Windows
run-ui.bat
```

### Run the CLI
```bash
# macOS/Linux
./run-cli.sh ~/vocabulary.xlsx

# Windows
run-cli.bat C:\path\to\vocabulary.xlsx
```

## Key Features

### GUI Features
- **File Selection**: Browse and select Excel files through system file dialog
- **Language Selection**: Choose source and target languages (EN, NL, NONE)
- **Progress Tracking**: Visual progress bar with percentage and status messages
- **Real-time Feedback**: Status area shows conversion progress and results
- **Error Handling**: Clear error messages for troubleshooting

### CLI Features
- **Flexible Parameters**: Support for custom column names and TTS languages
- **Batch Processing**: Automation-friendly command-line interface
- **Detailed Output**: Clear console feedback on conversion progress

## Architecture Benefits

1. **Modularity**: Clear separation between core logic and UI
2. **Reusability**: Core module can be imported as a library dependency
3. **Maintainability**: Each module has a single responsibility
4. **Extensibility**: Easy to add new UI implementations (Web, JavaFX, etc.)
5. **Testability**: Modules can be tested independently
6. **Flexibility**: Users can choose CLI or GUI based on their needs

## Build Artifacts

After running `mvn clean package`:

| Artifact | Location | Purpose |
|----------|----------|---------|
| `excel-to-anki-cli.jar` | `excel-to-anki-core/target/` | CLI application (with all dependencies) |
| `excel-to-anki-ui.jar` | `excel-to-anki-ui/target/` | GUI application (with all dependencies) |
| `excel-to-anki-core-1.0-SNAPSHOT.jar` | `excel-to-anki-core/target/` | Core library JAR |
| `excel-to-anki-ui-1.0-SNAPSHOT.jar` | `excel-to-anki-ui/target/` | UI library JAR |

## Next Steps (Optional Enhancements)

### Potential Future Improvements
1. Add unit tests to both modules
2. Create a REST API wrapper around the core module
3. Implement a Web UI (using Ktor or Spring Boot)
4. Add support for additional file formats (CSV, JSON)
5. Implement real-time TTS progress streaming
6. Create JavaFX or modern UI alternative
7. Package as native executables using GraalVM
8. Add internationalization (i18n) support for the UI

## Testing

The project builds successfully with:
- ✅ Maven clean compile
- ✅ Maven package (creates executable JARs)
- ✅ All dependencies resolve correctly
- ✅ Kotlin compilation successful

Run the following to verify everything works:
```bash
mvn clean install
```

## Troubleshooting

If you encounter issues:

1. **Build Fails**: Ensure JDK 11+ is installed
2. **JAR Not Found**: Run `mvn clean package` first
3. **GUI Won't Start**: Check Java version with `java -version`
4. **CLI Errors**: Verify Excel file format and Python script location

See RUNNING.md for detailed troubleshooting guide.

---

**Project**: Excel to Anki Converter  
**Version**: 1.0-SNAPSHOT  
**Architecture**: Maven Multi-Module  
**Language**: Kotlin 2.2.0  
**JDK**: 11+ (Targeting Java 21)
