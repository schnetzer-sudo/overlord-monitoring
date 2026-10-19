# Changelog

Je Version Datum und die Abschnitte „Neu“, „Geändert“ und „Behoben“. Versionen nach SemVer; Zweige,
Tags und die Definition of Done für ein Release stehen in
[`DEVELOPMENT_GUIDELINES.md`](DEVELOPMENT_GUIDELINES.md) §12. Eine Änderung an
`docs/PROJEKTBESCHREIBUNG.md` oder `docs/README.md` nennt hier Grund und Entscheidung.

## Unveröffentlicht

### Neu

- Auftragsmodell der Assistent-Linie (A1): Tabellen `task`, `task_type` und `task_type_mandant`
  in `overlord_monitor` (Migration `V100`), neu in `docs/PROJEKTBESCHREIBUNG.md` §5
  ([`docs/tasks.md`](docs/tasks.md) §5).
- Auftragstypen sind Daten: Definition per SQL ohne Deployment, Prüfung beim Laden, der erste Typ
  `WERK_ABLADESTELLE` ([`docs/tasks.md`](docs/tasks.md) §3, §4; E‑1018 bis E‑1024).
- Endpunkte `GET /api/tasks/typen`, `POST /api/tasks`, `GET /api/tasks` und
  `GET /api/tasks/{taskId}` ([`docs/tasks.md`](docs/tasks.md) §6).
- Vertrag für die EDI-Plattform in [`docs/task-schnittstelle.md`](docs/task-schnittstelle.md).
- Fachpaket `task` in `docs/PROJEKTBESCHREIBUNG.md` §6 ([`docs/tasks.md`](docs/tasks.md)).
- Assistent im Backend (A2), abgeschaltet bis zur Prüfung durch den Datenschutzbeauftragten:
  Endpunkte unter `/api/assistent`, Anbindung an die Anthropic-API hinter einer anbieterneutralen
  Schnittstelle, Werkzeuge aus den Typdefinitionen, Testsatz
  ([`docs/assistent.md`](docs/assistent.md); E‑1028 bis E‑1044).
- `POST /api/tasks/pruefung`: Prüfung eines Auftrags ohne Anlegen, für die Karte (E‑1042).
- Die Festlegungen der Umsetzung des Assistenten als E‑1045 bis E‑1057, bestätigt am
  16.10.2026 ([`docs/assistent.md`](docs/assistent.md) §2).
- Fachpaket `assistent` in `docs/PROJEKTBESCHREIBUNG.md` §6, weil der Assistent ein eigenes
  Fachpaket ist ([`docs/assistent.md`](docs/assistent.md) §4.1).
- `docs/PROJEKTBESCHREIBUNG.md` §7, neuer Abschnitt „Assistent“: Datenfluss zum Anbieter, Schalter,
  keine Speicherung — weil Kundentext erstmals das Haus verlässt (E‑1029, E‑1035, E‑1038).
- `docs/PROJEKTBESCHREIBUNG.md` §11, neue offene Annahme: Übermittlung an Anthropic in die USA auf
  Grundlage des Data Privacy Framework, Prüfung durch den Datenschutzbeauftragten ausstehend
  (E‑1029, Punkt 1009).
- Assistent im Frontend (A3): Symbol in der Kopfleiste links neben dem Nutzermenü, Chatbox als
  Overlay, Bestätigungskarte mit Prüfung über `POST /api/tasks/pruefung` und Anlegen per Klick;
  erscheint nur, wenn `GET /api/assistent` `verfuegbar: true` meldet
  ([`docs/assistent-frontend.md`](docs/assistent-frontend.md); E‑1065 bis E‑1082).
- `docs/README.md`: Eintrag für `docs/assistent-frontend.md` und Hinweis auf die Sichtprüfungen
  1018 bis 1022 im Register, weil jede neue Datei dort steht (Dokumentationspflicht in
  [`CLAUDE.md`](CLAUDE.md)).

### Geändert

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
