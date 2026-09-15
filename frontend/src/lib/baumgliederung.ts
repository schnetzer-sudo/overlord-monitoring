import { createParser } from "nuqs";

/**
 * Die zwei Gliederungen des Prozessbaums — `PARTNER` und `PROJEKT`
 * (`docs/process-view.md` §48).
 *
 * | Wert | Ebenen, von außen nach innen |
 * |---|---|
 * | `PARTNER` | Partner, Richtung, Prozess — am kuratierten Katalog |
 * | `PROJEKT` | Projekt, Prozess — ohne Kuratierung |
 *
 * ## Warum in `lib` und nicht im Feature
 *
 * Zwei Features brauchen die Menge: `features/nachrichten` gliedert den Baum
 * danach, `features/benutzer` pflegt die Vorgabe je Konto. **Ein Feature
 * importiert nicht aus einem Nachbarfeature** (`docs/frontend-grundlagen.md`
 * §8) — dieselbe Bewegung wie `common/Baumgliederung` im Backend und wie
 * `lib/rollupzeitraum.ts` am 02.09.2026.
 *
 * ## Was hier bewusst **nicht** steht
 *
 * **Keine Vorgabe.** Welche Gliederung ohne Angabe gilt, entscheidet der Server,
 * und zwar **je Konto** (`app_user.tree_layout`). Ein Standardwert hier wäre für
 * jeden Nutzer derselbe, während der echte je Konto verschieden ist — er liefe
 * dem Server nicht hinterher, er widerspräche ihm.
 *
 * **Keine Beschriftungen.** Die stehen in den Sprachdateien.
 */
export const BAUMGLIEDERUNGEN = ["PARTNER", "PROJEKT"] as const;

export type Baumgliederung = (typeof BAUMGLIEDERUNGEN)[number];

export function istBaumgliederung(wert: string | null | undefined): wert is Baumgliederung {
  return (
    wert !== null && wert !== undefined && (BAUMGLIEDERUNGEN as readonly string[]).includes(wert)
  );
}

/**
 * Die Gliederung in der URL (E‑143).
 *
 * **Ohne `withDefault`, und das ist die Entscheidung.** Fehlt der Parameter,
 * steht nichts in der URL, und der Server setzt die Vorgabe des angemeldeten
 * Kontos ein. Die Oberfläche liest die aktive Gliederung dann aus der
 * **Antwort**. Ein geteilter Link ohne Parameter zeigt beim Empfänger damit
 * dessen eigene Vorgabe — erst wer umschaltet, nimmt die Wahl in den Link mit.
 *
 * **Ein unbekannter Wert landet nicht in der Anfrage** — er wäre am Endpunkt ein
 * garantiertes `400 gliederung-unbekannt`. Aus der URL gelesen gilt er als nicht
 * gewählt, dieselbe Bauform wie `parseAsRollupzeitraum`.
 */
export const parseAsBaumgliederung = createParser<Baumgliederung>({
  parse: (wert) => (istBaumgliederung(wert) ? wert : null),
  serialize: (wert) => wert,
});
