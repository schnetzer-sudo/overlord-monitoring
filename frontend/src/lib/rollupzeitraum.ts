import { createParser } from "nuqs";

/**
 * Die drei Paare aus **Fensterbreite und Eimerbreite** — `48H`, `30T`, `12M`.
 *
 * ## Warum sie in `lib` stehen und nicht mehr im Dashboard
 *
 * *(seit 02.09.2026, Schritt 10c‑2.)* Sie hießen bis dahin `Dashboardzeitraum`
 * und lagen in `features/dashboard/api.ts`. Die **Prozessansicht** braucht
 * dieselben drei Codes: dieselbe Menge, dieselbe Reihenfolge im Umschalter,
 * dieselbe Prüfung. **Ein Feature importiert nicht aus einem Nachbarfeature**
 * (`docs/frontend-grundlagen.md` §8) — braucht ein zweites Feature einen Typ,
 * wandert der Typ nach `lib` und nicht ins Nachbarfeature.
 *
 * Das ist **dieselbe Bewegung, die das Backend am selben Tag gemacht hat**:
 * `dashboard.Dashboardzeitraum` → `common.Rollupzeitraum`, Entscheidung E‑44 in
 * `docs/process-view.md`. Der Name folgt dorthin, damit dieselbe Sache in beiden
 * Hälften gleich heißt.
 *
 * **Die Alternative wäre eine zweite Liste mit denselben drei Codes gewesen** —
 * und die driftet: Käme je ein viertes Paar dazu, hätte die eine Ansicht es und
 * die andere nicht, und beide sähen richtig aus.
 *
 * ## Was hier bewusst **nicht** steht
 *
 * **Keine Vorgabe.** Welches Paar ohne Angabe gilt, entscheidet das Backend, und
 * es entscheidet je Endpunkt verschieden: Das Dashboard wählt nach Belegung
 * (`docs/dashboard.md` §3), die Prozessansicht nimmt fest `48H` (Entscheidung
 * E‑38). Eine Zahl hier wäre ein zweiter Standardwert und liefe einem von beiden
 * hinterher.
 *
 * **Keine Fenstergrenzen.** Die rechnet das Backend gegen die Anwendungsuhr
 * (Regel Z1) und nennt sie in der Antwort. Im Browser gerechnet wären sie gegen
 * die Browseruhr gerechnet — und die Testkopie liegt Monate hinter der realen
 * Uhrzeit.
 *
 * **Keine Beschriftungen.** Die stehen in den Sprachdateien (`texte.zeitraum`).
 */
export const ROLLUPZEITRAEUME = ["48H", "30T", "12M"] as const;

export type Rollupzeitraum = (typeof ROLLUPZEITRAEUME)[number];

export function istRollupzeitraum(wert: string | null | undefined): wert is Rollupzeitraum {
  return (
    wert !== null && wert !== undefined && (ROLLUPZEITRAEUME as readonly string[]).includes(wert)
  );
}

/**
 * Ein unbekannter Code landet nicht in der URL — er wäre ein garantiertes `400`
 * `zeitraum-unbekannt`, und zwar an **beiden** Endpunkten.
 *
 * **Kein `withDefault`, und damit auch keine `clearOnDefault`-Falle.** Ohne
 * Standardwert prüft `nuqs` gar nicht erst auf ihn; der Parameter verschwindet
 * genau dann, wenn er auf `null` gesetzt wird. Das ist die Regel aus
 * `docs/frontend-grundlagen.md` §8: Ein Standardwert, der etwas *setzt*, gehört
 * nicht in die URL.
 */
export const parseAsRollupzeitraum = createParser<Rollupzeitraum>({
  parse: (wert) => (istRollupzeitraum(wert) ? wert : null),
  serialize: (wert) => wert,
});

/**
 * Welche Zeitraum-Schaltfläche hervorgehoben ist — **was gilt, nicht was in der
 * URL steht.**
 *
 * Solange nichts gewählt ist, hebt der Umschalter das Paar hervor, das der
 * Endpunkt genommen hat; das steht in der Antwort und wird nirgends
 * zurückgeschrieben (Entscheidung E‑n). Fehlt auch die Antwort noch, ist keine
 * Schaltfläche gedrückt — eine vorgemerkte wäre eine Vermutung, die beim
 * Eintreffen der Antwort springt.
 *
 * **Die Regel steht hier und nicht zweimal in zwei Features.** Beide Ansichten,
 * die den Umschalter tragen, brauchen genau sie; und der Unterschied zwischen
 * *gewählt* und *gewirkt* ist der ganze Inhalt von E‑n.
 */
export function hervorgehobenesPaar(
  gewaehlt: Rollupzeitraum | null,
  ausDerAntwort: Rollupzeitraum | undefined,
): Rollupzeitraum | null {
  return gewaehlt ?? ausDerAntwort ?? null;
}

/**
 * Die gelesenen Fenstergrenzen einer Rollup-Antwort, **UTC und `bis`
 * ausschließend**.
 *
 * Beide liegen auf einer **Eimergrenze**; die obere ist der Anfang des
 * *nächsten* Eimers. Das unterscheidet sich absichtlich vom Listen-Endpunkt,
 * der `zeitraum` auf die Sekunde genau auflöst — für ein Diagramm wäre das
 * falsch (`docs/dashboard.md` §2).
 *
 * **Der Typ steht hier und nicht zweimal in zwei Features.** Dashboard und
 * Prozessansicht bekommen dasselbe Feld mit derselben Bedeutung aus demselben
 * Backend-Record (`ZeitfensterResponse`); zweimal deklariert liefe er beim
 * nächsten Feld auseinander, und weil TypeScript strukturell prüft, fiele der
 * Unterschied an keiner Zuweisung auf.
 */
export type Fenster = { von: string; bis: string };

/* ─────────────────────────────────────────────────────────────────────────────
   Das freie Zeitfenster der Prozessansicht (07.09.2026, Schritt 10c‑4b)

   **Nachgebaut aus `lib/filter.ts`, nicht importiert.** Dort stehen die
   Zeitraumcodes der Liste (`24h`/`7d`/`30d`) — eine andere Menge. Zwei Mengen
   unter einem Namen sind der Anfang zweier Mengen; deshalb tragen die Funktionen
   hier ihre eigenen Namen und ihren eigenen Zustandstyp.
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Der Code, den der Baum-Endpunkt für ein freies Fenster nennt. **Nie in der
 * URL** — dort stehen `von`/`bis`; und **kein viertes Paar**: `ROLLUPZEITRAEUME`
 * bleibt bei dreien, genau wie `Rollupzeitraum` im Backend.
 */
export const FREI = "FREI" as const;

/** Was die Antwort des Baums als `zeitraum` nennen kann. */
export type Baumzeitraum = Rollupzeitraum | typeof FREI;

export function istBaumzeitraum(wert: string | null | undefined): wert is Baumzeitraum {
  return wert === FREI || istRollupzeitraum(wert);
}

/**
 * Der Zustand des Baumfensters, wie er in der URL steht: **ein Paar oder
 * `von`/`bis`**, nie beides. Beides zugleich wäre am Endpunkt `400`
 * `zeitfenster-mehrdeutig`, und zwar bewusst statt einer stillen Vorrangregel;
 * {@link mitPaar} und {@link mitFreiemBaumfenster} löschen deshalb jeweils den
 * anderen Modus.
 */
export type Baumfensterzustand = {
  zeitraum: Rollupzeitraum | null;
  von: Date | null;
  bis: Date | null;
};

export type Baumfenstermodus = "vorwahl" | "frei" | "offen";

/**
 * `offen` heißt: nichts gewählt, der Endpunkt nimmt `48H` (E‑38). Das ist ein
 * **eigener Zustand** und nicht „48H" — sonst stünde die Vorgabe an zwei
 * Stellen.
 */
export function baumfenstermodus(zustand: Baumfensterzustand): Baumfenstermodus {
  if (zustand.von !== null || zustand.bis !== null) {
    return "frei";
  }
  return zustand.zeitraum !== null ? "vorwahl" : "offen";
}

/**
 * Der Modus, den die Oberfläche **zeigt** — er kann einen Schritt vor dem der
 * URL liegen.
 *
 * „Frei gewählt, aber noch nichts eingetragen" lässt sich in der URL nicht
 * ausdrücken: Ein freies Fenster ohne beide Zeitpunkte ist von „keine Auswahl"
 * nicht zu unterscheiden. Und es *soll* sich nicht ausdrücken lassen — der
 * freie Modus beginnt bewusst leer, und ein leeres freies Fenster zeigt
 * denselben Baum wie gar keine Auswahl. **Ohne diese Unterscheidung wäre der
 * freie Modus über die Oberfläche gar nicht erreichbar**: Der Klick schriebe
 * einen Zustand, der sich vom vorherigen nicht unterscheidet, und die Felder
 * erschienen nie. Dieselbe Lösung wie `angezeigterModus` in `lib/filter.ts`,
 * gefunden in der Sichtprüfung der Liste am 06.08.2026.
 *
 * @param freiGewaehlt Komponentenzustand: Hat der Nutzer „Frei" gedrückt?
 */
export function angezeigterBaumfenstermodus(
  zustand: Baumfensterzustand,
  freiGewaehlt: boolean,
): Baumfenstermodus {
  const ausDerUrl = baumfenstermodus(zustand);
  return ausDerUrl === "offen" && freiGewaehlt ? "frei" : ausDerUrl;
}

/** Ein Paar löscht ein freies Fenster — beide zugleich wären `400`. */
export function mitPaar(zeitraum: Rollupzeitraum): Baumfensterzustand {
  return { zeitraum, von: null, bis: null };
}

/** Und umgekehrt. Beide `null`: der freie Modus beginnt leer. */
export function mitFreiemBaumfenster(von: Date | null, bis: Date | null): Baumfensterzustand {
  return { zeitraum: null, von, bis };
}

/**
 * Welche Schaltfläche des Umschalters hervorgehoben ist, **mit** dem vierten
 * Knopf: Im freien Modus — auch dem noch leeren — ist es `FREI`; sonst gilt
 * {@link hervorgehobenesPaar}, die Regel des Dashboards, unverändert.
 */
export function hervorgehobenerBaumzeitraum(
  zustand: Baumfensterzustand,
  freiGewaehlt: boolean,
  ausDerAntwort: Baumzeitraum | undefined,
): Baumzeitraum | null {
  if (angezeigterBaumfenstermodus(zustand, freiGewaehlt) === "frei") {
    return FREI;
  }
  return zustand.zeitraum ?? ausDerAntwort ?? null;
}

/**
 * Das Baumfenster als Anfrageparameter — **ISO 8601 in UTC**, wie Richtlinie
 * §5.3 verlangt; `toISOString` liefert genau das. `bis` ist die **letzte
 * enthaltene Stunde**; die eine Stunde bis zum ausschließenden Ende rechnet das
 * Backend.
 *
 * Ein unvollständiges freies Fenster wird mitgeschickt und **nicht** hier
 * abgefangen: Das Backend antwortet `zeitfenster-unvollstaendig`, und die
 * Oberfläche entscheidet nur, wo die Antwort erscheint. Zwei Stellen, die
 * dieselbe Prüfung machen, driften auseinander.
 */
export function baumfensterAlsParameter(zustand: Baumfensterzustand): [string, string][] {
  if (baumfenstermodus(zustand) === "frei") {
    const parameter: [string, string][] = [];
    if (zustand.von !== null) {
      parameter.push(["von", zustand.von.toISOString()]);
    }
    if (zustand.bis !== null) {
      parameter.push(["bis", zustand.bis.toISOString()]);
    }
    return parameter;
  }
  return zustand.zeitraum === null ? [] : [["zeitraum", zustand.zeitraum]];
}
