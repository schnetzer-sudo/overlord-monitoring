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

/**
 * Das Ziel nach einem Mandantenwechsel — **garantiert ohne Filter in der URL.**
 *
 * Der Zwischenspeicher zu leeren genügt nicht: Ein Teil des Zustands liegt gar
 * nicht dort, sondern in der Adresszeile. Der **Prozessfilter** ist der Fall, an
 * dem das weh tut — `ProcessID`s sind mandantengebunden, und die des alten
 * Mandanten sind für den neuen bedeutungslos. Bliebe der Filter stehen, sähe der
 * Nutzer nach dem Wechsel eine dauerhaft leere Liste, deren Ursache in einem
 * Auswahlfeld steckt, das ihm nichts mehr anzeigen kann.
 *
 * Deshalb ist das Ziel des Wechsels der reine Pfad. Diese Funktion existiert, um
 * die Regel prüfbar zu machen: Wer den Wechsel später umbaut und auf der Liste
 * stehen bleiben will, muss hier vorbei.
 */
export function zielNachMandantenwechsel(startseite: string): string {
  return startseite.split("?")[0].split("#")[0];
}
