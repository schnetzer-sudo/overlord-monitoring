import { istZeilenende } from "./vorspann";

/**
 * **Umbruch nach jedem Segmentende** — die gemeinsame Regel von EDIFACT und
 * ANSI X12 (`docs/dateiansicht-darstellung.md` §3).
 *
 * Drei Sätze, und jeder ist eine Eigenschaft, die `tests/darstellung.test.ts`
 * prüft:
 *
 * 1. **Ein Zeilenumbruch direkt hinter dem Segmentende wird übernommen, nicht
 *    verdoppelt.** Eine Datei, die schon je Segment eine Zeile trägt, sieht
 *    danach aus wie vorher.
 * 2. **Zeilenumbrüche innerhalb eines Segments entfallen** — das sind die
 *    umbrochenen Dateien, bei denen ein Segment über mehrere Zeilen läuft.
 * 3. **Das Zeichen hinter einem Freigabezeichen ist nie ein Trenner**: `?'` ist
 *    ein Apostroph in den Daten und `??` ein Fragezeichen. X12 kennt kein
 *    Freigabezeichen und übergibt `null`.
 *
 * Ist das Segmentende selbst ein Zeilenumbruch, bleibt der Text, wie er ist —
 * es gibt dann nichts, was umzubrechen wäre.
 *
 * Außer Zeilenumbrüchen wird kein Zeichen eingefügt, verschoben oder entfernt
 * (E‑197). Der Vorspann bis `ab` geht unverändert voran.
 */
export function umbrecheNachSegmentende(
  text: string,
  ab: number,
  segmentende: string,
  freigabe: string | null,
): string {
  if (istZeilenende(segmentende)) {
    return text;
  }

  const teile: string[] = [text.slice(0, ab)];
  const laenge = text.length;
  // Ob wir gerade in einem Segment stehen. Zwischen zwei Segmenten bleiben
  // Zeilenumbrüche stehen; innerhalb eines Segments entfallen sie.
  let imSegment = false;

  let i = ab;
  while (i < laenge) {
    const zeichen = text.charAt(i);

    if (freigabe !== null && zeichen === freigabe) {
      // Das freigegebene Zeichen geht mit, was immer es ist — auch ein
      // Segmentende oder ein Zeilenumbruch.
      teile.push(text.slice(i, i + 2));
      i += 2;
      imSegment = true;
      continue;
    }

    if (zeichen === segmentende) {
      teile.push(zeichen);
      imSegment = false;
      i += 1;
      if (i < laenge && !istZeilenende(text.charAt(i))) {
        teile.push("\n");
      }
      continue;
    }

    if (istZeilenende(zeichen)) {
      if (!imSegment) {
        teile.push(zeichen);
      }
      i += 1;
      continue;
    }

    teile.push(zeichen);
    imSegment = true;
    i += 1;
  }

  return teile.join("");
}

/**
 * Zählt die Segmentenden ab `ab`, ohne die freigegebenen. Für die Erkennung:
 * Ohne ein einziges Segmentende ist es keine EDI-Übertragung.
 */
export function anzahlSegmentenden(
  text: string,
  ab: number,
  segmentende: string,
  freigabe: string | null,
): number {
  let anzahl = 0;
  for (let i = ab; i < text.length; i += 1) {
    const zeichen = text.charAt(i);
    if (freigabe !== null && zeichen === freigabe) {
      i += 1;
    } else if (zeichen === segmentende) {
      anzahl += 1;
    }
  }
  return anzahl;
}
