import type { Spalte, Spaltenwahl } from "@/lib/spaltenwahl";

/**
 * **Die Spaltenwahl der Nachrichtenliste** (`docs/spaltenwahl.md` §5.4, E‑148;
 * `docs/nachrichtenliste.md` §8.1, E‑162) — die gemessenen Mindestbreiten in
 * ganzen Pixeln (M177, M180), die Stufe, in der das Projekt dazukommt, und der
 * Umbau an ihr.
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
 * erkennt (Auftrag Teil 2, 3.4). Unter der Schwelle ist er deshalb die freie
 * Spalte: Er bekommt, was übrig bleibt.
 *
 * **Unter der Grundmenge gilt die heutige Bauform** (Antwort des Auftraggebers
 * vom 15.09.2026): Zeitpunkt und Status behalten ihre Breite, der Ablauf kürzt
 * weiter — mit vollem `title`, wie bei jeder Breite. Das ist der Fall der
 * schmalen Spalte neben dem Baum (294 px) und des Fensters unter 430 px.
 *
 * ## Ab der Schwelle nimmt das Projekt den Rest (E‑162, 16.09.2026)
 *
 * Am breiten Fenster bekam der Ablauf die ganze Überbreite — bei 1920 px 780 px
 * leer hinter dem längsten Namen —, und der Schritt neben der Plakette kürzte auf
 * „Schritt…". **Mit dem Projekt kommen deshalb zugleich** eine Statusspalte, in
 * der Plakette und Schritt stehen, und ein Ablauf in der Breite seines längsten
 * Namens; das Projekt wird die freie Spalte am Zeilenende. Eine eigene Schwelle
 * für den Schritt gibt es nicht — Projekt und Schritt weichen gemeinsam, der
 * Ablauf nach beiden (Antwort des Auftraggebers vom 16.09.2026).
 *
 * Die Breiten stehen als wörtliche `rem`-Klassen an den Kopfzellen
 * (`components/nachrichten-tabelle.tsx`), die Sichtbarkeit steht hier — beides
 * wörtlich, weil Tailwind nur findet, was im Quelltext steht.
 * `tests/spaltenwahl.test.tsx` hält die Klassen gegen diese Zahlen, auf den Pixel.
 */

/** Die englische Form `12/30/2025, 04:02:04 AM`, breiteste Ziffer; deutsch 154 px. */
const ZEITPUNKT: Spalte = { schluessel: "zeitpunkt", mindestbreite: 187 };
/**
 * Die Plakette allein, „Zusammengeführt" — in **allen vier Dichtestufen** (M180):
 * Bei `xs` braucht sie 9,692 rem, weil ihr 1‑px-Rahmen nicht mit der Schrift
 * skaliert; 9,6875 rem (M177, nur `m`) kürzten sie dort um 0,063 px. Der Zusatz
 * „Schritt: …" daneben kürzt (`nachrichtenliste.md` §8.1).
 */
const STATUS: Spalte = { schluessel: "status", mindestbreite: 156 };
/**
 * Der breiteste der 1.403 verschiedenen Ablaufnamen der Testkopie (M180, 53
 * Zeichen, 25,473 rem); M177 hatte aus 300 Zeilen 304 px. Median 234,88 px.
 */
const ABLAUF: Spalte = { schluessel: "ablauf", mindestbreite: 408 };
/** Der längste Projektname beider Mandanten. */
const PROJEKT: Spalte = { schluessel: "projekt", mindestbreite: 286 };

export const NACHRICHTENLISTE: Spaltenwahl = {
  container: "nachrichtenliste",
  grundmenge: [ZEITPUNKT, STATUS, ABLAUF],
  stufen: [[PROJEKT]],
  frei: "ablauf",
  umbau: [
    {
      stufe: 0,
      // Status: „Wartend" samt „Schritt: Send Message to Pool", 17,815 rem bei
      // `xs` (M180). Ablauf: seine Mindestbreite, jetzt als feste Breite.
      breiten: { status: 286, ablauf: 408 },
      frei: "projekt",
    },
  ],
  rinne: 0,
};

/**
 * Die Sichtbarkeit des Projekts, wörtlich: `hidden` unter der Schwelle, eine
 * Tabellenzelle ab ihr — an `th` **und** `td`, nie eine Breite von 0 px.
 * 1.167 px = 187 + 286 + 408 + 286.
 */
export const NACHRICHTEN_SICHTBAR = {
  projekt: "hidden @min-[72.9375rem]/nachrichtenliste:table-cell",
} as const;
