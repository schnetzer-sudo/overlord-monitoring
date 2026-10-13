#!/usr/bin/env python3
"""Erzeugt die Sitzungsdateien der Messung M191 (EERP_FAILED als Fehler) auf SQL-Ebene.

Auftrag:  "EERP_FAILED als Fehlerstatus einordnen", 13.10.2026
Ergebnis: docs/message-status.md, Abschnitt "EERP_FAILED"

DIE STATEMENTTEXTE SIND NICHT ABGETIPPT. gerendert.txt ist aus dem Code gerendert
(jOOQ-Attrappe mit StatementType.STATIC_STATEMENT, Wegwerf-Test, danach geloescht),
in der NEUEN Fassung der Fehlerbedingung. Die ALTE Fassung entsteht hier durch
genau eine Ersetzung -- IN ('COMMIT_REJECTED', 'EERP_FAILED') -> = 'COMMIT_REJECTED'.
Fuer die Lesung ist die Ersetzung gegen den in M188 gemessenen Text geprueft
(scripts/messung-fehler-live/erzeuge.py, LESUNG): zeichengleich, beide Mandanten.

Zwei Statements, je alt und neu, je NEXANS und SUTTONS:
  LESUNG  FehlerLiveRepository.ausDerQuelle -- die Fehlerkachel der Uebersicht und der
          Prozessbaum, live ueber Message; Fenster 30T am Anker (wie M188)
  LISTE   NachrichtenRepository.finde mit Statusfilter FEHLER -- die Liste; 30 Tage
          bis zum Anker der Dev-Uhr, 50 Zeilen (wie F0-*-30d-fehler aus 10b-1)

Je Statement: EXPLAIN (Regel L15); der Bereichszugriff aus dem Optimizer-Trace
(welche Indexbereiche auf MessageStatusIDX der Plan liest -- zwei alt, drei neu);
ein Lauf zwischen zwei Lesungen der Handler-Zaehler; dann ein Aufwaermlauf und
fuenf Laeufe, Laufzeit aus information_schema.PROFILING UND an der Wanduhr
(SYSDATE(6), E-107). Reihenfolge NEXANS alt->neu, SUTTONS neu->alt, damit keine
Fassung immer als zweite auf dem waermeren Puffer laeuft.

Sitzungen:
  s1-statuszaehlung.sql  von Hand, die Statuszaehlung des Auftrags (kein Generator)
  s2-lesung.sql          python erzeuge.py s2
  s3-liste.sql           python erzeuge.py s3
  s4-weitere.sql         python erzeuge.py s4 -- die drei uebrigen Statements mit der
                         Fehlerbedingung, gerendert in gerendert-weitere.txt:
                         BLOCK6     DashboardRepository.zuletztAufgefallen, 48H, 10 Prozesse
                         VERTEILUNG DashboardRepository.verteilungOhneFehler, 48H, Partnersicht
                         VERENGUNG  VerengungRepository.frageStufe, Statusfilter FEHLER, die
                                    weiteste Stufe des 30-Tage-Fensters, Schwelle 51, Wasserstand
                                    2026-08-27 15:00 (MAX(fenster_bis) der Testkopie am 13.10.2026)
  s5-rest.sql            python erzeuge.py s5 -- nach der Durchsicht nachgereicht, gerendert in
                         gerendert-rest.txt: LISTE_UNGEKLAERT und VERENGUNG_UNGEKLAERT (der
                         Statusfilter UNGEKLAERT traegt NOT fehlerBedingung) und
                         VERTEILUNG_<paar>_<sicht> fuer die fuenf Formen ausser 48H/PARTNER

Aufruf:  mysql ... -t < scripts/messung-eerp-failed/<datei>.sql > ergebnis/<datei>.txt
Danach:  python scripts/messung-eerp-failed/filtere.py ergebnis/s2.txt > ergebnis/s2.gefiltert.txt
"""

import re
import sys
from pathlib import Path

HIER = Path(__file__).resolve().parent
LAEUFE = 6  # ein Aufwaermlauf, dann beste von fuenf -- dieselbe Form wie M146, M185, M188

KOPF = """-- Messung M191 -- {titel}
-- Auftrag:  "EERP_FAILED als Fehlerstatus einordnen", 13.10.2026
-- Ergebnis: docs/message-status.md, Abschnitt "EERP_FAILED"
--
-- ERZEUGT von scripts/messung-eerp-failed/erzeuge.py. Die Statementtexte sind aus
-- dem Code gerendert (gerendert.txt); die alte Fassung entsteht durch eine Ersetzung.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/s*.txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;
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


EICHUNG = """
SELECT '=== 00a Eichung: Wanduhr um ein leeres Statement ===' AS marke;
SET @t0 = SYSDATE(6); SELECT 1; SET @e1 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e2 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SET @t0 = SYSDATE(6); SELECT 1; SET @e3 = TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6));
SELECT 'M191-EICHUNG-WAND' AS fall, ROUND(@e1 / 1000, 3) AS ms1, ROUND(@e2 / 1000, 3) AS ms2,
       ROUND(@e3 / 1000, 3) AS ms3;

SELECT '=== 00b Eichung: Handler-Zaehler um ein leeres Statement ===' AS marke;
{zaehler_vorher}
SELECT 1;
{zaehler_nachher}
{zaehler_ausgabe}
"""


def trace(name: str, sql: str) -> str:
    """Die Indexbereiche, die der Plan erwaegt und waehlt -- aus dem Optimizer-Trace."""
    return f"""
SELECT '=== {name} · Bereiche (Optimizer-Trace) ===' AS marke;
SET optimizer_trace = 'enabled=on';
EXPLAIN {sql};
SELECT '{name}-BEREICHE' AS fall,
       JSON_EXTRACT(TRACE, '$**.chosen_range_access_summary') AS gewaehlt,
       JSON_EXTRACT(TRACE, '$**.range_scan_alternatives') AS erwogen
FROM information_schema.OPTIMIZER_TRACE;
SET optimizer_trace = 'enabled=off';
"""


def block(name: str, sql: str) -> str:
    zeilen = [f"\nSELECT '=== {name} · Plan (Regel L15) ===' AS marke;", f"EXPLAIN {sql};"]
    zeilen.append(trace(name, sql))
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


# Fuer Sitzung 4: dieselbe Ersetzung, auch auf der Rohwertspalte des Rollups.
NEU_RE = re.compile(r"`(MessageStatus|message_status)` in \('COMMIT_REJECTED', 'EERP_FAILED'\)")


def gerendert(datei: str = "gerendert.txt") -> dict:
    texte = {}
    for zeile in (HIER / datei).read_text(encoding="utf-8").splitlines():
        art, mandant, sql = zeile.split("|", 2)
        alt, anzahl = NEU_RE.subn(lambda treffer: f"`{treffer.group(1)}` = 'COMMIT_REJECTED'", sql)
        if anzahl != 1:
            raise SystemExit(f"{art}/{mandant}: die neue Bedingung steht nicht genau einmal da")
        texte[(art, mandant, "neu")] = sql
        texte[(art, mandant, "alt")] = alt
    return texte


def sitzung(art: str, titel: str, datei: str = "gerendert.txt") -> str:
    texte = gerendert(datei)
    teile = [KOPF.format(titel=titel)]
    teile.append(
        EICHUNG.format(
            zaehler_vorher=zaehler_lesen("0"),
            zaehler_nachher=zaehler_lesen("1"),
            zaehler_ausgabe=zaehler_ausgabe("M191-EICHUNG-ZAEHLER"),
        )
    )
    for mandant, reihe in (("NEXANS", ("alt", "neu")), ("SUTTONS", ("neu", "alt"))):
        for fassung in reihe:
            sql = texte[(art, mandant, fassung)]
            teile.append(block(f"M191-{art}-{mandant}-{fassung}", sql))
    return "".join(teile)


def main() -> None:
    welche = sys.argv[1]
    if welche == "s2":
        ziel = HIER / "s2-lesung.sql"
        ziel.write_text(
            sitzung("LESUNG", "Sitzung 2: die Lesung, alt gegen neu"), encoding="utf-8", newline="\n"
        )
    elif welche == "s3":
        ziel = HIER / "s3-liste.sql"
        ziel.write_text(
            sitzung("LISTE", "Sitzung 3: die Liste mit Statusfilter FEHLER, alt gegen neu"),
            encoding="utf-8",
            newline="\n",
        )
    elif welche == "s4":
        ziel = HIER / "s4-weitere.sql"
        teile = [
            sitzung(
                "BLOCK6",
                "Sitzung 4: Block 6, die Verteilung ohne Fehler und die Verengung, alt gegen neu",
                "gerendert-weitere.txt",
            )
        ]
        # Kopf und Eichung stehen einmal; die beiden weiteren Statements haengen dahinter.
        for art in ("VERTEILUNG", "VERENGUNG"):
            texte = gerendert("gerendert-weitere.txt")
            for mandant, reihe in (("NEXANS", ("alt", "neu")), ("SUTTONS", ("neu", "alt"))):
                for fassung in reihe:
                    teile.append(
                        block(f"M191-{art}-{mandant}-{fassung}", texte[(art, mandant, fassung)])
                    )
        ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
    elif welche == "s5":
        # Nachgereicht nach der Durchsicht: die Statusfilter UNGEKLAERT von Liste und Verengung
        # (NOT fehlerBedingung) und die fuenf uebrigen Formen der Verteilung ohne Fehler.
        ziel = HIER / "s5-rest.sql"
        texte = gerendert("gerendert-rest.txt")
        arten = list(dict.fromkeys(art for art, _, _ in texte))
        teile = [
            KOPF.format(
                titel="Sitzung 5: UNGEKLAERT (Liste, Verengung) und die uebrigen Verteilungen"
            ),
            EICHUNG.format(
                zaehler_vorher=zaehler_lesen("0"),
                zaehler_nachher=zaehler_lesen("1"),
                zaehler_ausgabe=zaehler_ausgabe("M191-EICHUNG-ZAEHLER"),
            ),
        ]
        for art in arten:
            for mandant, reihe in (("NEXANS", ("alt", "neu")), ("SUTTONS", ("neu", "alt"))):
                for fassung in reihe:
                    teile.append(
                        block(f"M191-{art}-{mandant}-{fassung}", texte[(art, mandant, fassung)])
                    )
        ziel.write_text("".join(teile), encoding="utf-8", newline="\n")
    else:
        raise SystemExit("unbekannte Sitzung: " + welche)
    print("geschrieben:", ziel.name)


if __name__ == "__main__":
    main()
