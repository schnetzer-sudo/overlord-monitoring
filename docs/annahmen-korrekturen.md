# Annahmen und Korrekturen

Was die Erhebungen gegen die Testkopie gegenüber `PROJEKTBESCHREIBUNG.md` verändert oder bestätigt
haben. Einmalig festgehaltene Fakten über das Quellsystem, die sonst niemand mehr nachvollziehen
kann.

---

## Erhebung 27.07.2026 (bereits in die Projektbeschreibung eingearbeitet)

- **Mengengerüst rund Faktor zehn kleiner als früher angenommen.** Nicht 36 Mio. Zeilen in `Message`,
  sondern 3,34 Mio. / 2,9 GB. Die maßgebliche Kennzahl ist nicht die Zeilenzahl, sondern die
  **Bytegröße** von `MessageProperty` (46,96 Mio. Zeilen, **61 GB**, 82 % der Datenbank).
- **Aufbewahrung 22 Monate**, nicht ein Jahr — ältester Datensatz `2024-10-01`.
- **Testkopie reicht bis 08.07.2026**, nicht bis Ende 2025.
- **Annahme A6 widerlegt:** `COMMIT_REJECTED` ist ein Fehler ohne `ERROR_`-Präfix.
- **Annahme A7 bestätigt:** 1.490 Prozesse.
- **`MessageStatisticHistory` enthält 42 Zeilen** und ist als Aggregationsquelle unbrauchbar — daher
  das eigene `message_rollup` (Schritt 10).
- **Offen (Annahme A8):** 142 Projekte stehen 134 Zeilen in `ProjectMandant` gegenüber. Projekte ohne
  Mandantenzuordnung wären für niemanden sichtbar. **Vor Schritt 4 zu klären.**

---

## Erhebung 01.08.2026 (vor Schritt 4)

Vollständig mit Statements, `EXPLAIN` und Laufzeiten in
[`messungen-schritt4.md`](messungen-schritt4.md). Was sich dadurch an der bestehenden Dokumentation
geändert hat:

### `MessageTimeout` ist in **Sekunden**, nicht in Minuten (Messung M8)

Die bisher an vier Stellen dokumentierte „Dauer in Minuten" ist **falsch**. Beleg:
`SOSActionTimeout = 1800` steht 37.120-mal neben dem Ablaufschritt `WAIT|30M` — 1800 Sekunden sind
exakt 30 Minuten; der einzige andere vorkommende Wert `300` passt zum Schritt `5M`. Unter der
Minuten-Lesart stünde eine Frist von 30 **Stunden** neben einem Schritt, der 30 **Minuten** wartet.

Korrigiert in `datenmodell.md` §3 und §5.3, `DEVELOPMENT_GUIDELINES.md` Z2 und §5.3 (Feldname
`timeoutSekunden` statt `timeoutMinuten`) sowie `PROJEKTBESCHREIBUNG.md` Abschnitt 3.3. **Die
Größenordnung ändert sich um Faktor 60.**

Die Schwachstelle der Belegkette gehört dazu: Gemessen ist `SOSActionTimeout`, **nicht**
`Message.MessageTimeout` selbst. Ein direkter Beleg ist auf der Testkopie nicht zu bekommen, weil
dort keine Nachricht existiert, an der diese Frist sichtbar abläuft (`RUNNING` kommt null Mal vor).
Die Einheit steht im Code deshalb an genau einer Stelle als benannte Konstante.

### `ERROR_TIMEOUT` entsteht nicht aus `MessageTimeout` (Messung M8)

Die 52 so gekennzeichneten Nachrichten laufen 2 bis 5,6 Minuten und brechen **höchstens 120 Sekunden**
nach dem Start ihrer letzten Aktion ab. Das ist eine kürzere Frist auf Dienstebene. Wer die 52 Zeilen
als Beispiele für ein abgelaufenes `MessageTimeout` liest, liest sie falsch.

### Der Bestand der Testkopie hat eine fünfmonatige Lücke (Messungen M0, M9)

Dicht bis `2025-12-30`, dann **keine einzige Zeile** von Januar bis Mai 2026, dann fünf verstreute
Tage mit zusammen 5.133 Zeilen, an denen ausschließlich `NEXANS` Daten hat. Der Datenstand
`08.07.2026` ist als **Maximum** korrekt, beschreibt aber nicht die Dichte.

Folge: Der Anker der Dev-Uhr ist von `MAX(Message.MessageLastUpdate)` auf den jüngsten Zeitpunkt an
einem Tag mit mindestens drei Mandanten umgestellt (`2025-12-30 04:09:47`). Vorher zeigte das
24-Stunden-Standardfenster 285 Zeilen eines Mandanten, jetzt 6.382 Zeilen aus sechs — darunter
`VOTG` und `SUTTONS`, die beiden Testmandanten des Isolationstests. Details in
[`datenzugriff.md`](datenzugriff.md) §6.

### Weitere Befunde ohne Änderung an der Dokumentation

- **`ProjectMandant` ist n:m im Schema, 1:1 in den Daten** — alle 134 Projekte gehören genau einem
  Mandanten, der Join vervielfacht nichts. Zu **Annahme A8**: 140 Projekte gegenüber 134 Zuordnungen;
  die sechs nicht zugeordneten tragen **keine** Nachricht.
- **Die View `MessageMandantID` ist mit den Rechten der Anwendung nicht analysierbar** —
  `SHOW CREATE VIEW` und `EXPLAIN` scheitern beide am fehlenden Recht `SHOW VIEW`. Regel L7 ist für
  diesen Zugriffsweg damit nicht erfüllbar.
- **`Message` hat sechs Indizes**, `datenmodell.md` §3 nennt drei.
- Die Zählstände von `information_schema` sind veraltet (`Message` 3.560.486 gegenüber 3.341.519
  gezählt).

---

## Erhebung 28.07.2026 (Schritt 2, neu)

### Der Server der Testkopie ist global `read_only`

`SELECT @@global.read_only` → `1`. Konsequenzen, alle verifiziert:

- **`monitor_read` kann nirgends schreiben** — jeder Schreibversuch scheitert mit Fehler **1290**
  („server is running with the --read-only option"), noch **vor** der Rechteprüfung. Das ist ein
  vierter, unbeabsichtigter Schutz vor Schreibzugriffen auf `GlassfishDB` (siehe `datenzugriff.md`
  §4).
- **`monitor_write` schreibt trotzdem erfolgreich** nach `overlord_monitor` (schema-weite
  `ALL PRIVILEGES`), obwohl der Server global `read_only` ist — sechs von sechs Schreibversuchen
  erfolgreich, Flyway-Migration `V1` erfolgreich angewandt.
- ⚠️ **Transienter Effekt:** In einem kurzen Fenster (vermutlich Neubefüllung der Testkopie) scheiterte
  auch `monitor_write` kurzzeitig mit `1290`. Schlägt ein lokaler Build mit dieser Meldung fehl, ist
  das **kein Code-Fehler** — den Lauf wiederholen. Der Effekt ist auf die Testkopie beschränkt.
- Der Schreibverbotstest akzeptiert deshalb `1290` **oder** `1142` (fehlendes Recht).

### `transaction_read_only` existiert auf MariaDB 10.6 nicht

`SET SESSION ... transaction_read_only=1` → Fehler **1193** „Unknown system variable". Es ist eine
MySQL-Variable. Der Lese-Pool bleibt bei `SET SESSION max_statement_time=10`. Details:
`datenzugriff.md` §1.

### `bit(1)` → `Boolean` bestätigt

`Message.Source` und `Message.Target` sind `bit(1)`; der jOOQ-`forcedType` erzeugt korrekt
`Boolean`-Felder.

### Kleinere Abweichungen der Zählstände

Die Zählstände der Testkopie wandern mit jeder Neubefüllung. Am 28.07.2026:

- `Mandant`: 10 Zeilen.
- `Project`: **140** Zeilen (Projektbeschreibung nennt 142). Für **Annahme A8** heißt das: der genaue
  Abgleich `Project` ↔ `ProjectMandant` ist vor Schritt 4 ohnehin frisch zu erheben; die absolute
  Zahl ist nicht stabil.
- `DISTINCT MessageStatus`: 12 vorhandene Werte (alle dokumentierten außer `RUNNING`), kein neuer.

### Umgebung: Maven Central nur über IPv4 erreichbar

In diesem Netz scheitert der Weg zu `repo.maven.apache.org` über IPv6 (Timeout auf `2606:4700::…`).
Lokale Builds brauchen `MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"` (oder ein `.mvn/jvm.config`).
Die GitHub-CI ist nicht betroffen. Kein Fachthema, aber ohne diesen Hinweis kostet der erste Build
Zeit.
