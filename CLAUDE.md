# CLAUDE.md — Arbeitsanweisung

## Das Projekt

**Overlord Monitoring** ist ein neues Monitoring-Werkzeug für die EDI-Integrationsplattform
*Overlord*. Es liest ausschließlich lesend aus der bestehenden MariaDB `GlassfishDB` und zeigt
Mandanten den Zustand ihrer EDI-Übertragungen. Das Altwerkzeug (ExtJS 4.1) wird **nicht**
nachgebaut, es dient nur als fachliche Referenz. Der typische Nutzer ist kein EDI-Spezialist: Er
sucht einen Beleg und will wissen, wo dieser steht — jede Entscheidung fällt im Zweifel zugunsten
dieser Frage, nicht zugunsten technischer Vollständigkeit.

## Lesereihenfolge vor jeder Arbeit

Lies in dieser Reihenfolge, **bevor** du Code schreibst:

1. `CLAUDE.md` (diese Datei)
2. `DEVELOPMENT_GUIDELINES.md` — Architektur, Konventionen, die Regeln in voller Länge
3. Die für die Aufgabe relevanten Dateien in `docs/` — mindestens
   `docs/PROJEKTBESCHREIBUNG.md` und `docs/datenmodell.md`

`docs/PROJEKTBESCHREIBUNG.md` und `docs/IMPLEMENTIERUNGSPLAN_MVP.md` sind die verbindliche
Wahrheit. Bei Widerspruch zwischen Code und diesen Dateien gilt die Datei.

## Die unverhandelbaren Regeln — Kurzfassung

Ausführlich und begründet in `DEVELOPMENT_GUIDELINES.md`. Keine dieser Regeln wird
stillschweigend gebrochen:

1. **Kein Endpunkt nimmt jemals eine Mandanten-ID entgegen.** Der Mandant kommt aus der Session.
2. **Jede Repository-Methode hat den Mandanten als ersten Pflichtparameter.** Keine Überladung
   ohne ihn.
3. **Kein Schreibzugriff auf `GlassfishDB`** — unter keinen Umständen.
4. **Jeder Listen-Endpunkt hat ein Pflicht-Zeitfenster.** Standard 24 h, Maximum ein Jahr.
5. **Keine `OFFSET`-Paginierung** — ausschließlich cursor-basiert.
6. **`MessageProperty` nur über `MessageID`** — nie über den Wert filtern, gruppieren, sortieren.
7. **Keine Live-Aggregation über `Message`** — Kennzahlen kommen aus `message_rollup`.
8. **`LocalDateTime.now()` wird nirgends direkt aufgerufen** — stattdessen die `Clock`-Beans aus
   `common/ZeitConfig` (Anwendungsuhr bzw. `systemClock`).
9. **Fehlerprüfung nur über `MessageStatusClassifier.fehlerBedingung(...)`** — niemals über das
   naive `LIKE 'ERROR_%'` (`_` ist ein Platzhalter). Die dort umgesetzte Fassung und ihre
   Begründung stehen in `docs/message-status.md`.
10. **Pro Endpunkt ein Mandanten-Isolationstest.** Ohne ihn wird nicht gemergt.

## Dokumentationspflicht

**Ein Schritt gilt erst als fertig, wenn die Dokumentation steht.**

- Neues Feature → **neue Datei** in `docs/`
- Geändertes Feature → **aktualisierte Datei** in `docs/`
- Neue Datei → Eintrag in `docs/README.md`

Code ohne zugehörige Dokumentation ist unfertig, auch wenn er läuft.

## Befehle

| Zweck | Befehl |
|---|---|
| Backend bauen und prüfen | `cd backend && ./mvnw verify` |
| Backend starten (Profil `dev`) | `cd backend && ./mvnw spring-boot:run` |
| Backend-Tests | `cd backend && ./mvnw test` |
| Backend formatieren | `cd backend && ./mvnw spotless:apply` |
| Frontend-Abhängigkeiten | `cd frontend && pnpm install` |
| Frontend bauen und prüfen | `cd frontend && pnpm build` |
| Frontend starten | `cd frontend && pnpm dev` |
| Frontend prüfen | `cd frontend && pnpm lint && pnpm typecheck && pnpm format:check` |

Formatierung und Linting sind im Build verankert. `./mvnw verify` schlägt bei
Formatverstößen fehl; `pnpm build` läuft erst nach `lint` und `typecheck`.

## Was ausdrücklich nicht getan werden darf

- **Nicht** in `GlassfishDB` schreiben, auch nicht testweise, auch nicht in einer Migration.
- **Keine** Zugangsdaten, Hostnamen, Passwörter oder Produktionsdaten ins Repository.
  Platzhalter über Umgebungsvariablen, Vorlage in `backend/.env.example`.
- **Kein** JPA/Hibernate. Datenzugriff läuft über jOOQ.
- **Keine** Muster aus Spring Boot 3 / Spring Security 6 / Jackson 2 übernehmen. Dieses Projekt
  läuft auf Spring Boot 4 mit Spring Framework 7, Spring Security 7 und Jackson 3.
- **Nichts erfinden**, was nicht in `docs/PROJEKTBESCHREIBUNG.md` steht — insbesondere keine
  geratenen Partner, Richtungen oder Belegarten aus Namen ableiten. Nicht zugeordnet heißt
  „nicht zugeordnet“.
- **Nicht** über den Umfang des aktuellen Schritts hinausbauen. Die Abgrenzung jedes Schritts
  steht in `docs/IMPLEMENTIERUNGSPLAN_MVP.md`.

## Bei Unklarheit

**Frag nach, statt zu raten.** Fehlt eine Information, widersprechen sich zwei Vorgaben, oder ist
eine Anforderung mehrdeutig: nachfragen. Eine falsche Annahme im Fundament kostet mehr als eine
Rückfrage. Bei Versions- und Bibliotheksfragen gilt zusätzlich: aktuelle Dokumentation prüfen,
nicht aus dem Gedächtnis schreiben.
