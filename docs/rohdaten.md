# Rohdaten und Protokolle

Stand: 17.08.2026, **Entscheidung 6 korrigiert am 18.08.2026 und am 19.08.2026** (§3, §5) ·
Schritt 8 des MVP
Grundlage: `messungen-schritt8.md` (M52–M71 und **M73**, Abschnitt Q),
`messungen-schritt8-auftrag.md` Fassung 3

Ergänzt `PROJEKTBESCHREIBUNG.md` §7 „Rohdatenzugriff". Bei Widersprüchen gilt die
Projektbeschreibung — mit Ausnahme der in §3 datiert festgehaltenen Abweichungen.

---

## 1. Zweck

Ein Nutzer sieht zu einer Nachricht **alle zugehörigen Dateien** — die eingegangene Nutzdatei, die
umgewandelten Fassungen und die Protokolle der einzelnen Schritte —, kann sie **im Browser ansehen**
und herunterladen.

**Die Anzeige ist der Regelfall, nicht der Download.** Das ist die tragende Änderung gegenüber dem
Implementierungsplan, der ausschließlich Download vorsah.

> **Abgrenzung zu §9 der Projektbeschreibung.** Dort ist die **aufbereitete** Anzeige ausgeschlossen
> — EDIFACT, VDA und IDOC in Segmente zerlegt. Das bleibt ausgeschlossen. Gebaut wird die
> **Rohtextanzeige**: die Datei als Text, unverändert, in Festbreitenschrift.
>
> Bemerkenswert dazu: Das Altwerkzeug **hat** die aufbereitete Anzeige. Vier Formate stehen in der
> Auswahl — Edifact, VDA, XML, ANSI —, alle vier sind `disabled: true`, der Umformatierungscode
> existiert. Jemand hat sie gebaut und abgeschaltet. Warum, ist offen (§13).

---

## 2. Was gemessen ist

Alles in diesem Abschnitt ist belegt. Fundstellen in `messungen-schritt8.md`.

### 2.1 Die Artefakte

| | |
|---|---|
| Namensmuster | `<Dienst>.Payload.GUID` und `<Dienst>.Log.GUID` in `MessageProperty` (M54) |
| Wertform | `<Ablagenkennung>\|<UUID>`, durchgängig (M54) |
| Anzahl je Nachricht | **3 bis 15**, immer mindestens ein Protokoll, bei jedem Mandanten (M55) |
| Ort | jedes Artefakt hängt über `MessageActionID` an seinem Schritt; auf Schritt `0` liegt das Paar des Lesedienstes (M57, M17 (3)) |
| Lesbarer Schrittname | **nur für 44,02 %** — 55,98 % der Artefakte lösen zu keinem `SOSActionName` auf, bei `ohne_schrittzeile = 0` (M57) |
| **Kein Artefakt** | `Message.Payload.GUID` — er trägt in 6.249 von 6.249 und 214.330 von 214.330 Nachrichten den Verweis der Nutzdatenzeile mit dem **höchsten `MessageActionID`** derselben Nachricht (**M73**) und ist seit dem 19.08.2026 aus der Liste |

> **Korrigiert 19.08.2026 in der Zeile „Ort".** Sie lautete bis heute: „jedes Artefakt hängt über
> `MessageActionID` an seinem Schritt; **`Message.Payload.GUID` steht auf Schritt `0`** (M57,
> M17 (3))". Der zweite Halbsatz ist als Zeilenlage weiterhin richtig und als Aussage über ein
> *Artefakt* nicht mehr: Die Zeile steht in `MessageProperty`, die Liste führt sie nicht.
>
> **Die drei Zahlen darüber und daneben sind unberührt** — sie stammen aus M54, M55 und M57 und
> sind für ihren Erhebungszeitpunkt richtig. Dass **3 bis 15** und **55,98 %** über eine Menge
> rechnen, die die entfallene Zeile einschließt, ist als offener Punkt vermerkt
> ([`rohdaten-frontend.md`](rohdaten-frontend.md) §11, Punkt 18). **Nachgerechnet wird in dieser
> Runde nichts.**

### 2.2 Die Ablagen

| | |
|---|---|
| Auflösung | die Kennung vor der Pipe **ist** eine `Service.ServiceID`; lesbare Codes, Primärschlüsselzugriff (M52) |
| Verteilung | die Ablage hängt am **Zeitraum**, nicht am Mandanten. Rotationsgrenze taggenau **2025-07-23** (M53, Teil B) |
| Spiegelung | **keine.** 20 von 20 Kreuzabrufen scheitern, dieselben Verweise gelingen am eigenen Knoten (M68) |

> **Daraus folgt für den Bau:** Die Auflösung über `Service` ist tragend und nicht wegzukürzen. Ein
> Rückfall auf eine andere Ablage ist falsch, auch wenn er gelegentlich funktionieren würde.

### 2.3 Der Abrufweg

| | |
|---|---|
| Protokoll | **SOAP**, ausschließlich. Kein WSDL, keine Authentifizierung |
| Endpunkt | der `ServiceConnectString` der Ablage, **unverändert**, ohne Anhängsel (Q1) |
| Operation | `RETRIEVE` (Q1) |
| Kennung | die **nackte UUID hinter der Pipe** (Q1, `JsonServlet.java:784–:786`) |
| Antwort | Rumpf mit `Response`, die Datei als **SOAP-Anhang** (Q1, M71) |
| Anhangsform | **ZIP.** In 693 geholten Dateien **kein einziges** mit mehr als einem Eintrag (Teil B) |
| Dauer | 38 bis 244 ms je Abruf, je nach Größe (M66, M60) |

### 2.4 Die Dateien

| | |
|---|---|
| Größe | größtes Artefakt **609.995 Byte**, kleinstes 2 Byte, **keines über 1 MiB** (M60) |
| Vorabgröße | `FileReader.FileProperty.Size` zählt **Bytes** — belegt durch Abgleich mit `Content-Length`, exakt in 7 von 7 (M60). Deckt aber nur FileReader-Schritte, rund 69,6 % (M17) |
| Kodierung | **8 von 8 Protokollen und 9 von 16 Nutzdateien sind kein gültiges UTF-8** (M61). Das Altsystem dekodiert hart mit `ISO-8859-1` (Q4) |
| Binäranteil | **18,2 % der Nutzdateien**, 0 % der Protokolle (M61) |

### 2.5 Die Marken

| | |
|---|---|
| Paarung | wo Marken stehen, sind sie **ausnahmslos vollständig gepaart** — 395 von 395, kein Echo-Fall (M63) |
| Fehlernachrichten | **33 von 33** vollständig gepaart. Der befürchtete Fall „Start ohne Ende" tritt nicht ein (M64) |
| Fehlende Marken | `HTTPSender` **30 von 30 ohne**, `FTPSender` **28 von 30 ohne** (M63) |
| Innenbereich | **90,4 % der Bytes**, je Familie zwischen 9,3 % und 99,4 % (M67) |
| Inhalt | Pfade und Dienstkennungen stehen **überwiegend innerhalb** — 808 gegen 252 Zeilen, 331 gegen 93 (M65) |

> **Belegvermerk (L10).** *Gemessen war:* die Verteilung von Pfaden und Dienstkennungen über 395
> Protokolldateien. *Behauptet wird:* Der Beschnitt ist eine **Lesbarkeitsregel**, keine
> Vertraulichkeitsgrenze — er hält weniger zurück, als sein Name nahelegt, und er verkürzt kaum.
> Er wird trotzdem gebaut (§3, Entscheidung 5). Dieser Vermerk steht hier, damit später niemand eine
> Zusage darauf gründet.

---

## 3. Entscheidungen

| # | Entscheidung | Datum |
|---|---|---|
| 1 | **Anzeige ist Regelfall**, Rohtext, nicht aufbereitet | 14.08.2026 |
| 2 | Alle Rollen sehen **alle Dateien** der Nachrichten, die sie ohnehin erreichen. Keine zweite Berechtigungsstufe. **Bestätigt 20.08.2026** — Kasten unter dieser Tabelle | 14.08.2026, bestätigt 20.08.2026 |
| 3 | Bei Protokollen sieht `MANDANT` nur den Bereich zwischen den Marken. `ADMIN` sieht vollständig. **Über die Rolle, nicht über ein Flag** | 14.08.2026 |
| 4 | Kodierung **`ISO-8859-1`**, belegt durch M61 und deckungsgleich mit Q4 | 17.08.2026 |
| 5 | Markenregel wie in §6, einschließlich **keine Startmarke → nichts** | 17.08.2026 |
| 6 | **E1 = Artefakte an der Zeitleiste**, Schritt `0` einzeln darüber. **Korrigiert am 18.08.2026 und am 19.08.2026**, beide alten Fassungen in den Kästen unter dieser Tabelle | 17.08.2026, korrigiert 18.08. und 19.08.2026 |
| 7 | **E2 = eigene Route.** Ein Sheet über der Detailansicht ist eine spätere Zugabe, kein MVP-Bestandteil | 17.08.2026 |
| 8 | **E3 = Binärdateien werden erkannt und benannt**, nicht angezeigt | 17.08.2026 |
| 9 | **E4 = Download liefert, was die Anzeige liefert.** Für `MANDANT` bei Protokollen also die beschnittene Fassung | 17.08.2026 |
| 10 | „Keine Datei vorhanden" ist ein **Fehlerzustand**, kein Regelfall — produktiv decken sich Datenbank und Filestore (Auskunft 17.08.2026) | 17.08.2026 |

> **Entscheidung 2 ist am 20.08.2026 bestätigt, nicht korrigiert.** Am selben Tag war
> zwischenzeitlich entschieden, das Flag `app_user.download_allowed` in die Oberfläche zu holen und
> E2 zu korrigieren — **diese Entscheidung ist zurückgenommen.** Die Spalte fällt stattdessen per
> Migration in Schritt 9a (E20). **E2 gilt unverändert.** Der offene Punkt 8 in
> [`rohdaten-backend.md`](rohdaten-backend.md) §11 ist damit geschlossen — mit „Spalte entfernt",
> nicht mit „geprüft". Die drei Endpunkte aus Schritt 8 bleiben unangetastet.

### Entscheidung 6 ist am 18.08.2026 korrigiert worden

> **Die alte Fassung, wortgleich wie sie am 17.08.2026 hier stand:**
>
> > 6 · **E1 = Zweigeteilt**: Nutzdaten und Protokolle getrennt, beide nach Schritt geordnet,
> > Originaldatei im Kopf
>
> Dazu §5 in seiner alten Gestalt: *„Zweigeteilt, beide Teile nach Schritt geordnet — Kopf: die
> eingegangene Datei · Nutzdaten je Schritt · Protokolle je Schritt, darunter, sichtbar abgesetzt."*

**Der Grund.** Die Entscheidung entstand **vor** M57. M57 hat danach gezeigt, dass jedes Artefakt
über `MessageActionID` an **seinem Schritt** hängt — und die Entscheidung ist nicht nachgezogen
worden. Sichtbar wurde das erst im gebauten Zustand: Das Nachrichtendetail trug drei Blöcke auf
derselben Achse — Zeitleiste, Dateien, technische Eigenschaften —, alle drei nach Schritt sortiert,
alle drei untereinander. Im Dateienblock standen **neun Zeilen, davon acht mit vier sich
wiederholenden Schrittnamen**: denselben, die drei Zeilen darüber schon in der Zeitleiste standen,
dort mit Dauer und Balken.

**Der Fehler lag in der Entscheidung, nicht in der Umsetzung.** Ein Nutzer will nicht „alle
Nutzdaten", er will **einen Schritt aufmachen** und sehen, was dort liegt.

**Die neue Fassung:**

| | |
|---|---|
| **Zeitleiste** | je Schritt zusätzlich **die Artefakte, die auf ihm liegen** — in aller Regel zwei, Datei und Protokoll. Wo nichts liegt, kein Ziel; und es wird auch nichts abgeschnitten, wo mehr liegt |
| **Schritt `0`** | einzeln, in einer eigenen Zeile **über** der Zeitleiste, beschriftet mit *Eingang*. Er ist kein Ablaufschritt und kommt in `schritte[]` nicht vor |
| **Technische Eigenschaften** | bleiben ein eigener Block, nach Schritt gruppiert — **anspringbar aus der Zeitleiste** |
| **Block „Dateien und Protokolle"** | **entfällt vollständig** |

> **Korrigiert 19.08.2026 — die zweite Zeile hieß „Eingegangene Datei".** Sie lautete: „**|
> Eingegangene Datei | einzeln, in einer eigenen Zeile über der Zeitleiste. Sie hängt an keinem
> Ablaufschritt |**". Was dort einzeln steht, ist **Schritt `0`** und nicht eine bestimmte Datei;
> darin hängt seit dem 19.08.2026 das Paar des Lesedienstes, Datei und Protokoll.
>
> **Gemessen ist (M73):** `Message.Payload.GUID` — bis dahin geführt als *die eingegangene Datei* —
> trägt in **6.249 von 6.249** und **214.330 von 214.330** Nachrichten denselben Verweis wie die
> Nutzdatenzeile mit dem **höchsten `MessageActionID`** derselben Nachricht, ohne Gegenfall in
> beiden Fenstern. Er ist damit kein eigenes Artefakt und aus der Liste entfallen —
> [`messungen-schritt8.md`](messungen-schritt8.md) M73, Befunde 2 bis 5.
>
> **Ausdrücklich nicht behauptet:** dass „höchster `MessageActionID`" gleichbedeutend mit „zeitlich
> zuletzt" ist. Gemessen ist die Schrittnummer, nicht die Uhr.
>
> **Die Entscheidung selbst ist unberührt.** Artefakte hängen an der Zeitleiste, Schritt `0` steht
> darüber, der eigene Block bleibt entfallen. Was fällt, ist ein Ziel in der obersten Zeile — und
> mit ihm die Behauptung, es sei die eingegangene Datei.

**Warum die technischen Eigenschaften trotzdem ein eigener Block bleiben.** Die Trennlinie ist nicht
„gehört zum Schritt oder nicht" — sie hängen genauso am Schritt —, sondern **„ein Ziel oder ein
Textblock"**: Dateien sind null bis zwei Verweise je Schritt, Eigenschaften rund 23
Schlüssel-Wert-Paare je Nachricht (M44). Zwei Ziele passen in eine Schrittzeile, zehn Wertepaare
sprengen sie.

**Was die Korrektur nicht anfasst:** die Aufteilung der *Antwort*. Das Backend liefert weiterhin
`eingang`, `nutzdaten` und `protokolle` getrennt — sie folgt aus dem Datenmodell und nicht aus einer
Gestaltungsentscheidung, und die Oberfläche teilt daraus neu ein. **Kein Endpunkt ist geändert
worden.**

> **Korrigiert 19.08.2026 zum Absatz darüber.** Er gilt für den 18.08.2026 und ist seit dem
> 19.08.2026 an einer Stelle überholt: Das Backend liefert `eingang` **nicht** mehr. Das Feld ist
> aus `ArtefaktlisteResponse` entfallen, die Antwort ist zweigeteilt — `nutzdaten` und
> `protokolle`.
>
> **Warum das Feld mitfällt und nicht nur die Beschriftung.** Ein Feld namens `eingang`, das den
> Verweis der Nutzdatenzeile mit dem höchsten `MessageActionID` trägt, ist dieselbe Falschauskunft
> eine Schicht tiefer. An diesen Endpunkten hängt nicht nur die Oberfläche: Der Chatbot der
> Ausbaustufe 1 greift laut [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §10 auf **dieselben**
> zu, und dort fiele es niemandem auf.
>
> **Der Satz „Kein Endpunkt ist geändert worden" gilt weiterhin für den 18.08.2026** und nicht mehr
> für heute: Der Pfad ist derselbe, die Antwort trägt ein Feld weniger. Ein Umleitungspfad für die
> Kennung `0-Message.Payload.GUID` ist **nicht** gebaut worden — das Feature war einen Tag alt, und
> der Endpunkt antwortet auf sie mit `404` wie auf jede unbekannte Kennung. Das ist kein
> Sonderpfad, sondern das Ausbleiben eines Sonderpfads.

---

## 4. Der Abrufweg im Backend

**Der Proxy ist kein Bytestrom-Proxy.** Der Implementierungsplan beschreibt ihn so; der Filestore
spricht aber ausschließlich SOAP und liefert die Datei als ZIP-Anhang. Das Backend muss lesen,
entpacken und neu ausgeben — und für `MANDANT` bei Protokollen zusätzlich beschneiden.

Ablauf je Abruf:

1. **Mandantenprüfung im Statement**, nicht nachgelagert (Regel M1/M3). Wer eine fremde `MessageID`
   errät, bekommt null Zeilen.
2. Artefaktverweis aus `MessageProperty` über `MessageID` (Regel L4 — nie über den Wert).
3. Verweis an der Pipe zerlegen: Kennung und UUID.
4. Kennung über `Service` auflösen. **Keine Auflösung → benannter Fehlerzustand**, kein Rückfall.
5. SOAP-`RETRIEVE` gegen den `ServiceConnectString`, mit der nackten UUID.
6. Anhang lesen, ZIP entpacken, **ersten Eintrag** verwenden. Mehr als ein Eintrag ist bisher nie
   vorgekommen (0 von 693) — tritt er auf, wird das **protokolliert und angezeigt**, nicht
   stillschweigend verworfen wie im Altsystem.
7. Binärprüfung, dann Kodierung, dann gegebenenfalls Beschnitt.

**Grenzen:** harte Größenobergrenze deutlich über dem gemessenen Maximum von 610 KB, Zeitgrenze für
den SOAP-Aufruf, und die Größenprüfung greift **während** des Lesens — `FileReader.FileProperty.Size`
deckt nur 69,6 % und taugt nicht als Vorabprüfung (M60).

**Die SOAP-Abhängigkeit ist eine offene technische Entscheidung**, siehe §13.

---

## 5. Die Artefakte in der Oberfläche

*Fassung vom 19.08.2026. Die Fassung vom 17.08.2026 steht im Kasten unter §3, die vom 18.08.2026 in
den beiden Kästen unten.*

**An der Zeitleiste, nicht in einem eigenen Block.** Es gibt **eine** Schrittfolge, die führt, und
einen Block, der ihr folgt — statt dreier gleichrangiger Listen derselben Sache.

- **Je Schritt bis zu zwei Ziele** an seiner Zeile: die umgewandelte Fassung und das Protokoll. Wo
  nichts liegt, hängt nichts — kein Platzhalter, kein ausgegrautes Zeichen.
- **Schritt `0` steht darüber, in einer eigenen Zeile mit der Beschriftung *Eingang*.** Dort liegt
  das Paar des Lesedienstes, Datei und Protokoll (M57). Schritt `0` ist der Ort der Metadaten und
  kein Ablaufschritt; er kommt in `schritte[]` gar nicht vor und steht deshalb in keiner Zeile der
  Leiste. Ohne diese Zeile wären seine Artefakte unerreichbar. **Liegt dort nichts, gibt es die
  Zeile nicht** — seit dem 19.08.2026 kommt das vor, siehe Kasten unten.
- **Die technischen Eigenschaften bleiben ein eigener Block** — und sind aus jeder Zeile der
  Zeitleiste anspringbar.

> **Korrigiert 19.08.2026 — der Kernsatz dieses Abschnitts.** Der zweite Punkt lautete bis heute:
> „**Der Eingang steht darüber, in einer eigenen Zeile.** Auf Schritt `0` liegt die eingegangene
> Datei (`Message.Payload.GUID`) **und** das Paar des Lesedienstes (M57)."
>
> **Er ist die Stelle, an der eine Vermutung zur Tatsache geworden ist.** M57 hat gemessen, *welche
> Namen* auf Schritt `0` liegen — nicht, *worauf ihre Verweise zeigen*. Die Klammer
> „(`Message.Payload.GUID`)" hinter „die eingegangene Datei" ist am 18.08.2026 aus dem Namen
> abgeleitet und nie belegt worden.
>
> **Gemessen ist (M73, 19.08.2026):** `Message.Payload.GUID` trägt in **6.249 von 6.249** Nachrichten
> (Fenster A) und **214.330 von 214.330** (Fenster B) denselben Verweis wie die Nutzdatenzeile mit
> dem **höchsten `MessageActionID`** derselben Nachricht. Kein Gegenfall. Die Beschriftung
> *Eingegangene Datei* traf in **0,016 %** bzw. **0,015 %** der Nachrichten zu — Befunde 2 und 3.
>
> **Ausdrücklich nicht behauptet:** dass „höchster `MessageActionID`" gleichbedeutend mit „zeitlich
> zuletzt" ist. Gemessen ist die Schrittnummer, nicht die Uhr.
>
> **Der Satz „Ohne diese Zeile wären seine Artefakte unerreichbar" bleibt** — und M73 belegt ihn
> erstmals: In **1** Nachricht (Fenster A) und **33** (Fenster B) liegt die einzige gleichlautende
> Zeile ausschließlich auf Schritt `0` (Befund 5).
>
> **Nachgetragen 20.08.2026: In einem messbaren Teil des Bestands ist die Zeile jetzt leer.** Für
> `MessageActionID = 0` führt M57 (Fenster A) ausschließlich die neun Lesedienst-Paare und
> `Message.Payload.GUID`. Wo kein Lesedienst auf Schritt `0` liegt, lag dort also **nur** der
> entfallene Name, und die Zeile wird gar nicht mehr gezeichnet. **Gemessen (M73, Befund 6):**
> **950 von 6.249** Nachrichten in Fenster A und **28.616 von 214.330** in Fenster B — genau die
> Nachrichten mit `DataWarehouse.Payload.GUID`, vollständig komplementär zu den **5.299** bzw.
> **185.714** mit Lesedienst, ohne eine einzige Ausnahme.
>
> **Unerreichbar wird dadurch nichts:** `DataWarehouse.Payload.GUID` liegt auf Schritt `1` (M57) und
> hängt an einer Zeile der Leiste. Was verschwindet, ist die Zeile — nicht ein Artefakt.

**Beschriftung.** Wo ein `SOSActionName` auflöst, wird er verwendet. Für die **55,98 % ohne** (M57)
gilt Schrittnummer plus technische Familie — `Schritt 2 · Converter`. **Auf Schritt `0` steht die
Familie allein** (`SAPReader`): Eine Nummer, die in der Zeitleiste nirgends auftaucht, ist keine
Auskunft. **Nichts wird geraten** (Regel Q4), und die Dienst*namen* aus `Service.ServiceName`
bleiben unsichtbar (M15 (3)).

> **Korrigiert 19.08.2026 zum Absatz darüber.** Der Schluss lautete bis heute: „… ist keine Auskunft
> — **dieselbe Begründung, aus der der Eingang nicht „Schritt 0 · Message" heißt.**" Der Vergleich
> zeigt auf ein Artefakt, das die Liste nicht mehr führt; die Familie `Message` kommt in ihr nicht
> mehr vor. Die Begründung selbst — eine Nummer, die der Nutzer nirgends wiederfindet, sagt nichts —
> trägt unverändert.
>
> **Der Anteil 55,98 % bleibt stehen und ist zu hoch.** Er stammt aus M57 und rechnet über eine
> Menge, die die 6.249 `Message.Payload.GUID`-Zeilen einschließt. **Nachgerechnet ist er nicht** —
> das wäre eine Messung, und diese Runde erhebt keine Zahl. Als offener Punkt vermerkt in
> [`rohdaten-frontend.md`](rohdaten-frontend.md) §11, Punkt 18.

---

## 6. Der Beschnitt bei Protokollen

Gilt **nur** für Artefakte mit `Log.GUID` im Namen und **nur** für `MANDANT`.

| Fall | `MANDANT` sieht | Verhältnis zum Altsystem |
|---|---|---|
| Vollständiges Paar | Innenbereich, **ohne** die Markenzeilen | identisch (Q2) |
| Mehrere Paare | **erste Start- bis nächste Endmarke**, nichts sonst | **strenger** — das Altsystem hängt alle Blöcke aneinander |
| Startmarke ohne Endmarke | **nichts**, mit Hinweis | **strenger** — das Altsystem zeigt alles ab der Startmarke |
| Keine Startmarke | **nichts**, mit Hinweis | identisch (Q2) |
| Alle Fälle | Pfadmaskierung im Innenbereich (Q3) | identisch |

`ADMIN` sieht in allen Fällen die vollständige, **unmaskierte** Datei — auch das identisch zum
Altsystem (Q3).

> **Warum zwei Fälle strenger sind.** Das Protokoll gibt Werte aus der Nachricht aus —
> `Message.DestinationFilename`, `Message.ReceivingPartner`, `Message.SourceMessageID`. Diese Werte
> kommen aus der EDI-Datei und damit **vom Partner**. Ein Dateiname, der `***EndOfLog***` enthält,
> landet als echote Zeile im Protokoll. Die Marken sind also von außen beeinflussbar; die beiden
> großzügigeren Regeln ließen sich durch eine eingeschleuste Marke dazu bringen, Bereiche
> freizugeben, die außen liegen. Gemessen ist der Fall bisher nicht (M63: kein Echo-Fall in 395
> Dateien) — die Regel kostet nichts und schließt ihn aus.

**Bekannte Folge, ausdrücklich hingenommen:** Bei `HTTPSender` und `FTPSender` sieht `MANDANT` damit
**nie** ein Protokoll (M63). `FTPSender` hängt an rund 69 % der Nachrichten. Die Oberfläche zeigt
dort keinen leeren Kasten, sondern einen benannten Hinweis (§8).

---

## 7. Anzeige

**Eigene Route** `/nachrichten/{messageId}/dateien/{artefakt}`, verlinkbar, mit eigenem Bildlauf.

- Festbreitenschrift, keine Umformatierung, keine Syntaxhervorhebung.
- Der Text wird als **Textknoten** gerendert, niemals als HTML. Der Anzeigepfad liefert JSON oder
  `text/plain` mit `nosniff` — **nie** einen Bytestrom mit ratbarem Typ.
- Kodierung `ISO-8859-1`. Ein Umschalter auf UTF-8 ist zulässig, aber die Voreinstellung ist gemessen
  und wird nicht zur Laufzeit erraten.
- Kappung mit sichtbarem Hinweis, wenn sie greift. Bei einem Maximum von 610 KB ist sie eine
  Schutzmaßnahme, kein Regelfall.

---

## 8. Die vier Zustände

Jeder bekommt einen eigenen, benannten Text. **Keiner davon ist ein leeres Feld.**

| Zustand | Auslöser | Warum eigen |
|---|---|---|
| **Binärdatei** | Nullbytes bzw. Anteil druckbarer Zeichen unter Schwelle (M61: 18,2 % der Nutzdateien) | Das Altsystem zeigt hier Zeichenmüll, der wie ein Fehler aussieht |
| **Kein anzeigbarer Protokollteil** | Beschnitt greift, aber kein vollständiges Markenpaar (M63: zwei Familien durchgängig) | Betrifft die häufigste Familie; darf nicht wie ein Ausfall wirken |
| **Datei nicht vorhanden** | Abruf liefert nichts, Ablage antwortet aber | Produktiv ein Fehlerzustand; **in der Entwicklung der Normalfall** (§12) |
| **Ablage nicht erreichbar** | Kennung löst nicht auf oder Knoten antwortet nicht | Etwas anderes als „Datei weg" — und für den Betrieb die wichtigere Unterscheidung |

---

## 9. Download

- **Immer über das Backend**, niemals als durchgereichter Link. Der Filestore kennt unsere Nutzer
  nicht.
- `Content-Disposition: attachment`, **niemals inline.** `Content-Type: application/octet-stream`,
  kein Erraten.
- **Gleichlauf mit der Anzeige** (Entscheidung 9): Was `MANDANT` sieht, bekommt er auch als Datei.
  Bei Protokollen also die beschnittene und maskierte Fassung. `ADMIN` bekommt vollständig.
- **Dateiname** aus `FileReader.FileProperty.OriginalFilename`, wo vorhanden (69,6 %, M17), sonst
  konstruiert. Das Altsystem verwendet die `MessageID` (Q4) — das ist unnötig unhandlich.

---

## 10. Protokollierung

Drei Ereignisarten im `audit_log`, nicht eine:

| Ereignis | Zusätzlich festgehalten |
|---|---|
| Artefakt angesehen | Artefaktname, Fassung: **beschnitten oder vollständig** |
| Artefakt heruntergeladen | dasselbe |
| Abruf fehlgeschlagen | welcher der vier Zustände aus §8 |

**Die Fassung ist der Punkt.** Ein Eintrag, der beschnitten und vollständig nicht unterscheidet, ist
bei einer Rückfrage wertlos — und die Rückfrage ist genau der Grund, warum es das Protokoll gibt.

---

## 11. Was aus dem Altsystem **nicht** übernommen wird

Q5 hat drei Dinge gezeigt, die hier nichts nachbauen, sondern ersetzen:

1. Die Rolle kommt dort aus einem **Anfrageparameter** (`JsonServlet.java:789–:790`) — bei uns
   ausschließlich aus der Session.
2. Der Dateipfad läuft dort **an der Sitzungsprüfung vorbei** (`:165–:171` gegen `:94–:100`) — bei
   uns gilt sie ausnahmslos.
3. `mandant` wird dort in `getData` **nie verwendet** — bei uns ist der Filter Bestandteil jedes
   Statements.

Die Behandlung dieser Befunde im laufenden Altsystem liegt beim Auftraggeber und ist nicht
Gegenstand dieser Datei.

---

## 12. Vor dem Bau zu erledigen

| # | Was | Warum |
|---|---|---|
| 1 | **Dev-Zeitanker ins abrufbare Fenster legen** | Er zeigt auf den jüngsten Bestand, also `2026-07-08` — dort liefert der Filestore **nichts** (M66 (2)). Der Rohdatenzugriff wäre lokal bei jedem Start scheinbar kaputt. Abrufbar ist **2025-07-24 bis 2025-12-30** |
| 2 | **`PROJEKTBESCHREIBUNG.md` §8 nachziehen** | „Die Aufbewahrung beträgt 22 Monate" beschreibt die **Testkopie**. Produktiv sind es rund 18, danach Archivsystem. Schritt 10 rechnet sonst mit der falschen Zahl |
| 3 | **Archivsystem als Annahme aufnehmen** | Steht in keinem Projektdokument. Für den MVP außen vor — aber der Unterschied zwischen „weg" und „archiviert" ist genau das, was ein Nutzer wissen will |
| 4 | **`START-LOKAL.md` ergänzen** | Von der Entwicklungsmaschine sind 63,2 % des Bestands nicht abrufbar, weil zwei Ablagen aus sind. Produktiv gibt es das nicht. Ohne Vermerk jagt jemand einen Fehler, den es nicht gibt |

---

## 13. Offene Punkte

| # | Punkt |
|---|---|
| 1 | **Die SOAP-Abhängigkeit.** M71 hat mit `javax.xml.soap-api:1.4.0` und `saaj-impl:1.5.3` funktioniert — einem Zweig, der in einem Spring-Boot-4-Projekt (Jakarta EE 11) ein Fremdkörper ist. Die `jakarta`-Variante wäre stimmiger, aber ihr Verhalten auf der Leitung ist ungeprüft. **Auflösbar durch eine Messung:** Lauf gegen den lokalen Lauscher aus M70, Vergleich der Bytes |
| 2 | **Ein handgebauter Envelope ist unbelegt.** M59 (2) scheiterte; welcher der zwölf Unterschiede aus M70 den Ausschlag gab, ist nicht gemessen. Der BOM ist der plausible Kandidat, mehr nicht |
| 3 | **Warum sind die vier Formate im Altsystem abgeschaltet?** Der Umformatierungscode existiert. Die Antwort betrifft §9 der Projektbeschreibung |
| 4 | **V4 bleibt unbeantwortet.** Die Aufbewahrungsfrist im Filestore ist nicht gemessen — die zuständigen Ablagen sind aus. Die 18 Monate sind Auskunft, keine Messung |
| 5 | **Die Abnahme braucht eine Nachricht aus 2025-07-24 bis 2025-12-30.** Das ist das einzige Fenster, in dem Datenbankkopie und Filestore-Kopie sich decken |
| 6 | **Mehr als ein ZIP-Eintrag** ist nie vorgekommen (0 von 693). Der Fall wird trotzdem behandelt — falls er auftritt, ist zu entscheiden, ob alle Einträge angeboten werden |
| 7 | **Dass das Paar des Lesedienstes den *Eingang* der Nachricht bezeichnet, ist eine Sichtprüfung** *(neu am 19.08.2026)*. Gemessen ist, **welche** Namen auf `MessageActionID = 0` liegen (M57) — nicht, was die Dateien dahinter sind. Der Auftraggeber hat die beiden Artefakte am 19.08.2026 an **einer** Nachricht geöffnet und die Zuordnung bestätigt; darauf beruht die Beschriftung *Eingang* (§5). **Belegvermerk nach Regel L10**, ausführlich in [`rohdaten-frontend.md`](rohdaten-frontend.md) §3. **Zu entscheiden: messen oder als Sichtbefund führen** |
| 8 | **M73 ist in zwei Fenstern gemessen, nicht im Bestand** *(neu am 19.08.2026, = offener Punkt 32 in [`messungen-schritt8.md`](messungen-schritt8.md))*. 220.579 Nachrichten sind rund **6,6 %** der 3.341.519 des Gesamtbestands; Fenster C (`2024-10-01`) ist nicht gefahren. Ob `Message.Payload.GUID` am alten Ende ebenso zeigt, kostet eine einzige Sitzung |
| 9 | **Der Downloaddateiname hängt an derselben Annahme** *(neu am 19.08.2026, = offener Punkt 31 in [`messungen-schritt8.md`](messungen-schritt8.md))*. Der Anlassfall der Regel „Muster statt Familie" ist mit dem Artefakt entfallen; die Regel bleibt und ist **nicht** umgebaut worden. Ob ein anderer Fall bleibt, in dem Muster- und Familiensuche auseinandergehen, ist **benannt und nicht gemessen** — [`rohdaten-backend.md`](rohdaten-backend.md) §7 |
