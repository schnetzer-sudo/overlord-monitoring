-- ═══════════════════════════════════════════════════════════════════════════════
-- Simulation der Plattform für die Sichtprüfungen zu „Ihre Aufträge“ (A4)
-- ═══════════════════════════════════════════════════════════════════════════════
--
--   NUR AUF DER TESTKOPIE. NIE IN DER PRODUKTION.
--
-- In der Produktion holt die echte Plattform ab und meldet zurück. Dieses Skript
-- spielt sie nach; dort liefe es gegen echte Aufträge von Kunden, und ein Kunde
-- sähe einen Stand, den keine Plattform gemeldet hat.
--
-- Beschrieben in docs/assistent-frontend.md §12.12 (Sichtprüfungen 1028 bis 1032).
--
-- ── Vorher ───────────────────────────────────────────────────────────────────
--
-- Über den Assistenten SECHS Aufträge unter NEXANS anlegen, jeder mit zwei oder
-- drei Einträgen und erfundenen Werten. Für Sichtprüfung 1032 später einen
-- SIEBTEN (siehe Teil 2). Das Skript nimmt ausschließlich offene Aufträge des
-- EIGENEN Kontos unter NEXANS — fremde Aufträge und andere Mandanten fasst es
-- nicht an.
--
-- ── Ausführen ────────────────────────────────────────────────────────────────
--
-- In MySQL Workbench, Verbindung mit dem Schreibbenutzer der Testkopie
-- (monitor_write), Vorgabeschema overlord_monitor. Je Teil die Zeilen markieren
-- und ausführen, im selben Reiter: Die Variablen oben gelten für die Sitzung.
-- Ohne die beiden SET-Zeilen unten ändert das Skript nichts — alle Kennungen
-- bleiben NULL, und jedes Statement trifft null Zeilen.
--
-- Die Statements der Plattform sind WÖRTLICH die aus docs/task-schnittstelle.md
-- §3 (hier als PREPARE mit ?). Die Simulation fährt damit den Vertrag und nicht
-- etwas Ähnliches. Die Rückgabewerte sind die von WERK_ABLADESTELLE.

SET @benutzer = NULL;      -- der eigene Benutzername im Monitor, etwa 'lukas'
SET @bestaetigung = NULL;  -- 'TESTKOPIE' eintragen, wenn die Verbindung zur Testkopie geht

-- ═══ Teil 1: fünf Stände herstellen, einer bleibt offen ═════════════════════════

-- Wie viele offene Aufträge des eigenen Kontos gibt es? Unter sechs: erst anlegen.
SELECT COUNT(*) AS offene_eigene
FROM overlord_monitor.task
WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
  AND @bestaetigung = 'TESTKOPIE';

-- Die sechs jüngsten, neueste zuerst. @t1 bleibt offen.
SET @t1 = (SELECT task_id FROM overlord_monitor.task
           WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
             AND @bestaetigung = 'TESTKOPIE'
           ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1 OFFSET 0);
SET @t2 = (SELECT task_id FROM overlord_monitor.task
           WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
             AND @bestaetigung = 'TESTKOPIE'
           ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1 OFFSET 1);
SET @t3 = (SELECT task_id FROM overlord_monitor.task
           WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
             AND @bestaetigung = 'TESTKOPIE'
           ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1 OFFSET 2);
SET @t4 = (SELECT task_id FROM overlord_monitor.task
           WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
             AND @bestaetigung = 'TESTKOPIE'
           ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1 OFFSET 3);
SET @t5 = (SELECT task_id FROM overlord_monitor.task
           WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
             AND @bestaetigung = 'TESTKOPIE'
           ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1 OFFSET 4);
SET @t6 = (SELECT task_id FROM overlord_monitor.task
           WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
             AND @bestaetigung = 'TESTKOPIE'
           ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1 OFFSET 5);

-- Die Zahl der Einträge je Auftrag; die Rückgabe hat eine Zeile je Eintrag.
SET @n3 = (SELECT JSON_LENGTH(eintraege) FROM overlord_monitor.task WHERE task_id = @t3);
SET @n4 = (SELECT JSON_LENGTH(eintraege) FROM overlord_monitor.task WHERE task_id = @t4);
SET @n6 = (SELECT JSON_LENGTH(eintraege) FROM overlord_monitor.task WHERE task_id = @t6);

-- Erledigt: jede Zeile Ok.
SET @r3 = SUBSTRING(REPEAT(CONCAT(CHAR(10), 'Ok'), @n3), 2);
-- Teilweise erledigt: die erste Zeile „Schlüssel nicht gefunden“, der Rest Ok.
SET @r4 = CONCAT('Schlüssel nicht gefunden', REPEAT(CONCAT(CHAR(10), 'Ok'), @n4 - 1));
-- Rückmeldung unvollständig: eine Zeile zu wenig (bei einem Eintrag: gar keine).
SET @r6 = SUBSTRING(REPEAT(CONCAT(CHAR(10), 'Ok'), @n6 - 1), 2);
-- Fehlgeschlagen: Code und Klartext, wie ihn die Plattform dem Kunden schreibt.
SET @code = 'SIMULATION';
SET @text = 'Die Tabelle konnte nicht gespeichert werden. Bitte wenden Sie sich an die Betreuung.';

-- Die Statements der Plattform (docs/task-schnittstelle.md §3), wörtlich.
PREPARE abholen FROM
  'UPDATE overlord_monitor.task
   SET status = ''IN_AUSFUEHRUNG'', abgeholt_am = UTC_TIMESTAMP(3)
   WHERE task_id = ? AND status = ''OFFEN''';
PREPARE erledigt_melden FROM
  'UPDATE overlord_monitor.task
   SET status = ''ERLEDIGT'', rueckgabe = ?, ausgefuehrt_am = UTC_TIMESTAMP(3)
   WHERE task_id = ? AND status = ''IN_AUSFUEHRUNG''';
PREPARE fehlgeschlagen_melden FROM
  'UPDATE overlord_monitor.task
   SET status = ''FEHLGESCHLAGEN'', fehlercode = ?, fehlertext = ?,
       ausgefuehrt_am = UTC_TIMESTAMP(3)
   WHERE task_id = ? AND status = ''IN_AUSFUEHRUNG''';

-- In Ausführung
EXECUTE abholen USING @t2;
-- Erledigt
EXECUTE abholen USING @t3;
EXECUTE erledigt_melden USING @r3, @t3;
-- Teilweise erledigt
EXECUTE abholen USING @t4;
EXECUTE erledigt_melden USING @r4, @t4;
-- Fehlgeschlagen
EXECUTE abholen USING @t5;
EXECUTE fehlgeschlagen_melden USING @code, @text, @t5;
-- Rückmeldung unvollständig
EXECUTE abholen USING @t6;
EXECUTE erledigt_melden USING @r6, @t6;

DEALLOCATE PREPARE abholen;
DEALLOCATE PREPARE erledigt_melden;
DEALLOCATE PREPARE fehlgeschlagen_melden;

-- Was jetzt dasteht. Danach in der Übersicht „Neu laden“.
SELECT task_id, status, JSON_LENGTH(eintraege) AS eintraege,
       REPLACE(rueckgabe, CHAR(10), ' | ') AS rueckgabe, fehlercode, eingestellt_am
FROM overlord_monitor.task
WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND @bestaetigung = 'TESTKOPIE'
ORDER BY eingestellt_am DESC, task_id DESC;

-- ═══ Teil 2: den jüngsten offenen Auftrag abholen (Sichtprüfung 1032) ═════════
--
-- Erst einen weiteren Auftrag über den Assistenten anlegen und in der Übersicht
-- aufklappen. OHNE die Übersicht neu zu laden diesen Teil ausführen, dann dort
-- „Auftrag löschen“ und „Löschen“.

SET @abholen = (SELECT task_id FROM overlord_monitor.task
                WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND status = 'OFFEN'
                  AND @bestaetigung = 'TESTKOPIE'
                ORDER BY eingestellt_am DESC, task_id DESC LIMIT 1);
PREPARE abholen FROM
  'UPDATE overlord_monitor.task
   SET status = ''IN_AUSFUEHRUNG'', abgeholt_am = UTC_TIMESTAMP(3)
   WHERE task_id = ? AND status = ''OFFEN''';
EXECUTE abholen USING @abholen;
DEALLOCATE PREPARE abholen;
SELECT task_id, status, abgeholt_am FROM overlord_monitor.task WHERE task_id = @abholen;

-- ═══ Teil 3: aufräumen ════════════════════════════════════════════════════════
--
-- Löscht ALLE Aufträge des eigenen Kontos unter NEXANS, gleich in welchem Stand.
-- Vorher mit dem SELECT ansehen.

SELECT task_id, status, eingestellt_am
FROM overlord_monitor.task
WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND @bestaetigung = 'TESTKOPIE'
ORDER BY eingestellt_am DESC;

DELETE FROM overlord_monitor.task
WHERE mandant_id = 'NEXANS' AND angelegt_von = @benutzer AND @bestaetigung = 'TESTKOPIE';
