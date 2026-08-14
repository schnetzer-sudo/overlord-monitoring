# Die BAM-Suche

Entsteht in **Schritt 7, Teil 2b** (13.08.2026, Backend §1–§9), **Teil 3** (13.08.2026,
Oberfläche §10 bis §14) und **Teil 4** (14.08.2026, Präfixsuche §15 bis §21).

> ### 📌 Teil 4 — die Präfixsuche, 14.08.2026
>
> **Die Suche bleibt exakt.** Dazu **kommt** ein Parameter `modus=exakt|praefix` mit der Vorgabe
> `exakt`: Geht die exakte Suche leer aus, lässt sich die Suche über den **Anfang** des Werts
> anfordern. **Ohne den Parameter verhält sich der Endpunkt Zeichen für Zeichen wie vorher.**
>
> Die Entscheidung, ihre Begründung und der Deckel auf 30 Tage stehen in **§15 bis §21**; gemessen
> ist sie in **M50**. **Die Oberfläche ist nicht angefasst** — der Rückfall ist damit am Endpunkt
> vorhanden und noch nicht bedienbar (§13, Punkt 4).

> ### 📌 Teil 3 — die Oberfläche, 13.08.2026
>
> **Am Endpunkt aus Teil 2b ändert sich nichts.** Nicht an der Parameterform, nicht am
> Zeitfenster, nicht am Limit, nicht am Abbruchpfad; §1 bis §9 sind unangetastet. Dazu **kommt**
> ein zweiter, kleiner Endpunkt — `GET /api/bam/typen` (§10) —, weil die Auswahl neben dem
> Suchfeld sonst nichts anzubieten hätte. Er liest ausschließlich Stammdaten und ist in **M48**
> gemessen.
>
> Die Oberfläche steht in **§11** (Feld, Marken, Route, Trefferliste, Leerzustand), **§12** (die
> Sichtprüfung) und **§13** (die offenen Punkte, die sie hinterlässt). **§14** hält den einen
> Befund fest, den ein Nutzer als Fehler lesen wird und der keiner ist: die **Lieferschein-Suche
> ohne führende Null**.

Sie beantwortet die Frage, mit der der typische Nutzer dieses Werkzeug öffnet, in ihrer zweiten
Hälfte: **Wer eine Belegnummer hat, findet die Nachrichten, auf denen sie steht** — auch ohne die
führende Null, und auch mit mehreren Nummern gleichzeitig.

Nicht „welcher Beleg ist das" (das ist [`bam-werte.md`](bam-werte.md), Teil 1), nicht „wo steht mein
Beleg" im Sinne einer gefilterten Liste ([`nachrichtenliste.md`](nachrichtenliste.md)) und nicht „was
hängt daran" ([`verkettung.md`](verkettung.md)).

Grundlage sind die Erhebung [`messungen-schritt7.md`](messungen-schritt7.md) — **M32** bis **M46**,
**E6** — und die Messung **M47** gegen das **fertige** Statement. Die Kuratierung, ohne die die
Normalisierung kein Maß hätte, steht in [`bam-sollaengen.md`](bam-sollaengen.md).

> **Skills — für die beiden Teile verschieden, und beides ist geprüft.**
>
> **Teil 2b: keiner.** Installiert sind unter anderem `frontend-design`, `shadcn`, `dataviz`,
> `code-review`, `security-review`, `simplify`, `run` und `init`. Keiner davon ist eingebunden
> worden: Teil 2b fasst die Oberfläche nicht an, `frontend-design` und `shadcn/ui` sind für diesen
> Teil ausdrücklich ausgeschlossen, und die übrigen betreffen Werkzeuge und nicht dieses Feature.
>
> **Teil 3: `frontend-design` und `shadcn`, beide vorher als installiert geprüft.** Sie haben
> nichts entschieden, was die Projektdateien schon entscheiden — Farbrollen, Dichte, Umbruchpunkte
> und Bauformen stehen in [`visuelles-konzept.md`](visuelles-konzept.md) und
> [`frontend-grundlagen.md`](frontend-grundlagen.md), und bei Widerspruch gelten sie. Was aus
> `frontend-design` übrig blieb, ist die Haltung zur Sprache: **jedes Element tut genau eine
> Sache**, und ein leerer Zustand ist eine Einladung zu handeln und keine Stimmung. Aus `shadcn`
> kam **kein neuer Baustein** — `DropdownMenuRadioGroup`, `Input`, `Button` und `Table` lagen
> bereits im Generatorbereich.

> **Teil 1 und Teil 2b fassen dieselbe Tabelle von zwei Seiten an.** Teil 1 steigt über die
> `MessageID` ein — `ref` über das Präfix des Primärschlüssels, in diesem Projekt mehrfach gemessen.
> Teil 2b steigt über den **Wert** ein und trägt ganz andere Kosten: bis **234.159** Treffer auf
> einem einzigen Wert (M33), **10,6 s** ohne Zeitfenster (M34). Der Schnitt zwischen den Teilen war
> genau deshalb gesetzt, und diese Datei ist die riskante Hälfte.

---

## 1. Der Endpunkt

```
GET /api/bam/suche
```

Angemeldet, **Mandant aus der Sitzung** (Regel M1). Er nimmt **keine** Mandanten-ID entgegen; die
Ausnahmeliste in [`mandantentrennung.md`](mandantentrennung.md) §3 bleibt bei zwei Einträgen und
wächst hier nicht.

| Parameter | Form | Bedeutung |
|---|---|---|
| `begriff` | **wiederholt**, Format `<typ>:<wert>` | ein Suchbegriff. Der Doppelpunkt ist **Pflicht**; ohne Typ lautet er `:4711815`. Mindestens einer, höchstens **acht**. **Ein Komma im Wert ist Teil des Werts** (§22) |
| `von`, `bis` | ISO 8601 UTC | das Zeitfenster. Fehlen beide, gilt die Vorgabe von **30 Tagen** |

**Warum `/api/bam/suche` und nicht `/api/nachrichten/suche`.** Der Endpunkt beantwortet eine eigene
fachliche Frage und ist keine Spielart der Liste: kein Zeitraum-Kürzel, kein Status- und kein
Prozessfilter, kein Cursor, ein anderes Standardfenster. Unter `/api/nachrichten` sähe er aus wie
derselbe Endpunkt mit anderen Parametern. Der Belegdaten-Block aus Teil 1 liegt aus dem umgekehrten
Grund unter `/api/nachrichten`: Er beantwortet eine Frage **zu einer benannten Nachricht**.

### Warum der Doppelpunkt Pflicht ist

Ohne ihn müsste die Anwendung raten, ob eine führende Ziffernfolge ein Typ ist oder Teil des Werts.
Beides sind Zahlen: Die BAM-Typen heißen `0`, `1`, `2`, `2000`, `9018`, und die Werte sind nach M38
bei fast allen Typen rein numerisch.

**Ein BAM-Wert kann selbst einen Doppelpunkt enthalten — 585 tun es** *(gemessen am 13.08.2026,
M49‑4; bis dahin stand hier „ist nicht gemessen")*. Mit Pflichttrenner und Aufteilung am **ersten**
Doppelpunkt ist die Frage **gegenstandslos**, statt nach Regel Q4 beantwortet zu werden: Alles
hinter dem ersten Trenner ist Wert, einschließlich weiterer Doppelpunkte. Ein leerer Typteil
(`:4711815`) heißt „unter jedem Typ".

> **Die Messung bestätigt die Bauform, statt sie zu erübrigen.** Sie war als Vorsichtsmaßnahme gegen
> eine *unbekannte* Lage gebaut; die Lage ist jetzt bekannt und die Maßnahme deshalb nicht weniger,
> sondern **mehr** begründet. Wer am **letzten** Doppelpunkt teilte oder den Trenner freistellte,
> zerlegte diese 585 Werte falsch. Der Anteil ist klein (0,0038 % von 15.406.350), die Zahl ist es
> nicht.
>
> *Gemessen:* das **Vorkommen** des Zeichens über den gesamten Bestand (Vollerhebung).
> *Behauptet wird:* dass die Teilung am ersten Doppelpunkt notwendig ist.
> **Die Lücke:** Gemessen ist, dass solche Werte **existieren** — nicht, dass heute jemand nach
> einem von ihnen sucht, und nicht, dass die Produktion dieselben Werte trägt (Stand 08.07.2026).

### Höchstens acht Begriffe — Schutzgeländer, keine fachliche Grenze

Fachlich wäre keine nötig: M42‑2 misst **0,094 ms** je zusätzlichem Begriff, und M47 bestätigt es
gegen das gebaute Statement — zwei Begriffe **1,209 ms**, fünf Begriffe **1,622 ms**. Begrenzt wird
etwas anderes: die Zahl der **Join-Reihenfolgen**, die der Optimierer durchprobiert. Jeder Begriff
ist ein weiterer Selbstjoin auf `MessageBAM`, und diese Suche verlässt sich ausdrücklich darauf,
dass der Optimierer die Reihenfolge selbst wählt (§4).

> **Und die Zahl ist über fünf hinaus nicht gemessen.** M42 endet bei fünf Begriffen, M47 ebenso;
> acht ist die Zahl, die das Geländer trägt, und sie steht in keiner Messung. Deshalb ein Geländer
> und keine Zusage. Im Code: `BamSuchfilter.HOECHSTENS_BEGRIFFE`.

### Keine Mindestlänge — und das ist eine begründete Abweichung von Regel L5

Regel L5 verlangt „hartes Limit **und** Mindestlänge". Das harte Limit steht (§5); die Mindestlänge
**nicht**, und zwar aus zwei Messungen:

- **E6 benennt die Kostengröße, und sie ist nicht die Zeichenlänge, sondern die Trefferzahl.** Exakt,
  Präfix und selbst `enthält` unterscheiden sich bei *gleicher* Trefferzahl um Prozente; was die
  Laufzeit macht, ist allein die Zahl der Zeilen hinter dem Schlüssel.
- **M38 zeigt, dass eine feste Länge je Typ verschieden falsch wäre.** Die Werte sind zwischen **1
  und 35** Zeichen lang. Eine Mindestlänge von vier machte neun Typen unsuchbar (9015, 9016, 9017,
  9020, 9000, 9005, 9034, 9037, 9038 tragen Werte der Länge 1); eine von drei ließe beim gemessenen
  Prüfwert 264.469 Treffer zu.

**Was stattdessen trägt, ist gemessen:** das Zeitfenster (§2) und das harte Limit von 50 (§5). M47
misst die Gegenprobe an einer zweistelligen Eingabe — **20,2 ms** roh, **22,4 ms** mit fünf
Varianten. Eine kurze Eingabe ist also teuer, aber nicht gefährlich, solange das Fenster steht.

> **Belegvermerk** (Regel L10).
> *Gemessen (M47):* eine zweistellige Eingabe mit 2.256 globalen Treffern über 30 Tage bei `NEXANS`,
> in einer und in fünf Fassungen.
> *Behauptet wird:* dass eine Mindestlänge entbehrlich ist.
> **Die Lücke:** Gemessen ist **eine** kurze Eingabe. Die Zahl, die den Satz wirklich trüge, wäre die
> teuerste kurze Eingabe des Bestands — und die ist nicht erhoben. Was trägt: *Diese zweistellige
> Eingabe kostet mit fünf Varianten 22 ms.* Was **nicht** gemessen ist: *ob es eine zweistellige
> Eingabe gibt, die deutlich mehr kostet.* Die Obergrenze zieht auch dort das Zeitfenster, nicht die
> Länge.

### Antwortform

```json
{
  "nachrichten": [
    {
      "messageId": "…",
      "zeitpunkt": "2025-12-29T22:53:50Z",
      "status": "FINISHED",
      "statusKind": "ABGESCHLOSSEN",
      "bedeutungNichtVerifiziert": false,
      "processId": "…",
      "processName": "40000_AMG_LAB_VDA",
      "projectName": "300_KundenEingehend",
      "sosName": "Versand Einzel IDOC aus Split",
      "schritt": null,
      "rollen": ["SPLIT_WURZEL"],
      "treffer": [
        { "typ": 9012, "bezeichnung": "Charge_L_SAP", "wert": "00…" }
      ]
    }
  ],
  "begriffe": [
    { "eingabe": "…", "typ": null, "varianten": ["…", "00…"] }
  ],
  "von": "2025-11-29T23:00:00Z",
  "bis": "2025-12-29T23:00:00Z",
  "abgeschnitten": false
}
```

| Feld | Bedeutung |
|---|---|
| `nachrichten` | die Treffer, absteigend nach Zeitpunkt. **Immer vorhanden, leer statt fehlend** |
| die ersten zehn Felder je Zeile | **wortgleich mit `message/NachrichtResponse`** — wer aus der Suche heraus weiterarbeitet, sieht dieselbe Zeilengestalt wie aus der Liste |
| `rollen` | die Stellung in der Verkettung, **immer vorhanden, leer statt fehlend** |
| `treffer` | welcher Typ mit welchem Wert getroffen hat. Mehrere sind kein Randfall (M37) |
| `begriffe` | die Begriffe mit ihren **gesuchten Fassungen** — keine stille Korrektur (§3) |
| `von`, `bis` | das **tatsächlich verwendete** Zeitfenster, auch wenn es die Vorgabe war (§2) |
| `abgeschnitten` | ob es mehr Treffer gäbe. Gezählt **nach** dem Mandantenfilter (§5) |

**Warum `rollen` dabeisteht.** Weil die Suche fast immer die **Wurzel** findet: 96,87 Prozent der
Wurzeln tragen BAM-Werte, nur 2,42 Prozent der Kinder (M26‑1b). Ohne die Rolle wüsste der Nutzer
nicht, dass er das Bündel gefunden hat und nicht das Einzelstück. Und sie kostet **keinen**
zusätzlichen Zugriff (E4): Die vier Verkettungsspalten stehen auf der `Message`-Zeile, die die
Abfrage ohnehin liest.

**Warum `treffer` dabeisteht.** Der Nutzer hat den Wert getippt; was er **nicht** weiß, ist, worauf er
getroffen hat — ob seine Nummer als Lieferschein-Nr., als Charge oder als Kundenmaterialnummer auf
dieser Nachricht steht.

**Kein Sammelstatus über die Kette.** Er kostete je Trefferzeile eine Abwärtsauflösung und stünde bei
**11,38 Prozent** der Merge-Ergebnisse auf einer gedeckelten Menge (M30‑2) — also auf einer Zahl, die
nicht die ganze Wahrheit ist. Wer wissen will, was an der Nachricht hängt, öffnet sie.

### Fehlerfälle

Alle nach RFC 9457, alle mit eigenem `type` — die Oberfläche übersetzt anhand des `type`, nicht
anhand von `detail`.

| `type` | Status | Wann | neu? |
|---|---|---|---|
| `suchbegriff-fehlt` | 400 | kein brauchbarer Begriff angegeben | **neu** |
| `suchbegriff-ohne-typtrenner` | 400 | ein Begriff ohne Doppelpunkt | **neu** |
| `suchbegriff-typ-ungueltig` | 400 | der Typteil ist keine Typnummer | **neu** |
| `zu-viele-suchbegriffe` | 400 | mehr als acht Begriffe | **neu** |
| `zeitpunkt-ungueltig` | 400 | `von`/`bis` nicht als ISO 8601 lesbar | vorhanden |
| `zeitfenster-unvollstaendig` | 400 | nur `von` oder nur `bis` | vorhanden |
| `zeitfenster-ungueltig` | 400 | `bis` liegt vor `von` | vorhanden |
| `zeitfenster-zu-gross` | 400 | Spanne über einem Jahr | vorhanden |
| `suche-abgebrochen` | 400 | Statement in `max_statement_time` gelaufen | **vorhanden** (§6) |
| `kein-mandant-gewaehlt` | 403 | kein aktiver Mandant in der Sitzung | vorhanden |

**Vier neue Problemtypen, und alle vier betreffen die Parameterform.** Für Zeitfenster und Abbruch
entsteht **keiner**: Das sind dieselben Fälle wie in der Nachrichtenliste, und sie bekommen denselben
Schlüssel.

**Ein Treffer, den es nicht gibt, ist `200` mit leerer Liste** — kein `404`. Das ist nicht dieselbe
Lage wie bei Detail, Kette und Belegdaten: Dort benennt der Pfad eine Ressource, hier stellt der
Aufrufer eine Frage. Was die Ununterscheidbarkeit hier trägt, steht in §7.

---

## 2. Das Pflicht-Zeitfenster — 30 Tage statt 24 Stunden

Regel L1 verlangt an jedem Listen-Endpunkt ein Zeitfenster mit Vorgabe **24 Stunden** und Maximum
**ein Jahr**. Das Maximum bleibt unverändert. **Die Vorgabe weicht ab, und die Abweichung ist
gemessen** (M35, bestätigt in M47 gegen das gebaute Statement):

| Fenster | typischer Wert (1 Treffer) | schlimmster Wert (234.159 Treffer) | Treffer des schlimmsten |
|---|---:|---:|---:|
| ohne *(M35, nicht gebaut)* | 0,729 ms | **10.752,8 ms** — Grenze gerissen | 234.159 |
| 24 Stunden *(M35)* | 0,757 ms | 90,5 ms | 279 |
| **30 Tage** *(M47, gebaut)* | **1,095 ms** | **1.655,8 ms** | — |
| **ein Jahr** *(M47, gebaut)* | **1,089 ms** | **8.939,7 ms** | — |

**Kein Standard von 24 Stunden.** Der Nutzer mit einer Belegnummer hat **kein Datum**. Ein
Tagesfenster fände beim schlimmsten Wert **279 von 234.159** Nachrichten — es verändert also nicht
den Preis, sondern die **Antwort**, und zwar ohne dass der Nutzer es merkte.

**Kein offenes Fenster.** Ohne Fenster reißt derselbe Wert die Zeitgrenze des Lese-Pools
(`max_statement_time=10`, [`datenzugriff.md`](datenzugriff.md) §1).

**Warum 30 Tage die richtige Mitte sind.** Sie lassen gegen die 10-Sekunden-Grenze **Faktor 6**;
ein Jahr lässt **12 Prozent**. Wer die Vorgabe weit setzt, verlagert den Schutz vollständig auf den
Abbruchpfad.

**Das verwendete Fenster steht in der Antwort**, nicht nur in der Anfrage. Es ist die eine Angabe,
ohne die eine leere Trefferliste nicht zu deuten ist.

**Die Obergrenze wird nicht neu erfunden.** Der absolute Zweig ist derselbe wie in der
Nachrichtenliste — `common/Zeitfenster.mitVorgabe` ruft dieselbe Prüfung auf wie
`Zeitfenster.aufloesen`, samt `zeitfenster-unvollstaendig`, `zeitfenster-ungueltig` und
`zeitfenster-zu-gross`. **Es wird nichts gekappt:** Ein zu großes Fenster ist `400` und keine
stillschweigend verkleinerte Antwort.

> **Warum `mitVorgabe` und nicht die vorhandene Methode.** `Zeitfenster.VORGABE` sind 24 Stunden, und
> das ist die richtige Vorgabe für eine *Liste*: Wer sie öffnet, will wissen, was gerade läuft. Die
> Suche stellt eine andere Frage. Die Vorgabe ist deshalb ein **Parameter** der Methode und keine
> zweite Konstante in `common`; welche Zahl sie trägt, entscheidet der Endpunkt und begründet sie
> hier.

---

## 3. Die Normalisierung

**Die Varianten entstehen vor dem Statement, nicht darin.** `bam_sollaenge` wird **nie** gegen
`GlassfishDB` gejoint ([`bam-sollaengen.md`](bam-sollaengen.md) §6); die Tabelle hat sechzehn Zeilen,
ein Vollabzug für den Mandanten der Sitzung ist der richtige Zugriff und kostet **0,336 ms** (M47).

Je Begriff entsteht eine Menge von Werten, die gemeinsam über `IN` gesucht werden:

1. **Der rohe Wert**, an den Rändern beschnitten.
2. **Die aufgefüllten Fassungen.** *Mit* Typ die Sollänge genau dieses Paares aus `(mandant_id,
   message_bam_type)`; *ohne* Typ alle für diesen Mandanten kuratierten **verschiedenen** Sollängen.
3. **Die Fassung mit führendem Leerzeichen**, für die Paare mit gesetztem Kennzeichen.

Im Code: `bam/Sollaengen` — reine Rechenlogik, ohne Datenbankzugriff und deshalb ohne Datenbank
prüfbar (`SollaengenTest`).

### Warum überhaupt aufgefüllt wird

Der Nutzer tippt die Nummer vom Beleg ab, und führende Nullen stehen dort nicht. M43‑4 misst die
Folge an echten Werten: Bei **sechs von acht** geprüften Typen findet die rohe Fassung **null**
Treffer und die aufgefüllte die richtigen. Nicht „weniger" — **keine**. Und eine leere Trefferliste
sieht aus wie „gibt es nicht", nicht wie „falsch getippt".

**M47 reproduziert das an einem eigenen Prüfwert**, hergeleitet aus einem kuratierten Paar mit 100 %
Längendominanz und 100 % führender Null: Der Kern ohne die Nullen hat **null** globale Treffer, die
auf die Sollänge aufgefüllte Fassung **zwei**.

### Die drei Regeln, die die Variantenmenge klein halten

| Regel | Wirkung, gemessen |
|---|---|
| **Aus den verschiedenen Sollängen bilden, nicht je Typ** | `NEXANS` hat **zehn** kuratierte Zeilen, davon **acht** mit einer Sollänge — aber nur **drei verschiedene**: 10, 4 und 3 (M47) |
| **Nur auffüllen, nie kürzen** | Sollängen, die nicht größer sind als die Eingabe, entfallen |
| **Doppelte entfernen** | Erzeugen zwei Regeln denselben Wert, steht er einmal in der Liste |

**Wie viele Varianten das tatsächlich sind, ist belegt und nicht gerechnet** (M47, Abnahmepunkt 4):
Bei `NEXANS` — dem Mandanten mit den meisten kuratierten Zeilen — ist die **Obergrenze fünf**
gesuchte Fassungen, und die erreicht nur eine Eingabe von **einem oder zwei** Zeichen. Für eine
siebenstellige Eingabe bleiben **drei** (roh, auf 10 aufgefüllt, mit Leerzeichen), für eine
zehnstellige **zwei**.

> **Der Auftrag zu Teil 2b nennt „elf Sollängen" bei `NEXANS`.** Gezählt sind es **acht** auf zehn
> Zeilen. Die Zahl ändert am Schluss nichts — es kommt auf die *verschiedenen* an, und das sind drei
> —, sie steht hier, weil sie in der Aufgabenstellung anders steht.

### Warum nur aufgefüllt und nie gekürzt wird

Auffüllen macht einen Wert länger und damit **spezifischer**. M43‑4 zeigt die Gegenrichtung: Bei
9036 und 9006 fand die **rohe**, kurze Fassung **2.371** beziehungsweise **1.642** Treffer statt der
800 und 4 richtigen — kurze Kerne kommen unter anderen Typen vielfach vor. Die kurze Fassung ist dort
nicht die großzügigere, sondern die unbrauchbare.

### `sollaenge` ist `NULL`-fähig — und das ist kein Fehlerfall

Zwei der sechzehn Zeilen tragen ausschließlich das Leerzeichen-Kennzeichen und **keine** Sollänge:
`NEXANS`/9018 und `NEXANS`/9020, deren Längendominanz bei 43,61 % und 82,31 % liegt und damit weit
unter der Schwelle. **Existiert für ein Paar keine Zeile oder ist die Sollänge `NULL`, wird für
diesen Typ nur roh gesucht.**

Das trifft ausgerechnet den naheliegendsten Suchtyp: **9018 sitzt bei `NEXANS` auf 92,26 Prozent der
Wurzeln** (M39) und trägt keine Sollänge.

**Der generierte Typ von `sollaenge` ist `org.jooq.types.UByte`** — der Preis für `TINYINT UNSIGNED`;
die Zahl kommt über `.intValue()`.

### Die Varianten stehen in der Antwort — bis auf eine

**Keine stille Korrektur.** Wer `4711815` tippt und `004711815` findet, muss erfahren, warum:
*„gesucht nach 4711815 und 004711815."* Ohne diese Angabe sähe die Trefferliste aus, als hätte die
Datenbank etwas anderes enthalten als sie enthält — und wer die Nummer anschließend im Altwerkzeug
nachschlägt, fände sie dort nicht.

**Ausnahme: die Leerzeichen-Fassung wird gesucht und nicht gemeldet.** Ein führendes Leerzeichen
sieht der Nutzer weder in seiner Eingabe noch im angezeigten Wert; eine Zeile *„gesucht nach 4711 und
␣4711"* läse sich wie ein Anzeigefehler. Die führende Null ist das Gegenteil — sie steht auf dem
Beleg nicht, im Bestand aber sichtbar, und die Meldung erklärt dem Nutzer genau den Unterschied.
Wohin das Leerzeichen gehört, ist die **Hilfe zur Suche**; sie entsteht in Teil 3.

### Folgende Leerzeichen werden nicht behandelt — und die Kehrseite dazu

`utf8mb4_general_ci` ist eine **PAD SPACE**-Kollation: `'a ' = 'a'` ist wahr, der `=`-Vergleich
ignoriert folgende Leerzeichen. M43‑3 belegt es zweimal — am Ausdruck und an echten Werten (12 Treffer
mit wie ohne Leerzeichen). **Hier ist nichts nachzurüsten.**

> ⚠️ **Die Kehrseite gehört dazu, damit sie niemand übersieht:** `LIKE` folgt der PAD-SPACE-Regel
> **nicht** (`'a' LIKE 'a '` ist falsch, M43‑3). Würde je präfixweise gesucht, verhielten sich
> folgende Leerzeichen anders als hier — und was heute folgenlos ist, wäre es dann nicht mehr. Die
> Präfixsuche steht als offener Punkt in §9.

### Zwei kuratierte Einträge finden nachweislich nichts

`IBIS`/1 und `SUTTONS`/2000: Bei beiden liegt im ganzen Bestand kein Wert **mit** führender Null auf
der Sollänge (M46‑1c, M46‑2c). **Sie werden hier nicht ausgeschlossen und nicht ausgebessert** — die
Befüllung der Kuratierung ist mechanisch, und ob die Regel um eine Wirksamkeitsbedingung ergänzt
wird, ist eine Entscheidung des Auftraggebers ([`bam-sollaengen.md`](bam-sollaengen.md) §8).

**Was sie kosten, ist jetzt gemessen** und war es bis M47 nicht: eine zusätzliche Variante je Suche
auf diesen beiden Typen. Der Sprung von einer auf zwei Varianten kostet an einem Wert mit wenigen
Treffern **0,175 ms** (0,956 → 1,131 ms), von einer auf fünf an einem Wert mit 2.256 Treffern
**2,2 ms** (20,228 → 22,439 ms). **Der Preis einer wirkungslosen Variante liegt damit im Bereich von
Zehntelmillisekunden** und ist kein Argument in der offenen Frage — die Frage ist, ob die Regel
sagen soll, was sie meint.

---

## 4. Die Abfrageform

**Selbstjoin auf `MessageBAM` über die `MessageID`**, ein zusätzlicher Join je weiterem Begriff. Der
Kern liefert höchstens 51 Zeilen; die Anzeigespalten hängen **darüber**:

```sql
SELECT treffer.MessageID, …, p.ProcessName, prj.ProjectName, s.SOSName, sa.SOSActionName,
       treffer.Source, treffer.SourceMessageID, treffer.TargetMessageID, treffer.Target
FROM (
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, m.SOSID, m.SOSActionID,
         m.Source, m.SourceMessageID, m.TargetMessageID, m.Target
  FROM MessageBAM b1
  JOIN MessageBAM b2 ON b2.MessageID = b1.MessageID
  JOIN Message m     ON m.MessageID  = b1.MessageID
  WHERE b1.MessageBAMValue IN (…Varianten des ersten Begriffs…)
    AND b2.MessageBAMValue IN (…Varianten des zweiten…)
    AND m.MessageLastUpdate >= ? AND m.MessageLastUpdate <= ?
    AND EXISTS (SELECT 1 FROM Process mp JOIN ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ?)
  GROUP BY m.MessageID
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  FETCH NEXT 51 ROWS ONLY) AS treffer
LEFT JOIN Process    p   ON p.ProcessID   = treffer.ProcessID
LEFT JOIN Project    prj ON prj.ProjectID = p.ProjectID
LEFT JOIN SOS        s   ON s.SOSID       = treffer.SOSID
LEFT JOIN SOSAction  sa  ON sa.SOSID      = treffer.SOSID AND sa.SOSActionID = treffer.SOSActionID
ORDER BY treffer.MessageLastUpdate DESC, treffer.MessageID DESC
```

*Die Deckelung rendert jOOQ für MariaDB als `FETCH NEXT … ROWS ONLY`; das ist eine Sache des
Dialekts, die Zahl ist die Zusage.*

### Kein `STRAIGHT_JOIN` — gemessen, und gegen die ursprüngliche Erwartung

Der Optimierer steigt in **jeder** gemessenen Konstellation über den **selteneren** Begriff ein und
tauscht die Tabellen selbst. M47 belegt es am gebauten Statement:

| Fall | führende Tabelle im `EXPLAIN` | `rows` | zweiter Begriff |
|---|---|---:|---|
| zwei Begriffe, seltener **zuerst** geschrieben | `b1` (der seltene) | 1 | `ref`, `key_len` **428**, `rows` 1 |
| zwei Begriffe, seltener **zuletzt** geschrieben | **`b2`** (der seltene) | 1 | derselbe Plan |
| fünf Begriffe, seltener an **fünfter** Stelle | **`b5`** | 1 | vier × `ref`, `key_len` 428, `rows` 1 |

**Der `key_len` 428 ist der Punkt** (schon M42‑1): `MessageBAM_BAMValueOnly` ist als
`KEY (MessageBAMValue)` angelegt, trägt in InnoDB aber den Primärschlüssel mit — der Zugriff läuft
als `ref (MessageBAMValue = const, MessageID = …)` über 282 + 146 Bytes und liefert **genau eine**
Zeile.

**Mit `STRAIGHT_JOIN` wäre die Reihenfolge bindend** und die Eingabereihenfolge des Nutzers eine
Leistungsfrage: gemessen Faktor **219** bei zwei und **1.094** bei fünf Begriffen (M42‑1, M42‑2). Er
steht in der Nachrichtenliste, wo er eine andere Lage löste (L15) — **das ist kein Grund, ihn hier
nachzurüsten.** `BamSucheStatementsTest.kein_straight_join` hält es fest.

**M47 misst die Gegenprobe an der Laufzeit:** 1,209 ms gegen 1,202 ms, je nachdem, welcher der beiden
Begriffe zuerst steht — und 1,622 gegen 1,607 ms bei fünf. **Die Eingabereihenfolge ist folgenlos.**

### Nicht die Bauform `IN` plus `HAVING COUNT(DISTINCT …)`

Sie kommt ohne n Joins aus und ist die, die man zuerst schreibt. Sie fällt aber in vier von acht
gemessenen Konstellationen ab, im Normalfall um Faktor **873** (K2) bis **954** (fünf Begriffe); der
Selbstjoin verliert nur in einer (K5c), und beide schlechten Fälle liegen bei rund 900 ms (M42‑3).

**Die beiden Bauformen sind gegenläufig, nicht gestuft:** Die eine bezahlt *Trefferzahl des seltensten
Begriffs × BAM-Werte je Kandidatennachricht*, die andere die *Summe der Trefferzahlen aller Begriffe*.
Eine Wahl je Lage bräuchte eine Größe, die der Endpunkt vorab nicht kennt.

### Erst deckeln, dann beschriften

`Process`, `Project`, `SOS` und `SOSAction` hängen **über** der abgeleiteten Tabelle und nicht daneben.
Das ist derselbe Befund wie in Teil 1 ([`bam-werte.md`](bam-werte.md) §4) — und **M47 misst ihn hier
zum zweiten Mal**, weil er beim nächsten Umbau sofort wieder entstünde:

| Fassung, schlimmster Wert, 30 Tage | Laufzeit |
|---|---:|
| **gebaut** (Joins über der Deckelung) | **1.655,8 ms** |
| Gegenform (Joins neben `MessageBAM`, flach) | **2.443,3 ms** |
| Faktor | **1,48** |

**Und ihr `EXPLAIN` sieht genauso gut aus** — `b1` als `ref` mit `Using index`, danach `m`, `p2`,
`prj`, `mp`, `pm`, `s`, `sa` allesamt `eq_ref` auf `PRIMARY`. Genau wie in Teil 1: Der Plan verrät den
Unterschied nicht, weil er nicht sagt, **wie oft** eine Zeile angefasst wird. Hier sind es 234.159
Kandidatenzeilen statt 51. `BamSucheStatementsTest.erst_deckeln_dann_beschriften` hält die Gestalt
fest.

### Die Typangabe ist Ergebnisverfeinerung, keine Entlastung

Ist ein Typ angegeben, kommt `AND bN.MessageBAMType = ?` dazu. **M36 misst, dass das nicht
beschleunigt** (+1,5 bis +4 %), M47 bestätigt es am gebauten Statement (1,131 ms ohne Typ gegen
1,187 ms mit). Sie darf deshalb **nirgends als Entlastung vorausgesetzt** werden — insbesondere darf
keine Grenze mit dem Argument gelockert werden, es sei ja ein Typ gewählt.

### Verdichtung auf `MessageID`

M37 misst, dass bei **4,17 Prozent** der Paare `(MessageID, MessageBAMValue)` derselbe Wert unter
mehreren Typen steht — nie unter mehr als vier. Ohne die Verdichtung stünde dieselbe Nachricht
mehrfach in der Liste. Sie kostet nichts: `GROUP BY` und `ORDER BY … DESC` legen ohnehin eine
temporäre Tabelle an.

> ⚠️ **Das `GROUP BY` steht auf dem Primärschlüssel, die übrigen Spalten hängen funktional daran.**
> Das ist zulässig, solange `@@sql_mode` kein `ONLY_FULL_GROUP_BY` führt — **gemessen** in M47:
> `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION`.
> Dieselbe Form verwenden alle Statements aus M34, M35 und M42. Wer die Einstellung ändert, ändert
> dieses Statement mit; der Punkt steht in §9.

### Zwei Abfragen, wie in Teil 1

Die erste liefert höchstens 51 `MessageID`s und die Listenfelder; die zweite holt für **genau diese**
Nachrichten, welcher Typ mit welchem Wert getroffen hat. **Kein `GROUP_CONCAT`.**

Die Bedingung ist je Begriff dieselbe wie in der ersten Abfrage — **verodert statt verundet**. Damit
stehen dort genau die Zeilen, die die Nachricht zum Treffer gemacht haben: Wer `9012:123` sucht,
bekommt nicht die `123`, die auf derselben Nachricht unter 9018 steht.

Der Zugriff läuft über die Kennungen und kostet **0,650 ms** (M47): `Message` als `range` über
`PRIMARY`, `MessageBAM` als **`ref` über `MessageBAM_MessageFK` mit `Using index`** — die Tabelle
selbst wird nicht angefasst.

---

## 5. Das Limit und die Abschneidung

**Hartes Limit 50**, erkannt über die 51. Zeile. **Kein Cursor, kein Nachladen.**

Ein Cursor wäre hier zudem nicht dieselbe Zusage wie in der Liste: Die Sortierung ist der Zeitpunkt,
die Auswahl aber ein Indexzugriff über den Wert. Wer mehr sehen will, verengt den Zeitraum oder nennt
eine zweite Nummer — und **die zweite Nummer ist die billigere Verengung**: Die Verundung mit dem
schlimmsten Wert der Runde kostet 1,209 ms statt 1.655,8 ms.

### Die Abschneidung wird nach dem Mandantenfilter gezählt

Der Mandantenfilter steht **im** Statement (Regel M3), die 51. Zeile ist also bereits gefiltert. Eine
Meldung auf Basis der Rohtreffer sagte einem Nutzer etwas über die **Datenmenge fremder Mandanten** —
genau die Sorte Leck, gegen die die 404-Regel beim Mandantenwechsel gebaut ist.

`BamSucheIsolationDbIT.abschneidung_wird_nach_dem_mandantenfilter_gezaehlt` prüft es an einem echten
Wert, der bei `NEXANS` an einem Tag auf mehr als 50 Nachrichten steht: **dieselbe Suche, zwei
Mandanten, zwei verschiedene Zahlen** — 50 Treffer und `abgeschnitten: true` für den einen, null
Treffer und `abgeschnitten: false` für den anderen.

---

## 6. Wenn das Statement in die Zeitgrenze läuft

Das Zeitfenster macht den Abbruch unwahrscheinlich, nicht unmöglich. **Alle Zahlen stammen von einer
ruhenden Testkopie**, und der Jahresfall liegt dort bereits bei **89 Prozent** der 10-Sekunden-Grenze
des Lese-Pools. Unter Last läuft derselbe Fall wieder auf.

**Es entsteht kein zweiter Problemtyp.** `suche-abgebrochen` ist der, den die Nachrichtenliste seit
Schritt 4 trägt, mit demselben Status und demselben Handlungshinweis. Gefangen wird genau **eine**
Ausnahme: MariaDB meldet Fehler `1969` mit SQLState `70100`, Connector/J 3.5 macht daraus eine
`SQLTimeoutException`, und jOOQ verpackt sie in eine `DataAccessException` mit genau dieser Ursache.
Ein Syntaxfehler, ein Verbindungsabriss oder ein fehlendes Recht bleiben, was sie sind: technische
Fehler mit `500`.

**Zwei Unterschiede zur Nachrichtenliste, beide bewusst:**

1. **Ohne Bedingung.** Dort gilt der Pfad nur bei gesetztem Suchbegriff, weil die Liste auch ohne
   einen aufgerufen wird. **Diesen Endpunkt gibt es ohne Suchbegriff nicht** — jeder Aufruf trägt
   mindestens einen.
2. **Der Text nennt die andere Abhilfe.** „Verkleinere den Zeitraum oder **nenne eine zweite
   Belegnummer**" statt „schärfe den Suchbegriff": Ein BAM-Wert lässt sich nicht schärfen, ein
   zweiter Begriff senkt die Laufzeit dagegen um Größenordnungen (M42‑1).

> **Die Übersetzung steht ein zweites Mal im Code** (`BamSucheRepository.anDerZeitgrenze` neben
> `NachrichtenRepository.anDerZeitgrenze`), und das ist die Paketregel und keine Nachlässigkeit: Ein
> Fachpaket importiert nicht aus einem Nachbarpaket. Dieselbe Abwägung wie bei der Existenzprüfung in
> [`bam-werte.md`](bam-werte.md) §10 — **wandert sie nach `common`, wandert sie mit ihrem Test.**
> Steht als offener Punkt in §9.

---

## 7. Mandantentrennung

**Der Filter ist Bestandteil beider Statements auf dem Quellschema** (Regel M3), als `EXISTS` über
`Process → ProjectMandant` in genau der Form, die Liste, Detail, Kette und Belegdaten verwenden:

```sql
AND EXISTS (SELECT 1 FROM Process p
            JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
            WHERE p.ProcessID = m.ProcessID AND pm.MandantID = :mandant)
```

**Auch die zweite Abfrage trägt ihn**, obwohl ihre `MessageID`s aus der ersten stammen: Eine Menge,
die einmal gefiltert war, bleibt es nicht dadurch, dass jemand es weiß.

**Als `EXISTS` und nicht als Join**, weil `ProjectMandant` im Schema n:m ist. **Hier hätte ein Join
eine zweite Folge:** Er säße im Kern, also *vor* dem Limit — die Vervielfachung fräße Plätze der
Trefferliste, und die Abschneidung meldete zu früh „es gibt mehr".

**Der Vollabzug der Kuratierung filtert über `mandant_id`** und fasst `GlassfishDB` nicht an. Er
liest damit ausschließlich die Sollängen des aktiven Mandanten — die Sollängen bestimmen, *wonach*
gesucht wird, und eine Methode ohne Mandanten läse hier die Kuratierung fremder Mandanten mit.
`BamSucheRepository.findeSollaengen` trägt `MandantContext` deshalb als ersten Pflichtparameter
(Regel M2), obwohl ArchUnit die Regel nur für `jooq.glassfish` erzwingt.

### Der Isolationstest (Regel M4)

`BamSucheIsolationDbIT`, Paarung **`NEXANS` gegen `SUTTONS`** — zwei Mandanten aus verschiedenen
Häusern und die beiden mit dem größten Bestand, also die Paarung, bei der ein Leck am ehesten sichtbar
würde. Dieselbe Wahl wie in `NachrichtenIsolationDbIT`.

**Hier sieht die Gegenprobe anders aus als bei den Pfad-Endpunkten, und das ist ihre Übertragung und
kein Abweichen von der Vorlage.** Bei Detail, Kette und Belegdaten steht die Kennung *im Pfad*, und
verglichen werden zwei `404`-Rümpfe. **Hier gibt es kein `404`**: Eine Suche, die nichts findet, ist
`200` mit leerer Liste — genau wie eine Suche nach einem Wert, den es nicht gibt. Die
Ununterscheidbarkeit verschiebt sich damit vom **Statuscode** auf den **Rumpf**.

**Und der Rumpf zitiert die Frage.** Er nennt die Begriffe samt ihrer Varianten, weil es keine stille
Korrektur geben soll (§3). Das ist dieselbe Lage wie beim Feld `instance` nach RFC 9457: eine
Spiegelung der Eingabe und keine Auskunft über den Bestand. Der Test behandelt sie genauso — er
normalisiert das Zitat **und** weist zusätzlich nach, dass es genau die gesendete Eingabe ist und
dass jede Variante auf ihr endet.

**Die Varianten hängen ausschließlich am Mandanten der Sitzung**, nie am Mandanten des gesuchten
Werts. Damit ein Unterschied in der *Zahl* der Varianten den Vergleich nicht stört, ist die erfundene
Eingabe **genauso lang** wie die fremde.

Geprüft wird:

1. Beide Mandanten haben im Fenster BAM-Werte und finden ihre **eigenen**.
2. Ein **fremder, echter** Wert liefert `200` mit **leerer** Liste — und keine fremde Kennung, keinen
   fremden Prozess im Rumpf.
3. **Die Gegenprobe:** Ein fremder echter und ein erfundener Wert derselben Länge liefern
   **ununterscheidbare** Rümpfe (ohne `traceId`, mit normalisiertem Zitat).
4. Das Zitat bleibt ein Zitat — Zeichen für Zeichen die gesendete Eingabe.
5. Die Trennung gilt in **beide** Richtungen.
6. **Die Abschneidung wird nach dem Mandantenfilter gezählt** (§5).
7. Ohne aktiven Mandanten `403`, auch für ADMIN.
8. **Die schärfere Gegenprobe über den Mandantenwechsel:** als ADMIN zu `SUTTONS` wechseln, dort
   einen Wert holen und nachweisen, dass er *dort* gefunden wird, zurückwechseln, denselben Wert
   suchen — leere Liste, ununterscheidbar von der erfundenen Eingabe.

**Das Zeitfenster ist absolut** (Fenster B, 30.11. bis 30.12.2025). Außer `NEXANS` endet jeder Mandant
am 30.12.2025 (M3); in einem relativen Fenster sähe `SUTTONS` je nach Datenstand null Zeilen, und der
Test bewiese nur, dass leer leer ist.

**Die Gegenprobe ist gelaufen** — am 13.08.2026, mit von Hand entferntem `EXISTS` im Kern der ersten
Abfrage:

| Verfälschung von Hand | Ergebnis |
|---|---|
| Mandantenfilter aus dem Kern entfernt | **rot: 4 von 7** — `fremd_und_erfunden_sind_ununterscheidbar` („die fremde Nachricht ist unerreichbar"), `trennung_gilt_in_beide_richtungen`, `gegenprobe_ueber_den_mandantenwechsel` und `abschneidung_wird_nach_dem_mandantenfilter_gezaehlt` („derselbe Wert gehoert SUTTONS nicht") |
| zurückgesetzt | grün, 7 Tests |

**Dass die Abschneidungsprüfung mitfällt, ist der Punkt und kein Beifang:** Sie ist der einzige der
vier Fälle, in dem kein einziger fremder Wert herausgegeben wird — verraten würde nur die **Zahl**.

**Die Vorlage in [`mandantentrennung.md`](mandantentrennung.md) §5 wird ergänzt** — der Fall ist der
erste, bei dem die Gegenprobe ohne `404` auskommen muss.

---

## 8. Aufbau im Code

```
bam/
├─ BamSucheController.java     REST, nimmt nie eine Mandanten-ID
├─ BamSuchfilter.java          geprüfte Parameter: Begriffe und Zeitfenster
├─ Suchbegriff.java            <typ>:<wert>, geteilt am ERSTEN Doppelpunkt
├─ Sollaengen.java             die Kuratierung EINES Mandanten + die Variantenbildung
├─ Varianten.java              gesucht gegen gemeldet
├─ Suchbedingung.java          ein Begriff, wie das Statement ihn sieht
├─ BamSucheService.java        normalisieren, deckeln, beschriften
├─ BamSucheRepository.java     die drei Statements, je mit Mandantenfilter
├─ BamSollaengeZeile.java      eine Zeile aus bam_sollaenge
├─ BamTrefferZeile.java        eine Zeile aus Abfrage (a)
├─ BamTrefferWertZeile.java    eine Zeile aus Abfrage (b)
├─ Typbezeichnung.java         die Beschriftungsregel — geteilt mit Teil 1
└─ BamSucheResponse.java / BamTrefferResponse.java / BamBegriffResponse.java
   / BamTrefferWertResponse.java
```

**`common/Zeitfenster` bekommt `mitVorgabe(…)`** — eine additive Ergänzung für Endpunkte, die nur den
absoluten Modus anbieten und eine eigene Vorgabe haben (§2). An der Nachrichtenliste ändert sich
nichts.

**`Typbezeichnung` ist aus `BamService` herausgezogen** und wird jetzt von beiden Teilen benutzt. Der
Grund ist die Drift: Zwei Nachbauten derselben Regel beschrifteten denselben fehlenden Typ irgendwann
verschieden. Sie bleibt in `bam` und wandert ausdrücklich **nicht** nach `common` — dorthin gehört,
was ein *zweites Fachpaket* braucht.

### Tests

| Datei | Was, und ob mit Datenbank |
|---|---|
| `SollaengenTest` | **ohne DB** — die Normalisierung: Sollänge je Paar, **je Mandant** (2000 bei `SUTTONS` gegen `VOTG`), `sollaenge IS NULL`, „nur auffüllen, nie kürzen", verschiedene statt aller Sollängen, die Leerzeichen-Fassung gesucht aber nicht gemeldet, kein kuratierter Eintrag → nur roh |
| `BamSuchfilterTest` | **ohne DB** — der Pflichttrenner, die Teilung am ersten Doppelpunkt, der ungültige Typteil, „mindestens einer", **keine** Mindestlänge, acht gegen neun Begriffe, die Vorgabe von 30 Tagen, das Fenstermaximum |
| `BamSucheStatementsTest` | **ohne DB** — Mandantenfilter in **jedem** Statement auf dem Quellschema, `bam_sollaenge` **nie** gegen `GlassfishDB` gejoint, **kein** `STRAIGHT_JOIN`, kein `GROUP_CONCAT`, je Begriff ein Selbstjoin (kein `HAVING`), Verdichtung, Zeitfenster, Limit im Statement, **erst deckeln dann beschriften**, Typbedingung nur mit Typ, keine Präfixsuche |
| `BamSucheServiceTest` | **ohne DB** — die Varianten in der Antwort, die Leerzeichen-Fassung nur in der Suche, die Abschneidung samt Grenzfall *genau 50*, die Verdichtung (ein Wert unter zwei Typen → **eine** Zeile mit zwei Treffern), Beschriftung ohne Beschreibung, Rollen aus den vier Spalten, der Schritt nur bei offenen Status, das Fenster in der Antwort |
| `BamSucheDbIT` | `@Tag("db")` — die Suche **ohne führende Null findet die Nachricht** und die Antwort nennt die aufgefüllte Fassung, die vollständige Trefferzeile, das Fenster in der Antwort, die Verundung als **Eigenschaft** (jede Zeile trägt beide Werte), der Wert ohne Treffer, und die Parametergrenzen am laufenden Endpunkt |
| `BamSucheIsolationDbIT` | `@Tag("db")` — **der Pflicht-Isolationstest** (Regel M4), acht Fälle, §7 |

**Kein Prüfwert steht in einer Testdatei** (Regel G1). Jeder wird zur Laufzeit aus der Kuratierung
und dem Bestand **hergeleitet**, nach seiner Gestalt — „ein Wert eines kuratierten Paares, der auf
der Sollänge liegt und mit einer Null beginnt". Findet die Herleitung nichts, wird der Test rot und
sagt, dass sich der Bestand geändert hat.

---

## 9. Regelbezug und offene Punkte

### Regelbezug

| Regel | Wie umgesetzt |
|---|---|
| **M1** | Kein Parameter für den Mandanten; er kommt über `MandantService.aktuellerKontext` aus der Sitzung |
| **M2** | `MandantContext` erster Pflichtparameter **jeder** Repository-Methode — auch der auf `bam_sollaenge` |
| **M3** | `EXISTS` über `ProjectMandant` in beiden Statements auf dem Quellschema |
| **M4** | `BamSucheIsolationDbIT` |
| **M5** | Die Trennung gilt auch für die Abschneidung (§5) |
| **S1** | Ausschließlich `SELECT`; die Messung fasst `GlassfishDB` nur lesend an |
| **L1** | Pflicht-Zeitfenster. **Vorgabe 30 Tage statt 24 Stunden** — begründet in §2 |
| **L2** | Kein `total`, keine Aggregation über `Message` |
| **L3** | Kein `OFFSET` — und hier auch kein Cursor (§5) |
| **L4** | `MessageProperty` bleibt unberührt |
| **L5** | Hartes Limit **ja**, Mindestlänge **nein** — begründet in §1 |
| **L7** | M47 über `NEXANS` und `IBIS`, mit `EXPLAIN` und Laufzeit |
| **L10** | Belegvermerke in §1 und in M47 |
| **L15** | Die Einstiegstabelle ist im `EXPLAIN` **belegt**, nicht angenommen (§4) |
| **Q4** | Weder Trennzeichen noch Wirksamkeitsbedingung geraten |
| **Z1** | Die Fenstervorgabe wird gegen die **Anwendungsuhr** aufgelöst |

### Offene Punkte

1. ~~**Präfixsuche.** Nicht gebaut, auch nicht als Schalter. E6 sagt, warum die Frage keine
   Leistungsfrage ist, sondern eine nach der zugelassenen Trefferzahl — und M38, dass eine
   Mindestlänge dafür **je Typ** gelten müsste. Käme sie, käme die PAD-SPACE-Kehrseite mit (§3).~~
   ✔ **Geschlossen am 14.08.2026 in Teil 4** (§15 bis §21).

   > **Die Entscheidung: Die Präfixsuche kommt — aber nicht als Voreinstellung.** Die Suche bleibt
   > exakt; findet sie **null** Treffer, bekommt der Nutzer die Präfixsuche **angeboten**, und sie
   > läuft erst auf sein Zutun. Am Endpunkt ist das der neue Parameter `modus=exakt|praefix` mit der
   > Vorgabe `exakt` (§16).
   >
   > **Der Grund ist nicht der Preis**, sondern M49‑3: **Ein vollständig eingetippter Wert findet als
   > Präfix 23 Nachrichten statt einer.** Als Voreinstellung änderte die Präfixsuche damit die Antwort
   > auch für den Nutzer, der nichts falsch macht. Im Nulltreffer-Fall gibt es diese Kehrseite nicht —
   > dort ist die heutige Antwort leer, und jeder Treffer ist rein zusätzlich.
   >
   > **Der Nutzen bleibt unbeziffert, und das ist eine bewusste Auslassung.** Die Zahl, die ihn trüge
   > — *wie oft die exakte Suche heute leer ausgeht* —, stünde nur in einem Suchprotokoll. **Es ist
   > keines gebaut worden**, weder in Teil 4 noch vorher: Die BAM-Suche schreibt nichts ins
   > `audit_log`, und ein Zähler wäre eine eigene Entscheidung mit eigener Datenhaltung gewesen. Der
   > Punkt wird also mit bezifferten **Kosten** und unbeziffertem **Nutzen** geschlossen; wer ihn
   > später beziffern will, braucht zuerst den Zähler.
   >
   > **Die PAD-SPACE-Kehrseite ist mitgekommen und behandelt** (§17), und die Mindestlänge bleibt aus
   > — aus einem *neuen* Grund, den M49‑2a geliefert hat (§19).

   > **Fortgeschrieben am 13.08.2026 — der Punkt bleibt offen, aber er steht nicht mehr auf
   > Vermutungen.** [`messungen-schritt7.md`](messungen-schritt7.md) **M49** hat die vier Fragen
   > gemessen, die dahinter ungeklärt waren. **Der Punkt wird dadurch nicht geschlossen: Eine
   > Messung entscheidet ihn nicht, eine Entscheidung tut es.** Was jetzt dasteht:
   >
   > | Was gemessen ist | Zahl |
   > |---|---|
   > | **Auffüllen und Präfix schließen einander aus** — beide ankern vorn. Die rohe Präfixfassung findet den aufgefüllten Wert in **allen acht** geprüften Fällen **nicht** | M49‑1 |
   > | Der Ausweg — die Nullen **ins Muster** ziehen — funktioniert in allen acht Fällen; sein Preis sind **3 bis 7** Fassungen je Begriff statt der heutigen fünf **insgesamt** (M47) | M49‑1 |
   > | Bei kurzen Kernen kippt der Ausweg: 9006 und 9036 erzeugen über **eine Million** Kandidatenzeilen | M49‑1 |
   > | **Es gibt keine schützende Mindestlänge.** Typlos steht die schlimmste Trefferzahl bei sechs Zeichen auf **234.159** und kann nie darunter fallen — ein einziger *exakter* Wert trägt diese Zeilen | M49‑2a, M33 |
   > | Zwei Zeichen mehr entwaffnen **sechs Prozent** der gefährlichen Präfixe (1.731 → 1.624) | M49‑2a |
   > | Je Typ liegt die Grenze um **Faktor 3.969** auseinander — und durchsetzbar wäre sie nur mit gewähltem Typ. **Die Suche ist typlos voreingestellt** (M36). Diese Spannung ist benannt und **nicht aufgelöst** | M49‑2b |
   > | Der Plan ändert sich **nicht**: `MessageBAM_BAMValueOnly`, `range`, Einstieg weiter über den seltensten Begriff — kein `STRAIGHT_JOIN` | M49‑3 |
   > | Die Kosten: Normalfall **1,4 ms**, schlimmster Wert **1,823 s** über 30 Tage, vier Zeichen verkürzt **1,361 s** und **3,400 s** über ein Jahr | M49‑3 |
   > | **Schon der vollständige Wert als Präfix findet 23 statt 1.** M34s Bedingung „bei gleicher Trefferzahl" ist bei **keiner** Eingabelänge erfüllt | M49‑3 |
   > | `%` kommt im Bestand **nicht** vor und wäre als Kennzeichen frei; `_` (2.696) und `*` (1.738) sind es nicht. Jede Eingabe bräuchte ein `ESCAPE` — dieselbe Falle wie Regel Q1 | M49‑4 |
   > | **602.794 Werte (3,91 %) tragen ein folgendes Leerzeichen.** Mit `=` harmlos, mit `LIKE` nicht: **Ein Trim auf der Eingabe würde Pflicht** | M49‑4, M43‑3 |
   >
   > **Die Zahl, die den Punkt entscheiden würde, fehlt weiterhin — und sie fehlt nicht aus
   > Nachlässigkeit:** *wie oft die exakte Suche heute leer ausgeht.* Sie stünde nur in einem
   > Suchprotokoll, das die Anwendung nicht führt. **Damit sind die Kosten der Präfixsuche beziffert
   > und ihr Nutzen nicht.**
2. **`ODER` zwischen Begriffen.** Nicht gebaut. Die gemessene Entlastung der Verundung (Faktor
   15.843, M42‑1) gilt für `UND`; für `ODER` ist nichts gemessen, und die Bauform wäre eine andere.
3. **Cursor und Nachladen.** Nicht gebaut. Es gäbe einen Sortierschlüssel, aber keine gemessene
   Zusage darüber, was die zweite Seite kostet.
4. **Die zwei wirkungslosen Einträge** `IBIS`/1 und `SUTTONS`/2000. Was sie kosten, ist seit M47
   beziffert (§3); ob die Kuratierungsregel um eine Wirksamkeitsbedingung ergänzt wird, ist eine
   Entscheidung des Auftraggebers ([`bam-sollaengen.md`](bam-sollaengen.md) §8).
5. **9006.** Der Typ liegt mit 94,21 % um 0,79 Prozentpunkte unter der Schwelle und ist der einzige,
   für den M43‑4 die Wirkung an echten Werten belegt hat (roh 1.642 Treffer, aufgefüllt 4). Die Suche
   findet dort heute die 1.642. Ebenfalls eine Entscheidung des Auftraggebers.
6. **`ONLY_FULL_GROUP_BY`.** Das `GROUP BY` auf dem Primärschlüssel hängt an einer gemessenen
   Servereinstellung (§4). Ob es sich lohnt, das Statement davon unabhängig zu machen — und was die
   Alternative kostet —, ist nicht gemessen.
7. **`anDerZeitgrenze` steht zweimal im Code** (§6). Wandert die Übersetzung nach `common`, wandert
   sie mit ihrem Test; solange nur zwei Fachpakete sie brauchen, ist die Doppelung die kleinere
   Drift.
8. **Der schlimmste Fall unter Last.** Alle Zahlen stammen von einer ruhenden Testkopie. Der
   Jahresfall liegt dort bei 89 % der Zeitgrenze; was er unter dem Verkehr des Altsystems kostet,
   ist nicht gemessen und wäre nur gegen die Produktion zu messen.
9. **Mehr als fünf Begriffe.** Das Geländer steht bei acht, gemessen ist bis fünf (§1).
10. ~~**Die Kuratierung der Suchfeldtypen**~~ ✔ **Entschieden in Teil 3, und zwar gegen eine neue
    Kuratierung** (§10). Die Auswahl kommt aus `MessageBAMMandant`; eine zweite Tabelle entsteht
    nicht.
11. ~~**Ein Komma im Wert macht ihn unsuchbar** *(neu in Teil 4, 14.08.2026)*. **55.989 Werte
    (0,363 %) tragen eines** (M50‑5), und Spring zerlegt einen `@RequestParam List<String>` am Komma:
    Der Wert zerfällt in zwei Begriffe, der zweite trägt keinen Pflichttrenner, die Antwort ist
    `400 suchbegriff-ohne-typtrenner`. **Nicht behoben**, weil die Änderung an der Bindung den
    exakten Pfad träfe, der gebaut, getestet und in M47 gemessen ist.~~
    ✔ **Behoben am 14.08.2026, noch am selben Tag** (§22).

    > **Der Punkt hat einen halben Tag gelebt und war trotzdem ein Defekt und kein offener Punkt.**
    > Er war nicht die Abwesenheit einer Fähigkeit, sondern ein Pfad, der seit Teil 2b als fertig
    > galt und 0,363 % der Werte nicht fand. Als solcher ist er in
    > [`annahmen-korrekturen.md`](annahmen-korrekturen.md) eingetragen und nicht stillschweigend
    > repariert worden. **M51** hat davor gezählt, wen er traf: drei Typen von 62, davon **96,62 %
    > unter Typ 9003** allein, und alle drei bei **einem** Mandanten.
12. **Der Rückfall über ein großes Fenster ist `400` und nicht möglich** *(neu in Teil 4)*. Ging die
    exakte Suche über ein Jahr leer aus, lässt sich der Präfixmodus über **dasselbe** Fenster nicht
    anbieten: Er ist auf 30 Tage gedeckelt (§18). Ein Rückfall müsste dort also entweder das Fenster
    mit verkleinern — und änderte damit zwei Dinge statt einem — oder ausbleiben. **Der Endpunkt
    entscheidet das nicht**; er sagt `400` mit eigenem Fehlertyp und nennt beide Zahlen. Was die
    Oberfläche daraus macht, gehört zu ihr und ist nicht gebaut.
13. **Die gebaute Höchstform ist nicht gemessen** *(neu in Teil 4)*. Acht Begriffe zu je bis zu sieben
    Nullen-im-Muster-Fassungen sind **56 `LIKE`-Zweige**. Gemessen sind ein Begriff ohne
    Zusatzfassungen (M50) und vier verodere `LIKE` bei einem Begriff (M49‑3). **Das Geländer bei acht
    Begriffen ist im Präfixmodus damit noch weniger belegt als im exakten** — dort endet die Messung
    bei fünf (§1).

> **Die offenen Punkte der Oberfläche stehen in §13** — sie sind andere als diese, und sie
> gehören neben die Ansicht, die sie betreffen.

---

# Teil 3 — die Oberfläche

---

## 10. Der zweite Endpunkt: die Belegarten zur Auswahl

```
GET /api/bam/typen
```

Angemeldet, **Mandant aus der Sitzung** (Regel M1). Er nimmt **keine** Mandanten-ID entgegen und
überhaupt keinen Parameter; die Ausnahmeliste in
[`mandantentrennung.md`](mandantentrennung.md) §3 bleibt bei zwei Einträgen.

Er liefert die für **diesen** Mandanten in `MessageBAMMandant` konfigurierten BAM-Typen, in der
Reihenfolge ihres `MessageBAMTypeSortIndex`:

```json
[{ "typ": 9018, "bezeichnung": "Kundenmaterialnummer_K_SAP", "sortIndex": 18 }]
```

| Feld | Bedeutung |
|---|---|
| `typ` | die Typnummer — zugleich der erste Teil des Suchparameters `<typ>:<wert>` |
| `bezeichnung` | `MessageBAMType.MessageBAMTypeDescription`, unverändert. **Nie `null`**: Fehlt die Zeile, steht hier die Typnummer ({@code Typbezeichnung}, dieselbe Regel wie in Teil 1) |
| `sortIndex` | die Ordnung des Altsystems |

**Warum `sortIndex` dabeisteht, obwohl der Belegdaten-Block ihn ausdrücklich nicht liefert.** Dort
wäre sein *Fehlen* die Auskunft „für diesen Mandanten nicht konfiguriert" und damit eine interne
Angabe ([`bam-werte.md`](bam-werte.md) §6). **Hier ist jeder Eintrag konfiguriert** — sonst stünde
er nicht in der Liste —, und die Antwort benennt damit ihre eigene Ordnung, statt sie nur zu haben.

### Keine neue Kuratierungstabelle

`MessageBAMMandant` sagt bereits je Mandant, welche Typen zählen. **Für ein Angebot, in dem der
Nutzer selbst greift, genügt das** — die Kritik aus M40, die den Belegdaten-Block davon abhält,
der Konfiguration zu folgen, traf sie als **Spaltenauswahl** und nicht als Angebot: Dort entschied
sie, was jemand *sieht*; hier entscheidet sie, was jemand *wählen kann*, und wer nichts Passendes
findet, sucht typlos weiter.

### Mandanten ohne konfigurierten Typ bekommen keine Auswahl

`EDITIONLINGERI`, `SYSTEM` und `WOC` haben keinen (M40, in M48 bestätigt), `ZAST` genau einen. Für
sie ist die Antwort `200` mit **leerer Liste**, und die Oberfläche zeigt dann **gar keine**
Typwahl — kein Platzhalter, keine leere Liste. Dieselbe Regel wie bei der leeren Spalte in
[`nachrichtenliste.md`](nachrichtenliste.md) §8.1.

**Kein `404`.** Der Aufrufer stellt eine Frage und benennt keine Ressource — dieselbe Lage wie bei
der Suche selbst (§1).

### Datenquelle, `EXPLAIN` und Laufzeit (Regel L7)

Gemessen in **M48** ([`messungen-schritt7.md`](messungen-schritt7.md)) gegen die Testkopie:

| Fall | Laufzeit |
|---|---:|
| `NEXANS` — 40 konfigurierte Typen | **0,534 ms** |
| `WOC` — keine Konfiguration | **0,410 ms** |

Der Zugriff ist `ref` über `MandantIDSortIndexBAMTYpeIDX` mit **`Using index`**; die Beschriftung
hängt als `eq_ref` auf `PRIMARY` darüber. Der Index ist
`(MandantID, MessageBAMTypeSortIndex, MessageBAMType)` und trägt damit Filter **und** beide
Sortierschlüssel — **es entsteht kein `filesort`**.

**Zwei Sortierschlüssel, und der zweite ist nicht Kosmetik.** M48 findet, dass
`MessageBAMTypeSortIndex` **nicht eindeutig** ist: Bei `VOTG` tragen die Typen 2002 und 2011 beide
den Index 2002. Ohne die Typnummer als zweiten Schlüssel entschiede dort die Reihenfolge der
Speicherung.

**Kein Zeitfenster, und Regel L1 ist nicht berührt.** Sie gilt für *Listen* über `Message`; hier
stehen zwei Stammdatentabellen mit zusammen **131 Zeilen**, von denen keine einen Zeitstempel
trägt.

**`Message` und `MessageBAM` werden nicht angefasst**, und das ist eine Entscheidung: Eine Auswahl,
die nur *belegte* Typen anböte, wäre eine Existenzfrage über den Gesamtbestand eines Mandanten —
genau die Gestalt, die [`nachrichtenliste.md`](nachrichtenliste.md) §1 als **L15-Falle** führt
(13,2 s für `IBIS` gegen 11,9 ms für `WOC`, beide ohne ein einziges Ergebnis).
`BamTypenStatementsTest.nur_stammdaten` hält es fest.

### Mandantentrennung und der Isolationstest (Regel M4)

**Der Mandantenfilter ist hier das `WHERE` selbst** und keine `EXISTS`-Kette — `MessageBAMMandant`
trägt die `MandantID` als Teil ihres Primärschlüssels. Die Kette über `Process → ProjectMandant`
gibt es dort, wo eine Tabelle den Mandanten *nicht* selbst führt; `Message` und `MessageBAM` tun
das nicht, diese hier schon.

`BamTypenIsolationDbIT`, Paarung **`NEXANS` gegen `SUTTONS`**. **Das ist nicht die Paarung der
Vorlage, und der Grund gehört dazu:** `SicherheitsTestbasis` führt `VOTG` gegen `SUTTONS`, und für
die Pfad-Endpunkte ist das richtig. Hier trüge es nur die halbe Aussage — die Konfiguration von
`SUTTONS` (2000, 2001) ist eine **echte Teilmenge** der von `VOTG` (2000 bis 2011, M48), ein Leck
von `SUTTONS` nach `VOTG` bliebe also unsichtbar. `NEXANS` gegen `SUTTONS` ist dagegen **disjunkt**
(9xxx gegen 2xxx), und die Trennung ist in **beide** Richtungen prüfbar.

**Die Gegenprobe sieht hier ein drittes Mal anders aus.** Bei Detail, Kette und Belegdaten stehen
zwei `404`-Rümpfe nebeneinander; bei der Suche zwei `200`-Rümpfe (§7). **Hier gibt es überhaupt
keinen Parameter** — die Frage „fremd oder erfunden" ist gegenstandslos, und an ihre Stelle tritt
die Frage, **woher die Antwort ihren Mandanten nimmt**. Geprüft wird deshalb der Mandantenwechsel:
dieselbe Sitzung, derselbe Aufruf, ein anderer aktiver Mandant — und die Antwort muss sich
vollständig ändern.

Geprüft wird:

1. Beide Mandanten sind konfiguriert, und ihre Typmengen sind **disjunkt** (zur Laufzeit
   hergeleitet — ändert sich die Testkopie, wird der Test rot und sagt es).
2. Jeder sieht **genau** seine eigene Auswahl, verglichen gegen das Quellschema und nicht gegen
   den Prüfling selbst.
3. Kein Mandant sieht einen Typ des anderen, **in beide Richtungen**.
4. Der Mandantenwechsel wechselt die Auswahl vollständig.
5. Der Rumpf trägt **keine** Mandantenkennung.
6. Ohne aktiven Mandanten `403`, auch für ADMIN.

Dazu `BamTypenDbIT` mit dem Fall, den die Regel verlangt: **ein Mandant ohne konfigurierten Typ**
(`WOC`) bekommt `200` mit `[]` und keinen Fehler — sowie der Ordnung bei doppeltem Sortierindex.
Und `BamTypenStatementsTest` hält ohne Datenbank fest, was in der CI sonst niemand prüft:
Mandantenfilter im Statement, nur Stammdaten, `LEFT JOIN` auf die Beschriftung, zwei
Sortierschlüssel.

---

## 11. Die Oberfläche

Route `/suche`, Feld in der Kopfzeile, Feature `features/nachrichten`.

```
components/
├─ marke.tsx                    die gemeinsame Marken-Bauform (Bedienbarkeit als Schalter)
└─ suchsignal.tsx               die eine Meldung „diesen Begriff gibt es schon"

features/nachrichten/
├─ api.ts                       + BamTyp, BamTreffer, BamSuchergebnis, holeBamTypen, holeBamSuche
├─ suche.ts                     der Zustand als reine Funktionen — ohne React
├─ hooks.ts                     + useBamTypen, useSuchzustand, useBamSuche
└─ components/
   ├─ suchfeld.tsx              das Feld in der Kopfzeile samt Typwahl und „+"
   ├─ marken-leiste.tsx         die Begriffe als entfernbare Marken
   ├─ treffer-tabelle.tsx       die Trefferliste
   └─ suche-ansicht.tsx         der Zusammenbau samt Zeitfenster, Varianten und Leerzustand

app/(app)/suche/page.tsx        Server-Komponente, holt nichts vor
```

### Warum die Suche in `features/nachrichten` liegt und nicht in einem eigenen Feature

[`bam-werte.md`](bam-werte.md) §11 ließ die Frage offen — mit der Begründung, die Suche habe
„keine gemeinsame Komponente mit dem Block". **Sie hat welche, nur andere:** Sie zeigt
Nachrichtenzeilen (`StatusPlakette`, `ZeitpunktZelle`, `AblaufZelle`) und öffnet das
**Detail-Panel**. Ein Feature importiert nicht aus einem Nachbarfeature — ein eigenes `suche`
müsste also entweder nachbauen, was es zeigt, oder die halbe Detailansicht nach `components/`
schieben.

Das ist dieselbe Abwägung, mit der `/api/prozesse` in `features/nachrichten` liegt
([`frontend-grundlagen.md`](frontend-grundlagen.md) §8): **Der fachliche Schnitt des Frontends
folgt dem, was der Nutzer sieht** — Nachrichten —, und nicht der Paketstruktur des Backends, wo
`bam` und `message` einander nicht kennen dürfen.

**Zwei Bausteine wandern trotzdem nach `components/`**, weil sie die Kopfzeile *und* eine Ansicht
betreffen: die **Marke** (unten) und das **Suchsignal** (§11.3).

### 11.1 Das Feld in der Kopfzeile

Der Platz ist seit Schritt 3 reserviert (`data-bereich="suche"`, `--dichte-suchbereich` = 18 rem,
[`visuelles-konzept.md`](visuelles-konzept.md) §5) und **seit Teil 3 gefüllt**.

**Kein Navigationseintrag.** Das Feld steht auf jeder Seite; ein Menüpunkt daneben wäre eine zweite
Tür in denselben Raum.

**Die Beschriftung benennt, was gesucht wird: „Belegnummer suchen".** Das ist der wichtigste Satz
dieses Abschnitts, und er hat eine zweite Hälfte: `/nachrichten` hat bereits ein Suchfeld, das
Prozess-, Projekt- und Ablaufnamen filtert. Zwei Felder auf demselben Bildschirm, die verschiedene
Dinge tun und verschieden fehlschlagen, sind eine Falle — besonders für den Nutzer, der kein
EDI-Spezialist ist. **Die Beschriftung des Listenfilters ist deshalb angepasst worden**, von
„Suche" auf „Prozess, Projekt oder Ablauf durchsuchen"
([`nachrichtenliste.md`](nachrichtenliste.md) §8.2).

**Gesucht wird auf Eingabe, nicht beim Tippen.** Keine Entprellung, kein Vorschlagsmenü, keine
Suche je Zeichen: Der Zugriff über den Wert ist der teuerste Pfad dieses Projekts — M35 misst für
den schlimmsten Wert 8,66 s beim Jahresfenster, auf einer *ruhenden* Testkopie.

**Kein Aufklappmenü unter dem Feld.** Eine Trefferzeile trägt Zeitpunkt, Status, Ablauf, Treffertyp
und Kettenhinweis — das ist eine Tabellenzeile und kein Vorschlagseintrag.

**Optional ein Typ dazu, Voreinstellung ist keiner.** Der Typ ist Verfeinerung und keine Pflicht:
M36 misst, dass er nicht beschleunigt (+1,5 bis +4 %), und typlos kostet kaum etwas — `NEXANS`
trägt zehn kuratierte Zeilen, aber nur **drei verschiedene** Sollängen, und aufgefüllt wird nur
nach oben (M47). Die Auswahl erscheint **gar nicht**, wenn der Mandant keinen Typ konfiguriert hat
(§10).

**Die gewählte Belegart und die begonnene Eingabe stehen nicht in der URL.** Sie beschreiben keinen
Ausschnitt, sondern eine begonnene Eingabe — dieselbe Prüfung wie beim halb ausgefüllten freien
Zeitfenster der Liste ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8).

**Geschrieben wird über den Router und nicht über `nuqs`.** Das Ziel ist eine *andere* Route;
gelesen wird trotzdem über dieselben Parser, damit das Geländer bei acht Begriffen greift, sobald
es greifen muss.

### 11.2 Die Begriffe als Marken

Wer einen Wert eingibt und `+` drückt — oder die Eingabetaste —, legt ihn als **Marke** ab, und die
Suche läuft. Weitere Begriffe kommen genauso dazu; sie werden mit **UND** verknüpft. Jede Marke
trägt ihren Typ, falls einer gewählt war, und lässt sich einzeln entfernen.

**Die Marken stehen über der Trefferliste und nicht am Feld.** Das Feld ist 18 rem breit; acht
Marken passen dort nicht hin. Vor allem gehören sie neben das Ergebnis, das sie erzeugen — die
Trefferspalte zeigt bewusst **keinen** Wert, weil er hier steht (§11.5).

**Dieselbe Gestalt wie die Werte-Marken aus Teil 1**, und seit Teil 3 **dieselbe Bauform**:
`components/marke.tsx` führt sie an einer Stelle, der Belegdaten-Block nutzt dieselbe Klassenliste.
Es gibt keine zweite Marken-Gestalt im Projekt.

> **Diese Marke ist ein Bedienelement, die aus Teil 1 nicht — und daran ändert sich nichts.**
> [`bam-werte.md`](bam-werte.md) §11 hält fest, dass die Werte-Marke im Detail **kein Knopf** ist:
> kein Zeigerwechsel, kein Fokusrahmen, kein `title`. Die Bedienbarkeit ist deshalb ein
> **Schalter** und springt nicht per Voreinstellung an: Wer keine Schließen-Schaltfläche mitgibt,
> bekommt eine Anzeige. Und selbst mit ihr ist **der Knopf in der Marke** der Bedienbare und nicht
> die Marke — ein Klick auf den Text tut auch hier nichts.

**Höchstens acht.** Danach ist `+` gesperrt, mit sichtbarer Begründung über der Liste — und die
Begründung sagt, was die Grenze ist: **ein Schutzgeländer und keine fachliche Grenze** (§1). Die
Zahl steht im Frontend *und* im Backend; ein Frontend, das mehr zuließe, führte den Nutzer in ein
`400`.

**Ein doppelter Begriff wird nicht abgelegt.** Gleicher Typ **und** gleicher Wert: Die vorhandene
Marke wird kurz hervorgehoben, es entsteht keine zweite.

**Der Schlüssel ist `(typ, wert)` und niemals der Wert allein** — bei 4,17 Prozent der Paare steht
derselbe Wert unter mehreren Typen (M37). Er ist zugleich die Parameterform `<typ>:<wert>` und
damit eindeutig: Der Typteil enthält nie einen Doppelpunkt, der erste ist also immer der Trenner.

### 11.3 Warum es für den doppelten Begriff eine eigene Naht gibt

Das Feld steht in der Kopfzeile, die Marken stehen auf der Seite — **keines von beiden kennt das
andere**. Die Meldung „diesen Begriff gibt es schon" läuft deshalb über einen Kontext, den der
Anwendungsrahmen spannt (`components/suchsignal.tsx`), genau wie die Anzeigezone.

**Warum nicht über die URL:** Was in der URL steht, steht in jedem geteilten Link. Ein Hinweis, der
einmal aufblitzt, ist kein Filterzustand ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8).

### 11.4 Die Route und der URL-Zustand

Eine eigene Route **`/suche`**. In der URL stehen die Begriffe als **wiederholter**
`begriff`-Parameter in derselben Form wie am Endpunkt, dazu `von`/`bis` und die geöffnete
`nachricht`.

**Nicht in `/nachrichten` hinein.** Verlockend wäre es, weil das Ergebnis eine Nachrichtenliste
ist. Aber die Liste hat ein Pflicht-Zeitfenster mit der Vorgabe **24 Stunden** als Wesenszug, die
Suche eines von **30 Tagen** (§2) — die Suche dorthin zu legen hieße, die Fensterentscheidung
stillschweigend gegen den Nutzer zu treffen, der eine Nummer hat und kein Datum. Dazu stünden zwei
Suchparameter in derselben URL, die verschiedene Dinge tun.

> **Warum die Begriffe wiederholt und nicht getrennt in einem Parameter stehen.** `nuqs` kann
> beides (`createMultiParser` gegen `parseAsArrayOf`), und die Wahl fällt nicht aus Geschmack: Eine
> Liste in *einem* Parameter braucht ein Trennzeichen, und für jedes Trennzeichen wäre ungemessen,
> ob ein BAM-Wert es enthält. **Genau diese Annahme schließt Regel Q4 aus** — und der Endpunkt hat
> sie schon einmal umgangen, indem der Doppelpunkt Pflicht ist und am *ersten* Vorkommen geteilt
> wird (§1). Ein Listentrenner holte die Frage zurück.

**`nachricht` steht in der URL und in keiner Abfrage.** Dieselbe Trennung wie in der
Nachrichtenliste ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) — und hier wiegt sie
schwerer: Träte die geöffnete Nachricht in den Abfrageschlüssel des Zwischenspeichers ein, liefe
bei jedem Klick auf eine Zeile die teuerste Abfrage dieses Projekts noch einmal.

**Das Detail-Panel wird wiederverwendet, nicht nachgebaut.** Es lädt über seine eigene Kennung und
hängt nicht am Ergebnis der Suche ([`nachrichtendetail.md`](nachrichtendetail.md) §10) — ein tiefer
Link auf einen Beleg zeigt ihn auch dann, wenn die Suche dahinter leer ist.

**Ohne den Ansicht-Umschalter.** Er führt aus dem Panel auf die eigene Route und von dort zurück an
die **Liste** (`lib/routen.ts`), nicht an die Suche. Ein Umschalter, dessen Rückweg woanders endet
als dort, wo er herkam, ist keiner; seine beiden Angaben sind deshalb seit Teil 3 freiwillig
([`nachrichtendetail.md`](nachrichtendetail.md) §10.7).

### 11.5 Die Trefferliste

Dieselbe Zeilenform wie die Nachrichtenliste — Zeitpunkt, Status, Ablauf —, und **dieselben
Zellen**: `ZeitpunktZelle` und `AblaufZelle` sind seit Teil 3 ausgeführt statt nachgebaut. Der
Endpunkt sagt zu, dass die zehn Listenfelder wortgleich sind (§1); zwei Nachbauten machten aus
dieser Zusage mit der Zeit zwei Zeilengestalten.

Dazu kommen zwei Spalten:

**„Treffer" zeigt den Typ, nicht den Wert.** Den Wert hat der Nutzer selbst getippt, er steht in
der Marke über der Liste; was er **nicht** weiß, ist, worauf die Nummer getroffen hat.
Kundenmaterialnummer oder Bestellnummer entscheidet, ob er den richtigen Beleg vor sich hat. Bei
mehreren Typen steht der erste und dahinter `+N` — gezählt werden **verschiedene Typen und nicht
Zeilen**, denn zwei Werte desselben Typs sind eine Belegart.

**Kein Wert in der Spalte.** Alle Zeilen haben denselben Begriff getroffen; ihn je Zeile zu
wiederholen wäre Rauschen. Wich der gefundene Wert vom getippten ab, steht das **einmal** über der
Liste (§11.6).

> **Die Längenregel, und warum sie die Zelle trifft und nicht den Text.** Die Beschriftungen sind
> lang und bleiben es: **M45** hat das Kürzen der Endungen ausgeschlossen — ohne sie fallen 62
> Beschreibungen auf 57, und `Abladestelle_L_SAP` und `Abladestelle_K_SAP` stehen über einen Monat
> auf 3.405 Nachrichten gemeinsam. `Lieferantennummer beim Kunden_K_SAP` sind 35 Zeichen, mit `+2`
> dahinter 38. **Gekürzt wird deshalb die Zelle**, mit dem vollständigen Text im `title` und für
> Vorleseprogramme im Markup — dieselbe Regel, die seit Schritt 4 für *jede* Zelle der
> Nachrichtenliste gilt ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1). Die Spalte ist
> breiter als die übrigen Zusatzspalten (13 rem, ab `lg` 16 rem); **die Hauptinformation der Zeile
> weicht dafür nicht** — Zeitpunkt und Status haben ihre eigene Breite.

**Der Kettenhinweis aus `rollen`.** Er kostet nichts — die vier Verkettungsspalten stehen auf der
Zeile, die die Abfrage ohnehin liest (E4) — und er ist hier wichtiger als in der Liste:
**96,87 Prozent der Wurzeln tragen BAM-Werte, aber nur 2,42 Prozent der Kinder** (M26‑1b). Die
Suche findet also fast immer die Wurzel, und die trägt bei einer Aufteilung einen Endstatus, der
die Frage „ist der Beleg beim Partner angekommen" nicht beantwortet. **Ohne Kettenhinweis hält
sich der Nutzer für fertig.** Kurz in der Zelle, als ganzer Satz im `title` — auf einem Touchgerät
gibt es keinen Hover.

**Kein Sammelstatus über die Kette, keine Zählung der Folgenachrichten.** Beides kostete je Zeile
eine eigene Auflösung, und die Zahl stünde bei 87 Prozent der Wurzeln ohnehin auf Eins.

**Am schmalen Fenster fällt der Ablauf weg** (unter `md`, dem Umbruchpunkt des Projekts). Das ist
eine andere Wahl als in der Liste, die dort das *Projekt* weglässt, und sie folgt derselben Frage:
Was beantwortet *welcher Beleg ist das* und *bin ich fertig*? Der Ablaufname beantwortet keines von
beiden und steht im Detail vollständig da.

### 11.6 Was über der Liste steht

**Die Trefferzahl samt Zeitfenster**, und bei Abschneidung **beides zusammen**: dass abgeschnitten
wurde *und* dass ein Fenster galt. Nur eines von beidem ist irreführend — „mehr als 50" ohne
Fenster liest sich wie eine Aussage über den ganzen Bestand.

**Die Varianten der Normalisierung**, sobald sie vom Getippten abweichen: *„Gesucht nach 80337215
und 0080337215."* Keine stille Korrektur. **Die Leerzeichen-Fassung wird nicht gemeldet** — sie ist
für den Nutzer nicht nachvollziehbar (§3) und steht stattdessen in der Hilfe im Leerzustand.

**Das Zeitfenster, sichtbar und verstellbar.** Es verändert nicht nur die Laufzeit, sondern die
**Antwort**: M35 misst 279 von 234.159 Treffern bei einem Tagesfenster. Wer nicht weiß, dass er
durch ein Fenster schaut, hält das Gefundene für alles, was es gibt — deshalb steht es neben der
Trefferzahl und nicht in den Voreinstellungen.

> **Drei Bedienelemente, und keines rechnet gegen die Browseruhr.** Das ist der Punkt, an dem Regel
> Z1 hängt: Die Anwendungsuhr steht im Profil `dev` Monate hinter der realen Zeit, ein aus
> `Date.now()` gerechnetes Fenster liefe an den Daten vorbei.
>
> | Bedienelement | Was es tut |
> |---|---|
> | **„Auf ein Jahr erweitern"** | rechnet **vom `bis` aus der Antwort** zurück — von dem Zeitpunkt also, den das Backend selbst verwendet hat. **365 Tage und nicht „ein Kalenderjahr"**: Die Grenze aus L1 ist `bis.minusYears(1)`, und am 29. Februar läge ein im Browser gerechnetes Kalenderjahr einen Tag darüber, weil JavaScript den Stichtag auf den 1. März schiebt |
> | **„Vorgabe wiederherstellen"** | entfernt beide Parameter und überlässt dem Backend seine 30 Tage. **Kein zweiter Standardwert im Frontend** — er liefe dem ersten irgendwann hinterher |
> | **Die beiden Felder** | nehmen Wanduhrzeit entgegen und rechnen in der **Anzeigezone** um. Ein halb getipptes `datetime-local` meldet sich nicht von selbst; herausgegeben wird der Zustand allein über `validity.badInput`, gelesen an `keyup` und `blur` — derselbe Befund wie am freien Fenster der Liste ([`nachrichtenliste.md`](nachrichtenliste.md) §8.2) |

**Die Nulltreffer-Zeile.** Jede Marke verengt. Landet die dritte bei null, sieht der Nutzer nicht,
welche es war — und mit einer nicht mitgetippten führenden Null passiert genau das. **Die vorige
Trefferzahl steht ohnehin auf dem Schirm:** *„Mit diesem Begriff: 0. Ohne ihn: 12."* kostet
**keine** zusätzliche Abfrage, weil vor jedem `+` schon gesucht wurde.

> **War die vorige Runde abgeschnitten, sagt die Zeile „mehr als".** Dort ist die gelieferte Zahl
> die **Seitengröße** und nicht die Trefferzahl; *„Ohne ihn: 50"* behauptete eine Zahl, die es so
> nicht gibt — ausgerechnet in der Zeile, die vor einem falschen Schluss bewahren soll. Gefunden in
> der Sichtprüfung am 13.08.2026 (§12), behoben, und in beiden Sprachen als eigener Satz geführt.

> **Sie erscheint nur, wenn wirklich ein Begriff dazugekommen ist.** Wer das Zeitfenster
> verkleinert und dabei auf null fällt, bekommt sie nicht: „ohne ihn" benennte dann etwas, das gar
> nicht die Ursache war. Die Regel steht als reine Funktion (`nulltrefferHinweis`) und nicht als
> Bedingung in einer Komponente.

### 11.7 Warten und Abbruch

Die Suche kann dauern — M35 misst 8,66 s beim Jahresfenster für den schlimmsten Wert, auf einer
**ruhenden** Testkopie. Es gibt deshalb einen sichtbaren Wartezustand (Skelett), und der
Abbruchpfad ist der vorhandene: `suche-abgebrochen` (§6). **Kein zweiter Problemtyp.**

**Kein „Erneut versuchen".** Dieselbe Abfrage liefe in dieselbe Zeitgrenze; der Zwischenspeicher
wiederholt diesen einen Fall ohnehin nicht (`lib/query-client.ts`).

> **Derselbe Typ, ein anderer Handlungshinweis — und das ist eine Entscheidung der Ansicht.** Die
> Nachrichtenliste rät „Zeitraum verkleinern oder **Suchbegriff schärfen**". Ein BAM-Wert lässt
> sich nicht schärfen; hier heißt es „Zeitraum verkleinern oder **eine zweite Belegnummer
> nennen**", und die ist die billigere Verengung (§5). **Übersetzt wird weiterhin über den `type`**
> — welcher von zwei Sätzen erscheint, entscheidet die Ansicht, die weiß, worin sie sucht, und
> nicht der Fehlerkatalog.

### 11.8 Der Leerzustand

`/suche` ohne Begriffe braucht eine tragfähige Seite: Es gibt **keinen Navigationseintrag**, wer
das Feld leert, hätte sonst keine Wegweisung mehr.

Sie erklärt, **was gesucht werden kann** — Lieferschein-, Bestell- und Transportnummern, Charge,
Werk, Materialnummer — und verweist auf das Feld in der Kopfzeile. **Kein zweites Suchfeld auf der
Seite.** Zwei Felder für dieselbe Sache sind so verwirrend wie zwei für verschiedene.

**Die Belegarten des eigenen Mandanten stehen dabei**, wenn es welche gibt (aus §10). Sie sind die
einzige Aufzählung auf dieser Seite, die für *diesen* Mandanten nachweislich gilt — alles andere
ist Beispiel. Hat er keine konfiguriert, erscheint auch keine Liste.

**Und hier steht die Hilfe zur Suche**, die §3 ausdrücklich hierhin verschoben hat: dass führende
Nullen und ein führendes Leerzeichen mitgesucht werden, und dass mehrere Begriffe mit **UND**
verknüpft sind.

### 11.9 Am schmalen Fenster

Unterhalb des vorhandenen Umbruchpunkts (768 px, [`visuelles-konzept.md`](visuelles-konzept.md)
§6) trägt der 18-rem-Bereich in der Kopfzeile nicht. **Das Feld wird dort zu einer eigenen, vollen
Zeile** der Kopfzeile.

**Es entfällt nicht.** §5 hielt für den *leeren* Bereich fest, dass er am Handy ganz wegfällt —
„dort ist jeder Pixel Breite vergeben". Das galt für einen Platzhalter; für den **Haupteinstieg**
gilt es nicht: Eine Suche, die es am schmalen Fenster nicht gibt, ist keine. Der Preis ist eine
Zeile Höhe — dieselbe Abwägung, die §6 beim Umbruch der Kopfzeile selbst trifft.

**Kein neuer Umbruchpunkt.** Umgesetzt als `order-6 w-full md:order-3 md:w-suchbereich`; `md` ist
die 48 rem = 768 px des Projekts.

> **Von Hand zu prüfen und nicht durch den Browsertest.** `resize_window` meldet Erfolg und ändert
> `innerWidth` nicht ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Nachgesehen ist das
> **Regelwerk**; die drei Posten — Feld, Marken, Trefferliste — stehen in der Tabelle *Offene
> Sichtprüfungen* in [`README.md`](README.md).

### 11.10 Tests

| Datei | Was |
|---|---|
| `tests/suche.test.ts` | **ohne DOM** — die Parameterform (Pflichttrenner, Teilung am ersten Doppelpunkt, ungültiger Typteil), der Rundlauf URL → Zustand → URL, der übergangene unbrauchbare Begriff, die Abfrage **mit und ohne geöffnete Nachricht Zeichen für Zeichen dieselbe**, der doppelte Begriff, die Sperre beim neunten, die Nulltreffer-Zeile in allen vier Lagen, die Spalte „Treffer" samt Dedupe, die Varianten, das Jahresfenster **einschließlich Schalttag**, und dass die Abschneidemeldung in **beiden** Sprachen Fenster und Abschneidung nennt |
| `tests/suche-marken.test.tsx` | **gerenderter Baum**, vier Fälle: derselbe Wert unter zwei Typen **ohne `console.error`** (der Schlüssel ist `(typ, wert)`, M37), der unbekannte Typ als Nummer, die **Längenregel** der Trefferspalte an der längsten vorkommenden Beschreibung (Klasse plus `title`) und der Kettenhinweis |
| `BamTypenStatementsTest` | **ohne DB** — Mandantenfilter im Statement, nur Stammdaten, `LEFT JOIN` auf die Beschriftung, zwei Sortierschlüssel |
| `BamTypenDbIT` | `@Tag("db")` — der Mandant **ohne** konfigurierten Typ, die vollständige Zeile, die Ordnung bei doppeltem Sortierindex |
| `BamTypenIsolationDbIT` | `@Tag("db")` — **der Pflicht-Isolationstest** (Regel M4), sechs Fälle, §10 |

**Die Prüfung auf `console.error` ist ein Fehlschlagsgrund** (`tests/setup/konsole.ts`), und eine
pauschale Unterdrückung ist untersagt. Der erste Render-Test hängt vollständig daran.

> **Die Gegenprobe ist gelaufen** — am 13.08.2026, mit von Hand auf `begriff.wert` geändertem
> Schlüssel: Der Lauf wurde rot mit *„Encountered two children with the same key, `0050`"*, und die
> Änderung wurde zurückgenommen. Ein Netz, das man nicht gerissen hat, ist eine Behauptung.

**Die Zahl der gerenderten Bäume ist von sieben auf elf gestiegen** und wird an **einer** Stelle
geführt: dem Kopfkommentar von `frontend/vitest.config.mts`
([`frontend-grundlagen.md`](frontend-grundlagen.md) §9). Alle vier neuen sind dieselbe Klasse wie
die bestehenden: eine Konsolenmeldung und Regeln, die **selbst** eine Klasse plus ein `title` sind.

---

## 12. Sichtprüfung

**Durchgeführt am 13.08.2026** am laufenden System (`dev-start.ps1`, Backend `localhost:8080`,
Oberfläche `localhost:3000`), über die Browsersteuerung bei einem Sichtfeld von **1568 × 726 px**,
mit einem Zugang über alle Mandanten. **Eingaben wurden getippt und geklickt, nicht gesetzt** — mit
zwei benannten Ausnahmen: das Neuladen (Punkt 8) *ist* eine von Hand geöffnete URL, denn genau das
ist der geprüfte Vorgang, und die acht Marken (Punkt 12) sind über einen geteilten Link entstanden,
weil acht Eingaben nichts prüfen, was eine prüft.

**Die Prüfwerte stehen nach Regel G1 nicht in dieser Datei.** Beschrieben ist ihre Gestalt: ein
`NEXANS`-Wert des kuratierten Paares 9012 (Charge, Sollänge 10, 100 % führende Null), getippt
**ohne** seine führenden Nullen. **Nicht mit einer Lieferschein-Nummer** — der Grund steht in §14.

| # | Zu prüfen | Erwartet | Befund |
|---|---|---|---|
| 1 | Belegnummer in die Kopfzeile tippen, Eingabetaste | `/suche` öffnet sich, der Begriff steht in der URL, die Trefferliste steht | ✔ `?begriff=:<wert>` — der **wiederholte** Parameter in der Form des Endpunkts. Marke, Trefferzeile und Tabelle stehen |
| 2 | **Normalisierung an einem wirksamen Paar** — Wert ohne führende Nullen | Treffer, und über der Liste die aufgefüllte Fassung | ✔ *„Gesucht nach `<8 Ziffern>` und `00<8 Ziffern>`."* Im geöffneten Detail steht der Wert im Belegdaten-Block **mit** führender Null — die Normalisierung ist damit an beiden Enden gesehen |
| 3 | Zweiten Begriff mit `+` hinzufügen | Die Trefferzahl sinkt, beide Marken stehen in der URL | ✔ **mehr als 50 → 1**, beide Marken in der URL. Die Trefferspalte zeigt dabei `Charge_L_SAP +1` — zwei Typen auf derselben Nachricht (M37) |
| 4 | Einen Begriff hinzufügen, der auf null führt | Die vorige Trefferzahl steht daneben | ✔ *„Mit diesem Begriff: 0. Ohne ihn: **mehr als 50**."* — siehe den zweiten Befund unten |
| 5 | Zeitfenster auf ein Jahr | Die Trefferzahl ändert sich, das Fenster steht sichtbar | ✔ **3 → 30 Treffer**, Fenster von *30.11.–30.12.2025* auf *30.12.2024–30.12.2025*. Der Knopf „Auf ein Jahr erweitern" verschwindet dann, „Vorgabe wiederherstellen" erscheint, und die beiden Felder tragen die Wanduhrzeit |
| 6 | Eine Suche mit mehr als 50 Treffern | Die Meldung nennt **Abschneidung und Fenster** | ✔ *„Mehr als 50 Treffer — gezeigt werden die 50 neuesten im Zeitfenster 30.11.2025, 04:14 bis 30.12.2025, 04:14. Verkleinere den Zeitraum oder nenne eine zweite Belegnummer."* |
| 7 | Eine Trefferzeile anklicken | Das bekannte Detail-Panel mit dem Belegdaten-Block; dessen Marken **nicht** anklickbar | ✔ Panel mit Kopf, `Belegdaten (6)`, Zeitleiste und Eigenschaften. Am gerenderten Baum abgelesen: `cursor: auto`, **kein** `title`, **kein** Knopf, **kein** `tabindex`; Fläche gedämpft, `border-radius: 4,8 px`, `Geist Mono`. **Kein Ansicht-Umschalter** (§11.4) |
| 8 | URL kopieren, neu laden | Dieselbe Suche, dieselben Marken, dasselbe Fenster | ✔ unverändert, einschließlich `von`/`bis` und geöffneter Nachricht |
| 9 | `/suche` ohne Begriffe | Eine Seite, die erklärt, was suchbar ist — **keine leere Fläche** | ✔ Titel, der Satz mit den Belegarten, die **40 konfigurierten Belegarten von `NEXANS`** als Marken, und die Hilfe zu führenden Nullen und Leerzeichen. Kein zweites Suchfeld |
| 10 | `ZAST` (ein Typ) und `WOC` (keiner) | Die Typauswahl verhält sich wie beschrieben, ohne leeren Platzhalter | ✔ `ZAST`: Auswahl mit *Alle Belegarten* (angehakt) und *Rechnungsnummer*; im Leerzustand die eine Belegart. `WOC`: **die Typwahl erscheint gar nicht**, und der Leerzustand nennt keine Belegarten — kein Platzhalter, keine leere Liste |
| 11 | Zeile mit sehr langer Typbeschreibung | Zeitpunkt und Status bleiben lesbar | ✔ `Lieferantennummer beim Kunden_K_SAP` (35 Zeichen): Die **Trefferzelle** kürzt (`scrollWidth > clientWidth`), der `title` trägt *„Getroffen als: …"* vollständig — und der Zeitpunkt kürzt **nicht**. Die Hauptinformation weicht nicht |
| 12 | Acht Marken, dazu ein doppelter Begriff | `+` gesperrt mit sichtbarer Begründung; kein doppelter Eintrag | ✔ Bei acht ist `+` `disabled`, und über der Liste steht *„Mehr als 8 Begriffe nimmt die Suche nicht an — ein Schutzgeländer, keine fachliche Grenze."* Nach dem Entfernen einer Marke erzeugt derselbe Begriff **keine zweite**: Die vorhandene bekommt einen Fokusring, der nach 1,5 s wieder verschwindet |
| 13 | **Schmales Fenster, von Hand** | Feld, Marken und Trefferliste unter 768 px | **offen** — `resize_window` meldet Erfolg und ändert nichts ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Steht als drei Punkte in der Tabelle *Offene Sichtprüfungen* in [`README.md`](README.md) |

**Konsole:** 16 Einträge über den ganzen Durchgang, sämtlich Fremdmeldungen (React-DevTools-Hinweis,
`[HMR] connected`). **Kein `error`, keine Schlüsselmeldung.**

### Was die Abnahme zusätzlich gezeigt hat — zwei Fehler, beide behoben

**1. Die URL trug zwei Begriffe, die Ansicht zeigte einen.** Das Feld schrieb über
`router.replace`, die Marken und das Zeitfenster über `nuqs`. Solange nur der Router schrieb, ging
das gut; **nach der ersten `nuqs`-Änderung kam eine Navigation daneben nicht mehr an** — `nuqs`
führt seinen eigenen Stand, und eine Navigation, die an ihm vorbeigeht, hebt ihn nicht auf. Sichtbar
wurde es erst in der Reihenfolge *Marke entfernen → Begriff hinzufügen*: In der Adresszeile standen
beide Begriffe, in der Liste einer.

> **Behoben:** Auf `/suche` schreibt das Feld über **denselben** Weg wie alles andere; von jeder
> anderen Seite aus bleibt es der Router, weil `nuqs` nicht auf eine andere Route führen kann. Der
> Kommentar an `suchfeld.tsx` hält beides samt dem Grund fest.
>
> **Warum kein Test das gefangen hat, und was daraus folgt:** Der Fehler steckt nicht in einer
> Entscheidung, sondern im Zusammenspiel zweier Zustandsquellen über zwei Komponenten hinweg. Ein
> gerenderter Baum ohne Router und ohne `nuqs`-Adapter kann ihn nicht zeigen — **das ist genau die
> Klasse, für die es die Sichtprüfung gibt.**

**2. „Ohne ihn: 50" — obwohl es mehr waren.** Die Nulltreffer-Zeile nannte die Zahl der
*gelieferten* Zeilen. War die vorige Runde **abgeschnitten**, ist das die Seitengröße und nicht die
Trefferzahl — ein falscher Schluss in ausgerechnet der Zeile, die vor einem falschen Schluss
bewahren soll.

> **Behoben:** `nulltrefferHinweis` gibt die **ganze** vorige Runde zurück statt einer Zahl, und die
> Ansicht wählt daraus einen von zwei Sätzen: *„Ohne ihn: 50."* gegen *„Ohne ihn: **mehr als 50**."*
> `tests/suche.test.ts` hält es fest, in beiden Sprachen.

### Drei Beobachtungen ohne Handlungsbedarf

1. **Der Kettenhinweis stand auf denselben Zeilen wie der Status „Aufgeteilt".** Gesucht wurde ein
   Wert auf Split-Wurzeln; dort sagen Status und Rolle dasselbe, und der Hinweis ist redundant.
   **Der Fall, für den er gebaut ist, ist der andere** — eine Wurzel mit *Endstatus* (M24‑3: bei
   `IBIS`, `IBISGUS` und `ZAST` trägt die Wurzel `FINISHED`). Er ist in dieser Runde nicht
   aufgetreten und bleibt ungesehen.
2. **Bei acht Marken meldet ein doppelter Begriff nichts.** Die Sperre greift vor der Prüfung auf
   Doppelte, das `+` ist schon gesperrt. Das ist die richtige Reihenfolge — bei acht ist „keine
   weiteren" die vorrangige Auskunft —, aber es heißt: Wer bei acht denselben Begriff noch einmal
   tippt, bekommt die Grenzmeldung und nicht den Hinweis auf die vorhandene Marke.
3. **Ein typloser Begriff wird auch auf nicht-numerische Eingaben aufgefüllt.** Bei `WOC` wurde aus
   `q1` zusätzlich `0000q1` gesucht und gemeldet. Das ist das dokumentierte Verhalten aus §3 — ohne
   Typ gelten *alle* kuratierten Sollängen des Mandanten — und keine Eigenheit dieses Teils.

---

## 13. Offene Punkte der Oberfläche

1. **Kein Verweis von einer Wert-Marke im Detail in die Suche.** Er wäre jetzt möglich und ist
   reizvoll: Der Belegdaten-Block kennt beide Teile des Schlüssels, und die Parameterform stünde
   bereit ([`bam-werte.md`](bam-werte.md) §11). **Er wirft aber eine eigene Frage auf** — ersetzt
   der Klick die laufende Suche, oder legt er eine Marke dazu? —, und mit ihm käme die Marke aus
   Teil 1 als Knopf zurück. **Nicht gebaut, und ausdrücklich als offener Punkt notiert.**
2. **Keine Hervorhebung des Treffers im Detail.** Wer aus der Suche eine Nachricht öffnet, sieht im
   Belegdaten-Block alle Werte gleich — der getroffene ist nicht ausgezeichnet. Nicht gebaut.
3. **Gekoppelte Typen werden nicht ausgeblendet und nicht gewarnt.** M37 misst, dass zwei gemeinsam
   gesetzte Typen als Suchpaar wertlos sind — 9015/9016 stehen auf denselben 1.182 Nachrichten,
   9032/9033 auf denselben 100.343. **Welche Paare gekoppelt sind, ist nur für einige Typen
   gemessen** (Regel Q4); eine Warnung wäre für die übrigen geraten. Der Befund gehört notiert und
   nicht in die Oberfläche.
4. ~~**Keine Präfixsuche**, auch nicht als Schalter — unverändert §9, Punkt 1.~~ **Am Endpunkt seit
   Teil 4 vorhanden** (§16). **In der Oberfläche weiterhin nicht** — Teil 4 fasst sie ausdrücklich
   nicht an; der Rückfall im Nulltreffer-Fall ist damit noch nicht bedienbar (§15). Zu bauen wäre er
   dort, wo die Nulltreffer-Zeile schon steht (§11.6) — und mit ihm die Frage aus §9, Punkt 12, was
   bei einem Jahresfenster geschieht.
5. **Kein `ODER` zwischen Begriffen**, keine Klammern, kein Abfragebaukasten — unverändert §9,
   Punkt 2.
6. **Kein Cursor und kein Nachladen** — unverändert §9, Punkt 3.
7. **Die Marke steht seit Teil 3 an einer Stelle, die Bauform aber an zwei Orten beschrieben.**
   `components/marke.tsx` führt die Gestalt; [`bam-werte.md`](bam-werte.md) §11a beschreibt sie aus
   der Sicht des Belegdaten-Blocks, [`visuelles-konzept.md`](visuelles-konzept.md) §5 als
   gemeinsame Bauform. **Zusammengeführt ist der Code, nicht der Text.**
8. **Das schmale Fenster ist nicht gesehen** (§11.9).

---

## 14. Die Lieferschein-Suche ohne führende Null wirkt unvollständig — und das ist kein Fehler

**Das ist der erste Ort, an dem eine offene Entscheidung des Auftraggebers für den Nutzer sichtbar
wird**, und deshalb steht sie hier und nicht nur in einer Kuratierungsdatei.

Typ **9006** („Lieferschein-Nr._L_SAP") ist mit **94,21 %** Längendominanz um 0,79 Prozentpunkte
durch die Schwelle gefallen und steht **nicht** in `bam_sollaenge`
([`bam-sollaengen.md`](bam-sollaengen.md) §4.3 und §8). Für ihn wird deshalb **nicht aufgefüllt**.

**Was der Nutzer sieht:** Wer eine Lieferschein-Nummer ohne führende Null tippt, bekommt die
**rohe** Suche — und M43‑4 misst, was das heißt: **1.642 Treffer statt 4**. Die Liste ist damit
nicht leer, sondern *zu voll*, und die Zeile „Gesucht nach …" nennt keine zweite Fassung. Für den
Nutzer sieht das aus wie eine Suche, die nicht funktioniert.

**Warum die Oberfläche das nicht heilt.** Ein Sonderfall im Frontend wäre eine zweite
Kuratierungsstelle neben `bam_sollaenge` — und die erste liefe der zweiten irgendwann hinterher.
Die Entscheidung lautet: **9006 namentlich in die Kuratierung aufnehmen, oder die 1.642 Treffer
hinnehmen.** Die Schwelle zu senken hilft nicht: Bei 94 % kämen 9028, 9029 und 9004 mit, die
0,21 %, 0,21 % und 5,71 % führende Nullen tragen (§4.3 dort).

> **Für die Abnahme heißt das:** Die Normalisierung ist **nicht** an einer Lieferschein-Nummer zu
> prüfen. Wirksame Paare sind `NEXANS`/9012 (Charge) und `NEXANS`/9024 (Rechnungsnummer) — beide
> mit 100 % Dominanz und 100 % führender Null ([`bam-sollaengen.md`](bam-sollaengen.md) §4.1). Ein
> Durchklicken mit 9006 sähe aus wie ein Programmierfehler und wäre keiner.

---

# Teil 4 — die Präfixsuche

*Entstanden am 14.08.2026, Backend §15 bis §21. **Die Oberfläche ist nicht angefasst** — geprüft mit
`git status`: Der Diff dieses Teils berührt `backend/src/`, `docs/` und sonst nichts.*

> ### 📌 Was sich am Endpunkt aus Teil 2b ändert — und was nicht
>
> **Genau zwei Dinge kommen dazu:** der Parameter `modus=exakt|praefix` (§16) und das Feld `modus`
> in der Antwort (§20). **Ohne den Parameter verhält sich der Endpunkt Zeichen für Zeichen wie
> vorher** — dieselben Statements, dieselben Varianten, dieselben Zeilen; `BamSucheDbIT` vergleicht
> die beiden Rümpfe.
>
> Im Präfixmodus ändert sich **ein** Ausdruck des Statements: Aus `MessageBAMValue IN (…)` wird
> `(MessageBAMValue LIKE ? ESCAPE '\' OR …)`. Mandantenfilter, `GROUP BY`, Deckelung auf 51, die vier
> Anzeigetabellen über der Deckelung, die Sortierung und das fehlende `STRAIGHT_JOIN` bleiben
> unberührt.
>
> **Skills: keiner.** Der Auftrag schließt `frontend-design` und `shadcn` aus; geprüft ist zu Beginn,
> was installiert ist (die Liste steht in [`messungen-schritt7.md`](messungen-schritt7.md) M50‑0).
> Keiner ist eingebunden worden.

Grundlage sind **M49** (die Erhebung vom 13.08.2026) und **M50** (die Messung vom 14.08.2026 gegen
das gebaute Statement mit getauschtem Wertprädikat).

---

## 15. Die Entscheidung, auf der alles steht

`GET /api/bam/suche` sucht exakt. Wer `12345` tippt, findet `123456` nicht.

**Entschieden am 14.08.2026: Die Präfixsuche kommt — aber nicht als Voreinstellung.** Die Suche
bleibt exakt. Findet sie **null** Treffer, bekommt der Nutzer die Präfixsuche **angeboten**, und sie
läuft erst auf sein Zutun.

**Der Grund ist nicht der Preis.** M49‑3 misst ihn als tragbar: 1,374 ms im Normalfall, 1,823 s im
schlimmsten *Wert* über 30 Tage. Der Grund ist der Befund, den dieselbe Messung zusätzlich gefunden
hat:

> **Ein vollständig eingetippter Wert findet als Präfix 23 Nachrichten statt einer**, weil 22 längere
> Werte mit ihm beginnen (M49‑3).

Eine Präfixsuche als Voreinstellung änderte damit die Antwort **auch für den Nutzer, der nichts
falsch macht**. Im Nulltreffer-Fall gibt es diese Kehrseite nicht: Dort ist die heutige Antwort leer,
und jeder Treffer ist rein zusätzlich.

**Was daraus nicht folgt: dass die Präfixsuche billig ist.** Sie ist die teuerste Zugriffsform dieses
Projekts, und der teuerste bekannte Fall war bis zum 14.08.2026 nie gelaufen — er ist es jetzt (§18).

---

## 16. Der Parameter

```
GET /api/bam/suche?begriff=…&von=…&bis=…&modus=exakt|praefix
```

| | |
|---|---|
| **Vorgabe** | `exakt`. Fehlt der Parameter oder ist er leer, verhält sich der Endpunkt wie vor Teil 4 |
| **Gültige Werte** | `exakt`, `praefix` — ohne Rücksicht auf Groß- und Kleinschreibung, Ränder werden beschnitten |
| **Unbekannter Wert** | `400` mit dem Problemtyp `suchmodus-ungueltig`. **Kein stiller Rückfall** — wer `modus=prefix` schreibt, soll es erfahren und nicht unbemerkt exakt suchen |
| **Im Code** | `bam/Suchmodus` — ein Aufzählungstyp, kein freier Text |

**Der Modus kommt als Zeichenkette in den Controller und nicht als Aufzählungstyp.** Sonst wäre ein
unbekannter Wert die Typumwandlung von Spring und nicht der Fehler dieses Endpunkts — mit einem
Rumpf, den die Oberfläche nicht übersetzen kann.

### Er gilt für die ganze Suche und nicht je Begriff

**Ein Kennzeichen *im* Begriff schiede aus.** Die Parameterform `<typ>:<wert>` teilt am **ersten**
Doppelpunkt, und M49‑4 hat gemessen, dass **585 BAM-Werte** einen tragen; ein zweites Trennzeichen
wäre nach Regel Q4 geraten, solange nicht gemessen ist, ob Werte es enthalten. **Wie berechtigt diese
Vorsicht ist, hat M50‑5 gezeigt** — beim Komma, das *nicht* geprüft worden war, sind es 55.989 Werte
(§9, Punkt 11).

Fachlich braucht es das auch nicht: Der Rückfall feuert, wenn die **ganze** Suche leer war.

---

## 17. Das Wertprädikat

Aus `MessageBAMValue IN (…)` wird `(MessageBAMValue LIKE ? ESCAPE '\' OR …)` — ein Zweig je Fassung,
verodert. **Alles andere bleibt Zeichen für Zeichen unverändert.**

**Kein `STRAIGHT_JOIN`, und Teil 4 liefert dafür das schärfste Argument, das dieses Projekt hat**
(§18).

### Das Maskieren ist Pflicht und keine Vorsichtsmaßnahme

M49‑4 hat gezählt: `_` steht in **2.696** Werten des Bestands, `%` in **keinem**. Vor dem Anhängen
des Platzhalters werden im Eingabewert `\`, `%` und `_` maskiert; das Maskierungszeichen selbst
zuerst. **Der Escape-Zeichen ist `\`** — derselbe wie in `LIKE 'ERROR\_%' ESCAPE '\'`
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1, Regel Q1), aus Gründen der Wiedererkennung.

*jOOQ rendert die Klausel für MariaDB als `escape '\\'` — der Rückstrich ist im Zeichenkettenliteral
verdoppelt, wie MariaDB es verlangt. Das ist eine Sache des Dialekts; die Maskierung ist die Zusage.*

**Die Semantik ist an der gebauten Fassung belegt und nicht behauptet** (M50‑4, acht Ausdrücke ohne
Tabellenzugriff): `'50%' LIKE '50\%%' ESCAPE '\'` → 1, `'50X' LIKE '50\%%' ESCAPE '\'` → **0**,
`'5X0' LIKE '5\_0%' ESCAPE '\'` → **0**.

### Folgende Leerzeichen werden im Präfixmodus abgeschnitten — im exakten nicht

**Der Grund ist die Kollation.** `utf8mb4_general_ci` ist PAD SPACE: `'4711 ' = '4711'` ist wahr,
`LIKE` folgt dieser Regel aber **nicht** — `'4711' LIKE '4711 %'` ist **falsch** (M43‑3, in M49‑4 und
M50‑4 an Ausdrücken belegt). Ein folgendes Leerzeichen wäre unter `=` folgenlos und unter `LIKE` der
Unterschied zwischen Treffer und Leere.

**Im exakten Modus wird nichts abgeschnitten** — nicht weil es dort schadete, sondern weil dieser
Pfad gebaut, getestet und in M47 gemessen ist und ohne Anlass nicht angefasst wird.

> ⚠️ **Und hier gehört die Ehrlichkeit dazu, die der Auftrag verlangt hat: Über den Endpunkt feuert
> der Schnitt heute nie.** `Suchbegriff.ausParameter` beschneidet den Wertteil bereits an **beiden**
> Rändern (§3, „der rohe Wert, an den Rändern beschnitten"), und keine der gebildeten Fassungen hängt
> hinten etwas an — die aufgefüllten setzen Nullen davor, die Leerzeichen-Fassung ein Leerzeichen.
> **Der dokumentierte Unterschied zwischen den Modi ist damit heute null.**
>
> **Der Schnitt steht trotzdem im Code** (`Suchbedingung.muster()`), und zwar an der Stelle, an der
> die Zusage gilt: Wer je eine Fassung baut, die auf ein Leerzeichen endet, soll sie nicht in ein
> stilles Nullergebnis laufen lassen. `BamSucheStatementsTest` prüft ihn deshalb an der Bedingung
> direkt und nicht über den Endpunkt — und die Gegenprobe im exakten Modus daneben.

**Führende Leerzeichen bleiben unangetastet.** Sie sind bedeutungstragend: Die Leerzeichen-Fassung
aus `bam_sollaenge` wird auch im Präfixmodus gebildet und ergibt `LIKE ' 4711%'` (M46‑3).

### Die führenden Nullen — der Teil, der eine Regel braucht

**Das Auffüllen aus Teil 2b und ein Präfixmuster ankern gegeneinander.** M49‑1 hat es in **allen
acht** prüfbaren Fällen gemessen: Wer `47118` tippt und `0004711815` sucht, findet mit
`LIKE '47118%'` nichts — und mit der aufgefüllten Fassung `LIKE '0000047118%'` erst recht nicht.
**Die Variantenbildung des exakten Modus ist im Präfixmodus wirkungslos.**

Der Ausweg ist gemessen und funktioniert ebenfalls in allen acht Fällen: **die Nullen ins Muster
ziehen** — ein Muster je plausibler Nullenzahl, `j` von 0 bis `sollaenge − länge(eingabe) − 1`. Das
sind `sollaenge − länge(eingabe)` Fassungen einschließlich der rohen; bei Sollänge 10 also **drei**
für eine siebenstellige und **sieben** für eine dreistellige Eingabe — genau die Spanne „3 bis 7" aus
M49‑1.

> **Warum die Reihe eine Null vor der Sollänge endet.** Bei `j = sollaenge − länge(eingabe)` wäre das
> Muster selbst schon so lang wie die Sollänge — dann wäre der Kern des Werts vollständig eingetippt,
> und das ist genau der Fall, den die **exakte** Suche mit ihrer aufgefüllten Fassung bereits trifft.

**Er wird nur unter einer Bedingung gebaut:**

> **Die Nullen-im-Muster-Fassungen entstehen ausschließlich für Begriffe, die einen Typ tragen und
> für die es eine `sollaenge` gibt. Ein Begriff ohne Typ bekommt im Präfixmodus ausschließlich die
> rohe Fassung** — auch keine mit führendem Leerzeichen, denn die hängt an einem kuratierten Paar.

**Drei gemessene Gründe:**

1. **Der Hauptfall braucht die Fassungen gar nicht.** 9018 sitzt bei `NEXANS` auf **92,26 %** der
   Wurzeln (M39) und hat **keine** Sollänge — dort ist die rohe Präfixsuche wirksam und kostet keine
   einzige Zusatzfassung.
2. **Ohne Typ müssten alle Sollängen des Mandanten bedient werden.** M49‑1 zählt **3 bis 7** Fassungen
   je Begriff bei *einer* Sollänge; die heutige Obergrenze liegt bei fünf **insgesamt** (M47). Bei
   acht erlaubten Begriffen wären es sonst bis zu 56 `LIKE`-Zweige.
3. **Bei kurzen Kernen kippt der Ausweg.** 9006 (Kern 3 Zeichen) erzeugt **1.245.618**, 9036 (Kern
   1 Zeichen) **1.156.360** Kandidatenzeilen. Ein gewählter Typ macht daraus eine bewusste
   Eingrenzung statt einer Nebenwirkung.

**Die Typwahl bekommt damit eine zweite Rolle.** Bisher war sie reine Ergebnisverfeinerung (M36: +1,5
bis +4 %). Das bleibt für die exakte Suche richtig; im Präfixmodus ist sie zusätzlich die
Voraussetzung dafür, dass über führende Nullen hinweg gesucht werden kann. **Das ist kein Widerspruch
zu M36** — dort geht es um Geschwindigkeit, hier darum, welche Fassungen bildbar sind.

---

## 18. Was M50 gemessen hat — und warum das Zeitfenster gedeckelt ist

**Die Frage stand vor der Messung, und die Lesart auch.** M49‑2a kennt einen Vierzeichen-Präfix mit
**1.332.180** Zeilen über den Bestand; durch das gebaute Statement gelaufen war bisher höchstens ein
Fall mit 155.871 Zeilen. Der bekannte Bösfall ist **Faktor 8,5** größer und war nie gelaufen.

| Fall, `NEXANS`, ein Begriff | Laufzeit | Anteil an der 10‑s‑Grenze des Lese-Pools |
|---|---:|---:|
| **30 Tage** | **3,851 s** | **38,5 %** |
| **ein Jahr** | **Abbruch an der 60‑Sekunden-Grenze** | — |

**Der Abbruch ist das Ergebnis.** Er ist im Aufwärmlauf eingetreten, nicht wiederholt worden, und die
Grenze ist nicht ausgesetzt worden. Die vorregistrierte Lesart nennt dafür **Zweig B**:

> **Der Präfixmodus ist auf 30 Tage gedeckelt.** Ein größeres Fenster zusammen mit `modus=praefix` ist
> `400` mit dem eigenen Problemtyp `praefixsuche-fenster-zu-gross`. **Die exakte Suche behält ihr
> Jahresmaximum** — sie ist ein anderer Zugriff und läuft im schlimmsten Fall in 8,940 s durch (M47).

**Es wird nichts gekappt.** Ein stillschweigend verkleinertes Fenster wäre hier besonders schlecht:
Das Fenster verändert bei dieser Suche nicht den Preis, sondern die **Antwort** (M35: 279 von
234.159). Wer ein Jahr anfragt und dreißig Tage bekommt, ohne es zu erfahren, hält das Gefundene für
alles, was es gibt. **Die Antwort nennt deshalb beide Zahlen** — `grenzeTage` und `angefragtTage` —,
dieselbe Bauform wie bei `suche-fenster-zu-gross` in der Nachrichtenliste.

**Numerisch ist der Deckel dieselbe Zahl wie die Fenstervorgabe, inhaltlich nicht.** Die eine sagt,
was gilt, wenn niemand etwas nennt (§2), die andere, wie weit jemand gehen darf. Im Code stehen sie
getrennt (`BamSuchfilter.FENSTER_VORGABE` und `PRAEFIX_FENSTER_MAXIMUM`) und dürfen sich
auseinanderbewegen.

> **Und die unangenehme Folge steht hier und nicht in einer Fußnote:** Ging die exakte Suche über ein
> **Jahr** leer aus, lässt sich der Rückfall nicht über dasselbe Fenster anbieten. Er müsste dann
> entweder das Fenster mitverkleinern — und änderte zwei Dinge statt einem — oder ausbleiben. **Der
> Endpunkt entscheidet das nicht**; er sagt `400` und nennt die Zahlen. Steht als offener Punkt in §9,
> Punkt 12.

### Der `EXPLAIN` — und der Befund, mit dem niemand gerechnet hat (Regel L15)

**Die Einstiegstabelle ist belegt und nicht angenommen — und sie ist eine andere als in M49‑3.**

| Fall | führende Tabelle | `type` | `key` | `rows` | `MessageBAM` |
|---|---|---|---|---:|---|
| M49‑3, schlimmster **Wert** (443.830) | `b1` | `range` | `MessageBAM_BAMValueOnly` | 443.830 | Einstieg |
| **M50, schlimmster Präfix (1.332.180), 30 T** | **`Message`** | **`range`** | **`MessageLastUpdateIDX`** | **409.758** | **zuletzt, `ref` über `PRIMARY`, `rows` 8, `Using index`** |
| **M50, derselbe Fall, ein Jahr** | **`Message`** | `range` | `MessageLastUpdateIDX` | **1.780.243** | ebenso |

Bei 1,33 Millionen Kandidatenzeilen dreht der Optimierer die Reihenfolge um: Er steigt über das
**Zeitfenster** ein und probt `MessageBAM` erst am Ende über den Präfix des Primärschlüssels.
`MessageBAM_BAMValueOnly` steht nur noch unter `possible_keys`.

**Das ist die richtige Wahl** — der Wertindex läge bei 1,33 Millionen Einträgen, das Fenster liefert
409.758 geschätzte Zeilen. **Und es ist das schärfste Argument gegen ein `STRAIGHT_JOIN`, das dieses
Projekt hat:** Eine festgeschriebene Reihenfolge nähme dem Optimierer diese Wahl ausgerechnet im
teuersten Fall. `BamSucheStatementsTest.kein_straight_join_im_praefixmodus` hält es fest.

**Die gerenderte Fassung mit `ESCAPE` ändert am Plan nichts** — auch nicht mit zwei veroderten
`LIKE` (M50‑4). Und **die Deckelung steht, wo sie stand**: `<derived2>` mit 51 Zeilen, darüber vier
`eq_ref` auf `PRIMARY`.

---

## 19. Keine Mindestlänge — auch hier nicht, und aus einem neuen Grund

Regel L5 verlangt sie, Teil 2b hat sie mit Begründung weggelassen (§1), und **M49‑2a hat den Grund
verschärft statt ihn zu entkräften**:

- **Typlos fällt die schlimmste Trefferzahl von k = 4 bis k = 6 nur von 1.332.180 auf 234.159** — und
  kann darunter **nie** fallen, weil genau ein *exakter* Wert diese 234.159 Zeilen trägt (M33). **Der
  Bösfall ist kein Präfixproblem**; er ist im exakten Bestand schon da.
- **Zwei zusätzliche Zeichen entwaffnen sechs Prozent** der gefährlichen Präfixe (1.731 → 1.624).
- **Je Typ liegt die Grenze um Faktor 3.969 auseinander** (59 bei 2000 gegen 234.159 bei 9014).
  Durchsetzbar wäre eine typgebundene Länge nur mit gewähltem Typ — **und die Voreinstellung ist
  typlos** (M36).

**Es wird also keine gebaut.** Was schützt, ist dasselbe wie im exakten Modus: **Zeitfenster, hartes
Limit 50 und der Abbruchpfad** — dazu, dass der Präfixmodus überhaupt nur auf ausdrückliches Zutun
läuft, und dass sein Fenster gedeckelt ist (§18).

> **„Gefährlich sind nur die Kennungsfelder" trägt als Beruhigung nicht**, und das ist inzwischen
> zweimal gemessen. **M49‑2a:** Drei der zehn schlimmsten Präfixpaare gehören Typ **9019
> „Bestellnummer vom Kunden"** — also genau dem, was ein Nutzer eintippt. **M50‑1 schärfer:** Der
> schlimmste Präfix des ganzen Bestands verteilt sich auf 9002 (789.416, Kennung), **9019 (410.030),
> 9034 „Bestellnummer_L_SAP" (132.693) und 0 „Bestellnummer" (2)** — **542.725 seiner 1.332.180
> Zeilen, gut zwei Fünftel, sind Belegnummern.**

---

## 20. Die Antwort

`BamSucheResponse` bekommt **ein** zusätzliches Feld:

```json
{ "nachrichten": [ … ], "begriffe": [ … ], "von": "…", "bis": "…",
  "abgeschnitten": false, "modus": "EXAKT" }
```

| Feld | Bedeutung |
|---|---|
| `modus` | der **tatsächlich verwendete** Modus, `EXAKT` oder `PRAEFIX` |

**Er steht aus demselben Grund in der Antwort wie `von` und `bis`:** Er verändert nicht den Preis,
sondern die Antwort. M49‑3 misst, dass schon ein vollständig eingetippter Wert als Präfix **23
Nachrichten statt einer** findet — wer nicht weiß, welcher Vergleich gelaufen ist, kann die
Trefferliste nicht deuten.

> **Der Parameter ist klein, das Antwortfeld groß geschrieben**, und das ist kein Versehen: Die
> URL-Parameter dieses Projekts sind kleingeschrieben und deutsch (`von`, `bis`, `begriff`), die
> kontrollierten Vokabulare der Antwort sind es nicht (`statusKind`, `rollen`). Gelesen wird der
> Parameter deshalb ohne Rücksicht auf Groß- und Kleinschreibung — wer `modus=PRAEFIX` zurückschickt,
> bekommt keine Fehlermeldung.

**`begriffe[].varianten` trägt weiterhin lesbare Werte und keine Muster.** Der angehängte Platzhalter
gehört nicht in die Anzeige; er entsteht erst in `Suchbedingung.muster()`. Bei Begriffen mit
Nullen-im-Muster stehen dort die aufgefüllten Fassungen, genau wie im exakten Modus; die
Leerzeichen-Fassung wird gesucht und **nicht** gemeldet — unverändert zur Begründung in §3.

### Der neue Fehlerfall

| `type` | Status | Wann | neu? |
|---|---|---|---|
| `suchmodus-ungueltig` | 400 | `modus` ist weder `exakt` noch `praefix` | **neu** |
| `praefixsuche-fenster-zu-gross` | 400 | `modus=praefix` mit einem Fenster über 30 Tagen. Der Rumpf trägt `grenzeTage` und `angefragtTage` | **neu** |

**Zwei neue Problemtypen, und beide betreffen die Parameterform.** Für den Abbruch entsteht **keiner**:
Das ist derselbe Fall wie im exakten Modus und bekommt denselben Schlüssel `suche-abgebrochen` (§6).

---

## 21. Mandantentrennung, Aufbau und Tests

### Die Trennung ist unverändert und ohne Ausnahme

Der Filter als `EXISTS` über `Process → ProjectMandant` steht in **beiden** Statements auf dem
Quellschema (Regel M3), `MandantContext` bleibt erster Pflichtparameter jeder Repository-Methode
(M2), kein Endpunkt nimmt eine Mandanten-ID entgegen (M1). **Der neue Modus ändert daran nichts** —
er tauscht einen Ausdruck innerhalb desselben `WHERE`.

### Der Isolationstest ist erweitert, nicht ergänzt (Regel M4)

`BamSucheIsolationDbIT` führt den Nachweis jetzt **zweimal**: für `modus=exakt` (§7, acht Fälle,
unverändert) und für `modus=praefix` (fünf weitere). Dieselbe Paarung `NEXANS` gegen `SUTTONS`,
dasselbe absolute Fenster, dieselbe Verschiebung der Ununterscheidbarkeit vom **Statuscode** auf den
**Rumpf** — eine leere Suche ist `200` mit leerer Liste und kein `404`.

**Geprüft wird präfixweise:**

1. Beide Mandanten finden ihre **eigenen** Werte auch über den Anfang.
2. Ein **fremder, echter** Wert liefert `200` mit leerer Liste — und einen Rumpf, der von dem einer
   **erfundenen** Eingabe derselben Länge nicht zu unterscheiden ist.
3. Die Trennung gilt in **beide** Richtungen, ebenfalls mit dem Rumpfvergleich.
4. Die **Gegenprobe über den Mandantenwechsel**: als ADMIN zu `SUTTONS` wechseln, dort nachweisen,
   dass der Wert *präfixweise* erreichbar ist, zurückwechseln, erneut suchen.
5. Die **Abschneidung wird auch präfixweise nach dem Mandantenfilter gezählt**.

> **Ein Präfix trifft mehr als ein exakter Vergleich, und der Test muss das aushalten.** Der Wert
> eines fremden Mandanten kann zufällig der Anfang eines **eigenen** Werts sein — dann fände die Suche
> völlig zu Recht eigene Nachrichten, und der Rumpfvergleich vergliche zwei verschiedene Fragen.
> **Die Prüfwerte werden deshalb so hergeleitet, dass das ausgeschlossen ist:** über das Quellschema,
> unabhängig vom Prüfling, wird ein fremder Wert gesucht, für den der eigene Mandant im Fenster
> **null** Präfixtreffer hat. Findet die Herleitung keinen, wird der Test rot und sagt, dass sich die
> Testkopie geändert hat.

### Aufbau im Code

```
bam/
├─ Suchmodus.java              NEU — exakt oder ueber den Anfang, kein freier Text
├─ Suchbedingung.java          + modus, + muster() — Maskierung und Platzhalter an einer Stelle
├─ Sollaengen.java             + die Praefix-Rechnung (Nullen ins Muster)
├─ BamSuchfilter.java          + modus, + PRAEFIX_FENSTER_MAXIMUM
├─ BamSucheRepository.java     + der LIKE-Zweig des Wertpraedikats
├─ BamSucheController.java     + der Parameter, roh als Zeichenkette
└─ BamSucheResponse.java       + modus
```

**Die Maskierung sitzt an genau einer Stelle** (`Suchbedingung.muster()`) und nicht im Repository:
Das Repository rendert, es rechnet nicht. Damit ist sie ohne Datenbank prüfbar — und sie ist es.

### Tests

| Datei | Was, und ob mit Datenbank |
|---|---|
| `SollaengenTest` | **ohne DB** — zwei Gruppen nebeneinander: `Exakt` unverändert, `Praefix` neu. Ohne Typ **genau eine** Fassung (auch bei zweistelliger Eingabe, wo exakt fünf entstehen), mit Typ die Nullen-im-Muster-Fassungen in beiden Grenzen von M49‑1 (drei und sieben), die Reihe endet eine Null vor der Sollänge, keine Fassung trägt einen Platzhalter |
| `BamSuchfilterTest` | **ohne DB** — die Vorgabe `exakt`, der leere Parameter als Nicht-Angabe, Groß- und Kleinschreibung, der unbekannte Wert als `400`, der 30‑Tage-Deckel **genau auf der Grenze und einen Tag darüber**, und dass er für `exakt` **nicht** gilt |
| `BamSucheStatementsTest` | **ohne DB** — `LIKE … ESCAPE` statt `IN`, `%`/`_`/`\` der Eingabe maskiert, je Fassung ein veroderter Zweig, das folgende Leerzeichen weg und das führende da, **kein `STRAIGHT_JOIN`**, der Mandantenfilter, die Anzeigespalten über der Deckelung, die Typbedingung, die zweite Abfrage folgt dem Modus mit — und die Gegenprobe, dass im exakten Modus **kein** `LIKE` steht |
| `BamSucheServiceTest` | **ohne DB** — der Modus in der Antwort, der Modus an **jeder** Bedingung, und die gemeldeten Fassungen je Modus nebeneinander |
| `BamSucheDbIT` | `@Tag("db")` — **das Abnahmekriterium** (verkürzte Eingabe: exakt findet die Nachricht nicht, Präfix findet sie), ohne Typ genau eine Fassung, `%` und `_` literal (als **Paar** mit der Gegenprobe), die Antwort ohne `modus` **byteidentisch** zu `modus=exakt`, der 30‑Tage-Deckel am laufenden Endpunkt, der unbekannte Modus — und der Komma-Befund (seit dem 14.08.2026 umgedreht, §22) |
| `BamSucheIsolationDbIT` | `@Tag("db")` — **der Pflicht-Isolationstest**, jetzt **zwölf** Testfälle statt sieben |

**Kein Prüfwert steht in einer Testdatei** (Regel G1) — unverändert. Neu ist eine zusätzliche
Bedingung an die Herleitung des Präfixfalls: **Sollänge ≥ 8 und Kern ≥ 7 Zeichen.** Ein kurzer Präfix
ist unselektiv (M49‑2a: bis 1.332.180 Zeilen bei vier Zeichen), liefe ins harte Limit von 50, und der
Test schlüge fehl, weil die gesuchte Nachricht **zu weit hinten** steht — nicht, weil der Präfixmodus
kaputt ist.

### Regelbezug

| Regel | Wie umgesetzt |
|---|---|
| **M1–M5** | Unverändert; der Isolationstest deckt den neuen Pfad ab |
| **L1** | Pflicht-Zeitfenster. **Im Präfixmodus zusätzlich auf 30 Tage gedeckelt** — begründet in §18 |
| **L5** | Hartes Limit **ja**, Mindestlänge **nein** — zum zweiten Mal begründet abgewichen (§19). Ob das in `PROJEKTBESCHREIBUNG.md` vermerkt wird, ist eine offene Frage fürs Sparring |
| **L7** | **M50** über `NEXANS`, mit `EXPLAIN` und Laufzeit — vor dem Merge |
| **L10** | Belegvermerk in M50, und die Sätze hier tragen ihre Messnummer |
| **L15** | Die Einstiegstabelle ist im `EXPLAIN` **belegt** — und sie ist eine **andere** als in M49‑3 (§18) |
| **Q1** | Dieselbe Falle, dasselbe Maskierungszeichen (§17) |
| **Q4** | Kein Trennzeichen geraten: Der Modus ist ein eigener Parameter und kein Kennzeichen im Begriff (§16) |
| **Z1** | Unverändert — die Fenstervorgabe wird gegen die Anwendungsuhr aufgelöst |

**`PROJEKTBESCHREIBUNG.md` ist nicht angefasst.**

---

# Nachtrag — das Komma im Wert

*14.08.2026. **Kein neuer Teil und keine neue Fähigkeit**: eine Reparatur an einem Pfad, der seit
Teil 2b als fertig galt.*

---

## 22. Der Kommadefekt — Befund, Lebensdauer, Behebung, Schnitt

### Was kaputt war

Spring bindet einen Anfrageparameter auf eine Liste, indem es einen **einzeln gesetzten** Wert am
**Komma** zerlegt. `?begriff=:4711,815` kam damit nicht als *ein* Begriff `4711,815` an, sondern als
zwei — `:4711` und `815`. Der zweite trug keinen Pflichttrenner mehr, und die Antwort war

```
400 suchbegriff-ohne-typtrenner
```

**Das traf beide Suchmodi**, weil der Fehler *vor* dem Modus sitzt: in der Annahme des Parameters
und nicht im Vergleich. Wer den Wert exakt suchte, bekam `400`; wer ihn als Präfix suchte, ebenso.

### Wie lange er lebte, und wie er gefunden wurde

**Seit Teil 2b** — seit dem Tag, an dem die Suche über den Wert gebaut wurde. Er ist nicht durch
Nachdenken gefunden worden, sondern beim Bau des **Isolationstests** zu Teil 4: Dessen Herleitung
nimmt die längsten `NEXANS`-Werte im Fenster, und **alle 25 Kandidaten trugen ein Komma**. Der
Endpunkt antwortete auf sämtliche mit `400`.

> **Das ist der eigentliche Vermerk, und er steht in
> [`annahmen-korrekturen.md`](annahmen-korrekturen.md):** Ein als fertig gemeldeter Pfad fand
> 0,363 % der Werte nicht, und es fiel erst beim Bau eines Tests auf, der eine ganz andere Frage
> stellte.

### Wie groß der Schaden war — M51

[`messungen-schritt7.md`](messungen-schritt7.md) **M51** hat gezählt, **wo** die Kommas sitzen; M50‑5
hatte nur gezählt, **wie viele** es sind. Beides zusammen:

| | Zeilen | Anteil |
|---|---:|---:|
| `MessageBAM` insgesamt | 15.406.350 | 100 % |
| mit `,` | **55.989** | 0,363 % |
| davon **Typ 9003** `Material-Nr. beim Lieferanten_L_SAP` | **54.096** | **96,62 %** der Kommas · **5,05 %** dieses Typs |
| davon Typ 9016 `Abladestelle_K_SAP` | 1.888 | 3,37 % der Kommas · 0,46 % dieses Typs |
| davon Typ 9018 `Kundenmaterialnummer_K_SAP` | **5** | 0,01 % der Kommas · 0,0002 % dieses Typs |

**Drei von 62 Typen, und alle drei bei genau einem der sieben Mandanten** (`NEXANS`). Die
tragenden Suchtypen aus M39 sind praktisch nicht betroffen — von den sieben nur 9018, und dort mit
fünf Werten. **Der Defekt war also kein Breitenproblem, sondern ein tiefes bei einem Mandanten**:
Dort war jeder zwanzigste Wert des Typs 9003 unsuchbar.

> Die Messung hat den Bau **nicht** entschieden — repariert worden wäre so oder so. Sie entscheidet,
> was hier über die Tragweite stehen darf, statt dass es geschätzt wird.

### Die Behebung

Die Bindung von `begriff` zerlegt nicht mehr am Komma. Im Code: ein `@InitBinder` in
`BamSucheController`, der für `String[]` einen Editor registriert, dessen einzige Aufgabe es ist,
**einen Parameterwert zu einem Eintrag** zu machen:

```java
@InitBinder
void einParameterIstEinBegriff(WebDataBinder binder) {
  binder.registerCustomEditor(String[].class, new EinWertEinEintrag());
}
```

**Mehrere Begriffe kommen weiterhin als mehrfach gesetzter Parameter** — `?begriff=…&begriff=…`,
unverändert wie seit Teil 2b. Der Editor greift nur dort, wo Spring einen *einzelnen* Wert vor sich
hat; bei zwei gesetzten Parametern liegt bereits ein `String[]` vor und bleibt unangetastet.

### Am Statement ändert sich nichts — geprüft, nicht angenommen

**Das Komma ist weder in `=` noch in `LIKE` ein Platzhalter**, sondern ein gewöhnliches Zeichen.
Maskiert werden weiterhin genau `\`, `%` und `_` (§17); das Komma **nicht**. Kein neues Prädikat,
keine neue Fassung, kein neuer Abfrageplan — und deshalb auch keine Messung des Plans nötig.

Geprüft ist das an zwei Stellen von `BamSucheStatementsTest`, gegen den **gerenderten** Text und die
gebundenen Werte:

- `das_komma_wird_nicht_maskiert` — die Eingabe `47_11,815` wird im Präfixmodus zu `47\_11,815%`:
  der Unterstrich maskiert, das Komma unverändert.
- `das_komma_wird_exakt_unveraendert_gebunden` — im exakten Modus steht `4711,815` als gebundener
  Wert, ohne Platzhalter und ohne Rückstrich.

### Der Schnitt — wo die Änderung greift und wo ausdrücklich nicht

| | greift die Änderung? |
|---|---|
| `GET /api/bam/suche`, Parameter `begriff` | **ja** — hier und nur hier |
| `GET /api/nachrichten`, Parameter `status` | **nein** — trennt weiterhin am Komma |
| `GET /api/nachrichten`, Parameter `prozess` | **nein** — trennt weiterhin am Komma |
| Anmeldung, Mandantenwechsel, Administration | **nein** — sie nehmen JSON-Rümpfe entgegen, keine Listen aus Anfrageparametern |
| Detail, Kette, Belegdaten, Belegarten, Prozesse | **nein** — Kennung im Pfad bzw. keine Listenparameter |

**`@InitBinder` gilt für einen Controller**, und `BamSucheController` trägt genau einen Endpunkt mit
genau einem Listenparameter. **Eine projektweite Umstellung wäre etwas anderes gewesen**: Sie träfe
Anmeldung, Nachrichtenliste und Administration mit, und keiner dieser Pfade hat darum gebeten.

**Die Nachbarschaft ist durchgesehen** (M51‑4, alle zehn Controller). Listenparameter gibt es außer
`begriff` genau zwei, beide in der Nachrichtenliste — und bei beiden ist ein Komma **heute**
ausgeschlossen:

- **`status`** nimmt Namen von `MessageStatusKind` (`FEHLER`, `WARTEND`, `LAEUFT`, `AUFGETEILT`,
  `ZUSAMMENGEFUEHRT`, `ABGESCHLOSSEN`, `QUITTIERT`). Aufzählungsnamen tragen kein Komma; ein Komma
  darin gäbe es nur als Tippfehler und ergäbe `400 status-unbekannt`.
- **`prozess`** nimmt eine `Process.ProcessID`. **Gemessen und nicht geschätzt** (Regel Q4):
  **0 von 1.503** Prozessen tragen ein Komma, in zwei Formen gegengeprüft (`LIKE` und `LOCATE`).

> **Nicht mitrepariert, und das ist Absicht** — ein Auftrag, ein Eingriff. Die Zahl sagt, dass dort
> heute nichts kaputt ist, nicht dass die Bindung dort richtig wäre. **Der Zustand ist in
> `BamSucheDbIT.die_nachrichtenliste_trennt_weiterhin_am_komma` festgeschrieben**: Fällt dieser Test
> eines Tages um, ist die Bindung dort geändert worden — dann gehört dieser Abschnitt
> fortgeschrieben und nicht der Test gelöscht.

### Tests

| Test | Was er hält |
|---|---|
| **`BamSucheKommabindungTest`** *(neu, ohne DB)* | Zehn Fälle über `MockMvc`: ein Komma trennt nicht (mit und ohne Typ, auch zwei Kommas), zwei Parameter bleiben zwei Begriffe, der gemischte Fall ergibt zwei, die Reihenfolge bleibt, der leere Parameter fällt weiter weg, derselbe Nachweis im Präfixmodus, Komma **und** Unterstrich nebeneinander — und ein **Nachbar-Controller im selben Aufbau**, der weiterhin am Komma trennt |
| `BamSucheStatementsTest` | Zwei neue Fälle: das Komma wird nicht maskiert (Präfix) und unverändert gebunden (exakt) |
| `BamSucheDbIT.ein_komma_im_wert_wird_gefunden` | **Umgedreht und umbenannt**, nicht gelöscht. Hieß `ein_komma_im_wert_ist_heute_400` und behauptete den Ist-Zustand als Sollzustand. Der Prüfwert ist **hergeleitet** (`LIKE '%,_%'`, längster Kandidat von `NEXANS` im Fenster) und findet die Nachricht exakt **und** im Präfixmodus |
| `BamSucheDbIT.komma_und_zweiter_begriff_ergeben_zwei_begriffe` | Ein Begriff mit Komma und einer ohne ergeben am laufenden Endpunkt **zwei** Begriffe |
| `BamSucheDbIT.die_nachrichtenliste_trennt_weiterhin_am_komma` | Der Schnitt, am laufenden System nachgewiesen |
| `BamSucheIsolationDbIT` | **Unberührt in seiner Aussage und grün** (zwölf Testfälle). Die Zeile `not like '%,%'` in der Herleitung bleibt stehen — sie ist ab jetzt eine Einschränkung der Auswahl und kein Befund mehr; der Kommentar sagt das |

> **Warum der Bindungstest ohne Datenbank sein muss.** Der Defekt lebte einen ganzen Teil lang, ohne
> dass ihn etwas fing — die Tests gegen die Testkopie tragen `@Tag("db")` und laufen in der CI
> **nicht**, und die Einheitstests begannen erst bei `BamSuchfilter.aus`, also *hinter* der Bindung.
> Genau diese Lücke schließt `BamSucheKommabindungTest`.

### Was die Kombination `,` **und** `_` angeht

Sie ist nur **ohne** Datenbank geprüft, und das ist kein Versehen: Bei `NEXANS` trägt im Fenster
**kein einziger** Wert beides zugleich. Ein DbIT dafür wäre entweder rot oder müsste sich seinen
Prüfwert ausdenken — beides schlechter als der Statement-Test, der die Maskierung am gerenderten
Text prüft.

### Regelbezug dieses Nachtrags

| Regel | Wie umgesetzt |
|---|---|
| **M1–M5** | Unberührt. Die Änderung fasst den Mandantenfilter nicht an; der Isolationstest ist gelaufen und grün |
| **L7** | **M51** vor dem Merge — sie entscheidet den Bau nicht, aber sie beziffert die Tragweite |
| **L9** | Berührt: M51 misst ohne Zeitfenster über den Bestand. Begründung wie in M49‑4 und M50‑5 — die Frage gilt dem Bestand und nicht einer Suche |
| **L10** | Belegvermerk in M51, samt der benannten Lücke: **Wie oft solche Werte gesucht werden, ist nicht erhoben** und wird es in diesem Projekt auch nicht |
| **Q1** | Unverändert — dieselbe Maskierung, dasselbe Maskierungszeichen. Das Komma gehört nicht dazu |
| **Q4** | Nichts geraten: Dass `ProcessID` kein Komma trägt, ist gemessen und nicht angenommen |
| **G1** | Kein Prüfwert in einer Testdatei und kein BAM-Wert in dieser Datei — abgedruckt sind Typnummern, Typbeschreibungen und Zählungen |

**`PROJEKTBESCHREIBUNG.md` ist nicht angefasst.** Kein Pfad unter `frontend/` ist geändert.
