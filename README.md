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
├─ scripts/                   Helfer für die lokale Entwicklung
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

### Beides auf einmal (Windows)

```powershell
.\scripts\dev-start.ps1
```

Startet Backend und Frontend in zwei eigenen Fenstern und öffnet am Ende den Browser. Davor prüft
das Skript der Reihe nach, was erfahrungsgemäß schiefgeht: ob die `OVERLORD_*`-Variablen gesetzt
sind, ob die Datenbank über das Netz erreichbar ist, und **ob die Ports 8080 und 3000 noch frei
sind**. Der letzte Punkt ist der eigentliche Grund für das Skript: Eine vergessene Altinstanz lässt
den Health-Check grün melden, während man die alte Version testet.

Das Skript **enthält und setzt keine Zugangsdaten** — es liest die Umgebungsvariablen nur, um den
Datenbank-Host für den Erreichbarkeitstest zu finden.

| Schalter | Wirkung |
|---|---|
| `-Force` | startet auch bei belegten Ports |
| `-CodeGen` | lässt die jOOQ-Codegenerierung mitlaufen (langsamer, braucht die Datenbank) |
| `-NoBrowser` | öffnet am Ende keinen Browser |
| `-DbHost` / `-DbPort` | Ziel des Erreichbarkeitstests von Hand setzen |

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
