-- Messrunde M94-M98 — Sitzung 3: M94 Verteilung, drei Paare, zwei Mandanten (L7)
-- Auftrag:  "Messrunde vor Schritt 10b", Stand 27.08.2026
-- Ergebnis: docs/messungen-schritt10b.md
--
-- Ausschliesslich SELECT / SET / EXPLAIN. (Regel S1)
-- G1: Der Partnername verlaesst die innere Abfrage nicht. Nach aussen dringen
--     nur Anzahlen — dieselbe Bauform wie M89 Variante B.
--
-- Z1: Zeitpunkte als Literal, hergeleitet aus dem Anker 2025-12-30 04:09:47 (V5).
--     Die Verteilung gruppiert ueber das GANZE Fenster; die Eimerbreite des
--     Paares spielt fuer sie keine Rolle, die Fensterbreite schon.
--
-- Befund 11 der Vorrunde, hier vorgebeugt: KEIN Alias heisst wie eine Spalte
-- von message_rollup (stunde, process_id, message_status, anzahl) oder von
-- process_catalog (partner, richtung, ...). Sonst bindet GROUP BY/ORDER BY
-- still an die Tabellenspalte. Deshalb `partner_wert` und `nachrichten`.
--
-- `nicht zugeordnet` ist E-i: keine Katalogzeile ODER pflegestatus <> GEPFLEGT
-- ODER gepflegt mit leerem Partner — alles drei zusammen, als NULL.

SELECT '=== 00 read_only (erste Abfrage der Sitzung) ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;

SET max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;


-- ══ M94 Verteilung · 48 h · NEXANS ═══════════════════════════════
SELECT '=== M94-T P1-NEXANS Plan ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P1-NEXANS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS gesamt_nachrichten
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00';

SELECT '=== M94-T P1-NEXANS Ergebnis ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P1-NEXANS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

-- ══ M94 Verteilung · 30 Tage · NEXANS ═══════════════════════════════
SELECT '=== M94-T P2-NEXANS Plan ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P2-NEXANS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS gesamt_nachrichten
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00';

SELECT '=== M94-T P2-NEXANS Ergebnis ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P2-NEXANS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

-- ══ M94 Verteilung · 12 Monate · NEXANS ═══════════════════════════════
SELECT '=== M94-T P3-NEXANS Plan ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P3-NEXANS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS gesamt_nachrichten
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00';

SELECT '=== M94-T P3-NEXANS Ergebnis ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P3-NEXANS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

-- ══ M94 Verteilung · 48 h · SUTTONS ═══════════════════════════════
SELECT '=== M94-T P1-SUTTONS Plan ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P1-SUTTONS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS gesamt_nachrichten
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00';

SELECT '=== M94-T P1-SUTTONS Ergebnis ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P1-SUTTONS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

-- ══ M94 Verteilung · 30 Tage · SUTTONS ═══════════════════════════════
SELECT '=== M94-T P2-SUTTONS Plan ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P2-SUTTONS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS gesamt_nachrichten
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-12-01 00:00:00'
  AND r.stunde <  '2025-12-31 00:00:00';

SELECT '=== M94-T P2-SUTTONS Ergebnis ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P2-SUTTONS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-12-01 00:00:00'
    AND r.stunde <  '2025-12-31 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

-- ══ M94 Verteilung · 12 Monate · SUTTONS ═══════════════════════════════
SELECT '=== M94-T P3-SUTTONS Plan ===' AS marke;
EXPLAIN SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P3-SUTTONS gelesene Rollupzeilen, gezaehlt ===' AS marke;
SELECT COUNT(*) AS rollupzeilen, SUM(r.anzahl) AS gesamt_nachrichten
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'SUTTONS'
  AND r.stunde >= '2025-01-01 00:00:00'
  AND r.stunde <  '2026-01-01 00:00:00';

SELECT '=== M94-T P3-SUTTONS Ergebnis ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== M94-T P3-SUTTONS Laufzeit: Aufwaermlauf + fuenf ===' AS marke;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'SUTTONS'
    AND r.stunde >= '2025-01-01 00:00:00'
    AND r.stunde <  '2026-01-01 00:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
            WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
            WHEN c.partner IS NULL OR c.partner = '' THEN NULL
            ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;

SELECT '=== 99 Laufzeiten ===' AS marke;
SHOW PROFILES;

SELECT '=== 99 read_only am Ende der Sitzung ===' AS marke;
SELECT @@global.read_only AS read_only, NOW() AS serverzeit;
