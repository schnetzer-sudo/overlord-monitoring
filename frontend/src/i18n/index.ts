import { de, type Texte } from "./de";
import { en } from "./en";

/**
 * Zwei Sprachen, **kein Sprachpräfix in der URL**.
 *
 * Die Sprache ist eine Eigenschaft des Nutzers, nicht der Ansicht: Ein geteilter
 * Link soll beim Empfänger in dessen Sprache erscheinen, nicht in der des
 * Absenders. Deshalb liegt die Wahl in einem Cookie und nicht im Pfad.
 *
 * Eine Spalte für die Sprachwahl am Nutzer wird bewusst **nicht** angelegt — das
 * Cookie genügt, solange niemand die Sprache geräteübergreifend erwartet.
 */
export const SPRACHEN = ["de", "en"] as const;

export type Sprache = (typeof SPRACHEN)[number];

export const STANDARDSPRACHE: Sprache = "de";

/** Name des Cookies mit der Sprachwahl. Kein `HttpOnly` — es ist keine Auskunft. */
export const SPRACHE_COOKIE = "overlord_sprache";

/** Ein Jahr. Die Sprachwahl trifft niemand gern zweimal. */
export const SPRACHE_COOKIE_DAUER = 60 * 60 * 24 * 365;

const TEXTE: Record<Sprache, Texte> = { de, en };

export function istSprache(wert: string | undefined | null): wert is Sprache {
  return wert !== undefined && wert !== null && (SPRACHEN as readonly string[]).includes(wert);
}

/** Fällt bei unbekanntem oder fehlendem Wert auf Deutsch zurück. */
export function spracheAus(wert: string | undefined | null): Sprache {
  return istSprache(wert) ? wert : STANDARDSPRACHE;
}

export function texteFuer(sprache: Sprache): Texte {
  return TEXTE[sprache];
}

/**
 * Setzt Werte in einen Text ein: `einsetzen(texte.…bamWeitere, { anzahl: 4 })`.
 *
 * **Warum überhaupt Platzhalter.** Ein Satz wie „+4 weitere" aus Bausteinen
 * zusammenzusetzen, hieße die Wortstellung im Code festzulegen — und die ist je
 * Sprache eine andere. Mit einem Platzhalter bleibt der ganze Satz in der
 * Sprachdatei, und die Übersetzung darf ihn umstellen.
 *
 * Bewusst winzig: keine Pluralregeln, keine Formatierung, keine Bibliothek. Wo
 * eine Zahl formatiert werden muss, geschieht das vorher über `lib/format.ts`.
 */
export function einsetzen(text: string, werte: Record<string, string | number>): string {
  return text.replace(/\{(\w+)\}/g, (unveraendert, name: string) =>
    name in werte ? String(werte[name]) : unveraendert,
  );
}

export type { Texte };
