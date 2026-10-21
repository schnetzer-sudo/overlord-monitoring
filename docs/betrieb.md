# Betrieb

*Angelegt am 16.10.2026 mit Version 1.0.0.* Was das Repository über den Betrieb belegt: Bausteine,
Profile, Umgebungsvariablen, Flyway, Rollup-Job, Ablagenprüfung, Release und Rollback. **Was nicht im
Repository steht, steht hier auch nicht als Tatsache** — es ist als offene Frage am Ende geführt und im
Register [`offene-punkte.md`](offene-punkte.md) eingetragen. Lokale Unterschiede stehen in
[`START-LOKAL.md`](START-LOKAL.md), die Architektur in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6.

> **Ergänzt am 21.10.2026 mit 1.1.0.** Die Linie `assistent` ist nach `main` zusammengeführt. Neu für
> den Betrieb: drei **optionale** Variablen des Assistenten (§3), die Migrationen `V100` bis `V103`
> (§4), die Zeitgrenze der Weiterleitung, die ausgehende Verbindung zum Modellanbieter (§5a) und was
> ein älteres JAR gegen das neue Schema bedeutet (§8). Das Deployment bleibt, wie es ist.

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
| `OVERLORD_ASSISTENT_AKTIV` | Backend | Schalter des Assistenten ([`assistent.md`](assistent.md) §7). Die Prüfung durch den Datenschutzbeauftragten ist erledigt (Punkt 1009, E‑1131) | nein, Vorgabe `false` |
| `OVERLORD_ASSISTENT_API_KEY` | Backend | Schlüssel des Modellanbieters; ohne ihn ist der Assistent für alle Mandanten aus | nein; nur mit eingeschaltetem Assistenten nötig |
| `OVERLORD_ASSISTENT_MODELL` | Backend | Modellkennung beim Anbieter | nein, Vorgabe `claude-sonnet-5-5` |
| `OVERLORD_BACKEND_URL` | Frontend | Ziel der `/api`-Weiterleitung, serverseitig | nein, Vorgabe `http://localhost:8080` |

Der Codegen und das Flyway-Maven-Plugin lesen dieselben `OVERLORD_DB_*`.

**Mit 1.1.0 kommt keine Pflicht-Variable dazu** (21.10.2026). Die drei `OVERLORD_ASSISTENT_*` sind
optional; fehlen sie, startet das Backend wie mit 1.0.x, und der Assistent ist für alle Mandanten aus.
Beim Start steht dann eine Zeile im Protokoll (Stufe `INFO`, Logger `AssistentConfig`):
`Assistent fuer alle Mandanten aus: kein Schluessel (…)` bzw. `…: Schalter aus (…)`, mit
`schluessel=fehlt` oder `schluessel=gesetzt`, **nie mit dem Schlüssel selbst**. Eingeschaltet steht
dort ein `WARN` mit dem Ziel der Aufrufe. Auch der Prozess `prod,rollup` schreibt diese Zeile; der
Assistent tut dort nichts (§5).

**Einschalten in der Produktion, mit 1.1.0 für `SYSTEM`:** `OVERLORD_ASSISTENT_AKTIV=true` und
`OVERLORD_ASSISTENT_API_KEY` setzen, Backend neu starten, dann
`scripts/assistent-freischaltung-system.sql` ausführen ([`tasks.md`](tasks.md) §3.9). Verfügbar ist
der Assistent nur für Mandanten mit Freischaltung; ohne sie fehlt das Symbol, und Gespräche und
Anlegen antworten `404`. Ein weiterer Mandant ist ein `INSERT`, kein Deployment (E‑1126). Alle
übrigen Grenzen stehen unter `overlord.assistent` in `application.yml`
([`assistent.md`](assistent.md) §7). Die Frontend-Vorlage bekommt keine neue Variable.

---

## 4. Flyway beim Start

Jeder Start migriert `overlord_monitor` über den Schreib-Pool (`@FlywayDataSource`), und zwar nur
dieses Schema: `create-schemas: false`, `baseline-on-migrate: false`. Die Migrationen liegen in
`backend/src/main/resources/db/migration`: **`V1` bis `V13` und seit 1.1.0 `V100` bis `V103`**
(Aufträge und Assistent, [`tasks.md`](tasks.md) §5.1, §3.9). In der Produktion gilt die strenge
Reihenfolge; `out-of-order` ist nur in `dev` an (`FlywayOutOfOrderTest`). Fremde, noch unbekannte
Migrationen ignoriert jeder Start über die Vorgabe `ignore-migration-patterns: *:future`. Auch der
Prozess `prod,rollup` führt Flyway aus. Das Maven-Plugin (`./mvnw flyway:migrate`) ist an keine
Bauphase gebunden.

**Der erste Start von 1.1.0 in der Produktion** findet `V1` bis `V13` angewandt und wendet `V100`,
`V101`, `V102` und `V103` in dieser Reihenfolge an: Alle vier liegen über `V13`, die strenge
Reihenfolge ist eingehalten. `V100` legt die drei Tabellen `task_type`, `task_type_mandant` und
`task` an und den Typ `WERK_ABLADESTELLE` (aktiv) samt Freischaltung für `NEXANS` und `IBIS`,
`V101` und `V102` ändern seine Definition, `V103` nimmt die beiden Freischaltungen zurück. Danach ist
der Typ für niemanden freigeschaltet, bis das Skript für `SYSTEM` läuft.

**Danach gibt es nur noch eine Linie** (E‑1127, [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md)
§12): Sobald `V100` und folgende in der Produktion angewandt sind, kann dort keine Migration mit
kleinerer Nummer mehr folgen — ohne `out-of-order` wäre sie ein Validierungsfehler beim Start. Die
nächste Migration ist `V104`. `out-of-order` nur in `dev` (gehalten von `FlywayOutOfOrderTest`) und
`ignore-migration-patterns: *:future` bleiben unverändert; `out-of-order` wird nach dem
Zusammenführen nicht mehr gebraucht, schadet aber nicht, weil keine Migration unter `V104` mehr
entsteht.

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
- **Aus der Linie `assistent` läuft im Prozess `prod,rollup` nichts mit** (geprüft am 21.10.2026):
  Sie hat keine zeitgesteuerte Bean, kein `@Scheduled`, keinen eigenen Takt. Ihre Beans entstehen,
  weil es dieselbe Anwendung ist, und tun nichts, solange niemand den Webserver dieses Prozesses
  anfragt (Punkt 258). Flyway wendet `V100` bis `V103` auch hier an (§4).

---

## 5a. Assistent: Zeitgrenze, Streaming, ausgehende Verbindung *(1.1.0, 21.10.2026)*

**Die Antwort kommt am Stück, ohne Streaming** (E‑1043, E‑1075): Das Backend schickt erst, wenn die
Antwort des Modells fertig ist, und bis dahin fließt kein Byte. Eine Nachricht kann bis zu vier
Modellaufrufe brauchen — drei Werkzeugrunden und die Antwort —, jeder bis zu 30 s (`zeitgrenze`) und
bei `429` oder `529` ein zweiter Versuch nach 1 s. **Im ungünstigsten Fall sind das
4 × (30 + 1 + 30) s = 244 s.** Gemessen sind im Mittel 3,9 bis 8,1 s je Testfall (M1001, M1003,
[`assistent.md`](assistent.md) §8).

**Die Zeitgrenze der `/api`-Weiterleitung in Next.js** ist deshalb **300 s**
(`experimental.proxyTimeout` in `frontend/next.config.ts`). Die Vorgabe in der installierten
Version 16.2.11 ist 30.000 ms (`next/dist/server/lib/router-utils/proxy-request.js`); gemessen wird
die Zeit ohne Daten am Socket, und danach bricht Next.js die Weiterleitung ab und antwortet mit
`500`. Alle übrigen Endpunkte enden weit früher, weil die Datenbank ihre Statements nach 10 s bzw.
30 s beendet (§1); für sie ändert die höhere Grenze nichts. Keine neue Frontend-Variable, der Bau
bleibt `pnpm check`, `pnpm build`, `next start`. Wer `zeitgrenze` oder `werkzeugrunden-je-nachricht`
im Backend anhebt, rechnet die Grenze nach (Punkt 1036).

**Für den Reverse Proxy** (Konfiguration nicht im Repository, ein Hinweis für Lukas, Punkt 1033):
Weil nicht gestreamt wird, braucht er für den Assistenten **keine** Einstellung zu Pufferung oder
Kompression. Er braucht aber eine **Lese-Zeitgrenze von mindestens 300 s** für
`/api/assistent/gespraeche/*/nachrichten` (oder für `/api` insgesamt): Gängige Vorgaben liegen bei
60 s (nginx `proxy_read_timeout`, Apache `ProxyTimeout` bzw. `Timeout`). Ist sie kürzer, bricht der
Proxy eine lange Antwort ab, obwohl Backend und Next.js weiterarbeiten.

**Ausgehende Verbindung:** Der Server, auf dem das Backend läuft, braucht **ausgehendes HTTPS
(Port 443) zu `api.anthropic.com`** (`overlord.assistent.basis-url`), sobald Schalter und Schlüssel
gesetzt sind. Ob der Weg in der Produktion frei ist (Firewall, ausgehender Proxy), ist ungeprüft
(Punkt 1010). Ist er gesperrt, antwortet der Assistent `503 assistent-voruebergehend-gestoert`,
spätestens nach der Verbindungs-Zeitgrenze von 30 s; der Rest der Anwendung ist davon nicht berührt.
Einen ausgehenden HTTP-Proxy kennt das Backend nicht: `AnthropicSprachmodell` baut seinen
`HttpClient` ohne `ProxySelector`.

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

*Bis 1.1.0 stand hier als dritter Schritt: Nach einem Fix-Release wird `main` in `assistent`
gemergt. Mit dem Zusammenführen gibt es eine Linie (E‑1127), der Schritt entfällt.*

**Für 1.1.0 zusätzlich:** nach dem ersten Start das Skript für `SYSTEM`
([`tasks.md`](tasks.md) §3.9), Schalter und Schlüssel nach §3, Reverse Proxy und ausgehende
Verbindung nach §5a.

**Bau und Auslieferung nach dem Tag sind im Repository nicht beschrieben.** Die CI
([`.github/workflows/ci.yml`](../.github/workflows/ci.yml)) baut und prüft Pushes auf `main`,
`assistent` und `assistent-*` und jeden Pull Request, ohne Datenbank
(`-Djooq.codegen.skip=true -DexcludedGroups=db`); sie reagiert auf keinen Tag, lädt kein Artefakt
hoch und liefert nichts aus. Ob die Auslöser für `assistent` bleiben, ist Punkt 1035. Siehe die
offenen Fragen.

---

## 8. Rollback

- **Code:** den vorigen Tag bauen und ausliefern.
- **Schema:** Flyway kennt keine Rückwärtsmigration. Ein Rollback heißt deshalb **Wiederherstellung
  aus einer Sicherung** oder **Vorwärtskorrektur** — eine neue Migration, die die vorige aufhebt.
  Ein älteres JAR startet gegen ein neueres Schema nicht ohne Weiteres: Flyway findet angewandte
  Migrationen, die es nicht kennt, und ignoriert sie nur über die Vorgabe `*:future`; ob die Anwendung
  danach mit dem Schema arbeitet, hängt an der Migration.
- **Ein 1.0.x-JAR gegen das Schema mit `V100` bis `V103`** *(1.1.0, 21.10.2026)*: Es **startet** —
  `V100` bis `V103` sind für seinen Flyway künftige Migrationen und werden über `*:future`
  übergangen; `V1` bis `V13` sind dieselben Dateien. Es **arbeitet** auch: Die vier Migrationen legen
  nur die neuen Tabellen `task_type`, `task_type_mandant` und `task` an und schreiben dort; keine
  Tabelle, die 1.0.x liest oder schreibt, ist geändert, und `IndexbestandDbIT` von 1.0.x sieht die drei
  Tabellen nicht, weil 1.0.x sie aus dem Codegen und aus der Tabellenprüfung ausnimmt. Die Tabellen
  bleiben stehen, mit ihren Aufträgen; 1.0.x zeigt sie nicht. **Was nicht mehr geht:** ein
  Fix-Release 1.0.x **mit einer Migration**. Jede Nummer, die 1.0.x vergäbe (`V14` …), läge unter
  `V103`; die strenge Reihenfolge der Produktion wiese sie beim Start ab. Ein Fix nach 1.1.0 kommt
  deshalb als 1.1.x mit `V104` und folgenden (E‑1127). Zurück auf 1.1.0 geht ohne Weiteres: Das
  Schema ist dasselbe.
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
| 261 | **Artefaktname:** Die Version stand seit dem 16.10.2026 auf 1.0.0 und steht seit dem 21.10.2026 auf 1.1.0 (`backend/pom.xml`, `frontend/package.json`). Ohne `finalName` heißt das JAR `overlord-monitor-1.1.0.jar`, die Doku schreibt `overlord-monitor.jar` (`rollup.md`, `dienste.md`). Wird `finalName` gesetzt, oder heißt das JAR im Betrieb anders? |
| 262 | **Bootstrap in der Produktion:** Ist das erste Konto über `prod,bootstrap` angelegt worden? (Die beiden `OVERLORD_BOOTSTRAP_ADMIN_*` stehen seit dem 16.10.2026 in `backend/.env.example`.) |
| 263 | **Protokolle:** Wohin geht die Konsolenausgabe (journald, Container, Datei), und wie lange wird sie aufbewahrt? |
| 1010 | **Ausgehende Verbindung:** Erreicht der Produktionsserver `api.anthropic.com` über HTTPS (Firewall, ausgehender Proxy)? Seit A2 offen; mit 1.1.0 Voraussetzung für den Test mit `SYSTEM` (§5a) |
| 1033 | **Reverse Proxy und der Assistent:** Hat der Proxy für `/api` eine Lese-Zeitgrenze von mindestens 300 s? Eine Nachricht an den Assistenten kann bis zu 244 s dauern, ohne dass ein Byte fließt (§5a) |
