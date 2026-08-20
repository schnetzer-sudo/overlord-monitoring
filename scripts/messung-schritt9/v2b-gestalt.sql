-- V2b — Gestalt der Schluesselspalten: ist ProcessID lesbar oder eine GUID?
SELECT 'Process.ProcessID' AS spalte,
       SUM(ProcessID REGEXP '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-') AS guid_form,
       SUM(ProcessID REGEXP '_')                              AS mit_unterstrich,
       MIN(CHAR_LENGTH(ProcessID)) AS len_min, MAX(CHAR_LENGTH(ProcessID)) AS len_max,
       COUNT(*) AS zeilen
FROM Process
UNION ALL
SELECT 'Process.ProcessName',
       SUM(ProcessName REGEXP '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-'),
       SUM(ProcessName REGEXP '_'),
       MIN(CHAR_LENGTH(ProcessName)), MAX(CHAR_LENGTH(ProcessName)), COUNT(ProcessName)
FROM Process
UNION ALL
SELECT 'Project.ProjectID',
       SUM(ProjectID REGEXP '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-'),
       SUM(ProjectID REGEXP '_'),
       MIN(CHAR_LENGTH(ProjectID)), MAX(CHAR_LENGTH(ProjectID)), COUNT(*)
FROM Project
UNION ALL
SELECT 'Project.ProjectName',
       SUM(ProjectName REGEXP '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-'),
       SUM(ProjectName REGEXP '_'),
       MIN(CHAR_LENGTH(ProjectName)), MAX(CHAR_LENGTH(ProjectName)), COUNT(ProjectName)
FROM Project
UNION ALL
SELECT 'ProjectMandant.MandantID',
       SUM(MandantID REGEXP '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-'),
       SUM(MandantID REGEXP '_'),
       MIN(CHAR_LENGTH(MandantID)), MAX(CHAR_LENGTH(MandantID)), COUNT(*)
FROM ProjectMandant;
