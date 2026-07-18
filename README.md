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

This creates an executable JAR file at `target/excel-to-anki-1.0-SNAPSHOT.jar`

## Usage

### Prepare Your Excel File

1. Create an Excel file (.xlsx) with the following structure:
   - **First row**: Headers for your source and target columns (defaults: "English" and "Farsi")
   - **Subsequent rows**: Your vocabulary pairs

**Example Excel Structure:**

| English | Farsi |
|---------|-------|
| Hello | سلام |
| **Book** | *کتاب* |
| <u>Water</u> | آب |

**Notes on Formatting:**
- You can apply **bold**, *italic*, or <u>underline</u> formatting to any text in Excel
- These styles will be preserved in your Anki flashcards
- Only the text content is used for audio synthesis (styles are not read aloud)

### Run the Application

```bash
java -jar target/excel-to-anki-1.0-SNAPSHOT.jar /path/to/your/file.xlsx [sourceColumnName] [targetColumnName] [sourceTtsLang] [targetTtsLang]
# Example using defaults (no target voice if target language not supported):
java -jar target/excel-to-anki-1.0-SNAPSHOT.jar ~/vocabulary.xlsx
# Example specifying columns and both TTS languages:
java -jar target/excel-to-anki-1.0-SNAPSHOT.jar ~/vocabulary.xlsx English Farsi en nl
```

**Example:**
```bash
java -jar target/excel-to-anki-1.0-SNAPSHOT.jar ~/vocabulary.xlsx
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

```
excel-to-anki/
├── src/
│   ├── main/
│   │   └── kotlin/example/exceltoanki/
│   │       └── App.kt              # Main application code
│   └── test/
│       └── kotlin/                 # Test files
├── create_apkg_from_csv.py         # Python script for .apkg creation
├── pom.xml                         # Maven configuration
└── README.md                        # This file
```

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

