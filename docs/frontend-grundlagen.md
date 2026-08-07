# Frontend-Grundlagen

Entsteht in Schritt 3, Teil 2. Beschreibt den **Unterbau** der Oberfläche: wie der Browser mit dem
Backend spricht, warum die Routensperre kein Schutz ist, wie die Sprachdateien aufgebaut sind,
welche Regeln für den Zwischenspeicher gelten und wie ein Fehlertyp zu einer Übersetzung wird.

Das Aussehen steht in [`visuelles-konzept.md`](visuelles-konzept.md). Die Endpunkte selbst stehen in
[`authentifizierung.md`](authentifizierung.md) und [`mandantentrennung.md`](mandantentrennung.md).

---

## 1. Der Browser spricht ausschließlich mit Next.js

Jeder `/api`-Aufruf geht an die Next.js-Adresse. Ein Rewrite in `next.config.ts` reicht ihn an das
Backend weiter:

```ts
async rewrites() {
  return [{ source: "/api/:pfad*", destination: `${BACKEND}/api/:pfad*` }];
}
```

**Was das erspart:** kein CORS, kein `credentials: "include"`, keine Cookie-Domain, keine Sonderregel
für `SameSite`, keine Preflight-Anfragen. Alles ist gleiche Herkunft.

**Warum das auch im Betrieb richtig ist:** Dort zieht ohnehin ein Reverse Proxy Frontend und Backend
unter eine Domain. Der Rewrite bildet genau diese Topologie schon lokal ab — was in der Entwicklung
funktioniert, funktioniert deshalb auch danach. Die verbreitete Alternative (Frontend auf Port 3000
spricht direkt mit Port 8080) verhält sich in **beiden** Umgebungen anders als die Produktion und
verschiebt die Cookie-Probleme nur nach hinten.

**Warum in der Konfiguration und nicht in einem Route Handler:** Ein eigener Handler müsste
Kopfzeilen, Cookies, Statuscodes und Datenströme selbst durchreichen — eine zweite Stelle, an der
etwas verloren geht. Beim Rohdaten-Download in Schritt 8 wäre das ein echtes Problem.

Die Backend-Adresse kommt aus `OVERLORD_BACKEND_URL`, Vorlage in `frontend/.env.example`. Der
Standard `http://localhost:8080` gilt ausschließlich lokal. **Die Variable trägt bewusst kein
`NEXT_PUBLIC_`**: Der Browser sieht diese Adresse nie.

### CSRF

Das Backend legt den Token als Cookie `XSRF-TOKEN` ab (nicht `HttpOnly`) und erwartet ihn als
Kopfzeile `X-XSRF-TOKEN`. Sein `CsrfCookieFilter` hängt **vor** der Autorisierung, das Cookie
entsteht also auch bei einer `401`-Antwort.

`lib/http.ts` nutzt genau das: Fehlt der Token vor dem ersten schreibenden Aufruf, holt es ihn mit
einem `GET /api/auth/me` — statt den ersten Versuch mit `403` scheitern zu lassen. Lesende Aufrufe
brauchen ihn nicht.

Nachgemessen am 29.07.2026 über den Rewrite:

```
GET /api/auth/me  →  401
                     set-cookie: XSRF-TOKEN=…; Path=/; SameSite=Lax
                     content-type: application/problem+json
```

---

## 2. Die Routensperre ist Bequemlichkeit, kein Schutz

`src/proxy.ts` (seit Next.js 16 heißt die Datei so statt `middleware.ts`) prüft **ausschließlich, ob
ein Sitzungs-Cookie vorhanden ist**. Nicht, ob es gültig ist. Keine Rolle, kein Mandant.

Sie kann das gar nicht: Das Cookie ist `HttpOnly` und sein Inhalt ist eine undurchsichtige
Sitzungs-ID, die nur das Backend auflösen kann.

Ihr einziger Zweck ist, einem nicht angemeldeten Nutzer den Ladevorgang einer Seite zu ersparen, die
ihm sofort ein `401` einbrächte. Der Hinweis steht als Kommentar in der Datei — ohne ihn baut
irgendwann jemand eine Berechtigungsprüfung hinein, und dann liegt die Sicherheitsentscheidung im
Browser des Nutzers.

**Verbindlich entscheidet immer das Backend**, in jedem einzelnen SQL-Statement.

### Zwei Feinheiten

**`/api` ist vom Matcher ausgenommen.** Sonst bekäme der HTTP-Client bei fehlender Sitzung eine
HTML-Weiterleitung statt eines `401`, und die gesamte Fehlerbehandlung liefe ins Leere.

**Die Anmeldeseite leitet nur ohne `weiter`-Parameter weiter.** Ohne diese Bedingung entstünde bei
einem **abgelaufenen** Cookie eine Endlosschleife: Der Sperre genügt die Anwesenheit des Cookies, sie
schickt zur Startseite; das Backend antwortet `401`; der QueryClient schickt zurück zur Anmeldung;
die Sperre sieht wieder das Cookie. Mit `weiter` bleibt die Anmeldeseite stehen.

### Zurück im Browser

`Cache-Control: no-store` würde den Zurück-Vorwärts-Zwischenspeicher des Browsers abschalten — genau
das, was nach dem Abmelden gebraucht wird.

**Es lässt sich hier nicht setzen.** Gemessen gegen Next.js 16.2.11: Eigene Kopfzeilen aus `proxy.ts`
kommen beim Browser an, `Cache-Control` nicht. Next.js vergibt für dynamisch gerenderte Seiten seinen
eigenen Wert (`no-cache, must-revalidate`) und überschreibt sowohl die Kopfzeile aus `proxy.ts` als
auch `headers()` aus `next.config.ts`. Beides wurde ausprobiert, beides ist wirkungslos.

`no-cache` verhindert die Auslieferung aus dem HTTP-Zwischenspeicher, **nicht** die Wiederherstellung
aus dem bfcache. Der Schutz liegt deshalb im Anwendungsrahmen: Er hört auf `pageshow` und lädt neu,
sobald `event.persisted` gesetzt ist. Die neue Anfrage läuft durch die Routensperre, und ohne
Sitzungs-Cookie landet der Nutzer auf der Anmeldung.

---

## 3. Der Ablauf nach dem Anmelden

Die Verzweigung steht in **einer reinen Funktion**, `lib/ablauf.ts`:

```
Änderungszwang  →  Mandantenauswahl  →  Startseite
```

| Zustand | Ziel |
|---|---|
| nicht angemeldet | `/anmeldung` |
| `mustChangePassword` | `/passwort` — schlägt alles andere |
| `mandant === null` | `/mandantenauswahl` |
| sonst | bleibt, wo er ist |

Ohne React und ohne Netzwerk, damit sie sich prüfen lässt: `tests/ablauf.test.ts` deckt unter anderem
den Fall ab, dass **beide** Bedingungen zugleich gelten — dann gewinnt der Änderungszwang. Der
Anwendungsrahmen tut nichts weiter, als das Ergebnis an den Router weiterzureichen.

Auch das ist keine Absicherung: Der `PasswortwechselInterceptor` im Backend lehnt bei gesetztem Zwang
jeden Endpunkt außer Selbstauskunft, Passwortänderung und Abmeldung ab, und ohne aktiven Mandanten
antwortet jeder fachliche Endpunkt mit `kein-mandant-gewaehlt`. Die Oberfläche zeigt nur den Weg,
statt den Nutzer gegen eine Fehlermeldung laufen zu lassen.

**Solange der Änderungszwang steht, gibt es keine Navigation.** Ein Menü wäre dort eine Einladung in
die Sackgasse.

### Der `weiter`-Parameter

Nach der Anmeldung geht es an den ursprünglich angefragten Ort zurück. Ungeprüft wäre das eine offene
Weiterleitung: Ein Link auf `/anmeldung?weiter=https://…` führte nach erfolgreicher Anmeldung auf
eine fremde Seite — mit dem Vertrauen, das der Nutzer gerade dieser Anwendung entgegengebracht hat.

`lib/routen.ts` lässt deshalb nur einen Pfad innerhalb der Anwendung durch: genau ein führender
Schrägstrich, kein Protokoll, kein Backslash, nicht die Anmeldeseite selbst. Alles andere wird zur
Startseite. Geprüft in `tests/routen.test.ts`.

---

## 4. Sprachen

### Aufbau

```
src/i18n/
├─ de.ts        Leitsprache. Definiert zugleich den Typ `Texte`
├─ en.ts        `export const en: Texte = { … }`
├─ index.ts     Sprachliste, Cookie-Name, Auswahl
├─ server.ts    aktive Sprache für Server-Komponenten
├─ provider.tsx Kontext für Client-Komponenten (`useTexte`, `useSprache`)
└─ aktion.ts    Server-Aktion zum Umschalten
```

**Keine Zeichenkette in einer Komponente.** Sie greift über `texte.anmeldung.titel` zu — verschachtelt
und typsicher, kein Nachschlagen über einen Punktpfad zur Laufzeit.

Zwei Sicherungen gegen Abweichung:

1. **Zur Bauzeit** — `en: Texte`. Ein fehlender Schlüssel ist ein Typfehler, ein überzähliger auch
   (überschüssige Eigenschaften eines Objektliterals).
2. **Zur Laufzeit** — `tests/sprachdateien.test.ts` vergleicht beide Schlüsselsätze in beide
   Richtungen und prüft, dass kein Text leer ist.

`de.ts` trägt bewusst **kein** `as const`: Sonst wären die Werte Literaltypen und jede englische
Übersetzung wäre „nicht zuweisbar an `'Anmeldung'`".

### Kein Sprachpräfix in der URL

Die Sprache ist eine Eigenschaft des **Nutzers**, nicht der Ansicht. Ein geteilter Link erscheint
beim Empfänger in dessen Sprache — bei einem Werkzeug, in dem man Links zu Störungen weitergibt, ist
das der wichtigere Fall.

Die Wahl liegt im Cookie `overlord_sprache` (ein Jahr, `SameSite=Lax`, nicht `HttpOnly`). Das
Wurzel-Layout liest es serverseitig, setzt `<html lang>` und reicht **nur die aktive** Sprachdatei in
den Client-Kontext — die zweite landet nie im ausgelieferten Zustand.

Umgeschaltet wird über eine **Server-Aktion** in einem Formular. Der Grund: Das Layout liest die
Sprache auf dem Server, der neue Wert muss also dort ankommen; `revalidatePath("/", "layout")`
erzwingt das. Nebeneffekt, der es wert ist: Die Umschaltung funktioniert auch ohne JavaScript.

**Eine Spalte für die Sprachwahl am Nutzer gibt es bewusst nicht.** Das Cookie genügt, solange
niemand die Sprache geräteübergreifend erwartet. Wird das je gefordert, kommt eine Spalte an
`app_user` dazu und das Cookie wird zum Zwischenspeicher — kein Umbau der Oberfläche.

### Datum und Zahlen

Über `Intl` mit der aktiven Sprache (`lib/format.ts`).

> ⚠️ **Korrigiert am 06.08.2026 (Schritt 4, Aufgabe 11). Der bisherige Stand dieses Abschnitts ist
> überholt.** Er lautete: „Die Zeitstempel aus `GlassfishDB` sind Wanduhrzeit des Altsystem-Servers
> ohne Zeitzone. Sie werden angezeigt **wie geliefert**: keine Umrechnung, kein `timeZone` beim
> Formatieren" — und `lib/format.ts` las die gelieferten Felder deshalb einzeln, ausdrücklich auch
> bei angehängtem `Z`.
>
> **Für Schritt 3 war das richtig.** Damals lieferte kein Endpunkt Zeitstempel aus `GlassfishDB`,
> und die Regel schützte vor genau dem Fehler, den sie benennt: einen zonenlosen Wert durch die
> Browserzone zu schicken.
>
> **Seit Schritt 4 ist es falsch.** Der Listen-Endpunkt rechnet die Wanduhrzeit im Backend nach UTC
> um (`common/Zeitpunkte`, [`nachrichtenliste.md`](nachrichtenliste.md) §2). „Wie geliefert" heißt
> ab da: den UTC-Wert ablesen und die Umrechnung unterschlagen — also genau die Verschiebung, die
> vermieden werden sollte. Aus `23:53:50` in der Datenbank wurde `22:53:50Z` auf der Leitung und
> **22:53** in der Anzeige, im Sommer 21:53. Das Altwerkzeug zeigt 23:53.

#### Die Auflösung: UTC auf der Leitung, feste Zone in der Anzeige

Ein Zeitstempel durchläuft drei Stationen und bedeutet an jeder etwas anderes:

| Station | Wert | Was er ist |
|---|---|---|
| `GlassfishDB` | `2025-12-29 23:53:50` | Wanduhrzeit des Altsystem-Servers, ohne Zone |
| API | `2025-12-29T22:53:50Z` | UTC — dort ist der Wert eindeutig |
| Anzeige | `29.12.2025, 23:53` | wieder die Wanduhrzeit |

**UTC bleibt auf der Leitung.** `von` und `bis` müssen einen Punkt auf der Zeitachse benennen, und
das kann nur UTC (Richtlinie §5.3). Daran wird nicht gerührt.

**Das Frontend formatiert in einer festen Zone, nicht in der des Browsers.** Ein Nutzer in München
und einer in Antwerpen sehen dieselbe Uhrzeit, und beide dieselbe wie im Altsystem. Das ist Absicht:
Der Zeitpunkt ist hier eine **Eigenschaft des Belegs**, kein Termin im Kalender des Betrachters.

**Die Zone liefert das Backend** — als `anzeigezone` in der Selbstauskunft (`GET /api/auth/me` und
jede Antwort, die dieselbe Auskunft trägt). Es ist dieselbe Zone, mit der `common/Zeitpunkte` die
Wanduhrzeit nach UTC umrechnet. Eine Konstante im Frontend wäre eine zweite Pflegestelle und liefe
beim Umzug des Servers auseinander — lautlos, weil eine um Stunden verschobene Uhrzeit plausibel
aussieht.

Der Weg durch den Baum: `components/anwendungsrahmen.tsx` hat die Selbstauskunft und füllt
`components/zeitzone.tsx`; jedes Feature liest sie über `useAnzeigezone()`. **Kein Feature importiert
dafür aus `features/sitzung`** — der Kontext ist genau die Naht dazwischen.

**Fehlt die Zone** — etwa solange die Selbstauskunft lädt —, wird in **UTC** formatiert
(`ZEITZONE_RUECKFALL`), niemals in der Zone des Browsers. Ein Rückfall auf den Browser wäre derselbe
Fehler, nur seltener und damit schwerer zu finden: verschoben je nach Standort, ohne dass irgendwo
etwas fehlschlägt. UTC ist für alle gleich und weicht sichtbar ab.

**Ein Wert ohne Zonenversatz wird als UTC gelesen**, nicht als Ortszeit des Browsers.
`new Date("2025-12-29T22:53:50")` täte Letzteres; die API überträgt laut Richtlinie §5.3
ausschließlich UTC.

#### Der Test belegt die ganze Kette

Zwei Tests, dieselben konkreten Werte — läuft eine Seite weg, wird die andere rot:

| Hälfte | Test | Fall |
|---|---|---|
| Wanduhrzeit → UTC | `backend` `ZeitpunkteTest` | `2025-12-29T23:53:50` in `Europe/Berlin` → `2025-12-29T22:53:50Z` |
| UTC → Anzeige | `frontend` `tests/format.test.ts` | `2025-12-29T22:53:50Z` in `Europe/Berlin` → `29.12.2025, 23:53` |

Dazu der Sommerfall (zwei Stunden statt einer) und der Nachweis, dass die Anzeige an der
**gelieferten** Zone hängt und nicht am Standort: derselbe UTC-Wert ergibt in `Europe/Berlin`,
`UTC` und `America/New_York` drei verschiedene Uhrzeiten.

#### Relative Zeit

`formatiereRelativ` liefert „vor 3 Stunden" — als **Ergänzung** zum absoluten Zeitpunkt, nie als
Ersatz. Der absolute Wert ist der, den man gegen das Altwerkzeug hält und in eine Störungsmeldung
schreibt.

Bezugspunkt ist die Uhr des **Browsers**, nicht die Anwendungsuhr des Backends. In Produktion ist
das dasselbe. Im Profil `dev` liegt die Testkopie Monate zurück, und der Tooltip liest sich
entsprechend („vor 7 Monaten") — er sagt dann die Wahrheit über die realen Daten und nicht über die
verstellte Uhr. Der absolute Wert daneben bleibt davon unberührt.

---

## 5. Zwischenspeicher

Serverdaten liegen ausschließlich in TanStack Query. Ein Client, zentral konfiguriert in
`lib/query-client.ts`.

### Kein zweiter Versuch bei 401, 403 und 404 — und keiner bei einer abgebrochenen Suche

```ts
retry: (versuche, fehler) => !istEndgueltig(fehler) && !istZeitgrenze(fehler) && versuche < 1
```

Bei diesen drei Codes stand das Ergebnis schon beim ersten Aufruf fest. Ohne diese Regel wartet der
Nutzer mehrere Sekunden auf eine Meldung, die sich nicht mehr ändern kann. Mutationen wiederholen
grundsätzlich nicht.

**Der vierte Fall ist seit dem Nachtrag zu Schritt 4 dabei und hat einen anderen Grund** (ergänzt
06.08.2026): `suche-abgebrochen` heißt, dass ein Statement in die Zeitgrenze der Datenbank gelaufen
ist. Dort bringt der zweite Versuch nicht nur nichts, er **schadet** — er stellt dieselbe Abfrage
noch einmal und läuft wieder in dieselbe Grenze. Erkannt wird er am `type` und nicht am Statuscode;
`400` als Ganzes wird weiterhin wiederholt, denn dort ist der zweite Versuch billig.

### Beim Mandantenwechsel wird geleert, nicht invalidiert

`queryClient.clear()`, nicht `invalidateQueries`.

Der Unterschied ist der ganze Punkt: `invalidateQueries` markiert Daten nur als veraltet und **zeigt
sie weiter an**, bis die neue Antwort da ist. Nach dem Umschalten stünden für einen Moment die Daten
des vorherigen Mandanten auf dem Bildschirm — obwohl das Backend sauber ist. Bei einem Werkzeug,
dessen Kernversprechen die Trennung ist, wäre das der peinlichste denkbare Fehler.

Geleert wird **vor** dem Weitergehen. `lib/zwischenspeicher.ts` macht die Reihenfolge zu einer
eigenen Funktion, damit sie prüfbar ist; `tests/zwischenspeicher.test.ts` weist sie nach.

Beim **Abmelden** dasselbe, zusätzlich mit harter Navigation — sie wirft auch den Zustand im Speicher
weg, den ein Router-Wechsel stehen ließe. Und sie läuft in `onSettled`, nicht in `onSuccess`: Der
Nutzer wollte gehen, auch wenn der Aufruf fehlschlug.

### Bei 401 wird umgeleitet, nicht gemeldet

Eine `401`-Antwort heißt „die Sitzung ist weg". Die einzige sinnvolle Reaktion ist die Anmeldung, und
zwar ohne Fehlermeldung. `lib/query-client.ts` ist die einzige Stelle, die das tut — für Abfragen
und Mutationen gleichermaßen. Auf der Anmeldeseite selbst greift sie nicht: Dort ist ein `401` die
abgelehnte Anmeldung und gehört angezeigt.

### Vier Zustände je Ansicht

Laden, Leer, Fehler, Daten — `components/zustand.tsx`. „Leer" ist kein Fehler und sieht auch nicht so
aus; wer bei jedem leeren Zeitfenster eine rote Meldung sieht, hört auf, rote Meldungen ernst zu
nehmen. Bei „Leer" wird gesagt, woran es liegen kann. Das gilt auch für die bewusst leere Startseite.

---

## 6. Fehler: von `type` zur Übersetzung

Antworten im Format RFC 9457 werden in `lib/http.ts` zu einem `ProblemFehler` gelesen: `status`,
`typ`, `titel`, `detail`, `traceId` und die Feldfehler aus `errors`.

**Übersetzt wird anhand des maschinenlesbaren `type`**, nicht anhand von `detail`:

```
https://overlord.kraftwerkone.de/probleme/konto-gesperrt
                                          └── Schlüssel in texte.fehler
```

`lib/fehlertext.ts` schlägt `texte.fehler[typ]` nach. Fehlt eine Übersetzung, ist `detail` die
**Rückfallebene** — lieber ein richtiger Satz in der falschen Sprache als „unbekannter Fehler". Die
Fehler-Kennung (`traceId`) wird nur bei technischen Fehlern gezeigt; bei „Passwort falsch" wäre sie
Rauschen.

Der Text aus dem Backend ist deutsch und für den Nutzer lesbar — aber eben deutsch. Ohne einen
Schlüssel müsste die Oberfläche Texte vergleichen, und die ändern sich.

### Eigene Felder neben `type`, `title` und `detail`

RFC 9457 lässt eigene Felder im Rumpf ausdrücklich zu. `ProblemFehler` hebt sie als `angaben` auf —
alles, was nicht zu den Feldern des Formats selbst gehört — und `zahl(name)` liest eine Zahl daraus
heraus, oder `undefined`, wenn sie fehlt oder keine ist.

Gebraucht wird das seit Schritt 4, Nachtrag, von genau einem Fall: `suche-fenster-zu-gross` bringt
`grenzeTage` und `angefragtTage` mit. **Der Sinn ist, die Zahl nicht zweimal zu pflegen.** Die
Grenze gehört dorthin, wo sie gemessen wurde — ins Backend; stünde sie auch in einem Sprachtext,
liefe eine der beiden der anderen irgendwann hinterher, und weil beide plausibel aussehen, fiele es
niemandem auf. Fehlt eine der Zahlen, greift der allgemeine Satz aus dem Fehlerkatalog statt einer
Meldung mit einer Lücke darin.

### Die beiden Problemtypen der Suche

Beide entstanden im Nachtrag zu Schritt 4 und beide gehören an das **Suchfeld**, nicht über die
Ansicht ([`nachrichtenliste.md`](nachrichtenliste.md) §5 und §8.2):

| `type` | Was er heißt | Was die Oberfläche tut |
|---|---|---|
| `suche-fenster-zu-gross` | Bei gesetztem Suchbegriff ist das Zeitfenster begrenzt; die Anfrage liegt darüber | Hinweis am Feld mit beiden Zahlen aus der Antwort, dazu **„Trotzdem suchen"** — die Schaltfläche setzt `langeSuche` in der URL. Die Liste bleibt stehen. |
| `suche-abgebrochen` | Das Statement ist in `max_statement_time` gelaufen | Hinweis am Feld: Zeitraum verkleinern oder Begriff schärfen. **Kein** zweiter Versuch — siehe unten. |

**`suche-abgebrochen` ist der eine Fall, in dem `lib/query-client.ts` nicht wiederholt.** Sonst
bleibt es bei einem Wiederholungsversuch für alles außer `401`, `403` und `404`. Hier ändert der
zweite Versuch das Ergebnis nicht, er kostet es noch einmal: dieselbe Abfrage, dieselbe Zeitgrenze,
zehn weitere Sekunden auf der Produktionsdatenbank. Erkannt wird der Fall über `istZeitgrenze` in
`lib/http.ts` — am `type` und nicht am Statuscode, denn `400` als Ganzes wird weiterhin wiederholt.

### Die eine Ergänzung am Backend

Die Fehlerantworten aus Teil 1 trugen fast alle einen Typ. Nicht getroffen waren die Fälle, die
`ResponseEntityExceptionHandler` selbst beantwortet — unlesbares JSON, falsche HTTP-Methode,
unbekannter Pfad. Sie trugen `about:blank`.

`GlobalExceptionHandler.handleExceptionInternal` setzt jetzt einen Rückfalltyp:

| Status | Typ |
|---|---|
| `404` | `nicht-gefunden` — **derselbe** wie bei `RessourceNichtGefundenException` |
| `5xx` | `technischer-fehler` |
| sonst | `anfrage-ungueltig` |

Bewusst eine grobe Zuordnung nach Statuscode und keine Liste je Ausnahmetyp: Diese Antworten sind
Randfälle, die kein Nutzer im Normalbetrieb sieht. Dass `404` denselben Schlüssel bekommt, ist
dagegen keine Bequemlichkeit — ein unbekannter Pfad und eine fremde Ressource sollen sich auch hier
nicht unterscheiden.

Nachgemessen am 29.07.2026:

```
POST /api/auth/mandant  {"mandantId":"GIBTESNICHT"}
  → 404  type=…/nicht-gefunden

GET  /api/gibtesnicht
  → 404  type=…/nicht-gefunden      ← ununterscheidbarer Typ

POST /api/auth/me
  → 405  type=…/anfrage-ungueltig
```

Der englische `detail`-Text der zweiten Antwort („No static resource …") erreicht den Nutzer nie: Der
Typ ist übersetzt, die Rückfallebene greift nicht.

### Ein 404 sagt niemals etwas über Berechtigung

„Das Gesuchte gibt es nicht." / „What you are looking for does not exist."

Kein „kein Zugriff", kein „nicht berechtigt". Das Backend verbirgt sorgfältig, ob eine Ressource
nicht existiert oder einem fremden Mandanten gehört — beides liefert denselben Statuscode und
denselben Rumpf. Ein Wort wie „berechtigt" in der Oberfläche hebelte genau das aus.

`tests/sprachdateien.test.ts` prüft beide Sprachen gegen eine Wortliste und blockiert den Build,
sobald jemand den Text „hilfreicher" macht.

### Meldungen bei der Anmeldung

Unspezifisch, wie im Backend: Ob der Benutzername unbekannt oder das Passwort falsch war, steht
nirgends. Die Ausnahme aus Teil 1 — gesperrtes oder deaktiviertes Konto bei **korrektem** Passwort —
kommt als eigener Problemtyp (`konto-gesperrt`, `konto-deaktiviert`) und wird angezeigt.

---

## 7. Der Rahmen steht, nur der Inhalt scrollt

`components/anwendungsrahmen.tsx` ist so gebaut:

```
div            relative  h-dvh  flex-col  overflow-hidden     ← genau eine Fensterhöhe
├─ header      shrink-0                                       ← steht
└─ div         flex  flex-1  min-h-0
   ├─ aside    relative  w-navspalte  shrink-0  overflow-y-auto   ← steht, scrollt notfalls selbst
   └─ main     relative  flex-1  min-h-0  min-w-0  overflow-y-auto ← der einzige Scrollbereich
```

### Die drei Bedingungen

**1. `min-h-0` auf jedem Flex-Kind im Pfad.** Ein Flex-Kind bekommt implizit `min-height: auto` und
wächst damit über seinen Container hinaus, statt zu scrollen. Ohne diese Klasse dehnt sich der
Inhaltsbereich unter das Fenster, das Dokument bekommt eine zweite Bildlaufleiste, und die
Kopfzeile wandert beim Scrollen weg. Das ist die häufigste Ursache für eine doppelte
Bildlaufleiste und der Grund, warum sie hier zweimal steht.

**2. Genau ein Element mit `overflow-y-auto`**, und unterhalb davon nichts, das seine Höhe an der
Fensterhöhe bemisst (kein zweites `h-dvh`, kein `min-h-screen`, kein `h-full` in einem bereits
begrenzten Bereich).

**3. Jeder Scrollbereich ist zugleich Bezugspunkt — `relative`.** Nachgetragen am **06.08.2026**,
nach einem Fehler, den die ersten beiden Bedingungen nicht abgedeckt haben und auch nicht abdecken
konnten.

> **Ein absolut positioniertes Element ohne positionierten Vorfahren hängt am Ursprungsblock der
> Seite. `overflow-hidden` weiter oben beschneidet es deshalb nicht — sein Platz zählt zur
> Scrollfläche des Dokuments.**

Das klingt nach einem Randfall und ist keiner: **Tailwinds `sr-only` ist `position: absolute`.**
Jede verborgene Beschriftung für Vorleseprogramme ist also ein solches Element, und in einer langen
Liste sitzt sie weit unten.

Gemessen an der Nachrichtenliste, 1920 × 889, 50 Zeilen (`nexans1`, Standardfenster):

| | vorher | nachher |
|---|---|---|
| `documentElement.scrollHeight` | **2.243** | 889 |
| `documentElement.clientHeight` | 889 | 889 |
| erreichbares `window.scrollY` | **1.354** | **0** |
| tiefstes Element ohne positionierten Vorfahren | `sr-only` im Aktualisieren-Knopf, `docBottom = 2.243` | — |
| `main.scrollTop` am Listenende | 1.386 von 1.386 | 1.386 von 1.386 |

Der Befund entstand nicht durch Hinsehen, sondern durch Ausschluss: `overflow-x: hidden` an `html`
(das `overflow-y` auf `auto` hochstuft) war die naheliegende Vermutung und ist **widerlegt** — mit
`overflow-y: hidden` an `html` blieben dieselben 1.354 px erreichbar. Erst das Ausblenden einzelner
Teilbäume zeigte den Verursacher: Der Blätter-Block ist 32 px hoch, und ihn auszublenden nahm dem
Dokument 1.354 px Scrollfläche. Vier von 104 `sr-only`-Elementen im Inhaltsbereich hatten keinen
positionierten Vorfahren; das tiefste lag exakt auf der beobachteten Scrollhöhe.

**Was der Nutzer davon sah:** Unter der Fußzeile der Tabelle folgten mehrere hundert Pixel Leere.
Wer dort hineinscrollte, schob den gesamten Anwendungsrahmen — Kopfzeile, Navigation, Liste — aus
dem Bild und sah eine weiße Fläche. Beim nächsten Rendern rechnete der Browser die Scrollfläche neu
und klemmte die Position zurück: das gemeldete „Zurückspringen".

**Die Regel, die daraus folgt:** *Ein Bereich, der scrollt oder beschneidet, ist `relative`.* Sonst
beschneidet er nur, was er zufällig als Nachfahren im Fluss hat — und nicht das, was danebensteht.

**Warum jetzt und nicht in Schritt 4:** Dort braucht die Nachrichtenliste eine feststehende
Tabellenkopfzeile über einem scrollenden Bereich. Ein Rahmen, der das nicht hergibt, wird dann
mitten in einer Listenansicht umgebaut — und zwar von jemandem, der eigentlich eine Liste bauen
wollte.

**Die Seite selbst scrollt nie.** Nachgemessen gegen den fertigen Build, 1920 × 1080, mit 4000 px
Inhalt im Inhaltsbereich:

```
documentElement.scrollHeight   1080   = clientHeight  → keine Bildlaufleiste am Dokument
main.scrollHeight            > clientHeight           → der Inhaltsbereich scrollt
Kopfzeile links 0, Breite 1920; Navigationsspalte links 0; Inhalt rechts 0
```

Bei 360 px: `scrollWidth` 360, kein horizontales Scrollen, Navigationsspalte und Suchplatz
entfallen, der aktive Mandant bleibt.

**Eine Maximalbreite gehört nicht in den Rahmen**, nur in eine Ansicht mit Fließtext — die
Begründung steht in [`visuelles-konzept.md`](visuelles-konzept.md) §5.

---

## 8. Aufteilung des Codes

```
src/
├─ app/                    Routen. Server-Komponenten, soweit möglich
│  ├─ (public)/anmeldung/
│  └─ (app)/               Anwendungsrahmen: /, /passwort, /mandantenauswahl, /nachrichten, …
├─ components/             Zusammensetzung: Rahmen, Kopfzeile, Navigation, Zustände, Zeitzone
│  └─ ui/                  shadcn/ui — Generatorbereich, nicht von Hand ändern
├─ features/
│  ├─ sitzung/             Anmeldung, Sitzung, Passwort, Mandantenwahl
│  └─ nachrichten/         Liste, Filter, Blättern (Schritt 4)
├─ i18n/                   Sprachdateien und Kontext
├─ lib/                    Infrastruktur: http, query-client, ablauf, routen, format, filter, …
└─ proxy.ts                Routensperre
```

**Warum Sitzung und Mandant ein Feature sind:** Im Backend liegt beides im Paket `security`, und die
Selbstauskunft bringt den aktiven Mandanten mit. Zwei Features müssten sich genau diesen Typ teilen —
und ein Feature importiert nicht aus einem Nachbarfeature.

**`features/nachrichten` importiert nicht aus `features/sitzung`.** Zwei Dinge braucht es trotzdem
von dort, und beide gehen über die Naht in `components/`:

| Was | Weg |
|---|---|
| die Anzeigezone aus der Selbstauskunft | `components/zeitzone.tsx` — der Rahmen füllt sie, jedes Feature liest sie |
| der Zustand der Sitzung selbst | gar nicht — der Rahmen entscheidet, was überhaupt gerendert wird |

Aus demselben Grund liegt der Aufruf von `/api/prozesse` in `features/nachrichten` und nicht in einem
eigenen Feature `prozesse`: Es entstünde allein für einen Fetch und müsste sofort von `nachrichten`
importiert werden. Kommt in Schritt 10 eine eigene Prozessansicht, wandert der gemeinsame Teil nach
`components/` oder `lib/` — nicht ins Nachbarfeature.

`"use client"` steht so weit unten wie möglich. Server-Komponenten sind: Wurzel-Layout, alle
`page.tsx`, `seiten-platzhalter.tsx`. Client sind: alles mit Zustand, Interaktion oder TanStack
Query. Auch `features/nachrichten/filter.ts` ist bewusst **frei von React** — die Umrechnung Zustand
→ Anfrage ist eine reine Funktion und wird als solche geprüft.

### Filterzustand

`lib/filter.ts` hält die nuqs-Abstraktion für das **Zeitfenster** — den einen Filter, den jeder
Listen-Endpunkt hat. Was nur die Nachrichtenliste betrifft (Status, Prozess, Suche,
Zwischenschritte, Sortierung), liegt im Feature; `lib` ist Infrastruktur, nie Fachlichkeit.

Entstanden in Schritt 3 ohne Wirkung, damit Schritt 4 nicht anfängt, Zeitfenster in
Komponentenzustand zu legen und später umzubauen. Seit Schritt 4 ist es der Filter der
Nachrichtenliste ([`nachrichtenliste.md`](nachrichtenliste.md) §8.2).

Bewusst **ohne** Standardwert: Fehlt das Zeitfenster, setzt das Backend den Standard aus Regel L1
(24 Stunden). Ein zweiter Standardwert im Frontend liefe dem ersten irgendwann hinterher.

> **Die eine Ausnahme, und warum sie keine ist.** `zwischenschritte` steht ausdrücklich in der URL,
> ab dem ersten Rendern und auch dann, wenn es der Vorgabe entspricht — dafür trägt der Parser
> `clearOnDefault: false`, sonst entfernte `nuqs` ihn wieder. Der Unterschied zum Zeitfenster:
> Dort wird ein Standard *gesetzt*, hier wird ein Drittel aller Zeilen *weggelassen*. Was man sieht,
> muss man teilen können; ohne den Parameter sähe der Empfänger eines Links dieselbe Ansicht mit
> anderen Zeilen.

---

## 9. Tests

`pnpm test` (Vitest, in `pnpm build` verankert). Bewusst klein: kein jsdom, keine Testing Library,
kein React-Plugin. Geprüft werden die **Entscheidungen**, nicht das Markup — das sind alles reine
Funktionen, und ein gerenderter Baum brächte hier nichts außer Laufzeit und Abhängigkeiten.

| Datei | Was |
|---|---|
| `ablauf.test.ts` | Änderungszwang vor Mandantenauswahl vor Startseite |
| `sprachdateien.test.ts` | gleicher Schlüsselsatz; 404-Wortwahl; Rückfall auf `detail` |
| `farbwerte.test.ts` | kein Hex-Wert, keine Tailwind-Farbklasse in einer Komponente |
| `zwischenspeicher.test.ts` | geleert **vor** dem Weitergehen, bei Wechsel und Abmeldung; das Ziel nach dem Mandantenwechsel trägt keine Filter |
| `format.test.ts` | UTC → Anzeige in der gelieferten Zone; Rückfall auf UTC statt auf den Browser; relative Zeit; Wanduhrzeit der Eingabefelder, auch am Umstellungstag |
| `routen.test.ts` | `weiter` als offene Weiterleitung ausgeschlossen |
| `nachrichtenfilter.test.ts` | URL → Zustand → URL; unbekannte Werte werden übergangen; **der Cursor taucht in keiner erzeugten URL auf**; die beiden Zeitfenstermodi schließen einander aus; `langeSuche` steht in der URL und wird nur mit dem Suchbegriff geschickt; welche Problemtypen an das Suchfeld gehören und welche über die Ansicht |

---

## 10. Offene Punkte

- **Der Änderungszwang-Pfad ist in der Oberfläche nicht end-to-end durchgeklickt.** Die Verzweigung
  ist unit-getestet und der Interceptor im Backend durch Teil 1 abgedeckt; die Abnahme durch die
  Oberfläche braucht ein frisch angelegtes Konto und wurde bewusst nicht gegen die geteilte
  Testkopie ausgeführt.
- **`components/ui/sheet.tsx` enthält eine feste Zeichenkette** („Close" für den Schließen-Knopf) aus
  dem Generator. Generatorbereich; wenn sie stört, wird die Komponente umschlossen, nicht geändert.
- **Kein Dunkelmodus, keine Barrierefreiheit über die Grundlagen hinaus** — bewusst außerhalb dieses
  Schritts.
- **Die Sprachwahl liegt nur im Cookie**, also je Gerät und Browser.
