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

### Geändert

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
