-- Overlord Monitoring — Prozess-Katalog: kuratierter Partner und Richtung (Schritt 9b).
--
-- WOZU. Der Katalog macht aus etwas Lesbarem etwas Gruppierbares, und das ist
-- seine einzige Aufgabe. SOSName ist im Altsystem in Klartext gepflegt, aber
-- "Eingehender IFTMIN BAYER" ist ein Satz und kein Schluessel; ein GROUP BY
-- darauf ginge nur mit Parsen, und Parsen ist nach PROJEKTBESCHREIBUNG.md §4.4
-- ausgeschlossen. Abnehmer ist Schritt 10: die Verteilung nach Partner, die nach
-- Partner und Richtung, und die nach Partner gruppierte Prozessansicht.
--
-- WARUM AUCH TOTE PROZESSE EINE ZEILE BEKOMMEN. 765 von 1.503 Prozessen tragen
-- im Bestand der Testkopie keine einzige Nachricht (M74b). Sie werden trotzdem
-- gepflegt — ein Monitoring muss auch zeigen, was NICHT passiert ist. Ohne
-- zugeordneten Partner laesst sich "von X kam seit acht Wochen nichts" nicht
-- formulieren, weil es kein X gibt.
--
-- WARUM DIE ProcessID ALLEIN TRAEGT. Naheliegend waere ein Schluessel aus
-- Mandant und Prozess. M74a widerlegt das: projekte_mit_mehreren_mandanten = 0,
-- 1.490 Joinzeilen auf 1.490 verschiedene ProcessID. Eine Katalogzeile gehoert
-- immer genau einem Mandanten, und der Mandantenbezug faellt aus dem Join ueber
-- ProjectMandant — er braucht keine eigene Spalte (docs/prozess-katalog.md E3).
--
-- KEIN Fremdschluessel ueber die Schemagrenze (§6): process_id zeigt fachlich
-- auf GlassfishDB.Process.ProcessID. Verschwindet ein Prozess im Altsystem,
-- bleibt die Katalogzeile bestehen — sonst verschwaende rueckwirkend die
-- Partnerzuordnung aller historischen Nachrichten. Verwaiste Eintraege sind hier
-- ausdruecklich erwuenscht, dieselbe Regel wie bei bam_spalte und bam_sollaenge.
--
-- Zeichensatz und Sortierung stehen explizit (nie geerbt), sonst bricht der Join
-- gegen GlassfishDB, sobald der Server-Default sich aendert oder die Instanz auf
-- MariaDB 11 gehoben wird. Begruendung: PROJEKTBESCHREIBUNG.md §5. Genau daran
-- haengt diese Tabelle mehr als jede andere: Ihre Pflegeliste joint
-- process_catalog.process_id gegen GlassfishDB.Process.ProcessID.
--
-- KEINE _bin-Spalte: hier steht nichts Tokenartiges (§5). process_id ist ein
-- lesbarer Prozessname (8 bis 36 Zeichen, keine GUID, M75) und keine Kennung im
-- Sinne einer Sitzungs-ID. Dass utf8mb4_general_ci ohne Ruecksicht auf Gross-
-- und Kleinschreibung vergleicht, legt hier keine Falle: Die Quellspalte traegt
-- dieselbe Sortierung und koennte zwei nur im Fall verschiedene Kennungen selbst
-- nicht halten.
--
-- KEINE Vorbelegung, anders als bei V4 und V5. Die Zeilen entstehen nicht in der
-- Migration, sondern im Heuristik-Lauf je Mandant und per Hand (E13) — eine
-- Vorbelegung waere geraten und nicht kuratiert (Regel Q4).

CREATE TABLE process_catalog (
  -- VARCHAR(36) passend zu GlassfishDB.Process.ProcessID (gegen
  -- information_schema erhoben, nicht uebernommen: varchar(36) NOT NULL, PRI —
  -- messungen-schritt9.md V2, 20.08.2026). Gemessene Laengen: 8 bis 36 Zeichen.
  process_id         VARCHAR(36)  NOT NULL,

  -- Der kuratierte Partner. NULL IST EIN GUELTIGER GEPFLEGTER ZUSTAND (E4):
  -- zusammen mit pflegestatus = 'GEPFLEGT' heisst er "hingesehen, es gibt
  -- nichts". Ohne diese Speicherbarkeit stuenden Auffangprozesse dauerhaft auf
  -- offen und der Fortschritt erreichte nie sein Ende.
  partner            VARCHAR(100) NULL,

  -- EINGEHEND oder AUSGEHEND, sonst NULL. Bewusst VARCHAR statt ENUM: ein
  -- weiterer Wert soll keine Migration kosten, dieselbe Entscheidung wie bei
  -- audit_log.event_type und app_user.role. Die Whitelist steht im Code.
  richtung           VARCHAR(20)  NULL,

  -- OFFEN oder GEPFLEGT — genau zwei Zustaende. Ein dritter ("nicht zuordenbar")
  -- ist verworfen: "gepflegt mit leerem Partner" sagt dasselbe mit den Feldern,
  -- die ohnehin da sind (E4, docs/prozess-katalog.md §9).
  pflegestatus       VARCHAR(20)  NOT NULL,

  -- Woher der Vorschlag stammt, den die Zeile traegt: REGEL_A (Praefix und
  -- Position), REGEL_B (CamelCase mit Richtungsanker) oder KEINE. Sie steht als
  -- eigene Spalte und nicht als Kommentar, weil die Regeln unter offenen Punkten
  -- stehen (docs/prozess-katalog.md §10) und sich aendern werden: Ohne sie waere
  -- nach einer Regelaenderung nicht mehr feststellbar, welche Zeile aus welcher
  -- Fassung stammt. KEINE heisst "geprueft, nichts abgeleitet" — nicht "noch
  -- nicht gelaufen" (Regel Q4).
  vorschlag_herkunft VARCHAR(20)  NOT NULL,

  -- Wann und von wem die Zeile zuletzt angefasst wurde. Das steht NICHT
  -- anstelle des audit_log, sondern neben ihm: Die Tabelle ist handkuratiert und
  -- nicht wiederherstellbar (docs/bam-sollaengen.md §7.2), deshalb muss an der
  -- Zeile selbst ablesbar sein, wann sie zuletzt geaendert wurde — ohne das
  -- Protokoll zu durchsuchen.
  --
  -- DATETIME(3) in UTC und nicht TIMESTAMP: Zeitzonen und die 2038-Grenze,
  -- dieselbe Festlegung wie in V1 und V2. Gesetzt wird der Wert von der
  -- Anwendung aus der Systemuhr (common/ZeitConfig, Regel Z1), nie von der
  -- Datenbank — es gibt deshalb kein DEFAULT CURRENT_TIMESTAMP und kein
  -- ON UPDATE.
  geaendert_am       DATETIME(3)  NOT NULL,
  -- VARCHAR(100) wie die Obergrenze der Benutzernamen beim Anlegen; die Grenze
  -- kommt aus SPRING_SESSION.PRINCIPAL_NAME (admin/AdminUserService).
  geaendert_von      VARCHAR(100) NOT NULL,

  PRIMARY KEY (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
