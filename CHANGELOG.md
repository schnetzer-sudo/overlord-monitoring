# Changelog

Je Version Datum und die Abschnitte „Neu“, „Geändert“ und „Behoben“. Versionen nach SemVer; Zweige,
Tags und die Definition of Done für ein Release stehen in
[`DEVELOPMENT_GUIDELINES.md`](DEVELOPMENT_GUIDELINES.md) §12. Eine Änderung an
`docs/PROJEKTBESCHREIBUNG.md` oder `docs/README.md` nennt hier Grund und Entscheidung.

## Unveröffentlicht

### Neu

- `scripts/werk-abladestelle-schluessel-leerzeichen.sql` mit Rücknahme
  `scripts/werk-abladestelle-schluessel-leerzeichen-ruecknahme.sql`: setzt das Muster der drei
  Schlüsselteile von `WERK_ABLADESTELLE` auf `^(?:[^/,\p{Z}]| ){1,35}$`, idempotent, mit drei
  Wachen. Lukas führt es in der Produktion aus, **erst nach dem Deployment dieser Version**
  ([`docs/betrieb.md`](docs/betrieb.md) §7a E, E‑1149). Auf der Testkopie gelaufen am 23.10.2026
  (E‑1145).
- `scripts/werk-abladestelle-beschreibung-zeilenformat.sql` mit Rücknahme
  `scripts/werk-abladestelle-beschreibung-zeilenformat-ruecknahme.sql`: Die Beschreibung von
  `WERK_ABLADESTELLE` nennt das Zeilenformat (Schlüssel, Komma, SNDPRN, Komma, SNDPRN_4908) und sagt,
  dass ein Feld nach einem Komma ausdrücklich leer ist; bei einer Anpassung fragt der Assistent dann
  nicht nach, sondern sagt vor dem Anlegen, dass der Wert entfernt wird (E‑1155). Grund: Im Test in
  der Produktion las das Modell eine solche Zeile falsch. Idempotent, mit drei Wachen, **an keine
  Version gebunden**, vor oder nach dem Deployment ([`docs/betrieb.md`](docs/betrieb.md) §7a F,
  E‑1156). Lukas führt es in der Produktion aus. Auf der Testkopie gelaufen am 26.10.2026.
- `scripts/werk-abladestelle-abladestelle-optional.sql` mit Rücknahme
  `scripts/werk-abladestelle-abladestelle-optional-ruecknahme.sql`: **Bei `WERK_ABLADESTELLE` ist
  die Abladestelle optional**; Pflicht bleibt nur das Werk. Das Skript entfernt `pflicht` an der
  Abladestelle, ersetzt in der Beschreibung den Satz zum Schlüssel und ergänzt im Absatz zum
  Zeilenformat das Beispiel `0101159903/40NP/,120764,` (E‑1176); die Rücknahme stellt beides
  zeichengleich wieder her. Grund: Die Plattform verarbeitet Schlüssel ohne Abladestelle bereits, mit
  Schrägstrich am Ende (E‑1172). Idempotent, mit drei Wachen. Lukas führt es in der Produktion aus,
  **erst nach dem Deployment dieser Version und nach dem Skript zum Zeilenformat**
  ([`docs/betrieb.md`](docs/betrieb.md) §7a G, E‑1171). Auf der Testkopie gelaufen am 26.10.2026 und
  danach zurückgenommen; die Testkopie trägt den Stand von `main`.

### Geändert

- **Schlüsselteile frei wählbar, Schlüssel von hinten ausgerichtet** (Entscheidungen des
  Auftraggebers vom 26.10.2026, E‑1164 bis E‑1177, [`docs/tasks.md`](docs/tasks.md) §2). Grund: Eine
  Definition mit optionaler Abladestelle war ungültig, weil nur der erste Teil des Schlüssels
  optional sein durfte; der Assistent wäre für den Mandanten verschwunden.
  - **Jeder Teil eines Schlüssels kann optional sein**, an jeder Position; mindestens einer bleibt
    Pflicht, alle übrigen Prüfungen am Schlüssel bleiben (E‑1164 bis E‑1166). Erledigt Punkt 1007.
  - **Der Schlüsseltext ist von hinten ausgerichtet**, für jeden Typ: Teile vor dem ersten Teil mit
    Wert entfallen samt Trenner, danach behält jeder Teil seinen Platz, ein fehlender als leerer
    Text (`a//c`, `a/b/`). Bei `WERK_ABLADESTELLE` heißt ein Eintrag ohne Abladestelle damit
    `0101159903/40NP/` bzw. `72332/`, wie in der Tabelle der Plattform, und `010/18VZE` (Werk,
    Abladestelle) und `010/18VZE/` (Lieferantennummer, Werk) sind verschiedene Schlüssel (E‑1172 bis
    E‑1174; ersetzt „fehlende Teile entfallen an jeder Position“, E‑1167, aus dem ersten Durchgang
    desselben Tages).
  - **Werkzeugbeschreibung:** statt „fehlt ein Teil, ist es der optionale erste“ nennt sie die
    optionalen Teile mit ihrem Label und sagt, der Schlüssel sei von hinten ausgerichtet, der letzte
    Teil gehöre zum letzten Feld, ein leerer Teil heiße „Feld leer“; das Modell zerlegt ohne
    Rückfrage (E‑1175, ändert E‑1060). Ohne optionale Teile bleibt der Text Wort für Wort.
  - [`docs/task-schnittstelle.md`](docs/task-schnittstelle.md) §7 und §8: die Regel von hinten und
    die vier Schlüsselformen `L/W/A`, `W/A`, `L/W/`, `W/`.
  - **Testsatz:** S24 erwartet den Vorschlag ohne Abladestelle, neu S26 und S27, 27 Fälle (E‑1177).
    Lauf M1010 über die acht betroffenen Fälle, drei Durchgänge: je 8 von 8 bestanden, Stil 8 von 8
    ([`docs/assistent.md`](docs/assistent.md) §8.7). Punkt 1045 erledigt. Kein voller DB-Lauf (F51).

- **Der Assistent antwortet ohne Innenleben** (Entscheidungen des Auftraggebers vom 26.10.2026,
  E‑1150 bis E‑1157, [`docs/assistent.md`](docs/assistent.md) §2). Grund: Im Test in der Produktion
  am 23.10.2026 sah der Nutzer die Überlegungen des Modells, Sätze über „das System“ und den
  „gültigen Vorschlag“ und nach einem gültigen Vorschlag alle Einträge noch einmal als Text.
  - **Anweisung:** drei Regeln ersetzt, am Ende der Abschnitt „So antwortest du“ — nur Ergebnis oder
    Frage, den Kunden direkt ansprechen, weder Regeln noch Werkzeuge, Backend, System oder Prüfung
    erwähnen, nach einem gültigen Vorschlag die Einträge nicht wiederholen (E‑1150, E‑1151).
  - **Werkzeugergebnis:** Ein gültiger Vorschlag wird dem Modell nur noch als `{"gueltig": true}`
    gemeldet, ohne Satz zum Nachsprechen (E‑1152).
  - **`denken: adaptive`** statt `between_tools`: Das Modell denkt auch vor einer reinen
    Textantwort verborgen (E‑1153, ändert E‑1046). Gleiche Vorgabe in `application.yml` und im Code.
  - **`max_tokens` 8.192** statt 2.048, weil das Denken mitzählt (E‑1154, ändert E‑1064).
  - **Testsatz:** Gruppe 16 „Zeilenformat mit Komma“ (S22 bis S25, verfremdete Fälle aus der
    Produktion) und eine Stilprüfung jeder Antwort, getrennt von der strukturellen Bewertung
    (E‑1157). Lauf M1008: strukturell zweimal 25 von 25, Stil 25 und 24 von 25 (S12 zu lang,
    Punkt 1042).
  - **Mandantenregel neu formuliert:** „bereitest du nichts vor und sagst ihm in einem Satz“ statt
    „reichst du keinen Vorschlag ein“, weil das Modell den Wortlaut nachsprach; die Stilprüfung kennt
    dazu „muss ich“, „ich analysiere“ und „einreichen“ (E‑1158, ändert den Wortlaut von E‑1059).
    Nachlauf M1009: S12 sechsmal mit Stil ok (167 bis 257 Zeichen), voller Durchgang 25 von 25,
    Stil 25 von 25.
  - Festlegungen der Umsetzung bestätigt als E‑1159 bis E‑1162; ein voller DB-Lauf entfällt für
    diesen Fix (E‑1163).

- **Leerzeichen im Schlüssel des Auftrags „Anpassen der Werk-/Abladestellentabelle“**
  (Entscheidung des Auftraggebers vom 21.10.2026, Rückfragen und Festlegungen der Umsetzung am
  23.10.2026 bestätigt; E‑1136 bis E‑1149, [`docs/tasks.md`](docs/tasks.md) §2). Grund: Die Plattform verarbeitet Schlüssel mit Leerzeichen
  bereits, der Monitor lehnte sie ab. Erlaubt ist U+0020 im Innern eines Schlüsselteils, auch mehrere
  hintereinander, zeichengenau. **Das Backend kürzt jeden Schlüsselteil vor der Prüfung um
  Leerzeichen am Rand**, an genau einer Stelle (`AuftragsPruefung`), für Neuanlage, Anpassung und
  Löschung gleich; gespeichert, in den Parametertext gebaut und auf der Karte gezeigt wird die
  gekürzte Fassung. Tabulator, Zeilenumbruch, U+00A0 und jedes andere Leerzeichen bleiben
  unzulässig. Ändert E‑1012 („kein Leerraum“). Das Kürzen gilt für die Schlüsselteile jedes Typs, eine
  Option in der Definition gibt es dafür nicht (E‑1144).
- Die Werkzeugbeschreibung des Assistenten sagt für jeden Typ mit Schlüssel, dass Schlüsselteile
  zeichengenau mit jedem Leerzeichen übergeben werden (E‑1148). Der Testsatz hat drei Fälle mehr, S19
  bis S21 als Gruppe 15 ([`docs/assistent.md`](docs/assistent.md) §8).
- Karte und „Ihre Aufträge“ zeigen Schlüssel mit erhaltenen Leerzeichen (`white-space: pre-wrap`,
  in der zugeklappten Zeile `pre`, E‑1146), ohne Ersatzzeichen.
- [`docs/task-schnittstelle.md`](docs/task-schnittstelle.md) §8: Ein Schlüsselteil kann Leerzeichen
  enthalten, nie am Rand. Statements und Ablauf der Plattform sind unverändert.

### Behoben

- Ein geschütztes Leerzeichen (U+00A0) oder ein anderes Unicode-Leerzeichen in einem Schlüsselteil
  von `WERK_ABLADESTELLE` wurde angenommen, obwohl E‑1012 „kein Leerraum“ verlangte: Das alte Muster
  `^[^ /,]{1,35}$` sperrte nur U+0020. Jetzt `FORMAT` (E‑1142).

## 1.1.0 (21.10.2026)

Die Linie `assistent` — **Aufträge über den Assistenten**, A1 bis A4 — ist nach `main`
zusammengeführt. **Der Assistent ist nur für freigeschaltete Mandanten sichtbar, mit diesem Release
zunächst nur im Mandanten `SYSTEM`, den ausschließlich `ADMIN` sieht.** Nutzer aller anderen
Mandanten sehen weder den Assistenten noch „Ihre Aufträge“. Weitere Mandanten schaltet ein
SQL-Insert frei, kein Deployment ([`docs/tasks.md`](docs/tasks.md) §3.9; E‑1125 bis E‑1131).

### Neu

- **Zusammenführung (21.10.2026):** eine Stelle entscheidet je Mandant über den Assistenten
  (`AssistentFreigabe`): Schalter an, Schlüssel gesetzt und mindestens ein für den Mandanten
  freigeschalteter Typ (E‑1126, E‑1130). Nicht verfügbar antworten Gespräche, Prüfen und Anlegen
  `404` wie bei einem nicht zulässigen Mandanten, und kein Aufruf geht an den Anbieter
  ([`docs/tasks.md`](docs/tasks.md) §3.9, F30 bis F33).
- Migration `V103`: nimmt die Freischaltungen von `WERK_ABLADESTELLE` für `NEXANS` und `IBIS` aus
  `V100` zurück (E‑1125).
- `scripts/assistent-freischaltung-system.sql` schaltet `WERK_ABLADESTELLE` ausschließlich für
  `SYSTEM` frei, idempotent, mit Rücknahme
  `scripts/assistent-freischaltung-system-ruecknahme.sql`; Lukas führt es in der Produktion aus (F34).
- `docs/betrieb.md` §5a: Zeitgrenze, Antwort am Stück ohne Streaming, Reverse Proxy und ausgehendes
  HTTPS zu `api.anthropic.com`.
- **Nachtrag (21.10.2026):** `docs/betrieb.md` §7a hält die Deployment-Routine der Produktion fest,
  wie Lukas sie mitgeteilt hat (E‑1133): systemd mit drei Units, Bau auf dem Server, Sicherung vor
  dem Start, Rollback. Der Schlüssel des Assistenten liegt dort in einer eigenen `EnvironmentFile`
  nur für die Unit `overlord-monitor`, zusammen mit dem Schalter (E‑1134). Erledigt sind die Punkte
  255, 260 und 261; neu offen sind 1038 bis 1041 aus dem Abgleich der Routine mit dem Repository.
- Auftragsmodell der Assistent-Linie (A1): Tabellen `task`, `task_type` und `task_type_mandant`
  in `overlord_monitor` (Migration `V100`), neu in `docs/PROJEKTBESCHREIBUNG.md` §5
  ([`docs/tasks.md`](docs/tasks.md) §5).
- Auftragstypen sind Daten: Definition per SQL ohne Deployment, Prüfung beim Laden, der erste Typ
  `WERK_ABLADESTELLE` ([`docs/tasks.md`](docs/tasks.md) §3, §4; E‑1018 bis E‑1024).
- Endpunkte `GET /api/tasks/typen`, `POST /api/tasks`, `GET /api/tasks` und
  `GET /api/tasks/{taskId}` ([`docs/tasks.md`](docs/tasks.md) §6).
- Vertrag für die EDI-Plattform in [`docs/task-schnittstelle.md`](docs/task-schnittstelle.md).
- Fachpaket `task` in `docs/PROJEKTBESCHREIBUNG.md` §6 ([`docs/tasks.md`](docs/tasks.md)).
- Assistent im Backend (A2), abgeschaltet bis zur Prüfung durch den Datenschutzbeauftragten
  (seit 21.10.2026 erledigt, Punkt 1009): Endpunkte unter `/api/assistent`, Anbindung an die Anthropic-API hinter einer anbieterneutralen
  Schnittstelle, Werkzeuge aus den Typdefinitionen, Testsatz
  ([`docs/assistent.md`](docs/assistent.md); E‑1028 bis E‑1044).
- `POST /api/tasks/pruefung`: Prüfung eines Auftrags ohne Anlegen, für die Karte (E‑1042).
- Die Festlegungen der Umsetzung des Assistenten als E‑1045 bis E‑1057, bestätigt am
  16.10.2026 ([`docs/assistent.md`](docs/assistent.md) §2).
- Fachpaket `assistent` in `docs/PROJEKTBESCHREIBUNG.md` §6, weil der Assistent ein eigenes
  Fachpaket ist ([`docs/assistent.md`](docs/assistent.md) §4.1).
- `docs/PROJEKTBESCHREIBUNG.md` §7, neuer Abschnitt „Assistent“: Datenfluss zum Anbieter, Schalter,
  keine Speicherung — weil Kundentext erstmals das Haus verlässt (E‑1029, E‑1035, E‑1038).
- `docs/PROJEKTBESCHREIBUNG.md` §11, neue Annahme: Übermittlung an Anthropic in die USA auf
  Grundlage des Data Privacy Framework (E‑1029, Punkt 1009); bis 21.10.2026 offen, seitdem unter
  „Geklärt“.
- Assistent im Frontend (A3): Symbol in der Kopfleiste links neben dem Nutzermenü, Chatbox als
  Overlay, Bestätigungskarte mit Prüfung über `POST /api/tasks/pruefung` und Anlegen per Klick;
  erscheint nur, wenn `GET /api/assistent` `verfuegbar: true` meldet
  ([`docs/assistent-frontend.md`](docs/assistent-frontend.md); E‑1065 bis E‑1082).
- `docs/README.md`: Eintrag für `docs/assistent-frontend.md` und Hinweis auf die Sichtprüfungen
  1018 bis 1022 im Register, weil jede neue Datei dort steht (Dokumentationspflicht in
  [`CLAUDE.md`](CLAUDE.md)).
- „Ihre Aufträge“ in der Übersicht (A4): ein Block in der Reihe von „Zuletzt aufgefallen“, nur wenn
  der Mandant Aufträge hat, mit dem Stand jedes Auftrags, dem Ergebnis je Eintrag und dem Löschen
  offener Aufträge ([`docs/assistent-frontend.md`](docs/assistent-frontend.md) §12; E‑1098 bis
  E‑1124).
- `DELETE /api/tasks/{taskId}`: löscht einen Auftrag, solange die Plattform ihn nicht abgeholt hat,
  mit einem Ereignis `AUFTRAG_GELOESCHT` im `audit_log`; `404` für fremde und unbekannte, `409` für
  nicht mehr offene Aufträge ([`docs/tasks.md`](docs/tasks.md) §6.6; E‑1110, E‑1113 bis E‑1115).
- Farbrolle `--teilweise` für „Teilweise erledigt“, mit den Werten von `--ueberfaellig`, weil jene
  der Kategorie *Überfällig* vorbehalten bleibt ([`docs/visuelles-konzept.md`](docs/visuelles-konzept.md)
  §3; F16).
- `scripts/auftraege-simulation.sql`: bringt eigene Testaufträge auf der Testkopie mit den
  Statements des Plattformvertrags in jeden Stand, für die Sichtprüfungen 1028 bis 1032.
- Die Festlegungen der Chatbox als E‑1083 bis E‑1097, bestätigt am 19.10.2026
  ([`docs/assistent-frontend.md`](docs/assistent-frontend.md) §2).

### Geändert

- **Sicherung vor jedem Schemaeingriff: das ganze Schema `overlord_monitor`** statt der beiden
  Tabellen `process_catalog` und `bam_sollaenge`, weil auch `app_user`, `app_user_mandant` und
  `bam_spalte` von Hand gepflegt sind (E‑1135, `docs/betrieb.md` §8). Die Liste in
  `docs/bam-sollaengen.md` §7.2 führt jetzt sieben Tabellen.
- Doku: `OVERLORD_BACKEND_URL` wirkt beim Bau des Frontends, nicht beim Start; `next start` liest
  die Weiterleitung aus `.next/routes-manifest.json` (`docs/betrieb.md` §3, Punkt 1041).
- `docs/PROJEKTBESCHREIBUNG.md` §6, „Betrieb“, und `docs/README.md`, Eintrag `betrieb.md`: verweisen
  auf die Deployment-Routine, weil sie seit dem Nachtrag in `docs/betrieb.md` §7a steht (E‑1133).
- `WERK_ABLADESTELLE`: höchstens 20 statt 50 Einträge je Auftrag, und die Beschreibung lässt
  den Assistenten nachfragen, wenn der Kunde bei einer Anpassung nur einen der beiden Werte
  nennt; Migration `V101` (E‑1058, [`docs/tasks.md`](docs/tasks.md) §4).
- `docs/PROJEKTBESCHREIBUNG.md` §3.0: `monitor_write` wird zusätzlich von der EDI-Plattform
  verwendet; sie hat damit alle Rechte auf `overlord_monitor` (E‑1004,
  [`docs/tasks.md`](docs/tasks.md) §2).
- `docs/PROJEKTBESCHREIBUNG.md` §8, Regeln 1 und 2: Die Regeln 1 bis 6 gelten für Lesezugriffe auf
  `GlassfishDB`, nicht für Tabellen in `overlord_monitor`, die nicht daraus abgeleitet sind
  (E‑1016, [`docs/tasks.md`](docs/tasks.md) §2); derselbe Satz bei L1 und L3 in
  `DEVELOPMENT_GUIDELINES.md` §4.4 und in `CLAUDE.md` (E‑1027).
- Teilschritte der Assistent-Linie heißen `assistent-<teil>` statt `assistent/<teil>`, weil Git
  neben dem Zweig `assistent` keinen Zweig `assistent/<teil>` anlegen kann; die CI baut auch
  Pushes auf `assistent-*` (`DEVELOPMENT_GUIDELINES.md` §12).
- `docs/PROJEKTBESCHREIBUNG.md` §6: Typdefinition, Typkatalog und Prüfung der Aufträge liegen in
  `common/auftrag` statt in `task`, weil der Assistent sie als zweiter Verbraucher braucht
  (E‑1037); die Liste der ArchUnit-Regeln nennt die Regeln des Assistenten.
- `scripts/dev-start.ps1` schaltet den Assistenten lokal mit `-Assistent` ein
  (`OVERLORD_ASSISTENT_AKTIV=true` im Backend-Fenster); ohne den Schalter setzt es ausdrücklich
  `false`. Es meldet beim Start den ausgecheckten Zweig und, ob der Schlüssel gesetzt ist
  ([`docs/START-LOKAL.md`](docs/START-LOKAL.md) §6).
- Die Antwort von `GET /api/tasks`, `GET /api/tasks/{taskId}` und `POST /api/tasks` trägt `felder`
  mit `name` und `label` aus der Kopie der Definition, weil die Übersicht die Werte mit ihrem
  `label` zeigt (E‑1120, [`docs/tasks.md`](docs/tasks.md) §6.5). Abwärtsverträglich.
- Plattformvertrag: neuer §3a — der Monitor löscht offene Aufträge auf Wunsch des Kunden, für die
  Plattform ändert sich nichts; Abholtakt fünf Minuten als Auskunft (E‑1117, E‑1118,
  [`docs/task-schnittstelle.md`](docs/task-schnittstelle.md)).
- Doku: Abgeholt und ausgeführt wird von der Plattform, nicht von Lobster (E‑1119).
- „Zuletzt aufgefallen“ bricht Prozessnamen um, statt sie abzuschneiden, weil die Spalte neben
  „Ihre Aufträge“ schmaler ist ([`docs/dashboard-frontend.md`](docs/dashboard-frontend.md) §5.6).
- Nach dem Anlegen in der Chatbox lädt die Liste der Aufträge neu (E‑1109).
- `docs/README.md`: Einträge zu `assistent-frontend.md`, `tasks.md` und `task-schnittstelle.md`
  nennen A4, und die Sichtprüfungen 1028 bis 1032 stehen beim Hinweis auf das Register — weil die
  Dateien seit A4 mehr enthalten, als ihr Eintrag sagte.
- `docs/PROJEKTBESCHREIBUNG.md` §5: `audit_log` nennt Anlegen und Löschen von Aufträgen, `task` das
  Löschen offener Aufträge durch den Kunden — weil beides seit A1 bzw. A4 geschieht (E‑1114).
- **Zusammenführung (21.10.2026):** Nicht verfügbar ist `404 nicht-gefunden` statt
  `503 assistent-nicht-verfuegbar`; der Problemtyp entfällt (F33). Anlegen und Prüfen fragen die
  Freigabe zuerst (F32).
- Die `/api`-Weiterleitung in Next.js wartet 300 s statt 30 s (`experimental.proxyTimeout`), weil
  eine Nachricht an den Assistenten im ungünstigsten Fall 244 s dauert (F37).
- Der Stand des Assistenten gilt im Frontend je Mandant; nach einem Mandantenwechsel wird er neu
  gelesen, eine offene Chatbox schließt sich (F36).
- Die Logzeile beim Start nennt, warum der Assistent für alle Mandanten aus ist (kein Schlüssel,
  Schalter aus), nie den Schlüssel.
- Die DB-Tests legen ihre Freischaltung selbst an und räumen sie ab (F35).
- `DEVELOPMENT_GUIDELINES.md` §12, `CLAUDE.md`: Das Modell zweier Linien ist durch eine Linie
  ersetzt; Migrationen und projektweite Nummern schließen an die höchste vergebene an, der Bereich
  unter 1000 wird nicht weiter befüllt; die Ausnahmen bei Codegen und Indexprüfung entfallen
  (E‑1127). Grund: die Entscheidung des Auftraggebers vom 21.10.2026, die Linie zusammenzuführen.
- `docs/PROJEKTBESCHREIBUNG.md`: Kopfkasten auf 1.1.0; §5 Migrationsbereich `V1` bis `V13` und
  `V100` bis `V103`, `task_type_mandant` als Freischaltung des Assistenten; §6 Codegen ohne
  Ausnahme, Paketbaum ohne „Linie assistent“, die Freigabe in `common/auftrag`; §7 der Assistent
  verfügbar nur für freigeschaltete Mandanten, zunächst `SYSTEM`, und die Freischaltung als keine
  Ausnahme von M1; §9 „Funktionsumfang 1.1“ mit den Aufträgen, „Nicht enthalten“ ohne „Assistent und
  Chatbot“, dafür Fragen an den Chatbot und der Assistent für Kundenmandanten; §10 neu gefasst;
  §11 die Übermittlung an Anthropic unter „Geklärt“. Grund: E‑1125 bis E‑1131.
- `docs/ROADMAP.md` §1 und §2: Linien zusammengeführt, Stand A1 bis A4 und 1.1.0.
- `docs/README.md`: Einträge ohne „Linie `assistent`“ als laufende Linie, Hinweis auf die
  Freischaltung — weil es seit 1.1.0 eine Linie gibt.
- Punkt 1009 (Prüfung durch den Datenschutzbeauftragten) ist erledigt, Auskunft des Auftraggebers
  vom 21.10.2026 (E‑1131). Neu im Register: 1033 bis 1037 (Reverse Proxy, Freischaltung bei offener
  Chatbox, Auslöser der CI, Kopplung der Zeitgrenze, Deadlock der Sitzungsablage).

### Behoben

## 1.0.0 (16.10.2026)

Erste festgeschriebene Version von Overlord Monitoring, seit September 2026 produktiv. Sie umfasst
Anmeldung mit Mandantentrennung, Übersicht, Nachrichtenliste und -detail mit Verkettung, Beleg- und
Property-Suche, Rohdaten und Protokolle, Prozessansicht sowie die Administration von Benutzern und
Prozess-Katalog; der vollständige Umfang steht in
[`docs/PROJEKTBESCHREIBUNG.md`](docs/PROJEKTBESCHREIBUNG.md) §9. Mit diesem Release sind die
Projektbeschreibung und das Verzeichnis `docs/README.md` auf den Produktstand neu gefasst (die
Fassungen davor liegen in `docs/archiv/`), die offenen Punkte stehen im Register
[`docs/offene-punkte.md`](docs/offene-punkte.md), und die Planung führt
[`docs/ROADMAP.md`](docs/ROADMAP.md) statt des abgeschlossenen Implementierungsplans.
