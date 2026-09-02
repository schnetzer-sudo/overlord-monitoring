#!/usr/bin/env python3
"""Erzeugt s4-gebaut.sql — die Abschlussmessung der GEBAUTEN Statements (M116).

Regel L7 verlangt die Messung DER Abfrage und nicht einer aehnlichen. Die
Statements in dieser Datei sind deshalb nicht abgetippt, sondern aus dem Code
gefallen: `gerendert.txt` entsteht aus ProzessbaumRepository gegen eine
jOOQ-Attrappe mit StatementType.STATIC_STATEMENT — derselbe Weg, den
ProzessbaumStatementsTest und ProzessbaumPlanDbIT gehen.

Aufruf:  python scripts/messung-prozessansicht/erzeuge-s4.py
"""

from pathlib import Path

LAEUFE = 6


def block(fall, sql):
    zeilen = [f"SELECT '=== {fall} · Plan (Regel L15) ===' AS marke;",
              "EXPLAIN " + sql + ";",
              f"SELECT '=== {fall} · sechs Laeufe ===' AS marke;",
              "SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;"]
    zeilen.extend([sql + ";"] * LAEUFE)
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


kopf = """-- Messung M116: DIE ABSCHLUSSMESSUNG DER GEBAUTEN STATEMENTS
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 4
-- Ergebnis: docs/process-view.md §7
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s4.py aus gerendert.txt.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- DIE STATEMENTS SIND NICHT ABGETIPPT. Sie fallen aus ProzessbaumRepository
-- gegen eine jOOQ-Attrappe mit StatementType.STATIC_STATEMENT -- derselbe Weg,
-- den ProzessbaumStatementsTest woertlich festhaelt. Was hier gemessen wird,
-- ist damit Zeichen fuer Zeichen das, was die Anwendung schickt (Regel L7).
--
-- Vier Mandanten (Regel L7 verlangt drei Groessenordnungen):
--   NEXANS  733 Prozesse -- der groesste
--   VOTG    390          -- der mittlere, und der teuerste Fall in M115
--   IBIS    192          -- der zweite mittlere, mit dem breitesten Baum
--   SUTTONS  17          -- der kleine, und der Ausreisser in M112
--
-- Alle drei Fensterbreiten, hergeleitet aus dem Dev-Anker 2025-12-30 04:09:47.
-- Das Geruest kennt kein Fenster und wird deshalb je Mandant einmal gemessen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

"""

quelle = Path(__file__).with_name("gerendert.txt")
teile = [kopf]
anzahl = 0
for zeile in quelle.read_text(encoding="utf-8-sig").splitlines():
    zeile = zeile.strip()
    if not zeile.startswith("###"):
        continue
    felder = zeile[3:].split("|")
    if felder[0] == "GERUEST":
        fall = f"M116-GERUEST-{felder[1]}"
        sql = felder[2]
    else:
        fall = f"M116-KENNZAHLEN-{felder[1]}-{felder[2]}"
        sql = felder[3]
    teile.append(block(fall, sql))
    anzahl += 1

teile.append("SELECT '=== 99 fertig ===' AS marke;\n")

ziel = Path(__file__).with_name("s4-gebaut.sql")
ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
print(f"geschrieben: {ziel}  ({anzahl} Faelle, {ziel.stat().st_size} Byte)")
