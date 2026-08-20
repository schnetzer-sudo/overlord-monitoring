# Nachrichtendetail

Entsteht in Schritt 5. **Teil 1 (Backend)** ist §1 bis §9, **Teil 2 (Oberfläche)** ist §10.

Der Detail-Endpunkt beantwortet die Frage, die die Liste offen lässt: **Was ist im Einzelnen
passiert?** Nicht „wo steht mein Beleg" (das ist [`nachrichtenliste.md`](nachrichtenliste.md)) und
nicht „was hängt daran" (das ist Schritt 6).

Grundlage ist die Erhebung [`messungen-schritt5.md`](messungen-schritt5.md) — M14 bis M22 und der
Nachtrag S1. Wo unten eine Zahl steht, steht dort ihr Statement.

*Ergänzt am 20.08.2026:* Die Zahlen in den Abschnitten zu den Artefakten an der Zeitleiste (§5, §10.4)
stammen aus der zweiten Erhebung, [`messungen-schritt8.md`](messungen-schritt8.md) — **M55**, **M57**
und **M73**. Auch für sie gilt: Wo hier eine Zahl steht, steht dort ihr Statement.

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
  "rollen": ["SPLIT_WURZEL"],
  "zeitpunkt": "2025-12-29T22:53:50Z",
  "start": "2025-12-29T22:41:12Z",
  "gesamtdauerSekunden": 758,
  "fristSekunden": 1800,
  "eigenschaftenAnzahl": 22,
  "bamAnzahl": 9,
  "offenerZustand": "WARTET_IN",
  "naechsterSchritt": "Send Message to Pool",
  "wartetSeitSekunden": 15120,
  "ueberfaellig": true,
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

**`fristSekunden` hieß bis zum 10.08.2026 `timeoutSekunden`.** Es ist unverändert
`Message.MessageTimeout` in **Sekunden** (Regel Z2, M8) — neu ist, dass eine `0` als `null` geht:
„keine Frist gesetzt". **Umbenannt statt ergänzt:** Zwei Felder aus derselben Spalte mit
verschiedener `null`-Bedeutung wären eine zweite Wahrheit. Der Timeout **je Schritt** heißt
weiterhin `timeoutSekunden` und bleibt roh — dort ist die Bedeutung der `0` eine offene Frage
(Frage 8 in [`messungen-schritt5.md`](messungen-schritt5.md)), hier ist sie geklärt.

**`rollen` steht im Kopf** *(neu am 11.08.2026, Schritt 6 Teil 2b)* — die Stellung dieser Nachricht
in der Verkettung, **immer vorhanden, leer statt fehlend**. Ein fehlendes Feld hieße „unbekannt", ein
leeres heißt „nicht in einer Kette".

**Es kostet keinen Join und kein zweites Statement.** Die vier Verkettungsspalten liegen auf der
`Message`-Zeile, die `findeKopf` ohnehin liest; die Zugriffspfade in §8 ändern sich nicht, `Message`
bleibt `const`. Genau dafür war E4 die Messung: Ob eine Nachricht eine Kette hat, steht auf der Zeile
— ohne Abfrage.

**Abgeleitet wird in `common/Kettenrollen`**, der einen Stelle dafür — dieselbe Bauform wie beim
`MessageStatusClassifier`. Hier wird so wenig neu abgeleitet, wie hier neu klassifiziert wird.
Wozu die Oberfläche das Feld braucht, steht in [`verkettung.md`](verkettung.md) §8.1: Ist die Liste
leer, gibt es keinen Kettenblock und **keine zweite Anfrage**.

**Die Anzahl der Eigenschaften steht im Kopf, obwohl die Eigenschaften selbst nicht mitkommen.**
Ohne sie könnte die Oberfläche den eingeklappten Block nicht beschriften, ohne ihn zu laden — womit
der zweite Endpunkt seinen Zweck verlöre.

**`bamAnzahl` steht aus demselben Grund im Kopf** *(neu am 12.08.2026, Schritt 7 Teil 1)* — die Zahl
der Belegnummern auf dieser Nachricht, **immer vorhanden, `0` statt fehlend**. Sie beschriftet den
eingeklappten BAM-Block (§10.4b) und entscheidet, ob er überhaupt entsteht.

**Sie wiegt schwerer als die Anzahl der Eigenschaften**, weil die Leere hier der Normalfall ist:
**80,6 Prozent** aller Nachrichten in Fenster B tragen **keinen** BAM-Wert, bei Merge-Eingängen sind
es 38.628 von 38.628 (M41). Ohne die Zahl müsste die Oberfläche einen Block zeichnen und eine
Anfrage stellen, um festzustellen, dass er leer ist. Dieselbe Begründung wie bei `rollen`.

**Dieselbe Bauform wie `eigenschaftenAnzahl`:** eine zählende Unterabfrage über den Präfix des
Primärschlüssels von `MessageBAM`, `ref` auf `PRIMARY` mit `Using index` — sie liest **keinen
einzigen Wert**. Die Werte selbst liegen unter `GET /api/nachrichten/{messageId}/bam`, vollständig
beschrieben in [`bam-werte.md`](bam-werte.md).

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
| `LAEUFT_AUF` | Nachricht offen, und ein Schritt hat kein `MessageActionEnd` |
| `WARTET_IN` | Nachricht offen, jeder Schritt beendet, und `Message.SOSID`/`SOSActionID` zeigen auf den **zuletzt ausgeführten** Schritt |
| `WARTET_VOR` | Nachricht offen, jeder Schritt beendet, und sie zeigen auf einen **anderen** Schritt |
| `EMPFANGEN` | Nachricht offen, es gibt eine `MessageAction` mit `SOSActionID = 0` und **keine** mit `SOSActionID <> 0` |
| `OHNE_AKTION` | Nachricht offen, und es gibt **gar keine** `MessageAction` |
| `KEINER` | Nachricht nicht offen |

**Die Reihenfolge der Prüfung ist Teil der Regel.** „Keine Schrittfolge" muss **vor** „jeder Schritt
ist beendet" stehen: Über einer leeren Menge ist die zweite Bedingung wahr, und die Nachricht bekäme
einen Wartezustand samt einem Schritt, an dem sie gar nicht steht.

**Das `switch` ohne `default` erzwingt, dass jede Stelle bewusst nachgezogen wird.** Ein neuer Wert
soll einen Compilerfehler auslösen und keine stille Voreinstellung erben — dieselbe Bauart wie beim
`MessageStatusKind`. Bei der Aufteilung unten hat genau das gewirkt: Die beiden neuen Werte konnten
`wartetSeitSekunden` nicht stillschweigend als `null` erben.

### Der Wartezustand ist aufgeteilt — und die Aufteilung liegt im Backend

*Geändert am 10.08.2026, Anlass ist [M29](messungen-schritt5.md#m29--worauf-zeigt-messagesosactionid-bei-wartenden-nachrichten).*

Bis dahin gab es **einen** Wartezustand, und die Oberfläche verglich zwei gelieferte Felder —
`naechsterSchritt` gegen die Namen der gelaufenen Schritte —, um zu entscheiden, welchen Satz sie
schreibt (§10.11). Das widerspricht der Regel aus Teil 1: *Das Backend liefert den Zustand, die
Oberfläche stellt ihn dar und leitet nichts selbst ab.*

**Der Unterschied ist die Präposition, und sie ist ehrlich:** Die Nachricht wartet *in* einem
Schritt, der sie schlafen gelegt hat, oder *vor* einem, der noch nicht begonnen hat.

**Verglichen wird über die Kennungen, nicht über den Namen.** Zwei Schritte desselben Ablaufs können
gleich heißen; ein Namensvergleich wäre dann eine Verwechslung. Beide Hälften des zusammengesetzten
Schlüssels zählen — 2,51 Prozent der Nachrichten haben Schritte aus mehr als einem Ablauf (M20), und
eine `SOSActionID` allein ist dort nicht eindeutig.

**Ist `Message.SOSID` leer**, zeigt der Verweis auf nichts: Das ergibt `WARTET_VOR` mit einem
`naechsterSchritt` von `null`, und die Oberfläche benennt genau das. Über den Gesamtbestand läuft
dieser Verweis zu 43,9 Prozent ins Leere (M13).

> ⚠️ **`WARTET_VOR` ist ein unbeobachteter Zweig.** M29 hat über **alle 538** wartenden Nachrichten
> gemessen: **538 Mal** `WARTET_IN`, **null Mal** `WARTET_VOR`. Gebaut, unit-getestet, nie gesehen —
> vollständig in §10.12.

### Der »zuletzt ausgeführte Schritt« ist der letzte Schritt der Schrittfolge

**Also ohne `SOSActionID = 0`.** Der Metadaten-Schritt erscheint nicht in der Zeitleiste (S1); ihn
beim Vergleich mitzuzählen hieße, `WARTET_IN` gegen eine Zeile zu entscheiden, die der Nutzer nicht
sieht. Bei den 538 wartenden Nachrichten der Testkopie ändert das nichts — sie haben sämtlich genau
zwei echte Schritte (M29 1) —, aber die Produktion muss sich nicht daran halten, was die Testkopie
zufällig enthält.

**Der Schritt wird nach derselben Ordnung bestimmt, nach der die Zeitleiste sortiert:**
`MessageActionStart`, bei Gleichstand `MessageActionID`. Diese Ordnung steht an **genau einer
Stelle**, nämlich im `ORDER BY` von `findeAktionen`; der Service nimmt das letzte Element und
sortiert nicht ein zweites Mal. Ein zweites Sortierkriterium an einer zweiten Stelle wäre genau die
Drift, gegen die diese Regel gerichtet ist.

> M29 (0) hat nachgemessen, dass diese Ordnung und `MAX(MessageActionID)` auf allen 538 wartenden
> Nachrichten **dieselbe** Zeile treffen — 538 von 538, null Abweichungen. Das ist ein Befund über
> die Daten und keine Freigabe, im Code die andere Ordnung zu nehmen.

### `OHNE_SCHRITT` trug zwei Fälle und ist aufgeteilt worden

*Geändert am 10.08.2026, im Nachtrag zu Schritt 5.* Der Wert hieß zuletzt „es gibt keinen **echten**
Schritt" und deckte damit zwei Lagen ab, die verschiedene Fragen beantworten:

| Wert | Was er sagt |
|---|---|
| `EMPFANGEN` | Die Nachricht ist im System angekommen und seitdem nicht weitergelaufen — eine Auskunft über die **Plattform** |
| `OHNE_AKTION` | Zu dieser Nachricht ist kein Ablauf protokolliert — eine Auskunft über die **Datenlage** |

**Ein gemeinsamer Text müsste so vage sein, dass er beides abdeckt — und wäre dann für keinen der
beiden brauchbar.** Deshalb zwei Werte und zwei Sätze.

**Der Name `OHNE_SCHRITT` verschwindet und wird für keinen der beiden weiterverwendet.** Sonst
überlebt die alte, unscharfe Bedeutung in irgendeinem Kopf. `LAEUFT_AUF`, `WARTET_IN`, `WARTET_VOR`
und `KEINER` bleiben unverändert.

Beide erben, was die Runde davor entschieden hat: **Der Metadaten-Schritt ist kein Schritt der
Zeitleiste** (S1). Eine Nachricht, die nur ihn hat, als `LAEUFT_AUF` zu führen hieße, eine Zeile zu
markieren, die die Leiste gar nicht zeigt.

#### Woran der Code „nur Schritt 0" erkennt

An **`SOSActionID = 0`**, wie S1 es festlegt — nicht an `MessageActionID`.

> **Belegvermerk** (Regel L10).
> *Gemessen (M15 1, Fenster A):* `SOSActionID = 0` steht ausschließlich neben `MessageActionID = 0`
> und umgekehrt — 6.249 zu 6.249, ohne Abweichung.
> *Behauptet wäre:* Die beiden Spalten bezeichnen dieselbe Menge.
> **Die Lücke:** ein Tag und ein Fenster. Für die Erkennung ist sie folgenlos, weil S1 die Regel
> ohnehin an `SOSActionID` festmacht und dieselbe Deckung über **704.427** Aktionen **beider**
> Fenster nachgemessen hat, mit null Abweichungen in allen drei Paarvergleichen. Der Vermerk steht
> hier, weil er überall hingehört, wo ein Satz weiter reicht als seine Messung.

#### An den Daten der Testkopie ändert die Aufteilung nichts

Beide Fälle kommen dort null Mal vor: M16 (3) hat „gar keine Aktion" mit `0` gemessen, M29 (1)
„keine echte Aktion" mit `0` von 538. **Nicht widerlegt, nur nicht beobachtet** — mit je eigenem
Grund in §10.12.

**„Offen" heißt `WARTEND` oder `LAEUFT`** aus dem `MessageStatusClassifier` — ausdrücklich
aufgezählt und **nicht** über `istEndstatus` geholt. [`message-status.md`](message-status.md) führt
dazu eine eigene Warnung: Jene Methode gehört der Überfälligkeitsrechnung, und für `UNGEKLAERT`
antwortet sie `true`; in ihrem Zusammenhang die vorsichtige Antwort, hier wäre dieselbe `true` die
unvorsichtige. Dieselbe Entscheidung trifft die Liste in `NachrichtenService.aktuellerSchritt`, aus
demselben Grund.

**Nicht über `MessageStatus = 'RUNNING'` definiert** — den Wert gibt es in der Testkopie null Mal.
**Nicht auf `ERROR_TIMEOUT` gestützt** — M8 hat gezeigt, dass dieser Status nicht das Ablaufen von
`MessageTimeout` ist, sondern eine kürzere Frist auf Dienstebene.

### `WARTET_IN` ist der Normalfall des Wartens

Bei **allen 538** `SUSPENDED`-Nachrichten der Testkopie ist **jede** Aktion beendet (M16 3), und bei
**allen 538** zeigt der Verweis auf den Schritt, der zuletzt gelaufen ist (M29). Eine wartende
Nachricht steht also nicht *zwischen* zwei Schritten, sondern **in** dem Schritt, der sie schlafen
gelegt hat — dem mit `WAITUNTIL|…|SUSPEND`.

> **Das korrigiert die Lesart von M16 (3), nicht seine Zahl.** Dort stand „eine wartende Nachricht
> steht *zwischen* zwei Schritten". Gemessen war: jede Aktion ist beendet. Das stimmt weiterhin —
> nur folgt daraus nicht, dass ein *nächster* Schritt aussteht. M29 hat die Lücke gefüllt, die M16
> offen gelassen hat.

In **beiden** Wartezuständen kommt `naechsterSchritt` mit — aufgelöst aus `Message.SOSID` und
`Message.SOSActionID`. **Nullbar**, weil dieser Verweis über den Gesamtbestand zu 43,9 Prozent ins
Leere läuft (M13); bei allen 538 wartenden Nachrichten löst er auf (M29 3). Dass die Oberfläche ihn
bei `WARTET_IN` nur in den Tooltip schreibt, ändert nichts daran, dass er geliefert wird: Der Name
ist die Auskunft, welchen Schritt das Altsystem meint, und die hängt nicht davon ab, wie sie
dargestellt wird.

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

## 3a. Wartedauer, Frist und Überfälligkeit

*Neu am 10.08.2026.* Die Frage, die ein Nutzer vor einer hängenden Nachricht tatsächlich stellt,
lautet nicht „wie lange lag sie zwischen zwei Schritten", sondern **„wie lange steht sie schon"**.
Der Endpunkt beantwortet sie, statt die Oberfläche rechnen zu lassen.

| Feld | Wert |
|---|---|
| `wartetSeitSekunden` | der Bezugspunkt hängt am offenen Zustand — siehe die Tabelle darunter |
| `fristSekunden` | `Message.MessageTimeout` — eine Dauer in **Sekunden**, kein Zeitpunkt (M8). `null` bei `NULL` und bei `0` |
| `ueberfaellig` | ob `MessageLastUpdate + MessageTimeout` in der Vergangenheit liegt **und** die Nachricht nicht in einem Endstatus ist |
| `gesamtdauerSekunden` | fachlicher Start bis `MessageLastUpdate`, für **jede** Nachricht |

**Die Berechnung läuft im Backend gegen die Anwendungsuhr, niemals im Browser.** Im Dev-Profil ist
die Uhr um Monate versetzt; eine Oberfläche, die `Date.now()` gegen einen gelieferten Zeitstempel
rechnete, zeigte dort Monate statt Stunden. Das ist genau der Grund, warum es die Uhr gibt.

Es ist die **Anwendungsuhr**, nicht die Systemuhr — die Ausnahme in Regel A5 gilt für
*sicherheitsrelevante* Zeit, also Sperrfristen und Sitzungsablauf.

**Eine negative Dauer wird `null`**, bei beiden Feldern und aus demselben Grund wie bei
`dauerSekunden` (§4): „wartet seit minus drei Sekunden" ist schlechter als gar keine Angabe.
Vorgekommen ist es nicht — die Anwendungsuhr steht auf dem jüngsten Zeitpunkt des Bestands und läuft
vorwärts —, aber eine Uhr, die einmal zurückspringt, soll keine negative Dauer erzeugen.

### Der Bezugspunkt je Zustand

| Zustand | Bezugspunkt |
|---|---|
| `LAEUFT_AUF` | Beginn der offenen Aktion |
| `WARTET_IN`, `WARTET_VOR` | Ende der letzten Aktion **der Schrittfolge** |
| **`EMPFANGEN`** | **Ende des Schritts `0`; ist es nicht gesetzt, dessen Beginn** |
| `OHNE_AKTION` | `null` — es gibt keinen Anker |
| `KEINER` | `null` |

> **Korrigiert am 10.08.2026.** Hier stand: *„Hat eine Nachricht ausschließlich den Schritt `0`, ist
> das der Fall `OHNE_SCHRITT`, und `wartetSeitSekunden` ist `null` — nicht die Zeit seit dem
> Metadaten-Eintrag."* **Das war verkehrt herum.**
>
> Eine Nachricht, die um 14:32 angekommen und seitdem nicht angefasst worden ist, **hängt** — und
> das ist der Zustand, in dem ein Nutzer dieses Werkzeug öffnet. Mit `null` sagte das Feld an der
> einzigen Stelle nichts, an der es etwas zu sagen hätte. Die alte Fassung hat auf definitorische
> Sauberkeit optimiert (*„der Metadaten-Schritt zählt nicht zu den Schritten"*) statt auf die Frage
> des Nutzers. Der Schritt `0` ist kein Verarbeitungsschritt, aber er **ist** ein Ereignis mit einem
> Zeitpunkt — aus genau diesem Grund rechnet der fachliche Start ja auch über ihn (M17 3).

**Bei `OHNE_AKTION` bleibt es bei `null`, und zwar konsequent:** Der fachliche Start im Kopf ist dort
ebenfalls `null` (`MIN(MessageActionStart)` über eine leere Menge). Eine Dauer aus
`MessageLastUpdate` zu rechnen wäre eine erfundene Zahl — der Zeitpunkt der letzten Änderung ist
nicht der Zeitpunkt des Eingangs.

**`ueberfaellig` ändert sich durch die Aufteilung nicht** — es hängt an `MessageLastUpdate +
MessageTimeout` und `istEndstatus`, nicht am offenen Zustand. Erwähnenswert ist trotzdem, dass die
Kategorie mit `EMPFANGEN` zum ersten Mal einen Zustand bekommt, in dem sie wirklich etwas sagt: eine
Nachricht, die eingegangen und über ihre Frist hinaus nicht weitergelaufen ist.

### Auch hier zählt der Metadaten-Schritt nicht — außer bei `EMPFANGEN`

„Die letzte Aktion" ist die letzte Aktion **der Schrittfolge**, also ohne Schritt `0`.

> **Die beiden Mengen rund um den Metadaten-Schritt, nebeneinander — und warum das keine
> Unsauberkeit ist.**
>
> | Frage | Menge | Grund |
> |---|---|---|
> | **fachlicher Start** (`start`, `gesamtdauerSekunden`) | **alle** Aktionen, **einschließlich** Schritt `0` | Der Start ist der Zeitpunkt, an dem die Nachricht ins System kommt — und das *ist* der Metadaten-Schritt (M17 3: dort hängen die `Message.*`-Eigenschaften und die `*Reader`-Dienste). Ihn auszunehmen ergäbe einen zu späten Start |
> | **letzter Schritt** (`WARTET_IN`/`WARTET_VOR`, `LAEUFT_AUF`, die Zeitleiste) | **nur echte** Schritte, **ohne** Schritt `0` | Der letzte Schritt ist etwas, das der Nutzer in der Leiste sieht. Gegen eine unsichtbare Zeile zu entscheiden hieße, ihm eine Aussage über etwas zu machen, das für ihn nicht existiert |
> | **`EMPFANGEN`** | **genau** Schritt `0` | Hier ist der Metadaten-Schritt nicht die falsche Zeile, sondern die einzige Auskunft, die es gibt — dieselbe Lesart wie beim fachlichen Start. Er wird trotzdem keine Zeile der Leiste |
>
> **Verschiedene Fragen, verschiedene Mengen.** Beim nächsten Lesen sieht das nach einer
> Inkonsistenz aus; es steht deshalb hier, samt Begründung. Das Kriterium ist in allen drei Fällen
> dasselbe (`SOSActionID = 0`, S1) — verschieden ist nur, ob es ausschließt, einschließt oder allein
> steht.

### `fristSekunden` und `timeoutSekunden` sind absichtlich verschieden

Der Kopf trägt seit der Nachbesserung `fristSekunden`, der Schritt weiterhin `timeoutSekunden`. Zwei
Namen, zwei **verschiedene Größen** — und das ist keine Uneinheitlichkeit.

| Feld | Quellspalte | Wo | Bedeutung |
|---|---|---|---|
| `fristSekunden` | `Message.MessageTimeout` | Kopf | die Frist der **Nachricht**; `0` und `NULL` gehen beide als `null` hinaus |
| `timeoutSekunden` | `MessageAction.SOSActionTimeout` | je Schritt | die Frist des **Ablaufschritts**, roh geliefert und nicht gedeutet |

**Sie werden nicht zusammengeführt, und das ist kein Versäumnis an der Vereinheitlichung.** M8 hat
gemessen, dass die beiden fachlich auseinanderliegen: Alle 52 `ERROR_TIMEOUT`-Nachrichten tragen
`MessageTimeout = 1800`, und trotzdem liegen zwischen dem Start ihrer letzten Aktion und dem Fehler
**höchstens 120 Sekunden**; jede von ihnen trägt genau eine Aktion mit `SOSActionTimeout = 0`. Der
Status hängt damit an einer **Dienstfrist** — weder an der Nachrichtenfrist noch an der Schrittfrist,
die dort `0` ist. Ein gemeinsames Feld behauptete eine Gleichheit, die gemessen nicht besteht.

> **Belegvermerk** (Regel L10).
> *Gemessen (M8):* Bei 52 von 52 `ERROR_TIMEOUT`-Nachrichten stehen 1.800 Sekunden Nachrichtenfrist,
> `0` Sekunden Schrittfrist und höchstens 120 Sekunden tatsächlicher Abstand nebeneinander.
> *Behauptet wird:* Nachrichtenfrist und Schrittfrist sind zwei verschiedene Größen.
> **Die Lücke:** Der Schluss ruht auf einem Status. Dass die beiden Spalten *überall* auseinander
> laufen, ist nicht gemessen — gemessen ist, dass sie an **einer** Stelle nachweislich verschiedene
> Dinge sagen. Für die Entscheidung „nicht zusammenführen" genügt das: Ein Gegenbeispiel widerlegt
> eine Gleichsetzung, auch wenn es sie nicht flächendeckend widerlegt.

Die Einheit ist in beiden Fällen dieselbe (**Sekunden**, Regel Z2) und steht an genau einer Stelle
als benannte Konstante — `MessageStatusClassifier.TIMEOUT_EINHEIT`. Gleich ist die Einheit, nicht die
Größe.

Die `0` behandeln die beiden Felder verschieden, und auch das ist Absicht: Im Kopf ist ihre Bedeutung
geklärt („keine Frist gesetzt"), je Schritt ist sie eine offene Frage (Frage 8 in
[`messungen-schritt5.md`](messungen-schritt5.md)). Ein Feld, das eine ungeklärte `0` in ein `null`
übersetzte, nähme die Antwort vorweg.

### `ueberfaellig` ist Problemkategorie 2, und sie entsteht an genau einer Stelle

`MessageStatusClassifier.istUeberfaellig` in `common` — eine benannte, von außen aufrufbare Einheit,
die das Dashboard später **ruft** statt sie dort nachzubauen. Dasselbe Muster wie die Einordnung
selbst. Sie prüft beide Bedingungen: nicht in einem Endstatus **und** Frist abgelaufen. Nicht über
`MessageStatus = 'RUNNING'`, nicht über `ERROR_TIMEOUT`.

> **Die Prüfung „nicht in einem Endstatus" benutzt `istEndstatus`** — und zwar genau hier, denn
> [`message-status.md`](message-status.md) weist die Methode ausdrücklich der Überfälligkeitsrechnung
> zu und sonst niemandem. **Nicht** die Aufzählung `WARTEND`/`LAEUFT`, die §3 für den offenen Zustand
> verwendet. Die beiden Formen stehen aus guten Gründen nebeneinander im Code und sind hier nicht
> austauschbar: Für `UNGEKLAERT` liefert `istEndstatus` `true`, und das ist in dieser Rechnung die
> vorsichtige Antwort — keine Behauptung, die Nachricht hänge. In §3 wäre dieselbe `true` die
> unvorsichtige.

Ist `MessageTimeout` null oder nicht gesetzt, ist `ueberfaellig` `false` und `fristSekunden` `null` —
keine erfundene Frist.

**Die Kategorie ist gegen die Testkopie praktisch nicht prüfbar.** Über den Gesamtbestand sind
*alle* 538 offenen Zeilen überfällig, im 24-Stunden-Standardfenster genau eine — beides sagt nichts
darüber, ob die Frist fachlich richtig gewählt ist. Das steht schon in
[`message-status.md`](message-status.md) und gilt hier unverändert. Der Datenbanktest prüft deshalb
nicht *ob*, sondern die **Kopplung**: ohne Frist keine Überfälligkeit, und wer überfällig ist, ist
offen.

### Die Gesamtdauer ist die Abdeckung für die gestrichene Lückenzeile

Endet Schritt 3 und beginnt Schritt 4 drei Stunden später, weil ein Dienst weg war, steht diese Zeit
in **keiner** Schrittdauer — und nach dem Streichen der Lückenzeile (§10.12) wäre sie unsichtbar.
Passt die Summe der Schrittdauern nicht zur Gesamtdauer, steckt die Zeit dazwischen, und man sieht
es, ohne dass ein Element dafür existiert, das nie jemand ausgelöst hat.

Sie kommt für **jede** Nachricht, offen oder nicht: Auch bei einer abgeschlossenen ist „wie lange hat
das gedauert" eine sinnvolle Frage, und das Feld hat mit dem offenen Zustand nichts zu tun.

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
Hauptinformation. `Message.Payload.GUID` wird in **Schritt 8 ein Knopf** und kein Anzeigewert;
`Message.SourceMessageID` ist seit Schritt 6 eine **Verkettung** und erscheint als Zeile im
Kettenblock — gelesen aus der Spalte von `Message` und nicht aus dieser Eigenschaft.

> **Korrigiert 20.08.2026, nachgetragen zur Korrektur vom 19.08.2026.** Hier steht:
> „`Message.Payload.GUID` wird in **Schritt 8 ein Knopf** und kein Anzeigewert". Er wird in
> Schritt 8 **gar nichts** — weder Anzeigewert noch Knopf.
>
> **Gemessen ist (M73):** Der Name trägt in **6.249 von 6.249** Nachrichten (Fenster A) und
> **214.330 von 214.330** (Fenster B) denselben Verweis wie die Nutzdatenzeile mit dem **höchsten
> `MessageActionID`** derselben Nachricht — kein Gegenfall. Er benennt keine eigene Datei, sondern
> zeigt auf eine, die ohnehin an ihrem Schritt hängt, und ist deshalb aus der Artefaktliste
> entfallen ([`rohdaten.md`](rohdaten.md) §5, [`rohdaten-backend.md`](rohdaten-backend.md) §7,
> Erhebung in [`messungen-schritt8.md`](messungen-schritt8.md)).
>
> *Ausdrücklich nicht behauptet:* dass „höchster `MessageActionID`" gleichbedeutend mit „zeitlich
> zuletzt" ist. Gemessen ist die Schrittnummer, nicht die Uhr.
>
> **Der Ausschluss selbst bleibt richtig** — nur seine Begründung nicht. Dieselbe Berichtigung steht
> im Javadoc von `KuratierteEigenschaften`.

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
| `NachrichtendetailServiceTest` | **ohne DB** — die **sechs** offenen Zustände samt `WARTET_IN` gegen `WARTET_VOR` (beide Schlüsselhälften, Verweis auf einen nicht-letzten Schritt, Verweis ins Leere) und `EMPFANGEN` gegen `OHNE_AKTION` (Wartedauer ab dem Ende des Schritts `0`, Rückfall auf dessen Beginn, `null` ohne jede Aktion, `EMPFANGEN` kann überfällig sein), Metadaten-Schritt ausgenommen, offener Metadaten-Schritt ist kein Hänger, Wartedauer gegen eine **feste** Anwendungsuhr, Frist und Überfälligkeit samt `UNGEKLAERT`, Gesamtdauer, negative Dauer, kuratierte Auswahl, Kappung auf Zeichengrenze, UTC-Umrechnung |
| `NachrichtendetailStatementsTest` | **ohne DB** — Mandantenfilter in jedem Statement, kein Zugriff über `MessagePropertyValue`, Begrenzung des Werts in der Abfrage |
| `NachrichtendetailDbIT` | `@Tag("db")` — echte Schrittfolgen, `WARTET_IN` samt Wartedauer, die Kopplung von `ueberfaellig` an Frist und Offenheit, die Gesamtdauer deckt die Schrittdauern ab, beide Auflösungsstufen kommen vor, Anzahl im Kopf stimmt mit der Liste überein |
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
   ├─ ansicht-umschalter.tsx       ab 11.08.2026 — der Knopf zwischen beiden (§10.7)
   ├─ zeitleiste.tsx               Schritt-, Lücken- und Erwartungszeile
   ├─ eigenschaften-block.tsx      eingeklappt, lädt erst beim Aufklappen
   ├─ kette-block.tsx              ab Schritt 6, Teil 2b — zwischen Kopf und Zeitleiste
   └─ nachrichten-tabelle.tsx      der Zeilenklick bekommt seine Funktion

app/(app)/nachrichten/[messageId]/page.tsx   Server-Komponente, reicht die Kennung durch
lib/format.ts                                + formatiereDauer
lib/routen.ts                                + die beiden Zielrouten des Umschalters (11.08.2026)
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

**Daneben die Gesamtdauer** (`gesamtdauerSekunden`, §3a) — sie steht zwischen Beginn und Projekt,
weil sie genau die Spanne zwischen den beiden Zeitfeldern darüber ist. Fehlt sie (keine Aktion mit
Start), erscheint dieselbe Kennzeichnung wie bei jedem anderen fehlenden Wert.

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

#### Die Wartezeile am offenen Zustand

*wartet seit 4 h 12 min · Frist 30 min* — eine Zeile unter der Leiste, sobald
`wartetSeitSekunden` gesetzt ist. Bei `LAEUFT_AUF` heißt sie *läuft seit …*; das Verb kommt aus dem
Zustand, nicht aus einer Bedingung in der Komponente.

**Beide Zahlen kommen fertig aus dem Backend** (§3a). Die Oberfläche entscheidet nur, ob die Zeile
erscheint und welches Verb sie trägt — gerechnet wird hier nichts, weil die Anwendungsuhr im Profil
`dev` Monate zurücksteht.

Fehlt die Frist, steht eben nur die eine Hälfte da. Eine erfundene Frist wäre schlechter als keine.

**Bei `ueberfaellig` wird die Zeile hervorgehoben — und zwar ohne eine einzige Farbe.** Nicht nur
„nie allein über Farbe", sondern hier **gar nicht** über Farbe: Rot gehört nach
[`visuelles-konzept.md`](visuelles-konzept.md) §7 ausschließlich der Kategorie *Fehler*. Würde
„überfällig" rot, verschmölzen zwei der drei Problemkategorien in der Wahrnehmung, obwohl Regel Q3
sie im Code sorgfältig trennt. Ein Status-Gelb gibt es in diesem Farbsystem nicht, und eine eigene
Rolle dafür ist dort ausdrücklich einer späteren Entscheidung vorbehalten — sie hier zu erfinden
hieße, dieser Entscheidung vorzugreifen. Die Hervorhebung ist deshalb **Zeichen, Wort und
Schriftstärke** gegen die gedämpfte Umgebung.

**Das Wort für die Kategorie steht in `texte.problem`**, auf oberster Ebene der Sprachdatei und
nicht unter `nachrichten`: Das Detail benennt die Kategorie an einer Nachricht, das Dashboard zählt
sie über viele — beide müssen dasselbe Wort sagen, sonst hält ein Nutzer dieselbe Sache für zwei
Sachen.

#### Die Lückenzeile ist entfernt worden

**Sie war gebaut, mit Schwelle (`LUECKE_SCHWELLE_SEKUNDEN` = 60), Unit-Tests und einer eigenen Zeile
zwischen zwei Schritten — und sie ist am 10.08.2026 wieder entfernt worden.** Der Grund steht in
§10.12: Über rund 700 geprüfte Nachrichten ist sie nie erschienen, weil die größte Lücke zwischen
zwei Schritten **eine Sekunde** beträgt. Das ist kein knapper Fehlschlag, sondern strukturell — die
Wartezeit steckt in der Dauer des `WAITUNTIL`-Schritts, nicht zwischen zwei Schritten.

An ihre Stelle sind zwei Dinge getreten, die dieselbe Frage besser beantworten: die **Wartezeile**
oben (für die offene Nachricht) und `gesamtdauerSekunden` im Kopf (für die abgeschlossene). Ein
Vitest hält fest, dass zwischen zwei Schritten **keine** Zeile mehr entsteht, auch bei drei Stunden
Abstand nicht — die Streichung ist damit eine Zusage und kein Versehen.

#### Der offene Zustand wird gezeigt, nicht errechnet

Das Backend liefert ihn als Feld (§3). Die Oberfläche stellt ihn dar und leitet nichts ab:

| Feld | Darstellung |
|---|---|
| `LAEUFT_AUF` | der betroffene Schritt ist markiert — Kontur in der Rolle `--status-offen`, dazu Zeichen **und** Text „läuft gerade"; keine Dauer, kein Balken |
| `WARTET_IN` | **nach** dem letzten Schritt eine Zeile mit gestrichelter Kontur: *„Die Nachricht wartet — von selbst geht es hier nicht weiter."* **Der Name wird nicht wiederholt** — er steht eine Zeile darüber; der Verweis bleibt im Tooltip |
| `WARTET_VOR` | dieselbe Zeile, aber mit dem **Namen** des Schritts und „noch nicht begonnen" |
| `EMPFANGEN` | statt einer leeren Leiste: *„Empfangen am … — seitdem ist kein Schritt ausgeführt worden."* Darunter die **Wartezeile** wie bei den anderen offenen Zuständen |
| `OHNE_AKTION` | *„Zu dieser Nachricht ist kein Ablauf protokolliert."* — und **keine** Wartezeile |
| `KEINER` | nichts Zusätzliches |

**Der Zeitpunkt in der `EMPFANGEN`-Zeile ist `start`**, also der fachliche Start — und der ist dort
genau der Metadaten-Schritt, die einzige Aktion, die es gibt. Formatiert mit derselben Zone und
derselben Funktion wie im Kopf. Fehlt er (`MessageActionStart` ist `NULL`-fähig, gemessen aber auf
keiner der 10,3 Millionen Zeilen leer, M22), steht der Satz ohne Datum da statt mit einem
Platzhalter.

**Bei `OHNE_AKTION` bleibt die Wartezeile weg, ohne dass die Komponente das entscheidet:** Das
Backend liefert dort keine Wartedauer, und die Zeile hängt allein daran (§3a). Eine Zeile mit
Platzhalter wäre schlechter als keine.

**Die Zeitleiste selbst bleibt, wie sie war.** Der Schritt `0` erscheint auch bei `EMPFANGEN` nicht
als Zeile (S1). Was sich am 10.08.2026 geändert hat, ist der Satz **über** der leeren Leiste — nicht
ihr Inhalt.

**Die Wortwahl folgt dem gelieferten Zustand, nicht einem Vergleich.** Bis zum 10.08.2026 verglich
`detail.ts` `naechsterSchritt` mit den Namen der gelaufenen Schritte; jetzt trägt das Feld die
Antwort (§3). Ein Vitest hält fest, dass ein zufällig gleicher Name daran nichts mehr ändert.

Ist der benannte Schritt `null`, wird das benannt und nicht weggelassen: *„Die Nachricht wartet —
worauf, ist in der Ablaufdefinition nicht hinterlegt."* Ihn stillschweigend zu unterschlagen hieße,
eine offene Nachricht wie eine abgeschlossene aussehen zu lassen. Über den Gesamtbestand läuft dieser
Verweis zu 43,9 Prozent ins Leere (M13).

> **„Die Leiste endet dort" heißt nicht, dass etwas abgeschnitten wird.** Bei `LAEUFT_AUF` hängt die
> Zeitleiste nichts an — mehr nicht. Schritte hinter dem laufenden werden **nicht** weggelassen: Der
> laufende ist in den Daten der letzte (er trägt kein Ende, bekommt also weder eine Lückenzeile noch
> eine Fortsetzung), und gemessene Zeilen stillschweigend zu verschweigen wäre etwas anderes als
> eine Leiste, die von selbst dort aufhört.

**`EMPFANGEN`, `OHNE_AKTION` und „abgeschlossen ohne Schritt" sehen in der Zeilenliste gleich aus —
leer.** Was der Nutzer liest, entscheidet die Komponente über den *Zustand* und nicht über die Länge
der Liste; sonst hieße „angekommen und seitdem nichts" dasselbe wie „fertig und ohne Schritt".

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

#### Ergänzung vom 18.08.2026 — die Zeile trägt die Artefakte ihres Schritts

> **Kein Satz des Abschnitts darüber ist falsch geworden.** Die Rechnung in `detail.ts`, die
> Normierung des Balkens, die Wartezeile, die erwartete Zeile, die drei Texte der leeren Leiste, die
> feste Zeilenhöhe, der Tooltip mit der Herkunft — all das gilt unverändert. Ergänzt ist **zweierlei
> an der Zeile**, und beides ist Anzeige und keine Rechnung.

Anlass ist die Nachbesserung von Schritt 8: Die Artefakte einer Nachricht hängen über
`MessageActionID` **an ihrem Schritt** (M57) und standen trotzdem in einem eigenen Block darunter,
der dieselben Schrittnamen ein zweites Mal führte. Vollständig begründet in
[`rohdaten.md`](rohdaten.md) §3 (Kasten zu Entscheidung 6) und
[`rohdaten-frontend.md`](rohdaten-frontend.md) §3.

| Ergänzt | Was es tut |
|---|---|
| **Die Ziele** | je Schritt bis zu zwei kleine Zeichen — Datei und Protokoll —, jedes ein Verweis auf `/nachrichten/{id}/dateien/{artefaktId}`. Wo nichts liegt, hängt nichts. Sichtbar ist allein das Zeichen; der Name steht im `sr-only`-Text, der Rohname im `title` |
| **Der Name als Weg zu den Eigenschaften** | er wird zur Schaltfläche und führt in die Gruppe desselben Schritts (§10.5). **Nur wo es Eigenschaften gibt** — bei `eigenschaftenAnzahl === 0` bleibt er Text |

**Die Leiste führt Schritt `0` weiterhin nicht.** Das ist der Punkt, an dem eine Ergänzung zur
Änderung geworden wäre: `schritte[]` bleibt die einzige Quelle der Zeilen, und der Metadaten-Schritt
kommt dort nicht vor (§4). Was auf ihm liegt — das Paar des Lesedienstes, Datei und Protokoll (M57)
—, steht in einer eigenen, gestrichelten Zeile **über** der Leiste, beschriftet mit *Eingang*.
Gestrichelt wie die erwartete Zeile: Was gestrichelt ist, ist kein ausgeführter Schritt. **Liegt
dort nichts, gibt es die Zeile nicht** — dasselbe „wo nichts liegt, hängt nichts" wie an den
Schrittzeilen.

> **Korrigiert 20.08.2026, nachgetragen zur Korrektur vom 19.08.2026.** Hier stand bis heute: „Die
> Artefakte, die auf ihm liegen — **die eingegangene Datei und das Paar des Lesedienstes** (M57) —,
> stehen in einer eigenen, gestrichelten Zeile über der Leiste." Das dritte Ziel war
> `Message.Payload.GUID`, geführt als *Eingegangene Datei*, und es ist nach **M73** aus der Liste
> entfallen (Begründung oben unter „Was ausdrücklich ausgeschlossen ist").
>
> **Die Zeile trägt seither höchstens zwei Ziele — und manchmal keines.** Für `MessageActionID = 0`
> führt M57 (Fenster A) ausschließlich die neun Lesedienst-Paare und `Message.Payload.GUID`. Wo kein
> Lesedienst auf Schritt `0` liegt, lag dort also **nur** der entfallene Name, und die Zeile bleibt
> heute leer. **Gemessen (M73, Befund 6):** Das sind **950 von 6.249** Nachrichten in Fenster A und
> **28.616 von 214.330** in Fenster B — genau die Nachrichten mit `DataWarehouse.Payload.GUID`,
> vollständig komplementär zu den **5.299** bzw. **185.714** mit Lesedienst, ohne eine einzige
> Ausnahme.
>
> **Verloren geht dabei nichts:** `DataWarehouse.Payload.GUID` liegt auf Schritt `1` (M57) und
> hängt damit an einer Zeile der Leiste. Die Zeile *Eingang* verschwindet, kein Artefakt.
>
> **Die übrige Aussage bleibt** — die Zeile steht über der Leiste, gestrichelt, ohne Balken und ohne
> Dauer, und die Leiste bekommt für Schritt `0` weiterhin keine eigene Zeile.
>
> **Belegvermerk (L10):** Gemessen ist, **welche** Namen auf `MessageActionID = 0` liegen (M57) —
> nicht, was die Dateien dahinter sind. Dass das Paar des Lesedienstes den *Eingang* der Nachricht
> bezeichnet, beruht auf einer Sichtprüfung des Auftraggebers an **einer** Nachricht vom
> 19.08.2026. Offener Punkt 17 in [`rohdaten-frontend.md`](rohdaten-frontend.md) §11.
>
> Belege durchgehend in [`messungen-schritt8.md`](messungen-schritt8.md) unter M57 und M73.

### 10.4a Der Kettenblock — zwischen Kopf und Zeitleiste

*Neu am 11.08.2026 (Schritt 6, Teil 2b).* Er beantwortet die dritte Frage des Werkzeugs — **was
hängt an dieser Nachricht** — und steht deshalb **zwischen Kopf und Zeitleiste**: näher an der
Nachricht selbst als der Ablauf ihrer Schritte.

**Ist `rollen` leer, gibt es ihn nicht** — keine Überschrift, kein leerer Kasten, und **keine
Anfrage auf `/kette`**. Dieselbe Regel wie beim Eigenschaftenblock bei `eigenschaftenAnzahl === 0`.
Genau deshalb darf er dauerhaft sichtbar sein statt eingeklappt: Sein Hauptnachteil wäre gewesen,
bei der Mehrheit der Nachrichten Platz ohne Inhalt zu kosten.

**Vollständig beschrieben ist er in [`verkettung.md`](verkettung.md) §8** — die Einteilung nach der
Flussrichtung, die Zahl in der Überschrift, das Nachladen statt eines Sprungs in die Liste, die
Zustände und warum der Block keine eigene Farbe trägt. Hier steht nur, **wo** er sitzt und **dass**
er die Zeitleiste nicht anfasst: An ihr, an der Wartezeile und an den Zuständen ändert sich nichts.

### 10.4b Der BAM-Block — zwischen Kettenblock und Zeitleiste

*Neu am 12.08.2026 (Schritt 7, Teil 1).* Er beantwortet die **erste** Frage des Werkzeugs —
**welcher Beleg ist das** — und steht deshalb **vor der Zeitleiste**: Die beantwortet, *was mit dem
Beleg passiert ist*, und nach dem Leitsatz kommt die erste Frage zuerst.

Die Reihenfolge im Panel ist damit: **Kopf → Kette → Belegdaten → Zeitleiste → Eigenschaften.** Von
oben nach unten: *was ist das*, *was hängt daran*, *welcher Beleg ist das*, *was ist passiert*, und
zuletzt das Technische.

**Ist `bamAnzahl` null, gibt es ihn nicht** — kein Rahmen, kein Schalter, **keine Anfrage auf
`/bam`**. Bei 80,6 Prozent der Nachrichten ist das der Fall, bei Merge-Eingängen bei allen (M41).
Dieselbe Regel wie beim Kettenblock; anders als der Eigenschaftenblock lässt er dabei auch **keine
Zeile Text** stehen — die Begründung steht in [`bam-werte.md`](bam-werte.md) §11.

**Eingeklappt, lädt beim Aufklappen**, mit der Zahl in der Überschrift: dieselbe Bauform wie der
Eigenschaftenblock (§10.5) und aus demselben Grund.

**Vollständig beschrieben ist er in [`bam-werte.md`](bam-werte.md)** — die Zweiteilung aus Zählung
und gedeckelten Werten, die Deckelung bei zwanzig je Gruppe, die Entscheidung gegen den
Konfigurationsfilter, die Sortierregel und der Schlüssel `(typ, wert)`. Hier steht nur, **wo** er
sitzt und **dass** er nichts anfasst: An Zeitleiste, Wartezeile, Zuständen und Eigenschaftenblock
ändert sich nichts.

> **Nacharbeit vom 13.08.2026 — und §10.3 ist ausdrücklich nicht betroffen.** Der Block ordnet seine
> Werte seitdem **nebeneinander als Marken**, mit der Beschriftung links in einer **gedeckelten**
> Spalte ([`bam-werte.md`](bam-werte.md) §11a). Das ist dieselbe Grundform *„Beschriftung links,
> Wert rechts"* wie die Beschreibungsliste im Kopf (§10.3) — **mit einem Unterschied, der der Punkt
> ist:** Die Liste im Kopf ist **inhaltsbreit** (`grid-cols-[auto_1fr]`), der BAM-Block ist
> **gedeckelt**. Der Kopf darf inhaltsbreit sein, weil seine Beschriftungen kurz und in der Zahl
> fest sind; die BAM-Beschriftungen kommen aus dem Altsystem und werden bis zu 35 Zeichen lang.
> **Die Beschreibungsliste im Kopf ist deshalb nicht mit umgestellt worden.**

> **Nachgebessert am 13.08.2026, nach der Nacharbeit desselben Tages: Auf der eigenen Route ist der
> Deckel weiter** — 16 statt 10 rem, gesetzt am Wrapper der Route und nicht am Block
> ([`bam-werte.md`](bam-werte.md) §11a). Die 10 rem sind für das **Panel** gemessen; hier ist
> dieselbe Gruppenzeile gemessene 1.126 px breit statt 454, und die Beschriftungen brachen um, ohne
> dass der Wert dadurch Platz gewann. **§10.3 bleibt auch davon unberührt** — sie ist inhaltsbreit und
> hat gar keinen Deckel, den man je Einhängepunkt setzen könnte.

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

#### Nacharbeit vom 17.08.2026 — die Werte stehen nach Schritt gruppiert

> **Kein Satz des Abschnitts darüber ist falsch geworden.** Eingeklappt, beschriftet aus dem Kopf,
> Laden beim Aufklappen, Ladezustand im Block, `key={messageId}`, Zustand nicht in der URL, eine
> Zeile Text statt eines Schalters bei `eigenschaftenAnzahl === 0` — all das gilt unverändert.
> Geändert hat sich ausschließlich, **was innerhalb des aufgeklappten Blocks steht**.

**Der Anlass.** In der flachen Liste stand `Converter.Log.GUID` zweimal und `Service.Type` dreimal
untereinander und sah aus wie eine Dublette. Es sind Einträge **verschiedener Prozessschritte**.
Künftig steht über jeder Gruppe der Schrittname aus der Zeitleiste; der Nutzer sieht damit, **in
welchem Schritt** ein Wert entstanden ist — dass etwa per OFTP empfangen und später per FTP versendet
wurde.

**Die gemessene Grundlage** ([`messungen-schritt5.md`](messungen-schritt5.md) **M17 3**, Fenster A):

| Befund | Zahl |
|---|---|
| Zeilen an `MessageActionID = 0` (Metadaten-Schritt) | 82.943 von 141.037 — **58,8 %**, 71 verschiedene Namen |
| Namen auf **genau einem** Schritt | 70 von 101 |
| Namen auf **mehr als einem** Schritt | **31** |
| Beleg, dass ein Name je Nachricht mehrfach vorkommt | `Converter.Payload.GUID`: 7.862 Zeilen auf 6.149 Nachrichten |
| Ganze `Message.*`-Familie | **ausnahmslos** Schritt 0 |
| Schritt ohne jede Eigenschaft | `MessageActionID = 502` — in `MessageAction` vorhanden, in `MessageProperty` nicht |

##### Der Gruppierungsschlüssel ist `MessageActionID` — und nicht `SOSActionID`

1. **`MessageProperty` hat keine `SOSActionID`** (M14) — nur `MessageActionID`. Über die
   Ablaufkennung zu gruppieren verlangte einen Join, den niemand braucht.
2. **Die beiden Kennungen sind nicht deckungsgleich.** M15 (1) misst `MessageActionID = 2` mal neben
   `SOSActionID` 1, mal neben 2, und `MessageActionID = 3` neben 2, 3 oder 10 — **614 von 20.352**
   Aktionen des Tagesfensters weichen ab.
3. **`MessageActionID` ist die Ausführung** und über den Primärschlüssel `(MessageID,
   MessageActionID)` je Nachricht eindeutig. `SOSActionID` ist der Schlüssel in die
   *Ablaufdefinition*; führte ein Ablauf denselben Definitionsschritt zweimal aus, verschmölzen beide
   Ausführungen zu einer Gruppe.

Im Frontend heißt das Feld auf beiden Seiten bereits `position` — an `Eigenschaft` wie an `Schritt`,
und beide Male steht `MessageActionID` dahinter (§1, §4). **Geprüft und nicht angenommen:**
`EigenschaftResponse.position` kommt aus `MessageProperty.MessageActionID`,
`SchrittResponse.position` aus `MessageAction.MessageActionID`.

##### Die fünf Entscheidungen

| # | Entscheidung |
|---|---|
| E1 | **Gruppiert wird im Frontend** aus den beiden vorhandenen Antworten. Kein Backend-Feld, keine neue Abfrage |
| E2 | **`position === 0` ist die erste Gruppe** und heißt **„Nachricht"** |
| E3 | **Flach:** eine Ebene, alle Gruppen offen, Überschriften dazwischen. Keine klappbaren Untergruppen — gemessen sind 22,6 Eigenschaften je Nachricht, Minimum 14, Maximum 38 (M17 1), und das wäre Mechanik für zwanzig Zeilen |
| E4 | **Gruppenkopf = Schrittname + Anzahl**, etwa `Datei konvertiert (7)`. Tooltip wie in der Zeitleiste |
| E5 | **Innerhalb einer Gruppe bleibt die Reihenfolge der Antwort** — die Gruppierung ist stabil und ordnet nicht um |

**Die Beschriftung „Nachricht" sagt, *wo* die Werte hängen — an der Nachricht statt an einem Schritt
— und behauptet nichts über ihren Inhalt.** „Metadaten der Nachricht" oder „Allgemeine Angaben"
wären ausdrücklich falsch: Dass dort *ausschließlich* die `Message.*`-Familie steht, ist **nicht**
gemessen. Gemessen ist die Gegenrichtung — die `Message.*`-Familie steht ausnahmslos dort (M17 3).

**Die Gruppenreihenfolge folgt `schritte[]` und wird nicht numerisch nachsortiert.** Die Ordnung der
Schritte (`MessageActionStart`, bei Gleichstand `MessageActionID`) steht laut §4 an **genau einer**
Stelle, nämlich im `ORDER BY` von `findeAktionen`. Eine zweite Sortierung in der Oberfläche wäre die
Drift, gegen die diese Regel gerichtet ist — und die Gruppen stünden in einer anderen Reihenfolge als
die Zeilen der Zeitleiste darüber. Positionen ohne gelieferten Schritt tragen `beschriftung = null`,
kommen ans Ende und werden dort aufsteigend nach Zahl geordnet; ihr Kopf trägt den Rückfall
*Schritt N*. **Kein erfundener Name.**

> **Belegvermerk** (Regel L10).
> *Gemessen (M15 1, Fenster A; S1 über 704.427 Aktionen beider Fenster, null Abweichungen):*
> `SOSActionID = 0` und `MessageActionID = 0` treffen dieselbe Menge.
> *Behauptet wird:* dass `position === 0` in der Oberfläche der Metadaten-Schritt ist.
> **Die Lücke:** Das Frontend sieht die `SOSActionID` gar nicht und verlässt sich auf diese Deckung.
> Sie ist belegt, aber sie ist eine Messung und keine Zusage des Schemas.

##### Der Tooltip des Gruppenkopfs ist derselbe wie in der Zeitleiste — und wird nicht nachgebaut

Name, `Baustein: <Rohwert>` und der Herkunftstext zu `DIREKT` / `HERGELEITET` / `ROHWERT`. Die
Zusammensetzung lag bis zu dieser Nacharbeit **inline** in `components/zeitleiste.tsx` und ist nach
`features/nachrichten/detail.ts` gezogen worden (`schrittHinweis`); beide Stellen rufen sie.
**Zwei Stellen, die denselben Schritt verschieden benennen, sind genau der Fehler, den diese
Gruppierung beseitigen soll.** Bei der Gruppe „Nachricht" gibt es keinen Schritt und deshalb auch
keinen Tooltip statt eines leeren.

**Der React-Schlüssel ist `${position}:${name}`.** Der Primärschlüssel ist `(MessageID,
MessagePropertyName, MessageActionID)`; innerhalb einer Gruppe ist der Name damit eindeutig, über
Gruppen hinweg **nicht**. Ein Schlüssel aus dem Namen allein brächte die `console.error`-Warnungen
aus Schritt 6 zurück — sichtbar falsch wäre nichts.

**Keine eigene Farbe, keine Animation, kein Übergang** ([`visuelles-konzept.md`](visuelles-konzept.md)
§7): Die Überschrift ist eine Beschriftung und keine Statusaussage, und sie nutzt denselben
gedämpften Ton wie die Beschriftung im BAM-Block. **Kein eigener Scrollbereich** — es bleibt beim
einen senkrechten Scroller ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7).

##### Nachtrag vom 18.08.2026 — die Gruppen sind aus der Zeitleiste anspringbar

**Kein Satz des Abschnitts darüber ist falsch geworden**, und die Gruppierung selbst ist nicht
angefasst: dieselbe Einteilung über `position`, dieselbe Reihenfolge, dieselben Köpfe mit demselben
Tooltip. Ergänzt ist allein **der Weg hierher**.

Ein Klick auf einen Schritt in der Zeitleiste (§10.4) klappt den Block auf und setzt den **Fokus**
auf den Abschnitt dieses Schritts. Fokus und kein Bildlauf: Er bewegt die Ansicht ebenso, nimmt aber
die Tastatur mit — ein Bildlauf ohne Fokus ließe ein Vorleseprogramm dort stehen, wo es war. Jede
Gruppe trägt dafür eine `id` und `tabIndex={-1}`; gibt es zu dem Schritt keine Gruppe — gemessen
möglich, `MessageActionID = 502` steht in `MessageAction` und fehlt in `MessageProperty` (M17 3) —,
bekommt der Bereich selbst den Fokus.

**Aufgeklappt wird beim Rendern und nicht in einem Effekt** (`react-hooks/set-state-in-effect`); der
Fokus dagegen ist einer, weil er das Dokument ändert und erst laufen kann, wenn die Gruppen im Baum
stehen. Ausführlich in [`rohdaten-frontend.md`](rohdaten-frontend.md) §3a.

##### Unberührt bleiben Zeitleiste, Kettenblock, BAM-Block und Kopf

Ausdrücklich: An §10.3 (Kopf), §10.4 (Zeitleiste), §10.4a (Kettenblock) und §10.4b (BAM-Block)
ändert sich **nichts**. Die Zeitleiste hat eine Zeile Code abgegeben — die Zusammensetzung ihres
Tooltips — und zeigt danach dasselbe wie vorher. Auch die Nachrichtenliste ist nicht berührt.

> **Das gilt für die Nacharbeit vom 17.08.2026.** Am 18.08.2026 hat die Zeitleiste die beiden
> Ergänzungen aus §10.4 bekommen — die Ziele und den Weg hierher. Auch dort ist an ihrer Rechnung,
> ihrer Sortierung und ihren Zeilen nichts geändert.

**Ebenso wenig geändert:** keine Übersetzung, Deutung oder Umbenennung von Eigenschaftsnamen — sie
bleiben Rohwerte. Kein Filter, keine Suche, keine Sortierumschaltung im Block. Und **keine Deutung,
welcher Schritt „empfängt" oder „versendet"**: Der Schrittname kommt aus dem Ablauf; was er bedeutet,
sagt das Werkzeug nicht dazu (Regel Q4).

##### Es war keine Messung fällig, weil keine Abfrage entstanden ist

Regel L7 verlangt, dass **jede neue Abfrage** vor dem Merge gegen die Testkopie gemessen wird. Hier
entsteht keine: Beide Datensätze — Detail und Eigenschaften — werden in derselben Ansicht schon heute
geladen, mit denselben zwei Aufrufen. Die Änderung ordnet an, was bereits im Baum liegt. **Kein
Statement im Diff, kein neues Feld, kein neuer Endpunkt.**

> **Befund zu Aufgabe 0, gemeldet und nicht verändert.** Das Statement der Eigenschaften
> (`NachrichtendetailRepository.findeEigenschaften`) **trägt** ein `ORDER BY`, nämlich
> `MessagePropertyName ASC, MessageActionID ASC` — also die Reihenfolge des Primärschlüssels, ohne
> Sortierlauf. Damit ist die heute sichtbare alphabetische Ordnung eine **Zusage des Backends** und
> nicht nur eine Beobachtung, und E5 („innerhalb einer Gruppe bleibt die Reihenfolge der Antwort")
> steht auf festem Grund. **Am Statement ist nichts geändert worden**, und im Frontend ist
> ausdrücklich **kein** Ersatzsortierer gebaut.

##### Sichtprüfung im Browser (17.08.2026)

Gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Rolle ADMIN — **geklickt, nicht
zugewiesen**. Geprüft an **beiden** Einhängepunkten, im Panel neben der Liste und auf der eigenen
Route. Die Bezugsnachricht ist nach ihrer **Gestalt** gewählt und ihre `MessageID` steht wie in §8
bewusst nicht hier: eingehende Nachricht mit **drei Schritten** und **36 Eigenschaften**, per OFTP
empfangen und per FTP weitergereicht.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | Gruppenreihenfolge gegen die Zeitleiste | Zeitleiste *X12 EDICs Router → ANSI X12 2 Text → APIMS Transfer*; Gruppen *Nachricht → X12 EDICs Router → ANSI X12 2 Text → APIMS Transfer*. **Gleiche Reihenfolge, wortgleiche Namen** |
| 2 | Die scheinbaren Dubletten | `Service.Type` steht **viermal** — je einmal in jeder Gruppe, mit **vier verschiedenen Werten**. `Converter.Log.GUID`, `Converter.Payload.GUID` und `Converter.TransactionID` stehen **je zweimal**, in zwei verschiedenen Gruppen und mit verschiedenen Werten |
| 3 | Summe gegen den Blockkopf | 17 + 10 + 5 + 4 = **36**, und im Kopf steht *Technische Eigenschaften (36)* |
| 4 | Netzanfragen beim Laden des Details | genau zwei — Kopf und `/kette`. **Keine auf `/eigenschaften`** |
| 5 | Netzanfragen nach dem Klick auf die Überschrift | **genau eine** auf `/eigenschaften` |
| 6 | Tooltip von Gruppenkopf und Zeitleistenzeile | beide *„X12 EDICs Router / Baustein: NXS_EDICSROUTER / Name aus der Ablaufdefinition"* — **zeichengleich**, weil sie aus derselben Funktion kommen |
| 7 | Konsole | **keine Meldung**, obwohl vier gleichnamige Einträge im Baum stehen — der Schlüssel `${position}:${name}` trägt |

**Nicht zu sehen war der Rückfall *Schritt N*.** Diese Nachricht hat zu jeder Position einen
gelieferten Schritt. Der Zweig ist ausschließlich in `tests/eigenschaften-block.test.tsx` belegt —
gebaut und unit-geprüft, nicht gegen echte Daten. Dieselbe Lage wie bei `WARTET_VOR` in §10.12.

> **Der Befund, der die Wortwahl aus §7 bestätigt — und der Belegvermerk dazu.**
>
> In der Gruppe **„Nachricht"** stehen bei dieser Nachricht **nicht nur** `Message.*`-Einträge:
> Neben den neun `Message.*` hängen dort sieben `OFTPReader.*`-Einträge und ein `Service.Type`.
> Hätte die Gruppe „Metadaten der Nachricht" oder „Allgemeine Angaben" geheißen, stünde eine
> Behauptung über den Inhalt darüber, die schon an der ersten geöffneten Nachricht falsch gewesen
> wäre. Die gewählte Beschriftung sagt, **wo** die Werte hängen, und behauptet nichts darüber, was
> sie sind.
>
> *Gemessen:* **eine** Nachricht in der Testkopie, gesichtet am 17.08.2026 — dort trägt
> `MessageActionID = 0` neben der `Message.*`-Familie auch die `OFTPReader.*`-Familie.
> *Behauptet wird:* nur, dass die verworfene Formulierung **widerlegt** ist. Ein einziger Gegenbeleg
> genügt dafür, und mehr wird daraus nicht abgeleitet.
> **Die Lücke:** Wie sich Schritt `0` über den Bestand zusammensetzt, ist **nicht** gemessen. M17 (3)
> misst die Gegenrichtung — die `Message.*`-Familie steht ausnahmslos dort — und beziffert 71
> verschiedene Namen auf Schritt `0`, ohne sie aufzuschlüsseln.

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

### 10.7 Nebeneinander, am schmalen Fenster — und der Umschalter dazwischen

> **Ergänzt am 11.08.2026** (Nachbesserung 1 zu Schritt 6). Bis dahin beschrieb dieser Abschnitt
> ausschließlich die zwei Einhängepunkte **nach Umbruchpunkt** — welcher von beiden erscheint, war
> allein eine Folge der Fensterbreite. Das bleibt richtig und steht unverändert unten; neu ist, dass
> der Nutzer ab `xl` selbst zwischen ihnen wechseln kann. **Kein Satz der alten Fassung ist falsch
> geworden**, es fehlte einer.

> **Ergänzt am 13.08.2026 (Schritt 7, Teil 3): Es gibt einen dritten Einhängepunkt, und dort
> erscheint der Umschalter nicht.**
>
> Die Trefferliste der Belegsuche öffnet **dieselbe Komponente** über denselben Parameter
> `nachricht` ([`bam-suche.md`](bam-suche.md) §11.4) — sie ist wiederverwendet und nicht
> nachgebaut. **Was dort fehlt, ist der Umschalter**, und zwar nicht aus Platzgründen: Sein Ziel
> ist die eigene Route, und deren Rückweg führt über `ansichtNebenListe` an die **Liste**
> (`lib/routen.ts`) — nicht an die Suche. Ein Umschalter, dessen Rückweg woanders endet als dort,
> wo er herkam, ist keiner.
>
> Umgesetzt als **freiwillige Angaben**: `umschaltenZu` und `aufUmschalten` sind seitdem optional,
> und ohne sie erscheint der Knopf nicht. **An den beiden bestehenden Einhängepunkten ändert sich
> nichts** — sie geben beides weiterhin mit. Ob die Suche je einen eigenen Rückweg bekommt, ist
> nicht entschieden und steht als offener Punkt in [`bam-suche.md`](bam-suche.md) §13.

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

#### Warum es den Umschalter gibt: das Panel wird relativ schmaler, je breiter das Fenster ist

Das Panel hat eine **feste** Breite in `rem`, die Liste bekommt den Rest. Gemessen am 11.08.2026
gegen die laufende Anwendung, Fenster 1920 × 889:

| Fensterbreite | Inhaltsbereich (`main`) | Panel | Anteil |
|---|---:|---:|---:|
| 1280 px (`xl`) | 1072 px | 26 rem = 416 px | 39 % |
| **1920 px (`2xl`)** | **1697 px** | **30 rem = 480 px** | **28,3 %** |

> **Belegvermerk** (Regel L10).
> *Gemessen:* die Zeile für 1920 px — `main.clientWidth = 1697`, Panelbreite `480`.
> *Behauptet wird:* dieselbe Bewegung auch bei 1280 px.
> **Die Lücke:** Die 1280er-Zeile ist **gerechnet, nicht gemessen** (1280 − 208 px Navigationsspalte
> ohne Abzug für die Bildlaufleiste); bei 1920 px liegt der gemessene Wert aus demselben Grund
> 15 px unter dem gerechneten. Für den Schluss genügt das: Der Zähler ist konstant, der Nenner
> wächst mit dem Fenster — die Richtung hängt an keiner der beiden Zahlen.

Ein Klick auf eine Zeile setzt den Fokus ins Panel, der optische Schwerpunkt bleibt aber auf der
Liste. **Am großen Monitor — dort, wo das Werkzeug betrieben wird — ist das Missverhältnis am
größten.**

#### Der Umschalter

| Einhängepunkt | Beschriftung | Ziel |
|---|---|---|
| Panel (`/nachrichten?nachricht=<id>&…`) | „Ohne Liste anzeigen" | `/nachrichten/<id>?<Abfragezeichenkette ohne `nachricht`>` |
| eigene Route (`/nachrichten/<id>?…`) | „Neben der Liste anzeigen" | `/nachrichten?nachricht=<id>&<Abfragezeichenkette>` |

**Es entsteht kein neuer Mechanismus, kein neuer Zustand und keine neue Route.** Beide Ziele gibt es
seit Schritt 5; gebaut ist der Weg dazwischen. **Der Modus *ist* die Route** — kein `localStorage`,
kein Cookie, kein Kontext, kein zusätzlicher Suchparameter. Daraus folgt zweierlei von selbst: Der
Zurück-Knopf führt Schritt für Schritt zurück, weil beide Wege echte Navigationen sind, und der Modus
überlebt das Öffnen eines Kettenglieds, weil [`verkettung.md`](verkettung.md) §8.6 auf der eigenen
Route ohnehin auf dieselbe Route führt. **Dafür ist nichts gebaut worden.**

- **Der Schließen-Knopf bleibt in beiden Modi unverändert**, der Umschalter tritt **neben** ihn.
  Zwei Knöpfe, zwei Aussagen: *diese Nachricht anders zeigen* gegen *diese Nachricht schließen*.
- **`nachricht` wird beim Maximieren aus der Abfragezeichenkette entfernt** — sonst stünde die
  Kennung zweimal im Ziel, einmal im Pfad und einmal als Parameter. Beim Verkleinern wird sie
  gesetzt. Alles andere bleibt **unverändert und in seiner Reihenfolge** stehen.
- **Die Entscheidung ist die Zielroute**, und die steht als reine Funktion in `lib/routen.ts`
  (`ansichtOhneListe`, `ansichtNebenListe`), nicht als Ausdruck in einer Komponente. Sie führt die
  Abfragezeichenkette **roh** weiter, statt sie über `URLSearchParams` neu aufzubauen: Jene Klasse
  kodierte `von=2026-07-08T00:00:00Z` zu `…T00%3A00%3A00Z` um. Gleichwertig ist nicht unverändert,
  und in einer geteilten URL sieht man den Unterschied.
- **Derselbe Mechanismus wie beim bestehenden Schließen:** `window.location.search` wird **im
  Ereignis** gelesen, kein `useSearchParams`, keine neue Suspense-Grenze (§10.6). Der Preis ist, dass
  der Umschalter **kein Mittelklick-Ziel** ist und sich nicht in einem neuen Tab öffnen lässt. Das
  wird bewusst getragen — Konsistenz mit dem vorhandenen Weg wiegt hier schwerer, und die Ansicht ist
  über die Adresszeile weiterhin vollständig erreichbar und teilbar.
- **Unter `xl` erscheint er nicht.** Dort füllt die Detailansicht ohnehin die Stelle der Liste; ein
  Schalter, der nichts Sichtbares ändert, verspricht etwas, das er nicht hält. Umgesetzt über die
  Klassen (`hidden xl:inline-flex`) und **nicht** über eine Abfrage der Fensterbreite in JavaScript —
  die wäre ein zweiter Umbruchpunkt neben dem der Ansicht, und zwei laufen auseinander.
- **Anfassbarkeit wie der Kopierknopf der `MessageID`:** `button`, `aria-label` **und** `title`,
  Fokusring, `Enter` und `Leertaste`. Zeichen aus `lucide-react` (`Maximize2` / `Minimize2`). **Keine
  Farbe in der Komponente, keine Animation, kein Übergang** ([`visuelles-konzept.md`](visuelles-konzept.md) §7).

#### Die eigene Route nutzt die volle Inhaltsbreite

**Das war bereits so.** Vor der Nachbesserung festgestellt und hier festgehalten, damit es niemand
für neu hält: `nachricht-seite.tsx` trägt seit Schritt 5, Teil 2 die Klasse `max-w-inhalt` — also
`--dichte-inhaltsbreite`, 72 rem. Die Route hat den Panel-Baustein nie in 26 rem gezeigt.

Gemessen am 11.08.2026, Fenster 1920: Ansichtsbreite **1152 px** (= 72 rem bei 16 px Grundschrift),
linke Kante bei **x = 228** — also bündig mit Listenkopf und Panelkopf (`main` beginnt bei 208, dazu
20 px Innenabstand). **Linksbündig, nicht zentriert**, und das ist die Entscheidung: Beim Umschalten
springt der Inhalt dadurch nicht seitwärts, sondern wird nur breiter. Zentriert läge die Kante bei
x = 488.

> ⚠️ **Die 72 rem sind gewählt, nicht gemessen.** Das Token ist in
> [`visuelles-konzept.md`](visuelles-konzept.md) §5 für **Fließtext** begründet — längere Zeilen sind
> schwer zu lesen —, und die Detailansicht ist keiner. Es gibt keine Messung, die eine andere Zahl
> trägt, und für diesen Zweck wird auch keine erfunden. Der Wert steht hier, weil ein vorhandenes
> Token besser ist als eine zweite frei gewählte Zahl daneben.

**Eine Spalte, keine Umverteilung.** Die Reihenfolge der Blöcke bleibt: Kopf, Kettenblock,
Zeitleiste, technische Eigenschaften. Eine zweispaltige Anordnung wäre ein Entwurf und kein
Breitenwechsel. **Kein eigener Scrollbereich** — es bleibt beim einen senkrechten Scroller aus
[`frontend-grundlagen.md`](frontend-grundlagen.md) §7.

> **Die Zeitleiste bleibt senkrecht**, obwohl §10.4 sie „weil das Panel schmal ist" so begründet. Die
> Begründung gilt weiterhin für den Modus, in dem die Ansicht meistens steht; eine Leiste, die je
> nach Einhängepunkt ihre Richtung wechselt, wäre zwei Leisten.

#### Der Preis des Umschaltens, und wer ihn zahlt

**Der Weg von der eigenen Route zurück zur Liste kostet eine Listenabfrage.** Der
`display: none`-Trick oben hält den Zustand nur *innerhalb* einer Seite; die eigene Route ist eine
andere. Das ist die bekannte Eigenschaft dieser Route und **keine neue** — sie gilt für den
Schließen-Knopf seit Schritt 5 genauso. Der Umschalter macht sie nur häufiger sichtbar. Die
Gegenrichtung ist billiger: Wer maximiert, hängt die Liste aus und fragt sie nicht.

### 10.8 Was die Oberfläche bewusst nicht zeigt

- **Keine Deutung des Timeouts.** `timeoutSekunden` kommt je Schritt mit und wird **nicht**
  angezeigt. 124 von 14.063 beendeten Schritten überschreiten ihre Frist, und zwar um mehr als das
  Achtundvierzigfache (§4) — ob das dieselbe Kennzeichnung ist wie die Problemkategorie „Überfällig"
  aus Regel Q3 oder eine andere Ebene, ist offen (Frage 7 in `messungen-schritt5.md`). Eine
  Kennzeichnung zu erfinden, bevor die Frage beantwortet ist, hieße die Antwort vorwegzunehmen.
- ~~**Keine Verkettung.**~~ **Erledigt in Schritt 6, Teil 2b** (§10.4a): Der Kettenblock steht
  zwischen Kopf und Zeitleiste, und ein Klick auf ein Glied öffnet dessen Detail. **Die
  Aufteilungszahl aus den kuratierten Eigenschaften verlinkt weiterhin zu nichts** — sie ist ein
  `MessageProperty`-Wert und keine Verkettung; was tatsächlich an der Nachricht hängt, steht im
  Block darunter und kommt aus den vier Spalten von `Message`.
- **Kein Rohdaten-Download** — das ist Schritt 8.
- **Kein Gerüst geplanter Schritte** — die Entscheidung samt Zahlen steht in §11 (M21).

### 10.9 Tests

| Datei | Was |
|---|---|
| `tests/nachrichtendetail.test.ts` | die Normierung des Balkens (längster voll, Mindestbreite, alles unter einer Sekunde, keine Dauer → kein Balken); **dass zwischen zwei Schritten keine Zeile mehr entsteht**, auch bei drei Stunden Abstand nicht; die sechs offenen Zustände samt `WARTET_IN` gegen `WARTET_VOR` und ohne benannten Schritt; **dass `EMPFANGEN` und `OHNE_AKTION` der Leiste nichts anhängen** — der Metadaten-Schritt wird auch dort keine Zeile; **dass ein zufällig gleicher Name die Wortwahl nicht mehr ändert**; dass bei `LAEUFT_AUF` nichts angehängt und nichts weggelassen wird; die Wartezeile mit Dauer, Frist, Kategorie, **ihr Erscheinen bei `EMPFANGEN` mit dem Verb des Wartens** und ihre Abwesenheit ohne Wartedauer; `bedeutungNichtVerifiziert` als abgeleiteter Wert |
| `tests/nachrichtenfilter.test.ts` | `nachricht` steht in der URL und lässt sich wieder einlesen; **taucht in keiner Abfrage der Liste auf** und lässt deren Abfrageschlüssel unverändert; Schließen lässt den übrigen Filterzustand stehen; eine leere Kennung ist keine Auswahl |
| `tests/format.test.ts` | `formatiereDauer` mit höchstens zwei Einheiten, „< 1 s" statt „0 s", nie eine negative Dauer, Bausteine aus der aktiven Sprache |
| `tests/sprachdateien.test.ts` | unverändert — beide Sprachdateien tragen den neuen Abschnitt vollständig |
| `tests/routen.test.ts` *(11.08.2026)* | die beiden Zielrouten des Umschalters (§10.7): leere Abfragezeichenkette in beide Richtungen; jeder Filter unverändert und in seiner Reihenfolge; `nachricht` entfernt beziehungsweise gesetzt und im Ziel **genau einmal**, auch wenn es am Anfang oder am Ende stand; eine Kennung mit Sonderzeichen einmal und nicht doppelt kodiert; und dass `NACHRICHT_PARAMETER` denselben Parameter meint wie `NACHRICHTEN_PARAMETER` |
| `tests/ansicht-umschalter.test.tsx` *(11.08.2026)* | **gerenderter Baum, begründete Ausnahme:** dass der Knopf `hidden xl:inline-flex` trägt und `inline-flex` **nicht** stehen bleibt, und dass er im Panel und auf der eigenen Route verschiedene Beschriftungen führt — je in `aria-label` **und** `title` |
| `tests/bam-block.test.tsx` *(12.08.2026)* | **gerenderter Baum, begründete Ausnahme:** derselbe Wert unter zwei Typen **ohne `console.error`**; bei `bamAnzahl === 0` **nicht im Baum und keine Anfrage**; eingeklappt mit Werten die Überschrift mit der Zahl und **immer noch keine Anfrage**. Vollständig in [`bam-werte.md`](bam-werte.md) §11 |
| `tests/nachrichtendetail.test.ts` — Gruppierung *(17.08.2026)* | zehn Fälle zu `gruppiereEigenschaften` (§10.5): Gruppe `0` vorn, auch wenn sie in der Eingabe nicht zuerst steht; **die Gruppenreihenfolge folgt `schritte[]` und nicht der Zahl** (`[3, 1, 2]` ergibt `0, 3, 1, 2`); ein Schritt ohne Eigenschaften erzeugt keine Gruppe; Positionen ohne Schritt landen ohne Beschriftung am Ende, aufsteigend; ohne `position === 0` entsteht keine leere Gruppe „Nachricht"; derselbe Name bleibt in zwei Gruppen zweimal stehen; innerhalb einer Gruppe wird nicht umsortiert; **die Invariante** Summe der Gruppengrößen = Länge der Eingabe; `gekappt` und `originalLaengeBytes` überstehen die Gruppierung; leere Eingabe → leere Liste |
| `tests/eigenschaften-block.test.tsx` *(17.08.2026)* | **gerenderter Baum, begründete Ausnahme:** derselbe Name in **drei** Gruppen **ohne `console.error`** (der Schlüssel ist `${position}:${name}`); bei `anzahl === 0` **kein Schalter und keine Anfrage**; eingeklappt mit Werten die Überschrift mit der Zahl und **immer noch keine Anfrage**; ohne gelieferte `schritte` trägt jede Gruppe den Rückfall *Schritt N* |
| `tests/zeitleiste-ziele.test.tsx` *(18.08.2026)* | **gerenderter Baum, begründete Ausnahme:** die Ergänzungen aus §10.4 und §10.5 — welche Zeile welches Ziel trägt (beide Arten, nur eine, keine); dass die Artefakte des **Metadaten-Schritts** über der Leiste erreichbar bleiben, **ohne dass die Leiste eine vierte Zeile bekäme**; die Belastungsprobe aus M55 mit fünfzehn eigenen Zielen ohne doppelten React-Schlüssel; das **Anspringen** der Eigenschaftengruppe samt der drei Fälle bei kaltem Zwischenspeicher; und dass ohne Eigenschaften **kein Schalter** am Schrittnamen steht. Vollständig in [`rohdaten-frontend.md`](rohdaten-frontend.md) §3 und §3a |

Kein gerenderter Baum, mit den Ausnahmen aus `tests/detail-baum.test.tsx` und
`tests/ansicht-umschalter.test.tsx`: Geprüft werden die **Entscheidungen**, nicht das Markup
([`frontend-grundlagen.md`](frontend-grundlagen.md) §9).

> **Warum der Umschalter eine Ausnahme rechtfertigt.** Seine Sichtbarkeitsregel *ist* eine Klasse —
> sie steht bewusst nicht in JavaScript (§10.7). Und der Umbruchpunkt, an dem sie greift, ist von
> Hand nicht zu prüfen: Die Browsersteuerung kann das Fenster nicht verkleinern
> ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7). Was über die Klassen belegbar ist, wird
> deshalb dort belegt, wo es belegbar ist.

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
ist, schlicht ein Widerspruch.

> **Nachgebessert am 10.08.2026, und zwar an der richtigen Stelle.** Die erste Fassung ließ die
> Oberfläche zwei gelieferte Felder über den **Namen** vergleichen und daraus die Wortwahl
> bestimmen. Das war eine Ableitung, die dort nichts zu suchen hat — und über den Namen war sie
> zusätzlich angreifbar: Zwei Schritte desselben Ablaufs können gleich heißen.
>
> [M29](messungen-schritt5.md#m29--worauf-zeigt-messagesosactionid-bei-wartenden-nachrichten) hat
> zuerst gemessen, wie oft die Beobachtung gilt: **538 von 538**, und der Gegenfall **null Mal**.
> Daraufhin ist der Wartezustand im Backend aufgeteilt worden — `WARTET_IN` gegen `WARTET_VOR`,
> verglichen über die **Kennungen** (§3). Die Oberfläche vergleicht seither gar nichts mehr; sie
> stellt den gelieferten Zustand dar.

**3. Mit der Tastatur ging Öffnen, aber Schließen nur mühsam.** Der Schließen-Knopf steht im DOM
hinter der Tabelle — man hätte durch bis zu fünfzig Zeilen tabben müssen. Das erfüllt „erreichbar"
und verfehlt „bedienbar". **`Escape` schließt das Panel jetzt**, mit zwei Ausnahmen, damit die Taste
nicht zweierlei tut: in einem Eingabefeld und bei einem offenen Auswahlfeld bleibt sie, was sie ist.
Ein Fokussprung ins Panel wäre die Alternative gewesen und ist verworfen — er nähme dem Nutzer die
Stelle in der Liste, an der er gerade war, und einem Mausnutzer, der nichts davon wollte, ebenso.

> **Nachgezogen am 10.08.2026: `Escape` wirkt auch auf `/nachrichten/<id>`** und führt dort zurück
> zur Liste — dieselbe Wirkung wie der Schließen-Knopf, mitsamt der Abfragezeichenkette. Die bis
> dahin offene Notiz lautete, auf der eigenen Route sei der Schließen-Knopf ohnehin der erste
> Tabstopp, ein Kürzel also entbehrlich. Das stimmt und macht die Taste trotzdem nicht falsch: Wer
> die Ansicht im Panel mit `Escape` schließt und denselben Beleg später über einen geteilten Link
> öffnet, drückt dieselbe Taste und erwartet dasselbe. **Eine Taste, die je nach Einhängepunkt wirkt
> oder nicht, lernt niemand.**
>
> Die Regel steht seither samt ihren zwei Ausnahmen in **einem** Hook (`useEscapeSchliesst`) statt in
> zwei `useEffect`. Ein zweiter Abzug derselben Bedingungen wäre die Stelle, an der eine davon
> irgendwann fehlt — und dann räumt `Escape` in einem Suchfeld nicht mehr die Eingabe, sondern
> schließt die Ansicht.

### 10.12 Was die Testkopie nicht hergibt

**Jeder Punkt nennt seinen Grund** — Versäumnis oder Unmöglichkeit. Das ist der Unterschied, auf den
es hier ankommt: Nach einem Versäumnis sucht man einen Testfall, nach einer Unmöglichkeit nicht.

| | Warum | Art |
|---|---|---|
| **`LAEUFT_AUF`** | siehe unten — **in der Testkopie nachweislich unbeobachtbar** | Unmöglichkeit |
| **`WARTET_VOR`** | M29: **0 von 538**. Der Verweis zeigt bei jeder wartenden Nachricht auf den zuletzt gelaufenen Schritt | Unmöglichkeit im heutigen Bestand |
| **`EMPFANGEN`** | tritt im Produktivbetrieb bei eingehenden, noch nicht weitergelaufenen Nachrichten auf. In der Testkopie ist der Bestand abgeschnitten, und `RUNNING` kommt null Mal vor — dort steht nichts mehr am Anfang seiner Verarbeitung | Unmöglichkeit im heutigen Bestand |
| **`OHNE_AKTION`** | M16 (3) hat `ohne_jede_aktion = 0` über die geprüften Status gemessen. **Nicht widerlegt, nur nicht beobachtet** — `MessageAction` kennt keinen Zwang, der den Fall ausschlösse | nicht beobachtet |
| **Die Kappung** | der größte gemessene Wert liegt bei 12.732 Byte, die Grenze bei 16.384 (§6). Eine Grenze, die auf der Testkopie griffe, wäre zu niedrig gewählt | Unmöglichkeit von Natur aus |
| **`ueberfaellig` als Kategorie** | über den Gesamtbestand sind *alle* 538 offenen Zeilen überfällig, im 24-h-Fenster genau eine — beides sagt nichts darüber, ob die Frist fachlich richtig gewählt ist ([`message-status.md`](message-status.md)) | Unmöglichkeit |

#### `LAEUFT_AUF` ist nicht ungetestet, sondern unbeobachtbar

Das ist keine Ausrede, sondern eine Kette von drei Messungen:

1. **`RUNNING` kommt in der Testkopie null Mal vor** ([`message-status.md`](message-status.md)).
2. **Bei allen 538 `SUSPENDED` ist jede Aktion beendet** (M16 3). Es gibt in der Testkopie also
   **keine offene Nachricht mit einer offenen Aktion**.
3. **Die 95 offenen Aktionen aus M22 helfen nicht.** 49 davon liegen im Fehlerzustand
   `ERROR_TIMEOUT`, die übrigen 46 auf `FINISHED` und `CHECKED` — nach der Definition in §3 ist
   keine dieser Nachrichten *offen*, und alle ergeben `KEINER`.

**Es gibt also keinen Datensatz, an dem sich `LAEUFT_AUF` vorführen ließe** — nicht weil niemand
gesucht hätte, sondern weil es ihn nicht gibt. Das steht hier als Begründung und nicht als
Versäumnis; sonst sucht in drei Monaten jemand nach einem Testfall, den es nicht geben kann.

#### `WARTET_VOR` ist gebaut, unit-getestet und nie gesehen

M29 hat über **alle 538** wartenden Nachrichten gemessen — `n = 538`, das ist der gesamte Bestand
dieses Status —, und der Fall »zeigt auf einen anderen, noch nicht ausgeführten Schritt« kommt
**null Mal** vor. Er ist damit nicht widerlegt, sondern nicht beobachtet.

**Und die Fallzahl trägt weniger, als sie aussieht.** Die 538 sind *eine* Gestalt: ausnahmslos zwei
echte Schritte (M29 1), ausnahmslos dieselbe `SOSActionID` in demselben Ablauf (M29 4), sieben Tage.
`n = 538` ist die Zahl der Zeilen, nicht die Zahl der Fälle. Dazu fehlt der Status, der es zeigen
würde: `RUNNING` kommt null Mal vor. Genau deshalb bleibt der Zweig gebaut und wird nicht
wegoptimiert.

#### `EMPFANGEN` und `OHNE_AKTION` sind gebaut, unit-getestet und lokal nicht zu sehen

*Beide neu am 10.08.2026, aus der Aufteilung von `OHNE_SCHRITT` (§3).* Sie stehen hier **mit
Begründung und nicht als Versäumnis** — dieselbe Formulierung wie bei `LAEUFT_AUF`.

**`EMPFANGEN` ist fachlich beantwortet worden, nicht gemessen.** Ursprünglich war für diesen Nachtrag
eine Messung vorgesehen (kommt eine Nachricht mit ausschließlich dem Metadaten-Schritt vor?). Sie ist
**entfallen**, weil der Auftraggeber die Frage fachlich beantwortet hat: **Im Produktivbetrieb tritt
der Fall auf**, sobald Nachrichten eingehen und noch nicht weitergelaufen sind. Die Testkopie hat
eine Schnittkante und kennt kein `RUNNING`; ihre Leere hat in diesem Projekt bereits dreimal nichts
bedeutet. Der Zweig wird deshalb gebaut, unabhängig davon, ob er lokal vorführbar ist — genau wie
`LAEUFT_AUF`. Die freigehaltene Messnummer ist in
[`messungen-schritt5.md`](messungen-schritt5.md) vermerkt, damit sie nicht als verlorene Messung
gelesen wird.

**`OHNE_AKTION` ist gemessen und null Mal gefunden worden** (M16 3, `ohne_jede_aktion = 0` über
`SUSPENDED`, `ERROR_DUPLICATE`, `COMMIT_REJECTED` und `ERROR_TIMEOUT`). Er ist damit **nicht
widerlegt, nur nicht beobachtet**: `MessageAction` kennt keinen Zwang, der eine Nachricht ohne jede
Aktion ausschlösse, und die Detailansicht hält ihn deshalb aus.

**Was sich lokal sehr wohl prüfen lässt, ist die Gegenrichtung:** dass die bestehenden Zustände
unverändert erscheinen. Das steht in §10.14.

#### Die Lückenzeile — gebaut und wieder entfernt

**Sie steht hier, damit niemand sie in einem halben Jahr erneut vorschlägt, ohne den Grund zu
finden.**

Gebaut war: eine eigene Zeile zwischen zwei Schritten, sobald der Abstand `LUECKE_SCHWELLE_SEKUNDEN`
= 60 überschritt, mit Symbol, Text und Unit-Tests in beide Richtungen.

Gemessen ist: **Über rund 700 geprüfte Nachrichten des dichten Tages und der Woche davor beträgt die
größte Lücke zwischen zwei Schritten eine Sekunde.** Die Zeile ist nie erschienen.

**Das war kein knapper Fehlschlag, den eine niedrigere Schwelle geheilt hätte, sondern
strukturell:** Die Wartezeit steckt in der **Dauer des `WAITUNTIL`-Schritts**, nicht im Zwischenraum
zwischen zwei Schritten. Eine wartende Nachricht steht *in* ihrem letzten Schritt (§3, M29) — sie
liegt nicht zwischen zweien. Eine Schwelle von fünf Sekunden hätte die Leiste bei jedem
Schrittwechsel verlängert und wäre trotzdem nicht die Antwort auf die Frage gewesen, die ein Nutzer
stellt.

**Ersetzt ist sie durch zwei Dinge**, die dieselbe Frage tatsächlich beantworten: die **Wartezeile**
am offenen Zustand (§10.4) und `gesamtdauerSekunden` im Kopf (§3a). Der zweite deckt genau den Fall
ab, für den die Lückenzeile gedacht war: Endet Schritt 3 und beginnt Schritt 4 drei Stunden später,
passt die Summe der Schrittdauern nicht zur Gesamtdauer — und man sieht es.

**Das ist kein Versäumnis der Prüfung, sondern die Gestalt des Bestands** — und es steht hier, damit
niemand die grüne Testliste für eine Vorführung hält. Was sich vorführen ließ, ist in §10.10 und
§10.13 aufgezählt.

### 10.13 Sichtprüfung nach der Nachbesserung (10.08.2026)

Gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Rolle ADMIN, Fenster 1920 × 889.
**Geklickt und getippt, nicht zugewiesen.** Die Anwendung ist vor der Prüfung **neu gestartet**
worden — ein laufendes Backend kennt neue Antwortfelder nicht, und eine Prüfung gegen die alte
Instanz bewiese nichts.

**Die Stichprobe ist nach Gestalt gewählt, nicht nach Aktualität:** Zeitfenster 7 Tage, Status
`Wartend` über die Filterleiste geklickt. Von den wartenden Nachrichten fällt genau eine aus der
Reihe — sie liegt am 29.12. um 12:37, alle übrigen am 24.12. um 06:56. Diese eine ist geprüft.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | **Der Zustand kommt als `WARTET_IN`** | `offenerZustand: "WARTET_IN"`, `naechsterSchritt: "Send Message to Pool"` |
| 2 | Der Name wird nicht wiederholt | Unter „Send Message to Pool · 2 min" steht *„Die Nachricht wartet — von selbst geht es hier nicht weiter."* — kein zweites Mal derselbe Name |
| 3 | Der Verweis bleibt im Tooltip | `title` der Zeile: „Der Ablauf verweist auf: Send Message to Pool" |
| 4 | **Der Rohwert bestätigt M29 an dieser Nachricht** | `title` des letzten Schritts: `NXS_MERGE\|BMW\|WAITUNTIL\|now+170H@…\|SUSPEND` — der Schritt, der sie schlafen legt, ist derselbe, auf den der Verweis zeigt |
| 5 | **Die Wartezeile** | *wartet seit 15 h 35 min · Frist 30 min · ⚠ Überfällig* |
| 6 | Die Frist ist in **Sekunden** gelesen | `fristSekunden: 1800` → angezeigt „30 min". Unter der Minuten-Lesart stünde dort „30 h" |
| 7 | Die Uhr ist die **Anwendungsuhr** und läuft | zwei Minuten später auf der eigenen Route: „wartet seit 15 h **37** min". Gegen `Date.now()` gerechnet stünden dort acht Monate |
| 8 | **Überfällig ohne Farbe** | Zeichen, Wort „Überfällig" und Schriftstärke; kein Rot, kein Gelb (§10.4) |
| 9 | **Die Gesamtdauer im Kopf** | „3 min 15 s" = 12:34:01 → 12:37:16. `gesamtdauerSekunden: 195`, Schrittdauern 75 + 120 = **195** — die Summe *ist* die Gesamtdauer, es steckt keine Zeit dazwischen |
| 10 | **Keine Lückenzeile** | zwischen den beiden Schritten steht nichts — die Zeile ist entfernt |
| 11 | Das alte Feld ist weg | `"timeoutSekunden" in antwort === false` im Kopf; je Schritt trägt es weiterhin `1800` |
| 12 | **Escape im Panel** | schließt; nur `nachricht` fällt aus der URL, `zeitraum`/`status`/`zwischenschritte` bleiben |
| 13 | **Escape auf `/nachrichten/<id>`** | führt auf `/nachrichten` — **mit** der Abfragezeichenkette des geöffneten Links |
| 14 | **Die Ausnahme hält** | im Suchfeld „BMW" getippt, `Escape` gedrückt: Das Feld ist geräumt (`suche=BMW` verschwindet), **das Panel bleibt offen** |
| 15 | Eine Bildlaufleiste, kein Überlauf | Dokument scrollt nicht, `main` ist der einzige senkrechte Scroller, waagerechter Überlauf **0** — trotz des neuen Containers um die Leiste |
| 16 | Feste Zeilenhöhe | jede Schrittzeile **36 px** (`--dichte-zeile`) |
| 17 | Schmales Fenster | **nicht gesehen** — dieselbe Grenze wie immer ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) |

**Punkt 9 ist der stärkste Beleg dafür, dass die Lückenzeile richtig gestrichen ist.** An einer
Nachricht, die seit über fünfzehn Stunden wartet, ist die Summe der Schrittdauern **auf die Sekunde**
gleich der Gesamtdauer: Zwischen den Schritten liegt nichts. Die ganze Wartezeit steckt hinter dem
letzten Schritt — und genau dort steht sie jetzt.

> ✅ **Der Befund aus dieser Runde ist am 11.08.2026 behoben** (Schritt 6, Teil 2a). In der Liste
> daneben stand in derselben Zeile `wartet vor: Send Message …` — die Beschriftung aus Schritt 5,
> Teil 2, die M29 widerlegt hat; Detail und Liste widersprachen sich **an derselben Nachricht,
> nebeneinander sichtbar**. Die Zelle nennt jetzt keine Präposition mehr, sondern `Schritt: …`
> ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1). Der Weg, `offenerZustand` in den
> Listen-Endpunkt zu ziehen, ist dabei erwogen und **verworfen** worden — er wäre je Zeile ein Join
> auf `MessageAction`.

Gesehen wurden dagegen: bis zu **vier Schritte** je Nachricht, Dauern von `< 1 s` bis `1 min 57 s`
nebeneinander (die Normierung trägt genau dort), **36 Eigenschaften**, beide kuratierten Felder mit
ihrer deutschen Beschriftung (`Absender BMW`, `Aufteilungszahl 0`), beide Auflösungsstufen des
Schrittnamens und der Zustand `WARTET_VOR`.

### 10.14 Sichtprüfung nach dem Nachtrag (10.08.2026)

Gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Rolle ADMIN, Fenster 1920 × 889.
**Geklickt und getippt, nicht zugewiesen.** Die Anwendung ist vor der Prüfung **neu gestartet**
worden — ein laufendes Backend kennt einen neuen Zustandswert nicht, und eine Prüfung gegen die alte
Instanz bewiese nichts.

> **Was hier geprüft wird, ist die Gegenrichtung.** `EMPFANGEN` und `OHNE_AKTION` sind lokal nicht
> vorführbar (§10.12) — die Testkopie enthält beide nicht. Geprüft ist deshalb, dass die
> **bestehenden** Zustände unverändert erscheinen. Das ist die Frage, die eine Aufteilung von
> Zustandslogik aufwirft: nicht „sieht der neue Fall gut aus", sondern „hat der alte sich still
> verändert".

**Die Stichprobe ist nach Gestalt gewählt, nicht nach Aktualität:** Zeitfenster 7 Tage und Status
über die Filterleiste geklickt. Unter `Wartend` fällt genau eine Nachricht aus der Reihe — sie liegt
am 29.12. um 12:37, alle übrigen am 24.12. um 06:56. Es ist dieselbe Gestalt wie in §10.13, und das
ist hier der Zweck: derselbe Beleg, nachher gemessen.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | **Der Zustand ist unverändert `WARTET_IN`** | `offenerZustand: "WARTET_IN"`, `naechsterSchritt: "Send Message to Pool"` |
| 2 | Die Zeitleiste zeigt dieselben zwei Schritte | „Konverter VDA4908 an GSVERF IDOC · 1 min 15 s" und „Send Message to Pool · 2 min" |
| 3 | Der Name wird nicht wiederholt | darunter *„Die Nachricht wartet — von selbst geht es hier nicht weiter."* |
| 4 | Der Verweis bleibt im Tooltip | `title` der Zeile: „Der Ablauf verweist auf: Send Message to Pool" |
| 5 | **Die Wartezeile steht wie bisher** | *wartet seit 15 h 34 min · Frist 30 min · ⚠ Überfällig* |
| 6 | Der Tooltip des Schrittnamens trägt Baustein und Herkunft | `NXS_MERGE\|BMW\|WAITUNTIL\|now+170H@…\|SUSPEND` samt „Name aus der Ablaufdefinition" — und bestätigt M29 an dieser Nachricht ein zweites Mal |
| 7 | Feste Zeilenhöhe | jede Schrittzeile **36 px** (`--dichte-zeile`) |
| 8 | **Überfällig weiterhin ohne Farbe** | gemessen `lab(3.6999 0 0)` — Buntheit **null**; unterschieden nur durch Zeichen, Wort und Schriftstärke (500 gegen 400) |
| 9 | Die Gesamtdauer deckt die Schrittdauern | `gesamtdauerSekunden: 195`, Schrittdauern 75 + 120 = **195** |
| 10 | Das alte Kopffeld bleibt weg | `"timeoutSekunden" in antwort === false`; je Schritt trägt es weiterhin `1800` |
| 11 | **`KEINER` ist unverändert** | eine abgeschlossene Nachricht (drei Schritte): `offenerZustand: "KEINER"`, `wartetSeitSekunden: null`, kein `naechsterSchritt`, **keine Wartezeile**, keine angehängte Zeile |
| 12 | Auch dort deckt die Gesamtdauer | `300` gegen 171 + 94 + 35 = **300** |
| 13 | `Escape` schließt weiterhin | nur `nachricht` fällt aus der URL, `zeitraum`/`status` bleiben |
| 14 | Schmales Fenster | **nicht gesehen** — dieselbe Grenze wie immer (§10.10, Punkt 15) |

**Punkt 8 ist der, der ohne Messung nicht zu haben ist.** „Kein Rot" sieht man; „gar keine Farbe"
sieht man nicht — `lab(3.6999 0 0)` sagt es. Die Kennzeichnung greift der offenen Entscheidung in
[`visuelles-konzept.md`](visuelles-konzept.md) §7a damit nachweislich nicht vor.

**Nicht gesehen, weil es sie nicht gibt:** `EMPFANGEN`, `OHNE_AKTION`, `LAEUFT_AUF`, `WARTET_VOR` und
die Kappung. Jeder Punkt mit seinem Grund in §10.12.

> ✅ **Der bekannte Widerspruch ist am 11.08.2026 aufgelöst** (Schritt 6, Teil 2a). Die Liste trug
> bei derselben Nachricht `wartet vor: Send Message …`, während das Detail „wartet in" sagte; sie
> sagt jetzt `Schritt: Send Message …` und behauptet damit nichts mehr über die Lage
> ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1).

### 10.15 Sichtprüfung des Umschalters (11.08.2026)

Gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Rolle ADMIN, Fenster 1920 × 889.
**Geklickt und getippt, nicht zugewiesen.** Kein Neustart nötig — die Nachbesserung ist reines
Frontend, das Backend ist unberührt.

Zwei Belege, gefahren mit gesetzten Filtern: `zeitraum=30d`, `status=AUFGETEILT`,
`prozess=70320_TYCO_US_787690_LS_856`, `sortierung=neueste`.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | Der Umschalter steht im Panelkopf **neben** dem Schließen-Knopf | zwei Knöpfe bei `x = 1800` und `x = 1840`; der Umschalter links davon |
| 2 | Ein Klick führt auf die eigene Route, `nachricht` fällt weg | `/nachrichten/ab498ef3-…?zeitraum=30d&status=AUFGETEILT&sortierung=neueste&prozess=70320_…` — Reihenfolge unverändert, **kein** `nachricht=` |
| 3 | Dort volle Inhaltsbreite, linksbündig, Liste weg | Ansicht **1152 px** (= 72 rem) bei `main` 1712, linke Kante `x = 228` gegen `main` bei 208 |
| 4 | Der Umschalter sagt dort *„Neben der Liste anzeigen"*, ein Klick führt zurück | `aria-label` **und** `title` gewechselt; Ziel `/nachrichten?nachricht=…&zeitraum=30d&status=AUFGETEILT&sortierung=neueste&prozess=70320_…` |
| 5 | Der Zurück-Knopf führt **Schritt für Schritt** zurück | drei Stationen einzeln durchlaufen: Panel → Route → Panel → Liste ohne Kennung, kein Übersprung |
| 6 | **Kein Filter geht verloren, keiner kommt hinzu** | alle drei über die Filterleiste geklickt; nach hin und zurück steht `Status: 1 gewählt`, `Prozess: 1 gewählt`, `30 Tage` unverändert |
| 7 | Auf der eigenen Route ein Kettenglied anklicken | *„Versand Einzel IDOC aus Split"* → `/nachrichten/6e24d350-…?<dieselben Filter>` — **Route bleibt**, nur die Kennung im Pfad wechselt |
| 8 | Der Schließen-Knopf verhält sich in beiden Modi wie vorher | im Panel fällt genau `nachricht` aus der URL; auf der Route führt *„Zurück zur Liste"* auf `/nachrichten?<dieselben Filter>` |
| 9 | Keine zweite Bildlaufleiste | `documentElement.scrollHeight === clientHeight === 889` in **beiden** Modi |
| 10 | Konsole | keine Fehler, keine Warnungen — nur Next-Rauschen (`[HMR] connected`) |
| 11 | **Unter 1280 px erscheint er nicht** | **nicht gesehen** — siehe unten |

**Punkt 11 ist über das Regelwerk belegt und nicht über die Darstellung.** Die Browsersteuerung kann
das Fenster nicht verkleinern ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7). Belegt ist
damit dies und nur dies, gelesen aus dem gebauten Stylesheet:

```
.hidden{display:none}                                            ← ohne Bedingung
@media (min-width:80rem){ … .xl\:inline-flex{display:inline-flex} … }
```

`80rem` sind bei gemessenen 16 px Grundschrift genau **1280 px**; der Knopf trägt beide Klassen
(`tests/ansicht-umschalter.test.tsx`, und im laufenden DOM nachgesehen). **Was daraus folgt, folgt
aus der Regel — gesehen worden ist es nicht.**

**Ein Befund zur Prüfmethode, nicht zur Anwendung:** Ein Klick unmittelbar nach einer Navigation
läuft ins Leere — der Knopf bekommt den Fokusring, aber die Hydration ist noch nicht durch, und der
Ereignisbehandler hängt noch nicht. Der zweite Klick wirkte sofort. Das gehört hierher, damit es beim
nächsten Mal nicht als Fehler des Umschalters gelesen wird.

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

### Keine Deutung des Timeouts, kein Download

`SOSActionTimeout` wird geliefert und nicht gedeutet (§4). Kein Filestore-Verweis wird hier
aufgelöst — das ist Schritt 8.

> **Korrigiert 20.08.2026, nachgetragen zur Korrektur vom 19.08.2026.** Hier stand bis heute: „Der
> Filestore hinter **`Message.Payload.GUID`** wird nicht aufgelöst — das ist Schritt 8." Der Satz
> führte diesen Namen als das, was Schritt 8 auflösen würde. **Schritt 8 löst ihn gar nicht auf:**
> Aufgelöst werden `<Dienst>.Payload.GUID` und `<Dienst>.Log.GUID`; `Message` ist kein Dienst, und
> der Name benennt nach **M73** kein Artefakt (siehe Kasten in §5 und
> [`rohdaten-backend.md`](rohdaten-backend.md) §7). Die Abgrenzung selbst — hier wird kein Verweis
> aufgelöst — ist unberührt.

> **Die Verkettung stand hier bis zum 11.08.2026** und ist in Schritt 6 aufgelöst worden: Die vier
> Spalten stehen als `rollen` im Kopf (§1), der Block darunter zeigt die Kette
> ([`verkettung.md`](verkettung.md) §8). Was **nicht** aufgelöst wird, ist die Aufteilungszahl aus
> den kuratierten Eigenschaften: Sie ist ein `MessageProperty`-Wert und verlinkt weiterhin zu
> nichts.

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
| **L7** jede Abfrage gemessen | §8, sechs Statements gegen drei Gestalten. `rollen` (11.08.2026) ist **keine neue Abfrage**: vier Spalten mehr auf der Zeile, die `findeKopf` ohnehin liest. `bamAnzahl` (12.08.2026) **ist** eine — eine zählende Unterabfrage, gemessen in [`bam-werte.md`](bam-werte.md) §4 und §8: `findeKopf` kostet damit 0,981 ms an der typischen und 4,668 ms an der teuersten bekannten Nachricht |
| **Die Rollen entstehen an einer Stelle** | `common/Kettenrollen`, gerufen und nicht nachgebaut — dieselbe Bauform wie beim `MessageStatusClassifier` (§1) |
| **L8** keine Quelltabelle ohne Erhebung | `MessageAction`, `MessageProperty`, `SOSAction`, `Service` sind in M14 erhoben |
| **L9** kein Durchlauf ohne Zeitfenster in Anwendungscode | keiner; die Erhebungen dieses Dokuments tragen alle ein Fenster |
| **Z1** kein `now()` | die Zone der Zeitumrechnung **und** der Bezugspunkt von `wartetSeitSekunden`/`ueberfaellig` kommen aus der **Anwendungsuhr** (`LocalDateTime.now(anwendungsuhr)`), nie aus der Systemuhr und nie aus dem Browser (§3a) |
| **Z2** `MessageTimeout` in Sekunden | `fristSekunden` im Kopf, `timeoutSekunden` je Schritt, Einheit aus `MessageStatusClassifier.TIMEOUT_EINHEIT` |
| **Q3** die drei Problemkategorien bleiben getrennt | `ueberfaellig` ist ein eigenes Feld neben `statusKind` und wird nie mit *Fehler* zusammengefasst; in der Anzeige trägt es **keine** Farbe, damit es auch optisch nicht mit Rot verschmilzt (§10.4) |
| **§4.2 Kategorie 2 entsteht an einer Stelle** | `MessageStatusClassifier.istUeberfaellig` in `common` — von außen aufrufbar, damit das Dashboard sie ruft statt nachbaut (§3a) |
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
- ~~**Die Beschriftung der Nachrichtenliste.**~~ **Erledigt am 11.08.2026** (Schritt 6, Teil 2a).
  Die Zwischenfassung aus Schritt 5, Teil 2 („wartet vor: …" / „läuft auf: …") ist durch M29
  widerlegt worden; die Zelle nennt jetzt **keine Präposition** mehr, sondern `Schritt: …`
  ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1). Damit ist auch der zweite Teil des Punktes
  erledigt, und zwar durch Verzicht statt durch Beweis: Weder für `WARTEND` noch für `LAEUFT`
  behauptet die Liste noch etwas, das sie aus ihrer Datenquelle nicht wissen kann.
- ~~**Drei Teile der Oberfläche sind nicht gegen echte Daten gesehen** — die Lückenzeile,
  `LAEUFT_AUF` und `OHNE_SCHRITT` samt der Kappung.~~ **Fortgeschrieben am 10.08.2026.** Die
  Lückenzeile ist entfernt; an ihre Stelle sind die Wartezeile und die Gesamtdauer getreten, beide
  gegen echte Daten gesehen. Ungesehen bleiben `LAEUFT_AUF`, `WARTET_VOR`, **`EMPFANGEN`**,
  **`OHNE_AKTION`** und die Kappung — mit je eigenem Grund in §10.12.
- **`EMPFANGEN` ist gebaut, ohne dass eine Messung ihn gesucht hätte.** Die vorgesehene Erhebung ist
  entfallen, weil der Auftraggeber die Frage fachlich beantwortet hat: Im Produktivbetrieb tritt der
  Fall auf. Das ist eine **Auskunft und kein Befund** — sie steht hier als solche, damit sie nicht
  irgendwann als gemessene Zahl gelesen wird. Prüfen ließe sie sich erst an der Produktion, dort dann
  zusammen mit `LAEUFT_AUF` und `RUNNING`.
- **`WARTET_VOR` ist gebaut und in der Testkopie null Mal beobachtet** (M29, `n = 538`). Ob es den
  Fall in der Produktion gibt, ist offen: Gemessen ist ausschließlich `SUSPENDED`, und diese 538
  sind *eine* Gestalt. Die Frage steht als Nummer 17 in
  [`messungen-schritt5.md`](messungen-schritt5.md) und wäre eine Auskunft aus dem Altsystem —
  bedeutet `Message.SOSActionID` dort je „der nächste", oder immer „der zuletzt angefasste"?
- ~~🔴 **Die Liste sagt weiterhin „wartet vor", das Detail sagt „wartet in".**~~ **Erledigt am
  11.08.2026** (Schritt 6, Teil 2a). Von den drei Wegen ist der genommen worden, der ohne die
  Unterscheidung auskommt: Die Zelle nennt Status und Schritt ohne Präposition. Der Weg über
  `offenerZustand` im Listen-Endpunkt ist **verworfen** — er wäre je Zeile ein Zugriff auf
  `MessageAction` (10,3 Mio. Zeilen) für ein Wörtchen, und damit genau der Join, den die Liste nach
  L2/L3 nicht macht ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1). **Das Detail behält seine
  sechs Werte unverändert** — es kann die Unterscheidung treffen, weil es eine Nachricht lädt.
- **Die Problemkategorie „Überfällig" hat keine Farbrolle**, und das ist eine offene Entscheidung
  und kein Versäumnis. [`visuelles-konzept.md`](visuelles-konzept.md) §7 hält fest, dass sie eine
  eigene bekommt — nicht Rot, und wenn gelb, dann auf der orangen Seite bei Ton höchstens 85. Bis
  dahin trägt die Kennzeichnung Zeichen, Wort und Schriftstärke. Entschieden wird das dort, wo das
  Dashboard entsteht, und nicht hier nebenbei.

  > **Nachgetragen am 10.08.2026: Die Entscheidung ist aufgeschoben, nicht getroffen.** Beißen wird
  > sie im **Dashboard** — dort stehen die drei Problemkategorien nebeneinander, und trägt nur eine
  > davon Farbe, liest sich das als Rangfolge. Genau die schließt Regel Q3 aus, wo Fehler,
  > Überfällig und Unquittiert ausdrücklich getrennt und **gleichrangig** geführt werden. Der Punkt
  > steht deshalb jetzt auch dort, wo er fällt: unter den offenen Punkten in
  > [`visuelles-konzept.md`](visuelles-konzept.md), mit dem Rahmen aus §3 und mit dem Zeitpunkt.
  > **Im Detail bleibt es vorerst bei Text und Zeichen; das wird nicht geändert** — „nie allein über
  > Farbe" gilt ohnehin.
- **Das schmale Fenster ist nicht gesehen** (§10.10, Punkt 15). Die Browsersteuerung kann das
  Fenster nicht verkleinern; geprüft ist das Regelwerk, nicht die Darstellung. Derselbe offene Punkt
  wie in [`nachrichtenliste.md`](nachrichtenliste.md) §8.4 und aus demselben Grund — er gehört von
  Hand nachgeholt.
- ~~**`Escape` schließt das Panel, die eigene Route hat keine solche Taste.**~~ **Nachgezogen am
  10.08.2026** (§10.11): Die Taste wirkt auf beiden Einhängepunkten und tut dort dasselbe wie der
  Schließen-Knopf. Die Regel samt ihren zwei Ausnahmen liegt in **einem** Hook, nicht in zwei
  `useEffect`.
