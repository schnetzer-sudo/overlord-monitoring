-- ═══════════════════════════════════════════════════════════════════════════════
-- Erzeuger der Kandidatendatei zur Index-Sollliste (E39, 21.08.2026)
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- Dieses Statement erhebt den Indexbestand beider Schemata und gibt ihn in genau
-- dem Format aus, das backend/src/test/resources/indizes-sollliste.txt fuehrt.
--
-- ES SCHREIBT NICHT IN DIE SOLLLISTE. Seine Ausgabe geht nach
-- backend/src/test/resources/indizes-sollliste.kandidat; nachgezogen wird von
-- Hand ueber den Diff. Ein Erzeuger, der die Sollliste ueberschreibt, macht den
-- Test zum Gummistempel: rot, Befehl laufen lassen, gruen — und niemand hat
-- hingesehen. Der Test lebt davon, dass das Nachziehen ein bewusster Akt ist.
-- Vollstaendig begruendet im Kopf der Sollliste.
--
-- Aufruf: scripts\indizes-kandidat.ps1 (setzt die drei Variablen und schreibt
-- die Datei). Von Hand aufgerufen erwartet das Statement:
--
--   @glassfish  echter Name des Quellschemas    (OVERLORD_DB_GLASSFISH_SCHEMA)
--   @monitor    echter Name des eigenen Schemas (OVERLORD_DB_MONITOR_SCHEMA)
--   @bewacht    die im Quellschema bewachten Tabellen, komma-getrennt und ohne
--               Leerzeichen — genommen aus den tabelle;glassfish;-Zeilen der
--               Sollliste, nicht neu erfunden: WELCHE Quelltabelle bewacht wird,
--               entscheidet Regel L8 und nicht dieses Statement.
--
-- Fuer das eigene Schema gilt das Gegenteil: Dort werden ALLE Basistabellen
-- erhoben. Das Schema gehoert uns, und IndexbestandDbIT prueft die Tabellenliste
-- deshalb zusaetzlich auf Vollstaendigkeit.
--
-- Ausschliesslich SELECT auf information_schema (Regel S1).

SELECT zeile FROM (

  SELECT 1 AS block, TABLE_NAME AS tab, '' AS idx,
         CONCAT('tabelle;glassfish;', TABLE_NAME) AS zeile
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = @glassfish
     AND TABLE_TYPE   = 'BASE TABLE'
     AND FIND_IN_SET(TABLE_NAME, @bewacht)

  UNION ALL

  SELECT 2, TABLE_NAME, INDEX_NAME,
         CONCAT('index;glassfish;', TABLE_NAME, ';', INDEX_NAME, ';',
                GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','), ';',
                IF(MIN(NON_UNIQUE) = 0, 'ja', 'nein'))
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @glassfish
     AND FIND_IN_SET(TABLE_NAME, @bewacht)
   GROUP BY TABLE_NAME, INDEX_NAME

  UNION ALL

  SELECT 3, TABLE_NAME, '', CONCAT('tabelle;monitor;', TABLE_NAME)
    FROM information_schema.TABLES
   WHERE TABLE_SCHEMA = @monitor
     AND TABLE_TYPE   = 'BASE TABLE'

  UNION ALL

  SELECT 4, TABLE_NAME, INDEX_NAME,
         CONCAT('index;monitor;', TABLE_NAME, ';', INDEX_NAME, ';',
                GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ','), ';',
                IF(MIN(NON_UNIQUE) = 0, 'ja', 'nein'))
    FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @monitor
   GROUP BY TABLE_NAME, INDEX_NAME

) AS alle
ORDER BY block, tab, idx;
