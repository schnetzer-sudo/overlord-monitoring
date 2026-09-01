/**
 * **Die eine Stelle**, an der eine fachliche Aussage auf eine Farbrolle trifft.
 *
 * Zwei Zuordnungen wohnen hier, und beide bewusst in **derselben** Datei:
 * `ZUORDNUNG` (Statusart → Rolle) und `PROBLEM_ZUORDNUNG` (Problemkategorie →
 * Rolle). Zwei Dateien mit Farbzuordnung weichten genau die Regel auf, um
 * derentwillen es diese Datei gibt — „Zuordnung an genau einer Stelle"
 * (`docs/visuelles-konzept.md` §2).
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
  // `scripts/farbrolle-ueberfaellig/rechne.mjs`.
  UEBERFAELLIG: "ueberfaellig",
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

export function statusrolle(art: Statusart): Statusrolle {
  return ZUORDNUNG[art];
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
