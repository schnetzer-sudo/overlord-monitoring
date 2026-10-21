-- Overlord Monitoring — 1.1.0: WERK_ABLADESTELLE ist fuer keinen Kundenmandanten
-- freigeschaltet (docs/tasks.md §3.9, E-1125, E-1126).
--
-- V100 hat den Typ fuer NEXANS und IBIS freigeschaltet. Mit 1.1.0 kommt die Linie
-- assistent nach main und in die Produktion; sichtbar ist der Assistent dort zunaechst
-- nur im Mandanten SYSTEM (Entscheidung des Auftraggebers vom 21.10.2026). V100 ist auf
-- der Testkopie angewandt und bleibt unveraendert. Diese Migration nimmt genau die beiden
-- Freischaltungen aus V100 zurueck, auf der Testkopie wie in der Produktion.
--
-- Die Freischaltung fuer SYSTEM kommt NICHT per Migration, sondern per Skript
-- (scripts/assistent-freischaltung-system.sql, Ruecknahme daneben): Freischaltungen sind
-- Pflegedaten wie die Typen selbst (E-1019), und das Ausrollen auf weitere Mandanten ist
-- ein INSERT in der Produktion, kein Deployment (E-1126).
--
-- Die erste Migration nach dem Zusammenfuehren der Linien. Neue Migrationen schliessen
-- an die hoechste vergebene Nummer an (E-1127); unter V103 folgt keine mehr.
--
-- Keine Tabelle und keine Spalte: Zeichensatz und Sortierung sind nicht beruehrt.
DELETE FROM task_type_mandant
WHERE task_typ = 'WERK_ABLADESTELLE'
  AND mandant_id IN ('NEXANS', 'IBIS');
