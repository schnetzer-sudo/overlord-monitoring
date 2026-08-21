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

**Nutzbare Indizes:** `Message` trägt **acht**.

| Index | Spalten |
|---|---|
| `PRIMARY` | `MessageID` |
| `MessageLastUpdateIDX` | `MessageLastUpdate` |
| `MessageStatusIDX` | `MessageStatus` |
| `MessageLastUpdateProcessMessageIDX` | `(MessageLastUpdate, ProcessID, MessageID)` |
| `Message_ProcessFK` | `ProcessID` |
| `ProejctIDIDX` *(sic)* | `ProcessID` |
| `SourceMessageIDIDX` | `SourceMessageID` |
| `TargetMessageIDIDX` | `TargetMessageID` |

> **Bewacht seit dem 21.08.2026 (E37).** **Verbindlich für die Indexlisten dieses Abschnitts ist die
> Datenbank**, nicht dieser Text. Die Sollliste liegt in
> [`backend/src/test/resources/indizes-sollliste.txt`](../backend/src/test/resources/indizes-sollliste.txt)
> — aus `information_schema.STATISTICS` erhoben, nicht übernommen — und wird von
> **`IndexbestandDbIT`** bewacht, **in beide Richtungen**: Der Test wird auch dann rot, wenn die
> Datenbank einen Index trägt, den die Sollliste **nicht** führt. Das ist der Fall, der hier
> eingetreten war; in der Datenbank hatte nichts gefehlt.
>
> Diese Beschreibung bleibt stehen und bleibt nützlich — sie ist nur nicht die Quelle. Dieselbe
> Festlegung als **Ausnahme für Indizes** in der Präambel von
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md).

> **Und warum es die Sollliste zusätzlich zu `Indexes.java` gibt** (E40, 21.08.2026): Die
> generierte
> [`Indexes.java`](../backend/src/main/generated-java/de/kraftwerkone/overlord/monitor/jooq/glassfish/Indexes.java)
> trägt den vollständigen Bestand des Quellschemas seit Schritt 2 — mehr Indizes als die
> Sollliste, und ohne Zutun aktuell. Sie leistet trotzdem nichts für diese Frage: **Sie zieht bei
> jedem Codegen still nach und kann deshalb nie rot werden.** Sie ist ein Abbild, die Sollliste
> ist die geprüfte Fassung. Die Doppelung ist der Zweck und kein Versehen.

> **Korrigiert 20.08.2026.** Hier standen **fünf** Indizes, es sind **acht**. Nachgetragen sind
> `PRIMARY`, **`Message_ProcessFK`** und **`ProejctIDIDX`** — die beiden letzten sind
> **eigenständige Indizes auf `ProcessID`**, und dass sie fehlten, hat gewirkt: Ein Arbeitsauftrag
> zu Schritt 9b trug am 20.08.2026 ein Abbruchkriterium, das auf ihrer Abwesenheit beruhte. Der
> Indexname `ProejctIDIDX` ist im Altsystem so geschrieben — Buchstabendreher inbegriffen — und er
> steht auf `ProcessID`, nicht auf `ProjectID`. Bestand, kein Tippfehler dieser Datei.
> Dieselbe Korrektur in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2; erhoben in
> [`messungen-schritt9.md`](messungen-schritt9.md), V2, gemessen bereits in
> [`messungen-schritt4.md`](messungen-schritt4.md) M1 vom 01.08.2026.

> **Die beiden Verkettungsindizes sind am 10.08.2026 nachgetragen**
> ([`messungen-schritt6.md`](messungen-schritt6.md) M23‑1). Sie standen hier nicht, und ob es sie
> gibt, war nirgends dokumentiert — die Gestalt der Ketten-Auflösung in Schritt 6 hing daran. Beide
> decken die **volle** Spaltenlänge ab (`SUB_PART` leer, anders als bei `MessageProperty`). Die
> Kinder einer Wurzel kosten damit einen `ref`-Zugriff.

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

Rund **23 Zeilen pro Nachricht** im dichten Bestand (M17) — und **22,62** über den Gesamtbestand,
seit die Zeilenzahl gezählt ist (M44). **75.571.462 Zeilen (gezählt, 12.08.2026), 61,0 GB**
insgesamt; die Bytegröße stammt aus der Erhebung vom 27.07.2026 und ist seither über fünf Messtage
**byteidentisch**. Der Index belegt davon **45,9 GB**, die Nutzdaten nur 15,1 GB — die
„1,3 Kilobyte je Zeile" aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 sind
**808 Byte** und weiterhin **zu drei Vierteln Index** (608 B Index gegen 200 B Daten, 75,3 %).

*Korrigiert 12.08.2026:* Hier standen **46.964.279 Zeilen (Erhebung 27.07.2026)**. Das war die
`information_schema`-Schätzung und lag **60,9 % zu niedrig**; gezählt in
[`messungen-schritt7.md`](messungen-schritt7.md) M44. Die Bytegrößen sind unberührt. Vollständig im
Kasten in §8.

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
>
> ⚠️ **Nachtrag 12.08.2026: Die Erklärung „verschiedene Nenner" ist widerlegt.** Der Absatz oben
> deutet die Lücke zwischen 14,05 (Gesamtbestand) und 22,57 (dichter Bestand) als Zeitraumeffekt.
> Das war plausibel und ist falsch: Die 14,05 sind aus der **Schätzung** gerechnet. Mit der
> gezählten Zeilenzahl ergeben sich **75.571.462 / 3.341.519 = 22,62** — also praktisch derselbe
> Wert wie im dichten Bestand. **Ebenso überholt ist die Gesamtzahl in der zweiten Zeile:** nicht
> 47 Millionen, sondern **75,6 Millionen**. Der Kasten bleibt stehen, weil sein Kern richtig ist —
> „rund zehn" und „mehrere hundert Millionen bei einem Jahr" waren beide falsch, und die Aufbewahrung
> beträgt 22 Monate. Falsch sind nur die beiden Zahlen, die er aus der Schätzung gerechnet hat.
> Belege in [`messungen-schritt7.md`](messungen-schritt7.md) M44.

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
| `Message.Payload.GUID` | Format `<Ablagenkennung>\|<UUID>`, etwa `FILESTOREPROD09\|d95499ff-...`. **Kein Artefakt** — siehe Kasten unten |
| `Message.InterchangeNumber` | |
| `Message.CommitInterchangeNumber` | |
| `Message.SourceMessageID` | |
| `Message.DestinationFilename` | **Ergänzt 08.09.2026.** Als Suchfeld für `NEXANS` konfiguriert seit dem Handabgleich von `MessagePropertySearchListEntry` mit der Produktion (M161), gemessen in M162 bis M167 ([`messungen-property-suche.md`](messungen-property-suche.md), Nachtrag; offener Punkt 151 dort): 20.765 Zeilen in Fenster B, 3 bis 73 Zeichen (M56), 14,518 % Deckung über die Wurzeln bei `NEXANS` (M162). **Fachliche Bedeutung in keiner Projektdatei belegt.** Dieselbe Ergänzung in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 |

> **Korrigiert 20.08.2026, nachgetragen zur Korrektur vom 19.08.2026.** Die Zeile zu
> `Message.Payload.GUID` lautete bis heute: „Format **`FILESTOREPROD09|<uuid>`** — **Grundlage des
> Rohdaten-Downloads**". Beide Hälften sind berichtigt.
>
> **1. Er ist die Grundlage von nichts.** Der Rohdatenzugriff läuft über `<Dienst>.Payload.GUID` und
> `<Dienst>.Log.GUID` ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §7); `Message` ist kein
> Dienst. Nach **M73** trägt dieser Name in **6.249 von 6.249** Nachrichten (Fenster A) und
> **214.330 von 214.330** (Fenster B) denselben Verweis wie die Nutzdatenzeile mit dem **höchsten
> `MessageActionID`** derselben Nachricht — kein Gegenfall. Er benennt kein eigenes Artefakt und ist
> seit dem 19.08.2026 aus der Artefaktliste ([`rohdaten-backend.md`](rohdaten-backend.md) §7).
>
> **2. Die Formatangabe nannte eine konkrete Ablage als Format.** `FILESTOREPROD09` ist *eine*
> `Service.ServiceID` von mehreren; die Ablage hängt am Zeitraum, und Kreuzabrufe scheitern
> ausnahmslos (M53, M68). Dieselbe Präzisierung steht seit dem 19.08.2026 in
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2.
>
> *Ausdrücklich nicht behauptet:* dass „höchster `MessageActionID`" gleichbedeutend mit „zeitlich
> zuletzt" ist. Gemessen ist die Schrittnummer, nicht die Uhr.
>
> **Unberührt bleibt** die Form des Werts selbst — `<Ablagenkennung>|<UUID>`, durchgängig, 52
> Zeichen (M54) — und dass die Kennung vor der Pipe eine `Service.ServiceID` **ist** (M52).

### `MessageBAM` — fachliche Suchschlüssel

PK `(MessageID, MessageBAMType, MessageBAMValue)` · `MessageBAMValue` varchar(70), **eigener
Index**

Das ist die **zentrale Suchdimension für Fachanwender**: Lieferschein-Nr., Bestellnummer,
Transport-Nummer, Charge, Werk, Materialnummer und so weiter. `MessageBAMType` verweist auf
`MessageBAMType.MessageBAMTypeDescription`.

**Gezählt am 11.08.2026: 15.406.350 Zeilen**, 7,1 GB, rund **fünf** Einträge je Nachricht
(15.406.350 / 3.341.519 = 4,61). Erhoben in
[`messungen-schritt7.md`](messungen-schritt7.md) M32 (Spalten und Indizes) und M33‑0 (Zeilenzahl) —
die Erhebung, die Regel L8 für diese Tabelle nachgeholt hat. M32 hat die drei Angaben der Zeile
darüber **bestätigt**: Primärschlüssel, `varchar(70)` und ein eigener Index auf dem Wert; der Index
ist ein **Vollindex** über 70 Zeichen und kein Präfixindex.

> **Nachgetragen 21.08.2026 (E37).** Der Satz „M32 hat die drei Angaben der Zeile darüber
> **bestätigt**" ist wahr und hat trotzdem geschadet: Er gibt einer Liste aus **zwei** Indizes das
> Ansehen einer Erhebung über **fünf**.
>
> - Die Bestätigung bezog sich auf drei **Angaben** — Primärschlüssel, `varchar(70)`, eigener Index
>   auf dem Wert — und **nicht** auf die **Anzahl** der Indizes.
> - M32 hat **fünf** erhoben, jeden mit Spalten, `SUB_PART` und Kardinalität: `PRIMARY`,
>   `MessageBAM_MessageID`, `MessageBAM_BAMValue`, `MessageBAM_MessageFK` und
>   `MessageBAM_BAMValueOnly`. Die Zeile darüber nennt **zwei**.
> - **M32 hat nichts verloren.** Die zweizeilige Fassung ist älter als die Messung: Sie steht hier
>   und in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 seit dem **24.07.2026**, also
>   achtzehn Tage vor M32 vom **11.08.2026**. Nicht die Messung war unvollständig, sondern ihre
>   Übernahme.
> - **Derselbe Vorgang wie bei `Message`** weiter oben in diesem Abschnitt — mit dem Unterschied,
>   auf den es hier ankommt: Bei `Message` hat die Übernahme **nie** stattgefunden, die Liste blieb
>   sichtbar unbelegt. Hier hat sie **stattgefunden** und die unvollständige Liste dabei
>   **festgeschrieben**. Wer den Bestätigungssatz liest, hält die zwei Zeilen seither für geprüft.
>
> Der Satz bleibt stehen, er ist nicht falsch. Verbindlich für die Anzahl ist ohnehin die Sollliste
> in
> [`backend/src/test/resources/indizes-sollliste.txt`](../backend/src/test/resources/indizes-sollliste.txt).

⚠️ **Hartes Ergebnislimit und Mindestlänge des Suchbegriffs** sind Pflicht. Werte wie `050` kommen
millionenfach vor. (Regel L5) — **gemessen unterlegt seit M33:** Das Maximum liegt bei **234.159**
Treffern für einen einzigen Wert, das 99. Perzentil bei 75, der Median bei 1.

> **Ergänzt 12.08.2026.** Dieser Absatz stand hier bis heute **ohne Zeilenzahl** — die 10.859.666
> standen nur in §8 und in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 und §8. Er
> bekommt sie jetzt in der **gezählten** Fassung und mit ihrer Herkunft, damit dieselbe Lücke nicht
> ein zweites Mal entsteht. Zur alten Zahl siehe den Korrekturkasten in §8.

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

Trägt den `ServiceConnectString`. Über ihn wird die Ablagenkennung vor der Pipe — sie **ist** eine
`Service.ServiceID` (M52) — zum konkreten Filestore aufgelöst (Servicetyp „TOMCAT Filestore").
Grundlage des Rohdatenzugriffs.

> **Korrigiert 20.08.2026, nachgetragen zur Korrektur vom 19.08.2026.** Hier stand bis heute: „Über
> ihn wird die **`FilestoreID` aus `Message.Payload.GUID`** (Servicetyp „TOMCAT Filestore") zum
> konkreten Filestore aufgelöst. Grundlage des **Rohdaten-Download-Proxys**." Drei Berichtigungen in
> zwei Sätzen:
>
> - **Der Beispielname ist der falsche.** Aufgelöst werden die Kennungen aus
>   `<Dienst>.Payload.GUID` und `<Dienst>.Log.GUID`. `Message.Payload.GUID` ist nach **M73** kein
>   Artefakt und aus der Liste entfallen (siehe Kasten bei „Bekannte Namen" oben).
> - **`FilestoreID` ist kein Spaltenname.** Die Kennung **ist** eine `Service.ServiceID`, gemessen
>   und über den Primärschlüssel erreicht (M52) — kein zweiter Begriff für dieselbe Sache.
> - **Ein Proxy ist es nicht.** Die Ablage spricht SOAP und liefert ein ZIP; das Backend liest,
>   entpackt und gibt neu aus ([`rohdaten.md`](rohdaten.md) §2.3 und §4). Dieselbe Berichtigung
>   steht seit dem 19.08.2026 in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §7.

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

🚫 **Es wird nicht geparst, es wird kuratiert.** Partner und Richtung sind
Felder im `process_catalog`. Eine Heuristik befüllt vor, die Wahrheit steht im Katalog. Nicht
zugeordnete Prozesse erscheinen in Auswertungen sichtbar als **„nicht zugeordnet"** — niemals als
geratener Wert. (Regel Q4)

*Korrigiert 20.08.2026:* Hier standen **vier** kuratierte Felder — „Partner, Standort, Richtung und
Belegart“. Standort und Belegart entfallen; im MVP liest sie nichts. Die Überschrift dieses
Abschnitts und der Satz „Diese vier Angaben“ bleiben stehen: Sie sagen, was in `GlassfishDB`
**nicht als Daten vorhanden** ist, und das gilt unverändert für alle vier. Kuratiert werden nur
zwei davon. Dieselbe Korrektur in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.4.

**Sonderfall `00001_Undefined`**: ein Auffangprozess, in der Oberfläche gesondert behandelt.

### 5.7 Die COMMIT-Zuordnung ist bis zu eine Stunde verzögert

Siehe Datenbank-Events. Das muss in der Oberfläche kommuniziert werden, sonst wirkt eine korrekt
übertragene Nachricht wie unquittiert.

### 5.8 `bit(1)` lässt sich nicht summieren

`Message.Source` und `Message.Target` sind `bit(1)` mit Vorgabe `b'0'`. `SUM(Source)` liefert dort
**keinen brauchbaren Wert** — summiert wird die Bitfolge, nicht die Zahl. Für jede Auszählung:

```sql
SUM(Source + 0)
```

Verifiziert am 10.08.2026 ([`messungen-schritt6.md`](messungen-schritt6.md) M23‑2). Im
Anwendungscode tritt das nicht auf — der jOOQ-Codegen bildet `bit(1)` per `forcedType` auf `Boolean`
ab ([`datenzugriff.md`](datenzugriff.md) §9) —, wohl aber in jeder Erhebung von Hand.

### 5.9 Unbelegte Verkettung ist `NULL`, nie leerer String

`SourceMessageID` und `TargetMessageID` sind unbelegt **ausnahmslos `NULL`**; über beide
Bezugsfenster von M23‑2 (6.249 und 214.330 Zeilen) kommt kein einziger leerer String vor. Die
zusätzliche Bedingung `<> ''` ist damit folgenlos — sie bleibt in den Statements trotzdem stehen,
weil die Produktion sich nicht daran halten muss, was die Testkopie zufällig enthält.

---

## 6. Datenbank-Events

Laufen weiter, **gehören uns nicht**:

| Event | Takt | Wirkung |
|---|---|---|
| `CreateMessageStatisticHistory` | täglich | füllt `MessageStatisticHistory` |
| `MatchInterchange` | ⚠️ **„stündlich" — ungedeckt** (M31‑3, 11.08.2026) | ordnet COMMITs über Interchange-Nummern zu, setzt `MessageStatus = 'COMMIT_RECEIVED'` und `SourceMessageID` |
| `SetTargetFlag` | — | setzt `Target`-Flag |
| `MoveDTNA997` | stündlich | verschiebt Nachrichten zwischen zwei Prozessen (kundenspezifisch) |

> **Gekennzeichnet am 11.08.2026.** Die Tabelle ist aus
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3 übernommen, und dort steht auch die
> Begründung: M31‑3 hat den Takt von der Testkopie aus **nicht** belegen können — die direkte
> Auskunft ist durch fehlende Rechte verschlossen, und die Wirkung des Events lässt sich nicht von
> der Ballung des gewöhnlichen EDI-Verkehrs unterscheiden. **Gestrichen wird die Angabe nicht**
> (die Messung kann den Takt zeigen, aber nicht ausschließen); ihre Quelle liegt beim Altsystem.
> Die Kennzeichnung steht hier nur, damit die beiden Tabellen nicht auseinanderlaufen.

---

## 7. Verkettung (Lineage)

Eine Nachricht ist selten allein. Über `SourceMessageID`, `TargetMessageID` sowie die Flags
`Source` und `Target` entsteht eine Kette: eine eingehende Sammelnachricht wird gesplittet, die
Teile werden verarbeitet, mehrere werden zusammengeführt, am Ende kommt eine Quittung zurück.

Für den Nutzer ist genau das die Antwort auf „wo ist mein Lieferschein". Die Verkettung ist deshalb
**MVP-Bestandteil, nicht Ausbaustufe**.

Der Mandantenfilter gilt auch für verkettete Nachrichten (Regel M5), und die Tiefe wird begrenzt,
damit Endlosketten nicht auflaufen.

### Die vier Spalten, gemessen (10.08.2026)

Der Absatz oben beschrieb bis zum 10.08.2026 einen Ablauf, aber nicht die **Mechanik**. Sie ist in
[`messungen-schritt6.md`](messungen-schritt6.md) erhoben. Die vier Angaben sind **nicht** vier
Sichten auf dieselbe Beziehung, sondern zwei Beziehungen mal zwei Richtungen:

| Angabe | Steht auf | Bedeutet | Beziehung |
|---|---|---|---|
| `SourceMessageID` | dem **Kind** | „mein Elternteil ist …" | Aufteilung |
| `Source` | der **Wurzel** | „ich habe Kinder" | Aufteilung, Gegenrichtung |
| `TargetMessageID` | dem **Merge-Eingang** | „ich bin zusammengeführt worden nach …" | Zusammenführung |
| `Target` | dem **Merge-Ergebnis** | „ich bin aus einer Zusammenführung entstanden" | Zusammenführung, Gegenrichtung |

- **Die beiden ID-Spalten schließen einander je Zeile aus** (M28‑1). Keine Zeile trägt
  `SourceMessageID` **und** `TargetMessageID` — null Fälle über beide Bezugsfenster. Wer nur eine der
  beiden liest, verliert die Hälfte der Fälle.

  > ⚠️ **Geschärft am 10.08.2026.** Hier stand: *„Die beiden ID-Spalten sind vollständig disjunkt
  > (M25‑2). Kein Merge-Ergebnis trägt einen `SourceMessageID`, keine Split-Wurzel einen
  > `TargetMessageID` — in 4.528 geprüften Paaren nicht ein einziges Mal."* Die zweite Hälfte trägt
  > (`Wurzel + Eingang` = 0). **Die erste nicht:** M28‑1c findet über Fenster B **33 `NEXANS`-Zeilen**,
  > die zugleich Merge-Ergebnis (`Target = 1`) **und** Split-Kind (`SourceMessageID` belegt) sind.
  > M25‑2 hatte für seine Stichprobe recht — die sechs Merge-Ergebnisse in Fenster A —, und die
  > Verallgemeinerung auf den Bestand war ein Schluss zu weit. **Disjunkt sind die beiden
  > ID-Spalten, nicht die beiden Beziehungen:** Eine Zeile kann sehr wohl in der einen Kette Kind und
  > in der anderen Ergebnis sein.
  >
  > > **Belegvermerk** *(nachgetragen am 10.08.2026 nach Regel L10)*.
  > > *Gemessen (M25‑2):* Die **sechs** Merge-Ergebnisse in Fenster A tragen keinen
  > > `SourceMessageID`. `n = 6`.
  > > *Behauptet war:* **Kein** Merge-Ergebnis trägt einen `SourceMessageID` — eine Aussage über den
  > > Bestand.
  > > **Die Lücke:** sechs Zeilen eines Tagesfensters gegen 3,34 Millionen. Hier war `n` tatsächlich
  > > zu klein, und der Vermerk hätte es gezeigt: Die zweite Zeile hätte „im Bestand" gesagt, wo die
  > > erste „in sechs Zeilen" sagt. **Damit stehen die beiden Fehlerarten dieses Projekts
  > > nebeneinander:** Hier war der Umfang zu klein, bei „wartet vor"
  > > ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1) war er vollständig und der Schluss
  > > trotzdem zu weit. `n` und Vermerk sichern verschiedene Dinge.
- **Eine Zeile kann zwei Rollen tragen, aber nie drei oder vier** (M28‑1c). Über Fenster B sind es
  **514 von 214.330 Zeilen = 0,240 %**; je Mandant reicht die Spanne von 0 % bis **1,278 %**
  (`IBISGUS`). Selten, aber nicht null — die Rolle einer Nachricht ist deshalb eine **Menge** und
  kein einzelner Wert. Gemessene Kombinationen: Wurzel + Kind (456), Kind + Ergebnis (33),
  Ergebnis + Wurzel (25); Wurzel + Eingang und Kind + Eingang kommen **nicht** vor.
- **Die Flags decken sich exakt mit der tatsächlichen Verkettung** (E4): `Source = 1` genau dann,
  wenn mindestens ein Kind existiert (479/479, und 0 von 5.770 mit `Source = 0`); `Target = 1` genau
  dann, wenn eine `MERGED`-Zeile auf die Nachricht zeigt (6/6, 0 von 6.243). Ob eine Nachricht
  überhaupt eine Kette hat, steht damit **auf der Zeile selbst** — ohne Abfrage.
- **Der Status sagt über die Stellung in der Kette nichts Verlässliches.** Meist trägt die Wurzel
  `SPLITTED` und das Kind `FINISHED`, aber bei `IBIS`, `IBISGUS` und `ZAST` trägt die Wurzel
  `FINISHED` — bei Mandanten also, die über den ganzen Bestand **keine einzige**
  Zwischenschritt-Zeile haben (M24‑3). Verlässlich ist `Source`, nicht `MessageStatus`.
- **`MERGED` ist der Eingang, nicht das Ergebnis.** Das Ergebnis trägt `Target = 1` und meist
  `EERP_RECEIVED`; der Zusammenführungsgrad liegt bei **23 : 1** (M25‑1).
- **Breite und Tiefe:** bis **3.048 Kinder** an einer Wurzel im Bezugsmonat (87 % haben genau eines),
  und mindestens **vier Ebenen** — das Ergebnis einer Zusammenführung kann selbst wieder aufgeteilt
  werden (M24‑2, E2). Die Tiefenbegrenzung oben ist richtig; eine **Breitenbegrenzung** fehlte und
  ist ebenso nötig.

  > **Präzisiert am 10.08.2026 (M30‑2, M30‑3).** Über den **gesamten** Bestand statt über Fenster B
  > gemessen: bis **3.350** Kinder an einer Wurzel (452.822 Wurzeln, davon 1,07 % mit mehr als 50)
  > und bis **897** Eingänge an einem Merge-Ergebnis (31.185 Ergebnisse, davon **11,38 %** mit mehr
  > als 50). **Die Breitengrenze greift beim Merge zehnmal häufiger als beim Split** — der Grund
  > steht in M25‑1: Ein Merge sammelt im Normalfall Dutzende (Grad 23 : 1), ein Split hat im
  > Normalfall genau ein Kind.
  >
  > Und aus „mindestens vier Ebenen" ist **genau vier** geworden: Ketten mit fünf Gliedern gibt es
  > über alle 214.330 Startzeilen aus Fenster B **nicht** (M30‑3). Damit ist zugleich **kein
  > Zyklus** erreichbar — ein Kreis lieferte auf jeder Stufe Treffer —, und über den gesamten
  > Bestand gibt es **null Selbstverweise** (`SourceMessageID = MessageID` oder
  > `TargetMessageID = MessageID`, n = 3,34 Mio.). Der Zyklusschutz in
  > [`verkettung.md`](verkettung.md) §4 bleibt trotzdem: Die Kette entsteht durch Datenbank-Events,
  > die uns nicht gehören (§6).
- **Die Kette überschreitet die Mandantengrenze nicht** (M27): In allen 9.101 Zeilen mit
  Prozesswechsel führen Quell- und Zielprozess zum selben Mandanten. Der Filter aus Regel M5 bleibt
  trotzdem gesetzt — er ist die Zusicherung, nicht die Beobachtung.

---

## 8. Größenordnung

Mengengerüst — **Zeilenzahlen gezählt (Stand 12.08.2026), Bytegrößen erhoben am 27.07.2026**
(ersetzt die frühere Schätzung). Die Kennzeichnung gilt **jeder** Zeile und nicht nur den
geänderten:

| Tabelle | Zeilen | Herkunft der Zeilenzahl | Größe |
|---|---|---|---|
| `MessageProperty` | **75.571.462** | **gezählt** (M44, 12.08.2026) | 61,0 GB — davon **45,9 GB Index** |
| `MessageBAM` | **15.406.350** | **gezählt** (M33‑0, 11.08.2026) | 7,1 GB |
| `MessageAction` | **10.308.590** | **gezählt** (M14, 07.08.2026) | 3,0 GB |
| `Message` | 3.341.519 | **gezählt** (M0, 01.08.2026) | 2,9 GB |
| `Process` | **1.503** | **gezählt** (M10, 01.08.2026; bestätigt M44) | — |
| `Project` | 140 | **gezählt** (28.07.2026; bestätigt M44) | — |
| `User` | 36 | **gezählt** (M44, 12.08.2026) | — |

**Keine Zeile steht mehr auf einer Schätzung.** Die Bytegrößen stammen ohnehin aus belegten Seiten
und nicht aus einer Stichprobe; sie sind über fünf Messtage byteidentisch geblieben
([`messungen-schritt7.md`](messungen-schritt7.md) §0 und M44‑0).

| Kennzahl | Wert |
|---|---|
| Nachrichten pro Tag | **rund 7.300 im dichten Bestand** (nicht 5.000) |
| Aufbewahrung | **22 Monate** (ältester Datensatz 01.10.2024) |
| Prozesse | **1.503 (gezählt)** — Annahme A7 bestätigt |
| Eigenschaften je Nachricht | **22,62** über den Gesamtbestand, 22,57 bis 22,88 im dichten (M17, M44) |
| BAM-Einträge je Nachricht | **4,61** über den Gesamtbestand (M33‑0) |

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

> **Korrektur 12.08.2026 — die Herkunft steht jetzt an jeder Zeile.**
>
> **Drei Zahlen sind ersetzt.** `MessageBAM` **10.859.666 → 15.406.350** (gezählt, M33‑0, +41,9 %),
> `MessageProperty` **46.964.279 → 75.571.462** (gezählt, M44, +60,9 %) und `Process`
> **1.490 → 1.503** — die letzte ist seit dem 01.08.2026 gezählt und war hier nie nachgezogen worden.
> Alle drei waren `information_schema.TABLE_ROWS`; die Bytegrößen daneben sind unberührt und
> byteidentisch. Neu in der Tabelle ist `User` mit 36 (gezählt, M44).
>
> **Diese Datei lag diesmal vorne, und das gehört genauso festgehalten wie der umgekehrte Fall.**
> Sie unterschied bei `MessageProperty` und `MessageAction` bereits zwischen „(geschätzt)" und
> „(gezählt)" und führte `Project` mit den gezählten **140**, während
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 alles unter „Gemessenes Mengengerüst"
> zusammenfasste und 142 nannte. **Genau umgekehrt zum 07.08.2026**, wo diese Datei der korrigierten
> `annahmen-korrekturen.md` sechs Tage hinterherlief (Kasten darüber). Wer beide Kästen nebeneinander
> liest, sieht: Die Fehler laufen **nicht** immer in dieselbe Richtung, und keine der drei Dateien
> ist verlässlich die vordere.
>
> **Was die Kennzeichnung hier trotzdem nicht leistete:** Sie stand an **drei** von sechs Zeilen —
> `MessageProperty` „(geschätzt)", `MessageAction` und `Project` „(gezählt)". Die übrigen drei
> (`MessageBAM`, `Message`, `Process`) trugen **gar keine** Angabe und sahen dadurch aus wie die
> gezählten; zwei von ihnen waren Schätzungen, und beide falsch. **Eine Kennzeichnung an einem Teil
> der Zeilen ist schlechter als keine** — sie lässt den Rest bestätigt aussehen. Ab dieser Fassung
> trägt **jede** Zeile ihre Herkunft, auch die unveränderten.
>
> **Unberührt:** Regel L4 (sie wird durch die höhere Zeilenzahl eher strenger), die **82 Prozent**
> (sie rechnen mit Bytes), Regel L5 (durch M33 gemessen unterlegt) und die Leistungsregeln L1 bis
> L7. Einordnung in [`annahmen-korrekturen.md`](annahmen-korrekturen.md).

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
