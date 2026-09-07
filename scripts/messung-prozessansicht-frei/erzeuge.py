#!/usr/bin/env python3
"""Erzeugt die fuenf Sitzungsdateien der Messrunde 10c-4a: M147 bis M151.

Auftrag:  "Prozessansicht -- freies Zeitfenster, Messrunde (Schritt 10c-4a)",
          Stand 07.09.2026
Ergebnis: docs/process-view.md Paragraf 30 ff.

DER STATEMENTTEXT IST NICHT ABGETIPPT. Er kommt aus
scripts/messung-prozessansicht/gerendert.txt -- dem Text, der in M116 aus
ProzessbaumRepository gegen eine jOOQ-Attrappe gefallen ist und den
ProzessbaumStatementsTest woertlich festhaelt. Variiert werden AUSSCHLIESSLICH
die Ebene (Stunde / Tag / Monat) und das Bereichspraedikat; jede Ersetzung wird
gezaehlt und muss genau einmal greifen, sonst bricht der Generator ab. Die vier
Eigenschaften aus process-view.md Paragraf 6 bleiben damit unangetastet:
Deckelung statt MAX() (betrifft nur das Geruest), EXISTS statt Join, keine
Funktion um den Eimerschluessel, jede Lesung auf ihrer Ebene.

Die Sitzungen:
  s0-rahmen.sql                 Rahmen, Bestand, Vergleichsanker (M116 wiederholt),
                                Belegungsprobe je Fenster und Ebene
  s1-m147-stundenebene.sql      M147: vier Mandanten x fuenf Spannen, Stundenebene
  s2-m148-tagesebene.sql        M148: vier Mandanten x fuenf Spannen, Tagesebene
  s3-m149-m150-zerlegung.sql    M149 (Z-U, ein Statement) und M150 (Z-D, drei
                                Statements) ueber den Boesfall, vier Mandanten
  s4-m151-gleichheitsprobe.sql  M151: zerlegte Summe gegen ungeteilte Stundenlesung,
                                je (process_id, message_status), vier Mandanten

Aufruf:  python scripts/messung-prozessansicht-frei/erzeuge.py
"""

from pathlib import Path

HIER = Path(__file__).resolve().parent
GERENDERT = HIER.parent / "messung-prozessansicht" / "gerendert.txt"
LAEUFE = 6  # ein Aufwaermlauf, dann beste von fuenf -- dieselbe Form wie M116

MANDANTEN = ["NEXANS", "VOTG", "IBIS", "SUTTONS"]

# Alle Fenster enden auf 2025-12-30 03:00 -- unterhalb des Dev-Ankers
# (2025-12-30 04:09:47) und auf einer Stundengrenze, vollstaendig im dichten
# Bestand (2024-10-01 bis 2025-12-30).
BIS_STUNDE = "2025-12-30 03:00:00"
BIS_TAG = "2025-12-30"

# (Code, von auf der Stundenebene [M147], von auf der Tagesebene [M148])
SPANNEN = [
    ("24H", "2025-12-29 03:00:00", "2025-12-29"),
    ("7T", "2025-12-23 03:00:00", "2025-12-23"),
    ("30T", "2025-11-30 03:00:00", "2025-11-30"),
    ("90T", "2025-10-01 03:00:00", "2025-10-01"),
    ("365T", "2024-12-30 03:00:00", "2024-12-30"),
]

# Der Boesfall fuer M149/M150/M151: beide Enden krumm, fuenf Abschnitte, alle
# drei Ebenen beteiligt. 10 + 2 + 11 + 29 + 3 = 55 Zeitscheiben.
BOES_VON = "2024-12-29 14:00:00"
BOES_BIS = BIS_STUNDE
STUNDE_KOPF = ("2024-12-29 14:00:00", "2024-12-30 00:00:00")  # 10 Eimer
TAG_KOPF = ("2024-12-30", "2025-01-01")  # 2 Eimer
MONAT_MITTE = ("2025-01-01", "2025-12-01")  # 11 Eimer
TAG_FUSS = ("2025-12-01", "2025-12-30")  # 29 Eimer
STUNDE_FUSS = ("2025-12-30 00:00:00", "2025-12-30 03:00:00")  # 3 Eimer

# ─── Die Vorlagen aus gerendert.txt ──────────────────────────────────────────

EBENEN = {
    # Ebene: (Paar in gerendert.txt, Tabelle, Schluesselspalte, Literaltyp,
    #         die beiden gebundenen Werte, die M116 gerendert hat)
    "stunde": ("48H", "message_rollup", "stunde", "timestamp",
               "2025-12-28 05:00:00.0", "2025-12-30 05:00:00.0"),
    "tag": ("30T", "message_rollup_tag", "tag", "date",
            "2025-12-01", "2025-12-31"),
    "monat": ("12M", "message_rollup_monat", "monat", "date",
              "2025-01-01", "2026-01-01"),
}


def lies_vorlagen():
    """Liest die NEXANS-Kennzahlenstatements der drei Paare aus gerendert.txt."""
    vorlagen = {}
    for zeile in GERENDERT.read_text(encoding="utf-8-sig").splitlines():
        if not zeile.startswith("###KENNZAHLEN|NEXANS|"):
            continue
        _, _, paar, sql = zeile[3:].split("|", 3)
        vorlagen[paar] = sql.strip()
    assert set(vorlagen) == {"48H", "30T", "12M"}, sorted(vorlagen)
    return vorlagen


VORLAGEN = lies_vorlagen()


def ersetze_genau_einmal(text, alt, neu):
    anzahl = text.count(alt)
    assert anzahl == 1, f"'{alt[:60]}' kommt {anzahl}x vor, erwartet 1x"
    return text.replace(alt, neu)


def spalte(ebene):
    _, tabelle, schluessel, _, _, _ = EBENEN[ebene]
    return f"`overlord_monitor`.`{tabelle}`.`{schluessel}`"


def literal(ebene, wert):
    typ = EBENEN[ebene][3]
    if typ == "timestamp" and not wert.endswith(".0"):
        wert = wert + ".0"  # dieselbe Schreibweise, die jOOQ rendert
    return f"{typ} '{wert}'"


def bereich(ebene, von, bis):
    s = spalte(ebene)
    return f"{s} >= {literal(ebene, von)} and {s} < {literal(ebene, bis)}"


def praedikat_original(ebene):
    _, _, _, _, von, bis = EBENEN[ebene]
    return bereich(ebene, von, bis)


def kennzahlen(ebene, mandant, bereiche, summe_alias=False):
    """Das gerenderte Kennzahlenstatement der Ebene, mit neuem Bereichspraedikat
    und neuem Mandanten. `bereiche` ist eine Liste von (von, bis); bei zweien
    entsteht ((a) or (b)) -- Kopf und Fuss in einem Statement."""
    paar, tabelle, _, _, _, _ = EBENEN[ebene]
    sql = VORLAGEN[paar]
    if len(bereiche) == 1:
        neu = bereich(ebene, *bereiche[0])
    else:
        neu = "(" + " or ".join(f"({bereich(ebene, v, b)})" for v, b in bereiche) + ")"
    sql = ersetze_genau_einmal(sql, praedikat_original(ebene), neu)
    sql = ersetze_genau_einmal(sql, "'NEXANS'", f"'{mandant}'")
    if summe_alias:
        summe = f"sum(`overlord_monitor`.`{tabelle}`.`anzahl`)"
        sql = ersetze_genau_einmal(sql, summe, summe + " as `summe`")
    return sql


def mandantenkette(mandant, prozessspalte):
    """Die EXISTS-Kette, Wort fuer Wort wie gerendert, nur mit anderer Spalte."""
    return ("exists (select 1 as `one` from `GlassfishDB`.`Process` as `baum_process`"
            " join `GlassfishDB`.`ProjectMandant` on"
            " `GlassfishDB`.`ProjectMandant`.`ProjectID` = `baum_process`.`ProjectID`"
            f" where (`baum_process`.`ProcessID` = {prozessspalte} and"
            f" `GlassfishDB`.`ProjectMandant`.`MandantID` = '{mandant}'))")


def lesung(ebene, bereiche):
    """Eine Bereichslesung ohne Gruppierung und ohne Mandantenkette -- der
    Baustein der Ableitung in Z-U."""
    _, tabelle, _, _, _, _ = EBENEN[ebene]
    t = f"`overlord_monitor`.`{tabelle}`"
    if len(bereiche) == 1:
        praed = bereich(ebene, *bereiche[0])
    else:
        praed = "(" + " or ".join(f"({bereich(ebene, v, b)})" for v, b in bereiche) + ")"
    return (f"select {t}.`process_id`, {t}.`message_status`, {t}.`anzahl`"
            f" from {t} where {praed}")


def zu(mandant, summe_alias=False):
    """Fassung Z-U: die drei Bereichslesungen als UNION ALL in EINER Ableitung,
    darauf EINE Gruppierung und die Mandantenkette."""
    ableitung = " union all ".join([
        lesung("stunde", [STUNDE_KOPF, STUNDE_FUSS]),
        lesung("tag", [TAG_KOPF, TAG_FUSS]),
        lesung("monat", [MONAT_MITTE]),
    ])
    summe = "sum(`t`.`anzahl`)" + (" as `summe`" if summe_alias else "")
    return (f"select `t`.`process_id`, `t`.`message_status`, {summe}"
            f" from ({ableitung}) as `t`"
            f" where {mandantenkette(mandant, '`t`.`process_id`')}"
            " group by `t`.`process_id`, `t`.`message_status`")


def zd(mandant, summe_alias=False):
    """Fassung Z-D: drei Statements, eines je Ebene. Stunde und Tag tragen je
    zwei Bereiche (Kopf und Fuss), der Monat einen. Summiert wird in Java --
    das ist hier NICHT enthalten."""
    return [
        ("stunde", kennzahlen("stunde", mandant, [STUNDE_KOPF, STUNDE_FUSS], summe_alias)),
        ("tag", kennzahlen("tag", mandant, [TAG_KOPF, TAG_FUSS], summe_alias)),
        ("monat", kennzahlen("monat", mandant, [MONAT_MITTE], summe_alias)),
    ]


def zd_summe_in_sql(mandant):
    """Die Summe der drei Z-D-Ergebnisse, in SQL nachgebildet -- fuer die
    Gleichheitsprobe. Das ist die Arithmetik, die Java taete, nicht Java."""
    teile = " union all ".join(sql for _, sql in zd(mandant, summe_alias=True))
    return (f"select `u`.`process_id`, `u`.`message_status`, sum(`u`.`summe`) as `summe`"
            f" from ({teile}) as `u` group by `u`.`process_id`, `u`.`message_status`")


def ungeteilt(mandant):
    """Fassung E im Boesfall: die Stundenebene ueber das ganze Fenster."""
    return kennzahlen("stunde", mandant, [(BOES_VON, BOES_BIS)], summe_alias=True)


# ─── Die Bausteine der Sitzungen ─────────────────────────────────────────────

AUSWERTUNG = (
    "SELECT '{fall}' AS fall,\n"
    "       COUNT(*)                          AS anzahl_laeufe,\n"
    "       MAX(CASE WHEN rn = 1 THEN ms END) AS aufwaermlauf,\n"
    "       MIN(CASE WHEN rn > 1 THEN ms END) AS beste_von_fuenf,\n"
    "       GROUP_CONCAT(CASE WHEN rn > 1 THEN ms END ORDER BY rn) AS alle_fuenf\n"
    "FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,\n"
    "             ROUND(SUM(DURATION) * 1000, 3) AS ms\n"
    "      FROM information_schema.PROFILING\n"
    "      WHERE QUERY_ID > @basis + 1\n"
    "      GROUP BY QUERY_ID) t;"
)


def block(fall, sql):
    """EXPLAIN, dann sechs Laeufe, dann die Auswertung -- die Form aus M116."""
    zeilen = [f"SELECT '=== {fall} · Plan (Regel L15) ===' AS marke;",
              "EXPLAIN " + sql + ";",
              f"SELECT '=== {fall} · sechs Laeufe ===' AS marke;",
              "SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;"]
    zeilen.extend([sql + ";"] * LAEUFE)
    zeilen.append(AUSWERTUNG.format(fall=fall))
    return "\n".join(zeilen) + "\n\n"


def block_dreier(fall, statements):
    """Drei Statements je Runde, sechs Runden. Ausgewertet je Ebene (beste von
    fuenf) und je Runde (Summe der drei, beste von fuenf). Die Java-Summierung
    und die zwei zusaetzlichen Umlaeufe sind NICHT enthalten."""
    zeilen = []
    for ebene, sql in statements:
        zeilen.append(f"SELECT '=== {fall} · Plan {ebene} (Regel L15) ===' AS marke;")
        zeilen.append("EXPLAIN " + sql + ";")
    zeilen.append(f"SELECT '=== {fall} · sechs Runden ===' AS marke;")
    zeilen.append("SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;")
    for _ in range(LAEUFE):
        for _, sql in statements:
            zeilen.append(sql + ";")
    je_lauf = (
        "SELECT ELT(((rn - 1) MOD 3) + 1, 'stunde', 'tag', 'monat') AS ebene,\n"
        "       FLOOR((rn - 1) / 3) + 1 AS runde, ms\n"
        "FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,\n"
        "             ROUND(SUM(DURATION) * 1000, 3) AS ms\n"
        "      FROM information_schema.PROFILING\n"
        "      WHERE QUERY_ID > @basis + 1\n"
        "      GROUP BY QUERY_ID) t"
    )
    zeilen.append(
        f"SELECT '{fall}' AS fall, ebene,\n"
        "       COUNT(*)                             AS anzahl_laeufe,\n"
        "       MAX(CASE WHEN runde = 1 THEN ms END) AS aufwaermlauf,\n"
        "       MIN(CASE WHEN runde > 1 THEN ms END) AS beste_von_fuenf,\n"
        "       GROUP_CONCAT(CASE WHEN runde > 1 THEN ms END ORDER BY runde) AS alle_fuenf\n"
        f"FROM ({je_lauf}) u\n"
        "GROUP BY ebene ORDER BY FIELD(ebene, 'stunde', 'tag', 'monat');")
    zeilen.append(
        f"SELECT '{fall}' AS fall, 'summe_je_runde' AS ebene,\n"
        "       COUNT(*)                             AS anzahl_runden,\n"
        "       MAX(CASE WHEN runde = 1 THEN ms END) AS aufwaermlauf,\n"
        "       MIN(CASE WHEN runde > 1 THEN ms END) AS beste_von_fuenf,\n"
        "       GROUP_CONCAT(CASE WHEN runde > 1 THEN ms END ORDER BY runde) AS alle_fuenf\n"
        f"FROM (SELECT runde, ROUND(SUM(ms), 3) AS ms FROM ({je_lauf}) u\n"
        "      GROUP BY runde) r;")
    return "\n".join(zeilen) + "\n\n"


def kopf(titel, zweck):
    return f"""-- Messrunde 10c-4a -- {titel}
-- Auftrag:  "Prozessansicht -- freies Zeitfenster, Messrunde (Schritt 10c-4a)",
--           Stand 07.09.2026
-- Ergebnis: docs/process-view.md Paragraf 30 ff.
--
-- ERZEUGT von scripts/messung-prozessansicht-frei/erzeuge.py. Der Statementtext
-- kommt aus scripts/messung-prozessansicht/gerendert.txt (M116, aus
-- ProzessbaumRepository gefallen) und ist NUR in Ebene und Bereichspraedikat
-- variiert.
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1) Benutzer monitor_read.
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes.
-- L9: SET max_statement_time = 60. Jedes Statement, das die Grenze reisst, ist
--     eine Abweichung vom Rahmen und wird als solche ausgewiesen.
--
-- Z1: Alle Fenster enden auf 2025-12-30 03:00:00 -- unterhalb des Dev-Ankers
--     2025-12-30 04:09:47, auf einer Stundengrenze, im dichten Bestand.
--
{zweck}
SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version,
       @@max_statement_time AS grenze_vorher;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

"""


def schreibe(name, inhalt):
    ziel = HIER / name
    ziel.write_text(inhalt, encoding="utf-8", newline="\n")
    print(f"geschrieben: {ziel.name}  ({ziel.stat().st_size} Byte)")


# ─── s0: Rahmen, Bestand, Vergleichsanker, Belegungsprobe ────────────────────

def belegung(bezeichnung, ebene, von, bis):
    _, tabelle, schluessel, _, _, _ = EBENEN[ebene]
    return (f"SELECT '{bezeichnung}' AS fenster, '{ebene}' AS ebene,"
            f" COUNT(*) AS zeilen, SUM(anzahl) AS nachrichten,"
            f" COUNT(DISTINCT {schluessel}) AS eimer,"
            f" COUNT(DISTINCT process_id) AS prozesse"
            f" FROM overlord_monitor.{tabelle}"
            f" WHERE {schluessel} >= '{von}' AND {schluessel} < '{bis}'")


def s0():
    teile = [kopf(
        "Sitzung 0: Rahmen, Bestand, Vergleichsanker, Belegungsprobe",
        "-- ZWECK. Drei Dinge, bevor gemessen wird:\n"
        "--   1. Der Bestand der drei Ebenen -- sind es die Zahlen aus rollup.md?\n"
        "--   2. Der Vergleichsanker: M116 NEXANS 48H (6,781 ms) und 12M (75,743 ms)\n"
        "--      wiederholt. Eine Abweichung ueber 25 % wird ausgewiesen, nicht\n"
        "--      weggerechnet -- dann ist diese Sitzung mit M116 nicht vergleichbar.\n"
        "--   3. Die Belegungsprobe: je Fenster und Ebene COUNT(*) und SUM(anzahl),\n"
        "--      mandantenfrei -- die Zeilenzahl, die der Bereichszugriff liest, und\n"
        "--      gegen die die Laufzeiten normiert werden.\n")]

    teile.append("SELECT '=== 01 Bestand der drei Ebenen ===' AS marke;\n"
                 "SELECT 'stunde' AS ebene, COUNT(*) AS zeilen, SUM(anzahl) AS nachrichten,"
                 " MIN(stunde) AS erster, MAX(stunde) AS letzter FROM overlord_monitor.message_rollup\n"
                 "UNION ALL SELECT 'tag', COUNT(*), SUM(anzahl), MIN(tag), MAX(tag)"
                 " FROM overlord_monitor.message_rollup_tag\n"
                 "UNION ALL SELECT 'monat', COUNT(*), SUM(anzahl), MIN(monat), MAX(monat)"
                 " FROM overlord_monitor.message_rollup_monat;\n\n")

    teile.append("SELECT '=== 02 Vergleichsanker: M116 wiederholt, Zeichen fuer Zeichen ===' AS marke;\n")
    teile.append(block("M116-REPRO-NEXANS-48H", VORLAGEN["48H"]))
    teile.append(block("M116-REPRO-NEXANS-12M", VORLAGEN["12M"]))

    teile.append("SELECT '=== 03 Belegungsprobe: die fuenf Spannen auf der Stundenebene (M147) ===' AS marke;\n")
    teile.append("\nUNION ALL\n".join(
        belegung(code, "stunde", von, BIS_STUNDE) for code, von, _ in SPANNEN) + ";\n\n")

    teile.append("SELECT '=== 04 Belegungsprobe: die fuenf Spannen auf der Tagesebene (M148) ===' AS marke;\n")
    teile.append("\nUNION ALL\n".join(
        belegung(code, "tag", von, BIS_TAG) for code, _, von in SPANNEN) + ";\n\n")

    teile.append("SELECT '=== 05 Belegungsprobe: der Boesfall, fuenf Abschnitte und die ungeteilte Stundenlesung ===' AS marke;\n")
    teile.append("\nUNION ALL\n".join([
        belegung("1 stunde_kopf", "stunde", *STUNDE_KOPF),
        belegung("2 tag_kopf", "tag", *TAG_KOPF),
        belegung("3 monat_mitte", "monat", *MONAT_MITTE),
        belegung("4 tag_fuss", "tag", *TAG_FUSS),
        belegung("5 stunde_fuss", "stunde", *STUNDE_FUSS),
        belegung("E ungeteilt", "stunde", BOES_VON, BOES_BIS),
    ]) + ";\n\n")

    teile.append("SELECT '=== 06 Ergebniszeilen je Mandant: 365T Stundenebene und Boesfall Z-U ===' AS marke;\n")
    for m in MANDANTEN:
        s365 = kennzahlen("stunde", m, [(SPANNEN[4][1], BIS_STUNDE)], summe_alias=True)
        teile.append(f"SELECT '{m}' AS mandant, '365T stunde' AS fall, COUNT(*) AS ergebniszeilen,"
                     f" SUM(summe) AS nachrichten FROM ({s365}) x\n"
                     f"UNION ALL SELECT '{m}', 'boesfall Z-U', COUNT(*), SUM(summe)"
                     f" FROM ({zu(m, summe_alias=True)}) y;\n")
    teile.append("\nSELECT '=== 99 fertig ===' AS marke;\n")
    schreibe("s0-rahmen.sql", "".join(teile))


# ─── s1: M147, s2: M148 ──────────────────────────────────────────────────────

def s1():
    teile = [kopf(
        "Sitzung 1: M147, die Stundenebene ueber fuenf Spannen",
        "-- ZWECK. Fassung E im Boesfall: Ein stundengenaues Fenster faellt auf die\n"
        "-- Stundenebene, auch ueber zwoelf Monate. Vier Mandanten x fuenf Spannen,\n"
        "-- alle bis 2025-12-30 03:00:00. Das 365T-Fenster liest rund 280.000 Zeilen.\n"
        "-- Vorregistriert: NEXANS/365T reisst 700 ms (Erwartung 2); die zwei\n"
        "-- Vorhersagen dafuer lauten 713 ms (aus M116 hochgerechnet) und 1,5 bis\n"
        "-- 3,2 s (aus den Zeilenkosten M94) -- welche traegt, ist die Frage.\n")]
    for m in MANDANTEN:
        for code, von, _ in SPANNEN:
            teile.append(block(f"M147-{m}-{code}", kennzahlen("stunde", m, [(von, BIS_STUNDE)])))
    teile.append("SELECT '=== 99 fertig ===' AS marke;\n")
    schreibe("s1-m147-stundenebene.sql", "".join(teile))


def s2():
    teile = [kopf(
        "Sitzung 2: M148, die Tagesebene ueber dieselben Spannen",
        "-- ZWECK. Dieselben fuenf Spannen, tagesgenau, auf der Tagesebene. Das\n"
        "-- 365T-Fenster liest rund 100.000 Zeilen. Vorregistriert (Erwartung 3):\n"
        "-- rund 780 ms bei NEXANS -- trifft das zu, ist auch ein TAGESGENAUES\n"
        "-- Jahresfenster ohne Zerlegung nicht tragbar.\n")]
    for m in MANDANTEN:
        for code, _, von in SPANNEN:
            teile.append(block(f"M148-{m}-{code}", kennzahlen("tag", m, [(von, BIS_TAG)])))
    teile.append("SELECT '=== 99 fertig ===' AS marke;\n")
    schreibe("s2-m148-tagesebene.sql", "".join(teile))


# ─── s3: M149 (Z-U) und M150 (Z-D) ───────────────────────────────────────────

def s3():
    teile = [kopf(
        "Sitzung 3: M149 (Z-U) und M150 (Z-D), die Zerlegung des Boesfalls",
        "-- ZWECK. Der Boesfall 2024-12-29 14:00 -> 2025-12-30 03:00 in fuenf\n"
        "-- Abschnitten: 10 Stunden, 2 Tage, 11 Monate, 29 Tage, 3 Stunden = 55\n"
        "-- Zeitscheiben. Zwei Bauformen:\n"
        "--   M149 = Z-U: EIN Statement, die drei Bereichslesungen als UNION ALL in\n"
        "--          einer Ableitung, darauf eine Gruppierung und die Mandantenkette.\n"
        "--   M150 = Z-D: DREI Statements, eines je Ebene; Stunde und Tag tragen je\n"
        "--          zwei Bereiche (Kopf und Fuss). Ausgewiesen wird die Summe der drei\n"
        "--          SQL-Laufzeiten. Die Summierung in Java und die zwei zusaetzlichen\n"
        "--          Umlaeufe sind NICHT enthalten -- das ist die Luecke dieser Zahl.\n"
        "-- Vorregistriert: beide unter 100 ms (Erwartung 4), Z-U langsamer als Z-D\n"
        "-- (Erwartung 6, wegen der Ableitung aus M114 mit 98 bis 398 ms).\n")]
    for m in MANDANTEN:
        teile.append(block(f"M149-ZU-{m}", zu(m)))
        teile.append(block_dreier(f"M150-ZD-{m}", zd(m)))
    teile.append("SELECT '=== 99 fertig ===' AS marke;\n")
    schreibe("s3-m149-m150-zerlegung.sql", "".join(teile))


# ─── s4: M151, die Gleichheitsprobe ──────────────────────────────────────────

def antijoins(links_name, links, rechts_name, rechts):
    """Zwei Anti-Joins in einem UNION ALL -- MariaDB kennt kein FULL OUTER JOIN.
    Verglichen wird je (process_id, message_status) UND ueber die Summe: Eine
    Zeile, die auf der anderen Seite fehlt oder eine andere Summe traegt, faellt
    heraus. Als LEFT JOIN und nicht als NOT EXISTS, damit jede Ableitung genau
    einmal je Zweig ausgewertet wird."""
    return (f"SELECT '{links_name}' AS nur_auf_seite, COUNT(*) AS abweichende_zeilen"
            f" FROM ({links}) AS l LEFT JOIN ({rechts}) AS r"
            " ON r.process_id = l.process_id AND r.message_status = l.message_status"
            " AND r.summe = l.summe WHERE r.process_id IS NULL\n"
            f"UNION ALL SELECT '{rechts_name}', COUNT(*)"
            f" FROM ({rechts}) AS r LEFT JOIN ({links}) AS l"
            " ON l.process_id = r.process_id AND l.message_status = r.message_status"
            " AND l.summe = r.summe WHERE l.process_id IS NULL;")


def s4():
    teile = [kopf(
        "Sitzung 4: M151, die Gleichheitsprobe zerlegt gegen ungeteilt",
        "-- ZWECK. Ueber dasselbe Boesfall-Fenster: die ZERLEGTE Summe gegen die\n"
        "-- UNGETEILTE Lesung der Stundenebene, verglichen je (process_id,\n"
        "-- message_status) und ueber die Summe -- nicht nur als Gesamtsumme.\n"
        "-- Zweimal je Mandant: Z-U (ein Statement) und die Z-D-Summe, in SQL\n"
        "-- nachgebildet (die Arithmetik, die Java taete -- nicht Java selbst).\n"
        "-- Erwartet: NULL abweichende Zeilen (Erwartung 5). Findet die Probe auch\n"
        "-- nur eine, ist Ausgang 1 des Tors erreicht: sofort anhalten.\n"
        "-- Kein Ergebnis dieser Sitzung traegt eine Prozesskennung (G1): Es werden\n"
        "-- ausschliesslich Zaehler und Summen ausgegeben.\n")]
    for m in MANDANTEN:
        h = ungeteilt(m)
        teile.append(f"SELECT '=== M151-{m} · Seitenzaehler: Zeilen und Nachrichten je Fassung ===' AS marke;\n")
        teile.append(f"SELECT 'E ungeteilt (Stunde)' AS fassung, COUNT(*) AS zeilen, SUM(summe) AS nachrichten FROM ({h}) AS e\n"
                     f"UNION ALL SELECT 'Z-U zerlegt', COUNT(*), SUM(summe) FROM ({zu(m, summe_alias=True)}) AS u\n"
                     f"UNION ALL SELECT 'Z-D Summe (SQL)', COUNT(*), SUM(summe) FROM ({zd_summe_in_sql(m)}) AS d;\n")
        teile.append(f"SELECT '=== M151-{m} · Z-U gegen ungeteilt, je (process_id, message_status, summe) ===' AS marke;\n")
        teile.append(antijoins("nur in Z-U", zu(m, summe_alias=True), "nur in E ungeteilt", h) + "\n")
        teile.append(f"SELECT '=== M151-{m} · Z-D-Summe gegen ungeteilt, je (process_id, message_status, summe) ===' AS marke;\n")
        teile.append(antijoins("nur in Z-D-Summe", zd_summe_in_sql(m), "nur in E ungeteilt", h) + "\n\n")
    teile.append("SELECT '=== 99 fertig ===' AS marke;\n")
    schreibe("s4-m151-gleichheitsprobe.sql", "".join(teile))


if __name__ == "__main__":
    s0()
    s1()
    s2()
    s3()
    s4()
