-- Schritt 10b-2: die dritte Rollup-Ebene.
--
-- Dieselbe Bauform wie `message_rollup` (V9) und `message_rollup_tag` (V10), nur mit
-- `monat` statt `stunde` beziehungsweise `tag`. Jede dort getroffene Entscheidung gilt
-- unveraendert und ist in docs/rollup.md §2 begruendet; sie ist hier nicht wiederholt:
--
--   * eigene Tabelle statt einer Ebenen-Spalte -- sonst staenden ein Monatseimer und
--     seine Tageseimer nebeneinander, und jede Summe ueber die Tabelle zaehlte lautlos
--     mehrfach;
--   * der Rohstatus in `message_status` und nicht die Einordnung (E-g);
--   * derselbe Primaerschluessel, dieselbe Sortierung, dieselbe Zeichenkodierung --
--     `utf8mb4_general_ci`, damit Quelle und Ebene gleich gruppieren und der
--     Katalog-Join ueber `process_id` traegt;
--   * `monat` ist der erste Tag des Monats als DATE und traegt Wanduhrzeit des
--     Quellservers, nicht UTC.
--
-- KEIN SEKUNDAERINDEX. Der Index aus V11 liegt ausschliesslich auf der Stundenebene,
-- weil nur sie von der Fensterverengung der Nachrichtenliste gelesen wird. Fuer die
-- Monatsebene gibt es keine gemessene Frage, die der Primaerschluessel nicht bedient.
--
-- Erwartete Groesse ueber den Gesamtbestand: 11.957 Zeilen (M87, Variante 4).

CREATE TABLE message_rollup_monat (
  monat          DATE        NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (monat, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
