# Messungen zum Plan der Suche — Produktionsbefund und F4 im Suchpfad

Erhoben am **13.10.2026**. Auftrag: „Suche der Nachrichtenliste: Plan unabhängig von der Statistik
(F4) und Anzeige bei Abbruch", Teil A und C, Zweig `fix/suche-plan` (abgezweigt von
`diag/suche-produktion`, `8f31912`).

**Anlass.** Die Freitextsuche der Nachrichtenliste bricht in Produktion bei `NEXANS` für jeden
Begriff nach 10 s mit `suche-abgebrochen` ab, auch im 24‑Stunden‑Fenster; bei `SUTTONS` nicht. Das
Diagnoseskript [`scripts/diagnose-produktion/suche.sql`](../scripts/diagnose-produktion/suche.sql)
hat am 13.10.2026 in Produktion gezeigt, warum (M193). Gebaut ist daraufhin die Fassung F4 aus
[`nachrichtenliste.md`](nachrichtenliste.md) §5a, **nur im Suchpfad** (E‑237). Diese Datei hält den
Befund, die Vorprüfung (M194), die Messung des gebauten Statements gegen die Testkopie (M195) und das
erweiterte Diagnoseskript (M196) fest.

---

## Nummernvergabe

| | |
|---|---|
| Messungen | `git grep -E '\bM[0-9]{1,3}\b'` über `docs`, `scripts` und `*.md` auf **allen lokalen Zweigen und `origin/main`**, Höchstwert **M192** ([`message-status.md`](message-status.md)). Dann `git grep -nE '\bM(19[3-9]\|20[0-9])\b'` über den **ganzen** Baum aller Zweige und `grep -rnE` über die drei Worktrees (`overlord-monitoring-e37`, `overlord-monitoring-neu-laden`, `olm-abbruch`): **kein Treffer** |
| Eichung | `git grep -cE '\bM(19[0-2])\b' main -- docs` → Treffer in **neun** Dateien, u. a. `message-status.md` (14, dort steht M192), `PROJEKTBESCHREIBUNG.md` (5), `README.md` (4). `\b` greift in `git grep -E` also |
| Folge | **M193 bis M196** |
| Entscheidungen | `git grep -E 'E(‑\|-)[0-9]{1,3}\b'` auf allen Zweigen: Höchstwerte 231 bis 236, dann **249** und **780**. Beide gelesen: **E‑249** steht in [`spaltenwahl.md`](spaltenwahl.md) Zeile 749 als Bereichsangabe („E‑231 bis E‑249 auf keinem Stand"), **E‑780** ist der bekannte Falschtreffer (ein Datenwert in einer Rohausgabe). Ganzer Baum aller Zweige mit `E[^0-9]{1,3}(23[7-9]\|24[0-9])\b` (Code-Kommentare eingeschlossen): nur die Zeile in `spaltenwahl.md` |
| Eichung | `git grep -cE 'E[^0-9]{1,3}(23[4-6])\b' main` → Treffer in **29** Dateien (Backend, Tests, `docs`, Frontend), u. a. `BamSucheService.java`, `MessageStatusClassifier.java` (3), `MessageStatusKind.java` |
| Folge | **E‑237 bis E‑239**, je eine Entscheidung vom 13.10.2026 |
| Offene Punkte | Projektweit nummeriert. Zwei Schreibweisen — *und eine dritte als Tabellenzeile `\| **NNN** \|`, nachgetragen nach der Gegenprüfung: geeicht an 196 bis 198 in `dateiansicht-darstellung.md`, über 220 kein Treffer* —: `- **218 — …**` ([`message-status.md`](message-status.md)) und `- **76. …**`. Suche über beide auf allen Zweigen: Höchstwert **220**. Treffer darüber gelesen und ausgesondert: `155.871`, `234.159`, `264.469` (Zahlen in [`messungen-schritt7.md`](messungen-schritt7.md) Zeile 4402 bis 4407) und „22 Punkte, 231 Fenster" in `BaumfensterTest.java` |
| Eichung | `git grep -cE '^\s*[-*] (~~)?\*\*2(1[89]\|20)( \|\.\|\*)' main -- docs` → 3 Treffer in `message-status.md` |
| Folge | **Punkte 221 bis 224** — *nachgezogen am selben Abend:* **225** (aus M195) und **226 bis 229** (Teil B, geprüfte Punkte). Die Suche über alle Zweige ist für 221 bis 239 in allen drei Schreibweisen wiederholt, ohne Treffer |
| Bekannte Falschtreffer laut Auftrag | `E‑780`; Zeile 21 in [`messungen-schritt10.md`](messungen-schritt10.md) („Der Bereich … ist frei"); M179 als Fließtext. Zeile 21 und M179 liegen unterhalb der vergebenen Bereiche, `E‑780` ist ein Datenwert in einer Rohausgabe; alle drei sind ohne Folge. *(Berichtigt am selben Abend — hier stand „Alle drei liegen unterhalb der vergebenen Bereiche"; für E‑780 stimmte das nicht.)* |

### Die Entscheidungen vom 13.10.2026

| Nummer | Entscheidung des Auftraggebers | Umgesetzt in |
|---|---|---|
| **E‑237** | **F4 nur im Suchpfad:** `IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)` auf `Message`, genau dann, wenn ein Suchbegriff gesetzt ist und kein Prozessfilter | `NachrichtenRepository.ignoriertProzessindizes` und `PROZESSINDIZES`; [`nachrichtenliste.md`](nachrichtenliste.md) §5, Kasten vom 13.10.2026 |
| **E‑238** | **Bei `suche-abgebrochen` wird die alte Seite ausgeblendet.** Bei `suchbegriff-zu-kurz`, `suchbegriff-zu-unscharf` und `suche-fenster-zu-gross` bleibt sie stehen wie bisher | Code auf Zweig `fix/suche-abbruch-anzeige`, Doku hier: [`nachrichtenliste.md`](nachrichtenliste.md) §8.2, Kasten vom 13.10.2026. **Der Code-Zweig darf nicht vor diesem gemergt werden** — sonst steht in `main` Code ohne seine Doku |
| **E‑239** | **Das Diagnoseskript ist Pflicht:** Jede Änderung, die den Plan von Liste oder Suche berühren kann, erweitert `scripts/diagnose-produktion/suche.sql`, und Lukas führt das `EXPLAIN` vor dem Merge in der Produktion aus | [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) L7 und [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 Regel 7, je ein Zusatz; §11 Annahme A12; M196 |

---

## M193 — Der Produktionsbefund vom 13.10.2026

> **Belegvermerk.** *Erhoben von Lukas (Auftraggeber) in MySQL Workbench gegen die Produktion*, am
> 13.10.2026 um 16:59:50 Serverzeit, mit `suche.sql` vom Stand `8f31912`. **Nur `EXPLAIN` und
> `information_schema`, keine Laufzeiten** — das Skript führt kein Statement an `Message` aus. Die
> beiden Suchbegriffe sind Kundennamen und stehen nirgends (Regel G1); bekannt ist nur ihre Länge
> und ihre Auflösung. Die vollständige Ausgabe liegt unter
> `scripts/diagnose-produktion/ergebnis/produktion-2026-10-13.txt` und ist von Git ignoriert. Die
> Gegenüberstellung mit der Testkopie stammt aus demselben Skript, am selben Tag mit dem Anker
> `2025-12-30 04:09:47` gegen die Testkopie gefahren (`ergebnis/testkopie.txt`).

### Server und Optimierer

| | Produktion | Testkopie |
|---|---|---|
| Version | `10.6.22-MariaDB-0ubuntu0.22.04.1-log` | dieselbe |
| `optimizer_switch` | — | **zeichengleich** mit der Produktion |
| `innodb_stats_persistent` / `_auto_recalc` / `_sample_pages` | `ON` / `ON` / `20` | dieselben |
| `use_stat_tables` · `optimizer_use_condition_selectivity` · `optimizer_search_depth` · `eq_range_index_dive_limit` | `PREFERABLY_FOR_QUERIES` · 4 · 62 · 200 | dieselben |
| `@@global.read_only` | **`ON`** | `ON` |
| `@@global.max_statement_time` | `0` | `0` |
| Zeitzone | `SYSTEM`, `CEST`, `NOW() − UTC_TIMESTAMP()` = 02:00 | dieselbe |
| `Max_statement_time_exceeded` seit Serverstart | **45** | 244 |
| `Com_kill` seit Serverstart | **26** | 6 |
| `Uptime` | 39.919.245 s (rund 462 Tage) | 11.401.985 s |

> **Nebenbefund, nicht gedeutet: Auch die Produktion meldet `@@global.read_only = ON`.** Der
> Auftrag beschreibt die Testkopie als `read_only` und die Produktion als den Server, der seine
> Statistik regelmäßig neu rechnet. Beides kann zugleich wahr sein — `read_only` hält gewöhnliche
> Verbindungen vom Schreiben ab, nicht die Replikation und nicht die Hintergrundstatistik von
> InnoDB —, aber es ist **nicht erhoben**, gegen welchen Server die Anwendung in Produktion liest und
> ob es ein Replikat ist. Der Unterschied in der Statistik unten belegt, dass dieser Server sie neu
> gerechnet hat; wie, sagt die Ausgabe nicht.

### Die Indexstatistik

`ca. Zeilen je Wert` ist `TABLE_ROWS / CARDINALITY` — die Zahl, die der Optimierer für einen
Nachschlag über den Index erwartet.

| Tabelle · Index | Spalte | Produktion `CARDINALITY` | je Wert | Testkopie `CARDINALITY` | je Wert |
|---|---|---:|---:|---:|---:|
| `Message` (`TABLE_ROWS`) | | **5.083.782** | | **3.560.486** | |
| `Message` · **`Message_ProcessFK`** | `ProcessID` | **9.127** | **557** | 18 | 197.805 |
| `Message` · **`ProejctIDIDX`** | `ProcessID` | 18 | 282.432 | 18 | 197.805 |
| `Message` · `MessageStatusIDX` | `MessageStatus` | 18 | 282.432 | 18 | 197.805 |
| `Message` · `MessageLastUpdateIDX` | `MessageLastUpdate` | 1.270.945 | 4 | 1.780.243 | 2 |
| `Message` · `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdate`, `ProcessID`, `MessageID` | 1.694.594 / 5.083.782 / 5.083.782 | 3 / 1 / 1 | 1.780.243 / 3.560.486 / 3.560.486 | 2 / 1 / 1 |
| `Message` · `PRIMARY` | `MessageID` | 5.083.782 | 1 | 3.560.486 | 1 |
| `Message` · `SourceMessageIDIDX` | `SourceMessageID` | 2.541.891 | 2 | 1.780.243 | 2 |
| `Message` · `TargetMessageIDIDX` | `TargetMessageID` | 94.144 | 54 | 63.580 | 56 |
| `Process` (`TABLE_ROWS`) · `Process_ProjectFK` | `ProjectID` | 1.566 · 223 | 7 | 1.490 · 298 | 5 |
| `ProjectMandant` (`TABLE_ROWS`) · `ProjectMandant_Mandant_idx` | `MandantID` | 131 · 21 | 6 | 134 · 22 | 6 |

**Der eine Unterschied, der zählt:** `Message_ProcessFK` trägt in der Produktion **9.127** und
damit **557** geschätzte Zeilen je Prozess. Auf der Testkopie stehen alle drei Indizes auf
`ProcessID` und `MessageStatus` bei **18** — der Befund aus M83, auf dem §5a seine beiden
Planfamilien erklärt. In der Produktion schätzt der Optimierer einen Nachschlag je Prozess damit
**rund 355‑mal kleiner** als auf der Testkopie (557 gegen 197.805 Zeilen), und genau diesen Weg
nimmt er.

### Die Vorfilterung

| Fall | Mandant | Begriff | Prozesse | Abläufe |
|---|---|---|---:|---:|
| a, e | `NEXANS` | 6 Zeichen | 2 | 5 |
| c | `SUTTONS` | 3 Zeichen | 3 | 3 |

Dieselben Trefferzahlen auf der Testkopie — die Stammdaten der beiden Begriffe sind dort gleich
aufgelöst.

### Die Pläne, Fall für Fall

Fenster: 24 Stunden bis `NOW()` der Produktion (`2026-10-12 16:59:50` bis `2026-10-13 16:59:50`);
Fall f ab `2026-10-13 15:00:00`. Testkopie: 24 Stunden bis zum Anker, Fall f ab `2025-12-30 03:00:00`.

| Fall | Testkopie: Treiberzeile | Produktion: Treiber und Kette |
|---|---|---|
| **a** `NEXANS`, mit Suche, 24 h | `Message` `range` `MessageLastUpdateIDX`, 13.534 | **`ProjectMandant` `ref` (18) → `mandanten_process` `ref` `Process_ProjectFK` (7) → `Message` `ref` `Message_ProcessFK` (557)**, `Using where; Using index; Using temporary; Using filesort` |
| **b** `NEXANS`, ohne Suche, 24 h unverengt | `Message` `range` `MessageLastUpdateIDX`, 13.534 | **prozessgetrieben, Zeile für Zeile wie a** |
| **c** `SUTTONS`, mit Suche, 24 h | `Message` `range` `MessageLastUpdateIDX`, 13.534 | prozessgetrieben, `ProjectMandant` `ref` (**1**) → `mandanten_process` (7) → `Message` `ref` `Message_ProcessFK` (557), `Using temporary; Using filesort` |
| **d** `SUTTONS`, ohne Suche, unverengt | `Message` `range` `MessageLastUpdateIDX`, 13.534 | prozessgetrieben wie c |
| **e** wie a mit `IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)` | `Message` `range` `MessageLastUpdateIDX`, 13.534 | **`Message` `range` `MessageLastUpdateIDX`, 15.662**, `possible_keys` nur noch `MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX`; danach `Process`, `Project`, `SOS`, `mandanten_process`, `ProjectMandant`, `SOSAction` je `eq_ref` |
| **f** `NEXANS`, ohne Suche, ab der vollen Stunde vor Ende − 1 h | `Message` `range` `MessageLastUpdateIDX`, 358 | **`Message` `range` `MessageLastUpdateIDX`, 575** — zeitgetrieben, obwohl beide Prozessindizes in `possible_keys` stehen |

### Was daraus folgt (Stand 13.10.2026, mit dem Auftrag abgestimmt)

1. **Nicht der Suchbegriff lässt den Plan kippen, sondern das fehlende enge Fenster.** Fall b kippt
   ohne Suche genauso wie a, und laut Optimizer-Trace (`ergebnis/trace-roh.txt`, Testkopie) ändert
   der Begriff keine einzige Schätzung. Die Suche trifft es, weil sie **nie** verengt wird
   (`Abfragemerkmal.SUCHBEGRIFF`, [`nachrichtenliste.md`](nachrichtenliste.md) §5d); die Liste ohne
   Suche läuft bei einem dichten Mandanten über die erste Verengungsstufe, und dort ist sie
   zeitgetrieben (Fall f).
2. **Die Planwahl hängt an einer Statistik, die in der Produktion neu gewürfelt wird.** Eine
   Stichprobe von 20 Seiten über 5 Mio. Zeilen. Die Abfrage darf davon nicht abhängen.
3. **F4 nimmt den prozessgetriebenen Weg aus der Auswahl und hält in Produktion** (Fall e). Unter F4
   ist die Testkopie für diesen Pfad wieder eine gültige Messgrundlage: Beide Server wählen
   zeitgetrieben, und genau unter diesem Plan sind die Grenzen aus
   [L13](messungen-schritt4.md#l13--freitext-über-lange-fenster) gemessen.
4. **F4 darf nicht pauschal gesetzt werden.** §5a hat gemessen, dass F4 den Prozessfilter zerstört
   (`NEXANS`, drei kleine Prozesse: 1,730 → 1.547,772 ms).

---

## M194 — Vorprüfung: unbekannter Indexname und der gerenderte Hinweis (A1)

### Was MariaDB 10.6 bei einem unbekannten Namen tut

Gegen die Testkopie, `monitor_read@%`, `@@global.read_only = 1` als erste Abfrage, nur `EXPLAIN`:

| | Hinweis | Ergebnis |
|---|---|---|
| A1‑1 | `IGNORE INDEX FOR JOIN (gibt_es_nicht)` | **`ERROR 1176 (42000): Key 'gibt_es_nicht' doesn't exist in table 'Message'`** |
| A1‑2 | `IGNORE INDEX FOR JOIN (ProejctIDIDX, gibt_es_nicht)` — ein bekannter, ein unbekannter Name | **derselbe Fehler 1176** |
| A1‑3 | `IGNORE INDEX FOR JOIN (proejctididx, message_processfk)` — klein geschrieben | **kein Fehler**, Plan wie A1‑4 |
| A1‑4 | `IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)` — Kontrolle | `range` über `MessageLastUpdateProcessMessageIDX`, `possible_keys` ohne beide |

**Die Erwartung des Auftrags trifft zu.** Die Suche hängt ab jetzt an zwei Indexnamen des
Altsystems, einer davon mit dem Buchstabendreher `ProejctIDIDX`. Benennt das Altsystem einen der
beiden um — auch nur einen —, scheitert jedes Statement des Suchpfads mit Fehler 1176. jOOQ reicht
ihn als `DataAccessException` weiter, `NachrichtenRepository.anDerZeitgrenze` fängt nur
`SQLTimeoutException`, die Antwort ist **`500`**. Groß- und Kleinschreibung spielt keine Rolle.
Bewacht durch `NachrichtenSuchindizesDbIT` gegen die jeweils verbundene Datenbank; offener Punkt
**223** in [`nachrichtenliste.md`](nachrichtenliste.md) §9.

### Wie jOOQ 3.21.5 den Hinweis rendert

`MESSAGE.ignoreIndexForJoin("ProejctIDIDX", "Message_ProcessFK")`, gerendert über eine Wegwerfklasse
im Scratchpad gegen `backend/target/classes` und eine `MockConnection` (`SQLDialect.MARIADB`, keine
Datenbank):

```
… from `GlassfishDB`.`Message` ignore index for join (`ProejctIDIDX`, `Message_ProcessFK`) left outer join …
```

| Abgleich | Ergebnis |
|---|---|
| Hinweis gegen Fall e des Skripts (`' IGNORE INDEX FOR JOIN (…)'`) | **nicht zeichengleich** — jOOQ schreibt die Schlüsselwörter klein. Gleich bis auf Groß- und Kleinschreibung; MariaDB liest Schlüsselwörter ohne sie, der Plan ist derselbe. M196 schreibt deshalb die jOOQ-Form ins Skript |
| gerendertes Statement **mit** Suche, Hinweis entfernt, gegen `VORBEREITET BEIDE` aus [`gerendert.txt`](../scripts/diagnose-produktion/gerendert.txt) (Stand vor dem Bau) | **zeichengleich**, MD5 beidseits `408902c873c409b8ea3c599b3e4a20e6` |
| Statement **ohne** Suche gegen `VORBEREITET OHNE_SUCHE` | **zeichengleich** — der Pfad ohne Suche ist unberührt |
| Hinweis gesetzt bei | Suche (Prozesse und Abläufe), Suche nur über Prozesse, Suche mit Sortierung „älteste" |
| **kein** Hinweis bei | Suche mit Prozessfilter, ohne Suche, Statusfilter allein |

---

## Rahmen der Messung M195

| | |
|---|---|
| Ziel | **Testkopie**. `SELECT @@global.read_only` als **erste Abfrage** der Sitzung; der Lauf bricht ab, wenn sie nicht `1` liefert. Dieselbe Abfrage am Ende |
| Benutzer | `monitor_read@%` aus `OVERLORD_DB_READ_*`, ausschließlich `SELECT`, `SET`, `EXPLAIN`, `SHOW` |
| Werkzeug | **Eine Wegwerfklasse im Paket `message`, im Scratchpad**, gegen `backend/target/classes` des Commits `341369c` — **nicht** `mysql.exe` wie in §5a. *Abweichung vom Rahmen aus §5a, mit Absicht:* Das Statement wird vom Repository **gerendert** und über **denselben Treiber** wie in der Anwendung geschickt (Connector/J 3.5.8, URL ohne Parameter, also clientseitig vorbereitet: Zeitpunkte und Kennungen stehen als **Literale** auf der Leitung, nie als Sitzungsvariablen). Die Suchbegriffe verlassen die JVM nicht (G1) |
| Sitzung | **eine** Verbindung für den ganzen Lauf, alle Abfragen nacheinander. `SET SESSION max_statement_time = 60` — die wahre Laufzeit soll sichtbar werden, nicht ein Abbruch; gehalten wird jede Zahl gegen die **10 s** des Lese-Pools und gegen das Tor von **5 s** |
| Anker | `2025-12-30 04:09:47`, als Literal. Fenster 24 h, 30 Tage, 90 Tage bis zum Anker, beide Grenzen einschließlich |
| Statement | `NachrichtenRepository.finde` mit einer Abfrage aus echtem Suchtreffer (`loeseSucheAuf` gegen die Testkopie), `limit` 50, also `fetch next 51 rows only`. **F4** ist das Statement, wie es gebaut ist; **F0** dasselbe mit entferntem Hinweis — nach M194 zeichengleich mit dem Stand vor dem Bau |
| Hülle | wie §5a: `COUNT(*)`, `SUM(… IS NOT NULL)` über jede gejointe Spalte, `COUNT(DISTINCT MessageStatus)`, `MIN`/`MAX(MessageLastUpdate)` über das Statement als abgeleitete Tabelle. Keine Datenzeile verlässt den Server (G1); ohne die Hülle optimierte MariaDB die `LEFT JOIN` weg |
| Plan | `EXPLAIN` am **nackten** Statement, je Fall und Fassung |
| Laufzeit | je Fall: F0 und F4 je ein Aufwärmlauf, dann fünfmal **abwechselnd** F0, F4. Gewertet: **beste von fünf** aus dem Profil (`SET profiling = 1`, `profiling_history_size = 100`, die Dauer der jeweils letzten Abfrage, deren Text die Hülle ist — geprüft in der JVM, nicht ausgegeben), daneben die **Wanduhr** der JVM um Ausführung und Abholen |
| Gleichheit | Die Hülle muss für F0 und F4 dieselben Werte liefern; jede Abweichung wird gemeldet |

---

## Vorregistrierung — geschrieben vor dem ersten Lauf

*Diese Abschnitte sind vor dem ersten Lauf von M195 committed und danach nicht mehr geändert worden.*

### Die Erwartungen des Auftrags, wortgleich

- `NEXANS` unter F4 wie L13: 24 h im Millisekundenbereich, im schlimmsten Fall rund 1,2 s über 30
  und rund 3,9 s über 90 Tage.
- Die Mandanten, die heute über 30 Tage prozessgetrieben laufen, werden mit Suche teurer: von
  Millisekunden auf bis zu den L13-Werten. Das ist der Preis, und er gilt als tragbar, solange das
  Tor hält.
- Der Fall „nur Prozesse" bleibt unter F4 an das Fenster gebunden: Der Plan darf
  `MessageLastUpdateProcessMessageIDX` nehmen, aber keinen der beiden ignorierten Indizes.

**Tor:** Jeder Mandant, jede Klasse und jedes Fenster bleibt unter F4 warm bei **höchstens 5 s**:
bauen, testen, weiter mit A4. Ein Fall liegt darüber: nicht mergen, melden, mit dem Plan im
Volltext.

### Eine eigene Erwartung, als solche gekennzeichnet

- **Der schlimmste Fall kostet unter F4 bei allen zehn Mandanten ungefähr dasselbe.** Unter F4 ist
  der Plan zeitgetrieben, und der Zeitindex liest das Fenster **aller** Mandanten, bis 51 Treffer
  beisammen sind — trifft der Begriff im Fenster nichts, liest er es ganz. Was ein Mandant selbst im
  Fenster hat, ändert daran nichts. Erwartet: je Fenster eine Zahl für alle zehn, in der Größe der
  L13-Werte (L13 lag am Fensterende `2025-12-30 00:00:00`, diese Runde am Anker 04:09:47).

### Die Begriffsklassen, festgelegt vor dem Lauf

Kandidaten je Mandant: **jeder Name, den die Oberfläche für diesen Mandanten zeigt** —
`ProcessName`, `ProjectName` und `SOSName` seiner Stammdaten, am Rand befreit, mindestens drei
Zeichen —, als vollständiger Suchbegriff. Die Treffer rechnet die Wegwerfklasse vor, **maßgeblich
ist aber die Auflösung durch `loeseSucheAuf` gegen die Testkopie**; weicht sie ab (die Spalten
vergleichen nach `utf8mb4_general_ci`, Java nicht), fällt der Kandidat heraus und wird gezählt. Mehr
als 200 Treffer je Art scheiden aus (`suchbegriff-zu-unscharf`). Die Zeilen im Fenster kommen aus
**einem** gruppierten Durchlauf über das 90‑Tage‑Fenster (`ProcessID`, `SOSID`, je Fenster gezählt),
dem Mandanten zugeordnet über `Process → ProjectMandant` — so, wie die Mandantenkette es tut.

| Klasse | Für | Bedingung | Auswahl bei mehreren |
|---|---|---|---|
| **schlimmster Fall** | alle zehn | trifft Prozesse **und** Abläufe (die Regelform des Suchzweigs, wie L13), **keine** Zeile im 90‑Tage‑Fenster — damit auch keine in 30 Tagen und 24 Stunden | der erste nach Begriff (Java-`String`-Ordnung) |
| | | gibt es keinen: je Fenster der erste ohne Zeile **in diesem** Fenster | |
| **viele** | `NEXANS`, `SUTTONS` | Prozesse und Abläufe, **mindestens 51** Zeilen im 24‑Stunden‑Fenster — die Seite füllt sich in jedem Fenster am oberen Rand | die meisten Zeilen in 24 h |
| **wenige** | `NEXANS`, `SUTTONS` | Prozesse und Abläufe, **1 bis 50** Zeilen im 24‑Stunden‑Fenster | die wenigsten Zeilen in 90 Tagen, dann in 24 h, dann Begriff |
| **nur Prozesse** | `NEXANS`, `SUTTONS` | mindestens ein Prozess, **kein** Ablauf — die Form `(… in (…) or false)`, in der MariaDB `or false` wegfaltet | die wenigsten Zeilen in 90 Tagen, dann Begriff |

**Trifft ein Mandant für eine Klasse keinen Begriff, steht das in der Tabelle**, und es wird nichts
ersetzt — keine andere Klasse, keine andere Form. Ausgegeben werden je Begriff nur Länge,
Trefferzahlen und Zeilen je Fenster, nie der Begriff und keine Kennung.

---

## M195 — F0 gegen F4 über alle Mandanten, Klassen und Fenster

Gefahren am **13.10.2026 von 17:30:49 bis 17:40:58 Serverzeit** gegen die Testkopie, eine Sitzung,
Rahmen wie oben. `@@global.read_only = 1` am Anfang und am Ende, Benutzer `monitor_read@%`. Der
gruppierte Durchlauf für die Begriffsauswahl lieferte 884 Gruppen in 6,8 s; im 24‑Stunden‑Fenster stehen
**6.382** Zeilen aller Mandanten, im 30‑Tage‑Fenster **214.204**, im 90‑Tage‑Fenster **680.447**.
**Kein Kandidat ist verworfen worden**: Die Auflösung durch `loeseSucheAuf` stimmte bei jedem
geprüften Begriff mit der Vorausrechnung überein.

**Die Hülle lieferte in allen 46 gemessenen Fällen für F0 und F4 dieselben Werte** — der Hinweis
verändert den Weg, nicht das Ergebnis, wie in §5a.

### Das Ergebnis, Fall für Fall

Laufzeiten in Millisekunden aus dem Profil, beste von fünf; die Wanduhr der JVM daneben. Treiber ist
die erste Zeile des Plans, in Klammern die geschätzten Zeilen. „`Message`" ist die Zeile dieser
Tabelle im F4-Plan — in keinem einzigen Fall steht dort `ProejctIDIDX` oder `Message_ProcessFK`,
weder unter `possible_keys` noch unter `key`.

| Mandant | Klasse | Fenster | Begriff: Zeichen · Prozesse · Abläufe | Zeilen des Begriffs 24 h / 30 d / 90 d | **F0** Treiber | **F0** beste | **F4** Treiber | **F4** `Message` | **F4** beste | **F4** Wanduhr | ≤ 5 s |
|---|---|---|---|---|---|---:|---|---|---:|---:|:---:|
| `NEXANS` | schlimmster | 24h | 23 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 37,809 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **37,762 ms** | 39,247 ms | ✔ |
| `NEXANS` | schlimmster | 30d | 23 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.232,231 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.230,561 ms** | 1.231,877 ms | ✔ |
| `NEXANS` | schlimmster | 90d | 23 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | 3.947,015 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **3.934,451 ms** | 3.935,459 ms | ✔ |
| `NEXANS` | viele | 24h | 28 · 2 · 2 | 126 / 12.835 / 23.769 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 23,111 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **22,751 ms** | 23,577 ms | ✔ |
| `NEXANS` | viele | 30d | 28 · 2 · 2 | 126 / 12.835 / 23.769 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 23,336 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **23,785 ms** | 24,745 ms | ✔ |
| `NEXANS` | viele | 90d | 28 · 2 · 2 | 126 / 12.835 / 23.769 | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | 23,693 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **23,239 ms** | 24,034 ms | ✔ |
| `NEXANS` | wenige | 24h | 17 · 1 · 1 | 1 / 5 / 13 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 38,071 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **38,990 ms** | 40,127 ms | ✔ |
| `NEXANS` | wenige | 30d | 17 · 1 · 1 | 1 / 5 / 13 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.211,158 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.238,128 ms** | 1.239,271 ms | ✔ |
| `NEXANS` | wenige | 90d | 17 · 1 · 1 | 1 / 5 / 13 | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | 3.902,032 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **3.868,290 ms** | 3.869,126 ms | ✔ |
| `NEXANS` | nur Prozesse | 24h | 16 · 2 · 0 | 0 / 0 / 0 | `Message` `range` `ProejctIDIDX` (14) | 1,530 ms | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (17) | `range` `MessageLastUpdateProcessMessageIDX` (12.902) | **44,604 ms** | 45,804 ms | ✔ |
| `NEXANS` | nur Prozesse | 30d | 16 · 2 · 0 | 0 / 0 / 0 | `Message` `range` `ProejctIDIDX` (14) | 1,545 ms | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (17) | `range` `MessageLastUpdateIDX` (437.150) | **1.423,037 ms** | 1.424,037 ms | ✔ |
| `NEXANS` | nur Prozesse | 90d | 16 · 2 · 0 | 0 / 0 / 0 | `Message` `range` `ProejctIDIDX` (14) | 1,640 ms | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (17) | `ALL` (3.560.486) | **3.478,564 ms** | 3.479,125 ms | ✔ |
| `SUTTONS` | schlimmster | 24h | 43 · 1 · 1 *(nur 24 h ohne Zeile)* | 0 / 85 / 273 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 36,897 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **37,319 ms** | 37,950 ms | ✔ |
| `SUTTONS` | schlimmster | 30d | **kein Begriff** — keiner mit Prozess- und Ablauftreffer ohne Zeile in diesem Fenster | | | | | | | | |
| `SUTTONS` | schlimmster | 90d | **kein Begriff** — keiner mit Prozess- und Ablauftreffer ohne Zeile in diesem Fenster | | | | | | | | |
| `SUTTONS` | viele | 24h | 30 · 1 · 1 | 96 / 2.877 / 8.637 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 27,977 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **27,780 ms** | 28,384 ms | ✔ |
| `SUTTONS` | viele | 30d | 30 · 1 · 1 | 96 / 2.877 / 8.637 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (1) | 915,042 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **29,494 ms** | 30,395 ms | ✔ |
| `SUTTONS` | viele | 90d | 30 · 1 · 1 | 96 / 2.877 / 8.637 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (1) | 1.073,862 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **31,783 ms** | 33,375 ms | ✔ |
| `SUTTONS` | wenige | 24h | 37 · 1 · 1 | 1 / 10 / 32 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 42,441 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **41,977 ms** | 42,709 ms | ✔ |
| `SUTTONS` | wenige | 30d | 37 · 1 · 1 | 1 / 10 / 32 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (1) | 976,520 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.366,152 ms** | 1.366,823 ms | ✔ |
| `SUTTONS` | wenige | 90d | 37 · 1 · 1 | 1 / 10 / 32 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (1) | 906,177 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **3.932,435 ms** | 3.933,206 ms | ✔ |
| `SUTTONS` | nur Prozesse | 24h | 31 · 1 · 0 | 96 / 2.878 / 8.638 | `mandanten_process` `const` `PRIMARY` (1) | 24,841 ms | `mandanten_process` `const` `PRIMARY` (1) | `range` `MessageLastUpdateProcessMessageIDX` (12.902) | **24,742 ms** | 25,854 ms | ✔ |
| `SUTTONS` | nur Prozesse | 30d | 31 · 1 · 0 | 96 / 2.878 / 8.638 | `mandanten_process` `const` `PRIMARY` (1) | 24,661 ms | `mandanten_process` `const` `PRIMARY` (1) | `range` `MessageLastUpdateIDX` (437.150) | **24,624 ms** | 25,312 ms | ✔ |
| `SUTTONS` | nur Prozesse | 90d | 31 · 1 · 0 | 96 / 2.878 / 8.638 | `mandanten_process` `const` `PRIMARY` (1) | 24,475 ms | `mandanten_process` `const` `PRIMARY` (1) | `range` `MessageLastUpdateIDX` (1.468.910) | **24,556 ms** | 25,510 ms | ✔ |
| `VOTG` | schlimmster | 24h | 27 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 37,579 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **38,773 ms** | 39,748 ms | ✔ |
| `VOTG` | schlimmster | 30d | 27 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.213,648 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.219,939 ms** | 1.220,678 ms | ✔ |
| `VOTG` | schlimmster | 90d | 27 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | 3.926,183 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **3.907,695 ms** | 3.908,912 ms | ✔ |
| `IBIS` | schlimmster | 24h | 6 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 41,140 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **40,495 ms** | 40,914 ms | ✔ |
| `IBIS` | schlimmster | 30d | 6 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.247,355 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.239,125 ms** | 1.240,038 ms | ✔ |
| `IBIS` | schlimmster | 90d | 6 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | 4.028,055 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **4.027,805 ms** | 4.028,222 ms | ✔ |
| `IBISGUS` | schlimmster | 24h | 6 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 39,233 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **38,960 ms** | 39,750 ms | ✔ |
| `IBISGUS` | schlimmster | 30d | 6 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.265,518 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.249,460 ms** | 1.250,967 ms | ✔ |
| `IBISGUS` | schlimmster | 90d | 6 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | 4.060,218 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **4.059,027 ms** | 4.059,839 ms | ✔ |
| `ZAST` | schlimmster | 24h | 42 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 39,945 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **39,979 ms** | 40,574 ms | ✔ |
| `ZAST` | schlimmster | 30d | 42 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.247,568 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.252,379 ms** | 1.253,324 ms | ✔ |
| `ZAST` | schlimmster | 90d | 42 · 1 · 1 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (4) | 28,565 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **3.966,031 ms** | 3.967,348 ms | ✔ |
| `WOC` | schlimmster | 24h | 20 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 40,331 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **38,874 ms** | 39,674 ms | ✔ |
| `WOC` | schlimmster | 30d | 20 · 1 · 1 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (1) | 14,597 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.275,492 ms** | 1.276,083 ms | ✔ |
| `WOC` | schlimmster | 90d | 20 · 1 · 1 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (1) | 15,694 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **4.270,992 ms** | 4.272,022 ms | ✔ |
| `SYSTEM` | schlimmster | 24h | 5 · 3 · 3 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateProcessMessageIDX` (12.956) | 41,567 ms | `Message` `range` `MessageLastUpdateProcessMessageIDX` (12.956) | `range` `MessageLastUpdateProcessMessageIDX` (12.956) | **43,066 ms** | 47,475 ms | ✔ |
| `SYSTEM` | schlimmster | 30d | 5 · 3 · 3 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (2) | 2,436 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.296,616 ms** | 1.306,893 ms | ✔ |
| `SYSTEM` | schlimmster | 90d | 5 · 3 · 3 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (2) | 2,728 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **4.491,257 ms** | 4.491,733 ms | ✔ |
| `NXHBE` | schlimmster | 24h | 23 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateProcessMessageIDX` (12.956) | 42,161 ms | `Message` `range` `MessageLastUpdateProcessMessageIDX` (12.956) | `range` `MessageLastUpdateProcessMessageIDX` (12.956) | **41,851 ms** | 42,360 ms | ✔ |
| `NXHBE` | schlimmster | 30d | 23 · 1 · 1 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (2) | 1,787 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.391,656 ms** | 1.393,274 ms | ✔ |
| `NXHBE` | schlimmster | 90d | 23 · 1 · 1 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (2) | 1,811 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **4.166,319 ms** | 4.167,953 ms | ✔ |
| `EDITIONLINGERI` | schlimmster | 24h | 32 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (13.534) | 41,819 ms | `Message` `range` `MessageLastUpdateIDX` (13.534) | `range` `MessageLastUpdateIDX` (13.534) | **40,577 ms** | 41,104 ms | ✔ |
| `EDITIONLINGERI` | schlimmster | 30d | 32 · 1 · 1 | 0 / 0 / 0 | `Message` `range` `MessageLastUpdateIDX` (437.150) | 1.280,259 ms | `Message` `range` `MessageLastUpdateIDX` (437.150) | `range` `MessageLastUpdateIDX` (437.150) | **1.272,282 ms** | 1.272,961 ms | ✔ |
| `EDITIONLINGERI` | schlimmster | 90d | 32 · 1 · 1 | 0 / 0 / 0 | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (3) | 1,589 ms | `Message` `range` `MessageLastUpdateIDX` (1.468.910) | `range` `MessageLastUpdateIDX` (1.468.910) | **3.981,606 ms** | 3.982,003 ms | ✔ |

### Die Vorregistrierung, dagegengehalten

| Erwartung | | Gemessen |
|---|---|---|
| `NEXANS` unter F4 wie L13: 24 h im Millisekundenbereich, schlimmster Fall rund 1,2 s über 30 und rund 3,9 s über 90 Tage | **trifft** | 37,762 ms · 1.230,561 ms · 3.934,451 ms |
| Die heute über 30 Tage prozessgetriebenen Mandanten werden mit Suche teurer, von Millisekunden auf bis zu den L13-Werten | **trifft — und reicht über 90 Tage etwas darüber** | `WOC` 14,597 → 1.275,492 ms (30 d) und 15,694 → 4.270,992 ms (90 d); `SYSTEM` 2,436 → 1.296,616 und 2,728 → **4.491,257 ms**; `NXHBE` 1,787 → 1.391,656 und 1,811 → 4.166,319 ms; über 90 Tage zusätzlich `ZAST` (28,565 → 3.966,031) und `EDITIONLINGERI` (1,589 → 3.981,606). Bis zu 14 % über dem L13-Wert von 3.937 ms |
| „Nur Prozesse" bleibt unter F4 an das Fenster gebunden: `MessageLastUpdateProcessMessageIDX` erlaubt, keiner der beiden ignorierten Indizes | **verfehlt über 90 Tage** | Keiner der beiden Indizes in irgendeinem Plan — das hält. **An das Fenster gebunden bleibt der Plan aber nur über 24 Stunden und 30 Tage.** Über 90 Tage liest `Message` bei `NEXANS` **vollständig** (`type ALL`, 3.560.486 geschätzte Zeilen), 3.478,564 ms. Siehe unten |
| *eigene:* Der schlimmste Fall kostet unter F4 bei allen zehn Mandanten ungefähr dasselbe | **trifft** | 24 h: 37,319 bis 43,066 ms (zehn Mandanten); 30 d: 1.219,939 bis 1.391,656 ms (neun); 90 d: 3.907,695 bis 4.491,257 ms (neun). Spanne je Fenster höchstens 16 % |

### Das Tor

**Jeder Mandant, jede Klasse und jedes Fenster bleibt unter F4 warm bei höchstens 5 s.** Der höchste
Wert ist `SYSTEM`, schlimmster Fall, 90 Tage: **4.491,257 ms** (Wanduhr 4.491,733 ms); auch der
langsamste **einzelne** Lauf der ganzen Messung liegt mit 4.545,445 ms darunter, ebenso der
langsamste Aufwärmlauf (4.471,595 ms, `SUTTONS`, wenige, 90 Tage). **Das Tor hält — mit 10 %
Abstand, und nur über 90 Tage knapp.** Der höchste Wert erreicht 45 % der Zeitgrenze des
Lese-Pools von 10 s.

**Damit geht es nach dem Auftrag weiter mit A4.** Der Abstand ist aber kleiner, als er aussieht, und
zwei Punkte gehören deshalb neben das Ergebnis, nicht in eine Fußnote:

1. **Die Zahlen gelten für den Bestand der Testkopie.** Die Produktion trägt 43 % mehr Zeilen in
   `Message` (5.083.782 gegen 3.560.486 laut `TABLE_ROWS`, M193). Wie dicht ihr 90‑Tage‑Fenster ist,
   ist nicht erhoben; skaliert es mit dem Bestand, läge der schlimmste Fall über 90 Tage zwischen
   5,6 s (`VOTG`, `NEXANS`) und 6,4 s (`SYSTEM`) — über dem Tor, unter der Zeitgrenze. **Das ist eine
   Rechnung, keine Messung.**
2. **90 Tage gibt es nur auf ausdrücklichen Wunsch** („Trotzdem suchen", `langeSuche`, §5). Ohne ihn
   ist bei 30 Tagen Schluss, und dort liegt der höchste Wert bei 1.423,037 ms (`NEXANS`, nur
   Prozesse).

### „Nur Prozesse" über 90 Tage — der volle Durchlauf, im Volltext

Unter F4 steigt der Optimierer bei einem Begriff, der nur Prozesse trifft, über die
**Mandantenkette** ein (`ProjectMandant` → `mandanten_process`; unter F0 war der Treiber `Message`
über `ProejctIDIDX`), puffert deren Zeilen und liest
`Message` in einem Zug dagegen (`Using join buffer (flat, BNL join)`), danach `Using temporary; Using
filesort`. Über 24 Stunden liest er `Message` dabei über `MessageLastUpdateProcessMessageIDX`
(12.902), über 30 Tage über `MessageLastUpdateIDX` (437.150) — **über 90 Tage gibt er den Index auf
und liest die ganze Tabelle.** Gedeutet, nicht per Trace belegt: Er schätzt das 90‑Tage‑Fenster auf
1.468.910 Zeilen, 41 % von `TABLE_ROWS` (gezählt stehen darin 680.447 Zeilen, 19 %), und hält den
vollen Durchlauf dann für billiger als den Bereich:

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|---|
| 1 | PRIMARY | `ProjectMandant` | `ref` | `PRIMARY,ProjectMandant_Mandant_idx` | `ProjectMandant_Mandant_idx` | 146 | const | 17 | Using where; Using index; Using temporary; Using filesort |
| 1 | PRIMARY | `mandanten_process` | `ref` | `PRIMARY,Process_ProjectFK` | `Process_ProjectFK` | 147 | `GlassfishDB.ProjectMandant.ProjectID` | 5 | Using where; Using index |
| 1 | PRIMARY | **`Message`** | **`ALL`** | `MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX` | NULL | NULL | NULL | **3.560.486** | Using where; Using join buffer (flat, BNL join) |
| 1 | PRIMARY | `Process` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | `GlassfishDB.mandanten_process.ProcessID` | 1 | |
| 1 | PRIMARY | `Project` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | `GlassfishDB.Process.ProjectID` | 1 | Using where |
| 1 | PRIMARY | `SOS` | `eq_ref` | `PRIMARY` | `PRIMARY` | 146 | `GlassfishDB.Message.SOSID` | 1 | Using where |
| 1 | PRIMARY | `SOSAction` | `eq_ref` | `PRIMARY,SOSAction_SOSFK` | `PRIMARY` | 148 | `GlassfishDB.Message.SOSID,GlassfishDB.Message.SOSActionID` | 1 | Using where |

Unter F0 läuft derselbe Fall in 1,640 ms über `range` auf `ProejctIDIDX` (14 geschätzte Zeilen) —
die Form `(… in (…) or false)` faltet MariaDB zu einem reinen Prozessfilter, und den trägt genau der
Index, den F4 ausschließt. **Das ist derselbe Mechanismus wie beim Prozessfilter in §5a**, nur
ausgelöst durch einen Suchbegriff statt durch die Prozessauswahl. Kosten unter F4: 45 ms über
24 Stunden, 1,4 s über 30 Tage, 3,5 s über 90 Tage — **und über 90 Tage hängen sie am Bestand der
ganzen Tabelle, nicht am Fenster.** Er ist der einzige gemessene Fall, der mit `Message` wächst,
auch wenn das Fenster gleich bleibt; mit dem Bestand der Produktion gerechnet läge er bei rund
5,0 s. Offener Punkt **225**; Fall k des Diagnoseskripts (M196) zeigt seinen Plan in
Produktion, über 24 Stunden und — mit `@fensterlaenge_stunden = 2160` — über 90 Tage.

### Was F4 außerdem zeigt, ohne gefragt zu sein

- **`SUTTONS` mit vielen Zeilen wird unter F4 schneller, nicht langsamer:** 915,042 → 29,494 ms über
  30 Tage, 1.073,862 → 31,783 ms über 90 Tage. Unter F0 wählt die Testkopie dort den
  prozessgetriebenen Plan und liest alle Zeilen des Mandanten; unter F4 füllt sich die Seite am
  oberen Fensterrand. Der Fall aus offenem Punkt 57, diesmal mit Suche.
- **`SUTTONS` mit wenigen Zeilen wird teurer:** 976,520 → 1.366,152 ms über 30 Tage, 906,177 →
  3.932,435 ms über 90 Tage.
- **Für `SUTTONS` gibt es über 30 und 90 Tage keinen schlimmsten Fall:** Keiner seiner 21 Kandidaten
  trifft Prozesse und Abläufe und hat dort keine Zeile. Nach der Vorregistrierung steht das hier und
  wird durch nichts ersetzt. Der 24‑Stunden‑Fall nimmt den ersten Begriff ohne Zeile in 24 Stunden.

### Gemeldete Abweichungen

1. **Werkzeug.** Gemessen über JDBC aus einer Wegwerfklasse statt mit `mysql.exe` (siehe Rahmen).
   Die Laufzeiten stammen trotzdem aus dem Profil des Servers; die beste Wanduhr der JVM liegt in
   jedem Fall 0,4 bis 10,3 ms über dem besten Profilwert — es gibt keinen Fall, in dem das Profil
   die Laufzeit unterschlägt.
2. **Eine zweite Sitzung lief kurz parallel.** Um 17:32:55 lief eine einzelne Prüfabfrage für das
   Diagnoseskript (Auswahl des Begriffs für Fall k, `Process` und `SOS`, unter einer Sekunde) in
   einer eigenen Sitzung — während der ersten `NEXANS`-Fälle. Die fünf Läufe dieser Fälle streuen
   nicht auffällig (siehe Anhang); gewertet ist ohnehin der beste.
3. **Ein Probelauf ging voraus** *(nachgetragen nach der Gegenprüfung)*: von 17:30:25 bis 17:30:36
   Serverzeit, nach dem Commit der Vorregistrierung (17:28) und vor dem gewerteten Lauf, in einer
   eigenen Sitzung — derselbe gruppierte Durchlauf und die vier `SUTTONS`-Fälle über 24 Stunden, um
   den Messrahmen zu prüfen. **Nicht gewertet.** Er hat den Puffer für diese Fälle vorgewärmt; die
   Aufwärmläufe der Messung selbst tun dasselbe.

### Anhang: alle Läufe


| Mandant | Klasse | Fenster | F0 Aufwärmlauf | F0 fünf Läufe (ms) | F4 Aufwärmlauf | F4 fünf Läufe (ms) | Zeilen der Seite | Hülle F0 = F4 |
|---|---|---|---:|---|---:|---|---:|:---:|
| `NEXANS` | schlimmster | 24h | 40,123 | 39,215 · 39,517 · 39,104 · 38,256 · 37,809 | 40,978 | 39,964 · 38,038 · 37,762 · 37,845 · 37,774 | 0 | ja |
| `NEXANS` | schlimmster | 30d | 1.281,289 | 1.276,959 · 1.232,231 · 1.283,523 · 1.251,829 · 1.266,628 | 1.257,819 | 1.243,304 · 1.230,561 · 1.279,067 · 1.248,880 · 1.265,117 | 0 | ja |
| `NEXANS` | schlimmster | 90d | 3.940,960 | 4.007,706 · 3.947,015 · 4.172,804 · 3.988,533 · 3.964,531 | 3.903,811 | 3.992,734 · 3.935,665 · 3.934,451 · 4.011,815 · 3.959,225 | 0 | ja |
| `NEXANS` | viele | 24h | 23,964 | 24,092 · 24,369 · 23,523 · 23,111 · 24,830 | 24,307 | 24,076 · 24,846 · 24,865 · 22,751 · 24,095 | 51 | ja |
| `NEXANS` | viele | 30d | 23,802 | 23,336 · 24,812 · 24,813 · 24,613 · 24,443 | 23,486 | 24,728 · 24,538 · 23,785 · 24,350 · 24,251 | 51 | ja |
| `NEXANS` | viele | 90d | 24,052 | 23,984 · 23,693 · 24,074 · 24,293 · 24,391 | 25,690 | 23,858 · 23,398 · 23,754 · 23,905 · 23,239 | 51 | ja |
| `NEXANS` | wenige | 24h | 39,306 | 39,701 · 38,765 · 39,843 · 39,636 · 38,071 | 39,557 | 39,433 · 40,200 · 38,990 · 39,869 · 41,221 | 1 | ja |
| `NEXANS` | wenige | 30d | 1.270,203 | 1.233,683 · 1.211,158 · 1.274,339 · 1.263,905 · 1.311,952 | 1.245,725 | 1.238,128 · 1.249,956 · 1.261,054 · 1.250,863 · 1.374,881 | 5 | ja |
| `NEXANS` | wenige | 90d | 4.076,639 | 4.163,857 · 3.975,199 · 3.951,428 · 3.953,648 · 3.902,032 | 4.014,810 | 4.021,195 · 3.930,777 · 4.016,467 · 3.907,258 · 3.868,290 | 13 | ja |
| `NEXANS` | nur Prozesse | 24h | 2,255 | 1,607 · 1,950 · 1,581 · 1,530 · 1,595 | 45,069 | 44,958 · 45,997 · 46,704 · 44,604 · 45,732 | 0 | ja |
| `NEXANS` | nur Prozesse | 30d | 1,347 | 1,610 · 1,695 · 1,920 · 1,545 · 1,580 | 1.451,727 | 1.432,978 · 1.460,049 · 1.437,578 · 1.423,037 · 1.457,958 | 0 | ja |
| `NEXANS` | nur Prozesse | 90d | 1,482 | 1,848 · 1,642 · 1,640 · 1,785 · 1,721 | 3.549,833 | 3.486,911 · 3.486,123 · 3.478,564 · 3.505,315 · 3.502,448 | 0 | ja |
| `SUTTONS` | schlimmster | 24h | 37,557 | 37,711 · 37,531 · 37,796 · 39,571 · 36,897 | 37,346 | 37,643 · 37,319 · 37,680 · 38,608 · 41,487 | 0 | ja |
| `SUTTONS` | viele | 24h | 29,057 | 29,667 · 29,506 · 27,977 · 29,139 · 28,892 | 29,056 | 28,802 · 29,004 · 27,780 · 29,253 · 30,194 | 51 | ja |
| `SUTTONS` | viele | 30d | 940,641 | 926,490 · 915,042 · 922,123 · 937,621 · 991,615 | 31,763 | 33,794 · 30,167 · 29,791 · 29,494 · 29,506 | 51 | ja |
| `SUTTONS` | viele | 90d | 1.044,788 | 1.073,862 · 1.077,564 · 1.097,566 · 1.099,527 · 1.094,797 | 34,226 | 33,392 · 31,783 · 34,644 · 33,679 · 32,560 | 51 | ja |
| `SUTTONS` | wenige | 24h | 41,548 | 44,858 · 46,277 · 42,441 · 42,596 · 45,106 | 44,513 | 43,581 · 43,766 · 43,004 · 45,075 · 41,977 | 1 | ja |
| `SUTTONS` | wenige | 30d | 989,023 | 1.016,995 · 976,520 · 1.018,287 · 990,301 · 994,599 | 1.419,810 | 1.408,235 · 1.402,365 · 1.396,340 · 1.366,152 · 1.398,824 | 10 | ja |
| `SUTTONS` | wenige | 90d | 998,705 | 1.002,685 · 1.007,939 · 1.021,652 · 906,177 · 926,589 | 4.471,595 | 4.412,429 · 4.472,528 · 4.238,657 · 3.984,722 · 3.932,435 | 32 | ja |
| `SUTTONS` | nur Prozesse | 24h | 24,534 | 24,841 · 24,910 · 26,331 · 25,357 · 25,521 | 24,743 | 25,409 · 26,919 · 26,005 · 24,742 · 26,818 | 51 | ja |
| `SUTTONS` | nur Prozesse | 30d | 25,173 | 24,661 · 24,934 · 26,102 · 27,624 · 26,334 | 25,058 | 24,964 · 24,624 · 25,443 · 25,959 · 24,959 | 51 | ja |
| `SUTTONS` | nur Prozesse | 90d | 26,533 | 24,475 · 25,488 · 25,860 · 24,633 · 25,072 | 25,881 | 24,556 · 26,073 · 24,654 · 24,717 · 26,103 | 51 | ja |
| `VOTG` | schlimmster | 24h | 38,758 | 40,464 · 37,579 · 39,500 · 40,013 · 38,669 | 37,946 | 39,704 · 39,425 · 38,773 · 39,317 · 39,511 | 0 | ja |
| `VOTG` | schlimmster | 30d | 1.244,638 | 1.255,201 · 1.240,467 · 1.233,246 · 1.213,648 · 1.257,543 | 1.252,715 | 1.248,214 · 1.219,939 · 1.235,887 · 1.245,801 · 1.289,460 | 0 | ja |
| `VOTG` | schlimmster | 90d | 4.281,905 | 3.927,143 · 3.926,183 · 4.311,550 · 4.105,783 · 4.034,936 | 3.890,011 | 3.911,716 · 3.907,695 · 4.216,054 · 4.075,598 · 4.100,159 | 0 | ja |
| `IBIS` | schlimmster | 24h | 41,529 | 41,369 · 42,796 · 43,335 · 44,465 · 41,140 | 42,570 | 41,205 · 40,495 · 40,543 · 43,516 · 41,181 | 0 | ja |
| `IBIS` | schlimmster | 30d | 1.299,438 | 1.259,533 · 1.299,668 · 1.287,676 · 1.289,300 · 1.247,355 | 1.298,640 | 1.260,368 · 1.292,567 · 1.291,511 · 1.239,125 · 1.240,258 | 0 | ja |
| `IBIS` | schlimmster | 90d | 4.056,466 | 4.028,055 · 4.132,223 · 4.058,575 · 4.080,400 · 4.067,092 | 3.978,846 | 4.056,926 · 4.124,074 · 4.052,496 · 4.075,317 · 4.027,805 | 0 | ja |
| `IBISGUS` | schlimmster | 24h | 39,770 | 39,914 · 39,233 · 41,100 · 40,147 · 43,836 | 39,026 | 39,079 · 38,960 · 41,590 · 39,252 · 41,154 | 0 | ja |
| `IBISGUS` | schlimmster | 30d | 1.301,760 | 1.301,278 · 1.292,046 · 1.265,518 · 1.298,244 · 1.322,561 | 1.285,555 | 1.311,153 · 1.249,460 · 1.280,768 · 1.334,801 · 1.269,699 | 0 | ja |
| `IBISGUS` | schlimmster | 90d | 4.036,101 | 4.311,390 · 4.121,604 · 4.062,357 · 4.117,135 · 4.060,218 | 4.227,481 | 4.091,588 · 4.059,027 · 4.063,955 · 4.111,850 · 4.106,941 | 0 | ja |
| `ZAST` | schlimmster | 24h | 39,687 | 39,945 · 41,223 · 41,442 · 40,666 · 41,812 | 40,864 | 39,979 · 42,013 · 41,217 · 40,923 · 40,417 | 0 | ja |
| `ZAST` | schlimmster | 30d | 1.303,259 | 1.284,844 · 1.300,694 · 1.262,764 · 1.270,118 · 1.247,568 | 1.296,238 | 1.254,793 · 1.287,169 · 1.272,651 · 1.292,863 · 1.252,379 | 0 | ja |
| `ZAST` | schlimmster | 90d | 28,218 | 28,565 · 29,922 · 29,805 · 28,887 · 30,683 | 3.997,363 | 3.988,727 · 3.966,031 · 4.025,531 · 3.976,147 · 4.108,053 | 0 | ja |
| `WOC` | schlimmster | 24h | 41,203 | 41,744 · 41,595 · 41,822 · 40,685 · 40,331 | 40,859 | 40,366 · 40,837 · 39,237 · 38,874 · 39,570 | 0 | ja |
| `WOC` | schlimmster | 30d | 16,600 | 14,597 · 15,792 · 15,516 · 16,218 · 16,948 | 1.266,319 | 1.275,492 · 1.366,160 · 1.369,279 · 1.399,166 · 1.406,088 | 0 | ja |
| `WOC` | schlimmster | 90d | 15,992 | 16,283 · 18,453 · 16,717 · 16,797 · 15,694 | 4.389,483 | 4.330,478 · 4.336,145 · 4.270,992 · 4.315,423 · 4.323,987 | 0 | ja |
| `SYSTEM` | schlimmster | 24h | 48,732 | 44,608 · 44,160 · 42,374 · 41,567 · 43,618 | 44,055 | 45,641 · 45,921 · 43,066 · 44,053 · 45,111 | 0 | ja |
| `SYSTEM` | schlimmster | 30d | 2,300 | 2,647 · 2,449 · 2,537 · 2,436 · 2,872 | 1.354,129 | 1.353,790 · 1.296,616 · 1.339,326 · 1.341,833 · 1.303,843 | 0 | ja |
| `SYSTEM` | schlimmster | 90d | 2,243 | 3,012 · 2,728 · 2,750 · 2,806 · 2,953 | 4.408,133 | 4.517,153 · 4.491,257 · 4.545,445 · 4.511,036 · 4.543,593 | 0 | ja |
| `NXHBE` | schlimmster | 24h | 45,925 | 42,161 · 42,416 · 42,802 · 45,058 · 45,564 | 42,313 | 41,851 · 42,521 · 42,320 · 45,185 · 46,022 | 0 | ja |
| `NXHBE` | schlimmster | 30d | 1,434 | 1,813 · 1,787 · 2,993 · 3,689 · 1,880 | 1.411,926 | 1.391,656 · 1.430,542 · 1.483,262 · 1.402,496 · 1.408,349 | 0 | ja |
| `NXHBE` | schlimmster | 90d | 1,465 | 1,917 · 1,816 · 1,857 · 1,811 · 1,830 | 4.426,154 | 4.303,705 · 4.166,319 · 4.237,661 · 4.258,340 · 4.265,759 | 0 | ja |
| `EDITIONLINGERI` | schlimmster | 24h | 39,311 | 41,819 · 44,105 · 42,524 · 42,817 · 43,225 | 40,177 | 40,577 · 42,717 · 44,436 · 42,310 · 42,713 | 0 | ja |
| `EDITIONLINGERI` | schlimmster | 30d | 1.324,395 | 1.322,516 · 1.310,045 · 1.280,259 · 1.311,966 · 1.291,336 | 1.267,950 | 1.318,453 · 1.277,562 · 1.318,481 · 1.272,282 · 1.305,758 | 0 | ja |
| `EDITIONLINGERI` | schlimmster | 90d | 1,294 | 1,726 · 1,676 · 1,855 · 2,361 · 1,589 | 4.094,235 | 4.138,775 · 4.086,822 · 4.120,142 · 4.066,581 · 3.981,606 | 0 | ja |

---

## Die Verletzungsproben zu den Tests (A5)

Je ein Eingriff in `NachrichtenRepository.java`, gefahren von einem Läufer im Scratchpad:
Sicherungskopie, Eingriff als exakter Textersatz mit Zählprüfung, `diff` gegen die Sicherung als
Beleg, Lauf von `NachrichtenStatementsTest` (Surefire) und `NachrichtenPlanDbIT`,
`NachrichtenSuchindizesDbIT`, `NachrichtenIsolationDbIT` (Failsafe) mit
`-Dmaven.test.failure.ignore=true`, ausgewertet aus den XML-Berichten **dieses** Laufs, dann
Rückspielen aus der Sicherung und `cmp`. **Jedes `cmp` war gleich, der SHA-256 der Datei am Ende
gleich dem am Anfang**; kein Eingriff ist in einem Commit. Keine Probe hatte einen Kompilierfehler.

| Probe | Eingriff | Fälle | rot |
|---|---|---:|---|
| P0 | Leerprobe, kein Eingriff | 23 | **0** |
| **P1** | **Hinweis entfernt** (`.from(MESSAGE)`) | 23 | **2:** `suche_ohne_prozessfilter_traegt_den_hinweis` und `suche_schliesst_die_prozessindizes_aus` — *„Message darf mit Suchbegriff keinen Prozessindex erwaegen (NEXANS)"* |
| **P2** | **Hinweis immer gesetzt** | 23 | **3:** `ohne_suche_oder_mit_prozessfilter_kein_hinweis`, `ohne_hinweis_stehen_die_prozessindizes_zur_wahl` — *„ProejctIDIDX muss ohne Hinweis zur Wahl stehen (NEXANS, …)"* —, und `verengtes_fenster_zieht_suttons_auf_den_zeitindex`: SUTTONS steigt über 30 Tage nicht mehr über die Mandantenkette ein |
| **P3** | **Bedingung ohne die Prüfung auf den Prozessfilter** | 23 | **3:** `ohne_suche_oder_mit_prozessfilter_kein_hinweis`, `bedingung_ohne_hinweis` — *„[Suche mit Prozessfilter] Expecting value to be false but was true"* —, `ohne_hinweis_stehen_die_prozessindizes_zur_wahl` |
| P4 *(Zusatz)* | Mandantenkette im Hauptstatement entfernt | 23 | **6:** die **zwei** alten Isolationsfälle `keine_fremde_zeile_in_der_liste` und `trennung_gilt_in_beide_richtungen` mit echten fremden Zeilen, `fenster_und_kette_stehen_da`, zwei Plantests — und der neue Fall `suche_mit_hinweis_trennt_in_beide_richtungen`, aber nur an seiner **Vorbedingung** („findet eigene Zeilen"): Die undichte Liste reicht ihm einen fremden Prozessnamen als eigenen. `freitext_findet_keine_fremden_prozesse` bleibt grün, weil die Vorfilterung noch trennt. *(Berichtigt nach der Gegenprüfung: Hier stand „die drei alten Isolationsfälle".)* |
| P5 *(Zusatz)* | Mandantenfilter der **Vorfilterung** entfernt (beide Abfragen in `loeseSucheAuf`) | 23 | **0 — ein Befund.** Die Mandantenkette des Hauptstatements nimmt die fremden Zeilen wieder heraus; der Antwortrumpf zeigt das Leck nie. Daraufhin ergänzt: `vorfilterung_liefert_keine_fremden_kennungen`, am Repository und in beide Richtungen. **Wiederholt mit dem Fall: 24 Fälle, 1 rot**; Leerprobe davor 24, 0 rot |
| P6 *(Zusatz)* | P4 und P5 zusammen | 23 | **7**, darunter `suche_mit_hinweis_trennt_in_beide_richtungen` jetzt **mit echten fremden Kennungen** und `freitext_findet_keine_fremden_prozesse` |

**Nachgeschärft nach der Gegenprüfung.** Zwei Lücken waren geblieben: Der Fall der Oberfläche kann ein
Leck im Hauptstatement nicht sehen (seine Kennungen kommen aus der Vorfilterung, die nur eigene
liefert), und der Fall der Vorfilterung prüfte nur die Prozessabfrage — die Ablaufabfrage trägt
ihren eigenen Mandantenfilter, und P5 hatte beide zugleich entfernt. Ergänzt sind
`hauptstatement_mit_hinweis_traegt_die_mandantenkette` (am Repository: ein Suchtreffer aus fremden
Prozessen, die im Fenster Zeilen haben, unter dem Hinweis — die Liste muss leer bleiben, in beide
Richtungen) und die Ablaufseite in `vorfilterung_liefert_keine_fremden_kennungen` (gesucht wird
zusätzlich nach einem fremden Ablaufnamen; jede gelieferte Prozess- **und** Ablaufkennung muss dem
eigenen Mandanten gehören). Danach je Filter **einzeln**:

| Probe | Eingriff | Fälle | rot |
|---|---|---:|---|
| P0 | Leerprobe | 25 | **0** |
| P4 | Mandantenkette im Hauptstatement entfernt | 25 | **7** — jetzt auch `hauptstatement_mit_hinweis_traegt_die_mandantenkette`, **mit echten fremden Zeilen** (*„NEXANS darf ueber fremde Prozesskennungen keine Zeile bekommen"*) |
| P5a | nur der Mandantenfilter der **Prozessabfrage** der Vorfilterung entfernt | 25 | **1:** *„Die Prozessabfrage der Vorfilterung von NEXANS liefert nur eigene"* |
| P5b | nur der Mandantenfilter der **Ablaufabfrage** der Vorfilterung entfernt | 25 | **1:** *„Die Ablaufabfrage der Vorfilterung von NEXANS liefert nur eigene"* |

Jedes `cmp` gleich, der SHA-256 der Datei am Ende gleich dem am Anfang. **Jeder der drei
Mandantenfilter des Suchpfads macht jetzt für sich allein einen Test rot.**

Die Meldungen der Isolationsfälle tragen Kennungen und Prozessnamen; sie stehen nur in den
Rohausgaben im Scratchpad und sind hier gekürzt (G1).

---

## M196 — Das erweiterte Diagnoseskript gegen die Testkopie (A4)

Pflicht nach E‑239. `scripts/diagnose-produktion/suche.sql` trägt jetzt **elf** Fälle; a bis f
bleiben unverändert, dazu kommen:

| Fall | Was | Hinweis | Auswahl |
|---|---|---|---|
| **g** | `NEXANS` mit Suchbegriff, **wie gebaut** | ja, in der Schreibung von jOOQ | Begriff 1 |
| **h** | `SUTTONS` mit Suchbegriff, wie gebaut | ja | Begriff 2 |
| **i** | `NEXANS` mit Suchbegriff **und Prozessfilter**, kleiner Prozess | nein, wie die Anwendung es dann schickt | aus `message_rollup` im Schema `@monitor_schema`: der Prozess von Mandant 1 mit den **wenigsten** Nachrichten im Fenster (mindestens einer) |
| **j** | dasselbe, verkehrsreicher Prozess | nein | der mit den **meisten** |
| **k** | `NEXANS`, ein Begriff, der **nur Prozesse** trifft | ja, Form `(… or false)` | vom Skript gewählt: der erste **Projektname** von Mandant 1, dessen Projekt mindestens zwei Prozesse trägt und der in keinem Ablaufnamen vorkommt; sonst der erste solche Prozessname |

Gelesen werden dafür nur Stammdaten und der Rollup, **nie `Message`**; ausgegeben werden nur
Längen, Trefferzahlen und die Nachrichtenzahl des gewählten Prozesses laut Rollup (G1). Der Prozess
steht als **Literal** im Statement (`QUOTE`), wie der Treiber ihn auf die Leitung legt. Ist der
Rollup für den angemeldeten Benutzer nicht sichtbar, liefern i und j eine Zeile mit dem Grund statt
des Plans. Die Endmarke ist Ausgabe 26; vollständig heißt jetzt: jede Vorfilterung **und** jede
Auswahl ist gelaufen.

> **Eine Abweichung, beim Gegenlesen gefunden und korrigiert.** Die erste Fassung von k wählte den
> ersten Prozessnamen, der in keinem Ablaufnamen vorkommt. Auf der Testkopie traf er **genau einen**
> Prozess — dann wird die Mandantenkette konstant, und der Plan ist ein anderer als der, den M195
> als riskant ausweist (zwei Prozesse, Join-Puffer, über 90 Tage voller Durchlauf). Seitdem wählt k
> zuerst einen Projektnamen mit mindestens zwei Prozessen. Auf der Testkopie trifft er **46
> Prozesse und keinen Ablauf**.

### Gegen die Testkopie, mit dem Anker

Die Testkopie-Fassung setzt `@fensterende = TIMESTAMP '2025-12-30 04:09:47'` und dieselben beiden
Begriffe wie der Abgleich vom Nachmittag; sie liegt unter `ergebnis/` und ist von Git ignoriert.
Zwei Läufe, einer mit 24 Stunden, einer mit `@fensterlaenge_stunden = 2160`; **beide enden mit
„ENDE: alle Ausgaben vollstaendig"**; ein Lauf dauert rund 5 s.

| Fall | 24 Stunden: Zeile `Message` | 90 Tage: Zeile `Message` |
|---|---|---|
| g | `range` `MessageLastUpdateIDX` (13.534), `possible_keys` nur die beiden Zeitindizes | `range` `MessageLastUpdateIDX`, ebenso |
| h | `range` `MessageLastUpdateIDX` (13.534), ebenso | `range` `MessageLastUpdateIDX`, ebenso |
| i | `ref` `ProejctIDIDX` (333), Prozess mit 1 Nachricht im Fenster | `ref` `ProejctIDIDX` (93) |
| j | `range` `MessageLastUpdateProcessMessageIDX` (12.956), Prozess mit 4.053 Nachrichten | `range` `MessageLastUpdateIDX` (1.468.910) |
| k | `range` `MessageLastUpdateIDX` (13.534), Treiber `Message` | **`ALL` (3.560.486)** — Treiber `ProjectMandant` → `mandanten_process`, `Using join buffer (flat, BNL join)`, `Using temporary; Using filesort` |

**k zeigt über 90 Tage denselben Plan wie „nur Prozesse" in M195** — mit einem anderen Begriff
(46 statt 2 Prozesse); über 24 Stunden treibt in k `Message`, in M195 die Mandantenkette. In g, h und k steht keiner der beiden
Prozessindizes in `possible_keys` oder `key`; in i und j stehen beide zur Wahl.

### Der Abgleich gegen jOOQ, wiederholt

Wie am Nachmittag: im Skript `CHAR_LENGTH` und `MD5` jedes Statements (ein angehängter Block in der
Testkopie-Fassung, vor dem Aufräumen); in einer Wegwerfklasse dieselbe Auflösung über
`loeseSucheAuf`, dieselbe Auswahl für i, j und k über dieselben SQL-Abfragen, dann `finde` gegen
eine Attrappe gerendert und die Bindewerte so eingesetzt, wie Connector/J 3.5.8 sie auf die Leitung
legt. Für a, c und e die Fassung vor dem Bau (Hinweis entfernt bzw. in Großschreibung), für g bis k
das Statement **ohne jeden Eingriff**.

| Fall | Länge | MD5 (Skript = jOOQ) | |
|---|---:|---|---|
| a | 1.770 | `4faab29e4df79bc8720fe99de2437570` | gleich dem Abgleich vom Nachmittag |
| b | 1.489 | `80edeaf99aeba0f8fcc6b2da3216beba` | ebenso |
| c | 1.719 | `e840970b84fde8fff70781362b3286cb` | ebenso |
| d | 1.490 | `30939af1442f171c194024aebfb59e57` | ebenso |
| e | 1.830 | `12190898dec9bb47fef0a1263e426b78` | ebenso |
| f | 1.489 | `a227b510b471bb8307548090c8c13c2b` | ebenso |
| g | 1.830 | `aee841b29c9ddadbda8aa3a3a599e95b` | neu |
| h | 1.779 | `28e1d94c477abcd18198e2d5988b02eb` | neu |
| i | 1.843 | `8c3a816160f80d569fd0a13b57c268d6` | neu |
| j | 1.841 | `3f4c36dbec2324601a4e141eb2255807` | neu |
| k | 2.711 | `6661bb5e2c4475e957901545b9de3b4d` | neu |

**Alle elf zeichengleich, auch in der Reihenfolge der Kennungen.** a bis f sind dieselben
Prüfsummen wie vor der Erweiterung — die bestehenden Fälle sind unberührt.

### Der Ablauf für Lukas im Workbench

1. Die Datei **vom Zweig `fix/suche-plan`** nehmen und wie bisher in einen neuen SQL-Reiter
   kopieren; im Repository nichts bearbeiten.
2. `@begriff_1` und `@begriff_2` eintragen, dieselben wie am 13.10.2026. `@fensterende = NOW()` und
   `@fensterlaenge_stunden = 24` stehen lassen. `@monitor_schema` nur ändern, wenn das Schema der
   Anwendung in der Produktion nicht `overlord_monitor` heißt.
3. „Execute (All or Selection) to Text", den Reiter „Query Output" vollständig kopieren. Er endet
   mit **„ENDE: alle Ausgaben vollstaendig"** (Ausgabe 26).
4. **Ein zweites Mal mit `@fensterlaenge_stunden = 2160`** (90 Tage) — für Fall k, der dort auf
   der Testkopie `Message` vollständig liest. Es läuft wieder nur `EXPLAIN`.
5. Zurück nur die beiden Texte, nach dem Begriff durchsucht wie beim ersten Mal.

**Worauf es ankommt:** In g, h und k darf in der Zeile `Message` weder `ProejctIDIDX` noch
`Message_ProcessFK` stehen (sonst hält der Hinweis in Produktion nicht). Die Zeile `Message` in g
und h sollte `range` über `MessageLastUpdateIDX` zeigen wie Fall e. In k über 90 Tage: `ALL`
oder ein Bereich? Und für i und j: Wählt die Produktion dort den prozessgetriebenen Plan, und mit
welchen Schätzungen? Ausgabe 3 sagt für i und j, ob der Rollup sichtbar war.
