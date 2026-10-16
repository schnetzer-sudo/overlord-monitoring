# Betrieb

*Angelegt am 16.10.2026 mit Version 1.0.0.* Was das Repository über den Betrieb belegt: Bausteine,
Profile, Umgebungsvariablen, Flyway, Rollup-Job, Ablagenprüfung, Release und Rollback. **Was nicht im
Repository steht, steht hier auch nicht als Tatsache** — es ist als offene Frage am Ende geführt und im
Register [`offene-punkte.md`](offene-punkte.md) eingetragen. Lokale Unterschiede stehen in
[`START-LOKAL.md`](START-LOKAL.md), die Architektur in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6.

---

## 1. Bausteine

| Baustein | Was das Repository belegt |
|---|---|
| **Backend** | Ein ausführbares JAR mit eingebettetem Server (`spring-boot-maven-plugin` ohne weitere Konfiguration, Paketierung `jar`). Gebaut mit `./mvnw verify`; ohne `finalName` heißt es `target/overlord-monitor-<version>.jar`. Port `${SERVER_PORT:8080}`, `server.shutdown: graceful`, virtuelle Threads an. Zwei Verbindungspools: `glassfish-read` (höchstens 5, `max_statement_time = 10`) und `monitor-write` (höchstens 5, `max_statement_time = 30`) — `config/DataSourceConfig` |
| **Frontend** | Next.js 16: `pnpm build` (läuft erst nach `pnpm check`), dann `next start` (`frontend/package.json`). Kein `output: "standalone"`; der Betrieb braucht `node_modules`, `.next` und `next start`. Port nicht konfiguriert, also der Next-Standard 3000. `next.config.ts` leitet `/api/*` an `OVERLORD_BACKEND_URL` weiter |
| **Reverse Proxy** | führt Frontend und Backend unter einer Domain zusammen (Kommentar in `next.config.ts`, [`frontend-grundlagen.md`](frontend-grundlagen.md)). Seine Konfiguration liegt **nicht** im Repository |
| **Rollup-Job** | dieselbe Anwendung mit Profil `rollup`, im Betrieb als eigener Prozess `prod,rollup` ([`rollup.md`](rollup.md) §8) |

Gesundheit: `/actuator/health` ist der einzige freigegebene Actuator-Endpunkt, ohne Anmeldung
erreichbar, außerhalb von `dev` ohne Details. Fehlerantworten tragen in keinem Profil interne Details.
Protokolliert wird auf die Konsole (Boot-Standard, keine Logback-Datei); die Stufen setzen
`LOG_LEVEL_ROOT` und `LOG_LEVEL_APP`, was protokolliert wird, regelt
[`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §6.

---

## 2. Profile

| Profil | Zweck |
|---|---|
| `dev` | **Vorgabe, wenn nichts gesetzt ist.** Lokale Entwicklung gegen die Testkopie: Anwendungsuhr mit Versatz auf den Datenstand, Flyway mit `out-of-order`, Ablagenprüfung und Rollup-Auslösung aus, Sitzungs-Cookie ohne `Secure`, Health mit Details, Anwendungs-Log `DEBUG` |
| `prod` | **Betrieb**, gesetzt über `SPRING_PROFILES_ACTIVE=prod`. Systemuhr, `server.forward-headers-strategy: framework`, Log `WARN` bzw. `INFO` für die Anwendung |
| `rollup` | zusätzlich zu `prod`: der zeitgesteuerte Rollup-Job (`RollupPlaner`); ohne Profil gibt es ihn nicht |
| `bootstrap` | legt einmalig das erste `ADMIN`-Konto aus `OVERLORD_BOOTSTRAP_ADMIN_*` an, nur wenn noch keines existiert, mit Zwang zur Passwortänderung; danach ohne das Profil neu starten ([`authentifizierung.md`](authentifizierung.md) §7) |

Profilabhängig sind gehalten: `Secure` am Cookie (`CookieAttributeTest`) und `out-of-order` nur in
`dev` (`FlywayOutOfOrderTest`).

---

## 3. Umgebungsvariablen

Nur Namen; Werte stehen nie im Repository (Regel G1). Vorlagen: `backend/.env.example`,
`frontend/.env.example`. Die Dateien werden nicht automatisch eingelesen.

| Name | Teil | Zweck | Pflicht |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | Profile (`prod`, `prod,rollup`, `prod,bootstrap`) | nein, Vorgabe `dev` |
| `SERVER_PORT` | Backend | HTTP-Port | nein, Vorgabe 8080 |
| `LOG_LEVEL_ROOT`, `LOG_LEVEL_APP` | Backend | Protokollstufen | nein |
| `OVERLORD_DB_HOST`, `OVERLORD_DB_PORT` | Backend | MariaDB | ja |
| `OVERLORD_DB_GLASSFISH_SCHEMA` | Backend | Quellschema, nur lesen | ja |
| `OVERLORD_DB_MONITOR_SCHEMA` | Backend | eigenes Schema, auch Ziel von Flyway | ja |
| `OVERLORD_DB_READ_USER`, `OVERLORD_DB_READ_PASSWORD` | Backend | Lese-Benutzer | ja |
| `OVERLORD_DB_WRITE_USER`, `OVERLORD_DB_WRITE_PASSWORD` | Backend | Schreib-Benutzer (nur `overlord_monitor`) | ja |
| `OVERLORD_BOOTSTRAP_ADMIN_USER`, `OVERLORD_BOOTSTRAP_ADMIN_PASSWORD` | Backend | nur mit Profil `bootstrap` (seit 16.10.2026 in `.env.example`) | mit `bootstrap` |
| `OVERLORD_ASSISTENT_AKTIV` | Backend | Schalter des Assistenten ([`assistent.md`](assistent.md) §7); erst nach der Prüfung durch den Datenschutzbeauftragten (Punkt 1009) | nein, Vorgabe `false` |
| `OVERLORD_ASSISTENT_API_KEY` | Backend | Schlüssel des Modellanbieters; ohne ihn ist der Assistent aus | nur mit eingeschaltetem Assistenten |
| `OVERLORD_ASSISTENT_MODELL` | Backend | Modellkennung beim Anbieter | nein, Vorgabe `claude-sonnet-5-5` |
| `OVERLORD_BACKEND_URL` | Frontend | Ziel der `/api`-Weiterleitung, serverseitig | nein, Vorgabe `http://localhost:8080` |

Der Codegen und das Flyway-Maven-Plugin lesen dieselben `OVERLORD_DB_*`.

**Der Assistent** schickt bei eingeschaltetem Schalter HTTPS-Anfragen an `api.anthropic.com`. Ob der
Produktionsserver dorthin darf, ist ungeprüft (Punkt 1010). Alle übrigen Grenzen stehen unter
`overlord.assistent` in `application.yml` ([`assistent.md`](assistent.md) §7).

---

## 4. Flyway beim Start

Jeder Start migriert `overlord_monitor` über den Schreib-Pool (`@FlywayDataSource`), und zwar nur
dieses Schema: `create-schemas: false`, `baseline-on-migrate: false`. Die Migrationen liegen in
`backend/src/main/resources/db/migration` (`V1` bis `V13`). In der Produktion gilt die strenge
Reihenfolge; `out-of-order` ist nur in `dev` an, weil `main` und `assistent` sich die Testkopie
teilen ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). Fremde, noch unbekannte
Migrationen ignoriert jeder Start über die Vorgabe `ignore-migration-patterns: *:future`. Auch der
Prozess `prod,rollup` führt Flyway aus. Das Maven-Plugin (`./mvnw flyway:migrate`) ist an keine
Bauphase gebunden.

**Flyway kennt keine Rückwärtsmigration**, und im Repository gibt es keine (`U__`-Dateien fehlen).

---

## 5. Rollup-Job

Schlüssel unter `overlord.rollup` (`application.yml`, [`rollup.md`](rollup.md)):

- **Zwei Riegel:** Profil `rollup` **und** `aktiv: true`; in `dev` ist `aktiv` ausdrücklich `false`.
- **Takt:** Delta-Lauf `delta-plan: "0 5 * * * *"` (stündlich, Minute 5), Volllauf
  `voll-plan: "0 0 3 * * *"` (03:00), Pause zwischen den Monatsscheiben `scheiben-pause: 1s` —
  Uhrzeit und Pause sind gesetzt, nicht gemessen (Punkt 50).
- **Von Hand:** Startparameter `--overlord.rollup.beim-start=DELTA|VOLL`, unabhängig von Profil und
  Schalter; ein Fehlschlag verhindert den Start nicht. Kein HTTP-Endpunkt.
- Es läuft immer nur ein Lauf (Sperre über `rollup_lauf`).
- **Der erste Start auf leeren Tabellen** holt im ersten Delta-Lauf den ganzen Bestand
  ([`rollup.md`](rollup.md)); auf der Testkopie rund 46 s.

---

## 6. Ablagenprüfung

Schlüssel unter `overlord.ablagenpruefung`: `aktiv: true`, `takt: 60s` (fester Abstand, gesetzt,
nicht gemessen); in `dev` aus. Je Takt wird jede Ablage aus `Service.ServiceDefaultFileStore` per
`RETRIEVE` auf die Null-UUID angefragt, das Ergebnis liegt im Speicher und speist die Ablagenkachel
der Übersicht ([`dienste.md`](dienste.md) §7). **Im Rollup-Prozess gehört sie aus**
(`--overlord.ablagenpruefung.aktiv=false`, [`dienste.md`](dienste.md) §14) — das Repository erzwingt es
nicht. Lokal einschalten: [`START-LOKAL.md`](START-LOKAL.md) §4.

---

## 7. Release-Ablauf

Belegt in [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12:

1. Auf `main` liegt der Stand, die CI ist grün, `CHANGELOG.md` hat den Eintrag mit Version und Datum,
   das Register ist nachgeführt.
2. **Lukas setzt den Tag** `v<version>` auf `main`.
3. Nach einem Fix-Release wird `main` in `assistent` gemergt.

**Bau und Auslieferung nach dem Tag sind im Repository nicht beschrieben.** Die CI
([`.github/workflows/ci.yml`](../.github/workflows/ci.yml)) baut und prüft Pushes auf `main` und
`assistent` und jeden Pull Request, ohne Datenbank (`-Djooq.codegen.skip=true -DexcludedGroups=db`);
sie reagiert auf keinen Tag, lädt kein Artefakt hoch und liefert nichts aus. Siehe die offenen Fragen.

---

## 8. Rollback

- **Code:** den vorigen Tag bauen und ausliefern.
- **Schema:** Flyway kennt keine Rückwärtsmigration. Ein Rollback heißt deshalb **Wiederherstellung
  aus einer Sicherung** oder **Vorwärtskorrektur** — eine neue Migration, die die vorige aufhebt.
  Ein älteres JAR startet gegen ein neueres Schema nicht ohne Weiteres: Flyway findet angewandte
  Migrationen, die es nicht kennt, und ignoriert sie nur über die Vorgabe `*:future`; ob die Anwendung
  danach mit dem Schema arbeitet, hängt an der Migration.
- **Was vor jedem Schemaeingriff gesichert wird:** die von Hand gepflegten Tabellen `process_catalog`
  und `bam_sollaenge` ([`bam-sollaengen.md`](bam-sollaengen.md) §7.2). `message_rollup` und die
  abgeleiteten Ebenen sind aus `GlassfishDB` neu berechenbar (Volllauf). Wie und wohin gesichert wird,
  steht nicht im Repository.

---

## 9. Offene Fragen an Lukas

Nicht im Repository belegt. Je Frage ein Eintrag im Register.

| Nr. | Frage |
|---|---|
| 255 | **Betriebsform:** systemd-Dienst oder Container? Wo liegen JAR und Frontend auf dem Server, unter welchem Benutzer, wie heißen die Units? |
| 256 | **Reverse Proxy:** Konfiguration, Domain und Zertifikat liegen nicht im Repository. Routet er `/api` an Next.js (dann über `OVERLORD_BACKEND_URL`) oder direkt an das Backend? |
| 257 | **Client-IP hinter dem Proxy:** `prod` setzt `forward-headers-strategy: framework`, Anmeldung und `audit_log` lesen `getRemoteAddr()`. Sieht die IP-Begrenzung in der Produktion die echte Client-IP (Kette Proxy → Next.js → Backend)? Ungeprüft |
| 258 | **Rollup-Prozess:** Läuft er als zweiter Prozess `prod,rollup`, auf welchem `SERVER_PORT` (beide starten den Webserver), mit `--overlord.ablagenpruefung.aktiv=false`? |
| 259 | **Sicherung:** Wie und wie oft wird `overlord_monitor` gesichert, insbesondere `process_catalog` und `bam_sollaenge` vor jedem Schemaeingriff, und wer stellt wieder her? Davon hängt der Rollback in §8 ab |
| 260 | **Release-Bau und Auslieferung:** Wer baut nach dem Tag JAR und Frontend, wo, und wie kommen sie auf den Server? |
| 261 | **Artefaktname:** Die Version steht seit dem 16.10.2026 auf 1.0.0 (`backend/pom.xml`, `frontend/package.json`). Ohne `finalName` heißt das JAR `overlord-monitor-1.0.0.jar`, die Doku schreibt `overlord-monitor.jar` (`rollup.md`, `dienste.md`). Wird `finalName` gesetzt, oder heißt das JAR im Betrieb anders? |
| 262 | **Bootstrap in der Produktion:** Ist das erste Konto über `prod,bootstrap` angelegt worden? (Die beiden `OVERLORD_BOOTSTRAP_ADMIN_*` stehen seit dem 16.10.2026 in `backend/.env.example`.) |
| 263 | **Protokolle:** Wohin geht die Konsolenausgabe (journald, Container, Datei), und wie lange wird sie aufbewahrt? |
