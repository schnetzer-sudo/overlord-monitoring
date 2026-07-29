# Entwicklungsrichtlinien

Stand: 24.07.2026 · Abgeleitet aus `docs/PROJEKTBESCHREIBUNG.md`.

Diese Datei beschreibt **wie** gebaut wird. **Was** gebaut wird, steht in
`docs/PROJEKTBESCHREIBUNG.md` und `docs/IMPLEMENTIERUNGSPLAN_MVP.md`. Bei Widerspruch zwischen
dieser Datei und der Projektbeschreibung gilt die Projektbeschreibung.

---

## 1. Architekturüberblick

Drei Teile, klar getrennte Zuständigkeiten:

| Teil | Zuständig für | Nicht zuständig für |
|---|---|---|
| **Backend** (Spring Boot 4.1, Java 21, jOOQ) | Datenzugriff, Mandantentrennung, Berechtigungen, Kennzahlen, Rohdaten-Proxy | Darstellung |
| **Frontend** (Next.js 16, App Router) | Darstellung, Filterzustand, Navigation | Berechtigungsentscheidungen, Datenhaltung |
| **`GlassfishDB`** (MariaDB, fremd) | Quelldaten des Altsystems | — nur lesend |
| **`overlord_monitor`** (MariaDB, eigen) | Benutzer, Katalog, Rollup, Protokoll, gespeicherte Filter | Quelldaten |

### Die beiden Datenbankschemata

`GlassfishDB` gehört uns nicht. Es wird **ausschließlich gelesen**. Der DB-Benutzer für dieses
Schema besitzt ausschließlich `SELECT`.

`overlord_monitor` liegt auf derselben MariaDB-Instanz und wird per Flyway verwaltet. Flyway
verwaltet **nur** dieses Schema.

Beide Datenbankbenutzer erhalten `SELECT` auf beide Schemata, damit schemaübergreifende Joins in
einer Abfrage möglich sind (`GlassfishDB.Message` gejoint auf `overlord_monitor.process_catalog`).
Nur einer der beiden besitzt zusätzlich Schreibrechte, und ausschließlich auf `overlord_monitor`.
Die Schreibgarantie hängt an den DB-Rechten, nicht an der Trennung der Verbindungen. Der Lese-Pool
wird zusätzlich auf JDBC-Ebene als `readOnly` markiert.

**Die Regel für die beiden jOOQ-`DSLContext`s** (ab Schritt 2) lautet nicht „`monitor`-Klassen
gehören zum Schreib-Kontext", sondern:

> Der Lese-`DSLContext` (`glassfishDsl`) darf **beide** Schemata lesen. Der Schreib-`DSLContext`
> (`monitorDsl`) darf ausschließlich `overlord_monitor` und ausschließlich dort schreiben.

Schemaübergreifende Abfragen laufen zwingend über eine einzige Verbindung, also über den Lese-Pool.
Deshalb kein `defaultSchema` in der jOOQ-Konfiguration — Schemanamen werden immer voll qualifiziert
gerendert. Keine der beiden DataSources ist `@Primary`; es gibt genau einen
`PlatformTransactionManager`, gebunden an `overlord_monitor`. Vollständig in
[`docs/datenzugriff.md`](docs/datenzugriff.md).

**Keine lokale Datenbank für die Entwicklung, kein `docker-compose.yml`.** Entwickelt wird gegen
die Testkopie. Eine Datenbank im Container wäre nutzlos, weil `overlord_monitor` wegen der
schemaübergreifenden Joins zwingend auf derselben Instanz liegen muss wie `GlassfishDB`. Container
werden erst beim Betrieb relevant.

**Keine Fremdschlüssel über Schemagrenzen.** `process_catalog` verweist auf die `ProcessID` als
Zeichenkette. Wird ein Prozess im Altsystem gelöscht, bleibt die Katalogzeile bestehen — sonst
verschwände rückwirkend die Partnerzuordnung aller historischen Nachrichten. Verwaiste Einträge
sind hier erwünscht.

**Kein JPA.** Das Quellschema gehört uns nicht, ist lesegetrieben und teilweise EAV — ORM-Mapping
wäre hier ein Nachteil. jOOQ generiert typsichere Zugriffe aus dem Schema.

### Warum das Frontend keine eigenen Daten hält

Das Frontend hat **keine Datenbank und keinen eigenen Datenbestand**. Es spricht ausschließlich
über HTTP mit dem Backend. Next.js dient als BFF für das Cookie-Handling und trifft **keine**
Berechtigungsentscheidungen. Rollenabhängige Navigation ist reine Bequemlichkeit; verbindlich
prüft immer das Backend.

Der Grund ist die Mandantentrennung. Sie ist bei externen Nutzern eine Sicherheitsanforderung,
keine Komfortfunktion. Gäbe es zwei Orte, an denen Daten liegen, gäbe es zwei Orte, an denen die
Trennung durchgesetzt werden müsste — und einer davon läuft im Browser des Nutzers. Es gibt genau
eine Stelle, an der über Sichtbarkeit entschieden wird: das SQL-Statement im Backend.

---

## 2. Paketstruktur Backend

Der folgende Abschnitt ist **wörtlich** aus Abschnitt 6 der Projektbeschreibung übernommen:

> **Basispaket: `de.kraftwerkone.overlord.monitor`.** Reverse-Domain zu `overlord.kraftwerkone.de`.
> `overlord.monitor` statt `overlordmonitor`, damit das Werkzeug im Namensraum sichtbar vom
> beobachteten System getrennt bleibt. Maven: `groupId = de.kraftwerkone`,
> `artifactId = overlord-monitor`. Die Hauptklasse `OverlordMonitorApplication` liegt direkt
> im Basispaket, damit der Component-Scan die gesamte Anwendung erfasst.
>
> Paketstruktur — fachlich geschnitten, nicht nach Schichten, passend zur Regel "pro Ansicht ein
> eigenes Modul":
>
> ```
> de.kraftwerkone.overlord.monitor
> ├─ config/      DataSources, jOOQ, Security, Flyway
> ├─ common/      Fehlerformat, Cursor-Paginierung, Filterabstraktion, TimeProvider
> ├─ security/    MandantContext, Session, Anmeldesperre
> ├─ audit/
> ├─ message/     Liste, Detail, Verkettung
> ├─ bam/
> ├─ payload/     Download-Proxy
> ├─ catalog/     process_catalog, partner
> ├─ rollup/      per Profil separat startbar
> ├─ dashboard/
> ├─ admin/
> └─ jooq/        generiert, nicht handgepflegt
> ```
>
> Fachpakete kennen einander nicht. Gemeinsames liegt in `common`, nicht in einem Nachbarmodul.
>
> **Zwei getrennte Zielpakete für die jOOQ-Codegenerierung:**
>
> ```
> de.kraftwerkone.overlord.monitor.jooq.glassfish   ← Quellschema, ausschließlich lesend
> de.kraftwerkone.overlord.monitor.jooq.monitor     ← eigenes Schema, schreibend
> ```
>
> Das ist Schutz, keine Kosmetik. Bei einem schemaübergreifenden Join ist am Import sofort
> erkennbar, ob gerade eine Tabelle angefasst wird, auf die niemand schreiben darf. Gleichnamige
> Tabellen in beiden Schemata kollidieren nicht.
>
> Der Rollup-Job bekommt trotz separater Startbarkeit kein eigenes Wurzelpaket. Die Trennung läuft
> über das Spring-Profil, nicht über den Namensraum.

### Was daraus für den Alltag folgt

`jooq/` wird **nicht von Hand angelegt**. Es entsteht in Schritt 2 durch Codegenerierung und wird
nie manuell bearbeitet.

Die Regel „Fachpakete kennen einander nicht" wird durch `PaketstrukturTest` maschinell geprüft.
Damit sie prüfbar ist, sind die Pakete in drei Rollen eingeteilt:

| Rolle | Pakete | Regel |
|---|---|---|
| **Verdrahtung** | `config` | Darf alles sehen. Niemand importiert aus `config`. |
| **Gemeinsames** | `common`, `security`, `audit` | Dürfen von jedem Fachpaket genutzt werden. `common` importiert aus keinem anderen Anwendungspaket. |
| **Fachpakete** | `message`, `bam`, `payload`, `catalog`, `rollup`, `dashboard`, `admin` | Kennen einander **nicht**. |

`security` und `audit` stehen bei „Gemeinsames", weil die Projektbeschreibung sie an mehreren
Stellen quer fordert: Der `MandantContext` ist Pflichtparameter jeder Repository-Methode
(Abschnitt 7), und protokolliert werden Anmeldungen, Fehlversuche, Passwortänderungen,
Katalogänderungen und jeder Rohdaten-Download (Abschnitte 5 und 7) — also aus `security`,
`payload`, `catalog` und `admin`.

**Braucht ein zweites Fachpaket einen Typ, wandert der Typ nach `common`** — niemals importiert
ein Fachpaket aus einem Nachbarpaket. Sucht die BAM-Suche eine Nachrichten-Kurzdarstellung, liegt
diese in `common`, nicht in `message`.

### Aufbau innerhalb eines Fachpakets

Innerhalb eines Fachpakets wird nach Schichten geordnet — aber ohne Unterpakete, solange das Paket
klein bleibt:

```
message/
├─ MessageController.java     REST, nimmt nie eine Mandanten-ID entgegen
├─ MessageService.java        Fachlogik
├─ MessageRepository.java     jOOQ, Mandant ist erster Pflichtparameter
└─ <Name>Dto.java / <Name>Record.java
```

Nur Repository-Klassen dürfen Typen aus `…jooq.glassfish` verwenden. Services und Controller
sehen ausschließlich eigene Typen. Ab Schritt 2 prüft ArchUnit das.

### Namenskonventionen

- Klassen: `<Fachbegriff><Rolle>` — `MessageController`, `ProcessCatalogRepository`
- DTOs nach außen: `…Response` / `…Request`, unveränderlich, als `record`
- Interfaces ohne `I`-Präfix, Implementierungen mit `…Impl` nur wenn es genau eine gibt
- Tests: `<KlasseUnterTest>Test` (Unit), `<Thema>IT` (Integration),
  `<Endpunkt>IsolationTest` (Mandanten-Isolation)
- **Fachsprache deutsch, Code englisch.** Die Datenbankbegriffe (`Message`, `MessageBAM`,
  `ProcessID`) werden nicht eingedeutscht — sie sind die Sprache des Quellsystems. Kommentare,
  Dokumentation und Oberflächentexte sind deutsch.

---

## 3. Ordnerstruktur Frontend

```
frontend/
├─ src/
│  ├─ app/
│  │  ├─ (public)/              Routengruppe: nicht angemeldet
│  │  │  └─ anmeldung/page.tsx
│  │  ├─ (app)/                 Routengruppe: angemeldet, mit Anwendungsrahmen
│  │  │  ├─ layout.tsx
│  │  │  ├─ dashboard/page.tsx
│  │  │  ├─ nachrichten/page.tsx
│  │  │  ├─ prozesse/page.tsx
│  │  │  └─ administration/page.tsx
│  │  ├─ layout.tsx             Wurzel-Layout
│  │  ├─ providers.tsx          TanStack Query und nuqs — die einzige "use client"-Insel oben
│  │  └─ globals.css
│  ├─ components/
│  │  └─ ui/                    ausschließlich shadcn/ui — nicht von Hand erweitern
│  ├─ features/
│  │  └─ <feature>/             fachliche Bausteine, spiegeln die Backend-Fachpakete
│  │     ├─ components/
│  │     ├─ hooks/
│  │     └─ api.ts
│  └─ lib/                      Infrastruktur: HTTP-Client, Query-Client, Formatierung, Utils
├─ components.json              shadcn/ui-Konfiguration — nicht von Hand ändern
├─ pnpm-workspace.yaml          Freigabe von Installationsskripten (allowBuilds)
├─ .prettierrc.json
└─ package.json
```

- **Routengruppen** `(public)` und `(app)` trennen angemeldet von nicht angemeldet. Die Klammern
  erscheinen nicht in der URL; sie erlauben zwei unterschiedliche Layouts.
- **`components/ui`** gehört shadcn/ui. Was der Generator hierhin schreibt, wird nicht von Hand
  umgebaut. Eigene Bausteine liegen in `features/<feature>/components` oder in
  `components/` direkt darüber.
- **`features/<feature>`** ist der fachliche Schnitt und spiegelt die Backend-Fachpakete
  (`nachrichten` ↔ `message`, `bam`, `prozesse` ↔ `catalog`, …). Wie im Backend gilt: Ein Feature
  importiert nicht aus einem Nachbarfeature. Gemeinsames liegt in `components/` oder `lib/`.
- **`lib`** ist Infrastruktur, nie Fachlichkeit.
- **Routennamen sind deutsch**, weil sie der Nutzer sieht und teilt.

---

## 4. Die unverhandelbaren Regeln

Vollständig aus Abschnitt 7 (Sicherheit) und Abschnitt 8 (Leistung) der Projektbeschreibung.
Diese Regeln werden nicht abgewogen. Wer meint, eine davon brechen zu müssen, fragt nach.

### 4.1 Mandantentrennung — die wichtigste Regel des Projekts

**M1 — Kein Endpunkt nimmt eine Mandanten-ID entgegen.** Nicht als Pfadsegment, nicht als
Query-Parameter, nicht im Body, nicht im Header. Der Mandant wird ausschließlich aus der Session
gelesen.

**Genau zwei Ausnahmen**, beide namentlich in [`docs/mandantentrennung.md`](docs/mandantentrennung.md)
geführt: `POST /api/auth/mandant` (Wechsel, geprüft gegen die zulässige Menge) und
`POST /api/admin/users` (Anlegen eines Kontos, nur ADMIN). Die erste fragt einen Datenausschnitt, die
zweite definiert ein Konto. Taucht dort jemals eine dritte auf, ist das ein Signal und keine
Kleinigkeit.

**M2 — Jede Repository-Methode bekommt den Mandanten als ersten Pflichtparameter.** Es gibt keine
Überladung ohne ihn. Eine Methode `findById(String messageId)` ohne Mandant darf nicht existieren,
auch nicht `private`, auch nicht „nur für den Test".

Der Typ ist `security/MandantContext` und trägt **genau eine** Mandanten-ID — für beide Rollen. Auch
ADMIN wählt einen Mandanten und wechselt ihn, statt alle gleichzeitig zu sehen; damit gibt es keinen
Codepfad ohne Mandantenfilter und die Signaturen sind rollenunabhängig. `PaketstrukturTest` prüft die
Regel maschinell (ab Schritt 3): Jede öffentliche Methode einer Klasse, die Typen aus
`jooq.glassfish` verwendet, hat `MandantContext` als ersten Parameter.

Die **einzige** Ausnahme sind die Methoden, die den Kontext erst *herstellen* — sie lesen die Menge
der zulässigen Mandanten, bevor feststeht, welcher aktiv ist. Sie tragen `@OhneMandantenkontext` mit
Pflichtbegründung, stehen ausschließlich im `MandantRepository` und liefern niemals fachliche Daten.
Vollständig geführt in [`docs/mandantentrennung.md`](docs/mandantentrennung.md).

**M3 — Der Filter über `ProjectMandant` ist Bestandteil jedes Statements**, nicht nachgelagerte
Prüfung. Wer eine fremde `MessageID` errät, bekommt null Zeilen — weil die Zeile für ihn nie
existiert hat. Kein „erst laden, dann prüfen".

**M4 — Pro Endpunkt existiert ein automatisierter Isolationstest**, der mit Mandant A abfragt und
nachweist, dass Daten von Mandant B unerreichbar sind. **Ein neuer Endpunkt ohne diesen Test wird
nicht gemergt.** Die Vorlage ist `MandantenIsolationDbIT` (Schritt 3); sie enthält zusätzlich die
Gegenprobe mit einer erfundenen ID.

**404 statt 403.** Eine nicht vorhandene Ressource und eine Ressource eines fremden Mandanten liefern
**dieselbe** Antwort — nicht nur denselben Statuscode, sondern denselben Rumpf. Ein `403` verriete,
dass der Datensatz existiert.

**M5 — Die Mandantentrennung gilt auch quer.** Auch verkettete Nachrichten, auch Suchtreffer, auch
Rollup-Zeilen, auch Downloads unterliegen ihr.

### 4.2 Schreibverbot

**S1 — Kein Datenbankbenutzer dieser Anwendung besitzt irgendein Schreibrecht auf `GlassfishDB`.**
Es wird niemals in dieses Schema geschrieben — kein `INSERT`, `UPDATE`, `DELETE`, kein DDL, keine
Flyway-Migration, kein Testfixture. Der Lese-Pool ist zusätzlich auf JDBC-Ebene als `readOnly`
markiert, und ein jOOQ-`ExecuteListener` weist auf dem Lese-`DSLContext` alles außer `SELECT` ab.

**S2 — Flyway verwaltet ausschließlich `overlord_monitor`.**

### 4.3 Rohdatenzugriff

**R1 — Der Download läuft immer über das Backend als Proxy, niemals als direkter Link in den
Browser.** Der Filestore kennt unsere Nutzer nicht und kann die Mandantenprüfung nicht leisten.
Ein durchgereichter Link wäre ein unkontrollierter, per Copy-Paste teilbarer Zugang.

**R2 — Ausschließlich `Content-Disposition: attachment`, niemals inline rendern.** Eine EDI-Datei
kann gültiges HTML oder SVG enthalten — inline wäre das eine von außen befüllbare
Cross-Site-Scripting-Lücke.

**R3 — `Content-Type: application/octet-stream`**, kein Erraten des Typs.

**R4 — Jeder Download wird mit Nutzer, Nachricht, Zeitpunkt und IP im `audit_log` protokolliert.**

**R5 — Erst Mandantenprüfung, dann Abruf.** Nicht umgekehrt.

**R6 — Die Download-Berechtigung ist ein Flag an `app_user`, Standardwert erlaubt.** Fachlich sind
alle Mandantennutzer berechtigt; das Flag wird trotzdem von Anfang an modelliert, damit ein
späterer Entzug keine Migration erfordert.

Der Grund für die Strenge: Die EDI-Rohdatei ist deutlich sensibler als die Metadaten. Dort stehen
Preise, Mengen und Kundendaten.

### 4.4 Leistung

Größenordnung: 10.000 bis 100.000 Nachrichten pro Tag, ein Jahr Aufbewahrung. Bis zu rund
36 Millionen Zeilen in `Message`, mehrere hundert Millionen in `MessageProperty`. **Es wird auf der
Produktionsdatenbank gelesen.** Eine laufend aktualisierte Replica existiert nicht. Daher sind
L1 bis L6 verbindlich und nicht verhandelbar.

**L1 — Jeder Listen-Endpunkt hat ein Pflicht-Zeitfenster.** Standard 24 Stunden, Maximum ein Jahr.
Ohne Zeitfenster keine Abfrage. Fehlt es in der Anfrage, wird der Standard gesetzt — es wird nie
unbegrenzt gelesen.

**L2 — Keine Live-Aggregation über `Message`.** Dashboard-Kennzahlen kommen ausschließlich aus
`message_rollup`. Ein stündlicher Job schreibt inkrementell fort.

**L3 — Keine `OFFSET`-Paginierung.** Cursor-basiert über `(MessageLastUpdate, MessageID)`.

**L4 — `MessageProperty` nur über `MessageID`.** Nie filtern, gruppieren oder sortieren über
`MessagePropertyValue`. Die Indizes darauf sind Präfix-Indizes über 50 Zeichen und für Aggregation
ungeeignet.

**L5 — BAM-Suche mit hartem Limit und Mindestlänge** des Suchbegriffs. BAM-Werte wie `050` kommen
millionenfach vor.

**L6 — Der Rollup-Job läuft gedrosselt.** Er teilt sich die Instanz mit der Produktion.

**L7 — Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen** (`EXPLAIN` plus
Laufzeit). Kein Statement geht ungeprüft in Produktion. Das Ergebnis gehört in die
Feature-Dokumentation unter `docs/`.

### 4.5 Zeit

**Z1 — `LocalDateTime.now()` wird nirgends direkt aufgerufen.** Das gilt ebenso für
`LocalDate.now()`, `Instant.now()`, `ZonedDateTime.now()`, `OffsetDateTime.now()`,
`new Date()` und `System.currentTimeMillis()`.

Stattdessen zwei `java.time.Clock`-Beans in `common` (`common/ZeitConfig`, ab Schritt 2): die
**Anwendungsuhr** (`@Primary`) und `systemClock`. In Produktion ist die Anwendungsuhr die Systemuhr,
im Dev-Profil um den Rückstand der Testkopie zurückversetzt (Maximum aus `Message.MessageLastUpdate`,
läuft weiter statt einzufrieren). Grund: Die Testkopie liegt hinter der realen Uhrzeit; ein
Standard-Zeitfenster von 24 Stunden liefert dort sonst null Zeilen und lässt korrekte
Anwendungsteile kaputt aussehen. Nebeneffekt: Die Anwendung wird testbar.

**Sicherheitsrelevante Zeit** (Sitzungsablauf, Sperrfristen ab Schritt 3) und Protokollzeit nutzen
niemals die Anwendungsuhr, sondern `systemClock` (echte Uhr, UTC).

Die einzige Stelle, die die Systemuhr liest, ist `ZeitConfig` in `common`. `PaketstrukturTest`
prüft, dass `now()` und Verwandte außerhalb von `common` nicht aufgerufen werden.

**Z2 — `MessageTimeout` ist eine Dauer in Minuten, kein Zeitpunkt.** Der Timeout-Zeitpunkt wird im
Backend als `MessageLastUpdate + MessageTimeout` berechnet.

### 4.6 SQL-Fallstricke

**Q1 — Fehlerprüfung über `LEFT(MessageStatus, 6) = 'ERROR_'`.** Niemals `LIKE 'ERROR_%'`: In SQL
ist `_` ein Platzhalter für ein beliebiges Zeichen, `LIKE 'ERROR_%'` trifft auch `ERRORX…`. Zeigt
eine Messung ein Problem mit dem Index, ist die Alternative
`MessageStatus LIKE 'ERROR\_%' ESCAPE '\'` — nicht die naive Variante.

**Q2 — Es gibt kein Anlagedatum an `Message`.** `MessageLastUpdate` ist der Zeitpunkt der letzten
Änderung. Der fachliche Start ist `MIN(MessageAction.MessageActionStart)`.

**Q3 — Die drei Problemkategorien bleiben getrennt.** *Fehler* (`ERROR_*`), *Überfällig*
(läuft noch, `MessageLastUpdate + MessageTimeout` in der Vergangenheit) und *Unquittiert*
(ausgehend ohne Empfangsbestätigung) werden nie zu „Fehler" zusammengefasst. `SUSPENDED` ist
**kein** Fehler.

**Q4 — Nicht zugeordnet heißt „nicht zugeordnet".** Partner, Standort, Richtung und Belegart
werden **kuratiert, nicht geparst**. Eine Heuristik befüllt vor, die Wahrheit steht im
`process_catalog`. Nicht zugeordnete Prozesse erscheinen in Auswertungen sichtbar als „nicht
zugeordnet" — niemals als geratener Wert, niemals stillschweigend verteilt.

### 4.7 Authentifizierung

**A1 — BCrypt mit Kostenfaktor 12.** Serverseitige Session, Session-ID im Cookie mit `HttpOnly`,
`SameSite=Lax` und `Secure`. **Kein JWT** — bei externen Nutzern wiegt sofortige Rücknehmbarkeit
schwerer als Zustandslosigkeit. `Secure` ist **per Profil schaltbar** und im Profil `dev` aus; ohne
das funktioniert die lokale Entwicklung über `http://localhost` nicht.

**A2 — Sperre nach fünf Fehlversuchen für 15 Minuten**, zusätzlich Begrenzung pro IP. Die
Nutzersperre ist persistent in `app_user`, die **IP-Begrenzung liegt ausschließlich im
Arbeitsspeicher** und schreibt niemals in die Datenbank — sonst wäre der Schutzmechanismus selbst der
Angriffsvektor.

**A3 — Fehlermeldungen bleiben unspezifisch.** Nie „Benutzer unbekannt" gegen „Passwort falsch"
unterscheiden, weder im Text noch in der Antwortzeit — deshalb wird auch bei unbekanntem
Benutzernamen ein BCrypt-Vergleich gegen einen Dummy-Hash gerechnet. **Eine Ausnahme:** War das
Passwort korrekt und das Konto ist gesperrt oder deaktiviert, darf das benannt werden.

**A4 — Die Klartext-Passwörter der Alt-`User`-Tabelle werden nicht übernommen** und gelten als
kompromittiert. **Es findet überhaupt keine Migration statt** (Entscheidung 28.07.2026): Konten
entstehen einzeln über `POST /api/admin/users`, das erste über das Profil `bootstrap`. Jedes neue
Konto startet mit Zwang zur Passwortänderung.

**A5 — Sicherheitsrelevante Zeit rechnet mit der Systemuhr.** Sperrfristen, Sitzungsablauf und
Protokollzeit nutzen `systemClock` aus `common/ZeitConfig`, **niemals** die Anwendungsuhr — die ist
im Dev-Profil um Wochen zurückversetzt, und eine Sperre liefe dort erst in drei Wochen ab.

Vollständig in [`docs/authentifizierung.md`](docs/authentifizierung.md).

### 4.8 Geheimnisse

**G1 — Keine Zugangsdaten, Hostnamen, Passwörter, Zertifikate oder Produktionsdaten im
Repository.** Alles über Umgebungsvariablen, Vorlage in `backend/.env.example`. `.env` ist
ignoriert.

---

## 5. API-Konventionen

### 5.1 Die API ist kennzahlenorientiert, nicht tabellenorientiert

Endpunkte bilden **fachliche Fragen** ab, keine Datenbanktabellen. Nicht `GET /api/message-bam`,
sondern `GET /api/suche?begriff=…`. Nicht `GET /api/message-rollup`, sondern
`GET /api/dashboard/volumen`.

Die Begründung steht in Abschnitt 10 der Projektbeschreibung: Die erste Ausbaustufe ist ein
Chatbot, und der arbeitet **nicht** mit rohem Text-to-SQL — bei EAV plus Namenskonventionen
erfindet jedes Modell plausible Zahlen. Stattdessen ruft er Werkzeuge auf, die auf dieselben
Endpunkte gehen, die auch das Frontend nutzt. Das ist der Grund, die API von Anfang an
kennzahlenorientiert statt tabellenorientiert zu schneiden.

Praktisch heißt das: Ein Endpunkt liefert eine beantwortbare Frage vollständig — inklusive der
übersetzten, lesbaren Werte. Der Aufrufer soll nichts nachschlagen und nichts zusammenrechnen
müssen.

### 5.2 Benennung

- Präfix `/api`, danach die fachliche Ressource: `/api/nachrichten`, `/api/suche`,
  `/api/dashboard/volumen`
- **Kein Versionspräfix.** Frontend und Backend werden gemeinsam ausgeliefert, es gibt keinen
  fremden Konsumenten. Ein `/v2` lässt sich jederzeit nachrüsten, falls sich das ändert.
- Pfadsegmente **kebab-case**, deutsch, Mehrzahl bei Sammlungen
- JSON-Felder **camelCase**, englisch — sie sind Code, nicht Oberfläche
- Kein Verb im Pfad. Die HTTP-Methode ist das Verb. Ausnahme sind Vorgänge ohne Ressourcen­charakter
  (`POST /api/anmeldung`, `POST /api/abmeldung`)
- **Niemals** ein Segment oder Parameter `mandant`/`mandantId` — siehe M1

### 5.3 Datum und Zeit

- **In der Übertragung ausschließlich ISO 8601 mit UTC**: `2026-07-24T18:30:00Z`. Keine lokalen
  Zeiten, keine Zeitzonen-Namen, keine Epoch-Zahlen.
- Umrechnung in die Anzeigezeitzone passiert **im Frontend**, nirgends sonst.
- Zeitfenster werden als `von`/`bis` übergeben, beide ISO 8601 UTC. Fehlen sie, gilt der Standard
  aus L1.
- Dauern werden als **ganze Sekunden** übertragen (`dauerSekunden: 42`), nicht als formatierter
  Text. `MessageTimeout` bleibt in Minuten, weil die Quelle das so führt — das Feld heißt dann
  auch `timeoutMinuten`.

### 5.4 Paginierung

Cursor-basiert, niemals `OFFSET` (L3). Einheitliche Hülle:

```json
{
  "items": [ … ],
  "nextCursor": "eyJ0IjoiMjAyNi0wNy0yNFQxODozMDowMFoiLCJpZCI6IjhmM2EtLi4uIn0",
  "hasMore": true
}
```

- `nextCursor` ist **undurchsichtig**: Base64URL des Sortierschlüssels
  `(MessageLastUpdate, MessageID)`. Der Aufrufer darf ihn nicht auseinandernehmen, und das
  Backend verlässt sich nicht darauf, dass er unverändert zurückkommt — er wird validiert.
- `nextCursor` ist `null`, wenn `hasMore` `false` ist.
- Die Seitengröße kommt als `limit`, hat einen Standard und ein hartes Maximum.
- Es gibt **kein** `total`. Eine Gesamtzahl über `Message` wäre genau die Live-Aggregation, die
  L2 verbietet.

### 5.5 Fehlerformat

Einheitlich **RFC 9457 Problem Details**, `Content-Type: application/problem+json`. Spring
Framework 7 bringt `ProblemDetail` mit; es wird nichts Eigenes gebaut.

```json
{
  "type": "https://overlord.kraftwerkone.de/probleme/zeitfenster-zu-gross",
  "title": "Zeitfenster zu groß",
  "status": 400,
  "detail": "Das Zeitfenster darf höchstens ein Jahr umfassen.",
  "instance": "/api/nachrichten",
  "traceId": "b7c1f2e4-…"
}
```

- `title` und `detail` sind **deutsch und für den Nutzer lesbar**. Sie dürfen unverändert angezeigt
  werden.
- `traceId` ist eine Korrelations-ID (ab Schritt 2 so benannt). Sie steht auch im Serverprotokoll.
  Damit kann ein Nutzer eine Störung melden, ohne dass ihm interne Details gezeigt werden.
- Feldbezogene Prüffehler kommen zusätzlich als `errors: [{ "feld": …, "meldung": … }]`.

Statuscodes:

| Code | Wann |
|---|---|
| `400` | Anfrage fachlich unbrauchbar (Zeitfenster zu groß, Suchbegriff zu kurz, Cursor ungültig) |
| `401` | Nicht angemeldet |
| `403` | Angemeldet, aber Rolle reicht nicht |
| `404` | Nicht vorhanden **oder nicht für diesen Mandanten sichtbar** — beides ist ununterscheidbar |
| `409` | Konflikt bei Schreibvorgängen im eigenen Schema |
| `500` | Technischer Fehler |

**`404` statt `403` bei fremden Daten ist Absicht.** Ein `403` verriete, dass die Nachricht
existiert. Für den Nutzer hat die Zeile nie existiert (M3).

---

## 6. Fehlerbehandlung

### Fachlich gegen technisch

**Fachliche Fehler** sind vorhergesehen: zu großes Zeitfenster, zu kurzer Suchbegriff, gesperrtes
Konto, Nachricht ohne Nutzdaten. Sie bekommen einen eigenen Ausnahmetyp in `common`, einen
sprechenden deutschen Text und einen `4xx`-Status. Sie werden auf `INFO`/`WARN` protokolliert,
ohne Stacktrace.

**Technische Fehler** sind nicht vorhergesehen: Datenbank weg, Filestore antwortet nicht,
Nullzeiger. Sie werden auf `ERROR` **mit** Stacktrace protokolliert und ergeben `500`.

### Was der Nutzer sieht

Bei fachlichen Fehlern: den fachlichen Text, mit Handlungshinweis („Wähle ein Zeitfenster von
höchstens einem Jahr").

Bei technischen Fehlern: einen neutralen Text plus die `fehlerId`. **Niemals** interne Details:
keine Stacktraces, keine SQL-Statements, keine Tabellen- oder Spaltennamen, keine
Verbindungszeichenketten, keine Hostnamen, keine Klassennamen. Auch nicht im `detail`-Feld, auch
nicht im `dev`-Profil — sonst wird es irgendwann in Produktion vergessen.

Externe Nutzer bedeuten: Jede durchgereichte technische Meldung ist eine Auskunft über die
Infrastruktur an jemanden außerhalb des Hauses.

### Was protokolliert wird

Ein globaler `@RestControllerAdvice` in `common` ist die **einzige** Stelle, die Ausnahmen in
Antworten übersetzt. Kein Controller fängt selbst.

Jeder Protokolleintrag trägt die `traceId`, den Endpunkt, den Benutzernamen und den Mandanten.
**Nie protokolliert werden**: Passwörter, Session-IDs, Cookie-Werte, Inhalte von EDI-Nutzdaten
und `MessagePropertyValue` — dort stehen Preise, Mengen und Kundendaten.

Sicherheitsrelevante Ereignisse (Anmeldung, Fehlversuch, Sperre, Passwortänderung,
Katalogänderung, Rohdaten-Download) gehen zusätzlich ins `audit_log` — nicht nur ins
Anwendungsprotokoll.

---

## 7. Teststrategie

### Die vier Ebenen

| Ebene | Werkzeug | Gegenstand |
|---|---|---|
| **Unit** | JUnit Jupiter, AssertJ, Mockito | Fachlogik ohne Datenbank: Statusabbildung, Timeout-Berechnung, Cursor-Kodierung, Übersetzung der Prozessschritte |
| **Integration** | Spring Boot Test gegen die **Testkopie** | Repositories und Endpunkte mit echtem SQL |
| **Isolation** | Spring Boot Test gegen die Testkopie | Mandantentrennung, **pflichtig je Endpunkt** |
| **Architektur** | ArchUnit | Paketstruktur, Zeitquelle (`Clock` statt `now()`), kein JPA, ab Schritt 2 `jooq.glassfish` |

### Der Mandanten-Isolationstest ist Pflicht

**Pro Endpunkt genau ein Test**, der mit Mandant A abfragt und nachweist, dass Daten von Mandant B
unerreichbar sind. Das Muster:

1. Ein bekannter Datensatz von Mandant B wird ermittelt.
2. Der Endpunkt wird als Nutzer von Mandant A aufgerufen — mit der `MessageID` aus Schritt 1.
3. Erwartet wird `404` beziehungsweise eine leere Liste. **Nicht** `403`.
4. Zusätzlich: Kein Ergebnis der Antwort gehört zu Mandant B.

Und die Gegenprobe, die den eigentlichen Kern ausmacht: Dieselbe Anfrage mit einer **erfundenen** ID
muss eine **ununterscheidbare** Antwort liefern. Ein `404` allein genügt nicht.

Benennung **`<Thema>IsolationDbIT`** (angeglichen in Schritt 3): Ein Isolationstest braucht immer eine
Datenbank, trägt also `@Tag("db")` und gehört über Failsafe in dieselbe Gruppe wie die übrigen
`*IT`. Die Vorlage ist `MandantenIsolationDbIT`. **Ein neuer Endpunkt ohne diesen Test wird nicht
gemergt.** Das ist kein Richtwert und keine Empfehlung.

### Integrationstests laufen gegen die Testkopie

Vollkopie der Produktion, Datenstand 08.07.2026 und damit hinter der realen Uhrzeit. Deshalb: Kein
Test darf `now()` als Referenzzeitpunkt annehmen — er nutzt die Anwendungsuhr (`Clock` aus `common`,
Z1). Tests schreiben **niemals** nach
`GlassfishDB` (S1); sie lesen vorhandene Daten und legen benötigte eigene Datensätze
ausschließlich in `overlord_monitor` an.

Zugangsdaten der Testkopie kommen aus Umgebungsvariablen.

**Datenbankgebundene Tests tragen ab Schritt 2 `@Tag("db")`** und laufen lokal standardmäßig mit
(Namensschema `<Thema>IT` über Failsafe in der Phase `verify`; der Kontext-Rauchtest über Surefire).
Die CI — und jeder ohne DB-Zugang — schließt die Gruppe über `-DexcludedGroups=db` aus; die Option
wirkt in Surefire wie in Failsafe. Der Unit- und der Architekturteil laufen immer, auch ohne
Datenbank. Zusammen mit den eingecheckten jOOQ-Quellen und `-Djooq.codegen.skip=true` bleibt der
Build für jemanden ohne DB-Zugang vollständig brauchbar.

### Messung

Zu L7: Jede neue Abfrage wird vor dem Merge gegen die Testkopie gemessen — `EXPLAIN` plus
Laufzeit. Das Ergebnis gehört in die Feature-Dokumentation unter `docs/`, nicht in eine
Commit-Nachricht.

---

## 8. Frontend-Konventionen

### Server- und Client-Komponenten

**Standard ist die Server-Komponente.** `"use client"` steht so weit unten im Baum wie möglich —
an der Komponente, die wirklich Zustand, Effekte oder Browser-APIs braucht, nicht an der Seite.

- **Server**: Layouts, Seitengerüste, alles Statische, alles Rollenabhängige in der Navigation
- **Client**: Tabellen mit Interaktion, Filterleisten, Formulare, Diagramme, alles mit
  TanStack Query

Die Seite lädt serverseitig genug für einen sinnvollen ersten Anblick; das Nachladen beim Filtern
übernimmt der Client.

### Serverzustand: TanStack Query

**Serverdaten liegen ausschließlich in TanStack Query.** Kein Redux, kein Zustand, kein
globaler Store, kein `useEffect`-mit-`fetch`. Query-Keys sind strukturiert und beginnen mit dem
Feature: `['nachrichten', 'liste', filter]`.

Der HTTP-Client liegt in `lib/`, schickt `credentials: 'include'` und übersetzt eine
`problem+json`-Antwort in einen typisierten Fehler. Ein `401` führt zur Weiterleitung auf die
Anmeldung.

### Filterzustand: nuqs

**Filterzustand liegt in der URL**, damit Ansichten teilbar sind — Zeitfenster, Statusfilter,
Suchbegriff, Sortierung, gewählte Nachricht. Verwaltet mit `nuqs`, nicht mit `useState`.

Faustregel: Was der Nutzer einstellt und was ein Kollege sehen soll, wenn er den Link bekommt,
gehört in die URL. Was nur die Darstellung betrifft (aufgeklappte Abschnitte), gehört nicht dahin.

### shadcn/ui

`components/ui` ist Generatorbereich. Komponenten werden mit `pnpm dlx shadcn@latest add <name>`
geholt und dort nicht von Hand umgebaut. Reicht eine Komponente nicht, wird sie **umschlossen**,
nicht verändert.

**Unterbau ist Radix** (`components.json`: `"style": "radix-nova"`, `"baseColor": "neutral"`).
shadcn/ui kann auch auf Base UI oder React Aria aufsetzen; die Entscheidung fiel auf Radix aus
demselben Grund, aus dem die Projektbeschreibung Spring Boot statt Quarkus wählt — dafür existiert
mit Abstand das meiste Material, der generierte Code ist verlässlicher. **Diese Wahl wird nicht
pro Komponente neu getroffen.** `components.json` wird nicht von Hand geändert.

Tabellen: TanStack Table mit serverseitiger Paginierung und Sortierung. Diagramme: Recharts.
Beide werden installiert, wenn sie gebraucht werden — nicht auf Vorrat.

### Abhängigkeiten

pnpm führt Installationsskripte nur aus, wenn sie in `pnpm-workspace.yaml` unter `allowBuilds`
freigegeben sind. Eine neue Freigabe ist eine bewusste Entscheidung und wird im Pull Request
begründet: Ein Installationsskript läuft mit den Rechten dessen, der baut.

### Keine eigene Datenhaltung

Das Frontend hat keine Datenbank, keinen ORM, keine Serverfunktion, die selbst SQL spricht. Next.js
ist BFF für Cookies und trifft **keine** Berechtigungsentscheidungen. Rollenabhängige Navigation
ist Bequemlichkeit; verbindlich prüft immer das Backend.

Konkret: Eine Route für Administratoren wird im Menü ausgeblendet, **und** der Endpunkt dahinter
prüft die Rolle. Das Ausblenden allein ist keine Absicherung.

### Zustände

Jede datengetriebene Ansicht behandelt vier Zustände sichtbar: **Laden**, **Leer**, **Fehler**,
**Daten**. „Leer" heißt nicht „Fehler" und sieht auch nicht so aus. Bei „Leer" wird gesagt, woran
es liegen kann — meist am Zeitfenster.

---

## 9. Versionsstand

Geprüft am 24.07.2026:

| Baustein | Version | Anmerkung |
|---|---|---|
| Java | 21 (LTS) | |
| Spring Boot | 4.1.x | Bringt **Spring Framework 7, Spring Security 7, Jackson 3** |
| Maven | über Wrapper (`./mvnw`) | keine lokale Installation nötig |
| Next.js | 16.x | Active LTS bis Oktober 2027 |
| React | 19.x | |
| pnpm | 11.x | Paketverwalter im Frontend, **nicht** npm oder yarn |

Spring Boot 3.x hat mit 3.5.16 seinen letzten Patch erhalten und bekommt keine Sicherheitsfixes
mehr. Next.js 15 läuft im Oktober 2026 aus.

### Was das für generierten Code bedeutet

**Spring Boot 4 ist keine Punktversion über Spring Boot 3.** Muster der Vorgängergeneration sind
im Trainingsmaterial überrepräsentiert und werden hier **nicht übernommen**. Insbesondere:

- **Jackson 3** statt Jackson 2 — Paket `tools.jackson…` statt `com.fasterxml.jackson…`.
  Ein Import auf `com.fasterxml.jackson` ist fast immer ein Fehlgriff.
- **Spring Security 7** — die Konfiguration ist die Lambda-DSL; `WebSecurityConfigurerAdapter`
  und die `and()`-Verkettung gibt es seit Langem nicht mehr.
- **Jakarta EE 11** — `jakarta.*`, niemals `javax.*`.

Dasselbe gilt im Frontend. Next.js 16 bringt gegenüber dem verbreiteten Material ebenfalls Brüche
bei APIs, Konventionen und Dateistruktur. Die maßgebliche Fassung liegt **im Projekt selbst**:

```
frontend/node_modules/next/dist/docs/
```

Dort wird nachgeschlagen, bevor Next-spezifischer Code entsteht — nicht auf einer beliebigen Seite
im Netz, die eine ältere Hauptversion beschreibt.

**Im Zweifel die aktuelle Dokumentation prüfen, statt aus dem Gedächtnis zu schreiben.** Wenn eine
Signatur, ein Paketname oder eine Konfigurationsoption nicht sicher ist: nachschlagen. Ein
plausibel aussehender Aufruf aus der Vorgängergeneration kostet mehr Zeit, als das Nachschlagen
gedauert hätte.

### Warum ArchUnit einen Teil davon prüft

`PaketstrukturTest` verbietet Importe aus `com.fasterxml.jackson`, `javax.*` und
`jakarta.persistence`. Diese drei sind die häufigsten Rückfälle in die Vorgängergeneration und
lassen sich maschinell erkennen — im Gegensatz zu den meisten anderen. Der Test ersetzt das
Nachschlagen nicht, er fängt nur das Offensichtliche ab.

---

## 10. Commit-Konventionen

**Conventional Commits**, Betreff auf Deutsch:

```
<typ>(<bereich>): <was, im Imperativ, klein, ohne Punkt>

<warum — nicht was. Das „was" steht im Diff.>
```

Typen: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.

Bereiche: `backend`, `frontend`, `docs`, `ci` oder das Fachpaket (`message`, `bam`, `catalog`, …).

**Beispiel.** Verbindlich ist die Form, nicht der Inhalt — die spitzen Klammern stehen für
Werte, die aus der tatsächlichen Messung nach L7 kommen. Kein erfundener Zugriffspfad, keine
erfundene Laufzeit:

```
feat(message): nachrichtenliste mit pflicht-zeitfenster und cursor-paginierung

Zeitfenster ist Pflicht (L1), Standard 24 h, Maximum ein Jahr. Cursor über
(MessageLastUpdate, MessageID) statt OFFSET (L3), weil OFFSET bei 36 Mio. Zeilen
linear teurer wird.

EXPLAIN gegen die Testkopie: <gewählter Index>, <Laufzeit> bei 24 h.
```

Regeln:

- **Ein Commit, ein Anliegen.** Formatierung getrennt von Fachlichkeit.
- Der Hauptzweig bleibt jederzeit baubar. Gearbeitet wird auf Zweigen
  `<typ>/<kurzbeschreibung>`.
- Wird eine der Regeln aus Abschnitt 4 berührt, steht die Regelnummer im Text.
- **Keine** Zugangsdaten, Hostnamen oder Produktionsdaten in Commit-Nachrichten.

---

## 11. Definition of Done

Ein Schritt ist fertig, wenn **alle** Punkte zutreffen. Nicht „im Wesentlichen", sondern alle.

**Bauen und prüfen**
- [ ] `cd backend && ./mvnw verify` läuft fehlerfrei — einschließlich Spotless und ArchUnit
- [ ] `cd frontend && pnpm build` läuft fehlerfrei — einschließlich Lint und Typprüfung
- [ ] Die CI ist grün

**Fachlich**
- [ ] Das Abnahmekriterium des Schritts aus `docs/IMPLEMENTIERUNGSPLAN_MVP.md` ist erfüllt und
      wurde tatsächlich ausgeführt — nicht nur plausibel
- [ ] Die Abgrenzung des Schritts wurde eingehalten, es wurde nichts vorgezogen

**Sicherheit**
- [ ] **Jeder neue Endpunkt hat seinen Mandanten-Isolationstest** (M4)
- [ ] Kein Endpunkt nimmt eine Mandanten-ID entgegen (M1)
- [ ] Jede neue Repository-Methode hat den Mandanten als ersten Pflichtparameter (M2)
- [ ] Keine Zugangsdaten, Hostnamen oder Produktionsdaten im Diff (G1)
- [ ] Keine internen Details in Fehlerantworten

**Leistung**
- [ ] Jeder neue Listen-Endpunkt hat ein Pflicht-Zeitfenster (L1)
- [ ] Keine `OFFSET`-Paginierung (L3)
- [ ] Jede neue Abfrage ist gegen die Testkopie gemessen, `EXPLAIN` und Laufzeit sind
      dokumentiert (L7)

**Zeit**
- [ ] Kein direkter Aufruf von `now()` (Z1)

**Dokumentation**
- [ ] Neues Feature → neue Datei in `docs/`; geändertes Feature → aktualisierte Datei
- [ ] Neue Datei ist in `docs/README.md` verzeichnet
- [ ] Berührt die Änderung eine Regel aus Abschnitt 4, ist das hier vermerkt

**Ohne Dokumentation ist ein Schritt nicht fertig, auch wenn der Code läuft.**
