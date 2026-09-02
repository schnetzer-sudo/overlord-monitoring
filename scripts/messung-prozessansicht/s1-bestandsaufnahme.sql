-- Messung M109 bis M111: BESTANDSAUFNAHME VOR DEM BAU DER PROZESSANSICHT
-- Auftrag:  "Schritt 10c-1: Prozessansicht, Backend", Stand 01.09.2026, Teil 2 und Teil 4
-- Ergebnis: docs/process-view.md
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
--
-- WAS DIESE SITZUNG BEANTWORTET, UND WARUM SIE VOR DEM BAU LAEUFT:
--
--   M109  Welche Indizes traegt der Rollup HEUTE? Teil 2 des Auftrags sagt
--         ausdruecklich: "Stelle fest, was heute tatsaechlich da ist, statt eine
--         der beiden Stellen zu glauben." docs/rollup.md §2 sagt an einer Stelle
--         "kein Sekundaerindex" (durchgestrichen) und an anderer, V11 habe einen
--         angelegt. Gefragt ist information_schema, nicht das Dokument.
--
--   M110  Wie gross ist der Baum je Mandant? Partner-, Richtungs- und
--         Prozesszahlen -- die Zahl, die 10c-2 braucht. Ohne sie laesst sich die
--         Oberflaeche nicht entwerfen.
--
--   M111  Wie verteilen sich die drei Zustaende bewegt / still / nie?
--
-- G1: Es werden ausschliesslich ZAHLEN ausgegeben, keine Partnernamen und keine
--     Prozessnamen. Partnernamen sind nach docs/README.md geschuetzt.
-- Z1: Alle Fenstergrenzen stehen als Literal und sind aus dem Dev-Anker
--     2025-12-30 04:09:47 hergeleitet (common/ZeitConfig). Kein NOW() in einer
--     fachlichen Bedingung.
--
-- Laufzeiten aus information_schema.PROFILING.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit, VERSION() AS version;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 200;


-- ===========================================================================
-- M109 -- DER INDEXBESTAND DES ROLLUPS, GEMESSEN STATT GELESEN
-- ===========================================================================

SELECT '=== M109-1 Indizes auf den drei Rollup-Ebenen und dem Katalog ===' AS marke;
SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'overlord_monitor'
  AND TABLE_NAME IN ('message_rollup','message_rollup_tag','message_rollup_monat',
                     'process_catalog')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

SELECT '=== M109-2 Zeilen und Platz je Ebene ===' AS marke;
SELECT TABLE_NAME, TABLE_ROWS AS zeilen_geschaetzt,
       DATA_LENGTH AS daten_bytes, INDEX_LENGTH AS index_bytes
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'overlord_monitor'
  AND TABLE_NAME IN ('message_rollup','message_rollup_tag','message_rollup_monat',
                     'process_catalog');

SELECT '=== M109-3 Zeilen gezaehlt (information_schema schaetzt) ===' AS marke;
SELECT (SELECT COUNT(*) FROM overlord_monitor.message_rollup)       AS stundenebene,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_tag)   AS tagesebene,
       (SELECT COUNT(*) FROM overlord_monitor.message_rollup_monat) AS monatsebene,
       (SELECT COUNT(*) FROM overlord_monitor.process_catalog)      AS katalogzeilen;

SELECT '=== M109-4 Summenprobe: alle drei Ebenen muessen dieselbe Summe tragen ===' AS marke;
SELECT (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup)       AS aus_stunden,
       (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup_tag)   AS aus_tagen,
       (SELECT SUM(anzahl) FROM overlord_monitor.message_rollup_monat) AS aus_monaten;

SELECT '=== M109-5 Wie viele verschiedene Prozesse traegt die Stundenebene ueberhaupt ===' AS marke;
SELECT COUNT(DISTINCT process_id) AS prozesse_mit_rollupzeile,
       MIN(stunde) AS fruehester_eimer, MAX(stunde) AS juengster_eimer
FROM overlord_monitor.message_rollup;


-- ===========================================================================
-- M110 -- DIE BAUMGROESSE JE MANDANT
--
-- Die Gliederung ist Partner -> Richtung -> Prozess. Zugeordnet ist ein Wert
-- nur, wenn eine Katalogzeile existiert, sie GEPFLEGT traegt und das Feld
-- gefuellt ist (Entscheidung E-i aus docs/dashboard.md §4). Genau dieser
-- Ausdruck steht hier -- nicht eine bequemere Naeherung, sonst misst die
-- Erhebung etwas anderes als der Code spaeter liest.
-- ===========================================================================

SELECT '=== M110-1 Prozesse, Partnergruppen, Richtungsgruppen je Mandant ===' AS marke;
SELECT pm.MandantID                                            AS mandant,
       COUNT(*)                                                AS prozesse,
       COUNT(c.process_id)                                     AS mit_katalogzeile,
       SUM(c.pflegestatus = 'GEPFLEGT')                        AS gepflegt,
       COUNT(DISTINCT CASE WHEN c.pflegestatus = 'GEPFLEGT'
                            AND c.partner IS NOT NULL AND c.partner <> ''
                           THEN c.partner END)                 AS partner_gruppen,
       SUM(NOT (c.process_id IS NOT NULL
                AND c.pflegestatus = 'GEPFLEGT'
                AND c.partner IS NOT NULL AND c.partner <> '')) AS ohne_partner,
       SUM(c.process_id IS NOT NULL AND c.pflegestatus = 'GEPFLEGT'
           AND c.richtung = 'EINGEHEND')                        AS eingehend,
       SUM(c.process_id IS NOT NULL AND c.pflegestatus = 'GEPFLEGT'
           AND c.richtung = 'AUSGEHEND')                        AS ausgehend,
       SUM(NOT (c.process_id IS NOT NULL
                AND c.pflegestatus = 'GEPFLEGT'
                AND c.richtung IS NOT NULL AND c.richtung <> '')) AS ohne_richtung
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;

SELECT '=== M110-2 Wie viele Aeste hat der Baum je Mandant (Partner x Richtung) ===' AS marke;
SELECT mandant, COUNT(*) AS gruppen, SUM(prozesse) AS prozesse,
       MAX(prozesse) AS groesste_gruppe, ROUND(AVG(prozesse), 2) AS schnitt
FROM (
  SELECT pm.MandantID AS mandant,
         CASE WHEN c.process_id IS NOT NULL AND c.pflegestatus = 'GEPFLEGT'
                   AND c.partner IS NOT NULL AND c.partner <> ''
              THEN c.partner END AS partner_schluessel,
         CASE WHEN c.process_id IS NOT NULL AND c.pflegestatus = 'GEPFLEGT'
                   AND c.richtung IS NOT NULL AND c.richtung <> ''
              THEN c.richtung END AS richtung_schluessel,
         COUNT(*) AS prozesse
  FROM GlassfishDB.Process p
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
  GROUP BY pm.MandantID, partner_schluessel, richtung_schluessel) g
GROUP BY mandant
ORDER BY prozesse DESC;

SELECT '=== M110-3 Wie viele Richtungsgruppen haengen unter einem Partner ===' AS marke;
SELECT mandant, richtungen_je_partner, COUNT(*) AS partner
FROM (
  SELECT pm.MandantID AS mandant, c.partner,
         COUNT(DISTINCT CASE WHEN c.richtung IS NOT NULL AND c.richtung <> ''
                             THEN c.richtung END)
         + MAX(c.richtung IS NULL OR c.richtung = '') AS richtungen_je_partner
  FROM GlassfishDB.Process p
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
  JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
  WHERE c.pflegestatus = 'GEPFLEGT' AND c.partner IS NOT NULL AND c.partner <> ''
  GROUP BY pm.MandantID, c.partner) x
GROUP BY mandant, richtungen_je_partner
ORDER BY mandant, richtungen_je_partner;


-- ===========================================================================
-- M111 -- DIE ZUSTANDSVERTEILUNG bewegt / still / nie
--
-- Die Schwelle ist DREI MONATE und haengt NICHT am gewaehlten Zeitfenster.
-- Stichtag ist der Dev-Anker 2025-12-30 04:09:47, drei Monate davor ist
-- 2025-09-30 04:09:47.
--
--   nie     kein einziger Rollupeimer
--   still   juengster Eimer aelter als die Schwelle
--   bewegt  sonst
-- ===========================================================================

SELECT '=== M111-1 Zustandsverteilung je Mandant ===' AS marke;
SELECT pm.MandantID AS mandant,
       COUNT(*)                                                       AS prozesse,
       SUM(r.letzte_bewegung IS NULL)                                 AS nie,
       SUM(r.letzte_bewegung IS NOT NULL
           AND r.letzte_bewegung <  '2025-09-30 04:09:47')            AS still,
       SUM(r.letzte_bewegung >= '2025-09-30 04:09:47')                AS bewegt
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;

SELECT '=== M111-2 Gegenprobe: traegt_nachrichten aus dem Katalog (E14) ===' AS marke;
-- Der Katalog fuehrt fuer denselben Sachverhalt eine eigene, vom Bestandslauf
-- gefuellte Spalte. Sie sagt "haengt im Bestand mindestens eine Nachricht dran";
-- der Rollup sagt "es gibt mindestens einen Eimer". Weichen die beiden ab, ist
-- das ein Befund und keine Kleinigkeit -- deshalb steht die Probe hier.
SELECT pm.MandantID AS mandant,
       SUM(c.traegt_nachrichten IS NULL)                     AS katalog_nie_geprueft,
       SUM(c.traegt_nachrichten = 0)                         AS katalog_ohne,
       SUM(c.traegt_nachrichten = 1)                         AS katalog_mit,
       SUM(c.traegt_nachrichten = 1 AND r.letzte_bewegung IS NULL)  AS widerspruch_a,
       SUM(c.traegt_nachrichten = 0 AND r.letzte_bewegung IS NOT NULL) AS widerspruch_b
FROM GlassfishDB.Process p
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = p.ProcessID
LEFT JOIN (SELECT process_id, MAX(stunde) AS letzte_bewegung
           FROM overlord_monitor.message_rollup
           GROUP BY process_id) r ON r.process_id = p.ProcessID
GROUP BY pm.MandantID
ORDER BY mandant;


SELECT '=== 99 Laufzeiten dieser Sitzung (Orientierung, nicht der Messwert) ===' AS marke;
SELECT QUERY_ID, ROUND(SUM(DURATION) * 1000, 3) AS ms
FROM information_schema.PROFILING
GROUP BY QUERY_ID
ORDER BY QUERY_ID;
