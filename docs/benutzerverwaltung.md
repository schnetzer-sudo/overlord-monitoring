# Benutzerverwaltung

Stand: 21.08.2026 · Schritt 9a · Fachliche Festlegung
Baut auf [`authentifizierung.md`](authentifizierung.md) (Schritt 3) und
[`mandantentrennung.md`](mandantentrennung.md) auf.

**Entscheidungen E1 bis E21.** E19 bis E21 sind am 21.08.2026 dazugekommen und betreffen die
Oberfläche (§7a); gebaut sind sie **nicht** — sie gehören in den 9a-Frontend-Auftrag, und **E20**
verlangt zusätzlich eine Backend-Änderung.

---

## 1. Wozu

`IMPLEMENTIERUNGSPLAN_MVP.md` nannte als Ziel: *„Das System ist ohne Datenbankzugriff betreibbar."*

**Das ist kein Ziel dieses Projekts.** Der Entwickler ist zugleich der Betreiber und hat einen
SQL-Client. Der Satz stammt aus einer Vorlage und ist am 20.08.2026 als Begründung **verworfen**.

**E1 — 9a wird gebaut, weil über zwanzig externe Nutzer absehbar sind**, die keinen
Datenbankzugang haben und niemals bekommen werden. Jedes vergessene Passwort, jede Sperre, jeder
Austritt landet sonst per Zuruf beim Betreiber.

> Der Austausch der Begründung ist ausdrücklich vollzogen und nicht ergänzt. Das Projekt hat den
> Grundsatz schon: *„Eine Regel, die mit einer falschen Zahl begründet ist, wird beim ersten Zweifel
> gekippt."* Eine Entscheidung, die eine von ihrem Eigentümer zurückgewiesene Begründung trägt, ist
> instabil — sie fällt beim nächsten Zweifel, unabhängig davon, ob sie richtig war.

**Was schon existiert und nicht neu gebaut wird:** `POST /api/admin/users` seit Schritt 3, als
zweite M1-Ausnahme geführt. Das Abnahmekriterium „Ein Admin legt einen Nutzer an, der sich anmelden
kann" ist bereits ohne 9a erfüllbar. *(Gelesen aus `mandantentrennung.md` §3, am Code nicht
nachgeprüft.)*

---

## 2. Mandantentrennung gilt hier anders

**E2 — Die Nutzerverwaltung ist mandantenfrei.** `app_user` liegt in `overlord_monitor`; Regel M2
bindet nur Repositories, die `jooq.glassfish` anfassen. Die Liste zeigt **alle** Konten, unabhängig
vom aktiven Mandanten.

> **Gegenprobe.** Ein ADMIN-Konto trägt eine wirkungslose Mandantenzuordnung. In einer
> mandantengefilterten Nutzerliste stünde es unter einem Mandanten, für den es nicht gilt — die
> mandantenfreie Liste ist deshalb nicht die bequemere, sondern die richtige.

**E3 — Regel M4 wird zum Rollentest.** Es gibt hier keine Mandantengrenze, an der ein Leck entstehen
könnte. Geprüft wird stattdessen die **Rollengrenze**: Jeder Endpunkt dieses Dokuments gibt einem
MANDANT-Nutzer `403` — **auch dann, wenn er auf dessen eigenes Konto zeigt.**

Das ist die zweite Übertragung derselben Regel, nach der Verschiebung von der Eingabe auf die
Ausgabe bei `ProzesseIsolationDbIT`. Keine Aufweichung. Nachtrag in `mandantentrennung.md` §5.

**E4 — Eine dritte M1-Ausnahme.** `PUT /api/admin/users/{id}/tenants` nimmt Mandanten-IDs entgegen.

`mandantentrennung.md` §3 trägt den Satz, eine dritte Ausnahme sei „ein Signal und keine
Kleinigkeit". **Der Satz bleibt stehen** — er hat gewirkt. Alle drei Ausnahmen **definieren eine
Berechtigung**, statt einen Datenausschnitt abzufragen; das ist der gemeinsame Grund, aber die
Aufzählung bleibt die prüfbare Form, nicht das Prinzip.

`POST /api/admin/users` **bleibt unverändert** bei einem Mandanten. Eine Signaturänderung an einem
getesteten Vertrag aus Schritt 3 wäre der teurere Weg zum selben Ergebnis.

---

## 3. Sitzungen

**E5 — Alle Sitzungen eines Kontos werden verworfen** bei sperren, deaktivieren, Rollenänderung,
Mandantenänderung und Passwort-Reset durch den Admin. Eine Regel, keine Fallunterscheidung.

Das Projekt hat JWT abgelehnt, weil „bei externen Nutzern sofortige Rücknehmbarkeit schwerer wiegt
als Zustandslosigkeit". Ohne Entzug wird diese Zusage nicht eingelöst.

**E6 — Die eigene Passwortänderung verwirft die *übrigen* Sitzungen und behält die aktuelle.** Heute
erneuert sie nur die Sitzungs-ID. Wer sein Passwort ändert, weil er einen fremden Zugriff vermutet,
erwartet genau das. Die Ungleichbehandlung zum Admin-Reset — der **alle** verwirft — ist damit
begründet und nicht vergessen.

### Der Mandantenfall ist ein Defekt, kein Komfortthema

> **Belegvermerk (L10).** *Gelesen* aus `mandantentrennung.md` §1: Der `MandantContext` wird aus der
> Sitzung aufgelöst; `MandantService.wechsle` prüft die zulässige Menge **beim Wechsel**.
> *Behauptet wird:* Wird einem Konto ein Mandant entzogen, während es angemeldet ist, bleibt der
> aktive Kontext bestehen und wird pro Anfrage nicht nachgeprüft. **Am Code nicht verifiziert.**
>
> Trifft es zu, ist es ein Leck in der Regel, die dieses Projekt selbst „die wichtigste" nennt —
> und es **besteht heute**, ohne Oberfläche. Eine Pflegemaske hätte es nicht verursacht, nur
> häufiger ausgelöst.

**E5 und E9 greifen deshalb ineinander:** Die Mehrfachzuordnung wäre ohne Sitzungsentzug gefährlicher
als die bisherige Einfachzuordnung — wer einem Nutzer einen von zwei Mandanten nimmt, nimmt ihm
möglicherweise genau den, den er gerade aktiv hat.

### E7 — Verifikation vor der ersten Zeile Code

Bauform wie M72 vor Schritt 8. Vier Punkte, alle am laufenden System:

| | |
|---|---|
| a | Enthält `V3__spring_session.sql` den Index auf `PRINCIPAL_NAME`? Er steht im Lieferskript, ist nie gebraucht worden |
| b | Steht nach einer Anmeldung ein Wert darin? Die Anmeldung läuft über einen **eigenen Controller** statt `formLogin` |
| c | Ist `JdbcIndexedSessionRepository` als `FindByIndexNameSessionRepository` injizierbar, wo zwei DataSources stehen und **keine `@Primary`** ist? |
| d | Stimmt der Wert mit `app_user.username` überein? Die Spalte trägt `utf8mb4_general_ci` — ohne Rücksicht auf Groß- und Kleinschreibung, was hier **erwünscht** ist, aber gemessen gehört |

**Rückfallebene:** ein Zähler an `app_user`, bei jeder Änderung aus E5 hochgesetzt und je Anfrage
gegen die Sitzung geprüft. Kostet eine Abfrage je Anfrage, kommt dafür ohne Spring-Session-Interna
aus. **Zweite Wahl, nicht erste.**

---

## 4. Konten

| | Entscheidung | Grund |
|---|---|---|
| **E8** | **Kein Löschen, nur Deaktivieren** | `audit_log.actor_user_id` verweist auf `app_user`. Ein gelöschtes Konto macht seine Protokollzeilen unlesbar — und das Protokoll ist der Teil, der bei einem Vorfall gebraucht wird |
| **E9** | **n:m-Pflege wird gebaut** | `NEXANS`/`NXHBE` und `IBIS`/`IBISGUS` sind je dasselbe Haus. Bei über zwanzig Nutzern über zehn Mandanten braucht jemand beide. Tabelle `app_user_mandant` und mengenbasierte Prüfung stehen seit Schritt 3 — es fehlte allein die Pflege |
| **E10** | **Die letzte Mandantenzuordnung lässt sich nicht entfernen — für beide Rollen** | Schritt 3 speichert sie auch für ADMIN, „damit sie bei einer späteren Herabstufung nicht ins Leere fällt". Dürfte man sie beim ADMIN entfernen, entstünde beim nächsten Rollenwechsel genau der verbotene Zustand |
| **E11** | **Herabstufung ADMIN → MANDANT wird abgelehnt**, solange keine Zuordnung besteht | Kein Sonderpfad, nur eine Reihenfolge — mit einer Meldung, die sagt, was zuerst zu tun ist |
| **E12** | **Selbstschutz zweistufig:** nicht sich selbst sperren, deaktivieren oder herabstufen; und der **letzte nutzbare** ADMIN auch nicht | „Nutzbar" heißt aktiv und nicht gesperrt. Zählte man gesperrte mit, wäre der zweite Admin ein Feigenblatt. **Das ist kein Sicherheitsnetz, sondern die Vermeidung eines vermeidbaren Supportfalls** — der Betreiber hat einen SQL-Client |
| **E13** | Passwort-Reset: **der Admin tippt es**, mindestens zwölf Zeichen, **und es darf nicht dem aktuellen entsprechen** (per BCrypt-Vergleich gegen den gespeicherten Hash prüfbar). Konto bekommt Änderungszwang. **Ein Codepfad gemeinsam mit dem Anlegen** | Konsistent mit `POST /api/admin/users` seit Schritt 3 |

> **Das Bootstrap-Konto ist der Anlass für E11.** `BootstrapAdminRunner` legt das erste ADMIN-Konto
> aus zwei Umgebungsvariablen an — Benutzername und Passwort, kein Mandant. Es ist damit das einzige
> Konto ohne Zuordnung.
>
> *Gelesen aus `authentifizierung.md` §7 — die Variablenliste nennt keinen Mandanten. **Am Code
> nicht geprüft**; gehört in dieselbe Verifikation wie E7.*

**Annahme A2 fällt.** Mit gepflegter n:m-Zuordnung ist „Ein Nutzer gehört zu genau einem Mandanten"
keine Annahme unter Druck mehr, sondern erledigt. `PROJEKTBESCHREIBUNG.md` §11 trägt den datierten
Eintrag.

---

## 5. Endpunkte

**Ein Endpunkt je Vorgang.** Der Grund ist das Protokoll: Ein gemeinsames `PATCH`, das Rolle,
Mandanten und Sperrzustand in einem Aufruf ändern kann, erzeugt eine Protokollzeile, die entweder
aufgespalten werden muss oder zu einem nichtssagenden `NUTZER_GEAENDERT` verwässert. **Ein Endpunkt,
ein Vorgang, eine Ereignisart** — dann ist das Protokoll bei einem Vorfall lesbar, und genau dafür
existiert es.

| Endpunkt | Ereignisart |
|---|---|
| `GET /api/admin/users` | — |
| `PUT /api/admin/users/{id}/lock` | `SPERRE_DURCH_ADMIN` / `ENTSPERRT_DURCH_ADMIN` |
| `PUT /api/admin/users/{id}/active` | `NUTZER_DEAKTIVIERT` / `NUTZER_REAKTIVIERT` |
| `PUT /api/admin/users/{id}/role` | `ROLLE_GEAENDERT` |
| `PUT /api/admin/users/{id}/tenants` | `MANDANTEN_GEAENDERT` ← **dritte M1-Ausnahme** |
| `POST /api/admin/users/{id}/password` | `PASSWORT_ZURUECKGESETZT` |

Sperre und Deaktivierung sind je **ein** Endpunkt mit Zustand, nicht zwei. Jeder der fünf
schreibenden Vorgänge verwirft alle Sitzungen des Kontos (E5).

**E14 — Die Admin-Sperre heißt `SPERRE_DURCH_ADMIN`, nicht `NUTZER_GESPERRT`.** Neben dem seit
Schritt 2 bestehenden `KONTO_GESPERRT` — der automatischen Sperre nach fünf Fehlversuchen — wäre
das um drei Uhr nachts nicht auseinanderzuhalten. **Der Name benennt die Ursache statt des Objekts,
und die Ursache ist der Unterschied:** ein Angriff und ein Verwaltungsakt gehören nicht in dieselbe
Zeile.

**E15 — Der Sitzungsentzug bekommt keine eigene Ereignisart.** Er ist nie ein eigenständiger
Vorgang und stünde sonst als Anhängsel in jeder zweiten Zeile. Die Zahl verworfener Sitzungen gehört
an das auslösende Ereignis.

**Pfade englisch**, unter `/api/admin/users`. Das Projekt führt Fachpfade deutsch
(`/api/nachrichten`, `/dateien`), Infrastrukturpfade englisch (`/api/auth/...`, das bestehende
`/api/admin/users`). E4 lässt die Sammlung unangetastet; deutsche Unterpfade darunter wären
schlechter als beide reinen Varianten.

---

## 6. Die Liste

**E16 — Keine Paginierung, keine serverseitige Suche.** Größenordnung dreißig Konten. Jede Mechanik
dafür wäre Beiwerk.

Je Zeile: Benutzername, Rolle, Mandanten, Sperrzustand, Aktivzustand, Änderungszwang, **letzte
Anmeldung**.

**E17 — Die letzte Anmeldung kommt aus dem `audit_log`**, nicht aus einer neuen Spalte an
`app_user`. Bei über zwanzig externen Nutzern ist *„hat der sich überhaupt je angemeldet"* die
häufigste Supportfrage, und das Protokoll hat die Antwort bereits.

> **Das ist eine Aggregation und braucht eine Messung.** `MAX(zeitpunkt)` je Nutzer, gefiltert auf
> Anmeldeereignisse, über eine Tabelle, die **jeden Download** mitprotokolliert und entsprechend
> wächst. `EXPLAIN` plus Laufzeit vor dem Merge (Leistungsregel 7); fehlt ein passender Index, ist
> das ein Befund und keine Kleinigkeit. **Nicht als gegeben annehmen.**

---

## 7. `download_allowed` fällt

**E18 — Die Spalte wird per Migration entfernt.** Sie war seit Schritt 3 tot: `rohdaten.md` §3 E2
schließt eine zweite Berechtigungsstufe aus, die Endpunkte prüfen sie nicht, `GET /api/auth/me` gibt
sie trotzdem aus.

| Folge | |
|---|---|
| Migration | Spalte entfällt |
| `GET /api/auth/me` | verliert das Feld — **Vertragsbruch aus Schritt 3**, `authentifizierung.md` §1 datiert korrigiert |
| `PROJEKTBESCHREIBUNG.md` §7 | Die Begründung *„damit ein späterer Entzug keine Migration erfordert"* ist **umgestoßen**. Genau die wird jetzt fällig, falls je jemand danach fragt — bewusst in Kauf genommen, weil niemand danach gefragt hat |
| `rohdaten.md` §3 E2 | **bestätigt**, nicht korrigiert |
| `rohdaten-backend.md` | offener Punkt 8 schließt mit *„Spalte entfernt"* |

Die drei Endpunkte aus Schritt 8 bleiben unangetastet.

---

## 7a. Drei Entscheidungen zur Oberfläche (21.08.2026)

**Gebaut wird hier nichts.** E19 bis E21 sind am 21.08.2026 gefallen und stehen hier, weil sie sonst
nur im Gespräch existierten. Ihre Umsetzung gehört in den **9a-Frontend-Auftrag**; E20 verlangt
zusätzlich eine Backend-Änderung, die unten benannt ist.

**E19 — Vorwarnung, wenn ein Admin sein eigenes Konto trifft.** Wer sich selbst sperrt, deaktiviert
oder die Rolle nimmt, bekommt vorher einen ausdrücklichen Hinweis — keine stille Ausführung.

**Der Vergleich läuft über den Benutzernamen, ohne Rücksicht auf Groß- und Kleinschreibung.** Das
ist keine Bequemlichkeit, sondern die einzige verfügbare Möglichkeit: **`GET /api/auth/me` liefert
keine `id`.** Die Selbstauskunft trägt `username`, `role`, `mandant`, `mustChangePassword` und
`anzeigezone` — mehr nicht (`security/SelbstauskunftResponse`). Die Verwaltungsliste führt daneben
eine `id`, aber es gibt keinen Wert, über den sich die beiden verknüpfen ließen außer dem Namen.

Ohne Rücksicht auf die Schreibweise, weil die Anmeldung sie ebenfalls nicht beachtet: Ein Konto
`Admin`, das sich als `admin` anmeldet, sähe seine eigene Zeile sonst als fremde und bekäme die
Warnung nicht — also genau im Grenzfall nicht, für den sie da ist.

> **Die Alternative wäre, `id` in die Selbstauskunft aufzunehmen.** Sie ist **nicht** gewählt: Die
> Selbstauskunft ist seit Schritt 3 ein Vertrag, der in 9a schon einmal gebrochen worden ist (E18,
> `downloadAllowed`), und ein zweiter Bruch in derselben Runde für eine Bequemlichkeit der
> Oberfläche wäre schlecht bezahlt. Der Namensvergleich trägt.

**E20 — `lockedUntil` als eigenes Feld, nur wenn es in der Zukunft liegt — nie in `locked` gemischt.**
Die automatische Sperre nach fünf Fehlversuchen bekommt ein eigenes Antwortfeld. Sie wird **nicht**
in `locked` verrechnet; `locked` bleibt allein die administrative Sperre.

> **Das kehrt eine Entscheidung aus 9a um und ist deshalb mehr als ein Absatz.** Heute liefert das
> Backend `lockedUntil` **gar nicht** — `admin/NutzerzeileResponse` trägt acht Felder, keins davon
> ist es, und `AppUserRepository` liest die Spalte erst gar nicht. Weggelassen ist sie **bewusst**,
> mit dieser Begründung im Code (`NutzerzeileResponse`, Verweis auf **E14**): *„Nicht die
> automatische nach fünf Fehlversuchen: die läuft nach fünfzehn Minuten von selbst ab, und ein
> Zustand, der beim Hinsehen schon wieder anders ist, gehört nicht in eine Verwaltungsliste."*
>
> **Dieser Einwand bleibt richtig — er trifft nur nicht mehr, was E20 vorschlägt.** Er richtet sich
> gegen einen Zustand, der als *gesperrt* in der Liste steht und beim nächsten Blick verschwunden
> ist. E20 zeigt keinen Zustand, sondern einen **Zeitpunkt**, und genau deshalb steht die Bedingung
> „nur wenn in der Zukunft" darin: Ein abgelaufener Wert wird nicht übertragen, statt als
> abgelaufene Sperre erklärt werden zu müssen.
>
> **Wozu er trotzdem gebraucht wird:** Ein Admin, den ein Nutzer anruft, weil er nicht
> hineinkommt, muss zwischen „ich habe dich gesperrt" und „du hast dich fünfmal vertippt"
> unterscheiden können. Ohne das Feld sieht er `locked: false` und hat keine Erklärung.
>
> **Was das kostet:** die Spalte in `AppUserRepository` mitlesen, ein Feld an
> `NutzerzeileResponse`, und der zitierte Kommentar bekommt einen datierten Vermerk. **Zu bauen im
> 9a-Auftrag, nicht hier.** Der Wert ist UTC aus der Systemuhr und wird in der `anzeigezone`
> formatiert wie jeder andere Zeitpunkt.

**E21 — Der Umschalter bleibt, die Liste trägt den Hinweis.** Sperren und Entsperren bleiben ein
Umschalter je Zeile; es entsteht kein zweiter Bedienweg für die automatische Sperre. Der Hinweis aus
E20 steht **an der Zeile** und nicht in einem eigenen Bereich — er erklärt, warum sich jemand nicht
anmelden kann, und ist damit an genau der Zeile richtig, die man ohnehin ansieht.

Dass der Umschalter die automatische Sperre **nicht** aufhebt, ist Absicht und keine Lücke: Sie
läuft nach fünfzehn Minuten von selbst ab. Ein Knopf, der sie vorzeitig löscht, wäre ein zweiter
Weg zu einem Zustand, der sich selbst aufräumt.

---

## 8. Verworfene Möglichkeiten

| Verworfen | Grund |
|---|---|
| „Ohne Datenbankzugriff betreibbar" als Begründung | E1 |
| Löschen von Konten | E8 |
| Ein gemeinsames `PATCH` für alle Felder | §5 — das Protokoll |
| `NUTZER_GESPERRT` als Name der Admin-Sperre | E14 |
| Eigene Ereignisart für den Sitzungsentzug | E15 |
| Neue Spalte `letzte_anmeldung` an `app_user` | E17 — das Protokoll hat die Antwort |
| Sitzungsentzug nur bei Sperre und Deaktivierung | E5 — eine Regel statt Fallunterscheidungen |
| Systemseitig erzeugtes Einmalpasswort | E13 — Konsistenz mit dem Anlegen aus Schritt 3 |
| `download_allowed` in die Oberfläche holen und `rohdaten.md` E2 korrigieren | Am 20.08.2026 zwischenzeitlich entschieden und **zurückgenommen**. Die Spalte fällt stattdessen (E18) |
| Spaltenumbenennung `raw_data_allowed` | dito — hinfällig mit E18 |
| Signaturänderung an `POST /api/admin/users` | E4 |
| Zählung gesperrter ADMINs beim Selbstschutz | E12 |
| `id` in `GET /api/auth/me` aufnehmen | E19 — der Namensvergleich trägt; kein zweiter Vertragsbruch an der Selbstauskunft in derselben Runde |
| Die automatische Sperre in `locked` verrechnen | E20 — zwei Sperren mit verschiedener Ursache und verschiedener Behebung |
| Eigener Knopf, der die automatische Sperre vorzeitig löscht | E21 — sie läuft nach fünfzehn Minuten selbst ab |

---

## 9. Offene Punkte

| | |
|---|---|
| 1 | **E7 fahren, bevor an 9a eine Zeile geschrieben wird.** Fällt einer der vier Punkte aus, gilt die Rückfallebene und der Endpunktschnitt bleibt unberührt — aber es ist vorher zu wissen |
| 2 | Trägt das Bootstrap-Konto wirklich keine Mandantenzuordnung? Gehört in dieselbe Verifikation |
| 3 | Messung zu E17: `MAX(zeitpunkt)` je Nutzer über `audit_log` |
| 4 | Ist der `MandantContext` je Anfrage gegen die zulässige Menge geprüft, oder nur beim Wechsel? Siehe §3 — **das ist die sicherheitsrelevanteste der vier Fragen** |
