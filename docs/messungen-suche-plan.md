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
| Eichung | `git grep -cE '\bM(19[0-2])\b' main -- docs` → Treffer in `PROJEKTBESCHREIBUNG.md` (5), `README.md` (4), `bam-suche.md` (2). `\b` greift in `git grep -E` also |
| Folge | **M193 bis M196** |
| Entscheidungen | `git grep -E 'E(‑\|-)[0-9]{1,3}\b'` auf allen Zweigen: Höchstwerte 231 bis 236, dann **249** und **780**. Beide gelesen: **E‑249** steht in [`spaltenwahl.md`](spaltenwahl.md) Zeile 749 als Bereichsangabe („E‑231 bis E‑249 auf keinem Stand"), **E‑780** ist der bekannte Falschtreffer (ein Datenwert in einer Rohausgabe). Ganzer Baum aller Zweige mit `E[^0-9]{1,3}(23[7-9]\|24[0-9])\b` (Code-Kommentare eingeschlossen): nur die Zeile in `spaltenwahl.md` |
| Eichung | `git grep -cE 'E[^0-9]{1,3}(23[4-6])\b' main` → Treffer in `BamSucheService.java`, `MessageStatusClassifier.java` (3), `MessageStatusKind.java` |
| Folge | **E‑237 bis E‑239**, je eine Entscheidung vom 13.10.2026 |
| Offene Punkte | Projektweit nummeriert. Zwei Schreibweisen: `- **218 — …**` ([`message-status.md`](message-status.md)) und `- **76. …**`. Suche über beide auf allen Zweigen: Höchstwert **220**. Treffer darüber gelesen und ausgesondert: `155.871`, `234.159`, `264.469` (Zahlen in [`messungen-schritt7.md`](messungen-schritt7.md) Zeile 4402 bis 4407) und „22 Punkte, 231 Fenster" in `BaumfensterTest.java` |
| Eichung | `git grep -cE '^\s*[-*] (~~)?\*\*2(1[89]\|20)( \|\.\|\*)' main -- docs` → 3 Treffer in `message-status.md` |
| Folge | **Punkte 221 bis 224** |
| Bekannte Falschtreffer laut Auftrag | `E‑780`; Zeile 21 in [`messungen-schritt10.md`](messungen-schritt10.md) („Der Bereich … ist frei"); M179 als Fließtext. Alle drei liegen unterhalb der vergebenen Bereiche und sind ohne Folge |

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

*Folgt nach dem Lauf.*

---

## M196 — Das erweiterte Diagnoseskript gegen die Testkopie

*Folgt nach dem Lauf.*
