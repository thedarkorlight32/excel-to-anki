#!/bin/bash
# Excel to Anki CLI - Command line interface
# Usage: ./run-cli.sh <excel-file> [sourceColumn] [targetColumn] [sourceLang] [targetLang]

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_PATH="$SCRIPT_DIR/excel-to-anki-core/target/excel-to-anki-cli.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: CLI JAR not found at $JAR_PATH"
    echo "Please build the project first: mvn clean package"
    exit 1
fi

java -jar "$JAR_PATH" "$@"
