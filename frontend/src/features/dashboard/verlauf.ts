import { STATUSARTEN, statusrolle, type Statusart, type Statusrolle } from "@/lib/status-farbe";
import type { Zeitaufloesung } from "@/lib/format";

import type { Dashboardzeitraum, Einordnungszahl, Verlaufspunkt } from "./api";

/**
 * **Vier Reihen, nicht acht** (Entscheidung E‑l).
 *
 * Der Endpunkt liefert je Eimer die vorkommenden **Einordnungen** — acht
 * mögliche. `lib/status-farbe.ts` bildet sie auf **vier** Farbrollen ab. Acht
 * Reihen mit vier Farben ergäben einen Balken, in dem `ABGESCHLOSSEN` und
 * `QUITTIERT` sowie `WARTEND`, `LAEUFT`, `AUFGETEILT` und `ZUSAMMENGEFUEHRT`
 * jeweils farbgleich aneinanderstoßen und **wie ein Segment aussehen**. Die
 * Legende verspräche acht Unterscheidungen, das Bild lieferte vier.
 *
 * **Der Unterschied geht dabei nicht verloren, er wandert in den Tooltip.** Dort
 * stehen die enthaltenen Einordnungen einzeln mit ihren Zahlen — und damit
 * bleibt *aufgeteilt* von *zusammengeführt* unterscheidbar, genau dort, wo
 * jemand nachsieht.
 *
 * **Dieses Modul ist bewusst frei von React.** Die Zusammenfassung ist eine
 * reine Funktion und wird als solche geprüft (`docs/frontend-grundlagen.md` §9).
 */

/**
 * Die Reihenfolge des Stapels — **fest und über alle Eimer dieselbe**.
 *
 * Sie ist eine Anzeigeentscheidung und keine Ableitung: Recharts stapelt in der
 * Reihenfolge, in der die Reihen im Baum stehen, und das erste Segment sitzt
 * unten an der Achse. Dort steht *Fehler* — die Kategorie, wegen der jemand
 * dieses Werkzeug öffnet, liegt damit an der Kante, an der ein Segment von einem
 * Pixel noch am ehesten auffällt. Dahinter das Offene, dann das Abgeschlossene,
 * zuletzt das Ungeklärte.
 *
 * **Farben werden nie nach Position vergeben**, sondern über die Rolle
 * (`lib/status-farbe.ts`). Eine andere Reihenfolge hier verschiebt Segmente und
 * niemals Farben.
 */
export const STAPELREIHENFOLGE: readonly Statusrolle[] = [
  "fehler",
  "offen",
  "abgeschlossen",
  "ungeklaert",
];

/**
 * Eine Zeile des Diagramms: die vier Summen als eigene Felder, weil Recharts je
 * Reihe einen Schlüssel im Datensatz erwartet.
 */
export type Verlaufszeile = Record<Statusrolle, number> & {
  /** Der Anfang des Eimers, UTC — die Achse formatiert ihn. */
  eimer: string;
  /**
   * Die Summe, wie sie das Backend nennt. **Nicht nachgerechnet**: Sie ist die
   * Auskunft der Antwort, und die Summe der vier Reihen soll ihr gleichen.
   */
  gesamt: number;
  /** Die enthaltenen Einordnungen, unverändert — der Tooltip nennt sie einzeln. */
  einordnungen: readonly Einordnungszahl[];
};

function leereZeile(eimer: string, gesamt: number): Verlaufszeile {
  return {
    eimer,
    gesamt,
    einordnungen: [],
    fehler: 0,
    offen: 0,
    abgeschlossen: 0,
    ungeklaert: 0,
  };
}

/**
 * Die Rolle einer Einordnung — **mit Rückfall für einen Wert, den diese Fassung
 * nicht kennt**.
 *
 * Das Backend liefert einen Aufzählungswert; eine neunte Einordnung entstünde
 * nur, wenn dort eine dazukäme. Dann gilt Regel Q4: Sie fällt nach
 * `ungeklaert` — der Rolle, deren ganze Bedeutung *„unbekannter Statuswert"*
 * ist — und ausdrücklich in **keinen** der drei fachlich belegten Eimer. Sie
 * still `offen` oder `abgeschlossen` zuzuschlagen wäre eine Behauptung über
 * einen Wert, über den nichts bekannt ist.
 *
 * **Weggelassen wird sie nicht.** Dann wäre der Balken niedriger als `gesamt`,
 * und die Zahl im Tooltip passte nicht zu dem, was danebensteht.
 *
 * Dieselbe Regel wie in der Statusplakette der Liste, und aus demselben Grund.
 */
function rolleVon(einordnung: string): Statusrolle {
  return (STATUSARTEN as readonly string[]).includes(einordnung)
    ? statusrolle(einordnung as Statusart)
    : "ungeklaert";
}

/**
 * Acht Einordnungen je Eimer → vier Summen je Eimer.
 *
 * Die Reihenfolge der Eimer bleibt, wie der Endpunkt sie liefert — er sortiert
 * bereits über eine `TreeMap`, und ein zweites Sortieren hier wäre ein zweiter
 * Ort für dieselbe Zusicherung.
 */
export function verlaufszeilen(punkte: readonly Verlaufspunkt[]): Verlaufszeile[] {
  return punkte.map((punkt) => {
    const zeile = leereZeile(punkt.eimer, punkt.gesamt);
    zeile.einordnungen = punkt.einordnungen;
    for (const eintrag of punkt.einordnungen) {
      zeile[rolleVon(eintrag.einordnung)] += eintrag.anzahl;
    }
    return zeile;
  });
}

/**
 * Die Einordnungen einer Rolle, für den Tooltip — **einzeln und mit ihren
 * Zahlen**.
 *
 * Ohne sie wäre der Tooltip die Wiederholung dessen, was der Balken schon zeigt;
 * mit ihnen ist er die Stelle, an der die Zusammenfassung wieder aufgeht.
 */
export function einordnungenDerRolle(
  zeile: Verlaufszeile,
  rolle: Statusrolle,
): readonly Einordnungszahl[] {
  return zeile.einordnungen.filter((eintrag) => rolleVon(eintrag.einordnung) === rolle);
}

/**
 * Trägt der Verlauf in dieser Rolle überhaupt etwas?
 *
 * Eine Reihe, die über alle Eimer null ist, bekommt keinen Legendeneintrag: Sie
 * verspräche eine Unterscheidung, die im Bild nicht vorkommt.
 */
export function rolleKommtVor(zeilen: readonly Verlaufszeile[], rolle: Statusrolle): boolean {
  return zeilen.some((zeile) => zeile[rolle] > 0);
}

/**
 * Die Auflösung der Zeitachse — sie folgt der **Eimerbreite** des Paares und
 * nicht der Zahl der Eimer.
 */
export function achsenaufloesung(zeitraum: Dashboardzeitraum): Zeitaufloesung {
  switch (zeitraum) {
    case "48H":
      return "stunde";
    case "30T":
      return "tag";
    case "12M":
      return "monat";
  }
}

/**
 * Wie viele Eimer zwischen zwei beschrifteten Achsenwerten übersprungen werden.
 *
 * **Bei 48 Eimern wird nicht jeder beschriftet** — die Beschriftungen
 * überlagerten sich schon am breiten Fenster. Gerechnet wird aus der Zahl der
 * Eimer und einer Höchstzahl an Beschriftungen; welche Höchstzahl tragbar ist,
 * ist am schmalsten unterstützten Fenster angesehen worden und keine Formel.
 *
 * Recharts erwartet hier den Wert von `interval`: `0` heißt „jeden", `n` heißt
 * „einen, dann n überspringen".
 */
export const ACHSE_HOECHSTENS = 12;

export function achsenabstand(eimer: number, hoechstens: number = ACHSE_HOECHSTENS): number {
  if (eimer <= hoechstens) {
    return 0;
  }
  return Math.ceil(eimer / hoechstens) - 1;
}
