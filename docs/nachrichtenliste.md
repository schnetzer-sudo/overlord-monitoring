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
      "bamWerte": [
        { "typ": 9006, "beschreibung": "Lieferschein-Nr._L_SAP",
          "werte": ["LS-000001", "LS-000002", "LS-000003"], "weitere": 2 },
        { "typ": 9001, "beschreibung": "Abrufnummer_L_SAP",
          "werte": [], "weitere": 0 }
      ]
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

`processName` und `projectName` dürfen `null` sein. Was der Nutzer anstelle einer fehlenden
Zuordnung liest, ist eine Oberflächenentscheidung und gehört in die Sprachdateien, nicht in eine
Abfrage (Regel Q4).

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
| `limit-ungueltig` | 400 | außerhalb 1 bis 200 |
| `cursor-ungueltig` | 400 | unlesbar **oder** Zeitpunkt außerhalb des Fensters |
| `kein-mandant-gewaehlt` | 403 | kein aktiver Mandant in der Sitzung |

**Kein unbekannter Wert wird stillschweigend auf die Vorgabe gezogen.** Wer `zeitraum=24` schreibt,
bekäme sonst 24 Stunden und hätte keinen Anlass, den Tippfehler zu bemerken.

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
> reichen von 2 ms bis zu einigen Sekunden — 1,3 Sekunden über 30 Tage im Fall von L7c.
>
> **Zwei Abhilfen wurden geprüft, beide tragen nicht** (Messungen M10 und L11, 06.08.2026). Die
> Auflösung über `SOS.ProcessID` zu einer einzigen Kennungsliste **verliert 9.101 Zeilen**, weil der
> Ablauf dort auf einen anderen Prozess zeigt als die Nachricht. Und die `UNION`-Fassung ist
> **langsamer** statt schneller: `Message.SOSID` hat **keinen Index**, der zweite Zweig muss also
> ebenfalls über das Zeitfenster einsteigen — der `UNION` liest das Fenster zweimal statt einmal.
> Der Stand und was daraus folgt, steht unter „Offene Punkte".

`%` und `_` im Suchbegriff werden maskiert. Ohne das wäre `_` ein Platzhalter für ein beliebiges
Zeichen — derselbe Fallstrick wie in Regel Q1.

---

## 6. Die BAM-Werte — zweite Abfrage je Seite

Die BAM-Werte sind das, woran ein Sachbearbeiter seinen Beleg wiedererkennt: Lieferschein-Nr.,
Bestellnummer, Transport-Nummer. Welche zwei Typen ein Mandant sieht, entscheidet die
Auflösungsregel aus [`datenzugriff.md`](datenzugriff.md) §5 (Vorrang der Kuratierung, sonst die zwei
kleinsten Sortierindizes, bei Gleichstand der kleinere Typ).

### Nicht als Join, sondern als zweite Abfrage

```sql
SELECT MessageID, MessageBAMType, MessageBAMValue
FROM MessageBAM
WHERE MessageID IN (…bis zu 200…)
  AND MessageBAMType IN (:typ1, :typ2)
```

Als Join wäre `MessageBAM` (10,9 Mio. Zeilen, 7,1 GB) Teil der Sortier- und Limit-Rechnung. Ein Typ
kann je Nachricht **mehrfach** vorkommen — der Wert steht im Primärschlüssel
`(MessageID, MessageBAMType, MessageBAMValue)` —, und `LIMIT 50` bedeutete dann 50 *Wertzeilen*
statt 50 Nachrichten. Die Seite hätte je nach Belegart eine andere Länge.

Der Einstieg über `MessageID` nutzt das Präfix dieses Primärschlüssels. Über `MessageBAMValue` wird
**nie** gefiltert, gruppiert oder sortiert (Regeln L4/L5); die Werte werden im Speicher sortiert, auf
höchstens ein paar hundert Zeichenketten.

Die Abfrage läuft **nach** dem Abschneiden der Seite: Die Zusatzzeile, an der `hasMore` erkannt wird,
bekommt keine Werte. Und sie läuft **gar nicht**, wenn der Mandant keine BAM-Konfiguration hat
(`EDITIONLINGERI`, `SYSTEM`, `WOC`) — kein Aufruf mit leerer Typliste.

**Der Mandant steht nicht im Statement** und muss es nicht: Die `MessageID`s stammen aus der Seite,
die das mandantengefilterte Hauptstatement gerade geliefert hat — eine fremde Kennung kann gar nicht
darunter sein. Der `MandantContext` ist trotzdem Pflichtparameter (Regel M2) und macht sichtbar, dass
die Methode nur mit einer solchen Liste aufgerufen werden darf.

### Darstellung

- Mehrere Werte eines Typs werden **zusammengefasst**, sortiert und ab dem vierten gekürzt;
  `weitere` sagt, wie viele fehlen. Eine stumm gekürzte Liste sieht aus wie eine vollständige.
- Doppelte Werte fallen weg — derselbe Lieferschein zweimal ist keine zusätzliche Auskunft.
- **Jede Zeile trägt so viele Einträge, wie der Mandant Spalten hat** — auch die Zeile, die dazu
  nichts zu sagen hat (dann mit leerer Werteliste). Sonst müsste die Oberfläche die Spalten je Zeile
  neu ausrichten.
- Ein Mandant ohne Konfiguration bekommt **keinen** Eintrag. Keine leere Spalte, kein Platzhalter —
  eine Spalte ohne Inhalt behauptet, es gäbe dort etwas zu sehen.

### Kosten

Zwei zusätzliche Abfragen je Seite: die Spaltenauflösung (Kuratierung plus Konfiguration, zusammen
unter 2 ms) und das Nachladen der Werte. Messungen L9 und L10 in
[`messungen-schritt4.md`](messungen-schritt4.md) zeigen den Verlauf über 50 und 200 Kennungen.

---

## 7. Aufbau im Code

```
common/                              message/
├─ Zeitfenster, Zeitraum             ├─ NachrichtenController   REST, nimmt nie eine Mandanten-ID
├─ Seitenposition, Seite             ├─ NachrichtenService      Fachlogik, Einordnung, Zeitpunkte
├─ Sortierrichtung                   ├─ NachrichtenRepository   jOOQ, MandantContext zuerst
├─ Zeitpunkte  (Wanduhr ↔ UTC)       ├─ BamSpaltenRepository    jOOQ, MandantContext zuerst
├─ BamSpalte, BamSpaltenRegel        ├─ NachrichtenFilter       geprüfte Parameter
└─ MessageStatusClassifier           └─ NachrichtResponse       DTO nach außen
```

Was in `common` liegt, liegt dort, weil ein zweites Fachpaket es braucht: Zeitfenster, Cursor und
Sortierung gehören zu **jedem** Listen-Endpunkt (Schritt 6, 7 und 10 folgen), die BAM-Spaltenregel
ab Schritt 7 auch dem Paket `bam`.

### Warum die BAM-Regel in `common` liegt, der Zugriff darauf aber nicht

Die Auflösungsregel aus [`datenzugriff.md`](datenzugriff.md) §5 steht als reine Rechenlogik in
`common/BamSpaltenRegel` — sie wird von `message` (Spalten der Liste) und ab Schritt 7 von `bam`
(Suchfelder) gebraucht, und Fachpakete kennen einander nicht.

Der **Datenzugriff** kann dort trotzdem nicht liegen, und zwar aus zwei Regeln, die zusammen nicht
erfüllbar sind:

- `common` darf von keinem anderen Anwendungspaket abhängen — also auch nicht von `MandantContext`
  aus `security`.
- Regel M2 verlangt `MandantContext` als **ersten Pflichtparameter** jeder Methode, die
  `jooq.glassfish` anfasst. `PaketstrukturTest` prüft beides maschinell.

Eine Klasse in `common`, die das Quellschema liest, müsste also gleichzeitig `MandantContext`
verlangen und ihn nicht kennen dürfen. Getrennt wird deshalb dort, wo die Entscheidung liegt: **die
Regel gemeinsam, die zwei Statements je Fachpaket.** Der Ausweg über rohes SQL (wie ihn `ZeitConfig`
für den Anker der Dev-Uhr geht) wäre hier falsch — er verstecke einen fachlichen Zugriff auf das
Quellschema vor genau der Prüfung, die ihn sichtbar machen soll.

---

## 8. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | kein Parameter, Mandant aus der Sitzung über `MandantService` |
| **M2** Mandant als erster Pflichtparameter | `NachrichtenRepository`, `BamSpaltenRepository`; ArchUnit prüft es |
| **M3** Filter im Statement | `EXISTS` über `Process → ProjectMandant`, Teil jeder Bedingungsliste |
| **M4** Isolationstest je Endpunkt | `NachrichtenIsolationDbIT` |
| **L1** Pflicht-Zeitfenster | `common/Zeitfenster`, Vorgabe 24 h, Maximum ein Jahr |
| **L2** keine Live-Aggregation | kein `COUNT`, `limit + 1` statt `total` |
| **L3** keine `OFFSET`-Paginierung | Cursor über `(MessageLastUpdate, MessageID)` |
| **L4/L5** `MessageProperty`/BAM nur über die Kennung | `MessageProperty` wird nicht angefasst; Suche nur über Stammdaten, mit Mindestlänge und Deckel |
| **L7** jede Abfrage gemessen | [`messungen-schritt4.md`](messungen-schritt4.md), Abschnitte L1 bis L10 |
| **Z1** kein `now()` | Zeitfenster über die Anwendungsuhr, aufgelöst in `common` |
| **Q1** Fehlerbedingung | ausschließlich `MessageStatusClassifier.fehlerBedingung` |
| **Q4** nicht zugeordnet heißt nicht zugeordnet | `processName`/`projectName` bleiben `null` |

---

## 9. Offene Punkte

- **Der Freitextfilter bleibt teuer, und beide vorgesehenen Abhilfen sind widerlegt** (geprüft am
  06.08.2026, Messungen M10 und L11). Der Punkt bleibt offen — aber er ist jetzt ein *bekannter*
  offener Punkt und keine ungehobene Verbesserung:

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
  gehört uns nicht, Regel S1 verbietet jedes DDL. Damit ist die Mindestlänge (Regel L5), die
  Entprellung im Suchfeld und ein enges Zeitfenster die einzige verfügbare Abhilfe. Sollte die
  Suche in Produktion zum Problem werden, ist die nächste zu prüfende Stufe eine **Obergrenze für
  das Zeitfenster bei gesetztem Suchbegriff** — eine fachliche Einschränkung statt einer
  technischen, und deshalb eine Entscheidung und keine Umsetzung.
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
- **Die BAM-Spaltenauflösung läuft je Anfrage** (zwei kleine Abfragen, zusammen unter 2 ms). Ein
  Zwischenspeicher je Mandant wäre möglich, bringt aber eine Invalidierungsfrage mit — offen, bis
  eine Messung zeigt, dass es sich lohnt.
- **Kein `SOSName` in der Liste.** Der Anzeigename des Ablaufs kommt in Schritt 5; hier wäre er ein
  weiterer Join ohne Nutzen für die Frage „wo steht mein Beleg".
- **Der Zeitzonen-Übergang** (Wanduhrzeit der Quelle → UTC der API) setzt voraus, dass Anwendungs-
  und Datenbankserver dieselbe Zone haben. Für die Testkopie ist das gemessen; für die Produktion
  ist es die Annahme, die die Anwendungsuhr ohnehin macht. Ein Auseinanderlaufen fiele als
  systematischer Versatz aller Zeitpunkte auf.
