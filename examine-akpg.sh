#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 path/to/your.apkg"
  exit 1
fi

APKG="$1"

if [ ! -f "$APKG" ]; then
  echo "File not found: $APKG"
  exit 2
fi

echo "=== Listing entries in: $APKG ==="
unzip -l "$APKG" || { echo "unzip failed"; exit 3; }
echo

echo "=== media (first 200 bytes) ==="
if unzip -p "$APKG" media >/dev/null 2>&1; then
  unzip -p "$APKG" media | head -c 200 || true
  echo
else
  echo "(no 'media' entry found)"
fi
echo

echo "=== media.json (first 200 bytes) ==="
if unzip -p "$APKG" media.json >/dev/null 2>&1; then
  unzip -p "$APKG" media.json | head -c 200 || true
  echo
else
  echo "(no 'media.json' entry found)"
fi
echo

TMPFILE="$(mktemp /tmp/collection.anki2.XXXXXX)"
trap 'rm -f "$TMPFILE"' EXIT

echo "=== Extracting collection.anki2 to: $TMPFILE ==="
if unzip -p "$APKG" collection.anki2 > "$TMPFILE" 2>/dev/null; then
  if ! command -v sqlite3 >/dev/null 2>&1; then
    echo "sqlite3 is not installed or not on PATH. Install sqlite3 to inspect the DB."
    exit 4
  fi

  echo "=== SQLite: length(models), length(decks), first 200 chars of models ==="
  sqlite3 "$TMPFILE" "select length(models), length(decks), substr(models,1,200) from col limit 1;"
else
  echo "(no 'collection.anki2' entry found in the apkg)"
  exit 5
fi