#!/usr/bin/env python3
"""Liest die Laufzeit-Auswertungen aus den gefilterten Protokollen und gibt sie
als Markdown-Tabelle aus -- damit keine Zahl abgetippt wird.

Jede Auswertungszeile der Sitzungen hat die Form
    | M147-NEXANS-365T | 6 | 812.345 | 790.123 | 790.123,… |
bzw. fuer M150 mit einer Ebenenspalte dahinter. Ausgegeben werden Fall,
(Ebene,) Aufwaermlauf, beste von fuenf und alle fuenf.

Aufruf:  python scripts/messung-prozessansicht-frei/werte.py [s1 s2 …]
"""

import re
import sys
from pathlib import Path

ORDNER = Path(__file__).with_name("ergebnis")
ZEILE = re.compile(r"^\|\s*(M1\d\d-\S+)\s*\|(.*)\|\s*$")


def lies(datei: Path):
    for zeile in datei.read_text(encoding="utf-8", errors="replace").splitlines():
        m = ZEILE.match(zeile)
        if not m:
            continue
        felder = [f.strip() for f in m.group(2).split("|")]
        yield m.group(1), felder


def main(namen):
    dateien = sorted(ORDNER.glob("*.gefiltert.txt"))
    if namen:
        dateien = [d for d in dateien if d.name.split(".")[0] in namen]
    for datei in dateien:
        print(f"\n### {datei.name}\n")
        print("| Fall | Ebene | Laeufe | Aufwaermlauf | **beste von fuenf** | alle fuenf |")
        print("|---|---|---:|---:|---:|---|")
        for fall, felder in lies(datei):
            if len(felder) == 4:
                laeufe, warm, beste, alle = felder
                ebene = ""
            elif len(felder) == 5:
                ebene, laeufe, warm, beste, alle = felder
            else:
                print(f"| {fall} | ? | " + " | ".join(felder) + " |")
                continue
            print(f"| `{fall}` | {ebene} | {laeufe} | {warm} | **{beste}** | {alle} |")


if __name__ == "__main__":
    main(sys.argv[1:])
