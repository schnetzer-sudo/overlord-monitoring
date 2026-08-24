"use client";

import { useTexte } from "@/i18n/provider";

import type { Katalogzeile } from "../api";
import { KatalogZeile } from "./katalog-zeile";

/**
 * Die Pflegeliste als Tabelle — **bis zu 733 Zeilen auf einmal** (E8, keine
 * Paginierung).
 *
 * ## Der Rahmen steht, nur der Inhalt scrollt
 *
 * `docs/frontend-grundlagen.md` §7 gilt hier zum ersten Mal für eine sehr lange
 * Liste, und die Umsetzung besteht aus dem, was **nicht** dasteht:
 *
 * 1. **Kein zweites `overflow-y-auto`.** Der einzige Scrollbereich bleibt das
 *    `main` des Anwendungsrahmens. Die Kopfzeile bleibt trotzdem stehen, weil
 *    `position: sticky` keinen eigenen Scrollcontainer braucht — sie hängt sich
 *    an den nächsten, und das ist genau jenes `main`.
 * 2. **Kein `overflow-x-auto` darüber.** Das ist die Stelle, an der es sonst
 *    stillschweigend kippt: `overflow-x: auto` stuft `overflow-y` auf `auto`
 *    hoch, der Container wird selbst zum Scrollbereich, und die klebende
 *    Kopfzeile klebt an ihm statt am Fenster — sie bewegt sich dann nie. Deshalb
 *    steht hier **kein** `<Table>` aus `components/ui`: Der Baustein bringt
 *    diesen Container mit. Seine Zellen-Gestalt ist stattdessen hier
 *    nachgebildet, und breite Inhalte brechen um, statt waagerecht zu scrollen.
 * 3. **Keine Höhe am Fenster.** Kein `h-dvh`, kein `min-h-screen`, kein `h-full`.
 *
 * `<caption>` ist `sr-only` und damit `position: absolute` — genau die Klasse
 * Element, die §7 seine dritte Bedingung gekostet hat. Sie ist hier unschädlich,
 * weil `main` `relative` ist und sie deshalb an ihm hängt und nicht am
 * Ursprungsblock der Seite.
 *
 * ## Die Reihenfolge kommt vom Backend
 *
 * `ProjectID`, dann `ProcessID` (E6) — beides Schlüssel und damit stabil. **Hier
 * wird nicht umsortiert.** Eine Spaltensortierung wäre eine neue Entscheidung
 * und keine Ausbaustufe: Ohne Paginierung und ohne eindeutigen Zweitschlüssel
 * hat eine Liste, die nach einem nicht eindeutigen Feld sortiert, keine feste
 * Reihenfolge.
 *
 * ## `border-separate` ist kein Geschmack
 *
 * Mit `border-collapse` gehören die Rahmen der Tabelle und nicht den Zellen —
 * eine klebende Kopfzeile ließe ihren Trennstrich beim Scrollen zurück. Die
 * Rahmen sitzen deshalb an den Zellen.
 */
const KOPFZELLE =
  "bg-background border-border sticky top-0 z-10 border-b px-2 py-1.5 text-left align-bottom font-medium";

export function KatalogTabelle({
  zeilen,
  zeileAktionen,
}: {
  zeilen: readonly Katalogzeile[];
  /** Was in der Spalte „Pflege" unter dem Status steht — ab Teil 4 die Bearbeitung. */
  zeileAktionen?: (zeile: Katalogzeile) => React.ReactNode;
}) {
  const texte = useTexte();

  return (
    <table className="text-basis [&_td]:border-border w-full table-fixed border-separate border-spacing-0 [&_td]:border-b">
      <caption className="sr-only">{texte.katalog.tabelle}</caption>
      <thead>
        <tr>
          <th
            scope="col"
            className={`${KOPFZELLE} w-[9rem] sm:w-[12rem] md:w-[16rem] lg:w-[20rem]`}
          >
            {texte.katalog.spalten.prozess}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden w-[14rem] lg:table-cell`}>
            {texte.katalog.spalten.projekt}
          </th>
          <th scope="col" className={KOPFZELLE}>
            {texte.katalog.spalten.partner}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden w-[8rem] md:table-cell`}>
            {texte.katalog.spalten.richtung}
          </th>
          <th scope="col" className={`${KOPFZELLE} hidden w-[11rem] md:table-cell`}>
            {texte.katalog.spalten.bestand}
          </th>
          <th scope="col" className={`${KOPFZELLE} w-[6rem] md:w-[8.5rem]`}>
            {texte.katalog.spalten.pflege}
          </th>
        </tr>
      </thead>
      <tbody>
        {zeilen.map((zeile) => (
          <tr key={zeile.processId} className="hover:bg-muted/50">
            <KatalogZeile zeile={zeile} aktionen={zeileAktionen?.(zeile)} />
          </tr>
        ))}
      </tbody>
    </table>
  );
}
