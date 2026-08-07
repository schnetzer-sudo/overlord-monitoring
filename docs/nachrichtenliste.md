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
| `suche` | Freitext, mindestens 3 Zeichen | — | Prozess-, Projekt- und Ablaufname |
| `langeSuche` | `true`, `false` | `false` | hebt die Fenstergrenze der Suche auf — bis 90 Tage, nicht weiter |
| `zwischenschritte` | `true`, `false` | `false` | `SPLITTED`/`MERGED` |
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
      "sosName": "Versand Einzel IDOC aus Split"
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
| `suchbegriff-zu-kurz` | 400 | unter drei Zeichen |
| `suchbegriff-zu-unscharf` | 400 | mehr Treffer als die Grenze |
| `suche-fenster-zu-gross` | 400 | `suche` gesetzt und Spanne über der Grenze (§5) |
| `suche-abgebrochen` | 400 | Statement in `max_statement_time` gelaufen, bei gesetztem `suche` |
| `limit-ungueltig` | 400 | außerhalb 1 bis 200 |
| `cursor-ungueltig` | 400 | unlesbar **oder** Zeitpunkt außerhalb des Fensters |
| `kein-mandant-gewaehlt` | 403 | kein aktiver Mandant in der Sitzung |

**Kein unbekannter Wert wird stillschweigend auf die Vorgabe gezogen.** Wer `zeitraum=24` schreibt,
bekäme sonst 24 Stunden und hätte keinen Anlass, den Tippfehler zu bemerken.

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

### Der zweite Endpunkt: was der Bestand hergibt

```
GET /api/nachrichten/merkmale        →  { "zwischenschritteVorhanden": true }
```

Ergänzt am 07.08.2026. **Kein Parameter, auch kein Zeitfenster** — die Antwort hängt ausschließlich
an der Sitzung (Regel M1), und das ist zugleich der Kern seines Isolationstests: Zwei Nutzer stellen
dieselbe Anfrage und bekommen verschiedene Antworten, weil es keine Eingabe gibt, über die einer die
Auskunft des anderen erreichen könnte.

Er beantwortet eine Frage über die **Stammdaten des Mandanten**, nicht über einen Ausschnitt: *Kommen
bei diesem Mandanten überhaupt Zwischenschritte vor?* Die Oberfläche entscheidet daran, ob sie den
Ausblenden-Schalter anbietet (§8.2).

**Warum nicht ein Feld an jeder Seite.** Der Wert beschreibt den Gesamtbestand, ändert sich selten
und wird zwischengespeichert; die Liste aktualisiert sich unter Umständen jede Minute. An jeder Seite
zu hängen hieße, ihn jedes Mal neu zu ermitteln — und das ist genau der `COUNT` je Anfrage, den Regel
L2 ausschließt.

**Zwischengespeichert je Mandant, eine Stunde, im Arbeitsspeicher** (`NachrichtenService`). Keine
Tabelle: Der Wert wird *ermittelt*, nicht gepflegt — eine Konfigurationstabelle wäre eine zweite
Wahrheit, die der ersten irgendwann hinterherliefe. Die Haltbarkeit rechnet gegen die **Systemuhr**;
gegen die Anwendungsuhr gerechnet liefe sie im Profil `dev` nie ab, weil die dort Monate zurücksteht.

**Scheitert die Ermittlung, antwortet der Endpunkt `true`** und nicht mit einem Fehler. Ein Abbruch
beim Ermitteln eines *Anzeigehinweises* darf die Liste nicht mitreißen; `true` ist der Zustand, den
die Liste vor der Nachbesserung für alle hatte — also keine Verschlechterung. Der Rückfall wird
mitzwischengespeichert, sonst wäre der Schutzmechanismus die Last. Sichtbar bleibt er im Protokoll
(`WARN`, mit Ausnahme).

Kosten und die Form des Statements stehen in
[L15](messungen-schritt4.md#l15--gibt-es-beim-mandanten-überhaupt-zwischenschritte); die Kurzfassung:
`STRAIGHT_JOIN` über `ProjectMandant → Process → Message`, weil der Optimierer sonst je Mandant einen
anderen Plan wählt und für manche auf einen vollen Durchlauf zurückfällt (13,2 s gegen 12 ms bei zwei
Mandanten, die *beide* keinen Zwischenschritt haben).

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

### Zwischenschritte sind ausgeblendet

`zwischenschritte=false` ist die Vorgabe und schließt `SPLITTED` und `MERGED` aus. Der Grund ist
keine technische Erwägung: **34,38 Prozent aller Zeilen** sind Zwischenprodukte (M6). Eine Liste,
die zu einem Drittel aus Begriffen besteht, die der Zielnutzer nicht kennt, kostet beim ersten
Kontakt Vertrauen.

**Ein ausdrücklicher Statusfilter auf `ZWISCHENSCHRITT` schlägt die Vorgabe.** Sonst filterte der
Nutzer danach und bekäme garantiert null Zeilen.

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
├─ Zeitpunkte  (Wanduhr ↔ UTC)       ├─ NachrichtResponse       DTO nach außen
│                                    └─ NachrichtenMerkmaleResponse  Stammdaten der Ansicht
├─ BamSpalte, BamSpaltenRegel
└─ MessageStatusClassifier
```

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
├─ api.ts                          Typen und die zwei Aufrufe
├─ filter.ts                       Filterzustand, rein — ohne React
├─ hooks.ts                        URL-Bindung, Blättern, Aktualisierung
└─ components/
   ├─ nachrichten-ansicht.tsx      der Zusammenbau, "use client"
   ├─ filterleiste.tsx             Zeitfenster, Status, Suche, Zwischenschritte
   ├─ prozess-filter.tsx           Mehrfachauswahl aus /api/prozesse
   ├─ nachrichten-tabelle.tsx      Spalten, Zeitpunkt, BAM-Zellen
   ├─ status-plakette.tsx          Status — nie allein über Farbe
   └─ blaettern.tsx                Seiten, Stand, automatische Aktualisierung
```

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

**Status nie allein über Farbe.** Jede Plakette trägt Beschriftung **und** Zeichen; die Farbrolle
ist die halbe Aussage. Bei `bedeutungNichtVerifiziert` wird der **Rohwert** zur Beschriftung, dazu
der Hinweis „Bedeutung nicht verifiziert" — statt einen plausiblen Text zu erfinden. Bei gesicherter
Einordnung steht der Rohwert im Tooltip, damit ein Anwender ihn gegen die alte Oberfläche halten
kann. Die Zuordnung Status → Farbe steht weiterhin an genau einer Stelle (`lib/status-farbe.ts`);
die Komponente kennt keine Farbe.

**Der Zeilenklick hat keine Funktion.** Kein Panel, kein Kopieren, kein Hover-Zustand — die
Hover-Färbung, die `components/ui/table` mitbringt, ist ausdrücklich abgeschaltet. Schritt 5 belegt
den Klick; bis dahin wäre ein Anfassgefühl ohne Wirkung schlimmer als gar keins.

**Am schmalen Fenster** fällt zuerst und einzig **Projekt** weg (unter `md`). Übrig bleiben
Zeitpunkt, Status und Ablauf. Der aktive Mandant bleibt bei jeder Breite in der Kopfzeile sichtbar
— das entscheidet der Anwendungsrahmen (bestehende Regel aus
[`visuelles-konzept.md`](visuelles-konzept.md) §6).

### 8.2 Filter und URL

In der URL stehen: `zeitraum` **oder** `von`/`bis` · `status` · `prozess` · `suche` · `langeSuche` ·
`zwischenschritte` · `sortierung`.

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

**Zwischenschritte sind ausgeblendet — und das steht sichtbar da.** Als Chip, der in einem Halbsatz
erklärt, was fehlt, und ihn einschaltet; und **ausdrücklich in der URL, ab dem ersten Rendern**. Das
ist kein Widerspruch zu „kein Standardwert im Frontend", sondern die andere Seite derselben Münze:
Hier wird ein Drittel aller Zeilen *weggelassen*, und was man sieht, muss man teilen können.

> **Der Chip erscheint nur, wo es etwas auszublenden gibt** (seit 07.08.2026). Die 34,38 Prozent aus
> Messung M6 waren ein Durchschnitt über **einen** Mandanten — `NEXANS` stellt 86 Prozent des
> Bestands, und dort sind es 39,6 Prozent. Messung [M12](messungen-schritt4.md) hat das
> aufgeschlüsselt: **Fünf von neun Mandanten mit Nachrichten haben über den gesamten Bestand nicht
> eine einzige Zwischenschritt-Zeile** — `IBIS`, `IBISGUS`, `ZAST`, `WOC` und `SYSTEM`, zusammen
> 112.801 Nachrichten. Für sie kündigte der Chip eine Ausblendung an, die nichts ausblendet, und ein
> Bedienelement ohne Wirkung ist schlimmer als keins.
>
> Die Auskunft kommt aus `GET /api/nachrichten/merkmale` (§1) und **nicht aus der Antwort jeder
> Seite**. Sie beschreibt den Gesamtbestand und nicht das Zeitfenster: `VOTG` hat 40
> Zwischenschritte über den ganzen Bestand und null im dichten Monat — ein Kriterium über das
> gewählte Fenster ließe den Chip dort erscheinen und verschwinden, ohne dass ein Zusammenhang
> erkennbar wäre.
>
> **Solange die Auskunft lädt, erscheint der Chip nicht.** Dieselbe Entscheidung wie beim
> Mandantenumschalter ([`visuelles-konzept.md`](visuelles-konzept.md) §5): Ein Bedienelement, das
> einen Moment später erscheint, ist besser als eines, das wieder verschwindet. **Scheitert sie,
> erscheint er** — wie vor der Nachbesserung.
>
> **`zwischenschritte` steht trotzdem in der URL, auch ohne Chip.** Sonst verhielte sich ein
> geteilter Link je nach Mandant anders — und genau den Unterschied soll die URL abbilden. Der
> Parameter wirkt weiterhin; er ist nur bei diesen Mandanten folgenlos, weil es nichts gibt, das er
> ausblenden könnte.

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
greifenden Einschränkungen — Zeitfenster, ausgeblendete Zwischenschritte, Suchbegriff, Status- und
Prozessfilter —, nicht nur die erste: Wer den Suchbegriff leert und immer noch nichts sieht, weil
auch der Statusfilter steht, käme sonst zweimal an dieselbe Wand. Dazu eine Schaltfläche „Auf 30
Tage erweitern".

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

### 8.4 Tests

| Datei | Was |
|---|---|
| `tests/nachrichtenfilter.test.ts` | URL → Zustand → URL; unbekannte Werte werden übergangen; **der Cursor taucht in keiner erzeugten URL auf**; die beiden Zeitfenstermodi schließen einander aus; `langeSuche` steht in der URL und wird nur zusammen mit dem Suchbegriff geschickt; **welche Problemtypen an das Suchfeld gehören, welche an die Zeitfensterfelder und welche über die Ansicht**, samt der beiden Zahlen aus der Antwort; **das halb ausgefüllte freie Fenster** wird erkannt und lässt die Liste stehen |
| `tests/format.test.ts` | UTC → Anzeige in der gelieferten Zone; Wanduhrzeit der Eingabefelder, auch am Umstellungstag |
| `tests/zwischenspeicher.test.ts` | das Ziel nach dem Mandantenwechsel trägt keine Filter — auch kein `langeSuche` |

---

## 9. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | kein Parameter, Mandant aus der Sitzung über `MandantService` |
| **M2** Mandant als erster Pflichtparameter | `NachrichtenRepository`; ArchUnit prüft es |
| **M3** Filter im Statement | `EXISTS` über `Process → ProjectMandant`, Teil jeder Bedingungsliste |
| **M4** Isolationstest je Endpunkt | `NachrichtenIsolationDbIT` — fuer die Liste **und** fuer `/api/nachrichten/merkmale` |
| **L1** Pflicht-Zeitfenster | `common/Zeitfenster`, Vorgabe 24 h, Maximum ein Jahr |
| **L2** keine Live-Aggregation | kein `COUNT`, `limit + 1` statt `total`; die Merkmale sind eine Existenzfrage mit `LIMIT 1` und zwischengespeichert (§1) |
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
- **Nachrichten wechseln den Prozess, während die `SOSID` stehen bleibt — und für Schritt 6 ist
  offen, was das für die Mandantengrenze bedeutet.** Nachgewiesen ist der Wechsel selbst (M10):
  **9.101 Zeilen**, bei denen `SOS.ProcessID` und `Message.ProcessID` auseinanderfallen — elf
  `SOS`-Zeilen, vierzehn Prozesse, verteilt über fünfzehn Monate und damit kein Ausreißer eines
  Tages. **Ungeprüft ist, ob Quell- und Zielprozess über `ProjectMandant` zum selben Mandanten
  führen.**

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

- **Eine tatsächlich hängende `SPLITTED`-Nachricht erscheint nie als überfällig.** `ZWISCHENSCHRITT`
  gilt als Endstatus (`message-status.md`), und die Überfälligkeitsrechnung setzt „nicht in einem
  Endstatus" voraus. Bleibt eine gesplittete Nachricht wirklich hängen, sieht man das **nicht** am
  Status, sondern erst über die Verkettung in Schritt 6 — dort fehlt dann die Fortsetzung. Das ist
  eine bewusste Entscheidung (sonst wären 34,38 Prozent aller Zeilen Kandidaten für „überfällig"),
  aber es ist eine Lücke, und sie gehört hier benannt.
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
- **Der Zeitzonen-Übergang** (Wanduhrzeit der Quelle → UTC der API) setzt voraus, dass Anwendungs-
  und Datenbankserver dieselbe Zone haben. Für die Testkopie ist das gemessen; für die Produktion
  ist es die Annahme, die die Anwendungsuhr ohnehin macht. Ein Auseinanderlaufen fiele als
  systematischer Versatz aller Zeitpunkte auf — und seit Aufgabe 11 an **einer** Stelle: Die
  Oberfläche formatiert mit derselben Zone, die das Backend zum Umrechnen benutzt.

### Zur Oberfläche (Aufgaben 13 bis 15)

- **Der Zeilenklick hat keine Funktion.** Das ist die Abgrenzung dieses Schritts, kein Versehen —
  Schritt 5 belegt ihn mit der Detailansicht.
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
