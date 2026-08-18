-- M66 - Bestandsgrenzen, zur Einordnung der leeren Zeitscheiben
--
-- Zwei der fuenf beauftragten Scheiben (2026-05 und die Woche vor 2026-07-08)
-- tragen null Nachrichten. Ohne die Grenzen des Bestands laesst sich "leer"
-- nicht von "die Kopie endet frueher" unterscheiden - und genau diese
-- Unterscheidung braucht die vorregistrierte Deutung von M66.
--
-- Regel L9: Dieses Statement traegt KEIN Zeitfenster. Es laeuft ausschliesslich
-- ueber Message, nicht ueber MessageProperty, und nutzt MIN/MAX auf einer
-- indizierten Spalte.
--
-- Ausschliesslich SELECT / SET. (Regel S1)

SELECT @@global.read_only AS global_read_only;
SET SESSION max_statement_time = 60;

SELECT MIN(m.MessageLastUpdate) AS aeltester,
       MAX(m.MessageLastUpdate) AS juengster
FROM Message m;
