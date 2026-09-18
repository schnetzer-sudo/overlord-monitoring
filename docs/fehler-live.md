# Fehler live — Teil A: der Baustein und die Übersicht

*18.09.2026.* Auftrag „Fehler live, Teil A (Baustein und Übersicht)", Stand 18.09.2026, Zweig
`feat/fehler-live`. Schritt **10f** im [Implementierungsplan](IMPLEMENTIERUNGSPLAN_MVP.md).
Entscheidungen **E‑208** bis **E‑212**, Messung **M188**, offene Punkte **209** bis **215**.

> *Teil B, am selben Tag:* Auftrag „Fehler live, Teil B (Prozessbaum)", Stand 18.09.2026, derselbe
> Zweig — Teil A lag noch nicht in `main`. Entscheidungen **E‑213** und **E‑214**, Messung **M189**,
> offener Punkt **216**; **Punkt 209 ist geschlossen.** Der Prozessbaum steht in §5b.

**Die Frage dieser Datei:** Wie hört die Übersicht auf, eine Nachricht als Fehler zu zählen, die
nach einer Nachverarbeitung keiner mehr ist — ohne den Rollup anzufassen und ohne dass Kachel,
Verlauf und Verteilung auseinanderlaufen? *Und mit Teil B:* Wie zählt der Prozessbaum dieselben
Fehler wie die Übersicht, über denselben Baustein?

> ### Der Ertrag in einem Absatz
>
> Die Übersicht liest die Einordnung `FEHLER` seit heute aus einer **Live-Lesung über `Message`**
> (`common/FehlerLiveRepository`): je Stunde, Prozess und Rohstatus die Nachrichten des Fensters, die
> **jetzt** die Fehlerbedingung erfüllen. **Nach der Verrechnung des Live-Rests** fallen die
> Fehlerzeilen aus Rollup und Korrektur heraus, und die der Lesung kommen auf dem Eimer des Paares
> hinzu (`common/FehlerLiveErsatz`, eine reine Funktion). Block 5 liest dann den Rollup **ohne**
> Fehler, und die Fehlerzeilen gehen wie die Korrekturzeilen über die Katalog-Nachlesung. Der Fall aus
> der Produktion — ein nachverarbeiteter `ERROR_TIMEOUT`, der bis zum Volllauf als Fehler zählte — ist
> im Bau behoben; die Diensttests stellen ihn nach (ausgesetzt Fehler 1 und Nachrichten 2, angewandt 0
> und 1), an der Produktion ist er nicht nachgemessen. **Alle drei Tore halten** (M188): Die Lesung
> steigt in allen zwölf Lagen über `MessageStatusIDX` ein, liest 3.412 bis 3.413 Indexsätze und kostet
> 20,1 bis 31,5 ms in SQL; die Verteilung ohne Fehler fährt Zeile für Zeile den Plan aus M178 und
> kostet höchstens das 1,062-Fache davon; die Seite liegt durch den Endpunkt bei höchstens 418,4 ms.
> Auf der Testkopie ist der Ersatz eine Identität — keine Zahl der Übersicht hat sich geändert. Fällt
> die Lesung aus, rechnet die Seite wie vorher und sagt es über den Kacheln.
>
> **Drei Dinge waren nicht selbstverständlich:** Die Erwartung „20 bis 26 ms, unabhängig von Fenster und
> Mandant" trifft für die gelesene Menge zu, nicht ganz für die Zeit — mit 711 Fehlern im Fenster
> kostet die Lesung bei denselben Indexsätzen rund 10 ms mehr. Der Zuschlag der Seite ist die Lesung
> **plus** ein kleiner, aber systematischer Preis der Bedingung in beiden Verteilungsstatements (bis
> 3,7 ms je Statement), den die Vorregistrierung mit null angesetzt hatte. Und eine Nachricht, die aus
> einem **anderen** Status heraus ihren Status wechselt, zählt zwischen zwei Volllaufen weiter
> doppelt — das ist jetzt benannt (bekannte Grenze 3 berichtigt, Punkt 210) und bewusst nicht gebaut.

> ### Teil B in einem Absatz *(18.09.2026)*
>
> Der Prozessbaum ruft **denselben Baustein**: die Einordnung `FEHLER` aus der Live-Lesung, alle
> anderen aus Rollup und Live-Rest — ein zweiter Verbraucher der vierten Ausnahme, **kein fünfter
> Fall**. Die Lesung ist das **letzte** Statement eines Aufrufs, über das Fenster der Antwort; der
> Ersatz sitzt je (Prozess, Rohstatus) **nach** der Verrechnung des Live-Rests und **vor** Kennzahl und
> Klemme. **Baum und Übersicht zählen Fehler seither gleich** — auf der Testkopie sind Kopfzahl und
> Kachel in allen neun Paaren dieselben, und Punkt 209 ist geschlossen; der Fall aus der Produktion
> steht je Prozess in den Diensttests. **Alle vier Tore von M189 halten:** Die Lesung im
> Jahresfenster mit den meisten Fehlern (3.204) steigt über `MessageStatusIDX` ein und kostet 69,8 ms;
> der Baum bleibt in der typischen Stunde und im freien Fenster unter 200 ms (höchstens 167,2 ms — die
> alte Schranke von 150 ms hätte er gerissen, deshalb die neue des Auftraggebers) und im dichtesten
> Bereich unter 500 ms (höchstens 349,3 ms). Der Zuschlag ist die Lesung. Bei `AUSGESETZT` steht der
> Hinweis im Kopf der Baumspalte, unter dem zum Live-Rest.
>
> **Ein Fund daneben, nicht umgebaut:** Die Fensterverengung der Nachrichtenliste liest die Einordnung
> `FEHLER` weiter aus dem Rollup — nach einem Abgang ist ihre Zählung keine Unterschranke mehr
> (Punkt 216).

---

## Nummernvergabe

Gesucht am 18.09.2026 vor der ersten Vergabe mit `git grep -E` über `docs/**/*.md`,
`DEVELOPMENT_GUIDELINES.md`, `backend/src`, `frontend/src` und `frontend/tests` auf `HEAD`, über die
zwei Zweige, die nicht in `main` sind (`feat/suchfeld-untermenues`, `test/indexbestand-e37`), und mit
`grep -rE` über die Arbeitsbäume `overlord-monitoring-e37` und `overlord-monitoring-neu-laden`. Der
Strich als `-` **oder** U+2011 über `E.{0,3}<Zahl>\b` — die Zeichenklasse `[‑-]` findet den
geschützten Bindestrich nicht. **Geeicht** an bekannt vergebenen Nummern: auf `HEAD` E‑207 (10
Treffer), M187 (9) und Punkt `**208**` (3); auf `feat/suchfeld-untermenues` M150 (9), auf
`test/indexbestand-e37` M44 (66). Jeder Treffer ist gelesen, nicht gezählt.

| | |
|---|---|
| **Entscheidungen** | **ab E‑208.** Höchste vergebene: **E‑207** ([`dateiansicht-darstellung.md`](dateiansicht-darstellung.md), 18.09.2026). Der einzige Treffer auf E‑208/E‑209 ist dort der Suchvermerk *„die Suche nach E‑206 bis E‑209 … war leer"* — gelesen, keine Vergabe. **E‑780** ist der bekannte Falschtreffer. Kein Zweig und kein Arbeitsbaum trägt E‑208 oder höher. *Der Auftrag erwartete „ab E‑193" — vergeben seit dem 17.09.2026 an die Darstellungswahl der Dateiansicht (E‑193 bis E‑207)* |
| **Messung** | **M188.** Höchste: **M187** (Sichtprüfung der Darstellungswahl, [`dateiansicht-darstellung.md`](dateiansicht-darstellung.md) §11). Kein Treffer auf `\bM18[89]\b` oder `\bM19[0-9]\b` — weder auf `HEAD` noch in den Zweigen oder Arbeitsbäumen. *Der Auftrag erwartete M187* |
| **Offene Punkte** | **ab 209.** Höchster: **208** ([`dateiansicht-darstellung.md`](dateiansicht-darstellung.md)). Die Treffer auf `**209**` bis `**219**` sind Zahlen in Tabellenzellen und im Fließtext der Messdateien (`messungen-schritt7.md` **216**, **219**; `messungen-schritt9.md` **217**; `messungen-schritt10.md` **216**; `messungen-schritt10b.md` **212**; die Zeile zu `messungen-schritt7.md` in [`README.md`](README.md)) — gelesen, keine Punkte. *Der Auftrag erwartete Punkt 196 — vergeben an dieselbe Runde (196 bis 208)* |
| **Schritt** | **10f.** Kein Treffer auf `\b10f\b` oder `10‑f` in keiner Quelle; die Darstellungswahl ist ein Nachtrag zu Schritt 8 und kein eigener Schritt |

> **Teil B, 18.09.2026, vor der ersten Vergabe** — dieselben Quellen mit `git grep -n -E` auf
> `HEAD` (`docs/*.md`, `DEVELOPMENT_GUIDELINES.md`, `backend/src`, `frontend/src`, `frontend/tests`,
> dazu `scripts/`), über die zwei Zweige, die nicht in `main` sind (`feat/suchfeld-untermenues`,
> `test/indexbestand-e37`), und mit `grep -rnE` über die Arbeitsbäume `overlord-monitoring-e37` und
> `overlord-monitoring-neu-laden`. Der Strich über `E.{0,3}<Zahl>\b`. **Geeicht** auf `HEAD`: E‑212
> (7 Zeilen, alle mit U+2011), die ASCII-Form an E‑208 im Java-Code (20 Zeilen `E-208`, 38 mit dem
> Muster), M188 (33 Zeilen), `**215**` (3); auf `feat/suchfeld-untermenues` M150 (12), auf
> `test/indexbestand-e37` M44 (69), in den Arbeitsbäumen M44 (89 und 114). Jeder Treffer gelesen.
>
> | | |
> |---|---|
> | **Entscheidungen** | **ab E‑213.** Höchste vergebene: **E‑212** (Teil A). Kein Treffer auf `E.{0,3}2(1[3-9]\|2[0-9])\b` — weder auf `HEAD` noch in den Zweigen, den Arbeitsbäumen oder `scripts/`. Die bekannten Falschtreffer (E‑780; die Zeile *„Der Bereich M86–M99 ist frei"* in [`messungen-schritt10.md`](messungen-schritt10.md)) trifft dieses Muster nicht |
> | **Messung** | **M189.** Höchste: **M188** (Teil A). Kein Treffer auf `\bM(189\|19[0-9])\b`, auch nicht in `scripts/` |
> | **Offene Punkte** | **ab 216.** Höchster: **215** (Teil A). Die Treffer auf `**216**` bis `**229**` sind Zahlen in Tabellenzellen und im Fließtext, keine Punkte: [`messungen-schritt7.md`](messungen-schritt7.md) **219** (2), **216** (3); [`messungen-schritt9.md`](messungen-schritt9.md) **226** (3), **217** (2), **224** (2), **222**; [`messungen-schritt10.md`](messungen-schritt10.md) **216**; [`bam-suche.md`](bam-suche.md) **219**; [`process-view.md`](process-view.md) **229** (2); [`prozess-katalog-backend.md`](prozess-katalog-backend.md) **224**; zwei Zeilen in [`README.md`](README.md) (**226**, **216**) und die Nummernvergabe oben |
> | **Schritt** | **10f** — Teil B ist kein eigener Schritt |
>
> *Der Auftrag erwartete „ab E‑213, M189, Punkt 216" — zutreffend.*

---

## 1. Anlass

**Gemeldet vom Auftraggeber am 18.09.2026, aus der Produktion:** Eine Nachricht stand auf
`ERROR_TIMEOUT` und wurde korrekt als Fehler gezählt. Nach der Nachverarbeitung stand sie auf
`RUNNING`. „Zuletzt aufgefallen" und die Kachel *Läuft* zeigten das sofort — beide lesen live. **Die
Fehlerkachel und der Verlauf zählten sie weiter als Fehler.**

**Die Ursache — hergeleitet, nicht an der Produktion gemessen.** Der Rollup bucht eine Nachricht in
den Stundeneimer von `MessageLastUpdate` ([`rollup.md`](rollup.md) §7), und ein Statuswechsel setzt
`MessageLastUpdate` neu: Die Nachricht wandert in die aktuelle Stunde. Der Delta-Lauf schreibt nur die
vorige und die laufende Stunde neu (§3, §5 dort), der Live-Rest korrigiert nur ab G
([`live-rest.md`](live-rest.md) §2). **Der alte Eimer behält den Fehler deshalb bis zum Volllauf um
03:00.** Das ist kein *Nachschreiben* im Sinne von Punkt 49 in [`rollup.md`](rollup.md) §13 — dort
wandert ein Zeitstempel um Sekunden, und das Nachlauffenster fängt ihn —, sondern ein **Abgang aus
einem alten Eimer**, und kein Nachlauffenster erreicht ihn. Die bekannte Grenze 3 in
[`dashboard.md`](dashboard.md) §2 (*„doppelt gezählt wird also nichts"*) gilt erst nach dem Volllauf.

**Vorhergesagt war der Mechanismus**, gemessen werden konnte er nicht: M96
([`messungen-schritt10b.md`](messungen-schritt10b.md)) hält fest, dass eine Nachricht ihren Status
ändern kann, *nachdem* ihr Eimer gerollt wurde, und dass der Satz *„im Betrieb springt nichts"*
damit nicht belegt ist — auf der eingefrorenen Testkopie tritt der Fall nie ein.

---

## 2. Die Regel

> **Die Übersicht liest die Einordnung `FEHLER` nicht mehr aus dem Rollup, sondern aus einer
> Live-Lesung über `Message`. Alle anderen Einordnungen kommen weiter aus Rollup und Live-Rest.**

Das ist die **vierte benannte Ausnahme** von Regel L2 ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
§8, Regel 2) — nach den Kacheln *Läuft* und *Wartend* (E‑72, E‑73) und dem Live-Rest der laufenden
Stunde ([`live-rest.md`](live-rest.md)).

**Die Begründung, dort eingetragen:** Ein Fehlerstatus ist durch Nachverarbeitung nicht endgültig —
eine Nachverarbeitung setzt `RUNNING` ([`message-status.md`](message-status.md)) —, und der
Delta-Lauf entfernt einen Abgang aus einem alten Eimer nicht. Anders als bei den drei Ausnahmen davor
hängt die Zahl weder an einer Frist noch an einem flüchtigen Status noch am Takt des Laufs, sondern
daran, dass **ein Endstatus zurückgenommen werden kann**.

**Der Umfang:** die Übersicht, und dort allein die Einordnung `FEHLER` — in Verlauf, Kachel *Fehler*
samt Fehlerarten und in beiden Sichten der Verteilung. Der Prozessbaum ist Teil B, ein eigener
Auftrag (Punkt 209).

> ### Ergänzt am 18.09.2026 — Teil B: der Prozessbaum ist der zweite Verbraucher (§5b)
>
> **Die Regel oben gilt unverändert**, und ihr Umfang ist seither die Übersicht **und** der
> Prozessbaum: dort *Fehler* je Prozess, je Gruppe und in der Kopfzahl, in allen Modi und beiden
> Gliederungen. **Das ist dieselbe vierte Ausnahme von L2 und kein fünfter Fall** — derselbe Baustein,
> dieselbe Lesung, derselbe Ersatz; so eingetragen im Kasten der vierten Ausnahme in
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Regel 2. Wie beim Live-Rest, dessen Teil B
> die Übersicht als zweiten Verbraucher dazunahm.

---

## 3. Die Entscheidung des Auftraggebers — Weg B (E‑208)

**Entschieden vom Auftraggeber am 18.09.2026, per Auswahl aus vier Wegen.** Alle vier gehören ins
Protokoll, auch die verworfenen; beschrieben ist jeder nur so weit, wie der Auftrag ihn benennt.

| Weg | Was er täte | Entschieden |
|---|---|---|
| **A** | Abgleich im Delta-Lauf | verworfen |
| **B** | **Fehler live** — die Einordnung `FEHLER` der Übersicht aus einer Live-Lesung über `Message` | **genommen (E‑208)** |
| **C** | das Nachlauffenster des Delta-Laufs auf 48 Stunden | verworfen |
| **D** | nur dokumentieren | verworfen |

---

## 4. Der Baustein in `common` (E‑209)

Gebaut wie der Live-Rest ([`live-rest.md`](live-rest.md) §6): eine Lesung, ein Dienst, eine reine
Funktion und ein Antwortblock — **in `common`**, weil mit Teil B der Prozessbaum denselben Baustein
ruft und Fachpakete einander nicht kennen.

| Klasse | Was sie ist |
|---|---|
| `FehlerLiveRepository` | **die Lesung** — `ausDerQuelle(MandantContext, von, bis)`, über `glassfishDsl` |
| `FehlerLiveService` | **der Dienst** — `ermittle(mandant, von, bis)`, mit dem Ausfall nach E‑185 |
| `FehlerLiveErsatz` | **die reine Funktion** — der Ersatz, ohne Datenbank |
| `FehlerLiveErgebnis`, `FehlerLiveZeile`, `FehlerLiveZustand` | Zustand und Zeilen; nur `ANGEWANDT` trägt Zeilen, ein Widerspruch fällt im Konstruktor |
| `FehlerLiveResponse` | **der Block `fehlerLive { zustand }`** der Antwort |

### Die Lesung

Der Text, den `DashboardStatementsTest.FehlerLive.die_lesung_woertlich` Zeichen für Zeichen pinnt —
hier gekürzt um die Qualifizierung:

```sql
SELECT date_format(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus, count(*)
FROM GlassfishDB.Message
WHERE (MessageStatus LIKE ? ESCAPE '\' OR MessageStatus = ?)       -- fehlerBedingung, gerufen
  AND MessageLastUpdate >= ? AND MessageLastUpdate < ?
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process AS fehler_process
              JOIN GlassfishDB.ProjectMandant ON ProjectMandant.ProjectID = fehler_process.ProjectID
              WHERE fehler_process.ProcessID = Message.ProcessID AND ProjectMandant.MandantID = ?)
GROUP BY date_format(MessageLastUpdate, '%Y-%m-%d %H:00:00'), ProcessID, MessageStatus
```

| Regel des Auftrags | Wie gehalten | Belegt in |
|---|---|---|
| Fehlerbedingung und Stundenbildung gerufen, nicht nachgebaut | `MessageStatusClassifier.fehlerBedingung(MESSAGE.MESSAGESTATUS)` und `Stundeneimer.ausdruck(MESSAGE.MESSAGELASTUPDATE)` | `die_lesung_woertlich` |
| Keine Funktion um `MessageLastUpdate` in der Bedingung | halboffener Bereich auf der Spalte | `die_lesung_gestalt` |
| `GROUP BY` über den vollen Ausdruck (Befund 11) | derselbe `Field` in `SELECT` und `GROUP BY`, kein Alias | `die_lesung_gestalt` |
| Mandantenkette als `EXISTS`, nie als Join | eigene Kette mit eigenem Alias `fehler_process`, dieselbe Form wie in `LiveRestRepository` | `die_lesung_gestalt`, Verletzungsprobe (§9) |
| Kein Indexhinweis | keiner — dass trotzdem `MessageStatusIDX` den Einstieg macht, belegt der Plan | `DashboardPlanDbIT.fehlerlesung_faehrt_ueber_den_statusindex`, M188 Tor 1 |

**Ohne Sortierung und ohne Deckelung:** Die Verbraucher summieren in ihre eigenen Eimer. Genau das
unterscheidet die Lesung von Block 6, dessen `ORDER BY … LIMIT 10` den Zeitindex verlockend machte
und deshalb den einzigen Indexhinweis des Projekts trägt ([`dashboard.md`](dashboard.md) §7a).

### Der Dienst — der Ausfall wie beim Live-Rest

**Dieselbe Ausnahmebehandlung wie `LiveRestService`, nicht weiter und nicht enger:** Jede
`DataAccessException` ergibt `AUSGESETZT` und ein `WARN` im Protokoll — die Zeitgrenze des
Lese-Pools ebenso wie ein Syntaxfehler, denn der Live-Rest unterscheidet dort nicht. Was keine
`DataAccessException` ist, läuft durch. Der Verbraucher rechnet dann wie vor diesem Schritt.

> **Das ist nicht der enge Fang der Kacheln *Läuft* und *Wartend***, der nur die Zeitgrenze
> abfängt ([`dashboard.md`](dashboard.md) §5). Dort fehlt bei einem Abbruch eine Zahl, und sie heißt
> „nicht ermittelbar"; hier fehlt keine — die Fehler kommen dann aus dem Rollup, wie bis gestern, und
> die Oberfläche sagt es. Der Auftrag verlangt ausdrücklich die Haltung von E‑185.

### Der Ersatz — die reine Funktion

`FehlerLiveErsatz.ersetze(zustand, grundzeilen, liveZeilen, rohstatus, klassifizierer)`:

1. Aus den Grundzeilen fallen **alle**, die `MessageStatusClassifier.einordnung` als `FEHLER`
   einordnet — auch ein unbekanntes `ERROR_…`, eine andere Schreibweise und `COMMIT_REJECTED`.
2. Die Zeilen der Lesung kommen hinzu, **über denselben Schlüssel**: Der Verbraucher hebt sie
   vorher auf die Gestalt seiner Grundzeilen.

Bei `AUSGESETZT` bleiben die Grundzeilen, wie sie sind; eine Live-Zeile in diesem Zustand wäre ein
Widerspruch und fällt laut. **Die Einordnung entsteht beim Lesen** (E‑g): Die Zeilen tragen
Rohwerte, und welche Fehlerart sie tragen, sagt danach derselbe Klassifizierer wie immer.

**Warum ersetzen und nicht verrechnen:** Der Live-Rest verrechnet, weil der Rollup vor G stimmt. Bei
den Fehlern stimmt er auch vor G nicht. Die Fehlerzeilen des Rollups abzuziehen und die der Quelle
dazuzuzählen ergäbe je Schlüssel genau die Zeilen der Quelle — ersetzen ist dieselbe Rechnung ohne den
Umweg.

### Der Block `fehlerLive`

```jsonc
"fehlerLive": { "zustand": "ANGEWANDT" }   // oder "AUSGESETZT"
```

**Benannt nach der gelebten Konvention** — `liveRest` / `LiveRestZustand` / `LiveRestResponse`
wird `fehlerLive` / `FehlerLiveZustand` / `FehlerLiveResponse`; keine Abweichung. **Ohne
Zeitangabe**, anders als `liveRest.vollstaendigBis`: Ein Abgang kann jeden Eimer vor dem letzten
Volllauf treffen, und es gibt keinen Zeitpunkt, bis zu dem die Fehlerzahlen des Rollups stimmen.

> **Eine Eigenschaft, auf der der Ersatz ruht:** SQL und Java müssen sich einig sein, was ein Fehler
> ist. Die Lesung wählt in SQL aus, der Ersatz entfernt in Java. Für jeden bekannten Rohwert, jede
> Groß- und Kleinschreibung und jedes `ERROR_…` sind sie es — dafür stellt der Klassifizierer den
> Rohwert hoch ([`message-status.md`](message-status.md)). Nicht für Rohwerte, die nur die Sortierung
> der Spalte gleichmacht: `'COMMIT_REJECTED '` mit Leerzeichen am Ende ist unter `PAD SPACE` gleich
> `'COMMIT_REJECTED'`, für Java nicht. Auf der Testkopie kommt kein solcher Wert vor; der Test gegen
> neue Statuswerte fiele bei seinem ersten Auftreten. Offener Punkt **213**.

---

## 5. Die Übersicht (E‑210, E‑211)

### Die Reihenfolge der Statements

**Die Lesung läuft an zweiter Stelle** — nach dem Verlauf und **vor Block 5**, weil dessen zwei
Statements an ihrem Zustand hängen (E‑210). Alle folgenden rücken um eins; der Live-Rest und die
Nachlesung bleiben am Ende. `DashboardStatementsTest.FehlerLive` benennt jede Lage einzeln:

| Lage | Statements | Folge |
|---|---:|---|
| Lesung angewandt, kein Fehler im Fenster, Live-Rest ohne Lauf | **11** | Verlauf · **Fehlerlesung** · Verteilung Partner **ohne Fehler** · Richtung **ohne Fehler** · Läuft · Erscheinungsbedingung · Wartend · Zuletzt aufgefallen · Stand · Dienste · Wasserstand |
| … mit Fehler im Fenster | **12** | dieselben elf · **Nachlesung** |
| Lesung ausgesetzt | **11** | dieselben elf, die zwei Verteilungsstatements **im heutigen Wortlaut** |
| ausgesetzt, Live-Rest angewandt mit Korrekturzeile | **14** | dieselben elf · Live-Rest A · B · Nachlesung — wie vor diesem Schritt, plus die ausgefallene Lesung |
| angewandt, Live-Rest angewandt ohne Korrekturzeile | **13** | die elf · A · B |
| angewandt, Live-Rest mit Korrekturzeile — mit oder ohne Fehler im Fenster | **14** | die elf · A · B · **eine** Nachlesung für alle Prozesse |
| angewandt, die einzige Korrekturzeile ist ein Fehler | **13** | die elf · A · B — Block 5 hat nichts zuzurechnen |

Mit genanntem `zeitraum` also elf bis vierzehn, ohne ihn zwölf bis siebzehn (eine bis drei
Belegungsproben davor, [`dashboard.md`](dashboard.md) §3).

### Blöcke 1 bis 3 — der Ersatz nach dem Live-Rest

Die Zeilen des Verlaufs entstehen wie seit E‑190: Rollupzeilen plus die Korrektur des Live-Rests, je
(Eimer, Rohstatus) auf null geklemmt. **Danach** der Ersatz: Die Fehlerzeilen fallen heraus, und die
Zeilen der Lesung kommen hinzu — **auf den Eimer des Paares gehoben** wie in E‑190 (`48H` die Stunde,
`30T` `DATE(stunde)`, `12M` der Monatserste) und je (Eimer, Rohstatus) summiert
(`Liveverrechnung.gehoben`). Verlauf, Kachel *Nachrichten*, Kachel *Fehler* und die Fehlerarten
entstehen daraus **wie bisher**.

**Warum nach dem Live-Rest und nicht davor:** Die Korrektur trägt ihre eigenen Fehlerzeilen (ein
Fehler in der laufenden Stunde steht dort mit Plus). Stünde der Ersatz davor, kämen sie hinterher
wieder hinein, und derselbe Fehler zählte zweimal — `DashboardServiceTest.fehler_im_live_bereich_zaehlt_einmal`.

### Block 5 — ohne die Fehler des Rollups, die Lesung über die Nachlesung

| Zustand der Lesung | Die zwei Verteilungsstatements | Was Block 5 über die Nachlesung zugerechnet bekommt |
|---|---|---|
| **angewandt** | `verteilungOhneFehler` — dieselbe Gestalt mit **einer** Bedingung mehr hinter der Mandantenkette: `and not (message_status like ? escape '\' or message_status = ?)` | die Korrekturzeilen des Live-Rests **ohne** Fehlerzeilen, und die Fehlerzeilen der Lesung — beide je Prozess summiert, dem Schlüssel über die Katalog-Nachlesung zugeordnet (E‑191) |
| **ausgesetzt** | `verteilung` — **Zeichen für Zeichen der heutige Wortlaut** | die Korrektur des Live-Rests wie bisher |

**Die heutige Form ist unverändert, und das ist belegt, nicht angenommen:** Beide Formen teilen eine
Methode, und die Zusatzbedingung ist für die heutige `DSL.noCondition()`, die jOOQ nicht rendert.
Der gerenderte Text aller zwölf Fassungen (drei Paare × zwei Sichten, mit Platzhaltern und mit
Literalen) ist vor und nach dem Umbau mit `cmp` verglichen worden: gleich. Seither pinnt
`DashboardStatementsTest` beide Fassungen wörtlich.

**So zählen Kachel und Sichten in beiden Zuständen gleich:** angewandt nehmen beide die Fehler aus
der Lesung, ausgesetzt beide aus dem Rollup. `DashboardIsolationDbIT.sichtZaehltDieEigenenNachrichten`
läuft auf der Testkopie seither über die Nachlesung (`SUTTONS` hat Fehler in `30T` und `12M`) und ist
grün.

> **Eine Auslegung, gemeldet (E‑211).** Der Auftrag sagt: *„Die Nachlesung läuft, sobald es
> Korrekturzeilen oder Live-Fehlerzeilen im Fenster gibt."* Gebaut ist: sobald es im Fenster
> **Zeilen für Block 5** gibt — Korrekturzeilen ohne Fehler oder Fehlerzeilen der Lesung. Der
> Unterschied ist eine Lage: Die einzigen Korrekturzeilen sind Fehler, und die Lesung ist angewandt.
> Dann hat Block 5 nichts zuzurechnen, und die Nachlesung fragte nach Prozessen, deren Zeilen sie
> gleich wieder verwirft. Sie läuft dann nicht (`angewandt_korrekturzeile_ist_ein_fehler`). Bei
> ausgesetzter Lesung ist der Wortlaut des Auftrags genau erfüllt.

### Übergreifend

- **Ein Uhrenschlag:** Die Lesung bekommt das Fenster, das die Seite aus ihrem einen `jetzt` bildet;
  der Live-Rest bekommt dasselbe `jetzt` (`DashboardServiceTest.ein_uhrenschlag`).
- **Der Leerzustand folgt der Kachel *Nachrichten* nach dem Ersatz** — in beide Richtungen: War der
  einzige Eintrag ein Fehler, den es nicht mehr gibt, ist die Seite leer; ein Fehler, den der Rollup
  noch nicht kennt, macht sie nicht leer.
- **Bei `liveRest = AUSGESETZT` zählt die Lesung trotzdem bis `bis`.** So gelassen und hier benannt:
  **Die Fehler sind dann vollständiger als der übrige Verkehr.** Ein Fehler der letzten Stunden steht
  in der Kachel *Fehler*, während die Kachel *Nachrichten* ihn nur über die Fehlerzeile kennt — die
  übrigen Nachrichten derselben Stunden fehlen, und der Hinweis zum Live-Rest sagt das.
- **Unverändert:** *Läuft*, *Wartend* samt Erscheinungsbedingung, Block 6 *Zuletzt aufgefallen* mit
  seinem Indexhinweis, *Stand*, *Plattform* und die Belegungsprobe des Standardfensters.
- **Kachel und Liste lesen jetzt dieselbe Menge:** Der Verweis der Fehlerkachel führt in die Liste
  mit `status=FEHLER` ([`dashboard-frontend.md`](dashboard-frontend.md) §5.4), und die Liste liest
  live. Bis heute konnte die Kachel dort eine nachverarbeitete Nachricht mitzählen, die die Liste
  nicht mehr zeigte; bei angewandter Lesung nicht mehr. Die eine Grenze bleibt, wie sie war: `bis`
  ist im Dashboard ausschließend, in der Liste nicht (§6.3 dort).

---

## 5b. Der Prozessbaum — Teil B *(18.09.2026)*

*Auftrag „Fehler live, Teil B (Prozessbaum)", Stand 18.09.2026, Zweig `feat/fehler-live` — Teil A
liegt noch nicht in `main`. Schritt 10f.* Der Baum ruft denselben Baustein wie die Übersicht: **die
Einordnung `FEHLER` aus der Live-Lesung, alle anderen aus Rollup und Live-Rest** (E‑208, unverändert).
Das ist ein zweiter Verbraucher derselben vierten Ausnahme von L2 und **kein fünfter Fall**.

**Eingecheckt ist zuerst nur die Vorregistrierung der Messung M189, vor dem ersten Lauf.** Der Bau,
die Ergebnisse und die Tests folgen in diesem Abschnitt.

> *Nachgetragen am selben Tag:* Die Vorregistrierung steht als Commit `233bf86` vor dem ersten Lauf
> im Zweig. Bau, Ergebnis und Tests stehen jetzt in diesem Abschnitt; der Kasten der
> Vorregistrierung ist nicht angefasst.

### Der Aufruf und die Stelle der Lesung (E‑213)

`ProzessbaumService.baum` ruft `FehlerLiveService.ermittle(mandant, fenster.von(), fenster.bis())` —
**mit dem Fenster, das der Baum aus seinem einen `jetzt` bildet**, demselben, das in der Antwort
steht. Für die drei Paare ist es `Rollupzeitraum.fenster(jetzt)`, dieselben Grenzen wie in der
Übersicht (`ProzessbaumFehlerLiveDbIT` vergleicht beide Antworten, bevor es ihre Zahlen vergleicht);
für `FREI` das Fenster der Antwort mit dem **ausschließenden** Ende, also die angefragte letzte Stunde
plus eine Stunde. Der Live-Rest bekommt weiter dasselbe `jetzt` — ein Uhrenschlag für Fenster,
Live-Rest und Lesung.

**Die Lesung ist das letzte Statement eines Aufrufs**, nach Gerüst, Kennzahlen, Wasserstand und den
zwei Live-Lesungen. Die Stellen der Statements davor rücken nicht; anders als in der Übersicht hängt
hier kein Statement am Zustand der Lesung. `ProzessbaumStatementsTest.EinAufruf` benennt jede Lage
einzeln, als Folge von Namen:

| Lage | Statements | Folge |
|---|---:|---|
| Live-Rest angewandt — Paar oder `FREI` | **6** | Gerüst · Kennzahlen · Wasserstand · Live-Rest A · Live-Rest B · **Fehlerlesung** |
| Live-Rest nicht nötig oder ausgesetzt | **4** | Gerüst · Kennzahlen · Wasserstand · **Fehlerlesung** |

**Der Text der Lesung ist der der Übersicht** — dasselbe `FehlerLiveRepository`, für jedes Paar und das
freie Fenster; gepinnt bleibt er in `DashboardStatementsTest`, `ProzessbaumStatementsTest` hält den
Baum dagegen. `Message` steht damit in einem angewandten Aufruf in **zwei** Statements (Live-Rest B,
ab G; die Lesung, im Fenster), sonst in einem.

### Der Ersatz — nach dem Live-Rest, vor den Kennzahlen (E‑213)

`ProzessbaumService.kennzahlenJeProzess` rechnet seither in drei Schritten:

1. **Die Zeilen je (Prozess, Rohstatus):** die Zeilen der Kennzahlenabfrage plus die Korrekturzeilen
   des Live-Rests, deren Stunde im Fenster liegt — die Verrechnung nach E‑188, unverändert.
2. **Der Ersatz** (`FehlerLiveErsatz.ersetze`): Alle Zeilen, die der Klassifizierer als `FEHLER`
   einordnet, fallen heraus; die Zeilen der Lesung kommen hinzu — vorher **je (Prozess, Rohstatus)
   über die Stunden des Fensters summiert**, mit derselben Fensterprüfung wie die Korrektur (`von`
   einschließend, `bis` ausschließend; die Lesung liest ohnehin nur das Fenster).
3. **Je Prozess die zwei Zahlen**, über den Klassifizierer, und **die Klemme auf null — wo sie war**:
   je Prozess und je Zahl einzeln.

**Warum nach dem Live-Rest:** Die Korrektur trägt ihre eigenen Fehlerzeilen; stünde der Ersatz davor,
kämen sie hinterher wieder hinein, und ein Fehler der laufenden Stunde zählte zweimal — die Begründung
von E‑210, im Baum belegt durch `fehler_im_live_bereich_zaehlt_einmal` und die Gegenprobe (§9).
**Warum vor den Kennzahlen:** Nur die Zeilen tragen den Rohstatus; nach der Verdichtung gibt es nichts
mehr zu ersetzen.

**Die Verrechnung des Live-Rests selbst ist unverändert** — dieselben Zeilen, dasselbe Vorzeichen,
dieselbe Fensterprüfung. Neu ist nur die Zwischengestalt je (Prozess, Rohstatus), die der Auftrag für
den Ersatz verlangt; weil Summen sich nicht nach der Reihenfolge richten, ergibt sie bei ausgesetzter
Lesung jede Zahl von vorher. Belegt: Die neun Diensttests des Live-Rests laufen ohne Änderung grün.

> **Eine Folge der Klemme, die es vor Teil B schon gab, hier benannt:** Geklemmt wird je Zahl. Fällt
> die Summe eines Prozesses durch eine negative Korrektur unter null, zeigt er null *Nachrichten* —
> und kann trotzdem *Fehler* tragen, jetzt aus der Lesung (`die_klemme`). Vor Teil B entstand dasselbe
> mit einer Fehlerzeile des Rollups. Die negative Korrektur einer **Fehlerzeile** dagegen fällt mit
> dem Ersatz heraus.

### Was unverändert bleibt

Gerüst; das Kennzahlenstatement **Zeichen für Zeichen** (E‑42) — die drei Paare, der Bösfall und die
Eigenschaften in `ProzessbaumStatementsTest` sind nicht angefasst; die letzte Bewegung (E‑34, E‑35)
samt Zuständen und Schwelle — die Lesung berührt sie nicht (`letzte_bewegung_und_zustand_unveraendert`);
beide Gliederungen; die Verrechnung des Live-Rests; die Übertragungsliste. **An keiner dieser Stellen
hat der Code eine Änderung verlangt.**

### Ausfall und Antwort

**Der Ausfall** läuft über `FehlerLiveService` wie in Teil A: Jede `DataAccessException` ergibt
`AUSGESETZT` und ein `WARN`, und der Baum rechnet wie vorher — `FehlerLiveErsatz` gibt die Zeilen im
Zustand `AUSGESETZT` unverändert zurück.

**Die Antwort** trägt den Block neben `liveRest`, **in allen Modi und beiden Gliederungen** — dasselbe
Record wie die Übersicht (`common/FehlerLiveResponse`):

```jsonc
"liveRest": { "zustand": "NICHT_NOETIG", "vollstaendigBis": null },
"fehlerLive": { "zustand": "ANGEWANDT" },            // oder "AUSGESETZT"
```

Kein neuer Endpunkt, kein neuer Parameter, keine neue Mandantenausnahme.

### Die Oberfläche (E‑214)

`components/fehler-live-hinweis.tsx` steht im Baum **am Ort des Hinweises zum Live-Rest**: im
klebenden Kopf der Baumspalte, unter den Kopfzahlen, und **unter** dem Hinweis zum Live-Rest, wenn
beide stehen. Bei `ANGEWANDT` steht nichts. Der Baustein ist der aus Teil A, nur eingesetzt; **keine
neuen Texte** (`texte.fehlerLive` gilt für beide Ansichten), kein Nachladen (E‑164, E‑186). Der Typ
der Baumantwort (`Prozessbaum` in `features/nachrichten/api.ts`) trägt das Feld `fehlerLive`, der Typ
selbst liegt in `lib/fehler-live.ts`. `components/ui` ist nicht angefasst; der shadcn-Skill war nicht
nötig, weil `Alert` eingebunden bleibt wie beim Live-Rest.

**Lokal erscheint der Hinweis nicht** — die Lesung ist auf der Testkopie angewandt. Belegt sind Ort,
Reihenfolge und Abwesenheit in `tests/fehler-live.test.tsx` (§9); eine Sichtprüfung mit gestellter
Antwort steht aus, wie für Teil A.

### Weitere Leser der Einordnung `FEHLER` aus den Rolluptabellen

Gesucht am 18.09.2026 im Hauptcode des Backends: jede Klasse, die `message_rollup`,
`message_rollup_tag` oder `message_rollup_monat` liest, und jeder Aufruf von
`MessageStatusClassifier.bedingung`, `fehlerBedingung` oder `einordnung`. Die Oberfläche liest keine
Rolluptabelle.

| Leser | Liest er die Einordnung `FEHLER` aus dem Rollup? | Stand |
|---|---|---|
| Übersicht — Verlauf, Kacheln, Verteilung (`DashboardRepository`) | ja | seit Teil A ersetzt (§5) |
| Prozessbaum — Kennzahlen (`ProzessbaumRepository.kennzahlen`) | ja | seit Teil B ersetzt |
| Live-Rest A (`LiveRestRepository.ausDemRollup`) | liest die Rollupzeilen des Live-Bereichs samt Fehlerzeilen, für beide Verbraucher | kein eigener Leser — die Fehlerzeilen fallen in beiden mit dem Ersatz |
| Belegungsprobe der Übersicht (`DashboardRepository.belegung`) | nein — Summe aller Status je Eimer, ordnet nicht ein | kein Fund |
| Letzte Bewegung im Gerüst | nein — die jüngste Stunde je Prozess, ohne Status | kein Fund |
| **Fensterverengung der Nachrichtenliste** (`message/VerengungRepository.frageStufe`) | **ja** — bei `status=FEHLER` zählt sie über `MessageStatusClassifier.bedingung(…)` auf `message_rollup.message_status` die Fehler je Stunde, um das Fenster der Liste vorab zu verengen | **Fund, nicht umgebaut — Punkt 216** |
| Rollup-Job (`rollup/*`) | nein — er schreibt den Rollup und liest `Message` | kein Leser |

Alle übrigen Aufrufe des Klassifizierers (Liste, Detail, Ketten, BAM-Suche) lesen `Message` live.

**Was der Fund bedeutet — hergeleitet aus dem Code, nicht gemessen:** Die Verengung ruht auf einer
Eigenschaft (`Verengungsgrenzen`): *Die gezählte Menge ist stets eine Unterschranke der wirklichen.*
Ein Abgang aus einem alten Eimer bricht sie — eine nachverarbeitete Nachricht steht im Rollup bis
03:00 weiter als Fehler, die Liste liest sie live nicht mehr. Zählt der Rollup mehr Fehler, als es
gibt, kann die Verengung das Fenster enger ziehen, als sie dürfte; die verengte Abfrage liefert dann
weniger als `limit + 1` Zeilen, die Seite meldet keine weitere (`Seite.aus`), und ältere Fehler des
Fensters erscheinen bis zum Volllauf nicht. Nach Punkt 210 gilt dasselbe für jeden Statusfilter. Auf
der Testkopie ist der Fall nicht herstellbar (kein Abgang); der Auftrag verlangt melden, nicht
umbauen.

### Die Messung — M189 (Regeln L7, L15)

> #### Vorregistriert — eingetragen und eingecheckt vor dem ersten Lauf
>
> **Rahmen, für alle vier Tore.** Gegen die Testkopie, Profil `dev`. **Sequenziell, nie parallel** —
> kein zweiter Lauf gegen die Testkopie zur selben Zeit. Je Lage **ein Aufwärmlauf, dann die beste
> von fünf**.
>
> - **In SQL (Tor 1):** `information_schema.PROFILING` **und** die Wanduhr des Servers (`SYSDATE(6)`
>   vor und nach dem Statement, E‑107), Eichung um `SELECT 1`; `EXPLAIN` (Regel L15); die
>   Handler-Zähler aus `information_schema.SESSION_STATUS` für einen Lauf (gelesene Indexsätze,
>   gemessen statt geschätzt), ebenfalls um `SELECT 1` geeicht. **Der Text der Lesung ist aus dem Code
>   gerendert** (`StatementType.STATIC_STATEMENT`, Wegwerf-Programm gegen eine jOOQ-Attrappe, am
>   18.09.2026 vor dieser Vorregistrierung) und zeichengleich mit dem Text von M188; eingesetzt sind
>   nur Fenster und Mandant. Die Sitzungen erzeugt `scripts/messung-fehler-live/erzeuge_m189.py`.
> - **Am Endpunkt (Tore 2 bis 4):** `MessungM189DbIT`, die Bauform von M185 und M188. Je Lage **durch
>   den Endpunkt** (`GET /api/prozesse/baum?…&gliederung=PARTNER`, HTTP-Umlauf im Testclient samt
>   Sitzung und Serialisierung), **am Dienst** (`ProzessbaumService.baum`, Gliederung `PARTNER`) und
>   als **Bezug in derselben Sitzung**: derselbe Baum am Dienst mit ausgesetzter Lesung — dieselben
>   Bausteine, dieselbe Uhr, derselbe Live-Rest, nur `FehlerLiveService` liefert `AUSGESETZT`, ohne
>   zu lesen. Das ist der Baum, wie er vor Teil B war. **Zuschlag** = Dienst minus Bezug. Uhr und
>   Wasserstand über `@TestBean` gestellt wie in M185; kein Schreibzugriff auf `GlassfishDB` oder
>   `rollup_lauf` (S1, T2). Zeiten nach `System.out` und in keine Zusicherung (T1). Ausgegeben wird
>   je Lage auch die Zahl der Fehler im Fenster (die Kopfzahl `gesamt.fehler`).
>
> #### Tor 1 — die Lesung in SQL, das freie Fenster mit den meisten Fehlern (vor dem Einbau)
>
> **Die Lage wird per Zählung bestimmt, nach einer vorab festgelegten Regel:** unter allen freien
> Fenstern von **genau einem Kalenderjahr** `[von, von + 1 Jahr)` — der größten Breite, die
> `Baumfenster.ausAnfrage` zulässt —, deren `von` eine volle Stunde ist, in der `NEXANS` mindestens
> einen Fehler trägt, das Fenster **mit den meisten Fehlern** (Fehlerbedingung, Mandantenkette,
> `MessageLastUpdate` im Fenster); bei Gleichstand das früheste. *Warum die Fehlerstunden als Anfang
> genügen:* Beginnt ein Fenster in einer Stunde ohne Fehler, trägt das Fenster ab seiner ersten
> Fehlerstunde mindestens ebenso viele. Die Zählung ist die erste Abfrage der Sitzung und steht im
> Protokoll; sie gibt Stunden und Zahlen aus, keine Kennungen (G1).
>
> **Erwartet:**
>
> | | |
> |---|---|
> | **Plan** | `Message` über **`MessageStatusIDX`** (`range`), die Kette per Primärschlüssel — wie in allen zwölf Lagen von M188 |
> | **Gelesen** | **3.412** Indexsätze (`read_next`) — die Fehlerzeilen des ganzen Bestands, dieselbe Zahl wie in M188, unabhängig vom Fenster |
> | **Laufzeit** | **Sie wächst mit den Fehlern im Fenster** (M188: 711 statt 50 Fehler kosten rund 10 ms mehr; die Lage mit 2.163 Fehlern ist dort nicht einzeln gemessen). *Gerechnet, nicht gemessen, nur für die Größenordnung:* die Gerade durch die zwei `NEXANS`-Lagen von M188 (Profil 21,6 ms bei 50, 31,5 ms bei 711 Fehlern), also rund 1,5 ms je 100 Fehler — `21,6 + 0,015 × (N − 50)` ms bei N Fehlern im Fenster. Das Fenster trägt mindestens 2.163 Fehler (das Zwölfmonatsfenster bis `2024-11-01` aus M188 ist ein Jahresfenster) und höchstens 3.301 (alle von `NEXANS`): nach der Geraden **53 bis 70 ms** |
>
> **Grenze:** In **keiner** Planzeile ein Zeitindex als `key` — weder `MessageLastUpdateIDX` noch
> `MessageLastUpdateProcessMessageIDX`, auch nicht als Rowid-Filter. **Höchstens 100 ms** — in Profil
> und Wanduhr.
>
> *Zusätzlich zum Auftrag, in derselben Sitzung:* dieselbe Lesung für **jedes Fenster der Tore 2 bis
> 4** (die Tabellen unten; das `SUTTONS`-Fenster `2024-07-01` bis `2025-07-01` steht in Tor 2 und
> Tor 3 und wird einmal gemessen) — `EXPLAIN`, Handler-Zähler, beste von fünf. Erwartet: derselbe
> Plan, dieselbe Zahl gelesener Indexsätze (3.412 bei `NEXANS`, 3.413 bei `SUTTONS`, wie in M188).
> Grenze dieselbe: kein Zeitindex, 100 ms.
>
> #### Tor 2 — der Baum durch den Endpunkt, typische Stunde aus M185
>
> **Lagen:** die typische Stunde aus M185 ([`live-rest.md`](live-rest.md) §8), `jetzt` = G + 1 h 30,
> Wasserstand G + 1 h → Live-Rest `ANGEWANDT` über zwei Eimer; je `48H` und `12M`. Die Fenster aus
> `Rollupzeitraum.fenster(jetzt)`, vom Code ausgegeben und nicht von Hand gerechnet (Wanduhrzeit der
> Quelle, `bis` ausschließend):
>
> | Mandant | G | `jetzt` | Live-Bereich | `48H` | `12M` |
> |---|---|---|---|---|---|
> | `NEXANS` | `2025-11-09 15:00` | `16:30` | `15:00`–`17:00` | `2025-11-07 17:00` bis `2025-11-09 17:00` | `2024-12-01` bis `2025-12-01` |
> | `SUTTONS` | `2025-06-09 19:00` | `20:30` | `19:00`–`21:00` | `2025-06-07 21:00` bis `2025-06-09 21:00` | `2024-07-01` bis `2025-07-01` |
>
> **Erwartet (Auftrag):** der Bezug plus **20 bis 35 ms** — die Lesung (M188 am Code: 21,5 bis 34,5 ms);
> bei `NEXANS` `12M` durch den Endpunkt **rund 176 ms** (141,6 aus M185 plus 34,5 aus M188). Für alle
> vier Lagen gerechnet, M185 durch den Endpunkt plus 20 bis 35 ms:
>
> | Lage | M185, Endpunkt | **erwartet, Endpunkt** |
> |---|---:|---:|
> | `NEXANS` `48H` | 83,862 | **103,9–118,9 ms** |
> | `NEXANS` `12M` | 141,574 | **161,6–176,6 ms** |
> | `SUTTONS` `48H` | 42,156 | **62,2–77,2 ms** |
> | `SUTTONS` `12M` | 62,830 | **82,8–97,8 ms** |
>
> **Grenze: höchstens 200 ms je Lage** durch den Endpunkt. *Festgelegt vom Auftraggeber in diesem
> Auftrag:* Die Schranke der typischen Stunde war **150 ms** (M152, M185); mit der Lesung wird sie
> nach Rechnung überschritten (141,6 + 34,5 = 176,1 ms). Die neue Schranke ist die alte plus die
> teuerste Lesung aus M188 (34,5 ms am Code), aufgerundet.
>
> #### Tor 3 — dasselbe, dichtester Vierstundenbereich aus M185
>
> **Lagen:** der dichteste Vierstundenbereich aus M185, `jetzt` = G + 3 h, Wasserstand G + 1 h →
> `ANGEWANDT` über vier Eimer; je `48H` und `12M`:
>
> | Mandant | G | `jetzt` | Live-Bereich | `48H` | `12M` |
> |---|---|---|---|---|---|
> | `NEXANS` | `2024-10-09 18:00` | `21:00` | `18:00`–`22:00` | `2024-10-07 22:00` bis `2024-10-09 22:00` | `2023-11-01` bis `2024-11-01` |
> | `SUTTONS` | `2025-06-12 07:00` | `10:00` | `07:00`–`11:00` | `2025-06-10 11:00` bis `2025-06-12 11:00` | `2024-07-01` bis `2025-07-01` |
>
> **Erwartet (Auftrag):** der Bezug plus **20 bis 35 ms**. Gerechnet, M185 plus 20 bis 35 ms:
>
> | Lage | M185, Endpunkt | **erwartet, Endpunkt** |
> |---|---:|---:|
> | `NEXANS` `48H` | 336,990 | **357,0–372,0 ms** |
> | `NEXANS` `12M` | 324,843 | **344,8–359,8 ms** |
> | `SUTTONS` `48H` | 69,116 | **89,1–104,1 ms** |
> | `SUTTONS` `12M` | 75,473 | **95,5–110,5 ms** |
>
> **Ein Vorbehalt, aus denselben Zahlen gerechnet:** `NEXANS` `12M` liest hier das Fenster bis
> `2024-11-01` mit **2.163 Fehlern** (M188, Zusatzlage von Tor 3). Nach der Geraden aus Tor 1 kostet
> die Lesung dort rund 53 ms in SQL; der Zuschlag der Übersicht lag an dieser Lage bei 45,9 bis
> 47,3 ms. **Dort wird der Zuschlag vermutlich über dem Band liegen** — rund 45 bis 55 ms. Das Band
> bleibt die vorregistrierte Erwartung; die Rechnung steht daneben und ist keine Ausrede.
>
> **Grenze:** höchstens **500 ms** je Lage durch den Endpunkt.
>
> #### Tor 4 — der Baum durch den Endpunkt, `FREI`
>
> **Lagen:** `NEXANS` und `SUTTONS`, je die zwei freien Fenster aus M152 ([`process-view.md`](process-view.md)
> §42), in der Anfrage in UTC wie dort:
>
> | Fenster | Wanduhrzeit, `bis` ausschließend | Segmente | Anfrage |
> |---|---|---|---|
> | **Jahr monatsbündig** | `2025-01-01 00:00` bis `2026-01-01 00:00` | eines, Monat | `von=2024-12-31T23:00:00Z&bis=2025-12-31T22:00:00Z` |
> | **Bösfall** | `2024-12-30 14:00` bis `2025-12-30 03:00` | fünf, über alle drei Ebenen | `von=2024-12-30T13:00:00Z&bis=2025-12-30T01:00:00Z` |
>
> **Uhr und Wasserstand, festgelegt vor dem Lauf:** der Anker `2025-12-30 04:09:47`, Wasserstand
> `2025-12-30 04:00` → G = `03:00`, Live-Rest `ANGEWANDT` über zwei Eimer wie in der typischen
> Stunde. Die Uhr am Anker hält die Fenster von M152 gültig (alle enden auf oder vor ihm); das Jahr
> enthält den Live-Bereich, der Bösfall endet genau an G — dort laufen die Live-Lesungen und
> verrechnen nichts.
>
> **Erwartet (Auftrag):** der Bezug plus **20 bis 35 ms**. Gerechnet, durch den Endpunkt: M152 plus rund
> 16 bis 20 ms für Wasserstand und Live-Rest (M185 gegen M152, typische Stunde von `NEXANS`) plus 20
> bis 35 ms:
>
> | Lage | M152, Endpunkt | **erwartet, Endpunkt** |
> |---|---:|---:|
> | `NEXANS` Jahr | 120,554 | **156,6–175,6 ms** |
> | `NEXANS` Bösfall | 111,784 | **147,8–166,8 ms** |
> | `SUTTONS` Jahr | 56,056 | **92,1–111,1 ms** |
> | `SUTTONS` Bösfall | 52,954 | **89,0–108,0 ms** |
>
> **Grenze: höchstens 200 ms je Lage** durch den Endpunkt.
>
> #### Wie gelesen wird — festgelegt vor dem Lauf
>
> - **Liegt eine Lage über ihrer Grenze, oder steigt die Lesung über einen Zeitindex ein: anhalten
>   und berichten, nicht nachjustieren.** Ein Indexhinweis wäre eine Entscheidung und keine Korrektur.
> - Der **Zuschlag** trägt die Aussage über den Bau; der **Endpunkt** trägt die Tore 2 bis 4; die
>   **Rechnungen** tragen nur die Größenordnung. Abweichungen werden benannt und nicht umgedeutet;
>   sie stehen neben diesem Kasten, nicht in ihm.
> - **Zugesichert wird im Läufer nur**, dass jede Antwort `200` ist, dass `fehlerLive` und `liveRest`
>   `ANGEWANDT` sind, dass der Bezug `AUSGESETZT` trägt und dass Endpunkt und Dienst dieselbe
>   Kopfzahl tragen und jedes Blatt einmal steht — sonst mäße er die Laufzeit eines falschen Baums.
>
> #### Die Dev-Zeile — die Eichung, kein Tor
>
> Auf der Testkopie ist ein Abgang nicht herstellbar. **Keine Zahl des Baums darf sich ändern**, Feld
> für Feld bis auf `fehlerLive` — für `NEXANS`, `SUTTONS` und `VOTG` in den drei Paaren und einem
> freien Fenster, geprüft in `ProzessbaumFehlerLiveDbIT` und nicht im Läufer. Die Kopfzahl *Fehler*
> ist je Paar die Kachel der Übersicht: `NEXANS` **50 / 55 / 711**, `SUTTONS` **0 / 5 / 103**, `VOTG`
> **0 / 0 / 8** (M188). Weicht eine Zahl ab: zuerst den Wasserstand prüfen, dann melden.

#### Ergebnis — 18.09.2026, gegen die Vorregistrierung

**Alle vier Tore halten.** Die Erwartungen treffen nicht überall zu; jede Abweichung steht bei ihrem
Tor, keine ist umgedeutet. Sequenziell gefahren, kein Lauf parallel zu einem anderen gegen die
Testkopie: Sitzung 3 (die Zählung) und Sitzung 4 (Tor 1 und die Fenster der Tore 2 bis 4) ab 12:45
Serverzeit, **vor dem Einbau**; danach der Bau und die Tests mit Datenbank; dann die Tore 2 bis 4 am
Endpunkt in **zwei Läufen** (L1, L2) nacheinander — das lokale Dev-Backend war währenddessen
gestoppt, damit kein zweiter Prozess gegen die Testkopie lief.

**Wo die Zahlen stehen:** `scripts/messung-fehler-live/` — `erzeuge_m189.py`, die Sitzungen
`s3-m189-zaehlung.sql` und `s4-m189-tor1-lesung.sql`, die gefilterten Protokolle
`ergebnis/s3.gefiltert.txt` und `s4.gefiltert.txt` (Marken, Zählung, Pläne, Zähler, Laufzeiten) und
die Laufzeilen `ergebnis/m189-lauf1.txt` und `m189-lauf2.txt`. **Die Rohausgaben der Sitzungen sind
nicht eingecheckt** (G1): Die Lesung gibt `ProcessID` aus. Geeicht: dieselbe Suche nach
Kennungsmustern findet in der Rohausgabe von Sitzung 4 2.149 Treffer, in den eingecheckten Dateien
keinen; Sitzung 3 gibt keine Kennung aus. `filtere.py` lässt dafür eine Tabelle mehr durch, die der
Zählung (Kopf `| von `, nur Stunden und Zahlen). MariaDB 10.6.22, `read_only = 1`. Eichung der
Wanduhr um `SELECT 1`: 1,021 bis 1,227 ms; die Handler-Zähler um `SELECT 1`: 9 `read_rnd_next`,
sonst null.

##### Sitzung 3 — das Jahresfenster, nach der Regel

| `von` | `bis`, ausschließend | Fehler im Fenster | Stunden mit Fehlern im Fenster |
|---|---|---:|---:|
| **`2024-10-04 11:00`** | **`2025-10-04 11:00`** | **3.204** | 42 |
| `2024-10-04 12:00` | `2025-10-04 12:00` | 3.203 | 41 |
| `2024-10-04 14:00` | `2025-10-04 14:00` | 3.190 | 40 |
| `2024-10-08 12:00` | `2025-10-08 12:00` | 3.178 | 39 |
| `2024-10-09 20:00` | `2025-10-09 20:00` | 3.165 | 38 |

Nach der Regel ist es das Fenster **`2024-10-04 11:00` bis `2025-10-04 11:00`**, 3.204 der 3.301
Fehler von `NEXANS`. **Die Zählung ist in derselben Sitzung geeicht**, an zwei Zahlen aus M188: alle
Fehler von `NEXANS` 3.301 (M188: 3.301), im Zwölfmonatsfenster bis `2024-11-01` 2.163 (M188:
2.163); 58 Stunden tragen überhaupt einen Fehler. Sitzung 4 wiederholt die Zählung als erste Abfrage,
mit demselben Ergebnis.

##### Tor 1 — die Lesung in SQL: gehalten

Beste von fünf nach einem Aufwärmlauf, in Millisekunden; dazu — *zusätzlich zum Auftrag,
vorregistriert* — dieselbe Lesung für jedes Fenster der Tore 2 bis 4. Die letzte Spalte ist die
vorregistrierte Gerade `21,6 + 0,015 × (N − 50)`, gerechnet für `NEXANS`:

| Mandant | Lage | Fenster | Fehler im Fenster | Zeilen der Lesung | gelesen (`read_next`) | `read_key` | Profil | Wanduhr | Gerade aus M188 |
|---|---|---|---:|---:|---:|---:|---:|---:|---:|
| `NEXANS` | **Tor 1:** das Jahresfenster | `2024-10-04 11:00` bis `2025-10-04 11:00` | 3.204 | 42 | 3.412 | 3.210 | **69,756** | 71,017 | 68,9 |
| `NEXANS` | Tor 2, typisch `48H` | `2025-11-07 17:00` bis `2025-11-09 17:00` | 0 | 0 | 3.412 | 2 | **20,495** | 21,368 | 20,9 |
| `NEXANS` | Tor 2, typisch `12M` | `2024-12-01` bis `2025-12-01` | 656 | 29 | 3.412 | 694 | **29,458** | 30,704 | 30,7 |
| `NEXANS` | Tor 3, dicht `48H` | `2024-10-07 22:00` bis `2024-10-09 22:00` | 15 | 2 | 3.412 | 19 | **19,962** | 21,170 | 21,1 |
| `NEXANS` | Tor 3, dicht `12M` | `2023-11-01` bis `2024-11-01` | 2.163 | 13 | 3.412 | 2.167 | **39,894** | 41,262 | 53,3 |
| `NEXANS` | Tor 4, Jahr | `2025-01-01` bis `2026-01-01` | 711 | 35 | 3.412 | 762 | **29,790** | 30,970 | 31,5 |
| `NEXANS` | Tor 4, Bösfall | `2024-12-30 14:00` bis `2025-12-30 03:00` | 662 | 34 | 3.412 | 702 | **29,794** | 30,988 | 30,8 |
| `SUTTONS` | Tor 2, typisch `48H` | `2025-06-07 21:00` bis `2025-06-09 21:00` | 1 | 1 | 3.413 | 5 | **19,380** | 20,385 | — |
| `SUTTONS` | Tor 2 und 3, `12M` | `2024-07-01` bis `2025-07-01` | 37 | 28 | 3.413 | 42 | **24,105** | 25,170 | — |
| `SUTTONS` | Tor 3, dicht `48H` | `2025-06-10 11:00` bis `2025-06-12 11:00` | 1 | 1 | 3.413 | 5 | **19,458** | 20,534 | — |
| `SUTTONS` | Tor 4, Jahr | `2025-01-01` bis `2026-01-01` | 103 | 61 | 3.413 | 131 | **22,929** | 24,044 | — |
| `SUTTONS` | Tor 4, Bösfall | `2024-12-30 14:00` bis `2025-12-30 03:00` | 103 | 61 | 3.413 | 125 | **23,095** | 24,421 | — |

**Die Pläne:** In allen zwölf Fenstern liest `Message` über **`MessageStatusIDX`** (`range`,
`key_len` 123, 6.257 geschätzte Zeilen). Bei `NEXANS` (sieben Pläne) treibt `Message`, und die Kette
hängt per `eq_ref` über `PRIMARY` an; bei `SUTTONS` (fünf) beginnt der Plan bei `ProjectMandant` über
`ProjectMandant_Mandant_idx` und liest `Message` mit Join-Puffer — die Beobachtung aus M146 und M188.
**Von 36 Planzeilen trägt keine einen Zeitindex als `key`**, keine einen Rowid-Filter, keine ist ein
Durchlauf. Das Jahresfenster, gekürzt um `possible_keys`:

```
| id | select_type | table          | type   | key              | key_len | ref                                        | rows | Extra
|  1 | PRIMARY     | Message        | range  | MessageStatusIDX | 123     | NULL                                       | 6257 | Using index condition; Using where; Using temporary; Using filesort
|  1 | PRIMARY     | fehler_process | eq_ref | PRIMARY          | 146     | GlassfishDB.Message.ProcessID              | 1    | Using where
|  1 | PRIMARY     | ProjectMandant | eq_ref | PRIMARY          | 292     | GlassfishDB.fehler_process.ProjectID,const | 1    | Using where; Using index
```

| Vorregistriert | Gemessen | trifft zu? |
|---|---|---|
| Einstieg über `MessageStatusIDX` | im Jahresfenster und in allen elf Fenstern der Tore 2 bis 4 | **ja** |
| 3.412 gelesene Indexsätze | 3.412 bei `NEXANS`, 3.413 bei `SUTTONS` — in jedem Fenster dieselben | **ja** |
| Die Laufzeit wächst mit den Fehlern im Fenster; nach der Geraden 53 bis 70 ms | **69,756 ms** im Profil bei 3.204 Fehlern — die Gerade sagte 68,9 | **ja**, für diese Lage |
| **Grenze:** kein Zeitindex, höchstens 100 ms | kein Zeitindex; 69,8 ms im Profil, 71,0 ms an der Wanduhr | **gehalten** |

**Die Abweichung, benannt: Die Gerade trifft die Lage von Tor 1 und nicht jede.** Bei null bis 711
Fehlern liegt sie höchstens 1,7 ms neben der Messung, bei 3.204 Fehlern 0,8 ms — bei 2.163 Fehlern
(`NEXANS`, bis `2024-11-01`) aber 13,4 ms darüber: 53,3 gerechnet, 39,9 gemessen. Die Zeit wächst mit
den Fehlern im Fenster, aber nicht mit ihrer Zahl allein. Gemessen ist, was mitwächst: `read_key` —
die Nachschläge der Kette — steigt mit den Zeilen im Fenster (2.167 und 3.210), die Zeilen der Lesung
mit den Gruppen (13 und 42). **Wohin die Zeit geht, ist nicht gemessen.** Für die Frage des Tors
genügt es: Die teuerste Lage, die das freie Fenster auf der Testkopie zulässt, kostet 69,8 ms.

> **Belegvermerk (Regel L10).** *Gemessen ist:* das Jahresfenster mit den meisten Fehlern und elf
> weitere Fenster, je `EXPLAIN`, ein Lauf zwischen den Handler-Zählern, ein Aufwärmlauf und die beste
> von fünf in Profil und Wanduhr; warm, gegen die Testkopie. *Behauptet wird:* Die Lesung steigt in
> jedem gemessenen Fenster über den Statusindex ein, liest die Fehlerzeilen des Bestands und kostet
> auf der Testkopie höchstens 69,8 ms. **Die Lücke:** 3.412 Fehlerzeilen sind der Bestand der
> Testkopie; wie viele es in der Produktion sind und wie viele in einem Jahresfenster liegen, ist
> nicht erhoben (Punkt 212). Kein Fall ist kalt gemessen.

##### Tore 2 bis 4 — der Baum durch den Endpunkt: gehalten

Beste von fünf nach einem Aufwärmlauf, in Millisekunden, **zwei Läufe** (L1, L2) nacheinander; keiner
ist ausgewählt. **Endpunkt** ist `GET /api/prozesse/baum?…&gliederung=PARTNER` im Testclient samt
Sitzung und Serialisierung, **Dienst** `ProzessbaumService.baum`, **Bezug** derselbe Baum am Dienst
mit ausgesetzter Lesung — der Baum von vor Teil B —, **Zuschlag** Dienst minus Bezug. *im Band* misst
den Endpunkt gegen die Rechnung der Vorregistrierung; *Fehler im Fenster* ist die Kopfzahl
`gesamt.fehler` und dieselbe Zahl wie in SQL.

| Tor | Mandant · Lage | Fenster | erwartet, Endpunkt | **Endpunkt** L1 / L2 | im Band | Dienst L1 / L2 | Bezug L1 / L2 | **Zuschlag** L1 / L2 | Fehler im Fenster | Nachrichten |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|
| 2 | `NEXANS` typisch `48H` | `2025-11-07 17:00` bis `2025-11-09 17:00` | 103,9–118,9 | **87,178 / 91,338** | darunter / darunter | 58,979 / 61,549 | 41,808 / 44,142 | **17,171 / 17,408** | 0 | 16.464 |
| 2 | `NEXANS` typisch `12M` | `2024-12-01 00:00` bis `2025-12-01 00:00` | 161,6–176,6 | **159,947 / 167,246** | darunter / ja | 137,948 / 138,989 | 107,559 / 110,534 | **30,388 / 28,455** | 656 | 2.282.520 |
| 2 | `SUTTONS` typisch `48H` | `2025-06-07 21:00` bis `2025-06-09 21:00` | 62,2–77,2 | **63,528 / 58,883** | ja / darunter | 48,542 / 44,804 | 24,722 / 25,314 | **23,820 / 19,490** | 1 | 1.360 |
| 2 | `SUTTONS` typisch `12M` | `2024-07-01 00:00` bis `2025-07-01 00:00` | 82,8–97,8 | **90,043 / 83,731** | ja / ja | 75,432 / 71,549 | 45,890 / 46,242 | **29,543 / 25,307** | 37 | 67.686 |
| 3 | `NEXANS` dicht `48H` | `2024-10-07 22:00` bis `2024-10-09 22:00` | 357,0–372,0 | **336,264 / 345,357** | darunter / darunter | 315,591 / 343,978 | 298,643 / 301,782 | **16,948 / 42,196** | 15 | 33.148 |
| 3 | `NEXANS` dicht `12M` | `2023-11-01 00:00` bis `2024-11-01 00:00` | 344,8–359,8 | **349,339 / 346,653** | ja / ja | 328,724 / 348,388 | 289,942 / 305,669 | **38,782 / 42,719** | 2.163 | 222.049 |
| 3 | `SUTTONS` dicht `48H` | `2025-06-10 11:00` bis `2025-06-12 11:00` | 89,1–104,1 | **90,577 / 85,685** | ja / darunter | 76,442 / 72,705 | 52,475 / 49,500 | **23,967 / 23,205** | 1 | 1.410 |
| 3 | `SUTTONS` dicht `12M` | `2024-07-01 00:00` bis `2025-07-01 00:00` | 95,5–110,5 | **100,468 / 95,099** | ja / darunter | 86,626 / 83,961 | 59,422 / 57,397 | **27,205 / 26,564** | 37 | 67.686 |
| 4 | `NEXANS` `FREI` Jahr | `2025-01-01 00:00` bis `2026-01-01 00:00` | 156,6–175,6 | **161,241 / 158,189** | ja / ja | 141,746 / 138,767 | 106,969 / 109,651 | **34,777 / 29,116** | 711 | 2.308.005 |
| 4 | `NEXANS` `FREI` Bösfall | `2024-12-30 14:00` bis `2025-12-30 03:00` | 147,8–166,8 | **149,280 / 151,351** | ja / ja | 131,994 / 131,398 | 101,653 / 102,393 | **30,340 / 29,005** | 662 | 2.309.634 |
| 4 | `SUTTONS` `FREI` Jahr | `2025-01-01 00:00` bis `2026-01-01 00:00` | 92,1–111,1 | **86,989 / 87,124** | darunter / darunter | 73,324 / 73,626 | 49,065 / 49,807 | **24,259 / 23,819** | 103 | 196.536 |
| 4 | `SUTTONS` `FREI` Bösfall | `2024-12-30 14:00` bis `2025-12-30 03:00` | 89,0–108,0 | **80,172 / 79,679** | darunter / darunter | 67,972 / 67,559 | 43,073 / 43,465 | **24,899 / 24,094** | 103 | 196.508 |

**Die Tore: gehalten.** Die teuerste Lage je Tor, über beide Läufe: **Tor 2** `NEXANS` `12M` mit
**167,246 ms** (L2), 32,8 ms unter der Grenze von 200; **Tor 3** `NEXANS` dicht `12M` mit
**349,339 ms** (L1), 150,7 ms unter 500; **Tor 4** `NEXANS` Jahr mit **161,241 ms** (L1), 38,8 ms
unter 200. In jeder Lage sind Fehler live und Live-Rest angewandt, und Endpunkt und Dienst tragen
dieselbe Kopfzahl. **Die alte Schranke von 150 ms hätten fünf Messungen gerissen** — `NEXANS` `12M`
typisch und `NEXANS` Jahr in beiden Läufen, der Bösfall in L2 mit 151,351 ms —, wie die Rechnung der
Vorregistrierung es angekündigt hatte.

**Die Rechnung am Endpunkt trifft in zwölf von 24 Messungen** (sieben in L1, fünf in L2); **die
übrigen zwölf liegen darunter, keine darüber.** Sie hatte M185 und M152 durch den Endpunkt als
Grundlage; die Seite ohne Lesung war an diesem Tag nicht teurer als dort — getrennt erhoben ist das
nicht, der Bezug steht am Dienst.

**Der Zuschlag liegt in 17 von 24 Messungen im Band** von 20 bis 35 ms (neun in L1, acht in L2).
Darunter liegen Fenster mit null bis 15 Fehlern: `NEXANS` `48H` typisch in beiden Läufen (17,2 und
17,4), `NEXANS` `48H` dicht in L1 (16,9), `SUTTONS` `48H` typisch in L2 (19,5). Darüber liegt
`NEXANS` dicht `12M` in beiden Läufen (38,8 und 42,7) — das Fenster mit 2.163 Fehlern, für das die
Vorregistrierung den Vorbehalt eingetragen hatte; die Richtung traf, ihre Größe (45 bis 55 ms) nicht —
und `NEXANS` `48H` dicht in L2 (42,2).

**Der Zuschlag ist die Lesung.** Gegen die Lesung desselben Fensters in SQL (Tor 1, Zusatzfenster)
liegt er in 22 von 24 Messungen höchstens 5 ms daneben, in beide Richtungen. Die zwei übrigen:
`SUTTONS` typisch `12M` in L1 mit 5,4 ms darüber, und `NEXANS` dicht `48H` in L2 mit 22,2 ms darüber.
**Im dichtesten Bereich trägt der Zuschlag nichts:** Dort kostet der Baum rund 300 ms, fast alles
Statement B des Live-Rests über vier Eimer, und dessen Streuung zwischen zwei Läufen ist größer als
die Lesung (Dienst `NEXANS` `48H` 315,6 gegen 344,0 ms bei 298,6 gegen 301,8 ms Bezug) — derselbe
Befund wie in M188 für die Übersicht. Der Ersatz selbst, eine Schleife über höchstens einige hundert
Zeilen, verschwindet in dieser Streuung.

> **Belegvermerk (Regel L10).** *Gemessen ist:* zwölf Lagen durch den Endpunkt, am Dienst und als
> Bezug, zwei Läufe, warm, Testclient auf demselben Rechner wie der Server, Uhr und Wasserstand
> gestellt. *Behauptet wird:* Der Prozessbaum mit Fehler live bleibt auf der Testkopie in der
> typischen Stunde und im freien Fenster unter 200 ms und im dichtesten Bereich unter 500 ms, und die
> Lesung kostet ihn, was sie in SQL kostet — 19 bis 40 ms in den gemessenen Fenstern. **Die Lücke:**
> Der Wasserstand ist gestellt — seine Abfrage (rund 1 ms) fehlt deshalb in der gemessenen Seite, wie
> in M185 und M188; die Kombination *viele Fehler im Fenster und dichter Verkehr am Bestandsende* gibt
> es auf der Testkopie nicht (Punkt 193 in [`live-rest.md`](live-rest.md) gilt weiter); kein Fall ist
> kalt gemessen.

#### Die Dev-Zeile — gehalten

`ProzessbaumFehlerLiveDbIT`, Uhr am Anker, Wasserstand der Testkopie (Live-Rest `NICHT_NOETIG`):
**Keine Zahl des Baums hat sich geändert** — angewandt gleich ausgesetzt, Feld für Feld bis auf
`fehlerLive`, für `NEXANS`, `SUTTONS` und `VOTG` in den drei Paaren und im freien Bösfall aus M152, in
beiden Gliederungen. Die Vorprobe davor — die Fehler des Rollups treffen `Message` — hielt in allen
zwölf Lagen. **Baum gleich Übersicht** in allen neun Paaren. Ausgegeben, nicht behauptet (Regel T2):

| | `48H` | `30T` | `12M` | `FREI` (Bösfall) |
|---|---:|---:|---:|---:|
| `NEXANS` Fehler | 50 | 55 | 711 | 662 |
| `NEXANS` Nachrichten | 9.950 | 176.050 | 2.308.005 | 2.309.634 |
| `SUTTONS` Fehler | 0 | 5 | 103 | 103 |
| `SUTTONS` Nachrichten | 1.337 | 20.964 | 196.536 | 196.508 |
| `VOTG` Fehler | 0 | 0 | 8 | 8 |
| `VOTG` Nachrichten | 399 | 5.937 | 113.291 | 113.776 |

Je Paar sind Kopfzahl *Fehler* und *Nachrichten* des Baums die Kacheln der Übersicht (M188: `NEXANS`
**50 / 55 / 711**, `SUTTONS` **0 / 5 / 103**, `VOTG` **0 / 0 / 8**), und die Fehler je Prozess sind
`COUNT(*)` aus `Message` mit Fehlerbedingung, Fenster und Kette — auch im freien Fenster.

---

## 6. Die Oberfläche (E‑212)

Bei `fehlerLive.zustand = AUSGESETZT` steht über den Kacheln ein Satz:

> *Die Fehlerzahlen konnten nicht live ermittelt werden und stammen aus der stündlichen Aggregation.
> Nachverarbeitete Nachrichten können darin noch als Fehler zählen.*

— der Textvorschlag des Auftrags, unverändert; englisch *„The error figures could not be determined
live and come from the hourly aggregation. Reprocessed messages may still count as errors there."*

- **Bauform und Ort des Hinweises zum Live-Rest** (E‑192): `Alert` ohne Variante, `Info`-Zeichen,
  kein Rot, kein neues Farbtoken; über den Kacheln, **auch im Leerzustand**.
- **Ein eigener Baustein:** `components/fehler-live-hinweis.tsx`, der Typ in `lib/fehler-live.ts`,
  die Texte unter `texte.fehlerLive` in `i18n/de.ts` und `en.ts` — dieselbe Bewegung wie beim
  Live-Rest, damit der Prozessbaum ihn mit Teil B ruft, ohne aus einem Nachbarfeature zu importieren.
- **Stehen beide Hinweise, stehen sie untereinander, der zum Live-Rest zuerst.**
- **Bei `ANGEWANDT` steht nichts** — ein Hinweis, der immer da ist, wird nicht mehr gelesen.
- **Kein Nachladen** (E‑137, E‑164); der Hinweis wechselt mit der nächsten Antwort.
- `components/ui` ist nicht angefasst; der shadcn-Skill war nicht nötig, weil `Alert` eingebunden
  bleibt wie beim Live-Rest.

**Lokal erscheint der Hinweis nicht:** Die Lesung ist auf der Testkopie angewandt. Eine
Sichtprüfung mit gestellter Antwort steht aus (§13).

> ### Ergänzt am 18.09.2026 — derselbe Baustein im Prozessbaum (E‑214, §5b)
>
> Der Baustein steht seit Teil B auch im Baum: **im klebenden Kopf der Baumspalte, unter den
> Kopfzahlen, unter dem Hinweis zum Live-Rest** — am Ort, den der Live-Rest dort seit E‑186 hat.
> Dieselben Texte, dieselbe Bauform, bei `ANGEWANDT` nichts, kein Nachladen. Die Überlegung „ein eigener
> Baustein, damit der Prozessbaum ihn mit Teil B ruft" oben ist damit eingelöst.

---

## 7. Die Dev-Zeile

Lesend geprüft am 18.09.2026 (M188, Sitzung 0), nichts verändert:

| | |
|---|---|
| **Rollup gegen Quelle** | `SUM(anzahl)` über `message_rollup` = `COUNT(*)` über `Message` = **3.341.519** — der Rollup deckt den ganzen Bestand |
| **Fehlerzeilen im ganzen Bestand** | **3.412**: `ERROR_DUPLICATE` 3.248, `COMMIT_REJECTED` 111, `ERROR_TIMEOUT` 53. Je Mandant `NEXANS` 3.301, `SUTTONS` 103, `VOTG` 8, alle übrigen sieben null. *M146 nannte für `NEXANS` 3.300 — der Unterschied von eins ist nicht nachgegangen und berührt hier nichts* |
| **Live-Rest lokal** | `NICHT_NOETIG` — W steht seit dem Lauf vom 27.08.2026 hinter der Anwendungsuhr ([`live-rest.md`](live-rest.md) §11) |
| **Fehler live lokal** | `ANGEWANDT` |

**Die Eichung hält: Keine Zahl der Übersicht hat sich geändert.**
`DashboardFehlerLiveDbIT.die_dev_zeile_ist_eine_identitaet` vergleicht die Seite bei angewandter
Lesung mit derselben Seite bei ausgesetzter — also der von vor diesem Schritt — Feld für Feld, bis
auf den Block `fehlerLive`: gleich, für `NEXANS`, `SUTTONS` und `VOTG` in allen drei Paaren. Die
Vorprobe davor — die Fehler des Rollups treffen `Message` im Fenster — hielt in allen neun Lagen, der
Wasserstand musste nicht nachgesehen werden. Die Kachel *Fehler* zählt, was M146 für Block 6 zählte:

| | `48H` | `30T` | `12M` |
|---|---:|---:|---:|
| `NEXANS` | 50 | 55 | 711 |
| `SUTTONS` | 0 | 5 | 103 |
| `VOTG` | 0 | 0 | 8 |

— je Paar gleich `COUNT(*)` aus `Message` mit Fehlerbedingung, Fenster und Kette, und gleich der
Summe über „Zuletzt aufgefallen" (höchstens drei betroffene Prozesse). **Ausgegeben, nicht
behauptet** (Regel T2): Zugesichert sind die Gleichheiten, nicht die Zahlen.

**Ein Abgang ist hier nicht herstellbar** — `RUNNING` kommt null Mal vor, und Tests schreiben nicht
(S1, T2). Den Fall aus der Produktion tragen die Diensttests mit erfundenen Zeilen (§9).

---

## 8. Die Messung — M188 (Regeln L7, L15)

### Vorregistriert — eingetragen und eingecheckt vor dem ersten Lauf

> **Rahmen, für alle drei Tore.** Gegen die Testkopie, Profil `dev`, am Anker `2025-12-30 04:09:47`
> — derselbe Stichtag wie M146 und M178. Die Fenster je Paar aus `Rollupzeitraum.fenster(Anker)`,
> Wanduhrzeit der Quelle, `bis` ausschließend: `48H` `2025-12-28 05:00` bis `2025-12-30 05:00`,
> `30T` `2025-12-01` bis `2025-12-31`, `12M` `2025-01-01` bis `2026-01-01`.
>
> - **Sequenziell, nie parallel** — kein zweiter Lauf gegen die Testkopie zur selben Zeit.
> - Je Lage **ein Aufwärmlauf, dann die beste von fünf**.
> - **In SQL:** `information_schema.PROFILING` **und** die Wanduhr des Servers (`SYSDATE(6)` vor und
>   nach dem Statement, E‑107), Eichung mit `SELECT 1`; `EXPLAIN` je Statement (Regel L15); dazu die
>   Handler-Zähler aus `information_schema.SESSION_STATUS` für einen Lauf je Lage (gelesene
>   Indexsätze, gemessen statt geschätzt). **Die Statementtexte kommen gerendert aus dem Code**
>   (`StatementType.STATIC_STATEMENT`, nur die Bindewerte als Literale) und sind nicht abgetippt.
> - **Am Code:** `MessungM188DbIT`, Bauform von M178 und M186. Zeiten nach `System.out`, in keine
>   Zusicherung (Regel T1). Die Uhr über `@TestBean` gestellt; kein Schreibzugriff auf
>   `GlassfishDB` oder `rollup_lauf` (S1, T2).
>
> #### Tor 1 — die Lesung (vor dem Einbau gemessen)
>
> **Lagen:** `NEXANS`, `SUTTONS`, `VOTG` und **ein Mandant ohne eine einzige Fehlerzeile**, je
> `48H`, `30T`, `12M` — zwölf Lagen. **Der vierte Mandant wird per Zählung bestimmt, nach einer vorab
> festgelegten Regel:** unter den Mandanten, bei denen im **ganzen** Bestand keine Zeile die
> Fehlerbedingung erfüllt, der mit den meisten Nachrichten im ganzen Bestand; bei Gleichstand der
> alphabetisch erste. Die Zählung steht als erste Abfrage der Sitzung im Protokoll.
>
> **Erwartet:**
>
> | | |
> |---|---|
> | **Plan** | `Message` wird über **`MessageStatusIDX`** gelesen, mit **6.257** geschätzten Zeilen — dieselbe Zahl wie Block 6 in M146, in allen zwölf Lagen; die Kette hängt per Primärschlüssel an. Welche Tabelle den Einstieg macht, darf wie in M146 am Mandanten hängen (`SUTTONS`: `ProjectMandant`) |
> | **Gelesen** | in allen zwölf Lagen **dieselbe Zahl** gelesener Indexsätze über den Statusbereich — unabhängig von Fenster und Mandant |
> | **Laufzeit** | **20 bis 26 ms** in SQL (M145, M146: 20,0 bis 26,2 ms für Block 6), am Code in der Größenordnung von M146 (23,3 bis 30,2 ms); unabhängig von Fenster und Mandant, auch beim Mandanten ohne Fehler |
>
> **Grenze:** In **keiner** Planzeile steht `MessageLastUpdateIDX` als `key` — auch nicht als
> Rowid-Filter (`MessageStatusIDX|MessageLastUpdateIDX`). *Zusätzlich zum Auftrag, als dieselbe
> Frage:* auch nicht `MessageLastUpdateProcessMessageIDX`, der zweite Zeitindex. Und **höchstens
> 100 ms je Lage** — in SQL (Profil und Wanduhr) und am Code.
>
> #### Tor 2 — die Verteilung ohne Fehler, beide Sichten
>
> **Lagen:** `NEXANS`, `SUTTONS`, je `48H`, `30T`, `12M`, je Sicht — zwölf Statements; in derselben
> Sitzung daneben jeweils die Form **ohne** Zusatzbedingung.
>
> **Erwartet:** **Der Plan ist der aus M178** — Zeile für Zeile (Tabelle, Zugriffsart, Index) derselbe
> wie der Plan der Form ohne Zusatzbedingung: bei `NEXANS` die Rollup-Ebene `range` über `PRIMARY`,
> bei `SUTTONS` `ProjectMandant` über `ProjectMandant_Mandant_idx`, der Katalog `eq_ref` über
> `PRIMARY`. Die Bedingung `NOT (…)` auf `message_status` ist ein Filter auf der gelesenen Zeile und
> kein Zugriffspfad. **Die Laufzeit liegt im Rauschen** der Form ohne Zusatzbedingung derselben
> Sitzung.
>
> **Grenze:** **Plan unverändert** — Zeile für Zeile wie oben. **Höchstens das 1,2‑Fache von M178
> je Lage.** Gemessen wird dafür **am Repository**, wie M178 gemessen hat (jOOQ-Rendering,
> Verbindung, Zeilenabbildung; beste von fünf); Profil und Wanduhr in SQL stehen daneben. **Bezug je
> Lage ist der kleinere der beiden M178-Läufe derselben Form** — die strengere Lesart, festgelegt vor
> dem Lauf. Gerechnet, nicht gemessen:
>
> | Lage | Partnersicht: Bezug → Grenze | Richtungssicht: Bezug → Grenze |
> |---|---:|---:|
> | `NEXANS` `48H` | 12,380 → **14,856 ms** | 12,685 → **15,222 ms** |
> | `NEXANS` `30T` | 71,205 → **85,446 ms** | 70,195 → **84,234 ms** |
> | `NEXANS` `12M` | 97,744 → **117,293 ms** | 95,738 → **114,886 ms** |
> | `SUTTONS` `48H` | 10,139 → **12,167 ms** | 9,968 → **11,962 ms** |
> | `SUTTONS` `30T` | 51,136 → **61,363 ms** | 51,391 → **61,669 ms** |
> | `SUTTONS` `12M` | 66,862 → **80,234 ms** | 66,444 → **79,733 ms** |
>
> #### Tor 3 — die Seite durch den Endpunkt
>
> **Lagen:** `NEXANS`, `SUTTONS`, je `48H`, `30T`, `12M` — sechs Lagen, am Anker, mit dem
> Wasserstand der Testkopie (Live-Rest `NICHT_NOETIG`, [`live-rest.md`](live-rest.md) §11). Je Lage
> **durch den Endpunkt** (`GET /api/dashboard?zeitraum=…`, HTTP-Umlauf im Testclient samt Sitzung
> und Serialisierung), **am Dienst** (`DashboardService.landingpage`) und als **Bezug in derselben
> Sitzung**: dieselbe Seite am Dienst mit ausgesetzter Fehlerlesung — das ist die Seite, wie sie vor
> diesem Schritt war. **Zuschlag** = Dienst minus Bezug.
>
> **Erwartet: der bisherige Wert plus die Lesung.** Gerechnet, nicht gemessen — die Seite aus M178
> (am Dienst), plus 10 bis 25 ms für den Endpunkt über dem Dienst (M186), plus die Lesung am Code
> (23,3 bis 30,2 ms, M146), plus 0 bis 2 ms für Wasserstand und Nachlesung:
>
> | Lage | M178, Seite | **erwartet, Endpunkt** |
> |---|---:|---:|
> | `NEXANS` `48H` | 65,4–69,9 ms | **98,7–127,1 ms** |
> | `NEXANS` `30T` | 223,3–225,5 ms | **256,6–282,7 ms** |
> | `NEXANS` `12M` | 294,2–297,6 ms | **327,5–354,8 ms** |
> | `SUTTONS` `48H` | 58,7–61,8 ms | **92,0–119,0 ms** |
> | `SUTTONS` `30T` | 166,3–167,5 ms | **199,6–224,7 ms** |
> | `SUTTONS` `12M` | 206,7–209,4 ms | **240,0–266,6 ms** |
>
> **Der Zuschlag** liegt erwartet bei **23 bis 32 ms** je Lage — die Lesung, bei Fehlern im Fenster
> die Nachlesung; die Verteilung ohne Fehler kostet, was die Verteilung kostet (Tor 2).
>
> *Zusätzlich zum Auftrag:* die teuerste Lage aus M186 — `NEXANS` im dichtesten Vierstundenbereich
> (G `2024-10-09 18:00`, jetzt G + 3 h, Live-Rest `ANGEWANDT` über den gesetzten Wasserstand), je
> Paar, damit beide Ausnahmen derselben Seite zusammen gemessen sind. Erwartet: M186 plus der
> Zuschlag, also **399,1–408,0 / 411,9–420,8 / 382,4–391,3 ms** für `48H` / `30T` / `12M`.
>
> **Grenze:** **höchstens 500 ms je Lage** durch den Endpunkt — das Tor aus M186, für alle neun Lagen.
>
> #### Die Dev-Zeile — die Eichung
>
> Auf der Testkopie ist der Rollup vollständig, und ein Abgang ist nicht herstellbar (`RUNNING` kommt
> null Mal vor, Tests schreiben nicht). **Der Ersatz ist hier deshalb eine Identität: Keine Zahl der
> Übersicht darf sich ändern** — Verlauf, Kacheln *Nachrichten* und *Fehler* samt Fehlerarten, beide
> Sichten der Verteilung, Leerzustand — für `NEXANS`, `SUTTONS` und `VOTG` in allen drei Paaren. Die
> Kachel *Fehler* ist danach, was Block 6 in M146 je Paar zählte: `NEXANS` **50 / 55 / 711**,
> `SUTTONS` **0 / 5 / 103**, `VOTG` **0 / 0 / 8**. Weicht eine Zahl ab: zuerst den Wasserstand
> prüfen, dann melden.
>
> #### Wie gelesen wird — festgelegt vor dem Lauf
>
> - **Liegt eine Lage über ihrer Grenze, oder steigt die Lesung über einen Zeitindex ein: anhalten
>   und berichten, nicht nachjustieren.** Ein Indexhinweis wäre eine Entscheidung (offener Punkt 82
>   in [`dashboard.md`](dashboard.md) §11) und keine Korrektur.
> - Der **Zuschlag** trägt die Aussage über diesen Bau; der **Endpunkt** trägt Tor 3; die
>   **Rechnungen** oben tragen nur, ob die Größenordnung hält. Die Form **ohne** Zusatzbedingung
>   derselben Sitzung trägt, ob eine Abweichung in Tor 2 dem Bau gehört oder dem Tag.
> - Abweichungen werden benannt und nicht umgedeutet; sie stehen neben diesem Kasten, nicht in ihm.
> - **Zugesichert wird im Läufer nur**, dass jede Antwort `200` ist, dass die Fehlerlesung
>   angewandt ist (in der Zusatzlage auch der Live-Rest) und dass Kachel *Nachrichten* und beide
>   Sichten dieselbe Zahl tragen — sonst mäße er die Laufzeit einer falschen Seite.

### Ergebnis — 18.09.2026, gegen die Vorregistrierung

**Alle drei Tore halten.** Die Erwartungen treffen nicht überall zu; jede Abweichung steht bei ihrem
Tor, keine ist umgedeutet. Sequenziell gefahren, kein Lauf parallel zu einem anderen gegen die
Testkopie: Sitzung 0 (Zählung), Sitzung 1 (Tor 1 in SQL), Tor 1 am Code, dann erst der Einbau,
danach Sitzung 2 (Tor 2 in SQL), Tor 2 am Repository und Tor 3 in einem Lauf, Tor 3 ein zweites Mal.

**Wo die Zahlen stehen:** `scripts/messung-fehler-live/` — `erzeuge.py` und die drei Sitzungen
(`s0-zaehlung.sql`, `s1-tor1-lesung.sql`, `s2-tor2-verteilung.sql`), die gefilterten Protokolle
`ergebnis/s1.gefiltert.txt` und `s2.gefiltert.txt` (Marken, Pläne, Zähler, Laufzeiten) und die
Laufzeilen der Läufer `ergebnis/m188-*.txt`. **Die Rohausgaben der Sitzungen sind nicht eingecheckt**
(G1): Die Lesung gibt `ProcessID` aus. Geeicht: dieselbe Suche nach Kennungsmustern findet in den
Rohausgaben 770 und 154 Treffer, in den eingecheckten Dateien keinen. Sitzungen ab 10:14 Serverzeit,
MariaDB 10.6.22, `read_only = 1`. Eichung der Wanduhr um `SELECT 1`: 0,806 bis 0,897 ms; die
Handler-Zähler um `SELECT 1`: 9 `read_rnd_next`, sonst null.

#### Sitzung 0 — der vierte Mandant, nach der Regel

| Mandant ohne eine einzige Fehlerzeile | Nachrichten im ganzen Bestand |
|---|---:|
| **`IBIS`** | **75.746** |
| `IBISGUS` | 29.339 |
| `ZAST` | 5.036 |
| `WOC` | 2.529 |
| `SYSTEM` | 151 |
| `NXHBE` | 9 |
| `EDITIONLINGERI` | 0 |

Nach der Regel ist es **`IBIS`**. Die Nachrichten stammen aus `message_rollup`, dessen Deckung in
derselben Sitzung geprüft ist (3.341.519 gegen `COUNT(*)` 3.341.519).

#### Tor 1 — die Lesung: gehalten

In Millisekunden, beste von fünf; *Profil* und *Wanduhr* aus Sitzung 1, *am Code* aus
`MessungM188DbIT.tor1_die_lesung`, vor dem Einbau:

| Mandant | Paar | Fehler im Fenster | Zeilen der Lesung | Indexsätze gelesen (`read_next`) | Profil | Wanduhr | am Code |
|---|---|---:|---:|---:|---:|---:|---:|
| `NEXANS` | `48H` | 50 | 2 | 3.412 | 21,611 | 22,489 | 23,370 |
| `NEXANS` | `30T` | 55 | 6 | 3.412 | 22,059 | 23,014 | 23,756 |
| `NEXANS` | `12M` | 711 | 35 | 3.412 | **31,512** | 32,669 | **34,536** |
| `SUTTONS` | `48H` | 0 | 0 | 3.413 | 21,238 | 22,275 | 23,505 |
| `SUTTONS` | `30T` | 5 | 5 | 3.413 | 21,137 | 22,264 | 22,895 |
| `SUTTONS` | `12M` | 103 | 61 | 3.413 | 24,864 | 26,603 | 26,506 |
| `VOTG` | `48H` | 0 | 0 | 3.412 | 20,500 | 21,722 | 21,517 |
| `VOTG` | `30T` | 0 | 0 | 3.412 | 20,163 | 20,995 | 22,192 |
| `VOTG` | `12M` | 8 | 1 | 3.412 | 21,444 | 22,482 | 24,018 |
| `IBIS` | `48H` | 0 | 0 | 3.412 | 20,116 | 21,276 | 22,491 |
| `IBIS` | `30T` | 0 | 0 | 3.412 | 21,145 | 22,427 | 22,118 |
| `IBIS` | `12M` | 0 | 0 | 3.412 | 22,527 | 23,434 | 23,838 |

**Die Pläne:** In allen zwölf Lagen liest `Message` über **`MessageStatusIDX`** (`range`,
`key_len` 123), mit **6.257** geschätzten Zeilen — dieselbe Zahl wie Block 6 in M146. Neunmal
treibt `Message`, und die Kette hängt per `eq_ref` über `PRIMARY` an; bei `SUTTONS` (drei Lagen)
beginnt der Plan bei `ProjectMandant` über `ProjectMandant_Mandant_idx` und liest `Message` mit
Join-Puffer — die Beobachtung aus M146. **`MessageLastUpdateIDX` steht in `possible_keys` und in
keiner Planzeile als `key`**, `MessageLastUpdateProcessMessageIDX` ebenso wenig, und kein Plan zeigt
einen Rowid-Filter. `NEXANS` `48H`, gekürzt um `possible_keys`:

```
| id | select_type | table          | type   | key              | key_len | ref                                        | rows | Extra
|  1 | PRIMARY     | Message        | range  | MessageStatusIDX | 123     | NULL                                       | 6257 | Using index condition; Using where; Using temporary; Using filesort
|  1 | PRIMARY     | fehler_process | eq_ref | PRIMARY          | 146     | GlassfishDB.Message.ProcessID              | 1    | Using where
|  1 | PRIMARY     | ProjectMandant | eq_ref | PRIMARY          | 292     | GlassfishDB.fehler_process.ProjectID,const | 1    | Using where; Using index
```

| Vorregistriert | Gemessen | trifft zu? |
|---|---|---|
| Einstieg über `MessageStatusIDX` | in allen zwölf Lagen | **ja** |
| dieselben 6.257 Zeilen wie Block 6 (M146) | 6.257 geschätzt in allen zwölf; **gelesen** sind 3.412 bis 3.413 Indexsätze — die Fehlerzeilen des ganzen Bestands, in jeder Lage dieselben | **ja** |
| unabhängig von Fenster und Mandant | **die gelesene Menge ja, die Zeit nicht ganz** | teilweise |
| 20 bis 26 ms (M145, M146) | in SQL 20,1 bis 24,9 ms in elf Lagen, **31,5 ms** bei `NEXANS` `12M`; am Code 21,5 bis 26,5 ms in elf Lagen, **34,5 ms** bei `NEXANS` `12M` | **nein, in einer Lage** |
| **Grenze:** kein Zeitindex, höchstens 100 ms | kein Zeitindex, höchstens 34,5 ms | **gehalten** |

**Die Abweichung, benannt:** Die eine Lage über dem Band ist die mit den meisten Fehlern im Fenster —
711 in 35 Gruppen, gegen 0 bis 103 sonst. Gelesen wird dort nicht mehr (dieselben 3.412 Indexsätze),
aber die Zähler der temporären Tabelle steigen (`read_rnd` 35 gegen 2, `read_rnd_next` 45 gegen 12
bei `NEXANS` `48H`). **Wohin die rund 10 ms gehen, ist nicht gemessen**; plausibel ist die
Gruppierung der Zeilen, die alle Bedingungen erfüllen. Die Aussage „unabhängig vom Fenster" gilt
damit für den Zugriff und nicht für die Laufzeit: **Die Lesung wächst mit der Zahl der Fehler im
Fenster** — offener Punkt 212.

> **Belegvermerk (Regel L10).** *Gemessen ist:* zwölf Lagen, je `EXPLAIN`, ein Lauf zwischen den
> Handler-Zählern, ein Aufwärmlauf und die beste von fünf in Profil und Wanduhr; dasselbe am Code im
> Testclient. Warm, gegen die Testkopie, am Anker. *Behauptet wird:* Die Lesung steigt über den
> Statusindex ein, liest die Fehlerzeilen des Bestands und kostet auf der Testkopie höchstens 34,5 ms.
> **Die Lücke:** 3.412 Fehlerzeilen sind der Bestand der Testkopie, nicht der Produktion; wie viele es
> dort sind und wie viele davon in einem Fenster liegen, ist nicht erhoben. Kein Fall ist kalt gemessen.

#### Tor 2 — die Verteilung ohne Fehler: gehalten

Am Repository, wie M178 gemessen hat (jOOQ-Rendering, Verbindung, Zeilenabbildung; beste von fünf),
in derselben Sitzung die heutige Form daneben; rechts dieselben Statements in SQL (Profil, beste von
fünf). In Millisekunden:

| Mandant | Paar | Sicht | heute | **ohne Fehler** | Unterschied | Grenze (1,2 × M178) | im Tor | SQL heute | SQL ohne Fehler |
|---|---|---|---:|---:|---:|---:|---|---:|---:|
| `NEXANS` | `48H` | richtung | 10,429 | **10,473** | +0,044 | 15,222 | ja | 9,651 | 9,858 |
| `NEXANS` | `48H` | partner | 10,604 | **10,790** | +0,186 | 14,856 | ja | 9,627 | 9,952 |
| `NEXANS` | `30T` | richtung | 68,329 | **71,519** | +3,190 | 84,234 | ja | 68,107 | 69,997 |
| `NEXANS` | `30T` | partner | 69,011 | **71,282** | +2,271 | 85,446 | ja | 68,463 | 70,511 |
| `NEXANS` | `12M` | richtung | 94,625 | **98,287** | +3,662 | 114,886 | ja | 95,090 | 97,372 |
| `NEXANS` | `12M` | partner | 95,307 | **98,992** | +3,685 | 117,293 | ja | 93,876 | 97,015 |
| `SUTTONS` | `48H` | richtung | 9,725 | **10,258** | +0,533 | 11,962 | ja | 9,326 | 9,517 |
| `SUTTONS` | `48H` | partner | 9,814 | **10,185** | +0,371 | 12,167 | ja | 9,187 | 9,567 |
| `SUTTONS` | `30T` | richtung | 50,682 | **53,612** | +2,930 | 61,669 | ja | 50,153 | 52,455 |
| `SUTTONS` | `30T` | partner | 50,999 | **53,706** | +2,707 | 61,363 | ja | 50,767 | 52,884 |
| `SUTTONS` | `12M` | richtung | 67,007 | **70,578** | +3,571 | 79,733 | ja | 65,696 | 68,993 |
| `SUTTONS` | `12M` | partner | 66,589 | **70,256** | +3,667 | 80,234 | ja | 66,402 | 69,848 |

**Das Tor hält mit Abstand:** Die Form ohne Fehler kostet das 0,826- bis 1,062-Fache von M178, die
Grenze war das 1,2-Fache. Die Summen stimmen mit der Kachel: heute minus die Fehler des Fensters
(`NEXANS` 9.950 − 50 = 9.900, 176.050 − 55 = 175.995, 2.308.005 − 711 = 2.307.294; `SUTTONS`
1.337, 20.964 − 5 = 20.959, 196.536 − 103 = 196.433).

**Der Plan ist der aus M178, Zeile für Zeile** — in allen zwölf Lagen dieselbe Folge aus Tabelle,
Zugriffsart und Index, dieselben `rows` und dieselbe `Extra`-Spalte wie die heutige Form: bei
`NEXANS` die Rollup-Ebene `range` über `PRIMARY` als Einstieg, bei `SUTTONS` `ProjectMandant` über
`ProjectMandant_Mandant_idx`, der Katalog `eq_ref` über `PRIMARY`. Gelesen wird derselbe Bereich
(`read_next` gleich: `NEXANS` 929 / 6.843 / 9.649, `SUTTONS` 930 / 6.844 / 9.650); `read_key` ist
bei der Form ohne Fehler um 2 bis 25 kleiner — die Fehlerzeilen fallen vor dem Katalog-Nachschlag weg.

**Die Abweichung, benannt: „im Rauschen" trifft in der Größe zu, nicht in der Richtung.** Die Form
ohne Fehler ist in **allen zwölf** Lagen die teurere — am Repository um 0,04 bis 3,69 ms (0,4 bis
5,8 %), in SQL um 0,19 bis 3,45 ms. Das Rauschen zwischen zwei Läufen lag in M178 bei bis zu 7,6 %,
aber es hatte keine Richtung. **Hier hat der Unterschied eine:** Die Bedingung wird je gelesener
Rollupzeile geprüft, und über zwölf Monate sind das bei `NEXANS` 9.649 Zeilen. Klein, gemessen, und
in Tor 3 als zweiter Summand des Zuschlags wiederzufinden.

> **Belegvermerk (Regel L10).** *Gemessen ist:* zwölf Lagen × zwei Fassungen, am Repository und in
> SQL, warm, in derselben Sitzung nebeneinander; `EXPLAIN` und Handler-Zähler je Fassung. *Behauptet
> wird:* Die Bedingung ändert keinen Zugriffspfad und kostet auf der Testkopie höchstens 3,7 ms je
> Statement. **Die Lücke:** ein Lauf; der Vergleich mit M178 überbrückt zwei Tage, und Punkt 134 in
> [`dashboard.md`](dashboard.md) zeigt, dass die Instanz zwischen zwei Tagen Dutzende Millisekunden
> verschieben kann — getragen wird die Aussage vom Vergleich derselben Sitzung.

#### Tor 3 — die Seite durch den Endpunkt: gehalten

Beste von fünf nach einem Aufwärmlauf, in Millisekunden, **zwei Läufe** (L1, L2) nacheinander; keiner
ist ausgewählt. **Endpunkt** ist `GET /api/dashboard?zeitraum=…` im Testclient samt Sitzung und
Serialisierung, **Dienst** `DashboardService.landingpage`, **Bezug** dieselbe Seite am Dienst mit
ausgesetzter Lesung — die Seite von vor diesem Schritt —, **Zuschlag** Dienst minus Bezug. Uhr und
Wasserstand gestellt (`@TestBean`); am Anker `NICHT_NOETIG`, in der Zusatzlage `ANGEWANDT`.

| Mandant · Lage | Paar | erwartet, Endpunkt | **Endpunkt** L1 / L2 | im Band | Dienst L1 / L2 | Bezug L1 / L2 | Zuschlag L1 / L2 | Nachrichten | Fehler |
|---|---|---:|---:|---|---:|---:|---:|---:|---:|
| `NEXANS` Anker | `48H` | 98,7–127,1 | **123,809 / 121,669** | ja / ja | 90,126 / 94,555 | 75,306 / 71,291 | 14,820 / 23,264 | 9.950 | 50 |
| `NEXANS` Anker | `30T` | 256,6–282,7 | **272,035 / 276,005** | ja / ja | 251,939 / 251,221 | 223,576 / 222,173 | 28,363 / 29,048 | 176.050 | 55 |
| `NEXANS` Anker | `12M` | 327,5–354,8 | **357,364 / 356,035** | darüber / darüber | 335,645 / 333,238 | 294,095 / 295,432 | 41,550 / 37,806 | 2.308.005 | 711 |
| `SUTTONS` Anker | `48H` | 92,0–119,0 | **99,363 / 95,649** | ja / ja | 84,007 / 78,009 | 57,815 / 57,402 | 26,191 / 20,607 | 1.337 | 0 |
| `SUTTONS` Anker | `30T` | 199,6–224,7 | **215,549 / 206,069** | ja / ja | 196,002 / 189,359 | 165,528 / 162,217 | 30,474 / 27,142 | 20.964 | 5 |
| `SUTTONS` Anker | `12M` | 240,0–266,6 | **265,311 / 254,620** | ja / ja | 245,427 / 236,855 | 205,749 / 205,386 | 39,678 / 31,469 | 196.536 | 103 |
| `NEXANS` dicht *(zusätzlich)* | `48H` | 399,1–408,0 | **414,530 / 398,984** | darüber / darunter | 385,223 / 376,186 | 370,276 / 350,556 | 14,947 / 25,629 | 33.148 | 15 |
| `NEXANS` dicht *(zusätzlich)* | `30T` | 411,9–420,8 | **418,374 / 403,609** | ja / darunter | 405,504 / 384,466 | 380,718 / 359,621 | 24,786 / 24,846 | 79.033 | 41 |
| `NEXANS` dicht *(zusätzlich)* | `12M` | 382,4–391,3 | **403,343 / 384,746** | darüber / ja | 388,725 / 373,282 | 341,403 / 327,359 | 47,323 / 45,923 | 222.049 | 2.163 |

**Das Tor: gehalten.** Die teuerste Lage ist `NEXANS` im dichtesten Vierstundenbereich über 30 Tage
mit **418,374 ms** (L1); alle achtzehn Messungen liegen unter 500 ms. In jeder Lage stimmen Kachel und
beide Sichten, und Fehler live ist angewandt.

**Die Rechnung der Seite trifft in zwölf von achtzehn Messungen,** am Anker in zehn von zwölf:
`NEXANS` `12M` liegt in beiden Läufen knapp darüber (+2,6 und +1,2 ms). In der Zusatzlage liegen zwei
Messungen darüber, zwei darunter und zwei im Band — die Bezüge dort streuen zwischen den Läufen um
bis zu 21 ms, mehr als doppelt so weit, wie die Bänder breit sind.

**Der Zuschlag trifft nicht zu: 14,8 bis 47,3 ms statt 23 bis 32.** Die Vorregistrierung hatte ihn
als *die Lesung, bei Fehlern im Fenster die Nachlesung* gerechnet und die Verteilung ohne Fehler mit
null angesetzt. Tor 2 hat gezeigt, dass sie das nicht ist. Nachgerechnet mit den Zahlen **derselben
Runde** — die Lesung am Code aus Tor 1, der Preis der Bedingung aus Tor 2 für beide Sichten:

| Lage (Anker) | Lesung (Tor 1, am Code) | + Preis der Bedingung, beide Sichten (Tor 2) | = gerechnet | Zuschlag gemessen, L1 / L2 |
|---|---:|---:|---:|---:|
| `NEXANS` `48H` | 23,370 | 0,230 | 23,600 | 14,820 / 23,264 |
| `NEXANS` `30T` | 23,756 | 5,461 | 29,217 | 28,363 / 29,048 |
| `NEXANS` `12M` | 34,536 | 7,347 | 41,883 | 41,550 / 37,806 |
| `SUTTONS` `48H` | 23,505 | 0,904 | 24,409 | 26,191 / 20,607 |
| `SUTTONS` `30T` | 22,895 | 5,637 | 28,532 | 30,474 / 27,142 |
| `SUTTONS` `12M` | 26,506 | 7,238 | 33,744 | 39,678 / 31,469 |

**Die Zerlegung trägt:** In neun von zwölf Werten liegt der gemessene Zuschlag höchstens 4 ms neben
der Rechnung. Die drei übrigen — `NEXANS` `48H` L1 (14,8), `NEXANS` `12M` L2 (37,8), `SUTTONS`
`12M` L1 (39,7) — liegen 4,1 bis 8,8 ms daneben, und zwar in beide Richtungen. Der Ausreißer nach
unten ist die **erste** Lage des ersten Laufs: Ihr Bezug fiel über die fünf Läufe noch von 88,3 auf
75,3 ms, im zweiten Lauf lag er bei 71,3. Dass sie noch nicht eingeschwungen war, ist plausibel und
nicht gemessen. Der Rest des Zuschlags — Nachlesung und Ersatz in Java — verschwindet in dieser
Streuung. In der Zusatzlage wächst der Zuschlag über zwölf Monate auf 45,9 bis 47,3 ms: Dort liegen
2.163 Fehler im Fenster, und nach Tor 1 wächst die Lesung mit ihnen — an dieser Lage selbst ist sie
nicht einzeln gemessen.

> **Belegvermerk (Regel L10).** *Gemessen ist:* neun Lagen durch den Endpunkt, am Dienst und als
> Bezug, zwei Läufe, warm, Testclient auf demselben Rechner wie der Server. *Behauptet wird:* Die
> Übersicht mit Fehler live bleibt auf der Testkopie in jeder Lage unter 500 ms, und Fehler live
> kostet die Seite die Lesung plus zweimal den Preis der Bedingung — gemessen 14,8 bis 47,3 ms. **Die Lücke:** Der
> Wasserstand ist gestellt, die Wasserstandsabfrage (rund 1 ms) fehlt deshalb in dieser Messung wie in
> M186; die Kombination *viele Fehler und dichter Verkehr am Bestandsende* gibt es auf der Testkopie
> nicht (Punkt 193 in [`live-rest.md`](live-rest.md) gilt weiter); kein Fall ist kalt gemessen.

#### Die Dev-Zeile — gehalten

Keine Zahl der Übersicht hat sich geändert (§7).

---

## 9. Tests

Keine Wanduhrzeit in einer Zusicherung (T1), kein Bestandswert als Erwartung (T2).

### Ohne Datenbank

| Test | Was er hält |
|---|---|
| `common/FehlerLiveTest` (**14**, neu) | **Der Ersatz (7):** der Fall aus der Produktion (Rollup `09:00 ERROR_TIMEOUT 1`, keine Live-Zeile in 09:00, `14:00 RUNNING 1` → Fehler 0, Nachrichten 1, kein Fehleranteil um 09:00); ein erneuter Fehler nach der Nachverarbeitung zählt einmal; ein Fehler im Live-Bereich zählt einmal, obwohl Live-Rest und Lesung ihn kennen; ein unbekanntes `ERROR_`, eine kleine Schreibweise und `COMMIT_REJECTED` sind `FEHLER`; leere Live-Zeilen entfernen alle Fehler und nur die Fehler; `AUSGESETZT` lässt die Grundzeilen unverändert; `AUSGESETZT` mit Zeilen fällt laut. **Werte (3):** nur `ANGEWANDT` trägt Zeilen; eine Zeile braucht Stunde, Prozess und mindestens eine Nachricht; der Block trägt nur den Zustand. **Der Ausfall (4):** an der Zeitgrenze (`SQLTimeoutException`, 1969) `AUSGESETZT` **mit genau einem `WARN`** im Protokoll (Logback-Anhang, ohne Uhr); ein Syntaxfehler setzt ebenfalls aus — wie beim Live-Rest; was keine `DataAccessException` ist, läuft durch; ohne Ausfall `ANGEWANDT` mit den Zeilen |
| `DashboardServiceTest` (59, **9 neu** unter „Fehler live") | der Fall aus der Produktion **vorher und nachher** mit denselben Zeilen (ausgesetzt: Fehler 1, Nachrichten 2 — die Meldung; angewandt: 0 und 1, der Eimer 09:00 verschwindet, Kachel und Sicht zählen 1); ein Fehler im Live-Bereich zählt einmal, auch in Block 5; die Hebung je Paar (Stunde, `DATE(stunde)`, Monatserster); Kachel und Fehlerarten aus der Lesung (das `ERROR_TIMEOUT` des Rollups steht nirgends mehr); Block 5 angewandt über die Nachlesung mit einem Prozess ohne Katalogzeile, und ausgesetzt im heutigen Wortlaut ohne Nachlesung — Kachel und Sichten gleich; der Leerzustand nach dem Ersatz in beide Richtungen; ausgesetzt ergibt die Zahlen von vorher samt Block; ein Uhrenschlag für Fenster, Live-Rest und Lesung. Die Lesung ist eine Attrappe; **ohne Stellung ausgesetzt**, damit die 50 Fälle von vorher dieselben Zahlen sehen |
| `DashboardStatementsTest` (43, **11 neu** unter `FehlerLive`) | die Lesung **wörtlich**, für jedes Paar derselbe Text; ihre Gestalt (Bereich ohne Funktion, voller Ausdruck im `GROUP BY`, Kette als `EXISTS`, kein Indexhinweis, keine Sortierung, keine Deckelung); **beide Fassungen der Verteilung wörtlich** und für alle sechs Kombinationen aus Paar und Sicht: die ohne Fehler ist die heutige mit genau der einen Bedingung hinter der Kette; **acht Lagen, jede mit ihren Statements einzeln benannt** — als Folge von Namen, nicht als Zahl (§5); ausgesetzt sind die zwei Verteilungsstatements Zeichen für Zeichen die heutigen |

### Mit Datenbank (`@Tag("db")`)

| Test | Was er hält |
|---|---|
| `DashboardFehlerLiveDbIT` (**3**, neu) | je Paar für `NEXANS`, `SUTTONS`, `VOTG`: **Kachel *Fehler* = `COUNT(*)` aus `Message`** mit Fehlerbedingung, Fenster und Kette, die Fehlerarten ebenso; **Kachel *Fehler* = Summe über „Zuletzt aufgefallen"**, solange höchstens zehn Prozesse betroffen sind (gezählt mit `COUNT(DISTINCT ProcessID)`); **die Dev-Zeile als Identität** — angewandt gleich ausgesetzt, Feld für Feld bis auf `fehlerLive`, nach einer Vorprobe gegen `Message` |
| `DashboardPlanDbIT` (11, **2 neu**) | die Lesung über **`MessageStatusIDX`**, in keiner Planzeile ein Zeitindex — auch nicht als Rowid-Filter —, keine Tabelle voll, für `NEXANS` und `SUTTONS` in allen drei Paaren; **die Verteilung ohne Fehler fährt Zeile für Zeile den Plan der Verteilung**, je Mandant, Paar und Sicht |
| `DashboardIsolationDbIT` (28, **2 neu**) | der Block `fehlerLive` steht in der Antwort mit einem der zwei Zustände; **die Lesung am Repository**: für `VOTG` über `12M` keine Prozesskennung von `SUTTONS`, und jede Kennung der Lesung in der Prozessliste von `VOTG` — mit der Eichung, dass `VOTG` dort Fehler hat. `sichtZaehltDieEigenenNachrichten` (Partner- und Richtungssicht) **grün** und läuft seither über die Nachlesung |
| `DashboardLiveRestDbIT` (1) | **grün**, unverändert in seinen Zusicherungen; der Dienst läuft seither mit der Lesung |
| `MessungM188DbIT` (3) | §8; zugesichert nur `200`, die Zustände und Kachel gleich beiden Sichten |

`PaketstrukturTest` (19) ist **unverändert grün**: `FehlerLiveRepository` endet auf `Repository`,
seine öffentliche Methode nimmt `MandantContext` zuerst, `common` hängt an nichts. Die drei
namentlichen Ausnahmen von M2 sind weiter drei. **Einheitstests des Backends: 1.004, grün**
(`./mvnw verify -DexcludedGroups=db`).

### Oberfläche

| Test | Was er hält |
|---|---|
| `tests/fehler-live.test.tsx` (**6**, gerendert, neu) | bei `AUSGESETZT` **vor der ersten Kachel** im Dokument und **auch im Leerzustand** ohne Kacheln; bei `ANGEWANDT` nichts, mit Kacheln und im Leerzustand; der Kasten trägt nicht die Fehlerfarbe; **stehen beide Hinweise, steht der zum Live-Rest zuerst** |

`pnpm check` grün mit **1.246 Fällen in 46 Dateien**; die gerenderten sind **180 in 22 Dateien** —
aus dem Lauf gezählt und je Datei gegen den Basisstand `2e3f094` (1.236 in 45) verglichen: die sechs
hier, +3 in `farbwerte` und +1 in `serverbausteine` für die zwei neuen Quelldateien. Der Kopf von
`vitest.config.mts` ist fortgeschrieben.

### Geänderte gepinnte Zusicherungen — einzeln

Ausschließlich Statementtexte und Statementzählungen, wie der Auftrag sie zulässt:

| Test | vorher | nachher | warum |
|---|---|---|---|
| `DashboardStatementsTest.die_zehn_statements_je_seite` | zehn Statements, Indizes 0 bis 9 | **`die_elf_statements_je_seite`**: elf; an Stelle 2 die Lesung (wörtlich), alle folgenden um eins verschoben; die Verteilung an 3 und 4 in der Form ohne Fehler | die Lesung läuft vor Block 5 |
| dasselbe, letzte Zusicherung | *„Ohne Lauf liest keine Seite Message außer den Kacheln und Zuletzt aufgefallen"*: kein Statement mit `date_format(` | das einzige Statement mit `date_format(` ist die Lesung | die Lesung bildet ihre Eimer wie der Rollup-Job |
| `…keine_frist_mehr_in_der_ganzen_seite` | die vollste Seite: dreizehn | vierzehn | eine Lesung mehr |
| `LiveRest.nicht_noetig_zehn` | zehn, kein `date_format(` | **`nicht_noetig_elf`**: elf, kein `live_process` | wie oben |
| `LiveRest.angewandt_ohne_korrekturzeile_zwoelf` | zwölf, A und B an 10 und 11 | **`…_dreizehn`**: dreizehn, A und B an 11 und 12 | eins verschoben |
| `LiveRest.angewandt_mit_korrekturzeile_dreizehn` | dreizehn, `subList(0, 12)`, Nachlesung an 12 | **`…_vierzehn`**: vierzehn, `subList(0, 13)`, Nachlesung an 13 | eins verschoben |
| `LiveRest.nachlesung_gestalt` | Nachlesung an Stelle 12 | an Stelle 13 | eins verschoben |

**Am Prüfaufbau geändert, an keiner Zusicherung:** Die Attrappe von `DashboardStatementsTest`
liefert die Live-Zeile nur noch für Statement B des Live-Rests (`live_process`) und nicht für jedes
Statement mit `date_format(` — sonst hätte die Lesung dieselbe Zeile bekommen; dazu zwei Schalter
für eine Fehlerzeile und einen Ausfall. `kein_zusammengelegtes_verteilungsstatement` läuft über vier
Seiten statt zwei. Die drei Tests, die `DashboardService` von Hand bauen, bekommen den neuen
Konstruktorparameter; `DashboardServiceTest` stellt die Lesung vorab auf ausgesetzt. In drei
Frontend-Tests trägt der erfundene Rumpf das neue Feld `fehlerLive`.

### Die Verletzungsprobe — ausgeführt, nicht angenommen

Gefahren am 18.09.2026: die Mandantenkette aus `FehlerLiveRepository.ausDerQuelle` entfernt →
`DashboardIsolationDbIT.die_fehlerlesung_liefert_keine_fremde_zeile:793` **rot** (*„Die
Fehlerlesung für VOTG darf keinen Prozess von SUTTONS liefern — ohne Mandantenkette im Statement
täte sie es"*). Aus einer Sicherungskopie zurückgespielt, mit `cmp` verglichen, in keinem Commit.
**Warum am Repository:** Durch den Endpunkt zeigte sich ein Leck der Lesung nur als Zahl, und die
Nachlesung hat ihre eigene Kette — sie ordnete fremde Prozesse *nicht zugeordnet* zu, Kachel und
Sichten zählten die fremden Fehler also beide und blieben gleich. Hergeleitet, nicht mit
ausgehängter Kette durch den Endpunkt gefahren; derselbe Grund wie beim Live-Rest
([`live-rest.md`](live-rest.md) §12).

**Zwei Gegenproben an den Diensttests**, ebenso zurückgespielt: der Ersatz ausgehängt → fünf der
neun Fälle rot (`der_fall_aus_der_produktion`, `hebung_je_paar`,
`kachel_und_fehlerarten_aus_der_lesung`, `leerzustand_nach_dem_ersatz`, `block5_angewandt`); Block 5
immer im heutigen Wortlaut → neun Fälle rot: `block5_angewandt` und `fehler_im_live_bereich_zaehlt_einmal`,
sechs Lagen in `DashboardStatementsTest` und `die_elf_statements_je_seite`.

> ⚠️ **`DashboardIsolationDbIT.keine_mandanten_id` ist in beiden Läufen dieses Tages gefallen**, 27
> von 28 grün. Der Vergleich der zwei Rümpfe als JSON zeigt in beiden Läufen **ausschließlich**
> `plattform.dienste[*].alterSekunden`, sechs Lampen um je eine Sekunde; der Block `fehlerLive` ist
> in beiden Rümpfen `ANGEWANDT`. Das ist Punkt **182** in [`dashboard.md`](dashboard.md) §11 — der
> Test vergleicht ganze Rümpfe über eine Sekundengrenze. **Nicht nebenbei repariert.** Die drei
> Aufrufe des Tests dauern auf der Testkopie zusammen 2,6 bis 4,2 Sekunden; dass er je grün wird,
> hängt damit am Zufall.

### Teil B — die Tests des Prozessbaums *(18.09.2026)*

Keine Wanduhrzeit in einer Zusicherung (T1), kein Bestandswert als Erwartung (T2).

**Ohne Datenbank**

| Test | Was er hält |
|---|---|
| `ProzessbaumServiceTest` (57, **8 neu** unter „Fehler live") | der Fall aus der Produktion **je Prozess, vorher und nachher mit denselben Zeilen** (ausgesetzt: Fehler 1, Nachrichten 2; angewandt: 0 und 1); ein Fehler im Live-Bereich zählt einmal, obwohl Live-Rest und Lesung ihn kennen; die Summen je Knoten und in `gesamt`, in beiden Gliederungen; `FREI` vor G (keine Korrektur, aber die Lesung), über G hinweg und ab G, die Lesung jeweils mit dem ausschließenden Ende; letzte Bewegung und Zustand bleiben durch die Lesung unverändert (still, nie, bewegt); die Klemme — eine negative Korrektur neben Fehlern der Lesung, je Prozess nach dem Ersatz, und die negative Korrektur einer Fehlerzeile fällt mit dem Ersatz heraus; ausgesetzt ergibt die Zahlen von vorher samt Block, in allen Modi und beiden Gliederungen; ein Uhrenschlag für Fenster, Live-Rest und Lesung. Die Lesung ist eine Attrappe, **ohne Stellung ausgesetzt**, damit die 49 Fälle von vorher dieselben Zahlen sehen |
| `ProzessbaumStatementsTest` (29, **1 neu**, 5 fortgeschrieben) | jede Lage mit ihren Statements **als Folge von Namen**: angewandter Live-Rest sechs (Gerüst, Kennzahlen, Wasserstand, A, B, Fehlerlesung), nicht nötig und ausgesetzt ohne Lauf je vier, `FREI` ebenso; `Message` in zwei Statements eines angewandten Aufrufs (B ab G, die Lesung im Fenster), sonst nur in der Lesung, nie als Join; neu: **die Lesung ist der Text der Übersicht**, für jedes Paar und das freie Fenster gegen das gerenderte `FehlerLiveRepository` gehalten — gepinnt bleibt er in `DashboardStatementsTest` |
| `FehlerLiveTest` (14) | unverändert grün |

**Mit Datenbank** (`@Tag("db")`, gefahren am 18.09.2026, 45 Fälle in fünf Klassen, alle grün)

| Test | Was er hält |
|---|---|
| `ProzessbaumFehlerLiveDbIT` (**3**, neu) | je Mandant (`NEXANS`, `SUTTONS`, `VOTG`), je Paar und für den freien Bösfall: **`gesamt.fehler` und die Fehler je Prozess sind `COUNT(*)` aus `Message`** mit Fehlerbedingung, Fenster und Kette; **Baum gleich Übersicht** — die beiden Antworten tragen dasselbe Fenster, und dann sind `gesamt.fehler` die Kachel *Fehler* und `gesamt.nachrichten` die Kachel *Nachrichten*; **die Dev-Zeile als Identität** — angewandt gleich ausgesetzt, Feld für Feld bis auf `fehlerLive`, in beiden Gliederungen, nach einer Vorprobe gegen `Message`. Die Uhr steht am Anker (`@TestBean`), die Zahlen werden ausgegeben |
| `ProzessbaumIsolationDbIT` (22, **1 neu**) | der Block `fehlerLive` steht in der Antwort, beide Gliederungen und das freie Fenster, mit einem der zwei Zustände. **Keine neue Mandantenkette:** Die Isolation der Lesung ist am Repository belegt (Teil A, `DashboardIsolationDbIT.die_fehlerlesung_liefert_keine_fremde_zeile`, Verletzungsprobe rot) |
| `ProzessbaumPlanDbIT` (15, **1 neu**) | die Lesung im freien Fenster steigt über **`MessageStatusIDX`** ein, in keiner Planzeile ein Zeitindex — auch nicht als Rowid-Filter —, keine Tabelle voll; vier Mandanten, vier Fenster (die drei freien aus M152 und das Jahresfenster aus Tor 1, als Eingabe). *„Kein Plan enthält `Message`"* gilt weiter für Gerüst und Kennzahlen |
| `ProzessbaumGleichheitDbIT` (3) | **grün**, unverändert |
| `ProzessbaumLiveRestDbIT` (2) | **grün**, keine Zusicherung geändert; der Dienst läuft seither mit der Lesung, die Summenprobe gilt mit beiden Ausnahmen angewandt |
| `MessungM189DbIT` (1, neu) | §5b; zugesichert nur `200`, die Zustände, dieselbe Kopfzahl an Endpunkt und Dienst und jedes Blatt einmal |

`PaketstrukturTest` (19) ist **unverändert grün**; die Klassen des Bausteins sind nicht angefasst bis
auf drei Javadoc-Sätze. **Einheitstests des Backends: 1.013, grün** (`./mvnw verify
-DexcludedGroups=db`; Teil A: 1.004).

**Oberfläche**

| Test | Was er hält |
|---|---|
| `tests/fehler-live.test.tsx` (10, **4 neu** im Prozessbaum) | bei `AUSGESETZT` **im klebenden Kopf der Baumspalte** (dem Behälter des Eingrenzungsfelds), nach dem Absatz der Kopfzahlen und vor dem Baum im Dokument; stehen beide Hinweise, steht der zum Live-Rest zuerst; bei `ANGEWANDT` steht keiner, mit dem Baum als Eichung; keine Fehlerfarbe |

`pnpm check` grün mit **1.250 Fällen in 46 Dateien**; die gerenderten sind **184 in 22 Dateien** — aus
dem Lauf gezählt; gegenüber Teil A (1.246, 180) genau die vier hier, keine neue Quelldatei. Der Kopf
von `vitest.config.mts` ist fortgeschrieben. In drei Frontend-Tests trägt der erfundene Baum das neue
Pflichtfeld `fehlerLive`.

#### Geänderte Zusicherungen — Teil B, einzeln

Ausschließlich Statementzählungen und Statementfolgen, wie der Auftrag sie zulässt — alle in
`ProzessbaumStatementsTest.EinAufruf`:

| vorher | nachher | warum |
|---|---|---|
| `angewandt_fuenf_statements`: fünf, einzeln an den Stellen 1 bis 5 | **`angewandt_sechs_statements`**: sechs als Namensfolge; die Stellen 1 bis 5 unverändert zugesichert, die sechste ist die Lesung | die Lesung läuft zuletzt |
| `message_nur_im_live_teil`: `Message` in genau **einem** Statement, dem letzten | **`message_im_live_teil_und_in_der_lesung`**: in genau **zwei** — B an Stelle 5 (ab G, Kette, kein Join, wie bisher) und die Lesung zuletzt (eigene Kette als `EXISTS`, kein Join) | die vierte Ausnahme liest `Message` |
| `ausgesetzt_ohne_lauf_drei_statements`: drei, **kein** `Message` | **`…_vier_statements`**: vier; die ersten drei ohne `Message`, das vierte ist die Lesung | die Lesung läuft auch ohne Live-Rest |
| `nicht_noetig_drei_statements`: drei, kein `Message` | **`…_vier_statements`**: dieselben vier | dasselbe |
| `freies_fenster_fuenf_statements`: fünf | **`freies_fenster_sechs_statements`**: sechs, die sechste die Lesung im freien Fenster | dasselbe |

**Am Prüfaufbau geändert, an keiner Zusicherung:** `ProzessbaumServiceTest` bekommt die Lesung als
Attrappe (ohne Stellung ausgesetzt); `ProzessbaumStatementsTest` baut den Dienst mit der echten Lesung
an der Attrappe; `ProzessbaumLiveRestDbIT` baut ihn mit dem Dienst aus dem Kontext. Ein neuer
Diensttest hieß zuerst wie ein Fall im Block des Live-Rests und ist umbenannt
(`lesung_im_freien_fenster_in_drei_lagen`) — Surefire zählt gleichnamige Methoden im Kopf seines
Berichts nur einmal.

#### Die Gegenproben — ausgeführt, nicht angenommen

Gefahren am 18.09.2026, jeweils aus einer Sicherungskopie zurückgespielt, mit `cmp` verglichen, in
keinem Commit; die roten Fälle aus dem XML des Berichts gezählt:

| gesetzt | rot | grün geblieben, und warum |
|---|---|---|
| **der Ersatz ausgehängt** (die Lesung wird nie angewandt) | **6 von 8** neuen Diensttests: `der_fall_aus_der_produktion`, `summen_in_beiden_gliederungen`, `lesung_im_freien_fenster_in_drei_lagen`, `letzte_bewegung_und_zustand_unveraendert`, `die_klemme`, `ausgesetzt_ergibt_die_zahlen_von_vorher` | `fehler_im_live_bereich_zaehlt_einmal` — ohne Ersatz zählt der Fehler über die Korrektur ohnehin einmal; `ein_uhrenschlag` prüft nur die Aufrufe |
| **der Ersatz vor dem Live-Rest** | **2**: `fehler_im_live_bereich_zaehlt_einmal`, `die_klemme` | — der Doppelzähler wird gefangen |
| **die Klemme je Zeile** statt je Prozess | **1**: `die_klemme` | — |
| **Oberfläche:** der Hinweis ausgehängt · vor den Hinweis zum Live-Rest gesetzt · aus dem klebenden Kopf genommen | **3** · **1** (die Reihenfolge) · **1** (der Ort) | die Abwesenheit bei `ANGEWANDT` bleibt in allen drei grün — richtig so |

**Keine Verletzungsprobe der Mandantenkette:** Teil B schreibt keine. Die Lesung ist die aus Teil A,
deren Kette dort mit ausgehängtem Filter rot wurde; ihre Zeilen erreichen den Rumpf des Baums zudem nur
über die Blätter des mandantengefilterten Gerüsts.

---

## 10. Regelbezug

| Regel | Stand |
|---|---|
| **L2** Keine Live-Aggregation über `Message` | **vierte benannte Ausnahme**, eingetragen und begründet in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Regel 2: Ein Fehlerstatus ist durch Nachverarbeitung nicht endgültig, und der Delta-Lauf entfernt Abgänge aus alten Eimern nicht. Gemessen in M188 |
| **L7**, **L15** | **erfüllt** — M188 mit `EXPLAIN` je Statement in beiden Sitzungen; `DashboardPlanDbIT` hält Einstieg und Zeitindex fest |
| **L9** | **erfüllt** — die Lesung hat ein Fenster, das der Seite |
| **L10** | **erfüllt** — drei Belegvermerke in §8 |
| **M1** | **erfüllt** — kein neuer Endpunkt, kein neuer Parameter, keine neue Mandantenausnahme |
| **M2** | **erfüllt** — `ausDerQuelle(MandantContext, …)`; `PaketstrukturTest` unverändert grün |
| **M3** | **erfüllt** — die Kette als `EXISTS` im Statement; Verletzungsprobe rot |
| **M4** | **erfüllt** — Isolation der Lesung am Repository, die Sichten weiter durch den Endpunkt |
| **S1** | **erfüllt** — Lese-Kontext; die Tests schreiben nicht in `GlassfishDB` und nicht in `rollup_lauf` (nur ihre `it-`-Konten in `overlord_monitor`) |
| **T1**, **T2** | **erfüllt** — keine Zeit und kein Bestandswert in einer Zusicherung; der Läufer gibt aus |
| **Z1** | **erfüllt** — ein Uhrenschlag; die Lesung bekommt das Fenster |
| **Q4**, E‑g | **erfüllt** — die Einordnung entsteht beim Lesen über den einen Klassifizierer; kein zweites `switch` |
| **Befund 11** | **erfüllt** — `GROUP BY` über den vollen Ausdruck |
| **§6 der Projektbeschreibung** | **erfüllt** — der Baustein liegt in `common`; das Dashboard ruft ihn, der Prozessbaum mit Teil B |

> ### Teil B, 18.09.2026 — der Regelbezug des Prozessbaums
>
> | Regel | Stand |
> |---|---|
> | **L2** | **dieselbe vierte Ausnahme, kein fünfter Fall** — der Baum ruft dieselbe Lesung; im Kasten der Ausnahme in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Regel 2 als zweiter Verbraucher eingetragen. Gemessen in M189 |
> | **L7**, **L15** | **erfüllt** — M189 mit `EXPLAIN` im Jahresfenster und in allen elf Fenstern der Tore 2 bis 4; `ProzessbaumPlanDbIT` hält den Einstieg der Lesung im freien Fenster fest |
> | **L9** | **erfüllt** — die Lesung hat das Fenster der Antwort, bei `FREI` höchstens ein Kalenderjahr |
> | **L10** | **erfüllt** — zwei Belegvermerke in §5b |
> | **M1** | **erfüllt** — kein neuer Endpunkt, kein neuer Parameter, keine neue Mandantenausnahme |
> | **M2**, **M3** | **erfüllt** — keine neue Lesung und keine neue Kette; `PaketstrukturTest` unverändert grün |
> | **M4** | **erfüllt** — der Block in `ProzessbaumIsolationDbIT`; die Isolation der Lesung ist aus Teil A am Repository belegt, Verletzungsprobe rot |
> | **S1**, **T1**, **T2** | **erfüllt** — die Tests schreiben nicht in `GlassfishDB` und nicht in `rollup_lauf`; keine Zeit und kein Bestandswert in einer Zusicherung |
> | **Z1** | **erfüllt** — ein Uhrenschlag für Fenster, Live-Rest und Lesung (`ein_uhrenschlag`) |
> | **Q4**, E‑g | **erfüllt** — die Einordnung entsteht beim Lesen über den einen Klassifizierer; der Ersatz tauscht über den Rohwert |
> | **§6 der Projektbeschreibung** | **erfüllt** — der Baum ruft den Baustein aus `common`, das Frontend den Baustein aus `components` |

---

## 11. Die Entscheidungen dieser Runde

| | | Wer |
|---|---|---|
| **E‑208** | **Weg B, Fehler live:** Die Übersicht liest die Einordnung `FEHLER` aus einer Live-Lesung über `Message`; alle anderen Einordnungen aus Rollup und Live-Rest. **Verworfen:** A (Abgleich im Delta-Lauf), C (Nachlauf auf 48 Stunden), D (nur Doku) | Auftraggeber, 18.09.2026, per Auswahl |
| **E‑209** | Der Baustein in `common`: Lesung ohne Indexhinweis und ohne Sortierung, Kette mit eigenem Alias; Dienst mit dem Ausfall nach E‑185; der Ersatz als reine Funktion über einen Rohwert je Zeile; Block `fehlerLive` nur mit Zustand, ohne Zeitangabe | Auftrag; Ausgestaltung Bau |
| **E‑210** | Die Lesung an zweiter Stelle der Seite, vor Block 5; der Ersatz nach der Verrechnung des Live-Rests, die Zeilen auf den Eimer des Paares gehoben und je (Eimer, Rohstatus) summiert | Auftrag (vor Block 5, nach E‑190); Ort Bau |
| **E‑211** | Block 5: die Form ohne Fehler als zweite Methode derselben Gestalt, die heutige Zeichen für Zeichen; Korrekturzeilen ohne Fehler und Fehlerzeilen über die Nachlesung — **die Nachlesung nur, wenn Block 5 etwas zuzurechnen hat** (Auslegung, §5) | Auftrag; Auslegung Bau, gemeldet |
| **E‑212** | Der Hinweis bei `AUSGESETZT` über den Kacheln, unter dem zum Live-Rest, auch im Leerzustand; eigener Baustein, Typ, Texte; der Text des Auftrags | Auftrag; Baustein Bau |
| *Tore M188* | 100 ms je Lage für die Lesung ohne Zeitindex; das 1,2-Fache von M178 und derselbe Plan für die Verteilung; 500 ms je Lage für die Seite | Auftraggeber, im Auftrag |
| **E‑213** *(Teil B)* | **Der Prozessbaum ruft Fehler live:** die Lesung als **letztes** Statement über das Fenster der Antwort, aus demselben Uhrenschlag, bei `FREI` mit dem ausschließenden Ende; der Ersatz je (Prozess, Rohstatus) **nach** der Verrechnung des Live-Rests und **vor** Kennzahl und Klemme, die Zeilen der Lesung über das Fenster summiert, mit derselben Fensterprüfung wie die Korrektur; die Klemme unverändert je Prozess und Zahl; der Ausfall wie in Teil A; der Block `fehlerLive` in allen Modi und beiden Gliederungen | Auftrag; Zwischengestalt und Fensterprüfung Bau |
| **E‑214** *(Teil B)* | Der Hinweis im Baum im klebenden Kopf der Baumspalte, unter den Kopfzahlen und unter dem Hinweis zum Live-Rest; derselbe Baustein, keine neuen Texte, kein Nachladen | Auftrag |
| *Tore M189* *(Teil B)* | Tor 1: kein Zeitindex, höchstens 100 ms; Tore 2 und 4: **höchstens 200 ms** je Lage — die alte Schranke der typischen Stunde (150 ms, M152, M185) plus die teuerste Lesung aus M188 (34,5 ms), aufgerundet, weil die Lesung die alte nach Rechnung überschreitet; Tor 3: 500 ms. Dazu die Regel der Zählung für das Jahresfenster und die Uhr von Tor 4, beide vorregistriert | Auftraggeber, im Auftrag; Zählregel und Uhr von Tor 4 Bau, vorregistriert |

---

## 12. Offene Punkte

| | |
|---|---|
| ~~**209**~~ | ~~**Prozessbaum und Übersicht zählen Fehler verschieden — bis Teil B.** Der Baum nimmt seine Fehler weiter aus Rollup und Live-Rest; eine nachverarbeitete Nachricht zählt dort bis zum Volllauf als Fehler, in der Übersicht nicht mehr. Wie Punkt 191 beim Live-Rest: Teil B ruft denselben Baustein~~ **Erledigt am 18.09.2026 mit Teil B** (§5b): Der Baum ruft denselben Baustein; auf der Testkopie sind Kopfzahl *Fehler* und *Nachrichten* in allen neun Paaren die Kacheln der Übersicht (`ProzessbaumFehlerLiveDbIT.baum_gleich_uebersicht`), und der Fall aus der Produktion steht je Prozess in den Diensttests |
| **210** | **Abgänge aus anderen Status bleiben bis zum Volllauf stehen — benannte Grenze.** Wechselt eine Nachricht aus `RUNNING`, `SUSPENDED`, `FINISHED` oder einem anderen Status heraus ihren Status, nachdem der Delta-Lauf ihren Eimer zum letzten Mal gerechnet hat, steht sie im alten Eimer weiter und im neuen dazu: Liegen beide im Zeitraum, zählt die Kachel *Nachrichten* sie bis 03:00 **doppelt**. Die bekannte Grenze 3 in [`dashboard.md`](dashboard.md) §2 ist entsprechend berichtigt. Für Fehler ist das seit heute behoben, für alles andere bewusst nicht gebaut |
| **211** | **Block 6 und die Lesung lesen dieselbe Menge zweimal** — derselbe Statusbereich, einmal je Prozess gedeckelt, einmal je Stunde. Eine Zusammenlegung ist nicht gebaut (Auftrag); sie sparte eine der beiden Lesungen — gerechnet, nicht gemessen, rund 20 ms je Seite (M146, Tor 1) —, kostete aber die Deckelung von Block 6 oder die Gruppierung der Lesung und bräuchte eine eigene Messung |
| **212** | **An der Produktion ist nichts gemessen.** Weder der Mechanismus — hergeleitet aus dem Bau und aus einer Meldung — noch die Kosten: Die Lesung liest alle Fehlerzeilen des Bestands (auf der Testkopie 3.412) und wächst zusätzlich mit den Fehlern im Fenster (711 kosten rund 10 ms mehr als 50). Wie viele es in der Produktion sind, ist nicht erhoben. Die Grenze von 100 ms je Lage gilt der Testkopie. *Nachtrag 18.09.2026 (M189, Tor 1):* Im Jahresfenster mit den meisten Fehlern der Testkopie — 3.204, im Baum als freies Fenster abrufbar — kostet die Lesung **69,8 ms**, sieben Zehntel der Grenze; sie wächst mit den Fehlern im Fenster, aber nicht mit ihrer Zahl allein (2.163 Fehler kosten 39,9 ms). Ein Mandant mit mehr Fehlern in einem Jahr käme der Grenze entsprechend näher |
| **213** | **SQL und Java müssen sich einig sein, was ein Fehler ist** (§4). Für Rohwerte, die nur die Sortierung der Spalte gleichmacht — `'COMMIT_REJECTED '` mit Leerzeichen am Ende unter `PAD SPACE` —, sind sie es nicht; die Nachricht zählte dann in der Kachel *Nachrichten* doppelt und nicht als Fehler. Auf der Testkopie kommt kein solcher Wert vor; der Test gegen neue Statuswerte fiele bei seinem ersten Auftreten. Eine eigene Entscheidung, falls es ihn je gibt |
| **214** | **Der Ausfall des Live-Rests (E‑185) hat keinen eigenen Test seines `catch`-Zweigs.** Beim Bau „wie beim Live-Rest" gesucht und nicht gefunden: kein Test lässt eine Live-Lesung scheitern. Fehler live hat einen (`FehlerLiveTest.Ausfall`); für den Live-Rest ist er nicht nachgerüstet — außerhalb dieses Auftrags |
| **215** | **Eine Rohausgabe mit Prozesskennungen ist eingecheckt:** `scripts/messung-live-rest/ergebnis/s1.txt` (M185) trägt rund 200 Zeilen mit Kennungen, die Partnernamen Dritter enthalten — gegen die Trennung, die `.gitignore` für alle anderen Messrunden vorsieht (G1). Nicht angefasst; eine Entfernung stünde weiter in der Historie |
| **216** *(Teil B)* | **Die Fensterverengung der Nachrichtenliste liest die Einordnung `FEHLER` aus dem Rollup** (`message/VerengungRepository.frageStufe`, gefunden bei der Suche nach weiteren Lesern, §5b). Bei `status=FEHLER` zählt sie die Fehler je Stunde aus `message_rollup`, um das Fenster vorab zu verengen, und ruht dabei auf der Eigenschaft *„die gezählte Menge ist stets eine Unterschranke der wirklichen"* (`Verengungsgrenzen`). **Ein Abgang bricht sie** — hergeleitet, nicht gemessen: Eine nachverarbeitete Nachricht steht bis 03:00 im Rollup als Fehler, die Liste liest sie live nicht mehr; zählt der Rollup mehr Fehler, als es gibt, kann die Verengung zu eng ziehen, die Seite meldet dann keine weitere, und ältere Fehler des Fensters erscheinen bis zum Volllauf nicht. Nach Punkt 210 gilt dasselbe für jeden Statusfilter. Auf der Testkopie nicht herstellbar; **nicht umgebaut** (der Auftrag: melden). Eine eigene Entscheidung |

**Punkt 182** in [`dashboard.md`](dashboard.md) §11 bleibt offen und hat einen Nachtrag (§9 oben).

---

## 13. Was nicht gebaut ist

**Kein Prozessbaum** — das ist Teil B (Punkt 209). **Nichts am Rollup:** weder am Rollup-Job noch an
den Rolluptabellen, an `rollup_lauf`, an `NACHLAUF_MINUTEN` oder am Zeitplan. **Kein Abgleich für
andere Status** (Punkt 210). **Block 6 unverändert**, samt Indexhinweis, und nicht mit der Lesung
zusammengelegt (Punkt 211). **Keine Änderung** an Nachrichtenliste, Fensterverengung, BAM-Suche und
Property-Suche. **Kein neuer Endpunkt, kein neuer Parameter, keine neue Mandantenausnahme.** Kein
Nachladen im Takt (E‑137, E‑164), keine Migration, kein Index, **kein Indexhinweis**. **Keine
Regeländerung im `MessageStatusClassifier`**; `istEndstatus` bleibt. Keine Zeitangabe im Block
`fehlerLive`. Keine Sichtprüfung im Browser — der Hinweis erscheint lokal nicht.

> *Teil B, 18.09.2026:* Der Prozessbaum ist gebaut (§5b). **Nichts an der Übersicht** — Teil A steht;
> auch der Javadoc-Satz in `DashboardService`, der den Baum noch als künftigen Verbraucher nennt, ist
> nicht angefasst. **Nichts am Rollup**, weder an den Rolluptabellen noch an `rollup_lauf`, an
> `NACHLAUF_MINUTEN` oder am Zeitplan. Kein Abgleich für andere Status (Punkt 210), keine
> Zusammenlegung von Block 6 und Lesung (Punkt 211), kein Test für den Ausfall des Live-Rests (Punkt
> 214), nichts an Punkt 182. **Keine Änderung** am Kennzahlenstatement, an der letzten Bewegung, an
> Zuständen, Schwelle, Gliederungen, Übertragungsliste oder Nachrichtenliste — **auch nicht an der
> Fensterverengung**, die Punkt 216 betrifft. Kein neuer Endpunkt, kein neuer Parameter, keine neue
> Mandantenausnahme; kein Nachladen im Takt, keine Migration, kein Index, kein Indexhinweis, keine neuen
> Texte. Keine Sichtprüfung im Browser — lokal ist die Lesung angewandt, der Hinweis erscheint nicht.

---

## 14. Abweichungen vom Auftrag, einzeln

1. **Die Nummern:** E‑208 statt „ab E‑193", M188 statt M187, Punkt 209 statt 196 — die erwarteten
   waren am 17. und 18.09.2026 an die Darstellungswahl der Dateiansicht vergeben (Nummernvergabe oben).
2. **Die Bedingung der Nachlesung** ist ausgelegt als „Zeilen für Block 5" (§5, E‑211). Der
   Unterschied zum Wortlaut ist die eine Lage, in der die einzigen Korrekturzeilen Fehler sind.
3. **Tor 1 strenger als verlangt:** Geprüft ist auch der zweite Zeitindex und der Rowid-Filter, nicht
   nur `MessageLastUpdateIDX` — vorregistriert.
4. **Tor 3 mit einer Lage mehr** — der dichteste Bereich aus M186 mit angewandtem Live-Rest —,
   vorregistriert; und **zweimal gefahren**, weil der Zuschlag im ersten Lauf außerhalb der Erwartung
   lag. Beide Läufe stehen da.
5. **Tor 3 mit gestelltem Wasserstand** wie M186 — die Wasserstandsabfrage (rund 1 ms) fehlt in der
   gemessenen Seite.
6. **„Ausfall: geprüft wie beim Live-Rest"** — der Live-Rest hat dafür keinen eigenen Test (Punkt
   214). Geprüft ist der Dienst selbst: Zeitgrenze und Syntaxfehler setzen aus, mit `WARN`; alles
   andere läuft durch.
7. **Über die Doku-Liste des Auftrags hinaus** stehen datierte Kästen in [`dashboard.md`](dashboard.md)
   §5 („nicht ermittelbar" — der Rückfall des Live-Rests seit E‑185 stand dort nie, der von Fehler
   live kommt hinzu) und in [`process-view.md`](process-view.md) §3 (die Fehler des Baums bis Teil B).

**Teil B, 18.09.2026, einzeln:**

8. **Tor 1 mit elf Fenstern mehr:** dieselbe Lesung in SQL für jedes Fenster der Tore 2 bis 4 —
   vorregistriert, zusätzlich zum Auftrag; sie trägt die Aussage „der Zuschlag ist die Lesung".
9. **Die Tore 2 bis 4 zweimal gefahren** (L1, L2), nicht vorregistriert — weil im ersten Lauf drei
   Zuschläge außerhalb des Bandes lagen. Beide Läufe stehen da, keiner ist ausgewählt.
10. **Die Uhr von Tor 4** — der Auftrag nennt für `FREI` keine Stunde aus M185: am Anker mit
    Wasserstand `04:00`, Live-Rest angewandt über zwei Eimer; vorregistriert.
11. **Die Abfrage am Endpunkt nennt `gliederung=PARTNER`**, damit Endpunkt und Dienst denselben Baum
    bauen; M185 hatte die Vorgabe des Kontos genommen.
12. **Das lokale Dev-Backend war während der Läufe gestoppt** und ist danach mit demselben Befehl
    wieder gestartet worden — damit kein zweiter Prozess gegen die Testkopie lief, und weil es nach dem
    Bau alte Klassen geladen hatte (der Baum der Oberfläche fand sonst `fehlerLive` nicht).
13. **`filtere.py` lässt eine Tabelle mehr durch** — die der Zählung, nur Stunden und Zahlen.
14. **Drei Javadoc-Sätze in `common`** (`FehlerLiveErgebnis`, `FehlerLiveErsatz`, `FehlerLiveResponse`)
    nennen den Baum jetzt als Verbraucher; am Code des Bausteins ist nichts geändert.
15. **`ProzessbaumPlanDbIT` prüft die Lesung für alle vier Mandanten** der Klasse und vier freie Fenster,
    darunter das Jahresfenster aus Tor 1 als Eingabe; der Auftrag nannte das freie Fenster allgemein.
16. **Über die Doku-Liste des Auftrags hinaus:** die Zeile von Teil B im Kopf von
    [`process-view.md`](process-view.md), die Kästen in §2 und §6 dieser Datei, die Einträge in
    [`README.md`](README.md) und je eine datierte Zeile in
    [`dashboard-frontend.md`](dashboard-frontend.md) und [`live-rest.md`](live-rest.md) §16, die den
    Baum noch als künftigen Verbraucher nannten.
