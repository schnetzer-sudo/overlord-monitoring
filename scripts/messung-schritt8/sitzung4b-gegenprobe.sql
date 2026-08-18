-- Messrunde vor Schritt 8 — Sitzung 4b (Gegenprobe zu M54 (b))
-- Zweck: M54 (b) hat in Fenster A und B keine Zeile geliefert. Eine leere
-- Ausgabe ist kein Beleg fuer eine leere Ergebnismenge — sie kann auch von
-- einer unterdrueckten Meldung kommen. Diese Gegenprobe macht die Null
-- ausdruecklich sichtbar.
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- Gegenprobe 1 — Fenster A: Zahl der Zeilen, die M54 (b) faende
SELECT COUNT(*) AS treffer_fenster_a FROM (
  SELECT 1
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyValue REGEXP
        '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    AND mp.MessagePropertyName NOT LIKE '%.Payload.GUID'
    AND mp.MessagePropertyName NOT LIKE '%.Log.GUID'
) x;

-- Gegenprobe 2 — Positivkontrolle: derselbe Gestaltfilter OHNE den
-- Namensausschluss muss in Fenster A eine grosse Zahl liefern. Tut er das
-- nicht, ist der Regulaerausdruck defekt und Gegenprobe 1 wertlos.
SELECT COUNT(*) AS treffer_ohne_namensausschluss_fenster_a FROM (
  SELECT 1
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-12-29 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyValue REGEXP
        '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
) x;

-- Gegenprobe 3 — Fenster B
SELECT COUNT(*) AS treffer_fenster_b FROM (
  SELECT 1
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND mp.MessagePropertyValue REGEXP
        '^[^|]+\\|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    AND mp.MessagePropertyName NOT LIKE '%.Payload.GUID'
    AND mp.MessagePropertyName NOT LIKE '%.Log.GUID'
) x;

SHOW PROFILES;
