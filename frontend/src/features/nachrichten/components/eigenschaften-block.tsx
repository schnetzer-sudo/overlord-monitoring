"use client";

import { useId, useState } from "react";
import { ChevronRight } from "lucide-react";

import { Fehler, Laden } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Eigenschaft, Schritt } from "../api";
import { gruppiereEigenschaften, schrittHinweis, type EigenschaftenGruppe } from "../detail";
import { useEigenschaften } from "../hooks";

/**
 * Die technischen Eigenschaften — **eingeklappt, erst beim Aufklappen geladen
 * und nach ausgeführtem Schritt gruppiert.**
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
 *
 * ## Die Gruppierung (17.08.2026)
 *
 * Flach untereinander stand `Converter.Log.GUID` zweimal und `Service.Type`
 * dreimal und sah aus wie eine Dublette — es sind Einträge **verschiedener
 * Prozessschritte** (M17 3). Gruppiert wird über `position`, beschriftet mit dem
 * Schrittnamen aus der Zeitleiste; **gerechnet wird das in `../detail.ts`**, weil
 * es eine Entscheidung ist und Entscheidungen geprüft werden, Markup nicht.
 *
 * **Eine Ebene, alle Gruppen offen, Überschriften dazwischen.** Keine klappbaren
 * Untergruppen: Gemessen sind 22,6 Eigenschaften je Nachricht, Minimum 14,
 * Maximum 38 (M17 1) — das wäre Mechanik für zwanzig Zeilen.
 *
 * @param schritte die Schrittfolge aus dem Detail, allein zum Beschriften.
 *   Fehlen sie, tragen alle Gruppen den Rückfall; die Einteilung selbst hängt
 *   nicht an ihnen.
 */
export function EigenschaftenBlock({
  messageId,
  anzahl,
  schritte = [],
}: {
  messageId: string;
  anzahl: number;
  schritte?: Schritt[];
}) {
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
            <div className="flex flex-col gap-3">
              {gruppiereEigenschaften(anfrage.data ?? [], schritte).map((gruppe) => (
                // Die Position ist der Schlüssel: Sie kommt je Nachricht genau
                // einmal vor, und die Einteilung gruppiert genau darüber.
                <Gruppe key={gruppe.position} gruppe={gruppe} />
              ))}
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
}

/**
 * Eine Gruppe: die Überschrift des Schritts und darunter seine Einträge.
 *
 * ## Der Kopf sagt, in welchem Schritt der Wert entstanden ist
 *
 * `{Beschriftung} ({Anzahl})` — bei der Nachricht selbst die übersetzte
 * Beschriftung, bei einer Position ohne gelieferten Schritt der Rückfall
 * *Schritt N*. **Kein erfundener Name.**
 *
 * **Der Tooltip ist derselbe wie in der Zeitleiste** und wird nicht nachgebaut:
 * Er kommt aus `../detail.ts`, damit ein Gruppenkopf wortgleich dasselbe sagt wie
 * die Zeile darüber. Bei der Nachricht selbst gibt es keinen Schritt und deshalb
 * auch keinen Tooltip.
 *
 * ## Sie ist eine Beschriftung und keine Statusaussage
 *
 * **Keine eigene Farbe, keine Animation, kein Übergang**
 * (`visuelles-konzept.md` §7): der vorhandene gedämpfte Ton, wie bei der
 * Beschriftung im BAM-Block. Und **kein eigener Scrollbereich** — es bleibt beim
 * einen senkrechten Scroller (`frontend-grundlagen.md` §7).
 *
 * **Umbruch statt Kürzung.** Die gemessene Namenslänge geht bis 61 Zeichen; die
 * Anzahl steht am Ende derselben Zeichenkette und darf nicht als Erstes
 * wegfallen. Ein Schrittname, der im Panel nicht in eine Zeile passt, bricht
 * deshalb um — dieselbe Entscheidung wie bei den Beschriftungen des BAM-Blocks.
 */
function Gruppe({ gruppe }: { gruppe: EigenschaftenGruppe }) {
  const texte = useTexte();
  const sprache = useSprache();
  const titelId = useId();

  const bausteine = texte.nachrichten.detail.eigenschaften;
  const name = gruppe.istNachricht
    ? bausteine.gruppeNachricht
    : (gruppe.beschriftung ?? einsetzen(bausteine.gruppeSchritt, { nummer: gruppe.position }));

  return (
    <section aria-labelledby={titelId} className="flex flex-col gap-1">
      <h3
        id={titelId}
        className="text-muted-foreground text-beiwerk font-medium break-words"
        // `beschriftung` ist der Name des Schritts — dieselbe Zeichenkette, die
        // die Zeitleiste als Zeile führt, und deshalb derselbe Tooltip.
        title={schrittHinweis(
          {
            name: gruppe.beschriftung,
            rohwert: gruppe.rohwert,
            namensherkunft: gruppe.namensherkunft,
          },
          texte,
        )}
      >
        {einsetzen(bausteine.gruppe, {
          name,
          anzahl: formatiereZahl(gruppe.eintraege.length, sprache),
        })}
      </h3>
      <ul className="flex flex-col">
        {gruppe.eintraege.map((eigenschaft) => (
          <EigenschaftZeile
            // Der Primärschlüssel ist `(MessageID, MessagePropertyName,
            // MessageActionID)` — innerhalb einer Gruppe ist der Name damit
            // eindeutig. Über Gruppen hinweg ist er es **nicht** (M17 3), und
            // deshalb steht die Position davor.
            key={`${gruppe.position}:${eigenschaft.name}`}
            eigenschaft={eigenschaft}
          />
        ))}
      </ul>
    </section>
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
