/**
 * **Die eine Stelle**, an der ein fachlicher Status auf eine Farbrolle trifft.
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
 * Erster Verbraucher ist die Nachrichtenliste in Schritt 4.
 */

/** Fachliche Einordnung eines Status, wie sie das Backend liefert. */
export type Statusart =
  | "FEHLER"
  | "WARTEND"
  | "LAEUFT"
  | "ZWISCHENSCHRITT"
  | "ABGESCHLOSSEN"
  | "QUITTIERT"
  | "UNGEKLAERT";

/**
 * Die Farbrollen. Ihre Namen tragen die **Fachlichkeit**, nicht die Farbe —
 * sonst hieße die Rolle „grün" und ließe sich nie umfärben.
 */
export type Statusrolle = "abgeschlossen" | "fehler" | "offen" | "ungeklaert";

const ZUORDNUNG: Record<Statusart, Statusrolle> = {
  // Rot — und nur hier.
  FEHLER: "fehler",
  // Grün — und nur hier. Quittiert ist der Endzustand einer ausgehenden
  // Nachricht, abgeschlossen der einer eingehenden.
  ABGESCHLOSSEN: "abgeschlossen",
  QUITTIERT: "abgeschlossen",
  // Neutral: Zwischenschritte und Wartendes. `SUSPENDED` ist kein Fehler (Q3),
  // und `SPLITTED`/`MERGED` sind Zwischenprodukte, keine Ergebnisse.
  WARTEND: "offen",
  LAEUFT: "offen",
  ZWISCHENSCHRITT: "offen",
  // „Nicht zugeordnet heißt nicht zugeordnet" (Q4). Ein unbekannter Statuswert
  // bekommt keine geratene Farbe, sondern eine eigene, sichtbar zurückhaltende.
  UNGEKLAERT: "ungeklaert",
};

/**
 * Die Tailwind-Klassen je Rolle. Vollständige Zeichenketten, weil Tailwind den
 * Quelltext nach Klassennamen durchsucht — ein zusammengesetzter Name entstünde
 * nie im erzeugten CSS.
 */
const KLASSEN: Record<Statusrolle, string> = {
  abgeschlossen:
    "text-status-abgeschlossen bg-status-abgeschlossen-flaeche border-status-abgeschlossen-kontur",
  fehler: "text-status-fehler bg-status-fehler-flaeche border-status-fehler-kontur",
  offen: "text-status-offen bg-status-offen-flaeche border-status-offen-kontur",
  ungeklaert: "text-status-ungeklaert bg-status-ungeklaert-flaeche border-status-ungeklaert-kontur",
};

export function statusrolle(art: Statusart): Statusrolle {
  return ZUORDNUNG[art];
}

export function statusKlassen(art: Statusart): string {
  return KLASSEN[statusrolle(art)];
}
