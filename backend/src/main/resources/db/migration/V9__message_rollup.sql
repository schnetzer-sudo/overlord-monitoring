-- Overlord Monitoring — stuendliche Aggregate und das Laufprotokoll (Schritt 10a).
--
-- WOZU. Leistungsregel L2 verbietet Live-Aggregation ueber Message fuer
-- Dashboard-Kennzahlen. message_rollup ist die Tabelle, aus der das Dashboard
-- stattdessen liest (Schritt 10b). Gefuellt wird sie von einem stuendlichen
-- Delta-Lauf und einem naechtlichen Volllauf; beide stehen in rollup/RollupJob.
--
-- DIE GROESSENORDNUNG IST GEMESSEN, NICHT GESCHAETZT. M89 hat genau diese
-- Tabellendefinition als Probetabelle angelegt und ueber den Gesamtbestand
-- gefuellt: 335.610 Zeilen, SUM(anzahl) = 3.341.519 (also jede Nachricht genau
-- einmal), 21,59 MiB, davon 0 Byte Indexanteil. Gegenueber Message (2.763,9 MiB)
-- ist das Faktor 128 kleiner. Der Verdichtungsfaktor ist 9,96 und liegt in jedem
-- einzelnen Monat zwischen 9,0 und 12,1 (M92) — der Rollup verdichtet also um
-- eine Zehnerpotenz und nicht um Groessenordnungen. Das ist bekannt und kein
-- Grund fuer eine zweite Ebene: docs/messungen-schritt10.md Befund 7.

CREATE TABLE message_rollup (
  -- Der Anfang des Stundeneimers, gerechnet ueber Message.MessageLastUpdate.
  --
  -- ZEITZONE: Das ist WANDUHRZEIT DES QUELLSERVERS und nicht UTC. Zeitstempel
  -- aus GlassfishDB werden nirgends konvertiert (docs/datenzugriff.md §7), und
  -- eine Umrechnung genau hier waere die Stelle, an der die Bruchkante
  -- unsichtbar wuerde. Die beiden UTC-Zeitstempel dieses Schritts stehen in
  -- rollup_lauf.gestartet_am / beendet_am und heissen bewusst anders.
  --
  -- DATETIME(0): Die Quellspalte ist datetime ohne Nachkommastellen, und der
  -- Wert ist ohnehin auf die volle Stunde abgeschnitten.
  stunde         DATETIME    NOT NULL,

  -- VARCHAR(36) passend zu GlassfishDB.Message.ProcessID und zu
  -- process_catalog.process_id (V6). Die drei Spalten werden in 10b gegeneinander
  -- gejoint; eine abweichende Laenge oder Sortierung braeche den Join.
  process_id     VARCHAR(36) NOT NULL,

  -- DER ROHWERT AUS Message.MessageStatus, nicht die Einordnung (Entscheidung
  -- E-g). Der MessageStatusClassifier laeuft beim LESEN, also in 10b. Grund: Die
  -- Einordnung ist eine Regel, die sich aendern kann; der Rohwert ist eine
  -- Tatsache. Waere die Kategorie materialisiert, muesste jede Regelaenderung die
  -- ganze Tabelle neu rechnen. Der Preis dafuer ist gemessen und betraegt EINE
  -- Zeile: 335.610 statt 335.609 (M87, Variante 1 gegen Variante 2).
  --
  -- VARCHAR(30) ist die Laenge der Quellspalte. MessageStatus ist FREIER TEXT
  -- und kein Aufzaehlungstyp (CKECKED ist der Beweis) — deshalb steht hier kein
  -- ENUM und keine Pruefbedingung.
  message_status VARCHAR(30) NOT NULL,

  -- Wie viele Nachrichten in diesem Eimer stehen. INT: der groesste gemessene
  -- Wert liegt weit darunter, die dichteste Stunde des Gesamtbestands traegt
  -- 8.630 Zeilen auf 14 Rollupzeilen (M88).
  anzahl         INT         NOT NULL,

  -- DER SCHLUESSEL IST (stunde, process_id, message_status) — Entscheidung E-a.
  -- MANDANT, PARTNER UND RICHTUNG STEHEN NICHT IN DER ZEILE. Sie kommen in 10b
  -- aus dem Join ueber Process -> ProjectMandant bzw. process_catalog.
  --
  -- WARUM DER MANDANT NICHT IN DEN SCHLUESSEL GEHOERT. Naheliegend waere er, denn
  -- jeder Mandant liest so den Rollup-Bereich ALLER Mandanten und filtert erst
  -- danach. Gemessen ist genau diese Fassung: 11,299 ms fuer das Standardfenster
  -- (48 h) mit Katalog-Join, 237,673 ms fuer ein Monatsfenster — bei einem
  -- Budget von 500 ms fuer die ganze Landingpage (M89). Der Mandant im Schluessel
  -- machte die Zeile breiter und spaerte davon nichts ein, was gebraucht wuerde.
  -- Waechst die Zahl der Mandanten deutlich ueber zehn, ist das die Stelle, an
  -- der neu zu rechnen ist — nicht heute.
  --
  -- WIDERSPRUCH ZU PROJEKTBESCHREIBUNG.md §5. Dort steht "stuendliche Aggregate
  -- je Mandant, Prozess, Partner, Richtung, Status". Das ist die aeltere Fassung;
  -- E-a hebt sie auf, und M89 stuetzt E-a. Der Nachtrag in §5 steht als offener
  -- Punkt in docs/rollup.md — er ist Sache der Korrekturrunde und nicht dieses
  -- Schritts.
  PRIMARY KEY (stunde, process_id, message_status)

  -- KEIN SEKUNDAERINDEX. Die Probetabelle aus M89 hatte 0 Byte Indexanteil, und
  -- beide dort gemessenen Plaene steigen ueber den Primaerschluessel ein (range
  -- auf PRIMARY, key_len 5 — also ueber stunde). Ein Index fuer die
  -- Prozesssicht (10c) ist nicht gemessen und wird hier nicht auf Verdacht
  -- gebaut. Jeder Index kostete bei jedem Delta-Lauf Pflege.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ZEICHENSATZ UND SORTIERUNG STEHEN EXPLIZIT AN DER TABELLE, nie geerbt
-- (PROJEKTBESCHREIBUNG.md §5) — sonst bricht der schemauebergreifende Join,
-- sobald der Server-Default sich aendert oder die Instanz auf MariaDB 11 gehoben
-- wird, wo die uca1400-Sortierungen Standard sind.
--
-- utf8mb4_general_ci IST ABSICHT UND NICHT NACHLAESSIGKEIT. message_status muss
-- dieselbe Sortierung tragen wie GlassfishDB.Message.MessageStatus, sonst
-- gruppieren Quelle und Rollup verschieden — und process_id muss dieselbe tragen
-- wie Process.ProcessID und process_catalog.process_id, sonst braeche der Join
-- in 10b. Das ist KEINE tokenartige Spalte im Sinne der _bin-Regel aus §5: Hier
-- steht kein Sitzungsschluessel und kein Ruecksetz-Token, sondern ein lesbarer
-- Prozessname und ein Statuswort. Dass ohne Ruecksicht auf Gross- und
-- Kleinschreibung verglichen wird, deckt sich mit dem Verhalten der Quelle und
-- mit MessageStatusClassifier, der jeden Rohwert vor dem Vergleich hochstellt.


-- Wasserstand und Protokoll in einer Tabelle.
--
-- WARUM DER WASSERSTAND EINE EIGENE TABELLE BRAUCHT UND NICHT MAX(stunde) IST.
-- Eine Stunde ohne Verkehr erzeugt KEINE Rollup-Zeile. MAX(stunde) sagt darum
-- "letzte Stunde mit Verkehr", nicht "bis hierhin ist gerechnet". Der
-- Unterschied ist genau der zwischen NICHTS PASSIERT und NOCH NICHT NACHGESEHEN
-- — und den darf das Dashboard spaeter nicht verwechseln. Auf dem Bestand der
-- Testkopie ist das keine Feinheit: Fuenf Monate (2026-01 bis 2026-05) tragen
-- null Zeilen (M92).
CREATE TABLE rollup_lauf (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,

  -- DELTA oder VOLL. VARCHAR und kein ENUM, dieselbe Entscheidung wie bei
  -- audit_log.event_type, app_user.role und process_catalog.richtung: ein
  -- weiterer Wert soll keine Migration kosten. Die Whitelist steht im Code
  -- (rollup/LaufArt).
  art                VARCHAR(10) NOT NULL,

  -- DATENZEIT: welchen Ausschnitt aus GlassfishDB dieser Lauf verarbeitet hat.
  -- Gerechnet mit der ANWENDUNGSUHR (Clock, @Primary), weil die Werte gegen
  -- Message.MessageLastUpdate gehalten werden und im Profil dev beim Anker der
  -- Testkopie liegen muessen. Ausdehnung immer auf ganze Stunden: fenster_von
  -- ist ein Stundenanfang, fenster_bis der Anfang der ersten NICHT mehr
  -- verarbeiteten Stunde (Obergrenze ausschliessend).
  --
  -- DATETIME(0) und nicht (3): Es sind Stundengrenzen, Millisekunden waeren
  -- vorgetaeuschte Genauigkeit.
  fenster_von        DATETIME    NOT NULL,
  fenster_bis        DATETIME    NOT NULL,

  -- PROTOKOLLZEIT in UTC, gerechnet mit systemClock — wie geaendert_am und das
  -- audit_log (Regel A5). NICHT die Anwendungsuhr: Sie ist im Profil dev um den
  -- Rueckstand der Testkopie zurueckversetzt (Stand 26.08.2026: 214 Tage), und
  -- eine Protokollzeile mit zurueckversetzter Uhr waere im Betrieb unlesbar.
  --
  -- DASS IN EINER ZEILE ZWEI VERSCHIEDENE UHREN STEHEN, IST DER KERN DIESES
  -- SCHRITTS und keine Ungenauigkeit. Im Profil dev liegen die beiden Paare
  -- Monate auseinander; das ist der erwartete Anblick. Begruendung vollstaendig
  -- in docs/rollup.md, Abschnitt "Die zwei Uhren".
  --
  -- DATETIME(3) in UTC und nicht TIMESTAMP: Zeitzonen und die 2038-Grenze,
  -- dieselbe Festlegung wie in V1, V2, V6 und V8. Gesetzt wird der Wert von der
  -- Anwendung, nie von der Datenbank — kein DEFAULT CURRENT_TIMESTAMP, kein
  -- ON UPDATE.
  gestartet_am       DATETIME(3) NOT NULL,

  -- NULL heisst ABGEBROCHEN oder NOCH LAUFEND — und beides zaehlt nicht zum
  -- Wasserstand. Ohne diese Unterscheidung hoebe ein Lauf, der mitten im
  -- Schreiben abstuerzt, den Wasserstand ueber ein Fenster, das nur zur Haelfte
  -- gerechnet ist.
  beendet_am         DATETIME(3) NULL,

  -- Wie viele Rollup-Zeilen dieser Lauf geschrieben hat. NULL, solange er nicht
  -- fertig ist. Nicht die Zahl der gelesenen Nachrichten — die steht nirgends
  -- und wird auch nicht gebraucht.
  zeilen_geschrieben INT         NULL,

  -- Die Meldung der Ausnahme, wenn der Lauf gescheitert ist. TEXT, weil eine
  -- Datenbankmeldung lang werden kann. Eine Zeile mit fehler IS NOT NULL zaehlt
  -- NICHT zum Wasserstand, auch wenn beendet_am gesetzt ist: Der Lauf ist
  -- abgeschlossen, aber sein Fenster ist nicht verlaesslich gerechnet.
  fehler             TEXT        NULL,

  PRIMARY KEY (id),

  -- Der Index fuer die Wasserstandsabfrage:
  --   SELECT MAX(fenster_bis) WHERE beendet_am IS NOT NULL AND fehler IS NULL
  -- Die Spaltenreihenfolge folgt der Abfrage: erst die Gleichheitsbedingung auf
  -- beendet_am (IS NOT NULL), dann der Wert, dessen Maximum gesucht ist. fehler
  -- steht NICHT im Index — TEXT-Spalten brauchen dort eine Praefixlaenge, und
  -- die Bedingung trifft im Normalbetrieb ohnehin jede Zeile.
  --
  -- ANDERS ALS BEI message_rollup gibt es hier einen Sekundaerindex, und der
  -- Unterschied hat einen Grund: rollup_lauf waechst um rund 25 Zeilen am Tag
  -- und wird bei JEDEM Lauf einmal vollstaendig nach dem Maximum gefragt.
  -- message_rollup waechst um Tausende und wird ueber den Primaerschluessel
  -- gelesen.
  KEY rollup_lauf_stand_idx (beendet_am, fenster_bis)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- KEINE VORBELEGUNG, dieselbe Entscheidung wie in V6 und V8: Die Zeilen
-- entstehen im Lauf und nirgends sonst. Eine leere rollup_lauf-Tabelle bedeutet
-- ausdruecklich "noch nie gerechnet" und loest im Delta-Lauf den Rueckgriff auf
-- MIN(Message.MessageLastUpdate) aus.
--
-- KEINE DOWN-MIGRATION, wie ueberall in diesem Projekt.
--
-- KEIN FREMDSCHLUESSEL ueber die Schemagrenze (§6): message_rollup.process_id
-- zeigt fachlich auf GlassfishDB.Process.ProcessID. Verschwindet ein Prozess im
-- Altsystem, bleiben seine Rollup-Zeilen bestehen — dieselbe Regel wie bei
-- process_catalog, bam_spalte und bam_sollaenge.
