# Running Excel to Anki

This guide explains how to run both the CLI and GUI versions of the Excel to Anki converter.

## Prerequisites

- **Java**: JDK 11 or higher installed and in PATH
- **Maven**: 3.6.0 or higher (for building from source)
- **Python**: Python 3.6+ with genanki library (for the conversion process)

### Install Python Dependencies
```bash
pip3 install genanki
```

## Building the Project

From the project root directory:

```bash
mvn clean package
```

This builds both modules and creates executable JARs:
- `excel-to-anki-core/target/excel-to-anki-cli.jar` - Command line interface
- `excel-to-anki-ui/target/excel-to-anki-ui.jar` - Graphical user interface

## Option 1: GUI (Graphical User Interface)

### On macOS/Linux

```bash
./run-ui.sh
```

### On Windows

```bash
run-ui.bat
```

### Or run directly with Java

```bash
java -jar excel-to-anki-ui/target/excel-to-anki-ui.jar
```

### GUI Features

The GUI provides an intuitive interface with:

1. **Upload Button** - Browse and select an Excel file (.xlsx or .xls)
2. **Language Dropdowns**:
   - **Source Language**: Select from EN (English), NL (Dutch), or NONE
   - **Target Language**: Select from EN (English), NL (Dutch), or NONE
3. **Convert Button** - Start the conversion process
4. **Progress Bar** - Visual progress indicator (0-100%)
5. **Status Messages** - Real-time updates on the conversion process
6. **Status Area** - Displays results and any error messages

### GUI Usage Workflow

1. Click the **Upload** button and select your Excel file
2. Select your **Source Language** (the original language in the Excel file)
3. Select your **Target Language** (the translated language in the Excel file)
4. Click the **Convert to APKG** button
5. Watch the progress bar for real-time updates
6. Once complete, the status area will show the path to the generated `.apkg` file

## Option 2: CLI (Command Line Interface)

### On macOS/Linux

```bash
./run-cli.sh <excel-file> [sourceColumn] [targetColumn] [sourceTtsLang] [targetTtsLang]
```

### On Windows

```bash
run-cli.bat <excel-file> [sourceColumn] [targetColumn] [sourceTtsLang] [targetTtsLang]
```

### Or run directly with Java

```bash
java -jar excel-to-anki-core/target/excel-to-anki-cli.jar <excel-file> [options]
```

### CLI Parameters

| Parameter | Description | Default | Example |
|-----------|-------------|---------|---------|
| `excel-file` | Path to the Excel file (required) | N/A | `/path/to/vocabulary.xlsx` |
| `sourceColumn` | Name of the source language column | "English" | "English" |
| `targetColumn` | Name of the target language column | "Farsi" | "Dutch" |
| `sourceTtsLang` | Language code for source TTS | "en" | "en", "nl" |
| `targetTtsLang` | Language code for target TTS | sourceTtsLang | "nl", "en" |

### CLI Examples

```bash
# Using all defaults (English -> Farsi, both with TTS)
./run-cli.sh ~/vocabulary.xlsx

# Specify columns and TTS languages
./run-cli.sh ~/vocabulary.xlsx English Dutch en nl

# Custom columns
./run-cli.sh ~/vocabulary.xlsx French German fr de

# No target language TTS
./run-cli.sh ~/vocabulary.xlsx English Farsi en none
```

### CLI Output

```
Wrote notes.csv: /tmp/anki_work_xxx/notes.csv
Wrote 5 media file(s) to: /tmp/anki_work_xxx/media
Created package: /home/user/vocabulary.apkg
Done.
```

## Excel File Format

Prepare your Excel file with this structure:

### Header Row (Row 1)
- Column 1: Source language header (default: "English")
- Column 2: Target language header (default: "Farsi")

### Data Rows (Row 2+)
- Each row contains a vocabulary pair
- Formatting (bold, italic, underline) is preserved in the Anki flashcard

### Example

| English | Dutch |
|---------|-------|
| Hello | Hallo |
| **Good morning** | *Goedemorgen* |
| Water | Water |

## Supported Languages

### Text-to-Speech Languages
The following language codes are supported for audio synthesis:
- `en` - English
- `nl` - Dutch
- `none` - No audio synthesis

Other languages (es, fr, de, etc.) can be used, but may not have audio support depending on the TTS provider.

## Importing into Anki

After conversion, you'll have a `.apkg` file:

1. Open **Anki Desktop**
2. Go to **File** → **Import**
3. Select the generated `.apkg` file
4. Review the import settings and click **Import**
5. Your flashcards will be added to your selected deck

## Troubleshooting

### "JAR not found" Error
**Cause**: The executable JAR wasn't built
**Solution**: Run `mvn clean package` to build the project

### "Java not found" Error
**Cause**: Java is not installed or not in PATH
**Solution**: Install JDK 11+ and add it to your PATH

### "Python script not found" Error
**Cause**: `create_apkg_from_csv.py` is missing from the project root
**Solution**: Ensure the Python script is in the same directory as the project root

### Audio Synthesis Fails
**Cause**: Network issues or rate limiting from Google Translate
**Solution**: 
- Check your internet connection
- Wait a few minutes and try again
- Test with a smaller Excel file

### "Missing required columns" Error
**Cause**: Excel file doesn't have the expected column names
**Solution**: 
- Add a header row with column names (default: "English" and "Farsi")
- Or pass the correct column names as CLI parameters

## Performance Tips

- **First conversion**: May take longer as audio files are generated (2-5 seconds per row)
- **Large files**: Process files with 100+ rows may take 5-10 minutes
- **Network**: A stable internet connection is required for TTS audio synthesis
- **Batch processing**: Use the CLI for automating conversions of multiple files

## Advanced Usage

### Batch Processing (Linux/macOS)
```bash
for file in *.xlsx; do
  ./run-cli.sh "$file"
done
```

### Batch Processing (Windows)
```batch
for %%f in (*.xlsx) do (
  call run-cli.bat "%%f"
)
```

### Custom Languages
Edit the column name mappings in the `ConversionService.kt` file to add support for additional languages.
