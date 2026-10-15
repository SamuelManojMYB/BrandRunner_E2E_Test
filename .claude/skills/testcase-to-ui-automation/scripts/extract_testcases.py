#!/usr/bin/env python3
"""Turns a test-case document into plain text you can grep, and a first-pass catalogue of its test case ids.

Usage:
    python -I extract_testcases.py <document.docx> <out_dir>

Writes, into <out_dir>:
    document.txt   every paragraph; every table row as 'cell | cell | cell' (in document order)
    catalogue.tsv  one line per row whose first cell looks like a test case id (BC-062, AL-012, E2E-03, TC_001 ...):
                   id <TAB> remaining cells joined by ' | ' <TAB> defect ids found in the row (D-48 ...)
    summary.txt    counts per id prefix, rows that mention a known defect, rows marked 'Not Run' / 'confirm' / 'TBD'

Why a script: the document is the source of truth, so it has to be read completely and searched precisely (by id, by
defect, by wording of an alert). Python on Windows needs UTF-8 on stdout, hence the files instead of printing.
Only .docx is handled here; for .xlsx or .csv read the sheet directly (the xlsx skill or csv module) and build the same
three files by hand.
"""
import os
import re
import sys
import zipfile
from collections import Counter

ID_RE = re.compile(r'^[A-Z][A-Z0-9]{1,9}[-_ ]?\d{1,4}$')
DEFECT_RE = re.compile(r'\b(?:D|DEF|BUG)[-_ ]?\d{1,4}\b')
FLAG_WORDS = ('not run', 'confirm', 'tbd', 'to be decided', 'clarify', 'define whether')


def text_of(fragment: str) -> str:
    # <w:tab/> and <w:br/> separate words inside a cell; text runs carry the visible characters.
    fragment = re.sub(r'<w:(?:tab|br)[^>]*/>', ' ', fragment)
    parts = re.findall(r'<w:t[^>]*>([^<]*)</w:t>', fragment)
    raw = ''.join(parts)
    return (raw.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
            .replace('&quot;', '"').replace('&apos;', "'")).strip()


def read_docx(path: str):
    with zipfile.ZipFile(path) as z:
        xml = z.read('word/document.xml').decode('utf8')
    lines, rows = [], []
    for block in re.findall(r'<w:tr[ >].*?</w:tr>|<w:p[ >].*?</w:p>', xml, flags=re.S):
        cells = re.findall(r'<w:tc>.*?</w:tc>', block, flags=re.S)
        if cells:
            values = [text_of(c) for c in cells]
            lines.append(' | '.join(values))
            rows.append(values)
        else:
            t = text_of(block)
            if t:
                lines.append(t)
    return lines, rows


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    src, out = sys.argv[1], sys.argv[2]
    os.makedirs(out, exist_ok=True)
    lines, rows = read_docx(src)
    with open(os.path.join(out, 'document.txt'), 'w', encoding='utf8') as f:
        f.write('\n'.join(lines))

    catalogue, prefixes, defect_rows, flagged = [], Counter(), [], []
    for values in rows:
        if not values or not ID_RE.match(values[0].strip()):
            continue
        tid = values[0].strip()
        rest = ' | '.join(values[1:])
        defects = sorted(set(m.replace(' ', '-').replace('_', '-') for m in DEFECT_RE.findall(rest)))
        catalogue.append(f"{tid}\t{rest}\t{','.join(defects)}")
        prefixes[re.split(r'[-_ ]', tid)[0]] += 1
        if defects:
            defect_rows.append(f"{tid}: {','.join(defects)}")
        low = rest.lower()
        hits = [w for w in FLAG_WORDS if w in low]
        if hits:
            flagged.append(f"{tid}: {', '.join(hits)}")
    with open(os.path.join(out, 'catalogue.tsv'), 'w', encoding='utf8') as f:
        f.write('\n'.join(catalogue))
    with open(os.path.join(out, 'summary.txt'), 'w', encoding='utf8') as f:
        f.write('Test case rows per prefix: ' + ', '.join(f'{k}={v}' for k, v in sorted(prefixes.items())) + '\n\n')
        f.write('Rows that mention a defect id (assert the EXPECTED result, let the test fail):\n  ' + '\n  '.join(defect_rows) + '\n\n')
        f.write("Rows marked not run / confirm / tbd (ambiguous or blocked: flag, do not invent):\n  " + '\n  '.join(flagged) + '\n')
    print(f"{len(lines)} lines, {len(catalogue)} test-case rows -> {out}")


if __name__ == '__main__':
    main()
