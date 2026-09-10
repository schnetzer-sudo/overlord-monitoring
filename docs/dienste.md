# Dienste und Ablagen auf dem Dashboard

Stand: 10.09.2026 · Schritt 10d, Teil A · ergänzt [`dashboard.md`](dashboard.md)

**Zweck in zwei Sätzen.** Das Dashboard zeigt neben den mandantenbezogenen Kacheln einen
**plattformweiten Block**: je eine Lampe für jeden Dienst des Altsystems, der eine Zeitgrenze trägt,
und **eine** Kachel für die Ablagen. Er ist für jeden Mandanten identisch — er beschreibt nicht den
Bestand eines Mandanten, sondern den Zustand der Anlage, über die dessen Belege laufen.

> **Was hier ausdrücklich nicht steht.** Kein `ServiceName`, keine `ServiceDescription`, keine
> `ServiceLastStatusMessage` und **keine `ServiceConnectString`** — weder in einer Antwort noch in
> einer Protokollzeile oberhalb von `DEBUG` noch in dieser Datei (Regel G1). Genannt werden
> ausschließlich `ServiceID`-Kennungen.

---

## 1. Die Entscheidungen aus dem Sparring vom 10.09.2026

Die Nummern sind projektweit vergeben; die höchste vorher vergebene war **E‑115**
([`frontend-grundlagen.md`](frontend-grundlagen.md), die klebende Spalte). Jeder Treffer der Suche
ist einzeln gelesen worden — `E‑780` in
[`messungen-property-suche.md`](messungen-property-suche.md) ist der bekannte Falschtreffer, ein
Datenwert aus einer Rohausgabe und keine Vergabe.

| # | Entscheidung | Kern |
|---|---|---|
| **E‑116** | **Ort und Rolle** | Die Übersicht steht auf dem **Dashboard**, für **alle Rollen**, im selben Aufruf |
| **E‑117** | **Grün heißt `HEARTBEAT` bei `ServiceTimeout > 0`** | Abseits der Ablagen erscheint nur, wer eine Zeitgrenze trägt |
| **E‑118** | **Einordnung über den exakten Rohwert** | Kein Präfix, keine eigene Frist über `ServiceLastUpdate` |
| **E‑119** | **Die Ablagen sind eine Kachel** | Geprüft wird jede Ablage aus `ServiceDefaultFileStore`; die Werte der Spalte werden nicht erhoben |
| **E‑120** | **Der Zustand kommt aus einer echten Prüfung** | Über denselben Abrufweg wie die Rohdaten, im Hintergrund, im Speicher gehalten |
| **E‑121** | **Farben: bestehende Rollen** | Zweite Anwendung derselben Rollen; Umsetzung in Teil B |
| **E‑122** | **Angezeigt wird die `ServiceID`** | Vier Spalten von `Service` stehen in keiner Antwort |
| **E‑123** | **Regel M2 bekommt ihre dritte benannte Ausnahme** | Die eine Klasse, die `Service` liest |
| **E‑124** | **Ein eigener Klassifizierer für Dienste** | Nicht der `MessageStatusClassifier` — gleicher Rohwert, anderer Gegenstand |
| **E‑125** | **Ein Stand ohne Beleg ist *ungeklärt*, nicht rot** | Abgeschaltet, kein Durchgang, veraltet, kein Ziel — vier benannte Gründe |

### E‑116 — die Übersicht steht auf dem Dashboard, für alle Rollen

**Damit ist eine Zeile aus [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §9 aufgehoben** —
*„Service- und Heartbeat-Überwachung (`Service.ServiceStatus`) — Betriebssicht, nicht Kundensicht"*
stand dort unter **Nicht enthalten** —, und ein Teil von **Ausbaustufe 3** (§10, *„Service-Überwachung
für die interne Betreuung"*) ist vorgezogen. Beide Stellen tragen seit dem 10.09.2026 einen
datierten Korrekturblock; der alte Wortlaut bleibt stehen.

**Der Grund ist die Frage, die dieses Werkzeug beantworten soll.** Wer einen Beleg sucht und ihn
nicht findet, hat zwei mögliche Erklärungen: „er ist nicht da" und „die Anlage steht". Ohne diesen
Block ist die zweite nicht unterscheidbar, und der Nutzer sucht weiter.

### E‑117 — grün heißt `HEARTBEAT`, und nur Dienste mit Zeitgrenze bekommen eine Lampe

**Herkunft: Auskunft des Auftraggebers vom 10.09.2026. Nicht gemessen.** Was `ServiceTimeout` bei
einem Dienst genau bewacht — Einheit, Takt, wer ihn setzt —, ist unbekannt und wird für diese
Ansicht nicht gebraucht (offener Punkt 164).

**Was daraus folgt:** Die elf Ablagen (`ServiceTypeID = 1`) tragen `ServiceTimeout = 0` und
`HEARTBEAT` seit dem **07.06.2012** (M52). Eine Lampe „grün seit vierzehn Jahren" wäre die
schlechteste Auskunft, die diese Ansicht geben könnte — sie sähe aus wie eine Messung und wäre
eine Karteileiche. **Deshalb erscheinen die Ablagen nicht als Lampen, sondern als Kachel mit einer
echten Prüfung** (E‑119, E‑120).

### E‑118 — der exakte Rohwert, kein Präfix, keine eigene Frist

**Kein Präfixvergleich.** `ERROR_TIMEOUT` wird als **ganzer Wert** verglichen, nicht über
`LEFT(ServiceStatus, 6) = 'ERROR_'`. Der Grund ist nicht Regel Q1 — die verbietet nur `LIKE
'ERROR_%'` —, sondern der Gegenstand: Bei einem **Dienst** ist die Menge der Statuswerte klein,
bekannt und wird von einem Drift-Test bewacht (§5). Ein Präfix führte eine Verallgemeinerung ein,
für die es hier keinen Anlass gibt.

**Keine eigene Frist über `ServiceLastUpdate`.** Das Backend rechnet **nicht** aus, ob ein Dienst
„zu lange nichts gesagt hat" — es liefert den Zeitpunkt und sein Alter, und die Einordnung kommt
allein aus dem Rohwert. **Die Begründung ist dieselbe wie bei E‑71:** Eine Schwelle, die niemand
gesetzt hat, wäre erfunden (Regel Q4), und eine erfundene Schwelle in einem Überwachungswerkzeug
ist schlimmer als keine. Das Altsystem hat einen Wächter; **er** setzt `ERROR_TIMEOUT`, und genau
den zeigt die Lampe.

### E‑119 und E‑120 — eine Kachel, und ihr Zustand ist gemessen statt abgelesen

**Ablagen senden keinen Heartbeat** (Auskunft vom 10.09.2026, nicht gemessen). Ihr `ServiceStatus`
ist deshalb keine Auskunft über ihre Erreichbarkeit — M52 zeigt für alle elf denselben
eingefrorenen Stand.

**Geprüft wird, was tatsächlich benutzt wird:** jede Ablage, die in `ServiceDefaultFileStore`
irgendeiner Zeile von `Service` eingetragen ist. Laut derselben Auskunft ist das in der Produktion
in der Regel **eine**, höchstens zwei. **Die Werte der Spalte werden nicht erhoben** — welche
Kennung dort steht, ist eine Angabe zum Lauf und keine Erhebung (§4).

**Die Prüfung läuft im Hintergrund und nicht in der Anfrage.** Ein Dashboard, das beim Aufruf eine
fremde Maschine anspricht, hängt an deren Zeitgrenzen; das Leistungsbudget der Landingpage
(500 ms) verträgt das nicht, und die Gegenprobe aus §4 zeigt warum — eine **abgeschaltete** Ablage
antwortet nicht sofort, sondern nach rund 2,7 Sekunden.

### E‑122 — angezeigt wird die `ServiceID`

`ServiceName`, `ServiceDescription`, `ServiceLastStatusMessage` und `ServiceConnectString` stehen
in **keiner** Antwort. Die ersten drei sind Betriebstexte des Altsystems und für den Nutzer ohne
Wert; die vierte fällt unter Regel G1.

> **Eine Beobachtung aus dem Screenshot vom 10.09.2026 gehört hierher, und sie ist *gesehen* und
> nicht *erhoben*:** Bei allen fünf Diensten mit `ERROR_TIMEOUT` steht `ServiceLastStatusMessage`
> weiterhin auf „Heartbeat". **Die Spalte trägt also den letzten *gemeldeten* Text und nicht den
> Grund des aktuellen Zustands.** Wäre sie in der Antwort, sagte sie „Heartbeat" neben einer roten
> Lampe. Das ist der zweite Grund, warum sie nicht darin steht.

---

## 2. Was gebaut wird — und was nicht

| | |
|---|---|
| **Gebaut** | Ein Block `plattform` in der vorhandenen Antwort von `GET /api/dashboard`: die Lampen und die Ablagenkachel |
| **Nicht gebaut** | Keine Oberfläche, keine Sprachschlüssel, keine Farbzuordnung — das ist **Teil B** |
| **Nicht gebaut** | Keine Lampen für Dienste mit `ServiceTimeout` 0 oder `NULL`, keine Einzellampen für Ablagen |
| **Nicht gebaut** | Keine eigene Frist über `ServiceLastUpdate`, keine Umrechnung von `ServiceTimeout` |
| **Nicht gebaut** | Keine Prüfung von Ablagen, die nicht in `ServiceDefaultFileStore` stehen |
| **Nicht gebaut** | Keine Alarmierung, kein Schreibzugriff, keine Migration, kein neuer Endpunkt |

---

## 3. Teil 1 — der Abrufweg liegt jetzt in `common`

*Fertig am 10.09.2026, verhaltensgleich.*

`Ablagezugriff`, `SaajAblagezugriff` und `Abrufergebnis` sind von `payload` nach `common` gewandert;
dazu zwei neue Typen, `Abrufzustand` und `Ablagegrenzen`. **Der Grund ist ein zweiter Verbraucher
und keine Umgestaltung:** Die Ablagenprüfung dieses Schritts nimmt denselben Weg wie der
Rohdatenabruf, und zwei Fachpakete dürfen einander nicht kennen
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6).

Vollständig samt Tabelle der verschobenen Klassen im Korrekturblock in
[`rohdaten-backend.md`](rohdaten-backend.md) §12. **Kein Verhalten der drei Rohdaten-Endpunkte hat
sich geändert**; die sechs Testklassen dieses Abschnitts sind inhaltlich unverändert grün.

---

## 4. Teil 2 — M174: was eine Ablage auf die Null-UUID antwortet

*Gefahren am 10.09.2026 · `MessungM174DbIT` · sequenziell, mit Wanduhr*

### Die Frage

Die Kachel soll die Erreichbarkeit einer Ablage feststellen, **ohne eine echte Datei zu holen**: ein
`RETRIEVE` mit der **Null-UUID** `00000000-0000-0000-0000-000000000000`. Sie ist syntaktisch gültig
und zeigt mit Sicherheit auf keine Datei. **Trägt die Einordnung des Rohdatenabrufs diese Frage?**
Eine erreichbare Ablage muss „Datei nicht vorhanden" antworten, eine abgeschaltete „Ablage nicht
erreichbar".

**Warum über den verschobenen `Ablagezugriff` und nicht über den Alt-Client:** weil der Dauerlauf
genau diesen Weg nimmt. M66 und M71 sind mit dem Alt-Client gefahren; was dort gemessen wurde, gilt
für den Alt-Client. Diese Messung nimmt außerdem die **Zeitgrenzen der Anwendung** mit.

### Die vorregistrierte Deutung — sie stand vor dem Lauf fest

| Ergebnis | Folge | Eingetreten? |
|---|---|---|
| Jedes Ziel „Datei nicht vorhanden"; `07` „Ablage nicht erreichbar" | Die Einordnung trägt. Weiter mit Teil 3 | **ja** |
| Ein Ziel antwortet anders (Fault, Daten, sonstiger Zustand) | **Anhalten und melden** | nein |
| `07` antwortet mit „Datei nicht vorhanden" | `07` läuft wieder, die Auskunft vom 17.08.2026 ist überholt | nein — `07` ist weiterhin aus |
| Ein Ziel ist `07` oder `08` oder löst auf keine Zeile auf | Melden, nicht auflösen | nein |
| Die Ziele sind nicht genau `09` und `10` | **Melden** | **ja — es ist genau eines** |
| Ein erreichbares Ziel braucht deutlich länger als die 38 bis 244 ms aus M66/M60, **oder die Gegenprobe endet nicht sofort** | **Melden** | **ja — die Gegenprobe braucht 2,7 s** |

### Das Ergebnis

**Zwei Läufe, dieselbe JVM-Bauform, 9 Minuten auseinander.** Der zweite ist keine Wiederholung aus
Zweifel, sondern die Gegenprobe darauf, dass die Zahlen keine Momentaufnahme sind.

| | Lauf 1 (11:50) | Lauf 2 (11:59) |
|---|---|---|
| Ziele aus `ServiceDefaultFileStore` | `FILESTOREPROD10` | `FILESTOREPROD10` |
| Zahl der Ziele | **1** | **1** |
| `FILESTOREPROD10` — Einordnung | `DATEI_NICHT_VORHANDEN` | `DATEI_NICHT_VORHANDEN` |
| `FILESTOREPROD10` — Rohantwort | `Response=Error (Skipped)` | `Response=Error (Skipped)` |
| `FILESTOREPROD10` — Anhang | keiner | keiner |
| `FILESTOREPROD10` — Dauer | **116,339 ms** | **136,472 ms** |
| `FILESTOREPROD07` (Gegenprobe) — Einordnung | `ABLAGE_NICHT_ERREICHBAR` | `ABLAGE_NICHT_ERREICHBAR` |
| `FILESTOREPROD07` — Rohantwort | `SOAPExceptionImpl`, kein `Response` | `SOAPExceptionImpl`, kein `Response` |
| `FILESTOREPROD07` — Dauer | **2.718,792 ms** | **2.716,546 ms** |

**Die Ziele werden als Angabe zum Lauf genannt** (Kennungen, Regel G1) und nicht als Erhebung der
Spalte: Ohne sie wäre nicht nachvollziehbar, wogegen gemessen worden ist. Wie viele Zeilen welchen
Wert tragen, ist **nicht** ermittelt worden.

### Befund 1 — die Einordnung des Rohdatenabrufs trägt die Frage

*Gemessen war:* Das eine eingetragene Ziel antwortet auf die Null-UUID mit `Error (Skipped)` im
Rumpf und **ohne Anhang**; der Abrufweg ordnet das als `DATEI_NICHT_VORHANDEN` ein. Die
abgeschaltete Ablage endet in einer `SOAPException` und wird als `ABLAGE_NICHT_ERREICHBAR`
eingeordnet. Beides in zwei Läufen gleich.

*Behauptet wird:* **Die Null-UUID wird von der Ablage genauso behandelt wie ein fehlender Verweis**
— sie ist damit als Prüfanfrage brauchbar, und die Kachel darf ihre Einordnung aus dem vorhandenen
Ergebnis nehmen, ohne die Antwort ein zweites Mal zu lesen.

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* **eine** erreichbare und **eine** abgeschaltete Ablage, je zwei Abrufe.
> *Behauptet wird:* eine Eigenschaft der Schnittstelle.
> **Die Lücke:** `Error (Skipped)` ist derselbe Rumpf, den M66 (2) für **153 echte, aber nicht mehr
> vorhandene Verweise** gemessen hat. Dass die Ablage zwischen „gibt es nicht" und „hat es nie
> gegeben" unterscheidet, ist **nicht** gemessen und wird auch nicht gebraucht.

### Befund 2 — es ist genau ein Ziel eingetragen, und das ist zu melden

*Gemessen war:* `SELECT DISTINCT ServiceDefaultFileStore` über alle Zeilen von `Service`, `NULL` und
Leerstring ausgenommen, liefert **einen** Wert: `FILESTOREPROD10`.

*Behauptet wird:* **Die Kachel prüft damit nicht alle Ablagen, auf die geschrieben wird.** M53
Befund 1 hat gemessen, dass zu jedem Zeitpunkt **zwei** Ablagen gleichzeitig beschrieben werden und
jeder Mandant auf beide verweist — im heutigen Zeitraum `FILESTOREPROD09` **und**
`FILESTOREPROD10`. Die Spalte nennt nur eine davon.

**Was daraus folgt und was nicht.** Die Kachel ist damit ein **Stichprobenwächter** und keine
Zusicherung über alle benutzten Ablagen: Fiele `FILESTOREPROD09` aus, bliebe sie grün, und ein Teil
der Belege wäre trotzdem nicht abrufbar. **Nicht aufgelöst wird das hier** — welcher Mechanismus
`ServiceDefaultFileStore` setzt und warum dort nur eine der beiden steht, ist unbekannt (Regel Q4).
**Offener Punkt 166.**

**Die Vorgabe des Auftrags ist unverändert umgesetzt:** Geprüft wird, was in der Spalte steht. Eine
Kachel, die sich ihre Ziele aus den Verweisen der letzten Nachrichten zusammensucht, wäre eine
andere Entscheidung und keine Nachbesserung.

### Befund 3 — die Gegenprobe endet nicht sofort, sondern nach 2,7 Sekunden

*Gemessen war:* Der Abruf gegen die abgeschaltete `FILESTOREPROD07` endet nach **2.718,792 ms**
bzw. **2.716,546 ms** — zweimal derselbe Wert auf 2 ms genau, und weit unterhalb der
Verbindungszeitgrenze von 5 s.

*Behauptet wird:* nichts über die Ursache. Die Gleichmäßigkeit spricht gegen eine Zeitgrenze und
für einen festen Verlauf im Netz oder in der Namensauflösung; **gemessen ist das nicht**, und
geraten wird hier nicht (Regel Q4). **Offener Punkt 167.**

**Was daraus für den Bau folgt, und es ist der Grund, warum dieser Befund zu melden war:** Ein
Durchgang über zwei Ziele kostet im schlechten Fall rund 5,5 Sekunden. **Das ist ein Vielfaches
dessen, was in einer Anfrage vertretbar wäre** — und damit die nachträgliche Bestätigung von
E‑120: Die Prüfung gehört in den Hintergrund, nicht in den Aufruf. Beim Takt von 60 Sekunden
(§7) ist der Abstand groß genug; `fixedDelay` sorgt zusätzlich dafür, dass ein langsamer Durchgang
den nächsten nicht überholt.

### Was die Messung nicht zeigt

1. **Die Antwortverarbeitung des `jakarta`-Zweigs mit einer *gelieferten* Datei** bleibt ungemessen
   ([`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 3). M174 misst ausschließlich den Fall
   **ohne** Datei — der Anhang wird nie gelesen. **Offener Punkt 165.**
2. **Ob `FILESTOREPROD09` auf die Null-UUID genauso antwortet**, ist nicht gemessen: Sie steht nicht
   in der Spalte und wurde deshalb nicht angesprochen. Der Auftrag nennt als Ziele, was die Spalte
   nennt.
3. **Die Zeitgrenze der Anwendung ist weiterhin nicht erprobt** (ebenda, Punkt 4). Beide Fälle des
   Laufs enden lange davor.

### Regel G1 im Lauf selbst

Der Lauf gibt aus: Kennungen, die Einordnung, das Attribut `Response` aus dem Rumpf und die Dauer.
**Die Verbindungszeichenkette erscheint nirgends.** `SaajAblagezugriff` schreibt sie auf `DEBUG`
(„Abruf fehlgeschlagen gegen …") — die Messung schaltet die Weitergabe dieses Protokolls deshalb
ab und übernimmt aus `DEBUG` **ausschließlich** die eine Zeile, die das Attribut `Response` trägt.

**Sie ist außerdem die einzige Stelle des Testbestands, die eine Ablage anspricht.** Jeder
eigentliche Test dieses Projekts ersetzt `Ablagezugriff` durch eine Attrappe.

---

## 5. Offene Punkte

| # | Punkt |
|---|---|
| **163** | **Bekäme eine Ablage eine Zeitgrenze, erschiene sie zusätzlich als Lampe.** Heute trifft das keine — alle elf tragen `ServiceTimeout = 0` (M52). Der Fall ist nicht gesondert behandelt: Die Abfrage nimmt, was `ServiceTimeout > 0` erfüllt, und das ist richtig so. Er stünde dann in beiden Abschnitten, als Lampe **und** als Ziel der Kachel |
| **164** | **Einheit und Takt der Zeitüberwachung im Altsystem sind unbekannt** und werden für diese Ansicht nicht gebraucht. `ServiceTimeout` steht bei allen betroffenen Diensten auf `600`; ob das Sekunden sind wie bei `MessageTimeout` (M8), ist **nicht gemessen** und wird nirgends umgerechnet |
| **165** | **Die Antwortverarbeitung des `jakarta`-Zweigs bleibt ungemessen** ([`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 3). M174 misst nur den Fall ohne Datei |
| **166** | **`ServiceDefaultFileStore` nennt nur eine der beiden gleichzeitig beschriebenen Ablagen** (M174 Befund 2 gegen M53 Befund 1). Die Kachel ist damit ein Stichprobenwächter. Welcher Mechanismus die Spalte setzt, ist unbekannt |
| **167** | **Ein Abruf gegen eine abgeschaltete Ablage endet nach rund 2,7 s**, reproduzierbar auf 2 ms genau (M174 Befund 3). Die Ursache ist nicht gemessen |
