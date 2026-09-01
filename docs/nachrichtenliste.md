# Nachrichtenliste

Entsteht in Schritt 4. Der erste fachliche Endpunkt des Werkzeugs — und der einzige, der `Message`
zeilenweise liest.

Er beantwortet die Frage, um die es in diesem Projekt geht: **Wo steht mein Beleg?** Nicht „was ist
im Einzelnen passiert" (das ist Schritt 5) und nicht „was hängt daran" (Schritt 6).

---

## 1. Der Endpunkt

```
GET /api/nachrichten
```

Angemeldet, Mandant aus der Sitzung. **Kein Parameter für den Mandanten** (Regel M1) — und es darf
auch keiner entstehen.

| Parameter | Werte | Vorgabe | Bemerkung |
|---|---|---|---|
| `zeitraum` | `24h`, `7d`, `30d` | `24h` | relativ, **im Backend** gegen die Anwendungsuhr aufgelöst |
| `von`, `bis` | ISO 8601 UTC | — | zweiter Modus, schließt `zeitraum` aus; beide oder keiner |
| `status` | mehrfach, Werte aus `MessageStatusKind` | alle | **keine Rohwerte** |
| `prozess` | mehrfach, `ProcessID` | alle | |
| `ueberfaellig` | `true`, `false` | `false` | **kein Filter, sondern eine zweite Abfrageform** (§5b). Unvereinbar mit einem `status`, der weder `WARTEND` noch `LAEUFT` enthält |
| `suche` | Freitext, mindestens 3 Zeichen | — | Prozess-, Projekt- und Ablaufname |
| `langeSuche` | `true`, `false` | `false` | hebt die Fenstergrenze der Suche auf — bis 90 Tage, nicht weiter |
| `sortierung` | `neueste`, `aelteste` | `neueste` | ausschließlich über den Zeitpunkt |
| `cursor` | undurchsichtig | — | Seitenposition der vorigen Antwort |
| `limit` | 1 bis 200 | 50 | hartes Maximum |

### Antwort

```json
{
  "items": [
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
      "schritt": null
    }
  ],
  "nextCursor": "MjAyNS0xMi0yOVQyMzoyMjo0MXxjZGI2…",
  "hasMore": true
}
```

**Status doppelt, und das mit Absicht.** `status` ist der Rohwert des Altsystems — damit ein
Anwender ihn gegen die alte Oberfläche halten kann und damit ein unbekannter Wert überhaupt sichtbar
wird. `statusKind` ist die fachliche Einordnung, an der die Oberfläche Farbe und Sortierung
festmacht. Beide kommen aus derselben Stelle: `common/MessageStatusClassifier`.

`bedeutungNichtVerifiziert` steht bei `UNGEKLAERT` — `CHECKED`, `CKECKED`, `COMMIT_SENT` und alles
Unbekannte. Der Wert kommt so aus dem Altsystem, seine fachliche Bedeutung ist nicht belegt; die
Oberfläche kennzeichnet das, statt einen plausiblen Text zu erfinden.

`processName`, `projectName` und `sosName` dürfen `null` sein. Was der Nutzer anstelle einer
fehlenden Zuordnung liest, ist eine Oberflächenentscheidung und gehört in die Sprachdateien, nicht
in eine Abfrage (Regel Q4).

**`sosName` ist der Anzeigename des Ablaufs** und seit der Nachbesserung zu Schritt 4 die Spalte
„Ablauf" (§8.1). **`bamWerte` gibt es nicht mehr** — die Begründung steht in §6.

**`schritt` steht ausschließlich bei `WARTEND` und `LAEUFT`**, sonst `null`. Das ist **keine
Anzeigeentscheidung, sondern eine fachliche**: `SOSActionID` ist auf *jeder* Zeile gesetzt (M13:
3.341.519 von 3.341.519), auch auf abgeschlossenen — dort benennt sie aber den **letzten** Schritt
und nicht den aktuellen. Das Feld heißt „der Schritt, auf dem sie steht"; einen solchen gibt es nur,
solange sie läuft. Ihn trotzdem mitzuschicken hieße, dem Aufrufer einen Wert zu geben, der etwas
anderes bedeutet als sein Name — und der Chatbot aus Ausbaustufe 1 ruft dieselben Endpunkte auf wie
die Oberfläche.

**Kein `total`.** Eine Gesamtzahl über `Message` wäre genau die Live-Aggregation, die Regel L2
verbietet — und sie kostet mehr als die Seite selbst: Ein `COUNT` über ein Jahresfenster liest den
ganzen Bereich, während die Seite nach `limit` Zeilen abbricht. Stattdessen wird `limit + 1`
gelesen; kommt die Zusatzzeile, gibt es eine weitere Seite.

### Fehlerfälle

Alle nach RFC 9457, alle mit eigenem `type` — die Oberfläche übersetzt anhand des `type`, nicht
anhand von `detail`.

| `type` | Status | Wann |
|---|---|---|
| `zeitfenster-mehrdeutig` | 400 | `zeitraum` **und** `von`/`bis` gesetzt |
| `zeitfenster-unvollstaendig` | 400 | nur `von` oder nur `bis` |
| `zeitfenster-ungueltig` | 400 | `bis` liegt vor `von` |
| `zeitfenster-zu-gross` | 400 | Spanne über einem Jahr |
| `zeitpunkt-ungueltig` | 400 | `von`/`bis` nicht als ISO 8601 lesbar |
| `zeitraum-unbekannt` | 400 | anderer Wert als `24h`/`7d`/`30d` |
| `sortierung-unbekannt` | 400 | anderer Wert als `neueste`/`aelteste` |
| `status-unbekannt` | 400 | Wert ist keine `MessageStatusKind` (etwa ein Rohwert) |
| `ueberfaellig-und-status-unvereinbar` | 400 | `ueberfaellig=true` mit einem `status`, der weder `WARTEND` noch `LAEUFT` enthält — die Antwort wäre ohne Rücksicht auf die Daten leer (§5b) |
| `suchbegriff-zu-kurz` | 400 | unter drei Zeichen |
| `suchbegriff-zu-unscharf` | 400 | mehr Treffer als die Grenze |
| `suche-fenster-zu-gross` | 400 | `suche` gesetzt und Spanne über der Grenze (§5) |
| `suche-abgebrochen` | 400 | Statement in `max_statement_time` gelaufen, bei gesetztem `suche` |
| `limit-ungueltig` | 400 | außerhalb 1 bis 200 |
| `cursor-ungueltig` | 400 | unlesbar **oder** Zeitpunkt außerhalb des Fensters |
| `kein-mandant-gewaehlt` | 403 | kein aktiver Mandant in der Sitzung |

**Kein unbekannter Wert wird stillschweigend auf die Vorgabe gezogen.** Wer `zeitraum=24` schreibt,
bekäme sonst 24 Stunden und hätte keinen Anlass, den Tippfehler zu bemerken.

> **Ein alter Link mit `zwischenschritte=false` wird trotzdem nicht abgewiesen** (seit dem
> 11.08.2026, §5). Der Parameter ist keiner mehr und wird wie jeder unbekannte Suchparameter
> übergangen — kein Fehler, keine Umleitung, kein Hinweis. Das ist kein Widerspruch zum Absatz
> darüber: Dort geht es um einen **falschen Wert** für einen Parameter, den es gibt; hier um einen
> Parameter, den es nicht mehr gibt. Der Nutzer, der einen solchen Link öffnet, kann die Entscheidung
> gar nicht mehr treffen, über die man ihn belehren würde — und er bekommt ohnehin zu sehen, was der
> Parameter einblenden sollte. Festgehalten in `NachrichtenlisteDbIT.alter_parameter_wird_uebergangen`,
> und zwar mit beiden Hälften: derselbe Statuscode **und** dieselbe Zeilenmenge.

**`suche-fenster-zu-gross` trägt zwei zusätzliche Felder**, `grenzeTage` und `angefragtTage`. RFC
9457 lässt eigene Felder ausdrücklich zu, und sie sind hier der Punkt: Die Oberfläche baut daraus
eine konkrete Meldung samt Schaltfläche, ohne die Grenze ein zweites Mal zu kennen. Stünde sie auch
im Frontend, liefe eine der beiden Zahlen der anderen irgendwann hinterher — und weil beide
plausibel aussehen, fiele es niemandem auf.

```json
{
  "type": "https://overlord.kraftwerkone.de/probleme/suche-fenster-zu-gross",
  "title": "Zeitfenster für die Suche zu groß",
  "status": 400,
  "detail": "Die Suche ist auf 30 Tage begrenzt; angefragt sind 60. …",
  "grenzeTage": 30,
  "angefragtTage": 60,
  "traceId": "…"
}
```

### ~~Der zweite Endpunkt: was der Bestand hergibt~~ — entfallen am 11.08.2026

```
GET /api/nachrichten/merkmale        →  { "zwischenschritteVorhanden": true }
```

Ergänzt am 07.08.2026, **entfernt am 11.08.2026** (Schritt 6, Teil 2a). Er hat genau eine Frage
beantwortet: *Kommen bei diesem Mandanten überhaupt Zwischenschritte vor?* — damit die Oberfläche
entscheiden konnte, ob sie den Ausblende-Schalter anbietet. **Mit dem Schalter fällt die Frage weg,
und mit der Frage der Endpunkt** samt seinem Zwischenspeicher je Mandant, seinem Rückfall auf `true`
und seinem Pflicht-Isolationstest nach Regel M4. Die Begründung steht in §5.

> ⚠️ **Damit fällt auch die L15-Falle — und das gehört so notiert.** Die Existenzabfrage dahinter
> brauchte für `IBIS` gemessene **13,2 Sekunden** gegen **11,9 Millisekunden** für `WOC`
> ([L15](messungen-schritt4.md#l15--gibt-es-beim-mandanten-überhaupt-zwischenschritte)) — zwei
> Mandanten, die *beide* keinen einzigen Zwischenschritt haben. Der Lese-Pool bricht bei 10 Sekunden
> ab ([`datenzugriff.md`](datenzugriff.md) §1); ohne den `STRAIGHT_JOIN` über
> `ProjectMandant → Process → Message`, der die einzige selektive Reihenfolge erzwang, wäre das
> Statement dort gestorben. Der Grund war die veraltete Statistik der Quelle: `Message_ProcessFK` und
> `MessageStatusIDX` stehen beide mit Kardinalität 18 (M1), und darauf lässt sich keine Planwahl
> gründen.
>
> **Sie verschwindet nicht, weil sie langsam war, sondern weil die Frage nicht mehr gestellt wird.**
> Der Unterschied ist der ganze Punkt dieses Vermerks: Wer ihn überliest, hält den Wegfall für eine
> Leistungsoptimierung und baut dieselbe Gestalt bei nächster Gelegenheit an anderer Stelle wieder
> auf — die Falle steckt nicht im Statement, sondern in der Form „Existenzfrage über den ganzen
> Bestand eines Mandanten".

**Was daraus für andere Endpunkte bleibt:** Eine Frage über den *Gesamtbestand* eines Mandanten hat
keine Kostenobergrenze, die man am Zeitfenster ablesen könnte. Wer je wieder eine stellt, misst sie
je Mandant und nicht einmal (L7) — und legt vorher fest, was passiert, wenn sie in die Zeitgrenze
läuft.

---

## 2. Das Pflicht-Zeitfenster (Regel L1)

Ohne Zeitfenster wird nicht gelesen. Fehlt die Angabe, gilt die Vorgabe von 24 Stunden; das Maximum
ist ein Jahr, gerechnet als Kalenderjahr (`bis.minusYears(1)`) und nicht als 365 Tage — sonst hinge
die Grenze am Schaltjahr.

Die Begründung steht in den Zahlen aus [`messungen-schritt4.md`](messungen-schritt4.md): Mit
Zeitfenster arbeitet MariaDB im `range`-Zugriff über `MessageLastUpdateIDX` und braucht
Millisekunden. Ohne Zeitfenster wird jede Abfrage über `Message` zu einem vollen Durchlauf von 1,5
bis 20 Sekunden.

**Relative Zeiträume werden niemals im Frontend gerechnet.** Der Aufrufer schickt `24h`, nicht zwei
Zeitpunkte. Käme das Fenster aus der Browseruhr, wäre die Anwendungsuhr aus `common/ZeitConfig`
umgangen — und die Liste lokal immer leer, weil die Testkopie Monate hinter der realen Uhrzeit liegt
(Regel Z1).

Die beiden Modi schließen einander aus. Sind `zeitraum` und `von`/`bis` zugleich gesetzt, ist das
`400` und **keine stille Vorrangregel**: Wer beides schickt, hat eine Vorstellung davon, welches
gewinnt; rät das Backend, bekommt er ohne Hinweis ein anderes Fenster als gedacht.

Das Fenster ist beidseitig geschlossen (`von <= MessageLastUpdate <= bis`).

### Zeitzonen — die eine Umrechnung

Zwei Festlegungen treffen aufeinander: Zeitstempel aus `GlassfishDB` sind **Wanduhrzeit des
Servers** und werden nirgends konvertiert ([`datenzugriff.md`](datenzugriff.md) §7), die API
überträgt dagegen **ausschließlich UTC** (Richtlinie §5.3). Dazwischen braucht es eine Zone.

Genommen wird die **Zone der Anwendungsuhr**, an genau einer Stelle (`common/Zeitpunkte`). Damit
kommt keine neue Annahme hinzu: Dass Anwendungs- und Datenbankserver in derselben Zone laufen, setzt
die Anwendungsuhr bereits voraus, sobald sie ein 24-Stunden-Fenster gegen `MessageLastUpdate` hält.
Für die Testkopie ist das gemessen (M0: Serverzeit und Arbeitsplatzuhr gehen gleich).

Die Umrechnung ist bewusst **nicht** Jackson überlassen: Ein `LocalDateTime` hätte dort keine Zone
und ein `Instant` nur die, die zufällig konfiguriert ist.

**Die Gegenrichtung gehört dazu** (ergänzt 06.08.2026). Eine Umrechnung nach UTC, die in der Anzeige
nicht zurückgerechnet wird, ist keine Umrechnung, sondern eine Verschiebung: Aus 23:53:50 in der
Datenbank würde 22:53 auf dem Bildschirm, im Sommer 21:53 — das Altwerkzeug zeigt 23:53. Deshalb
formatiert das Frontend in einer **festen** Zone und nicht in der des Browsers, und **dieselbe** Zone
liefert das Backend als `anzeigezone` in der Selbstauskunft. Es ist genau die der Anwendungsuhr; die
Kette schließt sich damit über einen Wert, der an einer Stelle gepflegt wird. Vollständig in
[`frontend-grundlagen.md`](frontend-grundlagen.md) §4.

---

## 3. Die Mandantenkette — `EXISTS` statt der View

Der Mandantenfilter ist **Bestandteil des Statements** (Regel M3), nicht nachgelagerte Prüfung:

```sql
AND EXISTS (SELECT 1 FROM Process p
            JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
            WHERE p.ProcessID = m.ProcessID AND pm.MandantID = :mandant)
```

**Die vorhandene View `MessageMandantID` wird dafür nicht verwendet.** Das ist eine Abkehr von
[`datenmodell.md`](datenmodell.md) §2, wo die Kette als „in der View gekapselt" beschrieben war, und
die Begründung gehört dazu:

- **Der Zugriffspfad ist strukturell nicht einsehbar.** Weder `SHOW CREATE VIEW` (Fehler 1142) noch
  `EXPLAIN` über die View (Fehler 1345) sind mit unseren Rechten möglich; beide brauchen `SHOW VIEW`,
  das in `SELECT` nicht enthalten ist. Das gilt für den Lese- **und** den Schreibbenutzer und in
  Produktion ebenso wie auf der Testkopie. Ein Zugriffspfad, den wir nie einsehen können, kann
  **Regel L7 nicht erfüllen** — und L7 ist keine Empfehlung.
- **Die View läuft mit `DEFINER = root`** und wird als `IS_UPDATABLE = YES` geführt (M4). Ein
  `SELECT` durch sie läuft also nicht mit den Rechten unseres Lesebenutzers. Schreiben kann über sie
  aus dieser Anwendung niemand — aber die Sicherheitsgrenze des Projekts sollte nicht an einem
  Objekt hängen, dessen Definition und Rechtekontext wir nicht sehen.
- **Die `EXISTS`-Fassung ist nachweislich brauchbar:** Sie nutzt `MessageLastUpdateIDX`, braucht kein
  `filesort` und liefert dieselbe Menge — die Gegenprobe in M4 ergab null Abweichung. Im dort
  gemessenen Fenster war sie zudem schneller (1,233 ms gegen 2,265 ms).

**Als `EXISTS` und nicht als Join:** `ProjectMandant` ist im Schema n:m, ein Join könnte Zeilen
vervielfachen, sobald ein Projekt mehreren Mandanten gehört. In den Daten tut er das heute nicht
(M3: alle 134 Projekte gehören genau einem Mandanten) — aber eine Liste, deren Zeilenzahl an einer
Stammdatenpflege hängt, ist die falsche Grundlage für eine Sicherheitsgrenze.

**Projekte ohne Zeile in `ProjectMandant`** bleiben für niemanden sichtbar, auch nicht für ADMIN
(Annahme A8, geklärt). Kein Sonderpfad, kein Pseudo-Mandant. Die sechs betroffenen Projekte tragen
ohnehin keine einzige Nachricht.

### Der Isolationstest (Regel M4)

`NachrichtenIsolationDbIT` — ohne ihn wird der Endpunkt nicht gemergt. Er kopiert das Muster aus
`MandantenIsolationDbIT` und tauscht Aufruf und Kennung.

**Mandant A ist `NEXANS`, Mandant B ist `SUTTONS`.** Über den Gesamtbestand liegt `SUTTONS` mit
197.158 Zeilen vor `VOTG` mit 145.840 (M3). `NXHBE` und `IBISGUS` scheiden aus — zwei Mandanten
desselben Hauses sind ein schlechter Beweis für eine Trennung, die zwischen Firmen greifen soll;
`NXHBE` hat ohnehin neun Nachrichten.

**Das Zeitfenster ist absolut** (29.12.2025). Außer `NEXANS` endet jeder Mandant am 30.12.2025 (M3,
„Zeitspanne je Mandant"); in einem relativen Fenster sähe `SUTTONS` je nach Datenstand null Zeilen —
und der Test bewiese nur, dass leer leer ist. Im gewählten Fenster hat `NEXANS` 5.043 und `SUTTONS`
685 Zeilen. **Der erste Testfall hält genau das fest**, damit ein späterer Datenstand die
Aussagekraft nicht stillschweigend verliert.

Geprüft wird:

1. Beide Mandanten haben Daten im Fenster — die Voraussetzung, ohne die alles Folgende wertlos wäre.
2. Der Nutzer auf `NEXANS` bekommt **keine** Nachricht und **keinen** Prozess von `SUTTONS`; die
   fremden Kennungen kommen im Antwortrumpf überhaupt nicht vor.
3. **Die Gegenprobe, die den Kern ausmacht:** Ein Filter auf einen fremden, *existierenden* Prozess
   und ein Filter auf eine *erfundene* Kennung liefern **ununterscheidbare** Antworten — gleicher
   Status, gleicher Rumpf (ohne `traceId`). Wären sie zu unterscheiden, ließe sich über den
   Prozessfilter die Prozesslandschaft fremder Mandanten abfragen. Und es ist `200` mit leerer
   Liste, nicht `403`: Die Zeile hat für diesen Nutzer nie existiert.
4. Der Freitextfilter findet keine fremden Prozesse — die Trennung gilt auch quer (Regel M5), also
   auch in der Vorfilterung über die Stammdaten.
5. Ein **fremder Cursor** öffnet keinen fremden Ausschnitt. Er trägt keine Berechtigung, sondern nur
   einen Zeitpunkt und eine Kennung.
6. Die Trennung gilt in **beide** Richtungen — sonst bewiese der Test nur, dass `NEXANS` alles sieht.

### Der aktive Mandant kommt über die zulässige Menge

Der Endpunkt liest den Mandanten nicht roh aus der Sitzung, sondern über
`MandantService.aktuellerKontext`. Der prüft den Sitzungswert gegen die zulässige Menge des Nutzers.
Verliert jemand während einer laufenden Sitzung seine Zuordnung, fällt der aktive Mandant damit von
selbst weg ([`mandantentrennung.md`](mandantentrennung.md) §8). Der Preis ist eine kleine
zusätzliche Abfrage je Aufruf (~0,6 ms), der Gegenwert ist, dass ein Entzug sofort wirkt.

---

## 4. Cursor-Paginierung (Regel L3)

Kein `OFFSET`. Der Cursor ist Base64URL über `(MessageLastUpdate, MessageID)` und **undurchsichtig**
— das Format bleibt damit änderbar, ohne einen Aufrufer zu brechen. Er ist nicht signiert: Er trägt
keine Berechtigung, der Mandantenfilter steht im Statement, und ein manipulierter Cursor verschiebt
höchstens die eigene Seitenposition innerhalb des eigenen Ausschnitts.

**Ein unlesbarer Cursor ist `400`, kein stillschweigendes „von vorne".** Sonst blättert der Aufrufer
endlos im Kreis, ohne dass jemand den Fehler bemerkt. Dasselbe gilt für einen Cursor, dessen
Zeitstempel außerhalb des Fensters liegt — er gehört zu einer anderen Anfrage.

**Der Cursor gehört nicht in die geteilte URL.** Filter und Zeitfenster ja, die Seitenposition nein:
Ein Link auf Seite sieben eines relativen Fensters zeigt beim Empfänger auf andere Zeilen.

### Die ODER-Form, gemessen statt vermutet

```sql
AND (MessageLastUpdate < :ts OR (MessageLastUpdate = :ts AND MessageID < :id))
```

Bei aufsteigender Sortierung gespiegelt. Die Alternative wäre der Tupelvergleich
`(MessageLastUpdate, MessageID) < (:ts, :id)`. Gemessen (L8):

| | ODER-Form | Tupelvergleich |
|---|---|---|
| gewählter Index | `MessageLastUpdateIDX` | `MessageLastUpdateIDX` |
| `key_len` | **151** (beide Spalten) | 5 (nur der Zeitstempel) |
| gelesene Zeilen (`r_rows`) für 51 gelieferte | **52** | 245 |
| Laufzeit, beste von fünf | **1,795 ms** | 2,551 ms |

**Warum `key_len = 151`:** `MessageLastUpdateIDX` steht laut Schema nur auf `MessageLastUpdate`.
InnoDB hängt an jeden Sekundärindex den Primärschlüssel, und MariaDB nutzt das
(`optimizer_switch: extended_keys=on`) — der Index ist damit faktisch
`(MessageLastUpdate, MessageID)` und genau der Sortierschlüssel dieser Liste. 5 Bytes Zeitstempel
plus 146 Bytes `varchar(36)` ergeben die 151.

**Nebenbefund, der eine dokumentierte Warnung entschärft:** Der Tiebreaker über `MessageID` löst
**kein** `filesort` aus. Die Warnung in [`datenmodell.md`](datenmodell.md) §3 zielte auf den
zusammengesetzten Index `MessageLastUpdateProcessMessageIDX`, der `ProcessID` zwischen den beiden
Cursor-Spalten hat — gebraucht wird der hier gar nicht.

**Sortiert wird ausschließlich über den Zeitpunkt**, Richtung wählbar. Kein zweiter Sortierschlüssel:
Cursor-Paginierung funktioniert nur auf dem Sortierschlüssel selbst, und für Status oder Prozessname
gibt es weder einen Index noch einen eindeutigen Tiebreaker.

---

## 5. Filter

### Status über `MessageStatusKind`, nicht über Rohwerte

Ein Nutzer sucht „Fehler", nicht `ERROR_DUPLICATE`. Die Übersetzung Einordnung → SQL steht in
`MessageStatusClassifier.bedingung(...)` — an derselben Stelle wie die Übersetzung Rohwert →
Einordnung und **nirgends nachgebaut**. Wäre sie im Repository nachgebaut, driftete sie beim
nächsten neuen Statuswert von der Anzeige weg.

- `FEHLER` nutzt die bestehende `fehlerBedingung`
  (`MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'`) — **nicht** die
  Aufzählung der drei bekannten Rohwerte, sonst fände der Filter eine künftige `ERROR_`-Art nicht.
- `UNGEKLAERT` ist der **Rest**: alles, was weder Fehler noch einer der anderen bekannten Werte ist,
  und `NULL`. Es kann keine Aufzählung sein, weil genau die unbekannten Werte hierher gehören.
- Alle übrigen Einordnungen sind geschlossene Mengen und werden aufgezählt.

Damit `bedingung` und `einordnung` deckungsgleich sind, ordnet der Classifier seit dem 06.08.2026
einen **unbekannten Wert mit Präfix `ERROR_` als `FEHLER`** ein. Das ist kein Raten, sondern
dieselbe Regel, die die SQL-Fehlerbedingung seit Schritt 2 anwendet und die `datenmodell.md` §4
nennt. Ohne diese Zeile lieferte der Filter `FEHLER` eine Zeile, die die Liste anschließend als
„Bedeutung nicht verifiziert" beschriftet. Ein Integrationstest hält die Deckungsgleichheit fest:
Jede gelieferte Zeile trägt die Einordnung, nach der gefiltert wurde.

### Der Ausblende-Schalter — gebaut, gemessen, entfernt

**Die Liste filtert nicht nach Status, außer der Nutzer sagt es ausdrücklich.** Seit dem 11.08.2026
(Schritt 6, Teil 2a) gibt es keine Statusvorgabe mehr — weder als Parameter, noch als Bedingung im
Statement, noch als Bedienelement. Dieser Abschnitt erzählt, was stattdessen dastand und warum es
wieder verschwunden ist; wer den Schalter in einem halben Jahr wieder vorschlägt, findet hier den
Grund und nicht nur die Tatsache.

#### Was gebaut war

`zwischenschritte=false` war die Vorgabe des Listen-Endpunkts und schloss `SPLITTED` und `MERGED`
aus. Ein ausdrücklicher Statusfilter auf `ZWISCHENSCHRITT` schlug sie — sonst hätte der Nutzer
danach gefiltert und garantiert null Zeilen bekommen. In der Oberfläche stand ein Chip, der den
Zustand benannte und umschaltete (§8.2); ob er überhaupt erschien, entschied
`GET /api/nachrichten/merkmale` (§1).

**Die Begründung war fachlich und klang gut:** **34,38 Prozent aller Zeilen** sind Zwischenprodukte
(M6). Eine Liste, die zu einem Drittel aus Begriffen besteht, die der Zielnutzer nicht kennt, kostet
beim ersten Kontakt Vertrauen.

#### Was gemessen wurde

**Sie war nie gemessen.** Die Runde vom 10.08.2026
([`messungen-schritt6.md`](messungen-schritt6.md)) hat sie geprüft und in drei Punkten widerlegt:

- **Die ausgeblendete `SPLITTED`-Zeile ist der Elternteil, die gezeigten `FINISHED`-Zeilen sind
  seine Fragmente** (M24). Die Vorgabe blendete also gerade die Zeile aus, die der Nutzer
  wiedererkennt — der Produktions-Screenshot, der die Runde ausgelöst hat, war der Normalfall.
- **96,9 Prozent der ausgeblendeten Wurzeln tragen BAM-Werte gegen 2,4 Prozent der gezeigten
  Kinder** (M26‑1b). Das ist kein Anzeige-, sondern ein **Suchproblem**: Der Treffer war da, die
  Zeile war weg. Für `NEXANS` waren beide kuratierten BAM-Spalten auf der Vorgabe-Ansicht deshalb
  praktisch leer (E1) — schon vor Schritt 7.
- **Drei Mandanten ohne einen einzigen Zwischenschritt haben trotzdem Ketten** (`IBIS`, `IBISGUS`,
  `ZAST`, M24‑3). Ein Kriterium über den *Status* erfasst die Verkettung gar nicht; `Source = 1`
  erfasst sie (E4).

#### Warum ersatzlos und nicht ersetzt

Naheliegend wäre gewesen, die Vorgabe zu **drehen**: statt über den Status über die *Stellung in der
Kette* auszublenden. M28‑1 hat beziffert, was das kostet (Fenster B, 214.330 Zeilen):

| `MandantID` | Zeilen | heute ausgeblendet | Stellungsprädikat P1 |
|---|---:|---:|---:|
| `NEXANS` | 180.251 | 36,69 % | **77,61 %** |
| `SUTTONS` | 21.516 | 2,97 % | 5,80 % |
| `IBIS` | 4.331 | **0 %** | 0,83 % |
| **`IBISGUS`** | 1.722 | **0 %** | **100,00 %** |
| **`ZAST`** | 283 | **0 %** | **92,93 %** |
| **gesamt** | 214.330 | 31,15 % | **66,80 %** |

**Bei `IBISGUS` blendet P1 jede einzelne Zeile aus** (1.722 von 1.722; `ohne_kette` ist dort null),
bei `ZAST` 92,93 Prozent. Für beide Mandanten wäre die Vorgabe-Ansicht leer beziehungsweise fast
leer — **heute sehen sie jede Zeile.**

> **Eine Vorgabe, die je nach Mandant zwischen 0 % und 100 % versteckt, ist keine Vorgabe.** Das ist
> der Satz, an dem die Entscheidung hängt: Nicht der Wert des Prädikats war falsch gewählt, sondern
> die Vorstellung, es gäbe überhaupt eine sinnvolle Ausblendung, die für alle Mandanten gleichzeitig
> gilt. Beide Kandidaten — der alte über den Status, der neue über die Stellung — sind an dieser
> Streuung gescheitert, und zwar an entgegengesetzten Enden.

Dazu kommt der Befund aus M26, der auch die gedrehte Fassung trifft: Die Zeile, die die Belegnummer
trägt, ist die Wurzel. Ein Prädikat, das Wurzeln ausblendet, versteckt genau die Zeile, die der
Nutzer sucht — dieselbe Fehlrichtung wie vorher, nur mit besserem Kriterium.

**Was an die Stelle tritt: nichts.** Die Liste zeigt jede Zeile ihres Zeitfensters. Was der Nutzer
nicht sehen will, filtert er weg — über den Statusfilter, der beide neuen Werte einzeln anbietet.

#### Was das kostet, und was es nicht kostet

Die Liste zeigt bei `NEXANS` sichtbar mehr Zeilen als vorher — im Tagesfenster des dichten Bestands
5.043 statt 4.228 (M28‑1 Fenster A, E1). Das ist die getragene Folge und keine Nebenwirkung: **Der
Zielnutzer bekommt Zeilen zu sehen, deren Status er nicht kennt.** Die Antwort darauf ist die
Beschriftung und nicht das Verstecken — `AUFGETEILT` und `ZUSAMMENGEFUEHRT` sagen in einem Wort, was
passiert ist ([`message-status.md`](message-status.md)).

**Am Zugriffspfad ändert sich nichts.** Die entfallene Bedingung war ein Filter auf `MessageStatus`,
kein Zugriffspfad; Treiber ist und bleibt das Zeitfenster über `MessageLastUpdateIDX` (L1 bis L3).
Eine neue Messung nach L7 braucht es dafür nicht — es fällt eine Bedingung weg, es kommt keine hinzu.

> **Der Abstrich, der dokumentiert gehört: Der Status ist über die Kette unzuverlässig.** Bei `IBIS`,
> `IBISGUS` und `ZAST` trägt die Wurzel `FINISHED` (M24‑3). Die Liste sagt damit bei `NEXANS` etwas
> über die Aufteilung und bei drei Mandanten nichts — und sie **kann es nicht besser wissen**, weil
> die verlässliche Auskunft (`Source`) nicht im Status steht. Das ist die bewusst getragene Folge
> daraus, dass die Liste **keine Rollenkennzeichnung** bekommt (Entscheidung des Auftraggebers; die
> Rolle erscheint im Detail). **Bekannter Punkt, kein Fehler** — er steht auch unter „Offene Punkte".

### Freitext — niemals gegen `Message`

Gesucht wird in den **Stammdaten**: `Process.ProcessName`, `Project.ProjectName` und `SOS.SOSName`.
Das sind 1.490, 140 und 1.818 Zeilen; ein voller Durchlauf über `SOS` kostet 3,1 ms (M5). Auf
`SOSName` gibt es keinen Index, und bei dieser Größe braucht es auch keinen. Ein `LIKE` gegen
`Message` wäre dagegen ein Durchlauf über 2,9 GB.

Die Treffer werden zu `ProcessID`s und `SOSID`s aufgelöst; das Hauptstatement filtert dann über
`m.ProcessID IN (…) OR m.SOSID IN (…)`. Trifft der Begriff nichts, ist die Liste leer, **ohne dass
`Message` angefasst wird**.

Auch die Vorfilterung trägt den Mandantenfilter — nicht aus Sicherheitsgründen (den trägt das
Hauptstatement), sondern damit die Trefferzahl den Mandanten beschreibt und nicht den Gesamtbestand.

**Die Trefferliste wird gedeckelt (200 je Seite der Auflösung).** Trifft der Begriff mehr, antwortet
der Endpunkt mit `suchbegriff-zu-unscharf` — und **nicht** mit einer abgeschnittenen Menge, die wie
ein vollständiges Ergebnis aussieht.

> ⚠️ **Der Freitextfilter ist der teuerste Fall dieses Endpunkts.** Die Vorfilterung selbst kostet
> nichts (3,0 und 5,8 ms); teuer ist das Hauptstatement. Die Kosten hängen am Suchbegriff und
> reichen von 2 ms bis zu einigen Sekunden.
>
> **Zwei Abhilfen wurden geprüft, beide tragen nicht** (Messungen M10 und L11, 06.08.2026). Die
> Auflösung über `SOS.ProcessID` zu einer einzigen Kennungsliste **verliert 9.101 Zeilen**, weil der
> Ablauf dort auf einen anderen Prozess zeigt als die Nachricht. Und die `UNION`-Fassung ist
> **langsamer** statt schneller: `Message.SOSID` hat **keinen Index**, der zweite Zweig muss also
> ebenfalls über das Zeitfenster einsteigen — der `UNION` liest das Fenster zweimal statt einmal.
> Der Stand und was daraus folgt, steht unter „Offene Punkte".

`%` und `_` im Suchbegriff werden maskiert. Ohne das wäre `_` ein Platzhalter für ein beliebiges
Zeichen — derselbe Fallstrick wie in Regel Q1.

### Die Fenstergrenze der Suche — 30 Tage, bewusst aufhebbar bis 90

Nachgetragen am 06.08.2026 (Messung [L13](messungen-schritt4.md#l13--freitext-über-lange-fenster)).
Der Filter bleibt, wie er ist; was dazukommt, ist eine Grenze.

**Ist `suche` gesetzt und die Spanne des Zeitfensters größer als 30 Tage, antwortet der Endpunkt mit
`400 suche-fenster-zu-gross`.** `langeSuche=true` hebt die Grenze auf **90 Tage** an — nicht weiter.
Das Maximum von einem Jahr aus Regel L1 bleibt darüber bestehen; bei gesetztem Suchbegriff greift es
nur nie, weil die engere Grenze früher zieht. Ohne Suchbegriff ändert sich **nichts**: Ein
Jahresfenster kostet dort dieselben 2,7 ms wie ein Tagesfenster (L1 bis L3).

**Die Grenze gilt für die Spanne, nicht für den Modus.** Praktisch beißt sie nur im `von`/`bis`-Modus,
weil `zeitraum` ohnehin nur `24h`, `7d` und `30d` anbietet. Das ist kein Grund, sie im relativen Modus
wegzulassen — käme dort je ein `90d` dazu, wäre die Lücke sonst still da, bevor jemand sie sucht.

**Warum eine Grenze auf das Fenster und keine auf die Trefferzahl.** Teuer ist nicht der breite
Suchbegriff, sondern der seltene. Ein Begriff, dessen Prozesse im Fenster viele Zeilen tragen, füllt
die 51 Treffer sofort (2,3 ms). Ein Begriff, dessen Prozesse kaum Zeilen tragen, zwingt MariaDB
durch das Fenster, bis 51 beisammen sind (499 ms bei 78.318 gelesenen Zeilen). Der schlimmste Fall
ist der Begriff, der in den Stammdaten trifft und im Fenster nichts findet: voller Durchlauf, leeres
Ergebnis. **Eine Grenze auf die Zahl der aufgelösten Prozesse bestrafte damit genau den schnellen
Fall und ließe den langsamen durch.**

**Warum 90 Tage und nicht ein Jahr.** Der schlimmste Fall kostet gemessen 1,2 s über 30 Tage,
**3,9 s über 90 Tage**, 8,0 s über 180 Tage und **15,6 s über ein Jahr**. Der Lese-Pool bricht bei
10 s ab (`max_statement_time`, [`datenzugriff.md`](datenzugriff.md) §1) — ein Jahresfenster reißt
diese Grenze also, und 180 Tage liegen mit 80 Prozent so dicht daneben, dass ein dichterer Tag
genügt. 90 Tage lassen Faktor 2,5 Luft und sind eine **gemessene** Spanne, keine interpolierte.

**Die Antwort nennt beide Zahlen** (`grenzeTage`, `angefragtTage`, §1). Damit baut die Oberfläche
eine konkrete Meldung statt einer allgemeinen — und die Grenze bleibt an einer Stelle gepflegt.

Im Code: `NachrichtenFilter.SUCHE_FENSTER` und `SUCHE_FENSTER_LANG`, geprüft in
`NachrichtenFilter.aus` — an derselben Stelle wie jede andere Parameterprüfung.

### Wenn das Statement trotzdem in die Zeitgrenze läuft

Die Grenze macht den Abbruch unwahrscheinlich, nicht unmöglich: Unter Last oder in einem dichteren
Bestand als dem gemessenen kann derselbe Fall wieder auflaufen. **Bei gesetztem Suchbegriff ist das
ab jetzt ein absehbarer Fall und kein Systemfehler** — er wird zu `400 suche-abgebrochen` mit einem
Text, der sagt, was zu tun ist: Zeitraum verkleinern oder Suchbegriff schärfen. Vorher wäre es ein
`500` mit neutralem Text und einer Fehler-Kennung gewesen, also die Bitte, eine Störung zu melden,
die keine ist.

**Gefangen wird genau eine Ausnahme.** Nachgeprüft gegen die Testkopie (L13): MariaDB meldet Fehler
`1969` mit SQLState `70100`, Connector/J 3.5 macht daraus eine `java.sql.SQLTimeoutException`, und
jOOQ verpackt sie in eine `DataAccessException` mit genau dieser Ursache — ein
Spring-`SQLExceptionTranslator` liegt nicht dazwischen, weil die beiden `DSLContext`-Beans in
`config/JooqConfig` von Hand gebaut werden. Ein Syntaxfehler, ein Verbindungsabriss oder ein
fehlendes Recht kommen ebenfalls als `DataAccessException` an und bleiben, was sie sind: technische
Fehler mit `500`.

**Und nur mit Suchbegriff.** Läuft ein Statement *ohne* Suche in die Zeitgrenze, ist das kein
vorhergesehener Fall, sondern ein Befund; er gehört mit Stacktrace ins Protokoll und nicht in einen
Hinweis, der dem Nutzer eine Verhaltensänderung nahelegt, die nichts ändern würde. Umgesetzt in
`NachrichtenRepository.anDerZeitgrenze`.

**`400` und nicht `503`** — mit einem Abstrich, der dazugehört: Die Anfrage ist so, wie sie gestellt
wurde, nicht beantwortbar, und was sich ändern muss, ist die Anfrage (Richtlinie §5.5, „Anfrage
fachlich unbrauchbar"). Der Abstrich ist, dass dieselbe Antwort auch dann kommt, wenn in Wahrheit die
Datenbank unter Last steht; der Nutzer liest dann eine Aufforderung, die ihm nicht hilft. Sichtbar
bleibt es trotzdem: Die `traceId` steht in der Antwort und im Protokoll, und häufen sich diese
Einträge, ist das das Signal. Steht unter „Offene Punkte".

---

## 5a. Der Plan der Liste — sechs Fassungen gemessen, keine gebaut (27.08.2026)

**Offener Punkt 57** aus [`messungen-schritt10b.md`](messungen-schritt10b.md) hält fest, dass diese
Abfrage bei `SUTTONS` über dreißig Tage **1.101,280 ms** kostet und bei `NEXANS` **1,803 ms** —
Faktor 611, und der kleinere Mandant ist der langsame. Schritt 10b‑1 hatte den Auftrag, drei
Fassungen dagegen zu messen und die beste zu bauen, **falls** eine trägt.

**Sechs sind gemessen. Keine trägt, und keine ist gebaut worden.** Der Grund ist nicht, dass keine
Fassung `SUTTONS` repariert — zwei tun das —, sondern dass jede von ihnen einen anderen Mandanten
oder einen anderen Filter derselben Liste zerstört.

**Am Code dieses Endpunkts ist deshalb nichts geändert worden.** Was bleibt, sind Zahlen: die zehn
Mandanten einzeln, die anderen Filter einzeln, und die Erkenntnis, dass die Liste heute **zwei
Planfamilien** hat, von denen jede für eine andere Klasse von Mandanten falsch ist.

### Der Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, `SELECT @@global.read_only` = **1** als erste Abfrage jeder Sitzung |
| Benutzer | `monitor_read`, ausschließlich `SELECT` / `SET` / `EXPLAIN` / `PREPARE` (Regel S1) |
| Anker | **`2025-12-30 04:09:47`** (V5 der Vorrunde), als Literal im Statement (Regel Z1) |
| Fenster | `zeitraum=30d` gegen diesen Anker, **beide Grenzen einschließlich** — wie `.ge`/`.le` es setzen. `24h` und `7d` in der Gegenprobe |
| Statement | abgeschrieben aus diesem Repository, nicht nachgebaut: vier `LEFT JOIN`, `EXISTS`-Mandantenkette, `ORDER BY (MessageLastUpdate, MessageID) DESC`, `LIMIT 51` |
| Laufzeit | Aufwärmlauf, dann **beste von fünf**, aus `information_schema.PROFILING` |
| Plan | am **nackten** Statement — `EXPLAIN` führt es nicht aus und gibt keine Datenzeile aus (G1) |
| Sitzungen | zwanzig, unter `scripts/messung-schritt10b-1/`. Rohausgaben über `.gitignore` ausgeschlossen |
| `STRAIGHT_JOIN` | **in keiner Fassung** (M42: Faktor 219 bis 1094) |
| `ANALYZE TABLE` | **nicht gefahren** — `monitor_read` hat auf `GlassfishDB` nur `SELECT`, und dort wird nie geschrieben |

> **Warum die Laufzeit an einer aggregierenden Hülle gemessen ist und der Plan am nackten
> Statement.** Dieselbe Bauform wie M97, aus denselben zwei Gründen: Das nackte Statement liefert
> `MessageID`, `ProcessID`, `ProcessName` und `ProjectName` und darf nicht ausgegeben werden (G1);
> und ohne eine Hülle, die **jede** gejointe Spalte anfasst, optimiert MariaDB die `LEFT JOIN` weg,
> sodass die Messung die ihrer Abwesenheit wäre. Beide Pläne stehen in den Sitzungsdateien
> nebeneinander und sind Zeile für Zeile gleich, bis auf die eine `<derived2>`-Zeile der Hülle.

### Die sechs Fassungen

| | Eingriff | Woher |
|---|---|---|
| **F0** | keiner — die heutige Fassung, Bezugsgröße | |
| **F1** | Prozessliste des Mandanten vorab auflösen, dann `m.ProcessID IN (…)` statt `EXISTS` | Auftrag |
| **F2** | `FORCE INDEX (MessageLastUpdateIDX)` auf `Message` | Auftrag |
| **F3** | `IGNORE INDEX (ProejctIDIDX)` auf `Message` | Auftrag |
| **F4** | `IGNORE INDEX FOR JOIN (ProejctIDIDX, Message_ProcessFK)` auf `Message` | ergänzt, weil F3 wirkungslos ist: der Optimierer weicht auf den Geschwisterindex aus |
| **F5** | `LIMIT 1` in der `EXISTS`-Unterabfrage | ergänzt, um die Semi-Join-Umformung zu verhindern, ohne einen Index anzufassen |
| **F6** | `IGNORE INDEX FOR JOIN (Process_ProjectFK)` auf **`Process` in der Mandantenkette** — auf `Message` liegt **kein** Hinweis | ergänzt, weil F4 den Prozessfilter mit erschlägt |

**Alle sechs liefern zeilengleich dasselbe.** Die Hülle gibt Zeilenzahl, ältesten und jüngsten
Zeitpunkt und die Zahl verschiedener Statuswerte aus; über alle Fassungen, alle Mandanten und beide
Cursor-Stellungen stimmen diese vier Werte **jedes Mal** überein. Der Eingriff verändert den Weg,
nicht das Ergebnis.

### Das Ergebnis für die beiden Mandanten des Auftrags

F0 und F6 in **derselben** Sitzung, abwechselnd gefahren — sonst wäre der Unterschied nur so genau
wie zwei Sitzungen es sind (`a1-entscheidung-nexans.sql`, `a1-entscheidung-suttons.sql`).

| Fall | Mandant | **F0** | **F6** | |
|---|---|---:|---:|---|
| 30 Tage, ohne Cursor | NEXANS | 1,861 ms | **1,789 ms** | unverändert |
| 30 Tage, mit Cursor | NEXANS | 2,250 ms | **2,242 ms** | unverändert |
| 30 Tage, `ueberfaellig` | NEXANS | 5,282 ms | 5,253 ms | unverändert |
| 30 Tage, `ueberfaellig`, mit Cursor | NEXANS | 5,803 ms | 5,713 ms | unverändert |
| **30 Tage, ohne Cursor** | **SUTTONS** | **1.034,784 ms** | **7,093 ms** | **Faktor 146** |
| **30 Tage, mit Cursor** | **SUTTONS** | **1.149,164 ms** | **5,499 ms** | **Faktor 209** |
| 30 Tage, `ueberfaellig` | SUTTONS | 7,081 ms | 6,991 ms | unverändert |
| 30 Tage, `ueberfaellig`, mit Cursor | SUTTONS | 7,646 ms | 7,623 ms | unverändert |

**Nach dem Tor des Auftrags wäre das ein Bauauftrag:** `SUTTONS` unter 50 ms, `NEXANS` nicht messbar
schlechter. Die anderen fünf Fassungen fallen schon hier durch:

| Fassung | Was sie tut | Was sie kaputt macht |
|---|---|---|
| **F1** | `SUTTONS` 1.034,784 → **4,598 ms** | **`NEXANS` mit Cursor 2,189 → 51,422 ms** (Faktor 23). 733 Bindeplätze werden je Kandidatenzeile geprüft |
| **F2** | `SUTTONS` → **7,841 ms** | **`ueberfaellig` bei `NEXANS` 5,170 → 129,759 ms** (Faktor 25) und bei `SUTTONS` 6,904 → **1.379,042 ms** (Faktor 200). Der erzwungene Zeitindex verdrängt `MessageStatusIDX` — und genau der trägt die überfällige Liste |
| **F3** | **nichts** — `SUTTONS` bleibt bei 1.139,967 ms | nichts. Der Optimierer weicht von `ProejctIDIDX` auf `Message_ProcessFK` aus: derselbe Plan, anderer Name |
| **F4** | `SUTTONS` → **7,653 ms** | **Den Prozessfilter.** `NEXANS` mit drei kleinen Prozessen 1,730 → **1.547,772 ms** (Faktor 894), `SUTTONS` 5,887 → 1.544,238 ms (Faktor 262) |
| **F5** | **nichts** — `SUTTONS` bleibt bei 1.117,393 ms | nichts. `LIMIT 1` verhindert die Semi-Join-Umformung nicht |

> **Der Prozessfilter ist zweimal gemessen worden, und die erste Messung war falsch.** In der ersten
> Fassung der Gegenprobe stand er als `m.ProcessID IN (@p1, @p2, @p3)` — mit Sitzungsvariablen, um
> keine Kennung ins Skript zu schreiben (G1). **Darauf führt MariaDB keine Bereichsanalyse aus:**
> `ProejctIDIDX` blieb in `possible_keys` und wurde nie als Bereich genutzt; gemessen war also nicht
> der Prozessfilter, sondern seine Abwesenheit. Die Zahlen oben stammen aus der zweiten Fassung
> (`a1-gegenprobe-prozessfilter-*.sql`), in der die Kennungen als **Literale** im Statement stehen —
> dieselbe Form, die der Treiber auf die Leitung legt. **Die erste Fassung ist trotzdem
> aufgehoben** (`a1-gegenprobe-filter-*.sql`, mit einem Hinweis im Kopf): Ihre übrigen Fälle — 24
> Stunden, sieben Tage, Statusfilter — sind von dem Fehler nicht berührt und stehen in der Übersicht
> unten.

### ⚠️ Die Gegenprobe über alle zehn Mandanten — und hier fällt die Entscheidung

Offener Punkt 57 verlangt zu prüfen, „ob es weitere Mandanten betrifft (acht sind ungemessen)".
Sie sind jetzt gemessen: 30 Tage, ohne Cursor, F0 gegen F6 in derselben Sitzung.

| Mandant | Zeilen im Fenster | Zeilen insgesamt | Treiber bei **F0** | **F0** | **F6** | |
|---|---:|---:|---|---:|---:|---|
| NEXANS | 180.154 | 2.885.711 | `m` / Zeit | 1,861 ms | 1,789 ms | |
| **SUTTONS** | 21.524 | 197.158 | **`pm` / Prozess** | **1.034,784 ms** | **7,093 ms** | **F6 hilft** |
| VOTG | 6.106 | 145.840 | `m` / Zeit | 36,602 ms | 36,000 ms | |
| IBIS | 4.331 | 75.746 | `m` / Zeit | 44,384 ms | 44,602 ms | |
| IBISGUS | 1.722 | 29.339 | `m` / Zeit | 54,787 ms | 55,257 ms | |
| ZAST | 283 | 5.036 | `m` / Zeit | 288,561 ms | 288,173 ms | beide schlecht |
| **WOC** | 118 | 2.529 | **`pm` / Prozess** | **16,186 ms** | **1.488,784 ms** | **F6 schadet, Faktor 92** |
| **SYSTEM** | 5 | 151 | **`pm` / Prozess** | **1,771 ms** | **3.267,541 ms** | **F6 schadet, Faktor 1.845** |
| **NXHBE** | 0 | 9 | **`pm` / Prozess** | **1,128 ms** | **3.386,439 ms** | **F6 schadet, Faktor 3.002** |
| **EDITIONLINGERI** | 0 | 0 | `m` / Zeit | **2.313,808 ms** | 2.292,054 ms | **beide schlecht** |

Die beiden Zeilenspalten stammen aus `message_rollup` (Regel L2 — kein zweiter Durchlauf über die
Quelle); `EDITIONLINGERI` steht dort überhaupt nicht und hat damit **keine einzige** Nachricht.

**F6 repariert genau einen Mandanten und zerstört drei.** Das ist die zweite Zeile des Tors:
*„Eine Fassung hilft dem einen und schadet dem anderen — nicht bauen. Melden."*

### Die zwei Planfamilien, und warum keine für alle richtig ist

Der Optimierer wählt heute zwischen genau zwei Wegen, und beide sind an einer Stelle blind:

| | **Zeit-getrieben** (`m` range über `MessageLastUpdateIDX`) | **Prozess-getrieben** (`pm` → `mp` → `m` ref über `ProejctIDIDX`) |
|---|---|---|
| Was er liest | den Zeitbereich, Zeile für Zeile, **bis 51 Treffer beisammen sind** | **alle Zeilen aller Prozesse des Mandanten, über den ganzen Bestand**, danach erst Zeitfilter und Sortierung |
| Sortierung | kommt aus dem Index — **kein `filesort`** | `Using temporary; Using filesort` |
| Kosten hängen an | wie **dünn** der Mandant im Fenster liegt | wie **viel** der Mandant insgesamt hat |
| Gut für | dichte Mandanten: `NEXANS` 1,861 ms bei 180.154 Zeilen im Fenster | dünne Mandanten: `NXHBE` 1,128 ms bei 9 Zeilen insgesamt |
| Schlecht für | dünne: `EDITIONLINGERI` **2,3 s** für null Zeilen, `ZAST` 289 ms für 283 | volumige: `SUTTONS` **1,03 s** für 197.158 Zeilen insgesamt |

**Die Schätzung, an der der Optimierer wählt, ist für jeden Mandanten dieselbe.** `ProejctIDIDX`,
`Message_ProcessFK` und `MessageStatusIDX` tragen alle drei die `CARDINALITY` **18** — der Befund
aus M83 —, und daraus folgt `3.560.486 / 18 = 197.804` geschätzte Zeilen je Prozessnachschlag. Diese
197.804 stehen in **jedem** prozess-getriebenen Plan dieser Runde, ob der Mandant neun Zeilen hat
oder zweihunderttausend. Dass die Wahl bei sechs von zehn Mandanten trotzdem richtig herauskommt,
ist keine Leistung der Schätzung.

**Und die 197.804 erklären zugleich den Zufall, aus dem offener Punkt 57 entstanden ist:**
`SUTTONS` hat 197.158 Zeilen insgesamt. Der Mandant, bei dem die Schätzung fast genau stimmt, ist
der, bei dem der Plan am teuersten ist.

> **Ein Index, der beide Familien billig machte, existiert nicht — und darf nicht entstehen.**
> Gebraucht würde `(ProcessID, MessageLastUpdate)` auf `Message`: Der Prozessnachschlag käme dann
> schon im Zeitfenster an. Vorhanden ist nur die umgekehrte Reihenfolge
> (`MessageLastUpdateProcessMessageIDX`, Spalten `MessageLastUpdate, ProcessID, MessageID`). Ihn
> anzulegen hieße, auf `GlassfishDB` zu schreiben — die erste unverhandelbare Regel des Projekts.

### Vier teure Fälle, und drei davon stehen in keinem offenen Punkt

1. **`EDITIONLINGERI` kostet heute 2.313,808 ms** — ohne jeden Eingriff, ohne Filter, im zulässigen
   Wert `zeitraum=30d`. Der Mandant hat keine einzige Nachricht; die Abfrage liest deshalb das ganze
   Fenster, ohne je 51 Zeilen zu finden. Das ist mehr als das Doppelte des `SUTTONS`-Falls, den
   Punkt 57 beschreibt.
2. **`ZAST` kostet 288,561 ms**, aus demselben Grund, eine Größenordnung darunter.
3. **Der Prozessfilter kostet bei `NEXANS` über 30 Tage 7.459,912 ms**, wenn die gewählten Prozesse
   viel Verkehr tragen. Der Lese-Pool bricht bei **10 s** ab ([`datenzugriff.md`](datenzugriff.md)
   §1) — das sind **74,6 %** der Grenze, warm gemessen, und der Kaltfaktor aus M44 geht bis 9,66.
   **Diese Kombination stirbt in Produktion.** Sie ist heute über die Prozessauswahl (§8.2) mit zwei
   Klicks erreichbar.
4. **`SUTTONS` mit 1.034,784 ms** — der bekannte Fall aus Punkt 57, hier bestätigt.

**Die Lesart „kleiner Mandant = langsam" aus Punkt 57 trägt nicht.** `SYSTEM` und `NXHBE` sind die
kleinsten und zugleich die schnellsten. Was den Plan bestimmt, sind zwei Zahlen, die gegeneinander
laufen: Dichte im Fenster und Gesamtvolumen.

### Das Tor, angewandt

| Zeile des Auftrags | Trifft zu? |
|---|---|
| Eine Fassung bringt `SUTTONS` unter 50 ms **und** verschlechtert `NEXANS` nicht messbar → **bauen** | F6 erfüllt das — **aber nur, solange man die anderen acht Mandanten nicht misst** |
| Eine Fassung hilft dem einen und schadet dem anderen → **nicht bauen. Melden** | **Ja. Das ist die Lage.** F6 schadet `WOC`, `SYSTEM` und `NXHBE` um Faktor 92 bis 3.002; F1, F2 und F4 schaden schon bei den beiden Mandanten des Auftrags |
| Keine Fassung trägt → **nicht bauen. Melden, mit allen Plänen im Volltext** | Ebenfalls ja, wenn man „trägt" als „ohne Schaden" liest |

**Gebaut wird nichts.** Der Auftrag nennt für die zweite Zeile die Lehre aus M42, und sie passt hier
Wort für Wort: Ein Eingriff, der die Planwahl festnagelt, hilft dem Fall, an dem er gemessen wurde,
und trifft die anderen ungesehen.

**Damit entfällt auch der Plantest aus A.3** — es gibt keine gewählte Fassung, deren Plan er
festhalten könnte. Der Plantest **für den Parameter `ueberfaellig`** entsteht trotzdem, weil Teil B
ihn eigenständig verlangt; er steht in §5b.

### Was es bräuchte — Vorlage an den Auftraggeber, nicht Vorschlag zur Umsetzung

Drei Wege sind denkbar; **keiner ist gemessen**, und keiner gehört in diesen Schritt.

1. **Zwei Abfrageformen, ausgewählt an einer Zahl aus `message_rollup`.** Der Rollup weiß seit
   Schritt 10a je Mandant und Zeitraum, wie viele Nachrichten im Fenster liegen und wie viele
   insgesamt — die Tabelle oben ist genau daraus gerechnet und kostet Millisekunden. Damit ließe
   sich die Planfamilie **wählen** statt raten. Das ist ein Entwurf und keine Kleinigkeit: Es macht
   aus einem Endpunkt zwei Formen, und die Schwelle wäre eine gewählte Zahl.
2. **Ein Index `(ProcessID, MessageLastUpdate)` auf `Message`.** Er löste beide Familien auf einmal
   und träfe auch den Prozessfilter. Er verlangt einen Schreibzugriff auf `GlassfishDB` und damit
   eine Entscheidung des Betreibers des Altsystems — **nicht dieses Projekts**.
3. **`optimizer_switch` je Sitzung.** Der Lese-Pool setzt schon `max_statement_time`; er könnte
   ebenso `semijoin=off` setzen. Das wirkte auf **jede** Abfrage der Anwendung, nicht nur auf diese,
   und ist deshalb der weitreichendste der drei. Ungemessen.

**Bis dahin gilt:** Der Endpunkt bleibt, wie er ist, und die vier teuren Fälle stehen unter
„Offene Punkte".

### Die Pläne (Regel L15)

**Eine Abweichung, und sie ist zu benennen:** Diese Runde hat **126** Pläne erhoben — 63 Fälle, je am
nackten Statement und an der Hülle. Sie alle im Volltext zu führen hieße, dieselben fünf
`eq_ref`-Zeilen hundertfach zu wiederholen. Im Volltext stehen unten die **elf**, die die
Entscheidung tragen; für **jeden** gemessenen Fall steht die Treiberzeile — Tabelle, Zugriffsart,
Index, `key_len`, `rows`, `Extra` — in der Übersicht darunter. Die vollständigen Pläne lassen sich
über die Sitzungsdateien unter `scripts/messung-schritt10b-1/` jederzeit neu erzeugen.


#### Übersicht: die Treiberzeile jedes gemessenen Falls

| Sitzung | Fall | Treiber | Zugriff | Index | `key_len` | `rows` | `Extra` | beste von fünf |
|---|---|---|---|---|---:|---:|---|---:|
| `entscheidung-nexans` | `F0-NEXANS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,861 ms |
| `entscheidung-nexans` | `F6-NEXANS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,789 ms |
| `entscheidung-nexans` | `F0-NEXANS-30d-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,250 ms |
| `entscheidung-nexans` | `F6-NEXANS-30d-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,242 ms |
| `entscheidung-nexans` | `F0-NEXANS-30d-ueberfaellig` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,282 ms |
| `entscheidung-nexans` | `F6-NEXANS-30d-ueberfaellig` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,253 ms |
| `entscheidung-nexans` | `F0-NEXANS-30d-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,803 ms |
| `entscheidung-nexans` | `F6-NEXANS-30d-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,713 ms |
| `entscheidung-suttons` | `F0-SUTTONS-30d-referenz` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.034,784 ms |
| `entscheidung-suttons` | `F6-SUTTONS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 7,093 ms |
| `entscheidung-suttons` | `F0-SUTTONS-30d-referenz-mitCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.149,164 ms |
| `entscheidung-suttons` | `F6-SUTTONS-30d-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 436.322 | where | 5,499 ms |
| `entscheidung-suttons` | `F0-SUTTONS-30d-ueberfaellig` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,081 ms |
| `entscheidung-suttons` | `F6-SUTTONS-30d-ueberfaellig` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 6,991 ms |
| `entscheidung-suttons` | `F0-SUTTONS-30d-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,646 ms |
| `entscheidung-suttons` | `F6-SUTTONS-30d-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,623 ms |
| `gegenprobe-filter-nexans` | `F0-NEXANS-24h-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 13.534 | where | 1,782 ms |
| `gegenprobe-filter-nexans` | `F4-NEXANS-24h-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 13.534 | where | 1,790 ms |
| `gegenprobe-filter-nexans` | `F0-NEXANS-7d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 52.752 | where | 1,729 ms |
| `gegenprobe-filter-nexans` | `F4-NEXANS-7d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 52.752 | where | 1,736 ms |
| `gegenprobe-filter-nexans` | `F0-NEXANS-30d-prozess` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 17 | where; index; temporary; filesort | 7.437,391 ms |
| `gegenprobe-filter-nexans` | `F4-NEXANS-30d-prozess` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 17 | where; index; temporary; filesort | 2.909,663 ms |
| `gegenprobe-filter-nexans` | `F0-NEXANS-30d-fehler` | `m` | range | `MessageStatusIDX` | 123 | 6.257 | index condition; where; filesort | 23,404 ms |
| `gegenprobe-filter-nexans` | `F4-NEXANS-30d-fehler` | `m` | range | `MessageStatusIDX` | 123 | 6.257 | index condition; where; filesort | 23,378 ms |
| `gegenprobe-filter-suttons` | `F0-SUTTONS-24h-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 13.534 | where | 8,128 ms |
| `gegenprobe-filter-suttons` | `F4-SUTTONS-24h-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 13.534 | where | 7,346 ms |
| `gegenprobe-filter-suttons` | `F0-SUTTONS-7d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 52.752 | where | 7,253 ms |
| `gegenprobe-filter-suttons` | `F4-SUTTONS-7d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 52.752 | where | 6,780 ms |
| `gegenprobe-filter-suttons` | `F0-SUTTONS-30d-prozess` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 432,106 ms |
| `gegenprobe-filter-suttons` | `F4-SUTTONS-30d-prozess` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.751,134 ms |
| `gegenprobe-filter-suttons` | `F0-SUTTONS-30d-fehler` | `m` | range | `MessageStatusIDX` | 123 | 6.257 | index condition; where; filesort | 23,725 ms |
| `gegenprobe-filter-suttons` | `F4-SUTTONS-30d-fehler` | `m` | range | `MessageStatusIDX` | 123 | 6.257 | index condition; where; filesort | 23,973 ms |
| `gegenprobe-prozessfilter-nexans` | `F0-NEXANS-30d-prozess-gross` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 17 | where; index; temporary; filesort | 7.459,912 ms |
| `gegenprobe-prozessfilter-nexans` | `F4-NEXANS-30d-prozess-gross` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 17 | where; index; temporary; filesort | 2.983,384 ms |
| `gegenprobe-prozessfilter-nexans` | `F0-NEXANS-30d-prozess-klein` | `m` | range | `ProejctIDIDX` | 147 | 134 | index condition; where; filesort | 1,730 ms |
| `gegenprobe-prozessfilter-nexans` | `F4-NEXANS-30d-prozess-klein` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 17 | where; index; temporary; filesort | 1.547,772 ms |
| `gegenprobe-prozessfilter-suttons` | `F0-SUTTONS-30d-prozess-gross` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 446,649 ms |
| `gegenprobe-prozessfilter-suttons` | `F4-SUTTONS-30d-prozess-gross` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.796,381 ms |
| `gegenprobe-prozessfilter-suttons` | `F0-SUTTONS-30d-prozess-klein` | `m` | range | `ProejctIDIDX` | 147 | 740 | index condition; where; filesort | 5,887 ms |
| `gegenprobe-prozessfilter-suttons` | `F4-SUTTONS-30d-prozess-klein` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.544,238 ms |
| `mandanten-1` | `F0-VOTG-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 36,602 ms |
| `mandanten-1` | `F6-VOTG-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 36,000 ms |
| `mandanten-1` | `F0-IBIS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 44,384 ms |
| `mandanten-1` | `F6-IBIS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 44,602 ms |
| `mandanten-1` | `F0-IBISGUS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 54,787 ms |
| `mandanten-1` | `F6-IBISGUS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 55,257 ms |
| `mandanten-1` | `F0-ZAST-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 288,561 ms |
| `mandanten-1` | `F6-ZAST-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 288,173 ms |
| `mandanten-2` | `F0-NXHBE-30d-referenz` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 2 | where; index; temporary; filesort | 1,128 ms |
| `mandanten-2` | `F6-NXHBE-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | index condition; where; temporary; filesort | 3.386,439 ms |
| `mandanten-2` | `F0-EDITIONLINGERI-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 2.313,808 ms |
| `mandanten-2` | `F6-EDITIONLINGERI-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 2.292,054 ms |
| `mandanten-2` | `F0-WOC-30d-referenz` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 16,186 ms |
| `mandanten-2` | `F6-WOC-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1.488,784 ms |
| `mandanten-2` | `F0-SYSTEM-30d-referenz` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 2 | where; index; temporary; filesort | 1,771 ms |
| `mandanten-2` | `F6-SYSTEM-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | index condition; where; temporary; filesort | 3.267,541 ms |
| `nexans-f0` | `F0-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,786 ms |
| `nexans-f0` | `F0-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,189 ms |
| `nexans-f0` | `F0-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,170 ms |
| `nexans-f0` | `F0-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,639 ms |
| `nexans-f1` | `F1-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 8,212 ms |
| `nexans-f1` | `F1-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 51,422 ms |
| `nexans-f1` | `F1-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 12,891 ms |
| `nexans-f1` | `F1-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 52,987 ms |
| `nexans-f2` | `F2-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,795 ms |
| `nexans-f2` | `F2-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,049 ms |
| `nexans-f2` | `F2-ueberfaellig-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 129,759 ms |
| `nexans-f2` | `F2-ueberfaellig-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 389.409 | where | 1,976 ms |
| `nexans-f3` | `F3-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,720 ms |
| `nexans-f3` | `F3-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,248 ms |
| `nexans-f3` | `F3-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,555 ms |
| `nexans-f3` | `F3-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,651 ms |
| `nexans-f4` | `F4-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,826 ms |
| `nexans-f4` | `F4-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,219 ms |
| `nexans-f4` | `F4-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,199 ms |
| `nexans-f4` | `F4-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,841 ms |
| `nexans-f5` | `F5-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,829 ms |
| `nexans-f5` | `F5-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,216 ms |
| `nexans-f5` | `F5-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,444 ms |
| `nexans-f5` | `F5-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 6,283 ms |
| `nexans-f6-a` | `F6-NEXANS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1,857 ms |
| `nexans-f6-a` | `F6-NEXANS-30d-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 437.035 | where | 2,282 ms |
| `nexans-f6-a` | `F6-NEXANS-30d-ueberfaellig` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 6,071 ms |
| `nexans-f6-a` | `F6-NEXANS-30d-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 6,209 ms |
| `nexans-f6-b` | `F6-NEXANS-24h-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 13.534 | where | 1,803 ms |
| `nexans-f6-b` | `F6-NEXANS-7d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 52.752 | where | 1,736 ms |
| `nexans-f6-b` | `F6-NEXANS-30d-fehler` | `m` | range | `MessageStatusIDX` | 123 | 6.257 | index condition; where; filesort | 25,050 ms |
| `nexans-f6-b` | `F6-NEXANS-30d-prozess-gross` | `mp` | range | `PRIMARY` | 146 | 3 | where; temporary; filesort | 7.456,150 ms |
| `nexans-f6-b` | `F6-NEXANS-30d-prozess-klein` | `m` | range | `ProejctIDIDX` | 147 | 134 | index condition; where; filesort | 1,826 ms |
| `suttons-f0` | `F0-referenz-ohneCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.037,139 ms |
| `suttons-f0` | `F0-referenz-mitCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.137,384 ms |
| `suttons-f0` | `F0-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 6,904 ms |
| `suttons-f0` | `F0-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,349 ms |
| `suttons-f1` | `F1-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 4,598 ms |
| `suttons-f1` | `F1-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 436.322 | where | 6,511 ms |
| `suttons-f1` | `F1-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 4,518 ms |
| `suttons-f1` | `F1-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 5,451 ms |
| `suttons-f2` | `F2-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 7,841 ms |
| `suttons-f2` | `F2-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 436.322 | where | 6,211 ms |
| `suttons-f2` | `F2-ueberfaellig-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 1.379,042 ms |
| `suttons-f2` | `F2-ueberfaellig-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 436.322 | where | 1.367,099 ms |
| `suttons-f3` | `F3-referenz-ohneCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.139,967 ms |
| `suttons-f3` | `F3-referenz-mitCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.132,900 ms |
| `suttons-f3` | `F3-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,437 ms |
| `suttons-f3` | `F3-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 8,942 ms |
| `suttons-f4` | `F4-referenz-ohneCursor` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 7,653 ms |
| `suttons-f4` | `F4-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 436.322 | where | 5,448 ms |
| `suttons-f4` | `F4-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,190 ms |
| `suttons-f4` | `F4-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,314 ms |
| `suttons-f5` | `F5-referenz-ohneCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.117,393 ms |
| `suttons-f5` | `F5-referenz-mitCursor` | `pm` | ref | `ProjectMandant_Mandant_idx` | 146 | 1 | where; index; temporary; filesort | 1.113,599 ms |
| `suttons-f5` | `F5-ueberfaellig-ohneCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 8,445 ms |
| `suttons-f5` | `F5-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 7,447 ms |
| `suttons-f6-a` | `F6-SUTTONS-30d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 7,359 ms |
| `suttons-f6-a` | `F6-SUTTONS-30d-referenz-mitCursor` | `m` | range | `MessageLastUpdateIDX` | 151 | 436.322 | where | 6,940 ms |
| `suttons-f6-a` | `F6-SUTTONS-30d-ueberfaellig` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 8,321 ms |
| `suttons-f6-a` | `F6-SUTTONS-30d-ueberfaellig-mitCursor` | `m` | range | `MessageStatusIDX` | 123 | 539 | index condition; where; filesort | 8,667 ms |
| `suttons-f6-b` | `F6-SUTTONS-24h-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 13.534 | where | 6,994 ms |
| `suttons-f6-b` | `F6-SUTTONS-7d-referenz` | `m` | range | `MessageLastUpdateIDX` | 5 | 52.752 | where | 7,493 ms |
| `suttons-f6-b` | `F6-SUTTONS-30d-fehler` | `m` | range | `MessageStatusIDX` | 123 | 6.257 | index condition; where; filesort | 25,572 ms |
| `suttons-f6-b` | `F6-SUTTONS-30d-prozess-gross` | `m` | range | `MessageLastUpdateIDX` | 5 | 437.150 | where | 6,373 ms |
| `suttons-f6-b` | `F6-SUTTONS-30d-prozess-klein` | `m` | range | `ProejctIDIDX` | 147 | 740 | index condition; where; filesort | 6,003 ms |

#### Die elf Pläne im Volltext

#### F0 · NEXANS · 30 Tage, ohne Cursor — die heutige Fassung

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                          | 437150 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
```

#### F0 · SUTTONS · 30 Tage, ohne Cursor — die heutige Fassung

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    1 | PRIMARY     | mp    | ref    | PRIMARY,Process_ProjectFK                                                              | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using index                                               |
|    1 | PRIMARY     | m     | ref    | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
```

#### F6 · SUTTONS · 30 Tage, ohne Cursor

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX       | 5       | NULL                                          | 437150 | Using where              |
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 1      | Using where; Using index |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+--------------------------+
```

#### F0 · NXHBE · 30 Tage — 1,128 ms

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 2      | Using where; Using index; Using temporary; Using filesort |
|    1 | PRIMARY     | mp    | ref    | PRIMARY,Process_ProjectFK                                                              | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using index                                               |
|    1 | PRIMARY     | m     | ref    | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
```

#### F6 · NXHBE · 30 Tage — 3.386,439 ms

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+---------------------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                                                               |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+---------------------------------------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX       | 5       | NULL                                          | 437150 | Using index condition; Using where; Using temporary; Using filesort |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where                                                         |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                                         |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1      |                                                                     |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                                         |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                                         |
|    1 | PRIMARY     | pm    | range  | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | NULL                                          | 2      | Using where; Using index; Using join buffer (flat, BNL join)        |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+---------------------------------------------------------------------+
```

#### F0 · EDITIONLINGERI · 30 Tage — 2.313,808 ms, ohne jeden Eingriff

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                          | 437150 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
```

#### F2 · NEXANS · 30 Tage, ueberfaellig — 129,759 ms

```
+------+-------------+-------+--------+------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table | type   | possible_keys                      | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+-------+--------+------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | m     | range  | MessageLastUpdateIDX               | MessageLastUpdateIDX | 5       | NULL                                          | 437150 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                            | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                            | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                            | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK            | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+-------+--------+------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
```

#### F0 · NEXANS · 30 Tage, Prozessfilter (drei kleine) — 1,730 ms

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+--------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key          | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+--------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX | 147     | NULL                                          | 134  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY      | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY      | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY      | 146     | GlassfishDB.m.ProcessID                       | 1    |                                                    |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY      | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY      | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY      | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+--------------+---------+-----------------------------------------------+------+----------------------------------------------------+
```

#### F4 · NEXANS · 30 Tage, Prozessfilter (drei kleine) — 1.547,772 ms

```
+------+-------------+-------+--------+---------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+------------------------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                           | key                        | key_len | ref                                           | rows   | Extra                                                                  |
+------+-------------+-------+--------+---------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+------------------------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                      | ProjectMandant_Mandant_idx | 146     | const                                         | 17     | Using where; Using index; Using temporary; Using filesort              |
|    1 | PRIMARY     | mp    | ref    | PRIMARY,Process_ProjectFK                               | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using where; Using index                                               |
|    1 | PRIMARY     | m     | range  | MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX       | 5       | NULL                                          | 437150 | Using index condition; Using where; Using join buffer (flat, BNL join) |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                 | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                 | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                                            |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                 | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                                            |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                 | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                                            |
+------+-------------+-------+--------+---------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+------------------------------------------------------------------------+
```

#### F0 · NEXANS · 30 Tage, Prozessfilter (drei grosse) — 7.459,912 ms

```
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 17     | Using where; Using index; Using temporary; Using filesort |
|    1 | PRIMARY     | mp    | ref    | PRIMARY,Process_ProjectFK                                                              | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using where; Using index                                  |
|    1 | PRIMARY     | m     | ref    | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
```

#### F1 · NEXANS · 30 Tage, mit Cursor — 51,422 ms

```
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+-------------+
| id   | select_type | table | type   | possible_keys                                                                                  | key                  | key_len | ref                                           | rows   | Extra       |
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+-------------+
|    1 | SIMPLE      | m     | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 151     | NULL                                          | 437035 | Using where |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where |
|    1 | SIMPLE      | s     | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where |
|    1 | SIMPLE      | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                        | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where |
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+-------------+
```


---

## 5b. `ueberfaellig` — ein Parameter, aber eine zweite Abfrageform (27.08.2026)

**Entscheidung E‑j, gebaut in Schritt 10b‑1.** Ein boolescher Parameter an `GET /api/nachrichten`,
Vorgabe **aus**. Er zeigt genau die Nachrichten, für die eine Frist abgelaufen ist.

> ### ⚠️ Der Wortlaut „ein neuer Parameter" trägt nicht. Das gehört an den Anfang und nicht in eine Fußnote.
>
> `ueberfaellig` ist **kein Filter auf der bestehenden Abfrage, sondern eine zweite Abfrageform im
> selben Endpunkt** (offener Punkt 56, M97). Er
>
> - zieht den Treiber von `MessageLastUpdateIDX` auf **`MessageStatusIDX`**,
> - macht die Sortierung zum **`filesort`** — der Statusindex liefert die Sortierfolge nicht,
> - und **entwertet den Cursor**: `key_len` bleibt bei 123 statt auf 151 zu steigen, die
>   Cursor-Bedingung wird nachgelagert geprüft.
>
> **Die Cursor-Messung aus M4/L8 gilt für diese Form nicht.** Wer den Endpunkt anfasst, hat es mit
> zwei Plänen zu tun und nicht mit einem.
>
> Das ist **kein Fehler und kein Grund, ihn nicht zu bauen** — die Laufzeit liegt bei 5,2 bis
> 7,8 ms, bei beiden Mandanten. Es ist eine Eigenschaft, die man kennen muss, bevor man daneben
> etwas baut.

### Das Prädikat: aufgerufen, nicht nachgebaut

Die Bedingung ist das SQL-Gegenstück zu `MessageStatusClassifier.istUeberfaellig` und liegt
**neben ihm**, in derselben Klasse (`ueberfaelligBedingung`). Gerendert:

```sql
MessageStatus IN ('RUNNING','SUSPENDED')
  AND MessageTimeout IS NOT NULL
  AND MessageTimeout > 0
  AND date_add(MessageLastUpdate, INTERVAL MessageTimeout SECOND) < ?
```

| | |
|---|---|
| **Die Statusmenge ist gezogen, nicht getippt** | Sie entsteht aus `istEndstatus` (`offeneRohwerte()`). Wäre sie hier aufgezählt, ergäbe ein neuer offener Statuswert **zwei** Wahrheiten: eine für die Detailansicht und eine für die Liste. `MessageStatusClassifierTest` hält beide Fassungen für jeden bekannten Rohwert gegeneinander |
| **Ein `IN` ist hier vollständig, nicht verkürzt** | Anders als beim Statusfilter: `istEndstatus` liefert für jeden **unbekannten** Wert `true` — unbekannt heißt Endstatus heißt „kann nicht überfällig werden". Die offene Menge ist damit geschlossen |
| **Kein `MessageLastUpdate IS NOT NULL`** | In SQL ist `NULL + INTERVAL … SECOND` selbst `NULL`, der Vergleich also nicht wahr. Die Bedingung wäre wirkungslos und wiche von der gemessenen Fassung ab |
| **Der Stichtag kommt aus der Anwendungsuhr** | Ein Uhrenschlag je Anfrage, im Service gelesen und über `Nachrichtenabfrage` durchgereicht — ein Repository liest keine Uhr (Regel Z1). Mit der Systemuhr wäre lokal jede offene Zeile überfällig |
| **Die Einheit steht an zwei Stellen** | `ChronoUnit.SECONDS` für Java, `DatePart.SECOND` für SQL. Ein Auseinanderlaufen wäre im Betrieb unsichtbar — aus dreißig Minuten würden dreißig Stunden. Ein Test hält sie gegeneinander |

`NachrichtenStatementsTest` hält den gerenderten Text Zeichen für Zeichen fest. Regel L7 verlangt
die Messung **der** Abfrage; die Zahlen unten sind an genau diesem Text erhoben, nicht an einem
nachgebauten.

### Die eine unvereinbare Kombination — `400`, keine leere Liste

`ueberfaellig=true` zusammen mit einem Statusfilter, der weder `WARTEND` noch `LAEUFT` enthält, ist
**ohne Rücksicht auf die Daten** leer. Der Endpunkt antwortet darauf `400` mit dem Problemtyp
`ueberfaellig-und-status-unvereinbar`.

**Warum nicht einfach eine leere Liste.** Sie hieße „in diesem Zeitfenster gibt es nichts" — eine
Auskunft über den Bestand. Wahr ist etwas anderes: Die Frage widerspricht sich selbst, und kein
Zeitfenster ändert daran etwas. Das ist derselbe Fall wie „Suchbegriff zu kurz" (Richtlinie §5.5,
*Anfrage fachlich unbrauchbar*). Die Antwort nennt die zulässigen Statuswerte, damit die Oberfläche
die Menge nicht ein zweites Mal kennen muss.

**Ohne Statusfilter greift die Prüfung nicht** — eine leere Auswahl heißt „alle" und enthält die
offenen Status. `ueberfaellig=true` allein ist der Normalfall.

> **`UNGEKLAERT` fällt unter die Ablehnung**, und das ist kein Versehen. `istEndstatus` liefert
> dafür `true`; die Weigerung, etwas zu behaupten, ist keine Offenheit
> ([`message-status.md`](message-status.md)).

### Gemessen am gebauten Statement (Regel L7, 27.08.2026)

Testkopie, Anker `2025-12-30 04:09:47`, 30‑Tage‑Fenster, `LIMIT 51`, Aufwärmlauf und dann beste von
fünf. **Die Statements sind die von jOOQ gerenderten**, aus dem Repository abgegriffen und nur in
die Messhülle gepackt; Sitzungen `b-gebaut-nexans.sql` und `b-gebaut-suttons.sql`.

| Fall | Mandant | Treiber / Index | `key_len` | `rows` | `Extra` | **beste von fünf** |
|---|---|---|---:|---:|---|---:|
| Referenz, ohne Cursor | NEXANS | `Message` / `MessageLastUpdateIDX` | 5 | 437.150 | `where` | 1,806 ms |
| Referenz, mit Cursor | NEXANS | `Message` / `MessageLastUpdateIDX` | **151** | 437.035 | `where` | 2,265 ms |
| **`ueberfaellig`, ohne Cursor** | NEXANS | `Message` / **`MessageStatusIDX`** | 123 | **539** | `index condition; where; **filesort**` | **5,227 ms** |
| **`ueberfaellig`, mit Cursor** | NEXANS | `Message` / **`MessageStatusIDX`** | **123** | 539 | `… filesort` | **5,851 ms** |
| Referenz, ohne Cursor | SUTTONS | `ProjectMandant` / `ProjectMandant_Mandant_idx` | 146 | 1 | `where; index; temporary; filesort` | 1.153,294 ms |
| Referenz, mit Cursor | SUTTONS | `ProjectMandant` / `ProjectMandant_Mandant_idx` | 146 | 1 | `… temporary; filesort` | 1.169,565 ms |
| **`ueberfaellig`, ohne Cursor** | SUTTONS | `Message` / **`MessageStatusIDX`** | 123 | **539** | `index condition; where; filesort` | **7,243 ms** |
| **`ueberfaellig`, mit Cursor** | SUTTONS | `Message` / **`MessageStatusIDX`** | 123 | 539 | `… filesort` | **7,781 ms** |

**Die Pläne sind Zeile für Zeile die aus M97**, obwohl dort mit der Hand geschriebenes SQL gemessen
wurde und hier der gerenderte Text steht. Zwei Unterschiede im Text, beide ohne Wirkung: jOOQ
schreibt `date_add(x, INTERVAL y SECOND)` statt `x + INTERVAL y SECOND` und `FETCH NEXT 51 ROWS
ONLY` statt `LIMIT 51`. MariaDB bildet beides auf dasselbe ab.

**Drei Beobachtungen, die dazugehören:**

1. **Der Parameter *rettet* `SUTTONS`.** Die Referenzliste kostet dort 1,15 s (offener Punkt 57,
   §5a), die überfällige 7,2 ms — Faktor **159**. Bei `NEXANS` ist es umgekehrt: Der Parameter
   kostet dort Faktor 2,9. Derselbe Parameter, entgegengesetzte Wirkung.
2. **`key_len` bleibt bei 123, mit und ohne Cursor.** Ohne den Parameter hebt der Cursor sie von 5
   auf 151, also auf beide Spalten des Zeitindex. Mit ihm bringt er dem Plan **nichts** — er filtert
   nur noch nachgelagert. `NachrichtenPlanDbIT` hält genau das fest.
3. **`rows = 539` bei beiden Mandanten.** Der Statusindex kennt keinen Mandanten; der Bereich ist
   für beide derselbe, die Mandantenkette wirkt erst danach. Auf dieser Testkopie sind **alle 538
   überfälligen Zeilen `NEXANS`-Zeilen** — `SUTTONS` bekommt null, und das ist der Ausgangspunkt
   des Isolationstests.

### Der Cursor blättert trotzdem richtig — nachgewiesen über elf Seiten

**Das ist die Frage, die offener Punkt 56 aufwirft**: Wenn der Cursor kein Indexbereich mehr ist,
blättert er dann noch korrekt? `NachrichtenUeberfaelligDbIT` weist es nach, über den Dezember 2025
bei `NEXANS` (538 Zeilen, `limit=50`):

| | |
|---|---|
| Seite 1 → Seite 2 | 50 Zeilen je Seite, **keine gemeinsame Kennung**, Seite 2 schließt lückenlos an |
| alle Seiten | **elf Seiten, 538 Zeilen, keine doppelt** — und über alle Seitengrenzen hinweg absteigend sortiert |
| Vergleichsgröße | die 538 aus M97. Weicht sie ab, ist die Testkopie neu befüllt und nicht der Code kaputt |

**Warum elf Seiten und nicht zwei.** Zwei Seiten zeigen, dass der Cursor greift; sie zeigen nicht,
dass er über eine lange Folge nichts verliert. Ein Blättern, das eine Zeile überspringt, fällt bei
zwei Seiten nicht auf.

### Der Plantest — er hält fest, was heute gilt, nicht was gut ist

`NachrichtenPlanDbIT` liest `EXPLAIN` und prüft **Treibertabelle und Index, nicht die Laufzeit**.

**Die Begründung ist im Projekt belegt:** `BamIsolationDbIT` misst Wanduhrzeit gegen eine
Faktor‑10‑Schranke und wird gelegentlich grundlos rot (279 ms gegen 20 ms). *Ein Sicherheitstest,
der zufällig rot wird, wird nach der dritten Wiederholung nicht mehr gelesen.* Ein Plantest ist
deterministisch und prüft die Ursache statt ihres Schattens.

Er läuft für **beide** Mandanten (Regel L7) und prüft drei Dinge:

1. Mit dem Parameter steigt die Abfrage bei beiden über `MessageStatusIDX` ein, als `range`, mit
   `filesort`.
2. Der Cursor ändert daran nichts — derselbe Plan mit und ohne ihn.
3. **Ohne** den Parameter steht `MessageStatusIDX` in keiner Planzeile. Ohne diese Gegenprobe
   zeigte der Test nur, dass irgendein Plan herauskommt.

**Was er absichtlich nicht prüft:** auf welchem Plan die *Referenzliste* läuft. Der hängt am
Mandanten (§5a), und diese Wahl ist heute ein Zufallstreffer der Statistik (offener Punkt 65).
Einen Zufall festzuschreiben hieße, den Test bei der ersten Statistikänderung rot zu machen, ohne
dass jemand etwas falsch gemacht hätte.

**Er ist einmal absichtlich gebrochen worden** und war rot: Mit ausgehängter Bedingung stand dort
`MessageLastUpdateIDX` statt `MessageStatusIDX`. Die Änderung ist zurückgenommen.

### Der Isolationstest (Regel M4)

Regel M4 verlangt ihn für **jeden Endpunktzustand** — auch für einen neuen Parameter am bestehenden
Endpunkt. Hier ist er nicht Formalie: Ein zweiter Plan ist ein zweiter Ort, an dem der
Mandantenfilter fehlen kann.

**Und er ist schärfer als der gewöhnliche, weil die Daten es hergeben.** Alle überfälligen Zeilen
des Gesamtbestands gehören `NEXANS` — 538, `SUTTONS` hat keine einzige. **Fiele der Mandantenfilter
aus dieser Abfrageform heraus, sähe `SUTTONS` nicht ein paar fremde Zeilen, sondern genau diese
538.** Die erwartete Null ist damit die schärfste Zusage, die dieser Bestand hergibt.

Drei Tests in `NachrichtenIsolationDbIT`:

- `SUTTONS` bekommt mit `ueberfaellig=true` **null** Zeilen, während `NEXANS` 538 sieht; keine
  Kennung von `NEXANS` steht im Antwortrumpf.
- Ein fremder, **existierender** Prozess und eine **erfundene** Kennung liefern auch mit
  `ueberfaellig=true` eine **ununterscheidbare** Antwort.
- Ein fremder Cursor öffnet keinen fremden Ausschnitt — hier ausdrücklich zu prüfen, weil der Plan
  den Cursor in dieser Form anders behandelt. Was sich am Plan ändert, darf sich an der Trennung
  nicht ändern.

### Was der Parameter auf dieser Testkopie zeigt

| Fenster | überfällige Zeilen |
|---|---:|
| 24 h ab dem Anker (das Standardfenster) | **1**, bei `NEXANS` |
| 30 Tage | **538**, alle bei `NEXANS`, alle `SUSPENDED`, alle mit Frist 1.800 s |
| jeder andere Mandant, jedes Fenster | **0** |

Das ist ein Befund über den Bestand und nicht über die Kategorie; er steht seit Schritt 4 so in
[`message-status.md`](message-status.md).


---

## 5c. Fensterverengung über `message_rollup` — gemessen, nicht gebaut (27.08.2026)

§5a endet mit dem Satz, es bräuchte „zwei Abfrageformen, ausgewählt an einer Zahl aus
`message_rollup`" — als Vorlage an den Auftraggeber, nicht als Vorschlag zur Umsetzung. Diese Runde
hat den Gedanken gemessen, in einer Fassung, die **keine zweite Abfrageform** baut: Der Rollup weiß,
in welchen Stunden ein Mandant Nachrichten hat; man fragt die Quelle nur nach diesen Stunden und
überlässt die Planwahl dem Optimierer.

**Nichts ist gebaut worden.** Alle Zahlen stehen in
[`messungen-liste-verengung.md`](messungen-liste-verengung.md) (M99 bis M103, sieben Sitzungen,
zehn Mandanten).

### Wie sie rechnet

Vom Fensterende rückwärts über die Stundeneimer summieren, bis die kumulierte `anzahl` **≥ 51**
ist; diese Stunde wird die neue Untergrenze. Angebrochene Randstunden und Stunden oberhalb des
Wasserstands (`MAX(fenster_bis)` aus `rollup_lauf`) gehen mit **0** ein. Die gezählte Menge ist damit
stets eine **Unterschranke** — die Verengung geht nie zu weit und kann keine Zeile verlieren.

### Was sie leistet

| Fall | heute | verengt, samt Vorabfrage | |
|---|---:|---:|---|
| **NEXANS, Prozessfilter, drei verkehrsreiche Prozesse** | **7.459,912 ms** *(§5a)* / 7.519,513 ms *(neu)* | **6,797 ms** | **1.106×** |
| **EDITIONLINGERI**, ohne Filter | **2.313,808 ms** *(§5a)* / 2.126,876 ms *(neu)* | **17,454 ms** | **122×** |
| **SUTTONS**, ohne Filter | **1.034,784 ms** *(§5a)* / 1.043,011 ms *(neu)* | **8,605 ms** | **121×** |
| **ZAST**, ohne Filter | **288,561 ms** *(§5a)* / 270,891 ms *(neu)* | 275,370 ms | **unverändert** |

Der teuerste bekannte Fall des Projekts — die Kombination, von der §5a schreibt, sie „stirbt in
Produktion" — kostet verengt **unter 7 ms**. Der Plan zeigt warum: Der Zugriff auf `Message`
wechselt von `ref` über `ProejctIDIDX` mit den geschätzten **197.804** Zeilen (der
`CARDINALITY`‑18‑Befund aus M83) auf `range` über `MessageLastUpdateIDX` mit **358**. Die *erste*
Planzeile bleibt dabei unverändert — die Entscheidung liegt in der vierten.

**Und der Optimierer bleibt, wo er richtig liegt.** `WOC`, `SYSTEM` und `NXHBE` behalten ihre
prozessgetriebene Form; genau sie hatte Fassung F6 aus §5a um Faktor 92 bis 3.002 zerstört. Ein
Fenster zu geben ist etwas anderes, als eine Form zu erzwingen.

### Woran es scheitert

**An der Vorabfrage, nicht am Plan.** Sie kostet jeden Mandanten etwas, und die dünnen am meisten:
Der Nachweis „weniger als 51 Zeilen in dreißig Tagen" muss **21.274** Rollup-Zeilen lesen, weil
`message_rollup` nach `(stunde, process_id, message_status)` geschlüsselt ist und keinen
Sekundärindex trägt. Diese Mandanten sind heute schon schnell.

| | heute | verengt | |
|---|---:|---:|---|
| NXHBE | 0,980 ms | 17,584 ms | 17,9× schlechter |
| SYSTEM | 1,695 ms | 18,290 ms | 10,8× schlechter |
| WOC | 15,102 ms | 32,256 ms | 2,14× schlechter |
| VOTG / IBIS / IBISGUS | 33,7 / 41,4 / 52,1 ms | +2 bis +4 ms | 1,03× bis 1,10× schlechter |

**Das ist die zweite Zeile des Tors, und sie gilt:** *„Ein Mandant wird schlechter — nicht bauen.
Melden."* Dieselbe Zeile wie bei F6 in §5a, aber aus einem anderen Grund: Dort schadete der **Plan**
unbegrenzt, hier ein **additiver Betrag** von höchstens rund 18 ms — auf einer Tabelle, die diesem
Projekt gehört.

### Der Index auf der eigenen Tabelle — gemessen, und er reicht nicht

Weil `message_rollup` uns gehört, ist die naheliegende Abhilfe geprüft worden (**M104**, an einer
Probetabelle nach dem Vorbild von M89, angelegt, gemessen, gelöscht — Löschung nachgewiesen; die
echte Tabelle ist unberührt). Ein Index `(process_id, stunde)` **wirkt genau dort, wo es weh tut**:

| | Vorabfrage, 30 Tage | |
|---|---|---|
| NXHBE | 97,756 → **0,962 ms** | 101,6× |
| SYSTEM | 96,997 → **0,985 ms** | 98,5× |
| EDITIONLINGERI | 101,920 → **1,079 ms** | 94,5× |
| WOC | 88,877 → **1,501 ms** | 59,2× |
| ZAST | 102,942 → **2,686 ms** | 38,3× |

Der Plan dreht sich um: Statt alle 21.274 Rollupzeilen zu lesen und je Zeile die Mandantenkette zu
prüfen, geht er von den wenigen Prozessen des Mandanten in den Rollup. Auf den engen Stufen kostet
der Index nichts. Er kostet **16,6 MiB** neben 21,6 MiB Daten und 1,3 s Aufbauzeit.

**Und er öffnet das Tor trotzdem nicht.** Der Schaden schrumpft um eine Größenordnung — NXHBE von
120,1× auf 6,8×, SYSTEM von 69,9× auf 4,9×, WOC von 8,1× auf 1,5×; `EDITIONLINGERI` fällt auf
**3,348 ms**, Faktor 635 —, aber **keine Zeile kippt von „schlechter" auf „besser".** Was übrig
bleibt, ist ein Sockel von 3,4 bis 4,4 ms auf der **24‑Stunden‑Stufe**, den der Index nicht anrührt;
`NXHBE` liegt heute bei 0,980 ms. Dazu eine Bedingung: In der Literalfassung macht derselbe Index
die engen Stufen *teurer* (NEXANS 2,204 → 7,813 ms) — **Index und Abfragefassung sind eine
Entscheidung, nicht zwei.**

Offener Punkt **73** ist damit beantwortet; was jetzt im Weg steht, steht als Punkt **76** in der
Messdatei.

### Zwei Befunde, die über diese Runde hinaus gelten

**Die Verengung kann einem bereits zeitgetriebenen Mandanten prinzipiell nichts sparen.** Der
zeitgetriebene Plan liest vom Fensterende rückwärts und hört bei der 51. Zeile auf — genau dort
setzt die Verengung die Untergrenze. Beide lesen dieselben Stunden. Das ist der Fall `ZAST`, und er
ist nicht durch eine bessere Fassung zu heilen (Punkt **70**).

> **⚠️ Eine Verengung, die nicht jeden Filter der Quellabfrage mitträgt, verliert stillschweigend
> Zeilen.** Gemessen mit dem Statusfilter `FEHLER`: `SUTTONS` liefert **null statt fünf** Zeilen,
> `NEXANS` **49 statt 51** — ohne Fehlermeldung. Der Rollup meldet 51 Nachrichten in der letzten
> Stunde, die Verengung schneidet darauf zu, und in dieser Stunde liegt kein einziger Fehler. Ein
> Mandant sähe „keine Fehler" und hätte fünf. **Wer der Liste künftig einen Filter hinzufügt, fügt
> zwei Dinge hinzu** (Punkt **72**).

`ueberfaellig` trägt die Verengung ohnehin nicht — `MessageTimeout` und `jetzt` stehen nicht im
Rollup. Der Rückfallpfad ist gemessen und unverändert: NEXANS 4,993 ms, SUTTONS 6,631 ms.


---

## 5d. Die Fensterverengung, gebaut (30.08.2026)

§5c endet mit einem geschlossenen Tor: Die Annahme trägt, der Optimierer wechselt bei engem Fenster
von selbst die Planfamilie — aber die Vorabfrage kostet die dünnen Mandanten mehr, als die Verengung
ihnen bringt. Der Auftraggeber hat am 27.08.2026 entschieden, den Index `(process_id, stunde)` auf
`message_rollup` anzulegen und die Verengung damit zu bauen.

**Gebaut ist:** `V11__message_rollup_prozess_index.sql`, die Klassen `Fensterverengung`,
`VerengungRepository`, `Verengungsgrenzen`, `Abfragemerkmal` und `Verengungsgrund` in `message/`, der
Schalter `overlord.nachrichtenliste.verengung`. **Nicht gebaut ist** eine Behandlung von `ZAST` —
Punkt 70 zeigt, dass die 275 ms strukturell nicht durch eine Verengung zu heilen sind.

### Der Befund, der den Bau prägt

> **Eine Verengung, die nicht jeden Filter der Quellabfrage mitträgt, verliert stillschweigend
> Zeilen** (offener Punkt 72). Gemessen: mit `status=FEHLER` liefert sie für `SUTTONS` **null statt
> fünf** Zeilen — ohne Fehlermeldung. **In einem Überwachungswerkzeug ist „keine Fehler" die
> schlimmste falsche Antwort, die es gibt.**

Sie ist deshalb **umgekehrt** gebaut: Sie greift nicht standardmäßig und setzt bei bekannten
Ausnahmen aus, sondern greift **nur**, wenn für **jedes** gesetzte Merkmal ausdrücklich entschieden
ist, dass der Rollup es trägt. Der Nachweis steht nicht in einer Prüfliste, sondern in zwei Riegeln,
die zusammen keine Lücke lassen:

| Riegel | Was er fängt |
|---|---|
| `Abfragemerkmal` — ein Wert je Bestandteil von `Nachrichtenabfrage`; `Fensterverengung` entscheidet darüber in **zwei** vollständigen `switch`-Ausdrücken ohne `default` | Ein neuer **Enum-Wert** bricht den Bau, zweimal |
| `FensterverengungMerkmaleTest` geht über `Nachrichtenabfrage.class.getRecordComponents()` und verlangt für jeden Bestandteil einen zugeordneten Wert | Ein neues **Feld** im Record — das merkt der Compiler nicht |

**Was der Rollup trägt:** Zeitfenster, Status (er speichert den Rohwert, E‑g, und
`MessageStatusClassifier.bedingung` nimmt ein beliebiges `Field<String>` — es ist *derselbe
Ausdruck*, nur mit anderem Feld), Prozesse, Cursor, Limit, Stichtag.

**Was er nicht trägt** — und jeder dieser drei Fälle fällt auf den heutigen Pfad zurück:

| | Warum |
|---|---|
| `ueberfaellig` | `MessageTimeout` steht nicht im Rollup und lässt sich aus `anzahl` nicht rekonstruieren. Das Nächste, was er könnte, wäre „offen" — und das ist nicht „überfällig" |
| Suchbegriff | Der Suchtreffer wirkt als `ProcessID IN (…) OR SOSID IN (…)`, und `message_rollup` hat **keine `sos_id`**. Eine Verengung nur über den Prozesszweig wäre **zu eng** |
| Sortierung `AELTESTE` | Dort stehen die ersten Zeilen am **Anfang** des Fensters; eine Untergrenze verschöbe die erste Seite und nicht den Suchraum. Die gespiegelte Rechnung wäre richtig, ist aber nicht gemessen — offener Punkt **77** |

Die **BAM-Suche** kennt die Verengung nicht einmal; `PaketstrukturTest` hält das als Regel fest.

### Wie sie rechnet — und warum sie nie zu weit geht

Vom Fensterende rückwärts über die Stundeneimer summieren, bis die kumulierte `anzahl` **≥ limit+1**
ist. Drei Dinge sorgen dafür, dass die gezählte Menge stets eine **Unterschranke** der wirklichen ist:

1. **Nur vollständig im Fenster liegende Stunden zählen.** Die Eimer sind halboffen
   (`[stunde, stunde+1h)`), das Fenster beginnt und endet fast immer mitten in einer Stunde. Die
   beiden angebrochenen Randstunden gehen mit **0** ein. Zählte man die obere voll mit, wäre es eine
   **Ober**schranke — läge dort ein Eimer mit sechzig Nachrichten, von denen nur fünf vor dem
   Fensterende liegen, hielte man 51 Zeilen für gefunden und lieferte fünf.
2. **Nie über den Wasserstand hinaus** (`MAX(fenster_bis)` über abgeschlossene, fehlerfreie Läufe).
3. **Nur bei Merkmalen, die der Rollup mitträgt** — siehe oben.

`Verengungsgrenzen` ist deshalb ein eigener Typ: Diese Rechnung ist die Stelle, an der ein Fehler
**Zeilen kostet** statt Laufzeit, und sie wird ohne Datenbank bei jedem Build geprüft.

**Gestuft gefragt** wird in drei Schritten — eine Stunde, vierundzwanzig, das ganze Fenster —, und
abgebrochen, sobald eine Stufe trägt. Eine einzige Abfrage über dreißig Tage kostet `NEXANS` 91 ms,
während seine Antwort in der letzten Stunde steht.

**Der Nullfall:** Meldet der Rollup null Zeilen, wird gegen `Message` **gar nicht gefragt**.

### Die Abschlussmessung (M106, alle zehn Mandanten)

Gemessen ist, was der Code schickt — die Statements sind aus `VerengungRepository` und
`NachrichtenRepository` **gerendert** und von dort abgeschrieben (Regel L7). Aufwärmlauf, dann beste
von fünf. Die Vorabfrage steigt bei allen zehn Mandanten über **`message_rollup_prozess_idx`** ein
(`ref`, 240 geschätzte Zeilen) — der Index aus `V11` wirkt.

**Über dreißig Tage:**

| Fall | heute | Vorabfrage | Quelle | **Summe** | |
|---|---:|---:|---:|---:|---|
| **NEXANS, Prozessfilter (drei verkehrsreiche)** | **7.433,790 ms** | 1,388 | 6,086 | **7,474 ms** | **995×** |
| **EDITIONLINGERI** | **2.211,771 ms** | 6,335 | *(Nullfall)* | **6,335 ms** | **349×** |
| **SUTTONS** | **1.107,092 ms** | 5,548 | 6,635 | **12,183 ms** | **91×** |
| ZAST | 284,824 ms | 8,069 | 278,476 | 286,545 ms | unverändert |
| IBISGUS | 53,515 ms | 5,519 | 54,501 | 60,020 ms | +6,5 ms |
| IBIS | 43,965 ms | 5,604 | 43,531 | 49,135 ms | +5,2 ms |
| VOTG | 34,518 ms | 5,749 | 33,037 | 38,786 ms | +4,3 ms |
| WOC | 15,713 ms | 6,443 | 16,051 | 22,494 ms | +6,8 ms |
| NEXANS, Prozessfilter (drei kleine) | 1,787 ms | 3,323 | 1,760 | 5,083 ms | +3,3 ms |
| NEXANS, ohne Filter | 1,645 ms | 1,444 | 1,639 | 3,083 ms | +1,4 ms |
| SYSTEM | 1,582 ms | 6,177 | 1,585 | 7,762 ms | +6,2 ms |
| NXHBE | 1,019 ms | 6,186 | *(Nullfall)* | 6,186 ms | +5,2 ms |

Der teuerste bekannte Fall des Projekts — die Kombination, von der §5a schreibt, sie „stirbt in
Produktion" — kostet **7,474 ms**. **Der höchste Aufschlag über alle zwölf Fälle beträgt 6,8 ms**,
und er trifft Fälle, die heute zwischen 1 und 16 ms liegen.

> ### ⚠️ Und beim Vorgabefenster kehrt sich das Bild um
>
> **24 Stunden ist der Wert, den die Liste ohne Parameter nimmt** (Regel L1) — nicht dreißig Tage.
> Dort greift bei vier Mandanten der **Nullfall**, und die Zahlen drehen sich:
>
> | Fall | heute | **verengt** | |
> |---|---:|---:|---|
> | **NXHBE** | **98,962 ms** | **5,212 ms** | **19,0× besser** |
> | **SYSTEM** | **98,358 ms** | **5,155 ms** | **19,1× besser** |
> | **ZAST** | **62,986 ms** | **5,380 ms** | **11,7× besser** |
> | **EDITIONLINGERI** | **60,703 ms** | **5,324 ms** | **11,4× besser** |
> | WOC | 82,996 ms | 85,913 ms | +2,9 ms |
> | IBISGUS / IBIS / VOTG | 54,0 / 42,8 / 34,6 ms | +4,0 bis +4,9 ms | |
> | SUTTONS | 6,777 ms | 12,147 ms | +5,4 ms |
> | NEXANS | 1,680 ms | 3,060 ms | +1,4 ms |
>
> **Genau die vier Mandanten, die über dreißig Tage am meisten aufschlagen, sind über das
> Vorgabefenster die größten Gewinner.** Der Grund steht im Plan: Über 24 Stunden laufen sie
> **nicht** über die Mandantenkette, sondern über den Zeitindex — und lesen dessen 13.534 Zeilen
> vergeblich, weil sie im Fenster gar keine Nachricht haben. Über dreißig Tage wählt der Optimierer
> für sie die prozessgetriebene Form, die bei neun Zeilen Gesamtbestand sehr billig ist.
>
> **Das ist ein Befund über die heutige Liste, nicht über die Verengung**, und er war bis hierher
> unbekannt: `NXHBE` und `SYSTEM` kosten über das **Vorgabefenster** rund 98 ms und über dreißig Tage
> 1 ms. §5a hat nur dreißig Tage gemessen. Offener Punkt **78**.

### Die vorregistrierten Erwartungen, dagegengehalten

| Erwartung aus C.5 | |
|---|---|
| `NEXANS` mit Prozessfilter unter 10 ms | **trifft** — 7,474 ms |
| `SUTTONS` unter 15 ms | **trifft** — 12,183 ms |
| `ZAST` unverändert bei rund 275 ms | **trifft** — 286,545 ms |
| `NXHBE` / `SYSTEM` / `WOC` höchstens rund 8 ms schlechter | **trifft** — +5,2 / +6,2 / +6,8 ms |
| `EDITIONLINGERI` unter 5 ms | **verfehlt — 6,335 ms** |

**Die eine Verfehlung ist klein und ihre Ursache benannt:** `EDITIONLINGERI` durchläuft alle drei
Stufen (er hat keine Zeile, also trägt keine), und die 24‑Stunden‑Stufe allein kostet **3,950 ms**.
Dazu kommt die Wasserstandsabfrage mit 0,422 ms, die in der Erwartung nicht mitgedacht war. Die
Schranke von 5 ms war gegen die Zahlen aus M104 gesetzt, die beides nicht enthielten.
**Nachgebessert ist nichts** — der Aufschlag ist die 24‑Stunden‑Stufe, und die ist offener Punkt
**76**.

### Der Schalter

`overlord.nachrichtenliste.verengung`, Vorgabe `true`. Die Verengung ist die **erste Stelle, an der
eine Kernabfrage von einer Tabelle abhängt, die dieses Projekt selbst fortschreibt**. Fällt der
Rollup aus, läuft die Liste unverengt weiter — langsamer, aber richtig. **Ein fehlender Schlüssel
schaltet ab, nicht ein.**

### Die Tests

| Test | Was er sichert |
|---|---|
| `FensterverengungDbIT` (27 Fälle) | **Verengt und unverengt liefern dieselben Zeilen** — alle zehn Mandanten, 24 h und 30 d, jede der acht Statusarten einzeln, zwei Seiten mit Cursor. Dazu ein Fall, der belegt, dass die Verengung überhaupt **greift**: Ohne ihn wäre die Gleichheitsprüfung grün, auch wenn sie stillschweigend nie zuschlüge |
| `FensterverengungGrenzenTest` | Die Grenzrechnung **ohne Datenbank**, bei jedem Build: Randstunden, Wasserstand über/mitten/unter dem Fenster, Cursor, Stufenbildung |
| `FensterverengungMerkmaleTest` | Der Riegel gegen ein neues Feld — und dass die Rückfallfälle das Repository **gar nicht anfassen** |
| `NachrichtenPlanDbIT` (3 neue Fälle) | Dass `SUTTONS` bei engem Fenster auf den Zeitindex wechselt, dass `NEXANS` seine Planfamilie **behält**, und dass die Vorabfrage nicht über einen vollen Durchlauf der Rolluptabelle geht |
| `PaketstrukturTest` | Die BAM-Suche kennt die Verengung nicht einmal |


---

## 5e. ⚠️ Die Oberfläche kannte `ueberfaellig` nicht *(01.09.2026)*

**Befund, gefunden beim Bau des Dashboard-Frontends.** Der Parameter steht seit Schritt 4 im
Endpunkt (§1, §5b) und hat seither in **keiner** Stelle der Oberfläche existiert: nicht in
`NACHRICHTEN_PARAMETER`, nicht in `Nachrichtenfilter`, nicht in `alsAbfrage`, nicht als
Bedienelement.

**Folgenlos war das, solange es keinen Weg dorthin gab.** Die Filterleiste bietet ihn nicht an,
und niemand tippt `?ueberfaellig=true` von Hand. Mit dem Dashboard gibt es einen Weg: Die Kachel
*Überfällig, im Fenster* verweist genau hierher (Entscheidung E‑m in
[`dashboard-frontend.md`](dashboard-frontend.md)).

> **Ohne den Parameter wäre der Verweis eine Lüge.** `nuqs` überginge ihn stillschweigend wie jeden
> unbekannten Suchparameter; der Nutzer landete auf der **ungefilterten** Liste — mit dem
> Zeitfenster der Kachel und ohne jeden Hinweis darauf, dass die eine Bedingung fehlt, um
> derentwillen er geklickt hat. Eine Liste, die mehr zeigt, als sie verspricht, ist in einem
> Überwachungswerkzeug dieselbe Art Fehler wie eine, die zu wenig zeigt.

**Gebaut ist deshalb der Parameter und sonst nichts:**

| | |
|---|---|
| Parser | `ueberfaellig: parseAsBoolean.withDefault(false)` — dieselbe Bauform wie `langeSuche` |
| **ohne `clearOnDefault: false`** | Die Vorgabe `false` lässt nichts weg, sie zeigt alles. *Ein Standardwert, der etwas weglässt, gehört in die URL; einer, der etwas zulässt, nicht* ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) |
| Anfrage | `ueberfaellig=true` **nur, wenn gesetzt**. Ein `false` wäre die Vorgabe ein zweites Mal und machte den Abfrageschlüssel des Zwischenspeichers unnötig verschieden |
| Anzeige | eine **sichtbare, entfernbare Marke** in der Filterleiste |
| **kein Einschalter** | Es gibt keine Schaltfläche, die die Form *einschaltet* |

### Warum eine Marke und kein Schalter

**Ein Filter, der die Liste einschränkt und nirgends steht, ist genau der Zustand, der den
Ausblende-Schalter am 11.08.2026 gekostet hat** (§5): Er versteckte gemessen ausgerechnet die
Zeile, die der Nutzer suchte, und niemand sah, dass etwas fehlte. Die Marke trägt die
Marken-Gestalt des Projekts (`components/marke.tsx`) und **keine Farbrolle** — sie sagt etwas über
den *Ausschnitt* und nichts über einen Zustand.

**Ein Einschalter wäre dagegen eine Gestaltungsentscheidung über die Liste** — welchen Platz er in
der Leiste bekommt, wie er neben dem Statusfilter steht, ob er die Vorwahlen verdrängt. Die ist in
diesem Schritt von niemandem getroffen worden, und ein Frontend-Schritt für das Dashboard ist nicht
der Ort, sie nebenbei zu treffen. Sie steht als offener Punkt **89**.

### Die beiden erscheinen nie zusammen

`ueberfaellig=true` und ein `status`, der weder `WARTEND` noch `LAEUFT` enthält, sind am Endpunkt
unvereinbar und ergeben `400` `ueberfaellig-und-status-unvereinbar` (§5b). **Die Oberfläche lässt
den Zustand gar nicht erst entstehen:** Eine Statuswahl beendet die Überfälligkeitsform —
`ohneUeberfaelligBeiStatus` in `features/nachrichten/filter.ts`, dieselbe Bauform, mit der
`mitVorwahl` und `mitFreiemFenster` die beiden Zeitfenstermodi auseinanderhalten.

**Gelöscht wird auch dann, wenn die Wahl zulässig wäre** — etwa `status=WARTEND`. Das ist Absicht:
`ueberfaellig` ist *kein Filter, sondern eine zweite Abfrageform*, und wer einen Status wählt, wählt
die erste. Eine Regel, die je nach gewähltem Status etwas anderes täte, wäre an der Oberfläche nicht
abzulesen.

Sieben Fälle in `tests/nachrichtenfilter.test.ts` halten das fest: der geteilte Link, die Vorgabe in
Anfrage **und** URL, der Rundlauf, und die drei Lagen der Ausschlussregel.

---

## 6. Die BAM-Werte sind aus der Liste heraus — und warum

Bis zur Nachbesserung von Schritt 4 trug jede Zeile zwei BAM-Spalten, nachgeladen in einer zweiten
Abfrage je Seite. Sie sollten das sein, woran ein Sachbearbeiter seinen Beleg wiedererkennt:
Lieferschein-Nr., Bestellnummer, Transport-Nummer. **Im Betrieb waren sie leer.**

Beim ersten Durchklicken durch den Auftraggeber stand die erste Spalte auf **jeder sichtbaren
Zeile** leer, und die zweite lieferte siebenundzwanzig laufende Positionsnummern je Nachricht.
Messung [M11](messungen-schritt4.md) sagt, wie systematisch das ist:

| | |
|---|---|
| Typ 9006 („Lieferschein-Nr._L_SAP"), kuratiert | auf **98,93 %** der Zeilen leer |
| Typ 9001 („Abrufnummer_L_SAP"), kuratiert | auf **96,97 %** der Zeilen leer, im Schnitt 4,42 Werte, im Höchstfall 238 |
| bestbelegter der **40** konfigurierten Typen | **16,25 %** |
| Typen, die im ganzen Monat kein einziges Mal befüllt sind | 2 |

**Der Auftraggeber hat also nicht eine unglückliche Seite erwischt, sondern den Normalfall
gesehen.** Und es ist keine Frage der Auswahl: Eine BAM-Spalte, die *immer* etwas zeigt, gibt es
bei `NEXANS` nicht. Auch die zwei bestbelegten Typen ließen vier von fünf Zeilen leer.

> ⚠️ **Der letzte Satz gilt für die Zeilen, die diese Liste zeigt — nicht für die, die sie
> ausblendet** (gemessen am 10.08.2026,
> [`messungen-schritt6.md`](messungen-schritt6.md#m28--rollenverteilung-kandidatenprädikate-und-der-bam-typ-je-rolle)
> M28‑2). Über die **Split-Wurzeln** von `NEXANS` gemessen liegt Typ **9018**
> („Kundenmaterialnummer_K_SAP") bei **92,26 %**, und vier weitere Typen liegen über 83 %. Bei `IBIS`
> erreicht Typ 0 („Bestellnummer") **92,59 %** der Wurzeln. Eine Spalte, die *fast* immer etwas zeigt,
> gibt es also sehr wohl — nur nicht auf der Menge, die hier betrachtet wurde.
>
> **M11 wird davon nicht widerlegt.** Auf den **Kindern**, also auf dem, was die Liste heute
> tatsächlich anzeigt, liegt die beste Quote bei `NEXANS` bei **0,72 %**. Die beiden kuratierten
> Typen stehen auf den Wurzeln bei 5,02 % (9006) und 11,79 % (9001) — die Kuratierung aus Schritt 4
> traf also auch die Wurzeln nicht, weil sie ohne Rücksicht auf die Stellung in der Kette gewählt
> wurde; diese Stellung war damals nicht gemessen.
>
> ✅ **Entschieden am 11.08.2026: Die Spalte kommt hier nicht zurück.** Die Frage gehört zu
> Schritt 7 und braucht eine eigene Messung — die Kuratierung 9006/9001 stammt aus Schritt 4 und
> wurde ohne Rücksicht auf die Stellung in der Kette gewählt; sie trifft auf den Wurzeln 5,02 %
> beziehungsweise 11,79 % (M28‑2). Eine Spalte zurückzuholen, deren Typ nachweislich der falsche
> ist, wäre derselbe Fehler mit umgekehrtem Vorzeichen.
>
> **Was sich am 11.08.2026 trotzdem geändert hat, gehört dazu:** Die Zeilen, auf denen die Werte
> sitzen, sind jetzt **sichtbar** — der Ausblende-Schalter ist gefallen (§5). Der Befund aus E1
> („beide kuratierten Spalten sind auf der Vorgabe-Ansicht praktisch leer") beschreibt damit einen
> Zustand, den es nicht mehr gibt; die Spalten selbst sind seit der Nachbesserung zu Schritt 4
> ohnehin draußen. Was bleibt, ist die Vorarbeit für Schritt 7: **Wo ein Wert sitzt, hängt an der
> Rolle und nicht am Status.**
>
> > **Belegvermerk** *(nachgetragen am 10.08.2026 nach Regel L10)*.
> > *Gemessen (M11):* Über **alle** 180.251 `NEXANS`-Zeilen des dichten Monats erreicht kein
> > konfigurierter BAM-Typ mehr als **16,25 %**.
> > *Behauptet war:* Eine BAM-Spalte, die immer etwas zeigt, gibt es bei `NEXANS` nicht — „das ist
> > keine Frage der Auswahl, sondern der Datenlage".
> > **Die Lücke:** „über alle Zeilen" ist nicht „über jede Teilmenge". Der Satz hat aus einer
> > Aussage über die **Gesamtmenge** eine über **jede beliebige Auswahl** gemacht. Genau daran
> > scheitert er: Auf den Split-Wurzeln liegt Typ 9018 bei 92,26 % (M28‑2). **`n` war hier so groß
> > wie möglich** — und die Menge trotzdem die falsche. Dass sie sich überhaupt maschinell
> > schneiden lässt, war zum Zeitpunkt von M11 nicht gemessen; die vier Verkettungsangaben sind
> > erst in M23 bis M28 erhoben worden.

Damit fällt die Spalte unter dieselbe Regel, die §8.1 seit Schritt 4 für einen Mandanten ohne
BAM-Konfiguration formuliert: **Eine Spalte ohne Inhalt behauptet, es gäbe dort etwas zu sehen.**
Der Unterschied war nur, dass sie hier je Zeile leer ist statt je Mandant — und dass sie den Platz
kostete, den die übrigen Spalten brauchen.

**Was verschwindet:** die zwei Spalten, das Feld `bamWerte` in der Antwort, die zweite Abfrage je
Seite (`findeBamWerte`) und die Spaltenauflösung im Paket `message`.

**Was bleibt, unangetastet:** die Tabelle `bam_spalte` mit ihrer Migration, die Auflösungsregel
`common/BamSpaltenRegel` samt Test und die Kuratierung in [`datenzugriff.md`](datenzugriff.md) §5.
**Schritt 7 braucht beides** — dort entsteht die BAM-Suche, und M11 ist ausdrücklich als Vorarbeit
dafür erhoben: Sie sagt, welche Felder ein Suchfeld anbieten sollte und welche leer sind. Der
Datenzugriff entsteht dann im Paket `bam` und nicht hier; Fachpakete kennen einander nicht (§7).

> **Der zweite Befund aus M11 gehört dazu, weil er die Suche in Schritt 7 betrifft und nicht die
> Liste.** Die Zahl der Werte je Nachricht ist der größere Fallstrick: Bei den Typen 9027, 9028 und
> 9029 stehen auf zehn Nachrichten je 15.790 Werte — **1.579 im Schnitt, bis zu 3.035 auf einer
> einzigen Nachricht**. Ein Suchfeld über solche Typen liefert keine Belegnummer, sondern eine
> Positionsliste. Die Deckelung aus Regel L5 ist damit nicht Vorsicht, sondern Voraussetzung.

---

## 7. Aufbau im Code

```
common/                              message/
├─ Zeitfenster, Zeitraum             ├─ NachrichtenController   REST, nimmt nie eine Mandanten-ID
├─ Seitenposition, Seite             ├─ NachrichtenService      Fachlogik, Einordnung, Zeitpunkte
├─ Sortierrichtung                   ├─ NachrichtenFilter       geprüfte Parameter
├─ Zeitpunkte  (Wanduhr ↔ UTC)       └─ NachrichtResponse       DTO nach außen
├─ BamSpalte, BamSpaltenRegel
└─ MessageStatusClassifier
```

**`NachrichtenMerkmaleResponse` ist am 11.08.2026 entfallen** — mit dem Endpunkt, den es beschrieb
(§1, §5). Ebenso entfallen sind `NachrichtenRepository.hatZwischenschritte`, der Zwischenspeicher im
`NachrichtenService` samt seiner zweiten Uhr und `MessageStatusClassifier.ohne(…)`, das ausschließlich
für `zwischenschritte=false` existierte.

Was in `common` liegt, liegt dort, weil ein zweites Fachpaket es braucht: Zeitfenster, Cursor und
Sortierung gehören zu **jedem** Listen-Endpunkt (Schritt 6, 7 und 10 folgen), die BAM-Spaltenregel
ab Schritt 7 dem Paket `bam`.

**`message/BamSpaltenRepository` ist mit der Nachbesserung entfallen** (§6). Die Regel in `common`
bleibt und behält ihren Test; ihr *Datenzugriff* entsteht in Schritt 7 neu im Paket `bam`, weil er
dort gebraucht wird und nicht mehr hier. Ein ungenutztes Statement in `message` stehen zu lassen,
hätte Schritt 7 nicht geholfen — es hätte kopiert werden müssen, denn Fachpakete kennen einander
nicht.

### Warum die BAM-Regel in `common` liegt, der Zugriff darauf aber nicht

Die Auflösungsregel aus [`datenzugriff.md`](datenzugriff.md) §5 steht als reine Rechenlogik in
`common/BamSpaltenRegel` — sie wird ab Schritt 7 von `bam` (Suchfelder) gebraucht, und Fachpakete
kennen einander nicht.

Der **Datenzugriff** kann dort trotzdem nicht liegen, und zwar aus zwei Regeln, die zusammen nicht
erfüllbar sind:

- `common` darf von keinem anderen Anwendungspaket abhängen — also auch nicht von `MandantContext`
  aus `security`.
- Regel M2 verlangt `MandantContext` als **ersten Pflichtparameter** jeder Methode, die
  `jooq.glassfish` anfasst. `PaketstrukturTest` prüft beides maschinell.

Eine Klasse in `common`, die das Quellschema liest, müsste also gleichzeitig `MandantContext`
verlangen und ihn nicht kennen dürfen. Getrennt wird deshalb dort, wo die Entscheidung liegt: **die
Regel gemeinsam, die Statements je Fachpaket.** Der Ausweg über rohes SQL (wie ihn `ZeitConfig`
für den Anker der Dev-Uhr geht) wäre hier falsch — er verstecke einen fachlichen Zugriff auf das
Quellschema vor genau der Prüfung, die ihn sichtbar machen soll.

---

## 8. Die Oberfläche

Entsteht in Schritt 4, Aufgaben 13 bis 15. Route `/nachrichten` im Anwendungsrahmen, Feature
`features/nachrichten` — es importiert **nicht** aus `features/sitzung`.

```
features/nachrichten/
├─ api.ts                          Typen und die Aufrufe
├─ filter.ts                       Filterzustand, rein — ohne React
├─ hooks.ts                        URL-Bindung, Blättern, Aktualisierung
└─ components/
   ├─ nachrichten-ansicht.tsx      der Zusammenbau, "use client"
   ├─ filterleiste.tsx             Zeitfenster, Status, Prozess, Suche
   ├─ prozess-filter.tsx           Mehrfachauswahl aus /api/prozesse
   ├─ nachrichten-tabelle.tsx      Spalten, Zeitpunkt, BAM-Zellen
   ├─ status-plakette.tsx          Status — nie allein über Farbe
   └─ blaettern.tsx                Seiten, Stand, automatische Aktualisierung
```

Seit Schritt 5 liegen im selben Feature die Bausteine der Detailansicht (`detail.ts`,
`nachricht-detail.tsx`, `nachricht-seite.tsx`, `zeitleiste.tsx`, `eigenschaften-block.tsx`).
Beschrieben sind sie in [`nachrichtendetail.md`](nachrichtendetail.md) §10 — sie beantworten eine
andere Frage und stehen deshalb dort, nicht hier.

`"use client"` steht so weit unten wie möglich: `page.tsx` bleibt Server-Komponente, `filter.ts` ist
frei von React (und deshalb als reine Funktion prüfbar).

### 8.1 Die Spalten

```
Zeitpunkt · Status · Ablauf · Projekt
```

**Neu geschnitten in der Nachbesserung zu Schritt 4.** Der Anspruch, an dem der alte Satz gemessen
wurde und den er verfehlt hat: **Jede Spalte muss etwas beitragen.**

**Die BAM-Spalten sind weg** (§6). Was an ihre Stelle tritt, ist keine Ersatzspalte, sondern eine,
die es schon gab und die falsch besetzt war.

**„Ablauf" zeigt `SOSName`, nicht `ProcessName`.** Die Projektbeschreibung §3.2 legt `SOSName` als
den **Anzeigenamen** fest, und er ist durchgängig in Klartext gepflegt — Messung
[L14](messungen-schritt4.md#l14--was-kosten-die-beiden-neuen-joins-der-liste): 1.818 Zeilen, kein
`NULL`, kein Leerwert, 6 bis 55 Zeichen, keiner besteht nur aus Ziffern, und auf **allen 180.251**
Nachrichten des dichten Monats auflösbar. `ProcessName` ist dagegen nur zufällig lesbar: Auf dem
Bild des Auftraggebers steht „Kunde A Lieferschein (VDA)" neben „KUNDE_B_MX_000000_LAB".

`ProcessName` geht nicht verloren — er steht im **Tooltip** der Ablaufzelle und für
Vorleseprogramme verborgen im Markup. Und der Freitextfilter durchsucht weiterhin Prozess-,
Projekt- und Ablaufnamen (§5). **Das passt jetzt zusammen: Was man sucht, sieht man auch.**

Der Join kostet 0,2 ms und ändert den Zugriffspfad nicht (L14, `eq_ref` über den Primärschlüssel
von `SOS`).

**Keine `MessageID`-Spalte.** Eine `varchar(36)`-UUID widerspricht dem Leitsatz „interne IDs sind
Beiwerk", und ohne Kopierfunktion trägt sie nichts. Sie kommt in Schritt 5 zurück, wenn es ein
Detail gibt, auf das sie zeigt.

#### Der Zeitpunkt trägt Sekunden

`29.12.2025, 23:39:14`.

Auf dem Bild des Auftraggebers standen mehrfach **zwei Zeilen mit identischem Zeitpunkt, Prozess
und Projekt** nebeneinander. Der Nutzer kann sie nicht auseinanderhalten — und für ein Werkzeug,
dessen Leitfrage „wo ist mein Beleg" lautet, sind zwei ununterscheidbare Zeilen so schädlich wie
eine leere Spalte.

**Die Sekunden stehen in der Zelle und nicht im Tooltip.** Ein Tooltip, den man je Zeile aufrufen
muss, um zwei Zeilen zu vergleichen, beantwortet die Frage nicht; auf einem Touchgerät gibt es ihn
ohnehin nicht. Der **relative** Abstand bleibt im Tooltip — er ist die Ergänzung, nie der Ersatz
([`frontend-grundlagen.md`](frontend-grundlagen.md) §4).

#### Feste Zeilenhöhe — ab jetzt die Regel für jede Spalte

> **Jede Zelle ist eine Zeile hoch. Was nicht hineinpasst, wird gekürzt; der Vollwert steht im
> `title`.**

Die Regel gilt **nicht nur** für die heutigen vier Spalten. Ohne sie zerreißt der nächste lange
Wert die Liste wieder — genau das haben die mehrwertigen BAM-Zellen getan, die untereinander bis zu
vier Zeilen hoch wurden und die Liste ungleichmäßig machten. Eine Liste mit springenden Zeilenhöhen
lässt sich nicht überfliegen, und Überfliegen ist die einzige Art, wie ein Nutzer 50 Zeilen liest.

Umgesetzt über `table-fixed` mit festen Breiten für Zeitpunkt, Status und Projekt; „Ablauf" bekommt
den Rest. Feste Spaltenbreiten sind die Voraussetzung dafür, dass eine Zelle überhaupt eine Breite
hat, auf die sich kürzen lässt. Die Höhe ist `--dichte-zeile`
([`visuelles-konzept.md`](visuelles-konzept.md) §5), die Kopfzeile der Tabelle ist enger geworden
und der Innenabstand der Zellen ebenfalls — zusammen mit den zwei entfallenen Spalten passen
spürbar mehr Zeilen ins Fenster.

#### Der aktuelle Schritt steht in der Statuszelle — bei offenen Nachrichten

Bei `WARTEND` und `LAEUFT` steht **neben** der Statusplakette der Schritt, auf dem die Nachricht
gerade steht (`schritt`, §1). Bei allen anderen erscheint dort nichts.

**Warum neben und nicht unter dem Status.** Die feste Zeilenhöhe gilt für alle Zeilen gleich. Sie
auf zwei Zeilen auszulegen kostete **jede** Zeile ein Drittel Höhe — für einen Zusatz, den in der
Testkopie 538 von 3,3 Millionen Zeilen tragen. Das widerspräche dem, wofür die Liste gerade
kompakter geworden ist. Der Zusatz steht deshalb einzeilig daneben, gekürzt, mit dem Vollwert im
Tooltip; die Statusspalte ist ab `lg` breiter, damit er dort lesbar bleibt.

**Er trägt keine eigene Farbrolle.** Er ist Beiwerk im Sinne des Leitsatzes und steht in der
gedämpften Textfarbe. Eine eigene Farbe wäre eine Statusaussage, die er nicht macht
([`visuelles-konzept.md`](visuelles-konzept.md) §3).

> ⚠️ **Diese Anzeige ist lokal kaum prüfbar.** `RUNNING` kommt in der Testkopie **null Mal** vor,
> `SUSPENDED` 538-mal und nur bis zum 2025-12-29 — und **alle 538 stehen auf demselben Schritt**
> („Send Message to Pool", M13). Was sich vorführen lässt, ist genau eine Zeile mit einem Text, nicht
> die Vielfalt, die die Produktion zeigen wird.
>
> **Der fehlende Schritt ist ein Normalfall, kein Fehler.** 43,9 Prozent aller Verweise laufen ins
> Leere (M13) — praktisch alle davon bei `FINISHED`, wo ohnehin nichts gezeigt wird. Bei den offenen
> Status ist die Verknüpfung in der Testkopie lückenlos; die Anzeige muss trotzdem mit `null`
> umgehen können, denn die Produktion muss sich daran nicht halten.
>
> **Nachgetragen 07.08.2026:** Die Herkunft dieser 43,9 Prozent ist inzwischen gemessen — sie
> entstehen aus einer **Nummerierungslücke** der Ablaufdefinition und nicht aus geänderten Abläufen
> ([`messungen-schritt5.md`](messungen-schritt5.md) M20). An der Anzeige ändert das nichts.

##### Die Zelle nennt keine Präposition — die Geschichte dieser Beschriftung

**Heute steht in der Zelle `Wartend` und daneben `Schritt: Send Message to Pool`.** Kein „vor", kein
„in", kein „auf". Das ist der dritte Stand, und die beiden davor gehören dazu, weil sie erklären,
warum es der letzte ist.

| Stand | Beschriftung | Was daran nicht stimmte |
|---|---|---|
| Schritt 4 | nur der Schrittname, Tooltip „Aktueller Schritt" | Wer „Send Message to Pool" neben `Wartend` liest, nimmt an, dieser Schritt laufe gerade. M16 (3): Bei allen 538 `SUSPENDED` ist **jede** Aktion beendet |
| Schritt 5, Teil 2 | „wartet vor: {Schritt}" / „läuft auf: {Schritt}" | **Widerlegt durch M29** (siehe unten) |
| **11.08.2026** | **„Schritt: {Schritt}"** | — die Zelle sagt, was ihre Datenquelle hergibt |

> ⚠️ **Warum „wartet vor" falsch war.**
> [M29](messungen-schritt5.md#m29--worauf-zeigt-messagesosactionid-bei-wartenden-nachrichten) hat
> über **alle 538** wartenden Nachrichten gemessen: `Message.SOSID`/`SOSActionID` zeigen ausnahmslos
> auf den Schritt, der **zuletzt gelaufen** ist — den mit `WAITUNTIL|…|SUSPEND`, der die Nachricht
> schlafen legt. Die Nachricht wartet also **in** diesem Schritt und nicht **davor**, in 538 von 538
> Fällen.
>
> **Die Berufung auf M16 (3) war ein Fehlschluss**, und es lohnt zu benennen welcher: Gemessen war
> „jede Aktion ist beendet". Daraus folgt, dass die Nachricht auf keinem laufenden Schritt steht —
> **nicht**, dass ein *nächster* aussteht. Die Zahl war richtig, die Lesart zu weit.
>
> > **Belegvermerk** *(nachgetragen am 10.08.2026 nach Regel L10)*.
> > *Gemessen (M16 3):* Bei allen 538 `SUSPENDED`-Nachrichten trägt **jede Aktion** ein
> > `MessageActionEnd`. `n = 538`, der gesamte Bestand dieses Status.
> > *Behauptet war:* Die Nachricht wartet **vor** einem Schritt, der noch aussteht.
> > **Die Lücke:** „Keine Aktion läuft" sagt nichts darüber, ob überhaupt noch eine **folgt**. Der
> > Satz hat aus einer Aussage über die *gelaufenen* Schritte eine über einen *kommenden* gemacht —
> > und genau die hat M29 widerlegt. **`n` war hier vollständig; der Umfang war nie das Problem.**
> > Das ist der Fall, an dem sich zeigt, warum `n =` allein nicht genügt.

**Warum ohne Präposition und nicht mit der richtigen.** Nicht als Kompromiss: Der Unterschied
zwischen *in* und *vor* entsteht aus dem Vergleich von `Message.SOSActionID` mit dem zuletzt
**ausgeführten** Schritt — und der steht in `MessageAction`, einer Tabelle mit 10,3 Millionen Zeilen
(M23‑1), die die Liste nach L2 und L3 nicht je Seite joinen soll. Das Detail kann es, weil es genau
eine Nachricht lädt ([`nachrichtendetail.md`](nachrichtendetail.md) §3). **Die Zelle sagt damit
genau das, was ihre Datenquelle hergibt** — und nicht mehr.

**Der verworfene Weg gehört dazu, damit er nicht in sechs Monaten als naheliegende Verbesserung
wiederkommt:** `offenerZustand` in den Listen-Endpunkt zu ziehen. Er wurde erwogen und **verworfen**
— es wäre je Zeile ein zusätzlicher Zugriff auf `MessageAction`, also genau der Join, den die Liste
seit Schritt 4 nicht macht. Der Preis stünde auf jeder Seite mit 50 Zeilen; der Gegenwert wäre ein
Wörtchen. Wer ihn trotzdem will, misst ihn vorher (L7) und begründet ihn gegen L2/L3 — nicht
umgekehrt.

**Beide Einordnungen tragen dieselbe Beschriftung.** Für `LAEUFT` war „läuft auf" ohnehin nie belegt
(`RUNNING` kommt in der Testkopie null Mal vor); die eine Formulierung trifft jetzt beide Lagen,
ohne über eine davon etwas zu behaupten.

**Die Beschriftung steht sichtbar da und nicht nur im Tooltip.** Auf einem Touchgerät gibt es keinen
Hover — dieselbe Lehre wie bei der Fußzeile zu „Bedeutung nicht verifiziert" (unten). Der `title`
trägt denselben Text ungekürzt, weil die Zelle eine Zeile hoch ist und kürzt.

> **Die Plakette weicht dem Zusatz nicht** (Sichtprüfung 07.08.2026). Mit der Beschriftung aus
> Schritt 5 stand in der Zelle zuerst `Warte…` statt `Wartend`: Die Plakette durfte schrumpfen, der
> Name daneben nicht. Genau verkehrt herum — der Status ist die Hauptinformation, der Schritt ist
> Beiwerk. Steht ein Schritt daneben, ist die Plakette `shrink-0`; ohne ihn darf sie weiter weichen,
> denn dort trägt sie bei `bedeutungNichtVerifiziert` einen Rohwert beliebiger Länge. **Das gilt
> unverändert weiter**, und der neue Text ist kürzer als der alte.

**Status nie allein über Farbe.** Jede Plakette trägt Beschriftung **und** Zeichen; die Farbrolle
ist die halbe Aussage. Bei `bedeutungNichtVerifiziert` wird der **Rohwert** zur Beschriftung, dazu
der Hinweis „Bedeutung nicht verifiziert" — statt einen plausiblen Text zu erfinden. Bei gesicherter
Einordnung steht der Rohwert im Tooltip, damit ein Anwender ihn gegen die alte Oberfläche halten
kann. Die Zuordnung Status → Farbe steht weiterhin an genau einer Stelle (`lib/status-farbe.ts`);
die Komponente kennt keine Farbe.

> **„Bedeutung nicht verifiziert" steht seit dem 07.08.2026 auch unter der Tabelle**, nicht nur im
> Tooltip. Der Grund ist der Touchscreen: **Dort gibt es keinen Hover.** Ein Nutzer sah eine
> Plakette mit einem Rohwert und einem Fragezeichen und erfuhr nie, was das heißt — er musste
> annehmen, die Anwendung sei kaputt.
>
> Es ist **eine** Fußzeile, nicht eine je Zeile: Sie erklärt eine Kennzeichnung, nicht ein
> Vorkommen. Und sie erscheint **nur, wenn eine solche Zeile auf der Seite steht.** Der Fall ist
> selten — `CHECKED`, `CKECKED` und `COMMIT_SENT` sind zusammen **0,04 Prozent** aller Zeilen
> ([`message-status.md`](message-status.md)). Eine dauerhaft stehende Fußzeile für 0,04 Prozent wäre
> Rauschen, und Rauschen unter einer Tabelle liest irgendwann niemand mehr — auch dann nicht, wenn
> es einmal zählt.

**Der Zeilenklick öffnet seit Schritt 5 die Detailansicht.** Bis dahin hatte er ausdrücklich keine
Funktion, und die Hover-Färbung aus `components/ui/table` war abgeschaltet — ein Anfassgefühl ohne
Wirkung ist schlimmer als gar keins. Jetzt trägt die Zeile beides: Zeigehand, Hover-Fläche,
Fokusring, `Tab`/`Enter`/`Leertaste`. Sie setzt den Parameter `nachricht` in der URL; was daraufhin
erscheint, steht in [`nachrichtendetail.md`](nachrichtendetail.md) §10.

Die geöffnete Zeile bleibt in der Liste erkennbar — `aria-current` und die blasse Akzenttönung,
dieselbe wie am aktiven Navigationseintrag. Sie sagt etwas über die **Anwendung** (welche Zeile
offen ist) und nicht über die Daten; eine Statusfarbe wäre hier eine Aussage, die die Zeile nicht
macht ([`visuelles-konzept.md`](visuelles-konzept.md) §3).

> **`Escape` schließt das Panel wieder** (Sichtprüfung 07.08.2026). Öffnen ging mit der Tastatur von
> Anfang an; zum Schließen hätte man durch bis zu fünfzig Zeilen tabben müssen, weil der
> Schließen-Knopf im DOM hinter der Tabelle steht. Das erfüllt „erreichbar" und verfehlt
> „bedienbar". In einem Eingabefeld und bei einem offenen Auswahlfeld bleibt `Escape`, was es ist —
> sonst täte die Taste zweierlei.

**Am schmalen Fenster** fällt zuerst und einzig **Projekt** weg (unter `md`). Übrig bleiben
Zeitpunkt, Status und Ablauf. Der aktive Mandant bleibt bei jeder Breite in der Kopfzeile sichtbar
— das entscheidet der Anwendungsrahmen (bestehende Regel aus
[`visuelles-konzept.md`](visuelles-konzept.md) §6).

### 8.2 Filter und URL

In der URL stehen: `zeitraum` **oder** `von`/`bis` · `status` · `prozess` · `suche` · `langeSuche` ·
`sortierung`.

> **`zwischenschritte` ist am 11.08.2026 aus der URL verschwunden** — mit dem Schalter, den er trug
> (§5). Er war der **einzige** Parameter, der ausdrücklich in der URL stand, auch wenn er der Vorgabe
> entsprach: Was ausgeblendet ist, muss man teilen können. Die Liste blendet nichts mehr aus, und
> damit gibt es nichts zu teilen; ohne Auswahl ist die URL jetzt leer. Mit ihm entfallen das
> `withDefault`, das `clearOnDefault: false` und der Effekt, der ihn beim ersten Rendern nachtrug.
>
> **Ein alter Link, der ihn trägt, wird nicht abgewiesen.** Er wird schlicht übergangen, wie jeder
> unbekannte Suchparameter — kein Fehler, keine Umleitung, kein Hinweis, und auch keine stille
> Umschreibung der URL. Er hat nie etwas anderes bewirkt, als das auszublenden, was jetzt ohnehin
> erscheint (§1).

Seit Schritt 5 kommt `nachricht` dazu — die geöffnete Detailansicht. Sie steht in der URL wie jeder
andere Wert und wird trotzdem **nicht** an `/api/nachrichten` geschickt; die Begründung samt der
`clearOnDefault`-Falle und dem Verlaufseintrag steht in
[`nachrichtendetail.md`](nachrichtendetail.md) §10.2.

**Der Cursor steht nicht in der URL.** Ein geteilter Link auf Seite sieben eines relativen Fensters
zeigte beim Empfänger auf andere Zeilen. Beim Öffnen eines Links beginnt die Liste auf Seite eins.
Ebenfalls nicht in der URL: der Schalter für die automatische Aktualisierung — er betrifft die
Arbeitsweise des Betrachters, nicht den gezeigten Ausschnitt.

**Das Zeitfenster als sichtbare Vorwahlen:** 24 Stunden · 7 Tage · 30 Tage · frei. „Frei" schaltet
auf `von`/`bis` um. **Beide Modi zugleich lässt die Oberfläche gar nicht erst zu** — eine Vorwahl
löscht `von`/`bis`, ein freies Fenster löscht `zeitraum`. Das Backend lehnte den Zustand mit
`zeitfenster-mehrdeutig` ab, und ein Nutzer, der über eine Schaltfläche in einen Fehlerzustand
gerät, hat keine Möglichkeit, ihn zu verstehen.

> **„Frei gewählt, aber noch nichts eingetragen" steht nicht in der URL** — und das ist keine
> Nachlässigkeit, sondern der Grund für eine eigene Funktion. Ein freies Fenster ohne beide
> Zeitpunkte ist dort von „keine Auswahl" nicht zu unterscheiden: beides ist `zeitraum=null,
> von=null, bis=null`. Es soll auch nicht unterscheidbar sein, denn beides zeigt denselben
> Ausschnitt, und der freie Modus beginnt bewusst leer (ein vorbelegtes Fenster wäre der zweite
> Standardwert). Der Zwischenzustand liegt deshalb im Komponentenzustand; `lib/filter.ts`
> `angezeigterModus` macht die Regel prüfbar. **Ohne sie ist der freie Modus über die Oberfläche
> gar nicht erreichbar** — der Klick schreibt einen Zustand, der sich vom vorherigen nicht
> unterscheidet, die Eingabefelder erscheinen nie, und nur eine von Hand gebaute URL kommt noch
> hinein. Gefunden in der Sichtprüfung am 06.08.2026.

**Kein Standardwert im Frontend.** Fehlt das Zeitfenster, setzt das Backend die 24 Stunden aus Regel
L1. Ohne Auswahl ist deshalb **keine** Vorwahl gedrückt; daneben steht der Hinweis „Ohne Auswahl
gilt das Standardfenster des Servers" — bewusst **ohne Zahl**, denn eine Zahl hier wäre der zweite
Standardwert, der dem ersten irgendwann hinterherliefe.

**Die `von`/`bis`-Felder rechnen in der Anzeigezone.** Ein `datetime-local` kennt keine Zone; läse
man seinen Wert mit `new Date()`, wäre das freie Fenster gegen die Daten verschoben, sobald jemand
nicht in der Zone des Servers sitzt — derselbe Fehler wie in Aufgabe 11, nur an der Eingabe statt an
der Anzeige. Umgerechnet wird in `lib/format.ts`, mit zwei Durchgängen, damit auch die beiden
Umstellungstage im Jahr treffen.

> **Ein halb getipptes `datetime-local` meldet sich nicht von selbst** (ergänzt 07.08.2026). Der
> Auftraggeber berichtete, „Frei" öffne die Datumsauswahl, aber nach der Eingabe passiere nichts.
> Nachgestellt im Browser — geklickt und getippt, nicht zugewiesen —: Wer nur das **Datum** einträgt
> und die Uhrzeit auslässt, sieht `01.12.2025` im Feld stehen und bekommt trotzdem `value === ""`.
> Ein `datetime-local` liefert seinen Wert erst, wenn **alle** Segmente stehen.
>
> **Der Kern ist nicht der leere Wert, sondern das ausbleibende Ereignis.** Solange die Segmente
> unvollständig sind, feuert Chrome überhaupt kein `input` — React sieht also kein `onChange`, und
> die Oberfläche konnte diesen Zustand deshalb gar nicht bemerken. Sie tat nichts und sagte nichts.
>
> Herausgegeben wird er allein über `validity.badInput`. Gelesen wird der an `keyup` und `blur`
> (`keyup` fängt jeden Tastendruck, `blur` den Weg über Maus und Kalenderfeld) und im
> **Komponentenzustand** gehalten, nicht in der URL: Eine halbe Eingabe ist keine Auswahl, und was
> die URL nicht ausdrücken kann, gehört nicht hinein
> ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8).
>
> **Was der Nutzer sieht, in dieser Reihenfolge:** „Bitte Datum und Uhrzeit vollständig eintragen"
> schlägt alles andere — es ist der einzige Zustand, in dem sonst gar nichts geschähe. Dann die
> Antwort des Servers, falls eine da ist. Dann „Für ein freies Zeitfenster fehlt noch der zweite
> Zeitpunkt", solange erst einer der beiden steht.
>
> **`zeitfenster-unvollstaendig`, `zeitfenster-ungueltig` und `zeitpunkt-ungueltig` stehen ab jetzt
> an den Datumsfeldern**, nicht über der Ansicht (`AM_ZEITFENSTER` in `filter.ts`, dieselbe Bauform
> wie `AM_SUCHFELD`). Wer ein freies Fenster ausfüllt, ist mitten in einer Eingabe; zwischen „Von"
> und „Bis" liegt zwangsläufig ein Moment mit nur einem Zeitpunkt. Diesen Moment mit einer roten
> Meldung über der ganzen Ansicht zu beantworten, hieße dem Nutzer die Liste wegzunehmen, weil er
> noch nicht fertig getippt hat — genau die Belehrung, die §8.2 für den zu kurzen Suchbegriff
> bereits ausschließt. **Die Prüfung bleibt im Backend**; das Frontend hält keine Anfrage zurück und
> rechnet nichts nach, es entscheidet nur, **wo** die Antwort erscheint. `zeitfenster-mehrdeutig`
> gehört ausdrücklich **nicht** dazu: Diesen Zustand lässt die Oberfläche gar nicht erst entstehen —
> käme er doch, ist er ein Befund und gehört sichtbar.

**Statusfilter über die Einordnungen** aus `MessageStatusKind`, nicht über Rohwerte; die
Beschriftungen kommen aus den Sprachdateien. **Prozessfilter** als Mehrfachauswahl aus
`/api/prozesse` ([`prozessauswahl.md`](prozessauswahl.md)); eingegrenzt wird örtlich über die
bereits geladene Liste, nicht über einen Serverparameter.

> **Die aufgeklappte Prozessauswahl ist breiter als das geschlossene Feld** (seit 07.08.2026) —
> 34 rem gegen 13 rem, nach oben durch das Fenster begrenzt (`calc(100vw - 2rem)`). Bei **733
> Einträgen** für `NEXANS` ist die Auswahl ohne vollständige Namen nicht bedienbar: Der längste
> Prozessname hat 58 Zeichen, der längste Projektname 44, und im Vorgängerstand schnitt die Liste
> bei 20 rem ab. Wer zwischen zwei Namen wählen soll, die sich erst hinter dem Schnitt
> unterscheiden, sieht beide Male dasselbe.
>
> **Umbrechen statt kürzen.** In der Tabelle gilt die feste Zeilenhöhe (§8.1), weil dort 50 Zeilen
> überflogen werden; hier wird *ausgewählt*, und ein gekürzter Name macht die Auswahl mehrdeutig.
> Die Kürzung mit Tooltip war als letztes Mittel vorgesehen und **wird nicht gebraucht**:
> Nachgemessen im Browser sind **0 von 733** Einträgen abgeschnitten (`scrollWidth > clientWidth`).
>
> **Kein Virtualisieren.** Die 733 Einträge stehen als 4.399 DOM-Knoten in einem Bereich von
> 32.288 px Höhe. Gemessen über 30 Sprünge à 1.000 px mit erzwungenem Layout: **0,3 ms zusammen,
> schlechtester Einzelwert 0,1 ms** — der Browser rechnet die Liste nicht neu. Ruckelt es nicht,
> bleibt es beim Einfachen; Virtualisierung wäre der erste Umbau, wenn ein Mandant je vierstellig
> viele Prozesse bekommt.
>
> **Der Platzhalter des Suchfelds war abgeschnitten** („Prozess-, Projekt- oder Ablaufnam…") und
> versprach damit weniger, als das Feld kann. Das Feld ist von 16 auf 20 rem gewachsen; die
> Filterleiste bricht um, wenn der Platz nicht reicht.

> **Die Beschriftung des Suchfelds ist am 13.08.2026 angepasst worden — von „Suche" auf „Prozess,
> Projekt oder Ablauf durchsuchen".** Der Anlass steht in [`bam-suche.md`](bam-suche.md) §11.1:
> Seit Schritt 7, Teil 3 steht ein **zweites** Suchfeld in der Kopfzeile, und das sucht etwas
> anderes — Belegnummern. Zwei Felder auf demselben Bildschirm, die verschiedene Dinge tun und
> verschieden fehlschlagen, sind eine Falle, besonders für den Nutzer, der kein EDI-Spezialist ist.
> **Beide Beschriftungen sagen deshalb, worin sie suchen.**
>
> Der **Platzhalter** ist unverändert („Prozess-, Projekt- oder Ablaufname") — er tat es schon; die
> Beschriftung, die für Vorleseprogramme daneben stand, tat es nicht.

**Das Suchfeld sucht ab drei Zeichen und entprellt** (400 ms). Der Freitextfilter ist der teuerste
Fall des Endpunkts (L7c, L11 und L13); bei jedem Tastendruck zu suchen hieße, dieselbe teure Abfrage
fünfmal für einen Begriff zu stellen, den der Nutzer noch nicht fertig getippt hat. **Zu kurz ist
kein Fehler, sondern ein Zwischenzustand** — der Nutzer läuft beim Tippen zwangsläufig hindurch.
`suchbegriff-zu-unscharf` und `suchbegriff-zu-kurz` erscheinen als **Hinweis am Suchfeld**, nicht als
Fehlerzustand der ganzen Ansicht.

**Die Fenstergrenze der Suche erscheint an derselben Stelle** und mit derselben ruhigen Farbrolle.
`suche-fenster-zu-gross` nennt die geltende Grenze und den gewählten Zeitraum — beide Zahlen aus der
Antwort, keine im Frontend — und daneben steht **„Trotzdem suchen"**. Die Schaltfläche setzt
`langeSuche` in der URL; die Anfrage wiederholt sich damit von selbst, weil der Filter der
Abfrageschlüssel ist.

**Die Liste bleibt dabei stehen, so wie sie war.** Das ist der Unterschied zwischen einer Rückmeldung
zu einer Eingabe und einem Fehlerzustand: Wer bei stehender Liste einen Begriff tippt, soll nicht
zusehen, wie sie unter ihm verschwindet — die neue Abfrage hat einen eigenen Schlüssel, für den nie
Daten ankamen, und der Leerzustand behauptete dann, im Zeitfenster stünde nichts. Umgesetzt über
`letzteSeite` in `hooks.ts`: die letzte tatsächlich gelieferte Seite, ausdrücklich abgerufen und
nicht über `placeholderData` — das hielte die alte Seite bei *jedem* Filterwechsel stehen und nähme
dem Nutzer die Rückmeldung, dass gerade geladen wird.

**Ist die Grenze aufgehoben, sagt ein ruhiger Hinweis, dass die Suche länger dauern kann.** Keine
Warnfarbe: Der Nutzer hat das gerade selbst entschieden, er soll nur wissen, was ihn erwartet.

**Der Hinweis ist auf die Breite des Eingabefelds begrenzt.** Ohne das verbreitert ein langer Text
die Spalte des Suchfelds, und das Feld rutscht in eine andere Zeile der Filterleiste — genau in dem
Moment, in dem der Nutzer hineinschreibt. Aufgefallen ist das erst in der Abnahme im Browser; die
kurzen Hinweise davor („noch zwei Zeichen") waren nie breit genug dafür.

**Ein neuer Suchbegriff setzt `langeSuche` zurück.** Die Grenze wurde für *diese* Suche bewusst
aufgehoben; sie stillschweigend über den nächsten Begriff mitzunehmen hieße, eine einmalige
Entscheidung dauerhaft zu machen — ausgerechnet die, die eine mehrsekündige Abfrage erlaubt.

**`suche-abgebrochen` steht ebenfalls am Suchfeld**, obwohl es kein Prüffehler ist. Beide Handlungen,
die helfen — Zeitraum verkleinern, Begriff schärfen —, finden dort statt; und eine Schaltfläche
„Erneut versuchen" wäre hier falsch, weil sie dieselbe Abfrage noch einmal in dieselbe Zeitgrenze
schickte. Aus demselben Grund wiederholt der Zwischenspeicher diesen einen Fall nicht automatisch
(`lib/query-client.ts`); bei jedem anderen `4xx` bleibt es beim einen Wiederholungsversuch.

#### ~~Der Chip „Zwischenschritte ausgeblendet"~~ — entfallen am 11.08.2026

Er stand für die ehrliche Hälfte einer Vorgabe, die sich nicht halten ließ: Wer ein Drittel aller
Zeilen weglässt, muss es sagen — als Chip, der in einem Halbsatz erklärt, was fehlt, und ihn
einschaltet. **Mit der Vorgabe fällt der Chip** (§5); es gibt nichts mehr anzukündigen. Mit ihm
verschwinden die Abfrage auf `/api/nachrichten/merkmale`, ihr Zwischenspeicher, die
Erscheinungslogik und die vier Zeichenketten in `de.ts`/`en.ts`.

> **Was an ihm richtig war und bleiben soll — für den nächsten Fall dieser Art.** Der Chip erschien
> seit dem 07.08.2026 **nur, wo es etwas auszublenden gab**: Messung
> [M12](messungen-schritt4.md) hatte gezeigt, dass fünf von neun Mandanten mit Nachrichten über den
> gesamten Bestand **nicht eine einzige** Zwischenschritt-Zeile haben (`IBIS`, `IBISGUS`, `ZAST`,
> `WOC`, `SYSTEM` — zusammen 112.801 Nachrichten). Für sie kündigte er eine Ausblendung an, die
> nichts ausblendet, und **ein Bedienelement ohne Wirkung ist schlimmer als keins.** Dieselbe Zahl,
> die seine Erscheinungslogik nötig machte, hat am 10.08.2026 die Vorgabe selbst zu Fall gebracht:
> Eine Ausblendung, die bei fünf von neun Mandanten nichts tut und bei einem 39,6 Prozent versteckt,
> beschreibt keinen gemeinsamen Zustand.
>
> **Ebenfalls richtig und für spätere Anzeigen gültig:** Solange die Auskunft lud, erschien er nicht
> (ein Bedienelement, das einen Moment später erscheint, ist besser als eines, das wieder
> verschwindet — [`visuelles-konzept.md`](visuelles-konzept.md) §5); scheiterte sie, erschien er.
> Und sein Text nannte **keine Menge**: Der Anteil lag je Mandant zwischen 44 und 0,03 Prozent
> (`NXHBE` 44,4 %, `NEXANS` 39,6 %, `SUTTONS` 3,0 %, `VOTG` 0,027 %), eine gemittelte Zahl wäre für
> die Hälfte seiner Empfänger falsch gewesen — genau die erfundene Auskunft, die Regel Q4
> ausschließt, und eine mandantengenaue Zahl wäre ein `COUNT` über `Message` und damit Regel L2.

**Sortiert wird über die Spaltenüberschrift „Zeitpunkt".** Ein eigenes Auswahlfeld wäre ein zweites
Bedienelement für eine Entscheidung mit zwei Werten — und es gibt ohnehin keinen zweiten
Sortierschlüssel (§4).

### 8.3 Blättern, Zustände, Aktualisierung

**Vorwärts über `nextCursor`, rückwärts über einen Stapel im Komponentenzustand** — nicht über einen
zweiten Cursor vom Server. **Keine Seitenzahlen:** Es gibt keine Gesamtzahl (Regel L2), und eine
erfundene wäre schlimmer als keine. Ein geänderter Filter setzt den Stapel zurück; der alte Cursor
träge einen Zeitpunkt, der im neuen Fenster nichts zu suchen hat (`cursor-ungueltig`).

**Vier Zustände** nach `components/zustand.tsx`. Die **Filterleiste bleibt in jedem** stehen — sie
ist der Weg aus dem leeren Zustand heraus; sie mit den Daten zu verstecken hieße, dem Nutzer genau
dann das Werkzeug wegzunehmen, wenn er es braucht.

**Der Leerzustand nennt eine Ursache** und sieht nicht wie ein Fehler aus. Genannt werden *alle*
greifenden Einschränkungen — Zeitfenster, Suchbegriff, Status- und Prozessfilter —, nicht nur die
erste: Wer den Suchbegriff leert und immer noch nichts sieht, weil auch der Statusfilter steht, käme
sonst zweimal an dieselbe Wand. Dazu eine Schaltfläche „Auf 30 Tage erweitern".

> **Die Klausel „ausgeblendete Zwischenschritte" ist am 11.08.2026 entfallen** — mit dem Schalter,
> auf den sie hinwies (§5). **Die übrigen bleiben vollzählig**; die Regel „alle greifenden
> Einschränkungen, nicht nur die erste" ist von der Löschung ausdrücklich nicht berührt.

> Der Leerzustand ist hier **besonders wichtig**: Im Profil `dev` enthält das
> 24-Stunden-Standardfenster je nach Mandant sehr wenige oder null Zeilen, und außer `NEXANS` hat
> kein Mandant Daten nach dem 30.12.2025 (Messung M3). „Nichts gefunden" allein ließe den Nutzer
> glauben, das Werkzeug sei kaputt.

**Fehler werden über den `type` der RFC-9457-Antwort übersetzt**, nicht über `detail`
([`frontend-grundlagen.md`](frontend-grundlagen.md) §6). Alle Problemtypen aus §1 haben einen
Eintrag in beiden Sprachdateien.

**Die automatische Aktualisierung ist eng gefasst:**

| | |
|---|---|
| Standard | **aus** — wer das Werkzeug öffnet, ist angespannt; eine Liste, die unter den Händen springt, hilft nicht |
| Intervall | 60 Sekunden |
| nur auf Seite eins | sonst springt die Ansicht oder der Cursor liegt außerhalb des Fensters |
| nur bei sichtbarem Tab | ohne das stellte ein über Nacht offenes Fenster 480 Abfragen auf der Produktionsdatenbank |
| Stand | **immer sichtbar**, auch bei manueller Bedienung |

Dass sie beim Blättern pausiert, steht daneben — ein Schalter, der an ist und nichts tut, ist
schlimmer als einer, der aus ist.

**Beim Mandantenwechsel** wird der Zwischenspeicher geleert, nicht invalidiert (bestehende Regel).
Der **Prozessfilter wird dabei mit zurückgesetzt**: `ProcessID`s sind mandantengebunden, und ein
stehengebliebener Filter erzeugte eine dauerhaft leere Liste, deren Ursache in einem Auswahlfeld
steckt, das nichts mehr anzeigen kann. **`langeSuche` fällt aus demselben Grund mit weg:** Die
Grenze wurde für einen bestimmten Begriff bei einem bestimmten Mandanten aufgehoben; sie über den
Wechsel mitzunehmen, hieße eine einmalige Entscheidung stillschweigend dauerhaft zu machen.
Umgesetzt über `zielNachMandantenwechsel` in `lib/zwischenspeicher.ts` — die Regel steht als
prüfbare Funktion da und nicht als Nebenwirkung einer Navigation.

Ein geteilter Link kann trotzdem fremde `ProcessID`s tragen. Die werden **nicht stillschweigend
entfernt** — das zeigte dem Empfänger einen anderen Ausschnitt als dem Absender —, sondern stehen
als eigener, entfernbarer Eintrag in der Auswahl.

### 8.4 Sichtprüfung im Browser (07.08.2026)

Nach der Nachbesserung, gegen die laufende Anwendung im Profil `dev`, Mandant `NEXANS`, Fenster
1484 × 935. **Eingaben wurden getippt und geklickt, nicht gesetzt** — kein Zuweisen von `value`,
keine von Hand gebaute URL. Der Anlass für diese Regel steht in §8.2: Zwei Prüfungen sind zuvor
durch Türen gelaufen, die es für Nutzer nicht gibt.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | Am Listenende ist Schluss | Das Dokument scrollt **nicht** (`scrollY` bleibt 0, auch nach `scrollTo(0, 1e7)`); der Inhaltsbereich scrollt auf **genau** sein Maximum (1.182 = `scrollHeight − clientHeight`); unter dem letzten Element stehen **16 px**, der Innenabstand des Bereichs. Bei einer Liste mit einer Zeile: `maxScroll = 0` — es gibt keine Scrollfläche, in die man hineinfallen könnte. |
| 2 | „Frei" anklicken, Daten tippen | Nach `20.12.2025 06:15` und `22.12.2025 18:45` steht `von=2025-12-20T05:15:00.000Z&bis=2025-12-22T17:45:00.000Z` in der URL, und die Liste zeigt den getippten Ausschnitt. Der Versatz stimmt (MEZ, UTC+1). |
| 3 | Vier Spalten, gleiche Zeilenhöhe, Sekunden | `Zeitpunkt · Status · Ablauf · Projekt`. **Alle 50 Zeilen sind exakt 36 px** hoch — eine einzige Höhe über die ganze Seite, also `--dichte-zeile`. Zeitpunkt `30.12.2025, 04:09:47`. |
| 4 | Ablauf in Klartext, Prozess im Tooltip | Zelle „Lieferabruf von Kunde A (VDA)", `title` „Lieferabruf von Kunde A (VDA) / Prozess: Kunde A LAB (VDA)". Genau das Gegensatzpaar aus der Aufgabenstellung. |
| 5 | Sekunden unterscheiden gleiche Minuten | Am Rand des Standardfensters stehen 27 Zeilen auf `04:09:47` und 18 auf `04:09:45` — bis zur Minute identisch, an den Sekunden auseinanderzuhalten. |
| 6 | Prozessauswahl lesbar, Bildlauf flüssig | 733 Einträge, **0 davon abgeschnitten**. 30 Sprünge à 1.000 px mit erzwungenem Layout: 0,3 ms zusammen. Mit dem Mausrad kein Ruckeln. |
| 7 | Fußzeile bei ungeklärtem Status | Bei fünf `CHECKED`-Zeilen erscheint sie unter der Tabelle; auf Seiten ohne solche Zeile ist sie nicht da. |
| 8 | Schritt bei offen, nicht bei abgeschlossen | `Wartend` · `Send Message to Pool`; `Abgeschlossen` zeigt nichts daneben. Zeilenhöhe in beiden Fällen 36 px. |
| 9 | Schmales Fenster | **Nicht gesehen — siehe unten.** |

> ⚠️ **Punkt 9 ist offen, und Punkt 1 nur zur Hälfte geprüft.** Die Browsersteuerung konnte das
> Fenster nicht verkleinern: Der Tab rendert ohne echtes Fenster (`outerWidth`/`outerHeight` melden
> `0 × 0`), und die Größenänderung meldet Erfolg, ohne dass sich `innerWidth`/`innerHeight` bewegen.
>
> **Was stattdessen geprüft wurde, und was das wert ist.** Für Punkt 1 wurde die *Inhaltslänge*
> variiert statt der Fensterhöhe — eine Zeile, 50 Zeilen, 50 Zeilen mit Fußzeile. Das trifft
> dieselbe Invariante, an der der gemeldete Fehler hing (der Scrollbereich war höher als sein
> Inhalt), und in allen drei Fällen deckt sich der erreichbare Scrollstand exakt mit dem Maximum.
> **Drei Fensterhöhen sind es trotzdem nicht.**
>
> Für Punkt 9 wurde das **Regelwerk** nachgesehen statt der Darstellung: `hidden md:table-cell`
> steht auf der Projektspalte, und zwar auf **jeder Zelle**, nicht nur auf der Überschrift — unter
> 768 px fällt also die ganze Spalte weg und nicht bloß ihr Kopf. Waagerechter Überlauf ist bei der
> geprüften Breite null, an der Karte wie am Dokument. **Gesehen ist das nicht.**
>
> Beides gehört von Hand nachgeholt, bevor der Schritt als abgenommen gilt.

---

### 8.5 Tests

| Datei | Was |
|---|---|
| `tests/nachrichtenfilter.test.ts` | URL → Zustand → URL; unbekannte Werte werden übergangen; **der Cursor taucht in keiner erzeugten URL auf**; die beiden Zeitfenstermodi schließen einander aus; `langeSuche` steht in der URL und wird nur zusammen mit dem Suchbegriff geschickt; **welche Problemtypen an das Suchfeld gehören, welche an die Zeitfensterfelder und welche über die Ansicht**, samt der beiden Zahlen aus der Antwort; **das halb ausgefüllte freie Fenster** wird erkannt und lässt die Liste stehen; **`AUFGETEILT` und `ZUSAMMENGEFUEHRT` werden gelesen, `ZWISCHENSCHRITT` nicht mehr**; **ein alter `zwischenschritte`-Parameter ist folgenlos** — derselbe Zustand und dieselbe Abfrage wie ohne ihn; ohne Auswahl bleibt die URL leer |
| `tests/format.test.ts` | UTC → Anzeige in der gelieferten Zone; Wanduhrzeit der Eingabefelder, auch am Umstellungstag |
| `tests/zwischenspeicher.test.ts` | das Ziel nach dem Mandantenwechsel trägt keine Filter — auch kein `langeSuche` |

---

### 8.6 Sichtprüfung im Browser (11.08.2026)

Nach Schritt 6, Teil 2a, gegen die laufende Anwendung im Profil `dev`, angemeldet als ADMIN, Fenster
1920 × 726. **Geklickt und getippt, nicht programmatisch gesetzt** — mit einer benannten Ausnahme:
Der alte Link (Punkt 6) *ist* eine von Hand geöffnete URL, denn genau das ist der geprüfte Vorgang.

| # | Geprüft | Ergebnis |
|---|---|---|
| 1 | Kein Chip mehr | Die Filterleiste ist eine Zeile mit **vier** Elementen (Zeitfenster, Status, Prozess, Suche). Kein Chip, keine zweite Zeile darunter |
| 2 | Der Statusfilter bietet beide Werte | Acht Einträge: Fehler · Wartend · Läuft · **Aufgeteilt** · **Zusammengeführt** · Abgeschlossen · Quittiert · Ungeklärt. **„Zwischenschritt" steht nirgends** |
| 3 | Beide liefern Zeilen | `Aufgeteilt` mit dem Split-Zeichen bei `NEXANS` im 24‑h‑Fenster; `Zusammengeführt` mit dem Merge-Zeichen über 7 Tage. Zwei verschiedene Zeichen, dieselbe neutrale Farbrolle |
| 4 | Die Vorgabe blendet nichts aus | Ohne Statusfilter enthält eine Seite von 200 Zeilen bei `NEXANS`: 137 `ABGESCHLOSSEN`, 49 `FEHLER`, **13 `AUFGETEILT`**, 1 `QUITTIERT` — vorher wären die 13 nicht dabei gewesen |
| 5 | Die Statuszelle nennt keine Präposition | `Wartend` · `Schritt: Send Message to …`, `title` trägt den Vollwert `Schritt: Send Message to Pool`. **„wartet vor" kommt im ganzen Dokument nicht mehr vor**, „Zwischenschritt" ebenso wenig |
| 6 | Liste gegen Detail | Dieselbe Nachricht angeklickt: Das Panel markiert `Send Message to Pool` als den Schritt, in dem sie steht („Die Nachricht wartet — von selbst geht es hier nicht weiter", *wartet seit 6 d 17 h · Frist 30 min · Überfällig*). Die Liste daneben sagt `Schritt: Send Message to Pool`. **Kein Widerspruch mehr** |
| 7 | Alter Link | `?zeitraum=7d&zwischenschritte=false` von Hand geöffnet: **200**, Liste wie ohne den Parameter, Statusfilter auf „Alle Status", keine Meldung, keine Umleitung. Der Parameter bleibt unverändert in der Adresszeile stehen — er wird übergangen, nicht weggeschrieben |
| 8 | Kleiner Mandant | Auf `IBISGUS` gewechselt — der Mandant, bei dem ein Stellungsprädikat **100 %** aller Zeilen ausgeblendet hätte (M28‑1). Die Liste zeigt ihren vollen Bestand |
| 9 | Ohne Auswahl bleibt die URL leer | Nach dem Mandantenwechsel steht `/nachrichten` ohne jeden Suchparameter da. Vorher trug sie ab dem ersten Rendern `zwischenschritte=false` |
| 10 | Zeilenhöhe unverändert | Alle 50 Zeilen exakt **36 px**, auch mit geöffnetem Panel und mit dem Schritt-Zusatz in der Zelle — dieselbe eine Höhe wie am 07.08.2026 |
| 11 | Schmales Fenster | **Nicht gesehen — siehe unten.** |

> ⚠️ **Punkt 11 ist offen, und zwar aus demselben Grund wie am 07.08.2026 und am 10.08.2026.** Die
> Browsersteuerung kann das Fenster nicht verkleinern: `innerWidth` und `outerWidth` melden beide
> 1920 und bewegen sich nicht. Das ist keine Beobachtung mehr, sondern eine Eigenschaft der Umgebung
> ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7).
>
> **Was stattdessen geprüft wurde, und was das wert ist.** Das **Regelwerk**: `hidden md:table-cell`
> steht auf **allen 51** Zellen der Projektspalte (Kopf und 50 Zeilen), unter 768 px fällt also die
> ganze Spalte weg und nicht bloß ihr Kopf. Waagerechter Überlauf ist bei der geprüften Breite
> **null**. Die Filterleiste ist `flex flex-wrap` und bricht damit um, statt zu überlaufen — und sie
> hat seit heute ein Element weniger zu brechen. **Gesehen ist das nicht.**
>
> **Das gehört von Hand nachgeholt, bevor der Schritt als abgenommen gilt.**

---

## 9. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | kein Parameter, Mandant aus der Sitzung über `MandantService` |
| **M2** Mandant als erster Pflichtparameter | `NachrichtenRepository`; ArchUnit prüft es |
| **M3** Filter im Statement | `EXISTS` über `Process → ProjectMandant`, Teil jeder Bedingungsliste |
| **M4** Isolationstest je Endpunkt | `NachrichtenIsolationDbIT` — für die Liste. Der zweite Endpunkt (`/api/nachrichten/merkmale`) ist am 11.08.2026 entfallen, sein Isolationstest mit ihm; **kein anderer wurde angefasst** |
| **L1** Pflicht-Zeitfenster | `common/Zeitfenster`, Vorgabe 24 h, Maximum ein Jahr |
| **L2** keine Live-Aggregation | kein `COUNT`, `limit + 1` statt `total` |
| **L3** keine `OFFSET`-Paginierung | Cursor über `(MessageLastUpdate, MessageID)` |
| **L4/L5** `MessageProperty`/BAM nur über die Kennung | `MessageProperty` wird nicht angefasst; **`MessageBAM` seit der Nachbesserung gar nicht mehr** (§6); Suche nur über Stammdaten, mit Mindestlänge, Deckel **und Fenstergrenze** (§5) |
| **L7** jede Abfrage gemessen | [`messungen-schritt4.md`](messungen-schritt4.md), Abschnitte L1 bis L15 |
| **Z1** kein `now()` | Zeitfenster über die Anwendungsuhr, aufgelöst in `common` |
| **Q1** Fehlerbedingung | ausschließlich `MessageStatusClassifier.fehlerBedingung` |
| **Q4** nicht zugeordnet heißt nicht zugeordnet | `processName`/`projectName`/`sosName` bleiben `null`; den Ersatztext wählt die Oberfläche |
| **Status nie allein über Farbe** | `status-plakette.tsx` — Beschriftung **und** Zeichen; die Zuordnung Status → Farbe bleibt in `lib/status-farbe.ts` |
| **Kein Farbwert in einer Komponente** | `tests/farbwerte.test.ts` deckt auch das neue Feature ab |

---

## 9. Offene Punkte

### Zur Fensterverengung (Stand 30.08.2026)

> **Wo die Vermerke zu den Punkten 70 bis 76 stehen und warum hier.** Die Punkte sind in
> [`messungen-liste-verengung.md`](messungen-liste-verengung.md) vergeben; Messdateien sind für
> diesen Schritt ausdrücklich **nicht anzufassen**. Die Vermerke stehen deshalb dort, wo die Sache
> hingehört — dieselbe Aufteilung, die Schritt 10b‑1 für die Punkte 55 bis 57 gewählt hat.

- ~~**57. Die Listenabfrage kostet bei `SUTTONS` über dreißig Tage 1.101,280 ms.**~~
  > ✔ **Erledigt am 30.08.2026 (§5d).** Verengt kostet derselbe Fall **12,183 ms** samt Vorabfrage —
  > Faktor 91. Der Weg dahin war keiner der sechs Fassungen aus §5a, sondern ein engeres Fenster:
  > Der Optimierer wechselt die Planfamilie dann von selbst, `pm`/`ref` → `m`/`range` über
  > `MessageLastUpdateIDX` mit 470 statt 437.150 geschätzten Zeilen.

- ~~**70. Die Verengung kann einem bereits zeitgetriebenen Mandanten nicht helfen.**~~
  > ✔ **Entschieden am 27.08.2026, bestätigt am 30.08.2026.** Der Auftraggeber hat `ZAST` mit
  > 275 ms ausdrücklich als tragbar entschieden und die Behandlung aus dem Bau herausgenommen. Die
  > Abschlussmessung bestätigt den Befund unverändert: 284,824 → 286,545 ms. **Der Punkt ist damit
  > nicht gelöst, sondern beantwortet** — er bleibt als Sachverhalt gültig und ist kein Auftrag mehr.

- ~~**72. Eine Verengung, die nicht jeden Filter mitträgt, verliert stillschweigend Zeilen.**~~
  > ✔ **Erledigt am 30.08.2026.** Der Bau ist umgekehrt herum gebaut: Er greift nur, wenn für jedes
  > gesetzte Merkmal ausdrücklich entschieden ist, dass der Rollup es trägt (§5d). Zwei Riegel
  > halten das — ein `Abfragemerkmal`-Enum mit zwei vollständigen `switch`-Ausdrücken ohne
  > `default`, und ein Test über `getRecordComponents()`, der ein neues **Feld** fängt, das der
  > Compiler nicht sieht. Der gemessene Fall selbst ist Test geworden:
  > `FensterverengungDbIT.statusfilter_verliert_keine_zeilen_bei_suttons`, samt Vorbedingung, dass
  > `SUTTONS` überhaupt Fehlerzeilen hat — sonst bewiese „null gleich null" nichts.

- ~~**73. Ob ein Index `(process_id, stunde)` die dünnen Mandanten rettet.**~~
  > ✔ **Erledigt am 30.08.2026.** Gebaut in `V11__message_rollup_prozess_index.sql`, nachdem M105
  > gemessen hat, was er den beiden Läufen kostet: der stündliche Delta-Lauf **nichts**, der
  > nächtliche Volllauf **+52,8 %** im Schreibpfad, die größte Scheibe 3,669 s gegen die vorher
  > gesetzte Schranke von 5 s ([`rollup.md`](rollup.md) §9c). Die Vorabfrage steigt seither bei
  > allen zehn Mandanten über `message_rollup_prozess_idx` ein.

- **76. Die 24‑Stunden‑Stufe der Vorabfrage kostet 3,4 bis 4,4 ms, und der Index rührt sie nicht
  an.** 🟡 **Bleibt offen.** Sie ist nach dem Bau der größte Einzelposten der Vorabfrage und die
  Ursache dafür, dass `EDITIONLINGERI` die 5‑ms‑Erwartung aus C.5 mit 6,335 ms verfehlt. Auffällig
  und weiterhin ungeklärt: Die 24‑Stunden‑Scheibe umfasst 587 Rollupzeilen, die 1‑Stunden‑Scheibe
  28 — der einundzwanzigfache Umfang bei vierfacher Laufzeit, während die 30‑Tage‑Stufe mit Index
  auf unter 1 ms fällt. **Der Plan dieser Stufe ist nach wie vor nicht erhoben.** Er ist die
  billigste offene Messung des Projekts.

- **77. Die Sortierrichtung `AELTESTE` fällt auf den heutigen Pfad zurück.** Die gespiegelte
  Rechnung — vom Fensteranfang **vorwärts** summieren und eine **Ober**grenze setzen — wäre nach
  demselben Argument richtig: Auch dort zählten nur vollständig im Fenster liegende Stunden, und die
  gezählte Menge bliebe eine Unterschranke. **Sie ist nicht gemessen und deshalb nicht gebaut.** Wie
  oft `sortierung=AELTESTE` überhaupt benutzt wird, ist ebenfalls nicht erhoben; ohne diese Zahl
  wäre der Bau eine Vermutung über den Nutzen.

- **78. 🔴 Die Liste ist über das Vorgabefenster teurer als über dreißig Tage — bei vier
  Mandanten um Faktor 60 bis 97.** Gemessen in M106: `NXHBE` kostet über **24 Stunden** 98,962 ms
  und über dreißig Tage **1,019 ms**; `SYSTEM` 98,358 gegen 1,582 ms; `ZAST` 62,986 gegen 284,824 ms
  in der anderen Richtung. Der Grund steht im Plan: Über 24 Stunden wählt der Optimierer den
  Zeitindex und liest dessen 13.534 Zeilen vergeblich, über dreißig Tage die prozessgetriebene Form,
  die bei neun Zeilen Gesamtbestand fast nichts kostet.
  **Das ist ein Befund über die heutige Liste und nicht über die Verengung** — §5a hat nur dreißig
  Tage gemessen und diesen Fall deshalb nie gesehen. **24 Stunden ist der Wert, den die Liste ohne
  Parameter nimmt** (Regel L1), also der häufigste Fall überhaupt. Die Verengung entschärft ihn für
  drei der vier Mandanten über den Nullfall (98,962 → 5,212 ms); `WOC` bleibt bei 85,913 ms, weil er
  eine einzige Nachricht im Fenster hat und damit weder unter den Nullfall noch über die Schwelle
  fällt. **Für `WOC` ist damit nichts gewonnen und nichts verloren — der Fall bleibt offen.**

### Zum Plan der Liste (Stand 27.08.2026, Schritt 10b‑1 Teil A)

> **Wo die Vermerke zu den Punkten 55 bis 57 stehen und warum hier.** Die Punkte sind in
> [`messungen-schritt10b.md`](messungen-schritt10b.md) vergeben; diese Datei ist für Schritt 10b‑1
> ausdrücklich **nicht anzufassen**. Die Vermerke stehen deshalb dort, wo die Sache hingehört: 56
> und 57 hier, 55 in [`rollup.md`](rollup.md) §13.

- **Offener Punkt 57 ist beantwortet und bleibt offen** *(27.08.2026)*. Beantwortet: Ja, es betrifft
  weitere Mandanten, und die vermutete Ursache trägt nicht — §5a hat alle zehn gemessen. Offen:
  **Es ist nichts repariert.** Sechs Fassungen sind gemessen, keine trägt ohne Schaden, und der
  Auftrag sieht für diesen Fall „nicht bauen, melden" vor. Was es bräuchte, steht in §5a unter
  „Was es bräuchte"; die Entscheidung gehört dem Auftraggeber.

- **63. Zwei Mandanten zahlen für ihre Dünne, und keiner von beiden steht in Punkt 57.**
  `EDITIONLINGERI` kostet über 30 Tage **2.313,808 ms**, `ZAST` **288,561 ms** — beide
  zeit-getrieben, beide ohne jeden Filter, beide im zulässigen Wert `zeitraum=30d`. Der Grund ist
  derselbe: Die Abfrage liest das ganze Fenster, weil sie nie 51 Zeilen findet.
  `EDITIONLINGERI` ist damit **der teuerste Fall des Endpunkts nach dem Prozessfilter** und mehr als
  doppelt so teuer wie der `SUTTONS`-Fall, um den Punkt 57 geht. **Ein Zeitfenster hilft dagegen
  nicht** — es ist bereits gesetzt.

- **64. Der Prozessfilter über 30 Tage liegt bei `NEXANS` auf 7.459,912 ms und damit auf 74,6 % der
  Zeitgrenze des Lese-Pools.** Gemessen warm, mit drei Prozessen, die viel Verkehr tragen; der
  Kaltfaktor aus M44 geht bis 9,66, und der Pool bricht nach 10 s ab
  ([`datenzugriff.md`](datenzugriff.md) §1). **Der Fall ist heute über die Prozessauswahl mit zwei
  Klicks erreichbar** und liefert dann `500` statt einer Liste. Er ist in §5a gemessen und in diesem
  Schritt nicht angefasst worden — ein Eingriff hier hätte dieselbe Prüfung über alle zehn Mandanten
  gebraucht wie der Listen-Fix selbst.

- **65. Die Planwahl ruht auf einer Schätzung, die für jeden Mandanten dieselbe ist.**
  `ProejctIDIDX`, `Message_ProcessFK` und `MessageStatusIDX` tragen alle die `CARDINALITY` **18**
  (M83, unabhängig bestätigt in §5a). Daraus folgt für jeden Prozessnachschlag dieselbe Schätzung
  von **197.804** Zeilen — bei einem Mandanten mit neun Zeilen wie bei einem mit zweihunderttausend.
  Solange das so bleibt, ist jede Planwahl dieses Endpunkts ein Zufallstreffer, und jeder Eingriff
  verschiebt nur, **welche** Mandanten davon profitieren. `ANALYZE TABLE` ist keine Abhilfe: Es
  wäre ein Schreibzugriff auf `GlassfishDB`.


### Zum Parameter `ueberfaellig` (Stand 27.08.2026, Schritt 10b-1 Teil B)

- **Offener Punkt 56 ist erledigt** *(27.08.2026)*. Er verlangte, dass benannt wird, was
  `ueberfaellig` wirklich ist, **bevor** er gebaut wird — das steht jetzt in §5b, mit den Plänen
  daneben, und es steht zusätzlich am Code: an `MessageStatusClassifier.ueberfaelligBedingung`, am
  Parameter des Controllers und in `NachrichtenPlanDbIT`, der die zweite Form maschinell festhält.
  **Auch der zweite Satz des Punktes ist umgesetzt:** Dass die Cursor-Messung aus M4/L8 für diese
  Form *nicht* gilt, steht in §5b und wird von `NachrichtenPlanDbIT` geprüft; dass sie trotzdem
  richtig blättert, weist `NachrichtenUeberfaelligDbIT` über elf Seiten nach.

- **66. Die zweite Abfrageform ist nur so lange billig, wie es wenige überfällige Zeilen gibt.**
  Der Statusbereich umfasst heute **539** Zeilen im Gesamtbestand; die Mandantenkette wirkt erst
  danach, und der `filesort` läuft über diesen Bereich. In einem Bestand, in dem `SUSPENDED` und
  `RUNNING` häufiger sind — in Produktion durchaus möglich, `RUNNING` kommt auf der Testkopie null
  Mal vor —, wächst er mit. **Gemessen ist er nur an 539 Zeilen.** Die Zahl gehört in eine spätere
  Messrunde gegen den Produktionsbestand; ein Zeitfenster hilft dagegen nicht, weil es erst nach
  dem Statusbereich greift.

### Zum Freitextfilter und zum Zeitfenster

- **Der Freitextfilter bleibt teuer, und beide vorgesehenen Abhilfen sind widerlegt** (geprüft am
  06.08.2026, Messungen M10 und L11). Der Punkt bleibt offen — aber er ist jetzt ein *bekannter*
  offener Punkt und keine ungehobene Verbesserung. **Eingegrenzt ist er seit dem 06.08.2026 durch
  die Fenstergrenze aus §5** (Messung L13); was folgt, beschreibt weiterhin die Ursache und nicht
  ihre Behebung:

  1. **Auflösung über `SOS.ProcessID` zu einer Kennungsliste — verworfen.** Sie hätte die
     ODER-Bedingung beseitigt und einen Indexzugriff auf `ProcessID` möglich gemacht. Die
     Vorprüfung (M10) ergibt aber **9.101 Zeilen**, bei denen `SOS.ProcessID` und
     `Message.ProcessID` auseinanderfallen — elf `SOS`-Zeilen, verteilt über fünfzehn Monate,
     `NEXANS` 9.038 und `IBIS` 63. Diese Nachrichten fände die Suche danach nicht mehr. **Eine
     Suche, die stillschweigend weniger findet, ist schlimmer als eine langsame.**
  2. **Zwei Zweige mit `UNION` — widerlegt.** Diese Datei nannte den `UNION` bisher als die
     naheliegende Abhilfe. Gemessen ist er **langsamer**: 985 ms gegen 499 ms beim selektiven
     Begriff, 7,2 s gegen 2,3 ms beim volumenstarken. Der Grund steht in M1 und war übersehen
     worden: **auf `Message.SOSID` gibt es keinen Index.** Der `SOSID`-Zweig kann deshalb keinen
     eigenen Zugriffspfad wählen und steigt wieder über `MessageLastUpdateIDX` ein — der `UNION`
     liest das Fenster **zweimal** statt einmal. Die 12,3 ms aus der Gegenmessung in L7c waren nur
     der `ProcessID`-Zweig **allein**, also eine Abfrage, die die Treffer aus `SOSName` schlicht
     weglässt.

  **Was bleibt.** Ein Index auf `Message.SOSID` würde es lösen und ist ausgeschlossen — `GlassfishDB`
  gehört uns nicht, Regel S1 verbietet jedes DDL. Damit sind die Mindestlänge (Regel L5), die
  Entprellung im Suchfeld und ein enges Zeitfenster die verfügbaren Abhilfen. Die zuletzt genannte
  nächste Stufe — eine **Obergrenze für das Zeitfenster bei gesetztem Suchbegriff** — ist am
  06.08.2026 gemessen (L13) und **umgesetzt** (§5): 30 Tage, bewusst aufhebbar bis 90.

  **Die Gestalt einer späteren Behebung ist durch M10 sichtbar geworden — und sie ist viel kleiner
  als der in Annahme A9 genannte „eigene Suchindex".** Was den Umbau über `SOS.ProcessID` zu Fall
  gebracht hat, ist ein sehr kleines Datenproblem: **elf `SOS`-Zeilen mit vierzehn abweichenden
  Prozessen.** Eine Zuordnungstabelle dieser Größe in `overlord_monitor`, vom Rollup-Job aus
  Schritt 10 fortgeschrieben, würde genügen: Ein Treffer in `SOSName` ließe sich damit auf **beide**
  Prozesskennungen auflösen — die des Ablaufs und die, unter der die Nachrichten tatsächlich
  stehen —, und die einfache Bedingung `ProcessID IN (…)` trüge wieder, ohne Zeilen zu verlieren.
  Damit fiele die ODER-Verknüpfung weg, die heute den Index auf `ProcessID` ausschließt. **Der Preis
  ist ein voller Durchlauf über `Message`** (10,9 s, M10) zum Füllen der Tabelle; als seltener
  Hintergrundlauf ist das tragbar, als Teil einer Anfrage nicht. Umgesetzt wird das hier nicht — es
  gehört zu Schritt 10 und braucht seine eigene Messung.
- **Nachrichten wechseln den Prozess, während die `SOSID` stehen bleibt.** ~~**Für Schritt 6 ist
  offen, was das für die Mandantengrenze bedeutet.**~~ **Geprüft am 10.08.2026 —
  [M27](messungen-schritt6.md#m27--mandantengrenze-bei-prozesswechseln): Quell- und Zielprozess
  führen in allen 9.101 Zeilen zum *selben* Mandanten** (`IBIS`/`IBIS` 63, `NEXANS`/`NEXANS` 9.038).
  Die drei unten genannten Möglichkeiten sind damit **nicht** gegeneinander abzuwägen; die Kette
  bleibt innerhalb der Grenze. Der Mandantenfilter der Kette wird trotzdem gesetzt — er ist die
  Zusicherung (Regel M5), nicht die Beobachtung.

  Nachgewiesen ist der Wechsel selbst (M10):
  **9.101 Zeilen**, bei denen `SOS.ProcessID` und `Message.ProcessID` auseinanderfallen — elf
  `SOS`-Zeilen, vierzehn Prozesse, verteilt über fünfzehn Monate und damit kein Ausreißer eines
  Tages.

  **Für die Liste ist das folgenlos**, weil der Mandantenfilter über den *aktuellen*
  `Message.ProcessID` läuft: Eine Zeile gehört immer genau dem Mandanten, unter dem sie gerade steht.
  **Für die Verkettung in Schritt 6 ist es das nicht.** Eine Kette über einen solchen Wechsel hätte
  Glieder in zwei Mandanten, und was der Endpunkt dann zeigt und was er verschweigt, ist eine
  Entscheidung, die **vor** dem Bau der Kette fallen muss — nicht während. Die naheliegenden
  Möglichkeiten (die Kette am Mandantenwechsel abschneiden; sie zeigen und die fremden Glieder
  unkenntlich machen; sie ganz verweigern) unterscheiden sich fachlich erheblich, und die
  Mandantentrennung ist bei externen Nutzern eine Sicherheitsanforderung (Regel M5: sie gilt auch
  quer).

  Der Punkt steht hier und nicht erst in Schritt 6, damit er dort nicht **neu gefunden** werden muss.

  > **Als Vermutung zur Ursache, nicht als Befund:** Das Datenbank-Event `MoveDTNA997` verschiebt
  > laut [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §3.3 stündlich Nachrichten zwischen zwei
  > Prozessen. Das passt zum Bild, ist aber **nicht nachgewiesen** — geprüft wurde die Abweichung,
  > nicht ihre Herkunft.

- **Eine tatsächlich hängende `SPLITTED`-Nachricht erscheint nie als überfällig.** `AUFGETEILT` und
  `ZUSAMMENGEFUEHRT` gelten als Endstatus (`message-status.md`), und die Überfälligkeitsrechnung
  setzt „nicht in einem Endstatus" voraus. Bleibt eine gesplittete Nachricht wirklich hängen, sieht
  man das **nicht** am Status, sondern erst über die Verkettung — dort fehlt dann die Fortsetzung.
  Das ist eine bewusste Entscheidung (sonst wären 34,38 Prozent aller Zeilen Kandidaten für
  „überfällig"), aber es ist eine Lücke, und sie gehört hier benannt. **Die Aufteilung des Status am
  11.08.2026 hat daran nichts geändert** — sie hat den Wert geteilt, nicht die Rechnung.
- 🟡 **Der Status sagt über die Stellung in der Kette nichts Verlässliches — und die Liste zeigt
  trotzdem nur ihn** *(neu am 11.08.2026)*. Bei `IBIS`, `IBISGUS` und `ZAST` trägt die Wurzel
  `FINISHED` (M24‑3). `AUFGETEILT` sagt bei `NEXANS` also etwas über die Aufteilung und bei drei
  Mandanten nichts. **Die Liste kann es nicht besser wissen:** Die verlässliche Auskunft steht in
  `Source`/`Target` und nicht im Status, und eine **Rollenkennzeichnung in der Liste ist
  ausgeschlossen** — Entscheidung des Auftraggebers, weder Spalte noch Symbol noch Tooltip; die
  Rolle erscheint im Detail. Das ist die bewusst getragene Folge und **kein Fehler**; sie steht
  hier, damit sie nicht als einer gemeldet wird.

  > **Belegvermerk** (Regel L10).
  > *Gemessen (M24‑3, Fenster B + Überhang):* Bei `IBIS` (1.714 Elternzeilen), `IBISGUS` (22) und
  > `ZAST` (20) tragen die Eltern `FINISHED`; bei `NEXANS` (27.644) und `SUTTONS` (640) tragen sie
  > `SPLITTED`.
  > *Behauptet ist:* Der Status ist über die Kette hinweg unzuverlässig.
  > **Die Lücke:** Gemessen sind fünf Mandanten in einem Monatsfenster, behauptet ist eine Aussage
  > über den Bestand. Sie trägt trotzdem, weil sie einen **Gegenbeleg** braucht und keine Quote: Ein
  > einziger Mandant, dessen Wurzel `FINISHED` trägt, widerlegt „der Status benennt die Stellung".
  > Drei sind gemessen.
- ~~**Groß-/Kleinschreibung bei der Fehlerbedingung.**~~ **Erledigt am 06.08.2026.** Die Sortierung
  des Quellschemas ist `utf8mb4_general_ci`, der SQL-Vergleich also unabhängig von der Schreibweise;
  `String.startsWith` und `Map.get` in Java sind es nicht. Ein Wert `error_x` wurde damit in SQL als
  Fehler gefunden und in Java als ungeklärt beschriftet — der Statusfilter `FEHLER` lieferte eine
  Zeile, die die Liste anschließend mit „Bedeutung nicht verifiziert" beschriftete.

  Angeglichen ist es **in Java**: `MessageStatusClassifier.einordnung` stellt den Rohwert vor jedem
  Vergleich mit `Locale.ROOT` hoch, sowohl für das `ERROR_`-Präfix als auch für den Abgleich gegen
  die bekannten Werte (und damit auch für `COMMIT_REJECTED`). `Locale.ROOT` und nicht die
  Standardsprache: Im türkischen Gebietsschema wird aus `i` ein `İ`, und `FINISHED` träfe seinen
  eigenen Eintrag nicht mehr.

  **SQL bleibt unangetastet.** Ein `UPPER()` in der Bedingung kostete den Indexbereich auf
  `MessageStatusIDX`, den die Messungen L5 und L6 als Treiber des Statusfilters ausweisen — für
  einen Fall, der auf der Testkopie kein einziges Mal vorkommt. `MessageStatusClassifierTest`
  deckt einen kleingeschriebenen Wert ab; `DatenzugriffDbIT` wird weiterhin rot, sobald im
  Altsystem ein dreizehnter Statuswert auftaucht.
- **`suche-abgebrochen` sagt dem Nutzer „mach die Anfrage kleiner", auch wenn in Wahrheit die
  Datenbank unter Last steht.** Der Statuscode ist `400`, weil das die Handlung ist, die dem Nutzer
  zur Verfügung steht — aber die Antwort unterscheidet die beiden Ursachen nicht, und sie kann es
  aus der Anwendung heraus auch nicht. Sichtbar wird der Unterschied nur in der Häufung: Jede dieser
  Antworten trägt eine `traceId`, die im Protokoll steht. Häufen sich die Einträge, ist das das
  Signal — eine Kennzahl dafür gibt es heute nicht, sie gehört zur Betriebsüberwachung und nicht in
  diesen Schritt.
- ~~**Die BAM-Spaltenauflösung läuft je Anfrage.**~~ **Gegenstandslos seit dem 07.08.2026** — die
  Liste löst keine BAM-Spalten mehr auf (§6). Die Frage nach einem Zwischenspeicher stellt sich in
  Schritt 7 neu und dann mit anderen Zahlen.
- ~~**Kein `SOSName` in der Liste.**~~ **Umgekehrt entschieden am 07.08.2026.** Diese Datei nannte
  ihn „einen weiteren Join ohne Nutzen für die Frage *wo steht mein Beleg*". Die erste Hälfte ist
  gemessen und stimmt (L14: `eq_ref`, 0,2 ms); die zweite ist durch das Durchklicken des
  Auftraggebers widerlegt — `ProcessName` ist nur zufällig lesbar, und die Spalte daneben war leer.
  Der Anzeigename ist jetzt die Spalte „Ablauf" (§8.1).
- ~~**Die Statuszelle sagt „steht auf", gemeint ist „wartet vor".**~~ **Erledigt am 07.08.2026 in
  Schritt 5, Teil 2** — die Zelle sagt jetzt „wartet vor: …" beziehungsweise „läuft auf: …" (§8.1).
  **Offen bleibt der zweite Teil des Punktes:** Belegt ist die Aussage nur für `SUSPENDED`; für
  `LAEUFT` ist sie es nicht und wird es lokal auch nicht. Der Befund selbst bleibt hier stehen,
  statt gelöscht zu werden — er ist die Begründung der heutigen Beschriftung:

  §8.1 zeigt bei `WARTEND` und `LAEUFT` den Schritt aus `SOSActionName` neben der
  Statusplakette. Gemessen am 07.08.2026 ([`messungen-schritt5.md`](messungen-schritt5.md) M16 3):
  Bei **allen 538** `SUSPENDED`-Nachrichten ist **jede** Aktion beendet — `MessageActionEnd` ist
  nirgends `NULL`, und keine Nachricht steht ohne Aktion da. Eine wartende Nachricht steht also
  **zwischen** zwei Schritten und nicht auf einem laufenden; beim Warten auf eine Zusammenführung ist
  genau das der Normalfall.

  **Die Anzeige ist fachlich richtig** — der genannte Schritt ist der, auf den `Message.SOSActionID`
  zeigt, und das ist der nächste, nicht der laufende. Falsch ist nur, dass die Zelle das nicht sagt.
  Wer „Send Message to Pool" neben `WARTEND` liest, nimmt an, dieser Schritt laufe gerade.

  **Nachgezogen wurde die Beschriftung in Schritt 5, Teil 2** — dort, wo die Zeitleiste den
  Unterschied zwischen „steht auf" und „wartet vor" ohnehin sichtbar machen muss. Sie zwischendurch
  isoliert zu ändern hätte geheißen, dieselbe Entscheidung zweimal zu treffen.

  > ⚠️ Belegt ist das für `SUSPENDED`, **nicht** für `LAEUFT`: `RUNNING` kommt in der Testkopie null
  > Mal vor (Projektbeschreibung §4.1). Gerade dort wäre der laufende Schritt der zu erwartende Fall
  > — die Beschriftung trägt deshalb **beide** Lagen und ist nicht einfach von „steht auf" auf
  > „wartet vor" umgestellt worden. **Dieser Teil bleibt offen und ist lokal nicht zu schließen.**

- ~~🔴 **„wartet vor" ist widerlegt und steht trotzdem noch in der Zelle**~~ **Erledigt am
  11.08.2026 (Schritt 6, Teil 2a).** Die Zelle nennt keine Präposition mehr, sondern Status und
  Schritt: `Wartend` · `Schritt: Send Message to Pool` (§8.1). Liste und Detail widersprechen sich
  damit nicht mehr.

  **Von den drei Wegen, die hier standen, ist der zweite genommen worden — in seiner strengsten
  Fassung.** Die Bewertung von damals gehört dazu, weil sie erklärt, warum:

  | Weg | Preis | Ergebnis |
  |---|---|---|
  | Listen-Endpunkt trägt den Wartezustand je Zeile | ein zusätzlicher Zugriff auf `MessageAction` (10,3 Mio. Zeilen) je Zeile | **verworfen** — genau der Join, den die Liste nach L2/L3 nicht macht; er stünde auf jeder Seite mit 50 Zeilen für ein Wörtchen |
  | Wortwahl ohne die Unterscheidung | kostet nichts, sagt weniger | **genommen**, aber nicht als „wartet: X" (das liest sich für `LAEUFT` schief), sondern als „Schritt: X" — eine Formulierung, die für beide Einordnungen trägt |
  | Beschriftung fällt weg, der Schritt steht wieder allein | zurück zum Zustand vor Schritt 5 | **verworfen** — die Sichtprüfung hatte ihn als irreführend befunden |

- 🟡 **Das schmale Fenster ist weiterhin ungesehen** *(fortgeschrieben am 11.08.2026)*. Zum dritten
  Mal geprüft ist nur das Regelwerk, nicht die Darstellung — die Browsersteuerung kann das Fenster
  nicht verkleinern ([`frontend-grundlagen.md`](frontend-grundlagen.md) §7). Neu ist, dass die
  Filterleiste ein Element weniger hat: Der Chip stand als eigene Zeile darunter und ist entfallen,
  die verbleibenden vier brechen um. **Was zu sehen wäre:** ob die vier Bedienelemente unter 768 px
  sinnvoll umbrechen und ob die Statuszelle mit `Schritt: …` dort noch lesbar bleibt — sie ist
  gegenüber „wartet vor: …" kürzer geworden, aber die Statusspalte ist ab `lg` breiter, und
  darunter greift diese Breite nicht.
- **Der Zeitzonen-Übergang** (Wanduhrzeit der Quelle → UTC der API) setzt voraus, dass Anwendungs-
  und Datenbankserver dieselbe Zone haben. Für die Testkopie ist das gemessen; für die Produktion
  ist es die Annahme, die die Anwendungsuhr ohnehin macht. Ein Auseinanderlaufen fiele als
  systematischer Versatz aller Zeitpunkte auf — und seit Aufgabe 11 an **einer** Stelle: Die
  Oberfläche formatiert mit derselben Zone, die das Backend zum Umrechnen benutzt.

### Zur Überfälligkeitsform (01.09.2026)

- **89. Es gibt keinen Weg, „nur überfällige" in der Liste selbst einzuschalten.** Der Parameter ist
  seit dem 01.09.2026 in der Oberfläche vorhanden (§5e), sichtbar und entfernbar — aber **eingeschaltet
  wird er ausschließlich über einen Verweis von außen**, heute die Kachel des Dashboards. Ein
  Einschalter in der Filterleiste ist eine Gestaltungsentscheidung über die Liste: welchen Platz er
  bekommt, wie er neben dem Statusfilter steht, was er mit einer bestehenden Statuswahl tut. **Die
  hat in diesem Schritt niemand getroffen**, und ein Frontend-Schritt für das Dashboard ist nicht der
  Ort, sie nebenbei zu treffen. Wer sie trifft, trifft dabei auch die Frage, ob die
  Ausschlussregel dann noch stimmt — heute beendet **jede** Statuswahl die Form, auch eine, die das
  Backend zuließe.

### Zur Oberfläche (Aufgaben 13 bis 15)

- ~~**Der Zeilenklick hat keine Funktion.**~~ **Erledigt in Schritt 5, Teil 2** — er öffnet die
  Detailansicht (§8.1, [`nachrichtendetail.md`](nachrichtendetail.md) §10).
- **Der relative Tooltip rechnet gegen die Browseruhr**, nicht gegen die Anwendungsuhr. In
  Produktion ist das dasselbe; im Profil `dev` liest er sich als „vor 7 Monaten", weil die Testkopie
  so weit zurückliegt. Er sagt damit die Wahrheit über die realen Daten und nicht über die
  verstellte Uhr — der absolute Wert daneben bleibt unberührt. Wollte man ihn an die Anwendungsuhr
  binden, müsste das Backend einen Bezugszeitpunkt mitliefern; das wäre ein neues Feld für einen
  Tooltip und ist den Preis heute nicht wert.
- **Die Prozessauswahl lädt alle Prozesse des Mandanten und grenzt im Speicher ein.** Bei `NEXANS`
  sind das 733 Einträge — vertretbar, aber die obere Kante. Ein serverseitiger Suchparameter wäre
  der nächste Schritt, wenn ein Mandant je vierstellig viele Prozesse bekommt; er brächte
  allerdings genau die Fallstricke mit, die den Freitextfilter der Liste teuer machen.
- **Kein Virtualisieren der Tabelle.** Bei einer Seitengröße von 50 (Maximum 200) ist es
  unnötig; käme je eine „alles laden"-Ansicht dazu, wäre es der erste Umbau.
- **Der Leerzustand unterscheidet nicht zwischen einem zu eng gewählten Fenster und einer
  Datenlücke** (offene Frage 3 aus [`messungen-schritt4.md`](messungen-schritt4.md)). Er nennt alle
  greifenden Einschränkungen und bietet an, das Fenster zu erweitern — dass die Testkopie zwischen
  Januar und Mai 2026 keine einzige Zeile hat, weiß er nicht. Dafür bräuchte es eine Auskunft über
  den Datenbestand, die es nicht gibt.
