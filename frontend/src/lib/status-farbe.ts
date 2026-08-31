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
 * Die Tailwind-Klassen je Rolle. Vollständige Zeichenketten, weil Tailwind den
 * Quelltext nach Klassennamen durchsucht — ein zusammengesetzter Name entstünde
 * nie im erzeugten CSS.
 */
const KLASSEN: Record<Statusrolle | Problemrolle, string> = {
  abgeschlossen:
    "text-status-abgeschlossen bg-status-abgeschlossen-flaeche border-status-abgeschlossen-kontur",
  fehler: "text-status-fehler bg-status-fehler-flaeche border-status-fehler-kontur",
  offen: "text-status-offen bg-status-offen-flaeche border-status-offen-kontur",
  ungeklaert: "text-status-ungeklaert bg-status-ungeklaert-flaeche border-status-ungeklaert-kontur",
  // Ohne `status-`-Präfix, weil es keine Statusart dahinter gibt — siehe
  // `Problemkategorie` oben und den Kommentar in `app/globals.css`.
  ueberfaellig: "text-ueberfaellig bg-ueberfaellig-flaeche border-ueberfaellig-kontur",
};

export function statusrolle(art: Statusart): Statusrolle {
  return ZUORDNUNG[art];
}

export function statusKlassen(art: Statusart): string {
  return KLASSEN[statusrolle(art)];
}

export function problemrolle(kategorie: Problemkategorie): Problemrolle {
  return PROBLEM_ZUORDNUNG[kategorie];
}

export function problemKlassen(kategorie: Problemkategorie): string {
  return KLASSEN[problemrolle(kategorie)];
}
