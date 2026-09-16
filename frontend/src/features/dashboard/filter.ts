import { createParser } from "nuqs";

import {
  hervorgehobenesPaar,
  istRollupzeitraum,
  parseAsRollupzeitraum,
  type Rollupzeitraum,
} from "@/lib/rollupzeitraum";

import {
  VERTEILUNGSSICHTEN,
  VERTEILUNG_VORGABE,
  istVerteilungssicht,
  type Verteilungssicht,
} from "./api";

/**
 * Der URL-Zustand des Dashboards — **nur die ausdrückliche Wahl**
 * (Entscheidung E‑n).
 *
 * | | |
 * |---|---|
 * | Ohne Klick | **kein** `zeitraum` in der URL, **kein** `verteilung`. Der Endpunkt wählt |
 * | Nach einem Klick | der gewählte Wert steht in der URL, über `nuqs`, wie in jeder anderen Ansicht |
 * | **Nie zurückgeschrieben** | das vom Endpunkt **gewählte** Paar landet nicht in der URL |
 *
 * **Das ist die Regel aus `docs/frontend-grundlagen.md` §8:** *Ein Standardwert,
 * der etwas weglässt, gehört in die URL; einer, der etwas setzt, nicht.* Beide
 * Parameter setzen etwas — sie lassen nichts weg, was ein Empfänger des Links
 * sonst nicht sähe.
 *
 * **Kein `withDefault`, und damit auch keine `clearOnDefault`-Falle.** Ohne
 * Standardwert prüft `nuqs` gar nicht erst auf ihn; der Parameter verschwindet
 * genau dann, wenn er auf `null` gesetzt wird. Käme hier je ein `withDefault`
 * dazu, gehörte `clearOnDefault: false` in dieselbe Zeile — oder eine
 * ausdrückliche Entscheidung dagegen.
 *
 * **`verteilung=PARTNER` steht aus demselben Grund nicht in der URL**, obwohl es
 * eine ausdrückliche Wahl sein kann: Es ist die Vorgabe, und die Adresse mit und
 * ohne den Parameter zeigt dasselbe. `RICHTUNG` schon. Das ist eine **Ableitung
 * aus E‑n und keine neue Entscheidung.**
 *
 * > ⚠️ **Seit dem 16.09.2026 ist `verteilung` ein URL-Parameter und kein
 * > Anfrageparameter mehr** (E‑161). Er beschreibt, welche Hälfte der Antwort zu
 * > sehen ist, und geht nicht an den Endpunkt — dieselbe Bauform wie `nachricht`
 * > in der Nachrichtenliste (`docs/nachrichtendetail.md` §10.2,
 * > `docs/frontend-grundlagen.md` §8, „Die dritte Regel"). **Die Vorgabe
 * > `PARTNER` liegt damit allein hier** (`VERTEILUNG_VORGABE`). Sie kommt
 * > trotzdem **nicht** als `withDefault` an den Parser: Der Zustand soll weiter
 * > „nicht gewählt" (`null`) von „gewählt" unterscheiden, und die Vorgabe wirkt
 * > erst in `hervorgehobeneSicht`. Wer das je umbaut, beachtet die
 * > `clearOnDefault`-Falle in ihrer Umkehrung: `nuqs` 2 steht auf
 * > `clearOnDefault: true` und hielte `PARTNER` damit aus der URL, wie E‑n es
 * > verlangt — ein `clearOnDefault: false` aus Gewohnheit schriebe es hinein.
 *
 * **Dieses Modul ist bewusst frei von React** — die Umrechnung Zustand → URL und
 * Zustand → Adresse ist eine reine Funktion und wird als solche geprüft.
 */

export const parseAsVerteilungssicht = createParser<Verteilungssicht>({
  parse: (wert) => (istVerteilungssicht(wert) ? wert : null),
  serialize: (wert) => wert,
});

export const DASHBOARD_PARAMETER = {
  zeitraum: parseAsRollupzeitraum,
  verteilung: parseAsVerteilungssicht,
};

/**
 * Was in der URL steht — **nicht**, was gilt.
 *
 * `null` heißt „nicht gewählt"; welches Paar dann wirkt, sagt allein die
 * Antwort. Die beiden Begriffe getrennt zu halten ist der ganze Inhalt von E‑n:
 * Wäre der gewählte Wert derselbe Zustand wie der gewirkte, müsste die Ansicht
 * ihn zurückschreiben, und ein Aufruf ohne Parameter wäre nach dem ersten
 * Rendern keiner mehr.
 */
export type Dashboardzustand = {
  zeitraum: Rollupzeitraum | null;
  verteilung: Verteilungssicht | null;
};

export const LEERER_ZUSTAND: Dashboardzustand = { zeitraum: null, verteilung: null };

/**
 * Welche Zeitraum-Schaltfläche hervorgehoben ist.
 *
 * **Die Wahl schlägt die Antwort nicht — sie ist dieselbe.** Solange nichts
 * gewählt ist, hebt der Umschalter das Paar hervor, das der Endpunkt genommen
 * hat; das steht in der Antwort und wird nirgends zurückgeschrieben. Fehlt auch
 * die Antwort noch, ist keine Schaltfläche gedrückt — eine vorgemerkte wäre eine
 * Vermutung, die beim Eintreffen der Antwort springt.
 */
export function hervorgehobenerZeitraum(
  zustand: Dashboardzustand,
  ausDerAntwort: Rollupzeitraum | undefined,
): Rollupzeitraum | null {
  // Die Regel selbst steht in `lib/rollupzeitraum.ts` — die Prozessansicht
  // braucht sie wörtlich genauso, und zweimal geschrieben liefe sie auseinander.
  return hervorgehobenesPaar(zustand.zeitraum, ausDerAntwort);
}

/**
 * Welche Sicht der Verteilungsblock zeigt — **und welche Schaltfläche gedrückt
 * ist**: die aus der URL, sonst `PARTNER`.
 *
 * Anders als beim Zeitraum genügt hier die Vorgabe: Sie hängt nicht am
 * Mandanten, und seit dem 16.09.2026 liegt sie im Frontend (E‑161). **Die
 * Antwort nennt keine Sicht mehr** — sie trägt beide, und diese Funktion
 * entscheidet allein, welche Hälfte zu sehen ist. Bis dahin las der Block die
 * Sicht aus der Antwort, und diese Funktion beantwortete nur, welche
 * Schaltfläche gedrückt aussah, bevor eine Antwort da war.
 */
export function hervorgehobeneSicht(zustand: Dashboardzustand): Verteilungssicht {
  return zustand.verteilung ?? VERTEILUNG_VORGABE;
}

/**
 * Der nächste URL-Zustand nach einem Klick auf eine Sicht.
 *
 * **Die Vorgabe wird zu `null` und verschwindet damit aus der URL.** Wer von
 * `RICHTUNG` zurück auf `PARTNER` klickt, hat wieder keine Absicht ausgedrückt,
 * die über die Vorgabe hinausginge — und eine URL, die die Vorgabe nennt, sagt
 * dem Empfänger nichts, was er ohne sie nicht sähe.
 */
export function mitSicht(sicht: Verteilungssicht): Verteilungssicht | null {
  return sicht === VERTEILUNG_VORGABE ? null : sicht;
}

/**
 * Der Zustand als **URL** — die Gegenrichtung zu dem, was `nuqs` beim Rendern
 * tut, als reine Funktion für den Test.
 *
 * Die Reihenfolge ist fest, damit zwei gleiche Zustände dieselbe URL ergeben.
 */
export function alsSuchparameter(zustand: Dashboardzustand): URLSearchParams {
  const parameter = new URLSearchParams();
  if (zustand.zeitraum !== null) {
    parameter.set("zeitraum", zustand.zeitraum);
  }
  if (zustand.verteilung !== null && zustand.verteilung !== VERTEILUNG_VORGABE) {
    parameter.set("verteilung", zustand.verteilung);
  }
  return parameter;
}

/** Liest den Zustand aus einer URL. Ein unbrauchbarer Wert wird übergangen. */
export function ausSuchparametern(suchparameter: URLSearchParams): Dashboardzustand {
  const zeitraum = suchparameter.get("zeitraum");
  const verteilung = suchparameter.get("verteilung");
  return {
    zeitraum: istRollupzeitraum(zeitraum) ? zeitraum : null,
    verteilung: istVerteilungssicht(verteilung) ? verteilung : null,
  };
}

/** Die beiden Sichten in der Reihenfolge, in der sie im Umschalter stehen. */
export const SICHT_REIHE = VERTEILUNGSSICHTEN;
