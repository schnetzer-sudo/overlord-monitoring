# Messungen vor Schritt 10b — Hochaggregation, Eimerbelegung, Nachtsprung, Liste, Verteilung

Erhoben am **27.08.2026** gegen die Testkopie (`GlassfishDB`, `overlord_monitor`).
Auftrag: „Messrunde vor Schritt 10b — Aufgabenstellung", Stand 27.08.2026, Nummernbereich
**M94–M98**.

**Diese Runde baut nichts und entscheidet nichts.** Die Lesarten standen vor der Erhebung fest; sie
sind unten je Messung als *Vorregistrierte Deutung* mitgeführt und nach dem Ergebnis unverändert
dagegengehalten. Was aus M94 folgt, entscheidet der Auftraggeber.

**Die eine benannte Ausnahme vom Satz „baut nichts"** ist M96. Sie lässt sich nicht abfragen: Was
eine Folge stündlicher Delta-Läufe geschrieben hätte, steht nirgends — es entsteht erst, wenn man
sie fährt. M96 leert deshalb den Rollupbereich eines 48-Stunden-Fensters und schreibt ihn neu.
`GlassfishDB` bleibt dabei unberührt; der Zustand ist am Ende der Runde Zeichen für Zeichen
wiederhergestellt, und der Nachweis steht in Sitzung 8b.

**Das eine gebaute Stück Code** ist der Messläufer
[`MessungM96DbIT`](../backend/src/test/java/de/kraftwerkone/overlord/monitor/rollup/MessungM96DbIT.java)
unter `src/test`. Er ist Messwerkzeug und kein Anwendungscode, vom Auftrag ausdrücklich freigegeben
und hier benannt.

---

## Nummernvergabe

| | |
|---|---|
| Prüfung | `grep -rnoE '\bM9[4-8]\b' docs/ scripts/ *.md` |
| Ergebnis | **kein Treffer.** M94 bis M98 sind hier vergeben |
| Gegenprobe | `grep -rnoE '\bM9[0-3]\b' docs/ scripts/ *.md` → Treffer in sechs Dateien, darunter [`messungen-schritt9.md`](messungen-schritt9.md) Z. 2225, wo **M93** am 26.08.2026 vergeben worden ist. Der Ausdruck greift |

> ### ⚠️ Der Auftrag erwartet **einen** Treffer. Es sind **null** — und das ist richtig so
>
> Der Auftrag schreibt: *„Erwartet ist genau ein Treffer, und er ist keine Vergabe:
> `docs/messungen-schritt10.md` Z. 21 enthält den Satz ‚Der Bereich **M86–M99** ist frei'. Der
> Ausdruck trifft ihn. **Die M93-Runde ist darüber gestolpert** und hat es nur bemerkt, weil sie die
> Fundstelle gelesen hat statt sie zu zählen."*
>
> **Die Fundstelle ist nachgeschlagen, und sie existiert.** [`messungen-schritt10.md`](messungen-schritt10.md)
> Z. 21 lautet wörtlich:
>
> ```
> | Ergebnis | **kein Treffer.** Der Bereich M86–M99 ist frei |
> ```
>
> **Aber `\bM9[4-8]\b` trifft sie nicht.** In der Zeile stehen `M86` und `M99`; die Zeichenklasse
> `[4-8]` deckt weder die `6` von `M86` noch die zweite `9` von `M99`. Die M93-Runde ist deshalb
> darüber gestolpert, weil **ihr** Ausdruck `\bM9[3-9]\b` lautete — und der trifft `M99`. Sie hält
> es in [`messungen-schritt9.md`](messungen-schritt9.md) Z. 2225 selbst so fest.
>
> **Die Erwartung des Auftrags stammt aus dem weiteren Ausdruck der Vorrunde und ist auf den engeren
> dieser Runde übertragen worden, ohne sie nachzurechnen.** Das ist derselbe Mechanismus wie bei
> offenem Punkt 41: eine Angabe, die im Auftrag steht und die niemand nachgeschlagen hat. Sie ist
> hier folgenlos — die Falle, vor der der Auftrag warnt, ist trotzdem umgangen worden, indem die
> Zeile gelesen und nicht gezählt wurde.

**Offene Punkte** setzen bei **55** an. Projektweit höchster vergebener Stand ist **54**
([`rollup.md`](rollup.md) §13, Z. 722); `grep -rnoE '^(4[0-9]|5[0-9]|6[0-9])\. \*\*' docs/*.md`
findet 41–48 in [`messungen-schritt10.md`](messungen-schritt10.md) und 49–54 in `rollup.md`, darüber
nichts.

**Die „Befunde, die in keine vorformulierte Zeile passten"** setzen bei **15** an — der gleichnamige
Abschnitt in [`messungen-schritt10.md`](messungen-schritt10.md) führt sechs bis vierzehn.

---

## Rahmen

| | |
|---|---|
| Ziel | **Testkopie**, MariaDB `10.6.22-MariaDB-0ubuntu0.22.04.1-log` — niemals die Produktion |
| Nachweis Testkopie | `SELECT @@global.read_only` → **`1`**, als **erste Abfrage jeder Sitzung** und in der Schlusssitzung erneut. In **allen** Sitzungen `1` |
| Benutzer | **`monitor_read@%`**, ausschließlich `SELECT`. Ausnahme: Sitzungen 5a, 5b und 8/8b mit `monitor_write` — siehe M96 |
| Sitzungen | sequenziell, jede eine eigene Verbindung, kein paralleler Lauf. Die Skriptdateien liegen unter `scripts/messung-schritt10b/` |
| Serverzeit Beginn | `2026-08-27 11:23:17` |
| Serverzeit Ende | `2026-08-27 11:51:54` |
| Client | `mysql.exe` **Ver 8.0.46** aus MySQL Workbench 8.0 CE, `--ssl-mode=DISABLED`, `--default-character-set=utf8mb4`, `-t`. **Abweichung A1, dieselbe wie seit M32** |
| Passwortübergabe | über `MYSQL_PWD` aus `OVERLORD_DB_READ_PASSWORD` bzw. `OVERLORD_DB_WRITE_PASSWORD` — kein Passwort auf der Befehlszeile, keines in einer Skriptdatei |
| Grenze | `SET max_statement_time = 60` in jeder Messsitzung. **Kein einziges Mal gerissen**; das teuerste Statement der Runde liegt bei 6,825 s |
| Laufzeit | `SET profiling = 1`, `SET profiling_history_size = 100` (Vorgabe der Instanz ist **15**), Auswertung über `SHOW PROFILES`. Ein Aufwärmlauf, dann **beste von fünf** |
| `@@div_precision_increment` | **4** — deshalb steht in keiner Abfrage dieser Runde ein `AVG` über einen Wahrheitswert; Anteile sind als `100.0000 * SUM(…) / SUM(…)` gerechnet |
| `@@session.sql_mode` | `STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION` |
| `@@max_statement_time` (Vorgabe) | **`0.000000`** — die Instanz kennt von sich aus keine Grenze; die 60 s sind allein die dieser Runde |
| `innodb_buffer_pool_size` | **26.843.545.600** Byte (25.600 MiB) |
| Zeitzone | `@@time_zone = SYSTEM`, `@@system_time_zone = CEST` |
| Datenstand | `MAX(Message.MessageLastUpdate)` = **`2026-07-08 17:21:10`** — unverändert gegenüber M0, M83 und M86–M92 |
| **Anker** | **`2025-12-30 04:09:47`** (V5). Jeder Zeitpunkt dieser Runde ist daraus hergeleitet und steht als **Literal** im Statement (Regel Z1) |

### Regelbezug — erfüllt oder nicht

| Regel | Stand | Begründung |
|---|---|---|
| **S1** — kein Schreibzugriff auf `GlassfishDB` | **erfüllt** | Zehn der dreizehn Sitzungen fahren ausschließlich `SELECT`, `SET`, `EXPLAIN`, `SHOW` mit `monitor_read`. Die drei Ausnahmen sind vom Auftrag freigegeben und fassen **ausschließlich `overlord_monitor`** an: **5a** legt `message_rollup_m96_sicherung` an und füllt sie, **5b** (der Messläufer) leert und beschreibt `message_rollup` im Fenster und schreibt `rollup_lauf`, **8b** stellt wieder her und löscht die Sicherungstabelle. **Gegengeprüft in 5a‑8 und 8‑11/8‑12:** 3.341.519 Zeilen, Datenstand `2026-07-08 17:21:10`, `DATA_LENGTH` und `INDEX_LENGTH` aller vier Tabellen byteidentisch mit V1 |
| **L4** — `MessageProperty` nur über `MessageID` | **erfüllt** | Die Tabelle wird in dieser Runde **gar nicht** angefasst. Einziges Vorkommen ist ihre Größe in V1, aus `information_schema` |
| **L7** — zwei Mandanten je Abfrage, die in Anwendungscode mündet | **erfüllt** | **M94** (Verlauf und Verteilung, je drei Paare, `NEXANS` und `SUTTONS`) und **M97** (vier Fälle je Mandant, dazu eine Gegenprobe über zwei weitere Fenster). **M95 läuft über alle zehn, M98 über vier** — dort ist der Mandantenvergleich der Gegenstand, nicht der Beleg. **M96** misst keinen Ausschnitt eines Kunden, sondern einen Ablauf über alle Mandanten; dieselbe Begründung wie bei M87 und M92 |
| **L9** — voller Durchlauf nur begründet | **erfüllt** | **Fünf** Statements laufen ohne Zeitfenster über `Message`, jedes an seiner Fundstelle begründet: V1b (`COUNT(*)`, Bestandskontrolle), V5 (die Ankerabfrage aus [`datenzugriff.md`](datenzugriff.md) §6, wörtlich übernommen), V5b (`MIN`/`MAX`, Indexspitzen), 6a‑3 und 6a‑4 (die offenen Zeilen des Gesamtbestands — ein Zeitfenster schnitte genau die Zeilen weg, um die es geht; der Zugriff läuft über `MessageStatusIDX` auf 539 von 3,34 Mio. Zeilen). **Keines ist Vorbild für Anwendungscode**, und das steht an jeder Fundstelle |
| **L10** — *gemessen war X / behauptet wird Y* | **erfüllt** | Belegvermerke bei V1, V3, M94, M95, M96, M97 und M98 |
| **L15** — `EXPLAIN` im Volltext | **erfüllt** | Je Messabfrage, Pläne ungekürzt: sechs für M94‑Verlauf, sechs für M94‑Verteilung, sechzehn für M97 (acht am nackten Statement, acht an der gemessenen Hülle), acht für die M97‑Gegenprobe, acht für M98. **Für M95 nicht erhoben** — der Auftrag nimmt sie ausdrücklich aus („Kein `EXPLAIN` nötig — das ist eine Verteilungsfrage, keine Leistungsfrage"). **Für M96 gibt es keinen Plan**, weil dort kein Statement gemessen wird, sondern ein Ablauf durch die ganze Anwendung |
| **G1** — Geheimhaltung | **erfüllt** | Keine `ProcessID`, kein `ProcessName`, **kein Partnername**, keine `MessageID`, keine Belegnummer, kein Hostname, kein `MessagePropertyValue` in dieser Datei. M98 gibt Partner **maskiert** als `Partner 1`, `Partner 2` … aus; **die Maske entsteht über `ROW_NUMBER()` im Statement selbst**, nicht durch Abschreiben, und die Zuordnung zu echten Namen ist nirgends festgehalten — auch nicht in den Sitzungsdateien. M97 gibt aus demselben Grund **keine einzige Datenzeile** aus: Der Plan kommt vom nackten Statement (`EXPLAIN` führt es nicht aus), die Laufzeit von derselben Abfrage in einer aggregierenden Hülle |
| **Z1** — kein `NOW()` | **erfüllt** | Jeder Zeitpunkt steht als **Literal** im Statement bzw. als Konstante im Messläufer. `NOW()` kommt ausschließlich in den Rahmenzeilen (`SELECT NOW() AS serverzeit`) vor, nie in einer Messabfrage |

### Die Sitzungen

Dreizehn statt der acht des Sitzungsplans — jede Messung, die geteilt oder nachgefahren werden
musste, hat eine eigene bekommen, damit die Reihenfolge nachvollziehbar bleibt.

| # | Datei | Inhalt | Benutzer | Serverzeit |
|---|---|---|---|---|
| 1 | `s1-rahmen-v1-v5.sql` | Rahmen, V1, V3, V4, V5, Nummernprobe | read | 11:23:17 – 11:23:25 |
| 1b | `s1b-v3-katalogherkunft.sql` | Nachtrag zu V3: Herkunft und Alter der Katalogzeilen | read | 11:24:20 |
| 2 | `s2-m94-verlauf.sql` | **M94 Verlauf**, drei Paare, zwei Mandanten | read | 11:25:31 – 11:26:03 |
| 3 | `s3-m94-verteilung.sql` | **M94 Verteilung**, drei Paare, zwei Mandanten | read | 11:27:49 – 11:28:23 |
| 4 | `s4-m95-belegte-eimer.sql` | **M95**, zehn Mandanten, drei Paare | read | 11:29:35 – 11:30:07 |
| 6a | `s6a-m97-vorprobe.sql` | Vorprobe zu **M97**: wie viele überfällige Zeilen trägt das Fenster? | read | 11:33:27 – 11:33:30 |
| 6b | `s6b-m97-nexans.sql` | **M97**, `NEXANS`, vier Fälle | read | 11:35:15 – 11:35:16 |
| 6c | `s6c-m97-suttons.sql` | **M97**, `SUTTONS`, vier Fälle | read | 11:35:16 – 11:35:32 |
| 7a | `s7a-m98-nexans-suttons.sql` | **M98**, `NEXANS` und `SUTTONS` | read | 11:37:32 – 11:37:41 |
| 7b | `s7b-m98-votg-ibis.sql` | **M98**, `VOTG` und `IBIS` | read | 11:37:37 – 11:37:41 |
| 6d | `s6d-m97-gegenprobe.sql` | M97 über 24 h und 7 Tage — die Gegenprobe zu Befund 19 | read | 11:44 – 11:46 |
| 5a | `s5a-m96-sicherung.sql` | **M96** — Sicherung anlegen, Stand vorher festhalten | **write** | 11:47:17 – 11:47:18 |
| 5b | `MessungM96DbIT` (kein `.sql`) | **M96** — leeren, 48 Delta-Läufe, Volllauf, Vergleich | **write** | 11:48 – 11:50 |
| 8 | `s8-abschluss.sql` | **abgebrochen** nach 8‑2 mit `ERROR 1046 (3D000)` — Abweichung A5 | **write** | 11:51:35 |
| 8b | `s8-abschluss.sql` (korrigiert) | **Wiederherstellung**, Nachweis, Abschluss-`read_only` | **write** | 11:51:52 – 11:51:54 |

> **Die Reihenfolge ist nicht die des Sitzungsplans**, und der Grund gehört hierher: Die schreibende
> Sitzung 5 setzt eine Übersetzung des Messläufers voraus, und die schreibt `backend/target/classes`
> unter der laufenden Entwicklungsinstanz neu. Sie ist deshalb **nach** allen lesenden Sitzungen
> gefahren worden, nachdem der Auftraggeber dem zugestimmt hatte. Unschädlich, weil 5a, 5b und 8b
> ausschließlich `overlord_monitor` anfassen und keine der lesenden Sitzungen aus `message_rollup`
> etwas gelesen hat, das M96 verändert hätte — die Wiederherstellung ist in 8b Zeichen für Zeichen
> nachgewiesen. Abweichung A4.

Die Rohausgaben liegen unter `scripts/messung-schritt10b/ergebnis/` und sind über `.gitignore`
**vom Repository ausgeschlossen** — dieselbe Trennung wie bei Schritt 8, 9 und 10: die
`.sql`-Sitzungen gehören ins Repository, sie sind der Beleg dafür, wie gemessen wurde; die
Laufergebnisse nicht, weil sie vollständige Prozess- und Projektkennungen und damit Partnernamen
Dritter tragen.

---

# V. Die Vorbedingungen

## V1 — Ist die Testkopie seit dem 26.08.2026 unverändert?

**Byteidentisch. Ja.**

| Tabelle | `DATA_LENGTH` | `INDEX_LENGTH` | Summe MiB | gegen [`messungen-schritt10.md`](messungen-schritt10.md) V1 |
|---|---:|---:|---:|---|
| `Message` | 740.851.712 | 2.157.330.432 | 2.763,9 | **byteidentisch** |
| `MessageAction` | 2.226.634.752 | 819.855.360 | 2.905,4 | **byteidentisch** |
| `MessageBAM` | 1.826.422.784 | 5.254.217.728 | 6.752,6 | **byteidentisch** |
| `MessageProperty` | 15.088.615.424 | 45.945.946.112 | 58.207,1 | **byteidentisch** |

`UPDATE_TIME` steht für `Message`, `MessageAction` und `MessageProperty` auf `2026-07-08 17:21:10`,
für `MessageBAM` auf `2026-06-18 13:56:08` — beides unverändert. Gezählt (nicht geschätzt):
**3.341.519** Zeilen in `Message`, Laufzeit 861,623 ms. **Diese Runde darf gegen M0…M93 gehalten
werden.**

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* vier Größenangaben aus `information_schema.TABLES`, vier `UPDATE_TIME`-Werte, eine
> gezählte Zeilenzahl und `MAX(MessageLastUpdate)`.
> *Behauptet wird:* Die Testkopie ist seit dem 26.08.2026 unverändert.
> **Die Lücke:** Byteidentische Segmentgrößen schließen eine Änderung nicht aus, die genauso viel
> Platz belegt wie das, was sie ersetzt. Der Satz trägt trotzdem, weil `UPDATE_TIME`, die gezählte
> Zeilenzahl und `MAX(MessageLastUpdate)` **unabhängig davon** dasselbe sagen. **`TABLE_ROWS` taugt
> dafür nicht:** Für `MessageProperty` steht dort heute 46.964.279; die Schätzung ist
> Statistikrauschen und kein Bestandssignal.

## V2 — Ist die Korrekturrunde gelandet?

**Alle drei Stichproben sind da. Die Runde ist nicht angehalten worden.**

| # | Stelle | Befund |
|---|---|---|
| **(a)** | [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5, Zeile `message_rollup` | ✔ Der Block **„⚠️ Der Schlüssel von `message_rollup` ist ein anderer *(berichtigt am 27.08.2026)*"** steht unmittelbar unter der Tabelle (Z. 587 ff.) und löst den Widerspruch zugunsten von E‑a auf |
| **(b)** | [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.2, „In der Anzeige trägt ‚überfällig' keine Farbe" | ✔ Der Satz (Z. 478) trägt den datierten Block **„Berichtigt 27.08.2026 — der Verweis geht in den falschen Abschnitt, und der Termin ist inzwischen benannt"**: richtig ist §7a, und der Termin ist **spätestens beim Dashboard**, also Schritt 10b |
| **(c)** | [`messungen-schritt9.md`](messungen-schritt9.md), offene Punkte 33 und 39 | ✔ **33** trägt „Erledigt am 20.08.2026 *(Vermerk nachgetragen am 27.08.2026)*" (Z. 827 ff.), **39** trägt „Beantwortet am 26.08.2026 durch M91 *(Vermerk eingetragen am 27.08.2026)*" (Z. 895 ff.) |

Zusätzlich gesehen und nicht verlangt: §4.2 führt „Unquittiert" seit dem 27.08.2026 mit dem Block
„⚠️ Nicht im MVP *(Entscheidung E‑d vom 24.08.2026, eingetragen am 27.08.2026)*".

## V3 — Wie steht der Katalog heute?

**Er ist ein anderer als in M91. Die Zahlen dort sind überholt, und zwar deutlich.**

### Gesamtstand

| | M91 / V2 (26.08.2026) | **heute (27.08.2026)** |
|---|---:|---:|
| Zeilen | 1.160 | **1.486** |
| `pflegestatus = 'GEPFLEGT'` | 770 | **1.457** |
| `pflegestatus = 'OFFEN'` | 390 | **29** |
| mit Partner (nicht leer) | 932 | **1.241** |
| **gepflegt und mit Partner** | 554 | **1.241** |
| mit Richtung | 759 | **1.060** |
| verschiedene Partner | 321 | **406** |

**„Gepflegt mit Partner" ist heute dasselbe wie „mit Partner".** Die 378 Zeilen, die in V2 einen
Vorschlag aus `REGEL_A` trugen, ohne dass ihn jemand bestätigt hatte, gibt es nicht mehr — jeder
eingetragene Partner ist heute auch bestätigt. Nach **E‑i** sind damit nur noch **245 der 1.486**
Katalogzeilen „nicht zugeordnet" (29 `OFFEN` + 216 gepflegt ohne Partner) statt 606 von 1.160.

### Je Mandant — die Bezugsgröße für M95 und M98

| Mandant | Prozesse | mit Katalogzeile | `GEPFLEGT` | gepflegt **mit Partner** | gepflegt **mit Richtung** | `OFFEN` | versch. Partner |
|---|---:|---:|---:|---:|---:|---:|---:|
| **NEXANS** | 733 | 733 | 733 | **517** | **722** | 0 | 154 |
| **VOTG** | 390 | 390 | 378 | **378** | **0** | 12 | 132 |
| **IBIS** | 192 | 192 | 192 | **192** | 188 | 0 | 79 |
| **IBISGUS** | 89 | 89 | 89 | **89** | 88 | 0 | 38 |
| **ZAST** | 35 | 35 | 35 | 35 | 35 | 0 | 34 |
| **SUTTONS** | 17 | 17 | **0** | **0** | **0** | **17** | 0 |
| **NXHBE** | 17 | 17 | 17 | 17 | 17 | 0 | 3 |
| **EDITIONLINGERI** | 9 | 9 | 9 | 9 | 9 | 0 | 3 |
| **WOC** | 4 | **0** | — | 0 | 0 | — | 0 |
| **SYSTEM** | 4 | 4 | 4 | 4 | **1** | 0 | 3 |

Kontrollsummen: 1.490 Prozesse in der Kette (wie M91), 1.486 Katalogzeilen (= Gesamtstand),
1.457 gepflegt, 1.241 gepflegt mit Partner, 1.060 gepflegt mit Richtung, 29 offen — jede Spalte
summiert auf ihren Gesamtwert. Die Richtungswerte verteilen sich auf **545 `AUSGEHEND`**,
**515 `EINGEHEND`** und **426 ohne Angabe**.

> **Sechs Mandanten, die in M91 keine einzige gepflegte Zeile hatten, sind heute vollständig
> gepflegt:** `VOTG` (0 → 378 mit Partner), `IBIS` (2 → 192), `IBISGUS` (0 → 89), `NXHBE` (0 → 17),
> `EDITIONLINGERI` (0 → 9), `SYSTEM` (0 → 4).
>
> **Zwei sind es nicht.** `SUTTONS` trägt 17 Katalogzeilen, von denen **keine einzige** gepflegt
> ist, und `WOC` hat weiterhin **keine Katalogzeile**. Das ist Befund 23 und offener Punkt 58.
>
> **Und eine Lücke ist geblieben, obwohl alles andere geschlossen wurde:** `VOTG` hat 378 gepflegte
> Partner und **keine einzige Richtung**; bei `NEXANS` fehlt die Richtung weiterhin an genau **elf**
> von 733 Zeilen — es sind dieselben elf aus Befund 10 der Vorrunde. Offene Punkte 59 und 60.

### Herkunft und Alter der Zeilen (Sitzung 1b)

| `pflegestatus` | `vorschlag_herkunft` | Zeilen | mit Partner | mit Richtung |
|---|---|---:|---:|---:|
| `GEPFLEGT` | `REGEL_A` | 887 | 887 | 505 |
| `GEPFLEGT` | `KEINE` | 306 | 90 | 291 |
| `GEPFLEGT` | **`REGEL_B`** | **264** | 264 | 264 |
| `OFFEN` | `KEINE` | 29 | 0 | 0 |

**`REGEL_B` gibt es in V2 der Vorrunde nicht.** Dort standen ausschließlich `REGEL_A` und `KEINE`.

| | |
|---|---|
| verschiedene Bearbeiter | **2** — keiner davon eine Testkennung (`it-…`) |
| älteste Änderung | `2026-08-24 11:15:42.517` |
| jüngste Änderung | **`2026-08-27 08:32:53.902`** |
| Änderungen je Tag | 24.08.: **769** · 26.08.: **212** · 27.08.: **505** |

> **505 Zeilen sind am Morgen dieser Runde geändert worden**, rund drei Stunden vor der ersten
> Messung. Das ist Befund 22 — und der Grund, warum V3 als Bezugsgröße an den Anfang gehört und
> nicht als Formalie durchgewinkt werden darf.
>
> **Belegvermerk** (Regel L10).
> *Gemessen ist:* `pflegestatus`, `vorschlag_herkunft`, `geaendert_am` und die Zahl verschiedener
> `geaendert_von`.
> *Behauptet wird:* Der Katalog ist **kuratiert** worden und nicht von einem Bestandslauf gefüllt.
> **Die Lücke:** Dass zwei Kennungen geändert haben und keine davon nach einer Testkennung aussieht,
> ist ein Indiz und kein Beweis — der Bestandslauf aus Schritt 9b schreibt unter der Kennung dessen,
> der ihn auslöst. Belastbar ist allein: **887 der 1.457 gepflegten Zeilen tragen weiterhin
> `REGEL_A` als Herkunft**, also einen Vorschlag, den jemand bestätigt hat. Ob die Bestätigung jede
> Zeile einzeln angesehen hat, sagt die Spalte nicht.

## V4 — Ist `message_rollup` gefüllt und wie?

**Ja, mit dem Ergebnis des Volllaufs gegen die Systemuhr. Ein eigener Volllauf war nicht nötig.**

| | Wert | [`rollup.md`](rollup.md) §9 |
|---|---:|---:|
| Zeilen | **335.610** | 335.610 |
| `SUM(anzahl)` | **3.341.519** | 3.341.519 |
| verschiedene `process_id` | **738** | 738 |
| verschiedene `message_status` | **12** | 12 |
| früheste `stunde` | `2024-10-01 02:00:00` | `2024-10-01 02:00:00` |
| späteste `stunde` | `2026-07-08 17:00:00` | `2026-07-08 17:00:00` |
| Größe | 21,61 MiB | 21,61 MiB |
| **davon Indexanteil** | **0 Byte** | 0 Byte |

`rollup_lauf` trägt drei Zeilen — die drei Läufe vom 26.08.2026:

| id | art | `fenster_von` | `fenster_bis` | Dauer |
|---:|---|---|---|---:|
| 25 | VOLL | `2024-10-01 02:00:00` | `2025-12-30 05:00:00` | 45.994 ms |
| 27 | VOLL | `2024-10-01 02:00:00` | `2026-08-26 18:00:00` | 45.773 ms |
| 29 | VOLL | `2024-10-01 02:00:00` | `2026-08-26 18:00:00` | 69.186 ms |

Wasserstand (`MAX(fenster_bis)` über abgeschlossene, fehlerfreie Läufe): **`2026-08-26 18:00:00`**.
Zeile 25 ist der dev-Lauf am Anker (335.595 Zeilen), 27 der Lauf gegen die Systemuhr ohne
Drosselung, 29 derselbe mit `scheiben-pause: 1s` — Zeichen für Zeichen die drei Zeilen aus
[`rollup.md`](rollup.md) §9.

## V5 — Der Ankerzeitpunkt

Die Drei-Mandanten-Abfrage aus [`datenzugriff.md`](datenzugriff.md) §6, **wörtlich übernommen**.
Einzige Änderung: ein vorangestelltes `USE GlassfishDB`, damit die unqualifizierten Tabellennamen
auflösen.

```sql
select max(m.MessageLastUpdate)
from Message m
join Process p on p.ProcessID = m.ProcessID
join ProjectMandant pm on pm.ProjectID = p.ProjectID
group by date(m.MessageLastUpdate)
having count(distinct pm.MandantID) >= 3
order by date(m.MessageLastUpdate) desc
limit 1
```

| | |
|---|---|
| **Anker** | **`2025-12-30 04:09:47`** |
| Laufzeit | **6.825,360 ms** — das teuerste Statement der Runde |
| `MAX(MessageLastUpdate)` (Gegenprobe) | `2026-07-08 17:21:10` |
| `MIN(MessageLastUpdate)` (Gegenprobe) | `2024-10-01 02:00:28` |

**Der Wert ist Zeichen für Zeichen der, den [`datenzugriff.md`](datenzugriff.md) §6 seit dem
01.08.2026 nennt**, und die 6,8 s decken sich mit den dort genannten „rund 6,9 Sekunden".

### Die Fenster dieser Runde, alle aus dem Anker hergeleitet

| Paar | Eimer | von (einschließlich) | bis (ausschließlich) |
|---|---:|---|---|
| **P1** 48 h / Stunde | 48 | `2025-12-28 05:00:00` | `2025-12-30 05:00:00` |
| **P2** 30 Tage / Tag | 30 | `2025-12-01 00:00:00` | `2025-12-31 00:00:00` |
| **P3** 12 Monate / Monat | 12 | `2025-01-01 00:00:00` | `2026-01-01 00:00:00` |

Die obere Grenze ist jeweils der **Anfang des nächsten Eimers** — genau wie
`RollupFenster.ausgedehnt` sie setzt. Der angebrochene Eimer, in dem der Anker liegt, gehört dazu.

**M97 rechnet anders, und das ist richtig so:** Der Listen-Endpunkt löst `zeitraum` gegen die
Anwendungsuhr auf und nicht gegen Eimergrenzen. Sein 30‑Tage‑Fenster läuft deshalb von
`2025-11-30 04:09:47` bis `2025-12-30 04:09:47`, **beide Grenzen einschließlich** (`.ge`/`.le` in
`NachrichtenRepository`).

---

# M94 — Was kostet die Hochaggregation auf Tages- und Monatsebene?

**Die wichtigste Messung der Runde. Der Ausgangswert war schlecht, und das Ergebnis liegt jenseits
des schlechtesten vorregistrierten Zweiges.**

Zwei Abfragen, getrennt gemessen, jede für drei Paare und zwei Mandanten (L7).

## Die Verlaufsabfrage

Rollup × Mandantenkette, gruppiert je Eimer und **Rohstatus**. **Ohne `process_catalog`** — die
Kurve kennt keinen Partner.

```sql
SELECT r.stunde AS eimer, r.message_status, SUM(r.anzahl) AS anzahl
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE pm.MandantID = 'NEXANS'
  AND r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY r.stunde, r.message_status
ORDER BY r.stunde, r.message_status;
```

Für die Tagesebene steht `DATE(r.stunde)` an Stelle von `r.stunde`, für die Monatsebene
`DATE_FORMAT(r.stunde, '%Y-%m-01')` — **in `SELECT`, `GROUP BY` und `ORDER BY` jeweils als voller
Ausdruck und nie über den Alias.** Das ist die Lehre aus Befund 11 der Vorrunde: Hieße ein Alias wie
eine Tabellenspalte, bände MariaDB still an die Spalte. `anzahl` **ist** eine Spalte von
`message_rollup`; deshalb steht auch dort der Ausdruck und kein Alias.

## Die Verteilungsabfrage

Rollup × Mandantenkette × `process_catalog`, gruppiert **je Partner über das ganze Fenster**. Die
Eimerbreite des Paares spielt für sie keine Rolle, die Fensterbreite schon.

```sql
SELECT COUNT(*)                                        AS partner_eimer,
       SUM(t.nachrichten)                              AS nachrichten,
       MAX(t.nachrichten)                              AS groesster_eimer,
       SUM(CASE WHEN t.partner_wert IS NULL
                THEN t.nachrichten ELSE 0 END)         AS nicht_zugeordnet
FROM (
  SELECT CASE WHEN c.process_id IS NULL                THEN NULL
              WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
              WHEN c.partner IS NULL OR c.partner = '' THEN NULL
              ELSE c.partner END AS partner_wert,
         SUM(r.anzahl) AS nachrichten
  FROM overlord_monitor.message_rollup r
  JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
  JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
  JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
  LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
  WHERE pm.MandantID = 'NEXANS'
    AND r.stunde >= '2025-12-28 05:00:00'
    AND r.stunde <  '2025-12-30 05:00:00'
  GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
                WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
                WHEN c.partner IS NULL OR c.partner = '' THEN NULL
                ELSE c.partner END
  ORDER BY SUM(r.anzahl) DESC
) t;
```

**Die äußere Hülle ist G1 und kostet nichts.** Sie hält den Partnernamen in der inneren Abfrage;
nach außen dringen nur Anzahlen. Dieselbe Bauform wie M89 Variante B — und sie holt den
Katalog-Join zurück, den **Befund 9** der Vorrunde wegoptimiert gesehen hat: `c` steht in **allen
sechs** Plänen, als `eq_ref` auf `PRIMARY`.

`LEFT JOIN` und nicht `JOIN`: `WOC` hat nach V3 keine einzige Katalogzeile. Ein innerer Join verlöre
seine vier Prozesse stillschweigend.

## Was jedes Paar liest

| Paar | Rollupzeilen im Bereich, **gezählt** | `rows` aus dem Plan | Schätzfehler | Nachrichten im Bereich |
|---|---:|---:|---:|---:|
| **P1** 48 h | **929** | 929 | **exakt** | 12.004 |
| **P2** 30 Tage | **20.971** | 45.840 | +118,6 % zu hoch | 209.408 |
| **P3** 12 Monate | **280.186** | 167.152 | **−40,3 % zu niedrig** | 2.705.843 |

Der Bereich ist mandantenunabhängig — der Range-Zugriff auf `message_rollup` liest ihn ganz, der
Mandantenfilter wirkt erst danach. Was davon je Mandant übrigbleibt:

| Paar | Mandant | Rollupzeilen | Nachrichten | belegte Eimer | versch. Prozesse |
|---|---|---:|---:|---:|---:|
| P1 | NEXANS | 209 | 9.950 | 48 / 48 | 61 |
| P1 | SUTTONS | 350 | 1.337 | 48 / 48 | 12 |
| P2 | NEXANS | 8.799 | 176.050 | 30 / 30 | 457 |
| P2 | SUTTONS | 6.009 | 20.964 | 30 / 30 | 17 |
| P3 | NEXANS | 129.882 | 2.308.005 | 12 / 12 | 512 |
| P3 | SUTTONS | 56.358 | 196.536 | 12 / 12 | 17 |

### Was die Verteilungsabfrage nebenbei liefert

Sie ist als Kostenmessung gefahren, gibt aber Zahlen aus, und die gehören berichtet — **sie zeigen
die Konzentration über drei Fensterbreiten, wo M98 sie nur für eine misst**:

| Paar | Mandant | Partner-Eimer | größter Eimer | Anteil | *nicht zugeordnet* | Anteil |
|---|---|---:|---:|---:|---:|---:|
| P1 · 48 h | NEXANS | 25 | 8.608 | **86,51 %** | 490 | 4,92 % |
| P2 · 30 Tage | NEXANS | 116 | 101.557 | **57,69 %** | 28.387 | 16,12 % |
| P3 · 12 Monate | NEXANS | 125 | 1.258.839 | **54,54 %** | 435.678 | 18,88 % |
| P1 · 48 h | SUTTONS | 1 | 1.337 | 100,00 % | 1.337 | **100,00 %** |
| P2 · 30 Tage | SUTTONS | 1 | 20.964 | 100,00 % | 20.964 | **100,00 %** |
| P3 · 12 Monate | SUTTONS | 1 | 196.536 | 100,00 % | 196.536 | **100,00 %** |

Der Partner-Eimer *nicht zugeordnet* ist mitgezählt; bei `NEXANS` sind es also 24, 115 und 124
benannte Partner. **Die Konzentration nimmt mit der Fensterbreite ab, nicht zu** — 86,51 % über
48 Stunden, 54,54 % über zwölf Monate. M91 hat dasselbe Gefälle zwischen Tages- und Monatsfenster
gemessen (83,03 % gegen 58,61 %); es setzt sich zur Jahresebene hin fort. **Die 115 Partner des
30‑Tage‑Fensters sind Zeichen für Zeichen die Zahl aus M91**, obwohl seither 505 Katalogzeilen
kuratiert worden sind.

## Laufzeit — ein Aufwärmlauf, dann beste von fünf

### Verlauf

| Paar | Mandant | Aufwärmlauf | **beste von fünf** | alle fünf (ms) | µs je gelesener Rollupzeile |
|---|---|---:|---:|---|---:|
| P1 | NEXANS | 7,139 ms | **6,834 ms** | 6,957 · 7,361 · 6,841 · 6,852 · 6,834 | 7,36 |
| P2 | NEXANS | 153,517 ms | **149,928 ms** | 152,376 · 151,348 · 149,928 · 154,994 · 151,276 | 7,15 |
| **P3** | **NEXANS** | 2.221,181 ms | **2.206,854 ms** | 2.239,677 · 2.214,201 · 2.206,854 · 2.215,985 · 2.233,335 | 7,88 |
| P1 | SUTTONS | 6,145 ms | **5,973 ms** | 6,041 · 6,082 · 5,994 · 5,973 · 5,999 | 6,43 |
| P2 | SUTTONS | 114,066 ms | **114,116 ms** | 114,881 · 114,116 · 115,010 · 114,493 · 118,231 | 5,44 |
| **P3** | **SUTTONS** | 1.492,916 ms | **1.490,789 ms** | 1.499,981 · 1.492,819 · 1.497,518 · 1.499,208 · 1.490,789 | 5,32 |

### Verteilung

| Paar | Mandant | Aufwärmlauf | **beste von fünf** | alle fünf (ms) | µs je Zeile | Aufschlag gegen Verlauf |
|---|---|---:|---:|---|---:|---:|
| P1 | NEXANS | 11,013 ms | **10,653 ms** | 10,653 · 10,762 · 10,826 · 11,242 · 10,708 | 11,47 | **×1,559** |
| P2 | NEXANS | 183,963 ms | **179,583 ms** | 181,312 · 179,583 · 179,765 · 181,048 · 180,591 | 8,56 | ×1,198 |
| **P3** | **NEXANS** | 2.517,506 ms | **2.491,443 ms** | 2.506,007 · 2.511,430 · 2.491,443 · 2.515,501 · 2.506,107 | 8,89 | ×1,129 |
| P1 | SUTTONS | 9,746 ms | **9,406 ms** | 9,535 · 9,471 · 9,406 · 9,488 · 9,418 | 10,12 | **×1,575** |
| P2 | SUTTONS | 130,305 ms | **129,279 ms** | 129,611 · 131,103 · 129,661 · 129,340 · 129,279 | 6,16 | ×1,133 |
| **P3** | **SUTTONS** | 1.610,966 ms | **1.566,861 ms** | 1.599,861 · 1.577,781 · 1.566,861 · 1.577,701 · 1.581,901 | 5,59 | ×1,051 |

**Die Aufwärmaufschläge liegen zwischen −0,04 % und +4,5 %** — deutlich enger als die 1,9 % bis
33,1 % der Vorrunde. Der negative Wert ist P2‑SUTTONS im Verlauf: Der Aufwärmlauf war um 0,04 %
schneller als der beste der folgenden fünf. Das ist Rauschen und kein Befund; es sagt nur, dass bei
dieser Abfragegröße kein Aufwärmanteil messbar ist.

## Die Pläne (Regel L15)

Zwölf Pläne, ungekürzt. **Das Muster aus Befund 13 der Vorrunde hält in allen zwölf:** `NEXANS`
steigt über `r` ein, `SUTTONS` über `pm`.

### Verlauf · P1 · NEXANS

```
+------------------------------+
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key     | key_len | ref                           | rows | Extra                                        |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 929  | Using where; Using temporary; Using filesort |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1    | Using where                                  |
|    1 | SIMPLE      | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1    | Using where; Using index                     |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1    | Using index                                  |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

### Verlauf · P2 · NEXANS

```
+------------------------------+
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    1 | SIMPLE      | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

### Verlauf · P3 · NEXANS

```
+------------------------------+
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+--------+----------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key     | key_len | ref                           | rows   | Extra                                        |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+--------+----------------------------------------------+
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 167152 | Using where; Using temporary; Using filesort |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1      | Using where                                  |
|    1 | SIMPLE      | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1      | Using where; Using index                     |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1      | Using index                                  |
+------+-------------+-------+--------+------------------------------------+---------+---------+-------------------------------+--------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

### Verlauf · P1 · SUTTONS

```
+-------------------------------+
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key                        | key_len | ref                           | rows | Extra                                                     |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
|    1 | SIMPLE      | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1    | Using where; Using index; Using temporary; Using filesort |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1    | Using index                                               |
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 929  | Using where; Using join buffer (flat, BNL join)           |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1    | Using where                                               |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
+----------------------------------------------------------+
| marke                                                    |
+----------------------------------------------------------+
```

### Verlauf · P2 · SUTTONS

```
+-------------------------------+
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key                        | key_len | ref                           | rows  | Extra                                                     |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
|    1 | SIMPLE      | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1     | Using where; Using index; Using temporary; Using filesort |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1     | Using index                                               |
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 45840 | Using where; Using join buffer (flat, BNL join)           |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     | Using where                                               |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
+----------------------------------------------------------+
| marke                                                    |
+----------------------------------------------------------+
```

### Verlauf · P3 · SUTTONS

```
+-------------------------------+
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                      | key                        | key_len | ref                           | rows   | Extra                                                     |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+--------+-----------------------------------------------------------+
|    1 | SIMPLE      | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    1 | SIMPLE      | pr    | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1      | Using index                                               |
|    1 | SIMPLE      | r     | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 167152 | Using where; Using join buffer (flat, BNL join)           |
|    1 | SIMPLE      | p     | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1      | Using where                                               |
+------+-------------+-------+--------+------------------------------------+----------------------------+---------+-------------------------------+--------+-----------------------------------------------------------+
+----------------------------------------------------------+
| marke                                                    |
+----------------------------------------------------------+
```

### Verteilung · P1 · NEXANS

```
+------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 929  |                                              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 929  | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1    |                                              |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1    | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1    | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1    | Using index                                  |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

### Verteilung · P2 · NEXANS

```
+------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 |                                              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

### Verteilung · P3 · NEXANS

```
+------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+--------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows   | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+--------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 167152 |                                              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 167152 | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1      | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1      | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1      | Using index                                  |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1      |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+--------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

### Verteilung · P1 · SUTTONS

```
+-------------------------------+
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key                        | key_len | ref                           | rows | Extra                                                     |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL                       | NULL    | NULL                          | 929  |                                                           |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1    | Using where; Using index; Using temporary; Using filesort |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1    | Using index                                               |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 929  | Using where; Using join buffer (flat, BNL join)           |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1    |                                                           |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1    | Using where                                               |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+------+-----------------------------------------------------------+
+----------------------------------------------------------+
| marke                                                    |
+----------------------------------------------------------+
```

### Verteilung · P2 · SUTTONS

```
+-------------------------------+
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key                        | key_len | ref                           | rows  | Extra                                                     |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL                       | NULL    | NULL                          | 45840 |                                                           |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1     | Using where; Using index; Using temporary; Using filesort |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1     | Using index                                               |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 45840 | Using where; Using join buffer (flat, BNL join)           |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     | Using where                                               |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     |                                                           |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
+----------------------------------------------------------+
| marke                                                    |
+----------------------------------------------------------+
```

### Verteilung · P3 · SUTTONS

```
+-------------------------------+
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key                        | key_len | ref                           | rows   | Extra                                                     |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL                       | NULL    | NULL                          | 167152 |                                                           |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1      | Using index                                               |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 167152 | Using where; Using join buffer (flat, BNL join)           |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1      | Using where                                               |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1      |                                                           |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+--------+-----------------------------------------------------------+
+----------------------------------------------------------+
| marke                                                    |
+----------------------------------------------------------+
```
## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert:
>
> | Ergebnis | Folge |
> |---|---|
> | alle drei Paare unter 150 ms | E‑b und die festen Paare stehen ohne Vorbehalt |
> | Monatsansicht zwischen 150 und 400 ms | tragbar, aber sie ist dann die teuerste Ansicht und gehört so dokumentiert |
> | Monatsansicht über 400 ms | **E‑b trägt nicht.** Eine materialisierte Tages- oder Monatsebene ist dem Auftraggeber vorzulegen — mit der Zahl, nicht mit der Vermutung |
> | Verlauf deutlich billiger als Verteilung | dann ist der Katalog-Join der Treiber und nicht die Zeilenzahl. **Das ist ein anderer Befund und eine andere Abhilfe** |

### Der dritte Zweig trifft, und zwar mit Abstand

**Die Zwölf-Monats-Ansicht kostet 2.206,854 ms (Verlauf) bzw. 2.491,443 ms (Verteilung) bei
`NEXANS`.** Das ist das **5,5‑ bis 6,2‑Fache** der 400‑ms‑Schwelle und das **4,4‑ bis 5,0‑Fache** des
gesamten 500‑ms‑Budgets der Landingpage. Bei `SUTTONS`, dem kleineren der beiden Mandanten, sind es
immer noch 1.490,789 bzw. 1.566,861 ms — **das 3,7‑ bis 3,9‑Fache der Schwelle.**

**Nach der vorregistrierten Lesart heißt das: E‑b trägt nicht.** Eine materialisierte Tages- oder
Monatsebene ist dem Auftraggeber vorzulegen. **Diese Runde legt sie nicht vor und entscheidet
nichts** — sie liefert die Zahl. Offener Punkt 55.

### Der vierte Zweig trifft **nicht** — und das ist der eigentliche Befund

Die Vorregistrierung fragt, ob der Katalog-Join der Treiber ist. **Er ist es nicht.**

| Paar | Aufschlag der Verteilung gegenüber dem Verlauf |
|---|---:|
| P1 · 48 h | ×1,559 (NEXANS) · ×1,575 (SUTTONS) |
| P2 · 30 Tage | ×1,198 · ×1,133 |
| P3 · 12 Monate | **×1,129** · **×1,051** |

**Der Aufschlag *fällt* mit der Fensterbreite.** Bei 48 Stunden kostet der Katalog-Join gut die
Hälfte obendrauf — Zeichen für Zeichen der Faktor 1,60 bis 1,66, den M89 für dasselbe Fenster
gemessen hat, und die +68 %, die M80 unabhängig davon an der Pflegeliste fand. Bei zwölf Monaten
sind es noch 5 bis 13 %.

**Der Grund ist arithmetisch und nicht überraschend:** Der Katalog-Join ist ein `eq_ref` je gelesener
Rollupzeile, also ein konstanter Aufschlag je Zeile. Was mit dem Fenster wächst, ist alles andere —
die Bereichsabtastung, die Mandantenkette, das `Using temporary; Using filesort` der Gruppierung.
**Die Zeilenzahl treibt, nicht der Join.** Das ist Befund 15, und es ist die für 10b wichtigere
Auskunft: Den Katalog-Join wegzulassen brächte bei zwölf Monaten 5 bis 13 %; das Problem läge danach
unverändert bei über zwei Sekunden.

### Die Hochrechnung aus dem Auftrag, nachgerechnet

> Vorregistriert: *„M89 hat für ein **Monatsfenster bei Stundenauflösung** mit Katalog-Join
> **237,673 ms** gemessen. Die Zwölf-Monats-Ansicht liest rund zwölfmal so viele Rollup-Zeilen.
> Linear hochgerechnet wären das **rund 2,85 s**."*

**Gemessen sind 2,491 s.** Die lineare Hochrechnung liegt **14,5 % zu hoch** — und sie liegt damit
in der richtigen Größenordnung. Der Auftrag hat den Befund vorweggenommen; die Messung bestätigt
ihn, statt ihn zu widerlegen.

### Die Kosten je Zeile — und warum sie diesmal nicht das ganze Bild sind

Der Auftrag verlangt, die Kosten je Rollupzeile mit den **10,2–11,4 µs** aus M88 vergleichbar zu
machen. Sie sind es nur bedingt, und das gehört gesagt: M88 misst **Aggregationskosten je gelesener
`Message`-Zeile**, hier stehen **Lesekosten je `message_rollup`-Zeile**. Es sind verschiedene
Tabellen und verschiedene Arbeiten.

| | µs je gelesener Rollupzeile |
|---|---|
| Verlauf, NEXANS | 7,36 · 7,15 · 7,88 |
| Verlauf, SUTTONS | 6,43 · 5,44 · 5,32 |
| Verteilung, NEXANS | 11,47 · 8,56 · 8,89 |
| Verteilung, SUTTONS | 10,12 · 6,16 · 5,59 |

**Über einen Mengenbereich von Faktor 302 (929 bis 280.186 Zeilen) bleiben sie zwischen 5,3 und
11,5 µs.** Die Leseabfrage skaliert also sauber linear mit der Zeilenzahl — genau wie die
Aggregation in M88 und M92. **Das ist zugleich die schlechte Nachricht:** Wenn nichts überproportional
teuer ist, gibt es auch nichts wegzuoptimieren. Die einzige Stellschraube ist die Zahl der gelesenen
Zeilen, und die senkt man nur, indem man die Tages- und Monatsebene materialisiert.

### Was M94 **nicht** sagt

**Nicht, dass die 48‑Stunden‑Ansicht ein Problem hätte.** 6,8 bis 10,7 ms bei `NEXANS` sind
2,1 % des Budgets. **Nicht, dass die 30‑Tage‑Ansicht eines hätte:** 149,9 bis 179,6 ms liegen im
zweiten vorregistrierten Zweig — tragbar, und dann ist sie die teuerste Ansicht, die gebaut wird.
**Der Befund betrifft ausschließlich das Paar 12 Monate/Monat.**

---

# M95 — Anteil belegter Eimer je Mandant und Paar

Die Zahl, aus der die Schwelle für das Standardfenster fallen soll. Für **alle zehn Mandanten** und
alle drei Paare, ab dem Anker zurück.

## Die Abfrage

Eine je Paar, über alle Mandanten gruppiert — zehn Einzelabfragen läsen denselben Rollupbereich
zehnmal.

```sql
SELECT pm.MandantID                  AS mandant,
       COUNT(DISTINCT r.stunde)      AS belegte_eimer,
       COUNT(*)                      AS rollupzeilen,
       SUM(r.anzahl)                 AS nachrichten,
       COUNT(DISTINCT r.process_id)  AS versch_prozesse
FROM overlord_monitor.message_rollup r
JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
WHERE r.stunde >= '2025-12-28 05:00:00'
  AND r.stunde <  '2025-12-30 05:00:00'
GROUP BY pm.MandantID
ORDER BY nachrichten DESC;
```

Für P2 steht `DATE(r.stunde)`, für P3 `DATE_FORMAT(r.stunde, '%Y-%m-01')` im `COUNT(DISTINCT …)`.
**Ein Mandant ohne eine einzige Rollupzeile im Fenster erscheint in der Gruppierung nicht** — er ist
der Befund und steht unten mit null.

**Gegenprobe je Paar, ohne Mandantenjoin.** Sie zeigt, dass die Summe über die Mandanten den ganzen
Bereich trifft, also kein Rollupbereich an einem Prozess ohne Mandantenzeile hängenbleibt:

| Paar | Bereich gesamt | Summe über die Mandanten | Prozesse im Bereich |
|---|---:|---:|---:|
| P1 | 929 Zeilen / 12.004 | **929 / 12.004** | 163 |
| P2 | 20.971 Zeilen / 209.408 | **20.971 / 209.408** | 643 |
| P3 | 280.186 Zeilen / 2.705.843 | **280.186 / 2.705.843** | 732 |

**Die dreizehn Prozesse, die an einem Projekt ohne Mandantenzeile hängen (M91), tragen in keinem der
drei Fenster eine einzige Rollupzeile.**

## Das Ergebnis

### P1 — 48 Stunden, 48 Stundeneimer

| Mandant | belegte Eimer | Anteil | Nachrichten | Rollupzeilen | Prozesse |
|---|---:|---:|---:|---:|---:|
| **NEXANS** | 48 | **100,00 %** | 9.950 | 209 | 61 |
| **SUTTONS** | 48 | **100,00 %** | 1.337 | 350 | 12 |
| **VOTG** | 48 | **100,00 %** | 399 | 215 | 14 |
| IBIS | 18 | 37,50 % | 235 | 115 | 63 |
| IBISGUS | 13 | 27,08 % | 81 | 38 | 12 |
| WOC | 2 | 4,17 % | 2 | 2 | 1 |
| ZAST | **0** | **0,00 %** | 0 | 0 | 0 |
| NXHBE | **0** | **0,00 %** | 0 | 0 | 0 |
| SYSTEM | **0** | **0,00 %** | 0 | 0 | 0 |
| EDITIONLINGERI | **0** | **0,00 %** | 0 | 0 | 0 |

### P2 — 30 Tage, 30 Tageseimer

| Mandant | belegte Eimer | Anteil | Nachrichten | Rollupzeilen | Prozesse |
|---|---:|---:|---:|---:|---:|
| **NEXANS** | 30 | **100,00 %** | 176.050 | 8.799 | 457 |
| **SUTTONS** | 30 | **100,00 %** | 20.964 | 6.009 | 17 |
| **VOTG** | 30 | **100,00 %** | 5.937 | 3.162 | 18 |
| IBIS | 29 | 96,67 % | 4.330 | 2.084 | 105 |
| **WOC** | **29** | **96,67 %** | **117** | 46 | 2 |
| IBISGUS | 20 | 66,67 % | 1.722 | 689 | 21 |
| ZAST | 8 | 26,67 % | 283 | 177 | 22 |
| SYSTEM | 5 | 16,67 % | 5 | 5 | 1 |
| NXHBE | **0** | **0,00 %** | 0 | 0 | 0 |
| EDITIONLINGERI | **0** | **0,00 %** | 0 | 0 | 0 |

### P3 — 12 Monate, 12 Monatseimer

| Mandant | belegte Eimer | Anteil | Nachrichten | Rollupzeilen | Prozesse |
|---|---:|---:|---:|---:|---:|
| NEXANS | 12 | 100,00 % | 2.308.005 | 129.882 | 512 |
| SUTTONS | 12 | 100,00 % | 196.536 | 56.358 | 17 |
| VOTG | 12 | 100,00 % | 113.291 | 51.379 | 38 |
| IBIS | 12 | 100,00 % | 58.672 | 29.261 | 113 |
| IBISGUS | 12 | 100,00 % | 23.299 | 10.117 | 23 |
| ZAST | 12 | 100,00 % | 3.896 | 2.436 | 24 |
| WOC | 12 | 100,00 % | 2.012 | 653 | 2 |
| SYSTEM | 12 | 100,00 % | 123 | 97 | 1 |
| NXHBE | 1 | 8,33 % | **9** | 3 | 2 |
| **EDITIONLINGERI** | **0** | **0,00 %** | **0** | 0 | 0 |

## Laufzeit

Kein `EXPLAIN` — der Auftrag nimmt M95 davon aus. Die Laufzeiten stehen trotzdem im Profil:

| Paar | Aufwärmlauf | **beste von fünf** | alle fünf (ms) |
|---|---:|---:|---|
| P1 | 11,930 ms | **10,863 ms** | 11,133 · 10,941 · 10,993 · 10,920 · 10,863 |
| P2 | 261,338 ms | **257,700 ms** | 266,981 · 257,700 · 262,031 · 259,773 · 263,218 |
| P3 | 4.318,604 ms | **4.256,655 ms** | 4.300,604 · 4.288,043 · 4.269,307 · 4.288,303 · 4.256,655 |

**Diese Abfrage ist nicht die des Dashboards** und darf nicht mit M94 verglichen werden: Sie
gruppiert über alle Mandanten und zählt zwei `DISTINCT`-Mengen. Sie steht hier, weil die Erhebung
sie gebraucht hat, nicht weil 10b sie bauen würde.

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„Gesucht ist die Grenze, unterhalb derer ein Diagramm nicht mehr trägt. Erwartet
> wird ein **deutlicher Sprung zwischen `NEXANS` und den kleinen Mandanten**; liegt er sauber, ist
> die Schwelle daraus abzulesen und als **Vorschlag** zu benennen. Erwartet wird außerdem, dass
> **mindestens ein Mandant bei keinem Paar trägt** (`EDITIONLINGERI` hat im Gesamtbestand keine
> einzige Nachricht); für ihn muss der Leerzustand greifen und nicht die Fensterwahl."*

### Die zweite Erwartung trifft auf die Zeile genau

**`EDITIONLINGERI` erscheint in keinem der drei Paare.** Null belegte Eimer bei 48 Stunden, bei
30 Tagen und bei zwölf Monaten — und das, obwohl der Mandant neun Prozesse hat, die seit dem
27.08.2026 alle gepflegt sind (V3). **Für ihn greift der Leerzustand und nicht die Fensterwahl**,
genau wie vorregistriert. `NXHBE` ist der Grenzfall daneben: neun Nachrichten in einem einzigen
Monat des ganzen Jahres.

### Die erste Erwartung trifft **nicht**

**Es gibt keinen Sprung zwischen `NEXANS` und den kleinen Mandanten — jedenfalls nicht im Anteil
belegter Eimer.** `NEXANS`, `SUTTONS` und `VOTG` liegen bei **allen drei Paaren** bei 100 %, obwohl
zwischen ihnen im 48‑Stunden‑Fenster der Faktor **25** an Nachrichten liegt (9.950 gegen 399).
Der Sprung liegt woanders, nämlich zwischen `VOTG` (48/48) und `IBIS` (18/48) — und das sind nicht
die kleinsten Mandanten, sondern die mit dem **verstreutesten** Verkehr: `IBIS` hat im
48‑Stunden‑Fenster 63 verschiedene Prozesse auf 235 Nachrichten, `VOTG` 14 auf 399.

### Und der Grund, warum die Schwelle so nicht abzulesen ist

**`WOC` hat im 30‑Tage‑Fenster 29 von 30 Tagen belegt — mit 117 Nachrichten auf zwei Prozessen.**
Knapp vier Nachrichten am Tag. Nach dem Anteil belegter Eimer wäre das mit 96,67 % das
zweitbeste Ergebnis nach den drei Hundertprozentigen; nach der Menge ist es ein Diagramm, in dem
jeder Balken einen Pixel hoch ist.

**Der Anteil belegter Eimer sagt, ob ein Diagramm Punkte hat. Er sagt nicht, ob es etwas zeigt.**
Das ist Befund 17.

> ### Der Vorschlag — und er ist ausdrücklich ein Vorschlag
>
> **Die Schwelle braucht zwei Bedingungen, nicht eine.** Aus den Zahlen oben liest sich:
>
> 1. **Anteil belegter Eimer ≥ 50 %** — das trennt `NEXANS`/`SUTTONS`/`VOTG` (100 %) und
>    `IBIS`/`WOC` bei 30 Tagen (96,67 %) von `IBIS`/`IBISGUS` bei 48 Stunden (37,50 % / 27,08 %).
> 2. **und mindestens ein Eimer mit mehr als einer Handvoll Nachrichten** — die Zahl gehört
>    gewählt, nicht gemessen; `WOC` mit 3,9 Nachrichten je Tag ist der Fall, den die zweite
>    Bedingung fangen muss.
>
> **Die Festlegung gehört in 10b.** M95 liefert die Zahlen und diesen Vorschlag; entschieden wird
> hier nichts.

## Die Grenze dieser Messung — sie gehört in den Befund

**Der Bestand der Testkopie ist ungleich verteilt und endet dicht am 30.12.2025.** Der Anteil
belegter Eimer am Anker beschreibt **diesen** Bestand und nicht den Produktionsbetrieb.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* die Belegung von 48 Stunden-, 30 Tages- und 12 Monatseimern in drei Fenstern, die
> alle am Anker `2025-12-30 04:09:47` enden.
> *Behauptet wird:* Aus diesen Zahlen lässt sich eine Schwelle für die Fensterwahl ablesen.
> **Die Lücke:** Der Anker liegt im **dichtesten** Teil des Bestands — die letzten Tage vor dem
> Abbruch am 30.12.2025. Ein Anker im Frühjahr 2026 ergäbe für **jeden** Mandanten null belegte
> Eimer bei 48 Stunden. Die Zahlen beschreiben also den günstigsten Fall dieses Bestands, nicht den
> mittleren und schon gar nicht den produktiven. **Was trotzdem trägt:** die *relative* Ordnung der
> Mandanten untereinander und der Nachweis, dass `EDITIONLINGERI` bei keiner Fensterbreite etwas
> zeigt — beides hängt nicht an der Ankerlage.
---

# M96 — Wie groß ist der nächtliche Sprung?

**Diese Messung lässt sich nicht abfragen. Sie ist nachgespielt worden.**

## Das Fenster und die Sicherung

Die 48 Stunden vor dem Anker, auf ganze Stunden ausgedehnt: **`2025-12-28 05:00:00` bis
`2025-12-30 05:00:00`** — dasselbe Fenster wie Paar P1 in M94 und M95.

**Sitzung 5a** hat den Stand davor festgehalten und den Rollupbereich in
`overlord_monitor.message_rollup_m96_sicherung` gesichert. Verglichen wird über eine
**reihenfolgeunabhängige Inhaltsprüfsumme**, `BIT_XOR(CRC32(CONCAT_WS('|', stunde, process_id,
message_status, anzahl)))` — sie fasst jede Zeile und hängt nicht an der Sortierung.

| Stand vor M96 | Zeilen | `SUM(anzahl)` | Prüfsumme |
|---|---:|---:|---:|
| **Fenster** | **929** | **12.004** | **382024424** |
| außerhalb des Fensters | 334.681 | 3.329.515 | 3951018540 |
| ganze Tabelle | 335.610 | 3.341.519 | 4256889028 |
| Sicherungstabelle | **929** | **12.004** | **382024424** |
| `rollup_lauf` | 3 Zeilen, Kennungen 25, 27, 29 | | |

**Die direkte Zahl aus `Message` über dasselbe Fenster: 12.004** auf 163 Prozessen und sieben
Statuswerten. Der Rollup traf sie schon vorher auf die Einheit.

## Der Messläufer und wie die 48 Läufe geschnitten sind

[`MessungM96DbIT`](../backend/src/test/java/de/kraftwerkone/overlord/monitor/rollup/MessungM96DbIT.java),
`@Tag("db")`, Profil `dev`. Er ruft `RollupJob.fuehreAus(von, bis, art)` mit **explizit gesetzten
Grenzen** auf — **nicht über die Uhr**: Ein verstellter `Clock` würde die Protokollzeiten in
`rollup_lauf` verfälschen (`rollup.md` §4).

**Die Läufe sind nicht 48 saubere Einzelstunden, sondern so geschnitten, wie der stündliche Job sie
wirklich schneidet.** `RollupFenster.delta(W, jetzt)` greift 15 Minuten hinter den Wasserstand
zurück; weil `W` selbst ein Stundenanfang ist, fällt der Rückgriff **stets in die vorige Stunde**.
Jeder Lauf ab dem zweiten umfasst deshalb **zwei** Eimer und überlappt seinen Vorgänger um einen:

| Lauf | Fenster |
|---|---|
| 0 (Bootstrap, ohne Vorgänger) | `05:00` – `06:00` |
| 1 | `05:00` – `07:00` |
| 2 | `06:00` – `08:00` |
| … | … |
| 47 | `2025-12-30 03:00` – `05:00` |

**Zusammen 95 verarbeitete Stundeneimer für 48 Stunden** — 1 + 47 × 2. Genau dieser Überlapp ist der
Grund, warum `RollupSchreibRepository.ersetzeFenster` löscht und neu schreibt statt hochzuzählen,
und genau deshalb ist er nachgespielt und nicht wegvereinfacht worden.

## Das Ergebnis

| Schritt | Wert |
|---|---:|
| direkte Zahl aus `Message` | **12.004** |
| Stand vorher | 929 Zeilen / 12.004 |
| **geleert** | 929 Zeilen gelöscht, danach **0** im Fenster |
| 48 Delta-Läufe, verarbeitete Stundeneimer | **95** |
| … davon **gelesene** Nachrichten (Summe über alle Läufe) | **23.948** |
| … davon **geschriebene** Rollupzeilen (Summe über alle Läufe) | **1.846** |
| **inkrementeller Stand** | **929 Zeilen / 12.004** |
| Volllauf über dasselbe Fenster, Scheiben | 1 |
| **Stand nach dem Volllauf** | **929 Zeilen / 12.004** |

| Kennzahl | Wert |
|---|---:|
| **Überzählung (inkrementell gegen Volllauf)** | **0 Nachrichten = 0,0000 %** |
| Volllauf gegen die direkte Zahl | **0** — er trifft sie auf die Einheit |
| **Überlapp, den Löschen-und-Neuschreiben schluckt** | **11.944 Nachrichten = 99,5002 %** |
| … in Rollupzeilen | 917 von 1.846 = 98,71 % |

### Laufzeit — durch die ganze Anwendung, einschließlich Spring, jOOQ, Netz und Schreibtransaktion

| | |
|---|---:|
| erster Delta-Lauf der JVM | **112 ms** |
| kleinster der übrigen 47 | **4 ms** |
| größter der übrigen 47 | **65 ms** |
| Summe über alle 48 | **1.251 ms** |
| Mittel je Lauf | 26,1 ms |
| **Volllauf über 48 Stunden** | **237 ms** |

Die 20 bis 31 ms aus `rollup.md` §9 für einen typischen Delta-Lauf sind damit bestätigt; der erste
Lauf der JVM kostet hier das **4,3‑Fache** des Mittels der übrigen 47 (112 ms gegen 26,1 ms) statt
des Zwölffachen aus [`rollup.md`](rollup.md) §9 — dort steht ein größeres Fenster dahinter
(387 ms gegen 20–31 ms).

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert:
>
> | Ergebnis | Folge |
> |---|---|
> | Überzählung unter 5 % | Die Kachel darf „Nachrichten" heißen. Die Zahl gehört trotzdem in `dashboard.md` |
> | 5 bis 15 % | „Nachrichten" bleibt, der Stand-Vermerk wird zur Pflicht |
> | über 15 % | **Die Entscheidung ist dem Auftraggeber erneut vorzulegen** |
>
> Dazu: *„Erwartet wird nach `SUM(anzahl)` = 3.341.519 im Volllauf (M89), dass der Volllauf **exakt**
> die direkte Zahl trifft. Tut er das nicht, ist das ein Befund über 10a und nicht über 10b."*

**Der oberste Zweig trifft, und zwar am äußersten Rand: die Überzählung ist exakt null.** Die Kachel
darf „Nachrichten" heißen. **Und der Volllauf trifft die direkte Zahl auf die Einheit** — 12.004
gegen 12.004; es gibt keinen Befund über 10a.

> ### ⚠️ Aber die Null ist nicht die Antwort, für die man sie halten könnte
>
> **Sie ist keine Messung des nächtlichen Sprungs, sondern eine Messung des Mechanismus, der ihn
> verhindert.** Auf einem eingefrorenen Bestand *kann* eine Folge von Delta-Läufen kein anderes
> Ergebnis liefern als ein Volllauf: Jeder Lauf ersetzt **ganze Stundeneimer**, und jeder Eimer
> enthält beim Nachspielen schon alles, was er je enthalten wird.
>
> **Das Phänomen, um das es fachlich geht, ist ein anderes:** dass eine Nachricht ihren Status
> *nachdem* ihr Eimer gerollt wurde noch ändert, oder dass `MessageLastUpdate` nachwandert. Beides
> kann auf der Testkopie nicht eintreten — `RUNNING` kommt null Mal vor, `MatchInterchange` läuft
> nicht sichtbar (M31‑3, Takt ungedeckt), und der Bestand wird nicht fortgeschrieben. **Der
> nächtliche Sprung ist gegen diese Testkopie nicht messbar**, aus demselben Grund, aus dem die
> Kategorie „Überfällig" es nicht ist.
>
> **Was die Messung dafür wirklich zeigt, ist die 99,50 %.** Ein Job, der `anzahl = anzahl + n`
> rechnete statt zu ersetzen, käme für dieses Fenster auf **23.948 statt 12.004 Nachrichten** —
> **fast das Doppelte, schon nach zwei Tagen.** Das ist die Zahl, die die Entscheidung
> „Löschen und neu schreiben" ([`rollup.md`](rollup.md) §5) sichtbar macht: Sie ist nicht Vorsicht,
> sondern der Unterschied zwischen 12.004 und 23.948.
>
> **Belegvermerk** (Regel L10).
> *Gemessen ist:* 48 Delta-Läufe und ein Volllauf über dasselbe 48‑Stunden‑Fenster eines
> **eingefrorenen** Bestands, mit identischem Ergebnis.
> *Behauptet wird:* Der nächtliche Sprung beträgt null.
> **Die Lücke:** Der behauptete Satz gilt nur für einen Bestand, der sich zwischen den Läufen nicht
> ändert. Produktiv ändert er sich — genau darum gibt es das Nachlauffenster von 15 Minuten und den
> nächtlichen Volllauf (offener Punkt 49). **Der zulässige Satz lautet: „Auf einem unveränderten
> Bestand ist die Folge der Delta-Läufe zeilengleich mit dem Volllauf."** Der Satz „im Betrieb
> springt nichts" ist damit **nicht** belegt.

## Die Wiederherstellung, nachgewiesen (Sitzung 8b)

**Der Volllauf aus Schritt 5 hat den Bereich bereits korrekt neu gerechnet**; die Wiederherstellung
aus der Sicherung hat deshalb keine Zeile mehr geändert. Nachgewiesen wird beides.

| Prüfung | Sollwert (5a) | gemessen (8b) | stimmt |
|---|---:|---:|:--:|
| Fenster, Zeilen | 929 | **929** | ✔ |
| Fenster, `SUM(anzahl)` | 12.004 | **12.004** | ✔ |
| Fenster, Inhaltsprüfsumme | 382024424 | **382024424** | ✔ |
| außerhalb des Fensters, Prüfsumme | 3951018540 | **3951018540** | ✔ |
| ganze Tabelle, Zeilen | 335.610 | **335.610** | ✔ |
| ganze Tabelle, `SUM(anzahl)` | 3.341.519 | **3.341.519** | ✔ |
| ganze Tabelle, Prüfsumme | 4256889028 | **4256889028** | ✔ |
| `rollup_lauf`, Kennungen | 25, 27, 29 | **25, 27, 29** | ✔ |
| Sicherungstabelle | — | **gelöscht**, 0 Treffer in `information_schema` | ✔ |

**Vor der Wiederherstellung war der Abgleich in beide Richtungen schon leer:** null Zeilen der
Sicherung fehlten oder wichen ab, null Zeilen standen zu viel im Fenster. Der `DELETE`/`INSERT`-Block
aus 8‑3 hat nichts zu tun gehabt — er steht als Sicherung im Skript und nicht als ausgeführte
Reparatur.

**`GlassfishDB` ist unberührt:** 3.341.519 Zeilen, Datenstand `2026-07-08 17:21:10`, `DATA_LENGTH`
und `INDEX_LENGTH` aller vier Tabellen byteidentisch mit V1, `UPDATE_TIME` unverändert.
`@@global.read_only` steht am Ende der Runde auf **`1`**.

> **Eine Spur bleibt und gehört benannt:** Der `AUTO_INCREMENT`-Zähler von `rollup_lauf` ist um 49
> weitergerückt (die Kennungen 30 bis 78 sind vergeben und wieder gelöscht worden). Das ist
> folgenlos — die Spalte ist eine Kennung und keine Zählung —, aber es ist nicht „nichts". Der
> Messläufer räumt **namentlich** ab, nur die 49 Kennungen, die er selbst bekommen hat; ein
> Aufräumen über ein Zeitfenster oder über die Laufart hätte die drei fremden Zeilen mitgenommen.

---

# M97 — Die Listenabfrage mit `ueberfaellig`

Der eine neue Parameter aus E‑j. **Er ist noch nicht gebaut** — gemessen ist die Abfrage, nicht der
Endpunkt.

## Das Prädikat

`MessageStatusClassifier.istUeberfaellig`, nach SQL übersetzt:

```sql
AND m.MessageStatus IN ('SUSPENDED','RUNNING')      -- nicht Endstatus
AND m.MessageTimeout IS NOT NULL
AND m.MessageTimeout > 0                            -- 0 heisst "kein Timeout"
AND m.MessageLastUpdate + INTERVAL m.MessageTimeout SECOND < '2025-12-30 04:09:47'
```

`SUSPENDED` und `RUNNING` sind nach [`message-status.md`](message-status.md) genau die offenen
Status; jeder andere und jeder unbekannte Wert ist Endstatus. Der Stichtag ist der Anker der
**Anwendungsuhr** und steht als Literal (Regel Z1).

## Die Vorprobe (Sitzung 6a) — und eine Nachmessung, die die Korrekturrunde offen gelassen hat

| Mandant | Zeilen im 30‑Tage‑Fenster | offen | **überfällig** |
|---|---:|---:|---:|
| **NEXANS** | 180.123 | **538** | **538** |
| SUTTONS | 21.517 | 0 | **0** |
| VOTG | 6.105 | 0 | 0 |
| IBIS | 4.331 | 0 | 0 |
| IBISGUS | 1.722 | 0 | 0 |
| ZAST | 283 | 0 | 0 |
| WOC | 118 | 0 | 0 |
| SYSTEM | 5 | 0 | 0 |

Im **24‑Stunden‑Fenster** ist es genau **eine** überfällige Zeile, bei `NEXANS`; bei allen anderen
null. Vier Mandanten tauchen dort gar nicht mehr auf:

| Mandant | Zeilen im 24‑h‑Fenster | offen | **überfällig** |
|---|---:|---:|---:|
| **NEXANS** | 5.177 | **1** | **1** |
| SUTTONS | 684 | 0 | 0 |
| IBIS | 233 | 0 | 0 |
| VOTG | 206 | 0 | 0 |
| IBISGUS | 81 | 0 | 0 |
| WOC | 1 | 0 | 0 |

**Die Kachel „Überfällig“ zeigt im Standardfenster also für neun von zehn Mandanten eine Null** und
für den zehnten eine Eins. Das ist kein Befund über die Kategorie, sondern über den Bestand — es
steht seit Schritt 4 so in [`message-status.md`](message-status.md) und ist der Grund, warum die
zweite Zahl („insgesamt“) neben ihr steht.

> ### Die Zahl, die die Korrekturrunde offen gelassen hat — und sie ist unauffällig
>
> [`messungen-schritt10.md`](messungen-schritt10.md) hält im Kasten bei V5 fest: *„Damit ist auch die
> Zahl **538 überfällig** ankerabhängig: Am geltenden Anker liegt der Stichtag rund 15,5 Stunden
> hinter der letzten offenen Zeile … und wie viele der 538 dort bereits überfällig sind, hängt an
> ihren `MessageTimeout`-Werten. **Die sind nicht erhoben.** … Eine Nachmessung am geltenden Anker
> wäre billig und gehört in die nächste Messrunde."*
>
> **Hier ist sie**, Laufzeit 4,863 ms:
>
> | | |
> |---|---:|
> | offene Zeilen im Gesamtbestand | **538** |
> | davon mit Frist (`MessageTimeout > 0`) | **538** |
> | **überfällig am geltenden Anker** `2025-12-30 04:09:47` | **538** |
> | überfällig am alten Anker `2026-07-08 17:21:10` | **538** |
> | älteste offene Zeile | `2025-12-23 11:04:13` |
> | jüngste offene Zeile | `2025-12-29 12:37:16` |
> | kleinste Frist | **1.800 s** |
> | größte Frist | **1.800 s** |
>
> **Alle 538 offenen Zeilen tragen dieselbe Frist von 1.800 Sekunden — dreißig Minuten.** Die jüngste
> liegt 15,5 Stunden vor dem Anker, also das 31‑Fache der Frist. **Die Zahl 538 ist damit an beiden
> Ankern dieselbe**, und der Grund dafür ist jetzt erhoben statt vermutet: nicht Zufall, sondern der
> Abstand zwischen einer halben Stunde Frist und mehr als einem halben Tag Ruhe. Offener Punkt 43
> aus der Vorrunde ist damit in diesem Teil beantwortet.

## Die gemessene Abfrage

Alles, was `GET /api/nachrichten` ohnehin trägt — Mandantenkette als `EXISTS`, Pflicht-Zeitfenster,
die vier `LEFT JOIN` für die Anzeigenamen, Sortierung über `(MessageLastUpdate, MessageID)`,
`LIMIT 51` (= 50 + 1, wie `NachrichtenRepository` liest) — abgeschrieben aus dem Repository und
nicht nachgebaut:

```sql
SELECT m.MessageID, m.MessageLastUpdate, m.MessageStatus, m.ProcessID,
       p.ProcessName, pr.ProjectName, s.SOSName, sa.SOSActionName
FROM GlassfishDB.Message m
LEFT JOIN GlassfishDB.Process   p  ON p.ProcessID  = m.ProcessID
LEFT JOIN GlassfishDB.Project   pr ON pr.ProjectID = p.ProjectID
LEFT JOIN GlassfishDB.SOS       s  ON s.SOSID      = m.SOSID
LEFT JOIN GlassfishDB.SOSAction sa ON sa.SOSID     = m.SOSID
                                  AND sa.SOSActionID = m.SOSActionID
WHERE m.MessageLastUpdate >= '2025-11-30 04:09:47'
  AND m.MessageLastUpdate <= '2025-12-30 04:09:47'
  AND EXISTS (SELECT 1 FROM GlassfishDB.Process mp
              JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = mp.ProjectID
              WHERE mp.ProcessID = m.ProcessID AND pm.MandantID = 'NEXANS')
  -- Fall C und D zusaetzlich: das Praedikat von oben
  -- Fall B und D zusaetzlich: der Cursor in der ODER-Form
  AND (m.MessageLastUpdate < @c_ts
       OR (m.MessageLastUpdate = @c_ts AND m.MessageID < @c_id))
ORDER BY m.MessageLastUpdate DESC, m.MessageID DESC
LIMIT 51;
```

> ### Wie hier gemessen wird, ohne G1 zu brechen
>
> Diese Abfrage liefert `MessageID`, `ProcessID`, `ProcessName` und `ProjectName`. Sie darf
> **nicht** ausgegeben werden. Deshalb zwei getrennte Erhebungen für dieselbe Abfrage:
>
> * **Der Plan (L15) kommt vom nackten Statement.** `EXPLAIN` führt es nicht aus und liefert keine
>   einzige Datenzeile.
> * **Die Laufzeit (L7) kommt von derselben Abfrage in einer aggregierenden Hülle**, die
>   **jede** gejointe Spalte anfasst (`SUM(t.ProcessName IS NOT NULL)` und so fort) — sonst
>   optimierte MariaDB die `LEFT JOIN` weg, und die Messung wäre die ihrer Abwesenheit. Das ist
>   Befund 9 der Vorrunde, hier vorgebeugt.
> * **Beide Pläne stehen nebeneinander, und sie sind gleich.** Die Hülle fügt genau eine Zeile
>   hinzu — `<derived2>` mit `rows = 51` —, darunter steht Zeile für Zeile derselbe Plan. Die
>   Messung ist damit die des Endpunkts und nicht die der Hülle.
>
> **Der Cursor steht als Sitzungsvariable und nicht als Literal**, ebenfalls wegen G1: Die
> `MessageID` darf weder in der Skriptdatei noch in der Ausgabe stehen. Die Form im Statement ist
> dieselbe, die jOOQ bindet — ein Parameter. Hergeleitet wird er deterministisch als 50. Zeile der
> ersten Seite; das `OFFSET` dabei ist Herleitung und kein Blättern (Regel L3 gilt dem Endpunkt).
> Hat die überfällige Liste keine 50 Zeilen, fällt ihr Cursor auf den der ungefilterten zurück —
> **bei `SUTTONS` ist das der Fall**, und es steht unten.

## Das Ergebnis

| Fall | Mandant | Zeilen | Aufwärmlauf | **beste von fünf** | alle fünf (ms) |
|---|---|---:|---:|---:|---|
| **A** Referenz, ohne Cursor | NEXANS | 51 | 1,831 | **1,803** | 1,803 · 1,837 · 1,804 · 1,834 · 1,810 |
| **B** Referenz, mit Cursor | NEXANS | 51 | 2,408 | **2,217** | 2,333 · 2,217 · 2,281 · 2,266 · 2,289 |
| **C** `ueberfaellig`, ohne Cursor | NEXANS | 51 | 6,254 | **6,176** | 7,336 · 7,685 · 6,675 · 6,856 · 6,176 |
| **D** `ueberfaellig`, mit Cursor | NEXANS | 51 | 7,483 | **6,193** | 7,345 · 7,422 · 7,289 · 6,444 · 6,193 |
| **A** Referenz, ohne Cursor | SUTTONS | 51 | 1.150,519 | **1.101,280** | 1.252,890 · 1.101,280 · 1.121,912 · 1.133,224 · 1.109,015 |
| **B** Referenz, mit Cursor | SUTTONS | 51 | 1.102,320 | **1.073,865** | 1.073,865 · 1.095,033 · 1.113,418 · 1.116,059 · 1.078,876 |
| **C** `ueberfaellig`, ohne Cursor | SUTTONS | **0** | 7,648 | **6,958** | 9,355 · 7,339 · 7,362 · 7,090 · 6,958 |
| **D** `ueberfaellig`, mit Cursor | SUTTONS | **0** | 7,621 | **7,330** | 7,330 · 7,479 · 7,533 · 7,537 · 7,440 |

**`SUTTONS` liefert in den Fällen C und D null Zeilen** — der Mandant hat keine einzige überfällige
Nachricht (Vorprobe). Der Cursor für D ist deshalb der aus A; das ist unten unter den Abweichungen
benannt.

**Die überfällige Liste von `NEXANS` reicht über zwei Seiten:** Fall C liefert die jüngsten 51 der
538 (`2025-12-24 06:19:16` bis `2025-12-29 12:37:16`), Fall D die nächsten 51 dahinter
(`2025-12-24 06:16:16` bis `2025-12-24 06:19:16`). Alle 51 tragen denselben Status und alle 51 einen
auflösbaren Schritt — erwartungsgemäß, weil `SUSPENDED` ein offener Status ist und
[`nachrichtenliste.md`](nachrichtenliste.md) §1 für die offenen Status eine lückenlose Verknüpfung
zu `SOSAction` festhält.

## Der Plan — und er ist die eigentliche Antwort

> Vorregistriert: *„Die entscheidende Frage ist **nicht** die Laufzeit, sondern der Plan: Nutzt die
> Abfrage denselben Einstieg wie die bestehende Liste, oder zieht das Status-Prädikat sie auf
> `MessageStatusIDX` und macht den Cursor zum `filesort`? Bleibt der Plan gleich, ist der Parameter
> ein Zusatz. Ändert er sich, ist `ueberfaellig` faktisch **eine zweite Abfrageform im selben
> Endpunkt** — und das gehört benannt, bevor es gebaut wird."*

**Der Plan ändert sich. In drei verschiedene Richtungen.**

| Fall | Mandant | Treiber | Index | `key_len` | `rows` | `Extra` |
|---|---|---|---|---:|---:|---|
| A | NEXANS | `m` | `MessageLastUpdateIDX` | 5 | 437.150 | `Using where` |
| B | NEXANS | `m` | `MessageLastUpdateIDX` | **151** | 437.035 | `Using where` |
| **C** | NEXANS | `m` | **`MessageStatusIDX`** | 123 | **539** | `Using index condition; Using where; **Using filesort**` |
| **D** | NEXANS | `m` | **`MessageStatusIDX`** | 123 | **539** | `… Using filesort` |
| **A** | SUTTONS | **`pm`** | `ProjectMandant_Mandant_idx` | 146 | 1 | `Using where; Using index; **Using temporary; Using filesort**` |
| **B** | SUTTONS | **`pm`** | `ProjectMandant_Mandant_idx` | 146 | 1 | `… Using temporary; Using filesort` |
| C | SUTTONS | `m` | **`MessageStatusIDX`** | 123 | **539** | `Using index condition; Using where; Using filesort` |
| D | SUTTONS | `m` | **`MessageStatusIDX`** | 123 | **539** | `… Using filesort` |

**Bei `NEXANS` tritt genau der befürchtete Fall ein.** Ohne den Parameter läuft die Abfrage über
`MessageLastUpdateIDX`, **ohne jedes `filesort`** — der Index liefert die Sortierfolge frei Haus, und
der Cursor hebt `key_len` von 5 auf **151** an, also auf beide Spalten (der Nebenbefund aus M4/L8,
hier unabhängig bestätigt). **Mit dem Parameter zieht das Status-Prädikat sie auf
`MessageStatusIDX`, und die Sortierung wird zum `filesort`.** Der Cursor bringt dort nichts mehr:
`key_len` bleibt 123, die Cursor-Bedingung wird nachgelagert geprüft.

**Bei `SUTTONS` ist es umgekehrt — und das ist die Überraschung.** Dort ist die **ungefilterte**
Liste der pathologische Fall: Sie steigt über `pm` ein, hangelt sich über `Process_ProjectFK` zu
`ProejctIDIDX` und liest von dort geschätzte 197.804 Zeilen mit `Using temporary; Using filesort` —
**1,101 s**. Das `ueberfaellig`-Prädikat **rettet** sie: 6,958 ms, ein Faktor **158**.

## Die Gegenprobe über schmalere Fenster (Sitzung 6d)

Der SUTTONS-Befund war unerwartet genug, um ihn nicht so stehen zu lassen.
[`messungen-schritt4.md`](messungen-schritt4.md) L4 misst „SUTTONS, 24 h, ohne Filter" mit
**33,978 ms** über `MessageLastUpdateIDX`. **Ein 30‑Tage‑Fenster ist dort nie gemessen worden.**

Die Zeilen für 30 Tage sind die Fälle A und C von oben; die für 24 Stunden und sieben Tage sind in
Sitzung 6d nachgefahren, alle ohne Cursor.

| Mandant | Fenster | Fall | Treiber / Index | Aufwärmlauf | **beste von fünf** | alle fünf (ms) |
|---|---|---|---|---:|---:|---|
| NEXANS | 24 h | Referenz | `m` / `MessageLastUpdateIDX` | 1,750 | **1,771** | 1,814 · 1,771 · 1,781 · 3,436 · 2,342 |
| NEXANS | 7 Tage | Referenz | `m` / `MessageLastUpdateIDX` | 1,880 | **1,770** | 1,880 · 2,059 · 2,010 · 1,770 · 1,788 |
| NEXANS | 30 Tage | Referenz | `m` / `MessageLastUpdateIDX` | 1,831 | **1,803** | 1,803 · 1,837 · 1,804 · 1,834 · 1,810 |
| **SUTTONS** | **24 h** | Referenz | `m` / **`MessageLastUpdateIDX`** | 8,164 | **6,951** | 7,903 · 7,089 · 6,951 · 7,068 · 7,003 |
| **SUTTONS** | **7 Tage** | Referenz | `m` / **`MessageLastUpdateIDX`** | 6,599 | **6,597** | 6,602 · 6,881 · 6,597 · 6,997 · 6,978 |
| **SUTTONS** | **30 Tage** | Referenz | **`pm` / `ProejctIDIDX`** | 1.150,519 | **1.101,280** | 1.252,890 · 1.101,280 · 1.121,912 · 1.133,224 · 1.109,015 |
| NEXANS | 24 h | `ueberfaellig` | `m` / `MessageStatusIDX` | 4,078 | **4,126** | 4,126 · 4,203 · 4,308 · 4,332 · 4,322 |
| NEXANS | 7 Tage | `ueberfaellig` | `m` / `MessageStatusIDX` | 5,745 | **5,066** | 5,517 · 5,627 · 5,066 · 5,093 · 5,497 |
| NEXANS | 30 Tage | `ueberfaellig` | `m` / `MessageStatusIDX` | 6,254 | **6,176** | 7,336 · 7,685 · 6,675 · 6,856 · 6,176 |
| SUTTONS | 24 h | `ueberfaellig` | **`pm` / `ProjectMandant_Mandant_idx`** | 5,791 | **3,723** | 5,367 · 4,393 · 4,321 · 3,723 · 4,834 |
| SUTTONS | 7 Tage | `ueberfaellig` | **`pm` / `ProjectMandant_Mandant_idx`** | 5,314 | **5,221** | 5,282 · 5,227 · 5,221 · 5,359 · 5,417 |
| SUTTONS | 30 Tage | `ueberfaellig` | `m` / `MessageStatusIDX` | 7,648 | **6,958** | 9,355 · 7,339 · 7,362 · 7,090 · 6,958 |

**Zwei Aufwärmläufe sind schneller als der beste der folgenden fünf** (NEXANS 24 h: 1,750 gegen
1,771; NEXANS 24 h `ueberfaellig`: 4,078 gegen 4,126). Dasselbe Rauschen wie bei M94 und M98 — bei
Laufzeiten unter fünf Millisekunden ist der Aufwärmanteil nicht messbar.

**Der Plan kippt zwischen sieben und dreißig Tagen**, und zwar nur bei `SUTTONS`. Bei 24 Stunden und
bei sieben Tagen läuft die Referenzliste über den Zeitindex und kostet 6,6 bis 7,0 ms — in derselben
Größenordnung wie die 33,978 ms aus L4, die mit einem anderen Fensterzuschnitt und ohne die
Aggregationshülle gemessen wurden. **Bei dreißig Tagen wechselt der Optimierer auf `ProejctIDIDX`
und kostet das 167‑Fache.**

**Das ist ein Befund über den gebauten Endpunkt und nicht über 10b** — `zeitraum=30d` ist ein
zulässiger Wert von `GET /api/nachrichten` heute. Befund 19, offener Punkt 57.

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* die Listenabfrage für `SUTTONS` über 24 Stunden, 7 Tage und 30 Tage am Anker,
> jeweils bester von fünf Läufen, mit `EXPLAIN` am nackten Statement.
> *Behauptet wird:* Der Optimierer kippt zwischen 7 und 30 Tagen auf einen Plan, der das 167‑Fache
> kostet.
> **Die Lücke:** Zwischen 7 und 30 Tagen liegen 23 ungemessene Fensterbreiten. **Wo genau** die
> Grenze liegt, ist nicht erhoben — nur, dass sie dazwischen liegt. Und `zeitraum` kennt nach
> [`nachrichtenliste.md`](nachrichtenliste.md) §1 ohnehin nur `24h`, `7d` und `30d`; die Grenze
> genauer zu bestimmen hätte für den Endpunkt keinen Wert. **Was fehlt, ist der Fall `von`/`bis`**,
> der beliebige Breiten zulässt.

## Nicht gefahren: `STRAIGHT_JOIN`

**In keiner Fassung**, wie der Auftrag es vorschreibt. M42 hat dafür Faktor 219 bis 1094 gemessen.
## Die Pläne im Volltext (Regel L15)

Acht am nackten Statement, acht an der gemessenen Hülle, acht aus der Gegenprobe.

### A · NEXANS — Referenz, ohne Cursor (nackt)

```
+--------------------------------------------------------+
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                          | 437150 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
+-------------------------------------------------+
| marke                                           |
+-------------------------------------------------+
```

### A · NEXANS — dieselbe Abfrage in der gemessenen Hülle

```
+-------------------------------------------------+
+------+-------------+------------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table      | type   | possible_keys                                                                          | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+------------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                   | NULL                 | NULL    | NULL                                          | 51     |                          |
|    2 | DERIVED     | m          | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                          | 437150 | Using where              |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    2 | DERIVED     | mp         | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+------------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
+-------------------------------+
| marke                         |
+-------------------------------+
```

### B · NEXANS — Referenz, mit Cursor (nackt)

```
+--------------------------------------------------------+
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                                  | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | m     | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 151     | NULL                                          | 437035 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                      | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                             | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                        | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
+-------------------------------------------------+
| marke                                           |
+-------------------------------------------------+
```

### B · NEXANS — Hülle

```
+-------------------------------------------------+
+------+-------------+------------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
| id   | select_type | table      | type   | possible_keys                                                                                  | key                  | key_len | ref                                           | rows   | Extra                    |
+------+-------------+------------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                           | NULL                 | NULL    | NULL                                          | 51     |                          |
|    2 | DERIVED     | m          | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 151     | NULL                                          | 437035 | Using where              |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where              |
|    2 | DERIVED     | mp         | eq_ref | PRIMARY,Process_ProjectFK                                                                      | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1      | Using where              |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                             | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1      | Using where; Using index |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                        | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1      | Using where              |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                        | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where              |
+------+-------------+------------+--------+------------------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+--------+--------------------------+
+-------------------------------+
| marke                         |
+-------------------------------+
```

### C · NEXANS — `ueberfaellig`, ohne Cursor (nackt)

```
+--------------------------------------------------------+
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                           | key              | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                                      | PRIMARY          | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY          | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+-------------------------------------------------+
| marke                                           |
+-------------------------------------------------+
```

### C · NEXANS — Hülle

```
+-------------------------------------------------+
+------+-------------+------------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                                                                                           | key              | key_len | ref                                           | rows | Extra                                              |
+------+-------------+------------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                                    | NULL             | NULL    | NULL                                          | 51   |                                                    |
|    2 | DERIVED     | m          | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    2 | DERIVED     | mp         | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                                      | PRIMARY          | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY          | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+------------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+-------------------------------+
| marke                         |
+-------------------------------+
```

### D · NEXANS — `ueberfaellig`, mit Cursor (nackt)

```
+--------------------------------------------------------+
+------+-------------+-------+--------+-----------------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                                   | key              | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+-----------------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                         | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                         | PRIMARY          | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                                       | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                                              | PRIMARY          | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                         | PRIMARY          | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                         | PRIMARY          | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+-----------------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+-------------------------------------------------+
| marke                                           |
+-------------------------------------------------+
```

### D · NEXANS — Hülle

```
+-------------------------------------------------+
+------+-------------+------------+--------+-----------------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                                                                                                   | key              | key_len | ref                                           | rows | Extra                                              |
+------+-------------+------------+--------+-----------------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                                            | NULL             | NULL    | NULL                                          | 51   |                                                    |
|    2 | DERIVED     | m          | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                                         | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                                         | PRIMARY          | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    2 | DERIVED     | mp         | eq_ref | PRIMARY,Process_ProjectFK                                                                                       | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                                              | PRIMARY          | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                                         | PRIMARY          | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                         | PRIMARY          | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+------------+--------+-----------------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+-------------------------------+
| marke                         |
+-------------------------------+
```

### A · SUTTONS — Referenz, ohne Cursor (nackt)

```
+---------------------------------------------------------+
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    1 | PRIMARY     | mp    | ref    | PRIMARY,Process_ProjectFK                                                              | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using index                                               |
|    1 | PRIMARY     | m     | ref    | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
+--------------------------------------------------+
| marke                                            |
+--------------------------------------------------+
```

### A · SUTTONS — Hülle

```
+--------------------------------------------------+
+------+-------------+------------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+------------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                   | NULL                       | NULL    | NULL                                          | 51     |                                                           |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    2 | DERIVED     | mp         | ref    | PRIMARY,Process_ProjectFK                                                              | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using index                                               |
|    2 | DERIVED     | m          | ref    | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+------------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
+--------------------------------+
| marke                          |
+--------------------------------+
```

### B · SUTTONS — Referenz, mit Cursor (nackt)

```
+---------------------------------------------------------+
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                  | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                             | ProjectMandant_Mandant_idx | 146     | const                                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    1 | PRIMARY     | mp    | ref    | PRIMARY,Process_ProjectFK                                                                      | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using index                                               |
|    1 | PRIMARY     | m     | ref    | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                        | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                        | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                        | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                        | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+-------+--------+------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
+--------------------------------------------------+
| marke                                            |
+--------------------------------------------------+
```

### B · SUTTONS — Hülle

```
+--------------------------------------------------+
+------+-------------+------------+--------+------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                                                                                  | key                        | key_len | ref                                           | rows   | Extra                                                     |
+------+-------------+------------+--------+------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                           | NULL                       | NULL    | NULL                                          | 51     |                                                           |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                             | ProjectMandant_Mandant_idx | 146     | const                                         | 1      | Using where; Using index; Using temporary; Using filesort |
|    2 | DERIVED     | mp         | ref    | PRIMARY,Process_ProjectFK                                                                      | Process_ProjectFK          | 147     | GlassfishDB.pm.ProjectID                      | 5      | Using index                                               |
|    2 | DERIVED     | m          | ref    | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | ProejctIDIDX               | 147     | GlassfishDB.mp.ProcessID                      | 197804 | Using where                                               |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                        | PRIMARY                    | 146     | GlassfishDB.mp.ProcessID                      | 1      |                                                           |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                        | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1      | Using where                                               |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                        | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1      | Using where                                               |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                        | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1      | Using where                                               |
+------+-------------+------------+--------+------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+--------+-----------------------------------------------------------+
+--------------------------------+
| marke                          |
+--------------------------------+
```

### C · SUTTONS — `ueberfaellig`, ohne Cursor (nackt)

```
+---------------------------------------------------------+
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                           | key                        | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX           | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                                      | ProjectMandant_Mandant_idx | 146     | const                                         | 1    | Using where; Using index                           |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+--------------------------------------------------+
| marke                                            |
+--------------------------------------------------+
```

### C · SUTTONS — Hülle

```
+--------------------------------------------------+
+------+-------------+------------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                                                                                           | key                        | key_len | ref                                           | rows | Extra                                              |
+------+-------------+------------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                                    | NULL                       | NULL    | NULL                                          | 51   |                                                    |
|    2 | DERIVED     | m          | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX           | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                                      | ProjectMandant_Mandant_idx | 146     | const                                         | 1    | Using where; Using index                           |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    2 | DERIVED     | mp         | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+------------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+--------------------------------+
| marke                          |
+--------------------------------+
```

### D · SUTTONS — `ueberfaellig`, mit Cursor (nackt)

```
+---------------------------------------------------------+
+------+-------------+-------+--------+-----------------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                                   | key                        | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+-----------------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX           | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                                              | ProjectMandant_Mandant_idx | 146     | const                                         | 1    | Using where; Using index                           |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                         | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                         | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                                       | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                         | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                         | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+-----------------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+--------------------------------------------------+
| marke                                            |
+--------------------------------------------------+
```

### D · SUTTONS — Hülle

```
+--------------------------------------------------+
+------+-------------+------------+--------+-----------------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                                                                                                   | key                        | key_len | ref                                           | rows | Extra                                              |
+------+-------------+------------+--------+-----------------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                                                                                                            | NULL                       | NULL    | NULL                                          | 51   |                                                    |
|    2 | DERIVED     | m          | range  | PRIMARY,ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX           | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                                              | ProjectMandant_Mandant_idx | 146     | const                                         | 1    | Using where; Using index                           |
|    2 | DERIVED     | p          | eq_ref | PRIMARY                                                                                                         | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                                                                                                         | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    2 | DERIVED     | mp         | eq_ref | PRIMARY,Process_ProjectFK                                                                                       | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    2 | DERIVED     | s          | eq_ref | PRIMARY                                                                                                         | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    2 | DERIVED     | sa         | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                         | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+------------+--------+-----------------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+--------------------------------+
| marke                          |
+--------------------------------+
```

### Gegenprobe · NEXANS, 24 h, Referenz

```
+---------------------------------------------------------+
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+-------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                  | key_len | ref                                           | rows  | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+-------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                          | 13534 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1     | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1     | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1     | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1     | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+-------+--------------------------+
+--------------------------------------+
| marke                                |
+--------------------------------------+
```

### Gegenprobe · NEXANS, 24 h, `ueberfaellig`

```
+--------------------------------------------------------+
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                           | key              | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                                      | PRIMARY          | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY          | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+-------------------------------------+
| marke                               |
+-------------------------------------+
```

### Gegenprobe · NEXANS, 7 Tage, Referenz

```
+--------------------------------------------------------+
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+-------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                  | key_len | ref                                           | rows  | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+-------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX | 5       | NULL                                          | 52752 | Using where              |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.p.ProjectID                       | 1     | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY              | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                     | PRIMARY              | 292     | GlassfishDB.mp.ProjectID,const                | 1     | Using where; Using index |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY              | 146     | GlassfishDB.m.SOSID                           | 1     | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY              | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1     | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------+---------+-----------------------------------------------+-------+--------------------------+
+-------------------------------------+
| marke                               |
+-------------------------------------+
```

### Gegenprobe · NEXANS, 7 Tage, `ueberfaellig`

```
+-------------------------------------------------------+
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                           | key              | key_len | ref                                           | rows | Extra                                              |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX | 123     | NULL                                          | 539  | Using index condition; Using where; Using filesort |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                        |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY          | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                        |
|    1 | PRIMARY     | pm    | eq_ref | PRIMARY,ProjectMandant_Mandant_idx                                                                      | PRIMARY          | 292     | GlassfishDB.mp.ProjectID,const                | 1    | Using where; Using index                           |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                 | PRIMARY          | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                        |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY          | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                        |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+------------------+---------+-----------------------------------------------+------+----------------------------------------------------+
+------------------------------------+
| marke                              |
+------------------------------------+
```

### Gegenprobe · SUTTONS, 24 h, Referenz

```
+----------------------------------------------------------+
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+-------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows  | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+-------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX       | 5       | NULL                                          | 13534 | Using where              |
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 1     | Using where; Using index |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1     | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1     | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1     | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+-------+--------------------------+
+---------------------------------------+
| marke                                 |
+---------------------------------------+
```

### Gegenprobe · SUTTONS, 24 h, `ueberfaellig`

```
+---------------------------------------------------------+
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+------------------------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                           | key                        | key_len | ref                                           | rows | Extra                                                                  |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+------------------------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                                      | ProjectMandant_Mandant_idx | 146     | const                                         | 1    | Using where; Using index; Using temporary; Using filesort              |
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX           | 123     | NULL                                          | 539  | Using index condition; Using where; Using join buffer (flat, BNL join) |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                                            |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                                            |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                                            |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                                            |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                                            |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+------------------------------------------------------------------------+
+--------------------------------------+
| marke                                |
+--------------------------------------+
```

### Gegenprobe · SUTTONS, 7 Tage, Referenz

```
+---------------------------------------------------------+
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+-------+--------------------------+
| id   | select_type | table | type   | possible_keys                                                                          | key                        | key_len | ref                                           | rows  | Extra                    |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+-------+--------------------------+
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageLastUpdateProcessMessageIDX | MessageLastUpdateIDX       | 5       | NULL                                          | 52752 | Using where              |
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                     | ProjectMandant_Mandant_idx | 146     | const                                         | 1     | Using where; Using index |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1     | Using where              |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                              | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1     | Using where              |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1     | Using where              |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1     | Using where              |
+------+-------------+-------+--------+----------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+-------+--------------------------+
+--------------------------------------+
| marke                                |
+--------------------------------------+
```

### Gegenprobe · SUTTONS, 7 Tage, `ueberfaellig`

```
+--------------------------------------------------------+
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+------------------------------------------------------------------------+
| id   | select_type | table | type   | possible_keys                                                                                           | key                        | key_len | ref                                           | rows | Extra                                                                  |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+------------------------------------------------------------------------+
|    1 | PRIMARY     | pm    | ref    | PRIMARY,ProjectMandant_Mandant_idx                                                                      | ProjectMandant_Mandant_idx | 146     | const                                         | 1    | Using where; Using index; Using temporary; Using filesort              |
|    1 | PRIMARY     | m     | range  | ProejctIDIDX,Message_ProcessFK,MessageLastUpdateIDX,MessageStatusIDX,MessageLastUpdateProcessMessageIDX | MessageStatusIDX           | 123     | NULL                                          | 539  | Using index condition; Using where; Using join buffer (flat, BNL join) |
|    1 | PRIMARY     | p     | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                                            |
|    1 | PRIMARY     | pr    | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.p.ProjectID                       | 1    | Using where                                                            |
|    1 | PRIMARY     | mp    | eq_ref | PRIMARY,Process_ProjectFK                                                                               | PRIMARY                    | 146     | GlassfishDB.m.ProcessID                       | 1    | Using where                                                            |
|    1 | PRIMARY     | s     | eq_ref | PRIMARY                                                                                                 | PRIMARY                    | 146     | GlassfishDB.m.SOSID                           | 1    | Using where                                                            |
|    1 | PRIMARY     | sa    | eq_ref | PRIMARY,SOSAction_SOSFK                                                                                 | PRIMARY                    | 148     | GlassfishDB.m.SOSID,GlassfishDB.m.SOSActionID | 1    | Using where                                                            |
+------+-------------+-------+--------+---------------------------------------------------------------------------------------------------------+----------------------------+---------+-----------------------------------------------+------+------------------------------------------------------------------------+
+-------------------------------------+
| marke                               |
+-------------------------------------+
```

---

# M98 — Top‑10 mit zwei Restzeilen, nach der Kuratierung

Die Verteilung, wie sie der Block zeigen wird — und die erste Messung **nach** der Katalogpflege.
Für `NEXANS`, `SUTTONS`, `VOTG` und `IBIS`, Paar **30 Tage / Tag**
(`2025-12-01 00:00:00` bis `2025-12-31 00:00:00`).

## Die gemessene Abfrage

```sql
SELECT x.gruppe,
       COUNT(*)                                                  AS partner,
       SUM(x.nachrichten)                                        AS nachrichten,
       ROUND(100.0000 * SUM(x.nachrichten)
             / SUM(SUM(x.nachrichten)) OVER (), 4)               AS anteil_prozent
FROM (
  SELECT g.nachrichten,
         CASE WHEN g.schluessel IS NULL THEN 'nicht zugeordnet'
              WHEN ROW_NUMBER() OVER (ORDER BY (g.schluessel IS NULL),
                                               g.nachrichten DESC) <= 10 THEN 'Top 10'
              ELSE 'Uebrige' END AS gruppe
  FROM (
    SELECT CASE WHEN c.process_id IS NULL                THEN NULL
                WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
                WHEN c.partner IS NULL OR c.partner = '' THEN NULL
                ELSE c.partner END AS schluessel,
           SUM(r.anzahl) AS nachrichten
    FROM overlord_monitor.message_rollup r
    JOIN GlassfishDB.Process        p  ON p.ProcessID  = r.process_id
    JOIN GlassfishDB.Project        pr ON pr.ProjectID = p.ProjectID
    JOIN GlassfishDB.ProjectMandant pm ON pm.ProjectID = pr.ProjectID
    LEFT JOIN overlord_monitor.process_catalog c ON c.process_id = r.process_id
    WHERE pm.MandantID = 'NEXANS'
      AND r.stunde >= '2025-12-01 00:00:00'
      AND r.stunde <  '2025-12-31 00:00:00'
    GROUP BY CASE WHEN c.process_id IS NULL                THEN NULL
                  WHEN c.pflegestatus <> 'GEPFLEGT'        THEN NULL
                  WHEN c.partner IS NULL OR c.partner = '' THEN NULL
                  ELSE c.partner END
  ) g
) x
GROUP BY x.gruppe
ORDER BY nachrichten DESC;
```

**Die Maske entsteht im Statement.** Die Rangliste gibt `CONCAT('Partner ', ROW_NUMBER() OVER (…))`
aus; der echte Name verlässt die innerste Abfrage nicht, und es gibt keine Stelle, an der die
Zuordnung festgehalten würde — auch nicht in der Skriptdatei. `ORDER BY (schluessel IS NULL),
nachrichten DESC` sortiert den Eimer *nicht zugeordnet* ans Ende, damit die Ränge 1…k lückenlos den
benannten Partnern gehören: **„nicht zugeordnet" ist keine Rangposition und fällt nie in „Übrige"**
(M91, offener Punkt 39).

Für die Richtung steht dasselbe Muster mit `c.richtung` an Stelle von `c.partner`. **Der
`pflegestatus`-Riegel steht auch dort** — heute folgenlos, weil nach V3 alle 1.060 Zeilen mit
Richtung gepflegt sind, aber die Regel ist E‑i und nicht der Zufall dieses Katalogstands.

`ROUND(100.0000 * … / …)` und kein `AVG`: `@@div_precision_increment` steht auf 4.

## Die Ranglisten, maskiert

### `NEXANS` — 176.050 Nachrichten, 115 benannte Partner

| Bezeichnung | Anzahl | Anteil |
|---|---:|---:|
| **Partner 1** | **101.557** | **57,6865 %** |
| Partner 2 | 19.748 | 11,2173 % |
| Partner 3 | 2.895 | 1,6444 % |
| Partner 4 | 2.019 | 1,1468 % |
| Partner 5 | 1.900 | 1,0792 % |
| Partner 6 | 1.827 | 1,0378 % |
| Partner 7 | 1.210 | 0,6873 % |
| Partner 8 | 1.174 | 0,6669 % |
| Partner 9 | 1.171 | 0,6652 % |
| Partner 10 | 980 | 0,5567 % |
| Partner 11 | 908 | 0,5158 % |
| Partner 12 | 836 | 0,4749 % |
| Partner 13 | 799 | 0,4538 % |

### `SUTTONS` — 20.964 Nachrichten, **null** benannte Partner

Eine einzige Zeile: *nicht zugeordnet*, **100,0000 %**.

### `VOTG` — 5.937 Nachrichten, sieben benannte Partner

| Bezeichnung | Anzahl | Anteil |
|---|---:|---:|
| **Partner 1** | **3.500** | **58,9523 %** |
| Partner 2 | 871 | 14,6707 % |
| Partner 3 | 91 | 1,5328 % |
| Partner 4 | 58 | 0,9769 % |
| Partner 5 | 16 | 0,2695 % |
| Partner 6 | 4 | 0,0674 % |
| Partner 7 | 1 | 0,0168 % |
| *nicht zugeordnet* | *1.396* | *23,5136 %* |

### `IBIS` — 4.330 Nachrichten, 50 benannte Partner, **kein** *nicht zugeordnet*

| Bezeichnung | Anzahl | Anteil |
|---|---:|---:|
| Partner 1 | 544 | **12,5635 %** |
| Partner 2 | 497 | 11,4781 % |
| Partner 3 | 389 | 8,9838 % |
| Partner 4 | 345 | 7,9677 % |
| Partner 5 | 320 | 7,3903 % |
| Partner 6 | 305 | 7,0439 % |
| Partner 7 | 249 | 5,7506 % |
| Partner 8 | 181 | 4,1801 % |
| Partner 9 | 149 | 3,4411 % |
| Partner 10 | 142 | 3,2794 % |
| Partner 11 | 138 | 3,1871 % |
| Partner 12 | 126 | 2,9099 % |
| Partner 13 | 116 | 2,6790 % |

## Der Block, wie 10b ihn zeigen wird

| Mandant | Zeile | Partner | Nachrichten | Anteil |
|---|---|---:|---:|---:|
| **NEXANS** | Top 10 | 10 | 134.481 | **76,3880 %** |
| | *nicht zugeordnet* | — | 28.387 | **16,1244 %** |
| | Übrige (105) | 105 | 13.182 | 7,4876 % |
| **SUTTONS** | *nicht zugeordnet* | — | 20.964 | **100,0000 %** |
| | Top 10 | **—** | — | — |
| | Übrige | **—** | — | — |
| **VOTG** | Top 10 | 7 | 4.541 | **76,4864 %** |
| | *nicht zugeordnet* | — | 1.396 | **23,5136 %** |
| | Übrige | **—** | — | — |
| **IBIS** | Top 10 | 10 | 3.121 | **72,0785 %** |
| | Übrige (40) | 40 | 1.209 | **27,9215 %** |
| | *nicht zugeordnet* | **—** | — | — |

**Drei der vier Mandanten haben nicht drei Zeilen.** `SUTTONS` hat eine, `VOTG` und `IBIS` haben
zwei. Bei `VOTG` fehlt „Übrige", weil es nur sieben Partner gibt; bei `IBIS` fehlt „nicht
zugeordnet", weil jeder seiner 192 Prozesse einen bestätigten Partner trägt. **Der Block muss
fehlende Zeilen vertragen** — offener Punkt 62.

## Die Verteilung nach Richtung

| Mandant | `AUSGEHEND` | `EINGEHEND` | *nicht zugeordnet* |
|---|---:|---:|---:|
| **NEXANS** | 47.960 (27,2423 %) | 26.533 (15,0713 %) | **101.557 (57,6865 %)** |
| **SUTTONS** | 0 | 0 | **20.964 (100,0000 %)** |
| **VOTG** | 0 | 0 | **5.937 (100,0000 %)** |
| **IBIS** | 1.019 (23,5335 %) | **3.240 (74,8268 %)** | 71 (1,6397 %) |

> ### Befund 10 der Vorrunde steht unverändert — und er ist jetzt der einzige, der noch steht
>
> Bei `NEXANS` ist die Zahl für *Richtung nicht zugeordnet* **101.557** — Zeichen für Zeichen die
> Zahl des größten Partners. M91 hat das nachgeprüft und aufgeklärt: Die **elf** `NEXANS`-Katalog­
> zeilen ohne Richtung tragen alle denselben Partner. **Nach der Kuratierung sind es immer noch elf**
> (V3: 722 von 733 mit Richtung), und sie tragen immer noch 57,69 % des Monatsvolumens. Die
> Kuratierung vom 24. bis 27.08.2026 hat 505 Zeilen angefasst und **genau diese elf nicht**.
>
> **`VOTG` ist dazugekommen**, und dort ist die Lücke größer: 378 gepflegte Partner, **keine einzige
> Richtung**. Die Richtungsverteilung dieses Mandanten ist zu 100 % *nicht zugeordnet*, obwohl seine
> Partnerverteilung zu 76,49 % zugeordnet ist. Offene Punkte 59 und 60.

## Laufzeit und Pläne

| Mandant | Abfrage | Aufwärmlauf | **beste von fünf** | alle fünf (ms) |
|---|---|---:|---:|---|
| NEXANS | Partner | 181,601 | **179,215** | 180,091 · 180,959 · 180,466 · 181,685 · 179,215 |
| NEXANS | Richtung | 177,774 | **180,546** | 183,526 · 186,330 · 180,680 · 180,546 · 180,859 |
| SUTTONS | Partner | 130,791 | **126,670** | 129,577 · 126,883 · 126,670 · 127,331 · 127,531 |
| SUTTONS | Richtung | 127,994 | **125,950** | 126,529 · 125,950 · 129,950 · 126,913 · 129,257 |
| VOTG | Partner | 135,754 | **136,817** | 137,171 · 138,814 · 137,062 · 146,891 · 136,817 |
| VOTG | Richtung | 134,536 | **133,832** | 142,599 · 135,494 · 133,832 · 139,017 · 135,540 |
| IBIS | Partner | 128,818 | **126,333** | 126,813 · 126,333 · 127,223 · 129,624 · 126,887 |
| IBIS | Richtung | 125,811 | **125,062** | 126,221 · 126,459 · 126,088 · 125,379 · 125,062 |

**125 bis 181 ms, und die Spanne hat wenig mit dem Mandanten zu tun.** `NEXANS` mit 176.050
Nachrichten kostet 179 ms, `IBIS` mit 4.330 kostet 126 ms — Faktor 1,42 bei Faktor 41 an Menge. Der
Grund steht im Plan: Der Bereichszugriff auf `message_rollup` liest die **20.971 Zeilen des ganzen
Fensters**, unabhängig vom Mandanten; nur was danach kommt, hängt an ihm. **Ein kleiner Mandant zahlt
im Verteilungsblock fast so viel wie der größte.**

Bei `NEXANS` und `VOTG` ist der Aufwärmlauf einmal *schneller* als der beste der folgenden fünf
(−1,5 % bzw. −0,8 %) — dasselbe Rauschen wie bei M94.

### Die Pläne (Regel L15)

Die Partnerabfrage hat wegen des `ROW_NUMBER()` eine Ableitungsstufe mehr als die Richtungsabfrage.
`c` steht in allen acht Plänen als `eq_ref` auf `PRIMARY` — der Katalog-Join ist wirklich gemessen.

#### NEXANS · Partnergruppen

```
+----------------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary; Using filesort              |
|    2 | DERIVED     | <derived3> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary                              |
|    3 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    3 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    3 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    3 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    3 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+-----------------------------------------------------------+
| marke                                                     |
+-----------------------------------------------------------+
```

#### NEXANS · Richtung

```
+----------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary; Using filesort              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+---------------------------------------------+
| marke                                       |
+---------------------------------------------+
```

#### SUTTONS · Partnergruppen

```
+-----------------------------------------+
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key                        | key_len | ref                           | rows  | Extra                                                     |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL                       | NULL    | NULL                          | 45840 | Using temporary; Using filesort                           |
|    2 | DERIVED     | <derived3> | ALL    | NULL                               | NULL                       | NULL    | NULL                          | 45840 | Using temporary                                           |
|    3 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1     | Using where; Using index; Using temporary; Using filesort |
|    3 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1     | Using index                                               |
|    3 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 45840 | Using where; Using join buffer (flat, BNL join)           |
|    3 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     | Using where                                               |
|    3 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     |                                                           |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
+------------------------------------------------------------+
| marke                                                      |
+------------------------------------------------------------+
```

#### SUTTONS · Richtung

```
+-----------------------------------+
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key                        | key_len | ref                           | rows  | Extra                                                     |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL                       | NULL    | NULL                          | 45840 | Using temporary; Using filesort                           |
|    2 | DERIVED     | pm         | ref    | PRIMARY,ProjectMandant_Mandant_idx | ProjectMandant_Mandant_idx | 146     | const                         | 1     | Using where; Using index; Using temporary; Using filesort |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY                    | 146     | GlassfishDB.pm.ProjectID      | 1     | Using index                                               |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY                    | 5       | NULL                          | 45840 | Using where; Using join buffer (flat, BNL join)           |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     | Using where                                               |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY                    | 146     | overlord_monitor.r.process_id | 1     |                                                           |
+------+-------------+------------+--------+------------------------------------+----------------------------+---------+-------------------------------+-------+-----------------------------------------------------------+
+----------------------------------------------+
| marke                                        |
+----------------------------------------------+
```

#### VOTG · Partnergruppen

```
+--------------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary; Using filesort              |
|    2 | DERIVED     | <derived3> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary                              |
|    3 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    3 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    3 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    3 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    3 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

#### VOTG · Richtung

```
+--------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary; Using filesort              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+-------------------------------------------+
| marke                                     |
+-------------------------------------------+
```

#### IBIS · Partnergruppen

```
+--------------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary; Using filesort              |
|    2 | DERIVED     | <derived3> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary                              |
|    3 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    3 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    3 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    3 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    3 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+---------------------------------------------------------+
| marke                                                   |
+---------------------------------------------------------+
```

#### IBIS · Richtung

```
+--------------------------------+
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
| id   | select_type | table      | type   | possible_keys                      | key     | key_len | ref                           | rows  | Extra                                        |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
|    1 | PRIMARY     | <derived2> | ALL    | NULL                               | NULL    | NULL    | NULL                          | 45840 | Using temporary; Using filesort              |
|    2 | DERIVED     | r          | range  | PRIMARY                            | PRIMARY | 5       | NULL                          | 45840 | Using where; Using temporary; Using filesort |
|    2 | DERIVED     | p          | eq_ref | PRIMARY,Process_ProjectFK          | PRIMARY | 146     | overlord_monitor.r.process_id | 1     | Using where                                  |
|    2 | DERIVED     | pm         | eq_ref | PRIMARY,ProjectMandant_Mandant_idx | PRIMARY | 292     | GlassfishDB.p.ProjectID,const | 1     | Using where; Using index                     |
|    2 | DERIVED     | pr         | eq_ref | PRIMARY                            | PRIMARY | 146     | GlassfishDB.p.ProjectID       | 1     | Using index                                  |
|    2 | DERIVED     | c          | eq_ref | PRIMARY                            | PRIMARY | 146     | overlord_monitor.r.process_id | 1     |                                              |
+------+-------------+------------+--------+------------------------------------+---------+---------+-------------------------------+-------+----------------------------------------------+
+-------------------------------------------+
| marke                                     |
+-------------------------------------------+
```

## Vorregistrierte Deutung, dagegengehalten

> Vorregistriert: *„M91 hat vor der Kuratierung **83,03 %** für den größten Partner im Tagesfenster
> gemessen und Rang 1 zu Rang 10 mit 108 : 1. Erwartet wird, dass sich daran wenig ändert — die
> Kuratierung mildert die Konzentration nicht, sie verschärft sie. **Bleibt der größte über 50 %**,
> ist Top‑10 die richtige Form und die Balkenlängen brauchen eine nichtlineare Skala oder eine reine
> Zahlendarstellung. … **Fällt die Zeile ‚nicht zugeordnet' bei einem der vier Mandanten über 30 %**,
> ist der Katalog dort weiter zu pflegen, bevor 10b abgenommen werden kann."*

### Die erste Erwartung trifft — für drei von vier

**Der größte Partner hält bei `NEXANS` 57,69 % und bei `VOTG` 58,95 %.** Beide über 50 %, beide
praktisch unverändert gegenüber M91 (58,61 % für `NEXANS` im Monatsfenster). **Die Kuratierung hat
die Konzentration nicht gemildert** — genau wie vorregistriert. Rang 1 zu Rang 10 steht bei `NEXANS`
bei **104 : 1** (101.557 gegen 980), M91 maß 108 : 1.

`SUTTONS` hat gar keinen Partner und fällt aus der Frage heraus.

### Und `IBIS` widerlegt sie

**Bei `IBIS` hält der größte Partner 12,56 %.** Rang 1 zu Rang 10 steht bei **3,8 : 1**. Die zehn
größten decken 72,08 % ab, die übrigen 40 zusammen 27,92 % — **die Zeile „Übrige (40)" wäre mit
1.209 Nachrichten der größte Balken des ganzen Blocks**, mehr als das Doppelte von Rang 1.

> **Das ist Befund 21, und für 10b ist es der unbequemste dieser Runde.** Die Gestaltung, die
> `NEXANS` braucht — ein riesiger Balken, neun kurze, eine Restzeile —, ist bei `IBIS` genau falsch
> herum: dort sind die zehn Balken ähnlich lang und die Restzeile überragt sie alle. **Eine
> Darstellung, die für beide trägt, ist eine Gestaltungsfrage für 10b und keine Entscheidung dieser
> Runde.** Der Auftrag hält das ausdrücklich so fest.

### Die zweite Erwartung trifft — bei genau einem Mandanten

| Mandant | *nicht zugeordnet* | über 30 %? |
|---|---:|:--:|
| NEXANS | 16,12 % | nein |
| VOTG | 23,51 % | nein |
| IBIS | **0,00 %** | nein |
| **SUTTONS** | **100,00 %** | **ja** |

**Bei `SUTTONS` ist der Katalog vor der Abnahme von 10b zu pflegen.** Siebzehn Katalogzeilen, alle
`OFFEN`, keine mit Partner — es ist die kleinste Pflegeaufgabe des Projekts und die einzige, die
zwischen dem heutigen Stand und der Abnahme steht. Offener Punkt 58.

**Die drei anderen sind unter der Schwelle**, und zwei davon deutlich: `IBIS` bei null, `NEXANS` bei
16,12 %. In M91 lagen `SUTTONS` und `VOTG` beide bei 100 % — **einer von beiden ist inzwischen
gepflegt.**

> **Belegvermerk** (Regel L10).
> *Gemessen ist:* die Verteilung über ein 30‑Tage‑Fenster am Anker, für vier Mandanten.
> *Behauptet wird:* Der Katalog ist bei `SUTTONS` vor der Abnahme zu pflegen, bei den anderen drei
> nicht.
> **Die Lücke:** Der Anteil *nicht zugeordnet* hängt am Fenster. Bei `NEXANS` sind die 216 gepflegten
> Zeilen ohne Partner (V3) über den Bestand nicht gleich verteilt; ein anderes Fenster kann einen
> anderen Anteil ergeben. **Was nicht am Fenster hängt, ist der Grund:** `SUTTONS` hat null
> bestätigte Partner, und null bleibt null in jedem Fenster.

### Die Aufteilung von *nicht zugeordnet* nach E‑i — und sie kostet wieder nichts

**E‑i ist entschieden.** Die folgende Aufteilung steht **nur zur Kenntnis**.

| Mandant | (a) keine Katalogzeile | (b) `OFFEN` | (c) `GEPFLEGT` ohne Partner |
|---|---|---|---|
| NEXANS | 0 Prozesse | 0 Prozesse | **216 Prozesse** |
| SUTTONS | 0 | **17 Prozesse** | 0 |
| VOTG | 0 | **12 Prozesse** | 0 |
| IBIS | 0 | 0 | 0 |

**Wie in M91 hat jeder Mandant genau eine der drei Ursachen** — `NEXANS` ausschließlich (c),
`SUTTONS` und `VOTG` ausschließlich (b). Eine getrennte Darstellung ergäbe für keinen von ihnen eine
zweite Zeile. **E‑i kostet auch nach der Kuratierung nichts an Aussagekraft** — und es ist derselbe
Zufall wie damals, nur mit anderen Ursachen: In M91 war `SUTTONS` (a) und `VOTG` (b), heute sind
beide (b).
---

# Die Zahlen, auf die es für 10b ankommt

| Was | Wert |
|---|---:|
| **Verlauf, 48 h / Stunde** (`NEXANS`) | **6,834 ms** |
| Verlauf, 30 Tage / Tag (`NEXANS`) | 149,928 ms |
| **Verlauf, 12 Monate / Monat** (`NEXANS`) | **2.206,854 ms** |
| Verteilung, 48 h (`NEXANS`) | 10,653 ms |
| Verteilung, 30 Tage (`NEXANS`) | 179,583 ms |
| **Verteilung, 12 Monate** (`NEXANS`) | **2.491,443 ms** |
| Kosten je gelesener Rollupzeile, über Faktor 302 an Menge | **5,3 – 11,5 µs** |
| Aufschlag des Katalog-Joins, 48 h → 12 Monate | ×1,56 → **×1,13** |
| **Verteilungsblock, 30 Tage** (`IBIS`, kleinster der vier) | **126,333 ms** |
| **Liste, 30 Tage, ohne Filter** (`NEXANS`) | **1,803 ms** |
| **Liste, 30 Tage, ohne Filter** (`SUTTONS`) | **1.101,280 ms** |
| Liste, 30 Tage, `ueberfaellig` (`NEXANS`) | 6,176 ms |
| Liste, 30 Tage, `ueberfaellig` (`SUTTONS`) | 6,958 ms |
| **Überfällige Zeilen am geltenden Anker, Gesamtbestand** | **538**, alle bei `NEXANS`, alle mit Frist 1.800 s |
| Überfällige Zeilen im 24‑h‑Standardfenster | **1** |
| **48 Delta-Läufe, Summe** | **1.251 ms** (4 – 65 ms je Lauf, erster 112 ms) |
| Volllauf über 48 Stunden | 237 ms |
| **Überzählung inkrementell gegen Volllauf** | **0,0000 %** |
| **Überlapp, den Löschen-und-Neuschreiben schluckt** | **99,5002 %** |
| Größter Partner, 30 Tage | 57,69 % (`NEXANS`) · 58,95 % (`VOTG`) · **12,56 % (`IBIS`)** |
| *nicht zugeordnet*, 30 Tage | 16,12 % (`NEXANS`) · 23,51 % (`VOTG`) · 0,00 % (`IBIS`) · **100,00 % (`SUTTONS`)** |

---

# Was diese Runde nicht zeigt

1. **Sie zeigt den nächtlichen Sprung nicht** (M96). Sie zeigt, dass eine Folge von Delta-Läufen auf
   einem **unveränderten** Bestand zeilengleich mit einem Volllauf ist. Das Phänomen, um das es
   fachlich geht — ein Status, der sich ändert, nachdem sein Eimer gerollt wurde —, kann auf der
   Testkopie nicht eintreten: `RUNNING` kommt null Mal vor, `MatchInterchange` läuft nicht sichtbar
   (M31‑3), und der Bestand wird nicht fortgeschrieben.
2. **Sie zeigt den Kaltlauf nicht.** `FLUSH TABLES` steht `monitor_read` nicht zu; der Puffer fasst
   `Message` 9,26‑mal. Gemessen ist der Warmfall — mit Ausnahme des ersten Delta-Laufs der JVM in
   M96 (112 ms gegen 4–65 ms), und der misst den kalten Abfrageplan, nicht die kalte Platte. **Für
   die 12‑Monats‑Ansicht ist das eine echte Lücke:** 2,2 s warm, und die Zeitgrenze des Lese-Pools
   liegt bei 10 s.
3. **Sie zeigt nichts über die Produktion.** Alle Zahlen stammen von der Testkopie. Der Anker liegt
   im dichtesten Teil des Bestands; ein Anker im Frühjahr 2026 ergäbe für jeden Mandanten null
   belegte Eimer bei 48 Stunden.
4. **Sie legt die Schwelle für das Standardfenster nicht fest** (M95). Sie liefert die Zahlen und
   einen Vorschlag mit zwei Bedingungen; die Festlegung gehört in 10b.
5. **Sie misst die Vergleichszahl nicht.** Sie ist am 27.08.2026 verworfen worden.
6. **Sie misst „Unquittiert" nicht** (E‑d, nicht im MVP).
7. **Sie misst die Kachelabfragen nicht.** M94 misst Verlauf und Verteilung; was die drei Kacheln
   *zusätzlich* kosten, ist nicht erhoben. Das 500‑ms‑Budget gilt der ganzen Landingpage, und diese
   Runde hat nur zwei ihrer Bestandteile gemessen.
8. **Sie misst den Endpunkt nicht, sondern seine Abfragen.** Weder Spring noch jOOQ noch die
   Serialisierung sind enthalten — mit der einen Ausnahme M96, die durch die ganze Anwendung läuft.
9. **Sie sagt nicht, wo genau der Plan der Liste kippt** (M97). Zwischen 7 und 30 Tagen liegen 23
   ungemessene Fensterbreiten. Für `zeitraum` ist das folgenlos (es kennt nur `24h`, `7d`, `30d`);
   für den Modus `von`/`bis`, der beliebige Breiten zulässt, nicht.
10. **Sie misst die Anzeige nicht.** Ob ein Diagramm mit 29 von 30 belegten Tagen und vier
    Nachrichten je Tag „trägt", ist eine Gestaltungsfrage und keine Messgröße. M95 liefert nur die
    Zahlen, an denen sie sich entscheiden lässt.

---

# Abweichungen vom Rahmen, einzeln benannt

| # | Abweichung | Begründung |
|---|---|---|
| **A1** | **Client `mysql.exe` 8.0.46 mit `--ssl-mode=DISABLED` statt `mariadb` mit `--skip-ssl`** | `--skip-ssl` ist die MariaDB-Schreibweise; der Workbench-Client bricht damit mit `unknown option` ab, **vor** der Sitzung. Ein MariaDB-Client 12.3.2 liegt auf dem Rechner, ist aber **nicht** verwendet worden: Ein Clientwechsel kostete die Vergleichbarkeit mit allen Runden seit M32 — und V1 dieser Runde beruht genau darauf. Dieselbe Abweichung wie in M80, M83 und M86–M92, mit derselben Begründung |
| **A2** | **Dreizehn Sitzungen statt acht** | M94 ist in Verlauf und Verteilung geteilt (Sitzungsplan sieht das vor), M97 in Vorprobe, zwei Mandanten und eine Gegenprobe, M98 in zwei Sitzungen zu je zwei Mandanten. Grund ist `profiling_history_size`: Bei 100 Statements je Sitzung fiele der Anfang einer größeren Sitzung lautlos aus `SHOW PROFILES`. Dazu die abgebrochene Sitzung 8 und ihr Nachlauf 8b |
| **A3** | **Der Messläufer für M96 ist eine JUnit-Klasse und keine `.sql`-Datei** | Der Auftrag gibt das ausdrücklich frei („Ein Testläufer oder eine Klasse unter `src/test` darf entstehen"). Sie heißt `MessungM96DbIT`, trägt `@Tag("db")` und ist mitcommittet. Ihre Ausgabe ist der Maven-Lauf und nicht eine `-t`-Tabelle; die Werte stehen als `M96 \| schlüssel \| wert`-Zeilen darin |
| **A4** | **Die schreibenden Sitzungen laufen *nach* den lesenden, nicht an ihrer Stelle im Sitzungsplan** | Die Übersetzung des Messläufers schreibt `backend/target/classes` unter der laufenden Entwicklungsinstanz neu. Sie ist deshalb ans Ende gelegt und erst nach Zustimmung des Auftraggebers gefahren worden; die Instanz ist danach neu gestartet worden. **Unschädlich**, weil 5a, 5b und 8b ausschließlich `overlord_monitor` anfassen und keine lesende Sitzung aus `message_rollup` etwas gelesen hat, das M96 verändert hätte — der Bereich ist am Ende Zeichen für Zeichen derselbe |
| **A5** | **Sitzung 8 ist abgebrochen und als 8b nachgefahren worden** | Das mehrtabellige `DELETE r FROM … LEFT JOIN …` mit Alias braucht eine Vorgabedatenbank; ohne sie bricht der Client mit `ERROR 1046 (3D000) at line 59: No database selected` ab. **Der Abbruch lag vor jedem Schreibvorgang** — 8‑1 und 8‑2 sind reine `SELECT` —, der Zustand war also unberührt. Behoben durch ein vorangestelltes `USE overlord_monitor;`, im Skript kommentiert. Die Skriptdatei trägt die korrigierte Fassung; die abgebrochene ist nicht aufgehoben |
| **A6** | **Der Cursor steht als Sitzungsvariable, nicht als Literal** (M97) | G1: Die `MessageID` darf weder in der Skriptdatei noch in der Ausgabe stehen. Die Form im Statement ist dieselbe, die jOOQ bindet — ein Parameter und kein Literal. Die Pläne zeigen, dass die Variable die Bereichsanalyse nicht verhindert: `key_len = 151` in Fall B, also beide Spalten |
| **A7** | **Die Laufzeit von M97 ist an einer Hülle gemessen, nicht am nackten Statement** | Das nackte Statement liefert `MessageID`, `ProcessID`, `ProcessName` und `ProjectName` (G1). Der Plan ist deshalb am nackten Statement erhoben (`EXPLAIN` führt nicht aus), die Laufzeit an derselben Abfrage in einer aggregierenden Hülle. **Beide Pläne stehen nebeneinander und sind Zeile für Zeile gleich**, bis auf die eine `<derived2>`-Zeile der Hülle. Der Unterschied zur Wirklichkeit ist die Übertragung von 51 Zeilen an den Client |
| **A8** | **Der Cursor für M97 Fall D bei `SUTTONS` stammt aus der ungefilterten Liste** | Die überfällige Liste von `SUTTONS` ist leer; eine 50. Zeile gibt es dort nicht. Der Rückfall steht im Skript als `COALESCE` und ist dort begründet: Der Cursor ist eine Position in der Sortierfolge und trägt keine Berechtigung. **Für den Planvergleich ist er gleichwertig** — und das Ergebnis bleibt null Zeilen |
| **A9** | **M94-Verteilung ist in einer aggregierenden Hülle gemessen** | Wie A7 und aus demselben Grund. Zusätzlich: Die Hülle ist es, die den Katalog-Join überhaupt sichtbar macht — ohne verwendete Katalogspalte optimiert MariaDB ihn weg (Befund 9 der Vorrunde) |
| **A10** | **Die Runde schreibt in mehr als eine Datei** | Neben `docs/messungen-schritt10b.md` sind das `scripts/messung-schritt10b/*.sql` (elf Sitzungsdateien), eine Zeile in `.gitignore`, der Messläufer unter `backend/src/test` und **ein Eintrag in `docs/README.md`**. Alle vier sind vom Auftrag vorgesehen; `docs/README.md` ist ausdrücklich **nicht** gesperrt, anders als in der Vorrunde (dortige Abweichung A12) |
| **A11** | **Ein Commit je Block statt je Sitzung** | Der Auftrag verlangt „Nach jeder Sitzung ein Commit". Gefahren sind drei Commits: die Sitzungsdateien und der Messläufer, die Ergebnisdatei, der README-Eintrag. Grund ist, dass die Ergebnisdatei erst nach der letzten Sitzung steht — ein Commit je Sitzung hätte elfmal dieselbe unfertige Datei festgehalten. **Die Sitzungsdateien selbst sind vollständig und einzeln nachvollziehbar** |
| **A12** | **`SET max_statement_time = 60` ist nie gerissen worden** | Keine Abweichung, sondern ihre Abwesenheit — hier vermerkt, weil der Rahmen den Fall ausdrücklich vorsieht („Wird sie erreicht, ist das der Befund"). Das teuerste Statement der Runde liegt bei 6,825 s, also bei 11,4 % der Grenze |
| **A13** | **Die Nummernprobe liefert null statt des erwarteten einen Treffers** | Ausführlich im Kasten unter „Nummernvergabe". Der Ausdruck dieser Runde ist enger als der der M93-Runde und trifft die Fundstelle in Z. 21 nicht. Die Fundstelle ist trotzdem gelesen worden |
| **A14** | **Die Sicherungstabelle ist in die jOOQ-Codegenerierung geraten und wieder daraus entfernt worden** | Der Lauf des Messläufers (`./mvnw test-compile failsafe:integration-test`) durchläuft die Phase `generate-sources`, und **die Codegenerierung liest die Datenbank, nicht die Migrationsdateien.** Zu diesem Zeitpunkt stand `message_rollup_m96_sicherung` dort. Entstanden sind zwei neue Klassen (`MessageRollupM96Sicherung`, `…Record`) und drei geänderte (`Tables`, `Keys`, `OverlordMonitor`, zusammen 16 Zeilen). **Alle fünf sind zurückgesetzt worden**, bevor irgendetwas committet wurde; `git status` ist an dieser Stelle sauber. **Die Lehre gehört hierher, weil sie beim nächsten Mal wieder zuschlägt:** Wer eine Hilfstabelle in `overlord_monitor` anlegt und danach einen Maven-Lauf ohne `-Djooq.codegen.skip=true` fährt, bekommt sie ins eingecheckte Modell. M89 der Vorrunde hatte dasselbe Muster (`message_rollup_probe`), dort ist es nicht aufgetreten, weil die Probetabelle nie einen Build gesehen hat |

---

# Befunde, die in keine vorformulierte Zeile passten

Fortlaufend an [`messungen-schritt10.md`](messungen-schritt10.md) anschließend, wo der gleichnamige
Abschnitt sechs bis vierzehn führt. **Fünfzehn bis dreiundzwanzig.**

| # | Messung | Der Befund |
|---|---|---|
| **15** | M94 | **Der Katalog-Join ist nicht der Treiber, und sein Aufschlag *fällt* mit der Fensterbreite** — ×1,56 bei 48 Stunden, ×1,13 bei zwölf Monaten. Er ist ein `eq_ref` je gelesener Zeile, also ein konstanter Anteil; alles andere wächst. **Ihn wegzulassen brächte bei zwölf Monaten 5 bis 13 %, und das Problem bliebe** |
| **16** | M94 / M95 | **Die Zeilenschätzung auf `message_rollup` ist bei kleinen Bereichen exakt (929 gegen 929), bei mittleren um 118,6 % zu hoch und bei großen um 40,3 % zu *niedrig*.** Befund 8 der Vorrunde beschreibt für `Message` eine Schätzung, die bei großen Bereichen um Faktor 2 zu **hoch** liegt — beim Rollup kippt das Vorzeichen |
| **17** | M95 | **Der Anteil belegter Eimer trennt die Mandanten nicht.** `NEXANS`, `SUTTONS` und `VOTG` liegen bei allen drei Paaren bei 100 %, obwohl zwischen ihnen Faktor 25 an Menge liegt. `WOC` hat 29 von 30 Tagen belegt — mit 3,9 Nachrichten am Tag. **Der Anteil sagt, ob ein Diagramm Punkte hat, nicht ob es etwas zeigt** |
| **18** | M97 | **`ueberfaellig` erzeugt drei verschiedene Pläne, je nach Mandant und Fenster** — `MessageLastUpdateIDX` ohne den Parameter, `MessageStatusIDX` mit ihm, und bei `SUTTONS` über 24 Stunden und 7 Tage `ProjectMandant_Mandant_idx`. Bei `NEXANS` kostet der Parameter Faktor 3,4, bei `SUTTONS` **spart** er Faktor 158 |
| **19** | M97 | **Die bestehende Listenabfrage kippt bei kleinen Mandanten zwischen sieben und dreißig Tagen auf `ProejctIDIDX` und kostet dort das 167‑Fache** — 1.101,280 ms gegen 6,597 ms bei `SUTTONS`. `messungen-schritt4.md` L4 misst 24 Stunden (33,978 ms); ein 30‑Tage‑Fenster für einen kleinen Mandanten ist dort nie gemessen worden. **Das ist ein Befund über den gebauten Endpunkt, nicht über 10b** |
| **20** | M96 | **Der Überlapp, den Löschen-und-Neuschreiben schluckt, beträgt 99,50 %.** 48 Delta-Läufe lesen über 95 Stundeneimer **23.948** Nachrichten, im Fenster stehen **12.004**. Ein Job, der hochzählte statt zu ersetzen, käme nach zwei Tagen auf fast das Doppelte |
| **21** | M98 | **Bei `IBIS` ist die Verteilung flach: größter Partner 12,56 %, Rang 1 zu Rang 10 wie 3,8 : 1, und die Zeile „Übrige (40)" wäre mit 27,92 % der größte Balken des Blocks.** Die Gestaltung, die `NEXANS` braucht, ist dort genau falsch herum |
| **22** | V3 | **Der Katalog ist am Morgen dieser Runde um 505 Zeilen erweitert worden**, rund drei Stunden vor der ersten Messung, und trägt seither eine dritte Vorschlagsherkunft `REGEL_B` (264 Zeilen), die es in V2 der Vorrunde nicht gab |
| **23** | V3 / M98 | **Sechs der sieben Mandanten ohne gepflegte Katalogzeile sind seit M91 vollständig gepflegt worden — `SUTTONS` nicht.** Er trägt 17 Katalogzeilen, von denen keine einzige gepflegt ist; `WOC` hat weiterhin überhaupt keine. **Und `VOTG` hat 378 bestätigte Partner und keine einzige Richtung** |

---

# Das teuerste Statement der Runde

**Die Ankerabfrage aus V5, 6.825,360 ms** — ein voller Durchlauf über `Message` mit
`GROUP BY date(…)` und `HAVING count(distinct …) >= 3`.

**Sie ist keine Messabfrage und kein Anwendungscode im Anfragepfad.** Sie steht in
[`datenzugriff.md`](datenzugriff.md) §6 und läuft **einmal beim Start und ausschließlich im Profil
`dev`**. In Produktion ist die Anwendungsuhr die Systemuhr; dort wird nichts gelesen. Die dort
genannten „rund 6,9 Sekunden" sind damit unabhängig bestätigt.

## Die fünfundzwanzig teuersten Statements

| # | Laufzeit | Sitzung | Statement |
|---:|---:|---|---|
| 1 | **6.825,360 ms** | 1 | V5, Ankerabfrage |
| 2 | 4.394,027 ms | 4 | M95 P3, Ergebnislauf |
| 3 | 4.318,604 ms | 4 | M95 P3, Aufwärmlauf |
| 4 | 4.300,604 ms | 4 | M95 P3 |
| 5 | 4.288,303 ms | 4 | M95 P3 |
| 6 | 4.288,043 ms | 4 | M95 P3 |
| 7 | 4.269,307 ms | 4 | M95 P3 |
| 8 | **4.256,655 ms** | 4 | M95 P3, **beste von fünf** |
| 9 | 3.132,992 ms | 6a | Vorprobe, offene Zeilen je Mandant über 30 Tage |
| 10 | 2.517,506 ms | 3 | M94 Verteilung P3 NEXANS, Aufwärmlauf |
| 11 | 2.515,501 ms | 3 | M94 Verteilung P3 NEXANS |
| 12 | 2.514,055 ms | 3 | M94 Verteilung P3 NEXANS, Ergebnislauf |
| 13 | 2.511,430 ms | 3 | M94 Verteilung P3 NEXANS |
| 14 | 2.506,107 ms | 3 | M94 Verteilung P3 NEXANS |
| 15 | 2.506,007 ms | 3 | M94 Verteilung P3 NEXANS |
| 16 | **2.491,443 ms** | 3 | M94 Verteilung P3 NEXANS, **beste von fünf** |
| 17 | 2.250,386 ms | 2 | M94 Verlauf P3 NEXANS, Ergebnislauf |
| 18 | 2.239,677 ms | 2 | M94 Verlauf P3 NEXANS |
| 19 | 2.233,335 ms | 2 | M94 Verlauf P3 NEXANS |
| 20 | 2.231,067 ms | 2 | M94 Verlauf P3 SUTTONS, Ergebnislauf |
| 21 | 2.221,181 ms | 2 | M94 Verlauf P3 NEXANS, Aufwärmlauf |
| 22 | 2.215,985 ms | 2 | M94 Verlauf P3 NEXANS |
| 23 | 2.214,201 ms | 2 | M94 Verlauf P3 NEXANS |
| 24 | **2.206,854 ms** | 2 | M94 Verlauf P3 NEXANS, **beste von fünf** |
| 25 | 1.967,515 ms | 2 | M94, Zählung der Rollupzeilen P3 NEXANS |

**386 Statements sind insgesamt gemessen worden** (ohne Marken, `SET` und `SHOW`), verteilt auf elf
`SHOW PROFILES`-Blöcke.

---

# Widerspricht ein Befund einer Entscheidung?

Jede Entscheidung aus der Tabelle des Auftrags, einzeln geprüft. **Wo ein Befund einer Entscheidung
widerspricht, ist er gemeldet und nicht aufgelöst.**

| # | Entscheidung | Steht sie? |
|---|---|---|
| **Kacheln** | Nachrichten · Fehler *(aufklappbar)* · Überfällig *(eigene Farbrolle, plus „insgesamt")* | **Ja.** M96 stützt den Namen „Nachrichten" (Überzählung null). **Ein Hinweis ohne Widerspruch:** Die Kachel „Überfällig" zeigt im 24‑Stunden‑Standardfenster **eine** Zeile bei `NEXANS` und null bei allen anderen; die zweite Zahl („insgesamt") ist die einzige, die auf dieser Testkopie überhaupt etwas anzeigt (538). Das ist seit Schritt 4 bekannt und kein neuer Befund |
| **Verlauf: feste Paare** | 48 h/Stunde, 30 Tage/Tag, 12 Monate/Monat | **Die Paare selbst stehen.** Widersprochen wird nicht der Wahl der Paare, sondern der Grundlage, auf der sie ruhen — siehe E‑b |
| **Standardfenster richtet sich nach dem Mandanten** | reicht ein Paar nicht, startet die Ansicht beim nächstweiteren | **Ja, und M95 belegt den Bedarf**: Bei 48 Stunden tragen vier Mandanten null Eimer und drei weitere unter 40 %. **Aber das Kriterium trägt nicht allein** (Befund 17): `WOC` erfüllt bei 30 Tagen 96,67 % mit 3,9 Nachrichten je Tag. Kein Widerspruch zur Entscheidung, wohl aber zu der stillen Annahme, der Anteil belegter Eimer genüge als Schwelle |
| **Vergleichszahl: keine** | Die Frage „ist gerade etwas anders?" beantwortet allein das Diagramm | **Nicht berührt.** Diese Runde misst sie nicht |
| **Verteilung: ein Block, Top‑10 plus zwei Restzeilen** | „Übrige (n)" und „nicht zugeordnet" getrennt | **Die Form steht, die stille Annahme dahinter nicht.** Bei drei der vier gemessenen Mandanten gibt es **nicht** drei Zeilen: `SUTTONS` hat eine, `VOTG` und `IBIS` je zwei. Und bei `IBIS` wäre „Übrige (40)" der größte Balken (Befund 21). **Beides ist eine Gestaltungsfrage für 10b und keine Entscheidung dieser Runde** — gemeldet, nicht aufgelöst |
| **Liste: „Zuletzt aufgefallen", im Zeitfenster, neueste zuerst** | | **Ja.** M97 misst genau diese Form; sie liefert bei `NEXANS` zwei volle Seiten und bei `SUTTONS` null Zeilen |
| **Endpunkt: einer** | nur die beiden Überfällig-Zahlen dürfen „nicht ermittelbar" liefern | **Berührt, und der Befund gehört gemeldet.** Ein einziger Endpunkt, der die Zwölf-Monats-Ansicht liefert, bringt Verlauf **und** Verteilung mit — zusammen 4,7 s bei `NEXANS`, wenn sie nacheinander laufen. Das Budget ist 500 ms für die ganze Landingpage. **Die Entscheidung „ein Endpunkt" ist davon nicht widerlegt** (er könnte beide Abfragen nebenläufig fahren, und die 48‑Stunden‑Ansicht kostet zusammen 17,5 ms), **aber sie ist auf der Monatsebene nicht ohne Weiteres tragfähig.** Sie hängt an derselben Zahl wie E‑b |
| **Leerzustand: ein Text** | | **Ja, und M95 zeigt, wann er greift:** `EDITIONLINGERI` hat bei keinem der drei Paare einen einzigen belegten Eimer, obwohl seine neun Prozesse seit dem 27.08.2026 gepflegt sind |
| **E‑a** — Schlüssel `(stunde, process_id, message_status)` | | **Ja.** Die Mandantenkette und der Katalog-Join zur Lesezeit sind in zwölf Plänen `eq_ref` auf `PRIMARY` und tragen nicht die Kosten. Was teuer ist, ist die Zeilenzahl des Rollups selbst |
| **E‑b** — nur Stundenebene materialisiert | | **⚠️ Nein — und das ist der Befund dieser Runde.** Die Zwölf-Monats-Ansicht kostet 2,207 s (Verlauf) bzw. 2,491 s (Verteilung) bei `NEXANS`, also das 5,5‑ bis 6,2‑Fache der vorregistrierten 400‑ms‑Schwelle. **Nach der vorregistrierten Lesart trägt E‑b nicht.** Eine materialisierte Tages- oder Monatsebene ist dem Auftraggeber vorzulegen. **Diese Runde legt sie nicht vor** — offener Punkt 55 |
| **E‑c** — Überfällig live | | **Ja.** Die Live-Abfrage kostet am geltenden Anker 4,078 bis 6,958 ms und läuft über `MessageStatusIDX` auf 539 von 3,34 Mio. Zeilen |
| **E‑g** — Rohwert im Rollup, Einordnung beim Lesen | | **Ja.** Der Verlauf gruppiert je Eimer und Rohstatus; die Einordnung kostet dort nichts, weil sie erst danach greift |
| **E‑h** — im Fenster | | **Ja.** Alle Messungen dieser Runde sind Fenstermessungen |
| **E‑i** — ein Eimer für „nicht zugeordnet" | | **Ja, und sie kostet weiterhin nichts.** Jeder der vier gemessenen Mandanten hat genau **eine** der drei Ursachen; eine getrennte Darstellung ergäbe für keinen eine zweite Zeile. **Wie in M91 ist das ein Befund zugunsten von E‑i, aber ein zufälliger** — er gilt für diesen Katalogstand |
| **E‑j** — ein neuer Parameter | | **⚠️ Der Wortlaut trägt nicht.** `ueberfaellig` ist kein Filter auf der bestehenden Abfrage, sondern **eine zweite Abfrageform im selben Endpunkt**: Er zieht den Treiber auf `MessageStatusIDX`, macht die Sortierung zum `filesort` und entwertet den Cursor (`key_len` bleibt 123 statt 151). **Genau der Fall, den die Vorregistrierung von M97 benannt hat.** Die Entscheidung, ihn als Parameter zu bauen, ist davon nicht widerlegt — was er ist, gehört aber benannt, bevor er gebaut wird. Offener Punkt 56 |

---

# Offene Punkte

Nummerierung im Anschluss an den projektweit höchsten Stand (**54**, in [`rollup.md`](rollup.md)
§13 Z. 722).

55. **E‑b trägt bei zwölf Monaten nicht — und das ist eine Vorlage an den Auftraggeber.** Die
    Zwölf-Monats-Ansicht kostet 2.206,854 ms (Verlauf) bzw. 2.491,443 ms (Verteilung) bei `NEXANS`
    und 1.490,789 bzw. 1.566,861 ms bei `SUTTONS`. Die vorregistrierte Schwelle liegt bei 400 ms,
    das Budget der ganzen Landingpage bei 500 ms. **Die naheliegende Abhilfe ist eine materialisierte
    Tages- oder Monatsebene**; sie ist weder entworfen noch gemessen. Was gemessen ist: Die Kosten
    hängen **linear an der Zahl gelesener Rollupzeilen** (5,3 bis 11,5 µs über Faktor 302 an Menge),
    und der Katalog-Join trägt bei zwölf Monaten nur 5 bis 13 % davon. **Eine Monatstabelle trüge
    über den *ganzen* Bestand 11.957 Zeilen** (M87 Variante 4) — weniger, als die
    Zwölf-Monats-Ansicht heute allein für ihr Fenster liest (280.186). Die Entscheidung gehört dem
    Auftraggeber; **diese Runde entscheidet sie nicht**.

    > ### ✔ Erledigt am 31.08.2026 — beide Ebenen sind gebaut und gemessen
    >
    > **Der Auftraggeber hat am 27.08.2026 entschieden.** Beide in diesem Punkt genannten Ebenen
    > sind inzwischen gebaut:
    >
    > | | gebaut | gemessen | Zwölf-Monats-Ansicht, `NEXANS` |
    > |---|---|---|---|
    > | **Tagesebene** `message_rollup_tag` | `V10`, 27.08.2026 | [`rollup.md`](rollup.md) §9a | 767,128 / 908,539 ms |
    > | **Monatsebene** `message_rollup_monat` | `V12`, 31.08.2026 | [`rollup.md`](rollup.md) §9d, **M107** | **65,350 / 88,672 ms** |
    >
    > **Das Tor bei 700 ms ist damit offen, mit Faktor 7,9 Luft.** Die Tagesebene allein hat es
    > nicht geöffnet — genau dafür war das Tor da.
    >
    > **Und die Vermutung dieses Punktes hat gehalten, ohne dass sie hätte halten müssen.** Er
    > sagt: *„Die Kosten hängen linear an der Zahl gelesener Rollupzeilen."* M107 hat das über die
    > **dritte** Ebene bestätigt — 6,77 µs je Monatszeile gegen 7,41 µs je Tageszeile bei `NEXANS`,
    > beide mitten im hier genannten Band von 5,3 bis 11,5 µs. **Die Ansicht wird schneller, weil
    > sie weniger liest, und um genau den Faktor, um den sie weniger liest.**
    >
    > **Eine Zahl dieses Punktes ist dabei falsch benutzt worden, und das gehört hierher:** Die
    > 11.957 Zeilen sind der **Gesamtbestand** der Monatsebene, nicht das, was ein
    > Zwölf-Monats-Fenster liest. Letzteres sind **9.649** (M87‑5, Jahresscheibe 2025, Variante 4).
    > Wer die 11.957 gleichmäßig auf 22 Monate verteilt und mit zwölf multipliziert, landet bei
    > rund 6.500 — und diese Mittelung ist auf diesem Bestand falsch: 2024 trägt drei Monate, 2026
    > ist praktisch leer, **2025 trägt allein 9.649**. Der Satz in diesem Punkt ist trotzdem
    > richtig, denn er vergleicht Gesamtbestand mit Fenster und sagt das auch.
56. **`ueberfaellig` ist eine zweite Abfrageform und kein Filter.** Er wechselt den Treiberindex,
    erzwingt ein `filesort` und entwertet den Cursor. Das ist kein Fehler und kein Grund, ihn nicht
    zu bauen — aber es gehört in [`nachrichtenliste.md`](nachrichtenliste.md), bevor er gebaut wird,
    und es heißt, dass die Cursor-Messung aus M4/L8 für ihn **nicht** gilt.
57. **Die bestehende Listenabfrage kippt bei kleinen Mandanten über 30 Tage auf einen Plan, der das
    167‑Fache kostet** (`SUTTONS`: 1.101,280 ms gegen 6,597 ms über sieben Tage). **Das betrifft
    `GET /api/nachrichten` heute**, nicht erst 10b — `zeitraum=30d` ist ein zulässiger Wert. Zu
    prüfen ist, ob es weitere Mandanten betrifft (acht sind ungemessen) und ob der Modus `von`/`bis`
    mit beliebigen Breiten schlimmer trifft. Die naheliegende Gegenmaßnahme ist ein
    `optimizer_switch`-Eingriff oder ein Index-Hinweis; **beides ist ungemessen**, und
    `STRAIGHT_JOIN` ist nach M42 ausgeschlossen.
58. **`SUTTONS` trägt keine einzige gepflegte Katalogzeile.** Siebzehn Zeilen, alle `OFFEN`, keine
    mit Partner; die Verteilung ist dort zu 100 % *nicht zugeordnet*. Nach der vorregistrierten
    Lesart von M98 ist **der Katalog dort zu pflegen, bevor 10b abgenommen werden kann**. Es ist die
    kleinste Pflegeaufgabe des Projekts — siebzehn Zeilen.
59. **`VOTG` hat 378 bestätigte Partner und keine einzige Richtung.** Die Partnerverteilung ist dort
    zu 76,49 % zugeordnet, die Richtungsverteilung zu 0 %. Der Umschalter Partner ⇄ Richtung zeigt
    bei diesem Mandanten auf der einen Seite eine brauchbare Verteilung und auf der anderen eine
    einzige Zeile.
60. **Die elf `NEXANS`-Katalogzeilen ohne Richtung aus Befund 10 stehen unverändert.** Die
    Kuratierung vom 24. bis 27.08.2026 hat 1.486 Zeilen angefasst und genau diese elf nicht. Sie
    tragen weiterhin 57,69 % des Monatsvolumens. **Elf Zeilen, und die Richtungsverteilung des
    größten Mandanten springt von 42,3 % auf 100 % Abdeckung.**
61. **Die Schwelle für das Standardfenster braucht zwei Bedingungen, nicht eine.** Der Anteil
    belegter Eimer allein lässt `WOC` mit 3,9 Nachrichten je Tag als tragfähig durchgehen. Der
    Vorschlag steht in M95; **die Festlegung gehört in 10b**, und die zweite Bedingung ist eine
    gewählte Zahl und keine gemessene.
62. **Der Verteilungsblock muss fehlende Restzeilen vertragen.** Bei drei von vier gemessenen
    Mandanten gibt es nicht drei Zeilen. Und bei einer flachen Verteilung wie `IBIS` überragt
    „Übrige (n)" jeden einzelnen Balken. Beides ist Gestaltung und gehört in 10b.

---

# Messrunde 10b‑4 — *Überfällig* widerlegt, *Läuft* und *Wartend* gebaut *(03.09.2026)*

**Eine eigene Runde in derselben Datei.** Sie gehört zu Schritt 10b und misst vier Dinge, die es
vorher nicht gab; die Runde M94–M98 oben bleibt unberührt und wird nicht nachgerechnet.

## Die Nummern — Regel V1 angewandt, und der Auftrag lag daneben

Der Auftrag nennt als höchste bekannte vergebene Nummer **M126** ([`process-view.md`](process-view.md) §21)
und schlägt M127 bis M130 vor. **Alle vier sind vergeben.** Erhoben über alle Markdown-Dateien in
`docs/` und im Wurzelverzeichnis:

| | |
|---|---|
| **M127–M130** | vergeben in [`process-view.md`](process-view.md) |
| **M131–M141** | vergeben in [`dunkelmodus.md`](dunkelmodus.md) (Schritte 11a und 11b, 03.09.2026) |
| **höchste tatsächlich vergebene** | **M141** |
| **vergeben in dieser Runde** | **M142 bis M145** |

Dasselbe gilt für die Entscheidungen: Der Auftrag nennt **E‑56** als höchste, tatsächlich ist es
**E‑70** ([`dunkelmodus.md`](dunkelmodus.md)). Diese Runde vergibt **E‑71 bis E‑77**. Und für die
offenen Punkte: die höchste vergebene ist **129** ([`dunkelmodus.md`](dunkelmodus.md) §15), diese
Runde vergibt ab **130**.

> **Das ist der zweite Fall dieser Art in Folge.** [`dunkelmodus.md`](dunkelmodus.md) §15 hält
> denselben Befund für seine eigene Runde fest: die dort vorgeschlagenen Nummern kamen im Bestand
> bereits vor. Die Nummern eines Auftrags sind Vorschläge; die Erhebung vor der Vergabe ist keine
> Formalie, sondern die einzige Sicherung gegen zwei Messungen mit derselben Kennung.

## Der Rahmen

Wie in der Runde oben: Client `mysql.exe` 8.0.46 aus MySQL Workbench mit `--ssl-mode=DISABLED`,
Benutzer `monitor_read`, ausschließlich `SELECT`/`SET`/`EXPLAIN`/`SHOW` (Regel S1). Ein
Aufwärmlauf, dann die **beste von fünf**. Der `EXPLAIN` läuft über das **gerenderte** Statement mit
Literalen. Zwei Mandanten, darunter ein kleiner (Regel L7): `NEXANS` und `SUTTONS`.

`jetzt` ist der Anker der Anwendungsuhr im Profil `dev`, **`2025-12-30 04:09:47`** (M9), als
Literal — kein `NOW()` (Regel Z1).

---

## Die vorregistrierten Deutungen — eingetragen **vor** dem ersten Lauf

**Dieser Abschnitt ist vor der ersten Abfrage dieser Runde geschrieben worden.** Was hier steht,
ist das Blatt, gegen das die Ergebnisse gehalten werden; es wird nachträglich **nicht**
umformuliert.

### M142 — trägt `SOSAction` das Wort, und was kostet es?

**Die Frage.** Wie viele Zeilen hat `SOSAction`, was kosten `LIKE '%SUSPEND%'` und
`LIKE '%WAITUNTIL%'` darüber, und tragen die **geplanten** Bausteine der 538 wartenden Nachrichten
das Wort?

**Erwartet:** `SOSAction` ist Stammdaten. M8 hat sie schon einmal vollständig mit einem `LIKE` über
`SOSActionServiceProperties` gruppiert und dafür **70,9 ms** gebraucht, bei **3.944** Zeilen
(11 + 2 + 3.743 + 186 + 1 + 1 aus der dortigen Tabelle). Beides ist hier nachzuzählen und nicht zu
übernehmen. Erwartet wird eine Zeilenzahl in derselben Größenordnung und ein Aufwand **deutlich
unter** den 70,9 ms, weil die hier gebaute Fassung ein `EXISTS` mit Mandantenkette ist und beim
ersten Treffer abbrechen darf.

> **Abbruchkriterium: mehr als 200 ms.** Dann ist §3.5 nicht so zu bauen, und der Schritt hält an.

**Trägt der geplante Ablauf das Wort nicht**, ist die Übertragung von M29 widerlegt und §3.5
ebenfalls offen. M29 (4) hat `SUSPEND` **und** `WAITUNTIL` bei **allen 538** gefunden — aber in
`MessageAction.SOSActionServiceProperties`, also im **ausgeführten** Baustein. Diese Abfrage liest
`SOSAction.SOSActionServiceProperties`, den **geplanten**.

> ⚠️ **Die Lücke, die vorher zu benennen ist, und sie ist größer als „538".** M29 (4) hat neben den
> beiden Marken auch gemessen: **eine einzige** `SOSActionID` und **ein einziger** Ablauf über alle
> 538 Zeilen. Die Nachprüfung am geplanten Baustein hängt damit an **einer** `SOSAction`-Zeile,
> nicht an 538. Sie kann die Übertragung **widerlegen**; bestätigen kann sie sie nur für diese eine
> Gestalt.

**Warum `SUSPEND` und nicht `WAITUNTIL`:** M29 hat beide bei allen 538 gefunden. `SUSPEND` ist das
Wort, das den Zustand benennt. **Gemessen werden beide**, und die Entscheidung fällt danach.

### M143 — die beiden neuen Kachelstatements

**Die Frage.** Was kosten `COUNT(*)` und `MIN(MessageLastUpdate)` über `MessageStatus = 'RUNNING'`
beziehungsweise `'SUSPENDED'` mit Mandantenkette, je Mandant?

**Erwartet:** in der Größenordnung der alten Überfällig-Statements — **5,102 / 6,171 ms**
(`NEXANS`) und **4,516 / 5,171 ms** (`SUTTONS`), gemessen in M108. Der Zugriffspfad ist `range`
über **`MessageStatusIDX`**.

> **Ein anderer Zugriffspfad als `range` über `MessageStatusIDX` ist ein Befund**, kein Detail. Das
> neue Statement ist **einfacher** als das alte: `=` auf den Rohwert statt `IN` über zwei Werte,
> und ohne die Fristbedingung. Ein `ref` statt `range` wäre deshalb kein Rückschritt, sondern die
> erwartbare Folge eines einzelnen Wertes — auch das ist zu berichten und nicht als „passt schon"
> abzutun.

**Für `RUNNING` wird `anzahl = 0` erwartet, bei jedem Mandanten**, und `aelteste = NULL`. `RUNNING`
kommt in der Testkopie null Mal vor. **Das ist keine Abnahmelücke**, sondern der bekannte Zustand
dieser Kopie.

### M144 — die Erscheinungsbedingung, je Mandant, für alle zehn

**Die Frage.** Liefert das Statement aus §3.5 — hat der Mandant einen Prozess, dessen **geplanter**
Ablauf einen `SUSPEND`-Baustein trägt — `true` oder `false`, für jeden der zehn Mandanten? Und wie
verteilen sich die 538 `SUSPENDED` auf die Mandanten?

**Erwartet:** wenige Mandanten mit `true`. **Das Ergebnis je Mandant ist die Zahl, die vorher
niemand hatte** — es ist vollständig zu protokollieren, auch jedes `false`.

> ⚠️ **Zur Aufteilung der 538 sagt der Auftrag, sie sei „nicht erhoben". Das trifft nicht zu.**
> [`messungen-schritt10.md`](messungen-schritt10.md) M90 hat sie erhoben: *„Alle 538 offenen Zeilen
> gehören `NEXANS` … Kein anderer Mandant hat auch nur eine."* Sie wird hier trotzdem nachgemessen,
> und zwar aus einem Grund: M90 hat gegen den **falschen Anker** gemessen
> (`2026-07-08 17:21:10` statt `2025-12-30 04:09:47`, [`dashboard.md`](dashboard.md) §8). Auf die
> Zuordnung zum Mandanten wirkt der Anker nicht — auf die Zahl der *überfälligen* Zeilen sehr wohl.
> **Erwartet ist deshalb: 538 für `NEXANS`, null für alle anderen, und M90 bestätigt.**

**Trifft das zu, hat die Kachel *Wartend* lokal genau bei einem Mandanten eine Zahl über null.**
Zeigt §3.5 für weitere Mandanten `true`, sehen diese die Kachel mit einer `0` — und genau das ist
ihr Zweck: *„heute wartet nichts"* ist eine Auskunft, *„dieser Mandant wartet nie"* eine andere.

### M145 — die ganze Landingpage

**Die Frage.** Wie teuer ist die zusammengesetzte Landingpage nach dem Umbau, je Mandant und je
Zeitraumpaar?

**Budget 500 ms.** Bezugswerte aus M108:

| Paar | `NEXANS` | `SUTTONS` |
|---|---:|---:|
| 48 h | 62,227 ms | 72,007 ms |
| 30 Tage | 152,814 ms | 109,829 ms |
| 12 Monate | **199,030 ms** | 127,038 ms |

**Erwartet wird eine Verbesserung**, und die Rechnung dazu steht vorher fest:

| | |
|---|---:|
| fällt weg: die Überfälligkeitshälfte von „Zuletzt aufgefallen" | **29 bis 34 ms** (M108) |
| fällt weg: die zwei Überfällig-Kachelstatements | **9,7 bis 11,3 ms** (M108) |
| kommt hinzu: die zwei neuen Kachelstatements | erwartet **9 bis 12 ms** (M143) |
| kommt hinzu: die Erscheinungsbedingung | erwartet **unter 200 ms**, Abbruch darüber (M142) |

> **Die Erwartung ist damit nicht eindeutig, und das gehört vorher gesagt.** Fällt M142 klein aus
> (wenige Millisekunden), erwarten wir die Landingpage um **rund 30 ms billiger**. Fällt M142 nahe
> an sein Abbruchkriterium, wird sie **teurer** — bei `NEXANS` über zwölf Monate rechnerisch
> 199 − 30 + 200 = **rund 369 ms**, immer noch unter Budget, aber ohne Luft. **Beides ist ein
> zulässiges Ergebnis; nur das Verschweigen wäre es nicht.**
