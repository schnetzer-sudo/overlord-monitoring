# Konsolidierung der Projektbeschreibung zu 1.0.0

*Angelegt am 16.10.2026.* Je Abschnitt und je Korrekturkasten der Fassung bis 1.0.0
([`PROJEKTBESCHREIBUNG_bis-1.0.0.md`](PROJEKTBESCHREIBUNG_bis-1.0.0.md), Zeilennummern dort) eine
Zeile: wo die Aussage heute steht ([`../PROJEKTBESCHREIBUNG.md`](../PROJEKTBESCHREIBUNG.md), „neu
§…“) oder warum sie entfallen ist. Damit lässt sich prüfen, dass nichts Geltendes verloren ging.

**Lesart.** *Steht in* = die Aussage gilt und steht dort, oft verdichtet. *Vorgeschichte* = der
Kasten erzählt, wie es zu einem heute geltenden Satz kam; der Satz steht, die Erzählung bleibt im
Archiv und meist in einer Feature-Datei. *Überholt durch* = die Aussage gilt nicht mehr.

## Kopf und Präambel

| Alt (Zeile) | Inhalt | Heute |
|---|---|---|
| 3–4 | Vorrang dieser Datei | neu Kopf, unverändert |
| 7 | Ausnahme vom Vorrang: Indizes, E37 gebaut | neu Kopf „Ausnahme vom Vorrang“, mit dem Stand vom 16.10.2026 (Tabellenliste rot, Punkt 252) |
| 21 | Korrektur 01.08.2026: A1 „Daten bis Ende 2025“ | Vorgeschichte; der Datenstand 08.07.2026 steht neu §8 Umgebungen, A1 neu §11 geklärt |
| 26, 31 | Revisionen 28.07. und 27.07.2026 | Vorgeschichte; die geänderten Inhalte stehen in den jeweiligen Abschnitten |

## §1 Zweck, §2 Nutzer und Rollen

| Alt | Inhalt | Heute |
|---|---|---|
| §1, Leitsatz | Zweck, kein Nachbau, Leitsatz | neu §1, unverändert |
| §2 Tabelle, Zielgruppe | Rollen `MANDANT`, `ADMIN` | neu §2 |
| §2 „ADMIN nacheinander“ | genau ein aktiver Mandant, Mandant in der Session, Kopfzeile | neu §2, verdichtet |
| §2 „Berechtigung als Menge“ | Menge statt Rolle | neu §2 |
| §2 Absatz A2 „unter Druck“ | n:m vorbereitet | überholt durch 9a: n:m wird gepflegt (neu §2, A2 neu §11 geklärt) |

## §3 Quellsystem

| Alt | Inhalt | Heute |
|---|---|---|
| §3 Kopf | nur lesend, nur `SELECT` | neu §3 |
| §3.0 Version, Sortierung, Schlüsselspalten, Rechtetabelle | | neu §3.0 |
| §3.0 Umbenennung `monitor_root` → `monitor_write` | | Vorgeschichte, entfallen |
| §3.0 MariaDB 10.6 Wartungszeitraum | | neu §3.0, A10 neu §11 |
| §3.0 kein TLS | Klartext im Netz, Verweis auf A11 | neu §3.0, mit Entscheidung 5 (tragbar laut Auskunft vom 16.10.2026) |
| §3.1 Hierarchie, View `MessageMandantID` | | neu §3.1 |
| 132 Kasten E42 | eigene Kette als `EXISTS`, View nicht benutzt | neu §3.1, als heutiger Sachverhalt; die Suche nach Verwendern der View ist Vorgeschichte |
| §3.2 `Message` Spalten, kein Anlagedatum | | neu §3.2 |
| 161 Korrektur 01.08.: `MessageTimeout` Sekunden (M8) | | neu §3.2, mit M8 |
| 167 Kasten 03.09.: Wächter auf `RUNNING`, `fristSekunden` `null` bei `WARTEND` (E‑76), `TIMEOUT_EINHEIT` ohne Verbraucher | | neu §3.2 (Wächter, Auskunft, E‑76); `TIMEOUT_EINHEIT` steht in [`message-status.md`](../message-status.md) |
| Indextabelle (acht) | | neu §3.2 |
| 198 Kasten „Bewacht seit 21.08.“ | | neu Kopf, Ausnahme vom Vorrang |
| 210 Kasten E40 `Indexes.java` | | neu §3.2, ein Satz |
| 218 Kasten Korrektur 20.08.: drei statt acht Indizes, falscher Arbeitsauftrag | | Vorgeschichte; die richtige Liste steht neu §3.2 |
| `MessageAction` und 239 Korrektur 07.08.: neun Spalten, Join über `MessageAction.SOSID` (M14, M15) | | neu §3.2 |
| `SOSActionServiceProperties` | | neu §3.2 |
| `MessageProperty`, 255 Zählung M44, 82 % | | neu §3.2 und §8 Mengengerüst |
| 264 Korrektur 12.08.: Schätzung statt Zählung | | Vorgeschichte; die gezählte Zahl steht neu §8 |
| 271 Kasten „rund vierzehn“ | | Vorgeschichte, entfallen |
| Zugriff nur über `MessageID` | | neu §3.2 und §8 Regel 4 |
| Bekannte Namen und 290 Kasten `DestinationFilename` (M161, E‑105) | | neu §3.2 |
| 300 Kasten `Message.Payload.GUID` (M73), Formatangabe | | neu §3.2 und §7 Rohdatenzugriff |
| `MessageBAM`, Zählung M33‑0, kein Zeitstempel | | neu §3.2 und §8 |
| 329 Korrektur 12.08.: Schätzung | | Vorgeschichte |
| `MessageBAMMandant` | | neu §3.2 |
| `Process`/`Project`, 346 Ergänzung 15.09.: Spalten erhoben | | neu §3.2; Folgen für die Prozessansicht stehen in [`process-view.md`](../process-view.md) §48 |
| Mandantentabelle | | neu §3.2 |
| SYSTEM/WOC „gesonderte Behandlung“ | | überholt durch E‑f |
| 384 Kasten E‑f, 399 Zahlen M91 | | neu §3.2 (E‑f, M91); die Abnahmebemerkung zu 10b ist Vorgeschichte |
| `SOS`/`SOSAction` | | neu §3.2 |
| `Service`, 447 Neueintrag 10.09. | | neu §3.2 |
| `User`, `MessageStatisticHistory` | | neu §3.2 |
| §3.3 Eventtabelle | | neu §3.3 |
| 469 „COMMIT-Zuordnung bis zu eine Stunde verzögert“ | | neu §3.3: verzögert ja, wie lange ungedeckt |
| 474 Kasten Takt `MatchInterchange` ungedeckt (M31‑3) | | neu §3.3 (verdichtet, mit Kennzeichnung) und §11 |

## §4 Fachliche Definitionen

| Alt | Inhalt | Heute |
|---|---|---|
| §4.1 freier Text, `CKECKED` | | neu §4.1 |
| §4.1 Statustabelle „seit 01.01.2025“ (27.07.2026) | | neu §4.1 mit den Zahlen des ganzen Bestands aus M191 (13.10.2026) und den Einordnungsnamen aus dem Code; die Zahlen von 2025 stehen in [`message-status.md`](../message-status.md) |
| `COMMIT_REJECTED` als Fehler | | neu §4.1, §4.2 |
| `EERP_FAILED` (E‑233) mit Belegvermerk | | neu §4.1 |
| `RUNNING` null Mal in der Kopie | | neu §4.1 |
| `EERP_PENDING` (E‑234, E‑235) mit Belegvermerk | | neu §4.1, §4.2 |
| 590 Kasten Wächter auf `RUNNING` | | neu §3.2 und §4.2 |
| `CHECKED`/`CKECKED`/`COMMIT_SENT` ungeklärt | | neu §4.1 |
| `SPLITTED`/`MERGED` zwei Einordnungen | | neu §4.1, §4.3; „an der Überfälligkeitsrechnung ändert das nichts“ ist mit E‑71 gegenstandslos |
| Fehlerabfrage alte Form, `LEFT()`-Verbot | | neu §4.1 in der geltenden Form |
| 623 Kasten E‑233: geltende Fehlerabfrage, M191 | | neu §4.1; Messzahlen in [`message-status.md`](../message-status.md) |
| Einordnung an einer Stelle | | neu §4.1 |
| Sicherung gegen neue Statuswerte | | neu §4.1 |
| 655 Kasten: Test erreicht die Produktion nicht (Punkt 218) | | neu §4.1 |
| 662 Kasten E‑236: Schnappschuss (Punkt 219) | | neu §4.1 |
| §4.2 Kopf „drei, nie zusammenfassen“ | | neu §4.2 (Q3) |
| Fehler und 688 Kasten E‑233 | | neu §4.2 Nr. 1 |
| Überfällig und 699 Kasten E‑71 | | neu §4.2 Nr. 2, ein Satz Begründung mit Verweis; Zahlen 538/0 neu §4.2 |
| Kasten `istUeberfaellig` (Stelle im Code, Anwendungsuhr, keine Farbe) | | überholt durch E‑71: der Code ist entfallen |
| 765 Kasten Farbrolle *Überfällig* | | überholt durch E‑71 (Punkt 88 gegenstandslos) |
| Unquittiert und 779 Kasten E‑d | | neu §4.2 Nr. 3 |
| `EERP_FAILED`/`EERP_PENDING` zu Unquittiert (Punkt 220) | | neu §4.2 Nr. 3 |
| §4.3 Verkettung | | neu §4.3 |
| §4.4 Partner, Richtung, Kuratierung | | neu §4.4 |
| 848 Korrektur 20.08.: Standort und Belegart entfallen | | neu §4.4, ein Satz |
| `00001_Undefined` | | neu §4.4 |

## §5 Eigenes Schema

| Alt | Inhalt | Heute |
|---|---|---|
| Tabellenliste (sechs Tabellen) | | neu §5, **aus den Migrationen `V1`–`V13` neu erhoben**: zwölf Tabellen. `saved_view` existiert nicht (keine Migration legt sie an); dazu gekommen sind die Session-, BAM- und Rollup-Tabellen |
| 871 Korrektur 20.08.: Tabelle `partner` entfällt | | Vorgeschichte; die Tabelle steht nicht in den Migrationen |
| 877 Kasten Rollup-Schlüssel E‑a, M89 | | neu §5 |
| Zeichensatz und Sortierung | | neu §5, unverändert |

## §6 Architektur

| Alt | Inhalt | Heute |
|---|---|---|
| Backend-Stack | | neu §6 |
| Zwei DataSources, drei Schutzschichten, kein `@Primary`, ein Transaktionsmanager, DSLContexts, Laufzeitgrenze, Zeitzonen | | neu §6 |
| Keine Fremdschlüssel über Schemagrenzen | | neu §5 |
| Kein JPA, Spring Boot statt Quarkus | | neu §6 |
| Basispaket, Maven-Koordinaten, Hauptklasse | | neu §6 (die Begründung des Namensraums ist entfallen) |
| Paketstruktur | | neu §6, **gegen den Code geprüft** (Verzeichnisse, `PaketstrukturTest`) |
| 1013 Kasten `MandantContext` in `common` | | neu §6 |
| 1021 Kasten `common` wächst mit dem zweiten Verbraucher | | neu §6, als Regel; die Tabelle der Umzüge steht in [`rohdaten-backend.md`](../rohdaten-backend.md) §12 und [`dienste.md`](../dienste.md) §3 |
| 1038 Korrektur `payload`, 1043 Paketname bleibt | | neu §6 |
| Zwei jOOQ-Zielpakete, Codegen eingecheckt, CI ohne DB | | neu §6 |
| Rollup ohne eigenes Wurzelpaket | | neu §6 (Paketbaum, Profil) |
| Frontend | | neu §6; „BFF für das Cookie-Handling“ heißt heute: `/api`-Weiterleitung, siehe [`frontend-grundlagen.md`](../frontend-grundlagen.md) |
| Erweiterbarkeit | | neu §6 |
| Betrieb: JAR, Node, Reverse Proxy, nicht GlassFish | | neu §6, ausführlich [`../betrieb.md`](../betrieb.md); „systemd-Dienst oder Container“ ist dort offene Frage 255 |
| Versionsstand 24.07.2026 | | steht in [`DEVELOPMENT_GUIDELINES.md`](../../DEVELOPMENT_GUIDELINES.md) §9 |

## §7 Sicherheit

| Alt | Inhalt | Heute |
|---|---|---|
| Authentifizierung, Sperre, Dummy-Hash, Ausnahme, Systemuhr | | neu §7 |
| Keine Migration der Alt-Benutzer, Bootstrap, Änderungszwang | | neu §7 |
| Mandantentrennung M1, drei Ausnahmen, `404` | | neu §7 |
| 1167 Korrektur 20.08.: „zwei“ statt drei Ausnahmen | | Vorgeschichte |
| `MandantContext`, ArchUnit, Filter im Statement, Isolationstest | | neu §7; die drei Ausnahmen von M2 sind neu dazugeschrieben (aus [`mandantentrennung.md`](../mandantentrennung.md) §4) |
| Rohdatenzugriff (geltende Fassung) | | neu §7 |
| 1196 Korrektur 17.09.: Darstellung (E‑193 bis E‑205) | | neu §7 |
| 1243 Kasten totes Flag, 1252 Korrektur 20.08.: Flag fällt | | neu §7: mit `V7` entfallen |
| 1260 Korrektur 19.08., zitierter Altabschnitt „Download-Proxy“, Abgrenzungslisten | | Vorgeschichte |
| 1307 Nachtrag 19.08.: Einstieg nicht über `Message.Payload.GUID` | | neu §3.2 und §7 (Verweise je Dienst) |

## §8 Leistungsregeln

| Alt | Inhalt | Heute |
|---|---|---|
| Mengengerüst mit Herkunft | | neu §8 |
| 7.300 je Tag | | neu §8 |
| 1381 Korrektur 07.08.: 5.000 je Tag | | Vorgeschichte |
| 1401 Korrektur 12.08.: Überschrift „gemessen“, Abweichungstabelle | | Vorgeschichte; neu §8 nennt `TABLE_ROWS` als Schätzung |
| 1450 Kasten Aufbewahrung ungedeckt | | neu §8 und §11 |
| Regel 1 und 1491/1501 „Spannung zur Aufbewahrung“ | | neu §8 Regel 1, verdichtet |
| 1516 Kasten Jahresmaximum (M168, M170, M171, M50, Punkt 159) | | neu §8 Regel 1 |
| Regel 2 und Begründung vom 27.07. | | neu §8 Regel 2 |
| 1568 Kasten E‑c *Überfällig* (M90) | | überholt durch E‑71; als entfallene Ausnahme erwähnt |
| 1604 Kasten *Läuft*, *Wartend* (E‑72, E‑73, M143) | | neu §8 Regel 2, Tabelle Nr. 1 und 2 |
| 1646 Kasten Live-Rest (M185, M186) | | neu §8 Regel 2, Tabelle Nr. 3 |
| 1689 Kasten Fehler live (E‑208, M188, M189) | | neu §8 Regel 2, Tabelle Nr. 4 |
| — | Block 6 „Zuletzt aufgefallen“ | **neu** in §8 Regel 2 als ungeklärt (Punkt 253); in der alten Fassung nicht erwähnt |
| Regel 3 | | neu §8; „Beleg- und Property-Suche blättern nicht“ ist neu dazugeschrieben (aus [`bam-suche.md`](../bam-suche.md) §5) |
| Regel 4 und 1735 Kasten E‑102 | | neu §8 Regel 4 |
| Regel 5 | | neu §8; die gebauten Abweichungen (keine Mindestlänge) sind neu dazugeschrieben, ihr Status als Ausnahme offen |
| Regel 6 und 1790 Kasten „verortet“ (M88, M92, Punkt 50) | | neu §8 Regel 6 |
| Regel 7 und 1825 Zusatz E‑239 | | neu §8 Regel 7 |
| Umgebungen, 1841 Bestätigung M0 | | neu §8 Umgebungen, mit der Produktion seit September 2026 |
| Replica, Codegen gegen Testkopie, Spaltenabgleich | | neu §8 Umgebungen; der Abgleich betrifft `Message` (`DatenzugriffDbIT`) |
| Referenzzeitpunkt, `Clock`, Ausnahme Sicherheit | | neu §8 Umgebungen und §6 |

## §9 MVP-Umfang

| Alt | Inhalt | Heute |
|---|---|---|
| „Enthalten“ | | neu §9 „Funktionsumfang 1.0“, je Funktion mit Feature-Datei |
| „Nicht enthalten“: Service-Überwachung, 1890 Kasten E‑116 | | neu §9 „Nicht enthalten“: über die Anzeige hinaus |
| „Nicht enthalten“: aufbereitete Anzeige, 1908 Kasten E‑193 bis E‑205 | | neu §9 und §7 |
| Benachrichtigungen, Chatbot | | neu §9 |
| 1926 Korrektur 19.08.: „nur Download“ | | Vorgeschichte |
| 1945 Korrektur 27.08.: zwei Kategorien, 1957 Kasten „eine“, Befund der Messrunde, „Was das für die Umsetzung heißt“ | | Vorgeschichte; der Stand (eine gebaut) steht neu §4.2 |

## §10 Ausbaustufen, §11 Annahmen, §12 Glossar

| Alt | Inhalt | Heute |
|---|---|---|
| §10 Chatbot, Alarmierung, Service-Überwachung (2000 Kasten E‑116), SOS-Baukasten | | [`../ROADMAP.md`](../ROADMAP.md) §3, je mit Vorbehalt; neu §10 drei Sätze |
| A1, A2, A4–A8 geklärt | | neu §11 „Geklärt“, je ein Satz |
| A3, A10 | | neu §11 „Offen“ |
| A9 | | neu §11 geklärt (M33/M34); die Rückfalloption eigener BAM-Index ist damit entfallen |
| A11 offen | | neu §11 geklärt nach Entscheidung 5 |
| A12 widerlegt | | neu §11 geklärt, Abhilfe neu §8 Regel 7 |
| — | ungedeckte Angaben (Takt, Aufbewahrung, Wächter) | neu §11 „Offen“ zusammengeführt |
| §12 Glossar | | neu §12, um Rollup, Live-Rest, Testkopie ergänzt |
