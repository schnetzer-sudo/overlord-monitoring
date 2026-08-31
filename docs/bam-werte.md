# Die Belegdaten am Nachrichtendetail

Entsteht in **Schritt 7, Teil 1** (12.08.2026). **Backend und Oberfläche in einem Stück** — §1 bis
§10 sind das Backend, §11 und §11a sind die Oberfläche.

> ### 📌 Nacharbeit vom 13.08.2026 — die Anordnung der Belegdaten
>
> **Eine reine Darstellungsänderung.** Der Belegdaten-Block zeigt dieselben Werte auf rund einem
> Fünftel der Höhe: Die Werte stehen nebeneinander als Marken, die Beschriftung links in einer
> gedeckelten Spalte. **Am Endpunkt ändert sich nichts** — nicht an der Antwortform, nicht an der
> Deckelung bei 20 je Gruppe, nicht an der Sortierung, nicht an `bamAnzahl`. §1 bis §10 sind
> unangetastet.
>
> Was neu ist, steht in **§11a** (die Anordnung mit ihren Begründungen) und **§15** (die
> Sichtprüfung dazu). Neu gemessen sind **E7** — wie viele Werte ein Leerzeichen *innen* tragen,
> die Begründung der Marke — und **M45**, ob die Typbeschreibungen ohne ihre Endung eindeutig
> blieben. **M45 ändert die Anzeige nicht**; sie beantwortet den ersten offenen Punkt in §13.

> ### 📌 Nachbesserung vom 13.08.2026 — die Beschriftungen, **nach** der Nacharbeit desselben Tages
>
> **Reine Frontend-Änderung, kein Statement und deshalb keine Messung nach Regel L7.** Zwei Dinge
> hat die Sichtprüfung oben nicht gesehen, und beide betreffen ausschließlich die **Beschriftung**:
>
> 1. **Der Umbruch fiel in die Endung** — `Kundenmaterialnummer_` / `K_SAP`. Sie ist jetzt eine
>    nicht umbrechbare Einheit; der Name bricht weiter um.
> 2. **Der Deckel galt auch dort, wo er nicht gebraucht wird.** Die 10 rem sind für das Panel
>    gemessen; auf der eigenen Route (Gruppenzeile 1.126 px) sind es **16 rem**.
>
> Beides steht in **§11a**, die Abnahme dazu in **§16**. Am Endpunkt, an seiner Antwortform, an der
> Deckelung bei 20 je Gruppe, an der Sortierung, an `bamAnzahl`, am Listenschlüssel `(typ, wert)`,
> am Aufklappverhalten, an der Einfügestelle, am Umbruchpunkt 768 px und an der Marke ändert sich
> **nichts**. **Die Endung wird nicht gekürzt** — M45 hat gemessen, dass das nicht verlustfrei wäre.

Der Block beantwortet die Frage, mit der der typische Nutzer dieses Werkzeug öffnet: **Welcher Beleg
ist das?** Nicht „wo steht mein Beleg" (das ist [`nachrichtenliste.md`](nachrichtenliste.md)), nicht
„was ist im Einzelnen passiert" ([`nachrichtendetail.md`](nachrichtendetail.md)) und nicht „was hängt
daran" ([`verkettung.md`](verkettung.md)).

Grundlage ist die Erhebung [`messungen-schritt7.md`](messungen-schritt7.md) — M32, M37, M39, M40,
M41 und M42‑1 K5. Wo unten eine Zahl steht, steht dort ihr Statement.

> **Der Suchendpunkt ist ausdrücklich nicht Teil dieses Schritts.** Er steigt über den **Wert** ein
> und ist Teil 2; das Suchfeld ist Teil 3. Die Trennung ist der Grund für den Schnitt: Alle
> bisherigen Zugriffe auf `MessageBAM` liefen über die `MessageID`, als `ref` über den Präfix des
> Primärschlüssels, jedes Mal `Using index` (M11, M26‑1b, M28‑2, M39‑1). Genau diesen Pfad braucht
> dieser Teil — er sitzt damit auf dem einzigen `MessageBAM`-Zugriff, der in diesem Projekt
> mehrfach gemessen ist. Der Einstieg über den Wert trägt ganz andere Kosten (M33: bis **234.159**
> Treffer auf einem einzigen Wert, 10,6 s ohne Zeitfenster). Beide in einen Schritt zu legen hieße,
> das sichere Stück an das riskante zu binden.

---

## 1. Der Endpunkt

```
GET /api/nachrichten/{messageId}/bam
```

Angemeldet, **Mandant aus der Sitzung** (Regel M1). Er nimmt **keine** Mandanten-ID entgegen; die
Ausnahmeliste in [`mandantentrennung.md`](mandantentrennung.md) §3 bleibt bei zwei Einträgen und
wächst hier nicht.

**Kein Zeitfenster** — und das ist keine Nachlässigkeit gegenüber Regel L1. Die gilt für *Listen*
über `Message`. Hier ist die Menge durch einen **Primärschlüssel** benannt; dieselbe Begründung wie
beim Detail- und beim Ketten-Endpunkt. Ein zweiter Grund kommt dazu: `MessageBAM` trägt **keinen
Zeitstempel** ([`datenmodell.md`](datenmodell.md) §3), ein Fenster könnte hier also gar nichts
eingrenzen, was der Aufrufer nicht schon gesagt hat.

**Keine Seitengröße und kein Cursor.** Die Antwort ist im Statement gedeckelt (§3); der Aufrufer hat
nichts zu stellen.

### Antwortform

```json
{
  "messageId": "…",
  "gruppen": [
    {
      "typ": 9018,
      "bezeichnung": "Kundenmaterialnummer_K_SAP",
      "gesamt": 3007,
      "werte": ["…", "…"],
      "weitereVorhanden": true
    }
  ]
}
```

| Feld | Bedeutung |
|---|---|
| `messageId` | die angefragte Kennung, gespiegelt |
| `gruppen` | je Typ eine Gruppe, in der Ordnung aus §6. **Immer vorhanden, leer statt fehlend** |
| `typ` | `MessageBAM.MessageBAMType`. Wird **nicht angezeigt** — er ist zusammen mit dem Wert der Schlüssel der Liste (§11) |
| `bezeichnung` | `MessageBAMType.MessageBAMTypeDescription`, unverändert. **Nie `null`**: Fehlt die Zeile im Altsystem, steht hier die Typnummer |
| `gesamt` | die **wahre** Zahl der Werte dieses Typs — aus Abfrage (a), unabhängig von der Deckelung |
| `werte` | höchstens **20**, aufsteigend nach `MessageBAMValue`. Nie `null` |
| `weitereVorhanden` | `gesamt > werte.length` |

**`gruppen` ist leer statt fehlend, und das ist die häufigste Antwort dieses Endpunkts überhaupt
nicht.** Die Oberfläche ruft ihn nur, wenn `bamAnzahl` im Kopf größer als null ist (§8) — und das
ist bei **19,4 Prozent** der Nachrichten der Fall (M41). Die leere Liste bleibt trotzdem
modelliert: Sie ist die richtige Antwort für einen Aufrufer, der die Zahl nicht gelesen hat, und
für den Chatbot der ersten Ausbaustufe.

### Fehlerfälle

| `type` | Status | Wann |
|---|---|---|
| `nicht-gefunden` | 404 | Die `MessageID` gibt es nicht **oder** sie gehört einem fremden Mandanten |
| `kein-mandant-gewaehlt` | 403 | Kein aktiver Mandant in der Sitzung |

**Es entsteht kein neuer Problemtyp.** `nicht-gefunden` ist der Typ, den
`common/error/RessourceNichtGefundenException` seit Schritt 2 trägt und den ein unbekannter Pfad
ebenfalls bekommt — genau damit sich diese Fälle nicht unterscheiden lassen.

> **Hier hat „404 statt 403" eine zweite Seite, und sie ist der Grund für die vorgelagerte
> Existenzprüfung.** 80,6 Prozent aller Nachrichten tragen keinen BAM-Wert (M41); eine leere
> Gruppenliste ist also der **Normalfall**. Bekäme eine fremde Nachricht dieselbe leere Liste, wäre
> „leer" die Auskunft *„gibt es, gehört aber jemand anderem"* — und der fremde Bestand ließe sich
> abfragen, ohne dass je ein Wert herausgegeben würde. Deshalb wird die Existenz **zuerst** geprüft
> und nicht aus der Zeilenzahl geschlossen (dieselbe Reihenfolge wie beim Eigenschaften-Endpunkt).

---

## 2. Warum zwei Abfragen und nicht eine

Die Zweiteilung ist der Kern dieses Endpunkts.

| | Was sie liefert |
|---|---|
| **(a) Die Zählung je Typ** | `GROUP BY MessageBAMType` über den Index — je Typ die **wahre** Gesamtzahl, unabhängig davon, wie viele Werte die Antwort zeigt |
| **(b) Die angezeigten Werte** | je Typ die ersten **20**, über `ROW_NUMBER() OVER (PARTITION BY MessageBAMType ORDER BY MessageBAMValue)` |

Zusammengelegt gäbe es nur eine der beiden Zahlen: Entweder die Antwort ist ungedeckelt, oder die
Restangabe ist geraten.

### Warum nicht einfach alles laden und in Java deckeln

**Weil die Zahl, an der die Antwortgröße dann hinge, nicht gemessen ist.** M41 misst über Fenster B
ein Maximum von **9.296** Werten auf **einer** Nachricht — ausgerechnet auf einem *Kind*, obwohl
Kinder fast nie Werte tragen (4,1 %). **Über den ganzen Bestand ist das Maximum unbekannt**; M41 hat
einen Monat erhoben.

Eine Antwort, deren Größe an einer ungemessenen Zahl hängt, ist keine gedeckelte Antwort. Die
Zweiteilung kostet eine zweite Abfrage über **denselben** Indexzugriff und macht die Obergrenze zu
einer **Eigenschaft des Endpunkts** statt zu einer Hoffnung über die Daten.

Der Preis ist gemessen und steht in §4: Auf der teuersten bekannten Nachricht kostet der ganze
Aufruf **31,6 Millisekunden** und liefert **20 Zeilen** — bei 9.296 Werten auf der Nachricht.

---

## 3. Die Deckelung: 20 Werte je Gruppe

`BamRepository.WERTE_JE_GRUPPE = 20`, und die Zahl ist gemessen.

| Befund (M41, Fenster B, n = 214.330) | Zahl |
|---|---|
| Wurzeln in der Klasse **6–20 Werte insgesamt** | **82,3 %** |
| Median der Typen je Nachricht *mit* Werten | **8** |
| 90. / 99. Perzentil der Typen je Nachricht | 10 / 13 |
| **Maximum der Typen je Nachricht** | **18** |
| Maximum der Werte auf **einer** Nachricht | **9.296** |

Zwanzig Werte je Gruppe decken damit den Normalfall vollständig ab und begrenzen die Antwort
trotzdem hart: höchstens **18 × 20 = 360 Zeilen**.

**Der Deckel sitzt in der Gruppe und nicht über der Nachricht.** Eine Gesamtdeckelung wäre bei 9.296
Werten unbrauchbar — sie schnitte willkürlich mitten in eine Gruppe, und der Nutzer sähe von einem
Typ alles und vom nächsten nichts.

**Der Deckel hängt nicht an der Rolle**, obwohl die Rollen extrem verschieden sind: 0 % bei
Merge-Eingängen (38.628 von 38.628 ohne Wert), 100 % bei Merge-Ergebnissen (1.669 von 1.669 mit
Werten, Median 20). Eine rollenabhängige Grenze wäre für den Nutzer unerklärlich, und die Rolle steht
ohnehin schon im Kettenblock.

> **Belegvermerk** (Regel L10).
> *Gemessen (M41):* die Zahl der Werte und der verschiedenen **Typen** je Nachricht über Fenster B.
> *Behauptet wird:* dass eine Deckelung **je Typgruppe** genügt.
> **Die Lücke:** Gemessen ist die Zahl je *Nachricht*, nicht die je *(Nachricht, Typ)*. Aus „18 Typen
> und 9.296 Werte" folgt nicht, dass die größte Gruppe 9.296/18 Werte hat — sie kann nahezu alle
> tragen, und M11 legt genau das nahe. Diese Runde hat den Fall **nachgemessen**, wenn auch nur an
> vier Nachrichten: Bei der 9.296er stehen **alle** Werte unter *einem* Typ (§4), bei der 3.409er
> stehen 1.679 + 1.679 unter zweien. **Die Deckelung je Gruppe hält in beiden Fällen** — weil sie je
> Gruppe greift und nicht je Nachricht. Die Verteilung der Werte je (Nachricht, Typ) über ein ganzes
> Fenster bleibt **ungemessen** und steht als offene Frage 6 in
> [`messungen-schritt7.md`](messungen-schritt7.md).

---

## 4. Datenquellen, `EXPLAIN` und Laufzeit (Regel L7, §8 Regel 7)

Gemessen am **12.08.2026** gegen die **Testkopie**.

| | |
|---|---|
| Ziel | Testkopie, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| Nachweis, dass es die Testkopie ist | `SELECT @@global.read_only` → **`1`**, **erste Abfrage jeder Sitzung**; am Ende erneut geprüft: wieder **`1`** |
| Benutzer | Lesebenutzer `monitor_read@%` |
| Serverzeit | Beginn `2026-08-12 13:39:25` (UTC `11:39:25`), Ende `2026-08-12 14:14:53` |
| Laufzeitmessung | serverseitig über `SET profiling = 1` / `information_schema.PROFILING`, **beste von fünf nach einem Aufwärmlauf** |
| Gemessen wurde | der **gerenderte** Text, den jOOQ tatsächlich schickt — nicht eine nachgebaute Fassung |
| Regel S1 | ausschließlich `SELECT`, `SHOW` und `EXPLAIN` |

**Die Testkopie ist unverändert**, geprüft und nicht angenommen — `DATA_LENGTH`/`INDEX_LENGTH` von
`Message`, `MessageAction` und `MessageBAM` sind **byteidentisch** mit
[`messungen-schritt7.md`](messungen-schritt7.md) §0 (11.08.2026) und mit dem Nachtrag vom 12.08.
Die Zahlen unten dürfen deshalb ohne Vorbehalt gegen M32 bis M44 gehalten werden.

### Die vier Bezugsnachrichten

Nach ihrer **Gestalt** gewählt, deterministisch hergeleitet; die `MessageID` steht bewusst nicht
hier — beschrieben wird die Gestalt, nicht der Datensatz.

| | Mandant | Rolle | BAM-Werte | Typen | Herleitung |
|---|---|---|---:|---:|---|
| **typisch** | `NEXANS` | Wurzel | **9** | 9 | die Ankernachricht aus M42‑0 (Fenster A, genau neun Werte, kleinste `MessageID`) |
| **fett** | `NEXANS` | Merge-Ergebnis **und** Split-Kind | **3.409** | 9 | die Prüfnachricht aus **M42‑1 K5** — das Maximum der Merge-Ergebnisse in Fenster B |
| **das Maximum** | **`ZAST`** | Kind | **9.296** | **1** | das Maximum aus **M41**, über Fenster B |
| **`ZAST` typisch** | `ZAST` | — | **445** | 1 | die Nachricht mit dem Umfang, der M40‑3 am nächsten kommt (441 je Nachricht) |

Die Gruppenaufteilung der ersten beiden reproduziert die Messungen Zeile für Zeile: Die fette trägt
1.679 Werte unter 9018 und 1.679 unter 9019, je 23 unter 9020 und 9024, je einen unter fünf weiteren
— genau die Tabelle aus M42‑1 K5.

> **Ein Befund, den der Auftrag nicht vorgesehen hat: Das Maximum aus M41 gehört `ZAST`.** Die
> Aufgabenstellung führt „das Maximum (ein Kind)" und „eine `ZAST`-Nachricht" als **zwei**
> Prüffälle. Gemessen sind sie **derselbe Mandant**: Die Nachricht mit 9.296 Werten ist eine
> `ZAST`-Nachricht, und ihre 9.296 Werte stehen unter **einem** Typ (3, Rechnungsnummer). Das
> bestätigt M40‑3 aus der Gegenrichtung — *„die Deckelung wird nicht vom größten Mandanten
> erzwungen, sondern vom kleinsten"* —, und es macht die Zeile `ZAST` im Messplan nicht überflüssig:
> Sie steht hier zusätzlich als **typischer** Fall dieses Mandanten und nicht als sein Randfall.
> **Regel L7 ist damit über `NEXANS` und `ZAST` erfüllt.**

### Laufzeiten

Alle Werte in Millisekunden, beste von fünf nach einem Aufwärmlauf.

| Statement | typisch (9) | fett (3.409) | Maximum (9.296) | `ZAST` (445) |
|---|---:|---:|---:|---:|
| `existiert` | **0,440** | 0,447 | 0,438 | 0,420 |
| **(a) `zaehleJeTyp`** | **0,868** | 3,752 | **8,335** | 1,206 |
| **(b) `findeWerte`** | **0,764** | 8,824 | **22,788** | 1,685 |
| **Aufruf gesamt** | **2,07** | **13,02** | **31,56** | **3,31** |
| `findeKopf` *(mit `bamAnzahl`)* | 0,981 | 2,324 | 4,668 | 1,135 |

**Der teuerste bekannte Fall kostet 31,6 Millisekunden** — die Größenordnung eines Listenaufrufs und
weit unter der Zeitgrenze des Lese-Pools.

**`existiert` ist unabhängig von der Nachricht**, wie es sein muss: 0,42 bis 0,45 ms über alle vier
Gestalten. Genau darauf ruht die Ununterscheidbarkeit aus §9 — der Zugriff, den eine fremde Kennung
kostet, ist derselbe wie der einer erfundenen.

### Die erste Fassung von (a) war siebzehnmal teurer — und ihr Plan sah gleich gut aus

**Das ist der Befund dieser Messung**, und er steht hier, weil er beim nächsten Umbau sofort wieder
entstehen würde.

Die naheliegende Fassung joint `MessageBAMType` und `MessageBAMMandant` **neben** `MessageBAM` und
gruppiert darüber. Ihr `EXPLAIN` ist unauffällig: `ref` auf `PRIMARY` mit `Using index` auf
`MessageBAM`, `eq_ref` auf beide Stammdatentabellen. Gemessen:

| | typisch | fett | Maximum | `ZAST` |
|---|---:|---:|---:|---:|
| erste Fassung (Joins neben `MessageBAM`) | 0,819 | **68,951** | **53,426** | 3,334 |
| **gebaute Fassung** (erst gruppieren, dann beschriften) | 0,868 | **3,752** | **8,335** | 1,206 |
| Faktor | 0,94 | **18,4** | **6,4** | 2,8 |

Der Grund ist, dass die beiden Nachschlagevorgänge **je Zeile** laufen und nicht je Gruppe:
3.409-mal statt neunmal. Am typischen Fall — neun Werte unter neun Typen — ist der Unterschied
naturgemäß null, und genau deshalb hätte eine Messung an nur einer Nachricht ihn **nicht gefunden**.
`BamStatementsTest.erst_gruppieren_dann_beschriften` hält die Gestalt seitdem fest.

### Zugriffspfade

**`existiert`** — außen `No tables used`, innen dreimal `const`. Derselbe Zugriff wie `S2` in
[`nachrichtendetail.md`](nachrichtendetail.md) §8.

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---:|---|
| `Message` | `const` | `PRIMARY` | 146 | 1 | |
| `mandanten_process` | `const` | `PRIMARY` | 146 | 1 | |
| `ProjectMandant` | `const` | `PRIMARY` | 292 | 1 | `Using index` |

**(a) `zaehleJeTyp`** — die Zeile, auf die es ankommt, ist die letzte:

| id | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---:|---|
| 1 | `<derived2>` | `ALL` | — | — | — | 9 | `Using filesort` |
| 1 | `MessageBAMType` | `eq_ref` | `PRIMARY` | 2 | `gezaehlt.typ` | 1 | |
| 1 | `MessageBAMMandant` | `eq_ref` | `PRIMARY` | 148 | `gezaehlt.typ, const` | 1 | `Using where` |
| 2 | `Message` | `const` | `PRIMARY` | 146 | const | 1 | |
| 2 | `mandanten_process` | `const` | `PRIMARY` | 146 | const | 1 | |
| 2 | `ProjectMandant` | `const` | `PRIMARY` | 292 | const,const | 1 | `Using index` |
| 2 | **`MessageBAM`** | **`ref`** | **`PRIMARY`** | **146** | const | 9 | **`Using index`** |

Auf der 9.296er-Nachricht steht dort `rows` **19.154** (die Schätzung des Optimierers) statt 9, und
`MessageBAMMandant` läuft dann als `ref` über `MandantIDSortIndexBAMTYpeIDX` mit `Using index` —
Zugriffsart und `Using index` auf `MessageBAM` sind in beiden Fällen dieselben.

**(b) `findeWerte`** — dieselbe Gestalt, das Fensterfunktions-`ORDER BY` als `filesort` über der
abgeleiteten Tabelle:

| id | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---:|---|
| 1 | `<derived2>` | `ALL` | — | — | 9 | `Using where; Using filesort` |
| 2 | `Message` | `const` | `PRIMARY` | 146 | 1 | `Using temporary` |
| 2 | **`MessageBAM`** | **`ref`** | **`PRIMARY`** | **146** | 9 | **`Using index`** |

**Die zählende Unterabfrage am Kopf** (`bamAnzahl`, §8) — sie liest **keinen einzigen Wert**:

| id | select_type | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|---:|---|
| 2 | `SUBQUERY` | **`MessageBAM`** | **`ref`** | **`PRIMARY`** | **146** | 9 | **`Using index`** |

**In allen drei Fällen ist der `MessageBAM`-Zugriff `ref` auf `PRIMARY` mit `Using index`** —
derselbe Pfad, den M11, M26‑1b, M28‑2 und M39‑1 gemessen haben. Die Tabelle selbst wird nie
angefasst.

---

## 5. Die Antwortgröße hängt nicht an der Zahl der Werte

Das ist der Nachweis, dass die Deckelung im **Statement** sitzt und nicht in der Anzeige.

| Bezugsnachricht | Werte auf der Nachricht | Gruppen | **ausgelieferte Werte** | Bytes der Werte |
|---|---:|---:|---:|---:|
| typisch | 9 | 9 | **9** | 91 |
| fett | **3.409** | 9 | **85** | 880 |
| das Maximum | **9.296** | 1 | **20** | 160 |
| `ZAST` typisch | 445 | 1 | **20** | 160 |

**Die Nachricht mit 9.296 Werten liefert dieselben 20 Zeilen wie die mit 445.** Zwischen der
schmalsten und der teuersten Antwort liegt der Faktor **9,7** an ausgelieferten Werten, während die
Zahl der Werte auf der Nachricht um den Faktor **1.033** auseinanderliegt.

`BamDbIT.antwortgroesse_haengt_nicht_an_der_wertzahl` hält die Eigenschaft dahinter fest — die Zahl
der ausgelieferten Werte ist höchstens *Gruppen × 20* —, und zwar für jede Nachricht statt für eine
zufällig getroffene.

---

## 6. Die Konfiguration ordnet — sie siebt nicht

**Die Reihenfolge der Gruppen:** zuerst die in `MessageBAMMandant` für **diesen** Mandanten
konfigurierten Typen, in der Reihenfolge ihres `MessageBAMTypeSortIndex`; danach alle übrigen nach
Typnummer.

**Der Block folgt der Konfiguration ausdrücklich nicht als Filter** (entschieden im Sparring vom
12.08.2026, auf Grundlage von M40). Zwei Gründe, und der zweite wiegt schwerer:

1. **Sie sagt nichts über den Bestand.** `WOC` hat **null** konfigurierte Typen und trägt trotzdem
   **2.067** BAM-Zeilen unter Typ 9014 — auf 74,58 % seiner Zeilen ohne Kette. Folgte der Block der
   Konfiguration, sähe dieser Mandant **nichts**. Bei `SYSTEM` deckten sich Konfiguration und
   Bestand (null zu null), bei `EDITIONLINGERI` stellt sich die Frage nicht (keine Nachricht) — die
   Konfiguration ist damit gemessen eine **Sichtbarkeitsentscheidung** und keine Aussage über den
   Bestand.
2. **Die Suche aus Teil 3 ist typlos** (M32, M36). Sie findet also Werte unter unkonfigurierten
   Typen. Fände ein Nutzer eine Nachricht und sähe im Block den gesuchten Wert **nicht**, wäre das
   ein Widerspruch, den niemand auflösen kann.

**Ein Typ wird nicht als „nicht konfiguriert" markiert.** Ob er in `MessageBAMMandant` steht, ist
eine interne Angabe und geht den Nutzer nichts an; er sieht ihn schlicht weiter unten. Deshalb
verlässt der Sortierindex das Backend gar nicht erst — es gibt kein Feld dafür in der Antwort.

**Umgesetzt wird das über zwei `LEFT JOIN`s**, und beide sind es aus eigenem Grund: Fehlt die Zeile
in `MessageBAMType`, darf die Gruppe nicht verschwinden (sie bekommt ihre Typnummer); fehlt sie in
`MessageBAMMandant`, erst recht nicht. Die Einschränkung auf den aktiven Mandanten steht in der
**Join-Bedingung** und nicht im `WHERE` — im `WHERE` wäre aus dem `LEFT JOIN` ein innerer geworden,
und der unkonfigurierte Typ fiele wieder heraus. `BamStatementsTest.die_konfiguration_siebt_nicht`
hält das fest.

### Wo die Ordnung im Code liegt

**In `BamService.ORDNUNG`, nicht im `ORDER BY`.** Das weicht von der Regel aus
[`nachrichtendetail.md`](nachrichtendetail.md) §3 ab („die Ordnung steht an genau einer Stelle,
nämlich im `ORDER BY`") und tut es begründet: Dort entscheidet die Sortierung über eine *fachliche
Aussage* (welcher Schritt der letzte ist) und muss deshalb dort stehen, wo die Zeilen entstehen.
Hier ordnet sie höchstens **18 Gruppen** (M41) und ist eine reine Darstellungsentscheidung — sie
liegt in Java, damit die beiden Fälle, die sie tragen, **ohne Datenbank** prüfbar sind:
`BamServiceTest.konfigurierte_typen_in_ihrer_reihenfolge` und
`…unkonfigurierter_typ_erscheint_hinten`. Das Statement sortiert trotzdem nach Typnummer — damit
zweimal dasselbe herauskommt, auch bevor der Service anfasst.

### Die Beschriftung bleibt, wie das Altsystem sie führt

`MessageBAMType.MessageBAMTypeDescription`, unverändert. Fehlt die Zeile, erscheint die **Typnummer**
— sichtbar unfertig, dieselbe Regel wie bei einem kuratierten Eigenschaftsnamen ohne Übersetzung
([`nachrichtendetail.md`](nachrichtendetail.md) §10.3). Ein **leerer** Text wird wie ein fehlender
behandelt: Eine Gruppe ohne jede Überschrift wäre die eine Darstellung, die schlechter ist als eine
unfertige.

---

## 7. Die Werte werden nicht angefasst

**Keine Normalisierung.** Führende Nullen und Randleerzeichen bleiben stehen, wie sie im Bestand
stehen. M43 misst beides — bei drei von fünf Prüfwerten der Hauptrunde steht eine führende Null —,
**entscheidet aber nichts**: Normalisierung ist ein Thema der **Suche** und nicht der Anzeige. Wer
einen Wert aus dem Block in eine E-Mail kopiert, soll den Wert bekommen, der in der Datenbank steht.

**Sortiert wird über den Wert**, aufsteigend. Eine fachliche Ordnung gibt es nicht: `MessageBAM`
trägt keine Reihenfolge und keinen Zeitstempel. Der Wert ist das einzige stabile Kriterium — und ein
stabiles ist nötig, damit zwei Aufrufe dasselbe zeigen.

---

## 8. `bamAnzahl` steht im Kopf des Detail-Endpunkts

Neu an `GET /api/nachrichten/{messageId}`: **`bamAnzahl`**, ein `int`, **immer vorhanden, `0` statt
fehlend**.

**Warum die Zahl in den Kopf gehört und nicht in diesen Endpunkt.** M41 misst, dass **80,6 Prozent**
aller Nachrichten in Fenster B **keinen** BAM-Wert tragen — bei Merge-Eingängen sind es **38.628 von
38.628**, also alle. Ohne diese Zahl im Kopf müsste die Oberfläche einen Block zeichnen und eine
Anfrage stellen, um festzustellen, dass er leer ist. Dieselbe Begründung wie bei `rollen` in
Schritt 6 und bei `eigenschaftenAnzahl` in Schritt 5.

**Dieselbe Bauform wie `eigenschaftenAnzahl`:** eine zählende Unterabfrage über den Präfix des
Primärschlüssels von `MessageBAM`, `ref` auf `PRIMARY` mit `Using index` — sie **liest keinen
einzigen Wert** und braucht deshalb selbst keine Deckelung.

**Regel L2 ist nicht berührt.** Sie verbietet Live-Aggregation über `Message` für
Dashboard-Kennzahlen; die kommen aus `message_rollup`. Dies ist ein `COUNT` über den Index für
**eine** benannte Nachricht.

**Was sie kostet:** `findeKopf` steigt von gemessenen 0,903 ms (Schritt 5, §8) auf **0,981 ms** an
der typischen Nachricht. Am teuersten bekannten Fall — 9.296 Indexeinträge zu zählen — sind es
**4,668 ms**.

> **Belegvermerk** (Regel L10).
> *Gemessen:* `findeKopf` mit `bamAnzahl` an vier Nachrichten (0,981 / 2,324 / 4,668 / 1,135 ms).
> *Behauptet wird:* dass das Feld den Detail-Aufruf nicht spürbar verteuert.
> **Die Lücke:** Der Vergleichswert 0,903 ms aus Schritt 5 stammt von **anderen** Nachrichten und
> aus einer anderen Sitzung; die 0,078 ms Differenz sind damit **kein** gemessener Zuwachs, sondern
> zwei Zahlen nebeneinander. Belastbar ist die Aussage über den **Randfall**: 4,7 ms für eine
> Nachricht mit 9.296 Werten, gegen 0,98 ms für eine mit neun. Der Zuwachs wächst also mit der Zahl
> der Indexeinträge und ist auch dort klein.

---

## 9. Mandantentrennung

**Der Filter ist Bestandteil jedes der drei Statements** (Regel M3), als `EXISTS` über
`ProjectMandant` in **genau der Form**, die Liste, Detail und Kette verwenden:

```sql
AND EXISTS (SELECT 1 FROM Process p
            JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
            WHERE p.ProcessID = m.ProcessID AND pm.MandantID = :mandant)
```

**Auch die Zählung trägt ihn.** Nähme sie ihn nicht, nennte die Antwort eine Zahl, zu der sie keine
Zeilen liefert — und der Unterschied sähe nach einem Fehler des Werkzeugs aus, nicht nach einem Leck.

**Als `EXISTS` und nicht als Join**, weil `ProjectMandant` im Schema n:m ist. Hier wäre die Folge
eines vervielfachenden Joins **doppelt** sichtbar: Die Zählung nennte ein Vielfaches, und die
gedeckelten Werte zeigten jeden Wert mehrfach.

**Jedes Statement steigt über `Message` ein.** Es gibt keinen Weg in `MessageBAM` hinein, der nicht
durch die Nachricht und damit durch den Filter führt.

### Der Isolationstest

`BamIsolationDbIT`, Paarung **`VOTG` gegen `SUTTONS`** — zwei Mandanten aus verschiedenen Häusern,
wie in der Vorlage `MandantenIsolationDbIT`. Geprüft wird:

1. Beide Mandanten haben Daten im Fenster und erreichen ihre **eigenen** Belegdaten.
2. Eine **fremde** `MessageID` liefert `404`, niemals `403` — **und niemals `200` mit leerer
   Gruppenliste**.
3. **Die Gegenprobe:** Eine fremde, *existierende* Kennung und eine *erfundene* liefern eine
   **ununterscheidbare** Antwort — gleicher Status **und** gleicher Rumpf.
4. **Auch in der Zahl der Datenbankzugriffe ununterscheidbar** *(so seit dem 31.08.2026 —
   bis dahin: „auch in der Laufzeit")*. Eine nachgelagerte Existenzprüfung kostete einen
   zusätzlichen Zugriff und wäre über genug Anfragen ein messbarer Kanal. Verglichen wird die
   **Folge der abgesetzten Statements** auf dem Lese-Kontext, gezählt von einem zweiten
   jOOQ-`ExecuteListener`, den ausschließlich diese Testklasse anhängt.
5. Kein Anzeigename und keine Prozesskennung des fremden Mandanten steht im Rumpf.
6. Die Trennung gilt in **beide** Richtungen.
7. Ohne aktiven Mandanten antwortet der Endpunkt `403` — auch für ADMIN.

> ### ✔ Erledigt am 31.08.2026 — Prüfung 4 misst nicht mehr die Uhr
>
> **Der Punkt hat gehalten, was der Absatz darunter angekündigt hat: „Er gehört in eine eigene
> Runde."** Die Entscheidung dieser Runde ist keine der drei dort genannten — kein Aufwärmaufruf,
> keine mehreren Läufe, keine großzügigere Schranke. **Der Zeitkanal wird an seiner Ursache
> geprüft: an der Zahl der Zugriffe.**
>
> **Warum keine der drei anderen.** Alle drei lassen die Wanduhr in der Zusicherung stehen und
> machen sie nur stiller. Eine großzügigere Schranke ist der schlechteste Fall davon: Sie fängt
> weniger und fällt trotzdem gelegentlich. Und der Kern des Einwands bleibt bei allen dreien
> unberührt — **geschützt werden 0,44 ms, gemessen wird über HTTP auf einem Testrechner.** Der
> Test war nicht ungenau; er hat die falsche Größe gemessen.
>
> **Was jetzt dasteht.** Der Test setzt beide Anfragen ab und vergleicht die Folge der Statements,
> die dabei auf `glassfishDsl` laufen. Der Text trägt Platzhalter statt Bindewerten; zwei Anfragen
> mit verschiedenen Kennungen sind darin Zeichen für Zeichen gleich. Beide Fälle setzen heute
> **zwei** Statements ab — die Mandantenliste der Sitzung und die eine `EXISTS`-Abfrage mit
> Mandantenkette.
>
> **Die Verletzungsprobe ist gefahren und zurückgenommen** (31.08.2026): eine ungefilterte
> Existenzabfrage vor die gefilterte gesetzt — also genau die Bauform, die Punkt 4 verbietet. Der
> Test wird rot, und die Meldung nennt das zusätzliche Statement im Wortlaut:
>
> ```
> select exists (select 1 as `one` from `GlassfishDB`.`Message`
>                where `GlassfishDB`.`Message`.`MessageID` = ?)
> ```
>
> **Zwanzig Läufe hintereinander, zwanzigmal grün** — jeder in einer eigenen JVM, also genau in
> der Lage, in der die alte Prüfung gefallen ist. Bei einem Test, der wegen Unzuverlässigkeit
> angefasst wurde, ist einmal grün kein Nachweis.
>
> > **Belegvermerk** (Regel L10). *Gemessen ist:* dass beide Fälle dieselbe Folge von Statements
> > absetzen, und dass ein zusätzliches Statement den Test rot macht. *Behauptet wird:* dass damit
> > der Zeitkanal aus Punkt 4 abgesichert ist. **Die Lücke:** Zwei gleich viele Zugriffe könnten
> > verschieden lange dauern — etwa weil das eine Statement Zeilen liest und das andere keine. Ob
> > das eine reale Lücke ist, ist **nicht** beantwortet und ausdrücklich **nicht** Gegenstand
> > dieser Runde. Sie steht als offener Punkt in [`testfestigkeit.md`](testfestigkeit.md) §6.
>
> **Die Vorgeschichte bleibt lesbar, weil ohne sie nicht zu verstehen ist, warum hier gezählt und
> nicht gestoppt wird:** Beobachtet war ein Fehlschlag mit **279 ms gegen 20 ms**, also Faktor 14
> bei erlaubtem Faktor 10; ein Wiederholungslauf war danach grün. Als Ursache war der kalte erste
> Aufruf einer JVM **erschlossen** — dieselbe Größenordnung, die [`rollup.md`](rollup.md) §9 für
> den ersten Delta-Lauf mit 387 ms gegen 20 bis 31 ms misst —, und **an `BamIsolationDbIT` nie
> gemessen.** Diese Lücke wird auch jetzt nicht geschlossen; sie wird gegenstandslos.

**Das Zeitfenster ist absolut** (29.12.2025): Außer `NEXANS` endet jeder Mandant am 30.12.2025 (M3);
in einem relativen Fenster sähe `SUTTONS` je nach Datenstand null Zeilen, und der Test bewiese nur,
dass leer leer ist.

**`instance` enthält die Kennung — und das ist keine Auskunft.** Nach RFC 9457 ist es der angefragte
Pfad; weil die `MessageID` *im Pfad* steht, spiegelt es zwangsläufig die Frage. Der Test normalisiert
es wie `traceId` **und prüft zusätzlich Zeichen für Zeichen, dass es genau dem gesendeten Pfad
entspricht** — damit die Spiegelung eine Spiegelung bleibt. Dieselbe Behandlung wie in
`NachrichtendetailIsolationDbIT` und `KettenIsolationDbIT`.

**Die Vorlage in [`mandantentrennung.md`](mandantentrennung.md) §5 wird nicht ergänzt.** Der Fall ist
die dritte Anwendung desselben Musters (Kennung im Pfad, `404` für fremd und erfunden); neu ist
allein die Begründung, warum eine leere Liste hier **kein** zulässiger Ersatz für `404` wäre — und
die steht in §1 dieser Datei, weil sie an den Daten dieses Endpunkts hängt.

---

## 10. Aufbau im Code

```
bam/
├─ BamController.java     REST, nimmt nie eine Mandanten-ID
├─ BamService.java        Ordnung, Zusammenlegen, Restangabe
├─ BamRepository.java     die drei Statements, je mit Mandantenfilter
├─ BamTypZeile.java       eine Zeile aus Abfrage (a)
├─ BamWertZeile.java      eine Zeile aus Abfrage (b)
└─ BamResponse.java / BamGruppeResponse.java
```

**Das Fachpaket ist `bam` und nicht `message`.** Der Zugriff auf `MessageBAM` gehört dorthin, wo in
Teil 2 der Einstieg über den Wert entsteht — Fachpakete kennen einander nicht. Der **Pfad** folgt
trotzdem der fachlichen Frage und liegt unter `/api/nachrichten` (§5.1 der Entwicklungsrichtlinien);
Spring löst über alle Controller hinweg auf.

**Die Existenzprüfung steht ein zweites Mal im Code** — dasselbe Statement gibt es in
`NachrichtendetailRepository.existiert`. Sie aus `message` zu holen wäre der Import aus einem
Nachbarpaket; sie nach `common` zu schieben hieße, `common` zur Sammelstelle zu machen, bevor ein
zweites Fachpaket sie wirklich braucht. **Wandert sie, wandert sie mit ihrem Test.**

### Tests

| Datei | Was, und ob mit Datenbank |
|---|---|
| `BamServiceTest` | **ohne DB** — die Ordnung (konfigurierte Typen nach Sortierindex, unkonfigurierter Typ hinten und ohne Markierung), die Deckelung samt der Grenze *genau 20* und der Restangabe, derselbe Wert unter zwei Typen, Beschriftung ohne und mit leerer Beschreibung, `404` statt leerer Liste, keine zweite Abfrage ohne Typen |
| `BamStatementsTest` | **ohne DB** — Mandantenfilter in **jedem** der drei Statements, kein Einstieg über `MessageBAMValue`, kein Zeitfenster, die Deckelung im Statement, beide `LEFT JOIN`s, und **dass das `GROUP BY` vor den Stammdaten-Joins steht** (der Befund aus §4) |
| `BamDbIT` | `@Tag("db")` — die Zahl im Kopf gegen die Summe der Gruppen, die Deckelung bei `ZAST` gegen echte Daten, der unkonfigurierte Typ bei `WOC`, die leere Antwort mit `200`, die Obergrenze *Gruppen × 20* |
| `BamIsolationDbIT` | `@Tag("db")` — **der Pflicht-Isolationstest** (Regel M4) |

**Keine fest eingetragene `MessageID` in den Datenbanktests.** Die Bezugsnachrichten werden über den
Listen- und den Detail-Endpunkt *gefunden* — nach ihrer Gestalt, nicht nach ihrer Kennung.

> **Ein Fund aus dem Testlauf, der hierhin gehört.** `BamDbIT` prüfte zuerst die **Antwortgröße**
> zweier Nachrichten gegeneinander und verlangte dafür, dass sich ihre Wertzahlen um den Faktor zehn
> unterscheiden. Der Test ist am 12.08.2026 rot geworden, und zwar zu Recht: Welche Nachricht die
> Liste zuerst liefert, hängt am Datenstand — der Test maß den Datenstand und nicht den Code. An
> seiner Stelle steht jetzt die **Eigenschaft** (*ausgelieferte Werte ≤ Gruppen × 20*), die für jede
> Nachricht gilt; der Größenvergleich steht als **Messung** in §5, dort mit benannten Gestalten.

---

## 11. Die Oberfläche

Route `/nachrichten` und `/nachrichten/<id>`, Feature `features/nachrichten`.

```
features/nachrichten/
├─ api.ts                      + BamGruppe, BamWerte, holeBamWerte, Schlüssel `bam`
├─ hooks.ts                    + useBamWerte
└─ components/
   └─ bam-block.tsx            eingeklappt, lädt beim Aufklappen
```

**Der Block liegt in `features/nachrichten` und nicht in einem eigenen Feature `bam`.** Er ist Teil
der Detailansicht, und die gehört diesem Feature; ein Nachbarfeature dürfte es nicht importieren.
Ob die **Suche** aus Teil 3 ein eigenes Feature bekommt, ist damit nicht entschieden — sie hat einen
eigenen Einstiegspunkt und keine gemeinsame Komponente mit dem Block.

### Wo er sitzt und warum

**Zwischen Kettenblock und Zeitleiste.** Er beantwortet *welcher Beleg ist das*, die Zeitleiste
beantwortet *was ist damit passiert*. Nach dem Leitsatz (§1 der Projektbeschreibung) kommt die erste
Frage zuerst: Der typische Nutzer ist kein EDI-Spezialist, er sucht einen Beleg. **Die Belegnummern
sind die Hauptinformation dieser Ansicht und nicht Beiwerk.**

An Kettenblock, Zeitleiste und Eigenschaftenblock ändert sich **nichts** außer der Einfügestelle.

### Er erscheint gar nicht, wenn es nichts zu zeigen gibt

Ist `bamAnzahl` null, gibt es **keinen Rahmen, keinen Schalter und keine Anfrage**. Bei **80,6
Prozent** der Nachrichten ist das der Fall, bei Merge-Eingängen bei **allen** (M41). Dieselbe Regel
wie beim Kettenblock bei leeren `rollen` ([`verkettung.md`](verkettung.md) §8) und beim
Eigenschaftenblock bei `eigenschaftenAnzahl === 0`.

> **Ein Unterschied zum Eigenschaftenblock, und er ist bewusst.** Der lässt bei null Eigenschaften
> eine Zeile Text stehen („Keine technischen Eigenschaften"). Der BAM-Block lässt **nichts** stehen:
> Dass eine Nachricht keine Belegnummer trägt, ist der Normalfall und keine Auskunft, für die jemand
> Platz opfern würde — bei den Eigenschaften ist die Leere die Ausnahme (M17: jede Nachricht des
> Tagesfensters hat welche) und deshalb erwähnenswert.

### Eingeklappt, lädt beim Aufklappen

Dieselbe Bauform wie der Eigenschaften-Block, aus demselben Grund. **Die Überschrift trägt die Zahl
aus `bamAnzahl`**, damit erkennbar ist, ob sich das Aufklappen lohnt — sie kommt aus dem Kopf und
kostet keine Anfrage.

**Der Ladezustand liegt im Block, nicht im Panel:** Wer die Belegdaten aufklappt, will die Zeitleiste
nicht verlieren. Vier Zustände, wie überall: Laden (Platzhalter), Fehler (inline, mit
Wiederholen), Leer (ein Satz), Daten.

**Der Zustand gehört nicht in die URL** — er ist keine Ansicht, die jemand teilt. Beim Blättern
zwischen Nachrichten beginnt der Block wieder eingeklappt; umgesetzt über `key={…}` und damit über
den Baum, nicht über einen Effekt.

### Aufgeklappt — Beschriftung links, Werte rechts

*Umgestellt am 13.08.2026 in der Nacharbeit zu Teil 1. Die erste Fassung setzte jeden Wert in eine
eigene Zeile und jede Beschriftung darüber; was daran nicht trug, steht in §11a.*

Je Typ **eine Zeile** aus zwei Spalten: links die Bezeichnung, rechts die Werte nebeneinander,
umbrechend, jeder in einer eigenen kleinen Fläche. Die Werte bleiben **monospaced** — sie sind
Nummern, keine Prosa, und stehen damit in derselben Laufweite wie Zeitpunkte und Kennungen in der
Liste.

Steht `weitereVorhanden`, folgt die **ehrliche Restangabe** *„und 2.987 weitere"* (`gesamt −
werte.length`) **hinter** den Werten und in derselben Zelle — als gedämpfter Text **ohne Fläche**.
Als Marke gesetzt sähe sie aus wie ein weiterer Wert. **Kein „mehr laden".** Das kommt erst, wenn
jemand es braucht, und es bräuchte einen Cursor.

**Kein Verweis auf die Suche.** Ein Klick auf einen Wert tut nichts — der Suchendpunkt existiert
noch nicht, und ein toter Verweis ist schlechter als keiner. Die Marke ist deshalb **kein Knopf**:
kein Zeigerwechsel, kein Fokusrahmen, kein `title`.

> **Nachgetragen am 13.08.2026:** Die Begründung stimmt nicht mehr in ihrer Voraussetzung — **der
> Suchendpunkt existiert seit Teil 2b** (`GET /api/bam/suche`, [`bam-suche.md`](bam-suche.md)).
> **Am Verhalten des Blocks ändert das hier nichts**, und zwar bewusst: Ob ein Klick auf eine Marke
> in die Suche führt, ist eine Entscheidung der **Oberfläche** und gehört damit in Teil 3, zusammen
> mit dem Suchfeld, den Chips und der Trefferliste. Was der Endpunkt dafür schon mitbringt, ist die
> Form, die ein solcher Verweis bräuchte: `begriff=<typ>:<wert>` — und der Block kennt beide Teile
> des Schlüssels bereits (§11).
>
> **Teil 3 hat entschieden, und zwar dagegen** (13.08.2026). Der Verweis ist **nicht** gebaut: Er
> wirft eine eigene Frage auf — ersetzt der Klick die laufende Suche, oder legt er eine Marke dazu?
> —, und mit ihm käme diese Marke als Knopf zurück. Er steht als offener Punkt in
> [`bam-suche.md`](bam-suche.md) §13. **Der Satz oben gilt damit unverändert weiter: Ein Klick auf
> einen Wert tut nichts.**

---

## 11a. Warum die Anordnung so und nicht anders

### Die Marke ist die Wortgrenze, nicht Schmuck

**Das ist der tragende Satz dieser Anordnung.** Werte nebeneinander zu setzen geht nur, wenn zu sehen
ist, wo einer aufhört. Ein Wert enthält selbst Leerzeichen — `0Z3 915 902 D` ist **ein** Wert und
nicht vier.

> **Belegvermerk** (Regel L10).
> *Gemessen (E7):* Über Fenster B tragen **27.792 von 936.529** BAM-Werten (**2,97 %**) ein
> Leerzeichen **innen**, verteilt auf **16** Typen. Angeführt von 9016 (11.360), 9003 (10.059) und
> **9018** (5.166) — und 9018 ist der Typ, den `NEXANS` auf 92,26 % seiner Wurzeln trägt (M39).
> *Behauptet wird:* dass eine Darstellung nebeneinander **ohne** sichtbare Grenze unlesbar wäre.
> **Die Lücke:** Gemessen ist das Vorkommen der Leerzeichen, nicht die Lesbarkeit. Der Schluss ist
> am 13.08.2026 an echten Daten **angesehen** (§15, Punkt 3) und nicht geprüft.

**Ein Trennzeichen taugt nicht**, und zwar aus zwei Gründen:

1. **Das naheliegende Trennzeichen ist genau das Zeichen, das in den Daten steht.** Ein Leerzeichen
   als Trenner zerschnitte 27.792 Werte je Monat.
2. **Für jedes andere Zeichen — Komma, Semikolon, Pipe — ist ungemessen, ob ein Wert es enthält.**
   Eines zu wählen hieße, eine Annahme über die Daten zu treffen (Regel Q4). **Die Marke muss diese
   Frage gar nicht beantworten:** Sie macht die Grenze zu einer Eigenschaft der *Darstellung*.

**Keine Statusfarbe.** Die Marke sagt nichts über einen Zustand. Sie nutzt den vorhandenen
gedämpften Flächenton (`--muted`, [`visuelles-konzept.md`](visuelles-konzept.md) §3) und führt
**keine neue Farbrolle** ein — nachgesehen am gerenderten Baum: Der Hintergrund der Marke ist
zeichengleich mit dem Token. Kein Rahmen, kleiner Radius (`rounded-sm`, 4,8 px).

### Die Endung bricht nicht auf — sie ist eine Einheit

*Nachgebessert am 13.08.2026, **nach** der Nacharbeit desselben Tages. Die Sätze darunter bleiben
stehen; hier steht, was an ihnen ungesehen geblieben war.*

Bei `Kundenmaterialnummer_K_SAP` stand auf der ersten Zeile `Kundenmaterialnummer_` und auf der
zweiten `K_SAP`. **Das ist kein Breitenproblem.** Es ist `overflow-wrap: break-word` an einer
Zeichenkette, die keine Wortgrenze hat: Der Bruch fällt dorthin, wo die Breite ausgeht, und der
Unterstrich bleibt am Zeilenende hängen. Gemeint ist `Kundenmaterialnummer` / `_K_SAP`.

Gezeigt wird deshalb **dieselbe Zeichenkette in zwei Teilen** (`lib/bam-beschriftung.ts`): Der Name
darf weiterhin umbrechen — eine Beschriftung wie `Gutschriftsanzeigen-Nummer` ist allein breiter als
die Spalte —, die Endung trägt `white-space: nowrap`. **Gekürzt wird nach wie vor nichts.**

**Die Endung ist der abschließende Lauf aus `_GROSSBUCHSTABEN`-Segmenten, und diese Fassung ist
gemessen und nicht gewählt.** Beide naheliegenden Fassungen sind an echten Daten falsch:

| Fassung | Was sie liefert |
|---|---|
| `[A-Z]` **mit** `i`-Flag | `Sender_Ident_FORS` → Endung `_Ident_FORS`. Genau der Fehler, den **M45‑2** unter der Spaltenkollation `general_ci` in SQL gemessen hat — dort traf `[A-Z]` auch Kleinbuchstaben. In JavaScript ist `[A-Z]` ohne Flag zeichengenau; ein `i` reproduzierte ihn |
| „ab dem letzten Unterstrich" | für **33** Typen `_SAP` statt `_L_SAP`/`_K_SAP` — und damit ausgerechnet der Verlust der Unterscheidung, wegen der die Endung überhaupt stehen bleibt (§13) |

> **Belegvermerk** (Regel L10).
> *Gemessen (M45‑1, M45‑2):* die 62 Typbeschreibungen des Bestands, ihre drei Endungen (`_L_SAP` 20,
> `_K_SAP` 13, `_FORS` 7; **22 ohne**) und die Fehlzuordnung von `Sender_Ident_FORS` unter
> `general_ci`.
> *Behauptet wird:* dass die Regel den Bruch an die richtige Stelle legt.
> **Die Lücke:** M45 hat die Beschreibungen erhoben, nicht ihren Umbruch. Dass die Endung danach in
> **einem** Zeilenrechteck steht, ist am 13.08.2026 am laufenden System **gesehen** (§16, Punkt 1)
> und nicht aus der Regel geschlossen.

**Nicht getrimmt.** Typ 9008 heißt `Beleg-Nr.··TSL·_L_SAP` — zwei Leerzeichen im Namen und eines vor
dem Unterstrich. Der Name behält seine Zeichen unverändert; das Leerzeichen davor ist dort sogar die
bessere Umbruchstelle. **Ziffern treffen die Regel nicht**, und das ist richtig: Fehlt die Zeile in
`MessageBAMType`, steht statt der Beschreibung die **Typnummer** (§6) — `9018` hat keine Endung, wie
22 der 62 echten Beschreibungen auch.

> ⚠️ **Zwischen den beiden Teilen darf kein Leerzeichen entstehen.** Stünde dort eines, hieße die
> Gruppe `Kundenmaterialnummer _K_SAP` — im Quelltext unauffällig, in der Anzeige falsch. Der Fall
> hängt an der Formatierung des JSX und nicht an der Zerlegung; er ist deshalb der einzige Punkt
> dieser Nachbesserung, der einen **gerenderten Baum** braucht (`tests/bam-block.test.tsx`), und er
> wird über `textContent` des umschließenden Elements geprüft — eine Textsuche fiele herein, weil der
> Text jetzt in zwei Kindern liegt.

### Die Beschriftungsspalte ist gedeckelt, nicht inhaltsbreit

`--dichte-beschriftung`. Die Zahl steht als benannte Größe bei den übrigen Dichtewerten
in `globals.css` und nicht als Zahl in der Komponente.

**Warum gedeckelt.** `Lieferantennummer beim Kunden_K_SAP` misst gegen die echte Schrift **249 px**.
Inhaltsbreit gesetzt bestimmte diese eine Beschriftung die Spalte für **alle** und nähme vier kurzen
(124–166 px) rund die Hälfte des Panels weg. Mit dem Deckel kostet sie zwei Zeilen in ihrer
**eigenen** Zelle und nimmt niemandem Platz. **Schlimmstenfalls ist eine Gruppe damit so hoch wie in
der gestapelten Form — nie höher.**

**Warum 10 rem und nicht mehr, gemessen und nicht geschätzt.** Im **Panel** — dem engeren der beiden
Einhängepunkte — ist die Gruppenzeile **454 px** breit. Bei 10 rem bleiben nach dem Spaltenabstand
**282 px** für die Werte. Der längste gemessene BAM-Wert hat **35 Zeichen** (M38, E6) und braucht als
Marke rund **285 px**. Die Spalte ist damit genau so breit, wie sie sein darf:

> **Eine breitere Beschriftungsspalte spart der Beschriftung eine Zeile und zwingt dafür den
> **Wert** in den Umbruch.** Das wäre die Regel aus
> [`frontend-grundlagen.md`](frontend-grundlagen.md) verkehrt herum — *in einer Zelle mit mehreren
> Angaben weicht die Hauptinformation nicht*. Die Belegnummern sind die Hauptinformation dieser
> Ansicht (§11), die Beschriftung ist Beiwerk nach dem Leitsatz. **Der Deckel liegt deshalb dort, wo
> der Wert gerade noch ganz hineinpasst, und nicht dort, wo die Beschriftung bequem läge.**

**Die Zeilen richten sich oben aus** (`align-items: start`). Mittig schwömme eine einzeilige
Beschriftung neben drei Zeilen Werten in der Mitte.

**Die Beschriftung bricht um, sie wird nicht gekürzt.** Damit entfällt auch der `title` mit dem
Vollwert, den die erste Fassung trug: Er wäre ein Versprechen auf etwas, das ohnehin dasteht.

### Der Deckel gehört zum Einhängepunkt — 10 rem im Panel, 16 rem auf der eigenen Route

*Nachgebessert am 13.08.2026, **nach** der Nacharbeit desselben Tages. Die 10 rem oben bleiben
richtig; sie waren nur nicht die ganze Antwort.*

Die 10 rem sind für das **Panel** gemessen und dort exakt richtig — 454 px Gruppenzeile, 282 px
bleiben den Werten, die längste Marke braucht 285 px. Die Detailansicht hat seit dem 11.08.2026 aber
einen **zweiten** Einhängepunkt: die eigene Route `/nachrichten/<id>` mit 72 rem Inhaltsbreite
(`--dichte-inhaltsbreite`). Dieselbe Gruppenzeile misst dort **1.126 px** statt 454 (§16). Bei
10 rem bliebe der Wertspalte davon **954 px** — und die längste gemessene Marke braucht rund
**285 px**. Die Beschriftungen brachen also um, ohne dass der Wert den Platz gebraucht hätte.
**Die Begründung für die 10 rem gilt dort schlicht nicht:** Sie ist die Rechnung *„was bleibt dem
Wert übrig"*, und dem Wert bleibt auf der Route reichlich.

Auf der eigenen Route sind es deshalb **16 rem = 256 px**. Die Zahl kommt aus derselben Messung wie
die 10 rem: Die längste Beschriftung des Bestands hat 35 Zeichen und misst gegen die echte Schrift
**249 px** (Sichtprüfung 13.08.2026, §15 Punkt 4). Es bleibt ein **Deckel** und keine feste Breite —
wird der Bestand im Altsystem länger, bricht die Beschriftung dort wieder um, und das ist richtig.

**Der Block erfährt nicht, wo er hängt.** Er liest `--dichte-beschriftung` wie bisher; die Route
setzt den Wert auf ihrem Wrapper herauf (`.beschriftung-breit` in `globals.css`, gesetzt in
`nachricht-seite.tsx`). Keine Zahl in einer Komponente, dieselbe Regel wie bei den Farbwerten. Dass
das genügt, ist **im gebauten CSS nachgesehen** und nicht angenommen: Wegen `@theme inline` steht in
der Utility-Klasse der Verweis selbst und nicht der aufgelöste Wert —

```css
@media (min-width:48rem){ … .md\:grid-cols-beschriftung{grid-template-columns:var(--dichte-beschriftung) minmax(0, 1fr)} … }
```

— und `var()` wird an dem Element aufgelöst, das die Klasse trägt. Ein zweites Dichtemaß und eine
Eigenschaft am Block wären dadurch entbehrlich.

**Was dabei gleich bleibt, und beides ist der Grund für einen festen Wert:**

- Der Rückfall auf die gestapelte Form unter **768 px** gilt auf **beiden** Einhängepunkten
  unverändert. **Kein neuer Umbruchpunkt.**
- **Alle Werte einer Gruppe beginnen bei derselben x-Position** — innerhalb eines Einhängepunkts,
  über alle Gruppen und über alle Nachrichten hinweg.

> **`fit-content(16rem)` wäre die knappere Variante und ist bewusst nicht gewählt.** Sie rückt die
> Spalte an die längste Beschriftung der *jeweiligen* Nachricht heran — und ließe damit die
> x-Position der Werte von Nachricht zu Nachricht wandern. Beim Blättern zwischen zwei Belegen wäre
> das eine Bewegung ohne Aussage.

### Am schmalen Fenster fällt der Block auf die gestapelte Form zurück

Unterhalb des **vorhandenen** Umbruchpunkts des Projekts (768 px,
[`visuelles-konzept.md`](visuelles-konzept.md) §6) steht die Beschriftung wieder **über** den Werten;
die Marken bleiben, wie sie sind. Zwei Spalten tragen dort nicht — 10 rem Beschriftung ließen bei
360 px Fensterbreite zu wenig für den Wert übrig.

**Kein neuer Umbruchpunkt.** Umgesetzt als `grid-cols-1 md:grid-cols-beschriftung`; `md` ist die
48 rem = 768 px des Projekts, nachgesehen im gebauten CSS.

> **Das ist von Hand zu prüfen und nicht durch den Browsertest.** `resize_window` meldet Erfolg und
> ändert `innerWidth` nicht ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Nachgesehen ist
> das **Regelwerk** — welche Klassen an welchem Umbruchpunkt greifen —, und das ist etwas anderes
> als eine Sichtprüfung. Der Posten steht in der Tabelle *Offene Sichtprüfungen* in
> [`README.md`](README.md).

### Was die Umstellung an Höhe spart

Gemessen am laufenden System, 13.08.2026, gegen echte Nachrichten:

| Nachricht | Werte / Gruppen | vorher (gerechnet) | nachher (gemessen) |
|---|---|---:|---:|
| sieben Typen zu je einem Wert | 7 / 7 | 478 px | **218 px** |
| vier gedeckelte Gruppen | 260 / 6 | 3.156 px | **~230 px** |
| `NEXANS`-Wurzel mit Leerzeichenwerten | 83 / 7 | 3.130 px | **~270 px** |

**Die alte Zahl ist gerechnet und nicht gemessen** — die alte Fassung steht nicht mehr —, aber sie
ist es aus den Maßen der Bauform: je Wert eine Zeile `--dichte-zeile` (36 px), je Gruppe eine
Beschriftungszeile (18 px) und der Zwischenraum. **Der Hauptposten ist nicht die eingesparte
Beschriftungszeile, sondern die 36-px-Zeile je Wert:** Eine Marke ist 22 px hoch, und es stehen
mehrere nebeneinander.

### Was ausdrücklich gleich geblieben ist

**Der Schlüssel der Liste ist weiter `(typ, wert)`** und niemals der Wert allein (§11, M37).
Unverändert außerdem: der Endpunkt und seine Antwortform, die Deckelung bei 20 je Gruppe, die
Sortierung der Werte, die Reihenfolge der Gruppen, `bamAnzahl`, die Zahl in der Überschrift, das
Aufklappverhalten und die Stelle, an der der Block sitzt. **Die Beschreibungsliste im Kopf
([`nachrichtendetail.md`](nachrichtendetail.md) §10.3) ist nicht mit umgestellt worden** — sie bleibt
inhaltsbreit, weil ihre Beschriftungen kurz und in der Zahl fest sind.

### ~~Warum die Marke nicht in `visuelles-konzept.md` steht~~ — ✔ **sie steht seit dem 13.08.2026 dort**

> **Erledigt in Schritt 7, Teil 3.** Der Absatz unten hatte die Bedingung selbst formuliert —
> *„Wenn die BAM-Suche in Teil 2 oder 3 dieselbe Marke braucht, wandert sie"* —, und sie ist
> eingetreten: Die Begriffe der Belegsuche tragen dieselbe Gestalt
> ([`bam-suche.md`](bam-suche.md) §11.2). Der Code liegt seitdem an **einer** Stelle
> (`components/marke.tsx`), die Bauform samt ihrer Begründung in
> [`visuelles-konzept.md`](visuelles-konzept.md) §5.
>
> **Am Block ändert das nichts, und das ist der Punkt:** Die Bedienbarkeit ist dort ein
> **Schalter**, der nicht per Voreinstellung anspringt. Ohne Schließen-Schaltfläche bleibt die
> Marke, was sie hier ist — **kein Knopf**, kein Zeigerwechsel, kein Fokusrahmen, kein `title`. Der
> Baum des Blocks ist unverändert: Er setzt dieselbe Klassenliste weiterhin unmittelbar auf sein
> `<li>`, statt eine Marke darin zu verschachteln.
>
> Der ursprüngliche Absatz bleibt stehen, weil er die Bedingung enthält, unter der die Wanderung
> richtig war — und nicht, weil sie es immer gewesen wäre.

**Weil sie heute an genau einer Stelle vorkommt.** Das visuelle Konzept führt Farbrollen, Schrift-
und Dichtewerte — also das, was mehrere Ansichten teilen. Eine Bauform dort zu führen, die es einmal
gibt, machte es zur Sammelstelle; dieselbe Überlegung wie bei `common` im Backend (§10). **Das
Dichtemaß `--dichte-beschriftung` steht dagegen sehr wohl dort** ([`visuelles-konzept.md`](visuelles-konzept.md)
§5), denn die Maße wohnen in `globals.css` und ihre Tabelle ist deren Verzeichnis.

**Wenn die BAM-Suche in Teil 2 oder 3 dieselbe Marke braucht, wandert sie** — dann ist sie eine
gemeinsame Bauform und gehört ins Konzept. Vorher wäre es geraten, welche Form die Suche braucht.

### Der Schlüssel der Liste ist `(typ, wert)`

**Niemals der Wert allein.** M37 misst, dass bei **4,17 Prozent** der Paare derselbe Wert unter
mehreren Typen steht; ein Schlüssel aus dem Wert allein erzeugte dort doppelte React-Schlüssel — und
die sind für Vitest unsichtbar, weil die Tests keine vollständigen Komponentenbäume rendern
([`frontend-grundlagen.md`](frontend-grundlagen.md) §9). Innerhalb einer Nachricht ist das Paar
eindeutig: Der Primärschlüssel ist `(MessageID, MessageBAMType, MessageBAMValue)`.

Genau deshalb steht `typ` in der Antwort, obwohl die Oberfläche ihn nicht anzeigt.

### Tests

| Datei | Was |
|---|---|
| `tests/bam-beschriftung.test.ts` *(neu, 13.08.2026)* | **ohne Baum** — die Zerlegung als reine Funktion, an den **gemessenen** Fällen aus M45‑1: `Kundenmaterialnummer_K_SAP`, `Sender_Ident_FORS`, `Empf_Ident_FORS`, `Material-Nr. beim Lieferanten_L_SAP`, `Bestellnummer` und `9018` ohne Endung, `Beleg-Nr.··TSL·_L_SAP` mit seinen Leerzeichen. Dazu **beide Gegenproben** ausgeschrieben — das `i`-Flag und „ab dem letzten Unterstrich" — und die Zusicherung, dass Name plus Endung wieder die Beschriftung ergeben, Zeichen für Zeichen |
| `tests/bam-block.test.tsx` | **gerenderter Baum, begründete Ausnahme** — **vier** Fälle: derselbe Wert unter zwei Typen **ohne `console.error`** (die Regression zum Schlüssel); `bamAnzahl === 0` → **nicht im Baum und keine Anfrage**; eingeklappt mit Werten → Überschrift mit der Zahl, **und immer noch keine Anfrage**; seit dem 13.08.2026 die **Fuge** der zerlegten Beschriftung — vollständig und ohne eingefügtes Leerzeichen, geprüft über `textContent` |
| `tests/detail-baum.test.tsx` | unverändert, um `bamAnzahl: 0` ergänzt |

**Die Prüfung auf `console.error` ist ein Fehlschlagsgrund** (`tests/setup/konsole.ts`, seit Schritt 6
Nacharbeit Teil C), und eine pauschale Unterdrückung ist untersagt. Der erste der drei Tests hängt
vollständig daran: Er besteht genau dann, wenn keine Meldung fällt.

**Die Zahl der gerenderten Bäume ist von vier auf sieben gestiegen**, und das ist eine Entscheidung
und keine Bequemlichkeit — der Kopfkommentar in `tests/hilfe/rendern.tsx` führt sie mit Begründung.
Alle drei neuen sind dieselbe Klasse wie der Doppelschlüssel vom 11.08.2026: zweimal eine Aussage
über **Abwesenheit**, einmal eine Konsolenmeldung. Keiner davon ist ohne Baum belegbar.

---

## 12. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | der Endpunkt nimmt eine `MessageID`; Mandant aus der Sitzung über `MandantService`. Die Ausnahmeliste bleibt bei zwei Einträgen |
| **M2** Mandant als erster Pflichtparameter | `BamRepository`, alle drei Methoden; ArchUnit prüft es |
| **M3** Filter im Statement | `EXISTS` über `Process → ProjectMandant` in **jedem** Statement (§9), zusätzlich ohne DB geprüft |
| **M4** Isolationstest je Endpunkt | `BamIsolationDbIT` |
| **404 statt 403** | `RessourceNichtGefundenException`, fester Text; fremd und erfunden ununterscheidbar **in Rumpf und Laufzeit** (§9) |
| **M5** die Trennung gilt auch quer | auch die **Zählung** trägt den Filter — sonst nennte die Antwort eine Zahl über fremden Bestand |
| **L1** Pflicht-Zeitfenster | gilt für Listen über `Message`; hier ist die Menge über einen Primärschlüssel benannt, und `MessageBAM` trägt keinen Zeitstempel (§1) |
| **L2** keine Live-Aggregation | zwei `COUNT`-Formen über **eine** Nachricht, beide `Using index` — keine Kennzahl über `Message` (§8) |
| **L3** keine `OFFSET`-Paginierung | es wird nicht geblättert; gedeckelt wird über `ROW_NUMBER`, nicht über `OFFSET` |
| **L4** `MessageProperty` nur über `MessageID` | unberührt — dieser Endpunkt fasst `MessageProperty` nicht an. Die sinngemäße Regel für `MessageBAM` ist eingehalten: **Einstieg ausschließlich über die `MessageID`**, nie über den Wert (§1) |
| **L7 / §8 Regel 7** | jede der drei Abfragen mit `EXPLAIN` **und** Laufzeit gegen vier Gestalten und **zwei** Mandanten (§4) |
| **L8** keine Quelltabelle ohne Erhebung | `MessageBAM` ist seit M32 erhoben, `MessageBAMType` und `MessageBAMMandant` seit M7/M40 |
| **L9** kein Durchlauf ohne Zeitfenster in Anwendungscode | keiner. Die Herleitung der Bezugsnachrichten ist eine **Erhebung** und kein Anwendungscode; sie trägt ein Fenster, wo eines möglich war |
| **L10** Belegvermerk | **dreimal**: bei der Deckelung (§3), bei den Kosten von `bamAnzahl` (§8) und bei der Marke als Wortgrenze (§11a, E7) |
| **Q4** nicht geraten | die Endungen der Typbeschreibungen bleiben stehen — seit M45 **gemessen** statt vermutet (§13); ein unkonfigurierter Typ wird nicht gedeutet, sondern gezeigt; und die Werte werden **nicht** durch ein Trennzeichen getrennt, dessen Verträglichkeit mit den Daten niemand geprüft hat (§11a) |
| **S1** kein Schreibzugriff | ausschließlich `SELECT`, auch in M45 und E7 |
| **Leitsatz §1** | die Belegnummern stehen **vor** der Zeitleiste — sie sind die Frage, mit der der Nutzer kommt. Und sie bekommen die Breite: Die Beschriftungsspalte ist so gedeckelt, dass der Wert ganz hineinpasst, nicht die Beschriftung bequem liegt (§11a) |
| **In einer Zelle weicht die Hauptinformation nicht** ([`frontend-grundlagen.md`](frontend-grundlagen.md)) | ebenda — der Deckel von 10 rem folgt aus dieser Regel und nicht aus dem Augenmaß |
| **Kein Farbwert in einer Komponente** | der Block trägt **keine** eigene Farbe; er nutzt `--muted`, `--muted-foreground` und `--border` wie seine Nachbarn. Die Marke führt **keine neue Farbrolle** ein (§11a) |
| **Kein neuer Umbruchpunkt** ([`visuelles-konzept.md`](visuelles-konzept.md) §6) | der Block schaltet bei den 768 px des Projekts um, nicht bei einer eigenen Grenze (§11a) |
| **Keine Zeichenkette in einer Komponente** | alles in `texte.nachrichten.detail.bam`, beide Sprachdateien |

---

## 13. Offene Punkte

- ✔ **Die Endungen `_K_SAP`, `_L_SAP` und `_FORS` bleiben stehen — jetzt gemessen und nicht mehr
  vermutet** *(erledigt am 13.08.2026 durch **M45**)*. Hier stand die Frage, ob die Beschreibungen
  **ohne** ihre Endung eindeutig blieben; sie war offen, und eine Kürzungsregel wäre nach Regel Q4
  geraten gewesen. **Die Antwort ist: nein, und der Grund ist schärfer als erwartet.**

  | | |
  |---|---|
  | Endungen im Bestand | **genau drei** — `_L_SAP` (20 Typen), `_K_SAP` (13), `_FORS` (7). **22 von 62 tragen keine** |
  | Beschreibungen ungekürzt | **62**, paarweise verschieden |
  | Beschreibungen **gekürzt** | **57** — fünf gehen verloren, über drei Kollisionen und acht Typen |
  | Die Kollisionen | `Abladestelle` (9000 / 9016 / 9025) · `Bestellnummer` (**0** / 9027 / 9034) · `Rechnungsnummer` (**3** / 9024) |

  > **Zwei Befunde, die den Punkt endgültig schließen.**
  >
  > 1. **Zwei der drei Kollisionen treffen einen Typ *ohne* Endung** — die Grundtypen 0
  >    (`Bestellnummer`) und 3 (`Rechnungsnummer`), die von einer Kürzung gar nicht betroffen wären.
  >    Eine Ausnahmeregel an den 9xxx-Typen könnte den Fall deshalb nicht heilen.
  > 2. **Die Kollision trifft tatsächlich zusammen.** `Abladestelle_L_SAP` (9000) und
  >    `Abladestelle_K_SAP` (9016) stehen über Fenster B auf **3.405 Nachrichten gemeinsam** —
  >    11,3 % aller Nachrichten des Fensters mit einer `Abladestelle`. Gekürzt stünden dort **zwei
  >    Gruppen mit identischer Überschrift** untereinander, mit verschiedenen Werten und ohne jedes
  >    Unterscheidungsmerkmal. `NEXANS` konfiguriert zudem alle drei `Abladestelle`-Typen zugleich.
  >
  > **Was offen bleibt und die Anzeige nicht berührt:** *was* `_L_SAP`, `_K_SAP` und `_FORS`
  > bedeuten. Dass die Endung **unterscheidet**, ist gemessen — jede der drei Kollisionen paart
  > *verschiedene* Endungen, keine dieselbe. Dass sie das *System* oder die *Seite* benennt, ist eine
  > benannte **Vermutung** (M45‑5) und aus den Beschreibungen erschlossen; bestätigen kann sie nur
  > das Altsystem, nicht diese Datenbank.
  >
  > **Eine Kürzung „nur wo eindeutig" wäre technisch möglich** — 54 der 62 Typen blieben eindeutig —
  > und ist hier **nicht entschieden**: Sie zeigte die Beschriftung mal mit und mal ohne Endung, und
  > sie hinge an einer Stammdatentabelle, die sich ohne unser Zutun ändern kann.

- **Gedeckelte Gruppen lassen sich nicht nachladen.** Wer bei `ZAST` die 21. Rechnungsnummer sehen
  will, kann es nicht. Ein „mehr laden" bräuchte einen Cursor über `(MessageBAMType,
  MessageBAMValue)` — technisch geradeaus, der Index trägt ihn (M32: `MessageBAM_BAMValue` ist
  `(MessageBAMType, MessageBAMValue)`). **Gebaut wird es erst, wenn jemand es braucht.** Ob das je
  der Fall ist, hängt an einer Frage, die dieses Projekt nicht gemessen hat: ob ein Nutzer bei 441
  Rechnungsnummern auf einer Nachricht überhaupt eine *bestimmte* sucht — oder ob er dann die Suche
  aus Teil 3 nimmt, die genau diese Frage beantwortet.

- **Die Verteilung der Werte je (Nachricht, Typ) ist nicht erhoben.** Sie ist die Zahl, die die
  Deckelung von 20 bemessen würde; heute ruht sie auf der Verteilung je *Nachricht* (M41) und auf
  vier nachgemessenen Einzelfällen (§3, §4). Sie steht als offene Frage 6 in
  [`messungen-schritt7.md`](messungen-schritt7.md).

- **Das schmale Fenster ist nicht durch den Browsertest gesehen** *(fortgeschrieben am 13.08.2026)*.
  Die Browsersteuerung kann das Fenster nicht verkleinern
  ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8, *„Was der Browsertest nicht kann"*);
  geprüft ist das Regelwerk, nicht die Darstellung. **Seit der Nacharbeit ist der Posten größer als
  vorher**, und das ist der Grund, warum er hier ausdrücklich stehen bleibt: Der Block erbte bis zum
  12.08.2026 das Breitenverhalten des Panels und brachte **keinen eigenen** Umbruchpunkt mit — jetzt
  wechselt er bei 768 px die Anordnung (§11a). Der Umbruchpunkt selbst ist **kein neuer**, es ist der
  des Projekts; **was dort umschaltet, ist neu.** Nachgesehen ist, dass die Regel greift
  (`grid-cols-1 md:grid-cols-beschriftung`, `md` = 48 rem im gebauten CSS); **nicht** gesehen ist,
  wie die gestapelte Form aussieht. **Sie gehört von Hand nachgeholt** — Tabelle *Offene
  Sichtprüfungen* in [`README.md`](README.md), und was dabei zu sehen ist, steht in §15.

- **`WOC` ist der einzige beobachtete Fall eines unkonfigurierten Typs.** M40 hat über Fenster B bei
  allen sieben konfigurierten Mandanten eine **vollständige** Schnittmenge gefunden. Ob in der
  Produktion auch bei einem großen Mandanten unkonfigurierte Typen vorkommen, ist offen — der Block
  wäre darauf vorbereitet, weil er nicht siebt.

---

## 14. Sichtprüfung

**Durchgeführt am 13.08.2026** am laufenden System (`dev-start.ps1`, Backend `localhost:8080`,
Oberfläche `localhost:3000`), über die Browsersteuerung bei einem Sichtfeld von **1568 × 726 px**.
Punkt 1 und 2 mit einem Zugang, der **nur** `NEXANS` führt; Punkt 3 und 4 nach Mandantenwechsel mit
einem Zugang über alle zehn. **Punkt 5 bleibt offen** — er ist der einzige, den die Browsersteuerung
nicht kann.

| # | Zu prüfen | Erwartet | Befund |
|---|---|---|---|
| 1 | Eine `NEXANS`-Wurzel öffnen | Der Block steht **zwischen Kettenblock und Zeitleiste**, eingeklappt, mit der Zahl in der Überschrift. Aufklappen zeigt die Gruppen | ✔ An einer Split-Wurzel: Kopf → Kettenblock *„Wurde zu — 1 Teil"* → **`Belegdaten (8)`** → Zeitleiste → Eigenschaften. Eingeklappt, Zahl in der Überschrift; das Aufklappen zeigt acht Gruppen mit den Endungen `_L_SAP` |
| 2 | Ein **Merge-Eingang** (`TargetMessageID` gesetzt) | Der Block ist **nicht da**, und im Netzwerk steht **keine** Anfrage an `/bam` | ✔ Plakette *„Zusammengeführt"*, `bamAnzahl = 0`: kein Rahmen, kein Schalter, kein Satz — auf den Kettenblock folgt unmittelbar die Zeitleiste. Netzwerkprotokoll **vorher geleert**: null Anfragen auf `/bam` |
| 3 | Eine `ZAST`-Nachricht | Mindestens eine Gruppe zeigt 20 Werte plus Restangabe | ✔ `Belegdaten (3.901)`, eine Gruppe *Rechnungsnummer* (Typ 3): genau **20** Werte, aufsteigend, monospaced, darunter **„und 3.881 weitere"** (3.901 − 20) |
| 4 | Ein Typ ohne Konfiguration (`WOC`) | Er erscheint — hinten und ohne Markierung | ✔ *ohne Markierung*, mit Einschränkung bei *hinten* — siehe Befund 3 unten |
| 5 | **Schmales Fenster, von Hand** | Der Block bricht nicht aus dem Panel aus; lange Bezeichnungen werden gekürzt, der Vollwert steht im `title`; die Werte bleiben in fester Laufweite lesbar; es entsteht **keine zweite Bildlaufleiste** | **offen** — `resize_window` meldet Erfolg und ändert nichts ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Steht in der Tabelle *Offene Sichtprüfungen* in [`README.md`](README.md) |

### Was die Abnahme zusätzlich gezeigt hat

1. **Der Fall aus M37 stand live auf dem Schirm.** Die Wurzel aus Punkt 1 trägt unter
   *Lieferschein-Nr._L_SAP* **und** unter *Transport-Nummer_L_SAP* denselben Wert `6116231634` —
   genau die Gestalt, an der ein Schlüssel aus dem Wert allein zerbrochen wäre. Beide Gruppen zeigen
   ihn, und die Browserkonsole blieb dabei **leer** (nur DevTools-Hinweis und HMR). Das ist dieselbe
   Aussage, die `tests/bam-block.test.tsx` maschinell hält — hier zum ersten Mal an echten Daten.

2. **Die führenden Nullen stehen unverändert da.** Der `WOC`-Wert lautet `094930` und nicht `94930`.
   §7 ist damit nicht nur behauptet, sondern gesehen.

3. **`WOC` kann *hinten* nicht zeigen, und das liegt an `WOC`.** Der Mandant hat **null**
   konfigurierte Typen (M40) — seine einzige Gruppe ist damit zwangsläufig auch die letzte. Belegt
   ist an ihm deshalb nur die Hälfte der Zeile: *Der unkonfigurierte Typ 9014 erscheint, und er
   trägt keine Markierung.* Die Reihenfolge trägt weiterhin
   `BamServiceTest.unkonfigurierter_typ_erscheint_hinten`, wo konfigurierte und unkonfigurierte
   Typen nebeneinanderstehen. **Ein Mandant mit beidem zugleich ist im Bestand nicht bekannt** —
   dieselbe Lücke, die §13 als letzten offenen Punkt führt.

4. **Wo kein Kettenblock ist, rückt der Block auf.** Die zuerst geöffnete `NEXANS`-Nachricht trug
   keine Rolle; dort folgt `Belegdaten (7)` unmittelbar auf den Kopf. Die Regel lautet *zwischen
   Kettenblock und Zeitleiste* und nicht *unter einem Kettenblock* — sie hängt an keiner Bedingung,
   die der Nachbarblock erfüllen müsste.

5. **Nur beim Aufklappen geht eine Anfrage hinaus, und genau eine.** Mit geleertem Netzwerkprotokoll
   geprüft: Die geladene Seite mit `Belegdaten (8)` stellt keine Anfrage, der Klick auf den Schalter
   genau eine (`GET …/bam`, `200`).

6. **Am Rand notiert, weil es die nächste Abnahme sonst aufhält:** Die Daten von `ZAST` enden am
   **25.12.2025**, die Anwendungsuhr steht auf dem 30.12.2025. Im Standardfenster von 24 Stunden ist
   dieser Mandant deshalb **leer**; die Prüfung lief im freien Fenster. Das ist eine Eigenschaft der
   Testkopie (M3) und keine des Blocks.

---

## 15. Sichtprüfung der Nacharbeit (13.08.2026)

**Durchgeführt am 13.08.2026** am laufenden System (Backend `localhost:8080`, Oberfläche
`localhost:3000`), über die Browsersteuerung bei einem Sichtfeld von **1568 × 772 px**
(`innerWidth` 1920), mit einem Zugang über alle Mandanten, aktiv `NEXANS`. Geprüft wurden **beide**
Einhängepunkte: das Panel neben der Liste (Gruppenzeile **454 px**) und die eigene Route
(Gruppenzeile **954 px**).

**Punkt 6 bleibt offen** — er ist der einzige, den die Browsersteuerung nicht kann.

| # | Zu prüfen | Erwartet | Befund |
|---|---|---|---|
| 1 | Nachricht mit **einem** Wert je Typ | Eine Zeile je Typ, Block deutlich niedriger als vorher | ✔ Sieben Typen zu je einem Wert: **sieben Zeilen**, Block **218 px**. Gerechnet für die alte Bauform: 478 px. Die Zeitleiste steht im Panel **ohne Scrollen** darunter |
| 2 | Gedeckelte Gruppen mit Restangabe | 20 Marken, dahinter die Restangabe **ohne** Fläche | ✔ Eine Nachricht mit **260** Werten in sechs Gruppen: vier Gruppen mit genau **20** Marken und *„und 57 weitere"* bzw. *„und 7 weitere"* dahinter, gedämpft und ohne Fläche. Ganzer Block **~230 px** statt gerechneter 3.156 px |
| 3 | **Ein Wert mit Leerzeichen ist als *ein* Wert erkennbar** | Die Marke trägt den ganzen Wert | ✔ Eine `NEXANS`-Wurzel mit **83** Werten: `Kundenmaterialnummer_K_SAP` trägt dort **20** Werte der Gestalt `0Z3 915 902 D` — drei Leerzeichen, vier Blöcke, 14 bis 15 Zeichen. Jeder steht in **einer** Marke von 113 bis 121 px Breite und 22 px Höhe. Das ist der gemessene Fall aus E7, hier gesehen. **Die Werte selbst stehen nach Regel G1 nicht in dieser Datei** — abgedruckt ist die Gestalt, nicht der Beleg |
| 4 | Lange Beschriftung | Bricht in ihrer Spalte um, die Werte beginnen auf gleicher Höhe wie überall | ✔ `Lieferantennummer beim Kunden_K_SAP` (natürliche Breite **249 px**) bricht auf zwei Zeilen in der 160-px-Spalte; ebenso `Unsere Material-Nr._L_SAP` (am Bindestrich) und `Material-Nr. beim Lieferanten_L_SAP`. **Alle Werte beginnen bei derselben x-Position**, unabhängig von der Länge der Beschriftung |
| 5 | Marke und Farbrolle | Gedämpfte Fläche, kleiner Radius, feste Laufweite, kein Rahmen, **keine neue Farbrolle** | ✔ Am gerenderten Baum abgelesen: Hintergrund der Marke **zeichengleich mit `--muted`**, `border-width: 0`, `border-radius: 4,8 px`, Schrift `Geist Mono`, `overflow-wrap: break-word`. `align-items: flex-start`. Kein horizontaler Überlauf — weder am Dokument noch am Inhaltsbereich |
| 6 | **Schmales Fenster, von Hand** | Unter 768 px steht die Beschriftung wieder **über** den Werten; die Marken bleiben; keine zweite Bildlaufleiste | **offen** — `resize_window` meldet Erfolg und ändert nichts ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Nachgesehen ist nur das Regelwerk: Die Klassen lauten `grid grid-cols-1 … md:grid-cols-beschriftung`, und `md` steht im gebauten CSS als `@media (min-width:48rem)`. Steht in der Tabelle *Offene Sichtprüfungen* in [`README.md`](README.md) |
| 7 | Konsole | Keine Meldung | ✔ Nach einem Neuladen samt Aufklappen: **vier** Einträge, sämtlich Fremdmeldungen (React-DevTools-Hinweis, `[HMR] connected`). Kein `error`, keine Schlüsselmeldung |

### Was die Abnahme zusätzlich gezeigt hat

1. **Die eingesparte Höhe kommt nicht aus der Beschriftungszeile, sondern aus der Wertzeile.** Das
   war vor der Messung anders vermutet. Die alte Bauform gab jedem Wert eine volle `--dichte-zeile`
   (36 px); eine Marke ist **22 px** hoch, und es stehen mehrere nebeneinander. Bei einer Gruppe mit
   20 Werten ist das der Unterschied zwischen 720 px und rund 90 px — die gesparte
   Beschriftungszeile sind 18 px davon.

2. **Der Deckel von 10 rem ist im Panel keine großzügige Wahl, sondern die knappe.** Bei 454 px
   Gruppenbreite bleiben 282 px für die Werte, und die längste gemessene Marke braucht rund 285 px.
   **Ein Zentimeter mehr Beschriftung ginge unmittelbar zu Lasten der Belegnummer** — siehe die
   Begründung in §11a. Der Richtwert der Aufgabenstellung und die gemessene Grenze fallen hier
   zusammen; das ist ein günstiger Zufall und kein Beleg dafür, dass die Zahl an anderer Stelle
   ebenso trüge.

3. **Wo die Restangabe steht, hängt davon ab, wie die Marken auslaufen.** Bei
   `Material-Nr. beim Lieferanten_L_SAP` steht *„und 57 weitere"* unmittelbar hinter der letzten
   Marke in derselben Zeile; bei `Lieferplannummer_L_SAP` füllen die 20 Marken zwei Zeilen genau
   aus, und die Restangabe rückt auf eine dritte. **Beides ist richtig** — sie steht in derselben
   Zelle und hinter den Marken, und mehr war nicht verlangt.

4. **Ein Merge-Ergebnis mit 77 Eingängen schiebt den Block weit nach unten.** Das liegt am
   Kettenblock und nicht an den Belegdaten: Er listet die Eingänge einzeln. Die Belegdaten selbst
   sind auf dieser Nachricht 230 px hoch. **Der Posten gehört zum Kettenblock**
   ([`verkettung.md`](verkettung.md)) und ist hier nur notiert, damit die nächste Abnahme ihn nicht
   dem falschen Block zuschreibt.

---

## 16. Sichtprüfung der Nachbesserung (13.08.2026)

**Durchgeführt am 13.08.2026** am laufenden System (Backend `localhost:8080`, Oberfläche
`localhost:3000`), über die Browsersteuerung, `innerWidth` **1920 px**, Zugang über alle Mandanten,
aktiv **`NEXANS`**. Geprüft wurden **beide** Einhängepunkte: das Panel neben der Liste (Gruppenzeile
**454 px**) und die eigene Route (Gruppenzeile **1.126 px**).

**Die beiden Prüfnachrichten**, nach ihrer **Gestalt** gewählt und über den Listen- und den
Detail-Endpunkt gefunden; die Kennungen stehen nach Regel G1 nicht hier:

| | Gestalt |
|---|---|
| **sieben Gruppen** | `NEXANS`, VDA-Lieferabruf, **7 Typen zu je einem Wert**. Alle sieben Beschriftungen tragen `_K_SAP`, darunter die längste des Bestands (`Lieferantennummer beim Kunden_K_SAP`) und der Fall aus der Aufgabenstellung (`Kundenmaterialnummer_K_SAP`) |
| **gedeckelte Gruppen** | `NEXANS`, **378 Werte in 7 Gruppen**, davon **drei** gedeckelt (26 / 169 / 169 gegen je 20 gezeigte) |

**Punkt 9 bleibt offen** — er ist der einzige, den die Browsersteuerung nicht kann.

| # | Zu prüfen | Befund |
|---|---|---|
| 1 | Panel: **ein** Zeilenrechteck je Endung | ✔ Alle sieben Beschriftungen tragen eine Endung, und jedes Endungs-Element hat `getClientRects().length` = **1** — siebenmal `1`, keine Ausnahme. Der Namensteil bricht dabei weiterhin um: bei sechs der sieben auf zwei Rechtecke. `white-space` der Endung am gerenderten Baum: **`nowrap`** |
| 2 | Panel: die Höhe wächst nicht | ✔ **Wachstum 0 px** — und das ist direkt gemessen und nicht aus §15 hergeleitet: Am selben Baum, in derselben Sitzung, wurde die **alte** Form nachgestellt (`h3.textContent = h3.textContent`, also die Beschriftung als *ein* Textknoten) und neu vermessen. **312 px vorher wie nachher**, und je Beschriftung dieselben sieben Höhen (36/18/18/36/36/36/36 px). ⚠️ **Die ~218 px aus §15 sind nicht reproduziert**, und zwar nicht wegen dieser Änderung: Sie gehören zu einer Nachricht mit sieben **kurzen** Beschriftungen. Über sechs Seiten à 100 Nachrichten des Dezemberfensters trägt keine erreichbare `NEXANS`-Nachricht sieben Gruppen, deren Beschriftungen alle in 160 px passen — die Zahl ist damit **nicht nachgemessen**, das Kriterium *„darf nicht wachsen"* dagegen schon |
| 3 | Panel: Text vollständig, ohne eingefügtes Leerzeichen | ✔ Am gerenderten Baum über `textContent` abgelesen, nicht am Quelltext: `Lieferantennummer beim Kunden_K_SAP` · `Kundenwerk_K_SAP` · `Abladestelle_K_SAP` · `(JIT-) Abrufnummer_K_SAP` · `Kundenmaterialnummer_K_SAP` · `Bestellnummer vom Kunden_K_SAP` · `Lieferschein, Entnahme , PUS_K_SAP`. **Kein Leerzeichen vor einer Endung.** Im Bild bricht `Kundenmaterialnummer` / `_K_SAP` an der gemeinten Stelle — vorher stand dort `Kundenmaterialnummer_` / `K_SAP` |
| 4 | Eigene Route: keine Beschriftung bricht um | ✔ Alle sieben `h3` sind **18 px** hoch, also einzeilig. Die längste (`Lieferantennummer beim Kunden_K_SAP`) misst **249,5 px** bei einer Spalte von **256 px** — die Vorhersage aus §11a, hier gegen die echte Schrift gemessen. `grid-template-columns` am gerenderten Baum: **`256px 858px`**. `--dichte-beschriftung` am Wrapper der Route und an der Gruppe **`16rem`**, an `:root` unverändert **`10rem`** — die Überschreibung greift genau dort, wo sie soll, und nirgends sonst |
| 5 | Eigene Route: gedeckelte Gruppen | ✔ Drei Gruppen mit je genau **20** Marken und *„und 6 weitere"* bzw. zweimal *„und 149 weitere"*, jeweils **unmittelbar hinter der letzten Marke** (`previousElementSibling` trägt `data-wert`) und **ohne Fläche** (`background-color: rgba(0, 0, 0, 0)`). Zwei der drei laufen über **drei** Markenzeilen. **Kein waagerechter Überlauf:** `scrollWidth − clientWidth` ist **0** am Dokument, **0** am Inhaltsbereich und **0** an jeder der sieben Gruppen |
| 6 | Beide Einhängepunkte: gleiche x-Position | ✔ **Panel: 1.590 px**, über alle sieben Gruppen genau **ein** Wert. **Eigene Route: 509 px**, ebenfalls genau ein Wert — und derselbe bei **beiden** Prüfnachrichten, also auch über Nachrichten hinweg. Dass die beiden Zahlen sich unterscheiden, ist der Punkt: Sie gilt **innerhalb** eines Einhängepunkts |
| 7 | Konsole | ✔ Nach Leeren, Neuladen und Aufklappen **zwei** Einträge, beide Fremdmeldungen: der React-DevTools-Hinweis und `[HMR] connected`. **Kein `error`, keine Schlüsselmeldung** |
| 8 | Tests | ✔ `pnpm check` grün: Lint, Typprüfung, Prettier und **249 Tests in 15 Dateien**, einschließlich der neuen `tests/bam-beschriftung.test.ts` |
| 9 | **Schmales Fenster (< 768 px)** | **offen, nicht prüfbar** — `resize_window` meldet Erfolg und ändert `innerWidth` nicht ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Steht in der Tabelle *Offene Sichtprüfungen* in [`README.md`](README.md), dort um die Endung ergänzt. **Nicht als bestanden ausgegeben** |

### Was die Abnahme zusätzlich gezeigt hat

1. **Weg A trägt, und der Grund steht im gebauten CSS.** Nachgesehen, nicht angenommen:
   `.md\:grid-cols-beschriftung{grid-template-columns:var(--dichte-beschriftung) minmax(0, 1fr)}`
   innerhalb von `@media (min-width:48rem)`. Weil `globals.css` `@theme inline` benutzt, steht in
   der Utility-Klasse der **Verweis** und nicht der aufgelöste Wert — und `var()` wird an dem
   Element aufgelöst, das die Klasse trägt. Ein zweites Dichtemaß und eine Eigenschaft `breit` am
   Block (Weg B) waren dadurch entbehrlich. **Wer `@theme inline` je zu `@theme` ändert, bricht
   das**, und zwar lautlos: Die Route zeigte dann wieder 10 rem.

2. **Die Gruppenzeile der eigenen Route misst 1.126 px und nicht 1.152.** Die 1.152 px sind
   `--dichte-inhaltsbreite` (72 rem); die 26 px Differenz sind der Innenabstand und der Rahmen des
   Detail-Rahmens (`p-3` plus 1 px je Seite). **An der Rechnung ändert das nichts** — die 16 rem
   sind gegen die Beschriftung bemessen und nicht gegen die Zeile —, aber die Zahl gehört richtig
   notiert, damit die nächste Abnahme nicht 26 px sucht.

3. **Die Endung bricht auch dort nicht auf, wo der Name mehrfach umbricht.** Bei
   `Lieferschein, Entnahme , PUS_K_SAP` bricht der Name an zwei Stellen (Komma und Leerzeichen), und
   `PUS_K_SAP` steht trotzdem geschlossen — die Regel greift am *abschließenden* Lauf und nicht am
   ersten Unterstrich, den sie findet.

4. **`Kundenwerk_K_SAP` und `Abladestelle_K_SAP` bleiben einzeilig, obwohl sie eine Endung tragen.**
   Das ist die stille Gegenprobe zur Zerlegung: Sie zerteilt jede Beschriftung, aber sie *erzwingt*
   keinen Umbruch. Wer beim nächsten Umbau einen `<br>` oder ein `block` einsetzt, sieht es hier
   zuerst.
