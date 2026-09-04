import { STATUSARTEN, statusrolle, type Statusart, type Statusrolle } from "@/lib/status-farbe";
import type { Zeitaufloesung } from "@/lib/format";
import type { Rollupzeitraum } from "@/lib/rollupzeitraum";

import type { Einordnungszahl, Verlaufspunkt } from "./api";

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
 * **Die Farbe der Fläche — der Akzent, und ausdrücklich keine Statusrolle**
 * (Entscheidung E‑84).
 *
 * Die Fläche trägt seit dem 04.09.2026 die **Gesamtsumme** je Eimer, und eine
 * Summe hat keinen Status. `--status-abgeschlossen` behauptete darüber „alles
 * fertig", `--status-offen` das Gegenteil; beides wäre eine Aussage, die die
 * Zahl nicht trägt. Der Akzent ist die einzige Farbe des Bestands, die
 * *nichts* über die Daten sagt — genau deshalb steht er hier
 * ([`docs/visuelles-konzept.md`](../../../../docs/visuelles-konzept.md) §3).
 *
 * **Zwei Stufen und nicht eine, und beide sind vergeben, wie §3 sie vergibt:**
 * `--akzent` ist dort die **Füllfarbe** („nur Fläche"), `--akzent-schrift` die
 * Stufe für „Verweise, aktive Beschriftungen, **dünne Linien**". Der Farbverlauf
 * ist die Fläche, die Oberkante ist die dünne Linie. §3 nennt dieselbe Paarung
 * als die eine bekannte Grenze der Farbe und ihre Behebung: *„wer die Lücke
 * schließen will, gibt gefüllten Flächen zusätzlich eine Kontur in
 * `--akzent-schrift`"* — eine gefüllte Akzentfläche erreicht auf Weiß nur
 * 1,98 : 1 und verfehlt die 3 : 1 aus WCAG 1.4.11. Die Kontur trägt 5,40 : 1 im
 * hellen und 10,72 : 1 im dunklen Block ([`docs/dunkelmodus.md`](../../../../docs/dunkelmodus.md)
 * §3.3).
 *
 * **Sie stehen hier und nicht im Diagramm** — derselbe Grund wie bei
 * `FUELLUNG` in `lib/status-farbe.ts`: Eine Komponente kennt keinen
 * Farbtokennamen. **Und nicht in `lib/status-farbe.ts`**, obwohl dort die
 * andere Diagrammfarbe wohnt: Diese Datei ist „die eine Stelle, an der eine
 * fachliche Aussage auf eine Farbrolle trifft", und hier trifft ausdrücklich
 * **keine** fachliche Aussage auf eine Farbe. Ein Eintrag dort verspräche eine
 * Zuordnung, die es nicht gibt.
 *
 * Dass `var()` auch **durch einen `<linearGradient>`** ankommt und auflöst, ist
 * am 04.09.2026 gemessen und nicht angenommen
 * ([`docs/frontend-grundlagen.md`](../../../../docs/frontend-grundlagen.md) §8b).
 */
export const VERLAUFSFLAECHE = "var(--akzent)";

/** Die Oberkante der Fläche. Siehe {@link VERLAUFSFLAECHE}. */
export const VERLAUFSKONTUR = "var(--akzent-schrift)";

/**
 * Die Auflösung der Zeitachse — sie folgt der **Eimerbreite** des Paares und
 * nicht der Zahl der Eimer.
 */
export function achsenaufloesung(zeitraum: Rollupzeitraum): Zeitaufloesung {
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
