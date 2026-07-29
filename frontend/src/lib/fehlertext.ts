import type { Texte } from "@/i18n";

import { ProblemFehler } from "./http";

/**
 * Übersetzt eine Fehlerantwort für die Anzeige.
 *
 * **Übersetzt wird anhand des maschinenlesbaren `type`, nicht anhand von
 * `detail`.** Der Text aus dem Backend ist deutsch und für den Nutzer lesbar —
 * aber eben deutsch. Ohne einen Schlüssel wäre eine zweisprachige Oberfläche
 * nicht sauber möglich; man müsste Texte vergleichen, und die ändern sich.
 *
 * Fehlt eine Übersetzung, ist `detail` die **Rückfallebene**: lieber ein
 * richtiger Satz in der falschen Sprache als „unbekannter Fehler".
 */
export type Fehleranzeige = {
  text: string;
  /** Korrelations-ID aus der Antwort. Nur bei technischen Fehlern gesetzt. */
  kennung?: string;
  /** Feldbezogene Prüffehler, falls das Backend welche geliefert hat. */
  felder: readonly { feld: string; meldung: string }[];
};

export function fehleranzeige(fehler: unknown, texte: Texte): Fehleranzeige {
  if (!(fehler instanceof ProblemFehler)) {
    return { text: texte.fehler.unbekannt, felder: [] };
  }

  const katalog: Record<string, string | undefined> = texte.fehler;
  const text = katalog[fehler.typ] ?? fehler.detail ?? texte.fehler.unbekannt;

  return {
    text,
    // Die Kennung hilft nur bei einem technischen Fehler weiter — bei „Passwort
    // falsch" wäre sie Rauschen.
    kennung: fehler.status >= 500 ? fehler.traceId : undefined,
    felder: fehler.felder,
  };
}
