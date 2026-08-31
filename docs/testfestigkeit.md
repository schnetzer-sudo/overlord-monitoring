# Testfestigkeit — was ein grüner Test noch bedeutet

Entstanden am 31.08.2026. Diese Datei behandelt **drei Tests, die eine Sicherheitsaussage tragen
sollen und es nicht tun** — und die zwei Regeln, die daraus folgen.

Sie ist keine Testübersicht. Was die einzelnen Tests prüfen, steht in der jeweiligen
Feature-Datei — [`mandantentrennung.md`](mandantentrennung.md) §5 für die Vorlage,
[`prozess-katalog-backend.md`](prozess-katalog-backend.md) und [`bam-werte.md`](bam-werte.md) §9
für die beiden hier behandelten Fälle. **Hier steht, woran ihre Aussage hing und woran sie
jetzt hängt.**

> **Der Satz, unter dem diese Runde steht:** Ein Test, der seine Aussage abschwächt, um zu
> bestehen, ist schlechter als der rote von heute. Wo eine Aussage verlorengeht, steht sie unten
> als offener Punkt — sie wird nicht zugemauert.

---

## 1. Die beiden roten Tests — der Befund

**Beide liegen in derselben Klasse**, `ProzessKatalogIsolationDbIT`. Der Abschlussbericht zu
`feat/liste-fensterverengung` nannte den Klassennamen und „kuratierter Katalog"; der zweite Fall war
über seine Ursache benannt und nicht über seinen Namen. Ein vollständiger `verify`-Lauf am
31.08.2026 (604 Unit-Tests, 311 Integrationstests) nennt sie namentlich, und es sind **genau diese
zwei**:

| Testfall | Zeile | Zusicherung, die fällt | gemessen |
|---|---|---|---|
| `uebernahme_erfasst_nur_den_aktiven_mandanten` | 409 | `assertThat(votgVorher).isPositive()` | `0` |
| `uebernahme_ignoriert_untergeschobenen_mandanten` | 447 | `assertThat(fremd).isNotEqualTo(eigen)` | `0` gegen `0` |

Beide Zusicherungen sind **Vorbedingungen**, keine Aussagen: Sie sollen dem eigentlichen Nachweis
Zähne geben. Der erste verlangt, dass Mandant `VOTG` überhaupt eine übernehmbare Zeile besitzt —
sonst hieße „nach dem fremden Lauf unverändert" nur, dass null gleich null bleibt. Der zweite
verlangt, dass beide Mandanten **verschieden viele** übernehmbare Zeilen haben — sonst bewiese ein
gleicher Antwortrumpf nichts über den untergeschobenen Mandanten.

### Woran genau sie hängen

Übernehmbar ist eine Katalogzeile, die `pflegestatus = 'OFFEN'` trägt **und** eine
`vorschlag_herkunft` aus `('REGEL_A','REGEL_B')` (`ProzessKatalogRepository.findeUebernehmbareVorschlaege`,
Entscheidung E22). Gezählt am 31.08.2026 gegen die Testkopie:

| | `VOTG` | `SUTTONS` |
|---|---:|---:|
| Prozesse des Mandanten | 390 | 17 |
| davon mit Katalogzeile | **390** | **0** |
| davon **übernehmbar** | **0** | **0** |

Und über den ganzen Katalog: **1.457 Zeilen `GEPFLEGT`, 12 `OFFEN`**, Herkunft 318 × `KEINE`,
887 × `REGEL_A`, 264 × `REGEL_B`. **Keine einzige Zeile ist heute übernehmbar** — nicht bei diesen
beiden Mandanten und bei keinem anderen:

| Mandant | Prozesse | mit Katalogzeile | **frei** | übernehmbar |
|---|---:|---:|---:|---:|
| SUTTONS | 17 | 0 | **17** | 0 |
| WOC | 4 | 0 | **4** | 0 |
| EDITIONLINGERI | 9 | 9 | 0 | 0 |
| NXHBE | 17 | 17 | 0 | 0 |
| NEXANS | 733 | 733 | 0 | 0 |
| VOTG | 390 | 390 | 0 | 0 |
| IBISGUS | 89 | 89 | 0 | 0 |
| SYSTEM | 4 | 4 | 0 | 0 |
| IBIS | 192 | 192 | 0 | 0 |
| ZAST | 35 | 35 | 0 | 0 |

Es hängt also **nicht** an einer Zeilenzahl, **nicht** an einem bestimmten Partner und **nicht** an
einem einzelnen Prozess, sondern am **Pflegestand**: Am 27.08.2026 sind 505 Katalogzeilen kuratiert
worden, und damit ist die Menge der offenen Regelvorschläge auf null gefallen.

> ### Das ist der Mangel, nicht die rote Farbe
>
> Ein Test, dessen Wahrheit von veränderlichen Daten in einer **geteilten** Testkopie abhängt, ist
> keine Prüfung, sondern eine Momentaufnahme. Er wird beim nächsten Pflegevorgang wieder rot und
> beim übernächsten wieder — und jedes Mal sucht jemand den Fehler in seinem eigenen Diff.
>
> **Die Erwartungswerte auf den heutigen Katalogstand nachzuziehen wäre keine Abhilfe, sondern die
> Ursache.** Es wäre grün bis zur nächsten Pflege.

### Was M4 verlangt und was Beiwerk ist

**Regel M4 verlangt den Nachweis, dass ein Mandant die Daten eines anderen nicht erreicht.** Dafür
braucht es zwei Mandanten aus verschiedenen Häusern und einen Aufruf. **Keine bestimmte Zeilenzahl
und keinen bestimmten Katalogstand.**

| | verlangt M4 | Beiwerk |
|---|---|---|
| zwei Mandanten aus verschiedenen Häusern (`VOTG` / `SUTTONS`) | ✔ | |
| eine übernehmbare Zeile, an der sich die Trennung zeigen **kann** | ✔ | |
| dass **der Katalog** diese Zeile mitbringt | | ✔ |
| dass die beiden Mandanten **verschieden viele** davon haben | ✔ (als Vorbedingung) | |
| dass **der Katalog** diesen Unterschied mitbringt | | ✔ |

**Die beiden rechten Zeilen sind der ganze Mangel.** Der Nachweis braucht die Zeile — er braucht
nicht, dass jemand anderes sie hingelegt hat.

---

## 2. Der erste Test — die Rollen sind vertauscht

`ProzessKatalogIsolationDbIT.uebernahme_erfasst_nur_den_aktiven_mandanten`.

**Der Weg, den der Test nicht gehen kann.** Naheliegend wäre: Der Test legt sich für **beide**
Mandanten eine übernehmbare Zeile an. Das geht nicht — eine Katalogzeile darf nur auf einem Prozess
**ohne** Zeile entstehen (reines `INSERT`, die Lehre vom 26.08.2026, siehe
[`mandantentrennung.md`](mandantentrennung.md) §5), und `VOTG` hat auf allen 390 Prozessen eine.
Zwei Mandanten mit je einem freien Prozess gäbe es heute nur als `SUTTONS`/`WOC` — und die zu
wählen hieße, den heutigen Pflegestand in den Test zu schreiben. Genau das ist untersagt.

**Der Weg, den er geht: die Rollen tauschen.** Die eine übernehmbare Zeile gehört `SUTTONS` — das
ist der Mandant, der freie Prozesse hat —, und der **fremde** Lauf ist der von `VOTG`. Der Nachweis
besteht aus zwei Schritten, die einander erst zu einem Beweis machen:

| | Schritt | was er zeigt |
|---|---|---|
| 1 | `AUSFUEHREN` als `VOTG` lässt die Zeile von `SUTTONS` **offen** | die Isolationsaussage |
| 2 | `AUSFUEHREN` als `SUTTONS` nimmt **genau diese** Zeile | die Zähne für Schritt 1 |

**Schritt 2 ist der Kern.** Ohne ihn bewiese Schritt 1 nur, dass der Knopf gar nichts tut. Mit ihm
steht fest: Die Zeile *war* übernehmbar, und der fremde Lauf hat sie trotzdem liegen lassen. Der
Mandant, der die Zeile besitzt, braucht dafür **keine** vorgefundene Zeile, und der Mandant, der den
fremden Lauf fährt, braucht **gar nichts** — auch nicht, dass er selbst welche hat.

**Die Gegenrichtung ist schärfer geworden, nicht schwächer.** Statt „die Zahl der übernehmbaren
Zeilen von `VOTG` ist unverändert" vergleicht der Test jetzt den **ganzen Antwortrumpf** der
Pflegeliste von `VOTG` vor und nach dem Lauf von `SUTTONS`. Eine Veränderung an irgendeiner Spalte
einer beliebigen Zeile fällt damit auf. Der Rumpf wird **nach** dem eigenen Lauf von `VOTG` erhoben,
damit der Vergleich die Wirkung des fremden Laufs misst und nicht die des eigenen.

### Die Verletzungsprobe

Ausprobiert am 31.08.2026 und **zurückgenommen**: In `ProzessKatalogRepository.findeUebernehmbareVorschlaege`
wurde `PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())` durch `isNotNull()` ersetzt — der
Mandantenfilter aus Regel M3, ausgehängt.

```
[Der Knopf von VOTG hat eine offene Zeile von SUTTONS uebernommen — er schreibt ueber
 Mandantengrenzen]
expected: "OFFEN"
 but was: "GEPFLEGT"
        at ProzessKatalogIsolationDbIT.uebernahme_erfasst_nur_den_aktiven_mandanten:455
```

**Der Test wird rot, und seine Meldung nennt den Vorgang statt einer Zahl.** Der Arbeitsbaum ist
danach über `git checkout --` wiederhergestellt worden; die Änderung ist in keinem Commit.

### Was verlorengegangen ist

**Nichts an der M4-Aussage.** Verloren ist eine Aussage, die der alte Test nie belegt hat: dass die
Übernahme auf einem Mandanten mit **mehreren** übernehmbaren Zeilen genau dessen Zeilen erfasst und
keine fremde. Sie steht als offener Punkt in §6.

---

## 3. Der zweite Test — den Unterschied herstellen statt vorfinden

`ProzessKatalogIsolationDbIT.uebernahme_ignoriert_untergeschobenen_mandanten`.

Er prüft **Regel M1 in beiden Formen**: als untergeschobenes `mandantId`-Feld im Anfragekörper und
als Abfrageparameter. Der Nachweis ist ein Vergleich dreier Antwortrümpfe — ohne Feld, mit Feld, mit
Parameter —, und er trägt nur, wenn die beiden Mandanten **verschieden viele** übernehmbare Zeilen
haben. Sonst sähe eine Antwort, die den untergeschobenen Mandanten befolgt, genauso aus wie eine,
die ihn ignoriert.

**Bisher hat der Test diesen Unterschied im Katalog vorausgesetzt.** Seit dem 27.08.2026 haben alle
Mandanten null, und null ist gleich null.

**Jetzt stellt er ihn her.** Er misst beide Ausgangszahlen und legt danach so viele eigene Zeilen
an, dass sie sich mit Sicherheit unterscheiden:

```java
int anzulegen = fremd == eigenVorher + 1 ? 2 : 1;
```

**Mehr als zwei sind nie nötig**, und zwar unabhängig vom Pflegestand:

| Ausgangslage | angelegt | danach |
|---|---:|---|
| `fremd ≠ eigenVorher + 1` | 1 | `eigenVorher + 1 ≠ fremd` — eine Zeile trennt sie, weil sie sie nicht genau zusammenführt |
| `fremd = eigenVorher + 1` | 2 | `eigenVorher + 2 = fremd + 1 ≠ fremd` |

Die Erwartung nennt damit **keine Zahl aus dem Katalog**, sondern eine, die der Test selbst gebaut
hat: `assertThat(mitFeld.betroffen).isEqualTo(eigen)` mit `eigen = eigenVorher + anzulegen`.

### Die Verletzungsprobe

Ausprobiert am 31.08.2026 und **zurückgenommen**: `VorschlagsuebernahmeRequest` bekam ein Feld
`mandantId`, und der Controller benutzte es, wenn es gesetzt war — Regel M1, gebrochen.

```
expected: "{"modus":"VORSCHAU","betroffen":1,"regelA":1,"regelB":0}"
 but was: "{"modus":"VORSCHAU","betroffen":0,"regelA":0,"regelB":0}"
        at ProzessKatalogIsolationDbIT.uebernahme_ignoriert_untergeschobenen_mandanten
```

Der untergeschobene Mandant hat die Antwort verändert — genau die Auskunft, die der Test verbietet.
Der Arbeitsbaum ist danach über `git checkout --` wiederhergestellt worden.

### Was verlorengegangen ist

**Nichts.** Der Test prüft dieselbe Aussage mit derselben Schärfe; er beschafft sich die
Vorbedingung nur selbst, statt sie vorauszusetzen.

---

## 4. Der dritte Test — gezählt statt gestoppt

`BamIsolationDbIT.gegenprobe_mit_echter_fremder_kennung`.

### Was er soll

[`bam-werte.md`](bam-werte.md) §9, Prüfung 4: Eine **fremde, existierende** und eine **erfundene**
`MessageID` müssen ununterscheidbar sein — **auch in der Laufzeit**. Sonst ließe sich über die
Antwortzeit herausfinden, ob eine Kennung existiert. Die Sorge steht dort im Klartext: *eine
nachgelagerte Existenzprüfung kostete einen zusätzlichen Zugriff.*

### Warum er das nicht konnte

| | |
|---|---|
| **geschützt** | ein Datenbankzugriff von **0,44 ms** ([`bam-werte.md`](bam-werte.md) §4) |
| **gemessen** | zwei HTTP-Aufrufe mit `System.nanoTime`, auf einem Testrechner |
| **Schranke** | Verhältnis `max/min < 10`, „bewusst grob" |
| **Verlauf** | einmal rot bei **279 ms gegen 20 ms** (Faktor 14), danach dreimal grün |

Der Rest ist Arithmetik: Zwischen dem Signal (0,44 ms) und der Schranke (Faktor 10 auf einem
Messwert von rund 20 ms) liegen zwei Größenordnungen Rauschen. **Der Test war nicht ungenau, er hat
die falsche Größe gemessen.** Was er tatsächlich erfasst hat, ist mit hoher Wahrscheinlichkeit der
erste Aufruf einer JVM — dasselbe Muster, das [`rollup.md`](rollup.md) §14 für den ersten
Delta-Lauf mit 387 ms gegen 20–31 ms beschreibt.

**Eine großzügigere Schranke hätte das nicht behoben, sondern verdeckt.** Sie macht den Test
stiller, nicht besser: Er fiele seltener und fände einen zusätzlichen Zugriff dann gar nicht mehr.

### Was jetzt dasteht: die Zahl der Datenbankzugriffe

Der jOOQ-`ExecuteListener` ist im Projekt schon da — `config/ReadOnlyExecuteListener` hängt als
dritte Schicht des Schreibschutzes auf dem Lese-Kontext
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6). Der Test hängt einen **zweiten** daneben,
der zählt statt abzuweisen.

| | |
|---|---|
| **Wo** | `BamIsolationDbIT.Zugriffszaehler`, angebracht über `@Import` einer `@TestConfiguration` in derselben Klasse — **ausschließlich `src/test`**, am Anwendungscode ändert sich nichts |
| **Woran** | nur `glassfishDsl`. Der Schreib-Kontext bleibt außen vor; Sitzungs- und Protokollschreiben in `overlord_monitor` gehören nicht zur Frage |
| **Wann** | in `executeStart`, weil `ExecuteContext.sql()` dort steht — gerendert, mit Platzhaltern statt Bindewerten |
| **Was verglichen wird** | nicht nur die **Zahl**, sondern die **Folge der Statement-Texte**. Zwei Anfragen mit verschiedenen Kennungen sind darin Zeichen für Zeichen gleich |

**Der `ReadOnlyExecuteListener` bleibt erhalten.** Der Zähler wird an die vorhandenen
`ExecuteListenerProvider` **angehängt** und ersetzt sie nicht — ein Ersetzen hätte den
Schreibschutz für die Dauer dieser Testklasse stillgelegt.

**Der Aufwärmlauf bleibt**, aber aus einem anderen Grund als vorher: Er beruhigt keine Messung mehr,
sondern sorgt dafür, dass ein einmaliger Zugriff beim ersten Aufruf nicht in genau einer der beiden
Folgen landet.

**Beide Fälle setzen heute zwei Statements ab** — die Mandantenliste der Sitzung und die eine
`EXISTS`-Abfrage mit Mandantenkette.

### Die Verletzungsprobe

Ausprobiert am 31.08.2026 und **zurückgenommen**: In `BamService.werte` wurde eine **ungefilterte**
Existenzabfrage vor die gefilterte gesetzt — die Bauform, vor der `bam-werte.md` §9 und
[`mandantentrennung.md`](mandantentrennung.md) §5 ausdrücklich warnen. Eine erfundene Kennung endet
danach nach einem Zugriff, eine fremde echte nach zweien.

Der Test wird rot, und die Meldung nennt **das zusätzliche Statement im Wortlaut**:

```
[eine nachgelagerte Existenzpruefung kostete einen zusaetzlichen Zugriff und waere hier sichtbar]
Expecting actual: [ …Mandant-Liste…,
                    select exists (select 1 as `one` from `GlassfishDB`.`Message`
                                   where `GlassfishDB`.`Message`.`MessageID` = ?),
                    select exists ( … and exists ( … ProjectMandant.MandantID = ?)) ]
to be equal to:   [ …Mandant-Liste…,
                    select exists (select 1 as `one` from `GlassfishDB`.`Message`
                                   where `GlassfishDB`.`Message`.`MessageID` = ?) ]
```

**Das ist der Unterschied zur Wanduhr in einem Bild:** Die alte Prüfung hätte hier zwei Zahlen
genannt, die man deuten muss. Diese nennt das Statement, das durchgerutscht ist.

### Zwanzig Läufe, zwanzigmal grün

Bei einem Test, der wegen Unzuverlässigkeit angefasst wurde, ist einmal grün kein Nachweis. Gefahren
am 31.08.2026: **zwanzig `verify`-Läufe hintereinander**, jeder in einer **eigenen JVM** — also
genau in der Lage, in der die alte Prüfung gefallen ist. Ergebnis in §5.

### Die Lücke, und sie wird benannt statt beantwortet

**Die Zugriffszählung deckt nicht ab, dass zwei gleich viele Zugriffe verschieden lange dauern
könnten** — etwa weil das eine Statement Zeilen liest und das andere keine. Ob das eine reale Lücke
ist, ist in dieser Runde **nicht** beantwortet. Sie steht als offener Punkt in §6.

---

## 5. Zwanzig Läufe, zwanzigmal grün

Gefahren am 31.08.2026, nach dem Umbau aus §4.

| | |
|---|---|
| **Bauform** | zwanzig vollständige `mvnw verify`-Aufrufe hintereinander, jeder mit `-Dit.test=BamIsolationDbIT` |
| **Warum zwanzig getrennte Aufrufe** | jeder startet eine **eigene JVM** und einen eigenen Anwendungskontext — also genau die Lage, in der die alte Prüfung mit 279 ms gegen 20 ms gefallen ist |
| **Ergebnis** | **20 von 20 grün**, je sieben Testfälle, kein Fehlschlag und kein Fehler |
| **Laufzeit je Lauf** | 19,54 s bis 21,29 s |

**Die Zahl steht hier, weil sie zur Aussage gehört.** Bei einem Test, der wegen Unzuverlässigkeit
angefasst wurde, ist einmal grün kein Nachweis — die alte Prüfung war *dreimal hintereinander* grün,
nachdem sie gefallen war.

---

## 6. Offene Punkte

| Nr. | Punkt | Woher |
|---|---|---|
| **T-1** | **Zwei gleich viele Datenbankzugriffe könnten verschieden lange dauern** — etwa weil das eine Statement Zeilen liest und das andere keine. Die Zugriffszählung aus §4 deckt das nicht ab. **Ob das eine reale Lücke ist, ist nicht beantwortet.** Zu klären wäre zuerst, ob der Unterschied überhaupt messbar ist, und erst danach, wie man ihn absichert — nicht über die Wanduhr | §4, [`bam-werte.md`](bam-werte.md) §9 |
| **T-2** | **Die Übernahme auf einem Mandanten mit *mehreren* übernehmbaren Zeilen ist ungeprüft.** Der Test aus §2 legt genau eine an. Dass die Übernahme bei fünf eigenen und drei fremden Zeilen genau die fünf erfasst, folgt daraus nicht — es folgt aus dem Statement, und das ist ein Argument, kein Test | §2 |
| **T-3** | **Der Test aus §2 braucht weiterhin einen Prozess ohne Katalogzeile.** Heute haben nur `SUTTONS` (17 frei) und `WOC` (4 frei) welche. Werden auch die kuratiert, wird der Test wieder rot — dann allerdings mit einer Meldung, die genau das sagt, und nicht mit einer Zahl, die niemand einordnen kann. **Eine Abhilfe wäre, dass die Testkopie einen Prozess dauerhaft frei hält;** das ist eine Absprache und keine Codeänderung | §2 |

---

## 7. Der Suchlauf — wo sonst noch eine Wanduhr in einer Zusicherung steht

Regel **T1** gilt rückwirkend. Der Testbestand ist deshalb am 31.08.2026 vollständig durchsucht
worden — **86 Java-Dateien** unter `backend/src/test`, davon 36 `*DbIT`.

**Gesucht wurde nach:** `System.nanoTime`, `System.currentTimeMillis`, `Duration.between`,
`Instant.now`, `LocalDateTime.now`, `StopWatch`, `.toMillis(`, `.toNanos(`, `.toSeconds(`,
`elapsed`, `dauer`, `laufzeit`, `verhaeltnis`, `isLessThan`, `isCloseTo`/`within(`, `Awaitility`,
`Thread.sleep`, `@Timeout`, `assertTimeout`, `TimeUnit`, `Clock.system`, `new Date(`.

> **Die entscheidende Unterscheidung ist nicht, ob eine Zeit gemessen wird, sondern ob der Messwert
> in einer Zusicherung landet.** Ein Messtest, der Zahlen ausgibt, behauptet nichts über
> Wanduhrzeit. Ein `assertThat` auf eine Dauer schon.

### Ein Fund — und er wird nicht mitgeändert

| Datei | Zeile | Was |
|---|---|---|
| `message/KettenIsolationDbIT.java` | 262–288 | `gegenprobe_mit_echter_fremder_kennung()` — **zeichengleich** zu der Fassung, die §4 gerade abgelöst hat: zwei `System.nanoTime` um zwei HTTP-Aufrufe, `assertThat(verhaeltnis).isLessThan(10.0)`. Nur der Pfad ist ein anderer (`…/{id}/kette`), und die Bezugsmessung heißt „eine halbe Millisekunde (M30-1)" statt 0,44 ms |

**Es ist derselbe Test an einem anderen Endpunkt, mit demselben Mangel.** Die Bauform aus §4 lässt
sich unverändert darauf übertragen: derselbe Zähler, dieselbe Vergleichsform.

> **Er wird in dieser Runde nicht angefasst.** Ein Fund ist ein Befund für die nächste Runde — und
> `KettenIsolationDbIT` ist heute grün. Ihn mitzuändern hieße, eine Änderung ohne Anlass in einen
> Commit zu schieben, dessen Abnahme etwas anderes prüft. **Offener Punkt T-4.**

### Kein Fund — vollständig, damit die Suche belegbar ist

| Was | Wo | Warum es keiner ist |
|---|---|---|
| `rollup/MessungM96DbIT.java:130–170` | vier `dauer*`-Größen | Sie gehen **ausschließlich** über `melde(…)` nach `System.out`. Die Zusicherungen der Datei (Z. 186–200) betreffen nur Zeilen- und Summenzahlen. Der Wert stammt nicht einmal aus dem Test, sondern aus `RollupErgebnis.dauer()` |
| `common/DevClockFactoryTest.java:47–59` | `Clock.systemUTC()` in einer Zusicherung | Geprüft wird nur **Monotonie** (`isAfterOrEqualTo`), keine Ober- oder Untergrenze. Kann durch Last nicht kippen |
| `common/DevClockFactoryTest.java:29–44` | `Duration.between` in Zusicherungen | Beide Uhren sind `Clock.fixed(...)` mit Literal-Instants. Reine Arithmetik |
| `PaketstrukturTest.java:300–331` | `System.currentTimeMillis` | Steht dort als **verbotener Aufruf** in einer ArchUnit-Regel, nicht als Messung |
| `security/AnmeldeServiceTest.java:137–159` | Zeitkanal beim Anmelden | **Die zeitfreie Gegenvariante, und sie ist älter als T1:** Geprüft wird der `verify(passwortKodierer).matches(...)`-**Aufruf**, ausdrücklich nicht die Laufzeit. Das Klassen-Javadoc sagt es in Zeile 33 |
| `message/NachrichtendetailDbIT`, `NachrichtendetailServiceTest` | „Dauer" als Fachfeld | Belegdurchlaufzeiten aus Datenbankzeitstempeln, gegen eine feste `JETZT`-Konstante — nicht gegen die Uhr des Testlaufs |
| dreizehn weitere `isLessThan`-Stellen | Zählwerte, Positionen im gerenderten SQL, Schleifenbremsen | kein Messwert |

**Negativbefunde:** kein `Thread.sleep`, kein `Awaitility`, kein `StopWatch`, kein `@Timeout`, kein
`assertTimeout`, kein `Instant.now()`/`LocalDateTime.now()`, kein `new Date()`, kein `isCloseTo(…,
within(…))` auf einer Dauer — im **gesamten** Testbestand. `System.nanoTime` kommt nach dem Umbau
aus §4 nur noch an **einer** Stelle vor, und das ist der Fund oben.

### Der offene Punkt

| Nr. | Punkt | Woher |
|---|---|---|
| **T-4** | **`KettenIsolationDbIT.gegenprobe_mit_echter_fremder_kennung` behauptet weiterhin etwas über Wanduhrzeit** (`isLessThan(10.0)` auf ein Dauerverhältnis) und verstößt damit gegen Regel T1. Die Abhilfe ist bekannt und in §4 gebaut; sie ist hier bewusst nicht mitgezogen worden | §7 |
