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

/*
 * **Hier stand `achsenabstand(eimer, hoechstens)`** — der `interval`-Wert von
 * Recharts, gerechnet aus der Zahl der Eimer und einem Deckel von zwölf
 * Beschriftungen. Er ist am 01.09.2026 entfallen, und zwar an einer Messung:
 * Am 1500 px breiten Fenster trug er, **bei 360 px überlappten die
 * Beschriftungen um 12 Pixel**.
 *
 * Eine Zahl, die von der Breite nichts weiß, kann bei beiden nicht richtig
 * sein — und der Auftrag sagt genau das: *„Welche Dichte tragbar ist,
 * entscheidet der Augenschein am schmalsten unterstützten Fenster."* Die
 * Entscheidung liegt seither bei `interval="equidistantPreserveStart"` und
 * `minTickGap` am Diagramm selbst (`components/verlauf-diagramm.tsx`);
 * gemessen: 6 Beschriftungen bei 360 px, 10 bei 768, 24 bei 1500, kleinster
 * Abstand 12 px.
 */

/**
 * Wie breit die y-Achse sein muss, damit ihre Beschriftung **hineinpasst**.
 *
 * ## Der Befund, der diese Funktion nötig gemacht hat *(01.09.2026)*
 *
 * Sie stand als feste Zahl da — 48 Pixel, genug für vier Stellen. Bei `NEXANS`
 * über zwölf Monate steht am oberen Rand **220.000**, und davon war
 * `:20.000` zu lesen: Recharts beschneidet die Beschriftung an der Achsenbreite,
 * ohne etwas zu melden. Gefunden in der Sichtprüfung am laufenden System, nicht
 * im Test — im Prüfwert der Tests stehen zweistellige Zahlen.
 *
 * **Gerechnet und nicht großzügig geschätzt.** Die Achse trägt
 * `font-size: 11` und `tabular-nums`; eine Ziffer ist damit rund 6,2 Pixel
 * breit, und zwischen Beschriftung und Zeichenfläche liegen zehn. Das eine
 * Zeichen Zuschlag ist Absicht: Recharts rundet die oberste Marke **über** den
 * größten Wert auf, und aus `99.000` wird dabei `100.000`.
 *
 * **Beide Diagramme bekommen dieselbe Breite** — die des Verlaufs, denn seine
 * Zahlen sind die größeren. Sonst stünden die Balken des Streifens nicht mehr
 * unter denen darüber, und die gemeinsame Zeitachse verspräche eine Zuordnung,
 * die es nicht gäbe.
 *
 * @param laengste die längste Beschriftung, die vorkommen kann, in Zeichen
 */
export function achsenbreite(laengste: number): number {
  return Math.max(48, Math.ceil((laengste + 1) * 6.2) + 10);
}
