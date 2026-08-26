-- Messrunde M86-M92 — Sitzung 7a: M92, die Scheibengrenzen ueber den ganzen Bestand
--
-- Der Auftrag verlangt zwei Zahlen, die keine Laufzeit sind:
--   1. Die Scheibengrenzen ueber den ganzen Bestand — MIN und MAX von MessageLastUpdate
--      je Kalendermonat, ueber Fenster G. "Die fuenf leeren Monate muessen darin als leere
--      Scheiben auftauchen — ein Rueckwaertslauf, der ueber sie stolpert oder sie
--      ueberspringt, waere falsch gebaut."
--   2. Die groesste Monatsscheibe. "Sie bestimmt die Obergrenze, nicht der Durchschnitt."
--
-- DESHALB DER FEST AUSGESCHRIEBENE KALENDER. Eine blosse Gruppierung ueber die vorhandenen
-- Zeilen KANN leere Monate nicht zeigen — sie haben keine Zeile, ueber die gruppiert wuerde.
-- Der Kalender steht als UNION ALL aus 22 Literalpaaren da und wird per LEFT JOIN gegen den
-- Bestand gehalten. Genau so muss auch der Rueckwaertslauf gebaut sein: Er iteriert ueber
-- einen Kalender, nicht ueber die vorhandenen Daten. (Z1: alle Grenzen als Literal.)
--
-- L9: voller Durchlauf ueber Message. Begruendung: Die Frage lautet, wie sich der Bestand
-- ueber alle 22 Monate verteilt — sie ist ohne vollen Durchlauf nicht zu beantworten, und
-- sie ist die Grundlage der Bauform des Rueckwaertslaufs. Kein Vorbild fuer Anwendungscode.
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

SELECT '=== M92-1 Scheibengrenzen je Kalendermonat, leere Monate eingeschlossen ===' AS marke;
SELECT k.monat,
       COALESCE(g.zeilen, 0)      AS zeilen,
       g.frueheste,
       g.spaeteste,
       COALESCE(g.prozesse, 0)    AS prozesse,
       COALESCE(g.rollupzeilen, 0) AS rollupzeilen,
       CASE WHEN g.zeilen IS NULL THEN 'LEER' ELSE '' END AS vermerk
FROM (
  SELECT '2024-10' AS monat, '2024-10-01 00:00:00' AS von, '2024-11-01 00:00:00' AS bis
  UNION ALL
  SELECT '2024-11' AS monat, '2024-11-01 00:00:00' AS von, '2024-12-01 00:00:00' AS bis
  UNION ALL
  SELECT '2024-12' AS monat, '2024-12-01 00:00:00' AS von, '2025-01-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-01' AS monat, '2025-01-01 00:00:00' AS von, '2025-02-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-02' AS monat, '2025-02-01 00:00:00' AS von, '2025-03-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-03' AS monat, '2025-03-01 00:00:00' AS von, '2025-04-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-04' AS monat, '2025-04-01 00:00:00' AS von, '2025-05-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-05' AS monat, '2025-05-01 00:00:00' AS von, '2025-06-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-06' AS monat, '2025-06-01 00:00:00' AS von, '2025-07-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-07' AS monat, '2025-07-01 00:00:00' AS von, '2025-08-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-08' AS monat, '2025-08-01 00:00:00' AS von, '2025-09-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-09' AS monat, '2025-09-01 00:00:00' AS von, '2025-10-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-10' AS monat, '2025-10-01 00:00:00' AS von, '2025-11-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-11' AS monat, '2025-11-01 00:00:00' AS von, '2025-12-01 00:00:00' AS bis
  UNION ALL
  SELECT '2025-12' AS monat, '2025-12-01 00:00:00' AS von, '2026-01-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-01' AS monat, '2026-01-01 00:00:00' AS von, '2026-02-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-02' AS monat, '2026-02-01 00:00:00' AS von, '2026-03-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-03' AS monat, '2026-03-01 00:00:00' AS von, '2026-04-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-04' AS monat, '2026-04-01 00:00:00' AS von, '2026-05-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-05' AS monat, '2026-05-01 00:00:00' AS von, '2026-06-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-06' AS monat, '2026-06-01 00:00:00' AS von, '2026-07-01 00:00:00' AS bis
  UNION ALL
  SELECT '2026-07' AS monat, '2026-07-01 00:00:00' AS von, '2026-08-01 00:00:00' AS bis
) k
LEFT JOIN (
  SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m')                       AS monat,
         COUNT(*)                                                      AS zeilen,
         MIN(MessageLastUpdate)                                        AS frueheste,
         MAX(MessageLastUpdate)                                        AS spaeteste,
         COUNT(DISTINCT ProcessID)                                     AS prozesse,
         COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H'),
                        ProcessID, MessageStatus)                      AS rollupzeilen
  FROM GlassfishDB.Message
  GROUP BY monat
) g ON g.monat = k.monat
ORDER BY k.monat;

SELECT '=== M92-2 Die groesste Monatsscheibe ===' AS marke;
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m') AS monat,
       COUNT(*) AS zeilen
FROM GlassfishDB.Message
GROUP BY monat
ORDER BY zeilen DESC
LIMIT 5;

SELECT '=== M92-3 Kontrolle: Summe der Scheiben gegen den Bestand ===' AS marke;
SELECT COUNT(*) AS zeilen_gesamt, COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m')) AS belegte_monate
FROM GlassfishDB.Message;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;
SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
