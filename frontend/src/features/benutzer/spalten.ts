import type { Spalte, Spaltenwahl } from "@/lib/spaltenwahl";

/**
 * **Die Spaltenwahl der Benutzertabelle** (`docs/spaltenwahl.md` §5.2, E‑147) —
 * die gemessenen Mindestbreiten in ganzen Pixeln (M177, beide Sprachen) und die
 * Stufen, in denen Spalten dazukommen.
 *
 * Die Breiten stehen als wörtliche `rem`-Klassen an den Kopfzellen
 * (`components/benutzer-tabelle.tsx`), die Sichtbarkeit steht hier — beides
 * wörtlich, weil Tailwind nur findet, was im Quelltext steht.
 * `tests/spaltenwahl.test.tsx` hält die Klassen gegen diese Zahlen, auf den Pixel.
 *
 * **Die festen Texte zählen mit, auch wenn die Testkopie sie nicht zeigt:**
 * „deaktiviert" (Konto), „Wechsel erforderlich" (Passwort) und ein Zeitpunkt
 * hinter „bis" (Zeitsperre) standen in keiner der sechs Zeilen und tragen trotzdem
 * die Breite ihrer Spalte.
 *
 * **Welche Spalte weicht, ist unverändert** (`docs/benutzerverwaltung-frontend.md`
 * §4): Benutzer, die beiden Sperren, Konto und Bearbeiten stehen bei jeder Breite;
 * Rolle und Mandanten kommen zuerst dazu, Passwortwechsel und letzte Anmeldung
 * danach.
 *
 * **Diese Tabelle hat keine freie Spalte** (E‑149, 16.09.2026). Bis dahin war es
 * die Mandantenspalte, und sie bekam bei jeder Breite den ganzen Rest: bei
 * 1.408 px Hülle 673 px für eine Reihe kurzer Marken, während Benutzername und
 * Rolle daneben auf 81 und 89 px standen und umbrachen. **Der Inhalt dieser
 * Zelle kann den Platz nicht nutzen.** Jede Spalte trägt deshalb ihre
 * Mindestbreite, und `table-layout: fixed` teilt den Überschuss anteilig auf —
 * genau das, was unterhalb von 591 px längst geschah, weil die freie Spalte dort
 * gar nicht dastand (M177, `docs/spaltenwahl.md` §5.5).
 */
const BENUTZER: Spalte = { schluessel: "benutzer", mindestbreite: 81 };
const ROLLE: Spalte = { schluessel: "rolle", mindestbreite: 89 };
const MANDANTEN: Spalte = { schluessel: "mandanten", mindestbreite: 96 };
const SPERRE: Spalte = { schluessel: "sperre", mindestbreite: 74 };
const ZEITSPERRE: Spalte = { schluessel: "zeitsperre", mindestbreite: 106 };
const AKTIV: Spalte = { schluessel: "aktiv", mindestbreite: 97 };
const PASSWORT: Spalte = { schluessel: "passwort", mindestbreite: 95 };
const LETZTE_ANMELDUNG: Spalte = { schluessel: "letzteAnmeldung", mindestbreite: 145 };
const AKTIONEN: Spalte = { schluessel: "aktionen", mindestbreite: 48 };

export const BENUTZERTABELLE: Spaltenwahl = {
  container: "benutzertabelle",
  grundmenge: [BENUTZER, SPERRE, ZEITSPERRE, AKTIV, AKTIONEN],
  stufen: [
    [ROLLE, MANDANTEN],
    [PASSWORT, LETZTE_ANMELDUNG],
  ],
  rinne: 0,
};

/**
 * Die Sichtbarkeit der zuschaltbaren Spalten, wörtlich — an `th` **und** `td`.
 * 591 px = 406 + 89 + 96, 831 px = 591 + 95 + 145. Die Schwellen sind von E‑149
 * unberührt: An jeder ist der Überschuss null, und jede Spalte steht dort genau
 * auf ihrer Mindestbreite.
 */
export const BENUTZER_SICHTBAR = {
  rolle: "hidden @min-[36.9375rem]/benutzertabelle:table-cell",
  mandanten: "hidden @min-[36.9375rem]/benutzertabelle:table-cell",
  passwort: "hidden @min-[51.9375rem]/benutzertabelle:table-cell",
  letzteAnmeldung: "hidden @min-[51.9375rem]/benutzertabelle:table-cell",
} as const;
