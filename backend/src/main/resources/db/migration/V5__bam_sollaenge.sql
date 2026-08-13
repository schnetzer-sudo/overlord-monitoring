-- Overlord Monitoring — Sollaengen-Kuratierung fuer die BAM-Suche (Schritt 7, Teil 2a).
--
-- WOZU. Der Nutzer tippt eine Belegnummer vom Beleg ab. Fuehrende Nullen stehen
-- dort nicht, im Bestand aber sehr wohl: Messung M43-4 zeigt, dass die rohe
-- Fassung in sechs von acht geprueften Faellen NULL Treffer findet und die auf
-- Sollaenge aufgefuellte die richtigen. Eine leere Trefferliste sieht fuer den
-- Nutzer aber aus wie "gibt es nicht", nicht wie "falsch getippt". Damit die
-- Suche auffuellen kann, braucht sie je Mandant und Typ eine Sollaenge — und die
-- wird kuratiert, nicht geraten (Regel Q4).
--
-- WARUM DER SCHLUESSEL DEN MANDANTEN TRAEGT. Naheliegend waere, die Sollaenge als
-- Eigenschaft des Typs zu behandeln. Messung M46-2 widerlegt das an zwei
-- Stellen, und beide sind gemessen und nicht befuerchtet:
--   * Typ 2000 (OrderNumber) hat bei SUTTONS die dominante Laenge 6 (97,33 %),
--     bei VOTG die Laenge 7 (84,06 %). Eine typweite Sollaenge waere fuer VOTG
--     falsch.
--   * Typ 9014 (Lieferantennummer beim Kunden) erreicht ueber den Bestand nur
--     58,45 % — bei WOC aber 95,21 %. Ein typweiter Schnitt haette den Eintrag
--     verworfen, obwohl er fuer diesen Mandanten traegt.
-- Deshalb (mandant_id, message_bam_type) und nicht message_bam_type allein. Das
-- ist der teurere, aber der richtige Schnitt.
--
-- WELCHE ZEILE ENTSTEHT. Mechanisch nach der 95-Prozent-Regel: ein Paar bekommt
-- eine Sollaenge, wenn der Typ bei diesem Mandanten ueberhaupt Werte mit
-- fuehrender Null traegt UND seine haeufigste Laenge ueber den Bestand
-- mindestens 95 % der Zeilen dieses Paares stellt. Die Schwelle stammt aus dem
-- 2001-Fall (M43): dort sagte ein Monat 100 % und der Bestand 67,21 %.
-- Vollstaendig mit allen 45 gemessenen Paaren in docs/bam-sollaengen.md.
--
-- Zeichensatz und Sortierung stehen explizit (nie geerbt), sonst bricht der Join
-- gegen GlassfishDB, sobald der Server-Default sich aendert oder die Instanz auf
-- MariaDB 11 gehoben wird. Begruendung: PROJEKTBESCHREIBUNG.md §5.
--
-- KEIN Fremdschluessel ueber die Schemagrenze (§6): mandant_id zeigt fachlich
-- auf GlassfishDB.Mandant.MandantID, message_bam_type auf
-- GlassfishDB.MessageBAMType.MessageBAMType. Verschwindet ein Typ im Altsystem,
-- bleibt die Kuratierung bestehen, statt rueckwirkend zu verschwinden — dieselbe
-- Regel wie bei bam_spalte und process_catalog.
--
-- KEINE _bin-Spalte: hier steht nichts Tokenartiges (§5).

CREATE TABLE bam_sollaenge (
  -- VARCHAR(36) passend zu GlassfishDB.Mandant.MandantID, wie in bam_spalte. Die
  -- MandantID ist ein lesbarer Code (NEXANS, VOTG, ...), keine UUID.
  mandant_id          VARCHAR(36)   NOT NULL,
  -- SMALLINT: gegen information_schema erhoben und nicht uebernommen —
  -- GlassfishDB.MessageBAM.MessageBAMType und GlassfishDB.MessageBAMType.MessageBAMType
  -- sind beide smallint(6) NOT NULL (M46-Rahmen, 13.08.2026).
  message_bam_type    SMALLINT      NOT NULL,

  -- Die Sollaenge, auf die ein Suchbegriff dieses Paares mit Nullen aufgefuellt
  -- wird. NULL heisst: fuer dieses Paar wird nicht aufgefuellt — die Zeile steht
  -- dann nur wegen des Leerzeichen-Kennzeichens da. Werte liegen zwischen 1 und
  -- 70 (GlassfishDB.MessageBAM.MessageBAMValue ist varchar(70), M32).
  -- Bewusst TINYINT UNSIGNED und nicht TINYINT(1): der Codegen bildet nur
  -- TINYINT(1) auf Boolean ab, tinyint(3) unsigned bleibt eine Zahl.
  sollaenge           TINYINT UNSIGNED NULL,

  -- Traegt dieses Paar Werte mit einem FUEHRENDEN Leerzeichen? Dann sucht die
  -- Anwendung zusaetzlich die Fassung mit Leerzeichen. Folgende Leerzeichen
  -- brauchen kein Kennzeichen: utf8mb4_general_ci ist PAD SPACE, '123 ' = '123'
  -- ist wahr (M43-3). Fuehrende sind es nicht — ' 123' = '123' ist falsch.
  fuehrendes_leerzeichen BOOLEAN    NOT NULL DEFAULT FALSE,

  -- Herkunftsnachweis (Regel L10). dominanz_prozent ist der gemessene Anteil der
  -- haeufigsten Laenge an allen Zeilen des Paares, leerzeichen_prozent der
  -- gemessene Anteil mit fuehrendem Leerzeichen. zeilen ist das n dahinter: "n
  -- sichert den Umfang, der Vermerk den Schluss" — eine Dominanz von 100 % auf 9
  -- Zeilen ist etwas anderes als dieselben 100 % auf 793.588.
  dominanz_prozent    DECIMAL(5,2)  NULL,
  leerzeichen_prozent DECIMAL(9,6)  NULL,
  zeilen              INT           NOT NULL,
  gemessen_am         DATE          NOT NULL,

  PRIMARY KEY (mandant_id, message_bam_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Befuellung aus M46 vom 13.08.2026, mechanisch nach der 95-Prozent-Regel.
-- Vierzehn Paare bekommen eine Sollaenge, zwei ein Leerzeichen-Kennzeichen.
--
-- ZWEI DER VIERZEHN KOENNEN NACHWEISLICH NICHT WIRKEN, und das steht hier und
-- nicht in einer Fussnote: Bei IBIS/1 und SUTTONS/2000 kommt ueberhaupt kein
-- Wert MIT fuehrender Null auf der Sollaenge vor (M46-2c). Die Regel, wie sie
-- beauftragt ist, faengt das nicht — sie fragt nur nach der Laengendominanz. Die
-- Zeilen stehen trotzdem da, weil die Befuellung mechanisch ist und nicht
-- nachgebessert wird; ob die Regel um diese Bedingung ergaenzt wird, ist eine
-- Entscheidung und wird hier nicht getroffen. Siehe docs/bam-sollaengen.md.
INSERT INTO bam_sollaenge
  (mandant_id, message_bam_type, sollaenge, fuehrendes_leerzeichen,
   dominanz_prozent, leerzeichen_prozent, zeilen, gemessen_am) VALUES
  -- Mandant   Typ           Soll  Leerz.  Dominanz  Leerz.-%  Zeilen  gemessen
  ('IBIS',     1,               7, FALSE,   99.00,   NULL,     155875, '2026-08-13'), -- Auftragsnummer; wirkungslos, s. o.
  ('SUTTONS',  2000,            6, FALSE,   97.33,   NULL,      12296, '2026-08-13'), -- OrderNumber; wirkungslos, s. o.
  ('VOTG',     2002,           10, FALSE,  100.00,   NULL,        456, '2026-08-13'), -- InvoiceNumber VTG
  ('VOTG',     2005,           10, FALSE,  100.00,   NULL,          9, '2026-08-13'), -- CustRef1
  ('VOTG',     2007,           10, FALSE,  100.00,   NULL,          9, '2026-08-13'), -- LoadNo
  ('NEXANS',   9000,            3, FALSE,   96.68,   NULL,     151063, '2026-08-13'), -- Abladestelle_L_SAP
  ('NEXANS',   9009,           10, FALSE,  100.00,   NULL,      17913, '2026-08-13'), -- Beleg-Nr. GS_L_SAP
  ('NEXANS',   9011,           10, FALSE,  100.00,   NULL,       1412, '2026-08-13'), -- Anlieferungs-Nr. ae_L_SAP
  ('NEXANS',   9012,           10, FALSE,  100.00,   NULL,      17934, '2026-08-13'), -- Charge_L_SAP
  ('NEXANS',   9013,           10, FALSE,  100.00,   NULL,       6148, '2026-08-13'), -- Nr. TSL_L_SAP
  ('WOC',      9014,            6, FALSE,   95.21,   NULL,       2067, '2026-08-13'), -- Lieferantennummer beim Kunden_K_SAP
  ('NEXANS',   9021,           10, FALSE,   99.24,   NULL,      55250, '2026-08-13'), -- Transportnummer_K_SAP
  ('NEXANS',   9024,           10, FALSE,  100.00,   NULL,      25359, '2026-08-13'), -- Rechnungsnummer_K_SAP
  ('NEXANS',   9036,            4, FALSE,   99.23,   NULL,      22847, '2026-08-13'), -- Lagerort Kunde_L_SAP
  -- Fuehrendes Leerzeichen, ohne Sollaenge: beide Typen liegen mit 43,61 % und
  -- 82,31 % Laengendominanz weit unter der Schwelle. 9018 ist ausgerechnet der
  -- Typ, der bei NEXANS auf 92,26 % der Wurzeln sitzt (M39). Bei 9020 ist es
  -- EINE Zeile von 998.686 — das Kennzeichen steht mechanisch, wirken wird es
  -- praktisch nie.
  ('NEXANS',   9018,         NULL, TRUE,     NULL,   1.349624, 2311236, '2026-08-13'), -- Kundenmaterialnummer_K_SAP
  ('NEXANS',   9020,         NULL, TRUE,     NULL,   0.000100,  998686, '2026-08-13'); -- Lieferschein, Entnahme, PUS_K_SAP
