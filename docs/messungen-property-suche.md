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
> 12.480 Zeilen", nicht der Wert. **Feldnamen sind Konfiguration und dürfen stehen.** Prozesse und
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
