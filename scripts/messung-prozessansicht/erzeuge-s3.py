#!/usr/bin/env python3
"""Erzeugt s3-letzte-bewegung.sql — die Stichwahl fuer „letzte Bewegung" (M114).

M112 hat drei Fassungen gemessen und einen Ausreisser gefunden: Die korrelierte
Unterabfrage (A) kostet bei SUTTONS 32,577 ms und bei VOTG 2,364 ms — bei 17
gegen 390 Prozessen. Der Plan sagt warum: `DEPENDENT SUBQUERY … ref … 453 rows
… Using index`. MariaDB zieht in der abhaengigen Unterabfrage KEINE
MIN/MAX-Optimierung; sie liest je Prozess den ganzen Indexbereich und nimmt
davon das Maximum. Der Aufwand haengt damit an der Zahl der EIMER je Prozess,
und die ist bei SUTTONS hoch.

Diese Sitzung misst vier weitere Fassungen gegen die beiden aus M112, die
ueberhaupt in Frage kommen. Die entscheidende ist F: dieselbe Abfrage wie B,
aber mit IGNORE INDEX — die Gegenprobe zu Teil 2 des Auftrags, was ohne den
Index aus V11 zu zahlen waere.

Aufruf:  python scripts/messung-prozessansicht/erzeuge-s3.py
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


GERUEST_KOPF = (
    "SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,\n")
GERUEST_FUSS = (
    "FROM GlassfishDB.Process p\n"
    "JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
    "LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID\n")


def d_korreliert_limit(mandant):
    """D — korrelierte Unterabfrage, aber als ORDER BY … LIMIT 1 statt MAX().

    Sie fragt denselben Wert und sollte je Prozess EINE Indexposition
    anspringen statt den ganzen Bereich zu lesen.
    """
    return (GERUEST_KOPF +
            "       (SELECT r.stunde FROM overlord_monitor.message_rollup r\n"
            "        WHERE r.process_id = p.ProcessID\n"
            "        ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung\n"
            + GERUEST_FUSS +
            f"WHERE pm.MandantID = '{mandant}'\n"
            "ORDER BY p.ProcessName;")


def e_ableitung_mandant(mandant):
    """E — Ableitung, aber nur ueber die Prozesse dieses Mandanten.

    Der Gedanke: weniger Gruppen als in B. Der Preis: Die Mandantenkette steht
    dann ZWEIMAL im Statement.
    """
    return (GERUEST_KOPF +
            "       r.letzte_bewegung\n"
            + GERUEST_FUSS +
            "LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung\n"
            "           FROM overlord_monitor.message_rollup rr\n"
            "           WHERE EXISTS (SELECT 1 FROM GlassfishDB.Process p2\n"
            "                         JOIN GlassfishDB.ProjectMandant pm2\n"
            "                              ON pm2.ProjectID = p2.ProjectID\n"
            "                         WHERE p2.ProcessID = rr.process_id\n"
            f"                           AND pm2.MandantID = '{mandant}')\n"
            "           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID\n"
            f"WHERE pm.MandantID = '{mandant}'\n"
            "ORDER BY p.ProcessName;")


def g_ableitung_gesamt(mandant, ignoriere_index=False):
    """G — Ableitung ueber den ganzen Bestand, mit Katalog. Der Kandidat.

    F ist dieselbe Abfrage mit IGNORE INDEX: die Gegenprobe zu Teil 2.
    """
    hinweis = " IGNORE INDEX (message_rollup_prozess_idx)" if ignoriere_index else ""
    return (GERUEST_KOPF +
            "       r.letzte_bewegung\n"
            + GERUEST_FUSS +
            "LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung\n"
            f"           FROM overlord_monitor.message_rollup rr{hinweis}\n"
            "           GROUP BY rr.process_id) r ON r.process_id = p.ProcessID\n"
            f"WHERE pm.MandantID = '{mandant}'\n"
            "ORDER BY p.ProcessName;")


kopf = """-- Messung M114: DIE STICHWAHL FUER „LETZTE BEWEGUNG"
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 2 und 4
-- Ergebnis: docs/process-view.md
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s3.py.
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
--   D  korrelierte Unterabfrage als ORDER BY … LIMIT 1 statt MAX()
--   E  Ableitung, auf die Prozesse des Mandanten eingeschraenkt
--   G  Ableitung ueber den ganzen Bestand, mit Katalog — der Kandidat
--   F  wie G, aber IGNORE INDEX (message_rollup_prozess_idx)
--
-- F IST DIE EIGENTLICHE FRAGE DES AUFTRAGS. Teil 2 fragt, was ein voller
-- Durchlauf ueber 335.610 Zeilen kostete, wenn es den Index aus V11 nicht
-- gaebe. Der Index wird dafuer NICHT geloescht — die Tabelle ist geteilt, und
-- die Fensterverengung der Nachrichtenliste haengt an ihm (docs/rollup.md §9b).
-- IGNORE INDEX beantwortet dieselbe Frage, ohne etwas anzufassen.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

"""

teile = [kopf]
for mandant in MANDANTEN:
    teile.append(block(f"M114-D-{mandant}", d_korreliert_limit(mandant)))
    teile.append(block(f"M114-E-{mandant}", e_ableitung_mandant(mandant)))
    teile.append(block(f"M114-G-{mandant}", g_ableitung_gesamt(mandant)))
    teile.append(block(f"M114-F-{mandant}", g_ableitung_gesamt(mandant, True)))

teile.append("""-- Gegenprobe: Liefern alle vier Fassungen dieselben Werte? Verglichen wird die
-- Zahl der Prozesse, die Zahl mit gesetzter letzter Bewegung und deren Maximum.
SELECT '=== M114-Z Gleichheitsprobe der Fassungen, NEXANS ===' AS marke;
SELECT 'D' AS fassung, COUNT(*) AS prozesse, COUNT(letzte_bewegung) AS mit_bewegung,
       MAX(letzte_bewegung) AS juengste
FROM (SELECT p.ProcessID,
             (SELECT r.stunde FROM overlord_monitor.message_rollup r
              WHERE r.process_id = p.ProcessID
              ORDER BY r.stunde DESC LIMIT 1) AS letzte_bewegung
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x
UNION ALL
SELECT 'G', COUNT(*), COUNT(letzte_bewegung), MAX(letzte_bewegung)
FROM (SELECT p.ProcessID, r.letzte_bewegung
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      LEFT JOIN (SELECT rr.process_id, MAX(rr.stunde) AS letzte_bewegung
                 FROM overlord_monitor.message_rollup rr
                 GROUP BY rr.process_id) r ON r.process_id = p.ProcessID
      WHERE pm.MandantID = 'NEXANS') y;

-- Die Summenprobe aus M113, ohne LATERAL (MariaDB 10.6 kennt es nicht).
SELECT '=== M113-Z Summenprobe EXISTS gegen JOIN, 12 Monate ===' AS marke;
SELECT m.mandant,
       (SELECT SUM(r.anzahl) FROM overlord_monitor.message_rollup_monat r
        WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
          AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
                      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                      WHERE p.ProcessID = r.process_id
                        AND pm.MandantID = m.mandant))            AS aus_exists,
       (SELECT SUM(r.anzahl) FROM overlord_monitor.message_rollup_monat r
        JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
        WHERE pm.MandantID = m.mandant
          AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01') AS aus_join
FROM (SELECT 'NEXANS' AS mandant UNION ALL SELECT 'VOTG' UNION ALL
      SELECT 'IBIS'   UNION ALL SELECT 'SUTTONS') m;

SELECT '=== 99 fertig ===' AS marke;
""")

ziel = Path(__file__).with_name("s3-letzte-bewegung.sql")
ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
print(f"geschrieben: {ziel}  ({ziel.stat().st_size} Byte)")
