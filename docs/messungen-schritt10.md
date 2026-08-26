# Messungen vor Schritt 10a — Rollup, Dashboard-Leseabfrage, Verteilungen

Erhoben am **26.08.2026** gegen die Testkopie (`GlassfishDB`, `overlord_monitor`).
Auftrag: „Messrunde vor Schritt 10a — Aufgabenstellung", Stand 24.08.2026, Nummernbereich **M86–M92**.

**Diese Runde baut nichts und entscheidet nichts.** Die Lesarten standen vor der Erhebung fest; sie
sind unten je Messung als *Vorregistrierte Deutung* mitgeführt und nach dem Ergebnis unverändert
dagegengehalten. Welche Bauform daraus wird, entscheidet der Auftraggeber.

**Die eine benannte Ausnahme vom Satz „baut nichts"** ist M89: eine Probetabelle
`overlord_monitor.message_rollup_probe`, angelegt, befüllt, gemessen und am Ende der Runde wieder
gelöscht. Die Löschung ist unten nachgewiesen.

---

## Nummernvergabe

| | |
|---|---|
| Prüfung | `grep -rnoE '\bM(8[6-9]\|9[0-9])\b' docs/ scripts/ *.md` |
| Ergebnis | **kein Treffer.** Der Bereich M86–M99 ist frei |
| Gegenprobe | `grep -rnoE '\bM(8[0-5])\b' docs/ scripts/ *.md` → **309 Treffer** (M80–M83 in `messungen-schritt9.md`, `annahmen-korrekturen.md`, `benutzerverwaltung-*.md`, `IMPLEMENTIERUNGSPLAN_MVP.md`). Der Ausdruck greift |
| Folge | **M86 bis M92 sind hier vergeben.** Keine Verschiebung, anders als bei M74 |

**Offene Punkte** setzen bei **41** an. Projektweit höchste vergebene Nummer ist **40**
(`messungen-schritt9.md` Z. 870); `grep -rnE '^(4[0-9]|5[0-9])\. \*\*' docs/*.md` findet außer dieser
Zeile nichts.

**Die „Befunde, die in keine vorformulierte Zeile passten"** setzen bei **6** an — der gleichnamige
Abschnitt in `messungen-schritt9.md` (Z. 1898–1909, M83-Nachtrag) führt fünf.

---

## Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` — niemals die Produktion |
| Nachweis Testkopie | `SELECT @@global.read_only` → **`1`**, als **erste Abfrage jeder Sitzung** und in der Schlusssitzung erneut. In allen Sitzungen `1` |
| Benutzer | **`monitor_read@%`**, ausschließlich `SELECT`. Ausnahme M89, siehe dort |
| Sitzungen | sequenziell, jede eine eigene `mysql`-Ausführung mit einer Skriptdatei unter `scripts/messung-schritt10/` |
| Serverzeit Beginn | `2026-08-26 09:45:52` |
| Serverzeit Ende | siehe Sitzung 8 |
| Client | `mysql.exe` **Ver 8.0.46** aus MySQL Workbench 8.0 CE, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t`. **Abweichung vom Rahmen, siehe unten** |
| Passwortübergabe | über `MYSQL_PWD` aus `OVERLORD_DB_READ_PASSWORD` — kein Passwort auf der Befehlszeile, keines in einer Skriptdatei |
| Grenze | `SET max_statement_time = 60` in jeder Messsitzung. **Einmal gerissen** (M87‑5), siehe dort — nicht hochgesetzt, sondern in Jahresscheiben gefahren |
| Laufzeit | `SET profiling = 1`, `SET profiling_history_size = 100` (Vorgabe der Instanz ist **15**), Auswertung über `SHOW PROFILES` |
| `@@div_precision_increment` | **4** — deshalb steht in keiner Abfrage dieser Runde ein `AVG` über einen Wahrheitswert; Anteile sind als `100.0 * SUM(…) / COUNT(*)` gerechnet |
| `@@session.sql_mode` | `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION` |
| `@@max_statement_time` (Vorgabe) | **`0.000000`** — die Instanz kennt von sich aus keine Grenze; die 60 s sind allein die dieser Runde |
| `innodb_buffer_pool_size` | **26.843.545.600** Byte (25.600 MiB) |
| Datenstand | `MAX(Message.MessageLastUpdate)` = **`2026-07-08 17:21:10`** — unverändert gegenüber M0 und M83 |

### Regelbezug — erfüllt oder nicht

| Regel | Stand | Begründung |
|---|---|---|
| **S1** — nur `SELECT` | **erfüllt mit der benannten Ausnahme** | Sitzungen 1–4 und 6–7 fahren ausschließlich `SELECT`, `SET`, `EXPLAIN`, `SHOW`. Sitzung 5 (M89) legt die vom Auftrag freigegebene Probetabelle an und befüllt sie; Sitzung 8 löscht sie. **Kein Schreibzugriff auf `GlassfishDB` in irgendeiner Sitzung** |
| **L4** — `MessageProperty` | **erfüllt** | Die Tabelle wird in dieser Runde **gar nicht** angefasst. Einziges Vorkommen ist ihre Größe in V1, aus `information_schema` |
| **L7** — zwei Mandanten | **erfüllt** | M89, M90 je für `NEXANS` und `SUTTONS`; M91 zusätzlich für `VOTG`. M86, M87, M88 und M92 sind mandantenübergreifend und wären mit einem Mandantenfilter etwas anderes — begründet je Messung |
| **L9** — voller Durchlauf | **erfüllt** | Jeder volle Durchlauf ist einzeln begründet: M87 (die Zeilenzahl einer Tabelle, die es nicht gibt, ist anders nicht zu bekommen), M92‑1 (Scheibengrenzen über den Bestand). **Keiner ist Vorbild für Anwendungscode**, und das steht an jeder Fundstelle |
| **L10** — *gemessen war X / behauptet wird Y* | **erfüllt** | In V1, V2, V3, V6, M86 (a) und im Abschnitt „Drei Angaben des Auftrags, die der Bestand nicht trägt" |
| **L15** — `EXPLAIN` im Volltext | **erfüllt** | Je Messabfrage; die Pläne stehen ungekürzt |
| **G1** — Geheimhaltung | **erfüllt** | Keine `ProcessID`, kein `ProcessName`, kein Partnername, keine `MessageID`, keine Belegnummer, kein Hostname, kein `MessagePropertyValue`. M91 gibt Partner und Prozesse **maskiert** als `Partner 1`, `Partner 2` … aus; die Zuordnung zu echten Namen ist **nirgends festgehalten**, auch nicht in den Sitzungsdateien |
| **Z1** — kein `NOW()` | **erfüllt** | Jeder Zeitpunkt steht als **Literal im Statement**. `NOW()` kommt ausschließlich in den Rahmenzeilen (`SELECT NOW() AS serverzeit`) vor, nie in einer Messabfrage |

### Die Sitzungen

| # | Datei | Inhalt | Serverzeit |
|---|---|---|---|
| 1 | `s1-rahmen-v1-v5.sql` | Rahmen, V1–V5, Nummernprobe | 09:45:52 – 09:46:27 |
| 2 | `s2-m86-wandert-lastupdate.sql` | M86 (a), (b), (c) | 09:50:36 – |
| 3 | `s3-m87-rollupzeilen.sql` | M87, vier Varianten | 09:56:25 – |
| 3b | `s3b-m87-jahresscheiben.sql` | M87‑5 in Jahresscheiben, nachdem die Grenze gerissen war | siehe dort |

Die Rohausgaben liegen unter `scripts/messung-schritt10/ergebnis/` und sind über `.gitignore`
**vom Repository ausgeschlossen** — dieselbe Trennung wie bei Schritt 8 und 9: die `.sql`-Sitzungen
gehören ins Repository, sie sind der Beleg dafür, wie gemessen wurde; die Laufergebnisse nicht, weil
sie vollständige Prozess- und Projektkennungen und damit Partnernamen Dritter tragen.

---

# V. Die Vorbedingungen

## V1 — Ist die Testkopie seit dem 24.08.2026 unverändert?

**Byteidentisch. Ja.**

```sql
SELECT TABLE_NAME, ENGINE, TABLE_ROWS, DATA_LENGTH, INDEX_LENGTH,
       DATA_LENGTH + INDEX_LENGTH AS gesamt_bytes,
       ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 1) AS gesamt_mib,
       DATA_FREE, AVG_ROW_LENGTH, UPDATE_TIME, CREATE_TIME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'GlassfishDB'
  AND TABLE_NAME IN ('Message','MessageAction','MessageBAM','MessageProperty')
ORDER BY TABLE_NAME;
```

| Tabelle | `DATA_LENGTH` | `INDEX_LENGTH` | Summe MiB | gegen frühere Runde |
|---|---:|---:|---:|---|
| `Message` | 740.851.712 | 2.157.330.432 | 2.763,9 | **byteidentisch** mit `messungen-schritt7.md` §0 |
| `MessageAction` | 2.226.634.752 | 819.855.360 | 2.905,4 | **byteidentisch** mit §0 |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 | 6.752,6 | **byteidentisch** mit §0 |
| `MessageProperty` | 15.088.615.424 | 45.945.946.112 | 58.207,1 | **byteidentisch** mit M44‑0 (§0 führt sie nicht) |

`UPDATE_TIME` steht für `Message`, `MessageAction` und `MessageProperty` auf `2026-07-08 17:21:10`,
für `MessageBAM` auf `2026-06-18 13:56:08`. **Diese Runde darf gegen M0…M85 gehalten werden.**

> **Belegvermerk** (Regel L10).
> *Gemessen war:* `messungen-schritt7.md` §0 führt **drei** Tabellen, nicht vier — `MessageProperty`
> fehlt dort vollständig. Die vierte Zeile steht in **M44‑0** derselben Datei (Z. 2489), dort
> ausdrücklich als „byteidentisch mit M14".
> *Behauptet wird* in der Aufgabenstellung: „`DATA_LENGTH`/`INDEX_LENGTH` für `Message`,
> `MessageAction`, `MessageBAM`, `MessageProperty` gegen `messungen-schritt7.md` §0 halten."
> **Die Lücke:** Für `MessageProperty` gibt es in §0 nichts zu halten. Der Vergleich ist gegen M44‑0
> gefahren und trägt genauso — er ist nur eine andere Fundstelle.

**Zeilenzahlen, gezählt statt geschätzt.** `information_schema.TABLES` liegt weiterhin daneben:

| Tabelle | `TABLE_ROWS` (geschätzt) | gezählt | Abweichung |
|---|---:|---:|---:|
| `Message` | 3.560.486 | **3.341.519** | +6,55 % zu hoch |
| `MessageAction` | 10.215.743 | **10.308.590** | −0,90 % zu niedrig |
| `MessageBAM` | 10.859.666 | **15.406.350** | −29,51 % zu niedrig |

Alle drei gezählten Werte stimmen mit dem Mengengerüst in `PROJEKTBESCHREIBUNG.md` §8 überein.

## V2 — Was trägt `process_catalog` heute?

**1.160 Zeilen, 770 davon gepflegt.** Die Tabelle ist seit M80 nicht mehr leer: dort war sie mit
1.490 Zeilen über den Codepfad gefüllt, gemessen und wieder geleert. Was heute darin steht, sind die
kuratierten Zeilen der Sichtprüfung vom 24.08.2026.

| | Zeilen |
|---|---:|
| `COUNT(*)` | **1.160** |
| `COUNT(partner)` — Partner nicht `NULL` | 932 |
| davon zusätzlich nicht leer (`partner <> ''`) | **932** — es gibt keinen leeren Zeichenketten-Partner |
| `pflegestatus = 'GEPFLEGT'` | **770** |
| `pflegestatus = 'OFFEN'` | 390 |
| `GEPFLEGT` **und** Partner leer/`NULL` | **216** |
| `COUNT(richtung)` | 759 (387 `EINGEHEND`, 372 `AUSGEHEND`) |
| verschiedene Partner | **321** |

| `pflegestatus` | `vorschlag_herkunft` | Anzahl |
|---|---|---:|
| `GEPFLEGT` | `REGEL_A` | 509 |
| `GEPFLEGT` | `KEINE` | 261 |
| `OFFEN` | `REGEL_A` | 378 |
| `OFFEN` | `KEINE` | 12 |

> **Zwei Dinge, die diese Zahlen sagen und die M91 braucht.**
>
> 1. **Der Katalog deckt den Prozessbestand nicht.** 1.160 Katalogzeilen stehen **1.503** Prozessen
>    gegenüber (V4c) — **343 Prozesse haben überhaupt keine Katalogzeile.** Ein Join auf
>    `process_catalog` verliert sie, wenn er nicht als `LEFT JOIN` gebaut ist. M91 fährt deshalb
>    `LEFT JOIN` und weist die Zeilen ohne Katalogzeile getrennt aus.
> 2. **„Partner nicht `NULL`" ist nicht „gepflegt mit Partner".** 932 Zeilen tragen einen Partner,
>    aber nur **554** davon sind auch `GEPFLEGT` (770 − 216). Die übrigen **378** sind
>    `OFFEN` **mit** einem Vorschlag aus `REGEL_A` — ein Vorschlag, den niemand bestätigt hat.
>    Nach **E‑i** zählen sie zu *nicht zugeordnet*, obwohl ein Name darin steht.

**Nach E‑i sind damit 606 der 1.160 Katalogzeilen „nicht zugeordnet"** (390 `OFFEN` + 216 gepflegt
ohne Partner) — **52,24 %**. Rechnet man die 343 Prozesse ohne Katalogzeile hinzu, sind es **949 von
1.503 Prozessen** oder **63,14 %**. Das ist die Bezugsgröße, gegen die M91 zu lesen ist.

## V3 — Welche Indizes trägt `Message` wirklich?

**Acht.** `SHOW INDEX FROM GlassfishDB.Message` und `information_schema.STATISTICS` stimmen überein:

| Index | Spalten | eindeutig | `CARDINALITY` |
|---|---|---|---:|
| `PRIMARY` | `MessageID` | **ja** | 3.560.486 |
| `MessageLastUpdateIDX` | `MessageLastUpdate` | nein | 1.780.243 |
| `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdate, ProcessID, MessageID` | nein | 1.780.243 / 3.560.486 / 3.560.486 |
| `MessageStatusIDX` | `MessageStatus` | nein | **18** |
| `Message_ProcessFK` | `ProcessID` | nein | **18** |
| `ProejctIDIDX` *(sic)* | `ProcessID` | nein | **18** |
| `SourceMessageIDIDX` | `SourceMessageID` | nein | 1.780.243 |
| `TargetMessageIDIDX` | `TargetMessageID` | nein | 63.580 |

Zeichen für Zeichen dieselbe Liste wie M83‑0. **Der Anker für M88 steht.**

> **Belegvermerk** (Regel L10) — **die Vorbedingung selbst ist überholt.**
> *Gemessen war:* `PROJEKTBESCHREIBUNG.md` §3.2 führt heute **acht** Indizes, in einer Tabelle mit
> Korrekturkasten vom **20.08.2026** („Hier standen **drei** Indizes, es sind **acht**").
> *Behauptet wird* in der Aufgabenstellung: „`PROJEKTBESCHREIBUNG.md` §3.2 nennt **drei**, M83 hat
> **acht** gefunden (offener Punkt 33)."
> **Der Widerspruch ist zugunsten der Datei aufzulösen, nicht zugunsten des Auftrags:** Die Korrektur
> ist am 20.08.2026 erfolgt, vier Tage vor Abfassung dieses Auftrags. Der erwartete Widerspruch
> zwischen Datei und Datenbank **besteht nicht mehr**. Was bleibt, ist der Erledigt-Vermerk an
> offenem Punkt 33 selbst (Z. 827), der noch fehlt — siehe offener Punkt 41.

**Zwei deckungsgleiche Indizes auf `ProcessID`** (`Message_ProcessFK` und `ProejctIDIDX`) bestehen
unverändert; das ist Befund 1 aus M83 und keine Neuigkeit dieser Runde.

## V4 — Wie heißen die Spalten der Mandantenkette wirklich?

Erhoben aus `information_schema.COLUMNS`, **nicht** aus einer Projektdatei.

| Tabelle | Pos. | Spalte | Typ | Null | Schlüssel |
|---|---:|---|---|---|---|
| `Process` | 1 | `ProcessID` | `varchar(36)` | NO | `PRI` |
| `Process` | 2 | `ProcessName` | `varchar(255)` | YES | |
| `Process` | 3 | `ProcessDescription` | `text` | YES | |
| `Process` | 4 | `ProjectID` | `varchar(36)` | **YES** | `MUL` |
| `Project` | 1 | `ProjectID` | `varchar(36)` | NO | `PRI` |
| `Project` | 2 | `ProjectName` | `varchar(255)` | YES | |
| `Project` | 3 | `ProjectDescription` | `text` | YES | |
| `ProjectMandant` | 1 | `ProjectID` | `varchar(36)` | NO | `PRI` |
| `ProjectMandant` | 2 | `MandantID` | `varchar(36)` | NO | `PRI` |

Alle Zeichenspalten tragen `utf8mb4_general_ci` — dieselbe Sortierung wie `process_catalog`, der
Join über die Schemagrenze ist also kollationsrein.

**Indizes der Kette:**

| Tabelle | Index | Spalten |
|---|---|---|
| `Process` | `PRIMARY` | `ProcessID` |
| `Process` | `Process_ProjectFK` | `ProjectID` |
| `Project` | `PRIMARY` | `ProjectID` |
| `ProjectMandant` | `PRIMARY` | `ProjectID, MandantID` |
| `ProjectMandant` | `ProjectMandant_Mandant_idx` | `MandantID` |

**Bezugsgrößen:** 1.503 Prozesse, 140 Projekte, 134 `ProjectMandant`-Zeilen, **10** Mandanten.

> **Ein Befund, der hier hingehört, weil er die Kette verkürzt.** `Project` trägt außer dem
> Schlüssel nur Name und Beschreibung. Die Kette `Process → Project → ProjectMandant` lässt sich
> deshalb zu `Process → ProjectMandant` über `Process.ProjectID = ProjectMandant.ProjectID`
> **verkürzen, ohne eine Zeile zu verlieren** — `Project` steuert zur Mandantenzuordnung nichts bei.
> M89 und M90 fahren trotzdem die **volle** Kette, weil der Auftrag sie so vorschreibt und weil der
> Plan sie ohnehin als `eq_ref` auf `PRIMARY` abräumt. Die Verkürzung ist notiert, nicht ausgeführt.
> Siehe Befund 6.

**`Process.ProjectID` ist `NULL`-bar.** Ein Prozess ohne Projekt hätte keinen Mandanten und fiele aus
jeder Kette heraus. Wie viele es sind, ist in M91 mitgezählt.

## V5 — Welcher Ankerzeitpunkt gilt als „jetzt"?

```sql
SELECT MAX(MessageLastUpdate) AS anker_v5, MIN(MessageLastUpdate) AS bestand_beginn
FROM GlassfishDB.Message;
```

| | |
|---|---|
| **Anker (V5)** | **`2026-07-08 17:21:10`** |
| Bestandsbeginn | `2024-10-01 02:00:28` |

Dieser Wert wird in M90 und in Fenster D **wörtlich** eingesetzt. Er ist unverändert gegenüber M0
(01.08.2026) und M83 (21.08.2026).

## V6 — Gilt die Aufbewahrung von 22 Monaten auch produktiv?

**Nicht beantwortet, und diese Runde beantwortet sie nicht.** Der Auftrag stellt das selbst fest.
Festgehalten ist hier nur, was die beiden Dateien sagen und was die Testkopie zeigt:

> **Belegvermerk** (Regel L10).
> *Gemessen war* (Testkopie, V5): Bestand von `2024-10-01 02:00:28` bis `2026-07-08 17:21:10` —
> **21,2 Monate**, davon eine fünfmonatige Lücke und ein praktisch leeres Jahr 2026 (siehe M87
> und M92).
> *Behauptet wird* in `PROJEKTBESCHREIBUNG.md` §8 Z. 955: „Die Aufbewahrung beträgt **22 Monate** —
> ältester Datensatz 01.10.2024 —, nicht ein Jahr."
> *Und daneben* sagt `rohdaten.md` §12: produktiv rund **18** Monate, danach Archivsystem.
> **Die Lücke:** Die 22 Monate sind aus `MIN`/`MAX` **der Testkopie** hergeleitet, nicht aus einer
> Aussage des Betreibers. Sie beschreiben, wie weit diese Kopie zurückreicht — nicht, wie lange
> produktiv aufbewahrt wird. **M92 rechnet deshalb ausdrücklich mit der Testkopie** und rechnet
> nichts hoch.

---

# M86 — Wandert `MessageLastUpdate`?

**Die wichtigste Messung der Runde.** Sitzung 2, `s2-m86-wandert-lastupdate.sql`.

## (a) Die Spaltendefinition, vollständig

```sql
SELECT ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT,
       EXTRA, COLUMN_KEY, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
ORDER BY ORDINAL_POSITION;
```

| Pos. | Spalte | Typ | Null | Default | `EXTRA` | Schlüssel |
|---:|---|---|---|---|---|---|
| 1 | `MessageID` | `varchar(36)` | NO | `NULL` | *(leer)* | `PRI` |
| 2 | `SOSID` | `varchar(36)` | YES | `NULL` | *(leer)* | |
| 3 | `SOSActionID` | `smallint(6)` | YES | `NULL` | *(leer)* | |
| 4 | **`MessageLastUpdate`** | **`timestamp`** | **YES** | **`NULL`** | ***(leer)*** | `MUL` |
| 5 | `MessageTimeout` | `smallint(6)` | YES | `NULL` | *(leer)* | |
| 6 | `MessageStatus` | `varchar(30)` | YES | `NULL` | *(leer)* | `MUL` |
| 7 | `SourceMessageID` | `varchar(36)` | YES | `NULL` | *(leer)* | `MUL` |
| 8 | `TargetMessageID` | `varchar(36)` | YES | `NULL` | *(leer)* | `MUL` |
| 9 | `ProcessID` | `varchar(36)` | YES | `NULL` | *(leer)* | `MUL` |
| 10 | `Source` | `bit(1)` | YES | `b'0'` | *(leer)* | |
| 11 | `Target` | `bit(1)` | YES | `b'0'` | *(leer)* | |

Die Gegenprobe, die nur Spalten mit gefülltem `EXTRA` zeigt, liefert **kein einziges Ergebnis**:

```sql
SELECT COLUMN_NAME, COLUMN_TYPE, EXTRA
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'GlassfishDB' AND TABLE_NAME = 'Message'
  AND EXTRA <> ''
ORDER BY ORDINAL_POSITION;
-- leere Ergebnismenge
```

> **`EXTRA` ist für jede Spalte leer. Es gibt kein `on update CURRENT_TIMESTAMP`.**
> Das ist die erste Hälfte der Antwort — und sie ist nicht selbstverständlich: `MessageLastUpdate`
> ist die **erste** `TIMESTAMP`-Spalte der Tabelle, und MariaDB rüstet der ersten `TIMESTAMP`-Spalte
> normalerweise `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` an. Dass hier nichts steht,
> liegt daran, dass die Spalte **`NULL`-bar mit `DEFAULT NULL`** angelegt ist; genau das schaltet die
> Automatik ab. Die Spalte wird also **von der Anwendung geschrieben**, nicht von der Datenbank.

> **Gegen `PROJEKTBESCHREIBUNG.md` §3.2 gehalten** (Regel L10).
> *Gemessen war:* elf Spalten — `MessageID`, `SOSID`, `SOSActionID`, `MessageLastUpdate`,
> `MessageTimeout`, `MessageStatus`, `SourceMessageID`, `TargetMessageID`, `ProcessID`, `Source`,
> `Target`.
> *Behauptet wird* (§3.2 Z. 130–132): dieselben **elf** Spalten.
> **Kein Befund der Art M14.** Die Mengen sind deckungsgleich; §3.2 führt sie lediglich in einer
> anderen Reihenfolge (`ProcessID` an zweiter statt an neunter Stelle). Das ist eine Lesereihenfolge,
> keine Abweichung. Was §3.2 **nicht** nennt, sind Nullbarkeit, Vorgabewert und `EXTRA` — also genau
> die drei Angaben, an denen M86 hängt. Sie stehen jetzt hier.

**Als Hilfsgröße** ist `MessageAction` mitgelesen worden (Lehre aus M14, wo zwei Spalten fehlten):
neun Spalten, `MessageID`+`MessageActionID` als `PRI`, `MessageActionStart` und `MessageActionEnd`
beide `timestamp` und `NULL`-bar, `EXTRA` durchweg leer. Deckungsgleich mit `datenmodell.md` §3.

## (b) Der Wirkungsbeleg — Fenster B

Gefragt ist: liegt `MessageLastUpdate` **nach** dem Ende des letzten ausgeführten Schrittes?

```sql
SELECT m.MessageStatus AS status,
       COUNT(*) AS nachrichten,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00') AS mit_aktionsende,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) AS lastupdate_nach_aktion,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate = a.letzte_aktion) AS lastupdate_gleich_aktion,
       SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate < a.letzte_aktion) AS lastupdate_vor_aktion,
       ROUND(100.0 * SUM(a.letzte_aktion IS NOT NULL AND a.letzte_aktion > '1971-01-01 00:00:00'
           AND m.MessageLastUpdate > a.letzte_aktion) / COUNT(*), 4) AS prozent_nachschrift
FROM GlassfishDB.Message m
LEFT JOIN (
    SELECT ma.MessageID, MAX(ma.MessageActionEnd) AS letzte_aktion
    FROM GlassfishDB.MessageAction ma
    JOIN GlassfishDB.Message mw
      ON mw.MessageID = ma.MessageID
     AND mw.MessageLastUpdate >= '2025-11-30 00:00:00'
     AND mw.MessageLastUpdate <  '2025-12-30 00:00:00'
    GROUP BY ma.MessageID
) a ON a.MessageID = m.MessageID
WHERE m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
GROUP BY m.MessageStatus
ORDER BY nachrichten DESC;
```

**`EXPLAIN` im Volltext:**

```
+------+-----------------+------------+--------+-----------------------------------------------------------------+----------------------+---------+--------------------------+--------+--------------------------------------------------------+
| id   | select_type     | table      | type   | possible_keys                                                   | key                  | key_len | ref                      | rows   | Extra                                                  |
+------+-----------------+------------+--------+-----------------------------------------------------------------+----------------------+---------+--------------------------+--------+--------------------------------------------------------+
|    1 | PRIMARY         | m          | range  | MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX         | MessageLastUpdateIDX | 5       | NULL                     | 409756 | Using index condition; Using temporary; Using filesort |
|    1 | PRIMARY         | <derived2> | ref    | key0                                                            | key0                 | 147     | GlassfishDB.m.MessageID  | 2      |                                                        |
|    2 | LATERAL DERIVED | ma         | ref    | PRIMARY,MessageAction_MessageFK                                 | PRIMARY              | 146     | GlassfishDB.m.MessageID  | 1      |                                                        |
|    2 | LATERAL DERIVED | mw         | eq_ref | PRIMARY,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | PRIMARY              | 146     | GlassfishDB.ma.MessageID | 1      | Using where                                            |
+------+-----------------+------------+--------+-----------------------------------------------------------------+----------------------+---------+--------------------------+--------+--------------------------------------------------------+
```

**Ergebnis, Fenster B** (Laufzeit 517,5 ms; n = 214.330, deckungsgleich mit M17):

| Status | Nachrichten | mit Aktionsende | `LastUpdate` **nach** Aktion | gleich | **vor** Aktion | Anteil nach |
|---|---:|---:|---:|---:|---:|---:|
| `FINISHED` | 139.474 | 139.474 | **864** | 138.610 | 0 | 0,6195 % |
| `MERGED` | 38.628 | 38.628 | **309** | 38.319 | 0 | 0,7999 % |
| `SPLITTED` | 28.144 | 28.144 | **300** | 27.844 | 0 | 1,0659 % |
| `EERP_RECEIVED` | 6.898 | 6.898 | **57** | 5.038 | **1.803** | 0,8263 % |
| `COMMIT_RECEIVED` | 552 | 552 | **12** | 430 | **110** | 2,1739 % |
| `SUSPENDED` | 538 | 538 | 5 | 533 | 0 | 0,9294 % |
| `COMMIT_SENT` | 80 | 80 | 0 | 80 | 0 | 0,0000 % |
| `CHECKED` | 5 | 5 | 0 | 5 | 0 | 0,0000 % |
| `COMMIT_REJECTED` | 5 | 5 | 0 | 5 | 0 | 0,0000 % |
| `ERROR_TIMEOUT` | 3 | 3 | 1 | 2 | 0 | 33,3333 % |
| `ERROR_DUPLICATE` | 3 | 3 | 0 | 3 | 0 | 0,0000 % |

**Jede Nachricht des Fensters hat ein Aktionsende** — 214.330 von 214.330, keine Ausreißer,
kein Nullzeitstempel.

**Die Verteilung der Differenz in Sekunden** — und sie ist die eigentliche Antwort:

| Status | mit Differenz | Min | **Max** | Mittel | ≤ 0 | 1–60 s | 61–3600 s | 1–24 h | > 24 h |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `FINISHED` | 139.474 | 0 | **4** | 0,006 | 138.610 | 864 | **0** | **0** | **0** |
| `MERGED` | 38.628 | 0 | **1** | 0,008 | 38.319 | 309 | 0 | 0 | 0 |
| `SPLITTED` | 28.144 | 0 | **1** | 0,011 | 27.844 | 300 | 0 | 0 | 0 |
| `EERP_RECEIVED` | 6.898 | **−58** | **1** | −1,224 | 6.841 | 57 | 0 | 0 | 0 |
| `COMMIT_RECEIVED` | 552 | **−8** | **1** | −0,822 | 540 | 12 | 0 | 0 | 0 |
| `SUSPENDED` | 538 | 0 | **1** | 0,009 | 533 | 5 | 0 | 0 | 0 |
| `COMMIT_SENT` | 80 | 0 | 0 | 0,000 | 80 | 0 | 0 | 0 | 0 |
| `COMMIT_REJECTED` | 5 | 0 | 0 | 0,000 | 5 | 0 | 0 | 0 | 0 |
| `CHECKED` | 5 | 0 | 0 | 0,000 | 5 | 0 | 0 | 0 | 0 |
| `ERROR_DUPLICATE` | 3 | 0 | 0 | 0,000 | 3 | 0 | 0 | 0 | 0 |
| `ERROR_TIMEOUT` | 3 | 0 | 1 | 0,333 | 2 | 1 | 0 | 0 | 0 |

**Perzentile je Status, Fenster B:**

| Status | Min | p50 | p90 | p99 | Max |
|---|---:|---:|---:|---:|---:|
| `CHECKED` | 0 | 0 | 0 | 0 | 0 |
| `COMMIT_RECEIVED` | **−8** | 0 | 0 | 1 | 1 |
| `COMMIT_REJECTED` | 0 | 0 | 0 | 0 | 0 |
| `COMMIT_SENT` | 0 | 0 | 0 | 0 | 0 |
| `EERP_RECEIVED` | **−58** | 0 | 0 | 0 | 1 |
| `ERROR_DUPLICATE` | 0 | 0 | 0 | 0 | 0 |
| `ERROR_TIMEOUT` | 0 | 0 | 1 | 1 | 1 |
| `FINISHED` | 0 | 0 | 0 | 0 | **4** |
| `MERGED` | 0 | 0 | 0 | 0 | 1 |
| `SPLITTED` | 0 | 0 | 0 | 1 | 1 |
| `SUSPENDED` | 0 | 0 | 0 | 0 | 1 |

## (c) Die Gegenprobe — Fenster A

Derselbe Aufbau, Fenster `>= '2025-12-29 00:00:00'` und `< '2025-12-30 00:00:00'`
(n = 6.249, deckungsgleich mit M17). `EXPLAIN` identisch bis auf `rows` (11.812 statt 409.756).

| Status | Nachrichten | nach Aktion | gleich | vor Aktion | Anteil nach |
|---|---:|---:|---:|---:|---:|
| `FINISHED` | 5.360 | **29** | 5.331 | 0 | 0,5410 % |
| `MERGED` | 432 | **5** | 427 | 0 | 1,1574 % |
| `SPLITTED` | 390 | 0 | 390 | 0 | 0,0000 % |
| `EERP_RECEIVED` | 65 | 0 | 45 | **20** | 0,0000 % |
| `ERROR_DUPLICATE` | 1 | 0 | 1 | 0 | 0,0000 % |
| `SUSPENDED` | 1 | 0 | 1 | 0 | 0,0000 % |

Differenzen: Maximum **1 Sekunde**, Minimum −8 Sekunden (`EERP_RECEIVED`). Kein einziger Wert
oberhalb von 60 Sekunden.

**Die Anteile stimmen grob überein** — `FINISHED` 0,62 % gegen 0,54 %, `MERGED` 0,80 % gegen 1,16 %.
**Der Befund ist nicht fensterabhängig.**

## Vorregistrierte Deutung, dagegengehalten

| Vorregistriert | Trifft zu? |
|---|---|
| `EXTRA` enthält `on update CURRENT_TIMESTAMP` → Nachlauffenster nötig | **Nein.** `EXTRA` ist für alle elf Spalten leer |
| `EXTRA` leer, aber (b) zeigt einen nennenswerten Anteil positiver Differenzen → Nachlauffenster nötig | **Teilweise — und hier entscheidet sich alles** |
| `EXTRA` leer **und** (b) praktisch null → einfacher Wasserstand genügt | **Teilweise** |

**Die vorregistrierte Deutung trifft in ihrer Form nicht zu, weil sie eine Größe nicht vorsah, die
die Messung liefert: die *Höhe* der Differenz.**

Sie fragt nach dem **Anteil** positiver Differenzen und lässt offen, ab wann er „nennenswert" ist.
Gemessen ist ein Anteil von **0,62 % bis 2,17 %** — für sich genommen weder null noch belanglos.
Aber die Differenzen betragen **höchstens vier Sekunden**, und zwar über beide Fenster, über alle
elf Statuswerte und über 220.579 Nachrichten hinweg. **Kein einziger Wert liegt über 60 Sekunden.**

Für einen **stündlichen** Eimer ist das die Antwort, auf die es ankommt:

> Eine Nachschrift von bis zu vier Sekunden verschiebt einen Stundeneimer nur dann, wenn die Zeile
> ohnehin **innerhalb der letzten vier Sekunden vor dem Stundenwechsel** liegt. Bei gleichmäßiger
> Verteilung sind das 4/3.600 = **0,111 %** der Zeilen, und davon wiederum nur die 0,62 % bis 2,17 %,
> die überhaupt nachgeschrieben werden — in der Größenordnung **eine von 100.000 Zeilen**.

**Was daraus für 10a folgt — und was ausdrücklich nicht.** Ein **Wasserstand mit einem
Nachlauffenster von wenigen Minuten** genügt auf diesem Bestand. Ein Nachlauffenster von 48 Stunden
(Fenster D), wie es die Aufgabenstellung als Möglichkeit mitführt, ist durch diese Messung **nicht
begründet** — es wäre um den Faktor 43.200 überdimensioniert. Ein Wasserstand **ohne jedes**
Nachlauffenster ist es aber auch nicht: die 1.545 nachgeschriebenen Zeilen in Fenster B sind
gemessen und nicht null.

## Die Grenze dieser Messung — sie gehört in den Befund

Der Auftrag schreibt sie selbst vor, und sie gilt unverändert:

- Der Bestand endet am **08.07.2026**, und das Jahr 2026 trägt nur **5.133 Zeilen** auf **drei**
  Prozessen (M87‑5c). Was gemessen ist, ist im Wesentlichen ein Bestand von 2024 und 2025.
- **`RUNNING` kommt null Mal vor.** Der Status, in dem eine Zeile am ehesten mehrfach
  fortgeschrieben würde, ist auf dieser Kopie nicht vorhanden.
- **`MatchInterchange` läuft auf der Testkopie nicht sichtbar** (M31‑3, Takt **ungedeckt**). Genau
  dieser Job setzt `MessageStatus = 'COMMIT_RECEIVED'` und `SourceMessageID` **nachträglich** — er
  ist der wahrscheinlichste Verursacher einer Wanderung, und er ist hier nicht am Werk.

> **Der zulässige Satz lautet deshalb: „Auf diesem Bestand ist eine Wanderung über mehr als vier
> Sekunden nicht beobachtet."** Nicht: „Sie findet nicht statt." Die Messung kann eine Wanderung
> belegen, wenn sie sie zeigt — sie kann sie nicht ausschließen, wenn sie sie nicht zeigt.
>
> **Ein Hinweis darauf, dass sie stattfindet, steht trotzdem in den Zahlen:** `COMMIT_RECEIVED` hat
> mit **2,17 %** den höchsten Nachschriftanteil aller Status — und `COMMIT_RECEIVED` ist genau der
> Wert, den `MatchInterchange` setzt. Die Menge ist mit 552 Zeilen zu klein, um daraus etwas
> abzuleiten; sie zeigt in die Richtung, in die M31‑3 ohnehin zeigt.

## Befund 6 — die Zeilen, deren letzte Aktion **nach** ihrer letzten Änderung endet

**1.803 `EERP_RECEIVED`- und 110 `COMMIT_RECEIVED`-Zeilen in Fenster B tragen ein
`MessageLastUpdate`, das *vor* dem Ende ihres letzten Schrittes liegt** — bis zu **58 Sekunden**
davor. In Fenster A sind es 20 von 65 `EERP_RECEIVED`-Zeilen.

Das steht in keiner vorformulierten Zeile des Auftrags und ist keine Wanderung, sondern ihr
Gegenteil: Für **26,14 %** aller `EERP_RECEIVED`-Zeilen des Fensters ist `MessageLastUpdate`
**nicht** der Zeitpunkt der letzten Änderung an dem Vorgang. Ein Schritt hat danach noch geendet,
ohne die Nachrichtenzeile anzufassen.

**Warum das für 10a zählt:** Ein Rollup über `MessageLastUpdate` ordnet diese Zeilen einem Eimer zu,
der vor dem tatsächlichen Ende des Vorgangs liegt. Für die Frage „wie viel lief in Stunde X" ist das
richtig genug — die Abweichung liegt unter einer Minute. Für die Aussage „`MessageLastUpdate` ist
der Zeitpunkt der letzten Änderung" (`PROJEKTBESCHREIBUNG.md` §3.2 Z. 135) ist es ein Gegenbeleg,
und der gehört notiert. Siehe offener Punkt 42.

---

# M87 — Wie viele Zeilen erzeugt der Rollup?

Sitzung 3, `s3-m87-rollupzeilen.sql`; Gegenprobe Sitzung 3b.

**Fenster G, voller Durchlauf über `Message`. Begründung nach Regel L9:** Gefragt ist die Zeilenzahl
einer Tabelle, **die es noch nicht gibt**. Sie ist nur zu bekommen, indem man die Kombinationen im
Quellbestand zählt, und das geht ohne vollen Durchlauf nicht — kein Index trägt `MessageStatus`
neben `MessageLastUpdate` und `ProcessID`. Der Durchlauf ist **Bezugsgröße** und ausdrücklich
**kein Vorbild für Anwendungscode**: der gebaute Job fährt Deltas (M88), nicht den Bestand.

`information_schema` ist hier bewusst nicht befragt — sie hat in diesem Projekt bei fünf von sechs
Zeilen danebengelegen, zuletzt um 29,51 % (V1).

## Bezugsgrößen

```sql
SELECT COUNT(*) AS zeilen_message, COUNT(DISTINCT ProcessID) AS verschiedene_prozesse,
       COUNT(DISTINCT MessageStatus) AS verschiedene_rohstatus,
       SUM(ProcessID IS NULL) AS ohne_prozess, SUM(MessageStatus IS NULL) AS ohne_status,
       SUM(MessageLastUpdate IS NULL) AS ohne_zeitstempel
FROM GlassfishDB.Message;
```

| | |
|---|---:|
| Zeilen in `Message` | **3.341.519** |
| verschiedene `ProcessID` | **738** |
| verschiedene `MessageStatus` | **12** |
| ohne `ProcessID` / ohne Status / ohne Zeitstempel | **0 / 0 / 0** |

Laufzeit 3.938,960 ms. **738 Prozesse tragen Nachrichten** — deckungsgleich mit M74b (1.503 − 765).
**Keine `NULL`** in den drei Schlüsselspalten des Rollups; der Job braucht dafür keine Sonderregel.

## Das `CASE` der Variante 2, im Volltext

Abgeschrieben aus `docs/message-status.md`, nicht erfunden:

```sql
CASE
  WHEN MessageStatus = 'FINISHED'                             THEN 'ABGESCHLOSSEN'
  WHEN MessageStatus IN ('EERP_RECEIVED','COMMIT_RECEIVED')   THEN 'QUITTIERT'
  WHEN MessageStatus LIKE 'ERROR!_%' ESCAPE '!'
       OR MessageStatus = 'COMMIT_REJECTED'                   THEN 'FEHLER'
  WHEN MessageStatus = 'SPLITTED'                             THEN 'AUFGETEILT'
  WHEN MessageStatus = 'MERGED'                               THEN 'ZUSAMMENGEFUEHRT'
  WHEN MessageStatus = 'SUSPENDED'                            THEN 'WARTEND'
  WHEN MessageStatus = 'RUNNING'                              THEN 'LAEUFT'
  ELSE 'UNGEKLAERT'
END
```

> **Eine Abweichung im Fluchtzeichen, ausdrücklich benannt.** `message-status.md` schreibt die
> Fehlerbedingung als `MessageStatus LIKE 'ERROR\_%' ESCAPE '\'`. So ist sie als **Literal in einer
> Skriptdatei nicht lauffähig**: `'\'` entwertet das schließende Hochkomma, und der
> Workbench-Client bricht zusätzlich mit `Unknown command '\_'` ab, bevor das Statement den Server
> erreicht. Gefahren ist deshalb `LIKE 'ERROR!_%' ESCAPE '!'` — **zeichengleich in der Wirkung**,
> `!_` steht für einen buchstäblichen Unterstrich. Der Beleg dafür ist M87‑6, siehe unten. **Der
> Ausdruck im Code ist davon nicht berührt**: jOOQ setzt das Fluchtzeichen selbst.

**M87‑6 — das `CASE` gegen den Bestand gehalten** (Laufzeit 8.460,350 ms):

| Rohstatus | Einordnung | Anzahl |
|---|---|---:|
| `FINISHED` | `ABGESCHLOSSEN` | 2.030.986 |
| `MERGED` | `ZUSAMMENGEFUEHRT` | 747.885 |
| `SPLITTED` | `AUFGETEILT` | 400.845 |
| `EERP_RECEIVED` | `QUITTIERT` | 143.871 |
| `COMMIT_RECEIVED` | `QUITTIERT` | 12.654 |
| `ERROR_DUPLICATE` | **`FEHLER`** | 3.248 |
| `COMMIT_SENT` | `UNGEKLAERT` | 1.051 |
| `SUSPENDED` | `WARTEND` | 538 |
| `CHECKED` | `UNGEKLAERT` | 276 |
| `COMMIT_REJECTED` | **`FEHLER`** | 111 |
| `ERROR_TIMEOUT` | **`FEHLER`** | 52 |
| `CKECKED` | `UNGEKLAERT` | 2 |

**Das `CASE` weicht vom Code nicht ab.** Zwölf Rohwerte, `RUNNING` fehlt wie dokumentiert;
`FEHLER` umfasst genau `ERROR_DUPLICATE`, `ERROR_TIMEOUT` und `COMMIT_REJECTED` (3.411 Zeilen) —
das Fluchtzeichen `!` trifft also dieselbe Menge wie `\`. `COMMIT_SENT` fällt nach `UNGEKLAERT` und
nicht nach `QUITTIERT`; `CKECKED` (2 Zeilen, der Tippfehler) fällt über den `ELSE`-Zweig ebenfalls
nach `UNGEKLAERT`. Alle drei Fälle so, wie `message-status.md` sie beschreibt.

## Die vier Varianten

Alle vier fahren denselben Plan — es gibt keinen anderen:

```
+------+-------------+---------+------+---------------+------+---------+------+---------+-------+
| id   | select_type | table   | type | possible_keys | key  | key_len | ref  | rows    | Extra |
+------+-------------+---------+------+---------------+------+---------+------+---------+-------+
|    1 | SIMPLE      | Message | ALL  | NULL          | NULL | NULL    | NULL | 3560486 |       |
+------+-------------+---------+------+---------------+------+---------+------+---------+-------+
```

`type = ALL`, `possible_keys = NULL`, kein Index — wie unter L9 begründet und erwartet.

| Variante | Schlüssel | **Zeilen** | Laufzeit |
|---|---|---:|---:|
| **1** | Stunde, `ProcessID`, `MessageStatus` — **die gebaute Fassung (E‑a + E‑g)** | **335.610** | 14.062,141 ms |
| **2** | Stunde, `ProcessID`, **Einordnung** | **335.609** | 15.301,694 ms |
| **3** | Tag, `ProcessID`, `MessageStatus` | **123.049** | 10.359,044 ms |
| **4** | Monat, `ProcessID`, `MessageStatus` | **11.957** | 7.714,630 ms |

Zusätzlich: **belegte Stunden × Prozesse** und **belegte Stunden** stehen in M87‑5, siehe dort.

## M87‑5 — die Gegenprobe, und die Grenze, die dabei gerissen ist

Die Gegenprobe sollte alle vier Zahlen in **einem** Durchlauf liefern. Sie ist nach
**60,219 s** an `max_statement_time = 60` abgebrochen worden.

> **Das ist der Befund, und die Grenze ist nicht hochgesetzt worden.** Der Rahmen (§1) schreibt für
> diesen Fall vor: „Die Messung wird dann über Jahresscheiben gefahren und summiert, mit Vermerk."
> Genau das ist in Sitzung 3b geschehen.
>
> **Warum das Summieren exakt ist und keine Näherung:** In allen vier Varianten steht die Zeit als
> erstes Glied des Schlüssels. Eine Kombination kann deshalb nicht über zwei Jahresscheiben hinweg
> existieren — die Scheibe ist aus dem Schlüssel selbst ableitbar. Die Teilmengen sind disjunkt, die
> Summe der Verschiedenheiten ist die Verschiedenheit der Vereinigung.

| Scheibe | Zeilen | V1 | V2 | V3 | V4 | Prozesse | Laufzeit |
|---|---:|---:|---:|---:|---:|---:|---:|
| 2024 (`2024-10-01` – `2025-01-01`) | 630.543 | 55.409 | 55.409 | 22.771 | 2.303 | 656 | 10.670,403 ms |
| 2025 (`2025-01-01` – `2026-01-01`) | 2.705.843 | 280.186 | 280.185 | 100.270 | 9.649 | 732 | 40.895,839 ms |
| 2026 (`2026-01-01` – `2026-08-01`) | **5.133** | **15** | **15** | **8** | **5** | **3** | 88,538 ms |
| **Summe** | **3.341.519** | **335.610** | **335.609** | **123.049** | **11.957** | — | 51.654,780 ms |

**Zeilen außerhalb der drei Scheiben: 0.** Die Dreiteilung deckt den Bestand vollständig.

**Alle vier Summen treffen die Einzelmessungen auf die Zeile.** Die Gegenprobe geht auf.

## Vorregistrierte Deutung, dagegengehalten

**Erstens: der Preis von E‑g A.**

> Vorregistriert: *„Variante 1 gegen Variante 2 ist der Preis von E‑g A in Zeilen. Liegt er unter
> 25 %, ist er belanglos und E‑g A steht ohne Vorbehalt. Liegt er über 60 %, gehört er dem
> Auftraggeber vorgelegt."*

**335.610 gegen 335.609. Der Preis beträgt genau eine Zeile — 0,0003 %.**

Die Deutung trifft zu, und zwar am äußersten Rand ihres unteren Zweigs: **E‑g A steht ohne
Vorbehalt.** Den Rohstatus in der Zeile zu führen statt der Einordnung kostet den Rollup auf diesem
Bestand **eine einzige zusätzliche Zeile** im gesamten Bestand.

**Warum das Ergebnis so extrem ausfällt — es ist kein Zufall.** Die Einordnung fasst nur an zwei
Stellen zusammen: `EERP_RECEIVED` + `COMMIT_RECEIVED` → `QUITTIERT`, und
`CHECKED` + `CKECKED` + `COMMIT_SENT` → `UNGEKLAERT`. Eine Zeile wird nur dann gespart, wenn zwei
solche Rohwerte **in derselben Stunde beim selben Prozess** auftreten. Das ist im ganzen Bestand
**einmal** passiert. `FINISHED`, `MERGED` und `SPLITTED` — zusammen 95,2 % der Zeilen — bilden je
eine eigene Einordnung und können gar nichts sparen.

> **Der Nebenschluss, der daraus folgt und der 10a gehört:** Weil der Preis eine Zeile ist, ist
> **E‑g B (die Einordnung speichern) auch kein Gewinn.** Die Entscheidung E‑g steht damit nicht
> mehr auf einer Mengenabwägung, sondern allein auf dem Argument, das sie ohnehin trägt: Der
> Rohwert bleibt lesbar, wenn sich die Einordnung ändert. Das ist ein besseres Argument als
> Zeilenersparnis, und es ist jetzt das einzige.

**Zweitens: die Grundlage für E‑b.**

> Vorregistriert: *„Varianten 3 und 4 gegen Variante 1 sind die Grundlage für E‑b: Kostet die
> Hochaggregation zur Lesezeit wenig, braucht es keine drei materialisierten Ebenen."*

| Ebene | Zeilen | gegen Variante 1 | Verdichtung |
|---|---:|---:|---:|
| Stunde (V1) | 335.610 | — | — |
| Tag (V3) | 123.049 | **36,66 %** | Faktor **2,73** |
| Monat (V4) | 11.957 | **3,56 %** | Faktor **28,07** |

**Die Verdichtung ist gering, und das ist die Antwort.** Ein Tag hat 24 Stunden, aber die
Tagesebene spart nur den Faktor **2,73** statt 24 — weil ein Prozess-Status-Paar im Mittel nur in
**2,73** von 24 Stunden eines Tages überhaupt vorkommt. Die Stundenebene ist also bereits dünn
besetzt.

**Praktisch heißt das:** Eine Tagesansicht über die Stundentabelle zu rechnen liest im Mittel
**2,73 Zeilen je Ergebniszeile**, eine Monatsansicht **28,07**. Beides ist eine Größenordnung, die
zur Lesezeit nichts kostet. **Drei materialisierte Ebenen sind durch diese Messung nicht
begründet** — eine Stundentabelle mit Hochaggregation zur Lesezeit trägt.

> **Was diese Zahlen nicht sagen.** Sie sind über den **Gesamtbestand** gerechnet, ohne
> Mandantenfilter. Eine Monatsansicht **eines** Mandanten liest nicht 28,07 Zeilen je Ergebniszeile,
> sondern so viele, wie dieser Mandant in dem Monat belegt. Für `NEXANS` mit 86,36 % der Nachrichten
> liegt das nahe an der Gesamtzahl, für `SUTTONS` deutlich darunter. Gemessen ist die
> Hochaggregation **nicht** — M89 misst die Leseabfrage auf Stundenebene, nicht die Verdichtung
> darüber.

## Befund 7 — die Rollup-Tabelle ist ein Zehntel der Quelltabelle, und das Jahr 2026 ist leer

**335.610 Rollup-Zeilen zu 3.341.519 Nachrichten: der Rollup verdichtet um den Faktor 9,96.** Das
ist deutlich weniger, als eine stündliche Aggregation vermuten lässt — bei 7.300 Nachrichten pro Tag
(§8) und 738 Prozessen ist der Bestand über Prozesse und Stunden so breit gestreut, dass im Mittel
nur **9,96 Nachrichten** auf eine Rollup-Zeile fallen.

Der zweite Teil des Befundes steht in der Jahresscheibe 2026: **5.133 Zeilen, drei Prozesse,
fünfzehn Rollup-Zeilen** für über sechs Monate. Der Bestand der Testkopie hört faktisch am
30.12.2025 auf; was danach kommt, ist ein Rinnsal aus drei Prozessen. **Jede Messung dieser Runde,
die über Fenster D (Juli 2026) läuft, misst deshalb an einem praktisch leeren Bestand** — das
betrifft M88, M89 und M90 unmittelbar und ist dort erneut vermerkt.
