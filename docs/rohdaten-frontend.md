# Rohdaten und Protokolle — die Oberfläche

Stand: 18.08.2026 · Schritt 8 des MVP, Teil Frontend
Vorgabe: [`rohdaten.md`](rohdaten.md). Bei Widersprüchen gilt jene Datei; alle Abweichungen sind
hier unter §10 benannt und begründet.
Bedient werden die drei Endpunkte aus [`rohdaten-backend.md`](rohdaten-backend.md).
Messungen: [`messungen-schritt8.md`](messungen-schritt8.md) M52–M72.

**Kein Backend.** Kein Endpunkt, keine Migration, kein jOOQ-Statement ist in diesem Schritt
angefasst worden.

> **Nachgebessert am 18.08.2026, am selben Tag.** Die Artefakte hängen jetzt **an der Zeitleiste**;
> der eigene Block „Dateien und Protokolle" ist vollständig entfallen. Der Anlass steht im Kasten zu
> Entscheidung 6 in [`rohdaten.md`](rohdaten.md) §3 — kurz: Der Block wiederholte mit neun Zeilen
> die Schrittnamen, die drei Zeilen darüber schon in der Zeitleiste standen, dort mit Dauer und
> Balken.
>
> **Diese Datei ist durchgehend auf die neue Fassung gezogen**, nicht mit einem Nachtrag versehen.
> Was sich geändert hat, steht in §3 und §3a; was gleich geblieben ist — die Ansicht, die vier
> Zustände, der Gleichlauf, der Umbruch —, steht unverändert dort, wo es stand. **Die drei
> Endpunkte sind unberührt.**

---

## 1. Was entsteht

| Ort | Was er beantwortet |
|---|---|
| **Ziele an der Zeitleiste** des Nachrichtendetails | *Welche Dateien hängen an diesem Schritt?* |
| **Eigene Route** `/nachrichten/{messageId}/dateien/{artefaktId}` | *Was steht in dieser Datei?* |

Beide Einhängepunkte des Nachrichtendetails bekommen die Ziele — das Panel neben der Liste und die
eigene Route. Sie entstehen in derselben Komponente wie bisher und brauchen dafür keine
Fallunterscheidung.

**Die Anzeige ist der Regelfall, nicht der Download** ([`rohdaten.md`](rohdaten.md) §1). Der
Download-Knopf steht in der Ansicht und nirgends sonst — die Begründung dafür steht in §6.

---

## 2. Der Punkt, der am ehesten schiefgeht: die Beschriftung

**Die Artefaktliste liefert keinen Namen.** Sie liefert `schritt` (die `MessageActionID`) und
`familie` (`Converter`, `FTPSender`, …). Das ist Abweichung 1 des Backends
([`rohdaten-backend.md`](rohdaten-backend.md) §10) und mit der Paketgrenze begründet: Die
dreistufige Namensauflösung liegt im Paket `message`, und Fachpakete kennen einander nicht.

**Die lesbare Beschriftung entsteht deshalb hier** — durch Verbindung mit der Schrittfolge aus
`GET /api/nachrichten/{messageId}`, deren `position` genau die `MessageActionID` ist. Das ist
dieselbe Verbindung, aus der seit dem 17.08.2026 die Gruppenköpfe des Eigenschaftenblocks entstehen
([`nachrichtendetail.md`](nachrichtendetail.md) §10.5), und es kostet **keine zweite Abfrage**:
Beide Datensätze liegen in der Detailansicht ohnehin im Baum.

### Die Regel

| Lage | Beschriftung | Anteil |
|---|---|---|
| Der **Eingang** (`Message.Payload.GUID`) | *Eingegangene Datei* | eine je Nachricht |
| Sonst auf Schritt `0` — die Lesedienste | die **Familie allein**, `SAPReader` | in Fenster A rund 85 % der Nachrichten (M57) |
| Der Schritt löst zu einem `SOSActionName` auf | dieser Name | **44,02 %** (M57) |
| Er löst **nicht** auf | `Schritt <n> · <Familie>` | Rest der **55,98 %** ohne Namen (M57) |

Gerechnet wird das in `features/nachrichten/rohdaten.ts` (`artefaktBeschriftung`), geprüft in
`tests/rohdaten.test.ts` — eine reine Funktion, weil es eine **Entscheidung** ist und keine
Darstellung.

> **Nichts wird geraten** (Regel Q4). Keine hübsche Umschrift der Familie, keine erfundene
> Bezeichnung, kein „vermutlich Umwandlung". `FTPSender` bleibt `FTPSender` — die Familie ist die
> Zeichenkette aus dem `MessagePropertyName` und wird als technischer Wert gezeigt, genau wie die
> Eigenschaftsnamen im Nachbarblock Rohwerte bleiben. Ein eigener Test hält das fest.
>
> Die Dienst**namen** aus `Service.ServiceName` bleiben unsichtbar (M15 (3)); sie kommen ohnehin
> nicht über die Schnittstelle.

### Warum auf Schritt `0` **nie** eine Schrittnummer steht

Er hängt auf Schritt `0`, und Schritt `0` ist der **Ort der Metadaten, kein Ablaufschritt** (M57,
M17 (3)) — er kommt in `schritte[]` gar nicht vor, weil das Backend ihn dort ausnimmt. „Schritt 0"
wäre technisch richtig und fachlich falsch. Dass es der Eingang ist, ist zudem **keine Ableitung
dieser Oberfläche**: Das Backend liefert ihn in einem eigenen Feld der Antwort.

> **Nachgezogen am 18.08.2026.** Dieselbe Begründung galt immer schon für die *anderen* Artefakte
> auf Schritt `0` — nur trug sie dort niemand ein. Für sie griff der allgemeine Rückfall und schrieb
> `Schritt 0 · SAPReader`, und genau dieser Eintrag war der sichtbare Widerspruch, mit dem die
> Nachbesserung anfing: **Die Zeitleiste zeigte drei Schritte, die Artefaktliste nannte vier.**
>
> Auf Schritt `0` liegt nicht nur der Eingang, sondern auch das Paar des Lesedienstes — gemessen
> `SAPReader` (443 Zeilen je Richtung), `FileReader` (4.199), `FTPReader` (111), `AS2Reader` (244),
> `MailReader` (32), `OFTPReader` (218), `OFTP2Reader` (46), `HTTPReader` (1), `SSHReader` (5),
> Fenster A. Sie tragen jetzt **ihre Familie allein**. Eine Nummer, die der Nutzer in der Zeitleiste
> nirgends wiederfindet, ist keine Auskunft.

### Ohne Schrittfolge trägt alles den Rückfall

Solange das Detail lädt — und auf der eigenen Route auch dann, wenn es gar nicht geladen werden
konnte —, ist `schritte` leer, und jede Zeile trägt `Schritt <n> · <Familie>`. Das ist der richtige
Zustand und kein halber: Nummer und Familie sind das, was ohne die Schrittfolge über ein Artefakt
bekannt ist. Dieselbe Bauform wie beim Eigenschaftenblock.

---

## 3. Die Ziele an der Zeitleiste

`features/nachrichten/components/artefakt-ziele.tsx`, eingehängt von
`components/nachricht-detail.tsx` und `components/zeitleiste.tsx`.

### Der Aufbau

```
┆ Eingang                                              📄 📄 📜
│ Datei konvertiert                    📄 📜   ▃▃▃▃▃▃▃▃      1,2 s
│ Datei versendet                          📜   ▃▃▃             0,4 s
│ Bestätigung verarbeitet                       ▃▃              0,2 s
▸ Technische Eigenschaften (23)
```

Ein Zeichen je Artefakt, in aller Regel zwei je Schritt — Datei und Protokoll. **Wo nichts liegt,
hängt nichts**: kein Platzhalter, kein leeres Zeichen, keine ausgegraute Stelle.

**Sichtbar ist allein das Zeichen.** Der Name steht für Vorleseprogramme im `sr-only`-Text und im
`title` der Rohname — gebaut wird beides in `rohdaten.ts` (`artefaktziel`), aus **derselben**
Funktion, aus der auch die Überschrift der Ansicht kommt. Das ist die Lektion vom 17.08.2026:
*Zwei Stellen, die denselben Schritt verschieden benennen, sind der Fehler.*

### Die eingegangene Datei steht einzeln, über der Leiste

**Auf Schritt `0` liegt nicht nur der Eingang.** Gemessen sitzen dort auch die Artefakte des
Lesedienstes, je Nachricht ein Paar aus Datei und Protokoll — `SAPReader`, `FileReader`,
`FTPReader`, `AS2Reader`, `MailReader`, `OFTPReader`, `OFTP2Reader`, `HTTPReader`, `SSHReader`
(M57, Fenster A). Die Zeitleiste führt Schritt `0` nicht: Das Backend nimmt den Metadaten-Schritt
aus `schritte[]` aus ([`nachrichtendetail.md`](nachrichtendetail.md) §4).

Er bekommt deshalb **eine eigene Zeile über der Leiste**, gestrichelt statt durchgezogen — dasselbe
Vokabular wie die erwartete Zeile am Ende der Leiste: Was gestrichelt ist, ist kein ausgeführter
Schritt. Kein Balken, keine Dauer.

Beschriftet wird sie mit **Eingang** und nicht mit *Eingegangene Datei*: Das ist der Name eines der
drei Artefakte darin, nicht der Name der Zeile. Die Artefakte des Lesedienstes heißen dort nach
**ihrer Familie allein** — `SAPReader`, nicht `Schritt 0 · SAPReader` (§2).

### Die Belastungsprobe sind fünfzehn Artefakte, nicht drei

Gemessen trägt jede Nachricht **3 bis 15** Artefakte und mindestens ein Protokoll, bei jedem
Mandanten (M55). Fünfzehn Ziele verteilen sich auf bis zu acht Schrittzeilen und die Eingangszeile
und kosten dort **keine einzige zusätzliche Zeile**. Genau das war der Grund für die Korrektur: Als
eigener Block waren dieselben fünfzehn Artefakte fünfzehn Zeilen mit sich wiederholenden
Schrittnamen.

Die Ziele tragen `shrink-0` und geben keine Breite ab; was bei wenig Platz weicht, ist der gekürzte
Schrittname daneben — die Regel aus [`nachrichtenliste.md`](nachrichtenliste.md) §8.1. **Name, Dauer
und Balken werden nicht verdrängt.**

### Der Ausschnitt hat seine sichtbare Marke verloren — und das ist Absicht

`beschnittMoeglich` ist **wahr nur bei Protokollen und nur für `MANDANT`**. Für einen gegebenen
Nutzer trifft es damit entweder auf *jedes* Protokoll zu oder auf keines; eine sichtbare Marke an
jedem Protokollzeichen sagte dasselbe wie das Zeichen selbst. Im alten Block stand sie neun Mal
untereinander.

**Angekündigt wird der Ausschnitt weiterhin, und weiterhin bevor jemand klickt:** im Namen für
Vorleseprogramme (*Protokoll · Datei konvertiert — Ausschnitt*) und im `title` für den Zeiger, dort
zusammen mit dem ausformulierten Satz. Beides, weil ein `title` auf einem Berührungsgerät nicht
erscheint und ein Vorleseprogramm ihn nicht verlässlich liest.

### Nichts wird ausgegraut

Ob hinter einem Protokoll für `MANDANT` etwas Anzeigbares liegt, weiß erst der Abruf — bei
`FTPSender` und `HTTPSender` in aller Regel nicht (M63). **Das Ziel wird trotzdem angeboten**; der
Zustand erscheint beim Öffnen als einer der vier benannten Texte (§5). Ein ausgegrautes Ziel wäre
eine Aussage, die das Backend nicht gemacht hat.

### Der Rest, den es gemessen nicht gibt

`zieleOhneZeile` sammelt ein, was weder Eingang noch Zeile der Zeitleiste ist, und stellt es
darunter in eine zweite gestrichelte Zeile: *Ohne Schritt in der Zeitleiste*.

**Gemessen kommt der Fall nicht vor** — `ohne_schrittzeile` ist in allen 63 Kombinationen aus
Fenster A **0** und in Fenster B über 1.516.642 Zeilen ebenfalls **0** (M57, Befund 1). Gebaut ist
die Zeile trotzdem, weil die Messung keine Zusage des Schemas ist: Seit die Artefakte an der
Zeitleiste hängen, fiele ein Artefakt ohne Zeile sonst **lautlos** aus der Oberfläche — keine
Meldung, kein leerer Kasten, nur eine Datei, die niemand mehr findet. Genau dafür trägt die
Beschriftungsregel ihren Rückfall `Schritt <n> · <Familie>` weiter (§2).

Solange sie leer ist, steht in der Oberfläche nichts davon.

### Nicht mehr eingeklappt — die Liste kommt mit dem Detail

Der Schalter gehörte dem Block, und den gibt es nicht mehr. Die Ziele hängen an der Zeitleiste, und
die steht immer da; ein Schalter, der erst geladen hätte, bliebe ohne Bedienelement.

**Der Preis ist eine Anfrage je Detailaufruf, und er ist gemessen klein:** 0,867 ms als reine
Datenbankabfrage ([`rohdaten-backend.md`](rohdaten-backend.md) §9). Vor allem aber **spricht sie
keine Ablage an** — sie liest `MessageProperty`. Der SOAP-Aufruf gegen den Filestore steckt allein
im *Inhalt* eines Artefakts (38 bis 244 ms je Datei, M66/M60), und der wird weiterhin erst beim
Öffnen der Ansicht geholt. Genau diese Trennung ist der Grund, warum das hier vertretbar ist und
dort nicht.

**Fällt die Abfrage aus**, steht die Zeitleiste weiterhin — sie hängt an einem anderen Endpunkt —
und es fehlen allein die Ziele. Gesagt wird es trotzdem, mit dem gewöhnlichen Fehlerbaustein unter
der Leiste: Sonst sähe eine Nachricht ohne erreichbare Dateien aus wie eine ohne Dateien, und die
gibt es gemessen nicht (M55).

---

## 3a. Der Weg zu den technischen Eigenschaften

**Ein Klick auf einen Schritt in der Zeitleiste führt zur zugehörigen Gruppe im
Eigenschaftenblock.** Kein neuer Block, keine Duplizierung — die Gruppierung nach Schritt besteht
seit dem 17.08.2026 ([`nachrichtendetail.md`](nachrichtendetail.md) §10.5), es fehlte nur der Weg
dorthin.

| | |
|---|---|
| **Was der Klick tut** | den Block aufklappen und den **Fokus** auf den Abschnitt dieses Schritts setzen |
| **Warum Fokus und kein Bildlauf** | Er bewegt die Ansicht ebenso, nimmt aber die Tastatur mit. Ein Bildlauf ohne Fokus lässt ein Vorleseprogramm dort stehen, wo es war |
| **Gibt es die Gruppe nicht** | bekommt der Bereich selbst den Fokus. Der Fall ist gemessen: `MessageActionID = 502` steht in `MessageAction`, kommt in `MessageProperty` aber nicht vor (M17 3) |
| **Gibt es gar keine Eigenschaften** | bleibt der Schrittname **Text und keine Schaltfläche**. Dieselbe Regel, aus der der Block bei `eigenschaftenAnzahl === 0` einen Satz statt eines Schalters zeigt |

**Der zugängliche Name enthält den sichtbaren** — *Technische Eigenschaften zu Datei konvertiert
anzeigen* —, wie es WCAG 2.5.3 verlangt: Wer die Schaltfläche per Sprache mit „Datei konvertiert"
anspricht, muss sie treffen. Der `title` bleibt die **Herkunft** des Namens; er beantwortet eine
andere Frage und wird nicht überladen.

**Aufgeklappt wird beim Rendern, nicht in einem Effekt.** Ein Sprung ist eine Änderung an einer
Eigenschaft, aus der sich der eigene Zustand ergibt; ein Effekt, der dafür `setState` ruft, löst
eine zweite Renderrunde aus — `react-hooks/set-state-in-effect` meldet das, und die Regel hat recht.
Der **Fokus** dagegen ist ein Effekt: Er ändert das Dokument und nicht den Zustand, und er kann erst
laufen, wenn die Gruppen im Baum stehen.

**Ein Klick, ein Sprung.** Das Sprungziel trägt eine laufende Nummer — sonst wäre der zweite Klick
auf dieselbe Zeile wirkungslos — und der Effekt merkt sich die zuletzt gesprungene; sonst spränge
die Ansicht auch dann wieder, wenn jemand den Block danach von Hand zu- und wieder aufklappt.

**Das Sprungziel trägt seine Nachricht mit sich und fällt beim Wechsel weg.** `NachrichtDetail` wird
beim Blättern zwischen Nachrichten nicht neu aufgebaut — nur ihre Kinder tragen ein `key`. Ohne das
Zurücksetzen klappte der Eigenschaftenblock der **nächsten** Nachricht von selbst auf, und wer
später zur ersten zurückkehrt, spränge dort ein zweites Mal. Auch das geschieht beim Rendern und
nicht in einem Effekt.

**Damit schließt zugleich der offene Punkt**, dass Block und Zeitleiste nach verschiedenen Spalten
ordneten (§11, Punkt 6 alte Fassung): Es gibt nur noch **eine** Ordnung, und das ist die der
Zeitleiste.

---

## 4. Die Ansicht auf ihrer eigenen Route

`/nachrichten/{messageId}/dateien/{artefaktId}` — `features/nachrichten/components/artefakt-ansicht.tsx`.

**Eigene Route, kein Sheet und kein Dialog** (Entscheidung 7). Das ist ausdrücklich eine spätere
Zugabe. Zwei Gründe, und beide folgen aus den Messungen:

1. **Verlinkbar.** „Schick mir mal den Link" ist bei diesem Werkzeug die eigentliche Anwendung, und
   bei einer Datei erst recht.
2. **Eigene Fläche, eigener Bildlauf.** Der Inhalt geht bis **609.995 Byte** (M60). Ein Sheet neben
   der Liste hätte dafür weder Breite noch Höhe, und es entstünde eine zweite Bildlaufleiste — genau
   das, was [`frontend-grundlagen.md`](frontend-grundlagen.md) §7 ausschließt.

### Der Aufbau

```
← Zurück zur Nachricht

Datei konvertiert                                          [ ⤓ Herunterladen ]
Converter.Payload.GUID · Nutzdaten · 12.480 Bytes · Kodierung ISO-8859-1

│ ℹ Du siehst den freigegebenen Ausschnitt dieses Protokolls. …
┌──────────────────────────────────────────────────────────────────────────┐
│ UNB+UNOC:3+…                                                             │
└──────────────────────────────────────────────────────────────────────────┘
```

**Die Herkunftszeile** — Rohname, Art, Größe und Kodierung in fester Laufweite — ist die Auskunft,
die das Altsystem nie gibt. Dort steht über dem Feld nichts, und wer eine leere Anzeige sieht, weiß
nicht, ob er ein Protokoll ohne freigegebenen Abschnitt vor sich hat, eine Binärdatei oder einen
Ausfall.

**Nichts steht dort, was nicht gemessen wäre.** Die Kodierung ist `ISO-8859-1`, weil 8 von 8
Protokollen und 9 von 16 Nutzdateien **kein** gültiges UTF-8 sind (M61) — sie wird nicht zur Laufzeit
erraten, und genau deshalb darf sie dastehen. Zwei Angaben erscheinen bedingt:

| Angabe | erscheint | warum nicht immer |
|---|---|---|
| Größe | wenn `groesseBytes > 0` | In den Zuständen ohne Inhalt liefert das Backend `0`, und „0 Bytes" wäre eine Aussage über eine Datei, die gar nicht abgerufen werden konnte |
| Kodierung | nur bei `ANZEIGBAR` | Sie beschreibt, wie *dieser* Text entstanden ist |

**Die Größe steht in rohen Bytes, ohne Umrechnung in KB oder MB.** Bei einem gemessenen Maximum von
609.995 Byte verlöre eine gerundete Angabe genau die Genauigkeit, mit der jemand zwei Fassungen
vergleicht. Dieselbe Entscheidung wie beim `gekapptHinweis` der technischen Eigenschaften.

### Die Anzeige selbst

- **Ein Textknoten in einem `<pre>`.** Niemals HTML: kein `dangerouslySetInnerHTML`, kein `iframe`,
  kein `srcDoc`, keine Blob-URL, kein `window.open` auf einen Inhalt. Eine EDI-Datei kann gültiges
  HTML oder SVG enthalten, und der Inhalt kommt **vom Partner** — er ist von außen befüllbar.
- **Kein Element je Zeile.** Bei 610 KB wären das Zehntausende Knoten und eine unbenutzbare Seite
  (M60). Daraus folgt unmittelbar: **keine Zeilennummern im MVP** — sie erzwingen die Zerlegung.
  Als offener Punkt notiert (§11), nicht heimlich eingebaut.
- **Festbreitenschrift, keine Umformatierung, keine Syntaxhervorhebung.** Die vier abgeschalteten
  Formatumwandlungen des Altsystems bleiben abgeschaltet (`PROJEKTBESCHREIBUNG.md` §9).

`tests/artefakt-ansicht.test.tsx` stellt einen erfundenen Inhalt, der **gültiges HTML ist**, und
weist nach: kein einziges Element im Feld, genau *ein* Kind, und das ist ein Textknoten. Damit sind
die Sicherheitsregel und die Bauvorgabe aus M60 in einem Test belegt.

### Drei Abfragen, und jede hat ihren Grund

| Abfrage | wofür | wann |
|---|---|---|
| `…/dateien/{id}/inhalt` | der Inhalt | **erst beim Öffnen der Ansicht** |
| `…/dateien` | der Eintrag zu dieser Kennung: Art, Schritt, Familie, Ausschnitt | aus dem Block im Zwischenspeicher |
| `…/{messageId}` | die Schrittfolge, **allein zum Beschriften** | ebenso |

Wer aus dem Nachrichtendetail hierher kommt, kostet die Ansicht **eine** Anfrage — die beiden
anderen liegen unter demselben Schlüssel bereits im Zwischenspeicher. Bei einem geteilten Verweis
sind es drei, und beide zusätzlichen sind reine Datenbankabfragen: **0,867 ms** für die Liste
([`rohdaten-backend.md`](rohdaten-backend.md) §9) und **rund 4 ms** für das Detail
([`nachrichtendetail.md`](nachrichtendetail.md) §8, vier Statements zusammen).

> **Berichtigt am 18.08.2026.** Hier stand „beide zusätzlichen sind reine Datenbankabfragen unter
> einer Millisekunde … 0,654 bis 0,867 ms für das Detail". Die 0,654 ms sind gemessen, gehören aber
> zum **Originaldateinamen** (Statement 4 in `rohdaten-backend.md` §9) und nicht zum Detail-Endpunkt.
> Der ist gemessen — nur woanders, und er ist viermal so teuer. **An der Aussage ändert das nichts**:
> Beide sind reine Datenbankabfragen und keine spricht eine Ablage an. An der Zahl ändert es etwas,
> und eine Zahl, die aus der falschen Zeile stammt, gehört korrigiert und nicht gerundet.

**Ohne sie stünde über der Datei eine Kennung statt eines Namens** — und dass die Zeile im Block und
die Überschrift der Ansicht **denselben** Namen sagen, ist der Grund, warum beide aus derselben
Funktion kommen. Es ist genau die Lektion aus der Nacharbeit vom 17.08.2026: *Zwei Stellen, die
denselben Schritt verschieden benennen, sind der Fehler.*

### Der Inhalt wird nicht mit der Liste geholt

Hinter jedem Abruf steht ein SOAP-Aufruf gegen die Ablage — 38 bis 244 ms je Datei (M66, M60) —, und
eine Nachricht trägt bis zu fünfzehn Artefakte. Die Liste vorzuladen hieße, fünfzehn fremde Anlagen
zu befragen, um drei Zeilen anzuzeigen.

**Der Inhalt wird auch nicht länger gehalten als die Vorgabe**, anders als Liste, Kette und
Belegdaten. Er beschreibt keinen Datenbankstand, sondern das Ergebnis eines Abrufs bei einer fremden
Anlage: *Ablage nicht erreichbar* ist ein Betriebszustand, der in einer Minute vorbei sein kann. Ihn
zu halten hieße, einen vorübergehenden Ausfall für eine Viertelstunde festzuschreiben.

---

## 5. Die vier Zustände

Jeder bekommt einen **eigenen, benannten Text**. **Keiner davon ist ein leeres Feld** — genau das
macht das Altsystem, und genau das ist der Unterschied.

| Zustand | Zeichen | Was der Nutzer liest | Zweiter Versuch |
|---|---|---|---|
| **Binärdatei** | `Binary` | *Diese Datei besteht nicht aus lesbarem Text und wird deshalb nicht angezeigt. Sie ist {bytes} Bytes groß.* | nein |
| **Kein anzeigbarer Protokollteil** | `ScrollText` | *Dieses Protokoll enthält keinen Abschnitt, der dir gezeigt wird. Bei vielen Schritten ist das der Normalfall und bedeutet nicht, dass etwas fehlgeschlagen ist.* | nein |
| **Datei nicht vorhanden** | `FileX` | *Die Dateiablage hat geantwortet und zu diesem Eintrag keine Datei geliefert. Möglicherweise ist ihre Aufbewahrungsfrist abgelaufen.* | nein |
| **Ablage nicht erreichbar** | `CloudOff` | *Die Dateiablage antwortet gerade nicht. Die Datei kann es weiterhin geben — versuche es in einigen Minuten erneut.* | **ja** |

### Die beiden letzten verschmelzen nicht — und der Unterschied wird nicht über Farbe getragen

Für den Betrieb ist genau diese Unterscheidung die wichtigere: *Die Ablage hat geantwortet und nichts
geliefert* ist etwas anderes als *die Ablage antwortet nicht*.

**Sichtbar wird der Unterschied über das Angebot, nicht über die Farbe:** Nur „Ablage nicht
erreichbar" bekommt eine Schaltfläche *Erneut versuchen*. Bei den anderen dreien ändert ein zweiter
Versuch nichts, und ein Knopf, der nichts bewirkt, ist eine Falschauskunft.

**Warum keine Farbe.** Rot hat in diesem Farbsystem genau eine Bedeutung — der `MessageStatus` ist
fehlgeschlagen ([`visuelles-konzept.md`](visuelles-konzept.md) §3: *„Es gibt keine zweite Bedeutung
von Grün oder Rot"*) —, und keiner dieser vier Zustände ist das. Alle vier tragen deshalb dieselbe
ruhige Gestalt: gestrichelte Kontur und gedämpfter Ton, wie der Leerzustand der Liste
(`components/zustand.tsx`). Die Entscheidung `erneutVersuchenSinnvoll` steht als reine Funktion in
`rohdaten.ts` und ist geprüft.

### Der zweite Zustand ist der häufigste, und der Text hält das aus

`FTPSender` trägt in **28 von 30** Fällen keine Marken und hängt an rund **69 %** der Nachrichten;
`HTTPSender` in **30 von 30** (M63). Für `MANDANT` ist eine leere Protokollansicht damit der
**Normalfall und kein Ausnahmezustand**. Der Satz sagt das ausdrücklich — *„Bei vielen Schritten ist
das der Normalfall und bedeutet nicht, dass etwas fehlgeschlagen ist"* — und vermeidet jedes Wort,
das nach Defekt klingt.

### Ein fünfter Fall, abgeleitet und nicht entschieden

**Eine Datei mit null Byte.** Sie ist nicht gemessen — das kleinste beobachtete Artefakt hat 2 Byte
(M60) —, aber die Antwort lässt sie zu: `zustand = ANZEIGBAR` bei leerem `text`. Ohne einen eigenen
Zweig stünde dort ein leerer Kasten, und genau den schließt [`rohdaten.md`](rohdaten.md) §8 aus. Er
bekommt deshalb einen benannten Text (*Leere Datei*). Dieselbe Bauform wie der fünfte Beschnittfall
des Backends: eine Ableitung aus einer bestehenden Regel, keine neue Entscheidung.

### Die Vermerke

Wo der Beschnitt gegriffen hat, **wird das sichtbar vermerkt** — der Nutzer soll wissen, dass er
einen Ausschnitt sieht. Ebenso, wenn die Kappung gegriffen hat.

| Vermerk | Auslöser | betrifft den Download |
|---|---|---|
| **Ausschnitt** | `beschnitten` — der Markenbeschnitt hat gegriffen | **ja**, er liefert denselben Ausschnitt (Gleichlauf) |
| **Kappung** | `gekuerzt` — die Anzeige endet an der Längengrenze | **nein**, die Kappung schützt den Browser, nicht die Vertraulichkeit |
| **Mehrere Archiveinträge** | `zipEintraege > 1` | — |

Sie stehen **zwischen Kopf und Inhalt**, weil sie gelesen sein müssen, bevor jemand den Text deutet.
Der Ausschnitt steht vorn: Er sagt als einziger etwas über den *Inhalt* aus — was fehlt, fehlt für
diesen Nutzer und fehlt auch im Download.

**Der dritte Vermerk ist in 693 geholten Dateien nie vorgekommen** und wird trotzdem gezeigt. Das
Altsystem verwirft den Rest stillschweigend (`JsonServlet.java:801`–`:802`); hier steht die Zahl in
der Antwort, und die Antwort steht auf dem Bildschirm ([`rohdaten.md`](rohdaten.md) §4).

`anzeigevermerke` ist eine reine Funktion; `tests/artefakt-ansicht.test.tsx` prüft zusätzlich im
gerenderten Baum, dass der Ausschnitt-Vermerk **fehlt**, wenn er nicht greift.

---

## 6. Der Download und der Gleichlauf

- **Über den Backend-Endpunkt, niemals ein Link auf den Filestore** ([`rohdaten.md`](rohdaten.md)
  §9). Der Aufruf geht an `/api/…` auf der Next.js-Adresse und wird vom Rewrite weitergereicht.
- **Ein gewöhnlicher Verweis** — keine Blob-URL, kein `window.open`, kein selbstgebauter Datenstrom.
  Der Browser sieht `Content-Disposition: attachment` und legt die Datei ab, ohne die Seite zu
  verlassen.
- **Kein `download`-Attribut.** Der Dateiname gehört dem Backend: Es baut ihn aus
  `%.FileProperty.OriginalFilename` auf demselben Schritt, wo es ihn gibt (69,6 %, M17), und
  bereinigt ihn von Steuerzeichen, Pfadangaben und Unicode-Formatzeichen. Ein zweiter Name hier
  liefe dem ersten hinterher, und der Browser zöge ihn vor.
- **Kein Download-Knopf in der Liste.** Ob sich ein Artefakt herunterladen lässt, weiß erst der
  Abruf.

### Die Oberfläche bietet keinen Knopf an, der etwas anderes verspricht als die Anzeige

Das ist Entscheidung 9 in der Oberfläche. `downloadMoeglich` in `rohdaten.ts` entscheidet es aus dem
Zustand der Anzeige:

| Zustand | Knopf | warum |
|---|---|---|
| `ANZEIGBAR` | **ja** | es gibt Bytes — auch beim beschnittenen Protokoll, und dann denselben Ausschnitt |
| `BINAERDATEI`, nicht beschnitten | **ja** | Nutzdatei oder `ADMIN`; beide bekommen die Datei ohnehin vollständig, die Anzeige kann Bytes nur nicht als Text darstellen |
| `BINAERDATEI`, beschnitten | nein | binäres Protokoll für `MANDANT` — der Endpunkt antwortet `409` |
| `KEIN_ANZEIGBARER_PROTOKOLLTEIL` | nein | `409` |
| `DATEI_NICHT_VORHANDEN` | nein | `404` |
| `ABLAGE_NICHT_ERREICHBAR` | nein | `502` |

> **`beschnitten` ist das verlässliche Kennzeichen, nicht die Rolle.** Die Oberfläche fragt nicht,
> wer der Nutzer ist — das Backend hat die Frage schon beantwortet, als es den Beschnitt anwandte.
> Eine zweite Ableitung aus der Rolle wäre eine zweite Wahrheit, und die falsche davon stünde im
> Browser.

**Kein ausgegrauter Knopf, keine Erklärung dahinter.** Ein Bedienelement, das verspricht, was die
Anzeige gerade verneint hat, ist genau der Widerspruch, gegen den Entscheidung 9 gerichtet ist. Im
Altsystem steht der Download-Knopf über einem leeren Feld und liefert die vollständige Datei — das
ist die Lücke, die hier nicht entsteht. `tests/artefakt-ansicht.test.tsx` weist für alle Lagen nach,
dass der Knopf **nicht im Baum** ist.

**Das BFF trifft dabei keine Berechtigungsentscheidung.** Next.js reicht durch; die Sichtbarkeit des
Knopfes ist Bequemlichkeit, verbindlich prüft das Backend in jedem Statement
([`frontend-grundlagen.md`](frontend-grundlagen.md) §2).

---

## 7. Schmale Fenster: Umbruch statt waagerechtem Bildlauf

**Die Entscheidung, und sie ist gemessen begründet.**

Eine Festbreitenanzeige mit langen Zeilen ist der unangenehmste Fall für schmale Ansichten. Zwei
Wege standen zur Wahl:

| Weg | Folge |
|---|---|
| `white-space: pre` + waagerechter Bildlauf | Zeilenfall bleibt originalgetreu, breite Zeilen werden geschoben |
| **`white-space: pre-wrap` + `overflow-wrap: break-word`** | nichts wird verborgen, der Zeilenfall der *Darstellung* ist ein anderer |

**Gewählt ist der Umbruch, und der Grund steht in M61: 36 von 206 Artefakten tragen überhaupt kein
Zeilenende.** Eine EDIFACT-Übertragung ist regelmäßig *eine* Zeile über die ganze Datei. Mit
waagerechtem Bildlauf sähe der Nutzer bei einer 610-KB-Zeile die ersten achtzig Zeichen und danach
eine Bildlaufleiste, deren Griff ein Pixel breit ist. **Der Umbruch verbirgt nichts; der waagerechte
Bildlauf verbirgt fast alles.**

**Der Preis, ausdrücklich getragen:** Eine Zeile, die im Original bis Spalte 300 lief, belegt hier
drei sichtbare Reihen. Der Text bleibt Zeichen für Zeichen derselbe. Da es ohnehin keine
Zeilennummern gibt — sie erzwängen ein Element je Zeile —, geht dabei **keine Angabe verloren, auf
die sich jemand beziehen könnte**.

`break-words` und nicht `break-all`: Gebrochen wird erst, wenn ein Stück sonst überliefe; Segmente
bleiben zusammen, solange sie passen.

### Eine Bildlaufleiste je Seite

Der einzige senkrechte Scrollbereich bleibt der Inhaltsbereich des Rahmens
([`frontend-grundlagen.md`](frontend-grundlagen.md) §7). Das Inhaltsfeld bekommt **keinen eigenen** —
dafür hat die Ansicht ihre eigene Route und damit die ganze Fläche. Und weil umgebrochen statt
geschoben wird, entsteht auch waagerecht keine.

### Die Ansicht nutzt die volle Inhaltsbreite

`/nachrichten/<id>` begrenzt sich auf `--dichte-inhaltsbreite` (72 rem), weil dort Beschriftungen
und Fließtext stehen. **Hier steht keiner.** Der Inhalt ist Rohtext in Festbreitenschrift, und weil
er umgebrochen wird, nimmt ihm jeder Pixel Breite einen künstlichen Zeilenumbruch weg. Eine
Maximalbreite gehört nach [`visuelles-konzept.md`](visuelles-konzept.md) §5 in eine Ansicht *mit
Fließtext*; die Sätze der Zustandsfelder tragen sie deshalb einzeln (`max-w-prose`), die Datei nicht.

> **Nicht gesehen, sondern entschieden.** Die Browsersteuerung kann das Fenster nicht verkleinern
> (`resize_window` meldet Erfolg, `innerWidth` bleibt stehen —
> [`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Geprüft ist damit das **Regelwerk**, nicht
> die Darstellung. Die Sichtprüfung steht aus, siehe §11.

---

## 8. Handwerk

| | |
|---|---|
| Datenholen | TanStack Query. Der Inhalt **erst beim Öffnen der Ansicht**, nicht mit der Liste |
| Zustand in der URL | Die Ansicht ist **Pfad, nicht Abfrage**. Der Aufklappzustand des Blocks steht nirgends — er ist keine Ansicht, die jemand teilt |
| BFF | Next.js reicht durch und trifft **keine** Berechtigungsentscheidung |
| Bildlaufleisten | **eine je Seite**, siehe §7 |
| Fehlerformat | RFC 9457 über `lib/http.ts`, übersetzt über den `type` — wie seit Schritt 3 |
| Farben | keine neue Farbrolle, kein Hex-Wert, keine Tailwind-Farbklasse (`tests/farbwerte.test.ts`) |
| Tests | Vitest. **`console.error` bleibt Fehlschlagsauslöser** |

### Der Verweis auf die Ansicht trägt keine Abfragezeichenkette

Anders als `ansichtOhneListe` und `ansichtNebenListe`, die den Filterzustand der Liste durchreichen.
Drei Gründe, und der erste ist der tragende:

1. **Es soll ein echter Verweis sein.** Der Filterzustand steht nur in `window.location.search`
   bereit; ihn beim Rendern zu lesen hieße `useSearchParams`, und der zwingt die Seite unter eine
   Suspense-Grenze — genau deshalb liest `nachricht-seite.tsx` ihn im *Ereignis*. Das geht bei einer
   Schaltfläche, nicht bei einem `<a>`. Und ein Dateiverweis, den man weder mit der mittleren
   Maustaste öffnen noch kopieren kann, verfehlt Entscheidung 7: **„verlinkbar" ist der ganze Grund
   für die eigene Route.**
2. **Ein geteilter Verweis auf eine Datei handelt von der Datei.** Welches Zeitfenster derjenige
   eingestellt hatte, der ihn verschickt, gehört nicht dazu.
3. Der Weg zurück bleibt gangbar: Die Ansicht führt an die **Nachricht** auf ihrer eigenen Route,
   und der Zurück-Knopf des Browsers führt Station für Station dorthin, wo jemand tatsächlich
   herkam — samt Filtern.

### Kein neues Farbtoken, kein neues Dichtemaß

Die Ziele und die Ansicht kommen mit den vorhandenen Rollen aus: `--muted` für die Fläche des
Inhaltsfelds, `--border` für Kontur und Vermerkslinie, `--dichte-zeile` für die Zeilenhöhe,
`--dichte-bedienelement` für Schalter und Verweise, die drei Schriftrollen unverändert.
`globals.css` ist in diesem Schritt **nicht angefasst worden**.

---

## 9. Tests

`pnpm test` — **19 Dateien, 336 Fälle**, alle grün. Auf dieses Feature entfallen drei Dateien mit
zusammen **46** Fällen.

| Datei | Art | Deckt ab |
|---|---|---|
| `tests/rohdaten.test.ts` | reine Funktionen, 29 Fälle | Beschriftungsregel in allen fünf Lagen, **einschließlich Familie allein auf Schritt `0`** · die **Ziele** je Schritt: Art vor dem Namen, keine Art beim Eingang, Ausschnitt in Name und `title`, Einteilung ohne Umsortieren, leere Einteilung ohne Liste, der Rest ohne Zeile in beide Richtungen · Gleichlauf über alle fünf Zustände samt der Ausnahme „binäres Protokoll" · Reihenfolge und Nachschlagen der Artefakte · die drei Vermerke · zweiter Versuch nur bei nicht erreichbarer Ablage · die vier Zustandstexte paarweise verschieden, in **beiden** Sprachen · kein Pfad trägt GUID oder Ablagenkennung |
| `tests/artefakt-ansicht.test.tsx` | gerenderter Baum, 9 Fälle | **der Textknoten** · die **vier Zustände**, je einer · der Ausschnitt-Vermerk in beide Richtungen · der Download-Knopf · die Beschriftung ohne Nachladen |
| `tests/zeitleiste-ziele.test.tsx` | gerenderter Baum, 8 Fälle | die **drei Lagen je Schritt** (beide Arten, nur eine, keine) · der **Eingang** über der Leiste, mit Familie statt Nummer und ohne dass die Leiste eine vierte Zeile bekäme · die **Belastungsprobe aus M55**: fünfzehn Artefakte, fünfzehn eigene Ziele, ohne doppelten React-Schlüssel · das **Anspringen** der Eigenschaftengruppe · die Gegenprobe: ohne Eigenschaften kein Schalter am Schrittnamen |

> **`tests/dateien-block.test.tsx` ist entfernt worden, nicht auskommentiert.** Der Block, den sie
> prüfte, existiert nicht mehr; ihre beiden Fälle sind in `tests/zeitleiste-ziele.test.tsx`
> aufgegangen, wo sie an der neuen Stelle dasselbe belegen.

**Siebzehn gerenderte Fälle sind siebzehn begründete Ausnahmen.** Die Bedingung ist unverändert die
aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §9: nicht „ein Baum wäre bequemer", sondern
*„es gibt keinen anderen Ort, an dem der Satz belegbar wäre"*. Hier sind es drei Klassen:

- **Aussagen über Abwesenheit** — kein Element aus dem Inhalt, kein Inhaltsfeld in den vier
  Zuständen, kein Download-Knopf, kein Schalter am Schrittnamen ohne Eigenschaften, keine vierte
  Zeile in der Leiste für Schritt `0`.
- **Regeln, die selbst Markup sind** — ob aus `<b>fett</b>` ein Element wird oder Text, entscheidet
  React beim Rendern und keine Funktion; **welche Zeile welches Ziel bekommt**, ebenso.
- **Zustände des Dokuments** — der Sprung endet auf `document.activeElement`, und den kennt keine
  reine Funktion.

Die Gesamtzahl wird an **einer** Stelle geführt: im Kopf von `frontend/vitest.config.mts`
(**34 in sieben Dateien**). Diese Datei nennt sie nicht noch einmal, sie nennt nur die eigenen drei.

### Kein Test und kein Testdatensatz enthält echten Dateiinhalt

Jeder Inhalt in den Tests ist **frei erfunden** — kein echter Partner, kein echter Knoten, kein
echter Pfad, keine echte Kennung, keine echte Belegnummer. Das gilt auch für den Binärfall: Dort
steht gar kein Inhalt, weil das Backend in diesem Zustand keinen liefert.

Der Inhalt, der nachweislich als Text und nicht als Markup landet, ist ebenfalls erfunden und
enthält absichtlich `<script>`, `<img onerror=…>`, `<svg>` und ein EDIFACT-`UNB`-Segment mit
Platzhalter-Kennungen.

---

## 10. Abweichungen von der Vorgabe

Vier, alle vorsätzlich und alle hier statt in einer Fußnote.

1. **Es gibt keine Zahl über den Artefakten.** Die Nachbarblöcke tragen eine („Belegdaten (9)",
   „Technische Eigenschaften (36)"), weil der Detail-Endpunkt `bamAnzahl` und
   `eigenschaftenAnzahl` im Kopf liefert. Für die Artefakte gibt es kein solches Feld.

   **Seit der Nachbesserung braucht es auch keins.** Die Zahl existierte bei den Nachbarn, um einen
   eingeklappten Block zu beschriften, ohne ihn zu laden — die Artefakte haben keinen eingeklappten
   Block mehr. Der Vorschlag `artefakteAnzahl` im Kopf des Detail-Endpunkts ist damit **hinfällig**
   und wird nicht weiter vorgetragen.

2. **Der Verweis auf die Ansicht trägt keine Abfragezeichenkette**, anders als die beiden
   Umschaltziele des Nachrichtendetails. Begründung in §8.

3. **Die Ansicht hat keine Maximalbreite**, anders als `/nachrichten/<id>`. Begründung in §7. Das
   widerspricht [`visuelles-konzept.md`](visuelles-konzept.md) §5 **nicht** — dort ist die
   Maximalbreite ausdrücklich für Ansichten *mit Fließtext* vorgesehen —, weicht aber von der
   Gewohnheit der Nachbarroute ab und gehört deshalb benannt.

4. **`docs/README.md` und [`frontend-grundlagen.md`](frontend-grundlagen.md) §9 sind ergänzt
   worden**, dazu am 18.08.2026 der Eintrag für [`rohdaten.md`](rohdaten.md) selbst — der offene
   Punkt 10 der ersten Fassung, den der Auftrag zur Nachbesserung ausdrücklich freigibt. Die Tabelle
   in [`frontend-grundlagen.md`](frontend-grundlagen.md) §9 führt die rendernden Testdateien und
   verlangt nach ihrer eigenen Regel einen Eintrag je neuer Datei; die **Zahl** steht weiterhin
   ausschließlich in `vitest.config.mts`.

### Was seit der Nachbesserung zusätzlich benannt gehört

**Die Zeitleiste ist angefasst worden — so wenig wie möglich, aber nicht gar nicht.** Sie ist
Schritt 5 und war fertig, geprüft und in Gebrauch. Geändert ist an ihr **zweierlei**, und beides
hängt *an* der Zeile statt in der Rechnung:

| Geändert | Unberührt |
|---|---|
| je Zeile die Ziele ihres Schritts | `zeitleiste(detail)` in `detail.ts` — keine andere Sortierung, keine zweite Datenquelle |
| der Name wird zur Schaltfläche, wo es Eigenschaften gibt | die Balkenrechnung, die Wartezeile, die erwartete Zeile, die drei Texte der leeren Leiste |
| | `schritte[]` bleibt die einzige Quelle der Zeilen — **Schritt `0` steht auch jetzt in keiner** |

**Die Zeitleiste zeigt Schritt `0` also nicht**, und damit ist keine Abweichung zu Schritt 5
entstanden. Seine Artefakte stehen in einer eigenen Zeile **über** ihr (§3).

**Die sichtbare Ausschnitt-Marke ist entfallen**, die Ankündigung nicht. Begründung in §3.

**Was ausdrücklich keine Abweichung ist:** Dass an einem Ziel kein sichtbarer Text steht, ist die
Umsetzung von [`rohdaten.md`](rohdaten.md) §5 in seiner Fassung vom 18.08.2026 und nicht ihre
Einschränkung — §5 legt die *Beschriftung* fest, und die steht im zugänglichen Namen. Begründung in
§3.

---

## 11. Offene Punkte

| # | Punkt |
|---|---|
| 1 | **Die Sichtprüfung im Browser steht aus** — an beiden Einhängepunkten und für die Ansicht. Sie braucht eine Anmeldung und einen Mandantenzugang; dazu eine Nachricht aus dem Fenster **2025-07-24 bis 2025-12-30**, dem einzigen, in dem Datenbankkopie und Filestore-Kopie sich decken ([`START-LOKAL.md`](START-LOKAL.md) §1, [`rohdaten.md`](rohdaten.md) §13 Punkt 5). Zu sehen wären: die Zeilen der Zeitleiste mit ihren Zielen bei fünfzehn Artefakten — **ob Name, Dauer und Balken im Panel von 26 rem daneben noch tragen, ist gerechnet und nicht gesehen** —, die Ansicht mit einer großen Datei, und der Umbruch am **schmalen Fenster**; die Browsersteuerung kann das Fenster nicht verkleinern, also ist es von Hand zu prüfen |
| 2 | **Keine Zeilennummern.** Sie erzwingen ein Element je Zeile und damit bei 610 KB Zehntausende Knoten (M60). Wer sie will, braucht vorher eine Bauform, die ohne die Zerlegung auskommt — etwa ein gezeichneter Rand statt echter Elemente |
| 3 | **Kein Umschalter auf UTF-8.** [`rohdaten.md`](rohdaten.md) §7 lässt ihn zu, verlangt ihn nicht; das Backend hat ihn nicht gebaut ([`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 7). Ohne ein Feld in der Antwort gibt es hier nichts umzuschalten |
| 4 | **Ein fehlgeschlagener Download zeigt den RFC-9457-Rumpf des Backends statt einer übersetzten Meldung.** Der Download ist eine Navigation auf den Endpunkt — anders geht es nicht, ohne eine Blob-URL zu bauen, und die ist ausgeschlossen. Der Fall setzt voraus, dass sich der Zustand **zwischen** Anzeige und Klick ändert (etwa die Ablage fällt aus); die Oberfläche bietet den Knopf sonst gar nicht erst an. **Zu entscheiden, wenn es jemanden trifft** |
| 5 | **Kein Sheet über der Detailansicht** — ausdrücklich eine spätere Zugabe (Entscheidung 7) und nicht Teil dieses Baus |
| 6 | ~~**Der Block ordnet nach `MessageActionID`, die Zeitleiste nach `MessageActionStart`.**~~ **Erledigt am 18.08.2026.** Der Block ist entfallen; Ziele und Eigenschaftengruppen folgen beide der Zeitleiste. Es gibt nur noch **eine** Ordnung, und damit nichts mehr, was auseinanderfallen könnte |
| 7 | **`Escape` schließt die Dateiansicht nicht.** Im Nachrichtendetail tut es das, weil der Schließen-Knopf dort hinter bis zu fünfzig Tabellenzeilen steht; hier ist der Rückweg der erste Tabstopp der Seite. Ob die Taste trotzdem einheitlich gelten soll, ist eine Frage an die Abnahme |
| 8 | **`app_user.download_allowed` wird auch hier nicht geprüft** — dieselbe Lage wie im Backend ([`rohdaten-backend.md`](rohdaten-backend.md) §11, Punkt 8). Gebaut ist nach [`rohdaten.md`](rohdaten.md) §3, Entscheidung 2: keine zweite Berechtigungsstufe. **Zu entscheiden: fällt Entscheidung 2, oder fällt das Flag?** |
| 9 | **Das Verhalten bei 610 KB im `<pre>` ist ungemessen.** Ein Textknoten dieser Größe mit `pre-wrap` ist theoretisch unproblematisch und praktisch ungeprüft — im lokalen Bestand ist keine so große Datei abrufbar. Gehört zur Sichtprüfung aus Punkt 1 |
| 10 | ~~**[`rohdaten.md`](rohdaten.md) selbst fehlt im Verzeichnis von `docs/README.md`.**~~ **Erledigt am 18.08.2026**, weil der Auftrag zur Nachbesserung den Eintrag ausdrücklich freigibt |
| 11 | **Der Sprung in die Eigenschaften findet die Gruppe über `document.getElementById`.** Das ist der kürzeste Weg zwischen zwei Bausteinen, die sonst nichts voneinander wissen, und er ist geprüft — aber er greift am Baum vorbei. Ein `ref` durch beide Komponenten wäre React-reiner und hätte hier vier Ebenen zu durchqueren. **Zu entscheiden, wenn ein dritter Aufrufer dazukommt** |
| 12 | **Ob der Schrittname als Schaltfläche gelesen wird, ist ungeprüft.** Er sieht aus wie zuvor und fühlt sich beim Überfahren an wie ein Bedienelement; ob ein Nutzer ihn *sucht*, sagt kein Test. Gehört zur Sichtprüfung aus Punkt 1 |
| 13 | **Ab etwa sieben Zielen an einer Zeile wird es eng.** Die Ziele geben keine Breite ab (`shrink-0`), der Schrittname schrumpft zuerst — im Panel von 26 rem ist bei rund sieben Zeichen für Name, Balken und Dauer Schluss. Gemessen liegen je Schritt in aller Regel **zwei** Artefakte, und die Zeitleiste bricht deshalb heute nicht; eine Zusage des Schemas ist das nicht. **Zu entscheiden, wenn es auftritt:** Umbruch der Zeile oder eine Zeile je Schritt mehr — abgeschnitten wird nichts |
| 14 | **Der Schrittname trägt die Antwort auf „wo steht der Beleg" und ist im Panel jetzt schmaler.** Zwei Ziele und die Schaltflächenpolsterung kosten rund 70 px von vorher rund 270. Gekürzt wird nach der Regel aus [`nachrichtenliste.md`](nachrichtenliste.md) §8.1, der Vollwert steht im `title`. **Ob das reicht, sagt erst die Sichtprüfung** (Punkt 1) |
| 15 | **`components/ui/separator.tsx` wird seit dem Wegfall des Dateienblocks nirgends mehr verwendet.** Es liegt im Generatorbereich von shadcn/ui und war auch vor Schritt 8 ungenutzt; entfernt wird es deshalb **nicht** in dieser Runde. Wer im Generatorbereich aufräumt, tut es für alle Bausteine auf einmal |
| 16 | **[`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) beschreibt Schritt 8 weiterhin als reinen Download.** Dass die **Anzeige der Regelfall** ist, steht seit dem 14.08.2026 in [`rohdaten.md`](rohdaten.md) §1 — der Plan ist nie nachgezogen worden. Nach der Lesereihenfolge aus `CLAUDE.md` ist er verbindlich, und wer ihn liest, hält die gebaute Anzeige für einen Regelverstoß. **Nicht hier nachgeholt:** Der Auftrag zu dieser Nachbesserung gibt drei Dokumentationsdateien frei, und der Plan gehört nicht dazu. **Zu entscheiden vom Auftraggeber** |

---

## 12. Die Dateien

| Datei | Zweck |
|---|---|
| `features/nachrichten/rohdaten.ts` | **Die Entscheidungen als reine Funktionen** — Beschriftung, Ziele je Schritt, Gleichlauf, Vermerke, Pfade |
| `features/nachrichten/api.ts` | Typen und Aufrufe der drei Endpunkte, dazu die beiden Abfrageschlüssel |
| `features/nachrichten/hooks.ts` | `useArtefakte` (mit dem Detail) und `useArtefaktinhalt` (erst beim Öffnen) |
| `features/nachrichten/components/artefakt-ziele.tsx` | Die Ziele: ein Zeichen je Artefakt, dazu die gestrichelte Zeile für Eingang und Rest |
| `features/nachrichten/components/zeitleiste.tsx` | *(Schritt 5)* je Zeile die Ziele ihres Schritts, der Name als Weg zu den Eigenschaften |
| `features/nachrichten/components/eigenschaften-block.tsx` | *(Schritt 5)* das Sprungziel: `id` je Gruppe, Aufklappen und Fokus |
| `features/nachrichten/components/nachricht-detail.tsx` | hängt Eingangszeile, Leiste, Rest und Eigenschaften zusammen und hält das Sprungziel |
| `features/nachrichten/components/artefakt-ansicht.tsx` | Die Ansicht: Herkunftszeile, Download, Vermerke, Inhalt oder benannter Zustand |
| `app/(app)/nachrichten/[messageId]/dateien/[artefaktId]/page.tsx` | Die Route. Server-Komponente, prüft nichts |
| `lib/routen.ts` | `artefaktAnsicht` und `nachrichtAnsicht` — die beiden neuen Ziele |
| `i18n/de.ts`, `i18n/en.ts` | Der Abschnitt `nachrichten.detail.dateien`. **Keine Zeichenkette steht in einer Komponente** |

**Entfallen ist** `features/nachrichten/components/dateien-block.tsx` samt
`tests/dateien-block.test.tsx` — **entfernt, nicht auskommentiert.**

**Unberührt geblieben sind:** `globals.css`, `lib/http.ts`, `lib/query-client.ts`, `proxy.ts`,
`next.config.ts`, `detail.ts` (die Rechnung der Zeitleiste), der Ketten- und der BAM-Block, die
Nachrichtenliste und die Belegsuche.

**Fast unberührt:** `artefakt-ansicht.tsx`. Die Ansicht selbst ist **nicht** umgebaut worden — sie
war nicht Gegenstand der Nachbesserung. Getauscht ist dort eine einzige Zeichenkette: Der Rückfall
der Überschrift heißt jetzt `ansichtTitel` statt `titel`, weil der alte Wert die Überschrift des
entfallenen Blocks war („Dateien und Protokolle" über einer einzelnen Datei).

---

## 13. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID | Kein Aufruf dieser Oberfläche trägt eine — auch keine Rolle, keine GUID, keine Ablagenkennung. Ein Test prüft die Pfade |
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | Die Familie wird nicht übersetzt, der Rückfall erfindet keinen Namen, **nichts auf Schritt `0` heißt „Schritt 0"**, und kein Ziel wird ausgegraut, weil das Backend nichts dazu gesagt hat |
| **Q3** Die Problemkategorien bleiben getrennt | Keiner der vier Zustände bekommt Rot; Rot bleibt dem `MessageStatus` |
| Berechtigung nur im Backend | Der Download-Knopf ist Bequemlichkeit; das BFF entscheidet nichts ([`frontend-grundlagen.md`](frontend-grundlagen.md) §2) |
| Farben nur über CSS-Variablen | `tests/farbwerte.test.ts` deckt die beiden neuen Komponenten mit ab; `globals.css` ist unverändert |
| `console.error` ist ein Fehlschlag | `tests/setup/konsole.ts` gilt für die drei neuen Dateien wie für alle |
| Kein Artefakt wird unerreichbar | Schritt `0` bekommt eine eigene Zeile, alles Weitere ohne Zeile die zweite; beides geprüft (§3, §9) |
