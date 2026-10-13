#!/usr/bin/env python3
"""Filtert ein Sitzungsprotokoll von M191 auf das, was eingecheckt wird: die Marken,
die EXPLAIN-Tabellen, die Bereiche aus dem Optimizer-Trace und die Auswertungszeilen --
nicht die Ergebniszeilen der Statements selbst (sie tragen Prozesskennungen, Regel G1).

Dieselbe Bauform wie scripts/messung-fehler-live/filtere.py: Gelesen wird ueber einen
Zustand und nicht ueber die Rahmenzeilen allein, sonst haelt der Filter die
schliessende Zeile einer Tabelle fuer die oeffnende der naechsten.

Aufruf:  python scripts/messung-eerp-failed/filtere.py ergebnis/s2.txt > ergebnis/s2.gefiltert.txt
"""

import sys
from pathlib import Path

# Tabellen, deren Kopf so beginnt, gehen vollstaendig durch; alle anderen nicht.
DURCHLASS = ("| id ", "| fall ", "| read_only ", "| marke ")


def main(pfad: str) -> None:
    # Die Windows-Konsole schreibt cp1252; der Optimizer-Trace traegt Zeichen ausserhalb davon.
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    zeilen = Path(pfad).read_text(encoding="utf-8", errors="replace").splitlines()
    zustand = 0  # 0 aussen, 1 Kopf erwartet, 2 Rahmen nach dem Kopf, 3 Datenzeilen
    oeffnend = ""
    durchlassen = False
    for zeile in zeilen:
        rahmen = zeile.startswith("+")
        if zustand == 0:
            if rahmen:
                oeffnend = zeile
                zustand = 1
            elif zeile.startswith("ERROR"):
                print(zeile)
            continue
        if zustand == 1:
            durchlassen = zeile.startswith(DURCHLASS)
            if durchlassen:
                print(oeffnend)
                print(zeile)
            zustand = 2
            continue
        if zustand == 2:
            if durchlassen:
                print(zeile)
            zustand = 3
            continue
        if durchlassen:
            print(zeile)
        if rahmen:
            zustand = 0


if __name__ == "__main__":
    main(sys.argv[1])
