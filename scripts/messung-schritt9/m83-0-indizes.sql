-- Messung M83-0 — Vorpruefung: welche Indizes liegen wirklich auf GlassfishDB.Message?
-- Auftrag:  "Messrunde M83 — die Bestandsabfrage vor E14", Fassung 1, 21.08.2026
-- Frage:    Fassung A (EXISTS je Prozess) steht und faellt damit, ob ProcessID an ERSTER
--           Position eines Index steht. M74bs Plan nennt "ProejctIDIDX" — ein Name, der
--           Project schreiben will und dabei GROUP BY ProcessID mit "Using index" bedient
--           hat. Gelesen wird deshalb der Inhalt der Instanz, nicht eine Dokumentation.
-- Ergebnis: docs/messungen-schritt9.md, Nachtrag vom 21.08.2026 — M83, Abschnitt M83-0
--
-- Ausschliesslich SELECT / SET / EXPLAIN / SHOW. (Regel S1)
-- G1: In dieser Datei steht kein Partnername, keine ProjectID, keine ProcessID.

-- 1. Alle Indizes auf Message, in Indexreihenfolge.
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, NULLABLE,
       INDEX_TYPE, CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 2. Die Frage der Vorpruefung, direkt beantwortet:
--    welche Indizes fuehren ProcessID an erster Position?
SELECT INDEX_NAME, NON_UNIQUE, CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
  AND COLUMN_NAME = 'ProcessID' AND SEQ_IN_INDEX = 1
ORDER BY INDEX_NAME;

-- 3. Zaehlwerte: wie viele Indizes traegt Message, und wie viele nennen ProcessID ueberhaupt?
SELECT COUNT(DISTINCT INDEX_NAME) AS indizes_gesamt,
       COUNT(DISTINCT CASE WHEN COLUMN_NAME = 'ProcessID' THEN INDEX_NAME END) AS nennen_processid,
       COUNT(DISTINCT CASE WHEN COLUMN_NAME = 'ProcessID' AND SEQ_IN_INDEX = 1
                           THEN INDEX_NAME END) AS processid_fuehrend
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message';

-- 4. Groesse der Tabelle — traegt die Einschaetzung zum Kaltlauf (M83-4).
SELECT TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH, DATA_LENGTH + INDEX_LENGTH AS gesamt,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 1) AS gesamt_mib
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message';

-- 5. Zum Vergleich die Indizes der beiden anderen beteiligten Tabellen.
SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME IN ('Process', 'ProjectMandant')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 6. Die Spaltentypen der Join-Spalten — ungleiche Typen verhindern den ref-Zugriff.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND ((TABLE_NAME = 'Message'        AND COLUMN_NAME IN ('ProcessID','MessageID'))
    OR (TABLE_NAME = 'Process'        AND COLUMN_NAME IN ('ProcessID','ProjectID'))
    OR (TABLE_NAME = 'ProjectMandant' AND COLUMN_NAME IN ('ProjectID','MandantID')))
ORDER BY TABLE_NAME, COLUMN_NAME;
