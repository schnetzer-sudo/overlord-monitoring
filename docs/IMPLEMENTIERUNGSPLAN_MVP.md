# Overlord Monitoring — Implementierungsplan MVP

Stand: 01.08.2026 · Ergänzt `PROJEKTBESCHREIBUNG.md`

**Stand der Umsetzung**

| Schritt | Status |
|---|---|
| 1 — Fundament | erledigt |
| 2 — Backend-Grundgerüst und Datenzugriff | erledigt, über die CI bestätigt, auf `main` |
| 3 — Anmeldung und Mandantentrennung | **geteilt in Teil 1 (Backend) und Teil 2 (Frontend)**; beide Teile erledigt (29.07.2026) |
| 4 — Nachrichtenliste | erledigt, samt Nachbesserung (07.08.2026); Messungen in [`messungen-schritt4.md`](messungen-schritt4.md) |
| 5 — Nachrichtendetail und Prozessschritte | **geteilt in Teil 1 (Backend) und Teil 2 (Frontend)**; beide Teile erledigt (07.08.2026). Messungen in [`messungen-schritt5.md`](messungen-schritt5.md) und [`nachrichtendetail.md`](nachrichtendetail.md) §8, die Oberfläche in §10 |
| 6 — Verkettung | **geteilt in Teil 1 (additiv), Teil 2a (Löschungen), Teil 2b (Kettenfläche)**; alle drei erledigt (11.08.2026), dazu Nacharbeit Teil B und Nachbesserung 1. Messungen in [`messungen-schritt6.md`](messungen-schritt6.md), das Feature in [`verkettung.md`](verkettung.md) |

**Korrektur 07.08.2026 zu Schritt 4.** Die Tabelle führte Schritt 4 bis hierhin als **offen**.
Dieser Stand war überholt: Der Listen-Endpunkt steht seit dem 06.08.2026 und ist am 07.08.2026
nachgebessert worden. Belege sind die Dokumente, die laut Dokumentationspflicht erst mit dem Feature
entstehen — [`nachrichtenliste.md`](nachrichtenliste.md) und [`prozessauswahl.md`](prozessauswahl.md)
—, dazu die grünen Pflichttests `NachrichtenIsolationDbIT` und `ProzesseIsolationDbIT`. Der überholte
Eintrag wird hier benannt und nicht stillschweigend überschrieben, nach demselben Muster wie die
Korrektur zu Schritt 3.

**Schritt 5 ist geteilt**, aus demselben Grund wie Schritt 3: Backend und Oberfläche zusammen sind zu
groß für einen Durchlauf. Teil 1 ist reines Backend und liefert die beiden Endpunkte; Teil 2 baut das
Detailpanel und zieht dabei die Beschriftung der Nachrichtenliste nach
([`nachrichtenliste.md`](nachrichtenliste.md) §9, „steht auf" gegen „wartet vor").

**Korrektur 01.08.2026 zu Schritt 3, Teil 2.** Die Tabelle führte Teil 2 bis hierhin als **offen**.
Dieser Stand war seit dem 29.07.2026 überholt: Teil 2 ist an diesem Tag zusammen mit Teil 1
abgeschlossen worden. Beleg sind die beiden Dokumente, die laut Dokumentationspflicht erst mit dem
Feature entstehen — [`frontend-grundlagen.md`](frontend-grundlagen.md) und
[`visuelles-konzept.md`](visuelles-konzept.md); beide sind angelegt und tragen Messungen vom
29.07.2026. Der überholte Eintrag wird hier benannt und nicht stillschweigend überschrieben, damit
nachvollziehbar bleibt, dass die Tabelle drei Tage lang falsch war und nicht etwa der Schritt
nachträglich umdatiert wurde.

**Revision 28.07.2026.** Schritt 3 ist geteilt. Die Übernahme der Altnutzer entfällt ersatzlos und
wird durch einen minimalen Anlege-Endpunkt ersetzt. Der Mandantenwechsel prüft eine Menge statt
einer Rolle. A8 ist geklärt.

**Revision 27.07.2026.** Schritt 2 ist nach dem Sparring ausformuliert. Die Statuserhebung ist von
Schritt 3 nach Schritt 2 vorgezogen, weil die Verbindung dort ohnehin steht. Schritt 4 und 7 sind
an die korrigierten Fakten angepasst.

Zehn Schritte. Zwei Fundamentschritte, danach acht Durchstiche mit Backend und Frontend zusammen.
Jeder Schritt wird ein eigener Claude-Code-Prompt und endet mit etwas, das man durchklicken kann.

Vor jedem Prompt findet ein kurzes Sparring statt: Was genau, welche Abgrenzung, welches
Abnahmekriterium. Erst danach wird der Prompt geschrieben.

---

## Vorlage für jeden Prompt

Diese Struktur bleibt über alle zehn Schritte gleich.

```
## Skills
Nutze die installierten Skills: <frontend-design>, <shadcn/ui>.
[Nur die für diesen Schritt relevanten nennen — Schritte 1, 2 und 10 sind reine
Backend-/Struktur-Schritte und brauchen die Frontend-Skills nicht.]

## Zuerst lesen
1. CLAUDE.md
2. DEVELOPMENT_GUIDELINES.md
3. docs/<für diesen Schritt relevante Dateien>
Beginne nicht mit dem Code, bevor du diese Dateien gelesen hast.

## Ziel
<ein Satz>

## Aufgabe
Backend: <...>
Frontend: <...>

## Abgrenzung
Was in diesem Schritt ausdrücklich NICHT gebaut wird: <...>

## Verbindliche Regeln
<die für diesen Schritt geltenden Regeln aus der Projektbeschreibung, wörtlich>

## Abnahme
<prüfbares Ergebnis>

## Dokumentation
Lege an bzw. aktualisiere: docs/<datei>.md
```

**Die Dokumentationspflicht steht in jedem Prompt.** Neues Feature bedeutet neue Datei in `/docs`,
geändertes Feature bedeutet aktualisierte Datei. Ein Schritt gilt erst als fertig, wenn die
Dokumentation steht.

---

## Schritt 1 — Fundament und Steuerdateien

**Ziel:** Das Repository steht, Claude Code weiß, woran es sich zu halten hat.

**Skills:** keine (reine Struktur)

**Inhalt**
- Repository-Struktur: `backend/` (Maven, Spring Boot), `frontend/` (pnpm, Next.js), `docs/`.
  **Kein `docker-compose.yml`** — entwickelt wird gegen die Testkopie. Eine lokale Datenbank
  wäre nutzlos, weil `overlord_monitor` wegen der schemaübergreifenden Joins zwingend auf
  derselben Instanz liegen muss wie `GlassfishDB`. Container werden erst beim Betrieb relevant
- **Basispaket `de.kraftwerkone.overlord.monitor`** mit der Paketstruktur aus Abschnitt 6 der
  Projektbeschreibung, wörtlich übernommen. Maven `groupId = de.kraftwerkone`,
  `artifactId = overlord-monitor`. Hauptklasse `OverlordMonitorApplication` direkt im
  Basispaket. Achtung: Spring Initializr leitet aus dem Artefaktnamen `de.kraftwerkone.overlordmonitor`
  ab — das Feld "Package name" wird manuell überschrieben
- `CLAUDE.md` als zentrale Steuerungsdatei. Verweist auf `DEVELOPMENT_GUIDELINES.md` und `docs/`,
  enthält die Anweisung, diese vor jeder Arbeit zu lesen, sowie die kurze Projekteinordnung
- `DEVELOPMENT_GUIDELINES.md` mit: Paketstruktur und Namenskonventionen, Fehlerbehandlung und
  API-Fehlerformat, Teststrategie, Umgang mit Datum und Zeit (`TimeProvider`, nie `now()`),
  Frontend-Konventionen, Commit-Konventionen, Definition of Done
- ArchUnit-Test, der die Paketstruktur festhält: Fachpakete greifen nicht aufeinander zu,
  Gemeinsames liegt in `common`. Die Regel zu `jooq.glassfish` folgt in Schritt 2
- `docs/` mit `README.md`, das die Struktur erklärt, plus `docs/datenmodell.md` als Auszug der
  Quellschema-Fakten aus der Projektbeschreibung
- Linting und Formatierung auf beiden Seiten, CI-Grundgerüst

**Abgrenzung:** Kein Fachcode, keine Datenbankverbindung.

**Abnahme:** Beide Projekte starten leer und fehlerfrei. `CLAUDE.md` verweist korrekt weiter.

---

## Schritt 2 — Backend-Grundgerüst und Datenzugriff

**Ziel:** Das Backend liest nachweisbar aus der Testkopie und schreibt nachweisbar nur ins
eigene Schema.

**Skills:** keine

**Voraussetzung:** Die Rechte auf der Testkopie sind gesetzt und `overlord_monitor` existiert mit
`utf8mb4` / `utf8mb4_general_ci`. Zugangsdaten liegen in Umgebungsvariablen, nicht im Repository.

**Inhalt**
- Zwei DataSources mit zwei DB-Benutzern. **Keine davon `@Primary`** — ein vergessener Qualifier
  soll beim Start scheitern, nicht zur Laufzeit den falschen Pool treffen. Flyway wird über
  `@FlywayDataSource` gebunden
- **Genau ein Transaktionsmanager**, gebunden an `overlord_monitor` und `@Primary`. Damit bedeutet
  `@Transactional` überall "schreibt ins eigene Schema"
- Lese-Pool klein, `readOnly`, mit `SET SESSION max_statement_time` als Laufzeitgrenze
- Zwei DSLContexts. Regel: Der Lese-Kontext darf **beide** Schemata lesen, der Schreib-Kontext
  ausschließlich `overlord_monitor`. Kein `defaultSchema`, Schemanamen immer voll qualifiziert
- jOOQ-Codegenerierung aus beiden Schemata, in **zwei getrennte Zielpakete**:
  `…monitor.jooq.glassfish` (nur lesend) und `…monitor.jooq.monitor` (schreibend). Gleichnamige
  Tabellen kollidieren dadurch nicht, und der Import zeigt die Schreibrichtung.
  Läuft bei jedem lokalen Build, **Ausgabe wird eingecheckt**; die CI überspringt sie über
  `-Djooq.codegen.skip=true`. Views einschließen (`MessageMandantID`), Routinen und Events
  ausschließen, `bit(1)` per `forcedType` auf `Boolean`
- ArchUnit: Klassen aus `jooq.glassfish` nur in Repository-Klassen, `DataSource` nur in `config`,
  kein direkter Aufruf von `now()`. Das Schreibverbot selbst prüft ArchUnit nicht — dafür sorgen
  die DB-Rechte, der `readOnly`-Pool und ein jOOQ-`ExecuteListener`, der auf dem Lese-`DSLContext`
  alles außer `SELECT` abweist
- Flyway ausschließlich für `overlord_monitor`, erste Migration mit `audit_log`. Zeichensatz und
  Sortierung in jeder Migration **explizit**, Token-Spalten mit `COLLATE utf8mb4_bin`
- Zeitquelle als `java.time.Clock`: Produktion Systemuhr, Dev-Profil ein `Clock.offset(...)`, der
  beim Start aus `MAX(Message.MessageLastUpdate)` berechnet wird. Die Zeit läuft weiter statt
  einzufrieren
- Einheitliches API-Fehlerformat (RFC 9457), globaler Exception-Handler. Regel ab hier: fremde und
  nicht existierende Ressourcen liefern beide `404`, niemals `403`
- Healthcheck, der beide Verbindungen getrennt ausweist
- **Statuskatalog vorgezogen aus Schritt 3:** `SELECT DISTINCT MessageStatus` erheben,
  `docs/message-status.md` anlegen, `MessageStatusClassifier` in `common` bauen. Die Fehlerbedingung
  entsteht hier einmal und wird nirgends nachgebaut
- Tests in zwei Stufen: ohne Datenbank (ArchUnit, Listener, Clock, Fehlerformat) und mit
  `@Tag("db")` markiert (Rauchtest, schemaübergreifender Join, Schreibverbot, Statuskatalog,
  Zeitzonen-Rundlauf, Schemaabgleich Test gegen Produktion)
- Die vorhandene CI-Konfiguration aus Schritt 1 anpassen: `-Djooq.codegen.skip=true` und
  `-DexcludedGroups=db`

**Abgrenzung:** Keine fachlichen Endpunkte, keine Authentifizierung, kein `process_catalog`.

**Abnahme:** Healthcheck grün und beide Verbindungen getrennt sichtbar. Rauchtest liefert echte
Mandanten. **Ein schemaübergreifender Join läuft in einem einzigen Statement durch** — ohne diesen
Nachweis ist der Schritt nicht fertig, denn daran hängt ab Schritt 9 die gesamte Partnerzuordnung.
Ein Schreibversuch auf `GlassfishDB` scheitert an fehlenden Rechten. Der Build läuft auch ohne
Datenbankzugriff durch.

**Dokumentation:** `docs/datenzugriff.md`, `docs/message-status.md`, `docs/annahmen-korrekturen.md`

---

## Schritt 3 — Durchstich: Anmeldung und Mandantentrennung

**Ziel:** Man kann sich anmelden und landet auf einer geschützten, noch leeren Seite.

**Geteilt in zwei Prompts**, weil Backend, erstes Frontend und Kontenanlage zusammen zu groß für
einen Durchlauf sind. Teil 1 ist reines Backend, Teil 2 das Frontend.

**Skills:** Teil 1 keine, Teil 2 frontend-design und shadcn/ui

**Backend (Teil 1)**
- `app_user` und `app_user_mandant` per Flyway
- BCrypt Kostenfaktor 12, Spring Security, serverseitige Session
- Cookie mit `HttpOnly`, `Secure`, `SameSite=Lax`
- Sperre nach fünf Fehlversuchen für 15 Minuten, zusätzlich Begrenzung pro IP
- Unspezifische Fehlermeldungen
- Session-Tabelle über `@SpringSessionDataSource` an `overlord_monitor` gebunden. Die Spalte mit
  der Session-ID bekommt `COLLATE utf8mb4_bin` — die Standardsortierung des Schemas vergleicht ohne
  Rücksicht auf Groß- und Kleinschreibung
- Sitzungsablauf und Sperrfristen rechnen mit der **Systemuhr**, nicht mit dem Clock aus Schritt 2
- **Keine Migration der Altnutzer.** Ersatzlos gestrichen, Begründung in
  `PROJEKTBESCHREIBUNG.md` Abschnitt 7. Stattdessen ein Startvorgang im Profil `bootstrap`, der
  das erste Admin-Konto aus Umgebungsvariablen anlegt — nur wenn noch kein ADMIN existiert
- **Minimaler Anlege-Endpunkt `POST /api/admin/users`** statt eines Migrationslaufs. Nur Anlegen;
  Auflisten, Sperren, Rollenwechsel und Zurücksetzen bleiben Schritt 9. Ohne ihn ist die Abnahme
  nicht möglich, weil ein Nutzer der Rolle `MANDANT` gebraucht wird
- **Passwortänderungspfad.** Jedes Konto startet mit Änderungszwang; solange er gesetzt ist, lehnt
  jeder Endpunkt außer Abmelden, Selbstauskunft und Passwortänderung ab. Ohne diesen Pfad wäre
  kein Konto nutzbar
- **Mandantenwechsel prüft die Menge, nicht die Rolle.** Ein existierender, aber nicht zulässiger
  Mandant liefert dieselbe Antwort wie eine erfundene ID
- Bei unbekanntem Benutzernamen wird trotzdem ein BCrypt-Vergleich gerechnet; die IP-Begrenzung
  liegt ausschließlich im Arbeitsspeicher
- `Secure` am Cookie ist per Profil schaltbar, im Profil `dev` aus
- Endpunkt für den angemeldeten Nutzer inklusive Rolle und aktivem Mandanten
- Der `MandantContext` trägt genau eine Mandanten-ID, wird aus der Session aufgelöst und ist ab
  hier erster Pflichtparameter jeder Repository-Methode auf das Quellschema. Eine ArchUnit-Regel
  prüft das

**Frontend (Teil 2)**
- Anwendungsrahmen: Navigation, Kopfzeile, Nutzermenü, Abmelden
- Anmeldeseite, geschützte Routen, Weiterleitung
- **Mandantenauswahl**, aktiver Mandant sichtbar in der Kopfzeile — nicht in einem Untermenü, weil
  Ansichten für ADMIN sonst unbemerkt einen anderen Ausschnitt zeigen
- **Beim Mandantenwechsel wird der Query-Cache vollständig geleert, nicht invalidiert.** Sonst
  zeigt die Oberfläche nach dem Umschalten weiter Daten des vorherigen Mandanten, obwohl das
  Backend sauber ist
- Erzwungene Passwortänderung als eigene Seite
- Erste Umsetzung des visuellen Konzepts

**Abgrenzung:** Keine Benutzerverwaltungsoberfläche, kein Zurücksetzen durch den Admin, keine
Übernahme der Altnutzer.

**Abnahme:** Anmeldung funktioniert, Abmeldung beendet die Session, unangemeldeter Aufruf einer
geschützten Route leitet um, sechster Fehlversuch sperrt, der Änderungszwang greift, und ein
fremder Mandant ist von einer erfundenen ID nicht zu unterscheiden.

**Dokumentation:** `docs/authentifizierung.md`, `docs/mandantentrennung.md`

---

## Schritt 4 — Durchstich: Nachrichtenliste

**Ziel:** Der Nutzer sieht echte Nachrichten seines Mandanten.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Listen-Endpunkt mit Pflicht-Zeitfenster (Standard 24 Stunden, Maximum ein Jahr)
- Cursor-Paginierung über `(MessageLastUpdate, MessageID)`, kein `OFFSET`
- Filter: Status, Prozess, Freitext auf Prozessname
- Statusabbildung **ausschließlich über den `MessageStatusClassifier` aus Schritt 2**, nicht inline.
  Fehler heißt `ERROR_*` **oder** `COMMIT_REJECTED`. `SUSPENDED` heißt Warten.
  Timeout-Überschreitung berechnet aus `MessageLastUpdate + MessageTimeout`, "läuft noch" definiert
  als "nicht in einem Endstatus" — **nicht** als `MessageStatus = 'RUNNING'`, den es in der
  Testkopie null Mal gibt
- A8 ist geklärt: Projekte ohne Zeile in `ProjectMandant` bleiben bewusst für niemanden sichtbar,
  auch nicht für ADMIN. Kein Sonderpfad, kein Pseudo-Mandant
- Zweiter Testnutzer für den Isolationstest auf `VOTG` oder `SUTTONS`, **nicht** auf `NXHBE` —
  zwei Mandanten desselben Konzerns sind ein schlechter Beweis für eine Trennung, die zwischen
  Firmen greifen soll
- Mandantenfilter fest im Statement über `ProjectMandant`
- Isolationstest: Mandant A fragt ab, Daten von Mandant B sind unerreichbar
- Hinweis für die Oberfläche: `SPLITTED` und `MERGED` machen zusammen rund 34 Prozent aller Zeilen
  aus. Eine ungefilterte Liste besteht zu einem Drittel aus Zwischenprodukten

**Frontend**
- Tabelle mit serverseitiger Paginierung und Sortierung
- Zeitfenster-Auswahl, Statusfilter, Suchfeld
- Filterzustand in der URL (nuqs), Ansicht teilbar
- Zustände für Laden, Leer und Fehler

**Abgrenzung:** Kein Detail, keine Verkettung, keine Rohdaten.

**Abnahme:** Liste zeigt echte Daten, Filter wirken, die URL ist teilbar und stellt den Zustand
wieder her. Der Isolationstest ist grün. Die Abfrage ist gegen die Testkopie gemessen.

**Dokumentation:** `docs/nachrichtenliste.md`

---

## Schritt 5 — Durchstich: Nachrichtendetail und Prozessschritte

**Ziel:** Ein Klick auf eine Nachricht zeigt in verständlicher Sprache, was mit ihr passiert ist.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Detail-Endpunkt: `MessageAction` als Schrittfolge mit Start, Ende und Dauer
- `MessageProperty` ausschließlich über `MessageID` geladen
- ~~**Übersetzung der `SOSActionServiceProperties` in Klartext.** Aus `NXS_FILE_CONVERT|E2A|UNWRAP`
  wird "Datei konvertiert", aus `NXS_MERGE|...|WAIT|30M` wird "wartet auf Zusammenführung,
  30 Minuten". Die Zuordnungstabelle wird gepflegt, unbekannte Bausteine erscheinen als Rohwert —
  nie geraten~~
- **Korrektur 07.08.2026: Die Zuordnungstabelle entfällt.** Der überholte Auftrag steht
  durchgestrichen darüber und wird nicht überschrieben — er stammt aus der Zeit vor
  [M13](messungen-schritt4.md#m13--trägt-sosactionid-einen-lesbaren-namen), das gezeigt hat, dass
  `SOSAction.SOSActionName` bereits durchgängig gepflegt und lesbar ist. Damit stand nicht mehr die
  Frage im Raum, *wie* übersetzt wird, sondern **ob eine `MessageAction`-Zeile diesen Namen
  überhaupt erreicht**. Drei Messungen beantworten das:
  - **[M15](messungen-schritt5.md#m15--lässt-sich-einer-messageaction-zeile-ein-lesbarer-name-zuordnen):**
    Der Klartext kommt aus dem Join `SOSAction ON (MessageAction.SOSID, MessageAction.SOSActionID)`
    — **niemals** über `Message.SOSID`. Belegt ist das nicht über die Auflösungsquote (die ist bei
    allen drei geprüften Fassungen fast gleich und belegt deshalb nichts), sondern über den
    Vergleich des *ausgeführten* mit dem *geplanten* Baustein: null Abweichungen gegen 3,83 Prozent.
  - **[M19](messungen-schritt5.md#m19--wie-groß-müsste-die-zuordnungstabelle-sein):** Eine Tabelle
    bräuchte über den ganzen dichten Monat **vier** Zeilen — und **drei davon wären keine
    Übersetzung**, weil `EERP received`, `Message has been sent` und `EERP pending` bereits lesbare
    Sätze sind. Der eine echte Fall, `FTPSender`, löst anderswo auf **25 verschiedene** Namen auf;
    eine Zeile `FTPSender → <ein Text>` wäre gröber als das, was ohne sie herauskommt.
  - **[M20](messungen-schritt5.md#m20--erklärt-messageactionsosid-die-439-prozent-aus-m13):** Die
    Ursache der namenlosen Schritte ist nicht ein fehlender Baustein, sondern eine **lückenhafte
    Nummerierung** — der Ablauf definiert `1, 98, 99`, die Ausführung zählt fortlaufend. Es fehlt
    nicht der Schritt, es fehlt die Übersetzung seiner Nummer.

  Gebaut ist stattdessen eine **dreistufige Namensauflösung** ohne eine einzige gepflegte Zeile:
  direkt über den Join, sonst über die erste Bausteinmarke im selben Ablauf (**nur bei genau einem
  Treffer**), sonst der Rohwert. Gemessen trägt sie **99,7 Prozent** der echten Schritte einen
  echten Namen ein statt 71,5. Vollständig in [`nachrichtendetail.md`](nachrichtendetail.md) §2.
- **Ebenfalls entfallen: das Gerüst der geplanten Schritte.**
  [M21](messungen-schritt5.md#m21--trägt-das-gerüst-der-geplanten-schritte) hat es gemessen — es
  trägt in 28 bis 45 Prozent der Fälle vollständig und ist in 0,31 bis 0,92 Prozent **nachweislich
  falsch**, mit wachsendem Anteil bei größerem Zeitfenster. Ein „Schritt 2 von 5" gäbe es ohnehin
  nicht: 14,5 Prozent der Abläufe nummerieren lückenhaft (M20)
- Anzeigename aus `SOS.SOSName`

**Frontend**
- Detailansicht als seitliches Panel, damit die Liste im Blick bleibt
- Schrittfolge als Zeitleiste mit Dauer je Schritt und deutlicher Markierung des Hängers
- Technische Eigenschaften eingeklappt im Hintergrund

**Abgrenzung:** Keine Verkettung zu anderen Nachrichten, kein Rohdaten-Download.

**Abnahme:** Für eine bekannte Nachricht stimmt die Schrittfolge mit dem Altsystem überein. Bei
einer hängenden Nachricht ist ohne Fachwissen erkennbar, wo sie steht.

**Dokumentation:** [`docs/nachrichtendetail.md`](nachrichtendetail.md).
~~`docs/prozessschritte-uebersetzung.md`~~ entfällt mit der Zuordnungstabelle, die sie beschrieben
hätte; der Vermerk dazu steht in [`docs/README.md`](README.md).

---

## Schritt 6 — Durchstich: Verkettung

**Ziel:** Die Frage "wo ist mein Lieferschein" wird über Aufteilung und Zusammenführung hinweg
beantwortet.

**Skills:** frontend-design, shadcn/ui (nur Teil 2)

> **Aufgeteilt in zwei Teile (10.08.2026), und zwar nicht entlang der üblichen Naht.** Bei Schritt 3
> und 5 lief die Trennung zwischen Backend und Oberfläche. Hier liegt sie anders: **Teil 1 fügt
> ausschließlich hinzu, Teil 2 entfernt.**
>
> Der Grund ist der Umbau, den [M24‑3, M26 und M28‑1](messungen-schritt6.md) ausgelöst haben — der
> Wegfall des Ausblende-Schalters, der Wegfall von `/api/nachrichten/merkmale`, die Aufteilung von
> `ZWISCHENSCHRITT`. **Jede dieser Löschungen fasst Backend und Oberfläche gleichzeitig an:** Der
> Parameter `zwischenschritte` wird vom Frontend gesetzt, `merkmale` vom Frontend gelesen, und
> `ZWISCHENSCHRITT` steht als `statusKind` in der API **und** als Zeichenkette in `de.ts`/`en.ts`.
> Trennte man sie nach Backend und Frontend, wäre das Projekt zwischen den beiden Teilen kaputt —
> und der Hauptzweig bleibt jederzeit baubar (Richtlinie §10).
>
> Deshalb: **Teil 1 = alles, was nur hinzufügt** (die beiden Endpunkte, die Rollenlogik). **Teil 2 =
> alles, was etwas wegnimmt oder ändert** (die Löschungen samt ihrer Entsprechung in der Oberfläche,
> und die Kettendarstellung selbst).

### Teil 1 — Backend, ausschließlich additiv

**Erledigt.** Vorarbeit: [M30](messungen-schritt6.md) — alle sechs Zugriffsrichtungen einzeln
gemessen, bevor eine Zeile Code entstand (Regel L7).

- Zwei Endpunkte: `GET /api/nachrichten/{messageId}/kette` und der cursor-basierte
  `GET /api/nachrichten/{messageId}/kette/nachfolger`
- Auflösung über `SourceMessageID`, `TargetMessageID` sowie die Flags `Source` und `Target` — alle
  vier, weil es **zwei Beziehungen** sind und nicht zwei Sichten auf eine (M25‑2)
- **Unsymmetrisch, aus gemessenen Gründen:** nach oben vollständig (je Ebene ein
  Primärschlüsselzugriff, 0,5 ms), nach unten **eine** Ebene (bis 3.350 Kinder, bis 897
  Merge-Eingänge, M30‑2)
- **Drei Grenzen** statt der einen geplanten: Tiefe 10, **Breite 50** und **Zyklus**. Die
  Breitengrenze fehlte im Plan und ist zwingend; der Zyklusschutz sagt „im Kreis" statt „tief"
- Rollenlogik als reine Rechenlogik in `common/Kettenrolle` und `common/Kettenrollen` — eine
  **Menge**, kein Aufzählungswert (M28‑1c), mit einem Bestandstest als Ersatz für das vollständige
  `switch`
- Mandantenfilter in jedem der fünf Statements, **auch in der Zählung** (Regel M5)

**Abgrenzung Teil 1:** Keine Oberfläche. **Keine Änderung an der Nachrichtenliste** — der
Ausblende-Schalter, der Chip, der Parameter `zwischenschritte` und `/api/nachrichten/merkmale`
bleiben unangetastet. Keine Aufteilung von `ZWISCHENSCHRITT`. Keine BAM-Werte im Kettenglied, kein
Download.

**Abnahme Teil 1:** Beide Endpunkte antworten; aufwärts wird bis zur Wurzel aufgelöst, abwärts genau
eine Ebene, alle drei Grenzen greifen. `nachfolgerGesamt` und die über den Cursor erreichbare Menge
stimmen überein — beide gefiltert. Isolationstest je Endpunkt, einschließlich der Gegenprobe mit
einer **echten** fremden Kennung. `./mvnw verify` grün, **die Oberfläche unverändert und lauffähig**.

### Teil 2a — die Statusaufteilung und die Löschungen

**Erledigt am 11.08.2026.** Der Teil, der Backend und Oberfläche gleichzeitig anfasst und deshalb
nicht entlang der üblichen Naht geteilt werden kann.

- Aufteilung von `ZWISCHENSCHRITT` in **`AUFGETEILT`** (`SPLITTED`) und **`ZUSAMMENGEFUEHRT`**
  (`MERGED`) — ändert `statusKind` in der API **und** die Sprachdateien. `istEndstatus` liefert für
  beide unverändert `true`; an der Überfälligkeitsrechnung ändert sich nichts
- Wegfall des Parameters `zwischenschritte`, seiner Bedingung im Statement und von
  `GET /api/nachrichten/merkmale` samt Zwischenspeicher und Isolationstest. **Die Liste filtert
  nicht mehr nach Status, außer der Nutzer sagt es ausdrücklich**
- Wegfall von Schalter und Chip samt ihrer Zeichenketten in `de.ts`/`en.ts` und der Klausel im
  Leerzustand — die übrigen Klauseln bleiben vollzählig
- Die Statuszelle nennt **keine Präposition** mehr (M29 hat „wartet vor" widerlegt); der Weg über
  `offenerZustand` im Listen-Endpunkt ist erwogen und verworfen

> **Der Schalter fällt ersatzlos** — und **nicht**, wie hier bis zum 11.08.2026 stand, „an der
> Stellung statt am Status". Der Grund steht in M28‑1 und ist erst mit dieser Messung entstanden:
> Ein Stellungsprädikat blendet bei `IBISGUS` **100 Prozent** aller Zeilen aus und bei `ZAST`
> 92,93 Prozent — beide sehen heute jede Zeile. Eine Vorgabe, die je nach Mandant zwischen 0 % und
> 100 % versteckt, ist keine. Vollständig in [`docs/nachrichtenliste.md`](nachrichtenliste.md) §5.

**Abgrenzung Teil 2a:** Keine Kettenfläche, keine Rollenkennzeichnung in der Liste, keine
BAM-Spalten, keine Änderung an `istEndstatus`, an der Fehlerbedingung oder an der
Überfälligkeitsrechnung, keine neue Messung.

**Abnahme Teil 2a:** `ZWISCHENSCHRITT` existiert nirgends mehr; der Statusfilter bietet beide Werte
an und liefert für beide Zeilen; `zwischenschritte` kommt in URL, Endpunkt, Filter und Statement
nicht mehr vor; ein Link mit `zwischenschritte=false` öffnet die Liste ohne Fehler und ohne Wirkung;
die Liste zeigt bei `NEXANS` sichtbar mehr Zeilen; Liste und Detail widersprechen sich an derselben
Nachricht nicht mehr.

### Teil 2b — die Kettenfläche

**Frontend**
- Darstellung der Kette im Detailpanel, aktuelle Nachricht hervorgehoben
- Navigation entlang der Kette, und der Sprung in die Nachfolger, wenn ein Glied breit ist
- Sichtbarer Hinweis, dass Quittungen bis zu eine Stunde verzögert eintreffen
  (`MatchInterchange`-Event)

**Abgrenzung Teil 2b:** Keine Graphenvisualisierung. Eine Liste oder ein einfacher Baum genügt.

**Abnahme Teil 2b:** Für eine aufgeteilte Nachricht sind alle Teile erreichbar. Für eine quittierte
Nachricht ist die Quittung sichtbar.

**Dokumentation:** [`docs/verkettung.md`](verkettung.md) — Teil 1 steht in §1 bis §7, **Teil 2b in
§8** (11.08.2026). Teil 2a steht in [`docs/nachrichtenliste.md`](nachrichtenliste.md) §5 und §8.1
sowie in [`docs/message-status.md`](message-status.md), also bei den Features, die er anfasst.

> **Zur Abnahme von Teil 2b.** „Alle Teile erreichbar" ist erfüllt, aber nicht wörtlich: Die Liste
> ist auf 50 Glieder gedeckelt, und darüber hinaus führt das Nachladen über den Cursor-Endpunkt
> ([`verkettung.md`](verkettung.md) §8.7) — gesehen an einer Wurzel mit 169 Teilen. Ein Sprung in
> die Nachrichtenliste wäre der naheliegende Weg gewesen und bricht Regel L1. **Die Quittung ist
> keine eigene Anzeige geworden:** Ein quittiertes Merge-Ergebnis trägt die Einordnung `QUITTIERT`
> in seiner Statusplakette, und die steht im Kettenglied wie im Kopf. Ein zweites Element dafür
> gäbe es nur, wenn `EERP_RECEIVED` mehr wäre als ein Status — das ist es nicht
> ([`message-status.md`](message-status.md)).

### Nacharbeit Teil B — die Ketten-API benennt den Mechanismus (11.08.2026)

**Der Vertrag oben ist an zwei Stellen überholt**, und das steht hier, statt ihn stillschweigend zu
ersetzen:

| oben genannt | seit dem 11.08.2026 |
|---|---|
| `GET /api/nachrichten/{messageId}/kette/nachfolger` | `GET /api/nachrichten/{messageId}/kette/abwaerts` — der alte Pfad ist **entfernt**, nicht als Weiche gehalten |
| `nachfolgerGesamt` (Abnahme Teil 1) | `abwaertsGesamt`; die Abnahme selbst ist unverändert |
| die Listen `vorgaenger` / `nachfolger` | `aufwaerts` / `abwaerts`, dazu **neu** `abwaertsCursor` |

**Warum.** Die alten Namen benannten eine *Bedeutung*, und die stimmt nur bei der Aufteilung: Beim
Merge-Eingang steht im Aufstieg das Ergebnis. Die Aufgabenstellung zu Teil 2b ist genau daran
gescheitert und hat bei 38.628 Zeilen das Gegenteil dessen behauptet, was passiert ist; die
Umsetzung hat die Oberfläche korrigiert, nicht die Ursache. Ausbaustufe 1 hängt den Chatbot an
genau diese Endpunkte. Vollständig samt Belegen in [`verkettung.md`](verkettung.md) §2.

**`abwaertsCursor`** beseitigt den offenen Punkt aus `verkettung.md` §11: Der erste Klick auf „Mehr
laden" kostet seither **eine** Anfrage statt zwei. **Kein Verhalten der Oberfläche ändert sich
sonst** — Beschriftungen, Einteilung nach der Flussrichtung, Zahl in der Überschrift und die drei
Grenzen bleiben.

### Nachbesserung 1 — die Ansicht ohne Liste (11.08.2026)

**Ausschließlich Frontend, keine Änderung am Backend.** Die Detailansicht bekommt einen Umschalter
zwischen den **beiden bereits vorhandenen** Einhängepunkten: dem Panel neben der Liste
(`/nachrichten?nachricht=<id>`) und der eigenen Route (`/nachrichten/<id>`).

**Der Grund ist ein Missverhältnis, das mit der Fensterbreite wächst.** Das Panel hat eine feste
Breite in `rem`, die Liste bekommt den Rest — bei 1920 px sind das gemessene **28,3 Prozent** des
Inhaltsbereichs (480 von 1697 px) gegen gerechnete 39 Prozent bei 1280 px. Ein Klick auf eine Zeile
setzt den Fokus ins Panel, der optische Schwerpunkt bleibt aber auf der Liste, und am großen Monitor
— dort, wo das Werkzeug betrieben wird — ist der Abstand am größten.

**Es entsteht kein neuer Mechanismus, kein neuer Zustand und keine neue Route.** Der Modus *ist* die
Route: kein `localStorage`, kein Cookie, kein Kontext, kein zusätzlicher Suchparameter. Zwei
Alternativen sind ausdrücklich verworfen — eine **ziehbare Trennlinie** (ihr Zustand könnte nicht in
die URL, und die Umbruchpunkte der Liste hängen an der *Fensterbreite*, nicht an ihrer eigenen: eine
ziehbare Liste quetschte sich, ohne es zu merken) und eine **größere Grundbreite** (verschiebt nur:
schmalere Liste bei unveränderten Umbruchpunkten).

**Ein Befund vor dem Bauen, festgehalten statt vorausgesetzt:** Die eigene Route hat den
Panel-Baustein **nie in 26 rem** gezeigt. Sie trägt seit Schritt 5, Teil 2 die volle Inhaltsbreite
(`max-w-inhalt` = `--dichte-inhaltsbreite`, 72 rem), linksbündig — gemessen 1152 px bei Fenster 1920.
Die zweite Aufgabe war damit weder Breitenänderung noch Umbau, sondern nur zu belegen.

**Abgrenzung:** Kein Backend, keine neue Abfrage und deshalb **keine Messung nach Regel L7** — es
gibt kein neues Statement. Keine Änderung an der Nachrichtenliste, am Kettenblock (die Grenze von 50
Abwärtsgliedern bleibt und ist Gegenstand einer eigenen Nachbesserung), an Zeitleiste, Kopf oder
Eigenschaftenblock, an der Panelbreite. Keine ziehbare Trennlinie, kein Container-Query, keine
Animation.

**Abnahme:** Neun der zehn Punkte sind im Browser geklickt und belegt, der zehnte — dass der
Umschalter unter 1280 px nicht erscheint — **ist nicht gesehen worden**: Die Browsersteuerung kann
das Fenster nicht verkleinern. Er ist über die Regel belegt (`.hidden` ohne Bedingung gegen
`.xl\:inline-flex` in `@media (min-width:80rem)`) und als solcher gekennzeichnet. Vollständig in
[`nachrichtendetail.md`](nachrichtendetail.md) §10.15.

**Dokumentation:** [`nachrichtendetail.md`](nachrichtendetail.md) §10.7 (gekennzeichnet und
datiert, nicht überschrieben), §10.9 und §10.15; [`visuelles-konzept.md`](visuelles-konzept.md) §5.

---

## Schritt 7 — Durchstich: BAM-Suche

**Ziel:** Der Einstieg für den Fachanwender. Belegnummer eingeben, Nachricht finden.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Suchendpunkt über `MessageBAM` mit dem eigenen Index auf `MessageBAMValue`
  (10,9 Millionen Zeilen, 7,1 GB)
- **`MessageBAM` hat keinen Zeitstempel.** Das Pflicht-Zeitfenster greift erst nach dem Join auf
  `Message`, also nach dem teuren Teil. Ein `LIMIT` vor dem Join schneidet die falschen Zeilen ab.
  Zuerst messen, dann entscheiden. Rückfalloption, falls die Messung nicht trägt: ein eigener
  BAM-Index in `overlord_monitor` mit Wert, Typ, `MessageID`, Zeitstempel und Mandant, vom
  Rollup-Job mitgeführt (Annahme A9)
- **Hartes Ergebnislimit und Mindestlänge des Suchbegriffs.** Werte wie `050` kommen
  millionenfach vor
- Sichtbare BAM-Typen und deren Reihenfolge aus `MessageBAMMandant` je Mandant
- Optionale Einschränkung auf einen BAM-Typ
- Mandantenfilter über den Join auf die Nachricht

**Frontend**
- Prominente Suche im Anwendungsrahmen, auf jeder Seite erreichbar
- Ergebnisliste mit Sprung ins Detail
- Klare Rückmeldung bei zu kurzem Suchbegriff oder abgeschnittenem Ergebnis

**Abgrenzung:** Keine unscharfe Suche, keine Suche über `MessageProperty`.

**Abnahme:** Eine bekannte Lieferscheinnummer findet die richtige Nachricht. Ein dreistelliger
Suchbegriff wird abgelehnt statt die Datenbank zu belasten. Laufzeit gemessen.

**Dokumentation:** `docs/bam-suche.md`

---

## Schritt 8 — Durchstich: Rohdaten-Download

**Ziel:** Die ursprüngliche EDI-Datei ist herunterladbar, kontrolliert und protokolliert.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Auflösung von `Message.Payload.GUID` im Format `<FilestoreID>|<UUID>`
- Auflösung der `FilestoreID` über `Service` und `ServiceConnectString`
- **Proxy-Endpunkt, niemals ein durchgereichter Link.** Erst Mandantenprüfung, dann Abruf,
  dann Weiterleitung des Datenstroms
- `Content-Disposition: attachment` und `Content-Type: application/octet-stream`, **niemals
  inline**
- Jeder Download mit Nutzer, Nachricht, Zeitpunkt und IP ins `audit_log`
- Berechtigungsflag an `app_user`, Standardwert erlaubt
- Zeitüberschreitung und Größenbegrenzung beim Abruf vom Filestore

**Frontend**
- Download-Schaltfläche im Detailpanel, deaktiviert wenn keine Nutzdaten vorliegen
- Verständliche Meldung, wenn der Filestore nicht erreichbar ist

**Abgrenzung:** Keine Anzeige oder Aufbereitung im Browser. Nur Download.

**Abnahme:** Die heruntergeladene Datei stimmt mit dem Altsystem überein. Der Versuch, eine
fremde Nachricht herunterzuladen, liefert 404. Der Eintrag im Protokoll existiert.

**Dokumentation:** `docs/rohdaten-download.md`

---

## Schritt 9 — Durchstich: Administration

**Ziel:** Das System ist ohne Datenbankzugriff betreibbar, und der Prozess-Katalog ist gefüllt.

**Skills:** frontend-design, shadcn/ui

**Backend**
- `process_catalog` und `partner` per Flyway
- Einmaliger Heuristik-Import: Vorschläge für Partner, Standort, Richtung und Belegart aus
  Projekt-, Prozess- und SOS-Namen. **Vorschläge, keine Wahrheit** — jeder Eintrag trägt einen
  Pflegestatus
- Pflegeliste absteigend nach Nachrichtenaufkommen der letzten 30 Tage
- **Massenzuordnung nach Projekt.** Bei Mandanten, deren Projekte den Partner tragen, wird das
  der Haupthebel
- Sonderbehandlung von `00001_Undefined`
- Benutzerverwaltung: anlegen, sperren, Rolle ändern, Passwort zurücksetzen

**Frontend**
- Administrationsbereich, nur für die Rolle `ADMIN` sichtbar
- Katalogpflege mit Massenzuordnung und sichtbarem Fortschritt
- Benutzerverwaltung

**Abgrenzung:** Keine Selbstregistrierung, kein Passwort-Reset per E-Mail.

**Abnahme:** Ein Admin legt einen Nutzer an, der sich anmelden kann. Die Prozesse mit dem höchsten
Aufkommen sind zugeordnet, der Fortschritt ist ablesbar.

**Dokumentation:** `docs/prozess-katalog.md`, `docs/benutzerverwaltung.md`

---

## Schritt 10 — Durchstich: Rollup, Process View und Dashboard

**Ziel:** Die Landingpage nach der Anmeldung, plus die nach Partner gruppierte Prozessansicht.

**Skills:** frontend-design, shadcn/ui

**Backend**
- `message_rollup` je Stunde, Mandant, Prozess, Partner, Richtung und Status
- Stündlicher Job, inkrementell und gedrosselt, plus einmaliger Rückwärtslauf
- **Dashboard-Kennzahlen ausschließlich aus dem Rollup**, niemals live über `Message`
- Kennzahlen: Volumen im Zeitverlauf, die drei getrennten Problemkategorien, Verteilung nach
  Partner und Richtung, Fehler nach Art aus dem Teil hinter `ERROR_`
- Nicht zugeordnete Prozesse werden als eigene Kategorie ausgewiesen, nie stillschweigend verteilt
- Process View: gruppiert nach kuratiertem Partner, Projekt als Filter

**Frontend**
- Dashboard als Landingpage, Kacheln führen gefiltert in die Nachrichtenliste
- Verlaufsdiagramm mit Umschaltung Stunde, Tag, Monat
- Process View
- Sichtbarer Stand der letzten Aktualisierung

**Abgrenzung:** Keine frei konfigurierbaren Dashboards, keine Alarmierung.

**Abnahme:** Das Dashboard lädt in unter 500 Millisekunden. Die Zahlen stimmen stichprobenartig
mit einer direkten Abfrage überein. Ein Klick auf eine Fehlerkachel führt in die gefilterte Liste.

**Dokumentation:** `docs/rollup.md`, `docs/dashboard.md`, `docs/process-view.md`

---

## Hinweise zum Ablauf

**Schritt 5 kann zu groß werden.** Falls die Übersetzung der Prozessschritte umfangreicher gerät
als erwartet, wird geteilt: erst die Detailansicht mit Rohwerten, dann die Übersetzung.

**Reihenfolge ist nicht beliebig.** Schritte 1 bis 3 sind Voraussetzung für alles Weitere.
Schritt 10 setzt den gefüllten Katalog aus Schritt 9 voraus, sonst zeigt das Dashboard
überwiegend "nicht zugeordnet".

**Die CI erreicht die Datenbank nicht.** Das Repository liegt in der Cloud, die Testkopie im
internen Netz. Deshalb gilt ab Schritt 2 dauerhaft: generierte jOOQ-Quellen sind eingecheckt,
datenbankgebundene Tests tragen `@Tag("db")` und werden in der CI ausgeschlossen. Jeder neue Test,
der eine Verbindung braucht, bekommt diese Markierung — sonst steht die Pipeline rot.

**Der Rollup in Schritt 10 ist kein Zwang, sondern eine Entlastung.** Bei 3,3 Millionen Zeilen in
`Message` wäre Live-Aggregation technisch möglich; das Altsystem macht sie tagesweise. Der Rollup
existiert für Ladezeit und Ruhe auf der Produktionsdatenbank. Diese Begründung bitte so
verwenden — die frühere Zahl von 36 Millionen Zeilen war falsch.

**Zum Messen braucht es einen Kommandozeilen-Client.** `mariadb` ist installiert, aber der Server
bietet kein TLS an — **jeder Aufruf braucht `--skip-ssl`**, sonst bricht der Client mit
„SSL is required" ab. Das sieht aus wie ein Rechte- oder Netzwerkproblem und ist keines.

**Offene Messung aus Schritt 2:** `docs/message-status.md` behauptet, die `LEFT`-Form der
Fehlerbedingung könne `MessageStatusIDX` nicht nutzen — bislang ohne `EXPLAIN`-Beleg. Wird in
Schritt 4 nachgeholt, wo die Bedingung erstmals in einer echten Abfrage landet.

**Nach jedem Schritt gilt:** Isolationstest grün, Abfrage gegen die Testkopie gemessen,
Dokumentation aktualisiert. Erst dann ist der Schritt fertig.
