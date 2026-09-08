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

> ### 📌 Teil 2 — Korrektur, Oberfläche, Absprung, 08.09.2026
>
> **Drei Teile.** Erstens eine **Backend-Korrektur**: `Message.ProcessName` wird nicht mehr über
> einen Join gesucht, sondern **vorab über die Stammdaten** zu Kennungen aufgelöst — die Form der
> Nachrichtenliste, 3 ms statt 4.592 ms, Gleichheitsprobe bestanden; die Rücknahme der Bauvorgabe aus
> Teil 1 steht als **datierter Korrekturblock in §10**. Zweitens die **Oberfläche** (§11): zwei
> Gruppen im Suchfeld, Feld-Marken neben den BAM-Marken, ein Geländer für beide, `feld` in der URL,
> die Trefferspalte, die bei reiner Feldsuche entfällt, und die fünfte Bedingung am Präfix-Angebot.
> Drittens der **Absprung in den Prozessbaum** aus dem Detailpanel (§12, E‑103/E‑104). Tests in
> §13, die Sichtprüfung in §14, Regelbezug und die offenen Punkte der Oberfläche in §15.
>
> **Die Abschnitte §1 bis §9 aus Teil 1 stehen unverändert**; wo Teil 2 sie berührt, steht ein
> datierter Verweis daneben und keine stille Ersetzung.

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
| ~~E‑97~~ | **Gegenstandslos, und der Grund ist korrigiert:** nicht weil die Suche in der Prozessansicht fehlt, sondern weil sie dort **hinausführt** — eine Marke wird nie *in* der Prozessansicht aktiv. Das Feld steht in der Kopfzeile, die Suche navigiert auf ihre eigene Route. **Die Prozessansicht bekommt keine Filterleiste und keine Markenzeile** *(bestätigt in Teil 2)* |
| **E‑109** *(Teil 2)* | **`Message.ProcessName` wird vorab über die Stammdaten aufgelöst** und dann als `ProcessID IN (…)` gesucht — die Form der Nachrichtenliste (§10). Entscheidung des Auftraggebers zu offenem Punkt 10; `SOSName`, `SOSID` und `Status` bleiben, wie sie sind |
| **E‑110** *(Teil 2)* | **Bei einer Suche ohne Belegnummer entfällt die Spalte „Treffer"**, statt leer zu bleiben (§11.5). Entschieden an der Frage — dem Zitat der Antwort —, nicht an den Zellen |
| **E‑111** *(Teil 2)* | **Der Absprung rundet das Fenster nach außen auf volle Stunden und deckelt es am Jahr**; liegt die Nachricht danach außerhalb, gibt es keinen Link (§12). Der Baum nimmt nur stundengenaue Fenster an (E‑95) — ein Link, der in einem anderen Fenster landet als versprochen, ist schlechter als kein Link |

**Nummernvergabe in Teil 2:** E‑109 bis E‑111, ermittelt über den Höchstwert — höchste vorher
vergebene Nummer **E‑108** (`perl -CSD` mit der Zeichenklasse aller vier Bindestriche über `docs/`
und die Wurzeldateien, weil `\b` an U+2011 nicht greift), Gegenprobe an E‑105 (23 Treffer in fünf
Dateien) und am bekannten Falschtreffer E‑780 (drei Treffer, alle in der Messdatei und hier).

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

> *Seit Teil 2 (08.09.2026):* Die Abbildung ist unverändert, aber **`Message.ProcessName` läuft
> nicht mehr über einen Join im Kern**, sondern wird vorab über `Process` zu Kennungen aufgelöst
> (E‑109, §10). Die Zielspalte bleibt `Process.ProcessName` — verglichen wird weiterhin gegen sie,
> nur an einer anderen Stelle des Ablaufs.

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

> *Seit Teil 2 (08.09.2026) gilt der Satz nur noch für `Message.SOSName`.* `feld_process` gibt es
> nicht mehr; `Message.ProcessName` wird vor dem Kern zu `ProcessID`s aufgelöst und im Kern als
> `ProcessID IN (…)` gesucht — **§10**, E‑109. Der Absatz bleibt stehen, weil er den Stand von
> Teil 1 beschreibt, gegen den §10 misst.

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
>
> *Nachtrag Teil 2 (08.09.2026):* **Jetzt ist es gebaut** — Entscheidung des Auftraggebers zu
> Punkt 10, E‑109, gemessen in **§10**: 3,041 ms über 30 Tage und 3,058 ms über ein Jahr bei
> `NEXANS`, Gleichheitsprobe gegen die Join-Form bestanden. Die Zeile `Message.ProcessName` in der
> Tabelle oben beschreibt seither den **Stand von Teil 1**; `Status`, `SOSID` und `SOSName` stehen
> unverändert.

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
    *Entschieden am 08.09.2026 (Teil 2):* `Message.ProcessName` wird umgebaut (E‑109, §10), die
    drei übrigen bleiben — für sie ist der Abbruch über ein Jahr der Regelfall und der Rat, den
    Zeitraum zu verkleinern, die richtige Auskunft (§11.7).
11. **`Message.ProcessName` vorab in Kennungen auflösen** — die Form der Nachrichtenliste
    ([`nachrichtenliste.md`](nachrichtenliste.md) §5) — machte aus 4.592 ms die 3 ms von
    `Message.ProcessID` (§6.4), weil die Mandantenkette dann zur Konstante wird. Gemessen an der
    Kennungsform, nicht gebaut; für `SOSName` hülfe es nicht. Hängt an Punkt 10.
    *Erledigt am 08.09.2026 (Teil 2):* gebaut und gemessen, **§10**.
12. **Ein Feldbegriff, den kein Treffer erfüllt, kostet den vollen Wertbereich** (§6.5: 1,35 s für
    eine leere Antwort bei `SNDPRN` und `Status`). Das ist der Preis der Verundung über den
    Wertindex und keine Fehlbedienung; ob die Oberfläche davor warnt, ist Teil 2.

> **Die offenen Punkte der Oberfläche entstehen in Teil 2** und gehören dann neben die Ansicht, die
> sie betreffen — sie stehen in **§15**.

---

## 10. Korrektur — `Message.ProcessName` über die Stammdaten (E‑109)

> ### ⚠️ Korrekturblock vom 08.09.2026 — die Bauvorgabe aus Teil 1 wird zurückgenommen
>
> **Der Auftrag zu Teil 1 sagte:** *„Zwei von ihnen brauchen einen Join (`Process`, `SOS`)."* Für
> `Process` war das die schlechtere Vorgabe, und sie wird hiermit **zurückgenommen** — nicht still
> ersetzt. Gebaut war der Join, wie beauftragt (§4, `feld_process`), und §6.4 hat gemessen, was er
> kostet: **4.592 ms** über 30 Tage und **Abbruch in sechs von sechs Läufen** über ein Jahr, während
> `Message.ProcessID` — **dieselbe Menge** — **3 ms** kostet. Der Unterschied ist allein, wo die
> Mandantenkette ausgewertet wird: Steht die Kennung als Konstante im Statement, wird
> `EXISTS (… WHERE ProcessID = ?)` zur Konstante und der Zeitindex nur bis zur Deckelung gelesen;
> kommt der Name über den Join, läuft die Kette je Zeile und das ganze Fenster geht in die
> temporäre Tabelle.
>
> **Entscheidung des Auftraggebers zu offenem Punkt 10:** `Message.ProcessName` wird umgebaut
> (E‑109); `Message.SOSName`, `Message.SOSID` und `Message.Status` bleiben, wie sie sind. Die
> Zeilen zu `ProcessName` in §3, §4 und §6.4 beschreiben seither den Stand von Teil 1 und tragen je
> einen datierten Verweis hierher.

### Was gebaut ist

**Dieselbe Form, die die Nachrichtenliste für ihren Freitext seit Schritt 4 verwendet**
([`nachrichtenliste.md`](nachrichtenliste.md) §5, *„niemals gegen `Message`"*): Der Name wird
**vor** dem Kern in den Stammdaten aufgelöst, der Kern filtert über die Kennung.

```sql
-- (1) die Auflösung: nur Process und ProjectMandant, exakt, mit Mandantenfilter
select distinct `Process`.`ProcessID` from `Process`
  join `ProjectMandant` on `ProjectMandant`.`ProjectID` = `Process`.`ProjectID`
 where `ProjectMandant`.`MandantID` = ? and `Process`.`ProcessName` = ?
 order by `Process`.`ProcessID`

-- (2) der Kern, unverändert bis auf das Prädikat: statt feld_process.ProcessName = ?
 where (`Message`.`ProcessID` in (?) and `Message`.`MessageLastUpdate` >= ? … and exists (… Mandantenkette …))
```

| | |
|---|---|
| **Exakt, kein `LIKE`** | Anders als der Freitext der Liste: Der Nutzer hat einen konkreten Namen gewählt, und die Suche findet genau ihn. Keine Maskierung nötig, weil kein Muster |
| **Mandantenfilter auch in der Auflösung** | aus demselben Grund wie in der Liste — nicht als Sicherheitsgrenze (die trägt der Kern, Regel M3), sondern damit ein Prozessname eines fremden Mandanten gar nicht erst zu einer Kennung wird. `FeldSucheIsolationDbIT` (Fall 5) prüft beide Pfade |
| **Trifft der Name nichts, wird `Message` nicht angefasst** | Die Verundung ist leer, die Antwort `200` mit leerer Liste — **ohne** ein Statement gegen `Message`, auch nicht neben einer Belegnummer. `FeldSucheStatementsTest` zählt die Statements (Regel T1: Zugriffe, nicht Zeit) |
| **Derselbe Name zweimal** | eine Auflösung, zwei Prädikate — dieselbe Verundung wie vorher beim Join, nur ohne ihn. Zwei verschiedene Namen: zwei Auflösungen, zwei Prädikate, leer bei disjunkten Kennungen |
| **Mehrere Kennungen zu einem Namen** | alle in der `IN`-Liste, **keine Deckelung**. Die Liste ist durch die Tabelle begrenzt (1.503 Zeilen in `Process`, davon ein Bruchteil je Mandant); eine Deckelung änderte still die Treffermenge |
| **Bei genau einer Kennung** | macht MariaDB aus `IN (?)` ein `= ?`, und die Kette wird zur Konstante — die Form aus der vierten Zeile von §6.4. Gemessen ist genau dieser Fall (unten) |
| **`feld_process` gibt es nicht mehr** | `feld_sos` bleibt, weil derselbe Umbau für `SOSName` nicht hülfe: `Message` trägt keinen Index auf `SOSID`, die Kennung liest das Fenster ebenso (§6.4). Einen Index anlegen dürfen wir nicht — `GlassfishDB` ist lesend (S1) |

Der BAM-Pfad ist unberührt: `BamPfadGleichheitTest` rendert weiterhin Zeichen für Zeichen den
Abzug vom 08.09.2026 vor der ersten Codeänderung (§8), alle fünf Fälle grün.

### Gemessen (Regel L7) — `scripts/messung-property-suche/p5-prozessname.sql`

Rahmen wie §6.1: Testkopie (`@@global.read_only` = 1), Benutzer `monitor_read`, Serverzeit
`15:27:18` bis `15:27:25`, zwei Sitzungen nacheinander (`p5a-nexans.sql`, `p5b-suttons.sql` setzen
`@mandant` und laden dasselbe Skript). **Die beiden Statements sind von jOOQ gerendert**
(`MockConnection`, `STATIC_STATEMENT`) und Zeichen für Zeichen die des Codes; der Kern steht in der
Hülle `SELECT COUNT(*), MAX(CHAR_LENGTH(ProcessName)) FROM (…)` wie in §6, die Zeitpunkte als
Literale wie in `p3-typ0.sql`. Prüfwert je Sitzung: der **häufigste Prozessname des 30‑Tage-Fensters
des Mandanten**, hergeleitet wie in §6.4 und nur als Länge ausgegeben (17 Zeichen bei `NEXANS`, 30
bei `SUTTONS`); **beide Namen tragen beim Mandanten genau eine Kennung.** Fenster wie §6: 30 Tage
ab `2025-11-30`, ein Jahr ab `2024-12-30`, Ende `2025-12-30 00:00:00` einschließlich. Wanduhr
(`SYSDATE(6)`), Aufwärmlauf und fünf Läufe, gewertet der Bestwert der Läufe 2 bis 6, das Profil
daneben; Eichung `SELECT 1`: 0,748 ms (`NEXANS`), 0,995 ms (`SUTTONS`).

| Mandant | Statement | Wanduhr, beste von fünf | Profil | Zeilen | Plan |
|---|---|---:|---:|---:|---|
| `NEXANS` | (1) Auflösung | **2,836 ms** | 2,016 ms | 1 Kennung | `ProjectMandant` `ref` `ProjectMandant_Mandant_idx` (17) → `Process` `ref` `Process_ProjectFK` (5); `Using temporary; Using filesort` für `DISTINCT` und `ORDER BY` |
| `NEXANS` | (2) Kern, 30 Tage | **3,041 ms** | 2,187 ms | 51 | `mandanten_process` **`const`**, `ProjectMandant` **`const`**, `Message` `range` `MessageLastUpdateIDX` (409.758 geschätzt), **kein `filesort`**; 1.634 `tmp_write` |
| `NEXANS` | (2) Kern, **ein Jahr** | **3,058 ms** | 2,152 ms | 51 | wie 30 Tage, 1.780.243 geschätzt, 2.266 `tmp_write` |
| `SUTTONS` | (1) Auflösung | **1,497 ms** | 0,661 ms | 1 Kennung | wie oben, `ProjectMandant` 1 Zeile |
| `SUTTONS` | (2) Kern, 30 Tage | **24,844 ms** | 23,671 ms | 51 | Zeichen für Zeichen derselbe Plan wie bei `NEXANS` |
| `SUTTONS` | (2) Kern, **ein Jahr** | **25,254 ms** | 23,976 ms | 51 | wie 30 Tage |

**Das Fenster spielt keine Rolle mehr** — 3,041 gegen 3,058 ms, 24,8 gegen 25,3 ms —, weil der
Zeitindex absteigend gelesen wird, bis 51 Zeilen des Prozesses beisammen sind, und die Kette als
Konstante im Plan steht. **Gegen Teil 1:** 4.592,376 ms → 3,041 ms über 30 Tage (Faktor 1.510),
Abbruch → 3,058 ms über ein Jahr. **Der kleine Mandant ist langsamer als der große**, und das ist
kein Widerspruch: Sein häufigster Prozess trägt 2.932 Nachrichten im Fenster gegen 99.290 bei
`NEXANS`; je seltener der Prozess, desto weiter läuft die absteigende Indexlese, bis 51 Treffer
stehen. Die Handler-Zähler zeigen den Unterschied nicht — `read_key` 4, `read_next` 0,
`read_rnd_next` 1.640, `tmp_write` 1.634 in beiden Sitzungen gleich —, weil die absteigende Lese
über `read_prev` läuft und der nicht erhoben ist. Die Wanduhr trägt ihn.

**Die Auflösung kostet 1,5 bis 2,8 ms und liest nur die Stammdaten** — 750 `read_next` bei `NEXANS`
(17 Projekte, je bis zu fünf Prozesse), 18 bei `SUTTONS`. Zusammen mit dem Kern liegt die neue Form
bei `NEXANS` unter 6 ms für beide Fenster.

### Die Gleichheitsprobe — dieselbe Treffermenge, alt gegen neu

Vorregistriert: Ein Unterschied ist ein Befund und kein Rundungsfehler; dann wird gemeldet und nicht
weitergebaut. Verglichen über 30 Tage, je Mandant, **gedeckelt wie im Code** (die 51 Zeilen des
Kerns) und **ungedeckelt über das ganze Fenster**, als Zahl und als reihenfolgeunabhängige
Prüfsumme der Kennungen (`BIT_XOR(CRC32(MessageID))`):

| Mandant | Form | gedeckelt: Zeilen / Prüfsumme | ungedeckelt: Zeilen / Prüfsumme |
|---|---|---|---|
| `NEXANS` | alt (Join `feld_process`) | 51 / 1016523881 | 99.290 / 2655921933 |
| `NEXANS` | **neu** (`ProcessID IN`) | **51 / 1016523881** | **99.290 / 2655921933** |
| `SUTTONS` | alt | 51 / 3586533450 | 2.932 / 177742654 |
| `SUTTONS` | **neu** | **51 / 3586533450** | **2.932 / 177742654** |

**Alle vier Paare gleich.** Die Probe ist ein Mengenvergleich und kein Laufzeitvergleich — ihre
Statements wählen nur die Kennung und tragen keine Beschriftung; dass die alte Form dort 1.077 statt
4.592 ms kostet, sagt nichts über §6.4 aus.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* die neue Form für den häufigsten Prozessnamen je Mandant über 30 Tage und ein
> Jahr, bei `NEXANS` und `SUTTONS`, mit **genau einer Kennung je Name**; die Gleichheit der
> Treffermenge über 30 Tage für dieselben zwei Namen.
> *Behauptet wird:* dass `Message.ProcessName` seit Teil 2 die Form der Kennung kostet und dieselbe
> Menge findet.
> **Die Lücke:** Ein Name, der beim Mandanten **mehrere** Kennungen trägt, rendert `IN (?, ?)`,
> und ob die Kette dann noch zur Konstante wird, ist nicht gemessen — die Verundung bleibt
> richtig, der Plan könnte ein anderer sein (offener Punkt 13 in §15). Und die Gleichheit ist für
> zwei Werte belegt, beide unter der Kollation der Spalte verglichen; ein Name in anderer
> Schreibweise findet in beiden Formen dasselbe, weil beide gegen `Process.ProcessName` vergleichen
> — das ist eine Aussage über die Form, keine gemessene.

---

## 11. Die Oberfläche

Route `/suche`, Feld in der Kopfzeile, Feature `features/nachrichten` — **die gebaute Fläche aus
[`bam-suche.md`](bam-suche.md) §11, erweitert, nicht ersetzt.** Was dort steht, gilt weiter; hier
steht nur, was dazukommt oder sich ändert.

```
features/nachrichten/
├─ api.ts                       + Suchfelder, SuchfeldBam, SuchfeldFeld, FeldBegriffTreffer,
│                                 holeSuchfelder; BamSuchergebnis.felder — holeBamTypen entfällt
├─ suche.ts                     + Feldbegriff, Suchmarke, ausFeldParameter, parseAsFelder,
│                                 markenschluessel, markenAus, alsZustand, zeigtTrefferspalte,
│                                 begriffeInAntwort; ergaenze über Marken; fünfte Bedingung
├─ hooks.ts                     useSuchfelder statt useBamTypen; useSuchzustand liefert marken,
│                                 felder, setzeMarken
└─ components/
   ├─ suchfeld.tsx              zwei Gruppen im Auswahlmenü, Platzhalter „Wert suchen" bei Feld
   ├─ marken-leiste.tsx         Marken beider Arten, Schlüssel mit Art voran
   ├─ treffer-tabelle.tsx       mitTrefferspalte
   └─ suche-ansicht.tsx         Felder im Leerzustand, Abbruchtext mit Feld, Trefferspalte,
                                 Absprungfenster aus der Antwort
components/marke.tsx            unverändert — keine zweite Markengestalt
```

### 11.1 Das Angebot im Feld

Die Auswahl neben dem Suchfeld liest `GET /api/bam/suchfelder` (§2.1) und zeigt **zwei Gruppen**
in einem Menü: *Belegarten* aus `bam`, *Felder* aus `felder`, je mit Überschrift und Trennlinie.

- **Feldnamen unverändert** (E‑105), in fester Laufweite: Wer `Message.SNDPRN` nicht versteht,
  sieht `Message.SNDPRN`. Keine Beschriftung, keine Übersetzung, kein Erklärtext daneben. Übersetzt
  sind allein die beiden Gruppenüberschriften und die Beschriftung des Schalters (*„Feld: …"*).
- **Die Ordnung ist die des Endpunkts.** Die Belegarten in der Ordnung des Altsystems, die Felder
  alphabetisch — die Tabelle hat keine Sortierspalte, und die Oberfläche sortiert nicht nach.
- **Der typlose Eintrag bleibt und behält seinen Platz** — zuerst, außerhalb beider Gruppen, mit
  der Beschriftung *„Alle Belegarten"*: Ein Wert ohne Auswahl sucht Belegnummern unter jeder
  Belegart und **erreicht nie ein Feld** (E‑100). Ein Feld muss gewählt sein, bevor eine Feld-Marke
  entsteht; deshalb kann die Oberfläche `feldname-fehlt` und `feldbegriff-ohne-trenner` nicht
  erzeugen.
- **Ist ein Feld gewählt, sagt der Platzhalter „Wert suchen"** statt „Belegnummer suchen" — die
  zweite Beschriftung wäre dann eine falsche Auskunft. Das Label für Vorleseprogramme trägt den
  Feldnamen.
- **Nur, was das Angebot dieses Mandanten kennt, ist wirksam.** Die Auswahl liegt im
  Komponentenzustand und überlebt einen Mandantenwechsel; zeigt sie danach auf einen Typ oder ein
  Feld, das es hier nicht gibt, gilt „keine Auswahl" — sichtbar am Schalter und beim Abschicken.

> **Eine bestehende Verhaltensregel ändert sich, und das ist zu benennen.** Bis Teil 2 erschien die
> Auswahl **gar nicht**, wenn der Mandant keine Belegart konfiguriert hatte
> ([`bam-suche.md`](bam-suche.md) §10). **Sie erscheint jetzt, sobald eine der beiden Gruppen etwas
> enthält** — und die Feldgruppe ist für keinen Mandanten leer, weil die acht Typ‑0‑Einträge
> global sind (M154). Für `EDITIONLINGERI`, `SYSTEM` und `WOC` erscheint sie damit doch, nur ohne
> die Gruppe *Belegarten*; für neun Mandanten enthält die Feldgruppe ausschließlich Typ‑0‑Einträge.
> **Eine leere Gruppe wird weggelassen**, nicht als leere Überschrift gezeigt. Der Korrekturkasten
> steht in `bam-suche.md` §10 und §11.1.

**`GET /api/bam/typen` ruft die Oberfläche nicht mehr.** Seine Antwort steckt Zeichen für Zeichen in
der Gruppe `bam` des neuen Endpunkts; zwei Aufrufe auf jeder Seite für dieselbe Liste wären einer zu
viel. Der Endpunkt bleibt bestehen (§9, Punkt 5 — der dort offene Ablösefall ist damit näher, nicht
entschieden). Das Angebot wird wie bisher lange gehalten (15 Minuten) und beim Mandantenwechsel mit
dem ganzen Zwischenspeicher geleert.

**Der Leerzustand nennt die Felder des Mandanten** unter den Belegarten, mit demselben Satzbau und
derselben Regel: nur, was nachweislich für diesen Mandanten gilt, und keine leere Liste. Der Satz
dazu nennt die Weiche aus E‑100 — ohne gewähltes Feld wird nie ein Feld durchsucht.

### 11.2 Die Marken

`components/marke.tsx` ist unverändert — **es gibt keine zweite Markengestalt im Projekt.** Geändert
ist, was eine Marke beschriftet und was ihr Schlüssel ist:

| | Schlüssel | Beschriftung |
|---|---|---|
| BAM | `bam:` + `<typ>:<wert>` — die Parameterform, unverändert | wie bisher: Belegart, dahinter der Wert |
| Feld | `feld:` + `<name>:<wert>` | der Name unverändert (gedämpft), dahinter der Wert in fester Laufweite |

**Der Schlüssel trägt die Art voran**, und das ist kein Schmuck: `9012:4711` kann eine Belegart mit
Wert oder ein Feldname mit Wert sein. Ohne die Art wären beide derselbe React-Schlüssel — eine
Meldung in der Konsole und sichtbar nichts; `tests/suche-marken.test.tsx` rendert genau diesen Fall
und besteht, weil kein `console.error` fällt.

- **Das Geländer von acht zählt beide Arten zusammen.** Das Backend tut es auch (§2.2); ein
  Frontend, das mehr zuließe, führte in ein `400`. Bei acht ist `+` gesperrt, mit der bekannten
  Begründung über der Liste.
- **Ein doppelter Begriff wird nicht abgelegt, und die Prüfung läuft über beide Arten hinweg:** Eine
  BAM-Marke und eine Feld-Marke sind nie Dubletten voneinander, zwei Feld-Marken mit gleichem Namen
  und Wert schon. Die vorhandene Marke hebt sich hervor, wie bisher.
- **Geteilt wird am ersten Doppelpunkt**, wie beim BAM-Parameter. Alles dahinter ist Wert; ein
  Feldname trägt keinen Doppelpunkt (Konfiguration mit vierzehn Zeilen, M161).
- **Jede Änderung an den Marken setzt auf `exakt` zurück** — beim Hinzufügen wie beim Entfernen,
  gleich welcher Art, aus dem Grund aus [`bam-suche.md`](bam-suche.md) §23.

### 11.3 Route und URL-Zustand

`feld` ist ein **wiederholbarer** Parameter neben `begriff`, in derselben Form wie am Endpunkt.
Gelesen wird über dieselben Parser wie am Feld (`parseAsFelder` neben `parseAsBegriffe`), damit das
Geländer greift, sobald es greifen muss; der Parser ist **strenger als das Backend, in dieselbe
Richtung**: Was dort `feldname-fehlt` oder `feldbegriff-ohne-trenner` wäre, wird hier übergangen.

**Die Marken über der Liste werden aus beiden Parametern rekonstruiert** — erst die Belegnummern,
dann die Felder. Die URL trägt zwei getrennte Parameter, und die Reihenfolge *zwischen* den Arten
steht in keinem von beiden; innerhalb einer Art bleibt sie, wie getippt. Das Backend verundet
ohnehin und ordnet den Einstieg selbst (kein `STRAIGHT_JOIN`).

**Die begonnene Eingabe und die gewählte Belegart oder das gewählte Feld stehen nicht in der URL** —
dieselbe Prüfung wie heute für die Belegart ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8):
Sie beschreiben eine begonnene Eingabe, keinen Ausschnitt.

**Ohne `feld` ist die Abfrage einer reinen BAM-Suche Zeichen für Zeichen die von Teil 3.**
`tests/suche.test.ts` hält es fest; am Backend hält es `BamPfadGleichheitTest`.

### 11.4 Über der Liste — und eine gemeldete Abweichung vom Auftrag

> **Der Auftrag sagte in §3.4:** *„Die Marken kommen aus `begriffe` und `felder` der Antwort, nicht
> aus der URL."* **Und in §3.3:** *„Die Marken über der Liste werden aus beiden Parametern
> rekonstruiert."* Beides zugleich geht nicht, und die Datei entscheidet: [`bam-suche.md`](bam-suche.md)
> §11 lässt Marken und Zeitfenster **in jedem Zustand** stehen, weil sie der Weg aus einem leeren
> Ergebnis heraus sind — auch beim Abbruch, wo es keine Antwort gibt. Kämen die Marken aus der
> Antwort, verschwänden sie genau dann, wenn der Nutzer eine entfernen muss.
>
> **Gebaut ist deshalb:** Die **Marken** kommen aus der URL (§3.3). Das **Zitat der Antwort** —
> `begriffe` und `felder` — trägt alles, was über der Liste *über die gelaufene Suche* aussagt:
> die Zahl der Begriffe für die Nulltreffer-Zeile, die Frage, ob eine Belegnummer dabei war
> (Trefferspalte, fünfte Bedingung), und die Varianten. Gemeldet als Abweichung.

**`felder` ist immer vorhanden, leer statt fehlend** (§2.2), und die Oberfläche prüft nicht auf
Abwesenheit, sondern zählt (`begriffeInAntwort`): Die Nulltreffer-Zeile *„Mit diesem Begriff: 0.
Ohne ihn: 12."* vergleicht die vorige Runde jetzt über beide Arten — eine Feld-Marke, die auf null
führt, bekommt dieselbe Zeile wie eine Belegnummer.

### 11.5 Die Trefferspalte bei reiner Feldsuche (E‑110)

`nachrichten[].treffer` enthält **ausschließlich BAM-Treffer**. Bei einer Suche allein über Felder
ist sie in jeder Zeile leer — richtig so: Ein Feldtreffer ist der eingegebene Wert selbst und steht
als Marke über der Liste.

**Entscheidung: Die Spalte entfällt**, sie bleibt nicht leer. Eine Überschrift *Treffer* über
fünfzig leeren Zellen wäre keine Auskunft, sondern ein Rätsel; ohne sie bekommt der Ablauf ihre
Breite, und Zeitpunkt, Status, Kette bleiben, wo sie sind. Bei einer **gemischten** Suche trägt jede
Zeile einen BAM-Treffer — sie muss jede Belegnummer erfüllen —, und die Spalte sagt wie bisher,
**worauf** die Nummer getroffen hat.

**Entschieden wird an der Frage, nicht an den Zellen:** `zeigtTrefferspalte` liest, ob die Antwort
eine Belegnummer zitiert (`begriffe.length > 0`), nicht, ob die Zellen leer sind. Ob Zellen leer
sind, hängt an den Daten; ob eine Belegnummer gefragt war, an der Frage — und eine Tabelle, die je
nach Daten ihre Gestalt wechselt, ist keine. Als reine Funktion, mit Test; dazu der gerenderte
Baum, der die **Abwesenheit** von Überschrift und Zelle nachweist (§13).

### 11.6 Der Präfix-Knopf bei reiner Feldsuche

`modus` wirkt **nur auf Belegnummern**; Feldbegriffe werden immer exakt verglichen (§2.2, §4). Ein
Knopf, der bei reiner Feldsuche erschiene, verspräche eine Wirkung, die es nicht gibt.

`zeigtPraefixAngebot` hat deshalb eine **fünfte Bedingung: mindestens ein BAM-Begriff in der
Antwort.** Sie steht in derselben reinen Funktion wie die vier aus [`bam-suche.md`](bam-suche.md)
§23 und hat einen eigenen Test — derselbe Fall wie die vierte: Sie ergäbe sich auch aus der Gestalt
der Marken, aber das wäre eine Eigenschaft des Markups und keine Zusage. Gezählt werden die
Belegnummern **der Antwort**, weil das Angebot zu der Suche gehören muss, die tatsächlich leer
ausgegangen ist.

Bei einer **gemischten** Suche erscheint der Knopf weiterhin — und wirkt dann auf die Belegnummern,
während die Feldbegriffe exakt bleiben. Das ist der Rückfall aus offenem Punkt 3 in §9; ob er die
richtige Antwort ist, bleibt offen (§15).

### 11.7 Abbruch, Fehlerfälle, Leerzustand

- **Der Abbruchpfad** `400 suche-abgebrochen` greift auch für Feldbegriffe — und für
  `Message.Status`, `Message.SOSID` und `Message.SOSName` über ein Jahr ist er **der Regelfall und
  nicht die Ausnahme** (§6.4: 24 von 24 Abbrüchen). Der Satz über der Liste wählt deshalb bei
  Feldbegriffen einen anderen Rat: *„Verkleinere den Zeitraum — bei Feldern wie Status oder
  Ablaufname ist das über ein Jahr der Regelfall."* Eine zweite Belegnummer wäre dort eine Antwort
  auf eine andere Frage. Entschieden am Zustand der URL, weil es beim Abbruch keine Antwort gibt;
  **übersetzt wird weiterhin über den `type`**, welcher von zwei Sätzen erscheint, entscheidet die
  Ansicht ([`bam-suche.md`](bam-suche.md) §11.7). Kein „Erneut versuchen", kein zweiter
  Problemtyp.
- **Zwei neue Problemtypen**, `feldname-fehlt` und `feldbegriff-ohne-trenner`, stehen im
  Fehlerkatalog (`texte.fehler`) mit den Texten des Backends. **Die Oberfläche kann sie nicht
  erzeugen** — ein Feld muss gewählt sein, bevor eine Feld-Marke entsteht, und der Parser übergeht
  eine von Hand gebaute Adresse ohne Namen. Die Behandlung ist trotzdem da: Der allgemeine
  Fehlerbaustein übersetzt über den `type`.
- **Zwei erweiterte Texte:** `suchbegriff-fehlt` und `zu-viele-suchbegriffe` stehen mit den Texten
  des Backends im Katalog und benennen beide Begriffsarten. Nicht neu formuliert.
- **Der Leerzustand** nennt neben den Belegarten die Felder des Mandanten (§11.1); die übrige
  Hilfe ist unverändert.

---

## 12. Der Absprung in den Prozessbaum (E‑103, E‑104, E‑111)

**Wo:** im **Detailpanel** der Nachricht, unter der Beschreibungsliste des Kopfs — nicht als
Kontextmenü, nicht als Spalte in der Liste. Grund: Tastatur- und Berührungserreichbarkeit, dieselbe
Disziplin wie bei den Berührungsflächen des Baums; der Verweis trägt `min-h-beruehrung` und einen
Fokusring. **Es ist ein echter Verweis** (`next/link`): mit der mittleren Maustaste zu öffnen, zu
kopieren, per Tab erreichbar.

**Wohin:** die bestehende Route, unverändert —

```
/prozesse?von=<ISO,UTC>&bis=<ISO,UTC>&prozess=<ProcessID>&nachricht=<MessageID>
```

Der aufgeklappte Partner steht **nicht** in der URL; er ergibt sich über `pfadZuProzess` aus dem
gewählten Prozess ([`process-view.md`](process-view.md) §15). Gebaut über `alsSuchparameter` der
Prozessansicht, damit die Adresse dieselbe Gestalt hat wie die, die sie selbst schreibt, und derselbe
Parser sie liest. **Es ist ein Link und kein Bau** — an der Prozessansicht ist nichts geändert.

### Die Prüfung, die vor dem Bau stand: nimmt die Prozessansicht ein freies `von`/`bis` an?

**Ja, seit 10c‑4b** ([`process-view.md`](process-view.md) §37–§41) — mit zwei Bedingungen, die
den Absprung prägen: **Ein Zeitpunkt muss auf einer vollen Stunde der Anwendungszone liegen**, sonst
`zeitfenster-zu-genau` (E‑95, abgewiesen statt gerundet); und `bis` ist dort die **letzte enthaltene
Stunde** (E‑94), die Jahresgrenze rechnet gegen das ausschließende Ende. Das Fenster der Suche ist
dagegen sekundengenau — sein `bis` ist die Anwendungsuhr.

### Das Zeitfenster (E‑104) — und die Rundung (E‑111)

Der Absprung reicht **immer ein absolutes `von`/`bis`** weiter, nie den relativen Modus: Die
relativen Zeiträume von Liste und Baum fallen paarweise nicht zusammen (`24h`/`7d`/`30d` gegen
`48H`/`30T`/`12M`), und ein durchgereichtes `24h` ließe den Baum ein anderes Fenster wählen als das,
aus dem man kommt.

**Gerundet wird nach außen auf volle Stunden** (`prozessansicht.ts` `absprungfenster`): `von` auf
die volle Stunde davor, `bis` auf die volle Stunde, in der es liegt. Das ist das kleinste Fenster,
das der Baum annimmt und das das Herkunftsfenster ganz enthält; es ist an jedem Ende **höchstens
59 Minuten 59 Sekunden weiter**. Die Rundung setzt volle Stunden in UTC voraus — die Anwendungszone
`Europe/Berlin` hat einen ganzstündigen Versatz, dort fallen die Stundengrenzen mit denen in UTC
zusammen; bei einer Zone mit halbstündigem Versatz käme aus dem Baum `zeitfenster-zu-genau`, ein
sichtbarer Fehler und kein falsches Fenster.

**Am Jahr gedeckelt.** Die Suche erlaubt ein Kalenderjahr, und „Auf ein Jahr erweitern" wählt 365
Tage; nach außen gerundet wären das 365 Tage und bis zu zwei Stunden, und der Baum wiese das als
`zeitfenster-zu-gross` ab. `von` rückt deshalb nie weiter zurück als 365 Tage vor dem ausschließenden
Ende — dieselbe Zahl wie `jahresfensterAb` und aus demselben Grund (der Schalttag).

**Die Nachricht ist im Zielfenster per Konstruktion enthalten — und das wird geprüft, nicht
angenommen:** Liegt ihr `zeitpunkt` außerhalb des gerundeten und gedeckelten Fensters (ein tiefer
Link auf einen Beleg außerhalb des Suchfensters; eine Nachricht in der durch den Deckel verlorenen
Stunde), **gibt es keinen Link.** Ein Link, der in einem anderen Fenster landet als versprochen, ist
schlechter als kein Link.

> **Gemeldete Abweichung:** Das Zielfenster ist damit **nicht identisch** mit dem Herkunftsfenster,
> sondern das kleinste stundengenaue Fenster, das es enthält. Die Sichtprüfung (§14, Punkt 10) misst
> beides gegeneinander.

### Woher das Fenster kommt — je Einhängepunkt

| Einhängepunkt | Fenster | Link |
|---|---|---|
| **Trefferliste der Suche** | `von`/`bis` **aus der Antwort** — das tatsächlich verwendete, gegen die Anwendungsuhr aufgelöst (Regel Z1) | ja, sobald eine Antwort da ist; beim Laden und beim Abbruch nicht |
| **Nachrichtenliste, freies Fenster** | `von`/`bis` aus der URL | ja |
| **Nachrichtenliste, relativer Modus oder Vorgabe** | — | **nein** |
| eigene Route `/nachrichten/<id>` | — | nein |
| Prozessansicht | — | nein, man ist schon dort |

> **Gemeldete Abweichung:** Der Auftrag sagt, der Absprung gelte von beiden Listen aus ohne
> zweiten Bau. **Aus der Nachrichtenliste gilt er nur im freien Modus.** Im relativen Modus kennt
> die Oberfläche das aufgelöste Fenster nicht: Die Antwort der Liste (`Seite<Nachricht>`) nennt es
> nicht — anders als die des Baums (`fenster`, E‑50) und die der Suche —, und gegen die Browseruhr
> wird nicht gerechnet (Regel Z1). Ein Link, der nur im Standardfall fehlt, ist besser als einer,
> der dort in ein erfundenes Fenster führt. Was es bräuchte, ist eine Zeile in der Antwort der Liste
> — ein Backend-Bau außerhalb dieses Auftrags (offener Punkt 14 in §15). Auf der eigenen Route
> liest die Seite die Adresse nur im Ereignis (keine Suspense-Grenze für `useSearchParams`,
> [`nachrichtendetail.md`](nachrichtendetail.md) §10), und ein Verweis braucht den Wert beim
> Rendern.

**E‑97 bleibt gegenstandslos, und der Grund ist korrigiert** (§1): nicht weil die Suche in der
Prozessansicht fehlt, sondern weil sie dort **hinausführt** — das Feld steht in der Kopfzeile, die
Suche navigiert auf ihre eigene Route, eine Marke wird nie *in* der Prozessansicht aktiv. **Die
Prozessansicht bekommt keine Filterleiste und keine Markenzeile.**

---

## 13. Tests der Oberfläche

| Datei | Was |
|---|---|
| `tests/suche.test.ts` | **ohne DOM** — die Parameterform von `feld` (Pflichtname, Teilung am ersten Doppelpunkt, Rundlauf `parseAsFelder`, übergangene Werte), `alsAbfrage` mit `feld` neben `begriff` und Zeichen für Zeichen unverändert ohne `feld`; die Marken aus beiden Parametern und zurück (`markenAus`, `alsZustand`); **der Markenschlüssel und die Dublettenprüfung über beide Arten** (gleiche Parameterform, verschiedene Art — keine Dublette); **das Geländer von acht, gemischt gezählt** (vier und vier, die neunte abgewiesen, gleich welcher Art); `zeigtPraefixAngebot` mit der **fünften Bedingung** als eigener Fall, dazu die Gegenprobe gemischt; `zeigtTrefferspalte`; `begriffeInAntwort` **mit leerem `felder`-Array** |
| `tests/suche-marken.test.tsx` | **gerenderter Baum**, sechs Fälle (zwei mehr): dieselbe Parameterform als BAM- und als Feld-Marke **ohne doppelten React-Schlüssel**, und die **Abwesenheit** der Spalte „Treffer" ohne Belegnummer — weder Überschrift noch Zelle, die Kette rückt an die dritte Stelle. Die Zählung im Kopf von `frontend/vitest.config.mts` steht bei **79 in dreizehn Dateien** |
| `tests/prozessansicht.test.ts` | **ohne DOM** — `absprungfenster`: Rundung nach außen, stundengenaues Fenster unverändert, Nachricht außerhalb → kein Link, unlesbarer Zeitpunkt → kein Link, **Jahresdeckel** gegen das ausschließende Ende samt der verlorenen Stunde; `absprungZiel`: die Adresse der Prozessansicht mit absolutem Fenster, von `ausSuchparametern` rund gelesen, `zeitraum` bleibt `null` |
| `FeldSucheStatementsTest` | **ohne DB**, erweitert — `ProcessName` als zwei Statements: Auflösung nur auf `Process` mit Mandantenfilter und `=`, Kern mit `ProcessID IN (?)` und ohne `feld_process`; **kein Statement gegen `Message` bei unbekanntem Namen** (Zugriffe gezählt, T1); mehrere Kennungen in der Liste; derselbe Name zweimal — eine Auflösung; neben einer Belegnummer bleibt `b1` führend |
| `BamPfadGleichheitTest` | **unverändert grün** — der BAM-Pfad rendert weiter den Abzug vom 08.09.2026 |
| `FeldSucheDbIT`, `FeldSucheIsolationDbIT`, `BamSucheDbIT` | `@Tag("db")` — nach dem Umbau gelaufen: 6, 8 und 18 Fälle grün, darunter *„Ein fremder Prozess als Spaltenfeld findet nichts — auch über den Namen"* (jetzt über die Auflösung) |

**Keine Wanduhrzeit als Prüfkriterium** (Regel T1) — kein Test dieses Teils misst eine Dauer; wo es
um Laufzeit geht, zählt `FeldSucheStatementsTest` Statements.

**Prüfläufe:** `pnpm check` (Lint, Typprüfung, Formatprüfung, 35 Dateien, 954 Tests) und
`./mvnw verify -DexcludedGroups=db` (Spotless, ArchUnit, 802 Einheitstests) am 08.09.2026 grün.

---

---

## 14. Sichtprüfung

**Durchgeführt am 08.09.2026** am laufenden System (Backend `localhost:8080` mit dem Stand nach
Commit 1, Oberfläche `localhost:3000`), **kopfloses Chrome 152 über das DevTools-Protokoll** bei
1568 × 900 px — Eingaben wurden geklickt und getippt (`Input.dispatchMouseEvent`,
`Input.insertText`, `Input.dispatchKeyEvent`), nicht gesetzt; von Hand gebaute Adressen sind die
Punkte 4, 8 und 9, und dort *ist* die Adresse der geprüfte Vorgang.

**Angemeldet war ein Wegwerfkonto** `it-sicht-teil2` (Rolle `ADMIN`, angelegt über
`AppUserRepository.legeAn` wie in jedem `DbIT`, Passwort aus einer Umgebungsvariable), weil in
`SPRING_SESSION` keine Sitzung stand und Passwörter des Auftraggebers nicht getippt werden. Der
Mandant wurde über `POST /api/auth/mandant` gesetzt, das Sitzungs-Cookie per `Network.setCookie`
gesetzt. **Nach der Prüfung sind Konto und Sitzungen gelöscht** (`app_user` mit Kaskade,
`SPRING_SESSION`; Gegenprobe: null Zeilen mit dem Präfix). Geschrieben wurde ausschließlich in
`overlord_monitor` (S1); `GlassfishDB` ist unberührt.

**Die Prüfwerte stehen nach Regel G1 nicht in dieser Datei.** Ihre Gestalt: bei `NEXANS` ein Wert
von `Message.SNDPRN` (6 Zeichen, 131 Nachrichten im Fenster), dazu ein Wert des Typs 9018
(`Kundenmaterialnummer_K_SAP`, 8 Zeichen, 61 Nachrichten im Fenster), der auf 35 dieser Nachrichten
mit dem `SNDPRN`-Wert zusammen steht — **hergeleitet innerhalb des Fensters, das die Antwort nennt**
(siehe den ersten Befund unten). Das Fenster der Anwendungsuhr lief während der Prüfung von
*30.11.2025, 07:14* bis *30.12.2025, 07:14* nach *07:20* weiter.

| # | Zu prüfen | Erwartet | Befund |
|---|---|---|---|
| 1 | Angebot mit beiden Gruppen — als `NEXANS` und als Mandant ohne Belegart | zwei Gruppen mit Überschrift; bei `WOC` nur *Felder*, keine leere Überschrift | ✔ `NEXANS`: **55 Einträge** — *Alle Belegarten* (angehakt), Trennlinie, `# Belegarten` mit 40, Trennlinie, `# Felder` mit 14 in der Ordnung des Endpunkts (`Converter.TransactionID` … `Message.VFN`), Feldnamen in fester Laufweite. `WOC`: **9 Einträge**, **eine** Überschrift *Felder*, **eine** Trennlinie, die acht Typ‑0‑Namen; der Leerzustand nennt die Felder und **keine** Belegarten-Liste |
| 2 | Eine Feld-Marke setzen, Treffer prüfen, Marke entfernen | Marke mit Name und Wert, `feld` in der URL, Treffer; nach dem Entfernen die leere URL und der Leerzustand | ✔ Schalter zeigt *„Feld: Message.SNDPRN"*, Platzhalter wechselt auf *„Wert suchen"*; nach Enter `?feld=Message.SNDPRN:<wert>`, eine Marke `Message.SNDPRN: <wert>`, *„Mehr als 50 Treffer — gezeigt werden die 50 neuesten im Zeitfenster …"*. Schließen-Knopf der Marke: URL leer, **null Marken**, *„Wonach suchst du?"* |
| 3 | Feld- und BAM-Marke gemischt, UND sichtbar | zwei Marken, die Trefferzahl sinkt | ✔ Belegnummer allein *„Mehr als 50"*, dann Feld dazu: **35 Treffer**, zwei Marken (`<wert>` und `Message.SNDPRN: <wert>`), Trefferspalte zeigt `Kundenmaterialnummer_K_SAP`. Feld-Marke wieder entfernt: **eine** Marke, *„Mehr als 50"*, URL nur noch `begriff` |
| 4 | Acht Marken beider Arten, `+` gesperrt, Begründung sichtbar | `disabled` und der Satz über der Liste | ✔ vier `begriff`, vier `feld` aus der Adresse: **8 Marken**, `+` `disabled`, im `title` **und** über der Liste *„Mehr als 8 Begriffe nimmt die Suche nicht an — ein Schutzgeländer, keine fachliche Grenze. …"*. Ein neunter Wert mit Enter: weiterhin 8 Marken, die Eingabe bleibt stehen |
| 5 | Reine Feldsuche mit null Treffern | **kein** Präfix-Knopf | ✔ `Message.SNDPRN:gibt-es-nicht-xyz`: *„0 Treffer im Zeitfenster …"*, Leerzustand *„Keine Nachricht mit diesem Beleg"* mit Hinweis — **kein** Knopf *„Nach dem Anfang der Nummer suchen"*, keine Frage |
| 6 | Gemischte Suche mit null Treffern | der Präfix-Knopf erscheint | ✔ Belegnummer (61 Treffer allein) plus das unmögliche Feld: 0 Treffer, **zwei** Marken, *„Soll nach Nummern gesucht werden, die damit anfangen?"* und der Knopf |
| 7 | Reine Feldsuche mit Treffern — die Trefferspalte | entfällt, ohne kaputt auszusehen | ✔ Spalten **Zeitpunkt · Status · Kette · Ablauf**, 50 Zeilen; keine Überschrift *Treffer*, keine leere Zelle. In der gemischten Suche (Punkt 3) steht sie wieder an dritter Stelle |
| 8 | `Message.Status` über ein Jahr bei `NEXANS` | Abbruch nach zehn Sekunden, Rat sichtbar | ✔ Adresse mit `von`/`bis` über 365 Tage: erst Skelett, nach zwölf Sekunden *„Die Suche hat zu lange gedauert und wurde abgebrochen. Verkleinere den Zeitraum — bei Feldern wie Status oder Ablaufname ist das über ein Jahr der Regelfall."* Marke steht, *Zeitfenster ändern* und *Vorgabe wiederherstellen* stehen, **kein** Präfix-Knopf, kein roter Fehlerbaustein |
| 9 | Neu laden mit Marken in der URL | dieselbe Ansicht | ✔ die Adresse aus Punkt 3 geladen: dieselben zwei Marken in derselben Reihenfolge, dieselbe Trefferzeile |
| 10 | Absprung aus dem Detailpanel | Baum aufgeklappt, Prozess gewählt, Nachricht markiert, Fenster wie das Herkunftsfenster | ✔ Zeile geöffnet, im Kopf unter der Beschreibungsliste *„Im Prozessbaum anzeigen"* mit `href=/prozesse?von=2025-11-30T06:00Z&bis=2025-12-30T06:00Z&prozess=…&nachricht=…` — das Suchfenster *07:14–07:14* Anzeigezeit auf volle Stunden gerundet. Klick: `/prozesse`, `role=tree` steht, **Partner (Ebene 1) und Richtung (Ebene 2) aufgeklappt**, der Prozess (Ebene 3) `aria-selected`, *Frei* gedrückt mit *Von 30.11.2025 07:00* und *Bis 30.12.2025 07:00*, Liste mit 50 Zeilen und **einer** `aria-current`-Zeile, Panel offen mit derselben Nachricht (29.12.2025, 08:04:12). Über der Liste *„Zeitraum 30.11.2025, 07:00 bis 30.12.2025, 08:00"* — **das kleinste stundengenaue Fenster um das Herkunftsfenster, nicht dasselbe** (E‑111, Abweichung 2 in §15) |
| 11 | Am schmalen Fenster | Feld, Marken, Tabelle, Angebot, Panel bei 360 px | ✔ **gemessen über `Emulation.setDeviceMetricsOverride` bei 360 × 780 px**: `scrollWidth` = `innerWidth` = 360 (keine zweite Bildlaufleiste); das Suchfeld ist eine **eigene Zeile** der Kopfzeile (links 12 px, 336 px breit, Kopfzeile 131 px hoch); zwei Marken **untereinander** (y 189 und 227); Spalten **Zeitpunkt · Status · Treffer · Kette**, der Ablauf weg; die Tabelle (696 px) scrollt **in ihrer Hülle** (319 px), nicht die Seite. Das Angebot öffnet **im Bild** (Menü 320 px breit, rechter Rand 332) mit beiden Überschriften. Panel geöffnet: Liste ausgeblendet, der Absprung-Verweis da, **44 px hoch**, im Bild |

**Konsole:** über den ganzen Durchgang ausschließlich der React-DevTools-Hinweis, je Seitenaufruf
einmal. **Kein `error`, keine Schlüsselmeldung, keine Ausnahme.**

### Was die Abnahme zusätzlich gezeigt hat

**1. Ein Prüfwertpaar, das ohne Fenster gezogen war, sah eine Runde lang wie ein Fehler in der
Verundung aus.** Der erste Belegwert (Typ 9018) stand per SQL auf einer Nachricht mit dem
`SNDPRN`-Wert — aber die Abfrage hatte kein Zeitfenster. Im Fenster der Anwendung fand die
Belegnummer allein 9 Nachrichten, das Feld allein 131, **beide zusammen 0** — und die
Nulltreffer-Zeile sagte genau das: *„Mit diesem Begriff: 0. Ohne ihn: 9."* Die Gegenprobe per SQL
im Fenster der Antwort: 9 / 131 / **0**; ohne Fenster 435 gemeinsame Nachrichten, die jüngste vom
26.10.2025. **Kein Befund über den Code**, ein Befund über die Herleitung: Paare für die
Sichtprüfung gehören in das Fenster gezogen, das die Antwort nennt. Punkt 3 ist mit einem Paar aus
dem Fenster wiederholt worden (61 → 35).

**2. Der Zeilenklick am schmalen Fenster traf ins Leere**, weil die Zeile 696 px breit in einem
319 px breiten scrollenden Container liegt und der Klick auf die Zeilenmitte außerhalb des
Sichtfelds landete. Mit dem Klick auf die **erste Zelle** öffnete das Panel. Eine Eigenschaft des
Prüfwerkzeugs, nicht der Ansicht — am Finger trifft niemand die Mitte einer verdeckten Zeile.

**3. Die Berührungsfläche der Marken-Schließknöpfe ist in der Emulation nicht messbar.** Sie maßen
32 px; die 44 px aus `--dichte-bedienelement` hängen an `pointer: coarse`, und die Emulation setzt
das nicht. Der Absprung-Verweis trägt seine 44 px unabhängig davon (`min-h-beruehrung`). Der Rest
in der Tabelle *Offene Sichtprüfungen* in [`README.md`](README.md) ist damit auf diesen einen Punkt
geschrumpft.

### Drei Beobachtungen ohne Handlungsbedarf

1. **Die Abschneidemeldung rät auch bei reiner Feldsuche zu einer „zweiten Belegnummer".** Sie ist
   der Text aus Teil 3 und nicht Gegenstand dieses Auftrags („keine Änderung am BAM-Pfad außer den
   zwei erweiterten Fehlertexten"); fachlich stimmt der Rat, eine Belegnummer verengt auch eine
   Feldsuche. Steht als Punkt 20 in §15.
2. **„Auf ein Jahr erweitern" fehlt beim Abbruchfall aus Punkt 8** — richtig so: Das Fenster war
   schon ein Jahr, der Knopf erscheint nur unter 360 Tagen.
3. **Der Baum blendet bei offenem Panel seine Spalte aus** (E‑57) — der Absprung landet damit in
   der Ansicht *Liste plus Panel*, und der gewählte Prozess steht als Überschrift über der Liste.
   Wer den Baum sehen will, schließt das Panel; die Auswahl bleibt.


## 15. Regelbezug und offene Punkte der Oberfläche

### Regelbezug (Teil 2)

| Regel | Wie umgesetzt |
|---|---|
| **M1** | Kein Mandantenparameter — auch nicht am Absprung: `/prozesse` trägt Fenster, Prozess und Nachricht, der Mandant kommt weiter aus der Sitzung |
| **M2** | Die Auflösung des Prozessnamens ist eine private Methode von `BamSucheRepository` mit `MandantContext` als erstem Parameter; `PaketstrukturTest` unverändert grün |
| **M3** | Der Mandantenfilter steht in der Auflösung **und** im Kern (§10); `FeldSucheIsolationDbIT` Fall 5 prüft den Namen über den neuen Pfad |
| **M4** | Kein neuer Endpunkt; die drei Isolationstests aus Teil 1 nach dem Umbau gelaufen (§13) |
| **S1** | Ausschließlich `SELECT`. Ein Index auf `Message.SOSID` wäre der Weg für `SOSName` — und ist ausgeschlossen, weil `GlassfishDB` lesend ist (§10) |
| **L1** | Fenster unverändert; der Absprung reicht ein absolutes weiter und rechnet keines gegen die Browseruhr |
| **L7** | §10 mit `EXPLAIN`, Wanduhr und Profil, zwei Mandanten, zwei Fenster, Gleichheitsprobe |
| **L10** | Belegvermerk in §10; die Lücke (mehrere Kennungen je Name) benannt |
| **Q4** | Nichts geraten: Feldnamen unverändert (E‑105), keine Deutung von `spalte` in der Oberfläche, die Art der Marke steht im Schlüssel statt aus der Parameterform gelesen zu werden |
| **T1** | Keine Wanduhrzeit in einer Zusicherung; `FeldSucheStatementsTest` zählt Statements |
| **Z1** | Das Absprungfenster kommt aus der Antwort der Suche oder aus der URL, nie aus `Date.now()`; die Rundung rechnet auf dem gelieferten Zeitpunkt |

### Gemeldete Abweichungen vom Auftrag

1. **Die Marken kommen aus der URL, nicht aus der Antwort** (§11.4). Der Auftrag sagte in §3.4 das
   eine und in §3.3 das andere; die Datei ([`bam-suche.md`](bam-suche.md) §11) entscheidet für die
   URL, weil die Marken auch beim Abbruch stehen müssen. Das Zitat der Antwort trägt die Zeilen
   darüber.
2. **Das Zielfenster des Absprungs ist nicht identisch mit dem Herkunftsfenster**, sondern das
   kleinste stundengenaue Fenster, das es enthält (§12, E‑111). Der Baum nimmt nichts anderes an.
3. **Aus der Nachrichtenliste gibt es den Absprung nur im freien Modus** (§12). Die Antwort der
   Liste nennt das aufgelöste Fenster nicht; im relativen Modus gäbe es keinen Link, der hält.
4. **Commits 2 und 3 des Auftrags sind zusammengefasst**, weil ein Angebot mit Feldern ohne
   Feld-Marken nicht baubar wäre — der Hauptzweig bleibt je Commit baubar (Richtlinie §10).
5. **Der Leerzustand nennt zusätzlich die Felder des Mandanten** (§11.1). Nicht beauftragt, aber
   dieselbe Regel wie für die Belegarten; ohne sie erklärte die Seite nicht mehr, was gesucht
   werden kann.

### Offene Punkte

13. **Ein Prozessname mit mehreren Kennungen ist nicht gemessen** (§10). Beide gemessenen Namen
    tragen beim Mandanten genau eine Kennung; bei mehreren rendert der Kern `IN (?, ?)`, und ob die
    Mandantenkette dann noch zur Konstante wird, sagt erst ein `EXPLAIN` mit einem solchen Namen.
    Die Verundung bleibt richtig, nur der Plan könnte ein anderer sein.
14. **Die Antwort der Nachrichtenliste nennt ihr aufgelöstes Fenster nicht.** Deshalb gibt es den
    Absprung aus der Liste nur im freien Modus (§12). Eine Zeile `von`/`bis` in `Seite<Nachricht>`
    — wie `fenster` in der Antwort des Baums (E‑50) — machte ihn im Standardfall möglich. Ein
    Backend-Bau außerhalb dieses Auftrags.
15. **Der Präfix-Rückfall bei gemischter Suche** (§11.6, aus Punkt 3 in §9): Der Knopf erscheint,
    wirkt auf die Belegnummern und lässt die Feldbegriffe exakt. Ob das die richtige Antwort ist,
    ist weiterhin nicht entschieden; die Oberfläche sagt nicht, worauf der Modus wirkt.
16. **Kein Hinweis auf die Deckung eines Feldes** — `Message.DestinationFilename` (14,5 %) und
    `Message.ReceiverID` (12,8 %) werden kommentarlos angeboten. Entscheidung des Auftraggebers;
    ein Nutzer, der dort nichts findet, erfährt nicht, dass die meisten Nachrichten den Namen gar
    nicht tragen.
17. **Die drei Felder, die über ein Jahr abbrechen, sind im Angebot nicht gekennzeichnet**
    (`Status`, `SOSID`, `SOSName`). Der Abbruchtext rät zum Zeitraum, aber erst nach zehn Sekunden;
    Punkt 10 in §9 bleibt für diese drei offen.
18. **Die Auswahl im Suchfeld hat ein Menü mit bis zu 54 Einträgen** (40 Belegarten und 14 Felder
    bei `NEXANS`), das in Gruppen scrollt. Ob ein Nutzer ein Feld darin findet, ohne die Gruppe zu
    kennen, zeigt erst die Nutzung; ein Eingrenzungsfeld im Menü ist nicht gebaut.
19. **Das schmale Fenster ist bei 360 px gemessen** (§14, Punkt 11); offen bleibt allein die
    Berührungsfläche am Finger, die die Emulation nicht setzt — Tabelle *Offene Sichtprüfungen* in
    [`README.md`](README.md).
20. **Die Abschneidemeldung rät auch bei reiner Feldsuche zu einer „zweiten Belegnummer"** (§14,
    Beobachtung 1). Der Text stammt aus Teil 3 und war nicht Gegenstand dieses Auftrags; ob er für
    Feldsuchen einen eigenen Satz bekommt, ist offen.
