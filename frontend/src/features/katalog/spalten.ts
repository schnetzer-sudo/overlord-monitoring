import type { Spalte, Spaltenwahl } from "@/lib/spaltenwahl";

/**
 * **Die Spaltenwahl des Prozess-Katalogs** (`docs/spaltenwahl.md` §5.3, E‑147) —
 * die gemessenen Mindestbreiten in ganzen Pixeln (M177, 1.123 Zeilen beider
 * Mandanten in beiden Sprachen) und die Stufen, in denen Spalten dazukommen.
 *
 * Die Breiten stehen als wörtliche `rem`-Klassen an den Kopfzellen
 * (`components/katalog-tabelle.tsx`), die Sichtbarkeit steht hier — beides
 * wörtlich, weil Tailwind nur findet, was im Quelltext steht.
 * `tests/spaltenwahl.test.tsx` hält die Klassen gegen diese Zahlen, auf den Pixel.
 *
 * **Der Katalog kürzt nicht, er bricht um.** Die Mindestbreite einer Spalte ist
 * deshalb ihr längstes Stück, das **ohne Notumbruch** nicht kleiner wird — ein
 * Wort, nie ein Buchstabe. Eine Kennung mit `break-all` darf überall brechen;
 * das ist ihre Bauform und zählt nicht.
 */
const PROZESS: Spalte = { schluessel: "prozess", mindestbreite: 317 };
const PROJEKT: Spalte = { schluessel: "projekt", mindestbreite: 163 };
const PARTNER: Spalte = { schluessel: "partner", mindestbreite: 145 };
const RICHTUNG: Spalte = { schluessel: "richtung", mindestbreite: 95 };
const BESTAND: Spalte = { schluessel: "bestand", mindestbreite: 106 };
const PFLEGE: Spalte = { schluessel: "pflege", mindestbreite: 76 };

export const KATALOG: Spaltenwahl = {
  container: "katalog",
  grundmenge: [PROZESS, PARTNER, PFLEGE],
  // Dieselbe Reihenfolge wie vorher an den Fensterschwellen: erst Richtung und
  // Nachrichten, dann das Projekt.
  stufen: [[RICHTUNG, BESTAND], [PROJEKT]],
  frei: "partner",
  rinne: 0,
};

/**
 * **Unterhalb der Grundmenge (538 px) die heutige dreispaltige Bauform**, in
 * Pixeln: Prozess 9 rem, Pflege 6 rem, Partner der Rest. Der Auftrag nennt sie
 * bei 360 px *„seine Bauform und kein Fehler"*; entschieden am 15.09.2026, dass
 * sie bis zur Grundmenge gilt.
 */
export const KATALOG_BAUFORM = { prozess: 144, pflege: 96 } as const;

/**
 * Die Sichtbarkeit der zuschaltbaren Spalten, wörtlich — an `th` **und** `td`.
 * 739 px = 538 + 95 + 106, 902 px = 739 + 163.
 */
export const KATALOG_SICHTBAR = {
  richtung: "hidden @min-[46.1875rem]/katalog:table-cell",
  bestand: "hidden @min-[46.1875rem]/katalog:table-cell",
  projekt: "hidden @min-[56.375rem]/katalog:table-cell",
} as const;
