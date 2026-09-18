#!/usr/bin/env python3
"""Filtert ein Sitzungsprotokoll von M188 auf das, was eingecheckt wird: die Marken,
die EXPLAIN-Tabellen, die Zaehlung je Mandant und die Auswertungszeilen — nicht die
Ergebniszeilen der Statements selbst (sie tragen Prozesskennungen, Regel G1).

Die Tabellen der Ausgabe von `mysql -t` haben genau drei Rahmenzeilen: oeffnend, nach
dem Kopf, schliessend. Gelesen wird deshalb ueber einen Zustand und nicht ueber die
Rahmenzeilen allein — sonst haelt der Filter die schliessende Zeile einer Tabelle fuer
die oeffnende der naechsten (docs: Messskript-Fallen, 08.09.2026).

Aufruf:  python scripts/messung-fehler-live/filtere.py ergebnis/s1.txt > ergebnis/s1.gefiltert.txt
"""

import sys
from pathlib import Path

# Tabellen, deren Kopf so beginnt, gehen vollstaendig durch; alle anderen nicht.
DURCHLASS = ("| id ", "| fall ", "| read_only ", "| mandant ", "| rollup_gesamt ", "| rohwert ",
             "| marke ")


def main(pfad: str) -> None:
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
        # zustand == 3: Datenzeilen bis zum schliessenden Rahmen
        if durchlassen:
            print(zeile)
        if rahmen:
            zustand = 0


if __name__ == "__main__":
    main(sys.argv[1])
