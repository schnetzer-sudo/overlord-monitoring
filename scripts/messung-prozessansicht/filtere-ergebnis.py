#!/usr/bin/env python3
"""Nimmt aus den Messprotokollen die Datenzeilen heraus (Regel G1).

WARUM. Das Geruest der Prozessansicht liest process_catalog.partner. Die rohen
Messprotokolle enthalten damit KURATIERTE PARTNERNAMEN, und die sind nach
docs/README.md („Was nicht hineingehoert") geschuetzt: Sie sind Namen Dritter und
gehoeren nicht ins Repository.

WAS BLEIBT. Alles, was die Messung ausmacht: die Marken, die EXPLAIN-Tabellen und
die Laufzeit-Auswertungen. Was faellt, sind ausschliesslich Ergebniszeilen der
gemessenen Abfragen selbst -- sie tragen keine Aussage, sie sind nur der Grund,
warum die Abfrage Zeit braucht.

WIE ERKANNT. Zwischen einer Marke `=== … sechs Laeufe ===` und der zugehoerigen
Auswertungszeile stehen ausschliesslich Ergebniszeilen. Der Bereich wird durch
eine Zeile ersetzt, die sagt, wie viele Zeilen weggefallen sind -- eine
weggelassene Menge, die niemand nennt, sieht aus wie eine leere.

Aufruf:  python scripts/messung-prozessansicht/filtere-ergebnis.py
"""

import re
from pathlib import Path

ORDNER = Path(__file__).with_name("ergebnis")

# Eine Marke schaltet um: Nach "sechs Laeufe" kommen Ergebniszeilen, bis die
# Auswertungszeile den Fall abschliesst.
BEGINN = re.compile(r"sechs Laeufe ===")
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
                heraus.append(f"-- {geschluckt} Ergebniszeilen entfernt (Regel G1)")
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
        heraus.append(f"-- {geschluckt} Ergebniszeilen entfernt (Regel G1)")
        gesamt += geschluckt
    return "\n".join(heraus) + "\n", gesamt


for datei in sorted(ORDNER.glob("s*.txt")):
    if datei.name.endswith(".gefiltert.txt"):
        continue
    inhalt = datei.read_text(encoding="utf-8-sig", errors="replace")
    gefiltert, entfernt = filtere(inhalt)
    ziel = datei.with_suffix(".gefiltert.txt")
    ziel.write_text(gefiltert, encoding="utf-8", newline="\n")
    print(f"{datei.name}: {entfernt} Zeilen entfernt -> {ziel.name} ({ziel.stat().st_size} Byte)")
