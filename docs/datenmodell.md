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

Sie ist in der vorhandenen View **`MessageMandantID`** gekapselt. `ProjectMandant` ist eine
**n:m-Beziehung** — ein Projekt kann mehreren Mandanten zugeordnet sein.

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

⚠️ **`MessageTimeout` ist eine Dauer in Minuten, kein Zeitpunkt.** Der Timeout-Zeitpunkt wird im
Backend als `MessageLastUpdate + MessageTimeout` berechnet.

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

### `MessageAction` — die einzelnen Prozessschritte

PK `(MessageID, MessageActionID)` · `MessageActionStart` / `MessageActionEnd` · `ServiceID` ·
`SOSActionServiceProperties` mediumtext · `SOSActionTimeout`

`SOSActionServiceProperties` enthält die ausgeführten Bausteine als **pipe-getrennte Liste**, etwa:

```
NXS_FILE_CONVERT|E2A|UNWRAP
NXS_MERGE|KE_OSTROV_734973|WAIT|30M|30406_..._MRG
```

⚠️ **Diese Rohwerte werden dem Nutzer nicht angezeigt**, sondern in lesbare Schritte übersetzt
(Schritt 5). Unbekannte Bausteine erscheinen als Rohwert — nie geraten.

### `MessageProperty` — Schlüssel/Wert-Paare je Nachricht (EAV)

PK `(MessageID, MessagePropertyName, MessageActionID)` · `MessagePropertyValue` mediumtext

Rund **zehn Zeilen pro Nachricht**, bei einem Jahr Aufbewahrung **mehrere hundert Millionen
Zeilen**.

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

`UserPassword` varchar(20) im **Klartext**.

🚫 **Wird nicht weiterverwendet.** Benutzername, Mandant und Rolle werden übernommen, die
Passwörter **nicht** — sie gelten als kompromittiert. Alle migrierten Konten starten gesperrt mit
Zwang zur Neuvergabe. (Abschnitt 7 der Projektbeschreibung)

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

Minuten, kein Zeitpunkt. Wer die Spalte als Zeitstempel liest, bekommt Unsinn.

### 5.4 `MessageProperty` ist EAV und riesig

Hunderte Millionen Zeilen, Präfix-Indizes über 50 Zeichen. Nur über `MessageID` zugreifen.

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
| `MessageProperty` | 46.964.279 | 61,0 GB |
| `MessageBAM` | 10.859.666 | 7,1 GB |
| `MessageAction` | 10.215.743 | 3,0 GB |
| `Message` | 3.341.519 | 2,9 GB |
| `Process` | 1.490 | — |
| `Project` | 142 | — |

| Kennzahl | Wert |
|---|---|
| Nachrichten pro Tag | rund 5.000 |
| Aufbewahrung | **22 Monate** (ältester Datensatz 01.10.2024) |
| Prozesse | 1.490 (Annahme A7 bestätigt) |

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
