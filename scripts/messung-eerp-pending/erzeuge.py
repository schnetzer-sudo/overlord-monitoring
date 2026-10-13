#!/usr/bin/env python3
"""Erzeugt die Sitzung der Messung M192 (EERP_PENDING als QUITTUNG_AUSSTEHEND).

Auftrag:  "Statuswert EERP_PENDING einordnen", 13.10.2026
Ergebnis: docs/message-status.md, Abschnitt "EERP_PENDING"

NUR EXPLAIN, KEINE LAUFZEITREIHE (Auftrag, Schritt 5). Gemessen werden die
Statements, deren Text sich mit der neuen Einordnung aendert:
  LISTE_UNGEKLAERT, VERENGUNG_UNGEKLAERT   EERP_PENDING steht neu im NOT IN
  LISTE_QUITTUNG_AUSSTEHEND, VERENGUNG_…    der neue Statusfilter, IN ('EERP_PENDING')
je fuer NEXANS und SUTTONS, dazu zum Vergleich die Fassung von M191
(scripts/messung-eerp-failed/gerendert-rest.txt). Die Texte sind aus dem Code
gerendert (Wegwerf-Test gegen die jOOQ-Attrappe, STATIC_STATEMENT, danach
geloescht) und stehen in gerendert.txt.

Je Statement: EXPLAIN und die gewaehlten Indexbereiche aus dem Optimizer-Trace
(nur Metadaten, kein Datenzugriff).

Aufruf:  python scripts/messung-eerp-pending/erzeuge.py
Dann:    mysql ... -t < scripts/messung-eerp-pending/s1-explain.sql > ergebnis/s1.txt
"""

from pathlib import Path

HIER = Path(__file__).resolve().parent
M191 = HIER.parent / "messung-eerp-failed" / "gerendert-rest.txt"

KOPF = """-- Messung M192 -- Sitzung 1: EXPLAIN der geaenderten Statements (keine Laufzeitreihe)
-- Auftrag:  "Statuswert EERP_PENDING einordnen", 13.10.2026
-- Ergebnis: docs/message-status.md, Abschnitt "EERP_PENDING"
--
-- ERZEUGT von scripts/messung-eerp-pending/erzeuge.py aus gerendert.txt.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. EXPLAIN gibt
--     keine Zeilen aus; der Trace nennt nur Indexbereiche.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version;
"""


def lies(datei: Path) -> dict:
    texte = {}
    for zeile in datei.read_text(encoding="utf-8").splitlines():
        art, mandant, sql = zeile.split("|", 2)
        texte[(art, mandant)] = sql
    return texte


def block(name: str, sql: str) -> str:
    return f"""
SELECT '=== {name} · Plan ===' AS marke;
SET optimizer_trace = 'enabled=on';
EXPLAIN {sql};
SELECT '{name}-BEREICHE' AS fall,
       JSON_EXTRACT(TRACE, '$**.chosen_range_access_summary') AS gewaehlt
FROM information_schema.OPTIMIZER_TRACE;
SET optimizer_trace = 'enabled=off';
"""


def main() -> None:
    neu = lies(HIER / "gerendert.txt")
    alt = lies(M191)
    teile = [KOPF]
    for art in ("LISTE_UNGEKLAERT", "VERENGUNG_UNGEKLAERT"):
        for mandant in ("NEXANS", "SUTTONS"):
            vorher = alt[(art, mandant)]
            nachher = neu[(art, mandant)]
            # Gegenprobe: Der neue Text ist der alte mit EERP_PENDING im NOT IN, sonst nichts.
            if nachher.replace("'EERP_PENDING', ", "", 1) != vorher:
                raise SystemExit(f"{art}/{mandant}: mehr als EERP_PENDING geaendert")
            teile.append(block(f"M192-{art}-{mandant}-M191", vorher))
            teile.append(block(f"M192-{art}-{mandant}-neu", nachher))
    for art in ("LISTE_QUITTUNG_AUSSTEHEND", "VERENGUNG_QUITTUNG_AUSSTEHEND"):
        for mandant in ("NEXANS", "SUTTONS"):
            teile.append(block(f"M192-{art}-{mandant}-neu", neu[(art, mandant)]))
    ziel = HIER / "s1-explain.sql"
    ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
    print("geschrieben:", ziel.name)


if __name__ == "__main__":
    main()
