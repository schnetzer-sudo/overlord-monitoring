# Messungen — Fensterverengung für die Nachrichtenliste (M99 bis M103)

Stand **27.08.2026** · Auftrag „Fensterverengung für die Nachrichtenliste" · Branch
`fix/liste-fensterverengung` · Ergebnis in [`nachrichtenliste.md`](nachrichtenliste.md) §5c

> ## ⚠️ Das Tor schließt. Es ist nichts gebaut worden.
>
> Die Annahme des Auftrags **trägt** — der Optimierer wechselt bei engem Fenster von selbst auf
> die zeitgetriebene Form, ohne Hint und ohne `STRAIGHT_JOIN`, und er bleibt bei der
> prozessgetriebenen, wo die richtig ist. Der teuerste bekannte Fall des Projekts fällt von
> **7.519,513 ms auf 6,027 ms**.
>
> Woran es scheitert, ist nicht der Plan, sondern **die Vorabfrage selbst**. Sie kostet jeden
> Mandanten etwas, und die dünnen Mandanten kostet sie am meisten — weil der Nachweis „weniger
> als 51 Zeilen in dreißig Tagen" den ganzen Rollup-Bereich lesen muss. Genau diese Mandanten
> sind heute schon schnell. **Sechs Fälle werden messbar schlechter**, zwischen Faktor 1,03 und
> 17,9. Das ist die zweite Zeile des Tors, Wort für Wort.

---

## Nummernvergabe

| | |
|---|---|
| Prüfung | `grep -rnoE '\bM(99\|10[0-3])\b' docs/ scripts/ *.md` |
| Ergebnis | **sieben Treffer auf `M99`, keiner davon eine Vergabe.** `M100` bis `M103`: **null Treffer**. **M99 bis M103 sind hier vergeben** |
| Gegenprobe | `grep -rnoE '\bM9[0-8]\b' docs/ scripts/ *.md` → **1.002 Treffer**. Der Ausdruck greift |

> ### Der Auftrag erwartet **einen** Treffer auf `M99`. Es sind **sieben** — und alle sieben sind richtig
>
> Der Auftrag schreibt: *„Erwartet: **ein** Treffer auf `M99`, und er ist keine Vergabe."* Gelesen
> statt gezählt, wie er es selbst verlangt, sind es sieben:
>
> | Fundstelle | Was dort steht |
> |---|---|
> | [`messungen-schritt10.md`](messungen-schritt10.md) Z. 21 | „Der Bereich M86–**M99** ist frei" — die Quelle |
> | [`messungen-schritt10b.md`](messungen-schritt10b.md) Z. 35, 43, 46, 47, 48 | **fünf** Treffer, alle im ⚠️-Kasten, der die Fundstelle zitiert und die Falle beschreibt |
> | [`messungen-schritt9.md`](messungen-schritt9.md) Z. 2225 | die M93-Runde, die dieselbe Zeile zitiert |
>
> **Die Erwartung stammt aus einer Zeit, als es die fünf Zitate noch nicht gab.** Schritt 10b hat
> einen ganzen Kasten darüber geschrieben, dass man die Fundstelle lesen und nicht zählen muss —
> und hat damit die Zahl der Fundstellen von zwei auf sieben gehoben. Der Auftrag hat die alte
> Zahl übernommen. Folgenlos, aber es ist derselbe Mechanismus zum dritten Mal.

**Offene Punkte** setzen bei **70** an — **nicht** bei 63, wie der Auftrag schreibt. Projektweit
höchster vergebener Stand ist **69** ([`rollup.md`](rollup.md) §13, Z. 1020). Die Punkte 63 bis 66
stehen in [`nachrichtenliste.md`](nachrichtenliste.md) §9, die Punkte 67 bis 69 in
[`rollup.md`](rollup.md) §13 — alle sieben aus Schritt 10b‑1, alle in der **Feature**-Form
`- **NN.` und deshalb von einem Ausdruck auf `^NN\. \*\*` nicht zu finden. Genau dieser Ausdruck
steht hinter der Zahl 63 im Auftrag.

**Ebenso überholt:** Der Auftrag nennt für die Feature-Doku den Abschnitt „§5b". Der ist seit
Schritt 10b‑1 mit `ueberfaellig` belegt ([`nachrichtenliste.md`](nachrichtenliste.md) Z. 1066).
Dieser Befund steht deshalb in **§5c**.

---

## Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, `SELECT @@global.read_only` = **1** als erste und als letzte Abfrage jeder Sitzung |
| Benutzer | `monitor_read`, ausschließlich `SELECT` / `SET` / `EXPLAIN` / `PREPARE` / `DEALLOCATE` (Regel S1) |
| Version | `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| Datenstand | `Message` **3.341.519** Zeilen, `MAX(MessageLastUpdate)` = **`2026-07-08 17:21:10`** — identisch mit M83 bis M98 |
| Rollup | `message_rollup` **335.610** Zeilen, `SUM(anzahl)` = **3.341.519** — deckungsgleich mit dem Bestand |
| Wasserstand | `MAX(fenster_bis)` über abgeschlossene, fehlerfreie Läufe = **`2026-08-27 15:00:00`** |
| Anker | **`2025-12-30 04:09:47`** (V5 der Vorrunde), als Literal (Regel Z1) |
| Fenster | `zeitraum=30d` dagegen, **beide Grenzen einschließlich** — wie `.ge`/`.le` es setzen |
| Statement | abgeschrieben aus dem Repository: vier `LEFT JOIN`, `EXISTS`-Mandantenkette, `ORDER BY (MessageLastUpdate, MessageID) DESC`, `LIMIT 51` |
| Laufzeit | Aufwärmlauf, dann **beste von fünf**, aus `information_schema.PROFILING`; `anzahl_laeufe` ist in **jedem** Block 6 |
| Sitzungen | sieben, unter `scripts/messung-liste-verengung/`. Rohausgaben über `.gitignore` ausgeschlossen |
| `STRAIGHT_JOIN` | **in keiner Fassung** (M42) |
| Indexhinweis | **in keiner Fassung.** Der Optimierer wird nirgends überstimmt — das ist der ganze Punkt |
| `ANALYZE TABLE` | **nicht gefahren** — `monitor_read` hat auf `GlassfishDB` nur `SELECT` |
| Geschrieben | **nichts.** Weder in `GlassfishDB` noch in `overlord_monitor`. Kein Index, keine Probetabelle |

### Zwei ausgewiesene Abweichungen

**A1 — der Plan ist an der Hülle erhoben, nicht am nackten Statement.** Schritt 10b‑1 hat beide
nebeneinander erhoben und festgehalten, dass sie „Zeile für Zeile gleich sind, bis auf die eine
`<derived2>`-Zeile der Hülle" ([`nachrichtenliste.md`](nachrichtenliste.md) Z. 583). Diese Runde
nutzt das aus: Sie erhebt nur den Plan der Hülle und liest den Treiber in **Zeile 2**. Damit
halbiert sich die Zahl der Pläne bei gleicher Aussage. Die nackten Statements stehen unverändert in
den Sitzungsdateien und lassen sich jederzeit einzeln planen.

**A2 — die Treiberzeile ist hier nicht immer die entscheidende.** Die Übersicht in §5a nimmt die
**erste** Planzeile als „Treiber". Für den teuersten Fall dieser Runde wäre das irreführend: Bei
`F0-NEXANS-30d-prozess-gross` bleibt die erste Zeile (`pm` / `ref`) unverändert, und trotzdem fällt
die Laufzeit um Faktor 1.247. Die Entscheidung liegt in **Zeile 4**, beim Zugriff auf `Message`.
Diese Runde führt deshalb bei den tragenden Fällen den Plan im Volltext.

### Regelbezug

| Regel | Stand | |
|---|---|---|
| **S1** | erfüllt | Nur `SELECT`/`SET`/`EXPLAIN`/`PREPARE`/`DEALLOCATE`. `read_only` = 1 am Anfang und am Ende jeder Sitzung belegt |
| **G1** | erfüllt | Keine `ProcessID`, keine `MessageID`, kein Partnername in Datei oder Ausgabe. Prozesslisten leben in `@proz`; Zeilenvergleiche laufen über **MD5** über die geordneten `MessageID` |
| **Z1** | erfüllt | Anker als Literal, nie `NOW()` |
| **L2** | erfüllt | Zeilenzahlen und Prozesswahl kommen aus `message_rollup`, nicht aus einem zweiten Durchlauf über `Message` |
| **L7** | erfüllt | Jede gemessene Abfrage mit `EXPLAIN` **und** Laufzeit, über **zehn** Mandanten statt zwei |
| **L15** | erfüllt, mit A1/A2 | Pläne erhoben; die tragenden im Volltext, für alle übrigen die Treiberzeile |

### Die Sitzungen

| Sitzung | Datei | Inhalt |
|---|---|---|
| M99 · 1 | `m99-mandanten-1.sql` | NEXANS, SUTTONS, VOTG, IBIS, IBISGUS |
| M99 · 2 | `m99-mandanten-2.sql` | ZAST, WOC, SYSTEM, NXHBE, EDITIONLINGERI |
| M99 · Nachtrag 1 | `m99b-vorabfrage-zerlegt.sql` | Woran hängen die Kosten der Vorabfrage? |
| M99 · Nachtrag 2 | `m99c-vorabfrage-exists.sql` | Die Vorabfrage als `EXISTS`-Kette, alle Stufen, alle zehn |
| M100 | `m100-prozessfilter.sql` | Der Prozessfilter, der 7,46‑s‑Fall |
| M101 | `m101-cursor.sql` | Zwei Seiten, jede mit eigener Verengung |
| M102 | `m102-wasserstand.sql` | Die Wasserstandsgrenze, drei Lagen |
| M103 | `m103-rueckfall.sql` | `ueberfaellig` und der Statusfilter |

---

## Die Verengung, und warum sie nie eine Zeile verlieren kann

`message_rollup` hält `(stunde, process_id, message_status, anzahl)`. Der Eimer `stunde` deckt
**`[stunde, stunde+1h)`** und zählt `Message.MessageLastUpdate` — nachgemessen an drei Stunden,
dreimal auf die Zeile gleich. `MessageLastUpdate` ist im Bestand **nirgends `NULL`** (`COUNT` = 0),
und `SUM(anzahl)` über den ganzen Rollup trifft `COUNT(*)` über `Message` auf die Zeile genau.
Der Rollup ist damit für dieses Feld lückenlos.

Vom Fensterende rückwärts wird über die Stundeneimer summiert; die Stunde, bei der die kumulierte
Zahl **≥ 51** erreicht, ist die neue Untergrenze:

```sql
SELECT SUM(g.anzahl) AS zeilen,
       MAX(CASE WHEN g.kum >= 51 THEN g.stunde END) AS untergrenze
FROM ( SELECT s.stunde, s.anzahl,
              SUM(CASE WHEN s.voll THEN s.anzahl ELSE 0 END)
                  OVER (ORDER BY s.stunde DESC) AS kum
       FROM ( SELECT r.stunde, SUM(r.anzahl) AS anzahl,
                     (r.stunde >= '2025-11-30 05:00:00'
                      AND r.stunde <= '2025-12-30 03:00:00') AS voll
              FROM overlord_monitor.message_rollup r
              WHERE r.stunde >= '2025-11-30 04:00:00'
                AND r.stunde <= '2025-12-30 04:00:00'
                AND r.process_id IN (…)
              GROUP BY r.stunde ) s ) g;
```

**Die Spalte `voll` ist der ganze Sicherheitsbeweis.** Das Fenster beginnt um `04:09:47` und endet
um `04:09:47`; die beiden Randstunden liegen also nur zum Teil darin. Sie gehen mit **0** in die
kumulierte Summe ein. Damit ist die gezählte Menge stets eine **Unterschranke** der wirklichen —
die Verengung geht nie zu weit, und `[Untergrenze, bis]` enthält immer mindestens 51 Zeilen.

Ginge man andersherum vor und zählte die angebrochene **obere** Randstunde voll mit, wäre es eine
Oberschranke: Läge dort ein Eimer mit sechzig Nachrichten, von denen nur fünf vor `04:09:47`
liegen, hielte man 51 Zeilen für gefunden und lieferte fünf.

Drei Grenzen, an denen die Verengung entfällt:

| | Regel |
|---|---|
| **Wasserstand** | Stunden ab `MAX(fenster_bis)` gehen ebenfalls mit 0 ein — derselbe Mechanismus wie bei den Randstunden, dieselbe Richtung. Über den Wasserstand hinaus wird nie verengt (M102) |
| **Unter 51** | Erreicht die Summe nie 51, gibt es keine Untergrenze und das Fenster bleibt, wie es ist |
| **Nullfall** | Sagt der Rollup über den **ganzen überlappenden** Bereich null Zeilen, wird die Quellabfrage gar nicht gestellt. Für diese Aussage zählen auch die Randstunden mit, sonst wäre „null" nicht gedeckt |

**Die Mandantenkette steht als `EXISTS`, nicht als `JOIN`.** `ProjectMandant` ist n:m; ein `JOIN`
vervielfachte Rollup-Zeilen und damit die Summe — eine **Über**schätzung, also genau die unsichere
Richtung.

---

## M99 — Trägt die Verengung, ohne Filter?

Alle zehn Mandanten, 30 Tage, ohne Filter. F0 und die verengte Fassung in **derselben** Sitzung.

### Was der Rollup sagt

| Mandant | Zeilen im Fenster | Untergrenze | Fenster | trägt ab Stufe |
|---|---:|---|---:|---|
| NEXANS | 180.154 | `2025-12-30 03:00:00` | **1 h** | 1 h |
| SUTTONS | 21.524 | `2025-12-30 02:00:00` | **2 h** | 24 h |
| VOTG | 6.106 | `2025-12-29 21:00:00` | 7 h | 24 h |
| IBIS | 4.331 | `2025-12-29 14:00:00` | 14 h | 24 h |
| IBISGUS | 1.722 | `2025-12-29 09:00:00` | 19 h | 24 h |
| ZAST | 283 | `2025-12-23 05:00:00` | 167 h | 7 d |
| WOC | 118 | `2025-12-11 08:00:00` | 452 h | 30 d |
| SYSTEM | 5 | — | 720 h | **keine Verengung** |
| NXHBE | 0 | — | 720 h | **Nullfall** |
| EDITIONLINGERI | 0 | — | 720 h | **Nullfall** |

Die Spalte „Zeilen im Fenster" trifft die aus §5a **für alle zehn Mandanten auf die Zeile** —
unabhängig hergeleitet, gleiches Ergebnis.

### Was die Quellabfrage dann kostet

| Mandant | **F0 heute** | **Quelle verengt** | Plan F0 → verengt |
|---|---:|---:|---|
| NEXANS | 1,634 ms | 1,615 ms | `m`/range, 437.150 → **358** geschätzte Zeilen |
| **SUTTONS** | **1.043,011 ms** | **6,496 ms** | **`pm`/ref → `m`/range** — die Planfamilie wechselt; verengt steht `m` als `range` mit **470** geschätzten Zeilen |
| VOTG | 33,672 ms | 32,996 ms | `m`/range, → 7.986 |
| IBIS | 41,377 ms | 41,129 ms | `m`/range, → 9.008 |
| IBISGUS | 52,129 ms | 51,100 ms | `m`/range, → 11.490 |
| ZAST | 270,891 ms | 270,824 ms | `m`/range, → 48.876 |
| WOC | 15,102 ms | 14,982 ms | `pm`/ref **unverändert** |
| SYSTEM | 1,695 ms | 1,556 ms | `pm`/ref **unverändert** |
| NXHBE | 0,980 ms | *(Nullfall)* | — |
| EDITIONLINGERI | 2.126,876 ms | *(Nullfall)* | — |

**Drei Befunde, und sie sind der Kern der Runde:**

1. **Der Optimierer wechselt von selbst, wo es richtig ist.** `SUTTONS` verlässt die
   prozessgetriebene Form und wird um **Faktor 161** schneller — ohne Hint, ohne `STRAIGHT_JOIN`,
   ohne dass ein Index angefasst wurde. Die Schätzung fällt von 437.150 auf 470, und daran wählt er.
2. **Er bleibt, wo das richtig ist.** Der tragende Beleg ist **`WOC`**: Sein Fenster wird von 720
   auf **452 Stunden** verengt, und er behält trotzdem seinen prozessgetriebenen Plan
   (`pm`/ref, unverändert) und seine 15 ms. Fassung F6 aus 10b‑1 hatte genau ihn um Faktor 92
   erzwungen umgestellt und auf 1.488,784 ms getrieben. **Ein Fenster zu geben ist etwas anderes,
   als eine Form zu erzwingen** — das ist der Unterschied, auf den §2 gesetzt hat, und er ist
   belegt. *(Bei `SYSTEM`, `NXHBE` und `EDITIONLINGERI` bleibt der Plan ebenfalls unverändert, aber
   dort ist das ohne Aussagekraft: Sie werden gar nicht erst verengt.)*
3. **`ZAST` bewegt sich nicht — und das ist kein Zufall, sondern strukturell.** `ZAST` ist heute
   schon zeitgetrieben. Der zeitgetriebene Plan liest vom Fensterende rückwärts und hört auf,
   sobald er 51 Zeilen hat — er läuft also **ohnehin nur bis zur 51. Zeile**, und genau dort setzt
   die Verengung die Untergrenze. Beide lesen dieselben 167 Stunden. **Die Verengung kann einem
   Mandanten, der bereits zeitgetrieben ist, prinzipiell nichts sparen.** Ihr Gewinn liegt
   ausschließlich bei (a) Mandanten auf der prozessgetriebenen Form und (b) dem Nullfall, wo der
   zeitgetriebene Plan das ganze Fenster vergeblich liest.

### Was die Verengung selbst kostet

Zwei Fassungen der Vorabfrage, je über vier Stufen (1 h, 24 h, 7 d, 30 d). Eine Stufe wird nur
gefahren, wenn die darüber nicht trägt.

| Mandant | Literalliste je Stufe (1 h / 24 h / 7 d / 30 d) | `EXISTS` je Stufe |
|---|---|---|
| NEXANS | 2,192 · 2,688 · 5,148 · 22,554 | **1,002** · 4,244 · 18,472 · 117,440 |
| SUTTONS | **0,781 · 1,328** · 3,871 · 19,150 | 1,057 · 4,161 · 17,736 · 104,194 |
| VOTG | 1,742 · 2,283 · 4,908 · 20,978 | 0,972 · 4,280 · 18,918 · 112,179 |
| IBIS | 1,218 · 1,671 · 3,524 · 17,140 | 0,934 · 4,161 · 17,339 · 108,220 |
| IBISGUS | 0,965 · 1,380 · 3,011 · 15,376 | 0,939 · 4,069 · 16,823 · 105,346 |
| ZAST | 0,775 · 1,113 · 2,658 · 13,536 | 0,974 · 3,857 · 16,508 · 102,942 |
| WOC | 0,697 · 1,054 · 2,508 · **13,015** | 0,960 · 3,422 · 13,895 · 88,877 |
| SYSTEM | 0,688 · 0,993 · 2,427 · 12,626 | 1,002 · 3,632 · 15,349 · 97,366 |
| NXHBE | 0,730 · 1,069 · 2,541 · 13,244 | 0,997 · 3,672 · 15,317 · 97,756 |
| EDITIONLINGERI | 0,747 · 1,047 · 2,481 · 13,179 | 0,898 · 3,781 · 16,574 · 101,920 |

Keine Fassung gewinnt überall: Die **`EXISTS`-Kette** ist bei engem Bereich billiger, die
**Literalliste** bei weitem — und zwar um Faktor 6 bis 8.

### Nachtrag 1 — woran die Kosten hängen (`m99b-vorabfrage-zerlegt.sql`)

Vier Fassungen über **denselben** Bereich von einer Stunde, also über dieselben achtundzwanzig
Rollup-Zeilen. NEXANS, weil er mit 733 Prozessen den Extremfall stellt:

| Fassung | beste von fünf |
|---|---:|
| **C** — ohne Mandantenfilter, der Boden | **0,722 ms** |
| **B** — `EXISTS`-Kette | 0,993 ms |
| **A** — 733 Literale (Statement: **19.285 Zeichen**) | **2,198 ms** |
| **D** — nur die Prozessliste beschaffen | 2,037 ms |

**Der Aufschlag der Literalliste liegt nicht im Lesen, sondern im Statement.** Achtundzwanzig
Zeilen kosten 0,722 ms; dieselben achtundzwanzig Zeilen hinter einer 19‑KB‑`IN`-Liste kosten
2,198 ms. Dazu kommen 2,037 ms, um die Liste überhaupt zu beschaffen. Für einen engen Bereich zahlt
die Literalliste also rund **3,5 ms**, um **0,27 ms** Lesearbeit zu sparen.

**Das schließt zugleich eine naheliegende Abhilfe aus.** `message_rollup` liegt in
`overlord_monitor`, wo geschrieben werden darf; ein Index `(process_id, stunde)` wäre erlaubt. Er
könnte aber nur den **Lese**anteil senken — und der ist bei NEXANS auf der tragenden Stufe bereits
0,27 ms. Der Aufschlag steckt im Parsen einer 19‑KB‑Liste, und dagegen hilft kein Index.
Für die **dünnen** Mandanten läge der Fall anders; siehe offenen Punkt **73**.

---

## M100 — Der Prozessfilter, der 7,46‑s‑Fall

Dieselben zwei Prozessmengen wie in 10b‑1, wortgleich hergeleitet: die drei mit den **meisten** und
die drei mit den **wenigsten** Nachrichten im Fenster.

| Fall | Zeilen im Fenster | Untergrenze | **F0 heute** | **Vorabfrage** | **Quelle verengt** | **Summe** | |
|---|---:|---|---:|---:|---:|---:|---|
| **drei große** | 118.119 | `2025-12-30 03:00:00` (1 h) | **7.519,513 ms** | 0,770 ms | **6,027 ms** | **6,797 ms** | **Faktor 1.106** |
| drei kleine | 3 | — *(unter 51)* | 1,730 ms | 16,836 ms | 1,720 ms | 18,556 ms | Faktor 10,7 schlechter |

**Das ist das größte Einzelergebnis der Runde und der wichtigste Fall überhaupt** — die Kombination,
von der §5a schreibt, sie „stirbt in Produktion": 7,46 s bei einer Poolgrenze von 10 s, warm
gemessen, mit einem Kaltfaktor bis 9,66. Sie kostet verengt **unter 7 ms.**

Der Plan zeigt genau, was passiert — und die **erste** Zeile bleibt dabei gleich:

```
F0 (7.519,513 ms)                        verengt (6,027 ms)
─────────────────────────────────────    ─────────────────────────────────────
pm  ref    ProjectMandant_Mandant_idx    pm  ref    ProjectMandant_Mandant_idx
           17   temporary; filesort                 17   temporary; filesort
mp  ref    Process_ProjectFK  5          mp  ref    Process_ProjectFK  5
m   ref    ProejctIDIDX                  m   range  MessageLastUpdateIDX
           key_len 147   rows 197.804               key_len 5     rows 358
p   eq_ref PRIMARY                       p   eq_ref PRIMARY
pr  eq_ref PRIMARY                       pr  eq_ref PRIMARY
s   eq_ref PRIMARY                       s   eq_ref PRIMARY
sa  eq_ref PRIMARY                       sa  eq_ref PRIMARY
```

Die **197.804** sind der `CARDINALITY`‑18‑Befund aus M83: `3.560.486 / 18`. F0 liest für jeden der
drei Prozesse dessen gesamten Bestand und filtert danach nach Zeit. Mit einem Fenster von einer
Stunde wird der Zeitindex zum billigeren Einstieg — **552‑mal weniger geschätzte Zeilen**, 1.247‑mal
weniger Laufzeit.

**Die Gegenprobe mit drei kleinen Prozessen trägt nicht:** Diese drei haben zusammen **drei**
Nachrichten in dreißig Tagen. Die Verengung erreicht nie 51, alle vier Stufen laufen vergeblich,
und der Fall wird um 16,8 ms teurer.

---

## M101 — Der Cursor

Zwei aufeinanderfolgende Seiten à 51 Zeilen, jede mit **eigener** Verengung; die zweite verengt
gegen den Cursor-Zeitpunkt statt gegen das Fensterende. Verglichen wird gegen die unverengte
Fassung als Wahrheit, über **MD5 über die geordneten `MessageID`** (G1).

| | SUTTONS | NEXANS |
|---|---|---|
| Seite 1 — Untergrenze | `2025-12-30 02:00:00` (2 h) | `2025-12-30 03:00:00` (1 h) |
| Seite 1 — Zeilen | 51 = 51 | 51 = 51 |
| Seite 1 — Prüfsumme | **gleich** | **gleich** |
| Cursor (50. Zeile) | `2025-12-30 02:23:50` | `2025-12-30 04:07:10` |
| Seite 2 — eigene Untergrenze | `2025-12-30 00:00:00` (2 h) | `2025-12-30 03:00:00` (1 h) |
| Seite 2 — Zeilen | 51 = 51 | 51 = 51 |
| Seite 2 — Prüfsumme | **gleich** | **gleich** |
| Laufzeit Seite 1 / 2 | 6,476 / 5,270 ms | 1,617 / 1,992 ms |

**Die Richtigkeit ist belegt.** Beide Seiten, beide Mandanten: zeichengleiche Prüfsummen, gleiche
älteste und jüngste Zeit, gleiche Reihenfolge.

> ### Der Auftrag erwartet „102 verschiedene Zeilen". Gemessen sind **102 gelesene und 101 verschiedene** — bei beiden Fassungen gleich
>
> | | gelesen | verschieden | Überschneidung |
> |---|---:|---:|---:|
> | verengt | 102 | **101** | 1 |
> | unverengt | 102 | **101** | 1 |
>
> Der Grund liegt im Blättern und nicht in der Verengung: Die Liste liest **51** Zeilen, zeigt
> **50** und setzt den Cursor auf die **50.** ([`Seite.java`](../backend/src/main/java/de/kraftwerkone/overlord/monitor/common/Seite.java)).
> Die 51. Zeile der ersten Seite ist die Sondierzeile für „es gibt mehr" — sie wird nie gezeigt und
> ist zugleich die erste Zeile der zweiten Seite. **Die unverengte Fassung liefert exakt dasselbe
> Bild.** Unter den *gezeigten* Zeilen gibt es weder Lücke noch Dopplung.

---

## M102 — Die Wasserstandsgrenze

Der wirkliche Wasserstand liegt bei `2026-08-27 15:00:00` und damit weit über dem Anker. Er ist
deshalb **nicht** in `rollup_lauf` zurückgesetzt worden — dort wird nicht geschrieben —, sondern als
Literal in der Vorabfrage.

| Mandant | Wasserstand W | Untergrenze | Fenster | Prüfsumme | Laufzeit |
|---|---|---|---:|---|---:|
| SUTTONS | `2026-08-27 15:00` *(über dem Fenster)* | `2025-12-30 02:00` | 2 h | **gleich** | 6,675 ms |
| SUTTONS | `2025-12-20 00:00` *(mitten im Fenster)* | `2025-12-19 22:00` | 246 h | **gleich** | 6,542 ms |
| SUTTONS | `2025-11-01 00:00` *(unter dem Fenster)* | — *(entfällt)* | 720 h | **gleich** | **1.072,393 ms** |
| NEXANS | `2026-08-27 15:00` | `2025-12-30 03:00` | 1 h | **gleich** | 1,596 ms |
| NEXANS | `2025-12-20 00:00` | `2025-12-19 23:00` | 245 h | **gleich** | 1,666 ms |
| NEXANS | `2025-11-01 00:00` | — *(entfällt)* | 720 h | **gleich** | 1,666 ms |

Alle drei Erwartungen des Auftrags treffen zu:

- **Die Verengung geht nie über den Wasserstand hinaus.** Bei W mitten im Fenster rutscht die
  Untergrenze auf `2025-12-19 22:00` — *unterhalb* von W, nicht darüber. Die Stunden ab W zählen
  mit 0, die kumulierte 51 wird also erst darunter erreicht. Das ist zwei Stunden statt 246 an
  Verengung verschenkt und dafür sicher.
- **Bei W unter dem Fenster entfällt sie vollständig** und die Abfrage läuft **exakt wie heute**:
  1.072,393 ms gegen 1.043,011 ms für F0 in der Nachbarsitzung — dieselbe Größenordnung, nicht
  schlechter.
- **Der Nullfall ist nur aussagbar, wenn das ganze Fenster unter W liegt.** Sonst könnten oberhalb
  Zeilen liegen, von denen der Rollup nichts weiß.

Nebenbefund, der zählt: **Auch 246 Stunden reichen, um `SUTTONS` umzustellen** — 6,542 ms statt
1.043 ms. Die Verengung muss also gar nicht bis auf zwei Stunden gehen, um zu wirken.

---

## M103 — Wo die Verengung nicht greift

### `ueberfaellig` — der Rückfallpfad

`ueberfaellig` hängt an `MessageTimeout` und an `jetzt`; beides steht nicht im Rollup und lässt sich
nicht daraus herleiten. Die Verengung wird dort **gar nicht erst betreten**.

| Fall | gemessen | Bezug aus §5a / M97 | |
|---|---:|---:|---|
| NEXANS, 30 Tage, `ueberfaellig` | **4,993 ms** | 5,282 / 5,170 ms | unverändert |
| SUTTONS, 30 Tage, `ueberfaellig` | **6,631 ms** | 7,081 / 6,904 ms | unverändert |

Plan in beiden Fällen `m` / range / `MessageStatusIDX` / `key_len` 123 / 539 Zeilen /
`index condition; where; filesort` — zeichengleich zu 10b‑1. `SUTTONS` liefert null Zeilen; alle
überfälligen Zeilen des Bestands gehören NEXANS.

### Der Statusfilter — hier wird es gefährlich

> ## ⚠️ Eine Verengung, die nicht **jeden** Filter der Quellabfrage mitträgt, verliert Zeilen
>
> | Fall | Zeilen geliefert | |
> |---|---:|---|
> | `F0-SUTTONS-30d-fehler` — unverengt, **die Wahrheit** | **5** | |
> | verengt, Rollup **ohne** Statusfilter | **0** | ⚠️ **alle fünf verloren** |
> | verengt, Rollup **mit** Statusfilter | **5** | ✔ richtig |
> | `F0-NEXANS-30d-fehler` — unverengt, **die Wahrheit** | **51** | |
> | verengt, Rollup **ohne** Statusfilter | **49** | ⚠️ **zwei verloren** |
> | verengt, Rollup **mit** Statusfilter | **51** | ✔ richtig |
>
> Der Mechanismus ist einfach und darum gefährlich: Der Rollup meldet, dass in der letzten Stunde
> 51 Nachrichten liegen, und die Verengung schneidet darauf zu. In dieser Stunde liegen aber
> **null Fehler**. Die Quellabfrage liefert eine leere Liste, obwohl fünf Fehler im Fenster stehen —
> und sie meldet keinen Fehler dabei. Ein Mandant sähe „keine Fehler" und hätte fünf.
>
> **Das ist keine Laufzeitfrage, sondern Punkt 1 aus §5 des Auftrags** („Sie darf nie eine Zeile
> entfernen, die die unverengte Abfrage geliefert hätte"). Jede künftige Fassung muss diese Probe
> bestehen, und sie muss über einen **Filter** laufen, der die Zeilenzahl stark drückt — mit
> `SUTTONS` und dem Statusfilter `FEHLER` gibt es dafür jetzt einen scharfen Testfall.

Trägt die Verengung den Statusfilter mit, ist sie richtig — und **nutzlos**:

| Fall | Zeilen im Fenster | Untergrenze | F0 | verengt |
|---|---:|---|---:|---:|
| NEXANS, `FEHLER` | 55 | — *(unter 51)* | 22,121 ms | 22,668 ms |
| SUTTONS, `FEHLER` | 5 | — *(unter 51)* | 22,448 ms | 22,444 ms |

Fehler sind selten. Über dreißig Tage hat NEXANS **55** und SUTTONS **5** — die kumulierte 51 wird
über volle Stunden nie erreicht, die Verengung entfällt, und es bleibt der Aufschlag der
Vorabfrage. **Beim Statusfilter ist die Verengung also entweder falsch oder wirkungslos.**

### Die BAM-Suche

Dort wird die Verengung ebenfalls nicht angewandt. Sie ist **nicht gemessen worden**, und das ist
eine bewusste Auslassung: Die BAM-Suche ist ein eigenes Repository
([`BamSucheRepository.java`](../backend/src/main/java/de/kraftwerkone/overlord/monitor/bam/BamSucheRepository.java)),
mit eigenem Fenster (`mitVorgabe`, 30 Tage), ohne Cursor, mit gedeckeltem Kern als abgeleiteter
Tabelle und Selbstjoin auf `MessageBAM`. Da nichts gebaut worden ist, ist ihr Statement
**unverändert** — es gibt keine Fassung, die sich von der heutigen unterschiede und die zu messen
wäre. Eine Messung hätte nur den heutigen Wert reproduziert. Festgehalten als offener Punkt **74**,
falls die Verengung je gebaut wird.

---

## Das Tor, angewandt

Vorabfrage jeweils in der **besseren** der beiden gemessenen Fassungen, Stufen bis einschließlich
der tragenden summiert, Nullfall angewandt (die Quellabfrage entfällt dann ganz).

| Fall | **heute** | Vorabfrage | Quelle | **Summe** | |
|---|---:|---:|---:|---:|---|
| **NEXANS, Prozessfilter groß** | **7.519,513** | 0,770 | 6,027 | **6,797** | **1.106× besser** |
| **EDITIONLINGERI** | **2.126,876** | 17,454 | *(Nullfall)* | **17,454** | **122× besser** |
| **SUTTONS** | **1.043,011** | 2,109 | 6,496 | **8,605** | **121× besser** |
| ZAST | 270,891 | 4,546 | 270,824 | 275,370 | unverändert |
| IBISGUS | 52,129 | 2,345 | 51,100 | 53,445 | 1,03× schlechter |
| IBIS | 41,377 | 2,889 | 41,129 | 44,018 | 1,06× schlechter |
| VOTG | 33,672 | 4,025 | 32,996 | 37,021 | 1,10× schlechter |
| **WOC** | 15,102 | 17,274 | 14,982 | **32,256** | **2,14× schlechter** |
| NEXANS, Prozessfilter klein | 1,730 | 16,836 | 1,720 | 18,556 | **10,7× schlechter** |
| **SYSTEM** | 1,695 | 16,734 | 1,556 | **18,290** | **10,8× schlechter** |
| NEXANS, ohne Filter | 1,634 | 1,002 | 1,615 | 2,617 | 1,60× schlechter |
| **NXHBE** | 0,980 | 17,584 | *(Nullfall)* | **17,584** | **17,9× schlechter** |

| Zeile des Auftrags | Trifft zu? |
|---|---|
| Die vier teuren Fälle fallen deutlich **und** kein Mandant wird messbar schlechter → **bauen** | **Nein.** Drei der vier fallen um Faktor 121 bis 1.106. **`ZAST` fällt nicht** — und zwar aus einem strukturellen Grund, nicht aus einem behebbaren |
| Ein Mandant wird schlechter → **nicht bauen. Melden** | **Ja. Das ist die Lage.** Sechs Fälle werden schlechter, zwischen Faktor 1,03 und 17,9 |
| Die Verengung greift, aber der Optimierer wechselt trotzdem nicht die Form → **nicht bauen. Melden** | **Nein.** Er wechselt, wo es richtig ist (`SUTTONS`, Prozessfilter), und bleibt, wo das richtig ist (`WOC`, `SYSTEM`, `NXHBE`). §2 ist **bestätigt** |

**Gebaut wird nichts** — auch nicht der Nullfall allein. Er ist der billigste Gewinn der Runde
(`EDITIONLINGERI` 2.126,876 → 17,454 ms), aber er trifft `NXHBE` genauso: Auch dort muss der Rollup
erst befragt werden, und dort kostet das 17,584 ms statt 0,980 ms. **Derselbe Handgriff, der einen
Mandanten um Faktor 122 rettet, verschlechtert einen anderen um Faktor 18.**

### Warum dieses „nicht bauen" anders ist als das aus 10b‑1

Beide Runden enden mit demselben Satz, aber nicht mit demselben Befund.

| | 10b‑1, Fassung F6 | diese Runde |
|---|---|---|
| Was schadet | der **Plan**: `WOC` ×92, `SYSTEM` ×1.845, `NXHBE` ×3.002 | die **Vorabfrage**: ein additiver Betrag, höchstens ~18 ms |
| Wie es skaliert | unbegrenzt, mit dem Bestand | fest, mit der Größe des Rollup-Bereichs |
| Ob die Annahme trug | **nein** — F6 nagelt die Planwahl fest und trifft die Ungesehenen | **ja** — der Optimierer wählt selbst und wählt richtig |
| Was fehlt zum Bauen | ein Index auf `GlassfishDB`, den wir nicht anlegen dürfen | eine billigere Vorabfrage für **dünne** Mandanten, auf **unserer eigenen** Tabelle |

Der Unterschied ist der Punkt: 10b‑1 endete an einer Grenze, die außerhalb dieses Projekts liegt.
Diese Runde endet an einer Stelle, an der dieses Projekt selbst arbeiten darf — siehe offenen
Punkt **73**.

---

## Vorregistrierte Deutung, dagegengehalten

### „Die vier teuren Fälle fallen unter 50 ms" — **drei von vier**

`NEXANS`-Prozessfilter auf 6,797 ms, `SUTTONS` auf 8,605 ms, `EDITIONLINGERI` auf 17,454 ms — alle
drei deutlich unter 50 ms. **`ZAST` bleibt bei 275,370 ms.**

### „Die sechs unauffälligen bleiben unverändert" — **nein, alle sechs werden teurer**

Zwischen 1,03× (`IBISGUS`) und 17,9× (`NXHBE`). Die Quellabfrage bleibt bei ihnen tatsächlich
unverändert — es ist ausschließlich die Vorabfrage, die aufschlägt. Die Erwartung hat die Kosten der
Vorabfrage nicht mitgedacht; der Auftrag verlangt sie in M99 ausdrücklich zu messen, und das war
richtig.

### „Wechselt der Plan trotz engem Fenster nicht, ist §2 widerlegt" — **§2 ist bestätigt, aber unvollständig**

Der Plan wechselt überall dort, wo ein Wechsel etwas bringt. Was §2 nicht sagt und was diese Runde
ergänzt: **Beim bereits zeitgetriebenen Mandanten ist die Verengung wirkungslos** — nicht, weil der
Optimierer nicht mitzöge, sondern weil das verengte Fenster per Konstruktion genau der Bereich ist,
den er ohnehin schon liest. Das ist der Befund zu `ZAST`, und er ist nicht wegzurechnen: Solange die
Untergrenze über „wo liegt die 51. Zeile" definiert ist, **kann** sie diesem Fall nicht helfen.

### Was diese Runde **nicht** sagt

- Sie sagt **nicht**, dass die Verengung eine schlechte Idee ist. Sie sagt, dass die Vorabfrage in
  der gemessenen Form für dünne Mandanten zu teuer ist.
- Sie sagt **nichts** über eine Vorabfrage gegen einen Rollup mit einem Index auf `process_id`.
  Das ist **nicht gemessen** (Punkt 73) — es hätte einen Schreibzugriff auf `overlord_monitor`
  verlangt, und diese Runde hat nirgends geschrieben.
- Sie sagt **nichts** über kalte Läufe. Alles ist warm gemessen; der Kaltfaktor aus M44 geht bis
  9,66 und trifft Vorabfrage und Quellabfrage nicht notwendig gleich.
- Sie sagt **nichts** über die Tagesebene `message_rollup_tag` als Grundlage der Verengung.
- Sie sagt **nichts** über das Verhalten bei parallelen Anfragen; jede Sitzung war allein.

---

## Offene Punkte

Nummerierung im Anschluss an den projektweit höchsten Stand (**69**, in
[`rollup.md`](rollup.md) §13, Z. 1020).

70. **Die Verengung kann einem bereits zeitgetriebenen Mandanten nicht helfen — strukturell, nicht
    behebbar.** `ZAST` liest heute 167 Stunden, um 51 Zeilen zu finden, und liest verengt dieselben
    167 Stunden. Der zeitgetriebene Plan hört ohnehin bei der 51. Zeile auf; genau dort setzt die
    Verengung an. **Wer `ZAST` und die 270 ms lösen will, braucht einen anderen Hebel** — die
    Verengung ist keiner. Damit bleibt offener Punkt **64** aus
    [`nachrichtenliste.md`](nachrichtenliste.md) unberührt bestehen.

71. **Der teuerste bekannte Fall des Projekts ist lösbar, und die Lösung liegt bereit.** Der
    Prozessfilter mit verkehrsreichen Prozessen fällt von **7.519,513 ms auf 6,797 ms**, und er ist
    der einzige gemessene Fall, der die 10‑s‑Grenze des Lese-Pools ernsthaft bedroht. Er ist über
    die Prozessauswahl mit zwei Klicks erreichbar. **Ihn allein zu verengen, wäre ein viel kleinerer
    Eingriff als die ganze Liste** — der Prozessfilter ist ohnehin schon eine Sonderform, und die
    Vorabfrage kostet dort 0,770 ms. Nicht gemessen ist, ob eine Verengung, die **nur** bei gesetztem
    Prozessfilter greift, irgendeinen Mandanten verschlechtert. Das ist die naheliegendste
    Anschlussfrage dieser Runde.

72. **Eine Verengung, die nicht jeden Filter mitträgt, verliert stillschweigend Zeilen.**
    Gemessen: `SUTTONS` mit Statusfilter `FEHLER` liefert **null statt fünf** Zeilen, `NEXANS`
    **49 statt 51**. Kein Fehler, keine Meldung — eine leere Liste, wo Fehler stehen. Wird die
    Verengung je gebaut, ist das die erste Prüfung, und der Testfall steht: Der Filter muss die
    Zeilenzahl stark drücken, sonst schlägt der Fehler nicht durch. Gilt gleichermaßen für jeden
    künftigen Filter, der der Liste hinzugefügt wird — **wer einen Filter ergänzt, ergänzt zwei
    Dinge.**

73. **Ob ein Index `(process_id, stunde)` auf `message_rollup` die dünnen Mandanten rettet, ist
    ungemessen.** Sie zahlen 13 bis 18 ms, weil der Nachweis „weniger als 51 Zeilen in dreißig
    Tagen" **21.274** Rollup-Zeilen lesen muss — die aller Mandanten, weil die Tabelle nach
    `(stunde, process_id, message_status)` geschlüsselt ist und keinen Sekundärindex trägt
    ([`rollup.md`](rollup.md) §2, ausdrückliche Entscheidung). Mit einem Index über `process_id`
    läse `WOC` seine **47**, `SYSTEM` seine **5** und `NXHBE` und `EDITIONLINGERI` **keine**.
    `message_rollup` liegt in `overlord_monitor`; ein Index dort ist erlaubt. **Er kostet Platz und
    verlangsamt den Rollup-Lauf**, und beides ist ungemessen. Für **NEXANS** hilft er nachweislich
    nicht (Nachtrag 1): Dessen Aufschlag steckt im Parsen der Literalliste, nicht im Lesen.
    Der Weg dahin wäre eine Probetabelle, angelegt, gemessen und wieder gelöscht — die Bauform von
    **M89**.

74. **Die BAM-Suche ist als Rückfallpfad nicht gemessen.** Da nichts gebaut wurde, ist ihr Statement
    unverändert und eine Messung hätte nur den heutigen Wert wiederholt. Wird die Verengung je
    gebaut, gehört der Nachweis dorthin — zusammen mit einem Test, der belegt, dass der Pfad gar
    nicht erst betreten wird.

75. **Die Vorabfrage hat zwei Fassungen, und keine gewinnt überall.** Die `EXISTS`-Kette ist bei
    einer Stunde billiger (0,993 gegen 2,198 ms) und braucht keine Prozessliste; die Literalliste
    ist bei dreißig Tagen um Faktor 6 bis 8 billiger (13,0 gegen 88,9 ms bei `WOC`). Eine gebaute
    Fassung müsste je nach Stufe wechseln — oder die Prozessliste zwischenspeichern, was sie
    zustandsbehaftet machte und in dieser Runde nicht betrachtet ist.

---

## Was diese Runde nicht getan hat

- **Nichts gebaut.** Kein Anwendungscode ist angefasst worden.
- **Nichts geschrieben** — weder in `GlassfishDB` noch in `overlord_monitor`. Kein Index, keine
  Probetabelle, keine Migration.
- **Keinen Hint, kein `STRAIGHT_JOIN`, keine erzwungene Reihenfolge.** Der Optimierer ist in keiner
  Fassung überstimmt worden.
- **Das Dashboard nicht angefasst.**
- **`PROJEKTBESCHREIBUNG.md`, `IMPLEMENTIERUNGSPLAN_MVP.md` und die bestehenden Messdateien nicht
  angefasst.**
