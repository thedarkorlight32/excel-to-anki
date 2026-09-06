# Excel to Anki

Convert Excel spreadsheets into Anki flashcard decks with automatic audio synthesis and style preservation.

## Overview

This project reads an Excel file containing English and Farsi vocabulary, automatically generates audio files using Google Translate's TTS endpoint, preserves text formatting (bold, italic, underline), and creates a fully-compatible Anki package (.apkg file).

## Features

- 📊 **Excel Support**: Read vocabulary from Excel files (XLSX format)
- 🎤 **Automatic Audio**: Generate pronunciation audio using Google Translate's TTS
- 💾 **Anki Compatible**: Creates professional Anki packages with embedded media
- 🎨 **Style Preservation**: Maintains text formatting (bold, italic, underline) from Excel in flashcards
- 🌍 **Multi-language**: Supports any language pair (configurable in code)
- 📱 **Mobile Ready**: Generated .apkg files work on all Anki platforms

## Prerequisites

### System Requirements
- **Java**: JDK 11 or higher
- **Maven**: 3.6.0 or higher
- **Python**: Python 3.6+ with `genanki` library

### Install Python Dependencies
```bash
pip3 install genanki
```

## Installation & Building

### 1. Clone/Download the Project
```bash
git clone <repository-url>
cd excel-to-anki
```

### 2. Build with Maven
```bash
mvn clean package
```

This creates two executable JAR files:
- `excel-to-anki-core/target/excel-to-anki-cli.jar` - Command-line interface
- `excel-to-anki-ui/target/excel-to-anki-ui.jar` - Graphical user interface

## Quick Start

### Run the GUI (Recommended for Most Users)
```bash
# macOS/Linux
./run-ui.sh

# Windows
run-ui.bat

# Or directly with Java
java -jar excel-to-anki-ui/target/excel-to-anki-ui.jar
```

### Run the CLI (For Automation/Scripting)
```bash
# macOS/Linux
./run-cli.sh ~/vocabulary.xlsx

# Windows
run-cli.bat C:\Users\YourName\vocabulary.xlsx

# Or directly with Java
java -jar excel-to-anki-core/target/excel-to-anki-cli.jar ~/vocabulary.xlsx
```

For detailed usage instructions, see [RUNNING.md](RUNNING.md)

## Usage

### Option 1: Using the GUI (Recommended)

Build and run the UI module:

```bash
mvn clean package
java -cp excel-to-anki-ui/target/excel-to-anki-ui-1.0-SNAPSHOT.jar:excel-to-anki-core/target/excel-to-anki-core-1.0-SNAPSHOT.jar example.exceltoanki.ui.ExcelToAnkiUIKt
```

The GUI provides:
- **Upload Button**: Select an Excel file (.xlsx)
- **Source Language Dropdown**: Select source language (EN, NL, NONE)
- **Target Language Dropdown**: Select target language (EN, NL, NONE)
- **Progress Bar**: Visual feedback during conversion
- **Status Messages**: Real-time progress updates
- **Convert Button**: Start the conversion process

### Option 2: Using the Command Line

Use the core module's CLI for batch processing or scripting:

```bash
java -cp excel-to-anki-core/target/excel-to-anki-core-1.0-SNAPSHOT.jar:other-deps.jar example.exceltoanki.AppKt /path/to/your/file.xlsx [sourceColumnName] [targetColumnName] [sourceTtsLang] [targetTtsLang]
# Example using defaults (no target voice if target language not supported):
java -jar target/excel-to-anki-1.0-SNAPSHOT.jar ~/vocabulary.xlsx
# Example specifying columns and both TTS languages:
java -jar target/excel-to-anki-1.0-SNAPSHOT.jar ~/vocabulary.xlsx English Farsi en nl
```

### Output

The application will:
1. Read your Excel file
2. Generate audio files for each English term
3. Create flashcards with the formatted text and audio
4. Output an Anki package file: `<filename>.apkg`

**Console Output Example:**
```
Processed row 1: 'Hello [sound:audio_1.mp3]' -> audio_1.mp3
Processed row 2: 'Book [sound:audio_2.mp3]' -> audio_2.mp3
Wrote notes.csv: /tmp/anki_work_xxx/notes.csv
Wrote 2 media file(s) to: /tmp/anki_work_xxx/media
Created package: ~/vocabulary.apkg
Done.
```

### Import into Anki

1. Open Anki
2. Click **File > Import**
3. Select the generated `.apkg` file
4. Review the import settings and click **Import**
5. Your flashcards will appear in your selected deck

## Project Structure

The project is now organized into a multi-module Maven structure:

```
excel-to-anki/
├── pom.xml                              # Parent POM (multi-module)
├── excel-to-anki-core/                  # Core library module
│   ├── pom.xml
│   └── src/
│       └── main/kotlin/example/exceltoanki/
│           ├── App.kt                   # CLI application
│           ├── Card.kt                  # Card data model
│           ├── CardBuilder.kt           # Card creation logic
│           ├── ExcelReader.kt           # Excel reading
│           ├── AnkiWriter.kt            # CSV and media writing
│           ├── ApkgCreator.kt           # APKG creation
│           └── TextToSpeechFetcher.kt   # TTS interface and Google Translate implementation
├── excel-to-anki-ui/                    # Kotlin UI module
│   ├── pom.xml
│   └── src/
│       └── main/kotlin/example/exceltoanki/ui/
│           ├── ExcelToAnkiUI.kt         # Swing-based GUI
│           └── ConversionService.kt     # UI business logic
├── create_apkg_from_csv.py              # Python script for .apkg creation
└── README.md                            # This file
```

### Modules

#### excel-to-anki-core
The core library containing all the conversion logic:
- **App.kt**: CLI entry point for command-line usage
- **ExcelReader.kt**: Reads Excel files and extracts vocabulary pairs with formatting
- **Card.kt**: Card data model (front/back HTML with optional audio)
- **CardBuilder.kt**: Creates Card objects from Excel rows, handles TTS synthesis
- **AnkiWriter.kt**: Writes cards to CSV and media files
- **ApkgCreator.kt**: Orchestrates Python script to create/update .apkg files
- **TextToSpeechFetcher.kt**: TTS interface and Google Translate implementation

#### excel-to-anki-ui
Desktop GUI for the conversion process:
- **ExcelToAnkiUI.kt**: Swing-based graphical interface with:
  - Upload button to select Excel files
  - Language dropdown selectors (EN, NL, NONE)
  - Convert button to start conversion
  - Progress bar showing conversion progress
  - Status area displaying messages and results
- **ConversionService.kt**: Handles file selection and conversion orchestration

## Architecture & Design

The project follows a **modular Maven architecture** with clear separation of concerns:

### Core Module (`excel-to-anki-core`)
- **Purpose**: Encapsulates all conversion logic
- **Reusability**: Can be used as a library in other projects
- **Execution**: Runnable as CLI (`java -jar excel-to-anki-cli.jar`)
- **Dependencies**: Minimal - only Apache POI and Python integration

### UI Module (`excel-to-anki-ui`)
- **Purpose**: Provides user-friendly graphical interface
- **Dependencies**: Depends on core module for conversion logic
- **GUI Framework**: Swing-based (no additional dependencies)
- **Execution**: Desktop application with progress tracking

### Benefits of Modular Design
1. **Reusability**: Core logic can be integrated into other applications
2. **Separation of Concerns**: UI and business logic are cleanly separated
3. **Independent Testing**: Each module can be tested in isolation
4. **Future Extensibility**: Easy to add new UI implementations (JavaFX, Web UI, etc.)
5. **Build Flexibility**: Build only what you need

## How It Works

### 1. Excel Reading
- Reads the first sheet of the Excel file
- Preserves cell formatting (bold, italic, underline) as HTML tags
- Extracts English and Farsi text from columns

### 2. Audio Synthesis
- Uses Google Translate's public TTS endpoint
- Generates MP3 audio files for each English term
- Breaks long texts into 200-character chunks for optimal quality

### 3. Flashcard Creation
- Combines formatted text with audio using Anki HTML format
- Creates CSV with front (English + audio) and back (Farsi) fields
- Uses Python `genanki` library to package into .apkg format

### 4. Media Embedding
- Embeds all audio files directly in the .apkg package
- No external dependencies needed when importing into Anki

## Configuration

To modify language pairs, columns, or TTS languages, pass optional positional arguments to the JAR:

- Usage: java -jar target/excel-to-anki-1.0-SNAPSHOT.jar <file.xlsx> [sourceColumnName] [targetColumnName] [sourceTtsLang] [targetTtsLang]
- Defaults: sourceColumnName="English", targetColumnName="Farsi", sourceTtsLang="en", targetTtsLang=sourceTtsLang
- Supported TTS languages for the back/target field (current build): nl, en. If the targetTtsLang is not in this list, the back field will not include synthesized audio.
- TTS Engine: Parameters are in `fetchTranslateTtsChunk()` function
- Audio chunk size: Adjust `maxLen` parameter in `synthesizeWithGoogleTranslateTts()` (line ~200)

## Troubleshooting

### Issue: "Python script create_apkg_from_csv.py not found"
**Solution**: Ensure `create_apkg_from_csv.py` is in the project root or the working directory when running the JAR.

### Issue: "Failed to synthesize" errors
**Possible causes**:
- Network connectivity issues
- Google Translate endpoint rate limiting
- Invalid text encoding

**Solution**: Check your internet connection and try again. If requests are frequently blocked, consider adding delays between requests.

### Issue: Audio files sound distorted or choppy
**Solution**: This is sometimes due to MP3 concatenation. Try reducing the chunk size or testing individual words.

### Issue: "Missing required columns 'English' and/or 'Farsi'"
**Solution**: Ensure your Excel file has:
- A header row with the source and target column names (defaults: "English" and "Farsi")
- Case-insensitive match; you may pass different column names as optional arguments when running the JAR

### Issue: Maven build fails
**Possible causes**:
- Java version incompatibility
- Missing dependencies

**Solution**:
```bash
# Check Java version
java -version

# Clear Maven cache and rebuild
mvn clean install -U
```

## Dependencies

### Kotlin/Java
- **Apache POI**: Reading and processing Excel files
- **Kotlin stdlib**: Kotlin standard library

### Python
- **genanki**: Creating Anki package files
- Built on: Python 3.6+

See `pom.xml` for exact versions.

## Advanced Usage

### Custom Deck Names
The deck name is automatically set to your Excel filename. To change it, modify the Python script invocation in `App.kt`.

### Batch Processing
To process multiple Excel files:
```bash
for file in *.xlsx; do
  java -jar target/excel-to-anki-1.0-SNAPSHOT.jar "$file"
done
```

### Different Language Pairs
Modify the `synthesizeWithGoogleTranslateTts()` call and language code parameter:
- Spanish: `"es"`
- French: `"fr"`
- Arabic: `"ar"`
- And many more ISO 639-1 codes

## Performance Notes

- **First run**: May take longer due to audio generation
- **Audio quality**: Depends on Google Translate's TTS quality (usually good)
- **File size**: Typically 10-20KB per audio file
- **Processing speed**: ~2-5 seconds per row depending on text length and network

## Limitations

- Depends on Google Translate's public TTS endpoint (unofficial, may change)
- No audio rate limiting (use responsibly)
- Maximum Excel file size: Limited by available memory
- Only processes the first sheet of the Excel file

## License

[Your License Here]

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Review the code comments in `App.kt`
3. Ensure your Excel file format matches the requirements
4. Check that all dependencies are properly installed

## Changelog

### Version 1.0
- Initial release
- Excel to Anki conversion
- Audio synthesis support
- Text formatting preservation (bold, italic, underline)
- Multiple language support

