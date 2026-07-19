#!/usr/bin/env python3
"""
Usage:
  pip install genanki
  ./create_apkg_from_csv.py --csv notes.csv --media-dir media --output deck.apkg --name "My Deck"
  ./create_apkg_from_csv.py --csv notes.csv --media-dir media --output deck.apkg --update existing.apkg
CSV format: two columns per row (front,back). Supports quoted CSV entries.
Media directory: contains audio files referenced as [sound:filename.mp3] in fronts.
"""
import argparse
import csv
import os
import genanki
import sys
import zipfile
import json
import tempfile
import shutil

def load_notes(csv_path):
    notes = []
    with open(csv_path, newline='', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            if not row: continue
            front = row[0]
            back = row[1] if len(row) > 1 else ""
            notes.append((front, back))
    return notes

def extract_apkg(apkg_path):
    """Extract an existing APKG and return temp directory, deck info, and media."""
    temp_dir = tempfile.mkdtemp()
    with zipfile.ZipFile(apkg_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)
    return temp_dir

def load_deck_from_apkg(apkg_path):
    """Load existing deck from APKG file."""
    try:
        import sqlite3
        import json
        temp_dir = extract_apkg(apkg_path)
        col_path = os.path.join(temp_dir, 'collection.anki2')
        if not os.path.exists(col_path):
            raise RuntimeError("Invalid APKG: missing collection.anki2")
        
        conn = sqlite3.connect(col_path)
        cursor = conn.cursor()
        
        # Get deck and model info from col table (Anki stores them as JSON)
        cursor.execute("SELECT decks, models FROM col LIMIT 1")
        row = cursor.fetchone()
        if not row:
            raise RuntimeError("No collection found in APKG")
        
        decks_json = json.loads(row[0])
        models_json = json.loads(row[1])
        
        # Extract first deck (usually ID 1 is the default deck)
        deck_id = None
        deck_name = None
        for did_str, deck_info in decks_json.items():
            if isinstance(deck_info, dict) and 'name' in deck_info:
                deck_id = int(did_str)
                deck_name = deck_info['name']
                break
        
        if not deck_id or not deck_name:
            raise RuntimeError("No valid deck found in APKG")
        
        # Extract first model
        model_id = None
        model_name = None
        for mid_str, model_info in models_json.items():
            if isinstance(model_info, dict) and 'name' in model_info:
                model_id = int(mid_str)
                model_name = model_info['name']
                break
        
        if not model_id or not model_name:
            raise RuntimeError("No valid model found in APKG")
        
        conn.close()
        shutil.rmtree(temp_dir)
        
        return deck_id, deck_name, model_id, model_name
    except Exception as e:
        print(f"Error reading existing APKG: {e}", file=sys.stderr)
        raise

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--csv', required=True, help='Path to notes CSV (front,back)')
    p.add_argument('--media-dir', required=True, help='Directory containing media files (optional)')
    p.add_argument('--output', required=True, help='Output .apkg file path')
    p.add_argument('--name', default='Exported Deck', help='Deck name')
    p.add_argument('--update', default=None, help='Existing APKG to update instead of creating new')
    args = p.parse_args()

    notes = load_notes(args.csv)
    if not notes:
        print('No notes found in', args.csv)
        sys.exit(1)

    # Determine deck ID and name
    if args.update:
        if not os.path.exists(args.update):
            print(f"Error: existing APKG not found: {args.update}", file=sys.stderr)
            sys.exit(1)
        deck_id, deck_name, model_id, _ = load_deck_from_apkg(args.update)
    else:
        deck_id = abs(hash(args.name)) % (10**9)
        deck_name = args.name
        model_id = 1607392319

    # create model (use consistent ID for updates)
    model = genanki.Model(
        model_id,
        'Simple Model',
        fields=[{'name':'Front'}, {'name':'Back'}],
        templates=[{
            'name': 'Card 1',
            'qfmt': '{{Front}}',
            'afmt': '{{FrontSide}}<hr id="answer">{{Back}}',
        }]
    )

    deck = genanki.Deck(deck_id, deck_name)

    pkg_media = []
    if os.path.isdir(args.media_dir):
        for fn in sorted(os.listdir(args.media_dir)):
            full = os.path.join(args.media_dir, fn)
            if os.path.isfile(full):
                pkg_media.append(full)

    for front, back in notes:
        note = genanki.Note(model=model, fields=[front, back])
        deck.add_note(note)

    package = genanki.Package(deck)
    if pkg_media:
        package.media_files = pkg_media
    package.write_to_file(args.output)
    print('Wrote', args.output)
    if pkg_media:
        print('Included media files:', len(pkg_media))

if __name__ == '__main__':
    main()