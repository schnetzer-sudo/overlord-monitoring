#!/usr/bin/env python3
"""Erzeugt die Sitzungsdatei der Messung M185 auf SQL-Ebene: die beiden Live-Statements
je Fall mit EXPLAIN, PROFILING und Wanduhr — und daneben die UNION-ALL-Fassung, an
der die Entscheidung "zwei Statements oder eines" haengt.

Auftrag:  "Live-Rest der laufenden Stunde, Teil A", Stand 17.09.2026
Ergebnis: docs/live-rest.md §8

DER STATEMENTTEXT IST NICHT ABGETIPPT. Er ist der Text, den
ProzessbaumStatementsTest.EinAufruf.live_teil_woertlich aus LiveRestRepository
gegen die jOOQ-Attrappe pinnt; hier werden nur die drei Bindewerte (von, bis,
Mandant) als Literale eingesetzt, so wie jOOQ sie mit STATIC_STATEMENT rendert.

Je Fall (Mandant x typische Stunde / dichtester Bereich):
  A  ausDemRollup   — die Rollupzeilen des Live-Bereichs, mit Kette
  B  ausDerQuelle   — die Zaehlung aus Message ueber denselben Bereich, mit Kette
  C  UNION ALL      — beide Teile in einer Ableitung, Kette in beiden Zweigen,
                      darueber eine Summe je Schluessel (nicht gebaut; gemessen,
                      damit die Wahl am Plan faellt)

Je Statement: EXPLAIN (Regel L15), dann ein Aufwaermlauf und fuenf Laeufe;
Laufzeit aus information_schema.PROFILING UND an der Wanduhr des Servers
(SYSDATE(6) vor und nach dem Statement, E-107). Jeder Lauf belegt drei
QUERY_IDs (SET, Statement, SET); ausgewertet wird die mittlere.

Aufruf:  python scripts/messung-live-rest/erzeuge.py
Dann:    mysql … < scripts/messung-live-rest/s1-m185-live-teil.sql > ergebnis/s1.txt
"""

from pathlib import Path

HIER = Path(__file__).resolve().parent
LAEUFE = 6  # ein Aufwaermlauf, dann beste von fuenf — dieselbe Form wie M116 und M152

# (Mandant, Art, G, Ende des Live-Bereichs) — die Stunden aus docs/live-rest.md §8.
# typisch: jetzt = G + 1 h 30 → Live-Bereich zwei Eimer; dicht: jetzt = G + 3 h → vier Eimer.
FAELLE = [
    ("NEXANS", "typisch", "2025-11-09 15:00:00", "2025-11-09 17:00:00"),
    ("NEXANS", "dicht", "2024-10-09 18:00:00", "2024-10-09 22:00:00"),
    ("VOTG", "typisch", "2025-06-22 01:00:00", "2025-06-22 03:00:00"),
    ("VOTG", "dicht", "2025-06-26 07:00:00", "2025-06-26 11:00:00"),
    ("IBIS", "typisch", "2025-08-13 14:00:00", "2025-08-13 16:00:00"),
    ("IBIS", "dicht", "2024-11-29 10:00:00", "2024-11-29 14:00:00"),
    ("SUTTONS", "typisch", "2025-06-09 19:00:00", "2025-06-09 21:00:00"),
    ("SUTTONS", "dicht", "2025-06-12 07:00:00", "2025-06-12 11:00:00"),
]

KETTE_ROLLUP = (
    "exists (select 1 as `one` from `GlassfishDB`.`Process` as `live_process` join"
    " `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
    " `live_process`.`ProjectID` where (`live_process`.`ProcessID` ="
    " `overlord_monitor`.`message_rollup`.`process_id` and"
    " `GlassfishDB`.`ProjectMandant`.`MandantID` = {mandant}))"
)
KETTE_MESSAGE = (
    "exists (select 1 as `one` from `GlassfishDB`.`Process` as `live_process` join"
    " `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
    " `live_process`.`ProjectID` where (`live_process`.`ProcessID` ="
    " `GlassfishDB`.`Message`.`ProcessID` and"
    " `GlassfishDB`.`ProjectMandant`.`MandantID` = {mandant}))"
)

A = (
    "select `overlord_monitor`.`message_rollup`.`stunde`,"
    " `overlord_monitor`.`message_rollup`.`process_id`,"
    " `overlord_monitor`.`message_rollup`.`message_status`,"
    " `overlord_monitor`.`message_rollup`.`anzahl` from `overlord_monitor`.`message_rollup`"
    " where (`overlord_monitor`.`message_rollup`.`stunde` >= {von} and"
    " `overlord_monitor`.`message_rollup`.`stunde` < {bis} and " + KETTE_ROLLUP + ")"
)
B = (
    "select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
    " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*)"
    " from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= {von}"
    " and `GlassfishDB`.`Message`.`MessageLastUpdate` < {bis} and " + KETTE_MESSAGE + ")"
    " group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
    " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`"
)
# Die Vereinigung: dieselben zwei Zweige, die Kette in jedem, darueber eine Summe je Schluessel.
C = (
    "select `t`.`stunde`, `t`.`process_id`, `t`.`message_status`, sum(`t`.`anzahl`) from ("
    "select `overlord_monitor`.`message_rollup`.`stunde` as `stunde`,"
    " `overlord_monitor`.`message_rollup`.`process_id` as `process_id`,"
    " `overlord_monitor`.`message_rollup`.`message_status` as `message_status`,"
    " -`overlord_monitor`.`message_rollup`.`anzahl` as `anzahl`"
    " from `overlord_monitor`.`message_rollup`"
    " where (`overlord_monitor`.`message_rollup`.`stunde` >= {von} and"
    " `overlord_monitor`.`message_rollup`.`stunde` < {bis} and " + KETTE_ROLLUP + ")"
    " union all "
    "select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
    " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*)"
    " from `GlassfishDB`.`Message` where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= {von}"
    " and `GlassfishDB`.`Message`.`MessageLastUpdate` < {bis} and " + KETTE_MESSAGE + ")"
    " group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
    " `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`"
    ") as `t` group by `t`.`stunde`, `t`.`process_id`, `t`.`message_status`"
)

KOPF = """-- Messung M185 -- Sitzung 1: die Live-Statements je Fall, EXPLAIN, PROFILING und Wanduhr
-- Auftrag:  "Live-Rest der laufenden Stunde, Teil A", Stand 17.09.2026
-- Ergebnis: docs/live-rest.md Paragraf 8
--
-- ERZEUGT von scripts/messung-live-rest/erzeuge.py. Der Statementtext ist der aus
-- ProzessbaumStatementsTest.EinAufruf.live_teil_woertlich (aus LiveRestRepository
-- gegen die jOOQ-Attrappe gefallen); eingesetzt sind nur die drei Bindewerte.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer monitor_read.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes.
-- L9: SET max_statement_time = 60. Jedes Statement, das die Grenze reisst, ist
--     eine Abweichung vom Rahmen und wird als solche ausgewiesen.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.
--
-- Je Statement ein Aufwaermlauf und fuenf Laeufe; jeder Lauf belegt drei QUERY_IDs
-- (SET @t0, Statement, SET @wN). Ausgewertet wird die mittlere; die Wanduhr steht
-- daneben (E-107).

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT '=== 00a Eichung: die Umlaufzeit der Wanduhr um ein leeres Statement ===' AS marke;
SET @t0 = SYSDATE(6); SELECT 1; SET @e1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M185-EICHUNG' AS fall, ROUND(@e1 / 1000, 3) AS ms1, ROUND(@e2 / 1000, 3) AS ms2, ROUND(@e3 / 1000, 3) AS ms3;
"""


def literal(zeit: str) -> str:
    return f"timestamp '{zeit}.0'"


def block(name: str, sql: str) -> str:
    zeilen = [f"SELECT '=== {name} · Plan (Regel L15) ===' AS marke;", f"EXPLAIN {sql};"]
    zeilen.append(f"SELECT '=== {name} · sechs Laeufe ===' AS marke;")
    zeilen.append("SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;")
    for lauf in range(1, LAEUFE + 1):
        zeilen.append("SET @t0 = SYSDATE(6);")
        zeilen.append(sql + ";")
        zeilen.append(f"SET @w{lauf} = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));")
    wand_alle = ", ".join(f"ROUND(@w{i} / 1000, 3)" for i in range(2, LAEUFE + 1))
    wand_beste = ", ".join(f"@w{i}" for i in range(2, LAEUFE + 1))
    zeilen.append(
        f"""SELECT '{name}' AS fall,
       COUNT(*)                          AS laeufe,
       MAX(CASE WHEN rn = 1 THEN ms END) AS profil_aufwaermlauf,
       MIN(CASE WHEN rn > 1 THEN ms END) AS profil_beste_von_fuenf,
       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS profil_alle_fuenf,
       ROUND(@w1 / 1000, 3)              AS wand_aufwaermlauf,
       ROUND(LEAST({wand_beste}) / 1000, 3) AS wand_beste_von_fuenf,
       CONCAT_WS('/', {wand_alle})       AS wand_alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1 AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;"""
    )
    return "\n".join(zeilen) + "\n"


def main() -> None:
    teile = [KOPF]
    for mandant, art, von, bis in FAELLE:
        werte = {"von": literal(von), "bis": literal(bis), "mandant": f"'{mandant}'"}
        teile.append(f"\nSELECT '=== {mandant} {art}: Live-Bereich {von} bis {bis} ===' AS marke;\n")
        for kennung, vorlage in (("A", A), ("B", B), ("C", C)):
            teile.append(block(f"M185-{mandant}-{art}-{kennung}", vorlage.format(**werte)))
    ziel = HIER / "s1-m185-live-teil.sql"
    ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
    print("geschrieben:", ziel)


if __name__ == "__main__":
    main()
