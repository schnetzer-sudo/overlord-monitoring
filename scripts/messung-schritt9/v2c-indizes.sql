-- V2c — Indexerhebung (Regel L8) fuer Message, Process, Project, ProjectMandant, SOS
SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, CARDINALITY, INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('Message','Process','Project','ProjectMandant','SOS')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
