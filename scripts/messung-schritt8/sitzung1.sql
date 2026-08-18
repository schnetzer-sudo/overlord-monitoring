-- Messrunde vor Schritt 8 — Sitzung 1
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: read_only-Nachweis, Rahmenangaben, M52
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SELECT VERSION() AS version, NOW() AS serverzeit;
SELECT @@div_precision_increment AS div_precision_increment,
       @@session.sql_mode        AS sql_mode,
       @@profiling_history_size  AS profiling_history_size_vorgabe;

SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

-- ─── M52 — Service: die Werte, nicht nur die Spalten ────────────────────────
-- ServiceName wird NICHT mitgezogen, ServiceConnectString nur als Laenge und
-- Ja/Nein-Spalten. (Regel G1)
SELECT ServiceID,
       CHAR_LENGTH(ServiceID)                                AS id_laenge,
       ServiceID REGEXP '^[A-Z0-9_]+$'                       AS id_code_form,
       ServiceID REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                                                             AS id_uuid_form,
       ServiceTypeID, ServiceStatus, ServiceTimeout,
       (ServiceDefaultFileStore IS NULL OR ServiceDefaultFileStore = '') AS ohne_default,
       CHAR_LENGTH(ServiceConnectString)                     AS cs_laenge,
       ServiceConnectString LIKE 'http://%'                  AS cs_http,
       ServiceConnectString LIKE 'https://%'                 AS cs_https,
       CHAR_LENGTH(ServiceConnectString)
         - CHAR_LENGTH(REPLACE(ServiceConnectString, '/', '')) AS cs_schraegstriche,
       ServiceConnectString LIKE '%?%'                       AS cs_mit_fragezeichen,
       ServiceLastUpdate
FROM Service
ORDER BY ServiceTypeID, ServiceID;

SHOW PROFILES;
