# Statuskatalog `Message.MessageStatus`

Erhebung gegen die Testkopie, vorgezogen aus Schritt 3 (die Verbindung steht in Schritt 2 ohnehin).
Die Einordnung entsteht **ausschließlich** in `common/MessageStatusClassifier` und wird nirgends
nachgebaut — sie wird in Liste, Dashboard, Rollup und später im Chatbot verwendet.

`MessageStatus` ist **freier Text, kein Aufzählungstyp.** `CKECKED` ist der Beweis (ein nie
korrigierter Tippfehler in den Produktivdaten). Unbekannte Werte werden zu `UNGEKLAERT`, niemals zu
einem geratenen Wert.

---

## Erhobene Werte (seit 01.01.2025, Stand der Projektbeschreibung)

| Status | Anzahl | Einordnung (`MessageStatusKind`) | Anzeige |
|---|---|---|---|
| `FINISHED` | 1.663.884 | `ABGESCHLOSSEN` | grün |
| `MERGED` | 607.277 | `ZUSAMMENGEFUEHRT` | neutral |
| `SPLITTED` | 310.263 | `AUFGETEILT` | neutral |
| `EERP_RECEIVED` | 116.828 | `QUITTIERT` | grün |
| `COMMIT_RECEIVED` | 10.126 | `QUITTIERT` | grün |
| `COMMIT_SENT` | 979 | `UNGEKLAERT` | neutral |
| `ERROR_DUPLICATE` | 659 | **`FEHLER`** | rot |
| `SUSPENDED` | 538 | `WARTEND` | neutral |
| `CHECKED` | 257 | `UNGEKLAERT` | neutral |
| `COMMIT_REJECTED` | 111 | **`FEHLER`** | rot |
| `ERROR_TIMEOUT` | 52 | **`FEHLER`** | rot |
| `CKECKED` | 2 | `UNGEKLAERT` (Tippfehler) | neutral |
| `RUNNING` | 0 | `LAEUFT` | neutral |

13 dokumentierte Werte. Die `MessageStatusKind`-Einordnung ist in `MessageStatusClassifier`
hinterlegt.

### `ZWISCHENSCHRITT` ist zwei Werte geworden (11.08.2026, Schritt 6, Teil 2a)

| alt | neu | Rohwert |
|---|---|---|
| `ZWISCHENSCHRITT` | **`AUFGETEILT`** | `SPLITTED` |
| `ZWISCHENSCHRITT` | **`ZUSAMMENGEFUEHRT`** | `MERGED` |

**Technisch waren beide dasselbe, fachlich bedeuten sie Gegenteiliges:** *aus eins wurde viel* gegen
*aus viel wurde eins*. Ein gemeinsamer Eimer verschluckt genau den Unterschied, den die Verkettung
sichtbar macht ([`verkettung.md`](verkettung.md) §1) — `MERGED` ist der **Eingang** einer
Zusammenführung und zeigt auf das Ergebnis, `SPLITTED` ist die **Wurzel** einer Aufteilung und wird
von ihren Kindern rückverwiesen (M23‑2, M25‑1).

**`istEndstatus` liefert für beide weiterhin `true`.** An der Überfälligkeitsrechnung ändert sich
**nichts**; M6 stützt das unverändert (siehe unten). Das vollständige `switch` ohne `default` hat
erzwungen, dass jede Stelle im Code bewusst nachgezogen wurde, statt eine stille Voreinstellung zu
erben — genau der Zweck, für den es dort steht.

**Das Wort „Zwischenschritt" kommt in keiner Sprachdatei und in keiner Oberflächenzeichenkette mehr
vor.** Der Statusfilter der Liste bietet beide Werte einzeln an
([`nachrichtenliste.md`](nachrichtenliste.md) §5).

> ⚠️ **Der Abstrich, der dazugehört: Der Status sagt über die Stellung in der Kette nichts
> Verlässliches.** Bei `IBIS`, `IBISGUS` und `ZAST` trägt die Wurzel `FINISHED` (M24‑3) — und das
> sind gerade die drei Mandanten, die über den ganzen Bestand **keine einzige** `SPLITTED`- oder
> `MERGED`-Zeile haben. Verlässlich ist die Spalte `Source`, nicht `MessageStatus`. Die Liste zeigt
> trotzdem nur den Status: Eine Rollenkennzeichnung dort ist Entscheidung des Auftraggebers und
> unterbleibt; die Rolle erscheint im Detail. Der Punkt steht als bekannter Abstrich in
> [`nachrichtenliste.md`](nachrichtenliste.md) §5 — **nicht als Fehler**.
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen (M24‑3, Fenster B + Überhang):* Bei `IBIS` (1.714), `IBISGUS` (22) und `ZAST` (20)
> > tragen die Elternzeilen `FINISHED`; bei `NEXANS` (27.644) und `SUTTONS` (640) tragen sie
> > `SPLITTED`.
> > *Behauptet ist:* Der Status ist über die Kette hinweg **unzuverlässig** — die Liste sagt bei
> > `NEXANS` etwas über die Aufteilung und bei drei Mandanten nichts.
> > **Die Lücke:** Gemessen sind fünf Mandanten in einem Monatsfenster, behauptet ist eine Aussage
> > über den Bestand. Sie trägt trotzdem, weil sie nur einen **Gegenbeleg** braucht und nicht eine
> > Quote: Ein einziger Mandant, dessen Wurzel `FINISHED` trägt, widerlegt „der Status benennt die
> > Stellung". Drei sind gemessen.

## Nachprüfung am 28.07.2026 (unbefristet, ganze Testkopie)

`SELECT DISTINCT MessageStatus FROM Message` lieferte **12 Werte** — exakt die obige Menge **ohne
`RUNNING`**. Kein neuer, undokumentierter Wert. Die Anzahlen liegen höher als oben (die Testkopie
reicht jetzt bis 08.07.2026), aber die *Menge* der Statuswerte ist unverändert.

---

## Anmerkungen, die mitzudenken sind

- **`COMMIT_REJECTED` zählt als Fehler**, obwohl der Wert **nicht** mit `ERROR_` beginnt. Der Partner
  hat die Übertragung abgelehnt, der Beleg ist nicht angekommen. Damit ist Annahme A6 der
  Projektbeschreibung widerlegt (siehe `annahmen-korrekturen.md`).
- **`RUNNING` kommt in der Testkopie null Mal vor**, in der Produktion aber sehr wohl. Der Status ist
  flüchtig und existiert nur, solange eine Nachricht tatsächlich in Arbeit ist. Folgen:
  - Die Problemkategorie „Überfällig" ist gegen die Testkopie nicht prüfbar.
  - „läuft noch" darf **nicht** als `MessageStatus = 'RUNNING'` definiert werden, sondern als „nicht in
    einem Endstatus".
- **`CHECKED`, `CKECKED` und `COMMIT_SENT`** gelten als bekannt, aber fachlich **ungeklärt**. Sie
  werden neutral behandelt und mit Rohwert plus Hinweis „Bedeutung nicht verifiziert" angezeigt. Es
  wird nichts geraten.

---

## Die Fehlerbedingung — eine Stelle, ein Ausdruck

`MessageStatusClassifier.fehlerBedingung(Field<String>)` liefert:

```sql
MessageStatus LIKE 'ERROR\_%' ESCAPE '\' OR MessageStatus = 'COMMIT_REJECTED'
```

**Nicht** `LEFT(MessageStatus, 6) = 'ERROR_'`: In SQL ist `_` ein Platzhalter für ein beliebiges
Zeichen, und die `LEFT`-Form kann `MessageStatusIDX` nicht nutzen — sie erzwingt in Kombination mit
der Oder-Bedingung einen vollen Durchlauf über 2,9 GB. Die `LIKE`-Fassung ergibt zwei Indexbereiche,
die MariaDB zusammenführen kann.

Das Feld wird als Parameter übergeben, damit dieser gemeinsame Baustein nicht auf generierte
`jooq.glassfish`-Typen zugreifen muss — die Fehlerbedingung bleibt in `common`.

---

## Endstatus und Überfälligkeit (ergänzt 01.08.2026, Schritt 4)

### `istEndstatus(MessageStatusKind)`

> **Offen sind allein `WARTEND` und `LAEUFT`.** Alles andere ist Endstatus.

> ⚠️ **Diese Methode gehört der Überfälligkeitsrechnung und sonst niemandem** (präzisiert am
> 06.08.2026). Sie beantwortet genau eine Frage: *Kann für diese Zeile noch eine Frist ablaufen?*
> Sie ist **nicht** die Grundlage eines späteren Filters „nur offene Nachrichten". Dort würde sie
> 1.051 `COMMIT_SENT`-Zeilen lautlos verschwinden lassen — obwohl weiter oben in dieser Datei steht,
> dass wir über genau diese Zeilen **nichts wissen**. Für `UNGEKLAERT` liefert sie `true`, und das
> ist in der Überfälligkeitsrechnung die vorsichtige Antwort (keine Behauptung, die Nachricht hänge);
> in einem Sichtbarkeitsfilter wäre dieselbe `true` die unvorsichtige. Wer „offen" im Sinne der
> Oberfläche braucht, definiert das dort — und begründet es dort.
>
> ✅ **Der Fall ist am 04.09.2026 eingetreten — und so ist er aufgelöst worden.** Die
> Oberfläche hat „offen" nie definiert. Stattdessen war der **Tokenname** der Farbrolle
> `--status-offen` in die **Beschriftungsposition** von Legende und Tooltip des
> Verlaufsdiagramms gerutscht und behauptete dort über `AUFGETEILT` und `ZUSAMMENGEFUEHRT`
> das Gegenteil der Tabelle darunter — gesehen als *Offen 49* über *Aufgeteilt 1* und
> *Zusammengeführt 48* in einem Eimer mit 60 Nachrichten. **Aufgelöst ist er nicht durch eine
> Definition, sondern durch den Verzicht auf das Wort:** Die Rolle heißt in der Oberfläche
> seither **„Ohne Ergebnis"**, der Token bleibt `--status-offen` (E‑82,
> [`dashboard-frontend.md`](dashboard-frontend.md) §5.2). **`istEndstatus` und diese Tabelle
> sind nicht angefasst.**

| Einordnung | Endstatus? | Rohwerte |
|---|---|---|
| `ABGESCHLOSSEN` | ja | `FINISHED` |
| `QUITTIERT` | ja | `EERP_RECEIVED`, `COMMIT_RECEIVED` |
| `FEHLER` | ja | `ERROR_*`, `COMMIT_REJECTED` |
| `AUFGETEILT` | **ja** | `SPLITTED` |
| `ZUSAMMENGEFUEHRT` | **ja** | `MERGED` |
| `UNGEKLAERT` | **ja** | `CHECKED`, `CKECKED`, `COMMIT_SENT`, alles Unbekannte |
| `WARTEND` | **nein** | `SUSPENDED` |
| `LAEUFT` | **nein** | `RUNNING` |

> **Die Aufteilung am 11.08.2026 hat an dieser Tabelle nichts geändert außer der Zeilenzahl.** Aus
> einer Zeile wurden zwei mit demselben Wert. `MessageStatusClassifierTest` hält beides fest — dass
> beide Endstatus sind und dass keiner von beiden je überfällig wird.

**Warum `AUFGETEILT` und `ZUSAMMENGEFUEHRT` fertig sind.** Eine gemergte oder gesplittete Nachricht wird nicht wieder
angefasst — sie ist als *Zeile* fertig, auch wenn der fachliche Vorgang über die Verkettung
weiterläuft. Messung M6 stützt das: `SPLITTED` und `MERGED` verteilen sich über fünfzehn Monate in
stabiler Größenordnung (38.428 bis 57.426 bzw. 20.609 bis 34.937 je Monat) und ballen sich **nicht**
am aktuellen Rand; wären sie flüchtige Zwischenzustände, sähe die Verteilung anders aus. Zählten sie
als offen, wären **34,38 Prozent aller Zeilen** Kandidaten für „überfällig" — und die Kategorie wäre
Rauschen.

**Warum `UNGEKLAERT` hier als fertig zählt.** Das ist keine Behauptung, die Nachricht sei fertig,
sondern die Weigerung, das Gegenteil zu behaupten: Sie als offen zu führen hieße, wir wüssten, dass
sie *nicht* fertig ist. Genau das wissen wir nicht. So bleibt sie aus **allen drei**
Problemkategorien heraus, statt mit einer Vermutung gefüllt zu werden.

Umgesetzt als vollständiges `switch` ohne `default`: Ein neuer Wert in `MessageStatusKind` löst
einen Compilerfehler aus und erbt keine stille Voreinstellung.

### Überfälligkeit

> ## ⚠️ Diese Kategorie ist am 03.09.2026 widerlegt und aus dem MVP genommen (E‑71)
>
> **Der ganze Abschnitt darunter bleibt im Wortlaut stehen** — er ist die Vorarbeit, gegen die die
> Widerlegung zu lesen ist, und ohne ihn wäre nicht mehr erkennbar, worauf sie sich bezieht.
> **Gebaut ist davon nichts mehr:** `MessageStatusClassifier.istUeberfaellig`,
> `timeoutZeitpunkt` und `ueberfaelligBedingung` sind entfallen, der Listenparameter
> `ueberfaellig` ebenso, und das Dashboard zeigt statt der Kachel *Überfällig* die beiden Kacheln
> *Läuft* und *Wartend* ([`dashboard.md`](dashboard.md)).
>
> ### Die Regel, die sie widerlegt
>
> 1. Eine Nachricht, die länger als `MessageTimeout` in `RUNNING` steht, wird vom Altsystem
>    automatisch auf `ERROR_TIMEOUT` gesetzt.
> 2. `SUSPENDED`-Nachrichten warten **absichtlich** — auf einen Folgeprozess, etwa auf den Versand
>    zu einem bestimmten Zeitpunkt. Sie werden nie automatisch beendet und sind **nicht
>    überfällig**.
> 3. In der Praxis liegen sie höchstens **rund eine Woche**.
>
> ### Herkunft
>
> **Herkunft:** fachliche Auskunft des Auftraggebers vom 03.09.2026. **Nicht gemessen.**
> Die Testkopie kann sie nicht belegen: `RUNNING` kommt dort null Mal vor, und die 538
> `SUSPENDED` sind der Bestand *eines* Status in *einer* Gestalt. **Gegen die Produktion zu
> prüfen** mit der Abfrage in `docs/message-status.md`, Abschnitt „Die offene Prüfung".
>
> ### Was daraus folgt
>
> *Überfällig* ist definiert als **nicht in einem Endstatus** und **Frist abgelaufen**. Offen sind
> genau zwei Statuswerte, und beide fallen weg:
>
> | | |
> |---|---|
> | `SUSPENDED` | wartet absichtlich → nie überfällig. **Falsch positiv, per Definition** |
> | `RUNNING` | über der Frist nur im Spalt zwischen Fristablauf und dem Zuschlagen des Wächters. Danach `ERROR_TIMEOUT` und damit **Fehler** |
>
> **Die Kategorie hat im eingeschwungenen Zustand keine wahren Treffer.** Gemessen an der
> Testkopie markierte `istUeberfaellig` im Gesamtbestand **538** Zeilen, davon **538 `SUSPENDED`**
> und **0 `RUNNING`** — **538 Fehlalarme und kein einziger Treffer.** Nicht die Mehrheit, alle.
>
> **Die Rechnung, die das Werkzeug anstellte, stellt das Altsystem bereits an;** sein Ergebnis
> heißt `ERROR_TIMEOUT` und steht bei uns in der Fehlerkachel, namentlich aufgeschlüsselt. Eine
> zweite Fassung derselben Auskunft, mit anderer Uhr und ohne Schreibrecht, ist keine zusätzliche
> Aussage.
>
> > **Belegvermerk** (Regel L10).
> > *Gemessen ist:* dass `istUeberfaellig` auf der Testkopie 538 Zeilen markiert, davon 538
> > `SUSPENDED` und null `RUNNING` (M90, in M144 am geltenden Anker bestätigt); und dass die
> > älteste dieser Zeilen **6,71 Tage** vor dem Anker liegt (M144 c).
> > *Behauptet wird:* Die Kategorie hat **keine** wahren Treffer.
> > **Die Lücke, und sie ist der ganze Inhalt dieses Vermerks:** Der Schluss ruht **nicht** auf der
> > Messung, sondern auf der Auskunft oben. Gemessen ist nur, dass alle 538 Treffer `SUSPENDED`
> > sind — dass `SUSPENDED` nie überfällig *ist*, sagt der Auftraggeber und nicht der Bestand. Die
> > 6,71 Tage sind mit „höchstens rund eine Woche" **verträglich** und belegen sie nicht: Eine
> > Gestalt, ein Status, ein Zeitraum von sieben Tagen.

**Der Stand bis zum 03.09.2026, wortgleich:**

> **Überfällig** = **nicht** in einem Endstatus **und** `MessageLastUpdate + MessageTimeout` liegt
> vor dem Zeitpunkt der **Anwendungsuhr**.

- **`MessageTimeout` ist eine Dauer in Sekunden** (Messung M8, korrigiert am 01.08.2026 — die
  Dokumentation nannte Minuten). Die Einheit steht im Code an genau einer Stelle:
  `MessageStatusClassifier.TIMEOUT_EINHEIT`.
- **`MessageTimeout = 0` wird als „kein Timeout" behandelt.** Solche Zeilen werden nie überfällig
  (6.915 Zeilen, M2).

  > ⚠️ **Das ist eine Analogie, kein Befund** (ergänzt am 06.08.2026). Ein Gegenbeleg steht in
  > derselben Messung, die die Einheit geklärt hat: Bei **allen 52** `ERROR_TIMEOUT`-Nachrichten
  > trägt die fehlschlagende Aktion `SOSActionTimeout = 0` — und trotzdem greift dort eine Frist von
  > höchstens 120 Sekunden (M8). In dieser Spalte bedeutet `0` also eher „nimm die Vorgabe" als
  > „keine Frist". Ob `Message.MessageTimeout` dasselbe meint, ist **nicht** belegt; die Übertragung
  > ist dieselbe Analogie wie bei der Einheit.
  >
  > **Praktisch folgenlos ist das heute nur aus einem Grund:** Alle 6.915 Zeilen mit
  > `MessageTimeout = 0` stehen in einem Endstatus (`FINISHED` 5.711, `COMMIT_SENT` 1.051,
  > `COMMIT_REJECTED` 103, `CHECKED` 50) und werden damit ohnehin nie überfällig — die Lesart der
  > `0` ändert an keiner einzigen Zeile etwas. Taucht in Produktion eine **offene** Zeile mit `0`
  > auf, ist das die Stelle, an der nachzusehen ist.
- **`NULL` wird behandelt**, obwohl es in 3,34 Millionen Zeilen kein einziges Mal vorkommt — die
  Spalte lässt es zu, und die Produktion muss sich nicht daran halten, was die Testkopie zufällig
  enthält.
- **„Läuft noch" heißt „nicht in einem Endstatus"**, nicht `MessageStatus = 'RUNNING'`.
- Getrennt von *Fehler* und *Unquittiert*, niemals mit ihnen zusammengefasst (Regel Q3). `SUSPENDED`
  ist kein Fehler — aber offen, und damit der eigentliche Kandidat dieser Kategorie.

In SQL sind die offenen Zeilen genau `MessageStatus IN ('SUSPENDED','RUNNING')`; jeder andere und
jeder unbekannte Wert ist Endstatus.

### Messung gegen die Testkopie (Regel L7)

Bezugspunkt ist die Anwendungsuhr im Profil `dev`, also der Anker aus M9 (`2025-12-30 04:09:47`).

```sql
SELECT COUNT(*) FROM Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
  AND MessageTimeout > 0
  AND MessageLastUpdate + INTERVAL MessageTimeout SECOND < ?;
```

`EXPLAIN`: `type=range`, `key=MessageStatusIDX`, `rows=539`, `Using index condition; Using where` —
kein voller Durchlauf. Laufzeit **11,0 ms** für die zusammengesetzte Auswertung, 4,5 ms für die
Einzelabfrage.

| | Zeilen |
|---|---|
| offen (nicht Endstatus), Gesamtbestand | 538 |
| davon **überfällig**, Gesamtbestand | **538** |
| offen im 24-h-Standardfenster | 1 |
| davon **überfällig** im 24-h-Standardfenster | **1** |

**Die Erwartung „nahezu null überfällige Zeilen" trifft zu — aber aus einem anderen Grund als
angenommen.** Nicht weil kaum etwas überfällig wäre: Über den Gesamtbestand sind es **alle 538**
offenen Zeilen, denn `RUNNING` kommt null Mal vor und die 538 `SUSPENDED` enden am 29.12.2025, also
vor dem Anker. Im Standardfenster von 24 Stunden liegt schlicht nur **eine** offene Zeile überhaupt.

> ⚠️ **Die Kategorie „Überfällig" ist gegen die Testkopie praktisch nicht prüfbar.** Sie zeigt dort
> entweder eine Zeile (24 h) oder alle offenen (unbegrenzt) — beides sagt nichts darüber, ob die
> Frist fachlich richtig gewählt ist. Das war schon in `message-status.md` für `RUNNING` vermerkt und
> gilt für diese Kategorie insgesamt.

**Nebenbefund zur Einheit:** Dieselbe Abfrage mit `INTERVAL … MINUTE` statt `SECOND` liefert 537
statt 538 — ein Unterschied von einer Zeile. **Auf der Testkopie ist die Wahl der Einheit also
nahezu unsichtbar**, weil der Anker weit hinter allen offenen Zeilen liegt. In Produktion, wo
Nachrichten in Minuten laufen, ist es der Unterschied zwischen 30 Minuten und 30 Stunden. Ein Test,
der die Einheit gegen die Testkopie belegen wollte, könnte das nicht leisten — deshalb steht sie als
benannte Konstante im Code und ihre Begründung in `messungen-schritt4.md` M8.

---

## Die offene Prüfung

**Sie entscheidet die Regel aus dem Abschnitt „Überfälligkeit", und sie gehört gegen die
Produktion.** Die Testkopie kann sie nicht beantworten: `RUNNING` kommt dort null Mal vor.

**Ausdrücklich nicht im Anfragepfad.** Das ist eine Erhebung und kein Anwendungscode.

```sql
SELECT MessageStatus, COUNT(*) AS zeilen,
       MIN(MessageLastUpdate) AS aelteste,
       MAX(TIMESTAMPDIFF(SECOND, MessageLastUpdate, NOW())) AS aeltestes_alter_sekunden
FROM GlassfishDB.Message
WHERE MessageStatus IN ('SUSPENDED','RUNNING')
GROUP BY MessageStatus;
```

### Die Erwartung, **vor** dem Ergebnis festgehalten

| Status | erwartet |
|---|---|
| `RUNNING` | trägt **kein** Alter oberhalb von `MessageTimeout` zuzüglich eines Wächtertakts |
| `SUSPENDED` | trägt eines, aber **keines über rund einer Woche** |

**Trifft das nicht zu, ist die Regel falsch oder unvollständig, und Schritt 10b‑4 ist neu zu
bewerten.**

### Was dieselbe Abfrage gegen die Testkopie sagt — und warum das nichts entscheidet

Gefahren am 03.09.2026 als M144 (c):

| Status | Zeilen | älteste | ältestes Alter |
|---|---:|---|---:|
| `SUSPENDED` | **538** | `2025-12-23 11:04:13` | **579.934 s = 6,71 Tage** |
| `RUNNING` | — | — | *kommt nicht vor* |

**Die zweite Zeile ist der Grund, warum die Prüfung offen bleibt.** Über `SUSPENDED` sagt die
Testkopie etwas, das mit der Auskunft verträglich ist — 6,71 Tage liegen unter „rund einer Woche".
Über `RUNNING` sagt sie **nichts**, und genau dort steht die Behauptung, an der die ganze
Widerlegung hängt: dass der Wächter zuschlägt.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* der Bestand **eines** Status auf **einer** Kopie.
> *Behauptet wird:* eine Eigenschaft **beider** offenen Status in der **Produktion**.
> **Die Lücke ist grundsätzlich** und durch keine Messung an dieser Kopie zu schließen.

---

## Sicherung gegen neue Statuswerte

`DatenzugriffDbIT.statuskatalog_entspricht_dokumentierter_menge` vergleicht `SELECT DISTINCT
MessageStatus` gegen die bekannte Menge:

1. **Kein unbekannter Wert** darf auftauchen (`bekannt ⊇ vorhanden`) — die eigentliche Sicherung.
2. Die vorhandenen Werte sind **genau** die bekannte Menge ohne `RUNNING`.

Der Test wird rot, sobald im Altsystem ein neuer Status auftaucht **oder** ein dokumentierter Wert
verschwindet. Nur deshalb ist es vertretbar, unbekannte Werte neutral zu behandeln, ohne dass ein
neuer Status unbemerkt aus der Problemsicht verschwindet. Reagiert wird durch Pflege dieser Datei und
des `MessageStatusClassifier`, nie durch Raten.
