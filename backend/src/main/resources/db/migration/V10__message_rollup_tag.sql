-- Overlord Monitoring — die Tagesebene des Rollups (Schritt 10b-1, Teil C).
--
-- WOZU. Die Stundenebene aus V9 traegt die 48-Stunden-Ansicht (6,8 ms) und die
-- 30-Tage-Ansicht (149,9 ms) muehelos. Bei ZWOELF MONATEN traegt sie nicht:
-- M94 hat dafuer 2.206,854 ms fuer den Verlauf und 2.491,443 ms fuer die
-- Verteilung gemessen — das 5,5- bis 6,2-Fache der vorregistrierten
-- 400-ms-Schwelle und das 4,4- bis 5,0-Fache des 500-ms-Budgets der ganzen
-- Landingpage. Entscheidung E-b ("nur die Stundenebene wird materialisiert")
-- traegt damit bei zwoelf Monaten nicht; das ist offener Punkt 55.
--
-- WARUM EINE ZWEITE EBENE UND NICHT EINE OPTIMIERUNG. M94 hat die Kosten je
-- gelesener Rollupzeile ueber einen Mengenbereich von Faktor 302 gemessen: 5,3
-- bis 11,5 Mikrosekunden, also SAUBER LINEAR. Wenn nichts ueberproportional
-- teuer ist, gibt es auch nichts wegzuoptimieren — die einzige Stellschraube ist
-- die Zahl der gelesenen Zeilen. Der Katalog-Join, der naheliegende Verdaechtige,
-- traegt bei zwoelf Monaten nur 5 bis 13 Prozent (Befund 15).
--
-- DIE GROESSENORDNUNG IST GERECHNET, NICHT GESCHAETZT. M87 hat den
-- Verdichtungsfaktor der Tagesebene gegenueber der Stundenebene mit 2,73
-- gemessen und daraus rund 123.000 Tageszeilen vorhergesagt. Nachgerechnet am
-- 27.08.2026 gegen die gefuellte Stundenebene: 123.049 Zeilen, SUM(anzahl) =
-- 3.341.519 — dieselbe Gesamtzahl wie in der Stundenebene und wie in Message
-- selbst. Die Zwoelf-Monats-Ansicht liest damit statt 280.186 Zeilen noch rund
-- 102.600; bei linearen Kosten (5,3 bis 11,5 us je Zeile, M94) ist das Faktor
-- 2,7 billiger.

CREATE TABLE message_rollup_tag (
  -- Der Kalendertag, gerechnet als DATE(message_rollup.stunde).
  --
  -- ZEITZONE: Wie stunde in V9 WANDUHRZEIT DES QUELLSERVERS und nicht UTC.
  -- Zeitstempel aus GlassfishDB werden nirgends konvertiert
  -- (docs/datenzugriff.md §7). Ein Tageseimer ist damit der Tag, wie ihn der
  -- Quellserver sieht — und genau so liest ihn das Dashboard wieder.
  --
  -- DATE und nicht DATETIME: Ein Tageseimer hat keine Uhrzeit. Ein DATETIME mit
  -- 00:00:00 sagte dasselbe und laedte dazu ein, ihn irgendwann mit einer
  -- Stundengrenze zu verwechseln.
  tag            DATE        NOT NULL,

  -- Wie in V9: VARCHAR(36) passend zu GlassfishDB.Message.ProcessID und zu
  -- process_catalog.process_id. Die Spalten werden in 10b-2 gegeneinander
  -- gejoint; eine abweichende Laenge oder Sortierung braeche den Join.
  process_id     VARCHAR(36) NOT NULL,

  -- DER ROHWERT, nicht die Einordnung — Entscheidung E-g, unveraendert. Die
  -- Tagesebene entsteht aus der Stundenebene, und die traegt den Rohwert.
  message_status VARCHAR(30) NOT NULL,

  -- Die Summe der anzahl-Werte der 24 Stundeneimer dieses Tages. INT wie in V9:
  -- Der groesste Tageseimer des Gesamtbestands traegt 19.126 Nachrichten
  -- (gemessen 27.08.2026, gegen 6.601 in der Stundenebene), der dichteste TAG
  -- insgesamt 28.444. Beides liegt um Groessenordnungen unter der Grenze.
  anzahl         INT         NOT NULL,

  -- DERSELBE SCHLUESSEL WIE IN V9, nur mit tag an Stelle von stunde. Der Einstieg
  -- laeuft ueber den Zeitbereich der ersten Spalte, genau wie dort.
  PRIMARY KEY (tag, process_id, message_status)

  -- KEIN SEKUNDAERINDEX, dieselbe Begruendung wie in V9: Die Probetabelle aus
  -- M89 hatte 0 Byte Indexanteil, die gebaute Tabelle nach dem Volllauf ebenso,
  -- und beide gemessenen Plaene steigen ueber den Primaerschluessel ein. Ein
  -- Index auf Verdacht kostete bei jedem Delta-Lauf Pflege.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ZEICHENSATZ UND SORTIERUNG EXPLIZIT, nie geerbt (PROJEKTBESCHREIBUNG.md §5)
-- und Zeichen fuer Zeichen wie bei message_rollup — aus DEMSELBEN Grund, und
-- hier zusaetzlich aus einem zweiten: Quelle und Ziel dieser Tabelle sind
-- dieselbe Datenbank. Die Tagesebene wird aus der Stundenebene GRUPPIERT; truege
-- sie eine andere Sortierung, fielen zwei Statuswerte, die sich nur in der
-- Gross- und Kleinschreibung unterscheiden, in der einen Ebene zusammen und in
-- der anderen auseinander. Die Summen stimmten dann nicht mehr ueberein, ohne
-- dass irgendetwas eine Fehlermeldung ergaebe.

-- WARUM EINE EIGENE TABELLE UND KEINE EBENEN-SPALTE IN message_rollup.
--
-- Laegen beide Ebenen in einer Tabelle, stuenden ein Tageseimer und seine 24
-- Stundeneimer NEBENEINANDER. Jede Summe ueber die Tabelle zaehlte damit
-- doppelt — und zwar lautlos: SUM(anzahl) ueber den Gesamtbestand ergaebe
-- 6.683.038 statt 3.341.519 (beide Ebenen tragen gemessen dieselbe
-- Gesamtzahl), und niemand saehe der Zahl an, dass sie zwei Ebenen addiert. Der
-- Primaerschluessel truege ausserdem zwei Bedeutungen: In der einen Zeile waere
-- die erste Spalte ein Stundenanfang, in der anderen ein Tagesanfang, und der
-- Unterschied stuende in einer Nachbarspalte.
--
-- Zwei Tabellen kosten dafuer eine zusaetzliche DELETE/INSERT-Runde je Lauf und
-- rund 40 Prozent des Platzes der Stundenebene. Beides ist billig; eine Summe,
-- die still das Doppelte ergibt, ist es nicht.

-- KEINE VORBELEGUNG. Die Zeilen entstehen im Lauf und nirgends sonst — dieselbe
-- Entscheidung wie in V6, V8 und V9.
--
-- KEINE DOWN-MIGRATION, wie ueberall in diesem Projekt.
--
-- KEIN FREMDSCHLUESSEL, weder ueber die Schemagrenze auf Process noch INNERHALB
-- von overlord_monitor auf message_rollup. Der zweite waere technisch moeglich
-- und trotzdem falsch: Die beiden Ebenen werden in derselben Transaktion
-- geloescht und neu geschrieben, und ein Fremdschluessel zwaenge die Reihenfolge
-- der beiden DELETE, ohne etwas zu sichern, was der Lauf nicht ohnehin sichert.
