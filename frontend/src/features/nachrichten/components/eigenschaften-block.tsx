"use client";

import { useId, useState } from "react";
import { ChevronRight } from "lucide-react";

import { Fehler, Laden } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Eigenschaft } from "../api";
import { useEigenschaften } from "../hooks";

/**
 * Die technischen Eigenschaften — **eingeklappt und erst beim Aufklappen
 * geladen.**
 *
 * Beschriftet wird der Block mit der Anzahl **aus dem Kopf**, also ohne ihn zu
 * laden. Genau dafür trägt der Detail-Endpunkt `eigenschaftenAnzahl`; lüde die
 * Oberfläche zum Beschriften, hätte der zweite Endpunkt keinen Zweck.
 *
 * **Der Ladezustand liegt im Block, nicht im ganzen Panel.** Wer die
 * Eigenschaften aufklappt, will die Zeitleiste nicht verlieren.
 *
 * **Der Zustand gehört nicht in die URL.** Er ist keine Ansicht, die jemand
 * teilt — und beim Blättern zwischen Nachrichten beginnt der Block wieder
 * eingeklappt (der Aufrufer setzt dafür `key={messageId}`).
 */
export function EigenschaftenBlock({ messageId, anzahl }: { messageId: string; anzahl: number }) {
  const texte = useTexte();
  const bereichId = useId();
  const [offen, setOffen] = useState(false);
  const anfrage = useEigenschaften(messageId, offen);

  const beschriftung = einsetzen(texte.nachrichten.detail.eigenschaften.titel, { anzahl });

  // Ohne Eigenschaften gibt es nichts aufzuklappen. Ein Schalter, der einen
  // leeren Bereich öffnet, ist schlimmer als keiner.
  if (anzahl === 0) {
    return (
      <p className="text-muted-foreground text-beiwerk">
        {texte.nachrichten.detail.eigenschaften.keine}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={() => setOffen((bisher) => !bisher)}
        aria-expanded={offen}
        aria-controls={bereichId}
        title={
          offen
            ? texte.nachrichten.detail.eigenschaften.zuklappen
            : texte.nachrichten.detail.eigenschaften.aufklappen
        }
        className="hover:bg-muted focus-visible:ring-ring min-h-bedienelement text-beiwerk -mx-1 flex w-fit max-w-full items-center gap-1.5 rounded-md px-1 text-left font-medium focus-visible:ring-2 focus-visible:outline-none"
      >
        {/* Eine Drehung, keine Bewegung: Das visuelle Konzept lässt außer
            Schublade und Menü keine Animation zu. */}
        <ChevronRight
          aria-hidden="true"
          className={cn("size-3.5 shrink-0 opacity-70", offen && "rotate-90")}
        />
        {beschriftung}
      </button>

      {offen ? (
        <div id={bereichId}>
          {anfrage.isPending ? (
            <Laden zeilen={3} />
          ) : anfrage.error ? (
            <Fehler fehler={anfrage.error} aufWiederholen={() => void anfrage.refetch()} />
          ) : (anfrage.data?.length ?? 0) === 0 ? (
            <p className="text-muted-foreground text-beiwerk">
              {texte.nachrichten.detail.eigenschaften.leer}
            </p>
          ) : (
            <ul className="flex flex-col">
              {anfrage.data?.map((eigenschaft, nummer) => (
                <EigenschaftZeile
                  // Der Primärschlüssel erlaubt denselben Namen auf mehreren
                  // Schritten (M17 3) — Name und Position zusammen sind der
                  // Schlüssel, die laufende Nummer die Rückfallebene.
                  key={`${eigenschaft.name}-${eigenschaft.position}-${nummer}`}
                  eigenschaft={eigenschaft}
                />
              ))}
            </ul>
          )}
        </div>
      ) : null}
    </div>
  );
}

/**
 * Name und Wert als **Rohwerte**, in fester Zeilenhöhe.
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
    <li className="h-zeile text-beiwerk flex items-center gap-2">
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
