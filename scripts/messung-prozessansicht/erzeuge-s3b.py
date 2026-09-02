#!/usr/bin/env python3
"""Erzeugt s3b-ohne-index.sql — was der Index aus V11 der GEWAEHLTEN Fassung bringt (M115).

M114 hat den Preis der Indexlosigkeit an Fassung G gemessen (995 ms statt 17 ms).
Gewaehlt ist aber D. Ein Kostensatz ueber eine nicht gebaute Fassung ist kein
Kostensatz ueber die gebaute — deshalb dieselbe Gegenprobe noch einmal, an D.

Aufruf:  python scripts/messung-prozessansicht/erzeuge-s3b.py
"""

from pathlib import Path

MANDANTEN = ["NEXANS", "VOTG", "IBIS", "SUTTONS"]
LAEUFE = 6


def block(fall, sql):
    zeilen = [f"SELECT '=== {fall} · Plan (Regel L15) ===' AS marke;",
              "EXPLAIN " + sql,
              f"SELECT '=== {fall} · sechs Laeufe ===' AS marke;",
              "SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;"]
    zeilen.extend([sql] * LAEUFE)
    zeilen.append(
        f"SELECT '{fall}' AS fall,\n"
        "       COUNT(*)                          AS anzahl_laeufe,\n"
        "       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,\n"
        "       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,\n"
        "       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf\n"
        "FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,\n"
        "             ROUND(SUM(DURATION) * 1000, 3) AS ms\n"
        "      FROM information_schema.PROFILING\n"
        "      WHERE QUERY_ID > @basis + 1\n"
        "      GROUP BY QUERY_ID) t;")
    return "\n".join(zeilen) + "\n\n"


def d(mandant, ohne_index):
    hinweis = " IGNORE INDEX (message_rollup_prozess_idx)" if ohne_index else ""
    return ("SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,\n"
            f"       (SELECT r.stunde FROM overlord_monitor.message_rollup r{hinweis}\n"
            "        WHERE r.process_id = p.ProcessID\n"
            "        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung\n"
            "FROM GlassfishDB.Process p\n"
            "JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
            "LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID\n"
            f"WHERE pm.MandantID = '{mandant}'\n"
            "ORDER BY p.ProcessName;")


kopf = """-- Messung M115: WAS DER INDEX AUS V11 DER GEWAEHLTEN FASSUNG BRINGT
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 2
-- Ergebnis: docs/process-view.md
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s3b.py.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- Der Index wird NICHT geloescht. Die Tabelle ist geteilt, und die
-- Fensterverengung der Nachrichtenliste haengt an ihm (docs/rollup.md §9b).
-- IGNORE INDEX beantwortet dieselbe Frage, ohne etwas anzufassen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

"""

teile = [kopf]
for mandant in MANDANTEN:
    teile.append(block(f"M115-mit-{mandant}", d(mandant, False)))
    teile.append(block(f"M115-ohne-{mandant}", d(mandant, True)))
teile.append("SELECT '=== 99 fertig ===' AS marke;\n")

ziel = Path(__file__).with_name("s3b-ohne-index.sql")
ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
print(f"geschrieben: {ziel}  ({ziel.stat().st_size} Byte)")
