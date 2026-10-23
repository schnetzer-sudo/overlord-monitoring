# Aufträge über den Assistenten — Auftragsmodell (A1)

*Angelegt am 16.10.2026 mit Teilschritt A1 der Linie `assistent`* (Zweig
`assistent-a1-auftragsmodell`, [`ROADMAP.md`](ROADMAP.md) §2). Der Vertrag für die EDI-Plattform
steht für sich lesbar in [`task-schnittstelle.md`](task-schnittstelle.md).

A1 baut das Fundament: drei Tabellen in `overlord_monitor`, die Typdefinition samt Prüfung, den
ersten Typ `WERK_ABLADESTELLE` als Datenzeile, vier Endpunkte und den Plattformvertrag. **Keine
Oberfläche, kein Sprachmodell, keine Ausführung.** Assistent, Chatbox und die Übersicht folgen mit
A2 bis A4 (§8).

> **Ergänzt am 19.10.2026 mit A4** (Zweig `assistent-a4-ihre-auftraege`): `DELETE
> /api/tasks/{taskId}` löscht einen Auftrag, solange die Plattform ihn nicht abgeholt hat (§6.6), und
> die Antwort trägt die `label` der Felder aus der Kopie (§6.5). Die Entscheidungen dazu stehen in §2
> unter „Löschen und Plattform“, die Oberfläche in [`assistent-frontend.md`](assistent-frontend.md)
> §12.

> **Ergänzt am 21.10.2026 mit 1.1.0** (Zweig `assistent-merge`): Die Linie ist nach `main`
> zusammengeführt. **Der Assistent — und mit ihm das Anlegen — ist nur für Mandanten verfügbar, für
> die ein Typ freigeschaltet ist**, mit 1.1.0 zunächst nur `SYSTEM` (§3.9). `V103` nimmt die
> Freischaltungen für `NEXANS` und `IBIS` aus `V100` zurück. Die Entscheidungen stehen in §2 unter
> „Zusammenführung und Freischaltung“.

> **Geändert am 23.10.2026** (Zweig `fix/auftrag-schluessel-leerzeichen`): **Der Schlüssel von
> `WERK_ABLADESTELLE` darf Leerzeichen enthalten.** Das Backend kürzt jeden Schlüsselteil vor der
> Prüfung um Leerzeichen am Rand (§5.3), das Muster der drei Teile lässt U+0020 im Innern zu und
> kein anderes Leerzeichen (§4.3). Die Definition ändert ein Skript, keine Migration
> (`scripts/werk-abladestelle-schluessel-leerzeichen.sql`). Die Entscheidungen stehen in §2 unter
> „Leerzeichen im Schlüssel“, die Tests in §9.7.

---

## 1. Zweck und Ablauf

Kunden beauftragen wiederkehrende Konfigurationsänderungen an der EDI-Plattform selbst, ohne
Umweg über die Betreuung. Der Ablauf, wenn A1 bis A4 stehen:

1. **Assistent.** Der Kunde öffnet den Assistenten (Chat) in der Kopfleiste. Er ist der
   **einzige** Eingang; es gibt keinen Menüpunkt und keine Formularseite (E‑1001). Der Assistent
   sammelt nur Werte, er beurteilt sie nicht (§7).
2. **Karte.** Der Assistent zeigt die gesammelten Werte auf einer Bestätigungskarte. Erst der Klick
   des Kunden schickt `POST /api/tasks` ab — nie das Modell.
3. **`task`.** Das Backend prüft die Werte gegen die Definition des Typs, baut daraus den
   Parametertext und legt den Auftrag mit Status `OFFEN` in `overlord_monitor.task` an. Es gibt
   keinen Freigabeschritt (E‑1002).
4. **Plattform.** Die EDI-Plattform holt offene Aufträge ab (`IN_AUSFUEHRUNG`), führt sie aus und
   meldet zurück (`ERLEDIGT` mit einer Rückgabezeile je Eintrag, oder `FEHLGESCHLAGEN`).
5. **Rückmeldung.** Der Monitor liest die Rückgabe und ordnet den Auftrag ein — Erledigt,
   Teilweise erledigt, Fehlgeschlagen —, an genau einer Stelle (`TaskStatusClassifier`, §5.5).
6. **Übersicht.** Der Kunde sieht den Stand unter „Ihre Aufträge“ (A4). Einen Auftrag, den die
   Plattform noch nicht abgeholt hat, kann er dort löschen (§6.6).

**Auftragstypen sind Daten** (E‑1018). Ein Typ ist eine Zeile in `task_type`: eine Beschreibung in
Fließtext für den Assistenten und eine Struktur (JSON), aus der das Backend Prüfung, Parametertext
und später die Werkzeuge des Modells baut. Ein neuer Typ, der mit den vorhandenen Regelbausteinen
auskommt, braucht keinen Code und kein Deployment (§3).

---

## 2. Entscheidungen

Herkunft: **A** = vom Auftraggeber am 15. oder 16.10.2026 entschieden. **V** = Vorschlag vom 15.
oder 16.10.2026, ohne Einspruch übernommen. Die E‑Nummer ist `E‑1000` plus die Nummer der
Entscheidung im Auftrag zu A1; `E‑1000` selbst ist nicht vergeben. Die Nummern beginnen bei 1000,
weil auf der Linie `assistent` alle projektweit gezählten Nummern dort beginnen
([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12).

### Ablauf und Plattform

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1001** | Aufträge entstehen ausschließlich über den Assistenten. Kein Menüpunkt, keine Formularseite. | A |
| **E‑1002** | Kein Freigabeschritt. Die Bestätigung des Kunden legt den Auftrag direkt mit Status `OFFEN` an. | A |
| **E‑1003** | Die Auftragstabelle liegt in `overlord_monitor`. | A |
| **E‑1004** | Die Plattform holt Aufträge ab und meldet zurück, mit dem Benutzer `monitor_write`. **Folge:** Sie hat damit alle Rechte auf `overlord_monitor`, einschließlich `app_user` und der Sitzungen. Bewusst so entschieden: kein neuer Benutzer, kein `GRANT`. | A |
| **E‑1005** | Ein Auftrag trägt genau eine Aktion aus den Aktionen seines Typs und eine Liste von Einträgen. | V |
| **E‑1006** | Den Parametertext baut ausschließlich das Backend aus geprüften Einzelwerten. Kein Endpunkt nimmt Parametertext entgegen. | V |
| **E‑1009** | Gesamtstand: **Erledigt** (alle Ok), **Teilweise erledigt** (mindestens einer nicht Ok), **Fehlgeschlagen** (Auftrag nicht verarbeitbar, mit Fehlercode). „Teilweise“ wird **nicht gespeichert**, sondern im Monitor aus der Rückgabe abgeleitet, an genau einer Stelle. Die Plattform setzt nur `ERLEDIGT` oder `FEHLGESCHLAGEN`. | A (Regel), V (Ableitung im Monitor) |
| **E‑1010** | Die Plattform setzt beim Abholen `IN_AUSFUEHRUNG`, mit einem bedingten Update, das nur bei Status `OFFEN` greift. Das verhindert doppelte Ausführung. | V |
| **E‑1011** | Alle Zeitstempel eines Auftrags in UTC, gesetzt in der Datenbank über `UTC_TIMESTAMP(3)`, nie aus Java. | V |
| **E‑1016** | Klarstellung zu [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §8: Die Regeln 1 bis 6 sind mit der Last auf `GlassfishDB` begründet und gelten für Lesezugriffe dort. Tabellen in `overlord_monitor`, die nicht aus `GlassfishDB` abgeleitet sind, folgen ihnen nicht. Regel 7 gilt weiter. | V |

### Typdefinitionen

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1018** | Auftragstypen sind Daten: eine Zeile in `task_type` mit Kennung, Name, Beschreibung (Fließtext für den Assistenten) und Struktur (JSON). Im Hauptcode kennt keine Stelle einen bestimmten Typ. | A |
| **E‑1019** | Neue und geänderte Definitionen kommen direkt per SQL in die Produktion, ohne Deployment und ohne Neustart. Das Backend prüft jede Definition beim Laden. Eine ungültige wird nicht angeboten und mit Grund als `WARN` ins Log geschrieben. | A |
| **E‑1020** | Ein Typ wird einem Mandanten angeboten, wenn er aktiv, gültig und für ihn freigeschaltet ist. Sonst existiert er für diesen Mandanten nicht: `404` beim Anlegen, nicht in der Typliste. | V |
| **E‑1021** | Die Struktur kennt einen festen Satz von Regelbausteinen (§3.3). Ein neuer Baustein ist Code und steht danach allen Typen zur Verfügung. Keine Ausdrucks- oder Skriptsprache in der Definition. | V |
| **E‑1022** | Unabhängig von der Definition sind in jedem Wert verboten: Zeilenumbrüche, Steuerzeichen und jedes Zeichen, das in der Zeilenvorlage oder als Schlüsseltrenner wörtlich vorkommt. So kann kein Wert den Parametertext zerlegen, auch wenn ein Muster in der Definition fehlt. | V |
| **E‑1023** | Jeder Auftrag speichert eine Kopie von Name und Struktur, mit denen er angelegt wurde, und seine Einträge strukturiert. Anzeige und Einordnung lesen die Kopie, nie die aktuelle Zeile. Eine Änderung an der Definition wirkt nur auf neue Aufträge. | V |
| **E‑1024** | Beschreibung und Struktur sind vertrauenswürdige Eingaben der Betreuung. Was der Kunde schreibt, ist es nicht. | V |

### Der erste Typ `WERK_ABLADESTELLE`

Diese Regeln stehen als Werte in seiner Definition, nicht im Code (§4).

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1007** | Anpassung: Ein leeres SNDPRN-Feld bedeutet „Wert wird entfernt“. Mindestens einer der beiden Werte muss gesetzt bleiben. Die Bedeutung steht in Beschreibung und Plattformvertrag; der Monitor prüft nur die Regel „mindestens eins“. | A |
| **E‑1008** | Neuanlage mit vorhandenem Schlüssel: Eintrag bleibt unverändert, Rückgabe `Schlüssel bereits vorhanden`. Das ist ein dritter Rückgabewert über Anhang A hinaus. | A |
| **E‑1012** | Schlüsselteile: 1 bis 35 Zeichen, kein Leerraum, kein `/`, kein `,`. Groß- und Kleinschreibung bleiben wie eingegeben. Die 35 sind eine technische Schranke, keine fachliche Regel. **Geändert am 23.10.2026 durch E‑1136, E‑1142 und E‑1143:** Leerzeichen (U+0020) im Innern sind erlaubt, am Rand kürzt das Backend; jedes andere Leerzeichen bleibt unzulässig. Die 35 gelten für den gekürzten Teil. | V |
| **E‑1013** | SNDPRN und SNDPRN_4908: leer oder genau sechs Ziffern. | A (Anhang A) |
| **E‑1014** | Doppelte Schlüssel innerhalb eines Auftrags werden abgelehnt. | V |
| **E‑1015** | Höchstens 50 Einträge je Auftrag. **Abgelöst am 16.10.2026 durch E‑1058** (20 statt 50). | A (16.10.2026) |
| **E‑1058** | **Höchstens 20 Einträge je Auftrag**, statt 50 (E‑1015). Angefragt waren ursprünglich 10; die 50 waren eine eigene Erweiterung. Ein Wert der Definition, geändert mit der Migration `V101` (§4.3). | A (16.10.2026) |
| **E‑1017** | Lesarten in Anhang A: „Anpasung“ ist „Anpassung“. „Löschen eines oder mehrerer neuer Einträge“ heißt „vorhandener“. | V |

### Rückfragen zu A1 (16.10.2026)

Drei Regeln der Richtlinien widersprachen dem Auftrag oder ließen ihn offen. Der Auftraggeber hat
am 16.10.2026 entschieden:

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1025** | Die Pfade heißen `/api/tasks` und `/api/tasks/typen`, die Antwortfelder sind deutsch. Das weicht von [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §5.2 ab (deutsche Pfadsegmente, englische Felder): Tabelle und Plattformvertrag heißen `task`, das Vorbild für einen englischen Pfad ist `/api/admin/users`; deutsche Felder sind die gelebte Praxis des Projekts. | A |
| **E‑1026** | Regel 7 gilt vollständig: Unter M1000 stehen je Abfrage `EXPLAIN` **und** die Laufzeit aus dem Profil, aus einer Sitzung mit wenigen Läufen — kein Lastgerüst, keine Wiederholungsreihen. | A |
| **E‑1027** | Die Klarstellung aus E‑1016 steht nicht nur in der Projektbeschreibung, sondern mit einem Satz auch bei L1 und L3 in [`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §4.4 und bei Regel 4 und 5 in [`CLAUDE.md`](../CLAUDE.md). Sonst hielte ein späterer Schritt `GET /api/tasks` ohne Zeitfenster für einen Regelverstoß. | A |

### Löschen und Plattform (A4, 19.10.2026)

Aus dem Auftrag zu A4. Die E‑Nummer ist `E‑1097` plus die Nummer der Entscheidung dort; die
Entscheidungen der Oberfläche stehen in [`assistent-frontend.md`](assistent-frontend.md) §12.1.
Herkunft: **A** = vom Auftraggeber am 19.10.2026 entschieden, **V** = Vorschlag vom 19.10.2026, ohne
Einspruch übernommen.

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1110** | **Löschbar ist nur ein Auftrag im Stand `OFFEN`.** Alle anderen Stände sind nicht löschbar, für keine Rolle, auch nicht für `ADMIN`. | A |
| **E‑1113** | Löschen darf jeder Benutzer des aktiven Mandanten, wie die Liste jedem zeigt, was der Mandant beauftragt hat — nicht nur der, der angelegt hat. | V |
| **E‑1114** | **Der Auftrag wird wirklich gelöscht**, weil er nie ausgeführt wurde. Ein Statement: `DELETE … WHERE task_id = ? AND mandant_id = <aus der Sitzung> AND status = 'OFFEN'`. Das Abholen der Plattform greift ebenfalls nur bei `OFFEN`; von beiden gewinnt genau eines. Eine gelöschte Zeile bekommt einen Eintrag im `audit_log`: Benutzer, Mandant, `task_id`, Typ, Aktion, Zahl der Einträge, Zeitpunkt des Einstellens. Kein Parametertext im Log. Löschen und Eintrag in einer Transaktion im eigenen Schema. | V |
| **E‑1115** | **Antworten:** gelöscht `204`. Null Zeilen gelöscht: Gibt es den Auftrag für diesen Mandanten nicht (unbekannt oder fremder Mandant), `404` wie bei jeder unbekannten Kennung. Gibt es ihn, aber nicht mehr `OFFEN`, `409` mit einem stabilen Fehlercode im vorhandenen Fehlerformat. Nachgelesen wird nur im eigenen Mandanten, mit `WHERE task_id = ? AND mandant_id = ?`. | V |
| **E‑1117** | Die Plattform holt **alle fünf Minuten** ab. Herkunft: Auskunft des Auftraggebers vom 19.10.2026, nicht gemessen (Punkt 1026). Die Zahl steht in der Doku, **nicht** in der Oberfläche. | A |
| **E‑1118** | Für die Plattform ändert sich am Ablauf nichts: Ein gelöschter Auftrag fehlt beim nächsten Abfragen, und verschwindet er zwischen Abfragen und Abholen, ändert das Abholen null Zeilen wie bei einem schnelleren Lauf. Das steht ausdrücklich in [`task-schnittstelle.md`](task-schnittstelle.md) §3a. | V |
| **E‑1119** | Abgeholt und ausgeführt wird von der Plattform, nicht von Lobster. Wo die Doku Lobster als den nannte, der abholt oder ausführt, steht jetzt „die Plattform“ (§3.7). | A |

**Rückfrage zu A4, am 19.10.2026 entschieden (A):**

| E | Entscheidung | Anlass |
|---|---|---|
| **E‑1120** | **Die Antwort trägt die `label` der Felder aus der Kopie:** `felder` mit `name` und `label` je Feld der Definition, in deren Reihenfolge — in Liste, Einzelabruf und Antwort auf `POST` gleich (§6.5). Abwärtsverträglich; die Karte aus A3 liest das Feld nicht. | `GET /api/tasks` lieferte je Eintrag nur `werte` mit Feldnamen. Die Übersicht braucht die `label` (E‑1104); sie aus `GET /api/tasks/typen` zu nehmen, hieße die aktuelle Definition statt der Kopie zu lesen (gegen E‑1023), und bei einem abgeschalteten Typ fehlten sie |

**Festlegungen der Umsetzung (A4), zur Bestätigung.** Eine E‑Nummer bekommen sie erst damit, wie in
A2 und A3; die F-Nummern setzen die der Oberfläche fort ([`assistent-frontend.md`](assistent-frontend.md)
§12.3).

| F | Festlegung | Grund |
|---|---|---|
| **F26** | **Löschen und Protokoll laufen in einer Transaktion:** `AuftragService.loesche` ist `@Transactional`, und der Eintrag geht über eine neue Methode `AuditLogWriter.schreibeInTransaktion` mit `Propagation.MANDATORY` in dieselbe. Das bisherige `schreibe` bleibt `REQUIRES_NEW`. | E‑1114 verlangt eine Transaktion. `schreibe` öffnet bewusst eine eigene, damit ein Fehlversuch protokolliert bleibt, wenn die Fachtransaktion zurückrollt — beim Löschen wäre das genau falsch: Ein Eintrag „gelöscht“ zu einem Löschen, das zurückgerollt ist. `MANDATORY` wirft, wenn keine Transaktion läuft, statt still eine eigene zu öffnen |
| **F27** | **`409` hat den Problemtyp `auftrag-nicht-offen`**, Titel „Auftrag nicht mehr offen“. Die Nachlesung liest den `status`, der nur ins Protokoll geht, nicht in die Antwort. | Ein stabiler Code nach dem Muster der übrigen Problemtypen ([`datenzugriff.md`](datenzugriff.md) §8). Die Oberfläche zeigt ihren eigenen Satz (E‑1116); welcher Stand es ist, erfährt sie aus dem Neuladen der Liste |
| **F28** | **`DELETE … RETURNING`**: Typ, Aktion, Einträge und Zeitpunkt des Einstellens für das Protokoll kommen aus dem Löschen selbst. | Ein Statement, wie E‑1114 verlangt, und kein Lesen davor: Ein `SELECT` vor dem `DELETE` könnte einen Stand lesen, den das Abholen der Plattform im selben Augenblick überholt |

### Zusammenführung und Freischaltung (1.1.0, 21.10.2026)

Aus dem Auftrag, die Linie `assistent` nach `main` zusammenzuführen. Die E‑Nummer ist `E‑1124` plus
die Nummer der Entscheidung dort (E‑1125 bis E‑1128); die drei Rückfragen vom selben Tag folgen als
E‑1129 bis E‑1131, der Nachtrag zum Auftrag vom selben Tag als E‑1132 bis E‑1135. Die nächste freie
Nummer nach A4 war E‑1125, per `grep` über alle Zweige geprüft.
Herkunft: **A** = vom Auftraggeber am 21.10.2026 entschieden.

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1125** | **`assistent` wird nach `main` zusammengeführt** und mit 1.1.0 ausgeliefert. Die Entscheidung vom 16.10.2026, Releases an Kunden ohne Chatbot auszuliefern, gilt damit für die **Sichtbarkeit**: Der Code geht mit, sichtbar ist der Assistent nur für freigeschaltete Mandanten, zunächst nur `SYSTEM`, den ausschließlich `ADMIN` sieht. Der Typ `WERK_ABLADESTELLE` ist definiert und ausschließlich für `SYSTEM` freigeschaltet; die Freischaltungen für `NEXANS` und `IBIS` aus `V100` nimmt `V103` zurück. | A |
| **E‑1126** | **Die Freischaltung ist datengetrieben:** Der Assistent steht einem Mandanten zur Verfügung, wenn für ihn mindestens ein Auftragstyp freigeschaltet ist (`task_type_mandant`, E‑1020). Das Ausrollen auf weitere Mandanten ist ein SQL-Insert in der Produktion, kein Deployment — wie bei neuen Typen (E‑1019). Kein zweiter Weg: keine Rolle, keine Mandantenliste in der Konfiguration, keine Bibliothek für Schalter. | A |
| **E‑1127** | **Eine Linie.** Sobald Migrationen ab `V100` in der Produktion angewandt sind, kann dort keine mit kleinerer Nummer mehr folgen. Neue Migrationen schließen an die höchste vergebene an (nach `V103` also `V104`), projektweite Nummern (E, M, Punkte) ebenso; der Bereich unter 1000 wird nicht weiter befüllt ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). | A |
| **E‑1128** | Version **1.1.0**. Den Tag setzt Lukas. | A |
| **E‑1129** | **A4 vor dem Zusammenführen nach `assistent`:** `assistent-a4-ihre-auftraege` per `--no-ff` nach `assistent` (`18f0f41`), dann `assistent-merge` von dort. Die Festlegungen F16 bis F29 bleiben Vorschläge. | A (Rückfrage: A4 lag nicht auf `assistent`) |
| **E‑1130** | **Der Schalter bleibt.** Verfügbar heißt: Schalter `OVERLORD_ASSISTENT_AKTIV` an, Schlüssel gesetzt (E‑1030, E‑1056) **und** mindestens ein freigeschalteter Typ. Fehlt Schalter oder Schlüssel, ist der Assistent für alle Mandanten aus. | A (Rückfrage: Der Auftrag nannte nur Schlüssel und Typ) |
| **E‑1131** | **Punkt 1009 ist erledigt:** Die Prüfung der Auftragsverarbeitung mit Anthropic durch den Datenschutzbeauftragten ist abgeschlossen. Auskunft des Auftraggebers vom 21.10.2026; Datum und Ergebnis der Prüfung selbst stehen nicht im Repository. | A (Rückfrage) |
| **E‑1132** | **Der Bau des Servers gehört zur Abnahme:** `./mvnw -B clean package -Djooq.codegen.skip=true -DexcludedGroups=db` ist auf dem gemergten Stand grün. Nach dem Merge läuft `pnpm install`, und `pnpm-lock.yaml` wird mitcommittet, weil der Server mit `pnpm install --frozen-lockfile` baut. Erfüllt am 21.10.2026 auf `865537e`: Das Lockfile bleibt unverändert, beide Bauten sind grün ([`betrieb.md`](betrieb.md) §7a). | A (Nachtrag) |
| **E‑1133** | **Die Deployment-Routine der Produktion ist die mitgeteilte** (`deploy-routine.txt`), festgehalten in [`betrieb.md`](betrieb.md) §7a: systemd mit drei Units, Bau auf dem Server, Sicherung vor dem Start, Rollback über das vorige JAR. Damit sind die Punkte 255, 260 und 261 erledigt. | A (Nachtrag) |
| **E‑1134** | **Der Schlüssel des Assistenten liegt in einer eigenen `EnvironmentFile`** (`/etc/overlord-monitor/assistent.env`) nur für die Unit `overlord-monitor`, nicht für `overlord-monitor-rollup`. Der Schalter `OVERLORD_ASSISTENT_AKTIV` steht in derselben Datei. | A (Nachtrag); der Schalter ergänzt, weil der Schlüssel allein den Assistenten nicht einschaltet (E‑1130) |
| **E‑1135** | **Vor jedem Schemaeingriff wird das ganze Schema `overlord_monitor` gesichert**, nicht eine Auswahl, weil auch `app_user`, `app_user_mandant` und `bam_spalte` von Hand gepflegt sind ([`betrieb.md`](betrieb.md) §8, [`bam-sollaengen.md`](bam-sollaengen.md) §7.2). | A (Nachtrag) |

**Festlegungen der Umsetzung (1.1.0), zur Bestätigung.** Eine E‑Nummer bekommen sie erst damit, wie
in A2 bis A4; die F-Nummern setzen F29 fort.

| F | Festlegung | Grund |
|---|---|---|
| **F30** | **Die eine Stelle ist `common.auftrag.AssistentFreigabe`**, gebaut in `config.AssistentConfig` aus Schalter und Schlüssel, dem Typkatalog und `Werkzeugbau::geeignet`. Assistent und Aufträge fragen nur sie. | `task` und `assistent` sind Fachpakete und kennen einander nicht; was beide brauchen, liegt in `common` ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §6). Nur `config` sieht, was „eingeschaltet“ heißt, und den Katalog |
| **F31** | **„Anbietbar“ gehört zur Verfügbarkeit:** Kann der Assistent keinen der freigeschalteten Typen als Werkzeug anbieten (E‑1054), hat der Mandant keinen Assistenten. Hat er ihn, wählt das Anlegen den Typ aus dem ganzen Katalog — ein ungeeigneter Typ bleibt anlegbar. | Eine Entscheidung, nicht zwei: Symbol, Gespräche und Anlegen sagen dasselbe. E‑1054 („über `POST /api/tasks` anlegbar“) gilt damit nur für Mandanten mit Assistenten |
| **F32** | **Prüfen und Anlegen fragen die Freigabe zuerst**, vor `anfrageId` und Wiederholung (§6.2). Nicht verfügbar ist `404` — auch ohne Schlüssel, dann für alle Mandanten. Liste, Einzelabruf, Löschen und Typliste sind nicht gesperrt. | E‑1001: Aufträge entstehen ausschließlich über den Assistenten. Folge: Eine Wiederholung mit derselben `anfrageId`, nachdem die Freischaltung entzogen wurde, bekommt `404` statt des vorhandenen Auftrags; in „Ihre Aufträge“ steht er trotzdem |
| **F33** | **Nicht verfügbar ist `404 nicht-gefunden`** statt `503 assistent-nicht-verfuegbar`, auch für ein noch offenes Gespräch. Der Problemtyp `assistent-nicht-verfuegbar` entfällt. `GET /api/assistent` antwortet weiter `200` mit `verfuegbar: false`. | Der Auftrag verlangt `404` wie bei einem nicht zulässigen Mandanten: derselbe feste Rumpf wie für jede unbekannte Ressource ([`mandantentrennung.md`](mandantentrennung.md) §3) |
| **F34** | **Die Freischaltung für `SYSTEM` kommt per Skript**, nicht per Migration (`scripts/assistent-freischaltung-system.sql`, Rücknahme daneben). Das Skript bricht ab, wenn für den Typ schon ein anderer Mandant freigeschaltet ist. | Freischaltungen sind Pflegedaten (E‑1126). Ein Skript, das „ausschließlich `SYSTEM`“ durch Löschen herstellte, nähme bei einem zweiten Lauf ein späteres Ausrollen zurück; dieses bricht stattdessen ab |
| **F35** | **Die Tests legen sich ihre Freischaltung selbst an** (`Testfreischaltungen`): reines `INSERT` auf eine freie Zeile, gelöscht wird genau diese. | Regel 12. Vor `V103` hingen vier DB-Tests an der Freischaltung aus `V100` |
| **F36** | **Der Stand des Assistenten gilt im Frontend je Mandant** (Abfrageschlüssel `["assistent", "stand", <Mandant>]`). | Nach einem Wechsel liest das Symbol neu, auch wenn der Zwischenspeicher nicht geleert würde; die Anfrage selbst trägt keinen Mandanten (M1) |
| **F37** | **Die Zeitgrenze der `/api`-Weiterleitung ist 300 s** (`experimental.proxyTimeout` in `frontend/next.config.ts`) statt der Vorgabe von 30 s. | Ohne Streaming dauert eine Nachricht im ungünstigsten Fall 244 s ([`betrieb.md`](betrieb.md) §5a) |
| **F38** | **Zweige nach dem Zusammenführen:** `fix/<thema>` und `feat/<thema>` von `main`, zurück mit `--no-ff` ([`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). | Der Auftrag legt eine Linie fest, aber keine Zweignamen; das ist die Praxis vor 1.0.0 |

### Leerzeichen im Schlüssel (23.10.2026)

Aus dem Auftrag „Leerzeichen im Schlüssel des Abladestellen-Auftrags“ (Zweig
`fix/auftrag-schluessel-leerzeichen`). Die E‑Nummer ist `E‑1135` plus die Nummer der Entscheidung
dort (E‑1136 bis E‑1141); die vier Rückfragen folgen als E‑1142 bis E‑1145, die bestätigten
Festlegungen der Umsetzung als E‑1146 bis E‑1149. Die nächste freie Nummer war E‑1136, per `grep` über
alle Zweige geprüft. Herkunft: **A** = vom Auftraggeber entschieden, am
21.10.2026 (Auftrag) oder am 23.10.2026 (Rückfragen). Anlass: Die Plattform verarbeitet Schlüssel
mit Leerzeichen bereits; angepasst wird nur der Monitor.

| E | Entscheidung | Herkunft |
|---|---|---|
| **E‑1136** | **Der Schlüssel von `WERK_ABLADESTELLE` darf Leerzeichen enthalten.** Erlaubt ist U+0020. Tabulator, Zeilenumbruch und das geschützte Leerzeichen U+00A0 sind unzulässig. Ändert E‑1012. | A |
| **E‑1137** | **Rand:** Leerzeichen am Anfang und Ende werden entfernt, bevor der Auftrag gespeichert wird — nur U+0020. `String.trim()` und `strip()` passen nicht: Sie entfernten auch andere Zeichen und ließen so eine sonst abgelehnte Eingabe durch. Erst kürzen, dann die erlaubten Zeichen prüfen. | A |
| **E‑1138** | **Innen** zeichengenau, auch mehrere Leerzeichen hintereinander. Kein Zusammenziehen. | A |
| **E‑1139** | Nach dem Kürzen leer heißt ungültig, wie ein fehlender Wert. | A |
| **E‑1140** | Dieselbe Regel für Neuanlage, Anpassung und Löschung, damit alle drei dieselbe Form an die Plattform geben. | A |
| **E‑1141** | **Verbindlich ist das Backend, an genau einer Stelle.** Das Modell übergibt den Schlüssel so, wie der Kunde ihn nennt, und kürzt nicht selbst. Die Karte zeigt den gekürzten Schlüssel, also das, was gespeichert wird. Kein Kürzen im Frontend und im Modell. Wo ein Schlüssel erscheint, zieht der Browser Leerzeichen nicht zusammen; keine Ersatzzeichen wie `·`. | A |
| **E‑1142** | **Nur U+0020 als Leerzeichen, über das Muster der Typdefinition:** `^(?:[^/,\p{Z}]\| ){1,35}$` an allen drei Schlüsselteilen. Es lehnt jedes Zeichen der Unicode-Kategorie Z außer U+0020 ab (U+00A0, U+202F, U+3000 …), mit `FORMAT`. Gesetzt per `JSON_SET`, das den Backslash im JSON maskiert (§3.5). | A (Rückfrage: E‑1012 sagte „kein Leerraum“, das alte Muster `^[^ /,]{1,35}$` sperrte aber nur U+0020 — U+00A0 und andere Leerzeichen gingen still durch) |
| **E‑1143** | **Gekürzt wird je Schlüsselteil**, nicht nur am Rand des zusammengesetzten Schlüssels. Die 1 bis 35 Zeichen gelten für den gekürzten Teil. Folge: Leerzeichen am Trenner fallen weg (`72332 / DDB0` wird `72332/DDB0`), und eine Lieferantennummer nur aus Leerzeichen fehlt — der Schlüssel ist dann zweiteilig. | A (Rückfrage: Der Schlüssel besteht aus drei Feldern, geprüft wird je Feld) |
| **E‑1144** | **Das Kürzen ist eine Regel für die Schlüsselteile jedes Typs**, keine Option der Definition (§3.2). Kein Code nennt einen Typ (E‑1018). | A (Rückfrage) |
| **E‑1145** | **Das Skript läuft auf der Testkopie**, die Produktion bekommt es von Lukas nach dem Deployment (§4.3, [`betrieb.md`](betrieb.md) §7a E). Auf der Testkopie gelaufen am 23.10.2026 (§9.7). | A (Rückfrage: Ohne den Lauf hingen `TestsatzDefinitionDbIT` und die Prüfung des Assistenten am alten Muster) |

**Festlegungen der Umsetzung.** Vorgeschlagen als F39 bis F42, die F-Nummern setzen F38 fort.

> **Bestätigt am 23.10.2026** (Auftraggeber): F39 bis F42 sind **E‑1146 bis E‑1149**, im Anschluss
> an die letzte vergebene Nummer (E‑1145). Die F-Nummer steht in Klammern dahinter, damit Verweise
> von vorher auffindbar bleiben. E‑1146 bis E‑1149 waren auf keinem Zweig vergeben, per `grep`
> geprüft.

| E (F) | Festlegung | Grund |
|---|---|---|
| **E‑1146** (F39) | **Die zugeklappte Zeile in „Ihre Aufträge“ zeigt die Schlüssel mit `white-space: pre`**, nicht mit `pre-wrap`; aufgeklappt, auf der Karte und an den Werten gilt `pre-wrap` ([`assistent-frontend.md`](assistent-frontend.md) §12.6). | Die Zeile ist einzeilig mit Auslassung gekürzt (E‑1102). `pre-wrap` bräche sie an den Leerzeichen um, die Auslassung fiele weg. `pre` hält die Leerzeichen und die eine Zeile |
| **E‑1147** (F40) | **`AuftragsPruefung.pruefe` gibt die gekürzten Einträge mit den Fehlern zurück** (`AuftragsPruefung.Ergebnis`). Anlegen, `POST /api/tasks/pruefung` und der Vorschlag des Assistenten nehmen die Einträge daraus; die Kürzung selbst steht nur dort. | Die eine Stelle aus E‑1141: Wer prüft, bekommt die geltende Fassung mit. Ein getrenntes „kürzen, dann prüfen“ ließe einen Aufrufer prüfen und die ungekürzte Fassung speichern |
| **E‑1148** (F41) | **Die Werkzeugbeschreibung sagt für jeden Typ mit Schlüssel:** „Schlüsselteile übergibst du zeichengenau, mit jedem Leerzeichen, auch mehreren hintereinander; Leerzeichen am Anfang und Ende entfernt das Backend.“ Die Beschreibung des Typs in `task_type` bleibt unverändert ([`assistent.md`](assistent.md) §4.5). | Die Regel gilt für alle Schlüssel (E‑1144), also gehört der Satz in den Werkzeugbau und nicht in einen Typ. Ein Modell neigt dazu, doppelte Leerzeichen zusammenzuziehen |
| **E‑1149** (F42) | **Die Typdefinition ändert ein Skript mit Rücknahme**, keine Migration. **Reihenfolge Pflicht:** erst die Version mit dem Kürzen ausrollen, dann das Skript; wer danach auf ein JAR ohne Kürzen zurückgeht, nimmt vorher das Skript zurück. Das Skript kann die Version nicht prüfen. | E‑1019: Definitionen kommen per SQL in die Produktion. Mit dem neuen Muster und ohne Kürzen nähme 1.1.0 Leerzeichen am Rand mit in Auftrag und Parametertext |

---

## 3. Auftragstypen definieren

*Geschrieben für die Betreuung (Lukas), die neue Typen per SQL anlegt.* Ein Typ, der mit den
Regelbausteinen unten auskommt, braucht keinen Code, kein Deployment und keinen Neustart (E‑1019).

### 3.1 Die Tabellen

**`task_type`** — eine Zeile je Typ.

| Spalte | Typ | Bedeutung |
|---|---|---|
| `task_typ` | `VARCHAR(64)`, Primärschlüssel | Kennung, zum Beispiel `WERK_ABLADESTELLE`. Steht im Auftrag und bestimmt den Ablauf der Plattform. Die Spalte vergleicht ohne Rücksicht auf Groß- und Kleinschreibung; das Backend vergleicht die Kennung in der Anfrage **genau** |
| `name` | `VARCHAR(200)` NOT NULL | Anzeigename, nicht leer |
| `beschreibung` | `TEXT` NOT NULL | Fließtext für den Assistenten: was der Auftrag bewirkt, was er nicht weiß, was er dem Kunden vor dem Anlegen sagen muss |
| `struktur` | JSON NOT NULL, von MariaDB per `JSON_VALID` geprüft | Aufbau nach §3.2 |
| `aktiv` | `BOOLEAN` NOT NULL, Vorgabe `FALSE` | eine neue Zeile ist erst aktiv, wenn sie ausdrücklich gesetzt wird |
| `geaendert_am` | `DATETIME(3)` NOT NULL, Vorgabe und `ON UPDATE` `CURRENT_TIMESTAMP(3)` | nur zur Erkennung von Änderungen (§3.6), wird nicht angezeigt |

**`task_type_mandant`** — die Freischaltung. Eine Zeile `(task_typ, mandant_id)` heißt: Dieser
Mandant bekommt den Typ angeboten, sofern er aktiv und gültig ist. Primärschlüssel über beide
Spalten, Fremdschlüssel auf `task_type` mit `ON DELETE CASCADE` — wer einen Typ löscht, löscht seine
Freischaltungen mit. Die Mandantenkennung ist die aus `GlassfishDB.Mandant`, ohne Fremdschlüssel
über die Schemagrenze.

### 3.2 Die Struktur

Ein JSON-Objekt. **Pflicht** sind `aktionen`, `maxEintraege`, `felder`, `zeile` und `rueckgaben`;
**optional** `schluessel` und `regeln`. Ohne `schluessel` darf keine Vorlage `{schluessel}`
verwenden. **Unbekannte Schlüssel machen die Definition ungültig** — auf jeder Ebene, auch in einem
Feld, einer Regel oder in `rueckgaben`. Ein Tippfehler (`pflichT`, `muser`) fällt so auf, statt still
wirkungslos zu bleiben.

| Schlüssel | Bedeutung |
|---|---|
| `aktionen` | Liste der Aktionen in Klartext, 1 bis 10, eindeutig, je 1 bis 64 Zeichen, ohne Zeilenumbruch und Steuerzeichen. Sie stehen so in der Kopfzeile des Parametertexts |
| `maxEintraege` | Höchstzahl Einträge je Auftrag, ganze Zahl von 1 bis 500 |
| `felder` | Liste der Felder, mindestens eines. Je Feld: `name` (Pflicht, `[a-z][a-z0-9]*`, eindeutig, nicht `schluessel`), `label` (Pflicht, nicht leer — so nennt der Klartext einer Prüfmeldung das Feld), `pflicht` (`true` oder `false`, Vorgabe `false`; gilt bei den Aktionen, bei denen das Feld vorkommt), `muster` (regulärer Ausdruck nach Java, den der **ganze** Wert erfüllen muss, optional), `nurBei` (Liste von Aktionen, optional, nicht leer; außerhalb davon ist das Feld verboten), `beispiel` und `hinweis` (optional, Text, für den Assistenten) |
| `schluessel` | `felder` (Liste von Feldnamen in Reihenfolge, mindestens einer, ohne `nurBei`) und `trenner` (genau ein Zeichen, kein Zeilenumbruch, kein Steuerzeichen). **Nur der erste Teil darf ein Feld ohne `pflicht` sein**, und mindestens ein Teil muss `pflicht` sein. Fehlt der Wert des ersten Teils, entfällt er mit seinem Trenner. **Jeder Teil wird vor der Prüfung um Leerzeichen (U+0020) am Rand gekürzt**, bei jedem Typ; eine Option dafür gibt es nicht (§5.3, E‑1144). Ob ein Leerzeichen im Innern zulässig ist, entscheidet das `muster` |
| `regeln` | Liste aus dem festen Satz in §3.3 |
| `zeile` | je Aktion genau eine Vorlage für die Zeile eines Eintrags, keine für eine unbekannte Aktion. Platzhalter `{feldname}` und `{schluessel}`; ein fehlender Wert wird zu leerem Text. Außerhalb eines Platzhalters keine geschweifte Klammer, kein Zeilenumbruch, kein Steuerzeichen |
| `rueckgaben` | `ok` (der Wert, der Erfolg heißt; Pflicht, nicht leer) und `weitere` (Liste bekannter anderer Werte, optional). Kein Wert mit Leerraum am Rand oder mit Zeilenumbruch — die Rückgabe wird zeilenweise und ohne Leerraum am Rand verglichen (§5.4) |

**Beispiel** — eine Struktur ohne fachliche Bedeutung, nur zur Form:

```json
{
  "aktionen": ["Anlage", "Entfernen"],
  "maxEintraege": 20,
  "felder": [
    { "name": "bereich", "label": "Bereich", "muster": "^[A-Z]{2}$", "beispiel": "NO" },
    { "name": "nummer", "label": "Nummer", "pflicht": true, "muster": "^[0-9]{1,10}$" },
    { "name": "wert", "label": "Wert", "nurBei": ["Anlage"], "pflicht": true,
      "hinweis": "Der Wert, der unter der Nummer hinterlegt wird." }
  ],
  "schluessel": { "felder": ["bereich", "nummer"], "trenner": "-" },
  "regeln": [ { "art": "eindeutig", "felder": ["schluessel"] } ],
  "zeile": { "Anlage": "{schluessel};{wert}", "Entfernen": "{schluessel}" },
  "rueckgaben": { "ok": "OK", "weitere": ["UNBEKANNT"] }
}
```

Daraus entsteht für eine Anlage mit zwei Einträgen der Parametertext

```
Aktion=Anlage
NO-17;abc
42;xyz
```

— im zweiten Eintrag fehlt der optionale erste Schlüsselteil, er entfällt mit seinem Trenner. In
jedem Wert verboten sind dabei `-` (Trenner) und `;` (steht wörtlich in einer Vorlage), §3.4.

### 3.3 Die Regelbausteine

Der feste Satz (E‑1021). Jede Regel ist ein Objekt mit `art`:

| `art` | Schlüssel | Wirkung | Fehlercode |
|---|---|---|---|
| `mindestensEins` | `felder` (Liste von Feldnamen, mindestens einer), `bei` (Liste von Aktionen, optional) | In jedem Eintrag hat mindestens eines der Felder einen Wert. Mit `bei` nur bei diesen Aktionen, sonst bei allen | `MINDESTENS_EINS` |
| `eindeutig` | `felder` (Liste von Feldnamen, mindestens einer; `schluessel` ist als Name zulässig, wenn die Struktur einen Schlüssel hat) | Keine zwei Einträge eines Auftrags tragen dieselbe Kombination dieser Werte. Verglichen wird genau, mit Groß- und Kleinschreibung. Einträge, bei denen alle diese Werte fehlen, zählen nicht | `DOPPELT` |

Ein neuer Baustein ist Code in `AuftragsPruefung` und `DefinitionsPruefung` und steht danach allen
Typen zur Verfügung. Eine Ausdrucks- oder Skriptsprache gibt es bewusst nicht.

### 3.4 Die automatische Zeichenprüfung

Unabhängig von jedem `muster` lehnt das Backend in **jedem** Wert ab (E‑1022, Fehlercode
`UNZULAESSIGES_ZEICHEN`):

- Zeilenumbrüche: LF, CR, `U+0085`, `U+2028`, `U+2029`;
- Steuerzeichen (Unicode-Kategorie Cc, `U+0000` bis `U+001F` und `U+007F` bis `U+009F`), also auch
  den Tabulator;
- jedes Zeichen, das nach Entfernen der Platzhalter in **irgendeiner** Zeilenvorlage des Typs steht;
- den Schlüsseltrenner.

Bei `WERK_ABLADESTELLE` sind das außer Umbrüchen und Steuerzeichen `,` und `/`. Ein Wert kann den
Parametertext damit nicht in zusätzliche Zeilen oder Spalten zerlegen, auch wenn die Definition ein
Muster vergisst. Das `muster` bleibt trotzdem die fachliche Prüfung: Ein Leerzeichen etwa ist kein
Steuerzeichen und fällt nur über das Muster (`FORMAT`), ebenso das geschützte Leerzeichen U+00A0
und jedes andere aus der Unicode-Kategorie Z. Bei `WERK_ABLADESTELLE` lässt das Muster der
Schlüsselteile seit dem 23.10.2026 U+0020 zu und kein anderes (§4.3, E‑1142).

### 3.5 Muster ohne Backslash

**Schreibe Muster ohne Backslash:** `[0-9]` statt `\d`, ein Leerzeichen statt `\s`. MariaDB liest in
einem SQL-Text den Backslash als Escapezeichen: Aus `\\d` im JSON würde beim Einfügen `\d`, das ist
kein gültiges JSON, und die Prüfung `JSON_VALID` wiese die Zeile ab. Tabulator und Zeilenumbruch
muss ein Muster nicht abwehren, das tut die Zeichenprüfung (§3.4).

**Braucht ein Muster doch einen Backslash** — etwa `\p{Z}` für alle Unicode-Leerzeichen, wofür es
keine Schreibweise ohne gibt —, setzt ihn `JSON_SET` mit einem SQL-Text als Wert statt eines
JSON-Texts: `JSON_SET` maskiert den Backslash im JSON selbst. Den Backslash im SQL-Text baut
`CHAR(92 USING utf8mb4)`, dann hängt das Skript nicht am `sql_mode` (`NO_BACKSLASH_ESCAPES`). So
macht es `scripts/werk-abladestelle-schluessel-leerzeichen.sql` (E‑1142); `TaskMigrationDbIT` hält
fest, dass genau ein Backslash ankommt.

Muster sind reguläre Ausdrücke nach `java.util.regex.Pattern` und müssen auf den **ganzen** Wert
passen (`matches`); `^` und `$` schaden nicht und machen das sichtbar.

### 3.6 Was beim Laden geprüft wird — und wo ein Fehler sichtbar wird

Das Backend liest die Definitionen bei jedem Zugriff aus der Datenbank, prüft eine Definition aber
nur einmal je `(task_typ, geaendert_am)` und hält das Ergebnis im Speicher. **Eine geänderte, neue
oder gelöschte Zeile wirkt beim nächsten Zugriff**, ohne Neustart. Gelesen werden je Zugriff nur die
Typen, die für den aktiven Mandanten freigeschaltet und aktiv sind (§5.6).

Geprüft wird beim Laden, jeweils mit Grund:

1. Die Struktur ist ein JSON-Objekt; der Name ist nicht leer.
2. Die Pflichtschlüssel sind da; es gibt keinen unbekannten Schlüssel, auf keiner Ebene.
3. `aktionen`: 1 bis 10, eindeutig, je 1 bis 64 Zeichen, ohne Zeilenumbruch und Steuerzeichen.
4. `maxEintraege`: ganze Zahl von 1 bis 500.
5. `felder`: mindestens eines; je Feld `name` nach `[a-z][a-z0-9]*`, eindeutig, nicht
   `schluessel`; `label` nicht leer; `pflicht` ein Wahrheitswert; jedes `muster` übersetzbar;
   `nurBei` nicht leer und nur aus bekannten Aktionen; `beispiel` und `hinweis` Text.
6. `schluessel`: Felder bekannt, eindeutig und ohne `nurBei`; `trenner` genau ein Zeichen, kein
   Umbruch, kein Steuerzeichen; ab dem zweiten Teil `pflicht`; mindestens ein Teil `pflicht`.
7. `regeln`: jede `art` aus dem festen Satz; jeder Feldname bekannt (`schluessel` nur bei
   `eindeutig` und nur mit Schlüssel); jede Aktion in `bei` bekannt.
8. `zeile`: für jede Aktion genau eine Vorlage, keine für eine unbekannte Aktion; jeder Platzhalter
   ein bekanntes Feld oder `{schluessel}` (nur mit Schlüssel); keine einzelne geschweifte Klammer,
   kein Umbruch, kein Steuerzeichen.
9. `rueckgaben`: `ok` nicht leer, ohne Leerraum am Rand und Umbruch, und nicht in `weitere`; jeder
   Wert in `weitere` ebenso.

**Eine ungültige Definition wird nicht angeboten** und steht mit allen Gründen als `WARN` im Log der
Anwendung — **einmal je Fassung**, nicht bei jedem Zugriff. Sie erscheint weder in der Typliste noch
lässt sie sich anlegen (`404`, E‑1020). **Sichtbar ist das nur im Log** (Punkt 1005); geprüft wird
eine Definition erst, wenn ein Mandant, für den sie freigeschaltet und aktiv ist, das erste Mal
zugreift.

### 3.7 Ein neuer Typ, Schritt für Schritt

**Die Reihenfolge ist Pflicht: erst den Ablauf in der Plattform bauen, dann aktivieren und
freischalten.** Ein aktiver, freigeschalteter Typ ohne Ablauf auf der Plattform führt zu Aufträgen,
die niemand abholt. Die Plattform bekommt die Definition, bevor der Typ aktiviert wird
([`task-schnittstelle.md`](task-schnittstelle.md) §7).

```sql
-- 1. Typ anlegen, inaktiv (aktiv hat die Vorgabe FALSE). Muster ohne Backslash.
INSERT INTO overlord_monitor.task_type (task_typ, name, beschreibung, struktur)
VALUES (
  'BEISPIEL',
  'Beispiel ohne fachliche Bedeutung',
  'Was der Auftrag bewirkt und was der Assistent dem Kunden vor dem Anlegen sagen muss.',
  '{
    "aktionen": ["Anlage"],
    "maxEintraege": 20,
    "felder": [
      { "name": "nummer", "label": "Nummer", "pflicht": true, "muster": "^[0-9]{1,10}$" }
    ],
    "zeile": { "Anlage": "{nummer}" },
    "rueckgaben": { "ok": "OK" }
  }'
);

-- 2. Fuer einen Mandanten freischalten. Noch wirkt nichts: Der Typ ist inaktiv.
INSERT INTO overlord_monitor.task_type_mandant (task_typ, mandant_id) VALUES ('BEISPIEL', 'NEXANS');

-- 3. Erst wenn der Ablauf in der Plattform steht: aktivieren.
UPDATE overlord_monitor.task_type SET aktiv = TRUE WHERE task_typ = 'BEISPIEL';
```

Danach in der Typliste des Mandanten nachsehen (`GET /api/tasks/typen`). Fehlt der Typ dort, steht
der Grund im Log (§3.6). Zurücknehmen: `aktiv = FALSE` — der Typ verschwindet beim nächsten Zugriff
aus dem Angebot; angelegte Aufträge bleiben unberührt.

*Berichtigt am 19.10.2026 (E‑1119): Hier stand zweimal „Lobster“ als der, der den Ablauf baut. Abgeholt
und ausgeführt wird von der Plattform.*

### 3.8 Eine Änderung wirkt nur auf neue Aufträge

Jeder Auftrag trägt eine Kopie von Name und Struktur, mit der er angelegt wurde (E‑1023). Anzeige
und Einordnung lesen die Kopie. Wer `rueckgaben` oder eine Vorlage ändert, ändert also nichts an
Aufträgen, die schon in `task` stehen — auch nicht an solchen, die die Plattform noch nicht
abgeholt hat: Deren Parametertext ist fertig gebaut.

### 3.9 Den Assistenten für einen Mandanten freischalten *(1.1.0, 21.10.2026)*

**Der Assistent ist für einen Mandanten verfügbar, wenn drei Dinge zusammenkommen** — und genau
eine Stelle prüft sie, `AssistentFreigabe` (E‑1126, E‑1130, F30):

1. Schalter an (`OVERLORD_ASSISTENT_AKTIV=true`) **und** Schlüssel gesetzt
   (`OVERLORD_ASSISTENT_API_KEY`), [`betrieb.md`](betrieb.md) §3;
2. für den Mandanten ist mindestens ein Typ freigeschaltet, der aktiv und gültig ist (E‑1020, §3.6);
3. der Assistent kann mindestens einen davon als Werkzeug anbieten (E‑1054, F31).

| | verfügbar | nicht verfügbar |
|---|---|---|
| Symbol in der Kopfleiste (`GET /api/assistent`) | `verfuegbar: true` | `verfuegbar: false`, kein Symbol |
| Gespräch eröffnen, Nachricht ([`assistent.md`](assistent.md) §6) | wie beschrieben | **`404`**, kein Aufruf beim Anbieter |
| Prüfen und Anlegen (§6.2, `POST /api/tasks/pruefung`) | Typ aus dem Katalog des Mandanten | **`404`**, kein Auftrag |
| Typliste, „Ihre Aufträge“, Einzelabruf, Löschen | unverändert | unverändert |

**`404` ist der feste Rumpf jeder unbekannten Ressource**, wie bei einem nicht zulässigen Mandanten
(F33). Ob ein anderer Mandant freigeschaltet ist, lässt sich darüber nicht erfragen. Gelesen wird bei
jedem Zugriff, mit dem einen Statement des Typkatalogs (§5.6, gemessen in M1000) — eine neue oder
entfernte Freischaltung wirkt im Backend beim nächsten Zugriff, ohne Neustart. Das Symbol fragt
einmal je Mandant und Seitenaufruf; es zeigt eine Änderung nach dem nächsten Neuladen der Seite oder
Mandantenwechsel (F36).

**Stand mit 1.1.0:** `WERK_ABLADESTELLE` ist **ausschließlich für `SYSTEM`** freigeschaltet
(E‑1125). `V103` nimmt die beiden Freischaltungen aus `V100` zurück, `SYSTEM` schaltet ein Skript
frei (F34):

| Skript | Was es tut |
|---|---|
| [`scripts/assistent-freischaltung-system.sql`](../scripts/assistent-freischaltung-system.sql) | schaltet `SYSTEM` frei; idempotent. Bricht ab, wenn das Vorgabeschema nicht das eigene ist, wenn `V103` noch nicht angewandt ist oder wenn für den Typ schon ein anderer Mandant freigeschaltet ist |
| [`scripts/assistent-freischaltung-system-ruecknahme.sql`](../scripts/assistent-freischaltung-system-ruecknahme.sql) | nimmt die Freischaltung für `SYSTEM` zurück; idempotent |

Beide laufen in MySQL Workbench mit dem Schreibbenutzer, Vorgabeschema das eigene Schema, über
„Execute (All or Selection) to Text“; die Einzelheiten stehen im Kopf der Skripte. Lukas führt sie in
der Produktion aus, **nach dem ersten Start von 1.1.0**, nach der Deployment-Routine über die
Kommandozeile (`mysql … overlord_monitor < scripts/…`, [`betrieb.md`](betrieb.md) §7a C). Der Client
bricht dort wie die Workbench beim ersten Fehler ab (geprüft am 21.10.2026). Auf der Testkopie sind
beide am 21.10.2026 gelaufen (§9.6). Ein Auftrag unter `SYSTEM` ist ein Auftrag wie jeder andere:
Die Plattform holt ihn nach [`task-schnittstelle.md`](task-schnittstelle.md) ab.

**Ausrollen auf einen weiteren Mandanten** ist ein `INSERT`, kein Deployment (E‑1126). Die
Reihenfolge aus §3.7 gilt auch hier: Der Typ ist aktiv, weil sein Ablauf auf der Plattform steht.

```sql
-- 1. Ist es der richtige Mandant? Die Kennung ist die aus GlassfishDB.Mandant (nur lesen).
SELECT MandantID, MandantName FROM GlassfishDB.Mandant WHERE MandantID = '<Mandantenkennung>';

-- 2. Freischalten. Wirkt beim naechsten Zugriff, ohne Neustart.
INSERT INTO overlord_monitor.task_type_mandant (task_typ, mandant_id)
VALUES ('WERK_ABLADESTELLE', '<Mandantenkennung>');

-- 3. Welche Mandanten haben den Typ jetzt?
SELECT mandant_id FROM overlord_monitor.task_type_mandant
WHERE task_typ = 'WERK_ABLADESTELLE' ORDER BY mandant_id;
```

Die Schemanamen sind die der Testkopie; in der Produktion gelten die aus `OVERLORD_DB_*`. Nachsehen
im Mandanten: Das Symbol steht, `GET /api/assistent` meldet `verfuegbar: true`.

**Zurücknehmen:** dieselbe Zeile löschen
(`DELETE FROM overlord_monitor.task_type_mandant WHERE task_typ = 'WERK_ABLADESTELLE' AND mandant_id = '<Mandantenkennung>'`).
Beim nächsten Zugriff ist der Assistent für den Mandanten nicht mehr verfügbar. Offene Aufträge
bleiben in `task`, und die Plattform holt sie weiter ab; „Ihre Aufträge“ zeigt sie, und gelöscht
werden können sie dort, solange sie offen sind (§6.6).

---

## 4. Der erste Typ `WERK_ABLADESTELLE`

### 4.1 Quelle: die Taskdefinition des Auftraggebers (15.10.2026), wortgleich

```
Taskname :
Anpassen der Werk-/Abladestellentabelle

Aktionen :
Anlegen eines oder mehrerer neuer Einträge, Anpassen eines oder mehrerer vorhandener Einträge, Löschen eines oder mehrerer neuer Einträge

Beschreibung :
Die EDI Plattform erzeugt aus den eingehenden Partnernachrichten IDOC Nachrichten für das Kundensystem. Dazu wird eine SNDPRN benötigt. Dieses ist ein sechsstelliger, numerischer Wert. Ist dieser nicht als Fixwert hinterlegt muss er über eine Kombination aus der Lieferantenummer, dem Werk und der Abladestelle, die aus der Eingangsnachricht gelesen werden, ermittelt werden. Ist die Lieferantennummer nicht ermittelbar, kann alternativ auch nur mit der Kombination aus dem Werk und der Abladestelle gearbeitet werden.
Ein Werk-/Abladestellentabelleneintrag besteht aus drei Teilen.
Dem Schlüssel, der SNDPRN, sowie der SNDPRN_4908 für die VDA4908 Prozesse.
Der Schlüssel setzt sich aus zwei (Werk und Abladestelle) oder drei Werten zusammen (Lieferantennummer und Werk und Abladestelle).
Die SNDPRN ist ein sechsstelliger, numerischer Wert und optional.
Die SNDPRN_4908 ist ein sechsstelliger, numerischer Wert und optional.
Mindestens einer der beiden SNDPRN Werte muss angegeben werden.
Der Schlüssel ist verpflichtend.
Die Übergabe mehrerer Listeneinträge zur Neuanlage soll möglich sein.
Die Übergabe mehrerer Listeneinträge zur Löschung soll möglich sein, dabei reicht es den Schlüssel des Eintrags anzugeben.
Die Übergabe mehrerer Listeneinträge zur Anpassung soll möglich sein, dabei reicht es den Schlüssel des Eintrags anzugeben, der angepasst werden soll, sowie die neu zu setzenden Werte für SNDPRN und SNDPRN_4908.
Die vom Bediener im Dialog ermittelten Werte sollen wie folgt in der Tasktabelle als Parameter für die Auftragsausführung hinterlegt werden :
Aktion=Neuanlage oder Löschung oder Anpassung
Liste der einzelnen Aktionen

Beispiel : Neuanlage
Aktion=Neuanlage
72332/DDB0,120764,
72261/DDR0,120764,
010/18VZE,120055,100050
0101159903/22/001N0,123085,

Beispiel : Löschung
Aktion=Löschung
010/18VZE
0101159903/22/001N0

Beispiel : Anpassung
Aktion=Anpasung
010/18VZE,123456,123456
0101159903/22/001N0,234567,

Mandanten :
NEXANS|IBIS

Task eingestellt am :
Zeitpunkt, zu dem der Task zur Verfügung gestellt wurde

Task ausgeführt am :
Zeitpunkt, zu dem der Task zurückgemeldet wurde

Rückgabewerte :
Zu jedem Listeneintrag wird eine Rückmeldung gegeben. Folgende Werte sind möglich : Ok, Schlüssel nicht gefunden
Beispiel für eine Übergabeliste mit drei Einträgen :
Ok
Schlüssel nicht gefunden
Ok
```

### 4.2 Lesarten und Regeln

- **Lesarten (E‑1017):** „Anpasung“ im Beispiel ist „Anpassung“. „Löschen eines oder mehrerer
  *neuer* Einträge“ heißt „*vorhandener*“.
- **Anpassung, leeres SNDPRN-Feld (E‑1007):** heißt „Wert wird entfernt“. Mindestens einer der
  beiden Werte muss gesetzt bleiben. Die Bedeutung steht in der Beschreibung und im
  Plattformvertrag; der Monitor prüft nur „mindestens eins“.
- **Neuanlage mit vorhandenem Schlüssel (E‑1008):** Eintrag bleibt unverändert, Rückgabe
  `Schlüssel bereits vorhanden` — ein dritter Rückgabewert über die Quelle hinaus.
- **Schlüsselteile (E‑1012, geändert durch E‑1136 bis E‑1143):** 1 bis 35 Zeichen, kein `/`, kein
  `,`; Groß- und Kleinschreibung bleiben wie eingegeben. **Leerzeichen (U+0020) sind erlaubt**, auch
  mehrere hintereinander, und bleiben zeichengenau. Am Rand kürzt das Backend sie vor der Prüfung,
  je Teil; die 35 gelten für den gekürzten Teil. Jedes andere Leerzeichen — Tabulator, Umbruch,
  U+00A0, U+202F, U+3000 — ist unzulässig. Ein Teil nur aus Leerzeichen fehlt: Bei Werk und
  Abladestelle ist das `PFLICHT_FEHLT`, eine solche Lieferantennummer entfällt, und der Schlüssel ist
  zweiteilig. Die 35 sind eine technische Schranke (Punkt 1002).
- **SNDPRN und SNDPRN_4908 (E‑1013):** leer oder genau sechs Ziffern.
- **Doppelte Schlüssel (E‑1014)** innerhalb eines Auftrags werden abgelehnt.
- **Höchstens 20 Einträge (E‑1058).** Bis zur Migration `V101` waren es 50 (E‑1015).
- **Anpassung mit nur einem Wert (16.10.2026, Auftraggeber):** Nennt der Kunde bei einer
  Anpassung nur einen der beiden SNDPRN-Werte, fragt der Assistent nach, ob der andere
  entfernt werden soll oder welchen Wert er behalten soll — sonst würde der nicht genannte
  Wert stillschweigend entfernt (E‑1007). Der Satz steht in der Beschreibung (§4.3), nicht
  im Code.
- **Neu oder ändern unklar (16.10.2026, Auftraggeber, E‑1061):** Ist unklar, ob ein Eintrag
  neu angelegt oder geändert werden soll, schlägt der Assistent die Neuanlage vor. Gibt es den
  Schlüssel schon, bleibt der Eintrag unverändert (E‑1008). Eine Anpassung schlägt er nur vor,
  wenn der Kunde ausdrücklich etwas ändern will. Der Absatz steht seit `V102` in der
  Beschreibung (§4.3), nicht im Code.
- **Mandanten:** in der Quelle `NEXANS` und `IBIS`; `V100` hat den Typ für beide freigeschaltet.
  **Seit 1.1.0 ausschließlich für `SYSTEM`** (E‑1125): `V103` nimmt die beiden Freischaltungen
  zurück, das Skript aus §3.9 schaltet `SYSTEM` frei.

### 4.3 Die Definition, Stand nach `V102` und dem Skript vom 23.10.2026

> **Geändert am 16.10.2026 mit `V101` und `V102`.** `V100` hat den Typ mit `maxEintraege` 50
> und einer kürzeren Beschreibung angelegt und ist unverändert. `V101` setzt `maxEintraege` per
> `JSON_SET` auf 20 (E‑1058) und schreibt die Beschreibung neu: „bis zu 20 Einträge“ statt
> „bis zu 50“ und der Satz zur Anpassung mit nur einem Wert (§4.2). `V102` schreibt die
> Beschreibung noch einmal neu, mit dem Absatz zu „neu oder ändern“ (E‑1061, §4.2); die
> Struktur ändert sie nicht. Aufträge von vorher tragen ihre Kopie (E‑1023).

> **Geändert am 23.10.2026 per Skript, nicht per Migration** (E‑1136, E‑1142, F42).
> [`scripts/werk-abladestelle-schluessel-leerzeichen.sql`](../scripts/werk-abladestelle-schluessel-leerzeichen.sql)
> setzt per `JSON_SET` das Muster der drei Schlüsselteile von `^[^ /,]{1,35}$` auf
> `^(?:[^/,\p{Z}]| ){1,35}$`; sonst ändert es nichts, auch nicht die Beschreibung. Die Rücknahme
> [`scripts/werk-abladestelle-schluessel-leerzeichen-ruecknahme.sql`](../scripts/werk-abladestelle-schluessel-leerzeichen-ruecknahme.sql)
> setzt das alte Muster zurück. Beide sind idempotent, mit drei Wachen wie die Skripte aus §3.9
> (eigenes Schema; Typ da und die ersten drei Felder sind die Schlüsselteile; jedes der drei Muster
> ist das alte oder das neue). **Erst nach dem Deployment der Version mit dem Kürzen ausführen**
> ([`betrieb.md`](betrieb.md) §7a E): 1.1.0 kürzt nicht und nähme mit dem neuen Muster Leerzeichen am
> Rand mit in den Auftrag. Auf der Testkopie gelaufen am 23.10.2026 (§9.7, E‑1145). Was hier steht,
> ist der Stand nach dem Skript.

Name: **Anpassen der Werk-/Abladestellentabelle**, `aktiv = TRUE`.

```json
{
  "aktionen": ["Neuanlage", "Anpassung", "Löschung"],
  "maxEintraege": 20,
  "felder": [
    { "name": "lieferantennummer", "label": "Lieferantennummer",
      "muster": "^(?:[^/,\\p{Z}]| ){1,35}$", "beispiel": "0101159903" },
    { "name": "werk", "label": "Werk", "pflicht": true,
      "muster": "^(?:[^/,\\p{Z}]| ){1,35}$", "beispiel": "72332" },
    { "name": "abladestelle", "label": "Abladestelle", "pflicht": true,
      "muster": "^(?:[^/,\\p{Z}]| ){1,35}$", "beispiel": "DDB0" },
    { "name": "sndprn", "label": "SNDPRN",
      "muster": "^[0-9]{6}$", "nurBei": ["Neuanlage", "Anpassung"], "beispiel": "120764",
      "hinweis": "Bei Anpassung heißt leer: der Wert wird entfernt." },
    { "name": "sndprn4908", "label": "SNDPRN_4908",
      "muster": "^[0-9]{6}$", "nurBei": ["Neuanlage", "Anpassung"], "beispiel": "100050",
      "hinweis": "Für VDA-4908-Prozesse. Bei Anpassung heißt leer: der Wert wird entfernt." }
  ],
  "schluessel": { "felder": ["lieferantennummer", "werk", "abladestelle"], "trenner": "/" },
  "regeln": [
    { "art": "mindestensEins", "felder": ["sndprn", "sndprn4908"], "bei": ["Neuanlage", "Anpassung"] },
    { "art": "eindeutig", "felder": ["schluessel"] }
  ],
  "zeile": {
    "Neuanlage": "{schluessel},{sndprn},{sndprn4908}",
    "Anpassung": "{schluessel},{sndprn},{sndprn4908}",
    "Löschung": "{schluessel}"
  },
  "rueckgaben": { "ok": "Ok", "weitere": ["Schlüssel nicht gefunden", "Schlüssel bereits vorhanden"] }
}
```

Im JSON steht der Backslash maskiert; das Muster der Schlüsselteile selbst ist
`^(?:[^/,\p{Z}]| ){1,35}$`: 1 bis 35 Zeichen, jedes entweder U+0020 oder keines aus `/`, `,` und der
Unicode-Kategorie Z. Es sieht den gekürzten Teil (§5.3). Tabulator und Umbruch passieren das Muster
und fallen über die Zeichenprüfung (§3.4).

Die Beschreibung (Fließtext für den Assistenten), wortgleich aus dem Auftrag an die
Plattform-Entwicklung vom 16.10.2026, mit den beiden Änderungen aus `V101` und dem Absatz aus
`V102`:

```
Die EDI-Plattform erzeugt aus eingehenden Partnernachrichten IDOC-Nachrichten für das Kundensystem und braucht dafür eine SNDPRN. Ist sie nicht fest hinterlegt, ermittelt die Plattform sie über die Werk-/Abladestellentabelle des Kunden. Mit diesem Auftrag legt der Kunde Einträge dieser Tabelle an, passt sie an oder löscht sie.

Ein Eintrag besteht aus einem Schlüssel und zwei Werten. Der Schlüssel besteht aus Werk und Abladestelle, wahlweise mit der Lieferantennummer davor; die Lieferantennummer gehört nur dazu, wenn der Kunde sie für diesen Eintrag nennt. Die Werte sind SNDPRN und SNDPRN_4908 (für VDA-4908-Prozesse), jeweils sechsstellig numerisch.

Neuanlage: Schlüssel und mindestens einer der beiden Werte. Ist der Schlüssel schon vorhanden, bleibt der Eintrag unverändert, und die Rückmeldung lautet „Schlüssel bereits vorhanden“.
Anpassung: der Schlüssel eines vorhandenen Eintrags und die neuen Werte. Ein leer gelassener Wert wird entfernt, mindestens einer muss gesetzt bleiben. Nenne dem Kunden vor dem Anlegen jeden Wert, der dadurch entfernt wird, denn er sieht die bisherigen Werte nicht. Nennt der Kunde bei einer Anpassung nur einen der beiden Werte, frage nach, ob der andere entfernt werden soll oder welchen Wert er behalten soll.
Löschung: nur der Schlüssel.

Ist unklar, ob ein Eintrag neu angelegt oder geändert werden soll, schlage die Neuanlage vor. Gibt es den Schlüssel schon, bleibt der Eintrag unverändert. Eine Anpassung schlägst du nur vor, wenn der Kunde ausdrücklich etwas ändern will.

Ein Auftrag enthält eine Aktion für bis zu 20 Einträge. Ob ein Schlüssel existiert, weiß der Assistent nicht; das zeigt erst die Ausführung.
```

**Woran die Regeln hängen:** Der Schlüssel aus zwei oder drei Teilen ist `schluessel` mit der
optionalen `lieferantennummer` vorn. „Löschung: nur der Schlüssel“ ist `nurBei` an beiden
SNDPRN-Feldern — eine Löschung mit SNDPRN wird mit `FELD_NICHT_ERLAUBT` abgelehnt. „Mindestens
einer der beiden Werte“ ist `mindestensEins` bei Neuanlage und Anpassung. Keine Zeile Code nennt den
Typ (E‑1018); ein Test hält das fest (§9).

---

## 5. Datenmodell, Parametertext, Einordnung

### 5.1 Die Migration `V100__task.sql`

Die erste Migration der Linie `assistent` (Migrationen ab `V100`,
[`DEVELOPMENT_GUIDELINES.md`](../DEVELOPMENT_GUIDELINES.md) §12). Zeichensatz `utf8mb4` und Sortierung
`utf8mb4_general_ci` stehen explizit an jeder Tabelle; die Kennungen `task_id` und `anfrage_id`
tragen `utf8mb4_bin` ([`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §5). Kein Fremdschlüssel
nach `GlassfishDB`. Die JSON-Spalten sind `LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin` mit
`CHECK (JSON_VALID(…))` — das ist genau das, was MariaDB aus dem Typ `JSON` macht, hier aber mit
ausgeschriebener Sortierung. Die Migration legt `WERK_ABLADESTELLE` an (§4.3; geändert mit `V101` und `V102`) und schaltet ihn für
`NEXANS` und `IBIS` frei. **`V103` nimmt diese beiden Freischaltungen mit 1.1.0 zurück** (§3.9,
E‑1125); seit dem Zusammenführen gibt es eine Linie, neue Migrationen schließen an `V103` an
(E‑1127).

**`task`**

| Spalte | Typ | Bemerkung |
|---|---|---|
| `task_id` | `CHAR(36)` `utf8mb4_bin`, Primärschlüssel | UUID, vom Backend erzeugt |
| `mandant_id` | `VARCHAR(36)` NOT NULL | aus der Sitzung |
| `task_typ` | `VARCHAR(64)` NOT NULL | ohne Fremdschlüssel: Der Auftrag trägt seine Kopie (E‑1023) |
| `definition` | JSON NOT NULL | Kopie zum Anlagezeitpunkt: `{"name": …, "struktur": …}`, die Struktur inhaltlich unverändert aus `task_type` (dieselbe Fassung, die beim Anlegen geprüft wurde) |
| `aktion` | `VARCHAR(64)` NOT NULL | eine der Aktionen der Kopie |
| `eintraege` | JSON NOT NULL | die geprüften Einträge in Eingabereihenfolge, je Eintrag ein Objekt Feldname → Wert, nur Felder mit Wert, in der Reihenfolge der Definition |
| `anfrage_id` | `CHAR(36)` `utf8mb4_bin` NOT NULL | vom Client je Absenden erzeugt, für Wiederholungen; klein geschrieben gespeichert |
| `parameter` | `TEXT` NOT NULL | Parametertext (§5.2) |
| `status` | `VARCHAR(30)` NOT NULL | `OFFEN`, `IN_AUSFUEHRUNG`, `ERLEDIGT`, `FEHLGESCHLAGEN` |
| `rueckgabe` | `TEXT` NULL | von der Plattform, eine Zeile je Eintrag |
| `fehlercode` | `VARCHAR(64)` NULL | nur bei `FEHLGESCHLAGEN` |
| `fehlertext` | `VARCHAR(1000)` NULL | Klartext, wird dem Kunden gezeigt |
| `eingestellt_am` | `DATETIME(3)` NOT NULL | UTC, im `INSERT` über `UTC_TIMESTAMP(3)` (E‑1011) |
| `abgeholt_am` | `DATETIME(3)` NULL | UTC, setzt die Plattform |
| `ausgefuehrt_am` | `DATETIME(3)` NULL | UTC, setzt die Plattform bei der Rückmeldung |
| `angelegt_von` | `VARCHAR(190)` NOT NULL | Benutzername des Anlegenden, wie `audit_log.actor_username`, ohne Fremdschlüssel |

**`angelegt_von` ist `VARCHAR(190)`** — dieselbe Breite wie `audit_log.actor_username`, nach dessen
Muster die Spalte gebaut ist. Der Auftrag an die Plattform vom 16.10.2026 nannte vorläufig
`VARCHAR(100)`; die Plattform liest die Spalte nicht.

Indizes:

| Index | Spalten | Zweck |
|---|---|---|
| `PRIMARY` | `task_id` | Einzelabruf, bedingte Updates der Plattform |
| `task_mandant_idx` | `mandant_id, eingestellt_am` | Liste des Mandanten, neueste zuerst |
| `task_status_idx` | `status, eingestellt_am` | Abfrage der offenen Aufträge durch die Plattform |
| `uq_task_anfrage` (eindeutig) | `mandant_id, anfrage_id` | Wiederholung mit derselben `anfrageId` |

`task_type` hat nur `PRIMARY (task_typ)`, `task_type_mandant` nur `PRIMARY (task_typ, mandant_id)`;
der Fremdschlüssel `fk_task_type_mandant_typ` nutzt den Primärschlüssel und braucht keinen eigenen
Index. Gehalten von `IndexbestandDbIT` über die Sollliste.

### 5.2 Parametertext (exakt)

Erste Zeile `Aktion=<Aktion>`, dann je Eintrag die Zeile nach der Vorlage seiner Aktion, in
Eingabereihenfolge. `{schluessel}` sind die Schlüsselteile mit Wert, verbunden mit dem Trenner;
`{feldname}` ist der Wert oder leerer Text. Ersetzt wird in einem Durchgang — ein Wert, der wie ein
Platzhalter aussieht, wird nicht noch einmal ersetzt. Zeilenende LF, **kein** abschließendes LF.
Kodierung UTF-8.

Mit `WERK_ABLADESTELLE` entstehen aus den entsprechenden Eingaben exakt die drei Texte der Quelle
(mit der Lesart „Anpassung“):

```
Aktion=Neuanlage
72332/DDB0,120764,
72261/DDR0,120764,
010/18VZE,120055,100050
0101159903/22/001N0,123085,
```

```
Aktion=Löschung
010/18VZE
0101159903/22/001N0
```

```
Aktion=Anpassung
010/18VZE,123456,123456
0101159903/22/001N0,234567,
```

**Die Spalte ist `TEXT`, höchstens 65.535 Bytes.** Bei `WERK_ABLADESTELLE` liegt der längste
mögliche Text bei rund 2,4 KB: 20 Zeilen zu höchstens 121 Zeichen (drei Schlüsselteile zu 35, zwei
Trenner, zwei Werte zu sechs Ziffern, zwei Kommata) plus Kopfzeile, hergeleitet, nicht gemessen;
bis `V101`, mit 50 Einträgen, waren es rund 6 KB. Ein Typ ohne Muster mit vielen Einträgen könnte die Grenze reißen; das
Backend lehnt einen solchen Auftrag deshalb mit `ZU_LANG` ab, statt an der Datenbank zu scheitern.

### 5.3 Prüfung eines Auftrags

**Zuerst werden die Schlüsselteile gekürzt** *(seit 23.10.2026, E‑1137, E‑1143, E‑1144)*: In
jedem Eintrag verliert jedes Feld aus `schluessel.felder` Leerzeichen (U+0020) am Anfang und am
Ende — nur dieses Zeichen; Tabulator, U+00A0 und alles andere bleiben stehen und fallen unten
durch. Innen bleibt jedes Zeichen, auch mehrere Leerzeichen hintereinander. Alle Prüfungen sehen
den gekürzten Wert, und **dieselbe Fassung wird gespeichert, in den Parametertext gebaut und dem
Assistenten als Karte gezeigt**: `AuftragsPruefung.pruefe` gibt sie mit den Fehlern zurück
(`AuftragsPruefung.Ergebnis`, F40). Das ist die einzige Stelle, an der gekürzt wird.

Dann in dieser Reihenfolge, **alle Fehler gesammelt** statt beim ersten abzubrechen:

1. Die Aktion ist eine der Aktionen der Definition (`AKTION_UNBEKANNT`).
2. Die Zahl der Einträge liegt zwischen 1 und `maxEintraege` (`ZU_WENIGE_EINTRAEGE`,
   `ZU_VIELE_EINTRAEGE`).
3. Je Eintrag: nur bekannte Feldnamen (`FELD_UNBEKANNT`); ein Feld außerhalb seiner
   `nurBei`-Aktionen trägt keinen Wert (`FELD_NICHT_ERLAUBT`); jedes `pflicht`-Feld hat bei den
   Aktionen, bei denen es vorkommt, einen Wert (`PFLICHT_FEHLT`); jeder Wert passt auf sein `muster`
   (`FORMAT`); kein Wert enthält ein Zeichen nach §3.4 (`UNZULAESSIGES_ZEICHEN`).
4. Die Regeln der Definition (`MINDESTENS_EINS`, `DOPPELT`).

`null` und leerer Text heißen gleichermaßen „fehlend“; ein Schlüsselteil nur aus Leerzeichen ist nach
dem Kürzen leer und fehlt also. **Sonst wird kein Wert verändert**: Ein Feld außerhalb des
Schlüssels wird nicht gekürzt, `" 120764"` als SNDPRN ist ein Wert mit Leerzeichen und fällt über
das Muster. Ist die
Aktion unbekannt, entfallen die Prüfungen, die an der Aktion hängen (`nurBei`, `pflicht`, Regeln
mit `bei`); alle übrigen laufen. Ein Wert, der zugleich das Muster verfehlt und ein verbotenes
Zeichen trägt, bekommt beide Meldungen. `DOPPELT` steht am späteren der beiden Einträge.

Jeder Fehler trägt den Index des Eintrags (ab 0, `null` für den Auftrag als Ganzes), das Feld
(`null`, wenn die Meldung kein einzelnes Feld betrifft), einen stabilen Code und einen deutschen
Klartext, der das Feld mit seinem `label` nennt und den Eintrag ab 1 zählt
(„Eintrag 3, SNDPRN: …“).

### 5.4 Rückgabe lesen

An LF trennen; ein abschließendes CR je Zeile entfernen; Leerraum am Rand entfernen. Endet die
Rückgabe mit einem Zeilenende, entsteht dadurch keine zusätzliche leere Zeile. Jede Zeile wird
gegen die **Kopie** der Definition im Auftrag gehalten:

| Zeile | Ergebnis |
|---|---|
| gleich `rueckgaben.ok` | `OK` |
| in `rueckgaben.weitere` | `NICHT_OK` |
| jeder andere Wert | `UNBEKANNT` — zählt als nicht Ok |

Verglichen wird genau, mit Groß- und Kleinschreibung. **Der Rohwert wird immer mitgeliefert und nie
umgedeutet.** Eine Rückgabe, die fehlt (`NULL`) oder nur aus Leerraum besteht, gilt als fehlend.

### 5.5 Einordnung (`TaskStatusClassifier`)

Die eine Stelle, an der aus gespeichertem Status und Rückgabe der Gesamtstand wird (E‑1009).

| gespeicherter Status | Rückgabe | Einordnung |
|---|---|---|
| `OFFEN` | beliebig | `OFFEN` |
| `IN_AUSFUEHRUNG` | beliebig | `IN_AUSFUEHRUNG` |
| `ERLEDIGT` | Zeilenzahl gleich Eintragszahl, alle `OK` | `ERLEDIGT` |
| `ERLEDIGT` | Zeilenzahl gleich Eintragszahl, mindestens eine nicht `OK` | `TEILWEISE_ERLEDIGT` |
| `ERLEDIGT` | fehlt, oder Zeilenzahl passt nicht zur Eintragszahl | `UNSTIMMIG` |
| `FEHLGESCHLAGEN` | beliebig | `FEHLGESCHLAGEN` |
| anderer Wert | beliebig | `UNBEKANNT`, der Rohwert steht in `status` |

`TEILWEISE_ERLEDIGT` gilt auch, wenn **kein** Eintrag Ok ist — Regel E‑1009 unterscheidet das
nicht; wie die Anzeige damit umgeht, ist offen (Punkt 1000).

### 5.6 Der Typkatalog

`TypKatalog` liefert die einem Mandanten angebotenen Typen: aktiv, für ihn freigeschaltet (beides
im Statement) und gültig (beim Laden geprüft). Er liest bei jedem Zugriff **ein** Statement — die
freigeschalteten aktiven Zeilen samt Struktur — und prüft nur Zeilen, deren `(task_typ,
geaendert_am)` er noch nicht kennt. Die Prüfung samt `WARN` läuft damit einmal je Fassung und nicht
bei jedem Aufruf; eine Änderung per SQL ändert `geaendert_am` (`ON UPDATE`) und wirkt beim nächsten
Zugriff. Gemessen in M1000 (§9).

> **Ergänzt 16.10.2026 (A2).** `TypKatalog`, `TypDefinition`, `DefinitionsPruefung`,
> `AuftragsPruefung`, `Prueffehler` und `Fehlercode` liegen seit A2 in `common.auftrag`, weil der
> Assistent sie ebenfalls braucht (E‑1037, [`assistent.md`](assistent.md) §4.1). Das Lesen der
> Typen heißt seitdem `AuftragstypRepository.angeboteneTypen` statt
> `AuftragRepository.angeboteneTypen`; Statement und Verhalten sind unverändert. Wo diese Datei
> die alten Orte nennt (§9.2, §9.3, §10), gilt das neue.

**Warum kein Zwischenspeicher mit Ablaufzeit:** Er hielte eine deaktivierte Definition bis zum
Ablauf im Angebot. Das eine Statement liest zwei Tabellen mit einer Handvoll Zeilen; was es kostet,
steht in M1000.

---

## 6. Endpunkte

Mandant ausschließlich aus der Sitzung. **Keiner der vier Endpunkte nimmt eine Mandanten-ID
entgegen**; es entsteht keine neue Ausnahme von M1 ([`mandantentrennung.md`](mandantentrennung.md)
§3). Ein untergeschobenes Feld `mandantId` im Anfragekörper wird ignoriert. Rollen `MANDANT` und
`ADMIN`, jeweils für den aktiven Mandanten; ohne aktiven Mandanten `403 kein-mandant-gewaehlt`. Der
Änderungszwang beim Passwort sperrt die Endpunkte wie alle übrigen. Pfade nach E‑1025.

Kein Endpunkt hat ein Zeitfenster und keiner blättert — das deckt E‑1016: `task` ist nicht aus
`GlassfishDB` abgeleitet, die Regeln 1 bis 6 gelten dort nicht.

> **Seit A4 (19.10.2026) kommt `DELETE /api/tasks/{taskId}` dazu** (§6.6). Auch er nimmt keine
> Mandanten-ID entgegen und ist keine Ausnahme von M1; Rollen, aktiver Mandant und Änderungszwang wie
> oben. `POST /api/tasks/pruefung` steht seit A2 in [`assistent.md`](assistent.md) §6.5.

> **Seit 1.1.0 (21.10.2026) gesperrt, wo der Assistent nicht verfügbar ist** (§3.9, F32):
> `POST /api/tasks` und `POST /api/tasks/pruefung` antworten dann `404`. `GET /api/tasks/typen`,
> `GET /api/tasks`, `GET /api/tasks/{taskId}` und `DELETE /api/tasks/{taskId}` bleiben, wie sie
> sind. Es entsteht kein neuer Endpunkt und keine Ausnahme von M1.

### 6.1 `GET /api/tasks/typen`

Die dem aktiven Mandanten angebotenen Typen (E‑1020), nach Kennung sortiert. Kein angebotener Typ:
`200` mit leerer Liste. Grundlage für A2 und A3.

```json
[
  {
    "typ": "WERK_ABLADESTELLE",
    "name": "Anpassen der Werk-/Abladestellentabelle",
    "beschreibung": "Die EDI-Plattform erzeugt …",
    "aktionen": ["Neuanlage", "Anpassung", "Löschung"],
    "maxEintraege": 20,
    "schluessel": { "felder": ["lieferantennummer", "werk", "abladestelle"], "trenner": "/" },
    "felder": [
      { "name": "sndprn", "label": "SNDPRN", "pflicht": false,
        "nurBei": ["Neuanlage", "Anpassung"], "muster": "^[0-9]{6}$",
        "beispiel": "120764", "hinweis": "Bei Anpassung heißt leer: der Wert wird entfernt." }
    ]
  }
]
```

`schluessel` ist `null`, wenn der Typ keinen hat; `nurBei`, `muster`, `beispiel` und `hinweis` sind
`null`, wo die Definition sie nicht setzt. `regeln`, `zeile` und `rueckgaben` gehen nicht hinaus:
Den Parametertext baut das Backend (E‑1006), und die Regeln prüft es selbst — der Assistent erfährt
sie aus der Beschreibung und aus den Fehlercodes (§7).

### 6.2 `POST /api/tasks`

```json
{
  "anfrageId": "8f0c2a4e-1d3b-4c5a-9e7f-0a1b2c3d4e5f",
  "typ": "WERK_ABLADESTELLE",
  "aktion": "Neuanlage",
  "eintraege": [
    { "werk": "72332", "abladestelle": "DDB0", "sndprn": "120764" }
  ]
}
```

| Fall | Antwort |
|---|---|
| angelegt | **`201`** mit dem Auftrag (§6.5) und `Location: /api/tasks/{taskId}`. Ein Ereignis `AUFTRAG_ANGELEGT` im `audit_log` mit Typ, Aktion, Zahl der Einträge und Task-ID — nicht mit dem Parametertext |
| gleiche `anfrageId` im selben Mandanten noch einmal | **`200`** mit dem vorhandenen Auftrag. Kein zweiter Auftrag, kein zweites Ereignis — auch wenn der übrige Rumpf diesmal anders aussieht |
| Assistent für den aktiven Mandanten nicht verfügbar (§3.9) — *seit 1.1.0* | **`404`**, derselbe Rumpf wie unten; kein Auftrag, kein Ereignis |
| `anfrageId` fehlt oder ist keine UUID | `400`, Fehlerliste mit `feld: "anfrageId"` und Code `PFLICHT_FEHLT` bzw. `FORMAT` |
| Typ unbekannt, inaktiv, ungültig **oder** für den aktiven Mandanten nicht freigeschaltet | **`404`**, in allen Fällen gleich: derselbe Rumpf, dasselbe Statement. Sonst ließe sich die Freischaltung anderer Mandanten abfragen |
| Prüffehler (§5.3) | `400` |

Die Reihenfolge ist: **die Freigabe prüfen** (seit 1.1.0, F32; sie liest den Typkatalog des
Mandanten, aus dem danach der Typ kommt — kein zweites Statement), `anfrageId` prüfen, nach einem
vorhandenen Auftrag mit dieser `anfrageId` suchen, Typ auflösen, Einträge prüfen, anlegen. Kommen zwei gleiche Anfragen gleichzeitig, verliert
die zweite am eindeutigen Index `uq_task_anfrage` und antwortet mit `200` und dem Auftrag der
ersten.

**Das Fehlerformat** ist das vorhandene ([`datenzugriff.md`](datenzugriff.md) §8): Problemtyp
`eingabe-ungueltig`, Status `400`, wie bei der Feldvalidierung; die Liste `errors` trägt je Fehler
`index`, `feld`, `code` und `meldung`.

```json
{
  "type": "https://overlord.kraftwerkone.de/probleme/eingabe-ungueltig",
  "title": "Auftrag nicht angenommen",
  "status": 400,
  "detail": "Bitte prüfe die genannten Angaben.",
  "errors": [
    { "index": 0, "feld": "sndprn", "code": "FORMAT",
      "meldung": "Eintrag 1, SNDPRN: Der Wert „12076“ hat nicht die erwartete Form." }
  ],
  "traceId": "…"
}
```

**Die Fehlercodes:** `AKTION_UNBEKANNT`, `ZU_WENIGE_EINTRAEGE`, `ZU_VIELE_EINTRAEGE`,
`FELD_UNBEKANNT`, `FELD_NICHT_ERLAUBT`, `PFLICHT_FEHLT`, `FORMAT`, `UNZULAESSIGES_ZEICHEN`,
`MINDESTENS_EINS`, `DOPPELT`, dazu `ZU_LANG` (§5.2). Sie sind stabil; der Klartext darf sich
ändern.

### 6.3 `GET /api/tasks`

Alle Aufträge des aktiven Mandanten, die noch laufen (`OFFEN`, `IN_AUSFUEHRUNG`), **unabhängig vom
Alter**, dazu alle übrigen, deren `COALESCE(ausgefuehrt_am, eingestellt_am)` in den letzten 7 Tagen
liegt (Datenbankzeit, `UTC_TIMESTAMP(3)`). Neueste zuerst nach `eingestellt_am`, höchstens 50. Kein
Zeitfenster-Parameter, keine Paginierung (E‑1016). Welche Status als laufend gelten, steht nur im
`TaskStatusClassifier`; das Statement übernimmt die Menge von dort.

### 6.4 `GET /api/tasks/{taskId}`

Ein Auftrag. Fremde oder unbekannte Kennung: `404`, ununterscheidbar — **ein** Statement mit
Kennung und Mandant im `WHERE`, keine Existenzprüfung davor
([`mandantentrennung.md`](mandantentrennung.md) §5).

### 6.5 Die Antwort

Liste und Einzelabruf liefern dieselbe Form, `POST` ebenso:

| Feld | Inhalt |
|---|---|
| `taskId` | UUID |
| `typ` | Kennung des Typs |
| `name` | Name aus der Kopie |
| `aktion` | die Aktion |
| `status` | gespeicherter Status, roh |
| `einordnung` | `OFFEN`, `IN_AUSFUEHRUNG`, `ERLEDIGT`, `TEILWEISE_ERLEDIGT`, `UNSTIMMIG`, `FEHLGESCHLAGEN` oder `UNBEKANNT` (§5.5) |
| `eingestelltAm`, `abgeholtAm`, `ausgefuehrtAm` | ISO 8601 mit `Z`, `null` wo nicht gesetzt |
| `fehlercode`, `fehlertext` | von der Plattform, sonst `null` |
| `eintraege` | je Eintrag `werte` (Feldname → Wert, nur Felder mit Wert), `schluessel` (Text nach der Kopie, `null` ohne Schlüssel), `rueckgabe` (Rohwert der zugehörigen Zeile oder `null`) und `ergebnis` (`OK`, `NICHT_OK`, `UNBEKANNT` oder `null`) |
| `felder` | *seit A4 (E‑1120)* je Feld der Kopie `name` und `label`, in der Reihenfolge der Definition — alle Felder, auch die ohne Wert in einem Eintrag |

Rückgabe und Ergebnis eines Eintrags gehören zur Zeile mit demselben Index; gibt es diese Zeile
nicht, sind beide `null`.

**`felder` kommt aus der Kopie** wie `name` (E‑1023): Eine spätere Änderung an einem `label` der
Typzeile ändert nichts an der Anzeige eines vorhandenen Auftrags. Die übrigen Angaben der Struktur
(`pflicht`, `nurBei`, `muster`, `hinweis`) gehen nicht hinaus; die Übersicht braucht sie nicht
(Punkt 1027 nennt, wofür `nurBei` und `hinweis` einmal taugen könnten).

### 6.6 `DELETE /api/tasks/{taskId}` *(A4, 19.10.2026)*

Löscht einen Auftrag, den die Plattform noch nicht abgeholt hat (E‑1110, E‑1113 bis E‑1115).
Kein Rumpf. **Ein Statement entscheidet**, im eigenen Schema über `monitorDsl`:

```sql
DELETE FROM overlord_monitor.task
WHERE task_id = ? AND mandant_id = ? AND status = 'OFFEN'
RETURNING task_typ, aktion, eintraege, eingestellt_am
```

Der Mandant kommt aus der Sitzung (M1, M3). Das `status = 'OFFEN'` ist dieselbe Bedingung, unter der
die Plattform abholt ([`task-schnittstelle.md`](task-schnittstelle.md) §3): Beide Statements
ändern dieselbe Zeile nur aus `OFFEN` heraus, und InnoDB sperrt die Zeile für das erste — **von
beiden gewinnt genau eines**. Das `RETURNING` liefert, was das Protokoll braucht, ohne ein Lesen
davor (F28).

**Löscht es null Zeilen, wird nur im eigenen Mandanten nachgelesen** (E‑1115):

```sql
SELECT status FROM overlord_monitor.task WHERE task_id = ? AND mandant_id = ?
```

| Fall | Antwort |
|---|---|
| gelöscht | **`204`**, kein Rumpf. Ein Ereignis `AUFTRAG_GELOESCHT` im `audit_log`, in derselben Transaktion (F26) |
| null Zeilen, die Nachlesung findet nichts — unbekannte Kennung **oder** Auftrag eines anderen Mandanten | **`404`**, derselbe Rumpf und dieselben zwei Statements wie bei jeder unbekannten Kennung |
| null Zeilen, die Nachlesung findet den Auftrag — er ist nicht mehr `OFFEN` | **`409`**, Problemtyp `auftrag-nicht-offen` (F27), kein Ereignis |
| ohne aktiven Mandanten, Änderungszwang, ohne CSRF-Token | wie bei den übrigen Endpunkten (`403`) |

```json
{
  "type": "https://overlord.kraftwerkone.de/probleme/auftrag-nicht-offen",
  "title": "Auftrag nicht mehr offen",
  "status": 409,
  "detail": "Die Plattform hat den Auftrag bereits übernommen. Er kann nicht mehr gelöscht werden.",
  "instance": "/api/tasks/…",
  "traceId": "…"
}
```

**Das Ereignis** `AUFTRAG_GELOESCHT` trägt, was E‑1114 nennt: Benutzer (`actor_user_id`,
`actor_username`), Mandant, `target_type = task`, `target_id` = Task-ID und im Detail Typ, Aktion,
Zahl der Einträge und Zeitpunkt des Einstellens — etwa `Typ WERK_ABLADESTELLE, Aktion Neuanlage, 2
Einträge, eingestellt 2026-10-19T12:32:10.123Z`. **Kein Parametertext, keine Werte**, wie beim
Anlegen.

**Gelöscht heißt gelöscht** (E‑1114): Die Zeile verschwindet aus `task`, es bleibt nur das
Ereignis. Ein gelöschter Auftrag ist nie ausgeführt worden; die Plattform hat ihn nicht gesehen oder
nicht abgeholt.

---

## 7. Hinweise für A2

> **Erledigt mit A2 (16.10.2026).** Umgesetzt in [`assistent.md`](assistent.md): Werkzeuge aus der
> Struktur ohne Mandantenparameter (E‑1034), Rückfragen aus den Fehlercodes (E‑1033), Anlegen nur
> per Klick, Datenschutz als Punkt 1009 (E‑1029, E‑1030). Typdefinition, Typkatalog und Prüfung
> liegen seitdem in `common.auftrag` (E‑1037); neu ist `POST /api/tasks/pruefung` (E‑1042,
> [`assistent.md`](assistent.md) §6.5).

Damit sie nicht verloren gehen:

- **Die Werkzeuge des Modells entstehen aus der Struktur des Typs** (Aktionen, Felder, Beispiele),
  die Beschreibung geht in die Anweisungen des Modells. **Kein Werkzeug hat einen
  Mandantenparameter.**
- **Die Fehlercodes aus §6.2 sind die Grundlage der Rückfragen**; das Modell beurteilt Werte nicht
  selbst.
- **Bei `WERK_ABLADESTELLE` nennt die Karte jedes leere SNDPRN-Feld einer Anpassung ausdrücklich
  als „wird entfernt“** (E‑1007), weil der Kunde den bisherigen Wert nicht sehen kann. Das gehört
  als Hinweis in die Beschreibung des Typs, nicht in den Code — es steht dort bereits.
- **Den Auftrag legt der Klick auf der Karte über `POST /api/tasks` an, nie das Modell.** Die
  `anfrageId` entsteht je Absenden der Karte, nicht je Gespräch.
- Vor A2 ist Datenschutz und Auftragsverarbeitung mit dem Modellanbieter zu klären (Punkt 1008).

---

## 8. Folgeschritte

| Teil | Inhalt |
|---|---|
| **A2** | Assistent im Backend: Anbindung an das Sprachmodell, Werkzeuge aus der Typdefinition — erledigt, [`assistent.md`](assistent.md) |
| **A3** | Chatbox in der Kopfleiste und Bestätigungskarte — erledigt, [`assistent-frontend.md`](assistent-frontend.md) |
| **A4** | Bereich „Ihre Aufträge“ in der Übersicht, über `GET /api/tasks`, und Löschen offener Aufträge (§6.6) — erledigt, [`assistent-frontend.md`](assistent-frontend.md) §12 |

---

## 9. Messungen und Tests

### 9.1 Tests ohne Datenbank

| Test | Was er hält |
|---|---|
| `DefinitionsPruefungTest` | `WERK_ABLADESTELLE` und die Gattung sind gültig; je Prüfregel beim Laden (§3.6) mindestens ein ungültiger Fall — unbekannter Schlüssel auf jeder Ebene, fehlender Pflichtschlüssel, nicht übersetzbares Muster, Platzhalter ohne Feld, Aktion ohne Zeilenvorlage, optionaler Schlüsselteil nicht vorn, `maxEintraege` außerhalb der Grenzen, `rueckgaben.ok` fehlt oder steht in `weitere`; alle Gründe kommen zurück |
| `AuftragsPruefungTest` | jeder Fehlercode aus §5.3 mindestens einmal; 35 und 36 Zeichen, fünf und sieben Ziffern, leere Liste, 20 und 21 Einträge (bis `V101`: 50 und 51), Schlüssel doppelt, Löschung mit SNDPRN, Anpassung mit beiden Werten leer, Tabulator (`UNZULAESSIGES_ZEICHEN`) im Schlüsselteil; seit 23.10.2026 die Leerzeichen im Schlüssel (§9.7); der Zeichenschutz an einem Feld ohne Muster (Trenner, Komma bzw. Zeichen jeder Vorlage, LF, CR, Steuerzeichen, `U+0085`, `U+2028`); die Gattung |
| `ParametertextTest` | die drei Texte aus §5.2 zeichengenau, Zeilenende, die Gattung, Ersetzen in einem Durchgang |
| `RueckgabeTest` | CRLF, abschließendes LF, Leerraum, unbekannter Wert, leere Zeile, fehlende Rückgabe |
| `TaskStatusClassifierTest` | jede Zeile der Tabelle in §5.5, dazu falsche Zeilenzahl |
| `TypUnabhaengigkeitTest` | `WERK_ABLADESTELLE` steht im Hauptcode (`src/main`, auch generiert und Ressourcen) nirgends außer in den Migrationen `V100` bis `V103` — mit Gegenprobe, dass die Suche ihn in genau diesen vieren findet (E‑1018; `V103` seit 1.1.0) |
| `AssistentFreigabeTest` *(1.1.0)* | die eine Stelle (§3.9): ausgeschaltet nichts angeboten und `404` ohne Datenbankzugriff; ohne freigeschalteten Typ `404`; nur ungeeignete Typen `404` auch für das Anlegen (F31); verfügbar bietet der Assistent nur das Anbietbare an, das Anlegen wählt aus dem ganzen Katalog |
| `PlattformvertragTest` | die vier Statements in [`task-schnittstelle.md`](task-schnittstelle.md) §3 sind zeichengleich mit denen, die `AuftraegeDbIT` ausführt |
| `PaketstrukturTest` | das Paket `task` ist als Fachpaket geführt und kennt kein anderes Fachpaket |

Die **Gattung** ist eine zweite Definition, die nur im Test besteht (`Testdefinitionen`): andere
Aktionen, Felder, Trenner (`-`), Vorlagen mit `;` und `|`, eine Regel `eindeutig` über ein Feld
statt über den Schlüssel, andere Rückgabewerte (`JA`, `NEIN`, `SPAETER`). Prüfung und
Parametertext arbeiten für sie richtig, ohne dass im Hauptcode etwas für sie steht.

### 9.2 Tests mit Datenbank (`@Tag("db")`)

| Test | Was er hält |
|---|---|
| `AuftraegeDbIT` | Anlegen ergibt eine Zeile `OFFEN` mit `eingestellt_am`, Kopie der Definition, geordneten Einträgen, Parametertext, `angelegt_von` und genau einem Ereignis ohne Parametertext; Wiederholung mit derselben `anfrageId` (auch groß geschrieben, mit anderem Rumpf) gibt `200` und keinen zweiten Auftrag; Prüffehler im Fehlerformat; **simulierte Plattform** mit den vier Statements aus dem Vertrag: Erledigt, Teilweise erledigt (CRLF), Unstimmig, Fehlgeschlagen, doppeltes Abholen, Zurückmelden ohne Abholen; Zeitstempel nur auf vorhanden und Reihenfolge (T1); seit 23.10.2026 Leerzeichen im Schlüssel über Prüfung und Anlegen bis in Zeile und Parametertext, U+00A0 `400` (§9.7) |
| `AuftraegeIsolationDbIT` | die vier Endpunkte: `NEXANS` gegen `IBIS`, `SUTTONS` ohne Freischaltung (`404` beim Anlegen, ununterscheidbar vom erfundenen Typ in Rumpf **und** Statements; leere Typliste), fremder Auftrag weder in der Liste noch einzeln (`404` gleich einer erfundenen Kennung, ein Statement), untergeschobenes `mandantId`, fremde `anfrageId`, ADMIN je aktivem Mandanten, Änderungszwang |
| `TypdefinitionDbIT` | ein eigener Testtyp per SQL, ohne Neustart: inaktiv `404`, aktiviert in der Typliste, Änderung an `maxEintraege` wirkt beim nächsten Anlegen, ungültige Struktur nimmt ihn aus dem Angebot, ein Auftrag von vorher liest seine Kopie; `WERK_ABLADESTELLE` bleibt unberührt |
| `TaskMigrationDbIT` | `JSON_VALID(struktur)` ist wahr; die Muster, in der Datenbank per `JSON_EXTRACT` herausgeholt, sind zeichengleich mit dem Auftrag; Struktur, Name und Beschreibung wie geschrieben, im Stand nach `V102` **und dem Skript vom 23.10.2026** — mit genau einem Backslash in `\p{Z}` (§3.5, §9.7) |
| `IndexbestandDbIT` | die drei Tabellen und ihre Indizes in der Sollliste, in beide Richtungen — seit 1.1.0 auch auf `main`, ohne Ausnahme |
| `AssistentOhneSchluesselDbIT` *(1.1.0)* | Start mit Schalter an und leerem Schlüssel: Der Kontext startet, `Sprachmodell` ist der abgeschaltete Ersatz, `verfuegbar: false` trotz Freischaltung; Eröffnen, Nachricht, Prüfen und Anlegen `404`, kein Auftrag; Liste und Typliste antworten |

> **Seit 1.1.0 (21.10.2026)** schalten `AuftragsTestbasis` und `AssistentIsolationDbIT` den Typ
> `WERK_ABLADESTELLE` für `NEXANS` und `IBIS` selbst frei (`Testfreischaltungen`, F35) und
> entfernen genau diese Zeilen danach; vom Bestand hängt nur noch ab, dass `SUTTONS` keine
> Freischaltung hat. Die Auftragstests laufen mit eingeschaltetem Assistenten — Platzhalter als
> Schlüssel, ein geschlossener Port als Ziel —, weil Anlegen und Prüfen sonst `404` antworten.
> `TaskMigrationDbIT` prüft die Freischaltungen nicht mehr: Sie sind seit `V103` Pflegedaten. Die
> Sperre selbst prüft `AssistentIsolationDbIT` ([`assistent.md`](assistent.md) §9.2).

**Verletzungsprobe je Mandantenfilter (16.10.2026).** Jeder Filter in `AuftragRepository` ist
einzeln entfernt worden, danach lief `AuftraegeIsolationDbIT`; die Datei ist jedes Mal byte-gleich
zurückgespielt.

| Entfernt | Rot |
|---|---|
| `mandant_id` in `angeboteneTypen` | `typliste`, `anlegen_ohne_freischaltung`, `admin_sieht_nur_den_aktiven_mandanten` |
| `mandant_id` in `finde` | `fremder_auftrag_unerreichbar`, `admin_sieht_nur_den_aktiven_mandanten` |
| `mandant_id` in `findeNachAnfrage` | `fremde_anfrage_id` |
| `mandant_id` in `findeAktuelle` | `fremder_auftrag_unerreichbar`, `mandant_id_im_rumpf_wird_ignoriert`, `admin_sieht_nur_den_aktiven_mandanten` |
| `legeAn` schreibt fest `IBIS` statt des Session-Mandanten | `mandant_id_im_rumpf_wird_ignoriert`, `fremde_anfrage_id`, `fremder_auftrag_unerreichbar`, `admin_sieht_nur_den_aktiven_mandanten` |

Jeder Filter hat also mindestens einen Test, der sein Fehlen bemerkt. `aenderungszwang_sperrt`
bleibt bei allen Eingriffen grün: Er prüft keine Mandantentrennung, sondern dass der
Änderungszwang beim Passwort auch diese Endpunkte sperrt. Eine erste Probe, die nur die Filter in
`angeboteneTypen` und `finde` gemeinsam entfernte, ließ deshalb drei Tests grün: Die beiden anderen
prüfen Filter, die sie nicht angefasst hatte.

Aufgeräumt wird, was das Präfix `it-` trägt: Aufträge über `angelegt_von`, Testtypen über
`task_typ` (Regel T2), seit 1.1.0 dazu genau die Freischaltungen, die der Test angelegt hat. Die
Zeile `WERK_ABLADESTELLE` in `task_type` fasst kein Test an.

### 9.3 Messung M1000 (Regel 7)

*Gemessen am 16.10.2026 gegen die Testkopie (MariaDB 10.6.22), eine Sitzung mit dem Workbench-Client,
nach Regel 7 mit `EXPLAIN` und Laufzeit (E‑1026): je Statement fünf Läufe, Dauer aus `SHOW PROFILES`
und Wanduhr über `SYSDATE(6)` vor und nach den fünf Läufen. Kein Lastgerüst.* Die Statements sind
die von jOOQ gerenderten (über eine Wegwerfklasse gegen eine `MockConnection` mit eingesetzten
Werten erhoben), die der Plattform wörtlich aus [`task-schnittstelle.md`](task-schnittstelle.md).

**Bestand bei der Messung:** `task_type` 1 Zeile, `task_type_mandant` 2, `task` 0 (die Tests räumen
ihre Aufträge weg). Die Pläne zeigen deshalb, **welchen Index der Optimizer nimmt**, nicht, wie er
unter Last läuft; Laufzeiten unter einer Millisekunde sind hier die Kosten des Rundlaufs.

| # | Statement | `EXPLAIN` | Profil (min–max) | Wanduhr je Lauf |
|---|---|---|---|---|
| L1 | Typen samt Freischaltung, `NEXANS` (`AuftragRepository.angeboteneTypen`) | `task_type`: `type=index`, `key=PRIMARY`, `rows=1`, `Using where` · `task_type_mandant`: `type=eq_ref`, `key=PRIMARY`, `ref=task_type.task_typ,const`, `Using index` | 0,552–0,668 ms | 1,233 ms |
| L1b | dasselbe, `SUTTONS` (keine Freischaltung) | wie L1 | 0,495–0,522 ms | 0,937 ms |
| L2 | Liste, `NEXANS` (`findeAktuelle`, `ORDER BY eingestellt_am DESC, task_id DESC`, 50) | `type=ref`, `key=task_mandant_idx`, `ref=const`, `rows=1`, `Using where` — kein `filesort` | 0,545–0,598 ms | 0,977 ms |
| L3 | Einzelabruf (`finde`, Kennung und Mandant) | `Impossible WHERE noticed after reading const tables` — Zugriff über `PRIMARY` als Konstante, die Kennung gibt es nicht | 0,406–0,498 ms | 0,802 ms |
| L4 | Wiederholung (`findeNachAnfrage`, Mandant und `anfrage_id`) | wie L3, über `uq_task_anfrage` | 0,419–0,445 ms | 0,787 ms |
| P1 | Plattform: offene lesen | `type=ref`, `key=task_status_idx`, `ref=const`, `rows=1`, `Using index condition; Using where` — kein `filesort` | 0,439–0,481 ms | 0,878 ms |
| P2 | Plattform: abholen (bedingtes `UPDATE`) | `type=range`, `key=PRIMARY`, `rows=1`, `Using where` | — (nur `EXPLAIN`) | — |
| P3 | Plattform: zurückmelden (`ERLEDIGT`; `FEHLGESCHLAGEN` hat dieselbe Bedingung) | wie P2 | — (nur `EXPLAIN`) | — |

**Was das trägt.** L2 und P1 lesen über den Index, der für sie angelegt ist, und sortieren ohne
`filesort`: `task_mandant_idx (mandant_id, eingestellt_am)` liefert die Liste rückwärts in
Reihenfolge, `task_status_idx (status, eingestellt_am)` samt dem angehängten Primärschlüssel die
offenen Aufträge in der Reihenfolge des Vertrags. Die beiden bedingten `UPDATE`s treffen genau eine
Zeile über `PRIMARY`; das `status` in der Bedingung wird an der Zeile geprüft. L1 beginnt bei
`task_type` und schlägt die Freischaltung über ihren vollen Primärschlüssel nach — bei einer
Handvoll Typen ist das der billigste Weg; mit Hunderten von Typen läse L1 jeden einmal.

**Was es nicht trägt.** Wie sich L2 verhält, wenn ein Mandant Tausende Aufträge hat, ist auf der
leeren Tabelle nicht zu sehen; `task` wird nie gelöscht. P2 und P3 sind nicht ausgeführt, weil die
Plattform sie ausführt und die Testkopie geteilt ist; `AuftraegeDbIT` führt sie aus, misst aber
nicht (T1).

### 9.4 Gegenprobe gegen `main` (16.10.2026)

Nachdem `V100` auf der Testkopie angewandt war: `main` (`1194079`) in einem eigenen Worktree im
Profil `dev` gebaut, mit Codegen und mit allen Tests unter `@Tag("db")` (`./mvnw verify`).
Flyway validiert 14 Migrationen und meldet „Schema is up to date“ — `V100` liegt für `main` in der
Zukunft und wird ignoriert. Der Codegen schließt `task`, `task_type` und `task_type_mandant` aus;
`src/main/generated-java` bleibt unverändert, der Arbeitsbaum danach sauber. **1.040 Tests ohne und
456 mit Datenbank, alle grün.** Auf `main` wurde nichts geändert; der Worktree ist entfernt.

### 9.5 Löschen (A4, 19.10.2026)

**Tests mit Datenbank** (`@Tag("db")`, gelaufen am 19.10.2026 gegen die Testkopie, alle grün):

| Test | Was er hält |
|---|---|
| `AuftraegeDbIT` (seit A4 19 Fälle, 10 neu) | `OFFEN` des eigenen Mandanten: `204`, Zeile weg, **genau ein** Ereignis `AUFTRAG_GELOESCHT` mit Benutzer, Mandant, `target_type = task` und dem Detail „Typ …, Aktion …, n Einträge, eingestellt <UTC>“ — ohne Parametertext und ohne Werte; ein **Kollege** desselben Mandanten löscht ebenso (E‑1113); `IN_AUSFUEHRUNG`, `ERLEDIGT`, `FEHLGESCHLAGEN`: `409 auftrag-nicht-offen`, Zeile Feld für Feld unverändert, kein Ereignis; **abgeholt zwischen Anzeige und Klick** (Liste zeigt `OFFEN`, dann `ABHOLEN` aus dem Vertrag, dann `DELETE`): `409`; ein zweites `DELETE`: `404`, und es bleibt bei einem Ereignis; unbekannte Kennung: `404`; **Löschen und Ereignis fallen gemeinsam** — `AuftragService.loesche` in einer äußeren Transaktion, die zurücksetzt: danach steht die Zeile wieder und es gibt kein Ereignis (F26); `felder` mit `name` und `label` in Einzelabruf und Liste, gegen die Kopie in der eigenen Zeile geprüft (E‑1120, Regel T2) |
| `AuftraegeIsolationDbIT` (seit A4 8 Fälle, 1 neu, 2 erweitert) | **`IBIS` löscht einen offenen Auftrag von `NEXANS`:** `404`, derselbe Rumpf wie bei einer erfundenen Kennung und **dieselben zwei Statements** auf dem Schreib-Kontext; ein untergeschobenes `?mandantId=NEXANS` ändert nichts, `SUTTONS` ebenso `404`; die Zeile von `NEXANS` steht danach Feld für Feld unverändert, kein Ereignis; Gegenprobe: `NEXANS` löscht sie mit `204`. ADMIN: ohne aktiven Mandanten `403`, unter `IBIS` `404`, unter `NEXANS` `204`. **Änderungszwang:** `403`, an einem echten offenen Auftrag desselben Mandanten, die Zeile bleibt |
| `TypdefinitionDbIT` (erweitert) | Ein `label` der Typzeile ändert sich nach dem Anlegen; der Auftrag von vorher zeigt weiter das `label` seiner Kopie (E‑1120, E‑1023) |
| `AuditLogWriterDbIT` (3 Fälle, 2 neu) | `schreibeInTransaktion` fällt mit dem Rollback der Fachtransaktion — innerhalb steht der Eintrag, danach nicht —, und ohne laufende Transaktion lehnt sie ab (`IllegalTransactionStateException`), ohne zu schreiben (F26) |

`./mvnw verify -DexcludedGroups=db`: **1.245 Fälle grün**, dazu Spotless, ArchUnit und
`PlattformvertragTest` (der Vertrag liest weiterhin nur den ersten `sql`-Block, die vier Statements;
§3a steht dahinter).

**Verletzungsproben** (19.10.2026), je ein Eingriff in den Hauptcode, Lauf über `AuftraegeDbIT` und
`AuftraegeIsolationDbIT`, Rücknahme per `git checkout` gegen den committeten Stand; danach
`target/classes` neu gebaut:

| Eingriff | rot |
|---|---|
| **A** Mandant im `DELETE` entfernt | `fremden_auftrag_loeschen`, `admin_sieht_nur_den_aktiven_mandanten` |
| **B** Mandant in der Nachlesung entfernt | dieselben zwei — ohne ihn wäre der fremde Auftrag `409` statt `404` |
| **C** `status = 'OFFEN'` im `DELETE` entfernt | `loeschen_nicht_offen` dreimal, `abgeholt_zwischen_anzeige_und_klick` |
| **D** Protokoll mit `schreibe` (eigene Transaktion) statt `schreibeInTransaktion` | **im ersten Lauf keiner** — über HTTP committet das Löschen immer. Deshalb ist `loeschen_und_ereignis_fallen_gemeinsam` dazugekommen; im zweiten Lauf rot, und nur er |

Jeder Mandantenfilter des Löschens hat damit einen Test, der sein Fehlen bemerkt.

#### Messung M1005 (Regel 7)

*Gemessen am 19.10.2026 gegen die Testkopie (MariaDB 10.6.22), eine Sitzung mit dem
Workbench-Client als `monitor_write`, nach dem Verfahren von M1000 (E‑1026): `EXPLAIN` je Statement,
je Fall fünf Läufe, Dauer aus `SHOW PROFILES`, Wanduhr über `SYSDATE(6)` vor und nach den fünf
Läufen.* Die Statements sind die von jOOQ gerenderten (Dialekt `MARIADB`, über eine Wegwerfklasse
erhoben, nicht eingecheckt), mit eingesetzten Werten.

**Bestand und Vorgehen:** `task` hatte vor der Messung **eine** Zeile. Die Sitzung legte sieben
eigene Testzeilen an (`angelegt_von = 'it-m1005'`, erfundene Kennungen, reines `INSERT`): fünf
offene unter `NEXANS`, eine offene unter `IBIS` (fremd), eine `IN_AUSFUEHRUNG` unter `NEXANS`. Die
Läufe stehen in **einer Transaktion, die zurückgerollt wird** — der Commit der Testkopie dauert
Sekunden und ist eine Eigenschaft der Umgebung, nicht des Statements. Danach hat die Sitzung ihre
sieben Zeilen gelöscht; `task` hatte wieder eine Zeile.

| # | Statement | `EXPLAIN` | Profil (min–max) | Wanduhr je Lauf |
|---|---|---|---|---|
| D1 | `DELETE … RETURNING`, offener eigener Auftrag (fünf verschiedene, je eine Zeile gelöscht) | `type=range`, `key=PRIMARY`, `key_len=144`, `rows=1`, `Using where` | 0,387–0,594 ms | 0,820 ms |
| D2 | dasselbe, Auftrag von `IBIS` als `NEXANS` — null Zeilen | wie D1 | 0,352–0,404 ms | 0,779 ms |
| D3 | dasselbe, erfundene Kennung — null Zeilen | wie D1 | 0,375–0,406 ms | 0,755 ms |
| D4 | dasselbe, eigener Auftrag in `IN_AUSFUEHRUNG` — null Zeilen | wie D1 | 0,354–0,428 ms | 0,764 ms |
| L1 | Nachlesung `SELECT status`, eigener abgeholter Auftrag (→ `409`) | `type=const`, `key=PRIMARY`, `ref=const`, `rows=1` | 0,360–0,461 ms | 0,797 ms |
| L2 | Nachlesung, Auftrag von `IBIS` (→ `404`) | `Impossible WHERE noticed after reading const tables` | 0,343–0,408 ms | 0,753 ms |
| L3 | Nachlesung, erfundene Kennung (→ `404`) | wie L2 | 0,325–0,383 ms | 0,720 ms |

`EXPLAIN` lief auf dem `DELETE` ohne die `RETURNING`-Klausel; der Zugriffsweg ist derselbe, und
`RETURNING` liefert die Spalten der Zeile, die der Zugriff ohnehin trifft.

**Was das trägt.** Das `DELETE` trifft seine Zeile über den Primärschlüssel und prüft Mandant und
Status an ihr — wie das Abholen der Plattform (P2 in M1000). Ein fremder, ein unbekannter und ein
abgeholter Auftrag kosten dasselbe wie ein Treffer, und die Nachlesung findet den fremden Auftrag
schon beim Lesen der Konstanten nicht: Die beiden `404`-Fälle sind im Plan nicht zu unterscheiden.
Alle Läufe liegen unter 0,6 ms; das sind auf dieser Tabelle die Kosten des Rundlaufs.

**Was es nicht trägt.** Wie lange ein Löschen samt Commit dauert, sagt die Messung nicht: Auf der
Testkopie dauert ein Commit Sekunden (eine Eigenschaft ihres Schreibpfads), und in der Produktion
ist er nicht gemessen. Die Tabelle hatte eine Zeile; über den Primärschlüssel ändert eine größere
nichts am Weg.

### 9.6 Freischaltung und Zusammenführung (1.1.0, 21.10.2026)

**Keine neue Abfrage, keine Messung.** Die Freigabe liest das Statement des Typkatalogs aus M1000
(§9.3), einmal je Anfrage: Anlegen und Prüfen wählen den Typ aus dem Katalog, den die Freigabe
gelesen hat, statt ihn ein zweites Mal zu lesen; Gespräche lesen das Angebot wie bisher einmal. Am
Weg der Nachrichtenliste und ihrer Suche ändert sich nichts — Regel 7 verlangt deshalb keine
Prüfung in der Produktion.

**Testkopie.** `V103` ist beim ersten Kontextstart des DB-Laufs um 12:24 angewandt worden. Danach
war `task_type_mandant` leer, und die Tests haben keine Zeile hinterlassen. Das Skript für `SYSTEM`
(§3.9) mit dem Schreibbenutzer:

| Lauf | Ergebnis |
|---|---|
| freischalten | vorher 0 Freischaltungen, nachher 1 (`SYSTEM`), Typ aktiv |
| freischalten, zweiter Lauf | vorher und nachher 1 — idempotent |
| Wache 1: Vorgabeschema `information_schema` | Abbruch mit `1231`, `BITTE_DAS_EIGENE_SCHEMA_ALS_VORGABE_WAEHLEN`, nichts geändert |
| Wache 3: Probezeile für die erfundene Kennung `it-probe` | Abbruch mit `1231`, `ANDERE_MANDANTEN_FREIGESCHALTET_SKRIPT_PASST_NICHT`; Probezeile danach entfernt |
| zurücknehmen, zweimal | vorher 1, nachher 0; der zweite Lauf ändert nichts |
| freischalten | Zielzustand: ausschließlich `SYSTEM` |

Wache 2 (`V103` noch nicht angewandt) ließ sich nicht nachstellen, weil `V103` schon angewandt war.
Der offene Auftrag unter `NEXANS` vom 19.10.2026 (Sichtprüfung) steht weiter in `task`; „Ihre
Aufträge“ zeigt ihn unter `NEXANS`, anlegen geht dort nicht mehr.

**Codegen und Indexbestand.** Der Codegen ohne Ausschluss der Tabellen der Aufträge hat 16 Tabellen
gelesen und 13 übernommen (ausgenommen `flyway_schema_history` und die beiden der Sitzung); am
eingecheckten Modell änderte sich nichts. `scripts\indizes-kandidat.ps1` vor und nach `V103`: kein
Unterschied zur Sollliste (95 Sätze).

**Voller Lauf** `.\mvnw.cmd clean verify` aus PowerShell, ohne `SPRING_PROFILES_ACTIVE`, 12:23 bis
12:50: **1.256 Tests ohne Datenbank grün**, darunter `PaketstrukturTest` (25), `FlywayOutOfOrderTest`
(5), `CookieAttributeTest` (4) und `AssistentFreigabeTest` (4). **503 Tests mit Datenbank, einer
rot:** `NachrichtendetailDbIT.kein_schritt_steht_leer` mit `500` — im Protokoll ein Deadlock
(MariaDB `1213`) um 12:45:00,1, zur vollen Minute, an der Spring Session abgelaufene Sitzungen
aufräumt (Vorgabe `0 * * * * *`, nicht umgestellt); auf derselben Testkopie lief ein zweites
Backend. Belegt ist der Zeitpunkt, nicht der Gegenspieler. Der Test hat mit dieser Änderung nichts
zu tun und lief einzeln nachgefahren grün (12 von 12).

**Abschlusslauf auf `72a515e`** (Code wie oben, dazu Doku und Version), `.\mvnw.cmd clean verify`,
13:07 bis 13:36: 1.256 ohne Datenbank grün; 503 mit Datenbank, **zwei rot, beide außerhalb dieser
Änderung**: `NachrichtendetailDbIT.rollen_stehen_im_kopf` mit `500`, diesmal mit Gegenspieler im
Protokoll — um 13:30:00 verliert das `UPDATE SPRING_SESSION` der Anfrage einen Deadlock gegen das
`DELETE FROM SPRING_SESSION` der Aufräumung (Punkt 1037, [`authentifizierung.md`](authentifizierung.md)
§12); und `DashboardIsolationDbIT.keine_mandanten_id`, Punkt 182: Der JSON-Diff beider Rümpfe zeigt
nur `plattform.dienste[*].alterSekunden`, sechs Dienste um je eine Sekunde. Beide Klassen einzeln
nachgefahren grün (28 von 28, 12 von 12). Die Abnahmeklassen waren in beiden Läufen grün. Grün darunter: `IndexbestandDbIT` (5, in beiden
Richtungen und mit den Tabellen der Aufträge), `AssistentIsolationDbIT` (14),
`AssistentOhneSchluesselDbIT` (1), `AuftraegeIsolationDbIT` (8), `AuftraegeDbIT` (19),
`TypdefinitionDbIT`, `TaskMigrationDbIT` und `TestsatzDefinitionDbIT`. Kein Test ruft die
Anthropic-API: Wo der Assistent eingeschaltet ist, ist der Schlüssel ein Platzhalter und das Ziel
ein geschlossener Port, und wo Nachrichten fließen, spielt ein geskriptetes Modell.

**Verletzungsprobe der Sperre (21.10.2026).** Je Probe ist die Sperre an genau einer Stelle
entfernt worden, danach liefen `AssistentOhneSchluesselDbIT` und `AssistentIsolationDbIT` bzw.
`AssistentServiceTest`; die Datei ist jedes Mal per `git checkout` auf den committeten Stand
(`1d33654`) zurückgespielt worden, der Baum war vorher sauber.

| # | Entfernt | Rot |
|---|---|---|
| P1 | `AuftragService.lege` liest den Katalog direkt statt über `verlangeKatalog` | `AssistentOhneSchluesselDbIT` (Anlegen ohne Schlüssel `201` statt `404`) |
| P2 | dasselbe in `AuftragService.pruefe` | `AssistentOhneSchluesselDbIT` (Prüfen ohne Schlüssel `200`) |
| P3 | `AssistentService.sende` nimmt `angebot` statt `verlangeAngebot` | `AssistentServiceTest` (Eingeschaltet) `.freischaltung_entzogen` |
| P4 | dasselbe in `AssistentService.eroeffne` | `AssistentIsolationDbIT.ohne_freischaltung_gesperrt`, `AssistentOhneSchluesselDbIT`; in `AssistentServiceTest` `.ohne_typ`, (Schalter aus) `.ohne_schluessel` und `.schalter_aus` |

`AssistentIsolationDbIT` bleibt bei P1 und P2 grün, und das ist kein Loch: Für `SUTTONS` weist
schon die Typauswahl ab, weil kein Typ freigeschaltet ist. Was nur die Freigabe abweist — ein
freigeschalteter Mandant ohne Schlüssel —, prüft `AssistentOhneSchluesselDbIT`.

### 9.7 Leerzeichen im Schlüssel (23.10.2026)

**Bestand vor der Änderung** (Stand `main` = `9352d1a`). Hier wurde der Schlüssel geprüft oder
beschrieben:

| Stelle | Was dort stand |
|---|---|
| `overlord_monitor.task_type`, Zeile `WERK_ABLADESTELLE`, Spalte `struktur`, `$.felder[0..2].muster` | `^[^ /,]{1,35}$` an Lieferantennummer, Werk und Abladestelle (aus `V100`; `V101` und `V102` ändern es nicht). Lokal auf der Testkopie vorhanden, freigeschaltet nur für `SYSTEM` |
| `AuftragsPruefung` | Muster (`FORMAT`) und Zeichenschutz (Cc, U+2028/9, Trenner); kein Kürzen, „kein Wert wird verändert“ |
| `AuftragService.lege`, `.pruefe`; `Gespraechsfuehrung.beantworte`; `Vorschlag.aus` | prüften und nutzten die Werte unverändert |
| `Werkzeugbau` | „Werte übergibst du als Text, genau so, wie der Kunde sie nennt“; das Muster in der Feldbeschreibung |
| `assistent/anweisung.txt` | „ändere, ergänze oder kürze ihn nicht“ — unverändert geblieben |
| `vorschlag-karte.tsx` | Werte mit `whitespace-pre-wrap`, die Überschrift mit dem Schlüssel ohne |
| `auftraege-block.tsx` | Schlüssel und Werte ohne Regel für Leerraum |
| E‑1012 (§2, §4.2), [`task-schnittstelle.md`](task-schnittstelle.md) §8 | „kein Leerraum“ |

**Widerspruch, gemeldet und entschieden (E‑1142):** E‑1012 und der Plattformvertrag verlangten
„kein Leerraum“, das Muster sperrte nur U+0020. U+00A0, U+202F, U+3000 und die übrigen
Unicode-Leerzeichen gingen still durch; der Zeichenschutz (§3.4) erfasst nur Cc und die beiden
Unicode-Trenner. **Der Ablauf** brauchte keinen Halt: Die Karte entsteht erst nach
`AuftragsPruefung` (§5.3, [`assistent.md`](assistent.md) §1).

**Skript auf der Testkopie** (E‑1145), MariaDB-Client 12.3 über die Standardeingabe wie in der
Deployment-Routine ([`betrieb.md`](betrieb.md) §7a E). Zuerst die Zeile gesichert (Scratchpad, nicht
eingecheckt). Mit falschem Vorgabeschema Abbruch an Wache 1 (Fehler `1231`). Dann fünf Läufe:
Rücknahme auf dem alten Stand (nichts geändert, `geaendert_am` 19.10.2026 unverändert), Skript
(`neu` 3), Skript noch einmal (nichts geändert, `geaendert_am` gleich), Rücknahme (`alt` 3), Skript
(`neu` 3). Danach unterschied sich die Struktur vom gesicherten Stand **nur in den drei Mustern**
(Textvergleich nach Ersetzen); `task/werk-abladestelle-struktur.json` ist zeichengleich aus der
Datenbank übernommen.

**Tests ohne Datenbank:** `./mvnw verify -Djooq.codegen.skip=true -DexcludedGroups=db` grün, **1.275
Tests**. Neu oder geändert:

| Test | Was er hält |
|---|---|
| `AuftragsPruefungTest`, Gruppe „Leerzeichen im Schlüssel“ (24 Fälle) | `A 1` angenommen und unverändert; `  A 1 ` wird `A 1`; `A  1` bleibt; nur Leerzeichen `PFLICHT_FEHLT` wie leer; eine Lieferantennummer nur aus Leerzeichen fehlt, der Schlüssel ist zweiteilig; je Teil gekürzt, auch am Trenner; `\tA1`, `A\n1`, ` \tA1 `, `A1\t` `UNZULAESSIGES_ZEICHEN`; U+00A0, U+2007, U+202F, U+3000 innen und am Rand `FORMAT`; 35 Zeichen mit Leerzeichen am Rand gehen, 36 innen nicht; Neuanlage, Anpassung und Löschung kürzen gleich; erst nach dem Kürzen gleich ist `DOPPELT` wie heute (E‑1014); `A 1` und `A  1` nicht doppelt; SNDPRN wird nicht gekürzt; die Gattung kürzt ihre Schlüsselteile und nur diese; ein `null`-Eintrag kommt leer zurück. Zwei alte Fälle („Leerzeichen `FORMAT`“, „kein Kürzen“) sind entfallen, einer nimmt jetzt U+00A0 statt des Leerzeichens |
| `GespraechsfuehrungTest` | die Karte trägt `A  1`/`DDB0` aus `"  A  1 "` und `" DDB0"` |
| `WerkzeugbauTest` | der Satz zu den Schlüsselteilen bei jedem Typ mit Schlüssel, bei keinem ohne; das Schema mit dem neuen Muster |
| `TestsatzTest` | 21 Fälle in 15 Gruppen; jede erwartete Karte schon in gekürzter Form |

**Tests mit Datenbank** gegen die Testkopie, 47 in sieben Klassen grün: `AuftraegeDbIT` (21, davon
zwei neu: Prüfung frei, gespeichert und im Parametertext gekürzt, innen zeichengenau —
`Aktion=Löschung\n0101 159903/A  1/DDB0\n72332/B 2`; U+00A0 ist `400 FORMAT` ohne Auftrag),
`AuftraegeIsolationDbIT` (8), `AssistentIsolationDbIT` (14), `AssistentOhneSchluesselDbIT`,
`TypdefinitionDbIT`, `TaskMigrationDbIT` (das neue Muster mit genau einem Backslash) und
`TestsatzDefinitionDbIT`. Kein neuer Endpunkt, kein neuer Isolationstest, keine neue Abfrage auf
`GlassfishDB`: Regel 7 greift nicht. Der volle DB-Lauf ist nicht gefahren.

**Verletzungsproben**, je ein Eingriff, zurückgespielt aus einer Kopie, nicht per `git checkout`:

| # | Eingriff | Rot |
|---|---|---|
| P1 | `AuftragsPruefung` kürzt nicht | 12 Fälle: elf der Gruppe „Leerzeichen im Schlüssel“, dazu der Kartenfall in `GespraechsfuehrungTest`; grün bleiben, wie erwartet, die Fälle, die nicht am Kürzen hängen |
| P2 | `Gespraechsfuehrung` baut den Vorschlag aus den ungekürzten Einträgen | genau der Kartenfall in `GespraechsfuehrungTest` |
| P3 | `AuftragService.lege` speichert die ungekürzten Einträge | genau `AuftraegeDbIT.leerzeichen_im_schluessel` (gespeichert wären `" 0101 159903/  A  1 /DDB0 "` und `"72332/ B 2"`) |

Danach `AuftraegeDbIT`, `TaskMigrationDbIT` und `TestsatzDefinitionDbIT` auf dem zurückgespielten Code
noch einmal grün. Der alte Failsafe-Bericht der Probe P3 hatte einen anschließenden Lauf mit
`-DexcludedGroups=db` rot gemacht, obwohl kein DB-Test lief; nach dem Löschen von
`target/failsafe-reports` grün.

**Assistent bis zur Karte:** Testsatz M1007 gegen das Modell ([`assistent.md`](assistent.md) §8.4):
20 von 21 im vollen Durchgang, die drei neuen Fälle in drei Durchgängen jedes Mal bestanden — auch
`HALLE  2` mit zwei Leerzeichen. S15 (Tippfehler) schwankte einmal. **Darstellung:**
[`assistent-frontend.md`](assistent-frontend.md) §12.10; das gebaute CSS (`pnpm build`) enthält
`.whitespace-pre{white-space:pre}` und `.whitespace-pre-wrap{white-space:pre-wrap}`. Eine
Sichtprüfung im Browser ist nicht gefahren, sie bräuchte eine Anmeldung durch Lukas.

---

## 10. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | keiner der vier; keine neue Ausnahme. Seit A4 auch `DELETE /api/tasks/{taskId}` nicht |
| **M2** Mandant als erster Pflichtparameter | jede Methode von `AuftragRepository`, auch das Laden der Typen |
| **M3** Filter im Statement | Mandant im `WHERE` jedes Statements; die Freischaltung als Join im Statement des Typkatalogs. Seit A4 auch im `DELETE` und in seiner Nachlesung |
| **M4** Isolationstest je Endpunkt | `AuftraegeIsolationDbIT`, seit A4 mit dem Löschen eines fremden Auftrags; seit 1.1.0 prüft `AssistentIsolationDbIT` die Sperre ohne Freischaltung (`404` wie erfunden) |
| **Freischaltung** (E‑1126) | genau eine Stelle, `AssistentFreigabe` (§3.9); kein zweiter Weg |
| **Regel 3** (kein Schreibzugriff auf `GlassfishDB`) | das Paket `task` liest und schreibt ausschließlich `overlord_monitor` |
| **Regeln 1 bis 6** | gelten hier nicht (E‑1016) |
| **Regel 7** | M1000 (E‑1026); für das Löschen M1005 (§9.5) |
| **Z1** | keine Uhr in Java; alle Zeitstempel aus der Datenbank (E‑1011) |

---

## 11. Offene Punkte

Nummern ab 1000 (Linie `assistent`); dieselben Zeilen stehen im Register
[`offene-punkte.md`](offene-punkte.md).

| Nr. | Punkt | Art |
|---|---|---|
| **1000** | Regel E‑1009 nennt einen Auftrag „Teilweise erledigt“, auch wenn **kein** Eintrag Ok ist, also nichts geändert wurde. Die Anzeige dafür ist offen. | Entscheidung offen |
| **1001** | Aufträge, die in `IN_AUSFUEHRUNG` hängen bleiben (die Plattform bricht ab), haben keine Frist und keinen Rückweg. | Entscheidung offen |
| **1002** | Die Schranke von 35 Zeichen je Schlüsselteil (E‑1012) ist zu bestätigen. | Entscheidung offen |
| **1003** | Ob die Plattform UTF-8 (ö in „Löschung“) und LF als Zeilenende sicher verarbeitet. | Sichtprüfung oder Messung offen |
| **1004** | Der Monitor kann die Werk-/Abladestellentabelle der Plattform nicht lesen. Ob ein Schlüssel existiert, zeigt sich erst bei der Ausführung. | Verbesserung |
| **1005** | Ungültige Definitionen sind nur im Log sichtbar — und erst, wenn ein freigeschalteter Mandant nach der Aktivierung zugreift. | Verbesserung |
| **1006** | Die Kopfzeile `Aktion=<Aktion>` ist fest; ein Typ mit anderer Kopfzeile braucht Code. | Verbesserung |
| **1007** | Nur der erste Schlüsselteil kann optional sein. | Verbesserung |
| **1008** | Datenschutz und Auftragsverarbeitung mit dem Modellanbieter, vor A2. **Erledigt am 16.10.2026:** beantwortet mit E‑1029 und E‑1030 (gebaut, aber abgeschaltet), weitergeführt als Punkt 1009 ([`assistent.md`](assistent.md) §11). | Entscheidung offen |
| **1026** | Die Plattform holt **alle fünf Minuten** ab (E‑1117) — eine Auskunft des Auftraggebers vom 19.10.2026, nicht gemessen. Wie lange ein Auftrag „Offen“ steht und damit löschbar bleibt, hängt daran. *(A4)* | Sichtprüfung oder Messung offen |
| **1037** | Eine Anfrage kann zur vollen Minute mit `500` scheitern, wenn das Speichern ihrer Sitzung einen Deadlock gegen die Aufräumung von Spring Session verliert — gesehen in den DB-Läufen zu 1.1.0 (§9.6). Beschrieben in [`authentifizierung.md`](authentifizierung.md) §12; nicht Teil dieses Schritts. *(1.1.0)* | Fehler |

**Fortgeschrieben am 19.10.2026 (A4):** Punkt **1000** bleibt offen. Die Übersicht zeigt einen
solchen Auftrag als „Teilweise erledigt“ mit „<n> von <n> nicht erfolgreich“
([`assistent-frontend.md`](assistent-frontend.md) §12.4); das Wort passt dann nicht, entschieden ist
es nicht. Die offenen Punkte der Oberfläche aus A4 (1023 bis 1025, 1027 bis 1032) stehen in
[`assistent-frontend.md`](assistent-frontend.md) §12.11.
