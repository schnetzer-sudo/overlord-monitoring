# Messungen vor Schritt 4

Erhebung gegen die **Testkopie** am 01.08.2026, vor dem Bau der Nachrichtenliste (Regel L7).

> **Was dieses Dokument ist und was nicht.** In **M0 bis M7** stehen ausschließlich erhobene Zahlen;
> dort werden keine Schlüsse gezogen. Was aus ihnen folgen könnte, steht unter „Offene
> Entscheidungen" als **Frage**, nicht als Antwort.
>
> **M8 und M9 sind anders** (nachgetragen am 01.08.2026, vor Schritt 4). Sie beantworten je genau
> eine Frage aus dieser Liste und ziehen die Schlussfolgerung **ausdrücklich** — beide entscheiden,
> wie der Code rechnet, und blieben sonst offen. Wo eine Frage damit erledigt ist, steht das unten
> an der Frage.
>
> **M11 bis M13 sind ebenfalls anders** (nachgetragen am 06.08.2026, Nachbesserung zu Schritt 4).
> Auch sie sind vor dem Bau erhoben worden und entscheiden je eine Frage der Oberfläche: M11 ist
> Vorarbeit für die BAM-Suche in Schritt 7, M12 entscheidet, ob der Zwischenschritte-Schalter
> datengetrieben wird, und **M13 entscheidet, ob der aktuelle Schritt überhaupt anzeigbar ist**. Ihre
> Schlussfolgerungen stehen ausdrücklich dabei.

---

## 0. Rahmen der Messung

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22` — niemals die Produktion |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → `1` (Merkmal der Testkopie, [`datenzugriff.md`](datenzugriff.md) §4); Datenstand deckt sich mit dem dort dokumentierten |
| Benutzer | **Lesebenutzer**, `SELECT` auf beide Schemata, sonst nichts (per `SHOW GRANTS` bestätigt) |
| Serverzeit während der Messung | `2026-08-01 11:48:48` (`UTC_TIMESTAMP` `09:48:48`, also UTC+2) |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES` |

**Warum serverseitig gemessen wird:** `SHOW PROFILES` liefert die reine Ausführungszeit im Server.
Eine Messung um den Client-Aufruf herum enthielte Prozessstart, Verbindungsaufbau und Netzweg — bei
Abfragen im Millisekundenbereich (M4, M7) wäre das ein Vielfaches des gemessenen Werts.

**Jede Angabe ist die beste von N Läufen nach einem Aufwärmlauf.** N steht bei jeder Messung dabei:
fünf, wo die Abfrage schnell genug ist, drei bei den vollen Durchläufen über `Message`, die je rund
zehn bis zwanzig Sekunden dauern. Wo N kleiner als fünf ist, ist das keine stillschweigende Kürzung,
sondern hier vermerkt.

### Abweichung: der Kommandozeilen-Client

Die Aufgabenstellung nennt `--skip-ssl`. Auf diesem Rechner ist **kein `mariadb`-Client
installiert**; verfügbar ist ausschließlich der `mysql`-Client `8.0.46`, der mit MySQL Workbench
mitgeliefert wird. Dieser Client kennt `--skip-ssl` nicht mehr:

```
mysql: [ERROR] unknown option '--skip-ssl'.
```

Verwendet wurde deshalb die gleichbedeutende Option **`--ssl-mode=DISABLED`**. Die Wirkung ist
dieselbe — unverschlüsselte Verbindung, weil der Server kein TLS anbietet (Annahme A11). Alle
Statements unten sind so ausgeführt worden.

---

## M1 — Spaltennamen und Indizes

**Zuerst gelaufen.** Alle folgenden Statements sind gegen dieses Ergebnis abgeglichen worden.

### Statement

```sql
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Process','Project','ProjectMandant','Mandant','SOS',
                     'MessageBAMMandant','MessageBAMType','MessageMandantID')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

### Ergebnis

| Tabelle | # | Spalte | Typ | NULL | Schlüssel |
|---|---|---|---|---|---|
| `Mandant` | 1 | `MandantID` | `varchar(36)` | NO | PRI |
| `Mandant` | 2 | `MandantName` | `varchar(255)` | YES | |
| `Mandant` | 3 | `MandantDescription` | `text` | YES | |
| `MessageBAMMandant` | 1 | `MessageBAMType` | `smallint(6)` | NO | PRI |
| `MessageBAMMandant` | 2 | `MandantID` | `varchar(36)` | NO | PRI |
| `MessageBAMMandant` | 3 | `MessageBAMTypeSortIndex` | `smallint(6)` | **NO** | |
| `MessageBAMType` | 1 | `MessageBAMType` | `smallint(6)` | NO | PRI |
| `MessageBAMType` | 2 | `MessageBAMTypeDescription` | `varchar(70)` | YES | |
| `MessageMandantID` | 1 | `MessageID` | `varchar(36)` | NO | |
| `MessageMandantID` | 2 | `MandantID` | `varchar(36)` | NO | |
| `Process` | 1 | `ProcessID` | `varchar(36)` | NO | PRI |
| `Process` | 2 | `ProcessName` | `varchar(255)` | YES | |
| `Process` | 3 | `ProcessDescription` | `text` | YES | |
| `Process` | 4 | `ProjectID` | `varchar(36)` | YES | MUL |
| `Project` | 1 | `ProjectID` | `varchar(36)` | NO | PRI |
| `Project` | 2 | `ProjectName` | `varchar(255)` | YES | |
| `Project` | 3 | `ProjectDescription` | `text` | YES | |
| `ProjectMandant` | 1 | `ProjectID` | `varchar(36)` | NO | PRI |
| `ProjectMandant` | 2 | `MandantID` | `varchar(36)` | NO | PRI |
| `SOS` | 1 | `SOSID` | `varchar(36)` | NO | PRI |
| `SOS` | 2 | `SOSName` | `varchar(255)` | YES | |
| `SOS` | 3 | `SOSDescription` | `text` | YES | |
| `SOS` | 4 | `ProcessID` | `varchar(36)` | YES | MUL |

Laufzeit: **2,403 ms** (ein Lauf nach Aufwärmlauf; Aufwärmlauf 3,007 ms).

**Zusätzlich erhoben** (nicht in der Aufgabenstellung, aber von M2, M5 und M6 gebraucht) — die
Spalten von `Message`:

| # | Spalte | Typ | NULL | Schlüssel |
|---|---|---|---|---|
| 1 | `MessageID` | `varchar(36)` | NO | PRI |
| 2 | `SOSID` | `varchar(36)` | YES | |
| 3 | `SOSActionID` | `smallint(6)` | YES | |
| 4 | `MessageLastUpdate` | `timestamp` | YES | MUL |
| 5 | `MessageTimeout` | `smallint(6)` | YES | |
| 6 | `MessageStatus` | `varchar(30)` | YES | MUL |
| 7 | `SourceMessageID` | `varchar(36)` | YES | MUL |
| 8 | `TargetMessageID` | `varchar(36)` | YES | MUL |
| 9 | `ProcessID` | `varchar(36)` | YES | MUL |
| 10 | `Source` | `bit(1)` | YES | |
| 11 | `Target` | `bit(1)` | YES | |

### Korrekturen an den Vorlage-Statements

**Keine.** Sämtliche in den Vorlagen von M2 bis M7 vermuteten Spaltennamen existieren genau so:
`Process.ProcessID`, `Process.ProjectID`, `ProjectMandant.ProjectID`, `ProjectMandant.MandantID`,
`SOS.SOSID`, `SOS.SOSName`, `MessageBAMMandant.MandantID`, `MessageBAMMandant.MessageBAMType`,
`MessageBAMMandant.MessageBAMTypeSortIndex`, `MessageBAMType.MessageBAMTypeDescription`,
`Message.SOSID`, `Message.MessageTimeout`, `Message.MessageStatus`, `Message.MessageLastUpdate`,
`Message.ProcessID`. Alle Statements sind unverändert ausgeführt worden.

### Indizes

```sql
SELECT TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, NON_UNIQUE, CARDINALITY, INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Process','Project','ProjectMandant','Mandant','SOS',
                     'MessageBAMMandant','MessageBAMType','MessageMandantID','Message')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;
```

| Tabelle | Index | Spalten (in Reihenfolge) | eindeutig | Kardinalität |
|---|---|---|---|---|
| `Mandant` | `PRIMARY` | `MandantID` | ja | 10 |
| `Message` | `PRIMARY` | `MessageID` | ja | 3.560.486 |
| `Message` | `MessageLastUpdateIDX` | `MessageLastUpdate` | nein | 1.780.243 |
| `Message` | `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdate`, `ProcessID`, `MessageID` | nein | 3.560.486 |
| `Message` | `MessageStatusIDX` | `MessageStatus` | nein | 18 |
| `Message` | `Message_ProcessFK` | `ProcessID` | nein | 18 |
| `Message` | `ProejctIDIDX` | `ProcessID` | nein | 18 |
| `Message` | `SourceMessageIDIDX` | `SourceMessageID` | nein | 1.780.243 |
| `Message` | `TargetMessageIDIDX` | `TargetMessageID` | nein | 63.580 |
| `MessageBAMMandant` | `PRIMARY` | `MessageBAMType`, `MandantID` | ja | 69 |
| `MessageBAMMandant` | `MandantIDSortIndexBAMTYpeIDX` | `MandantID`, `MessageBAMTypeSortIndex`, `MessageBAMType` | nein | 69 |
| `MessageBAMType` | `PRIMARY` | `MessageBAMType` | ja | 62 |
| `Process` | `PRIMARY` | `ProcessID` | ja | 1.490 |
| `Process` | `Process_ProjectFK` | `ProjectID` | nein | 298 |
| `Project` | `PRIMARY` | `ProjectID` | ja | 142 |
| `ProjectMandant` | `PRIMARY` | `ProjectID`, `MandantID` | ja | 134 |
| `ProjectMandant` | `ProjectMandant_Mandant_idx` | `MandantID` | nein | 22 |
| `SOS` | `PRIMARY` | `SOSID` | ja | 1.865 |
| `SOS` | `SOS_ProcessFK` | `ProcessID` | nein | 1.865 |

Alle Indizes `BTREE`. **`MessageMandantID` erscheint nicht** — Views haben keine Indizes.

Laufzeit: **1,813 ms** (ein Lauf nach Aufwärmlauf; Aufwärmlauf 1,811 ms).

---

## M0 — Datenstand

### Statement

```sql
SELECT MIN(MessageLastUpdate) AS aeltester,
       MAX(MessageLastUpdate) AS juengster,
       COUNT(*)               AS anzahl
FROM Message;
```

### EXPLAIN

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `Message` | `index` | NULL | `MessageLastUpdateIDX` | 5 | NULL | 3.560.486 | `Using index` |

### Ergebnis

| ältester | jüngster | Anzahl |
|---|---|---|
| `2024-10-01 02:00:28` | `2026-07-08 17:21:10` | 3.341.519 |

**Laufzeit:** Aufwärmlauf 867 ms, beste von fünf **864 ms**
(864 · 868 · 879 · 873 · 876 ms).

### Abstand zur echten Systemzeit

```sql
SELECT (SELECT MAX(MessageLastUpdate) FROM Message) AS juengster,
       NOW() AS serverzeit, UTC_TIMESTAMP() AS serverzeit_utc,
       TIMESTAMPDIFF(HOUR,   (SELECT MAX(MessageLastUpdate) FROM Message), NOW()) AS abstand_stunden,
       TIMESTAMPDIFF(MINUTE, (SELECT MAX(MessageLastUpdate) FROM Message), NOW()) AS abstand_minuten;
```

| jüngster | Serverzeit | Serverzeit UTC | Abstand |
|---|---|---|---|
| `2026-07-08 17:21:10` | `2026-08-01 11:48:48` | `2026-08-01 09:48:48` | **570 Stunden / 34.227 Minuten** = 23 Tage 18:27:38 |

Laufzeit: 0,9 ms. Die lokale Uhr des Arbeitsplatzes zeigte im selben Moment `2026-08-01 11:48:48
+02:00` — Server und Arbeitsplatz gehen gleich.

> Dieses Maximum ist der **Bezugspunkt aller folgenden relativen Zeitfenster** und derselbe Wert,
> den die Dev-Uhr beim Start liest ([`datenzugriff.md`](datenzugriff.md) §6). Er ist gegenüber dem
> 28.07.2026 unverändert; die Testkopie ist seither nicht neu befüllt worden.

---

## M2 — `MessageTimeout`

### Statement (a)

```sql
SELECT COUNT(*) AS gesamt,
       SUM(MessageTimeout IS NULL)   AS ist_null,
       SUM(MessageTimeout = 0)       AS ist_null_wert,
       MIN(NULLIF(MessageTimeout,0)) AS kleinster,
       MAX(MessageTimeout)           AS groesster
FROM Message;
```

**EXPLAIN**

| id | select_type | table | type | possible_keys | key | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `Message` | `ALL` | NULL | NULL | 3.560.486 | — |

**Ergebnis**

| gesamt | `IS NULL` | `= 0` | kleinster (ohne 0) | größter |
|---|---|---|---|---|
| 3.341.519 | **0** | 6.915 | **1800** | **1800** |

**Laufzeit:** Aufwärmlauf 2.411 ms, beste von fünf **2.315 ms**.

### Statement (b)

```sql
SELECT MessageTimeout, COUNT(*) AS anzahl
FROM Message GROUP BY MessageTimeout ORDER BY anzahl DESC LIMIT 20;
```

**EXPLAIN**

| id | select_type | table | type | key | rows | Extra |
|---|---|---|---|---|---|---|
| 1 | SIMPLE | `Message` | `ALL` | NULL | 3.560.486 | `Using temporary; Using filesort` |

**Ergebnis** — das `LIMIT 20` greift nicht, es gibt nur **zwei** Werte:

| `MessageTimeout` | Anzahl | Anteil |
|---|---|---|
| `1800` | 3.334.604 | 99,79 % |
| `0` | 6.915 | 0,21 % |

**Laufzeit:** Aufwärmlauf 1.701 ms, beste von fünf **1.691 ms**.

### Zusatz: Verteilung je Status

Nicht verlangt, aber die einzige Aufteilung, die zeigt, *wo* die `0` steht:

```sql
SELECT MessageStatus, COUNT(*) AS anzahl,
       SUM(MessageTimeout IS NULL) AS ist_null,
       SUM(MessageTimeout = 0)     AS ist_null_wert,
       SUM(MessageTimeout > 0)     AS positiv
FROM Message GROUP BY MessageStatus ORDER BY anzahl DESC;
```

| `MessageStatus` | Anzahl | `IS NULL` | `= 0` | `> 0` |
|---|---|---|---|---|
| `FINISHED` | 2.030.986 | 0 | 5.711 | 2.025.275 |
| `MERGED` | 747.885 | 0 | 0 | 747.885 |
| `SPLITTED` | 400.845 | 0 | 0 | 400.845 |
| `EERP_RECEIVED` | 143.871 | 0 | 0 | 143.871 |
| `COMMIT_RECEIVED` | 12.654 | 0 | 0 | 12.654 |
| `ERROR_DUPLICATE` | 3.248 | 0 | 0 | 3.248 |
| `COMMIT_SENT` | 1.051 | 0 | **1.051** | 0 |
| `SUSPENDED` | 538 | 0 | 0 | 538 |
| `CHECKED` | 276 | 0 | 50 | 226 |
| `COMMIT_REJECTED` | 111 | 0 | **103** | 8 |
| `ERROR_TIMEOUT` | 52 | 0 | 0 | 52 |
| `CKECKED` | 2 | 0 | 0 | 2 |

Laufzeit: 11.428 ms (ein Lauf). Zwölf Statuswerte — genau die in
[`message-status.md`](message-status.md) dokumentierte Menge ohne `RUNNING`, kein neuer Wert.

### Was die Zahlen nahelegen — ohne Entscheidung

- **`NULL` kommt kein einziges Mal vor.** Die Frage „bedeutet `NULL` kein Timeout" ist auf dieser
  Datenlage nicht beantwortbar, weil der Fall nicht existiert. Die Spalte ist zwar `NULL`-fähig,
  wird aber nie so befüllt.
- **`0` kommt vor, und nicht gleichverteilt.** Bei `COMMIT_SENT` tragen *alle* 1.051 Zeilen die `0`,
  bei `COMMIT_REJECTED` 103 von 111, bei `FINISHED` 5.711 von 2.030.986 (0,3 %). Bei `MERGED`,
  `SPLITTED`, `EERP_RECEIVED`, `COMMIT_RECEIVED`, `SUSPENDED`, `ERROR_DUPLICATE` und `ERROR_TIMEOUT`
  kein einziges Mal.
- **Es gibt genau einen positiven Wert: `1800`.** Kein zweiter, keine Streuung. Eine Rechnung
  `MessageLastUpdate + MessageTimeout` ergibt damit für 99,79 % aller Zeilen denselben Abstand.
- `1800` in der Einheit **Minuten** wären 30 Stunden, in der Einheit **Sekunden** 30 Minuten.
  [`datenmodell.md`](datenmodell.md) und Regel Z2 nennen Minuten. Die Zahl allein entscheidet das
  nicht. → **Entschieden durch [M8](#m8--einheit-von-messagetimeout): Sekunden.**

---

## M3 — Nachrichten je Mandant

### Statement (a) — letzte 30 Tage relativ zu M0

```sql
SELECT pm.MandantID, COUNT(*) AS nachrichten
FROM Message msg
JOIN Process p         ON p.ProcessID  = msg.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE msg.MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 30 DAY
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
```

Fenster: `2026-06-08 17:21:10` bis `2026-07-08 17:21:10`.

**EXPLAIN**

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `msg` | `range` | `MessageLastUpdateProcessMessageIDX` | 5 | NULL | 10.444 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | PRIMARY | `p` | `eq_ref` | `PRIMARY` | 146 | `msg.ProcessID` | 1 | `Using where` |
| 1 | PRIMARY | `pm` | `ref` | `PRIMARY` | 146 | `p.ProjectID` | 1 | `Using index` |
| 2 | SUBQUERY | NULL | NULL | NULL | NULL | NULL | NULL | `Select tables optimized away` |

**Ergebnis** — eine einzige Zeile:

| `MandantID` | Nachrichten |
|---|---|
| `NEXANS` | 5.133 |

Kein `VOTG`, kein `SUTTONS`, kein anderer Mandant.

**Laufzeit:** Aufwärmlauf 33,47 ms, beste von fünf **32,62 ms**.

### Statement (b) — Gesamtbestand, ohne Zeitfenster

```sql
SELECT pm.MandantID, COUNT(*) AS nachrichten
FROM Message msg
JOIN Process p         ON p.ProcessID  = msg.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
```

**EXPLAIN**

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `msg` | `index` | `ProejctIDIDX` | 147 | NULL | 3.560.486 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | SIMPLE | `p` | `eq_ref` | `PRIMARY` | 146 | `msg.ProcessID` | 1 | `Using where` |
| 1 | SIMPLE | `pm` | `ref` | `PRIMARY` | 146 | `p.ProjectID` | 1 | `Using index` |

**Ergebnis**

| `MandantID` | Nachrichten | Anteil |
|---|---|---|
| `NEXANS` | 2.885.711 | 86,36 % |
| `SUTTONS` | 197.158 | 5,90 % |
| `VOTG` | 145.840 | 4,36 % |
| `IBIS` | 75.746 | 2,27 % |
| `IBISGUS` | 29.339 | 0,88 % |
| `ZAST` | 5.036 | 0,15 % |
| `WOC` | 2.529 | 0,08 % |
| `SYSTEM` | 151 | 0,005 % |
| `NXHBE` | 9 | 0,0003 % |
| **Summe** | **3.341.519** | **100 %** |

`Mandant` hat 10 Zeilen; `EDITIONLINGERI` kommt in der Auswertung nicht vor, hat also **keine**
Nachricht.

**Laufzeit:** Aufwärmlauf 16,432 s, beste von drei **16,392 s** (16,395 · 16,392 · 16,405 s).

### Zusatz: vervielfacht der n:m-Join?

```sql
SELECT (SELECT COUNT(*) FROM Message) AS zeilen_message,
       (SELECT COUNT(*) FROM Message msg
          JOIN Process p ON p.ProcessID = msg.ProcessID
          JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID) AS zeilen_nach_join;

SELECT anzahl_mandanten, COUNT(*) AS projekte FROM (
  SELECT ProjectID, COUNT(*) AS anzahl_mandanten FROM ProjectMandant GROUP BY ProjectID
) x GROUP BY anzahl_mandanten ORDER BY anzahl_mandanten;
```

| Zeilen in `Message` | Zeilen nach dem Join |
|---|---|
| 3.341.519 | **3.341.519** |

| Mandanten je Projekt | Projekte |
|---|---|
| 1 | **134** |

Laufzeiten: 12.568 ms und 2,2 ms.

**Befund:** `ProjectMandant` ist im Schema n:m, **in den Daten aber durchgängig 1:1** — kein einziges
Projekt gehört mehr als einem Mandanten. Der Join vervielfacht deshalb nichts, und keine Nachricht
geht verloren (Summe je Mandant = Gesamtzahl).

### Zusatz: Zeitspanne je Mandant

```sql
SELECT pm.MandantID, COUNT(*) AS nachrichten,
       MIN(m.MessageLastUpdate) AS aelteste, MAX(m.MessageLastUpdate) AS juengste
FROM Message m
JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY nachrichten DESC;
```

| `MandantID` | Nachrichten | älteste | jüngste |
|---|---|---|---|
| `NEXANS` | 2.885.711 | `2024-10-01 02:00:28` | **`2026-07-08 17:21:10`** |
| `SUTTONS` | 197.158 | `2024-11-26 12:05:31` | `2025-12-30 04:08:53` |
| `VOTG` | 145.840 | `2024-10-01 02:02:05` | `2025-12-30 04:02:04` |
| `IBIS` | 75.746 | `2024-10-01 05:09:26` | `2025-12-29 23:42:50` |
| `IBISGUS` | 29.339 | `2024-10-01 06:24:29` | `2025-12-29 23:13:50` |
| `ZAST` | 5.036 | `2024-10-01 05:01:23` | `2025-12-25 05:07:37` |
| `WOC` | 2.529 | `2024-10-01 06:13:07` | `2025-12-29 06:11:06` |
| `SYSTEM` | 151 | `2024-10-09 14:16:35` | `2025-12-15 10:40:47` |
| `NXHBE` | 9 | `2025-05-19 06:14:08` | `2025-05-19 06:48:07` |

Laufzeit: 20,465 s (ein Lauf).

**Befund:** Außer `NEXANS` endet **jeder** Mandant am `2025-12-30` oder früher.

---

## M4 — Verhalten der View `MessageMandantID`

### Metadaten der View

```sql
SELECT TABLE_NAME, ALGORITHM, IS_UPDATABLE, DEFINER, SECURITY_TYPE, CHECK_OPTION, VIEW_DEFINITION
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'MessageMandantID'\G
```

| Feld | Wert |
|---|---|
| `ALGORITHM` | `UNDEFINED` |
| `IS_UPDATABLE` | **`YES`** |
| `SECURITY_TYPE` | **`DEFINER`** |
| `DEFINER` | `root@<interne Adressmaske>` |
| `CHECK_OPTION` | `NONE` |
| `VIEW_DEFINITION` | **leer** |

Laufzeit: **1,822 ms** (ein Lauf nach Aufwärmlauf).

### Die Definition ist mit dem Lesebenutzer nicht lesbar

`VIEW_DEFINITION` bleibt leer, weil `information_schema.VIEWS` den Text nur mit dem Recht
`SHOW VIEW` herausgibt. Der direkte Weg scheitert ebenso:

```
SHOW CREATE VIEW GlassfishDB.MessageMandantID;
  -> ERROR 1142 (42000): SHOW VIEW command denied to user 'monitor_read'@'…'
                         for table `GlassfishDB`.`MessageMandantID`
```

`SHOW GRANTS FOR CURRENT_USER()` bestätigt: `USAGE` auf alles, `SELECT` auf `GlassfishDB` und
`SELECT` auf `overlord_monitor` — sonst nichts. `SHOW VIEW` ist ein eigenes Recht und in `SELECT`
**nicht** enthalten; der Schreibbenutzer hat auf `GlassfishDB` laut
[`datenzugriff.md`](datenzugriff.md) §1 ebenfalls nur `SELECT`, also auch kein `SHOW VIEW`.

**Folge für diese Messung:** Auch `EXPLAIN` über die View ist unmöglich.

```
EXPLAIN SELECT … FROM Message m JOIN MessageMandantID mm ON …
  -> ERROR 1345 (HY000): ANALYZE/EXPLAIN/SHOW can not be issued;
                         lacking privileges for underlying table
```

**Variante (a) hat deshalb kein EXPLAIN.** Das ist kein Versäumnis dieser Erhebung, sondern das
Messergebnis: Mit den Rechten, die die Anwendung besitzt, ist der Zugriffspfad durch die View nicht
einsehbar. Ein einfaches `SELECT` durch die View funktioniert dagegen — das Recht dafür ist `SELECT`
auf die View, und die Ausführung läuft mit den Rechten des Definers.

Gegenprobe, dass die View die Mandantenkette tatsächlich abbildet:

```sql
SELECT COUNT(*) FROM GlassfishDB.MessageMandantID WHERE MandantID = 'NEXANS';
```

→ **2.885.711** — auf die Zeile identisch mit M3 (b) für `NEXANS`. Laufzeit **1.216 ms** (ein Lauf
nach Aufwärmlauf von 1.233 ms). Ein Mandantenfilter **ohne** Zeitfenster kostet über die View also
gut eine Sekunde.

### Bezugspunkt und Fenstergröße

```sql
SELECT (SELECT MAX(MessageLastUpdate) FROM Message) AS bezug,
       (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 1 DAY AS untergrenze_24h,
       (SELECT COUNT(*) FROM Message
        WHERE MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 1 DAY)
       AS zeilen_24h_alle_mandanten;
```

| Bezug | Untergrenze 24 h | Zeilen im Fenster, alle Mandanten |
|---|---|---|
| `2026-07-08 17:21:10` | `2026-07-07 17:21:10` | **285** |

Gewählter Mandant: **`NEXANS`** — der einzige mit Zeilen in diesem Fenster (M3 a). Alle 285 Zeilen
gehören ihm.

### Variante (a) — über die View

```sql
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus
FROM Message m
JOIN MessageMandantID mm ON mm.MessageID = m.MessageID
WHERE mm.MandantID = 'NEXANS'
  AND m.MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 1 DAY
ORDER BY m.MessageLastUpdate DESC
LIMIT 50;
```

| | |
|---|---|
| EXPLAIN | **nicht möglich** (Fehler 1345, siehe oben) |
| gewählter Index | nicht einsehbar |
| geschätzte Zeilen | nicht einsehbar |
| Treffer im Fenster vor `LIMIT` | 285 |
| gelieferte Zeilen | 50 |
| **Laufzeit** | Aufwärmlauf 3,570 ms, beste von fünf **2,265 ms** (2,265 · 2,502 · 2,325 · 2,523 · 2,354 ms) |

### Variante (b) — handgeschrieben mit `EXISTS`

```sql
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus
FROM Message m
WHERE m.MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 1 DAY
  AND EXISTS (SELECT 1 FROM Process p
              JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
              WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
ORDER BY m.MessageLastUpdate DESC
LIMIT 50;
```

**EXPLAIN**

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `m` | `range` | `ProejctIDIDX`, `Message_ProcessFK`, `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX` | **`MessageLastUpdateIDX`** | 5 | NULL | **285** | `Using where` |
| 1 | PRIMARY | `p` | `eq_ref` | `PRIMARY`, `Process_ProjectFK` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | PRIMARY | `pm` | `eq_ref` | `PRIMARY`, `ProjectMandant_Mandant_idx` | `PRIMARY` | 292 | `p.ProjectID`, `const` | 1 | `Using where; Using index` |
| 2 | SUBQUERY | NULL | NULL | NULL | NULL | NULL | NULL | NULL | `Select tables optimized away` |

**`ANALYZE`** (tatsächliche statt geschätzter Zeilen):

| table | rows (geschätzt) | `r_rows` (tatsächlich) | `filtered` | `r_filtered` |
|---|---|---|---|---|
| `m` | 285 | **50,00** | 100,00 | 100,00 |
| `p` | 1 | 1,00 | 100,00 | 100,00 |
| `pm` | 1 | 1,00 | 100,00 | 100,00 |

| | |
|---|---|
| gewählter Index | `MessageLastUpdateIDX` |
| geschätzte Zeilen | 285 |
| **kein** `Using filesort` | die Sortierung nach `MessageLastUpdate DESC` kommt aus dem Index |
| tatsächlich gelesene Zeilen | 50 — das `LIMIT` bricht ab, sobald es voll ist |
| **Laufzeit** | Aufwärmlauf 1,867 ms, beste von fünf **1,233 ms** (1,818 · 1,810 · 1,855 · 1,233 · 1,799 ms) |

### Gegenprobe: liefern beide Wege dieselbe Menge?

```sql
SELECT COUNT(*) AS nur_view FROM ( … View-Weg … ) v
LEFT JOIN ( … EXISTS-Weg … ) e ON e.MessageID = v.MessageID
WHERE e.MessageID IS NULL;
```

→ **0**. Beide Wege liefern im gemessenen Fenster dieselbe Zeilenmenge. Laufzeit 5,6 ms.

### Gegenüberstellung

| | (a) View | (b) `EXISTS` |
|---|---|---|
| EXPLAIN einsehbar | **nein** (Fehler 1345) | ja |
| gewählter Index | unbekannt | `MessageLastUpdateIDX` |
| geschätzte Zeilen | unbekannt | 285 |
| Laufzeit, beste von fünf | **2,265 ms** | **1,233 ms** |
| Ergebnismenge | identisch | identisch |

> ⚠️ **Zur Aussagekraft:** Das 24-Stunden-Fenster enthält 285 Zeilen, nicht die rund 5.000, die ein
> normaler Tag hätte (siehe „Auffälligkeiten"). Beide Laufzeiten sind an einem sehr kleinen Fenster
> gemessen.

---

## M5 — `SOSID`

### Statement (a)

```sql
SELECT COUNT(*) AS gesamt,
       SUM(SOSID IS NULL OR SOSID = '') AS ohne_sos
FROM Message;
```

**EXPLAIN**

| id | select_type | table | type | key | rows | Extra |
|---|---|---|---|---|---|---|
| 1 | SIMPLE | `Message` | `ALL` | NULL | 3.560.486 | — |

**Ergebnis**

| gesamt | ohne `SOSID` |
|---|---|
| 3.341.519 | **0** |

**Laufzeit:** Aufwärmlauf 1.592 ms, beste von drei **1.492 ms**.

### Statement (b)

```sql
SELECT COUNT(*) AS verwaist
FROM Message m LEFT JOIN SOS s ON s.SOSID = m.SOSID
WHERE m.SOSID IS NOT NULL AND m.SOSID <> '' AND s.SOSID IS NULL;
```

**EXPLAIN**

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `m` | `ALL` | NULL | NULL | NULL | 3.560.486 | `Using where` |
| 1 | SIMPLE | `s` | `eq_ref` | `PRIMARY` | 146 | `m.SOSID` | 1 | `Using where; Using index; Not exists` |

**Ergebnis**

| verwaist |
|---|
| **0** |

**Laufzeit:** Aufwärmlauf 10,263 s, beste von drei **10,303 s**.

### Zusatz: Pflegezustand von `SOSName`

```sql
SELECT COUNT(*) AS sos_gesamt,
       SUM(SOSName IS NULL) AS name_null,
       SUM(SOSName = '')    AS name_leer,
       SUM(SOSName IS NOT NULL AND SOSName <> '') AS name_gepflegt,
       MIN(CHAR_LENGTH(SOSName)) AS kuerzester, MAX(CHAR_LENGTH(SOSName)) AS laengster
FROM SOS;
```

| `SOS` gesamt | `NULL` | leer | gepflegt | kürzester Name | längster Name |
|---|---|---|---|---|---|
| 1.818 | **0** | **0** | **1.818** | 6 Zeichen | 55 Zeichen |

Laufzeit: 3,1 ms.

**Befund:** Jede Nachricht hat eine `SOSID`, jede `SOSID` findet ihre Zeile in `SOS`, und jede
`SOS`-Zeile hat einen nicht leeren `SOSName`. Die Verknüpfung ist auf dieser Datenlage lückenlos.
`SOS` hat **keinen Index auf `SOSName`** (siehe M1).

---

## M6 — Sind `SPLITTED` und `MERGED` terminal?

### Statement

```sql
SELECT MessageStatus,
       COUNT(*) AS anzahl,
       MIN(MessageLastUpdate) AS aeltester,
       MAX(MessageLastUpdate) AS juengster,
       SUM(MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message)
                                - INTERVAL 1 DAY) AS letzte_24h,
       SUM(MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message)
                                - INTERVAL 7 DAY) AS letzte_7t
FROM Message
WHERE MessageStatus IN ('SPLITTED','MERGED','FINISHED','SUSPENDED')
GROUP BY MessageStatus;
```

**EXPLAIN**

| id | select_type | table | type | possible_keys | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `Message` | `range` | `MessageStatusIDX` | `MessageStatusIDX` | 123 | 3.560.486 | `Using where` |
| 3 | SUBQUERY | NULL | NULL | NULL | NULL | NULL | NULL | `Select tables optimized away` |
| 2 | SUBQUERY | NULL | NULL | NULL | NULL | NULL | NULL | `Select tables optimized away` |

**Ergebnis**

| `MessageStatus` | Anzahl | ältester | jüngster | letzte 24 h | letzte 7 T |
|---|---|---|---|---|---|
| `FINISHED` | 2.030.986 | `2024-10-01 02:00:28` | `2026-07-08 17:21:10` | 284 | 284 |
| `MERGED` | 747.885 | `2024-10-01 03:00:30` | **`2025-12-29 23:22:43`** | 0 | 0 |
| `SPLITTED` | 400.845 | `2024-10-01 02:04:30` | `2026-07-08 17:16:26` | 1 | 1 |
| `SUSPENDED` | 538 | `2025-12-23 11:04:13` | **`2025-12-29 12:37:16`** | 0 | 0 |

**Laufzeit:** Aufwärmlauf 18,671 s, beste von drei **18,773 s**.

### Zusatz: 30 und 90 Tage relativ zu M0

```sql
SELECT MessageStatus,
       SUM(MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 30 DAY) AS letzte_30t,
       SUM(MessageLastUpdate >= (SELECT MAX(MessageLastUpdate) FROM Message) - INTERVAL 90 DAY) AS letzte_90t,
       COUNT(*) AS gesamt
FROM Message WHERE MessageStatus IN ('SPLITTED','MERGED','FINISHED','SUSPENDED') GROUP BY MessageStatus;
```

| `MessageStatus` | letzte 30 T | letzte 90 T | gesamt |
|---|---|---|---|
| `FINISHED` | 5.116 | 5.116 | 2.030.986 |
| `MERGED` | 0 | 0 | 747.885 |
| `SPLITTED` | 17 | 17 | 400.845 |
| `SUSPENDED` | 0 | 0 | 538 |

Laufzeit: 17,037 s (ein Lauf). 30 und 90 Tage liefern dieselben Zahlen — im Fenster dazwischen
liegt keine einzige Zeile.

### Zusatz: Monatsverteilung (der Bezug, ohne den M6 nicht lesbar ist)

```sql
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m') AS monat, COUNT(*) AS zeilen,
       SUM(MessageStatus = 'FINISHED')  AS finished,
       SUM(MessageStatus = 'MERGED')    AS merged,
       SUM(MessageStatus = 'SPLITTED')  AS splitted,
       SUM(MessageStatus = 'SUSPENDED') AS suspended
FROM Message GROUP BY monat ORDER BY monat;
```

| Monat | Zeilen | `FINISHED` | `MERGED` | `SPLITTED` | `SUSPENDED` |
|---|---|---|---|---|---|
| 2024-10 | 241.203 | 138.659 | 54.345 | 34.350 | 0 |
| 2024-11 | 220.103 | 126.560 | 47.835 | 34.937 | 0 |
| 2024-12 | 169.237 | 101.883 | 38.428 | 21.295 | 0 |
| 2025-01 | 196.425 | 115.314 | 48.625 | 20.609 | 0 |
| 2025-02 | 191.937 | 111.244 | 47.876 | 22.029 | 0 |
| 2025-03 | 220.115 | 129.215 | 52.517 | 25.882 | 0 |
| 2025-04 | 226.421 | 139.754 | 50.687 | 24.872 | 0 |
| 2025-05 | 237.918 | 149.607 | 47.684 | 29.784 | 0 |
| 2025-06 | 229.904 | 141.341 | 52.901 | 25.489 | 0 |
| 2025-07 | 248.320 | 151.176 | 53.667 | 31.809 | 0 |
| 2025-08 | 229.618 | 143.435 | 51.204 | 25.718 | 0 |
| 2025-09 | 243.537 | 147.822 | 57.426 | 27.053 | 0 |
| 2025-10 | 234.598 | 143.790 | 55.219 | 24.150 | 0 |
| 2025-11 | 237.642 | 151.402 | 50.843 | 24.864 | 0 |
| 2025-12 | 209.408 | 134.668 | 38.628 | 27.987 | 538 |
| **2026-01 … 2026-05** | **0** | 0 | 0 | 0 | 0 |
| 2026-06 | 4.848 | 4.832 | 0 | 16 | 0 |
| 2026-07 | 285 | 284 | 0 | 1 | 0 |

Laufzeit: 11,587 s (ein Lauf).

### Was die Zahlen zeigen

Die Frage der Aufgabenstellung lautete: verteilen sich `SPLITTED` und `MERGED` über den gesamten
Zeitraum ähnlich wie `FINISHED`, oder ballen sie sich in den letzten Stunden?

- **Sie ballen sich nicht in den letzten Stunden.** Beide beginnen am `2024-10-01`, im selben Monat
  wie `FINISHED`, und laufen über fünfzehn Monate hinweg in stabiler Größenordnung mit: `MERGED`
  zwischen 38.428 und 57.426 je Monat, `SPLITTED` zwischen 20.609 und 34.937.
- **`MERGED` und `SUSPENDED` enden beide am `2025-12-29`**, `FINISHED` und `SPLITTED` reichen bis
  zum `2026-07-08`. Der Unterschied fällt mit dem Ende des dichten Bestands zusammen (siehe
  „Auffälligkeiten"), nicht mit einer Eigenschaft des Status.
- Die Werte `letzte_24h` und `letzte_7t` sind für alle vier Status Zahlen aus einem Fenster, in dem
  insgesamt 285 Zeilen liegen. Sie sagen über die Flüchtigkeit eines Status nichts aus.

`SPLITTED` und `MERGED` machen zusammen 1.148.730 von 3.341.519 Zeilen aus — **34,38 %**, was die
Angabe „rund 34 Prozent" in [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) bestätigt.

---

## M7 — BAM-Konfiguration je Mandant

### Statement

```sql
SELECT bm.MandantID, bm.MessageBAMType, bt.MessageBAMTypeDescription,
       bm.MessageBAMTypeSortIndex
FROM MessageBAMMandant bm
LEFT JOIN MessageBAMType bt ON bt.MessageBAMType = bm.MessageBAMType
ORDER BY bm.MandantID, bm.MessageBAMTypeSortIndex;
```

**EXPLAIN**

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `bm` | `index` | `MandantIDSortIndexBAMTYpeIDX` | 150 | NULL | 69 | `Using index` |
| 1 | SIMPLE | `bt` | `eq_ref` | `PRIMARY` | 2 | `bm.MessageBAMType` | 1 | — |

Der Index bedient `ORDER BY MandantID, MessageBAMTypeSortIndex` vollständig — kein `filesort`.

**Laufzeit:** Aufwärmlauf 1,167 ms, beste von fünf **0,937 ms**.

### Ergebnis — 69 Zeilen

| `MandantID` | `MessageBAMType` | Beschreibung | `SortIndex` |
|---|---|---|---|
| `IBIS` | 0 | Bestellnummer | 0 |
| `IBIS` | 1 | Auftragsnummer | 1 |
| `IBIS` | 2 | Lieferscheinnummer | 2 |
| `IBIS` | 3 | Rechnungsnummer | 3 |
| `IBISGUS` | 0 | Bestellnummer | 0 |
| `IBISGUS` | 1 | Auftragsnummer | 1 |
| `IBISGUS` | 2 | Lieferscheinnummer | 2 |
| `IBISGUS` | 3 | Rechnungsnummer | 3 |
| `NEXANS` | 9000 | Abladestelle_L_SAP | 9000 |
| `NEXANS` | 9001 | Abrufnummer_L_SAP | 9001 |
| `NEXANS` | 9002 | Lieferplannummer_L_SAP | 9002 |
| `NEXANS` | 9003 | Material-Nr. beim Lieferanten_L_SAP | 9003 |
| `NEXANS` | 9004 | Unsere Material-Nr._L_SAP | 9004 |
| `NEXANS` | 9005 | Werk_L_SAP | 9005 |
| `NEXANS` | 9006 | Lieferschein-Nr._L_SAP | 9006 |
| `NEXANS` | 9007 | Transport-Nummer_L_SAP | 9007 |
| `NEXANS` | 9008 | Beleg-Nr.  TSL _L_SAP | 9008 |
| `NEXANS` | 9009 | Beleg-Nr. GS_L_SAP | 9009 |
| `NEXANS` | 9010 | Materialbeleg (Entnahme)_L_SAP | 9010 |
| `NEXANS` | 9011 | Anlieferungs-Nr. ae_L_SAP | 9011 |
| `NEXANS` | 9012 | Charge_L_SAP | 9012 |
| `NEXANS` | 9013 | Nr. TSL_L_SAP | 9013 |
| `NEXANS` | 9034 | Bestellnummer_L_SAP | **9015** |
| `NEXANS` | 9035 | Werk Kunde_L_SAP | 9016 |
| `NEXANS` | 9036 | Lagerort Kunde_L_SAP | 9017 |
| `NEXANS` | 9037 | Packmittelnummer Kunde_L_SAP | 9018 |
| `NEXANS` | 9038 | Packmittelnummer Lieferant_L_SAP | 9019 |
| `NEXANS` | 9039 | Daten-Sender-Nummer_L_SAP | 9020 |
| `NEXANS` | 9014 | Lieferantennummer beim Kunden_K_SAP | **9021** |
| `NEXANS` | 9015 | Kundenwerk_K_SAP | 9022 |
| `NEXANS` | 9016 | Abladestelle_K_SAP | 9023 |
| `NEXANS` | 9017 | (JIT-) Abrufnummer_K_SAP | 9024 |
| `NEXANS` | 9018 | Kundenmaterialnummer_K_SAP | 9025 |
| `NEXANS` | 9019 | Bestellnummer vom Kunden_K_SAP | 9026 |
| `NEXANS` | 9020 | Lieferschein, Entnahme , PUS_K_SAP | 9027 |
| `NEXANS` | 9021 | Transportnummer_K_SAP | 9028 |
| `NEXANS` | 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 9029 |
| `NEXANS` | 9023 | Übertragungsnummer Gutschrift_K_SAP | 9030 |
| `NEXANS` | 9024 | Rechnungsnummer_K_SAP | 9031 |
| `NEXANS` | 9032 | Sendercode_K_SAP | 9032 |
| `NEXANS` | 9033 | Empfaengercode_K_SAP | 9033 |
| `NEXANS` | 9025 | Abladestelle_FORS | **9034** |
| `NEXANS` | 9026 | LS/RE-Nummer_FORS | 9035 |
| `NEXANS` | 9027 | Bestellnummer_FORS | 9036 |
| `NEXANS` | 9028 | Material-Nr. beim Kunden_FORS | 9037 |
| `NEXANS` | 9029 | Material-Nr.beim Lieferanten_FORS | 9038 |
| `NEXANS` | 9030 | Sender_Ident_FORS | 9039 |
| `NEXANS` | 9031 | Empf_Ident_FORS | 9040 |
| `NXHBE` | 1000 | InvoiceNumber | 1000 |
| `NXHBE` | 1001 | PartNumber | 1001 |
| `NXHBE` | 1002 | PONumber | 1002 |
| `NXHBE` | 1003 | TradingPartnerID | 1003 |
| `NXHBE` | 1004 | ShippingAdress | 1004 |
| `NXHBE` | 1005 | ShipperNumber | 1005 |
| `SUTTONS` | 2000 | OrderNumber | 2000 |
| `SUTTONS` | 2001 | VendorReference | 2001 |
| `VOTG` | 2000 | OrderNumber | 2000 |
| `VOTG` | 2001 | VendorReference | 2001 |
| `VOTG` | 2002 | InvoiceNumber VTG | **2002** |
| `VOTG` | 2011 | InvoiceNumber Vendor | **2002** |
| `VOTG` | 2003 | OrderLoadNo | 2003 |
| `VOTG` | 2004 | YourReference | 2004 |
| `VOTG` | 2005 | CustRef1 | 2005 |
| `VOTG` | 2006 | CustRef2 | 2006 |
| `VOTG` | 2007 | LoadNo | 2007 |
| `VOTG` | 2008 | Tanknummer | 2008 |
| `VOTG` | 2009 | Statusreason | 2009 |
| `VOTG` | 2010 | Statuscode | 2010 |
| `ZAST` | 3 | Rechnungsnummer | 3 |

### Zusatz: Pflegezustand des Sortierindex

```sql
SELECT COUNT(*) AS zeilen,
       SUM(MessageBAMTypeSortIndex IS NULL) AS sortindex_null,
       SUM(MessageBAMTypeSortIndex = 0)     AS sortindex_null_wert,
       MIN(MessageBAMTypeSortIndex) AS kleinster, MAX(MessageBAMTypeSortIndex) AS groesster,
       COUNT(DISTINCT MandantID) AS mandanten, COUNT(DISTINCT MessageBAMType) AS bamtypen
FROM MessageBAMMandant;
```

| Zeilen | `IS NULL` | `= 0` | kleinster | größter | Mandanten | BAM-Typen |
|---|---|---|---|---|---|---|
| 69 | **0** | 2 | 0 | 9040 | **7** | 62 |

`IS NULL` **kann** nicht vorkommen: Die Spalte ist laut M1 `smallint(6) NOT NULL`.

```sql
SELECT MandantID, COUNT(*) AS bamtypen,
       COUNT(DISTINCT MessageBAMTypeSortIndex) AS verschiedene_sortindizes,
       SUM(MessageBAMTypeSortIndex = 0) AS sortindex_null_wert,
       MIN(MessageBAMTypeSortIndex) AS kleinster, MAX(MessageBAMTypeSortIndex) AS groesster
FROM MessageBAMMandant GROUP BY MandantID ORDER BY bamtypen DESC, MandantID;
```

| `MandantID` | BAM-Typen | verschiedene SortIndizes | davon `= 0` | kleinster | größter |
|---|---|---|---|---|---|
| `NEXANS` | **40** | 40 | 0 | 9000 | 9040 |
| `VOTG` | 12 | **11** | 0 | 2000 | 2010 |
| `NXHBE` | 6 | 6 | 0 | 1000 | 1005 |
| `IBIS` | 4 | 4 | 1 | 0 | 3 |
| `IBISGUS` | 4 | 4 | 1 | 0 | 3 |
| `SUTTONS` | 2 | 2 | 0 | 2000 | 2001 |
| `ZAST` | **1** | 1 | 0 | 3 | 3 |

Laufzeiten: 1,2 ms und 0,9 ms.

Alle 69 Zeilen finden ihre Beschreibung in `MessageBAMType` — geprüft mit
`WHERE bt.MessageBAMType IS NULL OR bt.MessageBAMTypeDescription IS NULL OR … = ''` → **0** Zeilen
(1,0 ms).

### Antworten auf die Fragen der Aufgabenstellung

- **Ist `MessageBAMTypeSortIndex` durchgängig gepflegt oder überwiegend `NULL`/`0`?** Weder noch:
  `NULL` ist per Schema ausgeschlossen, und `0` steht in genau **2 von 69** Zeilen — bei `IBIS` und
  `IBISGUS` jeweils einmal, dort als kleinster Wert einer lückenlosen Folge `0,1,2,3`, also
  erkennbar als regulärer erster Platz und nicht als fehlender Wert.
- **Meiste BAM-Typen:** `NEXANS` mit **40**. **Wenigste:** `ZAST` mit **1**.
- **Der Index trägt nicht überall eigene Information.** Bei `IBIS`, `IBISGUS`, `NXHBE`, `SUTTONS`,
  `ZAST` und dem größten Teil von `VOTG` ist er gleich `MessageBAMType`. Bei `NEXANS` weicht er in
  **18 von 40** Zeilen ab und ordnet erkennbar nach Gruppen (`_L_SAP`, `_K_SAP`, `_FORS`) statt nach
  Typnummer.
- **Ein Mandant hat einen doppelten Sortierindex:** `VOTG` vergibt `2002` zweimal (Typ `2002`
  „InvoiceNumber VTG" und Typ `2011` „InvoiceNumber Vendor"). Bei zwölf Typen gibt es nur elf
  verschiedene Indizes.
- **Drei von zehn Mandanten haben überhaupt keine BAM-Konfiguration:** `EDITIONLINGERI`, `SYSTEM`
  und `WOC` kommen in `MessageBAMMandant` nicht vor. `WOC` und `SYSTEM` haben Nachrichten
  (2.529 bzw. 151).

---

## M8 — Einheit von `MessageTimeout`

Nachgetragen am 01.08.2026. M2 hatte gezeigt: Es gibt genau zwei Werte, `1800` und `0`, niemals
`NULL`. `1800` Minuten wären 30 Stunden, `1800` Sekunden 30 Minuten.
[`datenmodell.md`](datenmodell.md) und Regel Z2 sagen Minuten.

### Statement (a) — die vorgesehene Messung

```sql
SELECT m.MessageID, m.MessageTimeout, m.MessageStatus,
       MIN(a.MessageActionStart) AS start,
       m.MessageLastUpdate       AS ende,
       TIMESTAMPDIFF(MINUTE, MIN(a.MessageActionStart), m.MessageLastUpdate) AS dauer_minuten
FROM Message m
JOIN MessageAction a ON a.MessageID = m.MessageID
WHERE m.MessageStatus = 'ERROR_TIMEOUT'
GROUP BY m.MessageID, m.MessageTimeout, m.MessageStatus, m.MessageLastUpdate
ORDER BY dauer_minuten;
```

Vorab geprüft: `MessageAction.MessageActionStart` existiert (`timestamp`, `NULL`-fähig), und **alle
52** `ERROR_TIMEOUT`-Nachrichten haben Zeilen in `MessageAction` — der Join verliert keine.

**Ergebnis, ohne `MessageID`** (52 Zeilen, alle mit `MessageTimeout = 1800`):

| Kennzahl | Sekunden | Minuten |
|---|---|---|
| Minimum | 119 | 1,98 |
| Median (Mittel aus 26. und 27. Wert) | 210 | 3,50 |
| Maximum | 335 | 5,58 |
| arithmetisches Mittel | ~176 | 2,9 |

Verteilung: 4 Zeilen bei 119 s, dann 44 Zeilen dicht zwischen 185 s und 237 s, dann je eine bei
264 s, 306 s und 335 s.

**Laufzeit:** 4,4 ms (Statement a), 3,7 ms (Auswertung ohne `MessageID`).

### Die vorgesehene Messung entscheidet die Frage nicht

Die Vorgabe lautete: rund 30 Minuten heißt Sekunden, rund 30 Stunden heißt Minuten. Gemessen sind
**3,5 Minuten** — keines von beidem. Die Entscheidungsregel greift ins Leere.

Der Grund ist auffindbar. Zusätzlich gemessen:

```sql
SELECT TIMESTAMPDIFF(SECOND, MAX(a.MessageActionStart), m.MessageLastUpdate) AS letzter_start_bis_ende
FROM Message m JOIN MessageAction a ON a.MessageID = m.MessageID
WHERE m.MessageStatus = 'ERROR_TIMEOUT' GROUP BY m.MessageID, m.MessageLastUpdate;
```

| | Sekunden |
|---|---|
| Minimum vom Start der **letzten** Aktion bis zum Fehler | 0 |
| Maximum | **120** |

Laufzeit 5,9 ms. Zwischen dem Start der letzten Aktion und dem Fehler liegen **höchstens
120 Sekunden** — eine glatte Zwei-Minuten-Grenze. Dazu passt, dass jede der 52 Nachrichten genau
eine `MessageAction` mit `SOSActionTimeout = 0` trägt (52 Zeilen) neben 149 Zeilen mit `1800`.

> **`ERROR_TIMEOUT` ist nicht das Ablaufen von `MessageTimeout`.** Der Status entsteht offenbar aus
> einer Zeitüberschreitung auf Dienstebene mit einer eigenen, kürzeren Frist. Die Messung zielte
> damit auf die falsche Grundgesamtheit — nicht auf die Nachrichten, an denen `MessageTimeout`
> tatsächlich abläuft.

### Die Gegenprobe, die entscheidet

`datenmodell.md` nennt das Muster `WAIT|30M` in `SOSActionServiceProperties`. Steht daneben ein
Zahlenwert für dieselbe Frist, ist die Einheit ablesbar:

```sql
SELECT a.SOSActionTimeout,
       SUBSTRING_INDEX(SUBSTRING_INDEX(a.SOSActionServiceProperties, 'WAIT|', -1), '|', 1) AS wait_marke,
       COUNT(*) AS zeilen
FROM MessageAction a
WHERE a.SOSActionServiceProperties LIKE '%WAIT|%'
  AND a.MessageActionStart >= '2025-12-01'
GROUP BY a.SOSActionTimeout, wait_marke ORDER BY zeilen DESC LIMIT 20;
```

| `SOSActionTimeout` | Marke im Ablauf | Zeilen |
|---|---|---|
| `1800` | `30M` | **37.120** |
| `1800` | `15M` | 909 |

**Laufzeit: 97,976 s** (ein Lauf) — ein `LIKE '%…%'` über `mediumtext` in `MessageAction` (10,2 Mio.
Zeilen, 3,0 GB), auch mit Zeitfenster. Das Statement ist eine einmalige Erhebung und **kein Muster
für Anwendungscode** (Regel L4 gilt sinngemäß).

Dazu die Stammdaten, wo Varianz zu erwarten wäre:

```sql
SELECT SOSActionTimeout,
       CASE WHEN SOSActionServiceProperties LIKE '%WAIT|%'
            THEN SUBSTRING_INDEX(SUBSTRING_INDEX(SOSActionServiceProperties,'WAIT|',-1),'|',1)
            ELSE '(kein WAIT)' END AS wait_marke,
       COUNT(*) AS zeilen
FROM SOSAction GROUP BY SOSActionTimeout, wait_marke ORDER BY SOSActionTimeout, zeilen DESC;
```

| `SOSActionTimeout` | Marke | Zeilen |
|---|---|---|
| `NULL` | (kein WAIT) | 11 |
| `300` | (kein WAIT) | 2 |
| `1800` | (kein WAIT) | 3.743 |
| `1800` | `30M` | 186 |
| `1800` | `5M` | 1 |
| `1800` | `15M` | 1 |

Laufzeit 70,9 ms. `SOSActionTimeout` ist also ein systemweiter Vorgabewert `1800` mit zwei
Ausreißern `300`.

### Schlussfolgerung — ausdrücklich

> **`1800` ist eine Sekundenangabe. Der Timeout beträgt 30 Minuten.
> `datenmodell.md` und Regel Z2 sind falsch.**

Die Belegkette:

1. `1800` steht 37.120-mal neben dem Ablaufschritt `30M`. **1800 Sekunden sind exakt 30 Minuten.**
   Unter der Minuten-Lesart stünde eine Frist von 30 **Stunden** neben einem Schritt, der 30
   **Minuten** wartet — Faktor 60 Luft, womit die Frist nichts mehr begrenzte.
2. Der einzige andere vorkommende Wert, `300`, ergibt in Sekunden 5 Minuten und passt zum ebenfalls
   vorhandenen Schritt `5M`. In Minuten wären es 5 Stunden.
3. Eine Vorgabefrist von 30 Minuten je Ablaufschritt ist für EDI-Übertragungen, die in Minuten
   laufen (M8 a: 2 bis 5,6 Minuten), die plausible Größenordnung; 30 Stunden sind es nicht.

**Die Schwachstelle, die dazugehört:** Gemessen ist `MessageAction.SOSActionTimeout` beziehungsweise
`SOSAction.SOSActionTimeout` — **nicht** `Message.MessageTimeout` selbst. Übertragen wird die
Einheit, weil beide Spalten denselben Typ (`smallint(6)`), denselben Wertevorrat (`1800`, `0`) und
dieselbe Namensendung tragen. Ein direkter Beleg an `Message.MessageTimeout` fehlt und ist auf der
Testkopie auch nicht zu bekommen, weil dort keine Nachricht existiert, an der diese Frist sichtbar
abläuft (`RUNNING` kommt null Mal vor).

**Entschieden am 01.08.2026 nach Rückfrage.** Die Einheit steht im Code an **genau einer Stelle** als
benannte Konstante, damit ein Gegenbeleg aus der Produktion eine Zeile kostet und keine Suche.

### Was das ändert

| Datei | Vorher | Jetzt |
|---|---|---|
| `datenmodell.md` §3, §5.3 | „Dauer in Minuten" | Dauer in **Sekunden**, mit Datum und Beleg |
| `DEVELOPMENT_GUIDELINES.md` Z2 | „Dauer in Minuten" | Dauer in **Sekunden** |
| `DEVELOPMENT_GUIDELINES.md` §5.3 | Feldname `timeoutMinuten` | `timeoutSekunden` |
| `PROJEKTBESCHREIBUNG.md` §3.3 | „Dauer in Minuten" | Dauer in **Sekunden** |

---

## M9 — Anker für die Dev-Uhr

Nachgetragen am 01.08.2026. Gesucht ist der jüngste Tag, an dem **mindestens drei Mandanten**
Nachrichten haben — als Bezugspunkt, der einen brauchbaren Ausschnitt liefert statt der 285 Zeilen
eines einzigen Mandanten am Maximum (Auffälligkeit A).

### Statement

```sql
SELECT DATE(m.MessageLastUpdate) AS tag,
       COUNT(*) AS zeilen,
       COUNT(DISTINCT pm.MandantID) AS mandanten,
       MAX(m.MessageLastUpdate) AS letzter_zeitpunkt
FROM Message m
JOIN Process p         ON p.ProcessID  = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY tag
HAVING mandanten >= 3
ORDER BY tag DESC
LIMIT 5;
```

### EXPLAIN

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `m` | `index` | `MessageLastUpdateProcessMessageIDX` | 298 | NULL | 3.560.486 | `Using where; Using index; Using filesort` |
| 1 | SIMPLE | `p` | `eq_ref` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | SIMPLE | `pm` | `ref` | `PRIMARY` | 146 | `p.ProjectID` | 1 | `Using index` |

### Ergebnis

| Tag | Zeilen | Mandanten | letzter Zeitpunkt |
|---|---|---|---|
| **2025-12-30** | 776 | **3** | **`2025-12-30 04:09:47`** |
| 2025-12-29 | 6.249 | 6 | `2025-12-29 23:53:50` |
| 2025-12-28 | 6.083 | 5 | `2025-12-28 23:53:15` |
| 2025-12-27 | 1.988 | 6 | `2025-12-27 23:53:41` |
| 2025-12-26 | 2.619 | 5 | `2025-12-26 23:56:06` |

**Laufzeit:** Aufwärmlauf 7,047 s, beste von drei **6,923 s** (6,948 · 6,968 · 6,923 s).

Die Erwartung aus den bisherigen Messungen ist bestätigt: Der jüngste Treffer liegt auf dem
**2025-12-30**. Kein Tag aus dem Ausläufer 2026 erfüllt die Bedingung — dort hat nur `NEXANS` Daten.

### Was der neue Anker im 24-Stunden-Fenster liefert

```sql
SELECT pm.MandantID, COUNT(*) AS zeilen
FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageLastUpdate >  TIMESTAMP('2025-12-30 04:09:47') - INTERVAL 1 DAY
  AND m.MessageLastUpdate <= TIMESTAMP('2025-12-30 04:09:47')
GROUP BY pm.MandantID ORDER BY zeilen DESC;
```

| `MandantID` | Zeilen |
|---|---|
| `NEXANS` | 5.177 |
| `SUTTONS` | 684 |
| `IBIS` | 233 |
| `VOTG` | 206 |
| `IBISGUS` | 81 |
| `WOC` | 1 |
| **gesamt** | **6.382** |

Laufzeiten 51,2 ms und 7,1 ms.

### Gegenüberstellung der beiden Anker

| | Anker `MAX(MessageLastUpdate)` | Anker M9 |
|---|---|---|
| Zeitpunkt | `2026-07-08 17:21:10` | `2025-12-30 04:09:47` |
| Zeilen im 24-h-Fenster | 285 | **6.382** |
| Mandanten mit Daten | 1 (`NEXANS`) | **6** |
| `VOTG` sichtbar (Isolationstest, Mandant A) | nein | **ja**, 206 Zeilen |
| `SUTTONS` sichtbar (Isolationstest, Mandant B) | nein | **ja**, 684 Zeilen |

**Schlussfolgerung, ausdrücklich:** Der Anker aus M9 ist der brauchbare. Mit ihm zeigt das
Standard-Zeitfenster von 24 Stunden für **beide** Testmandanten aus
[`mandantentrennung.md`](mandantentrennung.md) §5 echte Daten; mit dem alten Anker sieht jeder
Mandant außer `NEXANS` leer aus — und genau das soll Regel Z1 verhindern.

---

## L1 bis L10 — die Statements der Nachrichtenliste

Nachgetragen am **06.08.2026**, nach dem Bau des Listen-Endpunkts (Regel L7). Gemessen wurden die
**tatsächlich von jOOQ erzeugten** Statements: Sie wurden aus einem Integrationstest heraus mit
eingesetzten Werten protokolliert und unverändert gegen die Testkopie gestellt — kein von Hand
nachgebautes SQL.

> **Die Fenster sind absolut, nicht relativ.** Ein 24-Stunden-Fenster relativ zum Maximum enthält
> 285 Zeilen, und jede Messung darin sagt „schnell", unabhängig von der Abfrage. Der 29.12.2025 hat
> 6.249 Zeilen und liegt damit nahe an einem Produktionstag — im dichten Teil sind es rund 7.300 am
> Tag. Gemessen wurde `2025-12-29 00:00:00` bis `2025-12-30 00:00:00` (24 h), ab `2025-11-30`
> (30 Tage) und ab `2024-12-30` (ein Jahr).
>
> **Die Sitzung hatte kein `max_statement_time`.** Der Lese-Pool der Anwendung setzt 10 Sekunden
> ([`datenzugriff.md`](datenzugriff.md) §1); hier sollte die wahre Laufzeit sichtbar werden statt
> eines Abbruchs. Kein Statement kam in die Nähe — das langsamste liegt bei 1,3 Sekunden.
>
> Jede Angabe ist die **beste von fünf** Läufen nach einem Aufwärmlauf, serverseitig über
> `SET profiling = 1`. `rows` ist die Schätzung aus `information_schema` und liegt 6,5 Prozent zu
> hoch (Auffälligkeit G); `r_rows` ist der tatsächlich gelesene Wert aus `ANALYZE`.

### Ergebnis

| # | Abfrage | gewählter Index | `key_len` | `rows` | `r_rows` | Laufzeit |
|---|---|---|---|---|---|---|
| **L1** | NEXANS, 24 h, ohne Filter | `MessageLastUpdateIDX` | 5 | 11.814 | **194** | **2,715 ms** |
| **L2** | NEXANS, 30 Tage, ohne Filter | `MessageLastUpdateIDX` | 5 | 409.758 | **194** | **2,709 ms** |
| **L3** | NEXANS, ein Jahr, ohne Filter | `MessageLastUpdateIDX` | 5 | 1.780.243 | **194** | **2,692 ms** |
| **L4** | SUTTONS, 24 h, ohne Filter | `MessageLastUpdateIDX` | 5 | 11.814 | **2.890** | **33,978 ms** |
| **L5** | NEXANS, ein Jahr, Status FEHLER | **`MessageStatusIDX`** | 123 | 6.257 | 124 | **24,803 ms** |
| **L6** | SUTTONS, ein Jahr, Status FEHLER | **`MessageStatusIDX`** | 123 | 6.257 | 106 | **25,162 ms** |
| **L7a** | Vorfilter Prozess- und Projektname | `ProjectMandant_Mandant_idx` | 146 | 17 | 17 | **2,975 ms** |
| **L7b** | Vorfilter Ablaufname | `ProjectMandant_Mandant_idx` | 146 | 17 | 17 | **5,806 ms** |
| **L7c** | NEXANS, 30 Tage, Freitext aufgelöst | `MessageLastUpdateIDX` | 5 | 409.758 | **214.330** | **1.295,872 ms** |
| **L8** | NEXANS, 24 h, zweite Seite über Cursor | `MessageLastUpdateIDX` | **151** | 11.417 | **52** | **1,795 ms** |
| **L9** | BAM-Nachladung, 50 `MessageID` | `PRIMARY` (`Using index`) | 148 | 100 | — | **2,982 ms** |
| **L10** | BAM-Nachladung, 200 `MessageID` | `PRIMARY` (`Using index`) | 148 | 400 | — | **3,440 ms** |

Aufwärmläufe: 3,360 · 2,818 · 2,708 · 34,755 · 25,665 · 25,514 · 2,945 · 6,435 · 1.350,317 · 1,804 ·
3,078 · 3,442 ms.

### Was die Zahlen zeigen

**Die Fenstergröße kostet auf der ersten Seite nichts** (L1 = L2 = L3, alle bei 2,7 ms). Das ist der
überraschendste Befund. Die Abfrage sortiert absteigend und bricht nach 51 Zeilen ab; MariaDB läuft
den Index vom oberen Rand des Fensters rückwärts und liest 194 Zeilen — unabhängig davon, ob unter
ihnen noch 11.000 oder 1,8 Millionen weitere lägen. Die `rows`-Schätzung wächst um Faktor 150, die
tatsächliche Arbeit nicht. **Regel L1 rechtfertigt sich damit nicht über die erste Seite**, sondern
über alles, was danach kommt: über die letzte Seite eines Jahresfensters, über jeden Filter, der
nicht auf dem Zeitindex liegt, und über den Schutz davor, dass jemand den Zeitraum ganz weglässt.

**Der Mandant entscheidet mehr als das Fenster** (L1 gegen L4: 2,7 ms gegen 34,0 ms bei identischem
Fenster). `NEXANS` stellt 86 Prozent aller Zeilen; für 51 Zeilen von `SUTTONS` muss MariaDB
**2.890** Indexeinträge durchlaufen, weil dazwischen lauter fremde liegen. Ein kleiner Mandant zahlt
also für die Größe der großen. Bei `SUTTONS` (5,9 Prozent) sind das 34 ms; bei einem Mandanten mit
einem Promille wären es entsprechend mehr. Das ist die Kennzahl, die man im Auge behalten muss —
nicht die Gesamtzeilenzahl.

**Die Vermutung zum Statusfilter ist bestätigt.** Sobald `status=FEHLER` gesetzt ist, wechselt der
Treiber auf `MessageStatusIDX` (`key_len = 123`, `Using index condition; Using where; Using
filesort`). Der Zeitfilter wird dann nachgelagert geprüft, und weil die `LIKE`-Fassung der
Fehlerbedingung zwei Indexbereiche ergibt, bleibt die Kandidatenmenge klein: rund 3.400 Zeilen über
den Gesamtbestand. Das `filesort` ist dabei kein Befund — es sortiert diese kleine Menge, nicht das
Fenster.

**L6 ist nicht der pathologische Fall, für den er gehalten wurde.** Die Erwartung war, dass ein
seltener Status bei einem kleinen Mandanten über ein langes Fenster den Zeitindex als Treiber wählt
und den ganzen Jahresbereich ergebnislos durchläuft. Das tritt **nicht** ein: Der Statusfilter zieht
den Treiber auf `MessageStatusIDX`, und L6 kostet mit 25,2 ms genauso viel wie L5 mit 24,8 ms. Der
Mandant spielt hier keine Rolle mehr, weil die Kandidatenmenge schon vor dem Mandantenfilter klein
ist.

**Der pathologische Fall ist L7c — der Freitextfilter.** 1,3 Sekunden, `r_rows = 214.330`,
`r_filtered = 0,01 %`: MariaDB behält das Zeitfenster als Treiber und wirft 99,99 Prozent der
gelesenen Zeilen wieder weg. Die Ursache ist die **ODER-Verknüpfung über zwei Spalten**
(`ProcessID IN (…) OR SOSID IN (…)`) — sie schließt den Index auf `ProcessID` aus. Nachgemessen:

| Variante | Treiber | `r_rows` | Laufzeit |
|---|---|---|---|
| wie gebaut (`ProcessID` **oder** `SOSID`) | `MessageLastUpdateIDX` | 214.330 | **1.295,872 ms** |
| nur `ProcessID IN (…)`, ohne das ODER | **`ProejctIDIDX`** | **17** | **12,334 ms** |
| wie gebaut, aber 24-h-Fenster | `MessageLastUpdateIDX` | 6.249 | 40,393 ms |

Ohne das ODER wählt der Optimierer den Prozess-Index und ist **hundertmal schneller**. Die Kosten
wachsen linear mit dem Fenster (24 h: 40 ms, 30 Tage: 1,3 s) — ein Jahresfenster mit Suchbegriff
läge damit in der Größenordnung von zehn Sekunden und damit an der Grenze von `max_statement_time`.
**Das ist ein Befund, keine Behebung:** Der Umbau (zwei getrennte Abfragen mit `UNION` statt einer
ODER-Bedingung) ändert die Form des Statements und braucht seine eigene Messung. Er steht als
offener Punkt in [`nachrichtenliste.md`](nachrichtenliste.md).

**Die Cursor-Bedingung nutzt beide Indexspalten** (L8, `key_len = 151`). `MessageLastUpdateIDX` ist
laut Schema nur auf `MessageLastUpdate` definiert; InnoDB hängt an jeden Sekundärindex den
Primärschlüssel, und MariaDB nutzt das (`optimizer_switch: extended_keys=on`). Der Index ist damit
faktisch `(MessageLastUpdate, MessageID)` — genau der Sortierschlüssel der Liste. **Kein
`filesort`**, obwohl nach zwei Spalten sortiert wird. Die Gegenüberstellung mit dem Tupelvergleich:

| | ODER-Form | Tupelvergleich |
|---|---|---|
| `key_len` | **151** (beide Spalten) | 5 (nur der Zeitstempel) |
| `r_rows` für 51 gelieferte Zeilen | **52** | 245 |
| Laufzeit, beste von fünf | **1,795 ms** | 2,551 ms |

Eine erste Messung derselben Abfrage **ohne** den Zwischenschritt-Ausschluss ergab dasselbe Bild
(72 gegen 155 Zeilen, 1,842 gegen 1,990 ms). Der Tupelvergleich wird von MariaDB 10.6 nicht in einen
Indexbereich übersetzt, sondern als Filter ausgewertet.

**Die zweite Seite ist billiger als die erste** (1,795 ms gegen 2,715 ms). Der Cursor verengt den
Indexbereich; ohne ihn muss MariaDB vom Fensterrand aus lesen. Genau das ist der Unterschied zu
`OFFSET`, der mit jeder Seite teurer würde.

**Die BAM-Nachladung skaliert flach** (L9 gegen L10: 2,982 ms gegen 3,440 ms bei vierfacher
Kennungszahl). Der Zugriff läuft über das Präfix des Primärschlüssels und ist `Using index` — die
Werte stehen im Index selbst, es wird keine Zeile nachgeschlagen. Vier mal so viele Kennungen kosten
15 Prozent mehr Zeit.

**Die Vorfilterung des Suchbegriffs ist billig** (L7a 3,0 ms, L7b 5,8 ms). Beide steigen über
`ProjectMandant_Mandant_idx` ein — der Mandant ist der selektivste Teil der Bedingung — und laufen
dann über 17 Projekte, 43 Prozesse je Projekt und deren Abläufe. Der fehlende Index auf `SOSName`
kostet nichts: `SOS` hat 1.818 Zeilen.

### Statements

Die Statements stehen nicht vollständig hier — sie sind zwischen 400 und 8.300 Zeichen lang, weil
jOOQ jede Spalte voll qualifiziert und die Kennungslisten einsetzt. Die Form ist in
[`nachrichtenliste.md`](nachrichtenliste.md) beschrieben; erzeugt werden sie von
`message/NachrichtenRepository`. Die Kennungen der BAM-Messungen stammen aus einer echten Seite
(200 `MessageID` aus dem 24-Stunden-Fenster von NEXANS) und sind hier bewusst nicht abgedruckt.

---

## M10 und L11 — der Freitextfilter, zweiter Anlauf

Erhoben am **06.08.2026** (Aufgabe 10). Ziel war, den teuersten Fall dieses Endpunkts zu entschärfen
— L7c, 1,3 Sekunden über 30 Tage. Geprüft wurden **zwei** Wege. **Beide tragen nicht.** Das Ergebnis
steht hier vollständig, auch weil es das Gegenteil der Erwartung ist.

### M10 — trägt die Auflösung über `SOS.ProcessID`?

Der geplante Umbau wollte die Treffer aus `SOSName` über die Spalte `SOS.ProcessID` zu `ProcessID`s
auflösen und mit den Treffern aus `ProcessName`/`ProjectName` zu **einer** Kennungsliste vereinigen.
Im Hauptstatement bliebe dann eine einzige Bedingung `m.ProcessID IN (…)` und damit ein nutzbarer
Index. Die Vorbedingung dafür ist, dass der Weg über `SOS` zur selben `ProcessID` führt wie die
Nachricht selbst:

```sql
SELECT COUNT(*) AS abweichend
FROM Message m JOIN SOS s ON s.SOSID = m.SOSID
WHERE s.ProcessID IS NULL OR s.ProcessID <> m.ProcessID;
```

**EXPLAIN**

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `m` | `ALL` | NULL | NULL | NULL | 3.560.486 | `Using where` |
| 1 | SIMPLE | `s` | `eq_ref` | `PRIMARY` | 146 | `m.SOSID` | 1 | `Using where` |

**Ergebnis: `9101` — nicht 0.**

| Kennzahl | Wert |
|---|---|
| abweichende Zeilen | **9.101** von 3.341.519 = **0,27 %** |
| davon `Message.ProcessID IS NULL` | 0 |
| davon `SOS.ProcessID IS NULL` | 0 (alle 1.818 `SOS`-Zeilen tragen eine `ProcessID`) |
| betroffene `SOS`-Zeilen | **11** |
| verschiedene `SOS.ProcessID` | 11 |
| verschiedene `Message.ProcessID` | 14 |
| Zeitraum | `2024-10-01 06:50:31` bis `2025-12-29 13:40:14` — über den ganzen Bestand verteilt |

Laufzeit 10,877 s (ein Lauf, voller Durchlauf über `Message`).

Verteilung über die Mandanten (29,390 s, ein Lauf):

| `MandantID` | abweichende Zeilen | betroffene `SOS` |
|---|---|---|
| `NEXANS` | 9.038 | 10 |
| `IBIS` | 63 | 1 |

**Schlussfolgerung, ausdrücklich: Der Umbau findet nicht statt.** Er ist nicht bloß unscharf, er
**verliert Zeilen**: Für diese 9.101 Nachrichten zeigt der Ablauf auf einen anderen Prozess als die
Nachricht selbst. Ein Suchtreffer im Ablaufnamen fände sie über die aufgelöste `ProcessID` nicht
mehr — die Suche würde also stillschweigend weniger finden als heute. Elf betroffene `SOS`-Zeilen
über fünfzehn Monate sind auch kein Ausreißer eines Tages, sondern ein struktureller Zustand des
Quellsystems.

Die Unschärfe, die derselbe Umbau zusätzlich mitgebracht hätte (ein Treffer im Ablaufnamen liefert
alle Nachrichten seines Prozesses, also auch die Geschwister der `_OUT`- und `_MAIL`-Varianten),
wäre für sich vertretbar gewesen — verlorene Zeilen sind es nicht.

### L11 — trägt die Alternative mit `UNION`?

`nachrichtenliste.md` §9 führte den `UNION` bisher als die naheliegende Abhilfe: zwei Zweige,
`ProcessID IN (…)` und `SOSID IN (…)`, „der Optimierer wählt dann je Zweig einen Index". Diese
Annahme ist jetzt **gemessen statt vermutet** — und sie ist **falsch**.

Gemessen wurde dasselbe Fenster wie in L1 bis L10 (`2025-11-30` bis `2025-12-30`, 30 Tage), Mandant
`NEXANS`, Seitengröße 51, Zwischenschritte ausgeschlossen. Weil L7c seinen Suchbegriff nicht
festgehalten hat und das Verhalten stark am Begriff hängt, wurden **zwei** Begriffe gemessen: einer,
dessen Prozesse volumenstark sind, und einer, dessen Prozesse im Fenster kaum Zeilen tragen.

| Begriff | aufgelöst zu | OR-Form (wie gebaut) | `UNION`-Alternative |
|---|---|---|---|
| volumenstark | 14 Prozesse, 22 Abläufe | **2,290 ms** | **7.206,8 ms** |
| selektiv | 22 Prozesse, 31 Abläufe | **499,1 ms** | **985,4 ms** |

Aufwärmläufe: 2,465 · 7.384,2 ms bzw. 519,2 · 1.081,3 ms. Beste von fünf, serverseitig über
`SET profiling = 1`.

**Die `UNION`-Form ist in beiden Fällen langsamer — einmal um Faktor zwei, einmal um Faktor 3.100.**

**Die Ursache steht in M1 und ist bisher niemandem aufgefallen: `Message.SOSID` hat keinen Index.**
Die Tabelle trägt `PRIMARY`, `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX`,
`MessageStatusIDX`, `Message_ProcessFK`, `ProejctIDIDX`, `SourceMessageIDIDX` und
`TargetMessageIDIDX` — auf `SOSID` keinen. Der `SOSID`-Zweig **kann** deshalb gar keinen eigenen
Index wählen; er steigt zwangsläufig wieder über `MessageLastUpdateIDX` ein und liest das ganze
Fenster. `ANALYZE` für den selektiven Begriff:

| Form | Zweig | gewählter Index | `r_rows` |
|---|---|---|---|
| OR-Form | — | `MessageLastUpdateIDX` | 78.318 |
| `UNION` | `ProcessID IN (…)` | `MessageLastUpdateIDX` | 78.318 |
| `UNION` | `SOSID IN (…)` | `MessageLastUpdateIDX` | 78.318 |

Der `UNION` **teilt die Arbeit nicht auf, er verdoppelt sie**: zweimal dasselbe Fenster statt einmal,
plus Zusammenführung und `filesort` über das Ergebnis.

**Warum die Gegenmessung aus L7c das nicht gezeigt hat.** Dort wurde `nur ProcessID IN (…), ohne das
ODER` gemessen — 12,334 ms über `ProejctIDIDX`. Das ist **nur der eine Zweig** und damit keine
Alternative, sondern eine andere Abfrage: Sie lässt die Treffer aus `SOSName` einfach weg. Der
zweite Zweig und die Zusammenführung wurden nie gemessen. Nachgemessen für den volumenstarken
Begriff, jetzt vollständig:

| Zweig allein | gewählter Index | Laufzeit |
|---|---|---|
| `ProcessID IN (…)` | `ProejctIDIDX` | **5.865,4 ms** |
| `SOSID IN (…)` | `MessageLastUpdateIDX` | **1.323,6 ms** |

Auch die 12,3 ms aus L7c sind also nicht der Normalfall des Prozess-Zweigs: Über `ProejctIDIDX`
liest MariaDB **alle** Nachrichten der getroffenen Prozesse über den gesamten Bestand und filtert
das Zeitfenster erst danach. Bei den 17 volumenschwachen Prozessen von L7c war das billig, bei 14
volumenstarken kostet es knapp sechs Sekunden.

### Was daraus folgt

- **Der Freitextfilter bleibt, wie er ist.** Von den beiden vorgesehenen Wegen verliert der eine
  Zeilen und der andere Zeit. Ein dritter ist mit den Mitteln dieses Projekts nicht in Sicht: Der
  fehlende Index auf `Message.SOSID` ließe sich nur mit einem `CREATE INDEX` auf `GlassfishDB`
  beheben, und das ist durch Regel S1 ausgeschlossen — dieses Schema gehört uns nicht.
- **Die Kosten sind nicht die aus L7c.** Sie hängen am Suchbegriff und liegen zwischen 2 ms und
  einigen Sekunden. Der Hinweis „wer sucht, sollte das Zeitfenster eng halten" bleibt richtig und
  ist damit die einzige verfügbare Abhilfe.
- **Für die Oberfläche heißt das:** kein Suchfeld ohne Mindestlänge (steht bereits, Regel L5) und
  keine Suche, die bei jedem Tastendruck läuft (entprellt, Aufgabe 14).

---

## L13 — Freitext über lange Fenster

Erhoben am **06.08.2026**, nach M10 und L11. Beide Umbauten des Freitextfilters sind widerlegt; was
bleibt, ist eine Grenze. Diese Messung beantwortet die eine Frage, die vor dem Bau der Grenze offen
war: **Was handelt sich ein Nutzer ein, der sie bewusst aufhebt?**

Aufbau wie L11 — Mandant `NEXANS`, Fenster **absolut** und endend am `2025-12-30 00:00:00` (im
dichten Bestand), Seitengröße 51, Zwischenschritte ausgeschlossen, gemessen mit derselben Form des
Statements, die `message/NachrichtenRepository` erzeugt. Die Sitzung hatte **kein**
`max_statement_time`; sichtbar werden sollte die wahre Laufzeit und nicht ein Abbruch. Beste von
fünf Läufen nach einem Aufwärmlauf, serverseitig über `SET profiling = 1`; bei den Läufen über zehn
Sekunden beste von drei.

**Die beiden Begriffe sind nicht abgedruckt** (Regel G1 — es sind Kundennamen aus dem Bestand).
Sie sind über ihre Auflösung eindeutig beschrieben und wiederauffindbar.

### Der selektive Begriff — 22 Prozesse, 31 Abläufe

Derselbe wie in L11, wiedererkannt an `r_rows = 78.318` und rund 490 ms über 30 Tage.

| # | Fenster | `rows` (Schätzung) | `r_rows` | Laufzeit |
|---|---|---|---|---|
| — | 30 Tage (Kontrolle, = L11) | 409.758 | **78.318** | **488,4 ms** |
| **L13a** | 90 Tage | 1.476.168 | **78.318** | **487,9 ms** |
| **L13b** | 180 Tage | 1.780.243 | **78.318** | **495,1 ms** |
| **L13c** | ein Jahr | 1.780.243 | **78.318** | **478,2 ms** |

**EXPLAIN** ist in allen vier Fällen identisch — nur die Schätzung wächst:

| id | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | s. o. | `Using where` |
| 1 | `p` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | `pr` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | `mp` | `eq_ref` | `PRIMARY` | 146 | 1 | `Using where` |
| 1 | `pm` | `eq_ref` | `PRIMARY` | 292 | 1 | `Using where; Using index` |

**ANALYZE** ebenso: `r_rows = 78.318` auf `m`, `r_filtered = 0,07 %`, alle Folgetabellen `1,00`.

> **Das Fenster kostet beim selektiven Begriff gar nichts.** Alle vier Läufe lesen exakt dieselben
> 78.318 Zeilen und brauchen dieselbe halbe Sekunde. MariaDB läuft vom **oberen** Fensterrand
> rückwärts und bricht ab, sobald 51 Treffer beisammen sind; die Untergrenze des Fensters wird nie
> erreicht. Das ist dasselbe Muster wie L1 = L2 = L3, nur auf einem hundertfach höheren Niveau.
>
> **Damit ist die Vermutung aus L7c korrigiert.** Dort stand „die Kosten wachsen linear mit dem
> Fenster (24 h: 40 ms, 30 Tage: 1,3 s)". Das stimmt — aber nicht wegen der Fenstergröße, sondern
> weil der dort gemessene Begriff die Seite **nie gefüllt** hat: `r_rows` war mit 214.330 genau die
> Zeilenzahl des ganzen 30-Tage-Fensters. Es wächst nicht die Fenstergröße mit den Kosten, sondern
> die Zahl der Zeilen, die vor dem 51. Treffer liegen.

### Der schlimmste Fall — ein Begriff ohne eine einzige Zeile im Fenster

Ein Begriff, der drei Prozesse und drei Abläufe in den **Stammdaten** trifft, deren Nachrichten im
Fenster aber alle fehlen. Die Vorfilterung ist damit nicht leer — die Abkürzung „kein Treffer, also
leere Liste ohne `Message` anzufassen" greift nicht —, und die Seite füllt sich nie. Das ist der
volle Durchlauf mit leerem Ergebnis.

| Fenster | Zeilen im Fenster | `r_rows` | `r_filtered` | Laufzeit | N |
|---|---|---|---|---|---|
| 30 Tage | 214.330 | **214.330** | 0,00 % | **1.246 ms** | 5 |
| 90 Tage | 680.872 | **680.872** | 0,00 % | **3.937 ms** | 5 |
| 180 Tage | 1.385.909 | **1.385.909** | 0,00 % | **8.005 ms** | 3 |
| **ein Jahr** | 2.713.376 | **2.713.376** | 0,00 % | **15.636 ms** | 3 |

`EXPLAIN` unverändert (`range` über `MessageLastUpdateIDX`, `key_len = 5`); in `ANALYZE` stehen die
Folgetabellen auf `r_rows = NULL`, weil keine Kandidatenzeile je bis zum Join kommt.

### Die Entscheidung, ausdrücklich

> **Ein Jahr reißt `max_statement_time`.** Der Lese-Pool bricht bei 10 Sekunden ab
> ([`datenzugriff.md`](datenzugriff.md) §1); der schlimmste Fall braucht **15,6**. Die Aufhebung
> der Grenze wird deshalb **nicht** bis zum Maximum aus Regel L1 gewährt.
>
> **Gewählt sind 90 Tage.** Es ist die größte gemessene Spanne mit Sicherheitsabstand: 3,9 s sind
> 39 Prozent der Zeitgrenze, also Faktor 2,5 Luft. 180 Tage liegen mit 8,0 s bei 80 Prozent — das
> ist kein Abstand, sondern ein dichterer Tag.

Der Verlauf ist über alle vier Messungen streng linear bei rund **173.000 Zeilen je Sekunde**
(172,0 · 173,0 · 173,1 · 173,5 Tausend/s). Daraus lässt sich der Punkt ablesen, an dem die
Zeitgrenze fällt: 10 Sekunden entsprechen 1,73 Mio. Zeilen und damit **etwa 225 Tagen**. Die 180
Tage sind nicht „knapp darunter", sie sind ein Viertel davor.

Interpoliert wird trotzdem nicht: Gewählt ist eine Spanne, die gemessen wurde.

### Der Abbruch selbst — was der Treiber wirft

Nachgeprüft, weil die Behandlung im Code genau diese eine Ausnahme fangen soll und nicht pauschal
alles. Dasselbe Statement in einer Sitzung mit `SET SESSION max_statement_time = 10`:

```
ERROR 1969 (70100): Query execution was interrupted (max_statement_time exceeded)
```

Über JDBC (Connector/J 3.5.8) und über jOOQ 3.21, je einmal ausgeführt:

| Ebene | Ergebnis |
|---|---|
| MariaDB | Fehler **1969**, SQLState **70100** |
| Treiber | `java.sql.SQLTimeoutException` (Untertyp von `SQLTransientException`) |
| jOOQ | `org.jooq.exception.DataAccessException`, `sqlState() = "70100"`, Ursache die `SQLTimeoutException` |

Ein Spring-`SQLExceptionTranslator` liegt nicht dazwischen: Die beiden `DSLContext`-Beans werden in
`config/JooqConfig` von Hand gebaut, ohne den Übersetzer der Spring-Boot-Autokonfiguration. Was im
Repository ankommt, ist deshalb die `DataAccessException` von jOOQ mit der `SQLTimeoutException` als
Ursache — und genau darauf prüft `NachrichtenRepository.anDerZeitgrenze`.

---

## L12 — die Prozessauswahl

Erhoben am **06.08.2026** (Aufgabe 12) für den neuen Endpunkt `GET /api/prozesse`. Gemessen wurde
die schema-qualifizierte Entsprechung des von jOOQ erzeugten Statements; es enthält keine
Parameterlisten, die Form ist identisch.

```sql
SELECT p.ProcessID, p.ProcessName, pr.ProjectName
FROM Process p
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN Project pr   ON pr.ProjectID = p.ProjectID
WHERE pm.MandantID = ?
ORDER BY pr.ProjectName, p.ProcessName;
```

### EXPLAIN

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `ProjectMandant` | `ref` | `PRIMARY`, `ProjectMandant_Mandant_idx` | **`ProjectMandant_Mandant_idx`** | 146 | `const` | 17 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | SIMPLE | `Process` | `ref` | `Process_ProjectFK` | `Process_ProjectFK` | 147 | `ProjectMandant.ProjectID` | 5 | — |
| 1 | SIMPLE | `Project` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | `ProjectMandant.ProjectID` | 1 | — |

### Ergebnis

| Mandant | gelieferte Zeilen | Laufzeit (beste von fünf) | Aufwärmlauf |
|---|---|---|---|
| `NEXANS` | 733 | **4,225 ms** | 4,705 ms |
| `SUTTONS` | 17 | **0,606 ms** | 0,618 ms |

Einzelwerte `NEXANS`: 4,225 · 4,371 · 4,321 · 4,723 · 4,730 ms. `SUTTONS`: 0,718 · 0,606 · 0,611 ·
1,015 · 1,049 ms.

### Prozesse je Mandant

```sql
SELECT pm.MandantID, COUNT(*) AS prozesse, COUNT(DISTINCT p.ProjectID) AS projekte,
       SUM(pr.ProjectName IS NULL) AS ohne_projektname,
       SUM(p.ProcessName IS NULL)  AS ohne_prozessname
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN Project pr ON pr.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY prozesse DESC;
```

| `MandantID` | Prozesse | Projekte | ohne Projektname | ohne Prozessname |
|---|---|---|---|---|
| `NEXANS` | **733** | 17 | 0 | 0 |
| `VOTG` | 390 | 39 | 0 | 0 |
| `IBIS` | 192 | 46 | 0 | 0 |
| `IBISGUS` | 89 | 19 | 0 | 0 |
| `ZAST` | 35 | 4 | 0 | 0 |
| `SUTTONS` | 17 | 1 | 0 | 0 |
| `NXHBE` | 17 | 2 | 0 | 0 |
| `EDITIONLINGERI` | 9 | 3 | 0 | 0 |
| `WOC` | 4 | 1 | 0 | 0 |
| `SYSTEM` | 4 | 2 | 0 | 0 |

`Process` insgesamt: **1.503** gezählte Zeilen, davon **0** ohne `ProjectID`.

### Was die Zahlen zeigen

- **Der Einstieg läuft über den Mandanten**, nicht über `Process` — `ProjectMandant_Mandant_idx` ist
  derselbe Zugriffspfad wie bei der Vorfilterung des Suchbegriffs (L7a). Der Mandant ist der
  selektivste Teil der Bedingung.
- **`Using temporary; Using filesort` ist hier kein Befund.** Sortiert werden ein paar hundert
  Zeilen, und für `ORDER BY ProjectName, ProcessName` gibt es keinen Index, der das bediente. Bei
  733 Zeilen kostet das vier Millisekunden.
- **Die Laufzeit wächst mit der Prozesszahl des Mandanten, nicht mit dem Gesamtbestand** — 733
  Zeilen kosten das Siebenfache von 17. Das ist das Gegenteil des Musters aus L4, wo ein kleiner
  Mandant für die Größe des großen zahlte: Hier liest jeder nur seinen eigenen Ausschnitt.
- **`information_schema` nennt 1.490 Zeilen, gezählt sind es 1.503** — dieselbe Veralterung der
  Statistiken wie in Auffälligkeit G. Für die Größenordnung ändert das nichts.

---

## M11 — Wie oft ist welcher BAM-Typ überhaupt befüllt?

Erhoben am **06.08.2026** (Nachbesserung zu Schritt 4, Aufgabe 1). **Vorarbeit für Schritt 7**, nicht
für die Liste: Die BAM-Spalten verlassen die Nachrichtenliste ohnehin. Die BAM-Suche muss aber
wissen, welche Felder sie anbieten sollte und welche leer sind.

Anlass ist ein sichtbarer Fehlgriff der Kuratierung. `V4__bam_spalte.sql` setzt für `NEXANS` die
Typen **9006** („Lieferschein-Nr._L_SAP") und **9001** („Abrufnummer_L_SAP"). Auf dem Bild des
Auftraggebers war die erste Spalte auf jeder sichtbaren Zeile leer, und die zweite lieferte
Dutzende laufender Positionsnummern je Nachricht. Diese Messung sagt, wie systematisch das ist.

### Statement

Der Zugriff läuft ausschließlich über **`MessageID`** (Regeln L4/L5) — nie über `MessageBAMValue`.
Die Bezugsmenge wird über das Zeitfenster vorgewählt, `MessageBAM` hängt als `ref` über das Präfix
seines Primärschlüssels daran.

```sql
SELECT bm.MessageBAMType, bt.MessageBAMTypeDescription,
       COALESCE(g.nachrichten, 0) AS nachrichten_mit_wert,
       ROUND(100 * COALESCE(g.nachrichten, 0) / 180251, 2) AS abdeckung_prozent,
       COALESCE(g.werte, 0) AS werte_gesamt,
       ROUND(g.schnitt, 2) AS werte_je_nachricht, g.groesste AS meiste_werte_einer_nachricht
FROM MessageBAMMandant bm
LEFT JOIN MessageBAMType bt ON bt.MessageBAMType = bm.MessageBAMType
LEFT JOIN (
    SELECT je.MessageBAMType, COUNT(*) AS nachrichten, SUM(je.anzahl) AS werte,
           AVG(je.anzahl) AS schnitt, MAX(je.anzahl) AS groesste
    FROM (
        SELECT b.MessageBAMType, b.MessageID, COUNT(*) AS anzahl
        FROM Message m
        JOIN MessageBAM b ON b.MessageID = m.MessageID
        WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
          AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
          AND EXISTS (SELECT 1 FROM Process p
                      JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
                      WHERE p.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
        GROUP BY b.MessageBAMType, b.MessageID
    ) je
    GROUP BY je.MessageBAMType
) g ON g.MessageBAMType = bm.MessageBAMType
WHERE bm.MandantID = 'NEXANS'
ORDER BY nachrichten_mit_wert DESC, bm.MessageBAMType;
```

**Die Bezugsmenge:** `NEXANS` hat im Fenster `2025-11-30` bis `2025-12-30` genau **180.251**
Nachrichten (eigene Abfrage, beste von zwei: **683 ms**). Alle Prozentwerte unten beziehen sich
darauf.

### EXPLAIN (des inneren Teils, ohne die Hüllen)

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `m` | `range` | `MessageLastUpdateProcessMessageIDX` | 5 | NULL | 476.586 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | PRIMARY | `p` | `eq_ref` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | PRIMARY | `pm` | `eq_ref` | `PRIMARY` | 292 | `p.ProjectID`, `const` | 1 | `Using where; Using index` |
| 1 | PRIMARY | `b` | **`ref`** | **`PRIMARY`** | 146 | `m.MessageID` | 8 | **`Using index`** |

`MessageBAM` wird über das Präfix seines Primärschlüssels angefahren und ist `Using index` — es wird
keine Zeile nachgeschlagen. Derselbe Zugriffspfad wie bei der Nachladung der Liste (L9/L10).

### Ergebnis — alle 40 konfigurierten Typen

| Typ | Beschreibung | Nachrichten mit Wert | Abdeckung | Werte gesamt | Ø je Nachricht | meiste einer Nachricht |
|---|---|---|---|---|---|---|
| 9018 | Kundenmaterialnummer_K_SAP | 29.284 | **16,25 %** | 143.213 | 4,89 | 3.035 |
| 9015 | Kundenwerk_K_SAP | 29.268 | **16,24 %** | 29.659 | 1,01 | 8 |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 29.249 | **16,23 %** | 29.292 | 1,00 | 2 |
| 9020 | Lieferschein, Entnahme , PUS_K_SAP | 27.302 | **15,15 %** | 76.512 | 2,80 | 3.773 |
| 9016 | Abladestelle_K_SAP | 26.460 | **14,68 %** | 27.753 | 1,05 | 15 |
| 9019 | Bestellnummer vom Kunden_K_SAP | 15.615 | 8,66 % | 110.707 | 7,09 | 3.035 |
| 9032 | Sendercode_K_SAP | 14.864 | 8,25 % | 14.864 | 1,00 | 1 |
| 9033 | Empfaengercode_K_SAP | 14.864 | 8,25 % | 14.864 | 1,00 | 1 |
| 9017 | (JIT-) Abrufnummer_K_SAP | 13.147 | 7,29 % | 39.412 | 3,00 | 571 |
| 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 12.960 | 7,19 % | 14.496 | 1,12 | 500 |
| 9023 | Übertragungsnummer Gutschrift_K_SAP | 12.960 | 7,19 % | 12.966 | 1,00 | 4 |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 7.929 | 4,40 % | 55.833 | 7,04 | 642 |
| 9004 | Unsere Material-Nr._L_SAP | 7.923 | 4,40 % | 55.697 | 7,03 | 642 |
| 9005 | Werk_L_SAP | 7.801 | 4,33 % | 8.246 | 1,06 | 5 |
| 9000 | Abladestelle_L_SAP | 7.180 | 3,98 % | 7.764 | 1,08 | 6 |
| 9002 | Lieferplannummer_L_SAP | 5.964 | 3,31 % | 41.333 | 6,93 | 749 |
| **9001** | **Abrufnummer_L_SAP** | 5.459 | **3,03 %** | 24.136 | **4,42** | **238** |
| 9021 | Transportnummer_K_SAP | 2.454 | 1,36 % | 2.454 | 1,00 | 1 |
| **9006** | **Lieferschein-Nr._L_SAP** | 1.933 | **1,07 %** | 7.057 | 3,65 | 173 |
| 9007 | Transport-Nummer_L_SAP | 1.490 | 0,83 % | 1.727 | 1,16 | 32 |
| 9039 | Daten-Sender-Nummer_L_SAP | 1.432 | 0,79 % | 1.443 | 1,01 | 2 |
| 9034 | Bestellnummer_L_SAP | 1.251 | 0,69 % | 6.092 | 4,87 | 152 |
| 9037 | Packmittelnummer Kunde_L_SAP | 1.227 | 0,68 % | 1.846 | 1,50 | 12 |
| 9038 | Packmittelnummer Lieferant_L_SAP | 1.156 | 0,64 % | 2.639 | 2,28 | 31 |
| 9036 | Lagerort Kunde_L_SAP | 932 | 0,52 % | 953 | 1,02 | 3 |
| 9009 | Beleg-Nr. GS_L_SAP | 739 | 0,41 % | 739 | 1,00 | 1 |
| 9010 | Materialbeleg (Entnahme)_L_SAP | 739 | 0,41 % | 1.151 | 1,56 | 55 |
| 9024 | Rechnungsnummer_K_SAP | 433 | 0,24 % | 988 | 2,28 | 78 |
| 9012 | Charge_L_SAP | 292 | 0,16 % | 767 | 2,63 | 51 |
| 9008 | Beleg-Nr.  TSL _L_SAP | 234 | 0,13 % | 234 | 1,00 | 1 |
| 9013 | Nr. TSL_L_SAP | 234 | 0,13 % | 234 | 1,00 | 1 |
| 9011 | Anlieferungs-Nr. ae_L_SAP | 58 | 0,03 % | 58 | 1,00 | 1 |
| 9025 | Abladestelle_FORS | 10 | 0,01 % | 20 | 2,00 | 2 |
| 9027 | Bestellnummer_FORS | 10 | 0,01 % | 15.790 | **1.579,00** | 3.035 |
| 9028 | Material-Nr. beim Kunden_FORS | 10 | 0,01 % | 15.790 | **1.579,00** | 3.035 |
| 9029 | Material-Nr.beim Lieferanten_FORS | 10 | 0,01 % | 15.790 | **1.579,00** | 3.035 |
| 9030 | Sender_Ident_FORS | 10 | 0,01 % | 10 | 1,00 | 1 |
| 9031 | Empf_Ident_FORS | 10 | 0,01 % | 10 | 1,00 | 1 |
| **9026** | **LS/RE-Nummer_FORS** | **0** | **0,00 %** | 0 | — | — |
| **9035** | **Werk Kunde_L_SAP** | **0** | **0,00 %** | 0 | — | — |

**Laufzeit:** Aufwärmlauf 12,951 s, beste von drei **12,689 s** (13,147 · 12,689 · 12,697 s). Ohne
die Spalten „Ø" und „meiste" — also ohne die innere Gruppierung je Nachricht — kostet dieselbe
Auswertung **5,294 s**.

### Antworten auf die Fragen der Aufgabenstellung

- **Die fünf Typen mit der höchsten Abdeckung** sind 9018, 9015, 9014, 9020 und 9016 — sämtlich aus
  der Gruppe `_K_SAP`, zwischen 14,68 % und 16,25 %.
- **Zwei Typen sind über den ganzen Monat kein einziges Mal befüllt:** 9026 („LS/RE-Nummer_FORS") und
  9035 („Werk Kunde_L_SAP"). Beide sind konfiguriert, beide tragen nichts.
- **Kein Typ erreicht auch nur ein Fünftel der Nachrichten.** Der beste kommt auf 16,25 %. Eine
  BAM-Spalte, die *immer* etwas zeigt, gibt es bei `NEXANS` nicht — das ist keine Frage der Auswahl,
  sondern der Datenlage.

### Was das für Schritt 7 heißt

Drei Befunde, alle drei ohne Entscheidung — die fällt in Schritt 7:

1. **Die kuratierte Auswahl greift daneben, und die Messung sagt um wie viel.** 9006 ist auf
   **98,93 %** aller Zeilen leer, 9001 auf **96,97 %**. Der Auftraggeber hat also nicht eine
   unglückliche Seite erwischt, sondern den Normalfall gesehen. Die zwei bestbelegten Typen (9018,
   9015) lägen um den Faktor 15 beziehungsweise 5 darüber — auch sie ließen vier von fünf Zeilen
   leer.
2. **Die Zahl der Werte je Nachricht ist der zweite Fallstrick, und er ist größer als der erste.**
   9001 trägt im Schnitt 4,42 Werte, im Höchstfall **238**. Bei 9027, 9028 und 9029 stehen auf zehn
   Nachrichten je 15.790 Werte — **1.579 im Schnitt, bis zu 3.035 auf einer einzigen Nachricht**.
   Ein Suchfeld über solche Typen liefert keine Belegnummer, sondern eine Positionsliste. Die
   Deckelung aus Regel L5 ist damit nicht Vorsicht, sondern Voraussetzung.
3. **Die Gruppen `_L_SAP`, `_K_SAP` und `_FORS` verhalten sich völlig verschieden.** `_K_SAP` trägt
   die Masse, `_L_SAP` liegt eine Größenordnung darunter, `_FORS` ist mit zehn Nachrichten praktisch
   leer. Der Sortierindex, den die Auflösungsregel benutzt, ordnet genau nach diesen Gruppen
   ([`datenzugriff.md`](datenzugriff.md) §5) — und stellt dabei die schwächste Gruppe nach vorn.

---

## M12 — Zwischenschritte je Mandant

Erhoben am **06.08.2026**. Frage: Kommen `SPLITTED` und `MERGED` bei **jedem** Mandanten vor, oder
kündigt der Ausblenden-Schalter der Liste bei manchen eine Ausblendung an, die nichts ausblendet?

### Statement

Wie M3 als Join und nicht als `EXISTS`: In den Daten ist `ProjectMandant` durchgängig 1:1 (M3), der
Join vervielfacht also nichts, und die Summe je Mandant ergibt wieder den Gesamtbestand.

```sql
SELECT pm.MandantID, COUNT(*) AS nachrichten,
       SUM(m.MessageStatus IN ('SPLITTED','MERGED')) AS zwischenschritte,
       SUM(m.MessageStatus = 'SPLITTED') AS splitted,
       SUM(m.MessageStatus = 'MERGED')   AS merged,
       ROUND(100 * SUM(m.MessageStatus IN ('SPLITTED','MERGED')) / COUNT(*), 3) AS anteil_prozent
FROM Message m
JOIN Process p         ON p.ProcessID  = m.ProcessID
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID ORDER BY nachrichten DESC;
```

Variante (b) mit `WHERE m.MessageLastUpdate >= '2025-11-30' AND < '2025-12-30'` — die letzten
30 Tage des dichten Zeitraums, dasselbe Fenster wie M11.

**EXPLAIN (a):** `m` `ALL` (3.560.486), `p` `eq_ref` `PRIMARY`, `pm` `ref` `PRIMARY`,
`Using temporary; Using filesort`.
**EXPLAIN (b):** `m` `range` über `MessageLastUpdateIDX` (`key_len` 5, 409.756 geschätzt), sonst
gleich.

### Ergebnis (a) — Gesamtbestand

| `MandantID` | Nachrichten | Zwischenschritte | `SPLITTED` | `MERGED` | Anteil |
|---|---|---|---|---|---|
| `NEXANS` | 2.885.711 | 1.142.684 | 394.803 | 747.881 | **39,598 %** |
| `SUTTONS` | 197.158 | 6.002 | 6.002 | 0 | **3,044 %** |
| `VOTG` | 145.840 | 40 | 40 | 0 | **0,027 %** |
| `IBIS` | 75.746 | **0** | 0 | 0 | 0 % |
| `IBISGUS` | 29.339 | **0** | 0 | 0 | 0 % |
| `ZAST` | 5.036 | **0** | 0 | 0 | 0 % |
| `WOC` | 2.529 | **0** | 0 | 0 | 0 % |
| `SYSTEM` | 151 | **0** | 0 | 0 | 0 % |
| `NXHBE` | 9 | 4 | 0 | 4 | 44,444 % |

`EDITIONLINGERI` erscheint nicht — der Mandant hat überhaupt keine Nachricht (bekannt aus M3).

**Laufzeit:** Aufwärmlauf 32,729 s, beste von drei **32,677 s** (32,706 · 32,677 · 32,768 s).

### Ergebnis (b) — 2025-11-30 bis 2025-12-30

| `MandantID` | Nachrichten | Zwischenschritte | `SPLITTED` | `MERGED` | Anteil |
|---|---|---|---|---|---|
| `NEXANS` | 180.251 | 66.133 | 27.505 | 38.628 | **36,689 %** |
| `SUTTONS` | 21.516 | 639 | 639 | 0 | **2,970 %** |
| `VOTG` | 6.104 | **0** | 0 | 0 | 0 % |
| `IBIS` | 4.331 | **0** | 0 | 0 | 0 % |
| `IBISGUS` | 1.722 | **0** | 0 | 0 | 0 % |
| `ZAST` | 283 | **0** | 0 | 0 | 0 % |
| `WOC` | 118 | **0** | 0 | 0 | 0 % |
| `SYSTEM` | 5 | **0** | 0 | 0 | 0 % |

`NXHBE` fehlt: Seine neun Nachrichten stammen alle vom `2025-05-19` (M3).

**Laufzeit:** Aufwärmlauf 3,345 s, beste von drei **3,219 s** (3,259 · 3,280 · 3,219 s).

### Was die Zahlen zeigen

**Die 34,38 Prozent aus M6 sind ein Durchschnitt über einen einzigen Mandanten.** `NEXANS` stellt 86
Prozent des Bestands, und dort sind es 39,6 Prozent. Bei allen anderen ist der Anteil eine ganz
andere Größenordnung — oder null.

**Fünf von neun Mandanten mit Nachrichten haben über den gesamten Bestand nicht eine einzige
Zwischenschritt-Zeile:** `IBIS`, `IBISGUS`, `ZAST`, `WOC` und `SYSTEM`. Zusammen sind das 112.801
Nachrichten. Für sie kündigt der Chip der Liste heute etwas an, das es nicht gibt.

**`VOTG` ist der Grenzfall, und er entscheidet die Form des Kriteriums.** 40 Zeilen von 145.840 sind
0,027 Prozent — im dichten Monat null. Ein Kriterium, das *das Zeitfenster* betrachtet, schaltete
den Chip bei `VOTG` je nach gewähltem Zeitraum an und aus; ein Nutzer sähe ein Bedienelement
erscheinen und verschwinden, ohne einen Zusammenhang zu erkennen. **Ein Kriterium über den
Gesamtbestand ist an dieser Stelle das ruhigere** — und es ist zugleich das billigere, weil es zu
den Stammdaten des Mandanten gehört und nicht in die Antwort jeder Seite.

**`SPLITTED` und `MERGED` treten nicht gemeinsam auf.** Außer bei `NEXANS` gibt es entweder nur
`SPLITTED` (`SUTTONS`, `VOTG`) oder nur `MERGED` (`NXHBE`). Für die Oberfläche ändert das nichts —
der Chip spricht von „Zwischenschritten" und nennt keinen Rohwert.

---

## M13 — Trägt `SOSActionID` einen lesbaren Namen?

Erhoben am **06.08.2026**. Von dieser Messung hing ab, ob der aktuelle Schritt bei offenen
Nachrichten überhaupt anzeigbar ist.

### Zuerst: `SOSAction` in `information_schema` — M1 hat diese Tabelle nicht erfasst

```sql
SELECT TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'SOSAction' ORDER BY ORDINAL_POSITION;
```

| # | Spalte | Typ | NULL | Schlüssel |
|---|---|---|---|---|
| 1 | `SOSID` | `varchar(36)` | NO | **PRI** |
| 2 | `SOSActionID` | `smallint(6)` | NO | **PRI** |
| 3 | `SOSActionName` | `varchar(255)` | YES | |
| 4 | `ServiceID` | `varchar(36)` | YES | MUL |
| 5 | `SOSActionServiceProperties` | `mediumtext` | YES | |
| 6 | `SOSActionTimeout` | `smallint(6)` | YES | |

| Index | Spalten (in Reihenfolge) | eindeutig | Kardinalität |
|---|---|---|---|
| `PRIMARY` | **`SOSID`, `SOSActionID`** | ja | 4.232 |
| `SOSAction_ServiceFK` | `ServiceID` | nein | 24 |
| `SOSAction_SOSFK` | `SOSID` | nein | 4.232 |

`SOSAction` hat **3.944** Zeilen (1,5 MB Daten, 0,6 MB Index; `information_schema` schätzt 4.232 —
dieselbe Veralterung wie in Auffälligkeit G). Laufzeiten der drei Metadaten-Abfragen: 2,7 · 0,8 ·
0,7 ms, `COUNT(*)` 2,8 ms.

> ⚠️ **Der Primärschlüssel ist zusammengesetzt.** `Message.SOSActionID` allein identifiziert
> **keine** `SOSAction`-Zeile — der Schritt wird erst zusammen mit `Message.SOSID` eindeutig. Ein
> Join über `SOSActionID` allein wäre kein Zugriffspfad, sondern ein Kreuzprodukt: Die Kennung `2`
> gibt es 694-mal. Jede Auswertung unten joint deshalb über **beide** Spalten.

Ein Ablauf hat ein bis sechs Schritte: 442 Abläufe mit einem, 694 mit zwei, 480 mit drei, 134 mit
vier, 24 mit fünf, 3 mit sechs.

### (1) Auf wie vielen Nachrichten ist `SOSActionID` gesetzt?

```sql
SELECT COALESCE(m.MessageStatus, '<NULL>') AS status, COUNT(*) AS nachrichten,
       SUM(m.SOSActionID IS NOT NULL) AS mit_sosactionid,
       SUM(m.SOSActionID IS NULL)     AS ohne_sosactionid,
       SUM(m.SOSActionID = 0)         AS sosactionid_null_wert,
       MIN(m.SOSActionID) AS kleinste, MAX(m.SOSActionID) AS groesste
FROM Message m GROUP BY COALESCE(m.MessageStatus, '<NULL>') ORDER BY nachrichten DESC;
```

| Rohstatus | Einordnung | Nachrichten | mit `SOSActionID` | ohne | davon `= 0` | kleinste | größte |
|---|---|---|---|---|---|---|---|
| `FINISHED` | `ABGESCHLOSSEN` | 2.030.986 | 2.030.986 | **0** | 519 | 0 | 30 |
| `MERGED` | `ZWISCHENSCHRITT` | 747.885 | 747.885 | **0** | 0 | 1 | 2 |
| `SPLITTED` | `ZWISCHENSCHRITT` | 400.845 | 400.845 | **0** | 0 | 1 | 10 |
| `EERP_RECEIVED` | `QUITTIERT` | 143.871 | 143.871 | **0** | 0 | 1 | 10 |
| `COMMIT_RECEIVED` | `QUITTIERT` | 12.654 | 12.654 | **0** | 0 | 2 | 3 |
| `ERROR_DUPLICATE` | `FEHLER` | 3.248 | 3.248 | **0** | 0 | 1 | 1 |
| `COMMIT_SENT` | `UNGEKLAERT` | 1.051 | 1.051 | **0** | 0 | 2 | 2 |
| **`SUSPENDED`** | **`WARTEND`** | **538** | **538** | **0** | 0 | 2 | 2 |
| `CHECKED` | `UNGEKLAERT` | 276 | 276 | **0** | 50 | 0 | 3 |
| `COMMIT_REJECTED` | `FEHLER` | 111 | 111 | **0** | 0 | 1 | 2 |
| `ERROR_TIMEOUT` | `FEHLER` | 52 | 52 | **0** | 0 | 1 | 3 |
| `CKECKED` | `UNGEKLAERT` | 2 | 2 | **0** | 0 | 1 | 1 |
| **`RUNNING`** | **`LAEUFT`** | **0** | — | — | — | — | — |

**`SOSActionID` ist auf jeder einzelnen der 3.341.519 Zeilen gesetzt** — kein `NULL`, bei keinem
Status. Der Wert `0` kommt 569-mal vor, ausschließlich bei `FINISHED` und `CHECKED`.

**Laufzeit:** Aufwärmlauf 8,105 s, beste von drei **8,112 s** (8,112 · 8,131 · 8,116 s).

### (2) Führt der Verweis zu einer `SOSAction`-Zeile?

```sql
SELECT COUNT(*) AS gesetzt, SUM(a.SOSID IS NULL) AS verwaist
FROM Message m
LEFT JOIN SOSAction a ON a.SOSID = m.SOSID AND a.SOSActionID = m.SOSActionID
WHERE m.SOSActionID IS NOT NULL;
```

| gesetzt | verwaist |
|---|---|
| 3.341.519 | **1.467.042 (43,90 %)** |

**Laufzeit:** Aufwärmlauf 15,055 s, beste von drei **15,042 s**.

Das ist zunächst ein alarmierender Wert — und die Aufschlüsselung nach Status entschärft ihn
vollständig:

```sql
SELECT COALESCE(m.MessageStatus,'<NULL>') AS status, COUNT(*) AS nachrichten,
       SUM(a.SOSID IS NULL) AS verwaist,
       ROUND(100*SUM(a.SOSID IS NULL)/COUNT(*),2) AS verwaist_prozent
FROM Message m
LEFT JOIN SOSAction a ON a.SOSID = m.SOSID AND a.SOSActionID = m.SOSActionID
GROUP BY COALESCE(m.MessageStatus,'<NULL>') ORDER BY nachrichten DESC;
```

| Rohstatus | Nachrichten | verwaist | Anteil |
|---|---|---|---|
| `FINISHED` | 2.030.986 | 1.466.973 | **72,23 %** |
| `MERGED` | 747.885 | 0 | 0,00 % |
| `SPLITTED` | 400.845 | 0 | 0,00 % |
| `EERP_RECEIVED` | 143.871 | 17 | 0,01 % |
| `COMMIT_RECEIVED` | 12.654 | 0 | 0,00 % |
| `ERROR_DUPLICATE` | 3.248 | 0 | 0,00 % |
| `COMMIT_SENT` | 1.051 | 0 | 0,00 % |
| **`SUSPENDED`** | **538** | **0** | **0,00 %** |
| `CHECKED` | 276 | 52 | 18,84 % |
| `COMMIT_REJECTED` | 111 | 0 | 0,00 % |
| `ERROR_TIMEOUT` | 52 | 0 | 0,00 % |
| `CKECKED` | 2 | 0 | 0,00 % |

**Laufzeit:** Aufwärmlauf 23,751 s, beste von drei **22,965 s**.

**Die verwaisten Verweise liegen zu 99,995 Prozent bei `FINISHED`** — dem Status, bei dem der
Schritt ohnehin nicht gezeigt wird. Was übrig bleibt, sind 17 `EERP_RECEIVED` und 52 `CHECKED`.
**Bei den offenen Status ist die Verknüpfung lückenlos.**

> **Als Vermutung zur Ursache, nicht als Befund:** Der Wert `SOSActionID = 0` und ein Verweis auf
> einen Schritt, den es im heutigen Ablauf nicht mehr gibt, wären beide plausible Erklärungen für
> eine abgeschlossene Nachricht. Geprüft wurde die Lücke, nicht ihre Herkunft — für die
> Anzeigeentscheidung genügt, dass sie die offenen Zeilen nicht betrifft.

### (3) Ist der Name lesbar?

```sql
SELECT COUNT(*) AS zeilen, SUM(SOSActionName IS NULL) AS name_null,
       SUM(SOSActionName = '') AS name_leer,
       COUNT(DISTINCT SOSActionName) AS verschiedene,
       MIN(CHAR_LENGTH(SOSActionName)) AS kuerzester, MAX(CHAR_LENGTH(SOSActionName)) AS laengster
FROM SOSAction;
```

| Zeilen | `NULL` | leer | verschiedene | kürzester | längster |
|---|---|---|---|---|---|
| 3.944 | **0** | **0** | 575 | 8 Zeichen | 61 Zeichen |

Gestalt der Namen: **3.918 von 3.944 bestehen aus mehreren Wörtern**, 26 aus einem Wort ohne
Leerzeichen. **Kein einziger Name besteht nur aus Ziffern.** Laufzeiten 7,4 und 35,7 ms.

**Zehn Beispielwerte** — anonymisiert, weil in vielen Namen Kunden- und Partnernamen stehen
(`<Kunde>` ersetzt einen solchen; die übrigen Bestandteile sind Nachrichtenarten des Altsystems und
keine Kundendaten):

| `SOSActionName` | Zeilen |
|---|---|
| `Send Message to Partner` | 478 |
| `Format Conversion` | 460 |
| `Split Multiple IDOC` | 333 |
| `Merge files and wait` | 191 |
| `Send Message to Pool` | 132 |
| `Send File by Mail` | 126 |
| `Conversion DELFOR02 to VDA4905` | 186 |
| `Merge Invoice with attachments` | 111 |
| `Transport XML Inhouse to <Kunde>` | 78 |
| `Konvertierung nach §302` | 56 |

In sehr vielen weiteren Namen steht ein Kundenname als Bestandteil (`Conversion <Kunde> ORDERS96A to
XML Inhouse` und ähnlich, meist mit einstelliger Zeilenzahl je Name). Sie sind hier deshalb nicht
einzeln abgedruckt.

**Befund: Ja, `SOSAction` trägt einen für Menschen lesbaren Namen** — kein technischer Bezeichner,
keine Nummer, keine Kennung. Die Sprache ist überwiegend Englisch mit deutschen Einsprengseln; das
ist die Sprache des Quellsystems und wird nicht eingedeutscht (Richtlinie §2, Namenskonventionen).

### (4) Die offenen Nachrichten im Einzelnen — der Fall, um den es geht

```sql
SELECT m.MessageStatus, m.SOSActionID,
       COALESCE(a.SOSActionName, '<keine SOSAction-Zeile>') AS schritt, COUNT(*) AS nachrichten
FROM Message m
LEFT JOIN SOSAction a ON a.SOSID = m.SOSID AND a.SOSActionID = m.SOSActionID
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY m.MessageStatus, m.SOSActionID, schritt ORDER BY nachrichten DESC;
```

| `MessageStatus` | `SOSActionID` | Schritt | Nachrichten |
|---|---|---|---|
| `SUSPENDED` | 2 | **`Send Message to Pool`** | **538** |

**Eine einzige Zeile.** Alle 538 wartenden Nachrichten stehen auf demselben Schritt, alle lösen ihn
auf, keine ist verwaist. `RUNNING` kommt wie dokumentiert null Mal vor.

**Laufzeit:** Aufwärmlauf 38,0 ms, beste von fünf **4,57 ms** (11,20 · 6,53 · 4,66 · 4,57 · 4,72 ms).

Zum Vergleich dieselbe Auswertung über die Fehlerzustände (`ERROR_DUPLICATE`, `ERROR_TIMEOUT`,
`COMMIT_REJECTED`, 3.411 Zeilen, 79,0 ms) — dort verteilt sie sich auf sechs verschiedene Schritte,
von `Konverter VDA4908 an GSVERF IDOC` (3.248) bis `Konverter DELFOR97A an DELFOR IDOC` (3). Auch
diese lösen sämtlich auf.

### Die Verzweigung der Aufgabenstellung, ausdrücklich

> **`SOSAction` liefert einen lesbaren Namen. Aufgabe 6 wird umgesetzt.**

Mit drei Einschränkungen, die in die Feature-Dokumentation gehören:

1. **Gejoint wird über `(SOSID, SOSActionID)`**, nie über die Kennung allein — der Primärschlüssel
   ist zusammengesetzt.
2. **Der fehlende Schritt ist ein Normalfall, kein Fehler.** 43,9 Prozent aller Verweise laufen ins
   Leere; bei den offenen Zeilen null Prozent. Die Anzeige zeigt den Schritt nur bei `WARTEND` und
   `LAEUFT` und muss trotzdem mit `null` umgehen können — die Produktion muss sich nicht daran
   halten, was die Testkopie zufällig enthält.
3. **Lokal ist die Anzeige kaum prüfbar.** `RUNNING` kommt null Mal vor, `SUSPENDED` 538-mal und nur
   bis zum `2025-12-29`; alle 538 tragen denselben Schritt. Was sich prüfen lässt, ist genau eine
   Zeile mit einem Text — nicht die Vielfalt, die die Produktion zeigen wird.

---

## Auffälligkeiten

Was von der bestehenden Dokumentation abweicht. **Hier wird nichts entschieden und nichts
geändert** — außer den Stellen, die die Aufgabenstellung ausdrücklich benennt.

### A. Der Bestand hat eine fünfmonatige Lücke und einen dünnen Ausläufer

Die gravierendste Abweichung. `08.07.2026` ist zwar korrekt das Maximum, beschreibt aber nicht, wie
die Daten liegen:

```sql
SELECT DATE(MessageLastUpdate) AS tag, COUNT(*) AS zeilen
FROM Message WHERE MessageLastUpdate >= '2026-01-01' GROUP BY tag ORDER BY tag;
SELECT COUNT(*) FROM Message
WHERE MessageLastUpdate >= '2026-01-01' AND MessageLastUpdate < '2026-06-01';
```

| Tag | Zeilen |
|---|---|
| 2026-06-09 | 2 |
| 2026-06-14 | 2 |
| 2026-06-17 | 1.425 |
| 2026-06-18 | 3.419 |
| 2026-07-08 | 285 |
| **Summe 2026** | **5.133** |

Zeilen zwischen `2026-01-01` und `2026-05-31`: **0**. Laufzeiten 8,2 ms und 1,0 ms.

Das Ende des dichten Bestands:

| Tag | Zeilen |
|---|---|
| 2025-12-20 | 3.138 |
| 2025-12-21 | 9.714 |
| 2025-12-22 | 8.523 |
| 2025-12-23 | 3.971 |
| 2025-12-24 | 3.357 |
| 2025-12-25 | 2.759 |
| 2025-12-26 | 2.619 |
| 2025-12-27 | 1.988 |
| 2025-12-28 | 6.083 |
| 2025-12-29 | 6.249 |
| 2025-12-30 | 776 |
| 2025-12-31 | — |

Laufzeit 85,3 ms.

**Zusammengefasst:** dichter Bestand `2024-10-01` bis `2025-12-30` (3.336.386 Zeilen über 15 Monate),
dann **fünf Monate ohne eine einzige Zeile**, dann fünf verstreute Tage mit zusammen 5.133 Zeilen.

Betroffene Aussagen der Dokumentation:

| Fundstelle | Aussage | Messung |
|---|---|---|
| `PROJEKTBESCHREIBUNG.md` §8, `datenmodell.md` §8 | „rund 5.000 Nachrichten pro Tag" | im dichten Teil eher **rund 7.300/Tag** (3.336.386 Zeilen über 456 Tage), in den letzten sieben Monaten dagegen 5.133 Zeilen insgesamt |
| `datenmodell.md` §8 | „Aufbewahrung 22 Monate" | Spanne `2024-10-01` bis `2026-07-08` = 21 Monate, davon 5 leer |
| `datenzugriff.md` §6 | Dev-Uhr auf `MAX(MessageLastUpdate)`, damals „rund 19 Tage" Versatz | heute **23 Tage 18 h**; ein 24-h-Fenster ab diesem Bezugspunkt enthält 285 Zeilen eines einzigen Mandanten |
| `PaketstrukturTest` (Kommentar) | „die Testkopie endet Ende 2025" | trifft den **dichten** Bestand genauer als `08.07.2026`. Nicht geändert — außerhalb des Umfangs dieses Schritts |

### B. `MessageTimeout` kennt nur zwei Werte, und `NULL` ist keiner davon

`datenmodell.md` und Regel Z2 beschreiben eine „Dauer in Minuten" und sagen nichts über den
Wertebereich. Gemessen: `1800` (99,79 %) und `0` (0,21 %), niemals `NULL`.

> **Erledigt durch [M8](#m8--einheit-von-messagetimeout) am 01.08.2026:** Die Einheit ist
> **Sekunden**, `1800` sind 30 Minuten. `datenmodell.md`, `PROJEKTBESCHREIBUNG.md` und Regel Z2 sind
> entsprechend korrigiert; der Feldname der API heißt `timeoutSekunden` statt `timeoutMinuten`.
> Nebenbefund aus derselben Messung: **`ERROR_TIMEOUT` entsteht nicht aus `MessageTimeout`**,
> sondern aus einer kürzeren Frist auf Dienstebene (höchstens 120 Sekunden ab Start der letzten
> Aktion). Wer die 52 Zeilen als Beispiele für abgelaufene `MessageTimeout` liest, liest sie falsch.

### C. `ProjectMandant` ist n:m im Schema, 1:1 in den Daten

`datenmodell.md` §2 warnt ausdrücklich vor Zeilenvervielfachung durch den n:m-Join. Gemessen:
**alle 134 Projekte gehören genau einem Mandanten**, der Join vervielfacht nichts. Die Warnung ist
als Vorsichtsmaßnahme nicht falsch — sie beschreibt nur einen Fall, der in den Daten nicht auftritt.

Zu Annahme A8: 140 Zeilen in `Project` (gezählt) gegenüber 134 in `ProjectMandant` — sechs Projekte
ohne Mandantenzuordnung. Die Summe der Nachrichten je Mandant ist gleich der Gesamtzahl in
`Message`; diese sechs Projekte tragen also **keine** Nachricht.

### D. Die View `MessageMandantID` ist mit den Rechten der Anwendung nicht analysierbar

Weder `SHOW CREATE VIEW` (Fehler 1142) noch `EXPLAIN` über die View (Fehler 1345) sind mit dem
Lesebenutzer möglich; beide brauchen `SHOW VIEW`, das in `SELECT` nicht enthalten ist. In keiner
bestehenden Datei steht, dass die View damit für die Messpflicht aus Regel L7 nicht zugänglich ist.
`datenzugriff.md` §9 hält nur fest, dass die View in die Codegenerierung eingeschlossen ist.

### E. Die View läuft mit den Rechten des Definers und gilt als aktualisierbar

`SECURITY_TYPE = DEFINER` bei `DEFINER = root@…` und `IS_UPDATABLE = YES`. Ein `SELECT` durch die
View wird also nicht mit den Rechten des Lesebenutzers ausgeführt. Schreiben kann über sie niemand
aus dieser Anwendung — die Benutzer haben kein `INSERT`/`UPDATE` auf die View, und der Server läuft
global `read_only`. Der dreischichtige Schreibschutz in `datenzugriff.md` §4 erwähnt diesen Weg
nicht.

### F. `Message` hat mehr Indizes als dokumentiert

`datenmodell.md` §3 nennt drei (`MessageLastUpdateIDX`, `MessageStatusIDX`,
`MessageLastUpdateProcessMessageIDX`). Vorhanden sind sechs plus `PRIMARY`; zusätzlich
`Message_ProcessFK` und `ProejctIDIDX` (beide auf `ProcessID`, der Name ist ein Tippfehler im
Quellsystem), `SourceMessageIDIDX` und `TargetMessageIDIDX`. Die beiden letzten sind für die
Verkettung in Schritt 6 einschlägig. `ProejctIDIDX` wurde in M3 (b) tatsächlich gewählt.

### G. Die Statistiken von `information_schema` sind veraltet

| Kennzahl | `information_schema` | gezählt |
|---|---|---|
| `Message` Zeilen | 3.560.486 | 3.341.519 |
| `Project` Zeilen | 142 | 140 |

Jede `rows`-Angabe im `EXPLAIN` oben beruht auf den ersten Werten. `PROJEKTBESCHREIBUNG.md` §8 nennt
142 Projekte, `annahmen-korrekturen.md` nennt 140 — die 140 sind die gezählten.

### H. Die Statuszahlen sind gewachsen, die Statusmenge nicht

`message-status.md` führt Zahlen „seit 01.01.2025". Über den Gesamtbestand liegen sie höher
(`FINISHED` 2.030.986 statt 1.663.884, `MERGED` 747.885 statt 607.277 und so fort). Die **Menge**
der Statuswerte ist unverändert: zwölf, genau die dokumentierten ohne `RUNNING`. Kein neuer Wert.

### I. Zwei Mandanten mit sehr wenig Substanz

`NXHBE` hat **9** Nachrichten, alle am `2025-05-19` zwischen 06:14 und 06:48. `SYSTEM` hat 151.
`EDITIONLINGERI` hat keine.

### J. Die Testkopie war während der Erhebung zeitweise nicht erreichbar

Zwischen zwei Messreihen scheiterte ein Verbindungsaufbau mit `Connection timed out`, und ein
Integrationstest fiel einmalig aus (`404` erwartet, `500` erhalten); beim Wiederholungslauf war
alles grün. Das passt zu dem in `datenzugriff.md` §4 und `annahmen-korrekturen.md` bereits
dokumentierten transienten Verhalten der Testkopie und ist hier nur der Vollständigkeit halber
festgehalten.

---

## Offene Entscheidungen

Was aus den Zahlen folgen *könnte* — als Frage formuliert. Beantwortet wurden hier nachträglich nur
die beiden, die **M8** und **M9** ausdrücklich klären sollten; sie sind als erledigt gekennzeichnet.

### Zum Zeitbezug

1. ~~Wenn ein 24-Stunden-Fenster relativ zum Maximum 285 Zeilen eines einzigen Mandanten enthält:
   Bleibt der Bezugspunkt der Dev-Uhr `MAX(Message.MessageLastUpdate)`, oder wird er auf das Ende
   des **dichten** Bestands (`2025-12-30`) gelegt?~~
   **Erledigt durch [M9](#m9--anker-für-die-dev-uhr):** Der Bezugspunkt wird umgestellt auf den
   jüngsten Zeitpunkt an einem Tag mit mindestens drei Mandanten — ermittelt, nicht eingetragen.
   Umsetzung und Begründung in [`datenzugriff.md`](datenzugriff.md) §6.
2. Sind die fünf leeren Monate ein Fehler der Testkopie, der behoben wird — oder der Zustand, gegen
   den Schritt 4 zu entwickeln ist?
3. Wenn der Ausläufer bestehen bleibt: Wie unterscheidet die Oberfläche im Zustand „Leer" ein zu eng
   gewähltes Zeitfenster von einer Datenlücke, die niemand beeinflussen kann?

### Zum Isolationstest

4. Der Plan nennt `VOTG` oder `SUTTONS` als zweiten Mandanten, „der mit dem höheren Aufkommen
   gewinnt". Über den Gesamtbestand ist das `SUTTONS` (197.158 gegen 145.840). In den letzten
   30 Tagen relativ zu M0 hat **keiner von beiden** eine Zeile. Welche der beiden Zahlen entscheidet?
5. ~~Kann ein Isolationstest, der ein relatives Zeitfenster benutzt, mit `VOTG` oder `SUTTONS`
   überhaupt Daten sehen — oder braucht er ein absolutes Fenster im dichten Bestand?~~
   **Erledigt durch [M9](#m9--anker-für-die-dev-uhr):** Mit dem neuen Anker der Dev-Uhr
   (`2025-12-30 04:09:47`) liefert das relative 24-Stunden-Fenster `VOTG` 206 und `SUTTONS` 684
   Zeilen. Ein absolutes Fenster ist nicht nötig.

### Zu `MessageTimeout`

6. ~~Bedeutet `1800` dreißig **Stunden** (Minuten, wie Regel Z2 sagt) oder dreißig **Minuten**
   (Sekunden)?~~ **Erledigt durch [M8](#m8--einheit-von-messagetimeout): Sekunden**, belegt über
   `SOSActionTimeout` gegen die Ablaufmarke `WAIT|30M`. Offen bleibt der direkte Beleg an
   `Message.MessageTimeout` selbst — auf der Testkopie nicht zu bekommen, weil dort keine Nachricht
   existiert, an der diese Frist sichtbar abläuft.
7. Bedeutet `0` „kein Timeout" oder „nicht gesetzt"? Dass alle 1.051 `COMMIT_SENT` und 103 von 111
   `COMMIT_REJECTED` eine `0` tragen, `MERGED` und `SPLITTED` dagegen kein einziges Mal — spricht das
   für eine Bedeutung oder für einen technischen Nebeneffekt?
8. Wenn `NULL` in 3,34 Millionen Zeilen nie vorkommt: Behandelt die Timeout-Rechnung `NULL`
   trotzdem, oder wird der Fall als unmöglich betrachtet?

### Zur Mandantenkette

9. View oder handgeschriebenes `EXISTS`? Die `EXISTS`-Fassung war im gemessenen Fenster schneller
   (1,233 ms gegen 2,265 ms), aber die entscheidende Asymmetrie ist eine andere: Ihr Zugriffspfad
   ist einsehbar, der der View nicht. Wiegt das schwerer als die Kapselung, die
   `datenmodell.md` §2 der View zuschreibt?
10. Soll der Lesebenutzer `SHOW VIEW` auf `GlassfishDB` bekommen, damit Regel L7 auch für
    View-Zugriffe erfüllbar ist? Oder ist genau das der Grund, die View gar nicht erst zu benutzen?
11. Ist eine an 285 Zeilen gemessene Laufzeit eine tragfähige Grundlage für die Wahl des
    Zugriffspfads — oder muss vorher an einem Fenster im dichten Bestand nachgemessen werden?
12. Die `EXISTS`-Fassung nutzt `MessageLastUpdateIDX` und **kein** `filesort`. Bleibt das so, wenn
    Cursor-Paginierung über `(MessageLastUpdate, MessageID)` dazukommt — der zusammengesetzte Index
    hat `ProcessID` dazwischen (Warnung in `datenmodell.md` §3)?
13. Muss `datenzugriff.md` §4 um die View als vierten möglichen Schreibweg ergänzt werden
    (`DEFINER` = root, `IS_UPDATABLE = YES`), oder genügt der Hinweis, dass keiner unserer Benutzer
    dort schreiben darf?

### Zu `SPLITTED` und `MERGED`

14. Die Verteilung über fünfzehn Monate stützt die Entscheidung „terminal"; dass `MERGED` seit dem
    `2025-12-29` nicht mehr vorkommt, ist mit der Datenlücke erklärbar, aber nicht bewiesen. Reicht
    das als Beleg, oder braucht es eine Stichprobe gegen die **Produktion**, wo `RUNNING` existiert?
15. Wenn 34,38 % aller Zeilen Zwischenprodukte sind: Werden sie in der ungefilterten Liste
    ausgeblendet, oder erscheinen sie und werden gekennzeichnet?

### Zu `SOSName` und BAM

16. Die Verknüpfung `Message → SOS` ist lückenlos (0 ohne `SOSID`, 0 verwaist, 0 ohne Namen). Auf
    `SOSName` gibt es aber **keinen Index**. Wie soll ein Freitextfilter über 1.818 `SOS`-Zeilen
    laufen — Vorfilterung auf `SOS` und dann `SOSID IN (…)`, oder ein Join mit Filter?
17. Der Sortierindex ist gepflegt, aber bei `VOTG` **nicht eindeutig** (`2002` doppelt). Wenn die
    Liste zwei BAM-Spalten über die kleinsten Sortierindizes auswählt: Was entscheidet bei
    Gleichstand — `MessageBAMType`, die Beschreibung, oder etwas anderes?
18. `ZAST` hat genau **einen** BAM-Typ, `EDITIONLINGERI`, `SYSTEM` und `WOC` haben **keinen**. Was
    zeigen die zwei BAM-Spalten dort — nichts, eine Spalte, oder einen Ersatz?
19. ~~Bei `NEXANS` weicht der Sortierindex in 18 von 40 Zeilen vom Typ ab und gruppiert nach
    `_L_SAP` / `_K_SAP` / `_FORS`. Die beiden ersten Plätze wären damit „Abladestelle_L_SAP" und
    „Abrufnummer_L_SAP". Ist das die fachlich gewollte Auswahl?~~
    **Beantwortet durch [M11](#m11--wie-oft-ist-welcher-bam-typ-überhaupt-befüllt): Nein.** Der
    Sortierindex stellt die schwächste der drei Gruppen nach vorn. Die kuratierte Auswahl (9006 und
    9001) ist auf 98,93 % beziehungsweise 96,97 % aller Zeilen leer; die bestbelegten Typen stehen
    sämtlich in `_K_SAP`. Die Frage ist damit **nicht** für die Liste entschieden — dort entfallen
    die BAM-Spalten —, sondern als Vorgabe für die Suchfelder in Schritt 7.

### Zu den Zwischenschritten in der Oberfläche

20. ~~Gilt der Ausblenden-Schalter für alle Mandanten?~~
    **Beantwortet durch [M12](#m12--zwischenschritte-je-mandant): Nein.** Fünf von neun Mandanten mit
    Nachrichten haben über den gesamten Bestand keine einzige Zwischenschritt-Zeile. Offen bleibt,
    ob der Grenzfall `VOTG` (40 von 145.840 Zeilen) fachlich als „hat Zwischenschritte" gelten soll —
    die Umsetzung sagt Ja, weil ein Kriterium über den Gesamtbestand nicht mit dem Zeitfenster
    flackert.

### Zum aktuellen Schritt

21. ~~Trägt `Message.SOSActionID` etwas, das man einem Nutzer zeigen kann?~~
    **Beantwortet durch [M13](#m13--trägt-sosactionid-einen-lesbaren-namen): Ja.** `SOSActionName`
    ist durchgängig gepflegt und lesbar. Offen bleibt die Herkunft der 43,9 % verwaisten Verweise —
    sie liegen zu 99,995 % bei `FINISHED` und berühren die Anzeige nicht.

---

## Anhang: Laufzeiten auf einen Blick

Beste von N Läufen nach einem Aufwärmlauf, serverseitig gemessen.

| Messung | Statement | N | Laufzeit | Zugriffspfad |
|---|---|---|---|---|
| M1 | Spalten aus `information_schema.COLUMNS` | 1 | 2,4 ms | — |
| M1 | Indizes aus `information_schema.STATISTICS` | 1 | 1,8 ms | — |
| M0 | `MIN`/`MAX`/`COUNT` über `Message` | 5 | **864 ms** | `index`, `MessageLastUpdateIDX`, `Using index` |
| M0 | Abstand zur Systemzeit | 1 | 0,9 ms | `Select tables optimized away` |
| M2 a | Verteilung `MessageTimeout`, Kennzahlen | 5 | **2.315 ms** | `ALL` |
| M2 b | `GROUP BY MessageTimeout` | 5 | **1.691 ms** | `ALL`, `temporary` + `filesort` |
| M2 + | Timeout je Status | 1 | 11.428 ms | `ALL` |
| M3 a | je Mandant, 30 Tage | 5 | **32,6 ms** | `range`, `MessageLastUpdateProcessMessageIDX` |
| M3 b | je Mandant, Gesamtbestand | 3 | **16,392 s** | `index`, `ProejctIDIDX` |
| M3 + | Zeilen nach dem Join | 1 | 12,568 s | `ALL` |
| M3 + | Zeitspanne je Mandant | 1 | 20,465 s | `index`, `ProejctIDIDX` |
| M4 | Metadaten der View | 1 | 1,8 ms | — |
| M4 | `COUNT` über die View, **ohne** Zeitfenster | 1 | 1.216 ms | nicht einsehbar |
| M4 a | 24 h + Mandant **über die View** | 5 | **2,265 ms** | nicht einsehbar |
| M4 b | 24 h + Mandant **mit `EXISTS`** | 5 | **1,233 ms** | `range`, `MessageLastUpdateIDX`, kein `filesort` |
| M4 | Gegenprobe gleiche Menge | 1 | 5,6 ms | — |
| M5 a | `SOSID` fehlend | 3 | **1.492 ms** | `ALL` |
| M5 b | `SOSID` verwaist | 3 | **10,303 s** | `ALL` + `eq_ref`, `Not exists` |
| M5 + | Pflegezustand `SOSName` | 1 | 3,1 ms | `ALL` über `SOS` (1.818 Zeilen) |
| M6 | vier Status, Spanne und Fenster | 3 | **18,773 s** | `range`, `MessageStatusIDX` |
| M6 + | 30/90 Tage je Status | 1 | 17,037 s | `range`, `MessageStatusIDX` |
| M6 + | Monatsverteilung | 1 | 11,587 s | `ALL` |
| M7 | BAM je Mandant | 5 | **0,937 ms** | `index`, `MandantIDSortIndexBAMTYpeIDX`, kein `filesort` |
| M7 + | Pflegezustand Sortierindex | 1 | 1,2 ms | `index` |
| M8 a | Dauer der 52 `ERROR_TIMEOUT` | 1 | 4,4 ms | `range`, `MessageStatusIDX` + `eq_ref` auf `MessageAction` |
| M8 + | Start der letzten Aktion bis Fehler | 1 | 5,9 ms | wie M8 a |
| M8 + | `WAIT`-Marke gegen `SOSActionTimeout` | 1 | **97.976 ms** | `LIKE '%…%'` über `mediumtext`, 10,2 Mio. Zeilen — einmalige Erhebung, kein Muster für Code |
| M8 + | dasselbe über die Stammdaten `SOSAction` | 1 | 70,9 ms | `ALL` über 3.944 Zeilen |
| M9 | jüngster Tag mit ≥ 3 Mandanten | 3 | **6,923 s** | `index`, `MessageLastUpdateProcessMessageIDX`, `filesort` |
| M9 + | 24-h-Fenster ab dem neuen Anker | 1 | 51,2 ms | `range`, `MessageLastUpdateIDX` |
| A | Tagesverteilung 2026 | 1 | 8,2 ms | `range`, `MessageLastUpdateIDX` |
| A | Tagesverteilung Dez 2025 | 1 | 85,3 ms | `range`, `MessageLastUpdateIDX` |
| L1 | Liste NEXANS, 24 h | 5 | **2,715 ms** | `range`, `MessageLastUpdateIDX`, kein `filesort` |
| L2 | Liste NEXANS, 30 Tage | 5 | **2,709 ms** | wie L1 |
| L3 | Liste NEXANS, ein Jahr | 5 | **2,692 ms** | wie L1 |
| L4 | Liste SUTTONS, 24 h | 5 | **33,978 ms** | wie L1, aber 2.890 statt 194 gelesene Zeilen |
| L5 | Liste NEXANS, ein Jahr, FEHLER | 5 | **24,803 ms** | `range`, `MessageStatusIDX`, `filesort` |
| L6 | Liste SUTTONS, ein Jahr, FEHLER | 5 | **25,162 ms** | wie L5 |
| L7a | Vorfilter Prozess-/Projektname | 5 | **2,975 ms** | `ref`, `ProjectMandant_Mandant_idx` |
| L7b | Vorfilter Ablaufname | 5 | **5,806 ms** | `ref`, `ProjectMandant_Mandant_idx` + `SOS_ProcessFK` |
| L7c | Liste NEXANS, 30 Tage, Freitext | 5 | **1.295,872 ms** | `range`, `MessageLastUpdateIDX`, `r_filtered` 0,01 % |
| L8 | Liste NEXANS, 24 h, zweite Seite | 5 | **1,795 ms** | `range`, `MessageLastUpdateIDX`, `key_len` 151 |
| L9 | BAM-Nachladung, 50 Kennungen | 5 | **2,982 ms** | `range`, `PRIMARY`, `Using index` |
| L10 | BAM-Nachladung, 200 Kennungen | 5 | **3,440 ms** | `range`, `PRIMARY`, `Using index` |
| M10 | Abweichung `SOS.ProcessID` gegen `Message.ProcessID` | 1 | 10.877 ms | `ALL` + `eq_ref` — Ergebnis **9.101**, der Umbau entfällt |
| M10 + | dieselbe Abweichung je Mandant | 1 | 29.390 ms | `ALL` + zwei `eq_ref` |
| L11 | Freitext, volumenstarker Begriff, **OR-Form** | 5 | **2,290 ms** | `range`, `MessageLastUpdateIDX` |
| L11 | derselbe Begriff, **`UNION`-Alternative** | 5 | **7.206,8 ms** | beide Zweige `MessageLastUpdateIDX`, Fenster doppelt gelesen |
| L11 | Freitext, selektiver Begriff, **OR-Form** | 5 | **499,1 ms** | `range`, `MessageLastUpdateIDX`, `r_rows` 78.318 |
| L11 | derselbe Begriff, **`UNION`-Alternative** | 5 | **985,4 ms** | wie oben, `r_rows` 78.318 **je Zweig** |
| L11 + | nur `ProcessID IN (…)`, volumenstark | 2 | 5.865,4 ms | `ref`, `ProejctIDIDX` — liest den ganzen Bestand der Prozesse |
| L11 + | nur `SOSID IN (…)`, volumenstark | 2 | 1.323,6 ms | `range`, `MessageLastUpdateIDX` — auf `SOSID` gibt es keinen Index |
| L12 | Prozessauswahl `NEXANS` (733 Prozesse) | 5 | **4,225 ms** | `ref`, `ProjectMandant_Mandant_idx`, `temporary` + `filesort` |
| L12 | Prozessauswahl `SUTTONS` (17 Prozesse) | 5 | **0,606 ms** | wie oben |
| L13 | Freitext, selektiver Begriff, 30 Tage (Kontrolle) | 5 | **488,4 ms** | `range`, `MessageLastUpdateIDX`, `r_rows` 78.318 |
| L13a | derselbe Begriff, 90 Tage | 5 | **487,9 ms** | wie oben, `r_rows` **unverändert** 78.318 |
| L13b | derselbe Begriff, 180 Tage | 5 | **495,1 ms** | wie oben |
| L13c | derselbe Begriff, ein Jahr | 5 | **478,2 ms** | wie oben |
| L13 | schlimmster Fall (kein Treffer im Fenster), 30 Tage | 5 | **1.246 ms** | `range`, `MessageLastUpdateIDX`, `r_rows` 214.330 |
| L13 | derselbe Fall, 90 Tage | 5 | **3.937 ms** | `r_rows` 680.872 — **gewählte äußere Grenze** |
| L13 | derselbe Fall, 180 Tage | 3 | **8.005 ms** | `r_rows` 1.385.909 — 80 % von `max_statement_time` |
| L13 | derselbe Fall, ein Jahr | 3 | **15.636 ms** | `r_rows` 2.713.376 — **reisst die Zeitgrenze** |
| M11 | Bezugsmenge NEXANS, 30 Tage | 2 | **683 ms** | `range`, `MessageLastUpdateProcessMessageIDX` |
| M11 | BAM-Abdeckung je Typ, mit Ø und Maximum | 3 | **12,689 s** | `range` + `ref` auf `MessageBAM.PRIMARY`, `Using index` |
| M11 | dieselbe Auswertung ohne Ø und Maximum | 1 | 5.294 ms | wie oben, ohne die innere Gruppierung |
| M12 a | Zwischenschritte je Mandant, Gesamtbestand | 3 | **32,677 s** | `ALL` + zwei `eq_ref`, `temporary` + `filesort` |
| M12 b | dieselbe Auswertung, 30 Tage | 3 | **3,219 s** | `range`, `MessageLastUpdateIDX` |
| M13 | `SOSAction` aus `information_schema` | 1 | 2,7 ms | — |
| M13 (1) | `SOSActionID` je Rohstatus | 3 | **8,112 s** | `ALL` über `Message` |
| M13 (2) | verwaiste Verweise gesamt | 3 | **15,042 s** | `ALL` + `eq_ref` auf `SOSAction.PRIMARY` |
| M13 (3) | verwaiste Verweise je Status | 3 | **22,965 s** | wie oben, zusätzlich `temporary` + `filesort` |
| M13 (4) | die offenen Nachrichten (`SUSPENDED`/`RUNNING`) | 5 | **4,57 ms** | `range`, `MessageStatusIDX` + `eq_ref` |
| M13 + | Pflegezustand `SOSActionName` | 1 | 7,4 ms | `ALL` über 3.944 Zeilen |

**Muster, das über alle Messungen hinweg sichtbar ist:** Sobald ein Zeitfenster gesetzt ist, arbeitet
MariaDB im `range`-Zugriff über `MessageLastUpdate` und braucht Millisekunden. Ohne Zeitfenster wird
jede Abfrage über `Message` zu einem vollen Durchlauf von 1,5 bis 20 Sekunden. Regel L1
(Pflicht-Zeitfenster) hat in diesen Zahlen ihre Entsprechung.

**Die eine Ausnahme, und was sie wirklich sagt** (L7c, L11, L13): Der Freitextfilter bleibt im
`range`-Zugriff und braucht trotzdem Sekunden. Nicht die Fenstergröße treibt ihn — L13 zeigt
dieselbe halbe Sekunde über 30 Tage wie über ein Jahr —, sondern die Zahl der Zeilen, die vor dem
letzten gelieferten Treffer liegen. Ein Zeitfenster begrenzt diese Zahl nach oben; mehr kann es
nicht. Deshalb steht neben dem Pflicht-Zeitfenster aus L1 eine zweite, engere Grenze für den einen
Filter, der sie braucht.
