-- M74b — Vorpruefung: Plan des einen vollen Durchlaufs ueber Message
EXPLAIN SELECT ProcessID, COUNT(*) AS nachrichten FROM Message GROUP BY ProcessID;
SELECT '--- format=json ---' AS marke;
EXPLAIN FORMAT=JSON SELECT ProcessID, COUNT(*) AS nachrichten FROM Message GROUP BY ProcessID;
