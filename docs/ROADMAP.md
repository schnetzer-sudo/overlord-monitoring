# Roadmap

*Angelegt am 16.10.2026 mit Version 1.0.0, nachgeführt am 21.10.2026 mit 1.1.0.* Hier steht die
Planung ab 1.0.0: welche Linien es gab und gibt, was kommt und wie ein Arbeitsauftrag aussieht. Der
[`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) ist abgeschlossen und wird nicht mehr
fortgeschrieben (Entscheidung 10 vom 16.10.2026). Zweige, Versionen und Nummern regelt
[`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12.

---

## 1. Eine Linie auf `main` *(seit 1.1.0, 21.10.2026)*

**Die beiden Linien sind zusammengeführt** (E‑1125, E‑1127, [`tasks.md`](tasks.md) §2). Von 1.0.0
bis 1.1.0 war `main` die Release-Linie 1.x nur mit Fixes und `assistent` die Weiterentwicklung; seit
dem Zusammenführen kommt beides auf `main`, über Zweige `fix/<thema>` und `feat/<thema>`. Fixes
kommen weiter aus dem Register [`offene-punkte.md`](offene-punkte.md): Ein Punkt wird aufgegriffen,
behoben, an beiden Stellen als erledigt nachgeführt und mit dem nächsten Patch-Release (1.1.1, …)
ausgeliefert. Was ein Fix ist und was schon Weiterentwicklung, entscheidet Lukas.

Migrationen schließen an die höchste vergebene an (`V104` als nächste), projektweite Nummern
ebenso; der Bereich unter 1000 wird nicht weiter befüllt (E‑1127). Der Zweig `assistent` bleibt
stehen und bekommt keine Arbeit mehr.

---

## 2. Aufträge über den Assistenten (bis 1.1.0 die Linie `assistent`)

Die Ausbaustufe, die mit Release **1.1.0** nach `main` gekommen ist. **Aufträge entstehen nur im
Chat**; es gibt **keinen Freigabeschritt**. **Die Auftragstabelle liegt in `overlord_monitor`**, und
die Plattform holt die Aufträge dort ab. Die Details stehen in [`tasks.md`](tasks.md), der Vertrag
für die Plattform in [`task-schnittstelle.md`](task-schnittstelle.md).

| Teil | Inhalt | Stand |
|---|---|---|
| **A1** | Auftragsmodell: drei Tabellen (`V100`), Typdefinitionen als Daten mit Prüfung beim Laden, der Typ `WERK_ABLADESTELLE`, vier Endpunkte unter `/api/tasks`, Plattformvertrag | fertig, am 16.10.2026 nach `assistent` gemergt |
| **A2** | Assistent im Backend: anbieterneutrale Schnittstelle mit Adapter für die Anthropic-API, Werkzeuge aus den Typdefinitionen, Gesprächsführung, `POST /api/tasks/pruefung`, Testsatz ([`assistent.md`](assistent.md)) | fertig, am 16.10.2026 nach `assistent` gemergt; Nachschärfung am 19.10.2026 gemergt |
| **A3** | Chatbox in der Kopfleiste und Bestätigungskarte: Symbol links neben dem Nutzermenü, Chatbox als Overlay, Karte mit Prüfung über `POST /api/tasks/pruefung` und Anlegen per Klick; kein Backend-Umbau ([`assistent-frontend.md`](assistent-frontend.md)) | fertig, am 19.10.2026 nach `assistent` gemergt; Sichtprüfungen 1018 bis 1022 mit eingeschaltetem Assistenten offen |
| **A4** | „Ihre Aufträge“ in der Übersicht: Stand jedes Auftrags, Ergebnis je Eintrag, Löschen offener Aufträge über `DELETE /api/tasks/{taskId}` ([`assistent-frontend.md`](assistent-frontend.md) §12, [`tasks.md`](tasks.md) §6.6) | fertig, am 21.10.2026 nach `assistent` gemergt (E‑1129); F16 bis F29 zur Bestätigung, Sichtprüfungen 1028 bis 1032 offen |
| **1.1.0** | Zusammenführung nach `main`; Freischaltung je Mandant an einer Stelle, nicht verfügbar ist `404`; `V103` nimmt die Freischaltungen aus `V100` zurück; `SYSTEM` per Skript; Zeitgrenze der Weiterleitung 300 s ([`tasks.md`](tasks.md) §3.9, [`betrieb.md`](betrieb.md) §5a) | auf `main` am 21.10.2026; F30 bis F38 zur Bestätigung; den Tag setzt Lukas |

**Mit 1.1.0 ist die Ausbaustufe Aufträge ausgeliefert, sichtbar aber nur für freigeschaltete
Mandanten, zunächst nur `SYSTEM`** (E‑1125). Die Prüfung durch den Datenschutzbeauftragten ist
erledigt (Punkt 1009, E‑1131). Was nicht im Code liegt:

1. **Der Test gegen die Produktion mit `SYSTEM`:** Schalter und Schlüssel setzen, das Skript für
   `SYSTEM` ausführen ([`tasks.md`](tasks.md) §3.9), beides nach der Deployment-Routine
   ([`betrieb.md`](betrieb.md) §7a, Schritte A bis C), die ausgehende Verbindung (Punkt 1010) und die
   Zeitgrenze des Reverse Proxy (Punkt 1033) prüfen, die Sichtprüfungen 1018 bis 1022 und 1028 bis
   1032 dort nachholen.
2. **Der Ablauf in der Plattform** für `WERK_ABLADESTELLE`, gebaut nach
   [`task-schnittstelle.md`](task-schnittstelle.md). Dazu gehört Punkt 1003 (UTF-8 und LF). Der Typ
   ist seit `V100` aktiv; ein Auftrag, den niemand abholt, bleibt „Offen“ und lässt sich löschen.
3. **Das Ausrollen auf Kundenmandanten:** je Mandant ein `INSERT` in `task_type_mandant`, kein
   Deployment (E‑1126, [`tasks.md`](tasks.md) §3.9).

---

## 3. Weitere Ausbaustufen

Nicht geplant, nicht terminiert. Sie stehen hier mit den Vorbehalten, die die Projektbeschreibung
bis 1.0.0 dazu führte ([`archiv/PROJEKTBESCHREIBUNG_bis-1.0.0.md`](archiv/PROJEKTBESCHREIBUNG_bis-1.0.0.md) §10).

1. **Fragen an den Chatbot** — etwa „gab es heute Fehler bei der Übertragung an Partner XY?“.
   **Kein rohes Text-to-SQL:** Bei EAV und Namenskonventionen erfindet jedes Modell plausible
   Zahlen. Stattdessen Werkzeugaufrufe auf dieselben Endpunkte, die das Frontend nutzt; deshalb ist
   die API kennzahlenorientiert geschnitten.
2. **Alarmierung bei Fehlern.** Die Kategorie *Überfällig*, die hier früher mitgenannt war, ist seit
   E‑71 entfallen ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §4.2).
3. **Service-Überwachung für die interne Betreuung, soweit nicht mit 10d gebaut.** Gebaut ist die
   **Anzeige** des Dienstzustands und der Ablagen auf dem Dashboard ([`dienste.md`](dienste.md)).
   Offen ist, was die Stufe eigentlich meint: Alarmierung, Schwellen, eine Historie, eine
   Betriebssicht mit mehr als der `ServiceID`.
4. **SOS-Baukasten** — Nutzer stellen sich Abläufe aus `SOSAction`-Bausteinen selbst zusammen.
   **Vorbehalt:** Das macht aus dem Werkzeug ein Konfigurationssystem mit Schreibzugriff auf das
   Altschema, und das verbietet heute Regel 3 in [`CLAUDE.md`](../CLAUDE.md) ohne Ausnahme. Ein
   solches Schreibmodul wäre ein eigener, separat berechtigter Baustein und bräuchte eine neue
   Entscheidung; der bestehende Lesepfad wird dafür nicht aufgeweicht.

---

## 4. Vorlage für einen Arbeitsauftrag

Jeder Schritt wird ein eigener Auftrag an Claude Code. Vor dem Auftrag steht ein kurzes Sparring:
was genau, welche Abgrenzung, welches Abnahmekriterium.

```
# <Titel>

Zweig: <fix/<thema> oder feat/<thema>>, abgezweigt von main.

## Worum es geht
<zwei, drei Sätze: Ziel und Anlass>

## Zuerst lesen
Lies zunächst die unten genannten Dateien und ermittle anhand der angegebenen Abschnitte, welche
Teile für die Umsetzung tatsächlich relevant sind. Lies vollständige Dateien nur dort, wo
Abhängigkeiten oder widersprüchliche Regeln dies erforderlich machen. Bei Dateien über 500 Zeilen
zuerst die Gliederung (`grep -n "^## \|^### " <datei>`), dann gezielt lesen.
1. CLAUDE.md
2. DEVELOPMENT_GUIDELINES.md: <Abschnitte>
3. docs/<relevante Dateien und Abschnitte>
Widerspricht eine gelesene Regel diesem Auftrag: anhalten und nachfragen.

## Entscheidungen
<getroffene Entscheidungen mit Datum und Herkunft>

## Aufgabe
Backend: <…>
Frontend: <…>

## Verbindliche Regeln
<die berührten Regeln, wörtlich>

## Abnahme
<prüfbares Ergebnis; Messung nach L7, Isolationstest je Endpunkt>

## Dokumentation
<neue oder zu aktualisierende Datei in docs/, Register offene-punkte.md, CHANGELOG.md>

## Dev-Zeile
<Umgebung: Windows/PowerShell, Testkopie ja/nein, was in die Testkopie geschrieben werden darf>

## Skills
<nur die für diesen Schritt relevanten, z. B. frontend-design, shadcn; sonst „keine“>

## Was nicht gebaut wird
<ausdrücklich ausgeschlossen>

## Rückmeldung
Commits mit Hash, Messungen mit M-Nummer, neue offene Punkte mit Nummer, jede Abweichung mit Grund.
```

**Die Dokumentationspflicht steht in jedem Auftrag.** Ein Schritt gilt erst als fertig, wenn die
Dokumentation steht ([`CLAUDE.md`](../CLAUDE.md), Dokumentationspflicht).
