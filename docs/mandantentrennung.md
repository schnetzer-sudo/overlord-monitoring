# Mandantentrennung

Die wichtigste Regel des Projekts. Entsteht in Schritt 3, Teil 1 (Backend).

Beschreibt den `MandantContext`, warum ADMIN einen Mandanten *wählt* statt alle zu sehen, die genau
zwei Endpunkte, die eine Mandanten-ID entgegennehmen, die ArchUnit-Regel und die Vorlage für den
Isolationstest.

Anmeldung, Sperre und Sitzung stehen in [`authentifizierung.md`](authentifizierung.md).

---

## 1. Der `MandantContext`

```java
public record MandantContext(String mandantId) { … }
```

**Genau eine Mandanten-ID, für jede Rolle.** Auch ADMIN arbeitet in genau einem Mandantenkontext und
wechselt ihn, statt alle gleichzeitig zu sehen.

Was das gewinnt:

- Es gibt **keinen Codepfad ohne Mandantenfilter** — auch keinen, der „für Admins" den Filter
  wegließe.
- Die Repository-Signaturen sind für beide Rollen **identisch**.
- Der Isolationstest gilt für ADMIN **unverändert**.

Der Preis: Ansichten sind für ADMIN nur eingeschränkt teilbar, weil der aktive Mandant in der Sitzung
steht und nicht in der URL. Deshalb gehört er in Teil 2 sichtbar in die Kopfzeile — sonst zeigt eine
Ansicht unbemerkt einen anderen Ausschnitt.

**Aufgelöst wird er ausschließlich aus der Sitzung** (`security/MandantContextProvider`) — niemals aus
Pfad, Query, Header oder Cookie. Die Session wird dabei nie angelegt (`getSession(false)`): Ein
Lesezugriff auf den Mandanten darf keine Sitzung erzeugen.

Ist kein Mandant gewählt, wirft der Provider `KeinMandantGewaehltException` → `403`, Problemtyp
`kein-mandant-gewaehlt`. Das ist bei jedem ADMIN und bei jedem Nutzer mit mehreren Mandanten der
Zustand direkt nach dem Anmelden. **Ein Zugriff „einfach ohne Filter" existiert nicht.**

> **Hinweis für Schritt 10.** Der Rollup-Job läuft ohne Sitzung und wird sich seinen Kontext je
> Mandant **explizit erzeugen** müssen. Deshalb ist der Konstruktor öffentlich und der Typ nicht an
> die Web-Schicht gebunden. Diese Fähigkeit wird jetzt nicht gebaut, aber auch nicht verbaut.

---

## 2. Berechtigung ist eine Menge, keine Rolle

> **Die Regel:** *Du darfst zu jedem Mandanten wechseln, für den du berechtigt bist.*

| Rolle | Zulässige Menge |
|---|---|
| `ADMIN` | alle Zeilen aus `GlassfishDB.Mandant` |
| `MANDANT` | die eigenen Zeilen in `overlord_monitor.app_user_mandant` |

Damit ist ADMIN **kein Sonderfall**, sondern der Nutzer mit der größten Menge. Und ein
MANDANT-Nutzer mit zwei Mandanten funktioniert ohne Zusatzbau — was bei `NEXANS`/`NXHBE` und
`IBIS`/`IBISGUS` absehbar ist (Annahme A2 steht unter Druck).

`GET /api/mandanten` liefert **genau diese Menge**, nichts darüber hinaus. Die Liste ist selbst eine
Auskunft.

Hat ein Nutzer genau einen zulässigen Mandanten, wird dieser beim Anmelden gesetzt. Bei mehreren —
und bei jedem ADMIN — bleibt die Auswahl offen; einen zu raten wäre die schlechteste Variante, weil
der Nutzer dann Daten sähe, die er nicht ausgewählt hat.

### Die technischen Mandanten

`SYSTEM` (Systemmandant) und `WOC` („Without Contract", Auffangbecken für Verkehr ohne hinterlegten
Vertrag) sind **keine Kunden**. Sie werden hier bewusst **nicht** herausgefiltert: Die Menge ist genau
die Menge der Berechtigung und keine kuratierte Kundenliste. Für die Darstellung in Teil 2 und für das
Dashboard (Schritt 10) brauchen sie eine gesonderte Behandlung — sonst steht ein Auffangbecken
zwischen echten Kunden.

### Projekte ohne Mandantenzuordnung (Annahme A8)

142 Projekten stehen 134 Zeilen in `ProjectMandant` gegenüber. Nachrichten in Projekten ohne
Zuordnung sind im Werkzeug **für niemanden sichtbar, auch nicht für ADMIN**. Das ist geklärt und
bewusst akzeptiert: Es wird **kein** Sonderpfad und kein Pseudo-Mandant gebaut.

Dieser Eintrag muss stehen bleiben. Wird eine solche Nachricht gesucht, findet sie niemand — und ohne
diese Zeile wüsste auch niemand warum.

---

## 3. Die genau zwei Ausnahmen von Regel M1

**Kein Endpunkt nimmt eine Mandanten-ID entgegen.** Diese Liste ist vollständig und prüfbar:

| # | Endpunkt | Warum zulässig | Verhalten bei unzulässiger ID |
|---|---|---|---|
| 1 | `POST /api/auth/mandant` | wählt aus der **ohnehin zulässigen Menge** aus; die ID bestimmt nicht, *was* gelesen werden darf, sondern nur *welcher* der erlaubten Ausschnitte aktiv ist | `404` — ununterscheidbar von einer erfundenen ID |
| 2 | `POST /api/admin/users` | hier wird ein Konto **definiert**, kein Datenausschnitt **abgefragt**; welchen Mandanten der anlegende Admin gerade aktiv hat, sagt nichts darüber aus, für wen das neue Konto gilt. Nur `ADMIN` | `404` — ein ADMIN kennt die Mandantenliste ohnehin |

**Taucht hier jemals eine dritte auf, ist das ein Signal und keine Kleinigkeit.**

### Warum Ausnahme 1 nicht auf Existenz prüft

`MandantService.wechsle` prüft **ausschließlich gegen die zulässige Menge** — nie gegen
`GlassfishDB.Mandant`. Eine Existenzprüfung wäre genau der Unterschied, den es nicht geben darf: Ein
existierender, aber fremder Mandant und eine erfundene ID müssen ununterscheidbar beantwortet werden,
sonst ließe sich über den Umschalt-Endpunkt die gesamte Mandantenliste abfragen.

Der Isolationstest vergleicht die **vollständigen Antwortrümpfe** beider Fälle (ohne `traceId`) und
schlägt fehl, sobald sie sich unterscheiden.

Jeder Wechsel geht ins `audit_log` (`MANDANT_GEWECHSELT`), mit altem und neuem Mandanten.

---

## 4. Regel M2, maschinell geprüft

> **Jede Repository-Methode, die `jooq.glassfish` anfasst, hat `MandantContext` als ersten
> Pflichtparameter.** Keine Überladung ohne ihn — auch nicht `private`, auch nicht „nur für den
> Test".

`PaketstrukturTest` prüft zwei Dinge:

1. **`mandantcontext_ist_erster_parameter`** — sammelt alle handgeschriebenen Klassen, die
   irgendeinen Typ aus `…jooq.glassfish` verwenden, und verlangt für jede öffentliche Methode
   `MandantContext` an erster Stelle. Der Test schlägt außerdem fehl, wenn es **keine** solche Klasse
   mehr gibt — sonst prüfte er irgendwann nichts mehr.
2. **`ausnahme_nur_im_mandantrepository`** — die Ausnahme-Markierung darf nur an einer Stelle stehen.

### Die eine Ausnahme: `@OhneMandantenkontext`

Die Menge der zulässigen Mandanten muss gelesen werden, **bevor** feststeht, welcher Mandant aktiv
ist. Ein Kontext, der sich selbst voraussetzt, existiert nicht. Ohne eine benannte Ausnahme wäre
Regel M2 nicht umsetzbar.

`security/OhneMandantenkontext` ist eine Annotation mit **Pflichtbegründung** und steht ausschließlich
an den drei Methoden von `security/MandantRepository`:

| Methode | Begründung |
|---|---|
| `findeAlle()` | die wählbare Menge für ADMIN |
| `findeFuerNutzer(long)` | die wählbare Menge für MANDANT |
| `existiert(String)` | Prüfung beim Anlegen eines Kontos (Ausnahme 2 oben) |

**Keine so markierte Methode liefert jemals fachliche Daten** — weder Nachrichten noch Prozesse noch
Projekte. Sie liefern ausschließlich Stammdaten über Mandanten selbst.

Die Markierung ist eine Ausnahme, kein Werkzeug. Sie wird genauso vollständig geführt wie die Liste
der zwei Endpunkte oben.

---

## 5. Die Vorlage für den Isolationstest

`MandantenIsolationDbIT` ist die Vorlage, die **ab Schritt 4 für jeden neuen Endpunkt kopiert wird**.
Ein neuer Endpunkt ohne diesen Test wird nicht gemergt (Regel M4). Das ist kein Richtwert.

Das Muster in vier Schritten:

1. Ein bekannter Datensatz von **Mandant B** wird ermittelt.
2. Der Endpunkt wird als Nutzer von **Mandant A** aufgerufen — mit genau dieser Kennung.
3. Erwartet wird `404` beziehungsweise eine leere Liste. **Niemals `403`.**
4. Zusätzlich: Kein Ergebnis der Antwort gehört zu Mandant B.

Und die Gegenprobe, die den eigentlichen Kern ausmacht:

5. Dieselbe Anfrage mit einer **erfundenen** ID muss eine **ununterscheidbare** Antwort liefern. Ein
   `404` allein genügt nicht.

Schritt 4 tauscht dafür nur den Aufruf und die Kennung aus; Aufbau, Nutzer und die Gegenprobe bleiben.

### Die beiden Testmandanten

`MANDANT_A = VOTG`, `MANDANT_B = SUTTONS` — zwei Mandanten aus **verschiedenen Häusern**. Bewusst
**nicht** `NEXANS`/`NXHBE` oder `IBIS`/`IBISGUS`: Zwei Mandanten desselben Konzerns sind ein
schlechter Beweis für eine Trennung, die zwischen Firmen greifen soll.

Der Test prüft zu Beginn, dass beide in der Testkopie existieren und die erfundene ID nicht. Schlägt
das fehl, hat sich die Testkopie geändert — nicht der Code.

### Testkonten

`SicherheitsTestbasis` legt Konten mit dem Präfix `it-` an und räumt sie samt ihren Sitzungen nach
jedem Test wieder weg. Geschrieben wird **ausschließlich** in `overlord_monitor`; `GlassfishDB` wird
nur gelesen (Regel S1).

Die Tests laufen gegen einen **echten Server** mit einem echten HTTP-Client, nicht über MockMvc —
Begründung in [`authentifizierung.md`](authentifizierung.md) §5.

### Benennung

Die Richtlinie nennt das Namensschema `<Endpunkt>IsolationTest`. Datenbankgebundene Tests laufen in
diesem Projekt seit Schritt 2 über Failsafe und heißen `*IT`. Verbindlich ist deshalb
**`<Thema>IsolationDbIT`** — ein Isolationstest braucht immer eine Datenbank und gehört damit in
dieselbe Gruppe wie die übrigen `@Tag("db")`-Tests.

---

## 6. Messungen (Regel L7)

Gemessen am 29.07.2026 gegen die Testkopie, beste von fünf Läufen nach einem Aufwärmlauf:

| Abfrage | Zugriffspfad (`EXPLAIN`) | Laufzeit |
|---|---|---|
| `MandantRepository.findeAlle` | `type=ALL`, `rows=10`, `Using filesort` | ~0,6 ms |
| `MandantRepository.findeFuerNutzer` (schemaübergreifend) | `app_user_mandant`: `type=ref`, `key=PRIMARY`, `Using index` · `Mandant`: `type=eq_ref`, `key=PRIMARY` | ~0,6 ms |
| `MandantRepository.existiert` | `type=const`, `key=PRIMARY`, `Using index` | ~0,4 ms |

`type=ALL` auf `Mandant` ist hier **kein Befund**: Die Tabelle hat zehn Zeilen und passt in eine
einzige Seite; ein Index würde die Sortierung nicht billiger machen. Sollte die Mandantenzahl je
dreistellig werden, ist das die Stelle, an der man nachsieht.

Der schemaübergreifende Join läuft erwartungsgemäß über `app_user_mandant` zuerst und trifft
`Mandant` per Primärschlüssel. Das ist der Nachweis, dass die Sortierungen beider Schemata verträglich
sind — bräche `utf8mb4_general_ci` auf einer Seite weg, stünde hier ein Full Scan oder ein
Sortierungsfehler.

---

## 7. Regelbezug

| Regel | Wo umgesetzt |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | §3, zwei benannte Ausnahmen |
| **M2** Mandant als erster Pflichtparameter | §4, ArchUnit |
| **M3** Filter im Statement, nicht nachgelagert | ab Schritt 4; hier über die zulässige Menge in `MandantService` |
| **M4** Isolationstest je Endpunkt | §5 |
| **M5** Trennung gilt auch quer | ab Schritt 4 (Verkettung, Suche, Rollup, Download) |
| **404 statt 403** | §3, geprüft durch Vergleich beider Antwortrümpfe |

---

## 8. Offene Punkte

- **M3 ist hier noch nicht anwendbar.** Der Filter über `ProjectMandant` wird Bestandteil jedes
  Statements ab Schritt 4. Was Schritt 3 liefert, ist der Kontext und der Nachweis, dass er nicht
  umgangen werden kann.
- **Verliert ein Nutzer während einer laufenden Sitzung seine Mandantenzuordnung**, fällt der aktive
  Mandant von selbst weg — `MandantService.aktiver` prüft gegen die zulässige Menge, nicht gegen den
  rohen Sitzungswert. Fachliche Endpunkte antworten dann mit `kein-mandant-gewaehlt`.
