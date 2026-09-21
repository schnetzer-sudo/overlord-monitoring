"use client";

import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Eigenschaft } from "../api";

/**
 * **Die eine Zeilendarstellung einer technischen Eigenschaft** — herausgelöst
 * aus `eigenschaften-block.tsx` am 21.09.2026 (`docs/nachrichtendetail.md`
 * §10.16), weil sie seither an vier Stellen steht: im Block *Technische
 * Eigenschaften*, unter dem *Eingang*, unter jeder Schrittzeile und unter *Ohne
 * Schritt in der Zeitleiste*. **Benutzt, nicht nachgebaut.**
 *
 * **Der React-Schlüssel ist `${position}:${name}`.** Der Primärschlüssel ist
 * `(MessageID, MessagePropertyName, MessageActionID)`; innerhalb einer Position
 * ist der Name damit eindeutig, über Positionen hinweg **nicht** (M17 3). Er
 * steht hier und nicht beim Aufrufer, damit ihn keine der vier Stellen vergisst.
 */
export function EigenschaftenListe({
  eintraege,
  className,
}: {
  eintraege: readonly Eigenschaft[];
  className?: string;
}) {
  return (
    <ul className={cn("flex min-w-0 flex-col", className)}>
      {eintraege.map((eigenschaft) => (
        <EigenschaftZeile
          key={`${eigenschaft.position}:${eigenschaft.name}`}
          eigenschaft={eigenschaft}
        />
      ))}
    </ul>
  );
}

/**
 * Name und Wert als **Rohwerte**, in fester Laufweite und fester Zeilenhöhe,
 * gekürzt mit dem Vollwert im `title`.
 *
 * **Ein gekappter Wert wird als gekappt gekennzeichnet**, mit seiner
 * ursprünglichen Länge — das Backend liefert beides. Ein stillschweigend
 * abgeschnittener Wert ist schlimmer als ein sichtbar abgeschnittener: Ohne das
 * Kennzeichen läse jemand eine halbe Belegnummer als ganze.
 */
function EigenschaftZeile({ eigenschaft }: { eigenschaft: Eigenschaft }) {
  const texte = useTexte();
  const sprache = useSprache();

  const gekapptHinweis =
    eigenschaft.gekappt && eigenschaft.originalLaengeBytes !== null
      ? einsetzen(texte.nachrichten.detail.eigenschaften.gekapptHinweis, {
          bytes: formatiereZahl(eigenschaft.originalLaengeBytes, sprache),
        })
      : undefined;

  return (
    <li className="h-zeile text-beiwerk flex min-w-0 items-center gap-2">
      <span
        className="text-muted-foreground w-2/5 shrink-0 truncate font-mono"
        title={eigenschaft.name}
      >
        {eigenschaft.name}
      </span>
      <span className="min-w-0 flex-1 truncate font-mono" title={eigenschaft.wert}>
        {eigenschaft.wert}
      </span>
      {eigenschaft.gekappt ? (
        <span
          className="border-border text-muted-foreground shrink-0 rounded-sm border px-1"
          title={gekapptHinweis}
        >
          {texte.nachrichten.detail.eigenschaften.gekappt}
          {gekapptHinweis === undefined ? null : (
            <span className="sr-only"> — {gekapptHinweis}</span>
          )}
        </span>
      ) : null}
    </li>
  );
}
