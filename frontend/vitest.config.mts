import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

/**
 * Bewusst klein: kein React-Plugin, keine Testing Library.
 *
 * Geprüft werden die Entscheidungen, nicht das Markup — der Ablauf nach dem
 * Anmelden, die Gleichheit beider Sprachdateien, die Wortwahl bei 404, das
 * Leeren des Zwischenspeichers und die Zeitstempel ohne Zeitzonenverschiebung.
 * Das sind alles reine Funktionen. Ein gerenderter Baum brächte hier nichts
 * außer Laufzeit und Abhängigkeiten.
 *
 * **Die Ausnahmen sind gezählt, nicht gewachsen** — Stand 16.09.2026 sind es
 * **einhundertfünfundfünfzig in zwanzig Dateien**. Diese Zahl wird an genau dieser Stelle geführt;
 * `tests/hilfe/rendern.tsx` und `docs/frontend-grundlagen.md` §9 verweisen
 * darauf, statt sie zu wiederholen (drei Orte für dieselbe Zahl sind zwei zu
 * viel):
 *
 * | Datei | Fälle | Warum ein Baum |
 * |---|---|---|
 * | `tests/detail-baum.test.tsx` | 3 | zwei Sätze, die von Hand grundsätzlich nicht zu sehen sind, und die Regression zum Doppelschlüssel |
 * | `tests/ansicht-umschalter.test.tsx` | 2 | die Sichtbarkeitsregel des Umschalters **ist** eine Klasse, und ihr Umbruchpunkt ist von Hand nicht prüfbar (`docs/frontend-grundlagen.md` §7); dazu, dass er im Panel und auf der eigenen Route Verschiedenes sagt. **Am 18.08.2026 von 1 auf 2 berichtigt** — die Datei trug seit Schritt 6 zwei Fälle, die Zählung nur einen. Genau die Drift, gegen die die Regel „an einer Stelle geführt" gerichtet ist |
 * | `tests/bam-block.test.tsx` | 4 | zweimal eine Aussage über **Abwesenheit** (kein Block und keine Anfrage bei `bamAnzahl === 0`, und immer noch keine, solange niemand aufklappt), die Regression zum Schlüssel `(typ, wert)` — und seit dem 13.08.2026 die **Fuge** der zerlegten Beschriftung: ob zwischen Name und Endung ein Leerzeichen entsteht, entscheidet JSX und keine Funktion |
 * | `tests/suche-marken.test.tsx` *(13.08.2026, erweitert 08.09.2026)* | 6 | die Regression zum Schlüssel `(typ, wert)` an den Marken, der unbekannte Typ als Nummer, und zweimal eine Regel, die **selbst** eine Klasse plus ein `title` ist: die Längenregel der Trefferspalte und der Kettenhinweis. **Seit Teil 2 der Property-Suche zwei mehr:** dieselbe Parameterform als BAM- und als Feld-Marke **ohne doppelten React-Schlüssel** (der Schlüssel trägt die Art voran), und eine Aussage über **Abwesenheit** — ohne Belegnummer gibt es die Spalte „Treffer" nicht, weder Überschrift noch Zelle (E‑110) |
 * | `tests/eigenschaften-block.test.tsx` *(17.08.2026)* | 4 | die Regression zum Schlüssel `${position}:${name}` — derselbe Name in drei Gruppen **ohne `console.error`** —, zweimal eine Aussage über **Abwesenheit** (kein Schalter und keine Anfrage bei `anzahl === 0`, und immer noch keine, solange niemand aufklappt), und der Rückfall „Schritt N", der erst im Baum entsteht |
 * | `tests/artefakt-ansicht.test.tsx` *(18.08.2026)* | 9 | **der Textknoten** — ob aus `<b>fett</b>` ein Element wird oder Text, entscheidet React beim Rendern und keine Funktion; derselbe Test belegt die Bauvorgabe aus M60 (**ein** Kind, kein Element je Zeile). Dazu die **vier Zustände** aus `docs/rohdaten.md` §8, je einer: „keiner ist ein leeres Feld" ist eine Aussage über Anwesenheit von Text und Abwesenheit des Inhaltsfelds. Dazu der Ausschnitt-Vermerk in **beide** Richtungen, der Download-Knopf (Entscheidung 9: Ein Knopf, der etwas anderes verspricht als die Anzeige, **darf nicht im Baum stehen**) und die Beschriftung ohne Nachladen |
 * | `tests/zeitleiste-ziele.test.tsx` *(18.08.2026, Nachbesserung)* | 8 | An die Stelle von `tests/dateien-block.test.tsx` getreten, als der eigene Dateienblock entfiel. Fünf Aussagen, die kein reiner Aufruf trägt: **welche Zeile welches Ziel bekommt** in den drei Lagen (beide Arten, nur eine, keine); dass die Artefakte des **Metadaten-Schritts** über der Leiste erreichbar bleiben, obwohl die Leiste Schritt `0` nicht führt (`docs/nachrichtendetail.md` §4) — eine Aussage über Anwesenheit an einer Stelle ohne Zeile; die **Belastungsprobe aus M55** mit fünfzehn eigenen Zielen und **ohne doppelten React-Schlüssel**; das **Anspringen** der Eigenschaftengruppe, das auf `document.activeElement` endet und damit auf einem Zustand des Dokuments; und die Gegenprobe dazu — ohne Eigenschaften **kein Schalter** am Schrittnamen |
 * | `tests/katalog-tabelle.test.tsx` *(24.08.2026)* | 5 | Zwei Aussagen über **Anwesenheit und Abwesenheit im Baum**, und beide gehören zur Katalogpflege. Erstens: **`false` und `null` sagen Verschiedenes** — `zeile.traegtNachrichten ? A : B` ist die naheliegende Schreibweise und trifft beide im selben Zweig; keine reine Funktion fängt das, denn der Filter hält die Zeilen auseinander und die *Anzeige* muss es getrennt noch einmal tun (`docs/prozess-katalog.md` E14). Zweitens die **Verdrahtung** der Sperre aus E19: dass die andere Zeile ihre Schaltfläche wirklich gesperrt bekommt, dass die offene sie durch das Formular ersetzt, und die Gegenprobe ohne offene Zeile. Die *Regel* dahinter ist eine reine Funktion (`darfOeffnen`) — eine richtige Regel, die niemand abfragt, sieht von außen aus wie keine |
 * | `tests/benutzer-tabelle.test.tsx` *(24.08.2026, erweitert 15.09.2026)* | 11 | **Seit dem 15.09.2026 einer mehr:** Die Baumgliederung am eigenen Konto schickt ihren Aufruf **ohne** Dialog und führt **nicht** auf die Anmeldung (`docs/benutzerverwaltung.md` E26) — Verdrahtung und Abwesenheit zugleich; die Gegenprobe steht am Passwort. Drei Klassen, und beide gehören zur Benutzerverwaltung. **Vier Aussagen über Anwesenheit und Abwesenheit im Baum**, je mit Gegenprobe: `lastLogin = null` steht als „nie angemeldet" da und nicht als leere Zelle (`docs/benutzerverwaltung.md` E17) — der Unterschied zwischen einer Auskunft und einer fehlenden Angabe entsteht erst in der Zelle, und es gibt nichts zu rechnen, nur etwas hinzuschreiben; und die **zwei Sperren stehen in zwei Zellen und werden nie zu einer** (E14, E20), in beide Richtungen geprüft — die naheliegende Zusammenfassung zu einer Spalte „gesperrt" bestünde jede Prüfung an einer reinen Funktion. **Vier Aussagen über die Verdrahtung**: dass die Vorwarnung aus E19 *abgefragt* wird, bevor ein Aufruf losläuft (beim eigenen Konto zuerst der Dialog und **kein** Aufruf, auch bei abweichender Schreibweise des Namens; beim fremden Konto sofort der Aufruf), und dass die Mengenersetzung die **vollständige** Zielmenge über die Leitung schickt statt einer Differenz. **Zwei Aussagen über die Sperre des Öffnen-Knopfes**, samt Gegenprobe: Solange eine Zeile offen ist, bekommt jede andere ihre Schaltfläche wirklich gesperrt — sonst gingen die dort begonnenen Eingaben verloren, und darunter ein bereits getipptes Einmalpasswort, das danach an keiner Stelle mehr steht. Die Regeln selbst sind reine Funktionen in `tests/benutzer.test.ts` — eine richtige Regel, die niemand abfragt, sieht von außen aus wie keine |
 * | `tests/dashboard-bloecke.test.tsx` *(01.09.2026, fortgeschrieben 16.09.2026)* | 22 | **Vier Klassen, und alle vier sind Aussagen über Abwesenheit oder über Reihenfolge.** „Nicht ermittelbar" (E‑q): **keine `0` und kein Verweis** im Baum — die naheliegende Schreibweise `imFenster ?? 0` bestünde jede Prüfung an einer reinen Funktion, und dass die *übrigen* Kacheln stehen bleiben, sagt keine von ihnen; mit Gegenprobe. Die **beiden Restzeilen** der Verteilung: „Übrige" **fehlt** ohne Rang 11, „nicht zugeordnet" **steht da** auch bei null, und beide bleiben **unten**, obwohl „Übrige" hier der größte Balken ist — ein Reihenfolgevergleich, kein Vorhandensein (M98, Befund 21). Der **Leerzustand** (E‑p): Satz und bedienbarer Umschalter, **sonst nichts**, samt der Gegenprobe, dass die Nullzeile der Verteilung dort *nicht* steht. Und die **Zahl der Anfragen**: genau eine an `/api/dashboard`, der gewählte Zeitraum in der Adresse, `?verteilung=RICHTUNG` **nicht** — und **seit dem 16.09.2026 (E‑161) ein Sichtwechsel ohne jede Anfrage und ohne Ladezustand**, hin und zurück, mit dem Klick auf „30 Tage" als Eichung. Der Ladezustand wird über einen `MutationObserver` gezählt: Mit der Sicht im Abfrageschlüssel erschien und verschwand er innerhalb eines `act`, und ein Nachsehen im Baum blieb in der Gegenprobe zweimal grün. *Bis dahin stand hier „ein Sichtwechsel, der **dieselbe Adresse mit anderem Parameter** ruft statt nachzuladen".* |
 * | `tests/plattform-block.test.tsx` *(10.09.2026, am selben Tag neu zugeschnitten; E‑160 am 16.09.2026)* | 13 | **Vier Klassen, und alle vier sind Aussagen darüber, ob etwas im Baum ankommt** — die Kachel *Plattform*. Erstens: Die Kachel trägt **kein Wort im Bild**; das Wort steht im `title` **und** in einer `sr-only`-Spanne, und beide Wege müssen denselben Satz führen — ein `title` allein ließe ein Vorleseprogramm mit einem `aria-hidden`-Zeichen zurück, eine `sr-only`-Spanne allein die Maus. Die *Sätze* selbst sind reine Funktionen und in `tests/dashboard.test.ts` geprüft; hier steht, dass jemand sie abfragt — samt der ungeklärten Zeile, an der Wort **und** Rohwert stehen (E‑130), und der Gegenprobe an der eingeordneten. Zweitens die **Farbe der Zielzeilen** (E‑133): `traegtZielfarbe` ist eine reine Funktion, aber ob die Zeile die Rolle wirklich trägt, ist eine **Klasse am Element** — `mitFarbe ? … : …` bestünde jede Prüfung an der Funktion; geprüft in beide Richtungen, dazu die **Sammelzeile** ohne Ziel, die ihr Zeichen fest von `UNGEKLAERT` nimmt. Drittens: Wo **keine** Dienstzeile steht, steht ein **Satz** (E‑135), und die Ablagen stehen daneben trotzdem, mit Gegenprobe. Viertens die **Abwesenheit dessen, was am 10.09.2026 aus dem Bild gefallen ist** — der ganze Text der Kachel wird Zeichen für Zeichen verglichen: kein Zeitpunkt, kein Alter, kein Prüfzeitpunkt, kein Satz zum Grund, kein Satz unter der Überschrift |
 * | `tests/prozess-baum.test.tsx` *(02.09.2026, erweitert 10.09.2026 und 15.09.2026)* | 16 | **Seit dem 15.09.2026 einer mehr:** Der Projektbaum läuft über denselben Pfad — zwei Ebenen, und ein Projekt mit einem einzigen Prozess bleibt eine Gruppe über seiner Zeile (`docs/process-view.md` E‑145). Dass die Projektebene **nicht** wie eine Richtung wegfällt, ist eine Aussage über Abwesenheit im Baum. **Vier Klassen, und alle vier stehen nur im Baum.** Der **roving `tabindex`**: „genau ein Tabstopp, nicht 1.158" ist eine Aussage über den Baum als ganzen, und dass er auf der *gewählten* Zeile liegt, entscheidet, wo ein tiefer Link den Nutzer beim ersten Tabben absetzt. Die **ARIA-Ausgabe der flachen Form**: `aria-level` je Ebene, `aria-expanded` nur an Gruppen und `aria-selected` an jeder Zeile (`aria-posinset`/`aria-setsize` entstehen in `baumzeilen` und sind dort geprüft) — die Zeilen selbst sind in `tests/prozessbaum.test.ts` geprüft, ihr Niederschlag im DOM ist es nicht. Und **zwei Aussagen über Abwesenheit**: bei einem Partner mit einer Richtung entsteht **keine** zweite Ebene (E‑45), und es gibt **keine `sr-only`-Spanne je Zahl** — die Beschriftung steht in einem `aria-label`, weil zweitausend absolut positionierte Knoten in einer langen Liste genau der Befund aus `docs/frontend-grundlagen.md` §7 wären. Dazu die Klassen, die selbst die Regel *sind*: `min-h-beruehrung` an jeder Zeile, `relative` und **kein** `overflow-y` am Baum. Und viertens die **Verdrahtung der Tastatur**, die keine reine Funktion trägt: dass ein Pfeil den Fokus und den roving `tabindex` wirklich weitersetzt — und dass eine **Modifiertaste durchgelassen** wird. `Alt+←` ist das Zurück des Browsers und damit der Weg, den `prozess` und `nachricht` mit `history: "push"` erst anlegen; `tastenbefehl` sieht Modifier gar nicht, der Satz ist nirgends sonst belegbar. **Seit E‑115 zwei mehr:** der Sprung zur gewählten Zeile **beim Einstieg** über eine Adresse — eine Änderung am Scrollstand eines Kastens, für die es ohne gerenderte Zeilen kein Ziel gibt — und die Gegenprobe, dass ein Wechsel der Auswahl **ohne** Einstieg nichts bewegt |
 * | `tests/konto-anlegen.test.tsx` *(07.09.2026)* | 6 | **Drei Klassen, und keine davon steht in einer reinen Funktion.** Die **Verdrahtung der Sperre in beide Richtungen** (E22): Die *Regel* ist `darfOeffnen`, um `MASKE` erweitert und in `tests/benutzer.test.ts` geprüft — belegt wird hier, dass jemand sie abfragt, und zwar je mit der Gegenprobe im selben Fall (vorher frei, danach gesperrt). Zweitens **was nach dem Erfolg passiert**: dass wirklich ein `GET /api/admin/users` **nach** dem `POST` hinausgeht und im Zwischenspeicher danach genau dessen Antwort steht (E24) — die naheliegende Abkürzung, die neun Felder der Zeile aus den vier der `POST`-Antwort zu ergänzen, bestünde jede Prüfung, die nur auf den Bildschirm sieht; dazu das **leere Passwortfeld** und die Meldung *in* der Maske (E25). Drittens der Ort einer Meldung: ein `409` steht **im Formular**, die Maske bleibt offen, und solange der Aufruf läuft, lässt sie sich nicht zuklappen — sonst wäre er nirgends zu sehen. **Fünf Mutanten gesetzt, fünf gefallen**, jeder in genau seinem Fall |
 *
 * | `tests/zeitraum-umschalter.test.tsx` *(07.09.2026)* | 2 | **Eine Aussage über Abwesenheit und eine über Verdrahtung.** Der vierte Knopf „Frei" ist **freiwillig** (`docs/process-view.md` §37 ff.): Ohne `aufFrei` sind es drei Knöpfe, mit ihr vier — die naheliegende Schreibweise (der Knopf immer da, nur ohne Wirkung) bestünde jede Prüfung an einer reinen Funktion, und das Dashboard ruft den Umschalter ohne. Dazu, dass ein Klick auf „Frei" `aufFrei` ruft und **nicht** `aufAuswahl` — sonst käme im Feature ein Code an, den die Liste der drei Paare nicht kennt; mit der Gegenprobe, dass ein Paar weiterhin die Auswahl ruft |
 * | `tests/gliederung-umschalter.test.tsx` *(15.09.2026, am selben Tag auf einen Schalter umgebaut)* | 2 | **Eine Aussage über den Zustand und eine über Verdrahtung** (`docs/process-view.md` E‑143, E‑146). Der Schalter steht **an**, wo Projekt gilt, und sonst aus — `aria-checked` ist ein Attribut am Element, die Regel dahinter eine reine Funktion. Dazu, dass Umschalten den **anderen** Code meldet, und zwar auch über die **Beschriftung**: Sie trägt die Berührungsfläche (`min-h-beruehrung`), und ob ein Klick auf sie den Schalter erreicht, entscheidet `htmlFor` und keine Funktion |
 * | `tests/suchfeld-auswahl.test.tsx` *(09.09.2026)* | 6 | **Die Auswahl neben dem Suchfeld in zwei Untermenüs (E‑112).** Drei Aussagen über den Baum, die keine reine Funktion trägt: Die **oberste Ebene** trägt genau drei Einträge — den typlosen und je einen Untermenü-Auslöser — und keine Überschrift mehr; ein Untermenü zeigt beim Öffnen **seine** Einträge und die des anderen **nicht**; und für einen Mandanten ohne Belegart (`WOC`) **fehlt** der Auslöser der Belegarten vollständig — Abwesenheit, nicht Leere. Dazu die **Verdrahtung**: Eine Wahl im zweiten Untermenü hebt die im ersten auf, am Schalter, am Auslöser und am Häkchen; und der Auslöser der Gruppe mit der Auswahl trägt den gewählten Eintrag **im zugänglichen Namen, ohne `aria-checked`** — er ist `menuitem` mit `aria-haspopup`, kein Radioeintrag. Und die Regression zum Schlüssel: gleichlautende Einträge in beiden Gruppen **ohne `console.error`**. Geöffnet wird über `pointerdown` und `ArrowRight`, Radix' eigene Wege ohne Zeitgeber (T1); `next/navigation` ist ersetzt, weil `useRouter` außerhalb des App-Routers wirft |
 *
 * | `tests/in-sicht-bringen.test.tsx` *(09.09.2026, erweitert 10.09.2026)* | 11 | **Ein Haken, dessen ganze Wirkung ein Aufruf am DOM ist** (E‑114, `lib/in-sicht-bringen.ts`). Ohne Komponente ist er nicht aufrufbar, und ob `scrollIntoView` gerufen wird — beim ersten Rendern mit Schlüssel, bei jedem Wechsel, nicht ohne Wechsel, nicht ohne Schlüssel — steht nirgends sonst. Dazu die **Höhenregel** aus der Chrome-Vorprobe (`docs/process-view.md` M172): Für ein Ziel, das höher ist als sein Scrollbereich, zählt allein die Oberkante — `start` statt `nearest`, wenn sie oberhalb liegt, und **keine Bewegung**, wenn sie schon steht; `jsdom` rechnet kein Layout, die Maße werden gestellt. Und der Rückweg `useZuletztGeschlossen`: die Kennung genau nach dem Schließen und sonst `null` — ein Zustand, der beim Rendern angepasst wird und nur im Baum beobachtbar ist. **Was die Datei nicht zeigt, steht in ihrem Kopf:** ob etwas ins Bild kommt, sagt allein der Browser. **Seit E‑115 vier mehr, und sie stellen eine schärfere Frage:** nicht *ob* gerufen wird, sondern **welcher Kasten sich bewegt und um wie viel** — `scrollIntoView` ist aus dem Haken verschwunden, weil es jeden scrollenden Vorfahren bewegt. Ein Ziel in einer eigenen Spalte bewegt nur diese und nicht `main`; eine klebende Spalte beginnt in ihrem eigenen Scrollbereich wieder oben; ein klebender Rahmen setzt den Scrollbereich **darin** zurück; und ohne etwas Klebendes gilt E‑114 unverändert |
 *
 * | `tests/rollen-auswahl.test.tsx` *(15.09.2026)* | 4 | **Die Rollenauswahl ist eine Liste der Anwendung und kein natives Feld mehr** (Punkt 180, `docs/benutzerverwaltung-frontend.md` §18). Eine Aussage über **Abwesenheit** — kein `<select>` im Baum, und genau das war der Anlass: Die Liste eines nativen Felds zeichnet der Browser, und keine Prüfung des Projekts erreicht ihre Farbe. Dazu drei über **Verdrahtung**, die erst im Zusammenspiel von Ereignis und Zustand entstehen: Eine gesperrte Rolle steht da und lässt sich weder per Klick noch per Taste wählen; derselbe Wert noch einmal gewählt meldet **nichts** — im Zeilenformular wäre jede Meldung ein `PUT`, und jedes `PUT` verwirft alle Sitzungen des Kontos (E5); `↓` am geschlossenen Feld **öffnet** nur. **Drei Mutanten gesetzt, drei gefallen**, jeder in genau einem Fall |
 * | `tests/spaltenwahl.test.tsx` *(15.09.2026, an zwei Meldungen des 16.09.2026 erweitert)* | 6 | **Die Regel ist selbst eine Klasse** (E‑147, `docs/spaltenwahl.md`): Ab welcher Containerbreite eine Spalte dasteht, entscheidet `@min-[…]/<name>:table-cell` an `th` **und** `td` — wörtlich, weil Tailwind nur findet, was im Quelltext steht. **Seit dem 16.09.2026 einer mehr:** die **Nachrichtenliste** (E‑148) — sie hing als einzige noch am Fenster, und an ihr hängt zusätzlich die Bauform unter der Grundmenge (der Ablauf bekommt den Rest, das Projekt kommt nicht). Je Tabelle (Nachrichtenliste, Trefferliste mit und ohne Spalte „Treffer", Benutzertabelle, Katalog): Die Zahl in der Klasse ist auf den Pixel die Summe der Mindestbreiten; an den gemessenen Containerbreiten und an jeder Schwellenkante liegt keine sichtbare Spalte unter ihrer Mindestbreite; `td` trägt dieselbe Sichtbarkeit wie `th`; keine Zelle trägt eine Fensterschwelle. Die Breiten sind nach `table-layout: fixed` **gerechnet** — `jsdom` rechnet kein Layout, der Beleg im Browser ist M177. **Seit der zweiten Meldung des 16.09.2026 einer mehr** (E‑149): Die Benutzertabelle hat **keine freie Spalte** mehr, und der Fall hält an der Breite der Meldung fest, dass **ein** Faktor für alle Spalten gilt — die Mandanten hatten dort 673 von 1.408 px gehortet, während Benutzername und Rolle auf ihrer Mindestbreite umbrachen |
 * | `tests/neu-laden.test.tsx` *(16.09.2026, am selben Tag erweitert)* | 15 | **„Neu laden" und die automatische Aktualisierung — fast durchweg Aussagen über Anfragen, die hinausgehen oder nicht** (`docs/neu-laden.md`). Der Baustein: ohne `automatik` **kein Schalter** (Abwesenheit), die drei Lagen über `aria-pressed` und drei verschiedene Symbole, und ein Klick beim Laden ruft nichts. Übersicht: ein Klick, **genau eine** weitere Anfrage an dieselbe Adresse, auch im **Leerzustand** (E‑p ergänzt). Nachrichten: von Seite zwei **eine** Anfrage ohne Cursor; bei offenem Panel **keine** an einen Detail- oder Dateiendpunkt (E‑169), mit dem Panel als Eichung. Mit gestellter Uhr: aus — nichts; an — nach 60 s eine; Seite zwei — pausiert und nichts, nach „Neu laden" wieder eine. Prozessansicht: zwei Minuten ohne Klick nichts (E‑164); ein Klick: Baum, dann Liste ab Seite eins; bringt der Baum ein neues Fenster, **keine** Listenanfrage mit dem alten. **Verletzungsproben:** „nur Seite eins" ausgehängt und „Panel mitholen" eingebaut — beide rot, zurückgenommen, in keinem Commit. **Seit E‑173 drei mehr, Aussagen über Reihenfolge im Baum:** Auf Übersicht und Prozessansicht ist der Knopf der **nächste Knopf nach dem letzten Zeitraum-Knopf**, in den Nachrichten sind Schalter und Knopf die **letzten beiden** der Filterleiste — die Leiste über den Baum gesucht, nicht über eine Klasse. Den Rand selbst rechnet jsdom nicht (M183). **Seit E‑175** prüft der Fall der Prozessansicht zusätzlich den freien Modus — keine Fallzahl mehr |
 *
 * > ⚠️ **Fortgeschrieben am 15.09.2026 (Spaltenwahl und Rollenauswahl), aus dem Lauf gezählt**
 * > (`vitest run --reporter=json`, Fälle je `.tsx`-Datei): **133 in neunzehn Dateien.** Acht
 * > sind neu, vier je neue Datei; der Lauf über alle 41 Dateien trägt 1.068 Fälle.
 *
 * > ⚠️ **Fortgeschrieben am 16.09.2026 (die Nachrichtenliste, E‑148), wieder aus dem Lauf gezählt**
 * > (`vitest run --reporter=json`, Fälle je `.tsx`-Datei): **134 in neunzehn Dateien**, keine neue
 * > Datei; der Lauf über alle 41 Dateien trägt **1.072** Fälle. Die vier Fälle mehr im Gesamtlauf
 * > gegenüber dem 15.09.2026 sind einer hier und drei reine in `tests/spaltenwahl.test.ts`-Nachbarn
 * > desselben Tages — gezählt, nicht gerechnet.
 *
 * > ⚠️ **Noch einmal fortgeschrieben am 16.09.2026 (die Aufteilung der Spalten, E‑149)**, wieder aus
 * > dem Lauf: **135 in neunzehn Dateien**, keine neue Datei; der Lauf über alle 41 Dateien trägt
 * > **1.073** Fälle. Der eine Fall mehr steht in `tests/spaltenwahl.test.tsx`.
 *
 * > ⚠️ **Ein drittes Mal fortgeschrieben am 16.09.2026 (beide Sichten der Verteilung, E‑161)**, aus
 * > dem Lauf (`vitest run --reporter=json`, Fälle je `.tsx`-Datei): **140 in neunzehn Dateien**,
 * > keine neue Datei; der Lauf über alle 41 Dateien trägt **1.081** Fälle. **Von den fünf Fällen
 * > mehr stammen nur zwei aus diesem Schritt:** `dashboard-bloecke` von 20 auf 22 (einer
 * > entfallen, drei neu). **Die übrigen drei sind Drift**: `plattform-block` lief mit **13** Fällen
 * > und stand hier mit 10 — die Datei ist mit E‑160 (Commit `a6245e5`, derselbe Tag) um drei Fälle
 * > gewachsen, ohne dass die Zahl mitwuchs. Auf `main` stand der Kopf damit bei 135 und der Lauf
 * > bei 138. Gezählt aus diesem Lauf; die 138 ergeben sich, weil `plattform-block` in diesem
 * > Schritt nicht angefasst ist.
 *
 * > ⚠️ **Ein viertes Mal fortgeschrieben am 16.09.2026 („Neu laden", E‑163 bis E‑172)**, aus dem
 * > Lauf (`vitest run --reporter=json`, Fälle je Datei): **152 in zwanzig Dateien**, eine neue —
 * > `tests/neu-laden.test.tsx` mit 12. Der Lauf über alle **43** Dateien trägt **1.107** Fälle.
 * > **Die Zahl davor stimmte um eins nicht:** Am Stand `01a6db2` — dem Commit, zu dem der Absatz
 * > darüber gehört — zählt derselbe Lauf **1.080** und nicht 1.081; die 140 gerenderten stimmen.
 * > Die 27 Fälle mehr: 12 hier, 11 in `tests/aktualisierung.test.ts` (rein), und **4, die niemand
 * > geschrieben hat** — `farbwerte.test.ts` (+3) und `serverbausteine.test.ts` (+1) prüfen je
 * > Quelldatei, und es sind neue Quelldateien dazugekommen. Gezählt je Datei gegen `01a6db2`,
 * > nicht gerechnet.
 *
 * > ⚠️ **Ein fünftes Mal fortgeschrieben am 16.09.2026 (die Stelle des Knopfes, E‑173 und E‑174)**,
 * > aus dem Lauf (`vitest run --reporter=json`, Fälle je Datei): **155 in zwanzig Dateien**, keine
 * > neue. Der Lauf über alle 43 Dateien trägt **1.110** Fälle. Je Datei gegen den Lauf des Commits
 * > `2d3c62d` verglichen: Geändert hat sich allein `tests/neu-laden.test.tsx`, von 12 auf 15 —
 * > keine neue Quelldatei, also nichts aus `farbwerte` und `serverbausteine`.
 *
 * > ⚠️ **Zwei Zahlen im selben Kopf gingen auseinander** — die Tabellensumme
 * > stand oben richtig, der Schlusssatz nannte „neunundvierzig". Berichtigt am
 * > 02.09.2026. Genau die Drift, gegen die die Regel „an einer Stelle geführt"
 * > gerichtet ist; sie greift nur, wenn die *eine* Stelle auch eine ist.
 *
 * > ⚠️ **Und sie war trotzdem wieder da** — berichtigt am **10.09.2026**, diesmal
 * > gegen den Lauf und nicht gegen die eigene Summe. Zwei Zeilen standen zu
 * > niedrig: `dashboard-bloecke` mit 10 statt **20** und `prozess-baum` mit 10
 * > statt **13**; beide Dateien sind nach ihrem Eintrag gewachsen, ohne dass die
 * > Zahl mitwuchs. Die Summe lautete deshalb 92 und die gezählte **105** — vor
 * > den sechs Fällen, die E‑115 hinzufügt (vier in `in-sicht-bringen`, zwei im
 * > Baum). Gezählt wird seither aus dem
 * > Testlauf (`vitest run --reporter=json`, Fälle je `.tsx`-Datei) und nicht
 * > durch Weiterzählen; die Zahl trägt eine Begründung und muss deshalb aus dem
 * > Lauf kommen (Regel L10).
 *
 * > ⚠️ **Berichtigt am 10.09.2026, zum dritten Mal aus demselben Anlass** — und
 * > diesmal ohne Drift: Die neue Zahl ist wieder aus dem Lauf gezählt
 * > (`vitest run --reporter=json`, Fälle je `.tsx`-Datei) und nicht
 * > weitergezählt. Aus 111 in fünfzehn Dateien sind **116 in sechzehn**
 * > geworden; die fünf neuen stehen alle in `plattform-block`.
 *
 * > ⚠️ **Fortgeschrieben am 15.09.2026, wieder aus dem Lauf gezählt**
 * > (`vitest run --reporter=json`, Fälle je `.tsx`-Datei): **125 in siebzehn
 * > Dateien.** Vier sind neu — je einer in `prozess-baum` und
 * > `benutzer-tabelle`, zwei in der neuen Datei `gliederung-umschalter`. Dabei
 * > aufgefallen: Der Kopf nannte schon einhunderteinundzwanzig, der Satz
 * > darunter noch einhundertsechzehn; die Tabellensumme stimmte mit dem Kopf.
 * > Berichtigt ist der Satz.
 *
 * Allen einhundertvierunddreißig ist dasselbe gemeinsam: **Es gibt keinen anderen Ort, an dem sie
 * belegbar wären.** Das ist die Bedingung, nicht „es ließe sich so leichter
 * prüfen". Sie schalten ihre Umgebung selbst über `// @vitest-environment jsdom`
 * um — die Voreinstellung bleibt `node`, damit die übrigen Dateien nichts von
 * einem DOM bezahlen.
 *
 * `setupFiles` trägt das Netz darunter: Ein `console.error` lässt den Testlauf
 * fehlschlagen (`tests/setup/konsole.ts`). Es gilt für **alle** Dateien, nicht
 * nur für die rendernden — eine Meldung aus einer reinen Funktion ist genauso
 * ein Befund.
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "node",
    include: ["tests/**/*.test.ts", "tests/**/*.test.tsx"],
    setupFiles: ["./tests/setup/konsole.ts"],
  },
});
