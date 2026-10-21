# Overlord Monitoring — Projektbeschreibung

Stand: Version 1.0.0, 16.10.2026 · Diese Datei ist der verbindliche Kontext für alle Arbeiten am
Projekt. Bei Widersprüchen zwischen dieser Datei und einer Annahme im Code gilt diese Datei.

**Ab 1.0.0 beschreibt diese Datei den aktuellen Stand.** Eine Änderung ersetzt den Text; Grund und
Entscheidung stehen im Commit und in [`CHANGELOG.md`](../CHANGELOG.md) (Entscheidung 9 vom
16.10.2026, [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). Die Fassung bis
1.0.0 samt allen Korrekturkästen liegt unverändert in
[`archiv/PROJEKTBESCHREIBUNG_bis-1.0.0.md`](archiv/PROJEKTBESCHREIBUNG_bis-1.0.0.md); wo eine
Aussage von dort heute steht, zeigt [`archiv/konsolidierung-1.0.0.md`](archiv/konsolidierung-1.0.0.md).

**Herkunftsvermerk (Regel L10).** Jede Zahl trägt ihre Herkunft: *gemessen* oder *gezählt* (mit
M-Nummer), *Auskunft* (wer, wann) oder *ungedeckt*. Offene Punkte stehen mit Nummer im Register
[`offene-punkte.md`](offene-punkte.md).

> **Ausnahme vom Vorrang — Indizes.** Für die Indexlisten dieser Datei entscheidet die Datenbank,
> nicht der Text. Die Sollliste
> [`backend/src/test/resources/indizes-sollliste.txt`](../backend/src/test/resources/indizes-sollliste.txt)
> ist aus `information_schema.STATISTICS` erhoben (21.08.2026) und wird von **`IndexbestandDbIT`**
> in beide Richtungen bewacht: rot bei einem fehlenden und bei einem undokumentierten Index (E37,
> gebaut 21.08.2026). Weicht diese Datei von der Sollliste ab, gilt die Sollliste. Die Sollliste
> führt seit dem 16.10.2026 auch die Rollup-Tabellen aus `V9` bis `V12` (Punkt 252); auf `main` nimmt
> die Prüfung der Tabellenliste die Tabellen der Assistent-Linie aus, wie der Codegen
> ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12).

---

## 1. Zweck

Ein Monitoring-Werkzeug für die EDI-Integrationsplattform **Overlord**. Es liest aus der bestehenden
MariaDB (`GlassfishDB`) und stellt Mandanten den Zustand ihrer EDI-Übertragungen dar. Das
Altwerkzeug (ExtJS 4.1, Servlet-basiert) ist **nicht nachgebaut**; es dient als fachliche Referenz
dafür, welche Daten gebraucht werden. Die Bedienlogik ist neu gedacht.

### Leitsatz

Der typische Nutzer ist kein EDI-Spezialist. Er sucht einen Beleg und will wissen, wo dieser steht.
Jede Entscheidung fällt im Zweifel zugunsten dieser Frage — nicht zugunsten technischer
Vollständigkeit. Interne IDs, Statuscodes und Servicenamen sind Beiwerk, keine Hauptinformation.

---

## 2. Nutzer und Rollen

| Rolle | Wer | Sieht |
|---|---|---|
| `MANDANT` | Kundenmitarbeiter (extern) | ausschließlich Daten der ihm zugeordneten Mandanten |
| `ADMIN` | interne EDI-Betreuung | alle Mandanten, zusätzlich Benutzer- und Katalogpflege |

Primäre Zielgruppe ist `MANDANT`. Externe Nutzer heißen: Die Mandantentrennung ist eine
Sicherheitsanforderung, keine Komfortfunktion.

**Es ist immer genau ein Mandant aktiv, für jede Rolle.** `ADMIN` sieht alle Mandanten, aber
nacheinander. Die Rollen unterscheiden sich allein darin, welche Mandanten wählbar sind; es gibt
keinen Codepfad ohne Mandantenfilter, und der Isolationstest gilt für beide gleich. Der aktive
Mandant steht in der Session, nicht in der URL, und deshalb sichtbar in der Kopfzeile.

**Berechtigung ist eine Menge, keine Rolle:** Wer für einen Mandanten berechtigt ist, darf zu ihm
wechseln. Die Menge je Konto steht n:m in `app_user_mandant` und wird in der Benutzerverwaltung
gepflegt ([`benutzerverwaltung.md`](benutzerverwaltung.md)). `NEXANS`/`NXHBE` und `IBIS`/`IBISGUS`
sind je dasselbe Haus.

---

## 3. Quellsystem `GlassfishDB`

**Zugriff ausschließlich lesend.** Es wird niemals in dieses Schema geschrieben; kein
Datenbankbenutzer dieser Anwendung besitzt dort ein Schreibrecht.

### 3.0 Verbindung und Rechte

Server MariaDB 10.6.22; beide Schemata auf derselben Instanz, beide `utf8mb4` mit
`utf8mb4_general_ci`. Alle Schlüsselspalten (`MandantID`, `ProjectID`, `ProcessID`, `MessageID`)
sind `varchar(36)`.

| Benutzer | `GlassfishDB` | `overlord_monitor` |
|---|---|---|
| `monitor_read` | `SELECT` | `SELECT` |
| `monitor_write` | `SELECT` | `ALL PRIVILEGES` |

**`monitor_write` verwendet zusätzlich die EDI-Plattform**, um Aufträge aus `overlord_monitor.task`
abzuholen und zurückzumelden (E‑1004, [`tasks.md`](tasks.md)). **Folge:** Die Plattform hat damit
alle Rechte auf `overlord_monitor`, einschließlich `app_user` und der Sitzungen. Das ist bewusst so
entschieden — kein neuer Benutzer, kein `GRANT`; was die Plattform darf, begrenzt allein der
Vertrag [`task-schnittstelle.md`](task-schnittstelle.md).

Host, Benutzernamen und Passwörter stehen ausschließlich in Umgebungsvariablen
([`betrieb.md`](betrieb.md)). MariaDB 10.6 hat seinen Wartungszeitraum im Juli 2026 erreicht
(Annahme A10).

**Der Server bietet kein TLS an** (festgestellt 28.07.2026). Der JDBC-Treiber verbindet
unverschlüsselt; Kommandozeilen-Clients brauchen `--skip-ssl`. Der Verkehr zwischen Anwendung und
Datenbank läuft im Klartext — die Passwörter nicht (Frage-Antwort-Verfahren), jede Nachricht und
jeder BAM-Wert schon. Das ist bewusst akzeptiert: Anwendung und Datenbank stehen in der Produktion
im selben Netz (Entscheidung 5, Auskunft Lukas vom 16.10.2026, nicht gemessen; Annahme A11).

### 3.1 Hierarchie

```
Mandant ──< ProjectMandant >── Project ──< Process ──< SOS ──< SOSAction
                                            │           │
                                            └──< Message ──< MessageAction ──< MessageProperty
                                                    │
                                                    └──< MessageBAM
```

`ProjectMandant` ist n:m; ein Projekt kann mehreren Mandanten gehören. Das Quellsystem kapselt die
Zuordnung in der View `MessageMandantID`. **Diese Anwendung benutzt die View nicht:** Jedes
Statement löst die Kette selbst auf, als `EXISTS` über `Process → ProjectMandant`, und zwar im
Statement statt nachgelagert (Regel M3; E42, M4 in [`messungen-schritt4.md`](messungen-schritt4.md),
M47). Der Zugriffspfad der View ist mit den Rechten dieser Anwendung nicht einsehbar (Fehler 1142
und 1345) und kann Regel L7 nicht erfüllen ([`datenmodell.md`](datenmodell.md) §2). `Project`
liegt nicht auf der Kette.

### 3.2 Kerntabellen

Zeilenzahlen und Größen stehen im Mengengerüst in §8. Die vollständigen Spalten, Fallstricke und
Joins stehen in [`datenmodell.md`](datenmodell.md).

**`Message`** — eine Nachricht/Übertragung.
`MessageID` varchar(36) PK · `ProcessID` · `SOSID` · `SOSActionID` · `MessageStatus` varchar(30) ·
`MessageLastUpdate` timestamp · `MessageTimeout` smallint · `SourceMessageID` · `TargetMessageID` ·
`Source` bit · `Target` bit

- **Es gibt kein Anlagedatum.** `MessageLastUpdate` ist der Zeitpunkt der letzten Änderung; der
  fachliche Start ist `MIN(MessageAction.MessageActionStart)`.
- **`MessageTimeout` ist eine Dauer in Sekunden** (M8: `SOSActionTimeout = 1800` steht 37.120-mal
  neben `WAIT|30M`). Sie ist der Parameter eines Wächters des Altsystems: Eine Nachricht, die länger
  in `RUNNING` steht, wird automatisch `ERROR_TIMEOUT`; auf `SUSPENDED` wirkt er nicht (Auskunft
  des Auftraggebers vom 03.09.2026, nicht gemessen). Das Werkzeug zeigt keine Frist an (E‑76).
- **Indizes** — `Message` trägt acht (M1, erhoben in [`messungen-schritt9.md`](messungen-schritt9.md)
  V2; maßgeblich ist die Sollliste, siehe oben):

| Index | Spalten | eindeutig |
|---|---|---|
| `PRIMARY` | `MessageID` | ja |
| `MessageLastUpdateIDX` | `MessageLastUpdate` | nein |
| `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdate, ProcessID, MessageID` | nein |
| `MessageStatusIDX` | `MessageStatus` | nein |
| `Message_ProcessFK` | `ProcessID` | nein |
| `ProejctIDIDX` *(sic, auf `ProcessID`)* | `ProcessID` | nein |
| `SourceMessageIDIDX` | `SourceMessageID` | nein |
| `TargetMessageIDIDX` | `TargetMessageID` | nein |

  Die generierte `Indexes.java` ersetzt die Sollliste nicht: Sie zieht bei jedem Codegen still nach
  und kann nie rot werden (E40).

**`MessageAction`** — die ausgeführten Schritte einer Nachricht.
PK `(MessageID, MessageActionID)` · `SOSID` · `SOSActionID` smallint · `MessageActionStart` /
`MessageActionEnd` · `ServiceID` · `SOSActionServiceProperties` mediumtext · `SOSActionTimeout`
(neun Spalten, M14). Der Schrittname kommt über `SOSAction ON (MessageAction.SOSID,
MessageAction.SOSActionID)`, **niemals** über `Message.SOSID` (M15: 100 % gegen 96,17 %).
`SOSActionServiceProperties` trägt die Bausteine als pipe-getrennte Liste
(`NXS_FILE_CONVERT|E2A|UNWRAP`); angezeigt wird der Klartext `SOSAction.SOSActionName`.

**`MessageProperty`** — Schlüssel/Wert-Paare je Nachricht (EAV).
PK `(MessageID, MessagePropertyName, MessageActionID)` · `MessagePropertyValue` mediumtext.
Rund 23 Zeilen je Nachricht, 808 Byte je Zeile (aus M44 gerechnet). **Diese Tabelle ist 82 % der
Datenbank** (Bytes). **Zugriff ausschließlich über `MessageID`** (Regel 4 in §8, mit ihrer einen
benannten Ausnahme). Die Indizes auf dem Wert sind Präfixindizes über 50 Zeichen.

Bekannte Namen: `Message.GUID`, `Message.SendingPartner`, `Message.SNDPRN`, `Message.VFN`,
`Message.SOS`, `Message.SplitCount`, `Message.Payload.GUID`, `Message.InterchangeNumber`,
`Message.CommitInterchangeNumber`, `Message.SourceMessageID`, `Message.DestinationFilename`.

- `Message.DestinationFilename` ist für `NEXANS` als Suchfeld konfiguriert (M161) und angeboten,
  weil er konfiguriert ist; was er fachlich bedeutet, steht in keiner Projektdatei (E‑105).
- **`Message.Payload.GUID` ist kein eigenes Artefakt:** Er trägt in 6.249 von 6.249 (Fenster A) und
  214.330 von 214.330 (Fenster B) Nachrichten denselben Verweis wie die Nutzdatenzeile mit dem
  höchsten `MessageActionID` (M73). Der Rohdatenzugriff führt nicht über diesen Namen (§7).
  Verweise haben das Format `<Ablagenkennung>|<UUID>`.

**`MessageBAM`** — fachliche Suchschlüssel (Business Activity Monitoring).
PK `(MessageID, MessageBAMType, MessageBAMValue)` · `MessageBAMValue` varchar(70), eigener Index.
Die zentrale Suchdimension für Fachanwender (Lieferschein, Bestellung, Charge …); `MessageBAMType`
verweist auf `MessageBAMType.MessageBAMTypeDescription`. Rund 4,61 Einträge je Nachricht (aus M33‑0
gerechnet). **`MessageBAM` hat keinen Zeitstempel:** Das Zeitfenster greift erst nach dem Join auf
`Message`; häufige Werte liefern bis zu 234.159 Treffer auf einen Wert (M33).

**`MessageBAMMandant`** — je Mandant die sichtbaren BAM-Typen und ihre Reihenfolge
(`MessageBAMTypeSortIndex`); übernommen, nicht neu erfunden.

**`Process`** — PK `ProcessID` · `ProcessName` varchar(255) · `ProcessDescription` text ·
`ProjectID` varchar(36), `NULL`-fähig. **`Project`** — PK `ProjectID` · `ProjectName` varchar(255) ·
`ProjectDescription` text. Beide gegen `information_schema.COLUMNS` erhoben am 15.09.2026; alle
Textspalten `utf8mb4_general_ci`, alle außer dem PK `NULL`-fähig. `Mandant` und `ProjectMandant`
sind nicht nacherhoben.

**Die zehn Mandanten** (erhoben 28.07.2026; die `MandantID` ist ein lesbarer Code):

| ID | Name | |
|---|---|---|
| `EDITIONLINGERI` | Edition Lingerie GmbH | |
| `IBIS` | IBIS GmbH | gehört mit `IBISGUS` zusammen |
| `IBISGUS` | IBIS GmbH mit GUS WW | |
| `NEXANS` | Nexans autoelectric GmbH | trägt praktisch das gesamte Aufkommen |
| `NXHBE` | Nexans Belgien | gehört mit `NEXANS` zusammen |
| `SUTTONS` | Suttons Group | |
| `SYSTEM` | Systemmandant | technisch, kein Kunde |
| `VOTG` | VOTG Tanktainer GmbH | |
| `WOC` | Without Contract | technisch, kein Kunde — Auffangbecken für Verkehr ohne Vertrag |
| `ZAST` | Zast GmbH | |

**`SYSTEM` und `WOC` werden behandelt wie jeder andere Mandant** (E‑f, 24.08.2026): keine
Aussiebung, kein Sonderpfad. Ihnen wird nie ein externes Konto zugeordnet; sie sieht nur `ADMIN`.
Auf der Testkopie trägt `WOC` 2.529 Nachrichten, `SYSTEM` 151 (M91).

**`SOS` / `SOSAction`** — „Sequence of Services“, der aus Bausteinen gebaute Ablauf. `SOSName` ist in
Klartext gepflegt und dient als Anzeigename. Process zu SOS ist meist 1:1, gelegentlich 1:n.

**`Service`** — Dienste und Ablagen der Plattform, 20 Zeilen (M14, M52).
`ServiceID` varchar(36) PK · `ServiceTypeID` · `ServiceConnectString` · `ServiceName` ·
`ServiceDescription` · `ServiceStatus` varchar(30) · `ServiceLastUpdate` · `ServiceLastStatusMessage` ·
`ServiceTimeout` smallint · `ServiceDefaultFileStore` varchar(36)

- **Kennt keinen Mandanten** — Grundlage der dritten benannten Ausnahme von Regel M2 (§7).
- `ServiceID` ist ein lesbarer Code (20 von 20, M52); elf Zeilen sind Filestores
  (`ServiceTypeID = 1`). Die Auflösung einer Ablagenkennung ist ein Primärschlüsselzugriff.
- `ServiceStatus` kennt `HEARTBEAT`, `ERROR_TIMEOUT`, `SHUTDOWN`; `DienstkatalogDbIT` bewacht die
  Menge. `ServiceTimeout` ist bei den Ablagen `0`, sonst `600`; Einheit und Takt sind unbekannt und
  werden nicht umgerechnet (Punkt 164).
- `ServiceLastStatusMessage` trägt den letzten gemeldeten Text, nicht den Grund des Zustands; sie
  steht in keiner Antwort. `ServiceConnectString` enthält einen Hostnamen (Regel G1).
- `ServiceDefaultFileStore` nennt die Ablage der Erreichbarkeitsprüfung; auf der Testkopie ist eine
  Kennung eingetragen, beschrieben werden je Fenster zwei (M53; Punkt 166).

**`User`** — Alt-Benutzertabelle, `UserPassword` im Klartext. Wird nicht verwendet (§7).

**`MessageStatisticHistory`** und View `MessageStatistic` — Aggregation des Altsystems, Schlüssel
`<MandantID>&&<YYYYMMDDHH>&&HOUR|DAY|MONTH`, nur Anzahlen. 42 Zeilen (27.07.2026); als Quelle
unbrauchbar. Die Kennzahlen kommen aus dem eigenen Rollup (§5).

### 3.3 Datenbank-Events (laufen weiter, gehören uns nicht)

| Event | Takt | Wirkung |
|---|---|---|
| `CreateMessageStatisticHistory` | täglich | füllt `MessageStatisticHistory` |
| `MatchInterchange` | ⚠️ **„stündlich“ — ungedeckt** | ordnet COMMITs über Interchange-Nummern zu, setzt `COMMIT_RECEIVED` und `SourceMessageID` |
| `SetTargetFlag` | — | setzt `Target` |
| `MoveDTNA997` | stündlich | verschiebt Nachrichten zwischen zwei Prozessen (kundenspezifisch) |

> **Der Takt von `MatchInterchange` ist ungedeckt** (M31‑3, 11.08.2026). Eine Quelle ist nirgends
> verzeichnet; von der Testkopie aus ist er nicht sichtbar: `information_schema.EVENTS` ist leer
> (Rechteartefakt), `SHOW EVENTS` verweigert, und `MessageLastUpdate` der `COMMIT_RECEIVED`-Zeilen
> verteilt sich über alle 60 Minuten der Stunde wie ein Status, den kein Event schreibt (0,597 gegen
> 0,601). Die Messung kann den Takt nicht ausschließen, nur nicht zeigen. **Dass die
> COMMIT-Zuordnung verzögert ist, ist unstrittig; wie lange, nicht.** Ein Satz wie „kann bis zu
> eine Stunde dauern“ wäre geraten und ist nicht gebaut ([`verkettung.md`](verkettung.md) §11).
> Die Quelle ist beim Altsystem zu holen (§11).

---

## 4. Fachliche Definitionen

### 4.1 Nachrichtenstatus

**`MessageStatus` ist freier Text, kein Aufzählungstyp.** Unbekannte Werte werden als Rohwert
durchgereicht und fallen nie still in einen bekannten Eimer. Die Einordnung entsteht an **genau
einer Stelle** im Code, `common/MessageStatusClassifier` (Enum `MessageStatusKind`), und wird von
Liste, Detail, Rollup, Dashboard und Suche benutzt. Vollständig in
[`message-status.md`](message-status.md).

Bekannt sind fünfzehn Werte. Anzahl: ganzer Bestand der Testkopie, gezählt am 13.10.2026 (M191,
Datenstand 08.07.2026, Summe 3.341.519).

| Status | Anzahl | Einordnung | Anzeige |
|---|---:|---|---|
| `FINISHED` | 2.030.985 | `ABGESCHLOSSEN` | grün |
| `MERGED` | 747.885 | `ZUSAMMENGEFUEHRT` — Eingang einer Zusammenführung | neutral |
| `SPLITTED` | 400.845 | `AUFGETEILT` — Wurzel einer Aufteilung | neutral |
| `EERP_RECEIVED` | 143.871 | `QUITTIERT` | grün |
| `COMMIT_RECEIVED` | 12.654 | `QUITTIERT` | grün |
| `ERROR_DUPLICATE` | 3.248 | `FEHLER` | rot |
| `COMMIT_SENT` | 1.051 | `UNGEKLAERT` | neutral |
| `SUSPENDED` | 538 | `WARTEND` — wartet absichtlich, kein Fehler | neutral |
| `CHECKED` | 276 | `UNGEKLAERT` | neutral |
| `COMMIT_REJECTED` | 111 | `FEHLER` — vom Partner abgelehnt | rot |
| `ERROR_TIMEOUT` | 53 | `FEHLER` | rot |
| `CKECKED` | 2 | `UNGEKLAERT` (Tippfehler im Bestand) | neutral |
| `RUNNING` | 0 | `LAEUFT` | neutral |
| `EERP_FAILED` | 0 | `FEHLER` — Empfangsbestätigung fehlgeschlagen (E‑233) | rot |
| `EERP_PENDING` | 0 | `QUITTUNG_AUSSTEHEND` — übertragen, die Bestätigung des Partners steht aus; kein Endstatus (E‑234, E‑235) | neutral |

- **`RUNNING`, `EERP_FAILED` und `EERP_PENDING` kommen in der Testkopie nicht vor**, in der
  Produktion aber sehr wohl. `RUNNING` und `EERP_PENDING` sind flüchtig; „läuft noch“ darf nie als
  `MessageStatus = 'RUNNING'` allein definiert werden. Dass `EERP_FAILED` existiert und ein Fehler
  ist, ruht auf der Beobachtung in der Produktion am 09.10.2026 und E‑233; `EERP_PENDING` auf der
  Auskunft des Auftraggebers vom 13.10.2026. Beides ist nicht gemessen.
- **`QUITTUNG_AUSSTEHEND` ist ausdrücklich nicht `WARTEND`:** „wartet bei uns“ und „wartet auf den
  Partner“ sind verschiedene Antworten.
- `CHECKED`, `CKECKED` und `COMMIT_SENT` sind bekannt, aber fachlich ungeklärt: neutral, mit Rohwert
  und „Bedeutung nicht verifiziert“. Ein unbekannter Wert wird `UNGEKLAERT`, ein unbekannter mit
  Präfix `ERROR_` `FEHLER`.
- Die Anzeige „neutral“ sind im Frontend zwei Rollen: `offen` („Ohne Ergebnis“) und `ungeklaert`.

**Die Fehlerbedingung** — `MessageStatusClassifier.fehlerBedingung(...)`, nirgends nachgebaut:

```sql
MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus IN ('COMMIT_REJECTED', 'EERP_FAILED')
```

`_` ist in `LIKE` ein Platzhalter, deshalb das Escape. **Keine** `LEFT()`-Form (sie kann
`MessageStatusIDX` nicht nutzen) und kein Muster wie `'%_FAILED'`; jeder Fehler ohne Präfix steht
einzeln da. Der Zugriff ist ein `range` über `MessageStatusIDX` mit drei Intervallen, kein
`index_merge` (M188, M191).

**Sicherung gegen neue Statuswerte:** `DatenzugriffDbIT` vergleicht `SELECT DISTINCT MessageStatus`
mit der bekannten Menge und wird bei einem unbekannten Wert rot. Nur mit diesem Wächter ist es
vertretbar, unbekannte Werte neutral zu behandeln. **Er liest die Testkopie, nicht die Produktion**
(Punkt 218), und er ist ein Schnappschuss, der flüchtige Status grundsätzlich nicht
sieht; ein Laufzeitsignal dafür ist entschieden nicht gebaut (Punkt 219, E‑236). `EERP_FAILED` und
`EERP_PENDING` sind in der Produktion über die Fragezeichen-Plakette der Liste gefunden worden.

### 4.2 Die drei Problemkategorien

Fachlich sind es drei, und sie dürfen nie zu „Fehler“ zusammengefasst werden (Regel Q3). **Gebaut
ist eine.**

1. **Fehler** — gebaut. `MessageStatus` beginnt mit `ERROR_` **oder** ist `COMMIT_REJECTED` **oder**
   `EERP_FAILED`. Die Fehlerart ist bei `ERROR_*` der Namensteil dahinter, bei `COMMIT_REJECTED`
   „Vom Partner abgelehnt“, bei `EERP_FAILED` „Empfangsbestätigung fehlgeschlagen“
   (`MessageStatusClassifier.fehlerart`).
2. **Überfällig** — eine Nachricht nicht in einem Endstatus, deren `MessageLastUpdate +
   MessageTimeout` in der Vergangenheit liegt. **Widerlegt und nicht gebaut** (E‑71, 03.09.2026):
   Das Altsystem setzt `RUNNING` über der Frist selbst auf `ERROR_TIMEOUT`, und `SUSPENDED` wartet
   absichtlich (Auskunft des Auftraggebers, nicht gemessen; auf der Testkopie waren alle 538 Treffer
   `SUSPENDED`, keiner `RUNNING`, M90/M144). An ihrer Stelle zählen die Kacheln *Läuft* und *Wartend*
   einen Zustand ohne Schwelle (E‑72, E‑73); ob etwas zu lange hängt, beantworten sie nicht
   (Punkt 130). Vollständig in [`message-status.md`](message-status.md), Abschnitt Überfälligkeit.
3. **Unquittiert** — ausgehende Nachricht ohne zugeordnete Empfangsbestätigung; `COMMIT_REJECTED`
   und `EERP_FAILED` gehören ausdrücklich nicht dazu (sie sind Fehler). **Nicht gebaut** (E‑d,
   24.08.2026): Die Kategorie hat keine operative Definition, und sie hängt am ungedeckten Takt von
   `MatchInterchange` (§3.3). `EERP_PENDING` ist eine Teildefinition; wie er sich zur Kategorie
   verhält, ist Punkt 220. Bis dahin zählt eine hängende `EERP_PENDING`-Nachricht in keiner
   Kategorie.

### 4.3 Verkettung (Lineage)

Über `SourceMessageID`, `TargetMessageID` und die Flags `Source` und `Target` entsteht eine Kette:
Eine Sammelnachricht wird aufgeteilt, Teile werden zusammengeführt, am Ende kommt eine Quittung.
Für den Nutzer ist das die Antwort auf „wo ist mein Lieferschein“; die Verkettung ist deshalb
gebaut ([`verkettung.md`](verkettung.md)). `MERGED` ist der Eingang einer Zusammenführung,
`SPLITTED` die Wurzel einer Aufteilung.

### 4.4 Partner, Richtung, Belegart

**Partner und Richtung stehen in `GlassfishDB` nicht als Daten.** Sie stecken in
Namenskonventionen, die sich je Mandant unterscheiden (bei `NEXANS` trägt das Projekt die Richtung,
der Prozess Partner und Belegart; bei Suttons das Projekt den Partner, die Richtung nur der
`SOSName`), und Partnernamen sind nicht token-sauber.

**Es wird nicht geparst, es wird kuratiert.** Partner und Richtung sind Felder im Prozess-Katalog
(§5); eine Heuristik schlägt vor, die Wahrheit steht im Katalog. Nicht zugeordnete Prozesse
erscheinen sichtbar als „nicht zugeordnet“, nie als geratener Wert
([`prozess-katalog.md`](prozess-katalog.md)). Standort und Belegart werden nicht geführt; nichts
liest sie. `00001_Undefined` ist ein Auffangprozess.

---

## 5. Eigenes Schema `overlord_monitor`

Auf derselben Instanz, verwaltet von Flyway (`backend/src/main/resources/db/migration`, `V1` bis
`V13` auf `main`, ab `V100` auf der Linie `assistent`). Schreibrechte hat ausschließlich
`monitor_write`, und nur hier. Die Tabellen, erhoben aus den Migrationen:

| Tabelle | Migration | Schlüssel | Zweck |
|---|---|---|---|
| `audit_log` | V1 | `id` | Anmeldungen, Fehlversuche, Passwort-, Konto- und Katalogänderungen, Rohdatenabrufe, Anlegen und Löschen von Aufträgen |
| `app_user` | V2, V7, V13 | `id`, eindeutig `username` | Konto, Passwort-Hash, Rolle, Sperrzustand, Änderungszwang, Baumgliederung (`tree_layout`) |
| `app_user_mandant` | V2 | `(user_id, mandant_id)` | Mandantenmenge je Konto (n:m) |
| `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` | V3 | `PRIMARY_ID` bzw. `(SESSION_PRIMARY_ID, ATTRIBUTE_NAME)` | serverseitige Sitzung (Spring Session JDBC) |
| `bam_spalte` | V4 | `(mandant_id, position)` | kuratierte BAM-Spalten der Nachrichtenliste je Mandant |
| `bam_sollaenge` | V5 | `(mandant_id, message_bam_type)` | kuratierte Sollängen der Belegsuche ([`bam-sollaengen.md`](bam-sollaengen.md)) |
| `process_catalog` | V6, V8 | `process_id` | Partner, Richtung, Pflegestatus, Herkunft des Vorschlags, Bestandskennzeichen |
| `message_rollup` | V9, V11 | `(stunde, process_id, message_status)` | Anzahl je Stunde, Prozess und Rohstatus |
| `rollup_lauf` | V9 | `id` | Läufe des Rollup-Jobs, Wasserstand |
| `message_rollup_tag` | V10 | `(tag, process_id, message_status)` | dasselbe je Tag |
| `message_rollup_monat` | V12 | `(monat, process_id, message_status)` | dasselbe je Kalendermonat |
| `task_type` | V100 | `task_typ` | Auftragstypen als Daten: Name, Beschreibung für den Assistenten, Struktur (JSON), aktiv ([`tasks.md`](tasks.md) §3) |
| `task_type_mandant` | V100 | `(task_typ, mandant_id)` | Freischaltung eines Auftragstyps je Mandant, Fremdschlüssel auf `task_type` mit `ON DELETE CASCADE` |
| `task` | V100 | `task_id`, eindeutig `(mandant_id, anfrage_id)` | Aufträge der Kunden mit Kopie der Definition, Einträgen und Parametertext; die EDI-Plattform holt sie ab und meldet zurück, einen offenen kann der Kunde löschen ([`tasks.md`](tasks.md) §5, §6.6, [`task-schnittstelle.md`](task-schnittstelle.md)) |

**Der Rollup-Schlüssel trägt weder Mandant noch Partner noch Richtung** (E‑a, 24.08.2026). Sie kommen
zur Lesezeit dazu — der Mandant über `Process → ProjectMandant`, Partner und Richtung über
`process_catalog`. So wirkt eine Katalogkorrektur rückwirkend über den ganzen Bestand. Gemessen: Die
Leseabfrage des Dashboards kostet mit Katalog-Join im Standardfenster 11,299 ms (M89)
([`rollup.md`](rollup.md)).

**Keine Fremdschlüssel über Schemagrenzen.** `process_catalog` verweist auf die `ProcessID` als
Zeichenkette; eine Katalogzeile überlebt das Löschen des Prozesses im Altsystem.

### Zeichensatz und Sortierung — verbindlich

Das Schema und **jede Tabelle** bekommen `utf8mb4` und `utf8mb4_general_ci` **explizit** in der
Migration, nie geerbt — auch jede neue Spalte. Sonst bricht ein schemaübergreifender Join, sobald
sich der Server-Default ändert oder die Instanz auf MariaDB 11 (`uca1400`) gehoben wird.
`utf8mb4_general_ci` vergleicht ohne Rücksicht auf Groß- und Kleinschreibung — für `username`
erwünscht; **Session-IDs, Rücksetz-Token und vergleichbare Spalten bekommen `utf8mb4_bin`**.

---

## 6. Technische Architektur

### Backend

Spring Boot 4.1 (Spring Framework 7, Spring Security 7, Jackson 3), Java 21, jOOQ, Flyway (nur für
`overlord_monitor`). Versionsstand in [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §9.

**Zwei DataSources, eine Rechteregel.** Beide Benutzer lesen beide Schemata, damit
schemaübergreifende Joins in einem Statement laufen; nur `monitor_write` schreibt, und nur in
`overlord_monitor`. Der Schreibschutz auf `GlassfishDB` hat drei Schichten, die nicht gleichwertig
sind:

1. **Die DB-Rechte sind die Wahrheit.** Nur sie tragen die Zusage.
2. Der `readOnly`-Pool ist Zusatz, kein Nachweis.
3. Ein jOOQ-`ExecuteListener` auf dem Lese-Kontext weist alles ab, was nicht `ExecuteType.READ` ist.

**Keine DataSource ist `@Primary`** — ein vergessener Qualifier soll beim Start scheitern. Flyway
und die Session binden über `@FlywayDataSource` und `@SpringSessionDataSource` an den Schreib-Pool.
**Genau ein Transaktionsmanager**, an `overlord_monitor` gebunden und `@Primary`: `@Transactional`
heißt im ganzen Projekt „schreibt ins eigene Schema“; ein Lesezugriff darin läuft auf einer anderen
Verbindung.

**Die beiden DSLContexts:** Der Lese-Kontext darf beide Schemata lesen; der Schreib-Kontext darf
ausschließlich `overlord_monitor` und nur dort schreiben. Kein `defaultSchema`, Schemanamen werden
immer voll qualifiziert.

**Der Lese-Pool hat eine Laufzeitgrenze** (`max_statement_time = 10`). Zur Laufzeit wird auf der
Produktion gelesen; ein Statement, das nach zehn Sekunden stirbt, ist ein Fehler in der Oberfläche,
eines, das nicht stirbt, ein Vorfall in der EDI-Plattform.

**Zeit.** Zeitstempel aus `GlassfishDB` sind Wanduhrzeit des Servers und werden nirgends
konvertiert; die JDBC-URL trägt keine Zeitzonenparameter; im eigenen Schema steht UTC.
`LocalDateTime.now()` wird nirgends direkt aufgerufen, sondern die `Clock`-Beans aus
`common/ZeitConfig` (§8, Umgebungen); sicherheitsrelevante Zeit nutzt immer die Systemuhr.

**Spring Boot statt Quarkus**, weil das Projekt mit Claude Code umgesetzt wird und für Spring Boot
deutlich mehr Trainingsmaterial existiert. **Kein JPA.** Das Quellschema gehört uns nicht, ist
lesegetrieben und teilweise EAV. jOOQ generiert
typsichere Zugriffe in zwei getrennte Zielpakete — `…jooq.glassfish` (nur lesend) und
`…jooq.monitor` (schreibend) —, damit am Import erkennbar ist, welches Schema angefasst wird. Der
Codegen läuft lokal gegen die Testkopie; die erzeugten Quellen sind eingecheckt
(`src/main/generated-java`), die CI überspringt ihn (`-Djooq.codegen.skip=true`) und schließt Tests
mit `@Tag("db")` aus. Auf `main` schließt der Codegen die Tabellen der Assistent-Linie aus
([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12).

**Basispaket `de.kraftwerkone.overlord.monitor`** (Maven `de.kraftwerkone:overlord-monitor`),
Hauptklasse `OverlordMonitorApplication` im Basispaket. Fachlich geschnitten, nicht nach Schichten:

```
de.kraftwerkone.overlord.monitor
├─ config/      DataSources, jOOQ, Security, Flyway, Konfigurationseigenschaften
├─ common/      Fehlerformat, Cursor, Zeitfenster, Uhren, MandantContext, Statuseinordnung,
│               Pflegestatus, Transport zur Ablage, Live-Rest, Fehler live;
│               common/auftrag: Typdefinition, Typkatalog und Prüfung der Aufträge
├─ security/    Anmeldung, Sitzung, Anmeldesperre, Herstellung des MandantContext
├─ audit/       audit_log
├─ message/     Liste, Detail, Verkettung, Fensterverengung
├─ bam/         Belegdaten, Belegsuche, Property-Suche
├─ payload/     Rohdaten und Protokolle: Artefaktliste, Anzeige, Download
├─ catalog/     Prozess-Katalog, Prozessbaum
├─ rollup/      Rollup-Job (per Profil separat startbar)
├─ dashboard/   Übersicht, Dienste und Ablagenprüfung
├─ admin/       Benutzerverwaltung, Bootstrap
├─ task/        Aufträge: Anlegen, Prüfung ohne Anlegen, Parametertext, Einordnung (Linie assistent)
├─ assistent/   Assistent: Gesprächsführung, Werkzeuge, Endpunkte; assistent/modell die
│               anbieterneutrale Schnittstelle, assistent/anthropic der Adapter (Linie assistent)
└─ jooq/        generiert, nicht handgepflegt
```

**Fachpakete kennen einander nicht; Gemeinsames liegt in `common`.** Etwas wandert dorthin, wenn es
einen zweiten Verbraucher bekommt — so Typdefinition, Typkatalog und Prüfung der Aufträge mit dem
Assistenten nach `common/auftrag` ([`assistent.md`](assistent.md) §4.1). `common` hängt an keinem anderen Anwendungspaket, auch nicht an
`security` — deshalb liegt der `MandantContext` dort, und was ihn herstellt, bleibt in `security`
(17.09.2026, Punkt 186). `PaketstrukturTest` (ArchUnit) prüft das, dazu: `jooq.glassfish` nur in
`*Repository`-Klassen, `MandantContext` als erster Parameter, kein direkter Zugriff auf die
Systemuhr, kein JPA, kein `javax.*`, kein Jackson 2, DataSources nur in `config`, die
namentlichen Ausnahmen von Regel M2, und im Assistenten: HTTP und Typen eines Modellanbieters nur
im Adapter-Unterpaket, kein Datenbankzugriff. **Der Paketname `payload` passt nicht mehr** (es sind Anzeige
und Protokolle, kein Proxy) und bleibt trotzdem; eine Umbenennung ist offener Punkt in
[`rohdaten-backend.md`](rohdaten-backend.md) §11 Nr. 2.

### Frontend

Next.js 16 (App Router), shadcn/ui, TanStack Query, TanStack Table, Recharts, nuqs. Next.js leitet
`/api` an das Backend weiter und trifft **keine** Berechtigungsentscheidung; rollenabhängige
Navigation ist Bequemlichkeit, verbindlich prüft das Backend. Das Frontend hält **keine eigenen
Daten**. Filterzustand liegt in der URL (nuqs). Grundlagen in
[`frontend-grundlagen.md`](frontend-grundlagen.md), Gestaltung in
[`visuelles-konzept.md`](visuelles-konzept.md).

### Erweiterbarkeit

Neue Ansichten sind ohne Umbau ergänzbar: eine gemeinsame Filter- und Paginierungsabstraktion, eine
Layout-Hülle mit datengetriebener Navigation, je Ansicht ein eigenes Modul in Backend und Frontend.
Die API ist kennzahlenorientiert geschnitten, damit ein späterer Assistent dieselben Endpunkte
aufrufen kann ([`ROADMAP.md`](ROADMAP.md)).

### Betrieb

Backend als eigenständiges JAR mit eingebettetem Server, Frontend als Node-Prozess daneben, davor ein
Reverse Proxy, der beide unter einer Domain zusammenführt. Der Rollup-Job läuft als Profil `rollup`
derselben Anwendung. **Bewusst nicht im vorhandenen GlassFish:** Ein Monitoring-Werkzeug soll
unabhängig von dem laufen, was es beobachtet; eine Rückfalloption gibt es nicht (Spring Boot 4
braucht Jakarta EE 11, die Instanz ist 9 oder 10). Profile, Variablen, Release und Rollback stehen in
[`betrieb.md`](betrieb.md).

---

## 7. Sicherheit

### Authentifizierung

Eigene Benutzerverwaltung, BCrypt mit Kostenfaktor 12. Serverseitige Session; Session-ID im Cookie
mit `HttpOnly`, `SameSite=Lax` und `Secure`. Kein JWT — bei externen Nutzern wiegt sofortige
Rücknehmbarkeit schwerer. `Secure` ist nur im Profil `dev` aus; `CookieAttributeTest` hält das.

Sperre nach fünf Fehlversuchen für 15 Minuten, dazu eine Begrenzung je IP. Die Nutzersperre ist
persistent in `app_user`, die IP-Begrenzung liegt **nur im Arbeitsspeicher** — sonst wäre der
Schutz selbst der Angriffsweg. Fehlermeldungen bleiben unspezifisch; bei unbekanntem Benutzernamen
wird trotzdem gegen einen Dummy-Hash gerechnet, sonst verriete die Laufzeit den Namen. **Eine
Ausnahme:** War das Passwort richtig und das Konto gesperrt oder deaktiviert, wird das benannt.
Sperrfristen und Sitzungsablauf rechnen mit der Systemuhr.

**Die Alt-Benutzertabelle wird nicht übernommen** (Entscheidung 28.07.2026): `GlassfishDB.User`
bleibt lesbar, die Rolle `Admin` bedeutet dort etwas anderes, und eine Übernahme hätte Konten quer
über die Mandantengrenze gehoben. Ihre Klartext-Passwörter werden nicht gelesen und gelten als
kompromittiert. Das erste Admin-Konto legt das Profil `bootstrap` aus Umgebungsvariablen an — nur,
wenn noch kein `ADMIN` existiert, und mit Zwang zur Passwortänderung. **Jedes neue Konto startet mit
Änderungszwang**; bis dahin lehnt jeder Endpunkt außer Abmelden, Selbstauskunft und Passwortwechsel
ab. Ein Passwort-Reset erfolgt durch einen Admin (A3). Vollständig in
[`authentifizierung.md`](authentifizierung.md).

### Mandantentrennung — die wichtigste Regel des Projekts

**Kein Endpunkt nimmt eine Mandanten-ID entgegen** (M1); der Mandant kommt aus der Session.
**Genau drei benannte Ausnahmen**, alle in [`mandantentrennung.md`](mandantentrennung.md) §3:
`POST /api/auth/mandant` (Wechsel, geprüft gegen die zulässige Menge), `POST /api/admin/users`
(Konto anlegen, nur `ADMIN`) und `PUT /api/admin/users/{id}/tenants` (Mandantenmenge pflegen, nur
`ADMIN`). Alle drei definieren eine Berechtigung, statt einen Datenausschnitt abzufragen. Ein
existierender, aber nicht zulässiger Mandant antwortet wie eine erfundene ID mit `404`. Taucht eine
vierte auf, ist das ein Signal.

**Jede Repository-Methode auf das Quellschema hat den `MandantContext` als ersten Pflichtparameter**
(M2), ohne Überladung; der Kontext trägt immer genau eine Mandanten-ID. **Drei benannte
Ausnahmen** ([`mandantentrennung.md`](mandantentrennung.md) §4): `@OhneMandantenkontext` an drei
Methoden von `security/MandantRepository`, `RollupLeseRepository` (der Rollup liest mandantenfrei,
26.08.2026) und `DienstLeseRepository` (liest nur `Service`, E‑123). ArchUnit hält sie namentlich und
eng. Der Filter über `ProjectMandant` ist Teil jedes Statements: Wer eine fremde `MessageID` errät,
bekommt null Zeilen.

**Pro Endpunkt ein Mandanten-Isolationstest** — Mandant A fragt ab, Daten von Mandant B sind
unerreichbar. Ohne ihn wird nicht gemergt.

### Assistent

Der Assistent (Linie `assistent`, [`assistent.md`](assistent.md)) schickt, was der Kunde im Chat
schreibt, an einen Modellanbieter, zuerst die **Anthropic-API in den USA**: Verarbeitung nur `us`
oder `global`, Speicherung nur `us` (Auskunft vom 16.10.2026). Mit hin gehen die feste Anweisung,
Name, Beschreibung und Struktur der angebotenen Auftragstypen und die Prüfergebnisse — **nicht**
Benutzername, Mandanten-ID, Daten aus `GlassfishDB` oder Aufträge. **Er ist abgeschaltet**
(Schalter `overlord.assistent.aktiv`, Vorgabe aus; ohne Schlüssel ebenfalls aus), bis der
Datenschutzbeauftragte die Auftragsverarbeitung geprüft hat; abgeschaltet geht kein einziger
Aufruf hinaus. **Gespräche liegen nur im Arbeitsspeicher**, gebunden an Sitzung, Nutzer und
Mandant, nicht in der Datenbank, nicht in der Sitzung, nicht im Log; protokolliert werden nur
Metadaten. Anlegen kann nur der Kunde per Klick, nie das Modell.

### Rohdatenzugriff

Nutzer sehen zu einer Nachricht **alle zugehörigen Dateien** — Nutzdaten in jeder Fassung und die
Protokolle der Schritte —, **sehen sie im Browser an** und laden sie herunter. Das ist sensibler als
die Metadaten: Dort stehen Preise, Mengen und Kundendaten. Vollständig in [`rohdaten.md`](rohdaten.md),
[`rohdaten-backend.md`](rohdaten-backend.md), [`rohdaten-frontend.md`](rohdaten-frontend.md).

- **Die Anzeige ist der Regelfall** und Rohtext: unverändert, Festbreitenschrift, als Textknoten,
  niemals HTML. Seit dem 17.09.2026 gibt es die Auswahl *Darstellung* (EDIFACT, ANSI X12, VDA, IDoc,
  XML, JSON, Hex), die nur die **Struktur** formatiert — Umbrüche und Einrückung außerhalb von
  Daten — als reine Funktion im Browser (E‑193 bis E‑205,
  [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md)). Ausgeschlossen bleibt die Zerlegung
  in Segmente **mit Beschreibung**.
- **Jeder Abruf läuft über das Backend**, nie als Link in den Browser; der Filestore kennt unsere
  Nutzer nicht. Kein Endpunkt nimmt einen Verweis, eine Ablagenkennung oder eine GUID entgegen; die
  Kennung eines Artefakts ist `<MessageActionID>-<MessagePropertyName>`.
- **Ablauf:** Die Verweise stehen als `<Dienst>.Payload.GUID` und `<Dienst>.Log.GUID` in
  `MessageProperty`; die Kennung vor der Pipe ist eine `Service.ServiceID` und wird über den
  Primärschlüssel aufgelöst. Das Backend prüft die Mandantenzugehörigkeit im Statement, ruft die
  Ablage per **SOAP** (`RETRIEVE`, ohne WSDL, ohne Authentifizierung) ab, bekommt ein **ZIP**, liest
  und entpackt es und gibt neu aus.
- **Die Ablagen sind keine Spiegel:** kein Rückfall auf eine andere Ablage (20 von 20 Kreuzabrufen
  scheitern, M68).
- Download nur mit `Content-Disposition: attachment` und `application/octet-stream`; der Anzeigepfad
  liefert JSON bzw. `text/plain` mit `nosniff`.
- **Der Beschnitt bei Protokollen hängt an der Rolle:** `MANDANT` sieht den Bereich zwischen den
  Marken, pfadmaskiert; `ADMIN` die vollständige Datei. **Anzeige und Download gehen durch dieselbe
  Aufbereitung.**
- **Drei Ereignisarten im `audit_log`:** angesehen, heruntergeladen, Abruf fehlgeschlagen — je mit
  Nutzer, Nachricht, Zeitpunkt, IP und der Fassung (beschnitten oder vollständig).
- Alle Mandantennutzer sind berechtigt; eine zweite Berechtigungsstufe gibt es nicht. Das tote Flag
  `download_allowed` ist mit `V7` entfallen ([`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md)).

---

## 8. Verbindliche Leistungsregeln

**Mengengerüst der Testkopie** — Zeilen gezählt, Bytegrößen aus `DATA_LENGTH`/`INDEX_LENGTH`
erhoben (byteidentisch über fünf Messtage, [`messungen-schritt7.md`](messungen-schritt7.md) M44‑0).
`information_schema.TABLE_ROWS` ist eine Schätzung und lag bis zu 60,9 % daneben; sie gilt nicht als
Zählung.

| Tabelle | Zeilen | Herkunft | Größe |
|---|---:|---|---:|
| `MessageProperty` | 75.571.462 | gezählt, M44 (12.08.2026) | 61,0 GB |
| `MessageBAM` | 15.406.350 | gezählt, M33‑0 (11.08.2026) | 7,1 GB |
| `MessageAction` | 10.308.590 | gezählt, M14 (07.08.2026) | 3,0 GB |
| `Message` | 3.341.519 | gezählt, M0 (01.08.2026) | 2,9 GB |
| `Process` | 1.503 | gezählt, M10, bestätigt M44 | — |
| `Project` | 140 | gezählt 28.07.2026, bestätigt M44 | — |
| `User` | 36 | gezählt, M44 | — |

Rund **7.300 Nachrichten pro Tag** im dichten Bestand (3.336.386 Zeilen über 456 Tage,
[`messungen-schritt4.md`](messungen-schritt4.md)). Die Testkopie reicht von `2024-10-01` bis
`2026-07-08`, darin eine fünfmonatige Lücke. ⚠️ **Die produktive Aufbewahrung ist ungedeckt:** Die
21,2 Monate beschreiben die Kopie; „rund 18 Monate, danach ein Archivsystem“ ist eine Auskunft
([`rohdaten.md`](rohdaten.md) §12), beim Betreiber nicht nachgefragt. Davon hängt ab, ob Rollupzeilen
unterhalb eines neuen Bestandsanfangs stehen bleiben ([`rollup.md`](rollup.md), Punkt 54).

1. **Jeder Listen-Endpunkt hat ein Pflicht-Zeitfenster.** Standard 24 Stunden, Maximum ein Jahr.
   Ältere Monate des Bestands sind über die Listen nicht erreichbar; wer sie braucht, braucht das
   Archivsystem, und das ist kein Bau dieses Projekts. Benannte Abweichungen:
   - **Beleg- und Property-Suche: Vorgabe 30 Tage**, Maximum ein Jahr — 24 Stunden fänden 279 von
     234.159 Treffern (M35); 30 Tage kosten 1.655,8 ms, ein Jahr 8.939,7 ms (M47)
     ([`bam-suche.md`](bam-suche.md) §2, [`property-suche.md`](property-suche.md) §5).
   - **Präfixmodus der Belegsuche auf 30 Tage gedeckelt** — über ein Jahr Abbruch (M50).
   - **Kacheln *Läuft* und *Wartend* ohne Zeitfenster** — sie lesen nur die offenen Zeilen
     ([`dashboard.md`](dashboard.md) §5).

   **Geltungsbereich (E‑1016, [`tasks.md`](tasks.md)):** Diese Regel und die Regeln 2 bis 6 sind mit
   der Last auf `GlassfishDB` begründet und gelten für Lesezugriffe dort. Tabellen in
   `overlord_monitor`, die nicht aus `GlassfishDB` abgeleitet sind, folgen ihnen nicht — so liest
   `GET /api/tasks` die Aufträge des Mandanten ohne Zeitfenster und ohne Cursor. Die Rollup-Tabellen
   sind aus `GlassfishDB` abgeleitet und bleiben unter den Regeln. Regel 7 gilt für jede Abfrage.

   ⚠️ **Das Jahresmaximum trägt für den größten Mandanten möglicherweise nicht.** Über den Zeitbereich
   in `Message` gibt der Optimizer `MessageLastUpdateIDX` zwischen 30 und 90 Tagen auf und liest
   voll: über 90 Tage in allen zehn Messfällen Vollscan oder Abbruch (M168, M171), über ein Jahr hält
   ein anders gebautes Statement den Index (M50). Wo die Grenze liegt, ist für kein Statement
   gemessen (Punkt 159). **Wer einen Listen-Endpunkt baut, der über den Zeitbereich in `Message`
   einsteigt, misst ihn über ein Jahr gegen `NEXANS`.**

2. **Keine Live-Aggregation über `Message`.** Dashboard-Kennzahlen kommen aus den Rollup-Tabellen,
   die ein stündlicher Job fortschreibt. Der Grund ist Ladezeit und Ruhe auf der Produktion, nicht
   Unmöglichkeit — das Altsystem aggregiert tagesweise. Die Regel betrifft `GlassfishDB`; Zählungen
   über Tabellen in `overlord_monitor`, die nicht aus `GlassfishDB` abgeleitet sind, fallen nicht
   darunter (E‑1016, siehe Regel 1). **Fünf benannte Ausnahmen**, je einzeln begründet, gemessen und
   gezählt:

   | # | Ausnahme | liest live | warum nicht aus dem Rollup | gemessen |
   |---|---|---|---|---|
   | 1 | Kachel *Läuft* (E‑72, 03.09.2026) | `COUNT(*)`, `MIN(MessageLastUpdate)` über `RUNNING` | flüchtiger Status, der Rollup trägt keine Statushistorie | M143: 0,94–1,25 ms |
   | 2 | Kachel *Wartend* (E‑73, 03.09.2026) | dasselbe über `SUSPENDED` | wie 1 | M143: 3,34–4,66 ms |
   | 3 | Live-Rest der laufenden Stunde (16./17.09.2026, [`live-rest.md`](live-rest.md)) | `COUNT(*)` je Stundeneimer, Prozess, Rohstatus seit dem letzten Lauf, höchstens vier Eimer | hängt am Takt des Laufs: kurz vor dem nächsten Delta-Lauf fehlte bis zu eine Stunde | M185 (Baum): typisch höchstens 141,6 ms, dichtester Bereich 337,0 ms; M186 (Dashboard): höchstens 388,6 ms |
   | 4 | Fehler live (E‑208, 18.09.2026, [`fehler-live.md`](fehler-live.md)) | `COUNT(*)` je Stundeneimer, Prozess, Rohstatus über die Fehlerbedingung | ein Fehlerstatus ist durch Nachverarbeitung nicht endgültig; der Delta-Lauf entfernt Abgänge aus alten Eimern nicht | M188: Lesung 20,1–31,5 ms, Seite höchstens 418,4 ms; M189 (Baum) höchstens 167,2 ms typisch, 349,3 ms dicht |
   | 5 | Block 6 „Zuletzt aufgefallen“ (seit E‑90, 04.09.2026; eingetragen 16.10.2026, [`dashboard.md`](dashboard.md) §7a) | je Prozess `COUNT(*)` und jüngster Zeitpunkt der Nachrichten, die die Fehlerbedingung erfüllen, höchstens zehn Zeilen | der Block nennt den jüngsten Zeitpunkt, der Rollup kennt nur die Stunde; und ein Fehlerstatus ist durch Nachverarbeitung nicht endgültig (wie Nr. 4) — *hergeleitet, in `dashboard.md` nicht ausgeschrieben* | M146: 20,0–26,2 ms in SQL, 23,3–30,2 ms durch den Code, Einstieg über `MessageStatusIDX` |

   Die Ausnahmen 3 und 4 haben je zwei Verbraucher (Prozessbaum und Dashboard bzw. Übersicht und
   Prozessbaum), aber einen Baustein in `common`; sie bleiben je eine. **Keine Ausnahme** sind die
   Erscheinungsbedingung von *Wartend* (liest Stammdaten, E‑74) und die Katalog-Nachlesung des
   Live-Rests (Primärschlüssel). Die frühere Ausnahme *Überfällig* (E‑c) ist mit E‑71 entfallen.
   Block 6 gruppiert seit E‑90 je Prozess; vorher war er eine gedeckelte Liste ohne Aggregation. Als
   Ausnahme eingetragen ist er seit dem 16.10.2026 (Entscheidung des Auftraggebers, Punkt 253). Wer
   eine weitere Kennzahl live rechnen will, trägt sie hier ein, begründet und misst sie.

3. **Keine `OFFSET`-Paginierung.** Cursor-basiert über `(MessageLastUpdate, MessageID)`. Beleg- und
   Property-Suche blättern nicht; sie haben ein hartes Limit von 50 Treffern
   ([`bam-suche.md`](bam-suche.md) §5).

4. **`MessageProperty` nur über `MessageID`.** Nie filtern, gruppieren oder sortieren über den Wert.
   **Eine benannte Ausnahme:** Die Property-Suche steigt über **Name und Wert** ein
   (`MessagePropertyName = ? AND MessagePropertyValue = ?`, E‑102, eingetragen 08.09.2026,
   [`property-suche.md`](property-suche.md) §4). Gefiltert wird nur mit `=`, gruppiert und sortiert
   über den Wert auch dort nie; für jeden anderen Zugriff gilt die Regel unverändert. Der Grund: Der
   regelkonforme Pfad trägt die Suche nicht — über 30 Tage 3.186 bis 8.195 ms gegen 789 bis
   1.862 ms über den Wertindex (M171, M166), über 90 Tage Abbruch in jeder Form (M168: 42 von 42,
   M171: 4 von 4). Über 24 Stunden wählt der Optimizer ihn nur von selbst, nicht garantiert (M170:
   14 von 18 Kombinationen weichen aus). Der Preis: bis zu 124.715 gelesene Indexeinträge beim
   häufigsten Wert (M165); über ein Jahr kostet der teuerste Name 5.369 ms an der Wanduhr
   ([`property-suche.md`](property-suche.md) §6, ohne M-Nummer). Schon das Zählen eines Namens über den
   Bestand kostet 125,527 s (M157).

5. **Belegsuche mit hartem Limit und Mindestlänge** des Suchbegriffs; BAM-Werte wie `050` kommen
   millionenfach vor. Gebaut ist das Limit; **eine Mindestlänge haben weder die exakte Suche noch der
   Präfixmodus noch die Property-Suche** — begründet in [`bam-suche.md`](bam-suche.md) §1 und §19
   (M47: eine zweistellige Eingabe kostet 20,2 bis 22,4 ms). Ob diese Abweichungen hier als benannte
   Ausnahmen geführt werden, ist offen ([`offene-punkte.md`](offene-punkte.md), Teil 3,
   `bam-suche.md`).

6. **Der Rollup-Job läuft gedrosselt — dort, wo es wirkt.** Der stündliche Delta-Lauf ist ein
   Statement ohne Drosselung (dichteste Stunde 88,167 ms, 0,0024 % der Stunde, M88); der nächtliche
   Volllauf liest in Monatsscheiben mit konfigurierbarer Pause dazwischen (45,772 s ohne, 69,186 s
   mit 1 s Pause, M92) ([`rollup.md`](rollup.md) §9). Wie viel Last die Produktion nachts verträgt und
   ob 03:00 die richtige Stunde ist, ist ungemessen (Punkt 50).

7. **Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen** (`EXPLAIN` plus Laufzeit,
   Wanduhr neben dem Profil). Die Testkopie wählt Pläne nicht wie die Produktion (A12 widerlegt,
   M193). **Jede Änderung, die den Plan der Nachrichtenliste oder ihrer Suche berühren kann, wird
   zusätzlich per `EXPLAIN` in der Produktion geprüft** — über
   [`scripts/diagnose-produktion/suche.sql`](../scripts/diagnose-produktion/suche.sql), erweitert um
   den Fall, ausgeführt von Lukas vor dem Merge (E‑239,
   [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) Regel L7).

Zur Laufzeit wird auf der Produktion gelesen; eine Replica gibt es nicht. Die Regeln 1 bis 6 sind
deshalb für Lesezugriffe auf `GlassfishDB` nicht verhandelbar (Geltungsbereich bei Regel 1, E‑1016).

### Umgebungen

| Umgebung | Inhalt | Verwendung |
|---|---|---|
| Testkopie (interner Host, MariaDB 10.6) | Vollkopie der Produktion, **Datenstand 08.07.2026**: `MIN(MessageLastUpdate) = 2024-10-01 02:00:28`, `MAX = 2026-07-08 17:21:10`, 3.341.519 Zeilen (M0, seither nicht neu befüllt); die Verteilung ist ungleichmäßig ([`messungen-schritt4.md`](messungen-schritt4.md)) | Entwicklung, Tests, Messung; geteilt von `main` und `assistent`. Pläne unter der Statistik der Kopie — die Produktion kann anders wählen (Regel 7) |
| Produktion | live, **produktiv seit September 2026**, Linux-Server hinter einem Reverse Proxy (Auskunft Lukas) | Laufzeitdatenquelle der Anwendung; Betrieb in [`betrieb.md`](betrieb.md) |

Der Codegen läuft gegen die Testkopie, betrieben wird gegen die Produktion. `DatenzugriffDbIT`
gleicht die generierten Spalten von `Message` gegen `information_schema` der verbundenen Datenbank
ab.

**Referenzzeitpunkt.** Die Testkopie liegt hinter der realen Uhr; ein 24‑Stunden-Fenster liefert dort
null Zeilen. Deshalb gibt es zwei `Clock`-Beans: in der Produktion die Systemuhr, im Profil `dev` ein
`Clock.offset(...)`, dessen Versatz beim Start aus `MAX(Message.MessageLastUpdate)` berechnet wird
und weiterläuft. Eine ArchUnit-Regel hält das Verbot von `now()`.

---

## 9. Funktionsumfang 1.0

| Funktion | Beschrieben in |
|---|---|
| Anmeldung, Sitzung, Passwortwechsel, Mandantenwechsel | [`authentifizierung.md`](authentifizierung.md), [`mandantentrennung.md`](mandantentrennung.md) |
| Übersicht: Verlauf, Kacheln *Nachrichten*, *Fehler*, *Läuft*, *Wartend*, Verteilung nach Partner und Richtung, „Zuletzt aufgefallen“, Stand | [`dashboard.md`](dashboard.md), [`dashboard-frontend.md`](dashboard-frontend.md) |
| Plattform-Block: Dienste mit Zeitgrenze und Erreichbarkeit der Ablagen (10d) | [`dienste.md`](dienste.md) |
| Rollup je Stunde, Tag und Monat mit Delta- und Volllauf | [`rollup.md`](rollup.md) |
| Live-Rest der laufenden Stunde und Fehler live in Baum und Übersicht | [`live-rest.md`](live-rest.md), [`fehler-live.md`](fehler-live.md) |
| Nachrichtenliste mit Pflicht-Zeitfenster, Cursor, Filtern, Freitextsuche und Fensterverengung | [`nachrichtenliste.md`](nachrichtenliste.md), [`prozessauswahl.md`](prozessauswahl.md) |
| Nachrichtendetail mit Schrittfolge in Klartext, Eigenschaften und Zeitleiste | [`nachrichtendetail.md`](nachrichtendetail.md) |
| Verkettung über Aufteilung, Zusammenführung und Quittung | [`verkettung.md`](verkettung.md) |
| Belegdaten am Detail | [`bam-werte.md`](bam-werte.md) |
| Belegsuche („wo ist mein Lieferschein?“), exakt und mit Präfix, mit kuratierten Sollängen | [`bam-suche.md`](bam-suche.md), [`bam-sollaengen.md`](bam-sollaengen.md) |
| Property-Suche über technische Werte, Absprung in den Prozessbaum | [`property-suche.md`](property-suche.md) |
| Rohdaten und Protokolle: Anzeige als Rohtext, Darstellung, Download | [`rohdaten.md`](rohdaten.md), [`rohdaten-backend.md`](rohdaten-backend.md), [`rohdaten-frontend.md`](rohdaten-frontend.md), [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) |
| Prozessansicht: Baum nach Partner → Richtung → Prozess oder nach Projekt, mit Übertragungsliste | [`process-view.md`](process-view.md) |
| Prozess-Katalog: Pflege, Heuristik, Massenzuordnung, Vorschlagsübernahme | [`prozess-katalog.md`](prozess-katalog.md), [`prozess-katalog-backend.md`](prozess-katalog-backend.md), [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md) |
| Benutzerverwaltung: Konten, Mandantenmenge, Sperre, Anlegen | [`benutzerverwaltung.md`](benutzerverwaltung.md), [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md), [`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md) |
| Neu laden und automatische Aktualisierung | [`neu-laden.md`](neu-laden.md) |
| Anzeigegröße in vier Stufen, Dunkelmodus, Spaltenwahl nach Containerbreite | [`dichte-umschalter.md`](dichte-umschalter.md), [`dunkelmodus.md`](dunkelmodus.md), [`spaltenwahl.md`](spaltenwahl.md) |

**Nicht enthalten**

- Die Problemkategorien *Überfällig* (widerlegt, E‑71) und *Unquittiert* (nicht definiert, E‑d), §4.2.
- Service-Überwachung über die Anzeige hinaus: keine Alarmierung, keine Schwellen, keine Historie,
  keine Lampe für Dienste ohne Zeitgrenze, kein `ServiceName` in einer Antwort (E‑116, E‑122).
- Zerlegung der Rohdaten in Segmente mit Beschreibung, Syntaxhervorhebung, Farbe im Inhalt.
- Benachrichtigungen und Alarmierung.
- Assistent und Chatbot.
- Löschen von Konten, Selbstbedienung beim Passwort.

---

## 10. Weiterentwicklung

`main` ist ab 1.0.0 die Release-Linie 1.x und bekommt nur Fixes aus dem Register. Die
Weiterentwicklung „Aufträge über den Assistenten“ läuft auf dem Zweig `assistent` und kommt als 1.1.0.
Ausbaustufen, Vorlage für Arbeitsaufträge und Stand stehen in [`ROADMAP.md`](ROADMAP.md).

---

## 11. Annahmen und Risiken

**Offen**

| # | Annahme oder ungedeckte Angabe | Risiko wenn falsch |
|---|---|---|
| A3 | Kein SMTP-Relay; der Passwort-Reset erfolgt durch einen Admin | Selbstbedienung fehlt, später nachrüstbar |
| A10 | MariaDB 10.6 bleibt in Betrieb | Wartungszeitraum seit Juli 2026 vorbei; ein Upgrade auf 11.x ändert Standard-Sortierungen — deshalb steht die Sortierung in jeder Migration |
| — | Takt von `MatchInterchange` „stündlich“ (§3.3) | ungedeckt; Quelle beim Altsystem einzuholen. Daran hängen jede Aussage zur Verzögerung der Quittung und *Unquittiert* |
| — | Produktive Aufbewahrung „rund 18 Monate“ (§8) | Auskunft, beim Betreiber nicht nachgefragt; daran hängen die erreichbare Spanne und Rollupzeilen ohne Nachrichten (Punkt 54) |
| — | Übermittlung an Anthropic in die USA auf Grundlage des EU-US Data Privacy Framework ([`assistent.md`](assistent.md) §3.3) | Prüfung durch den Datenschutzbeauftragten ausstehend (Punkt 1009); bis dahin ist der Assistent abgeschaltet |
| — | Wächter auf `RUNNING`, absichtliches Warten von `SUSPENDED` (§4.2) | Auskunft vom 03.09.2026; gegen die Produktion zu prüfen mit der Abfrage in [`message-status.md`](message-status.md), „Die offene Prüfung“ |

**Geklärt** — A1 (Testkopie vorhanden, Datenstand 08.07.2026), A2 (n:m über `app_user_mandant`,
gepflegt seit 9a), A4 (Rohdaten für alle Mandantennutzer), A5 (eigenständiger Betrieb, kein
GlassFish), A6 (widerlegt: `COMMIT_REJECTED` und `EERP_FAILED` sind Fehler ohne Präfix, §4.1), A7
(1.503 Prozesse, gezählt), A8 (sechs Projekte ohne Mandant tragen null Nachrichten; ihre Nachrichten
sind für niemanden sichtbar, bewusst ohne Sonderpfad), A9 (die Belegsuche ist mit Fenster und Limit
begrenzbar, M33/M34), **A11** (unverschlüsselte Datenbankverbindung tragbar laut Auskunft vom
16.10.2026, nicht gemessen — Anwendung und Datenbank im selben Netz, Entscheidung 5), **A12**
(widerlegt am 13.10.2026: Die Testkopie wählt Pläne nicht wie die Produktion, M193; Abhilfe Regel 7).
Einzelheiten in [`annahmen-korrekturen.md`](annahmen-korrekturen.md) und im Archiv.

---

## 12. Glossar

| Begriff | Bedeutung |
|---|---|
| **Mandant** | Kunde bzw. Geschäftsbereich, die Sicherheitsgrenze des Systems |
| **Projekt** | Gruppierung von Prozessen; Bedeutung je Mandant unterschiedlich |
| **Prozess** | ein EDI-Ablauf, meist Partner plus Belegart |
| **SOS** | Sequence of Services — der konkrete, aus Bausteinen gebaute Ablauf |
| **SOSAction** | ein einzelner Baustein eines SOS |
| **Message** | eine Übertragung, der zentrale Datensatz |
| **MessageAction** | ein ausgeführter Schritt einer Übertragung |
| **BAM** | Business Activity Monitoring — fachliche Suchschlüssel wie die Lieferschein-Nr. |
| **Interchange** | EDIFACT-Austauschnummer, Grundlage der COMMIT-Zuordnung |
| **Rollup** | eigene Aggregate je Stunde, Tag und Monat in `overlord_monitor` |
| **Live-Rest** | der Verkehr seit dem letzten Rollup-Lauf, live dazugezählt |
| **Testkopie** | Vollkopie der Produktion mit Datenstand 08.07.2026, Grundlage aller Messungen |
