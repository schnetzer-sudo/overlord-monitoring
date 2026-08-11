# Messungen vor Schritt 6 — Verkettung

Erhoben am **10.08.2026** gegen die Testkopie (MariaDB 10.6.22).
Bezugsdokumente: [`messungen-schritt4.md`](messungen-schritt4.md) (M0–M13),
[`messungen-schritt5.md`](messungen-schritt5.md) (M14–M22).

**Nummerierung ab M23.** Geprüft: M22 war beim Anlegen dieser Datei die letzte in
`messungen-schritt5.md` vergebene Nummer.

> **Nachgetragen 10.08.2026.** Die Nummerierung läuft **projektweit** fort und nicht je Datei.
> Inzwischen stehen hier drei Blöcke — **M23–M27** samt E1–E5 (Verkettung), **M28** (Rollen und
> Kandidatenprädikate) und **M30** (die Kosten der Auflösung, vor Schritt 6 Teil 1). **M29** liegt
> dazwischen und steht in `messungen-schritt5.md`: der Nachtrag zum Wartezustand.
>
> **Ergänzt 11.08.2026.** Dazugekommen ist **M31** — die Nacharbeit zu Schritt 6 Teil 2b: wie viele
> Ansichten der Zickzack-Aufstieg verzerrt, und ob sich der Takt des `MatchInterchange`-Events von
> der Testkopie aus überhaupt messen lässt. M31 steht **nach** M30 und **vor** der Zusammenfassung;
> seine Laufzeiten stehen in einer eigenen Tabelle innerhalb des Blocks.

**Anlass.** Die Rückmeldung vom 10.08.2026 zu `MERGED` und `SPLITTED`: Ein Prozessbaum aus der
**Produktion** zeigt eine `SPLITTED`-Nachricht (`20385_<Kunde>_711004_LS`, 11:46:11) mit zwei
`FINISHED`-Kindern (`NXS_SingleIDOCOUT`, 11:52:07) darunter — der Status markiert dort erkennbar den
**Vorgänger**. Zugleich zeigt der Ablauf `30760_<Kunde>_725015_LAB` / `..._LAB_MERGED`, dass die
zusammengeführte Datei über einen eigenen Ablauf zum Partner geht.

> Die Partnerbestandteile beider Nachrichtennamen sind nach der Regel in [`README.md`](README.md)
> als `<Kunde>` maskiert; `NXS_SingleIDOCOUT` bleibt stehen, weil `NXS` ein Mandantenkürzel ist und
> Bausteinmarken ausdrücklich nicht geschützt sind.

Beides stellte die Prämisse hinter `zwischenschritte=false` in Frage
([`nachrichtenliste.md`](nachrichtenliste.md) §5). **Die Einordnung `ZWISCHENSCHRITT` selbst stand
nicht zur Debatte** — sie gehört der Überfälligkeitsrechnung, ist durch M6 gestützt und bleibt
unverändert. Zur Debatte stand ausschließlich die **Sichtbarkeitsentscheidung**, die daraus abgeleitet
wurde, ohne je gemessen worden zu sein.

> **Das Ergebnis vorweg, in einem Satz:** Der Screenshot ist der Normalfall. Der Elternteil trägt
> `SPLITTED`, die Kinder tragen `FINISHED` — und **96,9 Prozent** der Wurzeln tragen BAM-Werte gegen
> **2,4 Prozent** der Kinder, also sitzt die Belegnummer auf genau der Zeile, die die Liste per
> Vorgabe ausblendet.
>
> Alle Prozentpaare in diesem Dokument beziehen sich auf die **Stellung in der Kette** (M26‑1b),
> nicht auf den Status. Nach Status gemessen lautet dasselbe Verhältnis 97,7 % (`SPLITTED`) gegen
> 4,9 % (`FINISHED`, M26‑1); beide Paare sagen dasselbe, aber sie werden nicht vermischt.

---

## Die Lesarten standen vor der Messung fest

Jede Messung trägt einen Abschnitt **„Was daraus folgt"** mit den Befunden, die **vor** der Erhebung
aufgeschrieben wurden, und der jeweils zugehörigen Konsequenz. Nachträglich ergänzt ist in diesen
Tabellen ausschließlich die Spalte **„trifft zu"**. Wo ein gemessener Befund in keine der
vorformulierten Zeilen passt, steht das als eigener Absatz darunter — und nicht als stillschweigend
passend gemachte Zeile. Das ist an **acht** Stellen der Fall — M23, M25, M26, M28‑1, M28‑2 und
dreimal in M30 — und jedes Mal ausgewiesen.

---

## 0. Rahmen der Messung

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` (Ubuntu 22.04) — niemals die Produktion |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**. Erste Abfrage der Sitzung, vor jedem anderen Statement |
| Benutzer | **Lesebenutzer** `monitor_read@%` |
| Serverzeit zu Beginn | `2026-08-10 12:21:08` (`UTC_TIMESTAMP` `10:21:08`, also UTC+2) |
| Client | `mysql` `8.0.46` aus MySQL Workbench mit `--ssl-mode=DISABLED` — derselbe wie in Schritt 4 und 5, damit die Zahlen vergleichbar bleiben |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `SHOW PROFILES` — nie um den Client-Aufruf herum |
| Wiederholungen | beste von fünf Läufen nach einem Aufwärmlauf; bei Statements über zehn Sekunden beste von drei |
| Zugangsdaten | ausschließlich aus Umgebungsvariablen (`OVERLORD_DB_*`), an den Client über `MYSQL_PWD` übergeben |

### `SHOW GRANTS FOR CURRENT_USER()`

Drei Zeilen, unverändert gegenüber Schritt 4 und 5. Der Passwort-Hash der ersten wird nach Regel G1
**nicht** abgedruckt:

```
GRANT USAGE ON *.* TO `monitor_read`@`%` IDENTIFIED BY PASSWORD '<Hash>'
GRANT SELECT ON `GlassfishDB`.* TO `monitor_read`@`%`
GRANT SELECT ON `overlord_monitor`.* TO `monitor_read`@`%`
```

### Bezugsfenster

| | Zeitraum | Bezug |
|---|---|---|
| **Fenster A (Tag)** | `2025-12-29 00:00:00` ≤ x < `2025-12-30 00:00:00` | **6.249** Nachrichten — bestätigt in M23‑2 (a), deckt sich mit M15 |
| **Fenster B (Monat)** | `2025-11-30 00:00:00` ≤ x < `2025-12-30 00:00:00` | **214.330** Nachrichten — bestätigt in M23‑2 (b), deckt sich mit L13 und `messungen-schritt5.md` |
| **Kinderüberhang** | Fenster + 1 Tag am oberen Rand | Kinder entstehen **nach** dem Elternteil. Ohne Überhang fehlen die Kinder der letzten Stunden |

**`MERGED` endet in der Testkopie am `2025-12-29 23:22:43`** (M6). Beide Fenster enthalten den Status
noch; Fenster A mit 432 Zeilen, Fenster B mit 38.628.

**Kein Mandantenfilter**, aus demselben Grund wie in M14 bis M22: Die Fragen betreffen das
Datenmodell, nicht den Ausschnitt eines Kunden. Wo die Mandantensicht die Frage ist, steht sie
ausdrücklich in der Messung (M24‑3, M26‑3, M27, E1).

### Anonymisierung

Keine Belegnummer und kein BAM-Wert steht in diesem Dokument. **Ablaufnamen der Testkopie enthalten
Partnernamen** (`SOS.SOSName`, Gestalt „Lieferabruf an ⟨Partner⟩ (⟨Nummer⟩)"). Nach der Regel in
[`README.md`](README.md) wird der Partnerbestandteil als `<Kunde>` maskiert; die Nummer bleibt, weil
sie Konfigurationsvokabular ist und niemanden benennt. Die vollständige Namensliste aus M25‑1 ist
**nicht** abgedruckt — sie trüge nichts bei, was die Gestaltauswertung nicht schon sagt.

---

## Reihenfolge

**M23 zuerst**, weil die Gestalt von M24 und M25 davon abhängt, ob die beiden Verkettungsspalten
indiziert sind. Sie sind es (M23‑1) — beide Richtungen waren damit frei messbar.

---

# M23 — Vorabprüfung: Indizes und Belegung der vier Verkettungsspalten

**Frage.** Sind `SourceMessageID` und `TargetMessageID` indiziert? Und wie oft sind die vier
Verkettungsangaben überhaupt belegt — aufgeschlüsselt nach Status?

## M23‑1 Indizes

```sql
SELECT INDEX_NAME, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART, NULLABLE, CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
```

**Ergebnis** — zehn Indexspalten in acht Indizes:

| `INDEX_NAME` | `SEQ_IN_INDEX` | `COLUMN_NAME` | `SUB_PART` | `NULLABLE` | `CARDINALITY` |
|---|---|---|---|---|---|
| `MessageLastUpdateIDX` | 1 | `MessageLastUpdate` | — | YES | 1.780.243 |
| `MessageLastUpdateProcessMessageIDX` | 1 | `MessageLastUpdate` | — | YES | 1.780.243 |
| `MessageLastUpdateProcessMessageIDX` | 2 | `ProcessID` | — | YES | 3.560.486 |
| `MessageLastUpdateProcessMessageIDX` | 3 | `MessageID` | — | NO | 3.560.486 |
| `MessageStatusIDX` | 1 | `MessageStatus` | — | YES | 18 |
| `Message_ProcessFK` | 1 | `ProcessID` | — | YES | 18 |
| `PRIMARY` | 1 | `MessageID` | — | NO | 3.560.486 |
| `ProejctIDIDX` | 1 | `ProcessID` | — | YES | 18 |
| **`SourceMessageIDIDX`** | 1 | **`SourceMessageID`** | — | YES | **1.780.243** |
| **`TargetMessageIDIDX`** | 1 | **`TargetMessageID`** | — | YES | **63.580** |

**Beide Verkettungsspalten sind indiziert, beide über die volle Länge** (`SUB_PART` ist überall leer
— anders als bei `MessageProperty`, wo M14 Präfix-Indizes über 50 Zeichen gefunden hat).

Spaltendefinition (`information_schema.COLUMNS`), weil sie für die Auswertung gebraucht wird:

| Spalte | Typ | `NULL`? | Vorgabe | Sortierung |
|---|---|---|---|---|
| `MessageID` | `varchar(36)` | NO | — | `utf8mb4_general_ci` |
| `SourceMessageID` | `varchar(36)` | YES | `NULL` | `utf8mb4_general_ci` |
| `TargetMessageID` | `varchar(36)` | YES | `NULL` | `utf8mb4_general_ci` |
| `Source` | `bit(1)` | YES | `b'0'` | — |
| `Target` | `bit(1)` | YES | `b'0'` | — |

Größen der beteiligten Tabellen (`information_schema.TABLES`), zum Abgleich mit dem Plantext:

| Tabelle | Zeilen (geschätzt) | Daten | Index | zusammen |
|---|---:|---:|---:|---:|
| `Message` | 3.560.486 | 741 MB | 2.157 MB | **2,90 GB** |
| `MessageAction` | 10.215.743 | 2.227 MB | 820 MB | **3,05 GB** |
| `MessageBAM` | 10.859.666 | 1.826 MB | 5.254 MB | **7,08 GB** |
| `SOS` | 1.865 | 0,33 MB | 0,20 MB | 0,52 MB |
| `Process` | 1.490 | 0,25 MB | 0,20 MB | 0,44 MB |
| `ProjectMandant` | 134 | 0,02 MB | 0,02 MB | 0,03 MB |

Die Größen decken sich mit [`datenmodell.md`](datenmodell.md) §8 (2,9 / 3,0 / 7,1 GB) und mit der
Angabe im Implementierungsplan zu Schritt 7.

> **Die Testkopie ist seit dem 07.08.2026 nicht neu befüllt worden.** `MessageAction` steht hier auf
> **byteidentischen** Werten wie in [M14](messungen-schritt5.md) (`DATA_LENGTH` 2.226.634.752,
> `INDEX_LENGTH` 819.855.360). Das ist der Grund, warum die Zahlen dieser Runde ohne Vorbehalt gegen
> M0 bis M22 gehalten werden dürfen — und es ist geprüft, nicht angenommen.

> ⚠️ **`TABLE_ROWS` ist bei InnoDB eine Schätzung, kein Zählwert**, und die Abweichung ist hier
> sichtbar: `Message` steht mit 3.560.486 gegen die **gezählten** 3.341.519 aus M0, `MessageAction`
> mit 10.215.743 gegen die gezählten 10.308.590 aus M14 — einmal darüber, einmal darunter. Dasselbe
> gilt für die `CARDINALITY`-Spalte der Indextabelle oben. Für die Frage dieser Messung („gibt es den
> Index?") ist das folgenlos; als Mengengerüst sind die gezählten Werte zu nehmen.

**Laufzeit:** unter 10 ms (Katalogabfrage, nicht weiter gemessen).

## M23‑2 Belegung je Status

```sql
SELECT MessageStatus,
       COUNT(*)                                                   AS zeilen,
       SUM(SourceMessageID IS NOT NULL AND SourceMessageID <> '') AS mit_vorgaenger,
       SUM(SourceMessageID IS NULL)                               AS src_null,
       SUM(SourceMessageID = '')                                  AS src_leer,
       SUM(TargetMessageID IS NOT NULL AND TargetMessageID <> '') AS mit_nachfolger,
       SUM(TargetMessageID IS NULL)                               AS tgt_null,
       SUM(TargetMessageID = '')                                  AS tgt_leer,
       SUM(Source + 0)                                            AS flag_source,
       SUM(Target + 0)                                            AS flag_target
FROM Message
WHERE MessageLastUpdate >= '2025-12-29 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY MessageStatus
ORDER BY zeilen DESC;
```

> **`Source` und `Target` sind `bit`.** `SUM(Source)` liefert dort keinen brauchbaren Wert;
> `SUM(Source + 0)` erzwingt die numerische Auswertung. Bestätigt: Die Form mit `+ 0` liefert die
> erwarteten Zahlen. Dieselbe Falle wie bei jeder `bit`-Spalte — sie gehört in
> [`datenmodell.md`](datenmodell.md).
>
> **Leerer String und `NULL` sind getrennt geprüft** — die Spalten `src_null`/`src_leer` und
> `tgt_null`/`tgt_leer` sind gegenüber der geplanten Fassung ergänzt, weil die geplante Fassung die
> Frage nicht beantwortet hätte.

**`EXPLAIN`** (Fenster A):

| id | select_type | table | type | possible_keys | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `Message` | `range` | `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdateIDX` | 5 | 11.812 | `Using index condition; Using temporary; Using filesort` |

### Ergebnis (a) — Fenster A, 6.249 Zeilen

| `MessageStatus` | Zeilen | mit Vorgänger | mit Nachfolger | `Source` | `Target` |
|---|---:|---:|---:|---:|---:|
| `FINISHED` | 5.360 | 4.085 | 0 | 89 | 0 |
| `MERGED` | 432 | **0** | **432** | 0 | 0 |
| `SPLITTED` | 390 | 3 | **0** | **390** | 0 |
| `EERP_RECEIVED` | 65 | 8 | 0 | 0 | 6 |
| `ERROR_DUPLICATE` | 1 | 0 | 0 | 0 | 0 |
| `SUSPENDED` | 1 | 0 | 0 | 0 | 0 |

### Ergebnis (b) — Fenster B, 214.330 Zeilen

| `MessageStatus` | Zeilen | mit Vorgänger | mit Nachfolger | `Source` | `Target` |
|---|---:|---:|---:|---:|---:|
| `FINISHED` | 139.474 | 102.954 | 0 | 2.459 | 19 |
| `MERGED` | 38.628 | **0** | **38.628** | 0 | 0 |
| `SPLITTED` | 28.144 | 263 | **0** | **28.144** | 16 |
| `EERP_RECEIVED` | 6.898 | 684 | 0 | 150 | 1.601 |
| `COMMIT_RECEIVED` | 552 | 552 | 0 | 166 | 33 |
| `SUSPENDED` | 538 | 0 | 0 | 0 | 0 |
| `COMMIT_SENT` | 80 | 80 | 0 | 0 | 0 |
| `CHECKED` | 5 | 0 | 0 | 0 | 0 |
| `COMMIT_REJECTED` | 5 | 5 | 0 | 0 | 0 |
| `ERROR_DUPLICATE` | 3 | 0 | 0 | 0 | 0 |
| `ERROR_TIMEOUT` | 3 | 0 | 0 | 0 | 0 |

**Unbelegt heißt immer `NULL`, nie leerer String.** In beiden Fenstern ist `src_leer` und `tgt_leer`
durchgängig `0` beziehungsweise `NULL` (letzteres dort, wo *alle* Werte `NULL` sind und der Vergleich
deshalb kein Ergebnis liefert). Die Bedingung `<> ''` in allen Statements dieser Runde ist damit
folgenlos — sie bleibt trotzdem stehen, weil die Produktion sich nicht daran halten muss, was die
Testkopie zufällig enthält.

**Laufzeit:** **0,059 s** (Fenster A) · **1,795 s** (Fenster B), je beste von fünf.

### Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| `SourceMessageID`/`TargetMessageID` **indiziert** | **ja** | M24 und M25 konnten frei über die Kette laufen; Schritt 6 löst Vorgänger und Nachfolger direkt auf |
| **nicht indiziert** | nein | — (die Rückfalloption „eigener Index in `overlord_monitor`" entfällt) |
| `SPLITTED`/`MERGED` haben **keinen** Vorgänger, aber einen Nachfolger | **teilweise** | siehe Absatz unten |
| `SPLITTED`/`MERGED` haben **beides** | nein | — |
| Die Flags `Source`/`Target` decken sich **nicht** mit der Belegung der ID-Spalten | **ja, aber** | siehe Absatz unten und E4 |

**Wo die vorformulierte Zeile nicht passt.** Sie behandelt `SPLITTED` und `MERGED` als einen Fall.
Gemessen verhalten sie sich **entgegengesetzt**:

- **`MERGED` hat keinen Vorgänger und in 100 Prozent der Fälle einen Nachfolger** (432/432 und
  38.628/38.628). Für diesen Status trifft die Zeile wörtlich zu — nur bedeutet sie nicht „Wurzel",
  sondern das Gegenteil: Die `MERGED`-Zeile ist ein **Eingang**, der auf das Ergebnis zeigt.
- **`SPLITTED` hat weder das eine noch das andere** — kein Vorgänger (3 von 390), kein Nachfolger
  (0 von 390). Dass es trotzdem die Wurzel ist, steht in einer Spalte, die die vorformulierte Zeile
  gar nicht betrachtet: `Source = 1` auf **jeder einzelnen** `SPLITTED`-Zeile (390/390 und
  28.144/28.144). Die Kinder tragen den Rückverweis, nicht der Elternteil den Vorwärtsverweis.

**Zu den Flags.** Der vorformulierte Befund lautete „decken sich nicht mit der Belegung der
ID-Spalten → zwei unabhängige Wahrheiten über dieselbe Beziehung". Die erste Hälfte stimmt
(`MERGED` hat `TargetMessageID` belegt und `Target = 0`; `SPLITTED` hat `Source = 1` und
`SourceMessageID` leer), **die daraus gezogene Konsequenz stimmt nicht.** Es sind keine zwei
konkurrierenden Wahrheiten, sondern zwei Hälften derselben: Die ID-Spalte zeigt **weg** von der
Zeile, das Flag beschreibt, ob **auf sie** gezeigt wird. E4 belegt das mit einer exakten Deckung in
beiden Richtungen. In Schritt 6 ist deshalb **nichts festzulegen und nichts zu verwerfen** — beide
werden gelesen, jede für die Frage, die nur sie beantwortet.

---

# M24 — Richtung und Breite der Verkettung

**Frage.** Trägt `SPLITTED` beziehungsweise `MERGED` der **Vorgänger** oder der **Nachfolger**? Und
wie viele Kinder kann ein Elternteil haben — die Zahl, die die Darstellung in Schritt 6 begrenzen
muss?

## M24‑1 Kinder je Elternteil, nach Status des Elternteils

```sql
SELECT e.MessageStatus         AS eltern_status,
       COUNT(*)                AS eltern_zeilen,
       SUM(k.kinder)           AS kinder_gesamt,
       ROUND(AVG(k.kinder), 2) AS kinder_schnitt,
       MAX(k.kinder)           AS kinder_max
FROM (
  SELECT SourceMessageID AS eltern_id, COUNT(*) AS kinder
  FROM Message
  WHERE MessageLastUpdate >= '2025-12-29 00:00:00'
    AND MessageLastUpdate <  '2025-12-31 00:00:00'      -- Fenster A + Überhang
    AND SourceMessageID IS NOT NULL AND SourceMessageID <> ''
  GROUP BY SourceMessageID
) k
JOIN Message e ON e.MessageID = k.eltern_id
GROUP BY e.MessageStatus
ORDER BY eltern_zeilen DESC;
```

**`EXPLAIN`** — die abgeleitete Tabelle läuft wie vorgesehen über `MessageLastUpdateIDX`, der
Elternteil kommt als `eq_ref` über den Primärschlüssel dazu:

| id | select_type | table | type | possible_keys | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `<derived2>` | `ALL` | — | — | — | 6.239 | `Using where; Using temporary; Using filesort` |
| 1 | PRIMARY | `e` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | 1 | |
| 2 | DERIVED | `Message` | `range` | `SourceMessageIDIDX`, `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdateIDX` | 5 | 13.030 | `Using index condition; Using where; Using temporary; Using filesort` |

**Ergebnis** (Fenster A + Überhang):

| Status des Elternteils | Elternzeilen | Kinder gesamt | Schnitt | Maximum |
|---|---:|---:|---:|---:|
| `SPLITTED` | 530 | 4.406 | 8,31 | **235** |
| `FINISHED` | 89 | 90 | 1,01 | 2 |

**Kein anderer Status kommt als Elternteil vor** — insbesondere `MERGED` nicht ein einziges Mal.

**Gegenprobe:** **null** Eltern-IDs ohne zugehörige Zeile. Jeder `SourceMessageID` löst auf; es gibt
keine ins Leere zeigenden Rückverweise.

**Status der Kinder je Elternstatus** (dieselbe Menge, andere Gruppierung):

| Status des Elternteils | Status des Kindes | Kinder |
|---|---|---:|
| `SPLITTED` | `FINISHED` | 4.400 |
| `FINISHED` | `FINISHED` | 84 |
| `FINISHED` | `EERP_RECEIVED` | 6 |
| `SPLITTED` | `EERP_RECEIVED` | 3 |
| `SPLITTED` | `SPLITTED` | 3 |

**Laufzeit:** **0,066 s**, beste von fünf.

## M24‑2 Breitenverteilung

Fenster B + Überhang, mit Absicht: Der Ausreißer entscheidet hier, nicht der Schnitt.

```sql
SELECT CASE WHEN k.kinder = 1    THEN '1'
            WHEN k.kinder <= 5   THEN '2-5'
            WHEN k.kinder <= 20  THEN '6-20'
            WHEN k.kinder <= 50  THEN '21-50'
            WHEN k.kinder <= 100 THEN '51-100'
            WHEN k.kinder <= 300 THEN '101-300'
            ELSE                      'ueber 300' END AS breite,
       COUNT(*) AS eltern, MIN(k.kinder) AS kleinste, MAX(k.kinder) AS groesste
FROM ( … Fenster B + Überhang … ) k
GROUP BY breite ORDER BY kleinste;
```

**Ergebnis** — 31.060 Eltern insgesamt:

| Breite | Eltern | kleinste | größte |
|---|---:|---:|---:|
| 1 | 27.021 | 1 | 1 |
| 2–5 | 2.527 | 2 | 5 |
| 6–20 | 923 | 6 | 20 |
| 21–50 | 323 | 21 | 50 |
| 51–100 | 135 | 51 | 100 |
| 101–300 | 111 | 101 | 266 |
| **über 300** | **20** | **313** | **3.048** |

87,0 Prozent der Eltern haben genau **ein** Kind. Aber 266 Eltern (0,86 Prozent) haben mehr als 50,
und der größte hat **3.048**.

**Laufzeit:** **1,581 s**, beste von fünf.

## M24‑3 Dasselbe je Mandant (Regel L7)

**Ergebnis** (Fenster B + Überhang):

| `MandantID` | Status des Elternteils | Elternzeilen | Kinder gesamt | Maximum Kinder |
|---|---|---:|---:|---:|
| `NEXANS` | `SPLITTED` | 27.644 | 100.603 | **3.048** |
| `IBIS` | **`FINISHED`** | 1.714 | 1.736 | 2 |
| `NEXANS` | `FINISHED` | 704 | 750 | 9 |
| `SUTTONS` | `SPLITTED` | 640 | 1.248 | 10 |
| `NEXANS` | `COMMIT_RECEIVED` | 166 | 166 | 1 |
| `NEXANS` | `EERP_RECEIVED` | 150 | 150 | 1 |
| `IBISGUS` | **`FINISHED`** | 22 | 22 | 1 |
| `ZAST` | **`FINISHED`** | 20 | 263 | 33 |

**Regel L7 ist erfüllt:** fünf Mandanten, darunter drei kleine (`SUTTONS`, `IBISGUS`, `ZAST`).

> **Das ist der Befund, den die Messung als „den interessantesten der ganzen Runde" vorweggenommen
> hat, falls er einträte — und er ist eingetreten.** M12 hat festgestellt, dass `IBIS`, `IBISGUS` und
> `ZAST` über den gesamten Bestand **nicht eine einzige** Zwischenschritt-Zeile haben. Sie tauchen
> hier trotzdem auf, mit zusammen **1.756** Elternzeilen und Ketten bis zu 33 Kindern (`ZAST`).
> **Verkettung läuft also nicht über `SPLITTED`/`MERGED`.** Ein Mandant ohne einen einzigen
> Zwischenschritt kann verkettete Nachrichten haben — bei `IBIS` sind es mehr Elternzeilen als bei
> `SUTTONS`, das den Chip sieht.

**Laufzeit:** **2,003 s**, beste von fünf.

### Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Eltern tragen überwiegend `SPLITTED`/`MERGED`, Kinder `FINISHED` | **ja** (Eltern `SPLITTED`; `MERGED` gar nicht) | **Der Screenshot ist der Normalfall.** Die Liste blendet per Vorgabe die Zeile aus, die der Nutzer wiedererkennt, und zeigt die technischen Fragmente. Die Vorgabe `zwischenschritte=false` ist **falsch begründet und in der Wirkung umgekehrt zur Absicht** — sie gehört gedreht oder ersetzt |
| Eltern tragen `FINISHED`, Kinder `SPLITTED` | nein | — |
| gemischt | **teilweise** | Bei drei Mandanten trägt der Elternteil `FINISHED`. Der Status entscheidet die Stellung in der Kette also **nicht allein**; die verlässliche Angabe ist `Source = 1` (E4) |
| `kinder_max` ≤ 20 | nein | — |
| **`kinder_max` > 50** | **ja (3.048)** | **Der Plan trägt nicht.** Er begrenzt die Tiefe gegen Endlosketten und die Breite nirgends. Nötig ist eine Breitengrenze plus ein Sprung in die **Liste**, gefiltert auf die Kette — keine aufklappbare 3.048-Zeilen-Liste im Panel |

---

# M25 — Der Nachfolger beim Merge, und welche Spalte die Kette trägt

**Frage.** Wohin zeigt `TargetMessageID` bei einer `MERGED`-Nachricht? Und sind `SourceMessageID` und
`TargetMessageID` zueinander konsistent, sodass Schritt 6 **eine** Spalte lesen kann statt beide?

## M25‑1 Ziel und Ablauf des Nachfolgers

**`EXPLAIN`** (Fenster B):

| id | select_type | table | type | possible_keys | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `v` | `range` | `TargetMessageIDIDX`, `MessageLastUpdateIDX`, `MessageStatusIDX`, `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdateIDX` | 5 | 409.756 | `Using index condition; Using where; Using temporary; Using filesort` |
| 1 | SIMPLE | `n` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | 1 | |
| 1 | SIMPLE | `s` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | 1 | `Using where` |

**Ergebnis** (Fenster B), zusammengefasst über alle Abläufe:

| Status Vorgänger | Status Nachfolger | Verweise | Nachfolgerzeilen | Grad | `Target` des Nachfolgers | `Source` des Nachfolgers |
|---|---|---:|---:|---:|---:|---:|
| `MERGED` | `EERP_RECEIVED` | 37.219 | 1.602 | **23,23** | 37.219 | 15 |
| `MERGED` | `SPLITTED` | 909 | 16 | **56,81** | 909 | 909 |
| `MERGED` | `COMMIT_RECEIVED` | 390 | 32 | 12,19 | 390 | 0 |
| `MERGED` | `FINISHED` | 110 | 19 | 5,79 | 110 | 0 |

**`SPLITTED` kommt als Vorgänger nicht vor** — kein einziger Verweis. Der Zusammenführungsgrad liegt
insgesamt bei **38.628 : 1.669 = 23,15 : 1**, also deutlich über 1 : 1, wie erwartet.

**Kein Verweis zeigt ins Leere:** Die Gegenprobe auf `TargetMessageID`-Werte ohne zugehörige Zeile
liefert ein leeres Ergebnis.

### Der Ablauf des Nachfolgers trägt **keinen** `_MERGED`-Anhang

```sql
CASE WHEN s.SOSName IS NULL          THEN 'kein SOS'
     WHEN s.SOSName LIKE '%\_MERGED' THEN 'endet auf _MERGED'
     WHEN s.SOSName LIKE '%MERGE%'   THEN 'enthaelt MERGE'
     ELSE                                 'sonstiger Name' END
```

| Gestalt | Verweise | Nachfolgerzeilen | verschiedene Abläufe |
|---|---:|---:|---:|
| endet auf `_MERGED` | **0** | 0 | 0 |
| enthält `MERGE` | **0** | 0 | 0 |
| sonstiger Name | 38.628 | 1.669 | **140** |

Die 140 Abläufe der Nachfolger heißen durchweg wie gewöhnliche ausgehende Abläufe — Gestalt
„Lieferabruf an `<Kunde>` (⟨Nummer⟩)", „LABS an `<Kunde>` (⟨Nummer⟩)", „Steyr JIT von `<Kunde>`
(EDIFACT)". **Der `_MERGED`-Anhang aus dem Produktions-Screenshot sitzt nicht auf `SOS.SOSName`.** Er
gehört zum Datei- beziehungsweise Nachrichtennamen und ist über die hier gelesenen Tabellen nicht
sichtbar.

Das ist folgenlos: Die Zuordnung läuft ohnehin über `TargetMessageID` und nicht über eine
Namenskonvention — **Regel Q4 kommt gar nicht erst zum Tragen.**

**Laufzeit:** **1,668 s**, beste von fünf.

## M25‑2 Sind die beiden Spalten symmetrisch?

**Ergebnis** (Fenster A):

| mit Nachfolger | Rückverweis stimmt | Nachfolger ohne Vorgänger | zeigt woanders hin |
|---:|---:|---:|---:|
| 432 | **0** | **432** | 0 |

> **Der Gegentest mit `COLLATE utf8mb4_bin` ist gelaufen** — die Zeile stand im Plan, weil derselbe
> Punkt am 06.08.2026 beim Statusfilter ein echter Fehler war. Ergebnis **identisch** (432 / 0 / 432).
> Die Sortierung spielt hier keine Rolle; der Rückverweis fehlt schlicht.

**Die Gegenrichtung ebenso** — trägt ein Elternteil, das über `SourceMessageID` gefunden wurde, einen
`TargetMessageID`?

| Status des Elternteils | Kinder | Elternteil zeigt auf dieses Kind | Elternteil ohne Nachfolger |
|---|---:|---:|---:|
| `SPLITTED` | 4.007 | **0** | **4.007** |
| `FINISHED` | 89 | **0** | **89** |

**Laufzeit:** **0,036 s**, beste von fünf.

### Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| `MERGED` → Nachfolger mit `_MERGED`-Ablauf und Endstatus, Grad deutlich über 1 : 1 | **halb** | Endstatus **ja** (`EERP_RECEIVED` = `QUITTIERT`, `message-status.md`), Grad **ja** (23,15 : 1), `_MERGED`-Ablauf **nein**. Die Konsequenz trägt trotzdem: **Die zusammengeführte Datei ist eine eigene, sichtbare Nachricht.** Der Kunde sieht sie bereits; ausgeblendet sind die Eingänge |
| `MERGED` hat **keinen** Nachfolger | nein | — Der Weg über die Namenskonvention und Regel Q4 wird nicht gebraucht |
| Symmetrie durchgängig | nein | — |
| Symmetrie lückenhaft | **nein — sie fehlt vollständig** | siehe Absatz unten |

**Wo die vorformulierte Zeile nicht passt.** Vorgesehen waren „durchgängig" und „lückenhaft".
Gemessen ist **null**: In 432 von 432 Fällen trägt der Nachfolger keinen Rückverweis, und in 4.096
von 4.096 Fällen trägt der Elternteil keinen Vorwärtsverweis. Die beiden Spalten sind nicht
teilweise redundant, sondern **vollständig disjunkt** — sie beschreiben zwei verschiedene
Beziehungen:

- **`SourceMessageID`** trägt die **Aufteilung**: Das Kind zeigt auf den Elternteil.
- **`TargetMessageID`** trägt die **Zusammenführung**: Der Eingang zeigt auf das Ergebnis.

Schritt 6 muss beide lesen. „Eine Spalte genügt" war nie eine Option; welche der beiden man wählte,
verlöre die Hälfte der Fälle. Die Machbarkeit hängt damit an M23‑1 — und die ist gegeben.

> ⚠️ **„Disjunkt" gilt für die Spalten, nicht für die Zeile** (nachgetragen am 10.08.2026 nach
> M28‑1c). Aus den Zahlen oben wurde in `datenmodell.md` §7 der Satz *„Kein Merge-Ergebnis trägt
> einen `SourceMessageID`"* abgeleitet. **Das ist ein Schluss zu weit gewesen:** Über Fenster B sind
> **33 `NEXANS`-Zeilen** zugleich Merge-Ergebnis und Split-Kind. Richtig bleibt, was hier gemessen
> ist — keine Zeile trägt **beide ID-Spalten** (M28‑1: `Kind + Eingang` = 0 in beiden Fenstern) —,
> und richtig bleibt die Folgerung, dass beide Spalten gelesen werden müssen. Falsch war nur die
> Verallgemeinerung von der Stichprobe dieses Fensters auf den Bestand.

---

# M26 — Wo in der Kette sitzen die BAM-Werte?

**Frage.** Findet die BAM-Suche aus Schritt 7 die Elternzeile, die Kinder, oder beide?

Zugriff **ausschließlich über `MessageID`** (Regeln L4/L5), Gestalt wie M11.

## M26‑1 BAM-Belegung je Status

**Ergebnis** (Fenster A):

| `MessageStatus` | Nachrichten | mit BAM | Anteil | Schnitt | Maximum |
|---|---:|---:|---:|---:|---:|
| `FINISHED` | 5.360 | 260 | **4,85 %** | 0,22 | 436 |
| `MERGED` | 432 | 0 | **0,00 %** | 0,00 | 0 |
| `SPLITTED` | 390 | 381 | **97,69 %** | 21,90 | 765 |
| `EERP_RECEIVED` | 65 | 55 | 84,62 % | 34,66 | 405 |
| `SUSPENDED` | 1 | 1 | 100,00 % | 9,00 | 9 |
| `ERROR_DUPLICATE` | 1 | 1 | 100,00 % | 15,00 | 15 |

**Laufzeit:** **0,152 s**, beste von fünf.

## M26‑1b Dasselbe nach Stellung in der Kette *(ergänzt, nicht im Plan)*

Die Aufschlüsselung nach Status beantwortet die Frage nur mittelbar, weil der Status die Stellung in
der Kette nicht bestimmt (M24‑3). Dieselbe Auswertung, gruppiert nach Stellung:

| Stellung | Nachrichten | mit BAM | Anteil | Schnitt | Maximum |
|---|---:|---:|---:|---:|---:|
| **Wurzel** (`Source = 1`) | 479 | 464 | **96,87 %** | 18,15 | 765 |
| **Merge-Eingang** (`TargetMessageID` belegt) | 432 | 0 | **0,00 %** | 0,00 | 0 |
| **Merge-Ergebnis** (`Target = 1`) | 6 | 6 | 100,00 % | **197,67** | 405 |
| **Kind** (`SourceMessageID` belegt) | 4.092 | 99 | **2,42 %** | 0,04 | 62 |
| ohne Verkettung | 1.240 | 129 | 10,40 % | 1,55 | 436 |

## M26‑2 Teilen Elternteil und Kind mindestens einen Wert?

**`EXPLAIN`** — die korrelierte Unterabfrage läuft wie vorgesehen zweimal über den `MessageBAM`-Index
(`ref` über `MessageBAM_MessageFK`, dann `eq_ref` über den Primärschlüssel), beide `Using index`:

| id | select_type | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `k` | `range` | `MessageLastUpdateIDX` | 5 | 11.812 | `Using index condition; Using where; Using temporary; Using filesort` |
| 1 | PRIMARY | `e` | `eq_ref` | `PRIMARY` | 146 | 1 | |
| 2 | DEPENDENT SUBQUERY | `be` | `ref` | `MessageBAM_MessageFK` | 146 | 4 | `Using index` |
| 2 | DEPENDENT SUBQUERY | `bk` | `eq_ref` | `PRIMARY` | 430 | 1 | `Using index` |

**Ergebnis** (Fenster A):

| Status des Elternteils | Paare | mit gemeinsamem Wert |
|---|---:|---:|
| `FINISHED` | 89 | **84** |
| `SPLITTED` | 4.007 | **3** |

**Gegenprobe — hat das Kind überhaupt BAM-Werte?** Das ist die Frage, ohne die die drei oben
missverstanden würden:

| Status des Elternteils | Paare | Kind hat BAM | Elternteil hat BAM |
|---|---:|---:|---:|
| `FINISHED` | 89 | 84 | 84 |
| `SPLITTED` | 4.007 | **18** | **3.990** |

Bei `SPLITTED` teilen Elternteil und Kind fast nie einen Wert — **nicht, weil die Werte verschieden
wären, sondern weil das Kind keine hat.** 3.990 von 4.007 Eltern tragen BAM, 18 von 4.007 Kindern.

**Laufzeit:** **2,391 s**, beste von fünf. Fenster A hat gereicht; eine Erweiterung auf Fenster B war
nicht nötig, weil das Ergebnis bereits eindeutig ist.

## M26‑3 Welche BAM-Typen sitzen auf welchem Ende, je Mandant

**Ergebnis** (Fenster A, alle Zeilen):

| `MandantID` | Status | Typ | Beschreibung | Nachrichten | Werte |
|---|---|---:|---|---:|---:|
| `IBIS` | `FINISHED` | 0 | Bestellnummer | 136 | 216 |
| `IBIS` | `FINISHED` | 1 | Auftragsnummer | 40 | 240 |
| `IBIS` | `FINISHED` | 2 | Lieferscheinnummer | 10 | 43 |
| `IBIS` | `FINISHED` | 3 | Rechnungsnummer | 5 | 52 |
| `IBISGUS` | `FINISHED` | 0 | Bestellnummer | 73 | 76 |
| `IBISGUS` | `FINISHED` | 1 | Auftragsnummer | 8 | 74 |
| `NEXANS` | `SPLITTED` | 9018 | Kundenmaterialnummer_K_SAP | 353 | 1.306 |
| `NEXANS` | `SPLITTED` | 9014 | Lieferantennummer beim Kunden_K_SAP | 353 | 354 |
| `NEXANS` | `SPLITTED` | 9015 | Kundenwerk_K_SAP | 353 | 371 |
| `NEXANS` | `SPLITTED` | 9020 | Lieferschein, Entnahme, PUS_K_SAP | 310 | 3.377 |
| `NEXANS` | `SPLITTED` | 9016 | Abladestelle_K_SAP | 259 | 289 |
| `NEXANS` | `SPLITTED` | 9017 | (JIT-) Abrufnummer_K_SAP | 228 | 644 |
| `NEXANS` | `SPLITTED` | 9019 | Bestellnummer vom Kunden_K_SAP | 207 | 840 |
| `NEXANS` | `SPLITTED` | 9032 | Sendercode_K_SAP | 146 | 146 |
| `NEXANS` | `SPLITTED` | 9033 | Empfaengercode_K_SAP | 146 | 146 |
| `NEXANS` | `SPLITTED` | 9022 | Gutschriftsanzeigen-Nummer_K_SAP | 125 | 125 |
| `NEXANS` | `SPLITTED` | 9023 | Übertragungsnummer Gutschrift_K_SAP | 125 | 125 |
| `NEXANS` | `SPLITTED` | 9039 | Daten-Sender-Nummer_L_SAP | 28 | 28 |
| `NEXANS` | `SPLITTED` | **9006** | **Lieferschein-Nr._L_SAP** | **28** | 75 |
| `NEXANS` | `SPLITTED` | 9000 | Abladestelle_L_SAP | 28 | 29 |
| `NEXANS` | `SPLITTED` | 9007 | Transport-Nummer_L_SAP | 28 | 29 |
| `NEXANS` | `SPLITTED` | 9037 | Packmittelnummer Kunde_L_SAP | 28 | 33 |
| `NEXANS` | `SPLITTED` | 9003 | Material-Nr. beim Lieferanten_L_SAP | 28 | 246 |
| `NEXANS` | `SPLITTED` | 9004 | Unsere Material-Nr._L_SAP | 28 | 246 |
| `NEXANS` | `SPLITTED` | 9034 | Bestellnummer_L_SAP | 24 | 90 |
| `NEXANS` | `SPLITTED` | 9005 | Werk_L_SAP | 12 | 12 |
| `NEXANS` | `SPLITTED` | 9038 | Packmittelnummer Lieferant_L_SAP | 12 | 23 |
| `NEXANS` | `SPLITTED` | 9036 | Lagerort Kunde_L_SAP | 7 | 8 |
| `NEXANS` | `FINISHED` | 9033 / 9032 | Empfaenger-/Sendercode_K_SAP | je 3 | je 3 |
| `NEXANS` | `FINISHED` | 9028 / 9029 / 9027 | Material-Nr. Kunde/Lieferant, Bestellnummer_FORS | je 1 | je 144 |
| `NEXANS` | `FINISHED` | 9030 / 9031 / 9025 | Sender-/Empf_Ident, Abladestelle_FORS | je 1 | 1 / 1 / 2 |
| `SUTTONS` | `FINISHED` | 2000 | OrderNumber | 13 | 13 |
| `SUTTONS` | `FINISHED` | 2001 | VendorReference | 13 | 13 |

**`MERGED` kommt in dieser Tabelle nicht vor** — die 432 Merge-Eingänge tragen keinen einzigen
BAM-Wert.

Die Aufteilung fällt mit der Mandantengrenze zusammen: Bei `NEXANS` sitzen die Werte auf `SPLITTED`,
bei `IBIS`, `IBISGUS` und `SUTTONS` auf `FINISHED`. Das ist dieselbe Trennlinie wie in M24‑3.

**Laufzeit:** **0,165 s**, beste von fünf.

## E1 Die kuratierten Listenspalten *(ergänzt, nicht im Plan)*

Die Nachrichtenliste zeigt je Mandant zwei BAM-Werte als Spalten; für `NEXANS` sind das kuratiert
**9006** (Lieferschein-Nr._L_SAP) und **9001** (Abrufnummer_L_SAP), siehe `V4__bam_spalte.sql` und
[`datenzugriff.md`](datenzugriff.md) §5. Die Frage, die M26 aufwirft und die außerhalb von Schritt 7
liegt: **Wie voll sind diese beiden Spalten auf den Zeilen, die die Liste heute tatsächlich zeigt?**

**Ergebnis** (`NEXANS`, Fenster A):

| Zeilen der Liste | Nachrichten | mit 9006 | mit 9001 | mit irgendeinem BAM |
|---|---:|---:|---:|---:|
| **ausgeblendet** (`SPLITTED`/`MERGED`) | 815 | **28** | 0 | **381** (46,7 %) |
| **sichtbar** (Vorgabe) | 4.228 | **0** | 6 | **23** (0,54 %) |

Auf den 4.228 sichtbaren Zeilen ist die Spalte „Lieferschein-Nr." **null Mal** befüllt und die Spalte
„Abrufnummer" sechs Mal. **Beide kuratierten Spalten sind auf der Vorgabe-Ansicht praktisch leer** —
und die Werte, die sie zeigen sollen, stehen auf den ausgeblendeten Zeilen.

> Das ist kein Befund über Schritt 7, sondern über den **heutigen** Zustand von Schritt 4. Er gehört
> deshalb hierhin und nicht in die Vorarbeit für die Suche.

### Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Die Belegnummer sitzt **nur auf dem Elternteil** | **ja** (96,9 % gegen 2,4 %) | Die heutige Vorgabe ist kein Anzeigeproblem, sondern ein **Suchfehler**: Der Treffer ist da, die Zeile ist ausgeblendet. Die Vorgabe muss fallen, bevor Schritt 7 gebaut wird — und E1 zeigt, dass sie schon heute die Listenspalten leert |
| **nur auf den Kindern** | nein | — |
| **auf beiden**, gemeinsame Werte hoch | nein (3 von 4.007 Paaren) | — |
| Schnitt und Maximum wie in M11 (bis 3.035 Werte je Nachricht) | **ja, abgeschwächt** | Maximum hier **765** je Nachricht (Wurzel) und **405** beim Merge-Ergebnis, Schnitt dort **197,67**. Ein BAM-Block in der Detailansicht braucht dieselbe harte Deckelung wie die Suche |

**Wo die vorformulierte Zeile nicht passt.** Sie kennt nur „Elternteil", „Kinder" und „beide". Der
**Merge-Eingang** ist eine vierte Stelle und verhält sich anders als alle drei: `MERGED` trägt in
Fenster A **null** BAM-Werte. Daraus folgt eine Trennung, die die Sichtbarkeitsfrage in zwei
zerlegt:

- **Beim Split kostet das Ausblenden Treffer.** Die Wurzel trägt die Belegnummer, das Kind nicht.
- **Beim Merge kostet es keine.** Der Eingang trägt keine Belegnummer; das Ergebnis trägt sie und ist
  ohnehin sichtbar (`EERP_RECEIVED`).

Die beiden Status haben damit **entgegengesetzte** Folgen für dieselbe Vorgabe — genau das, was der
Nachtrag „ein gemeinsamer Eimer verschluckt beides" vorweggenommen hat, jetzt mit Zahlen.

---

# M27 — Mandantengrenze bei Prozesswechseln

**Frage.** M10 hat **9.101 Zeilen** gefunden, in denen `SOS.ProcessID` und `Message.ProcessID`
auseinanderfallen. Führen Quell- und Zielprozess über `ProjectMandant` zum **selben** Mandanten?

**Ohne Zeitfenster mit Absicht:** Die Bedingung `s.ProcessID <> m.ProcessID` erzwingt ohnehin einen
vollen Durchlauf. Einmalige Erhebung, **kein** Statement der Anwendung.

**`EXPLAIN`** — voller Durchlauf über `Message`, alle Stammdaten als `eq_ref`/`ref`:

| id | table | type | key | rows | Extra |
|---|---|---|---|---:|---|
| 1 | `m` | **`ALL`** | — | **3.560.486** | `Using where; Using temporary; Using filesort` |
| 1 | `p_m` | `eq_ref` | `PRIMARY` | 1 | `Using where` |
| 1 | `pm_m` | `ref` | `PRIMARY` | 1 | `Using index` |
| 1 | `s` | `eq_ref` | `PRIMARY` | 1 | `Using where` |
| 1 | `p_s` | `eq_ref` | `PRIMARY` | 1 | `Using where` |
| 1 | `pm_s` | `ref` | `PRIMARY` | 1 | `Using index` |

**Ergebnis**

| Mandant der Nachricht | Mandant des Ablaufs | Zeilen |
|---|---|---:|
| `IBIS` | `IBIS` | 63 |
| `NEXANS` | `NEXANS` | 9.038 |

**9.101 Zeilen, zwei Gruppen, beide Mandanten identisch.** Die Summe deckt sich exakt mit M10.

**Laufzeit:** **29,210 s**, beste von drei.

### Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| beide Mandanten immer gleich | **ja** | Die Kette bleibt innerhalb der Grenze; Schritt 6 braucht **keine** Sonderbehandlung. Der Mandantenfilter der Kette ist trotzdem zu setzen (Regel M5) — er ist die Zusicherung, nicht die Beobachtung |
| verschieden, auch in Einzelfällen | nein | — Die Entscheidung zwischen „abschneiden", „fremde Glieder unkenntlich zeigen" und „Kette verweigern" ist **nicht** zu treffen |

---

# Ergänzende Messungen *(nicht im Plan)*

Drei Fragen, die während der Erhebung entstanden sind und ohne die die obigen Zahlen falsch gelesen
würden. Sie sind hier getrennt geführt, damit die Trennlinie zwischen geplanter und nachgezogener
Messung sichtbar bleibt. E1 steht bei M26, weil es dort hingehört.

## E2 Tiefe — hat das Merge-Ergebnis selbst Kinder?

**Ergebnis** (Fenster B, Ziele aller `MERGED`-Zeilen):

| Status des Merge-Ergebnisses | Ergebniszeilen | davon mit Kindern | Kinder gesamt |
|---|---:|---:|---:|
| `EERP_RECEIVED` | 1.602 | 9 | 9 |
| `COMMIT_RECEIVED` | 32 | 0 | 0 |
| `FINISHED` | 19 | 0 | 0 |
| **`SPLITTED`** | **16** | **16** | **909** |

**Die Kette ist mehrstufig.** In 16 Fällen ist das Ergebnis einer Zusammenführung selbst eine Wurzel,
die wieder aufgeteilt wird: `MERGED`-Eingänge → `SPLITTED`-Ergebnis → 909 Kinder. Zusammen mit den
drei `SPLITTED`-Kindern eines `SPLITTED`-Elternteils aus M24‑1 sind damit **mindestens vier Ebenen**
belegt.

## E3 Tiefe — trägt die Wurzel eines Splits selbst einen Vorgänger?

**Ergebnis** (Fenster B + Überhang):

| Status der Wurzel | Wurzeln | davon mit Vorgänger | davon mit Nachfolger |
|---|---:|---:|---:|
| `SPLITTED` | 28.284 | 263 (0,9 %) | 0 |
| `FINISHED` | 2.460 | 27 (1,1 %) | 0 |
| `COMMIT_RECEIVED` | 166 | **166 (100 %)** | 0 |
| `EERP_RECEIVED` | 150 | 0 | 0 |

Die ganz überwiegende Mehrheit der Wurzeln ist auch wirklich eine — **außer bei
`COMMIT_RECEIVED`**, wo jede der 166 Zeilen selbst ein Kind ist. Eine Tiefenbegrenzung ist nötig,
aber sie greift selten.

## E4 Decken sich die Flags mit der tatsächlichen Verkettung?

Die Frage, die M23 offen gelassen hat. Beide Prüfungen über Fenster A, die Gegenseite **ohne**
Zeitfenster (sonst zählte man nur die Kinder, die zufällig am selben Tag entstanden sind):

| `Source` | Zeilen | davon mit mindestens einem Kind |
|---:|---:|---:|
| 0 | 5.770 | **0** |
| 1 | 479 | **479** |

| `Target` | Zeilen | davon von einer `MERGED`-Zeile angezeigt |
|---:|---:|---:|
| 0 | 6.243 | **0** |
| 1 | 6 | **6** |

**Exakte Deckung in beiden Richtungen, ohne eine einzige Abweichung.** Die Flags sind kein zweiter,
konkurrierender Wahrheitsanspruch, sondern der **Rückwärtsindex** der ID-Spalten:

| Frage | Antwort steht in | Kosten |
|---|---|---|
| Wer ist mein Elternteil? | `SourceMessageID` | ein Primärschlüsselzugriff |
| Wohin bin ich zusammengeführt worden? | `TargetMessageID` | ein Primärschlüsselzugriff |
| **Habe ich Kinder?** | **`Source`** | **auf der Zeile, kein Zugriff** |
| **Bin ich ein Merge-Ergebnis?** | **`Target`** | **auf der Zeile, kein Zugriff** |

Für Schritt 6 heißt das: Ob eine Nachricht überhaupt eine Kette hat, steht auf der Zeile selbst. Es
braucht **keine** Abfrage, um zu entscheiden, ob ein Ketten-Bereich im Detailpanel erscheint.

**Laufzeit:** **0,223 s**, beste von fünf (6.249 Zeilen mit je einer korrelierten `EXISTS`-Prüfung).

> **Verweis, nachgetragen am 11.08.2026 — an der Messung ändert sich nichts.** Aus E4 ist in
> [`verkettung.md`](verkettung.md) §11 ein **offener Punkt** geworden: Die exakte Deckung ist auf
> einem **Standbild** gemessen, in dem alle Events längst nachgezogen sind. Zwischen „Kinder
> existieren" und „Flag gesetzt" liegt ein Nachlauffenster, das eine Testkopie strukturell nicht
> abbilden kann — und in dem das Detailpanel gar nichts zeigt, weil es bei leerem `rollen` nicht
> einmal fragt (§8.2 dort). Dieselbe Argumentation wie bei `RUNNING`
> ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1). Zu beachten ist außerdem, dass die
> `Target`-Seite oben auf **sechs** positiven Beobachtungen steht: belastbar für die Richtung, dünn
> für eine Zusicherung. **Die Zahlen und Schlüsse von E4 bleiben unverändert stehen.**

## E5 Kosten der Auflösung in Gegenrichtung

```sql
EXPLAIN SELECT MessageID, MessageStatus, MessageLastUpdate
FROM Message WHERE SourceMessageID = ? ORDER BY MessageLastUpdate LIMIT 51;
```

| id | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---:|---|
| 1 | `Message` | `ref` | `SourceMessageIDIDX` | `SourceMessageIDIDX` | 147 | `const` | 1 | `Using index condition; Using where; Using filesort` |

Die Kinder einer Wurzel kosten **einen Indexzugriff**. `Using filesort` betrifft nur die gefundenen
Kinder (bis zur Breitengrenze), nicht die Tabelle.

---

# M28 — Rollenverteilung, Kandidatenprädikate und der BAM-Typ je Rolle

**Block 2 derselben Runde, erhoben am 10.08.2026**, Rahmen unverändert (siehe §0; erneut geprüft:
`@@global.read_only` → `1`, Serverzeit `2026-08-10 13:50:12`).

**Anlass.** Im Sparring zu Schritt 6 sind zwei Entscheidungen gefallen — die Sichtbarkeit hängt
künftig an der **Stellung** statt am `MessageStatus`, und die Rolle wird als **Menge je Zeile**
modelliert statt als Aufzählungswert. Beide stützten sich auf Doppelrollen, die in E2 und E3 an je
**einer** Stelle gefunden wurden, nicht in der Fläche. M28 misst die Fläche.

Vier Fragen:

1. Wie oft treten Doppelrollen tatsächlich auf?
2. Wie viele Zeilen blendet jedes Kandidatenprädikat aus, je Mandant?
3. Kann eine **einzelne Zeile** beide ID-Spalten tragen? (M25‑2 hat die **Symmetrie einer
   Beziehung** gemessen, nicht das.)
4. Welcher BAM-Typ säße auf den Zeilen, die eine geänderte Vorgabe zeigt?

## M28‑1 Rollenverteilung und Kandidatenprädikate

Die beiden Kandidatenprädikate:

| | Prädikat | Gedanke dahinter |
|---|---|---|
| **P1** | `SourceMessageID IS NOT NULL OR TargetMessageID IS NOT NULL` | „Die Zeile hat einen Vorgänger oder ist in etwas anderes geflossen" |
| **P2** | dasselbe **und** `Source = 0` | wie P1, aber eine Zeile, die selbst Wurzel ist, bleibt sichtbar |
| heute | `MessageStatus IN ('SPLITTED','MERGED')` | die Vorgabe aus Schritt 4 |

**`EXPLAIN`** (Fenster B) — **der Tripwire hat nicht ausgelöst:** `m` kommt als `range` über
`MessageLastUpdateIDX`, wie in M12 (b) gemessen, nicht als `ALL`:

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---:|---|
| 1 | SIMPLE | `m` | **`range`** | `ProejctIDIDX`, `Message_ProcessFK`, `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdateIDX` | 5 | NULL | 409.756 | `Using index condition; Using where; Using temporary; Using filesort` |
| 1 | SIMPLE | `p` | `eq_ref` | `PRIMARY`, `Process_ProjectFK` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | SIMPLE | `pm` | `ref` | `PRIMARY` | `PRIMARY` | 146 | `p.ProjectID` | 1 | `Using index` |

### Ergebnis (a) — Fenster B, 214.330 Zeilen

Rollen (eine Zeile erscheint in mehreren Spalten, wenn sie mehrere Rollen trägt):

| `MandantID` | Zeilen | Split-Wurzel | Split-Kind | Merge-Eingang | Merge-Ergebnis | ohne Kette |
|---|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 180.251 | 28.524 | 101.270 | 38.628 | 1.669 | 10.652 |
| `SUTTONS` | 21.516 | 639 | 1.247 | 0 | 0 | 19.630 |
| `VOTG` | 6.104 | 0 | 0 | 0 | 0 | 6.104 |
| `IBIS` | 4.331 | 1.714 | 36 | 0 | 0 | 2.581 |
| `IBISGUS` | 1.722 | 22 | **1.722** | 0 | 0 | **0** |
| `ZAST` | 283 | 20 | 263 | 0 | 0 | **0** |
| `WOC` | 118 | 0 | 0 | 0 | 0 | 118 |
| `SYSTEM` | 5 | 0 | 0 | 0 | 0 | 5 |

Prädikate — was jedes ausblenden würde:

| `MandantID` | Zeilen | heute | Anteil | P1 | Anteil | P2 | Anteil | P1 − P2 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 180.251 | 66.133 | 36,69 % | 139.898 | **77,61 %** | 139.464 | 77,37 % | 434 |
| `SUTTONS` | 21.516 | 639 | 2,97 % | 1.247 | 5,80 % | 1.247 | 5,80 % | 0 |
| `VOTG` | 6.104 | 0 | 0 % | 0 | 0 % | 0 | 0 % | 0 |
| `IBIS` | 4.331 | **0** | 0 % | 36 | 0,83 % | 36 | 0,83 % | 0 |
| `IBISGUS` | 1.722 | **0** | 0 % | 1.722 | **100,00 %** | 1.700 | 98,72 % | 22 |
| `ZAST` | 283 | **0** | 0 % | 263 | **92,93 %** | 263 | 92,93 % | 0 |
| `WOC` | 118 | 0 | 0 % | 0 | 0 % | 0 | 0 % | 0 |
| `SYSTEM` | 5 | 0 | 0 % | 0 | 0 % | 0 | 0 % | 0 |
| **gesamt** | **214.330** | **66.772** | **31,15 %** | **143.166** | **66,80 %** | **142.710** | 66,58 % | 456 |

Doppelrollen:

| `MandantID` | Wurzel + Kind | Ergebnis + Wurzel | Ergebnis + Eingang | **Kind + Eingang** |
|---|---:|---:|---:|---:|
| `NEXANS` | 434 | 25 | 0 | **0** |
| `IBISGUS` | 22 | 0 | 0 | **0** |
| alle übrigen | 0 | 0 | 0 | **0** |

### Ergebnis (b) — Fenster A, 6.249 Zeilen

| `MandantID` | Zeilen | Wurzel | Kind | Eingang | Ergebnis | ohne Kette | heute | P1 | P2 | Doppelrollen |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | 5.043 | 389 | 3.999 | 432 | 6 | 220 | 815 (16,16 %) | 4.431 (87,86 %) | 4.428 | 3 |
| `SUTTONS` | 685 | 7 | 13 | 0 | 0 | 665 | 7 (1,02 %) | 13 (1,90 %) | 13 | 0 |
| `IBIS` | 233 | 82 | 3 | 0 | 0 | 148 | 0 | 3 (1,29 %) | 3 | 0 |
| `VOTG` | 206 | 0 | 0 | 0 | 0 | 206 | 0 | 0 | 0 | 0 |
| `IBISGUS` | 81 | 1 | 81 | 0 | 0 | 0 | 0 | 81 (100 %) | 80 | 1 |
| `WOC` | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 |
| **gesamt** | **6.249** | 479 | 4.096 | 432 | 6 | 1.240 | **822 (13,15 %)** | **4.528 (72,46 %)** | 4.524 | **4** |

Die Spalte „Wurzel" (479) und „Kind" (4.096) decken sich mit E4 beziehungsweise M25‑2 — derselbe
Bestand, anders geschnitten.

**Laufzeit:** **3,744 s** (Fenster B) · **0,109 s** (Fenster A), je beste von fünf nach einem
Aufwärmlauf.

## M28‑1c Rollenzahl je Zeile *(ergänzt, nicht im Plan)*

**Warum das nachgezogen wurde.** Die vier im Plan erhobenen Doppelrollen sind nur **vier der sechs**
möglichen Paare; `Wurzel + Eingang` und `Kind + Ergebnis` fehlen. Bei `NEXANS` gingen die
Rollenspalten deshalb um **33 Zeilen** nicht auf, und Frage 1 wäre nur halb beantwortet gewesen. Die
Rollenzahl je Zeile beantwortet sie vollständig und ohne Inklusions-Exklusions-Rechnung:

| `MandantID` | 0 Rollen | 1 Rolle | **2 Rollen** | 3 oder 4 | Anteil mit ≥ 2 |
|---|---:|---:|---:|---:|---:|
| `NEXANS` | 10.652 | 169.107 | **492** | **0** | **0,273 %** |
| `SUTTONS` | 19.630 | 1.886 | 0 | 0 | 0 % |
| `VOTG` | 6.104 | 0 | 0 | 0 | 0 % |
| `IBIS` | 2.581 | 1.750 | 0 | 0 | 0 % |
| `IBISGUS` | 0 | 1.700 | **22** | **0** | **1,278 %** |
| `ZAST` | 0 | 283 | 0 | 0 | 0 % |
| `WOC` | 118 | 0 | 0 | 0 | 0 % |
| `SYSTEM` | 5 | 0 | 0 | 0 | 0 % |
| **gesamt** | 39.090 | 174.726 | **514** | **0** | **0,240 %** |

**Keine Zeile trägt drei oder vier Rollen** — in keinem der beiden Fenster.

Die beiden im Plan nicht erhobenen Paare, über Fenster B:

| `MandantID` | Wurzel + Eingang | **Kind + Ergebnis** |
|---|---:|---:|
| `NEXANS` | **0** | **33** |
| alle übrigen | 0 | 0 |

Damit geht die Rechnung auf: 434 + 25 + 0 + 0 + 0 + 33 = **492** — genau die Zahl aus der
Histogramm-Spalte. Über Fenster A: `NEXANS` 3, `IBISGUS` 1, zusammen **4 von 6.249 = 0,064 %**.

> **Die 33 sind der wichtigste Einzelbefund dieses Blocks** — nicht wegen ihrer Größe, sondern weil
> sie einen Satz widerlegen, der aus der ersten Runde stammt. Siehe „Was daraus folgt".

### Was daraus folgt — M28‑1

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Doppelrollen zusammen **< 0,1 %** bei jedem Mandanten | **nein** | — (`NEXANS` 0,273 %, `IBISGUS` 1,278 %) |
| Doppelrollen **≥ 1 %** bei mindestens einem Mandanten | **ja** (`IBISGUS` 1,278 %) | Die Menge ist Pflicht; ein Aufzählungswert gäbe diesen Zeilen still ein falsches Etikett. **Entscheidung 2b bleibt stehen** |
| **`kind_und_eingang` > 0** | **nein** (0 in beiden Fenstern) | — |
| `kind_und_eingang` = 0 über beide Fenster | **ja** | Eine Zeile trägt **nie** beide ID-Spalten. Die Aussage über die **Spalten** trägt; die Aussage über die **Zeile** in `datenmodell.md` §7 trägt nicht — siehe Absatz unten |
| **P1 und P2 unterscheiden sich um < 0,1 %** | **nein** | — |
| P1 und P2 unterscheiden sich messbar | **ja** (gesamt 0,213 %, `IBISGUS` 1,278 %) | Die Wahl ist eine fachliche Abwägung und gehört ins Sparring, **nicht in diese Messung**. Hier steht nur die Zahl |
| **P1 blendet weniger aus als heute** | **nein** | — |
| **P1 blendet mehr aus** | **ja, deutlich** | Die neue Vorgabe versteckt **mehr als das Doppelte** (66,80 % gegen 31,15 %; bei `NEXANS` 77,61 % gegen 36,69 %). Frage 1 braucht eine ausdrückliche Begründung, warum das trotzdem besser ist — **die Zahl allein trägt sie nicht** |
| `IBIS`, `IBISGUS`, `ZAST` haben Kettenzeilen bei `heute_ausgeblendet = 0` | **ja** | Bestätigt M24‑3 über das größere Fenster: `IBIS` 1.750, `IBISGUS` 1.722, `ZAST` 283 Kettenzeilen — bei **null** heute ausgeblendeten |

**Die Zahl, die im Sparring nicht übersehen werden darf.** Bei `IBISGUS` blendet P1 **100 Prozent**
der Zeilen aus (1.722 von 1.722; `ohne_kette` ist dort null), bei `ZAST` **92,93 Prozent**. Für diese
beiden Mandanten wäre die Vorgabe-Ansicht **leer beziehungsweise fast leer** — heute zeigt sie ihnen
jede Zeile. Das ist keine Deutung, sondern die unmittelbare Folge der Zahlen, und es ist der Punkt,
an dem die Begründung ansetzen muss.

**Wo die vorformulierte Zeile nicht passt — und was sie an einer früheren Aussage aufdeckt.** Die
Tabelle fragt nach `kind_und_eingang` (beide **ID-Spalten** auf einer Zeile). Das ist null. Aber das
nachgezogene Paar **`kind_und_ergebnis` ist 33** — und damit ist ein Satz falsch, der in der ersten
Runde aus M25‑2 in [`datenmodell.md`](datenmodell.md) §7 geschrieben wurde:

> *„Kein Merge-Ergebnis trägt einen `SourceMessageID`, keine Split-Wurzel einen `TargetMessageID` —
> in 4.528 geprüften Paaren nicht ein einziges Mal."*

Die zweite Hälfte trägt (`wurzel_und_eingang` = 0). **Die erste nicht:** 33 `NEXANS`-Zeilen sind
zugleich Merge-Ergebnis (`Target = 1`) **und** Split-Kind (`SourceMessageID` belegt). M25‑2 hatte
recht für seine Stichprobe — die sechs Merge-Ergebnisse in Fenster A —, und die Verallgemeinerung
auf den Bestand war ein Schluss zu weit. Genau dafür ist diese Messung da. `datenmodell.md` §7 ist
entsprechend geschärft, datiert und ohne Überschreiben des alten Stands.

## M28‑2 Welcher BAM-Typ sitzt auf welcher Rolle, je Mandant

Neuauflage von M26‑3 über **Fenster B** und über die **Rolle** statt über den Status. M26‑3 lief über
Fenster A, und Fenster A ist für dieses Verhältnis nicht repräsentativ: Dort sind 16,16 % der
`NEXANS`-Zeilen nach heutiger Vorgabe ausgeblendet, über Fenster B 36,69 % (M28‑1).

**`HAVING nachrichten >= 10`** schneidet Rauschen ab; die Grenze steht hier, damit sie nicht
verschwiegen ist.

**`EXPLAIN`** — `MessageBAM` wird ausschließlich über `MessageID` erreicht (Regel L4/L5), als `ref`
über den Primärschlüssel und `Using index`:

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|
| 1 | `m` | `range` | `MessageLastUpdateIDX` | 5 | NULL | 409.756 | `Using index condition; Using where; Using temporary; Using filesort` |
| 1 | `p` | `eq_ref` | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| 1 | `pm` | `ref` | `PRIMARY` | 146 | `p.ProjectID` | 1 | `Using index` |
| 1 | `bam` | `ref` | `PRIMARY` | 146 | `m.MessageID` | 8 | `Using index` |
| 1 | `bt` | `eq_ref` | `PRIMARY` | 2 | `bam.MessageBAMType` | 1 | |

### Ergebnis — `NEXANS` (28.524 Wurzeln, 1.669 Merge-Ergebnisse, 101.270 Kinder)

Die Spalte **„je Wurzel"** ist `auf_wurzeln` geteilt durch `split_wurzel` desselben Mandanten aus
M28‑1 — die Quote, um die es geht.

| Typ | Beschreibung | auf Wurzeln | **je Wurzel** | auf Kindern | auf Merge-Ergebnissen | **je Ergebnis** |
|---:|---|---:|---:|---:|---:|---:|
| **9018** | Kundenmaterialnummer_K_SAP | 26.315 | **92,26 %** | 627 | 69 | 4,13 % |
| 9014 | Lieferantennummer beim Kunden_K_SAP | 26.307 | 92,23 % | 602 | 60 | 3,59 % |
| 9015 | Kundenwerk_K_SAP | 26.300 | 92,20 % | 628 | 69 | 4,13 % |
| 9020 | Lieferschein, Entnahme, PUS_K_SAP | 24.333 | 85,31 % | 725 | 53 | 3,18 % |
| 9016 | Abladestelle_K_SAP | 23.818 | 83,50 % | 296 | 63 | 3,77 % |
| 9032 / 9033 | Sender-/Empfaengercode_K_SAP | je 13.738 | 48,16 % | je 680 | je 40 | 2,40 % |
| 9019 | Bestellnummer vom Kunden_K_SAP | 13.190 | 46,24 % | 627 | 69 | 4,13 % |
| 9017 | (JIT-) Abrufnummer_K_SAP | 13.118 | 45,99 % | 52 | 16 | 0,96 % |
| 9022 / 9023 | Gutschrift_K_SAP (zwei Typen) | je 12.419 | 43,54 % | 0 | 0 | 0 % |
| 9003 | Material-Nr. beim Lieferanten_L_SAP | 4.798 | 16,82 % | 0 | **1.600** | **95,87 %** |
| 9004 | Unsere Material-Nr._L_SAP | 4.792 | 16,80 % | 0 | **1.600** | **95,87 %** |
| 9000 | Abladestelle_L_SAP | 4.791 | 16,80 % | 0 | **1.600** | **95,87 %** |
| 9005 | Werk_L_SAP | 4.612 | 16,17 % | 0 | **1.600** | **95,87 %** |
| 9002 | Lieferplannummer_L_SAP | 3.362 | 11,79 % | 0 | **1.600** | **95,87 %** |
| **9001** | **Abrufnummer_L_SAP** *(kuratiert, Position 2)* | 3.362 | **11,79 %** | 0 | **1.600** | **95,87 %** |
| **9006** | **Lieferschein-Nr._L_SAP** *(kuratiert, Position 1)* | 1.432 | **5,02 %** | 0 | 0 | 0 % |
| 9039 / 9007 | Daten-Sender-Nummer / Transport-Nummer_L_SAP | je 1.432 | 5,02 % | 0 | 0 | 0 % |
| 9034 | Bestellnummer_L_SAP | 1.251 | 4,39 % | 0 | 0 | 0 % |
| 9037 | Packmittelnummer Kunde_L_SAP | 1.227 | 4,30 % | 0 | 0 | 0 % |
| 9038 | Packmittelnummer Lieferant_L_SAP | 1.156 | 4,05 % | 0 | 0 | 0 % |
| 9036 | Lagerort Kunde_L_SAP | 932 | 3,27 % | 0 | 0 | 0 % |
| 9021 | Transportnummer_K_SAP | 454 | 1,59 % | 360 | 0 | 0 % |
| 9024 | Rechnungsnummer_K_SAP | 9 | 0,03 % | 199 | 53 | 3,18 % |
| 9025 / 9027–9031 | sechs Typen `_FORS` | je 5 | 0,02 % | je 5 | 0 | 0 % |
| 9008–9013 | sechs Typen `_L_SAP` | 0 | 0,00 % | 0 | 0 | 0 % |

**Der bestbelegte Typ auf `NEXANS`-Wurzeln ist 9018 mit 92,26 Prozent.** Der bestbelegte Typ auf
Merge-Ergebnissen ist eine Gruppe von sechs `_L_SAP`-Typen mit je **95,87 Prozent**. Die höchste
Quote **auf Kindern** liegt bei 725 von 101.270 = **0,72 %** (Typ 9020).

### Ergebnis — die übrigen Mandanten

| `MandantID` | Typ | Beschreibung | auf Wurzeln | **je Wurzel** | auf Kindern | **je Kind** |
|---|---:|---|---:|---:|---:|---:|
| `IBIS` (1.714 Wurzeln, 36 Kinder) | **0** | Bestellnummer | 1.587 | **92,59 %** | 36 | 100 % |
| | 1 | Auftragsnummer | 127 | 7,41 % | 0 | 0 % |
| | 2 / 3 | Liefer-/Rechnungsnummer | 0 | 0 % | 0 | 0 % |
| `IBISGUS` (22 Wurzeln, 1.722 Kinder) | **0** | Bestellnummer | 22 | **100 %** | 1.595 | **92,63 %** |
| | 1 | Auftragsnummer | 0 | 0 % | 127 | 7,38 % |
| `SUTTONS` (639 Wurzeln, 1.247 Kinder) | 2000 | OrderNumber | **0** | **0 %** | 1.247 | **100 %** |
| | 2001 | VendorReference | **0** | **0 %** | 1.244 | 99,76 % |
| `ZAST` (20 Wurzeln, 263 Kinder) | 3 | Rechnungsnummer | **0** | **0 %** | 263 | **100 %** |
| `VOTG` (keine Kettenzeile) | 2002 | InvoiceNumber VTG | — | — | — | — |
| `WOC` (keine Kettenzeile) | 9014 | Lieferantennummer beim Kunden_K_SAP | — | — | — | — |

Bei `VOTG` (92 Nachrichten mit BAM) und `WOC` (88) liegen **alle** Zeilen außerhalb jeder Kette; für
sie hat die Frage „welcher Typ sitzt auf welcher Rolle" keinen Gegenstand.

**Laufzeit:** **12,682 s**, beste von drei nach einem Aufwärmlauf — die Kostenerwartung des Plans
(M11 kostete über Fenster B 12,7 s) ist auf **18 Millisekunden** genau getroffen. Die
60‑Sekunden‑Grenze war nie in Sicht, ein Rückfall auf Fenster A war **nicht** nötig, und die
Repräsentativitätsfrage bleibt damit **nicht** offen.

### Was daraus folgt — M28‑2

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| Es gibt je Mandant **einen Typ auf ≥ 80 % der Wurzeln** | **teilweise** — bei `NEXANS` (92,26 %), `IBIS` (92,59 %) und `IBISGUS` (100 %), **nicht** bei `SUTTONS` und `ZAST` (je 0 %) | Für `NEXANS` — den Mandanten, über den der Satz in [`nachrichtenliste.md`](nachrichtenliste.md) §6 spricht — ist er **widerlegt**. Frage 6 des Sparrings wird dort eine **Rücknahme**, keine Aufräumfrage. Siehe die Einschränkung unten |
| Bester Typ zwischen **30 und 80 %** | nein | — |
| Bester Typ **< 30 %** | nein (für Wurzeln) | — |
| Der beste Typ bei `NEXANS` ist **nicht** 9018 | **nein — er ist 9018** | siehe Absatz unten; die Zeile lässt sich nicht abhaken, wie sie gemeint war |
| Ein Mandant hat auf **keiner** Rolle einen tragfähigen Typ | **nein** | Jeder Mandant mit Kettenzeilen hat einen Typ über 90 % — nur nicht immer auf derselben Rolle. `VOTG` und `WOC` haben gar keine Rolle, was etwas anderes ist als „keinen tragfähigen Typ" |

**Die Einschränkung, ohne die die erste Zeile zu viel behauptet.** Widerlegt ist der Satz für die
**Wurzeln**. Für die Menge, die die Liste **heute** zeigt, bleibt er richtig: Auf den Kindern liegt
die beste Quote bei `NEXANS` bei 0,72 Prozent, und M11s 16,25 Prozent über alle Zeilen sind
unverändert gemessen. Der Satz war nie falsch — er war über eine andere Grundmenge gesprochen als
die, um die es in Schritt 6 geht. Das gehört in die Rücknahme hinein, sonst wird aus einer Präzisierung
eine Widerlegung, die M11 zu Unrecht diskreditiert.

**Wo die vorformulierte Zeile nicht passt.** Sie fragt, ob der beste Typ bei `NEXANS` **nicht** 9018
sei, und knüpft daran eine neue kuratierte Zeile in `V4__bam_spalte.sql`. Ihre Prämisse ist, dass
9018 der heutige Kuratierungsstand wäre. **Ist er nicht:** Kuratiert sind **9006** (Position 1) und
**9001** (Position 2). Gemessen an den Wurzeln stehen diese beiden bei **5,02 %** und **11,79 %**,
während 9018 bei 92,26 % liegt. Die Zeile lässt sich deshalb weder mit „ja" noch mit „nein"
abhaken — der Befund ist:

- **Auf den Wurzeln** wäre 9018 der richtige Typ, keine der beiden kuratierten Zeilen.
- **Auf den Merge-Ergebnissen** ist 9001 mit 95,87 % nahezu perfekt — die Kuratierung trifft dort
  also zu, nur eben auf einer Rolle, die 1.669 von 180.251 Zeilen ausmacht.
- **9006 trifft keine der beiden Rollen** (5,02 % / 0 %).

Die Kuratierung wurde in Schritt 4 **ohne Rücksicht auf die Rolle** gewählt — das war damals die
einzig mögliche Sicht, weil die Rollen erst in M23–M28 gemessen wurden. Eine Änderung an
`bam_spalte` ist **nicht** Teil dieser Aufgabe und fällt im Sparring.

---

# M30 — Was die Auflösung in allen vier Richtungen kostet

**Block 3 derselben Runde, erhoben am 10.08.2026** vor Schritt 6, Teil 1. Rahmen unverändert (siehe
§0; erneut geprüft: `@@global.read_only` → **`1`**, Serverzeit `2026-08-10 15:16:01`, `UTC_TIMESTAMP`
`13:16:01`, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log`, Lesebenutzer `monitor_read@%`).

**Anlass.** Regel L7 gilt für jedes neue Statement, und die Kosten der Auflösung sind bisher **nicht**
gemessen: E5 hat für die Gegenrichtung nur den `EXPLAIN` erhoben, keine Laufzeit — und schon gar
nicht an einer Wurzel mit 3.048 Kindern. M30 misst alle sechs Richtungen einzeln, jeweils **in der
Fassung, die der Code benutzt**: einschließlich der Mandantenkette über `Process`/`ProjectMandant`
und, wo eine Menge geliefert wird, einschließlich `ORDER BY MessageLastUpdate, MessageID` plus
`LIMIT 51`.

> **Die Nummer.** M22 endet in [`messungen-schritt5.md`](messungen-schritt5.md), M23–M28 stehen oben,
> **M29** ist der Nachtrag zum Wartezustand in `messungen-schritt5.md`. Die Nummerierung läuft
> projektweit fort und nicht je Datei; `M30` kommt in keiner anderen Datei vor (geprüft über `docs\`).

## Die Bezugszeilen

Nach ihrer **Gestalt** benannt. Die `MessageID` steht bewusst nicht hier — dieselbe Regel wie in
[`nachrichtendetail.md`](nachrichtendetail.md) §8: beschrieben wird die Gestalt, nicht der Datensatz.
Alle sieben gehören `NEXANS`; nur dort gibt es überhaupt Ketten dieser Breite (M28‑1).

| Kürzel | Gestalt |
|---|---|
| **(W‑breit)** | Split-Wurzel mit **3.048** Kindern, `SPLITTED` — die breiteste Wurzel aus M24‑2. Sie trägt selbst einen `SourceMessageID`, ist also zugleich Split-Kind (Doppelrolle Wurzel + Kind) |
| **(W‑schmal)** | Split-Wurzel mit **genau einem** Kind, `SPLITTED` — der Normalfall, 87 % der Wurzeln (M24‑2) |
| **(E‑breit)** | Merge-Ergebnis mit **749** Eingängen, `EERP_RECEIVED` — das breiteste in Fenster B + Überhang |
| **(E‑schmal)** | Merge-Ergebnis mit **genau einem** Eingang, `EERP_RECEIVED` |
| **(K)** | der Fuß der **tiefsten** Kette aus Fenster B: `FINISHED` → `SPLITTED` → `FINISHED` → `SPLITTED`, vier Glieder, drei Aufstiegsschritte |
| **(W‑max)** | die breiteste Wurzel des **gesamten** Bestands, **3.350** Kinder (M30‑2) |
| **(E‑max)** | das breiteste Merge-Ergebnis des **gesamten** Bestands, **897** Eingänge (M30‑2) |

## M30‑1 Die sechs Richtungen

Der Mandantenfilter steht in **jedem** der Statements als `EXISTS` über `Process → ProjectMandant` —
Wort für Wort derselbe wie in `NachrichtenRepository` und `NachrichtendetailRepository`:

```sql
EXISTS (SELECT 1 FROM Process mp JOIN ProjectMandant pm ON pm.ProjectID = mp.ProjectID
        WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ?)
```

Die gemessenen Statements, gekürzt um die immer gleiche Spaltenliste (`MessageID`, `MessageStatus`,
`MessageLastUpdate`, `SOSName`, `Source`, `Target`, `SourceMessageID`, `TargetMessageID`):

```sql
-- 1 und 3: ein Glied auflösen. Ein Statement, zwei Verwendungen — der Schlüssel kommt einmal
--          aus SourceMessageID (Elternteil), einmal aus TargetMessageID (Merge-Ergebnis).
SELECT … FROM Message m LEFT JOIN SOS s ON s.SOSID = m.SOSID
WHERE m.MessageID = ? AND <Mandantenkette>;

-- 2: meine Kinder                     -- 4: meine Merge-Eingänge (dasselbe mit TargetMessageID)
SELECT … FROM Message m LEFT JOIN SOS s ON s.SOSID = m.SOSID
WHERE m.SourceMessageID = ? AND <Mandantenkette>
ORDER BY m.MessageLastUpdate, m.MessageID LIMIT 51;

-- 5: die Zählung                      -- je einmal über SourceMessageID und TargetMessageID
SELECT COUNT(*) FROM Message m WHERE m.SourceMessageID = ? AND <Mandantenkette>;
```

### `EXPLAIN`

**1 und 3 — jede Tabelle `const`**, nicht `eq_ref`: Die Kennung steht als Literal im Statement, damit
löst MariaDB den Primärschlüsselzugriff schon beim Planen auf.

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|
| `m` (`Message`) | **`const`** | `PRIMARY` | 146 | 1 | |
| `s` (`SOS`) | `const` | `PRIMARY` | 146 | 1 | |
| `mp` (`Process`) | `const` | `PRIMARY` | 146 | 1 | |
| `pm` (`ProjectMandant`) | `const` | `PRIMARY` | 292 | 1 | `Using index` |

**2 — `ref` über `SourceMessageIDIDX`**, wie E5 vorhergesagt hat:

| table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|
| `m` | **`ref`** | `ProejctIDIDX`, `SourceMessageIDIDX`, `Message_ProcessFK` | **`SourceMessageIDIDX`** | 147 | `const` | 6.504 | `Using index condition; Using where; Using filesort` |
| `mp` | `eq_ref` | | `PRIMARY` | 146 | `m.ProcessID` | 1 | `Using where` |
| `pm` | `eq_ref` | | `PRIMARY` | 292 | `mp.ProjectID`, `const` | 1 | `Using where; Using index` |
| `s` | `eq_ref` | | `PRIMARY` | 146 | `m.SOSID` | 1 | `Using where` |

**4 — dieselbe Gestalt über `TargetMessageIDIDX`**, `rows` 749 statt 6.504.

**5 — `ref`, aber ohne `Using index`.** Die Zeilen `mp` und `pm` bleiben, nur `s` und das `filesort`
fallen weg:

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|
| `m` | `ref` | `SourceMessageIDIDX` bzw. `TargetMessageIDIDX` | 147 | 6.504 / 749 | `Using index condition; Using where` |

### Laufzeiten

Serverseitig über `SET profiling = 1` / `SHOW PROFILES`, **beste von fünf nach einem Aufwärmlauf**.
`n` ist die Zahl der Zeilen, die MariaDB dafür anfassen muss — nicht die Zahl der gelieferten.

| # | Richtung | Bezugszeile | `n` | beste von 5 |
|---|---|---|---:|---:|
| 1 | mein Elternteil (PK) | (K), Ebene −1 | n = 1 | **0,505 ms** |
| 2 | meine Kinder, **Breitfall** | (W‑breit) | n = 3.048 | **25,995 ms** |
| 2 | meine Kinder, **Normalfall** | (W‑schmal) | n = 1 | **0,653 ms** |
| 3 | mein Merge-Ergebnis (PK) | (E‑breit) | n = 1 | **0,482 ms** |
| 4 | meine Merge-Eingänge, **Breitfall** | (E‑breit) | n = 749 | **5,659 ms** |
| 4 | meine Merge-Eingänge, **Normalfall** | (E‑schmal) | n = 1 | **0,596 ms** |
| 5a | Zählung der Kinder, **Breitfall** | (W‑breit) | n = 3.048 | **18,659 ms** |
| 5a | Zählung der Kinder, **Normalfall** | (W‑schmal) | n = 1 | **0,544 ms** |
| 5b | Zählung der Eingänge, **Breitfall** | (E‑breit) | n = 749 | **4,197 ms** |
| 5b | Zählung der Eingänge, **Normalfall** | (E‑schmal) | n = 1 | **0,505 ms** |
| 6 | Aufstieg Ebene −1 | (K) | n = 1 | **0,461 ms** |
| 6 | Aufstieg Ebene −2 | (K) | n = 1 | **0,504 ms** |
| 6 | Aufstieg Ebene −3 | (K) | n = 1 | **0,476 ms** |

Und dasselbe am **absoluten Rand des Bestands**, damit die Entscheidungen nicht an einem Fenster
hängen:

| Richtung | Bezugszeile | `n` | beste von 5 |
|---|---|---:|---:|
| 5a Zählung der Kinder | (W‑max) | n = **3.350** | **19,760 ms** |
| 5b Zählung der Eingänge | (E‑max) | n = **897** | **4,904 ms** |
| 2 Kinder, `LIMIT 51` | (W‑max) | n = 3.350 | **26,358 ms** |
| 4 Eingänge, `LIMIT 51` | (E‑max) | n = 897 | **5,857 ms** |

### Die drei ausdrücklich gestellten Fragen

**Bleibt die Zählung unter 50 ms?** **Ja, mit Abstand.** An der breitesten Wurzel des gesamten
Bestands kostet sie **19,760 ms** (n = 3.350) — nicht die Hälfte der Grenze. Beim Merge sind es
**4,904 ms** (n = 897). Damit wird **die genaue Zahl geliefert** („aufgeteilt in 3.350 Teile"), nicht
die Ersatzform „mehr als 50". Die Entscheidung hängt an dieser Zahl und an keiner Abwägung.

**Kostet 4 dasselbe wie 2?** **Nein — und die Kardinalität ist nicht der Grund.** `TargetMessageIDIDX`
steht mit 63.580 gegen 1.780.243 bei `SourceMessageIDIDX` (M23‑1); der Preis je gelesener Zeile ist
trotzdem fast derselbe: 25,995 ms / 3.048 = **8,5 µs** gegen 5,659 ms / 749 = **7,6 µs**. Der
Unterschied ist die **Zahl der Zeilen hinter dem Schlüssel**, nicht die Verteilung des Index. Was
tatsächlich auseinanderfällt, ist die Häufigkeit — siehe M30‑2.

**Bleibt 6 linear in der Tiefe?** **Ja.** Drei Aufstiegsschritte kosten 0,461 + 0,504 + 0,476 =
**1,441 ms**, jeder Schritt ist ein `const`-Zugriff und keiner sieht den anderen. Zehn Ebenen — die
Grenze aus dem Plan — kosten hochgerechnet unter **5 ms**. Der Aufstieg ist damit der billigste Teil
des Endpunkts, und die Tiefengrenze schützt nicht vor Kosten, sondern vor einer Endlosschleife.

### Der Befund, der nicht erfragt war: `LIMIT 51` begrenzt die **Ausgabe**, nicht die **Arbeit**

Die Seite der Kinder kostet an (W‑breit) **25,995 ms**, die reine Zählung derselben Menge
**18,659 ms** — beide lesen alle 3.048 Zeilen. Das `Using filesort` im Plan ist der Grund: Sortiert
wird nach `MessageLastUpdate`, und dafür muss jede Trefferzeile gesehen werden, **bevor** das `LIMIT`
greifen kann. `SourceMessageIDIDX` steht auf `SourceMessageID` und liefert die Sortierreihenfolge
nicht mit.

Praktisch heißt das: **Die Breitengrenze von 50 schützt die Antwort, nicht die Datenbank.** Wer 3.048
Kinder hat, zahlt 26 ms, egal ob er 50 oder 3.048 Zeilen bekommt. Das ist tragbar — es ist die
Größenordnung eines Listenaufrufs (Messung L13) und liegt weit unter der Zeitgrenze von 10 s auf dem
Lese-Pool. Es ist aber kein Grund, die Grenze zu streichen: Sie begrenzt die **Antwort**, und 3.048
Zeilen in einem JSON-Rumpf sind unabhängig von der Datenbank ein Problem.

**Der Zusatzpreis der genauen Zahl ist deshalb kleiner, als er aussieht.** Zählung plus Seite kosten
an (W‑breit) zusammen 44,7 ms; die Seite allein 26,0 ms. Die Zahl kostet also **72 %** obendrauf —
aber beide Statements lesen dieselben Zeilen, und beim Normalfall sind es 0,54 ms gegen 0,65 ms.

**Laufzeit der Erhebung:** 13 Statements × 6 Läufe, zusammen unter 4 s.

## M30‑2 Breite über den **gesamten** Bestand *(ergänzt, nicht im Plan)*

**Warum ohne Zeitfenster.** M24‑2 hat die Breite über Fenster B gemessen und 3.048 gefunden. Die
Breitengrenze und die Entscheidung „genaue Zahl" müssen aber für die **breiteste Zeile des Bestands**
tragen, nicht für die breiteste eines Monats — ein Fenster kann diese Frage grundsätzlich nicht
beantworten. Die Erhebung ist einmalig, **kein** Statement der Anwendung, und sie läuft nicht über
`MessageProperty` oder `MessageAction`, sondern index-nah über die beiden Verkettungsspalten
(Regel L9, Form wie M27).

```sql
SELECT MAX(kinder), COUNT(*), SUM(kinder > 50)
FROM (SELECT SourceMessageID, COUNT(*) AS kinder FROM Message
      WHERE SourceMessageID IS NOT NULL AND SourceMessageID <> '' GROUP BY SourceMessageID) k;
```

| Frage | Ergebnis | Laufzeit |
|---|---|---:|
| Split-Wurzeln im Bestand | **n = 452.822**, größte **3.350** Kinder, **4.823** über 50 (**1,07 %**) | **1,701 s** |
| Merge-Ergebnisse im Bestand | **n = 31.185**, größtes **897** Eingänge, **3.550** über 50 (**11,38 %**) | **1,472 s** |
| Selbstverweise (`SourceMessageID = MessageID` oder `TargetMessageID = MessageID`) | **n = 3,34 Mio. geprüfte Zeilen, 0** | **1,514 s** |

**Die Breitengrenze greift beim Merge zehnmal häufiger als beim Split** — 11,38 % der
Merge-Ergebnisse haben mehr als 50 Eingänge, gegen 1,07 % der Wurzeln. Der Grund steht schon in
M25‑1: Der Zusammenführungsgrad liegt bei 23 : 1, ein Merge sammelt also **im Normalfall** Dutzende.
Ein Split hat im Normalfall genau ein Kind (87 %, M24‑2). Die Verteilung ist bei beiden schief, aber
in entgegengesetzte Richtungen.

Beide Erhebungen sind trotz fehlendem Zeitfenster billig — 1,5 s gegen die 29,2 s von M27 —, weil sie
den vollständigen Sekundärindex lesen und die Tabelle nie anfassen.

## M30‑3 Tiefe und Zyklus *(ergänzt, nicht im Plan)*

Der Aufstieg folgt je Zeile genau **einem** Verweis: `SourceMessageID`, wenn belegt, sonst
`TargetMessageID`. Dass nie beide belegt sind, ist M28‑1 (`Kind + Eingang` = 0). Gemessen wird die
Kettenlänge über wiederholte Selbstverknüpfung, Startmenge Fenster B:

| Glieder in der Kette | Ketten (`n` = Startzeilen in Fenster B) |
|---:|---:|
| 2 | 143.566 |
| 3 | 17.225 |
| 4 | **16.292** |
| 5 | **0** |
| 6 | **0** |

**Die tiefste Kette hat vier Glieder, also drei Aufstiegsschritte.** Das bestätigt „mindestens vier
Ebenen" aus E2/E3 und beziffert es: genau vier. Die Tiefengrenze von zehn greift in der Testkopie
**nie**.

**Und es gibt keinen Zyklus.** Ein Zyklus lieferte auf jeder Stufe Treffer, weil der Aufstieg nie
endete; die Stufe 5 ist über alle 214.330 Startzeilen aus Fenster B **leer**. Dazu **null**
Selbstverweise über den gesamten Bestand (M30‑2, n = 3,34 Mio.).

> **Das ändert am Zyklusschutz nichts.** Er ist die **Zusicherung**, nicht die Beobachtung — dieselbe
> Begründung wie beim Mandantenfilter (M27). Die Kette entsteht durch Datenbank-Events, die uns nicht
> gehören (`MatchInterchange`, `SetTargetFlag`, `MoveDTNA997`, `datenmodell.md` §6), und was heute
> keinen Kreis bildet, muss morgen keinen bilden. Ohne den Schutz stünde an dieser Stelle eine
> Endlosschleife statt einer Antwort; die Tiefengrenze allein bräche zwar ab, sagte aber „tief" statt
> „im Kreis".

## M30‑4 Die vorkommenden Rollenkombinationen — **das sechste Paar**

**Warum diese Zeile hier steht.** Der Plan verlangt zu prüfen, ob das sechste Paar
(`SPLIT_WURZEL` + `MERGE_EINGANG`) je erhoben wurde. **Es wurde** — in M28‑1c, dort aber
**ausschließlich über Fenster B**. Weil `KettenrollenBestandTest` die vorkommenden Kombinationen
gegen eine im Code hinterlegte Liste prüft, wird die Aufstellung hier über **beide** Fenster
vollständig erhoben, in einer einzigen Gruppierung und ohne Inklusions-Exklusions-Rechnung.

```sql
SELECT CONCAT_WS('+',
         CASE WHEN m.Source + 0 = 1 THEN 'WURZEL' END,
         CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '' THEN 'KIND' END,
         CASE WHEN m.TargetMessageID IS NOT NULL AND m.TargetMessageID <> '' THEN 'EINGANG' END,
         CASE WHEN m.Target + 0 = 1 THEN 'ERGEBNIS' END) AS kombination,
       COUNT(*), COUNT(DISTINCT pm.MandantID)
FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID
     JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
GROUP BY kombination ORDER BY COUNT(*) DESC;
```

| Kombination | Fenster A (n = 6.249) | Mandanten | Fenster B (n = 214.330) | Mandanten |
|---|---:|---:|---:|---:|
| `SPLIT_KIND` | 4.092 | 4 | 104.049 | 5 |
| *(leer, keine Rolle)* | 1.240 | 5 | 39.090 | 6 |
| `MERGE_EINGANG` | 432 | 1 | 38.628 | 1 |
| `SPLIT_WURZEL` | 475 | 3 | 30.438 | 4 |
| `MERGE_ERGEBNIS` | 6 | 1 | 1.611 | 1 |
| `SPLIT_WURZEL` + `SPLIT_KIND` | 4 | 2 | 456 | 2 |
| `SPLIT_KIND` + `MERGE_ERGEBNIS` | **0** | — | **33** | 1 |
| `SPLIT_WURZEL` + `MERGE_ERGEBNIS` | **0** | — | **25** | 1 |
| **`SPLIT_WURZEL` + `MERGE_EINGANG`** | **0** | — | **0** | — |
| `SPLIT_KIND` + `MERGE_EINGANG` | **0** | — | **0** | — |
| `MERGE_EINGANG` + `MERGE_ERGEBNIS` | **0** | — | **0** | — |
| drei oder vier Rollen | **0** | — | **0** | — |

**Drei der sechs Paare kommen vor, drei nicht** — und die drei, die nicht vorkommen, sind genau die
drei mit `MERGE_EINGANG`. Das ist kein Zufall, aber auch **nicht durchgängig** eine zweite Messung
derselben Sache:

- `MERGE_EINGANG` heißt „`TargetMessageID` belegt", und M28‑1 hat gemessen, dass keine Zeile beide
  ID-Spalten trägt — damit ist `Kind + Eingang` **ausgeschlossen** und die Null hier nur die
  Bestätigung.
- Für `Wurzel + Eingang` und `Eingang + Ergebnis` folgt daraus **nichts**: `Source` und `Target` sind
  Flags und keine ID-Spalten. Beide Nullen sind **gemessen und nicht abgeleitet** — und damit die
  beiden Zeilen, auf die `KettenrollenBestandTest` tatsächlich aufpasst.

Die Zahlen decken sich Zeile für Zeile mit M28‑1 und M28‑1c: 456 + 33 + 25 = **514** Zeilen mit zwei
Rollen über Fenster B, 4 über Fenster A. Die Spalte `SPLIT_WURZEL` ergibt mit den Doppelrollen
30.438 + 456 + 25 = **30.919** — genau die Summe der `split_wurzel`-Spalte aus M28‑1 (a).

> **Verweis, nachgetragen am 11.08.2026.** Diese Tabelle zählt **Knoten** — Zeilen, die zwei Rollen
> tragen. [`verkettung.md`](verkettung.md) §8.3 stützt sich für den Zickzack-Aufstieg auf die **33**
> aus der Zeile `SPLIT_KIND` + `MERGE_ERGEBNIS`. **M31 stellt eine andere Frage:** nicht wie viele
> Knoten es gibt, sondern wie viele **Ansichten** dadurch verzerrt werden. Die Antwort steht in
> M31‑1 (390 über dasselbe Fenster) und ersetzt die 33 nicht — sie beantwortet etwas anderes. Die
> Kontrolle in M31‑0 hat diese Tabelle Zeile für Zeile reproduziert.

**Laufzeit:** **0,109 s** (Fenster A) · **3,744 s** (Fenster B) — dasselbe Statement wie M28‑1 mit
anderer Gruppierung, deshalb dieselben Kosten und keine eigene Profilzeile.

## M30‑5 Kostet der Mandantenfilter in der Zählung etwas — und ändert er sie?

Die Zählung trägt den Mandantenfilter, weil sonst die Antwort „3.048 Teile" nennt und 3.000 liefert
(Regel M5). Ob er die Zahl überhaupt verändert, ist die Gegenprobe zu M27:

| Bezugszeile | ohne Filter | mit Filter |
|---|---:|---:|
| (W‑breit), n = 3.048 | 3.048 | **3.048** |
| (E‑breit), n = 749 | 749 | **749** |

**Identisch, wie M27 erwarten lässt.** Der Filter bleibt trotzdem in jedem der fünf Statements: Er ist
die Zusicherung, nicht die Beobachtung. Sein Preis steht in der Laufzeittabelle oben — er ist bereits
in jeder dort genannten Zahl enthalten, weil **alle** Statements mit ihm gemessen wurden.

## M30‑6 Hat jede Zeile einen Zeitpunkt? *(ergänzt, nicht im Plan)*

**Warum das gefragt wurde.** Der Sortierschlüssel der Nachfolger ist `(MessageLastUpdate,
MessageID)`, und daraus entsteht der Cursor. `MessageLastUpdate` ist laut Schema `NULL`-fähig
(M23‑1). Eine Zeile ohne Zeitpunkt hätte keine Cursor-Position — die Frage muss vor dem Bau
beantwortet sein und nicht danach.

```sql
SELECT COUNT(*) AS zeilen_gesamt, SUM(MessageLastUpdate IS NULL) AS ohne_zeitpunkt FROM Message;
```

| | Ergebnis |
|---|---:|
| Zeilen insgesamt | **n = 3.341.519** |
| davon ohne `MessageLastUpdate` | **0** |

**Laufzeit:** **1,159 s** (einmalige Erhebung ohne Zeitfenster, Form wie M30‑2).

Die Zeilenzahl deckt sich **exakt** mit der in M0 gezählten (3.341.519) — die Testkopie ist seit dem
07.08.2026 unverändert, wie schon der Abgleich in M23‑1 gezeigt hat.

**Folge für den Code — und die Grenze dessen, was sich behandeln lässt.** Der Fall wird behandelt,
aber nicht restlos, und das gehört genau hierhin:

- **Ausgeliefert wird eine solche Zeile.** Die Zusammenführung der beiden Abwärtsrichtungen in Java
  sortiert `null` **zuerst**, weil MariaDB es in `ORDER BY … ASC` so hält — liefe die eine Ordnung
  anders als die andere, stimmte die Seitengrenze nicht mehr mit dem Cursor überein. Weil `null`
  vorn steht, steht eine solche Zeile am Anfang der Seite und der Cursor entsteht aus der
  **letzten** Zeile; das Blättern bleibt also heil, solange irgendeine Zeile der Seite einen
  Zeitpunkt trägt.
- **Nicht behandelbar ist eine ganze Seite ohne Zeitpunkt.** Dann gibt es im Sortierschlüssel
  `(MessageLastUpdate, MessageID)` keine Position, an der weitergeblättert werden könnte — die
  Spalte ist der halbe Schlüssel. Der Endpunkt antwortet dort mit einem **technischen Fehler**
  (`500` mit `traceId` und Stacktrace) und nennt im Protokoll die gebrochene Zusicherung. Ein `4xx`
  wäre falsch: Er legte dem Aufrufer eine Verhaltensänderung nahe, die nichts änderte — er hat
  richtig gefragt, die Quelle hält ihre Zusage nicht.

Dieselbe Haltung wie bei `MessageTimeout = NULL` in [`message-status.md`](message-status.md): Die
Spalte lässt es zu, und die Produktion muss sich nicht daran halten, was die Testkopie zufällig
enthält. **Der Unterschied ist, dass sich dort jeder Fall abfangen lässt und hier nicht** — deshalb
steht hier, welcher übrig bleibt, statt „behandelt" zu behaupten.

### Was daraus folgt

| Befund (vor der Messung formuliert) | trifft zu | Konsequenz |
|---|---|---|
| 1 und 3 sind `eq_ref` | **ja, schärfer** | Sie sind `const` — MariaDB löst den Zugriff beim Planen auf. 0,48 bis 0,51 ms je Glied |
| 2 ist `ref` über `SourceMessageIDIDX` (E5) | **ja** | Der Zugriffspfad aus E5 ist bestätigt, jetzt mit Laufzeit |
| 4 ist `ref` über `TargetMessageIDIDX` | **ja** | dieselbe Gestalt, `rows` 749 statt 6.504 |
| 5 ist `ref` mit **`Using index`** | **nein** | siehe Absatz unten |
| Die Zählung bleibt an der breitesten Wurzel **unter 50 ms** | **ja** (19,760 ms bei n = 3.350) | **Die genaue Zahl wird geliefert**, nicht „mehr als 50" |
| Die Zählung liegt **über 50 ms** | nein | — (die Ersatzform „mehr als 50" wird nicht gebraucht) |
| 4 kostet dasselbe wie 2 | **nein** | 5,659 gegen 25,995 ms — aber je Zeile 7,6 gegen 8,5 µs. Es ist die Zeilenzahl, nicht die Kardinalität |
| 6 bleibt linear in der Tiefe | **ja** | 3 × rund 0,48 ms, jeder Schritt unabhängig. Zehn Ebenen unter 5 ms |
| Die Tiefengrenze von 10 greift in der Testkopie | **nein** (tiefste Kette: 4 Glieder) | Sie ist Schutz, kein Betriebsmittel — und sie bleibt, weil die Events uns nicht gehören |
| Ein Zyklus kommt vor | **nein** (Stufe 5 leer, 0 Selbstverweise) | Der Zyklusschutz bleibt trotzdem, aus demselben Grund wie der Mandantenfilter |
| Das sechste Paar (`Wurzel + Eingang`) war **ungemessen** | **nein** — M28‑1c hat es über Fenster B erhoben | M30‑4 zieht Fenster A nach; beide sind **0**. Es steht als Zeile in der hinterlegten Liste, weil „kommt nicht vor" und „ist nicht formulierbar" zweierlei sind |

**Wo die vorformulierte Zeile nicht passt — das erhoffte `Using index`.** Die Zählung liest den Index
und **muss trotzdem in die Tabelle**: Der Mandantenfilter braucht `Message.ProcessID`, und die steht
weder in `SourceMessageIDIDX` noch in `TargetMessageIDIDX`. Ein `COUNT(*)` allein über die
Verkettungsspalte wäre index-only — mit dem Filter ist es das nicht mehr. **Das ist der Preis von
Regel M5, und er ist gemessen: 18,7 ms für 3.048 Zeilen.** Er wird bezahlt und nicht verhandelt; die
Alternative wäre eine Zahl, die einen anderen Bestand beschreibt als die Liste darunter.

**Und ein zweiter Befund ohne vorformulierte Zeile: die Breite des Merge.** Der Plan nennt für die
Zusammenführung den **Durchschnitt** aus M25‑1 (23 : 1). Das Maximum war nie erhoben. Es liegt bei
**897** Eingängen an einem Ergebnis, und **11,38 %** aller Merge-Ergebnisse haben mehr als 50 — gegen
1,07 % der Split-Wurzeln. Für die Oberfläche heißt das: `weitereVorhanden` ist beim Merge der
**Regelfall** und nicht der Ausnahmefall, den der Split nahelegt.

---

# M31 — Der Zickzack-Aufstieg und der Takt von `MatchInterchange`

**Block 4, erhoben am 11.08.2026** als Nacharbeit zu Schritt 6, Teil 2b. Zwei Fragen, die Teil 2b
offen gelassen hat und die nicht in der Oberfläche beantwortet werden konnten:

1. **Wie viele Ansichten** verzerrt der Zickzack-Aufstieg wirklich?
   [`verkettung.md`](verkettung.md) §8.3 nennt dafür die **33** aus M30‑4 — und die zählt *Knoten*,
   nicht Ansichten (M31‑1, M31‑2).
2. **Wie oft läuft `MatchInterchange`?** §9 und §11 begründen den fehlenden Hinweis auf die
   Verzögerung damit, dass der Takt **nicht gemessen** sei; [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
   §3.3 nennt „stündlich" als Tatsache. Beides zusammen geht nicht auf (M31‑3).

> **Was dieser Block ausdrücklich nicht tut.** Er trifft **keine Entscheidung**. Kein Satz für die
> Oberfläche wird formuliert, kein Hinweis gebaut, keine Zahl in
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3 eingetragen oder gestrichen, und an
> [`verkettung.md`](verkettung.md) ändert sich nichts. Er stellt fest; die Auswertung gehört ins
> Sparring zu Schritt 7.
>
> **M30‑4 wird nicht überschrieben.** Die 33 bleiben, wo sie stehen. M31 stellt eine andere Frage,
> und beide Zahlen stehen nebeneinander.

## M31‑0 Rahmen und Kontrolle

Rahmen unverändert gegenüber §0, erneut geprüft — als **erste Abfrage der Sitzung**, vor jedem
anderen Statement:

| | |
|---|---|
| `SELECT @@global.read_only` | **`1`** — die Testkopie, niemals die Produktion |
| Serverzeit zu Beginn | `2026-08-11 12:01:01` (`UTC_TIMESTAMP` `10:01:01`, also UTC+2) |
| Version | `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| Benutzer | Lesebenutzer `monitor_read@%`; `SHOW GRANTS` liefert **dieselben drei Zeilen** wie in Schritt 4, 5 und in §0 (der Hash bleibt nach Regel G1 ungedruckt) |
| Client | `mysql` `8.0.46` aus MySQL Workbench mit `--ssl-mode=DISABLED` — derselbe wie in allen Runden zuvor |
| Laufzeitmessung | serverseitig über `SET profiling = 1`; beste von fünf nach einem Aufwärmlauf, bei Statements über zehn Sekunden beste von drei. **Zwei Ausnahmen sind ausgewiesen** — siehe die Laufzeittabelle |

**Die Testkopie ist seit dem 07.08.2026 unverändert**, und das ist geprüft und nicht angenommen:

| | `DATA_LENGTH` | `INDEX_LENGTH` |
|---|---:|---:|
| `Message` | 740.851.712 | 2.157.330.432 |
| `MessageAction` | 2.226.634.752 | 819.855.360 |

Beide **byteidentisch** mit M23‑1 vom 10.08.2026. Die gezählte Zeilenzahl von `Message` liegt
weiterhin bei **3.341.519** (M0, M30‑6). Die Zahlen dieses Blocks dürfen deshalb ohne Vorbehalt
gegen M0 bis M30 gehalten werden.

### Das Bezugsfenster, wörtlich zitiert

Alle Zahlen von M31‑1 und M31‑2 laufen über **Fenster B** aus §0, unverändert:

> `2025-11-30 00:00:00` ≤ `MessageLastUpdate` < `2025-12-30 00:00:00` — **214.330** Nachrichten.

**Kein Kinderüberhang, und der Grund ist ein anderer als bei M24.** Dort zählt das Statement
*abwärts* und braucht den Überhang, weil die Kinder nach dem Elternteil entstehen. M31 zählt Zeilen
**im** Fenster und löst von dort **aufwärts** auf — und **das Fenster steht in keinem der Joins**:
`a`, `w`, `p1` und `p2` werden ausschließlich über den Primärschlüssel erreicht, ohne jede Bedingung
auf `MessageLastUpdate`. Der Zeitpunkt des Aufstiegsziels ist damit gleichgültig, und das ist hier
nötig und nicht nur bequem: Beim Merge entsteht das Ergebnis **nach** seinen Eingängen, das
Aufstiegsziel kann also durchaus jenseits des oberen Randes liegen. Gemessen ist, dass **kein**
Aufstiegsziel ins Leere zeigt (0 von 143.166, siehe M31‑2).

### Die Kontrolle: die 33 aus M30‑4, reproduziert

Vor jeder neuen Zahl derselbe Aufbau wie in M30‑4 — dasselbe Statement, dasselbe Fenster:

```sql
SELECT CONCAT_WS('+',
         CASE WHEN m.Source + 0 = 1 THEN 'WURZEL' END,
         CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> '' THEN 'KIND' END,
         CASE WHEN m.TargetMessageID IS NOT NULL AND m.TargetMessageID <> '' THEN 'EINGANG' END,
         CASE WHEN m.Target + 0 = 1 THEN 'ERGEBNIS' END) AS kombination,
       COUNT(*), COUNT(DISTINCT pm.MandantID)
FROM Message m JOIN Process p ON p.ProcessID = m.ProcessID
     JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY kombination ORDER BY COUNT(*) DESC;
```

| Kombination | M30‑4 (10.08.) | M31‑0 (11.08.) | Mandanten |
|---|---:|---:|---:|
| `SPLIT_KIND` | 104.049 | **104.049** | 5 |
| *(leer, keine Rolle)* | 39.090 | **39.090** | 6 |
| `MERGE_EINGANG` | 38.628 | **38.628** | 1 |
| `SPLIT_WURZEL` | 30.438 | **30.438** | 4 |
| `MERGE_ERGEBNIS` | 1.611 | **1.611** | 1 |
| `SPLIT_WURZEL` + `SPLIT_KIND` | 456 | **456** | 2 |
| **`SPLIT_KIND` + `MERGE_ERGEBNIS`** | **33** | **33** | 1 |
| `SPLIT_WURZEL` + `MERGE_ERGEBNIS` | 25 | **25** | 1 |

**Zeile für Zeile identisch**, Summe 214.330. Die Kontrolle ist bestanden; alles Weitere steht auf
demselben Boden wie M30.

**Laufzeit:** **3,009 s**, beste von fünf (M30‑4 nannte 3,744 s für dasselbe Statement — dieselbe
Größenordnung, an einem ruhigeren Server).

## M31‑1 Merge-Eingänge, deren Ergebnis selbst ein Split-Kind ist

**Frage.** Ich bin **Merge-Eingang**. Stufe −1 ist mein Ergebnis `A` und steht nach
[`verkettung.md`](verkettung.md) §8.3 unter „Wurde zu" — richtig. Ist `A` selbst **Split-Kind**, dann
steht Stufe −2 unter „Kommt von", und ich komme nicht von dort: `B` ist der Ursprung von `A`, nicht
meiner. **Wie viele Zeilen sehen diese verzerrte Ansicht?**

```sql
-- Zaehler
SELECT COUNT(*) FROM Message e
JOIN Message a ON a.MessageID = e.TargetMessageID
WHERE e.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND e.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND e.TargetMessageID IS NOT NULL AND e.TargetMessageID <> ''
  AND a.SourceMessageID IS NOT NULL AND a.SourceMessageID <> '';

-- Nenner: alle Merge-Eingaenge im selben Fenster
SELECT COUNT(*) FROM Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
  AND TargetMessageID IS NOT NULL AND TargetMessageID <> '';
```

**`EXPLAIN`** — **die Abfrage steigt nicht über einen Verkettungsindex ein**, sondern über das
Fenster; `TargetMessageIDIDX` steht nur unter `possible_keys`. Das Ergebnis `a` kommt als `eq_ref`
über den Primärschlüssel dazu, genau wie Richtung 3 in M30‑1:

| table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|
| `e` | `range` | `TargetMessageIDIDX`, `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX` | **`MessageLastUpdateIDX`** | 5 | NULL | 409.756 | `Using index condition; Using where` |
| `a` | `eq_ref` | `PRIMARY`, `SourceMessageIDIDX` | `PRIMARY` | 146 | `e.TargetMessageID` | 1 | `Using where` |

Der Nenner läuft über dieselbe erste Zeile ohne die zweite. **Das ist erwartbar und kein Mangel:**
Das Fenster ist das selektive Prädikat, `TargetMessageID IS NOT NULL` ist es nicht.

### Ergebnis

| | Fenster B |
|---|---:|
| Merge-Eingänge insgesamt (Nenner) | **38.628** |
| davon mit einem Ergebnis, das selbst Split-Kind ist | **390** |
| Anteil | **1,01 %** |
| Anteil an allen 214.330 Zeilen des Fensters | **0,18 %** |
| verschiedene Ergebnisknoten `A` dahinter | **32** |
| Ansichten je Knoten | **12,19** |

**Je Mandant** (Regel L7) — der Mandant der *ansehenden* Zeile, über `Process → ProjectMandant`:

| `MandantID` | Merge-Eingänge | davon betroffen | Anteil |
|---|---:|---:|---:|
| `NEXANS` | 38.628 | **390** | **1,01 %** |
| alle übrigen | **0** | 0 | — |

**Kein anderer Mandant hat auch nur einen Merge-Eingang**, weder betroffen noch unbetroffen — das
deckt sich mit M28‑1 (a), wo die Spalte `Merge-Eingang` außer bei `NEXANS` durchgängig null ist. Die
Verzerrung ist damit **kein mandantenübergreifendes Phänomen**, sondern eines von `NEXANS`. Für
Regel L7 trägt diese Zahl allein nicht; die zweite Hälfte liefert M31‑2, wo fünf Mandanten in der
Grundmenge stehen.

### Knoten gegen Ansichten — die Zahl, um die es überhaupt geht

Die Verteilung der 390 auf ihre 32 Knoten:

| Eingänge an einem Knoten `A` | 1 | 2 | 3 | 4 | 6 | 7 | 8 | 9 | 10 | 13 | 14 | 18 | 21 | 23 | 24 | 27 | 31 | 32 | 40 |
|---|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|
| Knoten | 7 | 1 | 1 | 1 | 1 | 1 | 3 | 1 | 4 | 2 | 1 | 1 | 1 | 1 | 1 | 1 | 1 | 2 | 1 |

**Die Vermutung hinter der Frage trifft der Richtung nach zu, der Größe nach nicht.** Sie lautete:
Ein `A` kann bis zu 897 Eingänge haben (M30‑2), dann wären es 897 verzerrte Ansichten statt einer.
Gemessen hat der breiteste Zickzack-Knoten **40** Eingänge im Fenster:

| | Fenster B | ganzer Bestand |
|---|---:|---:|
| Knoten, die zugleich Merge-Ergebnis und Split-Kind sind | 33 (M30‑4) | **795** |
| Ansichten daran | **390** | **7.935** |
| Ansichten je Knoten | 11,8 | 9,98 |
| breitester solcher Knoten | 40 Eingänge | **50** Eingänge |
| davon über der Breitengrenze 50 | 0 | **0** |

**Die 897 aus M30‑2 gehören einem Merge-Ergebnis, das *kein* Split-Kind ist.** Über den gesamten
Bestand hat **kein einziger** Zickzack-Knoten mehr als 50 Eingänge — die Breitengrenze und die
Verzerrung treffen sich nirgends.

Zwei Gegenproben zur Sauberkeit der Auswahl:

- **Alle 32 Knoten tragen `Target = 1`**, sind also auch nach der Rollenregel aus
  [`verkettung.md`](verkettung.md) §5 Merge-Ergebnisse und nicht nur „Ziel eines Verweises". Das ist
  E4 über eine andere Stichprobe, und es stimmt wieder.
- **Alle 32 liegen selbst in Fenster B.** Die Zahl 32 gegen 33 ist deshalb keine Abweichung, sondern
  eine andere Frage: Die 33 aus M30‑4 tragen im **Bestand** zusammen **399** Eingänge, davon **390**
  in Fenster B. **Einer der 33 hat in Fenster B keinen Eingang** — im Bestand haben alle mindestens
  einen. Deshalb 32 Knoten mit Ansichten im Fenster und 33 Knoten insgesamt.

**Laufzeiten:** Zähler **1,337 s**, Nenner **1,217 s**, je beste von fünf. Die Auswertung je Mandant
(mit `Process`/`ProjectMandant` und `LEFT JOIN` auf `a`) **1,671 s**.

## M31‑2 Split-Kinder, deren Wurzel selbst Merge-Eingang ist

**Frage.** Der Spiegelfall: Ich bin **Split-Kind**, meine Wurzel `W` ist selbst **Merge-Eingang**
nach `M`. Dann steht `M` unter „Wurde zu", obwohl `W` zu `M` wurde und nicht ich.

```sql
-- Zaehler
SELECT COUNT(*) FROM Message k
JOIN Message w ON w.MessageID = k.SourceMessageID
WHERE k.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND k.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND k.SourceMessageID IS NOT NULL AND k.SourceMessageID <> ''
  AND w.TargetMessageID IS NOT NULL AND w.TargetMessageID <> '';

-- Nenner: alle Split-Kinder im selben Fenster
SELECT COUNT(*) FROM Message
WHERE MessageLastUpdate >= '2025-11-30 00:00:00'
  AND MessageLastUpdate <  '2025-12-30 00:00:00'
  AND SourceMessageID IS NOT NULL AND SourceMessageID <> '';
```

**`EXPLAIN`** — dieselbe Gestalt wie bei M31‑1, spiegelbildlich, und **ebenfalls über das Fenster
statt über `SourceMessageIDIDX`**:

| table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|
| `k` | `range` | `SourceMessageIDIDX`, `MessageLastUpdateIDX`, `MessageLastUpdateProcessMessageIDX` | **`MessageLastUpdateIDX`** | 5 | NULL | 409.756 | `Using index condition; Using where` |
| `w` | `eq_ref` | `PRIMARY`, `TargetMessageIDIDX` | `PRIMARY` | 146 | `k.SourceMessageID` | 1 | `Using where` |

### Ergebnis — **null**, und zwar je Mandant

| `MandantID` | Split-Kinder | davon betroffen | Wurzel löst nicht auf |
|---|---:|---:|---:|
| `NEXANS` | 101.270 | **0** | 0 |
| `IBISGUS` | 1.722 | **0** | 0 |
| `SUTTONS` | 1.247 | **0** | 0 |
| `ZAST` | 263 | **0** | 0 |
| `IBIS` | 36 | **0** | 0 |
| **gesamt** | **104.538** | **0** | **0** |

**Fünf Mandanten, darunter drei kleine — Regel L7 ist damit erfüllt**, und sie ist es an der Zahl,
die überhaupt eine Grundmenge außerhalb von `NEXANS` hat. Die Spalte „Wurzel löst nicht auf" ist die
Gegenprobe aus M24‑1 über die neue Menge: **kein Rückverweis zeigt ins Leere.**

Die Nenner decken sich mit M28‑1 (a) und M30‑4: 104.049 `SPLIT_KIND` + 456 `WURZEL+KIND` + 33
`KIND+ERGEBNIS` = **104.538**.

### Aber „null im Fenster" heißt nicht „gibt es nicht"

Der Fall setzt eine Zeile voraus, die zugleich **Split-Wurzel** und **Merge-Eingang** ist. M28‑1c und
M30‑4 haben dieses Paar über Fenster A und Fenster B erhoben, beide Male **null** — und
[`verkettung.md`](verkettung.md) §5 hält ausdrücklich fest, dass diese Null **gemessen und nicht
abgeleitet** ist. Über den **gesamten Bestand**, ohne Fenster, sieht es anders aus:

```sql
SELECT COUNT(*) FROM Message
WHERE TargetMessageID IS NOT NULL AND TargetMessageID <> '' AND Source + 0 = 1;
```

| | Ergebnis |
|---|---|
| Zeilen mit `SPLIT_WURZEL` + `MERGE_EINGANG` im Bestand | **4** |
| Mandant | **`NXHBE`** |
| Zeitpunkt | `2025-05-19 06:14:08` bis `06:14:10` |
| Kinder daran, und damit Ansichten der zweiten Art | **4** |

> ⚠️ **Das schärft eine Aussage, ohne sie umzustoßen.** Die Kombination
> `SPLIT_WURZEL` + `MERGE_EINGANG` ist über beide Bezugsfenster zu Recht mit null angegeben; über
> den Bestand kommt sie **vier Mal** vor. `Kettenrollen.FORMULIERTE_KOMBINATIONEN` führt sie
> ohnehin, es ist also **kein Codefehler** — aber `KettenrollenBestandTest` läuft über Fenster B und
> bekäme diese vier Zeilen nie zu sehen. **Das ist eine Feststellung, keine Forderung**; ob der Test
> sein Fenster ändert, gehört ins Sparring.
>
> **`NXHBE` kommt in keiner Messung dieser Datei vor** — nicht in M24‑3, nicht in M28‑1, nicht in
> M30‑4. Alle bisherigen Mandantenlisten stammen aus einem Fenster; dieser Mandant hat darin keine
> Zeile. Was das über seinen Bestand sonst sagt, ist **nicht** gemessen.

**Laufzeiten:** Zähler **1,519 s**, Nenner **1,262 s**, je Mandant **2,316 s**, die Bestandsprüfung
**1,263 s** — je beste von fünf. Die Bestandsprüfung läuft ohne Zeitfenster (Regel L9): Ein Fenster
kann die Frage „kommt das im Bestand vor" grundsätzlich nicht beantworten, und genau dieses Fenster
hatte die Antwort schon zweimal mit null gegeben. `EXPLAIN` weist sie als vollen Durchlauf aus
(`type: ALL`, `rows` 3.560.486) — `Source + 0 = 1` ist über kein Index prüfbar. Die Kosten sind
oben genannt und liegen bei einem Viertel dessen, was M30‑2 für seine drei fensterlosen Erhebungen
gekostet hat.

## M31‑1/2 Die dritte Stufe — gibt es einen Wechsel über zwei Stufen hinaus?

Die tiefste Kette der Testkopie hat vier Glieder (M30‑3), ein Wechsel wäre also auch auf Stufe −3
denkbar. Weil je Zeile **nie beide** ID-Spalten belegt sind (M28‑1; hier über Fenster B erneut
geprüft: **0**), folgt der Aufstieg genau einem Verweis, und die Beziehung je Stufe ist eindeutig:

```sql
SELECT CASE WHEN x.SourceMessageID  IS NOT NULL AND x.SourceMessageID  <> '' THEN 'A'
            WHEN x.TargetMessageID  IS NOT NULL AND x.TargetMessageID  <> '' THEN 'Z' END AS stufe_1,
       …  -- dasselbe fuer p1 und p2
       COUNT(*)
FROM Message x
LEFT JOIN Message p1 ON p1.MessageID = COALESCE(NULLIF(x.SourceMessageID,''),  NULLIF(x.TargetMessageID,''))
LEFT JOIN Message p2 ON p2.MessageID = COALESCE(NULLIF(p1.SourceMessageID,''), NULLIF(p1.TargetMessageID,''))
WHERE x.MessageLastUpdate >= '2025-11-30 00:00:00' AND x.MessageLastUpdate < '2025-12-30 00:00:00'
  AND ( … die Zeile hat ueberhaupt einen Aufstieg … )
GROUP BY stufe_1, stufe_2, stufe_3;
```

`A` = Aufteilung (über `SourceMessageID`), `Z` = Zusammenführung (über `TargetMessageID`), *(Ende)* =
die Kette hört hier auf.

| Stufe −1 | Stufe −2 | Stufe −3 | Zeilen | Wechsel? |
|---|---|---|---:|---|
| `A` | *(Ende)* | — | 87.703 | — |
| `Z` | *(Ende)* | — | 38.238 | — |
| `A` | `A` | `A` | 16.292 | nein |
| `A` | `A` | *(Ende)* | 543 | nein |
| **`Z`** | **`A`** | *(Ende)* | **390** | **ja, auf Stufe −2** |
| **gesamt** | | | **143.166** | |

**Nein — einen Wechsel über zwei Stufen hinaus gibt es nicht.** Die einzige Kombination mit Wechsel
(`Z` → `A`) endet auf Stufe −2; die einzige dreistufige Kette (`A` → `A` → `A`) wechselt nirgends.
Und die Gegenrichtung `A` → `Z` kommt auf **keiner** Stufe vor — das ist M31‑2 ein zweites Mal, aus
einem anderen Statement.

**Kreuzprobe:** 143.166 ist genau P1 aus M28‑1 (a) über Fenster B. Dieselbe Grundmenge, anderes
Statement, gleiche Zahl.

> **Eine Abweichung, die hier auffällt und die dieser Block nicht auflöst.** Liest man die Tabelle
> aus M30‑3 kumulativ („Ketten mit **mindestens** n Gliedern"), reproduzieren sich zwei ihrer drei
> Zeilen hier **exakt**: 4 Glieder → 16.292, und 3 Glieder → 543 + 390 + 16.292 = **17.225**. Die
> erste Zeile passt nicht: M30‑3 nennt **143.566**, gemessen sind hier **143.166** — **400 mehr**,
> als die Teile ergeben. M28‑1 (a) steht mit P1 = 143.166 auf der Seite der heutigen Messung.
> **Nicht weiterverfolgt**, weil es außerhalb der Frage dieses Blocks liegt; es steht hier, damit die
> Abweichung nicht unbemerkt bleibt. M30‑3 wird nicht angefasst.

**Laufzeit:** **2,017 s**, beste von fünf. Der Einstieg ist wieder `MessageLastUpdateIDX` (`range`,
409.756), beide Vorgänger sind `eq_ref` über den Primärschlüssel mit `ref: func` — der `COALESCE`
verhindert keinen Indexzugriff.

## Was daraus folgt — M31‑1 und M31‑2

Die Zeilen dieser Tabelle stammen aus dem Auftrag zu diesem Block und standen **vor** der Erhebung
fest; nachgetragen ist ausschließlich die Spalte „trifft zu".

| Befund (vorformuliert) | trifft zu | Konsequenz |
|---|---|---|
| M31‑1 + M31‑2 erreichen **dieselbe Größenordnung** wie die 38.628 aus M30‑4 | **nein** | — |
| Sie bleiben **zwei bis drei Zehnerpotenzen** darunter | **ja — knapp zwei** | 390 gegen 38.628 ist **Faktor 99**. Nach der vorformulierten Lesart ist das ein **Vermerk** und kein zweites Argument von gleichem Gewicht |
| Ein `A` mit vielen Eingängen macht aus einem Knoten viele Ansichten | **ja, aber gedämpft** | 12,19 Ansichten je Knoten statt der befürchteten bis zu 897. Der breiteste Zickzack-Knoten hat **40** Eingänge im Fenster, **50** im Bestand |
| Ein Aufstieg mit Wechsel über zwei Stufen hinaus existiert | **nein** | Die Verzerrung ist immer **zweistufig**; eine dritte Stufe entsteht nirgends |

**Die beiden Zahlen nebeneinander, ohne Urteil:**

| | Zeilen | Anteil an Fenster B |
|---|---:|---:|
| Zeilen, für die die Einteilung nach der **API-Richtung** falsch beschriftet wäre (M30‑4, die Zahl hinter §8.3) | **38.628** | 18,02 % |
| Zeilen, für die die Einteilung nach der **Flussrichtung** eine Stufe falsch beschriftet (M31‑1 + M31‑2) | **390** | 0,18 % |

Über den gesamten Bestand liegt dasselbe Verhältnis bei **7.939** Zickzack-Ansichten
(7.935 + 4) gegen **2.341.346** Zeilen mit Aufstieg — **0,34 %**. Die Quote ist damit über Fenster
und Bestand hinweg **stabil**: 1,01 % der Merge-Eingänge im Fenster, 1,06 % im Bestand.

**Wo die vorformulierte Zeile nicht passt.** Der Auftrag behandelt M31‑1 und M31‑2 als zwei Hälften
derselben Zahl. Gemessen ist die zweite Hälfte **null** — und zwar nicht knapp, sondern über fünf
Mandanten und 104.538 Zeilen. **Die Verzerrung ist einseitig:** Sie trifft ausschließlich
Merge-Eingänge, deren Ergebnis ein Split-Kind ist, und ausschließlich `NEXANS`. Der Spiegelfall
setzt eine Zeile voraus, die zugleich Wurzel und Merge-Eingang ist; die gibt es im Bestand **vier
Mal**, in keinem der beiden Bezugsfenster, und an ihr hängen **vier** Kinder.

**Die beiden Handlungsmöglichkeiten bleiben damit beide offen** — die Entscheidung fällt nicht hier:

- **Den Aufstieg beim Beziehungswechsel abbrechen.** Er endete dann bei 390 von 143.166 Zeilen
  (0,27 %) eine Stufe früher; alle anderen Ketten blieben unverändert, weil es außer `Z` → `A`
  keinen Wechsel gibt. Die Zahl der verlorenen Glieder ist genau 390 — dieselbe Zeile, die heute
  falsch beschriftet ist.
- **Die Verzerrung dokumentiert stehen lassen.** Sie beträfe 0,18 % der Zeilen des Fensters und
  0,34 % der Zeilen mit Aufstieg im Bestand, immer auf **Stufe −2** und nie tiefer.

## M31‑3 Wie oft läuft `MatchInterchange`?

**Frage.** [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3 und
[`datenmodell.md`](datenmodell.md) §6 nennen den Takt **„stündlich"**;
[`verkettung.md`](verkettung.md) §9 und §11 begründen den fehlenden Hinweis in der Oberfläche damit,
dass er **nicht gemessen** sei. Hat die Angabe eine Quelle, die von hier aus erreichbar ist?

### Erst direkt fragen

```sql
SELECT EVENT_SCHEMA, EVENT_NAME, EVENT_TYPE, INTERVAL_VALUE, INTERVAL_FIELD,
       STARTS, LAST_EXECUTED, STATUS FROM information_schema.EVENTS;
SHOW EVENTS FROM GlassfishDB;
SELECT @@global.event_scheduler;
```

| Abfrage | Antwort |
|---|---|
| `information_schema.EVENTS` | **leer** — `COUNT(*)` = 0, kein Fehler |
| `SHOW EVENTS FROM GlassfishDB` | **`ERROR 1044 (42000): Access denied for user 'monitor_read'@'%' to database 'GlassfishDB'`** |
| `@@global.event_scheduler` | **`ON`** |

**Die leere Menge ist nachweislich ein Rechteartefakt und kein Befund über den Bestand.** Der
Auftrag hat mit „leer statt Fehler" gerechnet; gemessen kommt **beides** — `information_schema.EVENTS`
antwortet leer, weil die Sicht auf die Rechte des Benutzers gefiltert ist, und dieselbe Frage über
`SHOW EVENTS` wird **ausdrücklich verweigert**. Die Verweigerung ist die bessere Auskunft: Sie
belegt, dass die leere Antwort nichts über die Existenz von Events sagt. Dazu läuft der Planer
(`event_scheduler = ON`) — Events **können** auf dieser Instanz laufen, wir sehen sie nur nicht.

### Dann die Wirkung messen

Das Event setzt `MessageStatus = 'COMMIT_RECEIVED'`. Läuft es getaktet, ballen sich die
Schreibzugriffe auf wenigen Minuten der Stunde.

**Umfang, ohne Fenster** (Regel L9 — ein Fenster kann die Frage nach dem Takt über die Laufzeit des
Bestands nicht beantworten; Kosten unten ausgewiesen und klein, weil `MessageStatusIDX` greift):

| | Ergebnis |
|---|---|
| `COMMIT_RECEIVED` im Bestand | **n = 12.654** |
| Zeitraum | `2024-10-01 05:08:33` bis `2025-12-27 05:09:35` — **408 Tage** |
| belegte Stunden | **1.995** von rund 9.800 des Zeitraums |

> Der Auftrag nennt „rund 10.126 Zeilen". Gemessen sind **12.654**. Die Abweichung ist nicht
> aufgelöst; gerechnet wird mit der gemessenen Zahl.

**Kontrollgruppe: `FINISHED`, den kein Event schreibt.** Stichprobe ist **Fenster B** — dieselbe
Definition wie oben, **139.474** Zeilen, und die Zahl deckt sich exakt mit M23‑2 (b).

| | `COMMIT_RECEIVED` (Bestand) | `FINISHED` (Fenster B) |
|---|---:|---:|
| Zeilen | 12.654 | 139.474 |
| belegte Minuten von 60 | **60** | **60** |
| Schnitt je Minute | 210,9 | 2.324,6 |
| kleinste Minute | 94 (Minute 19) | 696 (Minute 36) |
| größte Minute | 625 (Minute 9) | 6.894 (Minute 8) |
| **größte / Schnitt** | **2,96** | **2,97** |

**Beide Verteilungen sind gleich ungleich.** Kein Wert der `COMMIT_RECEIVED`-Verteilung hebt sich
ab, wie es ein Takt verlangte — bei einem stündlichen Event stünden rund 12.000 der 12.654 Zeilen
auf einer oder zwei Minuten. Stattdessen ist **jede** der 60 Minuten belegt, und die Kontrollgruppe
streut in genau demselben Verhältnis.

### Der schärfere Test, und warum er zunächst in die Irre führt

Ein Takt muss sich nicht in der **aggregierten** Minutenverteilung zeigen: Läuft das Event zu einer
Minute, die sich von Stunde zu Stunde verschiebt, mittelt das Aggregat sie weg. Der Test dafür ist
die Zahl **verschiedener Minuten je Stunde**:

| verschiedene Minuten je Stunde | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10–17 |
|---|--:|--:|--:|--:|--:|--:|--:|--:|--:|--:|
| Stunden mit `COMMIT_RECEIVED` | **806** | 452 | 293 | 133 | 94 | 59 | 41 | 38 | 23 | 56 |

**In 806 von 1.995 Stunden (40,4 %) liegen alle `COMMIT_RECEIVED`-Zeilen dieser Stunde in einer
einzigen Minute** — das sieht nach Takt aus. **Es ist aber ein Artefakt des Aufkommens:** Diese 806
Stunden tragen zusammen nur 2.143 Zeilen, also **2,66 je Stunde**. Wer drei Zeilen in einer Stunde
hat, trifft leicht dieselbe Minute. `FINISHED` hat in Fenster B **nie weniger als 8** verschiedene
Minuten je Stunde — bei 194 Zeilen je Stunde.

**Die Kontrollgruppe muss deshalb auf das Aufkommen passen, und `FINISHED` tut das nicht:** Über
denselben Zeitraum von 408 Tagen hat `FINISHED` ganze **fünf** Stunden mit 2 bis 12 Zeilen. Als
volumengleiche Kontrolle ist der Status unbrauchbar.

### Der volumengleiche Vergleich — und er entscheidet

Deshalb über **alle** Status, beschränkt auf Stunden mit **2 bis 12 Zeilen** — die Größenordnung, in
der `COMMIT_RECEIVED` lebt. „erwartet" ist die Zahl verschiedener Minuten bei Gleichverteilung über
60, also `60 × (1 − (59/60)^n)`; sie ist ein **Modell und keine Messung**, und die Aussage steckt im
Vergleich der Status untereinander, nicht im Modellwert.

| `MessageStatus` | Stunden | Zeilen | Minuten beobachtet | erwartet | **Verhältnis** |
|---|---:|---:|---:|---:|---:|
| `COMMIT_REJECTED` | 24 | 73 | 26 | 71 | **0,365** |
| `COMMIT_SENT` | 197 | 656 | 326 | 635 | **0,514** |
| **`COMMIT_RECEIVED`** | 1.150 | 5.599 | 3.189 | 5.345 | **0,597** |
| `MERGED` | 1.249 | 7.731 | 4.385 | 7.301 | **0,601** |
| `CHECKED` | 43 | 141 | 86 | 137 | **0,629** |
| `SPLITTED` | 3.825 | 24.188 | 17.700 | 22.860 | **0,774** |
| `EERP_RECEIVED` | 2.515 | 12.048 | 9.528 | 11.469 | **0,831** |

**`COMMIT_RECEIVED` ballt sich — und `MERGED` ballt sich genauso.** 0,597 gegen 0,601, bei einem
Status, den **kein** Event schreibt und der in derselben Datei aus derselben Verarbeitung entsteht.
Die Ballung ist die des EDI-Verkehrs und nicht die einer Uhr. Zwei Status ballen sich sogar
deutlich stärker als `COMMIT_RECEIVED`.

Zuletzt die Stunde des Tages: `COMMIT_RECEIVED` belegt **alle 24**, von 51 Zeilen (Stunde 6) bis
1.745 (Stunde 23). Auch das schließt einen stündlichen Takt nicht aus — und belegt ihn nicht.

### Ergebnis von M31‑3

**Der Takt ist nicht belegt.** Weder die aggregierte Minutenverteilung noch der volumengleiche
Ballungsvergleich unterscheidet `COMMIT_RECEIVED` von Status, die kein Event schreibt. Die direkte
Auskunft ist verschlossen, und das nachweislich durch fehlende Rechte und nicht durch Abwesenheit.

**Damit steht in der verbindlichen Datei ein ungedeckter Satz** — das ist der eigentliche Befund
dieses Abschnitts. `PROJEKTBESCHREIBUNG.md` §3.3 und `datenmodell.md` §6 nennen „stündlich" als
Tatsache; von hier aus ist die Angabe **weder bestätigt noch widerlegt**. Ihre Quelle liegt außerhalb
der Testkopie — beim Altsystem, dem das Event gehört. **Nichts daran wird hier geändert; die
Feststellung gehört ins Sparring.**

### Belegvermerk (Regel L10)

> *Gemessen:* Die Verteilung von `MessageLastUpdate` über die Minute der Stunde, für
> `COMMIT_RECEIVED` (n = 12.654, ganzer Bestand, 408 Tage) und für sechs Kontrollstatus bei
> gleichem Aufkommen (Stunden mit 2 bis 12 Zeilen).
>
> *Behauptet wird:* dass daraus etwas über den **Takt des Events** folgt.
>
> **Die Lücke, und sie ist grundsätzlich:** `MessageLastUpdate` ist der Zeitpunkt der **letzten**
> Änderung, nicht der des Event-Schreibzugriffs. Die Spalte zeigt den Takt nur insoweit, als der
> Schreibzugriff des Events der letzte auf dieser Zeile war. Schreibt danach irgendetwas noch einmal
> auf die Zeile, verwischt die Ballung. **Die Messung kann den Takt also belegen, wenn sie ihn
> zeigt — aber nicht ausschließen, wenn sie ihn nicht zeigt.** Sie zeigt ihn nicht. Der Satz
> „`MatchInterchange` läuft nicht stündlich" ist damit **nicht** gemessen und wird hier auch nicht
> behauptet. Gemessen ist ausschließlich: *Von hier aus ist der Takt nicht sichtbar.*
>
> Eine zweite, kleinere Lücke: Die Kontrollgruppe `FINISHED` steht über **Fenster B**, die
> Hauptmessung über den **ganzen Bestand**. Das ist Absicht (n = 12.654 gegen 1,66 Mio.) und
> ausgewiesen; der volumengleiche Vergleich läuft dagegen für **alle** Status über den ganzen
> Bestand und hebt den Unterschied auf.

## Laufzeiten dieser Runde

Serverseitig über `SET profiling = 1`, beste von fünf nach einem Aufwärmlauf, wo nicht anders
vermerkt.

| Messung | Fenster | Laufzeit |
|---|---|---:|
| M31‑0 Kontrolle (Rollenkombinationen) | B | 3,009 s |
| M31‑1 Zähler | B | 1,337 s |
| M31‑1 Nenner | B | 1,217 s |
| M31‑1 je Mandant | B | 1,671 s |
| M31‑2 Zähler | B | 1,519 s |
| M31‑2 Nenner | B | 1,262 s |
| M31‑2 je Mandant | B | 2,316 s |
| M31‑1/2 Stufenverteilung | B | 2,017 s |
| Ansichten je Zickzack-Knoten, Eingangsseite | **ohne** | 0,699 s |
| `SPLIT_WURZEL` + `MERGE_EINGANG` im Bestand | **ohne** | 1,263 s |
| Die 33 Knoten und ihre Eingänge | **ohne** | **≈ 1,29 s** (Wanduhr, siehe unten) |
| Ansichten je Zickzack-Knoten, Knotenseite | **ohne** | **≈ 1,42 s** (Wanduhr, siehe unten) |
| M31‑3 Minutenverteilung `COMMIT_RECEIVED` | **ohne** | 0,073 s |
| M31‑3 Minutenverteilung `FINISHED` | B | 1,439 s |
| M31‑3 Minuten je Stunde, `COMMIT_RECEIVED` | **ohne** | 0,110 s |
| **M31‑3 Ballungsvergleich über alle Status** | **ohne** | **19,313 s**, beste von 3 |

Kein Statement über 60 Sekunden; das teuerste ist der Ballungsvergleich mit 19,3 s und ist ein
voller Durchlauf über `Message` — `ANALYZE` weist `type: ALL` mit `r_rows` = **3.341.519** aus, aus
denen 9.026 Gruppen aus Status und Stunde entstehen. Es ist eine **einmalige Erhebung** und **kein**
Statement der Anwendung. Die Frage nach dem Takt über 408 Tage lässt sich mit keinem Fenster
beantworten (Regel L9), und die Kosten stehen hier.

> ⚠️ **Zwei Statements sind mit der Wanduhr gemessen und nicht über `SHOW PROFILES`** — und der
> Grund gehört dazu. Beide tragen eine **korrelierte Unterabfrage** (`DEPENDENT SUBQUERY` über
> `TargetMessageIDIDX`), und für diese Form meldet `SHOW PROFILES` **0,035 s**, während dasselbe
> Statement 1,53 s Wanduhrzeit braucht — Faktor 40. `ANALYZE` weist den vollen Durchlauf aus
> (`r_rows` = 3.341.519, Unterabfrage `r_rows` = 9,98 je Zeile), die Arbeit findet also statt und
> wird nur nicht zugeschrieben. Angegeben ist deshalb die Wanduhrzeit **abzüglich des gemessenen
> Client-Anlaufs von rund 0,11 s** (fünf Läufe `SELECT 1`). Zur Kontrolle mit derselben Methode:
> M31‑1 Zähler kostet 1,363 s Wanduhr gegen 1,337 s im Profil — dort deckt sich beides, und alle
> übrigen Zeilen dieser Tabelle stehen deshalb wie gewohnt auf der Profilmessung.

## Was M31 ausdrücklich **nicht** misst

- **Ob der Aufstieg beim Beziehungswechsel abgebrochen wird.** Das ist eine Entscheidung nach den
  Zahlen und gehört ins Sparring zu Schritt 7.
- **Wie lange `MatchInterchange` nachläuft.** Gemessen ist, dass der Takt von der Testkopie aus
  nicht sichtbar ist — nicht, wie groß die Verzögerung ist. Ein Hinweis in der Oberfläche bliebe
  nach Regel Q4 weiter ungedeckt.
- **Woher die Angabe „stündlich" in `PROJEKTBESCHREIBUNG.md` §3.3 stammt.** Die Frage gehört an das
  Altsystem; hier ist nur festgestellt, dass sie von der Datenbank aus nicht beantwortbar ist.
- **Der Bestand des Mandanten `NXHBE`.** Er taucht mit vier Zeilen auf und ist sonst in keiner
  Messung dieser Datei enthalten. Was das bedeutet, ist nicht erhoben.
- **Die Abweichung von 400 Zeilen in M30‑3.** Sie ist benannt und nicht verfolgt.

---

## Zusammenfassung: das Bild der Verkettung

```
  Aufteilung (SourceMessageID + Source)        Zusammenfuehrung (TargetMessageID + Target)

        Wurzel  Source = 1                       Eingang 1  ─┐  TargetMessageID
   meist SPLITTED, traegt 96,9 % der BAM         MERGED      │
        │                                        Eingang 2  ─┤  0 % BAM
        │ SourceMessageID der Kinder             MERGED      │
        ▼                                        …          ─┘
   Kind  Kind  Kind  …  (bis 3.048)                   ▼
   meist FINISHED, 2,4 % BAM                    Ergebnis  Target = 1
                                                meist EERP_RECEIVED, traegt die BAM
                                                Grad 23 : 1 — und selbst wieder teilbar (E2)
```

| Frage | Antwort |
|---|---|
| Wer ist der Elternteil beim Split? | Die Zeile mit `Source = 1`, meist `SPLITTED` — **ausgeblendet** |
| Wer trägt die Belegnummer beim Split? | **Der Elternteil** (96,9 % gegen 2,4 %) |
| Wer ist das Ergebnis beim Merge? | Die Zeile mit `Target = 1`, meist `EERP_RECEIVED` — **sichtbar** |
| Wer trägt die Belegnummer beim Merge? | **Das Ergebnis**; der Eingang trägt keine |
| Wie breit wird es? | Bis **3.350** Kinder und bis **897** Merge-Eingänge über den Bestand (M30‑2); 87 % der Wurzeln haben genau ein Kind |
| Wie oft greift eine Grenze von 50? | Bei **1,07 %** der Wurzeln, aber bei **11,38 %** der Merge-Ergebnisse (M30‑2) |
| Wie tief? | **Genau vier Ebenen**, drei Aufstiegsschritte (M30‑3); Stufe fünf ist leer |
| Gibt es einen Zyklus? | **Nein** — Stufe 5 leer, null Selbstverweise (M30‑3). Der Schutz bleibt trotzdem |
| Was kostet die Auflösung? | Ein Glied **0,5 ms**, die Seite im Breitfall **26 ms**, die Zählung **19,8 ms** (M30‑1) |
| Bleibt die Kette im Mandanten? | **Ja**, ausnahmslos (M27) |
| Reicht eine Spalte? | **Nein.** Die beiden **ID-Spalten** schließen einander je Zeile aus (M28‑1: `Kind + Eingang` = 0), aber beide Beziehungen kommen vor |
| Trägt eine Zeile mehrere Rollen? | **Selten, aber nicht nie:** 514 von 214.330 = 0,240 %; bei `IBISGUS` 1,278 %. Nie mehr als zwei (M28‑1c) |
| Was blendet ein Stellungsprädikat aus? | **66,80 %** gegen heute 31,15 % — mehr als das Doppelte; bei `IBISGUS` 100 % (M28‑1) |
| Gibt es einen BAM-Typ, der auf Wurzeln „immer" sitzt? | **Ja:** `NEXANS` 92,26 % (9018), `IBIS` 92,59 % (Typ 0). Auf Kindern dagegen 0,72 % (M28‑2) |

## Alle Laufzeiten

| Messung | Fenster | Laufzeit | Wiederholungen |
|---|---|---:|---|
| M23‑2 (a) | A | 0,059 s | beste von 5 |
| M23‑2 (b) | B | 1,795 s | beste von 5 |
| M24‑1 | A + Überhang | 0,066 s | beste von 5 |
| M24‑2 | B + Überhang | 1,581 s | beste von 5 |
| M24‑3 | B + Überhang | 2,003 s | beste von 5 |
| M25‑1 | B | 1,668 s | beste von 5 |
| M25‑2 | A | 0,036 s | beste von 5 |
| M26‑1 | A | 0,152 s | beste von 5 |
| M26‑2 | A | 2,391 s | beste von 5 |
| M26‑3 | A | 0,165 s | beste von 5 |
| **M27** | **ohne** | **29,210 s** | beste von 3 |
| E4 | A | 0,223 s | beste von 5 |
| M28‑1 (a) | B | 3,744 s | beste von 5 |
| M28‑1 (b) | A | 0,109 s | beste von 5 |
| **M28‑2** | **B** | **12,682 s** | beste von 3 |
| M30‑1 Richtung 1 / 3 (PK) | — | 0,505 / 0,482 ms | beste von 5 |
| M30‑1 Richtung 2, Breitfall | — | **25,995 ms** | beste von 5 |
| M30‑1 Richtung 2, Normalfall | — | 0,653 ms | beste von 5 |
| M30‑1 Richtung 4, Breitfall | — | 5,659 ms | beste von 5 |
| M30‑1 Richtung 4, Normalfall | — | 0,596 ms | beste von 5 |
| M30‑1 Richtung 5a / 5b, Breitfall | — | **18,659 / 4,197 ms** | beste von 5 |
| M30‑1 Richtung 5a / 5b, Normalfall | — | 0,544 / 0,505 ms | beste von 5 |
| M30‑1 Richtung 6, drei Ebenen | — | 1,441 ms zusammen | beste von 5 je Ebene |
| M30‑1 am Rand des Bestands (5a / 2) | — | **19,760 / 26,358 ms** | beste von 5 |
| M30‑2 Breite der Wurzeln | **ohne** | 1,701 s | einmalig |
| M30‑2 Breite der Merge-Ergebnisse | **ohne** | 1,472 s | einmalig |
| M30‑2 Selbstverweise | **ohne** | 1,514 s | einmalig |
| M30‑6 Zeilen ohne Zeitpunkt | **ohne** | 1,159 s | einmalig |

Kein Statement über 60 Sekunden. M27 ist mit Abstand das teuerste und bleibt eine einmalige
Erhebung; M28‑2 ist das zweitteuerste und trifft die Kostenerwartung aus M11 (12,7 s) auf 18 ms.

**Die Statements der Anwendung stehen alle im Millisekundenbereich.** Der teuerste Ketten-Aufruf des
gesamten Bestands — breiteste Wurzel, Seite plus Zählung plus die Merge-Richtung plus drei
Aufstiegsschritte — kostet zusammen unter **55 ms**. Die drei Erhebungen aus M30‑2 laufen ohne
Zeitfenster (Regel L9) und sind trotzdem billig, weil sie den Sekundärindex vollständig lesen und die
Tabelle nie anfassen.

> **M28‑1c ist nicht eigens profiliert.** Es ist dasselbe Statement wie M28‑1 mit einer anderen
> Gruppierung und derselben `EXPLAIN`-Gestalt; eine eigene Zeile wäre eine Zahl ohne eigene Aussage.

> **Diese Tabelle umfasst M23 bis M30 und ist mit dem 10.08.2026 abgeschlossen.** Die Laufzeiten des
> am 11.08.2026 erhobenen Blocks **M31** stehen in dessen eigener Tabelle („Laufzeiten dieser
> Runde"), weil sie an einem anderen Tag und teilweise mit einer anderen Methode entstanden sind.
> Auch dort liegt kein Statement über 60 Sekunden; das teuerste kostet 19,3 s.

---

## Was diese Runde ausdrücklich **nicht** misst

- **Ob `RUNNING` in der Kette vorkommt.** Der Status existiert in der Testkopie null Mal
  ([`message-status.md`](message-status.md)). Bleibt offen bis zu einer Stichprobe gegen die
  Produktion.
- **Ob `MoveDTNA997` die Ursache der Prozesswechsel ist.** M27 prüft die **Folge** für die
  Mandantengrenze — mehr braucht Schritt 6.
- **Ob die Vorgabe `zwischenschritte` gedreht wird.** Das ist eine fachliche Entscheidung nach den
  Zahlen, keine Messung. Sie fällt im Sparring zu Schritt 6. Die Zahlen dafür stehen jetzt
  vollständig hier; die Entscheidung steht bewusst **nicht** in diesem Dokument.
- **Wo der `_MERGED`-Anhang aus dem Produktions-Screenshot tatsächlich sitzt.** Auf `SOS.SOSName`
  nicht (M25‑1). Vermutlich auf dem Datei-/Nachrichtennamen — das ist eine Vermutung und wird nicht
  als Befund geführt.

## Was unabhängig von jedem Ergebnis feststeht

1. **`istEndstatus` bleibt unverändert.** `SPLITTED` und `MERGED` sind als *Zeile* fertig; das stützt
   M6 und keine Messung dieser Runde berührt es.
2. **Das Wort „Zwischenschritt" verschwindet aus der Oberfläche.** Es ist eine Behauptung über
   Relevanz, und genau die war ungemessen. Ersatz: zwei getrennte Aussagen — „aufgeteilt" und
   „zusammengeführt". M26 belegt jetzt, **warum** der gemeinsame Eimer schadet: Für den Split kostet
   das Ausblenden 96,9 Prozent der Belegnummern, für den Merge kostet es keine einzige.

## Offene Fragen für das Sparring zu Schritt 6

1. **Wird `zwischenschritte=false` gedreht, ersetzt oder abgeschafft?** Die Vorgabe blendet beim
   Split die Zeile aus, die der Nutzer sucht — und beim Merge eine, die er nicht vermisst.
2. **Woran macht die Liste die Sichtbarkeit fest — am Status oder an der Stellung?** M24‑3 zeigt
   Ketten bei drei Mandanten, die keinen einzigen Zwischenschritt haben. Ein Kriterium über den
   Status erfasst sie nicht; `Source = 1` erfasst sie (E4).
3. **Welche Breitengrenze gilt im Detailpanel, und wohin führt der Sprung darüber hinaus?** Bei
   3.048 Kindern ist eine aufklappbare Liste keine Darstellung.
4. **Wird der Chip-Text und die `merkmale`-Auskunft angepasst?** Sie beruhen auf
   `SPLITTED`/`MERGED`; nach M24‑3 beschreibt das die Verkettung nicht.
5. **Zeigt die Detailansicht BAM-Werte — und mit welcher Deckelung?** Beim Merge-Ergebnis sind es im
   Schnitt 197,67 Werte je Nachricht, im Höchstfall 405.
6. **Was passiert mit den kuratierten Listenspalten (E1)?** Sie sind auf der Vorgabe-Ansicht leer.
   Das ist unabhängig von der Ketten-Darstellung und betrifft Schritt 4.

### Was Block 2 (M28) an diesen Fragen geändert hat

- **Frage 1 und 2 sind schwerer geworden, nicht leichter.** Ein Stellungsprädikat blendet **66,80 %**
  aus statt heute 31,15 %, bei `IBISGUS` **100 %** und bei `ZAST` 92,93 %. Wer die Vorgabe dreht,
  muss diesen beiden Mandanten erklären, warum ihre Liste leer ist — heute sehen sie jede Zeile.
- **Frage 2 hat eine dritte Kandidatenform bekommen.** P1 und P2 unterscheiden sich um 456 Zeilen
  (0,213 %); die Wahl ist damit eine Abwägung und keine Ableitung. Die Zahl entscheidet sie nicht.
- **Frage 6 ist keine Aufräumfrage mehr, sondern eine Rücknahme** — begrenzt auf die Wurzeln: Dort
  gibt es bei `NEXANS` einen Typ mit 92,26 % (9018) und bei `IBIS` einen mit 92,59 %. Für die heute
  gezeigte Menge bleibt der Satz aus §6 richtig.
- **Neu offen:** Die Kuratierung in `V4__bam_spalte.sql` (9006 / 9001) wurde ohne Rücksicht auf die
  Rolle gewählt und trifft auf den Wurzeln 5,02 % beziehungsweise 11,79 %. 9001 trifft dagegen
  95,87 % der Merge-Ergebnisse. Ob und wie die Kuratierung rollenabhängig wird, ist eine neue Frage
  und gehört zu Frage 6.
- **Bestätigt:** Die Rolle als **Menge** ist Pflicht (`IBISGUS` 1,278 % Doppelrollen), aber nie mehr
  als **zwei** Rollen je Zeile — eine Modellierung, die drei oder vier zulässt, hat keinen Beleg.
