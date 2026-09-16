"use client";

import { Fragment } from "react";
import { SquarePen } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import type { Katalogzeile } from "../api";
import { KATALOG_SICHTBAR } from "../spalten";
import { darfOeffnen } from "../zuordnung";
import { KatalogZeile } from "./katalog-zeile";
import { ZeilenFormular } from "./zeilen-formular";

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
 * ## Die Spaltenmenge folgt dem Container, nicht dem Fenster (E‑147)
 *
 * Prozess, Partner und Pflege stehen immer; Richtung und Nachrichten kommen ab
 * 739 px **Breite dieser Hülle** dazu, das Projekt ab 902 px. Jede Schwelle ist
 * die Summe der gemessenen Mindestbreiten der Spalten, die ab dort sichtbar sind
 * (M177, `../spalten.ts`); jede feste Spalte trägt ab 538 px genau ihre
 * Mindestbreite, der Partner den Rest. Vorher hingen Sichtbarkeit und Breite an
 * `md` und `lg`, und bei 768 px fiel der Partner auf 0 px und brach
 * buchstabenweise um (Punkt 175).
 *
 * **Unter 538 px gilt die dreispaltige Bauform**, die der Katalog bei 360 px
 * schon hatte: Prozess 9 rem, Pflege 6 rem, Partner der Rest — umbrechend, nie
 * 0 px. Herleitung und Zahlen: `docs/spaltenwahl.md`.
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
 *
 * ## Die offene Zeile bekommt eine zweite `<tr>`
 *
 * Nicht ihre Zellen werden zu Eingabefeldern, sondern **unter** ihr klappt ein
 * Formular über die volle Breite auf. Die Begründung steht bei
 * {@link ZeilenFormular}; hier zählt die Folge für die Tabelle: Der bisherige
 * Stand der Zeile bleibt beim Tippen sichtbar, und die drei zuschaltbaren
 * Spalten nehmen der Bearbeitung nichts weg, wenn sie fehlen.
 */
/**
 * Die Kopfzelle — und die eine Zahl darin ist gemessen und nicht gewählt.
 *
 * **`-top-4` und nicht `top-0`.** Der Scrollbereich ist das `main` des
 * Anwendungsrahmens, und das trägt `py-4`. Eine klebende Zelle mit `top-0`
 * bleibt deshalb **einen Innenabstand zu tief** stehen — nachgemessen am
 * 24.08.2026 bei 1920 × 889: `main` beginnt bei y = 51, die Kopfzeile blieb bei
 * y = **67** stehen. Durch die 16 px dazwischen liefen die Zeilen sichtbar
 * hindurch, und über der Kopfzeile stand eine halbe fremde Zeile. Mit `-top-4`
 * sind beide bei y = 51.
 *
 * **Die Zahl gehört zum Rahmen und nicht zu dieser Tabelle** — sie ist der
 * Innenabstand aus `components/anwendungsrahmen.tsx`. Wer ihn dort ändert,
 * ändert ihn hier mit. Ein eigenes Dichtemaß dafür entsteht nicht: Es gäbe
 * seinen einzigen Verwender hier, und der Rahmen benutzte es nicht.
 */
const KOPFZELLE =
  "bg-background border-border sticky -top-4 z-10 border-b px-2 py-1.5 text-left align-bottom font-medium";

export function KatalogTabelle({
  zeilen,
  offeneZeile,
  aufOeffnen,
  aufSchliessen,
  vorschlaege,
}: {
  zeilen: readonly Katalogzeile[];
  offeneZeile: string | null;
  aufOeffnen: (processId: string) => void;
  aufSchliessen: () => void;
  vorschlaege: readonly string[];
}) {
  const texte = useTexte();

  return (
    // Der Container ist die eigene Hülle, benannt — nicht `main`, das selbst ein
    // Größencontainer ist (`lib/klebende-spalte.ts`). Die klebende Kopfzeile
    // klebt weiterhin an `main`: `container-type` macht die Hülle nicht zum
    // Scrollbereich.
    <div className="@container/katalog">
      <table className="text-basis [&_td]:border-border w-full table-fixed border-separate border-spacing-0 [&_td]:border-b">
        <caption className="sr-only">{texte.katalog.tabelle}</caption>
        <thead>
          <tr>
            {/* Unter der Grundmenge (538 px) die Bauform von 360 px, ab ihr die
              Mindestbreite: 317 px, der längste Prozessname ohne Notumbruch. */}
            <th
              scope="col"
              className={`${KOPFZELLE} w-[9rem] @min-[33.625rem]/katalog:w-[19.8125rem]`}
            >
              {texte.katalog.spalten.prozess}
            </th>
            <th scope="col" className={`${KOPFZELLE} ${KATALOG_SICHTBAR.projekt} w-[10.1875rem]`}>
              {texte.katalog.spalten.projekt}
            </th>
            <th scope="col" className={KOPFZELLE}>
              {texte.katalog.spalten.partner}
            </th>
            <th scope="col" className={`${KOPFZELLE} ${KATALOG_SICHTBAR.richtung} w-[5.9375rem]`}>
              {texte.katalog.spalten.richtung}
            </th>
            <th scope="col" className={`${KOPFZELLE} ${KATALOG_SICHTBAR.bestand} w-[6.625rem]`}>
              {texte.katalog.spalten.bestand}
            </th>
            <th
              scope="col"
              className={`${KOPFZELLE} w-[6rem] @min-[33.625rem]/katalog:w-[4.75rem]`}
            >
              {texte.katalog.spalten.pflege}
            </th>
          </tr>
        </thead>
        <tbody>
          {zeilen.map((zeile) => {
            const offen = offeneZeile === zeile.processId;
            return (
              <Fragment key={zeile.processId}>
                <tr className={cn(offen ? "bg-muted/50" : "hover:bg-muted/50")}>
                  <KatalogZeile
                    zeile={zeile}
                    aktionen={
                      offen ? null : (
                        <Button
                          type="button"
                          variant="outline"
                          size="icon"
                          disabled={!darfOeffnen(offeneZeile, zeile.processId)}
                          onClick={() => aufOeffnen(zeile.processId)}
                          title={texte.katalog.bearbeiten.oeffnen}
                          className="min-h-bedienelement"
                        >
                          <SquarePen aria-hidden="true" />
                          <span className="sr-only">{texte.katalog.bearbeiten.oeffnen}</span>
                        </Button>
                      )
                    }
                  />
                </tr>
                {offen ? (
                  <tr className="bg-muted/50">
                    <td colSpan={6}>
                      <ZeilenFormular
                        zeile={zeile}
                        aufSchliessen={aufSchliessen}
                        vorschlaege={vorschlaege}
                      />
                    </td>
                  </tr>
                ) : null}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
