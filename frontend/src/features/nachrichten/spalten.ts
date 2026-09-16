import type { Spalte, Spaltenwahl } from "@/lib/spaltenwahl";

/**
 * **Die Spaltenwahl der Nachrichtenliste** (`docs/spaltenwahl.md` §5.4, E‑148) —
 * die gemessenen Mindestbreiten in ganzen Pixeln (M177) und die Stufe, in der
 * das Projekt dazukommt.
 *
 * ## Die Entscheidung dahinter (E‑148, 16.09.2026)
 *
 * **Das Projekt kommt erst, wenn alle vier Spalten ihre Mindestbreite tragen.**
 * Bis dahin stehen die drei, mit denen sich ein Beleg wiederfinden lässt —
 * Zeitpunkt, Status, Ablauf. Vorher hing das Projekt an `md`: Bei 768 px
 * Fensterbreite kam es mit 288 px zurück, während dem Ablauf 12 px blieben, und
 * „Ablauf" stand über „Projekt" gedruckt (Punkt 114, M176 §4.3).
 *
 * **Der Ablauf weicht als letzte** — er ist die Spalte, an der man eine Zeile
 * erkennt (Auftrag Teil 2, 3.4). Er ist deshalb die freie Spalte: Er bekommt,
 * was übrig bleibt, und an der Schwelle genau seine Mindestbreite.
 *
 * **Unter der Grundmenge gilt die heutige Bauform** (Antwort des Auftraggebers
 * vom 15.09.2026): Zeitpunkt und Status behalten ihre Breite, der Ablauf kürzt
 * weiter — mit vollem `title`, wie bei jeder Breite. Das ist der Fall der
 * schmalen Spalte neben dem Baum (294 px) und des Fensters unter 430 px.
 *
 * Die Breiten stehen als wörtliche `rem`-Klassen an den Kopfzellen
 * (`components/nachrichten-tabelle.tsx`), die Sichtbarkeit steht hier — beides
 * wörtlich, weil Tailwind nur findet, was im Quelltext steht.
 * `tests/spaltenwahl.test.tsx` hält die Klassen gegen diese Zahlen, auf den Pixel.
 */

/** Die englische Form `12/30/2025, 04:02:04 AM`, breiteste Ziffer; deutsch 154 px. */
const ZEITPUNKT: Spalte = { schluessel: "zeitpunkt", mindestbreite: 187 };
/** Die Plakette allein; der Zusatz „Schritt: …" daneben kürzt (`nachrichtenliste.md` §8.1). */
const STATUS: Spalte = { schluessel: "status", mindestbreite: 155 };
/** Der längste Ablaufname beider Mandanten (300 Zeilen); Median 259,58 px. */
const ABLAUF: Spalte = { schluessel: "ablauf", mindestbreite: 304 };
/** Der längste Projektname beider Mandanten. */
const PROJEKT: Spalte = { schluessel: "projekt", mindestbreite: 286 };

export const NACHRICHTENLISTE: Spaltenwahl = {
  container: "nachrichtenliste",
  grundmenge: [ZEITPUNKT, STATUS, ABLAUF],
  stufen: [[PROJEKT]],
  frei: "ablauf",
  rinne: 0,
};

/**
 * Die Sichtbarkeit des Projekts, wörtlich: `hidden` unter der Schwelle, eine
 * Tabellenzelle ab ihr — an `th` **und** `td`, nie eine Breite von 0 px.
 * 932 px = 187 + 155 + 304 + 286.
 */
export const NACHRICHTEN_SICHTBAR = {
  projekt: "hidden @min-[58.25rem]/nachrichtenliste:table-cell",
} as const;
