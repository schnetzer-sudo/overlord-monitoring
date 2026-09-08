# Die Property-Suche

Entsteht in **Teil 1 — Dokumentation und Backend** (08.09.2026). **Die Oberfläche ist nicht
Gegenstand dieses Teils**; sie folgt als Teil 2, so wie die BAM-Suche in Teil 2b (Backend) und
Teil 3 (Oberfläche) gebaut wurde.

> ### 📌 Teil 1 — Backend, 08.09.2026
>
> **Am Endpunkt `GET /api/bam/suche` kommt ein Parameter dazu**, `feld=<name>:<wert>`, wiederholbar;
> der bestehende Parameter `begriff` bleibt Zeichen für Zeichen unverändert, und ohne `feld`
> rendert die Suche dasselbe Statement wie vor diesem Teil — ein Test hält es gegen den eingefrorenen
> Text fest (§8). **Dazu kommt ein zweiter Endpunkt**, `GET /api/bam/suchfelder`: das Angebot der
> Suchfläche aus beiden Quellen, in zwei Gruppen (§2.1).
>
> Die Entscheidungen stehen in **§1**, die Abfrageform in **§4**, die Messung der gebauten
> Statements in **§6** — darin der Jahresfall, der als einziger eine Entscheidung des Auftraggebers
> auslösen konnte. **Was ausdrücklich nicht gebaut ist, steht in §9.**

Sie beantwortet die Frage des typischen Nutzers in einer dritten Form: **Wer einen technischen
Wert hat — eine Absender-Kennung, einen Dateinamen, eine Transaktionsnummer, eine Nachrichten- oder
Prozesskennung —, findet die Nachrichten, auf denen er steht.** Nicht eine Belegnummer
([`bam-suche.md`](bam-suche.md)), nicht einen Zeitraum mit Filtern
([`nachrichtenliste.md`](nachrichtenliste.md)).

Grundlage sind die drei Messrunden in
[`messungen-property-suche.md`](messungen-property-suche.md) — **M153 bis M160** (die Tabelle und
ihre Deckung), der Nachtrag **M161 bis M167** (die drei neuen Namen und der ganze Weg) und die
L4‑Pfad-Runde **M168 bis M171** (ob der regelkonforme Pfad trägt) — und die Messung der gebauten
Statements in §6.

> **Skills: keiner.** Es gibt keinen Repository-Skill für Endpunkte, Isolationstests oder das
> Format der Feature-Dateien (`.claude/` trägt nur `settings.local.json`); die installierten
> Skills betreffen Oberfläche, Werkzeuge oder Prüfläufe und nicht diesen Teil. **Das Format dieser
> Datei ist das von [`bam-suche.md`](bam-suche.md).**

> **Die Property-Suche lebt im Paket `bam`, und das ist eine Entscheidung.** Sie teilt mit der
> BAM-Suche den Endpunkt, das Statement und die Antwort (E‑99: *eine* Suchfläche). Ein eigenes
> Fachpaket dürfte davon nichts importieren — Fachpakete kennen einander nicht —, und alles nach
> `common` zu heben hieße, die BAM-Suche zu zerlegen, um sie an derselben Stelle wieder
> zusammenzusetzen. Der Paketname ist damit enger als sein Inhalt; `package-info.java` sagt es.

---

## 1. Die Entscheidungen

Alle aus der Sparringsrunde und den drei Messrunden; **keine steht zur Disposition.** Die Nummern
E‑99 bis E‑105 sind die aus `messungen-property-suche.md` (dort zur Umnummerierung gegenüber dem
Auftrag: Abweichung 2). **E‑106 bis E‑108 vergibt dieser Teil** — ermittelt über den Höchstwert,
nicht über eine Bereichsprobe: höchste vorher vergebene Nummer **E‑105**, der Ausdruck
`E[‑‐‒–-]\d{1,3}` mit Unicode-Bindestrichen über `docs/` und die Wurzeldateien, Gegenprobe an E‑105
(16 Treffer in zwei Dateien) und an dem bekannten Falschtreffer E‑780 (steht in der Messdatei selbst
als solcher).

| # | Entscheidung |
|---|---|
| **E‑99** | **Eine Suchfläche in der Kopfzeile**, zwei Quellen im Angebot: BAM-Typen aus `MessageBAMMandant`, Feldnamen aus `MessagePropertySearchListEntry` |
| **E‑100** | Der Feldname ist **Pflicht**. Eine typlose Suche erreicht ausschließlich BAM und **nie** eine Property |
| **E‑101** | Beide Typen im Angebot; das Backend trennt **Spaltenprädikat** (Typ 0) von **EAV-Zugriff** (Typ 1) |
| **E‑102** | Regel L4 bekommt einen **benannten Ausnahmekasten** ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Leistungsregel 4). Fenster wie BAM: Vorgabe 30 Tage, Maximum ein Jahr |
| **E‑103** | Absprung „Im Prozessbaum anzeigen" im **Detailpanel** *(Teil 2)* |
| **E‑104** | Der Absprung reicht das Fenster als **absolutes `von`/`bis`** weiter *(Teil 2)* |
| **E‑105** | Das Angebot zeigt **technische Feldnamen unverändert**. Keine Kuratierung, keine Beschriftung |
| **E‑106** *(neu)* | **Ein eigener Parameter `feld`, nicht der bestehende `begriff`.** Ein gemeinsamer Parameter müsste aus der Zeichenkette raten, ob `9018` ein BAM-Typ oder ein Feldname ist; Raten ist nach Abschnitt 4.4 der Projektbeschreibung und Regel Q4 ausgeschlossen. E‑99 ist eine Aussage über die **Fläche**, nicht über die Parameterform (§2.2) |
| **E‑107** *(neu)* | **Die Wanduhr tritt neben das Profil** — Messkonvention in [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §7 (Punkt 158). Die Zahlen aller Runden bis M167 stehen unverändert |
| **E‑108** *(neu)* | **Abschnitt 9 der Projektbeschreibung bleibt unangetastet.** Die Property-Suche wird vor der Inbetriebnahme gebaut und **nicht** unter „Enthalten" geführt — Entscheidung des Auftraggebers. Abschnitt 9 beschreibt damit nicht vollständig, was ausgeliefert wird (offener Punkt 4 in §9) |
| ~~E‑97~~ | **Gegenstandslos, und der Grund ist korrigiert:** nicht weil die Suche in der Prozessansicht fehlt, sondern weil sie dort **hinausführt** — eine Marke wird nie *in* der Prozessansicht aktiv |

**Weiter entschieden, ohne E-Nummer:**

- Sichtbarkeit des Angebots: **`MandantID IS NULL OR MandantID = :mandant`** — Auskunft des
  Auftraggebers, keine Messung. `NULL` heißt „gilt für alle".
- Ein Typ‑0‑Name, den die Abbildung nicht kennt, wird **nicht angeboten**; ein Test wird rot (§3).
- Die Abbildung Name → Spalte lebt **im Code** (`bam/Typ0Feld`).
- Der Schutz gegen ein schlechtes Feld ist **strukturell** — Pflichtfenster, Deckelung — und
  **keine Namensliste**.
- `Message.DestinationFilename` und `Message.ReceiverID` werden **kommentarlos** angeboten, trotz
  14,518 % und 12,803 % Deckung über die Wurzeln (M162, M156).
- Die Suche liefert **nur die Treffer**, nicht die Kette dahinter: `SNDPRN` und `VFN` sitzen auf
  96,263 und 98,436 % der Wurzeln und auf 0,239 und 0,454 % der Kinder (M162); der Weg zur Kette
  ist der Absprung aus Teil 2 (E‑103).

---

## 2. Die zwei Endpunkte

### 2.1 Das Angebot: `GET /api/bam/suchfelder`

Angemeldet, **Mandant aus der Sitzung** (Regel M1), **kein Parameter**. Die Ausnahmeliste in
[`mandantentrennung.md`](mandantentrennung.md) §3 bleibt bei drei Einträgen.

```json
{
  "bam":    [{ "quelle": "bam",  "typ": 9018, "bezeichnung": "Kundenmaterialnummer_K_SAP", "sortIndex": 18 }],
  "felder": [{ "quelle": "feld", "name": "Message.SNDPRN", "spalte": false },
             { "quelle": "feld", "name": "Message.Status", "spalte": true }]
}
```

| Feld | Bedeutung |
|---|---|
| `bam` | die für den Mandanten konfigurierten Belegarten — **dieselbe Abfrage wie `GET /api/bam/typen`**, wiederverwendet über `BamTypenService`, nicht nachgebaut. Immer vorhanden, leer für drei Mandanten (M40) |
| `felder` | die für den Mandanten sichtbaren Feldnamen: die globalen (`MandantID IS NULL`) und die ihm zugeordneten, **alphabetisch nach dem Namen** |
| `quelle` | `bam` oder `feld`, **je Eintrag** — obwohl die Antwort schon zwei Gruppen trägt: Die Oberfläche führt beide in einem Feld zusammen (E‑99), und ein Eintrag, der seine Herkunft selbst nennt, bleibt auch in einer gemischten Liste eindeutig |
| `name` | `MessagePropertyName`, **unverändert** (E‑105). Wer `Message.SNDPRN` nicht versteht, sieht `Message.SNDPRN` |
| `spalte` | `true`, wenn der Name eine **Spalte** benennt (Typ 0) und die Suche ein Spaltenprädikat baut; `false` für eine Zeile in `MessageProperty` (Typ 1). Die Angabe steht in der Antwort, weil sie die Bedeutung des Werts verändert: `Message.Status` vergleicht gegen den Rohstatus, `Message.GUID` gegen eine Eigenschaftszeile |

**Warum zwei Gruppen und nicht eine Liste.** Die Oberfläche zeigt sie als zwei Gruppen (E‑99).
Eine gemischte Liste müsste die Gruppe aus der Quelle zurückrechnen; zwei Listen müssen es nicht.

**Die Ordnung ist der Name, und das ist die einzige, die ohne Kuratierung auskommt.** Die Tabelle
hat weder eine Beschriftungs- noch eine Sortierspalte (M153, Punkt 147); `MessageBAMMandant` hat
beides. Eine Reihenfolge, die an der Speicherung hinge, zeigte dieselbe Auswahl zweimal verschieden.

**Ein Typ‑0‑Name, den die Abbildung nicht kennt, erscheint nicht — und ein Eintrag mit unbekanntem
Typ ebenfalls nicht.** Für den ersten gäbe es kein Spaltenprädikat, für den zweiten weder Spalte
noch Zeile (M154 kennt genau `0` und `1`). Beide fallen **still** weg, und das ist nur deshalb
vertretbar, weil `Typ0AbbildungDbIT` beide Fälle meldet (§3).

**`GET /api/bam/typen` bleibt unverändert bestehen.** Ob er später abgelöst wird, ist offener
Punkt 5 in §9 und nicht Gegenstand dieses Teils.

**Kein Zeitfenster, und Regel L1 ist nicht berührt** — dieselbe Lage wie bei `/api/bam/typen`:
Angefasst werden zwei Konfigurationstabellen mit zusammen 14 und 131 Zeilen, keine trägt einen
Zeitstempel. `Message` und `MessageProperty` bleiben unberührt; ein Angebot, das nur *belegte*
Namen zeigte, wäre eine Existenzfrage über den Gesamtbestand — und schon das Zählen **eines**
Namens kostet dort 125,527 s (M157).

### 2.2 Die Suche: `GET /api/bam/suche` mit `feld`

```
GET /api/bam/suche?feld=<name>:<wert>[&feld=…][&begriff=<typ>:<wert>…]&von=…&bis=…[&modus=…]
```

| Parameter | Form | Bedeutung |
|---|---|---|
| `feld` | **wiederholt**, `<name>:<wert>` | ein Feldbegriff. **Der Name ist Pflicht** (E‑100): `feld=:4711` ist `400`. Geteilt wird am **ersten** Doppelpunkt; alles dahinter ist Wert. Ein Komma im Wert ist Teil des Werts — der Editor aus `BamSucheController` gilt für beide Listenparameter |
| `begriff` | unverändert | der BAM-Begriff aus [`bam-suche.md`](bam-suche.md) §1, Zeichen für Zeichen |
| `von`, `bis` | unverändert | **ein** Fenster für beide Begriffsarten; Vorgabe 30 Tage, Maximum ein Jahr (E‑102) |
| `modus` | unverändert | wirkt **auf die BAM-Begriffe**. Feldbegriffe werden immer exakt verglichen (§4) |

**Beide Listen zusammen** tragen die beiden Grenzen: mindestens ein Begriff — gleich welcher Art —,
höchstens **acht**. Eine Suche allein über Felder ist zulässig. Alle Begriffe werden **verundet**:
Eine Nachricht ist Treffer, wenn jeder BAM-Begriff und jeder Feldbegriff auf ihr steht.

> **Warum ein eigener Parameter, und warum das keine Geschmacksfrage ist (E‑106).** Ein gemeinsamer
> Parameter `begriff` müsste aus der Zeichenkette raten, ob `9018` ein BAM-Typ oder ein Feldname
> ist — beides kommt vor, und die Feldnamen sind nicht auf eine Form festgelegt (`Converter.…`,
> `Message.…`, morgen etwas anderes). Raten ist ausgeschlossen (Regel Q4). **E‑99 ist eine Aussage
> über die Fläche, nicht über die Parameterform:** Die Oberfläche führt beide in einem Feld
> zusammen und schickt sie getrennt — das ist Teil 2. Der BAM-Pfad bleibt damit vollständig
> unberührt, und `BamPfadGleichheitTest` weist es gegen den eingefrorenen Text nach (§8).

> **Das Geländer von acht zählt beide Arten zusammen.** Ein Feldbegriff über eine Eigenschaft ist
> ein weiterer Join — auf `MessageProperty` statt auf `MessageBAM` —, und die Zahl der
> Join-Reihenfolgen wächst mit jedem Join, gleich welcher Tabelle. **Gemessen ist für Feldbegriffe
> genau einer je Suche** (M166) und die Kombinationen aus §6; alles darüber trägt nur das Geländer.

**Antwortform** — die aus [`bam-suche.md`](bam-suche.md) §1, mit einem Feld mehr:

```json
{
  "nachrichten": [ … ],
  "begriffe":    [ … ],
  "felder":      [ { "name": "Message.SNDPRN", "wert": "123456", "spalte": false } ],
  "von": "…", "bis": "…", "abgeschnitten": false, "modus": "EXAKT"
}
```

| Feld | Bedeutung |
|---|---|
| `felder` | die Feldbegriffe, wie die Suche sie verstanden hat — **das Zitat der Frage**, wie `begriffe` für die Belegnummern. **Immer vorhanden, leer statt fehlend**; das ist die eine sichtbare Änderung an der Antwort einer reinen BAM-Suche |
| `felder[].spalte` | ob als Spalte gesucht wurde — dieselbe Angabe wie im Angebot |
| `nachrichten[].treffer` | **nur BAM-Treffer**, wie bisher. Bei einer Suche allein über Felder ist die Liste leer; ein Feldtreffer ist der eingegebene Wert selbst und wird nicht ein zweites Mal nachgeschlagen |

**Keine Varianten für Feldwerte.** Ein BAM-Wert wird normalisiert (Sollängen,
[`bam-sollaengen.md`](bam-sollaengen.md)); für einen Feldwert gibt es keine Kuratierung, und gesucht
wird genau der eingegebene Wert, an den Rändern beschnitten.

**Fehlerfälle** — alle nach RFC 9457, alle mit eigenem `type`:

| `type` | Status | Wann | neu? |
|---|---|---|---|
| `feldname-fehlt` | 400 | `feld` mit leerem Namen (`:4711`) — E‑100 | **neu** |
| `feldbegriff-ohne-trenner` | 400 | `feld` ohne Doppelpunkt | **neu** |
| `suchbegriff-fehlt` | 400 | weder ein brauchbarer BAM- noch ein brauchbarer Feldbegriff | vorhanden, Text erweitert |
| `zu-viele-suchbegriffe` | 400 | mehr als acht Begriffe **beider Arten zusammen** | vorhanden, Text erweitert |
| alle übrigen | | unverändert aus [`bam-suche.md`](bam-suche.md) §1, §16 und §20 | vorhanden |

**Zwei neue Problemtypen, beide zur Parameterform.** Für Fenster, Abbruch und Modus entsteht
keiner. **Zwei Texte sind erweitert**, weil sie mit „Belegnummer" nicht mehr das Ganze benannten;
die Problemtypen und Statuscodes sind unverändert.

**Ein Treffer, den es nicht gibt, ist `200` mit leerer Liste** — kein `404`, aus demselben Grund wie
bei der BAM-Suche: Der Aufrufer stellt eine Frage und benennt keine Ressource (§7).

---

## 3. Die Abbildung der Typ‑0‑Namen und ihr Test

**Acht Einträge, im Code, unveränderlich** — `bam/Typ0Feld`, abgeleitet aus **M155** und nicht aus
dem Auftrag:

| Typ‑0‑Name | Spalte | Verhältnis |
|---|---|---|
| `Message.MessageID` | `Message.MessageID` | wörtlich |
| `Message.ProcessID` | `Message.ProcessID` | wörtlich |
| `Message.SOSID` | `Message.SOSID` | wörtlich |
| `Message.MessageIDSource` | `Message.SourceMessageID` | umgestellt |
| `Message.MessageIDTarget` | `Message.TargetMessageID` | umgestellt |
| `Message.Status` | `Message.MessageStatus` | umbenannt |
| `Message.ProcessName` | `Process.ProcessName` | **andere Tabelle**, über `ProcessID` |
| `Message.SOSName` | `SOS.SOSName` | **andere Tabelle**, über `SOSID` |

**Die Ableitung ergibt acht — genau die acht aus M155**, drei wörtlich, zwei umgestellt, einer
umbenannt, zwei in anderen Tabellen. Keine Abweichung.

**Die Zuordnung ist eine Vermutung aus der Namensähnlichkeit, und sie steht so im Code.** M155 hat
gemessen, dass jede der acht Spalten existiert und dass keiner der acht Namen in
`MessageProperty` vorkommt — nicht, dass der Name die Spalte *meint*. Eine fachliche Bestätigung
des Auftraggebers steht aus (offener Punkt 1 in §9); bis dahin ist dies die einzige Lesart, die ohne
Raten auskommt.

**Verglichen wird ohne Rücksicht auf Groß- und Kleinschreibung**, weil `MessagePropertyName` unter
`utf8mb4_general_ci` steht und der Primärschlüssel der Konfigurationstabelle damit auch (M153):
Zwei Schreibweisen desselben Namens sind dort *ein* Eintrag, und Java soll nicht strenger
unterscheiden als die Spalte. Gemeldet wird der Name trotzdem, wie er konfiguriert ist.

### Der rote Test

`Typ0AbbildungDbIT`, Bauform wie die Sicherung gegen neue Statuswerte
([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.1), `@Tag("db")`:

1. **Jeder konfigurierte Typ‑0‑Name ist in der Abbildung** — sonst rot, mit dem Namen.
2. **Jeder Eintrag trägt den Typ 0 oder 1** (M154) — `NULL` oder ein dritter Wert ist rot.
3. **Kein abgebildeter Name kommt als Zeile in `MessageProperty` vor** — die Voraussetzung von
   E‑101, laufend geprüft, als `EXISTS` je Name in der Form von M155 (0,455 ms für zwölf Proben).

> **Der Test ist der Grund, warum ein unbekannter Name still aus dem Angebot fallen darf.** Ohne
> ihn wäre das Verschwinden unsichtbar — niemand sähe in der Oberfläche, dass ein konfigurierter
> Name fehlt. Mit ihm ist es ein roter Lauf mit dem Namen darin und damit ein Bauauftrag mit einer
> Zeile. Das steht als Kommentar am Test und am Service, nicht nur hier.

---

## 4. Die Abfrageform

**Derselbe Kern wie die BAM-Suche** ([`bam-suche.md`](bam-suche.md) §4), um zwei Dinge erweitert:
je Feldbegriff über eine Eigenschaft ein Join auf `MessageProperty`, je Feldbegriff über eine Spalte
ein Prädikat auf `Message` — und für zwei davon ein Join auf `Process` beziehungsweise `SOS`. Die
Fassung aus **M166**, im Kern der Suche:

```sql
SELECT treffer.MessageID, …, p.ProcessName, prj.ProjectName, s.SOSName, sa.SOSActionName, …
FROM (
  SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID, m.SOSID, m.SOSActionID,
         m.Source, m.SourceMessageID, m.TargetMessageID, m.Target
  FROM MessageProperty mp1                                    -- je Eigenschafts-Begriff einer
  JOIN Message m ON m.MessageID = mp1.MessageID
  WHERE mp1.MessagePropertyName = ? AND mp1.MessagePropertyValue = ?
    AND m.MessageLastUpdate >= ? AND m.MessageLastUpdate <= ?
    AND EXISTS (SELECT 1 FROM Process mp JOIN ProjectMandant pm ON pm.ProjectID = mp.ProjectID
                WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = ?)
  GROUP BY m.MessageID
  ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
  FETCH NEXT 51 ROWS ONLY) AS treffer
LEFT JOIN Process p … LEFT JOIN Project prj … LEFT JOIN SOS s … LEFT JOIN SOSAction sa …
ORDER BY treffer.MessageLastUpdate DESC, treffer.MessageID DESC
```

**Die führende Tabelle im Text** ist `b1`, wenn ein BAM-Begriff da ist, sonst `mp1`, sonst
`Message`. Für den Optimizer ist das folgenlos (kein `STRAIGHT_JOIN`); für den BAM-Pfad heißt es,
dass sein Statement ohne Feldbegriffe **Zeichen für Zeichen** das von Teil 2b bleibt. Mit beiden
Begriffsarten stehen die Joins hintereinander:

```sql
FROM MessageBAM b1
JOIN MessageProperty mp1 ON mp1.MessageID = b1.MessageID
JOIN Message m           ON m.MessageID   = b1.MessageID
WHERE b1.MessageBAMValue IN (…) AND mp1.MessagePropertyName = ? AND mp1.MessagePropertyValue = ? AND …
```

### Der EAV-Zugriff — die benannte Ausnahme von Leistungsregel 4 (E‑102)

`MessagePropertyName = ? AND MessagePropertyValue = ?`, beide `=`, beide gebunden. **Der Einstieg
läuft über den Wertindex** — `MessagePropertyNameValueIDX` oder `MessagePropertyValueIDX`, das
wählt der Optimizer (M165: in zwei von sechs Fällen den reinen Wertindex, folgenlos) —, einen
Präfixindex über 50 Zeichen. Bei längeren Werten prüft MariaDB den Rest auf der Zeile nach; bei
eindeutigem Präfix kostet das nichts Messbares (M167).

**Das ist der Zugriff, den Leistungsregel 4 verbietet, und er ist als Ausnahme benannt** —
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Leistungsregel 4, Bauform wie der E‑c‑Kasten
an Regel 2. Die Kurzfassung: Der L4‑konforme Pfad ist in vier Fassungen erzwungen und gemessen
worden (M168 bis M171) und trägt die Suche nicht — über 24 Stunden hält er (96–97 ms), über 30 Tage
kostet er 3.186 bis 8.195 ms gegen 789 bis 1.862 ms über den Wertindex, über 90 Tage bricht jede
Form ab. **Über 90 Tage ist die Ausnahme nicht die billigere, sondern die einzige antwortende
Form.** Und über 24 Stunden hält L4 nur als Wahl des Optimizers (M170: Fassung B bricht in 14 von
18 Fällen aus), bei einer Tabellenstatistik, die 37,9 % daneben liegt (M155).

**Kein `LIKE`, kein Muster, keine Maskierung auf `MessagePropertyValue`.** Ein Präfixmodus über
den 50‑Zeichen-Präfixindex ist nicht gebaut und nicht gemessen; `modus=praefix` wirkt
ausschließlich auf `MessageBAMValue`. Die Maskierung von `%` und `_` mit `ESCAPE`, die die
BAM-Präfixsuche braucht ([`bam-suche.md`](bam-suche.md) §17), ist hier deshalb gegenstandslos —
es gibt kein Muster, das sie schützen müsste. `FeldSucheStatementsTest` hält fest, dass auf dieser
Spalte nie ein `LIKE` steht (offener Punkt 2 in §9).

### Das Spaltenprädikat — Typ 0 fasst `MessageProperty` nicht an (E‑101)

Für die acht Namen aus §3 baut das Repository ein Prädikat auf der Spalte:
`Message.MessageStatus = ?`, `Message.SourceMessageID = ?`, … — und für `Message.ProcessName` und
`Message.SOSName` je einen Join unter eigenem Alias (`feld_process`, `feld_sos`), weil `Process` in
derselben Abfrage bereits zweimal steht: für die Mandantenkette und, über der Deckelung, für den
Anzeigenamen.

**`Message.Status` vergleicht gegen den Rohwert** und nicht gegen eine Einordnung. Der Nutzer hat
einen konkreten Wert getippt, und die Suche findet genau ihn; die Übersetzung Einordnung →
Bedingung (`FEHLER` → `LIKE 'ERROR\_%' …`) gehört der Nachrichtenliste
([`nachrichtenliste.md`](nachrichtenliste.md) §5) und wird hier nicht nachgebaut.

**Derselbe Join-Name zweimal ergibt zwei Prädikate und einen Join** — eine Verundung, die leer
ausgeht, wenn zwei verschiedene Prozessnamen genannt sind. Das ist die Frage des Nutzers und keine
Fehlbedienung des Endpunkts.

### Übernommen aus `bam-suche.md` §4, nicht neu erfunden

- **Pflichtfenster** im Kern, Vorgabe 30 Tage, Maximum ein Jahr (§5).
- **Hartes Limit** `FETCH NEXT 51 ROWS ONLY` im Statement — die Deckelung greift in der Datenbank,
  bevor die Zeitgrenze greift, und **nach** dem Mandantenfilter (§5).
- **Erst deckeln, dann beschriften**: `Process`, `Project`, `SOS`, `SOSAction` hängen über der
  abgeleiteten Tabelle (M47: Faktor 1,48 in der Gegenform). Die Feld-Joins sitzen dagegen **im**
  Kern — sie entscheiden über die Menge.
- **Verdichtung auf `MessageID`** über `GROUP BY` — bei einem Feldbegriff je Nachricht nicht nötig
  (der Primärschlüssel von `MessageProperty` trägt den Namen), bei BAM-Begriffen daneben schon
  (M37); die Form bleibt in jeder Kombination dieselbe.
- **Kein `STRAIGHT_JOIN`.** In diesem Projekt als Notbehelf belegt (Faktor 219 bis 1.094, M42/M47),
  und M171 zeigt, dass er den kleinen Mandanten am härtesten trifft: `SUTTONS` über 90 Tage durch
  680.872 Indexeinträge für 64.553 Treffer, Faktor 3,50 gegen die Form, die die Mandantenkette
  zulässt.
- **Die Mandantenkette im Statement** (§7), in jeder Form — auch dort, wo `Message` allein führt.
- **Derselbe Abbruchpfad** `suche-abgebrochen` bei Fehler 1969 ([`bam-suche.md`](bam-suche.md) §6);
  es entsteht kein zweiter Problemtyp.

### Abfrage (b) nur, wenn ein BAM-Begriff getroffen haben kann

Die zweite Abfrage der BAM-Suche beschriftet, **welcher Typ mit welchem Wert** getroffen hat. Bei
einer Suche allein über Felder gibt es keinen BAM-Treffer zu beschriften — und ohne Bedingung liefe
die Abfrage über **alle** Werte der gefundenen Nachrichten. Sie wird deshalb nur gestellt, wenn
mindestens ein BAM-Begriff da ist; `FeldSucheServiceTest` hält es fest.

---

## 5. Das Fenster, das Limit, die Abschneidung

**Ein Fenster für beide Begriffsarten**, Vorgabe **30 Tage**, Maximum **ein Jahr** — wie die
BAM-Suche ([`bam-suche.md`](bam-suche.md) §2), aus demselben Grund: Wer einen Wert hat, hat kein
Datum. Der Deckel des **Präfixmodus** (30 Tage, [`bam-suche.md`](bam-suche.md) §18) gilt weiter für
die ganze Suche, sobald `modus=praefix` gesetzt ist — er hängt am Modus, und der Modus wirkt auf die
BAM-Begriffe.

**Das Jahresmaximum steht hier per Messung und nicht mehr nur per Analogie** — §6 misst alle sechs
konfigurierten Typ‑1‑Namen über ein Jahr gegen `NEXANS`; was daraus folgt, steht dort.

**Hartes Limit 50**, erkannt über die 51. Zeile, **kein Cursor** — unverändert. **Die Abschneidung
wird nach dem Mandantenfilter gezählt**, und `FeldSucheIsolationDbIT` prüft es am häufigsten Prozess
von `NEXANS` an einem Tag: 50 Treffer und `abgeschnitten: true` für den einen Mandanten, null und
`false` für den anderen (§7).

---

## 6. Messung — Regel L7

**Jede neue Abfrage ist vor dem Merge gegen die Testkopie gemessen** — `EXPLAIN` plus Laufzeit,
**Wanduhr neben dem Profil** (E‑107). Die Zahlen stehen hier und nicht in einer Messdatei; **dieser
Teil vergibt keine M-Nummern.** Die vier Skripte liegen unter `scripts/messung-property-suche/`
(`p1-angebot.sql`, `p2-jahr.sql`, `p3-typ0.sql`, `p4-kombination.sql`), die Rohausgaben unter
`ergebnis/` und damit außerhalb des Repositories (G1).

### 6.1 Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, `SELECT @@global.read_only` → **`1`** als erste Abfrage jeder Sitzung; Benutzer `monitor_read`, ausschließlich `SELECT` |
| Serverzeit | `2026-09-08 14:22:24` bis `14:34:29`, vier Sitzungen nacheinander |
| Client | `mysql.exe` Ver 8.0.46, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t`, `--force`; Passwort über `MYSQL_PWD` |
| **Die Statements** | **von jOOQ gerendert** (`MockConnection`, Bindewerte als Literale eingesetzt) und damit Zeichen für Zeichen die des Codes — mit einer Hülle `SELECT COUNT(*), MAX(CHAR_LENGTH(ProcessName)) FROM (<Statement>)`, damit kein Prozess- oder Projektname in die Rohausgabe gerät (G1, wie M159 und M166). **Die Hülle hat eine Nebenwirkung, und sie steht hier:** Der Optimizer lässt die drei nicht referenzierten `LEFT JOIN` über der Deckelung (`Project`, `SOS`, `SOSAction`) weg; sie liegen über höchstens 51 Zeilen und wiegen nichts, aber der `EXPLAIN` zeigt nur `Process` über der abgeleiteten Tabelle |
| Prüfwert | je Fall der **häufigste Wert des 30‑Tage-Fensters** bei `NEXANS` — der Bösfall, wie in M159 und M166, für alle drei Fenster derselbe; deterministisch hergeleitet, über `QUOTE()` und `PREPARE` eingesetzt, ausgegeben nur als Länge (G1). Für die Typ‑0‑Felder entsprechend: häufigster Status, häufigster Prozess, häufigster Ablauf, häufigste `SourceMessageID` und `TargetMessageID`, die jüngste `MessageID` des Fensters |
| Fenster | **absolut**, Ende `2025-12-30 00:00:00` **einschließlich** (`<=`, wie im Code): 24 Stunden ab `2025-12-29`, 30 Tage ab `2025-11-30` (Fenster B), **ein Jahr ab `2024-12-30`** |
| Mandant | **`NEXANS`** für die Suche; das Angebot zusätzlich für `SUTTONS` und `WOC`. Für `SUTTONS` gibt es bei den drei Namen des Nachtrags keine Zeile (M162); seine Zahlen für `Message.GUID` und `Converter.TransactionID` stehen in M159 und M166 |
| Laufzeit | **Wanduhr**: `SELECT SYSDATE(6) INTO @t0; EXECUTE qp; SELECT TIMESTAMPDIFF(MICROSECOND, @t0, SYSDATE(6))` — Aufwärmlauf und fünf Läufe, gewertet der **Bestwert der Läufe 2 bis 6**; das Profil (`information_schema.PROFILING`, je Lauf über `QUERY_ID = @basis + 3`) daneben. Eichung `SELECT 1`: **1,049 · 1,311 · 0,871 · 0,792 ms** in den vier Sitzungen — die Wanduhr enthält die Umlaufzeit zweier Client-Anweisungen |
| Handler-Zähler | des ersten Laufs, aus `information_schema.SESSION_STATUS` (jede Abfrage darauf kostet selbst 10 `read_rnd_next`) |
| Grenze | **10 s** je Kandidat — die des Lese-Pools ([`datenzugriff.md`](datenzugriff.md) §1); ein Abbruch ist das Ergebnis. 60 s für die Herleitung der Prüfwerte |

**Profil und Wanduhr stimmen in allen 43 Fällen überein** — der Unterschied ist die Eichung (0,8 bis
2 ms). Keiner der Pläne enthält ein `LATERAL DERIVED` oder eine `DEPENDENT SUBQUERY`; die Wanduhr
ist hier die Gegenprobe, die nichts findet, und genau dafür ist sie da.

### 6.2 Das Angebot — `GET /api/bam/suchfelder`

| Mandant | Zeilen | Wanduhr, beste von fünf | Profil |
|---|---:|---:|---:|
| `NEXANS` | 14 | **1,147 ms** | 0,412 ms |
| `SUTTONS` | 8 | 1,303 ms | 0,417 ms |
| `WOC` | 8 | 1,540 ms | 0,388 ms |

`EXPLAIN`: `MessagePropertySearchListEntry` als `ALL` über 8 geschätzte (14 gezählte) Zeilen,
`Using where` — ein Vollscan über eine Tabelle von 14 Zeilen, ohne Index auf `MandantID`, und das
ist richtig so. Die Wanduhr ist hier fast ganz Eichung; das Profil sagt 0,4 ms. Die BAM-Gruppe ist
die Abfrage aus M48 (0,534 ms) und wird nicht ein zweites Mal gemessen.

### 6.3 Die sechs Typ‑1‑Namen — und der Jahresfall, getrennt gemeldet

**Der Jahresfall ist der einzige Messfall dieses Teils, dessen Ausgang eine Entscheidung des
Auftraggebers auslösen konnte.** Vorregistriert: Reißt einer der sechs konfigurierten Namen über ein
Jahr die Zeitgrenze, wird der Fall gemeldet und nichts gebaut (der Deckel ist nicht die
selbstverständliche Antwort — zwei Markenarten in einem Feld mit zwei Höchstfenstern wäre genau die
Kollision, die bei Punkt 156 gegen das Tagesfenster entschieden hat). **Reißt keiner, bleibt das
Jahresmaximum wie bei der exakten BAM-Suche.**

**Ergebnis: Keiner reißt sie.** Wanduhr, beste von fünf nach einem Aufwärmlauf, in Millisekunden;
das Profil in Klammern:

| Name | 24 Stunden | 30 Tage | **ein Jahr** | Zeilen 24 h / 30 T / 1 J | Plan 24 h | Plan 30 Tage und ein Jahr |
|---|---:|---:|---:|---:|---|---|
| `Converter.TransactionID` | 2,158 (1,298) | 2,763 (1,415) | **2,556** (1,460) | 0 / 2 / 9 | `mp1` `ref` `MessagePropertyNameValueIDX` (17) → `Message` `eq_ref` | wie 24 h |
| `Message.GUID` | 2,222 (1,194) | 2,274 (1,082) | **2,195** (1,106) | 0 / 1 / 1 | `mp1` `ref` `MessagePropertyValueIDX` (1) → `Message` `eq_ref` | wie 24 h |
| `Message.DestinationFilename` | 112,384 (110,483) | 897,612 (896,065) | **2.033,974** (2.032,187) | 0 / 51 / 51 | `Message` `range` `MessageLastUpdateIDX` (11.814) → `mp1` `ref` `PRIMARY` (548) | `mp1` `ref` `MessagePropertyNameValueIDX` (98.396) → `Message` `eq_ref` |
| `Message.ReceiverID` | 113,104 (111,136) | 1.348,429 (1.346,699) | **3.973,863** (3.972,467) | 51 / 51 / 51 | wie oben | `mp1` `ref` `MessagePropertyNameValueIDX` (192.284) → `Message` `eq_ref` |
| `Message.SNDPRN` | 95,910 (93,990) | 1.596,820 (1.594,837) | **4.139,130** (4.137,522) | 37 / 51 / 51 | wie oben | `mp1` `ref` `MessagePropertyValueIDX` (198.614) → `Message` `eq_ref` |
| `Message.VFN` | 96,006 (94,708) | 1.968,453 (1.966,316) | **5.369,088** (5.367,135) | 51 / 51 / 51 | wie oben | `mp1` `ref` `MessagePropertyNameValueIDX` (242.280) → `Message` `eq_ref` |

Die Prüfwerte sind 6, 36, 7, 10, 6 und 6 Zeichen lang; die Läufe mit null Zeilen messen wie in M166
den Leerlauf (der häufigste Wert des Monats liegt nicht im letzten Tag). Über der abgeleiteten
Tabelle in allen Fällen `<derived3>` als `ALL` über höchstens 51 Zeilen und `Process` als `eq_ref`.

**Der schlimmste Jahresfall liegt bei 5.369 ms — 53,7 % der Zehn-Sekunden-Grenze** — und damit
**unter** dem Jahresfall der exakten BAM-Suche (8.939,7 ms, M47; Faktor 0,60). Das Jahresmaximum
bleibt. **Über ein Jahr führt in jedem der sechs Fälle der Wertindex**, und `Message` wird über den
Primärschlüssel erreicht — der Zeitbereich in `Message` wird nie als Bereich gelesen. Der Vollscan
aus Punkt 159 (Kennzeichnung an Leistungsregel 1) tritt hier deshalb nicht ein: Er trifft die
Formen, die über den Zeitbereich *einsteigen*, und das tut diese nicht.

**Was das Jahr kostet, ist die Fenstermenge hinter dem Index, nicht der Index.** Die Handler-Zähler
des ersten Laufs, 30 Tage gegen ein Jahr:

| Name | `read_next` 30 T / 1 J | `tmp_write` 30 T / 1 J | Laufzeit 1 J / 30 T |
|---|---:|---:|---:|
| `Message.DestinationFilename` | 49.976 / **49.976** | 5.252 / **62.641** | 2,27× |
| `Message.ReceiverID` | 81.307 / 81.307 | 7.562 / 90.656 | 2,95× |
| `Message.SNDPRN` | 102.284 / 102.284 | 13.167 / 86.768 | 2,59× |
| `Message.VFN` | 124.715 / 124.715 | 15.229 / 105.310 | 2,73× |

Der Indexbereich ist in beiden Fenstern **derselbe** — der Index kennt kein Fenster, jede Zeile des
Werts wird gelesen und über den Primärschlüssel gegen das Fenster geprüft. Was mit dem Fenster
wächst, ist die Zahl der Zeilen, die in die temporäre Tabelle gehen und sortiert werden: das 6,6‑ bis
12‑Fache der Zeilen für das 2,3‑ bis 3‑Fache der Zeit.

**Gegen M166 und M159 (dieselben Werte, dieselben Fenster):** über 30 Tage
`Message.DestinationFilename` 789,350 → 897,612 ms (+13,7 %), `Message.SNDPRN` 1.531,134 →
1.596,820 (+4,3 %), `Message.VFN` 1.862,098 → 1.968,453 (+5,7 %), `Message.ReceiverID` (M159)
1.222,763 → 1.348,429 (+10,3 %). Das gebaute Statement liefert zwölf Spalten statt einer Zählung und
trägt die Beschriftung über der Deckelung; der Aufschlag ist der Preis dafür. Über 24 Stunden wählt
der Optimizer wie in M166 den Zeitindex und erreicht `MessageProperty` über den Primärschlüssel —
96 bis 113 ms, unabhängig vom Wert (6.249 ICP-Versuche in jedem Fall: die 6.249 Nachrichten des
Tages).

**Gegen den Maßstab der ausgelieferten BAM-Suche** (1.655,8 ms über 30 Tage, M47): `Message.VFN`
1,19×, `Message.SNDPRN` 0,96×, `Message.ReceiverID` 0,81×, `Message.DestinationFilename` 0,54×.
`VFN` reißt ihn wie in M166 (dort 1,12×) — Punkt 152 bleibt, wie er ist.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* der ganze Weg für sechs Namen über drei Fenster bei `NEXANS`, je mit dem häufigsten
> Wert des 30‑Tage-Fensters, auf einer ruhenden Testkopie.
> *Behauptet wird:* dass das Jahresmaximum für die Property-Suche trägt.
> **Die Lücke:** Der häufigste Wert des **Jahres** könnte ein anderer sein als der des Monats — er
> ist nicht erhoben, weil ihn zu bestimmen je Name die Zählung über den Bestand kostete (M157:
> 125,527 s). Und alle Zahlen stammen von einer ruhenden Kopie; die BAM-Suche führt denselben
> Vorbehalt für ihren Jahresfall ([`bam-suche.md`](bam-suche.md) §6) — dort bei 89 % der Grenze,
> hier bei 54 %.

### 6.4 Die acht Typ‑0‑Felder — vier davon brechen über ein Jahr ab

Wanduhr, beste von fünf, in Millisekunden; Profil in Klammern; `NEXANS`:

| Feld | 30 Tage | **ein Jahr** | Zeilen | Plan | gelesen (`read_next` 30 T / 1 J) |
|---|---:|---:|---:|---|---:|
| `Message.MessageID` | 1,602 (0,896) | **1,430** (0,683) | 1 / 1 | `Message` `const` `PRIMARY` | 0 / 0 |
| `Message.MessageIDTarget` | 8,917 (7,906) | **8,879** (8,039) | 51 / 51 | `Message` `ref` `TargetMessageIDIDX` (749) | 749 / 749 |
| `Message.MessageIDSource` | 28,911 (27,892) | **28,129** (27,021) | 51 / 51 | `Message` `ref` `SourceMessageIDIDX` (6.504) | 3.048 / 3.048 |
| `Message.ProcessID` | 2,992 (2,140) | **2,949** (2,129) | 51 / 51 | `Message` `range` `MessageLastUpdateIDX` (409.758 / 1.780.243), **Mandantenkette `const`**, kein `filesort` | 0 / 0 (2.396 `tmp_write`) |
| `Message.Status` | 4.717,477 (4.716,338) | **⛔ Abbruch**, 6 von 6 (10.010,325) | 51 / — | `Message` `range` `MessageLastUpdateIDX`, `Using temporary; Using filesort`; `MessageStatusIDX` nur unter `possible_keys` | 214.330 / 440.868 in 10 s |
| `Message.SOSID` | 4.272,428 (4.270,451) | **⛔ Abbruch**, 6 von 6 (10.010,747) | 51 / — | wie `Status` | 214.330 / 483.180 in 10 s |
| `Message.ProcessName` | 4.592,376 (4.590,267) | **⛔ Abbruch**, 6 von 6 (10.009,531) | 51 / — | `Message` `range` Zeitindex → `feld_process` `eq_ref` `PRIMARY`; `Using temporary; Using filesort` | 214.330 / 449.368 in 10 s |
| `Message.SOSName` | 4.780,131 (4.778,394) | **⛔ Abbruch**, 6 von 6 (10.009,429) | 51 / — | `Message` `range` Zeitindex → `feld_sos` `eq_ref` `PRIMARY`; `Using temporary; Using filesort` | 214.330 / 429.758 in 10 s |

**Zwei Klassen, und die Grenze verläuft nicht dort, wo der Name sie vermuten lässt.** Die vier
Felder, für die `Message` einen Index trägt (`MessageID`, `SourceMessageID`, `TargetMessageID`,
`ProcessID`), kosten 1 bis 29 ms in beiden Fenstern. Die vier anderen lesen **das ganze Fenster**:
214.330 Zeilen über 30 Tage (4,3 bis 4,8 s — das 2,6‑ bis 2,9‑Fache des BAM-Maßstabs), und über ein
Jahr kommen sie in zehn Sekunden 430.000 bis 480.000 Zeilen weit und brechen ab — **in allen sechs
Läufen je Feld, 24 Abbrüche**.

**Der Mechanismus steht im Plan, und er ist derselbe Zeitbereich in zwei Gestalten.** Bei
`ProcessID = ?` wird die Mandantenkette `EXISTS (… WHERE ProcessID = ?)` zur **Konstante** —
`mandanten_process` und `ProjectMandant` stehen als `const` im Plan —, und der Zeitindex wird
absteigend gelesen, bis 51 Zeilen stehen: 2.396 Zeilen in die temporäre Tabelle, 3 ms, gleich wie
weit das Fenster reicht. Bei `Status`, `SOSID` und den beiden Namen läuft die Kette **je Zeile**
(`eq_ref`), das `GROUP BY` braucht eine temporäre Tabelle, und die füllt sich mit dem ganzen Fenster,
bevor sortiert wird. **`Message.ProcessName` und `Message.ProcessID` benennen dieselbe Menge und
kosten 4.592 ms gegen 3 ms** — der Unterschied ist allein, dass der Name über den Join kommt und die
Kennung als Konstante.

> **Das ist der Befund dieses Teils, der in keiner Zeile des Auftrags stand, und er gehört dem
> Auftraggeber** (offener Punkt 10 in §9). Vier der acht Typ‑0‑Felder sind über ein Jahr für den
> größten Mandanten **nicht antwortfähig**, und drei davon (`Status`, `ProcessName`, `SOSName`)
> sind gerade die, die ein Nutzer ohne Kennung greifen würde. **Gebaut ist nichts dagegen**, aus
> demselben Grund wie beim Jahresfall der Typ‑1‑Namen: Ein Deckel für einen Teil des Angebots wäre
> die Kollision zweier Höchstfenster in einem Feld (E‑99), und ein Feld aus dem Angebot zu nehmen
> ist eine Entscheidung und keine Messung. **Was heute greift, ist der Abbruchpfad:** `400
> suche-abgebrochen` nach zehn Sekunden, mit dem Rat, den Zeitraum zu verkleinern — dieselbe
> Bauform wie beim Jahresfall der BAM-Suche unter Last. Über 30 Tage antworten alle acht.
>
> **Und die Zahl, die die Entscheidung erleichtert, ist bereits gemessen:** `Message.ProcessID`
> kostet 3 ms. Löste die Suche `Message.ProcessName` **vorab** über die Stammdaten in Kennungen auf
> — 1.503 Zeilen in `Process`, die Form, die die Nachrichtenliste für ihren Suchbegriff seit Schritt 4
> verwendet ([`nachrichtenliste.md`](nachrichtenliste.md) §5, „niemals gegen `Message`") — und
> suchte dann `ProcessID IN (…)`, wäre das die Form aus der vierten Zeile. Für `SOSName` hülfe das
> nicht: `SOSID` selbst liest das ganze Fenster, weil `Message` keinen Index darauf trägt. **Das ist
> gemessen, nicht gebaut** — gebaut ist der Join, wie beauftragt.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* acht Felder über zwei Fenster bei `NEXANS`, je mit dem häufigsten Wert des
> 30‑Tage-Fensters — für `Status` ist das `FINISHED`, für Prozess und Ablauf die Nummer eins des
> Monats.
> *Behauptet wird:* dass vier der acht Felder über ein Jahr abbrechen.
> **Die Lücke:** Gemessen ist der häufigste Wert. Ein seltener Status (`ERROR_TIMEOUT`, 52 Zeilen
> seit 2025) oder ein kleiner Prozess kostete über den Zeitindex dasselbe — der Preis ist die
> Fenstermenge, nicht der Wert (derselbe Befund wie in M169) —, **es sei denn, der Optimizer wählte
> `MessageStatusIDX`**, was er hier nicht getan hat und was nicht erhoben ist. Für den kleinen
> Mandanten ist nichts gemessen; M169 und M171 zeigen, dass er über die Mandantenkette
> (`ProejctIDIDX`) läuft und der Vollscan ihn nicht trifft.

### 6.5 Die Kombinationen

Wanduhr, beste von fünf, in Millisekunden; Profil in Klammern; `NEXANS`, je mit den häufigsten
Werten des 30‑Tage-Fensters (der häufigste BAM-Wert ist 8 Zeichen lang):

| Kombination | 30 Tage | ein Jahr | Zeilen | Plan 30 Tage | Plan ein Jahr |
|---|---:|---:|---:|---|---|
| BAM-Begriff **und** `Message.SNDPRN` | 1.725,009 (1.722,162) | **5.273,876** (5.272,753) | 51 / 51 | `mp1` `ref` `MessagePropertyValueIDX` (198.614) → `Message` `eq_ref` → `b1` `ref` `PRIMARY` | **`b1` `ref` `MessageBAM_BAMValueOnly` (443.830)** → `mp1` `ref` `PRIMARY` (548) → `Message` `eq_ref` |
| `Message.SNDPRN` **und** `Message.VFN` | 2.331,062 (2.329,520) | **4.775,596** (4.774,080) | 51 / 51 | `mp1` `ref` `MessagePropertyValueIDX` → `mp2` `ref` `PRIMARY` (548) → `Message` `eq_ref` | wie 30 Tage |
| `Message.SNDPRN` **und** `Message.Status` | 1.353,824 (1.351,776) | 1.352,559 (1.351,167) | **0 / 0** | `mp1` `ref` `MessagePropertyValueIDX` → `Message` `eq_ref` | wie 30 Tage |

**Die Verundung kostet ungefähr die Summe der Teile, und der Optimizer wählt den Einstieg
selbst.** Zwei Eigenschaften über 30 Tage: 2.331 ms gegen 1.597 und 1.968 einzeln — der zweite
Begriff wird je Kandidat über den Primärschlüssel geprüft (`mp2` `ref` `PRIMARY`, 100.205
zusätzliche `read_key` gegenüber `SNDPRN` allein). Beim BAM-Begriff **kippt der Einstieg mit dem Fenster**: über 30 Tage
führt `MessageProperty` über den Wertindex, über ein Jahr `MessageBAM` über
`MessageBAM_BAMValueOnly` — 443.830 geschätzte Zeilen, 466.014 gelesene — und `MessageProperty`
wird über den Primärschlüssel geprüft. Das ist die Wahl, die kein `STRAIGHT_JOIN` zuließe, und sie
kostet 5.274 ms statt eines Abbruchs.

**Die dritte Kombination hat null Treffer, und das ist kein Messfehler:** Der häufigste Status des
Monats ist `FINISHED`, der häufigste `SNDPRN` sitzt auf den Wurzeln — und Wurzeln sind `SPLITTED`
(M162: 96,263 % der Wurzeln tragen den Namen). Die Suche liest trotzdem den ganzen Wertbereich
(102.284 `read_next`), prüft jede Zeile gegen den Status und behält keine: 1,35 s für eine leere
Antwort, in beiden Fenstern gleich. Das ist der Preis eines Feldbegriffs, den kein Treffer erfüllt,
und er hängt am Wert, nicht am Fenster.

### 6.6 Was diese Messung nicht zeigt

- **Nur `NEXANS`** für die Suche. Der kleine Mandant ist für die Typ‑1‑Namen in M159 und M166
  gemessen (`Message.GUID` 0,901 ms, `Converter.TransactionID` 1,047 ms); für die Typ‑0‑Felder und
  die Kombinationen gar nicht.
- **Der Bösfall des Jahres ist der des Monats.** Ob ein anderer Wert über das Jahr häufiger ist, ist
  nicht erhoben (6.3).
- **Ein Feldbegriff je Suche, höchstens zwei in der Kombination.** Das Geländer von acht ist nicht
  gemessen (offener Punkt 8 in §9).
- **Ruhende Kopie.** Kein Lauf unter dem Verkehr des Altsystems; der Jahresfall der Typ‑1‑Namen liegt
  bei 54 % der Grenze, die vier Typ‑0‑Felder über 30 Tage bei 43 bis 48 %.
- **Die Statistik der Produktion ist eine andere** (M155: 37,9 % daneben auf der Kopie). Jede
  Planwahl hier — der Wertindex über ein Jahr, der Kipppunkt der BAM-Kombination, die konstante
  Kette bei `ProcessID` — ist eine Wahl des Optimizers auf *dieser* Statistik.

---

## 7. Mandantentrennung

**Der Filter ist Bestandteil jedes Statements auf dem Quellschema** (Regel M3):

- In der Suche als `EXISTS` über `Process → ProjectMandant`, in **jeder** Form des Kerns — auch
  dort, wo `Message` allein führt (Typ‑0‑Feld ohne weiteren Begriff) und wo ein Feldbegriff über
  den Primärschlüssel auf `Message` zugreift (`Message.MessageID`).
- Im Angebot als das `WHERE` selbst, mit **zwei Zweigen**: `MandantID IS NULL OR MandantID = ?`.
  Keine `EXISTS`-Kette, weil die Tabelle den Mandanten selbst trägt — dieselbe Lage wie bei
  `MessageBAMMandant`. **Ein `WHERE MandantID = ?` ohne den `NULL`-Zweig löschte acht von vierzehn
  Einträgen aus dem Angebot; ein Zugriff ohne Filter zeigte einem Mandanten die Konfiguration
  eines anderen.** `SuchfelderStatementsTest` hält die Form am Text fest — weil beides in einem
  grünen Isolationstest gleich aussähe, solange nur ein Mandant gebundene Einträge hat.

**`MandantContext` ist erster Pflichtparameter jeder Repository-Methode** (Regel M2), auch der auf
die Konfigurationstabelle; `PaketstrukturTest` prüft es unverändert.

### Die Isolationstests (Regel M4)

Zwei neue Endpunkte, zwei Tests, Vorlage [`mandantentrennung.md`](mandantentrennung.md) §5,
Paarung **`NEXANS` gegen `SUTTONS`** aus dem Grund von `BamTypenIsolationDbIT`: gegen `VOTG` bliebe
ein Leck von `SUTTONS` in einer Richtung unsichtbar (M48).

**`SuchfelderIsolationDbIT`** — das Angebot:

1. Es gibt globale Einträge, und **mindestens ein** Mandant hat gebundene; die BAM-Mengen sind
   disjunkt.
2. Jeder Mandant sieht **genau** seine Auswahl, beide Gruppen, gegen das Quellschema und die
   Abbildung gehalten.
3. **Die globalen Einträge stehen bei beiden.**
4. **Fremde gebundene Einträge erscheinen nicht — in beide Richtungen.**
5. Der Mandantenwechsel wechselt das Angebot vollständig (die Gegenprobe ohne Parameter).
6. Der Rumpf trägt keine Mandantenkennung.
7. Ohne aktiven Mandanten `403`, auch für ADMIN.

> **Was die Daten hergeben, und was nicht — gemeldet, nicht verschwiegen.** Nach dem Stand der
> Testkopie vom 08.09.2026 sind **alle sechs gebundenen Einträge `NEXANS` zugeordnet**; `SUTTONS`
> hat keinen (M161). Die Paarung, in der ein Leck in *beide* Richtungen sichtbar würde, gibt es für
> die Feld-Gruppe damit **heute nicht** — nicht, weil der Test es nicht versuchte, sondern weil es
> nichts gibt, das von `SUTTONS` nach `NEXANS` lecken könnte. Der Test leitet die Mengen zur
> Laufzeit her und prüft beide Richtungen; die zweite wird scharf, sobald ein zweiter Mandant
> gebundene Einträge bekommt. Verlangt wird nur, dass mindestens einer welche hat — sonst bewiese
> der Test, dass leer leer ist. Für die BAM-Gruppe gilt die Disjunktheit wie in
> `BamTypenIsolationDbIT`.

**`FeldSucheIsolationDbIT`** — die Suche, Fenster B (30.11. bis 30.12.2025), mit beiden Pfaden:

1. Beide Mandanten finden ihre eigene Nachricht über eine Eigenschaft.
2. **Eine fremde Eigenschaft ist ununterscheidbar von einer erfundenen** derselben Länge — `200`,
   leere Liste, gleicher Rumpf bis auf das Zitat.
3. Die Trennung gilt in beide Richtungen.
4. **Eine fremde `MessageID` als Spaltenfeld ist ununterscheidbar von einer erfundenen** — der
   schärfste Fall: ein Zugriff über den Primärschlüssel, der die Zeile in einem Schritt fände, stünde
   die Kette nicht im selben Statement. Und dieselbe Kennung ist beim eigenen Mandanten erreichbar.
5. Ein fremder Prozess als Spaltenfeld findet nichts — über die Kennung und über den Namen (der
   Join-Pfad).
6. **Die Abschneidung wird nach dem Mandantenfilter gezählt** (§5).
7. Ohne aktiven Mandanten `403`.
8. Die Gegenprobe über den Mandantenwechsel mit einer echten fremden Eigenschaft.

**Kein Prüfwert steht in einer Testdatei** (Regeln G1 und T2). Der Anker ist die erste Nachricht des
Mandanten am dichtesten Tag; davon die erste Eigenschaft, deren Wert **selten** ist — höchstens 50
Nachrichten tragen ihn —, damit „findet die eigene Nachricht" nicht an der Deckelung scheitert.
Welcher Name das ist, entscheidet der Bestand.

> **Ein Befund aus dem ersten Lauf, und er gehört hierher.** Beide Isolationstests waren im ersten
> Lauf rot — am Test, nicht am Code. **Der Rumpf zitiert die Frage:** Wer nach `Message.GUID:<wert>`
> sucht, bekommt den Wert im Rumpf zurück, und bei `Message.GUID` **ist** der Wert die Kennung der
> Nachricht (eine Zeile je Nachricht, M157). Ein Test, der die fremde Kennung im Rumpf verbietet,
> wird an seinem eigenen Zitat rot. Die Kennung wird deshalb im Rumpf **ohne das Zitat** gesucht —
> dieselbe Behandlung wie `instance` und `traceId`. Und: `doesNotContainAnyElementsOf` wirft bei
> einer **leeren** Verbotsmenge einen Fehler, keinen Fehlschlag — die gebundenen Einträge von
> `SUTTONS` sind genau das. `noneMatch` sagt dasselbe und ist bei leerer Menge wahr.

---

## 8. Aufbau im Code und Tests

```
bam/
├─ Typ0Feld.java                die Abbildung — acht Einträge, aus M155 (§3)
├─ Feldbegriff.java             <name>:<wert>, geteilt am ERSTEN Doppelpunkt, Name Pflicht
├─ Feldbedingung.java           ein Feldbegriff, wie das Statement ihn sieht: Spalte oder Zeile
├─ FeldBegriffResponse.java     das Zitat der Feldbegriffe in der Antwort
├─ BamSuchfilter.java           + felder; beide Grenzen über beide Listen
├─ BamSucheRepository.java      derselbe Kern, um Feld-Joins und Spaltenprädikate erweitert
├─ BamSucheService.java         Spalte oder Zeile über die Abbildung; Abfrage (b) nur mit BAM-Begriff
├─ BamSucheController.java      + Parameter feld
├─ BamSucheResponse.java        + felder
├─ SuchfelderController.java    GET /api/bam/suchfelder
├─ SuchfelderService.java       beide Gruppen; unbekannter Typ-0-Name fällt weg
├─ SuchfelderRepository.java    MandantID IS NULL OR MandantID = ?
├─ SuchfeldZeile.java           eine Konfigurationszeile, ohne MandantID
└─ SuchfelderResponse.java / SuchfeldBamResponse.java / SuchfeldFeldResponse.java
```

**Am BAM-Pfad ist nichts umgebaut.** `findeTreffer(mandant, bedingungen, fenster)` bleibt und
delegiert mit leerer Feldliste; ohne Feldbegriffe ruft der Service genau diese Methode.
`BamTypenService` wird vom Angebot wiederverwendet.

### Tests

| Datei | Was, und ob mit Datenbank |
|---|---|
| `Typ0FeldTest` | **ohne DB** — genau die acht Namen aus M155, drei wörtlich, Vergleich ohne Groß- und Kleinschreibung, unbekannt ist leer |
| `FeldSuchfilterTest` | **ohne DB** — Teilung am ersten Doppelpunkt, `feldname-fehlt`, `feldbegriff-ohne-trenner`, leerer Wert fällt weg, allein über Felder, beide Grenzen über beide Listen, Modus neben Feldern, alte Signatur |
| `FeldSucheStatementsTest` | **ohne DB** — EAV als Join mit Name und Wert je `=`, ein Join je Feldbegriff, **nie `LIKE` auf `MessagePropertyValue`**, Typ 0 ohne `MessageProperty`, jedes der acht Felder auf seiner Spalte, die zwei Joins unter eigenem Alias, Mandantenfilter, Fenster, Limit und **kein `STRAIGHT_JOIN`** in jeder Form, erst deckeln dann beschriften, ohne Bedingung kein Statement |
| `FeldSucheServiceTest` | **ohne DB** — Spalte oder Zeile über die Abbildung, das Zitat, keine Abfrage (b) ohne BAM-Begriff, beide Arten zusammen |
| `SuchfelderStatementsTest` | **ohne DB** — beide Zweige des Filters mit gebundenem Mandanten, nur die Konfigurationstabelle, Ordnung nach Name, `MandantID` keine Auswahlspalte |
| `SuchfelderServiceTest` | **ohne DB** — BAM-Gruppe unverändert mit Quelle, Typ 1 angeboten, Typ 0 nur mit Abbildung, unbekannter Typ nicht, Name wie konfiguriert |
| **`BamPfadGleichheitTest`** | **ohne DB** — **der Gleichheitstest des BAM-Pfads**: drei Statements Zeichen für Zeichen gegen den Abzug unter `src/test/resources/bam/`, gezogen am 08.09.2026 *vor* der ersten Codeänderung; die neue Signatur mit leerer Liste rendert denselben Text; ohne `feld` ruft der Service den alten Pfad |
| `Typ0AbbildungDbIT` | `@Tag("db")` — **der rote Test** (§3) |
| `FeldSucheDbIT` | `@Tag("db")` — am laufenden Endpunkt: Eigenschaft wird gefunden und zitiert, `Message.MessageID` findet genau eine, die übrigen Spaltenfelder greifen (verundet mit der Kennung), Belegnummer und Feld werden verundet, die Parametergrenzen, das Fenster |
| `SuchfelderIsolationDbIT` | `@Tag("db")` — **Pflicht-Isolationstest des Angebots** (§7) |
| `FeldSucheIsolationDbIT` | `@Tag("db")` — **Pflicht-Isolationstest der Suche** (§7) |

**Keine Wanduhrzeit als Prüfkriterium** (Regel T1) — kein Test dieses Teils misst eine Dauer.

---

## 9. Regelbezug und offene Punkte

### Regelbezug

| Regel | Wie umgesetzt |
|---|---|
| **M1** | Kein Parameter für den Mandanten; die Ausnahmeliste bleibt bei drei Einträgen |
| **M2** | `MandantContext` erster Pflichtparameter jeder Repository-Methode, auch auf der Konfigurationstabelle |
| **M3** | `EXISTS` über `ProjectMandant` in jeder Form des Kerns; `MandantID IS NULL OR = ?` im Angebot |
| **M4** | `SuchfelderIsolationDbIT`, `FeldSucheIsolationDbIT` |
| **M5** | Die Trennung gilt auch für die Abschneidung |
| **S1** | Ausschließlich `SELECT`; keine Migration, keine Tabelle in `overlord_monitor` |
| **L1** | Pflicht-Zeitfenster, Vorgabe 30 Tage, Maximum ein Jahr — gemessen in §6, gekennzeichnet in [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8, Regel 1 |
| **L2** | Kein `total`, keine Aggregation über `Message` |
| **L3** | Kein `OFFSET`, kein Cursor |
| **L4** | **Die erste benannte Ausnahme** — nur der Sucheinstieg über Name und Wert (E‑102) |
| **L5** | Hartes Limit ja, Mindestlänge nein — wie [`bam-suche.md`](bam-suche.md) §1 |
| **L7** | §6, mit `EXPLAIN`, Wanduhr und Profil |
| **L8** | `MessagePropertySearchListEntry` ist erhoben (M153), bevor sie in Anwendungscode steht |
| **L10** | Belegvermerke in §6 |
| **Q4** | Weder die Parameterform noch die Zuordnung Name → Spalte ist geraten; wo eine Vermutung steht, steht sie als solche (§3) |
| **T1** | Keine Wanduhrzeit in einer Zusicherung |
| **T2** | Alle Prüfwerte zur Laufzeit hergeleitet, keine Zahl aus dem Pflegestand |
| **Z1** | Die Fenstervorgabe wird gegen die Anwendungsuhr aufgelöst |

### Offene Punkte

1. **Die Zuordnung Name → Spalte ist eine Vermutung aus der Namensähnlichkeit** (M155,
   Punkt 143). Gemessen ist, dass die Spalten existieren und die Namen nicht als Zeilen vorkommen —
   nicht, dass `Message.Status` den `MessageStatus` *meint*. Eine fachliche Bestätigung des
   Auftraggebers steht aus.
2. **Kein Präfixmodus für Feldwerte.** `modus=praefix` wirkt nur auf BAM-Begriffe. Ein `LIKE` über
   den 50‑Zeichen-Präfixindex von `MessagePropertyValue` ist nicht gebaut und nicht gemessen; ob er
   gebraucht wird, sagt erst die Oberfläche (Teil 2).
3. **Der Rückfall auf die Präfixsuche mit Feldbegriffen.** Geht eine Suche aus BAM- und Feldbegriffen
   leer aus, bietet die Oberfläche heute den Präfixmodus an ([`bam-suche.md`](bam-suche.md) §23) —
   der wirkt dann auf die BAM-Begriffe und lässt die Feldbegriffe exakt. Ob das die richtige Antwort
   ist, entscheidet Teil 2.
4. **Abschnitt 9 der Projektbeschreibung beschreibt nicht vollständig, was ausgeliefert wird**
   (E‑108). Die Property-Suche wird vor der Inbetriebnahme gebaut und nicht unter „Enthalten"
   geführt — Entscheidung des Auftraggebers, hier eingetragen, damit es niemand später als
   Versäumnis „repariert".
5. **`GET /api/bam/typen` bleibt neben `GET /api/bam/suchfelder` bestehen.** Ob er abgelöst wird,
   wenn die Oberfläche auf das Angebot umgestellt ist, ist nicht Gegenstand dieses Teils.
6. **Die Konfigurationstabelle der Testkopie ist von Hand geändert** (Punkt 150). Wird die Kopie neu
   befüllt, verschwinden die drei Namen des Nachtrags aus dem Angebot, und `Service.Type` kehrt
   zurück — `Typ0AbbildungDbIT` merkt es nicht, weil `Service.Type` Typ 1 ist. Was dann gilt, steht
   in §6 bei den Zahlen für die sechs Namen.
7. **Die Feld-Gruppe des Isolationstests prüft heute nur eine Richtung scharf** (§7). Das ist eine
   Grenze der Daten; der Test ist für beide gebaut.
8. **Das Geländer von acht Begriffen ist für gemischte Suchen nicht gemessen.** Gemessen sind ein
   Feldbegriff (M166) und die Kombinationen aus §6; acht Joins über zwei Tabellen sind es nicht.
9. **`SNDPRN` und `VFN` finden nur Wurzeln** (Punkt 155). Die Suche liefert die Treffer und nicht die
   Kette; der Weg dorthin ist der Absprung aus Teil 2 (E‑103).
10. **Vier Typ‑0‑Felder brechen für `NEXANS` über ein Jahr ab** — `Message.Status`,
    `Message.SOSID`, `Message.ProcessName`, `Message.SOSName` (§6.4): 4,3 bis 4,8 s über 30 Tage,
    24 von 24 Abbrüchen über ein Jahr, weil die Mandantenkette je Zeile läuft und das ganze Fenster
    in die temporäre Tabelle geht. **Entscheidung des Auftraggebers, nicht dieses Teils:** so lassen
    (der Abbruchpfad greift, über 30 Tage antworten alle acht), ein Deckel — mit der Kollision zweier
    Höchstfenster in einem Feld (E‑99) —, oder die Felder aus dem Angebot. Gebaut ist nichts.
11. **`Message.ProcessName` vorab in Kennungen auflösen** — die Form der Nachrichtenliste
    ([`nachrichtenliste.md`](nachrichtenliste.md) §5) — machte aus 4.592 ms die 3 ms von
    `Message.ProcessID` (§6.4), weil die Mandantenkette dann zur Konstante wird. Gemessen an der
    Kennungsform, nicht gebaut; für `SOSName` hülfe es nicht. Hängt an Punkt 10.
12. **Ein Feldbegriff, den kein Treffer erfüllt, kostet den vollen Wertbereich** (§6.5: 1,35 s für
    eine leere Antwort bei `SNDPRN` und `Status`). Das ist der Preis der Verundung über den
    Wertindex und keine Fehlbedienung; ob die Oberfläche davor warnt, ist Teil 2.

> **Die offenen Punkte der Oberfläche entstehen in Teil 2** und gehören dann neben die Ansicht, die
> sie betreffen.
