#!/usr/bin/env python3
"""Erzeugt die Sitzungsdateien der Messung M188 (Fehler live) auf SQL-Ebene.

Auftrag:  "Fehler live, Teil A (Baustein und Uebersicht)", Stand 18.09.2026
Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 8

DIE STATEMENTTEXTE SIND NICHT ABGETIPPT. Sie sind aus dem Code gerendert
(jOOQ-Attrappe mit StatementType.STATIC_STATEMENT, Wegwerf-Test, danach
geloescht); hier werden nur Fenster und Mandant als Literale eingesetzt, so wie
jOOQ sie rendert. Der Text mit Fragezeichen steht woertlich in
DashboardStatementsTest.

Sitzungen:
  s0-zaehlung.sql   die Zaehlung, die den vierten Mandanten von Tor 1 bestimmt
                    (Regel vorab festgelegt: keine einzige Fehlerzeile im ganzen
                    Bestand, davon der mit den meisten Nachrichten, bei Gleichstand
                    der alphabetisch erste)
  s1-tor1-lesung.sql  Tor 1 — die Lesung, vier Mandanten x drei Paare
  s2-tor2-verteilung.sql  Tor 2 — die Verteilung ohne Fehler, beide Sichten, und
                    daneben die Form ohne Zusatzbedingung (erst nach dem Einbau); die
                    Texte stehen gerendert in gerendert-verteilung.txt

Je Statement: EXPLAIN (Regel L15); dann ein Lauf zwischen zwei Lesungen der
Handler-Zaehler (gelesene Indexsaetze, gemessen statt geschaetzt; Eichung am
Anfang der Sitzung); dann ein Aufwaermlauf und fuenf Laeufe, Laufzeit aus
information_schema.PROFILING UND an der Wanduhr des Servers (SYSDATE(6) vor und
nach dem Statement, E-107). Jeder Lauf belegt drei QUERY_IDs (SET, Statement,
SET); ausgewertet wird die mittlere, nach oben begrenzt.

Aufruf:  python scripts/messung-fehler-live/erzeuge.py <sitzung> [vierter Mandant]
Dann:    mysql ... < scripts/messung-fehler-live/<datei>.sql > ergebnis/<datei>.txt
"""

import sys
from pathlib import Path

HIER = Path(__file__).resolve().parent
LAEUFE = 6  # ein Aufwaermlauf, dann beste von fuenf — dieselbe Form wie M146 und M185

# Die Fenster am Anker 2025-12-30 04:09:47, aus Rollupzeitraum.fenster(Anker) — Wanduhrzeit der
# Quelle, bis ausschliessend. Gerendert, nicht gerechnet (siehe Kopf).
FENSTER = [
    ("48H", "2025-12-28 05:00:00.0", "2025-12-30 05:00:00.0"),
    ("30T", "2025-12-01 00:00:00.0", "2025-12-31 00:00:00.0"),
    ("12M", "2025-01-01 00:00:00.0", "2026-01-01 00:00:00.0"),
]

# Die Lesung, gerendert aus FehlerLiveRepository.ausDerQuelle. Die Rueckstriche sind die von
# jOOQ: 'ERROR\\_%' escape '\\' ist im SQL-Literal ERROR\_% mit dem Fluchtzeichen \.
LESUNG = (
    r"select date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`, '%Y-%m-%d %H:00:00'),"
    r" `GlassfishDB`.`Message`.`ProcessID`, `GlassfishDB`.`Message`.`MessageStatus`, count(*)"
    r" from `GlassfishDB`.`Message` where ((`GlassfishDB`.`Message`.`MessageStatus` like"
    r" 'ERROR\\_%' escape '\\' or `GlassfishDB`.`Message`.`MessageStatus` = 'COMMIT_REJECTED')"
    r" and `GlassfishDB`.`Message`.`MessageLastUpdate` >= timestamp '{von}' and"
    r" `GlassfishDB`.`Message`.`MessageLastUpdate` < timestamp '{bis}' and exists (select 1 as"
    r" `one` from `GlassfishDB`.`Process` as `fehler_process` join"
    r" `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` ="
    r" `fehler_process`.`ProjectID` where (`fehler_process`.`ProcessID` ="
    r" `GlassfishDB`.`Message`.`ProcessID` and `GlassfishDB`.`ProjectMandant`.`MandantID` ="
    r" '{mandant}'))) group by date_format(`GlassfishDB`.`Message`.`MessageLastUpdate`,"
    r" '%Y-%m-%d %H:00:00'), `GlassfishDB`.`Message`.`ProcessID`,"
    r" `GlassfishDB`.`Message`.`MessageStatus`"
)

KOPF = """-- Messung M188 -- {titel}
-- Auftrag:  "Fehler live, Teil A (Baustein und Uebersicht)", Stand 18.09.2026
-- Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 8
--
-- ERZEUGT von scripts/messung-fehler-live/erzeuge.py. Die Statementtexte sind aus
-- dem Code gerendert; eingesetzt sind nur Fenster und Mandant.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/*.txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;
"""

EICHUNG = """
SELECT '=== 00a Eichung: Wanduhr um ein leeres Statement ===' AS marke;
SET @t0 = SYSDATE(6); SELECT 1; SET @e1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M188-EICHUNG-WAND' AS fall, ROUND(@e1 / 1000, 3) AS ms1, ROUND(@e2 / 1000, 3) AS ms2,
       ROUND(@e3 / 1000, 3) AS ms3;

SELECT '=== 00b Eichung: Handler-Zaehler um ein leeres Statement ===' AS marke;
{zaehler_vorher}
SELECT 1;
{zaehler_nachher}
{zaehler_ausgabe}
"""

ZAEHLER = [
    "HANDLER_READ_KEY",
    "HANDLER_READ_NEXT",
    "HANDLER_READ_PREV",
    "HANDLER_READ_FIRST",
    "HANDLER_READ_RND",
    "HANDLER_READ_RND_NEXT",
    "HANDLER_ICP_ATTEMPTS",
    "HANDLER_ICP_MATCH",
]


def zaehler_lesen(suffix: str) -> str:
    spalten = ",\n       ".join(
        f"SUM(IF(VARIABLE_NAME = '{name}', VARIABLE_VALUE, 0))" for name in ZAEHLER
    )
    ziele = ", ".join(f"@{name.lower()}_{suffix}" for name in ZAEHLER)
    namen = ", ".join(f"'{name}'" for name in ZAEHLER)
    return (
        f"SELECT {spalten}\nINTO {ziele}\n"
        f"FROM information_schema.SESSION_STATUS WHERE VARIABLE_NAME IN ({namen});"
    )


def zaehler_ausgabe(fall: str) -> str:
    spalten = ",\n       ".join(
        f"@{name.lower()}_1 - @{name.lower()}_0 AS {name.lower().replace('handler_', '')}"
        for name in ZAEHLER
    )
    return f"SELECT '{fall}' AS fall,\n       {spalten};"


def block(name: str, sql: str) -> str:
    zeilen = [f"\nSELECT '=== {name} · Plan (Regel L15) ===' AS marke;", f"EXPLAIN {sql};"]
    zeilen.append(f"SELECT '=== {name} · Handler-Zaehler, ein Lauf ===' AS marke;")
    zeilen.append(zaehler_lesen("0"))
    zeilen.append(sql + ";")
    zeilen.append(zaehler_lesen("1"))
    zeilen.append(zaehler_ausgabe(f"{name}-ZAEHLER"))
    zeilen.append(f"SELECT '=== {name} · sechs Laeufe ===' AS marke;")
    zeilen.append("SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;")
    for lauf in range(1, LAEUFE + 1):
        zeilen.append("SET @t0 = SYSDATE(6);")
        zeilen.append(sql + ";")
        zeilen.append(f"SET @w{lauf} = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));")
    wand_alle = ", ".join(f"ROUND(@w{i} / 1000, 3)" for i in range(2, LAEUFE + 1))
    wand_beste = ", ".join(f"@w{i}" for i in range(2, LAEUFE + 1))
    obergrenze = 1 + 3 * LAEUFE
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
      WHERE QUERY_ID > @basis + 1 AND QUERY_ID <= @basis + {obergrenze}
        AND (QUERY_ID - @basis) % 3 = 0
      GROUP BY QUERY_ID) t;"""
    )
    return "\n".join(zeilen) + "\n"


# Die Zaehlung fuer Tor 1: je Mandant die Fehlerzeilen im ganzen Bestand (ueber die
# Fehlerbedingung, Kette als EXISTS) und die Nachrichten im ganzen Bestand (aus
# message_rollup, das auf der Testkopie den ganzen Bestand deckt; die Deckung wird in
# derselben Sitzung gegen COUNT(*) aus Message geprueft).
ZAEHLUNG = r"""
SELECT '=== 01 Die Zaehlung: Fehlerzeilen und Nachrichten je Mandant, ganzer Bestand ===' AS marke;
SELECT md.MandantID AS mandant,
       (SELECT COUNT(*) FROM `GlassfishDB`.`Message` m
         WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
           AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                         JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                        WHERE p.ProcessID = m.ProcessID AND pm.MandantID = md.MandantID))
         AS fehlerzeilen,
       (SELECT COALESCE(SUM(r.anzahl), 0) FROM `overlord_monitor`.`message_rollup` r
         WHERE EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                         JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                        WHERE p.ProcessID = r.process_id AND pm.MandantID = md.MandantID))
         AS nachrichten
FROM `GlassfishDB`.`Mandant` md
ORDER BY fehlerzeilen, nachrichten DESC, md.MandantID;

SELECT '=== 01a Die Deckung: message_rollup gegen Message, ganzer Bestand ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM `overlord_monitor`.`message_rollup`) AS rollup_gesamt,
       (SELECT COUNT(*) FROM `GlassfishDB`.`Message`) AS message_gesamt;

SELECT '=== 01b Die Fehlerzeilen im ganzen Bestand, Rohwert x Einordnung ===' AS marke;
SELECT m.MessageStatus AS rohwert, COUNT(*) AS anzahl
FROM `GlassfishDB`.`Message` m
WHERE m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED'
GROUP BY m.MessageStatus ORDER BY m.MessageStatus;
"""


def sitzung_zaehlung() -> str:
    return KOPF.format(titel="Sitzung 0: die Zaehlung fuer Tor 1") + ZAEHLUNG


def sitzung_tor1(vierter: str) -> str:
    teile = [KOPF.format(titel="Sitzung 1: Tor 1, die Lesung")]
    teile.append(
        EICHUNG.format(
            zaehler_vorher=zaehler_lesen("0"),
            zaehler_nachher=zaehler_lesen("1"),
            zaehler_ausgabe=zaehler_ausgabe("M188-EICHUNG-ZAEHLER"),
        )
    )
    teile.append(ZAEHLUNG)
    for mandant in ("NEXANS", "SUTTONS", "VOTG", vierter):
        for paar, von, bis in FENSTER:
            sql = LESUNG.format(von=von, bis=bis, mandant=mandant)
            teile.append(block(f"M188-1-{mandant}-{paar}", sql))
    return "".join(teile)


def verteilung_gerendert() -> dict:
    """Die Verteilung in beiden Fassungen, statisch gerendert aus dem Code (Wegwerf-Test gegen
    die jOOQ-Attrappe, 18.09.2026, danach geloescht) — je Zeile Fassung|Paar|Sicht|Statement, der
    Mandant als PLATZHALTERMANDANT. VERT ist die heutige Fassung, OHNE die ohne Fehler."""
    texte = {}
    for zeile in (HIER / "gerendert-verteilung.txt").read_text(encoding="utf-8").splitlines():
        fassung, paar, sicht, sql = zeile.split("|", 3)
        texte[(fassung, paar, sicht)] = sql
    return texte


def sitzung_tor2() -> str:
    teile = [KOPF.format(titel="Sitzung 2: Tor 2, die Verteilung ohne Fehler")]
    teile.append(
        EICHUNG.format(
            zaehler_vorher=zaehler_lesen("0"),
            zaehler_nachher=zaehler_lesen("1"),
            zaehler_ausgabe=zaehler_ausgabe("M188-EICHUNG-ZAEHLER"),
        )
    )
    texte = verteilung_gerendert()
    for mandant in ("NEXANS", "SUTTONS"):
        for paar, _von, _bis in FENSTER:
            for sicht in ("RICHTUNG", "PARTNER"):
                for fassung, name in (("VERT", "heute"), ("OHNE", "ohne-fehler")):
                    sql = texte[(fassung, paar, sicht)].replace(
                        "'PLATZHALTERMANDANT'", f"'{mandant}'"
                    )
                    teile.append(block(f"M188-2-{mandant}-{paar}-{sicht}-{name}", sql))
    return "".join(teile)


def main() -> None:
    sitzung = sys.argv[1]
    if sitzung == "s0":
        ziel = HIER / "s0-zaehlung.sql"
        ziel.write_text(sitzung_zaehlung(), encoding="utf-8", newline="\n")
    elif sitzung == "s1":
        ziel = HIER / "s1-tor1-lesung.sql"
        ziel.write_text(sitzung_tor1(sys.argv[2]), encoding="utf-8", newline="\n")
    elif sitzung == "s2":
        ziel = HIER / "s2-tor2-verteilung.sql"
        ziel.write_text(sitzung_tor2(), encoding="utf-8", newline="\n")
    else:
        raise SystemExit("unbekannte Sitzung: " + sitzung)
    print("geschrieben:", ziel.name)


if __name__ == "__main__":
    main()
