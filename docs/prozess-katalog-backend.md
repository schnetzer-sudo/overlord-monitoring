# Prozess-Katalog — Backend

Stand: 26.08.2026 · Schritt 9b, Teil Backend
Fachliche Grundlage: [`prozess-katalog.md`](prozess-katalog.md) (E1–E24)
Messgrundlage: [`messungen-schritt9.md`](messungen-schritt9.md), M74 bis M79 · Nachträge **M80**,
**M83**/**M84** (Bestandsabfrage zu E14) und **M93** (Vorschlagsübernahme zu E22)

**Kein Frontend.** Dieser Schritt liefert die Migration, die Heuristik, den Datenzugriff und fünf
Endpunkte. Die Oberfläche ist ein eigener Auftrag.

> **Nachtrag vom 26.08.2026 — der sechste Endpunkt.** Die **Vorschlagsübernahme** (E22–E24) kommt
> dazu: `POST /api/katalog/vorschlaege-uebernehmen` setzt alle offenen Zeilen mit einem
> Partnervorschlag auf `GEPFLEGT`. **Ohne Migration** — keine Spalte, kein neuer Pflegestatus,
> `Massenmodus` wiederverwendet. Der Datenzugriff steht in **§3.6**, der Endpunkt in **§4**, die
> Messung als **M93** in §8.2. Der Satz „fünf Endpunkte" oben bleibt stehen; er beschreibt den Stand
> vom 21.08.2026 richtig.

---

## 1. Was entstanden ist

| | |
|---|---|
| Migration | `V6__process_catalog.sql`, dazu `V8__bestandsspalte.sql` (21.08.2026, E14) |
| Heuristik | `catalog/Partnerheuristik` — reine Funktionen, ohne Spring, ohne Datenbank |
| Datenzugriff | `catalog/ProzessKatalogRepository` — zwei `DSLContext` |
| Fachlogik | `catalog/ProzessKatalogService` |
| Endpunkte | `catalog/ProzessKatalogController` — fünf, alle unter `/api/katalog`; **seit 26.08.2026 sechs** (§4) |
| Rollengrenze | eine Zeile in `config/SecurityConfig` |

Dazu die Aufzählungstypen `Pflegestatus`, `Richtung`, `VorschlagHerkunft`, `Zuordnungsfeld`,
`Massenmodus` und die Antwortsätze `KatalogzeileResponse`, `MassenzuordnungResponse`,
`VorschlagslaufResponse` — **seit 26.08.2026 dazu `VorschlagsuebernahmeResponse`** und die
Ereignisart `AuditEventType.KATALOG_VORSCHLAEGE_UEBERNOMMEN`. **Kein neuer Aufzählungstyp:**
`Massenmodus` wird wiederverwendet.

**Was ausdrücklich nicht entstanden ist:** keine Tabelle `partner` (E2), keine Felder Standort und
Belegart (E1), keine Auswertung „seit wann kam nichts" (§8 der Festlegung — das ist Schritt 10 aus
`message_rollup`), keine Ableitung der Richtung aus dem `SOSName` (§9), kein dritter Pflegestatus
(§9).

> **Berichtigt 21.08.2026.** Hier stand zusätzlich *„keine gespeicherte Spalte ‚trägt Nachrichten'
> (§9)"*. Das gilt nicht mehr: **E14** kehrt die Verwerfung um, `V8__bestandsspalte.sql` legt
> `traegt_nachrichten` und `bestand_geprueft_am` an, und der Bestandslauf füllt sie (§3.5). Der alte
> Wortlaut ist in [`messungen-schritt9.md`](messungen-schritt9.md) unter „Die zwei Sätze, die E14
> umkehrt" festgehalten.
>
> **Was weiter nicht entsteht, ist die Auswertung daneben:** „seit wann kam von diesem Partner
> nichts" bleibt Schritt 10 und bleibt Sache von `message_rollup`. E14 beantwortet **ob**, nicht
> **seit wann**.

---

## 2. Die Tabelle

`overlord_monitor.process_catalog`, eine Zeile je `ProcessID`, Primärschlüssel `process_id` allein.

| Spalte | Typ | |
|---|---|---|
| `process_id` | `varchar(36)` | Primärschlüssel |
| `partner` | `varchar(100)` | NULL erlaubt — **ein leerer Partner ist ein gültiger gepflegter Zustand** (E4) |
| `richtung` | `varchar(20)` | `EINGEHEND` / `AUSGEHEND` / NULL |
| `pflegestatus` | `varchar(20)` | `OFFEN` / `GEPFLEGT`, NOT NULL |
| `vorschlag_herkunft` | `varchar(20)` | `REGEL_A` / `REGEL_B` / `KEINE`, NOT NULL |
| `geaendert_am` | `DATETIME(3)` | UTC, NOT NULL |
| `geaendert_von` | `varchar(100)` | Benutzername, NOT NULL |
| `traegt_nachrichten` | `BOOLEAN` | **drei Zustände:** NULL = noch nie geprüft, `false` = geprüft und tot, `true` = geprüft und lebend (E14, `V8`) |
| `bestand_geprueft_am` | `DATETIME(3)` | UTC, NULL erlaubt — der Zeitpunkt des letzten Bestandslaufs über diese Zeile (E14, `V8`) |

Zeichensatz und Sortierung stehen explizit (`utf8mb4` / `utf8mb4_general_ci`), kein
`utf8mb4_bin` — hier steht nichts Tokenartiges. Kein Fremdschlüssel über die Schemagrenze; verwaiste
Einträge sind erwünscht. Keine Sekundärindizes und keine Vorbelegung. Begründungen stehen in der
Migration selbst und in [`datenzugriff.md`](datenzugriff.md) §5.

**`geaendert_am` ist `DATETIME(3)` und nicht `timestamp`** — siehe §9, Abweichung 1.
`bestand_geprueft_am` folgt derselben Wahl.

**Die beiden letzten Spalten sind nicht kuratiert, sondern beobachtet** (E14). Sie tragen deshalb
**keinen** Standardwert und **keinen** Index: `NULL` muss von `false` unterscheidbar bleiben (E20),
und gefiltert wird im Browser und nicht in der Datenbank. Der Bestandslauf steht in §3.5.

**`vorschlag_herkunft` beschreibt den Partner, nicht die Richtung.** Eine Zeile darf `KEINE` tragen
und trotzdem eine Richtung haben; das ist bei 224 `NEXANS`-Prozessen der Regelfall. Die Zählweise
folgt damit §3.5 der Festlegung, wo Regel A 887, Regel B 281 und „ohne Vorschlag" 322 Prozesse
zählt — allesamt Partnerzahlen.

---

## 3. Die Heuristik

`Partnerheuristik` ist eine Klasse ohne Zustand, ohne Spring-Annotation und ohne Datenbankzugriff.
Eingabe sind drei Zeichenketten (`mandantId`, `processId`, `projectId`), Ausgabe ist ein
`Partnervorschlag` — notfalls `Partnervorschlag.KEINER`.

Der Grund für diesen Schnitt steht in §10 der Festlegung: Die Regeln stehen unter offenen Punkten
und werden sich ändern. Eine Regeländerung darf keinen Service anfassen und keine Verbindung
brauchen.

### 3.1 Regel A — Präfix und Position

Führender `^[0-9]+_`-Präfix, dann ist **Token 2 der `ProcessID`** der Partnerkandidat.
Sie feuert bei **genau zwei oder drei** Unterstrichen, gezählt über die volle `ProcessID`
einschließlich des Präfix-Trenners. `SUTTONS` ist namentlich ausgenommen.

**Zwei Lesarten mussten entschieden werden, und beide sind an den Zahlen entschieden statt geraten.**

**(a) Token 2 wovon?** „Führenden Präfix abschneiden, Token 2 ist der Partnerkandidat" lässt sich
als Token 2 des *Rests* lesen. Die abgedruckte Gestalt entscheidet: `40000_AMG_LAB_VDA` hat den
Partner `AMG`. Token 2 des Rests wäre `LAB`. Gemeint ist Token 2 der **vollen** `ProcessID`, und das
Abschneiden ist die Bedingung, nicht der Schritt davor.

**(b) Ist der Nummernpräfix Bedingung oder nur Namensgeber?** Der Auftrag nennt als
Feuerbedingung nur die Unterstrichzahl. Dann aber träfe die Regel `NXHBE` (14 von 17 Prozessen mit
genau zwei Unterstrichen) und drei der vier `WOC`-Prozesse — beide führt §3.5 vollständig unter
„ohne Vorschlag", und beide haben laut M75 **keinen** Präfix. Mit dem Präfix als Bedingung geht die
Rechnung dagegen genau auf:

| Mandant | Unterstriche 2 | Unterstriche 3 | Regel A | §3.5 |
|---|---:|---:|---:|---:|
| `NEXANS` | 9 | 500 | **509** | 509 |
| `VOTG` | 118 | 260 | **378** | 378 |
| | | | **887** | **887** |

Und die Gegenprobe über die 322 ohne Vorschlag: `NEXANS` 6 (ein Unterstrich) + 168 (vier) + 50
(fünf) = **224**, `VOTG` 4 + 8 = **12** — beide Zahlen stehen so in §3.5. Damit ist die Lesart
belegt und nicht gewählt.

### 3.2 Regel B — CamelCase mit Richtungsanker

An `Eingehend` oder `Ausgehend` geteilt: was dahinter steht, ist der Partner, das Ankerwort selbst
ist die Richtung. Fehlt der Anker, gibt es **keinen** Vorschlag.

Der Anker gilt als **Wort**, nicht als Teilzeichenkette. Die Vorgabe sagt „an Großbuchstaben
zerlegen" — und genau das steht im Code: Ein Großbuchstabe *ist* in CamelCase ein Wortanfang, davor
braucht es keine weitere Bedingung. Gebraucht wird nur die Grenze **dahinter**: Kein Kleinbuchstabe
darf folgen, sonst wäre `LagerEingehendeMeldung` ein Treffer, obwohl das Folgezeichen noch zum
selben Wort gehört und alles „dahinter" geraten wäre. Die Prüfung ist fallunterscheidend; eine
durchgängig großgeschriebene Schreibweise ist ein anderer Fall.

> **Das ist die Fassung nach der Messung, nicht vor ihr.** Die erste Fassung verlangte zusätzlich
> einen Kleinbuchstaben *vor* dem Anker. Das war eine Erfindung und kostete **26** Prozesse —
> `IBIS` 20 und `IBISGUS` 6 tragen unmittelbar davor ein zweibuchstabiges Kürzel in Großschreibung.
> Gefunden hat es der Regressionstest gegen den Bestand (§7), und die Diagnose war eindeutig: Von
> den verfehlten Namen trugen **alle** den Anker in exakter CamelCase-Schreibweise, **keiner** in
> einer anderen. Es war die Wortgrenze und nichts sonst. Vollständig in
> [`messungen-schritt9.md`](messungen-schritt9.md) M80‑6.

**Auch hier eine entschiedene Lesart:** „Für Prozessnamen ohne Trennzeichen" ist als *Zweck*
gelesen und nicht als Bedingung — der Anker entscheidet. Im Bestand ist die Unterscheidung
**folgenlos**: Die sechs Prozesse mit Trennzeichen bei `IBIS`/`IBISGUS` tragen ohnehin keinen Anker.
Die Lesart bleibt trotzdem die des Dokuments, weil eine Bedingung, die nichts ausschließt, keine
Bedingung ist.

Ein führender Trenner am Partnerkandidaten wird entfernt: Ein Partner `_BAYER` wäre eine Behauptung
über Zeichensetzung, keine über den Namen. Steht hinter dem Anker gar nichts, gibt es eine Richtung
und **keinen** Partner — die Herkunft ist dann `KEINE`.

### 3.3 Die Richtung aus dem Projektnamen

Derselbe Anker, auf die Projektkennung angewandt (`300_KundenEingehend`,
`OrdersVerarbeitungEingehendNL`). Er wirkt **zusätzlich** zu beiden Regeln: Ein Prozess, für den
Regel A den Partner liefert, bekommt die Richtung von dort; ein Prozess ohne jeden Partnervorschlag
kann trotzdem eine Richtung tragen.

Die Prozesskennung schlägt die Projektkennung: Wo Regel B eine Richtung liefert, gilt sie — der
nähere Name gewinnt.

**Der `SOSName` wird nicht angefasst.**

### 3.4 Was die Heuristik nicht tut

Sie schlägt vor und entscheidet nicht. Jede geschriebene Zeile trägt `OFFEN`. Wo eine Regel nicht
sicher trifft, liefert sie **nichts** — nicht einen wahrscheinlichen Wert. `KEINE` heißt „geprüft,
nichts abgeleitet" und nicht „noch nicht gelaufen". Der Typ erzwingt das: Ein `Partnervorschlag` mit
Partner und Herkunft `KEINE` lässt sich gar nicht bauen.

### 3.5 Der Bestandslauf *(21.08.2026, E14–E16)*

**Ein Knopf, drei Schritte.** `POST /api/katalog/vorschlagen` fährt seit dem 21.08.2026 nicht mehr
nur die Heuristik, sondern erhebt anschließend den Bestand.

| | Was er tut | Wen er anfasst |
|---|---|---|
| **1** | fehlende Katalogzeilen anlegen | Prozesse ohne Zeile |
| **2** | Partner und Richtung vorschlagen | **nur `OFFEN`** — `GEPFLEGT` bleibt unberührt (E13) |
| **3** | Bestand erheben | **alle** Zeilen des Mandanten, **auch `GEPFLEGT`** (E15) |

**Schritt 2 und Schritt 3 sind verschieden vorsichtig, und das ist keine Nachlässigkeit.** E13
schützt **Kuratierung**, nicht **Beobachtung**: Partner, Richtung, Pflegestatus und Herkunft hat ein
Mensch entschieden; `traegt_nachrichten` sagt, was die Datenbank sagt. Eine Beobachtung, die für
gepflegte Zeilen stehenbliebe, wäre nach dem ersten Lauf falsch — und gerade dort ist sie wertvoll:
„kuratiert **und** ohne Verkehr" ist die Aussage, auf die Schritt 10 aufsetzt.

**Im Code sind es deshalb zwei getrennte Methoden mit sprechenden Namen** —
`speichereVorschlaege` und `speichereBestandsflags` — und kein Schalter an einer. Als Parameter wäre
E15 beim Lesen nicht zu erkennen und würde beim nächsten Anfassen „repariert".

**Schritt 3 läuft *nach* Schritt 1 und 2.** Danach trägt jeder Prozess des Mandanten eine
Katalogzeile, und das `UPDATE` findet sie alle. Andersherum gingen die eben angelegten Zeilen leer
aus und stünden bis zum nächsten Knopfdruck auf „noch nie geprüft".

#### Das Statement — Fassung A aus M83‑1

```sql
SELECT p.ProcessID,
       EXISTS (SELECT 1 FROM GlassfishDB.Message m WHERE m.ProcessID = p.ProcessID) AS traegt
FROM GlassfishDB.ProjectMandant pm
JOIN GlassfishDB.Process     p  ON p.ProjectID = pm.ProjectID
WHERE pm.MandantID = ?;
```

**Die Form ist gemessen und nicht frei gewählt.** Die naheliegende Alternative — `SELECT DISTINCT
m.ProcessID FROM Message …` und das Komplement im Dienst bilden — kostet **4.797 ms** statt 23 und
damit **Faktor 216,5** (M83‑2). Der Grund ist keine Feinheit des Plans, sondern die Bezugsgröße:
**Fassung A zahlt je Prozess, Fassung B je Nachricht.** Die Prozesszahl steht still, die
Nachrichtenzahl wächst — Fassung A wird nicht teurer, Fassung B schon.

Sie liefert `true` **und** `false`, nicht nur die lebenden Prozesse: Der Lauf schreibt beides, und
eine Fassung, die nur die lebenden nennt, verlöre den Unterschied zwischen „geprüft und tot" und
„nie geprüft".

Gelesen wird über den **Lese-Pool** (`glassfishDsl`), geschrieben über den **Schreib-Pool** — wie
überall in dieser Klasse. Kein `STRAIGHT_JOIN`, auch nicht als Reparatur (M42).

#### Die drei Zustände der Spalte

| Wert | Bedeutung |
|---|---|
| `NULL` | **noch nie geprüft** — für diese Zeile hat nie ein Bestandslauf stattgefunden |
| `false` | geprüft, es hängt **keine** Nachricht daran |
| `true` | geprüft, es hängen Nachrichten daran |

`NULL` wird **nicht** auf `false` abgebildet, anders als bei `pflegestatus` (→ `OFFEN`) und
`vorschlag_herkunft` (→ `KEINE`). Dort gibt es einen sinnvollen Ersatzwert, hier nicht: Der Filter
aus E20 muss ungeprüfte Zeilen **zeigen**, sonst verschwindet eine nie gemessene Zeile aus **beiden**
Filterstellungen. `KatalogzeileResponse.traegtNachrichten` ist deshalb `Boolean` und nicht `boolean`.

#### Das Schreiben ist ein `UPDATE` und bewusst **kein** Upsert

Es ist der einzige Schreibweg des Katalogs, der das ist — und der einzige, der auf gepflegte Zeilen
geht. Ein `INSERT … ON DUPLICATE KEY UPDATE` müsste die `NOT NULL`-Spalten mitliefern
(`pflegestatus`, `vorschlag_herkunft`, `geaendert_am`, `geaendert_von`) und überschriebe damit genau
die Kuratierung, die E15 unangetastet lässt. Das `UPDATE` fasst **ausschließlich die zwei
Beobachtungsspalten** an.

**`geaendert_am` und `geaendert_von` bleiben stehen.** Der Bestandslauf ist keine Änderung an der
Zeile im Sinne der Kuratierung; er trägt seinen eigenen Zeitstempel in `bestand_geprueft_am`. Zöge
er `geaendert_am` mit, sähe nach jedem Knopfdruck jede Zeile des Mandanten frisch bearbeitet aus.

**Die Uhr ist `systemClock`**, wie bei `geaendert_am` und beim `audit_log` (Regel A5) — **nicht** die
Anwendungsuhr. Sie ist im Profil `dev` um den Rückstand der Testkopie zurückversetzt; ein
Prüfzeitpunkt Wochen in der Vergangenheit wäre keiner, und zwei Zeitstempel derselben Zeile lägen
Wochen auseinander. Regel Z1 verbietet den direkten `now()`-Aufruf und schreibt **keine** der beiden
Uhren vor — sie verlangt für Protokollzeit ausdrücklich `systemClock`.

#### Die Antwort nennt zwei Zahlen

`VorschlagslaufResponse` trägt zusätzlich `bestandGeprueft` und `ohneNachrichten`. **Ohne sie ist
ein reihenweise wirkungsloser Lauf von einem erfolgreichen nicht zu unterscheiden** — dieselbe Falle
wie bei der Zahl verworfener Sitzungen in Schritt 9a. Die beiden Zahlen sind mit `angelegt`,
`aufgefrischt` und `unberuehrt` **nicht** zu verrechnen: Sie zählen einen anderen Schritt über
dieselbe Menge.

`00001_Undefined` wird dabei markiert und zählt mit (E16). Ein Filter auf `^0+_` gäbe es nicht — er
fängt vier reguläre Prozesse mit, darunter einen mit 1.602 Nachrichten (M78).

---

### 3.6 Die Vorschlagsübernahme *(26.08.2026, E22–E24)*

**Der zweite Schreibweg ohne Upsert — und der erste kuratierende.**

Sie setzt alle offenen Zeilen des Mandanten mit einem **Partner**vorschlag aus Regel A oder Regel B
auf `GEPFLEGT`. **Es wird kein einziger Feldwert kopiert:** Partner und Richtung stehen bereits in
der Zeile, die Heuristik hat sie beim Lauf geschrieben (§3.4). Übernehmen ist eine Statusänderung
plus Änderungsvermerk.

#### Die Lesung — `findeUebernehmbareVorschlaege`

```
GlassfishDB.ProjectMandant
  JOIN GlassfishDB.Process              ON Process.ProjectID = ProjectMandant.ProjectID
  JOIN overlord_monitor.process_catalog ON process_catalog.process_id = Process.ProcessID
WHERE ProjectMandant.MandantID = ?
  AND process_catalog.pflegestatus = 'OFFEN'
  AND process_catalog.vorschlag_herkunft IN ('REGEL_A','REGEL_B')
ORDER BY Process.ProcessID
```

Über den **Lese-Pool** (`glassfishDsl`), wie jede lesende Methode dieser Klasse. Gelesen werden
zwei Spalten: die Kennung und die Herkunft. Mehr wird nicht gebraucht.

**`INNER JOIN` auf `process_catalog`, kein `LEFT JOIN`** — das ist der Unterschied zur Pflegeliste
(§4), und er ist die Aussage. Die Pflegeliste ebnet Prozesse ohne Katalogzeile auf
`OFFEN`/`KEINE` ein, weil sie für den Nutzer genau das sind. Hier wäre dieselbe Einebnung falsch:
**Eine Zeile, die es nicht gibt, kann keinen Vorschlag tragen.**

> **Gemessen ist, dass der `LEFT JOIN` an dieser Stelle *dieselben* Zahlen liefert** (M93‑5b): Die
> `NULL`-Herkunft fällt an `IN ('REGEL_A','REGEL_B')` ohnehin heraus. **Der `INNER JOIN` ist damit
> die ehrlichere Schreibweise und nicht die engere Bedingung** — er sagt im Text, was die Bedingung
> ohnehin tut, statt es dem Leser zu überlassen.

Einstieg `ProjectMandant`, Mandantenfilter im Statement (M3), **kein Zugriff auf `Message`**, kein
`STRAIGHT_JOIN` (M42). Die Messung steht in §8.2.

#### Das Schreiben ist ein `UPDATE` und bewusst **kein** Upsert

```sql
UPDATE overlord_monitor.process_catalog
   SET pflegestatus = 'GEPFLEGT', geaendert_am = ?, geaendert_von = ?
 WHERE process_id IN (?, ?, …)
```

**Ein einziges Statement, kein `batch`.** Die Massenzuordnung und der Lauf schreiben über
`DSLContext.batch(…)`, und genau dort ist die Bindezähler-Falle zugeschnappt (§6, Falle 1). Hier
gibt es keine Vorlage mit Zeilen — es gibt eine Bedingung mit einer `IN`-Liste. Die obere Schranke
ist die Prozesszahl des größten Mandanten, also **733 Bindeplätze**; das ist unkritisch.

**Warum kein Upsert:** Die Zeile existiert **notwendigerweise**. Nur der Lauf schreibt
`REGEL_A`/`REGEL_B`, und er schreibt sie in eine vorhandene oder eben angelegte Zeile. Ein
`INSERT`-Zweig wäre **unerreichbar**, und der Upsert müsste vier Spalten zurückschreiben, die er
gar nicht ändern will: `partner`, `richtung`, `vorschlag_herkunft` und den Schlüssel.

**`geaendert_am` und `geaendert_von` werden gesetzt** — anders als beim Bestandslauf (§3.5). Das
hier **ist** eine Kuratierung: Ein Mensch hat entschieden, den Regelvorschlägen zu glauben. Die Uhr
ist `systemClock` (Regel A5), nicht die Anwendungsuhr — dieselbe Wahl wie bei `geaendert_am` seit
`V6` und beim `audit_log`.

**`vorschlag_herkunft` bleibt stehen.** Sie ist nach diesem Knopfdruck der **einzige** Hinweis in
der Zeile darauf, dass der Wert aus einer Regel und nicht aus einem Kopf stammt. Sie zu löschen
wäre das Vernichten des einzigen verbliebenen Belegs.

#### Die gemeldete Zeilenzahl geht nicht nach außen

Nach außen geht die **Größe der gelesenen Liste**, damit Vorschau und Ausführung dieselbe Zahl
nennen. Weicht die vom `UPDATE` gemeldete Zahl davon ab, schreibt der Dienst eine **`WARN`-Zeile
mit beiden Zahlen**.

**Das ist kein vorsorglicher Zusatz, sondern eine mögliche Lage:** Lesung und Schreiben laufen auf
**verschiedenen Verbindungen** — eine Lesung innerhalb einer `@Transactional`-Methode ist nicht Teil
dieser Transaktion ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6,
[`datenzugriff.md`](datenzugriff.md) §5). Eine Abweichung wäre ein Befund und kein Rauschen.

#### Keine Migration

Es entsteht keine Spalte und kein Aufzählungswert. `Massenmodus` (`VORSCHAU`/`AUSFUEHREN`) existiert
seit dem 20.08.2026 und wird **wiederverwendet**; ein zweiter Typ mit demselben Inhalt entsteht
nicht. Auch `AuditEventType.KATALOG_VORSCHLAEGE_UEBERNOMMEN` kostet keine Migration — die Whitelist
ist ein Aufzählungstyp im Code, `audit_log.event_type` ist `VARCHAR(64)`, und der Name ist 35
Zeichen lang.

---

## 4. Die Endpunkte

Pfade deutsch, Antwortfelder camelCase, Fehlertexte deutsch.

### `GET /api/katalog/prozesse`

Die Pflegeliste des aktiven Mandanten. Parameter `nurOffene` (Vorgabe `false`).

Je Zeile zehn Felder: `processId`, `projectId`, `projectName`, `processName`, `partner`,
`richtung`, `pflegestatus`, `vorschlagHerkunft`, `traegtNachrichten`, `bestandGeprueftAm`.

- `traegtNachrichten` ist **nullable** und trägt drei Zustände (E14, §3.5): `null` = noch nie
  geprüft, `false` = geprüft und ohne Verkehr, `true` = geprüft und mit. `null` wird **nicht** auf
  `false` abgebildet.
- `bestandGeprueftAm` ist UTC und `null`, solange kein Bestandslauf über die Zeile ging. Die
  Oberfläche leitet daraus das Alter der Erhebung für die ganze Liste ab; **ein zusätzliches Feld
  im Umschlag gibt es dafür bewusst nicht.**

- **Alle** Prozesse des Mandanten, auch die ohne Nachrichten (E5) und auch die ohne Katalogzeile —
  `process_catalog` hängt als `LEFT JOIN` daran.
- Prozesse ohne Katalogzeile erscheinen mit `pflegestatus = OFFEN`, `vorschlagHerkunft = KEINE` und
  leeren Feldern. Die Umsetzung des `NULL` passiert im Repository, damit kein Aufrufer die Frage
  anders beantwortet.
- Sortiert nach **`ProjectID`, dann `ProcessID`** (E6 in der eindeutigen Fassung vom 20.08.2026) —
  beides Schlüssel und damit stabil. Siehe §9, Abweichung 2.
- **Keine Paginierung** (E8), kein Zeitfenster. Die Fortschrittszahl bekommt keine eigene Abfrage;
  die volle Liste kommt zurück und wird vorne gezählt.
- `nurOffene=true` blendet gepflegte Zeilen aus. Prozesse **ohne** Katalogzeile bleiben drin — sonst
  verschwänden ausgerechnet die, die noch nie jemand angesehen hat.

### `GET /api/katalog/partner`

`SELECT DISTINCT partner` über die Katalogzeilen **des aktiven Mandanten**, leere Werte ausgelassen,
alphabetisch. Antwort ist eine Liste von Zeichenketten. **Keine Tabelle `partner`** (E2).

Der Mandantenbezug entsteht im Join über `ProjectMandant` und nicht in einer Spalte (E3): `BAYER`
bei `VOTG` und `BAYER` bei `SUTTONS` sind zwei Werte.

Ein leerer Partner ist ein gültiger *Pflegezustand*, aber kein wählbarer *Wert* — deshalb bleibt er
aus der Liste.

### `PUT /api/katalog/prozesse/{processId}`

Körper: `{"partner": …, "richtung": …}`, beide Felder dürfen fehlen oder `null` sein.
Setzt beide Felder und setzt `pflegestatus = GEPFLEGT`. Antwort ist die geänderte Zeile.

- **Ein leerer Partner ist gültig** und bedeutet „hingesehen, es gibt nichts" (E4). Ein Partner aus
  Leerzeichen wird zu leer — ein gefülltes Feld ohne Inhalt wäre ein dritter Zustand durch die
  Hintertür.
- Ein Partner über 100 Zeichen ist `400` (`partner-zu-lang`), eine unbekannte Richtung ist `400`
  (`richtung-unbekannt`). Ein unbekannter Wert fällt **nicht** stillschweigend auf leer zurück.
- `processId` außerhalb des aktiven Mandanten → `404`, nicht `403`, und nicht von einer erfundenen
  Kennung zu unterscheiden.
- Eintrag ins `audit_log` (`KATALOG_GEAENDERT`).
- `vorschlag_herkunft` bleibt beim Aktualisieren stehen: Sie beschreibt, welche Regelfassung diese
  Zeile einmal vorgeschlagen hat, und wird durch eine Kuratierung nicht falsch, sondern historisch.

### `POST /api/katalog/massenzuordnung`

Körper: `{"projectId": …, "feld": "PARTNER"|"RICHTUNG", "wert": …, "modus": "VORSCHAU"|"AUSFUEHREN"}`.
Setzt **ein** Feld für alle Prozesse eines Projekts (E11). Ohne `modus` gilt `VORSCHAU` — wer den
Modus vergisst, verändert nichts.

Antwort: `modus`, `projectId`, `feld`, `wert`, `betroffen`, `davonGepflegt`.

- **Beide Modi teilen ein Statement.** `speichereFeld` ruft dieselbe Methode
  `findeProjektbestand(mandant, projectId)`, die auch die Vorschau nennt, und schreibt genau auf die
  Zeilen, die sie liefert. Ein Attrappentest vergleicht die beiden gerenderten Statements Zeichen
  für Zeichen.
- Die Vorschau nennt die betroffenen Zeilen **und** davon die bereits gepflegten — das ist die Zahl,
  die verloren geht.
- **Sie überschreibt gepflegte Zeilen** (E12). Das ist gewollt: Der Schutzmodus „nur offene Zeilen"
  machte genau die Korrektur unmöglich, für die man sie braucht.
- Das jeweils andere Feld bleibt unangetastet.
- Projekt außerhalb des aktiven Mandanten → `404`, ununterscheidbar von einem erfundenen. Die
  Existenzprüfung läuft über `ProjectMandant` und nicht über die Zeilenzahl — ein Projekt **ohne**
  Prozesse gäbe sonst dieselbe leere Antwort wie ein fremdes, und das ist der Unterschied zwischen
  „nichts zu tun" und `404`.
- Nur `AUSFUEHREN` schreibt einen `audit_log`-Eintrag.

Größtes Projekt im Bestand: 226 Prozesse (`VOTG`, `110_VTG_SalesInvoice`), 161 bei `NEXANS`.

### `POST /api/katalog/vorschlagen`

Heuristik-Lauf **und Bestandslauf** für den aktiven Mandanten (E13, E14), leerer Körper.
Antwort: `angelegt`, `aufgefrischt`, `unberuehrt`, `regelA`, `regelB`, `keine`,
`bestandGeprueft`, `ohneNachrichten`.

| | |
|---|---|
| legt an | fehlende Zeilen |
| frischt auf | Zeilen mit `OFFEN`, **auch wenn sie schon einen Vorschlag tragen** |
| rührt nie an | Zeilen mit `GEPFLEGT` — **soweit es die Kuratierung angeht** |
| erhebt den Bestand auf | **allen** Zeilen des Mandanten, auch den gepflegten (E15, §3.5) |

Die ersten drei Zahlen ergeben zusammen die Zahl der Prozesse des Mandanten. Die drei danach zählen
nur die **geschriebenen** Zeilen nach der Herkunft ihres Partnervorschlags; unberührte Zeilen tragen
ihre eigene, ältere Herkunft.

**`bestandGeprueft` und `ohneNachrichten` sind mit den übrigen nicht zu verrechnen** — sie zählen
den dritten Schritt über dieselbe Menge. `bestandGeprueft` entspricht der Zahl der Prozesse des
Mandanten; `ohneNachrichten` ist die Teilmenge ohne jede Nachricht im Bestand. Ohne die beiden wäre
ein reihenweise wirkungsloser Lauf von einem erfolgreichen nicht zu unterscheiden.

**Nicht beim Anwendungsstart.** Kein `ApplicationRunner`, kein `@PostConstruct`, kein Scheduler.
Ein `audit_log`-Eintrag hält jeden Lauf mit seinen Zahlen fest.

Für die Unterscheidung „anlegen" gegen „auffrischen" gibt es eine eigene Lesung
(`findeBestand`): Die Pflegeliste ebnet den Unterschied bewusst ein, weil ein Prozess ohne
Katalogzeile für den Nutzer genau `OFFEN` ist — der Lauf muss ihn aber auseinanderhalten.

### `POST /api/katalog/vorschlaege-uebernehmen` *(26.08.2026, E22–E24)*

**Der sechste Endpunkt.** Er erbt die Rollengrenze aus `SecurityConfig` (`/api/katalog/**` →
`ADMIN`) und braucht dafür keine Zeile.

Körper: `{"modus": "VORSCHAU"|"AUSFUEHREN"}`. Fehlt `modus`, gilt `VORSCHAU` — wer den Modus
vergisst, verändert nichts. Ein *unbekannter* Wert ist `400` (`modus-unbekannt`) und fällt nicht
stillschweigend auf die Vorgabe zurück.

**Mehr nimmt er nicht entgegen** (E23): keine Liste von `ProcessID`, kein Projekt, keinen Filter.
Die Menge berechnet der Dienst aus der Bedingung von E22 (§3.6).

Antwort `VorschlagsuebernahmeResponse`:

| Feld | |
|---|---|
| `modus` | der gefahrene Modus, zurückgespiegelt |
| `betroffen` | Zahl der Zeilen, die die Bedingung aus E22 erfüllen |
| `regelA` | davon mit `vorschlag_herkunft = REGEL_A` |
| `regelB` | davon mit `REGEL_B` |

- **Beide Modi lesen, und zwar dasselbe** (E24). Die Vorschau zählt und antwortet; die Ausführung
  schreibt **genau die Liste, die sie gezählt hat**. Die Zahl, die der Nutzer bestätigt, ist damit
  die Zahl, die passiert — und zwar nicht, weil zwei Statements gleich aussehen, sondern weil es
  **nur eine Liste gibt**. Das ist eine stärkere Zusicherung als bei der Massenzuordnung.
- **`regelA + regelB = betroffen`** ist eine Invariante und in `ProzessKatalogDbIT` geprüft. Die
  Aufschlüsselung ist kein Schmuck: Sie ist die Kontrolle, an der sich ein Lauf gegen §3.5 der
  Festlegung halten lässt.
- **Ein `davonGepflegt` gibt es hier nicht**, anders als bei der Massenzuordnung. Es wäre
  konstruktionsbedingt immer `0`, weil nur `OFFEN`-Zeilen erfasst werden — und eine Zahl, die nie
  etwas anderes sagen kann, sagt nichts.
- **Die Vorschau schreibt nichts** und hinterlässt **keinen** `audit_log`-Eintrag.
- **Eine leere Menge ist kein Fehler.** Sie setzt kein `UPDATE` ab — eine `IN`-Liste ohne Werte wäre
  entweder ein Syntaxfehler oder ein `UPDATE` ohne Bedingung — und antwortet trotzdem, mit lauter
  Nullen.
- Nur `AUSFUEHREN` schreibt ins Protokoll, und zwar unter **`KATALOG_VORSCHLAEGE_UEBERNOMMEN`** und
  ausdrücklich **nicht** unter `KATALOG_GEAENDERT`. Der Eintrag trägt die drei Zahlen. Begründung
  unter §9, Abweichung 12.

---

## 5. Mandantentrennung und Rollengrenze

Der Katalog erbt M1 bis M4 unverändert (§7 der Festlegung).

- **Kein Endpunkt nimmt eine Mandanten-ID entgegen** (M1). Auch der Heuristik-Lauf läuft für den
  aktiven Mandanten und nicht für alle.
- **`MandantContext` ist erster Pflichtparameter jeder Repository-Methode** (M2) — auch der rein
  schreibenden. ArchUnit erzwingt das klassenweit, sobald eine Klasse `jooq.glassfish` berührt, und
  hier ist das kein Formalismus: `process_catalog` trägt keine Mandantenspalte, eine Schreibmethode
  ohne Mandantennachweis änderte deshalb ohne Weiteres fremde Kuratierung.
- **Der Filter über `ProjectMandant` ist Bestandteil jedes lesenden Statements** (M3). Wer eine
  fremde `ProcessID` errät, bekommt null Zeilen. Die schreibenden Statements setzen ihn nicht noch
  einmal, sondern schreiben ausschließlich auf Zeilen, die eine mandantengefilterte Lesung ergeben
  hat.
- **Ein Isolationstest je Endpunkt** (M4), **sechs** an der Zahl. Beim sechsten steht die Gegenprobe
  auf der **Wirkung** und nicht auf der Eingabe — er nimmt keine Kennung entgegen, es gibt also
  nichts, worüber sich Existenz erfragen ließe. Dieselbe Übertragung wie bei
  `ProzesseIsolationDbIT` ([`mandantentrennung.md`](mandantentrennung.md) §5).

**Zusätzlich eine Rollengrenze.** `/api/katalog/**` verlangt `ADMIN`:

```java
.requestMatchers("/api/katalog/**")
.hasRole("ADMIN")
```

Sie steht in `SecurityConfig` und nicht als Annotation am Controller — dieselbe Entscheidung wie bei
`AdminUserController`: Die Regel soll an einer Stelle stehen. Siehe §9, Abweichung 3.

Weil ein Administrator für **alle** Mandanten berechtigt ist, hat er keinen automatisch gewählten
Mandanten und bekommt ohne ausdrückliche Wahl `403`. Das ist geprüft.

---

## 6. Zwei Fallen, die zugeschnappt sind

Beide sind von Tests gefunden worden, nicht beim Lesen.

**1. Der Bindezähler des Stapels.** Die Massenzuordnung und der Heuristik-Lauf schreiben über
`DSLContext.batch(…)`. In der Vorlage standen zwei Statuswerte als **feste** Werte
(`Pflegestatus.GEPFLEGT.name()`), und genau die sind ebenfalls Bindeplätze — die Bindeliste war um
zwei Werte zu kurz. jOOQ behandelt das **nicht** als Fehler: Es schreibt eine Zeile ins Protokoll
(`Batch bind value set 0 has 4 values when 6 values were expected`) und füllt den Rest mit `null`.
Jede Zeile wäre um Spalten verschoben geschrieben worden. Behoben ist es, indem jede Zeile der
Vorlage ein `null`-Platzhalter ist und alle Werte in derselben Reihenfolge gebunden werden;
`ProzessKatalogStatementsTest.stapelsaetze_binden_vollstaendig` zählt die `?` im gerenderten Text
gegen jeden Bindesatz und fängt einen Rückfall.

**2. `@Transactional` bindet nur den Schreib-Kontext.** Beschrieben in
[`datenzugriff.md`](datenzugriff.md) §5 beim Abschnitt zu `V6`.

---

## 7. Tests

| Datei | Art | Fälle |
|---|---|---|
| `PartnerheuristikTest` | Unit, ohne Datenbank | 22 |
| `ProzessKatalogStatementsTest` | Unit, jOOQ-Attrappe | **22** |
| `ProzessKatalogDbIT` | `@Tag("db")` | **23** |
| `ProzessKatalogIsolationDbIT` | `@Tag("db")` | **15** |
| `HeuristikBestandDbIT` | `@Tag("db")`, **schreibt nichts** | 2 |
| `BestandslaufDbIT` | `@Tag("db")`, **schreibt nichts** | **4** |

**`HeuristikBestandDbIT` ist der Test, der sich bezahlt gemacht hat, bevor er einmal grün war.** Er
lässt die Heuristik über den echten Prozessbestand jedes Mandanten laufen und vergleicht die
Trefferzahlen mit §3.5. Beim ersten Lauf war er rot — und der Befund war nicht die Zahl, sondern
eine erfundene Bedingung im eigenen Code (§3.2). Er zählt **je Mandant** und nicht nur die Summe:
Die beiden gefundenen Abweichungen unterscheiden sich in der Summe um **eins** und wären an ihr
unsichtbar geblieben. Und er berichtet alle zehn Mandanten in einem Durchgang, statt beim ersten
Unterschied abzubrechen — beim Bau war genau die vollständige Liste der Befund.

**`PartnerheuristikTest`** legt den Schwerpunkt auf die Negativfälle: vier und fünf Unterstriche,
ein Unterstrich, fehlender Nummernpräfix, `SUTTONS`, fehlender Anker, Anker am Wortende, ein Anker
ohne Wortgrenze, leere und fehlende Eingabe. Eine Heuristik, die zu viel liefert, ist schlimmer als
eine, die zu wenig liefert.

**`ProzessKatalogStatementsTest`** prüft den gerenderten Text: Mandantenfilter in jedem lesenden
Statement, beide Schemata voll qualifiziert, `LEFT JOIN` auf `process_catalog`, keine Paginierung,
kein Zugriff auf `Message`, jedes Schreiben ein Upsert, kein Schreibzugriff berührt `GlassfishDB`,
die Massenzuordnung setzt genau ein Feld — und Vorschau und Ausführung fahren denselben Text.
Weil jOOQ Werte bindet statt sie einzusetzen, prüft er die Bindewerte daneben; eine reine
Textprüfung auf `'OFFEN'` ginge still ins Leere.

**`ProzessKatalogDbIT`** fährt auf `SUTTONS` — bewusst der kleinste Bestand: 17 Prozesse, ein
Projekt. Die Fragen hängen an der Logik und nicht an der Menge, und jeder Schreibvorgang ist ein
Commit auf einer gemeinsam genutzten Datenbank. Dass die Heuristik dort **nichts** liefert, ist ein
Vorteil: Jeder Partner in diesen Tests ist von Hand gesetzt und keiner aus einer Regel.

**`ProzessKatalogIsolationDbIT`** fährt `VOTG` gegen `SUTTONS` mit zwei Administratoren, die ihren
Mandanten ausdrücklich wählen. Je Endpunkt ein Nachweis, dazu die Rollengrenze (ein
`MANDANT`-Nutzer bekommt auf allen fünf Endpunkten `403`, erreicht seine eigene Nachrichtenfläche
aber sehr wohl), der Administrator ohne Mandantenwahl, und Regel M1 in zwei Formen — als
Abfrageparameter und als untergeschobenes Feld im Anfragekörper.

**`BestandslaufDbIT`** ist das Gegenstück zu `HeuristikBestandDbIT` für E14: Er liest die
Bestandsabfrage gegen **zwei** Mandanten (Regel L7) und hält sie gegen die Gegenprobe aus M83‑5 —
`NEXANS` **733 / 516 / 217**, `SUTTONS` **17 / 17 / 0**. Er schreibt nichts.

**`SUTTONS` ist dabei die schärfere Probe, obwohl es der kleinere Bestand ist.** Dort **muss** die
Menge der toten Prozesse leer bleiben. Eine Fassung, die versehentlich Zeilen erzeugt — ein Join zu
viel, ein `LEFT` statt eines inneren —, fällt genau dort auf und bei `NEXANS` nicht, wo 217 tote
Prozesse ohnehin erwartet werden. Dazu die Doublettenprobe (`COUNT` gegen `COUNT(DISTINCT …)`, wie
M83‑5 sie gefahren hat) und die Mandantentrennung.

**Vier neue Fälle in `ProzessKatalogDbIT`** decken E14 und E15 am laufenden Endpunkt ab: `null` vor
dem ersten Lauf und auch nach einer Zuordnung (Kuratierung ist keine Erhebung); der Lauf setzt das
Flag und meldet beide Zahlen; **eine `GEPFLEGT`-Zeile bekommt das Flag, während `unberuehrt = 1`
bleibt und Partner, Richtung und Pflegestatus stehenbleiben** — E13 und E15 in einem Test; und eine
Zuordnung nach dem Lauf trägt den erhobenen Bestand weiter, weil die Antwort gebaut und nicht
nachgelesen wird.

> **Eine bestehende Zusicherung ist dabei geschärft worden, nicht aufgeweicht.**
> `schreibende_statements_sind_upserts` verlangte von **jedem** Schreiben ein `INSERT … ON DUPLICATE
> KEY UPDATE`. Der Bestandslauf ist bewusst keins (§3.5). Der Test verlangt den Upsert jetzt von den
> **drei kuratierenden** Schreibwegen und vom vierten ausdrücklich das Gegenteil — samt Nachweis,
> dass er als eigener Schreibweg überhaupt auftaucht. Eine Ausnahme, die nur durch Weglassen
> entstünde, wäre beim nächsten Umbau wieder da.

> ### Und ein zweites Mal geschärft, am 26.08.2026 — mit demselben Satz begründet
>
> **Der alte Wortlaut darüber bleibt stehen; er beschreibt den Stand vom 21.08.2026 richtig.** Was
> gilt, steht hier.
>
> Die Aufteilung des Tests lief bis zum 26.08.2026 über ein **Textmerkmal**: Ein Schreibstatement
> mit `traegt_nachrichten` darin war der Bestandslauf, alles andere war kuratierend und musste ein
> Upsert sein. **Mit der Vorschlagsübernahme fällt diese Aufteilung**, denn sie ist ein
> *kuratierender* Schreibweg **ohne** Upsert (§3.6). Über das Textmerkmal wäre sie in die falsche
> Hälfte gerutscht — und die naheliegende Reparatur wäre ein **zweites** Merkmal gewesen, mit dem
> die Ausnahme wieder nur durch Weglassen entstünde.
>
> **Geführt werden deshalb die Wege und nicht die Merkmale.** Der Test ruft jeden der fünf
> Schreibwege **einzeln** auf, rendert sein Statement und prüft es namentlich:
>
> | Schreibweg | | Warum |
> |---|---|---|
> | `speichereZuordnung` (E19) | **Upsert** | Der Nutzer kuratiert eine Zeile, die es noch nie gab |
> | `speichereFeld` (E11) | **Upsert** | Ein Projekt enthält Prozesse ohne Katalogzeile |
> | `speichereVorschlaege` (E13) | **Upsert** | Fehlende Zeilen anzulegen **ist** Schritt 1 des Laufs |
> | `speichereBestandsflags` (E15) | **kein Upsert** | Das einzige Schreiben auf **gepflegte** Zeilen. Ein `INSERT` müsste die `NOT NULL`-Spalten mitliefern und überschriebe die Kuratierung |
> | `uebernehmeVorschlaege` (E22) | **kein Upsert** | Die Zeile existiert **notwendigerweise** — ein `INSERT`-Zweig wäre unerreichbar |
>
> **Dazu ein Riegel:** Der Test zählt die Schreibwege und verlangt genau fünf. Kommt ein sechster
> dazu, wird er hier rot, bevor jemand vergisst, ihn zu benennen.

**Aufgeräumt wird über `geaendert_von`** mit dem Testpräfix `it-`. Eine von Hand kuratierte Zeile
überlebt jeden Testlauf.

> ### Eine zweite Grenze dieser Aufräumregel, und sie hat am 26.08.2026 zugeschlagen
>
> **Die Regel verlässt sich darauf, dass das Präfix nur auf Zeilen steht, die ein Test *angelegt*
> hat.** Sie unterscheidet nicht zwischen „von einem Test angelegt" und „von einem Test zuletzt
> **angefasst**".
>
> Der erste Entwurf der Übernahmetests legte seine Zeile per **Upsert** auf den *ersten* Prozess des
> Mandanten. Trägt der bereits eine Zeile, überschreibt der Upsert deren `geaendert_von` mit dem
> Präfix — und das Aufräumen löscht danach eine **fremde** Zeile. Genau das ist passiert und hat
> eine `VOTG`-Katalogzeile gekostet; vollständig in [`messungen-schritt9.md`](messungen-schritt9.md)
> unter **M93‑6**.
>
> **Behoben ist es an der Ursache**, nicht an der Aufräumregel: Beide Testklassen suchen sich
> Prozesse **ohne** Katalogzeile und legen dort mit einem reinen `INSERT` an. Eine Kollision schlägt
> laut fehl, statt still zu überschreiben. Bleibt keine freie Zeile übrig, schlägt der Test mit
> einem Satz fehl, der sagt warum.
>
> **Solange kein Schreibweg dieses Backends `geaendert_von` auf ein fremdes Präfix setzen kann, ist
> die Grenze folgenlos** — und der einzige, der es konnte, war ein Test. Als offener Punkt vermerkt
> (§10, Punkt 12).

**Sechs neue Fälle in `ProzessKatalogDbIT` und drei in `ProzessKatalogIsolationDbIT` decken E22 bis
E24 ab** *(26.08.2026)*. Sie fahren wie der Rest auf `SUTTONS`, und **dort liefert die Heuristik
nichts** — jede Vorschlagszeile in diesen Tests ist von Hand angelegt und kommt aus keiner Regel.
Das ist kein Hindernis, sondern der Grund, warum der Test dort gut ist.

Geprüft ist: Die Vorschau schreibt nicht und nennt trotzdem die Zahl; Ausführen setzt **genau den
Status** und lässt Partner, Richtung und Herkunft stehen; eine Zeile mit `KEINE` und gefüllter
Richtung bleibt offen (E22 am laufenden Endpunkt — der Fall, der die 224 `NEXANS`-Zeilen schützt);
eine bereits gepflegte Zeile taucht in `betroffen` nicht auf und behält ihren Änderungsvermerk; die
leere Menge antwortet mit Nullen und wirft nicht; und `regelA + regelB = betroffen` bei gemischtem
Bestand.

> **Ein Fall prüft die Aufräumregel selbst.** Der Endpunkt **überschreibt** `geaendert_von` mit dem
> Namen des angemeldeten Testnutzers — und der muss das Präfix `it-` tragen, sonst hinterließe der
> erste Testlauf gepflegte Zeilen auf der geteilten Testkopie. Er tut es (`it-katalog-pflege`), und
> der Test sagt das ausdrücklich statt es anzunehmen.

> **Eine Grenze dieser Aufräumregel, die E15 sichtbar macht.** Der Bestandslauf schreibt auf **alle**
> Zeilen des Mandanten — auch auf solche, die ein Mensch angelegt hat und die das Präfix nicht
> tragen. Ein Testlauf hinterlässt dort also einen aufgefrischten `traegt_nachrichten`-Wert und
> einen neuen `bestand_geprueft_am`. **Das ist unschädlich und ausdrücklich in Kauf genommen:** Der
> Wert ist eine Beobachtung und keine Kuratierung, er ist reproduzierbar, und der nächste Lauf
> schriebe ihn ohnehin. Die kuratierten Felder bleiben unberührt — genau das prüft der E15-Fall.

---

## 8. Messung

Vollständig als Nachtrag **M80** in [`messungen-schritt9.md`](messungen-schritt9.md), gefahren am
20.08.2026 gegen die Testkopie mit **gefülltem** Katalog (1.490 Zeilen über den echten Codepfad;
nach der Messung wieder gelöscht). Das Kurzergebnis:

| Statement | Mandant | Laufzeit (beste von fünf) |
|---|---|---:|
| **Pflegeliste** | `NEXANS` (733 Zeilen) | **8,000 ms** |
| **Pflegeliste** | `SUTTONS` (17 Zeilen) | **0,812 ms** |
| dieselbe ohne Katalog-Join | `NEXANS` | 4,764 ms |
| Pflegeliste `nurOffene` | `NEXANS` | 8,006 ms |
| Partnerliste | `NEXANS` / `SUTTONS` | 4,084 / 0,681 ms |
| Projektbestand (Massenzuordnung, 226 Prozesse) | `VOTG` | 1,688 ms |

**`process_catalog` hängt als `eq_ref` auf `PRIMARY` am Plan** — der bestmögliche Zugriff, bei
beiden Mandanten identisch, Einstiegstabelle unverändert `ProjectMandant`. Regel L15 hat nicht
zugeschlagen, kein `STRAIGHT_JOIN`.

**Der Join kostet +3,24 ms, also 68 %.** Das ist harmlos, weil die Ausgangszahl klein ist — aber es
ist mehr, als „ein `eq_ref` kostet nichts" vermuten lässt, und deshalb steht die Zahl hier und nicht
nur im Messdokument. Keine Paginierung nötig, E8 bleibt.

**Der Vorbehalt an genau diese Zahl gehört danebengestellt:** Die **+68 % sind keine
Produktionszahl** — ein Verhältnis zweier kleiner Zahlen, beste von fünf, auf der Testkopie ohne
Nebenlast. Am **Umfang** liegt das nicht: Der Katalog stand bei der Messung mit **1.490 Zeilen** auf
voller Größe, eine je erreichbarem Prozess. Nachgemessen wird nicht; wird die Aussage später
gebraucht, wird sie gegen die Produktion neu erhoben.

**Eine Stelle ist seit der Messung nicht mehr deckungsgleich:** Gemessen ist `ORDER BY ProjectName,
ProcessName`, ausgeliefert wird `ORDER BY ProjectID, ProcessID` (§9, Abweichung 2). Derselbe
Sortierschritt über dieselbe Zeilenmenge — aber nicht nachgemessen.

**Zwei Zahlen sind ausdrücklich *nicht* gemessen:** was der Filter `nurOffene` im Betrieb spart (im
Messzustand stand alles auf `OFFEN`, er sparte nichts — gemessen ist seine obere Schranke), und der
Schreibweg, der über den Schreib-Pool läuft und nicht `EXPLAIN`-bar ist. Belegt ist dort nur, dass
1.490 Zeilen über zehn Transaktionen in unter zehn Sekunden entstehen.

### 8.1 Der Bestandslauf — **M83** und **M84** *(21./24.08.2026)*

Zwei Runden, und die zweite ist keine Wiederholung:

| | Was sie misst | Wann |
|---|---|---|
| **M83** | die **beauftragten** Fassungen A und B, von Hand getippt — die Entscheidungsgrundlage für die Bauform | 21.08.2026, **vor** dem Bau |
| **M84** | den **gerenderten** Text aus `findeBestandsflags` — der Nachweis am gebauten Code | 24.08.2026, **nach** dem Bau |

**M83 hat die zweite Runde selbst verlangt:** *„Der gemessene Text ist nicht gerendert … Sobald er
existiert, verlangt L7 eine neue Messung des gerenderten Textes — jOOQ qualifiziert mit
`GlassfishDB.` und rendert `EXISTS` anders, als man es von Hand tippt."* **Regel L7 ist deshalb
nicht schon mit M83 erfüllt gewesen, sondern erst mit M84.**

| Statement | Mandant | erster Lauf | beste von fünf |
|---|---|---:|---:|
| **Bestandsabfrage, gerendert** (M84) | `NEXANS` (733) | 24,510 ms | **23,153 ms** |
| **Bestandsabfrage, gerendert** (M84) | `SUTTONS` (17) | 1,320 ms | **1,170 ms** |
| dieselbe, von Hand getippt (M83) | `NEXANS` | 32,144 ms | 22,154 ms |
| dieselbe, von Hand getippt (M83) | `SUTTONS` | 2,536 ms | 1,109 ms |
| verworfene Fassung B (M83) | `NEXANS` | 4.840,413 ms | 4.797,038 ms |

**Der Plan ist Zeile für Zeile derselbe wie in M83‑1** — Einstieg `ProjectMandant`, `ref` über
`ProjectMandant_Mandant_idx`, `Process` über `Process_ProjectFK`, die Unterabfrage als `DEPENDENT
SUBQUERY` mit `ref` auf `ProejctIDIDX`, **dreimal `Using index`**, `key_len` 146/147. Der Optimierer
sieht dieselbe Abfrage; die Unterschiede im Text (volle Qualifizierung statt Aliase, Backticks,
``select 1 as `one` ``, kein Spaltenalias) sind **nicht planbestimmend**.

**Der gerenderte Text ist rund 5 % teurer — und diese 5 % sind nicht gedeutet.** Sie liegen in der
Spanne, in der auf dieser Instanz auch fünf Läufe desselben Statements streuen (hier 23,153 bis
24,510 ms, also 5,9 %). Für die Entscheidung ist es gleichgültig: **0,23 % der Laufzeitgrenze des
Lese-Pools** (`max_statement_time = 10`).

**Fassung B ist um Faktor 216,5 teurer, und der Grund ist die Bezugsgröße** — A zahlt je Prozess,
B je Nachricht. Die Prozesszahl steht still, die Nachrichtenzahl wächst.

**Nicht gemessen ist auch hier das Schreiben:** das `UPDATE` über alle Zeilen des Mandanten läuft
über den Schreib-Pool und ist nicht `EXPLAIN`-bar. Es gilt dieselbe Schranke wie oben.

**Und für beide Runden gilt der Vorbehalt vom 21.08.2026**
([`annahmen-korrekturen.md`](annahmen-korrekturen.md)): Laufzeiten von der Testkopie sind für die
Produktion eine **optimistische** Schranke — der Puffer fasst `Message` neunfach, die Trefferquote
steht bei 99,975 %, und außer uns belastet die Instanz niemand.

### 8.2 Die Vorschlagsübernahme — **M93** *(26.08.2026)*

Vollständig als Nachtrag **M93** in [`messungen-schritt9.md`](messungen-schritt9.md), gefahren
gegen die Testkopie mit dem **gerenderten** Text aus `findeUebernehmbareVorschlaege`. Reine
Lesesitzung, kein `INSERT`, kein `UPDATE`.

| Mandant | Prozesse | gelieferte Zeilen | erster Lauf | beste von fünf |
|---|---:|---:|---:|---:|
| `NEXANS` | 733 | **0** | 3,562 ms | **3,462 ms** |
| `VOTG` | 390 | **377** | 3,037 ms | **2,896 ms** |
| `IBIS` | 192 | **169** | 1,831 ms | **1,831 ms** |
| `IBISGUS` | 89 | **0** | 0,933 ms | **0,889 ms** |

**Der Plan ist bei allen vier Mandanten Zeile für Zeile derselbe:** Einstieg `ProjectMandant` als
`ref` über `ProjectMandant_Mandant_idx`, `Process` über `Process_ProjectFK` mit `Using index`, und
**`process_catalog` als `eq_ref` auf `PRIMARY`** — so hängt es seit M80 an jedem Plan dieser Klasse.
`rows` in der Einstiegszeile ist die Projektzahl je Mandant (17/19/39/46), sonst gibt es keinen
Unterschied. **Kein `STRAIGHT_JOIN`, keine Fassung, die eine Tabelle voll durchläuft.** Die
L15-Falle hat nicht zugeschlagen.

**Die teuerste Zahl der Runde ist 3,562 ms** — 0,036 % der Laufzeitgrenze des Lese-Pools. Zum
Vergleich: die Pflegeliste 8,0 ms (M80‑1), die Bestandsabfrage 23,2 ms (M84).

**Die Laufzeit hängt an der Prozesszahl und nicht an der Trefferzahl.** `NEXANS` liefert null
Zeilen und ist trotzdem das teuerste der vier. Das ist die Bauform und keine Überraschung: Der Plan
läuft über alle Prozesse des Mandanten und wirft an `process_catalog` weg, was die Bedingung nicht
erfüllt. **Die Bezugsgröße ist die Prozesszahl, und die steht still** — dasselbe Argument, mit dem
M83‑2 Fassung A entschieden hat.

**Ein Befund, der in keiner vorformulierten Zeile stand:** Die Einstiegszeile trägt
`Using temporary; Using filesort`. Das `Using temporary` ist **neu** gegenüber M80 und kommt aus
der Sortierung nach `Process.ProcessID` bei Einstieg über `ProjectMandant`. Gemessen ist, dass es
folgenlos ist — die zu sortierende Menge ist höchstens 733 Zeilen mit zwei kurzen Zeichenketten.
**Beziffert ist es nicht**, und eine Fassung ohne `ORDER BY` stünde ohnehin nicht zur Wahl: Ohne
feste Reihenfolge wäre die `IN`-Liste des `UPDATE` bei jedem Aufruf anders zusammengesetzt.

> ### Die vorregistrierten Erwartungen sind **nicht** eingetreten — und das ist der Befund
>
> Der Bauauftrag registriert vorab: `NEXANS` `betroffen = 509 / regelA = 509 / regelB = 0` und
> `IBISGUS` `regelB = 88 / regelA = 0`. **Gemessen ist bei beiden `betroffen = 0`.**
>
> | Mandant | warum |
> |---|---|
> | `NEXANS` | steht auf **733 gepflegten** Zeilen und **null offenen**. Die 509 aus Regel A sind nicht verschwunden — sie stehen unverändert in `vorschlag_herkunft`, aber die Zeilen sind schon entschieden. **Ein Lauf hilft dagegen nicht:** E13 rührt gepflegte Zeilen nie an |
> | `IBISGUS` | hat **überhaupt keine** Katalogzeile. Hier *würde* ein Lauf helfen — er legte 89 offene Zeilen an, davon 88 mit `REGEL_B`. Er ist bewusst nicht gefahren worden: Die Runde ist als reine Lesesitzung angelegt, und die Entscheidung gehört dem Auftraggeber |
>
> **Was das über §3.5 sagt: nichts.** Jene Zahlen sind Aussagen über die **Heuristik** — wie viele
> Prozesse einen Vorschlag *bekommen können*. M93 misst, wie viele Zeilen zu einem Zeitpunkt
> *offen* sind und einen tragen. Das sind zwei Fragen, und die zweite hängt daran, was ein Mensch
> inzwischen getan hat.
>
> **Belegt ist die Bedingung trotzdem, an zwei anderen Mandanten:** `VOTG` liefert **377 aus Regel
> A** gegen die 378 aus §3.5 (die Differenz ist die eine Zeile aus M93‑6), und bei `IBIS` fallen
> **zwei bereits gepflegte `REGEL_B`-Zeilen korrekt heraus** — 169 von 171. Das ist die schärfste
> Einzelkontrolle der Runde.

**Nicht gemessen ist auch hier das Schreiben:** Das `UPDATE` mit der `IN`-Liste läuft über den
Schreib-Pool und ist nicht `EXPLAIN`-bar. Es gilt dieselbe Schranke wie oben.

---

## 9. Abweichungen von der Festlegung und vom Auftrag

Jede Stelle, an der der Bau von `prozess-katalog.md` oder vom Auftragstext abweicht, mit Grund.

**1. `geaendert_am` ist `DATETIME(3)`, nicht `timestamp`.** Der Auftrag nennt in der Spaltentabelle
`timestamp`. Der Hausstil verbietet `TIMESTAMP` ausdrücklich — wegen Zeitzonen und der 2038-Grenze
steht in `V1` und `V2` `DATETIME(3)` in UTC, mit Begründung im Migrationstext. Eine `TIMESTAMP`-
Spalte wäre die erste des Schemas gewesen.

**2. ~~Sortiert wird nach `ProjectName`, nicht nach `ProjectID`.~~ — zurückgenommen am
20.08.2026.** Der Auftrag schreibt „sortiert nach Projekt, dann Prozesskennung" und beruft sich
dabei auf E6. E6 sagte „Projekt **und Name**", und M79 hatte die Stelle bereits benannt: *„Die im
Auftrag abgedruckte Abfrage sortiert dagegen nach `p.ProjectID, p.ProcessID` — das ist **nicht**
die beschlossene Sortierung. Gemessen ist die beschlossene."* Gebaut war daraufhin
`ORDER BY ProjectName, ProcessName`.

**Zurückgenommen ist die Abweichung nicht, weil ihre Auflösung falsch war, sondern weil E6
mehrdeutig formuliert war.** „Projekt und Name" lässt beide Lesarten zu. Am 20.08.2026 ist E6
eindeutig gemacht worden — `ProjectID`, dann `ProcessID` —, und damit fällt die Abweichung weg:
Beides sind Schlüssel und damit stabil, während `ProcessName` über den Bestand nicht eindeutig ist
und eine Liste ohne Paginierung nach ihm keine feste Reihenfolge hätte. Gebaut ist seither
`ORDER BY Process.ProjectID, Process.ProcessID` — die Projektkennung aus `Process` und nicht aus
dem `LEFT JOIN` auf `Project`, weil sie dort nie `null` ist und weil sie es ist, die in der Antwort
steht.

**Übrig bleibt ein Vorbehalt an die Messung und keiner am Bau:** L12 und M80‑1 haben
`ORDER BY ProjectName, ProcessName` gefahren. Der ausgelieferte Text ist an dieser einen Stelle
nicht mehr der gemessene (§8). Die Nummer 2 bleibt vergeben, damit Verweise auf sie nicht ins Leere
zeigen; es stehen damit **sechs** Abweichungen und eine zurückgenommene.

**3. Die Antwort trägt ein achtes Feld: `projectName`.** Der Auftrag zählt sieben Felder auf und
nennt `projectId`, nicht `projectName`. **Das Feld bleibt, seine Begründung ändert sich mit
Abweichung 2: Es dient der Anzeige und nicht dem Nachvollziehen der Reihenfolge.** Sortiert wird
seit dem 20.08.2026 nach Kennungen; lesbar ist die Zeile trotzdem erst mit dem Namen, denn
`ProjectName` und `ProjectID` sind verschiedene Zeichenketten (M75, `ProjectID` trägt bei 67 von
140 Projekten einen Unterstrich, `ProjectName` bei keinem). Das Feld ist additiv; die sieben
beauftragten sind unverändert da.

**4. `/api/katalog/**` verlangt die Rolle `ADMIN`.** Der Auftrag nennt keine Rolle. Ohne eine Regel
fiele der Katalog unter `anyRequest().authenticated()`, und jeder angemeldete Nutzer könnte die
Kuratierung seines Mandanten ändern. `IMPLEMENTIERUNGSPLAN_MVP.md` führt die Katalogpflege im
Administrationsbereich („nur für die Rolle `ADMIN` sichtbar"), `PROJEKTBESCHREIBUNG.md` §9 ebenso.
**Vom Auftraggeber am 20.08.2026 ausdrücklich entschieden**, nicht angenommen.

**5. Protokolliert werden alle drei schreibenden Vorgänge, nicht nur die einzelne Zuordnung.** Der
Auftrag verlangt einen `audit_log`-Eintrag nur bei `PUT`. Eine Massenzuordnung überschreibt bis zu
226 gepflegte Zeilen und ein Heuristik-Lauf berührt jede offene Zeile eines Mandanten; wären die
beiden nicht im Protokoll, wäre ausgerechnet die folgenreichste Änderung die unsichtbarste.
`AuditEventType.KATALOG_GEAENDERT` existiert seit Schritt 2, es kostet keine Migration.

**6. Zwei Lesarten der Regeln sind entschieden worden**, beide an den Zahlen aus §3.5 belegt und in
§3.1 und §3.2 ausgeschrieben: der Nummernpräfix als Bedingung von Regel A, und „ohne Trennzeichen"
als Zweckbeschreibung statt als Bedingung von Regel B. Ohne die erste bekämen 14 von 17
`NXHBE`-Prozessen einen Vorschlag, den §3.5 nicht kennt.

**6a. Die Zahlen aus §3.5 sind nicht erreichbar, und das ist gemessen** (M80‑6). Regel A trifft die
Projektion auf den Prozess genau. Regel B liefert **280 statt 281** — sechs `IBIS`/`IBISGUS`-Prozesse
tragen das Ankerwort überhaupt nicht und sind für **keine** Fassung der Regel erreichbar, dafür
trifft sie fünfmal bei `EDITIONLINGERI`, wo §3.5 sie nicht erwartet. Die Festlegung trägt seit dem
20.08.2026 einen datierten Korrekturkasten dazu; der alte Wortlaut steht unverändert daneben.

**6b. Regel B ist nach der ersten Messung korrigiert worden.** Die zuerst gebaute Fassung verlangte
vor dem Anker einen Kleinbuchstaben — eine Bedingung, die in der Vorgabe nicht steht. Sie kostete 26
Prozesse. Das ist die einzige Stelle, an der der Bau von seiner *eigenen* ersten Fassung abweicht,
und sie steht hier, weil eine stillschweigend behobene Erfindung dasselbe verschweigt wie eine
stillschweigende Abweichung.

**7. `SicherheitsTestbasis` hat einen `PUT`-Helfer bekommen.** Bis zum 20.08.2026 gab es im gesamten
Backend kein einziges `PUT`; ohne den Helfer wäre der Endpunkt nicht prüfbar gewesen. Er setzt den
CSRF-Kopf wie `sende(…)`.

### Zum Nachtrag vom 21.08.2026 (E14–E16)

**8. `bestand_geprueft_am` kommt aus `systemClock`, nicht aus der Anwendungsuhr.** Der Bauauftrag
verlangt ausdrücklich die **Anwendungsuhr** und beruft sich dabei auf Regel Z1. **Z1 trägt das
nicht:** Sie verbietet den direkten `now()`-Aufruf und führt *beide* Uhren auf — für
sicherheitsrelevante Zeit **und Protokollzeit** verlangt sie ausdrücklich `systemClock`
(`DEVELOPMENT_GUIDELINES.md` §4.5). Sie entscheidet die Frage also nicht gegen den Bau, sondern für
ihn.

Dazu kommt ein Argument aus der Zeile selbst: `geaendert_am` steht seit `V6` daneben und kommt aus
`systemClock` (Regel A5, dieselbe Wahl wie beim `audit_log`). Gemischt lägen **zwei Zeitstempel
derselben Zeile im Profil `dev` Wochen auseinander**, weil die Anwendungsuhr um den Rückstand der
Testkopie zurückversetzt ist — ein Prüfzeitpunkt in der Vergangenheit wäre keiner. **Gemeldet und
nicht stillschweigend aufgelöst.**

**9. Regel L7 war mit M83 *nicht* erfüllt — M84 ist nachgeholt worden.** Der Auftrag führt unter
V3: *„M83 ist die Messung zu diesem Bau. Regel 7 … ist damit erfüllt, solange das gerenderte SQL dem
gemessenen Text entspricht."* **M83 selbst sagt das Gegenteil:** Der dort gemessene Text ist von
Hand getippt, Anwendungscode existierte nicht, und *„sobald er existiert, verlangt L7 eine neue
Messung des gerenderten Textes"*.

Das gerenderte SQL ist protokolliert und gegen den gemessenen Text gehalten worden. **Alle drei im
Auftrag genannten Meldekriterien sind eingehalten** — Einstiegstabelle `ProjectMandant`, `EXISTS`
statt `IN`, kein zusätzlicher Join. Die Unterschiede sind genau die, die M83 vorhergesagt hat: volle
Qualifizierung statt Aliase, Backticks, ``select 1 as `one` ``, kein Spaltenalias `AS traegt`, und
ein Bindeplatz `?` statt der Sitzungsvariablen `@mandant`. **Keiner davon ist planbestimmend** —
`M84` weist denselben `EXPLAIN` nach, Zeile für Zeile (§8.1). Es war damit **kein** Meldefall im
Sinne des Auftrags, wohl aber eine offene Messpflicht; sie ist geschlossen.

**10. Der Bestandslauf ist der einzige Schreibweg ohne Upsert.** Das ist keine Abweichung vom
Auftrag, sondern von einer bestehenden Zusicherung dieses Backends, und steht deshalb hier: Der
Invariantentest verlangte von **jedem** Schreiben ein `INSERT … ON DUPLICATE KEY UPDATE`. Er ist
geschärft worden statt aufgeweicht (§7).

**11. Der Auftrag nennt `V8` ohne Dateinamen; gebaut ist `V8__bestandsspalte.sql`.** Erwähnt, weil
Flyway-Migrationen nach dem ersten Lauf eingefroren sind — der Name ist ab jetzt nicht mehr
änderbar, ohne den Anwendungsstart zu brechen.

### Zum Nachtrag vom 26.08.2026 (E22–E24)

**12. Die Übernahme bekommt eine eigene Ereignisart, nicht `KATALOG_GEAENDERT`.** Das ist keine
Abweichung vom Auftrag — er verlangt es —, sondern eine von Abweichung 5, die alle drei bisherigen
Schreibwege in **eine** Art gelegt hat. Der Grund für die Ausnahme ist der Kern des Risikos dieser
Funktion: **Nach dem Knopfdruck ist „per Regel übernommen" von „von Hand kuratiert" in der Zeile
selbst nicht mehr zu unterscheiden.** `geaendert_von` trägt in beiden Fällen denselben Namen — das
ist der offene Punkt 2 unten, und dieser Knopf hebt ihn von 733 Einzelfällen auf einen Massenfall.
Das `audit_log` ist damit der **einzige** Ort, an dem der Unterschied überhaupt noch steht; er darf
dort nicht mit der einzelnen Zuordnung in einen Topf fallen. **Ohne Migration** — die Whitelist ist
ein Aufzählungstyp im Code.

**13. Die Signatur des Schreibwegs weicht vom Auftragstext ab.** Er nennt
`uebernehmeVorschlaege(MandantContext, List<String> processIds, String benutzer, Instant jetzt)`;
gebaut ist `(MandantContext, List<String> processIds, LocalDateTime jetztUtc, String benutzer)`.

Zwei Änderungen, beide aus der Nachbarschaft begründet: Die Spalte ist `DATETIME(3)` in UTC, und
alle vier Nachbarmethoden dieser Klasse nehmen `LocalDateTime jetztUtc` in genau dieser Position
entgegen. Ein `Instant` legte die Zeitzonenumrechnung für **einen** von fünf Schreibwegen ins
Repository, während sie für die übrigen vier im Dienst steht (`ProzessKatalogService.jetztUtc`).
**Der Zeitpunkt selbst ist unverändert der beauftragte:** `systemClock`, Regel A5.

**14. Die Signatur des Dienstes weicht ebenfalls ab.** Der Auftrag nennt
`uebernehmeVorschlaege(MandantContext mandant, Massenmodus modus)`; gebaut ist
`(AngemeldeterNutzer nutzer, MandantContext mandant, String modusRoh, String ip)` — wie bei
`massenzuordnung`. **Die beauftragte Fassung könnte den `audit_log`-Eintrag nicht schreiben, den
derselbe Auftrag verlangt:** Er braucht Benutzer und IP. Und `Massenmodus.ausText` gehört in den
Dienst, weil ein *unbekannter* Modus `400` ist und nicht stillschweigend auf die Vorgabe fällt.

**15. Die `WARN`-Zeile steht im Dienst und nicht im Repository.** Der Auftrag ordnet sie keiner
Schicht zu. Sie steht dort, wo **beide** Zahlen liegen — die Größe der gelesenen Liste und die vom
`UPDATE` gemeldete —, und lässt das Repository eine reine Datenzugriffsmethode bleiben.

**16. „Vorschau und Ausführung fahren denselben gerenderten Lesetext" ist nicht wörtlich prüfbar,
und die gebaute Zusicherung ist stärker.** Der Auftrag verlangt den Vergleich zweier gerenderter
Texte „wie der bestehende Fall für die Massenzuordnung". Dort ruft `speichereFeld` die Lesung
selbst auf, und es *gibt* zwei Texte. Hier liest der **Dienst** einmal, zählt über genau diese
Liste und schreibt genau sie; **der Schreibweg trägt gar kein `SELECT`**. Der Statementtest prüft
deshalb beides: dass die Lesung Zeichen für Zeichen dieselbe bleibt, und dass das Schreiben keine
eigene Lesung mitbringt. **Es kann nichts driften, weil es nichts gibt, was auseinanderlaufen
könnte.**

**17. „Regel L15" gibt es in `DEVELOPMENT_GUIDELINES.md` nicht.** Der Auftrag beruft sich darauf;
§4.4 dort führt **L1 bis L10**. „L15" stammt aus [`messungen-schritt4.md`](messungen-schritt4.md)
als Nummer einer *Messung* und ist seither Projektkürzel für „die Einstiegstabelle im `EXPLAIN`
belegen statt annehmen, und kein `STRAIGHT_JOIN`". **Der Sache ist gefolgt worden**, der Nummer
nicht: M93 weist den Plan für alle vier Mandanten im Volltext nach. **Gemeldet und nicht
stillschweigend aufgelöst.**

---

## 10. Offene Punkte

> ### ⚠️ Zwei Tests in `ProzessKatalogIsolationDbIT` sind seit dem 27.08.2026 rot — und der Grund liegt in den Daten
>
> **Gefunden bei der Abnahme von Schritt 10b‑1**, der mit diesem Bereich nichts zu tun hat. Rot sind
> `uebernahme_erfasst_nur_den_aktiven_mandanten` und
> `uebernahme_ignoriert_untergeschobenen_mandanten`; beide scheitern an ihrer eigenen
> **Vorbedingung**, nicht an der Sache, die sie prüfen.
>
> **Was gemessen ist** (27.08.2026, gegen die Testkopie):
>
> ```
> SELECT pm.MandantID, c.pflegestatus, c.vorschlag_herkunft, COUNT(*)
> FROM process_catalog c JOIN Process p … JOIN ProjectMandant pm …
> WHERE c.pflegestatus = 'OFFEN' GROUP BY 1,2,3;
> → VOTG | OFFEN | KEINE | 12     (und sonst nichts)
> ```
>
> **Im gesamten Bestand gibt es keine einzige übernehmbare Zeile mehr.**
> `findeUebernehmbareVorschlaege` verlangt `pflegestatus = 'OFFEN'` **und**
> `vorschlag_herkunft IN ('REGEL_A','REGEL_B')`; die zwölf verbliebenen offenen Zeilen tragen alle
> `KEINE`. Damit liefert die Vorschau für **jeden** Mandanten `betroffen = 0`, und beide Tests
> verlangen von ihrem Gegenmandanten eine positive bzw. eine *andere* Zahl.
>
> **Der erste Test sagt seine eigene Diagnose:** *„Steht hier 0, ist der Katalog von `VOTG`
> vollständig kuratiert und dieser Test braucht einen anderen Gegenmandanten."* Genau das ist
> eingetreten — nur reicht ein anderer Gegenmandant nicht mehr, weil es **nirgends** mehr eine
> übernehmbare Zeile gibt. Die Kuratierung vom 24. bis 27.08.2026 hat 1.486 Katalogzeilen angefasst,
> davon 505 am Morgen des 27.08. ([`messungen-schritt10b.md`](messungen-schritt10b.md) V3, Befund
> 22); `SUTTONS`, das dort noch 17 offene Zeilen trug, ist seither ebenfalls gepflegt.
>
> **Es ist kein Codefehler.** Der Endpunkt verhält sich richtig: Wo nichts zu übernehmen ist, ist
> `betroffen = 0`. Rot ist die Annahme des Tests, der Bestand halte für ihn dauerhaft eine
> unkuratierte Ecke bereit.
>
> **Nicht in Schritt 10b‑1 repariert, und zwar bewusst.** Die Reparatur gehört diesem Bereich und
> nicht jenem Schritt, und sie ist keine Kleinigkeit: Der Test müsste sich seine Vorbedingung
> **selbst anlegen** — eine offene Zeile mit `REGEL_A` beim Gegenmandanten — und dabei die Falle aus
> §7 umgehen, die in `BestandslaufDbIT` schon einmal von Hand kuratierte Daten gekostet hat. **Das
> ist eine Entscheidung und ein eigener Schritt.**


1. **Drei der vier offenen Punkte aus [`prozess-katalog.md`](prozess-katalog.md) §10 bleiben offen,
   der vierte ist für vier seiner fünf Mandanten beantwortet.**
   - *Offen:* `ZAST` (35) und `NXHBE` (17) haben kein Verfahren für den **Partner**; ob `WOC` (4)
     und `SYSTEM` (4) überhaupt kuratiert werden sollen, ist nicht entschieden; und Regel A bei
     **zwei** Unterstrichen ist für die neun `NEXANS`-Prozesse dieser Gestalt weiterhin nicht
     geprüft. Der Bau nimmt keine dieser Entscheidungen vorweg.
   - *Beantwortet (M80‑7):* Der Anker in **Projektnamen** wirkt bei `NXHBE` für **alle 17**
     Prozesse und bei `ZAST`, `WOC` und `SYSTEM` **gar nicht**. **Offen bleibt dort allein
     `EDITIONLINGERI`:** Seine fünf Zeilen mit Richtung sind genau die fünf, die Regel B über den
     **Prozess**namen trifft — was seine Projektnamen tragen, ist an dieser Zählung nicht
     ablesbar.
   - *Und einer ist kleiner geworden:* `EDITIONLINGERI` steht nicht mehr vollständig ohne
     Verfahren — Regel B trifft dort bei fünf von neun Prozessen.
2. **Der Änderungsvermerk kennt kein „von wem angelegt".** `geaendert_von` trägt nach einem
   Heuristik-Lauf den Namen dessen, der den Knopf gedrückt hat — nicht den einer Kuratierung. Wer
   die beiden unterscheiden will, liest das `audit_log`; an der Zeile selbst steht nur der letzte
   Anfasser. Das ist bewusst so und hier vermerkt, damit niemand die Spalte später als Kuratierungs-
   nachweis liest.
3. **Es gibt keinen Sekundärindex auf `pflegestatus`.** Der Filter `nurOffene` arbeitet innerhalb
   der Prozessmenge eines Mandanten (höchstens 733 Zeilen) und wird vom Join getrieben. Bei einem
   deutlich größeren Bestand wäre die Frage neu zu stellen.
4. **Die Sonderbehandlung von `00001_Undefined` ist nicht gebaut.** Sie steht im Plan unter
   Schritt 9b, gehört aber laut M78 in die Oberfläche — und der Filter dafür darf nicht `^0+_` sein,
   sonst behandelt er vier reguläre Prozesse als Auffangbecken.
5. **Eine Sichtprüfung im Browser steht aus**, wie bei jedem Backend-Teil. Sie ist erst mit der
   Oberfläche möglich.

   > **Am 24.08.2026 zu vier Fünfteln nachgeholt**, mit der Oberfläche aus
   > [`prozess-katalog-frontend.md`](prozess-katalog-frontend.md): `GET /prozesse`,
   > `GET /partner`, `POST /vorschlagen` und `POST /massenzuordnung` im Modus `VORSCHAU` sind gegen
   > `NEXANS` gefahren und liefern, was §3.5 und M83‑5 erwarten lassen — `regelA 509`, `keine 224`,
   > `bestandGeprueft 733`, `ohneNachrichten 217`, und für das größte Projekt „betrifft 109
   > Prozesse". **`PUT /prozesse/{processId}` und `AUSFUEHREN` sind bewusst nicht gedrückt worden:**
   > Beide schreiben Kuratierung auf die geteilte Testkopie, und eine kuratierte Zeile überlebt nach
   > §7 jeden Testlauf.
6. **Das Schreiben des Bestandslaufs ist nicht gemessen.** Das `UPDATE` läuft über den Schreib-Pool
   und ist nicht `EXPLAIN`-bar; belegt ist nur die Schranke aus M80 (1.490 Zeilen über zehn
   Transaktionen in unter zehn Sekunden), und die ist eine **Beobachtung des Bauablaufs, keine
   Messung**. Es ist jetzt ein Stapel über bis zu 733 Zeilen je Knopfdruck — und die Testkopie
   braucht laut Erfahrung 10 bis 25 s je `COMMIT`. **Vor der Produktion zu erheben.**

   > **Teilweise beantwortet am 24.08.2026 durch M85** — gemessen ist nicht das `UPDATE`, sondern
   > der **ganze Knopfdruck** aus dem Browser: **509,4 ms** beim ersten Druck auf `NEXANS`
   > (733 Zeilen angelegt), **350,6 ms** beim zweiten (733 aufgefrischt). Die Sorge, ein Stapel über
   > 733 Zeilen könne die synchrone Bauform sprengen, ist damit für diesen Schreibweg **nicht
   > eingetreten** — die Erfahrung mit 10 bis 25 s je `COMMIT` stammt aus anderen Sitzungen und
   > wird durch M85 nicht nachgemessen. **Punkt 6 bleibt trotzdem offen:** M85 misst von außen und
   > kann das `UPDATE` weiterhin nicht auftrennen, und für die **Produktion** gilt der Vorbehalt
   > unverändert. Der Satz „vor der Produktion zu erheben" steht.
7. **Der Lauf hat kein Zeitlimit und keinen Fortschritt.** Er fährt drei Schritte hintereinander in
   **einer** Transaktion; die Oberfläche bekommt erst am Ende eine Antwort. Bei `NEXANS` sind das
   733 Zeilen — gemessen am 24.08.2026 mit **509,4 ms** von Anfang bis Ende (M85, Punkt 6). Ein
   Zeitlimit und ein Fortschritt bleiben trotzdem aus, und für die Produktion ist die Zahl eine
   optimistische Schranke.
8. **`bestandGeprueft` zählt Anweisungen, nicht geänderte Zeilen.** MariaDB meldet für ein `UPDATE`
   ohne Wertänderung **null** betroffene Zeilen. Gezählt wird deshalb jede Anweisung des Stapels,
   die nicht fehlgeschlagen ist — sonst meldete der zweite Lauf in Folge `0`. Die Zahl beantwortet
   damit „wie viele Zeilen hat der Lauf angefasst" und **nicht** „wie viele haben sich geändert".
   Wer Letzteres braucht, braucht eine andere Zählung.
9. **Der Filter aus E20 ist clientseitig und damit hier nicht gebaut.** Das Backend liefert die drei
   Zustände; wer sie filtert, ist die Oberfläche. Vermerkt, damit niemand später einen
   Serverparameter dafür sucht.

### Dazu seit dem 26.08.2026 (E22–E24)

10. **Es gibt keinen Weg von `GEPFLEGT` zurück nach `OFFEN`, und die Übernahme verbreitert die
    Einbahnstraße.** Gegen den Code geprüft: `PUT /api/katalog/prozesse/{processId}` setzt **immer**
    `GEPFLEGT` (`speichereZuordnung`), der Lauf überspringt gepflegte Zeilen (E13, `vorschlagen`),
    und die Massenzuordnung setzt **ein Feld** und keinen Pflegestatus — sie setzt ihn zwar
    ebenfalls auf `GEPFLEGT`, aber nie zurück. Mit E22 wird aus einem Einzelfall ein Massenfall von
    bis zu 509 Zeilen. **Gebaut wird nichts dagegen**; der Preis steht hier, bevor ihn jemand zahlt.

    > **Was das praktisch heißt:** Wer die Vorschläge übernimmt und den Regeln zu Unrecht geglaubt
    > hat, korrigiert von Hand oder über die Massenzuordnung — Zeile für Zeile oder Projekt für
    > Projekt. Ein „Übernahme rückgängig" gibt es nicht und wäre eine eigene Entscheidung; es
    > bräuchte einen Zustand, den die Tabelle heute nicht führt (siehe Punkt 11).

11. **„Per Regel übernommen" und „von Hand kuratiert" sind in der Zeile nicht mehr
    unterscheidbar** — nur noch im `audit_log`. Das ist Punkt 2 in verschärfter Form: Dort ging es
    um `geaendert_von` nach einem Heuristik-Lauf, hier um den Pflegestatus nach einer Übernahme.
    Denkbar wäre ein **vierter Wert für `vorschlag_herkunft`** (etwa `REGEL_A_UEBERNOMMEN`) oder
    eine **eigene Spalte `uebernommen_am`**. **Beides ist ausdrücklich nicht gebaut**: Es wäre eine
    Migration und eine eigene Entscheidung. Vermerkt, damit niemand die Spalte später als
    Kuratierungsnachweis liest.

12. **Die Aufräumregel der Tests unterscheidet nicht zwischen „angelegt" und „zuletzt angefasst".**
    Sie löscht, was in `geaendert_von` das Präfix `it-` trägt. Ein Test, der eine **vorhandene**
    Zeile per Upsert anfasst, setzt dieses Präfix — und lässt sie damit löschen. Am 26.08.2026 ist
    genau das passiert und hat eine `VOTG`-Katalogzeile gekostet (§7, M93‑6). Behoben ist es an der
    Ursache: Die Tests legen nur noch auf Prozessen **ohne** Zeile an, mit reinem `INSERT`. **Die
    Regel selbst bleibt, wie sie ist** — sie ist folgenlos, solange kein Schreibweg dieses Backends
    `geaendert_von` auf ein fremdes Präfix setzen kann, und der einzige, der es konnte, war ein
    Test.

13. **Das Schreiben der Übernahme ist nicht gemessen.** Dasselbe wie bei Punkt 6: Das `UPDATE`
    läuft über den Schreib-Pool und ist nicht `EXPLAIN`-bar. Anders als der Bestandslauf ist es ein
    **einzelnes** Statement mit bis zu 733 Bindeplätzen und kein Stapel — die Sorge aus Punkt 6
    trifft es also nicht in derselben Form. Belegt ist trotzdem nichts. **Vor der Produktion zu
    erheben.**

14. **Die vorregistrierten Zahlen der Messung sind auf der Testkopie nicht reproduzierbar**, weil
    der Katalog dort kuratiert ist: `NEXANS` steht auf 733 gepflegten Zeilen, `IBISGUS` hat keine
    (M93‑0). Belegt ist die Bedingung an `VOTG` und `IBIS`. **Wer die beauftragten Zahlen sehen
    will, braucht einen Katalog, der gelaufen und nicht kuratiert ist** — für `IBISGUS` genügt ein
    Knopfdruck auf `POST /api/katalog/vorschlagen`, für `NEXANS` nicht (E13).
