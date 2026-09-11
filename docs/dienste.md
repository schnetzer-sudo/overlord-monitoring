# Dienste und Ablagen auf dem Dashboard

Stand: 10.09.2026 · Schritt 10d, **Teil A und Teil B** · ergänzt [`dashboard.md`](dashboard.md)

> **Teil B ist gebaut** — die Oberfläche zu diesem Block steht in
> [`dashboard-frontend.md`](dashboard-frontend.md) §5.8, die zwölf Aufnahmen ihrer Sichtprüfung in
> §12 derselben Datei. Sie bringt **E‑126 bis E‑137** und zwei offene Punkte (**168**, **169**).
> **Keine Backend-Änderung**: kein Feld, kein Statement, keine Migration — was hier steht, gilt
> unverändert.

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

> ### Die drei Auskünfte vom 10.09.2026 — **Belegvermerk nach Regel L10**
>
> Drei Aussagen dieses Schritts stammen aus einer **fachlichen Auskunft des Auftraggebers** und sind
> **nicht gemessen**. Sie tragen die Entscheidungen E‑117, E‑119 und E‑120, und sie stehen hier
> zusammen, damit niemand sie später für Messergebnisse hält.
>
> | # | Auskunft | Was daran gemessen ist |
> |---|---|---|
> | 1 | **`HEARTBEAT` bei `ServiceTimeout > 0` heißt: der Dienst meldet sich** | **nichts.** Gemessen ist allein, *welche* Werte in der Spalte stehen (M52) — nicht, was sie bedeuten |
> | 2 | **Ablagen senden keinen Heartbeat** | **nichts.** Gemessen ist, dass alle elf denselben eingefrorenen Stand vom 07.06.2012 tragen (M52) — das ist *verträglich* mit der Auskunft und belegt sie nicht |
> | 3 | **In der Produktion steht in `ServiceDefaultFileStore` in der Regel eine Ablage, höchstens zwei** | **nichts über die Produktion.** Auf der Testkopie ist es **genau eine** (M174) — eine Kopie, nicht die Produktion |
>
> **Warum das trotzdem trägt:** Keine der drei Auskünfte wird *gerechnet*. Aus 1 folgt eine
> Beschriftung, aus 2 und 3 folgt, dass die Ablagen **gefragt statt abgelesen** werden — und dieses
> Fragen ist gemessen (M174). **Wäre Auskunft 1 falsch**, zeigte die Lampe einen anderen Zustand als
> gemeint; sie zeigte weiterhin den Rohwert daneben, und der ist eine Tatsache.
>
> **Die eine Stelle, an der eine falsche Auskunft wehtäte, ist Nummer 3** — und sie ist mit M174
> Befund 2 bereits an ihrer Grenze: In jedem Fenster werden **zwei** Ablagen gleichzeitig
> beschrieben (M53), eingetragen ist **eine**. Deshalb steht die Folge davon als offener Punkt 166
> und nicht als Fußnote.

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
bekannt und wird von einem Drift-Test bewacht (§6). Ein Präfix führte eine Verallgemeinerung ein,
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

## 5. Teil 3 — das Lesen: `DienstLeseRepository`

**Genau eine Klasse in `dashboard` liest `Service`**, mit zwei Methoden und **ohne**
`MandantContext` (E‑123, §10).

### Die Lampen

```sql
SELECT ServiceID, ServiceStatus, ServiceLastUpdate
  FROM GlassfishDB.Service
 WHERE ServiceTimeout > 0
 ORDER BY ServiceID;
```

| | |
|---|---|
| **Drei Spalten und keine vierte** | `ServiceName`, `ServiceDescription`, `ServiceLastStatusMessage` und `ServiceConnectString` werden **nicht einmal gelesen**. Eine Spalte, die gar nicht gelesen wird, kann in keiner Antwort landen — das ist der strengere Schutz als ein Feld, das man beim Zusammenbau wegläse |
| **`ServiceTimeout > 0` ist die ganze Bedingung** | Wer keine Zeitgrenze hat, kann sie auch nicht reißen. Zeilen mit `NULL` fallen durch den Vergleich von selbst heraus, und das ist die richtige Richtung: `NULL` heißt nicht „Grenze null", sondern „keine Angabe" |
| **`ORDER BY ServiceID`, nicht nach Zustand** | Sortierte man nach Zustand, spränge eine Lampe an eine andere Stelle, sobald sich ihr Zustand ändert — und genau dann sucht jemand sie an ihrem alten Platz |

### Die Prüfziele

```sql
SELECT DISTINCT s.ServiceDefaultFileStore, ziel.ServiceConnectString
  FROM GlassfishDB.Service s
  LEFT JOIN GlassfishDB.Service ziel ON ziel.ServiceID = s.ServiceDefaultFileStore
 WHERE s.ServiceDefaultFileStore IS NOT NULL
   AND s.ServiceDefaultFileStore <> ''
 ORDER BY s.ServiceDefaultFileStore;
```

| | |
|---|---|
| **`LEFT JOIN` und kein `JOIN`** | Eine Kennung, die auf keine Zeile auflöst, soll **erscheinen** und nicht verschwinden. Ein `JOIN` ließe die Kachel grün bleiben, weil das unauflösbare Ziel gar nicht erst geprüft würde — die Zeile ist der ganze Unterschied zwischen „rot" und „unbemerkt" |
| **`NULL` und Leerstring gelten beide als leer** | Unter der Sortierung `PAD SPACE` fällt eine Zeichenkette aus lauter Leerzeichen ebenfalls unter `<> ''`. Den Rest fängt `Ablagenziel.aufloesbar()` — ein leerer `ServiceConnectString` ist dasselbe wie keine Zeile |
| **Der `ServiceConnectString` geht von hier nur an den `Ablagezugriff`** | Er steht in keiner Antwort, in keiner Protokollzeile oberhalb von `DEBUG` und in keiner Fehlermeldung (Regel G1). `Ablagenziel` überschreibt `toString()` wie `Artefaktverweis` — auch ein versehentliches `log.info("{}", ziel)` gibt nichts preis |
| **Die Werte der Spalte werden nicht erhoben** (E‑119) | Die Methode liest, *wogegen* geprüft wird. Welche Zeile welchen Wert trägt und wie viele es sind, ist keine Frage dieses Schritts |

---

## 6. Die Einordnung der Dienste

**An genau einer Stelle: `DienstStatusClassifier` in `dashboard`.**

| Rohwert | Einordnung |
|---|---|
| `HEARTBEAT` | `MELDET_SICH` |
| `ERROR_TIMEOUT` | `ZEITUEBERSCHRITTEN` |
| `SHUTDOWN` | `HERUNTERGEFAHREN` |
| jeder andere Wert, `NULL` | `UNGEKLAERT` — **und der Rohwert wird mitgeliefert** (Regel Q4) |

### Warum nicht der `MessageStatusClassifier` (E‑124)

**Gleicher Rohwert, anderer Gegenstand.** `ERROR_TIMEOUT` steht in `Message.MessageStatus` *und* in
`Service.ServiceStatus`; bei einer Nachricht heißt es „der Wächter auf `RUNNING` hat zugeschlagen"
([`message-status.md`](message-status.md)), bei einem Dienst „dieser Dienst hat sich innerhalb seiner
Zeitgrenze nicht gemeldet". **Ein gemeinsamer Klassifizierer hätte zwei Gegenstände unter einem Namen
geführt** — und die nächste Ergänzung hätte einen davon still mitgeändert.

Was beide teilen, ist die **Bauform** und nicht der Inhalt: exakter Vergleich, unbekannte Werte nach
`UNGEKLAERT` mit Rohwert, und ein Drift-Test daneben.

### Die Sicherung gegen neue Statuswerte

`DienstkatalogDbIT` (`@Tag("db")`) hält zwei Dinge fest, nach dem Muster von
`DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge`:

1. **Kein unbekannter Wert** in `SELECT DISTINCT ServiceStatus` — die eigentliche Sicherung.
2. Die vorhandenen Werte sind **genau** die drei bekannten. Anders als beim Nachrichtenstatus gibt
   es hier keine Ausnahme wie `RUNNING`: Alle drei kommen vor.

**Nur deshalb ist es vertretbar, einen unbekannten Wert neutral zu behandeln.** Reagiert wird durch
Pflege des Klassifizierers und dieser Datei, nie durch Raten.

> **Warum das Regel T2 nicht verletzt.** `Service` ist keine veränderliche Tabelle: 20 Zeilen
> Stammdaten, auf der Testkopie ein eingefrorener Stand (M52 vom 14.08.2026, per Screenshot am
> 10.09.2026 als unverändert bestätigt). Und die Zusicherung nennt **keine Zahl**, sondern eine
> **Menge** — käme eine Zeile dazu oder fiele eine weg, bliebe der Test grün, solange kein neues
> Wort erscheint.

---

## 7. Die Prüfung: `Ablagenpruefung`

**Ein zeitgesteuerter Lauf in `dashboard`, außerhalb jeder Anfrage.**

### Die Schalter, nach dem Vorbild von `rollup.md` §8

| Schlüssel | Vorgabe | Wirkung |
|---|---|---|
| `overlord.ablagenpruefung.aktiv` | **`false`, wenn der Schlüssel fehlt** (`application.yml`: `true`, im `dev`-Block ausdrücklich `false`) | Ohne ihn gibt es die Bean gar nicht, und die Kachel sagt „abgeschaltet" |
| `overlord.ablagenpruefung.takt` | `60s` — **gesetzt, nicht gemessen** | Abstand zwischen zwei Durchgängen |

**Fehlt `aktiv`, ist die Prüfung aus.** Ein `boolean` ohne Angabe ist `false`, und das ist hier die
richtige Richtung: Ein Lauf, der wegen eines vergessenen Schlüssels unaufgefordert fremde Knoten
anspricht, wäre genau die Überraschung, die dieser Riegel verhindern soll.

**Kein Profil als dritter Riegel**, anders als beim Rollup: Der Job dort *schreibt* in die geteilte
Testkopie. Diese Prüfung liest zwei Stammdatenzeilen und schickt ein `RETRIEVE`, das
konstruktionsbedingt nichts ablegen kann (`messungen-schritt8.md`, Abschnitt QT1).

### `fixedDelay` und nicht `fixedRate`

**Ein Durchgang überholt den nächsten nie.** Bei `fixedRate` schickte ein Durchgang, der länger
braucht als sein Takt, den nächsten sofort hinterher — und ein Ziel, das nicht antwortet, kostet
allein rund 2,7 Sekunden (§4, Befund 3). Die Zeitgrenzen sind die des Rohdatenabrufs
(`common/Ablagegrenzen`: 5 s Verbindung, 15 s Antwort); mehr Schutz braucht es nicht.

### Ein Durchgang

1. **Die Ziele neu lesen** — je Durchgang, nicht einmal beim Start. Ein Eintrag in
   `ServiceDefaultFileStore` kann sich ändern, und ein Wechsel der Ablage ist genau der Vorgang, bei
   dem eine Überwachung nicht auf dem alten Ziel stehen bleiben darf. Zwei Stammdatenzeilen je
   Minute kosten nichts (M175).
2. **Je Ziel ein `RETRIEVE` mit der Null-UUID** `00000000-0000-0000-0000-000000000000`.
3. **Die Einordnung kommt aus dem vorhandenen Ergebnis, kein neues Parsen:**

| Was der Rohdatenabruf einordnet | Was die Kachel daraus macht |
|---|---|
| `DATEI_NICHT_VORHANDEN` | **erreichbar** — der Knoten lebt (M174, Befund 1) |
| `ABLAGE_NICHT_ERREICHBAR` | **nicht erreichbar** |
| `GELIEFERT` | **ungeklärt** — die Null-UUID zeigt auf keine Datei; kommt trotzdem eine, verhält sich der Knoten anders als gemessen |
| *Ziel nicht auflösbar* | **nicht erreichbar**, und es wird gar nicht erst gefragt — wie beim Rohdatenabruf ([`rohdaten-backend.md`](rohdaten-backend.md) §3) |

**Die gelieferten Bytes werden nicht angefasst** — kein Entpacken, kein Lesen, kein Protokollieren.

### Wo der Stand liegt

**Als unveränderlicher Satz im Speicher**, Prüfzeitpunkt aus der **Anwendungsuhr** (Regel Z1).
**Keine Tabelle, kein Eintrag im `audit_log`:** Das Protokoll hält fest, was ein *Nutzer* getan hat;
ein Hintergrundlauf im Minutentakt ist kein Abruf eines Nutzers und würde es fluten. Eine Tabelle
wäre eine zweite Wahrheit über einen Zustand, der ohnehin nur so lange gilt, wie er frisch ist.

**Die Folge ist gewollt:** Nach einem Neustart gibt es keinen Stand, und die Kachel sagt „noch kein
Durchgang" — statt einen alten Stand zu zeigen, für den niemand mehr einsteht.

**Scheitert ein Durchgang** (etwa weil die Datenbank nicht antwortet), wird **kein** Stand gesetzt:
Der alte altert weiter und wird nach zwei Takten „veraltet". Ein `catch`, das hier einen leeren Stand
schriebe, machte aus einem Datenbankfehler eine Aussage über die Ablagen.

---

## 8. Die Kachel: `Ablagenkachel`

**In dieser Reihenfolge geprüft**, und die Reihenfolge ist Entscheidung E‑125:

| # | Bedingung | Ergebnis |
|---|---|---|
| 1 | Die Prüfung ist **abgeschaltet** | `UNGEKLAERT`, Grund `ABGESCHALTET` |
| 2 | Sie hat **noch keinen Durchgang** | `UNGEKLAERT`, Grund `NOCH_KEIN_DURCHGANG` |
| 3 | Ihr letzter Stand ist älter als **zwei Takte** | `UNGEKLAERT`, Grund `STAND_VERALTET` |
| 4 | Es ist **kein Ziel** eingetragen | `UNGEKLAERT`, Grund `KEIN_ZIEL_EINGETRAGEN` |
| 5 | Ein Ziel ist **nicht erreichbar** | `NICHT_ERREICHBAR`, kein Grund |
| 6 | Sonst: ein Ziel ist **ungeklärt** | `UNGEKLAERT`, Grund `ZIEL_UNGEKLAERT` |
| 7 | Sonst | `ERREICHBAR`, kein Grund |

### E‑125 — ein Stand ohne Beleg ist *ungeklärt*, und zwar in jede Richtung

**Grün darf seinen Beleg nicht überleben** — aber Rot darf es genauso wenig. Ein Stand, der älter ist
als zwei Takte, sagt *nichts* über die Ablagen: Eine Kachel, die nach einem Ausfall der Prüfung auf
ihrem letzten roten Stand stehen bliebe, behauptete eine Störung, die niemand mehr geprüft hat; eine,
die auf dem letzten grünen stehen bliebe, behauptete das Gegenteil. **Beides ist dieselbe falsche
Auskunft**, und deshalb steht die Altersfrage **vor** der Frage nach dem Inhalt.

**Zwei Takte und nicht einer:** Ein Durchgang, der etwas länger braucht als sein Takt, ist kein
Befund — ein Abruf gegen eine abgeschaltete Ablage dauert allein rund 2,7 Sekunden (§4). Zwei Takte
lassen einen vollständigen Durchgang ausfallen, bevor die Kachel schweigt.

**Die alten Zielzeilen bleiben bei „veraltet" sichtbar.** Sie sind nicht mehr die Auskunft der
Kachel, aber sie sind das, was zuletzt festgestellt wurde — und der Prüfzeitpunkt daneben sagt, wie
alt das ist.

> **Ein Stand aus der Zukunft gilt als frisch.** Er entsteht nicht im Betrieb — Prüfzeitpunkt und
> `jetzt` kommen aus derselben Uhr —, und wenn doch, ist er kein Grund, eine gerade erhobene Auskunft
> wegzuwerfen. Dass sein Alter dann `null` ist, sagt es bereits (E‑75).

**Kein Zustand ohne Kachel.** Die Kachel steht **immer** in der Antwort, auch abgeschaltet. Eine
fehlende Kachel wäre Abwesenheit, und Abwesenheit ist der schwächste Kanal, den ein Zustand haben
kann — dieselbe Begründung wie bei E‑74 und E‑81.

---

## 9. Die Antwort: der Block `plattform`

**Im selben Aufruf von `GET /api/dashboard`** — kein Nachladen, kein neuer Endpunkt, für jeden
Mandanten identisch.

```jsonc
"plattform": {
  "dienste": [
    { "serviceId": "MPSERVICEPROD03", "zustand": "ZEITUEBERSCHRITTEN",
      "rohwert": "ERROR_TIMEOUT", "stand": "…Z", "alterSekunden": 6 }
  ],
  "ablagen": {
    "zustand": "ERREICHBAR",            // oder NICHT_ERREICHBAR / UNGEKLAERT
    "grund":   null,                     // nur bei UNGEKLAERT gesetzt, dort aber immer
    "ziele":   [ { "serviceId": "FILESTOREPROD10", "zustand": "ERREICHBAR" } ],
    "geprueftAm":    "…Z",
    "alterSekunden": 0
  }
}
```

| Feld | Anmerkung |
|---|---|
| `dienste[].serviceId` | **das Einzige, was den Dienst benennt** (E‑122) |
| `dienste[].rohwert` | immer mitgeliefert, auch bei bekannten Werten — der Anker für `UNGEKLAERT` (Regel Q4) |
| `dienste[].alterSekunden` | gegen die **Anwendungsuhr** wie `aeltesteSekunden` (E‑75); **`null`, wenn `stand` nach `jetzt` liegt** |
| `ablagen.grund` | benannt bei `UNGEKLAERT`, sonst `null`. Ein „ungeklärt" ohne Grund wäre ein Achselzucken |
| `ablagen.alterSekunden` | **der Beleg der Kachel** — wer wissen will, ob das grüne Ergebnis noch etwas wert ist, liest hier nach statt es zu glauben |

> ### ⚠️ Nachtrag vom 10.09.2026 — drei Felder werden von der Oberfläche **nicht mehr gelesen**
>
> **Die Tabelle darüber beschreibt den Stand von Teil B und bleibt Zeichen für Zeichen stehen: Am
> Vertrag ändert sich nichts.** `dienste[].stand`, `dienste[].alterSekunden`, `ablagen.geprueftAm`
> und `ablagen.alterSekunden` werden weiterhin geliefert, weiterhin gegen die **Anwendungsuhr**
> gerechnet (E‑75) und weiterhin durch Tests gedeckt.
>
> **Gelesen werden sie seit E‑138 nicht mehr.** Die Durchsicht am selben Tag hat den Block
> *Plattform* auf die **fünfte Kachel** der Kachelreihe zusammengezogen; darin steht je Dienst nur
> noch **Zeichen und `serviceId`**, und jeder Zeitpunkt und jedes Alter ist aus dem Bild gefallen
> ([`dashboard-frontend.md`](dashboard-frontend.md) §5.8).
>
> **Herausgenommen worden sind sie trotzdem nicht**, und das ist eine Entscheidung und kein Rest:
>
> | | |
> |---|---|
> | **`alterSekunden` ist der Beleg** | Die Zeile darüber sagt, warum es das Feld gibt — *wer wissen will, ob das grüne Ergebnis noch etwas wert ist, liest hier nach.* Dass die Anzeige heute nicht nachliest, macht die Frage nicht falsch; sie macht sie nur unbeantwortet (offener Punkt 168) |
> | **Ein Feld zu streichen ist teurer als es zu lassen** | Es hinge an Antwortklasse, Statement, Test und Vertrag. Der Nutzen wäre ein paar Bytes je Aufruf |
> | **Die Prüfung selbst braucht es** | `Ablagenkachel` entscheidet über das Alter, ob sie überhaupt etwas sagen darf (E‑125). Das Feld ist die **sichtbare Fassung derselben Zahl**, die die Kachel intern schon benutzt |

**Damit hat das Dashboard acht Blöcke und acht Statements.** Das achte sind die Lampen;
`DashboardStatementsTest` benennt es einzeln. **Die Ablagenprüfung steht nicht darin und darf es
nicht** — sie liegt außerhalb der Anfrage.

> ### ⚠️ Korrektur vom 10.09.2026 — „acht Blöcke" zählt 4a nicht mit
>
> **Der Satz darüber bleibt Zeichen für Zeichen stehen**, weil er in der Zählung *seiner eigenen
> Tabelle* richtig ist: [`dashboard.md`](dashboard.md) §2 führt neun Zeilen, aber acht Nummern —
> die Kachel *Wartend* steht dort als **4a** und nicht als 5.
>
> | | |
> |---|---|
> | **Blöcke** | **neun**, wenn man 4a mitzählt; **acht** Nummern, wenn man der Nummerierung folgt |
> | **Statements** | **acht** — und diese Zahl ist unstrittig, weil `DashboardStatementsTest` jedes einzeln benennt |
>
> **Beide Dateien sagen dasselbe und zählen verschieden.** Der Korrekturblock in
> [`dashboard.md`](dashboard.md) §2 nennt beide Zahlen nebeneinander; hier steht nur, warum sie
> nicht widersprüchlich sind. **Wer eine der beiden Stellen ändert, ändert beide** — oder er ändert
> keine.

---

## 10. Regel M2: die dritte benannte Ausnahme (E‑123)

> **Jede Repository-Methode, die `jooq.glassfish` anfasst, hat `MandantContext` als ersten
> Pflichtparameter.** Keine Überladung ohne ihn.

| | |
|---|---|
| **Wer** | genau eine Klasse: `dashboard/DienstLeseRepository` |
| **Was sie tut** | liest `Service` — `dienste()` und `pruefziele()` |
| **Warum zulässig** | **`Service` kennt keinen Mandanten.** 20 Zeilen Stammdaten, und keine Spalte verweist auf einen Mandanten, ein Projekt oder einen Prozess. Es gibt nichts zu filtern |
| **Warum kein Schein-Kontext** | Ein `MandantContext`, der entgegengenommen und im Statement nicht verwendet wird, sähe von außen wie Mandantentrennung aus und leistete nichts — genau das soll die Regel verhindern |
| **Wie sie technisch steht** | als **namentliche Liste** in `PaketstrukturTest` (`DIENST_AUSNAHME`), nach dem Muster von `ROLLUP_AUSNAHME`. Eine zweite Klasse in `dashboard`, die `jooq.glassfish` ohne Mandanten anfasst, fällt **nicht** von selbst darunter |

**Zwei Tests halten sie eng:**

- `dienst_ausnahme_ist_namentlich_und_eng` — die Liste ist nicht leer, nicht breiter als ihr Inhalt,
  und die Klasse trägt **nirgends** einen `MandantContext` in einer Signatur.
- `dienst_liest_nur_service_und_schreibt_nicht` — **die Ausnahme gilt für eine Tabelle und nicht für
  ein Paket.** Geprüft wird die Menge der angefassten `jooq.glassfish`-Typen (`Tables`, `Service`,
  `ServiceRecord`) und dass keine schreibende jOOQ-Methode gerufen wird. Fängt dieselbe Klasse an,
  `Message` zu lesen, ist die Begründung weg — und ohne diesen Test merkte es niemand.

**Und der Block selbst ist für jeden Mandanten identisch**, nachgewiesen in
`DashboardIsolationDbIT`. Das ist keine Lücke in der Trennung, sondern ihr Gegenstand: Er sagt nichts
über Belege, sondern über die Anlage, auf der sie laufen.

---

## 11. Die Messung der beiden Statements — M175 *(10.09.2026)*

**Regel 7:** „Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen (`EXPLAIN` plus
Laufzeit)." `MessungM175DbIT`, je beste von fünf nach einem Aufwärmlauf.

**Die Erwartung stand vor dem Lauf fest:** Vollzugriff über 20 Zeilen in der Größenordnung der
**1,348 ms aus M52** — die bei Leistungsregel L9 benannte Ausnahme für `Service`. Ein Plan mit
`type = ALL` ist hier **kein Befund**: Ein Index auf `ServiceTimeout` oder `ServiceDefaultFileStore`
existiert nicht und wäre bei 20 Zeilen ohne Wirkung. Zu melden wäre das Gegenteil — Zehntelsekunden
oder eine zweite Tabelle im Plan.

| Statement | Laufzeit | Plan | Zeilen im Ergebnis |
|---|---:|---|---:|
| **Lampen** (`ServiceTimeout > 0`) | **1,660 ms** | `Service` · `index` · `key=PRIMARY` · `rows=20` · `Using where` | **7** |
| **Prüfziele** (`DISTINCT` + `LEFT JOIN`) | **2,091 ms** | `Service` · `ALL` · `key=null` · `rows=20` · `Using where; Using temporary; Using filesort` — dazu `ablagenziel` · `eq_ref` · `key=PRIMARY` · `rows=1` | **1** |

**Beide liegen in der erwarteten Größenordnung**, und `Using temporary; Using filesort` über 20
Zeilen ist der Preis des `DISTINCT` — bei einer Stammdatentabelle dieser Größe folgenlos. Die
Auflösung des Ziels ist ein **Primärschlüsselzugriff** (`eq_ref`, `rows=1`), genau wie M52 es für die
Auflösung einer Ablagenkennung gemessen hat.

> **Belegvermerk (Regel L10).**
> *Gemessen ist:* beide Statements gegen die Testkopie, je beste von fünf, mit warmem Puffer.
> *Behauptet wird:* Sie sind für die Produktion unbedenklich.
> **Die Lücke:** `Service` hat dort dieselbe Größenordnung, ist aber nicht gezählt. Ein Kaltlauf ist
> auf der Testkopie ohnehin nicht messbar ([`messungen-schritt10b.md`](messungen-schritt10b.md)).

**Die Zusicherung des Laufs ist keine Zeit, sondern die Ursache** (Regel T1): dass der Plan
ausschließlich `Service` nennt. Kommt je eine zweite Tabelle dazu, fällt die Messung — und mit ihr
die Begründung der Ausnahme von Regel M2.

---

## 12. Tests

| Test | Was er sichert |
|---|---|
| `DienstStatusClassifierTest` | Alle vier Einordnungen, `NULL`, ein erfundener Wert — und **die beiden Fälle, die ein Präfixvergleich stillschweigend einsortiert hätte** (`ERROR_STARTUP`, `ERRORX`). Dazu: kein bekannter Wert fällt nach `UNGEKLAERT` |
| `AblagenkachelTest` | **Jeder Zustand und jeder Grund**, mit fester Uhr und erfundenen Kennungen. Darunter die beiden Richtungen von E‑125: ein veralteter Stand schlägt Grün **und** Rot; genau zwei Takte sind noch frisch |
| `AblagenpruefungTest` | Der Durchgang **mit ersetztem `Ablagezugriff`**: die drei Abrufzustände, das nicht auflösbare Ziel (wird nicht gefragt), die Null-UUID als Parameter, der Prüfzeitpunkt aus der Anwendungsuhr, die je Durchgang neu gelesenen Ziele, und der gescheiterte Durchgang, der **keinen** Stand setzt |
| `PlattformAntwortTest` | **Regel G1 an den Typen**: kein Feld der vier Antworttypen trägt `ServiceConnectString`, `ServiceName`, `ServiceDescription` oder `ServiceLastStatusMessage`; kein Antworttyp reicht ein `Ablagenziel` durch; `toString()` des Zieltyps gibt keine Verbindungszeichenkette preis |
| `DashboardServiceTest` | Der Block im Zusammenbau — Einordnung je Lampe, Alter gegen die Anwendungsuhr, **`null` bei einem Stand nach `jetzt`** (lokal der Fall von `MPSERVICEPROD01`), und die Kachel „abgeschaltet" |
| `DashboardStatementsTest` | Das **achte** Statement, einzeln benannt. Dazu die Gestalt beider neuen Abfragen: drei Spalten, keine der vier gesperrten, `LEFT JOIN` statt `JOIN`, **kein Mandantenfilter** (es gäbe nichts zu filtern) und keine zweite Tabelle |
| `DashboardIsolationDbIT` | **Der Block ist für `NEXANS` und `SUTTONS` identisch** — verglichen wird alles außer dem Alter (Regel T1). Dazu: kein `http://` und kein Betriebstext im Rumpf, die Kachel sagt lokal „abgeschaltet", ein erfundener Dienstparameter bleibt wirkungslos |
| `DienstkatalogDbIT` | Der **Drift-Test** (`@Tag("db")`): kein unbekannter `ServiceStatus`, die vorhandenen sind genau die bekannten, jede Lampe ist eingeordnet, die Lampen kommen sortiert und ohne Wiederholung |
| `PaketstrukturTest` | Die dritte benannte Ausnahme von Regel M2 — namentlich, eng, ehrlich; **nur `Service`**, **kein Schreibzugriff** |
| `MessungM174DbIT`, `MessungM175DbIT` | Die beiden Messungen. Keine Tests |

**Keine Wanduhr in einer Zusicherung** (Regel T1) und **kein Test spricht eine Ablage an** — die
einzige Ausnahme ist `MessungM174DbIT`, und sie ist als Messung ausgewiesen.

---

## 13. Die Abnahme, lokal gefahren *(10.09.2026)*

**Sieben Lampen, wie die Dev-Zeile des Auftrags vorhergesagt hat** — fünfmal
`ZEITUEBERSCHRITTEN`, zweimal `HERUNTERGEFAHREN`, **kein** `MELDET_SICH`. Anker der Anwendungsuhr im
Lauf: `2025-12-30 04:09:49`.

| `serviceId` | Einordnung | Rohwert | `alterSekunden` |
|---|---|---|---:|
| `COMSERVICEPROD00` | `HERUNTERGEFAHREN` | `SHUTDOWN` | 8.782.421 |
| `COMSERVICEPROD01` | `ZEITUEBERSCHRITTEN` | `ERROR_TIMEOUT` | **`null`** |
| `HTTPSERVICEPROD00` | `ZEITUEBERSCHRITTEN` | `ERROR_TIMEOUT` | **`null`** |
| `MPSERVICEPROD00` | `ZEITUEBERSCHRITTEN` | `ERROR_TIMEOUT` | **`null`** |
| `MPSERVICEPROD01` | `ZEITUEBERSCHRITTEN` | `ERROR_TIMEOUT` | **`null`** |
| `MPSERVICEPROD02` | `HERUNTERGEFAHREN` | `SHUTDOWN` | 8.782.363 |
| `MPSERVICEPROD03` | `ZEITUEBERSCHRITTEN` | `ERROR_TIMEOUT` | 6 |

**Die vier `null` sind richtig und erwartet.** `MPSERVICEPROD01` steht auf dem 13.07.2026 und damit
weit nach dem Anker; die drei übrigen tragen Zeitpunkte zwischen `04:09:56` und `04:10:18` und liegen
damit **Sekunden** nach dem Anker — genau der Fall, den die Dev-Zeile als „direkt nach dem Start ist
ihr Alter deshalb kurz `null`" benennt. **Der grüne Pfad (`MELDET_SICH`) ist gegen die Testkopie
nicht erreichbar** und allein durch Tests belegt, wie `RUNNING`.

**Die Kachel mit eingeschalteter Prüfung:**

| | |
|---|---|
| Zustand | **`ERREICHBAR`**, Grund `null` |
| Ziel | `FILESTOREPROD10` → `ERREICHBAR` |
| Prüfzeitpunkt | `2025-12-30T03:09:49Z` — das Datum des Ankers, **das Alter echt** (0 s) |

**Das ist genau der in Teil 2 gemessene Zustand** (§4). **Abgeschaltet zeigt sie `UNGEKLAERT` mit dem
Grund `ABGESCHALTET`** — festgehalten in `DashboardIsolationDbIT`, das im Profil `dev` läuft.

> **Der Lauf selbst ist ein Wegwerflauf gewesen** und liegt in keinem Commit: eine Testklasse mit
> `@TestPropertySource(properties = "overlord.ablagenpruefung.aktiv=true")`, die die Prüfung einmal
> von Hand auslöst und ihr Ergebnis ausgibt. **Sie ist nach dem Lauf gelöscht worden** — ein
> dauerhafter Test, der eine Ablage anspricht, wäre genau der, den §12 ausschließt.

---

## 14. Betriebshinweis

**Im reinen Rollup-Prozess gehört `overlord.ablagenpruefung.aktiv=false`.** Der Job dort aggregiert
`message_rollup` und bedient keine Oberfläche; er braucht keine fremden Knoten anzufragen, und zwei
Prozesse, die dieselbe Ablage im Minutentakt fragen, verdoppeln die Last ohne Nutzen.

```
SPRING_PROFILES_ACTIVE=prod,rollup \
  java -jar overlord-monitor.jar --overlord.ablagenpruefung.aktiv=false
```

**Lokal einschalten** — wie, steht in [`../START-LOKAL.md`](../START-LOKAL.md):

```
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--overlord.ablagenpruefung.aktiv=true
```

**Was dabei passiert, und es ist kein Versehen:** Die Anwendung spricht dann im Minutentakt die
produktive Ablage `FILESTOREPROD10` mit einem `RETRIEVE` auf die Null-UUID an. Das ist ein
Lesezugriff, der konstruktionsbedingt nichts ablegen kann — aber es ist ein Zugriff auf eine fremde
Anlage, und deshalb ist er lokal **aus** und wird einzeln eingeschaltet.

---

## 15. Offene Punkte

| # | Punkt |
|---|---|
| **163** | **Bekäme eine Ablage eine Zeitgrenze, erschiene sie zusätzlich als Lampe.** Heute trifft das keine — alle elf tragen `ServiceTimeout = 0` (M52). Der Fall ist nicht gesondert behandelt: Die Abfrage nimmt, was `ServiceTimeout > 0` erfüllt, und das ist richtig so. Er stünde dann in beiden Abschnitten, als Lampe **und** als Ziel der Kachel |
| **164** | **Einheit und Takt der Zeitüberwachung im Altsystem sind unbekannt** und werden für diese Ansicht nicht gebraucht. `ServiceTimeout` steht bei allen betroffenen Diensten auf `600`; ob das Sekunden sind wie bei `MessageTimeout` (M8), ist **nicht gemessen** und wird nirgends umgerechnet |
| **165** | **Die Antwortverarbeitung des `jakarta`-Zweigs bleibt ungemessen** ([`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 3). M174 misst nur den Fall ohne Datei |
| **166** | **`ServiceDefaultFileStore` nennt nur eine der beiden gleichzeitig beschriebenen Ablagen** (M174 Befund 2 gegen M53 Befund 1). Die Kachel ist damit ein Stichprobenwächter. Welcher Mechanismus die Spalte setzt, ist unbekannt |
| **167** | **Ein Abruf gegen eine abgeschaltete Ablage endet nach rund 2,7 s**, reproduzierbar auf 2 ms genau (M174 Befund 3). Die Ursache ist nicht gemessen |
| **168** | **Ein offener Tab lädt nicht nach** — die Anzeige zeigt den Stand ihres Aufrufs, während die Ablagenprüfung im Minutentakt weiterläuft. Gemessen und begründet in [`dashboard-frontend.md`](dashboard-frontend.md) §6.5 und §7.3; die Grenze betrifft **alle** Blöcke der Seite |
| **169** | **Im Leerzustand des Dashboards steht dieser Block nicht** (E‑p). Gerade dort wäre er am nützlichsten — der Leerzustand ist der Augenblick, in dem jemand wissen will, ob es an der Anlage liegt. Gemeldet, nicht aufgelöst ([`dashboard-frontend.md`](dashboard-frontend.md) §5.8) |
