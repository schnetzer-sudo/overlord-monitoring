import { Leer } from "./zustand";

/**
 * Eine Seite, die es noch nicht gibt.
 *
 * Auch sie behandelt ihre Zustände sichtbar: Sie ist **leer**, und Leer sieht
 * nicht aus wie ein Fehler. Und sie sagt, woran es liegt — hier: dass der Inhalt
 * noch nicht existiert.
 */
export function SeitenPlatzhalter({
  titel,
  platzhalterTitel,
  hinweis,
}: {
  titel: string;
  platzhalterTitel: string;
  hinweis: string;
}) {
  return (
    <div className="space-y-4">
      <h1 className="text-ueberschrift font-semibold">{titel}</h1>
      <Leer titel={platzhalterTitel} hinweis={hinweis} />
    </div>
  );
}
