# Prozessauswahl

Entsteht in Schritt 4, Aufgabe 12. Ein kleiner Endpunkt mit einem klaren Zweck: die Liste, aus der
der Nutzer seinen Prozessfilter wählt.

---

## 1. Warum es ihn gibt

`/api/nachrichten` nimmt `prozess` mehrfach entgegen ([`nachrichtenliste.md`](nachrichtenliste.md)
§1) — aber die Menge, aus der zu wählen ist, stand nirgends.

Ohne diesen Endpunkt müsste die Oberfläche die Prozesse aus den gerade angezeigten Zeilen
zusammensammeln. Der Filter zeigte dann nur, was ohnehin schon zu sehen ist, und **genau der Prozess,
der nichts geliefert hat, fehlte in der Auswahl** — obwohl er der interessante Fall ist, wenn jemand
wissen will, warum nichts ankommt.

---

## 2. Der Endpunkt

```
GET /api/prozesse
```

Angemeldet, Mandant aus der Sitzung. **Kein Parameter** — weder für den Mandanten noch für sonst
etwas.

### Antwort

```json
[
  { "processId": "…", "processName": "40000_AMG_LAB_VDA", "projectName": "300_KundenEingehend" },
  { "processId": "…", "processName": "40001_AMG_LAB_OUT",  "projectName": "300_KundenEingehend" }
]
```

Sortiert nach `ProjectName`, darin nach `ProcessName`. `ProjectName` gruppiert die Auswahl,
`ProcessName` ordnet innerhalb der Gruppe.

**Drei Felder und kein viertes.** Die Auswahl beantwortet genau eine Frage — „welchen Prozess meinst
du" — und dafür braucht der Nutzer den Namen und die Gruppe, in der er steht. Zahlen (wie viele
Nachrichten, wie viele Fehler) gehören nicht hierher: Sie wären eine Aggregation über `Message` ohne
Zeitfenster und damit genau das, was die Regeln L1 und L2 ausschließen.

`processName` und `projectName` dürfen `null` sein — die Spalten lassen es zu. Was der Nutzer
anstelle einer fehlenden Zuordnung liest, ist eine Oberflächenentscheidung und gehört in die
Sprachdateien, nicht in eine Abfrage (Regel Q4). In der Testkopie kommt der Fall nicht vor: Alle
1.503 Prozesse tragen einen Namen und ein Projekt.

### Fehlerfälle

| `type` | Status | Wann |
|---|---|---|
| `nicht-angemeldet` | 401 | keine Sitzung |
| `kein-mandant-gewaehlt` | 403 | angemeldet, aber kein aktiver Mandant |

Mehr gibt es nicht — es gibt keinen Parameter, der falsch sein könnte.

---

## 3. Keine Paginierung, kein Zeitfenster

Das ist **keine Nachlässigkeit gegenüber den Regeln L1 und L3, sondern ihre Anwendung.** Beide gelten
für `Message` mit seinen 3,3 Millionen Zeilen. Hier sind es Stammdaten:

| | Zeilen |
|---|---|
| `Process` insgesamt | **1.503** |
| größter Mandant (`NEXANS`) | **733** |
| kleinste Mandanten (`WOC`, `SYSTEM`) | **4** |

Vollständig, Stand 06.08.2026:

| `MandantID` | Prozesse | Projekte |
|---|---|---|
| `NEXANS` | 733 | 17 |
| `VOTG` | 390 | 39 |
| `IBIS` | 192 | 46 |
| `IBISGUS` | 89 | 19 |
| `ZAST` | 35 | 4 |
| `SUTTONS` | 17 | 1 |
| `NXHBE` | 17 | 2 |
| `EDITIONLINGERI` | 9 | 3 |
| `WOC` | 4 | 1 |
| `SYSTEM` | 4 | 2 |

**Ein Cursor** auf einer Liste, die vollständig in ein Auswahlfeld passt, wäre Aufwand ohne
Gegenwert. **Ein Zeitfenster** wäre schlimmer als überflüssig: Es blendete ausgerechnet die stillen
Prozesse aus — also die, nach denen jemand sucht, wenn nichts ankommt.

> **Nebenbefund.** `information_schema` nennt für `Process` 1.490 Zeilen, gezählt sind es **1.503**.
> Das ist dieselbe Veralterung der Statistiken wie in
> [`messungen-schritt4.md`](messungen-schritt4.md), Auffälligkeit G. Für die Größenordnung ändert
> das nichts.

---

## 4. Die Mandantenkette — hier als Join, nicht als `EXISTS`

```sql
SELECT p.ProcessID, p.ProcessName, pr.ProjectName
FROM Process p
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN Project pr   ON pr.ProjectID = p.ProjectID
WHERE pm.MandantID = :mandant
ORDER BY pr.ProjectName, p.ProcessName
```

Der Filter ist **Bestandteil des Statements** (Regel M3). Dass er hier als Join steht und in der
Nachrichtenliste als `EXISTS`, ist kein Widerspruch, sondern folgt aus der Richtung des Zugriffs:

- **In der Liste** treibt das Zeitfenster über `Message`. Ein Join auf das im Schema n:m stehende
  `ProjectMandant` könnte dort Zeilen vervielfachen, sobald ein Projekt jemals mehreren Mandanten
  gehörte — und eine Liste, deren Zeilenzahl an einer Stammdatenpflege hängt, ist die falsche
  Grundlage für eine Sicherheitsgrenze.
- **Hier** ist `ProjectMandant` der *selektivste* Teil der Bedingung und soll den Zugriff treiben.
  Vervielfachen kann er nichts: Der Primärschlüssel ist `(ProjectID, MandantID)`, und mit
  `MandantID = ?` bleibt je Projekt höchstens eine Zeile übrig. Das gilt **aus dem Schema heraus**
  und nicht erst aus den Daten.

**`Project` als `LEFT JOIN`:** `Process.ProjectID` ist `NULL`-fähig. Ein innerer Join ließe einen
Prozess ohne Projekt lautlos verschwinden. Er trägt dann zwar keinen Mandanten und ist ohnehin für
niemanden sichtbar (Annahme A8), aber die Sichtbarkeit soll an der Mandantenkette hängen und nicht
am Anzeigenamen.

**Prozesse ohne Projektnamen stehen vorn** — `NULL` sortiert in MariaDB aufsteigend zuerst. Sichtbar
und nicht am Ende versteckt.

### Warum kein Service

Zwischen HTTP und SQL ist hier nichts zu entscheiden: kein Zeitfenster aufzulösen, kein Suchbegriff
vorzufiltern, keine Rohwerte einzuordnen. Eine Service-Klasse wäre eine Weiterleitung mit eigenem
Dateinamen. Kommt später Fachlogik dazu — etwa die Anreicherung um den kuratierten Partner aus
`process_catalog` in Schritt 9 —, entsteht sie an dieser Stelle.

### Warum im Paket `catalog` und nicht in `message`

Der Endpunkt liefert **Stammdaten**, und `catalog` ist das Paket dafür. In `message` läge er falsch:
Schritt 9 (`process_catalog`) und Schritt 10 (Prozessansicht) bauen auf genau dieser Tabelle auf und
müssten dann aus einem Nachbarpaket importieren — was die Regel „Fachpakete kennen einander nicht"
verbietet.

Im Frontend liegt der Aufruf dagegen in `features/nachrichten`, weil er dort und nur dort gebraucht
wird; ein eigenes Feature `prozesse` entstünde sonst allein für einen Fetch und müsste sofort von
`nachrichten` importiert werden — dieselbe Regel, andere Seite.

---

## 5. Der Isolationstest (Regel M4)

`ProzesseIsolationDbIT`, Mandant A ist `NEXANS`, Mandant B ist `SUTTONS` — dieselbe Paarung wie in
`NachrichtenIsolationDbIT`: zwei verschiedene Häuser und die beiden größten Bestände, also die
Paarung, bei der ein Leck am ehesten sichtbar würde.

**Die Gegenprobe sieht hier anders aus als in der Vorlage.** `MandantenIsolationDbIT` und
`NachrichtenIsolationDbIT` stellen einer fremden, echten Kennung eine erfundene gegenüber und
verlangen ununterscheidbare Antworten. Das setzt einen Endpunkt voraus, der eine Kennung
*entgegennimmt* — dieser nimmt **überhaupt keinen Parameter**. Es gibt damit keine Eingabe, über die
sich Existenz erfragen ließe, und die Gegenprobe verschiebt sich auf die **Ausgabe**.

Geprüft wird:

1. Beide Mandanten haben überhaupt Prozesse — die Voraussetzung, ohne die alles Folgende wertlos
   wäre.
2. Der Nutzer auf `NEXANS` bekommt keine fremde `ProcessID` **und keinen fremden `ProcessName`**;
   beides kommt im Antwortrumpf überhaupt nicht vor. Der Name zählt mit: Über ihn ließe sich die
   Prozesslandschaft eines fremden Mandanten ebenso ablesen wie über die Kennung, und der
   Freitextfilter der Liste macht Namen unmittelbar verwertbar.
3. Die Trennung gilt in **beide** Richtungen.
4. **Auswahl und Listenfilter passen zusammen** (Regel M5, die Trennung gilt auch quer): Was die
   Auswahl anbietet, muss die Liste auch beantworten. Wäre die Auswahl weiter gefasst, führte sie
   den Nutzer auf garantiert leere Ergebnisse; wäre sie enger, fehlten ihm Prozesse, die er sehen
   darf.
5. **Auch ADMIN sieht nur den aktiven Mandanten.** Ohne gewählten Mandanten ist die Antwort `403`
   `kein-mandant-gewaehlt` — nicht etwa alle Prozesse. Nach dem Wechsel auf `NEXANS` sieht er exakt
   das, was ein `NEXANS`-Nutzer sieht.

**Kein Zeitfenster im Test**, denn der Endpunkt hat keines. Der Test ist damit unabhängig vom
Datenstand der Testkopie — anders als bei der Nachrichtenliste gibt es hier keinen Weg, an dem ein
leeres Fenster den Beweis aushöhlen könnte.

**Dieser Endpunkt gehört nicht zu den Ausnahmen von Regel M1.** Die Liste in
[`mandantentrennung.md`](mandantentrennung.md) §3 bleibt bei zwei Einträgen.

---

## 6. Messung (Regel L7)

Gemessen am 06.08.2026 gegen die Testkopie, serverseitig über `SET profiling = 1`, beste von fünf
Läufen nach einem Aufwärmlauf. Vollständig als **L12** in
[`messungen-schritt4.md`](messungen-schritt4.md).

**EXPLAIN**

| id | select_type | table | type | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `ProjectMandant` | `ref` | `ProjectMandant_Mandant_idx` | 146 | `const` | 17 | `Using where; Using index; Using temporary; Using filesort` |
| 1 | SIMPLE | `Process` | `ref` | `Process_ProjectFK` | 147 | `ProjectMandant.ProjectID` | 5 | — |
| 1 | SIMPLE | `Project` | `eq_ref` | `PRIMARY` | 146 | `ProjectMandant.ProjectID` | 1 | — |

| Mandant | Prozesse | Laufzeit |
|---|---|---|
| `NEXANS` | 733 | **4,225 ms** (Aufwärmlauf 4,705 ms) |
| `SUTTONS` | 17 | **0,606 ms** (Aufwärmlauf 0,618 ms) |

Der Einstieg läuft über `ProjectMandant_Mandant_idx` — der Mandant ist der selektivste Teil der
Bedingung, genau wie bei der Vorfilterung des Suchbegriffs (L7a). `Using temporary; Using filesort`
ist hier **kein Befund**: Sortiert werden ein paar hundert Zeilen, und für `ORDER BY ProjectName,
ProcessName` gibt es keinen Index, der das bediente. Bei 733 Zeilen kostet das vier Millisekunden.

**Was zu beobachten ist:** Die Laufzeit wächst mit der Prozesszahl des Mandanten, nicht mit dem
Gesamtbestand. Bekäme ein Mandant je fünfstellig viele Prozesse, wäre das die Stelle, an der man
nachsieht — und dann wäre auch die Auswahl in der Oberfläche die falsche Bauform, nicht erst die
Abfrage.

---

## 7. Der Zwischenspeicher im Frontend

Das Ergebnis wird **länger gehalten als die Nachrichtenliste** — Stammdaten ändern sich selten, und
die Auswahl steht neben einer Liste, die sich unter Umständen jede Minute aktualisiert.

**Beim Mandantenwechsel wird geleert, nicht invalidiert** (bestehende Regel,
[`frontend-grundlagen.md`](frontend-grundlagen.md) §5). Und der **Prozessfilter selbst wird
zurückgesetzt**: Die `ProcessID`s des alten Mandanten sind für den neuen bedeutungslos, und ein
stehengebliebener Filter erzeugte eine dauerhaft leere Liste, deren Ursache niemand sieht.

---

## 7a. Die aufgeklappte Auswahl braucht Platz

Nachgetragen am **07.08.2026** (Nachbesserung zu Schritt 4, Aufgabe 7). Beim Durchklicken durch den
Auftraggeber waren die Prozessnamen abgeschnitten — und bei 733 Einträgen ist die Liste ohne
vollständige Namen nicht bedienbar.

| | vorher | nachher |
|---|---|---|
| Breite der aufgeklappten Liste | 20 rem | **34 rem**, begrenzt durch `calc(100vw - 2rem)` |
| Breite des geschlossenen Felds | 13 rem | 13 rem |
| lange Namen | gekürzt | **umgebrochen** |
| abgeschnittene Einträge (gemessen, `NEXANS`) | — | **0 von 733** |

**Umbrechen statt kürzen.** In der Nachrichtenliste gilt die feste Zeilenhöhe, weil dort 50 Zeilen
überflogen werden; hier wird *ausgewählt*. Ein gekürzter Name macht die Auswahl mehrdeutig, sobald
sich zwei Prozesse erst hinter dem Schnitt unterscheiden — und genau das ist bei Namen wie
`… Lieferschein (VDA)` neben `… Lieferschein (EDIFACT)` der Regelfall. Die Kürzung mit Tooltip war
als letztes Mittel vorgesehen und wird nicht gebraucht.

Die Zahlen dazu: 733 Prozesse, längster Prozessname **58 Zeichen**, längster Projektname 44, im
Schnitt 21,8.

**Das Eingrenzungsfeld bleibt** — es ist bei 733 Einträgen der schnellere Weg als Scrollen, und es
arbeitet weiterhin rein örtlich (§9).

### Kein Virtualisieren — gemessen, nicht vermutet

Die 733 Einträge stehen als **4.399 DOM-Knoten** in einem Bereich von **32.288 px** Höhe bei 288 px
Sichtfenster. Gemessen im Browser über 30 Sprünge à 1.000 px, jeweils mit erzwungenem Layout
(`getBoundingClientRect` auf Container und letztes Kind):

| | |
|---|---|
| Summe über 30 Sprünge | **0,3 ms** |
| schlechtester Einzelwert | **0,1 ms** |

Der Browser rechnet die Liste nicht neu — sie steht, und es wird nur der Ausschnitt verschoben. Mit
dem Mausrad ist kein Ruckeln zu sehen. **Ruckelt es nicht, bleibt es beim Einfachen.**
Virtualisierung wäre der erste Umbau, wenn ein Mandant je vierstellig viele Prozesse bekommt — dann
allerdings zusammen mit dem serverseitigen Suchparameter aus §9, denn beides hat dieselbe Ursache.

---

## 8. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** kein Endpunkt nimmt eine Mandanten-ID | kein Parameter; die Ausnahmenliste bleibt bei zwei |
| **M2** Mandant als erster Pflichtparameter | `ProzessRepository.findeAlle(MandantContext, …)`; ArchUnit prüft es |
| **M3** Filter im Statement | Join auf `ProjectMandant`, Teil der Bedingung |
| **M4** Isolationstest je Endpunkt | `ProzesseIsolationDbIT` |
| **M5** Trennung gilt auch quer | Testfall 4: Auswahl und Listenfilter decken sich |
| **L1/L3** Zeitfenster und Cursor | gelten für `Message`; hier Stammdaten, siehe §3 |
| **L7** jede Abfrage gemessen | §6 und [`messungen-schritt4.md`](messungen-schritt4.md), L12 |
| **Q4** nicht zugeordnet heißt nicht zugeordnet | `processName`/`projectName` bleiben `null` |

---

## 9. Offene Punkte

- **Keine Suche in der Auswahl.** Bei 733 Prozessen (`NEXANS`) ist eine reine Liste an der Grenze
  des Zumutbaren. Die Mehrfachauswahl in der Oberfläche filtert deshalb im Speicher über den
  gelieferten Namen — ein serverseitiger Suchparameter würde den Endpunkt um genau die Fallstricke
  erweitern, die den Freitextfilter der Liste teuer machen, und ist bei dieser Zeilenzahl nicht
  nötig.
- **Kein kuratierter Partner.** `process_catalog` entsteht in Schritt 9. Bis dahin gruppiert die
  Auswahl nach `ProjectName` — dem einzigen Gruppierungsmerkmal, das die Quelle hergibt, ohne dass
  etwas geraten werden müsste.
- **Keine Angabe, ob ein Prozess im gewählten Zeitfenster überhaupt Zeilen hat.** Das wäre die
  hilfreichste Zusatzinformation der Auswahl und zugleich genau die Aggregation über `Message`, die
  Regel L2 verbietet. Ab Schritt 10 könnte sie aus `message_rollup` kommen.
