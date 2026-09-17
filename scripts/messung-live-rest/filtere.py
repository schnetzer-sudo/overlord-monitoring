#!/usr/bin/env python3
"""Filtert das Sitzungsprotokoll von M185 auf das, was in die Dokumentation gehoert:
die Marken, die EXPLAIN-Tabellen und die Auswertungszeilen — nicht die Ergebniszeilen
der Statements selbst.

Aufruf:  python scripts/messung-live-rest/filtere.py ergebnis/s1.txt > ergebnis/s1.gefiltert.txt
"""

import sys
from pathlib import Path


def main(pfad: str) -> None:
    zeilen = Path(pfad).read_text(encoding="utf-8", errors="replace").splitlines()
    im_plan = False
    fall_kopf = False
    for zeile in zeilen:
        if "=== " in zeile and "marke" not in zeile:
            print(zeile)
            im_plan = "Plan (Regel L15)" in zeile
            continue
        if im_plan:
            if zeile.startswith("+") or zeile.startswith("|"):
                print(zeile)
                continue
            if zeile.strip() == "":
                im_plan = False
            continue
        if zeile.startswith("| fall ") or zeile.startswith("| M185-") or zeile.startswith("| read_only"):
            print(zeile)
            fall_kopf = True
            continue
        if fall_kopf and zeile.startswith("+"):
            print(zeile)
            fall_kopf = False


if __name__ == "__main__":
    main(sys.argv[1])
