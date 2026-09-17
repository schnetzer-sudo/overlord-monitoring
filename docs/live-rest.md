# Live-Rest der laufenden Stunde — Teil A: der Baustein

*17.09.2026, begonnen und **angehalten**.* Auftrag „Live-Rest der laufenden Stunde, Teil A (Baustein
und Prozessbaum)", Stand 17.09.2026. Entscheidungen **E‑179** bis **E‑182**, **keine** Messung, neue
offene Punkte **186** bis **189**.

**Die Frage dieser Datei:** Wie bekommt der Prozessbaum den Verkehr seit dem letzten Rollup-Lauf
dazugerechnet, damit Baum und Übertragungsliste für dasselbe Fenster dieselbe Zahl zeigen — und zwar
über **einen** Baustein, den später auch das Dashboard ruft (Teil B)?

> ### ⚠️ Der Stand: angehalten vor der Live-Lesung — die Regeln lassen sie in `common` nicht zu
>
> **Gebaut und grün** sind die drei Teile, die an keiner Regelentscheidung hängen, je als eigener
> Commit: der **Wasserstand** als Schnittstelle in `common` (§4), die **Stundenbildung** des Rollups
> in `common` (§5) und die **Entscheidung samt Verrechnung** des Live-Rests als reine Funktionen in
> `common` (§3, §6), mit 20 Fällen ohne Datenbank.
>
> **Nicht gebaut** ist die **mandantengefilterte Lesung des Live-Bereichs** (Teil 1.3 des Auftrags:
> minus `message_rollup`, plus `Message`, beide mit Mandantenkette) — und alles, was an ihr hängt:
> die Verrechnung im `ProzessbaumService` (Teil 2), die Messung am gebauten Code (Teil 3), die
> Oberfläche (Teil 4) und die Datenbanktests (Teil 5).
>
> **Der Grund** steht in §7: `PaketstrukturTest.common_haengt_an_keinem_anderen_anwendungspaket`
> verbietet `common` jede Abhängigkeit von `security`, und dort liegt der `MandantContext`, den
> `mandantcontext_ist_erster_parameter` für jede öffentliche Methode einer Klasse verlangt, die
> `jooq.glassfish` anfasst. Eine Live-Lesung aus `Message` mit Mandantenkette kann in `common` damit
> **keine** der beiden Regeln einhalten. Der Auftrag sagt für genau diesen Fall: *„Lässt
> `PaketstrukturTest` oder eine ArchUnit-Regel einen Datenbankzugriff in `common` nicht zu: anhalten
> und melden, nicht umgehen."* Drei Wege stehen in §7; die Wahl ist eine des Auftraggebers.

---

## Nummernvergabe

Gesucht mit Python über `docs/**/*.md` und `*.md`, Wortgrenzen, die Zeichenklasse `[‑-]` mit dem
geschützten Bindestrich U+2011; dazu `git grep` über die zwei Zweige, die nicht in `main` sind
(`feat/suchfeld-untermenues`, `test/indexbestand-e37`).

| | |
|---|---|
| **Entscheidungen** | **E‑179 bis E‑182.** Höchste vergebene: **E‑178** ([`rohdaten-backend.md`](rohdaten-backend.md), [`rohdaten.md`](rohdaten.md), Plan). **E‑780** ist der bekannte Falschtreffer aus der Messdatei der Property-Suche. Keiner der beiden Zweige trägt eine Nummer über 178 |
| **Messungen** | **keine vergeben.** Höchste: **M184** ([`neu-laden.md`](neu-laden.md)). Teil 3 des Auftrags misst *am gebauten Code*, und der ist nicht gebaut; **M185** bleibt dafür frei. Die Messstunden sind vorab bestimmt (§8) und tragen keine Nummer |
| **Offene Punkte** | **186 bis 189.** Höchster: **185** ([`neu-laden.md`](neu-laden.md)). Der Treffer `**256**` in [`messungen-schritt10.md`](messungen-schritt10.md) ist eine Zahl in einer Tabellenzelle (Zeile 867) und kein Punkt — gelesen, nicht gezählt |
| **Schritt** | **10e** — `grep -rn "10e" docs/` und über beide Zweige: kein Treffer; der Kasten steht in [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md), Schritt 10 |

---

## 1. Zweck

Der Baum liest die Rollup-Ebene seines Fensters, und das Fenster schließt den angebrochenen Eimer
ein ([`process-view.md`](process-view.md) §3). Der Delta-Lauf rechnet diesen Eimer nur einmal pro
Stunde neu (`0 5 * * * *`, [`rollup.md`](rollup.md) §8). Die Übertragungsliste liest dasselbe Fenster
live aus `Message` (E‑50). **Kurz vor dem nächsten Lauf fehlt im Baum bis zu eine Stunde Verkehr**,
und links und rechts stehen zwei Zahlen für denselben Ausschnitt.

Entschieden vom Auftraggeber am 16.09.2026: **Live-Rest statt kürzerem Takt**; **ein** Baustein für
Prozessbaum und Dashboard; der Prozessbaum zuerst (Teil A), das Dashboard danach (Teil B).

---

## 2. Die Begriffe W und G

| | |
|---|---|
| **W**, der Wasserstand | `fenster_bis` des jüngsten abgeschlossenen und fehlerfreien Laufs ([`rollup.md`](rollup.md) §2). **Datenzeit der Anwendungsuhr**, ein voller Stundenanfang, ausschließend. `gestartet_am` und `beendet_am` derselben Zeile stammen aus der Systemuhr und gehen in keine Rechnung ein (§4 dort) |
| **G** = W − 1 h | der Stundeneimer, in dem dieser Lauf lief. Eimer **vor** G sind mit ihm vollständig gerechnet; der Eimer G nur bis zum Laufzeitpunkt |
| **Live-Bereich** | von G bis zum Anfang der Stunde nach `jetzt`, halboffen. `jetzt` kommt aus der Anwendungsuhr (Regel Z1), **einmal je Anfrage** im Verbraucher geschlagen und hereingereicht |

**Die Stundenarithmetik ist die aus `RollupFenster`:** Anfang der Stunde `truncatedTo(HOURS)`,
Anfang der nächsten `plusHours(1)` — auch dann, wenn `jetzt` genau auf der vollen Stunde liegt.
Gerechnet wird auf `LocalDateTime`, also auf den **Stundenlabels der Wanduhr**, wie
`message_rollup.stunde` sie führt. Am Umstellungstag gibt es ein Label doppelt oder gar nicht, und
die Eimer folgen dem Label, nicht der Sonne.

---

## 3. Die Entscheidung — `common/LiveRest`

Reine Funktion, ohne Datenbank: `LiveRest.entscheide(Optional<LocalDateTime> wasserstand,
LocalDateTime jetzt)` liefert eine `LiveRestEntscheidung` mit Zustand und den Zeitpunkten, die der
Zustand braucht.

| Fall | Zustand | Was die Entscheidung trägt |
|---|---|---|
| kein abgeschlossener fehlerfreier Lauf | **`AUSGESETZT`** | nichts — ohne Zeitangabe |
| G liegt **nach** dem Anfang der aktuellen Stunde | **`NICHT_NOETIG`** | nichts — der Rollup reicht über die Uhr hinaus. Im Profil `dev` nach einem Lauf gegen die Systemuhr der übliche Fall (§9) |
| zwischen G und `jetzt` liegen **mehr als drei Stunden** | **`AUSGESETZT`** | `vollstaendigBis` = G |
| sonst | **`ANGEWANDT`** | `liveVon` = G, `liveBis` = Anfang der Stunde nach `jetzt` |

**Die Grenzen sind so gezogen, dass im Zweifel korrigiert wird** (E‑181): G **gleich** dem Anfang
der aktuellen Stunde heißt *angewandt* (der Eimer ist angebrochen, der Live-Bereich ist genau diese
Stunde); **genau** drei Stunden heißen *noch angewandt* (vier Eimer). Ein zu großer Live-Bereich
kostet eine Stunde Zählung, ein zu kleiner kostet Zahlen.

**Die drei Stunden sind eine benannte Konstante mit Begründung** (`LiveRest.OBERGRENZE_STUNDEN`)
und kein Konfigurationsschlüssel — dieselbe Haltung wie `RollupFenster.NACHLAUF_MINUTEN`: Normal
liegen höchstens rund 65 Minuten zwischen G und `jetzt` (der Lauf um hh:05 rechnet den Eimer hh:00,
der Baum fragt bis hh+1:04), mit einem übersprungenen Lauf gut zwei Stunden. Drei Stunden fangen
einen ausgefallenen Lauf und lassen zwei ausgefallene als das erscheinen, was sie sind: eine Störung,
bei der die Zahlen unvollständig sind und die Oberfläche es sagt. Ein größerer Live-Bereich hieße
außerdem, je Anfrage mehr Stunden live aus `Message` zu zählen.

**W wird abgerundet, nie aufgerundet** (E‑181, zweiter Teil): Per Konstruktion ist W ein
Stundenanfang (`RollupFenster` weist alles andere ab). Wäre er es nicht, ist G = ⌊W⌋ − 1 h und
deckt einen Eimer **mehr** ab — minus und plus über einen ganzen Eimer heben sich auf, wo nichts
fehlt; ein Eimer weniger kostete Zahlen.

**Die Felder hängen am Zustand**, und ein Widerspruch fällt im Konstruktor des Records — nicht erst
in einem Verbraucher, der ein `null` nicht erwartet hat.

---

## 4. Der Wasserstand in `common` — E‑179

**Vorher:** `message/VerengungRepository.wasserstand()`, paketprivat, `null` für „nie gerechnet";
einziger Aufrufer `Fensterverengung`. **Jetzt:** `common/Wasserstand` (Schnittstelle, ein
`Optional<LocalDateTime>`) und `common/WasserstandRepository` als die eine lesende Stelle —
**dasselbe Statement, derselbe Lese-Kontext `glassfishDsl`**:

```sql
SELECT MAX(fenster_bis) FROM overlord_monitor.rollup_lauf
WHERE beendet_am IS NOT NULL AND fehler IS NULL;
```

**Warum eine Schnittstelle:** Die Tests des Live-Rests setzen den Wasserstand und schreiben dabei
**nicht** in `rollup_lauf` der geteilten Testkopie (Vorgabe des Auftrags, Regel T2). Ein Wert, den
ein Test stellt, braucht eine Stelle, an der er gestellt werden kann.

**Warum die Methode öffentlich ist und keinen `MandantContext` nimmt:** `rollup_lauf` trägt keinen
Mandanten; ein ignorierter Kontext wäre der Schein-Kontext, den `PaketstrukturTest` verbietet. Die
Regel M2 bindet Klassen, die `jooq.glassfish` anfassen — `WasserstandRepository` fasst nur
`jooq.monitor` an. In `message` war dieselbe Methode paketprivat, weil `VerengungRepository` das
Quellschema anfasst und die Regel dort jede öffentliche Methode der Klasse bindet
([`dashboard.md`](dashboard.md) §10 zu `letzterLauf()` sagt dasselbe).

**Verhaltensgleich, und das ist gelaufen, nicht angenommen:** `FensterverengungDbIT` **25 Fälle
grün** gegen die Testkopie (verengt und unverengt über alle zehn Mandanten), `FensterverengungMerkmaleTest`
5, `FensterverengungGrenzenTest` 12, `NachrichtenServiceTest` 11 — inhaltlich unverändert.

> **Abweichung vom Auftrag, gemeldet.** Der Auftrag verlangt *„nur Paketname und Importe"*. Das war
> hier nicht möglich: Die Methode stand in einer Klasse mit einer zweiten Aufgabe (der Vorabfrage
> der Verengung), und diese Klasse bleibt in `message`. Der Diff außerhalb der neuen Klassen:
> `Fensterverengung` bekommt einen **dritten Konstruktorparameter** (`Wasserstand`) und liest
> `wasserstand().orElse(null)` statt `repository.wasserstand()`; in drei Tests ändert sich genau
> dieser Konstruktoraufruf (`FensterverengungMerkmaleTest` mit einer zweiten Attrappe,
> `FensterverengungDbIT` mit dem autowirten `WasserstandRepository`, `NachrichtenServiceTest` mit
> `null`). Kein Erwartungswert und keine Zusicherung ist angefasst.

**Der Rollup-Job liest den Wasserstand an eigener Stelle** — `rollup/RollupSchreibRepository.wasserstand()`,
über den **Schreib**-Kontext `monitorDsl`, dieselbe Bedingung. Wie vom Auftrag verlangt: **gemeldet
und nicht nebenbei zusammengelegt** (offener Punkt 187). `dashboard/DashboardRepository.letzterLauf()`
liest dieselbe Bedingung ein drittes Mal für den Stand und bleibt, wo es ist (Vorgabe des Auftrags).

---

## 5. Die Stundenbildung in `common` — E‑180

Der Live-Bereich wird aus `Message` mit **derselben** Stundenbildung gezählt wie im Rollup-Job
([`rollup.md`](rollup.md) §7), sonst liefen Rollup und Live-Zählung an einem Eimerrand auseinander.
Der Ausdruck stand als private Konstante in `rollup/RollupLeseRepository`; ein Fachpaket importiert
nicht aus `rollup`, und nachgebaut werden darf er nicht.

**Gebaut:** `common/Stundeneimer.ausdruck(Field<LocalDateTime>)` liefert
`date_format({0}, '%Y-%m-%d %H:00:00')` als `VARCHAR`, `Stundeneimer.lies(String)` die
Gegenrichtung. **Ohne generierte Typen** — das Feld kommt vom Aufrufer, wie bei
`MessageStatusClassifier.bedingung(Field)`; so bleibt `common` frei von `jooq.glassfish`.

**Im Paket `rollup` ändert sich:** ein Import, die Konstante `STUNDE` ruft `Stundeneimer.ausdruck`,
und die Umwandlung ruft `Stundeneimer.lies`; die eigene Formatkonstante entfällt.
**`RollupStatementsTest` ist zeichengleich grün** (6 Fälle) — der gerenderte Text der Aggregation
ist derselbe.

> **Abweichung vom Auftrag, gemeldet.** *„Im Paket `rollup` ändert sich nur der Import"* — es sind
> der Import und die zwei Aufrufstellen der bisherigen Konstanten; die Konstante selbst konnte nicht
> stehen bleiben, ohne den Ausdruck zweimal zu führen.

---

## 6. Die Verrechnung — `common/LiveRestKorrektur` (E‑182)

Was der Baustein den Verbrauchern zurückgibt, ist gebaut; was er dafür **liest**, nicht (§7).

**Die Korrektur je `(stunde, process_id, message_status)`** ist eine Liste **vorzeichenbehafteter**
`LiveRestZeile`n — minus die Zeilen aus `message_rollup` im Live-Bereich, plus die Zählung aus
`Message` über denselben Bereich —, verrechnet als reine Funktion `LiveRestKorrektur.aus(ausDemRollup,
ausDerQuelle)`. Dazu je Prozess die **jüngste Stunde mit Live-Verkehr**, allein aus der Quelle.

**Warum abziehen statt neu zerlegen:** Tages- und Monatsebene entstehen im selben Lauf und in
derselben Transaktion aus genau diesen Stundeneimern ([`rollup.md`](rollup.md) §5). Die Korrektur
stimmt deshalb für jede Ebene; ein Verbraucher ordnet sie seinen eigenen Eimern zu, und die
gemessenen Kennzahlen-Statements samt E‑93 bleiben unberührt.

| Eigenschaft | Was gilt |
|---|---|
| Statuswechsel innerhalb von G | zwei Zeilen: minus alter, plus neuer Status. Die Nachrichtenzahl je Prozess bleibt gleich, die Fehlerzahl ändert sich, sobald der Verbraucher die Rohwerte über `MessageStatusClassifier` einordnet — **beim Lesen, nicht hier** (E‑g) |
| was sich zu null aufhebt | fällt weg; die jüngste Stunde bleibt trotzdem |
| nur Live-Verkehr, keine Rollupzeile | plus, mit jüngster Stunde — der Fall „Verkehr in der laufenden Stunde" |
| Schreibweisen | gruppiert nach dem Rohwert, Zeichen für Zeichen. Käme derselbe Status aus Rollup und Quelle in zwei Schreibweisen (`utf8mb4_general_ci`), blieben zwei Zeilen; die Summe je Prozess ist davon unberührt |

**So ist es geschnitten, dass Teil B es ohne Änderung nutzt:** Der Baum wird die Zeilen mit seinem
Fenster schneiden (nur Stunden, die im Fenster **und** im Live-Bereich liegen), je Prozess summieren,
bei null klemmen und die letzte Bewegung als Maximum aus E‑34 und der jüngsten Live-Stunde bilden;
das Dashboard ordnet dieselben Zeilen seinen Verlaufseimern zu. **Beides ist nicht gebaut** (§7).

---

## 7. ⚠️ Der Halt — die Regel, der Konflikt, drei Wege

**Die Regel.** `PaketstrukturTest.common_haengt_an_keinem_anderen_anwendungspaket`:

```java
noClasses().that().resideInAPackage("…monitor.common..")
    .should().dependOnClassesThat().resideInAnyPackage(anwendungspaketeAusserCommon())
    .because("common ist das Fundament. Haengt es an einem Fachpaket, ist es keines mehr.");
```

`anwendungspaketeAusserCommon()` sind alle Pakete aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
§6 außer `common` — **einschließlich `security`**, wo der `MandantContext` liegt. Und
`mandantcontext_ist_erster_parameter` verlangt für jede öffentliche Methode einer Klasse, die
`jooq.glassfish` anfasst, `MandantContext` als ersten Parameter; `jooq_glassfish_nur_in_repository_klassen`
verlangt dafür eine Klasse, deren Name auf `Repository` endet.

**Der Konflikt.** Die Live-Lesung zählt `GlassfishDB.Message` im Live-Bereich **mit Mandantenkette
im Statement** (Regel M3, [`process-view.md`](process-view.md) §6). Eine solche Klasse in `common`
muss `MandantContext` importieren (M2) und darf es nicht (Fundament). Was heute in `common` liegt,
ist genau deshalb ohne Mandantendimension gebaut — `Kettenrollen` und `BamSpaltenRegel` sagen es in
ihrem Kopf ausdrücklich, und die Mandantenkette steht heute in **vier** Repositories je einmal
(`NachrichtenRepository`, `VerengungRepository`, `DashboardRepository`, `ProzessbaumRepository`).

**Was ausdrücklich nicht getan wurde:** die Kette als `String`-Parameter (Schein-M2), die Kette als
fertige `Condition` vom Aufrufer (Umgehung der Repository-Regel), die Tabelle über `DSL.name(…)`
statt über generierte Typen (Umgehung des Schreibschutz-Signals am Import), eine vierte namentliche
Ausnahme von M2 (der Auftrag schließt neue benannte Ausnahmen aus). Jedes davon hielte die Regel
dem Buchstaben nach und bräche sie der Sache nach.

**Drei Wege, und die Wahl ist eine des Auftraggebers:**

| Weg | Was sich ändert | Preis |
|---|---|---|
| **(1) `MandantContext` wandert nach `common`** | ein Record ohne Abhängigkeiten; **77** Dateien ändern eine Importzeile (38 in `src/main`, 39 in `src/test`); `PaketstrukturTest` importiert ihn neu; [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6 (`security/ MandantContext`) und [`mandantentrennung.md`](mandantentrennung.md) §1 bekommen einen Kasten | mechanisch groß, fachlich klein. **Keine Regel wird aufgeweicht**, und der eine Baustein bleibt, wie am 16.09.2026 entschieden |
| **(2) `common` darf `security` kennen** | die ArchUnit-Regel und der Satz *„`common` importiert aus keinem anderen Anwendungspaket"* | `security` hängt bereits an `common` (`ZeitConfig`, `common.error`) — es entstünde ein Zyklus zwischen Fundament und Sicherheit. **Nicht empfohlen** |
| **(3) Die zwei Live-Statements je Verbraucher im Fachpaket** | `ProzessbaumRepository` jetzt, `DashboardRepository` in Teil B; `common` behält Entscheidung, Verrechnung, Wasserstand und Stundenbildung | dieselbe Bauform wie die Mandantenkette heute — und derselbe Preis: **zwei Fassungen desselben `Message`-Statements**, die auseinanderlaufen können; [`rollup.md`](rollup.md) §5 verbietet genau das für den Job. Ein Statementstest je Fachpaket könnte beide Texte aneinander pinnen |

**Empfehlung:** Weg (1). Er hält die Entscheidung des Auftraggebers („ein Baustein") und jede Regel;
sein Preis ist ein Import in 77 Dateien. Weg (3) ist der kleinste Diff und passt zur heutigen
Bauform, kauft das aber mit einer zweiten Fassung eines Live-Statements über `Message`.

**Was danach zu bauen ist, unverändert aus dem Auftrag:** `LiveRestRepository` mit den zwei
Lesungen über den Lese-Kontext (ob als ein `UNION ALL` oder zwei Statements, entscheidet der Plan —
§8), ein Dienst, der Wasserstand, Entscheidung, Lesung und Verrechnung zu einem Ergebnis bündelt;
die Verrechnung im `ProzessbaumService` mit `liveRest` in der Antwort; `ProzessbaumStatementsTest`
mit einzeln benannten Statements statt „genau zwei"; `ProzessbaumPlanDbIT` für den Einstieg über
`MessageLastUpdateIDX`; die Isolation am Repository samt Verletzungsprobe; die Summenprobe; M185
am gebauten Endpunkt mit den Schranken 150 ms und 500 ms; dann die Oberfläche.

---

## 8. Die Messstunden — vorab bestimmt, ohne M‑Nummer

Aus `message_rollup` mit Mandantenkette, Stunden **vor dem 01.01.2026** (der dichte Bestand endet am
30.12.2025; die fünf leeren Monate aus [`START-LOKAL.md`](START-LOKAL.md) §3 tragen ohnehin keine
Zeile, und die dünnen Monate 2026‑06/07 liegen hinter dem Anker der Anwendungsuhr). Gelesen am
17.09.2026, lesend, nicht verändert.

**Die typische Stunde** — der Median der Stunden mit Verkehr, nach Nachrichtenzahl geordnet, bei
Gleichstand die jüngste; sie dient als G mit einem Live-Bereich von **zwei** Eimern (`jetzt` = G + 1 h 30):

| Mandant | Stunden mit Verkehr | G (Median) | Nachrichten in G |
|---|---:|---|---:|
| `NEXANS` | 10.919 | `2025-11-09 15:00` | 158 |
| `VOTG` | 10.919 | `2025-06-22 01:00` | 9 |
| `IBIS` | 5.707 | `2025-08-13 14:00` | 9 |
| `SUTTONS` | 7.155 | `2025-06-09 19:00` | 28 |

**Der dichteste zusammenhängende Bereich von vier Stundeneimern** — der größte Live-Bereich, den die
Obergrenze zulässt (`jetzt` = G + 3 h, vier Eimer):

| Mandant | G | Nachrichten je Eimer | Summe |
|---|---|---|---:|
| `NEXANS` | `2024-10-09 18:00` | 3.564 · 4.550 · 7.011 · 3.590 | **18.715** |
| `VOTG` | `2025-06-26 07:00` | 31 · 98 · 19 · 223 | 371 |
| `IBIS` | `2024-11-29 10:00` | 85 · 107 · 91 · 95 | 378 |
| `SUTTONS` | `2025-06-12 07:00` | 237 · 42 · 37 · 36 | 352 |

> **Belegvermerk (Regel L10).** *Gemessen ist:* je Stunde und Mandant `SUM(anzahl)` über
> `message_rollup`, die Mandantenkette als `JOIN` über `Process → ProjectMandant`; Median über
> `ROW_NUMBER()`, der Vier-Stunden-Bereich über drei Selbstverknüpfungen auf `stunde + n h`.
> *Behauptet wird:* Das sind die typische Stunde und der dichteste Vierstundenbereich je Mandant.
> **Die Lücke:** Ein `JOIN` statt `EXISTS` vervielfachte Zeilen, hinge ein Projekt an mehreren
> Mandanten — auf dieser Testkopie hängt keines (M74a: 0), die Zahlen sind hier richtig und für die
> gebaute Fassung trotzdem als `EXISTS` zu lesen. Der Median nimmt bei gerader Zahl die obere Mitte.

**Vorregistriert bleibt, was der Auftrag vorregistriert:** typische Stunde `48H` und `12M` durch den
Endpunkt unter **150 ms** (M152); dichtester Bereich unter **500 ms** (§8 der Projektbeschreibung).
Reißt eine Schranke: anhalten, berichten, nichts nachbessern.

---

## 9. Die Dev-Zeile — lokaler Wasserstand und Zustand

Lesend geprüft am 17.09.2026 auf der Testkopie:

| | |
|---|---|
| **W** | `2026-08-27 15:00:00` — Lauf 407, `VOLL`, gegen die **Systemuhr** gefahren (`gestartet_am 2026-08-27 12:21:13 UTC`), 335.610 Zeilen. Der jüngste Lauf (907, 31.08.2026) endet bei `2025-12-30 05:00`, hebt den Wasserstand aber nicht — `MAX(fenster_bis)` bleibt beim älteren Volllauf |
| **G** | `2026-08-27 14:00` |
| **Anwendungsuhr** | Anker `2025-12-30 04:09:47` plus Laufzeit seit dem Start ([`START-LOKAL.md`](START-LOKAL.md) §1) |
| **Zustand lokal** | **`NICHT_NOETIG`** — G liegt rund acht Monate nach dem Anfang der aktuellen Stunde. Der Rollup deckt den ganzen Bestand (`MAX(stunde) = 2026-07-08 17:00`, 3.341.519 Nachrichten in 335.610 Zeilen, gleich `COUNT(*)` über `Message`) |

Eine Bewegung in der laufenden Stunde ist lokal nicht zu sehen: Der Bestand ändert sich nicht, und
der Rollup-Job läuft im Profil `dev` nicht. Belegt wird der Bau über Tests mit gesetzter Uhr und
gesetztem Wasserstand — genau dafür ist der Wasserstand eine Schnittstelle (§4).

---

## 10. Tests

| Test | Was er hält |
|---|---|
| `LiveRestTest` (20, ohne Datenbank) | **Die Entscheidung (13):** kein Lauf; der Normalfall; G nach dem Anfang der aktuellen Stunde (der Dev-Fall mit August gegen Dezember) und eine Stunde voraus; G gleich dem Anfang der aktuellen Stunde; ein übersprungener Lauf; G genau drei Stunden vor `jetzt` (angewandt) und eine Sekunde darüber (ausgesetzt, vollständig bis G); Sommerzeitbeginn und -ende auf den Labels; W abseits der vollen Stunde abgerundet; `jetzt` auf der vollen Stunde; ein Widerspruch zwischen Zustand und Feldern fällt im Konstruktor. **Die Verrechnung (7):** Statuswechsel innerhalb von G, Aufheben zu null mit bleibender jüngster Stunde, nur Live-Verkehr, jüngste Stunde allein aus der Quelle, Prozess ohne Live-Verkehr, leer, zwei Schreibweisen. Alle Werte erfunden (T2), keine Uhr (T1) |
| `FensterverengungDbIT` (25, `db`) | verengt und unverengt über alle zehn Mandanten — **unverändert grün** mit dem Wasserstand aus `common` |
| `FensterverengungMerkmaleTest` (5), `FensterverengungGrenzenTest` (12), `NachrichtenServiceTest` (11) | inhaltlich unverändert; nur der Konstruktoraufruf hat einen Parameter mehr |
| `RollupStatementsTest` (6) | **zeichengleich grün** — die Aggregation rendert mit `Stundeneimer` denselben Text |
| `PaketstrukturTest` (19) | grün — `common` hängt weiterhin an keinem Anwendungspaket, und `WasserstandRepository` steht zu Recht ohne `MandantContext` |

Zusammen 165 Fälle ohne Datenbank in einem Lauf (`-DexcludedGroups=db`, mit `ProzessbaumStatementsTest`,
`DashboardStatementsTest` und `RollupFensterTest`), dazu die 25 Fälle der `FensterverengungDbIT`
gegen die Testkopie — aus den Surefire- und Failsafe-Ausgaben, nicht abgezählt.

---

## 11. Regelbezug

| Regel | Stand |
|---|---|
| **L2** Keine Live-Aggregation über `Message` | **unberührt** — nichts liest live. Die dritte benannte Ausnahme wird erst mit der Live-Lesung eingetragen; bis dahin bleibt [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8 Regel 2 bei zwei |
| **L7** Jede neue Abfrage gemessen | **gegenstandslos** — keine neue Abfrage; `WasserstandRepository` rendert das bisherige Statement, `RollupLeseRepository` denselben Text |
| **M2** `MandantContext` erster Pflichtparameter | **erfüllt**, und die Regel ist der Grund des Halts (§7). `WasserstandRepository.wasserstand()` ist öffentlich ohne Kontext, weil die Klasse `jooq.glassfish` nicht anfasst — `PaketstrukturTest` grün |
| **M3** Filter im Statement | **noch nicht anwendbar** — die Live-Lesung ist nicht gebaut |
| **M4** Isolationstest je Endpunkt | **kein neuer Endpunkt**, kein neuer Parameter |
| **S1** Kein Schreibzugriff auf `GlassfishDB` | **erfüllt** — nur Lesen, und die Tests schreiben auch nicht in `rollup_lauf` (Schnittstelle) |
| **T1, T2** | **erfüllt** — keine Wanduhrzeit, kein Bestandswert in einer Zusicherung; die Zahlen in §8 und §9 sind ausgegeben, nicht behauptet |
| **Z1** Kein `now()` | **erfüllt** — `jetzt` ist ein Parameter der Entscheidung; keine Uhr in `common/LiveRest` |
| **§6 der Projektbeschreibung** Fachpakete kennen einander nicht | **erfüllt und der Anlass**: Wasserstand und Stundenbildung sind wegen des zweiten Verbrauchers nach `common` gewandert; die Live-Lesung kann es nicht (§7) |

---

## 12. Offene Punkte

| | |
|---|---|
| **186** | ⚠️ **Wo die mandantengefilterte Live-Lesung liegt, entscheidet der Auftraggeber** (§7). Bis dahin sind Teil 1.3, Teil 2, Teil 3, Teil 4 und die Datenbanktests aus Teil 5 nicht gebaut; die Empfehlung ist Weg (1) |
| **187** | **`rollup_lauf` wird an drei Stellen gelesen:** `common/WasserstandRepository` (Lese-Kontext, Verengung und künftig Live-Rest), `rollup/RollupSchreibRepository.wasserstand()` (Schreib-Kontext, der Job selbst) und `dashboard/DashboardRepository.letzterLauf()` (der Stand). Dieselbe Bedingung dreimal; der Auftrag hat beide anderen Stellen ausdrücklich stehen lassen. Zusammenlegen ist eine eigene Entscheidung |
| **188** | **Was passiert, wenn die Live-Lesung ausfällt** — etwa in die Zeitgrenze des Lese-Pools läuft (`max_statement_time = 10`). Die Fensterverengung fällt in diesem Fall auf die unverengte Liste zurück; ob der Live-Rest dann `AUSGESETZT` melden soll oder die Antwort scheitern darf, ist beim Bau der Lesung zu entscheiden und nicht hier |
| **189** | **Nachladen im Takt der Übertragungsliste** (Teil 4 des Auftrags): Der Baum soll nachladen, *solange die automatische Aktualisierung der Liste eingeschaltet ist*. Seit E‑164 ([`neu-laden.md`](neu-laden.md)) hat die Prozessansicht **keine** automatische Aktualisierung mehr — der Schalter existiert dort nicht, `useNachrichtenSeite` läuft mit `false`. Gebaut würde damit ein Takt, der in dieser Ansicht nie eintritt. Vor Teil 4 zu klären |

---

## 13. Was nicht gebaut ist

Alles, was an der Live-Lesung hängt (§7), und ausdrücklich außerdem: nichts am Dashboard
(`DashboardRepository.letzterLauf()` bleibt); kein kürzerer Takt, keine Änderung an
`overlord.rollup.*`, an `rollup_lauf` oder am Rollup-Job — im Paket `rollup` ändert sich allein die
Herkunft der Stundenbildung; keine Änderung an den Kennzahlen-Statements, am Gerüst oder an E‑34;
kein Live-Rest in Nachrichtenliste, BAM-Suche oder Property-Suche; keine Korrektur älterer Eimer;
keine Konfiguration für die Obergrenze; kein neuer Endpunkt, keine neue benannte Ausnahme; keine
Messung.
