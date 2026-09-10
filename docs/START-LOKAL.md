# Lokal starten — was anders ist als produktiv

Stand: 10.09.2026 *(zuletzt ergänzt um die Ablagenprüfung, §4)*

Wie das Backend und das Frontend gestartet werden, steht in [`README.md`](../README.md) im
Wurzelverzeichnis. **Diese Datei beschreibt nur das, was lokal anders aussieht als in Produktion** —
und zwar so, dass niemand einem Fehler nachjagt, den es nicht gibt.

---

## 1. Der Rohdatenzugriff findet zwei Drittel des Bestands nicht

> **Von der Entwicklungsmaschine sind 63,2 % des Bestands nicht abrufbar. Produktiv gibt es das
> nicht.**

Die Dateiablagen hängen am **Zeitraum**, nicht am Mandanten. Die Rotationsgrenze liegt taggenau auf
dem **23.07.2025** (M53, Teil B): Alles davor verweist auf `FILESTOREPROD07` und `FILESTOREPROD08` —
und diese beiden Knoten sind **abgeschaltet**. Das sind **2.111.355 von 3.341.519 Nachrichten**.

Am jüngeren Ende ist es umgekehrt und ebenso ungünstig: Die Datenbankkopie reicht bis zum
`2026-07-08`, die Filestore-Kopie nicht. **153 von 153** Verweisen dieses Tages liefern nichts
(M66 (2)).

| Zeitraum | Ablage | lokal abrufbar? |
|---|---|---|
| bis `2025-07-23` | `FILESTOREPROD07`/`08` | **nein** — Knoten aus |
| `2025-07-24` bis `2025-12-30` | `FILESTOREPROD09`/`10` | **ja** |
| ab `2025-12-31` | `FILESTOREPROD09`/`10` | Datenbank hat Zeilen, die Ablage hat die Dateien nicht |

**Was du siehst, wenn du außerhalb des mittleren Fensters klickst:** den benannten Zustand
*Datei nicht vorhanden*. Das ist **kein Fehler im Code** und keine kaputte Verbindung — es ist die
richtige Antwort auf einen Verweis, hinter dem lokal nichts liegt. Ein Knoten, der gar nicht
antwortet, ergibt den anderen Zustand: *Ablage nicht erreichbar*.

**Deshalb ist die Dev-Uhr auf dieses Fenster begrenzt.** Der Zeitanker der Anwendungsuhr wird beim
Start aus der Testkopie ermittelt (`common/ZeitConfig`) und liegt seit dem 18.08.2026 immer
zwischen `2025-07-24` und `2025-12-30`. **Im Profil `prod` ändert das nichts**: Dort ist die
Anwendungsuhr die Systemuhr.

> **Es ist ein Riegel, keine Korrektur.** Am 18.08.2026 gemessen liefert die Ankerabfrage *mit* und
> *ohne* Fenster denselben Wert — `2025-12-30 04:09:47`. Der Anker lag also schon vorher richtig,
> seit er am 01.08.2026 von `MAX(MessageLastUpdate)` auf „jüngster Tag mit mindestens drei
> Mandanten" umgestellt wurde: In `2026-06` und `2026-07` hat an keinem Tag ein dritter Mandant
> Daten. **Ohne das Fenster hängt das aber am Bestand und nicht an einer Regel** — eine
> Neubefüllung der Testkopie mit drei Mandanten an einem jungen Tag genügte.

> Der Zeitanker taucht beim Start als `WARN`-Zeile im Protokoll auf: *„Dev-Clock aktiv:
> Anwendungszeit auf … zurückversetzt"*. Wenn dort ein Datum außerhalb des Fensters steht, ist etwas
> an der Testkopie anders als angenommen — dann lohnt der Blick, nicht vorher.

---

## 2. Die Testkopie ist langsam beim Schreiben

Ein `COMMIT` auf der Testkopie dauert **Sekunden**, nicht Millisekunden. Jede Anfrage schreibt die
Sitzung nach `overlord_monitor.SPRING_SESSION` — dadurch fühlt sich die ganze Oberfläche zäh an,
obwohl die Lesezugriffe unter einer Millisekunde liegen. **Das ist eine Eigenschaft der Testkopie
und kein Befund über den Code.**

---

## 3. Fünf leere Monate

Der Bestand ist dicht bis zum `30.12.2025`. Danach folgen **fünf Monate ohne eine einzige
Nachricht** (`2026-01` bis `2026-05`), dann `2026-06` mit 4.848 und `2026-07` mit 285 Nachrichten.
Ein relatives Zeitfenster über diesen Bereich sieht leer aus, weil er leer *ist*.

Warum die Lücke existiert, ist **nicht gemessen** — unvollständige Kopie oder Betriebspause. Sie
steht auch in `PROJEKTBESCHREIBUNG.md` §8 und in `messungen-schritt8.md` unter „Offene Punkte" 18.

---

## 4. Die Ablagenprüfung ist lokal aus — und wie man sie einschaltet

*Neu am 10.09.2026 (Schritt 10d Teil A).*

Das Dashboard trägt seit Schritt 10d eine **Ablagenkachel**, deren Zustand aus einer echten
Erreichbarkeitsprüfung stammt: je Takt ein `RETRIEVE` mit der Null-UUID gegen jede Ablage aus
`Service.ServiceDefaultFileStore` ([`dienste.md`](dienste.md) §7).

**Im Profil `dev` ist sie ausdrücklich aus.** Die Kachel zeigt dann `UNGEKLAERT` mit dem Grund
`ABGESCHALTET` — **das ist kein Fehler**, sondern genau die Auskunft, die sie geben soll. Der Grund
für die Abschaltung: Ein lokaler Start soll nicht unaufgefordert im Minutentakt einen **produktiven**
Knoten anfragen.

**Zum Ausprobieren einschalten:**

```
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--overlord.ablagenpruefung.aktiv=true
```

Danach steht in der Antwort von `GET /api/dashboard` unter `plattform.ablagen` der gemessene
Zustand. **Lokal ist das `ERREICHBAR` mit dem Ziel `FILESTOREPROD10`** — gemessen am 10.09.2026
(M174 und die Abnahme in [`dienste.md`](dienste.md) §13). Der erste Durchgang startet sofort; bis er
fertig ist, sagt die Kachel `NOCH_KEIN_DURCHGANG`.

**Was dabei wirklich passiert, und es ist kein Versehen:** Die Anwendung spricht dann im Minutentakt
die produktive Ablage an. Der Abruf ist ein `RETRIEVE` und kann konstruktionsbedingt nichts ablegen
(`messungen-schritt8.md`, QT1) — aber er geht an eine fremde Anlage, und deshalb ist er lokal aus und
wird einzeln eingeschaltet.

> **Die sieben Lampen daneben brauchen keinen Schalter.** Sie kommen aus `Service` und stehen lokal
> auf fünfmal `ZEITUEBERSCHRITTEN` und zweimal `HERUNTERGEFAHREN`; **`MELDET_SICH` gibt es auf der
> Testkopie nicht** — der grüne Pfad ist dort so wenig erreichbar wie `RUNNING`. Vier der sieben
> tragen `alterSekunden: null`, weil ihr `ServiceLastUpdate` **nach** dem Anker der Anwendungsuhr
> liegt. Auch das ist richtig und in [`dienste.md`](dienste.md) §13 ausgeschrieben.

---

## 5. Was lokal gar nicht angesprochen wird

- **Kein Test spricht einen Filestore an.** Der Zugriff läuft über die Schnittstelle
  `common/Ablagezugriff` *(bis zum 10.09.2026: `payload/Ablagezugriff`)*, und die wird in jedem Test
  ersetzt. **Die eine Ausnahme ist ausgewiesen und keine Ausnahme von dieser Regel:**
  `MessungM174DbIT` ist eine **Messung** und kein Test — sie fragt die Ablagen einmal, um zu
  belegen, dass die Prüfung überhaupt zwischen „erreichbar" und „aus" unterscheidet.
- **Es wird nie in `GlassfishDB` geschrieben.** Der Lese-Pool trägt zusätzlich den
  `ReadOnlyExecuteListener` als dritte Schicht.
- Die `db`-Tests laufen nur lokal (`./mvnw verify`); die CI schließt sie über
  `-DexcludedGroups=db` aus, weil sie die Testkopie im internen Netz nicht erreicht.
