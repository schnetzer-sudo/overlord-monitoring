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

> **Nachtrag vom 31.08.2026 (Schritt 10b‑2), §9.** Zwei der offenen Punkte aus §6 sind abgetragen:
> **T‑3** über eine Absprache mit dem Auftraggeber (`WOC` bleibt unkuratiert) und **T‑4** über den
> Umbau von `KettenIsolationDbIT` auf dieselbe Zugriffszählung, die §4 beschreibt. **Damit gilt
> Regel T1 im gesamten Testbestand.** Offen bleiben T‑1, T‑2 und T‑5 — **und T‑6 ist dazugekommen**,
> gefunden von einer Verletzungsprobe (§9.3).

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

> **Dieser Abschnitt beschreibt den Entwurf; §8.2 beschreibt, wo er noch falsch war.** Der erste
> Bau fuhr den fremden Lauf als `AUSFUEHREN` und hätte damit auf einer geteilten Testkopie
> kuratierte Zeilen angefasst. Die Zähne stecken deshalb heute auf der **Leseseite**.
>
> **Und der Mandant ist seit dem 31.08.2026 ein anderer:** Wo unten `SUTTONS` steht, steht im Code
> heute `WOC`. Die Begründung ist T‑3 und steht in §9.1; an der Bauform ändert sich nichts, nur an
> der Kennung.

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
| **T-6** | **Die Kennungsvergleiche der übrigen Isolationstests sind nicht auf ihre Richtung geprüft.** Sie fragen *„steht hier eine Kennung des anderen Mandanten?"*; die schärfere Frage ist *„gehört jede Kennung, die hier steht, mir?"* (§9.3). Bei Liste, Detail, Kette und BAM haben beide Fassungen vermutlich Zähne, weil dort Zeilen und nicht Zahlen zurückkommen — **vermutlich, nicht geprüft** | §9.3 |
| **T-5** | **Die Übernahme auf einem Mandanten mit mehreren übernehmbaren Zeilen ist weiterhin ungeprüft** — und seit dem Umbau aus §8 lässt sich das auch nicht mehr durch einen fremden `AUSFUEHREN`-Lauf nachholen. Ein Testkonto mit einem eigenen, wegwerfbaren Mandanten wäre der saubere Weg; den gibt es nicht | §8 |
| ~~**T-3**~~ | ~~**Der Test aus §2 braucht weiterhin einen Prozess ohne Katalogzeile.** Heute haben nur `SUTTONS` (17 frei) und `WOC` (4 frei) welche. Werden auch die kuratiert, wird der Test wieder rot — dann allerdings mit einer Meldung, die genau das sagt, und nicht mit einer Zahl, die niemand einordnen kann. **Eine Abhilfe wäre, dass die Testkopie einen Prozess dauerhaft frei hält;** das ist eine Absprache und keine Codeänderung~~ — **erledigt am 31.08.2026, siehe §9** | §2 |

---

## 7. Der Suchlauf — wo sonst noch eine Wanduhr in einer Zusicherung steht

Regel **T1** gilt rückwirkend. Der Testbestand ist deshalb am 31.08.2026 vollständig durchsucht
worden — **86 Java-Dateien** unter `backend/src/test`, davon 36 `*DbIT` (Stand vor den drei Testklassen, die in dieser Runde dazugekommen sind).

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
>
> **Nachgeholt am 31.08.2026 in Schritt 10b‑2** — die nächste Runde ist dieselbe geblieben, aber ein
> eigener Commit. Siehe §9.2.

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
`assertTimeout`, kein `new Date()`, kein `isCloseTo(…, within(…))` auf einer Dauer — im
**gesamten** Testbestand.

> **Eine Zeile dieser Aufzählung war zu weit gefasst und ist am 31.08.2026 korrigiert worden.** Sie
> sagte zusätzlich „kein `Instant.now()`/`LocalDateTime.now()`". Das stimmt so nicht:
> `DatenzugriffDbIT:187` und `NachrichtenUeberfaelligDbIT:96` rufen **`LocalDateTime.now(anwendungsuhr)`**
> auf — mit der **Anwendungsuhr** als Argument, und das ist genau die Form, die Regel Z1 vorschreibt.
> **Kein Fund also, aber auch kein Negativbefund**: Verboten ist der argumentlose Aufruf, und der
> kommt nicht vor. Der Unterschied gehört benannt, weil eine falsche Vollständigkeitsaussage
> schlimmer ist als eine fehlende. `System.nanoTime` kommt nach dem Umbau
aus §4 nur noch an **einer** Stelle vor, und das ist der Fund oben.
>
> **Fortgeschrieben am 31.08.2026 (Schritt 10b‑2):** Auch diese eine Stelle ist weg. `System.nanoTime`
> steht seither in **keiner** Zusicherung mehr — die beiden verbliebenen Vorkommen sind Zitate im
> Klassen-Javadoc von `BamIsolationDbIT` und `KettenIsolationDbIT` und beschreiben, was dort bis zu
> diesem Tag stand. Siehe §9.2.

### Der offene Punkt

| Nr. | Punkt | Woher |
|---|---|---|
| ~~**T-4**~~ | ~~**`KettenIsolationDbIT.gegenprobe_mit_echter_fremder_kennung` behauptet weiterhin etwas über Wanduhrzeit** (`isLessThan(10.0)` auf ein Dauerverhältnis) und verstößt damit gegen Regel T1. Die Abhilfe ist bekannt und in §4 gebaut; sie ist hier bewusst nicht mitgezogen worden~~ — **erledigt am 31.08.2026, siehe §9.2** | §7 |

---

## 8. Nachtrag vom selben Tag — was eine Durchsicht danach noch gefunden hat

**Die elf Commits sind nach dem grünen `verify` durch eine adversariale Durchsicht gegangen**
(vier Prüfer mit je eigener Linse, jeder Befund einzeln gegengelesen). Zwei Befunde haben die
Gegenprobe überstanden, und **beide sind ernster als die Farbe eines Testlaufs** — kein Test war
rot, und in einem Fall behauptete die Dokumentation das Gegenteil des Codes.

### 8.1 Der Rückwärtslauf hatte keine Transaktion je Scheibe

**Behauptet an drei Stellen, umgesetzt an keiner.** `RollupNachzug`, `docs/rollup.md` §6a und der
Protokolltext für den Betreiber sagten *„jede Scheibe ist ihre eigene Transaktion"*. Im Code rief
der Nachzug die beiden Ebenenmethoden **einzeln** auf; beide tragen bewusst kein eigenes
`@Transactional`, und von außen aufgerufen lief jede im **Autocommit**.

Die Einzelheiten und die Abhilfe stehen in [`rollup.md`](rollup.md) §6a. **Hier steht, was daraus
für Tests folgt:**

> **Der Datenbanktest zum Abbruch konnte den Fehler nicht finden — und zwar aus einem Grund, der
> sich verallgemeinern lässt.** `RollupDbIT.keine_ebene_bleibt_bei_einem_abbruch_zurueck` nimmt eine
> Transaktion von außen zurück und prüft, dass keine Ebene etwas behält. Das ist eine richtige
> Prüfung, aber sie **bringt ihre Voraussetzung selbst mit**: `monitorDsl` hängt über
> `TransactionAwareDataSourceProxy` an der Transaktion des Aufrufers, also nimmt jedes Statement
> daran teil — ob die Methode annotiert ist oder nicht.
>
> **Ein Test, der die Bedingung herstellt, die er prüfen will, prüft sie nicht.** Das ist derselbe
> Fehler wie eine Vorbedingung, die aus dem Bestand kommt (§1) — nur andersherum.

Geprüft wird deshalb seit dem 31.08.2026 zusätzlich die **Ursache**:
`RollupTransaktionsgrenzenTest` hält über Reflexion fest, wo die Klammern sitzen
(`ersetzeFenster`, `rechneAbgeleiteteEbenenNeu`) und wo ausdrücklich keine sitzt (die beiden
Ebenenmethoden, weil sie Bausteine beider Klammern sind). **Dieselbe Bauform, die Regel T1 für
Laufzeiteigenschaften verlangt** — und derselbe Grund: Die Wirkung ist hier nicht prüfbar, die
Ursache schon.

### 8.2 Der neue Katalogtest fuhr `AUSFUEHREN` gegen den echten Bestand

**Der Umbau aus §2 hat eine Gefahr eingebaut, die der alte Test nicht hatte.** Um zu zeigen, dass
die Zeile von `SUTTONS` liegen bleibt, fuhr er `AUSFUEHREN` **als `VOTG`**. Der Knopf übernimmt
aber **alle** übernehmbaren Zeilen des aktiven Mandanten:

> Hätte `VOTG` welche, kuratierte der Testlauf sie mit, setzte dabei `geaendert_von` auf den
> Testnutzer — **und die Aufräumregel löschte sie danach.** Das ist der Vorfall vom 26.08.2026, nur
> über einen anderen Weg ([`mandantentrennung.md`](mandantentrennung.md) §5). Dass es nicht knallte,
> lag allein daran, dass `VOTG` zurzeit keine hat — **also an genau dem Pflegestand, von dem der
> Test unabhängig sein sollte.**

**Behoben, indem die Zähne auf die Leseseite gewandert sind.** Der Nachweis lautet jetzt: Die
**Vorschau** von `VOTG` zählt die neu angelegte Zeile von `SUTTONS` **nicht** mit — ein Vergleich
der Zahl vor und nach dem Anlegen, ohne einen einzigen Schreibzugriff. Die Zähne gibt ihm wie zuvor
der eigene `AUSFUEHREN`-Lauf von `SUTTONS`, der genau diese Zeile nimmt.

**Die Verletzungsprobe ist neu gefahren** (31.08.2026): Mandantenfilter in
`findeUebernehmbareVorschlaege` ausgehängt →

```
[Die Vorschau von VOTG zaehlt eine Zeile von SUTTONS mit — der Mandantenfilter der
 uebernehmbaren Vorschlaege traegt nicht]
expected: 0
 but was: 1
```

**Der Test wird jetzt früher rot als vorher** — auf der Leseseite, bevor überhaupt geschrieben wird.

**Und ein Wachposten steht vor jedem `AUSFUEHREN`:**
`nurEigeneDuerfenUebernommenWerden` sucht übernehmbare Zeilen des aktiven Mandanten **ohne**
Testpräfix und bricht ab, statt sie anzufassen. Er ist heute wirkungslos und wird es nicht bleiben.

### 8.3 Drei Monatstests waren gegen ihren eigenen Vorzustand blind

`monatsebene_ist_die_summe_der_tagesebene`, `fenster_ueber_den_monatswechsel_schreibt_beide_monate`
und `monatseimer_umfasst_den_ganzen_monat` verglichen die materialisierte Monatsebene mit der aus
der Tagesebene gerechneten. **Eine Monatszeile, die aus einem früheren Lauf korrekt dastand, besteht
diesen Vergleich auch dann, wenn der Lauf sie gar nicht angefasst hat.** Die ersten beiden legen
jetzt vorher eine Altlast an, die nur ein echtes Löschen-und-Neuschreiben entfernt; der dritte
rechnet sich seinen Vergleichswert selbst, statt ihn aus fremden Tageszeilen zu ziehen (Regel T2).

### Was die Durchsicht sonst noch fand

Zehn Widersprüche zwischen Dokumentation und Code — ein Schaltername, der nicht mehr existierte,
zwei Zahlen („vier Statements", „86 Dateien"), drei Stellen, die die Monatsebene weiterhin als
ungebaut führten, ein Querverweis auf den falschen Abschnitt, ein „eine Ebene höher", das „tiefer"
heißen musste, und die Vorlage in [`mandantentrennung.md`](mandantentrennung.md) §5, die weiterhin
einen Laufzeitvergleich verlangte, den T1 verbietet. Alle behoben.

**Ein Negativbefund dieser Datei war selbst falsch** und ist korrigiert: §7 sagte „kein
`LocalDateTime.now()`" — es gibt zwei, beide mit der **Anwendungsuhr** als Argument und damit genau
in der Form, die Z1 vorschreibt. **Eine falsche Vollständigkeitsaussage ist schlimmer als eine
fehlende.**

---

## 9. Schritt 10b‑2 — T‑3 und T‑4 abgetragen, T‑6 gefunden *(31.08.2026)*

Beide Punkte aus §6 sind erledigt, jeder in einem eigenen Commit und keiner davon durch eine
abgeschwächte Zusicherung. **Dazu ein neuer Fund**, und er stammt nicht aus einer Durchsicht,
sondern aus der Verletzungsprobe eines neu gebauten Tests (§9.3).

### 9.1 T‑3 — der Katalogtest hat eine dauerhaft freie Reserve

**T‑3 war kein Codefehler, sondern eine offene Absprache.** `ProzessKatalogIsolationDbIT` legt sich
seine übernehmbare Zeile selbst an (§2), und das geht nur auf einem Prozess **ohne** Katalogzeile.
Solche gab es am 31.08.2026 nur noch bei `SUTTONS` (17 frei) und `WOC` (4 frei) — und die
Katalogpflege des Auftraggebers hätte beide aufgebraucht.

**Der Auftraggeber hat am 31.08.2026 entschieden:** `SUTTONS` wird kuratiert, **`WOC` bleibt
dauerhaft unkuratiert** und ist die Reserve des Tests.

| | vorher | nachher |
|---|---|---|
| Mandant, auf dem der Test anlegt | `MANDANT_B` = `SUTTONS` | `MANDANT_FREI` = `WOC` |
| Betroffene Testfälle | `uebernahme_erfasst_nur_den_aktiven_mandanten`, `uebernahme_ignoriert_untergeschobenen_mandanten` | dieselben zwei |
| Paarung für Regel M4 | `VOTG` gegen `SUTTONS` | **`VOTG` gegen `WOC`** — ebenfalls zwei verschiedene Häuser |
| Die übrigen elf Testfälle | `SUTTONS` | **unverändert `SUTTONS`** |

**Warum nicht die ganze Klasse.** Nur diese beiden Fälle brauchen einen *freien* Prozess. Alle
anderen brauchen bloß einen *fremden* Bestand, und den hat `SUTTONS` kuratiert wie unkuratiert.
Sie mitzuziehen wäre eine Änderung ohne Anlass gewesen — und `WOC` hat mit vier Prozessen ohnehin
den dünneren Bestand.

**Die Konstante steht in der Testklasse und nicht in `SicherheitsTestbasis`.** `MANDANT_A` und
`MANDANT_B` sind die Paarung *aller* Isolationstests; `MANDANT_FREI` ist eine Eigenschaft, die
genau eine Klasse braucht — *mindestens ein Prozess ohne Katalogzeile*. Stünde sie in der Basis,
sähe es aus wie eine dritte Standardpaarung.

> ### Der Satz, ohne den die Abhilfe in einem halben Jahr weg wäre
>
> Eine Absprache, die nur im Kopf des Auftraggebers steht, ist keine. Sie steht deshalb in
> [`prozess-katalog.md`](prozess-katalog.md) §1 als eigener Kasten — **dort**, wo der Text sonst
> begründet, warum *alle* Prozesse kuratiert werden, und nicht in einer Fußnote am Ende. Der offene
> Punkt 2 in §10 derselben Datei ist für `WOC` mit demselben Datum geschlossen und für `SYSTEM`
> ausdrücklich offen geblieben.
>
> **`WOC` kostet nichts.** Er ist nach [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.2 ein
> technischer Mandant und kein Kunde; die Verteilung nach Partner wird für ihn nicht ausgewertet.

**Was verlorengegangen ist:** nichts. Der Test prüft dieselbe Aussage mit derselben Schärfe auf
einem anderen Mandanten. Was er **nicht** kann, ist weiterhin T‑5: die Übernahme bei *mehreren*
eigenen Zeilen.

**Der offene Rand, und er wird benannt statt zugemauert:** Werden eines Tages auch die vier
Prozesse von `WOC` kuratiert, ist der Test wieder rot — dann allerdings mit der Meldung *„Jeder
Prozess dieses Mandanten trägt bereits eine Katalogzeile"*, die genau das sagt. Das ist kein
Rückfall auf T‑3, sondern die Absprache, die gebrochen wurde.

### 9.2 T‑4 — `KettenIsolationDbIT` zählt jetzt, statt zu stoppen

**Der Fund aus §7 ist abgetragen.** `gegenprobe_mit_echter_fremder_kennung` misst keine Wanduhrzeit
mehr; die Bauform aus §4 ist übertragen — derselbe Zähler, dieselbe Vergleichsform.

| | vorher | nachher |
|---|---|---|
| Was verglichen wird | `max/min` zweier `System.nanoTime`-Differenzen gegen `isLessThan(10.0)` | die **Folge der gerenderten Statements** auf `glassfishDsl`, Zeichen für Zeichen |
| Was geschützt wird | ein Datenbankzugriff von einer halben Millisekunde (M30‑1) über HTTP | derselbe Zugriff, an seiner Ursache |
| Geprüfte Endpunkte | nur `…/{id}/kette` | **`…/{id}/kette` und `…/{id}/kette/abwaerts`** |

**Der zweite Endpunkt ist dazugekommen, und das ist keine Zugabe.** `…/kette/abwaerts` ist ein
eigener Weg mit eigener Vorprüfung; die alte Fassung hat ihn nie gemessen, weil eine zweite
Wanduhrmessung den Test nur noch unzuverlässiger gemacht hätte. Eine Zählung kostet nichts — teuer
wäre allein die Lücke gewesen.

> ### Der Zähler steht seit dieser Runde an einer Stelle, nicht an zweien
>
> `Zugriffszaehler` und `Zugriffszaehlung` sind aus `BamIsolationDbIT` herausgelöst und liegen jetzt
> als eigene Klassen neben `SicherheitsTestbasis` (beide ausschließlich in `src/test`). **Am
> Verhalten von `BamIsolationDbIT` ändert das nichts** — es ist derselbe Zähler, derselbe
> Anhängepunkt (`glassfishDsl`, angehängt statt ersetzt), dieselbe Vergleichsform.
>
> **Der Grund ist der, den [`prozess-katalog.md`](prozess-katalog.md) §9 für E23 nennt:** *eine
> Bedingung, die an zwei Stellen steht, driftet.* Bei einem Mechanismus, der in **beiden** Klassen
> die Sicherheitsaussage trägt, wäre eine Abschrift die schlechtere Hälfte von „unverändert
> übertragen".

**Damit gilt Regel T1 im gesamten Testbestand.** Die Suche aus §7 ist am 31.08.2026 wiederholt
worden — `System.currentTimeMillis`, `StopWatch`, `Thread.sleep`, `assertTimeout`, `@Timeout`:
**kein einziger Treffer** in `backend/src/test`.

> **`System.nanoTime` kommt weiterhin vor, und die Stelle gehört benannt** — eine falsche
> Vollständigkeitsaussage ist schlimmer als eine fehlende.
>
> | Wo | Was | Zusicherung? |
> |---|---|---|
> | `BamIsolationDbIT`, `KettenIsolationDbIT` | **Zitat im Klassen-Javadoc**: was dort bis zum 31.08.2026 stand | nein — Prosa |
> | `MessungM108DbIT` *(neu am 31.08.2026)* | die Laufzeiten des Dashboard-Endpunkts | **nein** — sie gehen ausschließlich über `melde(…)` nach `System.out`. Die einzige Zusicherung des Läufers ist ein Zählwert |
>
> **Die entscheidende Unterscheidung ist dieselbe wie in §7:** nicht, ob eine Zeit gemessen wird,
> sondern ob der Messwert in einer Zusicherung landet. `MessungM108DbIT` ist derselbe Fall wie
> `MessungM96DbIT` — ein Messläufer, kein Test.

**Die Lücke bleibt dieselbe und bleibt benannt:** T‑1 gilt jetzt für zwei Tests statt für einen.
Zwei gleich viele Zugriffe könnten verschieden lange dauern; ob das eine reale Lücke ist, ist
weiterhin nicht beantwortet.

### 9.3 Ein dritter Fund, und er stammt aus der Verletzungsprobe selbst

**Der Pflicht-Isolationstest des Dashboards war beim ersten Bau zur Hälfte zahnlos.** Er hielt die
Prozesskennungen des einen Mandanten gegen den Antwortrumpf des anderen — die Bauform, die bei
Liste, Detail, Kette und BAM trägt. **Mit ausgehängtem Mandantenfilter blieb er grün.**

> ### Der Grund ist strukturell und gilt für jedes Dashboard
>
> **Eine aggregierte Antwort trägt kaum Kennungen.** Der Verlauf besteht aus Zahlen, der
> Verteilungsblock zeigt Partnernamen. Die einzigen Kennungen stehen in „Zuletzt aufgefallen" —
> und die zehn Zeilen dort gehörten zufällig alle einem *dritten* Mandanten, gegen dessen Liste der
> Test gar nicht geprüft hat.
>
> **Das ist derselbe Mangel wie in §1, nur andersherum:** Dort hing eine Vorbedingung an
> veränderlichen Daten; hier hängt die *Aussage* daran, welche Zeilen zufällig oben stehen.

**Zwei Zusicherungen tragen den Nachweis jetzt, und beide fallen:**

| Zusicherung | Was sie zeigt | gemessen bei ausgehängtem Filter |
|---|---|---|
| `summen_sind_verschieden` | Zwei Mandanten sehen über dasselbe Fenster **verschiedene** Summen | beide sehen **12.004** — die Zahl des ganzen Bestands im 48‑Stunden‑Fenster (M95, P1) |
| `nur_eigene_prozesse_in_den_zeilen` | Jede **gezeigte** Prozesskennung steht in der **eigenen** Prozessliste | `40090_BMW_LAB_VDA` erscheint bei `VOTG` |

**Die zweite ist die allgemeinere.** Sie dreht die Frage um: statt *„steht hier eine Kennung des
einen bestimmten anderen Mandanten?"* fragt sie *„gehört jede Kennung, die hier steht, mir?"* —
und fällt damit unabhängig davon, wem die durchgerutschte Zeile gehört.

> **Gefunden hat den Mangel nicht das Nachdenken, sondern die Verletzungsprobe.** Sie steht in der
> Abnahme jedes Isolationstests, und dies ist der Lauf, der zeigt, wofür: Ein grüner Test, der seine
> Aussage nicht trägt, sieht von außen genauso aus wie einer, der sie trägt.

**Was das für die anderen Isolationstests heißt** — und es ist ausdrücklich **nicht** geprüft: Sie
liefern alle *Zeilen* und nicht *Zahlen*, ihre Kennungsvergleiche haben dort also Zähne. **Ob die
umgekehrte Fassung („gehört jede gezeigte Kennung mir?") auch dort schärfer wäre, ist offen** und
steht als Punkt **T‑6** in §6.
