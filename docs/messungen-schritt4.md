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
19. Bei `NEXANS` weicht der Sortierindex in 18 von 40 Zeilen vom Typ ab und gruppiert nach
    `_L_SAP` / `_K_SAP` / `_FORS`. Die beiden ersten Plätze wären damit „Abladestelle_L_SAP" und
    „Abrufnummer_L_SAP". Ist das die fachlich gewollte Auswahl?

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

**Muster, das über alle Messungen hinweg sichtbar ist:** Sobald ein Zeitfenster gesetzt ist, arbeitet
MariaDB im `range`-Zugriff über `MessageLastUpdate` und braucht Millisekunden. Ohne Zeitfenster wird
jede Abfrage über `Message` zu einem vollen Durchlauf von 1,5 bis 20 Sekunden. Regel L1
(Pflicht-Zeitfenster) hat in diesen Zahlen ihre Entsprechung.
