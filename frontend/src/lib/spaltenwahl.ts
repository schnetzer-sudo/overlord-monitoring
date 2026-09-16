/**
 * **Spaltenwahl nach dem Platz** — die Rechnung hinter den Container-Schwellen
 * der Tabellen (`docs/spaltenwahl.md`, E‑147).
 *
 * Die Spaltenmenge einer Tabelle folgt der Breite **ihres Containers** und nicht
 * der des Fensters. Eine Fensterschwelle müsste die Breite der Navigationsspalte
 * und die des Panels festschreiben und bräche beim nächsten Umbau erneut; der
 * Container weiß beides von selbst (M176: bei 768 px erscheint die
 * Navigationsspalte, `main` hat 560 px, und im selben Moment kam eine Spalte
 * dazu, die auf 0 px fiel).
 *
 * ## Was hier steht und was nicht
 *
 * Hier steht nur die **Rechnung**: Eine Schwelle ist die Summe der
 * Mindestbreiten aller Spalten, die ab dort sichtbar sind, plus Rinne. Sie ist
 * keine Zahl, die gut aussieht, sondern eine Summe von Messungen.
 *
 * **Die Klassen stehen nicht hier**, sondern als wörtliche Zeichenketten an den
 * Zellen: Tailwind findet eine Klasse nur, wenn sie im Quelltext steht, und eine
 * zusammengesetzte Zeichenkette erzeugte kein CSS. Dass die wörtliche Klasse und
 * die Rechnung dieselbe Zahl tragen, sichert `tests/spaltenwahl.test.tsx` zu —
 * auf den Pixel, in beide Richtungen.
 *
 * Reine Funktionen, ohne React.
 */

/**
 * Pixel je `rem` bei 100 % Wurzelschrift. **Die Mindestbreiten sind in Dichte `m`
 * gemessen** und werden in `rem` geschrieben: Schrift und Spaltenbreite skalieren
 * dann in allen vier Dichtestufen mit derselben Zahl
 * (`docs/visuelles-konzept.md` §5).
 */
export const PX_JE_REM = 16;

export type Spalte = {
  /** Derselbe Schlüssel wie in der Sprachdatei, `…spalten.<schluessel>`. */
  schluessel: string;
  /**
   * Die **gemessene** Mindestbreite in ganzen Pixeln, einschließlich Innenabstand:
   * das Größte aus Kopfbeschriftung (beide Sprachen), längstem Zellinhalt ohne
   * Notumbruch und der Rechnung über eine endliche Beschriftungsmenge —
   * aufgerundet (`docs/spaltenwahl.md` §3).
   */
  mindestbreite: number;
};

export type Spaltenwahl = {
  /** Der Name des Containers: `@container/<container>`. */
  container: string;
  /** Die Spalten, die bei jeder Breite stehen. */
  grundmenge: readonly Spalte[];
  /** Die zuschaltbaren Stufen, in der Reihenfolge, in der sie dazukommen. */
  stufen: readonly (readonly Spalte[])[];
  /**
   * Die eine Spalte **ohne** feste Breite — sofern die Tabelle eine hat. Sie
   * bekommt, was übrig bleibt, und an ihrer Schwelle genau ihre Mindestbreite.
   *
   * **`undefined` heißt: keine** (E‑149, `docs/spaltenwahl.md` §5.5). Dann trägt
   * jede Spalte ihre Mindestbreite, und `table-layout: fixed` teilt den
   * Überschuss **anteilig** auf alle sichtbaren Spalten auf. Eine freie Spalte
   * lohnt sich nur, wo ihr Inhalt den Platz auch nutzt: Ein gekürzter Ablaufname
   * zeigt mit jedem Pixel mehr, eine Reihe kurzer Marken nicht.
   */
  frei?: string;
  /** Was zwischen Containerkante und Spaltensumme liegt, in Pixeln. */
  rinne: number;
};

const summe = (spalten: readonly Spalte[]) =>
  spalten.reduce((gesamt, spalte) => gesamt + spalte.mindestbreite, 0);

/** Die Breite der Grundmenge in Pixeln — darunter gilt die Bauform der Tabelle. */
export function grundmengeInPixeln(wahl: Spaltenwahl): number {
  return summe(wahl.grundmenge) + wahl.rinne;
}

/**
 * Die Schwelle einer Stufe in Pixeln: **die Summe der Mindestbreiten aller
 * Spalten, die ab dort sichtbar sind, plus Rinne.**
 */
export function schwelleInPixeln(wahl: Spaltenwahl, stufe: number): number {
  if (!Number.isInteger(stufe) || stufe < 0 || stufe >= wahl.stufen.length) {
    throw new RangeError(`Stufe ${stufe} gibt es nicht (${wahl.stufen.length} Stufen)`);
  }
  return (
    grundmengeInPixeln(wahl) + wahl.stufen.slice(0, stufe + 1).reduce((g, s) => g + summe(s), 0)
  );
}

/** Die Stufe, in der eine Spalte dazukommt — `null` für die Grundmenge. */
export function stufeVon(wahl: Spaltenwahl, schluessel: string): number | null {
  if (wahl.grundmenge.some((spalte) => spalte.schluessel === schluessel)) {
    return null;
  }
  const stufe = wahl.stufen.findIndex((s) => s.some((spalte) => spalte.schluessel === schluessel));
  if (stufe < 0) {
    throw new RangeError(`Spalte ${schluessel} gehört nicht zu ${wahl.container}`);
  }
  return stufe;
}

/** Die Mindestbreite einer Spalte in Pixeln. */
export function mindestbreiteVon(wahl: Spaltenwahl, schluessel: string): number {
  const spalte = [...wahl.grundmenge, ...wahl.stufen.flat()].find(
    (s) => s.schluessel === schluessel,
  );
  if (spalte === undefined) {
    throw new RangeError(`Spalte ${schluessel} gehört nicht zu ${wahl.container}`);
  }
  return spalte.mindestbreite;
}

/** Die Spalten, die bei einer Containerbreite (Pixel) sichtbar sind, in Stufenfolge. */
export function sichtbareSpalten(wahl: Spaltenwahl, breite: number): Spalte[] {
  const sichtbar = [...wahl.grundmenge];
  wahl.stufen.forEach((stufe, index) => {
    if (breite >= schwelleInPixeln(wahl, index)) {
      sichtbar.push(...stufe);
    }
  });
  return sichtbar;
}
