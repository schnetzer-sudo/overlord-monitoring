#!/usr/bin/env python3
"""Erzeugt die Sitzungsdateien der Messung M189 (Fehler live, Teil B: der Prozessbaum) auf SQL-Ebene.

Auftrag:  "Fehler live, Teil B (Prozessbaum)", Stand 18.09.2026
Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 5b

DIE LESUNG IST NICHT ABGETIPPT. Ihr Text kommt aus erzeuge.py (LESUNG, M188) und ist am 18.09.2026
vor der Vorregistrierung erneut aus dem Code gerendert und verglichen worden: zeichengleich
(FehlerLiveRepository, jOOQ-Attrappe mit StatementType.STATIC_STATEMENT, Wegwerf-Programm). Eingesetzt
werden nur Fenster und Mandant. Messblock, Zaehler und Eichung sind dieselben wie in M188.

Sitzungen:
  s3-m189-zaehlung.sql  die Zaehlung, die das Fenster von Tor 1 bestimmt (Regel vorab festgelegt:
                        unter allen Jahresfenstern [von, von + 1 Jahr), deren von eine Stunde mit
                        mindestens einem NEXANS-Fehler ist, das mit den meisten Fehlern; bei
                        Gleichstand das frueheste) — dazu die Eichung der Zaehlung an zwei Zahlen aus
                        M188 (alle NEXANS-Fehler; das Zwoelfmonatsfenster bis 2024-11-01)
  s4-m189-tor1-lesung.sql  Tor 1 — die Lesung im Fenster aus s3; die Zaehlung steht dort noch einmal
                        als erste Abfrage. Zusaetzlich zum Auftrag dieselbe Lesung fuer jedes Fenster
                        der Tore 2 bis 4 (EXPLAIN, Zaehler, beste von fuenf)

Die Fenster der Tore 2 bis 4 sind vom Code ausgegeben (Rollupzeitraum.fenster, Baumfenster.ausAnfrage;
Wegwerf-Programm am 18.09.2026) und stehen so in docs/fehler-live.md Paragraf 5b — Wanduhrzeit der
Quelle, bis ausschliessend.

Aufruf:  python scripts/messung-fehler-live/erzeuge_m189.py s3
         python scripts/messung-fehler-live/erzeuge_m189.py s4 <von aus s3, etwa 2023-10-05T09:00>
Dann:    mysql ... -t < scripts/messung-fehler-live/<datei>.sql > ergebnis/<datei>.txt
"""

import sys
from datetime import datetime
from pathlib import Path

HIER = Path(__file__).resolve().parent
sys.path.insert(0, str(HIER))

import erzeuge  # noqa: E402  (LESUNG, block, Zaehler und Eichung aus M188 — derselbe Messblock)

# Die Fenster der Tore 2 bis 4 je Mandant: (Name, von, bis ausschliessend), Wanduhrzeit der Quelle.
# Das SUTTONS-Fenster 2024-07-01 bis 2025-07-01 ist in Tor 2 und Tor 3 dasselbe und steht einmal.
FENSTER_TORE_2_BIS_4 = {
    "NEXANS": [
        ("T2-typisch-48H", "2025-11-07 17:00:00.0", "2025-11-09 17:00:00.0"),
        ("T2-typisch-12M", "2024-12-01 00:00:00.0", "2025-12-01 00:00:00.0"),
        ("T3-dicht-48H", "2024-10-07 22:00:00.0", "2024-10-09 22:00:00.0"),
        ("T3-dicht-12M", "2023-11-01 00:00:00.0", "2024-11-01 00:00:00.0"),
        ("T4-jahr", "2025-01-01 00:00:00.0", "2026-01-01 00:00:00.0"),
        ("T4-boesfall", "2024-12-30 14:00:00.0", "2025-12-30 03:00:00.0"),
    ],
    "SUTTONS": [
        ("T2-typisch-48H", "2025-06-07 21:00:00.0", "2025-06-09 21:00:00.0"),
        ("T2T3-12M", "2024-07-01 00:00:00.0", "2025-07-01 00:00:00.0"),
        ("T3-dicht-48H", "2025-06-10 11:00:00.0", "2025-06-12 11:00:00.0"),
        ("T4-jahr", "2025-01-01 00:00:00.0", "2026-01-01 00:00:00.0"),
        ("T4-boesfall", "2024-12-30 14:00:00.0", "2025-12-30 03:00:00.0"),
    ],
}

KOPF = """-- Messung M189 -- {titel}
-- Auftrag:  "Fehler live, Teil B (Prozessbaum)", Stand 18.09.2026
-- Vorregistrierung und Ergebnis: docs/fehler-live.md Paragraf 5b
--
-- ERZEUGT von scripts/messung-fehler-live/erzeuge_m189.py. Die Lesung ist aus dem Code
-- gerendert; eingesetzt sind nur Fenster und Mandant.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer: der Lese-Benutzer.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes. Die
--     Rohausgabe traegt Prozesskennungen und bleibt deshalb ausserhalb des
--     Repositorys (ergebnis/s[0-9].txt); eingecheckt wird das gefilterte Protokoll.
-- L9: SET max_statement_time = 60 fuer diese Sitzung.
-- T1: Zeiten werden ausgegeben, nichts wird zugesichert.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;
"""

# Die Fehlerbedingung und die Kette wie in der Zaehlung von M188 (s0): LIKE mit ESCAPE und
# COMMIT_REJECTED, die Kette als EXISTS. Die Stunde ist dieselbe Bildung wie in der Lesung
# (date_format auf die volle Stunde), hier als DATETIME, damit + INTERVAL 1 YEAR rechnet wie
# LocalDateTime.plusYears(1) in Baumfenster.
ZAEHLUNG = r"""
SELECT '=== 01 Die Zaehlung: das Jahresfenster mit den meisten NEXANS-Fehlern ===' AS marke;
WITH je_stunde AS (
  SELECT CAST(DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00') AS DATETIME) AS stunde,
         COUNT(*) AS fehler
  FROM `GlassfishDB`.`Message` m
  WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
    AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                  JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
                 WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
  GROUP BY CAST(DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00') AS DATETIME)
)
SELECT a.stunde AS von, a.stunde + INTERVAL 1 YEAR AS bis_ausschliessend,
       SUM(b.fehler) AS fehler_im_fenster, COUNT(*) AS fehlerstunden_im_fenster
FROM je_stunde a
JOIN je_stunde b ON b.stunde >= a.stunde AND b.stunde < a.stunde + INTERVAL 1 YEAR
GROUP BY a.stunde
ORDER BY fehler_im_fenster DESC, a.stunde ASC
LIMIT 5;

SELECT '=== 01a Eichung der Zaehlung: zwei Zahlen aus M188 ===' AS marke;
SELECT 'M189-ZAEHLUNG-EICHUNG' AS fall,
       COUNT(*) AS fehler_nexans_ganzer_bestand,
       SUM(m.MessageLastUpdate >= '2023-11-01 00:00:00' AND m.MessageLastUpdate < '2024-11-01 00:00:00')
         AS fehler_bis_2024_11_01,
       COUNT(DISTINCT DATE_FORMAT(m.MessageLastUpdate, '%Y-%m-%d %H:00:00')) AS fehlerstunden
FROM `GlassfishDB`.`Message` m
WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\' OR m.MessageStatus = 'COMMIT_REJECTED')
  AND EXISTS (SELECT 1 FROM `GlassfishDB`.`Process` p
                JOIN `GlassfishDB`.`ProjectMandant` pm ON pm.ProjectID = p.ProjectID
               WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS');
"""


def eichung() -> str:
    return erzeuge.EICHUNG.replace("M188-EICHUNG-WAND", "M189-EICHUNG-WAND").format(
        zaehler_vorher=erzeuge.zaehler_lesen("0"),
        zaehler_nachher=erzeuge.zaehler_lesen("1"),
        zaehler_ausgabe=erzeuge.zaehler_ausgabe("M189-EICHUNG-ZAEHLER"),
    )


def literal(zeitpunkt: datetime) -> str:
    return zeitpunkt.strftime("%Y-%m-%d %H:%M:%S") + ".0"


def sitzung_zaehlung() -> str:
    return KOPF.format(titel="Sitzung 3: die Zaehlung fuer Tor 1") + ZAEHLUNG


def sitzung_tor1(von_text: str) -> str:
    von = datetime.fromisoformat(von_text)
    if von.minute or von.second:
        raise SystemExit("von muss auf einer vollen Stunde liegen: " + von_text)
    # [von, von + 1 Jahr) — wie LocalDateTime.plusYears(1): der 29. Februar faellt auf den 28.
    try:
        bis = von.replace(year=von.year + 1)
    except ValueError:
        bis = von.replace(year=von.year + 1, day=28)
    teile = [KOPF.format(titel="Sitzung 4: Tor 1, die Lesung im Jahresfenster aus Sitzung 3")]
    teile.append(ZAEHLUNG)
    teile.append(eichung())
    teile.append(
        f"\nSELECT '=== 02 Tor 1: das Fenster aus Sitzung 3, {literal(von)} bis {literal(bis)} ===' AS marke;\n"
    )
    sql = erzeuge.LESUNG.format(von=literal(von), bis=literal(bis), mandant="NEXANS")
    teile.append(erzeuge.block("M189-1-NEXANS-JAHRESFENSTER", sql))
    teile.append("\nSELECT '=== 03 Zusaetzlich: die Fenster der Tore 2 bis 4 ===' AS marke;\n")
    for mandant, fenster in FENSTER_TORE_2_BIS_4.items():
        for name, von_f, bis_f in fenster:
            sql = erzeuge.LESUNG.format(von=von_f, bis=bis_f, mandant=mandant)
            teile.append(erzeuge.block(f"M189-Z-{mandant}-{name}", sql))
    return "".join(teile)


def main() -> None:
    sitzung = sys.argv[1]
    if sitzung == "s3":
        ziel = HIER / "s3-m189-zaehlung.sql"
        ziel.write_text(sitzung_zaehlung(), encoding="utf-8", newline="\n")
    elif sitzung == "s4":
        ziel = HIER / "s4-m189-tor1-lesung.sql"
        ziel.write_text(sitzung_tor1(sys.argv[2]), encoding="utf-8", newline="\n")
    else:
        raise SystemExit("unbekannte Sitzung: " + sitzung)
    print("geschrieben:", ziel.name)


if __name__ == "__main__":
    main()
