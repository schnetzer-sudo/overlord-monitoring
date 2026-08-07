# Nachrichtendetail

Entsteht in Schritt 5. **Teil 1 (Backend)** ist §1 bis §9, **Teil 2 (Oberfläche)** ist §10.

Der Detail-Endpunkt beantwortet die Frage, die die Liste offen lässt: **Was ist im Einzelnen
passiert?** Nicht „wo steht mein Beleg" (das ist [`nachrichtenliste.md`](nachrichtenliste.md)) und
nicht „was hängt daran" (das ist Schritt 6).

Grundlage ist die Erhebung [`messungen-schritt5.md`](messungen-schritt5.md) — M14 bis M22 und der
Nachtrag S1. Wo unten eine Zahl steht, steht dort ihr Statement.

---

## 1. Die beiden Endpunkte

```
GET /api/nachrichten/{messageId}                 Kopf, Schrittfolge, kuratierte Eigenschaften
GET /api/nachrichten/{messageId}/eigenschaften   alle technischen Eigenschaften, auf Abruf
```

Angemeldet, Mandant aus der Sitzung. **Beide nehmen eine `MessageID` entgegen und keine
Mandanten-ID** (Regel M1); die Ausnahmeliste in [`mandantentrennung.md`](mandantentrennung.md) §3
bleibt bei zwei Einträgen und wächst hier nicht.

**Kein Zeitfenster.** Regel L1 gilt für *Listen* über `Message`. Hier ist die Nachricht über ihren
Primärschlüssel benannt; ein Zeitfenster könnte nur noch ausschließen, was der Aufrufer bereits
gesagt hat.

### Antwort des Detail-Endpunkts

```json
{
  "messageId": "…",
  "status": "SUSPENDED",
  "statusKind": "WARTEND",
  "processId": "…",
  "processName": "40000_AMG_LAB_VDA",
  "projectName": "300_KundenEingehend",
  "sosName": "Versand Einzel IDOC aus Split",
  "zeitpunkt": "2025-12-29T22:53:50Z",
  "start": "2025-12-29T22:41:12Z",
  "timeoutSekunden": 1800,
  "eigenschaftenAnzahl": 22,
  "offenerZustand": "WARTET_VOR",
  "naechsterSchritt": "Send Message to Pool",
  "schritte": [
    {
      "position": 1,
      "name": "Datei konvertiert",
      "namensherkunft": "DIREKT",
      "rohwert": "NXS_FILE_CONVERT|E2A|UNWRAP",
      "start": "2025-12-29T22:41:12Z",
      "ende": "2025-12-29T22:41:12Z",
      "dauerSekunden": 0,
      "timeoutSekunden": 1800,
      "laeuftAuf": false
    }
  ],
  "kuratierteEigenschaften": [
    { "name": "Message.SplitCount", "wert": "27", "rang": 2 }
  ]
}
```

**Status doppelt, und das mit Absicht** — dieselbe Begründung wie in der Liste: `status` ist der
Rohwert des Altsystems, `statusKind` die fachliche Einordnung. Beide kommen aus derselben Stelle,
`common/MessageStatusClassifier`; **hier wird nicht neu klassifiziert.**

`bedeutungNichtVerifiziert` gibt es hier **nicht**. Es ist genau `statusKind == "UNGEKLAERT"` und
damit aus der Antwort ableitbar; ein zweites Feld dafür wäre eine zweite Wahrheit. Die Liste führt
es, weil sie es je Zeile braucht — das Detail nicht.

**`zeitpunkt` ist `MessageLastUpdate`, `start` ist der fachliche Start.** Die Quelle hat kein
Anlagedatum (Regel Q2); der Start ist `MIN(MessageAction.MessageActionStart)` über **alle**
Aktionen, einschließlich des Metadaten-Schritts. Ihn auszunehmen ergäbe einen zu späten Start — an
ihm kommt die Nachricht ins System (M17 3).

**Die Anzahl der Eigenschaften steht im Kopf, obwohl die Eigenschaften selbst nicht mitkommen.**
Ohne sie könnte die Oberfläche den eingeklappten Block nicht beschriften, ohne ihn zu laden — womit
der zweite Endpunkt seinen Zweck verlöre.

### Antwort des Eigenschaften-Endpunkts

```json
[
  { "name": "Message.GUID", "wert": "…", "position": 0, "gekappt": false, "originalLaengeBytes": null },
  { "name": "FTPSender.TransactionID", "wert": "…", "position": 2, "gekappt": false, "originalLaengeBytes": null }
]
```

Eine nackte Liste, keine `Seite`-Hülle: Es gibt nichts zu blättern. Gemessen sind rund **22,6
Eigenschaften und 595 Byte je Nachricht** (M17 1 und 2) — die Antwort ist von Natur aus klein.

### Fehlerfälle

| `type` | Status | Wann |
|---|---|---|
| `nicht-gefunden` | 404 | Die `MessageID` gibt es nicht **oder** sie gehört einem fremden Mandanten |
| `kein-mandant-gewaehlt` | 403 | Kein aktiver Mandant in der Sitzung |

**Es entsteht kein neuer Problemtyp.** `nicht-gefunden` ist der Typ, den
`common/error/RessourceNichtGefundenException` seit Schritt 2 trägt und den ein unbekannter Pfad
ebenfalls bekommt (`GlobalExceptionHandler.mitRueckfallTyp`) — genau damit sich diese Fälle nicht
unterscheiden lassen.

---

## 2. Die dreistufige Namensauflösung

Der Kern dieses Schritts. Umgesetzt in `message/Schrittnamen` — **ohne Datenbankzugriff**, rein aus
übergebenen Daten, und deshalb einzeln prüfbar (`SchrittnamenTest`, kein `@Tag("db")`).

| Stufe | Bedingung | Ergebnis |
|---|---|---|
| **`DIREKT`** | Es gibt eine `SOSAction`-Zeile zu `(ma.SOSID, ma.SOSActionID)` | deren `SOSActionName` |
| **`HERGELEITET`** | Es gibt keine, aber im **selben Ablauf** genau **einen** Schritt mit derselben **ersten Marke** | dessen `SOSActionName` |
| **`ROHWERT`** | sonst | `ma.SOSActionServiceProperties`, unverändert |

Die **erste Marke** ist alles vor dem ersten `|`, oder der ganze Wert, wenn keiner vorkommt.
`NXS_MERGE|KE_OSTROV_734973|WAIT|30M` → `NXS_MERGE`.

### Was die zweite Stufe einbringt — gemessen

Über die **echten** Schritte (Metadaten-Schritt ausgenommen, Kriterium `SOSActionID <> 0` aus S1):

| | Fenster A (dichter Tag) | Fenster B (dichter Monat) |
|---|---|---|
| echte Schritte | 14.103 | 469.745 |
| **`DIREKT`** | 10.078 — **71,46 %** | 366.343 — **77,99 %** |
| **`HERGELEITET`** | 3.985 — **28,26 %** | 99.290 — **21,14 %** |
| **`ROHWERT`** | **40 — 0,28 %** | **4.112 — 0,88 %** |
| **benannt insgesamt** | **99,72 %** | **99,12 %** |

Laufzeit: Fenster A **447 ms**, Fenster B **12.819 ms** (je ein Lauf; abhängige Unterabfrage je
namenloser Zeile, dieselbe Form wie M19). Zugriffspfad wie M18: `range` über
`MessageLastUpdateIDX`, `ref` über `MessageAction.PRIMARY`, `eq_ref` über `SOSAction.PRIMARY`
(`key_len` 148), dazu `ref` über `SOSAction.PRIMARY` (`key_len` 146) für die Unterabfrage.

**Das ist die Zahl, die den Schritt trägt.** Ohne Stufe 2 zeigte gut jeder vierte Schritt einen
technischen Rohwert; mit ihr sind es drei von tausend. Und die verbleibenden 40 beziehungsweise
4.112 sind nicht irgendwelche — es sind die Pseudoschritte `500`, `501` und `502`, für die es im
Ablauf keinen Gegenpart gibt, weil sie keiner sind (M19).

### Warum die Eindeutigkeitsbedingung nicht verhandelbar ist

M19 hat gemessen: 96 bis 99 Prozent der namenlosen Schritte finden im selben Ablauf **genau einen**
Schritt mit derselben Marke, und **null Mal mehrere**. Die Bedingung schneidet also nichts weg, was
heute trägt — und sie hält die Tür zu, sobald ein Ablauf mehrdeutig wird.

Der Anlass steht in derselben Messung: **`FTPSender` löst anderswo auf 25 verschiedene
`SOSActionName` auf.** Wo diese Marke zweimal im selben Ablauf stünde, wäre jede Wahl geraten — und
geraten wird nicht (Regel Q4). Der Preis der Strenge ist gemessen und beträgt null Zeilen.

### Warum die Herkunft in der Antwort steht

Sie ist **keine Warnung an den Nutzer**, sondern Nachweis. Ein späterer Zweifel an einem Namen
kostet damit eine Abfrage statt einer Suche — dasselbe Muster wie die Einheit von `MessageTimeout`,
die an genau einer Stelle benannt steht. Die Oberfläche zeigt sie nicht.

**Der Rohwert kommt immer mit, auch bei `DIREKT`.** Ohne ihn ist im Zweifelsfall nicht prüfbar, ob
ein Name zur Sache passt. Genau diese Prüfung ist der Beweis, der M15 trägt.

### Mehrere Abläufe in einer Nachricht

2,51 Prozent (Fenster A) beziehungsweise 2,22 Prozent (Fenster B) der Nachrichten haben Schritte aus
**mehr als einem** Ablauf, bis zu drei (M20). Geladen werden deshalb die `SOSAction`-Zeilen **aller**
vorkommenden `ma.SOSID`, und beide Stufen schlagen ausschließlich innerhalb des Ablaufs nach, den
die jeweilige Aktion selbst nennt.

### Groß- und Kleinschreibung

Die Marken werden vor dem Vergleich mit `Locale.ROOT` hochgestellt. Die Sortierung des Quellschemas
ist `utf8mb4_general_ci`, die Gleichheit in SQL also unabhängig von der Schreibweise — `Map.get` ist
es nicht. Ohne diese Angleichung fände Java weniger, als die Messung gemessen hat. Dieselbe Falle
und dieselbe Lösung wie in `MessageStatusClassifier.einordnung` (dort erledigt am 06.08.2026).

### Der Join, über den nicht diskutiert wird

```sql
JOIN SOSAction sa ON sa.SOSID = ma.SOSID AND sa.SOSActionID = ma.SOSActionID
```

**Niemals über `Message.SOSID`** (M15). Die Auflösungsquote der drei geprüften Fassungen ist fast
gleich und belegt deshalb nichts; erst der Vergleich des *ausgeführten* mit dem *geplanten* Baustein
unterscheidet den richtigen Join vom zufällig treffenden — null Abweichungen über `MessageAction`
gegen 3,83 Prozent über `Message`.

---

## 3. Der offene Zustand

Ein Feld, das den Zustand benennt, statt ihn die Oberfläche erraten zu lassen.

| Wert | Bedingung |
|---|---|
| `LAEUFT_AUF` | Nachricht offen, und eine Aktion hat kein `MessageActionEnd` |
| `WARTET_VOR` | Nachricht offen, aber **jede** Aktion ist beendet |
| `OHNE_SCHRITT` | Nachricht offen, aber es gibt gar keine Aktion |
| `KEINER` | Nachricht nicht offen |

**Die Reihenfolge der Prüfung ist Teil der Regel.** „Gar keine Aktion" muss **vor** „jede Aktion ist
beendet" stehen: Über einer leeren Menge ist die zweite Bedingung wahr, und die Nachricht bekäme
`WARTET_VOR` samt einem nächsten Schritt, vor dem sie gar nicht steht.

**„Offen" heißt `WARTEND` oder `LAEUFT`** aus dem `MessageStatusClassifier` — ausdrücklich
aufgezählt und **nicht** über `istEndstatus` geholt. [`message-status.md`](message-status.md) führt
dazu eine eigene Warnung: Jene Methode gehört der Überfälligkeitsrechnung, und für `UNGEKLAERT`
antwortet sie `true`; in ihrem Zusammenhang die vorsichtige Antwort, hier wäre dieselbe `true` die
unvorsichtige. Dieselbe Entscheidung trifft die Liste in `NachrichtenService.aktuellerSchritt`, aus
demselben Grund.

**Nicht über `MessageStatus = 'RUNNING'` definiert** — den Wert gibt es in der Testkopie null Mal.
**Nicht auf `ERROR_TIMEOUT` gestützt** — M8 hat gezeigt, dass dieser Status nicht das Ablaufen von
`MessageTimeout` ist, sondern eine kürzere Frist auf Dienstebene.

### `WARTET_VOR` ist der Normalfall des Wartens

Bei **allen 538** `SUSPENDED`-Nachrichten der Testkopie ist **jede** Aktion beendet (M16 3). Eine
wartende Nachricht steht also **zwischen** zwei Schritten und nicht auf einem laufenden; beim Warten
auf eine Zusammenführung ist genau das der Normalfall.

Nur in diesem Zustand kommt `naechsterSchritt` mit — aufgelöst aus `Message.SOSID` und
`Message.SOSActionID`. **Nullbar**, weil dieser Verweis über den Gesamtbestand zu 43,9 Prozent ins
Leere läuft (M13); bei allen 538 wartenden Nachrichten löst er auf.

> ⚠️ **Belegt ist das für `SUSPENDED`, nicht für `LAEUFT`.** `RUNNING` kommt in der Testkopie null
> Mal vor, in der Produktion aber sehr wohl — und gerade dort wäre der laufende Schritt der zu
> erwartende Fall. Beide Zustände sind gebaut, prüfbar ist lokal nur einer.

> ⚠️ **`LAEUFT_AUF` ist lokal kaum vorführbar.** Im gesamten Bestand tragen 95 von 10,3 Millionen
> Aktionen ein offenes Ende, und die 49 davon, die zu einer *offenen* Nachricht gehören, liegen
> sämtlich in einer Spanne von 62 Sekunden an einem einzigen Tag (M22) — ein Massenereignis, kein
> Vorbild für den Normalbetrieb.

### `laeuftAuf` ist nicht dasselbe wie „kein Ende"

Der Schritt trägt ein eigenes Kennzeichen, das **nur** bei `LAEUFT_AUF` gesetzt wird. Ein fehlendes
Ende allein genügt nicht: 39 der 95 offenen Aktionen des Gesamtbestands gehören zu `FINISHED`,
sieben zu `CHECKED` (M22). Dort ist es eine Protokolllücke und kein Hänger, und ein Feld, das beides
gleich benennt, wäre eine falsche Auskunft. Wer das rohe Merkmal braucht, liest `ende` — das ist
dort `null`.

---

## 4. Die Schrittfolge

Je Schritt: Position (`MessageActionID`) · Name und `namensherkunft` · Rohwert · Start · Ende
(nullbar) · Dauer in Sekunden (nullbar) · `timeoutSekunden` (`SOSActionTimeout`) · `laeuftAuf`.

**Sortiert nach `MessageActionStart`, bei Gleichstand nach `MessageActionID`.** MariaDB stellt
`NULL` dabei nach vorn; einen Start ohne Wert gibt es im Gesamtbestand nicht (M22: 0 von
10.308.590), die Spalte lässt ihn aber zu.

### Der Metadaten-Schritt erscheint nicht

**Aktionen mit `SOSActionID = 0` sind kein Prozessschritt** (S1) und stehen deshalb nicht in der
Schrittfolge. Das Kriterium ist `SOSActionID` und nicht `MessageActionID = 0` oder „trägt keine
Bausteine": S1 hat alle drei gegeneinander gemessen und über 704.427 Aktionen beider Fenster
**dieselbe** Menge gefunden, null Abweichungen in allen drei Paarvergleichen. Gewählt ist die
Spalte, über die gejoint wird — Ausschluss und Wirkung stehen damit in derselben Spalte.

**Seine Eigenschaften gehen dabei nicht verloren.** An ihm hängen 58,8 Prozent aller
`MessageProperty`-Zeilen und ausnahmslos die ganze `Message.*`-Familie (M17 3); gelesen werden sie
über die `MessageID` und nicht über die Aktion.

Er wird trotzdem **mitgeladen**, weil er zwei Dinge beiträgt, die sonst fehlten: den fachlichen
Start und die Antwort auf „gibt es überhaupt eine Aktion" für den offenen Zustand.

### Keine Obergrenze — als Entscheidung festgehalten

**Die Schrittfolge wird nicht gedeckelt.** M16 (2) hat im dichten Tag höchstens **sieben** Aktionen
je Nachricht gemessen, davon eine der Metadaten-Schritt — also höchstens sechs echte Schritte; 98,9
Prozent der Nachrichten haben fünf oder weniger Aktionen. Eine Deckelung schützte vor nichts und
kostete die Vollständigkeit, die eine Zeitleiste erst brauchbar macht.

Die Aussage ruht ausdrücklich darauf, dass eine Detailansicht **nur eine einzige Nachricht** lädt:
Selbst ein unerwarteter Ausreißer von hundert Schritten wäre dort kein Leistungsproblem, sondern nur
eine lange Liste. **Diese Entscheidung soll beim ersten Zweifel nicht neu erfunden werden** — sie
steht hier, damit sie nachlesbar ist.

### Negative Dauern werden `null`

Liegt das Ende vor dem Start, wird `dauerSekunden` als `null` geliefert und nicht als negative Zahl.
**M16 (1) hat ausdrücklich darauf geprüft und `0` gemessen** — auf keiner der 20.352 Aktionen des
dichten Tages steht Start und Ende in der falschen Reihenfolge, und die Zeitleiste bräuchte dafür
keine Sonderregel. Die Regel ist trotzdem da: Sie kostet eine Zeile, und eine Zeitleiste, die „minus
drei Sekunden" anzeigt, ist schlechter als eine, die an dieser Stelle nichts sagt.

### Der Timeout je Schritt wird geliefert, nicht gedeutet

`timeoutSekunden` ist `SOSActionTimeout` — **Sekunden**, dieselbe Einheit wie
`Message.MessageTimeout` (Regel Z2). Im Tagesfenster trägt jeder echte Schritt `1800` (M16 4).

**Ob ein Schritt über seiner Frist gekennzeichnet wird, entscheidet die Oberfläche.** Das Backend
liefert Dauer und Frist und deutet sie nicht. Der Anlass dazu wäre gegeben — 124 von 14.063
beendeten Schritten überschreiten ihre Frist, und zwar um mehr als das Achtundvierzigfache, ein
Dazwischen gibt es nicht —, aber ob das dieselbe Kennzeichnung ist wie die Problemkategorie
„Überfällig" aus Regel Q3 oder eine andere Ebene, ist offen (Frage 7 in `messungen-schritt5.md`).

---

## 5. Die kuratierten Eigenschaften

Wenige, ausgewählte technische Werte im Kopf. **Die Auswahl steht als Konstante im Paket `message`**
(`KuratierteEigenschaften`), nicht in der Datenbank: Es sind wenige Namen, sie sind nicht
mandantenabhängig, und sie ändern sich nicht. Vorbild ist der `MessageStatusClassifier`, nicht
`bam_spalte` — dort ging es um eine je Mandant *verschiedene* Konfiguration, hier nicht.

**Geliefert werden Rohname, Wert und Rang.** Keine deutschen Beschriftungen im Backend; die kommen
in Teil 2 aus der Sprachdatei. Dieselbe Aufteilung, die die API schon bei Status und Fehlertypen
hält.

**Leere Werte werden nicht geliefert** — kein Feld mit leerer Zeichenkette, kein `null` als
Platzhalter. Die Oberfläche soll gar nicht erst in die Lage kommen, eine leere Zeile zu zeichnen.

### Die Auswahl, mit Befüllungsquote je Name

Gemessen am 07.08.2026 gegen die Testkopie, **je Nachricht** (nicht je Zeile) und **je Mandant**.

| Name | Fenster A gesamt | Fenster B gesamt | bester Mandant | drin? |
|---|---|---|---|---|
| **`Message.SendingPartner`** | 11,01 % | 17,00 % | **IBISGUS 98,8 / 98,7 %**, IBIS 75,1 / 75,5 % | **ja** |
| **`Message.SplitCount`** | 16,85 % | 22,24 % | **SUTTONS 98,1 / 93,9 %** | **ja** |
| `Message.InterchangeNumber` | 0 % | 0,37 % | höchster Mandantenwert 0,41 % | nein |
| `Message.CommitInterchangeNumber` | 0,03 % | 0,28 % | höchster Mandantenwert 0,40 % | nein |
| `Message.SNDPRN` | 6,13 % | 13,07 % | nur NEXANS, dort 15,5 % | nein |
| `Message.ReceivingPartner` | 8,24 % | 22,80 % | NEXANS 26,5 %, IBIS 23,5 % | nein |
| `Message.VFN` | 6,35 % | — | — | nein |
| `Message.SourceMessageID` | 65,55 % | — | — | nein, ausgeschlossen |
| `Message.GUID`, `.SOS`, `.Payload.GUID` | je 100 % | — | — | nein, ausgeschlossen |
| `Message.MessageActionID`, `.SOSActionID` | je 100 % | — | — | nein, ausgeschlossen |
| `Message.SOSActionServiceProperties` | 99,98 % | — | — | nein, ausgeschlossen |

Laufzeiten: Namensliste über Fenster A **441 ms**, Aufschlüsselung je Mandant Fenster A **854 ms**,
dieselbe über Fenster B **34.620 ms** (je ein Lauf). Der letzte Wert liegt über der
`max_statement_time` des Lese-Pools und ist deshalb ausdrücklich **kein Muster für Anwendungscode**;
er ist eine einmalige Erhebung über 4,9 Millionen `MessageProperty`-Zeilen.

### Warum je Mandant gemessen wurde — und was das ändert

**Über den Gesamtbestand fällt jeder der drei vorgesehenen Kandidaten durch.** Die höchste Quote
liegt bei 16,85 Prozent. Das ist genau die Lage, an der die BAM-Spalte in Schritt 4 gescheitert ist:
ein kuratiertes Feld, das fast immer leer ist.

**Je Mandant sieht es anders aus, und der Unterschied ist kein Rauschen.** `Message.SplitCount` steht
auf 93,9 bis 98,1 Prozent der `SUTTONS`-Nachrichten und bei allen übrigen Mandanten außer `NEXANS`
auf **null**; `Message.SendingPartner` auf 98,7 Prozent bei `IBISGUS` und 75,5 Prozent bei `IBIS`,
bei `SUTTONS` und `ZAST` auf null. Die Gesamtquote verdeckt das, weil `NEXANS` praktisch das ganze
Aufkommen trägt.

Ausschlaggebend für die Aufnahme ist der Unterschied zur Liste: **Das Detail liefert leere Werte gar
nicht erst.** Eine Listenspalte existiert je Zeile und behauptet auch dann, es gäbe dort etwas zu
sehen, wenn nichts da ist. Ein Feld, das nur erscheint, wenn es einen Wert hat, tut das nicht — und
für den Mandanten, dessen Belege eine Aufteilungszahl tragen, erscheint es auf 94 von 100 Detailseiten.

**Die Austauschnummer fällt auf jeder Lesart heraus.** Ihr höchster gemessener Wert liegt bei 0,41
Prozent, für keinen einzigen Mandanten höher. Sie wäre genau die Spalte gewesen, die auf 98,93
Prozent der Zeilen leer stand.

**`Message.VFN` bleibt draußen, unabhängig von der Quote.** Was die Abkürzung bedeutet, ist *nicht*
gemessen; die naheliegende Auflösung ist eine Vermutung aus dem Präfix eines Nachbarnamens (M17 4,
offene Frage 10). Regel Q4 — nicht geraten.

### Was ausdrücklich ausgeschlossen ist

Interne Kennungen, obwohl sie auf **jeder** Nachricht stehen: `Message.GUID`, `Message.SOS`,
`Message.Payload.GUID` und `Message.SourceMessageID`. Sie sind nach dem Leitsatz Beiwerk, keine
Hauptinformation. `Message.Payload.GUID` wird in **Schritt 8 ein Knopf** und kein Anzeigewert,
`Message.SourceMessageID` in **Schritt 6 eine Verkettung**.

Ebenso draußen: `Message.MessageActionID`, `Message.SOSActionID` und
`Message.SOSActionServiceProperties`. Sie stehen ebenfalls auf jeder Nachricht, wiederholen aber
nur, was die Schrittfolge ohnehin zeigt.

### Ein Name kann mehrfach vorkommen

Der Primärschlüssel `(MessageID, MessagePropertyName, MessageActionID)` erlaubt denselben Namen auf
mehreren Schritten, und `Converter.Payload.GUID` nutzt das (7.862 Zeilen auf 6.149 Nachrichten,
M17 3). **Eine Anzeige, die je Nachricht einen Wert je Namen erwartet, wäre damit falsch** — und das
ist gemessen, nicht vermutet. Für die beiden kuratierten Namen ist im Tagesfenster je genau ein
Schritt gemessen; die Abfrage liefert trotzdem alle Vorkommen, und die Antwort zeigt sie.

---

## 6. Die Kappung der Eigenschaftswerte

**Jeder Wert wird hart gekappt.** Die Grenze steht als benannte Konstante an genau einer Stelle:
`NachrichtendetailRepository.WERT_GRENZE_BYTES` = **16.384 Bytes (16 KiB)**.

| | |
|---|---|
| größter gemessener Wert, dichter Tag | 2.124 Zeichen |
| größter gemessener Wert, dichter Monat | **12.732 Zeichen** |
| alle Eigenschaften einer Nachricht zusammen | **595 Byte** |
| was der Typ `mediumtext` zulässt | **16 MB je Zelle** |

**Die gemessene Länge ist die Bemessungsgrundlage, nicht die Rechtfertigung, es zu lassen.** Die
Grenze liegt über allem Gemessenen — der längste bekannte Wert passt vollständig hinein — und drei
Größenordnungen unter dem, was der Typ erlaubt. Die Produktion ist nicht die Testkopie, und ein
einzelner Wert soll die Antwort nicht sprengen können.

**Gekappt wird zweistufig, und das ist kein Umweg.** In der Abfrage steht
`left(MessagePropertyValue, 16384)` — eine *Zeichen*-Grenze und damit eine sichere Überholung, weil
ein UTF-8-Zeichen nie weniger als ein Byte belegt: Was in die Byte-Grenze passt, ist garantiert noch
enthalten, und die Übertragung ist trotzdem gedeckelt. Ein Wert erst vollständig zu holen und dann
im Backend zu kürzen hätte die Leitung bereits belastet. Die genaue Kappung auf Bytes geschieht
danach im Backend, **auf einer Zeichengrenze**: Ein reiner Byte-Schnitt könnte mitten in eine
UTF-8-Folge fallen und ein Ersatzzeichen erzeugen, das im Wert nie stand.

**Ein gekappter Wert trägt ein Kennzeichen und seine ursprüngliche Länge** (`gekappt: true`,
`originalLaengeBytes`). Die Länge kommt als `octet_length()` aus der Datenbank und nicht aus dem
gelesenen Wert — der ist bereits begrenzt und wüsste seine eigene ursprüngliche Größe nicht. Ohne
das Kennzeichen wäre ein abgeschnittener Wert von einem kurzen nicht zu unterscheiden, und ein
Nutzer läse eine halbe Belegnummer als ganze.

---

## 7. Mandantentrennung

**Der Filter ist Bestandteil jedes Statements** (Regel M3), als `EXISTS` über `ProjectMandant` in
genau der Form, die die Nachrichtenliste verwendet:

```sql
AND EXISTS (SELECT 1 FROM Process p
            JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
            WHERE p.ProcessID = m.ProcessID AND pm.MandantID = :mandant)
```

Nicht als nachgelagerte Prüfung und nicht über die View `MessageMandantID` — deren Zugriffspfad ist
mit den Rechten dieser Anwendung strukturell nicht einsehbar (Fehler 1142 und 1345) und kann Regel
L7 damit nicht erfüllen. Begründung vollständig in [`nachrichtenliste.md`](nachrichtenliste.md) §3.

**Als `EXISTS` und nicht als Join**, weil `ProjectMandant` im Schema n:m ist. Beim Detail wäre die
Folge eines vervielfachenden Joins sogar sichtbarer als bei der Liste: Jeder Schritt erschiene
doppelt.

**Jedes Statement steigt über `Message` ein**, auch das, das am Ende Zeilen aus `SOSAction` liefert.
Es gibt keinen Weg in die Tabellen hinein, der nicht durch die Nachricht und damit durch den Filter
führt.

### Zwei Isolationstests, einer je Endpunkt

`NachrichtendetailIsolationDbIT`, Paarung **`VOTG` gegen `SUTTONS`** — zwei Mandanten aus
verschiedenen Häusern, wie in der Vorlage `MandantenIsolationDbIT`. `NXHBE` und `IBISGUS` scheiden
aus; zwei Mandanten desselben Konzerns sind ein schlechter Beweis für eine Trennung, die zwischen
Firmen greifen soll.

Geprüft wird für **beide** Endpunkte:

1. Beide Mandanten haben Daten im Fenster und erreichen ihr **eigenes** Detail.
2. Eine **fremde** `MessageID` liefert `404`, niemals `403`.
3. **Die Gegenprobe:** Eine fremde, *existierende* Kennung und eine *erfundene* liefern eine
   **ununterscheidbare** Antwort. Ein `404` allein genügt nicht.
4. Kein Anzeigename und keine Prozesskennung des fremden Mandanten steht im Rumpf.
5. Die Trennung gilt in **beide** Richtungen.

**Das Zeitfenster ist absolut** (29.12.2025): Außer `NEXANS` endet jeder Mandant am 30.12.2025 (M3);
in einem relativen Fenster sähe `SUTTONS` je nach Datenstand null Zeilen, und der Test bewiese nur,
dass leer leer ist.

### Die eine Stelle, an der sich die beiden `404` unterscheiden — und warum das keine ist

Die Antwortrümpfe sind **bis auf `traceId` und `instance`** identisch. `instance` ist nach RFC 9457
der angefragte Pfad, und weil die `MessageID` bei diesen Endpunkten **im Pfad** steht, enthält
`instance` sie zwangsläufig.

**Das ist keine Auskunft über den Bestand, sondern das Zitat der Frage.** Nichts an der Antwort
hängt davon ab, ob es die Nachricht gibt: Der Mandantenfilter steht im Statement, es kommt in beiden
Fällen dieselbe leere Menge zurück, und derselbe feste Text geht hinaus. Der Test normalisiert
`instance` deshalb wie `traceId` — **und prüft zusätzlich Zeichen für Zeichen, dass `instance` genau
dem gesendeten Pfad entspricht.** Damit bleibt die Spiegelung eine Spiegelung und wird nicht
unbemerkt zu einer Nachschlage-Auskunft.

Der Unterschied zur Vorlage in [`mandantentrennung.md`](mandantentrennung.md) §3 ist also kein
Abweichen von ihr, sondern ihre Übertragung: Dort steht die Kennung im **Rumpf** einer `POST`, hier
im **Pfad** einer `GET`.

### Der Mandantenfilter wird auch ohne Datenbank geprüft

`NachrichtendetailStatementsTest` rendert jedes der fünf Statements gegen eine jOOQ-Attrappe und
prüft am Text, dass `exists` und `ProjectMandant` darin stehen — dazu, dass keines über
`MessagePropertyValue` filtert, gruppiert oder sortiert (Regel L4).

**Er ersetzt den Isolationstest nicht**, Text ist kein Verhalten. Er schließt eine andere Lücke: Der
Isolationstest braucht die Testkopie und ist in der CI ausgeschlossen; ein vergessenes `EXISTS`
fällt hier auf, ohne dass jemand Datenbankzugang hat.

---

## 8. Datenquellen, `EXPLAIN` und Laufzeit (Regel L7)

Gemessen am **07.08.2026** gegen die **Testkopie**, `SELECT @@global.read_only` → **`1`** als erste
Abfrage der Sitzung. Serverzeit zu Beginn `2026-08-07 14:29:55` (UTC `12:29:55`), MariaDB
`10.6.22-MariaDB`, Lesebenutzer `monitor_read@%`. Laufzeit serverseitig über `SET profiling = 1` /
`SHOW PROFILES`, **beste von fünf nach einem Aufwärmlauf**. Gemessen wurden die **gerenderten**
Statements, also der Text, den jOOQ tatsächlich schickt.

### Die drei Bezugsnachrichten

Nach ihrer **Gestalt** gewählt, aus dem dichten Tag, Mandant `NEXANS`. Die `MessageID` steht
bewusst nicht hier — beschrieben wird die Gestalt, nicht der Datensatz.

| | Gestalt |
|---|---|
| **(a)** | **viele Schritte** — 6 Aktionen, davon 2 ohne `SOSAction`-Zeile (Stufe 2 trägt sie) |
| **(b)** | **wartend** — `SUSPENDED`, 3 Aktionen, alle beendet |
| **(c)** | **viele Eigenschaften** — 38 `MessageProperty`-Zeilen (Maximum des Tages) |

### Laufzeiten

| # | Statement | (a) | (b) | (c) |
|---|---|---|---|---|
| S1 | `findeKopf` | **0,903 ms** | 0,797 ms | 0,813 ms |
| S2 | `existiert` | **0,464 ms** | 0,443 ms | 0,441 ms |
| S3 | `findeAktionen` | **0,702 ms** | 0,644 ms | 0,666 ms |
| S4 | `findeAblaufschritte` | **1,699 ms** | 1,401 ms | 1,545 ms |
| S5 | `findeKuratierteEigenschaften` | **0,787 ms** | 0,763 ms | 0,720 ms |
| S6 | `findeEigenschaften` | **0,718 ms** | 0,670 ms | 0,690 ms |
| | **Detail-Aufruf** (S1+S3+S4+S5) | **4,1 ms** | **3,6 ms** | **3,7 ms** |
| | **Eigenschaften-Aufruf** (S2+S6) | **1,2 ms** | **1,1 ms** | **1,1 ms** |

Aufwärmläufe (a): 21,265 · 0,550 · 0,900 · 2,596 · 7,250 · 0,808 ms. Der erste Zugriff auf `Message`
und der erste auf `MessageProperty` sind darin deutlich teurer als alle folgenden — genau dafür
gibt es den Aufwärmlauf.

**Die Gestalt der Nachricht ändert am Preis fast nichts.** Zwischen der Nachricht mit sechs
Schritten und der mit 38 Eigenschaften liegen über alle sechs Statements weniger als 0,4
Millisekunden. Das passt zum Zugriffspfad: Jedes Statement hängt als `ref` am Präfix eines
Primärschlüssels, und die Menge dahinter ist von Natur aus klein.

### Zugriffspfade

**S1 `findeKopf`** — jede Tabelle `const`:

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|
| `Message` | `const` | `PRIMARY` | 146 | 1 | |
| `Process`, `Project`, `SOS` | `const` | `PRIMARY` | 146 | 1 | |
| `SOSAction` | `const` | `PRIMARY` | **148** | 1 | |
| `mandanten_process` | `const` | `PRIMARY` | 146 | 1 | |
| `ProjectMandant` | `const` | `PRIMARY` | 292 | 1 | `Using index` |
| `MessageProperty` (Unterabfrage) | `ref` | `PRIMARY` | 146 | 31 | **`Using index`** |

Die `key_len 148` auf `SOSAction` sind der Beleg, dass über den **ganzen** zusammengesetzten
Primärschlüssel gejoint wird: 146 Bytes `varchar(36)` plus 2 Bytes `smallint`. Und das `Using index`
auf der Unterabfrage ist der Grund, warum die Anzahl der Eigenschaften im Kopf nichts kostet: Sie
zählt Indexeinträge und **liest keinen einzigen Wert**.

**S2 `existiert`** — `No tables used` außen, innen dreimal `const`. Der billigste Zugriff des
Schritts.

**S3 `findeAktionen`** — `Message` und die Mandantenkette `const`, dann:

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|
| `MessageAction` | `ref` | `PRIMARY` | 146 | 6 | `Using where; Using filesort` |

Das `filesort` ist die Sortierung nach `MessageActionStart`, die nicht der Schlüsselreihenfolge
entspricht. Über höchstens sieben Zeilen ist das kein Posten — es steht hier, weil es im `EXPLAIN`
steht und nicht verschwiegen gehört.

**S4 `findeAblaufschritte`** — das teuerste der sechs, und immer noch unter zwei Millisekunden:

| table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|
| `Message` | `const` | `PRIMARY` | 146 | const | 1 | `Using temporary` |
| `MessageAction` | `ref` | `PRIMARY` | 146 | const | 6 | `Using where` |
| `SOSAction` | `ref` | `PRIMARY` | 146 | `MessageAction.SOSID` | 1 | |

`key_len 146` auf `SOSAction` ist hier **Absicht und nicht 148**: Gejoint wird nur über `SOSID`, also
über die erste Hälfte des Schlüssels, weil die Namensauflösung die **ganze** Ablaufdefinition
braucht und nicht den einen passenden Schritt. Das `Using temporary` ist das `DISTINCT`.

**S5 `findeKuratierteEigenschaften`** — der Beleg, dass die Namensbedingung **kein** Verstoß gegen
L4 ist:

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|
| `MessageProperty` | **`range`** | `PRIMARY` | **548** | 2 | `Using where` |

Die 548 sind `MessageID` (146) plus `MessagePropertyName` (402) — Kennung **plus Name** ist genau
der Bereich, den der Primärschlüssel abbildet. Verboten ist das Filtern über den **Wert**, dessen
Indizes Präfix-Indizes über 50 Zeichen sind (M14, gleich zweimal).

**S6 `findeEigenschaften`**:

| table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|
| `MessageProperty` | `ref` | `PRIMARY` | 146 | 31 | `Using where` |

Ausschließlich über die `MessageID` (Regel L4).

### Warum vier Statements für einen Detail-Aufruf und nicht eines

Die Namensauflösung braucht nicht den *einen* passenden Ablaufschritt, sondern **alle** Schritte
jedes berührten Ablaufs — sonst ließe sich Stufe 2 nicht auf Eindeutigkeit prüfen. In einem
Statement zusammengelegt wäre das ein Kreuzprodukt aus ausgeführten Aktionen und geplanten
Schritten; getrennt ist jedes Statement einzeln erklärbar, einzeln messbar und einzeln
mandantensicher. Der Preis sind gemessene **3,6 bis 4,1 Millisekunden** für den ganzen Aufruf.

---

## 9. Aufbau im Code

```
message/
├─ NachrichtendetailController.java   REST, nimmt nie eine Mandanten-ID
├─ NachrichtendetailService.java      Zustand, Dauern, Kappung, Zusammenbau
├─ NachrichtendetailRepository.java   die fünf Statements, je mit Mandantenfilter
├─ Schrittnamen.java                  die dreistufige Aufloesung — ohne Datenbank
├─ Schrittname.java / Namensherkunft.java
├─ Ablaufschritt.java                 ein geplanter Schritt   (SOSAction)
├─ MessageAktion.java                 ein ausgefuehrter Schritt (MessageAction)
├─ MessageEigenschaft.java            eine rohe MessageProperty-Zeile
├─ NachrichtKopfZeile.java            der rohe Kopf
├─ OffenerZustand.java
├─ KuratierteEigenschaften.java       die Konstante samt Begruendung je Name
└─ NachrichtendetailResponse.java / SchrittResponse.java /
   EigenschaftResponse.java / KuratierteEigenschaftResponse.java
```

**Nichts davon wandert nach `common`.** Kein zweites Fachpaket braucht es heute; wandert es
vorsorglich, ist `common` in drei Schritten die Sammelstelle. Braucht Schritt 6 die
Schrittnamen-Auflösung, wandert sie dann — mit ihrem Test.

**Ein eigener Controller neben `NachrichtenController`.** Beide bedienen `/api/nachrichten`, aber sie
beantworten verschiedene Fragen und haben nichts gemeinsam außer dem Pfadpräfix. Dass
`/api/nachrichten/merkmale` und `/api/nachrichten/{messageId}` nebeneinander bestehen, ist kein
Zufall, auf den man hofft: Spring löst über alle Controller hinweg auf und bevorzugt das wörtliche
Segment vor der Pfadvariablen. **`NachrichtendetailDbIT.merkmale_bleibt_erreichbar` hält das fest**,
damit es bei einer Umstellung nicht still kippt.

### Tests

| Datei | Was, und ob mit Datenbank |
|---|---|
| `SchrittnamenTest` | **ohne DB** — alle drei Stufen, kein `\|` im Wert, `null` als Wert, mehrdeutige Marke, Marke ohne Treffer, Marke im falschen Ablauf, Schreibweise, zwei Abläufe in einer Nachricht, Zeile ohne Namen |
| `NachrichtendetailServiceTest` | **ohne DB** — die vier offenen Zustände, Metadaten-Schritt ausgenommen, negative Dauer, kuratierte Auswahl, Kappung auf Zeichengrenze, UTC-Umrechnung |
| `NachrichtendetailStatementsTest` | **ohne DB** — Mandantenfilter in jedem Statement, kein Zugriff über `MessagePropertyValue`, Begrenzung des Werts in der Abfrage |
| `NachrichtendetailDbIT` | `@Tag("db")` — echte Schrittfolgen, `WARTET_VOR` samt nächstem Schritt, beide Auflösungsstufen kommen vor, Anzahl im Kopf stimmt mit der Liste überein |
| `NachrichtendetailIsolationDbIT` | `@Tag("db")` — **die zwei Pflicht-Isolationstests** (Regel M4) |

**Keine fest eingetragene `MessageID` in den Datenbanktests.** Die Bezugsnachrichten werden über den
Listen-Endpunkt *gefunden* — nach ihrer Gestalt, nicht nach ihrer Kennung. Eine eingetragene Kennung
wäre beim nächsten Befüllen der Testkopie ein rot gewordener Test, der nichts über den Code aussagt.

> **Ein Fund aus dem Testlauf, der hierhin gehört.** Die Prüfung „beide Auflösungsstufen kommen vor"
> sucht ausdrücklich am **alten** Ende des Fensters (`sortierung=aelteste`). Die namenlosen Schritte
> hängen an einem einzigen Ablauf (M20), und der läuft im Fenster 23. bis 30.12.2025 nur bis zum
> **29.12. um 23:03**. Die *neuesten* fünfzig Nachrichten liegen danach und tragen nachweislich
> keinen einzigen namenlosen Schritt — gegen sie geprüft bewiese der Test nur, dass Stufe 2 nichts
> zu tun hatte. Am alten Ende tragen 49 von 50 einen.

---

## 10. Die Oberfläche (Teil 2)

Entsteht in Schritt 5, Teil 2. Route `/nachrichten` und `/nachrichten/<id>`, Feature
`features/nachrichten` — es importiert **nicht** aus `features/sitzung`.

**Kein Feld wurde am Backend ergänzt.** Was hier steht, ist ausschließlich Darstellung dessen, was
§1 bis §9 liefern.

```
features/nachrichten/
├─ api.ts                          + Detailtypen und die zwei Aufrufe
├─ detail.ts                       reine Funktionen: Balkennormierung, Lückenschwelle,
│                                    Zeilen der Zeitleiste — ohne React
├─ filter.ts                       + der Parameter `nachricht`
├─ hooks.ts                        + useNachrichtendetail, useEigenschaften
└─ components/
   ├─ nachricht-detail.tsx         die eine Komponente beider Einhängepunkte: Kopf,
   │                                 Ladung, Fehler, Kennung
   ├─ nachricht-seite.tsx          der Rahmen der eigenen Route
   ├─ zeitleiste.tsx               Schritt-, Lücken- und Erwartungszeile
   ├─ eigenschaften-block.tsx      eingeklappt, lädt erst beim Aufklappen
   └─ nachrichten-tabelle.tsx      der Zeilenklick bekommt seine Funktion

app/(app)/nachrichten/[messageId]/page.tsx   Server-Komponente, reicht die Kennung durch
lib/format.ts                                + formatiereDauer
```

### 10.1 Zwei Einhängepunkte, eine Komponente

| Weg | Verhalten |
|---|---|
| `/nachrichten?nachricht=<id>` | Panel neben der Liste, die Liste bleibt im Blick |
| `/nachrichten/<id>` | dieselbe Komponente als eigene Seite |

**Keine abfangenden Routen** (die `(.)`-Konvention des App Routers). Sie sind der komplexeste Teil
des Routings für einen Gewinn, den wir nicht brauchen: Zwei schlichte Einhängepunkte und eine
Komponente leisten dasselbe und sind zu lesen, ohne die Konvention zu kennen. Der Preis wäre nicht
nur Verständlichkeit — abfangende Routen verhalten sich bei Neuladen, Zurück und geteilten Links
unterschiedlich, und genau diese drei Fälle sind hier die Abnahmekriterien.

**Die eigene Route existiert nicht aus Symmetrie.** Ein Link auf einen Beleg ist die eigentliche
Anwendung — „schick mir mal den Link" —, und die BAM-Suche in Schritt 7 braucht einen Einstieg ohne
Liste.

**Das Panel hängt nicht am Ergebnis der Liste.** Es lädt über seine eigene Kennung
(`NACHRICHTEN_SCHLUESSEL.detail`). Ein tiefer Link auf eine Nachricht außerhalb des aktuellen
Zeitfensters zeigt die Nachricht, auch wenn die Liste dahinter leer ist. Das ist gewollt und darf
nicht „repariert" werden, indem das Panel auf die Listendaten zugreift — der Empfänger eines Links
hat das Zeitfenster des Absenders nicht.

### 10.2 Der Parameter in der URL

`nachricht` gehört zum Filterzustand wie jeder andere Wert (nuqs, `NACHRICHTEN_PARAMETER`). Drei
Feinheiten, alle drei bewusst:

**Kein `withDefault`, und deshalb keine `clearOnDefault`-Falle.** `nuqs` entfernt einen Parameter
aus der URL, sobald er dem *Standardwert* gleicht — geprüft wird das aber nur, wenn überhaupt einer
gesetzt ist (`parser.defaultValue !== undefined`). Ohne Standardwert kann keine Kennung
versehentlich verschwinden; `null` entfernt den Parameter, und genau das ist „Schließen". Käme hier
je ein `withDefault` dazu, gehörte `clearOnDefault: false` in dieselbe Zeile — so wie bei
`zwischenschritte`.

**`history: "push"` statt `replace`.** Der Rest der Filterleiste ersetzt den Verlaufseintrag: Ein
Filter, den man verstellt, ist keine Station, zu der man zurückgeht. Eine geöffnete Nachricht ist
eine. Am schmalen Fenster füllt die Ansicht den Bildschirm, und das Zurück des Browsers ist dort der
Weg heraus — ohne eigenen Verlaufseintrag spränge es an der Liste vorbei.

**`nachricht` steht in keiner Anfrage an `/api/nachrichten`.** Der Endpunkt kennt den Parameter
nicht; träte er in den Abfrageschlüssel des Zwischenspeichers ein, lüde **jeder Klick auf eine Zeile
die ganze Liste neu** und setzte die Seitenposition zurück. `alsAbfrage` lässt ihn deshalb weg,
`alsSuchparameter` nimmt ihn mit — und `tests/nachrichtenfilter.test.ts` hält beides fest,
einschließlich der Probe, dass die Abfrage mit und ohne geöffnetes Panel Zeichen für Zeichen
dieselbe ist.

**Der Zeilenklick der Liste** setzt den Parameter. Die Zeile ist über `Tab` erreichbar, `Enter` und
`Leertaste` öffnen sie, **`Escape` schließt das Panel wieder** (§10.11), und ein Klick auf einen
Verweis *in* der Zeile öffnet das Panel nicht mit —
heute steht dort keiner, und die Bedingung steht trotzdem da, damit Schritt 6 sie nicht erst finden
muss. Die geöffnete Zeile trägt `aria-current` und die blasse Akzenttönung: dieselbe, die den
aktiven Navigationseintrag markiert. Sie sagt etwas über die **Anwendung** — welche Zeile offen ist
—, nicht über die Daten; eine Statusfarbe wäre hier eine Aussage, die die Zeile nicht macht.

### 10.3 Der Kopf

Ablaufname (`sosName`) als Überschrift, darunter die Statusplakette — dieselbe Komponente wie in der
Liste, also **nie allein über Farbe**, immer mit Beschriftung und Zeichen.

**`bedeutungNichtVerifiziert` gibt es in dieser Antwort nicht** (§1). Es ist genau die Einordnung
`UNGEKLAERT` und wird an **einer** Stelle abgeleitet (`detail.ts`), nicht in der Komponente
nachgebaut.

Dann Zeitpunkt und fachlicher Start, formatiert mit derselben Zone und derselben Funktion wie die
Liste ([`frontend-grundlagen.md`](frontend-grundlagen.md) §4): Sekunden in der Zelle, der relative
Abstand im `title`.

**Projekt und Prozess stehen dazu**, obwohl die Aufgabenstellung sie nicht nennt. Der Grund ist die
eigene Route: Ein tiefer Link zeigt die Ansicht **ohne** Liste, und ohne diese beiden Felder ist
nicht zu sehen, wozu die Nachricht gehört. Beide sind nullbar und erscheinen dann als „nicht
zugeordnet" (Regel Q4).

**Die kuratierten Eigenschaften stehen im Kopf**, sortiert nach ihrem `rang`. Die Zuordnung Rohname
→ Beschriftung ist eine Übersetzung und lebt dort, wo die anderen Übersetzungen leben
(`texte.nachrichten.detail.kuratiert`):

| Rohname | deutsch | englisch |
|---|---|---|
| `Message.SendingPartner` | Absender | Sender |
| `Message.SplitCount` | Aufteilungszahl | Split count |

> **Ein kuratiertes Feld ohne Übersetzung erscheint mit seinem Rohnamen**, sichtbar unfertig. Das ist
> besser als es zu verstecken: Ein neuer Name aus dem Altsystem fällt beim ersten Blick auf, statt
> lautlos zu fehlen.

**Der Kopf hält null kuratierte Felder aus.** Die Auswahl ist faktisch mandantenabhängig — für
`ZAST` und `SYSTEM` ist der Block **immer** leer (§13), und leere Werte liefert das Backend nicht.
Die Felder stehen deshalb in derselben Beschreibungsliste wie Zeitpunkt, Start, Projekt und Prozess
und nicht in einem eigenen Kasten: Fehlen sie, fehlt eine Zeile, und es bleibt kein leerer Rahmen
stehen.

**Ein Name kann je Nachricht mehrfach vorkommen** (M17 3, offene Frage 11 aus Teil 1). Die Antwort
liefert alle Vorkommen, und **der Kopf zeigt sie alle** — als mehrere Zeilen mit derselben
Beschriftung. Für die beiden kuratierten Namen ist im Tagesfenster je genau ein Vorkommen gemessen;
träte je ein zweiter auf, wäre die Anzeige zweier Werte richtig und ihre stillschweigende Reduktion
auf einen falsch.

**Die `MessageID` kehrt hier zurück**, nachdem sie in Schritt 4 aus der Liste geflogen ist. Klein,
in fester Laufweite, mit Kopierknopf. Sie ist Beiwerk nach dem Leitsatz — aber sie ist das, was
jemand in eine E-Mail an die EDI-Betreuung schreibt, und ohne Kopierfunktion trägt eine
`varchar(36)`-UUID nichts. Scheitert das Kopieren (kein sicherer Kontext), bleibt der Wert sichtbar
und markierbar stehen; eine Fehlermeldung für einen Knopf, der Beiwerk kopiert, wäre lauter als die
Sache.

### 10.4 Die Zeitleiste

Senkrecht, ein Schritt je Zeile, weil das Panel schmal ist. Je Schritt: der Name, ein schmaler
Balken, die Dauer als Text.

**Die Rechnung steht in `detail.ts`, nicht in der Komponente** — sie ist eine Entscheidung, und
Entscheidungen werden hier geprüft, Markup nicht (`tests/nachrichtendetail.test.ts`).

#### Der Balken ist auf die Nachricht normiert, nicht auf eine absolute Skala

Der längste Schritt **dieser** Nachricht bekommt die volle Breite, alle anderen anteilig davon.

**Der Grund ist die gemessene Gestalt der Daten.** Im Tagesfenster trägt jeder echte Schritt die
Frist `1800` (M16 4), und ein Wartschritt von 30 Minuten neben vier Schritten von
Sekundenbruchteilen ergäbe auf einer absoluten Zeitachse einen vollen Balken und vier unsichtbare
Striche — korrekt und nutzlos. Normiert beantwortet die Leiste die Frage, für die sie da ist:
**welcher Schritt hat die Zeit gefressen.**

**Der Preis ist bewusst in Kauf genommen:** Zwei Nachrichten sind über ihre Balken **nicht**
vergleichbar. Deshalb steht die Dauer immer auch als Text daneben — ein Balken ohne Zahl ist ein
Gefühl.

| Fall | Was passiert |
|---|---|
| längster Schritt | volle Breite |
| sehr kurzer Schritt | `BALKEN_MINDESTANTEIL` = **2 %** — „praktisch nichts", nicht „nicht vorhanden" |
| jeder Schritt unter einer Sekunde (`laengsteDauer === 0`) | jeder Balken 2 %; es gibt nichts zu vergleichen, und keine Division durch null |
| `dauerSekunden === null` | **kein Balken**, keine Dauer, keine erfundene Null |

**Eine Fläche der Breite null wäre falsch**, und zwar nicht kosmetisch: Sie sähe aus wie „praktisch
nichts" — und das ist etwas anderes als „nicht aufgezeichnet".

**Die Dauer als Text** kommt aus `lib/format.ts` `formatiereDauer`, mit höchstens **zwei** Einheiten
(`3 h 12 min`, `1 min 5 s`, `45 s`). Die Einheiten selbst stehen in der Sprachdatei — auch „s" und
„min" sind Text, den ein Nutzer sieht. **`0` wird zu „< 1 s" und nicht zu „0 s":** Das Backend
rechnet in ganzen Sekunden, ein Schritt mit `0` hat zwischen null und einer Sekunde gedauert, und
„0 s" behauptete eine Genauigkeit, die die Zahl nicht hat.

#### Die Lücke zwischen zwei Schritten ist eine eigene Zeile

Schwelle: **`LUECKE_SCHWELLE_SEKUNDEN` = 60**. Darunter ist der Abstand die normale Übergabe
zwischen zwei Diensten und keine Auskunft — eine Zeile je Schrittwechsel machte die Leiste doppelt
so lang und sagte nichts.

Ohne diese Zeile steht die Wartezeit **in keiner Schrittdauer**, und genau sie ist bei einer
hängenden Nachricht oft die ganze Antwort.

Keine Zeile entsteht, wenn der vorige Schritt gar kein Ende trägt (dann gibt es keinen Zwischenraum,
sondern einen offenen Schritt), wenn ein Zeitpunkt unlesbar ist, und bei negativem oder
null-Abstand. Eine negative Dauer liefert das Backend ohnehin als `null` (§4).

#### Der offene Zustand wird gezeigt, nicht errechnet

Das Backend liefert ihn als Feld (§3). Die Oberfläche stellt ihn dar und leitet nichts ab:

| Feld | Darstellung |
|---|---|
| `LAEUFT_AUF` | der betroffene Schritt ist markiert — Kontur in der Rolle `--status-offen`, dazu Zeichen **und** Text „läuft gerade"; keine Dauer, kein Balken |
| `WARTET_VOR` | **nach** dem letzten ausgeführten Schritt eine eigene Zeile mit gestrichelter Kontur — mit dem Namen und „noch nicht begonnen", **wenn** der benannte Schritt nicht schon in der Leiste steht; sonst nur der Satz, dass die Nachricht wartet (§10.11) |
| `OHNE_SCHRITT` | eigener Text statt einer leeren Leiste |
| `KEINER` | nichts Zusätzliches |

Ist bei `WARTET_VOR` der nächste Schritt `null`, wird das benannt und nicht weggelassen: *„Die
Nachricht wartet — worauf, ist in der Ablaufdefinition nicht hinterlegt."* Ihn stillschweigend zu
unterschlagen hieße, eine offene Nachricht wie eine abgeschlossene aussehen zu lassen. Über den
Gesamtbestand läuft dieser Verweis zu 43,9 Prozent ins Leere (M13).

> **„Die Leiste endet dort" heißt nicht, dass etwas abgeschnitten wird.** Bei `LAEUFT_AUF` hängt die
> Zeitleiste nichts an — mehr nicht. Schritte hinter dem laufenden werden **nicht** weggelassen: Der
> laufende ist in den Daten der letzte (er trägt kein Ende, bekommt also weder eine Lückenzeile noch
> eine Fortsetzung), und gemessene Zeilen stillschweigend zu verschweigen wäre etwas anderes als
> eine Leiste, die von selbst dort aufhört.

**`OHNE_SCHRITT` und „abgeschlossen ohne Schritt" sehen in der Zeilenliste gleich aus — leer.** Was
der Nutzer liest, entscheidet die Komponente über den *Zustand* und nicht über die Länge der Liste;
sonst hieße „offen und ohne Schritt" dasselbe wie „fertig und ohne Schritt".

#### Die Herkunft des Namens steht im Tooltip

Dezent, zusammen mit dem Rohwert, im `title` der Namenszelle — kein Symbol, kein Warnzeichen, keine
eigene Spalte. Ein Nutzer, der „Send File by FTP" liest, soll nicht mit der Frage belastet werden,
wie wir darauf gekommen sind; wer nachsehen will, findet es.

| Herkunft | Text im Tooltip |
|---|---|
| `DIREKT` | Name aus der Ablaufdefinition |
| `HERGELEITET` | Name über den Baustein aus dem Ablauf hergeleitet |
| `ROHWERT` | Im Ablauf ist kein Name hinterlegt — angezeigt wird der Baustein |

Bei `ROHWERT` steht ohnehin der Rohwert als Name; auch dort gehört die Herkunft in den Tooltip,
damit die Erklärung an **einer** Stelle liegt.

**Feste Zeilenhöhe** je Schritt (`--dichte-zeile`), nach der Regel aus
[`nachrichtenliste.md`](nachrichtenliste.md) §8.1: Was nicht hineinpasst, wird gekürzt, der Vollwert
steht im `title`. Die gemessene Namenslänge geht bis 61 Zeichen — in einem Panel von 26 rem passt
das nicht immer.

### 10.5 Die technischen Eigenschaften

Eingeklappt, beschriftet mit `eigenschaftenAnzahl` **aus dem Kopf** — also ohne sie zu laden. Genau
dafür trägt der Detail-Endpunkt die Zahl (§1); lüde die Oberfläche zum Beschriften, hätte der zweite
Endpunkt keinen Zweck.

Erst beim Aufklappen wird `GET /api/nachrichten/{id}/eigenschaften` gerufen (`enabled` an der
Abfrage). **Der Ladezustand liegt im Block, nicht im ganzen Panel** — wer die Eigenschaften
aufklappt, will die Zeitleiste nicht verlieren.

Name und Wert als Rohwerte in fester Laufweite, feste Zeilenhöhe, gekürzt mit Vollwert im `title`.
**Ein gekappter Wert trägt ein sichtbares Kennzeichen** samt seiner ursprünglichen Länge in Bytes
(§6) — ein stillschweigend abgeschnittener Wert ist schlimmer als ein sichtbar abgeschnittener.

**Bei `eigenschaftenAnzahl === 0` gibt es keinen Schalter**, sondern eine Zeile Text. Ein
Bedienelement, das einen leeren Bereich öffnet, ist schlimmer als keins — dieselbe Regel wie beim
Zwischenschritte-Chip der Liste.

**Der Zustand gehört nicht in die URL** — er ist keine Ansicht, die jemand teilt. Beim Blättern
zwischen Nachrichten beginnt der Block wieder eingeklappt; umgesetzt über `key={messageId}` und
damit über den Baum, nicht über einen Effekt.

### 10.6 Ladung, Leere, Fehler

**Laden.** Ein Platzhalter in der Gestalt der späteren Ansicht — Überschrift, vier Kopfzeilen, drei
Schrittzeilen —, kein Kreisel über einem leeren Kasten. Die Liste dahinter bleibt bedienbar.

**Fehler bei unbekannter oder fremder Kennung.** Panel und Route zeigen denselben Zustand inline;
**keine eigene Fehlerseite**, das Panel schließt sich nicht. Ein Link, der nichts tut, ist
schlechter als einer, der sagt warum.

> **Der Text ist für beide Fälle identisch.** Das Backend macht „gibt es nicht" und „gehört einem
> anderen Mandanten" absichtlich ununterscheidbar (§7). Eine Oberfläche, die „keine Berechtigung"
> schriebe, gäbe genau das preis, was die 404-Regel schützt.

Gezeigt wird der übersetzte Satz zu `nicht-gefunden` („Das Gesuchte gibt es nicht.") und darunter:

> *„Unter dem Mandanten in der Kopfzeile gibt es diese Nachricht nicht. Stammt der Link von jemand
> anderem, prüfe zuerst den Mandanten dort oben."*

Das ist für beide Fälle wahr und für einen Admin die eigentliche Handlungsanweisung — er sieht immer
nur einen Mandanten gleichzeitig (Projektbeschreibung §2), und ein geteilter Link zeigt erst nach
dem Wechsel etwas. **Es steht kein Wort über Berechtigungen darin**; `tests/sprachdateien.test.ts`
prüft die Wortliste weiterhin am Fehlerkatalog.

Bei jedem anderen Fehler steht der gewöhnliche Baustein aus `components/zustand.tsx` — er übersetzt
über den `type` der `problem+json`-Antwort und zeigt die Fehler-Kennung nur dort, wo sie hilft.

**Retry.** Die Regeln aus `lib/query-client.ts` gelten unverändert: **ein `404` wird nicht
wiederholt** (`istEndgueltig`), und deshalb steht bei ihm auch keine Schaltfläche dafür.

**Schließen** entfernt genau den einen Parameter aus der URL und lässt den übrigen Filterzustand
unberührt. Auf der eigenen Route führt Schließen zurück zur Liste, und zwar **mit der
Abfragezeichenkette, die in der URL steht** — nicht mit einem nebenher gemerkten Zustand: Was die
URL nicht ausdrückt, existiert nicht ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8). Trägt
der geöffnete Link Filter, kommen sie mit; trägt er keine, gilt auf der Liste wieder das
Standardfenster des Servers. Gelesen wird `window.location.search` **im Ereignis** und nicht über
`useSearchParams` — der Hook zwingt die Seite unter eine Suspense-Grenze, gebraucht wird der Wert
aber erst beim Klick.

### 10.7 Nebeneinander, und am schmalen Fenster

Ab `xl` (1280 px) steht das Panel **neben** der Liste (26 rem, ab `2xl` 30 rem), darunter **an ihrer
Stelle**: Am Handy gibt es kein „neben der Liste", dort füllt die Ansicht den Bildschirm, und das
Zurück des Browsers schließt sie, weil der Zustand in der URL steht.

**Beides sitzt im *einen* Scrollbereich des Anwendungsrahmens.** Es entsteht keine zweite
Bildlaufleiste, und nichts bemisst seine Höhe am Fenster — ein Panel, das für sich scrollt, wäre der
erste Verstoß gegen genau die Regeln, die
[`frontend-grundlagen.md`](frontend-grundlagen.md) §7 gemessen hat.

**Die Liste wird ausgeblendet, nicht ausgehängt** (`display: none`). Damit ist sie aus dem Bild und
aus der Tastaturreihenfolge, ihr Zustand bleibt aber stehen: Wer das Panel schließt, findet dieselbe
Seite wieder, ohne dass eine zweite Abfrage auf die Produktionsdatenbank geht. Der Preis ist, dass
ein tiefer Link am Handy die Liste im Hintergrund einmal lädt — eine Abfrage von gemessenen
2,7 ms (L1 bis L3), und der Weg heraus führt ohnehin dorthin.

### 10.8 Was die Oberfläche bewusst nicht zeigt

- **Keine Deutung des Timeouts.** `timeoutSekunden` kommt je Schritt mit und wird **nicht**
  angezeigt. 124 von 14.063 beendeten Schritten überschreiten ihre Frist, und zwar um mehr als das
  Achtundvierzigfache (§4) — ob das dieselbe Kennzeichnung ist wie die Problemkategorie „Überfällig"
  aus Regel Q3 oder eine andere Ebene, ist offen (Frage 7 in `messungen-schritt5.md`). Eine
  Kennzeichnung zu erfinden, bevor die Frage beantwortet ist, hieße die Antwort vorwegzunehmen.
- **Keine Verkettung.** Eine Aufteilungszahl erscheint als Zahl und verlinkt zu nichts — das ist
  Schritt 6.
- **Kein Rohdaten-Download** — das ist Schritt 8.
- **Kein Gerüst geplanter Schritte** — die Entscheidung samt Zahlen steht in §11 (M21).

### 10.9 Tests

| Datei | Was |
|---|---|
| `tests/nachrichtendetail.test.ts` | die Normierung des Balkens (längster voll, Mindestbreite, alles unter einer Sekunde, keine Dauer → kein Balken); die Lückenschwelle in beide Richtungen, negativ, null und nach einem Schritt ohne Ende; die vier offenen Zustände samt `WARTET_VOR` ohne benannten nächsten Schritt; **dass ein bereits gelaufener „nächster" Schritt erkannt wird** (§10.11); dass bei `LAEUFT_AUF` nichts angehängt und nichts weggelassen wird; `bedeutungNichtVerifiziert` als abgeleiteter Wert |
| `tests/nachrichtenfilter.test.ts` | `nachricht` steht in der URL und lässt sich wieder einlesen; **taucht in keiner Abfrage der Liste auf** und lässt deren Abfrageschlüssel unverändert; Schließen lässt den übrigen Filterzustand stehen; eine leere Kennung ist keine Auswahl |
| `tests/format.test.ts` | `formatiereDauer` mit höchstens zwei Einheiten, „< 1 s" statt „0 s", nie eine negative Dauer, Bausteine aus der aktiven Sprache |
| `tests/sprachdateien.test.ts` | unverändert — beide Sprachdateien tragen den neuen Abschnitt vollständig |

Kein gerenderter Baum: Geprüft werden die **Entscheidungen**, nicht das Markup
([`frontend-grundlagen.md`](frontend-grundlagen.md) §9).

### 10.10 Sichtprüfung im Browser (07.08.2026)

Gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Rolle ADMIN, Fenster 1920 × 889.
**Geklickt und getippt, nicht zugewiesen** — der Anlass für diese Regel steht in
[`nachrichtenliste.md`](nachrichtenliste.md) §8.2. Zeitfenster und tiefe Links wurden über die
**Adresszeile** angesteuert; das ist kein Umweg an der Oberfläche vorbei, sondern genau der Weg, den
ein geteilter Link nimmt, und drei der Abnahmekriterien beschreiben ihn.

**Die Stichproben sind nach Gestalt gewählt, nicht nach Aktualität.** Gesucht wurde über den
Detail-Endpunkt über **700 Nachrichten** des dichten Tages und der Woche davor.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | Zeilenklick öffnet, Liste bleibt bedienbar | Panel rechts, Liste links weiter scroll- und filterbar; die geöffnete Zeile trägt die Akzenttönung und `aria-current` |
| 2 | Die URL trägt die Kennung | `…&sortierung=aelteste&nachricht=7d07a0e1-…` |
| 3 | Neuladen stellt die Ansicht her | `F5` — Panel wieder da; drei Anfragen: `merkmale`, das Detail, die Liste |
| 4 | Link in einem neuen Tab | frischer Tab auf dieselbe URL: Panel samt Liste |
| 5 | Route und Panel zeigen dasselbe | Text der beiden Ansichten **Zeichen für Zeichen gleich**, bis auf die eine Beschriftung „Ansicht schließen" gegen „Zurück zur Liste"; die Route hat keine Tabelle |
| 6 | Schließen räumt die URL | nur `nachricht` fällt weg, `von`/`bis`/`zwischenschritte`/`sortierung` bleiben |
| 7 | Schließen auf der Route | führt auf `/nachrichten`; der geöffnete Link trug keine Filter, also gilt dort wieder das Standardfenster des Servers |
| 8 | **Erfundene gegen fremde Kennung** | siehe unten — **identisch** |
| 9 | Eigenschaften erst beim Aufklappen | nach dem Laden **kein** `/eigenschaften` in den Netzanfragen; der Klick auf „Technische Eigenschaften (22)" löst genau eine aus |
| 10 | Tastatur allein | `Tab` erreicht die Zeile (Fokusring sichtbar), `Enter` öffnet, `Escape` schließt — siehe unten |
| 11 | Feste Zeilenhöhe der Zeitleiste | jede Schrittzeile **36 px**, also `--dichte-zeile` |
| 12 | Normierung des Balkens | 70 s / 120 s ergeben `width: 58.33%` und `width: 100%` |
| 13 | Herkunft nur im Tooltip | Zelle „Send Message to Partner"; `title` = Name + `Baustein: FTPSender\|LOOKUP\|NEXANSP12IDOCOUT` + „Name über den Baustein aus dem Ablauf hergeleitet" |
| 14 | Eine Bildlaufleiste, auch mit Panel | Dokument scrollt nicht (`scrollY` bleibt 0, `scrollHeight = clientHeight = 889`), `main` ist der **einzige** senkrechte Scroller und erreicht exakt sein Maximum (1.298); waagerechter Überlauf null |
| 15 | Schmales Fenster | **nicht gesehen — siehe unten** |

**Zu 8, und das ist der wichtigste Punkt.** Geprüft wurde nicht nur eine erfundene Kennung, sondern
eine **echte fremde**: als ADMIN auf `SUTTONS` gewechselt, dort eine `MessageID` geholt, zurück auf
`NEXANS` gewechselt und dieselbe Kennung geöffnet. Der Text ist **Zeichen für Zeichen** derselbe wie
bei `00000000-dead-beef-…`:

> Das Gesuchte gibt es nicht.
> Unter dem Mandanten in der Kopfzeile gibt es diese Nachricht nicht. Stammt der Link von jemand
> anderem, prüfe zuerst den Mandanten dort oben.

Das Panel blieb in beiden Fällen offen, die Liste daneben bedienbar.

**Zu 15.** Die Browsersteuerung kann das Fenster nicht verkleinern: `resize_window` meldet Erfolg,
`innerWidth` bleibt bei 1920 und `outerWidth` meldet `0` — dieselbe Grenze wie in Schritt 4
([`nachrichtenliste.md`](nachrichtenliste.md) §8.4). Nachgesehen wurde deshalb das **Regelwerk**:
Unter `xl` trägt die Listenspalte `hidden … xl:flex` — also die ganze Spalte und nicht bloß eine
Zelle —, und die Panelspalte hat unterhalb von `xl` keine Breitenvorgabe, füllt die Zeile also.
**Gesehen ist das nicht.** Es gehört von Hand nachgeholt, bevor der Schritt als abgenommen gilt.

### 10.11 Drei Befunde aus der Sichtprüfung — und was sie geändert haben

**1. Die Statusplakette der Liste wich dem Zusatz.** Mit der neuen Beschriftung stand in der Zelle
`Warte…` statt `Wartend`: Die Plakette durfte schrumpfen, der Schrittname daneben nicht. Genau
verkehrt herum — der Status ist die Hauptinformation, der Schritt ist Beiwerk nach dem Leitsatz.
Steht ein Schritt daneben, ist die Plakette jetzt `shrink-0`; ohne ihn darf sie weiter weichen,
denn dort trägt sie bei `bedeutungNichtVerifiziert` einen Rohwert beliebiger Länge.

**2. Der „nächste" Schritt einer wartenden Nachricht ist derselbe, der gerade gelaufen ist.**
Nachgesehen an einer `SUSPENDED`-Nachricht: Schritt 2 heißt „Send Message to Pool" und trägt den
Rohwert `NXS_MERGE|BMW|WAITUNTIL|now+170H@…|SUSPEND` — er ist der Schritt, der die Nachricht
*schlafen legt*. Und `naechsterSchritt` aus `Message.SOSActionID` zeigt auf **ihn**.

Die geplante Zeile hätte damit „Send Message to Pool · noch nicht begonnen" unmittelbar unter
„Send Message to Pool · 2 min" geschrieben — für den Nutzer, der laut Leitsatz kein EDI-Spezialist
ist, schlicht ein Widerspruch. Die Zeile wiederholt den Namen deshalb nicht mehr, wenn er schon in
der Leiste steht; sie sagt dann *„Die Nachricht wartet — von selbst geht es hier nicht weiter."*,
und der Verweis bleibt im Tooltip nachlesbar. Steht dort ein **anderer** Schritt, erscheint er
weiterhin als noch nicht begonnen.

> **Der offene Zustand wird dadurch nicht errechnet.** Er kommt weiter aus dem Backend; verglichen
> werden zwei gelieferte Felder, und das Ergebnis entscheidet nur über die **Wortwahl** einer Zeile.
> Verglichen wird über den *Namen*: Zwei gleich benannte Zeilen untereinander sind für den Leser
> dieselbe Zeile, gleich welche Kennung dahintersteht.

**3. Mit der Tastatur ging Öffnen, aber Schließen nur mühsam.** Der Schließen-Knopf steht im DOM
hinter der Tabelle — man hätte durch bis zu fünfzig Zeilen tabben müssen. Das erfüllt „erreichbar"
und verfehlt „bedienbar". **`Escape` schließt das Panel jetzt**, mit zwei Ausnahmen, damit die Taste
nicht zweierlei tut: in einem Eingabefeld und bei einem offenen Auswahlfeld bleibt sie, was sie ist.
Ein Fokussprung ins Panel wäre die Alternative gewesen und ist verworfen — er nähme dem Nutzer die
Stelle in der Liste, an der er gerade war, und einem Mausnutzer, der nichts davon wollte, ebenso.

### 10.12 Was die Testkopie nicht hergibt

Drei Dinge sind gebaut und **nicht gegen echte Daten gesehen**:

| | Warum |
|---|---|
| **Die Lückenzeile** | Über **700 geprüfte Nachrichten** ist die größte Lücke zwischen zwei Schritten **eine Sekunde**. Die Schwelle von 60 Sekunden greift also nirgends. Belegt ist die Zeile ausschließlich durch `tests/nachrichtendetail.test.ts` |
| **`LAEUFT_AUF`** | `RUNNING` kommt in der Testkopie null Mal vor (§13); die Markierung des laufenden Schritts ist nur unit-getestet |
| **`OHNE_SCHRITT`** und **die Kappung** | beides kommt in der Testkopie nicht vor (§3, §6) |

**Das ist kein Versäumnis der Prüfung, sondern die Gestalt des Bestands** — und es steht hier, damit
niemand die grüne Testliste für eine Vorführung hält. Was sich vorführen ließ, ist in §10.10
aufgezählt.

Gesehen wurden dagegen: bis zu **vier Schritte** je Nachricht, Dauern von `< 1 s` bis `1 min 57 s`
nebeneinander (die Normierung trägt genau dort), **36 Eigenschaften**, beide kuratierten Felder mit
ihrer deutschen Beschriftung (`Absender BMW`, `Aufteilungszahl 0`), beide Auflösungsstufen des
Schrittnamens und der Zustand `WARTET_VOR`.

---

## 11. Die bewussten Nicht-Entscheidungen

Was hier **nicht** gebaut wurde, und mit welchen Zahlen.

### Keine Zuordnungstabelle für Bausteine (M19)

Der Plantext zu Schritt 5 beauftragt eine handgepflegte Übersetzung von
`SOSActionServiceProperties` nach Klartext. **Sie entfällt.** Über den ganzen dichten Monat stehen
hinter 103.402 namenlosen Schritten **vier** verschiedene Marken und vier verschiedene ganze Werte.
Drei davon — `EERP received`, `Message has been sent`, `EERP pending` — sind bereits lesbare
englische Sätze; für sie wäre eine „Übersetzung" allenfalls eine Eindeutschung. Übrig bliebe genau
**ein** echter Fall: `FTPSender`.

Und ausgerechnet der ist der, den eine Tabelle am schlechtesten abbildet: `FTPSender` löst dort, wo
er auflöst, auf **25 verschiedene** `SOSActionName` auf. Eine Tabellenzeile müsste diese 25 auf einen
Sammelbegriff eindampfen — sie wäre nicht die fehlende Übersetzung, sondern eine gröbere.

**Stattdessen die zweite Stufe der Namensauflösung** (§2). Sie liefert den *echten*
`SOSActionName` statt eines Sammelbegriffs, braucht keine gepflegte Zeile und deckt gemessen 28,26
Prozent (Fenster A) beziehungsweise 21,14 Prozent (Fenster B) der echten Schritte ab.

> **Die Schwachstelle, die dazugehört, und die durch den Bau nicht kleiner wird.** Der Weg über die
> Marke ist **auf Eindeutigkeit geprüft, nicht auf Richtigkeit.** Gemessen ist, dass es je Ablauf
> genau einen Schritt mit passender Marke gibt — nicht, dass es *derselbe* Schritt ist, der
> ausgeführt wurde. Genau der Beweis, der M15 trägt, ist hier konstruktionsbedingt nicht führbar: Er
> setzt eine aufgelöste `SOSAction`-Zeile voraus, und dass es keine gibt, ist der Anlass.
>
> Bei einem Ablauf mit zwei verschiedenen Bausteinen ist die Zuordnung zwingend; bei einem, der
> denselben Baustein zweimal ausführte, wäre sie es nicht. **Dieser Fall kommt in beiden Fenstern
> null Mal vor** — und die Eindeutigkeitsbedingung sorgt dafür, dass er zum Rohwert führt und nicht
> zu einem geratenen Namen. Das ist die Absicherung, die dieser Entwurf gegen seine eigene
> Schwachstelle hat; sie macht die Schwachstelle nicht kleiner, aber folgenlos.
>
> Die Herkunft `HERGELEITET` in der Antwort ist dafür der zweite Teil: Wer einem Namen misstraut,
> sieht an ihm sofort, auf welchem Weg er entstanden ist.

**`docs/prozessschritte-uebersetzung.md` entsteht deshalb nicht.** Der Eintrag in
[`README.md`](README.md) ist entsprechend vermerkt und nicht stillschweigend gelöscht.

### Kein Gerüst der geplanten Schritte (M21)

Ein Gerüst („Schritt 2 von 5") wird **nicht** gezeigt.

| | Fenster A | Fenster B |
|---|---|---|
| ausgeführt = geplant | 28,06 % | 45,18 % |
| ausgeführt < geplant | 71,63 % | 53,91 % |
| **ausgeführt > geplant** | **0,31 %** | **0,92 %** |

Es trägt in weniger als der Hälfte der Fälle vollständig, und in 0,31 bis 0,92 Prozent ist es
**nachweislich falsch**: Dort sind mehr Schritte ausgeführt worden, als der Ablauf heute definiert —
die Definition ist seit der Ausführung geändert worden, und ein Gerüst aus ihr wäre eine Lüge über
eine Nachricht, die es anders erlebt hat. **Der falsche Anteil wächst mit dem Zeitfenster**, passend
zu der Lesart, dass Abläufe zwischen Ausführung und heute geändert werden.

Dazu kommt: Die Nummerierung ist ohnehin lückenhaft. **257 von 1.777 Abläufen (14,5 %)** haben eine
größte Kennung über ihrer Schrittzahl, 233 nutzen Kennungen ab 99 — ein Ablauf nummeriert `1, 98,
99` (M20). Ein „Schritt 2 von 5" gäbe es also gar nicht. Und für die 2,2 bis 2,5 Prozent der
Nachrichten mit mehr als einem Ablauf hat kein einzelnes Gerüst Gültigkeit.

### Keine Obergrenze für die Schrittfolge (M16 2)

Siehe §4. Höchstens sieben Aktionen gemessen, eine Detailansicht lädt eine einzige Nachricht.

### Keine Deutung des Timeouts, keine Verkettung, kein Download

`SOSActionTimeout` wird geliefert und nicht gedeutet (§4). `SourceMessageID`, `TargetMessageID` und
die Flags `Source`/`Target` werden **nicht** aufgelöst — das ist Schritt 6; eine Aufteilungszahl aus
den kuratierten Eigenschaften erscheint als Zahl und verlinkt zu nichts. Der Filestore hinter
`Message.Payload.GUID` wird nicht aufgelöst — das ist Schritt 8.

---

## 12. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | beide Endpunkte nehmen eine `MessageID`; Mandant aus der Sitzung über `MandantService` |
| **M2** Mandant als erster Pflichtparameter | `NachrichtendetailRepository`, alle sechs Methoden; ArchUnit prüft es |
| **M3** Filter im Statement | `EXISTS` über `Process → ProjectMandant` in **jedem** Statement (§7), zusätzlich ohne DB geprüft |
| **M4** Isolationstest je Endpunkt | `NachrichtendetailIsolationDbIT` — für das Detail **und** für die Eigenschaften |
| **404 statt 403** | `RessourceNichtGefundenException`, fester Text; fremd und erfunden ununterscheidbar (§7) |
| **L1** Pflicht-Zeitfenster | gilt für Listen; hier ist die Nachricht über den Primärschlüssel benannt (§1) |
| **L2** keine Live-Aggregation | ein `COUNT` über `MessageProperty` **einer** Nachricht, `Using index` — keine Kennzahl über `Message` |
| **L4** `MessageProperty` nur über `MessageID` | Einstieg immer über die Kennung; die Namensbedingung nutzt den Primärschlüssel (`key_len 548`), niemals den Wert |
| **L7** jede Abfrage gemessen | §8, sechs Statements gegen drei Gestalten |
| **L8** keine Quelltabelle ohne Erhebung | `MessageAction`, `MessageProperty`, `SOSAction`, `Service` sind in M14 erhoben |
| **L9** kein Durchlauf ohne Zeitfenster in Anwendungscode | keiner; die Erhebungen dieses Dokuments tragen alle ein Fenster |
| **Z1** kein `now()` | die Zone der Zeitumrechnung kommt aus der Anwendungsuhr; sonst wird keine Zeit gebraucht |
| **Z2** `MessageTimeout` in Sekunden | `timeoutSekunden` im Kopf und je Schritt, Einheit aus `MessageStatusClassifier.TIMEOUT_EINHEIT` |
| **Q1** Fehlerbedingung | keine eigene; Einordnung ausschließlich über `MessageStatusClassifier.einordnung` |
| **Q2** kein Anlagedatum | `start` ist `MIN(MessageActionStart)`, `zeitpunkt` ist `MessageLastUpdate` |
| **Q4** nicht geraten | `Message.VFN` bleibt draußen; die Eindeutigkeitsbedingung in Stufe 2; `processName`/`projectName`/`sosName` bleiben `null` |
| **S1** kein Schreibzugriff | ausschließlich `SELECT` |
| **Status nie allein über Farbe** | dieselbe `StatusPlakette` wie in der Liste; der laufende Schritt trägt Kontur **und** Zeichen **und** Text (§10.4) |
| **Kein Farbwert in einer Komponente** | `tests/farbwerte.test.ts` deckt die vier neuen Dateien ab; der Balken nutzt `--muted`/`--muted-foreground`, die Auswahl in der Liste die Akzentfläche |
| **Keine Zeichenkette in einer Komponente** | alles in `texte.nachrichten.detail`, einschließlich der Einheiten der Dauer und der Beschriftung kuratierter Felder (§10.3) |
| **Die URL ist die einzige Quelle des Filterzustands** | `nachricht` steht in der URL; der Zustand des Eigenschaftenblocks ist **kein** Filterzustand und liegt deshalb in der Komponente (§10.2, §10.5) |
| **Next.js trifft keine Berechtigungsentscheidungen** | die Route reicht die Kennung durch und prüft nichts; über Sichtbarkeit entscheidet das Statement im Backend (§7) |

---

## 13. Offene Punkte

- **`LAEUFT_AUF` ist gebaut, aber lokal nicht im Normalbetrieb prüfbar.** `RUNNING` kommt in der
  Testkopie null Mal vor, und die einzige beobachtete Häufung offener Aktionen auf *offenen*
  Nachrichten ist ein Massenereignis von 49 Nachrichten in 62 Sekunden (M22). Was die Produktion
  zeigen wird, ist vermutlich der Einzelfall. Das Merkmal `MessageActionEnd IS NULL` selbst ist
  dagegen alltäglich — 46 der 95 Fälle verteilen sich über 36 verschiedene Tage —, nur eben auf
  *abgeschlossenen* Nachrichten, wo es `KEINER` ergibt.
- **Der Weg über die Marke ist plausibel und eindeutig, aber unbeweisbar** (§11). Die
  Eindeutigkeitsbedingung macht ihn folgenlos falsch statt still falsch; ein Beweis bliebe er
  trotzdem nicht. Wer ihn stützen will, braucht eine Auskunft aus dem Altsystem — insbesondere zu
  der Frage, ob `99` dort „letzter Schritt" bedeutet und `98` „vorletzter" (Frage 16 in
  `messungen-schritt5.md`). Wäre das so, ließe sich die Zuordnung *berechnen* statt herzuleiten.
- **`MessageActionID = 500` und `502`** kommen je 20-mal im Tagesfenster vor, tragen Bausteine, lösen
  nie auf und stellen zusammen mit `501` genau die 40 beziehungsweise 4.112 Fälle, die auf `ROHWERT`
  fallen. **Was sie bedeuten, ist nicht gemessen** (Frage 5). Sie erscheinen heute als normale
  Schritte mit ihrem Rohwert — falls sie fachlich keine sind, wäre das die Stelle, an der sie
  auszunehmen wären.
- **Die kuratierte Auswahl ist an zwei Namen und zwei Mandanten gemessen.** `Message.SplitCount`
  trägt `SUTTONS`, `Message.SendingPartner` trägt `IBIS`/`IBISGUS`. Für `ZAST` und `SYSTEM` ist der
  Block **immer leer**, für `NEXANS` in rund sechs von sieben Fällen. Das ist gewollt — leere Werte
  werden nicht geliefert —, aber es heißt auch: Der Nutzen dieses Blocks ist je Mandant sehr
  verschieden, und für den größten Mandanten ist er klein. Ob dort ein anderer Name trägt, ist
  offen; die vollständige Liste der 101 Namen steht in M17 (2).
- ~~**Ob die Oberfläche mehrere Werte eines kuratierten Namens sinnvoll darstellen kann.**~~
  **Entschieden in Teil 2** (§10.3): Sie zeigt sie alle, als mehrere Zeilen mit derselben
  Beschriftung. Der Fall bleibt trotzdem unbeobachtet — **für die beiden kuratierten Namen ist im
  Tagesfenster je genau ein Vorkommen gemessen**, die Darstellung ist also nur durch die Antwort
  gedeckt und nicht durch echte Daten. Dass ein Name mehrfach vorkommen *kann*, ist gemessen
  (M17 3, `Converter.Payload.GUID` auf 7.862 Zeilen über 6.149 Nachrichten) — nur eben nicht für
  diese zwei.
- **Die Kappung ist nie ausgelöst worden.** Der größte gemessene Wert liegt bei 12.732 Byte, die
  Grenze bei 16.384. Das Verhalten ist deshalb ausschließlich durch Unit-Tests belegt und nicht
  gegen echte Daten — was in der Natur der Sache liegt: Eine Grenze, die auf der Testkopie greift,
  wäre zu niedrig gewählt.
- **Die Zeitzonen-Umrechnung** setzt voraus, dass Anwendungs- und Datenbankserver dieselbe Zone
  haben. Für die Testkopie ist das gemessen; für die Produktion ist es die Annahme, die die
  Anwendungsuhr ohnehin macht. Unverändert gegenüber der Liste, und an derselben Stelle
  (`common/Zeitpunkte`).
- ~~**Die Beschriftung der Nachrichtenliste.**~~ **Nachgezogen in Teil 2.** Die Statuszelle sagt bei
  `WARTEND` jetzt „wartet vor: …" und bei `LAEUFT` „läuft auf: …"
  ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1). **Der Rest des Punktes bleibt offen, und
  zwar dauerhaft lokal:** Belegt ist „wartet vor" nur für `SUSPENDED` (M16 3); für `LAEUFT` steht
  die bisherige Lesart unverändert da, weil `RUNNING` in der Testkopie null Mal vorkommt. Die
  Beschriftung trägt beide Lagen — bewiesen ist nur eine.
- **Drei Teile der Oberfläche sind nicht gegen echte Daten gesehen** — die Lückenzeile,
  `LAEUFT_AUF` und `OHNE_SCHRITT` samt der Kappung. Vollständig mit Zahlen in §10.12.
- **Das schmale Fenster ist nicht gesehen** (§10.10, Punkt 15). Die Browsersteuerung kann das
  Fenster nicht verkleinern; geprüft ist das Regelwerk, nicht die Darstellung. Derselbe offene Punkt
  wie in [`nachrichtenliste.md`](nachrichtenliste.md) §8.4 und aus demselben Grund — er gehört von
  Hand nachgeholt.
- **`Escape` schließt das Panel, die eigene Route hat keine solche Taste.** Dort ist der
  Schließen-Knopf der erste Tabstopp nach der Navigation, ein Kürzel also entbehrlich; käme je eine
  zweite Ansicht mit langem Vorlauf dazu, wäre das die Stelle, an der man es nachzieht.
