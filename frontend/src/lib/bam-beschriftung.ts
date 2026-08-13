/**
 * Eine BAM-Typbeschreibung in **Name** und **Endung** zerlegt — damit die Endung
 * beim Umbruch nicht aufbricht.
 *
 * ## Das Problem, das sie löst
 *
 * Die Beschriftungsspalte des Belegdaten-Blocks ist gedeckelt
 * (`docs/bam-werte.md` §11a), eine lange Beschriftung bricht also in ihrer
 * eigenen Zelle um. Der Bruch fällt dabei dorthin, wo die Breite ausgeht, und
 * nicht dorthin, wo eine Wortgrenze wäre — `Kundenmaterialnummer_K_SAP` stand
 * als `Kundenmaterialnummer_` / `K_SAP`, mit dem Unterstrich am Zeilenende und
 * der Endung zerschnitten. Gemeint ist `Kundenmaterialnummer` / `_K_SAP`.
 *
 * Das ist **kein** Breitenproblem: Es ist `overflow-wrap: break-word` an einer
 * Zeichenkette ohne Wortgrenze. Gekürzt wird deshalb nichts (M45), umbrechen
 * darf der Name weiterhin — nur die Endung bleibt zusammen.
 *
 * ## Was die Endung ist
 *
 * Der **abschließende Lauf aus `_GROSSBUCHSTABEN`-Segmenten**. Im Bestand gibt
 * es genau drei — `_L_SAP` (20 Typen), `_K_SAP` (13), `_FORS` (7); **22 von 62
 * tragen keine** (M45‑1).
 *
 * Zwei Fassungen, die naheliegen und beide gemessen falsch sind:
 *
 * - **Ohne `i`-Flag, und das ist kein Stilpunkt.** M45‑2 hat die Falle in SQL
 *   gemessen: Unter der Spaltenkollation `general_ci` traf `[A-Z]` auch
 *   Kleinbuchstaben, und `Sender_Ident_FORS` bekam die Endung `_Ident_FORS`. In
 *   JavaScript ist `[A-Z]` ohne Flag zeichengenau; ein `i` reproduzierte den
 *   Fehler.
 * - **Nicht „ab dem letzten Unterstrich".** Die naive Fassung lieferte für 33
 *   Typen `_SAP` statt `_L_SAP` beziehungsweise `_K_SAP` — und verlöre genau die
 *   Unterscheidung, um die es geht: `Abladestelle_L_SAP` und
 *   `Abladestelle_K_SAP` stehen auf 3.405 Nachrichten eines Monats gemeinsam.
 *
 * **Es wird nicht getrimmt.** Typ 9008 heißt `Beleg-Nr.  TSL _L_SAP` — mit zwei
 * Leerzeichen und einem weiteren vor dem Unterstrich. Der Name behält seine
 * Zeichen unverändert; das Leerzeichen davor ist dort sogar die bessere
 * Umbruchstelle.
 *
 * **Ziffern treffen die Regel nicht, und das ist richtig so.** Fehlt die Zeile
 * in `MessageBAMType`, liefert die Antwort statt einer Beschreibung die
 * **Typnummer** (`docs/bam-werte.md` §6) — `9018` hat keine Endung.
 */

/** Das Ergebnis von {@link zerlegeBeschriftung}. */
export type ZerlegteBeschriftung = {
  /**
   * Alles vor der Endung, **unverändert** — einschließlich Leerzeichen am Ende.
   * Trägt die Beschriftung keine Endung, steht hier die ganze Zeichenkette.
   */
  name: string;
  /**
   * Der abschließende Lauf aus `_GROSSBUCHSTABEN`-Segmenten, mit seinem
   * führenden Unterstrich — oder `null`, wenn es keinen gibt.
   */
  endung: string | null;
};

/**
 * Der abschließende Lauf aus `_GROSSBUCHSTABEN`-Segmenten.
 *
 * **Ohne `i`-Flag** — die Begründung steht im Kopf dieser Datei und ist
 * gemessen, nicht geraten.
 */
const ENDUNG = /(_[A-Z]+)+$/;

/**
 * Zerlegt eine Typbeschreibung in Name und Endung.
 *
 * Eine Beschreibung, die **nur** aus einer Endung besteht, liefert einen leeren
 * Namen — die Regel wird auch dort angewandt und nicht mit einem Sonderfall
 * ausgehebelt. Im gemessenen Bestand kommt das nicht vor (M45‑1: 62
 * Beschreibungen, alle mit Namensteil).
 */
export function zerlegeBeschriftung(bezeichnung: string): ZerlegteBeschriftung {
  const treffer = ENDUNG.exec(bezeichnung);
  if (treffer === null) {
    return { name: bezeichnung, endung: null };
  }
  return { name: bezeichnung.slice(0, treffer.index), endung: treffer[0] };
}
