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
