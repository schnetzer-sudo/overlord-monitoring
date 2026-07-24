# Overlord Monitoring

Monitoring-Werkzeug für die EDI-Integrationsplattform **Overlord**. Es liest ausschließlich lesend
aus der bestehenden MariaDB `GlassfishDB` und zeigt Mandanten den Zustand ihrer EDI-Übertragungen —
Nachrichtenliste, Detailansicht mit verständlichen Prozessschritten, Verkettung über Aufteilung und
Zusammenführung, Suche über fachliche Belegnummern, Kennzahlen und Rohdaten-Download.

Der typische Nutzer ist kein EDI-Spezialist. Er sucht einen Beleg und will wissen, wo dieser steht.

> **Arbeitest du an diesem Projekt — ob Mensch oder Modell — lies zuerst [`CLAUDE.md`](CLAUDE.md).**
> Danach [`DEVELOPMENT_GUIDELINES.md`](DEVELOPMENT_GUIDELINES.md), danach die für deine Aufgabe
> relevanten Dateien in [`docs/`](docs/README.md).

## Stand

Schritt 1 von 10 ist umgesetzt: Repository-Struktur, Steuerungsdateien und leere Gerüste. Es gibt
**noch keine Datenbankverbindung, keine Authentifizierung und keine fachlichen Endpunkte**. Der
Fahrplan steht in [`docs/IMPLEMENTIERUNGSPLAN_MVP.md`](docs/IMPLEMENTIERUNGSPLAN_MVP.md).

## Aufbau

```
├─ CLAUDE.md                  Arbeitsanweisung — zuerst lesen
├─ DEVELOPMENT_GUIDELINES.md  Architektur, Konventionen, die unverhandelbaren Regeln
├─ docs/                      Projektbeschreibung, Plan, eine Datei je Feature
├─ backend/                   Spring Boot 4.1, Java 21, Maven
└─ frontend/                  Next.js 16, TypeScript, pnpm
```

## Voraussetzungen

| Werkzeug | Version | Anmerkung |
|---|---|---|
| JDK | 21 | z. B. Eclipse Temurin |
| Maven | — | nicht nötig, `./mvnw` bringt ihn mit |
| Node.js | 20.9 oder neuer | die CI baut mit Node 24 |
| pnpm | 11.x | `npm install -g pnpm` |

## Loslegen

```bash
# Backend — bauen, prüfen, starten
cd backend
./mvnw verify
./mvnw spring-boot:run          # http://localhost:8080/actuator/health

# Frontend — installieren, bauen, starten
cd frontend
pnpm install
pnpm build
pnpm dev                        # http://localhost:3000
```

Unter Windows in PowerShell statt `./mvnw` einfach `.\mvnw`.

## Konfiguration

Es liegen **keine Zugangsdaten im Repository**. Das Backend liest alles Umgebungsspezifische aus
Umgebungsvariablen; die Vorlage ist [`backend/.env.example`](backend/.env.example). Für lokale
Entwicklung kopieren:

```bash
cp backend/.env.example backend/.env
```

`backend/.env` ist von Git ignoriert und wird nie eingecheckt.

## Prüfungen

| Zweck | Befehl |
|---|---|
| Backend vollständig prüfen | `cd backend && ./mvnw verify` |
| Backend formatieren | `cd backend && ./mvnw spotless:apply` |
| Frontend vollständig prüfen | `cd frontend && pnpm check` |
| Frontend formatieren | `cd frontend && pnpm format` |

Dieselben Prüfungen laufen in der CI bei jedem Push und jedem Pull Request
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)).
