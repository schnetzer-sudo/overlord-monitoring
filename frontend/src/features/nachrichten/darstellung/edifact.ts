import { anzahlSegmentenden, umbrecheNachSegmentende } from "./segmente";
import { istZeilenende, vorspannEnde } from "./vorspann";

/**
 * **EDIFACT** — Umbruch nach jedem Segmentende (`docs/dateiansicht-darstellung.md` §3).
 *
 * EANCOM, ODETTE und die EDIFACT-basierten VDA-Empfehlungen sind EDIFACT und
 * landen hier; „VDA" in der Auswahl meint die Festlängenformate (E‑193).
 *
 * ## Erkennung
 *
 * Der Text beginnt mit `UNA` oder mit `UNB+`, und mindestens ein Segmentende
 * kommt vor. Nach `UNA` folgen sechs Zeichen — Komponententrenner,
 * Datenelementtrenner, Dezimalzeichen, Freigabezeichen, reserviert,
 * Segmentende —, danach `UNB`, gegebenenfalls nach einem Zeilenumbruch. Ohne
 * `UNA` gelten `:` `+` `.` `?` Leerzeichen `'`.
 *
 * Das ist eine Prüfung fester Syntaxmerkmale und kein Erraten (E‑199, Regel
 * Q4): Was nicht so beginnt, ist für diese Oberfläche kein EDIFACT — auch wenn
 * es eines sein könnte.
 */

type Trennzeichen = {
  /** Wo die Segmente beginnen — nach dem `UNA`-Segment, oder am Anfang. */
  ab: number;
  daten: string;
  freigabe: string;
  segmentende: string;
};

const STANDARD = { daten: "+", freigabe: "?", segmentende: "'" };

function trennzeichen(text: string): Trennzeichen | null {
  const anfang = vorspannEnde(text);

  if (text.startsWith("UNA", anfang)) {
    if (text.length < anfang + 9) {
      return null;
    }
    const daten = text.charAt(anfang + 4);
    const freigabe = text.charAt(anfang + 6);
    const segmentende = text.charAt(anfang + 8);
    // Nach UNA: UNB, gegebenenfalls nach einem Zeilenumbruch.
    let i = anfang + 9;
    while (i < text.length && istZeilenende(text.charAt(i))) {
      i += 1;
    }
    if (!text.startsWith("UNB" + daten, i)) {
      return null;
    }
    // Das Segmentende des UNA-Segments selbst (Stelle 8) ist das erste
    // Segmentende der Datei; der Umbruch beginnt dort.
    return { ab: anfang + 8, daten, freigabe, segmentende };
  }

  if (text.startsWith("UNB+", anfang)) {
    return { ab: anfang, ...STANDARD };
  }

  return null;
}

export function erkenneEdifact(text: string): boolean {
  const t = trennzeichen(text);
  if (t === null) {
    return false;
  }
  return anzahlSegmentenden(text, t.ab, t.segmentende, t.freigabe) > 0;
}

/** Der Text mit einem Umbruch nach jedem Segmentende — oder `null`, wenn er kein EDIFACT ist. */
export function formatiereEdifact(text: string): string | null {
  if (!erkenneEdifact(text)) {
    return null;
  }
  const t = trennzeichen(text);
  if (t === null) {
    return null;
  }
  return umbrecheNachSegmentende(text, t.ab, t.segmentende, t.freigabe);
}
