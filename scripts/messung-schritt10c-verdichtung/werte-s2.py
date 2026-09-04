# -*- coding: utf-8 -*-
"""Liest SHOW PROFILES aus den drei Ergebnisdateien und bildet die beste von fuenf.

    python scripts/messung-schritt10c-verdichtung/werte-s2.py

Die Zaehlprobe unten ist nicht Zierde: Beim ersten Lauf standen alle drei
Mandanten in einer Datei, MariaDB deckelt `profiling_history_size` bei 100, und
33 der 108 Messungen fehlten in SHOW PROFILES. Ohne die Probe waere daraus eine
Tabelle mit falschen Zuordnungen geworden — jede Zahl fuer sich plausibel.
"""

import io
import os
import re

HIER = os.path.dirname(os.path.abspath(__file__))
MANDANTEN = ["NEXANS", "SUTTONS", "VOTG"]
FENSTER = ["48H", "30T", "12M"]

reihenfolge = [
    z.split()
    for z in io.open(os.path.join(HIER, "reihenfolge.txt"), encoding="utf-8").read().split()
    and io.open(os.path.join(HIER, "reihenfolge.txt"), encoding="utf-8").read().strip().split("\n")
]

werte = {}
for mandant in MANDANTEN:
    pfad = os.path.join(HIER, "ergebnis", "s2-%s.txt" % mandant.lower())
    text = io.open(pfad, encoding="utf-8", errors="replace").read()
    zeilen = re.findall(r"^\|\s+(\d+)\s+\|\s+([\d.]+)\s+\|\s+(.*)$", text, re.M)
    mess = []
    for _id, dauer, rest in zeilen:
        if rest.startswith("SELECT m.MessageID"):
            mess.append(("alt", float(dauer)))
        elif rest.startswith("SELECT m.ProcessID, p.ProcessName, COUNT(*)"):
            mess.append(("neu", float(dauer)))

    erwartet = [r for r in reihenfolge if r[0] == mandant]
    if len(mess) != len(erwartet):
        raise SystemExit(
            "%s: %d Profilzeilen, erwartet %d — SHOW PROFILES hat gedeckelt."
            % (mandant, len(mess), len(erwartet))
        )
    for (fassung_ist, dauer), (_m, fenster, fassung_soll, lauf) in zip(mess, erwartet):
        if fassung_ist != fassung_soll:
            raise SystemExit("%s %s: Reihenfolge verrutscht" % (mandant, fenster))
        if int(lauf) == 0:
            continue  # Aufwaermlauf
        werte.setdefault((mandant, fenster, fassung_soll), []).append(dauer * 1000)

print("M146 — beste von fuenf, in Millisekunden (Aufwaermlauf zaehlt nicht mit)\n")
print("  Mandant   Fenster   alt (je Nachricht)   verdichtet (je Prozess)   Unterschied")
for mandant in MANDANTEN:
    for fenster in FENSTER:
        a = min(werte[(mandant, fenster, "alt")])
        n = min(werte[(mandant, fenster, "neu")])
        print(
            "  %-9s %-9s %10.3f %19.3f %14s"
            % (mandant, fenster, a, n, "%+.3f" % (n - a))
        )

print("\nSpannweite ueber alle neun Kombinationen:")
for f, name in (("alt", "alt       "), ("neu", "verdichtet")):
    alle = [min(v) for k, v in werte.items() if k[2] == f]
    print("  %s  %.3f bis %.3f ms" % (name, min(alle), max(alle)))

print("\nStreuung der fuenf Laeufe, verdichtete Fassung:")
for mandant in MANDANTEN:
    for fenster in FENSTER:
        v = werte[(mandant, fenster, "neu")]
        print("  %-9s %-5s %.3f .. %.3f ms" % (mandant, fenster, min(v), max(v)))
