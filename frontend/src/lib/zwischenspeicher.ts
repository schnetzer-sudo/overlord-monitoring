/** Alles, was sich leeren lässt — im Betrieb der `QueryClient`, im Test ein Doppel. */
export type Leerbar = { clear: () => void };

/**
 * Nach einem Mandantenwechsel wird der Zwischenspeicher **geleert, nicht
 * invalidiert**.
 *
 * Der Unterschied ist der ganze Punkt: `invalidateQueries` markiert die Daten
 * nur als veraltet und zeigt sie weiter an, bis die neue Antwort da ist. Bei
 * einem Werkzeug, dessen Kernversprechen die Mandantentrennung ist, hieße das,
 * nach dem Umschalten für einen Moment die Daten des vorherigen Mandanten zu
 * zeigen — obwohl das Backend sauber ist. Das wäre der peinlichste denkbare
 * Fehler.
 *
 * Geleert wird **vor** dem Weitergehen, nicht danach.
 */
export function nachMandantenwechsel(speicher: Leerbar, weiter: () => void): void {
  speicher.clear();
  weiter();
}

/**
 * Beim Abmelden gilt dasselbe: Sonst sieht der nächste Nutzer am selben Gerät
 * kurz die Daten des vorherigen.
 */
export function nachAbmeldung(speicher: Leerbar, weiter: () => void): void {
  speicher.clear();
  weiter();
}
