#!/usr/bin/env python3
"""
Usage:
  pip install genanki
  ./create_apkg_from_csv.py --csv notes.csv --media-dir media --output deck.apkg --name "My Deck"
CSV format: two columns per row (front,back). Supports quoted CSV entries.
Media directory: contains audio files referenced as [sound:filename.mp3] in fronts.
"""
import argparse
import csv
import os
import genanki
import sys

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

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--csv', required=True, help='Path to notes CSV (front,back)')
    p.add_argument('--media-dir', required=True, help='Directory containing media files (optional)')
    p.add_argument('--output', required=True, help='Output .apkg file path')
    p.add_argument('--name', default='Exported Deck', help='Deck name')
    args = p.parse_args()

    notes = load_notes(args.csv)
    if not notes:
        print('No notes found in', args.csv)
        sys.exit(1)

    # create model
    model = genanki.Model(
        1607392319,
        'Simple Model',
        fields=[{'name':'Front'}, {'name':'Back'}],
        templates=[{
            'name': 'Card 1',
            'qfmt': '{{Front}}',
            'afmt': '{{FrontSide}}<hr id="answer">{{Back}}',
        }]
    )

    deck_id = abs(hash(args.name)) % (10**9)
    deck = genanki.Deck(deck_id, args.name)

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