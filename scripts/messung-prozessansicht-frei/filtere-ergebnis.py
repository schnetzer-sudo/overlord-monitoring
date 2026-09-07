#!/usr/bin/env python3
"""Nimmt aus den Messprotokollen die Datenzeilen heraus (Regel G1).

WARUM. Die Kennzahlenabfrage gibt process_id aus, und Prozesskennungen tragen
Partnernamen Dritter. Die rohen Messprotokolle gehoeren deshalb nicht ins
Repository (docs/README.md, „Was nicht hineingehoert").

WAS BLEIBT. Alles, was die Messung ausmacht: die Marken, die EXPLAIN-Tabellen,
die Belegungsproben (nur Zaehler) und die Laufzeit-Auswertungen. Was faellt,
sind ausschliesslich Ergebniszeilen der gemessenen Abfragen selbst.

WIE ERKANNT. Zwischen einer Marke `=== … sechs Laeufe ===` (bzw. `sechs Runden`)
und der ersten zugehoerigen Auswertungszeile (`| M1xx-…`) stehen ausschliesslich
Ergebniszeilen. Der Bereich wird durch eine Zeile ersetzt, die sagt, wie viele
Zeilen weggefallen sind. Dieselbe Form wie scripts/messung-prozessansicht/.

ACHTUNG. Die Zahl der entfernten Zeilen ist eine Zeilenzahl der -t-Ausgabe
(mit Rahmen, Kopfzeilen und dem Kopf der Auswertungstabelle), keine Zaehlung
der Ergebniszeilen. Mengen kommen aus s0, Abschnitt 06 -- dort gezaehlt.

Aufruf:  python scripts/messung-prozessansicht-frei/filtere-ergebnis.py [s0 s1 …]
         (ohne Argument alle Rohausgaben unter ergebnis/)
"""

import re
import sys
from pathlib import Path

ORDNER = Path(__file__).with_name("ergebnis")
NUR = set(sys.argv[1:])

BEGINN = re.compile(r"sechs (Laeufe|Runden) ===")
ENDE = re.compile(r"^\|\s*M1\d\d-")


def filtere(text: str) -> tuple[str, int]:
    zeilen = text.splitlines()
    heraus = []
    im_ergebnis = False
    geschluckt = 0
    gesamt = 0
    for zeile in zeilen:
        if im_ergebnis:
            if ENDE.match(zeile):
                heraus.append(f"-- {geschluckt} Zeilen der -t-Ausgabe entfernt (Regel G1)")
                heraus.append(zeile)
                gesamt += geschluckt
                geschluckt = 0
                im_ergebnis = False
            else:
                geschluckt += 1
            continue
        heraus.append(zeile)
        if BEGINN.search(zeile):
            im_ergebnis = True
    if im_ergebnis:
        heraus.append(f"-- {geschluckt} Zeilen der -t-Ausgabe entfernt (Regel G1)")
        gesamt += geschluckt
    return "\n".join(heraus) + "\n", gesamt


for datei in sorted(ORDNER.glob("s*.txt")):
    if datei.name.endswith(".gefiltert.txt"):
        continue
    if NUR and datei.stem not in NUR:
        continue
    inhalt = datei.read_text(encoding="utf-8-sig", errors="replace")
    gefiltert, entfernt = filtere(inhalt)
    ziel = datei.with_suffix(".gefiltert.txt")
    ziel.write_text(gefiltert, encoding="utf-8", newline="\n")
    print(f"{datei.name}: {entfernt} Zeilen entfernt -> {ziel.name} ({ziel.stat().st_size} Byte)")
