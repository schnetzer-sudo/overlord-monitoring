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

### Annahme A8 ist erledigt: die sechs Projekte ohne Zuordnung tragen keine Nachricht

`Project` hat 140 Zeilen (gezählt), `ProjectMandant` 134 — sechs Projekte haben keinen Mandanten.
**Sie tragen zusammen null Nachrichten**: Die Summe der Nachrichten je Mandant aus M3 (b) ist exakt
gleich der Gesamtzahl in `Message` (3.341.519). Damit ist die offene Frage von Annahme A8
beantwortet, und zwar ohne Folgen — es gibt keine Nachricht, die durch die Mandantenkette fällt.

Die Entscheidung bleibt trotzdem, wie sie in [`mandantentrennung.md`](mandantentrennung.md) §2
steht: Nachrichten in Projekten ohne Zuordnung wären für niemanden sichtbar, auch nicht für ADMIN,
und es wird **kein** Sonderpfad und kein Pseudo-Mandant gebaut. Dass der Fall heute leer ist, macht
die Regel nicht überflüssig — es macht sie nur billig.

### Das Mengengerüst ist rund 7.300 Nachrichten am Tag, nicht 5.000

`PROJEKTBESCHREIBUNG.md` §8 und `datenmodell.md` §8 nannten „rund 5.000 pro Tag". Im **dichten** Teil
des Bestands sind es **rund 7.300**: 3.336.386 Zeilen über 456 Tage (01.10.2024 bis 30.12.2025).
Die 5.000 entstehen, wenn man den gesamten Zeitraum inklusive der fünfmonatigen Lücke durch die
Tage teilt.

> **Nachgezogen.** `datenmodell.md` §8 am 07.08.2026, `PROJEKTBESCHREIBUNG.md` §8 am selben Tag mit
> Schritt 5 — beide mit eigener, datierter Begründung an Ort und Stelle. Der Eintrag hier bleibt
> stehen: Er ist der Ort, an dem die Abweichung zuerst festgehalten wurde, und er belegt, dass die
> verbindliche Datei sechs Tage lang der korrigierten hinterherlief.

Für die Auslegung zählt die dichte Zahl: Ein 24-Stunden-Fenster im dichten Bestand hat 6.249 Zeilen
über alle Mandanten (M9) — das ist die Größenordnung, gegen die die Nachrichtenliste gemessen wurde
(L1 bis L10). Die Zahlen der Projektbeschreibung sind damit nicht falsch, aber sie beschreiben einen
Durchschnitt, den es an keinem einzigen Tag gab.

### Weitere Befunde ohne Änderung an der Dokumentation

- **`ProjectMandant` ist n:m im Schema, 1:1 in den Daten** — alle 134 Projekte gehören genau einem
  Mandanten, der Join vervielfacht nichts. Die Nachrichtenliste hält sich trotzdem an `EXISTS` statt
  an einen Join: Eine Liste, deren Zeilenzahl an einer Stammdatenpflege hängt, ist die falsche
  Grundlage für eine Sicherheitsgrenze.
- **Die View `MessageMandantID` ist mit den Rechten der Anwendung nicht analysierbar** —
  `SHOW CREATE VIEW` und `EXPLAIN` scheitern beide am fehlenden Recht `SHOW VIEW`. Regel L7 ist für
  diesen Zugriffsweg damit nicht erfüllbar.
- **`Message` hat sechs Indizes**, `datenmodell.md` §3 nennt drei.
- Die Zählstände von `information_schema` sind veraltet (`Message` 3.560.486 gegenüber 3.341.519
  gezählt).

---

## Erhebung 07.08.2026 (vor Schritt 5)

Vollständig mit Statements, `EXPLAIN` und Laufzeiten in
[`messungen-schritt5.md`](messungen-schritt5.md).

### `MessageAction` hat zwei Spalten, die niemand kannte — und es sind die wichtigsten

`SOSID` varchar(36) **NOT NULL** und `SOSActionID` smallint(6) **NOT NULL**. Beide fehlten in
`datenmodell.md` §3 und `PROJEKTBESCHREIBUNG.md` §3.2, die dort **sieben** statt **neun** Spalten
führten (Messung M14).

Der Grund für die Lücke ist der Vorgang, nicht der Inhalt: Die Spaltenliste war aus der
Projektbeschreibung **übernommen und nie gegen `information_schema` erhoben** — M1 hat
`MessageAction` nicht erfasst. Daraus folgt die neue Regel **L8** in
[`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md): Keine Quelltabelle taucht in
Anwendungscode auf, bevor ihre Spalten und Indizes erhoben sind. **`MessageBAM` ist bis heute nicht
erhoben und trägt Schritt 7.**

Die Folge war keine Kleinigkeit: Ohne diese Spalten wäre in Schritt 5 die im Plantext beauftragte
handgepflegte Zuordnungstabelle gebaut worden, die durch sie **entfällt**.

### Der Join auf die Ablaufdefinition läuft über `MessageAction`, nicht über `Message`

```sql
JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
```

Messung **M15** stellt drei Fassungen nebeneinander und vergleicht nicht die Auflösungsquote — die
ist bei allen dreien ähnlich und belegt deshalb nichts —, sondern den **ausgeführten Baustein gegen
den geplanten**:

| Fassung | erste Marke gleich | verschieden |
|---|---|---|
| `m.SOSID` + `ma.MessageActionID` (die ursprünglich vorgesehene) | 96,17 % | **375** |
| `m.SOSID` + `ma.SOSActionID` | 98,44 % | **157** |
| **`ma.SOSID` + `ma.SOSActionID`** | **100 %** | **0** |

Über den dichten Monat: 366.336 von 366.343 aufgelösten Zeilen stimmen überein (99,998 %), sieben
nicht.

⚠️ **`MessageActionID` ist nicht `SOSActionID`.** Die erste ist eine laufende Nummer je Nachricht ab
**0**, die zweite der Schlüssel in die Ablaufdefinition — und `SOSAction` nummeriert **nicht
lückenlos**: 257 von 1.777 Abläufen haben eine größte Kennung über ihrer Schrittzahl, 233 nutzen
Kennungen ab 99 (M20).

### Die 43,9 Prozent verwaister Verweise aus M13 sind erklärt — anders als vermutet

Die Vermutung lautete: `Message` trägt den heutigen Ablauf, `MessageAction` den zur Ausführungszeit.
**Widerlegt** (M20): Von den verwaisten Verweisen liegt **kein einziger** auf einer Zeile mit
abweichendem `SOSID` — alle liegen auf Zeilen, bei denen `MessageAction.SOSID` und `Message.SOSID`
übereinstimmen.

Die tatsächliche Ursache ist eine **Nummerierungslücke**: Der betroffene Ablauf definiert die
Schritte 1, 98 und 99; die Ausführung schreibt die fortlaufende Position 2. Der Verweis auf „2" muss
deshalb ins Leere laufen. Der ausgeführte Baustein ist derselbe — der geplante Schritt 98 und die
namenlose Aktion tragen beide die Marke `FTPSender`. **Die Zahlen von M13 bleiben unverändert; sie
sind richtig gemessen, nur anders zu lesen als damals angenommen.**

### `MessageProperty` ist kleiner und dichter als dokumentiert

| Angabe | Was dokumentiert war | Was gemessen ist |
|---|---|---|
| Zeilen je Nachricht | „rund zehn" (`datenmodell.md`), „rund vierzehn" (`PROJEKTBESCHREIBUNG.md`) | **22,57** über einen Tag, **22,88** über einen Monat; Minimum 14, Maximum 38 |
| Gesamtzahl | „mehrere hundert Millionen Zeilen bei einem Jahr Aufbewahrung" | **47 Millionen** bei **22 Monaten** |
| „1,3 Kilobyte je Zeile" | als Datenmenge gelesen | richtig als **Speicherbedarf**: 321 B Daten + 978 B Index. Die mittlere **Wertlänge** beträgt **26,4 Zeichen** |
| bekannte Namen | zehn | **101** im Tagesfenster, **119** im Monatsfenster |

**Die Zahl, die für die Detailansicht zählt, ist keine davon:** Alle Eigenschaften einer Nachricht
zusammen wiegen **595 Byte** — in beiden Fenstern auf das Byte gleich. Der Typ `mediumtext` erlaubt
16 MB je Zelle; der größte gemessene Wert liegt bei 12,4 KB über einen Monat (M17).

Korrigiert in `datenmodell.md` §3, §5.4 und §8. Bei der Gelegenheit dort nachgezogen: **„rund 5.000
Nachrichten pro Tag" ist rund 7.300 im dichten Bestand** — seit dem 01.08.2026 hier festgehalten,
in `datenmodell.md` §8 aber bis heute nicht übernommen gewesen.

### Was `docs/README.md` jetzt unterscheidet

Die Regel „keine Produktionsdaten" hat nicht beantwortet, ob eine Bausteinmarke wie
`NXS_FILE_CONVERT` darunter fällt, weil `NXS` ein Mandantenkürzel ist. Geschärft am 07.08.2026:
**Der Trennstrich läuft zwischen Nutzdaten und Konfiguration**, nicht zwischen „intern" und „extern".
Geschützt sind Belegnummern, `MessagePropertyValue`-Inhalte, **Partner**namen, Zugangsdaten und
Hostnamen; nicht geschützt ist das Konfigurationsvokabular des Altsystems einschließlich der
Mandantenkürzel — die zehn Mandanten stehen ohnehin mit vollem Firmennamen in
`PROJEKTBESCHREIBUNG.md` §3.2.

---

## Erhebung 11./12.08.2026 (vor Schritt 7)

Vollständig mit Statements, `EXPLAIN` und Laufzeiten in
[`messungen-schritt7.md`](messungen-schritt7.md) — die Hauptrunde M32 bis M41 und E6 vom 11.08.2026,
die Nachträge M42 bis M44 vom 12.08.2026. Hier steht nur, was sich dadurch an der bestehenden
Dokumentation geändert hat.

### `MessageBAM` ist um 41,9 Prozent größer als dokumentiert (M33‑0)

| | |
|---|---:|
| dokumentiert in `PROJEKTBESCHREIBUNG.md` §8 und `datenmodell.md` §8, als „gemessen" ausgewiesen | 10.859.666 |
| **gezählt** (zweimal, identisch) | **15.406.350** |
| Abweichung | **+ 4.546.684 (+ 41,9 %)** |

Die dokumentierte Zahl war die Stichprobenschätzung `information_schema.TABLE_ROWS`. Dass sie so
lange stehen blieb, hat einen benennbaren Grund: **`MessageBAM` war die Tabelle, die Regel L8 als
„nicht erhoben" führte** — die einzige nicht erhobene Tabelle, die einen MVP-Schritt trägt. Gezählt
worden ist sie erst, als Schritt 7 sie zum ersten Mal über den **Wert** statt über die `MessageID`
anfasste. Sie ist damit die **dritte** der vier großen Tabellen, deren Schätzung überhaupt geprüft
wurde — nach `Message` (M0) und `MessageAction` (M14) —, und sie hielt bis zum Folgetag den Rekord
der größten gefundenen Abweichung.

Die abgeleitete Kennzahl ändert sich mit: **4,61 statt „rund drei" BAM-Einträge je Nachricht**.

### `MessageProperty` ist um 60,9 Prozent größer als dokumentiert (M44)

Einen Tag später und mit noch größerem Abstand:

| | |
|---|---:|
| dokumentiert in `PROJEKTBESCHREIBUNG.md` §3.2 und §8, `datenmodell.md` §3 und §8 | 46.964.279 |
| **gezählt** (Kaltlauf und warmer Lauf, identisch) | **75.571.462** |
| Abweichung | **+ 28.607.183 (+ 60,91 %)** |

Auch hier ist der Grund benennbar, und er ist unangenehmer als bei `MessageBAM`: **M14 hat den
Zähllauf am 07.08.2026 ausdrücklich abgelehnt** — „die Schätzung genügt, ein Zähllauf über
47 Millionen Zeilen ist den Aufwand nicht wert". Ausgerechnet die Tabelle, bei der die Zählung als
zu teuer galt, trug die größte Abweichung. Sie kostet **199,380 s** kalt und **20,633 s** warm.

**Und die Zahl löst einen dokumentierten Widerspruch auf, statt einen zu erzeugen.** Der Abschnitt
„`MessageProperty` ist kleiner und dichter als dokumentiert" (07.08.2026, oben) stellt „rund
vierzehn Zeilen je Nachricht" gegen die gemessenen 22,57, und `datenmodell.md` §3 erklärt die Lücke
mit **verschiedenen Nennern**: das eine sei das Mittel über den Gesamtbestand, das andere der dichte
Bestand. **Die Erklärung war plausibel und ist falsch.** Aus der gezählten Zahl folgen
75.571.462 / 3.341.519 = **22,62** — praktisch derselbe Wert. Es gab keinen Dichteeffekt, es gab
eine um 60,9 % zu niedrige Schätzung; die Erklärung hat sie fünf Tage lang zugedeckt.

**Was der Abschnitt vom 07.08.2026 daraus gerechnet hatte, ändert sich mit** — alle drei Werte
stammen aus der Schätzung:

| dort | dort genannt | gerechnet aus der Zählung |
|---|---:|---:|
| Gesamtzahl | „**47 Millionen** bei 22 Monaten" | **75,6 Millionen** |
| Speicherbedarf je Zeile | 321 B Daten + 978 B Index | **200 B Daten + 608 B Index** = 808 B |
| Zeilen je Nachricht, Gesamtbestand | 14,05 | **22,62** |

Die **Verhältnis**aussage „zu drei Vierteln Index" hält unverändert (75,3 %), und die
22,57 / 22,88 aus M17 sind direkt gemessen und damit ebenfalls unberührt. Der Abschnitt vom
07.08.2026 bleibt stehen; korrigiert sind nur die Zahlen, die er aus der Schätzung abgeleitet hat.

### Der Befund, auf den es ankommt: das Muster

Dies ist die **vierte** als „gemessen" ausgewiesene Angabe in der verbindlichen Datei, für die keine
Messung existierte. Der Auftrag zu dieser Korrektur ging von dreien aus — die vierte ist bei der
Umsetzung dazugekommen, am selben Tag:

| Datum | Angabe | Was sie wirklich war |
|---|---|---|
| 07.08.2026 | „rund 5.000 Nachrichten pro Tag" | ein Durchschnitt über einen Zeitraum mit fünfmonatiger Datenlücke |
| 11.08.2026 | „`MatchInterchange` läuft stündlich" | eine Angabe ohne verzeichnete Quelle (M31‑3, als **ungedeckt gekennzeichnet**, nicht gestrichen) |
| 11.08.2026 | „`MessageBAM`: 10.859.666 Zeilen, gemessen" | die `information_schema`-Stichprobenschätzung (M33‑0) |
| 12.08.2026 | „`MessageProperty`: 46.964.279 Zeilen, gemessen" | dieselbe Schätzung, mit der größten Abweichung (M44) |

**Das Gemeinsame ist nicht die Zahl, sondern die Überschrift.** In allen vier Fällen stand eine
übernommene Angabe unter einer Formulierung, die eine Erhebung behauptete — „gemessen am", „Gemessenes
Mengengerüst", eine Tabellenzeile ohne Konjunktiv. Keine der vier war als Schätzung erkennbar, und
genau deshalb hat sie niemand nachgerechnet.

**Der Umfang ist mit M44 beziffert:** Von den **sieben** Zeilen des Mengengerüsts in
`PROJEKTBESCHREIBUNG.md` §8 war genau **eine** gezählt (`Message`). Sechs stammten aus
`information_schema`, fünf davon lagen daneben — in **beide** Richtungen und von **0,9 % bis
60,9 %**: `Project` 1,4 % zu hoch, `Process` und `MessageAction` je 0,9 % zu niedrig, `MessageBAM`
41,9 % und `MessageProperty` 60,9 % zu niedrig. **Es gibt keine Faustregel, mit der man die
Schätzung korrigieren könnte** — weder Richtung noch Größenordnung sind vorhersagbar.

**Und bei zweien fehlte nicht die Messung, sondern der Weg von der Messung in die Datei.** `Project`
140 ist seit dem **28.07.2026** gezählt, `Process` 1.503 seit dem **01.08.2026** — beide standen
benannt in `messungen-schritt4.md`, die 140 dort sogar ausdrücklich neben dem Hinweis, dass
`PROJEKTBESCHREIBUNG.md` §8 etwas anderes nennt, und die 1.503 zusätzlich dreimal in der
Feature-Datei `prozessauswahl.md`. **Fünfzehn** beziehungsweise **elf** Tage lang hat das niemand
nachgezogen. Das ist eine andere Fehlerart als die vier oben und gehört getrennt gelesen: **Dort
fehlte eine Zahl, hier ein Arbeitsschritt.**

> **Hier wird daraus keine Regel formuliert.** Ob dem Muster eine Vorschrift folgen soll — etwa eine
> Pflicht, jede Zahl mit ihrer Herkunft zu führen, oder eine Erweiterung von Regel L8 um die
> Zeilenzahl —, ist eine Entscheidung des Auftraggebers und gehört ins Sparring. Festgehalten ist
> der Befund. Die Frage zu L8 steht in
> [`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §4.4 ausdrücklich als offene Frage.

### Was unberührt bleibt

Kurz, damit beim nächsten Lesen niemand danach sucht:

- **Regel L4** (`MessageProperty` nur über `MessageID`) — sie ruht auf der **Bytegröße**, und die
  höhere Zeilenzahl macht sie **strenger**, nicht milder.
- **Die 82 Prozent** — sie rechnen mit Bytes.
- **Regel L5** (BAM-Suche mit Limit und Mindestlänge) — durch M33 erstmals **gemessen unterlegt**:
  Maximum 234.159 Treffer auf einen einzigen Wert.
- **Annahme A9** (Rückfalloption eigener BAM-Index) — durch M33/M34 **erledigt**, sie wird nicht
  gebraucht.
- **Keine der sieben Leistungsregeln aus §8 ändert sich.** Nur die Zahlen darunter.
- **Alle Bytegrößen** — sie stammen aus belegten Seiten und sind über fünf Messtage (27.07., 07.08.,
  10.08., 11.08., 12.08.2026) byteidentisch geblieben.

### Was noch offen ist

- **`mandantentrennung.md` §2 nennt weiterhin „142 Projekten".** Gezählt sind 140. Die Korrektur ist
  hier **absichtlich nicht** vorgenommen worden: Der Auftrag zu dieser Runde benennt seine Dateien
  einzeln, und diese gehört nicht dazu. Sie steht hier, damit die Zahl nicht verlorengeht.
- **Die Vollkopie-Annahme ist nie geprüft.** Jede Zahl dieses Abschnitts ist auf der **Testkopie**
  gemessen, Datenstand 08.07.2026, und wird als Aussage über die **Produktion** geführt. Es besteht
  kein Zugang dorthin. Belegvermerk zu M44.

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
