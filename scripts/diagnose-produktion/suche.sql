-- ===========================================================================================
-- Diagnose: Freitextsuche der Nachrichtenliste in Produktion          Zweig diag/suche-produktion
-- ===========================================================================================
--
-- Anlass: In Produktion endet die Freitextsuche der Nachrichtenliste bei jedem Suchbegriff nach
-- rund 10 s mit `400 suche-abgebrochen`, auch im Vorgabefenster von 24 Stunden. Auf der Testkopie
-- ist der Fehler nicht nachstellbar. Gefragt ist, welchen Plan der Optimierer fuer das
-- Hauptstatement der Liste waehlt -- mit und ohne Suchbegriff -- und ob die Wahl an der
-- Indexstatistik von `Message` haengt.
--
-- WAS DIESES SKRIPT TUT, UND WAS NICHT (Regel S1)
--   * Es liest Systemvariablen und information_schema, loest den Suchbegriff gegen die Stammdaten
--     auf (Process, Project, SOS, ProjectMandant) -- so wie die Anwendung es tut -- und gibt zum
--     Hauptstatement der Liste nur den PLAN aus. EXPLAIN fuehrt das Statement nicht aus.
--   * Kein Statement auf `Message` wird ausgefuehrt. Kein ANALYZE, kein Schreibzugriff.
--   * Ausgegeben werden weder Kennungen noch Namen noch der Suchbegriff (Regel G1): nur Zahlen,
--     Index- und Tabellennamen und das tabellarische EXPLAIN. Kein EXPLAIN FORMAT=JSON, denn es
--     gaebe die eingesetzten Literale aus.
--
-- DAS STATEMENT IST DAS DER ANWENDUNG, ZEICHEN FUER ZEICHEN
--   Der Text stammt aus jOOQ 3.21.5 (`NachrichtenRepository.finde`, siehe gerendert.txt), die
--   Werte sind so eingesetzt, wie Connector/J 3.5.8 sie bei einem vorbereiteten Statement auf die
--   Leitung legt: Zeitpunkte als '2025-12-29 04:09:47', Kennungen als '...', Seitengroesse 51.
--   Die Kennungen aus der Vorfilterung stehen als LITERALE im Statement, zusammengesetzt ueber
--   GROUP_CONCAT(QUOTE(...)) und PREPARE -- nicht als Sitzungsvariablen: Darauf macht MariaDB
--   keine Bereichsanalyse, und der Plan waere ein anderer (docs/nachrichtenliste.md §5a).
--   Abweichungen, alle ohne Wirkung auf Plan oder Treffermenge:
--     - Vorfilterung mit ESCAPE '!' statt '\\': maskiert werden '!', '%' und '_' -- dieselbe
--       Treffermenge wie DSL.escape(begriff, '\\') im Code, aber unabhaengig davon, wie der
--       Client Rueckstriche behandelt.
--     - Die Reihenfolge der Kennungen in den IN-Listen ist die von GROUP_CONCAT, nicht die des
--       Treibers. MariaDB sortiert IN-Listen ohnehin selbst.
--     - Die Anwendung schickt Zeitpunkte mit Sekundenbruchteilen ('...:22.123456'), das Skript
--       ganze Sekunden. Die Fenstergrenzen weichen um weniger als eine Sekunde ab.
--
-- AUSFUEHRUNG IN PRODUKTION (MySQL Workbench)
--   1. Diese Datei nicht im Repository bearbeiten und nicht speichern. Den Inhalt in einen neuen
--      SQL-Reiter kopieren, oder eine Kopie unter scripts/diagnose-produktion/ergebnis/ anlegen
--      (der Ordner ist von Git ignoriert). Der Suchbegriff ist ein Kundenname (Regel G1).
--   2. Im Parameterblock @begriff_1 und @begriff_2 eintragen -- PFLICHT, je mindestens 3 Zeichen.
--      Fehlt einer, bricht das Skript gleich nach dem Parameterblock mit Fehler 1231 ab
--      ("... can't be set to the value of 'BITTE_SUCHBEGRIFFE_EINTRAGEN'").
--   3. Nichts markieren, dann Query > Execute (All or Selection) to Text. Alles laeuft in einer
--      Sitzung. Bei einem Fehler haelt Workbench an; das ist die Vorgabe, daran nichts aendern.
--   4. Im Reiter "Query Output" alles markieren und kopieren. Vollstaendig ist der Text, wenn er
--      mit der Endmarke "ENDE: alle Ausgaben vollstaendig" schliesst (Ausgabe 16).
--   5. Zurueck NUR diesen Text: keine Bildschirmfotos, nicht den Reiter "Action Output", nicht die
--      History, nicht die Skriptkopie -- dort steht der Suchbegriff. Vor dem Senden im Text nach
--      dem Begriff suchen; er darf nicht vorkommen. Fehlt die Endmarke, aus dem Action Output nur
--      Fehlernummer und Meldung melden, ohne den Teil "near '...'".
--   Nicht ueber das Ergebnisraster (Blitz-Symbol): Dessen Zellen kuerzen nach 256 Zeichen.
--   Kommandozeile, falls doch (cmd.exe, nicht PowerShell):
--     mysql --default-character-set=utf8mb4 -t -h HOST -u BENUTZER -p < kopie.sql > ausgabe.txt 2> fehler.txt
--     Zurueck nur ausgabe.txt; aus fehler.txt nur Fehlernummer und Zeile (dort steht Benutzer@Host).
--
-- AUSGABE -- sechzehn Ergebnisse, immer in dieser Reihenfolge. Ein Fall ohne Statement an
-- `Message` liefert an Stelle seines EXPLAIN eine Zeile mit dem Grund.
--    1        Server und Optimierer, je Zeile eine Variable; dazu zwei Zaehler seit Serverstart:
--             Max_statement_time_exceeded (Abbrueche an einer Sitzungsgrenze) und Com_kill.
--             Laeuft das Skript vor und nach EINER abgebrochenen Suche in der Anwendung, sagt der
--             Unterschied, ob die Sitzungsgrenze (Fehler 1969) oder ein KILL abgebrochen hat.
--    2        Indexstatistik von Message, Process und ProjectMandant
--    3        Vorfilterung je Fall (nur Trefferzahlen)
--    4,  5    Fall a  Mandant 1 mit Suchbegriff                    Marke, EXPLAIN
--    6,  7    Fall b  Mandant 1 ohne Suchbegriff (Gegenstueck)     Marke, EXPLAIN
--    8,  9    Fall c  Mandant 2 mit Suchbegriff                    Marke, EXPLAIN
--   10, 11    Fall d  Mandant 2 ohne Suchbegriff (Gegenstueck)     Marke, EXPLAIN
--   12, 13    Fall e  wie a, mit IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)
--                     auf Message (Fassung F4 aus docs/nachrichtenliste.md §5a)
--   14, 15    Fall f  Mandant 1 ohne Suchbegriff, Fenster der ersten Verengungsstufe
--                     (Zusatz, nicht im Auftrag -- siehe unten)
--   16        Endmarke
--
-- Zu b und d: Ohne Suchbegriff verengt die Anwendung das Fenster vorab ueber message_rollup
-- (docs/nachrichtenliste.md §5d) und schickt dann meist ein deutlich kuerzeres Fenster. b und d
-- zeigen bewusst das UNVERENGTE Fenster: dasselbe Statement wie a und c, nur ohne Suchzweig.
-- Mit Suchbegriff verengt die Anwendung nie (Abfragemerkmal SUCHBEGRIFF): a und c sind genau das
-- Statement, das sie schickt. e ist a mit dem Hinweis aus F4, den die Anwendung nie setzt.
--
-- Zu f: Das Fenster beginnt an der vollen Stunde vor Fensterende minus 1 h -- die erste Stufe
-- der Verengung (Fensterverengung.STUFEN, Verengungsgrenzen.volleStunde). Enger verengt die
-- Anwendung nie: Die erste Stufe liefert genau diese Stunde oder keine. Traegt sie nicht (weniger
-- als 51 Zeilen in der Stunde, oder der Rollup reicht noch nicht so weit), verengt die Anwendung
-- auf eine fruehere volle Stunde oder gar nicht (= b). Das entscheidet der Rollup, den dieses
-- Skript nicht fragt. f steht fuer die Liste ohne Suchbegriff, wie sie bei einem dichten
-- Mandanten laeuft.
-- ===========================================================================================

SET SESSION max_statement_time = 5;
SET SESSION group_concat_max_len = 1048576;

-- -------------------------------------------------------------------------------------------
-- Parameter
-- -------------------------------------------------------------------------------------------
-- Fensterende. Produktion: NOW() -- die Wanduhr des Datenbankservers. Die Anwendung nimmt
-- ihre eigene Uhr; laufen beide in derselben Zeitzone, ist es dieselbe Zeit (Ausgabe 1 nennt
-- @@time_zone und @@system_time_zone).
-- Testkopie: der Anker der Anwendungsuhr im Profil dev, TIMESTAMP '2025-12-30 04:09:47'.
SET @fensterende = NOW();
-- Fensterlaenge: die Vorgabe der Liste, 24 Stunden. Mit Suchbegriff nimmt die Anwendung
-- hoechstens 720 h (30 Tage) an, mit "Trotzdem suchen" hoechstens 2160 h; darueber antwortet
-- sie mit 400 suche-fenster-zu-gross und schickt kein Statement.
SET @fensterlaenge_stunden = 24;

-- Mandant 1 und sein Suchbegriff (PFLICHT) -- fuer den Anlass NEXANS und der Begriff, mit dem
-- die Suche in Produktion abgebrochen ist, so wie er im Suchfeld stand.
SET @mandant_1 = 'NEXANS';
SET @begriff_1 = '';

-- Mandant 2 und sein Suchbegriff (PFLICHT): ein Teil (mindestens 3 Zeichen) eines Prozess-,
-- Projekt- oder Ablaufnamens, den die Anwendung fuer diesen Mandanten anzeigt. Mandant 2 ist der
-- Gegenpol zu NEXANS: wenig Bestand, auf der Testkopie die prozessgetriebene Planfamilie (§5a).
-- Auf der Testkopie traf 'OUT' 3 Prozesse und 3 Ablaeufe; in Produktion kann es anders sein.
-- Zeigt Ausgabe 3 fuer Fall c "Vorfilterung leer" oder "mehr als 200 Treffer", einen anderen
-- Begriff nehmen und das ganze Skript neu ausfuehren.
SET @mandant_2 = 'SUTTONS';
SET @begriff_2 = '';

-- Wache: ohne beide Begriffe kein Lauf. Der ungueltige sql_mode bricht mit Fehler 1231 ab;
-- sind beide gesetzt, bleibt sql_mode, wie er ist.
SET SESSION sql_mode = IF(
  CHAR_LENGTH(REGEXP_REPLACE(COALESCE(@begriff_1, ''), '^[[:space:][:cntrl:]]+|[[:space:][:cntrl:]]+$', '')) < 3
  OR CHAR_LENGTH(REGEXP_REPLACE(COALESCE(@begriff_2, ''), '^[[:space:][:cntrl:]]+|[[:space:][:cntrl:]]+$', '')) < 3,
  'BITTE_SUCHBEGRIFFE_EINTRAGEN', @@session.sql_mode);

-- -------------------------------------------------------------------------------------------
-- Ausgabe 1: Server und Optimierer
-- -------------------------------------------------------------------------------------------
SET @bis_text = DATE_FORMAT(@fensterende, '%Y-%m-%d %H:%i:%s');
SET @von_text = DATE_FORMAT(@fensterende - INTERVAL @fensterlaenge_stunden HOUR, '%Y-%m-%d %H:%i:%s');
SET @von_f_text = DATE_FORMAT(@fensterende - INTERVAL 1 HOUR, '%Y-%m-%d %H:00:00');

SELECT 'VERSION()' AS variable, CAST(VERSION() AS CHAR) AS wert
UNION ALL SELECT '@@global.max_statement_time', CAST(@@global.max_statement_time AS CHAR)
UNION ALL SELECT '@@session.max_statement_time', CAST(@@session.max_statement_time AS CHAR)
UNION ALL SELECT '@@optimizer_switch', CAST(@@optimizer_switch AS CHAR)
UNION ALL SELECT '@@innodb_stats_persistent', CAST(@@innodb_stats_persistent AS CHAR)
UNION ALL SELECT '@@innodb_stats_auto_recalc', CAST(@@innodb_stats_auto_recalc AS CHAR)
UNION ALL SELECT '@@innodb_stats_persistent_sample_pages', CAST(@@innodb_stats_persistent_sample_pages AS CHAR)
UNION ALL SELECT '@@use_stat_tables', CAST(@@use_stat_tables AS CHAR)
UNION ALL SELECT '@@optimizer_use_condition_selectivity', CAST(@@optimizer_use_condition_selectivity AS CHAR)
UNION ALL SELECT '@@optimizer_search_depth', CAST(@@optimizer_search_depth AS CHAR)
UNION ALL SELECT '@@eq_range_index_dive_limit', CAST(@@eq_range_index_dive_limit AS CHAR)
UNION ALL SELECT '@@global.read_only', CAST(@@global.read_only AS CHAR)
UNION ALL SELECT '@@innodb_stats_on_metadata', CAST(@@innodb_stats_on_metadata AS CHAR)
UNION ALL SELECT '@@session.sql_mode', CAST(@@session.sql_mode AS CHAR)
UNION ALL SELECT '@@time_zone', CAST(@@time_zone AS CHAR)
UNION ALL SELECT '@@system_time_zone', CAST(@@system_time_zone AS CHAR)
UNION ALL SELECT 'NOW() - UTC_TIMESTAMP()', CAST(TIMEDIFF(NOW(), UTC_TIMESTAMP()) AS CHAR)
UNION ALL SELECT 'Max_statement_time_exceeded (seit Serverstart)',
  (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'MAX_STATEMENT_TIME_EXCEEDED')
UNION ALL SELECT 'Com_kill (seit Serverstart)',
  (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'COM_KILL')
UNION ALL SELECT 'Uptime (s)',
  (SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME = 'UPTIME')
UNION ALL SELECT 'NOW()', CAST(NOW() AS CHAR)
UNION ALL SELECT 'Fenster von', @von_text
UNION ALL SELECT 'Fenster bis', @bis_text
UNION ALL SELECT 'Fenster von (Fall f)', @von_f_text;

-- -------------------------------------------------------------------------------------------
-- Ausgabe 2: Indexstatistik
-- ca_zeilen_je_wert = TABLE_ROWS / CARDINALITY -- die Zeilenzahl, die der Optimierer fuer einen
-- Nachschlag ueber diesen Index erwartet. Bei ProejctIDIDX und Message_ProcessFK ist das die
-- Schaetzung je Prozess, an der die prozessgetriebene Planfamilie haengt.
-- -------------------------------------------------------------------------------------------
SELECT s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX, s.COLUMN_NAME, s.NON_UNIQUE, s.CARDINALITY,
       t.TABLE_ROWS, ROUND(t.TABLE_ROWS / NULLIF(s.CARDINALITY, 0)) AS ca_zeilen_je_wert
  FROM information_schema.STATISTICS s
  JOIN information_schema.TABLES t
    ON t.TABLE_SCHEMA = s.TABLE_SCHEMA AND t.TABLE_NAME = s.TABLE_NAME
 WHERE s.TABLE_SCHEMA = 'GlassfishDB'
   AND s.TABLE_NAME IN ('Message', 'Process', 'ProjectMandant')
 ORDER BY s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX;

-- -------------------------------------------------------------------------------------------
-- Vorlagen, abgeschrieben aus gerendert.txt. Statt '?' stehen Marken:
--   #MANDANT#  #MUSTER#  #VON#  #BIS#  #HINWEIS#  #SUCHE#
-- Jede Zeile ist ein Stueck desselben einzeiligen Textes; CONCAT setzt sie ohne Zeilenumbruch
-- zusammen.
-- -------------------------------------------------------------------------------------------
-- [vorlage-anfang]
SET @vorfilter_prozesse = CONCAT(
  'select distinct `GlassfishDB`.`Process`.`ProcessID` from `GlassfishDB`.`Process`',
  ' join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID`',
  ' left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID`',
  ' where (`GlassfishDB`.`ProjectMandant`.`MandantID` = #MANDANT#',
  ' and (`GlassfishDB`.`Process`.`ProcessName` like #MUSTER# escape ''!''',
  ' or `GlassfishDB`.`Project`.`ProjectName` like #MUSTER# escape ''!''))',
  ' fetch next 201 rows only');

SET @vorfilter_ablaeufe = CONCAT(
  'select distinct `GlassfishDB`.`SOS`.`SOSID` from `GlassfishDB`.`SOS`',
  ' join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`SOS`.`ProcessID`',
  ' join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID`',
  ' where (`GlassfishDB`.`ProjectMandant`.`MandantID` = #MANDANT#',
  ' and `GlassfishDB`.`SOS`.`SOSName` like #MUSTER# escape ''!'')',
  ' fetch next 201 rows only');

SET @liste = CONCAT(
  'select `GlassfishDB`.`Message`.`MessageID`, `GlassfishDB`.`Message`.`MessageLastUpdate`,',
  ' `GlassfishDB`.`Message`.`MessageStatus`, `GlassfishDB`.`Message`.`ProcessID`,',
  ' `GlassfishDB`.`Process`.`ProcessName`, `GlassfishDB`.`Project`.`ProjectName`,',
  ' `GlassfishDB`.`SOS`.`SOSName`, `GlassfishDB`.`SOSAction`.`SOSActionName`',
  ' from `GlassfishDB`.`Message`#HINWEIS#',
  ' left outer join `GlassfishDB`.`Process` on `GlassfishDB`.`Process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID`',
  ' left outer join `GlassfishDB`.`Project` on `GlassfishDB`.`Project`.`ProjectID` = `GlassfishDB`.`Process`.`ProjectID`',
  ' left outer join `GlassfishDB`.`SOS` on `GlassfishDB`.`SOS`.`SOSID` = `GlassfishDB`.`Message`.`SOSID`',
  ' left outer join `GlassfishDB`.`SOSAction` on (`GlassfishDB`.`SOSAction`.`SOSID` = `GlassfishDB`.`Message`.`SOSID`',
  ' and `GlassfishDB`.`SOSAction`.`SOSActionID` = `GlassfishDB`.`Message`.`SOSActionID`)',
  ' where (`GlassfishDB`.`Message`.`MessageLastUpdate` >= #VON#',
  ' and `GlassfishDB`.`Message`.`MessageLastUpdate` <= #BIS#',
  ' and exists (select 1 as `one` from `GlassfishDB`.`Process` as `mandanten_process`',
  ' join `GlassfishDB`.`ProjectMandant` on `GlassfishDB`.`ProjectMandant`.`ProjectID` = `mandanten_process`.`ProjectID`',
  ' where (`mandanten_process`.`ProcessID` = `GlassfishDB`.`Message`.`ProcessID`',
  ' and `GlassfishDB`.`ProjectMandant`.`MandantID` = #MANDANT#))#SUCHE#)',
  ' order by `GlassfishDB`.`Message`.`MessageLastUpdate` desc, `GlassfishDB`.`Message`.`MessageID` desc',
  ' fetch next 51 rows only');

-- Fassung F4: der Hinweis auf Message, sonst nichts.
SET @hinweis_f4 = ' IGNORE INDEX FOR JOIN (`ProejctIDIDX`, `Message_ProcessFK`)';
-- [vorlage-ende]

-- -------------------------------------------------------------------------------------------
-- Vorfilterung, wie NachrichtenService.suchtreffer und NachrichtenRepository.loeseSucheAuf:
-- Begriff am Rand von Leer- und Steuerzeichen befreit (wie Java trim()), mindestens 3 Zeichen,
-- Mandantenfilter, %/_ maskiert, je Art hoechstens 200 Treffer -- mehr ist
-- `400 suchbegriff-zu-unscharf`, gelesen werden deshalb 201.
-- Ergebnis: Anzahl und Literalliste je Art. Die Liste wird nie ausgegeben. Die Zielvariablen
-- werden vorher geleert: Scheitert eine Vorfilterung, bleibt NULL stehen und nicht der Wert
-- eines frueheren Laufs in derselben Sitzung.
-- -------------------------------------------------------------------------------------------
SET @np_1 = NULL, @ip_1 = NULL, @ns_1 = NULL, @is_1 = NULL;
SET @np_2 = NULL, @ip_2 = NULL, @ns_2 = NULL, @is_2 = NULL;
SET @b_1 = REGEXP_REPLACE(COALESCE(@begriff_1, ''), '^[[:space:][:cntrl:]]+|[[:space:][:cntrl:]]+$', '');
SET @muster_1 = CONCAT('%', REPLACE(REPLACE(REPLACE(@b_1, '!', '!!'), '%', '!%'), '_', '!_'), '%');
SET @s = IF(CHAR_LENGTH(@b_1) < 3, 'SELECT 0, NULL INTO @np_1, @ip_1',
  CONCAT('SELECT COUNT(*), GROUP_CONCAT(QUOTE(t.`ProcessID`) SEPARATOR '', '') INTO @np_1, @ip_1 FROM (',
         REPLACE(REPLACE(@vorfilter_prozesse, '#MANDANT#', QUOTE(@mandant_1)), '#MUSTER#', QUOTE(@muster_1)),
         ') AS t'));
PREPARE vorfilter FROM @s;
EXECUTE vorfilter;
SET @s = IF(CHAR_LENGTH(@b_1) < 3, 'SELECT 0, NULL INTO @ns_1, @is_1',
  CONCAT('SELECT COUNT(*), GROUP_CONCAT(QUOTE(t.`SOSID`) SEPARATOR '', '') INTO @ns_1, @is_1 FROM (',
         REPLACE(REPLACE(@vorfilter_ablaeufe, '#MANDANT#', QUOTE(@mandant_1)), '#MUSTER#', QUOTE(@muster_1)),
         ') AS t'));
PREPARE vorfilter FROM @s;
EXECUTE vorfilter;

SET @b_2 = REGEXP_REPLACE(COALESCE(@begriff_2, ''), '^[[:space:][:cntrl:]]+|[[:space:][:cntrl:]]+$', '');
SET @muster_2 = CONCAT('%', REPLACE(REPLACE(REPLACE(@b_2, '!', '!!'), '%', '!%'), '_', '!_'), '%');
SET @s = IF(CHAR_LENGTH(@b_2) < 3, 'SELECT 0, NULL INTO @np_2, @ip_2',
  CONCAT('SELECT COUNT(*), GROUP_CONCAT(QUOTE(t.`ProcessID`) SEPARATOR '', '') INTO @np_2, @ip_2 FROM (',
         REPLACE(REPLACE(@vorfilter_prozesse, '#MANDANT#', QUOTE(@mandant_2)), '#MUSTER#', QUOTE(@muster_2)),
         ') AS t'));
PREPARE vorfilter FROM @s;
EXECUTE vorfilter;
SET @s = IF(CHAR_LENGTH(@b_2) < 3, 'SELECT 0, NULL INTO @ns_2, @is_2',
  CONCAT('SELECT COUNT(*), GROUP_CONCAT(QUOTE(t.`SOSID`) SEPARATOR '', '') INTO @ns_2, @is_2 FROM (',
         REPLACE(REPLACE(@vorfilter_ablaeufe, '#MANDANT#', QUOTE(@mandant_2)), '#MUSTER#', QUOTE(@muster_2)),
         ') AS t'));
PREPARE vorfilter FROM @s;
EXECUTE vorfilter;
DEALLOCATE PREPARE vorfilter;

-- Ob die Anwendung ueberhaupt ein Statement an Message schickt -- NULL heisst ja.
SET @grund_1 = CASE
  WHEN CHAR_LENGTH(@b_1) = 0 THEN 'kein Plan: Suchbegriff nicht eingetragen (die Anwendung liefe ohne Suche)'
  WHEN CHAR_LENGTH(@b_1) < 3 THEN 'kein Statement an Message: Suchbegriff kuerzer als 3 Zeichen (400 suchbegriff-zu-kurz)'
  WHEN @np_1 IS NULL OR @ns_1 IS NULL THEN 'kein Plan: Vorfilterung nicht gelaufen (Fehler, siehe Action Output)'
  WHEN @np_1 > 200 OR @ns_1 > 200 THEN 'kein Statement an Message: mehr als 200 Treffer (400 suchbegriff-zu-unscharf)'
  WHEN @np_1 + @ns_1 = 0 THEN 'kein Statement an Message: Vorfilterung leer, die Anwendung antwortet mit leerer Liste'
  END;
SET @grund_2 = CASE
  WHEN CHAR_LENGTH(@b_2) = 0 THEN 'kein Plan: Suchbegriff nicht eingetragen (die Anwendung liefe ohne Suche)'
  WHEN CHAR_LENGTH(@b_2) < 3 THEN 'kein Statement an Message: Suchbegriff kuerzer als 3 Zeichen (400 suchbegriff-zu-kurz)'
  WHEN @np_2 IS NULL OR @ns_2 IS NULL THEN 'kein Plan: Vorfilterung nicht gelaufen (Fehler, siehe Action Output)'
  WHEN @np_2 > 200 OR @ns_2 > 200 THEN 'kein Statement an Message: mehr als 200 Treffer (400 suchbegriff-zu-unscharf)'
  WHEN @np_2 + @ns_2 = 0 THEN 'kein Statement an Message: Vorfilterung leer, die Anwendung antwortet mit leerer Liste'
  END;

-- -------------------------------------------------------------------------------------------
-- Die sechs Statements. #SUCHE# wird zuletzt ersetzt, damit keine Kennung je durch ein
-- spaeteres REPLACE laeuft.
-- -------------------------------------------------------------------------------------------
-- [faelle-anfang]
-- Der Suchzweig, wie jOOQ ihn rendert: `ProcessID in (...) or SOSID in (...)`, eine leere Art
-- als `false`.
SET @suche_1 = CONCAT(' and (',
  IF(@np_1 = 0, 'false', CONCAT('`GlassfishDB`.`Message`.`ProcessID` in (', @ip_1, ')')), ' or ',
  IF(@ns_1 = 0, 'false', CONCAT('`GlassfishDB`.`Message`.`SOSID` in (', @is_1, ')')), ')');
SET @suche_2 = CONCAT(' and (',
  IF(@np_2 = 0, 'false', CONCAT('`GlassfishDB`.`Message`.`ProcessID` in (', @ip_2, ')')), ' or ',
  IF(@ns_2 = 0, 'false', CONCAT('`GlassfishDB`.`Message`.`SOSID` in (', @is_2, ')')), ')');

SET @q_a = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@liste,
  '#HINWEIS#', ''), '#VON#', QUOTE(@von_text)), '#BIS#', QUOTE(@bis_text)),
  '#MANDANT#', QUOTE(@mandant_1)), '#SUCHE#', @suche_1);
SET @q_b = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@liste,
  '#HINWEIS#', ''), '#VON#', QUOTE(@von_text)), '#BIS#', QUOTE(@bis_text)),
  '#MANDANT#', QUOTE(@mandant_1)), '#SUCHE#', '');
SET @q_c = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@liste,
  '#HINWEIS#', ''), '#VON#', QUOTE(@von_text)), '#BIS#', QUOTE(@bis_text)),
  '#MANDANT#', QUOTE(@mandant_2)), '#SUCHE#', @suche_2);
SET @q_d = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@liste,
  '#HINWEIS#', ''), '#VON#', QUOTE(@von_text)), '#BIS#', QUOTE(@bis_text)),
  '#MANDANT#', QUOTE(@mandant_2)), '#SUCHE#', '');
SET @q_e = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@liste,
  '#HINWEIS#', @hinweis_f4), '#VON#', QUOTE(@von_text)), '#BIS#', QUOTE(@bis_text)),
  '#MANDANT#', QUOTE(@mandant_1)), '#SUCHE#', @suche_1);
SET @q_f = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(@liste,
  '#HINWEIS#', ''), '#VON#', QUOTE(@von_f_text)), '#BIS#', QUOTE(@bis_text)),
  '#MANDANT#', QUOTE(@mandant_1)), '#SUCHE#', '');
-- [faelle-ende]

-- -------------------------------------------------------------------------------------------
-- Ausgabe 3: Vorfilterung je Fall. 201 heisst: mehr als 200.
-- -------------------------------------------------------------------------------------------
SELECT 'a' AS fall, @mandant_1 AS mandant, 'mit Suchbegriff' AS suche,
       CHAR_LENGTH(@b_1) AS begriff_zeichen, @np_1 AS prozesse, @ns_1 AS ablaeufe,
       COALESCE(@grund_1, 'Statement an Message, Plan in Ausgabe 5') AS folge
UNION ALL SELECT 'b', @mandant_1, 'ohne', NULL, NULL, NULL,
       'unverengtes Fenster ohne Suchzweig, Plan in Ausgabe 7'
UNION ALL SELECT 'c', @mandant_2, 'mit Suchbegriff', CHAR_LENGTH(@b_2), @np_2, @ns_2,
       COALESCE(@grund_2, 'Statement an Message, Plan in Ausgabe 9')
UNION ALL SELECT 'd', @mandant_2, 'ohne', NULL, NULL, NULL,
       'unverengtes Fenster ohne Suchzweig, Plan in Ausgabe 11'
UNION ALL SELECT 'e', @mandant_1, 'mit Suchbegriff, F4', CHAR_LENGTH(@b_1), @np_1, @ns_1,
       COALESCE(@grund_1, 'wie a mit IGNORE INDEX FOR JOIN, Plan in Ausgabe 13')
UNION ALL SELECT 'f', @mandant_1, 'ohne, Zusatz', NULL, NULL, NULL,
       'Fenster der ersten Verengungsstufe, Plan in Ausgabe 15';

-- -------------------------------------------------------------------------------------------
-- Ausgaben 4 bis 15: je Fall eine Marke und der Plan. Ein Fall ohne Statement an Message
-- liefert statt des EXPLAIN eine Zeile mit dem Grund.
-- -------------------------------------------------------------------------------------------
SELECT CONCAT('Fall a: ', @mandant_1, ', mit Suchbegriff, ', @fensterlaenge_stunden, ' h') AS fall;
SET @x = IF(@grund_1 IS NULL, CONCAT('EXPLAIN ', @q_a), CONCAT('SELECT ', QUOTE(@grund_1), ' AS kein_explain'));
PREPARE plan FROM @x;
EXECUTE plan;

SELECT CONCAT('Fall b: ', @mandant_1, ', ohne Suchbegriff, ', @fensterlaenge_stunden, ' h unverengt') AS fall;
SET @x = CONCAT('EXPLAIN ', @q_b);
PREPARE plan FROM @x;
EXECUTE plan;

SELECT CONCAT('Fall c: ', @mandant_2, ', mit Suchbegriff, ', @fensterlaenge_stunden, ' h') AS fall;
SET @x = IF(@grund_2 IS NULL, CONCAT('EXPLAIN ', @q_c), CONCAT('SELECT ', QUOTE(@grund_2), ' AS kein_explain'));
PREPARE plan FROM @x;
EXECUTE plan;

SELECT CONCAT('Fall d: ', @mandant_2, ', ohne Suchbegriff, ', @fensterlaenge_stunden, ' h unverengt') AS fall;
SET @x = CONCAT('EXPLAIN ', @q_d);
PREPARE plan FROM @x;
EXECUTE plan;

SELECT CONCAT('Fall e: ', @mandant_1, ', mit Suchbegriff, IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)') AS fall;
SET @x = IF(@grund_1 IS NULL, CONCAT('EXPLAIN ', @q_e), CONCAT('SELECT ', QUOTE(@grund_1), ' AS kein_explain'));
PREPARE plan FROM @x;
EXECUTE plan;

SELECT CONCAT('Fall f (Zusatz): ', @mandant_1, ', ohne Suchbegriff, ab ', @von_f_text, ' (erste Verengungsstufe)') AS fall;
SET @x = CONCAT('EXPLAIN ', @q_f);
PREPARE plan FROM @x;
EXECUTE plan;
DEALLOCATE PREPARE plan;

-- -------------------------------------------------------------------------------------------
-- Aufraeumen: Begriff, Muster und Kennungslisten aus der Sitzung nehmen (Regel G1), dann die
-- Endmarke -- Ausgabe 16. Fehlt sie im Text, ist der Lauf nicht vollstaendig.
-- -------------------------------------------------------------------------------------------
-- [aufraeumen-anfang]
SET @begriff_1 = NULL, @begriff_2 = NULL, @b_1 = NULL, @b_2 = NULL, @muster_1 = NULL, @muster_2 = NULL;
SET @ip_1 = NULL, @is_1 = NULL, @ip_2 = NULL, @is_2 = NULL, @suche_1 = NULL, @suche_2 = NULL;
SET @q_a = NULL, @q_c = NULL, @q_e = NULL, @s = NULL, @x = NULL;
-- [aufraeumen-ende]
SELECT IF(@np_1 IS NULL OR @ns_1 IS NULL OR @np_2 IS NULL OR @ns_2 IS NULL,
          'ENDE, ABER MIT FEHLERN: eine Vorfilterung ist nicht gelaufen, siehe Ausgabe 3',
          'ENDE: alle Ausgaben vollstaendig') AS ende;
