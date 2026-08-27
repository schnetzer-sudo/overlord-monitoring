-- Messung M99 (Nachtrag) - Die Vorabfrage zerlegt: woran haengen ihre Kosten?
-- Auftrag:  "Fensterverengung fuer die Nachrichtenliste", Stand 27.08.2026
-- Ergebnis: docs/messungen-liste-verengung.md
--
-- M99 zeigt: die Vorabfrage kostet bei NEXANS auf der 1-Stunden-Stufe 2,192 ms, bei SYSTEM
-- auf derselben Stufe 0,688 ms. Beide lesen denselben Bereich - achtundzwanzig Rollup-Zeilen.
-- Der Unterschied kann also nicht am Bereich liegen. Er korreliert mit der Zahl der Prozesse
-- des Mandanten (NEXANS 733, SYSTEM 4). Diese Sitzung trennt die beiden Anteile:
--
--   A  733 Literale, 1 Stunde   - die gemessene Fassung
--   B  EXISTS-Kette, 1 Stunde   - dieselbe Auskunft, ohne Literalliste
--   C  ohne Mandantenfilter     - der Boden: was der Bereich allein kostet
--   D  nur die Prozessliste aufloesen - was die Beschaffung der Liste kostet
--
-- Die Antwort entscheidet, ob ein Index auf message_rollup(process_id, stunde) den Aufschlag
-- bei NEXANS ueberhaupt beseitigen koennte: Steckt er in der Literalliste und nicht im Bereich,
-- kann kein Index ihn nehmen.
--
-- Ausschliesslich SELECT / SET / PREPARE / DEALLOCATE. (Regel S1)
-- G1: Die Prozesskennungen bleiben in @proz; weder Datei noch Ausgabe zeigen sie.
-- Z1: Anker 2025-12-30 04:09:47 als Literal.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, VERSION() AS version, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 300;
SET SESSION group_concat_max_len = 1048576;

SELECT GROUP_CONCAT(QUOTE(ProcessID)) INTO @proz
FROM (SELECT DISTINCT p.ProcessID
      FROM GlassfishDB.Process p
      JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID
      WHERE pm.MandantID = 'NEXANS') x;
SELECT 'NEXANS' AS mandant,
       LENGTH(@proz) - LENGTH(REPLACE(@proz, ',', '')) + 1 AS prozesse_anzahl,
       LENGTH(@proz) AS laenge_der_literalliste_in_zeichen;

SET @kopf = 'SELECT SUM(g.anzahl) AS zeilen, MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze';
SET @kern = ' FROM ( SELECT s.stunde, s.anzahl, SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END) OVER (ORDER BY s.stunde DESC) AS kum FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl, (r.stunde >= ''2025-11-30 05:00:00'' AND r.stunde <= ''2025-12-30 03:00:00'') AS voll FROM overlord_monitor.message_rollup r WHERE r.stunde >= ''2025-12-30 03:00:00'' AND r.stunde <= ''2025-12-30 04:00:00''';
SET @schwanz = ' GROUP BY r.stunde ) s ) g';

SET @a = CONCAT(@kopf, @kern, ' AND r.process_id IN (', @proz, ')', @schwanz);
SET @b = CONCAT(@kopf, @kern, ' AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID WHERE mp.ProcessID = r.process_id AND pm.MandantID = ''NEXANS'')', @schwanz);
SET @c = CONCAT(@kopf, @kern, @schwanz);
SET @d = 'SELECT GROUP_CONCAT(QUOTE(ProcessID)) FROM (SELECT DISTINCT p.ProcessID FROM GlassfishDB.Process p JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = p.ProjectID WHERE pm.MandantID = ''NEXANS'') x';

PREPARE qa FROM @a;
PREPARE qb FROM @b;
PREPARE qc FROM @c;
PREPARE qd FROM @d;

SELECT '=== Aufwaermlauf (wird verworfen) ===' AS marke;
EXECUTE qa; EXECUTE qb; EXECUTE qc; EXECUTE qd;

SELECT '=== Fuenf Durchgaenge, abwechselnd ===' AS marke;
SELECT MAX(QUERY_ID) INTO @basis FROM information_schema.PROFILING;
EXECUTE qa; EXECUTE qb; EXECUTE qc; EXECUTE qd;
EXECUTE qa; EXECUTE qb; EXECUTE qc; EXECUTE qd;
EXECUTE qa; EXECUTE qb; EXECUTE qc; EXECUTE qd;
EXECUTE qa; EXECUTE qb; EXECUTE qc; EXECUTE qd;
EXECUTE qa; EXECUTE qb; EXECUTE qc; EXECUTE qd;

SELECT CASE (rn - 1) % 4
         WHEN 0 THEN 'A  733 Literale, 1 Stunde'
         WHEN 1 THEN 'B  EXISTS-Kette, 1 Stunde'
         WHEN 2 THEN 'C  ohne Mandantenfilter, 1 Stunde'
         ELSE        'D  nur die Prozessliste aufloesen'
       END                       AS fassung,
       COUNT(*)                  AS laeufe,
       MIN(ms)                   AS beste_von_fuenf,
       MAX(ms)                   AS schlechteste,
       GROUP_CONCAT(ms ORDER BY rn) AS alle_fuenf
FROM (SELECT ROW_NUMBER() OVER (ORDER BY QUERY_ID) AS rn,
             ROUND(SUM(DURATION) * 1000, 3) AS ms
      FROM information_schema.PROFILING
      WHERE QUERY_ID > @basis + 1
      GROUP BY QUERY_ID) t
GROUP BY fassung
ORDER BY fassung;

DEALLOCATE PREPARE qa;
DEALLOCATE PREPARE qb;
DEALLOCATE PREPARE qc;
DEALLOCATE PREPARE qd;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_am_ende, NOW() AS serverzeit_ende;
