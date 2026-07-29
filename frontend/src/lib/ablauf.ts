import { ROUTEN } from "./routen";

/**
 * Der Ablauf nach dem Anmelden — **eine Funktion, ohne React, ohne Netzwerk**.
 *
 * Sie ist bewusst rein: Die Reihenfolge Änderungszwang → Mandantenauswahl →
 * Startseite ist die Regel, an der die ganze Anmeldung hängt, und sie soll sich
 * ohne gerenderten Baum prüfen lassen. Der Anwendungsrahmen tut nichts weiter,
 * als das Ergebnis dieser Funktion an den Router weiterzureichen.
 *
 * **Das ist keine Absicherung.** Verbindlich entscheidet immer das Backend: Der
 * `PasswortwechselInterceptor` lehnt bei gesetztem Zwang jeden Endpunkt außer
 * Selbstauskunft, Passwortänderung und Abmeldung ab, und ohne aktiven Mandanten
 * antwortet jeder fachliche Endpunkt mit `kein-mandant-gewaehlt`. Hier geht es
 * ausschließlich darum, dem Nutzer den Weg zu zeigen, statt ihn gegen eine
 * Fehlermeldung laufen zu lassen.
 */
export type Sitzungszustand =
  | { art: "laedt" }
  | { art: "abgemeldet" }
  | { art: "angemeldet"; passwortwechselNoetig: boolean; mandantAktiv: boolean };

/**
 * @returns die Route, auf die umgeleitet werden muss — oder `null`, wenn der
 *   aktuelle Pfad zum Zustand passt.
 */
export function zielRoute(zustand: Sitzungszustand, aktuellerPfad: string): string | null {
  if (zustand.art === "laedt") {
    return null;
  }

  if (zustand.art === "abgemeldet") {
    return aktuellerPfad === ROUTEN.anmeldung ? null : ROUTEN.anmeldung;
  }

  // 1. Änderungszwang schlägt alles. Solange er steht, ist jede andere Route
  //    eine Sackgasse — das Backend antwortet dort ohnehin mit 403.
  if (zustand.passwortwechselNoetig) {
    return aktuellerPfad === ROUTEN.passwort ? null : ROUTEN.passwort;
  }

  // 2. Danach der Mandant. Ohne ihn gibt es keinen zulässigen Datenausschnitt,
  //    und einen zu raten wäre die schlechteste Variante.
  if (!zustand.mandantAktiv) {
    return aktuellerPfad === ROUTEN.mandantenauswahl ? null : ROUTEN.mandantenauswahl;
  }

  // 3. Alles geklärt. Wer trotzdem noch auf der Anmeldung steht, gehört auf die
  //    Startseite; Passwortänderung und Mandantenwechsel bleiben freiwillig
  //    erreichbar.
  return aktuellerPfad === ROUTEN.anmeldung ? ROUTEN.startseite : null;
}
