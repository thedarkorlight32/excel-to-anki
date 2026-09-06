#!/bin/bash
# Excel to Anki UI - Graphical user interface
# Usage: ./run-ui.sh

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR_PATH="$SCRIPT_DIR/excel-to-anki-ui/target/excel-to-anki-ui.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: UI JAR not found at $JAR_PATH"
    echo "Please build the project first: mvn clean package"
    exit 1
fi

java -jar "$JAR_PATH"
