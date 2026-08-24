# Prozess-Katalog — die Oberfläche

Stand: 24.08.2026 · Schritt 9b des MVP, Teil Frontend
Vorgabe: [`prozess-katalog.md`](prozess-katalog.md) (E1–E21). Bei Widersprüchen gilt jene Datei;
alle Abweichungen sind hier unter §11 benannt und begründet.
Bedient werden die fünf Endpunkte aus [`prozess-katalog-backend.md`](prozess-katalog-backend.md).
Messungen: [`messungen-schritt9.md`](messungen-schritt9.md) M74–M80, **M83** und **M84**.

**Kein Backend, keine Migration, kein jOOQ-Statement.** Diese Runde fasst ausschließlich
`frontend/` an — dazu drei Dokumentationsdateien und `docs/README.md`.

---

## 1. Was entsteht

| Ort | Was er beantwortet |
|---|---|
| **`/administration`** | *Welche Bereiche gibt es hier, und wofür ist jeder da?* |
| **`/administration/katalog`** | *Welchem Partner gehört dieser Prozess — und was habe ich noch vor mir?* |
| **`/administration/benutzer`** | weiterhin der Platzhalter; Inhalt im 9a-Auftrag |

**Der Katalog macht aus etwas Lesbarem etwas Gruppierbares** ([`prozess-katalog.md`](prozess-katalog.md)
§1). Diese Oberfläche ist die Fläche, auf der das geschieht — und sie ist die einzige schreibende
Ansicht des ganzen Werkzeugs. Alles andere liest.

---

## 2. Der Administrationsbereich

**Ein Navigationseintrag, zwei Unterseiten** ([`frontend-grundlagen.md`](frontend-grundlagen.md) §2).
Der Pfad war seit Schritt 4 dreifach vergeben — `ROUTEN.administration`, der Navigationseintrag
und die Platzhalterseite —, und die drei Stellen sind **erweitert** worden, nicht verdoppelt.

| | |
|---|---|
| Hauptnavigation | **ein** Eintrag, `nurAdmin: true` (`lib/navigation.ts`) |
| Unternavigation | `lib/navigation.ts` `ADMINISTRATION`, gerendert von `components/bereichs-navigation.tsx` |
| Rahmen | `app/(app)/administration/layout.tsx` — trägt die Unternavigation, **keine Überschrift** |
| Übersicht | `app/(app)/administration/page.tsx` — beide Bereiche mit ihrer Beschreibung |

**Der Rahmen setzt bewusst kein `h1`.** Jede der drei Seiten setzt ihr eigenes, und
`seiten-platzhalter.tsx` bringt seines ohnehin mit; eine Überschrift hier und eine dort ergäbe zwei
`h1` auf derselben Seite.

**Die Unterseiten tragen kein `nurAdmin`.** Die Markierung säße dort an der falschen Stelle: Wer den
Bereich überhaupt sieht, ist am Eintrag darüber bereits vorbeigekommen — und wer die Adresse
eintippt, bekommt den Zustand „kein Zugriff" und keine ausgedünnte Liste.

### Der Zustand „kein Zugriff"

`components/kein-zugriff.tsx`, erkannt über `istKeinZugriff` in `lib/http.ts`.

> **Erkannt am Problemtyp und nicht am Statuscode.** `403` ist in diesem Backend **dreifach**
> vergeben, und die drei bedeuten Verschiedenes:
>
> | Typ | Was er heißt | Was hilft |
> |---|---|---|
> | `zugriff-verweigert` | Die Rolle reicht nicht | nichts — endgültig |
> | `csrf-token-ungueltig` | Der Token trug nicht | Seite neu laden und erneut senden |
> | `kein-mandant-gewaehlt` | Es ist kein Mandant aktiv | einen wählen; der Anwendungsrahmen schickt dorthin |
>
> Eine Prüfung auf den Status zeigte allen dreien dieselbe Meldung, und **zwei davon wären falsch**.
> Dieselbe Bauform wie bei `istZeitgrenze` und `istPraefixfensterZuGross` (§6 dort).

Er ist **keiner der drei Zustände** aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §5:

- **Kein Fehler.** Rot heißt in diesem Werkzeug „da ist etwas kaputt". Hier ist nichts kaputt; der
  Nutzer steht vor einer Grenze, die für ihn gilt. Eine rote Meldung lädt zum Melden ein, und es
  gibt nichts zu melden.
- **Kein Leerzustand.** „Nichts anzuzeigen" wäre die Unwahrheit: Es gibt etwas, er darf es nur nicht
  sehen.
- **Kein zweiter Versuch.** `403` steht schon beim ersten Aufruf fest (`lib/query-client.ts`), und
  ein Knopf „Erneut versuchen" verspräche, dass sich daran etwas ändern ließe.

**Der Satz kommt aus dem Fehlerkatalog, die Überschrift aus den Zustandstexten.**
`fehler["zugriff-verweigert"]` gab es bereits — es ist der Problemtyp, den das Backend liefert, und
er gilt überall gleich. Neu ist allein `zustand.keinZugriffTitel`. Zwei Wortlaute für dieselbe Sache
wären genau die Doppelpflege, vor der §6 dort warnt.

> **Für den 404-Text gilt weiterhin das Gegenteil.** Dort darf das Wort „Zugriff" nicht vorkommen,
> und `tests/sprachdateien.test.ts` erzwingt das. Hier *ist* fehlende Berechtigung die Wahrheit und
> darf benannt werden. Es sind zwei Schlüssel, und das ist der ganze Grund.

### Die Übersicht prüft die Rolle nicht

Sie ruft kein Backend auf und könnte es deshalb gar nicht belegen. Eine Prüfung gegen `role` aus der
Selbstauskunft wäre eine **Berechtigungsentscheidung im Browser** — dieselbe Grenze, die `proxy.ts`
mit einem eigenen Absatz zieht. Wer die Adresse ohne die Rolle eintippt, sieht zwei Verweise und
bekommt die Auskunft dort, wo sie belegt ist: an der Katalogpflege, aus deren `403`.

---

## 3. Die Pflegeliste

`GET /api/katalog/prozesse`, **ein Fetch, keine Paginierung, kein Zeitfenster** (E8). Bis zu 733
Zeilen bei `NEXANS`; L12 misst dafür 8,0 ms in der Datenbank.

### Die Spalten

| Spalte | Inhalt | Sichtbar ab |
|---|---|---|
| Prozess | `processId` (feste Laufweite) und `processName` darunter, dazu die Marke des Auffangprozesses | immer |
| Projekt | `projectId` und `projectName` | `lg` |
| Partner | Wert und **Vermerk** (siehe unten) | immer |
| Richtung | Eingehend / Ausgehend / nicht zugeordnet | `md` |
| Nachrichten | die drei Zustände aus E14, darunter das Alter der Erhebung | `md` |
| Pflege | offen / gepflegt und die Schaltfläche „Bearbeiten" | immer |

**Die Reihenfolge kommt vom Backend** — `ProjectID`, dann `ProcessID` (E6) — und wird im Browser
**nicht** umsortiert. Eine Spaltensortierung wäre eine neue Entscheidung und keine Ausbaustufe: Ohne
Paginierung und ohne eindeutigen Zweitschlüssel hat eine Liste, die nach einem nicht eindeutigen Feld
sortiert, keine feste Reihenfolge.

### Der Vermerk unter dem Partner — drei Bedeutungen aus zwei Feldern (E4)

| Pflegestatus | Feld | Vermerk |
|---|---|---|
| `OFFEN` | leer, Herkunft `KEINE` | **„kein Vorschlag ableitbar"** (E9) |
| `OFFEN` | gefüllt | **„Vorschlag"** — unbestätigt |
| `GEPFLEGT` | gefüllt | keiner; das ist der Normalfall |
| `GEPFLEGT` | leer | **„hingesehen, es gibt keinen"** |

**Der letzte braucht ihn am dringendsten.** Ohne ihn sähe die Zeile aus wie eine unbearbeitete — und
genau dieser Zustand ist die einzige Pflege, die ein toter Prozess je bekommt.

### Die drei Zustände von `traegtNachrichten` (E14)

| Wert | Anzeige |
|---|---|
| `true` | trägt Nachrichten |
| `false` | **trägt keine Nachrichten** |
| `null` | **nicht geprüft** — und ausdrücklich nicht dasselbe wie `false` |

**Keine Farbe, an keiner Stelle dieser Zeile.** Weder Pflegestatus noch Bestand noch Richtung
bekommen eine Farbrolle: Die vier Statusfarben des Projekts sind fachlich an den
**Nachrichtenstatus** vergeben, und *„es gibt keine zweite Bedeutung von Grün oder Rot"*
([`visuelles-konzept.md`](visuelles-konzept.md) §3). Ein grünes „gepflegt" neben einem grünen
„abgeschlossen" hieße zweierlei mit demselben Zeichen. Die Unterscheidung liegt vollständig im
**Wort** — die Regel „nie allein über Farbe" ist damit nicht knapp, sondern trivial erfüllt.

`false` und `null` stehen beide gedämpft und sagen Verschiedenes. Der Unterschied ist der ganze Grund
für die Spalte: Bei `VOTG` tragen **350 von 390** Prozessen keine einzige Nachricht (M83‑5) — der
Kurator schreibt dort 350-mal eine Zuordnung zu einem Vertrag, der nichts produziert, und das ist
eine andere Aussage als dieselbe Zeile bei einem Prozess mit Verkehr.

**Das Alter der Erhebung steht an der Zeile und nicht über der Liste.** Das Backend liefert dafür
ausdrücklich kein Feld im Umschlag: Die Angabe gehört an die Zeile, die sie beschreibt. Nach einem
Lauf tragen alle Zeilen denselben Zeitpunkt; davor können sie sich unterscheiden, und dann ist der
Unterschied die Auskunft.

### Der Auffangprozess (E16)

Erkannt an **`Undefined` in der `ProcessID`** und **niemals über `^0+_`**. M78 hat den zuerst
beauftragten Ausdruck gefahren und **sechs** Treffer bekommen, von denen **vier keine
Auffangprozesse sind** — regulär benannte Prozesse mit einem Nummernpräfix aus Nullen, darunter
einer mit **1.602** Nachrichten. Ein Filter darauf erklärte den größten davon zum Auffangbecken.

Er wird gekennzeichnet und **zählt normal mit** — in der Liste, im Fortschritt, im Filter. Er ist
kein Sonderfall der Menge, sondern einer der Benennung.

Die Kennzeichnung ist eine **Marke** (`components/marke.tsx`), ohne Schließen-Schaltfläche und damit
eine Anzeige und kein Bedienelement. Es gibt keine zweite Marken-Gestalt im Projekt.

### Der Rahmen steht, nur der Inhalt scrollt

[`frontend-grundlagen.md`](frontend-grundlagen.md) §7 gilt hier **zum ersten Mal für eine sehr lange
Liste**, und die Umsetzung besteht aus dem, was *nicht* dasteht:

1. **Kein zweites `overflow-y-auto`.** Der einzige Scrollbereich bleibt das `main` des
   Anwendungsrahmens. Die Tabellenkopfzeile bleibt trotzdem stehen, weil `position: sticky` keinen
   eigenen Scrollcontainer braucht — sie hängt sich an den nächsten, und das ist genau jenes `main`.
2. **Kein `overflow-x-auto` darüber.** Das ist die Stelle, an der es sonst stillschweigend kippt:
   `overflow-x: auto` stuft `overflow-y` auf `auto` hoch, der Container wird selbst zum
   Scrollbereich, und die klebende Kopfzeile klebt an *ihm* statt am Fenster — sie bewegt sich dann
   nie. **Deshalb steht in dieser Tabelle kein `<Table>` aus `components/ui`:** Der Baustein bringt
   diesen Container mit. Seine Zellen-Gestalt ist nachgebildet, und breite Inhalte brechen um, statt
   waagerecht zu scrollen.
3. **Keine Höhe am Fenster.** Kein `h-dvh`, kein `min-h-screen`, kein `h-full`.

**`border-separate` statt `border-collapse` ist kein Geschmack.** Mit `collapse` gehören die Rahmen
der *Tabelle* und nicht den Zellen — eine klebende Kopfzeile ließe ihren Trennstrich beim Scrollen
zurück.

`<caption>` ist `sr-only` und damit `position: absolute` — genau die Klasse Element, die §7 seine
dritte Bedingung gekostet hat. Sie ist hier unschädlich, weil `main` `relative` ist und sie deshalb
an ihm hängt und nicht am Ursprungsblock der Seite.

---

## 4. Die zwei Filter

Beide stehen **in der URL** (nuqs), beide liegen **im Feature** und nicht in `lib/filter.ts` — dort
steht allein die Zeitfenster-Abstraktion, und diese Liste hat keins.

| Filter | Wo gerechnet | Warum dort |
|---|---|---|
| `nurOffene` (Vorgabe `false`) | **Backend** | Der Endpunkt kennt den Parameter; er ist der Arbeitsmodus (E7) |
| `nurMitNachrichten` (Vorgabe `false`) | **Browser** | Die volle Liste liegt ohnehin vor (E8, E20) |

**Kein `clearOnDefault: false`.** Die Regel lautet: *ein Standardwert, der etwas weglässt, gehört in
die URL; einer, der etwas setzt, nicht*. Beide Vorgaben zeigen **alles** und lassen nichts weg — es
gibt nichts zu teilen, und eine URL ohne Parameter zeigt die vollständige Liste. Ein Wahrheitswert
braucht trotzdem ein `withDefault`, sonst wäre er `null` statt `false`; das ist dieselbe Ausnahme wie
bei `langeSuche` in der Nachrichtenliste.

**Zeilen mit `null` bleiben sichtbar**, auch bei aktivem Filter (E20). Das ist die Stelle, an der die
drei Zustände tragen: Eine Zeile, für die nie ein Bestandslauf lief, verschwände sonst aus **beiden**
Filterstellungen — und vor dem ersten Lauf wäre die Liste vollständig leer, obwohl es Prozesse gibt.

### Zwei Listen, und die zweite kostet meistens nichts

Das ist der Punkt, an dem `nurOffene` als **Anfrage**parameter etwas kostet, und er gehört benannt.

Die Tabelle zeigt die **gefilterte** Liste. Fortschritt (E18), Hinweis (E17) und die Projektauswahl
der Massenzuordnung lesen die **volle** — über der gefilterten wären alle drei Aussagen falsch,
sobald der Haken gesetzt ist:

- Der **Fortschritt** kennte seinen Zähler nicht: Die gepflegten Zeilen fehlen in der Antwort ganz.
- Der **Hinweis** übersähe eine gepflegte Zeile mit Regelherkunft und erschiene, wo er nicht
  hingehört.
- Die **Projektauswahl** verlöre ausgerechnet die vollständig gepflegten Projekte — also genau die,
  für die E12 die Massenzuordnung als Korrekturwerkzeug vorsieht.

Die Ansicht hält deshalb zusätzlich `useKatalogzeilen(false)`. **Solange der Haken nicht gesetzt ist,
sind beide Abfragen dieselbe** und TanStack Query stellt sie einmal; gesetzt kostet es eine zweite
über denselben Endpunkt. **Eine eigene Zähl-Abfrage gibt es weiterhin nicht** (E8) — es ist zweimal
die Liste und nicht einmal die Liste und einmal ein `COUNT`.

---

## 5. Bearbeitung in der Zeile (E19)

`PUT /api/katalog/prozesse/{processId}`, **ein Aufruf je Zeile**, kein Sammelspeichern.

`lib/http.ts` bekommt dafür den ersten schreibenden Aufruf, der kein `POST` ist. Das ist keine
Formsache: **`PUT` setzt beide Felder als Ganzes**, ein fehlendes bedeutet also *leer* und nicht
„unverändert" — und genau darauf beruht E4.

### Warum das Formular unter der Zeile steht

Vier Bauformen kamen in Frage, drei scheitern:

| Bauform | Woran sie scheitert |
|---|---|
| Eingabefelder **in** den Zellen | Unter `md` sind Richtung und Bestand ausgeblendet — die Richtung wäre am schmalen Fenster gar nicht erreichbar. Und 96 px Spaltenbreite tragen kein Auswahlfeld. **Genau der Fall, vor dem E19 warnt** |
| Ein **Dialog** über der Tabelle | Nimmt die Nachbarzeilen weg, an denen man sich beim Kuratieren orientiert. Für zwei Felder außerdem zu schwer |
| Die Zeile **ersetzen** | Der bisherige Stand verschwindet genau in dem Moment, in dem man ihn ändern will |
| **Die Zeile bleibt, das Formular klappt darunter auf** | ✔ Der alte Stand ist beim Tippen sichtbar, die Zuordnung funktioniert bei jeder Breite, die Tastaturreihenfolge ist die natürliche |

Umgesetzt als zweite `<tr>` mit `colSpan` unter der Zeile.

### „Nichts" ist an beiden Feldern eine Wahl und kein Zustand, in den man fällt

Das ist der eine Gedanke, aus dem der Rest folgt.

**Der Partner hat eine Leeren-Schaltfläche**, und wenn das Feld leer *ist*, sagt ein Satz darunter,
was das Speichern dann bedeutet: *„Gespeichert heißt hier: hingesehen, es gibt keinen Partner."* Er
erscheint erst dann — neben einem gefüllten Feld wäre er Rauschen.

> **In jeder anderen Oberfläche heißt ein leeres Feld „noch nicht ausgefüllt".** Hier ist es eine
> Angabe, und zwar die einzige, die ein toter Prozess je bekommt. Ohne die Schaltfläche müsste man
> den Vorschlag markieren und löschen — und niemand käme auf die Idee, dass das erlaubt ist.

**Die Richtung hat drei Knöpfe statt zwei.** Der dritte heißt „keine". Zwei Knöpfe mit Abwahl beim
zweiten Druck sagen dasselbe und sagen es niemandem. Im Code trägt er den Wert `OHNE_RICHTUNG` und
nicht die leere Zeichenkette: Die meldet eine `ToggleGroup` beim Abwählen, und beides wäre dann
dasselbe Zeichen für zwei verschiedene Dinge.

**Ein dritter Pflegestatus entsteht dadurch ausdrücklich nicht** (E4, §9 der Festlegung):
„gepflegt mit leerem Partner" sagt es mit den Feldern, die ohnehin da sind.

### Das Partnerfeld ist frei tippbar (E21)

Die Vorschlagsliste speist sich aus den bereits gepflegten Zeilen desselben Mandanten (E2). Eine
geschlossene Auswahl wäre deshalb ein Kreis ohne Eingang: **Der erste Partner eines Mandanten stünde
nie darin**, und die Kuratierung käme nie in Gang. Die Vorschläge sind ein **Geländer** gegen
Schreibvarianten — `BAYER` neben `Bayer` neben `BAYER AG` —, keine Schranke.

**Von Hand gebaut, nach dem einen Muster, das der Bestand kennt** (`prozess-filter.tsx`): Eingabefeld
plus gefilterte Liste.

- **Kein `Command`/cmdk.** Es brächte eine neue Abhängigkeit für einen einzigen Fall, und sein
  Eingabefeld trägt den *Suchbegriff* und nicht den Wert. Genau das ist hier verkehrt herum: **Das
  Feld ist der Wert**, die Liste hilft nur beim Treffen.
- **Kein `Popover`.** Er nimmt dem Feld den Fokus, und ein Auswahlfeld, in dem man nicht tippen kann,
  während die Liste offen ist, ist keins.

Tastatur: Pfeil ab öffnet und wandert, Pfeil auf zurück — bis **über** den ersten Eintrag hinaus, und
dann ist wieder der getippte Text der gewählte Wert. Enter übernimmt den hervorgehobenen Vorschlag,
ohne das Formular abzuschicken; ohne Hervorhebung schickt es ab, denn dann steht der gemeinte Wert
schon da. **Escape schließt die Liste und hält an** — das zweite Escape bricht die Bearbeitung ab.
Wer eine Liste offen hat, meint mit „weg damit" die Liste. Die Einträge reagieren auf `mousedown` und
nicht auf `click`, sonst nähme der Klick dem Feld vorher den Fokus und `onBlur` schlösse die Liste
unter dem Zeiger weg.

### Ungespeicherte Änderungen werden nie stillschweigend verworfen

**Zwei Sperren, und sie sind dieselbe Zusage von zwei Seiten:**

| Sperre | Warum |
|---|---|
| Keine zweite Zeile öffnen (`darfOeffnen`) | Der Entwurf lebt im Komponentenzustand; eine zweite zu öffnen hieße, den ersten zu verwerfen |
| Filter, Lauf und Massenzuordnung gesperrt | Alle drei holen die Liste neu, und die offene Zeile könnte dabei aus der Antwort fallen |

Der Satz *„Solange diese Zeile offen ist, lässt sich keine zweite öffnen"* steht **einmal** neben dem
Formular und erklärt damit alle ausgegrauten Schaltflächen der Liste auf einmal. Als `title` an jeder
einzelnen stünde er dort, wo ihn niemand sucht — und an einer deaktivierten Schaltfläche zeigen ihn
manche Browser gar nicht erst.

**Abbrechen und Escape sind ausdrückliche Anweisungen und brauchen keine Rückfrage.** Was die Regel
ausschließt, ist das Verwerfen *ohne Anlass*.

### Der offene Zeilenschlüssel steht nicht in der URL

Der Zweischritt aus [`frontend-grundlagen.md`](frontend-grundlagen.md) §8, und diesmal endet er in
der *zweiten* Verzweigung:

1. **Drückt die URL diesen Zustand aus?** Die Kennung ja — der getippte Text nicht.
2. **Ist er wirklich ein Filterzustand?** *Nein.* Er beschreibt keinen Ausschnitt, sondern eine
   **begonnene Eingabe**. Und die Hälfte, auf die es ankommt, lässt sich ohnehin nicht teilen: Ein
   geteilter Link zeigte dem Empfänger ein leeres Formular über einer Zeile und behauptete damit
   etwas, das der Absender nie gesehen hat.

Also Komponentenzustand — und die ableitende Regel als reine Funktion daneben (`darfOeffnen` in
`features/katalog/zuordnung.ts`), genau wie `angezeigterModus` in `lib/filter.ts`.

### Die Antwort geht in den Zwischenspeicher, die Liste wird nicht neu geholt

`setQueriesData` über den **Präfix**, also über beide Filterstellungen zugleich. Nur die gerade
sichtbare zu setzen hieße, dass ein Haken hin und zurück die alte Zeile wieder hervorholt — ohne
Anfrage und ohne Hinweis.

**Die gespeicherte Zeile bleibt stehen**, obwohl sie jetzt `GEPFLEGT` ist und in eine
`nurOffene`-Liste streng genommen nicht mehr gehört. Sie unter der Hand verschwinden zu lassen wäre
falsch: Der Nutzer hat gerade dort gearbeitet, und das Wort „gepflegt" in der Zeile **ist** die
Bestätigung, dass es geschrieben wurde. Beim nächsten Holen ist sie fort, und dann als Folge einer
Abfrage und nicht als Zaubertrick.

**Die Partnerliste wächst mit.** Wer zwanzig Zeilen demselben neuen Partner zuordnet, bekommt ihn ab
der zweiten vorgeschlagen. Der umgekehrte Fall wird **nicht** nachgeführt: Wer den letzten Partner
eines Namens entfernt, sieht ihn bis zum nächsten Holen weiter in der Auswahl. Das ist in Kauf
genommen — ein Name zu viel kostet nichts, ein fehlender Name kostet einen Tippfehler.

### Fehler stehen am Formular

`partner-zu-lang` und `richtung-unbekannt` sind übersetzt und erscheinen **im Formular**, nicht über
der Ansicht: Die Liste ist richtig, nur diese eine Eingabe nicht. Ohne „Erneut versuchen" — der
Nutzer schickt selbst noch einmal ab, und für `partner-zu-lang` ändert ein zweiter Versuch mit
demselben Wert ohnehin nichts.

**Ein `404` sagt niemals etwas über Berechtigung.** Eine `processId` außerhalb des aktiven Mandanten
ergibt `404` und ist von einer erfundenen Kennung nicht zu unterscheiden.

---

## 6. Der Lauf, und der Handlauf daran

`POST /api/katalog/vorschlagen`, leerer Körper. Der Endpunkt fährt drei Schritte in **einer**
Transaktion (E13, E14, E15) und antwortet erst am Ende.

### Kein Fortschrittsbalken

Ein Balken verspricht **bekannten** Fortschritt. Dieser Aufruf hat keinen: Er hat kein Zeitlimit und
meldet nichts zwischendurch ([`prozess-katalog-backend.md`](prozess-katalog-backend.md) §10,
Punkt 7). Ein Balken, der sich nach Gefühl füllt, ist eine Behauptung über etwas, das niemand misst.

**Sichtbar ist stattdessen die verstrichene Zeit.** Sie behauptet nichts, sie berichtet — und sie ist
genau die Angabe, die der Nutzer braucht, um zu entscheiden, ob er wartet.

Der Zähler ist `aria-hidden`: Eine neue Ansage je Sekunde machte den Lauf für ein Vorleseprogramm
unbenutzbar. Dass etwas läuft, sagt `aria-busy` am Container und der gesperrte Knopf.

### Die Dauer bleibt nach dem Lauf stehen — sie ist die Messung

Der Schreibweg ist **ungemessen**: bis zu 733 Zeilen je Druck, und §10 dort nennt für die Testkopie
10 bis 25 s je `COMMIT` gegen M80s Schranke von unter zehn Sekunden für 1.490 Zeilen. Wer den Knopf
drückt, liest die Zahl in der Oberfläche ab.

> ### Der Handlauf mit vorregistrierter Deutung
>
> | Dauer, `NEXANS`, 733 Zeilen | Was daraus folgt |
> |---|---|
> | unter **10 s** | Synchron bleibt. M80s Schranke gilt, die 10–25 s waren ein Sonderfall und werden als solcher benannt |
> | **10 bis 60 s** | Synchron bleibt, aber die Zeitgrenzen der ganzen Kette sind zu prüfen: Backend, BFF, Reverse Proxy |
> | über **60 s** oder Abbruch | Die Bauform fällt. Ein Hintergrundlauf mit Zustandsabfrage ist ein eigener Auftrag |
>
> **Die Zahl ist noch nicht genommen.** Sie braucht ein laufendes Backend und eine Anmeldung mit der
> Rolle `ADMIN` und gewähltem Mandanten `NEXANS`. Sie steht als offener Punkt in §12 und ist
> **vor der Abnahme dieses Schritts** nachzutragen.

### Alle acht Zahlen, auch die Nullen

`angelegt`, `aufgefrischt`, `unberuehrt`, `regelA`, `regelB`, `keine`, `bestandGeprueft`,
`ohneNachrichten` — in drei Gruppen, so wie das Backend sie zählt.

**Ein Lauf, der nichts bewegt hat, muss von einem erfolgreichen unterscheidbar sein.** Ohne die
letzten beiden sähe ein Bestandslauf, der wegen eines Fehlers null Zeilen anfasst, genauso aus wie
einer, der 733 aufgefrischt hat.

### Danach wird die Liste neu geholt

**Invalidiert und nicht gesetzt** — anders als bei der Zuordnung und aus dem umgekehrten Grund: Dort
halten wir die geänderte Zeile in der Hand, hier hat sich die Liste **in der Breite** geändert. Aus
acht Zahlen lässt sich keine Liste rekonstruieren.

---

## 7. Die Massenzuordnung

`POST /api/katalog/massenzuordnung` in zwei Schritten. **Der Modus wird immer ausdrücklich
mitgeschickt**, auch wenn er der Vorgabe entspricht: Sich darauf zu verlassen hieße, die harmloseste
Wirkung dem Weglassen zu überlassen, und beim nächsten Umbau stünde dort vielleicht etwas anderes.

**Ein Dialog, und der einzige der Ansicht.** Die Zeilenbearbeitung meidet ihn ausdrücklich — sie
braucht die Nachbarzeilen. Diese Handlung braucht das Gegenteil: Sie trifft bis zu 226 Zeilen auf
einmal, und wer sie auslöst, soll für einen Moment nichts anderes tun.

`components/ui/dialog.tsx` ist dafür über den Generator dazugekommen (§13). **Ohne seine
Schließen-Schaltfläche** (`showCloseButton={false}`): Sie trägt eine feste englische Zeichenkette —
dieselbe Klasse, die [`frontend-grundlagen.md`](frontend-grundlagen.md) §10 für `sheet.tsx` als
offenen Punkt führt. Statt den Generatorbereich von Hand zu ändern, wird sie weggelassen; das
Formular hat seinen eigenen Abbrechen-Knopf, und `Escape` schließt weiterhin.

### Die Vorschau nennt die Zahl, die verloren geht — in Worten

`betroffen` **und** `davonGepflegt`. Die zweite ist die eigentliche Auskunft und steht deshalb als
**Satz** da und nicht als Zahl in einer Tabelle:

> *„Betrifft 226 Prozesse. 14 davon sind bereits gepflegt — ihr bisheriger Wert geht verloren."*

Dass gepflegte Zeilen überschrieben werden, **ist gewollt** (E12): Ein Schutzmodus „nur offene
Zeilen" machte genau die Korrektur unmöglich, für die man das Werkzeug braucht — einem ganzen Projekt
einen falschen Partner in einem Zug richtigzustellen.

**„Ausführen" ist gesperrt, bis eine Vorschau vorliegt**, und **jede Änderung am Formular verwirft
sie**. Sonst bestätigte der Nutzer eine Zahl, die zu einer anderen Anfrage gehört — genau der Fehler,
den das gemeinsame Statement im Backend auf seiner Seite ausschließt.

### Das Projekt ist ein natives Auswahlfeld

Es muss **existieren** — frei tippbar wie der Partner wäre es ein sicheres `404`. Ein eigener
Baustein dafür entsteht nicht: Der Bestand kennt keinen `Select`, die Liste ist je Mandant höchstens
39 Einträge lang, und ein natives Feld bedient sich am Finger und mit der Tastatur besser als jeder
Nachbau. Die Optionen tragen Kennung, Namen und Zeilenzahl.

Das Partnerfeld aus §5 wird wiederverwendet, **samt seinem Weg zum leeren Wert** — ein leerer Wert
löscht das Feld in allen betroffenen Zeilen, und der Satz dazu steht da.

---

## 8. Fortschritt und Hinweis

### Der Fortschritt ist eine Zahl und kein Balken (E18)

*„412 von 733 Prozessen gepflegt · 56 % — tote Prozesse und der Auffangprozess zählen mit."*

Ein Balken ist hier **nicht** verboten: Anders als beim Lauf gibt es sehr wohl bekannten Fortschritt.
Er wäre trotzdem falsch. Bei `VOTG` stünde er lange bei einem Zehntel, und eine breite, fast leere
Fläche liest sich als Alarm. Die Zahl sagt dasselbe nüchtern, und nüchtern ist hier richtig — sie ist
die Wahrheit über eine Arbeit, die eben lange dauert.

**Der Satz daneben ist nicht Beiwerk.** Ohne ihn liest jemand die schlechte Zahl bei `VOTG` als
Fehler, wo 350 von 390 Prozessen schlicht keine Nachricht tragen.

**Der Nenner sind alle.** E14 teilt ihn ausdrücklich nicht: Eine zweite Quote „gepflegt unter denen
mit Nachrichten" stünde als bequemere Zahl neben der richtigen und wäre binnen einer Woche die
berichtete — bei `VOTG` sähe die Kuratierung dann zu 89,74 % fertig aus, während 350 Zeilen
unbearbeitet stehen.

Der Anteil wird über `Intl` formatiert (`lib/format.ts` `formatiereAnteil`) und nicht von Hand: Das
Prozentzeichen steht nicht in jeder Sprache an derselben Stelle — deutsch `56 %`, englisch `56%` —,
und eine selbst zusammengesetzte Zeichenkette wäre in genau einer der beiden Sprachen falsch, und
zwar unauffällig.

### Der Hinweis über der Liste (E17)

Wortlaut: *„für keinen Prozess konnte ein Partner vorgeschlagen werden"*, dazu ein Satz, was daraus
für die Arbeit folgt.

**Die Bedingung wird aus den Daten gerechnet, niemals aus einer Mandantenliste im Code:** keine Zeile
trägt eine `vorschlagHerkunft` außer `KEINE`.

> **Zur Kontrolle sind es fünf Mandanten** — `SUTTONS`, `ZAST`, `WOC`, `SYSTEM` und `NXHBE`. Die
> früheren Listen mit vier zählen etwas anderes, nämlich Mandanten ohne *jede* Ableitung; `NXHBE` hat
> eine Richtung, aber für keinen seiner 17 Prozesse einen Partner. **Die fünf sind Erwartung zum
> Nachprüfen und kein Datenbestand zum Einbauen.**

**Er hängt am Partner allein.** Die Richtung ist der leichtere Teil der Kuratierung, und ein Hinweis,
der wegen einer gefüllten Richtung verschwindet, verschwände genau dort, wo die eigentliche Arbeit
noch aussteht.

**Er bleibt stehen, auch wenn ein Mensch inzwischen jeden Partner von Hand eingetragen hat.**
`vorschlagHerkunft` beschreibt, welche Regelfassung diese Zeile einmal vorgeschlagen hat, und wird
durch eine Kuratierung nicht falsch, sondern historisch. Vorgeschlagen hat den Partner trotzdem
keiner.

Er ist eine Auskunft und kein Fehler und trägt deshalb die **neutrale** Fassung von `Alert`; die rote
gehört dem Fehlerzustand.

---

## 9. Die vier Zustände — und die Reihenfolge, in der sie hier stehen

[`frontend-grundlagen.md`](frontend-grundlagen.md) §5 nennt vier Zustände und **keine Reihenfolge**:
Die Nachrichtenliste prüft den Fehler zuerst, der Eigenschaftenblock das Laden. Hier steht **„kein
Zugriff" vor allem anderen und außerhalb der Kette** — er nimmt alles Bedienbare mit weg. Wer die
Liste nicht sehen darf, hat nichts zu filtern, nichts zu erheben und nichts zuzuordnen.

Danach die übliche Kette: **Laden, Fehler, Leer, Daten.**

### „Leer" hat drei Ursachen und sagt jede einzeln

| Lage | Was dasteht |
|---|---|
| Antwort leer, `nurOffene` gesetzt | *„Jede Zeile dieses Mandanten ist gepflegt."* — der Erfolgsfall, und er sieht auch so aus |
| Antwort leer, ohne Filter | *„Für diesen Mandanten sind keine Prozesse hinterlegt."* |
| Antwort voll, Browserfilter leert sie | *„Kein Prozess passt zu den gesetzten Filtern."* |

Die drei zu verschmelzen hieße, dem Nutzer im Erfolgsfall dasselbe zu sagen wie bei einem leeren
Mandanten — und ihn im dritten Fall den Bestand verdächtigen zu lassen, obwohl sein eigener Haken die
Ursache ist. **„Leer" ist kein Fehler und sieht auch nicht so aus.**

---

## 10. Tests

`pnpm test`, nach [`frontend-grundlagen.md`](frontend-grundlagen.md) §9. Ein `console.error` lässt
den Lauf fehlschlagen.

| Datei | Art | Was |
|---|---|---|
| `tests/katalogfilter.test.ts` | rein | die zwei Filter ↔ URL, auch in Kombination; der übergangene unbrauchbare Wert; **keine Vorgabe in der URL**; **`null` bleibt bei aktivem Filter sichtbar** samt Gegenprobe; der Auffangprozess an `Undefined` mit drei Gegenproben zu `^0+_` |
| `tests/katalog.test.ts` | rein | der **leere Partner** als `null` (Feld, Leerraum, Massenzuordnung); `darfOeffnen` in allen drei Lagen; die Antwort im Zwischenspeicher; die Vorschläge; der **Fortschritt** über eine bekannte Liste; die **Hinweisbedingung** je einmal erfüllt und nicht erfüllt, dazu „hängt am Partner allein" und „bleibt nach Handarbeit stehen"; die Projektauswahl; **„kein Zugriff" bei `403`** und bei keinem der beiden anderen `403`; die Fehlerabbildung für `partner-zu-lang` und `richtung-unbekannt` in beiden Sprachen |
| `tests/katalog-tabelle.test.tsx` | **gerenderter Baum**, fünf Fälle | dass **`false` und `null` drei verschiedene Sätze ergeben** und eine nie geprüfte Zeile nicht den Satz der toten trägt; dazu die **Verdrahtung** der Sperre aus E19 |
| `tests/format.test.ts` | rein, ergänzt | **Anteile** über `Intl`: deutsch `56 %`, englisch `56%`, auf ganze Prozent gerundet |

### Warum `katalog-tabelle.test.tsx` einen Baum rendern darf

Die Bedingung ist nicht „ein Baum wäre bequemer", sondern **„es gibt keinen anderen Ort, an dem der
Satz belegbar wäre"**. Beide Fälle erfüllen sie:

1. **`false` und `null` sagen Verschiedenes.** Die Unterscheidung entsteht erst in der Zelle:
   `zeile.traegtNachrichten ? A : B` ist die naheliegende Schreibweise und trifft beide im selben
   Zweig. Keine reine Funktion fängt das — der Filter hält die Zeilen auseinander, die *Anzeige* muss
   es getrennt noch einmal tun.
2. **Die Verdrahtung der Sperre.** Die *Regel* ist eine reine Funktion (`darfOeffnen`); was der Baum
   belegt, ist, dass die andere Zeile ihre Schaltfläche wirklich gesperrt bekommt und die offene sie
   durch das Formular ersetzt. **Eine richtige Regel, die niemand abfragt, sieht von außen aus wie
   keine.**

Die Gesamtzahl der gerenderten Fälle steht **ausschließlich** im Kopf von `frontend/vitest.config.mts`
und ist dort von 34 auf **39** in acht Dateien fortgeschrieben.

### Alle sichtbaren Texte stehen in den Sprachdateien

Zwei neue Zweige auf oberster Ebene, `administration` (die Möblierung des Bereichs) und `katalog`
(das Feature selbst), dazu `zustand.keinZugriffTitel` und vier neue Fehlerschlüssel. **Keiner steht in
einer Komponente.**

---

## 11. Abweichungen von der Vorgabe und vom Auftrag

**1. Teil 1a des Auftrags trifft auf einen anderen Bestand, als er annimmt.** Er trägt auf, „den
erfundenen Pfad `/verwaltung/…` in `docs/frontend-grundlagen.md` und in die E-Texte" zu korrigieren.
Gemessen am Repository:

- `/verwaltung` steht im ganzen Repository an **genau einer** Stelle — im Korrekturkasten von
  [`frontend-grundlagen.md`](frontend-grundlagen.md) §2, also dort, wo er *gemeldet* und nicht
  behauptet wird. Eine Code-Fundstelle gibt es nicht.
- **Die E-Texte tragen ihn nicht.** [`prozess-katalog.md`](prozess-katalog.md) nennt in keiner seiner
  einundzwanzig Entscheidungen eine Route.

Zu korrigieren war deshalb die **Benennung** und nicht ein falscher Pfad: Der Abschnitt hieß „Der
Verwaltungsbereich" und heißt jetzt „Der Administrationsbereich", und der Korrekturkasten hält die
getroffene Entscheidung samt altem Wortlaut fest.

**2. Teil 1b war ohne Änderung bereits erfüllt.** Der Auftrag sagt,
[`PROJEKTBESCHREIBUNG.md`](PROJEKTBESCHREIBUNG.md) §7 führe „**Genau zwei Ausnahmen**". Er führt seit
dem 20.08.2026 **drei**, nennt `PUT /api/admin/users/{id}/tenants`, trägt den Satz zur **vierten**
Ausnahme und einen datierten Korrekturkasten mit dem alten Wortlaut. Der Pfad ist gegen den Code
geprüft — `BenutzerverwaltungController` mappt `@PutMapping("/{id}/tenants")` —, und
`{benutzername}/mandanten` steht nur noch in den zitierten alten Wortlauten dreier Dateien.
**Gemeldet, nicht stillschweigend als Arbeit ausgegeben.**

**3. `lib/http.ts` bekommt einen `PUT`-Weg.** Der Auftrag nennt ihn nicht; ohne ihn wäre
`PUT /api/katalog/prozesse/{processId}` nicht aufrufbar. Dieselbe Lage wie im Backend, wo
`SicherheitsTestbasis` am 20.08.2026 einen `PUT`-Helfer bekam
([`prozess-katalog-backend.md`](prozess-katalog-backend.md) §9, Abweichung 7).

**4. `lib/format.ts` bekommt `formatiereAnteil`.** Für die Prozentangabe des Fortschritts. Die
Begründung steht in §8; eine selbstgebaute Zeichenkette wäre in genau einer der beiden Sprachen
falsch.

**5. Die Ansicht hält zwei Listen, sobald `nurOffene` gesetzt ist.** Der Auftrag nennt `nurOffene`
als Backend-Parameter und den Fortschritt als „aus der vollen Liste gezählt". Beides zugleich geht
nur so. Die volle Liste ist dieselbe Abfrage, solange der Haken nicht gesetzt ist. Vollständig
begründet in §4.

**6. `/administration` bekommt eine Übersicht statt einer Weiterleitung.** Der Auftrag sagt „ein
Navigationseintrag, zwei Unterseiten" und nichts darüber, was die Basisroute zeigt. Eine
Weiterleitung auf die Katalogpflege machte die Benutzerverwaltung nur über die Adresszeile
erreichbar; die Übersicht nennt beide Bereiche mit ihrer Aufgabe.

**7. Die Filter und der Lauf sind gesperrt, solange eine Zeile bearbeitet wird.** Der Auftrag
verlangt nur, dass sich keine zweite Zeile öffnen lässt. Alle drei holen die Liste neu, und die
offene Zeile könnte dabei aus der Antwort fallen — das wäre genau das stillschweigende Verwerfen, das
E19 ausschließt.

**8. Die Dauereinheiten stehen jetzt zweimal in den Sprachdateien.** Unter `nachrichten.detail.dauer`
und unter `katalog.lauf.dauer`. Sie gehören keiner Ansicht und müssten auf die oberste Ebene — das
fasst die Schlüssel des Nachrichtendetails an und ist eine eigene Runde. Als offener Punkt vermerkt.

**9. `components/ui/dialog.tsx` ist über den Generator dazugekommen.** Der Auftrag nennt vier
mögliche Bausteine (Popover, Command, Dialog, Tabelle). **Popover und Tabelle waren bereits da**,
`Command` wird nicht gebraucht (die Begründung steht in §5), und `Dialog` fehlte. Erzeugt ist deshalb
genau einer. Die Nebenwirkung des Generators auf `button.tsx` war reine Formatierung und ist
zurückgenommen worden.

---

## 12. Offene Punkte

1. **Der Handlauf zu Teil 5 ist offen: die Dauer des Laufs ist nicht gemessen.** Sie braucht ein
   laufendes Backend und eine Anmeldung mit der Rolle `ADMIN` und gewähltem Mandanten `NEXANS`. Die
   Oberfläche zeigt sie an; genommen und hier eingetragen ist sie **nicht**. Die vorregistrierte
   Deutung steht in §6 und gilt unverändert — **über 60 s oder Abbruch heißt: die Bauform fällt**.
2. **Eine Sichtprüfung im Browser steht aus** (§13 der Vorbereitungsliste in
   [`README.md`](README.md)). Sie ist bei dieser Ansicht besonders fällig: Die klebende Kopfzeile
   über 733 Zeilen, das aufklappende Zeilenformular und die Vorschlagsliste über den Zeilen darunter
   sind drei Dinge, die sich nur ansehen lassen.
3. **Das schmale Fenster ist ungesehen.** `resize_window` wirkt nicht
   ([`frontend-grundlagen.md`](frontend-grundlagen.md) §8) — geprüft ist das Regelwerk, nicht die
   Darstellung. Zu sehen sind hier drei Dinge: ob die Tabelle bei 360 px mit drei Spalten trägt, ob
   das Zeilenformular dort gestapelt lesbar bleibt, und ob die Vorschlagsliste des Partnerfeldes
   nicht aus dem Bild läuft.
4. **Die Dauereinheiten stehen zweimal** (§11, Punkt 8). Sie gehören auf die oberste Ebene der
   Sprachdateien.
5. **Ein entfernter Partner bleibt bis zum nächsten Holen in der Auswahl** (§5). In Kauf genommen;
   die Vorschläge sind ein Geländer und keine Schranke.
6. **Es gibt keine Rückfrage beim Verlassen der Seite mit offener Zeile.** Ein `beforeunload` würde
   den Fall abdecken, in dem jemand mit ungespeichertem Entwurf die Adresszeile benutzt. Innerhalb
   der Anwendung ist er gedeckt — Filter, Lauf und Massenzuordnung sind gesperrt —, außerhalb nicht.
   Nicht gebaut, weil der Auftrag es nicht verlangt und ein `beforeunload` eine
   anwendungsweite Entscheidung wäre.
7. **Die Reihenfolge der Partnervorschläge folgt `localeCompare`**, das Backend liefert sie nach
   `utf8mb4_general_ci`. Die beiden sind einander nahe, aber nicht dasselbe. Nur ein neu getippter
   Name wird lokal einsortiert; er kann bis zum nächsten Holen eine Position danebenstehen.

---

## 13. Die Dateien

```
frontend/src/
├─ app/(app)/administration/
│  ├─ layout.tsx                     Unternavigation, keine Überschrift
│  ├─ page.tsx                       Übersicht beider Bereiche
│  ├─ katalog/page.tsx               rendert `KatalogAnsicht`
│  └─ benutzer/page.tsx              Platzhalter (9a)
├─ components/
│  ├─ bereichs-navigation.tsx        zwei Links, dieselbe Gestalt wie die Hauptnavigation
│  ├─ kein-zugriff.tsx               der Zustand aus dem `403`
│  └─ ui/dialog.tsx                  Generatorbereich, neu
├─ features/katalog/
│  ├─ api.ts                         Typen, fünf Aufrufe, `KATALOG_SCHLUESSEL`
│  ├─ filter.ts                      nuqs-Parser, Rundlauf, `istAuffangprozess`, `sichtbareZeilen`
│  ├─ zuordnung.ts                   die Entscheidungen der Zeilenbearbeitung
│  ├─ kennzahlen.ts                  Fortschritt, Hinweisbedingung, Projekte
│  ├─ hooks.ts                       Filterbindung, zwei Abfragen, drei Mutationen
│  └─ components/
│     ├─ katalog-ansicht.tsx         die Zustände, die zwei Listen, der offene Zeilenschlüssel
│     ├─ katalog-kennzahlen.tsx      Fortschritt und Hinweis
│     ├─ katalog-filterleiste.tsx    die zwei Kontrollkästchen
│     ├─ katalog-tabelle.tsx         klebende Kopfzeile, die zweite `<tr>`
│     ├─ katalog-zeile.tsx           die Zellen einer Zeile
│     ├─ zeilen-formular.tsx         Bearbeitung in der Zeile
│     ├─ partner-feld.tsx            frei tippbar, mit Vorschlägen
│     ├─ lauf-knopf.tsx              der Lauf, die verstrichene Zeit, acht Zahlen
│     └─ massenzuordnung.tsx         Dialog mit Vorschau
├─ lib/
│  ├─ http.ts                        `aendere` (PUT), `istKeinZugriff`
│  ├─ routen.ts                      zwei Unterrouten
│  ├─ navigation.ts                  `ADMINISTRATION`, `istAktiverBereich`
│  └─ format.ts                      `formatiereAnteil`
└─ i18n/{de,en}.ts                   zwei neue Zweige, ein Zustandstext, vier Fehlerschlüssel
```

---

## 14. Regelbezug

| Regel | Umsetzung |
|---|---|
| **M1** Kein Endpunkt nimmt eine Mandanten-ID entgegen | Keiner der fünf Aufrufe in `features/katalog/api.ts` kennt einen Mandantenparameter; der Mandant kommt aus der Sitzung |
| **M3** Der Filter ist Bestandteil jedes Statements | Sache des Backends. Die Oberfläche filtert nichts nach und kennt keine Regel darüber, wer was sehen darf |
| **Q4** Nicht zugeordnet heißt nicht zugeordnet | Die drei Zustände von `traegtNachrichten` werden nie zusammengefasst; „kein Vorschlag ableitbar" steht als eigener Vermerk; der Auffangprozess wird über die gemessene Kennung erkannt und nicht über ein geratenes Muster |
| **L1** Pflicht-Zeitfenster je Listen-Endpunkt | Gilt hier nicht: Der Katalog ist keine Nachrichtenliste und hat kein Zeitfenster (E8). Deshalb liegt sein Filter im Feature und nicht in `lib/filter.ts` |
| **L2** Keine Live-Aggregation über `Message` | Die Oberfläche aggregiert nichts; sie zählt eine Liste, die sie ohnehin hält (E8) |
| **Z1** Kein direkter `now()`-Aufruf | Betrifft das Backend. Im Browser wird für die **Dauer** des Laufs die Browseruhr gelesen — eine Dauer und kein fachlicher Zeitpunkt. Zeitpunkte laufen weiter über die Anzeigezone aus der Selbstauskunft |
| Farben nur über Tokens | Diese Ansicht führt **keine** Farbrolle ein und nutzt keine; `tests/farbwerte.test.ts` deckt die neuen Dateien mit ab |
