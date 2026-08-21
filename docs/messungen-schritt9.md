# Messungen vor Schritt 9b — Prozesskatalog, Partner und Richtung

Erhoben am **20.08.2026** gegen die Testkopie (`GlassfishDB`).
Auftrag: „Messrunde vor Schritt 9b — Aufgabenstellung, **Fassung 1**" vom 19.08.2026.

**Diese Runde baut nichts und entscheidet nichts.** Die Lesarten standen vor der Erhebung fest;
sie sind unten je Messung als *Vorregistrierte Deutung* mitgeführt und nach dem Ergebnis
unverändert dagegengehalten. Welche Bauform daraus wird, entscheidet der Auftraggeber.

**`process_catalog` existiert zum Zeitpunkt dieser Runde nicht.** Alle Abfragen laufen gegen
`GlassfishDB` allein. Kein Filestore, kein Schreibzugriff, keine Migration.

---

## Nummernvergabe

**Der Auftrag geht von M72 als letzter vergebener Nummer aus. Das stimmt nicht.**

| | |
|---|---|
| Prüfung | `grep -rnoE '\bM(69\|7[3-9])\b' --include='*.md' --include='*.sql' --include='*.ps1' .` über `docs\`, das Wurzelverzeichnis und `scripts\` |
| Ergebnis | **`M73` ist am 19.08.2026 vergeben** — in [`messungen-schritt8.md`](messungen-schritt8.md) Z. 115 und Z. 991, für die Frage „Worauf zeigt `Message.Payload.GUID`?". Die Nummer ist von dort aus in `datenmodell.md`, `frontend-grundlagen.md`, `IMPLEMENTIERUNGSPLAN_MVP.md` und `annahmen-korrekturen.md` verlinkt |
| **M74 bis M79** | in `docs\`, im Wurzelverzeichnis und in `scripts\` **kein einziger Treffer**. Frei, hier vergeben |
| **M69** | bleibt **frei und unvergeben**, wie in [`messungen-schritt8.md`](messungen-schritt8.md) Z. 3196 festgehalten. Sie ist hier **nicht** umgewidmet worden |

**Die Runde ist deshalb um eins verschoben.** Der Auftrag belegte M73–M78, vergeben ist M74–M79:

| Auftrag | hier | Gegenstand |
|---|---|---|
| M73a | **M74a** | Prozessbestand je Mandant *(beantwortet V3)* |
| M73b | **M74b** | Wie viele Prozesse tragen überhaupt Nachrichten? |
| M74 | **M75** | Gestalt der Prozess- und Projektnamen je Mandant |
| M75 | **M76** | Trägt der Partner im Projekt oder im Prozess? |
| M76 | **M77** | Steckt die Richtung im `SOSName`? |
| M77 | **M78** | Wie viele Auffangprozesse gibt es? |
| M78 | **M79** | Kostet die Pflegeliste etwas? — **entfällt**, siehe dort |

> Die Aufgabenstellung ist damit an ihrer Vorbedingung V1 korrigiert und **nicht** doppelt
> vergeben worden. Das ist genau der Fall, für den V1 geschrieben war.

---

## Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB — niemals die Produktion |
| Nachweis Testkopie | `SELECT @@global.read_only` → **`1`**, als erstes Statement der Runde und in der Schlusssitzung erneut |
| Benutzer | `monitor_read@%`, ausschließlich `SELECT` |
| Sitzungen | sequenziell, jede eine eigene `mysql`-Ausführung mit einer Skriptdatei unter `scripts/messung-schritt9/` |
| Laufzeit | `SET profiling = 1` / `SHOW PROFILES`. **Aufwärmlauf bei den Hauptmessungen** von M74a und M76; die übrigen Statements sind einmalig profiliert und in der Laufzeittabelle mit `—` in der Aufwärmlaufspalte ausgewiesen |
| **S1** | ausschließlich `SELECT`, `SET`, `EXPLAIN`, `SHOW PROFILES` |
| **L7 / L15** | die einzige Abfrage dieser Runde, die Anwendungscode wird, ist M79 — und sie entfällt zugunsten der bestehenden Messung L12, die bereits zwei Mandanten fährt |
| **L10** | wo das Dokument eine bestehende Behauptung prüft, steht *Gemessen war* / *Behauptet wird* nebeneinander — in V2, V2 (Zusatz) und V3. Die Messungen selbst halten stattdessen die **vorregistrierte Deutung** gegen das Ergebnis; das ist dieselbe Regel in der Form, die der Auftrag vorgibt. Der zweite Gebrauch der Nummer (M76 und M78 liefern *Anschauungsmaterial, keine Kennzahl*) stammt wörtlich aus dem Auftrag |
| **G1** | keine Zugangsdaten, kein Hostname, keine UUID. Zu Partnernamen siehe den eigenen Abschnitt unten |

| Angabe | Wert |
|---|---|
| Versionsstring | `10.6.22-MariaDB-0ubuntu0.22.04.1-log` |
| Serverzeit zu Beginn | `2026-08-20 09:05:04` |
| Serverzeit am Ende | `2026-08-20 09:12:46` |
| **`@@global.read_only`** | **`1`** — Beginn und Ende |
| `@@div_precision_increment` | **4** — deshalb steht in keiner Abfrage dieser Runde ein `AVG` über einen Wahrheitswert; Anteile sind als `100.0 * SUM(…) / COUNT(*)` gerechnet ([`messungen-schritt7.md`](messungen-schritt7.md) M46) |
| `@@session.sql_mode` | `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION` |
| Client und Version | `mysql.exe` **Ver 8.0.46 for Win64 on x86_64** aus MySQL Workbench 8.0 CE, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t` |
| Passwortübergabe | über `MYSQL_PWD` aus `OVERLORD_DB_READ_PASSWORD`; **kein Passwort auf der Befehlszeile, keines in einer Skriptdatei** |
| `profiling_history_size` | Vorgabe der Instanz ist **15**. In jeder Sitzung auf **100** gesetzt |
| Datenstand | `MAX(Message.MessageLastUpdate)` = **`2026-07-08 17:21:10`** — unverändert gegenüber M0 |

**Abweichung vom Auftrag beim Client.** Der Auftrag schreibt `--skip-ssl` vor („sonst bricht der
Client mit ‚SSL is required' ab"). Das ist die **MariaDB**-Schreibweise; der hier verwendete
Workbench-Client kennt sie nicht und bricht mit `unknown option` ab, noch vor der Sitzung.
Gefahren ist deshalb `--ssl-mode=DISABLED`, dieselbe Wirkung. Ein Wechsel auf den
MariaDB-Client hätte die Vergleichbarkeit mit **allen** früheren Runden gekostet, die mit dem
Workbench-Client gemessen sind.

### Was unter G1 in dieser Datei steht — und was nicht

Diese Runde liest fast ausschließlich **Namen**. Das macht die Geheimhaltungsfrage zum
Kernproblem der Darstellung und nicht zur Formalie.

Maßgeblich ist die Schärfung vom 07.08.2026 ([`annahmen-korrekturen.md`](annahmen-korrekturen.md)
Z. 186–192), wörtlich: „**Der Trennstrich läuft zwischen Nutzdaten und Konfiguration**, nicht
zwischen ‚intern' und ‚extern'. Geschützt sind Belegnummern, `MessagePropertyValue`-Inhalte,
**Partner**namen, Zugangsdaten und Hostnamen; nicht geschützt ist das Konfigurationsvokabular des
Altsystems einschließlich der Mandantenkürzel".

`ProcessID` und `ProjectID` sind Konfigurationsvokabular — **tragen aber vielfach Partnernamen im
Text**. Die verbindliche Datei zieht die Linie selbst: `PROJEKTBESCHREIBUNG.md` §4.4 druckt
`100_VTG_BAYER`, `300_KundenEingehend`, `40000_AMG_LAB_VDA`, `TYCO_AMP`, `DELFINGEN_DE_HA`,
`BASF`, `BASFANTWERPEN`, `BASFPOLY`, `NONBASF` im Klartext ab.

**Daraus die Regel für diese Datei:**

- **Einzelne Kennungen als Muster:** ja — beschränkt auf solche, die bereits im Repository stehen,
  und auf die wenigen, die ein Befund ohne sie nicht tragen würde.
- **Vollständige Listen:** nein. Die 36 Partnerprojekte von `VOTG` und die rund 60 Handelsketten
  aus den `SOSName` von `IBIS`/`IBISGUS` sind **Kundenlisten Dritter**. Sie stehen hier als
  **Zahl und Gestalt**, nicht als Aufzählung.
- **Die Rohausgaben** liegen unter `scripts/messung-schritt9/ergebnis/` und sind über
  `.gitignore` **vom Repository ausgeschlossen** — dieselbe Trennung wie bei Schritt 8: die
  `.sql`-Sitzungen gehören ins Repository — sie sind der Beleg dafür, wie gemessen wurde —,
  die Laufergebnisse nicht.

---

## Vorbedingungen

### V1 — Nummernvergabe

**Erledigt, mit Korrektur.** Siehe „Nummernvergabe" oben. Die Runde belegt M74–M79.

### V2 — Ist `Process`/`Project` bereits nach Regel L8 erhoben?

**Ja — und die Erhebung ist hier nachgeprüft worden.**

*Gemessen war:* `information_schema.COLUMNS` liefert für `Process` **4** Spalten, `Project` **3**,
`ProjectMandant` **2**, `SOS` **4** — Zeichen für Zeichen die Liste, die
[`messungen-schritt4.md`](messungen-schritt4.md) **M1** am 01.08.2026 dokumentiert hat.

*Behauptet wird:* dasselbe. Keine Abweichung, keine verschwiegene Spalte. Der
`MessageAction`-Fall wiederholt sich hier **nicht**.

| Tabelle | # | Spalte | Typ | NULL | Schlüssel |
|---|---|---|---|---|---|
| `Process` | 1 | `ProcessID` | `varchar(36)` | NO | PRI |
| `Process` | 2 | `ProcessName` | `varchar(255)` | YES | |
| `Process` | 3 | `ProcessDescription` | `text` | YES | |
| `Process` | 4 | `ProjectID` | `varchar(36)` | YES | MUL |
| `Project` | 1 | `ProjectID` | `varchar(36)` | NO | PRI |
| `Project` | 2 | `ProjectName` | `varchar(255)` | YES | |
| `Project` | 3 | `ProjectDescription` | `text` | YES | |
| `ProjectMandant` | 1 | `ProjectID` | `varchar(36)` | NO | PRI |
| `ProjectMandant` | 2 | `MandantID` | `varchar(36)` | NO | PRI |
| `SOS` | 1 | `SOSID` | `varchar(36)` | NO | PRI |
| `SOS` | 2 | `SOSName` | `varchar(255)` | YES | |
| `SOS` | 3 | `SOSDescription` | `text` | YES | |
| `SOS` | 4 | `ProcessID` | `varchar(36)` | YES | MUL |

M74a schrumpft damit auftragsgemäß auf einen Verweis, **aber nur für die Spalten**. Zwei Dinge
hat die Nachprüfung zusätzlich ergeben, und beide sind Befunde:

**(a) `Process.ProcessID` ist lesbarer Text, keine GUID.** Das ist die stillschweigende
Voraussetzung der Messungen M75, M76 und M78 — sie zerlegen `ProcessID`, nicht `ProcessName`;
M77 benutzt die Spalte nur als Verbundschlüssel. Wäre
die Spalte eine UUID gewesen, hätte die halbe Runde die falsche Spalte vermessen. Gemessen:

| Spalte | GUID-Form | mit `_` | Länge min–max | Zeilen |
|---|---|---|---|---|
| `Process.ProcessID` | **0** | 1.214 | 8–36 | 1.503 |
| `Process.ProcessName` | **0** | 460 | 6–58 | 1.503 |
| `Project.ProjectID` | **0** | 67 | 5–35 | 140 |
| `Project.ProjectName` | **0** | 0 | 5–48 | 140 |
| `ProjectMandant.MandantID` | **0** | 0 | 3–14 | 134 |

**(b) `Process` trägt mit `ProcessName` eine zweite lesbare Spalte, die der Auftrag nicht
erwähnt.** Sie ist in M75 mitgemessen worden; das Ergebnis steht dort.

### V2 (Zusatz) — die Indexliste, die der Auftrag als „gelesen, nicht erhoben" ausweist

Der Auftrag vermerkt bei M74b selbst: „*Die Indexliste ist **gelesen** aus
`PROJEKTBESCHREIBUNG.md` §3.2, nicht für diese Runde erhoben.*" Sie ist jetzt erhoben, und
**sie ist unvollständig.**

*Gemessen war:* `Message` trägt **acht** Indizes (`PRIMARY` und sieben weitere).

*Behauptet wird* in `PROJEKTBESCHREIBUNG.md` §3.2 und im Auftrag: drei —
`MessageLastUpdateIDX`, `MessageStatusIDX`, `MessageLastUpdateProcessMessageIDX`.

| Tabelle | Index | Spalten | eindeutig | Kardinalität |
|---|---|---|---|---|
| `Message` | `PRIMARY` | `MessageID` | ja | 3.560.486 |
| `Message` | `MessageLastUpdateIDX` | `MessageLastUpdate` | nein | 1.780.243 |
| `Message` | `MessageLastUpdateProcessMessageIDX` | `MessageLastUpdate, ProcessID, MessageID` | nein | 3.560.486 |
| `Message` | `MessageStatusIDX` | `MessageStatus` | nein | 18 |
| `Message` | **`Message_ProcessFK`** | **`ProcessID`** | nein | 18 |
| `Message` | **`ProejctIDIDX`** *(sic)* | **`ProcessID`** | nein | 18 |
| `Message` | `SourceMessageIDIDX` | `SourceMessageID` | nein | 1.780.243 |
| `Message` | `TargetMessageIDIDX` | `TargetMessageID` | nein | 63.580 |

> **Das kippt die Begründung von M74b.** Der Auftrag schreibt dort: „**Es gibt keinen
> eigenständigen Index auf `ProcessID`.** Ein `EXISTS (SELECT 1 FROM Message WHERE ProcessID = ?)`
> je Prozess wäre damit 1.503-mal ein voller Durchlauf." Es gibt **zwei** eigenständige Indizes
> auf `ProcessID`. Der `EXISTS`-Weg ist gangbar und ist in M74b gefahren worden — er kostet
> 5,7 Sekunden für alle 1.503 Prozesse, nicht 1.503 Tabellendurchläufe.
>
> **Zu den Kardinalitäten:** die drei Werte `18` sind InnoDB-Schätzwerte und nicht die Zahl
> verschiedener Werte. M74b zählt für `Message.ProcessID` **738** verschiedene Kennungen. Der
> Befund hängt nicht daran — er hängt daran, dass die Indizes **existieren**.
>
> Der Indexname `ProejctIDIDX` ist im Altsystem so geschrieben — Buchstabendreher inbegriffen —
> und er steht auf `ProcessID`, nicht auf `ProjectID`. Beides ist Bestand, nicht Tippfehler
> dieser Datei.

**Das ist ein anderer Vorgang als bei `MessageAction`** — und der Unterschied ist wichtig genug,
um ihn auszuschreiben. Bei `MessageAction` war die Spaltenliste übernommen und bis M14
(07.08.2026) **nie gemessen** worden. Die Indizes von `Message` dagegen **sind** erhoben, und
zwar am selben Tag wie die Spalten: [`messungen-schritt4.md`](messungen-schritt4.md) **M1** vom
01.08.2026 trägt die Überschrift „Spaltennamen **und Indizes**" und führt alle acht mit denselben
Kardinalitäten wie oben. Regel L8 führt `Message` also zu Recht als „erhoben".

**Was fehlt, ist nicht die Messung, sondern ihre Übernahme.** Die Indexliste in
`PROJEKTBESCHREIBUNG.md` §3.2 ist seit dem 01.08.2026 durch die eigene Erhebung des Projekts
widerlegt und trotzdem nie nachgezogen worden. Der Auftrag hat sich auf sie berufen, weil sie in
der verbindlichen Datei steht — nicht, weil niemand nachgesehen hätte.

### V3 — Die Zahl „733 Prozesse für NEXANS"

**Der Auftrag irrt. Die Zahl ist nicht ungedeckt, sie ist seit dem 06.08.2026 gemessen.**

*Gemessen war* (hier, 20.08.2026): **733**.

*Behauptet wird* im Auftrag: „In der Sitzung vom 19.08.2026 ist zweimal ‚733 Prozesse für NEXANS'
geschrieben worden, **ohne Quelle**. […] Sie wird in M73a erhoben und bis dahin nicht verwendet."

Die Quelle existiert und ist an elf Stellen im Repository belegt. Primärfundstelle:
[`messungen-schritt4.md`](messungen-schritt4.md) Z. 1583, Messung **L12**, erhoben am 06.08.2026
für `GET /api/prozesse` — dort in einer Tabelle „Prozesse je Mandant" für **alle zehn** Mandanten.
Weiter in [`prozessauswahl.md`](prozessauswahl.md) Z. 70, 77, 204, 242, 250, 258, 291, in
[`messungen-schritt7.md`](messungen-schritt7.md) Z. 1441, in [`nachrichtenliste.md`](nachrichtenliste.md)
und sogar als Kommentar in `frontend/src/features/nachrichten/components/prozess-filter.tsx` Z. 89.

**Die Vorsichtsmaßnahme war trotzdem richtig** — sie hat nur das Falsche gefunden. Die
Sparringsrunde hat die Zahl aus dem Gedächtnis wiederholt statt sie zu belegen; dass sie zufällig
stimmte, ändert daran nichts. Der Nachweis ist jetzt geführt, und die Zahl ist zwei Wochen nach
L12 **unverändert**.

---

## M74a — Der Prozessbestand je Mandant *(beantwortet V3)*

**Frage.** Die Grundzahl, an der Pflegeaufwand, Fortschrittsanzeige und die Frage nach einem
Filter hängen.

**Statement.** `scripts/messung-schritt9/m74a-prozessbestand.sql`

```sql
SELECT pm.MandantID,
       COUNT(DISTINCT p.ProcessID) AS prozesse,
       COUNT(DISTINCT p.ProjectID) AS projekte
FROM Process p
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
GROUP BY pm.MandantID
ORDER BY prozesse DESC;
```

### Ergebnis

| Mandant | Prozesse | Projekte |
|---|---:|---:|
| `NEXANS` | **733** | 17 |
| `VOTG` | **390** | 39 |
| `IBIS` | 192 | 46 |
| `IBISGUS` | 89 | 19 |
| `ZAST` | 35 | 4 |
| `SUTTONS` | 17 | 1 |
| `NXHBE` | 17 | 2 |
| `EDITIONLINGERI` | 9 | 3 |
| `WOC` | 4 | 1 |
| `SYSTEM` | 4 | 2 |
| **Summe** | **1.490** | **134** |

Gegenproben derselben Sitzung:

| Gegenprobe | Ergebnis |
|---|---:|
| Prozesse ohne erreichbaren Mandanten | **13** |
| Projekte ohne Mandantenzeile | **6** |
| Projekte an mehr als einem Mandanten | **0** |
| Zeilen im Join / verschiedene `ProcessID` darin | 1.490 / **1.490** |

**Die Bilanz geht auf, in beide Richtungen:** 1.490 + 13 = **1.503** Prozesse (L12, M44);
134 + 6 = **140** Projekte (M44). Die Zahlen sind zwei Wochen nach der letzten Zählung unverändert.

**Laufzeiten.** Aufwärmlauf 4,369 ms, gemessen **2,979 ms**. Gegenprobe 4,625 ms, Summenprobe
2,494 ms, Mehrfachprojekte 0,657 ms, Projekte ohne Mandant 0,832 ms.

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| Summe = 1.503 → `ProcessID` allein trägt den Katalogschlüssel | **fast** — die Summe ist **1.490**, die Differenz sind die 13 herrenlosen Prozesse | **Der Schlüssel trägt.** Entscheidend ist nicht die Summe, sondern `projekte_mit_mehreren_mandanten = 0` und 1.490 Zeilen auf 1.490 verschiedene `ProcessID`. `ProcessID` allein genügt als Katalogschlüssel |
| Summe > 1.503 → zwei Mandanten teilen sich eine Katalogzeile | **nein** | Der Fall tritt nicht ein. Eine Katalogzeile gehört immer genau einem Mandanten |
| `prozesse_ohne_mandant > 0` → nie kuratierbar; **größer als die sechs aus A8 → A8 überholt, anhalten** | **13 > 6** — aber die Zahlen zählen Verschiedenes | **A8 ist nicht überholt, und es wird nicht angehalten.** Siehe den Kasten unten |
| Ein Mandant über 500 Prozessen → Filter „nur offene" ist Pflicht | **ja, `NEXANS` mit 733** | Der Filter ist Pflicht |
| Alle unter 200 → Filter ist Beiwerk | **nein** | — |

> **Zur Abbruchbedingung, ausdrücklich.** Die vorregistrierte Zeile vergleicht
> „`prozesse_ohne_mandant`" mit „den sechs aus A8". **A8 zählt Projekte, diese Abfrage zählt
> Prozesse.** 6 Projekte ohne Mandantenzeile tragen zusammen 13 Prozesse — das ist dieselbe
> Aussage in einer anderen Einheit, kein Widerspruch. Die Abbruchbedingung war falsch
> formuliert, nicht ausgelöst. Der eigentliche A8-Test ist die Nachrichtenzahl, und er steht in
> M74b (Teil D): **die 13 Prozesse tragen null Nachrichten.** A8 gilt unverändert.

**Befund.** Der Bestand ist **stark ungleich verteilt**: `NEXANS` allein hält 49,2 % aller
zugeordneten Prozesse, die beiden größten Mandanten zusammen 75,4 %. Sechs der zehn Mandanten
liegen unter 40 Prozessen. Eine Pflegeliste muss also **beides** können — 733 Zeilen bändigen und
bei 4 Zeilen nicht albern wirken.

---

## M74b — Wie viele Prozesse tragen überhaupt Nachrichten?

**Frage.** Tote Prozesse blähen die Pflegeliste und verhindern, dass der Fortschritt je 100 %
erreicht.

### Vorprüfung: der Plan, bevor die Abfrage läuft

```
EXPLAIN SELECT ProcessID, COUNT(*) FROM Message GROUP BY ProcessID;
```

| id | select_type | table | type | key | key_len | rows | Extra |
|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | `Message` | `index` | **`ProejctIDIDX`** | 147 | 3.560.486 | **`Using index`** |

`Using index` — die Bedingung des Auftrags ist erfüllt, die Abfrage wird gefahren. Bedient wird
sie von genau dem Index, den der Auftrag für nicht existent hielt.

### Ergebnis

| Kennzahl | Wert |
|---|---:|
| Prozesse **mit** mindestens einer Nachricht | **738** |
| Prozesse **ohne** jede Nachricht | **765** |
| Summe | **1.503** ✓ |
| Anteil ohne Nachricht | **50,90 %** |
| Nachrichten gesamt | 3.341.519 |
| kleinste / größte Zahl je Prozess | 1 / **1.472.788** |
| `ProcessID` in `Message` ohne Zeile in `Process` | **0** |

Je Mandant:

| Mandant | Prozesse | mit Nachrichten | ohne | Anteil ohne |
|---|---:|---:|---:|---:|
| `NEXANS` | 733 | 516 | 217 | 29,60 % |
| `VOTG` | 390 | 40 | **350** | **89,74 %** |
| `IBIS` | 192 | 113 | 79 | 41,15 % |
| `IBISGUS` | 89 | 23 | 66 | 74,16 % |
| `ZAST` | 35 | 24 | 11 | 31,43 % |
| `SUTTONS` | 17 | 17 | 0 | **0,00 %** |
| `NXHBE` | 17 | 2 | 15 | 88,24 % |
| `EDITIONLINGERI` | 9 | 0 | 9 | **100,00 %** |
| `WOC` | 4 | 2 | 2 | 50,00 % |
| `SYSTEM` | 4 | 1 | 3 | 75,00 % |

**(D) Die A8-Probe.** Die 13 Prozesse ohne erreichbaren Mandanten: **0 davon tragen eine
Nachricht.**

**Laufzeiten.** Voller Durchlauf **1.395,6 ms**. `NOT EXISTS` über alle 1.503 Prozesse
**5.651,5 ms**. Auswertung je Mandant 122,0 ms. A8-Probe 5,3 ms. Verweistreue 7,7 ms.
Sitzung gesamt 7,2 s.

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| `Using index`, Laufzeit unter 60 s → einmalige Erhebung tragbar | **ja** — 1,4 s | Tragbar. **Wird niemals Anwendungscode**, Regel L2 bleibt unberührt |
| kein `Using index` oder darüber → anhalten | nein | — |
| **über 30 % ohne Nachricht → Kennzeichnung „trägt keine Nachrichten" nötig** | **ja, deutlich: 50,90 %** | Die Pflegeliste **braucht** die Kennzeichnung |
| unter 5 % → nicht der Rede wert | nein | — |

**Befund 1 — die Hälfte des Katalogs ist tot.** 765 von 1.503 Prozessen haben in einem Bestand,
der bis zum 08.07.2026 reicht, **nie** eine Nachricht getragen. Ohne Kennzeichnung kuratiert
jemand 765 Karteileichen, und das Abnahmekriterium „jeder Prozess ist zugeordnet oder als
gepflegt-ohne-Partner gekennzeichnet" verlangt genau diese Arbeit.

**Befund 2 — und das ist der schärfere.** Der Anteil ist **nicht gleichmäßig**: `VOTG` hat
89,74 % tote Prozesse, `SUTTONS` **null**. Für `VOTG` heißt das: von 390 Zeilen der Pflegeliste
sind **40** je in Gebrauch gewesen. Eine Pflegeliste ohne diesen Filter stellt dem Nutzer von
`VOTG` die Aufgabe, 350 Prozesse zuzuordnen, die nichts tun.

**Befund 3 — ein einzelner Prozess hält 44,08 % aller Nachrichten** (1.472.788 von 3.341.519).
Für Schritt 10 ist das die wichtigere Zahl als jede Prozesszahl: eine Verteilung „nach Partner"
wird von diesem einen Prozess dominiert, egal wie gut der Rest kuratiert ist.

**Befund 4 — Verweistreue ist vollständig.** Alle 738 `ProcessID` aus `Message` haben eine Zeile
in `Process`. Es gibt keine Nachricht, deren Prozess unbekannt wäre.

> **Kreuzprobe zu A8, ungeplant.** `SUM(n)` über alle Gruppen ergibt **3.341.519** — exakt die
> Gesamtzahl, die [`annahmen-korrekturen.md`](annahmen-korrekturen.md) Z. 65–75 am 01.08.2026 für
> den A8-Nachweis herangezogen hat. Die Zahl ist damit unabhängig, mit anderer Abfrage und zwei
> Wochen später, ein zweites Mal bestätigt.

---

## M75 — Die Gestalt der Prozess- und Projektnamen, je Mandant

**Frage.** Die Trefferquote der Heuristik, **bevor** die Heuristik existiert. Der Punkt der
Messung ist die Aufschlüsselung je Mandant: „Ein Gesamtdurchschnitt würde vom größten Mandanten
bestimmt und verstecken, dass ein kleiner gar nichts bekommt."

### Ergebnis — Nummernpräfix `^[0-9]+_` in der `ProcessID`

| Mandant | mit Präfix | ohne | Prozesse | Anteil |
|---|---:|---:|---:|---:|
| `NEXANS` | 733 | 0 | 733 | **100,00 %** |
| `VOTG` | 390 | 0 | 390 | **100,00 %** |
| `SUTTONS` | 17 | 0 | 17 | **100,00 %** |
| `IBIS` | 0 | 192 | 192 | **0,00 %** |
| `IBISGUS` | 0 | 89 | 89 | **0,00 %** |
| `ZAST` | 0 | 35 | 35 | **0,00 %** |
| `NXHBE` | 0 | 17 | 17 | **0,00 %** |
| `EDITIONLINGERI` | 0 | 9 | 9 | **0,00 %** |
| `WOC` | 0 | 4 | 4 | **0,00 %** |
| `SYSTEM` | 0 | 4 | 4 | **0,00 %** |

**Das Ergebnis ist vollkommen zweigeteilt. Es gibt keinen Zwischenwert.** Drei Mandanten bei
100 %, sieben bei 0 %.

Über alle Prozesse gerechnet: **1.140 von 1.490 = 76,51 %** tragen den Präfix. **Genau diese
Zahl hätte die Messung ruiniert** — sie sagt „drei Viertel" und verschweigt, dass sieben von zehn
Mandanten leer ausgehen.

### Unterstriche in der `ProcessID`

| Mandant | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---:|---:|---:|---:|---:|---:|
| `NEXANS` | — | 6 | 9 | **500** | 168 | 50 |
| `VOTG` | — | 4 | 118 | **260** | 8 | — |
| `IBIS` | **188** | 4 | — | — | — | — |
| `IBISGUS` | **88** | 1 | — | — | — | — |
| `ZAST` | — | **35** | — | — | — | — |
| `SUTTONS` | — | — | — | 8 | 9 | — |
| `NXHBE` | — | 3 | **14** | — | — | — |
| `EDITIONLINGERI` | **9** | — | — | — | — | — |
| `WOC` | — | 1 | 2 | 1 | — | — |
| `SYSTEM` | **4** | — | — | — | — | — |

### `ProcessName` — die Spalte, die der Auftrag nicht kennt *(Erweiterung)*

| Mandant | Name mit Nummernpräfix | Name mit `_` | Name = `ProcessID` | Prozesse |
|---|---:|---:|---:|---:|
| `NEXANS` | **0** | 446 | 0 | 733 |
| `VOTG` | **0** | 0 | 0 | 390 |
| `IBIS` | **0** | 0 | 0 | 192 |
| `NXHBE` | **0** | 14 | 0 | 17 |
| *(übrige)* | **0** | 0 | 1 | — |

**`ProcessName` trägt in keinem einzigen Fall einen Nummernpräfix** und ist praktisch nie mit
`ProcessID` identisch. Es ist eine **eigene, prosaischere Bezeichnung** — kein Ersatzschlüssel und
keine zweite Chance für dieselbe Heuristik.

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| Präfix bei allen Mandanten über 90 % → gemeinsamer erster Schritt | **nein** | Es gibt **keinen** gemeinsamen ersten Schritt |
| **Ein Mandant unter 50 % → für ihn liefert die Heuristik nichts, und das gehört so in die Oberfläche** | **ja — aber nicht einer, sondern sieben** | Für `IBIS`, `IBISGUS`, `ZAST`, `NXHBE`, `EDITIONLINGERI`, `WOC`, `SYSTEM` liefert die Präfix-Zerlegung **nichts**. Die Oberfläche muss das sagen, nicht ein leeres Feld zeigen |
| Sehr verschiedene Unterstrich-Verteilungen → §4.4 bestätigt, Heuristik füllt nur, was sie sicher zerlegt | **ja, sehr deutlich** | §4.4 ist bestätigt. Die Heuristik lässt den Rest leer (Q4) |
| Durchgängig gleiche Verteilung → §4.4 zu streng, **Korrektur an der verbindlichen Datei** | **nein** | Keine Korrektur nötig. §4.4 ist an dieser Stelle eher **zu vorsichtig als zu streng** — es sind mehr Konventionen als dort genannt |

**Befund.** Die Aufschlüsselung je Mandant war die richtige Entscheidung, und sie hat das
Gegenteil des erhofften Ergebnisses geliefert. Die Heuristik ist **kein allgemeines Verfahren mit
Ausreißern**, sondern ein Verfahren, das für **drei** Mandanten gilt und für **sieben** nicht.

---

## M76 — Trägt der Partner im Projekt oder im Prozess?

**Frage.** `PROJEKTBESCHREIBUNG.md` §4.4 behauptet: NEXANS → Prozess trägt Partner;
VTG/Suttons → Projekt trägt Partner. **Diese Aussage ist gelesen und nirgends belegt.** Sie
bestimmt, wo die Massenzuordnung nach Projekt überhaupt greift.

**Diese Messung liefert Anschauungsmaterial, keine Kennzahl** (L10). 140 Projekte sind wenig
genug, um sie vollständig anzusehen — das ist geschehen; hier steht die Gestalt, nicht die Liste
(G1, siehe oben).

### Ergebnis, je Mandant

**`NEXANS` — 17 Projekte, 733 Prozesse.** Die Projektkennung trägt **Richtung und Rolle**, nie
einen Partner: `<Nr>_<Rolle><Richtung>`, mit `Kunden`/`Lieferanten` × `Eingehend`/`Ausgehend` und
einer Bereichsvorsilbe. Im Repository bereits abgedruckt: `300_KundenEingehend`. Die vier
größten Projekte tragen 161, 145, 109 und 78 Prozesse; dazu ein `900_SonstigeProzesse` mit 11.

Der **Partner steckt in der Prozesskennung**, Gestalt `<Nr>_<PARTNER>_<Belegart>_<Norm>` — im
Repository abgedruckt: `40000_AMG_LAB_VDA`. Innerhalb **eines** Projekts stehen durchgehend
**verschiedene** Partner nebeneinander.

**`VOTG` — 39 Projekte, 390 Prozesse.** Zweigeteilt, und die Teilung ist der eigentliche Befund:

| Projektform | Projekte | Prozesse | trägt |
|---|---:|---:|---|
| `100_VTG_<PARTNER>` | 36 | **157** | **den Partner** — je Projekt genau einen |
| `110_VTG_SalesInvoice` | 1 | **226** | eine **Belegart**, keinen Partner |
| `110_VTG_SONSTIGES`, `110_VTG_Belegarchivierung` | 2 | 7 | nichts |

Im Repository abgedruckt: `100_VTG_BAYER`. Die Prozesse unter `100_VTG_<PARTNER>` wiederholen
den Partnernamen im eigenen Namen (`<Nr>_<PARTNER>_<Nachrichtenart>`), ebenso die unter
`110_VTG_SalesInvoice`, dort in der Form `19900_<PARTNER>_<Nummer>_INVOICE` bzw. `…_INVOICEATT`.

**`SUTTONS` — 1 Projekt, 17 Prozesse.** Ein einziges Projekt der Form `100_SUTTONS_<PARTNER>`; alle 17
Prozesse hängen daran, Gestalt `<Nr>_<Präfix>_<Vorgang>`, wobei der Präfix bei allen 17 derselbe ist. Der Prozessname trägt **keinen Partner**,
sondern einen Vorgang (Buchung, Status).

**`IBIS`/`IBISGUS` — 46 bzw. 19 Projekte.** Dritte Konvention, die §4.4 gar nicht kennt.
Projektkennungen tragen **Belegart + Richtung + Land** (`OrdersVerarbeitungEingehendNL`,
`RechnungsVerarbeitungAusgehendDE`) **oder** einen Partner mit Vorsilbe `Lager<PARTNER>`.
Prozesskennungen tragen Richtung **und** Partner — aber in **CamelCase ohne Trennzeichen**
(`AuslagerungAusgehend<PARTNER>`, `InternRoutingEingehend<PARTNER>`).

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| Bei `VOTG`/`SUTTONS` wiederholt sich ein Namensteil des Projekts nicht in den Prozessen → §4.4 bestätigt, Massenzuordnung nach Projekt ist **der** Hebel | **teilweise** | Für die **36** `100_VTG_<PARTNER>`-Projekte ist die Massenzuordnung der Hebel — sie deckt aber nur **157 von 390** Prozessen ab. Für `SUTTONS` ist sie **wirkungslos**: ein Projekt, ein Wert |
| Bei `NEXANS` tragen die Prozesse eines Projekts **verschiedene** Partner → §4.4 bestätigt; für den Partner unbrauchbar, für die **Richtung** genau richtig | **ja, vollständig** | Bestätigt. Bei `NEXANS` ist die Massenzuordnung **je Projekt** für die **Richtung** das richtige Werkzeug und für den Partner das falsche |
| Ein Projekt mit sehr vielen Prozessen bei `NEXANS` → Vorschau muss die Zahl betroffener Zeilen zeigen | **ja — und ärger bei `VOTG`** | Größtes Projekt bei `NEXANS`: 161 Prozesse. Bei `VOTG`: **226**. Die Vorschau ist Pflicht |
| Das Bild passt zu keiner der beiden Beschreibungen → **anhalten**, §4.4 ist falsch | **nein — es passt, aber es ist unvollständig** | Nicht anhalten. §4.4 beschreibt zwei von **mindestens vier** Konventionen richtig |

**Befund 1 — §4.4 ist bestätigt, wo es etwas sagt, und lückenhaft, wo es schweigt.** Für `NEXANS`
und für die 36 Partnerprojekte von `VOTG` stimmt die Beschreibung wörtlich. Sie sagt nichts über
`IBIS`/`IBISGUS` (dritte Konvention, 281 Prozesse) und nichts über `110_VTG_SalesInvoice`.

**Befund 2 — die größte Einzellücke von §4.4.** Der Satz „VTG / Suttons: Projekt trägt
Geschäftsbereich und Partner" gilt für **157 von 390** `VOTG`-Prozessen (40,26 %). Die Mehrheit —
226 Prozesse, 57,95 % — hängt unter **einem** Projekt, das eine Belegart benennt. Wer die
Massenzuordnung nach Projekt für `VOTG` als „den Hebel" plant, erreicht damit **weniger als die
Hälfte** des Bestands.

**Befund 3 — für `SUTTONS` gibt es nichts zu massenzuordnen.** Ein Projekt, 17 Prozesse. Der
kleine Mandant der Regel L15 ist hier zugleich der Mandant, für den das geplante Werkzeug
strukturell wirkungslos ist.

---

## M77 — Steckt die Richtung im `SOSName`?

**Frage.** Das zweite kuratierte Feld. §4.4 sagt, bei VTG/Suttons stehe die Richtung **nur** dort.
**Das Vokabular wird nicht geraten, sondern erhoben** — deshalb die Verteilung der ersten Wörter
statt einer `LIKE '%ingehend%'`-Abfrage.

### Ergebnis — das erste Wort des `SOSName`

`SOS` hat **1.818** Zeilen, davon **1.804** an Prozessen mit erreichbarem Mandanten.

**`VOTG` — 398 SOS. Hier, und nur hier, trägt das erste Wort die Richtung:**

| erstes Wort | SOS |
|---|---:|
| `Ausgehender` | 142 |
| `Ausgehende` | 133 |
| `Eingehender` | 43 |
| `Eingehende` | 35 |
| `Ausgehendes` | 4 |
| `Eingehendes` | 1 |
| **Summe richtungstragend** | **358 von 398 = 89,95 %** |

Der gemessene Wortschatz ist damit **zwei Stämme in sechs Beugungsformen** — `Eingehend*` und
`Ausgehend*`, dekliniert nach dem folgenden Substantiv. Der Rest sind Partnernamen (drei
verschiedene, zusammen 25 SOS — hier als Gestalt, nicht als Aufzählung, G1), technische Begriffe (`Router`, `Monitor`, `Protokoll`, `Check`) und je einmal
`Undefined`, `Unkonfigurierter`, `Unkonfigurierte`.

**Alle anderen Mandanten — das erste Wort ist der Partner oder die Belegart, nie die Richtung:**

| Mandant | SOS | `Eingehend*`/`Eingang*` | `Ausgehend*`/`Ausgang*` | erstes Wort ist typischerweise |
|---|---:|---:|---:|---|
| `NEXANS` | 1.001 | 1 | 0 | Belegart (`Lieferabruf` 423, `Lieferschein` 254, `Gutschrift` 19) oder Partner |
| `VOTG` | 398 | **79** | **279** | **die Richtung** |
| `IBIS` | 196 | 1 | 0 | der Partner — rund 60 verschiedene Handelsketten |
| `IBISGUS` | 90 | 0 | 0 | der Partner |
| `ZAST` | 61 | 2 | 0 | der Partner (Kassen und Abrechnungsdienstleister) |
| `NXHBE` | 23 | 0 | 0 | Belegart |
| `SUTTONS` | 18 | 0 | 0 | Vorgang, **englisch** (`Retrieve` 8, `Outgoing` 3, `Booking…`) |
| `EDITIONLINGERI` | 9 | 0 | 0 | Partner oder Vorgang |
| `WOC` | 4 | 0 | 0 | Vorgang |
| `SYSTEM` | 4 | 0 | 0 | technisch |

> **Die beiden Spalten sind absichtlich weiter gefasst als der Wortstamm.** Gemessen ist
> `LIKE 'Eingehend%' OR LIKE 'Eingang%'`. Deshalb ist zu unterscheiden: Der eine `IBIS`-Treffer
> ist ein echtes `Eingehendes`; die **eine** Zeile bei `NEXANS` und die **zwei** bei `ZAST` sind
> `Eingangsbestätigung` bzw. `Eingangsdatensplit` — **kein** `SOSName` dieser beiden Mandanten
> beginnt mit `Eingehend`. Die Prozessdeckung darunter ist mit dem engeren Muster gerechnet.

Auf Prozesse statt SOS gerechnet:

| Mandant | Prozesse | mit Richtungshinweis | Anteil |
|---|---:|---:|---:|
| `VOTG` | 390 | **352** | **90,26 %** |
| `IBIS` | 192 | 1 | 0,52 % |
| *alle übrigen* | 908 | **0** | 0,00 % |

### Wie viele `SOS` hängen an einem Prozess?

| `SOS` je Prozess | Prozesse |
|---:|---:|
| 1 | **1.255** |
| 2 | 237 |
| 3 | 1 |
| 4 | 2 |
| 6 | 3 |
| 10 | 1 |
| 11 | 1 |
| 14 | 1 |
| 25 | 1 |

**247 von 1.502 Prozessen (16,44 %) haben mehr als einen `SOS`.** Genau **ein** Prozess hat gar
keinen.

### Widersprechen sich mehrere `SOS` eines Prozesses? *(Q4)*

| Mandant | Prozesse mit Richtungshinweis | **widersprüchlich** | eindeutig eingehend | eindeutig ausgehend |
|---|---:|---:|---:|---:|
| `VOTG` | 352 | **0** | 78 | 274 |
| `IBIS` | 1 | **0** | 1 | 0 |

**Kein einziger Prozess trägt gleichzeitig einen `Eingehend*`- und einen `Ausgehend*`-`SOS`.**

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| Wenige erste Wörter decken den Großteil und sind richtungstragend → Heuristik baubar, Wortschatz steht **gemessen** fest | **ja — aber nur für `VOTG`** | Für `VOTG` ist die Heuristik baubar, und der Wortschatz ist gemessen: `Eingehend*` / `Ausgehend*`, sechs Formen |
| Viele verschiedene erste Wörter ohne Richtungsbezug → Richtung wird **nicht** hergeleitet, sondern von Hand je Projekt gesetzt | **ja — für die neun anderen Mandanten** | Für alle außer `VOTG` wird die Richtung **von Hand** gesetzt |
| Prozesse mit mehr als einem `SOS` in nennenswerter Zahl → Herleitung kann **widersprüchliche** Auskünfte bekommen; dann kein Vorschlag statt eines gewürfelten | **Zahl ja (16,44 %), Widerspruch nein (0)** | Die Sorge ist gemessen **unbegründet**. Die Q4-Regel bleibt trotzdem stehen — sie kostet nichts und die Produktion kann anders aussehen |
| `anzahl_sos` durchgängig 1 → §3.2 vorsichtiger formuliert als nötig; **vermerken, nicht ändern** | **nein** — 247 Prozesse mit mehr als einem | §3.2 ist an dieser Stelle **richtig**. „Meist 1:1, gelegentlich 1:n" beschreibt 83,6 % / 16,4 % zutreffend |

**Befund 1 — §4.4 stimmt für VTG und nicht für Suttons.** Der Satz lautet „VTG / Suttons: […] die
Richtung steht nur im `SOSName`". Für `VOTG` ist das gemessen richtig (90,26 % der Prozesse). Für
`SUTTONS` ist es **falsch**: von 18 `SOS` trägt kein einziger ein deutsches Richtungswort; das
Vokabular ist englisch, und `Outgoing` erscheint dreimal. **Der Satz muss geteilt werden.**

**Befund 2 — die Richtungsherleitung ist ein Werkzeug für genau einen Mandanten.** Sie erreicht
**353** Prozesse, davon **352 bei `VOTG`** und einen einzigen bei `IBIS`. Das sind 23,7 % des
Gesamtbestands — und 90,26 % desjenigen Mandanten, bei dem sie greift. Der `IBIS`-Treffer ist ein
echtes `Eingehendes` und kein Zählfehler; er ist nur zu wenig, um für diesen Mandanten ein
Verfahren darauf zu gründen.

**Befund 3 — wo sie greift, ist sie eindeutig.** Null Widersprüche bei 352 Prozessen, davon
**sechs** mit mehr als einem `SOS`. Ein Vorschlag aus dieser Quelle muss nicht gegen sich selbst
geprüft werden.

> *Nachgemessen.* `VOTG` hält 398 `SOS` auf 390 Prozessen; **acht** Prozesse tragen mehr als einen,
> **sechs** davon sind richtungstragend. Die Zahl stammt aus einem Nachlauf
> (`m76d-nachlauf.sql`, 17,7 ms), nicht aus der Hauptsitzung.

**Befund 4 — der `SOSName` ist die beste Partnerquelle, die diese Runde gefunden hat.** Nicht
für die Richtung, sondern für den **Partner**: Bei `IBIS`, `IBISGUS` und `ZAST` steht der Partner
als **erstes Wort, durch ein Leerzeichen abgetrennt** — sauberer als in jeder Prozesskennung. Das
war nicht die Frage dieser Messung und ist deshalb hier nur vermerkt, nicht ausgewertet.

---

## M78 — Wie viele Auffangprozesse gibt es?

**Frage.** Der Plantext nennt „Sonderbehandlung von `00001_Undefined`" im **Singular**. Ob es
einer ist, ist nicht belegt. Mit zwei Pflegezuständen hängt daran, ob „gepflegt, ohne Partner"
genügt.

### Ergebnis

Der beauftragte Filter `ProcessID REGEXP 'ndefined' OR ProcessID REGEXP '^0+[_]'` liefert
**sechs** Treffer über **zwei** Mandanten:

| Mandant | `ProcessID` | Projekt | Nachrichten |
|---|---|---|---:|
| `VOTG` | `00000_NONBASF_ARRECHNUNG` | `110_VTG_SONSTIGES` | **1.602** |
| `SYSTEM` | `Undefined` | `Undefined` | **151** |
| `VOTG` | **`00001_Undefined`** | `110_VTG_SONSTIGES` | **3** |
| `VOTG` | `00000_<Komponente>XMLSplitter` | `110_VTG_SONSTIGES` | 0 |
| `VOTG` | `00000_<PARTNER>Router` | `110_VTG_SONSTIGES` | 0 |
| `VOTG` | `00000_<PARTNER>Router` | `110_VTG_SONSTIGES` | 0 |

**Der Filter fängt zu viel.** Vier der sechs Treffer sind **keine** Auffangprozesse, sondern
regulär benannte Prozesse, deren Nummernpräfix zufällig aus Nullen besteht — `00000_` sortiert
sie nur an den Anfang. `00000_NONBASF_ARRECHNUNG` ist eine Ausgangsrechnung für einen benannten
Partnerkreis; die beiden `…Router` sind Weichen; der `…XMLSplitter` ist ein
Splitter. Der Partnername `NONBASF` steht bereits in §4.4 als Beispiel für die
Granularitätsfrage.

**Echte Auffangprozesse sind zwei:** `00001_Undefined` (`VOTG`, **3** Nachrichten) und
`Undefined` (`SYSTEM`, 151 Nachrichten — und `SYSTEM` ist ein technischer Mandant, kein Kunde).

Weiter gefasst — `Undefined`, `Sonstige`, `Router` oder `Unkonfiguriert` in `ProcessID` **oder**
`ProjectID`:

| Mandant | `NEXANS` | `VOTG` | `IBIS` | `NXHBE` | `SYSTEM` | `IBISGUS` | `WOC` | **Summe** |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Treffer | 11 | 5 | 4 | 3 | 1 | 1 | 1 | **26** |

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| **Genau ein Treffer, wenige Nachrichten** → Plantext stimmt; „gepflegt, ohne Partner" genügt | **im Kern ja** — genau ein Auffangprozess bei einem Kundenmandanten, mit **3** Nachrichten | Der Plantext stimmt. Der Singular ist richtig, sobald man den Filter von seinen vier Fehltreffern befreit |
| Ein Auffangprozess **je Mandant** → tragbar, Heuristik erkennt und legt gleich als gepflegt an | **nein** — 8 von 10 Mandanten haben gar keinen | — |
| **Viele Treffer oder viele Nachrichten darauf** → kein Randfall; eigener Zustand nötig, Entscheidung „zwei Zustände" **wieder aufmachen** | **nein** | **Die Entscheidung vom 19.08.2026 bleibt zu.** Zwei Pflegezustände genügen |

> **Warum die 1.602 Nachrichten die Abbruchbedingung nicht auslösen.** Sie hängen an
> `00000_NONBASF_ARRECHNUNG`, und das ist ein benannter Prozess, kein Auffangprozess. Hätte man
> den Filter beim Wort genommen statt die Treffer anzusehen, hätte diese Zahl die Entscheidung
> „zwei Zustände" fälschlich wieder aufgemacht. **Das ist der Grund, warum M76 und M78 als
> Anschauungsmaterial beauftragt waren und nicht als Kennzahl** (L10).

**Befund.** „Sonderbehandlung von `00001_Undefined`" bleibt als Plansatz richtig. Der **Filter**
dafür darf aber nicht `^0+_` sein — er müsste auf `Undefined` allein stellen, sonst behandelt die
Oberfläche vier reguläre Prozesse als Auffangbecken.

---

## M79 — Kostet die Pflegeliste etwas? *(Regel L7)* — **entfällt**

**Die Vorabprüfung des Auftrags greift.** Sie lautet: „Deckt eine Messung aus Schritt 4 dieselbe
Abfrage bereits ab? Wenn ja, **entfällt M79** und es wird darauf verwiesen. Was gemessen ist, wird
nicht zweimal gemessen."

**Sie ist abgedeckt, und zwar in der teureren Fassung.** [`messungen-schritt4.md`](messungen-schritt4.md),
Messung **L12**, erhoben am 06.08.2026 für `GET /api/prozesse`:

```sql
SELECT p.ProcessID, p.ProcessName, pr.ProjectName
FROM Process p
JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
LEFT JOIN Project pr   ON pr.ProjectID = p.ProjectID
WHERE pm.MandantID = ?
ORDER BY pr.ProjectName, p.ProcessName;
```

| Mandant | Zeilen | Laufzeit (beste von fünf) | Aufwärmlauf |
|---|---:|---:|---:|
| `NEXANS` | 733 | **4,225 ms** | 4,705 ms |
| `SUTTONS` | 17 | **0,606 ms** | 0,618 ms |

`EXPLAIN`: `ProjectMandant` über `ProjectMandant_Mandant_idx` (`ref`, `Using where; Using index;
Using temporary; Using filesort`), `Process` über `Process_ProjectFK` (`ref`), `Project` über
`PRIMARY` (`eq_ref`).

**Warum das genügt — und sogar mehr als genügt:**

1. **Die Sortierung ist die richtige.** Die Sparringsrunde hat auf „**Projekt + Name**" entschieden.
   L12 misst genau `ORDER BY pr.ProjectName, p.ProcessName`. Die im Auftrag abgedruckte Abfrage
   sortiert dagegen nach `p.ProjectID, p.ProcessID` — das ist **nicht** die beschlossene
   Sortierung. Gemessen ist die beschlossene.
2. **L12 ist der Worst Case.** Es liest eine Spalte mehr und hat einen Join mehr (`LEFT JOIN
   Project`) als die geplante Abfrage. Die Pflegeliste kann nur billiger werden.
3. **Regel L15 ist erfüllt.** L12 fährt zwei Mandanten, den größten (733) und einen kleinen (17).

**Ergebnis gegen die vorregistrierte Deutung:**

| Vorab benannt | eingetreten? |
|---|---|
| Beide unter 50 ms, kein `filesort` über die große Menge | **Laufzeit ja** (4,2 / 0,6 ms), **`filesort` nein** — er tritt auf |
| **`filesort` bei mehreren hundert Zeilen → tragbar, vermerken, nicht optimieren** | **ja.** L12 hält dazu fest: „**`Using temporary; Using filesort` ist hier kein Befund.** […] Bei 733 Zeilen kostet das vier Millisekunden" |
| Deutlicher Unterschied zwischen den beiden Mandanten → L15 hat zugeschlagen | **nein** — 4,225 ms zu 0,606 ms ist Faktor 7 bei Faktor 43 der Zeilenzahl. Das ist Mengenverhalten, keine Planabweichung |
| Über 500 ms bei einem Mandanten → **anhalten**, Paginierung nötig | **nein** |

**Keine Paginierung nötig.** Die Pflegeliste kostet beim größten Mandanten rund vier Millisekunden
in der Datenbank.

> **Die eine verbleibende Lücke, benannt statt verschwiegen.** Nicht gemessen ist der Join gegen
> `process_catalog`, denn die Tabelle existiert nicht. M79 sagt über den Datenbankteil der
> Pflegeliste alles, was ohne sie zu sagen ist — und nichts darüber hinaus.

---

## Laufzeiten im Überblick

| Messung | Statement | Laufzeit | Aufwärmlauf |
|---|---|---:|---:|
| V2 | Spaltenerhebung `information_schema.COLUMNS` | 6,1 ms | — |
| V2 | Indexerhebung `information_schema.STATISTICS` | 4,8 ms | — |
| **M74a** | Prozesse/Projekte je Mandant | **2,979 ms** | 4,369 ms |
| M74a | Gegenprobe Prozesse ohne Mandant | 4,625 ms | — |
| M74a | Summenprobe | 2,494 ms | — |
| M74a | Projekte an mehreren Mandanten | 0,657 ms | — |
| M74a | Projekte ohne Mandant | 0,832 ms | — |
| **M74b** | voller Durchlauf `GROUP BY ProcessID` über `Message` | **1.395,6 ms** | — |
| **M74b** | `NOT EXISTS` über alle 1.503 Prozesse | **5.651,5 ms** | — |
| M74b | Auswertung je Mandant | 122,0 ms | — |
| M74b | A8-Probe (13 herrenlose Prozesse) | 5,3 ms | — |
| M74b | Verweistreue `Message` → `Process` | 7,7 ms | — |
| M75 | Unterstriche je Mandant | 4,2 ms | — |
| M75 | Nummernpräfix je Mandant | 4,1 ms | — |
| M75 | `ProcessName` (Erweiterung) | 5,9 ms | — |
| M75 | Unterstriche `ProjectID` | 1,3 ms | — |
| M76 | Projekte mit Prozesszahl *(nachgeholt)* | **5,594 ms** | 6,495 ms |
| M77 | erstes Wort des `SOSName` je Mandant | 30,6 ms | — |
| M77 | `SOS` je Prozess | 2,1 ms | — |
| M77 | Leerzeichen im `SOSName` | 12,8 ms | — |
| M77 | `SOS` je Prozess bei `VOTG` *(Nachlauf)* | 17,7 ms | — |
| M78 | Auffangprozesse + Nachrichtenzahl | < 10 ms | — |

**Nichts in dieser Runde ist teuer außer den beiden Vollzugriffen auf `Message` in M74b**, und
die werden nie Anwendungscode (L2).

---

## Was diese Runde ergeben hat — die sechs Fragen aus §5 des Auftrags

| # | Frage | Antwort aus der Messung |
|---|---|---|
| 1 | Braucht die Pflegeliste einen Filter „nur offene" oder genügt die Sortierung? *(M74a)* | **Filter ist Pflicht.** `NEXANS` hat 733 Prozesse, deutlich über der vorab gesetzten Schwelle von 500 |
| 2 | Werden Prozesse ohne Nachrichten gekennzeichnet? *(M74b)* | **Ja, zwingend.** 50,90 % tragen keine Nachricht, bei `VOTG` 89,74 %. Die Vorabschwelle lag bei 30 % |
| 3 | Welche Felder füllt die Heuristik für welchen Mandanten? *(M75, M76, M77)* | **Partner:** aus der **Prozess**kennung bei `NEXANS` (733) und bei `VOTG` (alle 390, auch die 226 unter `110_VTG_SalesInvoice`); aus der **Projekt**kennung zusätzlich bei den 36 Partnerprojekten von `VOTG`. **Richtung:** im Wesentlichen nur `VOTG` (353 Prozesse, davon 352 dort). Für die sieben Mandanten ohne Nummernpräfix füllt die Heuristik **nichts** |
| 4 | Wird die Richtung hergeleitet oder von Hand je Projekt gesetzt? *(M77)* | **Beides.** Hergeleitet für `VOTG` — dort eindeutig, null Widersprüche. Von Hand für die übrigen neun; bei `IBIS` bliebe ein einziger herleitbarer Prozess, zu wenig für ein Verfahren |
| 5 | Bleibt es bei zwei Pflegezuständen? *(M78)* | **Ja.** Genau ein echter Auffangprozess bei einem Kundenmandanten, mit drei Nachrichten. Die Abbruchbedingung ist nicht ausgelöst |
| 6 | Braucht die Pflegeliste Paginierung? *(M79)* | **Nein.** 4,2 ms beim größten Mandanten, gemessen als L12 am 06.08.2026 |

---

## Offene Punkte

Nummerierung im Anschluss an den projektweit höchsten Stand (**32**, in
[`rohdaten.md`](rohdaten.md) §13 und [`rohdaten-frontend.md`](rohdaten-frontend.md) §11).

33. **Die Indexliste von `Message` in `PROJEKTBESCHREIBUNG.md` §3.2 ist unvollständig** — sie
    nennt drei der **sieben** Sekundärindizes (`PRIMARY` ungezählt). Nachzutragen sind vier:
    `Message_ProcessFK`, `ProejctIDIDX`, `SourceMessageIDIDX`, `TargetMessageIDIDX`. Dabei ist zu
    unterscheiden:

    - `SourceMessageIDIDX` und `TargetMessageIDIDX` fehlen **nur** in §3.2.
      [`verkettung.md`](verkettung.md) ist auf ihnen geplant und gemessen (M30‑1, 10.08.2026),
      [`datenmodell.md`](datenmodell.md) führt sie seit M23‑1.
    - **`Message_ProcessFK` und `ProejctIDIDX` fehlen in §3.2 *und* in `datenmodell.md`** — die
      Liste „Nutzbare Indizes" dort führt dieselben fünf. Außerhalb der Messdokumente kennt sie
      nur [`nachrichtenliste.md`](nachrichtenliste.md).

    Das ist die eigentliche Lücke: Es sind genau diese beiden, die die Begründung von M74b
    kippen — und sie stehen in **keiner** der beiden Dateien, die man beim Planen aufschlägt.

34. **Nicht L8 ist lückenhaft — die Übernahme in die verbindliche Datei ist es.** L8 verlangt
    ausdrücklich, „ihre Spalten **und Indizes** gegen `information_schema`" zu erheben, und der
    Warnkasten unter der Stand-der-Erhebung-Tabelle sagt, die Liste führe genau beides; offen ist
    dort nur die **Zeilenzahl**. Die Regel hat also funktioniert, und die Messung lag vor. Zu
    entscheiden ist, wie eine erhobene Tabelle den Weg in `PROJEKTBESCHREIBUNG.md` findet —
    heute gibt es keinen Schritt, der das erzwingt.

35. **§4.4 nennt „diese **vier** Angaben" und listet im eigenen Titel drei** (Partner, Richtung,
    Belegart); Standort erscheint erst im Entscheidungssatz. Mit der Verkleinerung auf zwei
    kuratierte Felder vom 19.08.2026 ist der Abschnitt ohnehin zu überarbeiten.

36. **§4.4 kennt zwei von mindestens vier Namenskonventionen.** Nicht beschrieben sind
    `IBIS`/`IBISGUS` (CamelCase, Richtung und Partner ohne Trennzeichen, 281 Prozesse) und
    `110_VTG_SalesInvoice` (226 Prozesse unter einem Projekt, dessen **Projektname** keinen Partner
    nennt, während die **Prozess**namen ihn tragen).

37. **Der Satz „VTG / Suttons: […] die Richtung steht nur im `SOSName`" ist für `SUTTONS`
    falsch.** Dort ist das `SOSName`-Vokabular englisch und nicht richtungstragend. Der Satz ist
    zu teilen.

38. **Der `SOSName` ist bei `IBIS`, `IBISGUS` und `ZAST` die sauberste Partnerquelle des
    Bestands** — erstes Wort, durch Leerzeichen abgetrennt. Diese Runde hat das nur nebenbei
    gesehen; ob die Heuristik daraus schöpfen soll, ist nicht gemessen und nicht entschieden.

39. **Ein Prozess hält 44,08 % aller Nachrichten.** Für die Verteilungen aus Schritt 10 ist zu
    klären, ob eine Auswertung „nach Partner" mit dieser Konzentration überhaupt aussagekräftig
    ist oder eine logarithmische bzw. anteilige Darstellung braucht.

40. **`mandantentrennung.md` §2 nennt weiterhin „142 Projekten"** — gezählt sind 140, hier zum
    dritten Mal bestätigt. Der Punkt steht seit dem 13.08.2026 in
    [`annahmen-korrekturen.md`](annahmen-korrekturen.md) Z. 313–315 als bewusst nicht vorgenommene
    Korrektur.

---

## Abweichungen vom Auftrag

1. **Die Nummern sind um eins verschoben** (M74–M79 statt M73–M78), weil `M73` am 19.08.2026
   vergeben worden ist. V1 sieht genau das vor.

2. **`--ssl-mode=DISABLED` statt `--skip-ssl`.** Begründet im Rahmen. Kein Wechsel des Clients,
   um die Vergleichbarkeit mit allen früheren Runden zu erhalten.

3. **V2 ist nicht nur nachgeschlagen, sondern nachgemessen worden.** Der Auftrag lässt bei
   vorliegender Erhebung einen Verweis genügen. Gemessen wurde trotzdem — mit dem Ergebnis, dass
   die **Spalten** stimmen und die **Indexliste von `Message` nicht**. Der Mehraufwand betrug
   11 ms.

4. **M75 ist um `Process.ProcessName` erweitert worden.** Der Auftrag kennt die Spalte nicht.
   Sie ist mitgemessen, weil die Frage „trägt der Name einen Nummernpräfix" ohne sie nur zur
   Hälfte beantwortet wäre.

5. **M74b ist um drei Teilabfragen erweitert worden** (Auswertung je Mandant, A8-Probe,
   Verweistreue). Der Auftrag verlangt nur die Gesamtzahl. Die Aufschlüsselung je Mandant war
   nötig, weil dieselbe Falle droht wie in M75: Der Gesamtwert 50,90 % verdeckt die Spanne von
   0,00 % bis 100,00 %.

6. **M77 ist um die Widerspruchsprobe (Q4) und die Deckung je Prozess erweitert worden.** Der
   Auftrag fragt nach der Zahl der `SOS` je Prozess und leitet daraus die *Möglichkeit* eines
   Widerspruchs ab. Ob er eintritt, ist billig messbar — und er tritt nicht ein.

7. **M78 ist um eine weiter gefasste Zählung erweitert worden** und, wichtiger, die sechs Treffer
   des beauftragten Filters sind **einzeln angesehen** statt gezählt worden. Nur dadurch ist
   aufgefallen, dass vier davon keine Auffangprozesse sind.

8. **M79 ist nicht gefahren**, sondern durch Verweis auf L12 ersetzt — auftragsgemäß.

9. **Die Rohausgaben sind vom Repository ausgeschlossen** (`.gitignore`, neuer Abschnitt
   „Messrunde vor Schritt 9b"). Der Auftrag regelt das nicht; die Schritt-8-Runde hat dieselbe
   Trennung, und die Ausgaben dieser Runde tragen vollständige Partnerlisten Dritter.

10. **M76 ist zweimal gefahren worden.** Die erste Sitzung ist beim Mitschreiben der Ausgabe
    abgeschnitten worden und hat ihren `SHOW PROFILES`-Block verloren — die Laufzeit war damit
    unbelegt. Nachgeholt als `m76d-nachlauf.sql` mit Aufwärmlauf. **Die Zahlen der ersten Sitzung
    sind unverändert gültig** und im Nachlauf reproduziert; verloren war nur die Laufzeit.

11. **M77 hat einen Nachlauf bekommen.** Befund 3 nannte zunächst eine Zahl für „Prozesse mit
    mehr als einem `SOS` bei `VOTG`", die **keine Abfrage dieser Runde erhoben hatte**. Sie ist
    entfernt und durch den gemessenen Wert ersetzt (acht Prozesse, davon sechs richtungstragend).
    Der Befund selbst — null Widersprüche — war und ist gemessen.

12. **Nicht gemessen: das Aufkommen je Prozess in den letzten 30 Tagen.** Der Auftrag hat die
    Sortierung nach Aufkommen gestrichen, und Regel L2 verbietet die Live-Aggregation ohnehin.
    Die Nachrichtenzahl je Prozess aus M74b ist eine **Bestandszahl über den gesamten Datenstand**,
    kein 30-Tage-Fenster — sie ist deshalb geeignet, tote Prozesse zu erkennen, und **nicht**
    geeignet, eine Rangfolge zu begründen.

---

## Was diese Runde nicht getan hat

- Keine Migration, keine `process_catalog`, keine Zeile Anwendungscode
- Keine Heuristik — sie ist hier **vorbereitet**, nicht geschrieben
- Keine Aussage über die Produktion. Alles gilt für die Testkopie, Datenstand **08.07.2026**
- Keine Entscheidung. Die Spalte „Was daraus folgt" ist die vorab vereinbarte Lesart des
  Messwerts, kein Beschluss

---

# Nachtrag vom 20.08.2026 — M80

**Diese Messung gehört nicht zur Runde oben.** Sie ist nach dem Bau von Schritt 9b gefahren, gegen
gebaute Statements statt gegen Nachbildungen, und sie beantwortet die eine Lücke, die M79
ausdrücklich offengelassen hat:

> *„Die eine verbleibende Lücke, benannt statt verschwiegen. Nicht gemessen ist der Join gegen
> `process_catalog`, denn die Tabelle existiert nicht."*

Jetzt existiert sie (`V6__process_catalog.sql`).

| | |
|---|---|
| Sitzung | `scripts/messung-schritt9/m80-pflegeliste.sql`, Nachlauf `m80b-umfang.sql` |
| Rohausgabe | `scripts/messung-schritt9/ergebnis/m80.txt` — über `.gitignore` ausgeschlossen |
| Serverzeit | `2026-08-20 15:34:18` bis `15:34:19` |
| **`@@global.read_only`** | **`1`** — Beginn und Ende. Testkopie |
| Benutzer | `monitor_read@%`, ausschließlich `SELECT` |
| Version | `10.6.22-MariaDB-0ubuntu0.22.04.1-log`, `@@div_precision_increment` = 4 |
| Client | `mysql.exe` 8.0.46 aus MySQL Workbench, `--ssl-mode=DISABLED`, wie in allen Runden seit M32 |
| **L7 / L15** | zwei Mandanten — `NEXANS` (733 Prozesse, größter) und `SUTTONS` (17, klein). Die Einstiegstabelle ist am `EXPLAIN` abgelesen, nicht angenommen. **Kein `STRAIGHT_JOIN`** |
| **G1** | in der Sitzungsdatei steht kein Partnername, keine `ProjectID`, keine `ProcessID`. Das größte Projekt wird in der Sitzung deterministisch hergeleitet |

**Der gemessene Text ist gerendert und nicht nachgebaut.** Er stammt aus `ProzessKatalogRepository`
gegen eine jOOQ-Attrappe mit `StatementType.STATIC_STATEMENT` und unterscheidet sich vom
ausgelieferten nur dort, wo eine Sitzungsvariable an die Stelle eines Literals tritt (M80‑5).

> **Eine zweite Abweichung ist am selben Tag entstanden, nach der Messung.** Der gemessene Text
> sortiert nach `ProjectName, ProcessName`. Am Nachmittag des 20.08.2026 ist E6 eindeutig gemacht
> worden: Sortiert wird nach **`ProjectID, ProcessID`**
> ([`prozess-katalog.md`](prozess-katalog.md) §4). **Der ausgelieferte Text ist an dieser einen
> Stelle also nicht mehr der gemessene.** Der Sortierschritt geht über dieselbe Zeilenmenge und war
> schon in dieser Messung ein `filesort` — nachgemessen ist er trotzdem nicht, und diese Zeile
> steht hier, damit niemand das Gegenteil annimmt.

**Der Zustand, in dem gemessen wurde.** `process_catalog` war über den echten Codepfad gefüllt —
`POST /api/katalog/vorschlagen` je Mandant, **1.490 Zeilen**, alle mit Status `OFFEN`, davon 1.167
mit Partner und 1.020 mit Richtung. **Die Zeilen sind nach der Messung wieder gelöscht worden**; die
Tabelle steht seither leer. Wer M80 nachfahren will, muss sie erst wieder füllen — sonst misst er den
Join gegen eine leere Tabelle, und das ist eine andere Messung.

---

## M80‑1 — Was kostet die Pflegeliste?

**Frage.** L12 hat den GlassfishDB-Teil mit 4,2 ms gemessen. Was kostet der Join auf
`process_catalog` obendrauf?

### `EXPLAIN`, beide Mandanten identisch

| Tabelle | Typ | Schlüssel | `rows` | Extra |
|---|---|---|---:|---|
| `ProjectMandant` | `ref` | `ProjectMandant_Mandant_idx` | 17 / 1 | `Using where; Using index; Using temporary; Using filesort` |
| `Process` | `ref` | `Process_ProjectFK` | 5 | |
| `Project` | `eq_ref` | `PRIMARY` | 1 | |
| **`process_catalog`** | **`eq_ref`** | **`PRIMARY`** | **1** | |

**Der Katalog hängt als `eq_ref` auf `PRIMARY` daran — der bestmögliche Zugriff.** Der Plan ist
Zeichen für Zeichen derselbe wie in L12, mit einer zusätzlichen Zeile am Ende. Die Einstiegstabelle
bleibt `ProjectMandant`, bei beiden Mandanten; die einzige Änderung ist ihr `rows`-Wert (17 gegen 1),
und das ist Mengenverhalten und keine Planabweichung.

### Laufzeit (beste von fünf, davor ein Aufwärmlauf)

| Mandant | Zeilen | Pflegeliste **mit** Katalog | dieselbe Abfrage **ohne** Katalog | Aufschlag |
|---|---:|---:|---:|---:|
| `NEXANS` | 733 | **8,000 ms** | 4,764 ms | **+3,24 ms**, Faktor 1,68 |
| `SUTTONS` | 17 | **0,812 ms** | — | — |

Der Vergleichswert ohne Katalog ist in derselben Sitzung mitgefahren worden (M80‑2) und reproduziert
L12 auf 12,8 % genau (4,764 gegen 4,225 ms am 06.08.2026).

**Faktor 9,9 zwischen den beiden Mandanten bei Faktor 43 der Zeilenzahl** — dasselbe Bild wie in L12
(dort Faktor 7 bei Faktor 43). Regel L15 hat nicht zugeschlagen.

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| Der Katalog hängt als `eq_ref` auf `PRIMARY` → der Aufschlag ist eine Konstante je Zeile | **ja** | 3,24 ms auf 733 Zeilen sind **4,4 µs je Zeile**. Das ist der Preis, und er ist linear |
| Beide Mandanten unter 50 ms | **ja** (8,0 / 0,8) | Keine Paginierung nötig, E8 bleibt |
| Ein anderer Plan bei einem der beiden Mandanten → **anhalten**, Regel L15 | **nein** — identischer Plan | Kein `STRAIGHT_JOIN`, keine Umbauten |
| Über 500 ms bei einem Mandanten → **anhalten**, Paginierung nötig | **nein** | — |

**Der Befund, der in keiner vorformulierten Zeile stand:** Der Aufschlag ist mit **68 %** deutlich
größer, als „ein Join mehr auf einen Primärschlüssel" vermuten lässt. Er ist trotzdem harmlos, weil
die Ausgangszahl klein ist — aber wer die Zeile „ein `eq_ref` kostet nichts" als Faustregel
mitnimmt, nimmt die falsche mit.

> **Vorbehalt an genau diese Zahl, nachgetragen 20.08.2026.** **Die +68 % sind keine
> Produktionszahl.** Sie sind das Verhältnis zweier kleiner Zahlen (4,764 → 8,000 ms), jeweils die
> beste von fünf, auf der **Testkopie**, ohne Nebenlast und mit warmem Puffer.
>
> **Am Umfang liegt es nicht** — und das ist ausdrücklich festgehalten, weil der Vorbehalt zuerst
> dort vermutet worden ist: V0 dieser Sitzung zählt **1.490 Katalogzeilen**, also eine je
> erreichbarem Prozess, und der Join lief gegen genau diesen vollen Bestand (der `EXPLAIN` weist
> `process_catalog` als `eq_ref` auf `PRIMARY` aus, `rows = 1`). Wächst der Katalog, wächst die
> Baumtiefe kaum. **Der Vorbehalt gilt der Umgebung, nicht der Menge.**
>
> Die Richtung des Befunds steht damit. **Nachgemessen wird nicht.** Wenn die Aussage später
> gebraucht wird, wird sie gegen die Produktion neu erhoben.

---

## M80‑2 bis M80‑5 — die übrigen Statements des Endpunkts

Alle vier fahren denselben Einstieg über `ProjectMandant_Mandant_idx`.

| # | Statement | Mandant | Laufzeit (beste) | Aufwärmlauf |
|---|---|---|---:|---:|
| M80‑2 | Pflegeliste **ohne** Katalog-Join (Vergleichsanker zu L12) | `NEXANS` | **4,764 ms** | 4,487 ms |
| M80‑3 | Pflegeliste mit `nurOffene=true` | `NEXANS` | **8,006 ms** | 8,575 ms |
| M80‑4 | abgeleitete Partnerliste | `NEXANS` | **4,084 ms** | 4,165 ms |
| M80‑4 | abgeleitete Partnerliste | `SUTTONS` | **0,681 ms** | 0,724 ms |
| M80‑5 | Projektbestand — das Statement, das **Vorschau und Ausführung teilen** | `VOTG`, größtes Projekt | **1,688 ms** | 1,730 ms |

**M80‑3 ist so teuer wie M80‑1, und das hat einen benennbaren Grund, der die Zahl entwertet:** Im
Messzustand standen **alle** 1.490 Zeilen auf `OFFEN`, der Filter hat also nichts ausgeblendet und
dieselben 733 Zeilen geliefert. Gemessen ist damit die **obere** Schranke des Filters — was er
kostet, wenn er nichts spart. Was er im Betrieb spart, hängt am Pflegefortschritt und ist hier
**nicht** gemessen. Der `EXPLAIN` ist derselbe wie bei M80‑1, mit einem zusätzlichen `Using where`
an `process_catalog`.

**M80‑5 bestätigt die Zahl aus M76 aus der Gegenrichtung:** Das größte Projekt des Bestands trägt
**226** Prozesse, hergeleitet statt eingetragen. Vorschau und Ausführung der Massenzuordnung fahren
genau dieses Statement — die 1,688 ms fallen deshalb zweimal an, einmal beim Bestätigen und einmal
beim Ausführen.

---

## M80‑6 — Was die Heuristik im Bestand wirklich trifft

**Diese Messung stand nicht im Auftrag.** Sie ist entstanden, weil der Bau einen Regressionstest
gegen die Zahlen aus [`prozess-katalog.md`](prozess-katalog.md) §3.5 bekommen hat
(`HeuristikBestandDbIT`) — und der Test war beim ersten Lauf rot.

| Mandant | Prozesse | Regel A | Regel B | ohne Vorschlag | §3.5 projizierte |
|---|---:|---:|---:|---:|---|
| `NEXANS` | 733 | **509** | 0 | 224 | 509 / 0 / 224 ✔ |
| `VOTG` | 390 | **378** | 0 | 12 | 378 / 0 / 12 ✔ |
| `IBIS` | 192 | 0 | **187** | **5** | 0 / **192** / 0 |
| `IBISGUS` | 89 | 0 | **88** | **1** | 0 / **89** / 0 |
| `SUTTONS` | 17 | 0 | 0 | 17 | ✔ |
| `ZAST` | 35 | 0 | 0 | 35 | ✔ |
| `NXHBE` | 17 | 0 | 0 | 17 | ✔ |
| `EDITIONLINGERI` | 9 | 0 | **5** | **4** | 0 / **0** / **9** |
| `WOC` | 4 | 0 | 0 | 4 | ✔ |
| `SYSTEM` | 4 | 0 | 0 | 4 | ✔ |
| **Summe** | **1.490** | **887** | **280** | **323** | 887 / 281 / 322 |

**Regel A trifft die Projektion auf den Prozess genau — beide Mandanten, beide Zahlen.** Damit ist
die Lesart „der Nummernpräfix ist Bedingung und nicht nur Namensgeber" belegt und nicht gewählt:
Ohne sie bekämen 14 von 17 `NXHBE`-Prozessen einen Vorschlag, den §3.5 nicht kennt.

**Regel B weicht an zwei Stellen ab, und die beiden heben einander fast auf.**

1. **Sechs Prozesse weniger bei `IBIS`/`IBISGUS`.** Fünf bzw. einer tragen das Ankerwort
   `Eingehend`/`Ausgehend` **überhaupt nicht** — es sind im Wesentlichen die mit Unterstrich, die
   M75 dort gezählt hat. Keine Fassung der Regel kann sie erreichen. §3.5 hat für diese beiden
   Mandanten schlicht *alle* Prozesse gezählt.
2. **Fünf Prozesse mehr bei `EDITIONLINGERI`**, wo §3.5 alle neun unter „ohne Vorschlag" führt.
   Regel B trifft dort sehr wohl. Die Projektion hatte sie stillschweigend auf `IBIS`/`IBISGUS`
   beschränkt — **eine Regel gilt aber, wo ihr Muster steht, und nicht, wo man sie gemeint hat.**

> **Die Gesamtzahl stimmt damit auf eins genau — aus zwei Fehlern.** §3.5 projiziert 1.168
> Vorschläge, gemessen sind **1.167** (**78,32 %** statt 78,4 % — *der Prozentwert stand hier
> zuerst mit 77,99 %, das war ein Rechenfehler und ist am 20.08.2026 berichtigt; 1.167 von 1.490
> sind 78,32 %*). Wer nur die Summe geprüft hätte,
> hätte beide Abweichungen für nicht vorhanden gehalten. Das ist der Grund, warum der
> Regressionstest je Mandant zählt und nicht nur die Summe — und warum er alle zehn in einem
> Durchgang berichtet statt beim ersten Unterschied abzubrechen.

### Der Fehler, den diese Messung im gebauten Code gefunden hat

Die erste Fassung von Regel B verlangte vor dem Ankerwort einen Kleinbuchstaben oder einen
Nicht-Buchstaben. **Das war eine Erfindung** — die Vorgabe sagt „an Großbuchstaben zerlegen", und
dann ist jeder Großbuchstabe ein Wortanfang. Sie kostete **26** Prozesse (`IBIS` 20, `IBISGUS` 6),
die unmittelbar vor dem Anker ein zweibuchstabiges Kürzel in Großschreibung tragen.

Gemessen war der Befund eindeutig und ließ keine zweite Deutung zu: Von den verfehlten Namen trugen
**alle** den Anker in exakter CamelCase-Schreibweise, **keiner** in einer anderen. Es war die
Wortgrenze und nichts sonst.

---

## M80‑7 — Die Richtung, die niemand projiziert hatte

*(Nachlauf `m80b-umfang.sql`, reine Anzahlen)*

| Mandant | Prozesse | verschiedene Partner | mit Partner | **mit Richtung** |
|---|---:|---:|---:|---:|
| `NEXANS` | 733 | 156 | 509 | **722** |
| `VOTG` | 390 | 132 | 378 | **0** |
| `IBIS` | 192 | 78 | 187 | 188 |
| `IBISGUS` | 89 | 37 | 88 | 88 |
| `NXHBE` | 17 | 0 | 0 | **17** |
| `EDITIONLINGERI` | 9 | 3 | 5 | 5 |
| `ZAST`, `SUTTONS`, `WOC`, `SYSTEM` | 60 | 0 | 0 | 0 |
| **Summe** | **1.490** | — | **1.167** | **1.020** |

**Die Heuristik füllt für 1.020 von 1.490 Prozessen eine Richtung — 68,5 %.** Diese Zahl steht in
keinem Dokument, weil §3.5 nur den Partner zählt.

Drei Befunde daraus, und alle drei schließen offene Punkte aus
[`prozess-katalog.md`](prozess-katalog.md) §10:

1. **`NEXANS` bekommt bei 722 von 733 Prozessen eine Richtung** — aus dem Projektnamen, ohne einen
   einzigen Partner von dort. Genau die Arbeitsteilung, die §3.4 beschreibt, und deutlich wirksamer
   als dort veranschlagt.
2. **`VOTG` bekommt bei null Prozessen eine Richtung.** §3.4 sagt „VOTG bleibt der einzige Mandant,
   dessen Richtung von Hand kommt" — das ist hiermit gemessen und nicht mehr angenommen. Alle 39
   Projekte tragen keinen Anker.
3. **Offener Punkt 4 ist für vier der fünf Mandanten beantwortet.** Der Anker in Projektnamen wirkt
   bei **`NXHBE` für alle 17 Prozesse** — der einzige Mandant, der eine Richtung bekommt, ohne einen
   einzigen Partnervorschlag zu haben. Bei `ZAST`, `WOC` und `SYSTEM` wirkt er **gar nicht**. Für
   `EDITIONLINGERI` kommt die Richtung aus dem Prozessnamen (Regel B), nicht aus dem Projekt.

**Was das für die Oberfläche heißt und hier nur benannt wird:** E9 verlangt den Hinweis „kein
Vorschlag ableitbar" über der Liste, wenn für **keine** Zeile etwas abgeleitet werden konnte, und
nennt dafür `SUTTONS`, `ZAST`, `NXHBE` und `EDITIONLINGERI`. Nach dieser Messung trifft das auf
`NXHBE` und `EDITIONLINGERI` **nicht** zu — beide bekommen etwas, nur nicht das, was §3.5 erwartet
hat. Vollständig leer bleiben `SUTTONS`, `ZAST`, `WOC` und `SYSTEM`.

---

## Laufzeiten des Nachtrags im Überblick

| Messung | Statement | Laufzeit | Aufwärmlauf |
|---|---|---:|---:|
| V0 | Katalogbestand zählen | 1,887 ms | — |
| V0 | Katalogzeilen `NEXANS` zählen | 3,471 ms | — |
| **M80‑1** | **Pflegeliste `NEXANS` (733 Zeilen)** | **8,000 ms** | 8,591 ms |
| **M80‑1** | **Pflegeliste `SUTTONS` (17 Zeilen)** | **0,812 ms** | 0,867 ms |
| M80‑2 | dieselbe ohne Katalog-Join, `NEXANS` | 4,764 ms | 4,487 ms |
| M80‑3 | Pflegeliste `nurOffene`, `NEXANS` | 8,006 ms | 8,575 ms |
| M80‑4 | Partnerliste `NEXANS` | 4,084 ms | 4,165 ms |
| M80‑4 | Partnerliste `SUTTONS` | 0,681 ms | 0,724 ms |
| M80‑5 | Projektbestand, größtes Projekt (226) | 1,688 ms | 1,730 ms |
| M80‑5 | größtes Projekt herleiten | 1,508 ms | — |
| M80‑6 | Heuristik über 1.490 Prozesse *(Anwendungscode, nicht SQL)* | < 10 s für alle zehn Mandanten samt Schreiben | — |

**Nichts in diesem Nachtrag ist teuer.** Das teuerste Statement liegt bei **8,0 ms** und damit bei
0,08 % der Zeitgrenze des Lese-Pools. **Keine Abweichung vom Rahmen**, kein Statement über der
60-Sekunden-Grenze, kein Abbruch.

## Was dieser Nachtrag nicht gemessen hat

- **Den Schreibweg.** Upsert und Stapel laufen über den Schreib-Pool und sind nicht `EXPLAIN`-bar;
  belegt ist nur, dass 1.490 Zeilen über zehn Transaktionen in unter zehn Sekunden entstehen.
- **Was der Filter `nurOffene` im Betrieb spart.** M80‑3 misst die obere Schranke, siehe dort.
- **Die Pflegeliste bei teilweise gepflegtem Katalog.** Gemessen ist der Zustand „alles offen".
- **Die Sortierung, die seit dem Nachmittag des 20.08.2026 gilt.** Gemessen ist `ORDER BY
  ProjectName, ProcessName`, ausgeliefert wird `ORDER BY ProjectID, ProcessID` (E6) — siehe die
  Vorbemerkung oben.
- **Die Produktion.** Alles gilt für die Testkopie, Datenstand 08.07.2026. Das betrifft auch die
  **+68 %** aus M80‑1, die dort einen eigenen Vorbehalt tragen.

---

# Nachtrag vom 21.08.2026 — M81 und M82 (Schritt 9a)

**Auch dieser Nachtrag gehört nicht zur Runde oben.** Er ist vor und während des Baus von
Schritt 9a gefahren und beantwortet zwei Fragen, die `benutzerverwaltung.md` ausdrücklich offen
gelassen hat: **E7** (trägt der Sitzungsentzug?) und **E17** (was kostet die letzte Anmeldung aus
dem Protokoll?).

| | |
|---|---|
| Nummernvergabe | M80 war die höchste vergebene. `M81` und `M82` kamen in `docs\`, im Wurzelverzeichnis und in `scripts\` **nicht vor**; frei, hier vergeben |
| Sitzung M81 | `backend/src/test/java/de/kraftwerkone/overlord/monitor/security/SitzungssucheDbIT.java` — **kein Wegwerftest**, er bleibt bestehen und läuft bei jedem `verify` mit |
| Sitzung M82 | `scripts/messung-schritt9/m82-letzte-anmeldung.sql`, Rohausgabe `scripts/messung-schritt9/ergebnis/m82*.txt` — über `.gitignore` ausgeschlossen |
| Serverzeit | `2026-08-21 09:24:59` |
| **`@@global.read_only`** | **`1`** — Testkopie |
| Benutzer | M81 über die Anwendung (`monitor_write`, eigenes Schema); M82 als `monitor_read@%`, ausschließlich `SELECT` |
| Version | `10.6.22-MariaDB-0ubuntu0.22.04.1-log`, `@@div_precision_increment` = 4 |
| Client | M82 über `mysql.exe` 8.0.46 aus MySQL Workbench, `--ssl-mode=DISABLED`, wie in allen Runden seit M32 |
| **G1** | in `docs/` steht kein Benutzername aus dem Bestand. Die Kontenliste aus M82 ist gefahren, ihre Ausgabe steht **nur** in der ausgeschlossenen Rohdatei |

---

## M81 — Lassen sich alle Sitzungen eines Kontos finden? *(E7)*

**Frage.** Der Sitzungsentzug (E5) steht und fällt damit, ob sich **alle** Sitzungen eines Kontos
finden lassen. Das war bis heute aus der Spring-Session-Dokumentation *gelesen* und nie geprüft;
`V3__spring_session.sql` ist nicht selbst entworfen, sondern aus der Distribution übernommen.

**Bauform: ein Integrationstest, der bleibt.** Nicht ein einmaliger Blick — wenn der Entzug an einer
fremden Eigenschaft hängt, gehört sie bewacht. Fällt der Index, ändert eine Spring-Session-Fassung
den Indexnamen, oder landet nach einem Umbau der Anmeldung nichts mehr in `PRINCIPAL_NAME`, dann
verwirft der Entzug still **null** Sitzungen und meldet trotzdem Erfolg.

### Vorregistrierte Deutung — sie stand vor dem Ergebnis fest

| Ergebnis | Was daraus folgt |
|---|---|
| Alle vier erfüllt | **Bauform A** — `findByPrincipalName`, dann jede gefundene Sitzung löschen |
| Ein Punkt scheitert, Ursache **strukturell** | Bauform B — Generationszähler an `app_user` |
| Ein Punkt scheitert, Ursache **behebbar** | anhalten und melden, nicht selbst reparieren |

### a — Der Index, gegen `information_schema.STATISTICS`

Gelesen wird die **Datenbank**, nicht die Migrationsdatei: Was in der Datei steht, sagt nichts
darüber, was in der Instanz steht.

```
INDEX_NAME          SEQ_IN_INDEX  COLUMN_NAME     NON_UNIQUE  INDEX_TYPE
PRIMARY                        1  PRIMARY_ID               0  BTREE
SPRING_SESSION_IX1             1  SESSION_ID               0  BTREE
SPRING_SESSION_IX2             1  EXPIRY_TIME              1  BTREE
SPRING_SESSION_IX3             1  PRINCIPAL_NAME           1  BTREE
```

**Erfüllt.** `SPRING_SESSION_IX3` steht auf `PRINCIPAL_NAME`, und zwar als **erste** Spalte — an
zweiter Position wäre er für diese Abfrage wertlos.

### b — Steht nach der Anmeldung ein Wert darin?

Nicht selbstverständlich: Die Anmeldung läuft über einen **eigenen Controller** (`AuthController`)
statt `formLogin`, der Index wird also von keinem Standardfilter gefüllt.

```
SESSION_ID                            PRINCIPAL_NAME
f8233caa-b617-435c-9916-a5de462f76e8  it-SitzungsFall
```

**Erfüllt.** Er wird gefüllt, weil `AngemeldeterNutzer.getName()` überschrieben ist — dieselbe
Methode, die seit Schritt 3 verhindert, dass die Record-Darstellung in eine 100-Zeichen-Spalte
läuft.

### c — Injizierbar, wo zwei DataSources stehen und keine `@Primary` ist?

```
injizierter Typ: org.springframework.session.jdbc.JdbcIndexedSessionRepository
findByPrincipalName("it-SitzungsFall") -> [2103b51e-…, 91673055-…]
```

**Erfüllt**, und mehr als das: Von **zwei** parallelen Sitzungen desselben Kontos werden **beide**
gefunden. Der Test prüft bewusst zwei und nicht eine — der Fehler, den ein Test mit nur einer
Sitzung nicht fände, ist der naheliegendste: nur die zuletzt angelegte zu verwerfen.

Die Verdrahtung hängt an `@SpringSessionDataSource` am Schreib-Pool (`config/DataSourceConfig`);
ohne diese Markierung suchte Spring Session eine eindeutige DataSource und fände zwei.

### d — Zeichengenauigkeit und die Gegenprobe

```
app_user.username = 'it-SitzungsFall',  PRINCIPAL_NAME = 'it-SitzungsFall'
PRINCIPAL_NAME COLLATION_NAME = utf8mb4_general_ci
Anmeldung als 'it-sitzungsfall' -> PRINCIPAL_NAME = 'it-SitzungsFall'
   findByPrincipalName kanonisch [a76ea19c-…], abweichend [a76ea19c-…]
```

**Erfüllt, in beide Richtungen.** Der Wert stammt aus der **gefundenen Zeile** in `app_user` und
nicht aus der Eingabe — dieselbe Person hätte sonst je nach Tippweise mehrere Namen im Index. Und
die Sortierung `utf8mb4_general_ci` vergleicht ohne Rücksicht auf Groß- und Kleinschreibung, die
Suche findet die Sitzung also auch mit abweichender Schreibweise. **Das ist hier erwünscht** — es
ist dieselbe Sortierung, die seit Schritt 3 verhindert, dass „Lukas" und „lukas" zwei Konten werden
—, aber es ist jetzt **gesehen** statt angenommen.

### Ergebnis

**Alle vier Punkte erfüllt → Bauform A.** Keine Spalte `sitzungs_generation`, kein Filter je
Anfrage, keine Datenbankabfrage bei jedem Aufruf.

> **Ein Nebenbefund, der den ersten Lauf rot gemacht hat.** Das Sitzungs-Cookie trägt die ID
> **Base64-kodiert** (`OWMzY2QzNzQt…`), die Tabelle und `findByPrincipalName` tragen die rohe
> (`9c3cd374-…`). Wer beide gleichsetzt, vergleicht zwei Zeichenketten, die nie übereinstimmen
> können. Der Test hält das als Kommentar fest, damit der nächste Leser den Unterschied nicht für
> einen Defekt hält.

> **Und eine stille Falle, die kein Lauf zeigt.** `findByIndexNameAndIndexValue` liefert bei einem
> unbekannten Indexnamen eine **leere Map** statt einer Ausnahme. Ein wirkungsloser Entzug sieht
> deshalb aus wie einer, der nichts zu tun hatte. Genau deshalb geht die Zahl verworfener Sitzungen
> in das auslösende Protokollereignis (E15) — sie ist die einzige Stelle, an der ein reihenweise
> wirkungsloser Entzug auffiele.

---

## M82 — Was kostet die letzte Anmeldung je Nutzer? *(Regel L7, E17)*

**Frage.** E17 holt die letzte Anmeldung aus dem `audit_log` statt aus einer Spalte an `app_user` —
über eine Tabelle, die **jeden Download** mitprotokolliert und entsprechend wächst. `EXPLAIN` plus
Laufzeit vor dem Merge.

**Der gemessene Text ist gerendert und nicht nachgebaut.** Er stammt aus
`AppUserRepository.findeAlleKonten` gegen eine jOOQ-Attrappe mit `StatementType.STATIC_STATEMENT`
und unterscheidet sich vom ausgelieferten nur darin, dass er keine Bindeplätze trägt — es gibt
keine.

### Der Bestand, in dem gemessen wurde

| | |
|---|---|
| `audit_log` | **11.043** Zeilen |
| davon `ANMELDUNG_ERFOLG` | **8.825** = **79,9 %** |
| `app_user` | **3** Konten |
| verschiedene `actor_user_id` in Anmeldezeilen | **8.541** |

> **Die letzte Zahl ist die überraschende, und sie verzerrt die Messung — nach oben.** Drei Konten
> stehen 8.541 verschiedenen Kennungen im Protokoll gegenüber: Es sind die Testkonten mit dem
> Präfix `it-`, die jeder Integrationstestlauf anlegt und wieder löscht. Ihre Protokollzeilen
> bleiben — `audit_log.actor_user_id` trägt bewusst keinen Fremdschlüssel, damit ein gelöschtes
> Konto seine Zeilen nicht unlesbar macht (E8). **Die Gruppierung läuft hier also über 8.541 Gruppen
> statt über dreißig**, und genau die Gruppierung ist der teure Teil. Die gemessene Zahl ist damit
> eine **obere Schranke** und keine Schätzung des Normalfalls.

### `EXPLAIN`, die ausgelieferte Abfrage

| id | select_type | table | type | possible_keys | key | rows | Extra |
|---:|---|---|---|---|---|---:|---|
| 1 | PRIMARY | `app_user` | `ALL` | — | — | 2 | `Using filesort` |
| 1 | PRIMARY | `<derived2>` | `ref` | `key0` | `key0` | 56 | |
| 2 | DERIVED | `audit_log` | **`ALL`** | `idx_audit_type` | **`NULL`** | 11.329 | `Using where; Using temporary; Using filesort` |

### Laufzeit (beste von fünf, davor ein Aufwärmlauf)

| Was | beste von fünf | Aufwärmlauf |
|---|---:|---:|
| **Die ausgelieferte Abfrage** | **17,87 ms** | 20,02 ms |
| nur die Aggregation (abgeleitete Tabelle) | 18,87 ms | — |
| nur `app_user`, ohne Aggregation | **0,39 ms** | 0,72 ms |
| die Aggregation mit `FORCE INDEX (idx_audit_type)` | **34,64 ms** | — |

### Vorregistrierte Deutung, dagegengehalten

| Vorab benannt | eingetreten? | Was daraus folgt |
|---|---|---|
| Ein passender Index fehlt → **Befund, keine Kleinigkeit**: melden, Plan zeigen, Vorschlag machen, **nicht anlegen** | **ja** | Siehe „Der Befund" |
| Unter 50 ms → keine Paginierung nötig, E16 bleibt | **ja** (17,9 ms) | Die Liste bleibt ohne Paginierung und ohne serverseitige Suche |
| Über 500 ms → anhalten, E17 überdenken | **nein** | — |

### Der Befund: der vorhandene Index hilft nicht — und ihn zu erzwingen ist schlechter

`idx_audit_type (event_type, occurred_at)` steht in `possible_keys`, wird aber **nicht gewählt**
(`key = NULL`, `type = ALL`). Das ist **kein Optimiererfehler, sondern die richtige Wahl**, und es
ist gemessen statt vermutet: Mit `FORCE INDEX` läuft dieselbe Abfrage in **34,64 ms** statt
17,87 ms — **fast doppelt so lange**.

Der Grund steht in der Bestandstabelle: **79,9 %** aller Protokollzeilen sind Anmeldungen. Ein Index
auf `event_type` trennt hier nichts; er liest vier Fünftel der Tabelle über Indexeinträge statt
sequenziell und zahlt zusätzlich für jeden Treffer einen Sprung in die Zeile.

**Der Vorschlag — nicht angelegt, ausdrücklich zur Entscheidung:**

```sql
CREATE INDEX idx_audit_anmeldung ON audit_log (event_type, actor_user_id, occurred_at);
```

Damit stünde die Gruppierung in der Indexreihenfolge, `Using temporary; Using filesort` entfiele,
und `MAX(occurred_at)` je Gruppe wäre der letzte Eintrag jeder Gruppe. **Das ist eine Ableitung aus
dem Plan und keine Messung** — sie ließe sich erst nachweisen, wenn der Index steht, und er steht
bewusst nicht: Ein Index, der stillschweigend im Zuge eines Features entsteht, ist genau die Art
Änderung, die später niemand begründen kann.

### Was hier nicht gemessen ist

- **Der Index selbst.** Der Vorschlag ist aus dem Plan abgeleitet, nicht gefahren.
- **Die Produktion.** Der Bestand ist die Testkopie mit 8.541 Protokollkennungen auf drei Konten; in
  der Produktion stünden dreißig Konten und ein Protokoll ohne Testrückstände.
- **Das Wachstum.** Die Abfrage liest `audit_log` **vollständig**; ihre Kosten wachsen linear mit
  der Zahl aller Protokollzeilen, nicht mit der Zahl der Konten. Bei 11.043 Zeilen sind das 17,9 ms.
  Eine Hochrechnung steht hier bewusst nicht — sie wäre eine Multiplikation und keine Messung.
- **`app_user.last_login_at`.** Die Spalte existiert seit Schritt 3 und wird bei jeder erfolgreichen
  Anmeldung gepflegt; gelesen wird sie nirgends. Sie wäre die 0,39-ms-Antwort auf dieselbe Frage.
  E17 verwirft „eine **neue** Spalte" — diese ist nicht neu. **Nicht entschieden, hier vermerkt.**
