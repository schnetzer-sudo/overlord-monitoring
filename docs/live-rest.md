# Live-Rest der laufenden Stunde — Teil A: der Baustein und der Prozessbaum

*17.09.2026.* Auftrag „Live-Rest der laufenden Stunde, Teil A (Baustein und Prozessbaum)", Stand
17.09.2026. Entscheidungen **E‑179** bis **E‑188**, Messung **M185**, offene Punkte **186** bis
**192** (186, 188 und 189 am selben Tag entschieden). Schritt **10e** im
[Implementierungsplan](IMPLEMENTIERUNGSPLAN_MVP.md).

**Die Frage dieser Datei:** Wie bekommt der Prozessbaum den Verkehr seit dem letzten Rollup-Lauf
dazugerechnet, damit Baum und Übertragungsliste für dasselbe Fenster dieselbe Zahl zeigen — über
**einen** Baustein, den in Teil B auch das Dashboard ruft?

> ### Der Ertrag in einem Absatz
>
> Der Baustein liegt in `common`: die **Entscheidung** (`ANGEWANDT`, `NICHT_NOETIG`, `AUSGESETZT`;
> G = W − 1 h; Obergrenze drei Stunden als benannte Konstante), die **zwei Live-Lesungen** mit
> Mandantenkette (die Rollupzeilen des Live-Bereichs und die Zählung aus `Message` über denselben
> Bereich, dieselbe Stundenbildung wie der Rollup-Job), die **Verrechnung** je
> `(stunde, process_id, message_status)` und der **Wasserstand** als Schnittstelle. Der Prozessbaum
> verrechnet davon, was in seinem Fenster liegt, nimmt die jüngste Live-Stunde in die letzte Bewegung
> und nennt in der Antwort den Block `liveRest`. **Das Tor hält:** durch den Endpunkt höchstens
> 141,6 ms in der typischen Stunde (Schranke 150) und 337,0 ms im dichtesten Vierstundenbereich
> (Schranke 500), beide bei `NEXANS` (M185). Die Summenprobe trifft `COUNT(*)` aus `Message` je
> Schlüssel, die Isolation ist am Repository geprüft und die Verletzungsprobe je Kette rot. Die
> Oberfläche zeigt bei `AUSGESETZT` einen Hinweis bei den Zahlen — und sonst nichts.
>
> **Drei Dinge waren nicht selbstverständlich:** `MandantContext` musste nach `common` wandern,
> weil die Regeln eine mandantengefilterte Lesung dort sonst nicht zuließen (§7, Weg 1 —
> Entscheidung des Auftraggebers); die Zusicherung „genau zwei Statements je Aufruf und kein
> `Message`" ist bewusst gefallen; und das Nachladen im Takt der Liste ist **nicht** gebaut, weil
> die Prozessansicht seit E‑164 keinen Takt mehr hat (Punkt 189, entschieden).

---

## Nummernvergabe

Gesucht mit Python über `docs/**/*.md` und `*.md`, Wortgrenzen, die Zeichenklasse `[‑-]` mit dem
geschützten Bindestrich U+2011; dazu `git grep` über die zwei Zweige, die nicht in `main` sind
(`feat/suchfeld-untermenues`, `test/indexbestand-e37`).

| | |
|---|---|
| **Entscheidungen** | **E‑179 bis E‑188.** Höchste vergebene: **E‑178** ([`rohdaten-backend.md`](rohdaten-backend.md), [`rohdaten.md`](rohdaten.md), Plan). **E‑780** ist der bekannte Falschtreffer aus der Messdatei der Property-Suche. Keiner der beiden Zweige trägt eine Nummer über 178 |
| **Messung** | **M185.** Höchste: **M184** ([`neu-laden.md`](neu-laden.md)); **M179** ist Fließtext in [`messungen-property-suche.md`](messungen-property-suche.md). Kein Zweig vergibt eine Nummer über 184 |
| **Offene Punkte** | **186 bis 192.** Höchster: **185** ([`neu-laden.md`](neu-laden.md)). Der Treffer `**256**` in [`messungen-schritt10.md`](messungen-schritt10.md) ist eine Zahl in einer Tabellenzelle (Zeile 867) und kein Punkt — gelesen, nicht gezählt |
| **Schritt** | **10e** — `grep -rn "10e" docs/` und über beide Zweige: kein Treffer vor diesem Tag |

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
| **Live-Bereich** | von G bis zum Anfang der Stunde nach `jetzt`, halboffen. `jetzt` kommt aus der Anwendungsuhr (Regel Z1), **einmal je Anfrage** im Verbraucher geschlagen und hereingereicht — derselbe Schlag, der das Fenster des Baums bildet |

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
| G liegt **nach** dem Anfang der aktuellen Stunde | **`NICHT_NOETIG`** | nichts — der Rollup reicht über die Uhr hinaus. Im Profil `dev` nach einem Lauf gegen die Systemuhr der übliche Fall (§10) |
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
außerdem, je Anfrage mehr Stunden live aus `Message` zu zählen — und was das kostet, steht in §8.

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
ein Test stellt, braucht eine Stelle, an der er gestellt werden kann — die Summenprobe stellt ihn
über eine Lambda, M185 über `@TestBean` (§8).

**Warum die Methode öffentlich ist und keinen `MandantContext` nimmt:** `rollup_lauf` trägt keinen
Mandanten; ein ignorierter Kontext wäre der Schein-Kontext, den `PaketstrukturTest` verbietet. Die
Regel M2 bindet Klassen, die `jooq.glassfish` anfassen — `WasserstandRepository` fasst nur
`jooq.monitor` an ([`dashboard.md`](dashboard.md) §10 sagt zu `letzterLauf()` dasselbe).

**Verhaltensgleich, und das ist gelaufen, nicht angenommen:** `FensterverengungDbIT` **25 Fälle
grün** gegen die Testkopie, `FensterverengungMerkmaleTest` 5, `FensterverengungGrenzenTest` 12,
`NachrichtenServiceTest` 11 — inhaltlich unverändert.

> **Abweichung vom Auftrag, gemeldet.** Der Auftrag verlangt *„nur Paketname und Importe"*. Das war
> nicht möglich: Die Methode stand in einer Klasse mit einer zweiten Aufgabe (der Vorabfrage der
> Verengung), und die bleibt in `message`. Der Diff außerhalb der neuen Klassen: `Fensterverengung`
> bekommt einen **dritten Konstruktorparameter** (`Wasserstand`) und liest `wasserstand().orElse(null)`;
> in drei Tests ändert sich genau dieser Konstruktoraufruf. Kein Erwartungswert und keine Zusicherung
> ist angefasst.

**Der Rollup-Job liest den Wasserstand an eigener Stelle** — `rollup/RollupSchreibRepository.wasserstand()`,
über den **Schreib**-Kontext `monitorDsl`, dieselbe Bedingung; und `dashboard/DashboardRepository.letzterLauf()`
liest sie ein drittes Mal für den Stand. **Gemeldet und so gelassen** — vom Auftraggeber am
17.09.2026 so entschieden (Punkt 187).

---

## 5. Die Stundenbildung in `common` — E‑180

Der Live-Bereich wird aus `Message` mit **derselben** Stundenbildung gezählt wie im Rollup-Job
([`rollup.md`](rollup.md) §7), sonst liefen Rollup und Live-Zählung an einem Eimerrand auseinander.
Der Ausdruck stand als private Konstante in `rollup/RollupLeseRepository`; ein Fachpaket importiert
nicht aus `rollup`, und nachgebaut werden darf er nicht.

**Gebaut:** `common/Stundeneimer.ausdruck(Field<LocalDateTime>)` liefert
`date_format({0}, '%Y-%m-%d %H:00:00')` als `VARCHAR`, `Stundeneimer.lies(String)` die
Gegenrichtung. **Ohne generierte Typen** — das Feld kommt vom Aufrufer, wie bei
`MessageStatusClassifier.bedingung(Field)`.

**Im Paket `rollup` ändern sich** ein Import und die zwei Aufrufstellen der bisherigen Konstanten.
**`RollupStatementsTest` ist zeichengleich grün** (6 Fälle) — der gerenderte Text der Aggregation
ist derselbe.

---

## 6. Die Lesungen, die Verrechnung, der Dienst

### `common/LiveRestRepository` — zwei Statements, beide mit Mandantenkette (E‑184)

| | Statement | liest |
|---|---|---|
| **A** `ausDemRollup(mandant, von, bis)` | die Zeilen von `message_rollup` im Live-Bereich, je `(stunde, process_id, message_status)`, mit `EXISTS`-Kette — ohne Summe, ohne Sortierung | was der Rollup über den Bereich **schon weiß** |
| **B** `ausDerQuelle(mandant, von, bis)` | dieselbe Aggregation wie der Rollup-Job über `Message` im Live-Bereich, `DATE_FORMAT`-Stundeneimer, `GROUP BY` über den vollen Ausdruck, mit `EXISTS`-Kette | was **wirklich** im Bereich steht |

Beide Texte stehen **wörtlich** in `ProzessbaumStatementsTest.EinAufruf.live_teil_woertlich`; die
Messung in §8 misst genau diese Texte. Die Mandantenkette ist in beiden ein `EXISTS` und kein Join:
`ProjectMandant` ist n:m, ein Join vervielfachte Zeilen und damit die Zahl ([`process-view.md`](process-view.md)
§6). Um `MessageLastUpdate` steht keine Funktion.

**Zwei Statements und keine Vereinigung — am Plan entschieden (E‑184).** Die `UNION ALL`-Fassung
ist gemessen (§8, Fassung C): Der Optimierer materialisiert die Ableitung (`<derived2>`, `ALL` über
37.483 Zeilen bei `NEXANS`), gruppiert noch einmal darüber und ist in sieben von acht Fällen
langsamer als A + B zusammen — bei den kleinen Mandanten um 1 bis 4 ms, beim dichten `NEXANS`
innerhalb des Rauschens (261,5 gegen 266,8 ms). Getrennt hat jedes Statement seinen eigenen Plan,
jeder ist einzeln benannt und einzeln geprüft, und die Verrechnung in Java kostet bei höchstens 61
Zeilen nichts.

### `common/LiveRestKorrektur` — die Verrechnung (E‑182)

Reine Funktion `LiveRestKorrektur.aus(ausDemRollup, ausDerQuelle)`: Rollupzeilen mit **negativem**
Vorzeichen, Quellzeilen mit positivem, gleiche Schlüssel summiert, Nullzeilen weggelassen. Dazu je
Prozess die **jüngste Stunde mit Live-Verkehr**, allein aus der Quelle — sie steht auch dann da, wenn
sich die Zeilen zu null aufheben.

**Warum abziehen statt neu zerlegen:** Tages- und Monatsebene entstehen im selben Lauf und in
derselben Transaktion aus genau diesen Stundeneimern ([`rollup.md`](rollup.md) §5). Die Korrektur
stimmt deshalb für jede Ebene; ein Verbraucher ordnet sie seinen eigenen Eimern zu, und die
gemessenen Kennzahlen-Statements samt E‑93 bleiben unberührt.

| Eigenschaft | Was gilt |
|---|---|
| Statuswechsel innerhalb von G | zwei Zeilen: minus alter, plus neuer Status. Die Nachrichtenzahl je Prozess bleibt gleich, die Fehlerzahl ändert sich, sobald der Verbraucher die Rohwerte über `MessageStatusClassifier` einordnet — **beim Lesen** (E‑g) |
| was sich zu null aufhebt | fällt weg; die jüngste Stunde bleibt |
| nur Live-Verkehr, keine Rollupzeile | plus, mit jüngster Stunde — der Fall „Verkehr in der laufenden Stunde" |
| Schreibweisen | gruppiert nach dem Rohwert, Zeichen für Zeichen; zwei Schreibweisen desselben Status (`utf8mb4_general_ci`) blieben zwei Zeilen, die Summe je Prozess ist davon unberührt |

### `common/LiveRestService` — der Baustein (E‑185)

`ermittle(mandant, jetzt)`: Wasserstand lesen, entscheiden, bei `ANGEWANDT` A und B lesen und
verrechnen; zurück kommt ein `LiveRestErgebnis` aus Entscheidung und Korrektur. **Fällt eine der
beiden Live-Lesungen aus** — etwa an der Zeitgrenze des Lese-Pools —, antwortet der Verbraucher mit
den Rollup-Zahlen, der Zustand ist `AUSGESETZT` mit `vollstaendigBis` = G, und der Fehler steht als
`WARN` im Protokoll. Entschieden vom Auftraggeber am 17.09.2026 (Punkt 188): Rückfall statt Ausfall,
aber sichtbar. **Die Wasserstandsabfrage selbst wird nicht abgefangen** — sie liest das eigene Schema;
fällt sie, ist das kein fehlender Rest, sondern ein Vorfall (Punkt 192).

---

## 7. Der Regelkonflikt und Weg 1 — E‑183

**Die Regel.** `PaketstrukturTest.common_haengt_an_keinem_anderen_anwendungspaket` verbietet
`common` jede Abhängigkeit von einem anderen Anwendungspaket — **einschließlich `security`**, wo
der `MandantContext` lag. `mandantcontext_ist_erster_parameter` verlangt für jede öffentliche Methode
einer Klasse, die `jooq.glassfish` anfasst, `MandantContext` als ersten Parameter. Eine Live-Lesung
aus `Message` mit Mandantenkette in `common` konnte damit keine der beiden Regeln halten. Der Auftrag
sagt für diesen Fall *anhalten und melden, nicht umgehen* — und so ist es am Vormittag des 17.09.2026
gelaufen: gebaut waren Entscheidung, Verrechnung, Wasserstand und Stundenbildung; die Lesung nicht.

**Drei Wege standen zur Wahl** — `MandantContext` nach `common`; `common` darf `security` kennen
(Zyklus, weil `security` schon an `common` hängt); die zwei Live-Statements je Verbraucher im
Fachpaket (zwei Fassungen desselben `Message`-Statements, die auseinanderlaufen können).
**Entschieden vom Auftraggeber am 17.09.2026: Weg 1** (Punkt 186, E‑183). Der Record hängt an
nichts; **77 Dateien** ändern eine Importzeile (38 in `src/main`, 39 in `src/test`), sechs Klassen in
`security` bekommen den Import, den sie im eigenen Paket nicht brauchten. Alles, was den Kontext
**herstellt** — `MandantContextProvider`, `MandantService`, `@OhneMandantenkontext` —, bleibt in
`security`. Keine Regel wird aufgeweicht; `PaketstrukturTest` ist unverändert grün, und die drei
namentlichen Ausnahmen von M2 sind weiter drei. Datierte Kästen in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6, [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md)
§4.1 und [`mandantentrennung.md`](mandantentrennung.md) §1.

**Was ausdrücklich nicht getan wurde:** die Kette als `String`-Parameter (Schein-M2), die Kette als
fertige `Condition` vom Aufrufer (Umgehung der Repository-Regel), die Tabelle über `DSL.name(…)`
statt über generierte Typen (Umgehung des Schreibschutz-Signals am Import), eine vierte namentliche
Ausnahme von M2.

---

## 8. Die Messung — M185 (Regel L7)

### Die Messstunden, vorab bestimmt

Aus `message_rollup` mit Mandantenkette, Stunden **vor dem 01.01.2026** (der dichte Bestand endet am
30.12.2025; die fünf leeren Monate aus [`START-LOKAL.md`](START-LOKAL.md) §3 tragen keine Zeile).
Gelesen am 17.09.2026, lesend, nicht verändert.

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
> Mandanten — auf dieser Testkopie hängt keines (M74a: 0). Der Median nimmt bei gerader Zahl die
> obere Mitte.

### Am gebauten Endpunkt — `MessungM185DbIT`, beste von fünf, in Millisekunden

Dieselbe Form wie M152: je Fall ein Aufwärmlauf und fünf Läufe, **durch den Endpunkt** (HTTP‑Umlauf
im Testclient samt Sitzung und Serialisierung) und **am Dienst** (`ProzessbaumService.baum` im selben
Prozess: Gerüst, Kennzahlen, Wasserstand, die zwei Live-Lesungen, Verrechnung, Zusammensetzen).
**Uhr und Wasserstand sind im Test gesetzt** — über `@TestBean` an `devClock` und `Wasserstand`,
kein Schreibzugriff auf `rollup_lauf`. Die Zeilen stehen unverändert in
`scripts/messung-live-rest/ergebnis/m185-endpunkt.txt`.

| Mandant · Fall | Live-Eimer | Zeilen A / B | `48H` Endpunkt | `48H` Dienst | `12M` Endpunkt | `12M` Dienst |
|---|---:|---:|---:|---:|---:|---:|
| `NEXANS` typisch | 2 | 3 / 3 | 83,862 | 45,249 | **141,574** | 109,857 |
| `NEXANS` dicht | 4 | 37 / 37 | **336,990** | 312,571 | 324,843 | 296,689 |
| `VOTG` typisch | 2 | 12 / 12 | 55,039 | 32,532 | 76,743 | 54,498 |
| `VOTG` dicht | 4 | 42 / 42 | 69,799 | 50,134 | 88,319 | 69,892 |
| `IBIS` typisch | 2 | 12 / 12 | 50,084 | 30,057 | 81,604 | 62,408 |
| `IBIS` dicht | 4 | 61 / 61 | 61,397 | 41,869 | 61,136 | 38,916 |
| `SUTTONS` typisch | 2 | 14 / 14 | 42,156 | 24,931 | 62,830 | 46,435 |
| `SUTTONS` dicht | 4 | 51 / 51 | 69,116 | 53,114 | 75,473 | 59,590 |

**Vorregistriert war:** typische Stunde unter **150 ms** durch den Endpunkt (die Schranke aus M152),
dichtester Bereich unter **500 ms** (das Budget aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
§8). **Beide halten:** 141,574 ms (`NEXANS`, `12M`, typisch) und 336,990 ms (`NEXANS`, `48H`,
dicht). **Das Tor ist bestanden.**

Drei Beobachtungen, keine Zusicherungen:

- **Die typische Stunde kostet gegenüber M152 rund 16 bis 20 ms** — `NEXANS` `48H` 83,9 gegen
  65,9 ms, `12M` 141,6 gegen 125,6 ms: das ist die Wasserstandsabfrage, die zwei Live-Lesungen über
  drei Zeilen und die Verrechnung. Der Rumpf ist um den Block `liveRest` und die größere Kopfzahl
  gewachsen (203.309 gegen 153.885 Byte bei `48H`, weil das gesetzte Fenster mehr Verkehr trägt).
- **Der dichteste Bereich bei `NEXANS` ist der Preis der Ausnahme:** 18.715 Nachrichten in vier
  Eimern kosten am Dienst 312,6 ms, davon 265,7 ms das eine Statement B (unten). Das ist das
  2,7‑Fache der typischen Stunde und liegt unter dem Budget — auf der Testkopie. Ob die Produktion
  dichtere Stunden hat, ist nicht gemessen (Punkt 190).
- **Bei den drei kleineren Mandanten ändert der Live-Bereich fast nichts** — 42 bis 88 ms in beiden
  Fällen; ihre Vierstundenbereiche tragen 352 bis 378 Nachrichten.

### Auf SQL-Ebene — Profil und Wanduhr, `EXPLAIN` je Statement (Regel L15)

`scripts/messung-live-rest/erzeuge.py` erzeugt `s1-m185-live-teil.sql` aus dem gepinnten Text der
Statements (nur die drei Bindewerte eingesetzt); `ergebnis/s1.txt` ist das Protokoll,
`s1.gefiltert.txt` die Marken, Pläne und Auswertungen. Je Statement ein Aufwärmlauf und fünf
Läufe, die Laufzeit aus `information_schema.PROFILING` **und** an der Wanduhr des Servers
(`SYSDATE(6)` vor und nach dem Statement, E‑107); Eichung der Wanduhr mit `SELECT 1`: 0,708 bis
0,893 ms. Sitzung vom 17.09.2026 13:32 Serverzeit, MariaDB 10.6.22, `read_only = 1`. **Kein Plan
zeigt `LATERAL DERIVED` oder `DEPENDENT SUBQUERY`; Profil und Wanduhr stimmen überein.**

Beste von fünf, in Millisekunden — **A** Rollupzeilen des Live-Bereichs, **B** Zählung aus `Message`,
**C** die nicht gebaute `UNION ALL`-Fassung (A und B als Zweige, Kette in beiden, darüber eine Summe):

| Fall | A Profil | A Wand | B Profil | B Wand | A + B Profil | C Profil | C Wand |
|---|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` typisch | 0,737 | 1,403 | 3,213 | 3,920 | 3,950 | 4,060 | 4,878 |
| `NEXANS` dicht | 1,076 | 1,795 | **265,684** | 266,955 | 266,760 | 261,470 | 262,873 |
| `VOTG` typisch | 0,870 | 1,736 | 4,730 | 5,700 | 5,600 | 5,829 | 6,854 |
| `VOTG` dicht | 2,120 | 3,079 | 19,277 | 20,825 | 21,397 | 22,479 | 24,014 |
| `IBIS` typisch | 1,223 | 1,946 | 4,257 | 5,088 | 5,480 | 5,700 | 6,576 |
| `IBIS` dicht | 1,718 | 2,757 | 17,314 | 18,687 | 19,032 | 19,343 | 20,652 |
| `SUTTONS` typisch | 0,859 | 1,586 | 10,847 | 12,110 | 11,706 | 11,113 | 12,411 |
| `SUTTONS` dicht | 2,073 | 3,036 | 20,574 | 22,002 | 22,647 | 24,471 | 25,929 |

**Die Pläne**, `NEXANS` dicht — A:

```
+------+-------------+----------------+--------+------------------------------------+---------+---------+--------------------------------------------+------+--------------------------+
| id   | select_type | table          | type   | possible_keys                      | key     | key_len | ref                                        | rows | Extra                    |
+------+-------------+----------------+--------+------------------------------------+---------+---------+--------------------------------------------+------+--------------------------+
|    1 | PRIMARY     | message_rollup | range  | PRIMARY,message_rollup_prozess_idx | PRIMARY | 5       | NULL                                       | 71   | Using where              |
|    1 | PRIMARY     | live_process   | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.message_rollup.process_id | 1    | Using where              |
|    1 | PRIMARY     | ProjectMandant | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.live_process.ProjectID,const   | 1    | Using where; Using index |
+------+-------------+----------------+--------+------------------------------------+---------+---------+--------------------------------------------+------+--------------------------+
```

B — **der Einstieg ist der Zeitindex, wie beim Rollup-Job (M88)**, 37.412 geschätzte Zeilen für
18.715 Nachrichten in vier Eimern:

```
+------+-------------+----------------+--------+----------------------------------------------------------------------------------------+----------------------+---------+------------------------------------------+-------+---------------------------------------------------------------------+
| id   | select_type | table          | type   | possible_keys                                                                          | key                  | key_len | ref                                      | rows  | Extra                                                               |
+------+-------------+----------------+--------+----------------------------------------------------------------------------------------+----------------------+---------+------------------------------------------+-------+---------------------------------------------------------------------+
|    1 | PRIMARY     | Message        | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                     | 37412 | Using index condition; Using where; Using temporary; Using filesort |
|    1 | PRIMARY     | live_process   | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.Message.ProcessID            | 1     | Using where                                                         |
|    1 | PRIMARY     | ProjectMandant | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.live_process.ProjectID,const | 1     | Using where; Using index                                            |
+------+-------------+----------------+--------+----------------------------------------------------------------------------------------+----------------------+---------+------------------------------------------+-------+---------------------------------------------------------------------+
```

C — die Vereinigung materialisiert und liest die Ableitung voll:

```
+------+-------------+----------------+--------+---------------+----------------------+---------+--------------------------------------------+-------+---------------------------------------------------------------------+
| id   | select_type | table          | type   | possible_keys | key                  | key_len | ref                                        | rows  | Extra                                                               |
+------+-------------+----------------+--------+---------------+----------------------+---------+--------------------------------------------+-------+---------------------------------------------------------------------+
|    1 | PRIMARY     | <derived2>     | ALL    | NULL          | NULL                 | NULL    | NULL                                       | 37483 | Using temporary; Using filesort                                     |
|    2 | DERIVED     | message_rollup | range  | …             | PRIMARY              | 5       | NULL                                       | 71    | Using where                                                         |
|    2 | DERIVED     | live_process   | eq_ref | …             | PRIMARY              | 146     | overlord_monitor.message_rollup.process_id | 1     | Using where                                                         |
|    2 | DERIVED     | ProjectMandant | eq_ref | …             | PRIMARY              | 292     | GlassfishDB.live_process.ProjectID,const   | 1     | Using where; Using index                                            |
|    4 | UNION       | Message        | range  | …             | MessageLastUpdateIDX | 5       | NULL                                       | 37412 | Using index condition; Using where; Using temporary; Using filesort |
|    4 | UNION       | live_process   | eq_ref | …             | PRIMARY              | 146     | GlassfishDB.Message.ProcessID              | 1     | Using where                                                         |
|    4 | UNION       | ProjectMandant | eq_ref | …             | PRIMARY              | 292     | GlassfishDB.live_process.ProjectID,const   | 1     | Using where; Using index                                            |
+------+-------------+----------------+--------+---------------+----------------------+---------+--------------------------------------------+-------+---------------------------------------------------------------------+
```

**Zwei Planfamilien für B, und beides sind Indexzugriffe** — derselbe Befund wie bei der
Fensterverengung ([`nachrichtenliste.md`](nachrichtenliste.md) §5a): Bei `NEXANS`, `VOTG` und `IBIS`
treibt `Message` über den Zeitindex und die Kette folgt per `eq_ref`; bei `VOTG` dicht nimmt der
Optimierer den zusammengesetzten `MessageLastUpdateProcessMessageIDX` (1.937 geschätzte Zeilen). Bei
`SUTTONS` (typisch, 1.238 Zeilen) beginnt der Plan bei `ProjectMandant` über `ProjectMandant_Mandant_idx`
und liest `Message` als `range` über `MessageLastUpdateIDX` mit Join-Puffer — **auch dort steigt
`Message` über einen Index auf `MessageLastUpdate` ein**, und keine Tabelle wird voll gelesen.
`ProzessbaumPlanDbIT` hält genau das fest (Treiberindex aus der Menge der beiden Zeitindizes, Zugriff
`range`, keine Zeile `ALL`) und **nicht** die Reihenfolge der Tabellen.

> **Belegvermerk (Regel L10).** *Gemessen ist:* acht Fälle (vier Mandanten × typische Stunde /
> dichtester Bereich) × drei Statementformen, warm, ein Aufwärmlauf und beste von fünf, Profil und
> Wanduhr; dazu sechzehn Fälle durch den Endpunkt im Testclient auf demselben Rechner wie der Server.
> *Behauptet wird:* Der Live-Rest kostet auf der Testkopie durch den gebauten Endpunkt höchstens
> 141,6 ms in der typischen Stunde und 337,0 ms im dichtesten Vierstundenbereich, und zwei Statements
> sind der Vereinigung mindestens ebenbürtig. **Die Lücke:** Die dichteste Stunde der *Produktion* ist
> unbekannt; die Testkopie ist eine Vollkopie mit Datenstand 08.07.2026, und 18.715 Nachrichten in vier
> Stunden sind ihr Höchstwert, nicht der des Betriebs. Kein Fall ist kalt gemessen (dieselbe
> Einschränkung wie in M152).

---

## 9. Der Prozessbaum (E‑188)

`ProzessbaumService.baum` ruft den Baustein mit **demselben `jetzt`**, mit dem er sein Fenster bildet
(ein Uhrenschlag je Anfrage), und verrechnet:

- **Nachrichten und Fehler:** nur die Korrekturzeilen, deren Stunde im Fenster liegt (`von`
  einschließend, `bis` ausschließend — wie die Eimer), mit ihrem Vorzeichen durch denselben Weg wie
  die Rollupzeilen: Was Fehler ist, entscheidet `MessageStatusClassifier` beim Lesen. **Kein
  negativer Endwert** — beide Zahlen werden je Prozess einzeln auf null geklemmt. Ein Fenster, das
  vor G endet, bekommt keine Korrektur.
- **Letzte Bewegung:** das Maximum aus dem Wert nach E‑34 und der jüngsten Live-Stunde des
  Prozesses, **unabhängig vom Fenster** (E‑35). Zustand und die drei Zähler folgen daraus; ein
  Prozess mit Verkehr in der laufenden Stunde steht nie als „still" oder „nie" da.
- **Antwort:** der Block `liveRest` — `zustand` und `vollstaendigBis` (G in UTC, nur bei
  `AUSGESETZT` mit Lauf, sonst `null`), nach der gelebten Konvention der Nachbarn benannt
  ([`process-view.md`](process-view.md) §1). Kein neuer Endpunkt, kein neuer Parameter.
- **Das freie Fenster** bekommt dieselbe Verrechnung.

**Die Statements eines Aufrufs** sind seither **fünf** bei `ANGEWANDT` und **drei** sonst — Gerüst,
Kennzahlen, Wasserstand, A, B — und stehen einzeln benannt in `ProzessbaumStatementsTest.EinAufruf`;
die Zusicherung „genau zwei Statements je Aufruf und kein `Message`" ist bewusst gefallen. E‑42
bleibt für den Rollup-Teil bestehen: zwei Statements, unverändert und weiter wörtlich gepinnt. Die
Kästen dazu stehen in [`process-view.md`](process-view.md) §3, §4, §6, §11 und §49.

---

## 9b. Das Dashboard — Teil B (E‑190, E‑191) *(17.09.2026)*

**Der Auftrag aus Teil A, eingelöst:** Das Dashboard ruft **denselben Baustein** — `DashboardService.landingpage`
ruft `LiveRestService.ermittle(mandant, jetzt)` mit dem `jetzt`, aus dem es sein Fenster bildet
(ein Uhrenschlag je Anfrage) — und ordnet die Korrektur seinen eigenen Eimern zu. Drei
Entscheidungen des Auftraggebers vom 17.09.2026 (per Auswahl, wie die Punkte 186 bis 189):

| Frage | Entschieden |
|---|---|
| Trägt Block 5 (Verteilung) die Korrektur ebenfalls? | **Ja, über eine Katalog-Nachlesung** (E‑191) |
| Wo steht der Hinweis bei `AUSGESETZT`? | **Über den Kacheln**, als gemeinsamer Baustein beider Ansichten (E‑192) |
| Welches Tor gilt für M186? | **500 ms je Lage durch den Endpunkt, in beiden Messstunden** — das Seitenbudget aus M108/M145; liegt eine Lage darüber: anhalten und berichten, nicht nachjustieren |

### Die Zuordnung zu den Eimern — `dashboard/Liveverrechnung` (E‑190)

- **Verlauf, Kachel *Nachrichten*, Kachel *Fehler*:** Die Korrekturzeilen, deren **Stunde** im Fenster
  liegt (`von` einschließend, `bis` ausschließend), werden auf den Eimer des Paares gehoben — `48H`:
  die Stunde selbst; `30T`: `DATE(stunde)`; `12M`: der Monatserste — und den Rollupzeilen
  desselben Eimers und Rohstatus zugerechnet. **Das ist die Zuordnung des Rollups**: Die Tagesebene
  entsteht aus der Stundenebene, die Monatsebene aus der Tagesebene, beide in derselben Transaktion
  ([`rollup.md`](rollup.md) §5); was der nächste Delta-Lauf in diese Eimer schreibt, ist genau die
  Summe der Stunden. Danach rechnen Verlauf und Kacheln mit den verrechneten Zeilen **wie bisher** —
  Einordnung und Fehlerart bildet weiterhin `MessageStatusClassifier` beim Lesen; kein Block rechnet
  den Live-Rest selbst nach.
- **Klemme je (Eimer, Rohstatus) auf null**, und was auf null fällt, verschwindet: Ein Eimer ohne
  Zeile ist im Verlauf keiner. Anders als im Baum (Klemme je Prozess und Kennzahl) liegt die Klemme
  hier vor der Einordnung — das Dashboard kennt in Block 1 keinen Prozess.
- **Der Leerzustand** folgt der Kachel *Nachrichten* nach der Korrektur: Ein Mandant, dessen einziger
  Verkehr in der laufenden Stunde liegt, ist nicht leer.
- **Die Belegungsprobe des Standardfensters bleibt ohne Korrektur.** Sie entscheidet über das Paar,
  bevor der Live-Rest gelesen ist, und ein Paar, das ohne den angebrochenen Eimer nicht trägt, trägt
  mit ihm nicht besser. *Läuft*, *Wartend* und *Zuletzt aufgefallen* lesen ohnehin live; der Block
  *Stand* (`letzterLauf()`) bleibt, was er war (Punkt 187).
- **Antwort:** der Block `liveRest` zwischen `stand` und `plattform`, dasselbe Record wie im Baum —
  es liegt seit Teil B in `common` (E‑189, `LiveRestResponse.aus(entscheidung, zone)`), weil
  Fachpakete einander nicht kennen.

### Block 5 über die Katalog-Nachlesung (E‑191)

Die Verteilung gruppiert in der Datenbank je Schlüssel einer Sicht; die Korrekturzeilen tragen
Prozesskennungen. Damit Block 5 die Korrektur denselben Zeilen zuordnet wie das Verteilungsstatement
die Rollupzeilen, liest `DashboardRepository.katalogzuordnung(mandant, prozesse)` **für die Prozesse
der Korrekturzeilen** den Katalog nach — je Prozess der Schlüssel je Sicht, **mit demselben
`CASE`-Ausdruck** (E‑i, `common/Katalogzuordnung`) wie im Verteilungsstatement, das `IN` über den
Primärschlüssel, die Mandantenkette als `EXISTS`, **kein `GROUP BY`**:

```sql
SELECT process_id,
       CASE WHEN <E-i über partner>  THEN partner  END,
       CASE WHEN <E-i über richtung> THEN richtung END
FROM overlord_monitor.process_catalog
WHERE process_id IN (?, …)
  AND EXISTS ( … Mandantenkette … )
```

- **Es läuft nur, wenn es Korrekturzeilen im Fenster gibt.** Bei `ANGEWANDT` ohne Zeilen fragte es
  nach nichts. Die Zahl der Statements einer Seite ist damit **zehn** (die neun von vorher und der
  Wasserstand, bei `NICHT_NOETIG` und `AUSGESETZT`), **zwölf** (`ANGEWANDT` ohne Korrekturzeilen)
  oder **dreizehn** — `DashboardStatementsTest.LiveRest` benennt alle einzeln.
- **Ein Prozess ohne Katalogzeile fehlt im Ergebnis** und gilt als *nicht zugeordnet* — dieselbe
  Auskunft wie der `LEFT JOIN` im Verteilungsstatement. Die Summe je Schlüssel entsteht im Dienst,
  **ohne Groß- und Kleinschreibung** verglichen: Das Verteilungsstatement gruppiert unter
  `utf8mb4_general_ci`, die Nachlesung liefert die Schreibweise der einzelnen Zeile, und eine
  Gruppe darf nicht zerfallen, weil eine Katalogzeile anders geschrieben ist als die Gruppe, die
  sie vertritt. Klemme auf null je Schlüssel; *nicht zugeordnet* steht weiter immer, auch mit null.
- **Das ist nicht das zusammengelegte Verteilungsstatement**, das
  `DashboardStatementsTest.kein_zusammengelegtes_verteilungsstatement` seit E‑161 ausschließt: Jenes
  gruppierte nach beiden `CASE`-Ausdrücken; dieses gruppiert nicht. Der Test ist entsprechend
  verfeinert — **keine gruppierende** Abfrage der Seite liest beide Katalogspalten — und läuft
  seither über beide Seiten, mit und ohne Korrekturzeile.
- **Die vierte Mandantenkette der Seite**, und ihre Isolation ist **am Repository** belegt
  (`DashboardIsolationDbIT.nachlesung_liefert_keine_fremde_zeile`): Der Dienst reicht nur Kennungen
  aus einer mandantengefilterten Lesung herein, ein Leck zeigte sich durch den Endpunkt deshalb nie.
  Der Test legt sich seine Katalogzeile selbst an (ein `SUTTONS`-Prozess ohne Zeile, reines `INSERT`,
  Testpräfix, `@AfterEach`) und fragt sie als `VOTG` ab.

**Kachel und beide Sichten zählen damit dieselbe Zahl, auch in der laufenden Stunde** —
`DashboardIsolationDbIT.sichtZaehltDieEigenenNachrichten` verlangt das seit E‑161, und ohne E‑191
wäre der Test bei `ANGEWANDT` rot gewesen. Punkt **191** ist damit geschlossen: Baum und Dashboard
zählen die laufende Stunde gleich.

### Die Messung — M186 (Regel L7)

> #### Vorregistriert — eingetragen und eingecheckt vor dem ersten Lauf
>
> **Was gemessen wird.** `MessungM186DbIT`, die Bauform von M185: Uhr und Wasserstand über
> `@TestBean` gestellt, kein Schreibzugriff auf `rollup_lauf`; je Fall ein Aufwärmlauf, dann die
> beste von fünf. **Dieselben acht Fälle wie M185** (§8: vier Mandanten × typische Stunde mit zwei
> Live-Eimern / dichtester Vierstundenbereich), und je Fall **die drei Paare** der Landingpage:
>
> 1. **durch den Endpunkt** (`GET /api/dashboard?zeitraum=…`, HTTP-Umlauf im Testclient samt
>    Sitzung und Serialisierung) — die Zahl, an der das Tor gemessen wird;
> 2. **am Dienst** (`DashboardService.landingpage` im selben Prozess);
> 3. **der Bezug in derselben Sitzung:** dieselbe Seite am Dienst mit einem Wasserstand, der die
>    Stunde deckt (`NICHT_NOETIG`) — die Seite ohne die zwei bis drei Statements des Live-Rests. Die
>    Differenz zu 2 ist der **Zuschlag** des Live-Rests, ohne Tagesdrift gegen M178.
>
> **Die Erwartung, gerechnet und nicht gemessen** — M178 (Seite, 16.09.2026) plus der Zuschlag aus
> M185 (Dienst gegen M152; der dichteste Bereich bei `NEXANS` ist im Wesentlichen Statement B mit
> 265,7 ms), für `NEXANS` und `SUTTONS`, die M178 gemessen hat:
>
> | Lage | M178, Seite | + Zuschlag (M185) | **erwartet, Endpunkt** |
> |---|---:|---:|---:|
> | `NEXANS` typisch, `48H` / `30T` / `12M` | 65–70 / 223–226 / 294–298 ms | 16–20 ms | **≈ 85 / 245 / 315 ms** |
> | `NEXANS` dicht, `48H` / `30T` / `12M` | dito | 250–270 ms | **≈ 320 / 480 / 555 ms** |
> | `SUTTONS` typisch, `48H` / `30T` / `12M` | 59–62 / 166–167 / 207–209 ms | 12–18 ms | **≈ 75 / 180 / 225 ms** |
> | `SUTTONS` dicht, `48H` / `30T` / `12M` | dito | 25–30 ms | **≈ 90 / 195 / 240 ms** |
>
> Für `VOTG` und `IBIS` gibt es keine M178-Zahl; erwartet werden Seiten in der Größenordnung von
> `SUTTONS` plus ein Zuschlag von 20 bis 45 ms (M185). **Die Rechnung sagt voraus, dass `NEXANS`
> im dichtesten Bereich über zwölf Monate das Tor reißt** — die Seite trägt dort schon ohne
> Live-Rest 294 bis 298 ms, und das eine Statement B kostet 265,7 ms. Genau das soll die Messung
> zeigen oder widerlegen; die Rechnung ist keine Ausrede.
>
> **Das Tor (Entscheidung des Auftraggebers, 17.09.2026):** jede Lage **unter 500 ms durch den
> Endpunkt, in beiden Messstunden**. Liegt eine darüber, wird **angehalten und berichtet** — die
> Obergrenze von drei Stunden (E‑181), das Paar oder der Bereich werden nicht nebenbei
> nachjustiert.
>
> **Wie gelesen wird, festgelegt vor dem Lauf:** Der Zuschlag (2 minus 3) trägt die Aussage über
> diesen Bau; der Endpunkt trägt das Tor; die Rechnung oben trägt nur, ob die Größenordnung hält.
> Abweichungen werden benannt und nicht umgedeutet.
>
> **Zugesichert wird im Läufer nur** `200`, `ANGEWANDT` und dass Kachel und beide Sichten dieselbe
> Zahl tragen — Zeiten gehen nach `System.out` und in keine Zusicherung (Regel T1).

---

## 10. Die Oberfläche (E‑186, E‑187)

- **Typ:** `Prozessbaum.liveRest` in `features/nachrichten/api.ts`.
- **Der Hinweis bei `AUSGESETZT`**, bei den Zahlen des Baums, im klebenden Kopf der Baumspalte
  unter den Kopfzahlen: die Bauform des Katalog-Hinweises (`Alert` ohne Variante, `Info`-Zeichen,
  `katalog-kennzahlen.tsx`) — **kein Rot, kein neues Farbtoken, kein neues Dichtemaß**. Mit Lauf:
  *„Die stündliche Aggregation ist seit längerem nicht erfolgreich gelaufen. Vollständig sind die
  Zahlen bis {vollstaendigBis}."*, die Zeit absolut in der Anzeigezone wie der Stand der Übersicht
  ([`dashboard-frontend.md`](dashboard-frontend.md) §5.7); ohne Lauf: *„Die stündliche Aggregation
  ist noch nicht gelaufen. Die Zahlen sind unvollständig."* Bei `ANGEWANDT` und `NICHT_NOETIG` steht
  nichts — ein Hinweis, der immer da ist, wird nicht mehr gelesen. Texte in `i18n/de.ts` und
  `i18n/en.ts` unter `prozesse.baum.liveRest`.
- **Kein Nachladen** (E‑186, Punkt 189): Der Auftrag wollte den Baum im Takt der Übertragungsliste
  nachladen, *solange deren automatische Aktualisierung eingeschaltet ist*. Seit E‑164
  ([`neu-laden.md`](neu-laden.md)) hat die Prozessansicht keinen Schalter mehr; der Takt träte dort
  nie ein. Entschieden vom Auftraggeber am 17.09.2026: **E‑164 gilt**, Baum und Liste laden weiter
  nur von Hand über „Neu laden", dann beide zusammen. Die E‑50‑Folge beim Stundenwechsel — die
  Liste beginnt oben neu, wenn der Baum ein neues Fenster bringt — gilt damit nur beim Neuladen von
  Hand und ist in `tests/neu-laden.test.tsx` festgehalten.

---

## 11. Die Dev-Zeile — lokaler Wasserstand und Zustand

Lesend geprüft am 17.09.2026 auf der Testkopie, nicht verändert:

| | |
|---|---|
| **W** | `2026-08-27 15:00:00` — Lauf 407, `VOLL`, gegen die **Systemuhr** gefahren (`gestartet_am 2026-08-27 12:21:13 UTC`), 335.610 Zeilen. Der jüngste Lauf (907, 31.08.2026) endet bei `2025-12-30 05:00` und hebt den Wasserstand nicht |
| **G** | `2026-08-27 14:00` |
| **Anwendungsuhr** | Anker `2025-12-30 04:09:47` plus Laufzeit seit dem Start ([`START-LOKAL.md`](START-LOKAL.md) §1) |
| **Zustand lokal** | **`NICHT_NOETIG`** — G liegt rund acht Monate nach dem Anfang der aktuellen Stunde. Der Rollup deckt den ganzen Bestand (`MAX(stunde) = 2026-07-08 17:00`, 3.341.519 Nachrichten in 335.610 Zeilen, gleich `COUNT(*)` über `Message`) |

Eine Bewegung in der laufenden Stunde ist lokal nicht zu sehen; der Hinweis der Oberfläche
erscheint lokal nicht. Belegt ist der Bau über Tests mit gesetzter Uhr und gesetztem Wasserstand
(§12) und über die Messung an historischen Stunden (§8).

---

## 12. Tests

| Test | Was er hält |
|---|---|
| `LiveRestTest` (20, ohne Datenbank) | **Die Entscheidung (13):** kein Lauf; der Normalfall; G nach dem Anfang der aktuellen Stunde und eine Stunde voraus; G gleich dem Anfang der aktuellen Stunde; ein übersprungener Lauf; G genau drei Stunden vor `jetzt` (angewandt) und eine Sekunde darüber (ausgesetzt, vollständig bis G); Sommerzeitbeginn und -ende auf den Labels; W abseits der vollen Stunde abgerundet; `jetzt` auf der vollen Stunde; ein Widerspruch zwischen Zustand und Feldern fällt im Konstruktor. **Die Verrechnung (7):** Statuswechsel innerhalb von G, Aufheben zu null mit bleibender jüngster Stunde, nur Live-Verkehr, jüngste Stunde allein aus der Quelle, Prozess ohne Live-Verkehr, leer, zwei Schreibweisen |
| `ProzessbaumServiceTest` (49, davon **9 neu** unter „Der Live-Rest") | Statuswechsel innerhalb von G lässt die Nachrichtenzahl gleich und hebt die Fehlerzahl; nur innerhalb des Fensters; kein negativer Endwert; nur Live-Verkehr ohne Rollupzeile (Zahlen, letzte Bewegung, `BEWEGT`); die letzte Bewegung als Maximum in beide Richtungen; ein stiller Prozess mit Live-Verkehr wird in jedem Paar bewegt; das freie Fenster vor G, über G hinweg und ab G; der Block in allen drei Zuständen mit G in UTC; derselbe Stichtag für Fenster und Live-Rest. Der Live-Rest ist eine Attrappe; ohne Stellung „ausgesetzt, kein Lauf", damit die 40 Fälle von vorher dieselben Zahlen sehen |
| `ProzessbaumStatementsTest` (28, davon **6 neu** in `EinAufruf`) | die fünf Statements eines angewandten Aufrufs einzeln benannt; `Message` in genau einem, mit Zeitbereich ab G und Kette; ausgesetzt ohne Lauf und nicht nötig je drei, ohne `Message`; das freie Fenster ebenfalls fünf; die beiden Live-Statements **wörtlich**. **Gefallen, bewusst:** *genau zwei Statements*, *genau zwei im freien Fenster*, *kein `Message`* — je einzeln begründet in [`process-view.md`](process-view.md) §11 |
| `ProzessbaumPlanDbIT` (14, `db`, davon **3 neu** unter „Der Live-Teil") | B steigt über einen Index auf `MessageLastUpdate` ein (`range`, Treiberindex aus der Menge der beiden Zeitindizes) und keine Tabelle wird voll gelesen; A über `PRIMARY` oder `message_rollup_prozess_idx`, nie als Durchlauf; die Kette beider Lesungen nie als Durchlauf. **Geändert:** *„Kein Plan dieser Ansicht enthält Message"* gilt weiter für Gerüst und Kennzahlen und ist so umbenannt |
| `ProzessbaumIsolationDbIT` (21, `db`, davon **2 neu**) | die beiden Live-Lesungen liefern für `NEXANS` keine `ProcessID` von `SUTTONS`, am Repository (die dritte Mandantenkette, aus demselben Grund wie die zweite: der Dienst verwirft fremde Zeilen lautlos); der Block `liveRest` steht in der Antwort mit einem der drei Zustände |
| `ProzessbaumLiveRestDbIT` (2, `db`, neu) | **die Summenprobe:** für das 48‑Stunden-Fenster am Anker (`2025-12-28 05:00` bis `2025-12-30 05:00`), W gesetzt auf `03:00`, G = `02:00`, Live-Bereich drei Eimer — je `(process_id, message_status)` ist Rollup − Live-Eimer + Live-Zählung gleich `COUNT(*)` aus `Message`, für `NEXANS` und `SUTTONS`; die Vorprobe „Rollup allein trifft `Message`" entscheidet vorher, ob das Fenster taugt. **Durch den Dienst**, Uhr und Wasserstand gesetzt: die Kopfzahl des Baums ist die Nachrichtenzahl aus `Message`, der Block sagt `ANGEWANDT`. **Ausgegeben, nicht behauptet** (T2): `NEXANS` 72 Schlüssel, 9.950 Nachrichten, im Live-Bereich 17 Rollupzeilen mit 392 und 17 Quellzeilen mit 392 Nachrichten; `SUTTONS` 17 Schlüssel, 1.337 Nachrichten, 21 Zeilen mit 63 auf beiden Seiten; durch den Dienst 9.950 / 50 Fehler und 1.337 / 0 |
| `ProzessbaumGleichheitDbIT` (3, `db`) | unverändert grün — der Block `liveRest` ist in beiden Antworten derselbe |
| `MessungM185DbIT` (1, `db`) | §8; zugesichert wird nur `200`, `ANGEWANDT` und ein stimmiger Rumpf |
| `FensterverengungDbIT` (25, `db`), `RollupStatementsTest` (6), `PaketstrukturTest` (19) | unverändert grün — der Wasserstand aus `common`, die Stundenbildung aus `common`, `MandantContext` in `common` |
| `tests/live-rest.test.tsx` (5, gerendert) | der Hinweis bei `AUSGESETZT` mit Lauf (trägt die Zeitangabe, über denselben Weg formatiert) und ohne Lauf (nur der Satz); **kein** Hinweis bei `ANGEWANDT` und `NICHT_NOETIG` (Abwesenheit im Baum, mit dem Baum als Eichung); der Kasten trägt nicht die Fehlerfarbe. `pnpm check` grün mit **1.123 Fällen in 44 Dateien**; die gerenderten sind **166 in 21 Dateien** (Kopf von `vitest.config.mts`, aus dem Lauf gezählt) |

**Die Verletzungsprobe — ausgeführt, nicht angenommen.** Gefahren am 17.09.2026 je Kette einzeln
(ein Riegel macht den Test sonst blind), jeweils aus einer Sicherungskopie zurückgespielt und mit
`cmp` verglichen:

| entfernt | Ergebnis |
|---|---|
| die Mandantenkette von **B** (`ausDerQuelle`) | **rot**: `die_live_lesung_liefert_keine_fremde_zeile:336 [Message im Live-Bereich …] Expecting … not to contain` |
| die Mandantenkette von **A** (`ausDemRollup`) | **rot**: `…:332 [Rollup im Live-Bereich …] Expecting … not to contain` |

Keine Wanduhrzeit in einer Zusicherung (T1); kein Erwartungswert aus dem Bestand (T2). Alle
übrigen Prozessbaum-Tests sind grün; **geänderte Zusicherungen** stehen einzeln in
[`process-view.md`](process-view.md) §11 (Kasten vom 17.09.2026).

---

## 13. Regelbezug

| Regel | Stand |
|---|---|
| **L2** Keine Live-Aggregation über `Message` | **dritte benannte Ausnahme**, eingetragen und begründet in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Regel 2: Die Zahl hängt am **Takt des Laufs**, nicht an einer Frist und nicht an einem flüchtigen Status; der Bereich ist auf vier Eimer begrenzt; gemessen in M185; Verbraucher: Prozessbaum jetzt, Dashboard mit Teil B |
| **L7** Jede neue Abfrage gemessen | **erfüllt** — M185, `EXPLAIN` je Statement (§8), `ProzessbaumPlanDbIT` (**L15**) |
| **L10** Belegvermerk | **erfüllt** — zwei Vermerke in §8 |
| **M1** Kein Mandantenparameter | **erfüllt** — kein neuer Endpunkt, kein neuer Parameter |
| **M2** `MandantContext` erster Pflichtparameter | **erfüllt** — beide Live-Lesungen; `WasserstandRepository` öffentlich ohne Kontext, weil ohne `jooq.glassfish`; der Typ liegt seit heute in `common` (§7) |
| **M3** Filter im Statement | **erfüllt** — die Kette als `EXISTS` in A und B, Verletzungsprobe je Kette rot |
| **M4** Isolationstest je Endpunkt | **erfüllt** — `ProzessbaumIsolationDbIT` um die Live-Lesungen erweitert, am Repository |
| **S1** Kein Schreibzugriff auf `GlassfishDB` | **erfüllt** — Lese-Kontext; die Tests schreiben auch nicht in `rollup_lauf` |
| **T1, T2** | **erfüllt** — keine Wanduhrzeit und kein Bestandswert in einer Zusicherung |
| **Z1** Kein `now()` | **erfüllt** — `jetzt` ist ein Parameter; M185 stellt die Uhr über `@TestBean` |
| **§6 der Projektbeschreibung** Fachpakete kennen einander nicht | **erfüllt** — der Baustein liegt in `common`, `catalog` und später `dashboard` rufen ihn; dafür ist `MandantContext` ins Fundament gewandert (E‑183) |

---

## 14. Die Entscheidungen dieser Runde

| | | Wer |
|---|---|---|
| **E‑179** | Der Wasserstand als Schnittstelle `common/Wasserstand` mit `Optional`, die eine lesende Stelle `WasserstandRepository` über den Lese-Kontext, öffentlich ohne Kontext | Bau |
| **E‑180** | Die Stundenbildung als `common/Stundeneimer.ausdruck(Field)`, ohne generierte Typen | Bau |
| **E‑181** | G aus dem abgerundeten W; die Grenzen (G gleich der aktuellen Stunde, genau drei Stunden) zugunsten des Korrigierens | Bau |
| **E‑182** | Die Korrektur als vorzeichenbehaftete Zeilen samt jüngster Stunde je Prozess allein aus der Quelle | Bau |
| **E‑183** | `MandantContext` wandert von `security` nach `common` — Weg 1 von dreien | Auftraggeber, 17.09.2026 (Punkt 186) |
| **E‑184** | Zwei Statements statt `UNION ALL`, am Plan und an der Laufzeit entschieden | Bau, §8 |
| **E‑185** | Fällt eine Live-Lesung aus: `AUSGESETZT` mit G, `WARN` im Protokoll; die Wasserstandsabfrage nicht abgefangen | Auftraggeber, 17.09.2026 (Punkt 188) |
| **E‑186** | Kein Nachladen im Takt der Liste — E‑164 gilt | Auftraggeber, 17.09.2026 (Punkt 189) |
| **E‑187** | Der Hinweis als `Alert` ohne Variante bei den Kopfzahlen, zwei Sätze, die Zeit absolut in der Anzeigezone | Bau |
| **E‑188** | Die Verrechnung im Dienst: Schnitt mit dem Fenster, Klemme bei null je Zahl, letzte Bewegung als Maximum, ein Uhrenschlag | Bau |

---

## 15. Offene Punkte

| | |
|---|---|
| ~~**186**~~ | ~~Wo die mandantengefilterte Live-Lesung liegt~~ **Entschieden am 17.09.2026:** Weg 1, `MandantContext` nach `common` (E‑183, §7) |
| **187** | **`rollup_lauf` wird an drei Stellen gelesen:** `common/WasserstandRepository` (Lese-Kontext; Verengung und Live-Rest), `rollup/RollupSchreibRepository.wasserstand()` (Schreib-Kontext, der Job) und `dashboard/DashboardRepository.letzterLauf()` (der Stand). **Entschieden am 17.09.2026: so lassen.** Bleibt als Hinweis stehen — wer die Bedingung ändert, ändert sie dreimal |
| ~~**188**~~ | ~~Was passiert, wenn die Live-Lesung ausfällt~~ **Entschieden am 17.09.2026:** `AUSGESETZT` mit G, Hinweis, `WARN` (E‑185) |
| ~~**189**~~ | ~~Nachladen im Takt der Übertragungsliste~~ **Entschieden am 17.09.2026:** nicht gebaut, E‑164 gilt (E‑186) |
| **190** | **Der Preis des dichtesten Bereichs beim größten Mandanten ist an der Testkopie belegt, an der Produktion nicht.** 18.715 Nachrichten in vier Eimern kosten 265,7 ms im Statement B und 337,0 ms durch den Endpunkt — unter 500 ms, aber das 2,7‑Fache der typischen Stunde. Ob die Produktion dichtere Stunden hat, ist nicht erhoben; die Obergrenze von drei Stunden ist die Stellschraube, und sie ist eine Konstante mit Begründung, kein Schlüssel |
| **191** | **Bis Teil B zählen Baum und Dashboard die laufende Stunde verschieden:** Der Baum trägt den Live-Rest, das Dashboard liest weiter allein den Rollup. Für dasselbe Fenster können beide Ansichten bis zum nächsten Delta-Lauf zwei Zahlen zeigen. Kasten in [`dashboard.md`](dashboard.md) §2; Teil B schließt ihn |
| **192** | **Die Wasserstandsabfrage hat keinen Rückfall.** Fällt sie aus (das eigene Schema), scheitert die Antwort des Baums mit `500`; die Fensterverengung fängt denselben Fall ab und läuft unverengt weiter. Bewusst so gebaut (E‑185: ein Vorfall, kein fehlender Rest) — ob der Baum hier derselben Haltung folgen soll wie die Liste, ist eine eigene Entscheidung |

---

## 16. Was nicht gebaut ist

Nichts am Dashboard (`DashboardRepository.letzterLauf()` bleibt; Teil B ruft den Baustein); kein
kürzerer Takt, keine Änderung an `overlord.rollup.*`, an `rollup_lauf` oder am Rollup-Job — im Paket
`rollup` ändert sich allein die Herkunft der Stundenbildung; keine Änderung an den
Kennzahlen-Statements, am Gerüst oder an E‑34; kein Live-Rest in Nachrichtenliste, BAM-Suche oder
Property-Suche; keine Korrektur älterer Eimer — was mehr als 15 Minuten nachträglich geschrieben
wird, fängt weiter der Nachtlauf; keine Konfiguration für die Obergrenze; kein neuer Endpunkt, keine
neue benannte Mandantenausnahme; kein Nachladen im Takt (E‑186); keine Messung über Teil 3 hinaus.
