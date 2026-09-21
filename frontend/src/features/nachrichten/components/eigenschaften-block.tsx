"use client";

import { useState } from "react";
import { ChevronRight } from "lucide-react";

import { Fehler, Laden } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Eigenschaft } from "../api";
import { AUFKLAPP_UEBERGANG, AufklappInhalt, AufklappSchalter, Aufklappen } from "./aufklappen";
import { EigenschaftenListe } from "./eigenschaft-zeile";

/**
 * Die technischen Eigenschaften — **seit dem 21.09.2026 nur noch die allgemeinen
 * Angaben zur Nachricht** (E‑224, `docs/nachrichtendetail.md` §10.16).
 *
 * ## Was hier steht, und was nicht mehr
 *
 * `position === 0` **und** Name beginnt mit `Message.` — der Teil `allgemein`
 * aus `../detail.ts` `verteileEigenschaften`, **flach und ohne Gruppenköpfe**.
 * Alles Übrige steht unter seinem Schritt in der Zeitleiste, was auf Schritt `0`
 * liegt und nicht `Message.*` heißt, unter dem *Eingang*. Die Gruppierung vom
 * 17.08.2026 ist damit abgelöst, und mit ihr der Sprung aus der Zeitleiste vom
 * 18.08.2026 samt Sprungziel, Fokus-Effekt und Gruppenkennungen (E‑225).
 *
 * ## Die Bauform bleibt
 *
 * Eingeklappt, mit Pfeil, der Ladezustand im Block, `key={messageId}` am
 * Aufrufer, dieselbe Beschriftung. **Der Zustand gehört nicht in die URL.**
 *
 * ## Die Zahl ist die Zeilenzahl von `allgemein`
 *
 * Sie erscheint **mit den Daten**; vorher steht die Überschrift ohne Zahl.
 * **Keine erfundene Null.** `eigenschaftenAnzahl` aus dem Kopf zählt alle
 * Eigenschaften und wäre hier die falsche Zahl.
 *
 * ## Vier Lagen ohne Schalter oder ohne Inhalt
 *
 * | Lage | Was zu sehen ist |
 * |---|---|
 * | `eigenschaftenAnzahl === 0` | wie bisher eine Zeile Text statt eines Schalters |
 * | die Abfrage scheitert | der gewöhnliche Baustein aus `components/zustand.tsx` **an Stelle des Schalters**, sichtbar ohne Aufklappen |
 * | `allgemein` leer trotz Eigenschaften | ein eigener Satz, der **nicht** behauptet, es gäbe keine |
 * | die Antwort steht noch aus | der Schalter ohne Zahl; aufgeklappt der Ladezustand im Block |
 *
 * @param anzahl `eigenschaftenAnzahl` aus dem Kopf — entscheidet allein, ob es
 *   überhaupt etwas zu holen gab.
 * @param allgemein der Teil für diesen Block; `undefined`, solange die Antwort
 *   aussteht.
 * @param fehler der Fehler der Abfrage. Sie gehört dem Aufrufer, weil die
 *   Zeitleiste dieselbe Antwort braucht.
 */
export function EigenschaftenBlock({
  anzahl,
  allgemein,
  fehler = null,
  aufWiederholen,
}: {
  anzahl: number;
  allgemein: readonly Eigenschaft[] | undefined;
  fehler?: unknown;
  aufWiederholen?: () => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const bausteine = texte.nachrichten.detail.eigenschaften;
  const [offen, setOffen] = useState(false);

  // Ohne Eigenschaften gibt es nichts aufzuklappen. Ein Schalter, der einen
  // leeren Bereich öffnet, ist schlimmer als keiner.
  if (anzahl === 0) {
    return <p className="text-muted-foreground text-beiwerk">{bausteine.keine}</p>;
  }

  // Sichtbar ohne Aufklappen: Hinter einem Schalter verborgen sähe eine
  // gescheiterte Abfrage aus wie eine Nachricht, an deren Zeitleiste schlicht
  // nichts aufzuklappen ist.
  if (fehler !== null && fehler !== undefined) {
    return <Fehler fehler={fehler} aufWiederholen={aufWiederholen} />;
  }

  // Die Nachricht hat Eigenschaften, nur keine allgemeinen. Der Satz sagt, wo
  // die übrigen stehen — „keine Eigenschaften" wäre hier falsch.
  if (allgemein !== undefined && allgemein.length === 0) {
    return <p className="text-muted-foreground text-beiwerk">{bausteine.keineAllgemeinen}</p>;
  }

  const beschriftung =
    allgemein === undefined
      ? bausteine.titelOhneZahl
      : einsetzen(bausteine.titel, { anzahl: formatiereZahl(allgemein.length, sprache) });

  return (
    <Aufklappen offen={offen} aufWechsel={setOffen} className="flex flex-col">
      <AufklappSchalter
        title={offen ? bausteine.zuklappen : bausteine.aufklappen}
        className="hover:bg-muted focus-visible:ring-ring min-h-bedienelement text-beiwerk -mx-1 flex w-fit max-w-full items-center gap-1.5 rounded-md px-1 text-left font-medium focus-visible:ring-2 focus-visible:outline-none"
      >
        <ChevronRight
          aria-hidden="true"
          className={cn(
            "size-3.5 shrink-0 opacity-70 transition-[rotate]",
            AUFKLAPP_UEBERGANG,
            offen && "rotate-90",
          )}
        />
        {beschriftung}
      </AufklappSchalter>

      <AufklappInhalt className="pt-2">
        {allgemein === undefined ? (
          <Laden zeilen={3} />
        ) : (
          <EigenschaftenListe eintraege={allgemein} />
        )}
      </AufklappInhalt>
    </Aufklappen>
  );
}
