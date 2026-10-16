# Roadmap

*Angelegt am 16.10.2026 mit Version 1.0.0.* Hier steht die Planung ab 1.0.0: welche Linien es gibt,
was auf ihnen kommt und wie ein Arbeitsauftrag aussieht. Der
[`IMPLEMENTIERUNGSPLAN_MVP.md`](IMPLEMENTIERUNGSPLAN_MVP.md) ist abgeschlossen und wird nicht mehr
fortgeschrieben (Entscheidung 10 vom 16.10.2026). Zweige, Versionen und Nummern regelt
[`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12.

---

## 1. Linie 1.x auf `main`

**Nur Fixes, ohne eigenen Plan.** Die Fixes kommen aus dem Register
[`offene-punkte.md`](offene-punkte.md): Ein Punkt wird aufgegriffen, auf einem Zweig `fix/<thema>`
behoben, an beiden Stellen als erledigt nachgeführt und mit dem nächsten Patch-Release (1.0.1,
1.0.2, …) ausgeliefert. Was ein Fix ist und was schon Weiterentwicklung, entscheidet Lukas; im
Zweifel gehört es auf `assistent`.

---

## 2. Linie `assistent`: Aufträge über den Assistenten

Die Ausbaustufe, die als Release **1.1.0** nach `main` kommt. **Aufträge entstehen nur im Chat**;
es gibt **keinen Freigabeschritt**. **Die Auftragstabelle liegt in `overlord_monitor`**, und die
Plattform holt die Aufträge dort ab. Die Details stehen in [`tasks.md`](tasks.md), der Vertrag für
die Plattform in [`task-schnittstelle.md`](task-schnittstelle.md).

| Teil | Inhalt | Stand |
|---|---|---|
| **A1** | Auftragsmodell: drei Tabellen (`V100`), Typdefinitionen als Daten mit Prüfung beim Laden, der Typ `WERK_ABLADESTELLE`, vier Endpunkte unter `/api/tasks`, Plattformvertrag | gebaut auf `assistent-a1-auftragsmodell` (16.10.2026), Merge nach `assistent` offen |
| **A2** | Assistent im Backend | offen |
| **A3** | Chatbox in der Kopfleiste | offen |
| **A4** | „Ihre Aufträge“ in der Übersicht | offen |

Für diese Linie gelten Migrationen ab `V100` und projektweite Nummern ab 1000
([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12).

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

Zweig: <assistent-<teil> oder fix/<thema>>, abgezweigt von <assistent oder main>.
(Teilschritte mit Bindestrich: Git kann neben `assistent` keinen Zweig `assistent/<teil>` anlegen.)

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
