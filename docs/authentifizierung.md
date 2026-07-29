# Authentifizierung

Entsteht in Schritt 3, Teil 1 (Backend). Beschreibt, **wie** sich ein Nutzer anmeldet: Sperrlogik,
Auskunftsdisziplin, Sitzung und Cookie, Passwortwechsel, Bootstrap und warum die Altnutzer bewusst
nicht übernommen werden.

Die Mandantentrennung — `MandantContext`, Mandantenwechsel, Isolationstest — steht in
[`mandantentrennung.md`](mandantentrennung.md).

Alle in diesem Dokument genannten Messungen und Verhaltensweisen sind am 29.07.2026 gegen die
Testkopie (MariaDB `10.6.22`) verifiziert, nicht angenommen.

---

## 1. Endpunkte

| Methode | Pfad | Rolle | Zweck |
|---|---|---|---|
| `POST` | `/api/auth/login` | — | Anmeldung |
| `POST` | `/api/auth/logout` | — | Sitzung serverseitig verwerfen |
| `GET` | `/api/auth/me` | angemeldet | Nutzer, Rolle, aktiver Mandant, `mustChangePassword` |
| `POST` | `/api/auth/password` | angemeldet | Passwortänderung mit altem und neuem Passwort |
| `GET` | `/api/mandanten` | angemeldet | wählbare Mandanten — siehe `mandantentrennung.md` |
| `POST` | `/api/auth/mandant` | angemeldet | Mandantenwechsel — siehe `mandantentrennung.md` |
| `POST` | `/api/admin/users` | `ADMIN` | Nutzer anlegen |

`POST /api/auth/login` und `POST /api/auth/logout` sind ohne Sitzung erreichbar. Das Abmelden
bewusst auch: Eine abgelaufene Sitzung soll sich abmelden lassen, ohne dass der Aufrufer erst ein
`401` behandeln muss.

Alle Antworten sind `camelCase`/englisch, alle Fehlertexte deutsch und für den Nutzer lesbar
(RFC 9457, siehe [`datenzugriff.md`](datenzugriff.md) §8). Die Selbstauskunft:

```json
{
  "username": "lukas",
  "role": "MANDANT",
  "mandant": { "id": "VOTG", "name": "VOTG Tanktainer GmbH" },
  "mustChangePassword": false,
  "downloadAllowed": true
}
```

`mandant` ist `null`, solange keiner gewählt ist — bei jedem ADMIN und bei jedem Nutzer mit mehreren
Mandanten ist das der Zustand direkt nach dem Anmelden. Das ist **kein Fehler**, sondern eine offene
Auswahl.

---

## 2. Warum die Anmeldung ein eigener Controller ist

Weder `formLogin` noch ein `DaoAuthenticationProvider` sind im Spiel. Der Grund ist die
**Reihenfolge der Prüfungen**, und die ist der eigentliche Inhalt von `security/AnmeldeService`:

1. **IP-Begrenzung** zuerst — sie kostet nichts und hält Massenversuche fern.
2. **Passwortvergleich** vor jeder Kontoprüfung.
3. **Erst danach** Sperre und Deaktivierung.

`DaoAuthenticationProvider` prüft den Kontozustand **vor** dem Passwort (`preAuthenticationChecks`).
Damit verriete die Antwort einem Fremden, dass es das Konto gibt — genau das, was Regel A3 verbietet.
Die Reihenfolge ließe sich dort nur durch Umbau der Standardklasse ändern; ein eigener Service ist
ehrlicher und ist lesbar.

Für die Sitzung selbst werden die Standardbausteine von Spring Security verwendet, nicht
Selbstgebautes: `ChangeSessionIdAuthenticationStrategy` und
`DelegatingSecurityContextRepository` (`security/SitzungsVerwaltung`).

---

## 3. Auskunftsdisziplin — was der Nutzer erfährt

| Fall | Antwort | Titel |
|---|---|---|
| Unbekannter Benutzername | `401` | „Anmeldung fehlgeschlagen" |
| Falsches Passwort | `401` | „Anmeldung fehlgeschlagen" |
| Konto ohne Passwort-Hash | `401` | „Anmeldung fehlgeschlagen" |
| Passwort **korrekt**, Konto gesperrt | `401` | „Konto gesperrt" |
| Passwort **korrekt**, Konto deaktiviert | `401` | „Konto deaktiviert" |
| IP-Kontingent erschöpft | `429` | „Zu viele Anmeldeversuche" |

Die ersten drei Zeilen sind **byte-gleich** — ein Test vergleicht die vollständigen Antwortrümpfe
(ohne `traceId`) und schlägt fehl, sobald sie sich unterscheiden.

**Die Ausnahme in Zeile 4 und 5 ist Absicht.** Wer das richtige Passwort kennt, erfährt nichts Neues;
wer es nicht kennt, bekommt weiterhin die unspezifische Meldung. Ohne diese Ausnahme rennt ein
berechtigter Nutzer fünfzehn Minuten gegen eine Wand, ohne den Grund zu erfahren — und ruft an.

### Der Dummy-Vergleich

**Auch der unbekannte Benutzername kostet einen vollständigen BCrypt-Durchlauf.** Ohne ihn antwortete
der unbekannte Fall in zwei statt zweihundert Millisekunden, und Benutzernamen ließen sich über die
Laufzeit durchprobieren — die gleichlautende Fehlermeldung allein wäre wertlos.

Der Dummy-Hash wird beim Start aus einem Zufallswert erzeugt, dessen Klartext niemand kennt und der
sofort verworfen wird. Ein im Quelltext eingetragener Hash sähe aus wie eine Zugangsdatei und müsste
erklärt werden; ein zufälliger kann nie zu einem Konto passen. Er trägt denselben Kostenfaktor wie
jeder echte Hash — sonst wäre der Vergleich schneller und der Zweck verfehlt.

Ein Konto **ohne** `password_hash` (`NULL`) durchläuft denselben Vergleich und kann sich nie anmelden.

> **Getestet wird der Aufruf, nicht die Laufzeit.** Ein Zeitvergleich wäre auf einer Build-Maschine
> unzuverlässig und der Test damit schlimmer als keiner.

---

## 4. Sperre und Begrenzung — zwei getrennte Mechanismen

| | Kontosperre | IP-Begrenzung |
|---|---|---|
| Wo | `app_user.failed_attempts` / `locked_until` | ausschließlich Arbeitsspeicher (Caffeine) |
| Schwelle | 5 Fehlversuche | 20 Fehlversuche |
| Dauer | 15 Minuten | 15-Minuten-Fenster ab dem ersten Fehlversuch |
| Überlebt Neustart | ja | nein, bewusst |

**Die IP-Begrenzung schreibt niemals in die Datenbank.** Sonst wäre der Schutzmechanismus selbst der
Angriffsvektor: Jeder Bot erzeugte mit jedem Versuch Schreiblast auf einer Instanz, die wir uns mit
der EDI-Produktion teilen. Der Preis ist, dass sie bei einem Neustart und je Instanz von vorn
beginnt — die persistente Sperre an `app_user` ist der eigentliche Schutz des einzelnen Kontos.

Die IP-Grenze ist bewusst großzügiger als die Kontosperre: Hinter einer IP kann ein ganzes Büro
stehen (NAT). Der Speicher ist auf 10.000 Adressen begrenzt; ohne Obergrenze wäre die Tabelle selbst
der Angriffspunkt.

### Wann genau gesperrt wird

**Der fünfte Fehlversuch setzt `locked_until`.** Der sechste Versuch läuft dann gegen die Sperre —
beide Formulierungen beschreiben dasselbe Verhalten und beide Tests prüfen es.

Weitere Feinheiten, jede mit Grund:

- **Eine abgelaufene Sperre setzt den Zähler zurück.** Sonst genügte nach Ablauf ein einziger
  Fehlversuch, um sofort wieder fünfzehn Minuten zu sperren — die Frist wäre praktisch unbegrenzt.
- **Ein richtiges Passwort auf ein gesperrtes Konto erhöht den Zähler nicht.** Sonst verlängerte der
  berechtigte Nutzer seine eigene Sperre, indem er es noch einmal versucht.
- **Eine erfolgreiche Anmeldung setzt Zähler, Sperre und das IP-Kontingent zurück.**

---

## 5. Sitzung und Cookie

Serverseitige Sitzung über **Spring Session JDBC 4.1**, gebunden per `@SpringSessionDataSource` an
`monitorDataSource`. Die Sitzung liegt in `overlord_monitor.SPRING_SESSION` — sie muss sich jederzeit
serverseitig zurücknehmen lassen. Das ist der Grund, aus dem dieses Projekt **kein JWT** verwendet.

| Attribut | Wert | Warum |
|---|---|---|
| Name | `OVERLORD_SESSION` | gehört diesem Werkzeug, nicht irgendeinem Servlet-Container |
| `HttpOnly` | immer | kein Zugriff aus JavaScript |
| `SameSite` | `Lax` | |
| `Secure` | **im Profil `dev` aus, sonst an** | über `http://localhost` schickt der Browser ein `Secure`-Cookie nicht mit |
| Ablauf | 30 Minuten ohne Aktivität | |

`server.servlet.session.cookie.*` ist die **einzige** Quelle dieser Werte; Spring Boot baut daraus
den `CookieSerializer` von Spring Session, und `config/SecurityConfig` liest `…cookie.secure` für das
CSRF-Cookie aus derselben Stelle.

Zwei Tests sichern das ab:
`CookieAttributeTest` (ohne Datenbank) bindet `application.yml` je Profil und prüft, dass `Secure`
**nur** in `dev` fehlt — auch ein neues, unbekanntes Profil erbt die sichere Einstellung.
`AnmeldungDbIT` prüft am echten `Set-Cookie` des laufenden Servers Name, `HttpOnly` und `SameSite`.

> **Warum die Integrationstests einen echten Server starten und kein MockMvc verwenden:** Ohne
> eingebetteten Server konfiguriert Spring Boot den `CookieSerializer` nicht aus
> `server.servlet.session.cookie.*`. Das Cookie hieße dann `SESSION` statt `OVERLORD_SESSION`, und
> der Test bewiese etwas, das in Produktion anders läuft. Genau diese Art Abweichung soll ein Test
> finden und nicht verstecken.

### Sitzungs-ID und Sitzungsfestschreibung

Die Sitzungs-ID wird bei **jeder Rechteänderung** erneuert: beim Anmelden und nach der
Passwortänderung. Verwendet wird die Standardstrategie `ChangeSessionIdAuthenticationStrategy`.

Dabei zeigte sich ein Nebeneffekt der Konfiguration, der stärker ist als der Schutz selbst:
**Ein unangemeldeter Aufruf legt gar keine Sitzung an.** Das CSRF-Token liegt im Cookie, nicht in der
Sitzung; vor der Anmeldung existiert also nichts, was sich festschreiben ließe. Drei Tests halten das
fest — dass keine Sitzung entsteht, dass eine untergeschobene ID nicht übernommen wird, und dass eine
erneute Anmeldung auf laufender Sitzung die ID tauscht.

### `PRINCIPAL_NAME` ist der Benutzername

`AngemeldeterNutzer` implementiert `AuthenticatedPrincipal`. **Das ist nicht kosmetisch:** Ohne
`getName()` nimmt Spring Security `toString()` des Principals und schreibt es nach
`SPRING_SESSION.PRINCIPAL_NAME` (`VARCHAR(100)`). Die Darstellung eines Records ist dafür zu lang —
das Anmelden schlug mit `Data too long for column 'PRINCIPAL_NAME'` fehl, bevor das behoben war.

Zwei Folgen:

- Der Benutzername ist auf **100 Zeichen** begrenzt (`AdminUserService.MAX_BENUTZERNAME`), obwohl
  `app_user.username` 190 zulässt. Ein längerer Name ließe sich anlegen, könnte sich aber nie
  anmelden. Die Prüfung greift im Anlege-Endpunkt **und** im Bootstrap.
- Der Index auf `PRINCIPAL_NAME` bleibt brauchbar: Ab Schritt 9 lassen sich damit die Sitzungen
  eines gesperrten Nutzers gezielt verwerfen.

### CSRF

CSRF bleibt aktiv. `CookieCsrfTokenRepository.withHttpOnlyFalse()` mit dem einfachen
`CsrfTokenRequestAttributeHandler` — **nicht** dem `XorCsrfTokenRequestAttributeHandler`: Der erwartet
einen maskierten Wert und wiese den unverändert aus dem Cookie zurückgeschickten Token ab. Der BFF in
Teil 2 liest `XSRF-TOKEN` und schickt ihn als `X-XSRF-TOKEN`.

`security/CsrfCookieFilter` erzwingt, dass der Token je Anfrage tatsächlich entsteht. Spring Security
erzeugt ihn seit Version 6 verzögert; bei einer reinen JSON-API liest ihn niemand, das Cookie würde
nie geschrieben, und die erste schreibende Anfrage scheiterte mit `403`. Der Filter hängt **vor** der
Autorisierung, damit das Cookie auch bei einer `401`-Antwort gesetzt wird — genau das braucht der
Anmeldevorgang: Ein unangemeldeter `GET` holt das Token, danach geht `POST /api/auth/login`.

---

## 6. Passwortwechsel

Jedes Konto startet mit `must_change_password = TRUE`. **Ohne diesen Pfad wäre kein Konto nutzbar** —
Annahme A3 schließt eine Selbstbedienung per E-Mail aus.

Solange das Flag steht, antwortet **jeder Endpunkt außer** `/api/auth/me`, `/api/auth/password` und
`/api/auth/logout` mit `403` und Problemtyp `passwortwechsel-erforderlich`. Durchgesetzt von
`security/PasswortwechselInterceptor`.

> Bewusst ein `HandlerInterceptor` und kein Servlet-Filter: Eine hier geworfene Ausnahme läuft durch
> den `@RestControllerAdvice` und wird dort — an der **einzigen** Stelle, die Ausnahmen übersetzt —
> zu `problem+json`. Ein Filter müsste die Antwort selbst schreiben und das Format ein zweites Mal
> nachbauen.

Regeln für das neue Passwort: **mindestens zwölf Zeichen**, keine weiteren
Zusammensetzungsregeln, und es darf nicht dem alten entsprechen. Erzwungene Sonderzeichen erhöhen die
Entropie kaum und erzeugen vorhersagbare Muster; Länge wirkt.

Nach erfolgreicher Änderung: Flag zurückgesetzt, Sitzungs-ID erneuert, Eintrag im `audit_log`.

---

## 7. Bootstrap des ersten Kontos

`admin/BootstrapAdminRunner`, aktiv **nur** im Profil `bootstrap`:

```bash
SPRING_PROFILES_ACTIVE=dev,bootstrap \
OVERLORD_BOOTSTRAP_ADMIN_USER=… OVERLORD_BOOTSTRAP_ADMIN_PASSWORD=… \
./mvnw spring-boot:run
```

- **Legt nur an, wenn noch kein Konto mit Rolle `ADMIN` existiert.** Existiert eines, passiert nichts
  und es wird eine Meldung ausgegeben. Ein bestehendes Konto wird niemals überschrieben und kein
  Passwort zurückgesetzt — sonst wäre ein versehentlich mitgegebenes Profil ein Weg, sich Zugang zu
  verschaffen.
- Das Konto ist aktiv, hat Rolle `ADMIN` und `must_change_password = TRUE`. Die Umgebungsvariable
  transportiert damit nur ein **Einmalpasswort**; nach der ersten Anmeldung ist ihr Inhalt wertlos.
- **Das Passwort erscheint nirgends im Log** — weder im Klartext noch als Hash, weder bei Erfolg noch
  im Fehlerfall. Fehlermeldungen nennen ausschließlich die **Namen** der Variablen.
- Eintrag ins `audit_log` (`NUTZER_BOOTSTRAP`).

---

## 8. Nutzer anlegen statt migrieren

`POST /api/admin/users`, ausschließlich für `ADMIN`. Entgegengenommen werden Benutzername, Rolle,
eine Mandanten-ID und ein Einmalpasswort.

```json
{ "username": "…", "role": "MANDANT", "mandantId": "VOTG", "initialPassword": "…" }
```

| Fall | Antwort |
|---|---|
| Erfolg | `201` mit `id`, `username`, `role`, `mandantId` — **niemals** dem Passwort |
| Unbekannte Mandanten-ID | `404` |
| Benutzername vergeben | `409`, überschreibt nie |
| Unbekannte Rolle | `400` |
| Als `MANDANT` aufgerufen | `403` |

Das angelegte Konto ist aktiv und hat `must_change_password = TRUE`. Die Rolle `ADMIN` darf vergeben
werden. Die Mandantenzuordnung wird **auch für ADMIN** gespeichert: Sie hat dort keine Wirkung auf die
wählbare Menge, fällt aber bei einer späteren Herabstufung auf `MANDANT` nicht ins Leere — und eine
stillschweigend verworfene Eingabe wäre schlechter als eine wirkungslose Zeile.

Der Endpunkt nimmt eine Mandanten-ID entgegen und ist damit die zweite der genau zwei Ausnahmen von
Regel M1; beide sind in [`mandantentrennung.md`](mandantentrennung.md) namentlich geführt.

**Nicht in diesem Schritt:** Auflisten, Sperren, Rollenwechsel, Zurücksetzen durch den Admin,
Löschen. Das ist Schritt 9.

### Warum die Altnutzer nicht übernommen werden

Ursprünglich war ein Migrationslauf über `GlassfishDB.User` vorgesehen. Er entfällt **ersatzlos**:

- Die Tabelle bleibt dauerhaft lesbar. Eine Übernahme wäre nur ein vorgezogener `SELECT` und keine
  einmalige Gelegenheit.
- Alle 36 Konten wären gesperrt und ohne Passwort gestartet und hätten einzeln freigeschaltet werden
  müssen — derselbe Aufwand wie neu anlegen, nur mit toten Zeilen als Zwischenschritt.
- Die Rolle `Admin` bedeutet dort etwas anderes: Im Altsystem trägt jeder Nutzer eine `MandantID`,
  auch die zehn Admins; die Rolle war innerhalb eines Mandanten gedacht. Eine wörtliche Übernahme
  hätte zehn überwiegend externe Konten quer über genau die Grenze gehoben, die dieses Projekt
  schützt.

**Aus `GlassfishDB.User` wird nichts gelesen.** Insbesondere wird `UserPassword` (`varchar(20)`,
Klartext) nirgends angefasst — diese Passwörter gelten als kompromittiert.

Die Tabelle bleibt als Nachschlagequelle: `UserID` (Benutzername und Schlüssel), `MandantID`,
`UserRole` mit den Werten `Admin` (10 Zeilen) und `User` (26 Zeilen).

---

## 9. Protokollierung

Die Whitelist `audit/AuditEventType` ist um drei Arten gewachsen. **Die Namen bleiben deutsch:** Fünf
Arten sind seit Schritt 2 vergeben und stehen bereits als Text in `audit_log.event_type`; eine
Umbenennung machte vorhandene Zeilen unlesbar, ohne etwas zu gewinnen.

| Konstante | Bezeichnung in der Aufgabenstellung | seit |
|---|---|---|
| `ANMELDUNG_ERFOLG` | `LOGIN_SUCCESS` | Schritt 2 |
| `ANMELDUNG_FEHLVERSUCH` | `LOGIN_FAILED` | Schritt 2 |
| `ABMELDUNG` | `LOGOUT` | Schritt 2 |
| `KONTO_GESPERRT` | `ACCOUNT_LOCKED` | Schritt 2 |
| `PASSWORT_GEAENDERT` | `PASSWORD_CHANGED` | Schritt 2 |
| `MANDANT_GEWECHSELT` | `MANDANT_SWITCHED` | **Schritt 3** |
| `NUTZER_BOOTSTRAP` | `USER_BOOTSTRAPPED` | **Schritt 3** |
| `NUTZER_ANGELEGT` | `USER_CREATED` | **Schritt 3** |

**Ein Fehlversuch auf einen unbekannten Namen wird mit `actor_user_id = NULL` und gesetztem
`actor_username` protokolliert** — genau das ist der interessanteste Eintrag der Tabelle. Ein Test
prüft das.

**Passwörter erscheinen niemals** im Protokoll, im Log oder in einer Fehlermeldung. Der
`NUTZER_ANGELEGT`-Eintrag trägt Name, Rolle und Mandant — sonst nichts.

---

## 10. Was ein Aufrufer tun muss

Für Teil 2 und für jeden anderen Client, in dieser Reihenfolge:

1. Irgendein `GET` (etwa `/api/auth/me`) — die Antwort ist `401`, setzt aber das Cookie `XSRF-TOKEN`.
2. `POST /api/auth/login` mit `X-XSRF-TOKEN` aus diesem Cookie.
3. Ist `mustChangePassword` gesetzt: `POST /api/auth/password`, sonst weiter.
4. Ist `mandant` `null`: `GET /api/mandanten`, dann `POST /api/auth/mandant`.
5. Fachliche Endpunkte (ab Schritt 4).

Alle folgenden Aufrufe schicken das Sitzungs-Cookie mit; schreibende zusätzlich den CSRF-Header.

---

## 11. Messungen (Regel L7)

Gemessen am 29.07.2026 gegen die Testkopie, beste von fünf Läufen nach einem Aufwärmlauf:

| Abfrage | Zugriffspfad (`EXPLAIN`) | Laufzeit |
|---|---|---|
| `AppUserRepository.findeNachBenutzername` | `type=const`, `key=uq_app_user_username` | ~0,5 ms |
| `MandantRepository.existiert` | `type=const`, `key=PRIMARY`, `Using index` | ~0,4 ms |

Die beiden Abfragen der Mandantenmenge stehen in
[`mandantentrennung.md`](mandantentrennung.md) §6.

> Der Vollständigkeit halber: Der teuerste Teil einer Anmeldung ist **kein** SQL, sondern der
> BCrypt-Vergleich mit Kostenfaktor 12 — rund 200 ms, und das ist der Zweck.

---

## 12. Offene Punkte

- **Ein deaktiviertes oder gesperrtes Konto verliert seine laufende Sitzung nicht automatisch.** Das
  Principal liegt in der Sitzung und wird nicht je Anfrage neu geladen. Ab Schritt 9 (Sperren durch
  den Admin) müssen die Sitzungen des Nutzers gezielt verworfen werden — über
  `SPRING_SESSION.PRINCIPAL_NAME`, siehe §5.
- **Zurücksetzen eines Passworts durch den Admin** fehlt (Schritt 9). Bis dahin ist ein vergessenes
  Passwort nur über ein neues Konto zu lösen.
- **Die IP-Begrenzung gilt je Instanz.** Bei mehreren Instanzen hinter einem Reverse Proxy
  vervielfacht sich das Kontingent. Solange nur eine Instanz läuft, ist das ohne Wirkung.
