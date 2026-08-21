# Mandantentrennung

Die wichtigste Regel des Projekts. Entsteht in Schritt 3, Teil 1 (Backend).

Beschreibt den `MandantContext`, warum ADMIN einen Mandanten *wählt* statt alle zu sehen, die genau
drei Endpunkte, die eine Mandanten-ID entgegennehmen, die ArchUnit-Regel und die Vorlage für den
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

## 3. Die genau drei Ausnahmen von Regel M1

**Kein Endpunkt nimmt eine Mandanten-ID entgegen.** Diese Liste ist vollständig und prüfbar:

| # | Endpunkt | Warum zulässig | Verhalten bei unzulässiger ID |
|---|---|---|---|
| 1 | `POST /api/auth/mandant` | wählt aus der **ohnehin zulässigen Menge** aus; die ID bestimmt nicht, *was* gelesen werden darf, sondern nur *welcher* der erlaubten Ausschnitte aktiv ist | `404` — ununterscheidbar von einer erfundenen ID |
| 2 | `POST /api/admin/users` | hier wird ein Konto **definiert**, kein Datenausschnitt **abgefragt**; welchen Mandanten der anlegende Admin gerade aktiv hat, sagt nichts darüber aus, für wen das neue Konto gilt. Nur `ADMIN` | `404` — ein ADMIN kennt die Mandantenliste ohnehin |
| 3 | `PUT /api/admin/users/{id}/tenants` | hier wird die **Mandantenmenge eines Kontos gepflegt**, kein Datenausschnitt **abgefragt**; die übergebenen IDs sagen nichts darüber aus, was der pflegende Admin lesen darf, sondern nur, für wen das fremde Konto künftig gilt. Nur `ADMIN` | `404` — ein ADMIN kennt die Mandantenliste ohnehin |

**Alle drei definieren eine Berechtigung, statt einen Datenausschnitt abzufragen.** Das ist das
Merkmal, an dem eine Ausnahme zulässig wird — und das einzige.

**Taucht hier jemals eine vierte auf, ist das ein Signal und keine Kleinigkeit.**

> **Nachgetragen 06.08.2026 (Schritt 4, Aufgabe 12).** `GET /api/prozesse`
> ([`prozessauswahl.md`](prozessauswahl.md)) liefert die Prozesse eines Mandanten und wäre der
> naheliegende Kandidat für eine dritte Ausnahme — er ist **keine**. Der Endpunkt nimmt überhaupt
> keinen Parameter entgegen; der Mandant kommt wie überall sonst aus der Sitzung über
> `MandantService.aktuellerKontext`. **Die Liste bleibt bei zwei Einträgen.**

> **Ergänzt 20.08.2026 (Schritt 9a).** Die dritte Ausnahme ist gesetzt:
> `PUT /api/admin/users/{id}/tenants` pflegt die Mandantenmenge eines Kontos. Der
> Satz über der Liste hieß bis heute „Taucht hier jemals eine **dritte** auf“ — er hat gewirkt,
> und deshalb bleibt er stehen, verschoben auf die vierte. Diese Ausnahme ist bewusst gesetzt,
> geprüft und begründet; sie zu streichen würde ihn für die vierte entwerten.
>
> `POST /api/admin/users` bleibt unverändert bei **einem** Mandanten — die Signatur aus Schritt 3
> wird nicht angefasst. Die Menge wird nach dem Anlegen über die neue Ausnahme gepflegt, nicht
> beim Anlegen.
>
> *Korrigiert 20.08.2026:* Die Überschrift dieses Abschnitts lautete „Die genau **zwei** Ausnahmen
> von Regel M1“.
>
> *Korrigiert 21.08.2026 (Schritt 9a, Teil Backend):* Der Pfad stand hier zweimal als
> `PUT /api/admin/users/{benutzername}/mandanten`. [`benutzerverwaltung.md`](benutzerverwaltung.md)
> §5 führte dagegen `{id}/tenants` und begründet die englische Fassung ausdrücklich — **ein
> Widerspruch zwischen zwei verbindlichen Dateien**, gemeldet und entschieden, bevor gebaut wurde.
> **Gebaut ist `{id}/tenants`:** Die vier Nachbarendpunkte (`lock`, `active`, `role`, `password`)
> tragen alle `{id}`, und ein deutscher Unterpfad unter einer englischen Sammlung wäre schlechter
> als beide reinen Varianten. Dieselbe Korrektur steht in
> [`authentifizierung.md`](authentifizierung.md) §8 und
> [`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §7; gebaut in
> [`benutzerverwaltung-backend.md`](benutzerverwaltung-backend.md) §4.

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
der drei Endpunkte oben.

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

> **Ergänzt 06.08.2026 (Schritt 4, Aufgabe 12).** Die zweite Kopie ist `ProzesseIsolationDbIT`
> ([`prozessauswahl.md`](prozessauswahl.md) §5). Dort sieht die Gegenprobe **anders** aus, und das
> ist kein Abweichen von der Vorlage, sondern ihre Übertragung: Der Endpunkt nimmt **keinen
> Parameter** entgegen, es gibt also keine Eingabe, über die sich Existenz erfragen ließe. Die
> Gegenprobe verschiebt sich deshalb von der Eingabe auf die **Ausgabe** — der Antwortrumpf darf
> keine fremde Kennung *und keinen fremden Prozessnamen* enthalten. Zusätzlich prüft der Test die
> Rolle ADMIN: ohne aktiven Mandanten `403`, nach dem Wechsel genau der eine Mandant.
>
> **Ergänzt 10.08.2026 (Schritt 6, Teil 1).** Die vierte Kopie ist `KettenIsolationDbIT`
> ([`verkettung.md`](verkettung.md) §6) — zwei Endpunkte, zwei Tests, Paarung wie die Vorlage
> (`VOTG` gegen `SUTTONS`). Zwei Dinge kommen dort hinzu, die es vorher nicht gab:
>
> 1. **Regel M5 hat bei der Kette eine zweite Seite: die Zählung.** `abwaertsGesamt` trägt
>    denselben Mandantenfilter wie die gelieferten Zeilen. Eine ungefilterte Zahl neben gefilterten
>    Zeilen wäre selbst eine Auskunft über fremden Bestand — und sie sähe aus wie ein Fehler des
>    Werkzeugs, nicht wie ein Leck. *(Das Feld hieß bis zum 11.08.2026 `nachfolgerGesamt`;
>    [`verkettung.md`](verkettung.md) §2 sagt, warum es umbenannt wurde.)*
> 2. **Die Gegenprobe läuft zusätzlich mit einer *echten* fremden Kennung**, nicht nur mit einer
>    erfundenen: als ADMIN den Mandanten wechseln, dort eine `MessageID` holen, zurückwechseln,
>    dieselbe Kennung anfragen. Verglichen werden Rumpf **und Laufzeit** — genau der Kanal, den der
>    Absatz „Woran die Zusage tatsächlich hängt" unten beschreibt.
>
> **Ergänzt 13.08.2026 (Schritt 7, Teil 2b).** `BamSucheIsolationDbIT`
> ([`bam-suche.md`](bam-suche.md) §7) ist die erste Kopie, bei der die Gegenprobe **ohne `404`**
> auskommen muss — und das ist ihre Übertragung, kein Abweichen von der Vorlage. Bei Detail, Kette
> und Belegdaten steht die Kennung *im Pfad*, und verglichen werden zwei `404`-Rümpfe. Die Suche
> nimmt dagegen eine **Frage** entgegen: Ein Wert, den es für diesen Mandanten nicht gibt, ist `200`
> mit leerer Liste — genau wie ein Wert, den es überhaupt nicht gibt. **Die Ununterscheidbarkeit
> verschiebt sich damit vom Statuscode auf den Rumpf.**
>
> Zwei Dinge kommen dort hinzu, die es vorher nicht gab:
>
> 1. **Der Rumpf zitiert die Frage**, weil er die Suchbegriffe samt ihrer gebildeten Fassungen nennt
>    (keine stille Korrektur). Das ist dieselbe Lage wie bei `instance` nach RFC 9457 und wird
>    genauso behandelt: Der Test normalisiert das Zitat **und** weist zusätzlich nach, dass es genau
>    die gesendete Eingabe ist und jede Fassung auf ihr endet. Damit die *Zahl* der Fassungen den
>    Vergleich nicht stört, ist die erfundene Eingabe **genauso lang** wie die fremde.
> 2. **Die Abschneidung ist selbst eine Auskunft.** Der Test prüft dieselbe Suche mit zwei Mandanten
>    und verlangt **verschiedene** Zahlen — 50 Treffer mit `abgeschnitten: true` für den einen, null
>    Treffer mit `abgeschnitten: false` für den anderen. Würde sie aus den Rohtreffern gezählt,
>    meldete auch der zweite „es gibt mehr" und sagte damit etwas über die Datenmenge des ersten.
>
> **Ergänzt 06.08.2026 (Schritt 4).** Die erste Kopie der Vorlage ist `NachrichtenIsolationDbIT`
> ([`nachrichtenliste.md`](nachrichtenliste.md) §3). Sie paart `NEXANS` gegen `SUTTONS` statt `VOTG`
> gegen `SUTTONS` — auch das zwei verschiedene Häuser, aber die beiden mit dem größten Bestand, und
> damit die Paarung, bei der ein Leck am ehesten sichtbar würde. Sie benutzt außerdem ein
> **absolutes** Zeitfenster: Außer `NEXANS` endet jeder Mandant am 30.12.2025, ein relatives Fenster
> hinge damit am Datenstand der Testkopie. Der erste Testfall hält fest, dass beide Mandanten im
> gewählten Fenster Daten haben — sonst bewiese der Test nur, dass leer leer ist.
>
> **Ergänzt 20.08.2026 (Schritt 9a).** Bei **mandantenfreien** Endpunkten gibt es keine
> Mandantengrenze, an der ein Leck entstehen könnte: Die Nutzerverwaltung arbeitet auf `app_user`,
> und `app_user` liegt in `overlord_monitor` — Regel M2 bindet nur Repositories auf
> `jooq.glassfish`. Der Test verschiebt sich deshalb von der Mandantengrenze auf die
> **Rollengrenze**: Jeder Admin-Endpunkt gibt einem MANDANT-Nutzer `403` — **auch dann, wenn er
> auf dessen eigenes Konto zeigt.** Genau dieser Fall ist der Kern, denn er ist der einzige, bei
> dem eine Berechtigungsprüfung über den Kontoinhaber plausibel aussieht und trotzdem falsch ist.
>
> **Das ist die zweite Übertragung derselben Regel**, nach der Verschiebung von der Eingabe auf
> die Ausgabe bei `ProzesseIsolationDbIT`. **Keine Aufweichung** — die Regel verlangt einen Test
> je Endpunkt, nicht eine bestimmte Grenze; wo die Mandantengrenze fehlt, tritt die Rollengrenze
> an ihre Stelle, und ohne diesen Test wird genauso wenig gemergt.

Der Test prüft zu Beginn, dass beide in der Testkopie existieren und die erfundene ID nicht. Schlägt
das fehl, hat sich die Testkopie geändert — nicht der Code.

### Woran die Zusage „ununterscheidbar" tatsächlich hängt

Nachgetragen am **10.08.2026**, weil Schritt 6 (Verkettung) und Schritt 8 (Rohdaten-Download)
denselben Pfadparameter bekommen wie das Nachrichtendetail — dieselbe Bauform, dieselbe Falle.

> **Die Zusage hängt nicht daran, dass die beiden `404`-Rümpfe gleich aussehen, sondern daran, dass
> beide Fälle *dasselbe Statement mit null Zeilen* sind.** Baut jemand später eine Existenzprüfung
> davor, stimmen die Rümpfe weiterhin überein — und die Laufzeit verrät den Unterschied. Das Feld
> `instance` spiegelt den angefragten Pfad und ist deshalb unbedenklich, solange es eine Spiegelung
> bleibt; der Test weist das zeichenweise nach.

**Warum das eine eigene Zeile wert ist.** Der Isolationstest vergleicht Antwortrümpfe. Das ist die
richtige Prüfung, aber sie prüft die *Wirkung* und nicht die *Ursache*: Ein Code, der erst die
Existenz nachschlägt und dann bei einer fremden Nachricht denselben festen Text ausgibt, besteht ihn
— und ist trotzdem unterscheidbar, weil „gibt es nicht" einen Zugriff kostet und „gehört einem
anderen" zwei. Über genug Anfragen ist das ein messbarer Kanal.

**Die Zusage ist deshalb die Bauform, nicht der Text:** Der Mandantenfilter steht als `EXISTS` **im**
Statement (Regel M3), es kommt in beiden Fällen dieselbe leere Menge zurück, und derselbe feste Text
geht hinaus. Wer eine Existenzprüfung davorbaut, bricht die Zusage — auch wenn kein Test rot wird.

**`instance` ist die eine Stelle, an der sich die beiden Antworten unterscheiden**, und sie ist keine
Auskunft: Nach RFC 9457 ist es der angefragte Pfad, und weil die Kennung bei diesen Endpunkten *im
Pfad* steht, enthält `instance` sie zwangsläufig. Es ist das **Zitat der Frage**, nicht eine Antwort
über den Bestand. Damit das eine Spiegelung bleibt und nicht unbemerkt zu einer Nachschlage-Auskunft
wird, normalisiert der Test `instance` nicht nur wie `traceId`, sondern prüft zusätzlich **Zeichen
für Zeichen**, dass es genau dem gesendeten Pfad entspricht
([`nachrichtendetail.md`](nachrichtendetail.md) §7).

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
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | §3, drei benannte Ausnahmen |
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
