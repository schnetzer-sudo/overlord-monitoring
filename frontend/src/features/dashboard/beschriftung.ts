import { einsetzen, type Texte } from "@/i18n";

import type { Fehlerart, Verteilungssicht, Verteilungszeile } from "./api";

/**
 * Wie aus einem **Wert** ein **Anzeigetext** wird.
 *
 * Das ist Sache der Oberfläche und nicht des Endpunkts (Regel Q4: *das Backend
 * stellt fest, die Oberfläche beschriftet*). Beides steht hier als reine
 * Funktion und nicht als Ausdruck in einer Komponente — die Regeln sind
 * Entscheidungen und werden als solche geprüft (`docs/frontend-grundlagen.md`
 * §9).
 */

/**
 * Der Anzeigetext einer Fehlerart — **über den Rohwert, nicht über `art`**.
 *
 * | Rohwert | Was dasteht |
 * |---|---|
 * | `COMMIT_REJECTED` | „Vom Partner abgelehnt" — aus der Sprachdatei |
 * | `ERROR_TIMEOUT` | `TIMEOUT` — der Namensteil, den der Endpunkt liefert |
 * | alles andere | der Rohwert, unverändert (Regel Q4) |
 *
 * **Warum nicht einfach `art`.** Der Endpunkt setzt für `COMMIT_REJECTED` einen
 * **deutschen Festtext** (`MessageStatusClassifier.ABGELEHNT_VOM_PARTNER`,
 * `docs/dashboard.md` §2). Stünde der hier unverändert, läse ihn auch ein
 * englischer Nutzer. Für jeden anderen Rohwert ist `art` dagegen kein
 * Anzeigetext, sondern ein Wert — und Werte werden nicht übersetzt.
 *
 * **Der Rohwert geht dabei nicht verloren.** Er steht in der Zeile daneben;
 * ohne ihn wäre *„Vom Partner abgelehnt"* eine Zeichenkette, an der sich nichts
 * mehr festmachen ließe.
 *
 * Verglichen wird ohne Rücksicht auf Groß- und Kleinschreibung — dieselbe
 * Vorsicht, mit der `MessageStatusClassifier` den Rohwert einordnet.
 */
export function fehlerartText(art: Pick<Fehlerart, "rohwert" | "art">, texte: Texte): string {
  const bekannt: Record<string, string> = texte.dashboard.fehlerarten;
  return bekannt[art.rohwert.toUpperCase()] ?? art.art;
}

/**
 * Der Anzeigetext einer Verteilungszeile.
 *
 * | `art` | Was dasteht |
 * |---|---|
 * | `WERT` | der Wert. In der Richtungssicht übersetzt, in der Partnersicht **nie** |
 * | `UEBRIGE` | „Übrige (n)" aus `enthaltene` |
 * | `NICHT_ZUGEORDNET` | „nicht zugeordnet" |
 *
 * **Die Restzeilen tragen keinen Text aus der Antwort** — das Backend liefert
 * dort ausdrücklich keinen (`docs/dashboard.md` §4).
 *
 * **Ein Partnername wird nie übersetzt.** Er ist ein kuratierter Wert aus dem
 * Katalog und gehört dem Mandanten; die Richtung dagegen ist eine geschlossene
 * Menge aus `catalog/Richtung`. Ein unbekannter Richtungswert bleibt roh
 * (Regel Q4).
 */
export function verteilungszeileText(
  zeile: Verteilungszeile,
  sicht: Verteilungssicht,
  texte: Texte,
): string {
  switch (zeile.art) {
    case "UEBRIGE":
      return einsetzen(texte.dashboard.verteilung.uebrige, {
        anzahl: String(zeile.enthaltene ?? 0),
      });
    case "NICHT_ZUGEORDNET":
      return texte.dashboard.verteilung.nichtZugeordnet;
    case "WERT": {
      const wert = zeile.wert ?? "";
      if (sicht !== "RICHTUNG") {
        return wert;
      }
      const richtungen: Record<string, string> = {
        EINGEHEND: texte.dashboard.verteilung.EINGEHEND,
        AUSGEHEND: texte.dashboard.verteilung.AUSGEHEND,
      };
      return richtungen[wert] ?? wert;
    }
  }
}
