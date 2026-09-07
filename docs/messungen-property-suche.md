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
