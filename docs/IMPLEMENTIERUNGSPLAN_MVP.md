# Overlord Monitoring — Implementierungsplan MVP

Stand: 24.07.2026 · Ergänzt `PROJEKTBESCHREIBUNG.md`

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

**Inhalt**
- Zwei DataSources mit zwei DB-Benutzern: `GlassfishDB` nur lesend, `overlord_monitor` schreibend
- jOOQ-Codegenerierung aus beiden Schemata, in **zwei getrennte Zielpakete**:
  `…monitor.jooq.glassfish` (nur lesend) und `…monitor.jooq.monitor` (schreibend). Gleichnamige
  Tabellen kollidieren dadurch nicht, und der Import zeigt die Schreibrichtung
- ArchUnit-Test: Klassen aus `jooq.glassfish` werden ausschließlich in Repository-Klassen
  verwendet, nicht in Services oder Controllern. Das Schreibverbot selbst prüft ArchUnit nicht —
  dafür sorgen die DB-Rechte, der als `readOnly` markierte Pool und ein jOOQ-`ExecuteListener`,
  der auf dem Lese-`DSLContext` alles außer `SELECT` abweist
- Flyway ausschließlich für `overlord_monitor`, erste Migration mit `audit_log`
- `TimeProvider`-Interface: Produktion Systemuhr, Dev-Profil `MAX(Message.MessageLastUpdate)`
- Einheitliches API-Fehlerformat, globaler Exception-Handler
- Healthcheck, der beide Verbindungen prüft
- Ein Smoke-Test, der Mandanten und Projekte aus der Testkopie liest

**Abgrenzung:** Keine fachlichen Endpunkte, keine Authentifizierung.

**Abnahme:** Healthcheck grün, Smoke-Test liefert echte Mandanten. Ein Schreibversuch auf
`GlassfishDB` scheitert an fehlenden Rechten — das wird als Test festgehalten.

**Dokumentation:** `docs/datenzugriff.md`

---

## Schritt 3 — Durchstich: Anmeldung und Mandantentrennung

**Ziel:** Man kann sich anmelden und landet auf einer geschützten, noch leeren Seite.

**Skills:** frontend-design, shadcn/ui

**Backend**
- `app_user` und `app_user_mandant` per Flyway
- BCrypt Kostenfaktor 12, Spring Security, serverseitige Session
- Cookie mit `HttpOnly`, `Secure`, `SameSite=Lax`
- Sperre nach fünf Fehlversuchen für 15 Minuten, zusätzlich Begrenzung pro IP
- Unspezifische Fehlermeldungen
- `SELECT DISTINCT MessageStatus` einmalig ausführen und das Ergebnis dokumentieren
- Migrationsskript für Altnutzer: Name, Mandant, Rolle übernehmen, Konten gesperrt anlegen
- Endpunkt für den angemeldeten Nutzer inklusive Rolle
- Der `MandantContext` wird aus der Session aufgelöst und ist ab hier Pflichtparameter jeder
  Repository-Methode

**Frontend**
- Anwendungsrahmen: Navigation, Kopfzeile, Nutzermenü, Abmelden
- Anmeldeseite, geschützte Routen, Weiterleitung
- Erste Umsetzung des visuellen Konzepts

**Abgrenzung:** Keine Benutzerverwaltungsoberfläche, kein Passwort-Reset. Nutzer werden vorerst
per Skript angelegt.

**Abnahme:** Anmeldung funktioniert, Abmeldung beendet die Session, unangemeldeter Aufruf einer
geschützten Route leitet um, sechster Fehlversuch sperrt.

**Dokumentation:** `docs/authentifizierung.md`, `docs/mandantentrennung.md`

---

## Schritt 4 — Durchstich: Nachrichtenliste

**Ziel:** Der Nutzer sieht echte Nachrichten seines Mandanten.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Listen-Endpunkt mit Pflicht-Zeitfenster (Standard 24 Stunden, Maximum ein Jahr)
- Cursor-Paginierung über `(MessageLastUpdate, MessageID)`, kein `OFFSET`
- Filter: Status, Prozess, Freitext auf Prozessname
- Statusabbildung: `ERROR_*` gleich Fehler, `SUSPENDED` gleich Warten, Timeout-Überschreitung
  berechnet aus `MessageLastUpdate + MessageTimeout`
- Mandantenfilter fest im Statement über `ProjectMandant`
- Isolationstest: Mandant A fragt ab, Daten von Mandant B sind unerreichbar

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
- **Übersetzung der `SOSActionServiceProperties` in Klartext.** Aus `NXS_FILE_CONVERT|E2A|UNWRAP`
  wird "Datei konvertiert", aus `NXS_MERGE|...|WAIT|30M` wird "wartet auf Zusammenführung,
  30 Minuten". Die Zuordnungstabelle wird gepflegt, unbekannte Bausteine erscheinen als Rohwert —
  nie geraten
- Anzeigename aus `SOS.SOSName`

**Frontend**
- Detailansicht als seitliches Panel, damit die Liste im Blick bleibt
- Schrittfolge als Zeitleiste mit Dauer je Schritt und deutlicher Markierung des Hängers
- Technische Eigenschaften eingeklappt im Hintergrund

**Abgrenzung:** Keine Verkettung zu anderen Nachrichten, kein Rohdaten-Download.

**Abnahme:** Für eine bekannte Nachricht stimmt die Schrittfolge mit dem Altsystem überein. Bei
einer hängenden Nachricht ist ohne Fachwissen erkennbar, wo sie steht.

**Dokumentation:** `docs/nachrichtendetail.md`, `docs/prozessschritte-uebersetzung.md`

---

## Schritt 6 — Durchstich: Verkettung

**Ziel:** Die Frage "wo ist mein Lieferschein" wird über Aufteilung und Zusammenführung hinweg
beantwortet.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Auflösung über `SourceMessageID`, `TargetMessageID` sowie die Flags `Source` und `Target`
- Vorgänger und Nachfolger, begrenzte Tiefe gegen Endlosketten
- Quittungszuordnung einbeziehen (`COMMIT_RECEIVED`, `EERP_RECEIVED`)
- Mandantenfilter gilt auch für verkettete Nachrichten
- Hinweis auf die stündliche Verzögerung des `MatchInterchange`-Events mitliefern

**Frontend**
- Darstellung der Kette im Detailpanel, aktuelle Nachricht hervorgehoben
- Navigation entlang der Kette
- Sichtbarer Hinweis, dass Quittungen bis zu eine Stunde verzögert eintreffen

**Abgrenzung:** Keine Graphenvisualisierung. Eine Liste oder ein einfacher Baum genügt.

**Abnahme:** Für eine aufgeteilte Nachricht sind alle Teile erreichbar. Für eine quittierte
Nachricht ist die Quittung sichtbar.

**Dokumentation:** `docs/verkettung.md`

---

## Schritt 7 — Durchstich: BAM-Suche

**Ziel:** Der Einstieg für den Fachanwender. Belegnummer eingeben, Nachricht finden.

**Skills:** frontend-design, shadcn/ui

**Backend**
- Suchendpunkt über `MessageBAM` mit dem eigenen Index auf `MessageBAMValue`
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

**Nach jedem Schritt gilt:** Isolationstest grün, Abfrage gegen die Testkopie gemessen,
Dokumentation aktualisiert. Erst dann ist der Schritt fertig.
