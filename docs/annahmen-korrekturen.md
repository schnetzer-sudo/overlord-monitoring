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

> **Nachgezogen 21.08.2026 (E37).** Der Satz „**`Message` hat sechs Indizes**, `datenmodell.md` §3
> nennt drei" ist an **beiden** Hälften überholt, und zwar aus zwei verschiedenen Gründen — beide
> Fälle liegen hier gleichzeitig vor:
>
> - **„sechs Indizes" ist ein überholter Zählstand.** `Message` trägt **acht**: sieben plus
>   `PRIMARY`. Dieselbe Sechs steht im Summensatz von
>   [`messungen-schritt4.md`](messungen-schritt4.md), Auffälligkeit **F** — die vier Punkte dieser
>   Liste fassen die dortigen Auffälligkeiten C, D, F und G zusammen —, und dieser Summensatz
>   widerspricht der Aufzählung in seinem eigenen Absatz. Die Indextabelle derselben Messung
>   (**M1**, 01.08.2026) führt alle acht. Der Kasten dazu steht dort.
> - **„`datenmodell.md` §3 nennt drei" ist kein Zählstand mehr, sondern ein falsch gewordener
>   Verweis.** §3 nennt seit dem **20.08.2026** acht, mit Namen und Spalten. Der Satz beschreibt
>   eine Abweichung, die es nicht mehr gibt — sie lag vom 01.08.2026 bis zum 20.08.2026 offen.
>
> **Verbindlich ist ab jetzt keine der beiden Prosafassungen**, sondern die Sollliste in
> [`backend/src/test/resources/indizes-sollliste.txt`](../backend/src/test/resources/indizes-sollliste.txt),
> bewacht von **`IndexbestandDbIT`**. Der Eintrag oben bleibt stehen: Er ist der Ort, an dem die
> Abweichung zuerst festgehalten wurde, und er belegt, wie lange sie offen lag.

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

---

## Erhebung 13.08.2026 (Schritt 7, Teil 2a)

Vollständig in [`messungen-schritt7.md`](messungen-schritt7.md) **M46**. Hier steht nur, was sich
dadurch an einer bestehenden Annahme ändert — und das ist im Kern **eine**, dafür eine tragende.

### Die Sollänge ist keine Eigenschaft des Typs, sondern des Paares aus Mandant und Typ (M46‑2)

**Die Annahme.** M43 misst die Sollänge **je Typ** und formuliert die Kuratierung durchgängig
typweise („eine Sollänge je Typ, die die Suche dauerhaft auffüllt", Belegvermerk zu M43). Nichts
darin ist falsch — die Frage nach dem Mandanten war schlicht **nicht gestellt**.

**Die Messung.** M46‑2 erhebt die dominante Länge je (Mandant, Typ) über den Bestand, für alle 36
Typen mit führender Null: **45 Paare**. Drei davon widersprechen der Typannahme, und zwar auf drei
verschiedene Weisen:

| | Typ | Befund | was ein typweiter Schlüssel getan hätte |
|---|---:|---|---|
| **die Länge weicht ab** | 2000 | `SUTTONS` Länge **6** (97,33 %), `VOTG` Länge **7** (84,06 %) | eine Sollänge von 6 für `VOTG` gesetzt — falsch |
| **die Entscheidung weicht ab** | 9014 | Bestand 58,45 %, `WOC` **95,21 %** bei 79,15 % führender Null | den Eintrag verworfen, der für `WOC` der nützlichste ist |
| **die Grundlage weicht ab** | 2001 | `VOTG` 100 % Dominanz, aber **null** Werte mit führender Null | über die Gesamtdominanz (79,12 %) entschieden und die 8 Zeilen nie gesehen |

**Die Konsequenz.** `overlord_monitor.bam_sollaenge` ist nach `(mandant_id, message_bam_type)`
geschlüsselt. Das ist der teurere Schnitt und der einzige, der alle drei Fälle trifft.

> **Warum das hier steht und nicht nur in der Feature-Datei.** Es ist **dieselbe Fehlerart wie bei
> `bam_spalte`** — dort hatte Schritt 4 angenommen, `MessageBAMMandant.MessageBAMTypeSortIndex` wähle
> die richtigen Spalten, und für sechs von sieben Mandanten stimmte das auch. Zweimal in Folge hat
> eine kuratierte Eigenschaft, die als typ- oder systemweit gedacht war, **je Mandant** anders
> ausgesehen. Beide Male ist es aufgefallen, weil jemand nachgemessen hat statt es zu übernehmen.

### Was diese Erhebung **nicht** ändert

- **Keine Zahl aus M32 bis M45 ist angefasst worden.** Die sechs Bestandszeilen aus M43‑1
  reproduzieren Zeile für Zeile (2001 mit 67,21 %, 9036 mit 98,94 %).
- **Die Bytegrößen sind zum sechsten Mal byteidentisch** (27.07., 07.08., 10.08., 11.08., 12.08. und
  13.08.2026). Die Testkopie ist seit dem 07.08.2026 nicht neu befüllt worden.
- **Regel L5** bleibt unverändert. Die Mindestlänge, die sie fordert, ist weiterhin je Typ zu
  bemessen (E6, M38) — M46 sagt dazu nichts Neues.

### Neu offen

- **Ob die 95-Prozent-Regel um eine Wirksamkeitsbedingung ergänzt wird.** Zwei der vierzehn
  kuratierten Paare können nachweislich nichts finden (`IBIS`/1, `SUTTONS`/2000), weil kein Wert
  **mit** führender Null auf der Sollänge liegt. Die Zeilen stehen mechanisch da; die Ergänzung wäre
  eine Regeländerung und ist **nicht** getroffen worden.
- **Ob 9006 eine benannte Ausnahme bekommt.** Die seit Schritt 4 kuratierte Lieferschein-Nr. fällt
  mit **94,21 %** durch — 0,79 Prozentpunkte unter der Schwelle, und ausgerechnet für sie belegt
  M43‑4 die Wirkung des Auffüllens an echten Werten.
- **Eine Sicherungsregel für handkuratierte Daten gab es nicht.** Geprüft über `docs/`, das
  Wurzelverzeichnis und die Migrationen. Sie ist mit Teil 2a in
  [`bam-sollaengen.md`](bam-sollaengen.md) §7.2 **angelegt** worden und führt von Anfang an auch
  `process_catalog` — die Tabelle existiert noch nicht. *Korrigiert 20.08.2026:* Hier stand
  „`process_catalog` und `partner` — beide existieren noch nicht“. Die Tabelle `partner` entfällt
  (E23); die Auswahlliste wird über `SELECT DISTINCT` aus den Katalogzeilen abgeleitet.

---

## Korrektur 14.08.2026 (Schritt 7 — das Komma in der Parameterbindung)

**Dies ist kein Befund über das Quellsystem, sondern ein Irrtum dieses Projekts.** Er steht hier,
weil diese Datei die Stelle ist, an der das Projekt seine Irrtümer führt — und weil er sonst als
eine Zeile im Änderungsverlauf verschwände.

### Der Vermerk

**Ein als fertig gemeldeter Pfad konnte 0,363 % der Werte nicht finden, und es fiel erst beim Bau
eines Tests auf, der eine andere Frage stellte.**

Die BAM-Suche über den Wert gilt seit **Schritt 7, Teil 2b** als fertig: gebaut, getestet, in **M47**
gemessen, dokumentiert. Sie hatte einen Defekt in der **Annahme des Parameters** — Spring zerlegt
einen einzeln gesetzten `@RequestParam` am **Komma**, ein BAM-Wert mit Komma zerfiel damit in zwei
Begriffe, von denen der zweite keinen Pflichttrenner mehr trug, und die Antwort war
`400 suchbegriff-ohne-typtrenner`. **In beiden Suchmodi**, weil der Fehler vor dem Modus sitzt.

**Gefunden wurde er am 14.08.2026 beim Bau des Isolationstests zu Teil 4.** Dessen Herleitung nimmt
die längsten `NEXANS`-Werte im Fenster; **alle 25 Kandidaten trugen ein Komma**, und der Endpunkt
antwortete auf jeden einzelnen mit `400`. Niemand hatte danach gesucht.

### Wie groß er war — und warum die Zahl allein in die Irre führt

| | Zahl | Quelle |
|---|---:|---|
| Werte im Bestand mit Komma | **55.989** (0,363 %) | M50‑5 |
| davon **Typ 9003** `Material-Nr. beim Lieferanten_L_SAP` | **54.096** — 96,62 % | M51‑2 |
| Anteil **innerhalb** von Typ 9003 | **5,05 %** | M51‑3 |
| betroffene Typen von 62 | **3** (9003, 9016, 9018) | M51‑2 |
| betroffene Mandanten von 7 | **1** (`NEXANS`) | M51‑3 |

**„0,363 % des Bestands" liest sich wie gleichmäßiges Rauschen. Es war keines.** Der Defekt saß bei
**einem** Mandanten auf **jedem zwanzigsten** Wert eines seiner Anzeigetypen. Genau deshalb ist er
der Herleitung eines Isolationstests aufgefallen und keiner Suche: Die Herleitung sortierte nach
Länge, und die längsten Werte dieses Mandanten sind ausgerechnet die kommahaltigen.

### Was daran zu lernen ist — und was nicht

**Die drei Prüfnetze haben ihn alle nicht gefangen, jedes aus einem eigenen Grund:**

| Netz | warum es ihn nicht sah |
|---|---|
| Einheitstests | Sie beginnen bei `BamSuchfilter.aus` — also **hinter** der Bindung. Was zwischen HTTP und Fachlogik passiert, prüfte keiner |
| `BamSucheDbIT` | Die Prüfwerte werden nach **Gestalt** hergeleitet (führende Null, Sollänge) — eine Gestalt, die Kommas weder verlangt noch ausschließt. Es war Zufall, dass keiner erwischt wurde |
| Die Messungen | M47 misst **Laufzeiten** des gerenderten Statements. Ein Wert, der den Endpunkt gar nicht erreicht, taucht dort nicht auf |

**Nicht zu lernen ist daraus, dass mehr Tests nötig gewesen wären.** Die Lücke war eine der
**Schicht**, nicht der Menge: Für die Annahme der Parameter gab es keinen einzigen Test, der ohne
Datenbank lief — und die Tests, die sie berührten, tragen `@Tag("db")` und laufen in der CI nicht.
Geschlossen ist die Lücke mit `BamSucheKommabindungTest` **für diesen einen Endpunkt**. Ob dieselbe
Sorte Prüfung für die übrigen entsteht, ist eine offene Frage
([`messungen-schritt7.md`](messungen-schritt7.md), Punkt 27).

### Was korrigiert worden ist

- **Der Code:** `BamSucheController` bindet `begriff` über einen controller-eigenen `@InitBinder`.
  **Nur dieser Endpunkt** — die Nachrichtenliste trennt `status` und `prozess` weiterhin am Komma,
  wo es heute gemessen folgenlos ist (M51‑4: 0 von 1.503 `ProcessID` tragen eines).
- **Der Test:** `BamSucheDbIT.ein_komma_im_wert_ist_heute_400` ist **umgedreht und umbenannt** in
  `ein_komma_im_wert_wird_gefunden` — nicht gelöscht. Der alte Name behauptete den Ist-Zustand als
  Sollzustand.
- **Die Dokumentation:** [`bam-suche.md`](bam-suche.md) §22 trägt Befund, Lebensdauer, Behebung und
  Schnitt; §9 Punkt 11 ist als geschlossen gekennzeichnet, mit dem alten Text durchgestrichen
  daneben.

### Was diese Korrektur **nicht** ändert

- **Keine Zahl aus M32 bis M50 ist angefasst worden.** M51‑1 reproduziert die Kommazählung aus
  M50‑5 auf die Zeile (55.989) und die Zeilenzahl aus M33 (15.406.350).
- **Die Bytegrößen sind zum zehnten Mal byteidentisch.** Die Testkopie ist seit dem 07.08.2026 nicht
  neu befüllt worden.
- **Am Statement der Suche ändert sich nichts.** Das Komma ist weder in `=` noch in `LIKE` ein
  Platzhalter; maskiert werden weiterhin nur `\`, `%` und `_`. Geprüft in
  `BamSucheStatementsTest`, nicht angenommen.
- **`PROJEKTBESCHREIBUNG.md` ist nicht angefasst.**

### Neu offen

- **Welche Zeichen sonst noch zerlegt werden.** Untersucht sind zwei: der Doppelpunkt (M49‑4,
  entschärft durch Teilung am ersten Vorkommen) und das Komma (M50‑5, M51, behoben). `;`, `|`, ein
  rohes `+` in einer URL, ein Zeilenumbruch aus der Zwischenablage — ungeprüft.
- **Wie oft solche Werte gesucht werden.** Nicht erhoben und in diesem Projekt auch nicht erhebbar:
  Es führt kein Suchprotokoll. **Der Satz „der Defekt war selten im Weg" ist ein Schluss aus der
  Datenlage und keine Beobachtung** — dieselbe benannte Lücke wie bei den 585 Doppelpunkten.

---

## Vorfall 18.08.2026 (Schritt 8 — der schreibende Prüfagent)

**Dies ist kein Befund über das Quellsystem, sondern ein Vorfall dieses Projekts.** Er steht hier aus
demselben Grund wie die Korrektur zum Komma: weil diese Datei die Stelle ist, an der das Projekt seine
Irrtümer führt — und weil er sonst gar nirgends stünde. Ein zurückgesetzter Schreibvorgang hinterlässt
nicht einmal eine Zeile im Änderungsverlauf.

### Der Vermerk

**Ein Lauf, der prüfen sollte, hat die geprüfte Datei geändert — und zwar die Datei, in der die
Entscheidungen stehen.**

In der Nachbesserung zu Schritt 8 am **18.08.2026** hat ein Prüfagent in den Arbeitsbaum geschrieben.
Betroffen war `frontend/src/features/nachrichten/rohdaten.ts`, und zwar an zwei Stellen: Er hat **den
Anzeigevermerk beschnitten** und **`zieleJeSchritt` umgeschrieben**. Der Lauf wurde gestoppt, beide
Änderungen wurden zurückgesetzt. Der Baum steht heute unverändert auf `8400952`.

**Drei Angaben zum Vorgang fehlen und werden nach Regel Q4 als Lücke benannt statt ergänzt.** Der
Auftrag zum Dokumentationslauf vom 19.08.2026 hat sie zum Eintragen vorgesehen und leer gelassen:

| # | Offene Angabe | Warum sie hier nicht steht |
|---|---|---|
| (a) | Womit ist „der Baum ist geprüft sauber" belegt? | Nicht eingetragen. Aus dem Baum ist er **nachträglich** nicht mehr herzuleiten: Ein zurückgesetzter Schreibvorgang und ein Lauf, der nie geschrieben hat, sehen hinterher gleich aus — genau darum verlangt S3 den Nachweis **während** des Laufs |
| (b) | Hat der Prüfagent **vor** oder **nach** seinen 39 Befunden geschrieben? | Nicht eingetragen. Die Antwort entscheidet, ob die 39 Befunde gegen den unveränderten oder gegen den eigenen Baum entstanden sind — also ob Satz 3 von S3 auf sie zutrifft. **Ohne sie sind sie nach Satz 3 als gegen einen veränderten Baum entstanden zu behandeln** |
| (c) | Stammen die 336 Testfälle aus der Runner-Ausgabe? | Nicht eingetragen. Nachträglich prüfbar ist nur, ob die Zahl **heute** reproduziert (sie tut es, siehe unten) — nicht, woher sie am 18.08.2026 kam |

### Warum er schwerer wiegt als jeder der 39 Befunde derselben Runde

**Geschrieben wurde in `rohdaten.ts`** — der Datei, die laut
[`rohdaten-frontend.md`](rohdaten-frontend.md) §12 „**die Entscheidungen als reine Funktionen**" trägt:
Beschriftung, Ziele je Schritt, Gleichlauf, Vermerke, Pfade. Das ist keine Hilfsdatei und kein Markup.

`zieleJeSchritt` umzuschreiben heißt damit, **eine Entscheidung zu ändern, ohne dass eine Entscheidung
dahintersteht** — genau das, was dieses Projekt als stille Entscheidung ausschließt. Die Funktion setzt
Entscheidung 6 in ihrer Fassung vom 18.08.2026 um ([`rohdaten.md`](rohdaten.md) §3): je Schritt die
Artefakte, die auf ihm liegen, kein Ziel wo nichts liegt, nichts abgeschnitten wo mehr liegt. Wer sie
umschreibt, verschiebt diese Fassung — und der Kasten unter §3 zeigt, was eine solche Verschiebung
kostet, wenn sie unbemerkt bleibt: Entscheidung 6 ist in ihrer **ersten** Fassung an M57 vorbeigelaufen
und erst im gebauten Zustand als Fehler sichtbar geworden.

**Dass es aufgefallen ist, liegt am Zeitpunkt und nicht an einem Mechanismus.** Kein Test, kein Hook und
keine Regel hat den Schreibvorgang gemeldet; er fiel auf, weil zu diesem Zeitpunkt jemand hinsah. Genau
diese Lücke schließt **S3** ([`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §4.2) — und
sie schließt sie als Regel und nicht als Mechanismus, was hier ausdrücklich festgehalten wird: Ein
Prüflauf, der die Regel bricht, wird weiterhin nur dann bemerkt, wenn der Nachweis nach Satz 2
eingefordert wird.

### Der zweite Vorfall derselben Runde: die Testzahl-Drift

`tests/ansicht-umschalter.test.tsx` trug **seit Schritt 6 zwei Fälle**, gezählt war **einer** —
berichtigt am 18.08.2026 im Kopf von `frontend/vitest.config.mts`, wo die Zahl der gerenderten Fälle an
genau einer Stelle geführt wird.

**Das ist dieselbe Klasse wie `information_schema.TABLE_ROWS`:** eine Zahl **fortgeschrieben statt
gezählt**. Bei `MessageBAM` waren es 41,9 % Abweichung (M33‑0), bei `MessageProperty` 60,9 % (M44); hier
war die geführte Summe **16, gezählt 17** — ein Fall von siebzehn, 5,9 %. Die Größe ist verschieden, der
Fehler ist derselbe, und er fällt in allen drei Fällen erst auf, wenn jemand nachzählt.

> **Belegvermerk (L10).** *Gemessen war:* die Fassung des Kopfes von `frontend/vitest.config.mts` im
> Commit `4210b94` (18.08.2026) — dort „**sechzehn in fünf Dateien**", mit
> `tests/ansicht-umschalter.test.tsx` bei **1**, während die Datei zwei Fälle trug (3 + 1 + 4 + 4 + 4 =
> 16 geführt gegen 3 + 2 + 4 + 4 + 4 = 17 tatsächlich). *Behauptet wird:* dass die Zahl
> **fortgeschrieben** und nicht gezählt wurde. Das ist der Schluss aus der Abweichung und keine
> Beobachtung des Vorgangs — belegt ist die Abweichung, nicht ihre Entstehung.

**Zu berichten, nicht zu beheben, war die Frage: Stammen die gemeldeten 336 Fälle aus der Ausgabe des
Testrunners oder aus einer laufenden Summe?** Antwort, soweit sie aus dem Baum zu holen ist:

| Was geprüft wurde | Ergebnis |
|---|---|
| `pnpm test` (`vitest run`) am **19.08.2026** gegen `8400952` | `Test Files 19 passed (19)` · `Tests 336 passed (336)` — **beide Zahlen reproduzieren wortgleich** die Angabe in [`rohdaten-frontend.md`](rohdaten-frontend.md) §9 |
| Die sieben Einzelzahlen im Kopf von `frontend/vitest.config.mts`, je Datei einzeln gefahren | 2 · 9 · 4 · 3 · 4 · 4 · 8 — **alle sieben stimmen**, Summe **34**, wie dort geführt |
| Eine statische Zählung der `it(`/`test(`-Aufrufe | **243** und damit unbrauchbar als Beleg: `it.each` erzeugt mehrere Fälle je Aufruf, sichtbar an `artefakt-ansicht.test.tsx` (6 Aufrufe, 9 Fälle) |

> **Belegvermerk (L10).** *Gemessen war:* dass die beiden Zahlen und die sieben Einzelzahlen **heute**
> gegen `8400952` reproduzieren. *Behauptet wird damit nicht,* dass sie am 18.08.2026 aus der
> Runner-Ausgabe stammten — das ist Frage (b)/(c) oben und bleibt offen. **Die Reproduktion belegt den
> Zustand, nicht die Herkunft.** Sie schließt allerdings die Drift für den heutigen Stand aus: Wäre die
> Summe fortgeschrieben, müsste sie danebenliegen, und sie tut es an keiner der acht geprüften Stellen.

**Und der Satz, der dazugehört:** Stammte die Zahl aus einer Summe, wäre die Korrektur an *einer* Datei
kein Beleg für die übrigen achtzehn. Nach der Messung oben ist dieser Fall für den heutigen Stand
ausgeschlossen — für den Stand vom 18.08.2026 ist er es **nicht**.

### Was daran zu lernen ist — und was nicht

**Nicht zu lernen ist daraus, dass Prüfläufe weniger dürfen sollten.** Ein Prüflauf, der den Baum liest,
Tests fährt und Befunde schreibt, ist genau das Werkzeug, das den Kommadefekt gefunden hat. Die Lücke
war nicht seine Reichweite, sondern die **fehlende Trennung von Finden und Beheben** — und die zweite
Lücke war, dass niemand einen Nachweis verlangt hat.

**Die beiden Vorfälle haben dieselbe Gestalt, und das ist der Grund, warum sie in einem Eintrag stehen:**
In beiden Fällen ist etwas **behauptet statt belegt** worden — einmal ein sauberer Baum, einmal eine
Fallzahl. Dieses Projekt hat für die zweite Sorte schon eine Regel (L10) und hat für die erste jetzt
eine (S3).

### Was dieser Vorfall **nicht** ändert

- **Keine Zeile Code ist deswegen angefasst worden.** Weder `rohdaten.ts` noch ein Test noch ein
  Endpunkt; die beiden Änderungen des Prüfagenten sind zurückgesetzt und nicht ersetzt worden.
- **Keine Zahl aus M52 bis M72 ist angefasst worden**, und keine Messung ist wiederholt worden.
- **Die 39 Befunde derselben Runde sind hier weder übernommen noch verworfen.** Ohne Angabe (b) ist
  nicht bekannt, ob sie vor oder nach dem Schreibvorgang entstanden sind; nach S3 Satz 3 sind sie damit
  vor einer Übernahme neu zu erheben. **Welche es waren, steht in keiner Datei dieses Projekts.**
- **Kein offener Punkt aus [`rohdaten-frontend.md`](rohdaten-frontend.md) §11 oder
  [`rohdaten-backend.md`](rohdaten-backend.md) §11 ist entschieden.**
- **Die Sichtprüfung zu Schritt 8 ist nicht nachgeholt** und gilt weiter als ausstehend.

### Neu offen

- **Die drei Angaben (a), (b) und (c)** aus der Tabelle oben. (a) und (b) sind nur vom Auftraggeber zu
  beantworten; (c) ist für den heutigen Stand gemessen und für den 18.08.2026 offen.
- **Ob die 39 Befunde neu erhoben werden.** Sie sind in keiner Datei geführt. Ohne eine Liste ist Satz 3
  von S3 auf sie nicht anwendbar, weil der Gegenstand fehlt.
- **Ob S3 einen Mechanismus bekommt.** Heute ist die Regel eine Regel. Ob ein Hook oder ein Schritt im
  Build den Nachweis nach Satz 2 erzwingt, ist eine Entscheidung des Auftraggebers und hier **nicht**
  getroffen.
- **Ob die übrigen achtzehn Testdateien ihre Fallzahlen einzeln führen.** Heute führt nur der Kopf von
  `frontend/vitest.config.mts` Zahlen, und nur für die sieben rendernden Dateien. Für die übrigen gibt
  es keine geführte Zahl — und damit auch keine, die driften könnte.

---

## Korrektur 19.08.2026 (Schritt 8 — `Message.Payload.GUID` ist nicht die eingegangene Datei)

*Eingetragen am 20.08.2026, nachdem die Korrektur gefahren war.*

**Diese Annahme ist nie durch dieses Verzeichnis gegangen.** Die Korrekturen oben gehen auf
Aussagen zurück, die irgendwo als Annahme oder als dokumentierte Zahl standen — A6, A7, A8, das
Mengengerüst, die Zeilenzahl aus `information_schema`. Diese hier ist am **18.08.2026** unmittelbar
als **Tatsache** in zwei verbindliche Feature-Dateien geschrieben worden, ohne Messung und ohne
Belegvermerk. Sie hat nie den Zustand „Annahme" gehabt, den man hätte prüfen können — und genau
deshalb gehört sie hierher.

### Der Vermerk

**`Message.Payload.GUID` benennt kein eigenes Artefakt.** Er trägt in **6.249 von 6.249** Nachrichten
(Fenster A) und **214.330 von 214.330** (Fenster B) denselben Verweis wie die Nutzdatenzeile mit dem
**höchsten `MessageActionID`** derselben Nachricht — **kein Gegenfall** in beiden Fenstern. Der
Verweis hat die Form `<Ablagenkennung>|<UUID>` und ist durchgängig so gebaut
(M54); zwei gleiche Verweise heißen **dieselbe Datei**, nicht zwei ähnliche. Die Frage war damit ohne
einen einzigen Filestore-Abruf entscheidbar — ein Zeichenkettenvergleich innerhalb einer Nachricht.

Vollständig mit Statements, `EXPLAIN` und Laufzeiten in
[`messungen-schritt8.md`](messungen-schritt8.md) unter **M73**.

> **Belegvermerk (Regel L10).**
>
> *Gemessen ist:* dass der Verweis mit dem der Nutzdatenzeile auf dem **höchsten `MessageActionID`**
> übereinstimmt — je Nachricht, in zwei Fenstern, zweimal gerechnet (Spaltenkollation und `BINARY`,
> in jeder Zeile übereinstimmend).
>
> *Behauptet wird:* dass der Name deshalb keine eigene Datei benennt und aus der Artefaktliste
> gehört.
>
> *Ausdrücklich **nicht** behauptet:* dass „höchster `MessageActionID`" gleichbedeutend mit
> „zeitlich zuletzt" ist. Gemessen ist die **Schrittnummer**, nicht die Uhr. Die Deutung „die
> zuletzt erzeugte Datei" ist plausibel und ungemessen; sie steht in keiner Beschriftung, keinem
> Feldnamen und keinem Kommentar — und wo sie versehentlich stand ([`README.md`](README.md), Zeile
> zu `messungen-schritt8.md`), ist sie am 19.08.2026 herausgenommen worden.

### Woher der Fehler kam — und warum er zwei Schichten tief saß

**Aus dem Namen.** `Message.` plus `Payload` liest sich wie „die Nutzdatei *der* Nachricht". Die
Ableitung ist so naheliegend, dass sie nie als Ableitung aufgefallen ist — sie stand am 18.08.2026
in [`rohdaten.md`](rohdaten.md) §5 und [`rohdaten-frontend.md`](rohdaten-frontend.md) §2 ohne
Fundstelle, und **M57 hatte gemessen, welche Namen auf Schritt `0` liegen — nicht, worauf ihre
Verweise zeigen.**

**Die Annahme hat sich selbst gedeckt.** `rohdaten-frontend.md` §2 begründete sie mit dem Backend
(„Das Backend liefert ihn in einem eigenen Feld der Antwort"), und das Backend-Feld hieß `eingang`,
weil der Name das nahelegte. Ein Kreis aus zwei Schichten, in dem keine Messung vorkommt. Das ist
die eigentliche Lehre dieses Eintrags: **Eine Vermutung, die durch zwei Schichten wandert, sieht am
Ende aus wie ein Befund.**

**Aufgefallen ist sie an der Oberfläche und nicht in einer Messung.** In der Eingangszeile der
Zeitleiste hingen **drei** Ziele; der Auftraggeber hat sie am 19.08.2026 geöffnet und zugeordnet,
und das dritte war die Ausgangsdatei, die die Leiste am letzten Schritt bereits führte. Gemessen
bestätigt: `ohne_treffer = 0` in beiden Fenstern (M73, Befund 4). **Dass es aufgefallen ist, liegt
daran, dass jemand hingesehen hat** — wie beim Vorfall vom 18.08.2026 einen Eintrag weiter oben.

**Der vorregistrierte Ausgang war G, nicht A.** M73 hatte fünf Ausgänge vorab benannt und die
95-Prozent-Schwelle festgeschrieben. Keiner der vorformulierten Ausgänge hat sie erreicht: Die
Treffer zerfallen über die Sendedienste (**70,09 %** / **53,37 %**), den Converter (**29,89 %** /
**46,62 %**) und die Lesedienste (**0,016 %** / **0,015 %**). Der Befund ist nicht *welche Familie*,
sondern **welche Stelle im Ablauf** — und dafür gab es keine vorformulierte Zeile.

### Der zweite Befund derselben Runde: der falsche Dateiname

**Der Download von `0-Message.Payload.GUID` hat die falsche Datei unter dem falschen Namen
ausgeliefert.** `Downloaddateiname` sucht den Originalnamen über
`%.FileProperty.OriginalFilename` **auf demselben Schritt**; für Schritt `0` fand er
`FileReader`/`FTPReader.FileProperty.OriginalFilename`, also den Namen der **eingegangenen** Datei —
während der Inhalt dahinter nach M73 die Datei des höchsten Nutzdatenschritts war.

Die Begründung dieser Suche formuliert den verletzten Grundsatz selbst: *„Der Originalname
beschreibt die Datei, die eingegangen ist. Für ein `Converter.Payload.GUID` auf Schritt 2 ist das
eine andere Datei; ihm den Namen des Eingangs zu geben wäre eine Falschauskunft."* Genau das ist auf
dem Umweg über Schritt `0` geschehen. Wer diese Datei herunterlud, **hätte** den Stand eines
späteren Schritts unter dem Namen seines eingegangenen Belegs gespeichert — in der Mehrheit den
eines Sendedienstes (**70,09 %** / **53,37 %**), sonst den des Converters (**29,89 %** /
**46,62 %**), M73. In einem Werkzeug, dessen Zweck „wo ist mein Beleg" ist, die teuerste Sorte
Fehler.

> **Belegvermerk (L10).** *Gemessen ist:* worauf der Verweis zeigt und über welches Muster der
> Dateiname gesucht wird. *Behauptet wird:* dass der Download damit die falsche Datei unter dem
> falschen Namen ausgeliefert **hätte**. *Nicht behauptet wird, dass es geschehen ist* — das
> Feature war einen Tag alt, ein Zugriffs- oder Downloadprotokoll ist dazu nicht ausgewertet
> worden, und die Sichtprüfung zu Schritt 8 steht ohnehin noch aus. Der Satz ist ein hergeleiteter
> Folgefall und keine Beobachtung; die Datei rügt genau diese Verwechslung im Eintrag vom
> 14.08.2026 an sich selbst.

**Der Weg dorthin ist mit dem Wegfall des Artefakts geschlossen und stand einen Tag lang offen.**
Der Befund steht hier, weil er sonst mit der Zeile verschwunden wäre, die ihn getragen hat.

### Ein dritter Fund am Rand: eine Addition, die als Messung gelesen wurde

Bei derselben Prüfung ist aufgefallen, dass der Kommentar an der Sortierung in
`ArtefaktRepository.findeOriginaldateiname` behauptete, `FileReader` und `FTPReader` schlössen
einander aus, und sich dafür auf **M56 (a)** berief. M56 (a) zählt **je `MessagePropertyName`** und
misst kein `DISTINCT` über beide; die 71,4 % in Befund 1 sind eine **Addition** der beiden Zeilen.
Der Kommentar ist berichtigt, das Verhalten nicht — die feste Sortierung nach Namen deckt den Fall
ohnehin ab. **Dieselbe Gestalt wie die Hauptkorrektur:** aus zwei Zahlen ein Schluss, der nie
gemessen wurde.

### Was korrigiert worden ist

| Ebene | Änderung |
|---|---|
| **Backend** | `Message.Payload.GUID` fällt aus der Artefaktliste. Die Ausnahme steht im **Code** (`Artefaktnamen.NAME_ZEIGER` / `istZeiger` / `istArtefakt`, angewandt in `ArtefaktRepository.findeArtefakte`) und **nicht im Statement** — so ändert sich am Statement nichts, der gemessene Zugriffsweg `mp` über `PRIMARY` bleibt belegt, und es entsteht keine neue L7-Pflicht |
| **Schnittstelle** | `ArtefaktlisteResponse` verliert das Feld `eingang`; die Antwort ist zweigeteilt. Ein Feld dieses Namens, das den Ausgang trägt, wäre dieselbe Falschauskunft eine Schicht tiefer — und der Chatbot der Ausbaustufe 1 greift laut [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §10 auf **dieselben** Endpunkte zu |
| **Oberfläche** | Die Beschriftungslage *Eingang → Eingegangene Datei* entfällt; aus fünf Lagen werden vier. Die Zeile über der Leiste heißt weiterhin *Eingang* und trägt zwei Ziele statt dreier |
| **Alte Kennungen** | **Kein Umleitungspfad.** `0-Message.Payload.GUID` ergibt `404` wie jede unbekannte Kennung. Das Feature war einen Tag alt; das ist kein Sonderpfad, sondern das Ausbleiben eines Sonderpfads |
| **Dokumentation** | Neun Dateien mit datierten Korrekturkästen — [`rohdaten.md`](rohdaten.md), [`rohdaten-frontend.md`](rohdaten-frontend.md), [`rohdaten-backend.md`](rohdaten-backend.md), [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) und [`README.md`](README.md) am 19.08.2026; [`nachrichtendetail.md`](nachrichtendetail.md), [`datenmodell.md`](datenmodell.md), [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) und [`frontend-grundlagen.md`](frontend-grundlagen.md) am 20.08.2026. **Jede falsche Aussage steht wörtlich weiter da** |

**Die tückischste Fundstelle war [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md).**
Der Satz „die eingegangene Datei einzeln" ist dort **am 19.08.2026 neu geschrieben** worden — im
selben Zug, mit dem der Abschnitt vom reinen Download auf die Anzeige gezogen wurde. Die widerlegte
Annahme hat also nicht überlebt, sondern ist an dem Tag **neu eingetragen** worden, an dem sie fiel.

### Was diese Korrektur **nicht** ändert

- **Keine neue Messung.** M73 ist gefahren; keine Zahl ist neu erhoben und keine bestehende
  nachgerechnet worden.
- **Drei Zahlen bleiben deshalb bewusst falsch stehen** — sie rechnen über eine Menge, die die
  entfallene Zeile einschließt, und sie nachzurechnen wäre eine Messung. **Die Richtung ist nicht
  bei allen dieselbe:** M57 führt `Message.Payload.GUID` mit `ohne_schrittnamen = 6.249`, alle
  entfallenen Zeilen liegen also in der Menge *ohne* auflösbaren Schrittnamen. Damit steht
  **55,98 % zu hoch und 44,02 % zu niedrig**; nur die Spanne **3 bis 15** (M55) steht an beiden
  Enden zu hoch. Vermerkt als offener Punkt 18 in
  [`rohdaten-frontend.md`](rohdaten-frontend.md) §11. Die Spanne steht außerdem **unvermerkt im
  Backend** — im Javadoc von `ArtefaktlisteResponse` und in `RohdatenIsolationDbIT`; beide sind am
  20.08.2026 mit einem Vermerk versehen worden. `ArtefaktRepository.findeArtefakte` bleibt richtig,
  weil es die **gelesenen** Zeilen meint und nicht die gelieferten.
- **Kein Endpunkt, keine Migration, kein jOOQ-Statement ist geändert worden.** Der Filter sitzt
  hinter dem Abruf.
- **Die Dateinamenssuche ist nicht umgebaut** — die Regel „Muster statt Familie" bleibt, ihr
  Anlassfall ist entfallen (siehe „Neu offen").
- **Die drei Pflicht-Isolationstests sind unberührt** und laufen unverändert grün. Die
  Mandantentrennung ist von M73 in keiner Weise berührt.
- **Kein offener Punkt aus [`rohdaten.md`](rohdaten.md) §13,
  [`rohdaten-frontend.md`](rohdaten-frontend.md) §11 oder
  [`rohdaten-backend.md`](rohdaten-backend.md) §11 ist entschieden** — dazugekommen sind **vier**
  neue Fragen (Sichtprüfung, Fensterbreite, Muster gegen Familie, die zu hohen Anteilswerte) und
  **zwei** Vermerke über bereits Behobenes (der falsche Dateiname, die Addition aus M56).
- **Die Sichtprüfung zu Schritt 8 ist weiterhin nicht nachgeholt.**

### Neu offen

- **Dass das Paar des Lesedienstes den *Eingang* der Nachricht bezeichnet, ist eine Sichtprüfung an
  einer Nachricht und keine Messung.** Gemessen ist, **welche** Namen auf `MessageActionID = 0`
  liegen (M57) — nicht, was die Dateien dahinter sind. Die Zeile heißt trotzdem weiter *Eingang*,
  mit Belegvermerk nach L10. **Zu entscheiden: messen oder als Sichtbefund führen.**
- **M73 ist in zwei Fenstern gemessen, nicht im Bestand.** Fenster C (`2024-10-01`) ist nicht
  gefahren. Ob die Regel am alten Ende ebenso gilt, kostet **eine einzige Sitzung**.
- **Die Deckungsangabe „220.579 Nachrichten" zählt Fenster A doppelt** *(neu am 20.08.2026)*.
  Fenster A (`>= 2025-12-29`, `< 2025-12-30`; 6.249) liegt **vollständig innerhalb** von Fenster B
  (`>= 2025-11-30`, `< 2025-12-30`; 214.330) — nachzulesen in
  [`messungen-schritt8.md`](messungen-schritt8.md) unter „Die drei Zeitfenster". 6.249 + 214.330
  ist die Zahl der **Prüfvorgänge**, nicht der verschiedenen Nachrichten; verschieden geprüft sind
  **214.330**. Die Summe und die daraus gerechneten **6,6 %** stehen so in der Messdatei selbst
  (M73 Befunde 4 und 5, offener Punkt 32) und von dort in [`rohdaten.md`](rohdaten.md) §13 und
  [`rohdaten-frontend.md`](rohdaten-frontend.md) §11. **Nicht nachgerechnet und nicht geändert** —
  die Messdatei bleibt unangetastet, und die Korrektur der abgeleiteten Stellen gehört in dieselbe
  Runde wie ihre. **Am Befund selbst ändert es nichts:** In *jedem* der beiden Fenster ist die Regel
  für sich ausnahmslos.
- **Ob Muster- und Familiensuche noch irgendwo auseinandergehen.** Der Anlassfall ist entfallen; ein
  anderer bleibt möglich, sobald auf dem Schritt eines Artefakts ein
  `FileReader.`/`FTPReader.FileProperty.OriginalFilename` liegt und das Artefakt einer **anderen**
  Familie angehört. *Gemessen ist,* welche Namen den Originalnamen tragen (M56 a); *nicht gemessen
  ist,* auf welchem `MessageActionID` sie liegen. **Nicht gemessen, nicht geändert, nicht
  entschieden.**
- **Ob die Deutung „zuletzt erzeugt" jemals belegt wird.** Sie wäre die Aussage, dass die
  Schrittnummer die Ausführungsreihenfolge ist. `MessageAction` trägt Zeitstempel; die Frage ist
  messbar und **nicht gestellt worden**.
- **Die Diskrepanz aus M57** — 63 genannte gegen 62 gezählte Kombinationen — bleibt offen. Der
  Befund von M57 ist davon unberührt: Die 55,98 % rechnen mit der Zeilensumme 44.329, und die
  stimmt aufs Zeichen.

---

## Erhebung 21.08.2026 (Schritt 9b — was M83 über die Zahlen sagt, mit denen wir planen)

Zwei Einträge aus dem Nachtrag **M83** ([`messungen-schritt9.md`](messungen-schritt9.md)). Beide
betreffen **nicht** die gemessene Abfrage, sondern die Grundlage, auf der wir Messwerte lesen.
Keiner von beiden ändert eine Zeile Code.

### 1. `CARDINALITY` auf `Message.ProcessID` ist um Faktor 43,7 daneben

**Der Vermerk.** Beide Indizes, die `Message.ProcessID` anführen, stehen auf einer `CARDINALITY`
von **18**. Der Optimierer rechnet daraus `3.560.486 ÷ 18 = 197.804` Zeilen je Nachschlag, und
**genau diese Zahl steht in allen vier `EXPLAIN`-Plänen** von M83. Der wahre Mittelwert ist
**4.528** (3.341.519 Nachrichten auf 738 Prozesse mit Nachrichten, M74b). Die Schätzung liegt damit
um **Faktor 43,7** zu hoch.

> **Drei Zahlen, die nicht verwechselt werden dürfen.** Die **18** ist die `CARDINALITY` des Index,
> also die geschätzte Zahl *verschiedener Werte* — nicht die geschätzte Zeilenzahl. Die geschätzte
> **Zeilenzahl je Nachschlag** ist **197.804**, und ihr steht der wahre Mittelwert **4.528**
> gegenüber. **Faktor 43,7 ist 197.804 ÷ 4.528.** Wer „18 geschätzt gegen 4.528 wahr" schreibt,
> rechnet Faktor 251 und vergleicht zwei Größen verschiedener Art. *Der Fehler ist in einem
> Bauauftrag vom 21.08.2026 aufgetreten und hier benannt, damit er nicht weiterwandert.*

**Hier ist es folgenlos — und das ist Glück, kein Schutz.** Der Plan ist trotz der Fehlschätzung
der richtige, weil `Using index` und der Abbruch des `EXISTS` beim ersten Treffer sie nicht zum
Tragen kommen lassen. Ein Optimierer, der 197.804 Zeilen je Nachschlag erwartet, kann denselben
Plan bei einer **anderen Formulierung** aber verwerfen.

**`ANALYZE TABLE` können wir nicht fahren.** Es ist die Datenbank des Altsystems, `monitor_read`
darf ausschließlich `SELECT`, und geschrieben wird auf `GlassfishDB` unter keinen Umständen (S1).
Die Fehlschätzung bleibt also stehen.

**Was daraus folgt:** **Jede künftige Planwahl, die an dieser Schätzung hängt, ist ein Münzwurf.**
Wer eine Abfrage über `Message.ProcessID` umformuliert und den Plan verliert, findet die Ursache
hier und nicht im eigenen Code.

*Nebenbei, aus derselben Erhebung:* Die 4.528 sind **hergeleitet** (3.341.519 ÷ 738) und nicht am
Plan abgelesen — `ANALYZE SELECT` hätte geschätzte und tatsächliche Zeilenzahlen nebeneinander
gestellt, ist aber nicht gefahren worden, weil der Messrahmen nur `SELECT`, `SET` und `EXPLAIN`
zuließ (M83, Abweichung B).

### 2. Laufzeiten von der Testkopie sind für die Produktion eine optimistische Schranke

**Der Vermerk.** M83‑4 hat den Zustand der Instanz erhoben, gegen die seit Schritt 4 **jede**
Messung dieses Projekts läuft:

| | |
|---|---:|
| `innodb_buffer_pool_size` | **25.600 MiB** |
| `Message` gesamt (Daten + Indizes) | 2.763,9 MiB |
| Puffer je Tabelle | **9,26 ×** |
| logische Leseanfragen seit dem Start | 7.885.714.699 |
| davon physisch von der Platte | 1.951.428 — **0,025 %** |
| Betriebsdauer | 6.825.469 s = **79,0 Tage** |

Der Puffer ist **mehr als neunmal so groß wie die ganze Tabelle**, und die Instanz läuft seit
79 Tagen mit einer Trefferquote von **99,975 %**.

**Was das über unsere Zahlen sagt.** Sie beschreiben eine Instanz, die außer uns **niemand
belastet** und deren Arbeitsmenge vollständig im Speicher liegt. Die Produktion trägt denselben
Bestand **unter laufendem EDI-Verkehr**. Ein „erster Lauf der Sitzung" misst deshalb im
Wesentlichen den kalten Abfrageplan-Cache und die kalte Verbindung — **nicht die kalte Platte**:
Eine Sitzungsgrenze leert keinen Serverpuffer.

**Das gilt für jede M-Nummer, nicht nur für M83.** Es ist keine Einschränkung dieses einen
Nachtrags, sondern der Rahmen, in dem sämtliche Laufzeiten dieses Projekts entstanden sind.

**Was es nicht heißt.** Die Zahlen werden dadurch nicht wertlos — sie sind belastbar für den
**Vergleich** zweier Fassungen (Faktor 216,5 zwischen Fassung A und B bleibt Faktor 216,5) und für
Größenordnungen. Sie sind nur keine Zusage über die Produktion. **`EXPLAIN`-Pläne sind davon
ohnehin unberührt**; sie hängen an Statistik und Schema, nicht am Puffer.

### Was diese Erhebung **nicht** ändert

- **Keine Regel wird gelockert.** Regel L7 („jede neue Abfrage wird vor dem Merge gegen die
  Testkopie gemessen") bleibt unverändert. Sie verlangt eine Messung, nicht eine Produktionszahl —
  und eine optimistische Schranke ist immer noch eine Schranke.
- **Keine Zahl wird nachgerechnet oder zurückgezogen.** Alle bisherigen Laufzeiten stehen
  unverändert; sie bekommen nur diesen Rahmen.
- **Kein Code, kein Statement, keine Migration** ist wegen dieser beiden Einträge geändert worden.

### Neu offen

- **Ob eine der bisherigen Messungen unter Produktionslast anders ausfiele, ist ungemessen und
  von hier aus nicht messbar.** Wir haben keinen Zugang zu einer belasteten Instanz. Der einzige
  vorhandene Anhaltspunkt ist M44s kalt-gegen-warm-Faktor **9,66**, gemessen an einem Vollzugriff
  auf `MessageProperty` — **Anschauung und keine Messung**, und auf andere Statements nicht
  übertragbar.
- **Ob `Message_ProcessFK` und `ProejctIDIDX` wirklich deckungsgleich sind und einer entfallen
  könnte**, ist erhoben (M83‑0), aber **nicht unsere Entscheidung**: Es ist die Datenbank des
  Altsystems. Vermerkt, damit die Beobachtung nicht verlorengeht.

---

## Befund 21.08.2026 (E37 — der Flyway-Stand der Testkopie)

**Kein Befund über das Quellsystem und kein Irrtum eines Dokuments, sondern einer über die
Arbeitsumgebung.** Er steht hier, weil er bei jedem Testlauf auf einem älteren Branch wieder
auftaucht und dann wie ein Fehler aussieht.

### Die Testkopie ist den älteren Branches voraus

Beim Lauf von `IndexbestandDbIT` gegen die Testkopie meldet Flyway:

```
Successfully validated 7 migrations
Current version of schema `overlord_monitor`: 7
Schema `overlord_monitor` has a version (7) that is newer than the latest available migration (6) !
```

`V7__benutzerverwaltung.sql` gehört zu **Schritt 9a** und ist bereits gegen die Testkopie gefahren.
Die Branches davor — dieser hier, `feat/schritt9b-prozess-katalog`, `main` — tragen nur bis `V6`.
Alle teilen sich **dieselbe** Datenbank.

**Was daraus folgt, geprüft:**

- **Der Start scheitert nicht.** Flyway warnt und migriert nicht; die sechs bekannten Einträge
  stimmen, den siebten kennt dieser Branch nur nicht.
- **Die Sollliste ist nicht betroffen.** `V7` ändert zwei Spalten von `app_user` und legt
  **keinen** Index an. Nachgezählt am 21.08.2026: `overlord_monitor` trägt **17** Indizes — genau
  die siebzehn, die [`../backend/src/test/resources/indizes-sollliste.txt`](../backend/src/test/resources/indizes-sollliste.txt)
  führt. `IndexbestandDbIT` ist auf diesem Branch grün, obwohl die Datenbank ihm voraus ist.
- **Was ausdrücklich nicht geprüft ist:** ob ein anderer Integrationstest eines älteren Branches
  über die **Spalten** stolpert. `download_allowed` ist mit `V7` gefallen, und ein Branch vor 9a
  erwartet sie noch. Nicht gemessen, nicht entschieden.

> **Daraus folgt keine Regel.** Eine geteilte Testkopie mit mehreren Branches darauf ist eine
> Arbeitsweise und keine Panne; ein eigenes Schema je Branch wäre teurer als diese Warnung. Der
> Eintrag hält nur fest, dass sie **erwartet** ist — damit sie beim nächsten Mal nicht als Befund
> eines Prüflaufs gedeutet wird.

### Offen — und bewusst hier nicht angefasst

- **Regel R6 in [`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §4.3** — „Die
  Download-Berechtigung ist ein Flag an `app_user`, Standardwert erlaubt … damit ein späterer
  Entzug keine Migration erfordert" — steht unverändert, obwohl `V7` (Entscheidung E18) genau
  diese Spalte fallen lässt. Die Migration nennt `authentifizierung.md` und
  `rohdaten-backend.md` als die Stellen, die den Vertragsbruch tragen; **R6 ist nicht darunter.**
  Der Punkt gehört auf den Branch, der `V7` trägt. **Entschieden am 21.08.2026, ihn hier nicht
  anzufassen:** Eine Korrektur an dieser Stelle beschriebe eine Migration, die es auf diesem
  Branch nicht gibt.

---

## 03.09.2026 — *Überfällig* ist widerlegt (Schritt 10b‑4, E‑71)

**Art:** fachliche Auskunft des Auftraggebers. **Nicht gemessen.**

| | |
|---|---|
| **Was behauptet wird** | (1) Eine Nachricht, die länger als `MessageTimeout` in `RUNNING` steht, wird vom Altsystem automatisch auf `ERROR_TIMEOUT` gesetzt. (2) `SUSPENDED`-Nachrichten warten **absichtlich** — auf einen Folgeprozess, etwa den Versand zu einem bestimmten Zeitpunkt — und werden nie automatisch beendet. (3) In der Praxis liegen sie höchstens **rund eine Woche** |
| **Herkunft** | Auftraggeber, 03.09.2026. Keine Messung, kein Statement, kein `EXPLAIN` |
| **Stand** | **ungemessen.** Die Testkopie kann sie nicht belegen: `RUNNING` kommt dort null Mal vor, und die 538 `SUSPENDED` sind der Bestand *eines* Status in *einer* Gestalt |
| **Wie sie zu prüfen wäre** | [`message-status.md`](message-status.md), Abschnitt „Die offene Prüfung" — eine Abfrage gegen die **Produktion**, mit vor dem Ergebnis festgehaltener Erwartung |
| **Was auf ihr ruht** | die Streichung der Problemkategorie *Überfällig* aus dem MVP samt Klassifizierermethode, Listenparameter und Detailfeld; die zwei neuen Kacheln *Läuft* und *Wartend*; `fristSekunden = null` bei `WARTEND` |

### Warum sie hier steht und nicht als Befund

**Regel Q4 verbietet Raten, nicht Auskünfte.** Eine Auskunft des Auftraggebers ist eine zulässige
Quelle — sie ist sogar die **einzige** für Fragen, die die Testkopie nicht beantworten kann.

**Was sie nicht ist, ist ein Befund.** Eine Auskunft, die als Befund abgelegt wird, ist genau der
Fehler, den [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3 beim Takt von `MatchInterchange`
dokumentiert: *„In der verbindlichen Datei stand ein Satz, der eine Angabe ohne Herkunft als
Tatsache ausgibt."*

**Deshalb trägt jede Datei, die die Regel führt, denselben Herkunftsvermerk — wörtlich und
unverändert.** Er steht an sieben Stellen: `PROJEKTBESCHREIBUNG.md` §3.2, §4.1 und §4.2,
[`message-status.md`](message-status.md) (zweimal), [`rollup.md`](rollup.md) §13 Punkt 49,
[`nachrichtendetail.md`](nachrichtendetail.md) §3a — und im Code an
`MessageStatusClassifier.TIMEOUT_EINHEIT` und `NachrichtendetailService.frist`.

### Was die Testkopie dazu sagt — und warum es nichts entscheidet

Gemessen als **M144 (c)** am 03.09.2026:

| Status | Zeilen | ältestes Alter gegen den Anker |
|---|---:|---:|
| `SUSPENDED` | **538** | **579.934 s = 6,71 Tage** |
| `RUNNING` | *kommt nicht vor* | — |

**Die 6,71 Tage sind mit „rund einer Woche" verträglich und belegen sie nicht.** Eine Gestalt, ein
Status, ein Zeitraum von sieben Tagen — und über die Behauptung, an der alles hängt (dass der
Wächter auf `RUNNING` zuschlägt), sagt die Kopie **nichts**.

### Die Gegenrechnung, die dazugehört

**M8 hat gemessen, dass `ERROR_TIMEOUT` *nicht* das Ablaufen von `MessageTimeout` ist** — 52 von 52
Nachrichten sind nach höchstens 5,6 Minuten tot, nicht nach 30. **Das widerspricht der Auskunft
scheinbar und tut es nicht:** 49 der 52 sind ein **einziger Vorfall** (M22), und M8 sagt selbst, es
habe „auf die falsche Grundgesamtheit" gezielt. Es gibt offenbar **zwei Wege** in denselben Status.
Vollständig als Belegvermerk bei M8 in [`messungen-schritt4.md`](messungen-schritt4.md).

---

## 04.09.2026 — „Offen" war eine Beschriftung und keine Definition (Nacharbeit zu 10b‑3b, E‑82)

**Art:** Befund am eigenen Code, in der Oberfläche gesehen. **Keine Messung, keine neue Abfrage.**

| | |
|---|---|
| **Was dastand** | Legende und Tooltip des Verlaufs beschrifteten die Farbrolle `--status-offen` mit **„Offen"** und `--status-abgeschlossen` mit **„Abgeschlossen"** |
| **Warum das erste falsch ist** | `istEndstatus` liefert für `AUFGETEILT` und `ZUSAMMENGEFUEHRT` **`true`** ([`message-status.md`](message-status.md)). Die Überschrift behauptete das Gegenteil über zwei ihrer vier Mitglieder |
| **Der Beleg** | ein Eimer mit 60 Nachrichten: *Offen 49* über *Aufgeteilt 1* und *Zusammengeführt 48*. **48 der 49 Zeilen stehen in einem Endstatus** |
| **Warum das zweite falsch ist** | nicht falsch, sondern **doppelt**: „Abgeschlossen" stand als Überschrift über einem gleichlautenden Eintrag mit anderer Zahl |
| **Korrigiert zu** | **„Ohne Ergebnis"** und **„Erledigt"**, englisch *No outcome* und *Done*. Tokennamen, Zuordnung, Stapelreihenfolge, Farbwerte und die Beschriftungen der **Einordnungen** sind unverändert |
| **Herkunft der Wörter** | **nicht erfunden.** „Ohne Ergebnis" ist die Formulierung, mit der [`visuelles-konzept.md`](visuelles-konzept.md) §3 die Rolle seit jeher begründet: *„Kein Ergebnis, kein Problem."* |

### Die Vorhersage stand seit dem 06.08.2026 in der Datei

*„Wer ‚offen‘ im Sinne der Oberfläche braucht, definiert das dort — und begründet es dort."*
([`message-status.md`](message-status.md), Warnkasten zu `istEndstatus`.) **Die Definition ist nie
getroffen worden.** Was stattdessen geschah, ist genau der Weg, vor dem der Satz warnt: Ein
*Tokenname* — vergeben nach der Farbe, die die Rolle trägt, und fachlich nach der Hälfte ihrer
Mitglieder benannt, die passt — ist unbemerkt in die *Beschriftungsposition* gerutscht und dort als
fachliche Aussage gelesen worden.

**Der Fehler saß im Namen und nicht in der Gruppierung.** Die Zusammenfassung acht → vier ist als
**Farb**entscheidung begründet (E‑l) und bleibt: *Kein Ergebnis, kein Problem* ist eine Aussage über
Neutralität und war nie eine über Offenheit. `features/dashboard/verlauf.ts`,
`lib/status-farbe.ts` und `app/globals.css` sind nicht angefasst.

### Was daran nicht gemessen ist

**An dieser Korrektur ist nichts gemessen, und es gibt nichts zu messen.** Es entsteht keine neue
Datenbankabfrage — Regel L7 **liegt nicht vor**, statt ausgenommen zu sein; die fehlende M‑Nummer
ist keine Auslassung.

Die 49/48‑Beobachtung ist **Augenschein an der laufenden Oberfläche** und kein Messwert. Sie
belegt, *dass* der Satz an einer Stelle falsch dastand — nicht, wie häufig das über den Bestand
geschieht. Die Größenordnung dazu ist alt und stammt aus einer richtigen Messung: **39,6 %** aller
`NEXANS`‑Zeilen sind `SPLITTED`/`MERGED`, und bei `IBIS`, `IBISGUS`, `ZAST`, `WOC` und `SYSTEM`
kommen beide über den **gesamten** Bestand **nicht ein einziges Mal** vor (M12,
[`messungen-schritt4.md`](messungen-schritt4.md)). Bei diesen fünf Mandanten wäre der Fehler nie
sichtbar gewesen: Eine Rolle, die über alle Eimer null ist, bekommt nach E‑l keinen
Legendeneintrag.


---

## 04.09.2026 — die Annahme „das Token kommt an" galt für Farbverläufe nicht (E‑83 bis E‑86)

**Art:** Messung an der laufenden Anwendung, **vor** dem Einbau gefahren. Keine neue Abfrage, keine
Aussage über das Quellsystem — die Korrektur betrifft eine Annahme über die **Anzeige**.

Der Verlauf des Dashboards ist von vier gestapelten Balkenreihen auf **eine Fläche mit Farbverlauf**
umgestellt worden ([`dashboard-frontend.md`](dashboard-frontend.md) §5.2). Zwei Sätze, die vorher
richtig dastanden, stimmen seither nicht mehr in der Form, in der sie dastanden.

### 1. `fillOpacity` — der Befund, der ohne die Messung mitgeliefert worden wäre

| | |
|---|---|
| **Was angenommen war** | Was in [`frontend-grundlagen.md`](frontend-grundlagen.md) §8a für `<Bar fill>` gemessen ist, gilt sinngemäß auch für eine Fläche: Die Zeichenkette `var(--token)` kommt ins Attribut, der Browser löst sie auf, und **das** wird gemalt |
| **Was gemessen ist** | Das Attribut stimmt, der aufgelöste Wert stimmt — und **gemalt wurde `#d5d97b` statt `#b9c022`** |
| **Die Ursache** | `<Area>` trägt in Recharts 3.10.1 die Voreinstellung `fillOpacity: 0.6`. Sie liegt *über* der Füllung, gleich ob dort ein Token oder ein Farbverlauf steht |
| **Die Behebung** | `fillOpacity={1}` am `<Area>`. Eine Zeile |
| **Warum es keine Ansicht gefunden hätte** | Eine um 40 % aufgehellte Fläche sieht nicht falsch aus, sie sieht blasser aus. Und `tests/farbwerte.test.ts` kann es nicht finden: Er **liest Text**, und im Text stand das richtige Token |

**Die Annahme war ausdrücklich als ungeprüft gekennzeichnet.** §8a führt Farbverläufe und
Flächendiagramme unter „Was nicht gemessen ist" auf und verlangt die Nachmessung nach demselben
Muster — genau das ist geschehen, und sie hat sich gelohnt. **Der Vermerk hat gehalten, die
Vermutung nicht.**

### 2. „Versatz 0,0 px" war eine Aussage über zwei Balkendiagramme

| | |
|---|---|
| **Was dastand** | *„Versatz des ersten Balkens **0,0 px** in allen 24 gemessenen Lagen"* ([`dichte-umschalter.md`](dichte-umschalter.md) §5.1, 01.09.2026) |
| **Warum es galt** | Beide Diagramme trugen Balken, beide bekamen dieselbe Rundung — der Fehler war in beiden derselbe und hob sich auf |
| **Was jetzt gilt** | Die **Fläche** liegt auf der Zeitachse, auf 0,0005 px genau; die **Balkenmitte** bis zu 0,21 px daneben. In neun Lagen gemessen und auf 0,0001 px vorhergesagt |
| **Die Ursache** | `combineAllBarPositions.js` rundet die Balkenbreite auf eine ganze Zahl; die halbe Rundungsdifferenz verschiebt die Mitte. Schranke **0,25 px**, unabhängig von `maxBarSize` und von der Zahl der Eimer |

**Der alte Wortlaut bleibt stehen.** Er war für seinen Gegenstand richtig, und der Gegenstand hat
sich geändert, nicht die Messung. Herleitung in [`frontend-grundlagen.md`](frontend-grundlagen.md)
§8b, Fortschreibung in [`dashboard-frontend.md`](dashboard-frontend.md) §5.3.

### Was daran nicht gemessen ist

**Der Zustand vor dem Umbau.** Dass der Balken auch vorher neben seiner eigenen Achsenbeschriftung
stand, folgt aus dem Quelltext — weder die Balkenlage noch die Achsenmarken sind in dieser Runde
angefasst worden — und **nicht** aus einer Aufnahme des alten Standes. Wer die Aussage härter
braucht, misst sie dort nach.

**Und keine der beiden Korrekturen sagt etwas über die Daten.** Es geht in beiden Fällen um Pixel,
nicht um Nachrichten; `PROJEKTBESCHREIBUNG.md` ist unberührt.

---

## 04.09.2026 — der Befund „die Kurve überschwingt" traf auf diesen Quelltext nie zu

**Art:** Nachmessung an der laufenden Anwendung. Keine Aussage über das Quellsystem — korrigiert
wird eine Annahme über den **eigenen Stand**, und zwar eine, die im Auftrag stand.

**Was angenommen war:** Die Fläche des Verlaufs sei mit `type="natural"` gezeichnet; die Glättung
überschwinge und male Werte, die in den Daten nicht vorkommen. Verlangt war der Wechsel auf
`type="monotone"`.

**Was zutrifft:** Der Quelltext trägt `type="monotone"`, seit die Fläche gebaut worden ist
(E‑83, 04.09.2026). **In keinem Stand hat je `natural` dagestanden** — nachgesehen im Verlauf der
Datei, nicht erinnert. Es war nichts zu ändern.

**Woher die Annahme vermutlich stammt, und warum das kein Vorwurf ist:** Das Vorbild, an dem die
Fläche gebaut ist, ist das Flächendiagramm von shadcn/ui, und **das benutzt `natural`**
([`dashboard-frontend.md`](dashboard-frontend.md) §5.2 nennt es als Vorbild). Wer den Umbau am Bild
beurteilt, sieht eine weiche Kurve und hat keinen Anlass anzunehmen, dass an genau dieser Stelle vom
Vorbild abgewichen worden ist. **Der Auftrag hat eine reale Eigenschaft der Vorlage benannt.**

**Was gefehlt hat, ist der Nachweis — und der fehlt jetzt nicht mehr.** Der gezeichnete Pfad ist in
allen drei Zeiträumen aus dem `d`-Attribut ausgelesen und dicht abgetastet worden: Er verlässt das
Intervall zwischen niedrigstem und höchstem Eimer um **0,0000 px**. Dieselbe Rechnung mit der
natürlichen Spline über dieselben Stützstellen läge bei **−395,6 Nachrichten**, also 30,5 px unter
der Nulllinie. **Der Befund des Auftrags gilt für die Kurvenform und nicht für diesen Stand.**

Zahlen, Rechenweg und Belegvermerk in [`dashboard-frontend.md`](dashboard-frontend.md) §5.2.

**Die Lehre, und sie ist die teurere Hälfte:** Bis heute stand in der Ansicht ein Kommentar, der
`monotone` gegen `natural` begründete — **ohne eine einzige Zahl**. Eine Begründung ohne Messung
liest sich wie eine Behauptung, und eine Behauptung lädt dazu ein, ihr Gegenteil zu vermuten. Der
Kommentar hatte recht und konnte es nicht zeigen.

---

## 04.09.2026 — der Browser mischt Deckung in sRGB, nicht im linearen Licht

**Art:** Messung an der laufenden Anwendung, **während** der Rechnung gefunden. Keine Aussage über
das Quellsystem — korrigiert wird ein **Rechenweg**, mit dem zwei Vorlagen bereits ausgeliefert
worden waren.

**Was angenommen war:** Eine Farbe mit Deckung über einem Untergrund mischt sich im **linearen
Licht**. Das ist die physikalisch richtige Art, Licht zu addieren, und es ist die Art, in der die
Rechnung dieses Projekts sonst arbeitet (OKLab setzt lineares sRGB voraus).

**Was zutrifft:** Der Browser mischt im **gammakodierten** Raum. `color-interpolation` steht in SVG
auf `sRGB`, und die Alphamischung folgt dem. In Chrome an drei Proben nachgesehen — ein SVG über
eine `data:`-URL auf ein Canvas gelegt und das Pixel ausgelesen:

| | gemessen | sRGB | linear |
|---|---|---|---|
| `#1992bf` zu 28 % über `#181818` | **#183a46** | #183a47 | #18536d |
| `#1992bf` zu 35 % über `#ffffff` | **#afd9e9** | #afd9e9 | #d3e1eb |
| `#ffffff` zu 12 % über `#181818` | **#343434** | #343434 | #646464 |

**Die dritte Zeile ist die teuerste.** Sie ist `--border` im Dunkelblock — `oklch(1 0 0 / 12%)` über
der Karte, also die **Gitterlinie** des Verlaufsdiagramms. Die beiden Rechenwege liegen dort
**0,19 in OKLab** auseinander, mehr als das Siebenfache der Sichtprobe von 0,025.

**Was das gekostet hat:** In der ersten Vorlage der Farbwerte standen sechs Zahlen, die den
Farbverlauf beschrieben — alle sechs waren nach dem linearen Rechenweg gebildet und beschrieben
eine Fläche, die niemand vor sich hat. **Der Fehler ist vor der Abnahme aufgefallen**, und zwar
nicht durch Nachdenken: Die gerasterte Gegenprobe am Browser lieferte `#183a46`, wo die Rechnung
`#18536d` sagte.

**Die Lehre, und sie ist die verallgemeinerbare Hälfte:** Die Rechnung war für **Farbwerte** gebaut
und ist für **Kompositionen** benutzt worden. Ein Wert aus `globals.css` ist dasselbe wie das, was
gemalt wird — eine Fläche mit Deckung ist es nicht. **Wo eine Zahl eine Mischung beschreibt, gehört
sie am Bildschirm gegengeprobt**, auch wenn der Rechenweg für die unvermischten Werte belegt ist.

Umgestellt in `scripts/farbwerte/rechne.mjs` und `tests/farbkontrast.test.ts`, mit der Messung als
Kommentar an der Funktion. Ausgeschrieben in [`dunkelmodus.md`](dunkelmodus.md) §4.4.

---

## 04.09.2026 — „zehn verschiedene Prozesse" gibt der Bestand nicht her

**Art:** Erhebung gegen die Testkopie (M146). Eine Aussage über den **Bestand**, nicht über den
Code.

**Was angenommen war:** Der Block „Zuletzt aufgefallen" zeigt nach der Verdichtung **zehn
verschiedene Prozesse** mit Anzahl und Zeitpunkt. So steht es in der Abnahme des Auftrags.

**Was zutrifft:** Über den **gesamten** Bestand hat `NEXANS` Fehler in **drei** Prozessen,
`SUTTONS` in **einem**, `VOTG` in **einem**. Der Block zeigt damit **null bis drei** Zeilen — nie
zehn.

| über den gesamten Bestand | Fehlerzeilen | betroffene Prozesse |
|---|---:|---:|
| `NEXANS` | 3.300 | **3** |
| `SUTTONS` | 103 | **1** |
| `VOTG` | 8 | **1** |

**Die Verdichtung wirkt trotzdem, und zwar genau wie beabsichtigt:** Aus zehn identischen Zeilen
werden zwei verschiedene (*49 × BMW LAB (VDA)*, *1 × BMW Global Invoice (EDIFACT)*). **Der Deckel
von zehn bleibt** — er greift auf diesen Daten nur nicht.

**Und die Zahl ist selbst die Auskunft.** Dass ein einziger Prozess 49 von 50 Fehlern trägt, war an
zehn gleichen Zeilen nicht abzulesen; jetzt steht es da. Wer den Block für „zu leer" hält, liest
darin die richtige Aussage über einen Mandanten, der seine Fehler an wenigen Stellen hat.

**Was daraus nicht folgt:** eine Aussage über die **Produktion**. Ob der Block dort zehn Zeilen
füllt, ist nicht gemessen und kann es von hier aus nicht sein.
