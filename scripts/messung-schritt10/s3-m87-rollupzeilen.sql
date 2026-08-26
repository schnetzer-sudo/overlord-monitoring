-- Messrunde M86-M92 — Sitzung 3: M87 "Wie viele Zeilen erzeugt der Rollup?"
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- Fenster G (Gesamtbestand, ohne Zeitfenster). L9: voller Durchlauf ueber Message,
--   ausdruecklich als BEZUGSGROESSE begruendet — die Zeilenzahl einer Tabelle, die es
--   noch nicht gibt, ist anders nicht zu bekommen. NIEMALS Vorbild fuer Anwendungscode.
-- Ausgegeben werden nur Anzahlen, keine ProcessID. (G1)
--
-- Das CASE der Variante 2 ist aus docs/message-status.md abgeschrieben, nicht erfunden:
--   ABGESCHLOSSEN    = FINISHED
--   QUITTIERT        = EERP_RECEIVED, COMMIT_RECEIVED
--   FEHLER           = MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR = 'COMMIT_REJECTED'
--
-- ABWEICHUNG, ausdruecklich: das Fluchtzeichen ist hier '!' statt '\'. Der
-- mysql-Client aus MySQL Workbench deutet '\_' als eigene Befehlsfolge und bricht
-- mit "Unknown command '\_'" ab, bevor das Statement den Server erreicht; ausserdem
-- ist ESCAPE '\' als Literal unvollstaendig ('\'' entwertet das schliessende
-- Hochkomma). 'ERROR!_%' ESCAPE '!' ist zeichengleich in der Wirkung: '!_' steht
-- fuer einen buchstaeblichen Unterstrich. M87-6 haelt das Ergebnis dagegen — dort
-- muss FEHLER genau ERROR_TIMEOUT, ERROR_DUPLICATE und COMMIT_REJECTED umfassen.
--   AUFGETEILT       = SPLITTED
--   ZUSAMMENGEFUEHRT = MERGED
--   WARTEND          = SUSPENDED
--   LAEUFT           = RUNNING
--   UNGEKLAERT       = CHECKED, CKECKED, COMMIT_SENT, alles Unbekannte

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling_history_size = 100;
SET profiling = 1;

-- ===== Bezugsgroessen ======================================================
SELECT '=== M87-0 Bezugsgroessen ===' AS marke;
SELECT COUNT(*)                        AS zeilen_message,
       COUNT(DISTINCT ProcessID)       AS verschiedene_prozesse,
       COUNT(DISTINCT MessageStatus)   AS verschiedene_rohstatus,
       SUM(ProcessID IS NULL)          AS ohne_prozess,
       SUM(MessageStatus IS NULL)      AS ohne_status,
       SUM(MessageLastUpdate IS NULL)  AS ohne_zeitstempel
FROM GlassfishDB.Message;

-- ===== EXPLAIN je Variante (L15) ==========================================
SELECT '=== M87-1 EXPLAIN Variante 1 (Stunde, ProcessID, Rohstatus) ===' AS marke;
EXPLAIN
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus) AS kombinationen
FROM GlassfishDB.Message;

SELECT '=== M87-2 EXPLAIN Variante 2 (Stunde, ProcessID, Einordnung) ===' AS marke;
EXPLAIN
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID,
       CASE
         WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
         WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
         WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
              OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
         WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
         WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
         WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
         WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
         ELSE 'UNGEKLAERT'
       END) AS kombinationen
FROM GlassfishDB.Message;

SELECT '=== M87-3 EXPLAIN Variante 3 (Tag, ProcessID, Rohstatus) ===' AS marke;
EXPLAIN
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d'), ProcessID, MessageStatus) AS kombinationen
FROM GlassfishDB.Message;

SELECT '=== M87-4 EXPLAIN Variante 4 (Monat, ProcessID, Rohstatus) ===' AS marke;
EXPLAIN
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m'), ProcessID, MessageStatus) AS kombinationen
FROM GlassfishDB.Message;

-- ===== Die vier Varianten, einzeln (je eigene Laufzeit) ====================
SELECT '=== M87-1 Variante 1: Stunde, ProcessID, MessageStatus ===' AS marke;
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus) AS v1_zeilen
FROM GlassfishDB.Message;

SELECT '=== M87-2 Variante 2: Stunde, ProcessID, Einordnung ===' AS marke;
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID,
       CASE
         WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
         WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
         WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
              OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
         WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
         WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
         WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
         WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
         ELSE 'UNGEKLAERT'
       END) AS v2_zeilen
FROM GlassfishDB.Message;

SELECT '=== M87-3 Variante 3: Tag, ProcessID, MessageStatus ===' AS marke;
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d'), ProcessID, MessageStatus) AS v3_zeilen
FROM GlassfishDB.Message;

SELECT '=== M87-4 Variante 4: Monat, ProcessID, MessageStatus ===' AS marke;
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m'), ProcessID, MessageStatus) AS v4_zeilen
FROM GlassfishDB.Message;

-- ===== Alle vier in einem Durchlauf (Gegenprobe der Zahlen) ===============
SELECT '=== M87-5 Gegenprobe: alle vier in einem Durchlauf ===' AS marke;
SELECT COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus) AS v1_zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID,
         CASE
           WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
           WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
           WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
                OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
           WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
           WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
           WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
           WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
           ELSE 'UNGEKLAERT'
         END)                                                                               AS v2_zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d'), ProcessID, MessageStatus)  AS v3_zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m'), ProcessID, MessageStatus)     AS v4_zeilen,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID)        AS stunde_prozess,
       COUNT(DISTINCT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00'))                   AS belegte_stunden
FROM GlassfishDB.Message;

-- ===== Zusatz: das CASE gegen den Bestand gehalten (faellt etwas in UNGEKLAERT?) =====
SELECT '=== M87-6 Einordnung je Rohstatus (Kontrolle des CASE) ===' AS marke;
SELECT MessageStatus AS rohstatus,
       CASE
         WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
         WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
         WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
              OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
         WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
         WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
         WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
         WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
         ELSE 'UNGEKLAERT'
       END AS einordnung,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
GROUP BY rohstatus, einordnung
ORDER BY anzahl DESC;

SELECT '=== 98 SHOW PROFILES ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 Abschluss ===' AS marke;
SELECT @@global.read_only AS read_only_ende, NOW() AS serverzeit_ende;
