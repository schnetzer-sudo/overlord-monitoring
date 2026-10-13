# Statuskatalog `Message.MessageStatus`

Erhebung gegen die Testkopie, vorgezogen aus Schritt 3 (die Verbindung steht in Schritt 2 ohnehin).
Die Einordnung entsteht **ausschließlich** in `common/MessageStatusClassifier` und wird nirgends
nachgebaut — sie wird in Liste, Dashboard, Rollup und später im Chatbot verwendet.

`MessageStatus` ist **freier Text, kein Aufzählungstyp.** `CKECKED` ist der Beweis (ein nie
korrigierter Tippfehler in den Produktivdaten). Unbekannte Werte werden zu `UNGEKLAERT`, niemals zu
einem geratenen Wert.

---

## Erhobene Werte (seit 01.01.2025, Stand der Projektbeschreibung)

| Status | Anzahl | Einordnung (`MessageStatusKind`) | Anzeige |
|---|---|---|---|
| `FINISHED` | 1.663.884 | `ABGESCHLOSSEN` | grün |
| `MERGED` | 607.277 | `ZUSAMMENGEFUEHRT` | neutral |
| `SPLITTED` | 310.263 | `AUFGETEILT` | neutral |
| `EERP_RECEIVED` | 116.828 | `QUITTIERT` | grün |
| `COMMIT_RECEIVED` | 10.126 | `QUITTIERT` | grün |
| `COMMIT_SENT` | 979 | `UNGEKLAERT` | neutral |
| `ERROR_DUPLICATE` | 659 | **`FEHLER`** | rot |
| `SUSPENDED` | 538 | `WARTEND` | neutral |
| `CHECKED` | 257 | `UNGEKLAERT` | neutral |
| `COMMIT_REJECTED` | 111 | **`FEHLER`** | rot |
| `ERROR_TIMEOUT` | 52 | **`FEHLER`** | rot |
| `CKECKED` | 2 | `UNGEKLAERT` (Tippfehler) | neutral |
| `RUNNING` | 0 | `LAEUFT` | neutral |
| `EERP_FAILED` | nicht in der Testkopie, Produktion | **`FEHLER`** *(seit 13.10.2026, E‑233)* | rot |
| `EERP_PENDING` | 0 *(Testkopie, 13.10.2026)*, Produktion | **`QUITTUNG_AUSSTEHEND`** *(seit 13.10.2026, E‑234)* | neutral |

13 dokumentierte Werte. Die `MessageStatusKind`-Einordnung ist in `MessageStatusClassifier`
hinterlegt.

> **Ergänzt 13.10.2026: 14 dokumentierte Werte.** `EERP_FAILED` ist in der **Produktion**
> aufgefallen (09.10.2026) und steht deshalb ohne Anzahl in der Tabelle — er ist kein Ergebnis
> dieser Erhebung. In der Testkopie kommt er null Mal vor (M191). Alles Weitere im Abschnitt
> „`EERP_FAILED` — der zweite Fehler ohne Präfix" weiter unten.
>
> **Ergänzt 13.10.2026, später am Tag: 15 dokumentierte Werte.** Dazu kommt `EERP_PENDING`
> (E‑234), ebenfalls nur aus der Produktion bekannt; in der Testkopie 0 Zeilen (Zählung vom
> 13.10.2026). Abschnitt „`EERP_PENDING` — Quittung ausstehend" weiter unten.

### `ZWISCHENSCHRITT` ist zwei Werte geworden (11.08.2026, Schritt 6, Teil 2a)

| alt | neu | Rohwert |
|---|---|---|
| `ZWISCHENSCHRITT` | **`AUFGETEILT`** | `SPLITTED` |
| `ZWISCHENSCHRITT` | **`ZUSAMMENGEFUEHRT`** | `MERGED` |

**Technisch waren beide dasselbe, fachlich bedeuten sie Gegenteiliges:** *aus eins wurde viel* gegen
*aus viel wurde eins*. Ein gemeinsamer Eimer verschluckt genau den Unterschied, den die Verkettung
sichtbar macht ([`verkettung.md`](verkettung.md) §1) — `MERGED` ist der **Eingang** einer
Zusammenführung und zeigt auf das Ergebnis, `SPLITTED` ist die **Wurzel** einer Aufteilung und wird
von ihren Kindern rückverwiesen (M23‑2, M25‑1).

**`istEndstatus` liefert für beide weiterhin `true`.** An der Überfälligkeitsrechnung ändert sich
**nichts**; M6 stützt das unverändert (siehe unten). Das vollständige `switch` ohne `default` hat
erzwungen, dass jede Stelle im Code bewusst nachgezogen wurde, statt eine stille Voreinstellung zu
erben — genau der Zweck, für den es dort steht.

**Das Wort „Zwischenschritt" kommt in keiner Sprachdatei und in keiner Oberflächenzeichenkette mehr
vor.** Der Statusfilter der Liste bietet beide Werte einzeln an
([`nachrichtenliste.md`](nachrichtenliste.md) §5).

> ⚠️ **Der Abstrich, der dazugehört: Der Status sagt über die Stellung in der Kette nichts
> Verlässliches.** Bei `IBIS`, `IBISGUS` und `ZAST` trägt die Wurzel `FINISHED` (M24‑3) — und das
> sind gerade die drei Mandanten, die über den ganzen Bestand **keine einzige** `SPLITTED`- oder
> `MERGED`-Zeile haben. Verlässlich ist die Spalte `Source`, nicht `MessageStatus`. Die Liste zeigt
> trotzdem nur den Status: Eine Rollenkennzeichnung dort ist Entscheidung des Auftraggebers und
> unterbleibt; die Rolle erscheint im Detail. Der Punkt steht als bekannter Abstrich in
> [`nachrichtenliste.md`](nachrichtenliste.md) §5 — **nicht als Fehler**.
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen (M24‑3, Fenster B + Überhang):* Bei `IBIS` (1.714), `IBISGUS` (22) und `ZAST` (20)
> > tragen die Elternzeilen `FINISHED`; bei `NEXANS` (27.644) und `SUTTONS` (640) tragen sie
> > `SPLITTED`.
> > *Behauptet ist:* Der Status ist über die Kette hinweg **unzuverlässig** — die Liste sagt bei
> > `NEXANS` etwas über die Aufteilung und bei drei Mandanten nichts.
> > **Die Lücke:** Gemessen sind fünf Mandanten in einem Monatsfenster, behauptet ist eine Aussage
> > über den Bestand. Sie trägt trotzdem, weil sie nur einen **Gegenbeleg** braucht und nicht eine
> > Quote: Ein einziger Mandant, dessen Wurzel `FINISHED` trägt, widerlegt „der Status benennt die
> > Stellung". Drei sind gemessen.

## Nachprüfung am 28.07.2026 (unbefristet, ganze Testkopie)

`SELECT DISTINCT MessageStatus FROM Message` lieferte **12 Werte** — exakt die obige Menge **ohne
`RUNNING`**. *(Gemeint ist die Menge vom 28.07.2026. Seit dem 13.10.2026 steht in der Tabelle
zusätzlich `EERP_FAILED`, der in der Testkopie ebenfalls fehlt; M191 hat dieselben 12 Werte
erneut gezählt. Später am 13.10.2026 kam `EERP_PENDING` dazu (E‑234), in der Testkopie ebenfalls
0 Zeilen: Die 15 Tabellenwerte ohne `RUNNING`, `EERP_FAILED` und `EERP_PENDING` sind dieselben 12.)*
Kein neuer, undokumentierter Wert. Die Anzahlen liegen höher als oben (die Testkopie
reicht jetzt bis 08.07.2026), aber die *Menge* der Statuswerte ist unverändert.

---

## Anmerkungen, die mitzudenken sind

- **`COMMIT_REJECTED` zählt als Fehler**, obwohl der Wert **nicht** mit `ERROR_` beginnt. Der Partner
  hat die Übertragung abgelehnt, der Beleg ist nicht angekommen. Damit ist Annahme A6 der
  Projektbeschreibung widerlegt (siehe `annahmen-korrekturen.md`).
- **`RUNNING` kommt in der Testkopie null Mal vor**, in der Produktion aber sehr wohl. Der Status ist
  flüchtig und existiert nur, solange eine Nachricht tatsächlich in Arbeit ist. Folgen:
  - Die Problemkategorie „Überfällig" ist gegen die Testkopie nicht prüfbar.
  - „läuft noch" darf **nicht** als `MessageStatus = 'RUNNING'` definiert werden, sondern als „nicht in
    einem Endstatus".
- **`CHECKED`, `CKECKED` und `COMMIT_SENT`** gelten als bekannt, aber fachlich **ungeklärt**. Sie
  werden neutral behandelt und mit Rohwert plus Hinweis „Bedeutung nicht verifiziert" angezeigt. Es
  wird nichts geraten.
- **`EERP_FAILED` zählt als Fehler** *(ergänzt 13.10.2026, E‑233)*, obwohl der Wert **nicht** mit
  `ERROR_` beginnt — der zweite Fall nach `COMMIT_REJECTED`. Die Empfangsbestätigung ist
  fehlgeschlagen, der Beleg gilt nicht als angekommen. Abschnitt unten.
- **`EERP_PENDING` ist flüchtig wie `RUNNING`** *(ergänzt 13.10.2026, E‑234)*: übertragen, die
  Empfangsbestätigung des Partners steht aus. Eigene Einordnung `QUITTUNG_AUSSTEHEND`, neutral,
  kein Endstatus. Abschnitt unten.

---

## `EERP_FAILED` — der zweite Fehler ohne Präfix (13.10.2026, E‑233)

**Der Anlass.** In der Nachrichtenliste der **Produktion** stand `EERP_FAILED` am 09.10.2026 mit
der Fragezeichen-Plakette da — als unbekannter Wert, `UNGEKLAERT`, „Bedeutung nicht verifiziert".
So verlangt es die Regel für unbekannte Werte, und genau das war hier falsch: Die
Empfangsbestätigung (EERP) ist fehlgeschlagen, der Beleg gilt **nicht** als angekommen. Für den
Nutzer ist das dieselbe Frage wie bei `COMMIT_REJECTED`.

**Die Entscheidung E‑233** (Auftraggeber, 09.10.2026): `EERP_FAILED` gehört zur Problemkategorie
*Fehler*, wie `COMMIT_REJECTED`.

| | |
|---|---|
| Einordnung | **`FEHLER`**, Anzeige rot — ein Eintrag in `MessageStatusClassifier.BEKANNT` |
| Fehlerart | der feste Text **„Empfangsbestätigung fehlgeschlagen"** (`MessageStatusClassifier.EMPFANGSBESTAETIGUNG_FEHLGESCHLAGEN`), analog zu „Vom Partner abgelehnt"; die Oberfläche übersetzt über den Rohwert (`fehlerarten` in `de.ts` / `en.ts`: „Acknowledgement failed") |
| Fehlerbedingung in SQL | `… OR MessageStatus IN ('COMMIT_REJECTED', 'EERP_FAILED')` — Abschnitt „Die Fehlerbedingung" unten |
| `istEndstatus` | `true`, wie `COMMIT_REJECTED` — und wie die `ERROR_`-Werte: alle drei sind `FEHLER`. **Eine Abweichung zwischen `COMMIT_REJECTED` und `ERROR_` gibt es dort nicht**; es war nichts anzugleichen |
| `istUeberfaellig` | **gibt es nicht mehr** (E‑71, 03.09.2026). Ihre erste Bedingung war „nicht in einem Endstatus"; `EERP_FAILED` ist Endstatus und kann damit nie unter die offenen Zeilen fallen. `MessageStatusClassifierTest` hält es über `istEndstatus` fest |
| *Unquittiert* | gehört **nicht** dazu, wie `COMMIT_REJECTED` ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.2, Punkt 3) |

**Was ausdrücklich nicht gebaut ist.** Kein Muster wie `'%_FAILED'` oder `*_REJECTED`, weder in SQL
noch in Java: `COMMIT_FAILED` oder `EERP_REJECTED` blieben `UNGEKLAERT` (der Test hält beides fest).
An `CHECKED`, `CKECKED` und `COMMIT_SENT` ändert sich nichts, und es gibt keine Kategorie
*Unquittiert* und keine neue Farbrolle.

**Wo die Einordnung ankommt, und wo nicht.** Gespeichert ist sie nirgends: Der Rollup führt den
Rohwert (E‑g, [`rollup.md`](rollup.md) §2), alle Verbraucher — Liste, Detail, Kette, BAM-Suche,
Dashboard samt Fehler live, Prozessbaum — rufen `einordnung` bzw. `fehlerBedingung` zur Lesezeit.
**Ein Rollup-Volllauf ist deshalb nicht nötig**: Rollupzeilen mit `EERP_FAILED`, die es in der
Produktion bereits gibt, zählen ab dem Einspielen als Fehler. **Eine Nachbildung gibt es genau
einmal, und sie ist Absicht:** Die Oberfläche beschriftet die Fehlerart über den Rohwert aus der
Sprachdatei (`beschriftung.ts`, C.4) — deshalb der neue Eintrag dort.

### Die Statuszählung (M191, Sitzung 1)

`SELECT MessageStatus, COUNT(*) FROM GlassfishDB.Message GROUP BY MessageStatus` über den ganzen
Bestand, 13.10.2026, Datenstand `2026-07-08 17:21:10`, `read_only = 1`
([`scripts/messung-eerp-failed/s1-statuszaehlung.sql`](../scripts/messung-eerp-failed/s1-statuszaehlung.sql),
Ausgabe `ergebnis/s1.txt`):

| Status | Anzahl | dokumentiert |
|---|---:|---|
| `FINISHED` | 2.030.985 | ja |
| `MERGED` | 747.885 | ja |
| `SPLITTED` | 400.845 | ja |
| `EERP_RECEIVED` | 143.871 | ja |
| `COMMIT_RECEIVED` | 12.654 | ja |
| `ERROR_DUPLICATE` | 3.248 | ja |
| `COMMIT_SENT` | 1.051 | ja |
| `SUSPENDED` | 538 | ja |
| `CHECKED` | 276 | ja |
| `COMMIT_REJECTED` | 111 | ja |
| `ERROR_TIMEOUT` | 53 | ja |
| `CKECKED` | 2 | ja |
| **Summe, 12 Gruppen** | **3.341.519** | |

- **Kein undokumentierter Wert.** Es fehlen `RUNNING` (wie seit dem 28.07.2026 dokumentiert) und
  `EERP_FAILED` (`= 'EERP_FAILED'`: **0**; `LIKE '%FAIL%' OR LIKE 'EERP%'` findet nur
  `EERP_RECEIVED`).
- **Die Gegenprobe bytegenau** (`GROUP BY HEX(MessageStatus)`) liefert dieselben 12 Gruppen und
  dieselbe Summe. Die Spalte sortiert mit `utf8mb4_general_ci` (`PAD SPACE`), ein
  `GROUP BY MessageStatus` legte Schreibweisen und angehängte Leerzeichen also zusammen — es gibt
  keine.
- **Zwei Zahlen weichen von M87‑6 (26.08.2026) ab, die Menge nicht:** `FINISHED` 2.030.985 statt
  2.030.986, `ERROR_TIMEOUT` 53 statt 52, Summe gleich. Dieselbe 53 steht schon in M188
  ([`fehler-live.md`](fehler-live.md) §7, Dev-Zeile, M188 Sitzung 0, 18.09.2026). Dem Unterschied ist hier nicht nachgegangen;
  er berührt die Einordnung nicht.
- **Kosten** (L9): ein Lauf über den ganzen Bestand ohne Zeitfenster, mit dem Messclient und nicht
  im Anfragepfad; die Laufzeit ist nicht erhoben.

### Die Fehlerabfrage, alt gegen neu (M191, Sitzungen 2 bis 5)

**Gemessen ist, was der Code schickt** (L7): Alle Statements sind aus dem Code gerendert
(Wegwerf-Test gegen die jOOQ-Attrappe, `STATIC_STATEMENT`, danach gelöscht —
[`gerendert.txt`](../scripts/messung-eerp-failed/gerendert.txt), `gerendert-weitere.txt`,
`gerendert-rest.txt`), die alte Fassung entsteht daraus
durch genau eine Ersetzung. Für die Lesung ist sie gegen den in M188 gemessenen Text geprüft:
**zeichengleich**, für beide Mandanten. Generator und Filter:
[`scripts/messung-eerp-failed/`](../scripts/messung-eerp-failed/); eingecheckt sind die gefilterten
Protokolle `ergebnis/s2.gefiltert.txt` bis `ergebnis/s5.gefiltert.txt`.

- **Lesung** = `FehlerLiveRepository.ausDerQuelle` — die Fehlerkachel der Übersicht und der
  Prozessbaum, live über `Message`. Fenster `30T` am Anker (`2025-12-01` bis `2025-12-31`), wie M188.
- **Liste** = `NachrichtenRepository.finde` mit Statusfilter `FEHLER`, 30 Tage bis zum Anker der
  Dev-Uhr, 50 Zeilen (`fetch next 51 rows only`).
- **Sitzung 4, drei weitere Statements mit der Fehlerbedingung** — nicht vom Auftrag
  verlangt, aber von L7 („jede neue Abfrage"): **Block 6** (`DashboardRepository.zuletztAufgefallen`,
  48 Stunden, 10 Prozesse — liest `Message` wie die Lesung), die **Verteilung ohne Fehler**
  (`verteilungOhneFehler`, 48 Stunden, Partnersicht — `NOT fehlerBedingung` auf dem Rollup) und die
  **Vorabfrage der Fensterverengung** (`VerengungRepository.frageStufe`, Statusfilter `FEHLER`, die
  weiteste Stufe des 30‑Tage-Fensters, Wasserstand `2026-08-27 15:00` — `MAX(fenster_bis)` der
  Testkopie am 13.10.2026). Für die Verteilung ist die alte Fassung gegen den in M188 gemessenen
  Text geprüft: zeichengleich.
- **Sitzung 5, nachgereicht nach der Durchsicht** (13.10.2026, 11:25): der Statusfilter
  **`UNGEKLAERT`** von Liste und Verengung — er trägt die Fehlerbedingung als
  `NOT (… LIKE … OR … IN (…))` in `MessageStatusClassifier.bedingung(UNGEKLAERT, …)` und ändert sich
  damit ebenso — und die **fünf übrigen Formen der Verteilung ohne Fehler** (48H Richtung, 30T und
  12M je Partner und Richtung). Alle zehn alten Fassungen der Verteilung sind gegen die in M188
  gemessenen Texte geprüft: zeichengleich. **Damit ist jede Stelle gemessen, die `fehlerBedingung`
  ruft — je in einer Form:** Liste und Verengung mit genau einer Einordnung im Statusfilter, ohne
  Prozessfilter, Suchbegriff und Folgeseite; je Mandant in einer Fensterbreite. **Nicht gemessen**
  sind Mehrfachauswahlen im Statusfilter (etwa `FEHLER` + `UNGEKLAERT`, von
  `bedingung(Collection, …)` mit `OR` verbunden) und der Statusfilter zusammen mit einem
  Prozessfilter — so führt der Verweis aus Block 6 in die Liste (`status=FEHLER&prozess=…`,
  `prozessFehlerZiel` in `verweise.ts`); dort steht mit `ProejctIDIDX` ein weiterer Zugriffspfad zur
  Wahl.
- **Mandanten:** `NEXANS` und `SUTTONS` (der kleine Mandant aller Vergleichsrunden; bei der Lesung
  steigt sein Plan über `ProjectMandant` ein). Reihenfolge `NEXANS` alt → neu, `SUTTONS` neu → alt.
- Je Fall `EXPLAIN`, der Bereichszugriff aus dem Optimizer-Trace, ein Lauf zwischen zwei Lesungen
  der Handler-Zähler, dann ein Aufwärmlauf und fünf Läufe — Profil **und** Wanduhr (`SYSDATE(6)`).

**Die Erwartung, vor dem Lauf festgehalten** (13.10.2026, 10:53): (1) `range` über
`MessageStatusIDX` in beiden Fassungen, kein `ALL`, `key_len` 123 wie M188; (2) alt zwei Intervalle
auf dem Index, neu drei; (3) `read_next` gleich, `read_key` neu um eins höher; (4) Laufzeit innerhalb
der Streuung. Für Sitzung 4 (11:03): Block 6 wie die Lesung; bei Verteilung und Verengung ist der
Status ein Filter auf gelesenen Zeilen und kein Zugriffspfad — Plan und alle Zähler alt gleich neu,
Laufzeit innerhalb der Streuung. Für Sitzung 5 (11:25): dasselbe für `UNGEKLAERT` und die übrigen
Verteilungen.

**Lesung** — Eichung der Wanduhr um `SELECT 1`: 1,023 / 0,932 / 1,205 ms

| Mandant | Fassung | Zugriff auf `Message` | `rows` | Bereiche auf `MessageStatusIDX` | `read_key` | `read_next` | Profil, beste von fünf | Wanduhr, beste von fünf | Wanduhr, alle fünf |
|---|---|---|---:|---:|---:|---:|---:|---:|---|
| NEXANS | alt | `range` über `MessageStatusIDX`, `key_len` 123 | 6.257 | 2 | 74 | 3.412 | 20,197 ms | 21,783 ms | 21,783 / 21,992 / 22,230 / 25,424 / 22,877 |
| NEXANS | neu | `range` über `MessageStatusIDX`, `key_len` 123 | 6.258 | 3 | 75 | 3.412 | 20,163 ms | 21,064 ms | 21,940 / 21,131 / 21,064 / 21,968 / 21,297 |
| SUTTONS | alt | `range` über `MessageStatusIDX`, `key_len` 123 | 6.257 | 2 | 17 | 3.413 | 20,895 ms | 22,378 ms | 23,146 / 24,329 / 22,950 / 23,727 / 22,378 |
| SUTTONS | neu | `range` über `MessageStatusIDX`, `key_len` 123 | 6.258 | 3 | 18 | 3.413 | 20,693 ms | 22,206 ms | 24,045 / 23,261 / 23,747 / 25,822 / 22,206 |

**Liste** — Eichung der Wanduhr um `SELECT 1`: 0,918 / 0,965 / 0,762 ms

| Mandant | Fassung | Zugriff auf `Message` | `rows` | Bereiche auf `MessageStatusIDX` | `read_key` | `read_next` | Profil, beste von fünf | Wanduhr, beste von fünf | Wanduhr, alle fünf |
|---|---|---|---:|---:|---:|---:|---:|---:|---|
| NEXANS | alt | `range` über `MessageStatusIDX`, `key_len` 123 | 6.257 | 2 | 19 | 3.412 | 22,252 ms | 24,067 ms | 25,453 / 25,542 / 24,067 / 24,413 / 24,195 |
| NEXANS | neu | `range` über `MessageStatusIDX`, `key_len` 123 | 6.258 | 3 | 20 | 3.412 | 22,601 ms | 24,084 ms | 25,417 / 25,156 / 26,545 / 24,270 / 24,084 |
| SUTTONS | alt | `range` über `MessageStatusIDX`, `key_len` 123 | 6.257 | 2 | 90 | 3.472 | 22,186 ms | 23,397 ms | 24,762 / 23,780 / 25,364 / 25,605 / 23,397 |
| SUTTONS | neu | `range` über `MessageStatusIDX`, `key_len` 123 | 6.258 | 3 | 91 | 3.472 | 22,258 ms | 23,539 ms | 23,539 / 23,826 / 23,575 / 24,051 / 23,588 |

Die drei Bereiche der neuen Fassung, wie der Trace sie nennt:
`(COMMIT_REJECTED) <= (MessageStatus) <= (COMMIT_REJECTED)`,
`(EERP_FAILED) <= (MessageStatus) <= (EERP_FAILED)` und das Präfix — von `ERROR_` mit 24 × `\0`
bis `ERROR_` mit 24 × U+FFFF (hier nicht als Zeichen wiedergegeben).

**Alle vier Erwartungen treffen zu.** Der Plan ist in jedem Fall derselbe wie in der alten Fassung;
die Schätzung `rows` steigt um eins. Der dritte Bereich kostet **genau einen** Indexeinstieg mehr
(`read_key` +1 in allen vier Paaren) und liest auf der Testkopie **keinen** Indexsatz
(`read_next` gleich), weil es dort keine `EERP_FAILED`-Zeile gibt. Die Laufzeiten liegen innerhalb
der Streuung der fünf Läufe; die Unterschiede der besten Läufe sind kleiner als die Spannweite
innerhalb jeder Reihe.

**Sitzung 4** — Eichung der Wanduhr um `SELECT 1`: 0,804 / 0,807 / 0,852 ms

| Statement | Mandant | Fassung | Zugriff | `rows` | Bereiche auf `MessageStatusIDX` | `read_key` | `read_next` | Profil, beste von fünf | Wanduhr, beste von fünf | Wanduhr, alle fünf |
|---|---|---|---|---:|---:|---:|---:|---:|---:|---|
| Block 6 | NEXANS | alt | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 6.257 | 2 | 57 | 3.412 | 21,983 ms | 23,437 ms | 23,565 / 27,188 / 24,366 / 24,133 / 23,437 |
| Block 6 | NEXANS | neu | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 6.258 | 3 | 58 | 3.412 | 21,243 ms | 22,354 ms | 22,519 / 22,585 / 22,835 / 22,354 / 23,664 |
| Block 6 | SUTTONS | alt | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 6.257 | 2 | 7 | 3.413 | 21,371 ms | 22,412 ms | 23,704 / 22,412 / 23,700 / 23,549 / 22,557 |
| Block 6 | SUTTONS | neu | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 6.258 | 3 | 8 | 3.413 | 20,597 ms | 21,796 ms | 22,487 / 21,796 / 25,551 / 24,740 / 23,605 |
| Verteilung ohne Fehler | NEXANS | alt | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.514 | 929 | 10,032 ms | 11,120 ms | 11,120 / 11,354 / 12,321 / 13,151 / 13,510 |
| Verteilung ohne Fehler | NEXANS | neu | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.514 | 929 | 10,268 ms | 11,585 ms | 11,585 / 11,679 / 12,989 / 14,512 / 13,230 |
| Verteilung ohne Fehler | SUTTONS | alt | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.191 | 930 | 9,706 ms | 11,106 ms | 11,253 / 11,106 / 11,424 / 11,126 / 11,214 |
| Verteilung ohne Fehler | SUTTONS | neu | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.191 | 930 | 9,703 ms | 10,633 ms | 10,777 / 10,633 / 11,772 / 11,119 / 10,697 |
| Verengung | NEXANS | alt | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 757 | 756 | 57,168 ms | 58,647 ms | 58,985 / 61,971 / 58,647 / 58,830 / 58,888 |
| Verengung | NEXANS | neu | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 757 | 756 | 58,537 ms | 59,694 ms | 60,739 / 62,336 / 63,066 / 60,237 / 59,694 |
| Verengung | SUTTONS | alt | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 24 | 23 | 20,015 ms | 21,032 ms | 21,186 / 21,032 / 21,589 / 21,306 / 22,227 |
| Verengung | SUTTONS | neu | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 24 | 23 | 20,182 ms | 21,268 ms | 21,901 / 21,268 / 21,526 / 21,498 / 21,383 |

**Block 6 verhält sich wie die Lesung**: derselbe Plan, zwei auf drei Bereiche, `read_key` +1,
`read_next` gleich, Laufzeit innerhalb der Streuung. **Bei der Verteilung ohne Fehler und bei der
Verengung ist der Status kein Zugriffspfad** — der Zugriff ist `range` über `PRIMARY` (`stunde`)
bzw. `ref` über `message_rollup_prozess_idx`, ein Bereich auf dem Status kommt nicht vor; Plan **und
alle Zähler** sind alt und neu identisch.

**Sitzung 5** — Eichung der Wanduhr um `SELECT 1`: 0,754 / 0,705 / 0,730 ms

| Statement | Mandant | Fassung | Zugriff | `rows` | Bereiche auf `MessageStatusIDX` | `read_key` | `read_next` | Profil, beste von fünf | Wanduhr, beste von fünf | Wanduhr, alle fünf |
|---|---|---|---|---:|---:|---:|---:|---:|---:|---|
| Liste, `UNGEKLAERT` | NEXANS | alt | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 7.481 | 9 | 56 | 1.329 | 10,995 ms | 12,780 ms | 12,780 / 13,353 / 13,921 / 13,550 / 14,601 |
| Liste, `UNGEKLAERT` | NEXANS | neu | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 7.482 | 10 | 57 | 1.329 | 11,528 ms | 12,828 ms | 14,661 / 14,612 / 16,668 / 14,771 / 12,828 |
| Liste, `UNGEKLAERT` | SUTTONS | alt | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 7.481 | 9 | 90 | 1.383 | 11,873 ms | 13,426 ms | 14,580 / 15,112 / 14,889 / 13,426 / 13,621 |
| Liste, `UNGEKLAERT` | SUTTONS | neu | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 7.482 | 10 | 91 | 1.383 | 11,844 ms | 13,349 ms | 14,292 / 13,990 / 13,349 / 14,788 / 13,959 |
| Verengung, `UNGEKLAERT` | NEXANS | alt | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 756 | 755 | 57,487 ms | 60,288 ms | 61,404 / 60,621 / 61,296 / 60,847 / 60,288 |
| Verengung, `UNGEKLAERT` | NEXANS | neu | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 756 | 755 | 57,539 ms | 59,013 ms | 59,013 / 59,800 / 59,661 / 61,299 / 63,208 |
| Verengung, `UNGEKLAERT` | SUTTONS | alt | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 54 | 53 | 20,370 ms | 21,443 ms | 21,792 / 21,882 / 21,443 / 23,317 / 23,700 |
| Verengung, `UNGEKLAERT` | SUTTONS | neu | `message_rollup`: `ref` über `message_rollup_prozess_idx`, `key_len` 146 | 459 | — | 54 | 53 | 20,576 ms | 21,898 ms | 22,071 / 21,898 / 22,225 / 22,945 / 22,234 |
| Verteilung 48H, Richtung | NEXANS | alt | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.514 | 929 | 9,840 ms | 10,684 ms | 10,684 / 10,881 / 11,361 / 12,582 / 12,662 |
| Verteilung 48H, Richtung | NEXANS | neu | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.514 | 929 | 10,077 ms | 11,051 ms | 11,348 / 11,922 / 11,599 / 11,821 / 11,051 |
| Verteilung 48H, Richtung | SUTTONS | alt | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.191 | 930 | 9,714 ms | 10,632 ms | 11,140 / 11,364 / 10,632 / 11,031 / 11,047 |
| Verteilung 48H, Richtung | SUTTONS | neu | `message_rollup`: `range` über `PRIMARY`, `key_len` 5 | 929 | — | 2.191 | 930 | 9,780 ms | 10,649 ms | 10,649 / 10,930 / 10,799 / 13,833 / 11,597 |
| Verteilung 30T, Partner | NEXANS | alt | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 17.658 | 6.843 | 69,902 ms | 72,890 ms | 72,890 / 73,530 / 74,170 / 73,620 / 76,312 |
| Verteilung 30T, Partner | NEXANS | neu | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 17.658 | 6.843 | 71,844 ms | 74,199 ms | 75,105 / 74,257 / 74,199 / 74,922 / 75,430 |
| Verteilung 30T, Partner | SUTTONS | alt | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 12.289 | 6.844 | 53,234 ms | 54,286 ms | 57,561 / 55,790 / 54,286 / 56,070 / 54,702 |
| Verteilung 30T, Partner | SUTTONS | neu | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 12.289 | 6.844 | 54,251 ms | 55,138 ms | 55,703 / 55,419 / 55,471 / 55,881 / 55,138 |
| Verteilung 30T, Richtung | NEXANS | alt | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 17.658 | 6.843 | 70,604 ms | 72,166 ms | 76,158 / 72,878 / 73,060 / 72,166 / 72,321 |
| Verteilung 30T, Richtung | NEXANS | neu | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 17.658 | 6.843 | 71,833 ms | 73,386 ms | 75,566 / 74,482 / 75,136 / 74,443 / 73,386 |
| Verteilung 30T, Richtung | SUTTONS | alt | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 12.289 | 6.844 | 52,758 ms | 54,022 ms | 54,994 / 54,022 / 54,292 / 54,804 / 54,827 |
| Verteilung 30T, Richtung | SUTTONS | neu | `message_rollup_tag`: `range` über `PRIMARY`, `key_len` 3 | 14.214 | — | 12.289 | 6.844 | 53,826 ms | 55,348 ms | 56,459 / 55,789 / 55,348 / 55,953 / 57,639 |
| Verteilung 12M, Partner | NEXANS | alt | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 24.393 | 9.649 | 97,156 ms | 100,583 ms | 100,583 / 103,845 / 101,443 / 100,941 / 102,042 |
| Verteilung 12M, Partner | NEXANS | neu | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 24.393 | 9.649 | 98,830 ms | 101,909 ms | 106,786 / 102,214 / 105,139 / 102,898 / 101,909 |
| Verteilung 12M, Partner | SUTTONS | alt | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 15.899 | 9.650 | 69,069 ms | 70,805 ms | 70,805 / 71,025 / 70,817 / 71,048 / 71,688 |
| Verteilung 12M, Partner | SUTTONS | neu | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 15.899 | 9.650 | 70,228 ms | 71,970 ms | 73,425 / 72,267 / 71,970 / 72,377 / 73,863 |
| Verteilung 12M, Richtung | NEXANS | alt | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 24.393 | 9.649 | 97,128 ms | 98,135 ms | 98,135 / 98,767 / 99,178 / 98,281 / 99,392 |
| Verteilung 12M, Richtung | NEXANS | neu | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 24.393 | 9.649 | 98,804 ms | 100,324 ms | 102,210 / 104,321 / 100,899 / 100,324 / 101,265 |
| Verteilung 12M, Richtung | SUTTONS | alt | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 15.899 | 9.650 | 68,663 ms | 70,331 ms | 70,331 / 70,821 / 70,581 / 70,741 / 71,510 |
| Verteilung 12M, Richtung | SUTTONS | neu | `message_rollup_monat`: `range` über `PRIMARY`, `key_len` 3 | 6.033 | — | 15.899 | 9.650 | 70,729 ms | 72,413 ms | 72,990 / 72,413 / 73,013 / 73,085 / 72,652 |

**Die Liste mit `UNGEKLAERT` steigt ebenfalls über `MessageStatusIDX` ein** — das war nicht erwartet.
Der Optimierer macht aus `NOT (Fehler) AND NOT IN (bekannte Werte)` die **Lücken** zwischen den
ausgeschlossenen Werten: alt neun Intervalle, neu zehn, weil `EERP_FAILED` eine Lücke mehr schneidet.
`read_key` +1, `read_next` gleich (1.329 bzw. 1.383) — dieselbe Gestalt wie beim Filter `FEHLER`. Bei
der Verengung mit `UNGEKLAERT` und allen Verteilungen ist der Status kein Zugriffspfad; Plan und alle
Zähler sind alt und neu identisch.

**Alt gegen neu, verdichtet** — aus den Wanduhr-Spalten aller vier Tabellen; „paarweise langsamer"
zählt, in wie vielen der fünf gleich nummerierten Läufe die neue Fassung länger brauchte:

| Statement | Mandant | Wanduhr beste alt → neu | Δ beste | Wanduhr Median alt → neu | Δ Median | Δ Median % | neu paarweise langsamer |
|---|---|---|---:|---|---:|---:|---:|
| Lesung | NEXANS | 21,783 → 21,064 | -0,719 | 22,230 → 21,297 | -0,933 | -4,2 % | 1 von 5 |
| Lesung | SUTTONS | 22,378 → 22,206 | -0,172 | 23,146 → 23,747 | +0,601 | +2,6 % | 3 von 5 |
| Liste, `FEHLER` | NEXANS | 24,067 → 24,084 | +0,017 | 24,413 → 25,156 | +0,743 | +3,0 % | 1 von 5 |
| Liste, `FEHLER` | SUTTONS | 23,397 → 23,539 | +0,142 | 24,762 → 23,588 | -1,174 | -4,7 % | 2 von 5 |
| Block 6 | NEXANS | 23,437 → 22,354 | -1,083 | 24,133 → 22,585 | -1,548 | -6,4 % | 1 von 5 |
| Block 6 | SUTTONS | 22,412 → 21,796 | -0,616 | 23,549 → 23,605 | +0,056 | +0,2 % | 3 von 5 |
| Verteilung 48H, Partner | NEXANS | 11,120 → 11,585 | +0,465 | 12,321 → 12,989 | +0,668 | +5,4 % | 4 von 5 |
| Verteilung 48H, Partner | SUTTONS | 11,106 → 10,633 | -0,473 | 11,214 → 10,777 | -0,437 | -3,9 % | 1 von 5 |
| Verengung, `FEHLER` | NEXANS | 58,647 → 59,694 | +1,047 | 58,888 → 60,739 | +1,851 | +3,1 % | 5 von 5 |
| Verengung, `FEHLER` | SUTTONS | 21,032 → 21,268 | +0,236 | 21,306 → 21,498 | +0,192 | +0,9 % | 3 von 5 |
| Liste, `UNGEKLAERT` | NEXANS | 12,780 → 12,828 | +0,048 | 13,550 → 14,661 | +1,111 | +8,2 % | 4 von 5 |
| Liste, `UNGEKLAERT` | SUTTONS | 13,426 → 13,349 | -0,077 | 14,580 → 13,990 | -0,590 | -4,0 % | 2 von 5 |
| Verengung, `UNGEKLAERT` | NEXANS | 60,288 → 59,013 | -1,275 | 60,847 → 59,800 | -1,047 | -1,7 % | 2 von 5 |
| Verengung, `UNGEKLAERT` | SUTTONS | 21,443 → 21,898 | +0,455 | 21,882 → 22,225 | +0,343 | +1,6 % | 3 von 5 |
| Verteilung 48H, Richtung | NEXANS | 10,684 → 11,051 | +0,367 | 11,361 → 11,599 | +0,238 | +2,1 % | 3 von 5 |
| Verteilung 48H, Richtung | SUTTONS | 10,632 → 10,649 | +0,017 | 11,047 → 10,930 | -0,117 | -1,1 % | 3 von 5 |
| Verteilung 30T, Partner | NEXANS | 72,890 → 74,199 | +1,309 | 73,620 → 74,922 | +1,302 | +1,8 % | 4 von 5 |
| Verteilung 30T, Partner | SUTTONS | 54,286 → 55,138 | +0,852 | 55,790 → 55,471 | -0,319 | -0,6 % | 2 von 5 |
| Verteilung 30T, Richtung | NEXANS | 72,166 → 73,386 | +1,220 | 72,878 → 74,482 | +1,604 | +2,2 % | 4 von 5 |
| Verteilung 30T, Richtung | SUTTONS | 54,022 → 55,348 | +1,326 | 54,804 → 55,953 | +1,149 | +2,1 % | 5 von 5 |
| Verteilung 12M, Partner | NEXANS | 100,583 → 101,909 | +1,326 | 101,443 → 102,898 | +1,455 | +1,4 % | 3 von 5 |
| Verteilung 12M, Partner | SUTTONS | 70,805 → 71,970 | +1,165 | 71,025 → 72,377 | +1,352 | +1,9 % | 5 von 5 |
| Verteilung 12M, Richtung | NEXANS | 98,135 → 100,324 | +2,189 | 98,767 → 101,265 | +2,498 | +2,5 % | 5 von 5 |
| Verteilung 12M, Richtung | SUTTONS | 70,331 → 72,413 | +2,082 | 70,741 → 72,990 | +2,249 | +3,2 % | 5 von 5 |

**Wo der Status ein Zugriffspfad ist** (Lesung, Liste, Block 6, Liste mit `UNGEKLAERT`), liegen die
Unterschiede in beide Richtungen und innerhalb der Streuung.

> ⚠️ **Eine Erwartung trifft nicht zu: die Verteilung ohne Fehler über 30 Tage und 12 Monate.** Dort
> ist die neue Fassung im besten Lauf in **allen acht** Fällen (vier Formen, je zwei Mandanten)
> langsamer (+0,852 bis +2,189 ms), im Median in sieben von acht (bis +2,498 ms, +3,2 %), bei
> **identischen Zählern**. Die Reihenfolge erklärt es nicht: Im besten Lauf war die neue Fassung in
> beiden Reihenfolgen langsamer — bei `NEXANS` als zweite (auf dem wärmeren Puffer), bei `SUTTONS`
> als erste. Diese Statements lesen 6.843 bis 9.650
> Rollupzeilen und prüfen auf jeder die Bedingung `NOT (… LIKE … OR … IN (…))`; ein `IN` mit zwei
> Werten ist dort offenbar messbar teurer als ein `=`. **Der Aufschlag ist klein und benannt:** rund
> 1 bis 2,5 ms bei Laufzeiten von rund 54 bis 103 ms, weit unter der Zeitgrenze des Lese-Pools (10 s) — und
> er wächst mit der Zahl der Rollupzeilen im Fenster. Über 48 Stunden (929 Zeilen) ist nichts davon zu
> sehen.
>
> **Bei der Verengung ist ein solcher Aufschlag nicht belegt.** Mit `FEHLER` war `NEXANS` neu im
> Median um 1,851 ms langsamer (fünf von fünf Läufen); mit `UNGEKLAERT`, das dieselben 159.768
> Indexeinträge prüft (`icp_attempts` in allen vier `NEXANS`-Läufen gleich) und dabei dasselbe
> geänderte `IN (…)` gleich oft auswertet — dort verneint und um ein `NOT IN` über die übrigen
> bekannten Werte ergänzt —, war die
> neue Fassung um 1,047 ms **schneller**. Die Vorzeichen widersprechen sich; ein Aufschlag je
> geprüftem Eintrag, wie ihn der Fall `FEHLER` allein nahelegen würde, ist damit nicht belegt.

> ⚠️ **„Index-Merge" ist es nicht, und es war es nie.** Der Auftrag erwartete „weiterhin Index-Merge
> über `MessageStatusIDX`". Der Zugriff ist ein **einziger `range`** über **einen** Index, mit
> mehreren Intervallen — `type = range`, im Trace `range_scan`, nicht `index_merge`. So war es
> schon in der alten Fassung (M191 alt, M188). Gemeint war in §4.1 der Projektbeschreibung
> („zwei Indexbereiche, die MariaDB zusammenführen kann") genau das: Intervalle desselben Index.
> Kein Volldurchlauf über `Message` oder eine Rollup-Tabelle, in keinem Fall (`type = ALL` zeigt nur
> die Verengung, auf ihren eigenen abgeleiteten Zwischentabellen — alt wie neu).

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* auf der Testkopie **nichts über `EERP_FAILED`-Zeilen** — es gibt dort keine
> (0 von 3.341.519). Gemessen ist nur, dass die neue Bedingung in keinem der zwölf gemessenen Statements den
> Plan ändert, dort, wo der Status ein Zugriffspfad ist, einen Indexeinstieg mehr kostet, und auf der
> Verteilung ohne Fehler über 30 Tage und 12 Monate rund 1 bis 2,5 ms; bei der Verengung ist ein
> Aufschlag nicht belegt.
> *Behauptet wird:* dass der Wert **existiert** — aus der Beobachtung in der Produktion am
> 09.10.2026 — und dass er ein **Fehler** ist, durch die Entscheidung E‑233 vom selben Tag.
> **Die Lücke:** Beides ruht auf Auskunft und nicht auf Messung; die Testkopie kann es weder belegen
> noch widerlegen. Ungemessen ist auch, **wie viele** `EERP_FAILED`-Zeilen die Produktion trägt —
> und damit, was der dritte Bereich dort liest. Die Lesung skaliert mit der Zahl der Fehlerzeilen
> im Bestand, und die ist für die Produktion ohnehin nicht erhoben (offener Punkt 212,
> [`fehler-live.md`](fehler-live.md)).

---

## `EERP_PENDING` — Quittung ausstehend (13.10.2026, E‑234 bis E‑236)

**Der Anlass.** In der Produktion stand `EERP_PENDING` in der Nachrichtenliste als unbekannter
Wert mit Fragezeichen. Das war im Sinne von [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
§4.1 richtig — Rohwert, neutral, kein falscher Eimer. Jetzt ist der Wert bekannt.

**Drei Entscheidungen aus dem Sparring vom 13.10.2026:**

| | Entscheidung | Umgesetzt |
|---|---|---|
| **E‑234** | Eigene Einordnung `QUITTUNG_AUSSTEHEND`, Anzeige „Quittung ausstehend", neutral — das Gegenstück zu `QUITTIERT`, ausdrücklich **nicht** `WARTEND` | `MessageStatusKind` (gleich hinter `QUITTIERT`), `BEKANNT`, Farbrolle `offen`, Zeichen `Send`, Sprachdateien („Acknowledgement pending"), Statusfilter der Liste |
| **E‑235** | Kein Endstatus | `istEndstatus` liefert `false`. **Die zweite Hälfte trägt nicht:** „Bleibt die Empfangsbestätigung aus, wird die Nachricht über die bestehende Regel `istUeberfaellig` überfällig" — die Regel ist mit E‑71 (03.09.2026) entfallen. Auf Auswahl des Auftraggebers vom 13.10.2026 wird **nur eingeordnet**; eine eigene Überfälligkeitslogik ist nicht gebaut |
| **E‑236** | Die Testlücke nur benennen, nicht bauen | Kasten in §4.1 der Projektbeschreibung, offener Punkt 219 |

**Der Name.** Alle übrigen Einordnungen sind ein einziges Wort ohne Unterstrich. Geprüft sind
`UNQUITTIERT` (kollidiert mit der Problemkategorie *Unquittiert*) und `AUSSTEHEND` (verliert, was
aussteht); auf Rückfrage bleibt es bei `QUITTUNG_AUSSTEHEND`. Im übrigen Backend tragen Konstanten
häufig einen Unterstrich (`OffenerZustand.WARTET_VOR`).

### Bestandsaufnahme (13.10.2026, nur gelesen)

- **1a — Wie `istEndstatus` heute einen unbekannten Wert behandelt.** Unbekannt heißt
  `UNGEKLAERT`, und dafür liefert die Methode `true`. **Sie hat seit E‑71 im Anwendungscode keinen
  Aufrufer**; ihre einzige Wirkung ist der Compilerzwang des `switch`. Hängende `EERP_PENDING`
  sind deshalb seit dem Livegang **nirgends als überfällig gezählt worden** — die Kategorie
  *Überfällig* gibt es seit dem 03.09.2026 ohnehin nicht. Auch nach der Änderung zählt eine
  hängende Nachricht nirgends als Problem (offener Punkt 220).
- **1b — Wo Einordnungen aufgezählt oder abgebildet werden.** Backend: `MessageStatusKind`,
  `BEKANNT`, die beiden `switch` ohne `default` (`istEndstatus`, `bedingung`) — der Compiler
  erzwingt beide. Alles Übrige folgt aus der Zuordnung (Verlauf über `EnumMap`, Statusfilter über
  `valueOf`, `statusKind` als Name). Die „offenen" Zustände in Liste, Detail und BAM-Suche zählen
  `WARTEND` und `LAEUFT` von Hand auf und bleiben dabei — `QUITTUNG_AUSSTEHEND` zeigt keinen
  Schritt, kein Alter, keine Frist. Frontend: Typ `Statusart`, Laufzeitliste `STATUSARTEN` (Filter,
  URL), Farbrolle `ZUORDNUNG`, Zeichen `STATUSZEICHEN`, Sprachdateien. **`STATUSARTEN` fehlte bisher
  still**, wenn ein Wert vergessen wurde; `tests/nachrichtenfilter.test.ts` hält die Liste seither
  gegen Zeichen und Sprachdatei. **Keine Kachel und keine Diagrammreihe zieht die neue Einordnung
  in *Läuft* oder *Wartend*:** Beide Kacheln vergleichen auf ihren einen Rohwert
  (`einzigerRohwert`), der Verlauf ordnet über die Farbrolle — im Tooltip steht sie unter *Ohne
  Ergebnis* neben Wartend, Läuft, Aufgeteilt und Zusammengeführt, mit gleicher Farbe und ohne
  Kachel.
- **1c — Die Überfällig-Abfrage gibt es nicht.** Mit E‑71 sind `istUeberfaellig`,
  `ueberfaelligBedingung`, die Kachel und der Listenparameter entfallen. **Schritt 5 des Auftrags
  (EXPLAIN gegen M90) ist damit gegenstandslos.** Geändert haben sich stattdessen vier andere
  Statements, gemessen in M192 unten.
- **1d — Der DISTINCT-Test** nimmt `RUNNING` aus der erwarteten Menge (`erwartet.remove`), seit
  dem Vormittag auch `EERP_FAILED`. `EERP_PENDING` geht genauso; `bekannt ⊇ vorhanden` bleibt die
  eigentliche Sicherung.

**Die Zählung** (13.10.2026, 12:11:51, `monitor_read`):
`SELECT COUNT(*) FROM GlassfishDB.Message WHERE MessageStatus = 'EERP_PENDING'` → **0**.

### Die geänderten Statements (M192, nur `EXPLAIN`)

**Eine Überfällig-Abfrage gibt es nicht** (1c). Mit der neuen Einordnung ändern sich aber vier
Statementtexte: Liste und Vorabfrage der Verengung mit Statusfilter `UNGEKLAERT` — `EERP_PENDING`
steht seither im `NOT IN` — und dieselben beiden mit dem **neuen** Filter `QUITTUNG_AUSSTEHEND`.
Gefahren am 13.10.2026 gegen die Testkopie, für `NEXANS` und `SUTTONS`, je `EXPLAIN` und die
gewählten Indexbereiche aus dem Optimizer-Trace; **keine Laufzeitreihe**. Die Texte sind aus dem
Code gerendert ([`scripts/messung-eerp-pending/`](../scripts/messung-eerp-pending/)); der neue Text
für `UNGEKLAERT` unterscheidet sich vom in M191 gemessenen nur um `'EERP_PENDING'` im `NOT IN`
(im Generator geprüft), und beide stehen in derselben Sitzung nebeneinander.

| Statement | Fassung | Zugriff | `rows` | Bereiche |
|---|---|---|---:|---:|
| Liste, `UNGEKLAERT` | M191 | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 7.482 | 10 |
| Liste, `UNGEKLAERT` | neu | `Message`: `range` über `MessageStatusIDX`, `key_len` 123 | 7.483 | 11 |
| Verengung, `UNGEKLAERT` | M191 und neu | `message_rollup`: `ref` über `message_rollup_prozess_idx` | 459 | — |
| Liste, `QUITTUNG_AUSSTEHEND` | neu | `Message`: `ref` über `MessageStatusIDX`, `key_len` 123 | 1 | 1 |
| Verengung, `QUITTUNG_AUSSTEHEND` | neu | `message_rollup`: `ref` über `message_rollup_prozess_idx` | 459 | — |

Die Zeilen gelten für beide Mandanten gleich. **Gleiche Zugriffsart, gleicher Index:** Die Liste
mit `UNGEKLAERT` liest weiter die Lücken zwischen den bekannten Werten auf `MessageStatusIDX`, mit
einer Lücke mehr; die Verengung bleibt beim Prozessindex des Rollups. Der neue Filter liest genau
das eine Intervall `(EERP_PENDING) <= (MessageStatus) <= (EERP_PENDING)`.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* davon nichts. Die Testkopie trägt den Wert null Mal (Zählung vom 13.10.2026,
> 12:11:51, `monitor_read`), sie kann ihn weder belegen noch widerlegen.
> *Behauptet wird:* Die Datei ist per OFTP übertragen, die EERP des Partners steht aus; der Status
> ist flüchtig; `MessageTimeout` ist für diese Nachrichten sinnvoll gesetzt. Quelle: Auskunft des
> Auftraggebers vom 13.10.2026 und Beobachtung in der Produktion.
> **Die Lücke:** Der Übergang von `EERP_PENDING` nach `EERP_RECEIVED` ist nicht messbar, weil
> `GlassfishDB` keine Statushistorie führt. Und die Frist trägt heute nichts: Keine Regel liest
> `MessageTimeout` für diesen Status (E‑71).

**Nicht gebaut** (Auftrag): kein Logeintrag und kein Adminhinweis für unbekannte Status, keine
Änderung daran, wie `istEndstatus` Unbekanntes behandelt, keine Kachel, keine Farbrolle, keine
Kategorie *Unquittiert*, keine Einordnung von `COMMIT_SENT`, `CHECKED`, `CKECKED`.

> ⚠️ **Eine Einschränkung, die bleibt: Unter der Schwelle ist die Statusspalte für das neue Wort zu
> schmal.** Dort ist ihre Breite an „Zusammengeführt" gemessen (Nachrichtenliste 156 px unter der
> Containerschwelle, Trefferliste fest 155 px; M177/M180, [`spaltenwahl.md`](spaltenwahl.md));
> „Quittung ausstehend" ist länger und wird mit Auslassungspunkten gekürzt. Ab der Schwelle trägt die
> Spalte der Nachrichtenliste 286 px (E‑162); ohne Schritt daneben steht die Plakette dort
> voraussichtlich ungekürzt — gemessen ist auch das nicht. **Der Tooltip der Plakette trägt seither
> die ungekürzte Beschriftung** (vorher nur den Rohwert). Neu messen ginge nur mit einer
> Sichtprüfung in allen vier Dichtestufen — nicht Teil dieser Runde.

---

## Die Fehlerbedingung — eine Stelle, ein Ausdruck

`MessageStatusClassifier.fehlerBedingung(Field<String>)` liefert:

```sql
MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'
```

**Nicht** `LEFT(MessageStatus, 6) = 'ERROR_'`: In SQL ist `_` ein Platzhalter für ein beliebiges
Zeichen, und die `LEFT`-Form kann `MessageStatusIDX` nicht nutzen — sie erzwingt in Kombination mit
der Oder-Bedingung einen vollen Durchlauf über 2,9 GB. Die `LIKE`-Fassung ergibt zwei Indexbereiche,
die MariaDB zusammenführen kann.

Das Feld wird als Parameter übergeben, damit dieser gemeinsame Baustein nicht auf generierte
`jooq.glassfish`-Typen zugreifen muss — die Fehlerbedingung bleibt in `common`.

> ### ⚠️ Geändert *(Entscheidung E‑233 vom 09.10.2026, eingetragen am 13.10.2026)*
>
> **Der Text oben bleibt stehen.** Seit `EERP_FAILED` ein Fehler ist, liefert
> `fehlerBedingung`:
>
> ```sql
> MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus IN ('COMMIT_REJECTED', 'EERP_FAILED')
> ```
>
> Weiterhin **keine** `LEFT()`-Form und **kein** Muster wie `'%_FAILED'`: Jeder Fehler ohne Präfix
> steht einzeln da. Die Liste hinter `IN` ist im Code nicht ein zweites Mal aufgezählt, sondern aus
> derselben Zuordnung gelesen wie die Java-Einordnung (`FEHLER_OHNE_PRAEFIX` aus `BEKANNT`) —
> SQL und Java können sich über die Menge nicht uneinig werden.
>
> **Aus zwei Indexbereichen sind drei geworden.** Gemessen in M191 (Abschnitt „`EERP_FAILED`"
> oben): derselbe `range` über `MessageStatusIDX` wie vorher, ein Indexeinstieg mehr, auf der
> Testkopie kein gelesener Satz mehr, Laufzeit innerhalb der Streuung.
>
> Die Lücke zwischen SQL und Java für Rohwerte mit angehängtem Leerzeichen (`'COMMIT_REJECTED '`
> unter `PAD SPACE`, offener Punkt 213 in [`fehler-live.md`](fehler-live.md)) gilt für
> `'EERP_FAILED '` genauso. In der Testkopie kommt keiner der beiden vor (M191, bytegenaue Zählung).

---

## Endstatus und Überfälligkeit (ergänzt 01.08.2026, Schritt 4)

### `istEndstatus(MessageStatusKind)`

> **Offen sind allein `WARTEND` und `LAEUFT`.** Alles andere ist Endstatus.

> ⚠️ **Diese Methode gehört der Überfälligkeitsrechnung und sonst niemandem** (präzisiert am
> 06.08.2026). Sie beantwortet genau eine Frage: *Kann für diese Zeile noch eine Frist ablaufen?*
> Sie ist **nicht** die Grundlage eines späteren Filters „nur offene Nachrichten". Dort würde sie
> 1.051 `COMMIT_SENT`-Zeilen lautlos verschwinden lassen — obwohl weiter oben in dieser Datei steht,
> dass wir über genau diese Zeilen **nichts wissen**. Für `UNGEKLAERT` liefert sie `true`, und das
> ist in der Überfälligkeitsrechnung die vorsichtige Antwort (keine Behauptung, die Nachricht hänge);
> in einem Sichtbarkeitsfilter wäre dieselbe `true` die unvorsichtige. Wer „offen" im Sinne der
> Oberfläche braucht, definiert das dort — und begründet es dort.
>
> ✅ **Der Fall ist am 04.09.2026 eingetreten — und so ist er aufgelöst worden.** Die
> Oberfläche hat „offen" nie definiert. Stattdessen war der **Tokenname** der Farbrolle
> `--status-offen` in die **Beschriftungsposition** von Legende und Tooltip des
> Verlaufsdiagramms gerutscht und behauptete dort über `AUFGETEILT` und `ZUSAMMENGEFUEHRT`
> das Gegenteil der Tabelle darunter — gesehen als *Offen 49* über *Aufgeteilt 1* und
> *Zusammengeführt 48* in einem Eimer mit 60 Nachrichten. **Aufgelöst ist er nicht durch eine
> Definition, sondern durch den Verzicht auf das Wort:** Die Rolle heißt in der Oberfläche
> seither **„Ohne Ergebnis"**, der Token bleibt `--status-offen` (E‑82,
> [`dashboard-frontend.md`](dashboard-frontend.md) §5.2). **`istEndstatus` und diese Tabelle
> sind nicht angefasst.**

| Einordnung | Endstatus? | Rohwerte |
|---|---|---|
| `ABGESCHLOSSEN` | ja | `FINISHED` |
| `QUITTIERT` | ja | `EERP_RECEIVED`, `COMMIT_RECEIVED` |
| `FEHLER` | ja | `ERROR_*`, `COMMIT_REJECTED`, `EERP_FAILED` *(seit 13.10.2026, E‑233)* |
| `AUFGETEILT` | **ja** | `SPLITTED` |
| `ZUSAMMENGEFUEHRT` | **ja** | `MERGED` |
| `UNGEKLAERT` | **ja** | `CHECKED`, `CKECKED`, `COMMIT_SENT`, alles Unbekannte |
| `WARTEND` | **nein** | `SUSPENDED` |
| `LAEUFT` | **nein** | `RUNNING` |
| `QUITTUNG_AUSSTEHEND` | **nein** *(seit 13.10.2026, E‑235)* | `EERP_PENDING` |

> **Ergänzt 13.10.2026:** „Offen sind allein `WARTEND` und `LAEUFT`" (oben) gilt seither nicht
> mehr — auch `QUITTUNG_AUSSTEHEND` ist kein Endstatus. Wirkung zur Laufzeit hat das keine: Die
> Methode hat keinen Aufrufer, und die offenen Zustände in Liste, Detail und BAM-Suche bleiben
> ausdrücklich `WARTEND` und `LAEUFT`.

> **Die Aufteilung am 11.08.2026 hat an dieser Tabelle nichts geändert außer der Zeilenzahl.** Aus
> einer Zeile wurden zwei mit demselben Wert. `MessageStatusClassifierTest` hält beides fest — dass
> beide Endstatus sind und dass keiner von beiden je überfällig wird.

> ### ⚠️ Ergänzt am 18.09.2026 — ein Fehlerstatus ist fachlich nicht endgültig
>
> **Die Tabelle bleibt stehen, und `istEndstatus` bleibt unberührt.** Eine Nachverarbeitung setzt
> eine Nachricht im Fehler auf `RUNNING` — ein Fehlerstatus ist damit **fachlich nicht endgültig**.
> `istEndstatus` beantwortet weiter allein die Frage der Überfälligkeitsrechnung (Kasten darüber),
> und sie urteilt über einen Status, nicht über die Geschichte einer Nachricht: Steht die Nachricht
> nach der Nachverarbeitung auf `RUNNING`, ist sie eine Zeile in `LAEUFT` wie jede andere.
>
> **Herkunft:** Auftraggeber, 18.09.2026, **nicht gemessen** — auf der Testkopie kommt `RUNNING`
> null Mal vor, eine Nachverarbeitung ist dort nicht zu sehen.
>
> **Was daraus folgt, steht in [`fehler-live.md`](fehler-live.md):** Der Statuswechsel bucht die
> Nachricht in die Stunde der Nachverarbeitung um, und ihr alter Rollup-Eimer behält den Fehler bis
> zum Volllauf (hergeleitet, nicht an der Produktion gemessen). Die Übersicht liest die Einordnung
> `FEHLER` deshalb seit dem 18.09.2026 live (E‑208, die vierte benannte Ausnahme von Regel L2). Die
> Fehlerbedingung oben ist dieselbe geblieben — die Lesung ruft sie und baut sie nicht nach.

**Warum `AUFGETEILT` und `ZUSAMMENGEFUEHRT` fertig sind.** Eine gemergte oder gesplittete Nachricht wird nicht wieder
angefasst — sie ist als *Zeile* fertig, auch wenn der fachliche Vorgang über die Verkettung
weiterläuft. Messung M6 stützt das: `SPLITTED` und `MERGED` verteilen sich über fünfzehn Monate in
stabiler Größenordnung (38.428 bis 57.426 bzw. 20.609 bis 34.937 je Monat) und ballen sich **nicht**
am aktuellen Rand; wären sie flüchtige Zwischenzustände, sähe die Verteilung anders aus. Zählten sie
als offen, wären **34,38 Prozent aller Zeilen** Kandidaten für „überfällig" — und die Kategorie wäre
Rauschen.

**Warum `UNGEKLAERT` hier als fertig zählt.** Das ist keine Behauptung, die Nachricht sei fertig,
sondern die Weigerung, das Gegenteil zu behaupten: Sie als offen zu führen hieße, wir wüssten, dass
sie *nicht* fertig ist. Genau das wissen wir nicht. So bleibt sie aus **allen drei**
Problemkategorien heraus, statt mit einer Vermutung gefüllt zu werden.

Umgesetzt als vollständiges `switch` ohne `default`: Ein neuer Wert in `MessageStatusKind` löst
einen Compilerfehler aus und erbt keine stille Voreinstellung.

### Überfälligkeit

> ## ⚠️ Diese Kategorie ist am 03.09.2026 widerlegt und aus dem MVP genommen (E‑71)
>
> **Der ganze Abschnitt darunter bleibt im Wortlaut stehen** — er ist die Vorarbeit, gegen die die
> Widerlegung zu lesen ist, und ohne ihn wäre nicht mehr erkennbar, worauf sie sich bezieht.
> **Gebaut ist davon nichts mehr:** `MessageStatusClassifier.istUeberfaellig`,
> `timeoutZeitpunkt` und `ueberfaelligBedingung` sind entfallen, der Listenparameter
> `ueberfaellig` ebenso, und das Dashboard zeigt statt der Kachel *Überfällig* die beiden Kacheln
> *Läuft* und *Wartend* ([`dashboard.md`](dashboard.md)).
>
> ### Die Regel, die sie widerlegt
>
> 1. Eine Nachricht, die länger als `MessageTimeout` in `RUNNING` steht, wird vom Altsystem
>    automatisch auf `ERROR_TIMEOUT` gesetzt.
> 2. `SUSPENDED`-Nachrichten warten **absichtlich** — auf einen Folgeprozess, etwa auf den Versand
>    zu einem bestimmten Zeitpunkt. Sie werden nie automatisch beendet und sind **nicht
>    überfällig**.
> 3. In der Praxis liegen sie höchstens **rund eine Woche**.
>
> ### Herkunft
>
> **Herkunft:** fachliche Auskunft des Auftraggebers vom 03.09.2026. **Nicht gemessen.**
> Die Testkopie kann sie nicht belegen: `RUNNING` kommt dort null Mal vor, und die 538
> `SUSPENDED` sind der Bestand *eines* Status in *einer* Gestalt. **Gegen die Produktion zu
> prüfen** mit der Abfrage in `docs/message-status.md`, Abschnitt „Die offene Prüfung".
>
> ### Was daraus folgt
>
> *Überfällig* ist definiert als **nicht in einem Endstatus** und **Frist abgelaufen**. Offen sind
> genau zwei Statuswerte, und beide fallen weg:
>
> | | |
> |---|---|
> | `SUSPENDED` | wartet absichtlich → nie überfällig. **Falsch positiv, per Definition** |
> | `RUNNING` | über der Frist nur im Spalt zwischen Fristablauf und dem Zuschlagen des Wächters. Danach `ERROR_TIMEOUT` und damit **Fehler** |
>
> **Die Kategorie hat im eingeschwungenen Zustand keine wahren Treffer.** Gemessen an der
> Testkopie markierte `istUeberfaellig` im Gesamtbestand **538** Zeilen, davon **538 `SUSPENDED`**
> und **0 `RUNNING`** — **538 Fehlalarme und kein einziger Treffer.** Nicht die Mehrheit, alle.
>
> **Die Rechnung, die das Werkzeug anstellte, stellt das Altsystem bereits an;** sein Ergebnis
> heißt `ERROR_TIMEOUT` und steht bei uns in der Fehlerkachel, namentlich aufgeschlüsselt. Eine
> zweite Fassung derselben Auskunft, mit anderer Uhr und ohne Schreibrecht, ist keine zusätzliche
> Aussage.
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* dass `istUeberfaellig` auf der Testkopie 538 Zeilen markiert, davon 538
> > `SUSPENDED` und null `RUNNING` (M90, in M144 am geltenden Anker bestätigt); und dass die
> > älteste dieser Zeilen **6,71 Tage** vor dem Anker liegt (M144 c).
> > *Behauptet wird:* Die Kategorie hat **keine** wahren Treffer.
> > **Die Lücke, und sie ist der ganze Inhalt dieses Vermerks:** Der Schluss ruht **nicht** auf der
> > Messung, sondern auf der Auskunft oben. Gemessen ist nur, dass alle 538 Treffer `SUSPENDED`
> > sind — dass `SUSPENDED` nie überfällig *ist*, sagt der Auftraggeber und nicht der Bestand. Die
> > 6,71 Tage sind mit „höchstens rund eine Woche" **verträglich** und belegen sie nicht: Eine
> > Gestalt, ein Status, ein Zeitraum von sieben Tagen.

**Der Stand bis zum 03.09.2026, wortgleich:**

> **Überfällig** = **nicht** in einem Endstatus **und** `MessageLastUpdate + MessageTimeout` liegt
> vor dem Zeitpunkt der **Anwendungsuhr**.

- **`MessageTimeout` ist eine Dauer in Sekunden** (Messung M8, korrigiert am 01.08.2026 — die
  Dokumentation nannte Minuten). Die Einheit steht im Code an genau einer Stelle:
  `MessageStatusClassifier.TIMEOUT_EINHEIT`.
- **`MessageTimeout = 0` wird als „kein Timeout" behandelt.** Solche Zeilen werden nie überfällig
  (6.915 Zeilen, M2).

  > ⚠️ **Das ist eine Analogie, kein Befund** (ergänzt am 06.08.2026). Ein Gegenbeleg steht in
  > derselben Messung, die die Einheit geklärt hat: Bei **allen 52** `ERROR_TIMEOUT`-Nachrichten
  > trägt die fehlschlagende Aktion `SOSActionTimeout = 0` — und trotzdem greift dort eine Frist von
  > höchstens 120 Sekunden (M8). In dieser Spalte bedeutet `0` also eher „nimm die Vorgabe" als
  > „keine Frist". Ob `Message.MessageTimeout` dasselbe meint, ist **nicht** belegt; die Übertragung
  > ist dieselbe Analogie wie bei der Einheit.
  >
  > **Praktisch folgenlos ist das heute nur aus einem Grund:** Alle 6.915 Zeilen mit
  > `MessageTimeout = 0` stehen in einem Endstatus (`FINISHED` 5.711, `COMMIT_SENT` 1.051,
  > `COMMIT_REJECTED` 103, `CHECKED` 50) und werden damit ohnehin nie überfällig — die Lesart der
  > `0` ändert an keiner einzigen Zeile etwas. Taucht in Produktion eine **offene** Zeile mit `0`
  > auf, ist das die Stelle, an der nachzusehen ist.
- **`NULL` wird behandelt**, obwohl es in 3,34 Millionen Zeilen kein einziges Mal vorkommt — die
  Spalte lässt es zu, und die Produktion muss sich nicht daran halten, was die Testkopie zufällig
  enthält.
- **„Läuft noch" heißt „nicht in einem Endstatus"**, nicht `MessageStatus = 'RUNNING'`.
- Getrennt von *Fehler* und *Unquittiert*, niemals mit ihnen zusammengefasst (Regel Q3). `SUSPENDED`
  ist kein Fehler — aber offen, und damit der eigentliche Kandidat dieser Kategorie.

In SQL sind die offenen Zeilen genau `MessageStatus IN ('SUSPENDED','RUNNING')`; jeder andere und
jeder unbekannte Wert ist Endstatus.

### Messung gegen die Testkopie (Regel L7)

Bezugspunkt ist die Anwendungsuhr im Profil `dev`, also der Anker aus M9 (`2025-12-30 04:09:47`).

```sql
SELECT COUNT(*) FROM Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
  AND MessageTimeout > 0
  AND MessageLastUpdate + INTERVAL MessageTimeout SECOND < ?;
```

`EXPLAIN`: `type=range`, `key=MessageStatusIDX`, `rows=539`, `Using index condition; Using where` —
kein voller Durchlauf. Laufzeit **11,0 ms** für die zusammengesetzte Auswertung, 4,5 ms für die
Einzelabfrage.

| | Zeilen |
|---|---|
| offen (nicht Endstatus), Gesamtbestand | 538 |
| davon **überfällig**, Gesamtbestand | **538** |
| offen im 24-h-Standardfenster | 1 |
| davon **überfällig** im 24-h-Standardfenster | **1** |

**Die Erwartung „nahezu null überfällige Zeilen" trifft zu — aber aus einem anderen Grund als
angenommen.** Nicht weil kaum etwas überfällig wäre: Über den Gesamtbestand sind es **alle 538**
offenen Zeilen, denn `RUNNING` kommt null Mal vor und die 538 `SUSPENDED` enden am 29.12.2025, also
vor dem Anker. Im Standardfenster von 24 Stunden liegt schlicht nur **eine** offene Zeile überhaupt.

> ⚠️ **Die Kategorie „Überfällig" ist gegen die Testkopie praktisch nicht prüfbar.** Sie zeigt dort
> entweder eine Zeile (24 h) oder alle offenen (unbegrenzt) — beides sagt nichts darüber, ob die
> Frist fachlich richtig gewählt ist. Das war schon in `message-status.md` für `RUNNING` vermerkt und
> gilt für diese Kategorie insgesamt.

**Nebenbefund zur Einheit:** Dieselbe Abfrage mit `INTERVAL … MINUTE` statt `SECOND` liefert 537
statt 538 — ein Unterschied von einer Zeile. **Auf der Testkopie ist die Wahl der Einheit also
nahezu unsichtbar**, weil der Anker weit hinter allen offenen Zeilen liegt. In Produktion, wo
Nachrichten in Minuten laufen, ist es der Unterschied zwischen 30 Minuten und 30 Stunden. Ein Test,
der die Einheit gegen die Testkopie belegen wollte, könnte das nicht leisten — deshalb steht sie als
benannte Konstante im Code und ihre Begründung in `messungen-schritt4.md` M8.

---

## Die offene Prüfung

**Sie entscheidet die Regel aus dem Abschnitt „Überfälligkeit", und sie gehört gegen die
Produktion.** Die Testkopie kann sie nicht beantworten: `RUNNING` kommt dort null Mal vor.

**Ausdrücklich nicht im Anfragepfad.** Das ist eine Erhebung und kein Anwendungscode.

```sql
SELECT MessageStatus, COUNT(*) AS zeilen,
       MIN(MessageLastUpdate) AS aelteste,
       MAX(TIMESTAMPDIFF(SECOND, MessageLastUpdate, NOW())) AS aeltestes_alter_sekunden
FROM GlassfishDB.Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY MessageStatus;
```

### Die Erwartung, **vor** dem Ergebnis festgehalten

| Status | erwartet |
|---|---|
| `RUNNING` | trägt **kein** Alter oberhalb von `MessageTimeout` zuzüglich eines Wächtertakts |
| `SUSPENDED` | trägt eines, aber **keines über rund einer Woche** |

**Trifft das nicht zu, ist die Regel falsch oder unvollständig, und Schritt 10b‑4 ist neu zu
bewerten.**

### Was dieselbe Abfrage gegen die Testkopie sagt — und warum das nichts entscheidet

Gefahren am 03.09.2026 als M144 (c):

| Status | Zeilen | älteste | ältestes Alter |
|---|---:|---|---:|
| `SUSPENDED` | **538** | `2025-12-23 11:04:13` | **579.934 s = 6,71 Tage** |
| `RUNNING` | — | — | *kommt nicht vor* |

**Die zweite Zeile ist der Grund, warum die Prüfung offen bleibt.** Über `SUSPENDED` sagt die
Testkopie etwas, das mit der Auskunft verträglich ist — 6,71 Tage liegen unter „rund einer Woche".
Über `RUNNING` sagt sie **nichts**, und genau dort steht die Behauptung, an der die ganze
Widerlegung hängt: dass der Wächter zuschlägt.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* der Bestand **eines** Status auf **einer** Kopie.
> *Behauptet wird:* eine Eigenschaft **beider** offenen Status in der **Produktion**.
> **Die Lücke ist grundsätzlich** und durch keine Messung an dieser Kopie zu schließen.

---

## Sicherung gegen neue Statuswerte

`DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge` vergleicht `SELECT DISTINCT
MessageStatus` gegen die bekannte Menge:

1. **Kein unbekannter Wert** darf auftauchen (`bekannt ⊇ vorhanden`) — die eigentliche Sicherung.
2. Die vorhandenen Werte sind **genau** die bekannte Menge ohne `RUNNING`.

Der Test wird rot, sobald im Altsystem ein neuer Status auftaucht **oder** ein dokumentierter Wert
verschwindet. Nur deshalb ist es vertretbar, unbekannte Werte neutral zu behandeln, ohne dass ein
neuer Status unbemerkt aus der Problemsicht verschwindet. Reagiert wird durch Pflege dieser Datei und
des `MessageStatusClassifier`, nie durch Raten.

> ### ⚠️ Ergänzt am 13.10.2026 — der Test sieht die Testkopie und nicht die Produktion
>
> **Der Text oben bleibt stehen.** Zwei Dinge haben sich geändert:
>
> 1. **Punkt 2 nimmt seither auch `EERP_FAILED` aus.** Die vorhandenen Werte sind genau die
>    bekannte Menge ohne `RUNNING` **und ohne `EERP_FAILED`** — der Wert ist bekannt, kommt in der
>    Testkopie aber null Mal vor (M191). Taucht er dort doch auf, wird der Test rot, und diese Datei
>    ist nachzuziehen. Gelaufen am 13.10.2026 gegen die Testkopie: grün.
> 2. **Der Satz „Der Test wird rot, sobald im Altsystem ein neuer Status auftaucht" gilt nur für die
>    Testkopie.** `EERP_FAILED` ist in der Produktion aufgefallen, und der Test konnte ihn
>    grundsätzlich nicht sehen: Er liest die Kopie mit Datenstand 08.07.2026. Gefunden hat ihn ein
>    Mensch, der in der Liste eine Fragezeichen-Plakette gesehen hat. Die neutrale Behandlung
>    unbekannter Werte, die dieser Abschnitt begründet, setzt aber einen Wächter voraus, der die
>    **Produktion** erreicht. Offener Punkt **218** unten.
>
> **Später am 13.10.2026 ein Drittes:** Punkt 2 nimmt seither auch `EERP_PENDING` aus (E‑234) —
> flüchtig wie `RUNNING`, in der Testkopie 0 Zeilen. Und selbst gegen die Produktion wäre der Test
> ein **Schnappschuss**, der flüchtige Status nur zufällig sieht: offener Punkt **219** unten.

---

## Offene Punkte

- **218 — Der Wächter gegen neue Statuswerte erreicht die Produktion nicht** *(eingetragen
  13.10.2026, M191)*. [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1, „Sicherung gegen neue
  Statuswerte", begründet die neutrale Behandlung unbekannter Werte damit, dass ein Test rot wird,
  sobald im Altsystem ein unbekannter Wert auftaucht. `DatenzugriffDbIT` liest aber nur die
  Testkopie (Datenstand 08.07.2026); ein Wert, der erst in der Produktion entsteht, bleibt ihm
  unsichtbar — `EERP_FAILED` ist genau so aufgefallen, über die Fragezeichen-Plakette in der Liste
  und nicht über den Test. **Offen ist, wie ein Wächter die Produktion erreicht.** Eine Lösung ist
  ausdrücklich nicht Teil dieser Runde; gebaut ist nichts.
- **219 — Ein Laufzeitsignal für unbekannte Status** *(eingetragen 13.10.2026, E‑236)*. **Entschieden:
  nicht gebaut** — kein Logeintrag, kein Adminhinweis. Ein stündliches `SELECT DISTINCT` wäre wieder
  nur ein Schnappschuss und sähe flüchtige Status wie `RUNNING` und `EERP_PENDING` nur zufällig. Die
  verlässlichste Stelle für ein solches Signal wäre der Klassifizierer: Er sieht **jeden** Wert, der
  gelesen wird, in dem Augenblick, in dem er gelesen wird.
- **220 — Eine dauerhaft auf `EERP_PENDING` stehende Nachricht ist fachlich unquittiert**
  *(eingetragen 13.10.2026, E‑235)*. Im MVP zählt sie **in keiner** Problemkategorie: nicht als
  Fehler, nicht als überfällig (seit E‑71 entfallen; die erwartete Überfälligkeit über
  `istUeberfaellig` trägt deshalb nicht), und *Unquittiert* ist nicht gebaut (E‑d). Wird
  *Unquittiert* gebaut, ist zu entscheiden, in welcher Kachel sie steht — sonst steht derselbe Beleg
  in zwei Kacheln ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.2, Punkt 3). Bis dahin
  fällt eine hängende Nachricht nur auf, wer die Liste nach *Quittung ausstehend* filtert.
