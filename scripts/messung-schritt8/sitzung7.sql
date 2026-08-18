-- Messrunde vor Schritt 8 — Sitzung 7
-- Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026
-- Inhalt: M57 Fenster B
-- Der Auftrag sieht Fenster B fuer M57 "bei Auffaelligkeit" vor. Fenster A hat
-- eine: 55,98 % der Artefakte tragen keinen lesbaren Schrittnamen. Deshalb
-- dieser Lauf.
-- Ausschliesslich SELECT / SET / SHOW. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SELECT NOW() AS serverzeit;
SET SESSION max_statement_time = 60;
SET profiling = 1;
SET profiling_history_size = 100;

SELECT mp.MessagePropertyName, mp.MessageActionID,
       COUNT(*) AS zeilen,
       SUM(ma.MessageID IS NULL) AS ohne_schrittzeile,
       COUNT(DISTINCT sa.SOSActionName) AS verschiedene_schrittnamen,
       SUM(sa.SOSID IS NULL) AS ohne_schrittnamen
FROM Message m
JOIN MessageProperty mp ON mp.MessageID = m.MessageID
LEFT JOIN MessageAction ma ON ma.MessageID = mp.MessageID
                          AND ma.MessageActionID = mp.MessageActionID
LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
    OR mp.MessagePropertyName LIKE '%.Log.GUID')
GROUP BY mp.MessagePropertyName, mp.MessageActionID
ORDER BY mp.MessagePropertyName, mp.MessageActionID;

-- Verdichtung: die eine Zahl, um die es geht
SELECT SUM(zeilen) AS zeilen_gesamt,
       SUM(ohne_schrittzeile) AS ohne_schrittzeile_gesamt,
       SUM(ohne_schrittnamen) AS ohne_schrittnamen_gesamt
FROM (
  SELECT COUNT(*) AS zeilen,
         SUM(ma.MessageID IS NULL) AS ohne_schrittzeile,
         SUM(sa.SOSID IS NULL) AS ohne_schrittnamen
  FROM Message m
  JOIN MessageProperty mp ON mp.MessageID = m.MessageID
  LEFT JOIN MessageAction ma ON ma.MessageID = mp.MessageID
                            AND ma.MessageActionID = mp.MessageActionID
  LEFT JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
  WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
    AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
    AND (mp.MessagePropertyName LIKE '%.Payload.GUID'
      OR mp.MessagePropertyName LIKE '%.Log.GUID')
  GROUP BY mp.MessagePropertyName, mp.MessageActionID
) x;

SHOW PROFILES;
