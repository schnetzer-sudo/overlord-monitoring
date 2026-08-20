-- V2 — Spaltenerhebung nach Regel L8 fuer Process, Project, ProjectMandant, SOS
-- Messrunde vor Schritt 9b. Nur lesend gegen GlassfishDB.
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('Process','Project','ProjectMandant','SOS')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
