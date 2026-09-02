#!/usr/bin/env python3
"""Erzeugt s2-fassungen.sql — den Fassungsvergleich fuer die Prozessansicht (M112, M113).

Der Erzeuger steht hier, damit die 6 Laeufe je Fall nicht von Hand kopiert werden
muessen. Er ist Messwerkzeug und kein Anwendungscode; er schreibt ausschliesslich
in scripts/messung-prozessansicht/ und ist nach Regel S1 unbedenklich: Das
erzeugte SQL enthaelt ausschliesslich SELECT, SET und EXPLAIN.

Aufruf:  python scripts/messung-prozessansicht/erzeuge-s2.py
"""

from pathlib import Path

# Der Dev-Anker aus common/ZeitConfig. Alle Fenstergrenzen sind daraus
# hergeleitet, wie Dashboardzeitraum.fenster(jetzt) sie rechnet: Der Eimer, in
# dem `jetzt` liegt, ist der letzte und gehoert dazu; die obere Grenze ist der
# Anfang des naechsten Eimers und ausschliessend.
ANKER = "2025-12-30 04:09:47"

FENSTER = {
    # code: (tabelle, eimerspalte, von, bis)
    "48H": ("overlord_monitor.message_rollup", "stunde",
            "2025-12-28 05:00:00", "2025-12-30 05:00:00"),
    "30T": ("overlord_monitor.message_rollup_tag", "tag",
            "2025-12-01", "2025-12-31"),
    "12M": ("overlord_monitor.message_rollup_monat", "monat",
            "2025-01-01", "2026-01-01"),
}

MANDANTEN = ["NEXANS", "VOTG", "IBIS", "SUTTONS"]

LAEUFE = 6  # ein Aufwaermlauf plus fuenf


def block(fall, sql):
    """Ein Messfall: Plan, sechs Laeufe, Auswertung."""
    zeilen = []
    zeilen.append(f"SELECT '=== {fall} · Plan (Regel L15) ===' AS marke;")
    zeilen.append("EXPLAIN " + sql)
    zeilen.append(f"SELECT '=== {fall} · sechs Laeufe ===' AS marke;")
    zeilen.append("SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;")
    for _ in range(LAEUFE):
        zeilen.append(sql)
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


# ---------------------------------------------------------------------------
# M112 — „letzte Bewegung": MAX(stunde) je Prozess, fensterunabhaengig
#
# Drei Fassungen derselben Frage. Sie unterscheiden sich darin, WER den Zugriff
# treibt — der Mandant oder der Rollup.
# ---------------------------------------------------------------------------

def m112_a(mandant):
    """Fassung A — korrelierte Unterabfrage je Prozess.

    Treiber ist der Mandant: Aus ProjectMandant fallen die Prozesse, und je
    Prozess folgt ein Zugriff auf message_rollup_prozess_idx. Das ist die
    Fassung, die der Baum ohnehin braucht — er listet ALLE Prozesse des
    Mandanten, auch die ohne eine einzige Rollupzeile.
    """
    return (
        "SELECT p.ProcessID, p.ProcessName,\n"
        "       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r\n"
        "        WHERE r.process_id = p.ProcessID) AS letzte_bewegung\n"
        "FROM GlassfishDB.Process p\n"
        "JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
        f"WHERE pm.MandantID = '{mandant}'\n"
        "ORDER BY p.ProcessName;")


def m112_b(mandant):
    """Fassung B — LEFT JOIN auf die ueber ALLE Prozesse gruppierte Ableitung.

    Treiber ist der Rollup: Erst wird MAX(stunde) fuer alle 738 Prozesse des
    Gesamtbestands gerechnet, danach auf den Mandanten eingeschraenkt. Das ist
    die Fassung, die M111 gefahren hat.
    """
    return (
        "SELECT p.ProcessID, p.ProcessName, r.letzte_bewegung\n"
        "FROM GlassfishDB.Process p\n"
        "JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
        "LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung\n"
        "           FROM overlord_monitor.message_rollup\n"
        "           GROUP BY process_id) r ON r.process_id = p.ProcessID\n"
        f"WHERE pm.MandantID = '{mandant}'\n"
        "ORDER BY p.ProcessName;")


def m112_c(mandant):
    """Fassung C — wie A, aber mit dem Katalog daneben.

    Das ist die vollstaendige Geruestabfrage des Baums: Prozess, Name, Partner,
    Richtung, Pflegestatus und letzte Bewegung in einem Statement. Sie ist der
    Kandidat fuer den gebauten Code; A und B sagen, was der Zusatz kostet.
    """
    return (
        "SELECT p.ProcessID, p.ProcessName, c.partner, c.richtung, c.pflegestatus,\n"
        "       (SELECT MAX(r.stunde) FROM overlord_monitor.message_rollup r\n"
        "        WHERE r.process_id = p.ProcessID) AS letzte_bewegung\n"
        "FROM GlassfishDB.Process p\n"
        "JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
        "LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID\n"
        f"WHERE pm.MandantID = '{mandant}'\n"
        "ORDER BY p.ProcessName;")


# ---------------------------------------------------------------------------
# M113 — die Kennzahlen im Fenster: je Prozess und Rohstatus eine Summe
#
# Gruppiert wird nach dem ROHSTATUS und nicht nach der Einordnung (E-g): Die
# Einordnung ist eine Regel, die sich aendern kann, der Rohwert ist eine
# Tatsache. Sie entsteht beim Lesen in Java, ueber MessageStatusClassifier.
#
# Zwei Fassungen der Mandantenkette, und der Unterschied ist nicht nur Tempo:
# ProjectMandant ist n:m, ein JOIN kann jede Rollupzeile vervielfachen und
# damit DIE SUMME. Gemessen wird trotzdem beides, weil der Auftrag es verlangt.
# ---------------------------------------------------------------------------

def m113_exists(mandant, code):
    tabelle, eimer, von, bis = FENSTER[code]
    return (
        "SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl\n"
        f"FROM {tabelle} r\n"
        f"WHERE r.{eimer} >= '{von}' AND r.{eimer} < '{bis}'\n"
        "  AND EXISTS (SELECT 1 FROM GlassfishDB.Process p\n"
        "              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
        f"              WHERE p.ProcessID = r.process_id AND pm.MandantID = '{mandant}')\n"
        "GROUP BY r.process_id, r.message_status;")


def m113_join(mandant, code):
    tabelle, eimer, von, bis = FENSTER[code]
    return (
        "SELECT r.process_id, r.message_status, SUM(r.anzahl) AS anzahl\n"
        f"FROM {tabelle} r\n"
        "JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id\n"
        "JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID\n"
        f"WHERE pm.MandantID = '{mandant}'\n"
        f"  AND r.{eimer} >= '{von}' AND r.{eimer} < '{bis}'\n"
        "GROUP BY r.process_id, r.message_status;")


kopf = f"""-- Messung M112 und M113: DIE ZWEI STATEMENTS DER PROZESSANSICHT, IN FASSUNGEN
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 4
-- Ergebnis: docs/process-view.md
--
-- ERZEUGT von scripts/messung-prozessansicht/erzeuge-s2.py — nicht von Hand
-- aendern, sondern den Erzeuger aendern und neu erzeugen.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- M112  „letzte Bewegung" — MAX(stunde) je Prozess, FENSTERUNABHAENGIG. Drei
--       Fassungen: korrelierte Unterabfrage (A), Ableitung ueber den ganzen
--       Bestand (B), und A mit dem Katalog daneben (C, der Kandidat).
-- M113  die Kennzahlen im Fenster, je Ebene und je Fassung der Mandantenkette.
--
-- Z1: Alle Fenstergrenzen sind Literale, hergeleitet aus dem Dev-Anker {ANKER}.
-- G1: Ausgegeben werden Kennungen und Zahlen des Konfigurationsvokabulars,
--     keine Partnernamen — die Auswertung unten zaehlt nur.
--
-- Die Zeitgrenze steht auf 60 s und nicht auf den 10 s des Lese-Pools: Eine
-- Fassung, die reisst, soll ihre Laufzeit noch nennen koennen. Wo eine Fassung
-- ueber 10 s liegt, ist sie fuer die Anwendung unbrauchbar — das steht dann im
-- Befund und nicht in einem abgebrochenen Lauf.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 1000;

"""

teile = [kopf]

teile.append("-- ####################  M112 — letzte Bewegung  ####################\n\n")
for mandant in MANDANTEN:
    teile.append(block(f"M112-A-{mandant}", m112_a(mandant)))
    teile.append(block(f"M112-B-{mandant}", m112_b(mandant)))
    teile.append(block(f"M112-C-{mandant}", m112_c(mandant)))

teile.append("-- ####################  M113 — Kennzahlen im Fenster  ####################\n\n")
for mandant in MANDANTEN:
    for code in ("48H", "30T", "12M"):
        teile.append(block(f"M113-EXISTS-{mandant}-{code}", m113_exists(mandant, code)))
        teile.append(block(f"M113-JOIN-{mandant}-{code}", m113_join(mandant, code)))

teile.append("""-- ####################  Die Gegenprobe zur Vervielfachung  ####################
-- Sagt der JOIN dieselbe Summe wie das EXISTS? Auf DIESEM Bestand ja, weil
-- kein Projekt an mehreren Mandanten haengt (M74a). Die Probe steht hier,
-- damit die Zahl dieses Bestands nicht mit einer Zusicherung des Schemas
-- verwechselt wird.
SELECT '=== M113-Z Summenprobe EXISTS gegen JOIN, 12 Monate ===' AS marke;
SELECT m.mandant, e.summe AS aus_exists, j.summe AS aus_join, (e.summe = j.summe) AS gleich
FROM (SELECT 'NEXANS' AS mandant UNION ALL SELECT 'VOTG' UNION ALL
      SELECT 'IBIS'   UNION ALL SELECT 'SUTTONS') m
JOIN LATERAL (SELECT SUM(r.anzahl) AS summe
              FROM overlord_monitor.message_rollup_monat r
              WHERE r.monat >= '2025-01-01' AND r.monat < '2026-01-01'
                AND EXISTS (SELECT 1 FROM GlassfishDB.Process p
                            JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
                            WHERE p.ProcessID = r.process_id
                              AND pm.MandantID = m.mandant)) e ON TRUE
JOIN LATERAL (SELECT SUM(r.anzahl) AS summe
              FROM overlord_monitor.message_rollup_monat r
              JOIN GlassfishDB.Process p ON p.ProcessID = r.process_id
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE pm.MandantID = m.mandant
                AND r.monat >= '2025-01-01' AND r.monat < '2026-01-01') j ON TRUE;

SELECT '=== 99 fertig ===' AS marke;
""")

ziel = Path(__file__).with_name("s2-fassungen.sql")
ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
print(f"geschrieben: {ziel}  ({ziel.stat().st_size} Byte)")
