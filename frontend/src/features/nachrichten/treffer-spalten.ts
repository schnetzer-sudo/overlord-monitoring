import type { Spalte, Spaltenwahl } from "@/lib/spaltenwahl";

/**
 * **Die Spaltenwahl der Trefferliste** (`docs/spaltenwahl.md` §5.1, E‑147) — die
 * gemessenen Mindestbreiten in ganzen Pixeln (M177) und die Stufe, in der der
 * Ablauf dazukommt.
 *
 * Die Breiten stehen als wörtliche `rem`-Klassen an den Kopfzellen
 * (`components/treffer-tabelle.tsx`), die Sichtbarkeit steht hier — beides
 * wörtlich, weil Tailwind nur findet, was im Quelltext steht.
 * `tests/spaltenwahl.test.tsx` hält die Klassen gegen diese Zahlen, auf den Pixel.
 *
 * **Zeitpunkt, Status und Ablauf sind dieselben Zellen wie in der
 * Nachrichtenliste** und tragen dieselbe Mindestbreite: das Größte aus beiden
 * Tabellen. Die 304 px des Ablaufs kommen aus der Nachrichtenliste — die
 * Trefferliste zeigte in der Messung 51 Zeilen, die Liste 300, und dieselbe
 * Zelle kann hier jeden Ablaufnamen tragen.
 */
const ZEITPUNKT: Spalte = { schluessel: "zeitpunkt", mindestbreite: 187 };
/** Die Plakette allein; der Zusatz „Schritt: …" daneben kürzt (`nachrichtenliste.md` §8.1). */
const STATUS: Spalte = { schluessel: "status", mindestbreite: 155 };
/** Die längste Belegart-Bezeichnung beider Mandanten, allein (NEXANS, 40 Typen). */
const TREFFER: Spalte = { schluessel: "treffer", mindestbreite: 280 };
/** Die Kettenrollen einzeln; alle vier verkettet stehen in keiner Zeile. */
const KETTE: Spalte = { schluessel: "kette", mindestbreite: 74 };
const ABLAUF: Spalte = { schluessel: "ablauf", mindestbreite: 304 };

/** Mit der Spalte „Treffer" — sobald eine Belegnummer gesucht wurde. */
export const TREFFERLISTE: Spaltenwahl = {
  container: "trefferliste",
  grundmenge: [ZEITPUNKT, STATUS, TREFFER, KETTE],
  stufen: [[ABLAUF]],
  frei: "ablauf",
  rinne: 0,
};

/** Ohne sie — bei einer reinen Feldsuche (`property-suche.md` E‑110). Derselbe Container, eine frühere Schwelle. */
export const TREFFERLISTE_OHNE_TREFFER: Spaltenwahl = {
  ...TREFFERLISTE,
  grundmenge: [ZEITPUNKT, STATUS, KETTE],
};

/**
 * Die Sichtbarkeit des Ablaufs, wörtlich: `hidden` unter der Schwelle, eine
 * Tabellenzelle ab ihr — an `th` **und** `td`, nie eine Breite von 0 px.
 * 1.000 px = 696 + 304, 720 px = 416 + 304.
 */
export const TREFFER_SICHTBAR = {
  ablaufMitTreffer: "hidden @min-[62.5rem]/trefferliste:table-cell",
  ablaufOhneTreffer: "hidden @min-[45rem]/trefferliste:table-cell",
} as const;
