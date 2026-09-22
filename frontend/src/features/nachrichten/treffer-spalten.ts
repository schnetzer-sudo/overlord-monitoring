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
const ABLAUF: Spalte = { schluessel: "ablauf", mindestbreite: 304 };

/**
 * **Die Spalte „Kette" gibt es seit dem 22.09.2026 nicht mehr** (E‑231,
 * `docs/bam-suche.md` §11.5, Korrekturblock). Sie trug 74 px und stand in beiden
 * Grundmengen; die Schwellen sind allein um diese 74 px gesunken, die übrigen
 * Breiten sind die aus M177 (Punkt 183 bleibt offen).
 */

/** Mit der Spalte „Treffer" — sobald eine Belegnummer gesucht wurde. */
export const TREFFERLISTE: Spaltenwahl = {
  container: "trefferliste",
  grundmenge: [ZEITPUNKT, STATUS, TREFFER],
  stufen: [[ABLAUF]],
  frei: "ablauf",
  rinne: 0,
};

/** Ohne sie — bei einer reinen Feldsuche (`property-suche.md` E‑110). Derselbe Container, eine frühere Schwelle. */
export const TREFFERLISTE_OHNE_TREFFER: Spaltenwahl = {
  ...TREFFERLISTE,
  grundmenge: [ZEITPUNKT, STATUS],
};

/**
 * Die Sichtbarkeit des Ablaufs, wörtlich: `hidden` unter der Schwelle, eine
 * Tabellenzelle ab ihr — an `th` **und** `td`, nie eine Breite von 0 px.
 * 926 px = 622 + 304, 646 px = 342 + 304 (E‑231; vorher 1.000 und 720 px mit
 * der Spalte „Kette").
 */
export const TREFFER_SICHTBAR = {
  ablaufMitTreffer: "hidden @min-[57.875rem]/trefferliste:table-cell",
  ablaufOhneTreffer: "hidden @min-[40.375rem]/trefferliste:table-cell",
} as const;
