# Betrieb

*Angelegt am 16.10.2026 mit Version 1.0.0.* Was das Repository über den Betrieb belegt: Bausteine,
Profile, Umgebungsvariablen, Flyway, Rollup-Job, Ablagenprüfung, Release und Rollback. **Was nicht im
Repository steht, steht hier auch nicht als Tatsache** — es ist als offene Frage am Ende geführt und im
Register [`offene-punkte.md`](offene-punkte.md) eingetragen. Was Lukas über den Server mitteilt,
steht mit Quelle da (§7a). Lokale Unterschiede stehen in
[`START-LOKAL.md`](START-LOKAL.md), die Architektur in
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6.

> **Ergänzt am 21.10.2026 mit 1.1.0.** Die Linie `assistent` ist nach `main` zusammengeführt. Neu für
> den Betrieb: drei **optionale** Variablen des Assistenten (§3), die Migrationen `V100` bis `V103`
> (§4), die Zeitgrenze der Weiterleitung, die ausgehende Verbindung zum Modellanbieter (§5a) und was
> ein älteres JAR gegen das neue Schema bedeutet (§8). Das Deployment bleibt, wie es ist.
>
> **Nachgetragen am selben Tag:** die **Deployment-Routine der Produktion**, mitgeteilt von Lukas
> (§7a, E‑1133); der Schlüssel des Assistenten in einer eigenen `EnvironmentFile` nur für die Unit
> `overlord-monitor` (§3, §7a, E‑1134); vor jedem Schemaeingriff wird das **ganze** Schema gesichert
> (§8, E‑1135). Damit sind die Punkte 255, 260 und 261 erledigt (§9). Neu offen sind die Punkte 1038
> bis 1041 (§7a).

---

## 1. Bausteine

| Baustein | Was das Repository belegt |
|---|---|
| **Backend** | Ein ausführbares JAR mit eingebettetem Server (`spring-boot-maven-plugin` ohne weitere Konfiguration, Paketierung `jar`). Gebaut mit `./mvnw verify`; ohne `finalName` heißt es `target/overlord-monitor-<version>.jar`, auf dem Server liegt es umbenannt als `/opt/overlord-monitor/overlord-monitor.jar` (§7a, Punkt 261). Port `${SERVER_PORT:8080}`, `server.shutdown: graceful`, virtuelle Threads an. Zwei Verbindungspools: `glassfish-read` (höchstens 5, `max_statement_time = 10`) und `monitor-write` (höchstens 5, `max_statement_time = 30`) — `config/DataSourceConfig` |
| **Frontend** | Next.js 16: `pnpm build` (läuft erst nach `pnpm check`), dann `next start` (`frontend/package.json`). Kein `output: "standalone"`; der Betrieb braucht `node_modules`, `.next` und `next start`. Port nicht konfiguriert, also der Next-Standard 3000. `next.config.ts` leitet `/api/*` an `OVERLORD_BACKEND_URL` weiter, und zwar an die Adresse **zur Zeit des Baus** (§3). Auf dem Server liegt das ganze Verzeichnis `frontend/` unter `/opt/overlord-monitor/frontend/` (§7a) |
| **Reverse Proxy** | führt Frontend und Backend unter einer Domain zusammen (Kommentar in `next.config.ts`, [`frontend-grundlagen.md`](frontend-grundlagen.md)). Seine Konfiguration liegt **nicht** im Repository |
| **Rollup-Job** | dieselbe Anwendung mit Profil `rollup`, im Betrieb als eigener Prozess `prod,rollup` ([`rollup.md`](rollup.md) §8), auf dem Server die Unit `overlord-monitor-rollup` (§7a) |

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
| `OVERLORD_ASSISTENT_AKTIV` | Backend | Schalter des Assistenten ([`assistent.md`](assistent.md) §7). Die Prüfung durch den Datenschutzbeauftragten ist erledigt (Punkt 1009, E‑1131). Auf dem Server in `/etc/overlord-monitor/assistent.env` (§7a A) | nein, Vorgabe `false` |
| `OVERLORD_ASSISTENT_API_KEY` | Backend | Schlüssel des Modellanbieters; ohne ihn ist der Assistent für alle Mandanten aus. Auf dem Server **nur** in `/etc/overlord-monitor/assistent.env`, die allein die Unit `overlord-monitor` einliest, nicht `overlord-monitor-rollup` (E‑1134, §7a A) | nein; nur mit eingeschaltetem Assistenten nötig |
| `OVERLORD_ASSISTENT_MODELL` | Backend | Modellkennung beim Anbieter | nein, Vorgabe `claude-sonnet-5-5` |
| `OVERLORD_BACKEND_URL` | Frontend | Ziel der `/api`-Weiterleitung, serverseitig. **Wirkt beim Bau, nicht beim Start:** `pnpm build` schreibt die Adresse in `.next/routes-manifest.json`, und `next start` liest die Weiterleitung nur aus dieser Datei (Next.js 16.2.11, `server/lib/router-utils/filesystem.js`; die Konfiguration wertet nur `next dev` neu aus). In der Unit des Frontends gesetzt, bleibt sie wirkungslos (Punkt 1041) | nein, Vorgabe `http://localhost:8080` |

Der Codegen und das Flyway-Maven-Plugin lesen dieselben `OVERLORD_DB_*`.

**Mit 1.1.0 kommt keine Pflicht-Variable dazu** (21.10.2026). Die drei `OVERLORD_ASSISTENT_*` sind
optional; fehlen sie, startet das Backend wie mit 1.0.x, und der Assistent ist für alle Mandanten aus.
Beim Start steht dann eine Zeile im Protokoll (Stufe `INFO`, Logger `AssistentConfig`):
`Assistent fuer alle Mandanten aus: kein Schluessel (…)` bzw. `…: Schalter aus (…)`, mit
`schluessel=fehlt` oder `schluessel=gesetzt`, **nie mit dem Schlüssel selbst**. Eingeschaltet steht
dort ein `WARN` mit dem Ziel der Aufrufe. Auch der Prozess `prod,rollup` schreibt diese Zeile; der
Assistent tut dort nichts (§5). Nach der Routine aus §7a steht dort `Schalter aus (… schluessel=fehlt)`,
weil die Unit `overlord-monitor-rollup` die Datei des Assistenten nicht einliest.

**Einschalten in der Produktion, mit 1.1.0 für `SYSTEM`:** `OVERLORD_ASSISTENT_AKTIV=true` und
`OVERLORD_ASSISTENT_API_KEY` in die eigene `EnvironmentFile` der Unit `overlord-monitor` schreiben
(§7a A), Backend neu starten (§7a B), dann `scripts/assistent-freischaltung-system.sql` ausführen
(§7a C, [`tasks.md`](tasks.md) §3.9). Verfügbar ist
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
3. **Lukas baut und liefert auf dem Server aus**, nach der Routine in §7a (seit 21.10.2026).

*Bis 1.1.0 stand hier als dritter Schritt: Nach einem Fix-Release wird `main` in `assistent`
gemergt. Mit dem Zusammenführen gibt es eine Linie (E‑1127), der Schritt entfällt.*

**Für 1.1.0 zusätzlich:** Schalter und Schlüssel vor dem ersten Deployment (§7a A), nach dem ersten
Start das Skript für `SYSTEM` (§7a C, [`tasks.md`](tasks.md) §3.9), Reverse Proxy und ausgehende
Verbindung nach §5a.

**Gebaut wird auf dem Produktionsserver** (§7a B, seit 21.10.2026 beschrieben; bis dahin hieß es
hier: „im Repository nicht beschrieben“). Die CI
([`.github/workflows/ci.yml`](../.github/workflows/ci.yml)) baut und prüft Pushes auf `main`,
`assistent` und `assistent-*` und jeden Pull Request, ohne Datenbank, und baut dabei, was der Server
baut: `./mvnw … -Djooq.codegen.skip=true -DexcludedGroups=db verify` schließt die Phase `package` ein,
die der Server mit denselben Schaltern fährt, und das Frontend kommt in beiden mit
`pnpm install --frozen-lockfile` und `pnpm build`. Ist sie grün, scheitert der Bau auf dem Server
höchstens an seiner Umgebung (§7a). Sie reagiert auf keinen Tag, lädt kein Artefakt hoch und liefert
nichts aus. Ob die Auslöser für `assistent` bleiben, ist Punkt 1035.

---

## 7a. Deployment-Routine der Produktion *(21.10.2026, gültig ab 1.1.0)*

**Quelle: Mitteilung von Lukas vom 21.10.2026** (Datei `deploy-routine.txt`, E‑1133). Festgehalten
ist sie hier; nachprüfen lässt sie sich im Repository nicht. Platzhalter stehen in spitzen Klammern,
Host und Passwörter stehen nicht im Repository (G1). Die Befehle stehen, wie mitgeteilt. Ausgefüllt
sind zwei Platzhalter, weil das Repository sie kennt: die Variablen in A und der Pfad des Skripts in
C. Was der Abgleich mit dem Repository ergibt, steht am Ende des Abschnitts.

| | Auf dem Server |
|---|---|
| Betriebsform | systemd mit drei Units: `overlord-monitor` (Backend), `overlord-monitor-rollup` (Prozess `prod,rollup`, §5) und `overlord-monitor-frontend` (`next start`). Die Unit-Dateien selbst sind nicht mitgeteilt |
| Dienstbenutzer | `overlordmon` |
| Quellen und Bau | ein Klon des Repositorys unter `/usr/local/src/overlord-monitor`; dort wird gebaut |
| Backend | `/opt/overlord-monitor/overlord-monitor.jar`, die Vorversion daneben als `overlord-monitor.jar.vorher`. Nach der Routine laufen beide Backend-Units aus diesem einen JAR: B und D halten den Rollup-Prozess an, bevor sie es tauschen |
| Frontend | `/opt/overlord-monitor/frontend/`: das ganze Verzeichnis `frontend/` samt `node_modules` und `.next` (§1). Eine Vorversion wird nicht aufgehoben |
| Stand | `/opt/overlord-monitor/VERSION`, die Ausgabe von `git describe --tags --always` |
| Schlüssel des Assistenten | `/etc/overlord-monitor/assistent.env`, `root:root`, Modus `600`, eingelesen nur von `overlord-monitor` (E‑1134) |
| Protokolle | journald (`journalctl -u overlord-monitor`) |
| Sicherung | vor jedem Deployment das ganze Schema per `mysqldump` ins Heimatverzeichnis des Ausführenden (§8, E‑1135) |

### A. Einmalig vor dem ersten Deployment von 1.1.0: Schlüssel des Assistenten

Nur für die Unit `overlord-monitor`, nicht für `overlord-monitor-rollup`.

```sh
sudo mkdir -p /etc/overlord-monitor
sudo install -m 600 -o root -g root /dev/null /etc/overlord-monitor/assistent.env
sudoedit /etc/overlord-monitor/assistent.env
#   Inhalt, zwei Zeilen (mitgeteilt war eine, <NAME_DER_VARIABLE>=<schluessel>; siehe unten):
#   OVERLORD_ASSISTENT_AKTIV=true
#   OVERLORD_ASSISTENT_API_KEY=<schluessel>

sudo systemctl edit overlord-monitor
#   Inhalt des Drop-ins:
#   [Service]
#   EnvironmentFile=/etc/overlord-monitor/assistent.env

# Ausgehende Verbindung zum Modellanbieter prüfen (401 = erreichbar, Timeout = gesperrt)
curl -sS -o /dev/null -w '%{http_code}\n' https://api.anthropic.com/v1/models
```

- **Zwei Zeilen statt einer.** Der Schlüssel allein schaltet den Assistenten nicht ein: Ohne
  `OVERLORD_ASSISTENT_AKTIV` steht der Schalter auf `false` (§3, E‑1130), und das Protokoll meldet
  `Schalter aus`. In derselben Datei bleibt auch der Schalter dem Rollup-Prozess fern.
- **Wer die Datei liest:** systemd selbst, als `root`, bevor es den Dienst als `overlordmon` startet;
  der Dienstbenutzer braucht kein Leserecht. Der Drop-in ergänzt die Unit, statt sie zu ersetzen
  (`EnvironmentFile=` darf mehrfach stehen), und wirkt ab dem Neustart in B.
- **Gegenprobe, ohne den Schlüssel zu zeigen:** `systemctl cat overlord-monitor` zeigt den Drop-in,
  `systemctl show -p EnvironmentFiles overlord-monitor-rollup` nennt die Datei nicht. Nach dem
  Neustart steht im Protokoll das `WARN` `Assistent eingeschaltet (… schluessel=gesetzt …)` (§3).
- **Die Probe mit `curl`** nimmt einen ausgehenden Proxy aus den Variablen der Shell mit
  (`https_proxy`); die JVM des Backends liest diese Variablen nicht (§5a). Den Weg, den das Backend
  nimmt, prüft `curl --noproxy '*' …`. Das Ergebnis ist nicht mitgeteilt (Punkt 1010).

### B. Jedes Deployment

```sh
cd /usr/local/src/overlord-monitor
git pull
git describe --tags                     # zeigt den Tag des Stands, etwa v1.1.0

export MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"
cd backend  && ./mvnw -B clean package -Djooq.codegen.skip=true -DexcludedGroups=db
cd ../frontend && pnpm install --frozen-lockfile && pnpm build
cd /usr/local/src/overlord-monitor

# Sicherung des ganzen Schemas overlord_monitor vor dem Start (Flyway migriert beim Start)
mysqldump --skip-ssl -h <db-host> -u monitor_write -p --single-transaction overlord_monitor \
  > ~/overlord_monitor_vor-deploy_$(date +%F).sql

sudo cp -a /opt/overlord-monitor/overlord-monitor.jar /opt/overlord-monitor/overlord-monitor.jar.vorher
sudo systemctl stop overlord-monitor-rollup
sudo install -o overlordmon -g overlordmon -m 640 backend/target/overlord-monitor-*.jar /opt/overlord-monitor/overlord-monitor.jar
sudo rsync -a --delete frontend/ /opt/overlord-monitor/frontend/
sudo chown -R overlordmon:overlordmon /opt/overlord-monitor
git describe --tags --always | sudo tee /opt/overlord-monitor/VERSION

sudo systemctl restart overlord-monitor
# Nach dem Start: Migrationen angewandt, Assistent an oder aus?
sudo journalctl -u overlord-monitor --since "5 min ago" | grep -iE "flyway|migrat|assistent"

sudo systemctl restart overlord-monitor-frontend
sudo systemctl start   overlord-monitor-rollup
```

### C. Einmalig nach dem ersten Deployment von 1.1.0: Assistent für `SYSTEM` freischalten

```sh
mysql --skip-ssl -h <db-host> -u monitor_write -p overlord_monitor < scripts/assistent-freischaltung-system.sql
# Gegenprobe: mit einem MANDANT-Konto (etwa NEXANS) anmelden, kein Assistent-Symbol in der Kopfleiste.
```

Aufgerufen im Klon, wo B endet. Erwartet sind drei Ergebnisse, `vorher`, `nachher` und `fertig`; nach
dem ersten Lauf steht in `nachher` `1 1 0 1` (Kopf des Skripts). Stimmt etwas nicht, bricht eine Wache
mit Fehler `1231` ab, `mysql` endet mit Exit 1 und führt nichts mehr aus. **Geprüft am 21.10.2026 auf
der Testkopie** mit dem MariaDB-Client 12.3 in genau dieser Form (`--skip-ssl`, Schema als Argument,
Skript über die Standardeingabe): mit falschem Vorgabeschema Abbruch an Wache 1, mit dem eigenen drei
Ergebnisse und nichts geändert, weil `SYSTEM` dort schon freigeschaltet ist. Die Gegenprobe in die
andere Richtung: Als `ADMIN` im Mandanten `SYSTEM` steht das Symbol, nach dem Neuladen der Seite
([`tasks.md`](tasks.md) §3.9, F36).

### D. Rollback

```sh
sudo systemctl stop overlord-monitor-rollup
sudo cp -a /opt/overlord-monitor/overlord-monitor.jar.vorher /opt/overlord-monitor/overlord-monitor.jar
sudo systemctl restart overlord-monitor
sudo systemctl start   overlord-monitor-rollup
# Das Schema bleibt auf dem neuen Stand; ein älteres JAR ignoriert unbekannte Migrationen.
# Das Frontend hat keine Vorversion: für einen Rollback den vorigen Tag auschecken und neu bauen.
# Zurück auf den alten Datenstand nur über die Sicherung aus B.
```

### Abgleich mit dem Repository *(21.10.2026)*

**Was trägt:**

- **Der Bau ist der der CI** (§7). Am 21.10.2026 auf dem Stand von `main` (`865537e`) in einem eigenen
  Arbeitsbaum nachgefahren: `./mvnw -B clean package -Djooq.codegen.skip=true -DexcludedGroups=db`
  grün mit 1.251 Tests. Unter `target/` liegen danach `overlord-monitor-1.1.0.jar` und
  `overlord-monitor-1.1.0.jar.original`; das Muster `overlord-monitor-*.jar` in B trifft nur das
  erste. `pnpm install` lässt `pnpm-lock.yaml` unverändert (zuletzt geändert vor der Linie
  `assistent`), `pnpm install --frozen-lockfile` läuft durch, `pnpm build` ist grün mit 1.506 Tests.
- **Nur ein Prozess migriert.** Beide Backend-Units führen Flyway aus (§4). B hält
  `overlord-monitor-rollup` an, bevor es das JAR tauscht, und startet ihn erst nach dem Backend; dann
  ist `overlord_monitor` migriert, und der zweite Prozess findet nichts zu tun. D hält dieselbe
  Reihenfolge.
- **D stimmt mit §8 überein:** Ein älteres JAR startet gegen das neuere Schema (`*:future`); zurück auf
  den alten Datenstand geht es nur über die Sicherung.
- **Der Name des JAR (Punkt 261):** `install` legt `overlord-monitor-<version>.jar` als
  `overlord-monitor.jar` ab, unter dem Namen, mit dem [`rollup.md`](rollup.md) §8 und
  [`dienste.md`](dienste.md) §14 es aufrufen. Einen `finalName` braucht es nicht.

**Was abweicht oder fehlt:**

| Stelle | Befund | Folge | Punkt |
|---|---|---|---|
| A | Mitgeteilt ist eine Zeile mit dem Schlüssel; ohne Schalter bleibt der Assistent aus | oben ergänzt: zwei Zeilen | — |
| A, `curl` | prüft mit dem Proxy der Shell, das Backend hat keinen | `--noproxy '*'` (oben) | 1010 |
| B, `git pull` | Gebaut wird der Kopf des ausgecheckten Zweigs, nicht ausdrücklich der Tag. `git describe --tags` zeigt `v1.1.0` nur, wenn der Stand genau der Tag ist, sonst `v1.1.0-<n>-g<hash>`, und B läuft trotzdem weiter | den Tag auschecken (`git checkout v<version>`) oder `git describe --tags --exact-match` als Halt: Es bricht ab, wenn kein Tag genau passt | 1038 |
| B, Bau des Frontends | `OVERLORD_BACKEND_URL` wirkt beim Bau (§3). B baut in der Shell des Ausführenden; ist die Variable dort nicht gesetzt und steht sie auch in keiner `frontend/.env.local` des Klons, steht `http://localhost:8080` im Bau | richtig nur, wenn das Backend auf demselben Server an Port 8080 lauscht; `SERVER_PORT` der Unit ist nicht mitgeteilt | 1041 |
| B, Sicherung und `.vorher` | Der Dateiname trägt nur das Datum, `.vorher` nur einen Stand: Ein zweiter Durchlauf am selben Tag überschreibt die Sicherung von vor der Migration mit einer von danach und `.vorher` mit dem eben installierten JAR. Die Sicherung enthält Passwort-Hashes (`app_user`) und gültige Sitzungen (`SPRING_SESSION`) und entsteht mit den Rechten, die die `umask` der Shell vergibt | Uhrzeit in den Dateinamen und `umask 077` davor; `.vorher` nur anlegen, wenn das laufende JAR nicht schon das neue ist | 1039 |
| B, nach dem Neustart | Im Profil `prod` steht `root` auf `WARN` (`LOG_LEVEL_ROOT`, `application.yml`), und Flyway meldet seine Migrationen mit `INFO`: Der `grep` findet die Zeile des Assistenten, aber keine angewandte Migration. Eine gescheiterte zeigt er, weil der Start dann mit `ERROR` abbricht | Gegenprobe in der Datenbank: `SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 4;`, nach dem ersten Start von 1.1.0 oben `103` bis `100`, je `1`. Wache 2 in C prüft dasselbe | — |
| B als Ganzes | eine Befehlsfolge zum Abarbeiten von Hand, kein Skript: Nichts hält sie an, wenn ein Bau scheitert. `clean` hat das alte JAR unter `target/` dann schon gelöscht, ein abgebrochener `next build` lässt `.next` unvollständig zurück, und `rsync --delete` spiegelte das nach `/opt` | nach einem Fehler nicht weitermachen | — |
| B, zwischen `rsync` und dem Neustart des Frontends | Der alte `next start` läuft auf den neuen Dateien | fällt in den Neustart des Backends, in dem die Anwendung ohnehin nicht antwortet; B ist ein Wartungsfenster | — |
| D | Nur das Backend geht zurück. Das Frontend 1.1.0 vor einem Backend 1.0.x zeigt jedem Nutzer in der Übersicht „Ihre Aufträge konnten gerade nicht geladen werden.“, weil es `GET /api/tasks` dort nicht gibt; das Symbol des Assistenten fehlt still | Zurück auf 1.0.x gehört das Frontend dazu, und das hat keine Vorversion | 1040 |

---

## 8. Rollback

- **Code:** das Backend über das aufgehobene JAR `overlord-monitor.jar.vorher` (§7a D). Das Frontend
  hat keine Vorversion; es wird aus dem vorigen Tag neu gebaut und ausgeliefert. Ein Backend, das
  älter ist als das Frontend, kennt Endpunkte nicht, die das Frontend ruft: Zurück auf 1.0.x gehört
  das Frontend dazu (Punkt 1040). *Bis 21.10.2026 stand hier nur: den vorigen Tag bauen und
  ausliefern.*
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
  Schema ist dasselbe. Das Frontend 1.1.0 vor einem 1.0.x-Backend zeigt allerdings jedem Nutzer in
  der Übersicht den Fehler unter „Ihre Aufträge“ (§7a, Punkt 1040).
- **Was vor jedem Schemaeingriff gesichert wird: das ganze Schema `overlord_monitor`** (E‑1135,
  21.10.2026). Von Hand gepflegt sind nicht nur `process_catalog` und `bam_sollaenge`, sondern auch
  `app_user`, `app_user_mandant` und `bam_spalte` (Auskunft von Lukas), dazu die Freischaltungen in
  `task_type_mandant` (E‑1126) und neue Typdefinitionen in `task_type` (E‑1019); die Liste mit dem
  Grund je Tabelle steht in
  [`bam-sollaengen.md`](bam-sollaengen.md) §7.2. Weil jeder Start migrieren kann (§4), heißt das: vor
  jedem Deployment, mit `mysqldump --single-transaction` in §7a B. Mitgesichert sind damit auch
  `flyway_schema_history` und die Tabellen, die sich aus `GlassfishDB` neu berechnen ließen
  (`message_rollup` und die abgeleiteten Ebenen, Volllauf). *Bis 21.10.2026 standen hier nur
  `process_catalog` und `bam_sollaenge`, und „wie und wohin gesichert wird, steht nicht im
  Repository“.*
- **Wiederherstellen** heißt: alle drei Units anhalten, die Sicherung einspielen
  (`mysql … overlord_monitor < <sicherung>.sql`), dann das JAR starten, das zu ihr gehört. Verloren
  ist, was zwischen Sicherung und Wiederherstellung geschrieben wurde: Sitzungen, `audit_log`,
  Aufträge. **`mysqldump` legt nur die Tabellen neu an, die es gesichert hat.** Spielt man eine
  Sicherung von vor 1.1.0 zurück, bleiben `task_type`, `task_type_mandant` und `task` stehen, während
  `flyway_schema_history` `V100` bis `V103` nicht mehr kennt; ein späterer Start von 1.1.0 scheitert
  dann an `V100` (`CREATE TABLE` ohne `IF NOT EXISTS`), bis die drei Tabellen gelöscht sind. Ob
  außerhalb von Deployments gesichert wird, wie lange die Sicherungen bleiben und wer wiederherstellt,
  ist offen (Punkt 259).

---

## 9. Offene Fragen an Lukas

Nicht im Repository belegt. Je Frage ein Eintrag im Register.

| Nr. | Frage |
|---|---|
| 255 | **Betriebsform:** systemd-Dienst oder Container? Wo liegen JAR und Frontend auf dem Server, unter welchem Benutzer, wie heißen die Units? *Beantwortet am 21.10.2026 (§7a):* systemd mit den Units `overlord-monitor`, `overlord-monitor-rollup` und `overlord-monitor-frontend`, Dienstbenutzer `overlordmon`, JAR und Frontend unter `/opt/overlord-monitor/`. **Erledigt** |
| 256 | **Reverse Proxy:** Konfiguration, Domain und Zertifikat liegen nicht im Repository. Routet er `/api` an Next.js (dann über `OVERLORD_BACKEND_URL`) oder direkt an das Backend? |
| 257 | **Client-IP hinter dem Proxy:** `prod` setzt `forward-headers-strategy: framework`, Anmeldung und `audit_log` lesen `getRemoteAddr()`. Sieht die IP-Begrenzung in der Produktion die echte Client-IP (Kette Proxy → Next.js → Backend)? Ungeprüft |
| 258 | **Rollup-Prozess:** Läuft er als zweiter Prozess `prod,rollup`, auf welchem `SERVER_PORT` (beide starten den Webserver), mit `--overlord.ablagenpruefung.aktiv=false`? *21.10.2026:* Er ist die Unit `overlord-monitor-rollup` und startet in B nach dem Backend, sodass nur ein Prozess migriert (§7a). Port und Ablagenprüfung stehen in der Unit, die nicht mitgeteilt ist; offen |
| 259 | **Sicherung:** Wie und wie oft wird `overlord_monitor` gesichert, insbesondere `process_catalog` und `bam_sollaenge` vor jedem Schemaeingriff, und wer stellt wieder her? Davon hängt der Rollback in §8 ab. *Teilweise beantwortet am 21.10.2026:* Vor jedem Deployment wird das ganze Schema per `mysqldump` gesichert (§7a B, §8, E‑1135). Offen: ob außerhalb von Deployments gesichert wird, wie lange die Sicherungen bleiben, wer wiederherstellt |
| 260 | **Release-Bau und Auslieferung:** Wer baut nach dem Tag JAR und Frontend, wo, und wie kommen sie auf den Server? *Beantwortet am 21.10.2026 (§7a):* Lukas baut von Hand auf dem Produktionsserver, in einem Klon unter `/usr/local/src/overlord-monitor` nach `git pull`, und liefert per `install` und `rsync` nach `/opt/overlord-monitor/` aus. **Erledigt.** Dass `git pull` den Kopf des Zweigs baut und nicht ausdrücklich den Tag, ist Punkt 1038 |
| 261 | **Artefaktname:** Die Version stand seit dem 16.10.2026 auf 1.0.0 und steht seit dem 21.10.2026 auf 1.1.0 (`backend/pom.xml`, `frontend/package.json`). Ohne `finalName` heißt das JAR `overlord-monitor-1.1.0.jar`, die Doku schreibt `overlord-monitor.jar` (`rollup.md`, `dienste.md`). Wird `finalName` gesetzt, oder heißt das JAR im Betrieb anders? *Beantwortet am 21.10.2026 (§7a):* `finalName` wird nicht gesetzt; die Routine installiert `overlord-monitor-<version>.jar` als `/opt/overlord-monitor/overlord-monitor.jar`, den Namen aus `rollup.md` und `dienste.md`. **Erledigt** |
| 262 | **Bootstrap in der Produktion:** Ist das erste Konto über `prod,bootstrap` angelegt worden? (Die beiden `OVERLORD_BOOTSTRAP_ADMIN_*` stehen seit dem 16.10.2026 in `backend/.env.example`.) |
| 263 | **Protokolle:** Wohin geht die Konsolenausgabe (journald, Container, Datei), und wie lange wird sie aufbewahrt? *21.10.2026:* nach journald (§7a). Offen bleibt die Aufbewahrung |
| 1010 | **Ausgehende Verbindung:** Erreicht der Produktionsserver `api.anthropic.com` über HTTPS (Firewall, ausgehender Proxy)? Seit A2 offen; mit 1.1.0 Voraussetzung für den Test mit `SYSTEM` (§5a). Die Probe steht seit dem 21.10.2026 in §7a A, mit `--noproxy '*'`; ihr Ergebnis ist nicht mitgeteilt |
| 1033 | **Reverse Proxy und der Assistent:** Hat der Proxy für `/api` eine Lese-Zeitgrenze von mindestens 300 s? Eine Nachricht an den Assistenten kann bis zu 244 s dauern, ohne dass ein Byte fließt (§5a) |
| 1038 | **Tag oder Zweig:** B baut nach `git pull` den Kopf des ausgecheckten Zweigs; `git describe --tags` zeigt nur an, ob das der Tag ist, und hält nicht an. Soll B den Tag auschecken oder an `git describe --tags --exact-match` anhalten (§7a)? |
| 1039 | **Sicherung in B:** Ein zweiter Durchlauf am selben Tag überschreibt die Sicherung von vor der Migration und `.vorher`; die Sicherung trägt Passwort-Hashes und gültige Sitzungen mit den Rechten der `umask`. Uhrzeit in den Namen, `umask 077`, `.vorher` nur vom alten JAR (§7a)? |
| 1040 | **Rollback des Frontends:** D nimmt nur das Backend zurück, und das Frontend hat keine Vorversion. Vor einem 1.0.x-Backend zeigt das Frontend 1.1.0 jedem den Fehler unter „Ihre Aufträge“. Wird `frontend/` wie das JAR aufgehoben (§7a, §8)? |
| 1041 | **Ziel der Weiterleitung:** `OVERLORD_BACKEND_URL` wirkt beim Bau (§3), B baut ohne sie, also mit `http://localhost:8080`. Lauscht das Backend auf dem Server dort, oder gehört die Variable in die Shell des Baus bzw. in `frontend/.env.local` des Klons (§7a)? |
