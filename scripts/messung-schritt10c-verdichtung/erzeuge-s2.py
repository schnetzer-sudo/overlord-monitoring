# -*- coding: utf-8 -*-
"""Erzeugt s2-m146-<mandant>.sql — die alte gegen die verdichtete Fassung.

JE MANDANT EINE DATEI, und das ist kein Ordnungsgeschmack: MariaDB deckelt
`profiling_history_size` bei 100. Ein Lauf mit allen drei Mandanten hat 108
Messabfragen, und die ersten waeren aus SHOW PROFILES herausgefallen — ohne
dass eine Zahl falsch geworden waere. Gemerkt an der Zaehlprobe im Auswerter.

Bauform wie M108: ein Aufwaermlauf, dann die beste von fuenf. Die Reihenfolge
der Abfragen ist streng, damit SHOW PROFILES ohne Ratespiel zuzuordnen ist.

    python scripts/messung-schritt10c-verdichtung/erzeuge-s2.py
"""

import io
import os

HIER = os.path.dirname(os.path.abspath(__file__))

MANDANTEN = ["NEXANS", "SUTTONS", "VOTG"]
FENSTER = [
    ("48H", "2025-12-28 09:00:00", "2025-12-30 09:00:00"),
    ("30T", "2025-12-01 00:00:00", "2025-12-31 00:00:00"),
    ("12M", "2025-01-01 00:00:00", "2026-01-01 00:00:00"),
]
LAEUFE = 6  # ein Aufwaermlauf + fuenf

FEHLER = (
    "  WHERE (m.MessageStatus LIKE 'ERROR\\_%' ESCAPE '\\\\'\n"
    "         OR m.MessageStatus = 'COMMIT_REJECTED')"
)


def kette(mandant):
    """Die Mandantenkette als EXISTS. ProjectMandant ist n:m — ein JOIN
    vervielfachte die Zeilen und damit COUNT(*). `Project` steht nicht in der
    Kette (Befund 48)."""
    return (
        "  AND EXISTS (SELECT 1 FROM GlassfishDB.Process dp\n"
        "              JOIN GlassfishDB.ProjectMandant dpm ON dpm.ProjectID = dp.ProjectID\n"
        "              WHERE dp.ProcessID = m.ProcessID AND dpm.MandantID = '%s')" % mandant
    )


def alt(mandant, von, bis):
    """Der Stand vor dieser Runde: je Nachricht eine Zeile."""
    return (
        "SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, s.SOSName\n"
        "FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)\n"
        "LEFT JOIN GlassfishDB.SOS s ON s.SOSID = m.SOSID\n"
        + FEHLER
        + "\n  AND m.MessageLastUpdate >= '%s'\n  AND m.MessageLastUpdate <  '%s'\n" % (von, bis)
        + kette(mandant)
        + "\nORDER BY m.MessageLastUpdate DESC, m.MessageID DESC\nLIMIT 10"
    )


def neu(mandant, von, bis):
    """Die verdichtete Fassung: je Prozess eine Zeile, mit Anzahl und juengstem
    Zeitpunkt. `ProcessName` steht in der GROUP BY, obwohl es vom Schluessel
    funktional abhaengt — das aendert die Gruppen nicht und macht die Abfrage
    unabhaengig davon, wie streng `only_full_group_by` gerade steht."""
    return (
        "SELECT m.ProcessID, p.ProcessName, COUNT(*), MAX(m.MessageLastUpdate)\n"
        "FROM GlassfishDB.Message m IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)\n"
        "LEFT JOIN GlassfishDB.Process p ON p.ProcessID = m.ProcessID\n"
        + FEHLER
        + "\n  AND m.MessageLastUpdate >= '%s'\n  AND m.MessageLastUpdate <  '%s'\n" % (von, bis)
        + kette(mandant)
        + "\nGROUP BY m.ProcessID, p.ProcessName\n"
        "ORDER BY MAX(m.MessageLastUpdate) DESC, m.ProcessID DESC\nLIMIT 10"
    )


KOPF = """-- Messrunde 10c — M146: „Zuletzt aufgefallen" je Prozess statt je Nachricht
-- Mandant:  %s
-- Auftrag:  „Nachbesserung Uebersicht", 04.09.2026, Teil 3
-- Erzeugt von scripts/messung-schritt10c-verdichtung/erzeuge-s2.py
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- G1: keine Zugangsdaten, kein Hostname. Mandanten-IDs sind Codes und stehen in
--     PROJEKTBESCHREIBUNG.md Paragraf 3.2.
-- Z1: Anker 2025-12-30 04:09:47 (M9); die drei Fenster sind eimerausgerichtet
--     wie Rollupzeitraum.fenster().
--
-- BAUFORM WIE M108: ein Aufwaermlauf, dann die beste von fuenf. BEIDE Fassungen
-- tragen denselben Indexhinweis, damit der Vergleich die GRUPPIERUNG misst und
-- nicht den Hinweis.

SELECT '=== 00 read_only ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;
"""

reihenfolge = []
for mandant in MANDANTEN:
    aus = [KOPF % mandant]
    for name, von, bis in FENSTER:
        for fassung, bauer in (("alt", alt), ("neu", neu)):
            aus.append(
                "\nSELECT '--- %s %s %s (%d Laeufe) ---' AS marke;"
                % (mandant, name, fassung, LAEUFE)
            )
            for lauf in range(LAEUFE):
                aus.append(bauer(mandant, von, bis) + ";")
                reihenfolge.append((mandant, name, fassung, lauf))

    name, von, bis = FENSTER[-1]
    aus.append("\nSELECT '=== EXPLAIN verdichtet, %s %s ===' AS marke;" % (mandant, name))
    aus.append("EXPLAIN " + neu(mandant, von, bis) + ";")
    aus.append("\nSELECT '=== EXPLAIN alt, %s %s ===' AS marke;" % (mandant, name))
    aus.append("EXPLAIN " + alt(mandant, von, bis) + ";")
    aus.append("\nSELECT '=== Profil ===' AS marke;")
    aus.append("SHOW PROFILES;")

    ziel = os.path.join(HIER, "s2-m146-%s.sql" % mandant.lower())
    io.open(ziel, "w", encoding="utf-8", newline="").write("\n".join(aus) + "\n")
    print("erzeugt:", os.path.basename(ziel))

io.open(os.path.join(HIER, "reihenfolge.txt"), "w", encoding="utf-8", newline="").write(
    "\n".join("%s %s %s %d" % r for r in reihenfolge) + "\n"
)
print(len(reihenfolge), "Messabfragen, %d je Datei" % (len(reihenfolge) // len(MANDANTEN)))
