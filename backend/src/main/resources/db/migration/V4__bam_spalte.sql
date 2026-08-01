-- Overlord Monitoring — kuratierte BAM-Spalten der Nachrichtenliste (Schritt 4).
--
-- Die Liste zeigt je Mandant zwei BAM-Werte als Spalten. Naheliegend waere,
-- sie ueber GlassfishDB.MessageBAMMandant.MessageBAMTypeSortIndex zu waehlen —
-- und fuer sechs der sieben konfigurierten Mandanten trifft dieser Index die
-- richtige Wahl. Bei NEXANS nicht: Die beiden kleinsten Indizes sind 9000
-- "Abladestelle_L_SAP" und 9001 "Abrufnummer_L_SAP". Die Abladestelle ist genau
-- das Feld, das die Projektbeschreibung als millionenfach vorkommenden Wert
-- nennt ("050"); als Spalte staende dort auf fast jeder Zeile dasselbe. Die
-- Lieferschein-Nr. liegt auf Platz sieben.
--
-- Deshalb kuratiert statt geraten (Abschnitt 4.4 der Projektbeschreibung,
-- Regel Q4): Die Heuristik — hier der Sortierindex — befuellt vor, die Wahrheit
-- steht in dieser Tabelle. Vorbelegt wird ausschliesslich die Ausnahme.
--
-- Zeichensatz und Sortierung stehen explizit (nie geerbt), sonst bricht der
-- Join gegen GlassfishDB, sobald der Server-Default sich aendert oder die
-- Instanz auf MariaDB 11 gehoben wird. Begruendung: docs/datenzugriff.md §5.
--
-- KEIN Fremdschluessel ueber die Schemagrenze: mandant_id zeigt fachlich auf
-- GlassfishDB.Mandant.MandantID und message_bam_type auf
-- GlassfishDB.MessageBAMType.MessageBAMType, tragen aber bewusst keinen
-- Fremdschluessel dorthin (Abschnitt 6 der Projektbeschreibung). Verschwindet
-- ein BAM-Typ im Altsystem, bleibt die Kuratierung bestehen, statt
-- rueckwirkend zu verschwinden.

CREATE TABLE bam_spalte (
  -- VARCHAR(36) passend zu GlassfishDB.Mandant.MandantID. Die MandantID ist ein
  -- lesbarer Code (NEXANS, VOTG, ...), keine UUID.
  mandant_id       VARCHAR(36) NOT NULL,
  -- 1 oder 2 — die Reihenfolge der beiden Spalten in der Liste. Als Teil des
  -- Primaerschluessels ist damit von selbst ausgeschlossen, dass ein Mandant
  -- zwei erste Spalten bekommt; genau diese Mehrdeutigkeit hat der Sortierindex
  -- bei VOTG, der 2002 zweimal vergibt.
  position         TINYINT     NOT NULL,
  -- SMALLINT passend zu GlassfishDB.MessageBAMType.MessageBAMType.
  message_bam_type SMALLINT    NOT NULL,
  PRIMARY KEY (mandant_id, position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Vorbelegung: nur NEXANS, nur weil der Sortierindex dort danebengreift. Fuer
-- jeden anderen Mandanten bleibt die Tabelle leer und der Sortierindex gilt.
INSERT INTO bam_spalte (mandant_id, position, message_bam_type) VALUES
  ('NEXANS', 1, 9006),   -- Lieferschein-Nr._L_SAP
  ('NEXANS', 2, 9001);   -- Abrufnummer_L_SAP
