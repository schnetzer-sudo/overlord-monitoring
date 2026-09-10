# Rohdaten und Protokolle — das Backend

Stand: 18.08.2026, **korrigiert am 19.08.2026 nach M73** (§1, §2, §7, §11) · Schritt 8 des MVP,
Teil Backend
Vorgabe: [`rohdaten.md`](rohdaten.md). Bei Widersprüchen gilt jene Datei; alle Abweichungen sind
hier unter §10 benannt und begründet.
Messungen: [`messungen-schritt8.md`](messungen-schritt8.md) M52–M72 und **M73**.

**Kein Frontend.** Route, Ansicht und Beschriftungen sind ein eigener Schritt.

---

## 1. Die drei Endpunkte

| Methode und Pfad | Antwort |
|---|---|
| `GET /api/nachrichten/{messageId}/dateien` | `ArtefaktlisteResponse` — zweigeteilt |
| `GET /api/nachrichten/{messageId}/dateien/{artefaktId}/inhalt` | `AnzeigeResponse` — JSON mit Text und Metadaten |
| `GET /api/nachrichten/{messageId}/dateien/{artefaktId}/download` | die Datei als Anhang |

**Kein Endpunkt nimmt entgegen:** eine Mandanten-ID (Regel M1), eine Rolle, eine GUID, eine
Ablagenkennung oder einen Filestore-Verweis. Der Mandant kommt aus der Sitzung über
`MandantService.aktuellerKontext`, die Rolle aus dem `SecurityContext`, der Verweis wird
serverseitig hergeleitet.

**`messageId` unbekannt oder fremd → dieselbe Antwort:** `404`, mit demselben Rumpf. Der Unterschied
entsteht gar nicht erst, weil der Mandantenfilter im Statement steht und in beiden Fällen dieselbe
leere Menge zurückkommt.

> **Vermerk 19.08.2026 — „zweigeteilt" stimmt seit heute wörtlich.** Die Antwort trug bis dahin
> **drei** Felder: `eingang`, `nutzdaten` und `protokolle`. `eingang` führte
> `Message.Payload.GUID`; nach **M73** trägt dieser Name in 6.249 von 6.249 und 214.330 von 214.330
> Nachrichten den Verweis der Nutzdatenzeile mit dem **höchsten `MessageActionID`** derselben
> Nachricht und benennt kein eigenes Artefakt. Das Feld ist entfallen (§7 und
> [`rohdaten.md`](rohdaten.md) §3).
>
> **Die Zweiteilung ist gemessen und nicht angenommen:** Alle 30 `%.Log.GUID`-Kombinationen in
> Fenster A tragen `gleich = 0` — Nutzdaten und Protokolle teilen sich nie eine Datei (M73,
> Befund 8).
>
> **Kein Umleitungspfad für alte Kennungen.** `0-Message.Payload.GUID` findet in der Menge nichts
> mehr und ergibt `404` wie jede unbekannte Kennung. Das Feature war einen Tag alt; ein bereits
> geteilter Verweis auf diese Kennung ist praktisch ausgeschlossen.

---

## 2. Die `artefaktId` — die sicherheitskritische Zeile

`<MessageActionID>-<MessagePropertyName>`, also etwa `0-FileReader.Payload.GUID` oder
`2-FileReader.Log.GUID`. Alle vorkommenden Zeichen sind in einem URL-Pfad unreserviert.

> **Korrigiert 19.08.2026.** Das erste Beispiel lautete bis heute **„`0-Message.Payload.GUID`"**.
> Die Kennung zerfällt weiterhin sauber — die Form ist unverändert gültig —, trifft aber keine
> Zeile mehr: Die Artefaktliste führt `Message.Payload.GUID` seit M73 nicht mehr (§7). Als Beispiel
> für eine gültige Kennung taugt ausgerechnet die eine, die nie auflöst, nicht.

> **Sie enthält niemals die GUID und niemals die Ablagenkennung.**
>
> Nähme ein Endpunkt einen Verweis entgegen, wäre er ein offener Proxy vor einer Produktionsablage:
> Wer einen fremden Verweis kennt oder rät, holt sich eine fremde Datei — und die Mandantenprüfung
> liefe ins Leere, weil sie an der *Nachricht* hängt und nicht an der Datei. Genau das ist im
> Altsystem der Fall (`JsonServlet.java:165`–`:171`, Q5). Hier wird es ersetzt, nicht nachgebaut.

**Sie ist keine Berechtigung, sondern eine Auswahl.** Der Server liest zuerst die Artefakte *dieser*
Nachricht — mandantengefiltert im Statement — und sucht darin die passende Zeile. Eine erfundene
Kennung findet nichts; eine Kennung aus einer fremden Nachricht ebenso wenig, weil die Menge, in
der gesucht wird, fremde Zeilen gar nicht erst enthält.

**Streng geprüft, ohne Reparaturversuch.** Der linke Teil muss vollständig aus ASCII-Ziffern
bestehen und in ein `SMALLINT` passen, der rechte aus `A–Z`, `a–z`, `0–9`, Punkt, Bindestrich und
Unterstrich und höchstens 100 Zeichen lang sein (die Spaltenbreite). Was nicht passt, ergibt `404` —
dieselbe Antwort wie eine unbekannte Kennung.

> Arabisch-indische Ziffern werden ausdrücklich abgewiesen, obwohl `Short.parseShort` sie annähme.
> Sonst gäbe es zwei Schreibweisen derselben Kennung, und eine stabile Kennung ist genau das nicht.

---

## 3. Die Kette

1. **Mandantenprüfung im Statement**, nicht nachgelagert (Regel M3) — `ArtefaktRepository`.
2. **Artefakte lesen**, ausschließlich über die `MessageID` (Regel L4), gefiltert auf die beiden
   Namensmuster `%.Payload.GUID` und `%.Log.GUID` (M54).
3. **Verweis an der Pipe zerlegen**, Kennung über `Service` auflösen (Primärschlüssel, M52).
   **Keine Auflösung → *Ablage nicht erreichbar*. Kein Rückfall auf eine andere Ablage** — 20 von
   20 Kreuzabrufen scheitern, die Ablagen sind keine Spiegel (M68).
4. **SOAP-`RETRIEVE`** gegen den `ServiceConnectString`, **unverändert und ohne Anhängsel** (Q1),
   mit der **nackten UUID** hinter der Pipe. Zeitgrenze gesetzt.
5. **Anhang lesen, ZIP entpacken, ersten Eintrag verwenden.** Mehr als ein Eintrag wird **im
   Ergebnis vermerkt und protokolliert**, nicht stillschweigend verworfen wie im Altsystem
   (`:801`–`:802`).
6. **Binärprüfung, dann Kodierung `ISO-8859-1`, dann gegebenenfalls Beschnitt** — in dieser
   Reihenfolge. Umgekehrt liefe der Beschnitt auf einem Text aus Binärbytes und suchte Marken in
   Zeichenmüll.

### Regel L4 im Einzelnen

Der Einstieg läuft ausschließlich über die `MessageID`. Die zusätzliche Bedingung auf
`MessagePropertyName` ist **kein Verstoß**: Verboten ist das Filtern, Gruppieren und Sortieren über
den *Wert*, dessen Indizes Präfix-Indizes über 50 Zeichen sind (M14). Der *Name* ist die zweite
Spalte des Primärschlüssels.

**Der Wert wird nie zerlegt, gejoint oder gefiltert.** Die Auflösung der Ablage ist ein *zweites*
Statement mit dem Ergebnis des ersten als Parameter — kein Join über
`SUBSTRING_INDEX(MessagePropertyValue, '|', 1)`. Ein solcher Join wäre genau das Filtern über den
Wert, das L4 verbietet, und die Trennung ist ohnehin die gemessene Form (M58 (1) und (2)).

### Die Grenzen

| Grenze | Wert | Bemessungsgrundlage |
|---|---|---|
| Datei insgesamt | **8 MiB** | Größtes gemessenes Artefakt 609.995 Byte, keines über 1 MiB (M60). Dreizehnfach darüber |
| Anzeige | **1 MiB** | Über dem gemessenen Maximum; die Kappung greift im gemessenen Bestand nie |
| Verbindungsaufbau | **5 s** | — |
| Antwort | **15 s** | Gemessen 38 bis 244 ms je Abruf (M66, M60), langsamster Einzelabruf 497 ms (M71). Rund dreißigfach darüber |

**Die Größengrenze greift *während* des Lesens**, an zwei Stellen: beim Lesen des SOAP-Anhangs und
beim Entpacken des ZIP-Eintrags. Eine Vorabprüfung über `FileReader.FileProperty.Size` gäbe es nur
für rund 69,6 % der **Nachrichten** in Fenster A und 57,2 % in Fenster B (M56, Befund 1) — und eine
Grenze, die in vier von zehn Fällen nicht greift, ist keine. Beim Entpacken wird ebenfalls gezählt
statt geglaubt: `ZipEntry.getSize()` ist eine Angabe aus dem Archiv und damit eine Behauptung der
Gegenseite.

> **Berichtigt 20.08.2026.** Hier stand „für rund 69,6 % der **Artefakte** (M17, M60)". Gemessen ist
> die Abdeckung der **Nachrichten**, und nur in Fenster A: `FileReader.FileProperty.Size` deckt
> **4.351 von 6.249** Nachrichten (69,63 %) und **122.604 von 214.330** in Fenster B (57,20 %),
> M56 Befund 1. Auf Artefakte gerechnet läge der Anteil weit darunter. Die Schlussfolgerung — die
> Grenze muss während des Lesens greifen — wird davon nur stärker.

**Der `ServiceConnectString` enthält einen Hostnamen** (Regel G1). Er erscheint in keiner
Fehlerantwort und in keiner Protokollzeile oberhalb von `DEBUG`. `Artefaktverweis`,
`Artefaktzeile`, `Artefaktinhalt` und `Abrufergebnis` überschreiben `toString()`, damit auch ein
versehentliches `log.debug("{}", …)` nichts preisgibt.

---

## 4. Warum SAAJ, und warum der `jakarta`-Zweig

**Warum SAAJ und kein eigener Envelope.** Weil es gemessen ist. M59 (2) hat den Envelope von Hand
gebaut und mit `curl` gesendet — der Empfänger antwortete mit einer leeren `FileList`, ohne Anhang
und ohne `Fault`. M71 hat denselben Verweis mit dem echten Client geholt und bekam die Datei.
Zwischen beiden liegen **zwölf** Unterschiede in der Transportform (M70); **welcher davon den
Ausschlag gibt, ist nicht gemessen**. Solange das so ist, wird die Transportform nicht nachgebaut —
auch nicht mit einem HTTP-Client, der bequemere Zeitgrenzen hätte.

**Warum der `jakarta`-Zweig.** Zwei Gründe, einer gemessen und einer aus der Schnittstelle:

1. **M72** hat beide Zweige gegen denselben lokalen Lauscher laufen lassen und die Bytes
   verglichen: Anfragezeile, acht Kopfzeilen in gesendeter Reihenfolge und 276 Byte Rumpf sind
   identisch; nach Ersetzen des Loopback-Ports dieselbe SHA-256 über die vollständigen 579 Byte.
2. **`javax.xml.soap.SOAPConnection` kennt `setReadTimeout` überhaupt nicht.** Erst
   `jakarta.xml.soap` 3.x hat `setConnectTimeout` und `setReadTimeout`, und `saaj-impl` 3.x reicht
   beide an die `HttpURLConnection` durch. Mit dem `javax`-Zweig gäbe es eine Zeitgrenze nur
   JVM-weit über die Systemeigenschaften `saaj.connect.timeout` und `saaj.read.timeout`.

   > *Gelesen war:* die Signaturen beider `SOAPConnection`-Klassen und der Bytecode von
   > `com.sun.xml.messaging.saaj.client.p2p.HttpSOAPConnection` aus `saaj-impl 3.0.6`.
   > *Behauptet wird:* nur das, was dort steht. Ob die Zeitgrenze auf der Leitung greift, ist
   > **nicht gemessen** — dazu bräuchte es einen Knoten, der annimmt und dann schweigt.

| Abhängigkeit | Version |
|---|---|
| `jakarta.xml.soap:jakarta.xml.soap-api` | 3.0.2 |
| `com.sun.xml.messaging.saaj:saaj-impl` | 3.0.6 (`runtime`) |
| transitiv | `jakarta.activation-api` 2.1.3, `angus-activation` 2.0.3, `stax-ex` 2.1.0 |

**Was nachgestellt wird:** die Reihenfolge aus `FilestoreClient.java:65`–`:77` — Kopf abtrennen,
`m:FileList` im Namensraum `http://filestore.kraftwerkone.de`, darin ein `File` mit `Counter`, `ID`
und `Action`. Dass die Attribute auf der Leitung *alphabetisch* erscheinen und nicht in dieser
Reihenfolge, ist eine Eigenschaft der Serialisierung und aus dem Quelltext nicht ableitbar (M70,
Befund 2) — deshalb bleibt die Reihenfolge des Originals stehen und wird nicht „korrigiert".

**Was nicht nachgestellt wird:** der Schreibpfad (der Alt-Client legt den Anhang selbst als Datei
ab, `:112`–`:117`; dieses Werkzeug schreibt nichts auf Platte) und `CREATE` (dieses Werkzeug sendet
ausschließlich `RETRIEVE`; die Aussage von M72 gilt auch nur dafür).

---

## 5. Der Beschnitt

Gilt **nur** bei Artefakten mit `Log.GUID` im Namen und **nur** für `MANDANT` — über die Rolle, nie
über ein Kennzeichen aus der Anfrage.

| Fall | Ergebnis | gegenüber dem Altsystem |
|---|---|---|
| Vollständiges Paar | Innenbereich ohne die Markenzeilen | identisch (Q2) |
| Mehrere Paare | **nur das erste** | **strenger** — das Altsystem hängt alle Blöcke aneinander (`:841`) |
| Startmarke ohne Endmarke | **nichts**, benannter Zustand | **strenger** — das Altsystem zeigt alles ab der Startmarke (`:823`) |
| Keine Startmarke | **nichts**, benannter Zustand | identisch (Q2) |
| Paar ohne Inhalt | **nichts**, benannter Zustand | identisch im Ergebnis |

**Der fünfte Fall ist eine Ableitung, keine eigene Entscheidung.** `rohdaten.md` §6 nennt ihn nicht
getrennt, weil er dasselbe Ergebnis hat wie „kein vollständiges Paar": Es gibt nichts zu zeigen. Er
bekommt deshalb denselben benannten Zustand statt eines leeren Kastens — §8 sagt ausdrücklich, dass
keiner der Zustände ein leeres Feld ist. Er kommt vor: Die Selbstprüfung des Auswertungsskripts hat
ihn an `3.log` erzeugt, wo eine echote Zeile das Paar sofort schließt.

**Erkannt wird mit *enthält*, nicht mit Gleichheit** — wie im Altsystem (Q2). Die Markenzeilen
selbst erscheinen nicht in der Ausgabe. Zerlegt wird an `\r\n`, `\n` und `\r`, weil die Zeilenenden
uneinheitlich sind (M61: LF 95, CRLF 38, gemischt 37, keines 36 von 206).

**Pfadmaskierung nur im beschnittenen Zweig** (Q3). `ADMIN` bekommt vollständig und unmaskiert.

> **Der Kontoname steht nicht im Quelltext.** Das Altsystem ersetzt `/opt/txp/users/<dienstkonto>/`
> mit dem Namen als festem Literal. Hier steht stattdessen ein Muster — *ein* Pfadabschnitt unter
> `/opt/txp/users/`. Das ist kein Kompromiss, sondern besser: Es maskiert auch ein zweites
> Dienstkonto und veraltet nicht, wenn das erste umbenannt wird. Der Name ist ein produktiver
> Bezeichner (Regel G1) und wurde aus demselben Grund schon aus `messungen-schritt8.md` entfernt.

**Eine Abweichung vom Altsystem, bewusst:** Dort steht `else if`, es wird je Zeile höchstens *ein*
Präfix maskiert. Hier laufen beide Ersetzungen. Der Unterschied ist im gemessenen Bestand nicht
beobachtet, kostet nichts und beseitigt einen Fall, in dem das Ergebnis von der Reihenfolge im
Quelltext abhinge.

> **Der Beschnitt ist eine Lesbarkeitsregel und keine Vertraulichkeitszusage.** Er hält weniger
> zurück, als sein Name nahelegt: 808 Pfadzeilen und 331 Dienstkennungen liegen **innerhalb** der
> Marken (M65), und der Innenbereich ist mit 90,4 % der Bytes der überwiegende Teil (M67). Die
> Marken sind zudem von außen beeinflussbar — das Protokoll gibt Werte aus der EDI-Datei aus. Er ist
> trotzdem gebaut, weil das die Entscheidung ist (`rohdaten.md` §3, Entscheidung 5). Niemand soll
> später eine Zusage darauf gründen.

**Bekannte Folge, ausdrücklich hingenommen:** Bei `HTTPSender` und `FTPSender` sieht `MANDANT`
damit **nie** ein Protokoll (M63: 30 von 30 bzw. 28 von 30 ohne Marken). `FTPSender` hängt an rund
69 % der Nachrichten.

---

## 6. Die fünf Zustände

| Zustand | Auslöser | Anzeige | Download |
|---|---|---|---|
| `ANZEIGBAR` | Regelfall | Text | Datei |
| `BINAERDATEI` | Nullbyte, oder unter 95 % druckbare Zeichen (M61: 18,2 % der Nutzdateien) | benannt, kein Text | **Datei** — außer bei einem Protokoll für `MANDANT`, dann `409` |
| `KEIN_ANZEIGBARER_PROTOKOLLTEIL` | Beschnitt greift, kein vollständiges Paar (M63) | benannt | `409` |
| `DATEI_NICHT_VORHANDEN` | Ablage antwortet, liefert nichts (M68, M66 (2)) | benannt | `404` |
| `ABLAGE_NICHT_ERREICHBAR` | Kennung löst nicht auf, Knoten antwortet nicht, Anhang unlesbar oder zu groß | benannt | `502` |

> **Die eine Ausnahme bei `BINAERDATEI` ist der Grund, warum die Binärprüfung im beschnittenen
> Zweig steht und nicht davor.** Stünde sie davor — wie zunächst gebaut —, bekäme ein
> Mandantennutzer die **vollständige, unmaskierte Protokolldatei**, sobald sie als binär eingestuft
> wird. Und die Einstufung hängt am Inhalt, den das Protokoll teilweise aus der EDI-Datei des
> Partners echot: ein einziges Nullbyte in einem echoten Wert genügte.
>
> Dass 0 % der gemessenen Protokolle binär sind (M61), ist eine Beobachtung an **acht** Dateien und
> keine Zusage. Entscheidung 9 sagt „es gibt keinen Pfad" — dann darf es auch keinen geben, der nur
> selten begangen wird. Eine binäre Datei hat keinen Innenbereich zwischen Marken; für `MANDANT`
> gibt es dort also nichts, und das ist die richtige Antwort. `ADMIN` bekommt dieselbe Datei
> unverändert, und eine binäre **Nutzdatei** bleibt für beide Rollen herunterladbar — dort gibt es
> keinen Beschnitt, an dem etwas vorbeiführen könnte.

**Die Anzeige antwortet in allen fünf Fällen mit `200`.** Ein Fehlerstatus wäre dort falsch:
„Protokoll ohne Marken" ist bei `FTPSender` der Normalfall und darf sich für den Nutzer nicht von
„Nachricht gibt es nicht" ununterscheidbar anfühlen.

**Der Download kann das nicht.** Eine Datei mit null Byte auszuliefern wäre formal der Gleichlauf,
praktisch aber eine irreführende Antwort. Deshalb ein Fehlerrumpf nach RFC 9457 mit eigenem
Problemtyp je Zustand — dieselben Schlüssel, die auch die Anzeige liefert.

> **Das `404` für „Datei nicht vorhanden" ist von dem der unbekannten Nachricht zu unterscheiden**,
> und das ist unbedenklich: Diesen Problemtyp bekommt nur, wer die Nachricht ohnehin sehen darf. Wer
> eine fremde `MessageID` rät, kommt gar nicht bis hierher.

**Die Binärprüfung läuft auf den Bytes, nicht auf dem dekodierten Text.** `ISO-8859-1` bildet
*jedes* Byte auf ein Zeichen ab und scheitert nie; ein dekodierter Text trägt deshalb keine
Information mehr darüber, ob die Bytes Text waren. Bytes ab `0xA0` zählen als druckbar — dort liegen
in `ISO-8859-1` Umlaute und Akzente, und 8 von 8 Protokollen und 9 von 16 Nutzdateien sind kein
gültiges UTF-8 (M61); wer sie als Müll zählte, erklärte die Mehrheit der echten Protokolle zu
Binärdateien. Der Bereich `0x80`–`0x9F` zählt **nicht** mit: dort liegen unbelegte Steuerzeichen.

---

## 7. Anzeige und Download

**Anzeige liefert JSON**, niemals einen Bytestrom mit ratbarem Typ. Das Altsystem liefert für
Anzeige *und* Download denselben `application/octet-stream` (Q4); ein Bytestrom, dessen Typ der
Browser errät, ist der Weg, auf dem fremder Inhalt zu ausgeführtem Inhalt wird.

**Download liefert `Content-Disposition: attachment` und `Content-Type: application/octet-stream`**,
niemals `inline`, kein Erraten. Der Dateiname geht als `filename*` in UTF-8 hinaus (RFC 6266);
`ContentDisposition` baut zusätzlich eine ASCII-Fassung.
`X-Content-Type-Options: nosniff` wird **nicht** eigens gesetzt — Spring Security schickt es auf
jeder Antwort mit, und ein zweites Setzen ergäbe die Kopfzeile doppelt.

**Dateiname** aus `%.FileProperty.OriginalFilename` **auf demselben Schritt**, wo vorhanden (69,6 %
der Nachrichten, M17), sonst konstruiert als `<Familie>-schritt<N>-<art>-<messageId>`. Das Altsystem
verwendet die `MessageID` (Q4) — 36 Zeichen, und für alle Artefakte derselben Nachricht dieselben.

> **Gesucht wird über das Muster, nicht über die Familie des Artefakts.** Der Unterschied fällt am
> wichtigsten Artefakt auf: Der Eingang heißt `Message.Payload.GUID`, seine Familie also `Message` —
> aber `Message.FileProperty.OriginalFilename` **gibt es nicht**. Den Namen tragen ausschließlich die
> Lesedienste: `FileReader.FileProperty.OriginalFilename` (4.352 bzw. 122.609 Zeilen) und
> `FTPReader.FileProperty.OriginalFilename` (111 bzw. 2.034), M56 (a) in beiden Fenstern. Über die
> Familie gesucht bekäme ausgerechnet die eingegangene Datei — der Kopf der Liste und das
> naheliegendste Ziel eines Nutzers — nie ihren echten Namen, obwohl er in derselben Nachricht auf
> demselben Schritt steht.
>
> **Eingegrenzt bleibt es trotzdem, nämlich auf den Schritt.** Der Originalname beschreibt die
> Datei, die *eingegangen* ist. Für ein `Converter.Payload.GUID` auf Schritt 2 ist das eine andere
> Datei; ihm den Namen des Eingangs zu geben wäre eine Falschauskunft. Auf einem Wandlungsschritt
> greift deshalb der konstruierte Name.

> **Korrigiert 19.08.2026 — der Kasten darüber beschreibt ein Artefakt, das es nicht mehr gibt, und
> er beschrieb einen zweiten Fehler, den er selbst benennt.**
>
> **1. Was falsch ist.** „Der Eingang heißt `Message.Payload.GUID`" und „der Kopf der Liste" —
> beides trifft nicht mehr zu. Nach **M73** trägt der Name in **6.249 von 6.249** und **214.330 von
> 214.330** Nachrichten den Verweis der Nutzdatenzeile mit dem **höchsten `MessageActionID`**
> derselben Nachricht; er ist aus der Artefaktliste entfallen, und die Antwort ist zweigeteilt.
>
> **2. Der zweite Fehler, und er ist der teurere.** Der zweite Absatz formuliert den Grundsatz
> selbst: *„Der Originalname beschreibt die Datei, die eingegangen ist. Für ein
> `Converter.Payload.GUID` auf Schritt 2 ist das eine andere Datei; ihm den Namen des Eingangs zu
> geben wäre eine Falschauskunft."* **Genau das ist auf dem Umweg über Schritt `0` geschehen.**
> Für `0-Message.Payload.GUID` suchte der Download `%.FileProperty.OriginalFilename` auf Schritt
> `0` und fand den Namen der **eingegangenen** Datei — der Inhalt dahinter war nach M73 aber die
> Datei des höchsten Nutzdatenschritts. Wer diese Datei herunterlud, **hätte** damit den Stand eines
> späteren Schritts unter dem Namen seines eingegangenen Belegs gespeichert — in der Mehrheit den
> eines Sendedienstes (70,09 % / 53,37 %), sonst den des Converters (29,89 % / 46,62 %), M73.
> **Ob es jemand getan hat, ist nicht bekannt** und wird hier nicht behauptet: Das Feature war einen
> Tag alt, und ein Zugriffsprotokoll ist dazu nicht ausgewertet worden. **Der Weg dorthin ist mit
> dem Wegfall des Artefakts geschlossen** und stand einen Tag lang offen.
>
> **3. Die Regel bleibt trotzdem stehen.** Sie ist nicht falsch geworden, nur **gegenstandslos für
> den Fall, für den sie begründet wurde**. Ein Umbau der Dateinamenssuche ist nicht Teil dieser
> Runde.
>
> **4. Ob ein anderer Fall bleibt — benannt, nicht gemessen.** Muster- und Familiensuche gehen
> weiterhin auseinander, sobald auf dem Schritt eines Artefakts ein
> `FileReader.`/`FTPReader.FileProperty.OriginalFilename` liegt und das Artefakt einer **anderen**
> Familie angehört: über das Muster bekäme es einen Namen, über die Familie keinen.
>
> > **Belegvermerk** (Regel L10).
> >
> > *Gemessen ist:* welche `MessagePropertyName` den Originalnamen tragen — nur `FileReader` und
> > `FTPReader` (M56 a, beide Fenster).
> >
> > *Nicht gemessen ist:* auf welchem `MessageActionID` diese Zeilen liegen, und ob dort Artefakte
> > fremder Familien sitzen. M56 (a) gruppiert nach Namen, nicht nach Schritt.
> >
> > *Ausdrücklich nicht behauptet:* dass der Fall vorkommt — und ebenso wenig, dass er nicht
> > vorkommt. Er ist **offen** und steht als Punkt 9 in [`rohdaten.md`](rohdaten.md) §13.
>
> **5. Was unberührt bleibt.** Das Muster `%.FileProperty.OriginalFilename`, die Eingrenzung auf
> denselben `MessageActionID`, der konstruierte Name ohne Endung, die Bereinigung des Namens vom
> Partner — nichts davon ist angefasst worden. **Kein stilles Überschreiben:** Der Kasten oben
> bleibt wörtlich stehen.

> **Der konstruierte Name bekommt keine Endung.** M56 (c) hat die Endungen der Originalnamen
> erhoben: **4.307 von 4.352** in Fenster A enden auf einen Punkt und eine reine *Ziffernfolge*;
> `.txt` kommt viermal vor, `.cod` einmal. Die echten Dateinamen tragen selbst keine Typangabe. Eine
> zu erfinden hieße, dem Nutzer eine Information zu geben, die die Quelle nicht hat (Regel Q4).

**Der Originalname kommt vom Partner** und wird vor dem Kopf bereinigt:

- **Steuerzeichen**, darunter CR und LF — sie würden den Kopf spalten.
- **Pfadzeichen und führende Punkte** — `..` und `/` deuteten einen Pfad an.
- **Unicode-Formatzeichen**, darunter `U+202E` RIGHT-TO-LEFT OVERRIDE und die Zeichen ohne Breite.
  Sie sind unsichtbar und drehen die Anzeige des Dateinamens um; aus `rechnung<U+202E>gpj.exe` wird
  im Speichern-Dialog scheinbar `rechnungexe.jpg`. `ContentDisposition.filename(name, UTF_8)`
  prozentkodiert sie zwar für die Leitung, aber der Browser dekodiert sie wieder — die Kodierung ist
  also **kein** Schutz. Umlaute bleiben; entfernt wird die Kategorie `FORMAT`, nicht alles
  Nicht-ASCII.
- **Auf 200 Zeichen begrenzt** (längster gemessener Name: 127, M56).

### Gleichlauf (Entscheidung 9)

Anzeige und Download gehen durch **dieselbe** Aufbereitung — einen Codepfad, nicht zwei. Wären es
zwei, müsste jemand sie synchron halten, und der Tag, an dem das misslingt, ist der Tag, an dem ein
Mandantennutzer die vollständige Protokolldatei bekommt.

| Aufrufer und Artefakt | Anzeige | Download |
|---|---|---|
| `MANDANT`, Protokoll | beschnitten + maskiert | **dieselbe** beschnittene, maskierte Fassung |
| `ADMIN`, Protokoll | vollständig, unmaskiert | Bytes des ZIP-Eintrags, unverändert |
| beide, Nutzdaten | vollständig | Bytes des ZIP-Eintrags, unverändert |

**Zwei Stellen, an denen sich Anzeige und Download unterscheiden dürfen — und warum das kein
Widerspruch ist:**

1. **Kappung.** Die Anzeige wird bei 1 MiB gekappt, der Download nicht. Die Kappung schützt den
   Browser, nicht die Vertraulichkeit; `rohdaten.md` §7 nennt sie ausdrücklich als
   Schutzmaßnahme mit sichtbarem Hinweis. Bei einem gemessenen Maximum von 609.995 Byte greift sie
   ohnehin nie.
2. **Binärdateien.** Die Anzeige zeigt keinen Text, der Download liefert die Datei. Entscheidung 9
   richtet sich gegen einen Pfad, auf dem `MANDANT` *mehr* bekommt als in der Anzeige — hier
   bekommen beide Rollen dasselbe, und die Anzeige kann Bytes nur nicht als Text darstellen.

---

## 8. Protokollierung

Drei Ereignisarten in `audit_log`, geschrieben über den **Schreib-Kontext** auf `overlord_monitor`.
**Kein Schreibzugriff auf `GlassfishDB`, in keiner Form.**

| `event_type` | wann | `detail` |
|---|---|---|
| `ROHDATEN_ANGESEHEN` | Anzeige mit Inhalt oder mit Zustand *kein anzeigbarer Protokollteil* / *Binärdatei* | `Fassung: beschnitten` oder `Fassung: vollstaendig` |
| `ROHDATEN_DOWNLOAD` | Download geliefert | dasselbe |
| `ROHDATEN_ABRUF_FEHLGESCHLAGEN` | Ablage nicht erreichbar, Datei nicht vorhanden, oder Download ohne Inhalt | `Zustand: <einer der fünf>` |

Festgehalten werden außerdem Nutzer (`actor_user_id`, `actor_username`), Mandant, Zeitpunkt (aus
der **Systemuhr in UTC**, nie aus der Anwendungsuhr) und IP. `target_type` ist
`rohdaten-artefakt`, `target_id` ist `<messageId>#<artefaktId>`.

**Die Fassung ist der Punkt.** Ein Eintrag, der beschnitten und vollständig nicht unterscheidet, ist
bei einer Rückfrage wertlos — und die Rückfrage ist genau der Grund, warum es das Protokoll gibt.

**Kein Dateiinhalt, kein Dateiname, keine UUID, keine Ablagenkennung, keine Adresse** im Eintrag.
`ROHDATEN_ANGESEHEN` und `ROHDATEN_ABRUF_FEHLGESCHLAGEN` sind neu; `ROHDATEN_DOWNLOAD` steht seit
Schritt 2 im Aufzählungstyp. Eine Migration kostet das nicht — `audit_log.event_type` ist Text mit
Whitelist im Code.

**Die Artefaktliste wird nicht protokolliert.** Sie holt keine Datei; ein Eintrag je Listenaufruf
verwässerte die Einträge, die etwas bedeuten.

---

## 9. Messungen (Regel L7)

Erhoben am **18.08.2026** gegen die Testkopie. `scripts/messung-schritt8/sitzung8-rohdaten.sql`.
`@@global.read_only` war zu Beginn und am Ende **`1`**.

**Warum neu gemessen wurde.** M58 hat drei Statements gemessen, die den Mandantenfilter als **Join**
über `Process`/`ProjectMandant` tragen. Der gebaute Code verwendet die im Projekt eingeführte
**`EXISTS`**-Form (Regel M3, Begründung in `NachrichtendetailRepository`: `ProjectMandant` ist im
Schema n:m, ein Join könnte Zeilen vervielfachen — hier erschiene jedes Artefakt doppelt in der
Liste). Dazu kommen zwei Statements, die M58 nicht kennt.

**Prüfnachrichten** wie bei M58: je Mandant die Nachricht aus Fenster A mit den **meisten**
Artefaktverweisen, Gleichstand nach kleinster `MessageID`. Gemessen trägt die `NEXANS`-Nachricht
**13** Artefakte, die `IBISGUS`-Nachricht **7** — dieselben Zahlen wie bei M58.

### Zugriffspfade

**(1) Artefaktliste**

```
| table             | type  | key     | key_len | ref         | rows | Extra                       |
| Message           | const | PRIMARY | 146     | const       | 1    |                             |
| mandanten_process | const | PRIMARY | 146     | const       | 1    |                             |
| ProjectMandant    | const | PRIMARY | 292     | const,const | 1    | Using index                 |
| MessageProperty   | ref   | PRIMARY | 146     | const       | 38   | Using where; Using filesort |
```

**(2) Existenznachweis** — `No tables used` auf der äußeren Ebene, darunter `UNCACHEABLE SUBQUERY`
mit `Message`, `mandanten_process` und `ProjectMandant` je `const`.

**(3) Auflösung der Ablagenkennung** — `Service` `const` über `PRIMARY`, darunter dieselbe
`UNCACHEABLE SUBQUERY` mit drei `const`-Zeilen.

**(4) Originaldateiname** — alle vier Tabellen `const`; `MessageProperty` mit `key_len 550`, also
über den **vollständigen** Primärschlüssel `(MessageID, MessagePropertyName, MessageActionID)`.

### Laufzeiten

Beste von fünf nach einem Aufwärmlauf, wie bei M58.

| Abfrage | Mandant | Artefakte | Aufwärmlauf | **beste von fünf** | M58 zum Vergleich |
|---|---|---|---|---|---|
| (1) Artefaktliste | `NEXANS` | 13 | 1,082 ms | **0,867 ms** | 0,748 ms |
| (1) Artefaktliste | `IBISGUS` | 7 | 0,906 ms | **0,833 ms** | 0,702 ms |
| (2) Existenznachweis | `NEXANS` | — | 0,528 ms | **0,497 ms** | 0,512 ms |
| (3) Auflösung *mit* Mandantenkette | `NEXANS` | — | 0,728 ms | **0,668 ms** | 0,336 ms *(ohne Kette)* |
| (4) Originaldateiname | `NEXANS` | — | 0,740 ms | **0,654 ms** | — *(neu)* |

**Was ein Endpunkt kostet:**

| Endpunkt | Statements | Summe |
|---|---|---|
| Artefaktliste | (1), bei leerer Liste zusätzlich (2) | **0,867 ms** |
| Anzeige | (1) + (3) | **1,535 ms** |
| Download | (1) + (3) + (4) | **2,189 ms** |

**Befund 1 — die `EXISTS`-Form erzeugt denselben Plan wie der Join.**
*Gemessen war:* MariaDB flacht die `EXISTS`-Unterabfrage von Statement (1) ein — alle vier Tabellen
erscheinen mit `select_type = PRIMARY`, `Message`, `mandanten_process` und `ProjectMandant` je
`const`, `MessageProperty` als `ref` über `PRIMARY` mit `rows 38`. Das ist Zeile für Zeile der Plan
aus M58 (1).
*Behauptet wird:* Die im Projekt eingeführte Schreibweise kostet an dieser Stelle keinen
Zugriffspfad. Über andere Statements sagt das nichts.

**Befund 2 — `Using filesort` ist neu, und der Grund ist die Sortierung.**
*Gemessen war:* Statement (1) zeigt `Using where; Using filesort`, M58 (1) nur `Using where`. Die
Laufzeit steigt von 0,748 ms auf 0,867 ms, also um **0,119 ms**.
*Behauptet wird:* Der Unterschied kommt von `ORDER BY MessageActionID, MessagePropertyName`. Der
Primärschlüssel führt die andere Reihenfolge, also wird sortiert — über 38 Zeilen in einem Puffer.
**Die Sortierung bleibt**, weil die Oberfläche die Artefakte je Schritt gruppiert und die
Reihenfolge damit an einer Stelle steht statt zusätzlich im Frontend. 0,119 ms auf einem Pfad, der
von einem 38-bis-244-ms-SOAP-Aufruf beherrscht wird, sind kein Argument.

**Befund 3 — die Mandantenkette in der Auflösung verdoppelt sie und kostet trotzdem nichts.**
*Gemessen war:* 0,668 ms gegen 0,336 ms bei M58 (2) ohne Kette. Faktor 1,99.
*Behauptet wird:* Der Preis für „eine Ablagenadresse gibt es nur zu einer Nachricht, die der Mandant
sehen darf" ist ein Primärschlüsselzugriff. Er liegt absolut unter einer Millisekunde und damit
weit unter der 50-ms-Erwartung aus M58.

**Befund 4 — der teuerste und der kleinste Mandant unterscheiden sich nicht.**
*Gemessen war:* 0,867 ms gegen 0,833 ms bei (1). Die Kontrolle nach L15 ist unauffällig.
*Behauptet wird:* Der Zugriff hängt an der `MessageID` und nicht an der Größe des Mandanten — was
der `EXPLAIN` mit `const` auf drei von vier Tabellen auch zeigt.

> **Abweichung bei der Laufzeitmessung, wie bei M58.** Die Messläufe von (1), (3) und (4) laufen
> gegen eine **zählende Hülle** um das echte Statement, weil die echten Statements
> `MessagePropertyValue` und `ServiceConnectString` liefern und diese Werte nach G1 nicht auf den
> Bildschirm dürfen. Der **`EXPLAIN` läuft gegen das rohe Statement**, unverändert. Die vier
> Statements in der Skriptdatei sind **wörtlich** die von jOOQ gerenderten; ersetzt sind nur
> `MessageID` und Ablagenkennung durch Sitzungsvariablen, die in der Sitzung selbst hergeleitet
> werden.

---

## 9a. Die beiden Nachträge

**Der Dev-Zeitanker liegt jetzt im abrufbaren Fenster.** `common/ZeitConfig` begrenzt die
Ankerabfrage — und ihre Rückfallebene — auf `2025-07-24` bis `2025-12-30`. Im Profil `prod` ändert
sich nichts; dort ist die Anwendungsuhr die Systemuhr und die Abfrage läuft gar nicht.

> **Ein Riegel, keine Korrektur** — und das gehört gesagt, weil der Auftrag es anders vermutete.
> *Gemessen war* am 18.08.2026: Die Ankerabfrage liefert **mit und ohne Fenster denselben Wert**,
> `2025-12-30 04:09:47`. Der Anker zeigt also **nicht** auf den `2026-07-08`. Er tat es bis zum
> 01.08.2026, als er noch `MAX(Message.MessageLastUpdate)` war; seit der Umstellung auf „jüngster
> Tag mit mindestens drei Mandanten" schließt er die vereinzelten Tage in `2026-06` und `2026-07`
> von selbst aus, weil dort an keinem Tag ein dritter Mandant Daten hat.
> *Behauptet wird:* Das Fenster ändert heute nichts und ist trotzdem richtig — **ohne es hängt die
> Eigenschaft am Bestand statt an einer Regel.** Eine Neubefüllung der Testkopie mit drei Mandanten
> an einem jungen Tag genügte, und der Rohdatenzugriff sähe lokal ab dem ersten Start kaputt aus.
> Die Rückfallebene `MAX(MessageLastUpdate)` **zeigte ohne das Fenster sehr wohl auf den
> `2026-07-08`** — sie ist deshalb mitbegrenzt.

**`docs/START-LOKAL.md` ist neu** und trägt den Hinweis: von der Entwicklungsmaschine sind 63,2 %
des Bestands nicht abrufbar, weil `FILESTOREPROD07`/`08` abgeschaltet sind; produktiv gibt es das
nicht.

---

## 10. Abweichungen von `rohdaten.md`

Fünf, alle vorsätzlich und alle hier statt in einer Fußnote.

1. **Die Artefaktliste trägt keinen lesbaren Schrittnamen.** `rohdaten.md` §5 beschreibt die
   Beschriftung: `SOSActionName` wo er auflöst, sonst Schrittnummer plus technische Familie. Die
   Liste liefert stattdessen `schritt` (die `MessageActionID`) und `familie`. **Die Zuordnung ist
   trotzdem möglich und kostet keine zweite Abfrage:** `GET /api/nachrichten/{messageId}` liefert
   die Schrittfolge mit `position` — und `position` *ist* die `MessageActionID`. Die Oberfläche
   verbindet die beiden über diese Zahl.

   *Warum nicht hier auflösen:* Die dreistufige Namensauflösung (`Schrittnamen`) liegt im Paket
   `message`, und Fachpakete kennen einander nicht. Sie zu kopieren hieße, dieselbe Regel zweimal zu
   pflegen; sie nach `common` zu verschieben hieße, einen Umbau in denselben Diff zu legen wie ein
   neues Feature — dieselbe Begründung, aus der das Paket `payload` nicht umbenannt wird. §11 der
   Vorgabe verlangt außerdem, nicht über den Umfang des Schritts hinauszubauen, und §5 beschreibt
   die *Oberfläche*.

2. **Ein fünfter Beschnittfall.** „Vollständiges Paar ohne Inhalt" steht nicht getrennt in §6. Er
   bekommt denselben benannten Zustand wie „kein vollständiges Paar", weil das Ergebnis dasselbe
   ist und §8 einen leeren Kasten ausschließt. Begründung in §5 dieser Datei.

3. **Statuscodes des Downloads bei den drei inhaltslosen Zuständen.** `rohdaten.md` legt sie nicht
   fest — sie sind eine Folge daraus, dass ein Anhang Bytes braucht. Gewählt: `409`, `404`, `502`,
   je mit eigenem Problemtyp. Begründung in §6.

4. **Die Pfadmaskierung verwendet ein Muster statt des Kontonamens**, und sie maskiert je Zeile
   beide Präfixe statt höchstens eines. Begründung in §5.

5. **Kein Eintrag in `docs/README.md`.** `CLAUDE.md` verlangt für jede neue Datei in `docs/` einen
   Indexeintrag; der Auftrag zu diesem Schritt untersagt Änderungen an `docs/README.md`
   ausdrücklich. Der Auftrag ist die speziellere Anweisung und hat Vorrang. **Die beiden neuen
   Dateien — diese und `START-LOKAL.md` — fehlen deshalb im Index** und sind nachzutragen, sobald
   die Sperre aufgehoben ist. Siehe offener Punkt 1.

---

## 11. Offene Punkte

| # | Punkt |
|---|---|
| 1 | **`docs/README.md` fehlen zwei Einträge** — `rohdaten-backend.md` und `START-LOKAL.md`. Der Auftrag zu Schritt 8 untersagt Änderungen an dieser Datei; nachzutragen, sobald das nicht mehr gilt |
| 2 | **Das Paket `payload` heißt nicht mehr passend.** Der Name stammt aus Abschnitt 6 der Projektbeschreibung, als das Feature ein reiner Download-Proxy für die Nutzdatei sein sollte. Gebaut ist mehr: Protokolle gehören dazu, die Anzeige ist der Regelfall, und ein Proxy ist es nicht, weil die Ablage SOAP spricht und ein ZIP liefert. **Vorschlag: `rohdaten`.** Ein Paketumbenennen gehört nicht in denselben Diff wie ein neues Feature und berührt `PROJEKTBESCHREIBUNG.md`, `IMPLEMENTIERUNGSPLAN_MVP.md` und `PaketstrukturTest` |
| 3 | **Die Antwortverarbeitung des `jakarta`-Zweigs ist ungemessen** (M72, Nachtrag 29). M72 vergleicht, was *gesendet* wird. Ob `saaj-impl 3.0.6` einen ZIP-Anhang so entgegennimmt wie `1.5.3` es in M71 tat, entscheidet sich erst an einem echten Abruf |
| 4 | **Die Zeitgrenze ist gesetzt, aber nicht erprobt.** Dass `setReadTimeout` durchgereicht wird, ist aus dem Bytecode gelesen; dass sie auf der Leitung greift, bräuchte einen Knoten, der annimmt und dann schweigt |
| 5 | **Mehr als ein ZIP-Eintrag** ist nie vorgekommen (0 von 693). Der Fall wird behandelt, vermerkt und protokolliert — ob alle Einträge angeboten werden, ist offen (`rohdaten.md` §13, Punkt 6) |
| 6 | **Die Schwelle der Binärerkennung (95 % druckbare Zeichen) ist gesetzt, nicht gemessen.** Gemessen ist, dass die Textdateien bei 100 % liegen und die Binärdateien Nullbytes tragen (M61, M71) — zwischen 100 % und 95 % liegt im gemessenen Bestand nichts. Ob es in Produktion etwas dazwischen gibt, ist unbekannt |
| 7 | **Ein Umschalter auf UTF-8 ist nicht gebaut.** `rohdaten.md` §7 lässt ihn zu („zulässig"), verlangt ihn nicht. Die Voreinstellung `ISO-8859-1` ist gemessen |
| 8 | **`app_user.download_allowed` wird nicht geprüft — und das ist ein Widerspruch zwischen zwei Vorgaben.** Die Migration `V2__app_user.sql:29–32` legt das Flag an mit dem Kommentar „**Fuer Schritt 8 vorbereitet** […], damit ein spaeterer Entzug keine Migration erfordert (Regel R6)"; `AngemeldeterNutzer.downloadAllowed` trägt es bis in die Sitzung und `GET /api/auth/me` gibt es aus (`authentifizierung.md`). `rohdaten.md` §3 Entscheidung 2 sagt dagegen: „Alle Rollen sehen **alle Dateien** der Nachrichten, die sie ohnehin erreichen. **Keine zweite Berechtigungsstufe**." **Gebaut ist nach `rohdaten.md`**, weil das die verbindliche Vorgabe dieses Features ist — eine Prüfung einzubauen hieße, genau die zweite Stufe zu errichten, die Entscheidung 2 ausschließt. Damit ist das Flag derzeit ein totes Feld. **Zu entscheiden: fällt Entscheidung 2, oder fällt das Flag?** — **Geschlossen 20.08.2026: Spalte entfernt.** Es fällt das Flag. **Vollzogen am 21.08.2026 mit `V7__benutzerverwaltung.sql`** (Schritt 9a, dort E18): `app_user.download_allowed` ist entfernt, `AppUserZeile.downloadErlaubt`, `AngemeldeterNutzer.downloadAllowed` und die Ausgabe in `GET /api/auth/me` sind mit ihr entfallen ([`authentifizierung.md`](authentifizierung.md) §1, [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md) §2.1). Geprüft war vorher, dass das Frontend das Feld nirgends *liest* — es stand dort nur als Typzeile. `rohdaten.md` §3 Entscheidung 2 ist damit **bestätigt, nicht korrigiert**. Die drei Endpunkte aus Schritt 8 bleiben unangetastet — sie haben das Flag nie geprüft, und genau das war richtig |
| 9 | **Der Anlassfall der Regel „Muster statt Familie" ist entfallen, ein anderer bleibt möglich** *(neu am 19.08.2026, = offener Punkt 31 in [`messungen-schritt8.md`](messungen-schritt8.md))*. Ausführlich im Korrekturkasten in §7 samt Belegvermerk. **Nicht gemessen, nicht geändert, nicht entschieden** — der Umbau der Dateinamenssuche ist eine eigene Runde |
| 10 | **`Downloaddateiname` hat einen Tag lang eine falsch benannte Datei ausgeliefert** *(vermerkt 19.08.2026)*. Für `0-Message.Payload.GUID` fand die Suche `FileReader`/`FTPReader.FileProperty.OriginalFilename` auf Schritt `0` — den Namen der eingegangenen Datei — während der Inhalt nach M73 der des höchsten Nutzdatenschritts war. **Behoben** durch den Wegfall des Artefakts; hier vermerkt, weil ein Befund nicht mit der Zeile mitverschwinden soll, die ihn getragen hat |
| 11 | **Die Aussage „FileReader und FTPReader schließen einander aus" war ungedeckt** *(berichtigt 19.08.2026)*. Sie stand als Kommentar an der Sortierung in `findeOriginaldateiname` und berief sich auf M56 (a). **M56 (a) gruppiert je `MessagePropertyName`** und misst kein `DISTINCT` über beide; die 71,4 % in Befund 1 sind eine Addition der beiden Zeilen. Der Kommentar ist berichtigt, das Verhalten nicht — die feste Sortierung nach Namen deckt den Fall ohnehin ab |

---

## 12. Die Klassen

> ### ⚠️ Korrektur vom 10.09.2026 — drei dieser Klassen liegen nicht mehr in `payload`
>
> **Die Tabelle unten bleibt Zeichen für Zeichen stehen**, damit ablesbar bleibt, wie Schritt 8
> gebaut worden ist. Was gilt: `Ablagezugriff`, `SaajAblagezugriff` und `Abrufergebnis` liegen seit
> Schritt 10d Teil A in **`common`** — zusammen mit zwei neuen Typen, die es vorher nicht gab.
>
> | Klasse | Wo sie jetzt liegt | Was sich geändert hat |
> |---|---|---|
> | `Ablagezugriff` | `common` | nur der Paketname |
> | `SaajAblagezugriff` | `common` | Paketname; Zustandstyp; die Grenzen kommen aus `Ablagegrenzen` statt aus `RohdatenEigenschaften` |
> | `Abrufergebnis` | `common` | Paketname; trägt `Abrufzustand` statt `Artefaktzustand` |
> | **`Abrufzustand`** *(neu)* | `common` | die **drei** Fälle, die der Transport selbst unterscheiden kann: `GELIEFERT`, `DATEI_NICHT_VORHANDEN`, `ABLAGE_NICHT_ERREICHBAR` |
> | **`Ablagegrenzen`** *(neu)* | `common` | die **drei** Grenzen, die der Transport braucht — Obergrenze, Verbindungs- und Lesezeitgrenze |
> | `Artefaktzustand` | **bleibt in `payload`** | bekommt `aus(Abrufzustand)`: die Abbildung der engeren Menge in die weitere |
> | `RohdatenEigenschaften` | **bleibt in `payload`** | unverändert in Feldern und Schlüsseln; die Vorgabe für `maximalgroesse-bytes` kommt jetzt aus `Ablagegrenzen.VORGABE_MAXIMALGROESSE_BYTES` |
>
> **Der Grund ist ein zweiter Verbraucher und keine Umgestaltung.** Die Ablagenkachel des
> Dashboards prüft die Erreichbarkeit über **denselben Abrufweg** ([`dienste.md`](dienste.md) §7).
> Zwei Fachpakete dürfen einander nicht kennen — [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
> §6: *„Fachpakete kennen einander nicht. Gemeinsames liegt in `common`, nicht in einem
> Nachbarmodul."* Ein `import …payload.Ablagezugriff` in `dashboard` wäre genau der verbotene Fall,
> und `PaketstrukturTest.fachpakete_kennen_einander_nicht` hätte ihn gefangen.
>
> **Warum `Artefaktzustand` nicht mitgewandert ist.** Er kennt fünf Zustände, und zwei davon
> entstehen **nach** dem Abruf: `BINAERDATEI` bei der Binärprüfung, `KEIN_ANZEIGBARER_PROTOKOLLTEIL`
> beim Beschnitt. Der Transport sieht beide nie. `Abrufzustand` ist deshalb **nicht die halbe
> Aufzählung, sondern die vollständige Antwortmenge einer anderen Frage** — und die Abbildung steht
> in `payload`, weil dort die weitere Menge bekannt ist.
>
> **Warum zwei Datensätze denselben Zweig `overlord.rohdaten.*` lesen.** Weil **kein Schlüssel
> umbenannt** wird (Vorgabe des Auftrags). Die Überschneidung ist `maximalgroesse-bytes` — ein Wert,
> zwei Durchsetzungspunkte: der Transport bricht das **Lesen des Anhangs** daran ab, `payload` das
> **Entpacken des ZIP-Eintrags**. Die Vorgabe steht deshalb an genau einer Stelle im Code.
> `anzeige-grenze-bytes` ist bewusst **nicht** mitgewandert: Sie betrifft die Kappung der Anzeige und
> nicht den Transport.
>
> **Verhaltensgleich, und das ist nachgewiesen.** Die sechs Testklassen dieses Abschnitts sind
> inhaltlich unverändert grün (`ArtefaktServiceTest` 33, `ArtefaktbausteineTest` 24,
> `ArtefaktIdTest` 20, `ProtokollbeschnittTest` 12, `ArtefaktStatementsTest` 10 Testfälle,
> `RohdatenIsolationDbIT` 11 gegen die Testkopie — 110 zusammen, aus den Surefire- und
> Failsafe-Berichten des Laufs vom 10.09.2026 und nicht abgezählt). Angepasst sind an ihnen **zwei Importzeilen** in
> `ArtefaktServiceTest` und sonst nichts. Außerhalb der verschobenen Klassen besteht der ganze Diff
> aus Importen, der Verdrahtung in `config/RohdatenConfig` und der einen Zeile in
> `ArtefaktService`, die jetzt `Artefaktzustand.aus(abruf.zustand())` schreibt.
>
> **Der Paketname `payload` bleibt** — offener Punkt 2 unter §11, unverändert offen.

| Klasse | Zweck |
|---|---|
| `ArtefaktController` | Die drei Endpunkte. Holt Mandant und Rolle aus der Sitzung |
| `ArtefaktService` | Die Kette: auflisten, abrufen, aufbereiten, protokollieren. Ein Codepfad für Anzeige und Download |
| `ArtefaktRepository` | Vier jOOQ-Statements, jedes mit dem Mandantenfilter als `EXISTS` |
| `ArtefaktId` | Kennung aus Schritt und Name — ohne GUID, ohne Ablagenkennung. Streng geprüft |
| `Artefaktnamen` | Die beiden gemessenen Namensmuster, Art und technische Familie — **und die eine Ausnahme**: `Message.Payload.GUID` ist kein Artefakt (`NAME_ZEIGER`, `istZeiger`, M73). Sie steht dort und nicht im Statement (§7) |
| `Artefaktverweis` | `<Ablagenkennung>\|<UUID>` zerlegt. Verlässt das Backend nie |
| `Artefaktzeile` | Eine Zeile aus `MessageProperty`, wie das Repository sie liefert |
| `Artefaktart` | `NUTZDATEN` oder `PROTOKOLL` |
| `Artefaktzustand` | Die fünf Zustände |
| `Artefaktinhalt` | Das aufbereitete Artefakt — die eine Fassung für Anzeige und Download |
| `Ablagezugriff` | Schnittstelle zur Ablage. In Tests ersetzt, damit kein Test nach draußen spricht |
| `SaajAblagezugriff` | SOAP-`RETRIEVE` über den `jakarta`-Zweig von SAAJ, mit Zeitgrenzen |
| `Abrufergebnis` | Gepackte Bytes oder ein benannter Zustand. Nie eine Ausnahme nach außen |
| `Zipentnahme` | Erster Eintrag, gedeckelt gelesen, Zahl der Einträge im Ergebnis |
| `Binaerpruefung` | Nullbyte oder Anteil druckbarer Zeichen — auf den Bytes, vor der Dekodierung |
| `Protokollbeschnitt` | Erste Start- bis nächste Endmarke, fünf Fälle |
| `Pfadmaskierung` | Zwei Serverpfad-Präfixe zu `/IS/`, nur im beschnittenen Zweig |
| `Downloaddateiname` | Originalname wo vorhanden, sonst konstruiert; bereinigt |
| `AbrufFehlgeschlagenException` | RFC 9457 für die drei inhaltslosen Zustände beim Download |
| `RohdatenEigenschaften` | Die vier Grenzen, aus `overlord.rohdaten.*` |
| `ArtefaktlisteResponse`, `ArtefaktResponse`, `AnzeigeResponse` | Die Antworten |
| `config/RohdatenConfig` | Meldet `RohdatenEigenschaften` an. Eine Zeile, und die gehört nach `config` |

### Tests

| Test | Deckt ab |
|---|---|
| `RohdatenIsolationDbIT` | **Die drei Pflicht-Isolationstests** (Regel M4), je Endpunkt einer, plus: keine Mandanten-ID, keine Rolle, kein Verweis als Parameter. `@Tag("db")` |
| `ArtefaktStatementsTest` | Mandantenfilter in jedem Statement, kein Zugriff über den Wert, kein Join über den Verweis, kein Zeitfenster — **und beide Hälften der Ausnahme aus §7**: dass sie *nicht* im Statement steht, und dass `findeArtefakte` `Message.Payload.GUID` trotzdem nicht zurückgibt |
| `ArtefaktServiceTest` | Rolle aus der Sitzung, Gleichlauf, fünf Zustände, Kodierung, Kappung, zwei ZIP-Einträge, Protokollierung |
| `ProtokollbeschnittTest` | **Alle fünf Fälle** aus §5, gemischte Zeilenenden, eingeschleuste Marke, Maskierung |
| `ArtefaktIdTest` | Kennung stabil, URL-tauglich, ohne Verweis; zwölf unbrauchbare Formen |
| `ArtefaktbausteineTest` | Namensmuster, Verweisform, Binärerkennung, ZIP-Entnahme, Dateiname |

**Kein Test spricht einen Filestore an.** Jede Protokolldatei in den Tests ist **frei erfunden** —
kein echter Partner, kein echter Knoten, kein echter Pfad, keine echte Kennung.
