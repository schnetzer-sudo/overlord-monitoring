# Benutzerverwaltung — der Bau (Backend)

Stand: 21.08.2026, nachgetragen am 24.08.2026 (`lockedUntil`, §4) · Schritt 9a, Teil Backend
Vorgabe: [`benutzerverwaltung.md`](benutzerverwaltung.md) (E1–E18, dazu **E20** vom 21.08.2026) ·
Grundlage: [`authentifizierung.md`](authentifizierung.md) und
[`mandantentrennung.md`](mandantentrennung.md)
Messungen: [`messungen-schritt9.md`](messungen-schritt9.md), **M81** und **M82**

**Dieser Text beschreibt ausschließlich das Backend.** Die Oberfläche zu 9a ist ein eigener Auftrag
mit eigener Datei.

---

## 1. Was entstanden ist

| | |
|---|---|
| Migration | `V7__benutzerverwaltung.sql` — `download_allowed` fällt, `locked_by_admin` kommt |
| Endpunkte | sechs unter `/api/admin/users`, alle nur für `ADMIN` |
| Ereignisarten | sieben neue in `AuditEventType` |
| Dienst | `security/Sitzungsentzug` — **Bauform A**, über `SPRING_SESSION.PRINCIPAL_NAME` |
| Geändert | `POST /api/auth/password` verwirft jetzt die *übrigen* Sitzungen (E6) |
| Geändert | `GET /api/auth/me` verliert das Feld `downloadAllowed` — **Vertragsbruch aus Schritt 3**, datiert vermerkt |
| Nachgetragen *(24.08.2026)* | `lockedUntil` in der Kontenzeile — **eine umgekehrte Entscheidung** (E20), begründet im Korrekturkasten in §4 |

Neue Klassen: `admin/BenutzerverwaltungController`, `admin/BenutzerverwaltungService`,
`admin/NutzerzeileResponse`, `security/KontoZeile`, `security/Sitzungsentzug`.
`admin/AdminUserService` bleibt beim Anlegen (E4) und gibt nur eine Strecke ab:
`kodiereEinmalpasswort`.

---

## 2. Die Migration `V7`

```sql
ALTER TABLE app_user
  DROP COLUMN download_allowed,
  ADD COLUMN locked_by_admin BOOLEAN NOT NULL DEFAULT FALSE AFTER must_change_password;
```

### 2.1 `download_allowed` fällt (E18)

Die Spalte war seit Schritt 3 tot: [`rohdaten.md`](rohdaten.md) §3 E2 schließt eine zweite
Berechtigungsstufe aus, die Endpunkte aus Schritt 8 haben sie nie geprüft, und `GET /api/auth/me`
gab sie trotzdem aus. Mit ihr entfallen `AppUserZeile.downloadErlaubt`,
`AngemeldeterNutzer.downloadAllowed` und `SelbstauskunftResponse.downloadAllowed`.

**Geprüft, bevor entfernt wurde: Das Frontend liest das Feld nicht.** Es steht dort an genau einer
Stelle — als Typzeile in `frontend/src/features/sitzung/api.ts` — und wird nirgends gelesen, in
keinem Objektliteral aufgebaut und in keiner Bedingung verwendet. Der Download hängt am Inhalt des
Artefakts, nicht an einem Nutzerflag. **Die Typzeile bleibt vorerst stehen** und ist im Frontendteil
zu streichen; dieser Auftrag ist Backend-only.

> **Laufende Sitzungen überleben den Wegfall — nachgemessen, nachdem hier das Gegenteil stand.**
> `AngemeldeterNutzer` ist `Serializable` und liegt serialisiert in `SPRING_SESSION_ATTRIBUTES`.
> Die naheliegende Sorge — eine wegfallende Record-Komponente ändere die `serialVersionUID` und
> mache alte Sitzungen unlesbar — ist **falsch**, und zwar an beiden Gliedern der Kette:
>
> ```
> serialVersionUID (5 Komponenten) = 0
> serialVersionUID (4 Komponenten) = 0
> GELESEN: Nutzer[id=7, username=lukas, rolle=ADMIN, mustChange=false]
> ```
>
> Die `serialVersionUID` eines Record ist `0` und hängt **nicht** an seinen Komponenten; ein mit
> fünf Komponenten geschriebener Strom liest sich in denselben Record mit vier Komponenten sauber
> ein, das überzählige Feld wird verworfen. Gemessen am 21.08.2026 mit zwei Fassungen derselben
> Klasse unter demselben JDK. **`SPRING_SESSION` muss also nicht geleert werden.**
>
> **Der Kommentar in `V7__benutzerverwaltung.sql` trägt die widerlegte Fassung weiter, und das ist
> Absicht:** Die Migration ist aufgezogen, und Flyway prüft ihre Prüfsumme. Schon eine geänderte
> Kommentarzeile ergibt beim nächsten Start `Validate failed — Migration checksum mismatch for
> migration version 7`; das Backend startet dann nicht mehr. Eine aufgezogene Migration ist
> eingefroren. Die richtige Fassung steht im Javadoc von `AngemeldeterNutzer` und hier.

### 2.2 `locked_by_admin` kommt — und warum nicht `locked_until`

**Keine der drei verbindlichen Dateien sagt, wo die administrative Sperre gespeichert wird.** E14
unterscheidet nur die *Ereignisnamen*. Die Lücke ist am 21.08.2026 gemeldet und entschieden worden:
eigene Spalte.

`locked_until` mitzubenutzen bricht am Bestand, und zwar **nachweisbar an drei Stellen**:

| | |
|---|---|
| 1 | `AnmeldeService` reicht bei **jedem** Fehlversuch einen Wert an `AppUserRepository.setzeFehlversuche`, und die schreibt `LOCKED_UNTIL` **bedingungslos** — mit `null`, solange der Zähler unter fünf steht. **Ein einziger falscher Anmeldeversuch löschte damit die Sperre.** Der Schutzmechanismus wäre der Weg an ihm vorbei |
| 2 | `merkeAnmeldung` setzt `LOCKED_UNTIL` bei jeder erfolgreichen Anmeldung auf `null`. Für eine Zeitsperre richtig, für einen Verwaltungsakt falsch |
| 3 | Eine Admin-Sperre hat kein „bis". Sie bräuchte ein Sentineldatum, und `ENTSPERRT_DURCH_ADMIN` wäre anschließend von einem regulären Ablauf nicht mehr zu unterscheiden |

**Das ist E14 auf Datenebene fortgeschrieben:** Ein Angriff (`KONTO_GESPERRT`, fünf Fehlversuche,
fünfzehn Minuten) und ein Verwaltungsakt (`SPERRE_DURCH_ADMIN`, unbefristet) gehören nicht in
dieselbe Zeile — und auch nicht in dieselbe Spalte.

`BOOLEAN` und nicht `DATETIME`: Die Sperre kennt keine Frist. Wann gesperrt wurde und von wem, steht
im `audit_log`, wo es hingehört.

**Entsperren räumt zusätzlich die Zeitsperre ab** (`locked_until = NULL`, `failed_attempts = 0`).
Das ist kein Beiwerk, sondern der Supportfall, um dessentwillen der Endpunkt existiert: Wer anruft,
ist nach fünf Fehlversuchen ausgesperrt und nicht durch einen Verwaltungsakt. Ein Entsperren, das
den häufigeren der beiden Zustände stehen ließe, wäre eine Falle.

### 2.3 Die Anmeldung kennt jetzt zwei Sperren

`AnmeldeService` prüft die administrative **vor** der Zeitsperre. Stünde sie dahinter, bekäme ein
zugleich zeitgesperrtes Konto fünfzehn Minuten lang „versuche es später erneut" und danach die
richtige Auskunft — zwei Meldungen für denselben Zustand.

Der neue Fall bekommt einen **eigenen Problemtyp und einen eigenen Text**:

| Fall | Antwort | Problemtyp | Titel |
|---|---|---|---|
| Passwort korrekt, **administrativ** gesperrt | `401` | `konto-administrativ-gesperrt` | „Konto gesperrt" |
| Passwort korrekt, **zeitgesperrt** (fünf Fehlversuche) | `401` | `konto-gesperrt` | „Konto gesperrt" |

Der bestehende Text sagt wörtlich „nach mehreren Fehlversuchen"; bei einem Verwaltungsakt wäre das
eine falsche Auskunft. **Die Auskunftsdisziplin bleibt unberührt** — beide Fälle werden erst
erreicht, wenn das Passwort stimmt.

---

## 3. Der Sitzungsentzug — Bauform A

**E7 ist gefahren, bevor eine Zeile geschrieben wurde** (M81). Alle vier Punkte halten am laufenden
System, also gilt Bauform A: `findByPrincipalName(username)`, dann jede gefundene Sitzung löschen.
**Kein Generationszähler, keine Abfrage je Anfrage.**

**Der Entzug läuft immer erst, nachdem die Änderung festgeschrieben ist.** Die Reihenfolge ist am
21.08.2026 umgedreht worden, nachdem eine adversarische Nachprüfung das Fenster gefunden hat:

> Lief der Entzug *innerhalb* der offenen Transaktion, verwarf er die Sitzungen sofort — Spring
> Sessions `deleteById` läuft über eine eigene Transaktion mit `PROPAGATION_REQUIRES_NEW` und
> committet unabhängig —, während die Sperre selbst noch **uncommitted** war. In diesem Fenster
> sieht die Anmeldung auf einer anderen Verbindung weiterhin `locked_by_admin = 0`: **Der eben
> ausgesperrte Nutzer konnte sich neu anmelden und bekam eine Sitzung, die kein Entzug mehr traf**
> — sie entstand nach ihm. Auf der Testkopie dauert ein `COMMIT` zehn bis fünfundzwanzig Sekunden;
> das Fenster war keine theoretische Größe.
>
> Jetzt ist es umgekehrt: Wer sich zwischen Festschreiben und Entzug anmeldet, wird bereits
> abgewiesen. Übrig bleibt, dass eine *bestehende* Sitzung einen Wimpernschlag länger lebt — und
> die fällt unmittelbar danach.

Deshalb trägt **keine** der fünf Dienstmethoden `@Transactional`. Vier sind ein einzelnes `UPDATE`
und damit von selbst atomar; der fünfte — die Mandantenmenge — ist `DELETE` plus `INSERT`s und
trägt seine Transaktion in `AppUserRepository.ersetzeMandanten`, wo sie geschlossen ist, bevor der
Entzug beginnt.

`security/Sitzungsentzug` hat genau zwei Methoden:

| | |
|---|---|
| `verwirfAlle(username)` | alle Sitzungen des Kontos. Die fünf schreibenden Vorgänge (E5) |
| `verwirfUebrige(username, behalteSitzungsId)` | alle außer einer. Die eigene Passwortänderung (E6) |

**Die Reihenfolge in `AuthController.passwortAendern` ist der Punkt, an dem E6 kippen kann.** Die
Sitzungs-ID wird gelesen, **bevor** `SitzungsVerwaltung.erneuere` sie austauscht. Gliche man nach
der Erneuerung gegen die *alte* ID ab, verwürfe der Entzug genau die eben entstandene neue — und der
Nutzer wäre nach seiner Passwortänderung abgemeldet.

Die Erneuerung der Sitzungs-ID bleibt zusätzlich bestehen: Sie schützt vor Sitzungsfestschreibung,
der Entzug vor einer fremden, längst laufenden Sitzung. **Zwei Angriffe, zwei Maßnahmen.**

> **Der Entzug gilt auch für den Handelnden.** E5 ist *eine Regel, keine Fallunterscheidung*: Wer
> einen der fünf Vorgänge auf sein **eigenes** Konto anwendet — sich entsperren, seine eigene
> Mandantenmenge ändern, sein eigenes Passwort zurücksetzen —, verwirft dabei auch seine eigene
> Sitzung und muss sich neu anmelden. Eine Ausnahme für den Handelnden wäre genau die
> Fallunterscheidung, die E5 vermeidet, und sie wäre die eine, an die sich beim sechsten Vorgang
> niemand erinnert. Für das **eigene Passwort** gibt es deshalb `POST /api/auth/password` — der
> behält die aktuelle Sitzung.

**Die Zahl verworfener Sitzungen steht im `detail` des auslösenden Ereignisses** und bekommt keine
eigene Ereignisart (E15). Das ist nicht Kosmetik: `findByIndexNameAndIndexValue` liefert bei einem
unbekannten Indexnamen eine *leere Map* statt einer Ausnahme — ein reihenweise wirkungsloser Entzug
sähe ohne diese Zahl aus wie einer, der nichts zu tun hatte.

---

## 4. Die sechs Endpunkte

Alle unter `/api/admin/users`, alle nur `ADMIN`, Antwortfelder englisch camelCase, Fehlertexte
deutsch.

| Methode | Pfad | Rumpf | Ereignisart |
|---|---|---|---|
| `GET` | `/api/admin/users` | — | — |
| `PUT` | `/api/admin/users/{id}/lock` | `{"locked": true}` | `SPERRE_DURCH_ADMIN` / `ENTSPERRT_DURCH_ADMIN` |
| `PUT` | `/api/admin/users/{id}/active` | `{"active": false}` | `NUTZER_DEAKTIVIERT` / `NUTZER_REAKTIVIERT` |
| `PUT` | `/api/admin/users/{id}/role` | `{"role": "MANDANT"}` | `ROLLE_GEAENDERT` |
| `PUT` | `/api/admin/users/{id}/tenants` | `{"tenants": ["VOTG","SUTTONS"]}` | `MANDANTEN_GEAENDERT` |
| `POST` | `/api/admin/users/{id}/password` | `{"initialPassword": "…"}` | `PASSWORT_ZURUECKGESETZT` |

**Jeder der fünf schreibenden Endpunkte antwortet mit derselben Zeile wie die Liste** — so wie
`SelbstauskunftResponse` nach Anmeldung, Passwortänderung und Mandantenwechsel dieselbe bleibt. Der
Aufrufer soll seinen Zustand nie aus mehreren Antworten zusammensetzen müssen.

### Die Zeile

```json
{
  "id": 42,
  "username": "beispielnutzer",
  "role": "MANDANT",
  "tenants": ["SUTTONS", "VOTG"],
  "locked": false,
  "lockedUntil": "2026-08-24T12:14:30Z",
  "active": true,
  "mustChangePassword": false,
  "lastLogin": "2026-08-20T06:01:10.374Z"
}
```

- `tenants` sind **nur die Kennungen**, aufsteigend sortiert — keine Anzeigenamen. Die lägen in
  `GlassfishDB.Mandant` und kosteten einen schemaübergreifenden Join je Zeile, während die Kennung
  selbst der sprechende Code ist, den auch `POST /api/admin/users` entgegennimmt.
- `locked` ist die **administrative** Sperre, die `PUT /api/admin/users/{id}/lock` umschaltet —
  unbefristet, aufgehoben nur durch einen zweiten Verwaltungsakt.
- `lockedUntil` ist das Ende der **automatischen** Sperre nach fünf Fehlversuchen, und es steht
  **nur dann im Rumpf, wenn es in der Zukunft liegt**; sonst fehlt das Feld nicht, sondern ist
  `null`. Die beiden werden **nie verrechnet** — siehe den Korrekturkasten unten.
- `lastLogin` ist `null`, wenn sich das Konto **nie** angemeldet hat — bei über zwanzig externen
  Nutzern die häufigste Supportfrage (E17).

> ### ⚠️ `lockedUntil` — nachgetragen am 24.08.2026, und das ist eine Umkehr
>
> **Bis zum 24.08.2026 trug die Zeile acht Felder, und `lockedUntil` fehlte mit Absicht.** Der alte
> Wortlaut stand an dieser Stelle und lautete:
>
> > *„`locked` ist die **administrative** Sperre, nicht die automatische nach fünf Fehlversuchen:
> > Die läuft nach fünfzehn Minuten von selbst ab, und ein Zustand, der beim Hinsehen schon wieder
> > anders ist, gehört nicht in eine Verwaltungsliste."*
>
> **Was hier galt und was nicht.** Dieselbe Sache stand in diesem Dokument zweimal und einmal davon
> falsch: als **Entscheidung** hier und als **offener Punkt 5** in §9 — *„Die automatische
> Zeitsperre ist in der Liste nicht sichtbar … Wer wissen will, warum sich jemand gerade nicht
> anmelden kann, sieht es der Liste nicht an."* Beides zugleich geht nicht. Verbindlich ist
> [`benutzerverwaltung.md`](benutzerverwaltung.md) **E20** (21.08.2026), und die trägt die Umkehr
> ausdrücklich; die Entscheidung hier war ihr gegenüber die unterlegene Stelle und ist es, die
> korrigiert wird. Offener Punkt 5 ist damit geschlossen.
>
> **Der alte Einwand bleibt richtig — er trifft nur nicht, was gebaut ist.** Er richtet sich gegen
> einen *Zustand*, der als „gesperrt“ in der Liste steht und beim nächsten Blick verschwunden ist.
> Was hier steht, ist kein Zustand, sondern ein **Zeitpunkt**: nicht „gesperrt“, sondern *bis
> wann*. Genau dafür trägt E20 die Bedingung „nur wenn in der Zukunft“ — ein abgelaufener Wert wird
> gar nicht erst übertragen, statt als abgelaufene Sperre erklärt werden zu müssen.
>
> **Gebraucht wird er für den Fall, für den 9a überhaupt gebaut wird** (E1): Ein Admin, den einer
> von über zwanzig externen Nutzern anruft, weil er nicht hineinkommt, muss zwischen „ich habe dich
> gesperrt“ und „du hast dich fünfmal vertippt“ unterscheiden können. Ohne das Feld sieht er
> `locked: false` und hat keine Erklärung.
>
> **Was es gekostet hat:** `APP_USER.LOCKED_UNTIL` wird in `AppUserRepository.konten` mitgelesen —
> dieselbe Zeile, dieselbe Tabelle, kein zusätzlicher Join und keine zweite Abfrage —, `KontoZeile`
> trägt `gesperrtBisUtc` roh aus der Spalte, und `NutzerzeileResponse.fuer` bekommt den
> Vergleichszeitpunkt als Parameter.
>
> **Die Prüfung „läuft die Sperre noch“ rechnet mit der Systemuhr und nicht im Browser.** Ob eine
> Sperre noch gilt, ist sicherheitsnahe Zeit ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md)
> §7, `common/ZeitConfig`); ein verstellter Rechner sähe sonst eine abgelaufene Sperre als laufende
> oder umgekehrt. Die Liste liest die Uhr **einmal für alle Zeilen** — sonst entschiede bei einer
> Sperre, die während des Zusammenbauens abläuft, die Position der Zeile darüber, ob sie noch als
> laufend gilt.
>
> **Der Grenzfall ist entschieden:** Ein Wert, der *genau jetzt* abläuft, gilt als abgelaufen.
> „Nur wenn es in der Zukunft liegt“ wörtlich genommen.
>
> **Zwei Stellen bleiben unberührt, und beide mit Grund.** `V7__benutzerverwaltung.sql` trägt seine
> Begründung gegen eine *gemeinsame Spalte* (§2.2) — die ist von dieser Umkehr gar nicht betroffen
> und stünde ohnehin unter der eingefrorenen Prüfsumme. Und `KontoZeile.istNutzbarerAdmin` zählt
> die Zeitsperre weiterhin **nicht** mit: Eine Herabstufung für eine Viertelstunde zu verweigern,
> weil jemand sich vertippt hat, wäre eine Regel, die von der Uhr abhängt (§5).

### Die Statuscodes

**Keine der verbindlichen Dateien legt sie fest.** Die folgende Zuordnung ist aus dem Bestand
fortgeschrieben und hier begründet, damit sie prüfbar ist statt geraten:

| Fall | Status | Problemtyp | Woher |
|---|---|---|---|
| unbekannte Konto-ID | `404` | `nicht-gefunden` | wie überall (Regel M3) |
| unbekannte Mandanten-ID | `404` | `nicht-gefunden` | wie `POST /api/admin/users`: ein ADMIN kennt die Mandantenliste ohnehin |
| unbekannte Rolle | `400` | `unbekannte-rolle` | **bestehender** Typ aus Schritt 3 |
| Passwort zu kurz | `400` | `passwort-zu-kurz` | **bestehender** Typ aus Schritt 3 |
| Passwort gleich dem aktuellen | `400` | `passwort-unveraendert` | **bestehender** Typ aus Schritt 3 |
| Selbstschutz (E12, erste Stufe) | `409` | `selbstschutz` | neu |
| letzter nutzbarer ADMIN (E12, zweite Stufe) | `409` | `letzter-admin` | neu |
| letzte Mandantenzuordnung (E10) | `409` | `letzte-mandantenzuordnung` | neu |
| Herabstufung ohne Zuordnung (E11) | `409` | `rolle-ohne-mandant` | neu |
| MANDANT-Nutzer auf einem dieser Endpunkte | `403` | `zugriff-verweigert` | `SecurityConfig` |

**`409` und nicht `403` für die vier Zustandsfälle.** `403` bedeutet in diesem Projekt „die Rolle
reicht nicht" und wäre von der Rollengrenze nicht zu unterscheiden. Bei den vier Fällen ist die
*Eingabe* in Ordnung und der *Zustand* verbietet sie — das ist ein Konflikt, kein Rechteproblem. Die
drei bestehenden Problemtypen werden **wiederverwendet**, nicht neu erfunden: Zwei Namen für
dieselbe Sache sind die Art Abweichung, die erst im Frontend auffällt.

### `GET /api/admin/users`

**Alle Konten, mandantenfrei** (E2). Keine Paginierung, keine serverseitige Suche (E16) —
Größenordnung dreißig Konten, jede Mechanik dafür wäre Beiwerk.

Sortiert nach Benutzername. Er ist eindeutig, damit ist die Reihenfolge auch ohne Paginierung
stabil.

**`lastLogin` kommt aus dem `audit_log`** und steht **im selben Statement** wie die Kontenzeile, als
abgeleitete Tabelle. Zwei Gründe, beide zwingend:

1. **Gruppiert wird über `actor_user_id`, nicht über `actor_username`.** `AnmeldeService`
   protokolliert den Namen so, *wie er eingetippt wurde*, und `app_user.username` vergleicht ohne
   Rücksicht auf Groß- und Kleinschreibung. Eine Zuordnung über den Namen im Anwendungscode träfe
   genau die Konten nicht, deren Nutzer sich mit abweichender Schreibweise anmelden.
2. **Eine abgeleitete Tabelle statt einer korrelierten Unterabfrage:** Sie liest die Anmeldezeilen
   einmal statt einmal je Konto.

Regel L2 ist nicht berührt — sie verbietet Live-Aggregation über `Message` im Quellschema;
`audit_log` ist unser eigenes und um Größenordnungen kleiner. **Gemessen in M82: 17,87 ms.**

> **Die Einzelabfrage trägt diese Kosten nicht.** Jeder schreibende Vorgang liest die Zeile zweimal
> — einmal vor der Änderung (für `pruefeEntwertung` und den Protokolltext) und einmal danach für
> die Antwort. Bis zum 21.08.2026 zog jeder dieser Lesevorgänge dieselbe volle Aggregation über
> `audit_log` und **alle** Zeilen aus `app_user_mandant`, um das Ergebnis anschließend bis auf ein
> Konto wegzuwerfen: zweimal 17,9 ms je Schreibvorgang, für eine Zeile. Seit der Nachprüfung steht
> die Einschränkung auf das eine Konto **in** der abgeleiteten Tabelle, und die Mandanten kommen
> aus `mandantenVon(id)` statt aus dem Gesamtbestand.

### `PUT /api/admin/users/{id}/tenants` — die dritte M1-Ausnahme

Nimmt Mandanten-IDs entgegen und ist damit die dritte und derzeit letzte Ausnahme von Regel M1. Sie
ist zulässig aus demselben Grund wie die beiden anderen: **Hier wird eine Berechtigung *definiert*
und kein Datenausschnitt *abgefragt*.** Geprüft wird über `MandantRepository.existiert` — seit
Schritt 3 als `@OhneMandantenkontext` geführt; die Begründung dort ist um diesen zweiten Aufrufer
ergänzt, **eine neue Annotation gibt es nicht**.

Die Menge wird **vollständig ersetzt**, nicht ergänzt oder abgezogen: Bei dreißig Konten und zehn
Mandanten ist die Menge winzig, und eine Mengenersetzung hat genau ein Ergebnis, während eine
Differenzbildung zwei Fehlerarten hat. Doppelte Kennungen in der Eingabe werden still
zusammengezogen — zweimal dieselbe ID ist keine Eingabe, die einen Fehler wert wäre, aber der
Primärschlüssel von `app_user_mandant` verschluckte sich daran.

**Die letzte Zuordnung lässt sich nicht entfernen — für beide Rollen** (E10). Schritt 3 speichert
sie auch für ADMIN, „damit sie bei einer späteren Herabstufung nicht ins Leere fällt"; dürfte man
sie dort entfernen, entstünde beim nächsten Rollenwechsel genau der Zustand, den E11 verbietet.

> **Die Pfadschreibweise ist am 21.08.2026 entschieden worden.** `benutzerverwaltung.md` §5 führte
> `{id}/tenants` und begründet die englische Fassung ausdrücklich; `mandantentrennung.md` §3,
> `authentifizierung.md` §8 und `PROJEKTBESCHREIBUNG.md` §7 führten `{benutzername}/mandanten`.
> **Gebaut ist `{id}/tenants`**, weil die vier Nachbarendpunkte alle `{id}` tragen und ein deutscher
> Unterpfad unter einer englischen Sammlung schlechter wäre als beide reinen Varianten. Die drei
> anderen Dateien sind datiert nachgezogen.

### `POST /api/admin/users/{id}/password`

**Der Admin tippt es** (E13), mindestens zwölf Zeichen, und es darf nicht dem aktuellen entsprechen
— geprüft per BCrypt-Vergleich gegen den gespeicherten Hash. Das Konto bekommt Änderungszwang.

**Ein Codepfad gemeinsam mit dem Anlegen:** `AdminUserService.kodiereEinmalpasswort` prüft die Länge
und kodiert; beide Vorgänge gehen hindurch. Der Aufrufer trägt danach nur noch, was ihn
unterscheidet — beim Anlegen gibt es keinen gespeicherten Hash, gegen den sich vergleichen ließe.
`AppUserRepository.setzePasswort` hat dafür den Änderungszwang als **Parameter** bekommen statt fest
verdrahtetem `false`; dieselbe Bauform wie `legeAn`.

**Steht nicht unter Selbstschutz.** Für das eigene Passwort gibt es `POST /api/auth/password`. Wer
diesen Endpunkt auf sein eigenes Konto anwendet, wirft sich zwar aus allen Sitzungen (E5), sperrt
sich aber nicht aus — er kennt das eben getippte Passwort.

**In keiner Protokollzeile steht jemals ein Passwort**, auch nicht gehasht, auch nicht abgekürzt.

---

## 5. Der Selbstschutz (E12) — und warum die Reihenfolge Absicht ist

Zwei Stufen, geprüft bei **sperren**, **deaktivieren** und **herabstufen** — und nur in der
entwertenden Richtung. Sich selbst zu *entsperren* oder zu *reaktivieren* ist wirkungslos, aber kein
Verstoß; ein Verbot darauf wäre Lärm.

**Geprüft wird zuerst der letzte nutzbare Administrator, danach das eigene Konto.** Beide Fälle
überschneiden sich fast immer: Wer als einziger nutzbarer Admin angemeldet ist, trifft mit jedem
entwertenden Vorgang auf sich selbst. Die Meldung „das ist der letzte nutzbare Administrator" sagt
dann mehr als „du kannst dich nicht selbst sperren" — sie nennt die Bedingung, unter der es ginge.
Stünde der Selbstschutz vorn, wäre die zweite Stufe praktisch unerreichbar und damit ungetestet.

**Nutzbar heißt aktiv und nicht administrativ gesperrt.** Zählte man gesperrte mit, wäre der zweite
Admin ein Feigenblatt. Die *automatische* Sperre nach fünf Fehlversuchen zählt hier **nicht** als
gesperrt: Sie läuft nach fünfzehn Minuten ab, und eine Herabstufung für eine Viertelstunde zu
verweigern, weil jemand sich vertippt hat, wäre eine Regel, die von der Uhr abhängt.

Das ist kein Sicherheitsnetz, sondern die Vermeidung eines vermeidbaren Supportfalls — der Betreiber
hat einen SQL-Client.

---

## 6. Mandantentrennung: die Rollengrenze

**Die Nutzerverwaltung ist mandantenfrei** (E2). `app_user` liegt in `overlord_monitor`, Regel M2
bindet nur Repositories auf `jooq.glassfish`. Es gibt hier keine Mandantengrenze, an der ein Leck
entstehen könnte.

**Regel M4 wird deshalb zur Rollenprüfung**, ein Test je Endpunkt: Jeder gibt einem MANDANT-Nutzer
`403` — **auch dann, wenn er auf dessen eigenes Konto zeigt.** Genau dieser Fall ist der Kern, denn
er ist der einzige, bei dem eine Berechtigungsprüfung über den Kontoinhaber plausibel aussieht und
trotzdem falsch ist: Wer sein eigenes Konto entsperren, sich selbst zum ADMIN machen oder sich
Mandanten zuordnen darf, braucht die Benutzerverwaltung nicht mehr zu überwinden — er *ist* sie.

Dazu die Gegenprobe, die den Beweis trägt: Dieselben Aufrufe mit einer **erfundenen** Konto-ID
liefern einen Antwortrumpf, der bis auf `traceId` und `instance` **zeichengleich** ist — beide sind
je Anfrage verschieden und müssen es sein; `instance` spiegelt nach RFC 9457 den angefragten Pfad
und trägt die Kennung deshalb zwangsläufig. Wäre es anders — `403` für ein existierendes, `404`
für ein erfundenes Konto —, ließe sich über die Endpunkte durchzählen, welche Konto-IDs es gibt. Die
Rollengrenze greift **vor** jedem Datenbankzugriff, weil sie in `SecurityConfig` steht und nicht im
Service.

> **Die Regel in `SecurityConfig` bestand bereits und deckt die neuen Unterpfade ab.**
> `.requestMatchers("/api/admin/**").hasRole("ADMIN")` matcht mehrere Segmente. Sie ist am
> 21.08.2026 **geprüft und nicht ergänzt** worden — anders als bei `/api/katalog/**` in Schritt 9b,
> wo eine Regel fehlte. Eine zweite Regel daneben wäre der Fall, bei dem später niemand mehr weiß,
> welche greift.

---

## 7. Die Tests

| Datei | Art | Fälle | Was sie tragen |
|---|---|---:|---|
| `security/SitzungssucheDbIT` | `@Tag("db")` | 5 | **M81** — die vier Punkte aus E7, bleibend bewacht |
| `security/SitzungsentzugDbIT` | `@Tag("db")` | 7 | E5 für alle fünf Vorgänge, E6 für die eigene Änderung, die Zahl im Protokoll |
| `security/SitzungsentzugTest` | Einheit | 3 | Welche Sitzungen fallen — und dass eine leere Menge nichts löscht |
| `admin/BenutzerverwaltungIsolationDbIT` | `@Tag("db")` | 6 | Regel M4 als Rollengrenze, **einer je Endpunkt**, mit Gegenprobe |
| `admin/BenutzerverwaltungDbIT` | `@Tag("db")` | 16 | Liste, Sperre, Aktivzustand, Rolle, Mandanten, Passwort-Reset |
| `admin/BenutzerverwaltungServiceTest` | Einheit | 8 | E12, zweite Stufe **und die Reihenfolge der beiden Stufen** — siehe unten |
| `security/AppUserStatementsTest` | Einheit | 3 | Die Bedingung „nutzbar" steht **im** Statement — gerendert, nicht nachgebaut |

**Zwei Sitzungen je Konto, nicht eine.** Der Fehler, den ein Test mit nur einer Sitzung nicht fände,
ist der naheliegendste: nur die zuletzt angelegte zu verwerfen.

> **Warum die zweite Stufe von E12 ein Einheitstest ist und kein `DbIT` — eine benannte
> Abweichung.** Die Regel greift, wenn es im *gesamten* Bestand keinen zweiten aktiven, nicht
> gesperrten Administrator mehr gibt. Die Testkopie trägt aber die echten Administratorkonten des
> Betreibers; um den Zustand „genau ein nutzbarer ADMIN" herzustellen, müsste ein Test sie
> deaktivieren oder sperren. Das ist ein Eingriff in eine gemeinsam genutzte Umgebung, den kein
> Testlauf wert ist — ein misslungener Rollback nähme dem Betreiber den Zugang zu seinem eigenen
> Werkzeug. Geprüft wird deshalb die **Entscheidung** mit einem gestellten Repository; die Bedingung
> selbst steht als eine Zeile in `AppUserRepository.existiertAndererNutzbarerAdmin`.

---

## 8. Abweichungen von `benutzerverwaltung.md`

| | Abweichung | Grund |
|---|---|---|
| 1 | **Neue Spalte `locked_by_admin`**, die der Auftrag nicht vorsah | Die Vorgabe schweigt zur Speicherung der Admin-Sperre; `locked_until` mitzubenutzen ist am Bestand nachweisbar defekt (§2.2). Am 21.08.2026 gemeldet und entschieden |
| 2 | **Pfad `{id}/tenants`** statt `{benutzername}/mandanten` aus drei anderen Dateien | Widerspruch, am 21.08.2026 gemeldet und entschieden (§4) |
| 3 | **Statuscodes** für neun Fälle festgelegt, die keine Datei nennt | Ohne sie ist der Endpunkt nicht baubar; die Zuordnung ist in §4 begründet |
| 4 | **Neuer Problemtyp `konto-administrativ-gesperrt`** in der Anmeldung | Der bestehende Text sagt „nach mehreren Fehlversuchen" und wäre bei einem Verwaltungsakt falsch (§2.3) |
| 5 | **E12, zweite Stufe als Einheitstest** statt `DbIT` | §7 |
| 6 | **Der Entzug trifft auch den Handelnden** | E5 ist eine Regel ohne Fallunterscheidung; die Folge ist festgehalten statt weggebaut (§3) |
| 7 | **Keine der fünf Dienstmethoden trägt `@Transactional`** | Der Entzug muss nach dem Festschreiben laufen, sonst überlebt eine im Fenster entstandene Sitzung die Sperre. Vier Vorgänge sind ohnehin ein einzelnes `UPDATE`; der fünfte trägt seine Transaktion im Repository (§3) |

---

## 9. Offene Punkte

| | |
|---|---|
| 1 | **Kein Frontend.** Die Oberfläche zu 9a fehlt; ohne sie ist keine Sichtprüfung im Browser möglich |
| 2 | **Die Typzeile `downloadAllowed` steht noch im Frontend** (`features/sitzung/api.ts`). Sie wird nirgends gelesen, ist aber seit dem 21.08.2026 eine Zusage, die das Backend nicht mehr einhält. Im Frontendteil zu streichen |
| 3 | **Der Index-Vorschlag aus M82 ist nicht angelegt** und nicht gemessen: `(event_type, actor_user_id, occurred_at)`. Der vorhandene `idx_audit_type` hilft nicht, und ihn zu erzwingen ist fast doppelt so langsam |
| 4 | **`app_user.last_login_at` bleibt eine gepflegte tote Spalte.** Sie wäre die 0,39-ms-Antwort auf dieselbe Frage wie die 17,9-ms-Aggregation. E17 verwirft „eine **neue** Spalte" — diese ist nicht neu. Nicht entschieden |
| 5 | ~~**Die automatische Zeitsperre ist in der Liste nicht sichtbar.** `locked` zeigt nur die administrative. Wer wissen will, warum sich jemand gerade nicht anmelden kann, sieht es der Liste nicht an~~ · **Geschlossen am 24.08.2026.** `lockedUntil` steht in der Zeile, sobald es in der Zukunft liegt ([`benutzerverwaltung.md`](benutzerverwaltung.md) E20). Der Punkt widersprach der Entscheidung, die §4 an derselben Sache traf; welche galt und warum, steht im Korrekturkasten dort |
| 6 | **Der Kommentar in `V7__benutzerverwaltung.sql` trägt eine widerlegte Behauptung** zur `serialVersionUID` (§2.1). Er lässt sich nicht korrigieren, ohne die Prüfsumme der aufgezogenen Migration zu brechen; eine Berichtigung bräuchte ein `flyway repair` gegen die Testkopie. Nicht entschieden |

---

## 10. Was dieser Schritt nicht gebaut hat

- Kein Frontend, keine Oberfläche
- Kein Löschen von Konten (E8) — es gibt nur Deaktivieren
- Keine Spalte `letzte_anmeldung` (E17)
- Keine eigene Ereignisart für den Sitzungsentzug (E15)
- Keine Änderung am **Vertrag** von `POST /api/admin/users` — Pfad, Rumpf, Antwort und Statuscodes
  sind unangetastet (E4). Der *Codepfad* hat sich sehr wohl geändert: Die Längenprüfung und das
  Kodieren des Einmalpassworts sind nach `AdminUserService.kodiereEinmalpasswort` gewandert, damit
  Anlegen und Zurücksetzen dieselbe Strecke gehen (E13)
- Keine Änderung an den drei Rohdaten-Endpunkten aus Schritt 8
- Keine Reparatur an der Spring-Session-Konfiguration — es war keine nötig (M81)
- Kein Index auf `audit_log` — der Vorschlag steht in M82 und ist eine Entscheidung, keine Ableitung
