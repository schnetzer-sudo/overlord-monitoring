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

## Erhebung 28.07.2026 (Schritt 2, neu)

### Der Server der Testkopie ist global `read_only`

`SELECT @@global.read_only` → `1`. Konsequenzen, alle verifiziert:

- **`monitor_read` kann nirgends schreiben** — jeder Schreibversuch scheitert mit Fehler **1290**
  („server is running with the --read-only option"), noch **vor** der Rechteprüfung. Das ist ein
  vierter, unbeabsichtigter Schutz vor Schreibzugriffen auf `GlassfishDB` (siehe `datenzugriff.md`
  §4).
- **`monitor_root` schreibt trotzdem erfolgreich** nach `overlord_monitor` (schema-weite
  `ALL PRIVILEGES`), obwohl der Server global `read_only` ist — sechs von sechs Schreibversuchen
  erfolgreich, Flyway-Migration `V1` erfolgreich angewandt.
- ⚠️ **Transienter Effekt:** In einem kurzen Fenster (vermutlich Neubefüllung der Testkopie) scheiterte
  auch `monitor_root` kurzzeitig mit `1290`. Schlägt ein lokaler Build mit dieser Meldung fehl, ist
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
