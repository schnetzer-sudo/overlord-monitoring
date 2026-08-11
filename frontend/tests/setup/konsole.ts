import { format } from "node:util";
import { afterAll, afterEach } from "vitest";

/**
 * **Ein `console.error` lässt den Testlauf fehlschlagen** — mit der
 * ursprünglichen Meldung im Fehlertext.
 *
 * ## Warum es diese Datei gibt
 *
 * Am 11.08.2026 trugen der Ketten- und der Eigenschaftenblock beide
 * `key={messageId}` und standen damit im selben Elternteil auf demselben Platz
 * im Baum. React meldete *„Encountered two children with the same key"* — in der
 * Konsole. **Kein Test hätte das gefunden:** Die Vitests prüfen Entscheidungen
 * und rendern keinen Baum, und sichtbar falsch war nichts. Gefunden hat es die
 * Sichtprüfung (`docs/verkettung.md` §8.12).
 *
 * Das war keine Nachlässigkeit, sondern eine Lücke im Werkzeug: Die ganze
 * Fehlerklasse „steht nur in der Konsole" hatte kein Netz. Sie schließt sich
 * nicht dadurch, dass man beim nächsten Mal genauer hinsieht.
 *
 * ## Drei Regeln, und sie sind der eigentliche Inhalt
 *
 * 1. **Keine pauschale Ausnahmeliste.** Wird ein bestehender Test rot, ist die
 *    Meldung die Nachricht und nicht das Problem — behoben wird die Ursache.
 * 2. Kommt eine Meldung aus einer Fremdbibliothek und ist sie nachweislich
 *    nicht abstellbar, wird sie **einzeln** in {@link AUSNAHMEN} aufgenommen:
 *    exakte Meldung, ein Satz Begründung, Datum. Keine Muster, keine
 *    Platzhalter.
 * 3. Wird es viel, wird angehalten und berichtet, statt zwanzig Tests
 *    umzuschreiben.
 *
 * **`console.warn` bleibt vorerst außen vor.** Der Doppelschlüssel kommt über
 * `console.error`; eine zweite Verschärfung in derselben Runde machte den Befund
 * unlesbar.
 *
 * ## Warum die Prüfung in `afterEach` steht und nicht im Aufruf wirft
 *
 * Eine Ausnahme aus `console.error` heraus schlüge mitten im Rendervorgang zu
 * und stünde am Ende als irgendein React-Fehler da — nicht als die Meldung, um
 * die es geht. Gesammelt wird deshalb, und fehlschlagen tut der Test, der die
 * Meldung ausgelöst hat. Die Ausgabe bleibt daneben stehen: Wer den Lauf
 * beobachtet, soll die Meldung dort sehen, wo sie entstanden ist.
 */

/** Eine einzeln aufgenommene, nachweislich nicht abstellbare Fremdmeldung. */
type Ausnahme = {
  /** Die **exakte** Meldung, wie sie in der Konsole steht. Kein Teilstück, kein Muster. */
  meldung: string;
  /** Ein Satz: warum sie nicht abstellbar ist. */
  grund: string;
  /** Tag der Aufnahme, damit eine Ausnahme altern kann. */
  seit: string;
};

/**
 * **Absichtlich leer.** Jeder Eintrag hier ist eine Fehlerklasse, die dieses
 * Netz nicht mehr fängt — und deshalb steht er einzeln da, mit Begründung und
 * Datum, oder gar nicht.
 */
const AUSNAHMEN: readonly Ausnahme[] = [];

/**
 * `util.format` löst die Platzhalter auf, mit denen React seine Warnungen
 * schreibt (`"… same key, `%s`. …", key`). Ohne das stünde im Fehlertext der
 * Rohsatz und nicht der Schlüssel, um den es geht.
 */
const formatiere = format as (...argumente: readonly unknown[]) => string;

const gesammelt: string[] = [];
const echteAusgabe = console.error;

console.error = (...argumente: readonly unknown[]): void => {
  const meldung =
    argumente.length === 0 ? "console.error() ohne Argumente" : formatiere(...argumente);
  if (!AUSNAHMEN.some((ausnahme) => ausnahme.meldung === meldung)) {
    gesammelt.push(meldung);
  }
  echteAusgabe(...argumente);
};

function pruefe(wo: string): void {
  if (gesammelt.length === 0) {
    return;
  }
  const meldungen = gesammelt.splice(0, gesammelt.length);
  throw new Error(
    `${meldungen.length === 1 ? "Eine Meldung" : `${meldungen.length} Meldungen`} auf ` +
      `console.error ${wo}. Die Meldung ist der Befund, nicht das Problem — ` +
      `behoben wird die Ursache (tests/setup/konsole.ts):\n\n${meldungen.join("\n\n")}`,
  );
}

afterEach(() => pruefe("während des Tests"));

afterAll(() => {
  try {
    pruefe("außerhalb eines Tests");
  } finally {
    console.error = echteAusgabe;
  }
});
