"use client";

import { useRef } from "react";

import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Eigenschaft } from "../api";
import { tippenKopiert } from "../kopieren";
import { KopierKnopf, useKopieren } from "./kopier-knopf";

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
  const bausteine = texte.nachrichten.detail.eigenschaften;
  const { kopiert, kopiere } = useKopieren(eigenschaft.wert);
  // Die Zeigerart des `pointerup` derselben Geste — die zweite Quelle für
  // Safari, wo der Klick sie nicht oder falsch trägt (`kopieren.ts`).
  const zeigerart = useRef<string | null>(null);

  const gekapptHinweis =
    eigenschaft.gekappt && eigenschaft.originalLaengeBytes !== null
      ? einsetzen(bausteine.gekapptHinweis, {
          bytes: formatiereZahl(eigenschaft.originalLaengeBytes, sprache),
        })
      : undefined;

  return (
    <li className="group/eigenschaft h-zeile text-beiwerk flex min-w-0 items-center gap-2">
      {/* Die Namensspalte ist 40 % der Zeile. Neben der Liste wächst sie damit
          mit der gezogenen Panelbreite, bis 16 rem (E‑1210): Den Deckel setzt
          allein die Hülle dort (`--eigenschaft-name-max`, `lib/panelbreite.ts`).
          Ohne die Variable gilt kein Deckel — eigene Route und Vollansicht
          bleiben bei ihren 40 %. */}
      <span
        className="text-muted-foreground w-2/5 max-w-(--eigenschaft-name-max) shrink-0 truncate font-mono"
        title={eigenschaft.name}
      >
        {eigenschaft.name}
      </span>
      {/* **Ein Tippen auf den Wert kopiert ihn, ein Mausklick nicht** (E‑1207):
          Am Touchgerät gibt es kein Überfahren und damit keinen sichtbaren
          Knopf; mit der Maus bleibt der Wert markierbar wie bisher. Entschieden
          je Geste in `tippenKopiert`, nicht über `matchMedia`. */}
      <span
        className="min-w-0 flex-1 truncate font-mono"
        title={eigenschaft.wert}
        onPointerUp={(ereignis) => {
          zeigerart.current = ereignis.pointerType;
        }}
        onClick={(ereignis) => {
          const amKlick = (ereignis.nativeEvent as Partial<PointerEvent>).pointerType;
          const amLoslassen = zeigerart.current;
          zeigerart.current = null;
          if (tippenKopiert(amKlick, amLoslassen)) kopiere();
        }}
      >
        {eigenschaft.wert}
      </span>
      {eigenschaft.gekappt ? (
        <span
          className="border-border text-muted-foreground shrink-0 rounded-sm border px-1"
          title={gekapptHinweis}
        >
          {bausteine.gekappt}
          {gekapptHinweis === undefined ? null : (
            <span className="sr-only"> — {gekapptHinweis}</span>
          )}
        </span>
      ) : null}
      {/* **Die dritte Spalte, immer reserviert** (E‑1207): Der Knopf nimmt
          seinen Platz auch verborgen ein, damit der Wert nicht springt, wenn er
          erscheint. Verborgen über die Deckkraft und nicht über `visibility`
          oder `display` — so bleibt er in der Tab-Reihenfolge und für
          Vorleseprogramme erreichbar, auch am Touchgerät.

          Sichtbar beim Fokus in der Zeile und beim Überfahren der Zeile. Das
          Überfahren gilt nur unter `@media (hover: hover)` — das legt Tailwind 4
          um jede `hover:`-Variante, auch um `group-hover:`; ohne die Grenze
          bliebe der Knopf auf iOS nach einem Tippen hängend stehen. Solange das
          Häkchen steht, ist er sichtbar, auch nach einem Tippen auf den Wert.

          `transition-none`: kein Übergang beim Überfahren
          (`visuelles-konzept.md` §7); der `Button` brächte `transition-all` mit.
          `size-7` statt der Höhe eines Bedienelements, damit er in die feste
          Zeilenhöhe passt; `mr-1`, damit sein Fokusring nicht an der
          Schnittkante des aufklappbaren Inhalts (`overflow-hidden`) endet. */}
      <KopierKnopf
        kopiert={kopiert}
        aufKopieren={kopiere}
        beschriftung={einsetzen(bausteine.wertKopieren, { name: eigenschaft.name })}
        beschriftungKopiert={einsetzen(bausteine.wertKopiert, { name: eigenschaft.name })}
        className={EIGENSCHAFT_KOPIERKNOPF}
      />
    </li>
  );
}

/**
 * Die Klassen des Kopierknopfs einer Eigenschaftszeile — ausgeführt, damit der
 * Test die Sichtbarkeitsregel an genau diesen Klassen gegen Tailwind prüft.
 */
export const EIGENSCHAFT_KOPIERKNOPF =
  "text-muted-foreground mr-1 size-7 transition-none [&_svg:not([class*='size-'])]:size-3.5 " +
  "opacity-0 group-focus-within/eigenschaft:opacity-100 group-hover/eigenschaft:opacity-100 " +
  "data-kopiert:opacity-100";
