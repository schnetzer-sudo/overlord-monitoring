# Dashboard — die Landingpage

Stand: 03.09.2026 · Schritt 10b‑4 · **Backend, keine Oberfläche**

> **Was 10b‑4 geändert hat:** Die Problemkategorie *Überfällig* ist widerlegt und aus dem MVP
> genommen (E‑71); an die Stelle ihrer Kachel treten *Läuft* und *Wartend* (§5). Alle übrigen
> Abschnitte sind vom 31.08.2026 (Schritt 10b‑2 Teil D) und unverändert, soweit kein
> Korrekturblock daneben steht.

> **Was der 16.09.2026 geändert hat (E‑161, §9b):** Die Antwort trägt die Verteilung **in beiden
> Sichten**, der Parameter `verteilung` ist entfallen, und das Verteilungsstatement läuft je Seite
> zweimal — unverändert in seiner Gestalt. Gemessen als **M178** (§8). Korrekturblöcke dazu stehen
> in §1, §2, §4, §7 und §9; der alte Wortlaut bleibt überall stehen.

> **Was der 18.09.2026 geändert hat (Fehler live, E‑208 bis E‑212, [`fehler-live.md`](fehler-live.md)):**
> Die Einordnung `FEHLER` kommt nicht mehr aus dem Rollup, sondern aus einer Live-Lesung über
> `Message` — die vierte benannte Ausnahme von L2. Eine nachverarbeitete Nachricht zählte bis dahin
> bis zum Volllauf als Fehler. Gemessen als **M188**. Datierte Kästen dazu in §1, §2 (samt
> **Berichtigung der bekannten Grenze 3**), §4, §5, §7, §7a, §9 und §11.

Der eine Endpunkt, aus dem die Landingpage entsteht. Er liest die drei Rollup-Ebenen aus
[`rollup.md`](rollup.md), ordnet die Rohwerte über `MessageStatusClassifier` ein
([`message-status.md`](message-status.md)) und hängt den Prozess-Katalog an
([`prozess-katalog.md`](prozess-katalog.md)).

**Gebaut ist das Backend.** Kein Diagramm, keine Kachel, kein Umschalter — die Oberfläche ist ein
eigener Schritt. Was hier steht, ist die Antwort und ihre Begründung.

**Keine Migration.** Höchste Version bleibt `V12`.

---

## 1. Der Endpunkt

```
GET /api/dashboard?zeitraum={48H|30T|12M}&verteilung={PARTNER|RICHTUNG}
```

| Parameter | Werte | Vorgabe |
|---|---|---|
| `zeitraum` | `48H`, `30T`, `12M` | **keine** — der Endpunkt wählt selbst (§3) und **nennt das gewählte Paar in der Antwort** |
| `verteilung` | `PARTNER`, `RICHTUNG` | `PARTNER` |

**Kein Mandantenparameter, in keiner Form** (Regel M1). Der Mandant kommt aus der Sitzung. Ein
`?mandant=…` ist kein Fehler, sondern wirkungslos — `DashboardIsolationDbIT` hält das fest. **Dieser
Endpunkt ist keine neue benannte Ausnahme**; die drei, die es gibt, stehen in
[`mandantentrennung.md`](mandantentrennung.md) §3 und definieren allesamt eine *Berechtigung*, statt
einen Datenausschnitt abzufragen.

**Kein Eintrag in `SecurityConfig`, und das ist richtig.** Der Pfad fällt unter
`anyRequest().authenticated()`. Eingetragen wird dort nur, wer eine **Rollengrenze** braucht — der
Katalog etwa, weil er `ADMIN` verlangt.

> ### Ein Aufruf, eine Antwort
>
> **Kein Block wird nachgeladen.** Das ist keine Bequemlichkeit, sondern das Leistungsbudget: Sechs
> Anfragen mit je einer Sitzungsprüfung und je einem Verbindungsgriff kosten mehr als sieben
> Abfragen auf einer Verbindung — und auf der Testkopie schreibt jede Anfrage zusätzlich die
> Sitzung fort. Zwei Tests halten es fest: einer auf der Antwort (alle Blöcke sind da), einer auf
> den Statements (es sind genau sieben).

> ### ⚠️ Korrektur vom 16.09.2026 — `verteilung` ist kein Parameter mehr (E‑161)
>
> **Die Adresszeile, die Parametertabelle und der Kasten darüber bleiben Zeichen für Zeichen
> stehen**, damit ablesbar bleibt, wie der Vertrag bis dahin geschnitten war. Was gilt:
>
> ```
> GET /api/dashboard?zeitraum={48H|30T|12M}
> ```
>
> | Parameter | Werte | Vorgabe |
> |---|---|---|
> | `zeitraum` | `48H`, `30T`, `12M` | **keine** — unverändert (§3) |
>
> **Ein mitgeschicktes `verteilung` ist wirkungslos, wie `?mandant=`** — auch ein unbekannter Wert.
> Bis zu diesem Tag ergab der ein `400` mit dem Problemtyp `verteilung-unbekannt`; beides ist
> entfallen, denn es gibt nichts mehr zu wählen. `DashboardIsolationDbIT.verteilung_ist_wirkungslos`
> hält das fest (§9).
>
> **„Ein Aufruf, eine Antwort" bleibt bestehen, und zwar wörtlich.** Die Antwort trägt beide Sichten
> der Verteilung, und der Umschalter wechselt im Browser, ohne eine Anfrage zu stellen
> ([`dashboard-frontend.md`](dashboard-frontend.md) §2). Bis dahin war ein Sichtwechsel ein neuer
> Aufruf der **ganzen** Seite — und die Oberfläche baute dabei jeden Block neu auf, nicht nur die
> Verteilung. **Die Zahl im Kasten stimmt nicht mehr:** Es sind seit 10d acht Statements (§7) und seit
> diesem Tag **neun**; `DashboardStatementsTest` benennt sie einzeln.

### Die Antwort

```jsonc
{
  "zeitraum": "48H",                       // das gewaehlte Paar, immer gesetzt
  "fenster":  { "von": "…Z", "bis": "…Z" },// die gelesenen Grenzen, UTC, bis ausschliessend
  "leer":     false,                       // der Leerzustand (§6)
  "verlauf":  [ { "eimer": "…Z", "gesamt": 9,
                  "einordnungen": [ { "einordnung": "ABGESCHLOSSEN", "anzahl": 8 } ] } ],
  "kacheln": {
    "nachrichten": 9950,
    "fehler":      { "anzahl": 50,
                     "arten": [ { "rohwert": "ERROR_TIMEOUT", "art": "TIMEOUT", "anzahl": 49 } ] },
    "laeuft":      { "anzahl": 0,   "aeltesteSekunden": null,   "ermittelbar": true },
    "wartend":     { "anzahl": 538, "aeltesteSekunden": 579934, "ermittelbar": true }
  },
  "verteilung": { "sicht": "PARTNER",
                  "zeilen": [ { "art": "WERT", "wert": "…", "anzahl": 8608, "enthaltene": null },
                              { "art": "UEBRIGE", "wert": null, "anzahl": 31, "enthaltene": 14 },
                              { "art": "NICHT_ZUGEORDNET", "wert": null, "anzahl": 490,
                                "enthaltene": null } ] },
  "zuletztAufgefallen": [ { "messageId": "…", "zeitpunkt": "…Z", "status": "ERROR_TIMEOUT",
                            "statusKind": "FEHLER", "kategorie": "FEHLER",
                            "processId": "…", "sosName": "…" } ],
  "stand": { "beendetAm": "…Z", "art": "VOLL" },
  "plattform": {                           // Block 8, seit 10d — fuer jeden Mandanten gleich
    "dienste": [ { "serviceId": "…", "zustand": "ZEITUEBERSCHRITTEN",
                   "rohwert": "ERROR_TIMEOUT", "stand": "…Z", "alterSekunden": 6 } ],
    "ablagen": { "zustand": "ERREICHBAR", "grund": null,
                 "ziele": [ { "serviceId": "…", "zustand": "ERREICHBAR" } ],
                 "geprueftAm": "…Z", "alterSekunden": 0 }
  }
}
```

> **Der Block `plattform` ist am 10.09.2026 hinzugekommen** (Schritt 10d Teil A, E‑116). Er trägt je
> eine Lampe für jeden Dienst mit `ServiceTimeout > 0` und **eine** Kachel für die Ablagen;
> vollständig in [`dienste.md`](dienste.md). `alterSekunden` rechnet dort wie überall gegen die
> Anwendungsuhr und ist `null`, wenn der Zeitpunkt nach `jetzt` liegt (E‑75).

> ### ⚠️ Korrektur vom 16.09.2026 — der Block `verteilung` trägt beide Sichten (E‑161)
>
> **Der Rumpf oben bleibt stehen.** An der Stelle von `"verteilung": { "sicht": …, "zeilen": … }`
> steht jetzt:
>
> ```jsonc
> "verteilung": {
>   "partner":  { "zeilen": [ { "art": "WERT", "wert": "…", "anzahl": 8608, "enthaltene": null },
>                             { "art": "UEBRIGE", "wert": null, "anzahl": 31, "enthaltene": 14 },
>                             { "art": "NICHT_ZUGEORDNET", "wert": null, "anzahl": 490,
>                               "enthaltene": null } ] },
>   "richtung": { "zeilen": [ { "art": "WERT", "wert": "EINGEHEND", "anzahl": 5120, "enthaltene": null },
>                             { "art": "WERT", "wert": "AUSGEHEND", "anzahl": 3997, "enthaltene": null },
>                             { "art": "NICHT_ZUGEORDNET", "wert": null, "anzahl": 12,
>                               "enthaltene": null } ] }
> }
> ```
>
> **Das ist die Form aus dem Auftrag, ohne Abweichung.** Die Sicht ist der **Schlüssel** und kein
> Feld mehr: `sicht` sagte, welche Sicht geliefert worden war, und seit beide geliefert werden, gibt
> es darauf keine Antwort. Je Sicht gelten Zeilenarten, Top‑10‑Grenze, beide Restzeilen unten und
> E‑i **unverändert** (§4). *Die Zahlen im Beispiel sind erfunden, wie oben — aber so gewählt, dass
> beide Sichten zusammen dieselbe Summe zählen (9.129). Das ist keine Zufälligkeit des Beispiels,
> sondern die Eigenschaft, an der `DashboardIsolationDbIT` die Trennung je Sicht nachweist (§9).*
>
> Im Code: `VerteilungResponse(partner, richtung)`, je Sicht ein `VerteilungszeilenResponse(zeilen)`.
> Der Typ, der bis dahin `VerteilungResponse(sicht, zeilen)` hieß, ist darin aufgegangen.

> ### ⚠️ Ergänzt am 17.09.2026 — der Block `liveRest` (Live-Rest, Teil B)
>
> **Der Rumpf oben bleibt stehen.** Zwischen `stand` und `plattform` steht seither:
>
> ```jsonc
> "liveRest": { "zustand": "ANGEWANDT", "vollstaendigBis": null }
> ```
>
> `zustand` ist einer von `ANGEWANDT`, `NICHT_NOETIG`, `AUSGESETZT`; `vollstaendigBis` ist **G** in
> UTC und nur bei `AUSGESETZT` mit vorhandenem Lauf gesetzt. **Derselbe Block wie im Prozessbaum**
> (`common/LiveRestResponse`, E‑189), und er sagt für diese Seite: Verlauf, Kacheln *Nachrichten* und
> *Fehler* und beide Sichten der Verteilung tragen den Verkehr seit dem letzten Rollup-Lauf — oder,
> bei `AUSGESETZT`, sie tun es nicht, und die Oberfläche sagt es über den Kacheln. Vollständig in
> [`live-rest.md`](live-rest.md) §9b. `DashboardIsolationDbIT.der_block_live_rest_steht_in_der_antwort`
> hält den Block fest; kein neuer Parameter, kein neuer Endpunkt.

> ### ⚠️ Ergänzt am 18.09.2026 — der Block `fehlerLive` (Fehler live, E‑209)
>
> **Der Rumpf oben bleibt stehen.** Hinter `liveRest` und vor `plattform` steht seither:
>
> ```jsonc
> "fehlerLive": { "zustand": "ANGEWANDT" }
> ```
>
> `zustand` ist `ANGEWANDT` oder `AUSGESETZT`, **ohne Zeitangabe**. Angewandt heißt: Verlauf, Kachel
> *Fehler* samt Fehlerarten und beide Sichten der Verteilung tragen die Fehler, die **jetzt** in
> `Message` stehen. Ausgesetzt — die Lesung ist ausgefallen — tragen sie die Fehler aus dem Rollup,
> und die Oberfläche sagt es über den Kacheln. Vollständig in [`fehler-live.md`](fehler-live.md);
> `DashboardIsolationDbIT.der_block_fehler_live_steht_in_der_antwort` hält den Block fest. Kein
> neuer Parameter, kein neuer Endpunkt.

---

## 2. Die Blöcke — acht, und weiterhin sieben Statements

> ⚠️ **Die Zahl der Statements ist dieselbe geblieben, und das ist eine Falle.** In Schritt
> 10b‑4 sind **drei** weggefallen (zweimal *Überfällig*, die Überfälligkeitshälfte von Block 6)
> und **drei** hinzugekommen (*Läuft*, *Wartend*, die Erscheinungsbedingung). `DashboardStatementsTest`
> hat deshalb aufgehört zu zählen und **benennt** seither jedes Statement einzeln (§9).

| # | Block | Quelle | Statements |
|---|---|---|---:|
| 1 | **Verlauf** je Eimer, nach Einordnung | Rollup-Ebene des Paares × Mandantenkette | 1 |
| 2 | **Kachel Nachrichten** | *derselbe Lesevorgang wie 1* | — |
| 3 | **Kachel Fehler** samt Aufschlüsselung nach Art | *derselbe Lesevorgang wie 1* | — |
| 4 | **Kachel Läuft** — Zahl und Alter der ältesten | **live** über `Message` | 1 |
| 4a | **Kachel Wartend** — dieselben zwei Werte, dazu die Erscheinungsbedingung | **live** über `Message` bzw. `SOSAction` | 2 |
| 5 | **Verteilung** — Partner oder Richtung | Rollup-Ebene × Mandantenkette × `process_catalog` | 1 |
| 6 | **Zuletzt aufgefallen** | `Message`, nur noch die Fehlerbedingung (§7a) | 1 |
| 7 | **Stand** — Zeitpunkt und Laufart | `rollup_lauf` | 1 |
| 8 | **Plattform** — die Dienstlampen und die Ablagenkachel | `Service`; die Kachel aus dem Speicher | 1 |

> ### ⚠️ Korrektur vom 10.09.2026 — es sind **neun** Blöcke und **acht** Statements
>
> **Die Überschrift oben bleibt Zeichen für Zeichen stehen** („Die Blöcke — acht, und weiterhin
> sieben Statements"), damit ablesbar bleibt, wie die Seite bis Schritt 10c geschnitten war. Was gilt:
> Mit Schritt 10d Teil A kommt **Block 8** hinzu — der plattformweite Teil
> ([`dienste.md`](dienste.md)) —, und er kostet **ein** Statement. Neun Blöcke, wenn man 4a mitzählt
> wie die Überschrift es tut; acht Statements.
>
> **Er ist der einzige Block, der für jeden Mandanten identisch ist.** Er sagt nichts über Belege,
> sondern über die Anlage, auf der sie laufen — und das ist keine Lücke in der Mandantentrennung,
> sondern sein Gegenstand. `DashboardIsolationDbIT` hält die **Gleichheit** fest, nicht die
> Verschiedenheit.
>
> **Die Ablagenkachel kostet kein Statement der Seite**, und das ist der Punkt: Ihr Zustand stammt
> aus einer echten Erreichbarkeitsprüfung, die **im Hintergrund** läuft (`fixedDelay`, Vorgabe 60 s).
> Ein Abruf gegen eine abgeschaltete Ablage dauert allein rund **2,7 Sekunden** (M174) — beim Aufruf
> gefragt, hinge die Landingpage an den Zeitgrenzen fremder Knoten statt an den eigenen.
>
> `DashboardStatementsTest` **benennt das achte Statement einzeln**, wie die sieben davor.

> ### ⚠️ Korrektur vom 16.09.2026 — Block 5 kostet **zwei** Statements, es sind **neun** (E‑161)
>
> **Tabelle und Kasten darüber bleiben stehen.** Was sich ändert, ist genau eine Zeile:
>
> | # | Block | Quelle | Statements |
> |---|---|---|---:|
> | 5 | **Verteilung** — Partner **und** Richtung, beide in jeder Antwort | Rollup-Ebene × Mandantenkette × `process_catalog`, **je Sicht einmal** | **2** |
>
> **Die Zahl der Blöcke bleibt**; die Verteilung ist weiterhin ein Block mit einem Umschalter. Die
> Zahl der Statements steigt von acht auf **neun**, wenn `zeitraum` genannt ist.
>
> **Zwei Statements derselben Gestalt und kein zusammengelegtes.** Ein Statement mit beiden
> `CASE`-Ausdrücken im `GROUP BY` läse den Bereich nur einmal, wäre aber eine **andere Abfrage** als
> die gemessene — eine Zeile je vorkommender Kombination aus Partner und Richtung, die Summen je Sicht
> erst in Java — und bräuchte eigene Messung und Entscheidung. Der Auftrag schließt sie aus;
> `DashboardStatementsTest.kein_zusammengelegtes_verteilungsstatement` hält fest, dass keine Abfrage
> der Seite beide Katalogspalten zugleich liest. Das neunte Statement steht dort an dritter Stelle
> und ist einzeln benannt (§9).

> ### ⚠️ Korrektur vom 18.09.2026 — Block 3 liest die Fehler live (E‑208, E‑210)
>
> **Tabelle und Kästen darüber bleiben stehen.** Was sich ändert:
>
> | # | Block | Quelle | Statements |
> |---|---|---|---:|
> | 1 | **Verlauf** | Rollup-Ebene × Mandantenkette, plus Live-Rest — **die Einordnung `FEHLER` aus der Lesung** | 1 |
> | 3 | **Kachel Fehler** samt Arten | **die Lesung** über `Message` (`common/FehlerLiveRepository`), Fehlerbedingung × Fenster × Mandantenkette | **1** |
> | 5 | **Verteilung** | bei angewandter Lesung **ohne die Fehler des Rollups**, die Fehlerzeilen über die Katalog-Nachlesung | 2 |
>
> **Die Lesung ist das zweite Statement der Seite**, vor Block 5 — dessen zwei Statements hängen an
> ihrem Zustand. Mit genanntem `zeitraum` sind es seither **elf bis vierzehn** (§7).
> *„Derselbe Lesevorgang wie 1"* gilt für Block 3 nur noch, wenn die Lesung ausfällt.
>
> **Die Einordnung entsteht weiter beim Lesen, und die Fehlerarten kommen weiter aus dem Rohwert**
> (die beiden Abschnitte darunter): Die Zeilen der Lesung tragen Rohwerte, und dieselbe
> `MessageStatusClassifier.einordnung` entscheidet, welche Zeilen des Rollups herausfallen.

### Die Einordnung entsteht beim Lesen

**Im Rollup steht der Rohwert** (Entscheidung E‑g): `FINISHED`, nicht `ABGESCHLOSSEN`. Die Kategorie
bildet `MessageStatusClassifier.einordnung(rohwert)` — **gerufen, nicht nachgebaut**
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1). Ein zweites `switch` über Statuswörter
liefe beim nächsten neuen Statuswert von der Liste weg, und der Unterschied fiele erst auf, wenn
jemand zwei Zahlen nebeneinanderlegt.

**Ein unbekannter Rohwert fällt nach `UNGEKLAERT`** und in keinen bekannten Eimer (Regel Q4) — und
ein unbekannter Wert **mit** `ERROR_`-Präfix nach `FEHLER`, weil dieselbe Regel in SQL gilt.

**Je Eimer nur die Einordnungen, die vorkommen.** Alle acht je Eimer wären bei 48 Stunden 384
Einträge, die meisten null. Die Reihenfolge ist die der Aufzählung und damit über alle Eimer
dieselbe — eine Oberfläche, die Farben nach Position vergibt, bekäme sonst in jedem Balken eine
andere.

### Die Fehlerarten kommen aus demselben Rohwert

`MessageStatusClassifier.fehlerart(rohwert)`, ebenfalls an genau einer Stelle:

| Rohwert | `art` |
|---|---|
| `ERROR_DUPLICATE` | `DUPLICATE` — der Namensteil hinter dem Präfix |
| `COMMIT_REJECTED` | **`Vom Partner abgelehnt`** — der feste Text aus §4.2 |
| alles andere | **der Rohwert, unverändert** (Regel Q4) |

**Der Rohwert steht daneben und wird nicht ersetzt.** Ohne ihn wäre *„Vom Partner abgelehnt"* eine
Zeichenkette, an der sich nichts mehr festmachen ließe — keine Übersetzung, kein Link, kein Filter.

**Kein zweiter Lesevorgang.** Genau deshalb gruppiert Block 1 nach *Rohstatus* und nicht schon nach
Einordnung.

> **„Unquittiert" ist nicht gebaut** (Entscheidung E‑d vom 24.08.2026) — kein Feld, kein
> Platzhalter, keine leere Liste. Zwei Tests halten das fest, einer über die Feldnamen der Records
> und einer über den Antwortrumpf. `COMMIT_REJECTED` gehört ohnehin nicht dorthin: **Das ist eine
> Quittung, nur eine negative.**

### Die Fenstergrenzen liegen auf Eimergrenzen

Beide Grenzen liegen auf einer **Eimergrenze**, die obere ist der Anfang des *nächsten* Eimers und
**ausschließend**. Der angebrochene Eimer, in dem `jetzt` liegt, gehört dazu — genauso setzt
`RollupFenster.ausgedehnt` sein Fenster, und genauso hat M94 gemessen.

**Das unterscheidet sich absichtlich vom Listen-Endpunkt.** Der löst `zeitraum` auf die Sekunde
genau gegen die Anwendungsuhr auf. Für eine Liste ist das richtig; für ein Diagramm wäre es falsch:
Der erste und der letzte Balken wären angebrochen und würden trotzdem so hoch gezeichnet wie ein
ganzer.

> ### ⚠️ Seit dem 17.09.2026 zählen Baum und Dashboard die laufende Stunde verschieden — bis Teil B
>
> Der Prozessbaum rechnet seit dem Live-Rest ([`live-rest.md`](live-rest.md)) den Verkehr seit dem
> letzten Delta-Lauf dazu: minus die Rollupzeilen des Live-Bereichs, plus die Zählung aus `Message`.
> **Dieses Dashboard liest weiter allein den Rollup.** Für dasselbe Fenster können Baum und
> Übersicht deshalb bis zum nächsten Delta-Lauf zwei Zahlen zeigen — im angebrochenen Eimer, um
> das, was seit dem Lauf passiert ist. Das ist offener Punkt **191** in `live-rest.md`; **Teil B**
> ruft denselben Baustein (`common/LiveRestService`) und ordnet die Korrektur den Eimern des
> Verlaufs zu. Der Block *Stand* (`letzterLauf()`) bleibt, wo er ist.

> ### ✔ Am selben Tag eingelöst — Teil B *(17.09.2026, E‑190, E‑191)*
>
> **Der Kasten darüber bleibt stehen; sein Zustand hat wenige Stunden gedauert.** Seit Teil B ruft
> `DashboardService.landingpage` denselben Baustein mit demselben Uhrenschlag und ordnet die
> Korrektur seinen Eimern zu: `48H` die Stunde, `30T` `DATE(stunde)`, `12M` der Monatserste — die
> Zuordnung, mit der der Rollup seine abgeleiteten Ebenen bildet ([`rollup.md`](rollup.md) §5).
> Verlauf und beide Rollup-Kacheln rechnen mit den verrechneten Zeilen **wie bisher**; Block 5
> bekommt die Korrektur je Schlüssel über eine **Katalog-Nachlesung** (ein Statement mehr, nur bei
> `ANGEWANDT` mit Korrekturzeilen; §7). **Baum und Übersicht zählen die laufende Stunde seither
> gleich**, und Kachel und beide Sichten zählen dieselbe Zahl — `DashboardLiveRestDbIT` hält es je
> Paar gegen `COUNT(*)` aus `Message` fest. Punkt 191 ist geschlossen. **Die Belegungsprobe des
> Standardfensters bleibt ohne Korrektur** — sie entscheidet über das Paar, bevor der Live-Rest
> gelesen ist; der Block *Stand* bleibt, wo er ist. Vollständig in [`live-rest.md`](live-rest.md)
> §9b, gemessen als **M186** (dort §8b).

### Die Kachel *Nachrichten* zählt Aktivität und nicht Nachrichten

> ### ⚠️ Bekannte Grenze 3
>
> `message_rollup` gruppiert nach **`MessageLastUpdate`**. Wechselt eine Nachricht ihren Status,
> wandert sie in einen anderen Eimer — sie verschwindet dabei aus dem alten, **doppelt gezählt wird
> also nichts**, aber sie erscheint in der Zählung eines Zeitraums, in dem sie nicht entstanden ist.
>
> **Der sichtbare Fall ist der nächtliche Sprung:** Ein Batchlauf, der tausend alte Nachrichten
> anfasst, hebt den Balken der Nachtstunde, ohne dass eine einzige neue Nachricht eingegangen wäre.
>
> **Nicht gebaut wird dagegen etwas.** Die Alternative wäre eine zweite Rolluptabelle über
> `MessageCreated` — eine vierte Ebene, ein zweiter Lauf und eine zweite Wahrheit, zwischen denen
> die Oberfläche wählen müsste. Der Rollup zählt, was sich bewegt hat; das ist für ein Monitoring
> die brauchbarere Größe.

> ### ⚠️ Berichtigt am 18.09.2026 — zwischen zwei Volllaufen wird doppelt gezählt
>
> **Der Kasten darüber bleibt stehen. Sein Satz *„doppelt gezählt wird also nichts"* gilt erst nach
> dem Volllauf.** Dazwischen gilt er nicht: Wechselt eine Nachricht ihren Status, nachdem ihr alter
> Eimer gerechnet ist, bucht der Delta-Lauf sie in den Eimer ihrer neuen letzten Änderung — und den
> alten schreibt er nicht neu. Er rechnet nur die vorige und die laufende Stunde ([`rollup.md`](rollup.md)
> §3, §5), der Live-Rest korrigiert nur ab G ([`live-rest.md`](live-rest.md) §2). **Bis 03:00 steht
> die Nachricht in beiden Eimern.** Das ist ein *Abgang* aus einem alten Eimer und kein
> Nachschreiben; kein Nachlauffenster erreicht ihn ([`rollup.md`](rollup.md) §13, Punkt 49).
>
> **Gemeldet vom Auftraggeber am 18.09.2026 aus der Produktion:** ein `ERROR_TIMEOUT`, nach der
> Nachverarbeitung `RUNNING` — die Fehlerkachel und der Verlauf zählten ihn weiter als Fehler.
>
> | Abgang aus … | Stand seit dem 18.09.2026 |
> |---|---|
> | einem **Fehler** | **behoben** — die Übersicht liest die Einordnung `FEHLER` live ([`fehler-live.md`](fehler-live.md), E‑208). Die Kachel *Fehler* zählt die Nachricht nicht mehr |
> | **jedem anderen Status** (`RUNNING`, `SUSPENDED`, `FINISHED`, …) | **benannte Grenze, nicht gebaut** — liegen alter und neuer Eimer im Zeitraum, zählt die Kachel *Nachrichten* die Nachricht bis 03:00 doppelt, und der Verlauf zeigt sie im alten Eimer mit ihrem alten Status (Punkt 210 in [`fehler-live.md`](fehler-live.md)) |
>
> **Ein Fehler, der nur wandert — im Fehler bleibt, aber in einen neuen Eimer gebucht wird —, zählt
> nicht doppelt:** Der Ersatz nimmt alle Fehlerzeilen des Rollups heraus, auch die im alten Eimer,
> und setzt die der Lesung ein. Die Nachricht steht einmal da, dort, wo sie jetzt steht.

---

## 3. Das Standardfenster richtet sich nach dem Mandanten

Ohne `zeitraum` wird das **erste** Paar der Reihe 48 h → 30 Tage → 12 Monate genommen, das **beide**
Bedingungen erfüllt:

1. **Anteil belegter Eimer ≥ 50 %**
2. **und mindestens ein Eimer mit mehr als fünf Nachrichten**

**Die 50 % sind aus M95 abgeleitet und nicht gewählt:** `NEXANS`, `SUTTONS` und `VOTG` liegen bei
allen drei Paaren auf 100 %; `IBIS` und `IBISGUS` fallen bei 48 Stunden auf 37,50 % und 27,08 %. Der
Sprung liegt damit nicht zwischen groß und klein, sondern beim **verstreutesten** Verkehr — `IBIS`
hat 63 Prozesse auf 235 Nachrichten, `VOTG` 14 auf 399. Genau den soll die Ansicht nicht als
Diagramm mit Lücken zeigen.

**Gesucht wird der Reihe nach und nicht in einem Statement.** Der Normalfall — ein Mandant mit
Verkehr — ist nach der ersten, kleinsten Abfrage entschieden; nur wer bei 48 Stunden durchfällt,
kostet eine zweite. Drei Belegungsproben auf einmal kosteten **immer** auch die teuerste, und die
liest die Monatsebene.

**Mit ausdrücklich genanntem `zeitraum` entfällt die Probe ganz** — ein Aufruf mit Parameter kostet
also *weniger* als einer ohne.

> ### ⚠️ Bekannte Grenze 1: Bedingung 2 ist in der Praxis wirkungslos
>
> Sie sollte `WOC` fangen: **29 von 30 Tagen belegt bei 117 Nachrichten** (M95), also knapp vier am
> Tag — ein Diagramm mit Punkten, das nichts zeigt. **Weil EDI-Verkehr stoßweise ist, liegt aber mit
> hoher Wahrscheinlichkeit ein Tag über fünf, und `WOC` besteht die Bedingung.**
>
> **Das ist gerechnet und nicht gemessen** — M108 misst `NEXANS` und `SUTTONS`, nicht `WOC`. Die
> Rechnung: 117 Nachrichten auf 29 Tage bei stoßweisem Verkehr; damit **kein** Tag über fünf liegt,
> müssten sich die Nachrichten fast gleichmäßig verteilen, und genau das tun sie nicht.
>
> **Der Auftraggeber hat das am 31.08.2026 in Kenntnis dieser Folge so entschieden.** Es steht hier
> als bekannte Grenze und wird **nicht nachgebessert**: Eine Schwelle, die `WOC` sicher fängt, finge
> auch Mandanten mit echtem, aber dünnem Verkehr — und die hätten dann kein Diagramm, obwohl es
> etwas zu sehen gäbe.
>
> Damit ist offener Punkt **61** aus [`messungen-schritt10b.md`](messungen-schritt10b.md)
> geschlossen: Die Festlegung ist getroffen, beide Bedingungen sind gebaut, und die zweite ist als
> gewählte und nicht gemessene Zahl benannt.

---

## 4. Die Verteilung

**Ein Block, zwei Sichten** (Partner ⇄ Richtung) über **dasselbe Statement**, nur mit einer anderen
Katalogspalte im Ausdruck. Die Eimerbreite des Paares spielt keine Rolle — gruppiert wird über das
ganze Fenster —, die Fensterbreite schon.

### Die drei Zeilenarten

| `art` | Was | Wann |
|---|---|---|
| `WERT` | ein benannter Partner beziehungsweise eine benannte Richtung | Rang 1 bis 10 |
| `UEBRIGE` | alles ab Rang 11, mit `enthaltene` = wie viele | **nur, wenn es einen Rang 11 gibt** |
| `NICHT_ZUGEORDNET` | Entscheidung E‑i | **immer, auch bei null** |

**„Nicht zugeordnet" erscheint auch bei null**, und das ist eine Aussage über den **Katalog**: Null
heißt *alles kuratiert*, und `IBIS` liefert sie gerade. Wird die Zeile bei null ausgeblendet, ist
*vollständig gepflegt* nicht mehr von *diese Ansicht zeigt das nicht* zu unterscheiden.

**„Übrige" fehlt ohne Rang 11** — eine Null ist dort reines Rangartefakt und sagt nichts.

**Beide Restzeilen stehen immer unten**, unabhängig von ihrer Größe. Bei `IBIS` wäre „Übrige (40)"
mit 27,92 % sonst der größte Balken des Blocks und stünde auf Rang 1, als gäbe es einen Partner
dieses Namens (M98, Befund 21).

> Damit ist offener Punkt **62** geschlossen: Der Block verträgt fehlende Restzeilen — bei drei von
> vier in M98 gemessenen Mandanten gibt es nicht drei Zeilen —, und die Reihenfolge ist so gebaut,
> dass eine große Restzeile nicht nach vorn rutscht.

### Was das Backend nicht tut: beschriften

Die Antwort trägt **keinen** Anzeigetext für die Restzeilen. „nicht zugeordnet" und „Übrige (40)"
sind Beschriftungen; das Backend stellt fest, die Oberfläche beschriftet (Regel Q4). Die Texte
stehen bereits in `frontend/src/i18n/de.ts` und `en.ts`.

### Entscheidung E‑i, als ein Ausdruck

Zugeordnet ist ein Prozess nur, wenn **alle drei** Bedingungen halten; fällt eine, ist der Wert
*nicht zugeordnet*:

1. Es gibt eine Katalogzeile (der `LEFT JOIN` traf).
2. Sie ist `GEPFLEGT` — ein offener Regelvorschlag ist eine Vermutung und keine Zuordnung.
3. Das Feld ist gefüllt — *„gepflegt mit leerem Partner"* heißt **hingesehen, es gibt keinen** (E4)
   und fällt fachlich mit *nicht zugeordnet* zusammen.

**Der `pflegestatus`-Riegel steht auch in der Richtungssicht.** Heute ist er dort folgenlos — nach
der Kuratierung tragen alle Zeilen mit Richtung `GEPFLEGT` —, aber die Regel ist E‑i und nicht der
Zufall dieses Katalogstands.

> ### Zwei Fallen, und beide sind im Statement entschärft
>
> **`LEFT JOIN` und nie `JOIN`.** `WOC` hat keine einzige Katalogzeile; ein innerer Join verlöre
> seine vier Prozesse stillschweigend — und damit ausgerechnet die Zeilen, die als *nicht
> zugeordnet* erscheinen müssten.
>
> **Der `CASE` steht als *ein* Ausdruck in `SELECT`, `GROUP BY` und `ORDER BY`, ohne Alias.** Das
> ist Befund 11 der Vorrunde: MariaDB löst `GROUP BY` **zuerst gegen Tabellenspalten** auf und erst
> danach gegen Ausdrucksaliasse. Hieße der Alias `partner`, gruppierte die Datenbank still nach
> `c.partner` statt nach dem Ausdruck — bei `NEXANS` und `SUTTONS` fällt das nicht auf, bei `VOTG`
> zerfiel der Eimer in **acht** Zeilen. `DashboardStatementsTest` hält beides fest.

**`ORDER BY (schluessel IS NULL), summe DESC`** — *nicht zugeordnet* ist keine Rangposition und
fällt nie in „Übrige". Die Ränge 1…k gehören damit lückenlos den benannten Werten.

**Die Top‑10‑Grenze ist gewählt und nicht gemessen.** M98 hat sie für vier Mandanten durchgerechnet:
Top 10 trägt bei `NEXANS` 76,4 %, bei `VOTG` 76,5 %, bei `IBIS` 72,1 %. Sie steht als Konstante im
Code und nicht in der Konfiguration — ein Schalter dafür wäre eine Gestaltungsentscheidung, die
niemand getroffen hat.

> ### ⚠️ Korrektur vom 16.09.2026 — beide Sichten in jeder Antwort (E‑161)
>
> **Der erste Satz dieses Kapitels bleibt stehen und bleibt wahr:** ein Block, zwei Sichten über
> **dasselbe Statement**, nur mit einer anderen Katalogspalte. Geändert hat sich, **wie oft** es
> läuft: nicht mehr einmal für die angefragte Sicht, sondern **je Antwort zweimal**, einmal je Sicht.
> Der Umschalter wählt seither keine Antwort mehr, sondern die Hälfte einer Antwort, die schon da
> ist.
>
> **Alles darüber gilt je Sicht unverändert**, und das ist ausdrücklich so gebaut und getestet —
> nicht einmal für beide gerechnet:
>
> | Regel | je Sicht | belegt in |
> |---|---|---|
> | Zeilenarten `WERT` · `UEBRIGE` · `NICHT_ZUGEORDNET` | ja | `DashboardServiceTest.BeideSichten` |
> | Top‑10‑Grenze, „Übrige" nur bei eigenem Rang 11 | ja — die Partnersicht kann „Übrige" tragen und die Richtungssicht nicht, in derselben Antwort | ebenda, `restzeilen_je_sicht` |
> | „nicht zugeordnet" immer, auch bei null; beide Restzeilen unten | ja | ebenda und die Restzeilen-Fälle |
> | E‑i als ein Ausdruck, `pflegestatus`-Riegel auch in der Richtung | ja — das Statement ist Zeichen für Zeichen dasselbe, bis auf die Spalte | `DashboardStatementsTest.die_neun_statements_je_seite` |
> | `CASE` ohne Alias, `LEFT JOIN`, Mandantenkette als `EXISTS` | ja | ebenda |
>
> **Warum beide und nicht die gewählte:** Entscheidung **E‑161** (§9b). **Was es kostet:** der
> Bereichszugriff auf die Rollup-Ebene ein zweites Mal — gemessen als M178 (§8).

> ### ⚠️ Ergänzt am 18.09.2026 — die Verteilung ohne die Fehler des Rollups (E‑211)
>
> **Alles darüber gilt, und das Statement ist Zeichen für Zeichen dasselbe** — solange die
> Fehlerlesung ausfällt. Ist sie angewandt, läuft je Sicht `verteilungOhneFehler`: dieselbe Gestalt
> mit **einer** Bedingung mehr hinter der Mandantenkette, `NOT fehlerBedingung(message_status)`. Die
> Fehler des Fensters kommen dann aus der Lesung und gehen wie die Korrekturzeilen des Live-Rests
> über die Katalog-Nachlesung (E‑191) den Schlüsseln zu; die Korrekturzeilen gehen **ohne** ihre
> Fehlerzeilen hinein. So zählen Kachel und Sichten in beiden Zuständen dieselbe Zahl.
>
> **Der Plan ist Zeile für Zeile der von M178** — `message_status` steht im Primärschlüssel hinter
> Eimer und Prozess, ein `NOT (… LIKE … OR … = …)` ergibt keinen Bereich. Gemessen: 0,04 bis 3,69 ms
> teurer als die heutige Form derselben Sitzung, höchstens das 1,062-Fache von M178 (M188, Tor 2 in
> [`fehler-live.md`](fehler-live.md) §8). `DashboardPlanDbIT` hält den Plan fest,
> `DashboardStatementsTest` beide Fassungen wörtlich.

---

## 5. Läuft und Wartend — die zwei benannten Ausnahmen von L2

*Neu am 03.09.2026 (Schritt 10b‑4). Bis dahin stand hier die Kachel **Überfällig** als die erste
benannte Ausnahme; der alte Abschnitt steht als Korrekturblock am Ende dieses Kapitels.*

Zwei Kacheln, **beide live** über `Message`, **beide ohne Zeitfenster**, je **zwei Werte aus einem
Statement**:

```sql
SELECT COUNT(*) AS anzahl, MIN(m.MessageLastUpdate) AS aelteste
FROM GlassfishDB.Message m
WHERE m.MessageStatus = ?              -- 'RUNNING' bzw. 'SUSPENDED'
  AND EXISTS ( … Mandantenkette … );
```

| | |
|---|---|
| **`=` auf den Rohwert, nicht der Klassifizierer** | Die Einordnungen `LAEUFT` und `WARTEND` haben je genau **einen** Rohwert, und `MessageStatusIDX` trägt den Rohwert. Ein `IN` über eine einelementige Menge wäre derselbe Zugriff mit einer Unwahrheit darin |
| **Der Rohwert wird trotzdem gerufen** | `MessageStatusClassifier.einzigerRohwert(einordnung)`. Ein Literal `"SUSPENDED"` im Dashboard wäre dieselbe Zuordnung ein zweites Mal, und sie driftete beim nächsten Statuswert von der Liste weg. Die Methode **wirft**, sobald eine der beiden Einordnungen einen zweiten Rohwert bekäme |
| **Die Mandantenkette ist Bestandteil des Statements** (Regel M3) | als `EXISTS` und nicht als Join — `ProjectMandant` ist n:m, und ein Join vervielfachte Zeilen |
| **`aeltesteSekunden` rechnet das Backend** (E‑75) | gegen die **Anwendungsuhr** (Regel Z1). Bei `anzahl = 0` ist es `null` — ohne Zeile gibt es kein Alter, und eine `0` hieße „seit null Sekunden" |

### Warum das Alter der ältesten Zeile daneben steht (E‑75)

**Es hat zwei Aufgaben, und die zweite ist die wichtigere.**

**Erstens: Es ist die Grundlage eines Fensters, das die Oberfläche selbst wählen kann.** Eine Zahl
ohne Alter sagt „538 warten"; mit Alter sagt sie „538 warten, die älteste seit sieben Tagen". Das
ist der Unterschied zwischen einer Zahl und einer Auskunft, und die Kachel braucht dafür **keinen**
zweiten Aufruf — `MIN(MessageLastUpdate)` liest denselben Indexbereich wie `COUNT(*)`.

**Zweitens: Es ist die laufende Prüfung der Auskunft, auf der dieser ganze Schritt ruht.** Die Regel
sagt, `SUSPENDED`-Nachrichten lägen „höchstens rund eine Woche". **Das Feld zeigt bei jedem Aufruf,
ob das noch stimmt.** Steht dort eines Tages ein Alter von Monaten, ist die Auskunft widerlegt — und
zwar dort, wo jemand hinsieht, statt in einer Messung, die niemand wiederholt.

> **Das ist ausdrücklich keine Schwelle und keine Warnung.** Das Feld trägt eine Zahl und kein
> Urteil; ob sieben Tage viel sind, entscheidet niemand im Backend (Regel Q4). **Es macht die
> ungemessene Auskunft nur beobachtbar** — und das ist das Höchste, was ein Werkzeug ohne Schwelle
> für sie tun kann.

### Ohne Zeitfenster, und das ist durch Regel L9 gedeckt

Gefragt ist, was **jetzt** offen ist. Ein Zeitfenster schnitte gerade die **ältesten** Zeilen weg —
also die, um die es geht: Eine Nachricht, die seit sechs Tagen wartet, fiele aus einem
48‑Stunden‑Fenster heraus und ist trotzdem der Grund, warum es die Kachel gibt.

Es ist außerdem keine Aggregation über einen Bereich, sondern eine Zählung über die wenigen
Indexsätze, auf die `MessageStatusIDX` herunterführt — **538 im ganzen Bestand**.

### Warum sie nicht aus dem Rollup kommen können

**`message_rollup` trägt keine Statushistorie.** Der nächtliche Volllauf rechnet jeden Eimer aus dem
*heutigen* Zustand jeder Nachricht neu; eine Nachricht, die im März `SUSPENDED` war und im April
fertig wurde, hinterlässt im März **nichts**. Der Rollup ist nach jedem Volllauf eine Projektion des
Jetzt, gebucht nach letzter Änderung. Ausgeschrieben in [`rollup.md`](rollup.md) §7a.

**Das ist ein anderer Grund als bei *Überfällig*.** Dort hing die Kennzahl an einer **Frist**, die
zwischen zwei Läufen abläuft. Hier hängt sie an einem **flüchtigen Status**. Beide Male ist das
Ergebnis dasselbe — eine Live-Abfrage —, aber die Begründung ist es nicht, und sie ist einzeln
einzutragen (`PROJEKTBESCHREIBUNG.md` §8).

### 5a. Die Kachel *Wartend* erscheint strukturell (Entscheidung E‑74)

**Sie erscheint nur bei Mandanten, deren Abläufe überhaupt suspendieren.** Zeigt sie dann `0`, ist
das eine Auskunft und kein Rauschen.

```sql
SELECT EXISTS (
  SELECT 1
  FROM GlassfishDB.SOSAction sa
  JOIN GlassfishDB.SOS s ON s.SOSID = sa.SOSID
  WHERE sa.SOSActionServiceProperties LIKE '%SUSPEND%'
    AND EXISTS ( … Mandantenkette über s.ProcessID … )
) AS hat_wartende_ablaeufe;
```

**Die Beziehung ist vor dem Bau geprüft** (M142 a): `SOS` trägt eine `ProcessID`, sie ist
`NULL`-fähig, und **alle 1.818 `SOS`-Zeilen haben eine** — verteilt auf 1.502 der 1.503 Prozesse.
Die Kette ist damit dieselbe wie überall, nur mit `SOS.ProcessID` statt `Message.ProcessID`.

> ### Zwei Wege sind geprüft und beide verworfen
>
> **Nicht über die Zahl selbst.** „Kachel erscheint bei `anzahl > 0`" flackert: *heute wartet
> nichts* und *dieser Mandant wartet nie* sähen gleich aus, und ein Mandant mit nächtlichem
> Sammelversand hätte die Kachel tagsüber nicht. Das ist die bekannte Grenze 2 dieses Dokuments ein
> zweites Mal — **Abwesenheit ist der schwächste Kanal, den ein Zustand haben kann.**
>
> **Nicht über den Rollup.** Er trägt keine Statushistorie (siehe oben). Eine Frage nach *„hat der
> Mandant je gewartet"* ist dort nicht beantwortbar.

#### `SUSPEND` und nicht `WAITUNTIL` — und das ist gemessen, nicht gewählt

M29 (4) hat **beide** Marken bei **allen 538** wartenden Nachrichten gefunden; die Beobachtung am
Bestand entscheidet also nichts. **M144 entscheidet es, über die Stammdaten:**

| Marke | Mandanten mit `true` |
|---|---|
| **`SUSPEND`** | **`NEXANS` und `VOTG`** |
| `WAITUNTIL` | nur `NEXANS` |

**`WAITUNTIL` verlöre `VOTG`.** Dessen einzige `SUSPEND`-Zeile trägt die andere Marke nicht.
`SUSPEND` ist damit zugleich das Wort, das den Zustand benennt, **und** das treffsicherere — die
Entscheidung fällt nicht auf die Bedeutung, sondern auf die Messung.

#### Die Lücke, die dazugehört

M29 hat `MessageAction.SOSActionServiceProperties` gemessen — den **ausgeführten** Baustein. Diese
Abfrage liest `SOSAction.SOSActionServiceProperties` — den **geplanten**.

**M142 (c) hat die Übertragung nachgeprüft:** Bei **538 von 538** trägt auch der geplante Baustein
das Wort, und über beide Schritte aller 538 (1.076 Zeilen) gibt es **null Abweichungen** zwischen
ausgeführt und geplant.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* dass der geplante Baustein bei 538 von 538 wartenden Nachrichten `SUSPEND` und
> `WAITUNTIL` trägt, und dass ausgeführt und geplant über 1.076 Zeilen nirgends auseinanderfallen.
> *Behauptet wird:* Der geplante Ablauf ist ein tragfähiger Ersatz für den ausgeführten.
> **Die Lücke, und sie ist größer als „538":** Die 538 hängen an **einer** `SOSAction`-Zeile — ein
> Ablauf, eine `SOSActionID`, der Baustein `Send Message to Pool` (M29 4, M142 c). `n = 538` ist
> die Zahl der **Zeilen**, nicht die der **Fälle**; an Vielfalt liegt **eine** vor. Die Messung
> kann die Übertragung **widerlegen**; bestätigen kann sie sie nur für diese eine Gestalt.

#### Sie wird bei jedem Aufruf mitgelesen, nicht bedingt

Sonst hinge die Zahl der Statements am Mandanten und `DashboardStatementsTest` wäre nicht mehr
deterministisch. Die Entscheidung, ob die Kachel in der Antwort steht, fällt im Zusammenbau.

#### Warum ein `LIKE '%…%'` hier zulässig ist, wo M8 dafür 97,976 s gemessen hat

Jene Messung lief über **`MessageAction`** — 10,3 Millionen Zeilen, 3,0 GB. **`SOSAction` ist
Stammdaten: 3.944 Zeilen, 2,0 MiB** (M142 a, gezählt, nicht aus `information_schema`).

**Und der Plan ist die eigentliche Rechtfertigung:** Der Optimierer steigt über
`ProjectMandant_Mandant_idx` ein, also **beim Mandanten** — das `LIKE` läuft nur über dessen eigene
`SOSAction`-Zeilen und **nie über die Tabelle**.

```
pm  ref  ProjectMandant_Mandant_idx  rows 17  Using where; Using index
p   ref  Process_ProjectFK           rows  5  Using index
s   ref  SOS_ProcessFK               rows  1  Using index
sa  ref  PRIMARY                     rows  1  Using where
```

**Gemessen 1,24–1,40 ms** (M143). Das Abbruchkriterium des Auftrags lag bei **200 ms**.

### „Nicht ermittelbar" — und nur hier

Diese beiden Kacheln sind die **einzigen Felder der ganzen Antwort**, die *nicht ermittelbar*
zurückgeben dürfen. Sie sind der einzige Teil, der zur Laufzeit auf der **Produktion** live über
`Message` liest, wo `max_statement_time` nach zehn Sekunden abräumt. Der Rest kommt aus unserer
eigenen Tabelle. **Stirbt die Live-Abfrage, darf nicht die ganze Seite sterben.**

| | |
|---|---|
| **Kein allgemeiner Teilerfolg-Mechanismus** | Genau diese zwei Kacheln. Ein Dashboard, das jeden Block einzeln scheitern lassen kann, zeigt irgendwann eine Seite voller Lücken und nennt das eine Antwort |
| **Auch die Erscheinungsbedingung bekommt keinen** | Sie liest **Stammdaten** und ist damit dieselbe Art Zugriff wie die Mandantenkette in jedem anderen Statement. Ein dritter Block mit eigenem Ausfall wäre der Anfang genau dieser Seite |
| **Gefangen wird genau eine Ausnahme** | `DataAccessException` **mit** `SQLTimeoutException` als Ursache. Ein Syntaxfehler, eine abgerissene Verbindung oder ein fehlendes Recht bleiben technische Fehler mit `500`. **Ein pauschales `catch` machte aus jedem Bruch eine Beruhigung** |
| **Die beiden Kacheln fallen *nicht* zusammen** | Und das ist der Unterschied zur alten Kachel *Überfällig*: Dort waren „im Zeitraum" und „insgesamt" ein **Paar**, das man nebeneinander liest, und eine Zahl ohne die andere lud zu einer Rechnung ein, die nicht aufgeht. *Läuft* und *Wartend* sind **zwei verschiedene Auskünfte**. Fällt eine, steht die andere |
| **`ermittelbar: false` ist nicht `0`** | Null hieße „es läuft nichts". In einem Überwachungswerkzeug ist das die schlimmste falsche Antwort |

`DashboardZeitgrenzeTest` stellt beide Fälle her — den Abbruch an der Zeitgrenze und den
Syntaxfehler — und prüft, dass nur der erste geschluckt wird.

> ### ⚠️ Ergänzt am 18.09.2026 — zwei Rückfälle, die kein „nicht ermittelbar" sind
>
> **Die Tabelle darüber bleibt stehen, und ihr Satz *„Genau diese zwei Kacheln"* ist seit dem
> 17.09.2026 nicht mehr die ganze Wahrheit.** Zwei weitere Lesungen dieser Seite fangen ihren Ausfall
> ab — und zwar **jede `DataAccessException`**, nicht nur die Zeitgrenze:
>
> | Lesung | Fängt ab | Was dann in der Antwort steht | seit |
> |---|---|---|---|
> | der **Live-Rest** (`common/LiveRestService`) | jede `DataAccessException` der zwei Live-Lesungen | die Zahlen aus dem Rollup, `liveRest.zustand = AUSGESETZT`, ein Hinweis über den Kacheln | 17.09.2026, E‑185 — **hier bis heute nicht eingetragen** |
> | **Fehler live** (`common/FehlerLiveService`) | dasselbe, für die Fehlerlesung — „nicht weiter und nicht enger" als der Live-Rest | die Fehler aus dem Rollup, `fehlerLive.zustand = AUSGESETZT`, ein Hinweis über den Kacheln | 18.09.2026, E‑209 |
>
> **Beide sind kein allgemeiner Teilerfolg-Mechanismus im Sinne der Tabelle:** Es fehlt keine Zahl
> und kein Block. Die Seite rechnet wie vor dem jeweiligen Schritt und sagt, dass sie es tut.
> „Nicht ermittelbar" bleibt den zwei Kacheln vorbehalten, deren Zahl ohne ihre Lesung nicht existiert.

### Was diese beiden Kacheln **nicht** beantworten

**„Hängt hier etwas zu lange?"** Das war die Frage von *Überfällig*, und sie ist mit E‑71
unbeantwortet geblieben. *Läuft* und *Wartend* zählen einen **Zustand** und behaupten kein Problem;
das Alter der ältesten Zeile steht daneben, **ohne eine Schwelle**. Eine Schwelle zu erfinden
verbietet Regel Q4 — `MessageTimeout` ist es nachweislich nicht. **Offener Punkt 130.**

---

> ## ⚠️ Der Stand bis zum 03.09.2026 — die Kachel *Überfällig*
>
> **Er bleibt wortgleich stehen.** Ohne ihn wäre nicht mehr nachlesbar, dass die erste benannte
> Ausnahme von L2 einmal eine andere war, und woran sie gescheitert ist.
>
> > ### 5. Überfällig — die erste benannte Ausnahme von L2
> >
> > Zwei Zahlen, **beide live** über `Message`, beide über
> > `MessageStatusClassifier.ueberfaelligBedingung` (gerufen, nicht nachgebaut), `jetzt` aus der
> > **Anwendungsuhr** (Regel Z1):
> >
> > - **im Fenster** (E‑h) — dieselbe Zahl, die der Klick in die Liste liefert
> > - **insgesamt** — ohne Zeitfenster
> >
> > **Das Wort „ausschließlich" in L2 bleibt stehen, und daneben steht diese Ausnahme** (E‑c vom
> > 24.08.2026). Sie ist einzeln begründet und einzeln gemessen.
> >
> > **Die zweite Zahl hat kein Zeitfenster, und das ist durch Regel L9 gedeckt:** Gefragt ist genau,
> > was *außerhalb* des gezeigten Zeitraums hängt — ein Fenster schnitte die Zeilen weg, um die es
> > geht. Sie ist dabei die **billigere** von beiden (M90, Befund 14).
> >
> > **In der Anzeige trägt „überfällig" keine Farbe von *Fehler*.** Rot gehört ausschließlich der
> > Kategorie *Fehler*; die eigene Farbrolle (orange) ist in
> > [`visuelles-konzept.md`](visuelles-konzept.md) §7a entschieden.
>
> **Was daran gefallen ist und warum:** Die Problemkategorie *Überfällig* ist am 03.09.2026 durch
> eine fachliche Auskunft des Auftraggebers widerlegt (E‑71). Sie markierte auf der Testkopie 538
> Zeilen — **538 `SUSPENDED`, 0 `RUNNING`, also 538 Fehlalarme und kein Treffer.** Vollständig samt
> Herkunftsvermerk in `PROJEKTBESCHREIBUNG.md` §4.2 Punkt 2 und
> [`message-status.md`](message-status.md).
>
> **Was daran unberührt bleibt, und es ist der größere Teil:** die Bauform. Live über `Message`,
> ohne Zeitfenster nach L9, mit `EXISTS` als Mandantenkette, mit dem engen `catch` auf die
> Zeitgrenze und mit `ermittelbar` als eigenem Feld. **Die beiden neuen Kacheln erben sie
> vollständig** — gefallen ist die Kennzahl, nicht ihr Bau.
>
> **Die Berichtigung vom 31.08.2026 zur Farbrolle** — dass sie am Tag jenes Satzes noch nicht
> entschieden war — steht weiterhin in [`visuelles-konzept.md`](visuelles-konzept.md) §7a und ist
> von E‑71 nicht berührt. Die Rolle `--ueberfaellig` bleibt dort bestehen, **ohne Verbraucher**
> (E‑77).

---

## 6. Der Leerzustand

**Ein Feld, `leer`, und es unterscheidet nicht.** Die Oberfläche zeigt einen Satz, und der ist in
beiden Fällen wahr:

- im Zeitraum ist nichts passiert
- dieser Mandant hat überhaupt keine Daten

> ### ⚠️ Bekannte Grenze 2: Ein stiller Sonntag und `EDITIONLINGERI` sehen gleich aus
>
> Das ist die Folge, und sie ist gewollt. Die Alternative wäre eine zweite Abfrage gewesen — „hat
> dieser Mandant überhaupt jemals Verkehr gehabt?" —, um einen Satz anders zu formulieren, den
> niemand zweimal liest.

**Auch im Leerzustand nennt die Antwort ein Paar**, nämlich das erste der Reihe. Die Oberfläche
braucht eines zum Hervorheben und für die URL; ein Mandant ohne Daten sieht damit dieselbe Auswahl
wie jeder andere und darf durchschalten — er findet überall denselben Satz.

**Und auch im Leerzustand sagt der Katalog etwas:** Die Verteilung trägt genau eine Zeile,
`NICHT_ZUGEORDNET` mit null.

---

## 7. Die Statements (Regel L7)

Sieben je Seite, wenn `zeitraum` genannt ist; acht bis neun, wenn der Endpunkt selbst wählt (eine
bis drei Belegungsproben). Alle laufen über **`glassfishDsl`**, den Lese-Pool.

> **Berichtigt 10.09.2026: es sind acht je Seite.** Der Absatz oben bleibt stehen und war bis
> Schritt 10c richtig; hinzugekommen ist das Statement der **Dienstlampen** über `Service` — ein
> Vollzugriff über 20 Zeilen, gemessen mit **1,660 ms** (M175, [`dienste.md`](dienste.md) §11). Es
> läuft ebenfalls über `glassfishDsl`, trägt aber als einziges **keinen Mandantenfilter**: `Service`
> kennt keinen Mandanten, und ein Filter, der nichts filtert, sähe von außen wie Mandantentrennung
> aus. Das ist die **dritte benannte Ausnahme von Regel M2**
> ([`mandantentrennung.md`](mandantentrennung.md) §4).
>
> **Die Prüfziele** (`SELECT DISTINCT ServiceDefaultFileStore …`, 2,091 ms) sind **kein** Statement
> der Seite: Sie werden im Takt der Ablagenprüfung gelesen und nicht in der Anfrage.

> **Berichtigt 16.09.2026: es sind neun je Seite** (E‑161). Hinzugekommen ist das
> Verteilungsstatement **in der Richtungsform** — dasselbe Statement wie die Partnerform, nur mit
> `process_catalog.richtung` im Ausdruck. Mit genanntem `zeitraum` neun, ohne ihn zehn bis zwölf.
> Es läuft über `glassfishDsl` und trägt die Mandantenkette als `EXISTS` wie jedes andere. **Sein
> Plan ist Zeile für Zeile der Plan der Partnerform** (M178, §8), und `DashboardPlanDbIT` hält das
> fest.

> **Berichtigt 17.09.2026: es sind zehn — und bei angewandtem Live-Rest zwölf oder dreizehn** (Teil B,
> [`live-rest.md`](live-rest.md) §9b). Das zehnte ist der **Wasserstand** (`common/WasserstandRepository`,
> `MAX(fenster_bis)` über `rollup_lauf`) — die Seite fragt ihn bei jedem Aufruf. Bei `ANGEWANDT`
> kommen die zwei Live-Lesungen des Bausteins dazu (die Rollupzeilen des Live-Bereichs, die Zählung
> aus `Message` mit der Stundenbildung des Jobs; beide mit Mandantenkette, gemessen in M185), und
> **nur wenn im Fenster etwas zu verrechnen ist**, die **Katalog-Nachlesung** für die Prozesse der
> Korrekturzeilen: derselbe `CASE` wie im Verteilungsstatement, `IN` über den Primärschlüssel, die
> Kette als `EXISTS`, **kein `GROUP BY`** — es ist nicht das zusammengelegte Verteilungsstatement, das
> `kein_zusammengelegtes_verteilungsstatement` ausschließt; der Test ist entsprechend verfeinert
> (keine **gruppierende** Abfrage liest beide Spalten) und läuft über beide Seiten. Mit genanntem
> `zeitraum` also zehn bis dreizehn, ohne ihn elf bis sechzehn. Alle laufen über `glassfishDsl`;
> `DashboardStatementsTest.LiveRest` benennt sie einzeln, die Nachlesung wörtlich. **Der Block
> *Stand* liest `rollup_lauf` weiterhin selbst** — es sind seither zwei Statements auf dieser
> Tabelle je Seite (Punkt 187 in `live-rest.md`: so lassen).

> **Berichtigt 18.09.2026: es sind elf bis vierzehn** (Fehler live, [`fehler-live.md`](fehler-live.md)
> §5). Das neue ist die **Fehlerlesung** an zweiter Stelle, vor den zwei Verteilungsstatements; alle
> folgenden rücken um eins. Bei angewandter Lesung laufen die zwei Verteilungsstatements in der Form
> ohne Fehler, die Nachlesung läuft, sobald Block 5 Korrekturzeilen ohne Fehler oder Fehlerzeilen
> zuzurechnen hat. Mit genanntem `zeitraum` also elf bis vierzehn, ohne ihn zwölf bis siebzehn.
> `DashboardStatementsTest.FehlerLive` benennt jede Lage als Folge von Namen, die Lesung und beide
> Verteilungsfassungen wörtlich. **Die Lesung trägt keinen Indexhinweis** — sie steigt trotzdem über
> `MessageStatusIDX` ein (M188), und `DashboardPlanDbIT` hält das fest.

**Der Lese-Kontext und nicht `monitorDsl`** — die Aufteilung ist *lesen gegen schreiben* und nicht
*Quellschema gegen eigenes Schema*: Jede Abfrage hier joint `overlord_monitor.message_rollup*` gegen
`GlassfishDB.Process` und braucht dafür **eine einzige Verbindung**.

### Die Mandantenkette ist ein `EXISTS` und darf kein `JOIN` sein

`ProjectMandant` ist **n:m**. Ein `JOIN` vervielfachte jede Rollupzeile, sobald ein Projekt an
mehreren Mandanten hängt — und damit **die Summe**. Der Fehler wäre still: Die Zahlen sähen plausibel
aus und wären zu hoch.

**`Project` steht nicht in der Kette.** `Process → ProjectMandant` über `Process.ProjectID` liefert
dieselbe Menge; der Umweg über `Project` ist entbehrlich (Befund 48). **Die Messskripte von M94, M98
und M107 fahren die lange Fassung, der Anwendungscode die kurze** — der Unterschied ist in den
Laufzeiten unten enthalten und nicht herausgerechnet.

### Drei Ebenen, drei Statements

Die Ebenen ließen sich mit einem `CAST` auf einen gemeinsamen Schlüsseltyp zusammenfassen; das
kostete eine Funktion um die Schlüsselspalte und damit den Bereichszugriff. `DashboardStatementsTest`
hält fest, dass **um den Eimerschlüssel keine Funktion steht**.

### 7a. ⚠️ Der Befund: „Zuletzt aufgefallen" braucht zwei Statements

**Der erste Bau stellte Fehler und Überfällige mit `OR` in ein Statement.** Er lieferte das Richtige
und war falsch gebaut — und der Plan sagt warum:

| | mit `OR`, ein Statement | gebaut: zwei Statements **und** ein Indexhinweis |
|---|---|---|
| Treiberindex | **`MessageLastUpdateIDX`** | **`MessageStatusIDX`** |
| gelesene Zeilen, 48 h | 23.126 | 6.257 (Fehler) bzw. 539 (überfällig) |
| gelesene Zeilen, 30 Tage | 209.408 | dieselben |
| gelesene Zeilen, 12 Monate | **2.705.843** | dieselben |
| `SUTTONS`, 48 h, ganze Seite | **121,3 ms** | **72,0 ms** |
| `SUTTONS`, 12 Monate, ganze Seite | **2.585,7 ms** | **127,0 ms** |
| `VOTG`, 12 Monate | **`500`** — Abbruch an `max_statement_time` | läuft |

> ### ⚠️ Berichtigt am 18.09.2026 — die Zeile „gelesene Zeilen" mischt drei Arten von Zahlen
>
> **Die Tabelle darüber bleibt stehen.** Am Messprotokoll nachgesehen
> ([`fehler-live.md`](fehler-live.md) §8 und §5b), und berichtigt ist nur, was dort belegt ist:
>
> | Zahl | Was sie ist | Belegt in |
> |---|---|---|
> | **6.257** (Fehler) | **die Schätzung des `EXPLAIN`** (`rows`) für den Statusbereich — **keine gelesene Menge**. Gelesen werden **3.412 Indexsätze** (`Handler_read_next`, gemessen in M188 und M189), die Fehlerzeilen des ganzen Bestands, in jedem Fenster dieselben | M188 Tor 1 (zwölf Lagen), M189 Tor 1 (zwölf Fenster) |
> | **539** (überfällig) | ebenfalls die Schätzung des `EXPLAIN` — die Hälfte ist seit E‑71 entfallen und nicht nachgemessen | §8, Die Pläne (`rows = 539`) |
> | **209.408** (30 Tage), **2.705.843** (12 Monate) | **gezählt**: `COUNT(*)` der Zeilen von `Message` im Fenster über den ganzen Bestand, alle Mandanten — die Zeilen, die ein Bereich über den Zeitindex überstreicht. Keine Schätzung des `EXPLAIN`; dass die Fassung mit `OR` genau sie gelesen hat, folgt aus dem Plan und ist nicht mit Handler-Zählern gemessen | [`messungen-schritt10.md`](messungen-schritt10.md) (Monatsscheibe 2025‑12, Jahresscheibe 2025), [`messungen-schritt10b.md`](messungen-schritt10b.md) (P2, P3) |
> | **23.126** (48 h) | **in keinem Protokoll des Repositorys belegt** — gesucht in `docs/` und `scripts/` samt der nicht eingecheckten Rohausgaben. Ob Schätzung oder Zählung, ist nicht nachzuvollziehen; die Zahl bleibt stehen und ist nicht berichtigt | — |
>
> **Der Befund des Abschnitts ändert sich dadurch nicht:** Der Zeitindex überstreicht das Fenster,
> der Statusindex die Fehlerzeilen des Bestands — gemessen 3.412 Indexsätze statt der geschätzten
> 6.257.

Der Abbruch im Wortlaut, aus dem Messlauf vom 31.08.2026:

```
org.jooq.exception.DataAccessException: SQL [select … from `GlassfishDB`.`Message`
  left outer join `GlassfishDB`.`SOS` … where ((`MessageStatus` like 'ERROR\_%' escape '\'
  or `MessageStatus` = 'COMMIT_REJECTED') and `MessageLastUpdate` >= '2025-01-01 00:00:00'
  and `MessageLastUpdate` < '2026-01-01 00:00:00' and exists (… `MandantID` = 'VOTG'))
  order by `MessageLastUpdate` desc, `MessageID` desc fetch next 10 rows only];
(conn=98969) Query execution was interrupted (max_statement_time exceeded)
```

**Der Grund ist die Deckelung.** `ORDER BY … LIMIT 10` ist nur billig, wenn die zehn Zeilen früh
gefunden werden. Ein Mandant **ohne** Fehler im Fenster zwingt die Datenbank, den ganzen Bereich zu
durchsuchen, bevor sie „nichts" sagen darf — **gerade der gute Fall ist der teure**.

> ### ⚠️ Korrektur vom 03.09.2026 — die Begründung der Disjunktheit ist gegenstandslos
>
> **Der Abschnitt darunter bleibt vollständig stehen.** Sein Befund gilt unverändert: Mit einem
> gemeinsamen `OR` steigt MariaDB über den Zeitindex ein, und der Indexhinweis dreht den Plan um.
> **Gegenstandslos ist genau ein Argument** — das der Disjunktheit.
>
> Es lautete: *Aus zweimal zehn neuesten Zeilen sind die zehn neuesten dieselben wie aus einer
> gemeinsamen Abfrage, weil die beiden Mengen disjunkt sind (Fehler ist Endstatus, überfällig setzt
> das Gegenteil voraus).* **Das war richtig und wird nicht mehr gebraucht:** Mit E‑71 ist die
> Überfälligkeitshälfte entfallen, es gibt nur noch **eine** Menge, keine Zusammenführung und keine
> Nachsortierung.
>
> **Der Indexhinweis bleibt, und seine Messung auch.** Er hing nie an der zweiten Hälfte, sondern an
> der Deckelung: `ORDER BY … LIMIT 10` ist nur billig, wenn die zehn Zeilen früh gefunden werden.
> Das gilt für die Fehlerhälfte allein genauso.
>
> **Gemessen nach dem Umbau** (M145): Der Block kostet **21,9 bis 26,1 ms** über alle sechs
> Kombinationen — vorher 29,7 bis 34,1 ms für **beide** Hälften zusammen. Er bleibt von der
> Fensterbreite unabhängig, und das war der Zweck.

> ### ⚠️ Fortschreibung vom 04.09.2026 — der Block trägt **Prozesse** (E‑90)
>
> **Der Abschnitt darunter bleibt vollständig stehen.** Der Indexhinweis, sein Befund und seine
> Messung gelten unverändert; geändert hat sich, **was eine Zeile ist**.
>
> #### Der Befund: Er listete Nachrichten, wo er Prozesse listen sollte
>
> Am laufenden System zeigte der Block zehn Zeilen mit **demselben Zeitstempel und demselben
> Ablauf**. Der Grund steht in den Daten und nicht im Bau — und er ist erst mit M146 gemessen
> worden:
>
> | über den **gesamten** Bestand | Fehlerzeilen | betroffene **Prozesse** |
> |---|---:|---:|
> | `NEXANS` | 3.300 | **3** |
> | `SUTTONS` | 103 | **1** |
> | `VOTG` | 8 | **1** |
>
> Bei `NEXANS` über 48 Stunden stammen **49 der 50 Fehler aus einem einzigen Prozess**. Ein Prozess
> füllte die Liste allein, und **keine Zeile trug eine eigene Auskunft**.
>
> #### Was jetzt dasteht
>
> Eine Zeile **je Prozess**, mit **Anzahl** und **jüngstem Zeitpunkt**, weiterhin höchstens zehn,
> sortiert nach dem jüngsten Zeitpunkt. Aus zehn gleichen Zeilen werden zwei verschiedene: *49 ×
> BMW LAB (VDA)* und *1 × BMW Global Invoice (EDIFACT)*.
>
> > **Es sind selten zehn, und das ist die Auskunft und kein Mangel.** Auf dieser Testkopie zeigt
> > der Block **null bis drei** Zeilen. Er sagt damit etwas, das die zehn gleichen Zeilen davor
> > verschwiegen haben: *Es ist immer derselbe Prozess.* Der Deckel von zehn bleibt, er greift auf
> > diesen Daten nur nicht.
>
> #### Der Name kommt aus `Process.ProcessName`
>
> **Das folgt aus der Verdichtung und ist keine Geschmacksfrage.** Das Verhältnis Process zu SOS ist
> meist 1:1, gelegentlich 1:n ([`datenmodell.md`](datenmodell.md)) — eine Gruppe je Prozess kann
> damit *mehrere* Ablaufnamen enthalten, und einen davon zu wählen hieße raten (Regel Q4).
> `ProcessName` gehört dem Prozess allein, ist in Klartext gepflegt und ist derselbe Anzeigename,
> den Prozesskatalog und Prozessansicht tragen. Der `LEFT JOIN` bleibt ein `LEFT`: Ein Prozess ohne
> gepflegten Namen fällt nicht aus der Liste, er kommt ohne Namen.
>
> **`ProcessName` steht in der `GROUP BY`**, obwohl es vom Primärschlüssel funktional abhängt. Das
> ändert die Gruppen nicht und macht die Abfrage unabhängig davon, wie streng `only_full_group_by`
> auf der Instanz steht. **Gruppiert wird nach dem Schlüssel, nicht nach dem Namen** — zwei Prozesse
> mit gleichem Namen bleiben zwei Zeilen.
>
> #### Der Indexhinweis: die Sorge war berechtigt und hat sich nicht bestätigt
>
> Der Indexhinweis wirkte, weil er verbot, den Zeitindex **zur Sortierung** zu nehmen. Über einer
> Gruppierung gibt es keine freie Sortierung mehr — der Hinweis könnte ins Leere greifen, während
> der Optimierer den Zeitindex für den **Bereich** wählt. **Vier Fassungen gemessen** (M146): ohne
> Hinweis, `IGNORE INDEX FOR ORDER BY`, volles `IGNORE INDEX`, `FORCE INDEX (MessageStatusIDX)` —
> **alle vier steigen über `MessageStatusIDX` ein**, alle vier lesen dieselben 6.257 Zeilen, alle
> vier liegen bei 20 ms.
>
> > *Berichtigt am 18.09.2026:* **6.257 ist die Schätzung des `EXPLAIN`** und keine gelesene Menge.
> > Gelesen werden **3.412 Indexsätze** — gemessen mit den Handler-Zählern in M188 und M189
> > ([`fehler-live.md`](fehler-live.md) §8, §5b), die Fehlerzeilen des ganzen Bestands. „Dieselben
> > Zeilen" trifft zu, nur ist die Zahl eine andere; der Satz darüber bleibt stehen.
>
> **Der Hinweis bleibt trotzdem — als Riegel und nicht als Wirkung.** Er kostet gemessen nichts,
> `DashboardPlanDbIT` hält fest, dass der Zeitindex in keiner Planzeile steht, und die Wette auf die
> heutige Statistik ist seit M108 als offener Punkt 82 benannt. Ihn zu ziehen wäre eine eigene
> Entscheidung mit einer eigenen Messung; **diese Runde hat sie nicht getroffen.**
>
> **`Using temporary; Using filesort` steht jetzt im Plan, und es ist nicht das, wovor dieser
> Abschnitt warnt.** Sortiert und gruppiert wird die *Ergebnismenge* — höchstens ein paar hundert
> Zeilen —, nicht der Statusbereich; Treiberindex und gelesene Zeilenzahl sind Ziffer für Ziffer die
> von M108.
>
> #### Der Verweis führt in die Liste
>
> Die Zeile trägt einen Prozess mit *n* Nachrichten; eine davon herauszugreifen wäre eine
> Behauptung, die sie nicht macht. Das Ziel ist `/nachrichten?status=FEHLER&prozess=…` mit dem
> Fenster der Antwort — **genau die Menge, die die Zahl daneben nennt**. Am laufenden System
> nachgesehen: Der Klick auf *49* zeigt 49 Zeilen.
>
> **Damit ist offener Punkt 136 gegenstandslos** statt erledigt. Er verlangte den Rohstatus je Zeile
> zurück; eine Zeile, die einen ganzen Prozess zusammenfasst, hat keinen — sie kann zwanzig
> verschiedene enthalten. Der Rohstatus steht in der Liste, einen Klick entfernt und dort
> vollständig.

> ### Ergänzt am 18.09.2026 — Block 6 bleibt, und die Kachel zählt jetzt dieselbe Menge
>
> **Block 6 ist unverändert** — Statement, Indexhinweis, Deckelung. Neu liest die Fehlerlesung
> ([`fehler-live.md`](fehler-live.md)) **dieselbe Menge**: denselben Statusbereich, dasselbe Fenster,
> dieselbe Kette, nur je Stunde statt je Prozess und ohne Deckelung. **Deshalb ist die Kachel *Fehler*
> jetzt die Summe über diesen Block, solange höchstens zehn Prozesse betroffen sind** —
> `DashboardFehlerLiveDbIT.kachel_fehler_ist_zuletzt_aufgefallen` hält es fest. Bis dahin konnten
> beide zwischen zwei Volllaufen auseinanderlaufen: der Block live, die Kachel aus dem Rollup — genau
> der Fall aus der Produktion. **Die Menge wird damit zweimal gelesen**; eine Zusammenlegung ist nicht
> gebaut (Punkt 211 dort).

### Zwei Änderungen, und die erste allein genügte nicht

**1. Je Merkmal ein Statement.** Zusammengeführt und gedeckelt wird in Java; aus zweimal zehn
neuesten Zeilen sind die zehn neuesten dieselben wie aus einer gemeinsamen Abfrage, weil die beiden
Mengen **disjunkt** sind (Fehler ist Endstatus, überfällig setzt das Gegenteil voraus).

**Das hat den Plan aber nicht gedreht.** Auch die getrennte Fehlerabfrage stieg weiterhin über den
Zeitindex ein: Er liefert die Sortierung gratis, und das ist dem Optimierer mehr wert als der
kleinere Bereich.

**2. `IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)`** — der **einzige Indexhinweis dieses
Projekts**. Er verbietet genau eines: den Zeitindex *zur Sortierung* zu verwenden. Erst damit steigt
die Abfrage über `MessageStatusIDX` ein, und der Aufwand hängt an der Zahl der **auffälligen** Zeilen
im Bestand statt an der Breite des Fensters — 822 Fehlerzeilen und 538 offene.

| Fassung | `NEXANS` | `SUTTONS` | `VOTG` |
|---|---|---|---|
| ohne Hinweis, 48 h / 30 T / 12 M | 3,4 / 2,5 / 1,6 ms | 63,2 / **1.092,2** / **2.174,4** ms | 65,0 / **1.065,4** / *Abbruch* |
| `IGNORE INDEX FOR ORDER BY` | 25,1 / 24,0 / 25,0 ms | 23,1 / 24,1 / 24,4 ms | 23,3 / 22,8 / — |
| `FORCE INDEX (MessageStatusIDX)` | 24,3 / 24,1 / 24,1 ms | 23,9 / 24,0 / 23,9 ms | 23,1 / 22,6 / — |

*(nur die Fehlerhälfte; die Überfälligkeitshälfte liegt in allen drei Fassungen bei 4,4 bis 7,1 ms,
weil sie ohnehin über den Statusindex fährt.)*

**`IGNORE` und `FORCE` sind gleich schnell — genommen ist der schwächere Eingriff.** Er nimmt dem
Optimierer eine Möglichkeit und lässt ihm die Wahl des Zugriffspfads; `FORCE` schriebe den Pfad fest.

> ### Warum überhaupt ein Hinweis, wo `STRAIGHT_JOIN` ausgeschlossen ist
>
> Das eine ist ein Verbot der **Join-Reihenfolge** und war in M42 um Faktor 219 bis 1094 schlechter.
> Dies hier betrifft **einen Index und eine Verwendung davon**. Und es ist gemessen, in drei
> Fassungen und für drei Mandanten über alle drei Fensterbreiten — genau das, was offener Punkt 57
> für Index-Hinweise als ungemessen ausweist.
>
> **Der Preis ist benannt:** Im guten Fall — `NEXANS`, Fehler direkt am Fensterrand — kostet der
> Hinweis das Sieben- bis Fünfzehnfache. **Getauscht wird Schwankung gegen Verlässlichkeit:**
> konstante 24 ms bei einem Budget von 500 ms gegen einen Wert zwischen 1,6 ms und einem Abbruch, je
> nachdem, ob der Mandant gerade Fehler hat. Dass die Wette auf die heutige Statistik gesetzt ist,
> steht als offener Punkt 82.

`DashboardPlanDbIT` ist der Wächter: Er verlangt für **beide** Hälften und **alle drei** Paare den
Statusindex und prüft als Gegenprobe, dass `MessageLastUpdateIDX` in keiner Planzeile des Blocks
steht.

### 7b. Warum nicht das bestehende Listen-Repository

Der Auftrag nennt als Quelle für Block 6 *„das bestehende Listen-Repository"*. **Das geht nicht, und
zwar aus zwei Gründen — der erste allein genügte schon:**

1. ~~**Die Liste kann diese Frage gar nicht beantworten.**~~ **Entfallen am 03.09.2026 (E‑71):**
   Den Parameter `ueberfaellig` gibt es nicht mehr, und mit ihm nicht die `400`. **Der zweite Grund
   trägt allein — und hat immer allein getragen.** Der Satz bleibt im Wortlaut stehen: Dort sind
   `status=FEHLER` und
   `ueberfaellig=true` ausdrücklich **unvereinbar** und ergeben `400`
   (`ueberfaellig-und-status-unvereinbar`): Überfällig setzt `WARTEND` oder `LAEUFT` voraus, Fehler
   ist ein Endstatus. Über die Liste bräuchte der Block **zwei** Aufrufe, ein Zusammenführen und
   eine Neusortierung — also genau das, was jetzt dasteht, nur mit zwei HTTP-Schichten dazwischen.
2. **Fachpakete kennen einander nicht.** `dashboard` darf nicht aus `message` importieren
   (`PaketstrukturTest.fachpakete_kennen_einander_nicht`); braucht ein zweites Fachpaket einen Typ,
   **wandert der Typ nach `common`**.

**Wiederverwendet ist damit genau das, was driften könnte:** die Fehlerbedingung, aus
`common/MessageStatusClassifier`. **Nachgebaut ist nichts.** *(Bis zum 03.09.2026 stand hier „die
Fehlerbedingung und die Überfälligkeitsbedingung"; die zweite ist mit E‑71 entfallen.)* Die
Mandantenkette schreibt ohnehin jedes Fachpaket selbst — sie kann nicht nach `common` wandern, weil
dort keine `jooq.glassfish`-Typen stehen dürfen.

~~**Die Fensterverengung fällt dabei nicht weg, sie greift ohnehin nicht:** Für `ueberfaellig` ist sie
abgeschaltet, weil der Rollup keine Frist kennt.~~ **Gegenstandslos seit dem 03.09.2026:** Das
Merkmal `UEBERFAELLIG` ist mit E‑71 aus `Abfragemerkmal` entfallen. Es war das **einzige**, das der
Rollup aus einem Grund nicht mittragen konnte, der am *Bestand* lag; übrig bleibt `SUCHBEGRIFF`, und
dessen Grund liegt am *Schema* ([`nachrichtenliste.md`](nachrichtenliste.md) §5d).

> **Aus demselben Grund ist `Pflegestatus` von `catalog` nach `common` gewandert:** Der
> Verteilungsblock braucht `GEPFLEGT`. Die Alternative wäre ein Literal `"GEPFLEGT"` im Dashboard
> gewesen — dieselbe Bedingung an zwei Stellen, und die driftet.

---

## 8. Die Messung — M108 *(31.08.2026)*, überholt durch M145 *(03.09.2026)*

> **Ergänzt 17.09.2026 — M186, die Seite mit Live-Rest** (Teil B). Vorregistrierung, Tor und Ergebnis
> stehen in [`live-rest.md`](live-rest.md) §9b; das Tor ist das Seitenbudget dieses Kapitels (500 ms
> je Lage), auf Entscheidung des Auftraggebers auch für den dichtesten Vierstundenbereich.

> ### ⚠️ M108 ist für drei Zeilen überholt und für den Rest gültig
>
> **Die ganze Messung bleibt stehen.** Überholt sind genau die Zeilen, deren Statements es nicht
> mehr gibt: *Überfällig — im Fenster*, *Überfällig — insgesamt* und *Zuletzt aufgefallen (beide
> Statements)*. **M145 misst, was an ihrer Stelle steht**, und dazu die ganze Landingpage neu.
>
> #### Die neuen Statements (M143)
>
> | Statement | `NEXANS` | `SUTTONS` | Plan |
> |---|---:|---:|---|
> | Kachel *Läuft* | **1,09–1,25 ms** | **0,94–1,04 ms** | `Message`, `ref` über `MessageStatusIDX`, `key_len 123`, `rows = 1` |
> | Kachel *Wartend* | **4,40–4,66 ms** | **3,34–3,55 ms** | dasselbe, `rows = 538` |
> | Erscheinungsbedingung | **1,24–1,40 ms** | **1,40–1,42 ms** | Einstieg über `ProjectMandant_Mandant_idx`, dann `Process_ProjectFK`, `SOS_ProcessFK`, `SOSAction.PRIMARY` — alle `ref` |
>
> **`ref` statt `range`, und das ist der vorregistrierte Befund.** Erwartet war `range` über
> `MessageStatusIDX` wie bei der alten Kachel. Herausgekommen ist **`ref` über denselben Index** —
> die erwartbare Folge davon, dass das neue Statement mit `=` auf **einen** Rohwert vergleicht, wo
> das alte ein `IN` über zwei trug. **Derselbe Index, engerer Zugriff, kein Rückschritt.**
> `DashboardPlanDbIT` schreibt deshalb den **Index** fest und nicht die Zugriffsart.
>
> #### Die ganze Landingpage (M145) — und sie ist für einen Mandanten teurer geworden
>
> | Paar | `NEXANS` M108 → M145 | `SUTTONS` M108 → M145 |
> |---|---:|---:|
> | 48 h | 62,227 → **49,5–57,7 ms** | 72,007 → **49,5–53,4 ms** |
> | 30 Tage | 152,814 → **149,4–149,8 ms** | 109,829 → **116,0–119,5 ms** |
> | 12 Monate | 199,030 → **194,2–195,0 ms** | 127,038 → **139,7–144,6 ms** |
> | ohne `zeitraum` | 64,706 → **59,2–59,9 ms** | 77,957 → **55,1–59,9 ms** |
>
> **Das Budget von 500 ms ist in jeder Kombination weit unterschritten** — die teuerste überhaupt
> mögliche liegt bei 195 ms.
>
> ##### ⚠️ Die vorregistrierte Erwartung trifft nicht zu, und zwar aus zwei Gründen
>
> Erwartet war eine **Verbesserung** für beide Mandanten. Eingetreten ist sie für `NEXANS`
> (−4 ms bei zwölf Monaten) und **nicht** für `SUTTONS` (+13 bis +18 ms). Zwei Dinge sind
> auseinanderzuhalten:
>
> **1. Die Rechnung der Vorregistrierung war falsch, und der Fehler ist meiner.** Dort stand: *„es
> fällt weg: die Überfälligkeitshälfte von ‚Zuletzt aufgefallen' — **29 bis 34 ms** (M108)."* Die
> 29–34 ms sind in M108 aber die Kosten **beider Hälften zusammen** — die Zeile heißt dort
> ausdrücklich *„Zuletzt aufgefallen (beide Statements)"*. **Die Hälfte, die wegfällt, ist rund
> 8–10 ms wert, nicht 29–34.** Der eigene Beitrag dieses Schritts ist damit:
>
> | | |
> |---|---:|
> | fällt weg: zwei Überfällig-Statements | **−9,7 ms** (`SUTTONS`, M108) |
> | fällt weg: die zweite Hälfte von Block 6 | **−8 bis −10 ms** |
> | kommt hinzu: drei neue Statements | **+5,8 ms** (M143) |
> | **netto** | **rund −13 ms** |
>
> **2. Die Mehrkosten bei `SUTTONS` liegen in einem Block, den dieser Schritt nicht anfasst.**
> Aufgeschlüsselt:
>
> | Block, `SUTTONS` 12 Monate | M108 | M145 |
> |---|---:|---:|
> | Verlauf | 39,687 ms | **39,650 ms** |
> | **Verteilung** | 40,223 ms | **67,253 ms** |
> | Zuletzt aufgefallen | 34,084 ms (beide) | **24,978 ms** (eine) |
>
> **Die Verteilung ist um 27 ms teurer geworden, und an ihr ist keine Zeile geändert worden.**
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* die Laufzeit je Block, zweimal, ein Aufwärmlauf und die beste von fünf; dazu
> > der Bestand — `message_rollup` trägt unverändert **335.610** Zeilen, der letzte Volllauf
> > stammt vom **31.08.2026**, also demselben Tag wie M108, und `SUTTONS` hat **keine einzige**
> > Katalogzeile (die siebzehn aus offenem Punkt 58 sind nicht mehr da).
> > *Behauptet wird:* Die Mehrkosten stammen **nicht** aus diesem Schritt.
> > **Die Lücke:** Woher sie *stammen*, ist **nicht gemessen**. Weder Datenmenge noch Katalogstand
> > noch Rollup-Stand haben sich geändert; bleibt der Zustand der Instanz (Puffer, Fremdlast). Das
> > ist **plausibel und nicht belegt**, und es wird hier nicht als Ursache behauptet. Offener Punkt
> > **134**.
>
> **Was das für die Abnahme heißt:** Das Budget hält mit Faktor 2,6 Luft, und der eigene Beitrag des
> Schritts ist gemessen negativ. Die Drift im Verteilungsblock ist ein **eigener** Befund und wird
> nicht in diesem Schritt geheilt.

> ### M146 *(04.09.2026)* — der verdichtete Block, drei Mandanten, drei Fenster
>
> **Anlass:** Der Block gruppiert seit E‑90 nach Prozess (§7a). Zu messen war die Frage, ob die
> Gruppierung den Plan zurück auf den Zeitindex dreht — der Hinweis kann sie nicht mehr verhindern,
> weil er nur die *Sortierung* betrifft.
>
> #### Der Plan: unverändert
>
> | Mandant | Treibertabelle | Index auf `Message` | `rows` | `Extra` |
> |---|---|---|---:|---|
> | `NEXANS` | `Message` | **`MessageStatusIDX`** | 6.257 | `Using index condition; Using where; Using temporary; Using filesort` |
> | `VOTG` | `Message` | **`MessageStatusIDX`** | 6.257 | dasselbe |
> | `SUTTONS` | **`ProjectMandant`** | **`MessageStatusIDX`** | 6.257 | dazu `Using join buffer (flat, BNL join)` |
>
> **Welche Tabelle den Einstieg macht, hängt am Mandanten** — dieselbe Beobachtung wie in M108, und
> `DashboardPlanDbIT` schreibt deshalb weiterhin Zugriffsart und Index fest und nicht die
> Reihenfolge. `MessageLastUpdateIDX` steht in `possible_keys` und in **keinem** Plan als `key`.
>
> #### Die Laufzeit: beste von fünf, ein Aufwärmlauf davor
>
> **In SQL gegen die Testkopie**, beide Fassungen mit demselben Indexhinweis, damit der Vergleich
> die *Gruppierung* misst und nicht den Hinweis:
>
> | | `NEXANS` | `SUTTONS` | `VOTG` |
> |---|---|---|---|
> | 48 h — alt → **verdichtet** | 23,378 → **22,201** | 22,481 → **20,104** | 21,941 → **19,979** |
> | 30 T | 23,915 → **22,488** | 21,452 → **21,165** | 21,614 → **20,370** |
> | 12 M | 23,108 → **26,220** | 23,194 → **24,087** | 23,015 → **22,118** |
>
> **Fünf von neun Kombinationen werden schneller, eine wird um 3,1 ms teurer** — `NEXANS` über
> zwölf Monate, also der Mandant mit den meisten Fehlerzeilen im breitesten Fenster. Spannweite:
> alt 21,5 bis 23,9 ms, verdichtet **20,0 bis 26,2 ms**.
>
> **An dem, was der Code schickt** (`MessungM146DbIT`, also mit jOOQ-Rendering, Verbindung und
> Zeilenabbildung): **23,3 bis 30,2 ms** über dieselben neun Kombinationen, der Höchstwert wieder
> bei `NEXANS`/12 M.
>
> **Gegen das Vergleichsmaß:** M108 nennt 22 bis 25 ms, M145 21,9 bis 26,1 ms. Die Größenordnung
> hält; der eine Ausreißer liegt vier Millisekunden über der bisherigen Obergrenze, bei einem
> Budget von 500 ms für die ganze Seite.
>
> #### Was der Block jetzt liefert
>
> | | 48 h | 30 T | 12 M |
> |---|---|---|---|
> | `NEXANS` | **2** Prozesse / 50 Nachrichten | 3 / 55 | 3 / 711 |
> | `SUTTONS` | 0 / 0 | 1 / 5 | 1 / 103 |
> | `VOTG` | 0 / 0 | 0 / 0 | 1 / 8 |
>
> > **Belegvermerk (Regel L10).**
> > *Gemessen ist:* `EXPLAIN` und Laufzeit für drei Mandanten × drei Fenster, in zwei Fassungen
> > (alt und verdichtet) und für die verdichtete zusätzlich in vier Indexfassungen; ein Aufwärmlauf
> > und die beste von fünf, warm, gegen die Testkopie im Profil `dev` am Anker
> > `2025-12-30 04:09:47`. Die Laufzeit ist **zweimal** erhoben — in SQL und an dem, was der Code
> > schickt.
> > *Behauptet wird:* Die Gruppierung dreht den Plan nicht auf den Zeitindex, und der Block bleibt
> > in derselben Größenordnung wie M108.
> > **Die Lücken, und es sind drei.** Alle Werte sind **Warmwerte**; `FLUSH TABLES` steht
> > `monitor_read` nicht zu. Gemessen sind **drei von zehn** Mandanten. Und die Zahl der
> > betroffenen Prozesse ist eine Eigenschaft **dieser Testkopie** — über die Produktion sagt sie
> > nichts, und ob der Block dort zehn Zeilen füllt, ist **nicht** gemessen.
> >
> > **Nicht gemessen ist außerdem der Zustand vor dem Umbau an derselben Instanz zur selben
> > Stunde** für die Fassung *durch den Code*: Dort liegt nur die neue Zahl vor, die alte stammt
> > aus M145 (03.09.2026). Der Vergleich alt/neu oben ist deshalb der aus SQL, wo beide Fassungen
> > **in derselben Sitzung** gelaufen sind.
>
> Skripte, Auswerter und Rohausgaben: `scripts/messung-schritt10c-verdichtung/`.

> ### M178 *(16.09.2026)* — beide Sichten in einer Antwort (E‑161)
>
> **Anlass:** Seit E‑161 trägt die Antwort die Verteilung in **beiden** Sichten, und das
> Verteilungsstatement läuft je Landingpage **zweimal** — einmal je Sicht, in unveränderter Gestalt
> (§4). Regel L7 verlangt die Messung des neuen Statements, und die ganze Seite wird teurer.
>
> #### Vorregistriert — eingetragen und eingecheckt vor dem ersten Lauf
>
> **Was gemessen wird.** `MessungM178DbIT`, dieselbe Bauform wie M108 und M146: das, was der Code
> schickt, gegen die Testkopie im Profil `dev` am Anker `2025-12-30 04:09:47`; ein Aufwärmlauf, dann
> die beste von fünf; `EXPLAIN` über das gerenderte Statement mit Literalen; alle Läufe
> **nacheinander**, keiner parallel zu einem anderen Lauf gegen die Testkopie. Zeiten stehen nur
> hier und in keiner Zusicherung (Regel T1). Gemessen werden je `NEXANS` und `SUTTONS`:
>
> 1. **die Richtungsform einzeln**, je 48 Stunden, 30 Tage, 12 Monate und ohne `zeitraum` — ohne
>    `zeitraum` ist sie das Statement des Paares, das der Endpunkt wählt;
> 2. **die ganze Landingpage** in denselben vier Lagen;
> 3. *zusätzlich zum Auftrag:* **die Partnerform einzeln** in derselben Sitzung. Sie ist der
>    Vergleich, der ohne Tagesdrift auskommt — M108 ist vom 31.08.2026, M145 vom 03.09.2026, und
>    offener Punkt 134 zeigt, dass die Instanz zwischen zwei Tagen 27 ms verschieben kann.
>
> **Die Erwartung.** *Die Landingpage wird je Lage ungefähr um die Kosten eines
> Verteilungsstatements teurer.* Die Richtungsform fährt Zeile für Zeile denselben Plan wie die
> Partnerform (`DashboardPlanDbIT`), sie sollte also kosten, was die Partnerform kostet. Bezug sind
> die Verteilungskosten aus M108 — `NEXANS` **9,9 / 62,6 / 88,5 ms**, `SUTTONS` **21,7 / 32,0 /
> 40,2 ms** — und für `SUTTONS` bei zwölf Monaten seit M145 **67,3 ms** (offener Punkt 134). Die
> Seite selbst gegen M145 gerechnet, **gerechnet und nicht gemessen**:
>
> | Lage | M145, ganze Seite | + ein Verteilungsstatement | **erwartet** |
> |---|---:|---:|---:|
> | `NEXANS` 48 h | 49,5–57,7 ms | 9,9 ms | **59,4–67,6 ms** |
> | `NEXANS` 30 Tage | 149,4–149,8 ms | 62,6 ms | **212,0–212,4 ms** |
> | `NEXANS` 12 Monate | 194,2–195,0 ms | 88,5 ms | **282,7–283,5 ms** — jedenfalls **unter 300 ms** |
> | `NEXANS` ohne `zeitraum` (48 h) | 59,2–59,9 ms | 9,9 ms | **69,1–69,8 ms** |
> | `SUTTONS` 48 h | 49,5–53,4 ms | 21,7 ms | **71,2–75,1 ms** |
> | `SUTTONS` 30 Tage | 116,0–119,5 ms | 32,0 ms | **148,0–151,5 ms** |
> | `SUTTONS` 12 Monate | 139,7–144,6 ms | 40,2 ms (M108) bzw. 67,3 ms (M145) | **179,9–184,8** bzw. **207,0–211,9 ms** |
> | `SUTTONS` ohne `zeitraum` (48 h) | 55,1–59,9 ms | 21,7 ms | **76,8–81,6 ms** |
>
> **Das Tor:** jede Lage **unter 500 ms**. Liegt eine darüber, wird angehalten und berichtet und
> **nicht** umgebaut.
>
> **Wie gelesen wird, festgelegt vor dem Lauf.** Drei Vergleiche, und sie tragen Verschiedenes:
>
> | Vergleich | trägt | trägt nicht |
> |---|---|---|
> | Richtungsform gegen Partnerform, **dieselbe Sitzung** | ob das neue Statement kostet, was das alte kostet — die eigentliche Aussage über diesen Bau | die Seite |
> | Richtungsform gegen M108/M145 | ob die Größenordnung hält | eine Ursache für Abweichungen: Zwischen den Messungen liegen Tage und Punkt 134 |
> | Seite gegen die Rechnung oben | ob die Vorregistrierung zutrifft | **ob die Differenz aus diesem Schritt stammt** — die alte Seite ist in dieser Sitzung nicht gemessen, weil es ihren Code nicht mehr gibt |
>
> **Abweichungen werden benannt und nicht umgedeutet.** Weicht die Seite von der Rechnung ab, die
> Richtungsform aber nicht von der Partnerform derselben Sitzung, steht das so da: *die Seite weicht
> ab, das neue Statement nicht* — und eine Ursache wird nur behauptet, wenn sie gemessen ist.
>
> #### Nummernvergabe — nach dem Verfahren aus [`process-view.md`](process-view.md) §46
>
> | | |
> |---|---|
> | **Entscheidung** | **E‑161.** Python-Suche mit Wortgrenzen über `docs/*.md`, `DEVELOPMENT_GUIDELINES.md` und `CLAUDE.md`, Strich als `-` oder U+2011: höchste **E‑160** ([`dashboard-frontend.md`](dashboard-frontend.md), 16.09.2026, sechs Treffer, alle gelesen). **E‑150** und **E‑159** stehen nur in der Vergabetabelle von [`spaltenwahl.md`](spaltenwahl.md) als Suchbereiche („E‑147 bis E‑159", „E‑148, E‑149 und E‑150 null") — gelesen, keine Vergabe. **E‑780** ist der bekannte Falschtreffer. Die nicht gemergten Zweige `feat/suchfeld-untermenues` und `test/indexbestand-e37` tragen kein E‑16x; die Suche ist auf beiden geeicht (E‑112 bzw. ein E‑Treffer in `messungen-schritt8.md` gefunden) |
> | **Messung** | **M178.** Dieselbe Suche nach `M\d{2,4}`: höchste **M177** ([`spaltenwahl.md`](spaltenwahl.md)). **M178** steht nur in *„`M176` bis `M178` … kein Treffer"* ([`messungen-sichtprobe-schmal.md`](messungen-sichtprobe-schmal.md) §3) und im Zitat dieser Zeile in [`spaltenwahl.md`](spaltenwahl.md), **M179** nur als Fließtext in [`messungen-property-suche.md`](messungen-property-suche.md) — alle gelesen, keine Vergabe. Beide Zweige: kein M178, kein M18x; ihre Treffer auf `18x` sind Tabellenwerte |
> | **Offener Punkt** | **182**, falls einer entsteht. `\*\*18[1-9]\*\*` und `Punkt 18[1-9]`: höchster vergebener **181** ([`spaltenwahl.md`](spaltenwahl.md)); die übrigen Treffer auf 181 bis 188 sind Tabellenwerte (`messungen-schritt10b.md` „Partner 8 \| 181", `messungen-schritt8.md` 182, 184, 185, `messungen-sichtprobe-schmal.md` Breiten 184) — gelesen, keine Punkte |
>
> #### Ergebnis — zwei Läufe, 16.09.2026, 12:04 und 12:05
>
> `MessungM178DbIT` ist **zweimal** gelaufen, nacheinander und ohne anderen Lauf gegen die
> Testkopie dazwischen. Der Auftrag verlangt einen; der zweite ist nachgefahren worden, weil die
> teuerste Lage im ersten Lauf mit 297,6 ms knapp unter der vorregistrierten Marke von 300 ms lag
> und M145 Spannen aus mehreren Durchgängen nennt. **Beide Läufe stehen hier, keiner ist
> ausgewählt.** Rohausgaben: `scripts/messung-m178-beide-sichten/ergebnis/`.
>
> ##### 1. Die Richtungsform gegen die Partnerform derselben Sitzung — sie kostet dasselbe
>
> Beste von fünf, Lauf 1 / Lauf 2, in ms:
>
> | Lage | **Richtungsform** | Partnerform | Bezug M108 (Verteilung) |
> |---|---:|---:|---:|
> | `NEXANS` 48 h | **12,685 / 12,700** | 12,380 / 13,324 | 9,920 |
> | `NEXANS` 30 Tage | **70,195 / 70,781** | 71,722 / 71,205 | 62,570 |
> | `NEXANS` 12 Monate | **95,738 / 98,727** | 97,744 / 98,401 | 88,519 |
> | `NEXANS` ohne `zeitraum` (gewählt: 48 h) | **11,836 / 11,991** | 11,828 / 12,166 | 9,920 |
> | `SUTTONS` 48 h | **10,077 / 9,968** | 10,224 / 10,139 | 21,676 |
> | `SUTTONS` 30 Tage | **51,391 / 51,742** | 51,136 / 52,162 | 32,031 |
> | `SUTTONS` 12 Monate | **66,519 / 66,444** | 67,004 / 66,862 | 40,223 (M145: 67,253) |
> | `SUTTONS` ohne `zeitraum` (gewählt: 48 h) | **10,029 / 9,945** | 9,946 / 10,064 | 21,676 |
>
> **Die Richtungsform weicht von der Partnerform desselben Laufs höchstens um 2,0 ms ab** (`NEXANS`
> 12 Monate, Lauf 1) und relativ höchstens um **4,7 %** (`NEXANS` 48 h, Lauf 2: 0,6 ms). In 11 der
> 16 Paare ist sie die billigere, in 5 die teurere — eine Richtung hat die Abweichung nicht. Zum
> Maßstab: Die Partnerform allein schwankt zwischen ihren zwei Läufen bei `NEXANS` 48 h um 0,9 ms
> (7,6 %). Das ist die vorregistrierte Erwartung, und sie **trifft zu**.
>
> **Gegen M108 trifft die Größenordnung nicht zu, und zwar in beide Richtungen:** Bei `NEXANS`
> kostet die Verteilung heute **8 bis 34 % mehr** als am 31.08.2026, bei `SUTTONS` über 48 Stunden
> **knapp die Hälfte**, über 30 Tage und 12 Monate **60 bis 67 % mehr** — bei zwölf Monaten auf 1 ms
> der Wert von M145 (67,3 ms, offener Punkt 134). **Beide Formen tun das gleichermaßen**; die
> Abweichung gehört der Verteilung und nicht der Richtungsspalte.
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* Richtungs- und Partnerform einzeln, je zwei Mandanten × vier Lagen, zwei Läufe,
> > je ein Aufwärmlauf und die beste von fünf, warm, am Anker, über `DashboardRepository` — also mit
> > jOOQ-Rendering, Verbindung und Zeilenabbildung. Dazu die gelesenen Mengen: Beide Formen liefern je
> > Lage **dieselbe Summe** (`NEXANS` 9.950 / 176.050 / 2.308.005, `SUTTONS` 1.337 / 20.964 /
> > 196.536), und sie ist die Zahl der Kachel *Nachrichten* derselben Lage.
> > *Behauptet wird:* Das neue Statement kostet, was das bestehende kostet.
> > **Die Lücke:** Warum die Verteilung gegenüber M108 in beide Richtungen gewandert ist, ist
> > **nicht gemessen** — Bestand, Katalogstand und Instanzzustand sind in dieser Runde nicht erhoben.
> > Es wird keine Ursache behauptet; Punkt 134 bleibt, was er ist.
>
> ##### 2. Die ganze Landingpage — das Tor hält, die Rechnung nicht
>
> | Lage | erwartet (vorregistriert) | **gemessen, Lauf 1 / Lauf 2** | im Band? |
> |---|---:|---:|---|
> | `NEXANS` 48 h | 59,4–67,6 ms | **69,900 / 65,388 ms** | Lauf 2 ja, Lauf 1 **2,3 ms darüber** |
> | `NEXANS` 30 Tage | 212,0–212,4 ms | **223,273 / 225,532 ms** | **nein — 10,9 bis 13,1 ms darüber** |
> | `NEXANS` 12 Monate | 282,7–283,5 ms, **unter 300 ms** | **297,554 / 294,210 ms** | Band **nein — 10,7 bis 14,1 ms darüber**; die Marke von 300 ms **ja**, mit 2,4 bzw. 5,8 ms Abstand |
> | `NEXANS` ohne `zeitraum` | 69,1–69,8 ms | **72,632 / 70,906 ms** | **nein — 1,1 bis 2,8 ms darüber** |
> | `SUTTONS` 48 h | 71,2–75,1 ms | **61,792 / 58,668 ms** | **nein — 9,4 bis 12,5 ms darunter** |
> | `SUTTONS` 30 Tage | 148,0–151,5 ms | **167,460 / 166,314 ms** | **nein — 14,8 bis 16,0 ms darüber** |
> | `SUTTONS` 12 Monate | 179,9–184,8 (M108) bzw. 207,0–211,9 ms (M145) | **209,429 / 206,685 ms** | gegen M108 **nein**; gegen M145 Lauf 1 ja, Lauf 2 **0,3 ms darunter** |
> | `SUTTONS` ohne `zeitraum` | 76,8–81,6 ms | **66,050 / 65,880 ms** | **nein — 10,8 bis 10,9 ms darunter** |
>
> **Das Tor: gehalten.** Die teuerste Lage ist `NEXANS` über zwölf Monate mit **297,6 ms**; keine
> Lage kommt in die Nähe von 500 ms.
>
> **Die vorregistrierte Rechnung trifft in keiner Lage mit beiden Läufen zu**, in zwei Lagen mit
> einem von zwei, in sechs mit keinem. Von diesen sechs liegen **vier darüber und zwei darunter** —
> die Abweichungen gehen in beide Richtungen. Das steht so da und wird nicht umgedeutet.
>
> **Was die Tabelle unter 1 dazu zeigt, und es ist die vorregistrierte Leseregel:** *Die Seite
> weicht ab, das neue Statement nicht.* Die Rechnung setzte die Verteilungskosten von M108 ein, und
> genau diese Kosten sind heute andere — bei `SUTTONS` über 48 Stunden halb so hoch, und genau dort
> liegt die Seite unter dem Band; bei `SUTTONS` über 30 Tage 60 % höher, und dort liegt sie darüber.
>
> Nachgerechnet mit den Verteilungskosten **derselben Sitzung** statt denen von M108:
>
> | Lage | Seite heute − Seite M145 (gerechnet) | Richtungsform heute (gemessen) |
> |---|---:|---:|
> | `NEXANS` 48 h | 7,7 bis 20,4 ms | 12,7 ms |
> | `NEXANS` 30 Tage | 73,5 bis 76,1 ms | 70,2 bis 70,8 ms |
> | `NEXANS` 12 Monate | 99,2 bis 103,4 ms | 95,7 bis 98,7 ms |
> | `NEXANS` ohne `zeitraum` | 11,0 bis 13,4 ms | 11,8 bis 12,0 ms |
> | `SUTTONS` 48 h | 5,3 bis 12,3 ms | 10,0 bis 10,1 ms |
> | `SUTTONS` 30 Tage | 46,8 bis 51,5 ms | 51,4 bis 51,7 ms |
> | `SUTTONS` 12 Monate | 62,1 bis 69,7 ms | 66,4 bis 66,5 ms |
> | `SUTTONS` ohne `zeitraum` | 6,0 bis 11,0 ms | 9,9 bis 10,0 ms |
>
> In fünf Lagen liegt die Richtungsform innerhalb der Spanne, bei `SUTTONS` 30 Tage reicht sie bis
> 0,3 ms über deren oberes Ende. **Bei `NEXANS` liegt sie darunter** — über 30 Tage um 2,7 bis
> 5,9 ms, über zwölf Monate um 0,5 bis 7,7 ms: Dort ist die Seite um etwas mehr gewachsen als um das
> eine Statement.
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* die ganze Landingpage über `DashboardService.landingpage`, je zwei Mandanten × vier
> > Lagen, zwei Läufe, warm, gegen die Testkopie am 16.09.2026; und — in derselben Sitzung — die
> > Richtungsform einzeln.
> > *Behauptet wird:* Die Seite ist um ungefähr die Kosten eines Verteilungsstatements teurer
> > geworden.
> > **Die Lücke, und sie ist die ganze Aussage der zweiten Tabelle:** Die Seite **vor** diesem Schritt
> > ist in dieser Sitzung **nicht gemessen** — ihr Code existiert nicht mehr. Die Differenz rechnet
> > eine Messung vom 16.09.2026 gegen eine vom 03.09.2026, und zwischen beiden liegen dreizehn Tage
> > und die in Tabelle 1 sichtbare Wanderung der Verteilungskosten. **Die Nachrechnung ist
> > verträglich mit der Behauptung und belegt sie nicht.** Die bis zu 5,9 bzw. 7,7 ms über dem einen
> > Statement bei `NEXANS` 30 Tage und 12 Monate sind nicht aufgeklärt; ob sie aus diesem Schritt
> > stammen oder aus derselben Wanderung, ist nicht gemessen.
> > **Nicht gemessen außerdem:** Kaltwerte (`FLUSH TABLES` steht `monitor_read` nicht zu), die acht
> > übrigen Mandanten, die Produktion.
>
> ##### 3. Die Pläne der Richtungsform (Regel L7)
>
> Über das gerenderte Statement mit Literalen, am Anker:
>
> | Mandant | Einstieg | Rollup-Ebene | Katalog | Mandantenkette |
> |---|---|---|---|---|
> | `NEXANS` | die Rollup-Ebene, **`range` über `PRIMARY`** (`key_len` 5 / 3 / 3, `rows` 929 / 14.214 / 6.051), `Using temporary; Using filesort` | — (ist der Einstieg) | `eq_ref` über `PRIMARY` | `dashboard_process` `eq_ref` über `PRIMARY`, `ProjectMandant` `eq_ref` über `PRIMARY` |
> | `SUTTONS` | **`ProjectMandant`, `ref` über `ProjectMandant_Mandant_idx`**, `Using temporary; Using filesort` | `range` über `PRIMARY`, `Using join buffer (flat, BNL join)` | `eq_ref` über `PRIMARY` | `dashboard_process` `eq_ref` über `PRIMARY` |
>
> **Zeile für Zeile der Plan der Partnerform** — Tabelle, Zugriffsart und Index sind in allen sechs
> Kombinationen gleich; erhoben am 16.09.2026 vor dem Bau des Plantests und seither von
> `DashboardPlanDbIT.richtungsform_treiber_und_index` festgehalten. **Keine Tabelle wird voll
> gelesen.** Welche Tabelle den Einstieg macht, hängt wie seit M108 am Mandanten; der Test schreibt
> deshalb die **zwei** zulässigen Einstiege fest und nicht einen je Mandanten (§9).

### Die ursprüngliche Messung im Wortlaut — M108 *(31.08.2026)*

Gemessen mit `MessungM108DbIT`: **das, was der Code schickt**, gegen die Testkopie, im Profil `dev`
gegen den Anker `2025-12-30 04:09:47`. Ein Aufwärmlauf, dann die **beste von fünf** — dieselbe
Bauform wie M94, M98 und M107. Der `EXPLAIN` läuft über das **gerenderte** Statement mit Literalen.

**Die Zeiten stehen hier und in keiner Zusicherung** (Regel T1): `MessungM108DbIT` sichert nur
Zählwerte zu.

### Die Bezugswerte und was herausgekommen ist

| Ansicht | erwartet (M94/M107/M90) | **gemessen, `NEXANS`** | **gemessen, `SUTTONS`** |
|---|---:|---:|---:|
| 48 h — Verlauf | 6,8 ms | **8,955 ms** | 6,941 ms |
| 48 h — Verteilung | 10,7 ms | **9,920 ms** | 21,676 ms |
| 30 Tage — Verlauf | ~46 ms | **47,221 ms** | 30,789 ms |
| 30 Tage — Verteilung | ~62 ms | **62,570 ms** | 32,031 ms |
| 12 Monate — Verlauf | 65,4 ms | **63,699 ms** | 39,687 ms |
| 12 Monate — Verteilung | 88,7 ms | **88,519 ms** | 40,223 ms |
| Überfällig — im Fenster | 2,3 ms | 5,102 ms | 4,516 ms |
| Überfällig — insgesamt | 4,3 ms | 6,171 ms | 5,171 ms |
| Zuletzt aufgefallen (beide Statements), 48 h / 30 T / 12 M | — | 32,595 / 31,192 / 31,950 ms | 29,715 / 32,887 / 34,084 ms |
| Belegungsprobe, 48 h / 30 T / 12 M | — | 7,754 / 36,950 / 47,770 ms | 6,252 / 30,380 / 39,317 ms |
| Stand | — | 0,722 ms | 0,781 ms |

**Verlauf und Verteilung treffen die Vorhersage aus M107 auf die dritte Stelle** — 63,699 gegen
65,350 ms und 88,519 gegen 88,672 ms. Das ist der Beleg dafür, dass die gebaute Fassung dieselbe
Abfrage ist wie die gemessene, obwohl sie die Mandantenkette als `EXISTS` statt als `JOIN` fährt und
`Project` weglässt.

**„Zuletzt aufgefallen" ist von der Fensterbreite unabhängig** — 29 bis 34 ms über alle sechs
Kombinationen. Genau das ist der Zweck des Umbaus aus §7a; vorher lag derselbe Block zwischen
1,6 ms und einem Abbruch.

### Die ganze Landingpage

| Paar | **`NEXANS`** | **`SUTTONS`** |
|---|---:|---:|
| 48 h | **62,227 ms** | **72,007 ms** |
| 30 Tage | 152,814 ms | 109,829 ms |
| 12 Monate | **199,030 ms** | 127,038 ms |
| **ohne `zeitraum`** (Standardfenster; beide bekommen `48H`) | **64,706 ms** | **77,957 ms** |

**Das Budget sind 500 ms.** Die Standardansicht kostet 63 bis 78 ms, die teuerste überhaupt
mögliche 199 ms. **Deutlich darunter, wie verlangt.**

### Zwei Abweichungen, und beide sind Befunde

**1. Die Kachel *Überfällig* kostet mehr als M90 sagt** — 5,1 statt 2,3 ms und 6,2 statt 4,3 ms,
also gut das Doppelte. **Der Plan ist identisch** (`range` über `MessageStatusIDX`, `rows = 539`,
Zeichen für Zeichen der aus M90). Die naheliegende Erklärung steht in
[`messungen-schritt10.md`](messungen-schritt10.md) selbst: **M90 hat gegen den falschen Anker
gemessen** (`2026-07-08 17:21:10` statt `2025-12-30 04:09:47`), und im dortigen Fenster D lagen
**null** Treffer statt einem. **Gemessen ist damit nicht dieselbe Frage.** Die absolute Größe bleibt
belanglos — 6 ms von 500 —, und die Rechnung, für die M90 gebraucht wurde, trägt unverändert: Die
Kachel ist billig genug, um live zu laufen.

**2. Die Verteilung ist bei `SUTTONS` im 48‑Stunden‑Fenster teurer als bei `NEXANS`** — 21,7 gegen
9,9 ms, bei einem Achtel der Nachrichten. Das ist kein Widerspruch, sondern derselbe Befund wie in
M98: **Der Bereichszugriff liest die Rollupzeilen des ganzen Fensters**, unabhängig davon, wie viele
davon dem Mandanten gehören. *„Ein kleiner Mandant zahlt im Verteilungsblock fast so viel wie der
größte."*

### Die Pläne (Regel L7)

| Statement | Zugriffspfad auf die Quelle |
|---|---|
| Verlauf 48H | `message_rollup`, `range` über `PRIMARY`, `key_len 5`, `Using temporary; Using filesort` |
| Verlauf 30T | `message_rollup_tag`, `range` über `PRIMARY`, `key_len 3` |
| Verlauf 12M | `message_rollup_monat`, `range` über `PRIMARY`, `key_len 3` |
| Verteilung, alle drei | wie der Verlauf, plus `process_catalog` als `eq_ref` über `PRIMARY` |
| Belegung, alle drei | derselbe Bereich, gekapselt in einer abgeleiteten Tabelle |
| ~~Überfällig, beide~~ | *entfallen (E‑71). An ihrer Stelle:* |
| **Läuft** | `Message`, **`ref`** über **`MessageStatusIDX`**, `key_len 123`, `rows = 1` |
| **Wartend** | `Message`, **`ref`** über **`MessageStatusIDX`**, `key_len 123`, `rows = 538` |
| **Erscheinungsbedingung Wartend** | `ProjectMandant` `ref` über `ProjectMandant_Mandant_idx` → `Process` → `SOS` → `SOSAction`, alle `ref` über Index; **keine Tabelle wird voll gelesen** |
| Zuletzt aufgefallen, Fehlerhälfte | `Message`, `range` über **`MessageStatusIDX`**, `rows = 6.257`. *Seit E‑90 zusätzlich `Using temporary; Using filesort` über der **Ergebnismenge** — Index und Zeilenzahl unverändert (M146)* |
| ~~Zuletzt aufgefallen, Überfälligkeitshälfte~~ | *entfallen (E‑71)* |
| Stand | `rollup_lauf`, `range` über `rollup_lauf_stand_idx` |

Die Mandantenkette steht in **jedem** Plan als `eq_ref` über Primärschlüssel — nie als Durchlauf.

> **Welche Tabelle den Einstieg macht, hängt am Mandanten.** Bei `NEXANS` ist es die Rolluptabelle,
> bei `SUTTONS` steigt der Optimierer über `ProjectMandant` ein, weil dieser Mandant wenige Projekte
> hat. **Beide Pläne sind richtig**, beide sind gemessen schnell — und `DashboardPlanDbIT` schreibt
> deshalb **die Zugriffsart und den Index** fest und nicht die Reihenfolge. Dieselbe Überlegung wie
> in [`nachrichtenliste.md`](nachrichtenliste.md) §5b.

> ### Belegvermerk (Regel L10)
>
> *Gemessen ist:* alle sieben Statements und die zusammengesetzte Landingpage, je Paar und für zwei
> Mandanten, **warm**, ein Aufwärmlauf und dann die beste von fünf, gegen die Testkopie im Profil
> `dev`.
>
> *Behauptet wird:* Die Landingpage bleibt in jeder Kombination deutlich unter 500 ms.
>
> **Die Lücken, und es sind drei.** Alle Werte sind **Warmwerte**; `FLUSH TABLES` steht
> `monitor_read` nicht zu. Gemessen sind **zwei von zehn** Mandanten. Und alle Zahlen stammen von
> der Testkopie — über die Produktion sagt keine von ihnen etwas.

---

## 9. Tests

| Test | Was er sichert |
|---|---|
| `DashboardIsolationDbIT` | **Regel M4, Pflicht.** Seit 10b‑4 **je neuem Statement einzeln**: *Wartend* zeigt für zwei Mandanten verschiedene Zahlen (fiele der Filter, sähen beide den ganzen Bestand); die Erscheinungsbedingung trennt `NEXANS`/`VOTG` von `SUTTONS`; *Läuft* ist lokal **nicht** prüfbar, weil `RUNNING` null Mal vorkommt — dort trägt der Statementtest.  Zwei Zusicherungen tragen den Nachweis: **verschiedene Summen** über dasselbe Fenster (fiele der Filter, sähen beide die Zahl des ganzen Bestands) und **jede gezeigte Prozesskennung gehört dem eigenen Mandanten**. Dazu: keine fremde Kennung im Rumpf, kein Mandantenparameter, Standardfenster und Leerzustand, alle Blöcke in einer Antwort |
| `DashboardServiceTest` | Einordnung, Fehlerarten, die beiden Restzeilen, „nicht ermittelbar", Standardfenster, Leerzustand — **ohne Datenbank**, alle Prüfwerte erfunden |
| `DashboardStatementsTest` | Das **gerenderte** SQL: `EXISTS` statt `JOIN`, `CASE` ohne Alias, `LEFT JOIN` auf den Katalog, keine Funktion um den Eimerschlüssel. **Seit dem 03.09.2026 benennt er die sieben Statements einzeln, statt sie zu zählen** — in 10b‑4 sind drei weggefallen und drei hinzugekommen, die Zahl blieb sieben, und ein zählender Test hätte bestanden, ohne noch etwas zu bezeugen. Dazu ein Verbot: **kein Statement der Seite rechnet noch mit `MessageTimeout`** |
| `DashboardPlanDbIT` | **`EXPLAIN`, Treibertabelle und Index — keine Zeitmessung.** Der Wächter über den Befund aus §7a. Seit 10b‑4 zusätzlich: beide neuen Kacheln über `MessageStatusIDX`, und die Erscheinungsbedingung liest **keine Tabelle voll** — auch `SOSAction` nicht |
| `DashboardZeitgrenzeTest` | Der Abbruch an der Zeitgrenze wird zu „nicht ermittelbar", **jeder andere Fehler nicht** — seit 10b‑4 an den Kacheln *Läuft* und *Wartend*, und mit der Gegenprobe, dass auch die **Erscheinungsbedingung** nichts abfängt |
| `DashboardzeitraumTest` | Die Fenstergrenzen, gegen den Anker — Eimergrenzen, obere Grenze ausschließend, Kalendermonate statt 365 Tagen |
| `MessungM108DbIT` | Die Messung. Kein Test |

> **Ergänzt 10.09.2026 (Schritt 10d Teil A).** Zum Block `plattform` kommen fünf Tests hinzu, und
> sie stehen in [`dienste.md`](dienste.md) §12: `DienstStatusClassifierTest`, `AblagenkachelTest`,
> `AblagenpruefungTest`, `PlattformAntwortTest` und `DienstkatalogDbIT` (der Drift-Test über
> `SELECT DISTINCT ServiceStatus`). **Vier bestehende Tests sind erweitert:**
> `DashboardStatementsTest` benennt das achte Statement und die Gestalt beider neuen Abfragen,
> `DashboardServiceTest` den Zusammenbau des Blocks, `DashboardIsolationDbIT` die **Gleichheit** des
> Blocks für zwei Mandanten samt Regel G1 am Rumpf, und `PaketstrukturTest` die dritte benannte
> Ausnahme von Regel M2.

> **Ergänzt 17.09.2026 (Live-Rest, Teil B).** Ein Test kommt hinzu und vier sind erweitert, alle in
> [`live-rest.md`](live-rest.md) §12 aufgeführt: **`DashboardLiveRestDbIT`** (die Summenprobe je
> Paar — Kachel, Verlauf und beide Sichten gleich `COUNT(*)` aus `Message`, Uhr und Wasserstand
> gestellt); `DashboardServiceTest` mit dreizehn Fällen unter „Der Live-Rest" (Stunden-, Tages- und
> Monatseimer, nur im Fenster, Klemme, nur Live-Verkehr, Verteilung je Schlüssel samt Schreibweisen,
> keine Nachlesung ohne Zeilen, der Block in drei Zuständen, ein Uhrenschlag, die Belegungsprobe ohne
> Korrektur); `DashboardStatementsTest` benennt **zehn** Statements je Seite und unter `LiveRest`
> die zwölf und dreizehn — **die Zusicherung „genau neun" ist bewusst gefallen**, ebenso läuft
> `keine_frist_mehr_in_der_ganzen_seite` jetzt über die vollste Seite mit dreizehn;
> `DashboardPlanDbIT` prüft die Nachlesung (`PRIMARY`, nichts voll); `DashboardIsolationDbIT` den
> Block in der Antwort und die Nachlesung **am Repository** mit selbst angelegter Katalogzeile.
> **`keine_mandanten_id` ist am 17.09.2026 in einem von zwei Läufen über eine Sekundengrenze gefallen**
> — sechs Lampen um je eine Sekunde, sonst nichts, mit dem Block `liveRest` in beiden Rümpfen
> identisch; der zweite Lauf war grün (26 von 26): Punkt **182**, unverändert offen und nicht nebenbei
> repariert.

> **Ergänzt 18.09.2026 (Fehler live).** Ein Test kommt hinzu, vier sind erweitert, alle in
> [`fehler-live.md`](fehler-live.md) §9 aufgeführt: **`DashboardFehlerLiveDbIT`** (Kachel *Fehler* =
> `COUNT(*)` aus `Message` je Paar, = Summe über „Zuletzt aufgefallen" bis zehn Prozesse, die
> Dev-Zeile als Identität); `DashboardServiceTest` mit neun Fällen unter „Fehler live", darunter der
> Fall aus der Produktion vorher und nachher; `DashboardStatementsTest` mit elf, jede Lage benannt, die
> Lesung und beide Verteilungsfassungen wörtlich — **geändert:** *zehn* sind *elf*, alle Indizes nach
> der ersten Stelle um eins verschoben, und *„keine Seite ohne Lauf enthält `date_format(`"* heißt
> *„nur die Fehlerlesung"*, jede Änderung dort einzeln begründet; `DashboardPlanDbIT` (Lesung über
> `MessageStatusIDX`, kein Zeitindex; Verteilung ohne Fehler auf dem Plan der Verteilung);
> `DashboardIsolationDbIT` (Block in der Antwort, die Lesung am Repository, Verletzungsprobe rot).
> **`keine_mandanten_id` ist am 18.09.2026 in beiden Läufen gefallen** — beide Male ausschließlich
> `alterSekunden`, sechs Lampen um je eine Sekunde: Punkt **182**.

### Die Verletzungsprobe

Ausprobiert am 31.08.2026 und **zurückgenommen**: In `DashboardRepository.mandantenkette` wurde
`PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())` durch `isNotNull()` ersetzt — der
Mandantenfilter aus Regel M3, ausgehängt.

```
[Ohne Mandantenfilter saehen beide dieselbe Summe (48H)]
Expecting actual:
  12004L
not to be equal to:
  12004L
        at DashboardIsolationDbIT.summen_sind_verschieden

[Fremde Prozesskennung in „Zuletzt aufgefallen" (48H)]
Expecting JSONArray:
  ["18100_ARCHROMA_IFTSTA", "08100_ARCHROMA_IFTMIN", … ]   ← die Prozesse von VOTG
to contain: … but could not find the following element(s):
  ["40090_BMW_LAB_VDA", …]                                 ← sie gehoeren NEXANS
        at DashboardIsolationDbIT.nur_eigene_prozesse_in_den_zeilen
```

**12.004 ist die Zahl des ganzen Bestands im 48‑Stunden‑Fenster** (M95, Paar P1). Ohne
Mandantenfilter sieht sie jeder Mandant — die Meldung nennt damit nicht irgendeine Abweichung,
sondern genau den Zustand, den Regel M4 verbietet.

> ### ⚠️ Die Probe hat einen zweiten Test hervorgebracht, und das ist der eigentliche Ertrag
>
> **Beim ersten Durchlauf blieb `keine_fremden_prozesse` grün** — mit ausgehängtem Filter. Der Grund
> ist strukturell und gilt für jedes Dashboard: **Eine aggregierte Antwort trägt kaum Kennungen.**
> Der Verlauf besteht aus Zahlen, der Verteilungsblock zeigt Partnernamen, und die zehn Zeilen aus
> „Zuletzt aufgefallen" gehörten zufällig alle `NEXANS` — gegen dessen Prozessliste hat der Test
> gar nicht geprüft, weil die Paarung `VOTG`/`SUTTONS` ist.
>
> **`nur_eigene_prozesse_in_den_zeilen` dreht die Frage um:** Jede *gezeigte* Prozesskennung muss in
> der **eigenen** Prozessliste stehen. Damit fällt der Test, egal welchem fremden Mandanten die
> Zeile gehört — und er fällt schon dann, wenn nur der Filter dieses einen Blocks ausfällt.
>
> **Das ist genau der Fall, für den [`testfestigkeit.md`](testfestigkeit.md) angelegt worden ist:**
> ein grüner Test, der seine Aussage nicht trägt. Gefunden hat ihn nicht das Nachdenken, sondern die
> Verletzungsprobe.

Der Arbeitsbaum ist danach wiederhergestellt worden; die Änderung ist in keinem Commit.

> ### ⚠️ Fortschreibung vom 16.09.2026 — die Tests zu beiden Sichten (E‑161)
>
> **Tabelle und Probe darüber bleiben stehen.** Geändert und hinzugekommen:
>
> | Test | Was sich ändert |
> |---|---|
> | `DashboardStatementsTest` | **Neun Statements, einzeln benannt** — das neue steht an dritter Stelle als *„Verteilung, Richtungssicht"* und ist Zeichen für Zeichen die Partnersicht mit der anderen Spalte (`die_neun_statements_je_seite`, bis dahin `die_acht_statements_je_seite`). **Der Test stellt die Seite nicht mehr von Hand nach**, sondern ruft `DashboardService.landingpage` über die jOOQ-Attrappe: Die acht Repository-Aufrufe in der Reihenfolge des Service bewiesen die Gestalt der Statements, aber nicht, dass die Seite sie absetzt — ein Service ohne das zweite Verteilungsstatement hätte ihn bestehen lassen. Dazu `kein_zusammengelegtes_verteilungsstatement`: genau zwei Statements hängen den Katalog an, und keines liest `partner` und `richtung` zugleich |
> | `DashboardIsolationDbIT` | **Regel M4 für das Richtungsstatement, einzeln:** `richtungssicht_ist_getrennt` — die Zeilen der Richtungssicht zählen zusammen genau die Kachel *Nachrichten* derselben Antwort, je `VOTG` und `SUTTONS` und je Paar. Die Kachel kommt aus einem **anderen** Statement (dem Verlauf); fällt der Filter nur im Richtungsstatement, zählt die Sicht den ganzen Bestand des Fensters. Pflegeunabhängig (T2): Die Gleichheit hängt an keinem Katalogstand, weil der Katalog über seinen Primärschlüssel als `LEFT JOIN` anhängt. Dasselbe als `partnersicht_ist_getrennt` — **als eigener Test**, damit eine Probe, die nur einen Filter aushängt, genau einen fällt. **Entfallen:** `richtungssicht_traegt_nichts_fremdes` (Aufruf mit `?verteilung=RICHTUNG`, Suche nach fremden Prozesskennungen) — den Parameter gibt es nicht mehr, und der Test trug seine Aussage auch vorher nicht: Der Block zeigt Richtungen und keine Prozesskennungen. **Ersetzt:** *„Eine unbekannte Verteilungssicht ist 400"* durch `verteilung_ist_wirkungslos` — `RICHTUNG`, `PARTNER` und `BELEGART` ergeben `200` und denselben Block. Verglichen wird der Block und nicht der Rumpf, weil der Rumpf mit der Anwendungsuhr weiterläuft (Punkt 182). **Erweitert:** Leerzustand und „alle Blöcke in einer Antwort" prüfen beide Sichten |
> | `DashboardPlanDbIT` | `richtungsform_treiber_und_index` — **ohne Zeitmessung**, je `NEXANS` und `SUTTONS` und je Paar: Der Einstieg ist die Rollup-Ebene über `PRIMARY` **oder** `ProjectMandant` über `ProjectMandant_Mandant_idx` und nichts Drittes; die Ebene wird als `range` über `PRIMARY` gelesen, der Katalog hängt als `eq_ref` über `PRIMARY` an; und der Plan ist **Zeile für Zeile** (Tabelle, Zugriffsart, Index) der Plan der Partnerform. *Abweichung vom Wortlaut des Auftrags, und sie ist gewollt:* Die Treibertabelle steht als **Menge der zwei gemessenen Einstiege** fest und nicht je Mandant — §8 legt fest, dass dieser Test die Reihenfolge nicht je Mandant festschreibt, weil sie an der Statistik hängt |
> | `DashboardServiceTest` | Klasse `BeideSichten`, ohne Datenbank: jede Sicht bekommt ihre eigenen Zeilen und ihre eigenen Restzeilen — die Partnersicht mit „Übrige (2)", die Richtungssicht in **derselben** Antwort ohne, jede mit ihrem eigenen „nicht zugeordnet" (`restzeilen_je_sicht`); das Verteilungsstatement läuft je Sicht **genau einmal** (`je_sicht_ein_statement`); der Block hat genau die Felder `partner` und `richtung` und kein `sicht` (`felder_des_blocks`). **Entfallen:** *„Die Sicht steht in der Antwort, auch wenn der Parameter fehlte"* — es gibt weder den Parameter noch das Feld |
> | `MessungM178DbIT` | Die Messung. Kein Test (§8) |
> | `MessungM108DbIT` | **Nur an die neue Signatur angepasst** (`landingpage(mandant, zeitraum)`). Wer M108 nachfährt, misst seither die Seite **mit beiden Sichten** — die Zahlen oben in §8 sind die vom 31.08.2026 und bleiben es |
>
> **Die Gegenproben, gefahren und zurückgenommen, in keinem Commit:** Ein Service, der zweimal die
> Partnerform schickt, fällt `restzeilen_je_sicht`, `je_sicht_ein_statement` und
> `die_neun_statements_je_seite` (*„[3 Verteilung, Richtungssicht — seit dem 16.09.2026]"*).
>
> #### Die Verletzungsprobe vom 16.09.2026 — nur im Richtungsstatement
>
> In `DashboardRepository.verteilung` wurde für `Verteilungssicht.RICHTUNG` die Mandantenkette durch
> eine ersetzt, in der `PROJECTMANDANT.MANDANTID.eq(mandant.mandantId())` zu `isNotNull()` wird —
> dieselbe Verletzung wie am 31.08.2026, aber **nur in diesem einen Statement**. Die Partnerform und
> alle übrigen Statements blieben unberührt. Die Datei wurde vorher gesichert und danach aus der
> Sicherung zurückgeschrieben (Prüfsumme gleich), nicht über `git checkout`.
>
> `DashboardIsolationDbIT`, 24 Fälle, **3 rot**:
>
> ```
> [ERROR] DashboardIsolationDbIT.richtungssicht_ist_getrennt
> [Die Sicht richtung zaehlt 12004 Nachrichten, die Kachel 399 — ohne Mandantenfilter im
>  Verteilungsstatement saehe die Sicht den ganzen Bestand (48H)]
> expected: 399L
>  but was: 12004L
>
> [ERROR] DashboardIsolationDbIT.leerzustand
> [Und in der Richtungssicht dasselbe]
> Expecting actual:
>   ["WERT", "WERT", "NICHT_ZUGEORDNET"]
> to contain exactly (and in same order):
>   ["NICHT_ZUGEORDNET"]
>
> [ERROR] DashboardIsolationDbIT.keine_mandanten_id
> [Ein unbekannter Parameter aendert nichts — der Mandant kommt aus der Sitzung]
> ```
>
> **12.004 ist wieder die Zahl des ganzen Bestands im 48‑Stunden‑Fenster** (M95, Paar P1) — dieselbe
> wie in der Probe vom 31.08.2026, jetzt aber nur in der Richtungssicht, während die Kachel daneben
> die 399 von `VOTG` behält. **`partnersicht_ist_getrennt` blieb grün**, und das ist der Beleg dafür,
> dass die beiden Tests je ein Statement tragen und nicht beide dasselbe. Der Leerzustand fällt als
> Beifang: `EDITIONLINGERI` hat keine einzige Nachricht und bekam zwei fremde Richtungen.
>
> > ⚠️ **Der dritte rote Fall stammt nicht aus der Probe.** `keine_mandanten_id` vergleicht zwei
> > **ganze** Rümpfe, und darin steht `plattform.dienste[*].alterSekunden` — gerechnet gegen die
> > Anwendungsuhr, die im Profil `dev` weiterläuft. Die beiden Aufrufe lagen über einer
> > Sekundengrenze; der Unterschied der Rümpfe war **ausschließlich** dort (`8782456` gegen
> > `8782457` und fünf weitere Lampen um je eine Sekunde), der Verteilungsblock war in beiden gleich
> > verletzt. Im grünen Lauf davor bestand der Test. **Er hängt seit Schritt 10d an der Wanduhr** —
> > nicht als Zusicherung über eine Dauer, aber mit derselben Folge, die Regel T1 verhindern soll.
> > **Nicht repariert, sondern als offener Punkt 182 eingetragen** (§11): Die Regel T1 gilt
> > rückwirkend, und ein Verstoß wird benannt und nicht nebenbei mitgeändert. `verteilung_ist_wirkungslos`
> > vergleicht aus genau diesem Grund nur den Block.
>
> Der Arbeitsbaum ist danach wiederhergestellt worden; die Änderung ist in keinem Commit.

---

## 9a. Die Entscheidungen aus Schritt 10b‑4 *(03.09.2026)*

**Sieben, und die erste trägt die übrigen sechs.** Die Nummern sind nach Regel V1 vor der Vergabe
erhoben worden: Die höchste tatsächlich vergebene war **E‑70** ([`dunkelmodus.md`](dunkelmodus.md)),
nicht E‑56 wie im Auftrag angenommen.

| Kennung | Entscheidung |
|---|---|
| **E‑71** | ***Überfällig* ist widerlegt und fällt aus dem MVP** — samt `istUeberfaellig`, `timeoutZeitpunkt`, `ueberfaelligBedingung`, dem Listenparameter `ueberfaellig` und dem Detailfeld. Grundlage ist eine **fachliche Auskunft des Auftraggebers**, keine Messung; der Herkunftsvermerk steht überall dort, wo die Regel geführt wird |
| **E‑72** | **Kachel *Läuft*** — live über `Message`, ohne Zeitfenster, `=` auf den Rohwert `RUNNING`. Erste der zwei neuen benannten Ausnahmen von L2 |
| **E‑73** | **Kachel *Wartend*** — dasselbe für `SUSPENDED`. Zweite Ausnahme |
| **E‑74** | **Die Erscheinungsbedingung ist strukturell** — über `SOSAction`, nicht über die Zahl und nicht über den Rollup. `SUSPEND` und nicht `WAITUNTIL`, **weil M144 es so gemessen hat** |
| **E‑75** | **`aeltesteSekunden` an beiden Kacheln** — als Fenstergrundlage **und** als laufende Prüfung der Auskunft aus E‑71 (§5) |
| **E‑76** | **`fristSekunden` ist bei `WARTEND` `null`** — eine Frist, die niemand durchsetzt, ist eine falsche Auskunft. Bei `RUNNING` bleibt sie und wird erst dadurch richtig ([`nachrichtendetail.md`](nachrichtendetail.md) §3a) |
| **E‑77** | **`--ueberfaellig` bleibt ohne Verbraucher bestehen** — die Farbrolle ist gerechnet und gegengeprobt und wird an dem Tag gebraucht, an dem eine echte Schwelle zurückkommt ([`visuelles-konzept.md`](visuelles-konzept.md) §7a). Dasselbe gilt im Backend für `TIMEOUT_EINHEIT` |

> ### Was an dieser Runde ungewöhnlich ist, und es gehört benannt
>
> **Sechs dieser sieben Entscheidungen hängen an einem Satz, der nicht gemessen ist.** Das Projekt
> hat bisher jede tragende Aussage entweder gemessen oder als ungedeckt gekennzeichnet — hier fällt
> eine gebaute, getestete und gemessene Kategorie **auf eine Auskunft hin**.
>
> **Das ist zulässig und trotzdem eine andere Art von Entscheidung.** Regel Q4 verbietet Raten,
> nicht Auskünfte; und für die Frage, was das Altsystem mit einer Nachricht in `RUNNING` tut, ist
> der Auftraggeber die **einzige** verfügbare Quelle — die Testkopie kennt den Status nicht.
>
> **Was daraus folgt, ist die Disziplin drumherum:** der wörtlich gleiche Herkunftsvermerk an jeder
> Stelle, die offene Prüfung gegen die Produktion in
> [`message-status.md`](message-status.md), und `aeltesteSekunden` als Feld, das die Auskunft im
> laufenden Betrieb beobachtbar hält (E‑75).

---

## 9b. Entscheidung E‑161 — beide Sichten in einer Antwort *(16.09.2026)*

| Kennung | Entscheidung | Datum |
|---|---|---|
| **E‑161** | **`GET /api/dashboard` liefert die Verteilung in beiden Sichten, und der Wechsel zwischen Partner und Richtung geschieht allein im Browser.** Der Parameter `verteilung` entfällt aus dem Vertrag; ein mitgeschickter Wert ist wirkungslos wie `?mandant=`. Das Verteilungsstatement läuft je Sicht einmal, **in unveränderter Gestalt**; ein zusammengelegtes Statement über beide Katalogspalten gibt es nicht. „Ein Aufruf, eine Antwort" (§1) bleibt bestehen. Die Vorgabe `PARTNER` liegt seither im Frontend | 16.09.2026 |

**Der Anlass, gemeldet am 16.09.2026:** Beim Wechsel der Sicht lud sichtbar das **ganze** Dashboard
neu, nicht nur der Verteilungsblock. **Entschieden am selben Tag** durch den Auftraggeber.

**Der Mechanismus, nachgesehen am Code und nicht aus der Meldung übernommen** (Regel V1). Er lag
nicht an einem `key`, und nicht an einem Server-Roundtrip:

| Verdacht | Befund |
|---|---|
| ein `key`, der den Baum neu aufbaut | **nein.** Der einzige `key` in der Nähe steht am Container des Verlaufs und trägt das **Zeitraumpaar** (E‑86); ein Sichtwechsel ändert ihn nicht |
| eine nicht flache URL-Aktualisierung (`shallow: false`) | **nein.** Mitgeschrieben über `onUrlUpdate` des Test-Adapters: `{"history":"replace","scroll":false,"shallow":true}` — kein Server-Rendering |
| **ein Abfrageschlüssel ohne Daten** | **ja.** Der Schlüssel war `["dashboard", "landingpage", zeitraum, sicht]`. Ein Klick auf „Richtung" machte daraus einen **neuen** Schlüssel, für den der Zwischenspeicher nichts hatte; `useQuery` stand auf `isPending`, und `DashboardAnsicht` ersetzte **die ganze Seite** durch `<Laden zeilen={6}>`. Nach der Antwort wurde jeder Block neu eingehängt — der Verlauf samt seiner Aufbaubewegung (E‑85) |

Belegt am 16.09.2026 mit einem Wegwerf-Test gegen den unveränderten Code (danach gelöscht, in keinem
Commit): vor dem Klick **0** Elemente mit `aria-busy`, Kacheln im Baum; nach dem Klick **1**, Kacheln
und Verteilung **fort**; Anfragen `["/api/dashboard", "/api/dashboard?verteilung=RICHTUNG"]`. Der
Rückweg auf eine schon gesehene Sicht kostete keine Anfrage — das hat die Abnahme vom 01.09.2026
gemessen ([`dashboard-frontend.md`](dashboard-frontend.md) §10.3); ob er einen Ladezustand zeigte,
ist nicht erhoben.

**Nicht gebaut und nicht gemessen:** `placeholderData: keepPreviousData` am alten Schlüssel. Es hätte
den Ladezustand verdeckt, aber weiter **eine Anfrage je Wechsel** gestellt. Entschieden war die
Antwort mit beiden Sichten; die Alternative steht hier nur, damit niemand sie für übersehen hält.

**Was es kostet:** das Verteilungsstatement ein zweites Mal je Seite — gemessen als M178 (§8). Das
Tor von 500 ms hält in jeder Lage; die teuerste liegt bei 297,6 ms.

**Am laufenden System nachgesehen** am 16.09.2026 mit `NEXANS`, `SUTTONS` und `EDITIONLINGERI`:
Sichtwechsel hin und zurück ohne Anfrage, ohne Ladezustand und ohne neu eingehängtes Diagramm, die
Seite mit `?verteilung=RICHTUNG` direkt geladen zeigt die Richtungssicht, der Leerzustand ist
unverändert ([`dashboard-frontend.md`](dashboard-frontend.md) §14).

---

## 10. Regelbezug

| Regel | Stand |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | **erfüllt** — und **keine** neue benannte Ausnahme. `?mandant=` ist wirkungslos, geprüft |
| **M2** `MandantContext` erster Pflichtparameter | **erfüllt** — jede öffentliche Methode von `DashboardRepository`. `letzterLauf()` ist **paketprivat**, weil `rollup_lauf` keinen Mandanten trägt und ein Schein-Kontext schlimmer wäre als keiner |
| **M3** Filter im Statement, nicht nachgelagert | **erfüllt** — `EXISTS` in jeder Abfrage |
| **M4** Isolationstest je Endpunkt | **erfüllt** — `DashboardIsolationDbIT` |
| **M5** Trennung gilt auch quer | **erfüllt** — auch die Rollupzeilen, auch die Katalogwerte |
| **L1** Pflicht-Zeitfenster | **erfüllt** — jedes Paar hat eines, und es steht in der Antwort |
| **L2** Keine Live-Aggregation über `Message` | **erfüllt bis auf die eine benannte Ausnahme**, §5 |
| **L7** Jede neue Abfrage gemessen | **erfüllt** — M108, §8, mit `EXPLAIN` je Statement |
| **L9** Durchlauf ohne Zeitfenster nur begründet | **erfüllt** — nur *Überfällig insgesamt*, begründet in §5 |
| **Q3** Die Problemkategorien bleiben getrennt | **erfüllt** — `FEHLER` und `UEBERFAELLIG` sind eigene Werte und werden nie zu „Problem" zusammengefasst |
| **Q4** Nichts raten | **erfüllt** — unbekannte Rohwerte bleiben Rohwerte, Restzeilen tragen keinen Anzeigetext, `sosName` darf `null` sein |
| **T1** Kein Test behauptet etwas über Wanduhrzeit | **erfüllt** — der Plantest prüft den Plan, die Messung sichert nichts zu |
| **T2** Kein Test hängt an veränderlichen Daten | **erfüllt** — der Isolationstest bezieht sich auf die Prozessliste und nicht auf den Pflegestand |
| **Z1** Kein `now()` | **erfüllt** — ein Uhrenschlag je Anfrage, aus der Anwendungsuhr |

> ### Regelbezug zu E‑161 *(16.09.2026)* — im Wortlaut von [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md)
>
> | Regel | Wortlaut | Stand |
> |---|---|---|
> | **M1** | *„Kein Endpunkt nimmt eine Mandanten-ID entgegen. Nicht als Pfadsegment, nicht als Query-Parameter, nicht im Body, nicht im Header. Der Mandant wird ausschließlich aus der Session gelesen."* | **erfüllt** — der Endpunkt hat einen Parameter **weniger**; `?mandant=` bleibt wirkungslos, und `?verteilung=` ist es jetzt auch (`verteilung_ist_wirkungslos`) |
> | **M2** | *„Jede Repository-Methode bekommt den Mandanten als ersten Pflichtparameter. Es gibt keine Überladung ohne ihn."* | **erfüllt** — `DashboardRepository.verteilung(MandantContext, …)` ist unverändert und wird zweimal gerufen; keine neue Methode. `PaketstrukturTest` grün |
> | **M4** | *„Pro Endpunkt existiert ein automatisierter Isolationstest, der mit Mandant A abfragt und nachweist, dass Daten von Mandant B unerreichbar sind."* | **erfüllt, je Statement einzeln** — `richtungssicht_ist_getrennt` und `partnersicht_ist_getrennt`; die Verletzungsprobe nur im Richtungsstatement fällt genau den ersten (§9) |
> | **L7** | *„Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen (`EXPLAIN` plus Laufzeit). Kein Statement geht ungeprüft in Produktion."* | **erfüllt** — M178 (§8): `EXPLAIN` über das gerenderte Statement und Laufzeit, je `NEXANS` und `SUTTONS`, vier Lagen, zwei Läufe; Tor gehalten |
> | **L10** | *„Jeder Befundsatz trägt zwei Zeilen: was gemessen wurde und was behauptet wird."* | **erfüllt** — beide Befunde in M178 tragen ihren Vermerk; der zweite benennt, dass die Nachrechnung gegen M145 die Behauptung **nicht** belegt |
> | **T1** | *„Wo eine Laufzeiteigenschaft geprüft werden soll, wird die Ursache geprüft und nicht die Uhr: die Zahl der Datenbankzugriffe, der `EXPLAIN`-Plan, der Treiberindex, der abgesetzte Statement-Text. Eine Dauer darf gemessen und ausgegeben werden; in eine Zusicherung gehört sie nicht."* | **erfüllt für diesen Schritt** — Plan und Statement-Text statt Zeit, und im Frontend Züge der Warteschlange statt Wartezeit. ⚠️ **Ein Altbefund ist dabei aufgefallen:** `keine_mandanten_id` hängt über `alterSekunden` an der Anwendungsuhr — offener Punkt **182**, nicht repariert |
> | **V1** | *Nicht in `DEVELOPMENT_GUIDELINES.md` geführt.* Der Auftrag nennt die Regel mit der Kurzfassung *„Was ein Dokument über den Code sagt, wird am Code nachgesehen und nicht übernommen"* und verweist für den Wortlaut auf die Richtlinien; dort steht sie nicht — gesucht nach `V1` und nach dem Wortlaut, nur Verweise in [`dashboard-frontend.md`](dashboard-frontend.md) §6 und oben in §9a. | **angewandt** — der Mechanismus des Neuladens ist am Code und in einem Wegwerf-Test belegt (§9b), die Voreinstellungen von `nuqs` 2.9.2 (`shallow: true`, `clearOnDefault: true`) in den Typdeklarationen nachgesehen, die Pläne vor dem Plantest erhoben. **Die fehlende Fundstelle ist gemeldet und nicht still ergänzt** |

---

## 11. Offene Punkte

| Nr. | Punkt |
|---|---|
| **82** | **Der Indexhinweis aus §7a ist der erste des Projekts, und er ist eine Wette auf die Statistik.** `IGNORE INDEX FOR ORDER BY (MessageLastUpdateIDX)` nimmt dem Optimierer eine Möglichkeit, die er heute falsch bewertet. Ändert sich die Verteilung der Fehlerzeilen — etwa weil ein Mandant dauerhaft viele hat —, könnte der Zeitindex wieder die bessere Wahl sein, und der Hinweis stünde dann im Weg. **Gemessen ist der heutige Bestand**, nicht der von morgen. Einen Wächter dagegen gibt es nicht, wohl aber `MessungM108DbIT`: Wer die Zahlen nachmisst, sieht es |
| **83** | **Der Aufwand von „Zuletzt aufgefallen" hängt an der Zahl der auffälligen Zeilen im *Gesamtbestand*, nicht im Fenster.** 822 Fehlerzeilen und 538 offene heute, konstante rund 30 ms. In einem Bestand mit hunderttausend Fehlerzeilen wächst er mit — dann wäre ein Index auf `(MessageStatus, MessageLastUpdate)` die Antwort, und der läge auf `GlassfishDB` (Regel S1). **Gerechnet, nicht gemessen** |
| **84** | **Die Kachel *Überfällig* kostet doppelt so viel wie M90 sagt** (§8, Abweichung 1). Die Erklärung — M90 hat gegen den falschen Anker gemessen — ist plausibel und **nicht nachgemessen**. Eine Nachmessung von M90 am geltenden Anker steht ohnehin aus |
| **85** | **Acht von zehn Mandanten sind ungemessen.** Gemessen sind `NEXANS` und `SUTTONS` (Regel L7). `IBIS` ist der Mandant, bei dem der Verteilungsblock am ungünstigsten aussieht — flache Verteilung, „Übrige (40)" als größter Balken (M98, Befund 21). **Die Gestaltung wird ihn brauchen, die Laufzeit nicht** |
| **86** | **Der Leerzustand ist nicht unterscheidbar** (§6, bekannte Grenze 2). Gewollt, und hier nur benannt, damit es nicht als Fehler gemeldet wird |
| **87** | **Die Kachel *Nachrichten* zählt Aktivität und nicht Nachrichten** (§2, bekannte Grenze 3). Ebenfalls gewollt und ebenfalls nur benannt |
| **130** | **„Hängt hier etwas zu lange?" ist seit E‑71 unbeantwortet.** *Läuft* und *Wartend* zählen einen Zustand und liefern das Alter der ältesten Zeile — **ohne Schwelle**. Eine Schwelle steht nirgends in den Daten; `MessageTimeout` ist es nachweislich nicht (§5). Sie zu erfinden verbietet Regel Q4 — es ist wörtlich die Lage, in der *Unquittiert* mit E‑d gestorben ist. **Als offener Punkt eingetragen und nicht gebaut** |
| **134** | **Der Verteilungsblock ist bei `SUTTONS` um 27 ms teurer geworden, ohne dass eine Zeile daran geändert wurde** (§8). Bestand, Rollup und Katalogstand sind nachweislich unverändert. **Die Ursache ist nicht gemessen**; plausibel ist der Zustand der Instanz, belegt ist er nicht. Wer M145 nachmisst, sieht, ob es bleibt. ***Nachtrag 16.09.2026 (M178):*** *Es bleibt — die Partnerform kostet bei `SUTTONS` über zwölf Monate 66,9 bis 67,0 ms, die Richtungsform 66,4 bis 66,5 ms. Dafür ist die Verteilung bei `SUTTONS` über 48 Stunden auf knapp die Hälfte von M108 gefallen und über 30 Tage um 60 % gestiegen (§8). Die Ursache ist weiterhin nicht gemessen, und dieser Schritt behandelt den Punkt ausdrücklich nicht* |
| **182** | **`DashboardIsolationDbIT.keine_mandanten_id` hängt an der Wanduhr.** Der Test vergleicht zwei **ganze** Antwortrümpfe, und seit Schritt 10d steht darin `plattform.dienste[*].alterSekunden`, gerechnet gegen die im Profil `dev` weiterlaufende Anwendungsuhr. Liegen die zwei Aufrufe über einer Sekundengrenze, fällt er — beobachtet am 16.09.2026 in der Verletzungsprobe (§9), mit sechs Lampen um je eine Sekunde als **einzigem** Unterschied; im grünen Lauf davor bestand er. Keine Zusicherung über eine Dauer, aber dieselbe Folge, gegen die Regel T1 steht: ein Test, der zufällig rot wird. **Nicht repariert** — der naheliegende Eingriff wäre, den Block `plattform` aus dem Vergleich zu nehmen oder nur die mandantenabhängigen Blöcke zu vergleichen, wie `verteilung_ist_wirkungslos` es tut. Das ist eine eigene Änderung an einem bestehenden Test und gehört entschieden, nicht nebenbei gemacht. ***Nachtrag 18.09.2026 (Fehler live):*** *in beiden Läufen des Tages gefallen, beide Male ausschließlich `alterSekunden` — die drei Aufrufe des Tests dauerten zusammen 2,6 und 4,2 Sekunden; dass er grün wird, ist inzwischen der Zufall und nicht die Regel* |
| **135** | **Die Isolation der Kachel *Läuft* ist lokal nicht nachweisbar.** `RUNNING` kommt auf der Testkopie null Mal vor; jeder Mandant sieht `0`, mit und ohne Mandantenfilter. Der Nachweis ruht auf dem **gerenderten Statement** (`DashboardStatementsTest`) und nicht auf Daten. **Gegen die Produktion nachzuholen** |

### Und was hier geschlossen wird

*Die beiden ersten Zeilen sind am **03.09.2026 mit Schritt 10b‑5** nachgetragen, nachdem die
Oberfläche nachgezogen war.*

| Nr. | Woher | Stand |
|---|---|---|
| **131** | [`nachrichtenliste.md`](nachrichtenliste.md) §5e | **erledigt** — die Landingpage war zwischen dem 03.09.2026 und Schritt 10b‑5 im Browser defekt, weil sie `kacheln.ueberfaellig` las. Sie liest jetzt `laeuft` und `wartend`, der Verweis auf die entfallene Abfrageform ist fort, und ein alter Link `?ueberfaellig=true` lädt ohne Fehler ([`dashboard-frontend.md`](dashboard-frontend.md) §5.4) |
| **133** | dieser Datei, `Auffaelligkeit` | **erledigt** — die Aufzählung hat weiterhin **einen** Wert, und das Feld `kategorie` bleibt im Vertrag (Regel Q3). Was den Punkt offen hielt, war die Oberfläche: Sie las ihn und durfte nicht an einem fehlenden Feld brechen. **Sie zeichnet ihn seit 10b‑5 nicht mehr** — eine Plakette, die an jeder Zeile dasselbe Wort sagt, unterscheidet nichts. Was daraus für die Anzeige folgt, ist als Punkt **136** in [`dashboard-frontend.md`](dashboard-frontend.md) §9 benannt |
| **61** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **erledigt** — die Schwelle ist festgelegt, beide Bedingungen sind gebaut, und die zweite ist als gewählte und nicht gemessene Zahl benannt (§3) |
| **62** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **erledigt** — der Block verträgt fehlende Restzeilen und hält beide unten (§4) |
| **58** | [`messungen-schritt10b.md`](messungen-schritt10b.md) | **berührt, nicht erledigt.** Er verlangt, `SUTTONS` vor der Abnahme von 10b zu kuratieren. Die Katalogpflege ist mit Teil A freigegeben ([`prozess-katalog.md`](prozess-katalog.md) §1); dass sie geschieht, ist eine Handlung des Auftraggebers und keine Codeänderung |

> ### Nachgesehen und **nicht** als Punkt eingetragen *(03.09.2026, Schritt 10b‑5)*
>
> **Was liefert der Endpunkt, wenn die *Erscheinungsbedingung selbst* nicht auswertbar ist?** Fiele
> der Schlüssel `wartend` dann weg, würde ein Ausfall stillschweigend in die strukturelle Behauptung
> *„dieser Mandant wartet nie"* übersetzt — der schlimmste der drei denkbaren Fehler an dieser
> Stelle ([`dashboard-frontend.md`](dashboard-frontend.md) §5.4, E‑81).
>
> **Er tut es nicht, und §5 sagt es ausdrücklich:** Die Erscheinungsbedingung bekommt **keinen**
> Teilerfolg-Mechanismus. Sie liest Stammdaten und ist damit dieselbe Art Zugriff wie die
> Mandantenkette in jedem anderen Statement; bricht sie, ist die **ganze Antwort** ein Fehler.
> **Kein offener Punkt** — die Frage ist beantwortet, bevor sie die Oberfläche erreicht.

> **Warum die Vermerke zu 58, 61 und 62 hier stehen und nicht dort, wo die Punkte vergeben sind:**
> [`messungen-schritt10b.md`](messungen-schritt10b.md) ist eine Messdatei und in diesem Schritt
> ausdrücklich nicht anzufassen. Dasselbe Verfahren hat Schritt 10b‑1 für Punkt 55 gewählt.

---

## 12. Was dieser Schritt nicht zeigt

1. **Keine Oberfläche.** Kein Diagramm, keine Kachel, kein Umschalter, keine Farbe.
2. **Nichts über die Produktion.** Alle Zahlen stammen von der Testkopie.
3. **Den Kaltlauf nicht.** `FLUSH TABLES` steht `monitor_read` nicht zu; gemessen ist der Warmfall.
4. **Nichts über die acht übrigen Mandanten.** Gemessen sind `NEXANS` und `SUTTONS` (Regel L7).
5. **Nicht, ob die Kategorie *Überfällig* fachlich richtig geschnitten ist.** `RUNNING` kommt auf der
   Testkopie null Mal vor; gemessen sind Plan und Laufzeit, nicht die fachliche Größenordnung.

   > **Nachtrag 03.09.2026:** Sie war es nicht. Die Frage ist am 03.09.2026 durch eine fachliche
   > Auskunft des Auftraggebers entschieden worden — **gegen die Kategorie** (E‑71). Der Satz oben
   > bleibt stehen: Er benennt genau die Lücke, die diese Messrunde nicht schließen konnte, und die
   > geschlossen worden ist, ohne dass eine Messung sie geschlossen hätte.
6. **Nicht, ob die Auskunft stimmt, die *Überfällig* gestürzt hat.** Sie ist **nicht gemessen**;
   die offene Prüfung dafür steht in [`message-status.md`](message-status.md).
