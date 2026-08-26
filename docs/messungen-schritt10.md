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
| Serverzeit Ende | `2026-08-26 10:37:44` |
| `@@global.read_only` | **`1`** — in **jeder** der achtzehn Sitzungen als erste Abfrage, und in Sitzung 8 erneut am Ende |
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
| **S1** — nur `SELECT` | **erfüllt mit der benannten Ausnahme** | Sechzehn der achtzehn Sitzungen fahren ausschließlich `SELECT`, `SET`, `EXPLAIN`, `SHOW` mit `monitor_read`. Die beiden Ausnahmen sind vom Auftrag freigegeben: **5a** legt `message_rollup_probe` an und befüllt sie, **8** löscht sie. Beide laufen mit `monitor_write`, das auf `GlassfishDB` nur `SELECT` hat. **Kein Schreibzugriff auf `GlassfishDB` in irgendeiner Sitzung** — gegengeprüft in 5a‑7 und 8‑6/8‑7 |
| **L4** — `MessageProperty` | **erfüllt** | Die Tabelle wird in dieser Runde **gar nicht** angefasst. Einziges Vorkommen ist ihre Größe in V1, aus `information_schema` |
| **L7** — zwei Mandanten | **erfüllt** | M89, M90 je für `NEXANS` und `SUTTONS`; M91 zusätzlich für `VOTG`. M86, M87, M88 und M92 sind mandantenübergreifend und wären mit einem Mandantenfilter etwas anderes — begründet je Messung |
| **L9** — voller Durchlauf | **erfüllt** | Jeder volle Durchlauf ist einzeln begründet: M87 (die Zeilenzahl einer Tabelle, die es nicht gibt, ist anders nicht zu bekommen), M92‑1 (Scheibengrenzen über den Bestand). **Keiner ist Vorbild für Anwendungscode**, und das steht an jeder Fundstelle |
| **L10** — *gemessen war X / behauptet wird Y* | **erfüllt** | In V1, V2, V3, V6, M86 (a) und im Abschnitt „Drei Angaben des Auftrags, die der Bestand nicht trägt" |
| **L15** — `EXPLAIN` im Volltext | **erfüllt bis auf eine Stelle** | Je Messabfrage, Pläne ungekürzt. **Nicht erfüllt für Fenster H2 in M88** — dort ist kein Plan erhoben. Die fünf erhobenen decken den Mengenbereich von 256 bis 24.218 geschätzten Sätzen ab und sind untereinander identisch; die Lücke ist trotzdem eine. Siehe Abweichung A8 |
| **G1** — Geheimhaltung | **erfüllt** | Keine `ProcessID`, kein `ProcessName`, kein Partnername, keine `MessageID`, keine Belegnummer, kein Hostname, kein `MessagePropertyValue`. M91 gibt Partner und Prozesse **maskiert** als `Partner 1`, `Partner 2` … aus; die Zuordnung zu echten Namen ist **nirgends festgehalten**, auch nicht in den Sitzungsdateien |
| **Z1** — kein `NOW()` | **erfüllt** | Jeder Zeitpunkt steht als **Literal im Statement**. `NOW()` kommt ausschließlich in den Rahmenzeilen (`SELECT NOW() AS serverzeit`) vor, nie in einer Messabfrage |

### Die Sitzungen

Achtzehn Sitzungen statt der acht des Sitzungsplans — jede Messung, die nachgefahren oder ergänzt
werden musste, hat eine eigene bekommen, damit die Reihenfolge nachvollziehbar bleibt. Alle
sequenziell, jede eine eigene Verbindung, **kein paralleler Lauf**.

| # | Datei | Inhalt | Benutzer | Serverzeit |
|---|---|---|---|---|
| 1 | `s1-rahmen-v1-v5.sql` | Rahmen, V1–V5, Nummernprobe | read | 09:45:52 – 09:46:27 |
| 2 | `s2-m86-wandert-lastupdate.sql` | **M86** (a), (b), (c) | read | 09:50:36 – 09:51:33 |
| 3 | `s3-m87-rollupzeilen.sql` | **M87**, vier Varianten | read | 09:56:25 – 09:58:26 |
| 3b | `s3b-m87-jahresscheiben.sql` | M87‑5 in Jahresscheiben, nachdem die Grenze gerissen war | read | 09:59:39 – 10:00:31 |
| 4a | `s4a-m88-vorprobe.sql` | Vorprobe zu M88: welche Stunde, und was steht in Fenster D? | read | 10:06:11 – 10:06:19 |
| 4b | `s4b-m88-deltalauf.sql` | **M88**, sechs Fenster à sechs Läufe | read | 10:08:28 – 10:08:30 |
| 5a | `s5a-m89-probetabelle.sql` | **M89** — Probetabelle anlegen, in 22 Monatsscheiben füllen | **write** | 10:13:40 – 10:14:32 |
| 5b | `s5b-m89-leseabfrage.sql` | M89 Variante A, Fenster D und D2 | read | 10:16:06 |
| 5c | `s5c-m89-katalogjoin.sql` | M89 Variante B (Katalog wirklich gelesen) und Fenster B | read | 10:17:32 – 10:17:34 |
| 5d | `s5d-m89-katalog-monat.sql` | M89 Variante B über Fenster B | read | 10:18:24 – 10:18:27 |
| 6 | `s6-m90-ueberfaellig.sql` | **M90**, drei Fassungen je Mandant | read | 10:21:43 – 10:21:44 |
| 6b | `s6b-m91-verteilung.sql` | **M91**, erster Lauf | read | 10:24:20 – 10:24:41 |
| 6c | `s6c-m91-nachmessung.sql` | M91 nachgemessen (Befund 11), Katalogabdeckung je Mandant | read | 10:26:11 – 10:26:29 |
| 6d | `s6d-m91-richtung.sql` | M91 Richtung, größter Prozess je Mandant | read | 10:28:03 – 10:28:28 |
| 6e | `s6e-m91-richtungsluecke.sql` | M91, die Zahl, die zweimal gleich herauskam (Befund 10) | read | 10:29:24 – 10:29:36 |
| 7a | `s7a-m92-scheibengrenzen.sql` | **M92**, Scheibengrenzen und größte Scheibe | read | 10:33:11 – 10:33:41 |
| 7b | `s7b-m92-rueckwaertslauf.sql` | M92, vier Scheiben à sechs Läufe | read | 10:34:26 – 10:35:22 |
| 8 | `s8-abschluss.sql` | **Probetabelle löschen**, Löschung nachweisen, Abschluss | **write** | 10:37:43 – 10:37:44 |

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

**Ergebnis, Fenster B** (Laufzeit **17.182,4 ms**; n = 214.330, deckungsgleich mit M17):

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
Laufzeiten 517,5 ms · 531,8 ms · 487,3 ms für die drei Auswertungen — gegen 17.182,4 ms ·
17.407,7 ms · 20.556,8 ms in Fenster B. **Der Faktor 33 bis 42 entspricht dem Mengenverhältnis
34,3** (214.330 zu 6.249); auch diese Auswertung ist in der Zeilenzahl linear.

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

> ⚠️ **Diese Antwort gilt für die Fenster, in denen sie gemessen ist — 256 bis 12.332 Zeilen.**
> M92 hat dieselbe Abfrage über **Monatsscheiben** gefahren und dort das Gegenteil gefunden:
> Oberhalb einer geschätzten Bereichsgröße von rund einer halben Million Sätzen wählt der
> Optimierer **`MessageLastUpdateProcessMessageIDX`**. Für den **stündlichen** Delta-Lauf, um den
> es hier geht, bleibt es bei `MessageLastUpdateIDX`; für den Rückwärtslauf nicht. Die vollständige
> Antwort steht in M92 unter „Der Plan — und eine Ergänzung zu M88".

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
kosten für sich genommen nichts. Der Aufwärmlauf kostet zwischen **0,5 %** (A) und **21,9 %** (H3)
mehr als der beste Lauf — im Einzelnen: A 0,5 % · D 2,0 % · D2 3,0 % · H2 4,2 % · H1 14,9 % ·
H3 21,9 %. **Der Aufschlag fällt dort am höchsten aus, wo absolut am wenigsten zu tun ist** (H3 mit
285 Zeilen): Er ist im Wesentlichen ein fester Anteil für den kalten Abfrageplan, kein
mengenabhängiger.

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

---

# M90 — „Überfällig" live, mit Mandantenfilter und Fenster

Sitzung 6, `s6-m90-ueberfaellig.sql`. Die Abfrage, die E‑c A verlangt — und sie ist eine **andere**
als die in `docs/message-status.md` gemessene: Dort fehlten Mandantenfilter und Zeitfenster.

## Die gemessene Abfrage

```sql
SELECT COUNT(*) AS ueberfaellig
FROM GlassfishDB.Message m
WHERE m.MessageStatus IN ('SUSPENDED','RUNNING')
  AND m.MessageTimeout > 0
  AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2026-07-08 17:21:10'
  AND m.MessageLastUpdate >= '2025-11-30 00:00:00'
  AND m.MessageLastUpdate <  '2025-12-30 00:00:00'
  AND EXISTS (
        SELECT 1
        FROM GlassfishDB.Process        p
        JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
        JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
        WHERE p.ProcessID = m.ProcessID
          AND pm.MandantID = 'NEXANS'
      );
```

**Z1 ist eingehalten:** Der Anker steht als **Literal** im Statement — `'2026-07-08 17:21:10'`, der
Wert aus V5. Kein `NOW()`. **V4 ist eingehalten:** Die Spaltennamen der Kette sind gegen
`information_schema` erhoben, nicht aus einer Projektdatei übernommen.

## Die Zahlen — drei Fenster nebeneinander

| Fassung | Fenster | `NEXANS` | `SUTTONS` |
|---|---|---:|---:|
| **1** | **D** — was die Kachel nach E‑h zeigt | **0** | **0** |
| **2** | **B** — ein Monat | **538** | **0** |
| **3** | **G** — was „insgesamt" wäre | **538** | **0** |

Die Bezugsgrößen dazu, ohne Mandantenfilter:

| | Zeilen |
|---|---:|
| offen (`SUSPENDED` oder `RUNNING`), Gesamtbestand | **538** |
| davon `MessageTimeout > 0` | **538** |
| davon überfällig gegen den Anker | **538** |
| `RUNNING` (Gegenprobe) | **0** |
| `MessageTimeout IS NULL` (Gegenprobe) | **0** |

**Alle 538 offenen Zeilen gehören `NEXANS`**, und sie liegen zwischen `2025-12-23 11:04:13` und
`2025-12-29 12:37:16`. Kein anderer Mandant hat auch nur eine.

> **Die Zahl, um derentwillen die zweite Kachelzahl überhaupt erwogen wird — und sie fällt
> maximal aus.** Der Abstand zwischen „im Zeitraum" und „insgesamt" ist auf dieser Testkopie
> **0 gegen 538**. Die Kachel nach E‑h zeigt im Standardfenster **nichts**, während „insgesamt"
> 538 stünde. Das ist nicht ein Unterschied in der Größenordnung, sondern der Unterschied zwischen
> leer und nicht leer.
>
> **Und es ist ein Artefakt des Bestands, keine fachliche Aussage.** Die 538 offenen Zeilen enden am
> 29.12.2025, der Anker liegt am 08.07.2026 — mehr als ein halbes Jahr später. Jedes Fenster, das
> am Anker endet und kürzer als sechs Monate ist, enthält null davon. In Produktion, wo offene
> Zeilen am aktuellen Rand entstehen, wäre das Verhältnis umgekehrt.

## Die Pläne

**`NEXANS`, Fenster D** — MariaDB setzt den `rowid`-Filter aus 10.6 ein und kombiniert zwei Indizes:

```
+------+-------------+-------+--------------+--------------------------------------------------------------------------------------------------------+---------------------------------------+---------+-------------------------------+----------+--------------------------------------------------------+
| id   | select_type | table | type         | possible_keys                                                                                          | key                                   | key_len | ref                           | rows     | Extra                                                  |
+------+-------------+-------+--------------+--------------------------------------------------------------------------------------------------------+---------------------------------------+---------+-------------------------------+----------+--------------------------------------------------------+
|    1 | PRIMARY     | m     | range|filter | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX|MessageStatusIDX | 5|123   | NULL                          | 256 (0%) | Using index condition; Using where; Using rowid filter |
|    1 | PRIMARY     | p     | eq_ref       | PRIMARY,Process_ProjectFK                                                                              | PRIMARY                               | 146     | GlassfishDB.m.ProcessID       | 1        | Using where                                            |
|    1 | PRIMARY     | pm    | eq_ref       | PRIMARY,ProjectMandant_Mandant_idx                                                                     | PRIMARY                               | 292     | GlassfishDB.p.ProjectID,const | 1        | Using where; Using index                               |
|    1 | PRIMARY     | pr    | eq_ref       | PRIMARY                                                                                                | PRIMARY                               | 146     | GlassfishDB.p.ProjectID       | 1        | Using index                                            |
+------+-------------+-------+--------------+--------------------------------------------------------------------------------------------------------+---------------------------------------+---------+-------------------------------+----------+--------------------------------------------------------+
```

**`NEXANS`, Fenster B und Fenster G** — Einstieg über **`MessageStatusIDX`**, `rows = 539`:

```
+------+-------------+-------+--------+-------------------------------------------------+------------------+---------+-------------------------------+------+------------------------------------+
| id   | select_type | table | type   | possible_keys                                   | key              | key_len | ref                           | rows | Extra                              |
+------+-------------+-------+--------+-------------------------------------------------+------------------+---------+-------------------------------+------+------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageStatusIDX | MessageStatusIDX | 123     | NULL                          |  539 | Using index condition; Using where |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY,Process_ProjectFK                       | PRIMARY          | 146     | GlassfishDB.m.ProcessID       |    1 | Using where                        |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx              | PRIMARY          | 292     | GlassfishDB.p.ProjectID,const |    1 | Using where; Using index           |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                         | PRIMARY          | 146     | GlassfishDB.p.ProjectID       |    1 | Using index                        |
+------+-------------+-------+--------+-------------------------------------------------+------------------+---------+-------------------------------+------+------------------------------------+
```

**`SUTTONS`** steigt in allen drei Fassungen über **`ProjectMandant_Mandant_idx`** ein, mit
`Using join buffer (flat, BNL join)` auf `m` — dasselbe Muster wie in M89. **Der Einstieg hängt
auch hier am Mandanten und nicht an der Abfrage.**

> **`rows = 539` gegen 538 wahre Zeilen — die Schätzung ist hier auf eine Zeile genau.** Das ist
> derselbe Wert, den `message-status.md` für die Fassung ohne Mandantenfilter nennt, und der
> Gegenpol zu Befund 8: Wo MariaDB im Bereich zählen kann, zählt es richtig.
> `MessageStatusIDX` hat `CARDINALITY = 18` (V3) — die Schätzung kommt hier also **nicht** aus der
> Kardinalität, sondern aus einer Stichprobe über zwei diskrete Werte.

## Laufzeit — ein Aufwärmlauf, dann beste von fünf

| Fall | Fenster | Ergebnis | Aufwärmlauf | **beste von fünf** |
|---|---|---:|---:|---:|
| `NEXANS` | **D** | 0 | 3,744 ms | **2,275 ms** |
| `SUTTONS` | **D** | 0 | 2,270 ms | **2,274 ms** |
| `NEXANS` | **B** | 538 | 6,195 ms | **5,127 ms** |
| `SUTTONS` | **B** | 0 | 6,215 ms | **5,371 ms** |
| `NEXANS` | **G** | 538 | 4,612 ms | **4,275 ms** |
| `SUTTONS` | **G** | 0 | 4,100 ms | **4,115 ms** |

> **Ein Ergebnis, das der Erwartung widerspricht und das benannt gehört: Fenster G ist billiger als
> Fenster B.** 4,275 ms ohne jedes Zeitfenster gegen 5,127 ms mit einem Monatsfenster.
> Der Grund steht im Plan: **Ohne Zeitfenster bleibt nur ein Zugriffsweg** — `MessageStatusIDX`,
> 539 Sätze, fertig. **Mit** Zeitfenster muss MariaDB zusätzlich prüfen, ob sich der Einstieg über
> `MessageLastUpdateIDX` lohnt, und hängt eine weitere Bedingung an. Das Zeitfenster verengt hier
> nichts, weil der Statusfilter bereits auf 539 von 3,34 Millionen Zeilen herunterführt — es kostet
> nur.
>
> **Für 10b heißt das:** Die zweite Kachelzahl („insgesamt") ist **nicht teurer** als die erste,
> sondern billiger. Wer sie aus Kostengründen weglassen wollte, hätte kein Kostenargument.

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„Unter 50 ms in allen drei Fassungen: Die Ausnahme von L2 ist wohlfeil, und die
> zweite Zahl an der Kachel kostet nichts. Über 500 ms in Fassung 3: Die zweite Zahl kommt nicht
> live, und 10b braucht dafür einen anderen Weg. Über 500 ms in Fassung 1: **E‑c A trägt nicht**
> und ist vorzulegen."*

**Der oberste Zweig trifft, in allen drei Fassungen und für beide Mandanten.** Die teuerste
gemessene Fassung kostet **5,371 ms** — ein Zehntel der 50‑ms‑Schwelle.

> **Also, wie vorregistriert, so gesagt: Die Ausnahme von Regel L2 ist wohlfeil.** Eine
> Live-Abfrage, die 5 ms kostet, ist keine Live-Aggregation über `Message` im Sinne der Regel —
> sie liest 539 Indexsätze und schlägt für jeden einmal in der Mandantenkette nach. **E‑c A trägt.**
>
> **Und die zweite Zahl an der Kachel kostet nichts** — sie kostet sogar weniger als die erste.
>
> **Zusammen mit M89 ergibt das die Rechnung für die Landingpage:** 11,30 ms Rollup-Leseabfrage
> (48 h, mit Katalog) plus 2,28 ms Überfällig-Kachel plus 4,28 ms für die zweite Zahl =
> **17,86 ms** von 500 ms. Der Vorbehalt aus M89 („die Kachelabfragen dürfen nicht zusätzlich live
> rechnen") greift beim Standardfenster **nicht** — er greift erst beim Monatsfenster, und auch
> dort bleiben 237,67 + 5,13 + 4,28 = **247,08 ms** deutlich unter dem Budget.

## Die Grenze, die in den Befund gehört

Der Auftrag schreibt sie vor, und sie gilt unverändert und ungemildert:

> **`RUNNING` kommt auf der Testkopie null Mal vor** (gegengeprüft: 0), und die 538 offenen Zeilen
> enden am 29.12.2025, mehr als ein halbes Jahr vor dem Anker. **Gemessen sind hier der Plan und die
> Laufzeit, nicht die fachliche Größenordnung.** Die Kategorie „Überfällig" ist gegen diese
> Testkopie nicht prüfbar, und das steht seit Schritt 4 so in `docs/message-status.md`.
>
> Insbesondere: Dass Fassung 1 **null** liefert, sagt nichts darüber, ob die Kachel in Produktion
> eine sinnvolle Zahl zeigt. Es sagt nur, dass sie hier keine zeigt.
>
> **Was die Messung dagegen sehr wohl trägt:** Der Plan ist bei 0 Treffern und bei 538 Treffern
> derselbe, und die Laufzeit unterscheidet sich um 3 ms. Die Kategorie skaliert nicht mit ihrem
> Ergebnis, sondern mit der Zahl der offenen Zeilen — und die ist durch `MessageStatusIDX`
> begrenzt, nicht durch den Bestand.

---

# M91 — Trägt eine Verteilung nach Partner überhaupt?

Sitzungen 6b bis 6e. Offener Punkt 39 aus `messungen-schritt9.md`.

> **G1 gilt hier schärfer als sonst, und das ist der Kern der Darstellung.** Es wird **nie** ein
> Partnername ausgegeben. Die Rangfolge entsteht über `ROW_NUMBER()` **im Server**; der Name
> verlässt die innere Abfrage nicht. **Die Zuordnung von Rang zu echtem Namen ist nirgends
> festgehalten** — auch nicht in der Rohausgabe unter `ergebnis/`, auch nicht in den
> Sitzungsdateien. Die Richtung (`EINGEHEND`/`AUSGEHEND`) ist Konfigurationsvokabular und steht im
> Klartext.

## Der Katalogstand als Bezugsgröße — und er ist die eigentliche Antwort

Der Auftrag sagt: *„Der Katalogstand aus V2 steht als Bezugsgröße daneben. Ist er dünn, ist die
Verteilung entsprechend zu lesen — und das ist dann der Befund."* **Er ist nicht dünn. Er ist
ungleich verteilt.**

| Mandant | Prozesse | mit Katalogzeile | `GEPFLEGT` | gepflegt **mit Partner** | `OFFEN` | versch. Partner |
|---|---:|---:|---:|---:|---:|---:|
| **NEXANS** | 733 | **733** | **733** | **517** | 0 | 154 |
| **VOTG** | 390 | **390** | **0** | **0** | **390** | 0 |
| IBIS | 192 | 2 | 2 | 2 | 0 | 1 |
| IBISGUS | 89 | **0** | — | 0 | — | 0 |
| ZAST | 35 | 35 | 35 | 35 | 0 | 34 |
| NXHBE | 17 | **0** | — | 0 | — | 0 |
| **SUTTONS** | 17 | **0** | — | **0** | — | 0 |
| EDITIONLINGERI | 9 | **0** | — | 0 | — | 0 |
| WOC | 4 | **0** | — | 0 | — | 0 |
| SYSTEM | 4 | **0** | — | 0 | — | 0 |

Summen zur Kontrolle: 1.490 Prozesse in der Kette (13 weitere hängen an einem Projekt ohne
Mandantenzeile, `Process.ProjectID IS NULL` kommt **null** Mal vor); 1.160 Katalogzeilen = V2;
517 + 35 + 2 = **554** gepflegt mit Partner = V2.

> **Drei von zehn Mandanten sind kuratiert, sechs haben keine einzige Katalogzeile, und einer hat
> 390 offene.** Der Katalog ist kein dünner Gesamtstand, sondern ein **vollständiger Stand für
> `NEXANS`, `ZAST` und zwei Zeilen von `IBIS`** — und ein leerer für alle übrigen.

## Die Verteilung nach Partner, maskiert

**`NEXANS`, Fenster A** (ein Tag, 5.043 Nachrichten):

| Bezeichnung | Anzahl | Anteil |
|---|---:|---:|
| **Partner 1** | **4.187** | **83,0260 %** |
| Partner 2 | 289 | 5,7307 % |
| *nicht zugeordnet* | *453* | *8,9827 %* |
| Partner 3 | 20 | 0,3966 % |
| Partner 4 | 17 | 0,3371 % |
| Partner 5 | 13 | 0,2578 % |
| Partner 6 | 10 | 0,1983 % |
| Partner 7 | 9 | 0,1785 % |
| Partner 8 | 9 | 0,1785 % |
| Partner 9 | 7 | 0,1388 % |
| Partner 10 | 6 | 0,1190 % |
| Partner 11–15 | 3 · 3 · 3 · 2 · 2 | je unter 0,06 % |

**`NEXANS`, Fenster B** (ein Monat, 180.251 Nachrichten):

| Bezeichnung | Anzahl | Anteil |
|---|---:|---:|
| **Partner 1** | **105.654** | **58,6149 %** |
| *nicht zugeordnet* | *28.352* | *15,7292 %* |
| Partner 2 | 19.839 | 11,0063 % |
| Partner 3 | 2.895 | 1,6061 % |
| Partner 4 | 2.019 | 1,1201 % |
| Partner 5 | 1.901 | 1,0546 % |
| Partner 6 | 1.827 | 1,0136 % |
| Partner 7 | 1.212 | 0,6724 % |
| Partner 8 | 1.174 | 0,6513 % |
| Partner 9 | 1.171 | 0,6496 % |
| Partner 10 | 981 | 0,5442 % |
| Partner 11–15 | 908 · 836 · 800 · 548 · 546 | je unter 0,51 % |

**`SUTTONS`** und **`VOTG`**, beide Fenster: **eine einzige Zeile, `nicht zugeordnet`, 100,0000 %.**

| Mandant | Fenster | Nachrichten | *nicht zugeordnet* | versch. Partner |
|---|---|---:|---:|---:|
| SUTTONS | A | 685 | **685 (100 %)** | **0** |
| SUTTONS | B | 21.516 | **21.516 (100 %)** | **0** |
| VOTG | A | 206 | **206 (100 %)** | **0** |
| VOTG | B | 6.104 | **6.104 (100 %)** | **0** |

## Die Anteile

| Mandant | Fenster | gesamt | versch. Partner | **größter** | **drei größte** | **zehn größte** |
|---|---|---:|---:|---:|---:|---:|
| NEXANS | A | 5.043 | 24 | 4.187 = **83,03 %** | 4.496 = **89,15 %** | 4.567 = **90,56 %** |
| NEXANS | B | 180.251 | 115 | 105.654 = **58,61 %** | 128.388 = **71,23 %** | 138.673 = **76,93 %** |
| SUTTONS | A / B | 685 / 21.516 | 0 | — | — | — |
| VOTG | A / B | 206 / 6.104 | 0 | — | — | — |

## „Nicht zugeordnet" nach E‑i — und wie es sich aufteilen *würde*

**E‑i ist entschieden.** Die folgende Aufteilung steht **nur zur Kenntnis** und sagt lediglich, wie
groß der Unterschied gewesen wäre.

| Mandant | Fenster | (a) keine Katalogzeile | (b) `OFFEN` | (c) `GEPFLEGT` ohne Partner | (d) zugeordnet |
|---|---|---:|---:|---:|---:|
| NEXANS | A | 0 | 0 | **453** (12 Proz.) | 4.590 (45 Proz.) |
| NEXANS | B | 0 | 0 | **28.352** (145 Proz.) | 151.899 (312 Proz.) |
| SUTTONS | A | **685** (11 Proz.) | 0 | 0 | 0 |
| SUTTONS | B | **21.516** (17 Proz.) | 0 | 0 | 0 |
| VOTG | A | 0 | **206** (14 Proz.) | 0 | 0 |
| VOTG | B | 0 | **6.104** (18 Proz.) | 0 | 0 |

> **Der Unterschied, den E‑i zusammenfasst, ist bei diesen drei Mandanten gar keiner** — jeder von
> ihnen hat **genau eine** der drei Ursachen: `NEXANS` ausschließlich (c), `SUTTONS` ausschließlich
> (a), `VOTG` ausschließlich (b). Eine getrennte Darstellung hätte für keinen der drei eine zweite
> Zeile ergeben. **E‑i kostet hier nichts an Aussagekraft.** Das ist ein Befund zugunsten von E‑i,
> aber ein zufälliger: Er gilt für diesen Katalogstand, nicht grundsätzlich.

## Die Verteilung nach Richtung

| Mandant | Fenster | `AUSGEHEND` | `EINGEHEND` | *nicht zugeordnet* |
|---|---|---:|---:|---:|
| NEXANS | A | 457 (9,06 %) | 399 (7,91 %) | **4.187 (83,03 %)** |
| NEXANS | B | 47.968 (26,61 %) | 26.629 (14,77 %) | **105.654 (58,61 %)** |
| SUTTONS | B | 0 | 0 | **21.516 (100 %)** |
| VOTG | B | 0 | 0 | **6.104 (100 %)** |

Der Katalog trägt die Richtung eigentlich gut: 384 `EINGEHEND` und 338 `AUSGEHEND` bei `NEXANS`,
nur **11** von 733 Katalogzeilen ohne Richtung. Trotzdem steht mehr als die Hälfte der Nachrichten
in „nicht zugeordnet". Das ist Befund 10.

## Befund 10 — elf Katalogzeilen, ein Partner, 54,44 % des Bestands, keine Richtung

In Fenster B kam der größte Partner mit **105.654** Nachrichten heraus (58,6149 %) — und der Eimer
„Richtung nicht zugeordnet" mit **exakt derselben Zahl**. Zwei voneinander unabhängige
Gruppierungen, dieselbe Zahl. Nachgeprüft in Sitzung 6e, statt sie für Zufall zu halten:

**Partner × Richtung, gekreuzt, `NEXANS` Fenster B:**

| Partnerlage | Richtungslage | Nachrichten | Prozesse | Anteil |
|---|---|---:|---:|---:|
| **`GEPFLEGT` mit Partner** | **ohne Richtung** | **105.654** | **7** | **58,6149 %** |
| `GEPFLEGT` ohne Partner | `AUSGEHEND` | 26.945 | 78 | 14,9486 % |
| `GEPFLEGT` mit Partner | `EINGEHEND` | 25.222 | 159 | 13,9927 % |
| `GEPFLEGT` mit Partner | `AUSGEHEND` | 21.023 | 146 | 11,6632 % |
| `GEPFLEGT` ohne Partner | `EINGEHEND` | 1.407 | 67 | 0,7806 % |

**Es ist kein Zufall.** Die **11** `NEXANS`-Katalogzeilen ohne Richtung tragen **alle** einen
Partner — und zwar **alle denselben**: `COUNT(DISTINCT partner)` = **1**.

**Über den Gesamtbestand von `NEXANS`** (Fenster G, unter L9 als Bezugsgröße):

| Lage | Nachrichten | Prozesse | Anteil |
|---|---:|---:|---:|
| **ohne Richtung** | **1.571.027** | **9** | **54,4416 %** |
| mit Richtung | 1.314.684 | 507 | 45,5584 % |

> **Ein einziger Partner, neun lebende Prozesse, 54,44 % des gesamten Nachrichtenaufkommens des
> größten Mandanten — und keine gepflegte Richtung.** Von 733 Katalogzeilen sind es elf, die
> fehlen. Es sind die elf, auf die es ankommt.
>
> **Für 10b heißt das:** Eine Verteilung nach Richtung zeigt für `NEXANS` heute mehr als die Hälfte
> des Volumens als „nicht zugeordnet" — nicht, weil der Katalog dünn wäre, sondern weil **elf
> Zeilen** fehlen. Das ist die billigste Katalogpflege im ganzen Projekt: elf Zeilen, und die
> Richtungsverteilung springt von **41,4 %** Abdeckung auf 100 % (Fenster B) beziehungsweise von
> **45,6 %** auf 100 % (Gesamtbestand `NEXANS`: 1.314.684 von 2.885.711 Nachrichten hängen heute an
> einem Prozess mit gepflegter Richtung).

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„Hält der größte Partner über 50 %, trägt ein gewöhnliches Balkendiagramm nicht,
> und 10b braucht Top‑N plus ‚Rest' oder eine anteilige Darstellung. Liegt `nicht zugeordnet` über
> 30 %, ist der Katalog vor 10b weiter zu pflegen — dann steht Schritt 10 vor demselben Problem,
> wegen dessen 9b vor 9a gezogen wurde."*

**Beide Zweige treffen. Der erste für `NEXANS`, der zweite für `SUTTONS` und `VOTG`.**

**Erstens — der größte Partner hält über 50 %, in beiden Fenstern.** 83,03 % im Tagesfenster,
58,61 % im Monatsfenster. Die drei größten halten 89,15 % bzw. 71,23 %; die zehn größten 90,56 %
bzw. 76,93 %. **Die übrigen 105 Partner teilen sich in Fenster B zusammen 13.226 Nachrichten —
7,34 %.**

> **Ein gewöhnliches Balkendiagramm trägt nicht.** Bei 105.654 Nachrichten für den ersten und 981
> für den zehnten Balken ist das Verhältnis **108 : 1** — Rang 10 wäre bei 400 Pixeln Breite vier
> Pixel lang, Rang 15 zwei. **10b braucht Top‑N plus „Rest" oder eine anteilige Darstellung**, wie
> vorregistriert.
>
> **Offener Punkt 39 ist damit beantwortet, und die Antwort ist die unangenehme:** Die Konzentration
> wird durch die Kuratierung **nicht** besser, sondern bleibt. Nachgemessen (Sitzung 6d, unter L9):
> Der größte **Prozess** hält **1.472.788** Nachrichten = **44,0754 %** des Gesamtbestands und
> **51,0373 %** innerhalb von `NEXANS` — Zeichen für Zeichen die 44,08 % aus Punkt 39, unabhängig
> erneut erhoben. Der größte **Partner** fasst mehrere solcher Prozesse zusammen und kommt in
> Fenster B auf 58,61 %. **Die Aggregation nach Partner verschärft die Konzentration, sie mildert
> sie nicht.**

**Zweitens — `nicht zugeordnet` liegt über 30 %, und zwar bei 100 %.** Für `NEXANS` sind es 8,98 %
(Fenster A) und 15,73 % (Fenster B), also **unter** der Schwelle. Für `SUTTONS` und `VOTG` sind es
**100 %** in beiden Fenstern.

> **Also, wie vorregistriert: Der Katalog ist vor 10b weiter zu pflegen** — aber nicht überall.
> **Für `NEXANS` trägt die Verteilung heute.** Für `SUTTONS`, `IBISGUS`, `NXHBE`, `EDITIONLINGERI`,
> `WOC` und `SYSTEM` gibt es keine einzige Katalogzeile, für `VOTG` 390 offene. **Für sieben von
> zehn Mandanten zeigt die Partnerverteilung heute genau einen Balken: „nicht zugeordnet, 100 %".**
>
> **Und ja — das ist dasselbe Problem, wegen dessen 9b vor 9a gezogen wurde.** Der Unterschied ist,
> dass es diesmal nicht am Werkzeug liegt: Die Pflegeoberfläche steht seit 9b, der Katalog ist für
> den größten Mandanten vollständig. Was fehlt, ist die Pflege der übrigen — eine Arbeit, kein Bau.

## Der größte Prozess je Mandant *(L9, Bezugsgröße zu Punkt 39)*

| Mandant | Prozesse mit Nachrichten | größter Prozess | Nachrichten gesamt | Anteil im Mandanten | Anteil am Bestand |
|---|---:|---:|---:|---:|---:|
| NEXANS | 516 | **1.472.788** | 2.885.711 | **51,04 %** | **44,08 %** |
| SUTTONS | 17 | 26.921 | 197.158 | 13,65 % | 0,81 % |
| VOTG | 40 | 43.661 | 145.840 | 29,94 % | 1,31 % |
| IBIS | 113 | 8.842 | 75.746 | 11,67 % | 0,26 % |
| IBISGUS | 23 | 7.981 | 29.339 | 27,20 % | 0,24 % |
| ZAST | 24 | 1.906 | 5.036 | 37,85 % | 0,06 % |
| **WOC** | **2** | **2.068** | **2.529** | **81,77 %** | 0,06 % |

Laufzeit 22.491,0 ms (voller Durchlauf über `Message` mit der Mandantenkette).

**Drei Mandanten fehlen in dieser Tabelle** — `NXHBE`, `EDITIONLINGERI` und `SYSTEM` tragen im
Gesamtbestand **keine einzige** Nachricht. Zusammen mit E‑f („`SYSTEM` und `WOC` werden behandelt
wie jeder andere Mandant") heißt das: **`SYSTEM` bekommt ein Dashboard, das nie etwas zeigen wird**,
und `WOC` eines, in dem ein einziger Prozess 81,77 % hält. E‑f bleibt davon unberührt — es ist eine
Entscheidung über Gleichbehandlung, nicht über Inhalt —, aber die Zahl gehört daneben.

## Befund 11 — `GROUP BY` band an die Tabellenspalte, nicht an den Ausdrucksalias

**Ein Fehler dieser Runde, gefunden und behoben, und er gehört hierhin, weil er in Anwendungscode
genauso passieren wird.**

Sitzung 6b hat den maskierten Partner als Ausdruck mit dem Alias `partner` berechnet und
`GROUP BY nz, partner` geschrieben. **MariaDB löst `GROUP BY` zuerst gegen Tabellenspalten auf und
erst dann gegen Ausdrucksaliasse** — und `process_catalog` trägt eine Spalte, die ebenfalls
`partner` heißt. Gruppiert wurde deshalb nach `c.partner`, nicht nach dem `CASE`.

**Die Folge war sichtbar und hätte auch unsichtbar bleiben können.** Sichtbar wurde sie bei `VOTG`:
Der Eimer `nicht zugeordnet` zerfiel in **acht** Zeilen, weil die dortigen `OFFEN`-Zeilen
verschiedene `REGEL_A`-Vorschläge tragen, die der `CASE` auf `NULL` hätte abbilden sollen. Bei
`NEXANS` und `SUTTONS` fiel **nichts** auf — dort ist `c.partner` in genau den betroffenen Zeilen
ohnehin `NULL`, und das Ergebnis war zufällig richtig.

Die Anteilszahlen waren **nicht** betroffen (sie zählen über `nz`), die Verteilungstabelle schon.
Nachgemessen in Sitzung 6c mit dem Alias `partner_kuratiert`; `gruppen_nicht_zugeordnet` ist
seither in allen sechs Fällen **1**, wie es sein muss. Dieselbe Falle steckt in `richtung` —
nachgemessen in Sitzung 6d.

> **Für 10b ist das eine Bauvorgabe, keine Anekdote.** Der `CASE`, der E‑i umsetzt, wird in
> `message_rollup`-Leseabfragen stehen, und die Spalten heißen dort `partner` und `richtung`. Wer
> ihn mit dem naheliegenden Alias schreibt, bekommt **stillschweigend** eine Gruppierung nach dem
> Rohwert — und sieht es nur bei einem Mandanten, dessen Daten es verraten. **Der Alias muss anders
> heißen als die Spalte**, oder es ist `GROUP BY` über den vollen Ausdruck zu schreiben.

---

# M92 — Was kostet der Rückwärtslauf?

Sitzungen 7a und 7b — `s7a-m92-scheibengrenzen.sql`, `s7b-m92-rueckwaertslauf.sql`.

## Die erste Zahl, die keine Laufzeit ist: die Scheibengrenzen über den ganzen Bestand

Der Auftrag verlangt, dass **die fünf leeren Monate als leere Scheiben auftauchen** — „ein
Rückwärtslauf, der über sie stolpert oder sie überspringt, wäre falsch gebaut."

**Eine bloße Gruppierung kann leere Monate nicht zeigen**, denn sie haben keine Zeile, über die
gruppiert würde. Der Kalender steht deshalb als `UNION ALL` aus **22 Literalpaaren** und wird per
`LEFT JOIN` gegen den Bestand gehalten. **Genau so muss der Rückwärtslauf gebaut sein: Er iteriert
über einen Kalender, nicht über die vorhandenen Daten.** Das ist die Bauvorgabe, die aus dieser
Zahl folgt.

| Monat | Zeilen | früheste | späteste | Prozesse | Rollupzeilen | |
|---|---:|---|---|---:|---:|---|
| 2024-10 | 241.203 | `2024-10-01 02:00:28` | `2024-10-31 23:57:10` | 618 | 19.886 | |
| 2024-11 | 220.103 | `2024-11-01 00:02:10` | `2024-11-30 23:47:07` | 635 | 18.766 | |
| 2024-12 | 169.237 | `2024-12-01 00:02:07` | `2024-12-31 23:47:04` | 622 | 16.757 | |
| 2025-01 | 196.425 | `2025-01-01 00:02:04` | `2025-01-31 23:59:48` | 634 | 19.867 | |
| 2025-02 | 191.937 | `2025-02-01 00:00:05` | `2025-02-28 23:59:26` | 642 | 18.605 | |
| 2025-03 | 220.115 | `2025-03-01 00:00:18` | `2025-03-31 23:59:22` | 651 | 19.929 | |
| 2025-04 | 226.421 | `2025-04-01 00:00:16` | `2025-04-30 23:58:06` | 644 | 24.753 | |
| 2025-05 | 237.918 | `2025-05-01 00:00:08` | `2025-05-31 23:53:12` | 657 | 25.076 | |
| 2025-06 | 229.904 | `2025-06-01 00:03:05` | `2025-06-30 23:59:39` | 642 | 24.460 | |
| **2025-07** | **248.320** | `2025-07-01 00:00:17` | `2025-07-31 23:59:07` | 654 | **26.365** | **größte** |
| 2025-08 | 229.618 | `2025-08-01 00:01:09` | `2025-08-31 23:57:00` | 646 | 24.610 | |
| 2025-09 | 243.537 | `2025-09-01 00:02:03` | `2025-09-30 23:58:16` | 655 | 25.607 | |
| 2025-10 | 234.598 | `2025-10-01 00:00:16` | `2025-10-31 23:59:38` | 660 | 26.048 | |
| 2025-11 | 237.642 | `2025-11-01 00:01:31` | `2025-11-30 23:57:59` | 657 | 23.895 | |
| 2025-12 | 209.408 | `2025-12-01 00:03:02` | `2025-12-30 04:09:47` | 643 | 20.971 | |
| **2026-01** | **0** | — | — | 0 | 0 | **LEER** |
| **2026-02** | **0** | — | — | 0 | 0 | **LEER** |
| **2026-03** | **0** | — | — | 0 | 0 | **LEER** |
| **2026-04** | **0** | — | — | 0 | 0 | **LEER** |
| **2026-05** | **0** | — | — | 0 | 0 | **LEER** |
| 2026-06 | 4.848 | `2026-06-09 17:56:23` | `2026-06-18 14:08:43` | **3** | 13 | |
| 2026-07 | 285 | `2026-07-08 17:16:26` | `2026-07-08 17:21:10` | **2** | 2 | |

**Kontrolle:** 3.341.519 Zeilen, **17 belegte Monate**. 17 + 5 = 22. Die Summe der Scheiben ist der
Bestand, und die fünf leeren Monate stehen ausdrücklich als `LEER` darin.

> **Zwei Dinge, die diese Tabelle über die fünf leeren Monate hinaus zeigt.**
>
> 1. **Der letzte Monat mit vollem Betrieb endet am `2025-12-30 04:09:47`** — das ist auf die
>    Sekunde der Anker aus M9, mit dem `message-status.md` gerechnet hat. Danach kommt nichts mehr
>    außer 4.848 Zeilen im Juni 2026 und 285 im Juli, auf zwei bis drei Prozessen. **Der Bestand
>    hat kein dünnes Ende, sondern einen Abbruch.**
> 2. **Die Rollupzeilen je Monat schwanken zwischen 16.757 und 26.365** — bei Zeilenzahlen zwischen
>    169.237 und 248.320. Das Verhältnis liegt zwischen 9,4 und 12,1 Nachrichten je Rollupzeile und
>    ist über 15 Monate erstaunlich stabil. **Der Verdichtungsfaktor 9,96 aus Befund 7 ist kein
>    Mittelwert über heterogene Monate, sondern gilt in jedem einzelnen.**

## Die zweite Zahl, die keine Laufzeit ist: die größte Monatsscheibe

| Monat | Zeilen |
|---|---:|
| **2025-07** | **248.320** |
| 2025-09 | 243.537 |
| 2024-10 | 241.203 |
| 2025-05 | 237.918 |
| 2025-11 | 237.642 |

**Sie bestimmt die Obergrenze, nicht der Durchschnitt** — und sie liegt nur **11,6 %** über dem
Mittel der 15 vollen Monate (**222.426**). Der Bestand ist über die Monate gleichmäßig verteilt; es
gibt keine Scheibe, die aus der Reihe fällt, und deshalb ist die Obergrenze hier nur wenig über dem
Durchschnitt.

> **Eine Kontrolle, die nebenbei aufgeht.** Die 15 vollen Monate summieren sich auf **3.336.386**
> Zeilen — genau die Zahl, die `PROJEKTBESCHREIBUNG.md` §8 Z. 961 für „01.10.2024 bis 30.12.2025"
> nennt. Die restlichen 5.133 Zeilen des Bestands liegen sämtlich im Juni und Juli 2026.

> **Belegvermerk** (Regel L10) — **eine Angabe des Auftrags, die der Bestand nicht trägt.**
> *Gemessen war:* **Oktober 2024 trägt 241.203 Zeilen und ist die drittgrößte der 22 Scheiben** —
> 8,4 % **über** dem Mittel der vollen Monate.
> *Behauptet wird* im Auftrag (§M92): „plus eine Scheibe aus dem **dünnen Anfang des Bestands**
> (Oktober 2024)".
> **Die Lücke:** Der Bestand hat keinen dünnen Anfang. Er beginnt am `2024-10-01 02:00:28` sofort
> mit vollem Betrieb — die kleinste volle Scheibe ist Dezember 2024 mit 169.237 Zeilen, und auch
> die ist nicht dünn. Dünn ist ausschließlich das **Ende** (2026-06 und 2026-07). Die Scheibe ist
> trotzdem wie verlangt gefahren; sie misst nur etwas anderes, als der Auftrag annimmt — nämlich
> eine zweite große Scheibe.

## Die Laufzeit — vier Scheiben, je ein Aufwärmlauf und fünf Läufe

Gemessen wird die Aggregation aus M88, unverändert. **Kein `STRAIGHT_JOIN`, in keiner Fassung.**

| Scheibe | Zeilen | Rollupzeilen | Aufwärmlauf | **beste von fünf** | schlechteste | µs je Zeile |
|---|---:|---:|---:|---:|---:|---:|
| **B** `2025-11-30`–`2025-12-30` | 214.330 | 21.256 | 2,513 s | **2,450 s** | 2,494 s | 11,4 |
| **OKT** 2024-10 | 241.203 | 19.886 | 2,906 s | **2,656 s** | 2,896 s | 11,0 |
| **MAX** 2025-07 (größte) | 248.320 | 26.365 | 2,816 s | **2,745 s** | 3,065 s | 11,1 |
| **LEER** 2026-01 | **0** | 0 | 0,001 s | **0,001 s** | 0,001 s | — |

**11,0 bis 11,4 µs je Zeile** — deckungsgleich mit den 10,2 bis 11,3 µs aus M88, über einen
Mengenbereich, der dort bei 12.332 Zeilen endete und hier bei 248.320 beginnt. **Die Aggregation ist
über drei Größenordnungen linear.**

**Eine leere Scheibe kostet 1 Millisekunde.** Ein Rückwärtslauf, der über den Kalender iteriert,
zahlt für die fünf leeren Monate zusammen **5 ms**. Sie zu überspringen wäre eine Optimierung ohne
Gegenwert — und genau die Optimierung, die den Lauf falsch machen würde.

## Der Plan — und eine Ergänzung zu M88

```
+------+-------------+---------+-------+---------------------------------------------------------+------------------------------------+---------+------+--------+--------------------------------------------------------+
| id   | select_type | table   | type  | possible_keys                                           | key                                | key_len | ref  | rows   | Extra                                                  |
+------+-------------+---------+-------+---------------------------------------------------------+------------------------------------+---------+------+--------+--------------------------------------------------------+
|    1 | SIMPLE      | Message | range | MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateProcessMessageIDX | 5       | NULL | 518174 | Using index condition; Using temporary; Using filesort |
+------+-------------+---------+-------+---------------------------------------------------------+------------------------------------+---------+------+--------+--------------------------------------------------------+
```

| Scheibe | gewählter Index | geschätzte `rows` |
|---|---|---:|
| **LEER** 2026-01 | `MessageLastUpdateIDX` | 1 |
| **B** (30 Tage) | `MessageLastUpdateIDX` | 409.756 |
| **OKT** (31 Tage) | **`MessageLastUpdateProcessMessageIDX`** | 541.514 |
| **MAX** (31 Tage) | **`MessageLastUpdateProcessMessageIDX`** | 518.174 |

> **Das ergänzt M88 und schränkt dessen Antwort ein.** M88 hat über sechs Fenster von 256 bis
> 12.332 Zeilen gemessen und **immer** `MessageLastUpdateIDX` gefunden. Über einer geschätzten
> Bereichsgröße von rund einer halben Million Sätzen **kippt der Optimierer auf den
> zusammengesetzten Index**.
>
> **Der Satz aus M88 bleibt richtig, aber er gilt nur dort, wo er gemessen ist:** Für den
> **stündlichen** Delta-Lauf — die Abfrage, um die es in M88 geht — greift `MessageLastUpdateIDX`,
> und der zusammengesetzte Index bringt nichts. Für den **Rückwärtslauf** über Monatsscheiben
> greift der zusammengesetzte. **Beide Aussagen zusammen sind die vollständige Antwort auf die
> Frage von M88.**
>
> **Auf die Laufzeit wirkt sich der Wechsel nicht sichtbar aus:** B mit dem schmalen Index kostet
> 11,4 µs je Zeile, MAX mit dem breiten 11,1 µs. Der Optimierer wählt anders, aber nicht besser —
> und auch nicht schlechter.

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„Unter 30 s je Scheibe: Der Rückwärtslauf über 22 Monate ist eine Sache von etwa
> zehn Minuten, braucht keine Wiederaufnahme und keinen Fortschrittsvermerk. Über 5 Minuten je
> Scheibe: Er muss wiederaufnehmbar sein, mit einem Vermerk je abgeschlossener Scheibe — und dann
> gehört diese Bauform als Vorgabe nach 10a."*

**Der obere Zweig trifft, und er trifft um mehr als eine Größenordnung.** Die **größte** Scheibe
kostet **2,745 s**, ein Elftel der 30‑Sekunden‑Schwelle.

> **Also, wie vorregistriert, so gesagt: Der Rückwärtslauf braucht keine Wiederaufnahme und keinen
> Fortschrittsvermerk.** Und er ist keine Sache von zehn Minuten, sondern von **einer**.
>
> **Die Hochrechnung, als Hochrechnung gekennzeichnet:** 22 Scheiben zu höchstens 2,745 s ergeben
> **60,4 s**; über die tatsächlichen Zeilenzahlen gerechnet (3.341.519 × 11,1 µs) sind es **37,1 s**
> reine Aggregation.
>
> **Gemessen ist der ganze Lauf aber auch schon — in Sitzung 5a.** Dort ist die Probetabelle über
> **exakt diese 22 Monatsscheiben** gefüllt worden, `INSERT … SELECT`, Schreiben inbegriffen:
> **51,242 s insgesamt, teuerste Scheibe 3,827 s.** Das ist keine Hochrechnung, sondern die
> gemessene Dauer eines vollständigen Rückwärtslaufs über den Bestand der Testkopie.
>
> **Der Unterschied zwischen 37,1 s Aggregation und 51,2 s mit Schreiben ist der Preis des
> Schreibens: rund 14 s für 335.610 Zeilen**, also 42 µs je geschriebener Rollupzeile.

## Nicht hochgerechnet auf die Produktion

Der Auftrag verlangt es ausdrücklich, und hier steht es:

> **Was diese Messung liefert, ist die Zahl für die Testkopie.** Der Rückstand und der Umfang in
> Produktion sind ungeklärt (V6): `PROJEKTBESCHREIBUNG.md` §8 nennt 22 Monate, `rohdaten.md` §12
> nennt produktiv rund 18 und danach ein Archivsystem, und keine der beiden Zahlen ist beim
> Betreiber geholt. **Die 51,242 s gelten für 3.341.519 Zeilen auf dieser Kopie und für nichts
> sonst.**
>
> Was sich **ohne** Hochrechnung sagen lässt: Der Lauf ist in der Zeilenzahl linear (11 µs je
> Zeile, über drei Größenordnungen bestätigt) und in Monatsscheiben zerlegbar, die jede für sich
> unter drei Sekunden bleiben. **Wie viele Scheiben es in Produktion sind, ist die einzige
> Unbekannte** — und sie wirkt linear, nicht überproportional.

---

# Sitzung 8 — die Probetabelle ist gelöscht, und hier ist der Nachweis

`s8-abschluss.sql`, Benutzer `monitor_write`, Serverzeit `2026-08-26 10:37:43` bis `10:37:44`.

| Schritt | Ergebnis |
|---|---|
| `@@global.read_only` zu Beginn | **`1`** |
| **vorher** — `SELECT COUNT(*) FROM overlord_monitor.message_rollup_probe` | **335.610** |
| **vorher** — `DATA_LENGTH` / `INDEX_LENGTH` | 22.642.688 / 0 |
| `DROP TABLE overlord_monitor.message_rollup_probe` | ausgeführt |
| **nachher** — Zeilen in `information_schema.TABLES` für diese Tabelle | **`0`** |
| **nachher** — Tabellen in `overlord_monitor` | `app_user`, `app_user_mandant`, `audit_log`, `bam_sollaenge`, `bam_spalte`, `flyway_schema_history`, `process_catalog`, `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES` |
| `@@global.read_only` am Ende | **`1`** |

**Das sind genau die neun Tabellen aus Sitzung 5a‑2, vor der Anlage.** Die Probetabelle hat die
Runde nicht überlebt.

**`process_catalog` ist unverändert** — 1.160 Zeilen, 770 `GEPFLEGT`, 390 `OFFEN`, 932 mit Partner,
759 mit Richtung. Zeichen für Zeichen der Stand aus V2. Diese Runde hat den Katalog nicht angefasst.

**`GlassfishDB` ist unberührt** — 3.341.519 Zeilen, Datenstand `2026-07-08 17:21:10`. Und die vier
Tabellengrößen sind am Ende der Runde **byteidentisch** mit V1:

| Tabelle | `DATA_LENGTH` | `INDEX_LENGTH` | gegen V1 |
|---|---:|---:|---|
| `Message` | 740.851.712 | 2.157.330.432 | **identisch** |
| `MessageAction` | 2.226.634.752 | 819.855.360 | **identisch** |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 | **identisch** |
| `MessageProperty` | 15.088.615.424 | 45.945.946.112 | **identisch** |

---

# Laufzeiten im Überblick

## Das teuerste Statement der Runde

> **M87‑5, die Gegenprobe über alle vier Varianten in einem Durchlauf: 60,219 s — abgebrochen.**
> Sie hat `max_statement_time = 60` gerissen und ist als einzige Messung der Runde nicht
> durchgelaufen. Die Grenze ist **nicht** hochgesetzt worden; die Messung ist in Jahresscheiben
> nachgeholt (Sitzung 3b) und trifft dort die Einzelmessungen auf die Zeile.

**Das teuerste durchgelaufene Statement** ist die Jahresscheibe 2025 aus derselben Gegenprobe:
**40,896 s** für 2.705.843 Zeilen mit vier gleichzeitigen `COUNT(DISTINCT …)`.

## Die achtzehn teuersten Statements

| Laufzeit | Sitzung | Messung |
|---:|---|---|
| **60,219 s** | 3 | M87‑5 Gegenprobe, alle vier Varianten — **abgebrochen** |
| 40,896 s | 3b | M87‑5b Jahresscheibe 2025 |
| 22,491 s | 6d | M91‑R5 größter Prozess je Mandant (L9) |
| 21,087 s | 7a | M92‑1 Scheibengrenzen über 22 Monate |
| 20,557 s | 2 | M86b Perzentile der Differenz, Fenster B |
| 17,408 s | 2 | M86b Differenzverteilung, Fenster B |
| 17,182 s | 2 | M86b Anteile je Status, Fenster B |
| 15,302 s | 3 | M87 Variante 2 (Stunde, Prozess, Einordnung) |
| 14,062 s | 3 | M87 Variante 1 (Stunde, Prozess, Rohstatus) |
| 10,670 s | 3b | M87‑5a Jahresscheibe 2024 |
| 10,359 s | 3 | M87 Variante 3 (Tag) |
| 10,110 s | 6e | 6e‑3 Richtungslücke über Fenster G (L9) |
| 8,460 s | 3 | M87‑6 Einordnung je Rohstatus |
| 8,443 s | 4a | dichteste Stunde des Bestands (L9) |
| 7,715 s | 3 | M87 Variante 4 (Monat) |
| 5,330 s | 7a | M92‑2 größte Monatsscheibe |
| 3,939 s | 3 | M87‑0 Bezugsgrößen |
| 3,827 s | 5a | M89 Füllen, teuerste Monatsscheibe (`INSERT … SELECT`) |

## Die Zahlen, auf die es für 10a und 10b ankommt

| Was | Wert |
|---|---:|
| **Stündlicher Delta-Lauf, dichteste Stunde des Bestands** (8.630 Zeilen) | **88,167 ms** |
| Stündlicher Delta-Lauf, letzte Stunde des Bestands (285 Zeilen) | 3,190 ms |
| **Dashboard-Leseabfrage, 48 h, mit Katalog-Join** (`NEXANS`) | **11,299 ms** |
| Dashboard-Leseabfrage, 48 h, ohne Katalog-Join (`NEXANS`) | 6,793 ms |
| Dashboard-Leseabfrage, ein Monat, mit Katalog-Join (`NEXANS`) | 237,673 ms |
| **Überfällig-Kachel, Fenster D** (`NEXANS`) | **2,275 ms** |
| Überfällig, „insgesamt" (Fenster G, `NEXANS`) | 4,275 ms |
| **Rückwärtslauf, größte Monatsscheibe** (248.320 Zeilen) | **2,745 s** |
| **Rückwärtslauf über alle 22 Monate, mit Schreiben** (gemessen, Sitzung 5a) | **51,242 s** |
| Aggregationskosten, über drei Größenordnungen stabil | **10,2 – 11,4 µs je Zeile** |
| Zeilen in `message_rollup` (Variante 1, Gesamtbestand) | **335.610** |
| Größe der Rollup-Tabelle | **18,59 MiB** |

---

# Was diese Runde nicht zeigt

1. **Sie kann eine Wanderung von `MessageLastUpdate` nicht ausschließen** (M86). Sie kann sie
   belegen, wenn sie sie zeigt — und sie zeigt Nachschriften von höchstens vier Sekunden. Der
   zulässige Satz lautet „auf diesem Bestand nicht beobachtet", nicht „findet nicht statt".
   `RUNNING` kommt null Mal vor, `MatchInterchange` läuft nicht sichtbar (M31‑3, Takt **ungedeckt**),
   und der Bestand endet am 08.07.2026.
2. **Sie zeigt nichts über die Kategorie „Überfällig" in der Sache** (M90). Gemessen sind Plan und
   Laufzeit. Alle 538 offenen Zeilen gehören einem Mandanten und enden ein halbes Jahr vor dem
   Anker; Fenster D enthält null davon. Die Kategorie ist gegen diese Testkopie nicht prüfbar, und
   das steht seit Schritt 4 so in `docs/message-status.md`.
3. **Sie zeigt den Kaltlauf nicht.** `FLUSH TABLES` steht `monitor_read` nicht zu. Der Puffer fasst
   `Message` 9,26‑mal; der Aufwärmlauf misst den kalten Abfrageplan und die kalte Verbindung, nicht
   die kalte Platte. Wo ein Kaltlauffaktor genannt ist (M88), ist er aus M44 **übertragen** und
   nicht gemessen.
4. **Sie zeigt nichts über die Produktion** (V6). Weder die Aufbewahrungsdauer noch der Umfang noch
   der Rückstand. M92 rechnet mit der Testkopie und rechnet nicht hoch.
5. **Sie misst „Unquittiert" nicht** (E‑d). Die Kategorie ist am 24.08.2026 aus dem MVP genommen
   worden und hat in keiner Datei eine operative Definition. `PROJEKTBESCHREIBUNG.md` §4.2 Z. 456–458
   gibt drei Zeilen ohne Code-Verweis und ohne benannte Methode; §9 nennt den Begriff **gar nicht**.
   Eine Messung ohne Definition wäre geraten.
6. **Sie klärt den Takt von `MatchInterchange` nicht.** Ungedeckt seit M31‑3, die Quelle liegt beim
   Altsystem.
7. **Sie fasst `MessageProperty` nicht an** (L4). Einziges Vorkommen ist die Tabellengröße aus
   `information_schema` in V1.
8. **Sie misst die Hochaggregation zur Lesezeit nicht.** M87 liefert die Zeilenverhältnisse
   (Faktor 2,73 zur Tages-, 28,07 zur Monatsebene); was eine Tages- oder Monatsansicht **kostet**,
   wenn sie aus der Stundentabelle gerechnet wird, ist nicht gemessen. M89 misst die Stundenebene.
9. **Sie misst `GET /api/nachrichten` mit dem neuen Parameter `ueberfaellig` nicht** (E‑e/E‑j).
   M90 misst die Kachelzahl, nicht den Listen-Endpunkt mit Cursor und Sortierung.
10. **Sie zeigt das Dashboard nicht im Standardfenster.** Fenster D trägt 256 Zeilen auf zwei
    Prozessen, und für `SUTTONS` null. Jede Sichtprüfung von 10b wird ein anderes Fenster brauchen —
    das ist ein Befund über den Bestand, nicht über die Entscheidung.

---

# Abweichungen vom Rahmen, einzeln benannt

| # | Abweichung | Begründung |
|---|---|---|
| **A1** | **Client `mysql.exe` 8.0.46 mit `--ssl-mode=DISABLED` statt `mariadb` mit `--skip-ssl`** | `--skip-ssl` ist die MariaDB-Schreibweise; der Workbench-Client bricht damit mit `unknown option` ab, **vor** der Sitzung. Ein MariaDB-Client 12.3.2 liegt auf dem Rechner, ist aber **nicht** verwendet worden: Ein Clientwechsel kostete die Vergleichbarkeit mit allen Runden seit M32 — und V1 dieser Runde beruht genau darauf. Dieselbe Abweichung wie in M80 und M83, mit derselben Begründung |
| **A2** | **`max_statement_time = 60` einmal gerissen** (M87‑5, 60,219 s) | Nicht hochgesetzt. Wie im Rahmen vorgeschrieben in Jahresscheiben nachgefahren (Sitzung 3b); die Summen treffen die Einzelmessungen auf die Zeile. Das Summieren ist exakt und keine Näherung, weil die Zeit das erste Glied jedes Schlüssels ist |
| **A3** | **Fluchtzeichen `!` statt `\` im `CASE` der M87 Variante 2** | `LIKE 'ERROR\_%' ESCAPE '\'` ist als Literal in einer Skriptdatei nicht lauffähig (`'\''` entwertet das schließende Hochkomma) und der Client bricht zusätzlich mit `Unknown command '\_'` ab. `'ERROR!_%' ESCAPE '!'` ist zeichengleich in der Wirkung; **belegt** durch M87‑6, wo `FEHLER` genau `ERROR_TIMEOUT`, `ERROR_DUPLICATE` und `COMMIT_REJECTED` umfasst. Der Ausdruck im Code ist nicht berührt — jOOQ setzt das Fluchtzeichen selbst |
| **A4** | **M88 mit sechs statt drei Fenstern** | Fenster D trägt 256 Zeilen auf zwei Prozessen; eine Laufzeit dagegen misst einen leeren Indexbereich. Die drei verlangten Fenster sind alle gefahren, drei dichte kommen dazu |
| **A5** | **M89 mit vier statt einem Abfragezuschnitt** | Die Abfrage, die der Auftrag wörtlich beschreibt, lässt den `LEFT JOIN` auf `process_catalog` wegoptimieren (Befund 9). Variante B holt ihn zurück; Fenster D2 und B kommen dazu, weil Fenster D fast leer ist |
| **A6** | **M91 zweimal gefahren** (Sitzungen 6b, dann 6c/6d korrigiert) | `GROUP BY` band an die Tabellenspalte statt an den Ausdrucksalias (Befund 11). Die Anteilszahlen aus 6b waren nicht betroffen, die Verteilungstabelle schon. Beide Läufe stehen in `scripts/`, die Ergebnisdatei führt nur die korrigierten Zahlen |
| **A7** | **M86 (b) über alle elf Statuswerte statt über die vier verlangten** | Der Auftrag nennt „mindestens `COMMIT_RECEIVED`, `FINISHED`, `MERGED`, `EERP_RECEIVED`". Alle elf zu nehmen kostet nichts und hat Befund 6 sichtbar gemacht, der in den vier nicht steckt |
| **A8** | **Kein `EXPLAIN` für Fenster H2 in M88** | Fünf der sechs Fenster sind geplant; die fünf decken den Mengenbereich von 256 bis 24.218 geschätzten Sätzen ab, und der Plan ist über diesen ganzen Bereich unverändert. Für H2 fehlt der Plan im Volltext — **L15 ist an dieser einen Stelle nicht vollständig erfüllt** |
| **A9** | **M91 ohne Laufzeitmessung nach „beste von fünf"** | M91 ist eine Verteilungsfrage, keine Leistungsfrage; der Auftrag verlangt für sie keine Laufzeit. Die Einzellaufzeiten stehen trotzdem im Profil und in der Laufzeittabelle |
| **A10** | **Die Runde schreibt in mehr als eine Datei** | Der Auftrag sagt „genau eine Datei: `docs/messungen-schritt10.md`". Dazu gekommen sind `scripts/messung-schritt10/*.sql` (dreizehn Sitzungsdateien) und eine Zeile in `.gitignore`. Das folgt der Konvention seit Schritt 8: Die Sitzungsdateien sind der Beleg dafür, **wie** gemessen wurde, und ohne sie ist die Runde nicht nachfahrbar; die Rohausgaben bleiben ausgeschlossen. **Keine der unter „Gesperrte Dateien" genannten Dateien ist angefasst worden** |
| **A12** | **Kein Eintrag in `docs/README.md`, obwohl diese Datei neu ist** | `CLAUDE.md` verlangt unter „Dokumentationspflicht": „Neue Datei → Eintrag in `docs/README.md`". Der Auftrag dieser Runde führt `docs/README.md` unter **„Gesperrte Dateien — nicht anzufassen"**. Beides zusammen geht nicht. Aufgelöst zugunsten des Auftrags, weil er die spätere und die speziellere Vorgabe ist — **aber nicht stillschweigend**: Der Eintrag fehlt und gehört in die Korrekturrunde. Er ist unten unter den Korrekturen mitgeführt |
| **A11** | **Sechs Zahlen sind vor dem Abschluss der Runde berichtigt worden** | Alle **abgeleiteten** Größen dieser Datei sind am Ende gegen die Rohausgaben nachgerechnet worden. Sechs stimmten nicht und stehen jetzt richtig: (1) die Laufzeit von M86b Fenster B — beim ersten Auswerten waren die Werte von Fenster A zugeordnet worden, **517,5 ms statt 17.182,4 ms**; (2) das Mittel der 15 vollen Monatsscheiben, **222.426 statt 219.359**, und damit (3) der Abstand der größten Scheibe, **11,6 % statt 7,6 %**, und (4) der von Oktober 2024, **8,4 % statt 10,0 %**; (5) der kleinste Aufwärmlauf-Aufschlag in M88, **0,5 % (A) statt 1,4 % (D)**; (6) der Anteil der Partner ab Rang 11 in M91, **7,34 % auf 105 Partner statt 23,07 % auf 115**. **Keine gemessene Zahl war betroffen und keine vorregistrierte Deutung hing an einer von ihnen** — alle sechs sind aus richtigen Messwerten falsch weitergerechnet. Sie stehen hier, weil eine Runde, die ihre eigenen Rechenfehler verschweigt, ihre übrigen Zahlen mit entwertet |

---

# Befunde, die in keine vorformulierte Zeile passten

Fortlaufend an `messungen-schritt9.md` anschließend, wo der gleichnamige Abschnitt (Z. 1898–1909)
fünf führt. **Sechs bis vierzehn.** Jeder steht bei seiner Messung; hier sind sie gezählt und benannt.

| # | Messung | Der Befund |
|---|---|---|
| **6** | M86 (b) | **1.803 `EERP_RECEIVED`- und 110 `COMMIT_RECEIVED`-Zeilen tragen ein `MessageLastUpdate`, das *vor* dem Ende ihres letzten Schrittes liegt** — bis zu 58 Sekunden davor, bei 26,14 % aller `EERP_RECEIVED`-Zeilen des Fensters. Für sie ist `MessageLastUpdate` **nicht** der Zeitpunkt der letzten Änderung |
| **7** | M87 | **Der Rollup verdichtet nur um Faktor 9,96**, nicht um Größenordnungen — 335.610 Zeilen zu 3.341.519 Nachrichten, und das Verhältnis ist in jedem einzelnen Monat 9,4 bis 12,1. Dazu: **das Jahr 2026 trägt 5.133 Zeilen auf drei Prozessen** |
| **8** | M88 | **Die Zeilenschätzung ist bei großen Bereichen um Faktor 2 zu hoch, bei kleinen auf die Zeile genau.** Ursache ist `CARDINALITY = 1.780.243` für `MessageLastUpdateIDX` gegen 3.341.519 gezählte Zeilen — **genau die Hälfte** |
| **9** | M89 | **Der `LEFT JOIN` auf `process_catalog` wird vollständig wegoptimiert**, wenn keine seiner Spalten verwendet wird. Die Abfrage, die der Auftrag beschreibt, misst den Katalog-Join nicht, sondern seine Abwesenheit |
| **10** | M91 | **Elf Katalogzeilen ohne Richtung tragen alle denselben Partner und 54,44 % des gesamten `NEXANS`-Bestands.** Von 733 gepflegten Zeilen fehlen elf — es sind die elf, auf die es ankommt |
| **11** | M91 | **`GROUP BY` band an die Tabellenspalte `process_catalog.partner` statt an den gleichnamigen Ausdrucksalias.** Bei zwei von drei Mandanten war das Ergebnis zufällig trotzdem richtig |
| **12** | M88 (Vorprobe) | **Fenster D — das Standardfenster des Dashboards — trägt 256 Zeilen auf zwei Prozessen, alle innerhalb von vier Minuten.** 47 der 48 Stunden sind leer, und für `SUTTONS` ist das Fenster ganz leer |
| **13** | M89, M90 | **Die Einstiegstabelle des Plans hängt am Mandanten, nicht an der Abfrage.** `NEXANS` steigt über die Rollup- bzw. `Message`-Tabelle ein, `SUTTONS` über `ProjectMandant` — in allen elf gemessenen Plänen. M80 hatte `ProjectMandant` für **beide** gefunden |
| **14** | M90 | **Fenster G ist billiger als Fenster B** — 4,275 ms ohne jedes Zeitfenster gegen 5,127 ms mit einem Monatsfenster. Das Zeitfenster verengt nichts, weil `MessageStatusIDX` bereits auf 539 von 3,34 Millionen Zeilen herunterführt; es kostet nur. **Die zweite Kachelzahl ist billiger als die erste** |

---

# Offene Punkte

Nummerierung im Anschluss an den projektweit höchsten Stand (**40**, in
[`messungen-schritt9.md`](messungen-schritt9.md) Z. 870). Gegengeprüft:
`grep -rnE '^(4[0-9]|5[0-9])\. \*\*' docs/*.md` findet außer dieser Zeile nichts.

41. **Offener Punkt 33 trägt keinen Erledigt-Vermerk.** `PROJEKTBESCHREIBUNG.md` §3.2 und
    `datenmodell.md` führen seit dem 20.08.2026 alle acht Indizes; V3 dieser Runde bestätigt die
    Liste gegen `information_schema`. Der Punkt an seiner Fundstelle
    ([`messungen-schritt9.md`](messungen-schritt9.md) Z. 827) sieht weiterhin offen aus. **Der
    Auftrag dieser Runde ist auf die alte Fassung hereingefallen** und beschreibt V3 als Prüfung
    eines Widerspruchs, der nicht mehr besteht.
42. **`PROJEKTBESCHREIBUNG.md` §3.2 Z. 135 („`MessageLastUpdate` ist der Zeitpunkt der letzten
    Änderung") hat einen Gegenbeleg.** Befund 6: Bei 26,14 % der `EERP_RECEIVED`-Zeilen in Fenster B
    endet ein Schritt **nach** diesem Zeitpunkt. Der Satz stimmt als Näherung — die Abweichung liegt
    unter einer Minute —, aber er stimmt nicht wörtlich.
43. **Die Überfällig-Kachel ist gegen die Testkopie nicht sichtprüfbar.** Nach E‑h zeigt sie im
    Standardfenster **0**, während „insgesamt" 538 stünde. Wer 10b abnimmt, braucht dafür einen
    anderen Bestand oder ein anderes Fenster.
44. **Sieben von zehn Mandanten haben keine kuratierten Katalogzeilen.** `SUTTONS`, `IBISGUS`,
    `NXHBE`, `EDITIONLINGERI`, `WOC` und `SYSTEM` haben **keine einzige** Katalogzeile, `VOTG` hat
    390 offene. Für sie zeigt die Partnerverteilung genau einen Balken. **Das ist Pflegearbeit vor
    10b, kein Bau** — die Oberfläche dafür steht seit 9b.
45. **Elf `NEXANS`-Katalogzeilen ohne Richtung sind die billigste Katalogpflege im Projekt.** Sie
    tragen 54,44 % des Bestands des größten Mandanten. Elf Zeilen heben die Richtungsabdeckung von
    **41,4 % auf 100 %** in Fenster B und von **45,6 % auf 100 %** über den Gesamtbestand.
46. **Der Alias-Fallstrick aus Befund 11 gehört als Bauvorgabe nach 10b.** Der `CASE`, der E‑i
    umsetzt, wird in Leseabfragen über `process_catalog` stehen, und die Spalten heißen dort
    `partner` und `richtung`. Der Alias muss anders heißen als die Spalte, oder `GROUP BY` schreibt
    den vollen Ausdruck aus.
47. **`SYSTEM` bekommt nach E‑f ein Dashboard, das nie etwas zeigen wird.** `SYSTEM`, `NXHBE` und
    `EDITIONLINGERI` tragen im Gesamtbestand keine einzige Nachricht; `WOC` hat zwei Prozesse, von
    denen einer 81,77 % hält. E‑f bleibt davon unberührt — es ist eine Entscheidung über
    Gleichbehandlung, nicht über Inhalt —, aber die Zahl gehört daneben.
48. **`Project` ist aus der Mandantenkette entbehrlich.** V4: `Project` trägt außer dem Schlüssel
    nur Name und Beschreibung; `Process → ProjectMandant` über `Process.ProjectID` liefert dieselbe
    Menge. Die Verkürzung ist gemessen worden **nicht** — die Kette läuft in dieser Runde
    vollständig, wie der Auftrag sie vorschreibt — und sie ist im Plan ohnehin ein `eq_ref` auf
    `PRIMARY`. Sie ist notiert, nicht empfohlen.

---

# Korrekturen, die aus dieser Runde folgen — notiert, nicht ausgeführt

**Die Korrekturen sind eine eigene Runde.** Der Auftrag sagt das ausdrücklich, und diese Runde hält
sich daran: Keine der unter „Gesperrte Dateien" genannten Dateien ist angefasst worden.

Die vier bereits im Auftrag benannten Stellen, mit dem Stand aus dieser Runde daneben:

| Stelle | Was nachzuziehen ist | Was diese Runde dazu sagt |
|---|---|---|
| **E‑a gegen `PROJEKTBESCHREIBUNG.md` §5 und den Implementierungsplan** | §5 Z. 506 beschreibt `message_rollup` als „stündliche Aggregate je **Mandant, Prozess, Partner, Richtung, Status**". E‑a sagt: Schlüssel ist `(Stunde, ProcessID, Status)`, **Mandant, Partner und Richtung stehen nicht in der Zeile** | **M89 stützt E‑a.** 11,30 ms im Standardfenster mit Katalog-Join, 2,3 % des Budgets; keine der zehn gemessenen Fassungen kommt über 400 ms. Der Widerspruch ist zugunsten von E‑a aufzulösen |
| **E‑d gegen §4.2 und §9** | „Unquittiert" ist aus dem MVP genommen; §4.2 Z. 456–458 führt es weiter als dritte Problemkategorie | Diese Runde misst es nicht (siehe „Was diese Runde nicht zeigt", Punkt 5). **Ergänzend erhoben:** §9 nennt den Begriff **gar nicht** — nur Z. 1095 verweist pauschal auf „die drei Problemkategorien". Die Korrektur an §9 ist damit eine andere als angenommen |
| **V6 gegen §8** | §8 Z. 955 nennt 22 Monate Aufbewahrung, `rohdaten.md` §12 nennt produktiv rund 18 und danach ein Archivsystem | **Nicht auflösbar von hier aus.** M92 rechnet mit der Testkopie und sagt das dazu. Zusätzlich fällt auf: §8 Z. 1030 setzt für Listen-Endpunkte „Maximum ein Jahr" neben eine Aufbewahrung von 22 Monaten, ohne die Spannung zu kommentieren |
| **E‑f gegen `PROJEKTBESCHREIBUNG.md` §3.2** | So im Auftrag benannt | **Diese Zuordnung geht nicht auf.** §3.2 ist die Beschreibung der Tabelle `Message` und sagt über Mandanten nichts; E‑f („`SYSTEM` und `WOC` werden behandelt wie jeder andere Mandant") hat dort keine Entsprechung. Die zu korrigierende Stelle ist eine andere und in dieser Runde nicht gefunden worden. **Gemeldet, nicht aufgelöst** |

**Zwei weitere Stellen kommen hinzu**, die der Auftrag nicht nennt:

| Stelle | Was nachzuziehen ist |
|---|---|
| **`docs/README.md`** | Diese Datei ist neu und dort **nicht eingetragen**. `CLAUDE.md` verlangt den Eintrag, der Auftrag sperrt die Datei (Abweichung A12). Der Eintrag ist der erste Handgriff der Korrekturrunde |
| **offener Punkt 33** | trägt keinen Erledigt-Vermerk — siehe unten |

**Und die fünfte inhaltliche Stelle:** **offener Punkt 41** — die
Indexliste ist längst korrigiert, der offene Punkt 33 sieht aber weiter offen aus, und **genau
deshalb steht die überholte Fassung im Auftrag dieser Runde**. Das ist derselbe Mechanismus, den
Punkt 33 selbst beschreibt: „Die falsche Liste hat einen Arbeitsauftrag falsch gemacht."

---

# Widerspricht ein Befund einer Entscheidung aus §2?

**Nein.** Geprüft, einzeln:

| Entscheidung | Stand nach dieser Runde |
|---|---|
| **E‑a** — Schlüssel `(Stunde, ProcessID, Status)`, Mandant/Partner/Richtung zur Lesezeit gejoint | **gestützt** (M89: 11,30 ms im Standardfenster) |
| **E‑g** — Rohwert in der Zeile, Klassifikation beim Lesen | **gestützt** (M87: der Preis ist **eine** Zeile) |
| **E‑c** — „Überfällig" aus einer Live-Abfrage, Ausnahme von L2 | **gestützt** (M90: 2,3 bis 5,4 ms) |
| **E‑h** — die Kachel zählt nur im gewählten Zeitfenster | **nicht widerlegt**, aber gegen diese Testkopie nicht sichtprüfbar (0 gegen 538) → offener Punkt 43 |
| **E‑i** — `OFFEN` und gepflegt-mit-leerem-Partner fallen zusammen | **gestützt**, und der Zusammenfassungsverlust ist bei allen drei gemessenen Mandanten **null** |
| **E‑f** — `SYSTEM` und `WOC` wie jeder andere Mandant | **nicht berührt**; die Zahl daneben steht in offenem Punkt 47 |
| **E‑d** — „Unquittiert" nicht im MVP | **nicht gemessen**, wie vorgesehen |
| **E‑e / E‑j** — genau ein neuer Parameter | **nicht gemessen** |
| **Standard: Stundenauflösung über 48 Stunden** | **gestützt** in der Leistung (11,30 ms); der Bestand der Testkopie füllt dieses Fenster nicht (Befund 12) |

**Was der Runde widerspricht, ist nicht eine Entscheidung aus §2, sondern eine Vorbedingung des
Auftrags selbst:** V3 geht von einem Widerspruch zwischen `PROJEKTBESCHREIBUNG.md` §3.2 und der
Datenbank aus, der seit dem 20.08.2026 nicht mehr besteht. Gemeldet unter V3 und als offener
Punkt 41 — **nicht aufgelöst.**
