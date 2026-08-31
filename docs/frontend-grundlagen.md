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

### Der Administrationsbereich *(21.08.2026, umbenannt 24.08.2026)*

**Ein Navigationseintrag, zwei Unterseiten.** Die Benutzerverwaltung (Schritt 9a) und die
Katalogpflege (Schritt 9b) sind zwei Seiten desselben Bereichs und **kein zweiter Menüpunkt**:

| | |
|---|---|
| Navigationseintrag | **einer**, `nurAdmin: true` (`lib/navigation.ts`) |
| Basisroute | **`/administration`** (`lib/routen.ts`), seit Schritt 4 vergeben |
| Benutzerverwaltung | `/administration/benutzer` |
| Katalogpflege | `/administration/katalog` |

> **Zur Route — entschieden am 24.08.2026, der alte Wortlaut bleibt stehen.** Hier stand:
> *„Der Auftrag vom 21.08.2026 nennt `/verwaltung/benutzer` und `/verwaltung/katalog`. Hier steht
> `/administration/…`, weil der Pfad im Code bereits dreifach vergeben ist — `ROUTEN.administration`,
> der Navigationseintrag und die Platzhalterseite unter `app/(app)/administration/`. Eine Umbenennung
> ist möglich, aber sie ist eine **eigene** Entscheidung mit eigenem Aufwand (Route, Navigation,
> Sprachschlüssel `navigation.eintraege`, bestehende Verweise) und nicht der Nebeneffekt eines
> Backend-Auftrags. **Gemeldet, nicht stillschweigend aufgelöst.**"*
>
> **Der Auftrag zu Schritt 9b, Teil Frontend hat sie getroffen: Beschriftung und Route heißen beide
> *Administration*** — ein Wort, das der Nutzer liest und das in der Adresszeile steht. Damit ist
> `/verwaltung/…` erledigt und die Umbenennung findet nicht statt: Es gibt nichts umzubenennen.
> Die Überschrift dieses Abschnitts lautete bis dahin „Der **Verwaltungs**bereich".
>
> **Zwei Feststellungen dazu, weil der Auftrag sie anders annimmt** — er trägt auf, „den erfundenen
> Pfad in `docs/frontend-grundlagen.md` und in die E-Texte" zu korrigieren:
>
> - **`/verwaltung` steht im ganzen Repository an genau einer Stelle**, nämlich in dem oben
>   zitierten alten Wortlaut — also dort, wo er *gemeldet* und nicht behauptet wird. Eine
>   Code-Fundstelle gibt es nicht.
> - **Die E-Texte tragen ihn nicht.** [`prozess-katalog.md`](prozess-katalog.md) nennt den Pfad in
>   keiner seiner einundzwanzig Entscheidungen; §9 dort und §9, Abweichung 4 in
>   [`prozess-katalog-backend.md`](prozess-katalog-backend.md) sprechen vom
>   *Administrationsbereich* als Ort, nie von einer Route.
>
> Zu korrigieren war deshalb die **Benennung**, nicht ein falscher Pfad.

**Der Menüpunkt ist ausgeblendet, und das ist Bequemlichkeit — genau wie die Routensperre darüber.**
`sichtbareNavigation(rolle)` filtert den Eintrag heraus, solange die Rolle nicht `ADMIN` ist. Das
schützt nichts: Wer den Pfad kennt, ruft ihn auf. **Verbindlich prüft das Backend**, das für
`/api/admin/**` und `/api/katalog/**` die Rolle `ADMIN` verlangt.

**Die Rolle kommt aus der Selbstauskunft, nicht aus `proxy.ts`.** `GET /api/auth/me` liefert `role`;
der Anwendungsrahmen reicht sie an die Navigation weiter. `proxy.ts` **kennt die Rolle nicht und
soll sie nicht kennen** — sie steht nicht im Cookie, und der Versuch, sie dort zu beschaffen, legte
die Berechtigungsentscheidung in den Browser (§2 oben).

**Daraus folgt ein Zustand, den es zu bauen gilt:** Ein Nutzer mit der Rolle `MANDANT`, der
`/administration` von Hand aufruft, kommt durch die Routensperre — sie sieht nur das Cookie — und
bekommt vom Backend ein `403`. Die Seite braucht dafür einen **sauberen eigenen Zustand** und darf
weder leer bleiben noch in den Ladezustand hängen.

> **Beim Wortlaut ist eine Falle zu umgehen.** `tests/sprachdateien.test.ts` prüft eine Wortliste
> — `berecht`, `zugriff`, `erlaub`, `gesperrt`, `access`, `denied`, `forbidden` und weitere — und
> bricht den Build, sobald eines davon vorkommt. **Sie prüft aber genau einen Schlüssel:**
> `fehler["nicht-gefunden"]`, den 404-Text. Der Grund steht in §6: Ein 404 sagt niemals etwas über
> Berechtigung, weil das Backend „existiert nicht" und „gehört einem fremden Mandanten"
> ununterscheidbar hält.
>
> **Für ein echtes `403` gilt das nicht.** Dort *ist* fehlende Berechtigung die Wahrheit, und sie
> darf benannt werden. Der Text bekommt einen **eigenen** Schlüssel und wird nicht aus dem
> 404-Text abgeleitet — sonst fällt entweder die Prüfung oder die Aussage.

**Was hier bewusst nicht steht:** wie die beiden Seiten innen aussehen. Das gehört in die
Feature-Dateien ([`benutzerverwaltung.md`](benutzerverwaltung.md) §7a,
[`prozess-katalog.md`](prozess-katalog.md) §4) und in die jeweiligen Frontend-Aufträge.

> **Stand 24.08.2026.** Hier stand „**gebaut ist bislang nur der Platzhalter** unter
> `app/(app)/administration/page.tsx`". Seit Schritt 9b, Teil Frontend steht der Bereich:
>
> | | |
> |---|---|
> | `app/(app)/administration/layout.tsx` | die Unternavigation, auf allen drei Seiten |
> | `app/(app)/administration/page.tsx` | die Übersicht mit beiden Bereichen |
> | `app/(app)/administration/katalog/page.tsx` | die Pflegeliste ([`prozess-katalog-frontend.md`](prozess-katalog-frontend.md)) |
> | `app/(app)/administration/benutzer/page.tsx` | ~~weiterhin der Platzhalter — Inhalt im 9a-Auftrag~~ · **seit dem 24.08.2026 die Ansicht selbst** ([`benutzerverwaltung-frontend.md`](benutzerverwaltung-frontend.md)) |
>
> **Der Zustand „kein Zugriff" ist gebaut, und zwar an genau einer Stelle:** an der
> Katalogpflege, aus dem `403` ihres Endpunkts. `components/kein-zugriff.tsx` trägt ihn;
> erkannt wird er über `istKeinZugriff` in `lib/http.ts` — **am Problemtyp und nicht am
> Statuscode**, denn `403` ist in diesem Backend dreifach vergeben
> (`zugriff-verweigert`, `csrf-token-ungueltig`, `kein-mandant-gewaehlt`) und die drei
> bedeuten Verschiedenes.
>
> **Die Übersicht prüft die Rolle nicht.** Sie ruft kein Backend auf und könnte es deshalb
> gar nicht belegen; eine Prüfung gegen `role` aus der Selbstauskunft wäre eine
> Berechtigungsentscheidung im Browser — dieselbe Grenze, die §2 oben für `proxy.ts` zieht.
> Wer die Adresse ohne die Rolle eintippt, sieht zwei Verweise und bekommt die Auskunft
> dort, wo sie belegt ist.

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

**`suche-abgebrochen` war der erste Fall, in dem `lib/query-client.ts` nicht wiederholt.** Sonst
bleibt es bei einem Wiederholungsversuch für alles außer `401`, `403` und `404`. Hier ändert der
zweite Versuch das Ergebnis nicht, er kostet es noch einmal: dieselbe Abfrage, dieselbe Zeitgrenze,
zehn weitere Sekunden auf der Produktionsdatenbank. Erkannt wird der Fall über `istZeitgrenze` in
`lib/http.ts` — am `type` und nicht am Statuscode, denn `400` als Ganzes wird weiterhin wiederholt.

> **Seit dem 14.08.2026 sind es zwei.** `praefixsuche-fenster-zu-gross` — die Suche über den
> **Anfang** einer Belegnummer ist auf dreißig Tage gedeckelt
> ([`bam-suche.md`](bam-suche.md) §18) — wird ebenfalls nicht wiederholt, erkannt über
> `istPraefixfensterZuGross` und ebenfalls am `type`.
>
> **Der Grund ist derselbe, der Preis ein anderer.** Bei der Zeitgrenze kostet der zweite Versuch
> zehn Sekunden auf der Datenbank; hier kostet er nichts, weil der Fehler schon an der
> **Parameterform** feststeht — dasselbe Fenster, derselbe Modus, dieselbe Antwort. Verzögert wird
> trotzdem etwas, und zwar das Einzige, was hilft: die Meldung, die dem Nutzer den Ausweg nennt.
>
> **Der Nachbarfall `suche-fenster-zu-gross` ist bewusst nicht mitgeändert.** Für ihn gilt dasselbe
> Argument, er ist aber älter und hat mit „Trotzdem suchen" seinen eigenen Weg heraus; er steht als
> offener Punkt in [`bam-suche.md`](bam-suche.md) §27.

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

#### Ein Baustein des Generators ist client-only, ohne es zu sagen *(24.08.2026)*

> **`components/ui/button.tsx` trägt kein `"use client"` und lässt sich trotzdem nicht in einer
> Server-Komponente verwenden.** Es importiert `Slot` aus dem Sammelpaket `radix-ui`, und dessen
> Auswertung ruft `createContext`. Der Fehler fällt **beim Importieren**, nicht beim Rendern:
>
> ```
> Runtime TypeError
> createContext only works in Client Components.
>   src/components/ui/button.tsx (3:1)  @ module evaluation
> ```

**Gefunden beim Aufrufen von `/administration`, nicht beim Bauen.** Die Übersichtsseite ist eine
`page.tsx` und damit Server-Komponente; sie benutzte `Button asChild` um einen `Link`. Alle übrigen
dreiundzwanzig Verwender von `Button` im Projekt sind Client-Komponenten — deshalb war die Falle
seit Schritt 3 unsichtbar und schnappte beim allerersten Server-Verwender sofort zu.

**Kein bestehender Prüfschritt konnte das finden, und keiner hätte es können:**

| | |
|---|---|
| `pnpm check` | rendert **keine einzige Seite**. Lint, Typprüfung, Format und Vitest sagen nichts über die Grenze |
| die rendernden Tests | laufen in `jsdom` — dort ist alles Client |
| `next build` | prerendert diese Route nicht: `generateMetadata` liest die Sprache aus dem Cookie, die Route ist dynamisch |

**Seit dem 24.08.2026 gibt es dafür ein Netz:** `tests/serverbausteine.test.ts` berechnet die Liste
der serverunsicheren Bausteine aus den Dateien — `radix-ui` importiert, `"use client"` fehlt — und
weist nach, dass keine Server-Komponente einen davon importiert. Berechnet und nicht geschrieben,
weil `components/ui` Generatorbereich ist: Welche Datei die Auszeichnung trägt, entscheidet
`shadcn add`. Das Netz ist gerissen worden, bevor es gezählt hat.

**Was der Test ausdrücklich *nicht* verbietet:** dass eine Server-Komponente eine
**Client**-Komponente importiert. Das ist der Normalfall und die Naht selbst —
`seiten-platzhalter.tsx` ist Server und rendert `Leer`. Geprüft wird allein der Sonderfall des
**falsch ausgezeichneten** Moduls: eines, das sich wie eine Server-Komponente verhält und keine ist.

**Die Auflösung ist nicht `"use client"` an der Seite.** Jede `page.tsx` ist Server-Komponente, und
eine Liste ohne jedes Verhalten ist der schlechteste denkbare Anlass, diese Grenze zu verschieben.
Die Übersicht baut ihre Verweise stattdessen selbst — es ist ohnehin eine Liste von Links und keine
von Schaltflächen.

### Filterzustand

`lib/filter.ts` hält die nuqs-Abstraktion für das **Zeitfenster** — den einen Filter, den jeder
Listen-Endpunkt hat. Was nur die Nachrichtenliste betrifft (Status, Prozess, Suche,
Sortierung), liegt im Feature; `lib` ist Infrastruktur, nie Fachlichkeit.

Entstanden in Schritt 3 ohne Wirkung, damit Schritt 4 nicht anfängt, Zeitfenster in
Komponentenzustand zu legen und später umzubauen. Seit Schritt 4 ist es der Filter der
Nachrichtenliste ([`nachrichtenliste.md`](nachrichtenliste.md) §8.2).

Bewusst **ohne** Standardwert: Fehlt das Zeitfenster, setzt das Backend den Standard aus Regel L1
(24 Stunden). Ein zweiter Standardwert im Frontend liefe dem ersten irgendwann hinterher.

> ~~**Die eine Ausnahme, und warum sie keine ist.**~~ **Gegenstandslos seit dem 11.08.2026.** Die
> Ausnahme war `zwischenschritte`: Der Parameter stand ausdrücklich in der URL, ab dem ersten
> Rendern und auch dann, wenn er der Vorgabe entsprach — dafür trug der Parser `clearOnDefault:
> false`, sonst hätte `nuqs` ihn wieder entfernt. Der Unterschied zum Zeitfenster: Dort wird ein
> Standard *gesetzt*, dort wurde ein Drittel aller Zeilen *weggelassen*, und was man sieht, muss man
> teilen können.
>
> **Die Regel dahinter bleibt und ist die eigentliche Auskunft dieses Absatzes:** *Ein Standardwert,
> der etwas weglässt, gehört in die URL; einer, der etwas setzt, nicht.* Der Fall, an dem sie
> entstanden ist, gibt es nicht mehr — der Ausblende-Schalter ist entfallen
> ([`nachrichtenliste.md`](nachrichtenliste.md) §5), die Liste lässt nichts mehr weg, und ohne
> Auswahl ist die URL leer.

#### Die zweite Regel: was die URL nicht ausdrücken kann, ist kein Filterzustand

Nachgetragen am **07.08.2026**, neben der Entscheidung gegen einen zweiten Standardwert und aus
demselben Anlass — einem Zustand, den es gab, aber nicht geben konnte.

> **Ist die URL die alleinige Quelle des Filterzustands, kann kein Zustand existieren, den die URL
> nicht ausdrücken kann.**

**Der Anlass.** „Freies Zeitfenster, noch ohne Zeitpunkte" war von „keine Auswahl" nicht zu
unterscheiden: Beides ist `zeitraum=null, von=null, bis=null`, derselbe Nullzustand. Der Klick auf
„Frei" schrieb also einen Zustand, der sich vom vorherigen nicht unterschied — die Eingabefelder
erschienen nie, und **der Modus war über die Oberfläche gar nicht erreichbar**. Hinein kam nur, wer
sich eine URL von Hand baute. Gefunden in der Sichtprüfung am 06.08.2026.

**Was daraus folgt, ist nicht „mehr in die URL".** Die Auflösung war die umgekehrte: Der
Zwischenzustand ist *kein Filterzustand* — er beschreibt keinen Ausschnitt, sondern eine begonnene
Eingabe, und beide Ausschnitte sind identisch. Er gehört deshalb in den Komponentenzustand, und die
Regel, die ihn ableitet, in eine prüfbare reine Funktion (`lib/filter.ts` `angezeigterModus`).
Dasselbe gilt für „halb getipptes `datetime-local`" (07.08.2026,
[`nachrichtenliste.md`](nachrichtenliste.md) §8.2): auch das eine halbe Eingabe und keine Auswahl.

**Die Prüfung, die vor dem Bau steht**, ist deshalb ein Zweischritt:

1. **Drückt die URL diesen Zustand aus?** Wenn ja: hinein damit, fertig.
2. **Wenn nein — ist er wirklich ein Filterzustand?**
   - *Nein* (er beschreibt eine Eingabe, keinen Ausschnitt): Komponentenzustand, und die Ableitung
     kommt als reine Funktion neben den Filter, nicht als Bedingung in eine Komponente.
   - *Ja* (er zeigt einen anderen Ausschnitt): Dann fehlt der URL ein Parameter — und das ist ein
     Befund, keine Geschmacksfrage.

**Wer das übergeht, merkt es nicht am Code, sondern erst beim Durchklicken** — und auch dann nur,
wenn niemand die URL von Hand baut. Genau das ist zweimal passiert.

**Schritt 7 und Schritt 9 bekommen dieselbe Bauform.** Die BAM-Suche und der Katalog haben beide
einen Modus, der leer beginnt; die Frage stellt sich dort unverändert.

#### Die dritte Regel: nicht jeder URL-Parameter ist ein Anfrageparameter

Nachgetragen am **07.08.2026** (Schritt 5, Teil 2), als der erste Parameter dazukam, der in der URL
steht und in keiner Abfrage vorkommt.

> **Was in der URL steht, beschreibt die Ansicht. Was in der Abfrage steht, beschreibt die Frage an
> das Backend. Das ist nicht dieselbe Menge.**

**Der Anlass.** `nachricht` — die geöffnete Detailansicht — gehört in die URL: Was man sieht, muss
man teilen können, und „schick mir mal den Link" ist bei diesem Werkzeug die eigentliche Anwendung.
An `/api/nachrichten` gehört er trotzdem nicht: Der Listen-Endpunkt kennt ihn nicht, und träte er
in den Abfrageschlüssel des Zwischenspeichers ein, lüde **jeder Klick auf eine Zeile die ganze Liste
neu** und setzte die Seitenposition zurück — für eine Ansicht, die ihre Daten ohnehin selbst holt.

Umgesetzt ist das als zwei getrennte Funktionen mit zwei Zwecken (`features/nachrichten/filter.ts`):
`alsSuchparameter` baut die **URL**, `alsAbfrage` die **Anfrage**. Der Test hält beides fest,
einschließlich der Probe, dass die Abfrage mit und ohne geöffnetes Panel Zeichen für Zeichen
dieselbe ist.

#### `history`: „replace" für Filter, „push" für Ansichten

Ebenfalls aus Schritt 5, Teil 2. `useQueryStates` bekommt weiterhin `history: "replace"` — ein
Filter, den man verstellt, ist keine Station, zu der man zurückgeht, und ein Verlaufseintrag je
Tastendruck im Suchfeld machte den Zurück-Knopf unbrauchbar.

**Ein Parameter, der eine Ansicht *öffnet*, ist etwas anderes** und trägt deshalb am Parser
`withOptions({ history: "push" })` — `nuqs` wertet die Angabe je Schlüssel aus. Am schmalen Fenster
füllt die Detailansicht den Bildschirm, und das Zurück des Browsers ist dort der Weg heraus; ohne
eigenen Verlaufseintrag spränge es an der Liste vorbei.

#### In einer Zelle mit mehreren Angaben weicht die Hauptinformation nicht

Nachgetragen am **10.08.2026**, aus dem ersten Befund der Sichtprüfung zu Schritt 5, Teil 2.

> **In einer Zelle mit mehreren Angaben wird die Hauptinformation nie gekürzt, damit Beiwerk Platz
> bekommt. Der Status weicht nicht dem Schritt daneben.**

**Der Anlass.** In der Statuszelle der Nachrichtenliste stehen seit Schritt 5 zwei Dinge: die
Plakette und der Schritt, auf dem die Nachricht steht. Mit der neuen Beschriftung stand dort
`Warte…` statt `Wartend` — die Plakette war geschrumpft, damit der Schrittname vollständig
hineinpasste. **Genau verkehrt herum:** Der Status ist die Hauptinformation, der Schritt ist Beiwerk
nach dem Leitsatz.

Umgesetzt ist das über `shrink-0` an der Plakette, **sobald ein Schritt daneben steht**; ohne ihn
darf sie weiter weichen, denn dort trägt sie bei `bedeutungNichtVerifiziert` einen Rohwert beliebiger
Länge ([`nachrichtenliste.md`](nachrichtenliste.md) §8.1).

Die Regel ist allgemeiner als ihr Anlass: Sie gilt für jede Zelle, in der zwei Angaben um dieselbe
Breite konkurrieren. Wer entscheiden muss, welche weicht, fragt nicht „was passt", sondern „was
sucht der Nutzer".

#### Was der Browsertest nicht kann

Ebenfalls nachgetragen am **10.08.2026**, nachdem dieselbe Lücke in drei aufeinanderfolgenden
Schritten aufgetaucht ist.

> **Die Fensterbreite lässt sich nicht ändern** — `resize_window` meldet Erfolg, `innerWidth` bleibt
> stehen. Verhalten am schmalen Fenster ist deshalb bei **jedem** Schritt von Hand zu prüfen und
> gehört in die Vorbereitungsliste, nicht in die Abnahme durch Claude Code.

Gemessen jeweils so: `resize_window` liefert eine Erfolgsmeldung, `innerWidth` bleibt bei 1920,
`outerWidth` meldet `0`. Nachgesehen werden kann deshalb nur das **Regelwerk** — welche Klassen an
welchem Umbruchpunkt greifen —, und das ist etwas anderes als eine Sichtprüfung.

**Warum das hier steht und nicht je Feature.** Die Grenze hängt am Werkzeug und nicht an der
Ansicht; sie ist in Schritt 4 ([`nachrichtenliste.md`](nachrichtenliste.md) §8.4) und in Schritt 5
([`nachrichtendetail.md`](nachrichtendetail.md) §10.10) je einzeln entdeckt und je einzeln als
offener Punkt notiert worden. Beim dritten Mal ist das keine Beobachtung mehr, sondern eine
Eigenschaft der Umgebung — und die gehört an eine Stelle, an der sie **vor** dem Bau gelesen wird.

#### Ein 404 auf einer neuen Route ist erst der dritte Verdacht *(24.08.2026)*

Nachgetragen bei der Sichtprüfung zu Schritt 9a, weil der Befund genau wie ein echter Defekt aussah
und keiner war.

> **Der laufende `next dev` verliert Routen.** `/administration/benutzer` **und**
> `/administration/katalog` lieferten beide `404`, während `/administration`, `/` und
> `/nachrichten` mit `200` antworteten. Ein `touch` auf die beiden `page.tsx` genügte, danach
> standen beide wieder.

**Warum das eine Falle ist und keine Fußnote:** Ein `404` auf einer eben gebauten Seite liest sich
als „die Route ist falsch angelegt" — und am Tag davor war ein Befund derselben Gestalt ein echter
Fehler im Code (`/administration` lud gar nicht, §8 oben). Die Unterscheidung ist billig und steht
hier, damit sie nicht jedes Mal neu hergeleitet wird:

| Beobachtung | Was es heißt |
|---|---|
| **Nur die neue Route** ist `404` | Verdacht auf den eigenen Code |
| **Eine unberührte Nachbarroute ist es auch** | der Dev-Server, nicht der Code |
| Fehlerüberlagerung oder Konsolenmeldung | ein Übersetzungs- oder Laufzeitfehler, kein `404` |

Der zweite Fall lag hier vor: Die Katalogseite war seit dem Vortag unverändert und fiel mit aus.
**Erst `touch`, dann zweifeln** — und wenn das nicht hilft, den Dev-Server neu starten, bevor am
eigenen Diff gesucht wird.

**`curl` taugt für diese Unterscheidung nicht.** Ohne Sitzungs-Cookie antwortet `src/proxy.ts` mit
`307` auf *jede* dieser Adressen; der Statuscode sagt dann nichts über die Route. Geprüft wird im
angemeldeten Browser.

#### Die `clearOnDefault`-Falle, in einem Satz

`nuqs` entfernt einen Parameter aus der URL, sobald er dem **Standardwert** gleicht — geprüft wird
das nur, wenn überhaupt einer gesetzt ist. Daraus folgt beides: Ein Parameter mit Standardwert, der
in der URL stehen *soll*, braucht `clearOnDefault: false`; `nachricht` hat keinen Standardwert und
braucht es nicht. **Wer je ein `withDefault` ergänzt, schreibt `clearOnDefault: false` in dieselbe
Zeile** — oder entscheidet bewusst dagegen, wie bei `langeSuche`, das nichts weglässt, sondern etwas
zulässt.

> Der Fall, an dem die Falle entdeckt wurde, war `zwischenschritte`; er ist am 11.08.2026 entfallen
> ([`nachrichtenliste.md`](nachrichtenliste.md) §5). **Die Falle bleibt** — sie hängt an `nuqs` und
> nicht an diesem Parameter.

---

## 8a. Farbe in ein Diagramm — `var()` in einem Recharts-Prop *(31.08.2026)*

**Der Abschnitt steht hier und nicht in einer Ansichtsdatei**, weil er nichts über eine Ansicht sagt,
sondern darüber, **wie Farbe in die Anwendung kommt**. Das ist Unterbau.

### Die Frage, und warum sie eine Messung war

Recharts färbt über ein Prop: `<Bar fill="#b3261e" />`. Genau diese Form verbietet
`tests/farbwerte.test.ts` in eigenen Komponenten, und das visuelle Konzept steht und fällt damit
(§2 dort). Der naheliegende Ausweg ist `fill="var(--status-fehler)"` — mit der Erwartung, dass
Recharts die Zeichenkette unverändert ins SVG-Attribut schreibt und der Browser sie auflöst.

**Diese Erwartung war bis dahin niemandes Messung.** Sie ist vor dem Dashboard-Frontend belegt
worden und nicht darin.

### Wie gemessen worden ist

**In einem echten Browser.** `jsdom` wertet kein CSS aus und kann `var()` nicht auflösen; ein grüner
Test dort wäre kein Beleg gewesen. Eine Bauform für Browsertests gibt es im Projekt nicht (§9 —
Vitest, `node`, für einige Dateien `jsdom`), und es ist auch keine entstanden.

| | |
|---|---|
| **Browser** | das installierte **Chrome 151.0.7922.174**, kopflos, über das DevTools-Protokoll gesteuert. Keine neue Abhängigkeit, kein Playwright, kein heruntergeladener Browser |
| **Recharts** | **3.10.1**, nur für die Messung installiert und danach wieder entfernt |
| **Aufbau** | eine temporäre Route `src/app/farbprobe/page.tsx` — bewusst **außerhalb** der Gruppe `(app)`, damit weder Anwendungsrahmen noch Backend noch Anmeldung im Weg stehen. Die Routensperre prüft nur, ob ein Sitzungs-Cookie **da** ist (§2), ein Platzhalter genügte |
| **Zweimal gefahren** | gegen `next dev` **und** gegen `next build` + `next start`. Beide Läufe sind Zeichen für Zeichen gleich ausgefallen |
| **Gegenprobe auf derselben Seite** | ein Absatz mit `class="text-status-fehler"`. Er muss dieselbe Farbe ergeben wie das Segment — sonst wäre nicht das Prop, sondern das Token die Frage |

**Der Probecode ist entfernt.** Es bleibt dieser Befund.

### Was im DOM stand — der abgelesene Wortlaut

Das Segment, unverändert aus `document.querySelector(…).outerHTML`:

```html
<path fill="var(--status-fehler)" name="Fehler" x="87.5" y="232.04" width="180" height="30.96"
      radius="0" class="recharts-rectangle" d="M 87.5,232.04 h 180 v 30.96 h -180 Z"></path>
```

Der ganze Legendeneintrag. **Er trägt die Farbe zweimal und auf zwei verschiedene Arten** — am
Symbol als SVG-Attribut wie beim Segment, an der Beschriftung als Inline-Stil:

```html
<li class="recharts-legend-item legend-item-0" style="display: inline-block; margin-right: 10px; white-space: nowrap;"><svg aria-label="Fehler legend icon" class="recharts-surface" width="14" height="14" viewBox="0 0 32 32" style="display: inline-block; vertical-align: middle; margin-right: 4px;"><title></title><desc></desc><path stroke="none" fill="var(--status-fehler)" d="M0,4h32v24h-32z" class="recharts-legend-icon"></path></svg><span class="recharts-legend-item-text" style="color: var(--status-fehler); white-space: normal; overflow-wrap: break-word;">Fehler</span></li>
```

Der Tooltip-Eintrag — hier gibt es **kein** SVG und damit auch kein Attribut, die Farbe steht
ausschließlich im Inline-Stil:

```html
<li class="recharts-tooltip-item" style="display: block; padding-top: 4px; padding-bottom: 4px; color: var(--status-fehler);"><span class="recharts-tooltip-item-name">Fehler</span><span class="recharts-tooltip-item-separator"> : </span><span class="recharts-tooltip-item-value">12</span><span class="recharts-tooltip-item-unit"></span></li>
```

**Genau deshalb waren Legende und Tooltip mitzuprüfen.** Wer nur das SVG-Attribut ansieht, hat zwei
von vier Stellen gesehen; die anderen beiden gehen einen anderen Weg durch Recharts und hätten
anders ausfallen können.

### Was `getComputedStyle` daraus gemacht hat

| Lage | wie die Farbe dort steht | Eigenschaft | aufgelöster Wert |
|---|---|---|---|
| Balkensegment | SVG-Attribut | `fill` | `lab(42.4236 59.8149 41.9956)` |
| Legendensymbol | SVG-Attribut | `fill` | `lab(42.4236 59.8149 41.9956)` |
| Legendenbeschriftung | Inline-Stil | `color` | `lab(42.4236 59.8149 41.9956)` |
| Tooltip-Eintrag | Inline-Stil | `color` | `lab(42.4236 59.8149 41.9956)` |
| *Gegenprobe* `text-status-fehler` | Tailwind-Klasse | `color` | `lab(42.4236 59.8149 41.9956)` |
| *das Token selbst* | — | `--status-fehler` | `lab(42.4236% 59.8149 41.9956)` |

**Alle fünf gleich, und gleich dem Token.** Dass dort `lab()` und nicht `oklch()` steht, ist
Lightning CSS beim Bauen — es schreibt die Werte um; im gebauten CSS steht zu jedem zusätzlich ein
Hex-Rückfall. Für die Frage hier ändert das nichts.

### Und weil „aufgelöst" nicht „gemalt" heißt: das Pixel

Ein Bildschirmfoto über das DevTools-Protokoll, zurück in die Seite als `data:`-URL, auf eine
Leinwand gezeichnet, in der **Mitte jedes Segments** ein Pixel gelesen:

| Prop im Quelltext | Pixel im Bild | Sollwert des Tokens |
|---|---|---|
| `fill="var(--status-fehler)"` | `#be2323` | `#be2323` |
| `fill="var(--status-offen)"` | `#525252` | `#525252` |
| `fill="var(--status-abgeschlossen)"` | `#01684c` | `#01684c` |
| Legendensymbol, `var(--status-fehler)` | `#be2323` | `#be2323` |

Die Sollwerte stammen aus `scripts/farbrolle-ueberfaellig/rechne.mjs`; `#01684c` ist zusätzlich der
Wert, den [`visuelles-konzept.md`](visuelles-konzept.md) §3 seit Schritt 3 nennt.

### Die Entscheidung: **Recharts**

Die Regel stand vor der Messung fest und ist nicht neu erwogen worden: `var()` kommt an **und** löst
auf, in allen drei Lagen — also Recharts, und kein Eigenbau aus Flex-Spalten.

**Der dritte Weg bleibt ausgeschlossen**, auch als Notlösung: Farben zur Laufzeit über
`getComputedStyle` auslesen und als Literal in das Prop geben wäre ein **zweiter Weg**, auf dem Farbe
in die Anwendung kommt, und ein späterer Dunkelmodus käme ohne Neurendern nicht nach
([`visuelles-konzept.md`](visuelles-konzept.md) §2 schließt genau das aus).

#### ⚠️ Befund: `tests/farbwerte.test.ts` brauchte dafür **keine** Änderung

Der Auftrag sah vor, den Test „um `var(--…)` als erlaubte Form zu erweitern". **Nachgesehen: Er hat
diese Form nie verboten.** Die drei Muster treffen Hex-Werte, die Farbfunktionen
`oklch|oklab|rgb|rgba|hsl|hsla|color-mix` und die Tailwind-Palette — `var(` steht in keinem davon.
`fill="var(--status-fehler)"` ist heute schon zulässig, und `fill="#b3261e"` — die Form aus der
Recharts-Dokumentation — bleibt es nicht.

**Der Test steht damit genau richtig**, und die Datei hat nur einen Absatz bekommen, der das
festhält. Eine Musteränderung wäre eine Lockerung ohne Anlass gewesen.

### Was nicht gemessen ist

1. **Nur Recharts 3.10.1.** Führte eine spätere Fassung eine eigene Farbverarbeitung ein — etwa um
   einen Farbverlauf zu berechnen —, gälte der Befund nicht mehr. Er ist an eine Version gebunden
   und trägt sie deshalb im Text.
2. **Nur `<Bar>`, Legende und Tooltip**, jeweils in der Voreinstellung. `Cell`, `activeBar`,
   Farbverläufe (`<linearGradient>`), Flächen- und Liniendiagramme sind **nicht** angesehen worden.
   Wer eine davon braucht, misst sie nach demselben Muster nach.
3. **Nichts über Barrierefreiheit.** Dass eine Farbe ankommt, sagt nicht, dass sie genügt — für ein
   Diagramm gilt „nie allein über Farbe" unverändert.

---

## 9. Tests

`pnpm test` (Vitest, in `pnpm build` verankert). Bewusst klein: keine Testing Library, kein
React-Plugin. Geprüft werden die **Entscheidungen**, nicht das Markup — das sind alles reine
Funktionen, und ein gerenderter Baum brächte hier nichts außer Laufzeit und Abhängigkeiten.

> **Ergänzt am 11.08.2026, fortgeschrieben am 13.08.2026 — die Voreinstellung bleibt, die Ausnahmen
> sind benannt.** Dieser Absatz nannte bis zum 11.08.2026 zusätzlich **kein jsdom**. Das gilt
> weiterhin für die überwiegende Mehrheit der Testdateien: Die Umgebung ist `node`, und allein die
> **rendernden** schalten sie über `// @vitest-environment jsdom` für sich um. **Welche das sind
> und wie viele Fälle sie tragen, steht im Kopf von `frontend/vitest.config.mts`** — hier stünde
> sonst dieselbe Zahl ein zweites Mal. Gerendert wird mit `createRoot` und `act`; die einzige neue
> Abhängigkeit ist `jsdom`, und die Hülle steht in `tests/hilfe/rendern.tsx`.
>
> **Der Anlass ist kein Sinneswandel, sondern eine Fehlerklasse ohne Netz.** Am 11.08.2026 trugen
> zwei Geschwister im Detailpanel denselben React-`key`; die Konsole meldete es, kein Test konnte es
> finden, und sichtbar falsch war nichts ([`verkettung.md`](verkettung.md) §8.12). Gerendert wird
> deshalb für **sieben** Fälle und nicht mehr — Sätze und Umbruchpunkte, die von Hand grundsätzlich
> nicht zu sehen sind, zwei Aussagen über **Abwesenheit** (kein Block, keine Anfrage), und zweimal
> eine Regression zum Schlüssel. Alles Übrige bleibt reine Funktion.
>
> **Die Zählung wird an einer Stelle geführt:** dem Kopfkommentar von `frontend/vitest.config.mts`.
> Diese Tabelle und der Kopf von `tests/hilfe/rendern.tsx` verweisen darauf; wächst die Zahl, wächst
> sie dort. Der Anlass für diese Regel ist ein Befund der Abnahme vom 13.08.2026: Schritt 7 hatte
> `tests/hilfe/rendern.tsx` auf sieben Fälle fortgeschrieben, `vitest.config.mts` und diese Tabelle
> aber bei vier beziehungsweise drei stehen lassen — und `tests/ansicht-umschalter.test.tsx` fehlte
> hier seit Schritt 6 ganz. **Drei Orte für dieselbe Zahl sind zwei zu viel.**
>
> **Angewandt am selben Tag, bei der ersten Gelegenheit:** Schritt 7, Teil 3 bringt eine vierte
> rendernde Datei (`tests/suche-marken.test.tsx`). Die neue Gesamtzahl steht **ausschließlich** in
> `vitest.config.mts`; der Kopf von `tests/hilfe/rendern.tsx` nennt sie nicht mehr, sondern
> beschreibt die Bedingung, unter der ein Fall dazukommt. Diese Tabelle führt die Dateien weiter —
> **Dateien, keine Summe.**

### `console.error` lässt den Testlauf fehlschlagen *(seit 11.08.2026)*

`tests/setup/konsole.ts`, über `setupFiles` für **alle** Dateien — auch für die nicht rendernden:
Eine Meldung aus einer reinen Funktion ist genauso ein Befund. Die ursprüngliche Meldung steht im
Fehlertext, die Platzhalter von React (`%s`) sind aufgelöst.

Drei Regeln, und sie sind der eigentliche Inhalt:

1. **Keine pauschale Ausnahmeliste.** Wird ein Test rot, ist die Meldung die Nachricht und nicht das
   Problem — behoben wird die Ursache.
2. Eine nachweislich nicht abstellbare Fremdmeldung wird **einzeln** aufgenommen: exakte Meldung,
   ein Satz Begründung, Datum. Keine Muster, keine Platzhalter. **Die Liste ist heute leer.**
3. Wird es viel, wird angehalten und berichtet, statt zwanzig Tests umzuschreiben.

`console.warn` bleibt vorerst außen vor: Der Doppelschlüssel kommt über `console.error`, und eine
zweite Verschärfung in derselben Runde machte den Befund unlesbar.

**Geprüft ist das Netz, nicht behauptet.** Der Doppelschlüssel wurde am 11.08.2026 absichtlich
wieder eingebaut; der Lauf wurde rot mit der Meldung *„Encountered two children with the same key,
`8f3a1c2e-…`"* im Fehlertext, und die Änderung wurde zurückgenommen. Ein Netz, das man nicht
gerissen hat, ist eine Behauptung.

| Datei | Was |
|---|---|
| `ablauf.test.ts` | Änderungszwang vor Mandantenauswahl vor Startseite |
| `sprachdateien.test.ts` | gleicher Schlüsselsatz; 404-Wortwahl; Rückfall auf `detail` |
| `farbwerte.test.ts` | kein Hex-Wert, keine Tailwind-Farbklasse in einer Komponente |
| **`serverbausteine.test.ts`** *(24.08.2026)* | **Blockierungstest, aus einem Befund am laufenden System.** Er berechnet aus den Dateien, welche Bausteine in `components/ui` `radix-ui` auswerten und trotzdem kein `"use client"` tragen — heute genau `button.tsx` —, und weist nach, dass **keine Server-Komponente** einen davon importiert. Der Anlass steht in §8: `/administration` warf beim Aufrufen *„createContext only works in Client Components"*, und **kein bestehender Prüfschritt konnte das finden** (`pnpm check` rendert keine Seite, `jsdom` kennt die Grenze nicht, `next build` prerendert die dynamische Route nicht). Die Liste ist berechnet und nicht geschrieben, weil `components/ui` Generatorbereich ist; ein eigener Fall hält fest, dass sie nicht leer laufen darf |
| `zwischenspeicher.test.ts` | geleert **vor** dem Weitergehen, bei Wechsel und Abmeldung; das Ziel nach dem Mandantenwechsel trägt keine Filter |
| `format.test.ts` | UTC → Anzeige in der gelieferten Zone; Rückfall auf UTC statt auf den Browser; relative Zeit; Wanduhrzeit der Eingabefelder, auch am Umstellungstag; **Dauern** mit höchstens zwei Einheiten und „< 1 s" statt „0 s"; **Anteile** über `Intl` — deutsch `56 %` mit schmalem geschütztem Leerzeichen, englisch `56%`, auf ganze Prozent gerundet *(24.08.2026)* |
| `routen.test.ts` | `weiter` als offene Weiterleitung ausgeschlossen |
| `nachrichtenfilter.test.ts` | URL → Zustand → URL; unbekannte Werte werden übergangen; **der Cursor taucht in keiner erzeugten URL auf**; die beiden Zeitfenstermodi schließen einander aus; `langeSuche` steht in der URL und wird nur mit dem Suchbegriff geschickt; welche Problemtypen an das Suchfeld gehören, welche an die Zeitfensterfelder und welche über die Ansicht; das halb ausgefüllte freie Fenster; **`nachricht` steht in der URL und in keiner Abfrage** |
| `nachrichtendetail.test.ts` | die Normierung des Zeitleistenbalkens, die Schwelle der Lückenzeile, die vier offenen Zustände ([`nachrichtendetail.md`](nachrichtendetail.md) §10.9) |
| `kette.test.ts` | die Einteilung nach der Flussrichtung, die Zahl in der Überschrift, das Nachladen, ob es einen Block gibt ([`verkettung.md`](verkettung.md) §8.10) |
| **`detail-baum.test.tsx`** *(neu, 11.08.2026)* | **gerenderter Baum**, drei Fälle: `tiefeErreicht` und `zyklusErkannt` samt ihrer Lage **unter beiden** Abschnitten (§8.5 dort), und die Regression zum Doppelschlüssel — sie besteht genau dann, wenn kein `console.error` fällt |
| **`ansicht-umschalter.test.tsx`** *(11.08.2026)* | **gerenderter Baum**, zwei Fälle: die Sichtbarkeitsregel des Umschalters ist selbst eine Klasse, und ihr Umbruchpunkt ist von Hand nicht prüfbar (§7); dazu, dass er im Panel und auf der eigenen Route Verschiedenes sagt. **Am 18.08.2026 von „ein Fall" auf zwei berichtigt** — die Datei trug den zweiten seit Schritt 6, die Zählung nicht |
| **`bam-block.test.tsx`** *(12.08.2026, ergänzt 13.08.2026)* | **gerenderter Baum**, vier Fälle: derselbe Wert unter zwei Typen **ohne `console.error`** (der Schlüssel ist `(typ, wert)`, M37); `bamAnzahl === 0` → **nicht im Baum und keine Anfrage**; eingeklappt mit Werten → Überschrift mit der Zahl, **und immer noch keine Anfrage**; die **Fuge** der zerlegten Beschriftung — vollständig und ohne eingefügtes Leerzeichen vor der Endung, über `textContent` und nicht über eine Textsuche ([`bam-werte.md`](bam-werte.md) §11a) |
| `rohdaten.test.ts` *(18.08.2026, korrigiert 20.08.2026)* | die Entscheidungen des Rohdatenzugriffs: die **Beschriftungsregel** aus [`rohdaten-frontend.md`](rohdaten-frontend.md) §2 in allen **vier** Lagen (aufgelöster Schrittname, Rückfall `Schritt N · Familie`, ohne Schrittfolge, **Familie allein auf Schritt 0**), dass **`Message.Payload.GUID` kein Ziel erzeugt** (M73), die **Ziele** je Schritt samt der Ankündigung des Ausschnitts und dem Rest ohne Zeile, der **Gleichlauf** von Anzeige und Download über alle fünf Zustände samt der Ausnahme „binäres Protokoll für `MANDANT`", die drei Vermerke mit ihrer Reihenfolge, der zweite Versuch **nur** bei nicht erreichbarer Ablage, und dass kein Pfad eine GUID oder Ablagenkennung trägt |
| **`artefakt-ansicht.test.tsx`** *(18.08.2026)* | **gerenderter Baum**, neun Fälle: der **Textknoten** (ein Inhalt, der gültiges HTML ist, erzeugt kein einziges Element — und das `<pre>` hat genau ein Kind), die **vier Zustände** je einer, der Ausschnitt-Vermerk in beide Richtungen, der Download-Knopf nach Entscheidung 9 ([`rohdaten-frontend.md`](rohdaten-frontend.md) §6) und die Beschriftung ohne Nachladen (ebenda §4, „Drei Abfragen, und jede hat ihren Grund") |
| **`zeitleiste-ziele.test.tsx`** *(18.08.2026, Nachbesserung)* | **gerenderter Baum**, acht Fälle. Er ist an die Stelle von `dateien-block.test.tsx` getreten, als der eigene Dateienblock entfiel: die drei Lagen je Schritt (beide Arten, nur eine, keine), der **Eingang** über der Leiste — Schritt `0` hat dort keine Zeile und seine Artefakte dürfen trotzdem nicht verschwinden —, die **Belastungsprobe aus M55** mit fünfzehn eigenen Zielen ohne doppelten React-Schlüssel, das **Anspringen** der Eigenschaftengruppe (`document.activeElement`), und drei Fälle um den Sprung bei **kaltem Zwischenspeicher**: Er kommt nicht nach, wenn der Nutzer beim Warten zuklappt oder den Fokus weitersetzt, und er kommt sehr wohl, wenn der Nutzer stehen bleibt. Dazu die Gegenprobe: ohne Eigenschaften kein Schalter am Schrittnamen |
| **`eigenschaften-block.test.tsx`** *(17.08.2026)* | **gerenderter Baum**, vier Fälle: derselbe Name in **drei** Gruppen **ohne `console.error`** (der Schlüssel ist `${position}:${name}`, M17 3); `anzahl === 0` → **kein Schalter und keine Anfrage**; eingeklappt mit Werten → Überschrift mit der Zahl, **und immer noch keine Anfrage**; der Rückfall „Schritt N" ohne gelieferte `schritte`. Die Einteilung selbst ist reine Funktion und steht in `nachrichtendetail.test.ts` ([`nachrichtendetail.md`](nachrichtendetail.md) §10.5) |
| `bam-beschriftung.test.ts` *(13.08.2026)* | die Zerlegung der Typbeschreibung in Name und Endung, an den gemessenen Fällen aus M45‑1 — samt beider **Gegenproben**: das `i`-Flag (`Sender_Ident_FORS`) und „ab dem letzten Unterstrich" (`_SAP` statt `_L_SAP`) |
| `suche.test.ts` *(13.08.2026)* | die Entscheidungen der Belegsuche: Parameterform mit Pflichttrenner und Teilung am **ersten** Doppelpunkt, der Rundlauf URL → Zustand → URL über den **wiederholten** `begriff`-Parameter, der übergangene unbrauchbare Begriff, **die Abfrage mit und ohne geöffnete Nachricht Zeichen für Zeichen dieselbe**, der doppelte Begriff, die Sperre beim neunten, die Nulltreffer-Zeile, die Spalte „Treffer" samt Dedupe, das Jahresfenster einschließlich Schalttag, und dass die Abschneidemeldung in beiden Sprachen **Fenster und Abschneidung** nennt ([`bam-suche.md`](bam-suche.md) §11.10) |
| **`suche-marken.test.tsx`** *(13.08.2026)* | **gerenderter Baum**, vier Fälle: derselbe Wert unter zwei Typen **ohne `console.error`** — hier an den Marken der Suche —, der unbekannte Typ als Nummer, und zweimal eine Regel, die **selbst** eine Klasse plus ein `title` ist: die Längenregel der Trefferspalte und der Kettenhinweis |
| `katalogfilter.test.ts` *(24.08.2026)* | die zwei Filter der Pflegeliste: URL → Zustand → URL über beide, auch in Kombination; der unbrauchbare Wert wird übergangen; **keine Vorgabe steht in der URL** (kein `clearOnDefault: false`, weil beide Vorgaben alles zeigen); und die Stelle, an der E14 trägt — **„nur mit Nachrichten" lässt die nie geprüften Zeilen stehen** und wirft nur die toten weg, samt der Gegenprobe, dass vor dem ersten Bestandslauf nichts wegfällt. Dazu der Auffangprozess an `Undefined` und die drei Gegenproben zu `^0+_` (M78) |
| `katalog.test.ts` *(24.08.2026)* | die Entscheidungen der Katalogpflege ohne Ansicht: der **leere Partner** als `null` samt Leerraum und Massenzuordnung (E4); `darfOeffnen` in allen drei Lagen (E19); die Antwort im Zwischenspeicher — an ihrer Stelle, ohne Anhängen, und die wachsende Partnerliste; die Vorschläge (leeres Feld zeigt alles, Treffer an beliebiger Stelle, der getippte Wert bleibt); der **Fortschritt** über eine bekannte Liste samt toter Prozesse und Auffangprozess (E18); die **Hinweisbedingung** je einmal erfüllt und nicht erfüllt, dazu „hängt am Partner allein" und „bleibt nach Handarbeit stehen" (E17); die Projektauswahl; und der **Zustand „kein Zugriff"** — er greift bei `zugriff-verweigert` und bei **keinem** der beiden anderen `403` dieses Backends |
| **`katalog-tabelle.test.tsx`** *(24.08.2026)* | **gerenderter Baum**, fünf Fälle: dass **`false` und `null` drei verschiedene Sätze ergeben** und eine nie geprüfte Zeile nicht den Satz der toten trägt — die Unterscheidung entsteht erst in der Zelle, und `? :` über den Wahrheitswert träfe beide im selben Zweig; dazu die **Verdrahtung** der Sperre aus E19 (die andere Schaltfläche wirklich gesperrt, die offene durch das Formular ersetzt, und die Gegenprobe ohne offene Zeile) |
| `benutzer.test.ts` *(24.08.2026)* | die Entscheidungen der Benutzerverwaltung ohne Ansicht: **das eigene Konto über den Benutzernamen erkannt, ohne Rücksicht auf Groß- und Kleinschreibung** (`benutzerverwaltung.md` E19 — `GET /api/auth/me` liefert keine `id`, und ein `===` verfehlte genau den Grenzfall `Admin`/`admin`, für den die Warnung da ist); **welche der fünf Vorgänge am eigenen Konto überhaupt durchlaufen**, in allen acht Richtungen und am Backend abgelesen statt geraten; die Vorwarnung erscheint beim eigenen Konto, auch bei abweichender Schreibweise, **nicht** bei einem fremden und **nicht** für eine Richtung, die das Backend ohnehin ablehnt; das Einmalpasswort mit seiner Mindestlänge und der Grenze zur Backend-Prüfung; die Mandantenmenge samt **`SYSTEM` und `WOC` in der Auswahl**, dem Mandanten, den es nicht mehr zur Wahl gibt, der letzten Zuordnung (E10) **mit dem Tausch als Gegenprobe** und dem Mengen- statt Reihenfolgevergleich; die geänderte Zeile an ihrer Stelle; **jede der neun Übersetzungen in beiden Sprachen**, dazu dass die vier `409` nicht wie `zugriff-verweigert` klingen und die administrative Sperre nicht wie die automatische; und der Zustand **„kein Zugriff"** bei `zugriff-verweigert` und bei keinem der beiden anderen `403` |
| **`benutzer-tabelle.test.tsx`** *(24.08.2026)* | **gerenderter Baum**, zehn Fälle: **`lastLogin = null` als „nie angemeldet" und nicht als leere Zelle** (E17), mit Gegenprobe; die **zwei Sperren in zwei Zellen**, in beide Richtungen (E14, E20) — und ausdrücklich als Wortlautvergleich, denn „nicht gesperrt" enthält „gesperrt"; dazu die **Verdrahtung** in zwei Richtungen: dass die Vorwarnung abgefragt wird, *bevor* ein Aufruf losläuft, dass die Mengenersetzung die vollständige Zielmenge schickt, und dass die Schaltfläche jeder anderen Zeile gesperrt ist, solange eine offen ist — samt Gegenprobe |

> **Korrigiert 20.08.2026, nachgetragen zur Korrektur vom 19.08.2026.** Die Zeile zu
> `rohdaten.test.ts` führte die Beschriftungsregel „**in allen fünf Lagen** (aufgelöster
> Schrittname, Rückfall `Schritt N · Familie`, ohne Schrittfolge, **Eingang**, Familie allein auf
> Schritt 0)". Es sind **vier**: Die Lage *Eingang → Eingegangene Datei* ist am 19.08.2026
> entfallen.
>
> **Gemessen ist (M73):** `Message.Payload.GUID` trägt in **6.249 von 6.249** Nachrichten (Fenster
> A) und **214.330 von 214.330** (Fenster B) denselben Verweis wie die Nutzdatenzeile mit dem
> **höchsten `MessageActionID`** derselben Nachricht — kein Gegenfall. Das Artefakt ist aus der
> Liste entfallen, und an die Stelle der weggefallenen Lage ist ein Testfall getreten: dass für
> diesen Namen **kein Ziel** entsteht ([`rohdaten-frontend.md`](rohdaten-frontend.md) §2 und §9).
>
> *Ausdrücklich nicht behauptet:* dass „höchster `MessageActionID`" gleichbedeutend mit „zeitlich
> zuletzt" ist. Gemessen ist die Schrittnummer, nicht die Uhr.
>
> **Bei derselben Gelegenheit zwei Verweisfehler berichtigt, beide keine Folge von M73.** Die Zeile
> zu `rohdaten.test.ts` verwies für die Beschriftungsregel auf `rohdaten-frontend.md` **§3**; dort
> steht sie in **§2** — §3 sind die Ziele an der Zeitleiste. Die Zeile zu
> `artefakt-ansicht.test.tsx` verwies für den Download-Knopf und die Beschriftung ohne Nachladen auf
> **§8**; §8 ist „Handwerk" und trägt keinen der beiden. Richtig sind **§6** (Download und
> Gleichlauf) und **§4** („Drei Abfragen, und jede hat ihren Grund"). Beide Verweise waren seit dem
> 18.08.2026 falsch.
>
> **Die Fallzahlen der drei Dateien sind unverändert** — 29, 9 und 8. In `rohdaten.test.ts` sind
> zwei Fälle entfallen und zwei hinzugekommen; dass die Summe gleich bleibt, ist Zufall und keine
> Absicht. Die Gesamtzahl der gerenderten Fälle steht weiterhin **ausschließlich** im Kopf von
> `frontend/vitest.config.mts` und ist unberührt: Beide entfallenen Fälle sind reine Funktionen.

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
