# Rollup — `message_rollup` und der Job, der sie fortschreibt

Schritt 10a, gebaut am 26.08.2026. Baut auf [`messungen-schritt10.md`](messungen-schritt10.md)
(M86–M92) und der Sparringsrunde vom 24.08.2026.

## 1. Zweck, in zwei Sätzen

**Leistungsregel L2 verbietet Live-Aggregation über `Message` für Dashboard-Kennzahlen.**
`message_rollup` ist die Tabelle, aus der das Dashboard stattdessen liest; ein stündlicher
Delta-Lauf und ein nächtlicher Volllauf schreiben sie fort.

**Der Rollup existiert nicht, weil eine Live-Abfrage scheitern würde.** Bei 3,3 Millionen Zeilen und
2,9 GB wäre sie technisch möglich — das Altsystem macht sie tagesweise. Er existiert, damit das
Dashboard unter einer halben Sekunde lädt und die Produktionsdatenbank nichts davon merkt
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Begründung korrigiert am 27.07.2026).

### Was 10a ist und was nicht

| | |
|---|---|
| **Gebaut** | die Tabellen `message_rollup` und `rollup_lauf`, das Leserepository, der Job, die Auslösung, die Tests |
| **Nicht gebaut** | kein Endpunkt, keine Oberfläche, keine Kachel, kein Diagramm, kein neuer Parameter an `GET /api/nachrichten`. Das ist 10b |

---

## 2. Die zwei Tabellen (`V9__message_rollup.sql`)

### `message_rollup`

```sql
CREATE TABLE message_rollup (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

**Die Definition ist übernommen und nicht entworfen.** M89 hat genau diese Tabelle als Probetabelle
angelegt und über den Gesamtbestand gefüllt; die Zahlen unten in §9 sind gegen sie gemessen.

**Der Schlüssel ist `(stunde, process_id, message_status)` — Entscheidung E‑a.** Mandant, Partner
und Richtung stehen **nicht** in der Zeile. Der Mandant kommt in 10b aus dem Join über
`Process → ProjectMandant`, Partner und Richtung aus `process_catalog`.

> **Widerspruch zu [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5, und er ist bekannt.**
> Dort steht „stündliche Aggregate je **Mandant, Prozess, Partner, Richtung, Status**". Das ist die
> ältere Fassung; E‑a hebt sie auf, und M89 stützt E‑a: 11,30 ms für das Standardfenster mit
> Katalog-Join, 237,67 ms für ein Monatsfenster — bei einem Budget von 500 ms für die ganze
> Landingpage. **Der Nachtrag in §5 ist nicht Sache dieses Schritts**; er steht als offener Punkt
> 51 unten.

**In `message_status` steht der Rohwert, nicht die Einordnung — Entscheidung E‑g.** Der
`MessageStatusClassifier` läuft beim **Lesen**, also in 10b. Grund: Die Einordnung ist eine Regel,
die sich ändern kann; der Rohwert ist eine Tatsache. Wäre die Kategorie materialisiert, müsste jede
Regeländerung die ganze Tabelle neu rechnen. **Der Preis dafür ist gemessen und beträgt eine
Zeile:** 335.610 statt 335.609 (M87, Variante 1 gegen Variante 2).

**Kein Sekundärindex.** Die Probetabelle aus M89 hatte 0 Byte Indexanteil, und beide dort gemessenen
Pläne steigen über den Primärschlüssel ein. Die gebaute Tabelle bestätigt das: **0 Byte** auch nach
dem Volllauf (§9). Ein Index für die Prozesssicht (10c) ist nicht gemessen und wird nicht auf
Verdacht gebaut.

**`utf8mb4_general_ci` ist Absicht, nicht Nachlässigkeit.** `message_status` muss dieselbe Sortierung
tragen wie `GlassfishDB.Message.MessageStatus`, sonst gruppieren Quelle und Rollup verschieden;
`process_id` dieselbe wie `Process.ProcessID` und `process_catalog.process_id`, sonst bricht der
Join in 10b. Das ist **keine** tokenartige Spalte im Sinne der `_bin`-Regel aus §5 — hier steht kein
Sitzungsschlüssel, sondern ein lesbarer Prozessname und ein Statuswort.

**Zeitzone:** `stunde` ist **Wanduhrzeit des Quellservers** und nicht UTC. Zeitstempel aus
`GlassfishDB` werden nirgends konvertiert ([`datenzugriff.md`](datenzugriff.md) §7). Die beiden
UTC-Zeitstempel dieses Schritts stehen in `rollup_lauf` und heißen bewusst anders.

### `rollup_lauf`

```sql
CREATE TABLE rollup_lauf (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,
  art                VARCHAR(10) NOT NULL,
  fenster_von        DATETIME    NOT NULL,
  fenster_bis        DATETIME    NOT NULL,
  gestartet_am       DATETIME(3) NOT NULL,
  beendet_am         DATETIME(3) NULL,
  zeilen_geschrieben INT         NULL,
  fehler             TEXT        NULL,
  PRIMARY KEY (id),
  KEY rollup_lauf_stand_idx (beendet_am, fenster_bis)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

Wasserstand und Protokoll in einer Tabelle. `art` ist `DELTA` oder `VOLL`.

> ### Warum der Wasserstand eine eigene Tabelle braucht und nicht `MAX(stunde)` ist
>
> **Eine Stunde ohne Verkehr erzeugt keine Rollup-Zeile.** `MAX(stunde)` sagt darum „letzte Stunde
> mit Verkehr", nicht „bis hierhin ist gerechnet". Der Unterschied ist genau der zwischen *nichts
> passiert* und *noch nicht nachgesehen* — und den darf das Dashboard später nicht verwechseln.
>
> Auf der Testkopie ist das keine Feinheit: **Fünf zusammenhängende Monate** (2026‑01 bis 2026‑05)
> tragen null Zeilen (M92). Ein Wasserstand aus `MAX(stunde)` bliebe dort auf dem 30.12.2025 stehen
> und ließe den Job jede Nacht dieselben fünf Monate neu rechnen.

**Der Wasserstand ist `MAX(fenster_bis)` über Läufe, die abgeschlossen *und* fehlerfrei sind:**

```sql
SELECT MAX(fenster_bis) FROM rollup_lauf
WHERE beendet_am IS NOT NULL AND fehler IS NULL;
```

- `beendet_am IS NULL` heißt **abgebrochen oder noch laufend** — beides zählt nicht. Sonst
  übersprünge der nächste Lauf einen Bereich, der nur zur Hälfte geschrieben ist, und die Lücke
  fände niemand mehr.
- `fehler IS NOT NULL` heißt **abgeschlossen, aber nicht verlässlich gerechnet**. `beendet_am` wird
  im Fehlerfall trotzdem gesetzt, damit die Zeile nicht als „läuft gerade" den nächsten Lauf sperrt.

Ist die Tabelle leer, fällt der Delta-Lauf auf `MIN(Message.MessageLastUpdate)` zurück — der erste
Delta-Lauf holt damit den ganzen Bestand nach, ohne dass jemand einen Volllauf ansteuern müsste.

---

## 3. Der Zeitschnitt

`rollup/RollupFenster` — ein reiner Wertetyp, ohne Datenbank prüfbar.

**Beide Grenzen liegen immer auf einem vollen Stundenanfang**, `von` einschließlich, `bis`
ausschließlich. Der Konstruktor weist alles andere ab; darauf ruht Löschen-und-Neuschreiben.

### Fensterberechnung des Delta-Laufs

1. Wasserstand `W` (siehe §2). Ist `rollup_lauf` leer, ist `W` = `MIN(Message.MessageLastUpdate)`.
2. Rohes Fenster: von `W − 15 Minuten` bis `jetzt` (**Anwendungsuhr**).
3. **Auf ganze Stunden ausdehnen:** Der Beginn wird auf den Anfang seiner Stunde abgerundet, das
   Ende auf den Anfang der *nächsten* Stunde aufgerundet.
4. Verarbeitet werden die vollständigen Stundeneimer in diesem Bereich.

Fenster des Volllaufs: von `MIN(Message.MessageLastUpdate)` bis `jetzt`, ebenso ausgedehnt. **Kein
Nachlauf** — er begänne ohnehin vor der ersten Zeile.

> ### Schritt 3 ist der Kern und keine Formalie
>
> Die 15 Minuten sind ein Rückgriff auf den **Zeilenzeitstempel**, aber die Schreibeinheit ist der
> **ganze Stundeneimer**. Würde nur der rohe Bereich gelesen und dazugezählt, zählte der Anteil
> zwischen `W − 15 min` und `W` **doppelt**. Deshalb: ganze Eimer, gelöscht und neu geschrieben.
>
> **Daraus folgt die Eigenschaft, auf der der ganze Ablauf ruht.** Weil `W` selbst immer ein
> Stundenanfang ist, fällt `W − 15 min` **stets in die vorige Stunde**. Der zuletzt geschriebene
> Eimer wird also bei jedem Delta-Lauf noch einmal vollständig gerechnet — und genau das macht es
> unbedenklich, dass der obere Rand eine **angebrochene** Stunde einschließt. Sie wird beim nächsten
> Lauf ersetzt, nicht ergänzt.

**Das Ende ist der Anfang der *nächsten* Stunde, auch genau auf der Stundengrenze.** Läge `jetzt`
auf `14:00:00`, wäre das Ende `15:00` und nicht `14:00`. Sonst hätte der Lauf genau auf der
Stundengrenze ein leeres Fenster, abhängig davon, ob der Auslöser eine Sekunde vor oder nach der
vollen Stunde feuert — ein Verhalten, das an der Sekunde des Weckers hängt, ist kein Verhalten.

**Ein leeres Fenster ist ein Zustand und kein Fehler.** Es entsteht, wenn der Wasserstand *mehr als
eine Stunde* vor der Anwendungsuhr liegt. Dass ein Lauf unmittelbar auf den vorigen folgt, genügt
dafür **nicht** — der Rückgriff zieht `von` dann in die Stunde vor `bis`. Wirklich leer wird es
erst, wenn die **Uhr zurückspringt**: im Profil `dev` der praktische Fall, wenn die Testkopie neu
befüllt wird und der ermittelte Anker früher liegt als zuvor. Der Lauf schreibt trotzdem seine
Protokollzeile.

### Das Nachlauffenster: 15 Minuten, gemessen und nicht gewählt

M86 hat über **220.579 Nachrichten** und alle elf Statuswerte geprüft, wie weit
`MessageLastUpdate` nachträglich wandert:

| | |
|---|---:|
| Anteil nachgeschriebener Zeilen | 0,62 % bis 2,17 % |
| **größte beobachtete Differenz** | **4 Sekunden** |
| Werte über 60 Sekunden | **0** |
| nachgeschriebene Zeilen in Fenster B | 1.548 |

Ein Nachlauffenster von **48 Stunden** — als Möglichkeit mitgeführt — wäre um den Faktor **43.200**
überdimensioniert. Ein Wasserstand **ohne** Nachlauffenster ist aber ebenfalls nicht gedeckt: Die
1.548 nachgeschriebenen Zeilen sind gemessen und nicht null.

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* Auf dem Bestand der Testkopie wandert `MessageLastUpdate` um höchstens vier
> Sekunden.
> *Behauptet wird:* Fünfzehn Minuten genügen.
> **Die Lücke:** `RUNNING` kommt auf der Testkopie **null Mal** vor, und `MatchInterchange` — der
> Job, der `COMMIT_RECEIVED` nachträglich setzt — läuft dort **nicht sichtbar** (M31‑3, Takt
> ungedeckt). Der zulässige Satz lautet „auf diesem Bestand nicht beobachtet", nicht „findet nicht
> statt". Der Sicherheitsabstand von Faktor 225 gegenüber der gemessenen Obergrenze trägt dem
> Rechnung — und deshalb ist der nächtliche Volllauf keine Zierde, sondern die zweite Sicherung.

**Der Wert ist eine Konstante und kein Konfigurationsschlüssel** (`RollupFenster.NACHLAUF_MINUTEN`).
Es ist eine Entscheidung mit einer Messung dahinter, keine Betriebseinstellung; ein Drehknopf lüde
dazu ein, sie ohne neue Messung zu verstellen. Zeigt die Produktion eine längere Wanderung, ist das
ein Befund und eine Codeänderung wert — offener Punkt 49.

---

## 4. Die zwei Uhren

**Das ist die Stelle, an der bei diesem Schritt am ehesten etwas schiefgeht.** In derselben Zeile
von `rollup_lauf` stehen zwei Zeitpunkte aus **verschiedenen** Uhren. Die Zuordnung steht deshalb in
einer eigenen, ohne Datenbank prüfbaren Klasse (`rollup/RollupUhren`) und nicht verteilt im Job.

| Spalte | Uhr | Warum |
|---|---|---|
| `fenster_von`, `fenster_bis` | **Anwendungsuhr** (`Clock`, `@Primary`) | **Datenzeit.** Sie beschreiben, welchen Ausschnitt aus `GlassfishDB` der Lauf verarbeitet hat, und werden gegen `Message.MessageLastUpdate` gehalten. Im Profil `dev` liegt sie beim Anker der Testkopie — und genau so soll der Lauf dort auch rechnen |
| `gestartet_am`, `beendet_am` | **`systemClock`** (echte Uhr, UTC) | **Protokollzeit**, wie `geaendert_am` und das `audit_log` (Regel A5). Eine Protokollzeile mit zurückversetzter Uhr wäre im Betrieb unlesbar |

**Im Profil `dev` liegen die beiden Paare Monate auseinander** — am 26.08.2026 rund 239 Tage. **Das
ist der erwartete Anblick und kein Fehler.** Die gemessene Laufzeile Nr. 25 zeigt es:

| `fenster_von` | `fenster_bis` | `gestartet_am` | `beendet_am` |
|---|---|---|---|
| `2024-10-01 02:00:00` | `2025-12-30 05:00:00` | `2026-08-26 15:05:06.806` | `2026-08-26 15:05:52.800` |

**Regel Z1 ist eingehalten:** `LocalDateTime.now()` wird nicht aufgerufen. Beide Werte entstehen aus
`Clock.instant()` — dieselbe Form wie in `audit/AuditLogWriter`.

> ### Eine Folge, die vor der Abnahme zu kennen ist
>
> **Im Profil `dev` kann ein Volllauf den Gesamtbestand nicht abdecken.** Seine obere Grenze ist die
> Anwendungsuhr, und die steht am Anker `2025-12-30 04:09:47`; die 5.133 Zeilen des Jahres 2026
> liegen dahinter. Gemessen (§9): dev-Lauf **335.595 Zeilen / 3.336.386 Nachrichten**, Lauf gegen
> die Systemuhr **335.610 / 3.341.519**. Die Differenz ist Zeichen für Zeichen der Rest aus M92:
> **15 Rollupzeilen** (13 im Juni, 2 im Juli 2026) und **5.133 Nachrichten**.
>
> **In Produktion tritt das nicht auf**, weil die Anwendungsuhr dort die Systemuhr ist und damit
> hinter dem jüngsten Datensatz liegt. Wer die Abnahmezahlen lokal reproduzieren will, fährt den
> Lauf gegen die Systemuhr — siehe §8.

---

## 5. Der Lauf

`rollup/RollupJob` — **eine** Methode für beide Laufarten:

```java
RollupErgebnis fuehreAus(LocalDateTime von, LocalDateTime bis, LaufArt art)
```

Delta-Lauf und Volllauf unterscheiden sich **ausschließlich im Fenster** und laufen danach durch
denselben Code und dieselbe Abfrage. **Zwei getrennte Abfragen zu schreiben ist ausdrücklich
verboten** — sie liefen auseinander, und der Nachtlauf änderte dann still die Zahlen, die tagsüber
jemand gesehen hat. Die Laufart steht nur im Protokoll.

### Ablauf

1. **Zeile in `rollup_lauf` anlegen** (`gestartet_am`, `art`, Fenster), `beendet_am` bleibt `NULL`.
   **Vor** dem Rechnen: Stürzt die Anwendung mitten im Lauf ab, steht die Zeile trotzdem da — ohne
   `beendet_am`, damit sie den Wasserstand nicht hebt, und mit ihrem Fenster, damit ablesbar ist,
   woran der Lauf war.
2. **Aggregation über den Lese-Pool holen**, scheibenweise (§6). Das liegt **außerhalb** jeder
   Transaktion: Der Lesezugriff läuft auf einer anderen Verbindung und wäre ohnehin nicht Teil von
   ihr ([`datenzugriff.md`](datenzugriff.md) §2).
3. **Alle Rollup-Zeilen im Fenster löschen, dann die neuen einfügen — in genau einer Transaktion.**
4. **`beendet_am` und `zeilen_geschrieben` nachtragen.** Erst danach zählt das Fenster zum
   Wasserstand.

Bei einer Ausnahme: `fehler` füllen, `beendet_am` setzen, Ausnahme weiterreichen. Schlägt *das*
fehl, wird es der ursprünglichen Ausnahme angehängt und nicht an ihre Stelle gesetzt.

> ### Löschen und neu schreiben — nie `anzahl = anzahl + n`
>
> Zwei Gründe, und beide sind Fehler, die still bleiben:
>
> 1. **Der 15‑Minuten‑Rückgriff überlappt mit dem letzten Lauf.** Hochzählen verdoppelte den
>    Überlapp — und zwar bei jedem Lauf aufs Neue, sodass der Fehler wächst statt aufzufallen.
> 2. **Wechselt eine Nachricht innerhalb desselben Eimers den Status**, verschwindet die alte
>    Statuszeile nicht von selbst. Ein `INSERT … ON DUPLICATE KEY UPDATE` schriebe die neue Zeile
>    und ließe die alte mit ihrem alten Zählstand stehen. Die Summe wäre zu hoch, die Verteilung
>    nach Status falsch — beides ohne jede Fehlermeldung.
>
> Beide Fälle haben einen eigenen Datenbanktest (§10).

**Schritt 3 läuft in genau einer Transaktion.** Beim Volllauf umfasst sie 335.610 Zeilen und
21,6 MiB. Das ist gewollt: **Eine halb geleerte Rolluptabelle um 03:00 wäre ein leeres Dashboard.**

**Eingefügt wird in Stapeln zu 1.000 Zeilen** (`monitorDsl.batch(…)`, dasselbe Muster wie in
`ProzessKatalogRepository.speichereBestandsflags`). Die Größe ist eine Größenordnung und kein
gemessenes Optimum — gemessen ist der Lauf als Ganzes (§9).

### Kein `INSERT … SELECT` über die Schemagrenze

M89 hat die Probetabelle mit einem schemaübergreifenden `INSERT … SELECT` gefüllt: **51,242 s für
22 Monate.** Dieser Bau macht es anders — er liest in den Speicher und schreibt gebündelt —, weil
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6 sagt: *„Der Schreib-DSLContext darf
ausschließlich `overlord_monitor`."* Ein `INSERT … SELECT` aus `GlassfishDB` über den Schreib-Pool
widerspricht dem Satz, auch wenn `monitor_write` dort `SELECT` besitzt und die Schreibgarantie
unberührt bliebe.

**Der gebaute Weg ist gegen die 51,242 s gemessen: 45,772 s.** Er ist also **10,7 % schneller**
statt langsamer; die Faktor‑drei‑Schranke des Auftrags ist nicht in Sicht, und es gibt nichts
vorzulegen. Zahlen in §9.

---

## 6. Die Scheiben — über einen Kalender, nicht über die Daten

**Der Volllauf liest in Monatsscheiben.** Das ist keine Vorsicht, sondern eine gemessene Grenze:

> **Der Lese-Pool setzt `SET SESSION max_statement_time=10`** ([`datenzugriff.md`](datenzugriff.md)
> §1). Ein einziges Statement über den Gesamtbestand reißt sie — **gemessen am 26.08.2026:
> `ERROR 1969 (70100) Query execution was interrupted (max_statement_time exceeded)`** nach zehn
> Sekunden. Der Volllauf ist ohne Schnitt nicht baubar.

Eine Monatsscheibe kostet **2,575 s** (größte Scheibe 2025‑07, 248.320 Zeilen, beste von fünf) —
**Faktor 3,9** Luft zur Zeitgrenze.

**Die Form ist die Bauvorgabe aus M92 im Wortlaut:** *„Er iteriert über einen Kalender, nicht über
die vorhandenen Daten."* Eine leere Scheibe wird also **mitgerechnet** und nicht übersprungen — auf
der Testkopie sind das die fünf Monate 2026‑01 bis 2026‑05. Sie kosten 1 ms je Scheibe; sie zu
überspringen wäre eine Optimierung ohne Gegenwert und genau die, die den Lauf falsch machte: **Ein
Eimer, der einmal Zeilen hatte und heute keine mehr hat, würde sonst nie geleert.**

Die erste und die letzte Scheibe sind in der Regel angebrochen. Das ändert nichts an der
Eimertreue: Beide Grenzen bleiben Stundenanfänge, und ein Monatserster um Mitternacht ist einer.

**Ein Delta-Fenster ist genau eine Scheibe** — der stündliche Lauf zahlt für den Kalender nichts.

---

## 7. Die Abfragen (Regel L7)

### Die Aggregation

`rollup/RollupLeseRepository.aggregiere(von, bis)`, gelesen über `glassfishDsl`. **Wörtlich die
Fassung aus M88**, übernommen und nicht nachgebaut:

```sql
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID, MessageStatus, COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= ? AND MessageLastUpdate < ?
GROUP BY stunde, ProcessID, MessageStatus;
```

**Ein Unterschied zur Messfassung, und er ist der bessere Weg.** jOOQ schreibt in `GROUP BY` den
**vollen Ausdruck** aus, wo M88 den Alias `stunde` verwendet. Semantisch ist das dasselbe — und es
ist genau die Fassung, die **Befund 11** empfiehlt: Dort hat `GROUP BY` an eine gleichnamige
*Tabellenspalte* gebunden statt an den Ausdrucksalias, und bei zwei von drei Mandanten sah das
Ergebnis trotzdem richtig aus. `Message` hat zwar keine Spalte `stunde`, aber die sichere Form
kostet hier nichts. Ein Test hält das gerenderte Statement Zeichen für Zeichen fest
(`RollupStatementsTest`).

**Kein `STRAIGHT_JOIN`, in keiner Fassung** (M42: Faktor 219 bis 1094).

**`EXPLAIN` gegen die Testkopie, 26.08.2026** — Zeichen für Zeichen der Plan aus M88:

```
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
| id   | select_type | table   | type  | possible_keys                                           | key                  | key_len | ref  | rows  | Extra                                                  |
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
|    1 | SIMPLE      | Message | range | MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL | 17730 | Using index condition; Using temporary; Using filesort |
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
```

**`MessageLastUpdateProcessMessageIDX` bringt dem Rollup nichts.** Er ist für den Cursor der
Nachrichtenliste gebaut, und `MessageStatus` steht in keinem der beiden Indizes — jede Fassung
braucht den Rückgriff auf die Tabellenzeile. Ein deckender Index müsste `MessageStatus` enthalten;
ihn anzulegen hieße, auf `GlassfishDB` zu schreiben, und das ist ausgeschlossen (Regel S1).

**Oberhalb einer geschätzten Bereichsgröße von rund einer halben Million Sätzen kippt der Optimierer
doch auf den zusammengesetzten Index** (M92, Monatsscheiben). Auf die Laufzeit wirkt sich das nicht
sichtbar aus: 11,4 µs je Zeile mit dem schmalen, 11,1 µs mit dem breiten.

### Der früheste Zeitstempel

```sql
SELECT MIN(MessageLastUpdate) FROM GlassfishDB.Message;
```

`EXPLAIN`: **`Select tables optimized away`** — der Wert kommt aus dem ersten Blatt von
`MessageLastUpdateIDX`, ohne einen einzigen Tabellenzugriff.

**Ohne Zeitfenster, und das ist gedeckt (Regel L9):** Gefragt ist der Anfang des Bestands, und ein
Zeitfenster schnitte genau die Zeile weg, um die es geht. Es ist außerdem keine Aggregation über
Zeilen, sondern eine Indexspitze — dieselbe Form, die die Dev-Uhr seit Schritt 2 beim Start liest.
Kosten: **0,272 ms** (§9).

---

## 8. Auslösung, Profil, Konfiguration

### Zwei Riegel, und beide sind Absicht

1. **Das Spring-Profil `rollup`.** Ohne es gibt es `RollupPlaner` gar nicht — und damit im Profil
   `dev` standardmäßig keine Auslösung. **Ein Job, der beim lokalen Start unaufgefordert in die
   geteilte Testkopie schreibt, ist eine Überraschung.** Im Betrieb läuft der Job damit als eigener
   Prozess derselben Anwendung (`SPRING_PROFILES_ACTIVE=prod,rollup`), ohne zweites Artefakt und
   **ohne eigenes Wurzelpaket** ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6: „Die
   Trennung läuft über das Spring-Profil, nicht über den Namensraum.").
2. **`overlord.rollup.aktiv`.** Damit lässt sich die Auslösung abschalten, ohne das Profil zu
   ändern — im Betrieb kommt das Profil aus einer Umgebungsvariablen, und eine Abschaltung über sie
   kostete eine Neubereitstellung. **Fehlt der Schlüssel, ist die Auslösung aus.**

Der `dev`-Block in `application.yml` setzt `aktiv: false` **zusätzlich ausdrücklich**, damit auch
ein neugieriges `dev,rollup` nicht schreibt.

### Die Schlüssel unter `overlord.rollup`

| Schlüssel | Vorgabe | Wirkung |
|---|---|---|
| `aktiv` | `false`, wenn nicht gesetzt (`application.yml`: `true`, im `dev`-Block `false`) | zeitgesteuerte Auslösung an/aus |
| `delta-plan` | `0 5 * * * *` | Cron des stündlichen Laufs |
| `voll-plan` | `0 0 3 * * *` | Cron des nächtlichen Laufs — **ungemessen** |
| `scheiben-pause` | `1s` | Drosselung zwischen zwei Monatsscheiben (L6) — **ungemessen** |
| `beim-start` | — | `DELTA` oder `VOLL`: ein einmaliger Lauf beim Start |

**Beide Ausdrücke stehen als Platzhalter in `application.yml` und nicht als Literal im Code.** Fehlt
einer, scheitert der Start — das ist besser als ein Zeitplan, den niemand kennt.

> **Die Uhrzeit des Nachtlaufs ist ungemessen (Regel Q4).** Die Verteilung von `MessageLastUpdate`
> über die Tagesstunde ist in M86 bis M92 **nicht erhoben** worden. `03:00` ist die übliche Wahl und
> keine hergeleitete. Offener Punkt 50.

**Die Minute des Delta-Laufs (5) ist ebenfalls nicht gemessen — aber folgenlos.** Weil jeder Lauf
ganze Stundeneimer ersetzt und 15 Minuten zurückgreift, wird die zuletzt geschriebene Stunde beim
nächsten Lauf ohnehin neu gerechnet. Der Abstand zur vollen Stunde hält den Job nur aus dem Gedränge
der Jobs heraus, die dort üblicherweise starten.

### Nur eine Ausführung gleichzeitig

Läuft bereits ein Lauf — Zeile in `rollup_lauf` ohne `beendet_am`, jünger als **eine Stunde** —,
wird der neue übersprungen und das **auf WARN** protokolliert. Ein übersprungener Lauf ist kein
Normalfall, sondern der Hinweis darauf, dass ein Lauf länger braucht als sein Takt.

Die Altersgrenze von einer Stunde ist der Takt des Delta-Laufs. Ohne sie sperrte ein einziger
abgestürzter Lauf den Job **dauerhaft und lautlos**: Jeder folgende würde übersprungen, das
Dashboard veraltete, und im Protokoll stünde nichts als „übersprungen".

Zwei gleichzeitige Läufe wären fachlich nicht einmal falsch — sie löschen und schreiben dieselben
Eimer —, aber sie verdoppelten die Last auf einer Instanz, die sich mit der Produktion teilt (L6).

### Der Weg von Hand

**Als Startparameter, ausdrücklich nicht als HTTP-Endpunkt:**

```
java -jar overlord-monitor.jar --overlord.rollup.beim-start=VOLL
./mvnw spring-boot:run -Dspring-boot.run.arguments=--overlord.rollup.beim-start=DELTA
```

**Warum kein Endpunkt.** Ein Endpunkt, der die Rolluptabelle neu schreibt, wäre eine Schreibfläche
im Anfragepfad — und die gäbe es dann auch für den, der sie nicht bedienen soll. Der Startparameter
steht nur dem offen, der die Anwendung startet, und er hinterlässt dieselbe Spur in `rollup_lauf`
wie jeder andere Lauf.

Er hängt **weder am Profil `rollup` noch an `aktiv`** — beide regeln die *zeitgesteuerte* Auslösung;
dieser Lauf ist ausdrücklich angefordert worden. Ein gescheiterter Lauf verhindert den Start der
Anwendung **nicht**: Die Oberfläche hängt nicht am Rollup, und ein Backend, das wegen eines
Aggregationsfehlers gar nicht hochkommt, wäre der größere Schaden.

> **Für die Abnahmezahlen lokal** braucht es zusätzlich die Systemuhr statt des dev-Ankers (§4):
> `--spring.profiles.active=prod --server.port=0 --overlord.rollup.beim-start=VOLL`.

### Leistungsregel L6, und wo sie greift

> **L6 — „Der Rollup-Job läuft gedrosselt. Er teilt sich die Instanz mit der Produktion."**

**Beim Delta-Lauf ist sie Zeremonie statt Schutz, und das gehört so gesagt.** Ein Statement, das
88 ms braucht und einmal je Stunde läuft, belegt die Instanz zu **0,0024 %** — das ist die
*dichteste* Stunde von rund 15.000, nicht die durchschnittliche. Wovor die Drosselung schützen soll,
tritt beim stündlichen Lauf nicht ein. *Eine Regel, die ohne Grund zitiert wird, verliert ihre
Wirkung dort, wo sie gebraucht wird.*

**Gebraucht wird sie beim Nachtlauf**, und dort ist sie umgesetzt: eine Pause zwischen zwei
Monatsscheiben. Gewartet wird **zwischen** den Scheiben, nicht vor der ersten oder nach der letzten
— **ein Delta-Lauf hat genau eine Scheibe und wartet damit nie.**

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* Der Volllauf kostet ohne Pause 45,772 s, mit einer Sekunde je Scheibengrenze
> 69,186 s (22 Pausen).
> *Behauptet wird:* Eine Sekunde je Scheibe ist die richtige Drosselung.
> **Die Lücke:** Wie viel Last die Produktionsinstanz nachts verträgt, ist **nicht erhoben**. Der
> Wert ist eine Vorgabe ohne Messung und deshalb konfigurierbar. Offener Punkt 50.

---

## 9. Messungen (Regel L7)

Alle Werte gegen die Testkopie am **26.08.2026**, Client `mysql.exe` 8.0.46 mit
`--ssl-mode=DISABLED` (dieselbe Abweichung A1 wie seit M32), Laufzeiten aus `SHOW PROFILES`,
je ein Aufwärmlauf und dann **beste von fünf**.

### Die Aggregation, so wie der Code sie schickt

| Fenster | Nachrichten | Aufwärmlauf | **beste von fünf** | alle fünf (ms) | µs je Zeile |
|---|---:|---:|---:|---|---:|
| **dichteste Stunde des Bestands** `2025-12-07 17:00` | 8.630 | 94,424 ms | **84,999 ms** | 88,481 · 85,833 · 84,999 · 89,341 · 87,140 | 9,85 |
| **typischer Delta-Lauf** `2025-12-30 03:00`–`05:00` | 358 | 5,451 ms | **4,095 ms** | 4,095 · 4,460 · 4,385 · 4,273 · 4,174 | 11,4 |
| **letzte Stunde des Bestands** `2026-07-08 17:00` | 285 | 3,848 ms | **3,626 ms** | 3,903 · 3,860 · 4,913 · 4,223 · 3,626 | 12,7 |
| **24 h ab dem Anker** `2025-12-29 05:00`–`2025-12-30 05:00` | 6.248 | 68,364 ms | **64,150 ms** | 65,288 · 64,150 · 64,935 · 69,661 · 70,201 | 10,3 |
| **größte Monatsscheibe** `2025-07` | 248.320 | 2.622,968 ms | **2.575,006 ms** | 2.715,502 · 2.654,878 · 2.575,006 · 2.654,528 · 2.687,443 | 10,4 |
| `MIN(MessageLastUpdate)` | — | 0,508 ms | **0,272 ms** | 0,303 · 0,296 · 0,295 · 0,373 · 0,272 | — |

**9,85 bis 12,7 µs je Zeile über einen Mengenbereich von Faktor 871.** Das deckt sich mit M88
(10,2–11,3 µs) und M92 (11,0–11,4 µs), die unabhängig davon erhoben wurden. **Die Laufzeit hängt an
der Zeilenzahl, nicht an der Fensterbreite.**

Die dichteste Stunde des Bestands trägt hier wie in M88 **8.630 Zeilen** — die Testkopie ist seit
der Messrunde unverändert.

### Die Zeitgrenze des Lese-Pools

| | |
|---|---|
| Gesamtbereich in **einem** Statement, `max_statement_time=10` | **`ERROR 1969` nach 10 s** |
| größte Monatsscheibe | 2,575 s — Faktor 3,9 Luft |

**Das ist der Grund für den Kalender aus §6**, und es ist eine Messung und keine Auslegung.

### Der Volllauf, gebaut gegen gemessen

| Lauf | Scheiben | Zeilen | `SUM(anzahl)` | **Laufzeit** |
|---|---:|---:|---:|---:|
| M89, `INSERT … SELECT` über die Schemagrenze (22 Monatsscheiben) | 22 | 335.610 | 3.341.519 | 51,242 s |
| **gebaut**, Systemuhr, ohne Drosselung | 23 | **335.610** | **3.341.519** | **45,772 s** |
| **gebaut**, Systemuhr, mit `scheiben-pause: 1s` | 23 | 335.610 | 3.341.519 | **69,186 s** |
| gebaut, dev-Anker, ohne Drosselung | 15 | 335.595 | 3.336.386 | 45,994 s |

> **Der Auftrag verlangt, den gebauten Weg gegen die 51,242 s zu messen und das Ergebnis zu melden;
> läge er um mehr als Faktor drei darüber, wäre das eine Entscheidung für den Auftraggeber.**
> **Er liegt darunter: 45,772 s gegen 51,242 s, also 10,7 % schneller.** Es gibt nichts vorzulegen.
>
> **Belegvermerk (Regel L10).** *Gemessen ist:* zwei Läufe auf derselben Instanz, derselbe Bestand,
> beide warm. *Behauptet wird:* Der gebaute Weg ist nicht teurer als der verworfene. **Die Lücke:**
> Die 51,242 s stammen aus einer anderen Sitzung an einem anderen Tag; ein Aufwärmunterschied ist
> nicht auszuschließen. Der Abstand von 10,7 % liegt in derselben Größenordnung wie die
> Aufwärmaufschläge dieser Runde (0,9 % bis 33,7 %). **Belastbar ist deshalb nur der Satz „nicht
> teurer", nicht der Satz „schneller".**

### Der Delta-Lauf, durch die ganze Anwendung

Gemessen im Datenbanktest, also einschließlich Spring, jOOQ, Netz und Schreibtransaktion:

| Fenster | Nachrichten | Rollupzeilen | Laufzeit |
|---|---:|---:|---:|
| `2025-12-30 03:00`–`05:00`, erster Lauf der JVM | 358 | 28 | 387 ms |
| dasselbe Fenster, danach | 358 | 28 | **20 bis 31 ms** |
| `2025-12-29 05:00`–`2025-12-30 05:00` | 6.248 | 570 | **129 ms** |

**Der erste Lauf einer JVM kostet das Zwölffache.** Das ist der kalte Abfrageplan, die kalte
Verbindung und die erste Transaktion — kein mengenabhängiger Anteil.

### Die Tabelle, nach dem Volllauf

| | Wert | M89 |
|---|---:|---:|
| Zeilen in `message_rollup` | **335.610** | 335.610 |
| `SUM(anzahl)` | **3.341.519** | 3.341.519 |
| verschiedene `process_id` | **738** | 738 |
| verschiedene `message_status` | **12** | 12 |
| früheste Stunde | `2024-10-01 02:00:00` | `2024-10-01 02:00:00` |
| späteste Stunde | `2026-07-08 17:00:00` | `2026-07-08 17:00:00` |
| Größe (`DATA_LENGTH` + `INDEX_LENGTH`) | 21,61 MiB | 21,59 MiB |
| **davon Indexanteil** | **0 Byte** | 0 Byte |
| `TABLE_COLLATION` | `utf8mb4_general_ci` | — |
| `rollup_lauf` | 3 Zeilen, 0,03 MiB | — |

**Alle sechs Kontrollen aus M89 treffen auf die Einheit genau.** `SUM(anzahl) = 3.341.519` heißt:
jede Nachricht ist genau einmal gezählt, keine doppelt, keine verloren.

> Die 0,02 MiB Unterschied in der Größe sind `information_schema`-Rauschen und keine Abweichung —
> dieselbe Statistik hat in M89 zwischen zwei Ablesungen um 16 % geschwankt. **Wer die Tabelle
> wirklich vermessen will, misst sie nicht über `information_schema`.**

---

## 10. Tests

### Ohne Datenbank

| Test | Was er sichert |
|---|---|
| `RollupFensterTest` (24) | Rückgriff, Ausdehnung auf ganze Stunden, leerer Wasserstand, zurückspringende Uhr, Scheibenzerlegung (lückenlos, ohne Überlapp, Summe der Eimer gleich dem Fenster), die Invarianten des Fensters |
| `RollupUhrenTest` (4) | **Die Zuordnung der beiden Uhren.** Die Uhren stehen im Test acht Monate auseinander, damit ein vertauschter Aufruf auffällt — in Produktion wäre er unsichtbar |
| `RollupStatementsTest` (6) | Das **gerenderte** Statement, Zeichen für Zeichen. Regel L7 verlangt die Messung *der* Abfrage, nicht einer ähnlichen |
| `PaketstrukturTest` (+2) | Die namentliche Ausnahme ist eng und nicht leer; nichts in `rollup`, das `jooq.glassfish` anfasst, ruft eine schreibende jOOQ-Methode auf |

### Mit Datenbank (`RollupDbIT`, `@Tag("db")`)

| Test | Was er sichert |
|---|---|
| **Idempotenz** | Derselbe Lauf zweimal über dasselbe Fenster ergibt **zeilengleich** dasselbe. **Der wichtigste Test dieses Schritts** — er ist der einzige Wächter über Löschen-und-Neuschreiben |
| **Überlapp** | Zwei aufeinanderfolgende Delta-Läufe verdoppeln den Überlapp nicht: `SUM(anzahl)` über den gesamten berührten Bereich trifft die direkte Zeilenzahl aus `Message` |
| **Statuswechsel im Eimer** | Eine Statuszeile, die die Quelle nicht mehr hergibt, bleibt nicht stehen |
| **Summenprobe** | Über ein abgeschlossenes Tagesfenster gilt `SUM(anzahl)` = direkte Zeilenzahl aus `Message` |
| **Delta gegen Voll** | Über dasselbe Fenster liefern beide zeilengleich dasselbe |
| **Wasserstand** | Ein abgebrochener Lauf hebt ihn nicht — und ein abgeschlossener mit `fehler` ebenfalls nicht |
| **Leere Stunde** | Eine Stunde ohne Verkehr erzeugt keine Zeile und hebt den Wasserstand trotzdem |
| **Anschluss** | Das Fenster des nächsten Delta-Laufs baut auf dem Wasserstand des vorigen auf, mit genau einem Eimer Rückgriff |
| **Bestandsanfang** | `MIN(MessageLastUpdate)` ist `2024-10-01 02:00:28` — weicht er ab, ist die Testkopie neu befüllt und die Zahlen dieser Datei sind neu zu erheben |

> ### Kein Mandantentrennungstest, und das ist Absicht
>
> **Schritt 10a hat keine Fläche, die einen Mandanten kennt.** `message_rollup` trägt nach E‑a keine
> Mandantenspalte, es gibt keinen Endpunkt, und der Job liest ausdrücklich über alle Mandanten. Die
> Mandantentrennung entsteht in **10b** beim Join über `ProjectMandant` — dort ist der
> Isolationstest Pflicht (Regel M4). **Er fehlt hier nicht, es gibt ihn hier nicht.**

**Was die Tests auf der Testkopie hinterlassen.** Sie schreiben ausschließlich in
`overlord_monitor`; `GlassfishDB` wird nur gelesen (Regel S1). Ihre `rollup_lauf`-Zeilen räumen sie
**namentlich** wieder ab — nur die Kennungen, die sie selbst bekommen haben. Ein Aufräumen über ein
Zeitfenster oder über die Laufart löschte fremde Zeilen. Die geschriebenen
`message_rollup`-Zeilen bleiben stehen, und das ist richtig: Sie sind korrekt gerechnet.

---

## 11. Die dritte benannte Ausnahme von Regel M2

`rollup/RollupLeseRepository` bekommt **keinen `MandantContext`** — und das bricht eine
ArchUnit-Regel, die [`mandantentrennung.md`](mandantentrennung.md) §4 durchsetzt.

**Der Rollup-Job hat keinen Mandanten.** Er liest bewusst über alle Mandanten, weil
`message_rollup` nach E‑a keinen kennt.

**Ein `MandantContext.alle()` wäre eine Lüge im Typsystem** und würde die Regel entwerten, deren
einziger Zweck es ist, dass so etwas nicht existiert. Stattdessen steht in `PaketstrukturTest` eine
**namentliche** Ausnahme: `ROLLUP_AUSNAHME = List.of("RollupLeseRepository")`. Sie nennt die Klasse
beim Namen und gilt für keine andere — **auch nicht für eine zweite im selben Paket.** Die fiele
weiter durch, bis jemand sie einträgt und begründet.

**Zwei Tests halten die Ausnahme fest, von beiden Seiten:**

- Sie ist **nicht leer** (sonst prüft sie nichts mehr und gehört gelöscht) und **nicht breiter** als
  die Liste, die sie benennt.
- Sie ist **ehrlich**: Die ausgenommene Klasse trägt den `MandantContext` in keiner Signatur. Ein
  Schein-Kontext — ein Parameter, der entgegengenommen und nicht verwendet wird — wäre schlimmer als
  gar keiner, weil er von außen wie Mandantentrennung aussieht.

**Warum Lesen und Schreiben auf zwei Klassen verteilt sind.** Der Katalog aus Schritt 9b hält beide
`DSLContext` in *einer* Repository-Klasse, und das ist dort richtig. Hier nicht: Lägen beide Hälften
in einer Klasse, fiele **auch der Schreibpfad** unter die Ausnahme, und sie wäre breiter als ihr
Grund. `RollupSchreibRepository` fasst `jooq.glassfish` gar nicht erst an.

**Was die Ausnahme nicht aufweicht:** `message_rollup` selbst enthält keine Mandantenangabe. Die
Trennung entsteht in 10b beim Join über `ProjectMandant` — unverändert im Statement, nicht
nachgelagert.

> **Eine Ungenauigkeit im Auftrag, gemeldet und nicht stillschweigend übergangen.** Er nennt diese
> Ausnahme „die dritte benannte Ausnahme des Projekts … wie die beiden Endpunkt-Ausnahmen dort".
> Das trifft seit dem 20.08.2026 nicht mehr: [`mandantentrennung.md`](mandantentrennung.md) §3
> führt **drei** Endpunkt-Ausnahmen von Regel **M1**, und diese hier ist die **zweite** Ausnahme von
> Regel **M2** (die erste ist `@OhneMandantenkontext`). Sie steht deshalb in §4 und nicht in §3 —
> die Zählung des Auftrags ist überholt, die Sache ist es nicht.

---

## 12. Regelbezug

| Regel | Stand |
|---|---|
| **L2** Keine Live-Aggregation über `Message` | **erfüllt** — das ist der Zweck dieses Schritts. 10b liest ausschließlich aus `message_rollup` |
| **L6** Der Rollup-Job läuft gedrosselt | **erfüllt beim Volllauf** (Pause zwischen den Scheiben), **beim Delta-Lauf Zeremonie** und als solche benannt (§8) |
| **L7** Jede neue Abfrage gemessen | **erfüllt** — §9, `EXPLAIN` und Laufzeit für beide Abfragen, gegen das gerenderte Statement |
| **L9** Durchlauf ohne Zeitfenster nur begründet | **erfüllt** — nur `MIN(MessageLastUpdate)`, begründet in §7, und es ist eine Indexspitze ohne Tabellenzugriff |
| **L10** Belegvermerk | **erfüllt** — drei Vermerke: Nachlauffenster (§3), Drosselung (§8), Volllauf gegen M89 (§9) |
| **S1** Kein Schreibzugriff auf `GlassfishDB` | **erfüllt** — der Job liest über `glassfishDsl` mit `ReadOnlyExecuteListener`; eine ArchUnit-Regel verbietet zusätzlich schreibende jOOQ-Aufrufe in den Rollup-Klassen, die das Quellschema anfassen |
| **M2** `MandantContext` erster Pflichtparameter | **namentliche Ausnahme**, §11 |
| **M4** Isolationstest je Endpunkt | **gegenstandslos** — 10a hat keinen Endpunkt, §10 |
| **Z1** Kein `now()` | **erfüllt** — beide Zeitpunkte aus `Clock.instant()`, ArchUnit prüft es |
| **A5** Protokollzeit aus `systemClock` | **erfüllt** — §4 |
| **Q4** Nichts raten | **eingehalten, indem das Geratene benannt ist**: Nachtlauf-Uhrzeit und Drosselung stehen als ungemessen im Code, in `application.yml` und unter den offenen Punkten |
| **S2** Flyway nur für `overlord_monitor` | **erfüllt** — `V9__message_rollup.sql` |

---

## 13. Offene Punkte

49. **Das Nachlauffenster von 15 Minuten ist gegen einen Bestand gemessen, in dem der wahrscheinlichste
    Verursacher einer Wanderung nicht läuft.** `MatchInterchange` setzt `MessageStatus =
    'COMMIT_RECEIVED'` nachträglich und ist auf der Testkopie nicht sichtbar (M31‑3, Takt
    **ungedeckt**); `RUNNING` kommt null Mal vor. **In Produktion ist zu prüfen, ob die Wanderung
    dort größer ist** — die Prüfung selbst ist einfach: dieselbe Abfrage wie M86 (b), gegen den
    Produktionsbestand. Bis dahin trägt der nächtliche Volllauf.
50. **Zwei ungemessene Vorgaben, beide in `application.yml` markiert.** (a) Die Uhrzeit des
    Nachtlaufs (`03:00`) — die Verteilung von `MessageLastUpdate` über die Tagesstunde ist nicht
    erhoben. (b) Die Drosselung (`1s` je Scheibengrenze) — wie viel Last die Produktionsinstanz
    nachts verträgt, ist nicht erhoben. Beide sind konfigurierbar, damit eine Korrektur keine
    Codeänderung kostet.
51. **[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5 beschreibt `message_rollup` weiterhin
    als „stündliche Aggregate je Mandant, Prozess, Partner, Richtung, Status".** E‑a hebt das auf,
    M89 stützt E‑a — der Widerspruch ist zugunsten von E‑a aufzulösen. **Nicht in diesem Schritt
    ausgeführt**, weil die Korrekturen aus der Messrunde eine eigene Runde sind
    ([`messungen-schritt10.md`](messungen-schritt10.md), Abschnitt „Korrekturen").
52. **Die Monatsscheibe hat Faktor 3,9 Luft zur Zeitgrenze — warm gemessen.** M44 nennt für den
    Kaltfall einen übertragenen Faktor von bis zu **9,66**; auf die größte Scheibe angewandt wären
    das rund 24,9 s und damit **über** der Grenze von 10 s. **Das ist eine Übertragung und keine
    Messung** — der Faktor stammt aus einer anderen Abfrage, und `FLUSH TABLES` steht
    `monitor_read` nicht zu. Reißt eine Scheibe in Produktion die Grenze, steht es sichtbar in
    `rollup_lauf.fehler`, und die Scheibengröße ist die Stellschraube. Sie ist heute eine Konstante
    im Code (`RollupFenster.monatsscheiben()`).
53. **Der Volllauf hält 335.610 Zeilen auf einmal im Speicher** — rund 30 MiB. Das ist die Kehrseite
    der einen Transaktion und ausdrücklich gewollt. **Für die Produktion ist es nicht
    hochgerechnet**: Wächst der Bestand um Faktor zehn, sind es 300 MiB. Die Stelle, an der dann neu
    zu entscheiden wäre, ist dieselbe wie bei Punkt 52 — die Scheibengröße —, nur mit einer anderen
    Folge: Kleinere Scheiben helfen dort nichts, weil alle Scheiben zusammen in **eine** Transaktion
    gehen.

---

## 14. Was dieser Schritt nicht zeigt

1. **Nichts über die Produktion.** Alle Zahlen stammen von der Testkopie, und M92 rechnet
   ausdrücklich nicht hoch. Weder Aufbewahrungsdauer noch Umfang noch Rückstand sind dort bekannt.
2. **Den Kaltlauf nicht.** `FLUSH TABLES` steht `monitor_read` nicht zu; der Puffer fasst `Message`
   9,26‑mal. Gemessen ist der Warmfall — mit Ausnahme des ersten Delta-Laufs einer JVM (387 ms
   gegen 20–31 ms), und der misst den kalten Abfrageplan, nicht die kalte Platte.
3. **Ob der Zeitplan trägt.** Weder der stündliche noch der nächtliche Lauf ist im Betrieb
   ausgelöst worden — sie sind von Hand gefahren. Was der Zeitplan tut, ist nicht beobachtet,
   sondern konfiguriert.
4. **Nichts über 10b.** Die Leseabfrage des Dashboards ist in M89 gemessen (11,30 ms im
   Standardfenster mit Katalog-Join), aber nicht gebaut.
