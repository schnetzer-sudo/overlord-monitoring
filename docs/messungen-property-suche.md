# Messungen zur Property-Suche — `MessagePropertySearchListEntry`

Erhoben am **07.09.2026** gegen die Testkopie (`GlassfishDB`).
Auftrag: „Messauftrag — Property-Suche (M152 bis M159)", Stand 07.09.2026.

**Diese Runde baut nichts und entscheidet nichts.** Kein Endpunkt, keine Repository-Methode, kein
Frontend, keine Migration, kein Test. Die Lesarten standen vor der Erhebung fest; sie sind unten je
Messung als *Vorregistrierte Deutung* mitgeführt und nach dem Ergebnis unverändert dagegengehalten.
Welche Bauform daraus wird, entscheidet der Auftraggeber.

**Keine bestehende Datei ist angefasst worden**, insbesondere nicht
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) — weder Abschnitt 9 noch der Ausnahmekasten zu
Regel L4. Beides folgt im Bauauftrag, wenn die Messungen halten. Die einzige Ausnahme ist eine
Zeile in `.gitignore` für das Ergebnisverzeichnis dieser Runde.

---

## ⚠️ Zwei gemeldete Abweichungen vom Auftrag — beide betreffen Nummern, keine davon ist still

Der Auftrag nennt in §1 die höchsten vergebenen Nummern. **Beide Angaben sind zum Zeitpunkt der
Ausführung überholt**, weil zwischen dem Schreiben des Auftrags und dieser Runde der Bauschritt
10c‑4b eingegangen ist (Commits `1e7acd1` bis `e8f2fa4`, alle vom 07.09.2026).

### Abweichung 1 — **M152 ist vergeben; diese Runde vergibt M153 bis M160**

Der Auftrag nennt M151 als höchste vergebene Nummer und weist M152–M159 zu. **M152 ist seit dem
Bauschritt 10c‑4b vergeben** ([`process-view.md`](process-view.md) §42, „M152 — am gebauten
Endpunkt, beste von fünf, in Millisekunden"; Commit `dd92b3c`, „messung(10c‑4b): M152 am gebauten
Endpunkt"). Die Zeile `process-view.md:3468` führt die Prüfung selbst mit — sie war zu ihrem
Zeitpunkt richtig und ist es seit der Vergabe nicht mehr.

**Folge: Die acht Messungen dieser Runde laufen als M153 bis M160.** Die Zuordnung steht unten in
der Umsetzungstabelle. Der Inhalt jeder Messung ist unverändert der des Auftrags.

### Abweichung 2 — **E‑93 bis E‑98 sind doppelt belegt; diese Datei führt E‑99 bis E‑105**

Der Auftrag führt in §9 acht Entscheidungen der Sparringsrunde als **E‑93 bis E‑100**. **Sechs
dieser Nummern sind im Repository bereits an einen ganz anderen Gegenstand vergeben** —
[`process-view.md`](process-view.md) §43, sämtlich datiert 07.09.2026:

| Nummer | Belegt in `process-view.md` mit | Im Auftrag §9 belegt mit |
|---|---|---|
| **E‑93** | Die drei Paare bleiben unzerlegt, Text byteidentisch | Eine Suchfläche, zwei Quellen im Angebot |
| **E‑94** | `bis` in der Anfrage einschließend | Der Feldname ist Pflicht |
| **E‑95** | Nicht stundengenaues Fenster wird abgewiesen | Beide Typen im Angebot, Trennung im Backend |
| **E‑96** | Vierter Knopf freiwillig, Datumsfelder daneben | Ausnahmekasten zu Regel L4 |
| **E‑97** | Eigenschaft 4 wird ersetzt, E‑42 bleibt | *im Auftrag als gegenstandslos geführt* |
| **E‑98** | Kein Index auf der Tagesebene | Absprung im Detailpanel |

**Die Property-Suche-Entscheidungen stehen bisher in keiner Repository-Datei.** Sie mit den
Auftragsnummern festzuschreiben erzeugte im selben Moment sechs Doppelvergaben. Deshalb führt diese
Datei sie unter den **nächsten freien** Nummern und nennt die Auftragsnummer daneben.

**Nicht geändert wird der Inhalt.** §6 des Auftrags stellt die Entscheidungen außer Streit, und das
gilt hier unverändert: umnummeriert ist die Marke, nicht die Sache. **Der Auftraggeber entscheidet,
ob die Umsetzung so bleibt** — verworfen wird sie nur, indem `process-view.md` §43 umnummeriert
wird, und das ist eine Änderung an einer bestehenden Datei und damit nicht Gegenstand dieser Runde.

| Auftrag §9 | In dieser Datei | Entscheidung |
|---|---|---|
| E‑93 | **E‑99** | Eine Suchfläche in der Kopfzeile, zwei Quellen im Angebot |
| E‑94 | **E‑100** | Der Feldname ist Pflicht |
| E‑95 | **E‑101** | Beide Typen im Angebot, Trennung im Backend |
| E‑96 | **E‑102** | Regel L4 bekommt einen benannten Ausnahmekasten |
| ~~E‑97~~ | — | gegenstandslos, keine Nummer vergeben |
| E‑98 | **E‑103** | Absprung im Detailpanel |
| E‑99 | **E‑104** | Absolutes `von`/`bis` beim Absprung |
| E‑100 | **E‑105** | Technische Feldnamen unverändert |

---

## Nummernvergabe

| | |
|---|---|
| Prüfung Messungen | `grep -rnoE '\bM(15[2-9])\b' docs/ scripts/ *.md` |
| Ergebnis | **Treffer für M152** — elf in `docs/process-view.md`, einer in `docs/README.md`, dazu 22 in einer Ergebnisdatei unter `scripts/`. **Jeder Treffer einzeln gelesen**, wie der Auftrag es verlangt: keiner ist eine Aussage *über* Nummern, alle elf in `process-view.md` sind Vergabe (Überschrift §42, Messtabelle, Regelbezug, offener Punkt 139). **M153 bis M179: kein Treffer** |
| Gegenprobe | `grep -rnoE '\bM1(5[1-9]\|6[0-9])\b' docs/README.md` → findet **M151 und M152**, also greift der Ausdruck über denselben Bereich. Ein Ausdruck, der nichts findet, wäre ohne diese Probe wertlos |
| Folge | **M153 bis M160 sind hier vergeben.** Verschiebung um eins gegenüber dem Auftrag, siehe Abweichung 1 |
| Prüfung Entscheidungen | `grep -rhoE 'E(‑\|-)[0-9]{1,3}' docs/ scripts/ *.md` |
| Ergebnis | Höchste vergebene: **E‑98**. **E‑99, E‑100, E‑101 frei** (`grep -rnE 'E(‑\|-)(99\|100\|101)\b'` → kein Treffer) |
| Falschtreffer | `E‑780` in `scripts/messung-liste-verengung/ergebnis/m99b-vorabfrage-zerlegt.txt` — ein Datenwert in einer Rohausgabe, keine Entscheidung. Einzeln gelesen und ausgesondert |
| Folge | **E‑99 bis E‑105 sind hier vergeben**, siehe Abweichung 2 |

> **Der Suchausdruck des Auftrags greift für die E‑Nummern nicht.** `grep -rnoE 'E‑(1[0-9][0-9])\b'`
> und `grep -rhoE 'E[‑-][0-9]{1,3}'` liefern **null** beziehungsweise nur die ASCII-Treffer, obwohl
> `E‑93` bis `E‑98` in `process-view.md` stehen. Ursache ist die Zeichenklasse `[‑-]` mit dem
> Mehrbyte-Zeichen U+2011 — dieses `grep` wertet sie nicht als zwei Alternativen. **Lauffähig ist
> die Alternation außerhalb der Klasse:** `E(‑|-)[0-9]{1,3}`. Ohne die Gegenprobe wäre die
> Doppelvergabe aus Abweichung 2 unentdeckt geblieben, und der Ausdruck hätte „alles frei"
> gemeldet.

**Offene Punkte** setzen bei **142** an. Projektweit höchste vergebene Nummer ist **141**
([`process-view.md`](process-view.md) §45); `grep -rnoE 'Punkt \*?\*?14[2-9]' docs/*.md` findet
nichts.

---

## Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` — niemals die Produktion |
| Nachweis Testkopie | `SELECT @@global.read_only` → **`1`**, als **erste Abfrage jeder Sitzung** |
| Benutzer | **`monitor_read@%`**, ausschließlich `SELECT`. **Kein Statement dieser Runde schreibt**, keines fasst `overlord_monitor` an |
| Sitzungen | sequenziell, jede eine eigene `mysql`-Ausführung mit einer Skriptdatei unter `scripts/messung-property-suche/` |
| Serverzeit Beginn | `2026-09-07 16:11:14` |
| Client | `mysql.exe` **Ver 8.0.46** aus MySQL Workbench 8.0 CE, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t` |
| Passwortübergabe | über `MYSQL_PWD` aus `OVERLORD_DB_READ_PASSWORD` — kein Passwort auf der Befehlszeile, keines in einer Skriptdatei |
| Laufzeit | `SET profiling = 1`, `SET profiling_history_size = 100` (Vorgabe der Instanz ist **15**), Auswertung über `information_schema.PROFILING` — **nicht** über `SHOW PROFILES`, weil dieses den Statementtext mitgibt und damit `MessagePropertyValue` in die Rohausgabe trüge (Regel G1) |
| `@@session.sql_mode` | `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION` |
| `@@div_precision_increment` | **4** — deshalb steht in keiner Abfrage dieser Runde ein `AVG` über einen Wahrheitswert; Anteile sind als `100.0 * SUM(…) / COUNT(*)` gerechnet |
| `@@max_statement_time` (Vorgabe) | **`0.000000`** — die Instanz kennt von sich aus keine Grenze |
| Grenze dieser Runde | `SET max_statement_time = 60` je Messsitzung. **Der Lese-Pool der Anwendung hat 10 s** ([`datenzugriff.md`](datenzugriff.md) §1); jede gemessene Laufzeit wird gegen **diese** Zehn gehalten und nicht gegen die Sechzig des Messclients |
| `innodb_buffer_pool_size` | **26.843.545.600** Byte (25.600 MiB) |
| Datenstand | `Message.MessageLastUpdate` von **`2024-10-01 02:00:28`** bis **`2026-07-08 17:21:10`** — unverändert gegenüber M0 und M83 |

**Zeitfenster — absolut, nie relativ.** Die Testkopie hat eine fünfmonatige Lücke und ein praktisch
leeres Jahr 2026; der dichte Bestand endet am **2025‑12‑30**. Jedes Fenster dieser Runde endet dort
und ist absolut angegeben. Kein `NOW()`, kein relatives Fenster.

**Mandanten (Regel L7):** gemessen wird gegen **`NEXANS`** (trägt praktisch das gesamte Aufkommen)
und **`SUTTONS`** (klein). Wo eine Messung über alle zehn läuft, ist das dort ausdrücklich gesagt.

**Regel M1 und die Messung.** Kein Statement dieser Runde nimmt eine Mandanten-ID aus einer Anfrage
entgegen — es gibt in dieser Runde keine Anfrage und keinen Endpunkt. Die `MandantID` steht hier als
**Messparameter im Skript**, so wie in jeder Messrunde davor. **Der Unterschied ist der Ort:** Im
gebauten Endpunkt kommt sie aus der Sitzung und nirgends sonst
([`mandantentrennung.md`](mandantentrennung.md) §3, Ausnahmeliste unverändert zwei Einträge). Eine
Messung, die den Wert setzt, sagt nichts darüber, woher ein Endpunkt ihn nähme.

---

## Die Entscheidungen, die dieser Runde vorausgehen

Getroffen in der Sparringsrunde zur Property-Suche, **nicht** Gegenstand dieser Messungen. Sie
stehen hier, damit die Messungen einen benannten Zweck haben. Zur Nummerierung siehe Abweichung 2.

| # | Auftrag | Entscheidung |
|---|---|---|
| **E‑99** | E‑93 | Eine Suchfläche in der Kopfzeile, **zwei Quellen im Angebot**: BAM-Typen aus `MessageBAMMandant`, Feldnamen aus `MessagePropertySearchListEntry` |
| **E‑100** | E‑94 | **Der Feldname ist Pflicht.** Eine typlose Suche erreicht ausschließlich BAM und **nie** eine Property |
| **E‑101** | E‑95 | Beide Typen stehen im Angebot; das Backend trennt intern **Spaltenprädikat (Typ 0)** von **EAV-Zugriff (Typ 1)** |
| **E‑102** | E‑96 | Regel L4 bekommt einen **benannten Ausnahmekasten**, Bauform wie E‑c bei Leistungsregel 2. Der Regeltext selbst bleibt unverändert |
| **E‑103** | E‑98 | Absprung „Im Prozessbaum anzeigen" sitzt im **Detailpanel** der Nachricht, nicht als Kontextmenü. Grund: Tastatur- und Berührungserreichbarkeit, dieselbe Disziplin wie E‑51/E‑54 |
| **E‑104** | E‑99 | Der Absprung reicht das Fenster der Liste als **absolutes `von`/`bis`** weiter, nie den relativen Modus. Damit ist die gefundene Nachricht im Zielfenster **per Konstruktion** enthalten |
| **E‑105** | E‑100 | Das Angebot zeigt **technische Feldnamen unverändert**. Keine Kuratierungsspalte |
| **Umfang** | — | Die Property-Suche gehört in **Abschnitt 9 („Enthalten")** und ist damit MVP |

> **Ein Risiko ist dabei bewusst eingegangen worden, und es gehört hierher.** Die MVP-Zusage ist
> **vor** der Messung gegeben worden. Fällt die Deckung je Feldname aus wie bei den BAM-Spalten
> (M11: auf 98,93 % der Zeilen leer, Spalte anschließend ausgebaut), steht die Wahl zwischen einer
> leeren Suche und dem Streichen einer MVP-Zeile. **M156 ist die Messung, die das entscheidet.**

---

## Verifizierte Ausgangslage — in dieser Runde nicht neu erhoben

Übernommen aus §2 des Auftrags. Die Messdateien der Vorschritte sind dafür nicht geöffnet worden.

| Gegenstand | Wert | Herkunft |
|---|---|---|
| `MessageProperty`, Zeilen | **75.571.462**, gezählt | M44, 12.08.2026 |
| `MessageProperty`, Größe | **61,0 GB**, davon **45,9 GB** Index | M14 / M44 |
| `MessageBAM`, Zeilen | 15.406.350, gezählt | M33‑0 |
| `Message`, Zeilen | 3.341.519, gezählt | M0 |
| Primärschlüssel `MessageProperty` | `(MessageID, MessagePropertyName, MessageActionID)` | M14 |
| `MessagePropertyNameIDX` | `(MessagePropertyName)`, **Kardinalität 18** | M14 |
| `MessagePropertyValueIDX` | `(MessagePropertyValue(50))` — Präfix | M14 |
| `MessagePropertyNameValueIDX` | `(MessagePropertyName, MessagePropertyValue(50))` | M14 |
| Verschiedene Namen, an **einem** Tag gemessen | **101** | M17‑2 |
| Häufigster BAM-Wert überhaupt | 234.159 Treffer | M33 |
| `MessagePropertyValue` | `mediumtext`, einzige `NULL`-fähige Spalte | M44 |

**Die Spannung zwischen Kardinalität 18 und 101 gemessenen Namen ist der Kern von M158.** Der
Optimizer hält den Namensfilter für rund fünfmal selektiver, als er ist. Dieselbe unbrauchbare
Statistik hat bei `MessageStatusIDX` den 13,2-Sekunden-Fall aus L15 erzeugt.

---

## Die vorregistrierten Deutungen — vor dem ersten Lauf geschrieben

Dieser Abschnitt ist **vor** der ersten Messung festgeschrieben (Commit „docs: Messrahmen
Property-Suche, Nummernvergabe M153–M160"). Er steht hier vollständig, damit unter jeder Messung
nachzulesen ist, was vorher erwartet wurde — und nicht, was hinterher gepasst hätte.

| Messung | Gegenstand | Vorregistrierte Deutung |
|---|---|---|
| **M153** | Was die Tabelle ist | Drei Spalten — `MessagePropertyName`, `MandantID`, `MessagePropertyType`. **Keine Beschreibungs- oder Sortierspalte.** Trifft das nicht zu und es gibt eine Klartextspalte, ist **E‑105 auf falscher Voraussetzung getroffen** und gehört neu vorgelegt |
| **M154** | Verteilung über Typ und Mandant | `MessagePropertyType ∈ {0, 1}`. `MandantID IS NULL` bedeutet „gilt für alle Mandanten". Ein dritter Typwert bricht die Lesart aus M155 — dann ist M155 **nicht** auszuführen, sondern der Befund zu melden |
| **M155** | Gegenprobe der Typ-Lesart | Typ 1 → kommt in `MessageProperty.MessagePropertyName` vor. Typ 0 → kommt **nicht** vor, weil er eine Spalte von `Message`, `Process` oder `SOS` benennt. **Ein einziger Gegenfall in beide Richtungen kippt E‑101** |
| **M156** | Deckung je Name | Ein Name unter **20 %** Deckung über die **Wurzeln** ist als Suchfeld fragwürdig. Die Schwelle siebt nicht aus, sie erzeugt einen Vermerk und eine Vorlage. Begründung: M28‑2 misst für taugliche BAM-Typen 83 bis 92,26 % über die Wurzeln und 0,72 % für den untauglichen Fall |
| **M157** | Werteverteilung je Name | Ein Name, dessen häufigster Wert **234.159** Zeilen überschreitet, ist ohne zusätzliche Behandlung nicht anbietbar — das ist der gemessene Höchstwert der BAM-Suche (M33) |
| **M158** | Plan des Wertprädikats | `ref` über **`MessagePropertyNameValueIDX`**, beide Spalten genutzt. Wählt der Optimizer stattdessen `MessagePropertyNameIDX`, **ist das der Befund der Runde** |
| **M159** | Der ganze Weg | Unter **500 ms** im 30-Tage-Fenster (Budget aus §8 der Projektbeschreibung). Die 90 Tage sind hier **nicht** gesetzt, sondern werden gemessen |
| **M160** | Werte über die Präfixgrenze | Die Namen tragen Kennungen und Nummern, also überwiegend **kurze** Werte. Trifft das nicht zu, ist der Präfixindex für den betroffenen Namen ein **Vorfilter** und kein Zugriffspfad |

**Warum die Trennung nach Stellung in der Kette bei M156 Pflicht ist.** M11 hat „über alle Zeilen"
gemessen und daraus „über jede Teilmenge" geschlossen. Der Schluss war falsch und hat eine Spalte
gekostet.

> ⚠️ **Regel G1 greift in dieser Runde scharf, und sie greift genau an M157 und M160.** Diese Datei
> enthält **keinen einzigen `MessagePropertyValue`** — auch nicht abgekürzt, auch keine GUID, auch
> nicht als Beispiel. Ausgegeben werden ausschließlich **Ränge und Zähler**: „häufigster Wert:
> 12.480 Zeilen" — *die Zahl ist hier ein erfundenes Beispiel aus dem Auftragstext und
> kein Messwert dieser Runde* —, nicht der Wert. **Feldnamen sind Konfiguration und dürfen stehen.** Prozesse und
> Partner werden maskiert, und die Maske entsteht **im Statement** über `ROW_NUMBER()`, nicht durch
> Abschreiben.

---

## M153 — Was die Tabelle ist

**Sitzung** `scripts/messung-property-suche/s1-m153.sql`. `SHOW CREATE TABLE`,
`information_schema.COLUMNS`, `information_schema.STATISTICS`, Zeilenzahl per `COUNT(*)`.

> **Vorregistrierte Deutung.** Drei Spalten — `MessagePropertyName`, `MandantID`,
> `MessagePropertyType`. **Keine Beschreibungs- oder Sortierspalte.** Trifft sie nicht zu und es
> existiert eine Klartextspalte, ist E‑105 („technische Namen unverändert anzeigen") auf falscher
> Voraussetzung getroffen.

**Die Deutung hat getroffen, Spalte für Spalte.**

```sql
CREATE TABLE `MessagePropertySearchListEntry` (
  `MessagePropertyName` varchar(100) NOT NULL,
  `MandantID` varchar(36) DEFAULT NULL,
  `MessagePropertyType` int(11) DEFAULT NULL,
  PRIMARY KEY (`MessagePropertyName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
```

| pos | Spalte | Typ | `NULL` | Schlüssel | Kollation |
|---:|---|---|---|---|---|
| 1 | `MessagePropertyName` | `varchar(100)` | nein | **PRI** | `utf8mb4_general_ci` |
| 2 | `MandantID` | `varchar(36)` | ja | — | `utf8mb4_general_ci` |
| 3 | `MessagePropertyType` | `int(11)` | ja | — | — |

**Es gibt keine Beschreibungsspalte und keine Sortierspalte.** Damit steht **E‑105** auf der
Voraussetzung, die für sie angenommen war: Diese Tabelle *kann* keinen Klartext liefern, weil sie
keinen führt. Sie ist darin **ärmer als `MessageBAMMandant`**, das mit `MessageBAMTypeSortIndex`
eine Ordnung und über `MessageBAMType.MessageBAMTypeDescription` eine Bezeichnung anbietet
([`bam-suche.md`](bam-suche.md) §10). Die zweite Quelle des Angebots (E‑99) liefert also **Namen
ohne Beschriftung und ohne Ordnung**, die erste beides.

**Indizes: genau einer, der Primärschlüssel.**

| Index | Spalte | eindeutig | Kardinalität | Typ |
|---|---|---|---:|---|
| `PRIMARY` | `MessagePropertyName` | ja | **7** | BTREE |

> **Der Primärschlüssel steht auf dem Namen allein — und das ist der Befund dieser Messung, der
> über E‑99 hinausreicht.** `MessageBAMMandant` trägt die `MandantID` **im** Schlüssel; derselbe
> BAM-Typ kann dort für zehn Mandanten getrennt konfiguriert sein. Hier kann er das nicht: **Ein
> Feldname existiert genau einmal**, und die `MandantID` daneben ist eine Eigenschaft der Zeile,
> keine Achse der Konfiguration. Für zwei Mandanten denselben Namen mit verschiedenem Typ zu
> hinterlegen, ist in dieser Tabelle **nicht darstellbar**. Was daraus folgt, misst M154.

**Zeilenzahl: 12, gezählt.**

| Quelle | Wert |
|---|---:|
| `COUNT(*)` — **verwendet** | **12** |
| `information_schema.TABLE_ROWS` — **nicht verwendet** | 7 |
| `PRIMARY`-Kardinalität in `STATISTICS` | 7 |

**Die Schätzung liegt 41,7 % zu niedrig, und die Indexkardinalität liegt mit ihr falsch.** Das ist
dieselbe Falle, die am 12.08.2026 die Zeilenzahlen von `MessageBAM` und `MessageProperty` in der
Projektbeschreibung um 41,9 % und 60,9 % verfälscht hat (Kasten in §8). Sie ist hier folgenlos,
weil zwölf Zeilen in jedem Plan billig sind — **aber sie ist derselbe Fehler und wird deshalb
ausgewiesen**, nicht weggelassen.

**Weitere Angaben:** `ENGINE=InnoDB`, `CREATE_TIME` **2026‑08‑14 17:14:34**, `DATA_LENGTH` 32.768
Byte, `INDEX_LENGTH` 0.

*Belegvermerk (L10): gemessen sind Aufbau, Indizes und die gezählte Zeilenzahl der **Testkopie**.
Behauptet wird nichts über die Produktion — dort kann dieselbe Tabelle mehr Zeilen tragen. Der
Aufbau ist davon unberührt, die Verteilung in M154 nicht.*

---

## M154 — Verteilung über Typ und Mandant

**Sitzung** `scripts/messung-property-suche/s2-m154.sql`. Zeilen je `MessagePropertyType`, je
`MandantID`, **einschließlich `NULL`**, über alle zehn Mandanten.

> **Vorregistrierte Deutung.** `MessagePropertyType ∈ {0, 1}`. `MandantID IS NULL` bedeutet „gilt
> für alle Mandanten". Ein dritter Typwert bricht die Lesart aus M155 — dann ist M155 nicht
> auszuführen, sondern der Befund zu melden. `NULL` ist als eigene Zeile auszuweisen und **niemals**
> wegzufiltern.

**Der erste Teil hat getroffen: es gibt genau zwei Typwerte, 0 und 1.** M155 wird ausgeführt.

| `MessagePropertyType` | Zeilen |
|---:|---:|
| 0 | **8** |
| 1 | **4** |
| *`NULL`* | **0** |

| `MandantID` | Zeilen |
|---|---:|
| *`NULL`* | **8** |
| `NEXANS` | **4** |

**Die Kreuztabelle ist der eigentliche Befund, und sie ist vollständig entartet:**

| Typ | Mandant | Zeilen |
|---:|---|---:|
| 0 | *`NULL`* | **8** |
| 1 | `NEXANS` | **4** |

> ⚠️ **Typ und Mandant fallen zusammen — die Messung kann die beiden Wirkungen nicht trennen.**
> Es gibt **keine** Zeile mit Typ 0 und einem Mandanten, und **keine** mit Typ 1 und `NULL`. Die
> vorregistrierte Lesart „`NULL` heißt: gilt für alle Mandanten" ist mit diesen Daten **verträglich,
> aber nicht belegt**: „`NULL` heißt: keinem Mandanten zugeordnet" passt genauso gut, und die
> Verteilung unterscheidet die beiden nicht. **Die Lesart bleibt damit eine Vermutung** (L10:
> *gemessen war die Verteilung, behauptet wird eine Bedeutung*). Was sie stützt, misst M155 — aber
> auch dort nur mittelbar.

**Und dieser Befund trifft E‑99 unmittelbar.** Die vier Typ‑1‑Namen — also **alle**, die überhaupt
in `MessageProperty` stehen können — sind ausschließlich für **`NEXANS`** konfiguriert:

| Für einen Mandanten anbietbar | `NEXANS` | die übrigen neun |
|---|---:|---:|
| Typ‑0‑Namen (Spaltenprädikat) | 8 | 8 *unter der `NULL`-Lesart* |
| **Typ‑1‑Namen (EAV-Zugriff)** | **4** | **0** |

**Für neun von zehn Mandanten enthält die zweite Quelle des Angebots keine einzige echte
Property.** Ob das eine Kuratierungslage der Testkopie ist oder in der Produktion ebenso steht,
sagt diese Messung nicht — der Katalog der Testkopie ist nachweislich kuratiert und ändert sich.
**Offener Punkt 142.**

**Die zwölf Zeilen vollständig.** Feldnamen sind Konfiguration und dürfen stehen (G1); `MandantID`
ist ein in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 offen geführter Code.

| Typ | `MessagePropertyName` | `MandantID` |
|---:|---|---|
| 0 | `Message.MessageID` | *`NULL`* |
| 0 | `Message.MessageIDSource` | *`NULL`* |
| 0 | `Message.MessageIDTarget` | *`NULL`* |
| 0 | `Message.ProcessID` | *`NULL`* |
| 0 | `Message.ProcessName` | *`NULL`* |
| 0 | `Message.SOSID` | *`NULL`* |
| 0 | `Message.SOSName` | *`NULL`* |
| 0 | `Message.Status` | *`NULL`* |
| **1** | `Converter.TransactionID` | `NEXANS` |
| **1** | `Message.GUID` | `NEXANS` |
| **1** | `Message.ReceiverID` | `NEXANS` |
| **1** | `Service.Type` | `NEXANS` |

**Zwei Gegenproben, beide sauber:**

| Frage | Ergebnis |
|---|---|
| Kommt ein Name mehrfach vor? | 12 Zeilen, **12** verschiedene Namen — der Primärschlüssel hält, was M153 sagt |
| Ist `NULL` echt oder ein leerer String? | `MandantID IS NULL` **8**, leerer String **0**, `MessagePropertyType IS NULL` **0** |

*Belegvermerk (L10): gemessen ist die Verteilung auf der Testkopie am 07.09.2026. Behauptet wird
nicht, dass sie in der Produktion so aussieht, und nicht, dass `NULL` „alle Mandanten" bedeutet.*

---

## M155 — Gegenprobe der Typ-Lesart

**Sitzung** `scripts/messung-property-suche/s3-m155.sql`, Nachtrag `s3b-nachtrag.sql`. Für **jeden**
Namen aus M153: Existiert er in `MessageProperty.MessagePropertyName`? Als **`EXISTS`-Probe je
Name** gegen `MessagePropertyNameIDX`, ausdrücklich **nicht** als
`SELECT DISTINCT MessagePropertyName FROM MessageProperty` — letzteres liest gegen 75,6 Mio. Zeilen
an und ist für diese Frage nicht nötig.

> **Vorregistrierte Deutung.** Typ 1 → kommt vor. Typ 0 → kommt **nicht** vor, weil es eine Spalte
> von `Message`, `Process` oder `SOS` benennt. **Ein einziger Gegenfall in beide Richtungen kippt
> E‑101.**

```sql
SELECT e.MessagePropertyType, e.MessagePropertyName, e.MandantID,
       EXISTS (SELECT 1 FROM GlassfishDB.MessageProperty p
                WHERE p.MessagePropertyName = e.MessagePropertyName) AS kommtVor
  FROM GlassfishDB.MessagePropertySearchListEntry e
 ORDER BY e.MessagePropertyType, e.MessagePropertyName
```

*Der Name kommt aus der Tabelle selbst und wird nirgends abgeschrieben — das hält G1 ein und macht
die Probe zugleich vom Kuratierungsstand unabhängig.*

**`EXPLAIN` (Regel L15).** Zwölf Zeilen aus der Konfiguration, je eine abhängige Unterabfrage:

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---:|---|---:|---|
| 1 | PRIMARY | `e` | `index` | `PRIMARY` | 402 | — | 7 | `Using index` |
| 2 | DEPENDENT SUBQUERY | `p` | **`ref`** | **`MessagePropertyNameIDX`** | 402 | `e.MessagePropertyName` | **2.609.126** | `Using index` |

`EXPLAIN FORMAT=JSON` weist zusätzlich einen `expression_cache` über der Unterabfrage aus. Die
Tabelle selbst wird nie angefasst — `Using index` auf beiden Seiten.

**Die Deutung hat getroffen, in beide Richtungen, ohne einen einzigen Gegenfall.**

| Typ | Name | Mandant | kommt in `MessageProperty` vor |
|---:|---|---|:--:|
| 0 | `Message.MessageID` | *`NULL`* | **nein** |
| 0 | `Message.MessageIDSource` | *`NULL`* | **nein** |
| 0 | `Message.MessageIDTarget` | *`NULL`* | **nein** |
| 0 | `Message.ProcessID` | *`NULL`* | **nein** |
| 0 | `Message.ProcessName` | *`NULL`* | **nein** |
| 0 | `Message.SOSID` | *`NULL`* | **nein** |
| 0 | `Message.SOSName` | *`NULL`* | **nein** |
| 0 | `Message.Status` | *`NULL`* | **nein** |
| **1** | `Converter.TransactionID` | `NEXANS` | **ja** |
| **1** | `Message.GUID` | `NEXANS` | **ja** |
| **1** | `Message.ReceiverID` | `NEXANS` | **ja** |
| **1** | `Service.Type` | `NEXANS` | **ja** |

**Acht von acht Typ‑0‑Namen fehlen, vier von vier Typ‑1‑Namen sind da. E‑101 steht** — die Trennung
zwischen Spaltenprädikat und EAV-Zugriff hat eine Entsprechung in den Daten.

**Laufzeit** — Aufwärmlauf und danach fünf Läufe, je `information_schema.PROFILING`:

| Lauf | 1 *(Aufwärmlauf)* | 2 | 3 | 4 | 5 | 6 |
|---|---:|---:|---:|---:|---:|---:|
| ms | 0,792 | 0,455 | 0,482 | 0,478 | 0,474 | 0,455 |

**Beste von fünf: 0,455 ms.** Zwölf Indexzugriffe über eine 75,6-Millionen-Zeilen-Tabelle kosten
zusammen weniger als eine halbe Millisekunde, weil jeder von ihnen beim ersten Treffer abbricht.
Genau das war der Grund, die Probe als `EXISTS` und nicht als `DISTINCT` zu bauen.

### Die vermutete Quellspalte je Typ‑0‑Name — **Vermutung, kein Befund**

Der Auftrag verlangt die Zuordnung und ihre Kennzeichnung. **Gemessen ist ausschließlich, dass die
genannte Spalte existiert** (`information_schema.COLUMNS`); dass der Name *sie* meint, ist eine
Vermutung aus der Namensähnlichkeit und aus nichts sonst.

| Typ‑0‑Name | vermutete Quellspalte | existiert | Verhältnis |
|---|---|:--:|---|
| `Message.MessageID` | `Message.MessageID` `varchar(36)` | ja | **wörtlich** |
| `Message.ProcessID` | `Message.ProcessID` `varchar(36)` | ja | **wörtlich** |
| `Message.SOSID` | `Message.SOSID` `varchar(36)` | ja | **wörtlich** |
| `Message.MessageIDSource` | `Message.SourceMessageID` `varchar(36)` | ja | **umgestellt** |
| `Message.MessageIDTarget` | `Message.TargetMessageID` `varchar(36)` | ja | **umgestellt** |
| `Message.Status` | `Message.MessageStatus` `varchar(30)` | ja | **umbenannt** |
| `Message.ProcessName` | `Process.ProcessName` `varchar(255)` | ja | **andere Tabelle** |
| `Message.SOSName` | `SOS.SOSName` `varchar(255)` | ja | **andere Tabelle** |

> ⚠️ **Nur drei von acht Namen sind wörtliche Spaltennamen — eine Abbildung ist nötig und sie ist
> nicht ableitbar.** Zwei stellen die Wortteile um, einer heißt anders als seine Spalte, und zwei
> tragen das Präfix `Message.`, obwohl sie in `Process` beziehungsweise `SOS` wohnen. Wer E‑101
> baut, kann die Spalte **nicht** aus dem Namen rechnen; er braucht eine Tabelle im Code, und
> jeder künftige Eintrag in `MessagePropertySearchListEntry` mit Typ 0 fällt aus ihr heraus, bis
> jemand sie ergänzt. **Offener Punkt 143.**

*Belegvermerk (L10): gemessen ist die Existenz beziehungsweise Abwesenheit jedes der zwölf Namen in
`MessageProperty` am 07.09.2026, und die Existenz der acht vermuteten Spalten. Behauptet wird
nicht, dass ein Typ‑0‑Name die ihm zugeordnete Spalte meint — dafür fehlt jeder Beleg außer der
Ähnlichkeit.*

### Nebenbefund — die Statistik, an der M158 hängt, ist um 37,9 % zu niedrig

Die `rows`-Schätzung 2.609.126 im Plan oben ist nicht willkürlich. Erhoben im Nachtrag:

| Größe | Wert |
|---|---:|
| `MessageProperty`, **gezählt** (M44) | **75.571.462** |
| `information_schema.TABLE_ROWS` — die Grundlage des Optimizers | **46.964.279** |
| Kardinalität `MessagePropertyNameIDX` | **18** |
| 46.964.279 / 18 | **2.609.126,6** — die Zahl aus dem Plan |

**Der Optimizer rechnet mit einer Tabelle, die es nicht gibt.** Er hält sie für 37,9 % kleiner als
sie ist, und er hält den Namensfilter für rund fünfmal selektiver als er ist (18 gegen die 101 in
M17‑2 an *einem* Tag gemessenen Namen). **Beide Fehler zeigen in dieselbe Richtung: Ein Zugriff
über den Namen sieht im Plan billiger aus, als er ist.** Das ist die Lage, die M158 prüft, und es
ist dieselbe, die bei `MessageStatusIDX` den 13,2-Sekunden-Fall aus L15 erzeugt hat.

*Die 46.964.279 sind zugleich die Zahl, die bis zum 12.08.2026 als „gemessen" in der
Projektbeschreibung stand und dort im Kasten zu §8 korrigiert ist. Sie ist aus der Dokumentation
verschwunden und **im Optimizer geblieben**.*

---

## M156 — Deckung je Name

**Sitzungen** `s4-vorprobe.sql`, `s5-m156-nexans.sql` (**abgebrochen, siehe unten**),
`s6-m156-fassungB.sql`, `s7-m156-nexans.sql`.

Für jeden Typ‑1‑Namen: Auf wie vielen Nachrichten ist er belegt? Absolut und in Prozent, über ein
Fenster von **30 Tagen bis 2025‑12‑30**, je Mandant, **zusätzlich getrennt nach Stellung in der
Kette**.

> **Vorregistrierte Deutung und die Schwelle, vor dem Lauf gesetzt.** Ein Name unter **20 %**
> Deckung über die **Wurzeln** ist als Suchfeld fragwürdig. **Die Schwelle siebt nicht automatisch
> aus** — sie erzeugt einen Vermerk und eine Vorlage an den Auftraggeber. Begründung: M28‑2 misst
> für taugliche BAM-Typen 83 bis 92,26 % über die Wurzeln und 0,72 % für den untauglichen Fall;
> 20 % liegt deutlich unter dem einen und deutlich über dem anderen.

**Das Fenster.** `MessageLastUpdate >= '2025-11-30 00:00:00'` bis `< '2025-12-30 00:00:00'`,
halboffen, absolut, genau 30 Tage. **Es ist Ziffer für Ziffer das Fenster B aus M28‑1 und M28‑2** —
214.330 Nachrichten über alle Mandanten, bei `NEXANS` 28.524 Wurzeln und 101.270 Kinder. Das war
nicht geplant und ist der glücklichste Umstand dieser Runde: **Die 20‑%-Schwelle ist damit nicht
nur der Zahl nach, sondern über dieselbe Grundmenge vergleichbar.**

**Die Rollen sind wie in M28‑1 gerechnet** (`verkettung.md` §5): Wurzel heißt `Source` gesetzt, Kind
heißt `SourceMessageID` belegt. Die beiden überlappen; sie teilen den Bestand nicht.

> **`Source` und `Target` sind `bit(1)`, nicht `varchar`** — das ist beim Schreiben des Prädikats
> aufgefallen. `Source <> ''` und `Source = 1` liefern auf `bit(1)` dasselbe (beide 28.524, in
> M156‑0 gegengeprüft), weil der Leerstring im Zahlenkontext zu 0 wird. Verwendet ist `= 1`.

### Die erste Fassung ist in die Zeitgrenze gelaufen — und das ist ein Ergebnis

Gebaut war die Messung zuerst als **vier `EXISTS` je Nachricht**, ausgewertet über eine abgeleitete
Tabelle. Der Plan sah gut aus:

| id | table | type | key | rows | Extra |
|---|---|---|---|---:|---|
| 1 | `m` | `range` | `MessageLastUpdateProcessMessageIDX` | 476.586 | `Using where; Using index` |
| 3–6 | `p` (4×) | `ref` | **`PRIMARY`**, `key_len` 548 | 1 | `Using where; Using index` |
| 7 | `pr`, `pm` | `eq_ref` | `PRIMARY` | 1 | `Using where` |

**Jeder einzelne Zugriff ist optimal, und die Abfrage ist trotzdem nach 60 Sekunden abgebrochen**
(`ERROR 1969`, `max_statement_time` = 60). Der Grund steht nicht im Plan: 180.251 Nachrichten mal
vier Unterabfragen plus die Mandantenkette sind über eine Million Indexzugriffe. **Das ist derselbe
Befund wie „erst deckeln, dann beschriften" in [`bam-suche.md`](bam-suche.md) §4** — der `EXPLAIN`
sagt nicht, *wie oft* eine Zeile angefasst wird.

**Fassung B stellt dieselbe Frage billiger:** ein Join statt vier Unterabfragen, mit dem Namensfilter
in der Join-Bedingung und `COUNT(DISTINCT …)` je Name.

```sql
SELECT p.MessagePropertyName,
       COUNT(DISTINCT m.MessageID) AS belegt,
       COUNT(DISTINCT CASE WHEN m.Source = 1 THEN m.MessageID END) AS belegtWurzel,
       COUNT(DISTINCT CASE WHEN m.SourceMessageID IS NOT NULL AND m.SourceMessageID <> ''
                           THEN m.MessageID END) AS belegtKind
  FROM GlassfishDB.Message m
  JOIN GlassfishDB.MessageProperty p
    ON p.MessageID = m.MessageID
   AND p.MessagePropertyName IN (…die vier Typ‑1‑Namen…)
 WHERE m.MessageLastUpdate >= ? AND m.MessageLastUpdate < ?
   AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                 JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ?)
 GROUP BY p.MessagePropertyName
```

**`EXPLAIN` (L15) — und die beiden Mandanten bekommen verschiedene Pläne:**

| Mandant | führende Tabelle | Zugriff | dann |
|---|---|---|---|
| `SUTTONS` | **`pm`** (`ProjectMandant`) | `ref` über `ProjectMandant_Mandant_idx`, `Using index` | `pr` `ref`, `m` **`ref` über `ProejctIDIDX`** (197.804 rows), `p` `ref` über `PRIMARY` `Using index` |
| `NEXANS` | **`m`** (`Message`) | **`range` über `MessageLastUpdateIDX`** | `pr`, `pm` je `eq_ref` über `PRIMARY`, `p` `ref` über `PRIMARY` `Using index` |

**Beim kleinen Mandanten steigt der Optimierer über den Mandanten ein, beim großen über die Zeit.**
Das ist dasselbe Kippen, das M147 bis M151 auf der Rollup-Stundenebene gefunden haben, und es
passiert hier ohne Zutun. In beiden Fällen wird `MessageProperty` **ausschließlich über die
`MessageID`** erreicht, als `ref` über den Primärschlüssel mit `Using index` — **Regel L4 ist in
dieser Messung eingehalten.**

**Laufzeit** (ein Lauf je Mandant, siehe Vermerk zur Laufzeitdisziplin unten): `SUTTONS` 2,7 s,
`NEXANS` 15,0 s, jeweils die ganze Sitzung samt Verbindungsaufbau.

### Ergebnis `NEXANS` — 180.251 Nachrichten, 28.524 Wurzeln, 101.270 Kinder

| Name | belegt | Anteil | auf Wurzeln | **je Wurzel** | auf Kindern | je Kind |
|---|---:|---:|---:|---:|---:|---:|
| `Message.GUID` | 180.251 | 100,000 % | 28.524 | **100,000 %** | 101.270 | 100,000 % |
| `Service.Type` | 180.251 | 100,000 % | 28.524 | **100,000 %** | 101.270 | 100,000 % |
| `Converter.TransactionID` | 180.231 | 99,989 % | 28.524 | **100,000 %** | 101.256 | 99,986 % |
| **`Message.ReceiverID`** | 46.986 | 26,067 % | 3.652 | **12,803 %** | 386 | 0,381 % |

### Ergebnis `SUTTONS` — 21.516 Nachrichten, 639 Wurzeln, 1.247 Kinder

| Name | belegt | Anteil | auf Wurzeln | **je Wurzel** | auf Kindern | je Kind |
|---|---:|---:|---:|---:|---:|---:|
| `Message.GUID` | 21.516 | 100,000 % | 639 | **100,000 %** | 1.247 | 100,000 % |
| `Service.Type` | 21.516 | 100,000 % | 639 | **100,000 %** | 1.247 | 100,000 % |
| `Converter.TransactionID` | 21.516 | 100,000 % | 639 | **100,000 %** | 1.247 | 100,000 % |
| **`Message.ReceiverID`** | **0** | 0,000 % | 0 | **0,000 %** | 0 | 0,000 % |

### Was die Schwelle sagt — und was sie nicht sagt

**Die 20 % greifen bei genau einem Namen: `Message.ReceiverID` mit 12,803 % über die Wurzeln bei
`NEXANS` und 0 % bei `SUTTONS`.** Das ist der Vermerk, den die Schwelle erzeugt, und er geht als
**Vorlage an den Auftraggeber** — ausgesiebt wird nichts.

**Zum Vergleich, dieselbe Grundmenge, M28‑2:** taugliche BAM-Typen 83,50 bis 92,26 % je Wurzel, der
untaugliche Fall 0,72 %. `Message.ReceiverID` liegt mit 12,80 % **zwischen** beiden und näher am
untauglichen Ende. **Die 0,381 % über die Kinder liegen unter dem untauglichen BAM-Fall.**

> **Das befürchtete Risiko ist nicht eingetreten — und dafür ein anderes.** Die MVP-Zusage stand
> unter dem Vorbehalt, die Deckung könne ausfallen wie bei M11 (auf 98,93 % der Zeilen leer). **Sie
> fällt gegenteilig aus:** Drei von vier Namen sind auf **jeder** Nachricht belegt. Die Suche läuft
> also nicht leer.
>
> **Aber 100 % Deckung ist keine gute Nachricht, sie ist bloß nicht die befürchtete.** Ein Name, der
> überall steht, schränkt nichts ein; die gesamte Selektivität muss vom **Wert** kommen. Genau das
> misst M157 — und dort kippt der Befund.

### Der Befund, der E‑99 unmittelbar trifft

**Bei `SUTTONS` sind drei der vier Namen auf 100 % der Nachrichten belegt — und keiner von ihnen ist
für `SUTTONS` konfiguriert** (M154: alle vier Zeilen tragen `MandantID = 'NEXANS'`).

| | `NEXANS` | `SUTTONS` |
|---|---|---|
| in `MessagePropertySearchListEntry` konfiguriert | 4 Namen | **0 Namen** |
| in den Daten tatsächlich belegt | 4 | **3, davon drei zu 100 %** |

**Die Konfigurationstabelle beschreibt die Daten nicht.** Ein `SUTTONS`-Nutzer könnte nach
`Message.GUID` suchen und fände auf jeder seiner Nachrichten einen Wert — das Angebot nach E‑99
zeigte ihm das Feld nicht. Ob die Tabelle eine Kuratierungslücke hat oder eine bewusste
Freischaltung je Mandant abbildet, sagt diese Messung **nicht**. **Offener Punkt 144.**

*Belegvermerk (L10): gemessen ist die Belegung über Fenster B (30 Tage bis 2025‑12‑30) auf der
Testkopie, je Mandant und je Kettenstellung. Behauptet wird nicht, dass die Quoten über andere
Fenster oder in der Produktion gleich ausfallen — und ausdrücklich nicht, dass ein Name, der bei
`SUTTONS` belegt ist, dort auch angeboten werden soll.*

---

## M157 — Werteverteilung je Name

**Sitzungen** `s8-m157-vorprobe.sql` (**abgebrochen**), `s9-m157a.sql`, `s10-m157-zaehlung.sql`,
`s11-m157-verteilung.sql`, `s12-m157b-m160.sql`.

> ⚠️ **Regel G1 greift hier scharf.** Dieser Abschnitt enthält **keinen einzigen
> `MessagePropertyValue`** — weder ganz noch abgekürzt, weder als GUID noch als Beispiel.
> Ausgegeben sind ausschließlich **Ränge und Zähler**. Die Prüfwerte sind im Statement über eine
> deterministische Auswahl hergeleitet und haben das Skript nie berührt.

> **Vorregistrierte Schwelle.** Ein Name, dessen häufigster Wert **234.159** Zeilen überschreitet,
> ist ohne zusätzliche Behandlung nicht anbietbar — das ist der gemessene Höchstwert der BAM-Suche
> (M33), also der schlimmste Fall, der dort mit Deckelung noch tragbar war.

### Der Gesamtbestand ist nicht in einem Statement zählbar — und das ist der erste Befund

Die Frage nach dem häufigsten Wert war für den **Gesamtbestand** gestellt, weil die Schwelle aus
M33 von dort stammt. **Sie ist dort nicht zu beantworten.** Schon das Zählen der vier Namen in
einem Statement bricht ab:

| Fassung | Plan | Ausgang |
|---|---|---|
| vier Namen als `IN`-Liste | `range` über `MessagePropertyNameValueIDX`, `Using index`, **29.283.016 rows** | **Abbruch nach 120 s** (`ERROR 1969`) |
| ein Name als `=` | `ref` über `MessagePropertyNameIDX`, `Using index` | läuft |

**Je Name einzeln, über den Gesamtbestand, gezählt** (`ref`, `Using index`, ein Lauf je Name):

| Name | Zeilen im Gesamtbestand | Laufzeit |
|---|---:|---:|
| `Service.Type` | **10.217.134** | **125,527 s** |
| `Converter.TransactionID` | 4.522.624 | 42,212 s |
| `Message.GUID` | **3.341.519** | 24,673 s |
| `Message.ReceiverID` | 918.500 | 0,468 s *(im Puffer aus dem Vorlauf)* |

> **`Message.GUID` trägt 3.341.519 Zeilen — das ist auf die Zeile genau die Zeilenzahl von
> `Message`** (M0). Genau ein Eintrag je Nachricht, über den gesamten Bestand, ohne einen einzigen
> Ausreißer nach oben oder unten. Das ist die sauberste Eigenschaft, die diese Runde gefunden hat.

**Ein einziges `COUNT(*)` über einen Namen kostet bis zu 125 Sekunden.** Der Lese-Pool der Anwendung
hat 10 Sekunden ([`datenzugriff.md`](datenzugriff.md) §1). **Damit ist jede Abfrage, die über den
Namen allein einsteigt, im Anwendungscode unmöglich** — nicht langsam, sondern unmöglich. Das ist
die gemessene Begründung für Regel L4, und sie ist deutlicher ausgefallen als der Regeltext
vermuten lässt.

### Die Verteilung, gemessen über das 30-Tage-Fenster

**Abweichung, ausdrücklich gemeldet:** Weil der Gesamtbestand nicht messbar ist, steht die
Verteilung über **Fenster B** (30 Tage bis 2025‑12‑30), dasselbe wie in M156. **Jede Zahl darin ist
eine untere Schranke für den Gesamtbestand** — derselbe Wert trägt dort mindestens so viele Zeilen
wie hier. Ein Unterschreiten der Schwelle im Fenster sagt über den Gesamtbestand **nichts**; ein
Überschreiten entscheidet sie.

**`NEXANS`**

| Name | verschiedene Werte | **häufigster Wert (Zeilen)** | Zeilen gesamt | Werte mit genau 1 Zeile |
|---|---:|---:|---:|---:|
| `Message.GUID` | 180.251 | **1** | 180.251 | 180.251 (**100 %**) |
| `Converter.TransactionID` | 238.242 | **2** | 239.533 | 236.951 (99,46 %) |
| `Message.ReceiverID` | 355 | **5.176** | 46.986 | 36 |
| **`Service.Type`** | **18** | **225.416** | 532.215 | **0** |

**`SUTTONS`**

| Name | verschiedene Werte | **häufigster Wert (Zeilen)** | Zeilen gesamt | Werte mit genau 1 Zeile |
|---|---:|---:|---:|---:|
| `Message.GUID` | 21.516 | **1** | 21.516 | 21.516 (**100 %**) |
| `Converter.TransactionID` | 42.399 | **2** | 42.430 | 42.368 (99,86 %) |
| `Message.ReceiverID` | **0** | — | — | — |
| **`Service.Type`** | **6** | **42.430** | 104.355 | **0** |

### Die Schwelle ist gerissen — und die 90 Tage entscheiden es eindeutig

Mit 225.416 Zeilen liegt `Service.Type` bei `NEXANS` im 30-Tage-Fenster **3,7 % unter** der
Schwelle 234.159. Weil das nur eine untere Schranke ist, ist dieselbe Messung über **90 Tage**
nachgeholt:

| `Service.Type`, `NEXANS` | verschiedene Werte | häufigster Wert (Zeilen) | Zeilen gesamt |
|---|---:|---:|---:|
| 30 Tage | 18 | 225.416 | 532.215 |
| **90 Tage** | **18** | **708.893** | 1.658.919 |

**708.893 gegen die Schwelle 234.159 — überschritten um Faktor 3,03**, und immer noch als untere
Schranke für den Gesamtbestand mit seinen 10.217.134 Zeilen. **`Service.Type` ist ohne zusätzliche
Behandlung nicht anbietbar.**

> **Und die Zahl der Werte erklärt, warum das kein Randfall ist.** `Service.Type` hat über 90 Tage
> **dieselben 18** verschiedenen Werte wie über 30 — die Menge wächst nicht, nur die Belegung. Das
> ist kein Suchschlüssel, das ist eine **Kategorie**: ein Feld mit einer Handvoll Ausprägungen, das
> jede Nachricht trägt. Wer ihn in ein Suchfeld schreibt, bekommt kein Ergebnis, sondern eine
> Teilmenge des Bestands.

**Die vier Namen zerfallen damit in drei Klassen:**

| Klasse | Namen | Kennzeichen |
|---|---|---|
| **Schlüssel** | `Message.GUID`, `Converter.TransactionID` | 100 % Deckung, Wert praktisch eindeutig (häufigster Wert 1 bzw. 2 Zeilen) |
| **Merkmal** | `Message.ReceiverID` | 12,80 % Deckung über die Wurzeln, 355 Werte, häufigster 5.176 Zeilen |
| **Kategorie** | `Service.Type` | 100 % Deckung, **18** Werte, häufigster über 708.893 Zeilen |

*Belegvermerk (L10): gemessen sind die Zeilenzahlen je Name über den Gesamtbestand und die
Werteverteilung über 30 beziehungsweise 90 Tage. Behauptet wird **nicht**, dass 708.893 der
Höchstwert des Gesamtbestands ist — er ist eine gemessene untere Schranke dafür. Der Gesamtbestand
ist an dieser Stelle nicht messbar, und die Lücke bleibt offen.*

### Vermerk zur Laufzeitdisziplin — eine gemeldete Abweichung

Der Auftrag verlangt zu jeder Messung die **beste von fünf nach einem Aufwärmlauf**. Für M156 und
M157 steht sie **nicht** da, sondern ein Einzelwert je Lauf. **Grund:** Diese beiden sind
Erhebungen über den Bestand und keine Kandidaten für eine gebaute Abfrage; ihre Laufzeit ist kein
Prüfkriterium, sondern nur ein Hinweis auf die Kosten. Sechs Läufe des 125-Sekunden-Statements
hätten die geteilte Testkopie zwölf Minuten lang belegt, ohne eine Aussage zu tragen. **Die volle
Disziplin steht dort, wo sie zählt: in M155, M158, M159 und M160.**

---

## M158 — Der Plan des Wertprädikats

**Sitzungen** `s13-m158.sql`, Diagnose `s15-m158d-m160g.sql`.

`EXPLAIN` und Laufzeit für `MessagePropertyName = ? AND MessagePropertyValue = ?`, **ohne Join**,
für den bestbelegten, den schlechtestbelegten und einen Namen mit hoher Wertkardinalität. Gemessen
sind **alle vier** Typ‑1‑Namen, je in zwei Fällen: mit dem **häufigsten** und mit dem
**seltensten** Wert des 30-Tage-Fensters.

> **Vorregistrierte Deutung.** `ref` über **`MessagePropertyNameValueIDX`**, beide Spalten genutzt.
> **Wählt der Optimizer stattdessen `MessagePropertyNameIDX`, ist das der Befund der Runde** — die
> Kardinalität 18 schlägt dann zu, und der Zugriff läuft über einen Namen statt über das Paar.

**Wie die Prüfwerte hergeleitet sind (Regel G1).** Je Fall wählt eine Unterabfrage im Statement den
häufigsten beziehungsweise seltensten Wert des Fensters deterministisch aus
(`ORDER BY COUNT(*) DESC|ASC, MessagePropertyValue ASC LIMIT 1`), legt ihn über `QUOTE()` in eine
Sitzungsvariable und setzt ihn über `PREPARE … FROM CONCAT(…)` als **Literal** ein. **Kein Wert
berührt das Skript, keiner erscheint in der Ausgabe** — ausgegeben ist nur seine Zeichenlänge.
*Das Literal ist nötig, weil eine Sitzungsvariable im Prädikat keine Bereichsanalyse bekommt; dann
stünde der Index in `possible_keys` und würde nie benutzt.*

### Die Deutung hat in zwei von vier Fällen getroffen — und die beiden anderen sind der Befund

| Name | Fall | Wertlänge | gewählter Index | `key_len` | `rows` | Treffer | **beste von fünf** |
|---|---|---:|---|---:|---:|---:|---:|
| `Message.GUID` | häufigster | 36 | **`MessagePropertyValueIDX`** | 203 | 1 | 1 | **0,456 ms** |
| `Message.GUID` | seltenster | 36 | **`MessagePropertyValueIDX`** | 203 | 1 | 1 | **0,451 ms** |
| `Converter.TransactionID` | häufigster | 6 | `MessagePropertyNameValueIDX` | 605 | 17 | 17 | **0,565 ms** |
| `Converter.TransactionID` | seltenster | 5 | `MessagePropertyNameValueIDX` | 605 | 7 | 7 | **0,471 ms** |
| `Message.ReceiverID` | häufigster | 10 | `MessagePropertyNameValueIDX` | 605 | 192.284 | **81.307** | **601,269 ms** |
| `Message.ReceiverID` | seltenster | 10 | `MessagePropertyNameValueIDX` | 605 | 50 | 50 | **0,810 ms** |
| **`Service.Type`** | **häufigster** | 9 | **`NULL` — `type = ALL`** | — | **46.964.279** | **4.366.602** | **81.429,454 ms** |
| `Service.Type` | seltenster | 10 | `MessagePropertyNameValueIDX` | 605 | 48.050 | 28.348 | **217,699 ms** |

Alle Index-Fälle tragen `Using index condition; Using where`. Der `key_len` **605** ist das Paar
(402 für den Namen, 203 für die 50 Zeichen Wertpräfix); **203** allein ist der reine Wertindex.

### Befund 1 — bei `Message.GUID` wählt der Optimizer den **reinen Wertindex**

Nicht `MessagePropertyNameIDX`, wie der Auftrag als Bösfall vorweggenommen hatte, sondern
`MessagePropertyValueIDX` — der Index **ohne** den Namen. Er schätzt eine Zeile und trifft damit
genau: Der Wert ist bestandsweit eindeutig (M157). Der Namensfilter wird zur Nachprüfung auf der
Zeile.

**Die Diagnose zeigt, dass es folgenlos ist.** Mit `FORCE INDEX (MessagePropertyNameValueIDX)`:

| `Message.GUID`, häufigster Wert | Index | `rows` | beste von fünf |
|---|---|---:|---:|
| **gewählt** | `MessagePropertyValueIDX` | 1 | **0,456 ms** |
| erzwungen *(Diagnose)* | `MessagePropertyNameValueIDX` | 1 | **0,411 ms** |

**Der Unterschied ist 45 Mikrosekunden und liegt innerhalb der Streuung der einzelnen Läufe**
(0,411 bis 0,806 ms über fünf). Bei einem eindeutigen Wert ist die Indexwahl gleichgültig, weil
beide Wege genau eine Zeile finden.

### Befund 2 — bei `Service.Type` gibt der Optimizer den Index **ganz auf**

`type = ALL`, `key = NULL`, `rows = 46.964.279`: **ein voller Tabellenscan über
`MessageProperty`.** Er kostet **81,4 Sekunden** — das **Achtfache** der Zeitgrenze des Lese-Pools.

**Und die `rows`-Zahl ist genau die falsche Statistik aus M155:** Der Optimizer scannt eine
Tabelle, die er für 46,96 Millionen Zeilen hält, während sie 75,57 Millionen trägt. **Der
tatsächliche Scan ist um 60,9 % teurer als der, den er kalkuliert hat.**

**Die Diagnose — und sie ist ausdrücklich eine Diagnose und keine Bauempfehlung:**

| `Service.Type`, häufigster Wert | Zugriff | `rows` | beste von *drei* |
|---|---|---:|---:|
| **gewählt** | voller Tabellenscan | 46.964.279 | **81.429,454 ms** |
| erzwungen *(Diagnose)* | `ref` über `MessagePropertyNameValueIDX` | 9.262.588 | **27.892,210 ms** |
| Faktor | | | **2,92** |

> ⚠️ **`FORCE INDEX` ist hier Diagnose und keine Bauempfehlung.** Es zeigt, dass der Optimizer den
> billigeren Weg übersieht — und **es löst nichts**: 27,9 Sekunden sind das **2,8‑Fache** der
> Zehn-Sekunden-Grenze des Lese-Pools. Der Fall ist mit keiner Indexwahl tragbar; er ist erst mit
> einem Zeitfenster tragbar, und auch dann nicht immer (M159). **`STRAIGHT_JOIN` ist in diesem
> Projekt schon einmal als Allheilmittel missverstanden worden** ([`bam-suche.md`](bam-suche.md)
> §4); `FORCE INDEX` soll nicht der zweite Fall werden.
>
> *Abweichung: Für diesen Fall stehen Aufwärmlauf und **zwei** Läufe statt fünf. Sechs Läufe à 80
> Sekunden hätten die geteilte Testkopie acht Minuten belegt. Die drei Werte liegen mit 27.892,210,
> 27.901,920 und 28.232,718 ms um 1,2 % auseinander; eine engere Schranke änderte an der Aussage
> nichts.*

### Befund 3 — die Selektivität kommt vom Wert, und der Name trägt nichts bei

Die Laufzeiten desselben Namens unterscheiden sich um bis zu **Faktor 742** (`Message.ReceiverID`:
0,810 gegen 601,269 ms), je nachdem, welcher Wert gesucht wird. **Zwischen den Namen liegt keine
vergleichbare Ordnung.** Ein Wertprädikat ist genau so teuer, wie sein Wert häufig ist — und die
Kardinalität 18 aus M155 sorgt dafür, dass der Optimizer das vorher nie richtig schätzt.

*Belegvermerk (L10): gemessen sind Plan und Laufzeit für acht Wertprädikate ohne Join am
07.09.2026. Behauptet wird nicht, dass der Optimizer bei anderen Werten dieselbe Wahl trifft — die
Wahl hängt an seiner Schätzung, und die ist nachweislich falsch.*

---

## M159 — Der ganze Weg

**Sitzung** `s14-m159.sql`. Wertprädikat + Verdichtung auf `MessageID` + Join auf `Message` +
Mandantenkette + Zeitfenster, **in der Bauform aus [`bam-suche.md`](bam-suche.md) §4**: erst
deckeln, dann beschriften.

```sql
SELECT COUNT(*), MAX(CHAR_LENGTH(p2.ProcessName)), MAX(CHAR_LENGTH(prj.ProjectName))
FROM (SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID
        FROM GlassfishDB.MessageProperty mp
        JOIN GlassfishDB.Message m ON m.MessageID = mp.MessageID
       WHERE mp.MessagePropertyName = ? AND mp.MessagePropertyValue = ?
         AND m.MessageLastUpdate >= ? AND m.MessageLastUpdate < '2025-12-30 00:00:00'
         AND EXISTS (SELECT 1 FROM GlassfishDB.Process pr
                       JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
                      WHERE pr.ProcessID = m.ProcessID AND pm.MandantID = ?)
       GROUP BY m.MessageID
       ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC LIMIT 51) AS treffer
LEFT JOIN GlassfishDB.Process p2 ON p2.ProcessID = treffer.ProcessID
LEFT JOIN GlassfishDB.Project prj ON prj.ProjectID = p2.ProjectID
```

*Zwei Abweichungen von der Vorlage, beide ohne Wirkung auf den Plan der abgeleiteten Tabelle und
beide wegen G1: Die äußere Auswahl gibt **Längen statt Namen** aus, damit kein Partnername in die
Rohausgabe gerät und die beiden `LEFT JOIN` trotzdem ausgeführt werden; und das äußere `ORDER BY`
über 51 bereits sortierte Zeilen entfällt. `SOS` und `SOSAction` sind nicht mitgemessen — sie
hängen wie `Process` und `Project` als `eq_ref` über der Deckelung.*

**Der Prüfwert ist je Fall der häufigste Wert des 30-Tage-Fensters** — der **Bösfall**, und derselbe
über alle drei Fenster desselben Mandanten.

> **Die Grenze dieser Messung ist 10 Sekunden — die des Lese-Pools**
> ([`datenzugriff.md`](datenzugriff.md) §1), nicht die 60 der übrigen Sitzungen. **Ein Abbruch ist
> damit unmittelbar die Aussage „so scheitert es in der Anwendung".** Die Sitzung lief mit
> `--force`, damit ein Abbruch die folgenden Fälle nicht mitnimmt.

> **Vorregistrierte Deutung.** Unter **500 ms** im 30-Tage-Fenster (Budget aus §8 der
> Projektbeschreibung). Die 90 Tage sind die Grenze, die der Freitextfilter der Liste trägt — sie
> ist hier **nicht** gesetzt, sondern wird gemessen.

**`EXPLAIN` — für alle 15 Fälle dieselbe Gestalt:** `<derived2>` als `ALL` (die höchstens 51
gedeckelten Zeilen), darüber `p2` und `prj` je `eq_ref` über `PRIMARY`. **Die Beschriftung hängt
über der Deckelung und fasst nie mehr als 51 Zeilen an** — der Befund aus M47 trägt hier
unverändert.

### Ergebnis — beste von fünf nach einem Aufwärmlauf, in Millisekunden

**`NEXANS`**

| Name | 24 Stunden | **30 Tage** | 90 Tage | Zeilen |
|---|---:|---:|---:|---:|
| `Message.GUID` | 1,062 | **0,942** | 0,946 | 0 / 1 / 1 |
| `Message.ReceiverID` | 94,972 | **1.222,763** | 1.276,108 | 51 / 51 / 51 |
| `Service.Type` | 136,354 | **⛔ Abbruch bei 10 s** | **⛔ Abbruch bei 10 s** | 51 / — / — |

**`SUTTONS`**

| Name | 24 Stunden | **30 Tage** | 90 Tage | Zeilen |
|---|---:|---:|---:|---:|
| `Message.GUID` | 0,876 | **0,901** | 0,898 | 0 / 1 / 1 |
| `Message.ReceiverID` | *entfällt* | *entfällt* | *entfällt* | keine Zeile im Bestand (M156) |
| `Service.Type` | 70,665 | **1.396,667** | 3.725,905 | 51 / 51 / 51 |

*Die 24-Stunden-Zeile von `Message.GUID` hat **null** Treffer: Der Bösfall-Wert stammt aus dem
30-Tage-Fenster und liegt nicht im letzten Tag. Sie misst damit den Leerlauf des Zugriffs, nicht
einen Treffer — und der kostet dasselbe.*

### Die Deutung ist in einem von drei Fällen getroffen

| Fall | 30 Tage | gegen 500 ms |
|---|---:|---|
| `Message.GUID` (`NEXANS` / `SUTTONS`) | 0,942 / 0,901 ms | **getroffen**, Faktor 530 beziehungsweise 555 darunter |
| `Message.ReceiverID` (`NEXANS`) | 1.222,763 ms | **gerissen**, Faktor 2,45 darüber |
| `Service.Type` (`NEXANS`) | **Abbruch bei 10.005 ms** | **gerissen**, mindestens Faktor 20 darüber |
| `Service.Type` (`SUTTONS`) | 1.396,667 ms | **gerissen**, Faktor 2,79 darüber |

**Die 500-ms-Zusage hält genau für die Namen, deren Wert eindeutig ist.** Sobald ein Wert
zehntausende Nachrichten trägt, hilft weder die Deckelung auf 51 noch das Zeitfenster: Das
`ORDER BY MessageLastUpdate DESC` zwingt dazu, **alle** Kandidaten zu finden, bevor die ersten 51
feststehen. Die Deckelung schützt die Beschriftung, nicht den Kern — genau wie in der BAM-Suche,
nur dass dort der schlimmste Wert 234.159 Zeilen trägt und hier 4.366.602.

### Die 90 Tage, die nicht gesetzt waren

Sie sind gemessen worden, um zu sehen, ob die Grenze des Freitextfilters hier passt. **Sie passt
nicht:**

| Fall | 30 Tage | 90 Tage | Aufschlag |
|---|---:|---:|---:|
| `Message.GUID`, `NEXANS` | 0,942 | 0,946 | **+0,4 %** |
| `Message.ReceiverID`, `NEXANS` | 1.222,763 | 1.276,108 | +4,4 % |
| `Service.Type`, `SUTTONS` | 1.396,667 | 3.725,905 | **+167 %** |
| `Service.Type`, `NEXANS` | Abbruch | Abbruch | — |

**Bei einem eindeutigen Wert ist das Fenster gleichgültig** — der Zugriff findet eine Zeile,
gleich wie weit er zurückschaut. **Bei einem häufigen Wert kostet das dreifache Fenster fast das
Dreifache**, weil genau dreimal so viele Kandidaten zu sortieren sind. **Eine einheitliche
Fenstergrenze ist deshalb das falsche Werkzeug**: Sie wäre für die Schlüssel-Namen unnötig eng und
für `Service.Type` immer noch zu weit.

### Zwei Fälle, die in der Anwendung nicht existieren

`Service.Type` bei `NEXANS` über 30 und über 90 Tage ist **sechsmal** an der Zehn-Sekunden-Grenze
abgebrochen, jeder Lauf zwischen **10.004,576 und 10.007,815 ms**. **Das ist kein Messfehler und wird nicht durch
ein kleineres Fenster ersetzt.** Über 24 Stunden läuft derselbe Fall in 136,354 ms — die Grenze
liegt also **zwischen einem Tag und dreißig**, und wo genau, ist in dieser Runde nicht erhoben.
**Offener Punkt 145.**

*Belegvermerk (L10): gemessen sind 15 Fälle über zwei Mandanten, drei Fenster und drei Namen, jeder
mit dem häufigsten Wert seines 30-Tage-Fensters. Behauptet wird nicht, dass ein anderer Wert
dieselben Zeiten trägt — M158 zeigt Faktor 742 zwischen dem häufigsten und dem seltensten Wert
desselben Namens. Die hier gemessenen Zahlen sind **Bösfälle**, nicht Durchschnitte.*

---

## M160 — Werte über die Präfixgrenze hinaus

**Sitzungen** `s12-m157b-m160.sql`, Gegenprobe in `s15-m158d-m160g.sql`.

> **Vorregistrierte Deutung.** Die Namen dieser Tabelle tragen Kennungen und Nummern, also
> überwiegend kurze Werte. Trifft das nicht zu, ist der Präfixindex für den betroffenen Namen ein
> **Vorfilter** und kein Zugriffspfad — mit Folgen für M158 und M159.

**Die Deutung hat getroffen, und zwar vollständig.** Fenster B, `NEXANS`:

| Name | Zeilen | **länger als 50 Zeichen** | Wert `NULL` | kürzeste | längste | mittlere Länge |
|---|---:|---:|---:|---:|---:|---:|
| `Converter.TransactionID` | 239.533 | **0** | 0 | 2 | 7 | 6,21 |
| `Message.GUID` | 180.251 | **0** | 0 | **36** | **36** | **36,00** |
| `Message.ReceiverID` | 46.986 | **0** | 0 | **10** | **10** | **10,00** |
| `Service.Type` | 532.215 | **0** | 0 | 8 | 13 | 9,35 |

**Kein einziger Wert der vier Typ‑1‑Namen überschreitet die Präfixgrenze von 50 Zeichen.** Zwei von
ihnen haben eine **feste** Länge: `Message.GUID` immer 36 Zeichen (die Gestalt einer UUID),
`Message.ReceiverID` immer 10.

**Und `MessagePropertyValue` ist bei diesen vier Namen nirgends `NULL`** — obwohl es die einzige
`NULL`-fähige Spalte der Tabelle ist (M44).

### Die Gegenprobe — der Ausdruck greift, er findet hier nur nichts

Ein Prädikat, das über 998.985 Zeilen kein einziges Mal wahr wird, muss zeigen, dass es überhaupt
wahr werden kann. Dieselbe Messung über **alle** Namen, 24 Stunden, `NEXANS`:

| | |
|---|---:|
| Zeilen | 111.092 |
| verschiedene Namen darin | **77** |
| **länger als 50 Zeichen** | **35.325** (31,8 %) |
| länger als 200 Zeichen | 6 |
| **längster Wert** | **2.124 Zeichen** |

**Fast ein Drittel aller `MessageProperty`-Zeilen trägt einen Wert über der Präfixgrenze — nur
keine der vier konfigurierten.** Der Ausdruck greift also; die vier Nullen oben sind eine
Eigenschaft der Daten und kein blinder Messfehler.

### Was die Nachprüfung auf der Zeile kostet — **nicht messbar, und das ist die Antwort**

Der Auftrag fragt, was die Nachprüfung eines Werts über 50 Zeichen gegenüber einem Wert kostet, der
in den Präfix passt. **Diese Frage hat auf den Typ‑1‑Namen keinen Gegenstand:** Es gibt unter ihnen
keinen einzigen Wert über 50 Zeichen, also auch kein Paar, das sich vergleichen ließe. **Die Lücke
wird benannt und nicht mit einem Ersatzfall gefüllt** — ein Vergleich über einen fremden, nicht
konfigurierten Namen beantwortete eine andere Frage.

**Was daraus folgt, ist trotzdem eindeutig.** Für die vier Namen ist
`MessagePropertyNameValueIDX` ein **echter Zugriffspfad** und kein Vorfilter: Der Präfix von 50
Zeichen enthält den vollständigen Wert, und `Using index condition` in allen M158-Plänen
bestätigt, dass die Bedingung im Index ausgewertet wird. **Die Kosten aus M158 und M159 sind damit
nicht durch Nachprüfungen auf der Zeile erklärbar** — sie kommen allein aus der Zahl der Treffer.

> **Für einen künftigen Eintrag gilt das nicht.** Trägt ein neu konfigurierter Name Werte über 50
> Zeichen — und 31,8 % der Zeilen im Bestand tun das —, dann wird derselbe Index für ihn zum
> Vorfilter, und die Kosten aus M158 und M159 sind für ihn nicht übertragbar. **Offener Punkt 146.**

*Belegvermerk (L10): gemessen sind Längenverteilung und `NULL`-Anteil der vier Typ‑1‑Namen über
Fenster B sowie die Gegenprobe über alle 77 im 24-Stunden-Fenster vorkommenden Namen. Behauptet
wird nicht, dass kein Wert dieser vier Namen irgendwo im Gesamtbestand länger als 50 Zeichen ist —
gemessen ist das Fenster, nicht der Bestand.*

---

# Befunde

**Die vorregistrierten Deutungen, unverändert dagegengehalten:**

| Messung | Deutung | Ausgang |
|---|---|---|
| **M153** | Drei Spalten, keine Beschreibungs- oder Sortierspalte | **getroffen** |
| **M154** | Typ ∈ {0, 1}; `NULL` heißt „alle Mandanten" | **erste Hälfte getroffen**, zweite **nicht belegbar** — Typ und Mandant fallen zusammen |
| **M155** | Typ 1 kommt vor, Typ 0 nicht | **getroffen**, 12 von 12, kein Gegenfall |
| **M156** | Unter 20 % Deckung über die Wurzeln ist fragwürdig | **greift bei einem Namen** (12,803 %) |
| **M157** | Häufigster Wert über 234.159 Zeilen ist nicht anbietbar | **gerissen**, Faktor 3,03 |
| **M158** | `ref` über `MessagePropertyNameValueIDX` | **in zwei von vier Namen getroffen** |
| **M159** | Unter 500 ms im 30-Tage-Fenster | **in einem von drei Namen getroffen** |
| **M160** | Überwiegend kurze Werte | **getroffen**, 0 von 998.985 Zeilen über 50 Zeichen (239.533 + 180.251 + 46.986 + 532.215) |

## 1. Die Entscheidungen halten — bis auf eine, die neu vorgelegt gehört

| Entscheidung | Stand nach dieser Runde |
|---|---|
| **E‑101** (Trennung Typ 0 / Typ 1) | **belegt.** M155 findet die Trennung in den Daten wieder, ohne Gegenfall |
| **E‑105** (technische Namen unverändert) | **Voraussetzung bestätigt.** M153 zeigt: Es *gibt* keine Klartextspalte, die man zeigen könnte |
| **E‑99** (zwei Quellen im Angebot) | **trägt technisch, wird aber ihrem Zweck nicht gerecht.** Für neun von zehn Mandanten enthält die zweite Quelle **null** Typ‑1‑Namen (M154), während drei davon bei `SUTTONS` auf **jeder** Nachricht belegt sind (M156) |
| **E‑100** (Feldname ist Pflicht) | unberührt; diese Runde misst nichts dazu |
| **E‑102** (Ausnahmekasten zu L4) | **die Begründung ist deutlich härter ausgefallen als erwartet** — siehe Befund 3 |
| **E‑103**, **E‑104** (Absprung) | unberührt; diese Runde misst nichts dazu |
| **MVP-Umfang** | **Das befürchtete Risiko ist nicht eingetreten, ein anderes dafür** — siehe Befund 2 |

## 2. Das MVP-Risiko hat sich umgedreht

Die MVP-Zusage stand unter dem Vorbehalt, die Deckung je Feldname könne ausfallen wie bei den
BAM-Spalten (M11: auf 98,93 % der Zeilen leer). **Sie fällt gegenteilig aus:** drei von vier Namen
auf **100 %** der Nachrichten, bei beiden gemessenen Mandanten.

**Aber die vier Namen sind nicht vier Suchfelder, sondern drei verschiedene Dinge:**

| Klasse | Name | Deckung (Wurzeln, `NEXANS`) | häufigster Wert | 30 Tage durch den ganzen Weg |
|---|---|---:|---:|---:|
| **Schlüssel** | `Message.GUID` | 100,000 % | **1 Zeile** | **0,942 ms** |
| **Schlüssel** | `Converter.TransactionID` | 100,000 % | 2 Zeilen | *nicht gemessen* |
| **Merkmal** | `Message.ReceiverID` | **12,803 %** | 5.176 Zeilen | 1.222,763 ms |
| **Kategorie** | `Service.Type` | 100,000 % | **≥ 708.893 Zeilen** | **⛔ Abbruch bei 10 s** |

**Von vier angebotenen Feldern ist eines uneingeschränkt tauglich, eines vermutlich tauglich, eines
fragwürdig und eines unbrauchbar.** Die Zusage „Property-Suche im MVP" ist damit nicht widerlegt —
`Message.GUID` allein trägt sie —, aber sie ist kleiner als das Angebot, das E‑99 beschreibt.

## 3. Regel L4 ist härter begründet, als ihr Text vermuten lässt

Der Regeltext sagt: nie filtern, gruppieren oder sortieren über den Wert. **Diese Runde beziffert,
was passiert, wenn man es doch tut:**

| Zugriff | Kosten | gegen die 10 s des Lese-Pools |
|---|---:|---|
| `COUNT(*)` über **einen Namen**, Gesamtbestand (`Service.Type`) | **125,527 s** | **12,6‑fach** |
| Wertprädikat auf den häufigsten Wert, ohne Join (`Service.Type`) | **81,429 s** | **8,1‑fach** |
| dasselbe mit erzwungenem Index *(Diagnose)* | 27,892 s | 2,8‑fach |
| **derselbe Weg mit Zeitfenster und Deckelung, 30 Tage** | **⛔ Abbruch** | — |
| **derselbe Weg mit eindeutigem Wert, 30 Tage** | **0,942 ms** | **0,009 %** der Grenze |

**Zwischen dem besten und dem schlechtesten Fall derselben Bauform liegt mindestens Faktor 10.600** — und der
Unterschied ist **nicht** der Name, nicht das Fenster und nicht der Plan, sondern **wie viele
Zeilen der gesuchte Wert trägt**. Ein Ausnahmekasten nach E‑102 muss deshalb an dieser Größe
hängen und nicht am Feldnamen.

## 4. Der Optimizer rechnet mit einer Tabelle, die es nicht gibt

| Größe | Optimizer | gemessen | Abweichung |
|---|---:|---:|---|
| Zeilen `MessageProperty` | 46.964.279 | **75.571.462** (M44) | **37,9 % zu niedrig** |
| verschiedene `MessagePropertyName` | 18 | **101** an einem Tag (M17‑2) | **Faktor 5,6 zu selektiv** |

**Beide Fehler zeigen in dieselbe Richtung: Ein Zugriff über den Namen sieht im Plan billiger aus,
als er ist.** Die 46.964.279 sind zugleich die Zahl, die bis zum 12.08.2026 als „gemessen" in der
Projektbeschreibung stand und dort im Kasten zu §8 korrigiert ist — **aus der Dokumentation
verschwunden und im Optimizer geblieben.** Sie erscheint in M158 wörtlich als `rows` des
Tabellenscans.

## 5. Die Konfigurationstabelle ist ärmer als `MessageBAMMandant`, und in einem Punkt anders gebaut

| | `MessageBAMMandant` | `MessagePropertySearchListEntry` |
|---|---|---|
| Mandant im Schlüssel | **ja** | **nein** — Primärschlüssel ist der Name allein |
| Bezeichnung | über `MessageBAMType` | **keine** |
| Sortierung | `MessageBAMTypeSortIndex` | **keine** |
| Zeilen (Testkopie) | 131 über zwei Tabellen | **12** |

**Ein Feldname existiert genau einmal.** Denselben Namen für zwei Mandanten verschieden zu
konfigurieren ist in dieser Tabelle nicht darstellbar. Das Angebot aus dieser Quelle kommt
**ohne Beschriftung und ohne Ordnung** — E‑105 ist damit weniger eine Wahl als eine Feststellung.

## 6. Zwei Befunde, die in keine vorformulierte Zeile passten

**Erstens: die 30-Tage-Fenster dieser Runde und der Vorrunde sind dasselbe Fenster.** Ohne Absicht
trifft `2025-11-30` bis `2025-12-30` Ziffer für Ziffer das **Fenster B** aus M28‑1 und M28‑2 —
214.330 Nachrichten, bei `NEXANS` 28.524 Wurzeln und 101.270 Kinder. Die 20‑%-Schwelle aus M28‑2
ist dadurch nicht nur der Zahl nach, sondern **über dieselbe Grundmenge** vergleichbar. Das ist
Glück und keine Leistung, aber es macht den Vergleich belastbar.

**Zweitens: `Source` und `Target` in `Message` sind `bit(1)`, nicht `varchar`.** Das Prädikat
`Source <> ''` — naheliegend, wenn man „ist gesetzt" aus `verkettung.md` §5 wörtlich nimmt —
liefert auf `bit(1)` zufällig dasselbe wie `Source = 1`, weil der Leerstring im Zahlenkontext zu 0
wird. **Gegengeprüft in M156‑0** (beide 28.524). Wer das Prädikat einmal auf eine `varchar`-Spalte
überträgt, bekommt eine andere Bedeutung, ohne dass etwas bricht.

---

# Was diese Runde nicht zeigt

1. **Nichts über die Produktion.** Alle Zahlen stammen von der Testkopie
   (`192.168.11.148`, `@@global.read_only = 1`). Der Katalog der Testkopie ist nachweislich
   kuratiert und ändert sich zwischen Läufen. **Es ist nicht hochgerechnet worden.**

2. **Den häufigsten Wert über den Gesamtbestand.** Er ist nicht messbar — schon das Zählen eines
   Namens kostet bis zu 125 Sekunden, die Gruppierung über die Werte ist um Größenordnungen
   teurer. Alle Werteverteilungen sind **untere Schranken** aus einem Fenster.

3. **Die Bedeutung von `MandantID IS NULL`.** Typ und Mandant fallen in den Daten vollständig
   zusammen (8 × Typ 0 ohne Mandant, 4 × Typ 1 mit `NEXANS`). „Gilt für alle" und „keinem
   zugeordnet" sind mit derselben Verteilung verträglich. **Das entscheidet nur, wer das
   Altwerkzeug oder seinen Quelltext liest** — nicht diese Messung.

4. **Ob ein Typ‑0‑Name die ihm zugeordnete Spalte meint.** Gemessen ist, dass die acht vermuteten
   Spalten existieren. Fünf von acht Namen weichen von ihrem Spaltennamen ab. **Die Zuordnung ist
   eine Vermutung und als solche gekennzeichnet.**

5. **Die Kosten der Nachprüfung auf der Zeile.** Es gibt unter den vier Namen keinen Wert über 50
   Zeichen, also keinen Vergleichsfall. **Die Lücke ist nicht mit einem Ersatzfall gefüllt worden.**

6. **`Converter.TransactionID` durch den ganzen Weg.** M159 misst drei der vier Namen. Nach M157
   ist er ein Schlüssel wie `Message.GUID` (häufigster Wert 2 Zeilen), aber **gemessen ist er
   nicht**.

7. **Wo zwischen einem Tag und dreißig die Grenze liegt.** `Service.Type` bei `NEXANS` läuft über
   24 Stunden in 136 ms und bricht über 30 Tage ab. Der Punkt dazwischen ist nicht erhoben.

8. **Nichts über die Oberfläche, den Endpunkt oder die Antwortform.** Diese Runde hat nichts
   gebaut und nichts entschieden.

9. **Nichts über die acht übrigen Mandanten.** Gemessen sind `NEXANS` und `SUTTONS` (Regel L7).
   Die Verteilung in M154 läuft über alle zehn, die Deckung und die Laufzeiten nicht.

---

# Offene Punkte

Fortlaufend in der Reihe des Projekts; höchste vorher vergebene Nummer ist **141**
([`process-view.md`](process-view.md) §45).

**142. Alle vier Typ‑1‑Namen sind ausschließlich für `NEXANS` konfiguriert.** Für die übrigen neun
Mandanten enthält die zweite Quelle des Angebots (E‑99) **keine einzige** echte Property. Ob das
eine Kuratierungslage der Testkopie ist oder in der Produktion ebenso steht, ist offen — und es
entscheidet, ob E‑99 für neun von zehn Mandanten ein leeres Versprechen ist.

**143. Die Typ‑0‑Namen sind keine ableitbaren Spaltennamen.** Nur drei von acht sind wörtlich; zwei
stellen die Wortteile um, einer heißt anders, zwei wohnen trotz `Message.`-Präfix in `Process`
beziehungsweise `SOS`. E‑101 braucht eine **Abbildung im Code**, und jeder künftige Typ‑0‑Eintrag
fällt aus ihr heraus, bis jemand sie ergänzt. Was der Endpunkt mit einem unbekannten Typ‑0‑Namen
tut, ist zu entscheiden.

**144. Die Konfigurationstabelle beschreibt die Daten nicht.** Bei `SUTTONS` sind drei der vier
Namen auf **100 %** der Nachrichten belegt, und keiner ist dort konfiguriert. Bildet die Tabelle
eine bewusste Freischaltung ab oder eine Lücke? Vom Ergebnis hängt ab, ob das Angebot der
Konfiguration folgt oder den Daten.

**145. `Service.Type` bricht zwischen einem Tag und dreißig ab.** 136,354 ms über 24 Stunden,
Abbruch an der Zehn-Sekunden-Grenze über 30 Tage. Wo die Grenze liegt, ist nicht erhoben — und
davon hängt ab, ob ein Fenster als Schutz überhaupt taugt.

**146. Ein künftiger Feldname mit langen Werten trägt die Kosten dieser Runde nicht.** Für die vier
gemessenen Namen ist `MessagePropertyNameValueIDX` ein echter Zugriffspfad, weil kein Wert 50
Zeichen überschreitet. **31,8 % aller `MessageProperty`-Zeilen tun das aber.** Für einen solchen
Namen würde derselbe Index zum Vorfilter, und M158 und M159 wären für ihn nicht übertragbar.

**147. Das Angebot aus dieser Quelle hat weder Beschriftung noch Ordnung.** `MessageBAMMandant`
liefert `MessageBAMTypeDescription` und `MessageBAMTypeSortIndex`; hier gibt es beides nicht. In
welcher Reihenfolge die Feldnamen erscheinen und ob die beiden Quellen in einer gemeinsamen Liste
stehen können, ist offen.

**148. `Service.Type` ist eine Kategorie und kein Suchschlüssel.** 18 Werte über 30 wie über 90
Tage, auf jeder Nachricht belegt. Ihn als Suchfeld anzubieten heißt, dem Nutzer eine Abfrage
anzubieten, die entweder abbricht oder einen großen Teil seines Bestands zurückgibt. Ob er aus dem
Angebot fällt, ob eine Grenze an der Trefferzahl greift oder ob er als Filter statt als Suchfeld
gehört, ist zu entscheiden — **nicht in dieser Runde.**

**149. Die Statistik des Optimizers ist um 37,9 % veraltet.** Ob ein `ANALYZE TABLE` auf
`MessageProperty` die Planwahl in M158 ändert, ist nicht geprüft — es wäre ein Schreibzugriff auf
`GlassfishDB` und in dieser Runde ausgeschlossen. Der Punkt gehört dem Betrieb, nicht diesem
Werkzeug, aber er erklärt einen Teil der gemessenen Kosten.

---

# Regelbezug

| Regel | Stand | Begründung |
|---|---|---|
| **G1** — Geheimhaltung | **erfüllt** | Kein `MessagePropertyValue`, keine `MessageID`, keine Belegnummer, kein Partner- oder Hostname in dieser Datei. Prüfwerte sind im Statement über `QUOTE()` und `PREPARE` eingesetzt und haben kein Skript berührt; ausgegeben ist nur ihre Zeichenlänge. In M159 stehen **Längen statt Namen** für `ProcessName` und `ProjectName`. **Feldnamen sind Konfiguration und dürfen stehen** — sie stehen. Die Rohausgaben liegen unter `scripts/messung-property-suche/ergebnis/` und sind über `.gitignore` ausgeschlossen |
| **Q4** — kein unbelegter Wert | **erfüllt** | Jede Zahl stammt aus einem Lauf dieser Runde oder ist mit ihrer Quelle benannt (M0, M14, M17‑2, M28‑2, M33, M44). Die drei nicht messbaren Größen stehen unter „Was diese Runde nicht zeigt" |
| **L1** — Pflicht-Zeitfenster | **berührt, nicht verletzt** | Diese Runde baut keinen Endpunkt. Jedes Fenster ist absolut, endet am 2025‑12‑30 und ist je Messung genannt. M157 misst ausdrücklich **ohne** Fenster über den Gesamtbestand — als Erhebung, und das Ergebnis ist, dass es dort nicht geht |
| **L4** — `MessageProperty` nur über `MessageID` | **eingehalten in M155 und M156, bewusst verletzt in M157 bis M160** | M155 und M156 erreichen die Tabelle ausschließlich über die `MessageID` (`ref` über `PRIMARY`, `Using index`). M157 bis M160 steigen über Name und Wert ein — **das ist der Gegenstand der Messung** und die Grundlage, auf der E‑102 seinen Ausnahmekasten bekommen soll. Die Kosten sind beziffert |
| **L7** — mindestens zwei Mandanten, einer klein | **erfüllt** | `NEXANS` (180.251 Nachrichten im Fenster) und `SUTTONS` (21.516). M154 läuft über alle zehn |
| **L10** — Belegvermerk | **erfüllt** | Jede Messung schließt mit *gemessen war X / behauptet wird Y*, und die Lücke ist benannt |
| **L15** — `EXPLAIN` zu jedem Statement | **erfüllt** | Plan zu M155, M156 (beide Fassungen, beide Mandanten), M157 (beide Fassungen), M158 (acht Fälle plus zwei Diagnosen), M159 (Gestalt für alle 15 Fälle) |
| **M1** — keine Mandanten-ID aus einer Anfrage | **nicht berührt** | Es gibt in dieser Runde keine Anfrage und keinen Endpunkt. Die `MandantID` steht als Messparameter im Skript; die Ausnahmeliste in [`mandantentrennung.md`](mandantentrennung.md) §3 bleibt bei zwei Einträgen und wächst hier nicht |
| **T1** — keine Wanduhrzeit in Tests | **nicht berührt** | Diese Runde baut keine Tests |
| **S1** — nur `SELECT` | **erfüllt** | Alle elf Sitzungen fahren ausschließlich `SELECT`, `SET`, `SHOW`, `EXPLAIN`, `PREPARE`/`EXECUTE`/`DEALLOCATE` mit `monitor_read`. **Kein Schreibzugriff auf `GlassfishDB`**, kein Zugriff auf `overlord_monitor`. `@@global.read_only` = **1** als erste Abfrage jeder Sitzung |

## Was diese Runde entgegen §6 des Auftrags angefasst hat — beides gemeldet

§6 verlangt: keine Änderung an bestehenden Dateien. **Zwei Dateien sind trotzdem geändert, und
beide Male aus einer Vorschrift, die dem Auftrag vorgeht:**

1. **`.gitignore`** — eine Zeile für `scripts/messung-property-suche/ergebnis/`. Ohne sie lägen
   Rohausgaben mit `MessagePropertyValue` im Repository, und **G1 wiegt schwerer als §6**. Die
   Konvention ist seit Schritt 8 dieselbe.
2. **`docs/README.md`** — der Eintrag für diese neue Datei. [`CLAUDE.md`](../CLAUDE.md) führt ihn
   unter „Dokumentationspflicht" als Bedingung dafür, dass ein Schritt überhaupt fertig ist; nach
   §0 des Auftrags gilt bei Widerspruch die Datei.

**[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) ist nicht angefasst** — weder Abschnitt 9 noch
der Regeltext L4 noch ein Ausnahmekasten. Das gehört in den Bauauftrag.

## Die Sitzungen dieser Runde

| Datei | Inhalt | Ausgang |
|---|---|---|
| `s0-rahmen.sql` | Rahmen, Serverangaben, Datenstand | — |
| `s1-m153.sql` | M153 Aufbau der Tabelle | — |
| `s2-m154.sql` | M154 Verteilung | — |
| `s3-m155.sql`, `s3b-nachtrag.sql` | M155 Typ-Lesart, Laufzeit je Lauf, Optimizer-Statistik | — |
| `s4-vorprobe.sql` | Fenstergröße, Rollen | — |
| `s5-m156-nexans.sql` | M156 **Fassung A**, vier `EXISTS` | **Abbruch bei 60 s** |
| `s6-m156-fassungB.sql`, `s7-m156-nexans.sql` | M156 Fassung B, beide Mandanten | — |
| `s8-m157-vorprobe.sql` | M157 vier Namen als `IN`-Liste | **Abbruch bei 120 s** |
| `s9-m157a.sql`, `s10-m157-zaehlung.sql` | M157 Zählung je Name einzeln | — |
| `s11-m157-verteilung.sql`, `s12-m157b-m160.sql` | M157 Verteilung, 90-Tage-Schranke, M160 Längen | — |
| `s13-m158.sql` | M158 acht Wertprädikate | — |
| `s14-m159.sql` | M159 15 Fälle, Grenze 10 s, `--force` | **12 Abbrüche in 2 Fällen** |
| `s15-m158d-m160g.sql` | M158 `FORCE INDEX`-Diagnose, M160 Gegenprobe | — |

**Serverzeit Beginn `2026-09-07 16:11:14`.** Die Ergebnisdateien liegen unter
`scripts/messung-property-suche/ergebnis/` und sind nicht eingecheckt (G1).

---

# Nachtrag vom 08.09.2026 — die drei neuen Namen (M161 bis M167)

Erhoben am **08.09.2026** gegen dieselbe Testkopie. Auftrag: „Nachtrag zur Messrunde Property-Suche —
die drei neuen Namen", Stand 08.09.2026. **Ein Nachtrag, keine neue Runde:** Er hängt an dieser
Datei, vergibt keine E‑Nummern und wiederholt keine Messung zu `Message.GUID`, `Message.ReceiverID`
und `Service.Type` — deren Befunde stehen oben und werden nur zitiert.

**Dieser Nachtrag baut nichts und entscheidet nichts.** Kein Endpunkt, keine Abbildung im Code, kein
Test, keine Migration. [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) ist nicht angefasst, auch
nicht §3.2, obwohl `Message.DestinationFilename` dort fehlt — das gehört in den Bauauftrag.

**Anlass.** Die Konfigurationstabelle `MessagePropertySearchListEntry` der Testkopie ist am
08.09.2026 **von Hand** auf den Stand der Produktion gebracht worden — nur diese eine Tabelle. Der
Datenbestand ist unverändert (Datenstand von `Message` in N0 identisch mit M0); Anker, Zeitfenster
und Entwicklungsuhr gelten wie bisher. Was in der Tabelle tatsächlich steht, stellt **M161** fest,
bevor gemessen wird.

## ⚠️ Gemeldete Abweichungen und Widersprüche des Nachtrags

### Widerspruch 1 — `Message.DestinationFilename` steht in drei Projektdateien

Der Auftrag führt den Namen (§2 und M162) als „steht in **keiner** Projektdatei" und leitet daraus
ab, dass es für ihn keine Erwartung gibt. **Das trifft nicht zu:**

| Datei | Stelle | Was dort steht |
|---|---|---|
| [`messungen-schritt5.md`](messungen-schritt5.md) | M17 (2), Namen in Fenster A | **480** Zeilen auf 480 Nachrichten, Längen **3 bis 73** Zeichen, Mittel 39; und in der Liste der 17 `Message.*`-Namen, „die in keiner Dokumentationsdatei stehen" |
| [`messungen-schritt8.md`](messungen-schritt8.md) | M56, Ergebnis (a), Fenster A und B | Fenster B: **20.765** Zeilen auf 20.765 Nachrichten, kürzester **3**, längster **73** Zeichen, 166 rein numerisch |
| [`messungen-schritt8-auftrag.md`](messungen-schritt8-auftrag.md) | „Der Befund, der die Schnittregel entscheidet" | als Beispiel eines Werts, der aus der EDI-Datei und damit **vom Partner** kommt |

Nach §0 des Auftrags gilt die Datei. **Folge:** Für M164 gibt es Vorwissen — ein gemessenes Maximum
von 73 Zeichen über Fenster B, also über der Präfixgrenze; der **Anteil** über 50 ist dort nicht
erhoben und bleibt Gegenstand von M164. Für M162 bleibt es bei „keine Erwartung zur Deckung je
Kettenstellung": M56 kennt 20.765 Zeilen gegen 214.330 Nachrichten in Fenster B über **alle**
Mandanten (**9,688 %**), ohne Mandanten- und Rollentrennung. Was in keiner Datei steht, ist die
**Bedeutung** des Namens — dabei bleibt es, und der Satz des Auftrags zu §3.2 bleibt richtig: Dort
fehlt er.

Dasselbe Vorwissen gibt es für die beiden anderen Namen, und es gehört vor die Messung, nicht
dahinter: M17 (2) misst in Fenster A `Message.SNDPRN` mit 383 Zeilen auf 383 Nachrichten bei
**fester Länge 6** und `Message.VFN` mit 397 Zeilen auf 397 Nachrichten, 4 bis 21 Zeichen — und
**nur 43 verschiedenen Werten**. Die Deutung zu M163 unten nimmt das auf.

### Abweichung 2 — Erhebungen unter 180 s, Kandidaten unter 10 s

§4 des Auftrags setzt `max_statement_time = 10` für den ganzen Nachtrag. **M162 bis M164 laufen mit
180 s**, wie s11 und s12 der Hauptrunde: Sie sind Erhebungen über den Bestand und keine Kandidaten
für eine gebaute Abfrage — die Fassung B aus M156 hat für `NEXANS` allein 15 s gebraucht, und ein
Abbruch bei 10 s lieferte keine Deckungszahl, sondern nur die Wiederholung eines bekannten Befunds.
Das Fenster ist **nicht** verkleinert worden; das ist die Vorschrift, um die es §4 geht. **M165 bis
M167 laufen mit 10 s**, der Grenze des Lese-Pools ([`datenzugriff.md`](datenzugriff.md) §1), und
ein Abbruch ist dort das Ergebnis.

### Abweichung 3 — Laufzeitdisziplin bei M162 bis M164

Wie in der Hauptrunde (Vermerk unter M157): Für die drei Erhebungen steht **ein** Lauf je Statement,
nicht die beste von fünf. Die volle Disziplin steht in M165, M166 und M167.

### Abweichung 4 — M161 prüft eine Sache mehr, als der Auftrag nennt

M161‑7 wiederholt die Gegenprobe der Typ-Lesart aus M155 für die drei neuen Namen — je ein
`LIMIT 1`-Zugriff über `MessagePropertyNameIDX`. Ohne sie wäre offen, ob ein Name überhaupt in
`MessageProperty` vorkommt, bevor M162 seine Deckung misst.

## Nummernvergabe — Nachtrag

Der Auftrag nennt M161 und verlangt, den Stand nicht zu übernehmen, sondern über den **Höchstwert**
zu ermitteln — nicht über eine Bereichsprobe.

| | |
|---|---|
| Prüfung Messungen | `grep -rhoE 'M[0-9]{1,3}' docs/ scripts/ *.md \| grep -oE '[0-9]+' \| sort -un \| tail -20` |
| Ergebnis | 142 bis **160** lückenlos, dann **179**. M179 ist **keine Vergabe**, sondern Fließtext in dieser Datei („M153 bis M179: kein Treffer", Nummernvergabe der Hauptrunde) — genau der Fall, vor dem der Auftrag warnt. Höchste vergebene: **M160** |
| Gegenprobe | `grep -rnoE '\bM160\b' docs/ scripts/ *.md` → **21 Treffer in sechs Dateien** (diese Datei, `README.md`, zwei Skripte, zwei Rohausgaben). Der Ausdruck greift. **`\bM16[1-9]\b` und `\bM170\b`: kein Treffer** |
| Folge | **M161 bis M167 sind hier vergeben** — wie im Auftrag |
| Prüfung Entscheidungen | `grep -rhoE 'E.[0-9]{1,3}' docs/ scripts/ *.md \| grep -oE '[0-9]+' \| sort -un \| tail -20` |
| Ergebnis | Höchstwerte 143 bis 862 — fünf davon geöffnet (143, 427, 439, 500, 862): **sämtlich Datenwerte der Form `E_nnn`** in Rohausgaben unter `scripts/messung-schritt9/ergebnis/` und `scripts/messung-liste-verengung/ergebnis/`. Der Punkt im Ausdruck fängt den Unterstrich mit. Mit der Alternation aus der Hauptrunde, `E(‑\|-)[0-9]{1,3}`: höchste vergebene **E‑105**, dazu der bekannte Falschtreffer E‑780 |
| Folge | **Keine E‑Nummer vergeben**, wie der Auftrag es vorsieht. Geprüft ist der Stand trotzdem, damit der nächste Auftrag ihn nicht ungeprüft weiterreicht |
| Offene Punkte | `grep -rhoE 'Punkt \*?\*?1[0-9][0-9]' docs/*.md` → höchste 146; `grep -rhoE '^\*\*1[0-9][0-9]\.' docs/*.md` → 147, 148, 149 und **167**. Der Treffer 167 ist `**167.734 ist der Fall …` in `messungen-schritt7.md:4410`, eine Zahl mit Tausenderpunkt. Höchste vergebene: **149** (diese Datei). **Fortlaufend ab 150** |

## Rahmen — Nachtrag

Sitzung `n0-rahmen.sql`, gleiche Abfragen in gleicher Reihenfolge wie `s0-rahmen.sql`. Alles, was
hier nicht steht, gilt unverändert aus dem Rahmen der Hauptrunde.

| | |
|---|---|
| Ziel | **Testkopie**, `10.6.22-MariaDB-0ubuntu0.22.04.1-log`, `SELECT @@global.read_only` → **`1`** als erste Abfrage jeder Sitzung |
| Benutzer | `monitor_read@%`, ausschließlich `SELECT` |
| Serverzeit Beginn | `2026-09-08 09:39:48` |
| Client | `mysql.exe` **Ver 8.0.46**, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t`; Passwort über `MYSQL_PWD` |
| `@@session.sql_mode`, `@@div_precision_increment`, `@@max_statement_time` (Vorgabe), `@@profiling_history_size` (Vorgabe), `innodb_buffer_pool_size` | **unverändert**: `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION`, **4**, `0.000000`, **15**, 26.843.545.600 Byte |
| Datenstand | `Message.MessageLastUpdate` von `2024-10-01 02:00:28` bis `2026-07-08 17:21:10` — **unverändert** gegenüber M0 und der Hauptrunde |
| Grenzen | **180 s** für M162 bis M164 (Erhebung), **10 s** für M165 bis M167 (Kandidaten) — siehe Abweichung 2 |
| Fenster | **Fenster B**, `2025-11-30 00:00:00` bis `2025-12-30 00:00:00`, halboffen; 24 Stunden in M166: `2025-12-29 00:00:00` bis `2025-12-30 00:00:00`. Kein `NOW()` |
| Mandanten (L7) | `NEXANS` und `SUTTONS` |
| Sitzungen | `scripts/messung-property-suche/n*.sql`, Rohausgaben unter `ergebnis/n*.txt` — vom bestehenden `.gitignore`-Eintrag der Hauptrunde bereits ausgeschlossen (G1); **keine neue Ignorierregel nötig** |

## Verifizierte Ausgangslage — Nachtrag, nicht neu erhoben

Aus §3 des Auftrags; die Werte stehen oben in dieser Datei und sind nicht wiederholt gemessen worden.

| Gegenstand | Wert | Herkunft |
|---|---|---|
| `MessageProperty`, Zeilen | 75.571.462, gezählt | M44 |
| Optimizer-Schätzung derselben Tabelle | 37,9 % zu niedrig | M155, Nebenbefund |
| `MessagePropertyNameValueIDX` | `(MessagePropertyName, MessagePropertyValue(50))` | M14 |
| `MessagePropertyValueIDX` | `(MessagePropertyValue(50))` | M14 |
| Fenster B | 214.330 Nachrichten über alle Mandanten | M156 |
| davon `NEXANS` | 180.251 Nachrichten, 28.524 Wurzeln, 101.270 Kinder | M156 |
| davon `SUTTONS` | 21.516 Nachrichten, 639 Wurzeln, 1.247 Kinder | M156 |
| `Message.GUID`, ganzer Weg, 30 Tage | 0,942 ms | M159 |
| `Message.ReceiverID`, ganzer Weg, 30 Tage | 1.222,763 ms | M159 |
| Zählung **eines** Namens über den Gesamtbestand | bis 125,527 s | M157 |

### Der Vergleichsmaßstab ist die gebaute BAM-Suche, nicht das Dashboard

**Für eine Suche gibt es kein 500‑ms‑Budget.** Die halbe Sekunde aus
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 gehört dem Dashboard; M159 hat sie als
Maßstab genommen, und das steht dort so. Maßstab dieses Nachtrags ist die Suche, die das Projekt
bereits ausliefert — [`bam-suche.md`](bam-suche.md) §2 und §4, M47 in der gebauten Fassung:

| | 30 Tage | ein Jahr |
|---|---:|---:|
| BAM-Suche, gebaut (M47) | 1,095 bis **1.655,8 ms** | 1,089 bis **8.939,7 ms** |
| ohne Zeitfenster, nicht gebaut (M35) | 0,729 bis **10.752,8 ms** — Grenze gerissen | — |

Ein neuer Name unter 1.655,8 ms über 30 Tage liegt damit im Bereich des Ausgelieferten. Das heißt
nicht „gut", es heißt „nicht schlechter als das, was schon läuft".

## Die vorregistrierten Deutungen des Nachtrags — vor dem ersten Lauf geschrieben

Festgeschrieben vor M162 (Commit „docs: Nachtrag Property-Suche — Nummernstand und Ist-Stand der
Konfiguration (M161)"); M161 war zu diesem Zeitpunkt bereits gelaufen und ist unten mit seiner
Deutung dagegengehalten.

| Messung | Gegenstand | Vorregistrierte Deutung |
|---|---|---|
| **M161** | Ist-Stand der Konfigurationstabelle | Acht Typ‑0 ohne Mandant, sechs Typ‑1 mit `NEXANS`, kein `Service.Type`. Weicht der Ist-Stand ab, wird der **Ist-Stand** gemessen, nicht die Erwartung |
| **M162** | Deckung der drei neuen Namen | Schwelle unverändert **20 % über die Wurzeln** (M28‑2); sie siebt nicht aus, sie erzeugt einen Vermerk. Für `SNDPRN` und `VFN` **hohe Quoten** erwartet, weil beide in §3.2 als bekannte Namen geführt sind. Für `DestinationFilename` **keine Erwartung** zur Deckung je Kettenstellung — Vorwissen nur die 9,688 % über alle Mandanten aus M56, siehe Widerspruch 1 |
| **M163** | Werteverteilung | Dateiname und Senderkennung verhalten sich verschieden: Dateinamen nahezu eindeutig, eine Senderkennung ist eine **Kategorie** wie `Service.Type`. **Fällt `SNDPRN` in die Kategorieklasse, ist das der wichtigste Befund des Nachtrags.** Vorwissen zu `VFN` aus M17 (2): 43 verschiedene Werte auf 397 Zeilen an einem Tag — er ist danach **kein** Schlüssel |
| **M164** | Längenverteilung | `DestinationFilename` überschreitet 50 Zeichen **in nennenswertem Umfang**; `SNDPRN` und `VFN` nicht. Vorwissen: Maximum 73 (M56), `SNDPRN` fest 6, `VFN` 4 bis 21 (M17) |
| **M165** | Plan des Wertprädikats | **Nicht** zwingend `MessagePropertyNameValueIDX`, sondern möglicherweise der reine Wertindex wie bei `Message.GUID` (M158, Befund 3: die Selektivität kommt vom Wert). `FORCE INDEX` nur als Diagnose |
| **M166** | Der ganze Weg | Unter **1.655,8 ms** über 30 Tage. `Converter.TransactionID` verhält sich wie `Message.GUID`, einstellige Millisekunden |
| **M167** | Kosten der Nachprüfung | Die Nachprüfung kostet, aber nicht die Größenordnung. Wird sie zum beherrschenden Anteil, ist `DestinationFilename` in derselben Lage wie `Service.Type`. Findet M164 keinen Wert über 50 Zeichen, bleibt die Lücke offen und wird **nicht** mit einem konstruierten Fall gefüllt |

> ⚠️ **Regel G1 greift in diesem Nachtrag schärfer als in der Hauptrunde.** Die drei Namen tragen
> fachliche Inhalte: ein Dateiname, die EDIFACT-Senderkennung, eine fachliche Nummer. **In diese
> Datei kommt kein einziger dieser Werte** — nicht vollständig, nicht abgekürzt, nicht als Beispiel,
> nicht in einer `EXPLAIN`-Ausgabe. Ausgegeben werden ausschließlich Zähler, Längen, Ränge und
> Laufzeiten; wo eine Ausgabe Werte enthalten könnte, wird im Statement aggregiert oder über
> `QUOTE()` und `PREPARE` eingesetzt, nie nachträglich geschwärzt. Die Rohausgaben liegen unter
> `scripts/messung-property-suche/ergebnis/` und sind nicht eingecheckt.

---

## M161 — Ist-Stand der Konfigurationstabelle

**Sitzung** `n1-m161.sql`. Vollständiger Abzug von `MessagePropertyName`, `MandantID`,
`MessagePropertyType`, dazu `COUNT(*)`, die Kreuztabelle, der Verbleib von `Service.Type` und der
Tabellenstatus. **Feldnamen sind Konfiguration und dürfen stehen.**

> **Vorregistrierte Deutung.** Acht Typ‑0 ohne Mandant, sechs Typ‑1 mit `NEXANS`, kein
> `Service.Type`.

**Die Deutung hat getroffen, Zeile für Zeile.**

| | gezählt |
|---|---:|
| Zeilen (`COUNT(*)`) | **14** |
| verschiedene Namen | 14 |
| Typ 0, `MandantID IS NULL` | **8** |
| Typ 1, `MandantID = 'NEXANS'` | **6** |
| `Service.Type` | **0 Zeilen — entfernt, nicht bloß nicht ergänzt** |
| `MandantID = ''` / `MessagePropertyType IS NULL` | 0 / 0 |

**Die vierzehn Zeilen:**

| Typ | `MandantID` | `MessagePropertyName` | Stand |
|---|---|---|---|
| 0 | `NULL` | `Message.MessageID`, `Message.MessageIDSource`, `Message.MessageIDTarget`, `Message.ProcessID`, `Message.ProcessName`, `Message.SOSID`, `Message.SOSName`, `Message.Status` | **unverändert** — Zeile für Zeile identisch mit M154 (gegen `ergebnis/s2-m154.txt` verglichen: acht gegen acht, kein Unterschied) |
| 1 | `NEXANS` | `Converter.TransactionID`, `Message.GUID`, `Message.ReceiverID` | unverändert aus M154 |
| 1 | `NEXANS` | **`Message.DestinationFilename`, `Message.SNDPRN`, `Message.VFN`** | **neu** — Gegenstand dieses Nachtrags |
| 1 | `NEXANS` | ~~`Service.Type`~~ | **entfernt** |

**Der Handabgleich ist in den Metadaten sichtbar.** `information_schema.TABLES` zeigt
`CREATE_TIME 2026-08-14 17:14:34` (unverändert gegenüber M153) und **`UPDATE_TIME 2026-09-08
09:23:00`** — die Tabelle ist also nicht neu angelegt, sondern in der bestehenden geändert worden,
16,8 Minuten vor Beginn dieses Nachtrags. `TABLE_ROWS` steht dabei auf **8** und nicht auf 14:
Selbst bei einer Tabelle mit vierzehn Zeilen ist die Schätzung nach der Änderung veraltet — dieselbe
Eigenschaft, die M155 an `MessageProperty` mit 37,9 % beziffert hat, hier folgenlos.

**Alle drei neuen Namen kommen in `MessageProperty` vor** (M161‑7, Gegenprobe der Typ-Lesart in der
Form von M155, je `LIMIT 1`):

| id | table | type | key | key_len | rows | Extra |
|---|---|---|---|---:|---:|---|
| 1 | `MessageProperty` | `ref` | `MessagePropertyNameIDX` | 402 | 950.388 | `Using where; Using index` |

`kommtVor` = 1 für `Message.DestinationFilename`, `Message.SNDPRN` und `Message.VFN`. **Die
Typ-Lesart aus M155 hält damit auch für die drei neuen Zeilen** — sechs von sechs Typ‑1‑Namen
kommen in `MessageProperty` vor, und die acht Typ‑0‑Namen sind unverändert die acht, die M155 dort
nicht gefunden hat.

**Was M161 für den Rest des Nachtrags festlegt.** Gemessen werden die **drei neuen** Namen,
`Converter.TransactionID` nur in M166 (Lücke 6). `Service.Type` wird nicht mehr gemessen; seine
Befunde (M157 bis M159, Punkt 148) bleiben stehen und beschreiben nun einen Namen, der **nicht mehr
im Angebot** ist — was das für Punkt 148 heißt, steht am Ende des Nachtrags.

*Belegvermerk (L10): gemessen ist der Inhalt der Konfigurationstabelle der Testkopie am 08.09.2026
um 09:39 Serverzeit und das Vorkommen der drei Namen in `MessageProperty`. Behauptet wird, dass
Testkopie und Produktion in dieser Tabelle übereinstimmen — das ist **nicht** gemessen, sondern die
Aussage des Auftrags über den Handabgleich; die Produktion ist nicht befragt worden.*

---
