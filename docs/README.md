# docs/

Hier steht, **was** gebaut wird und **warum**. Wie gebaut wird, steht in
[`../DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md).

## Wie dieses Verzeichnis funktioniert

**Jedes Feature bekommt eine eigene Datei.** Keine Sammeldatei, keine „sonstiges.md".

| Anlass | Was zu tun ist |
|---|---|
| Neues Feature | **Neue Datei** anlegen und unten im Verzeichnis eintragen |
| Geändertes Feature | Die **vorhandene Datei aktualisieren** — nicht eine zweite anlegen |
| Entferntes Feature | Datei löschen, Eintrag entfernen |

**Ein Schritt gilt erst als fertig, wenn die Dokumentation steht.** Code ohne zugehörige
Dokumentation ist unfertig, auch wenn er läuft. Die Datei entsteht **im selben Commit** wie das
Feature, nicht später.

## Was in eine Feature-Datei gehört

Kurz, konkret, aus der Sicht von jemandem, der das Feature später ändern muss:

1. **Zweck** — welche fachliche Frage beantwortet das Feature? Ein bis zwei Sätze.
2. **Endpunkte** — Pfad, Parameter, Antwortform, Fehlerfälle.
3. **Datenquelle** — welche Tabellen, welche Joins, welcher Index. Bei jeder Abfrage: das
   `EXPLAIN`-Ergebnis und die gemessene Laufzeit gegen die Testkopie (Regel L7).
4. **Entscheidungen** — was wurde bewusst *nicht* gemacht und warum. Das ist der Teil, der
   sich sonst nicht rekonstruieren lässt.
5. **Regelbezug** — welche der unverhandelbaren Regeln aus Abschnitt 4 der Richtlinien betrifft
   das Feature, und wie ist sie umgesetzt.
6. **Offene Punkte** — was bekannt fehlt. Lieber notiert als vergessen.

**Nicht** hineingehören: Zugangsdaten, Hostnamen, Produktionsdaten, echte Belegnummern,
`MessagePropertyValue`-Inhalte. Beispiele werden anonymisiert.

## Verzeichnis

### Grundlagen — immer gültig

| Datei | Inhalt |
|---|---|
| [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) | **Die verbindliche Wahrheit.** Zweck, Rollen, Quellsystem, fachliche Definitionen, Architektur, Sicherheit, Leistungsregeln, MVP-Umfang, Annahmen |
| [`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) | Die zehn Schritte bis zum MVP, mit Abgrenzung und Abnahmekriterium je Schritt |
| [`datenmodell.md`](datenmodell.md) | **Nachschlagewerk für jede Abfrage.** Kerntabellen, Mandantenkette, Indizes, dokumentierte Fallstricke |

### Features

Noch keine — Schritt 1 legt nur das Fundament. Die folgenden Dateien entstehen laut Plan:

| Datei | Entsteht in | Inhalt |
|---|---|---|
| `datenzugriff.md` | Schritt 2 | DataSources, jOOQ-Codegenerierung, Flyway, `TimeProvider`, Fehlerformat |
| `authentifizierung.md` | Schritt 3 | Anmeldung, Session, Sperre, Migration der Altnutzer |
| `mandantentrennung.md` | Schritt 3 | Wie die Trennung durchgesetzt und geprüft wird |
| `nachrichtenliste.md` | Schritt 4 | Listen-Endpunkt, Zeitfenster, Cursor, Filter |
| `nachrichtendetail.md` | Schritt 5 | Detailansicht, Prozessschritte, Eigenschaften |
| `prozessschritte-uebersetzung.md` | Schritt 5 | Zuordnungstabelle `SOSActionServiceProperties` → Klartext |
| `verkettung.md` | Schritt 6 | Auflösung über Split, Merge und Quittung |
| `bam-suche.md` | Schritt 7 | Suche über `MessageBAM`, Limit und Mindestlänge |
| `rohdaten-download.md` | Schritt 8 | Filestore-Auflösung, Proxy, Protokollierung |
| `prozess-katalog.md` | Schritt 9 | `process_catalog`, Heuristik-Import, Massenzuordnung |
| `benutzerverwaltung.md` | Schritt 9 | Anlegen, Sperren, Rolle, Passwort zurücksetzen |
| `rollup.md` | Schritt 10 | `message_rollup`, stündlicher Job, Rückwärtslauf |
| `dashboard.md` | Schritt 10 | Kennzahlen, die drei Problemkategorien |
| `process-view.md` | Schritt 10 | Prozessansicht, gruppiert nach kuratiertem Partner |

Diese Tabelle ist eine Vorschau, keine Zusage über Dateinamen. Wer einen Schritt umsetzt, trägt
die tatsächlich entstandenen Dateien hier ein.

### Erhebungen

Einmalig erhobene Fakten über das Quellsystem, die sonst niemand mehr nachvollziehen kann:

| Datei | Entsteht in | Inhalt |
|---|---|---|
| `message-status-werte.md` | Schritt 3 | Ergebnis von `SELECT DISTINCT MessageStatus` — prüft Annahme A6 aus der Projektbeschreibung |
