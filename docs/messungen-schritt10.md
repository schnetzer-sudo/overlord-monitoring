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

---

# M88 — Was kostet der stündliche Delta-Lauf?

Sitzung 4a (Vorprobe) und 4b — `s4a-m88-vorprobe.sql`, `s4b-m88-deltalauf.sql`.

## Die Vorprobe, und warum sie nötig war

Der Auftrag verlangt drei Fenster: **eine Stunde**, **Fenster D** (48 h) und **Fenster A** (ein Tag).
*Welche* Stunde, sagt er nicht. Sie ist hergeleitet statt geraten — und dabei ist der Befund
aufgetaucht, der M88, M89 und M90 gleichermaßen betrifft.

**Fenster D ist auf der Testkopie praktisch leer:**

```sql
SELECT COUNT(*) AS zeilen_fenster_d, COUNT(DISTINCT ProcessID) AS prozesse,
       COUNT(DISTINCT MessageStatus) AS status,
       MIN(MessageLastUpdate) AS frueheste, MAX(MessageLastUpdate) AS spaeteste
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2026-07-06 17:21:10' AND MessageLastUpdate < '2026-07-08 17:21:10';
```

| Zeilen | Prozesse | Status | früheste | späteste |
|---:|---:|---:|---|---|
| **256** | **2** | **2** | `2026-07-08 17:16:26` | `2026-07-08 17:20:29` |

**256 Zeilen, zwei Prozesse — und alle innerhalb von vier Minuten.** Von den 48 Stunden des Fensters
sind 47 Stunden und 56 Minuten vollständig leer.

Die letzten dreißig Tage des Bestands bestätigen es: nur **fünf** Tage tragen überhaupt etwas.

| Tag | Zeilen | Prozesse |
|---|---:|---:|
| 2026-06-09 | 2 | 1 |
| 2026-06-14 | 2 | 1 |
| 2026-06-17 | 1.425 | 2 |
| 2026-06-18 | 3.419 | 2 |
| 2026-07-08 | 285 | 2 |

Summe 5.133 — das ganze Jahr 2026 (M87‑5c).

**Daraus folgt der Zuschnitt der Messung.** Gefahren sind **sechs** statt drei Fenster: die drei des
Auftrags plus zwei weitere Stunden, die den Bereich zwischen leer und dicht aufspannen, plus ein
dichtes 48‑h‑Fenster als Gegenstück zu D. Ohne sie wäre M88 die Messung eines leeren Indexbereichs.

| | Fenster | Zeilen | Ergebniszeilen |
|---|---|---:|---:|
| **H1** | dichteste Stunde des **ganzen Bestands**, `2025-12-07 17:00` | **8.630** | 14 |
| **H2** | dichteste Stunde des **Fensters A**, `2025-12-29 22:00` | 2.883 | 19 |
| **H3** | **letzte Stunde des Bestands**, `2026-07-08 17:00` | 285 | 2 |
| **D** | **Fenster D**, 48 h bis zum Anker aus V5 | 256 | 2 |
| **D2** | dichte 48 h, `2025-12-28` bis `2025-12-30` | 12.332 | 926 |
| **A** | **Fenster A**, ein Tag | 6.249 | 575 |

Die dichteste Stunde des Bestands ist deterministisch hergeleitet
(`GROUP BY stunde ORDER BY zeilen DESC LIMIT 5`, Laufzeit 8.442,916 ms, unter L9 als Bezugsgröße):
`2025-12-07 17:00` mit 8.630 Zeilen, davor `2024-11-13 18:00` (7.555), `2025-11-07 17:00` (7.043),
`2024-10-09 20:00` (7.021), `2024-11-21 17:00` (6.808).

## Die gemessene Abfrage

Unverändert die des Auftrags. **Kein `STRAIGHT_JOIN`, in keiner Fassung** (M42, Faktor 219–1094×).

```sql
SELECT DATE_FORMAT(MessageLastUpdate, '%Y-%m-%d %H:00:00') AS stunde,
       ProcessID,
       MessageStatus,
       COUNT(*) AS anzahl
FROM GlassfishDB.Message
WHERE MessageLastUpdate >= '2025-12-07 17:00:00'
  AND MessageLastUpdate <  '2025-12-07 18:00:00'
GROUP BY stunde, ProcessID, MessageStatus;
```

## `EXPLAIN` — bei allen gemessenen Fenstern derselbe Plan

```
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
| id   | select_type | table   | type  | possible_keys                                           | key                  | key_len | ref  | rows  | Extra                                                  |
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
|    1 | SIMPLE      | Message | range | MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL | 17730 | Using index condition; Using temporary; Using filesort |
+------+-------------+---------+-------+---------------------------------------------------------+----------------------+---------+------+-------+--------------------------------------------------------+
```

Nur `rows` unterscheidet sich: **H1 17.730 · H3 285 · D 256 · D2 24.218 · A 11.812.**
Für **H2** ist kein `EXPLAIN` gefahren worden — die fünf gefahrenen decken den Mengenbereich von 256
bis 24.218 ab, und der Plan ist über diesen ganzen Bereich unverändert. Das ist eine bewusste Lücke
und keine übersehene: sie steht unter „Abweichungen vom Rahmen".

**`EXPLAIN FORMAT=JSON` für H1, im Volltext:**

```json
{
  "query_block": {
    "select_id": 1,
    "filesort": {
      "sort_key": "date_format(Message.MessageLastUpdate,'%Y-%m-%d %H:00:00'), Message.ProcessID, Message.MessageStatus",
      "temporary_table": {
        "table": {
          "table_name": "Message",
          "access_type": "range",
          "possible_keys": ["MessageLastUpdateIDX", "MessageLastUpdateProcessMessageIDX"],
          "key": "MessageLastUpdateIDX",
          "key_length": "5",
          "used_key_parts": ["MessageLastUpdate"],
          "rows": 17730,
          "filtered": 100,
          "index_condition": "Message.MessageLastUpdate >= '2025-12-07 17:00:00' and Message.MessageLastUpdate < '2025-12-07 18:00:00'"
        }
      }
    }
  }
}
```

## Die Frage hinter M88, beantwortet

> Gefragt war: *„Greift `MessageLastUpdateProcessMessageIDX (MessageLastUpdate, ProcessID,
> MessageID)`, und wenn ja, wie weit? `MessageStatus` steht nicht in ihm — der Index kann also nicht
> deckend sein. Zu messen ist, ob MariaDB trotzdem über ihn einsteigt oder auf
> `MessageLastUpdateIDX` fällt."*

**MariaDB fällt auf `MessageLastUpdateIDX`. Bei allen fünf geplanten Fenstern, ohne Ausnahme.**

Der zusammengesetzte Index steht in `possible_keys`, wird aber nie gewählt. `used_key_parts` nennt
**genau einen** Schlüsselteil — `MessageLastUpdate`. Das wäre auch bei ihm nicht anders: Die
`WHERE`-Bedingung schränkt nur die erste Spalte ein; `ProcessID` und `MessageID` stehen dahinter und
werden von keiner Bedingung getroffen, sie können den Bereich also nicht verengen.

**Warum der schmalere Index gewinnt, und warum das kein Zufall ist.** Weil `MessageStatus` in keinem
der beiden Indizes steht, braucht **jede** Fassung einen Rückgriff auf die Tabellenzeile. Damit ist
der einzige Unterschied die Breite des Indexbereichs, der dafür gelesen wird:
`MessageLastUpdateIDX` trägt eine Spalte plus Primärschlüssel, `MessageLastUpdateProcessMessageIDX`
drei. Für dieselbe Zeilenmenge sind das mehr Indexseiten ohne jeden Gegenwert.

> **Der Nebenschluss für 10a:** `MessageLastUpdateProcessMessageIDX` bringt dem Rollup-Job
> **nichts**. Er ist für die Nachrichtenliste gebaut (Cursor über `(MessageLastUpdate, MessageID)`),
> und dort trägt er. Wer den Delta-Lauf plant, plant ihn gegen `MessageLastUpdateIDX`.
> **Ein deckender Index müsste `MessageStatus` enthalten** — er existiert nicht, und ihn anzulegen
> hieße, auf `GlassfishDB` zu schreiben. Das ist ausgeschlossen (Regel 3), und damit ist diese
> Möglichkeit nicht erst abzuwägen.

## Laufzeit — ein Aufwärmlauf, dann beste von fünf

| Fenster | Zeilen | Aufwärmlauf | **beste von fünf** | schlechteste | alle fünf (ms) |
|---|---:|---:|---:|---:|---|
| **H1** dichteste Stunde | 8.630 | 101,328 ms | **88,167 ms** | 90,321 ms | 88,167 · 88,326 · 90,321 · 88,986 · 88,606 |
| **H2** dichteste Stunde in A | 2.883 | 31,523 ms | **30,251 ms** | 31,356 ms | 30,251 · 30,345 · 31,356 · 30,415 · 30,375 |
| **H3** letzte Stunde | 285 | 3,890 ms | **3,190 ms** | 4,746 ms | 3,190 · 4,746 · 3,482 · 3,269 · 3,323 |
| **D** Fenster D (48 h) | 256 | 2,947 ms | **2,889 ms** | 3,022 ms | 3,003 · 2,937 · 2,890 · 3,022 · 2,889 |
| **D2** dichte 48 h | 12.332 | 140,311 ms | **136,258 ms** | 140,499 ms | 137,898 · 137,518 · 137,730 · 136,258 · 140,499 |
| **A** Fenster A (1 Tag) | 6.249 | 69,052 ms | **68,733 ms** | 72,061 ms | 69,661 · 68,733 · 69,078 · 72,061 · 70,965 |

**Die Laufzeit ist in der Zeilenzahl linear, nicht in der Fensterbreite.**

| Fenster | Zeilen | beste v5 | **µs je Zeile** |
|---|---:|---:|---:|
| H1 | 8.630 | 88,167 ms | 10,2 |
| H2 | 2.883 | 30,251 ms | 10,5 |
| A | 6.249 | 68,733 ms | 11,0 |
| D2 | 12.332 | 136,258 ms | 11,1 |
| H3 | 285 | 3,190 ms | 11,2 |
| D | 256 | 2,889 ms | 11,3 |

**10,2 bis 11,3 µs je Zeile, über einen Mengenbereich von Faktor 48.** Fenster D ist nicht deshalb
schnell, weil es günstig läge, sondern weil 256 Zeilen darin stehen; die 48 Stunden Fensterbreite
kosten für sich genommen nichts. Der Aufwärmlauf kostet zwischen 1,4 % (D) und 22,0 % (H3) mehr als
der beste Lauf.

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„Unter 100 ms für ein Ein-Stunden-Fenster: Der stündliche Job ist unauffällig, und
> Leistungsregel 6 („gedrosselt") ist an dieser Stelle Zeremonie statt Schutz — was dann so gesagt
> wird. Über 1 s: Die Drosselung ist ernst zu nehmen und gehört als Bauvorgabe nach 10a. Über
> `max_statement_time`: Der Job ist in dieser Form nicht baubar."*

**Der obere Zweig trifft, und er trifft mit Abstand.** Die **dichteste Stunde des gesamten
Bestands** kostet **88,167 ms** — unter der Schwelle von 100 ms. Das ist nicht die durchschnittliche
Stunde, sondern die teuerste von rund 15.000. Die letzte Stunde des Bestands kostet 3,190 ms.

**Also, wie vorregistriert, so gesagt:**

> **Leistungsregel 6 („Der Rollup-Job läuft gedrosselt. Er teilt sich die Instanz mit der
> Produktion.") ist an dieser Stelle Zeremonie statt Schutz.** Ein Statement, das 88 ms braucht und
> einmal je Stunde läuft, belegt die Instanz zu **0,0024 %**. Wovor die Drosselung schützen soll,
> tritt beim stündlichen Delta-Lauf nicht ein.

**Wo sie nicht Zeremonie ist, ist der Rückwärtslauf** — und der ist M92, nicht M88. Die Regel trägt
dort weiter; sie trägt nur nicht an der Stelle, an der sie am häufigsten zitiert wird.

> **Zwei Vorbehalte, die dazugehören.**
>
> 1. **Der Aufwärmlauf ist keine Kaltlaufschranke.** `innodb_buffer_pool_size` beträgt 25.600 MiB,
>    `Message` mit allen acht Indizes 2.763,9 MiB — der Puffer fasst die Tabelle 9,26‑mal (M83‑4).
>    Eine `mysql`-Sitzungsgrenze leert keinen Serverpuffer. Gemessen ist der Warmfall; für den echten
>    Kaltfall nennt M44 einen Faktor von bis zu **9,66**, was für H1 rund **850 ms** ergäbe. Auch das
>    bliebe unter einer Sekunde. **Das ist eine Übertragung und keine Messung** — der Faktor stammt
>    aus einer anderen Abfrage, und `FLUSH TABLES` steht `monitor_read` nicht zu.
> 2. **In Produktion ist die dichteste Stunde größer.** 8.630 Zeilen sind das Maximum *dieser
>    Testkopie*. Bei 11 µs je Zeile bliebe der Job auch bei **90.000 Zeilen je Stunde** unter einer
>    Sekunde — das ist eine Hochrechnung aus der gemessenen Linearität, keine gemessene Zahl.

## Befund 8 — die Zeilenschätzung ist bei großen Bereichen um Faktor 2 zu hoch, bei kleinen exakt

| Fenster | `rows` im Plan | tatsächlich | Verhältnis |
|---|---:|---:|---:|
| H1 | 17.730 | 8.630 | **2,05** |
| D2 | 24.218 | 12.332 | **1,96** |
| A | 11.812 | 6.249 | **1,89** |
| H3 | **285** | **285** | **1,00** |
| D | **256** | **256** | **1,00** |

Die beiden kleinen Bereiche sind **auf die Zeile genau** geschätzt, die drei großen um rund
Faktor 2 zu hoch. Der Faktor ist kein Zufall: `CARDINALITY` steht für `MessageLastUpdateIDX` auf
**1.780.243** gegen **3.341.519** gezählte Zeilen — also auf **genau der Hälfte** (V1, V3). Wo
MariaDB nicht mehr im Bereich zählt, sondern aus dieser Kardinalität hochrechnet, verdoppelt sich
die Schätzung.

**Folge für M88: keine.** Der Plan ist bei beiden Schätzungen derselbe, es gibt nur einen möglichen
Zugriffsweg. Der Befund steht hier, weil dieselbe halbierte Kardinalität in einer Abfrage mit
**mehreren** Joinpartnern die Joinreihenfolge kippen kann — und M89 ist genau so eine Abfrage.
Er schließt an Befund 2 aus M83 an, der dieselbe Statistik von der anderen Seite trifft
(`CARDINALITY` 18 auf `ProcessID`, Faktor 43,7).

---

# M89 — Die Leseabfrage des Dashboards, über die Schemagrenze

Sitzungen 5a bis 5d. **Hier steht die einzige schreibende Handlung dieser Runde.**

## Die Probetabelle — angelegt, befüllt, gemessen

Vorgehen nach dem Muster von M80, enger gefasst. Freigegeben ist **genau eine** Tabelle.

**Vorher:** `message_rollup_probe` existierte nicht (`information_schema.TABLES` → 0). `overlord_monitor`
trug neun Tabellen: `app_user`, `app_user_mandant`, `audit_log`, `bam_sollaenge`, `bam_spalte`,
`flyway_schema_history`, `process_catalog`, `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`.

```sql
CREATE TABLE overlord_monitor.message_rollup_probe (
  stunde         DATETIME    NOT NULL,
  process_id     VARCHAR(36) NOT NULL,
  message_status VARCHAR(30) NOT NULL,
  anzahl         INT         NOT NULL,
  PRIMARY KEY (stunde, process_id, message_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

Der Schlüssel ist **`(Stunde, ProcessID, MessageStatus)`** — E‑a plus E‑g, die gebaute Fassung.
Mandant, Partner und Richtung stehen **nicht** in der Zeile. Sortierung `utf8mb4_general_ci`, damit
der Join über die Schemagrenze kollationsrein bleibt (V4).

**Befüllt in 22 Monatsscheiben**, nicht in einem Zug. Grund: Ein einziges `INSERT … SELECT` über den
Gesamtbestand liefe gegen `max_statement_time = 60` — M87‑5 hat die Grenze mit einer ähnlich großen
Aggregation bereits gerissen. Der Rahmen verbietet, sie hochzusetzen. Also dieselbe Antwort wie dort:
Scheiben.

| Kontrolle | Wert |
|---|---:|
| Zeilen in der Probetabelle | **335.610** |
| `SUM(anzahl)` | **3.341.519** |
| verschiedene `process_id` | 738 |
| verschiedene `message_status` | 12 |
| früheste / späteste Stunde | `2024-10-01 02:00:00` / `2026-07-08 17:00:00` |
| Größe (`DATA_LENGTH` + `INDEX_LENGTH`) | **18,59 MiB** (19.496.960 B Daten, 0 B Index) |

**Beide Kontrollen gehen auf.** 335.610 ist Zeichen für Zeichen die Zahl aus M87 Variante 1, und
`SUM(anzahl)` = 3.341.519 heißt: **jede Nachricht ist genau einmal gezählt**, keine doppelt, keine
verloren.

**Fülldauer, 22 Scheiben, `INSERT … SELECT` je Scheibe:**

| Scheibe | ms | Scheibe | ms | Scheibe | ms |
|---|---:|---|---:|---|---:|
| 2024-10 | 3.668,4 | 2025-04 | 3.472,6 | 2025-10 | 3.721,9 |
| 2024-11 | 3.205,9 | 2025-05 | 3.714,9 | 2025-11 | 3.645,0 |
| 2024-12 | 2.561,6 | 2025-06 | 3.561,2 | 2025-12 | 3.304,5 |
| 2025-01 | 2.984,1 | 2025-07 | 3.812,5 | 2026-01 … 2026-05 | 1,7 – 4,2 |
| 2025-02 | 2.843,7 | 2025-08 | 3.562,5 | 2026-06 | 72,1 |
| 2025-03 | 3.263,1 | 2025-09 | **3.827,1** | 2026-07 | 7,7 |

**Summe 51,242 s für 22 Monate, teuerste Scheibe 3,827 s.** Das ist zugleich eine Vorwegnahme von
M92 — mit dem Unterschied, dass hier das Schreiben enthalten ist und M92 nur die Aggregation misst.

**Gegenprobe, dass `GlassfishDB` unberührt ist:** 3.341.519 Zeilen, Datenstand `2026-07-08 17:21:10`
— unverändert. Geschrieben wurde ausschließlich in `overlord_monitor`.

> **`@@global.read_only = 1` hat den Schreibvorgang nicht verhindert**, weil `monitor_write` die
> dafür nötige Berechtigung trägt — dieselbe, mit der die Anwendung `SPRING_SESSION` schreibt.
> Der `read_only`-Schalter ist in diesem Projekt der **Nachweis, dass es die Testkopie ist**, nicht
> der Schutz vor Schreibzugriffen. Der Schutz von `GlassfishDB` kommt aus den Rechten von
> `monitor_write`, das dort nur `SELECT` hat.

## Die gemessene Abfrage, Variante A — die des Auftrags

```sql
SELECT r.stunde, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup_probe r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2026-07-06 17:00:00'
  AND r.stunde <  '2026-07-08 18:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
```

`LEFT JOIN` auf `process_catalog` und nicht `JOIN`: Der Katalog deckt nur 1.160 der 1.503 Prozesse
(V2). Ein innerer Join verlöre 343 Prozesse stillschweigend.

**Zwei Fenster je Mandant**, aus demselben Grund wie in M88: Fenster D trägt 256 Nachrichten auf zwei
Prozessen. Daneben läuft `D2` — dieselben 48 Stunden Breite, aber im dichten Bestand.

| Fall | Ergebniszeilen | Nachrichten | Aufwärmlauf | **beste von fünf** |
|---|---:|---:|---:|---:|
| **NEXANS, Fenster D** | 2 | 285 | 2,481 ms | **0,738 ms** |
| **SUTTONS, Fenster D** | **0** | — | 0,814 ms | **0,693 ms** |
| NEXANS, D2 (dichte 48 h) | 110 | 10.252 | 6,940 ms | **6,793 ms** |
| SUTTONS, D2 (dichte 48 h) | 53 | 1.357 | 6,112 ms | **6,092 ms** |

**`SUTTONS` liefert in Fenster D null Zeilen.** Die beiden Prozesse, die den Bestand bis zuletzt
tragen, gehören nicht zu diesem Mandanten. Das Dashboard von `SUTTONS` wäre in seinem
Standardfenster leer — auf der Testkopie, nicht notwendig in Produktion.

## Der Plan — und die Antwort auf die Frage, die M89 ausdrücklich stellt

> Gefragt war: *„Zusätzlich festzuhalten: welche Tabelle der Plan als Einstieg wählt. Bei M80 war es
> `ProjectMandant`, bei beiden Mandanten. Bleibt es dabei, ist die Mandantentrennung auch im Plan
> sichtbar und nicht nur im Text."*

**Es bleibt nicht dabei. Der Einstieg hängt am Mandanten.**

**`NEXANS`** — Einstieg ist **`r`, die Rollup-Tabelle**:

```
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key     | key_len | ref                           | rows | Extra                                        |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY | 5       | NULL                          |  926 | Using where; Using temporary; Using filesort |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id |    1 | Using where                                  |
|    1 | SIMPLE      | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const |    1 | Using where; Using index                     |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       |    1 | Using index                                  |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
```

**`SUTTONS`** — Einstieg ist **`pm`, `ProjectMandant`**, wie bei M80:

```
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key                        | key_len | ref                           | rows | Extra                                                     |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
|    1 | SIMPLE      | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         |    1 | Using where; Using index; Using temporary; Using filesort |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      |    1 | Using index                                               |
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          |  926 | Using where; Using join buffer (flat, BNL join)           |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id |    1 | Using where                                               |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
```

**Das Muster ist stabil und hängt nicht an der Abfrageform:** In allen acht gemessenen Plänen
(Variante A und B, Fenster D, D2 und B) steigt `NEXANS` über `r` ein und `SUTTONS` über `pm`.
Der Grund liegt auf der Hand: `NEXANS` hält 733 von 1.503 Prozessen in 17 Projekten, `SUTTONS`
17 Prozesse in **einem**. Für `SUTTONS` ist der Mandantenfilter die schärfste Bedingung im ganzen
Statement, für `NEXANS` ist es das Zeitfenster.

> **Ist die Mandantentrennung damit im Plan sichtbar? Ja — aber an zwei verschiedenen Stellen.**
> Bei `SUTTONS` steht sie vorne, als `ref … const` auf `ProjectMandant_Mandant_idx`. Bei `NEXANS`
> steht sie hinten, als `eq_ref` auf `PRIMARY` mit `key_len = 292` und
> `ref = GlassfishDB.p.ProjectID,const` — **beide** Teile des Primärschlüssels
> `(ProjectID, MandantID)` sind belegt, `used_key_parts: ["ProjectID", "MandantID"]`, und die
> angehängte Bedingung lautet wörtlich `pm.MandantID = 'NEXANS'`. Die Trennung ist in beiden Fällen
> im Plan nachweisbar; sie ist nur nicht in beiden Fällen die *Einstiegs*bedingung.
>
> **Das ist kein Mangel, sondern der Optimierer, der seine Arbeit tut** — aber es widerlegt den
> Satz, dass die Mandantentrennung *immer* als Einstieg erscheint. Wer das als Prüfkriterium in
> einen Test schreibt, bekommt bei großen Mandanten ein rotes Licht ohne Fehler. Siehe Befund 9.

## Befund 9 — der `LEFT JOIN` auf `process_catalog` wird vollständig wegoptimiert

**In keinem der vier Pläne aus Sitzung 5b kommt `process_catalog` vor. Kein einziges Mal.**
`EXPLAIN FORMAT=JSON` führt genau vier Tabellen: `r`, `p`, `pm`, `pr`.

Der Grund ist sauber: Keine Spalte von `c` wird verwendet, und der Join läuft auf den
Primärschlüssel — er kann die Zeilenmenge also weder vergrößern noch verkleinern. MariaDB entfernt
ihn und hat recht damit.

> **Damit misst die Abfrage, die der Auftrag wörtlich beschreibt, den Katalog-Join nicht — sie misst
> seine Abwesenheit.** Der Auftrag verlangt „gejoint … auf `process_catalog` für Partner und
> Richtung, gruppiert nach Stunde und Rohstatus". Beides zusammen geht nicht: Wer nur nach Stunde
> und Rohstatus gruppiert, *verwendet* Partner und Richtung nicht, und dann steht der Join zwar im
> Text, aber nicht im Plan.

**Variante B holt ihn zurück**, indem Partner und Richtung tatsächlich in die Gruppierung eingehen.
Sie ist zugleich näher an dem, was 10b braucht. G1 bleibt gewahrt: Der Partnername verlässt die
innere Abfrage nicht, nach außen dringen nur die Zahl der Partner-Eimer, die Richtung
(`EINGEHEND`/`AUSGEHEND` ist Konfigurationsvokabular) und „zugeordnet ja/nein" nach E‑i.

```sql
SELECT t.stunde, t.message_status, t.richtung, t.zugeordnet,
       COUNT(*) AS partner_eimer, SUM(t.anzahl) AS nachrichten
FROM (
  SELECT r.stunde,
         r.message_status,
         COALESCE(c.richtung, 'nicht zugeordnet') AS richtung,
         CASE WHEN c.process_id IS NULL                THEN 'nein'
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN 'nein'
              WHEN c.partner IS NULL OR c.partner = '' THEN 'nein'
              ELSE 'ja' END                            AS zugeordnet,
         CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END                       AS partner,
         SUM(r.anzahl) AS anzahl
  FROM overlord_monitor.message_rollup_probe r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 00:00:00'
    AND r.stunde <  '2025-12-30 00:00:00'
  GROUP BY r.stunde, r.message_status, richtung, zugeordnet, partner
) t
GROUP BY t.stunde, t.message_status, t.richtung, t.zugeordnet
ORDER BY t.stunde, t.message_status;
```

Jetzt steht `c` im Plan — als **`eq_ref` auf `PRIMARY`, der bestmögliche Zugriff**, genau wie bei
M80:

```
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          |  926 | Using temporary; Using filesort              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          |  926 | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id |    1 |                                              |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id |    1 | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const |    1 | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       |    1 | Using index                                  |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
```

## Alle Laufzeiten im Überblick

| Fall | Fenster | Katalog im Plan? | Ergebniszeilen | Nachrichten | **beste von fünf** |
|---|---|---|---:|---:|---:|
| A · NEXANS | **D** (48 h, Anker V5) | nein (wegoptimiert) | 2 | 285 | **0,738 ms** |
| A · SUTTONS | **D** | nein | 0 | — | **0,693 ms** |
| A · NEXANS | D2 (dichte 48 h) | nein | 110 | 10.252 | **6,793 ms** |
| A · SUTTONS | D2 | nein | 53 | 1.357 | **6,092 ms** |
| **B · NEXANS** | D2 | **ja, `eq_ref`** | — | 10.252 | **11,299 ms** |
| **B · SUTTONS** | D2 | **ja, `eq_ref`** | — | 1.357 | **10,134 ms** |
| A · NEXANS | **B** (ein Monat) | nein | — | **180.251** | **148,840 ms** |
| A · SUTTONS | **B** | nein | — | 21.516 | **112,625 ms** |
| **B · NEXANS** | **B** | **ja** | — | 180.251 | **237,673 ms** |
| **B · SUTTONS** | **B** | **ja** | — | 21.516 | **150,521 ms** |

**Was der Katalog-Join kostet:** Faktor **1,66** in Fenster D2 (beide Mandanten), Faktor **1,60**
(NEXANS) bzw. **1,34** (SUTTONS) in Fenster B. Das deckt sich mit M80, das für die Pflegeliste
**+68 %** gemessen hat — eine unabhängige Bestätigung an einer ganz anderen Abfrage.

> **Eine Kontrolle, die aufgehen musste und aufgeht.** In Fenster B liefert die Abfrage für `NEXANS`
> **180.251** Nachrichten. Das ist Zeichen für Zeichen die Zahl, die
> [`messungen-schritt4.md`](messungen-schritt4.md) M11/M12 für „Fenster B, Mandant `NEXANS`" nennt
> und die `messungen-schritt5.md` Z. 90–92 als Bezugsgröße zitiert. Über alle Mandanten summiert die
> Probetabelle im selben Fenster **214.330** — die Zahl aus M17. **Der Rollup und die Mandantenkette
> reproduzieren zwei unabhängig erhobene Zahlen aus früheren Runden auf die Einheit genau.**

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„Das Budget ist **500 ms für die ganze Landingpage**, und diese Abfrage ist nur
> ein Teil davon. Bleibt sie unter 150 ms, trägt E‑a A ohne Vorbehalt. Zwischen 150 und 400 ms:
> E‑a A trägt, aber die Kachelabfragen dürfen nicht zusätzlich live rechnen. Über 400 ms: Der
> Mandant gehört doch in den Schlüssel, und E‑a ist dem Auftraggeber erneut vorzulegen."*

**Für das, was gefragt war — Fenster D — trifft der oberste Zweig, und zwar um mehr als zwei
Größenordnungen: 0,738 ms und 0,693 ms.** Aber das ist die Messung eines fast leeren Fensters und
trägt für sich genommen nichts.

**Die belastbare Antwort steht in den dichten Fenstern, und sie ist gestaffelt:**

| Fenster und Fassung | Laufzeit | Zweig der Deutung |
|---|---:|---|
| **48 h, Standardfenster des Dashboards (E‑a/Standard), ohne Katalog** | 6,79 ms | **unter 150 ms — E‑a A trägt ohne Vorbehalt** |
| **48 h, mit Katalog-Join (die Fassung, die 10b braucht)** | 11,30 ms | **unter 150 ms — E‑a A trägt ohne Vorbehalt** |
| ein Monat, ohne Katalog | 148,84 ms | knapp unter 150 ms |
| **ein Monat, mit Katalog-Join** | **237,67 ms** | **zwischen 150 und 400 ms** |

> **Die Deutung trifft — im Standardfenster ohne jeden Vorbehalt.** Bei Stundenauflösung über
> 48 Stunden, also genau der Voreinstellung aus §2, kostet die Leseabfrage **11,30 ms** mit allem
> Drum und Dran. Das sind **2,3 %** des 500‑ms‑Budgets der ganzen Landingpage. **E‑a A trägt: Der
> Mandant muss nicht in den Schlüssel.**
>
> **Über 400 ms kommt keine der zehn gemessenen Fassungen.** Die Frage „gehört der Mandant doch in
> den Schlüssel" ist damit **nicht** aufzuwerfen, auch nicht für ein Monatsfenster.
>
> **Der mittlere Zweig gilt trotzdem, und er gilt für das Monatsfenster:** Bei 237,67 ms bleiben von
> 500 ms noch 262 ms für alles Übrige. **Die Kachelabfragen dürfen dann nicht zusätzlich live
> rechnen** — und genau das täte die Überfällig-Kachel nach E‑c. Ob das trägt, entscheidet M90.

**Ein Zusatz, den die Zahlen hergeben und der 10b gehört:** Die Laufzeit hängt nicht am Mandanten,
sondern an der Zahl der Rollup-Zeilen im Fenster. `NEXANS` hat in Fenster B 8.861 Rollup-Zeilen und
braucht 148,84 ms, `SUTTONS` 6.142 und braucht 112,63 ms — 16,8 bzw. 18,3 µs je Zeile. **Der große
Mandant ist nicht überproportional teuer.** Beide Pläne lesen denselben Indexbereich der
Rollup-Tabelle (`rows = 45.354` für den ganzen Monat) und werfen danach weg, was nicht zum Mandanten
gehört; dass `NEXANS` 86 % davon behält und `SUTTONS` 10 %, ändert an der gelesenen Menge nichts.

> **Und daran hängt der einzige Vorbehalt, den diese Messung wirklich trägt.** Der Mandantenfilter
> greift **nach** dem Zeitfenster, nicht davor — jeder Mandant liest den Rollup-Bereich **aller**
> Mandanten. Bei zehn Mandanten ist das der Faktor, um den zu viel gelesen wird. Genau das würde
> ein Mandant im Schlüssel (E‑a B) sparen. Es lohnt sich hier trotzdem nicht: 11,30 ms im
> Standardfenster lassen keinen Raum für eine Optimierung, die die Zeile breiter macht. **Sollte
> die Zahl der Mandanten deutlich über zehn wachsen, ist das die Stelle, an der neu zu rechnen
> ist** — nicht heute.
