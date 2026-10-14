/**
 * **Der Verlauf des Browsers — gelesen, nie geschrieben** *(14.10.2026, E‑241)*.
 *
 * Zwei Fragen beantwortet die Oberfläche aus dem Verlauf, und beide stellt sie
 * erst im Augenblick des Bedarfs:
 *
 * - **Welche Adresse steht davor?** Ein Schritt zurück per `router.back()` ist
 *   nur erlaubt, wenn die vorige Seite nachweislich in dieser Anwendung liegt —
 *   sonst führte das Schließen einer per Link geteilten Datei aus der Anwendung
 *   hinaus (`docs/rohdaten-frontend.md` §4, Korrektur vom 14.10.2026).
 * - **Welche Adresse steht danach?** Nach dem Schritt zurück aus einer Datei ist
 *   sie der folgende Eintrag. An ihr findet die Nachricht den Auslöser, der den
 *   Fokus zurückbekommt.
 *
 * **Gemerkt wird dafür nichts** — kein Store, kein `sessionStorage`, kein
 * eigener Eintrag in `history.state`. Die Adressen stehen ohnehin im Verlauf; die
 * URL bleibt die einzige Zustandsquelle (`docs/PROJEKTBESCHREIBUNG.md` §6).
 *
 * ## Über die Navigation API, und nur über sie
 *
 * `window.history` nennt keine Adressen außer der aktuellen, und
 * `document.referrer` ändert sich bei einer Navigation innerhalb der Seite
 * nicht. Die Navigation API führt die Einträge **gleicher Herkunft** samt ihrer
 * Adresse (`navigation.entries()`).
 *
 * **Fehlt sie, gibt es keinen Beleg**, und die Aufrufer bleiben bei ihrem
 * bisherigen Weg. Das betrifft Firefox vor 147 und Safari vor 26.2 — beide
 * liegen innerhalb der Standardziele von Next.js (Firefox 111, Safari 16.4) —
 * und jsdom, weshalb die Tests den Verlauf stellen.
 *
 * TypeScript 5.9 kennt `window.navigation` noch nicht, wohl aber
 * `NavigationHistoryEntry`. Beschrieben ist deshalb nur der Ausschnitt, der hier
 * gelesen wird.
 */
type Verlaufszugang = {
  readonly currentEntry: NavigationHistoryEntry | null;
  entries(): NavigationHistoryEntry[];
};

function verlauf(): Verlaufszugang | undefined {
  if (typeof window === "undefined") {
    return undefined;
  }
  return (window as Window & { navigation?: Verlaufszugang }).navigation;
}

/**
 * Die Adresse des Nachbareintrags — oder `null`, wenn der Browser keine belegt:
 * ohne Navigation API, am Rand des Verlaufs, bei einem Eintrag anderer Herkunft
 * (er steht nicht in `entries()`).
 */
function nachbar(abstand: -1 | 1): string | null {
  const zugang = verlauf();
  const aktuell = zugang?.currentEntry;
  if (zugang === undefined || aktuell === null || aktuell === undefined || aktuell.index < 0) {
    return null;
  }
  return zugang.entries()[aktuell.index + abstand]?.url ?? null;
}

/** Die volle Adresse des vorigen Eintrags, wenn er dieselbe Herkunft hat. */
export function vorigeAdresse(): string | null {
  return nachbar(-1);
}

/** Die volle Adresse des folgenden Eintrags, wenn er dieselbe Herkunft hat. */
export function folgendeAdresse(): string | null {
  return nachbar(1);
}
