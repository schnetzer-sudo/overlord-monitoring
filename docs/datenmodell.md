# Datenmodell `GlassfishDB`

Auszug aus Abschnitt 3 der [Projektbeschreibung](PROJEKTBESCHREIBUNG.md). **Der schnelle
Nachschlageort bei jeder Abfrage.** Bei Widerspruch gilt die Projektbeschreibung.

> **Zugriff ausschließlich lesend.** Es wird niemals in dieses Schema geschrieben. Der DB-Benutzer
> für dieses Schema besitzt ausschließlich `SELECT`.

---

## 1. Hierarchie

```
Mandant ──< ProjectMandant >── Project ──< Process ──< SOS ──< SOSAction
                                            │           │
                                            └──< Message ──< MessageAction ──< MessageProperty
                                                    │
                                                    └──< MessageBAM
```

---

## 2. Die Mandantenkette

**Die wichtigste Kette des Projekts.** Die Zuordnung einer Nachricht zu einem Mandanten läuft über
vier Joins:

```
Message → Process → Project → ProjectMandant → Mandant
```

`ProjectMandant` ist eine **n:m-Beziehung** — ein Projekt kann mehreren Mandanten zugeordnet sein.

Es gibt für diese Kette eine vorhandene View **`MessageMandantID`**. **Sie wird nicht verwendet.**

> **Korrektur 06.08.2026.** Hier stand bis heute: *„Sie ist in der vorhandenen View
> `MessageMandantID` gekapselt."* Ab Schritt 4 wird die Kette **handgeschrieben als `EXISTS`**
> ausgeführt. Drei Gründe, alle gemessen:
>
> 1. **Der Zugriffspfad der View ist mit unseren Rechten strukturell nicht einsehbar.** Weder
>    `SHOW CREATE VIEW` (Fehler 1142) noch `EXPLAIN` über die View (Fehler 1345) sind möglich; beide
>    brauchen das Recht `SHOW VIEW`, das in `SELECT` nicht enthalten ist — beim Lese- wie beim
>    Schreibbenutzer, in Produktion wie auf der Testkopie. Was wir nie einsehen können, kann
>    **Regel L7 nicht erfüllen**.
> 2. Die View läuft mit `SECURITY_TYPE = DEFINER` bei `DEFINER = root` und wird als
>    `IS_UPDATABLE = YES` geführt (M4).
> 3. Die `EXISTS`-Fassung nutzt nachweislich `MessageLastUpdateIDX`, braucht kein `filesort` und
>    liefert dieselbe Menge — die Gegenprobe ergab null Abweichung.
>
> Belege in [`messungen-schritt4.md`](messungen-schritt4.md) M4, Umsetzung in
> [`nachrichtenliste.md`](nachrichtenliste.md) §3. Die View bleibt im Schema und in der
> jOOQ-Codegenerierung; sie wird nur nicht mehr als der vorgesehene Weg beschrieben.

**Diese Kette ist Bestandteil jedes Statements**, nicht nachgelagerte Prüfung (Regel M3). Wer eine
fremde `MessageID` errät, bekommt null Zeilen — weil die Zeile für ihn nie existiert hat.

Da `ProjectMandant` n:m ist, kann ein naiver Join **Zeilen vervielfachen**, sobald ein Projekt
mehreren Mandanten gehört. Der Mandantenfilter gehört deshalb als Einschränkung ins Statement
(`EXISTS` beziehungsweise Filter auf genau einen Mandanten), nicht als zusätzlicher Join, dessen
Treffer man anschließend zählt.

---

## 3. Kerntabellen

### `Message` — eine Nachricht/Übertragung

`MessageID` varchar(36) PK · `ProcessID` FK · `SOSID` · `SOSActionID` · `MessageStatus` varchar(30) ·
`MessageLastUpdate` timestamp · `MessageTimeout` smallint · `SourceMessageID` · `TargetMessageID` ·
`Source` bit · `Target` bit

⚠️ **Es gibt kein Anlagedatum.** `MessageLastUpdate` ist der Zeitpunkt der **letzten Änderung**.
Der fachliche Start ist `MIN(MessageAction.MessageActionStart)`.

⚠️ **`MessageTimeout` ist eine Dauer in Sekunden, kein Zeitpunkt.** Der Timeout-Zeitpunkt wird im
Backend als `MessageLastUpdate + MessageTimeout` berechnet.

> **Korrektur 01.08.2026.** Hier stand bis heute **„Dauer in Minuten"** — ebenso in Regel Z2 der
> Entwicklungsrichtlinien und in Abschnitt 3.3 der Projektbeschreibung. Das ist durch Messung M8
> widerlegt: `SOSActionTimeout = 1800` steht 37.120-mal neben dem Ablaufschritt `WAIT|30M`, und
> 1800 **Sekunden** sind exakt 30 Minuten; der einzige andere vorkommende Wert `300` passt zum
> Schritt `5M`. Unter der Minuten-Lesart stünde eine Frist von 30 Stunden neben einem Schritt, der
> 30 Minuten wartet. Vollständige Belegkette samt ihrer Schwachstelle in
> [`messungen-schritt4.md`](messungen-schritt4.md), Abschnitt M8.
>
> Die Größenordnung ändert sich damit um **Faktor 60**. Wer die Spalte in einer älteren Fassung als
> Minuten gelesen hat, hat die Kategorie „Überfällig" um 29,5 Stunden zu spät ausgelöst.

**Nutzbare Indizes:**

| Index | Spalten |
|---|---|
| `MessageLastUpdateIDX` | `MessageLastUpdate` |
| `MessageStatusIDX` | `MessageStatus` |
| `MessageLastUpdateProcessMessageIDX` | `(MessageLastUpdate, ProcessID, MessageID)` |

⚠️ **Vorsicht bei der Cursor-Sortierung.** Der zusammengesetzte Index enthält zwar beide Spalten
des Cursors aus Regel L3 — aber `ProcessID` steht **dazwischen**. Eine Sortierung nach
`(MessageLastUpdate, MessageID)` bedient er nur dann ordnungserhaltend, wenn `ProcessID` durch
eine Gleichheitsbedingung festgelegt ist. Welcher Zugriffspfad tatsächlich gewählt wird, steht
erst nach der Messung gegen die Testkopie fest (Regel L7) — nicht vorher und nicht durch
Hinsehen.

> **Gemessen am 06.08.2026 (Schritt 4).** Die Warnung bleibt richtig, trifft aber nicht zu: Gebraucht
> wird `MessageLastUpdateProcessMessageIDX` gar nicht. MariaDB wählt für die Liste
> `MessageLastUpdateIDX` — und der ist **faktisch `(MessageLastUpdate, MessageID)`**, weil InnoDB an
> jeden Sekundärindex den Primärschlüssel hängt und der Optimierer das nutzt
> (`optimizer_switch: extended_keys=on`). Die Cursor-Bedingung in der ODER-Form ergibt damit einen
> Bereich über **beide** Spalten (`key_len = 151`), und der Tiebreaker über `MessageID` löst **kein**
> `filesort` aus. Zahlen in [`messungen-schritt4.md`](messungen-schritt4.md) L8, Umsetzung in
> [`nachrichtenliste.md`](nachrichtenliste.md) §4.

### `MessageAction` — die einzelnen Prozessschritte

PK `(MessageID, MessageActionID)` · **`SOSID`** varchar(36) NOT NULL · **`SOSActionID`** smallint(6)
NOT NULL · `MessageActionStart` / `MessageActionEnd` timestamp · `ServiceID` varchar(36) ·
`SOSActionServiceProperties` mediumtext · `SOSActionTimeout` smallint(6)

**Nutzbare Indizes:** `PRIMARY (MessageID, MessageActionID)` und `MessageAction_MessageFK (MessageID)`
— sonst keine. Insbesondere **kein Index auf `SOSID`, `SOSActionID` oder `ServiceID`**: Jeder Zugriff
läuft von der `MessageID` aus in die kleine Zieltabelle hinein, nie umgekehrt.

> **Korrektur 07.08.2026.** Hier standen bis heute **sieben** Spalten; die Tabelle hat **neun**. Es
> fehlten `SOSID` und `SOSActionID`, beide `NOT NULL`. Der Grund für die Lücke ist der Vorgang, nicht
> der Inhalt: Diese Spaltenliste war aus der Projektbeschreibung **übernommen und nie gegen
> `information_schema` erhoben** — M1 in [`messungen-schritt4.md`](messungen-schritt4.md) hat
> `MessageAction` nicht erfasst. Gemessen wurde sie erst in
> [`messungen-schritt5.md`](messungen-schritt5.md) **M14**. Daraus folgt die neue Regel L8 in
> [`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md).
>
> **Die beiden Spalten sind nicht Beiwerk — sie tragen die Auflösung des Schrittnamens.** Der Join
> auf die Ablaufdefinition lautet:
>
> ```sql
> JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
> ```
>
> 🚫 **Niemals über `Message.SOSID`.** Messung **M15** stellt drei Fassungen nebeneinander und
> vergleicht den *ausgeführten* Baustein mit dem *geplanten*: Über `MessageAction` stimmen sie in
> **100 %** der aufgelösten Zeilen überein (366.336 von 366.343 über einen Monat, 99,998 %), über
> `Message.SOSID` mit `MessageActionID` nur in **96,17 %**. Die Auflösungsquote der drei Fassungen
> ist fast gleich — **sie belegt deshalb nichts**; nur der Markenvergleich unterscheidet den
> richtigen Join vom zufällig treffenden.
>
> ⚠️ **`MessageActionID` ist nicht `SOSActionID`.** Die erste ist eine laufende Nummer je Nachricht
> und beginnt bei **0**, die zweite ist der Schlüssel in die Ablaufdefinition. Der dritte Schritt
> einer Nachricht trägt je nach Ablauf die Kennung 2, 3 oder 10 (M15). Und `SOSAction` nummeriert
> **nicht lückenlos**: 257 von 1.777 Abläufen haben eine größte Kennung über ihrer Schrittzahl, 233
> nutzen Kennungen ab 99 (M20).

`SOSActionServiceProperties` enthält die ausgeführten Bausteine als **pipe-getrennte Liste**, etwa:

```
NXS_FILE_CONVERT|E2A|UNWRAP
NXS_MERGE|KE_OSTROV_734973|WAIT|30M|30406_..._MRG
```

⚠️ **Diese Rohwerte werden dem Nutzer nicht angezeigt**, sondern in lesbare Schritte übersetzt
(Schritt 5). Unbekannte Bausteine erscheinen als Rohwert — nie geraten.

> **Präzisiert 07.08.2026.** „Übersetzt" heißt **nicht** über eine handgepflegte Zuordnungstabelle,
> sondern über den Join oben: Der Klartext ist `SOSAction.SOSActionName`. Er erreicht 71 bis 78 % der
> echten Schritte; der Rest zeigt seinen Rohwert, und **jeder** dieser Schritte hat einen (M15, M18).
> Ob für den Rest zusätzlich übersetzt wird, ist offen — das Vokabular dafür ist mit **vier** Marken
> über einen ganzen Monat klein (M19).

### `MessageProperty` — Schlüssel/Wert-Paare je Nachricht (EAV)

PK `(MessageID, MessagePropertyName, MessageActionID)` · `MessagePropertyValue` mediumtext

Rund **23 Zeilen pro Nachricht** im dichten Bestand; **46.964.279 Zeilen, 61,0 GB** insgesamt
(Erhebung 27.07.2026). Der Index belegt davon **45,9 GB**, die Nutzdaten nur 15,1 GB — die
„1,3 Kilobyte je Zeile" aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 sind zu drei
Vierteln Index.

**Alle Eigenschaften einer Nachricht zusammen wiegen rund 595 Byte.** Das ist die Zahl, die für einen
Detail-Aufruf zählt — nicht die Speichergröße der Tabelle.

> **Korrektur 07.08.2026.** Hier stand bis heute: *„Rund zehn Zeilen pro Nachricht, bei einem Jahr
> Aufbewahrung mehrere hundert Millionen Zeilen."* Beide Hälften sind falsch:
>
> - **Zeilen je Nachricht:** gemessen **22,57** über einen Tag und **22,88** über einen Monat, Minimum
>   14, Maximum 38 ([`messungen-schritt5.md`](messungen-schritt5.md) M17). Die „rund vierzehn" der
>   Projektbeschreibung sind das Mittel über den **gesamten** Aufbewahrungszeitraum
>   (46.964.279 / 3.341.519 = 14,05); die „rund zehn" hier waren eine Schätzung ohne Messung.
> - **Gesamtzahl:** nicht „mehrere hundert Millionen", sondern **47 Millionen** — und die
>   Aufbewahrung beträgt **22 Monate**, nicht ein Jahr (§8 und `PROJEKTBESCHREIBUNG.md` §8). Die
>   Zeilenzahl war ohnehin nie die richtige Kennzahl; maßgeblich ist die Bytegröße.
>
> Die alte Angabe stammt aus der Zeit vor der Erhebung vom 27.07.2026, die das Mengengerüst um rund
> Faktor zehn nach unten korrigiert hat, und ist beim Nachziehen übersehen worden.

🚫 **Zugriff ausschließlich über `MessageID`.** Niemals filtern, gruppieren oder sortieren über
`MessagePropertyValue` — die Indizes darauf sind **Präfix-Indizes über 50 Zeichen** und für
Aggregation ungeeignet. (Regel L4)

**Bekannte Namen:**

| Name | Anmerkung |
|---|---|
| `Message.GUID` | |
| `Message.SendingPartner` | |
| `Message.SNDPRN` | |
| `Message.VFN` | |
| `Message.SOS` | |
| `Message.SplitCount` | |
| `Message.Payload.GUID` | Format `FILESTOREPROD09\|<uuid>` — Grundlage des Rohdaten-Downloads |
| `Message.InterchangeNumber` | |
| `Message.CommitInterchangeNumber` | |
| `Message.SourceMessageID` | |

### `MessageBAM` — fachliche Suchschlüssel

PK `(MessageID, MessageBAMType, MessageBAMValue)` · `MessageBAMValue` varchar(70), **eigener
Index**

Das ist die **zentrale Suchdimension für Fachanwender**: Lieferschein-Nr., Bestellnummer,
Transport-Nummer, Charge, Werk, Materialnummer und so weiter. `MessageBAMType` verweist auf
`MessageBAMType.MessageBAMTypeDescription`.

⚠️ **Hartes Ergebnislimit und Mindestlänge des Suchbegriffs** sind Pflicht. Werte wie `050` kommen
millionenfach vor. (Regel L5)

### `MessageBAMMandant` — sichtbare BAM-Typen je Mandant

Steuert je Mandant, welche BAM-Typen sichtbar sind und in welcher Reihenfolge
(`MessageBAMTypeSortIndex`). **Diese Konfiguration wird für die Spaltenauswahl und die Suchfelder
übernommen, nicht neu erfunden.**

### `Process` / `Project` / `Mandant` / `ProjectMandant`

Die Hierarchie. Reine Stammdaten.

### `SOS` / `SOSAction`

„Sequence of Services" — der konkrete, aus Bausteinen zusammengesetzte Ablauf.

**`SOSName` ist bereits in Klartext gepflegt** („Lieferabruf von AMG (VDA)", „Eingehender IFTMIN
BAYER") und wird als **Anzeigename** verwendet. Das Verhältnis Process zu SOS ist meist 1:1,
gelegentlich 1:n (Varianten wie `_OUT`, `_MAIL`).

### `User` — Alt-Benutzertabelle

`UserID` (Benutzername und Schlüssel) · `MandantID` · `UserRole` mit den Werten `Admin` und `User` ·
`UserPassword` varchar(20) im **Klartext**

🚫 **Es wird nichts übernommen. Die Tabelle ist reine Nachschlagequelle.** Aus ihr wird zur Laufzeit
nicht gelesen — weder Benutzername noch Mandant noch Rolle, und `UserPassword` schon gar nicht.
Konten entstehen einzeln über `POST /api/admin/users`, das erste über das Profil `bootstrap`.
Vollständig in [`authentifizierung.md`](authentifizierung.md) §8 und Abschnitt 7 der
[Projektbeschreibung](PROJEKTBESCHREIBUNG.md).

> **Korrektur 01.08.2026.** Hier stand bis heute: *„Benutzername, Mandant und Rolle werden
> übernommen, die Passwörter nicht … Alle migrierten Konten starten gesperrt mit Zwang zur
> Neuvergabe."* Dieser Stand ist seit der **Entscheidung vom 28.07.2026 widerrufen** — der
> Migrationslauf entfällt **ersatzlos**, es gibt keine migrierten Konten. Die drei Gründe
> (die Tabelle bleibt dauerhaft lesbar, eine Übernahme wäre nur ein vorgezogener `SELECT`; alle
> 36 Konten wären einzeln freizuschalten gewesen; `Admin` bedeutet im Altsystem etwas anderes, weil
> dort auch jeder Admin eine `MandantID` trägt) stehen in `authentifizierung.md` §8. Der überholte
> Satz wird benannt und nicht stillschweigend ersetzt: Wer ihn in einem älteren Stand liest, soll
> hier finden, dass er zurückgenommen wurde.

### `MessageStatisticHistory` und View `MessageStatistic`

Bestehende Aggregation des Altsystems. Der Schlüssel `Period` ist ein **zusammengesetzter String**
der Form `<MandantID>&&<YYYYMMDDHH>&&HOUR` bzw. `&&DAY` oder `&&MONTH`.

Liefert nur **Anzahlen**, keine Aufschlüsselung nach Status oder Partner — deshalb das eigene
`message_rollup` in `overlord_monitor`.

### `Service`

Trägt den `ServiceConnectString`. Über ihn wird die `FilestoreID` aus `Message.Payload.GUID`
(Servicetyp „TOMCAT Filestore") zum konkreten Filestore aufgelöst. Grundlage des
Rohdaten-Download-Proxys.

---

## 4. Nachrichtenstatus

| Status | Bedeutung | Anzeige |
|---|---|---|
| `ERROR_*` | **Fehler.** Alles mit Präfix `ERROR_`. Der Teil dahinter ist die Fehlerart. | rot |
| `SUSPENDED` | Wartet, zum Beispiel auf Zusammenführung. **Kein Fehler.** | neutral |
| `RUNNING` | Läuft gerade | neutral |
| `FINISHED` | Abgeschlossen | grün |
| `SPLITTED` / `MERGED` | Aufgeteilt bzw. zusammengeführt, Verkettung beachten | neutral |
| `COMMIT_RECEIVED` / `EERP_RECEIVED` | Empfangsbestätigung des Partners liegt vor | grün |

Die tatsächlich vorkommenden Statuswerte werden einmalig per `SELECT DISTINCT MessageStatus`
erhoben und in `docs/message-status-werte.md` dokumentiert (Schritt 3, prüft Annahme A6).

### Die drei Problemkategorien

Bewusst getrennt, **dürfen nie zu „Fehler" zusammengefasst werden**:

1. **Fehler** — `MessageStatus` beginnt mit `ERROR_`
2. **Überfällig** — läuft noch und `MessageLastUpdate + MessageTimeout` liegt in der Vergangenheit
3. **Unquittiert** — ausgehende Nachricht ohne zugeordnete Empfangsbestätigung

---

## 5. Fallstricke

### 5.1 `LIKE 'ERROR_%'` ist falsch

In SQL ist `_` ein Platzhalter für ein **beliebiges Zeichen**. `LIKE 'ERROR_%'` trifft auch
`ERRORX…`. Verwendet wird:

```sql
LEFT(MessageStatus, 6) = 'ERROR_'
```

Das nutzt `MessageStatusIDX` nicht direkt — falls die Messung ein Problem zeigt, stattdessen:

```sql
MessageStatus LIKE 'ERROR\_%' ESCAPE '\'
```

Nicht die naive Variante. (Regel Q1)

### 5.2 Es gibt keinen Anlagezeitpunkt

Siehe `Message`. Wer „seit wann läuft das" beantworten will, braucht
`MIN(MessageAction.MessageActionStart)` — nicht `MessageLastUpdate`.

### 5.3 `MessageTimeout` ist eine Dauer

**Sekunden**, kein Zeitpunkt. Wer die Spalte als Zeitstempel liest, bekommt Unsinn — und wer sie als
Minuten liest, rechnet um Faktor 60 daneben (Korrektur 01.08.2026, siehe oben).

In der Testkopie kommen genau zwei Werte vor: `1800` (99,79 %) und `0` (0,21 %), **niemals `NULL`**.
`0` wird als „kein Timeout" behandelt.

⚠️ **`ERROR_TIMEOUT` entsteht nicht aus dieser Spalte.** Die 52 so gekennzeichneten Nachrichten
laufen 2 bis 5,6 Minuten und brechen höchstens 120 Sekunden nach dem Start ihrer letzten Aktion ab —
das ist eine kürzere Frist auf Dienstebene. Wer die 52 Zeilen als Beispiele für ein abgelaufenes
`MessageTimeout` liest, liest sie falsch (M8).

### 5.4 `MessageProperty` ist EAV und riesig

**47 Millionen Zeilen, 61,0 GB** — davon 45,9 GB Index. Präfix-Indizes über 50 Zeichen, und zwar
**zwei**: `MessagePropertyValueIDX (MessagePropertyValue(50))` und
`MessagePropertyNameValueIDX (MessagePropertyName, MessagePropertyValue(50))`. Nur über `MessageID`
zugreifen.

> **Korrektur 07.08.2026.** Hier stand „Hunderte Millionen Zeilen" — es sind 47 Millionen. Die
> Präfixlänge 50 ist dagegen **gemessen und bestätigt** (`SUB_PART = 50`,
> [`messungen-schritt5.md`](messungen-schritt5.md) M14). Dass der Index dreimal so viel Platz braucht
> wie die Nutzdaten, ist der eigentliche Grund für Regel L4: Eine Aggregation über den Wert wälzt
> 61 GB um, nicht 15.

### 5.5 `MessageStatistic.Period` ist ein zusammengesetzter String

`<MandantID>&&<YYYYMMDDHH>&&HOUR`. Kein Datum, keine Zahl — String-Zerlegung nötig, wenn diese
Tabelle überhaupt angefasst wird.

### 5.6 Partner, Standort, Richtung und Belegart sind keine Daten

**Diese vier Angaben sind in `GlassfishDB` nicht als Daten vorhanden.** Sie stecken in
Namenskonventionen, und die Konventionen unterscheiden sich je Mandant:

- **NEXANS**: Projekt trägt Richtung und Kategorie (`300_KundenEingehend`), Prozess trägt Partner
  und Belegart (`40000_AMG_LAB_VDA`)
- **VTG / Suttons**: Projekt trägt Geschäftsbereich und Partner (`100_VTG_BAYER`,
  `100_SUTTONS_BAYER`), die Richtung steht nur im `SOSName`
- Partnernamen sind **nicht token-sauber**: `KE_OSTROV`, `DAS_DRAEXLMAIER`, `TYCO_AMP`,
  `DELFINGEN_DE_HA` bestehen aus mehreren Teilen
- Granularität ist eine **fachliche** Frage: `BASF`, `BASFANTWERPEN`, `BASFPOLY`, `NONBASF`

🚫 **Es wird nicht geparst, es wird kuratiert.** Partner, Standort, Richtung und Belegart sind
Felder im `process_catalog`. Eine Heuristik befüllt vor, die Wahrheit steht im Katalog. Nicht
zugeordnete Prozesse erscheinen in Auswertungen sichtbar als **„nicht zugeordnet"** — niemals als
geratener Wert. (Regel Q4)

**Sonderfall `00001_Undefined`**: ein Auffangprozess, in der Oberfläche gesondert behandelt.

### 5.7 Die COMMIT-Zuordnung ist bis zu eine Stunde verzögert

Siehe Datenbank-Events. Das muss in der Oberfläche kommuniziert werden, sonst wirkt eine korrekt
übertragene Nachricht wie unquittiert.

---

## 6. Datenbank-Events

Laufen weiter, **gehören uns nicht**:

| Event | Takt | Wirkung |
|---|---|---|
| `CreateMessageStatisticHistory` | täglich | füllt `MessageStatisticHistory` |
| `MatchInterchange` | stündlich | ordnet COMMITs über Interchange-Nummern zu, setzt `MessageStatus = 'COMMIT_RECEIVED'` und `SourceMessageID` |
| `SetTargetFlag` | — | setzt `Target`-Flag |
| `MoveDTNA997` | stündlich | verschiebt Nachrichten zwischen zwei Prozessen (kundenspezifisch) |

---

## 7. Verkettung (Lineage)

Eine Nachricht ist selten allein. Über `SourceMessageID`, `TargetMessageID` sowie die Flags
`Source` und `Target` entsteht eine Kette: eine eingehende Sammelnachricht wird gesplittet, die
Teile werden verarbeitet, mehrere werden zusammengeführt, am Ende kommt eine Quittung zurück.

Für den Nutzer ist genau das die Antwort auf „wo ist mein Lieferschein". Die Verkettung ist deshalb
**MVP-Bestandteil, nicht Ausbaustufe**.

Der Mandantenfilter gilt auch für verkettete Nachrichten (Regel M5), und die Tiefe wird begrenzt,
damit Endlosketten nicht auflaufen.

---

## 8. Größenordnung

Gemessenes Mengengerüst (27.07.2026, ersetzt die frühere Schätzung):

| Tabelle | Zeilen | Größe |
|---|---|---|
| `MessageProperty` | 46.964.279 (geschätzt) | 61,0 GB — davon **45,9 GB Index** |
| `MessageBAM` | 10.859.666 | 7,1 GB |
| `MessageAction` | **10.308.590 (gezählt)** | 3,0 GB |
| `Message` | 3.341.519 | 2,9 GB |
| `Process` | 1.490 | — |
| `Project` | 140 (gezählt) | — |

| Kennzahl | Wert |
|---|---|
| Nachrichten pro Tag | **rund 7.300 im dichten Bestand** (nicht 5.000) |
| Aufbewahrung | **22 Monate** (ältester Datensatz 01.10.2024) |
| Prozesse | 1.490 (Annahme A7 bestätigt) |

> **Korrektur 07.08.2026, zwei Zeilen.**
>
> **„Rund 5.000 Nachrichten pro Tag" beschreibt einen Durchschnitt, den es an keinem einzigen Tag
> gab.** Im dichten Teil sind es **rund 7.300** — 3.336.386 Zeilen über 456 Tage (01.10.2024 bis
> 30.12.2025). Die 5.000 entstehen, wenn man den gesamten Zeitraum **einschließlich der
> fünfmonatigen Lücke** durch die Tage teilt. Gemessen in
> [`messungen-schritt4.md`](messungen-schritt4.md), Auffälligkeit A; in
> [`annahmen-korrekturen.md`](annahmen-korrekturen.md) seit dem 01.08.2026 festgehalten, hier bis
> heute nicht nachgezogen.
>
> **Nachtrag vom selben Tag:** [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 hat die Zahl
> ebenfalls getragen und ist inzwischen nachgezogen — dort mit eigener, datierter Begründung. Damit
> nennen alle drei Dateien dieselbe Zahl; die Korrektur ist abgeschlossen und nicht mehr offen.
>
> **`MessageAction` ist gezählt worden und hat 10.308.590 Zeilen**, nicht die geschätzten 10.215.743
> ([`messungen-schritt5.md`](messungen-schritt5.md) M14). Bemerkenswert ist die Richtung: Bei
> `Message` **über**schätzt `information_schema` um 6,5 %, bei `MessageAction` **unter**schätzt es um
> 0,9 %. „Veraltet" heißt also nicht „zu hoch", sondern nur „unzuverlässig".

Die **Zeilenzahl war nie die richtige Kennzahl.** `MessageProperty` belegt 61 GB und ist damit 82 %
der Datenbank; dort entscheidet die Bytegröße. Die frühere Annahme (36 Mio. Zeilen in `Message`,
mehrere hundert Millionen in `MessageProperty`) ist überholt — siehe
[`annahmen-korrekturen.md`](annahmen-korrekturen.md).

**Gelesen wird zur Laufzeit auf der Produktionsdatenbank.** Eine laufend aktualisierte Replica
existiert nicht. Deshalb sind die Leistungsregeln L1 bis L6 verbindlich und nicht verhandelbar.

| Umgebung | Inhalt | Verwendung |
|---|---|---|
| **Testkopie** | Vollkopie der Produktion, Datenstand **08.07.2026** | Entwicklung, Tests, Messung von Abfrageplänen |
| **Produktion** | Live | Laufzeitdatenquelle der Anwendung |

⚠️ Weil die Testkopie hinter der realen Uhrzeit zurückliegt (am 28.07.2026 rund 19 Tage), liefert ein
Standard-Zeitfenster von 24 Stunden dort null Zeilen. Deshalb wird `LocalDateTime.now()` nirgends
direkt aufgerufen — stattdessen die Anwendungsuhr (`Clock`) aus `common` (Regel Z1). Siehe
[`datenzugriff.md`](datenzugriff.md).
