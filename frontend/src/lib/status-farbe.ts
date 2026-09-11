/**
 * **Die eine Stelle**, an der eine fachliche Aussage auf eine Farbrolle trifft.
 *
 * **Vier Zuordnungen wohnen hier**, und alle vier bewusst in **derselben**
 * Datei: `ZUORDNUNG` (Statusart → Rolle), `PROBLEM_ZUORDNUNG`
 * (Problemkategorie → Rolle) und seit dem 10.09.2026 `DIENST_ZUORDNUNG` sowie
 * `ABLAGEN_ZUORDNUNG` (E‑128, Schritt 10d Teil B). Eine zweite Datei mit einer
 * Farbzuordnung weichte genau die Regel auf, um derentwillen es diese Datei
 * gibt — „Zuordnung an genau einer Stelle" (`docs/visuelles-konzept.md` §2).
 *
 * **Vier Schlüsselmengen und weiterhin vier Rollen.** Keine der beiden neuen
 * Tabellen bringt eine Farbe mit; sie wenden die bestehenden ein zweites Mal an
 * (E‑121). Was sie *nicht* teilen, ist das Wort: Die Beschriftungen der Rollen
 * gehören den Einordnungen von Nachrichten (E‑82), ein Dienst ist keines ihrer
 * Mitglieder, und seine Wörter stehen unter `texte.dashboard.plattform`
 * (E‑129).
 *
 * Nirgends sonst steht, dass „abgeschlossen" grün ist. Wer die Zuordnung ändern
 * will, ändert sie hier; wer den Farbwert ändern will, ändert ihn in
 * `app/globals.css`. Beides zusammen ist der Grund, weshalb das visuelle Konzept
 * austauschbar bleibt.
 *
 * Die Statusarten spiegeln `common/MessageStatusKind` im Backend — die
 * Einordnung eines Rohwerts (`MessageStatus`) passiert ausschließlich dort
 * (`MessageStatusClassifier`, siehe `docs/message-status.md`) und wird hier
 * **nicht** nachgebaut.
 *
 * ## Status wird nie allein über Farbe ausgedrückt
 *
 * Jede Statusanzeige trägt zusätzlich eine Beschriftung oder ein Zeichen. Die
 * Klassen hier sind die **halbe** Aussage, nie die ganze.
 *
 * Zwei Gründe, beide zwingend: Rot-Grün-Schwäche betrifft rund acht Prozent der
 * Männer, und dieses Werkzeug ist ein Fehlermelder — ein Status, den man nicht
 * unterscheiden kann, ist kein Status. Und der Akzent der Anwendung ist ein
 * Gelbgrün; er liegt zwischen den beiden fachlich belegten Farbzonen und darf
 * nie als Statusaussage gelesen werden (`docs/visuelles-konzept.md`).
 *
 * Erster Verbraucher ist die Nachrichtenliste in Schritt 4. Der Satz gilt für
 * die Problemkategorie unverändert: Auch *überfällig* wird nie allein über
 * Farbe ausgedrückt — das Nachrichtendetail zeigt sie bis heute ganz ohne
 * (`docs/nachrichtendetail.md` §10.4).
 */

/** Fachliche Einordnung eines Status, wie sie das Backend liefert. */
export type Statusart =
  | "FEHLER"
  | "WARTEND"
  | "LAEUFT"
  | "AUFGETEILT"
  | "ZUSAMMENGEFUEHRT"
  | "ABGESCHLOSSEN"
  | "QUITTIERT"
  | "UNGEKLAERT";

/**
 * Dieselben acht als **Liste zur Laufzeit**, in der Reihenfolge von
 * `common/MessageStatusKind`.
 *
 * **Sie stand bis zum 01.09.2026 in `features/nachrichten/filter.ts`** und ist
 * mit dem Dashboard hierher gewandert: Ein zweites Fachpaket braucht sie, und
 * ein Feature importiert nicht aus dem Nachbarfeature — der gemeinsame Teil
 * wandert nach `lib` (`docs/frontend-grundlagen.md` §8). Die Liste steht
 * bewusst **hier** und nicht in einer dritten Datei: Sie ist derselbe
 * Schlüsselsatz wie {@link ZUORDNUNG} darunter, und zwei Orte liefen
 * auseinander.
 *
 * Der Typ bleibt die von Hand geschriebene Vereinigung darüber. Ihn aus dieser
 * Liste abzuleiten wäre möglich und würde die Reihenfolge zur Typinformation
 * machen — sie ist eine Anzeigeentscheidung und keine.
 */
export const STATUSARTEN: readonly Statusart[] = [
  "FEHLER",
  "WARTEND",
  "LAEUFT",
  "AUFGETEILT",
  "ZUSAMMENGEFUEHRT",
  "ABGESCHLOSSEN",
  "QUITTIERT",
  "UNGEKLAERT",
];

/**
 * Die Farbrollen. Ihre Namen tragen die **Fachlichkeit**, nicht die Farbe —
 * sonst hieße die Rolle „grün" und ließe sich nie umfärben.
 */
export type Statusrolle = "abgeschlossen" | "fehler" | "offen" | "ungeklaert";

/**
 * Eine der drei Problemkategorien aus `PROJEKTBESCHREIBUNG.md` §4.2 — und zwar
 * die **einzige**, die keine Statusart ist und deshalb eine eigene Zuordnung
 * braucht.
 *
 * *Fehler* ist ein Statuswert und läuft über `Statusart`; eine zweite
 * Zuordnung dorthin wäre ein zweiter Weg zu Rot. *Unquittiert* ist mit
 * Entscheidung E‑d vom 24.08.2026 aus dem MVP genommen und hat bis heute keine
 * operative Definition — eine Farbe dafür wäre eine Farbe für nichts.
 *
 * Dass hier heute genau ein Wert steht, ist deshalb kein Zwischenstand,
 * sondern die Lage. Ein zweiter kommt hinzu, wenn eine Kategorie hinzukommt.
 */
export type Problemkategorie = "UEBERFAELLIG";

/**
 * Die Farbrolle einer Problemkategorie. Getrennt von `Statusrolle`, weil die
 * beiden Mengen fachlich getrennt sind: Eine `WARTEND`-Zeile kann überfällig
 * sein oder nicht, und dieselbe Einordnung trägt beide Fälle. Die Kategorie
 * liegt **quer** zur Einordnung und ist keine weitere Ausprägung von ihr.
 */
export type Problemrolle = "ueberfaellig";

/**
 * Wie ein **Dienst des Altsystems** dasteht — die Einordnung aus
 * `dashboard/Dienstzustand` im Backend, gebildet allein vom
 * `DienstStatusClassifier` (`docs/dienste.md` §6).
 *
 * **Sie steht hier und nicht in `features/dashboard/api.ts`**, aus demselben
 * Grund wie {@link Statusart}: Der Schlüssel einer Farbzuordnung wohnt bei der
 * Zuordnung. Das Feature liest den Typ von hier.
 */
export type Dienstzustand =
  "MELDET_SICH" | "ZEITUEBERSCHRITTEN" | "HERUNTERGEFAHREN" | "UNGEKLAERT";

/**
 * Dieselben vier als **Liste zur Laufzeit**, in der Reihenfolge der Aufzählung
 * im Backend — für die Vollständigkeitsprüfung von {@link DIENST_ZUORDNUNG} und
 * der Wörter in beiden Sprachdateien.
 *
 * Wie bei {@link STATUSARTEN}: Der Typ bleibt die Vereinigung darüber, damit die
 * Reihenfolge eine Anzeigeentscheidung bleibt und keine Typinformation wird.
 */
export const DIENSTZUSTAENDE: readonly Dienstzustand[] = [
  "MELDET_SICH",
  "ZEITUEBERSCHRITTEN",
  "HERUNTERGEFAHREN",
  "UNGEKLAERT",
];

/**
 * Wie eine **Ablage** dasteht — aus `dashboard/Ablagenzustand`. Derselbe Typ
 * trägt den Zustand der ganzen Kachel **und** den einer einzelnen Zielzeile;
 * im Backend ist es dieselbe Aufzählung (`AblagenResponse.zustand`,
 * `AblagenzielResponse.zustand`).
 */
export type Ablagenzustand = "ERREICHBAR" | "NICHT_ERREICHBAR" | "UNGEKLAERT";

/** Dieselben drei als Liste zur Laufzeit. Siehe {@link DIENSTZUSTAENDE}. */
export const ABLAGENZUSTAENDE: readonly Ablagenzustand[] = [
  "ERREICHBAR",
  "NICHT_ERREICHBAR",
  "UNGEKLAERT",
];

const ZUORDNUNG: Record<Statusart, Statusrolle> = {
  // Rot — und nur hier.
  FEHLER: "fehler",
  // Grün — und nur hier. Quittiert ist der Endzustand einer ausgehenden
  // Nachricht, abgeschlossen der einer eingehenden.
  ABGESCHLOSSEN: "abgeschlossen",
  QUITTIERT: "abgeschlossen",
  // Neutral: Wartendes und die beiden Zwischenprodukte. `SUSPENDED` ist kein
  // Fehler (Q3), und `SPLITTED`/`MERGED` sind Zwischenprodukte, keine Ergebnisse.
  //
  // **Beide teilen sich eine Farbrolle, obwohl sie seit dem 11.08.2026 zwei
  // Statusarten sind.** Der Unterschied zwischen „aufgeteilt" und
  // „zusammengeführt" ist keine Aussage über *gut oder schlecht* — und nur die
  // trägt eine Farbe. Getragen wird er von Beschriftung und Zeichen.
  WARTEND: "offen",
  LAEUFT: "offen",
  AUFGETEILT: "offen",
  ZUSAMMENGEFUEHRT: "offen",
  // „Nicht zugeordnet heißt nicht zugeordnet" (Q4). Ein unbekannter Statuswert
  // bekommt keine geratene Farbe, sondern eine eigene, sichtbar zurückhaltende.
  UNGEKLAERT: "ungeklaert",
};

/**
 * Die **zweite** Zuordnung: Problemkategorie → Farbrolle.
 *
 * Sie steht neben `ZUORDNUNG` und nicht darin, weil ihr Schlüssel etwas
 * anderes ist. Rot bleibt damit an genau einer Stelle vergeben — an `FEHLER`
 * oben —, und die neue Rolle bekommt keinen zweiten Weg dorthin.
 */
const PROBLEM_ZUORDNUNG: Record<Problemkategorie, Problemrolle> = {
  // Orange, Ton 80 — nicht Rot. Würde „überfällig" rot, verschmölzen zwei der
  // drei Problemkategorien in der Wahrnehmung, obwohl Regel Q3 sie im Code
  // sorgfältig trennt und ausdrücklich gleichrangig führt. Die drei Werte
  // stehen in `app/globals.css`, gerechnet in
  // `scripts/farbwerte/rechne.mjs` (bis zum 03.09.2026
  // `scripts/farbrolle-ueberfaellig/`).
  UEBERFAELLIG: "ueberfaellig",
};

/**
 * Die **dritte** Zuordnung: Dienstzustand → Farbrolle (Entscheidung **E‑121**
 * aus Teil A, umgesetzt am 10.09.2026 als **E‑128**: zwei neue Tabellen, und
 * beide in *dieser* Datei — `docs/visuelles-konzept.md` §2 duldet keine zweite
 * Stelle mit einer Farbzuordnung).
 *
 * **Es sind die bestehenden vier Rollen, zum zweiten Mal angewendet** — keine
 * neue Farbe, kein neuer Token, keine Zeile in `app/globals.css`. Genau das ist
 * der Inhalt von E‑121: Ein Dienst, der sich nicht mehr meldet, ist dasselbe
 * *Rot* wie eine Nachricht im Fehler; es gibt kein zweites Vokabular
 * (`docs/visuelles-konzept.md` §3).
 *
 * > ⚠️ **Die Rollen teilen sich die Farbe, nicht das Wort** (Entscheidung
 * > **E‑129**). `--status-abgeschlossen` heißt in der Nachrichtenliste
 * > „Erledigt" und `--status-offen` „Ohne Ergebnis" (E‑82) — beides sind
 * > Beschriftungen für **Einordnungen von Nachrichten**. Ein Dienst ist keines
 * > ihrer Mitglieder im Sinne von E‑82; „Erledigt" an einer Dienstlampe wäre
 * > kein zusammenfassendes Wort, sondern ein falsches. Die Wörter stehen
 * > deshalb unter `texte.dashboard.plattform` und nirgends sonst.
 *
 * **Warum `HERUNTERGEFAHREN` neutral ist und nicht rot:** Ein geordnet
 * heruntergefahrener Dienst ist kein Befund — er ist abgeschaltet worden, und
 * das war eine Entscheidung. Rot ist selten und bleibt dem vorbehalten, was
 * niemand wollte (`docs/dienste.md` §6). `--status-offen` sagt genau das, was
 * sein Kommentar oben sagt: *kein Ergebnis, kein Problem.*
 */
const DIENST_ZUORDNUNG: Record<Dienstzustand, Statusrolle> = {
  MELDET_SICH: "abgeschlossen",
  // Rot, und es ist dasselbe Rot: Der Wächter des Altsystems hat zugeschlagen.
  ZEITUEBERSCHRITTEN: "fehler",
  HERUNTERGEFAHREN: "offen",
  // Der Rohwert steht in der Antwort daneben und wird an genau dieser Zeile
  // auch genannt (E‑130) — seit dem 10.09.2026 im `title` und für Vorleser,
  // weil die Kachel klein ist. Ohne ihn wäre „ungeklärt" ein Achselzucken.
  UNGEKLAERT: "ungeklaert",
};

/**
 * Die **vierte** Zuordnung: Ablagenzustand → Farbrolle (ebenfalls E‑121).
 *
 * Sie steht neben {@link DIENST_ZUORDNUNG} und nicht darin, obwohl beide heute
 * in dieselben Rollen zeigen: Es sind **zwei Aufzählungen**, und dass
 * `UNGEKLAERT` in beiden vorkommt, macht sie nicht zu einer. Genau daran ist
 * E‑82 entstanden — ein Name aus der einen Menge, der in die
 * Beschriftungsposition der anderen rutscht.
 *
 * **Ein Ziel ohne eigene Rolle gibt es hier nicht.** Ob eine Zielzeile ihre
 * Farbe überhaupt tragen darf, entscheidet nicht der Zustand des Ziels, sondern
 * ob der Stand der Kachel noch gilt — das ist eine Frage der Ansicht und steht
 * als reine Funktion in `features/dashboard/plattform.ts` (E‑133).
 *
 * **Genommen wird nur die Farbe, nie das Wort.** Auch eine gedämpfte Zielzeile
 * trägt ihr Zeichen und ihre Auskunft im `title` — sonst wäre der Zustand nur
 * noch über Helligkeit ausgedrückt, und genau das schließt
 * `docs/visuelles-konzept.md` §3 aus.
 */
const ABLAGEN_ZUORDNUNG: Record<Ablagenzustand, Statusrolle> = {
  ERREICHBAR: "abgeschlossen",
  NICHT_ERREICHBAR: "fehler",
  UNGEKLAERT: "ungeklaert",
};

/**
 * Vordergrund und Fläche je Rolle. Vollständige Zeichenketten, weil Tailwind den
 * Quelltext nach Klassennamen durchsucht — ein zusammengesetzter Name entstünde
 * nie im erzeugten CSS.
 */
const FLAECHE_UND_SCHRIFT: Record<Statusrolle | Problemrolle, string> = {
  abgeschlossen: "text-status-abgeschlossen bg-status-abgeschlossen-flaeche",
  fehler: "text-status-fehler bg-status-fehler-flaeche",
  offen: "text-status-offen bg-status-offen-flaeche",
  ungeklaert: "text-status-ungeklaert bg-status-ungeklaert-flaeche",
  // Ohne `status-`-Präfix, weil es keine Statusart dahinter gibt — siehe
  // `Problemkategorie` oben und den Kommentar in `app/globals.css`.
  ueberfaellig: "text-ueberfaellig bg-ueberfaellig-flaeche",
};

/**
 * Dieselben Vordergrundfarben als **Wert statt als Klasse** — für ein Diagramm.
 *
 * Recharts färbt nicht über `className`, sondern über ein Prop: `<Bar fill="…" />`.
 * Die zulässige Form dafür ist `var(--token)`; dass sie ankommt **und** auflöst,
 * ist in [`docs/frontend-grundlagen.md`](../../../docs/frontend-grundlagen.md)
 * §8a gemessen — an vier Stellen, einschließlich der Pixel im Bild.
 *
 * **Sie steht hier und nicht im Diagramm.** Sonst kennte eine Komponente den
 * Namen eines Farbtokens, und `docs/visuelles-konzept.md` §2 hinge an der
 * Sorgfalt dessen, der das nächste Diagramm baut. Zusammengesetzt wird der Name
 * ebenfalls nicht: Ein `var(--status-${rolle})` stünde nirgends vollständig im
 * Quelltext und wäre bei einer Umbenennung nicht auffindbar.
 */
const FUELLUNG: Record<Statusrolle | Problemrolle, string> = {
  abgeschlossen: "var(--status-abgeschlossen)",
  fehler: "var(--status-fehler)",
  offen: "var(--status-offen)",
  ungeklaert: "var(--status-ungeklaert)",
  ueberfaellig: "var(--ueberfaellig)",
};

/** Die Füllfarbe einer Rolle für ein Diagramm-Prop. Siehe {@link FUELLUNG}. */
export function rollenfuellung(rolle: Statusrolle | Problemrolle): string {
  return FUELLUNG[rolle];
}

/**
 * Der **dritte** Wert je Rolle, getrennt gehalten.
 *
 * ## Warum getrennt — die Sichtprobe vom 01.09.2026
 *
 * Bis dahin standen alle drei Werte in einer Zeichenkette, und es gab keinen
 * Anlass, sie zu teilen: Die vier Statuskonturen liegen bei L 0.85 bis 0.90 und
 * sind auf `--card` mit 1,35 : 1 bis 1,57 : 1 kaum zu sehen. **`--ueberfaellig-kontur`
 * liegt bei L 0.65 und erreicht 3,29 : 1** — sie ist die einzige, die WCAG 1.4.11
 * erfüllt, und genau deshalb fällt sie aus der Familie (`docs/visuelles-konzept.md`
 * §7a, Befund 4).
 *
 * Im Dashboard stehen die beiden Problemkategorien **nebeneinander**, und dort
 * ist das keine Fußnote mehr: Die Überfällig-Kachel bekommt einen sichtbar
 * gezeichneten Rand, die Fehler-Kachel praktisch keinen — und zwei Kacheln, von
 * denen eine umrandet ist, lesen sich als **Rangfolge**. Regel Q3 führt beide
 * gleichrangig. §7a hat diese Überlegung für die *Helligkeit* des Vordergrunds
 * gezogen und für die Kontur nicht; nachgeholt ist sie in
 * `docs/dashboard-frontend.md` §3.
 *
 * **Nachgedunkelt oder aufgehellt wird nichts.** §7a schließt beides
 * ausdrücklich aus — die Ungleichheit ist der Befund, und wer sie auflöst, tut
 * es für alle fünf Rollen zugleich. Getrennt wird deshalb nicht der *Wert*,
 * sondern seine **Verwendung**: Eine Ansicht, in der die Kategorien
 * nebeneinander stehen, nimmt zwei der drei Werte.
 */
const KONTUR: Record<Statusrolle | Problemrolle, string> = {
  abgeschlossen: "border-status-abgeschlossen-kontur",
  fehler: "border-status-fehler-kontur",
  offen: "border-status-offen-kontur",
  ungeklaert: "border-status-ungeklaert-kontur",
  ueberfaellig: "border-ueberfaellig-kontur",
};

/**
 * Alle drei Werte — die Fassung für eine Ansicht, in der eine Rolle **allein**
 * auftritt. So steht sie in der Nachrichtenliste, im Detail und im Kettenblock.
 */
function alleDrei(rolle: Statusrolle | Problemrolle): string {
  return `${FLAECHE_UND_SCHRIFT[rolle]} ${KONTUR[rolle]}`;
}

/**
 * Zwei der drei Werte — die Fassung für eine Ansicht, in der **zwei Rollen
 * nebeneinander** stehen und keine lauter sein darf als die andere.
 *
 * `border-transparent` gehört dazu und ist kein Beiwerk: `Badge` mit
 * `variant="outline"` setzt sonst `border-border` und zöge einen grauen Ring an
 * genau die Stelle, die hier leer bleiben soll.
 */
function ohneKontur(rolle: Statusrolle | Problemrolle): string {
  return `${FLAECHE_UND_SCHRIFT[rolle]} border-transparent`;
}

/**
 * Nur der **Vordergrund** — die Fassung für eine Zahl, die in einer Zeile
 * mitläuft und keine Plakette ist.
 *
 * Gebraucht seit dem 02.09.2026 vom Prozessbaum: Dort steht neben jedem Knoten
 * die Zahl der Fehler, und eine Plakette je Zeile wäre bei 1.158 Zeilen ein
 * Flächenteppich. **Die Farbe bleibt trotzdem hier** — eine Komponente kennt
 * keinen Farbnamen (`tests/farbwerte.test.ts`).
 *
 * **Farbe allein genügt nicht**, und das ist keine Frage dieser Datei: Wer diese
 * Klassen nimmt, stellt ein Zeichen oder eine Beschriftung daneben
 * (`docs/visuelles-konzept.md` §3).
 */
const VORDERGRUND: Record<Statusrolle | Problemrolle, string> = {
  abgeschlossen: "text-status-abgeschlossen",
  fehler: "text-status-fehler",
  offen: "text-status-offen",
  ungeklaert: "text-status-ungeklaert",
  ueberfaellig: "text-ueberfaellig",
};

export function statusrolle(art: Statusart): Statusrolle {
  return ZUORDNUNG[art];
}

/** Siehe {@link VORDERGRUND} — für eine Zahl in einer Zeile, ohne Fläche und ohne Kontur. */
export function statusVordergrund(art: Statusart): string {
  return VORDERGRUND[statusrolle(art)];
}

export function statusKlassen(art: Statusart): string {
  return alleDrei(statusrolle(art));
}

/** Siehe {@link ohneKontur} — für zwei Rollen nebeneinander. */
export function statusKlassenOhneKontur(art: Statusart): string {
  return ohneKontur(statusrolle(art));
}

export function problemrolle(kategorie: Problemkategorie): Problemrolle {
  return PROBLEM_ZUORDNUNG[kategorie];
}

export function problemKlassen(kategorie: Problemkategorie): string {
  return alleDrei(problemrolle(kategorie));
}

/** Siehe {@link ohneKontur} — für zwei Rollen nebeneinander. */
export function problemKlassenOhneKontur(kategorie: Problemkategorie): string {
  return ohneKontur(problemrolle(kategorie));
}

export function dienstrolle(zustand: Dienstzustand): Statusrolle {
  return DIENST_ZUORDNUNG[zustand];
}

/**
 * Nur der **Vordergrund** einer Dienstzeile (siehe {@link VORDERGRUND}).
 *
 * > ### ⚠️ Am 10.09.2026 an die Stelle von `dienstKlassenOhneKontur` getreten
 * >
 * > Die Kachel *Plattform* trug bis dahin je Dienst eine **Plakette** mit
 * > Fläche und Wort; seit sie die fünfte Kachel der Reihe ist, trägt sie je
 * > Dienst nur noch **Zeichen und Kennung** — die Bauform von E‑91, dieselbe
 * > wie in „Zuletzt aufgefallen". Die Fassung mit Fläche wird damit nirgends
 * > mehr gebraucht, und **E‑79 ist wieder eindeutig:** Gefüllt ist allein die
 * > Fehlerkachel.
 *
 * **Farbe allein genügt nicht**, und das ist keine Frage dieser Datei: Wer
 * diese Klassen nimmt, stellt ein Zeichen oder eine Beschriftung daneben
 * (`docs/visuelles-konzept.md` §3). In der Plattform-Kachel tut das ein
 * Zeichen, dessen **Form** sich je Zustand unterscheidet, und das Wort steht im
 * `title` (`features/dashboard/plattform.ts`).
 */
export function dienstVordergrund(zustand: Dienstzustand): string {
  return VORDERGRUND[dienstrolle(zustand)];
}

export function ablagenrolle(zustand: Ablagenzustand): Statusrolle {
  return ABLAGEN_ZUORDNUNG[zustand];
}

/** Siehe {@link dienstVordergrund} — dieselbe Lage, andere Aufzählung. */
export function ablagenVordergrund(zustand: Ablagenzustand): string {
  return VORDERGRUND[ablagenrolle(zustand)];
}
