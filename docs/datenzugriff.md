# Datenzugriff

Entsteht in Schritt 2. Beschreibt, **wie** das Backend liest und schreibt: die zwei DataSources,
die zwei DSLContexts, der eine Transaktionsmanager, der dreischichtige Schreibschutz auf
`GlassfishDB`, Flyway für `overlord_monitor`, die Zeitquellen, das Fehlerformat und die jOOQ-
Codegenerierung.

Alle in diesem Dokument genannten Fakten sind am 28.07.2026 gegen die Testkopie
(interne Testkopie, MariaDB `10.6.22`) verifiziert, nicht angenommen.

---

## 1. Zwei DataSources, eine Rechteregel

| Bean | Pool | Benutzer | Rechte | Zweck |
|---|---|---|---|---|
| `glassfishDataSource` | `glassfish-read` | `monitor_read` | `SELECT` auf **beide** Schemata | Lesen, auch schemaübergreifend |
| `monitorDataSource` | `monitor-write` | `monitor_root` | `SELECT` auf `GlassfishDB`, `ALL PRIVILEGES` auf `overlord_monitor` | Schreiben ins eigene Schema |

Beide Pools sind HikariCP, beide in `config/DataSourceConfig`.

**Keine der beiden DataSources ist `@Primary`.** Ein vergessener `@Qualifier` löst beim Start eine
`NoUniqueBeanDefinitionException` aus, statt zur Laufzeit still den falschen Pool zu treffen — ein
Startfehler ist besser als ein Laufzeitfehler in einem selten begangenen Codepfad.

Lese-Pool `glassfish-read`:
- `readOnly = true`
- `maximumPoolSize = 5`
- `connectionInitSql = "SET SESSION max_statement_time=10"`
- `connectionTimeout = 5 s`, `maxLifetime = 25 min`

Schreib-Pool `monitor-write`:
- `maximumPoolSize = 5`
- `connectionInitSql = "SET SESSION max_statement_time=30"`

### `transaction_read_only` gibt es auf MariaDB 10.6 nicht

Vorgeschlagen war, den Lese-Pool zusätzlich mit `SET SESSION max_statement_time=10,
transaction_read_only=1` zu initialisieren. **Getestet am 28.07.2026 als `monitor_read`:**

```
SET SESSION max_statement_time=10, transaction_read_only=1   -> Fehler 1193 "Unknown system variable 'transaction_read_only'"
SET SESSION max_statement_time=10                            -> OK
```

`transaction_read_only` ist eine MySQL-Variable; MariaDB 10.6 kennt sie nicht (MariaDB führt nur die
veraltete `tx_read_only`). Deshalb bleibt der Lese-Pool bei der **kurzen Form**
`SET SESSION max_statement_time=10`. Der `readOnly`-Marker des Pools bleibt als Zusatz bestehen (siehe §4).

---

## 2. Ein Transaktionsmanager

Genau **ein** `PlatformTransactionManager` (`JdbcTransactionManager`), gebunden an
`monitorDataSource`, markiert `@Primary`. Damit bedeutet `@Transactional` im gesamten Projekt
eindeutig „schreibt ins eigene Schema".

Folge, die man kennen muss: Ein Lesezugriff innerhalb einer `@Transactional`-Methode läuft auf einer
anderen Verbindung (dem Lese-Pool) und ist **nicht** Teil dieser Transaktion. Für den Lese-Pool wird
bewusst kein Transaktionsmanager angelegt.

---

## 3. Zwei DSLContexts

Beans `glassfishDsl` und `monitorDsl` in `config/JooqConfig`, Dialekt `MARIADB`.

> **Die Regel:** Der Lese-DSLContext darf **beide** Schemata lesen. Der Schreib-DSLContext darf
> ausschließlich `overlord_monitor` und ausschließlich dort schreiben.

- **Kein `defaultSchema`.** Schemanamen werden immer voll qualifiziert gerendert (`` `GlassfishDB`.`Message` ``),
  sonst funktionieren schemaübergreifende Abfragen nicht. Der generierte Code trägt die
  Schemazugehörigkeit, weil je Schema getrennt generiert wird.
- Bezeichner bleiben **gequotet** (jOOQ-Standard). Die Tabellennamen im Quellschema sind gemischt
  geschrieben (`Message`, `MessageBAM`) und auf dem Linux-Server case-sensitiv.
- `glassfishDsl` trägt den Schreibschutz-Listener (§4).
- `monitorDsl` läuft über einen `TransactionAwareDataSourceProxy`, damit `@Transactional` (auch
  `REQUIRES_NEW`) greift.

**Boots jOOQ-Autokonfiguration ist abgeschaltet** (`spring.autoconfigure.exclude:
JooqAutoConfiguration`). Sie leitet einen `ConnectionProvider` aus genau einer DataSource ab und
scheitert an den zwei bewusst nicht-primären DataSources. Die beiden DSLContexts werden von Hand
gebaut.

---

## 4. Schreibschutz auf `GlassfishDB` — drei Schichten, nicht gleichwertig

1. **Die DB-Rechte sind die Wahrheit — sie tragen die Garantie.** `monitor_read` und `monitor_root`
   besitzen auf `GlassfishDB` ausschließlich `SELECT`. Verifiziert per `SHOW GRANTS` am 28.07.2026.
2. **Der `readOnly`-Pool ist Zusatz, kein Nachweis.** Beim MariaDB-Treiber ist nicht verlässlich, was
   `setReadOnly` serverseitig bewirkt. Nicht als Garantie werten.
3. **Ein jOOQ-`ExecuteListener` auf `glassfishDsl`** (`config/ReadOnlyExecuteListener`) wirft
   `ReadOnlyViolationException`, sobald `ExecuteContext.type()` etwas anderes als `ExecuteType.READ`
   ist. Fängt den Fehler im Code, nicht erst im Netz.

**ArchUnit prüft das Schreibverbot nicht** — es lässt sich maschinell nicht erkennen. Dafür sorgen
die drei Schichten oben. ArchUnit prüft die *Kapselung*: generierte `jooq.glassfish`-Typen dürfen nur
in Klassen auftauchen, deren Name auf `Repository` endet.

### Die Testkopie ist serverseitig `read_only` — ein vierter, unbeabsichtigter Schutz

Der Server der Testkopie läuft global mit `read_only = ON` (verifiziert:
`SELECT @@global.read_only` → `1`). Für `monitor_read` (kein Schreibrecht, kein `READ_ONLY ADMIN`)
scheitert deshalb jeder Schreibversuch auf `GlassfishDB` bereits mit **Fehler 1290** („server is
running with the --read-only option"), noch vor der Rechteprüfung. Der Schreibverbotstest akzeptiert
deshalb `1290` **oder** `1142` (fehlendes Recht) — beides weist den Schreibversuch ab.

`monitor_root` besitzt `ALL PRIVILEGES` auf `overlord_monitor` und schreibt dort erfolgreich, obwohl
der Server global `read_only` ist (verifiziert: sechs von sechs Schreibversuchen erfolgreich).

> ⚠️ **Bekannter, transienter Effekt.** In einem kurzen Fenster (vermutlich während die Testkopie neu
> befüllt wird) scheiterte am 28.07.2026 auch `monitor_root` kurzzeitig mit `1290`. Schlägt ein Build
> mit „server is running with the --read-only option" fehl, ist das kein Code-Fehler — den Lauf
> wiederholen. In Produktion tritt das nicht auf.

---

## 5. Flyway, ausschließlich für `overlord_monitor`

- `monitorDataSource` ist mit `@FlywayDataSource` annotiert; zur Laufzeit migriert Spring Boot beim
  Start.
- `default-schema` und `schemas` = `overlord_monitor`, `create-schemas: false` (Schema existiert),
  `baseline-on-migrate: false`.
- **Keine Fremdschlüssel über Schemagrenzen**, in keiner Migration.
- Zeichensatz und Sortierung stehen in **jeder** Migration explizit
  (`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci`), nie geerbt — sonst bricht bei
  einem Server- oder Versionswechsel (MariaDB 11 setzt `uca1400` als Standard) jeder Join gegen
  `GlassfishDB`.
- Token- und Hash-Spalten (ab Schritt 3: Session-IDs, Rücksetz-Token) bekommen `COLLATE utf8mb4_bin`.

Für den lokalen Erst-Aufbau (bevor der Codegen das Monitor-Schema lesen kann) wird die Migration
über das **flyway-maven-plugin** angewandt:

```
cd backend && ./mvnw org.flywaydb:flyway-maven-plugin:migrate
```

Das Plugin ist bewusst an **keine** Bauphase gebunden — die CI erreicht die Datenbank nicht.

### `V1__audit_log.sql`

Begründungen zum Schnitt:
- **`DATETIME(3)` in UTC statt `TIMESTAMP`** — wegen Zeitzonen und der 2038-Grenze. Im eigenen Schema
  wird UTC gespeichert.
- **`event_type` als `VARCHAR` mit Whitelist im Code** (`audit.AuditEventType`), kein `ENUM` — sonst
  kostet jeder neue Ereignistyp eine Migration.
- **`actor_username` zusätzlich zur ID**, weil ein Fehlversuch auf einen unbekannten Namen keine ID
  hat und genau der interessanteste Eintrag der Tabelle ist.
- **`ip VARCHAR(45)`** wegen IPv6.
- **`mandant_id VARCHAR(36)`** passend zu `GlassfishDB.Mandant.MandantID`.

`audit/AuditLogWriter` schreibt in einer **eigenen** Transaktion (`Propagation.REQUIRES_NEW`): rollt
die Fachtransaktion zurück, bleibt der Protokolleintrag bestehen. Der Zeitpunkt kommt aus der
Systemuhr in UTC (`systemClock`), nicht aus der Anwendungsuhr.

---

## 6. Zeitquellen

Zwei `Clock`-Beans in `common/ZeitConfig` (bewusst in `common`: die einzige Stelle, die die Systemuhr
liest — `LocalDateTime.now()` und Verwandte werden nirgends aufgerufen, Regel Z1, ArchUnit prüft das):

| Bean | Zweck | Quelle |
|---|---|---|
| Anwendungsuhr (`@Primary`, Typ `Clock`) | relative Zeitfenster, Timeout-Berechnung | prod: Systemuhr · dev: um den Rückstand der Testkopie zurückversetzt |
| `systemClock` | **sicherheitsrelevante Zeit** (Sitzung, Sperrfristen ab Schritt 3) und Protokollzeit | immer echte Uhr, UTC |

**Dev-Versatz:** Beim Start liest die Anwendung `MAX(Message.MessageLastUpdate)` und versetzt die
Uhr um die Differenz zurück; die Zeit **läuft weiter** (kein Einfrieren). Gemessen am 28.07.2026:
Maximum `2026-07-08 17:21:10`, Versatz rund **19 Tage**. Der Wert wird ermittelt, nicht eingetragen —
er wächst täglich und springt bei jeder Neubefüllung. Schlägt die Abfrage fehl oder ist sie leer,
fällt die Uhr mit `WARN` auf die Systemuhr zurück.

Startmeldung (dev):

```
WARN ... ZeitConfig : Dev-Clock aktiv: Anwendungszeit auf 2026-07-08T17:21:10 zurueckversetzt
                      (Versatz -19 Tage / -474 Stunden). Sicherheitsrelevante Zeit nutzt weiterhin die Systemuhr.
```

> **Sicherheitsrelevante Zeit nutzt niemals die Anwendungsuhr**, sondern `systemClock`. Sonst wären
> Sitzungsablauf und Sperrfristen im Dev-Profil um 19 Tage verschoben.

---

## 7. Zeitzonen

Zeitstempel aus `GlassfishDB` werden als **Wanduhrzeit des Servers** behandelt und nirgends
konvertiert. Die JDBC-URL trägt **keine** Zeitzonenparameter. Im eigenen Schema wird UTC gespeichert.
Die Bruchstelle ist bewusst und dokumentiert — die Alternative, alles auf UTC zu ziehen, scheitert
daran, dass wir `GlassfishDB` nicht anfassen dürfen.

Der Zeitzonen-Rundlauf-Test (`DatenzugriffDbIT`) liest denselben `MessageLastUpdate` einmal als
Zeitstempel und einmal als `DATE_FORMAT(...)` und prüft auf Gleichheit. Schlägt er fehl, konvertiert
der Treiber und die URL braucht einen expliziten Zeitzonenparameter.

---

## 8. Fehlerformat

- RFC 9457 `ProblemDetail`, `application/problem+json`, ein globaler `@RestControllerAdvice`
  (`common/error/GlobalExceptionHandler`) — die **einzige** Stelle, die Ausnahmen übersetzt.
- Zusätzliches Feld **`traceId`** (Korrelations-ID; steht auch im Serverprotokoll).
- **Regel ab hier:** Eine nicht existierende Ressource und eine Ressource eines fremden Mandanten
  liefern beide **`404`**, niemals `403`. Die Antwort darf nicht verraten, dass es den Datensatz gibt
  (Regel M3).
- Stacktraces, SQL-Fragmente, Tabellen-, Spalten- und Klassennamen erscheinen **nie** in der Antwort —
  auch nicht im Dev-Profil.

> Hinweis zur Benennung: In `DEVELOPMENT_GUIDELINES.md` §5.5 hieß das Feld ursprünglich `fehlerId`.
> Verbindlich ist ab Schritt 2 der Name **`traceId`** (so auch die Abnahme des Schritts). Die
> Richtlinie ist entsprechend angeglichen.

---

## 9. jOOQ-Codegenerierung

- `jooq-codegen-maven` **ohne eigene `<version>`** (kommt aus dem Spring-Boot-BOM, `3.21.5`, an die
  Laufzeit gekoppelt). Zwei Ausführungen mit dem Benutzer `monitor_root`:
  - `GlassfishDB` → `de.kraftwerkone.overlord.monitor.jooq.glassfish`
  - `overlord_monitor` → `de.kraftwerkone.overlord.monitor.jooq.monitor`
- Ausgabe `src/main/generated-java`, **eingecheckt**. Grund: Der Codegen läuft lokal gegen die
  Testkopie, aber die GitHub-CI erreicht die interne Testkopie nicht. Ohne versionierte Quellen scheitert
  dort jeder Build mit Kompilierfehlern, die wie Syntaxfehler aussehen.
- `build-helper-maven-plugin` fügt das Verzeichnis als Quellverzeichnis hinzu; Spotless ist explizit
  auf `src/main/java` beschränkt und fasst den generierten Code nicht an.
- Property `jooq.codegen.skip` (Standard `false`); die CI setzt `true`.
- `.gitattributes`: `src/main/generated-java/** linguist-generated=true`.
- Je Schema: Views **einschließen** (`MessageMandantID` wird für die Mandantentrennung gebraucht),
  Routinen/Sequenzen/Pakete/UDTs ausschließen, `forcedType` `bit(1)` → `Boolean` (betrifft
  `Message.Source` und `Message.Target` — verifiziert: beide werden `Boolean`).

**Voraussetzung des Codegens:** `overlord_monitor.audit_log` muss existieren. Reihenfolge beim
Erst-Aufbau: `flyway:migrate` → `generate-sources`. Danach ist die Tabelle da und der Codegen läuft
bei jedem Build mit.

---

## 10. Tests, zwei Stufen

**Ohne Datenbank** (laufen überall, auch in der CI — Surefire):
- ArchUnit (`PaketstrukturTest`): u. a. `jooq.glassfish` nur in `*Repository`, `DataSource` nur in
  `config`, kein `now()` außerhalb `common`, generierte Pakete von allen Regeln ausgenommen.
- `ReadOnlyExecuteListenerTest`: weist einen Schreibversuch auf einer jOOQ-Mock-Verbindung ab.
- `DevClockFactoryTest`: erzeugt aus einem bekannten Maximum den erwarteten Versatz.
- `FehlerformatTest`: `application/problem+json` samt `traceId`, ohne interne Details.
- `MessageStatusClassifierTest`: Einordnung und die eine SQL-Fehlerbedingung.

**Mit Datenbank**, `@Tag("db")`, Namensschema `*IT` (Failsafe, Phase `verify`) bzw. der
Kontext-Rauchtest `OverlordMonitorApplicationTest` (Surefire). In der CI über `-DexcludedGroups=db`
ausgeschlossen — in Surefire wie in Failsafe:
- **Rauchtest** — liest echte Mandanten und Projekte.
- **Schemaübergreifender Join** — ein Statement über `glassfishDsl`, `GlassfishDB.Process` gegen
  `overlord_monitor.audit_log`, Ergebnis leer. Belegt Mechanismus und Sortierungsverträglichkeit.
- **Schreibverbot** — rohe JDBC-Verbindung aus `glassfishDataSource`, `UPDATE ... WHERE 1=0`, erwartet
  `1290`/`1142`.
- **Statuskatalog** — `DISTINCT MessageStatus` gegen die dokumentierte Menge (siehe
  `message-status.md`).
- **Zeitzonen-Rundlauf** — siehe §7.
- **Schemaabgleich** — generierte `Message`-Spalten gegen `information_schema.COLUMNS`.
- **AuditLogWriter** — der Protokolleintrag überlebt den Rollback der Fachtransaktion.
- **Health** — `/actuator/health` grün, beide Verbindungen unter ihren Bean-Namen.

---

## 11. Betriebs- und Umgebungshinweise

- Zugangsdaten kommen ausschließlich aus Umgebungsvariablen (`OVERLORD_DB_*`), gebunden über
  `config/DatabaseProperties` (`overlord.db.*`). Vorlage: `backend/.env.example`. Nichts davon steht
  in einer versionierten Datei.
- **Maven braucht in diesem Netz IPv4:** Der Weg zu Maven Central scheitert über IPv6 (Timeout). Für
  lokale Builds `MAVEN_OPTS="-Djava.net.preferIPv4Stack=true"` setzen (oder `.mvn/jvm.config`). Die
  GitHub-CI ist davon nicht betroffen.

## 12. Messungen (Regel L7)

Die beiden wiederkehrenden Lesezugriffe dieses Schritts gegen die Testkopie (`Message`: 3,34 Mio.
Zeilen, 2,9 GB):

| Abfrage | Zugriffspfad (`EXPLAIN`, 28.07.2026) | Laufzeit |
|---|---|---|
| `SELECT MAX(MessageLastUpdate) FROM Message` (Dev-Clock beim Start) | `Select tables optimized away` — aus dem Index gelesen, kein Tabellenzugriff | ~7 ms |
| `SELECT DISTINCT MessageStatus FROM Message` (Statuskatalog-Test) | `type=range`, `key=MessageStatusIDX`, `Using index for group-by` (Loose Index Scan, `rows≈19`) | ~1 ms |

> Fachliche Abfragen entstehen erst ab Schritt 4; ihre Messungen stehen in den jeweiligen
> Feature-Dokumenten.
