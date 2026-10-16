# Assistent im Backend (A2)

*Angelegt am 16.10.2026 mit Teilschritt A2 der Linie `assistent`* (Zweig
`assistent-a2-assistent-backend`, [`ROADMAP.md`](ROADMAP.md) §2). Er baut auf dem Auftragsmodell aus
A1 auf ([`tasks.md`](tasks.md)).

A2 baut das Backend des Assistenten: die Anbindung an ein Sprachmodell hinter einer eigenen,
anbieterneutralen Schnittstelle (zuerst Anthropic), die Werkzeuge des Modells aus den
Typdefinitionen, die Gesprächsführung mit Prüfung jedes Vorschlags, eine Prüfung ohne Anlegen für
die Karte und einen Testsatz, der misst, wie gut ein Modell die Anfragen versteht. **Keine
Oberfläche**; Chatbox und Karte folgen mit A3 (§12).

> **Der Assistent ist abgeschaltet**, bis der Datenschutzbeauftragte die Auftragsverarbeitung mit
> Anthropic geprüft hat (E‑1029, E‑1030, Punkt 1009). Abgeschaltet heißt: Kein einziger Aufruf geht
> an den Anbieter.

---

## 1. Zweck und Ablauf

Der Kunde öffnet in der Kopfleiste den Assistenten und schreibt in eigenen Worten, was er ändern
will. Der Assistent sammelt die Werte und schlägt einen Auftrag vor; der Kunde sieht den Vorschlag
als Karte, kann Felder ändern und legt den Auftrag mit einem Klick an.

1. **Nachricht.** Der Client schickt den Text des Kunden an
   `POST /api/assistent/gespraeche/{id}/nachrichten`. Das Backend prüft die Grenzen (§7) und ruft
   das Modell mit der festen Anweisung, dem bisherigen Verlauf und den Werkzeugen.
2. **Werkzeugaufruf.** Hat das Modell alle Angaben, reicht es einen Vorschlag über das Werkzeug des
   Typs ein. Jedes Werkzeug hat **keine Nebenwirkung**: Es nimmt nur einen Vorschlag entgegen
   (E‑1033).
3. **Prüfung.** Das Backend prüft den Vorschlag mit der Prüfung aus A1 (`AuftragsPruefung`,
   [`tasks.md`](tasks.md) §5.3). **Das Modell beurteilt Werte nicht selbst.**
4. **Rückfrage oder Karte.** Fehler gehen als Werkzeugergebnis an das Modell zurück, das daraus
   seine Rückfrage formuliert. Ein gültiger Vorschlag geht als `vorschlag` an den Client, zusammen
   mit dem Text des Modells.
5. **Klick.** Der Kunde prüft die Karte. Ändert er Felder, prüft die Karte über
   `POST /api/tasks/pruefung` (E‑1042). Der Klick schickt `POST /api/tasks` aus A1 ab.
   **Anlegen tut immer der Klick, nie das Modell.**

```
Kunde ──Text──▶ Backend ──Anweisung, Verlauf, Werkzeuge──▶ Modell
                   ▲                                         │
                   └──── Werkzeugaufruf (Vorschlag) ◀────────┘
                   │
              AuftragsPruefung
              ├─ Fehler ──▶ Werkzeugergebnis (is_error) ──▶ Modell ──▶ Rückfrage an den Kunden
              └─ gültig ──▶ Karte an den Client ──Klick──▶ POST /api/tasks
```

---

## 2. Entscheidungen

Herkunft: **A** = vom Auftraggeber am 16.10.2026 entschieden. **V** = Vorschlag vom 16.10.2026, ohne
Einspruch übernommen. Die E‑Nummer ist `E‑1027` plus die Nummer der Entscheidung im Auftrag zu A2
(nächste freie Nummer nach A1 war E‑1028, per `grep` über das Repository geprüft).

### Anbieter

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1028** | Erster Anbieter ist die **Anthropic-API direkt**. Modell per Konfiguration, Vorgabe `claude-sonnet-5-5`. Gebaut wird so, dass ein anderer Anbieter ohne Änderung an Gesprächsführung, Werkzeugen und Endpunkten angebunden werden kann. **Langfristiges Ziel ist ein lokal betriebenes Modell.** | A |
| **E‑1029** | **Folge von E‑1028:** Bei der Anthropic-API direkt gibt es keine Verarbeitung oder Speicherung in der EU (Verarbeitung nur `us` oder `global`, Speicherung nur `us`; Stand 16.10.2026). Was der Kunde schreibt, geht in die USA. Grundlage wäre das EU-US Data Privacy Framework. Der Assistent ist deshalb **abgeschaltet, bis der Datenschutzbeauftragte die Auftragsverarbeitung geprüft hat** (E‑1030). | A (Anbieter), V (Folge) |
| **E‑1030** | Schalter `aktiv`, Vorgabe **aus**. Aus heißt: Die Endpunkte melden „nicht verfügbar“, es geht kein einziger Aufruf an den Anbieter. Ohne API-Schlüssel gilt der Assistent ebenfalls als aus. | V |
| **E‑1031** | Die Anbindung läuft über einfaches HTTP mit dem `RestClient` von Spring und Jackson 3, **ohne SDK des Anbieters**. Gründe: Die Schnittstelle ist klein; ein SDK bringt eigene Abhängigkeiten, möglicherweise Jackson 2, das hier ausgeschlossen ist; und ein lokales Modell wird später ebenfalls über HTTP angesprochen. | V |
| **E‑1032** | Die Schnittstelle zum Modell ist **anbieterneutral**: eigene Typen für Anweisung, Verlauf, Werkzeug, Werkzeugaufruf, Werkzeugergebnis und Antwort. Alles Anthropic-Spezifische liegt in einem eigenen Unterpaket. Eine ArchUnit-Regel hält das. | V |

### Gesprächsführung und Sicherheit

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1033** | Das Modell hat **nur Werkzeuge ohne Nebenwirkung**: je angebotenem Auftragstyp eines, das einen Vorschlag einreicht. Das Backend prüft jeden Vorschlag mit der Prüfung aus A1. Fehler gehen als Werkzeugergebnis an das Modell zurück, das daraus seine Rückfrage formuliert. Ein gültiger Vorschlag geht an den Client als Karte. **Das Modell legt nie an und beurteilt Werte nicht selbst.** | A (Anlegen nur per Klick), V |
| **E‑1034** | Werkzeuge entstehen aus der Struktur des Typs: Name ist die Kennung des Typs, die Eingabe ist ein JSON Schema mit `aktion` als Aufzählung und `eintraege` als Liste von Objekten mit den Feldern des Typs (Beschreibung je Feld aus `label`, `hinweis`, `beispiel`, `muster`, `nurBei`; `maxItems` aus `maxEintraege`). **Kein Werkzeug hat einen Mandantenparameter.** Im Hauptcode kennt weiterhin keine Stelle einen bestimmten Typ. | V |
| **E‑1035** | **Datensparsamkeit.** Das Modell bekommt die feste Anweisung, Name, Beschreibung und Struktur der angebotenen Typen, was der Kunde schreibt, und die Prüfergebnisse. Es bekommt **nicht**: Benutzername, Mandanten-ID, Daten aus `GlassfishDB`, Aufträge aus `task`. | V |
| **E‑1036** | **Grenzen**, alle konfigurierbar, mit diesen Vorgaben: Nachricht des Kunden höchstens 4.000 Zeichen; höchstens 20 Nachrichten des Kunden je Gespräch; höchstens 3 Werkzeugrunden je Nachricht; höchstens 60 Nachrichten je Nutzer und Stunde, im Arbeitsspeicher wie die Begrenzung je IP; Zeitgrenze je Aufruf 30 Sekunden; `max_tokens` 1.024. | V |
| **E‑1037** | Was der Assistent aus `task` braucht (Typdefinition, Typkatalog, Prüfung), **wandert nach `common`**, weil es einen zweiten Verbraucher bekommt ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6). Betrifft die Verschiebung mehr als diese drei Teile samt ihrer Hilfsklassen: anhalten und melden. | V |
| **E‑1038** | **Gespräche liegen nur im Arbeitsspeicher**, gebunden an Sitzung, Nutzer und aktiven Mandanten, mit Ablauf nach 30 Minuten ohne Nachricht und höchstens 3 offenen Gesprächen je Sitzung. Nicht in der Datenbank, nicht in der Sitzung, nicht im Log. Ein Neustart beendet alle Gespräche; ein Mandantenwechsel beendet die Gespräche dieser Sitzung. | V |
| **E‑1039** | **Protokoll nur mit Metadaten:** Nutzer, Gespräch, Modell, Token ein und aus, Dauer, Zahl der Werkzeugaufrufe, Ergebnis. Nie der Text des Kunden, des Modells oder eines Vorschlags. Kein Eintrag im `audit_log`; dort steht wie bisher das Anlegen eines Auftrags. | V |
| **E‑1040** | Die feste Anweisung an das Modell steht als versionierte Ressource im Repository, nicht im Code (§4.4). | V |
| **E‑1041** | Ein Vorschlag trägt **jedes Feld, das bei seiner Aktion vorkommt**, leere ausdrücklich als leer, dazu je Feld `label` und `hinweis` aus der Definition. So kann die Karte in A3 die Bedeutung eines leeren Felds zeigen, ohne einen Typ zu kennen. | V |
| **E‑1042** | Neuer Endpunkt **`POST /api/tasks/pruefung`**: dieselbe Prüfung wie beim Anlegen, ohne Anlegen. Grundlage für die Karte, wenn der Kunde Felder ändert. | V |
| **E‑1043** | **Kein Streaming** in A2. Die Antwort kommt vollständig. Ob A3 Streaming braucht, ist offen (Punkt 1011). | V |
| **E‑1044** | Der Assistent antwortet in der Sprache des Kunden, Vorgabe Deutsch mit „Sie“. Feldnamen und Aktionen nennt er so, wie sie in der Definition stehen. | V |

### Festlegungen der Umsetzung

Was der Auftrag offenließ und beim Bau festgelegt wurde. Vorgeschlagen mit A2 als F1 bis F13,
**vom Auftraggeber am 16.10.2026 bestätigt**; die E‑Nummer ist `E‑1044` plus die frühere Nummer.

| E | Festlegung | Grund | Herkunft |
|---|---|---|---|
| **E‑1045** | **Anweisung und Werkzeuge werden beim Eröffnen eines Gesprächs festgelegt** und bleiben für das Gespräch gleich. Geprüft wird jeder Vorschlag dagegen gegen das **aktuelle** Angebot des Mandanten. | Anthropic prüft bei Claude Sonnet 5.5, dass Anweisung, Werkzeuge und frühere Nachrichten unverändert sind, wenn Denkblöcke zurückgeschickt werden („preserved thinking“); eine Änderung mitten im Gespräch wäre für neuere Konten ein `400`. Ein inzwischen deaktivierter Typ wird so beim Einreichen abgewiesen | V, bestätigt am 16.10.2026 |
| **E‑1046** | Anthropic-Einstellung **`denken: between_tools`** (Vorgabe): das Modell denkt nicht ausführlich. | Denken zählt gegen `max_tokens`; bei 1.024 bliebe sonst wenig für Antwort und Vorschlag. Der Wert gilt nur für Claude Sonnet 5.5 und ist deshalb einstellbar (`adaptive`, `keins`) | V, bestätigt am 16.10.2026 |
| **E‑1047** | **Werte nur als Text.** Liefert das Modell eine Zahl, einen Wahrheitswert oder ein Objekt als Wert, geht das als Fehler an das Modell zurück; nichts wird umgewandelt. | Eine Zahl verlöre führende Nullen (`0101159903`); umwandeln hieße Werte beurteilen | V, bestätigt am 16.10.2026 |
| **E‑1048** | **Je Nachricht höchstens ein Vorschlag.** Ein weiterer Werkzeugaufruf nach einem gültigen Vorschlag in derselben Nachricht wird als Fehler beantwortet. | Ein Auftrag nach dem anderen (feste Anweisung); die Karte zeigt einen Vorschlag | V, bestätigt am 16.10.2026 |
| **E‑1049** | **Runden:** Nach höchstens 3 Werkzeugrunden muss das Modell mit Text antworten. Ruft es danach noch ein Werkzeug, endet die Nachricht mit einer festen Antwort ohne Karte; die Zwischenschritte dieser Nachricht werden verworfen. | Der Verlauf bleibt gültig (jeder Werkzeugaufruf hat sein Ergebnis) und wächst nur am Ende | V, bestätigt am 16.10.2026 |
| **E‑1050** | **Abgeschnitten:** Endet eine Antwort an `max_tokens` mit einem Werkzeugaufruf, ist dessen Eingabe unvollständig. Sie wird nicht geprüft; die Nachricht endet mit der festen Antwort, den Auftrag aufzuteilen. | Unvollständige Eingabe ist kein Vorschlag | V, bestätigt am 16.10.2026 |
| **E‑1051** | **Verweigert das Modell** (`stop_reason: refusal`), endet die Nachricht mit einer festen Antwort ohne Karte. Kein serverseitiger Rückfall auf ein anderes Modell (`fallbacks`). | Ein Rückfall schickte denselben Text an ein weiteres Modell und braucht einen Beta-Kopf; für Werk-/Abladestellen ist eine Verweigerung nicht zu erwarten | V, bestätigt am 16.10.2026 |
| **E‑1052** | **Fehler des Anbieters** lassen das Gespräch unverändert: Die Nachricht zählt nicht gegen die 20 je Gespräch und kann wiederholt werden. Gegen die 60 je Stunde zählt sie. | Die Stundengrenze schützt vor Last und Kosten, auch bei Fehlern | V, bestätigt am 16.10.2026 |
| **E‑1053** | **Ein viertes Gespräch derselben Sitzung** beendet das am längsten ruhende. | Ein Client, der Gespräche nicht beendet, sperrt sich sonst selbst aus | V, bestätigt am 16.10.2026 |
| **E‑1054** | Einen Typ, dessen Kennung kein gültiger Werkzeugname ist (`[A-Za-z0-9_-]{1,64}`) oder der ein Feld hat, dessen Name mit `mandant` beginnt, **bietet der Assistent nicht an** (`WARN` im Log). Über `POST /api/tasks` bleibt er anlegbar. | Anthropic nimmt nur solche Namen an; ein Feld `mandant…` sähe für das Modell wie ein Mandantenparameter aus (E‑1034) | V, bestätigt am 16.10.2026 |
| **E‑1055** | Der Assistent prüft **nicht** auf `ZU_LANG` ([`tasks.md`](tasks.md) §5.2). `POST /api/tasks/pruefung` und `POST /api/tasks` prüfen es. | Den Parametertext baut nur `task`. Eine Werkzeugeingabe ist durch `max_tokens` auf wenige KB begrenzt, die Grenze liegt bei 65.535 Bytes | V, bestätigt am 16.10.2026 |
| **E‑1056** | Der Schalter ist **in jedem Profil aus**, auch in `dev`. Eingeschaltet wird über `OVERLORD_ASSISTENT_AKTIV=true`, und auch dann nur mit Schlüssel. | Die Testkopie ist eine Vollkopie der Produktion; ein lokaler Start soll nicht unaufgefordert Text an den Anbieter schicken | V, bestätigt am 16.10.2026 |
| **E‑1057** | Ablauf der Gespräche und Begrenzung je Stunde rechnen mit der **Systemuhr** (`systemClock`), nicht mit der Anwendungsuhr. | Die Anwendungsuhr ist in `dev` um Monate zurückversetzt ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6, Zeit) | V, bestätigt am 16.10.2026 |

---

## 3. Datenfluss

### 3.1 Was an den Anbieter geht (E‑1035)

Je Aufruf des Modells, und **nur** bei eingeschaltetem Assistenten:

- die **feste Anweisung** (§4.4), darin **Name und Beschreibung** jedes angebotenen Typs aus
  `task_type`;
- je angebotenem Typ ein **Werkzeug** mit dessen Kennung und der Struktur der Felder (§4.5);
- der **Verlauf dieses Gesprächs**: was der Kunde geschrieben hat, die Antworten und
  Werkzeugaufrufe des Modells und die Prüfergebnisse des Backends (Fehlercode und Klartext je
  Fehler, oder die Meldung, dass der Vorschlag gültig ist).

### 3.2 Was nicht hingeht

**Nicht** der Benutzername, **nicht** die Mandanten-ID, **keine** Daten aus `GlassfishDB` und
**keine** Aufträge aus `task`. Das Paket `assistent` fasst keinen Datenbankzugriff an (ArchUnit,
§9); die Typen bekommt es aus dem Typkatalog in `common`. Kein Werkzeug hat einen
Mandantenparameter (E‑1034). Ein Test hält fest, dass die Anweisung weder Benutzername noch
Mandanten-ID enthält.

**Was der Kunde selbst schreibt, geht hin** — auch wenn er dort einen Namen oder eine Mailadresse
nennt. Das lässt sich nicht verhindern, nur in der Oberfläche (A3) mit einem Hinweis vermeiden.

### 3.3 Wohin (E‑1029)

An `api.anthropic.com` (Basis-URL konfigurierbar). Laut Auftrag vom 16.10.2026 gibt es bei der
Anthropic-API direkt keine Verarbeitung oder Speicherung in der EU: Verarbeitung nur `us` oder
`global`, Speicherung nur `us`. Der Monitor setzt keinen Ort (`inference_geo`), Anthropic wählt
also selbst. **Was der Kunde schreibt, geht in die USA**; Grundlage wäre das EU-US Data Privacy
Framework, die Prüfung durch den Datenschutzbeauftragten steht aus (Punkt 1009). Ob Anthropic die
Anfragen ohne Aufbewahrung verarbeiten kann (Zero Data Retention), ist Punkt 1015. Ob der
Produktionsserver `api.anthropic.com` überhaupt erreicht, ist ungeprüft (Punkt 1010).

### 3.4 Was gespeichert wird (E‑1038)

**Nur der Verlauf eines laufenden Gesprächs, nur im Arbeitsspeicher** des Backends
(`Gespraechsspeicher`): nicht in der Datenbank, nicht in der Sitzung, nicht im Log. Er endet

- 30 Minuten nach der letzten Nachricht,
- mit `DELETE /api/assistent/gespraeche/{id}`,
- wenn dieselbe Sitzung ein viertes Gespräch eröffnet (das am längsten ruhende, E‑1053),
- beim Wechsel des aktiven Mandanten (alle Gespräche dieser Sitzung),
- mit dem Ende der Sitzung (ab dann unerreichbar, gelöscht beim Ablauf) und
- mit jedem Neustart.

Was der Kunde mit der Karte anlegt, steht danach wie jeder Auftrag in `task` — das legt der Klick
an, nicht der Assistent ([`tasks.md`](tasks.md) §6.2).

### 3.5 Was protokolliert wird (E‑1039)

Je Nachricht des Kunden eine Zeile auf `INFO` (Logger `…assistent.AssistentService`):

```
Assistent: nutzer=<Benutzername> mandant=<Mandant> gespraech=<UUID> modell=<Modell> tokenEin=<n> tokenAus=<n> dauerMs=<n> werkzeugaufrufe=<n> ergebnis=<Ergebnis>
```

`ergebnis` ist `TEXT` (Antwort ohne Karte), `VORSCHLAG` (mit Karte), `RUNDEN_AUSGESCHOEPFT` (E‑1049),
`ABGESCHNITTEN` (E‑1050), `VERWEIGERT` (E‑1051) oder `FEHLER_<Art>` (§4.3). Der Mandant steht dort, weil
jeder Protokolleintrag ihn trägt ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §6);
ans Modell geht er nicht. **Nie** im Protokoll: der Text des Kunden, des Modells oder eines
Vorschlags, die Sitzungs-ID, der API-Schlüssel. Bei einem Fehler des Anbieters steht dessen
HTTP-Status und Fehlertyp im Protokoll, nicht sein Antworttext. **Kein Eintrag im `audit_log`**;
dort steht wie bisher `AUFTRAG_ANGELEGT` beim Klick.

---

## 4. Aufbau

### 4.1 Pakete

```
de.kraftwerkone.overlord.monitor
├─ common/auftrag/     aus task gewandert (E‑1037): TypDefinition, DefinitionsPruefung,
│                      TypKatalog, AuftragstypRepository, AuftragsPruefung, Prueffehler, Fehlercode
├─ assistent/          Endpunkte, AssistentService, Gespraechsfuehrung, Gespraechsspeicher,
│  │                   Nachrichtenbegrenzung, Werkzeugbau, Anweisung, Vorschlag
│  ├─ modell/          die anbieterneutrale Schnittstelle (E‑1032)
│  └─ anthropic/       der Adapter für die Anthropic-API
├─ config/AssistentConfig   wählt den Adapter nach overlord.assistent.anbieter
└─ task/               unverändert bis auf POST /api/tasks/pruefung (E‑1042)
```

**Was nach `common` gewandert ist** (E‑1037), in das Unterpaket `common.auftrag`: `TypDefinition`,
`DefinitionsPruefung`, `TypKatalog`, `AuftragsPruefung`, `Prueffehler`, `Fehlercode` und das Lesen
der Typzeilen als `AuftragstypRepository` (vorher `AuftragRepository.angeboteneTypen`, unverändert).
In `task` bleiben Parametertext, Rückgabe, Einordnung, das Anlegen und Lesen von Aufträgen und die
Endpunkte. Das sind die drei genannten Teile samt ihren Hilfsklassen; Verhalten und Statements
sind unverändert.

ArchUnit (`PaketstrukturTest`) hält: `assistent` ist ein Fachpaket und kennt `task` nicht;
HTTP-Aufrufe (`RestClient`, `java.net.http`) und anbieterspezifische Typen gibt es in `assistent`
nur im Adapter-Unterpaket; nur `config` und der Adapter selbst kennen den Adapter; die neutrale
Schnittstelle kennt weder Gesprächsführung noch Adapter; `assistent` fasst weder jOOQ noch einen
`DSLContext` an.

### 4.2 Die Schnittstelle zum Modell (`assistent.modell`)

| Typ | Inhalt |
|---|---|
| `Sprachmodell` | `Modellantwort rufe(Modellanfrage)` — ein Aufruf, ohne Zustand |
| `Modellanfrage` | Anweisung, Verlauf, Werkzeuge, `maxTokens` |
| `Nachricht` | `Kundennachricht` (Text), `Modellnachricht` (Text, Werkzeugaufrufe, Rohinhalt), `Werkzeugergebnisse` |
| `Werkzeug` | Name, Beschreibung, Eingabeschema (JSON Schema) |
| `Werkzeugaufruf` | Kennung, Name, Eingabe (JSON) |
| `Werkzeugergebnis` | Kennung des Aufrufs, Inhalt, ob es ein Fehler ist |
| `Modellantwort` | Text, Werkzeugaufrufe, Ende (`FERTIG`, `WERKZEUG`, `ABGESCHNITTEN`, `VERWEIGERT`), Token ein und aus, Modell, Rohinhalt |
| `Modellfehler` | Art: `ZEITGRENZE`, `NICHT_ERREICHBAR`, `UEBERLASTET`, `ABGELEHNT` (4xx), `STOERUNG` (5xx), `UNLESBAR` |

**Der Rohinhalt** ist die Antwort des Modells in der Form des Anbieters. Die Gesprächsführung liest
ihn nie, sie reicht ihn mit dem Verlauf an denselben Adapter zurück. Anthropic verlangt, dass
Denkblöcke unverändert zurückkommen; ein anderer Adapter darf ihn leer lassen.

### 4.3 Der Adapter für Anthropic (`assistent.anthropic`)

- `POST <basis-url>/v1/messages` mit `x-api-key`, `anthropic-version: 2023-06-01` und
  `content-type: application/json`; Rumpf mit `model`, `max_tokens`, `system` (die Anweisung),
  `messages`, `tools` und `thinking` nach der Einstellung `denken` (E‑1046).
- Verlauf: `Kundennachricht` → `user` mit einem Textblock; `Modellnachricht` → `assistant` mit dem
  Rohinhalt, sonst Text und `tool_use`-Blöcke; `Werkzeugergebnisse` → `user` mit je einem
  `tool_result` (`tool_use_id`, `content`, `is_error`), alle Ergebnisse einer Runde in einer
  Nachricht.
- Antwort: Textblöcke werden verbunden, `tool_use`-Blöcke werden Werkzeugaufrufe, `usage` gibt die
  Token. `stop_reason`: `tool_use` → `WERKZEUG`, `max_tokens` und `model_context_window_exceeded`
  → `ABGESCHNITTEN`, `refusal` → `VERWEIGERT`, alles andere → `FERTIG`.
- Fehler: **ein** Wiederholungsversuch bei `429` und `529` nach kurzer Pause (Vorgabe 1 s),
  danach `UEBERLASTET`. Sonst keine Wiederholung: andere `5xx` → `STOERUNG`, `4xx` → `ABGELEHNT`,
  Zeitüberschreitung → `ZEITGRENZE`, Verbindungsfehler → `NICHT_ERREICHBAR`, unlesbare Antwort →
  `UNLESBAR`.

Der Client sieht nie Einzelheiten des Anbieters (§6.6).

### 4.4 Die feste Anweisung (E‑1040)

Die Ressource `backend/src/main/resources/assistent/anweisung.txt`, auf Deutsch. Daran hängt das
Backend je angebotenem Typ einen Abschnitt mit Name, Kennung des Werkzeugs und Beschreibung aus
`task_type`. Eine Änderung an der Anweisung ist eine Änderung am Repository und wird mit dem
Testsatz (§8) geprüft. Der Wortlaut steht in der Ressource; diese Datei wiederholt ihn nicht.

### 4.5 Die Werkzeuge (E‑1034)

`Werkzeugbau` macht aus jeder `TypDefinition` ein Werkzeug, ohne einen Typ zu kennen:

- **Name** ist die Kennung des Typs (E‑1054).
- **Beschreibung:** dass das Werkzeug einen Vorschlag für den Auftrag „Name“ einreicht, dass das
  Backend ihn prüft und der Kunde ihn selbst anlegt, dass Felder ohne Wert weggelassen werden, und
  aus welchen Feldern in welcher Reihenfolge der Schlüssel besteht.
- **Eingabe:** `aktion` (Text, `enum` der Aktionen) und `eintraege` (Liste, `minItems` 1,
  `maxItems` aus `maxEintraege`) mit je einem Objekt, das genau die Felder des Typs als Text trägt
  (`additionalProperties: false`). Die Beschreibung eines Felds nennt `label`, ob es Pflicht oder
  optional ist, `nurBei`, `muster`, `beispiel` und `hinweis`, soweit gesetzt.
- **Kein `strict`:** Anthropic garantiert schemagültige Eingaben nur ohne Grenzen an Listen wie
  `maxItems`. Geprüft wird ohnehin im Backend.

Das erzeugte Schema für `WERK_ABLADESTELLE` hält `WerkzeugbauTest` fest.

### 4.6 Die Gesprächsführung

Je Nachricht des Kunden (`AssistentService` mit `Gespraechsfuehrung`):

1. Gespräch suchen (gebunden an Sitzung, Nutzer und Mandant, sonst `404`); Schalter prüfen;
   Länge der Nachricht, Zahl der Nachrichten im Gespräch und je Stunde prüfen; das Gespräch für
   diese Nachricht sperren (`409`, wenn schon eine läuft).
2. Das aktuelle Angebot des Mandanten aus dem Typkatalog lesen.
3. Modell rufen. Ruft es Werkzeuge, wird jeder Aufruf beantwortet:
   - Typ nicht (mehr) angeboten oder unbekannt → Fehler „Diesen Auftragstyp gibt es hier nicht.“;
   - schon ein gültiger Vorschlag in dieser Nachricht → Fehler (E‑1048);
   - Eingabe nicht in der erwarteten Form oder Wert kein Text → Fehler (E‑1047);
   - `AuftragsPruefung` meldet Fehler → Fehler mit der Liste (`index`, `feld`, `code`, `meldung`);
   - sonst gültig → der Vorschlag wird gemerkt, das Ergebnis sagt dem Modell, dass der Kunde ihn
     auf der Karte prüft und selbst anlegt.
4. Höchstens 3 Runden, dann muss Text kommen (E‑1049).
5. Erst am Ende wird der Verlauf um diese Nachricht ergänzt. Ein Fehler des Anbieters lässt ihn
   unverändert (E‑1052).

**Kein Weg des Assistenten legt eine Zeile in `task` an:** Das Paket kennt weder `task` noch die
Datenbank (ArchUnit), und ein Test führt jeden Ausgang der Gesprächsführung gegen ein geskriptetes
Modell und zählt danach die Aufträge des Testnutzers (§9).

### 4.7 Der Vorschlag (E‑1041)

```json
{
  "typ": "WERK_ABLADESTELLE",
  "name": "Anpassen der Werk-/Abladestellentabelle",
  "aktion": "Anpassung",
  "felder": [
    { "name": "lieferantennummer", "label": "Lieferantennummer", "hinweis": null },
    { "name": "werk", "label": "Werk", "hinweis": null },
    { "name": "abladestelle", "label": "Abladestelle", "hinweis": null },
    { "name": "sndprn", "label": "SNDPRN", "hinweis": "Bei Anpassung heißt leer: der Wert wird entfernt." },
    { "name": "sndprn4908", "label": "SNDPRN_4908", "hinweis": "Für VDA-4908-Prozesse. Bei Anpassung heißt leer: der Wert wird entfernt." }
  ],
  "eintraege": [
    { "werte": { "lieferantennummer": null, "werk": "72332", "abladestelle": "DDB0",
                 "sndprn": "120764", "sndprn4908": null },
      "schluessel": "72332/DDB0" }
  ]
}
```

`felder` sind die Felder, die bei der Aktion vorkommen, in der Reihenfolge der Definition — bei
`Löschung` also ohne die beiden SNDPRN-Felder. `werte` trägt **jedes** dieser Felder, leere als
`null`. `schluessel` ist der Text nach der Definition oder `null` ohne Schlüssel. Die Karte schickt
für `POST /api/tasks/pruefung` und `POST /api/tasks` nur `typ`, `aktion` und die Werte.

---

## 5. Einen Anbieter wechseln

**Was ein neuer Adapter umsetzen muss:** das Interface `Sprachmodell` in einem eigenen
Unterpaket von `assistent` (zum Beispiel `assistent.openai`), also

1. die neutrale `Modellanfrage` in die Form des Anbieters übersetzen: Anweisung, Verlauf (mit den
   Werkzeugergebnissen und ihrem Fehlerkennzeichen), Werkzeuge mit ihrem JSON Schema, `maxTokens`;
2. die Antwort zurückübersetzen: Text, Werkzeugaufrufe mit Kennung, Name und Eingabe, das Ende
   (`FERTIG`, `WERKZEUG`, `ABGESCHNITTEN`, `VERWEIGERT`), Token ein und aus, das Modell;
3. Fehler auf die Arten von `Modellfehler` abbilden, mit Zeitgrenze und höchstens einer
   Wiederholung bei Überlastung.

Gesprächsführung, Werkzeugbau, Anweisung, Vorschlag und Endpunkte bleiben unverändert. **Gewählt
wird per Konfiguration:** `overlord.assistent.anbieter` (heute nur `anthropic`); `AssistentConfig`
baut daraus die Bean `Sprachmodell`. Ein unbekannter Anbieter bei eingeschaltetem Assistenten lässt
den Start scheitern. ArchUnit verlangt, dass HTTP und Anbietertypen im Unterpaket bleiben.

**Ob er gut genug ist, zeigt der Testsatz** (§8): denselben Satz mit dem neuen Adapter fahren und
die Quote mit der des bisherigen vergleichen, Fall für Fall.

**Der nächste Adapter** ist ausdrücklich **ein lokal betriebenes Modell über eine
OpenAI-kompatible Schnittstelle**, wie sie vLLM, Ollama oder llama.cpp anbieten
(`POST /v1/chat/completions` mit `tools` und `tool_calls`). Gebaut wird er in A2 nicht. Dabei zu
beachten: Werkzeugaufrufe mit Fehlerkennzeichen kennt diese Schnittstelle nicht als eigenes Feld —
der Adapter schreibt den Fehler in den Inhalt des Werkzeugergebnisses; und ob ein lokales Modell
Werkzeugaufrufe zuverlässig liefert, hängt am Modell und am Server, nicht am Protokoll.

---

## 6. Endpunkte

Mandant ausschließlich aus der Sitzung; **keiner nimmt eine Mandanten-ID entgegen**, die Liste der
Ausnahmen von M1 bleibt bei drei ([`mandantentrennung.md`](mandantentrennung.md) §3). Rollen
`MANDANT` und `ADMIN` für den aktiven Mandanten; ohne aktiven Mandanten `403
kein-mandant-gewaehlt`. Der Änderungszwang beim Passwort sperrt sie wie alle übrigen. Keiner hat ein
Zeitfenster oder blättert: Sie lesen nichts aus `GlassfishDB` (E‑1016).

### 6.1 `GET /api/assistent`

```json
{ "verfuegbar": true, "typen": ["Anpassen der Werk-/Abladestellentabelle"] }
```

`verfuegbar` ist wahr, wenn der Schalter an ist, ein Schlüssel gesetzt ist und mindestens ein Typ
für den Assistenten angeboten wird. `typen` sind die Namen dieser Typen, nach Kennung sortiert. Ist
der Schalter aus, ist die Antwort `{ "verfuegbar": false, "typen": [] }`, ohne Datenbankzugriff.

### 6.2 `POST /api/assistent/gespraeche`

Ohne Rumpf. **`201`** mit `{ "gespraechId": "<UUID>" }`. Beim Eröffnen werden Anweisung und
Werkzeuge für dieses Gespräch festgelegt (E‑1045). Nicht verfügbar: **`503`**, Problemtyp
`assistent-nicht-verfuegbar`.

### 6.3 `POST /api/assistent/gespraeche/{id}/nachrichten`

Rumpf `{ "text": "…" }`. **`200`** mit

```json
{ "text": "Bitte prüfen Sie den Vorschlag auf der Karte …", "vorschlag": { … } }
```

`vorschlag` nach §4.7, oder `null`, wenn keiner entstanden ist.

### 6.4 `DELETE /api/assistent/gespraeche/{id}`

Beendet das Gespräch, **`204`**.

### 6.5 `POST /api/tasks/pruefung` (E‑1042)

Rumpf wie `POST /api/tasks`, ohne `anfrageId` (eine mitgeschickte wird ignoriert). Dieselbe
Prüfung wie beim Anlegen, einschließlich `ZU_LANG`, ohne Anlegen und ohne Ereignis. **`200`** mit

```json
{ "errors": [ { "index": 0, "feld": "sndprn", "code": "FORMAT", "meldung": "Eintrag 1, SNDPRN: …" } ] }
```

Leer heißt gültig. Typ nicht angeboten: **`404`**, gleich wie beim Anlegen — derselbe Rumpf,
dasselbe Statement ([`tasks.md`](tasks.md) §6.2).

### 6.6 Fehler

Im vorhandenen Fehlerformat ([`datenzugriff.md`](datenzugriff.md) §8); der Problemtyp ist der
Fehlercode. Kein Rumpf nennt Einzelheiten des Anbieters.

| Fall | Status | Problemtyp |
|---|---|---|
| unbekanntes, abgelaufenes, beendetes oder fremdes Gespräch — auch eines anderen Nutzers oder einer anderen Sitzung desselben Nutzers | **`404`**, in allen Fällen gleich | `nicht-gefunden` |
| Assistent aus, kein Schlüssel, kein Typ angeboten | `503` | `assistent-nicht-verfuegbar` |
| Text leer oder länger als 4.000 Zeichen | `400` | `nachricht-ungueltig`, mit `hoechstensZeichen` |
| 20 Nachrichten in diesem Gespräch erreicht | `429` | `gespraech-ausgeschoepft`, mit `hoechstensNachrichten` |
| 60 Nachrichten in der letzten Stunde erreicht | `429` | `zu-viele-nachrichten`, mit `hoechstensJeStunde` |
| in diesem Gespräch läuft schon eine Nachricht | `409` | `nachricht-laeuft` |
| Anbieter überlastet, Zeitgrenze, nicht erreichbar | `503` | `assistent-voruebergehend-gestoert` |
| Anbieter lehnt ab (`4xx`), Störung (`5xx`), unlesbare Antwort | `502` | `assistent-gestoert` |

---

## 7. Grenzen und Konfiguration

Unter `overlord.assistent` in `application.yml`; Umgebungsvariablen in [`betrieb.md`](betrieb.md)
§3.

| Schlüssel | Vorgabe | Bedeutung |
|---|---|---|
| `aktiv` | `false` (`OVERLORD_ASSISTENT_AKTIV`) | Schalter (E‑1030, E‑1056) |
| `anbieter` | `anthropic` | welcher Adapter (§5) |
| `modell` | `claude-sonnet-5-5` (`OVERLORD_ASSISTENT_MODELL`) | Modellkennung beim Anbieter |
| `api-key` | leer (`OVERLORD_ASSISTENT_API_KEY`) | ohne Schlüssel ist der Assistent aus |
| `basis-url` | `https://api.anthropic.com` | Ziel der Aufrufe |
| `zeitgrenze` | `30s` | je Aufruf des Modells, auch für den Verbindungsaufbau |
| `max-tokens` | `1024` | Obergrenze der Ausgabe je Aufruf (Punkt 1016) |
| `nachricht-hoechstens-zeichen` | `4000` | Länge einer Nachricht des Kunden |
| `nachrichten-je-gespraech` | `20` | Nachrichten des Kunden je Gespräch |
| `werkzeugrunden-je-nachricht` | `3` | Werkzeugrunden je Nachricht (E‑1049) |
| `nachrichten-je-stunde` | `60` | je Nutzer, gleitend über die letzte Stunde, im Arbeitsspeicher |
| `gespraech-ablauf` | `30m` | Ablauf ohne Nachricht |
| `gespraeche-je-sitzung` | `3` | offene Gespräche je Sitzung (E‑1053) |
| `anthropic.denken` | `between_tools` | `between_tools`, `adaptive` oder `keins` (Feld weglassen), E‑1046 |
| `anthropic.wiederholung-pause` | `1s` | Pause vor dem einen Wiederholungsversuch bei `429`/`529` |

**Die Begrenzung je Nutzer** liegt wie die je IP ([`authentifizierung.md`](authentifizierung.md))
nur im Arbeitsspeicher und beginnt bei einem Neustart von vorn. Sie zählt jede angenommene
Nachricht, auch eine, bei der der Anbieter scheitert (E‑1052).

**Wie lange eine Nachricht höchstens dauert:** bis zu vier Aufrufe (drei Werkzeugrunden und die
Antwort), je höchstens 30 Sekunden, dazu bei Überlastung je eine Wiederholung. Ohne Streaming
(E‑1043) wartet der Client so lange; was A3 daraus macht, ist Punkt 1011.

---

## 8. Testsatz

Der Testsatz misst, wie gut ein Modell echte Anfragen versteht. Er ist das Maß für jeden späteren
Wechsel des Anbieters, auch auf ein lokales Modell, und für jede Änderung an Anweisung oder
Typbeschreibung.

- **Fälle:** `backend/src/test/resources/assistent/testsatz/faelle.json`. Ein Fall hat `kennung`,
  `herkunft` (`synthetisch` oder `echt`), `gruppe` (1 bis 13 nach dem Auftrag), `beschreibung`,
  `nachrichten` (eine oder mehrere, nacheinander) und genau eine Erwartung: `erwartet` — entweder
  `{ "vorschlag": { "typ", "aktion", "eintraege" } }` oder `{ "vorschlag": null }` —,
  `erwartetNach` (je Nachricht eine, etwa „nach der ersten keiner, nach der zweiten dieser“) oder
  `erwartetEinerVon` (mehrere erlaubte Vorschläge; Gruppe 9: Anlegen und Löschen in einer
  Nachricht ergibt einen Vorschlag mit genau einer der beiden Aktionen). `TestsatzTest` hält die
  Form, die Abdeckung aller 13 Gruppen, dass jeder erwartete Vorschlag die Prüfung aus A1 besteht,
  und die Bewertung.
- **Bewertet wird nur strukturell:** Typ, Aktion und Einträge nach Normalisierung (Felder ohne
  Wert entfallen, die Reihenfolge der Einträge zählt nicht) gleich dem Erwarteten — oder wie
  erwartet kein Vorschlag. Formulierungen werden nicht bewertet.
- **Lauf:** `AssistentTestsatzLauf` mit `@Tag("llm")`. **Aus dem normalen Lauf und der CI
  ausgeschlossen**, und zwar dreifach: Der Klassenname passt auf kein Muster von Surefire
  (`*Test`) oder Failsafe (`*IT`), die Klasse trägt `@Tag("llm")`, und ohne
  `OVERLORD_ASSISTENT_API_KEY` wird sie übersprungen. Er fährt die Gesprächsführung mit dem echten
  Adapter, ohne Datenbank, gegen die Definition von `WERK_ABLADESTELLE` aus den Testressourcen,
  und schreibt den Bericht nach `backend/target/assistent-testsatz/bericht-<Zeitpunkt>.md`: je Fall
  bestanden oder nicht mit dem tatsächlichen Vorschlag, dazu Gesamtquote, Token und Dauer.
  **Berichte werden nicht eingecheckt** (`target/` ist ignoriert). Ein Fall, an dem der Anbieter
  scheitert, zählt als nicht bestanden. Wahlweise `OVERLORD_ASSISTENT_MODELL`,
  `OVERLORD_ASSISTENT_BASIS_URL` und `OVERLORD_ASSISTENT_DENKEN` (§7, `anthropic.denken`).

  ```
  cd backend && OVERLORD_ASSISTENT_API_KEY=… ./mvnw test -Djooq.codegen.skip=true       -Dtest=AssistentTestsatzLauf -Dsurefire.failIfNoSpecifiedTests=false
  ```

  Die Probe gegen einen geschlossenen lokalen Port (Schlüssel ein Platzhalter,
  `OVERLORD_ASSISTENT_BASIS_URL=http://127.0.0.1:9`) lief am 16.10.2026 durch und schrieb den
  Bericht mit 15 Fällen „nicht erreichbar“ — der Läufer funktioniert, ohne dass etwas hinausgeht.

- **Startsatz:** 17 synthetische Fälle für `WERK_ABLADESTELLE`, mindestens je einer aus den 13
  Gruppen des Auftrags (Neuanlage, Lieferantennummer, Tabulatorliste, Anpassung mit leerem Wert,
  Löschung mehrerer, Werk fehlt mit Nachreichen, SNDPRN mit fünf Stellen, unklar ob neu oder
  ändern, Anlegen und Löschen zugleich, anderes Thema, Aufforderung, Regeln zu ignorieren oder für
  einen anderen Mandanten anzulegen, zu viele Einträge, Abkürzungen und Tippfehler).
  **Ergänzt am 16.10.2026** mit der Höchstzahl 20 (E‑1058): S13 hat jetzt 21 statt 51 Einträge
  (kein Vorschlag), **S16** hat genau 20 und erwartet den Vorschlag mit allen 20 (Gruppe 12; er
  zeigt, ob `max_tokens` reicht, Punkt 1016), und **S17** ist die neue Gruppe 14: eine Anpassung,
  bei der der Kunde nur einen der beiden SNDPRN-Werte nennt — erwartet ist kein Vorschlag,
  sondern die Rückfrage, ob SNDPRN_4908 entfernt werden soll ([`tasks.md`](tasks.md) §4.2).
- **Echte Fälle** liefert Lukas nach. Vor dem Einchecken anonymisieren: keine Namen, keine
  Mailadressen, keine Signaturen.

**Gelaufen ist er noch nicht:** In der Umgebung von A2 war kein Schlüssel gesetzt. Das erste
Ergebnis bekommt eine M‑Nummer und steht hier.

---

## 9. Tests

### 9.1 Ohne Datenbank und ohne Anbieter

| Test | Was er hält |
|---|---|
| `WerkzeugbauTest` | das Schema für `WERK_ABLADESTELLE` und für die Gattung aus A1; keine Eigenschaft beginnt mit `mandant`; Typen mit ungültigem Werkzeugnamen oder einem Feld `mandant…` entfallen (E‑1054) |
| `AnweisungTest` | die Anweisung enthält die Beschreibungen der angebotenen Typen, aber weder Benutzername noch Mandanten-ID |
| `GespraechsfuehrungTest` | gegen ein geskriptetes Modell: ungültiger Vorschlag → Werkzeugergebnis mit Fehlern, keine Karte; gültiger → Karte mit allen Feldern der Aktion, leere als `null`; Abbruch nach 3 Runden; Werkzeugaufruf für einen nicht angebotenen Typ abgewiesen; ein zweiter Vorschlag je Nachricht abgewiesen; Wert als Zahl abgewiesen; abgeschnitten, verweigert; Fehler des Anbieters lassen den Verlauf unverändert |
| `GespraechsspeicherTest` | Ablauf nach 30 Minuten, höchstens 3 je Sitzung, Bindung an Sitzung, Nutzer und Mandant, Mandantenwechsel — mit eingespeister Uhr (T1) |
| `NachrichtenbegrenzungTest` | 60 je Stunde, gleitend, mit eingespeister Uhr |
| `AnthropicSprachmodellTest` | gegen einen lokalen HTTP-Stub: Kopfzeilen, Aufbau der Anfrage (Anweisung, Verlauf mit Rohinhalt und `tool_result`, Werkzeuge, `thinking`), Text und `tool_use`, Abbruchgründe, ein Wiederholungsversuch bei `429` und `529`, keiner bei `500`, Zeitgrenze |
| `AssistentServiceTest` | **Schalter aus oder kein Schlüssel:** `AssistentConfig` baut keinen Adapter, Stand, Eröffnen und Nachricht melden nicht verfügbar, **kein HTTP-Aufruf** am Stub, kein Zugriff auf den Typkatalog — mit Gegenprobe, dass eingeschaltet genau ein Aufruf am Stub ankommt. Dazu: jede Fehlerart des Anbieters auf `503` bzw. `502` ohne Anbieterdetails, Länge der Nachricht, Höchstzahl je Gespräch, `409`, `404` in allen Fällen gleich, kein angebotener Typ |
| `TestsatzTest` | Form und Abdeckung des Testsatzes, jede erwartete Karte gültig, strukturelle Bewertung (§8) |
| `PaketstrukturTest` | die Regeln aus §4.1 |

### 9.2 Mit Datenbank (`@Tag("db")`)

`AssistentIsolationDbIT` mit einem geskripteten Modell statt des Anbieters (eine `@Primary`-Bean im
Test, Schalter an, Schlüssel ein Platzhalter): `NEXANS` gegen `IBIS`; `SUTTONS` bekommt
`verfuegbar: false` und `503`; ein Gespräch eines anderen Nutzers ist `404`, gleich einem
erfundenen; ein Gespräch derselben Person aus einer anderen Sitzung ist `404`; ADMIN verliert seine
Gespräche beim Mandantenwechsel; ein Typ, der nur `IBIS` freigeschaltet ist, steht nicht unter den
Werkzeugen von `NEXANS` und wird beim Einreichen abgewiesen; kein Weg legt einen Auftrag an;
`POST /api/tasks/pruefung` mit einem nicht angebotenen Typ ist `404`, ununterscheidbar von einem
erfundenen; Änderungszwang.

**Die Verletzungsprobe** steht in §9.3.

### 9.3 Verletzungsprobe (16.10.2026)

Jeder Mandantenfilter des neuen Codes ist **einzeln** entfernt oder verfälscht worden; danach
liefen die Tests ohne Datenbank in `assistent` und `common.auftrag` (150) und
`AssistentIsolationDbIT` (13). Die Datei ist jedes Mal per `git checkout` auf den committeten Stand
zurückgespielt worden (der Baum war vor jeder Probe sauber); `target/classes` ist danach neu gebaut.
`AssistentServiceTest` erscheint in der Tabelle mit seinen geschachtelten Klassen
(„Eingeschaltet“, „Schalter aus“).

| # | Entfernt oder verfälscht | Rot |
|---|---|---|
| P1 | `mandant_id` im `WHERE` von `AuftragstypRepository.angeboteneTypen` | `AssistentIsolationDbIT`: `stand`, `suttons_nicht_verfuegbar`, `werkzeuge_je_mandant`, `fremder_typ_wird_abgewiesen`, `pruefung_nicht_angeboten` |
| P2 | Sitzung in `Gespraechsspeicher.finde` | `AssistentIsolationDbIT.andere_sitzung_desselben_nutzers`; `GespraechsspeicherTest.bindung`; `AssistentServiceTest` (Eingeschaltet) `.nicht_gefunden` |
| P3 | Nutzer in `Gespraechsspeicher.finde` | `GespraechsspeicherTest.bindung`, `.beenden`; `AssistentServiceTest` (Eingeschaltet) `.nicht_gefunden` |
| P4 | Mandant in `Gespraechsspeicher.finde` | `GespraechsspeicherTest.bindung` |
| P5 | `Gespraechsspeicher.beendeNachMandantenwechsel` ohne Wirkung | `AssistentIsolationDbIT.admin_mandantenwechsel`; `GespraechsspeicherTest.mandantenwechsel` |
| P6 | `eroeffne` bindet das Gespräch fest an `IBIS` statt an den Sitzungsmandanten | 7 von 13 in `AssistentIsolationDbIT` (u. a. `fremdes_gespraech`, `karte_und_nichts_geschrieben`), 5 in `GespraechsspeicherTest`, 6 in `AssistentServiceTest` |
| P7 | `AssistentService` liest das Angebot fest für `IBIS` | `AssistentIsolationDbIT`: `stand`, `suttons_nicht_verfuegbar`, `werkzeuge_je_mandant`, `fremder_typ_wird_abgewiesen` |
| P8 | `AuftragService.pruefe` prüft fest für `IBIS` | `AssistentIsolationDbIT.pruefung_nicht_angeboten` |

**Jeder Filter hat mindestens einen Test, der sein Fehlen bemerkt.** Zwei bemerkt nur ein Test
ohne Datenbank, und das ist Absicht, kein Loch: **P3** (Nutzer) und **P4** (Mandant) sind zweite
Riegel. Über HTTP hat ein anderer Nutzer immer eine andere Sitzung, P2 fängt ihn also vorher; und
ein fremder Mandant in derselben Sitzung entsteht nur durch einen Wechsel, den P5 vorher aufräumt.
Fiele der erste Riegel, hielte der zweite — darum prüft `GespraechsspeicherTest.bindung` jeden
einzeln, mit genau einer Abweichung je Zugriff.

---

## 10. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | keiner der fünf neuen; kein Werkzeug hat einen Mandantenparameter (E‑1034) |
| **M2** Mandant als erster Pflichtparameter | `AuftragstypRepository` wie bisher; der Gesprächsspeicher sucht nur mit Mandant, Nutzer und Sitzung |
| **M3** Filter im Statement | die Freischaltung bleibt Teil des einen Statements im Typkatalog |
| **M4** Isolationstest je Endpunkt | `AssistentIsolationDbIT` |
| **Regel 3** | `assistent` fasst keine Datenbank an |
| **Regel 7** | keine neuen Statements; `POST /api/tasks/pruefung` und der Assistent nutzen das Statement des Typkatalogs aus M1000 ([`tasks.md`](tasks.md) §9.3) |
| **Z1** | keine Uhr außer `systemClock` (E‑1057) |
| **G1** | der Schlüssel nur aus der Umgebung, nie im Repository, nie im Protokoll |

---

## 11. Offene Punkte

Nummern ab 1009 (Linie `assistent`, per `grep` geprüft); dieselben Zeilen stehen im Register
[`offene-punkte.md`](offene-punkte.md). Punkt 1008 aus A1 („Datenschutz und Auftragsverarbeitung
mit dem Modellanbieter, vor A2“) ist mit E‑1029 und E‑1030 beantwortet — gebaut, aber abgeschaltet —
und geht in Punkt 1009 auf.

| Nr. | Punkt | Art |
|---|---|---|
| **1009** | Prüfung der Auftragsverarbeitung mit Anthropic durch den Datenschutzbeauftragten, vor dem Einschalten in der Produktion (E‑1029). | Entscheidung offen |
| **1010** | Der Produktionsserver muss `api.anthropic.com` erreichen; ob der Weg frei ist (Firewall, Proxy), ist ungeprüft. | Sichtprüfung oder Messung offen |
| **1011** | Streaming für A3 (E‑1043): Eine Nachricht kann ohne Streaming bis zu vier Modellaufrufe lang dauern (§7). | Entscheidung offen |
| **1012** | Was das Gespräch erfährt, wenn der Kunde die Karte ändert oder den Auftrag anlegt. Heute erfährt es nichts. | Entscheidung offen |
| **1013** | Ob der Assistent später auch Fragen zum Stand eigener Aufträge beantworten soll. Heute bekommt er keine Aufträge aus `task` (E‑1035). | Entscheidung offen |
| **1014** | Kosten je Gespräch, sobald echte Läufe vorliegen. Das Protokoll trägt die Token je Nachricht (§3.5). | Sichtprüfung oder Messung offen |
| **1015** | Ob eine Vereinbarung über Zero Data Retention mit Anthropic sinnvoll ist. | Entscheidung offen |
| **1016** | Ob `max_tokens` 1.024 (E‑1036) für einen Vorschlag mit der Höchstzahl an Einträgen reicht. Ein Eintrag von `WERK_ABLADESTELLE` als JSON kostet geschätzt 25 bis 35 Token — **geschätzt, nicht gemessen**. Bei den ursprünglichen 50 Einträgen wären das 1.250 bis 1.750 gewesen; seit E‑1058 (höchstens 20) sind es 500 bis 700, dazu der Text der Antwort. Ein abgeschnittener Vorschlag endet mit der Bitte, aufzuteilen (E‑1050). `max_tokens` bleibt 1.024 (16.10.2026). **Offen, bis ein Lauf des Testsatzes mit Fall S16 (genau 20 Einträge) zeigt, dass es reicht.** | Sichtprüfung oder Messung offen |

---

## 12. Folgeschritte

| Teil | Inhalt |
|---|---|
| **A3** | Chatbox in der Kopfleiste und Bestätigungskarte: `GET /api/assistent` entscheidet, ob die Chatbox erscheint; die Karte zeigt `vorschlag` mit `label` und `hinweis` je Feld, prüft Änderungen über `POST /api/tasks/pruefung` und legt mit `POST /api/tasks` an (`anfrageId` je Absenden). Offen: Streaming (1011), Rückmeldung ans Gespräch (1012) |
| **A4** | Bereich „Ihre Aufträge“ in der Übersicht, über `GET /api/tasks` |
