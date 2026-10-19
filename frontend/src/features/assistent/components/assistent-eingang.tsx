"use client";

import { useId, useRef, useState } from "react";
import { Building2, MessageSquarePlus, MessageSquareText, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { useTexte } from "@/i18n/provider";
import type { Mandant } from "@/lib/mandanten";

import { ansagetext } from "../gespraech";
import { useAssistentStand, useGespraech } from "../hooks";
import { Eingabe } from "./eingabe";
import { Verlauf } from "./verlauf";

/**
 * **Der einzige Eingang zum Assistenten** (E‑1065): ein Symbol in der
 * Kopfleiste, links neben dem Nutzermenü, und die Chatbox als Overlay
 * (E‑1066, `docs/assistent-frontend.md` §4).
 *
 * **Das Symbol steht nur bei `verfuegbar: true`** (E‑1068). Solange die Abfrage
 * läuft oder scheitert, fehlt es ohne Hinweis und ohne Platzhalter — lieber ein
 * Bedienelement, das einen Moment später erscheint, als eines, das wieder
 * verschwindet (dieselbe Regel wie beim Mandantenumschalter).
 *
 * **Das Gespräch lebt hier und nicht in der Chatbox** (F3, E‑1076): Der Inhalt
 * des Overlays hängt beim Schließen aus, diese Komponente steht so lange wie
 * die Kopfzeile. Die Kopfzeile hängt sie über den Mandanten als `key` ein —
 * ein Wechsel setzt die Chatbox zurück.
 *
 * **Den Mandanten** bekommt sie von der Kopfzeile: `auskunft.mandant`, dieselbe
 * Angabe, die dort steht (E‑1069). Keine Anfrage hier trägt ihn.
 */
export function AssistentEingang({ mandant }: { mandant: Mandant }) {
  const texte = useTexte();
  const t = texte.assistent;
  const { data: stand } = useAssistentStand();
  const steuerung = useGespraech();
  const [offen, setzeOffen] = useState(false);
  const feld = useRef<HTMLTextAreaElement>(null);
  const titelId = useId();

  if (stand?.verfuegbar !== true) {
    return null;
  }

  const { zustand } = steuerung;
  // Ein frisches Gespräch hat nichts zu beenden; der Knopf versprächte sonst
  // etwas, das nicht geschieht.
  const frisch =
    zustand.gespraechId === null &&
    zustand.beitraege.length === 1 &&
    zustand.hinweis === null &&
    !zustand.wartet;

  return (
    <Popover open={offen} onOpenChange={setzeOffen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          className="min-h-bedienelement min-w-bedienelement h-auto shrink-0 px-2"
          aria-label={t.symbol}
          title={t.symbol}
          data-teil="assistent-symbol"
        >
          <MessageSquareText aria-hidden="true" />
        </Button>
      </PopoverTrigger>
      {/*
       * Rechtsbündig unter dem Symbol, 26,25 rem breit — 420 px in der Stufe `m`
       * (E‑1066) — und mit fester Höhe, damit die Fläche nicht mit jeder Nachricht
       * wächst (F10). Unter 768 px die volle Breite ohne Rundung (E‑1077); die
       * Höhe reicht bis zum unteren Rand, über die verfügbare Höhe, die Radix am
       * Element setzt.
       */}
      <PopoverContent
        align="end"
        sideOffset={6}
        collisionPadding={0}
        aria-labelledby={titelId}
        data-teil="chatbox"
        className="text-basis relative flex h-[min(40rem,var(--radix-popover-content-available-height))] w-screen max-w-none flex-col gap-0 overflow-hidden p-0 max-md:rounded-none md:w-[26.25rem]"
        onOpenAutoFocus={(ereignis) => {
          // Der Fokus gehört ins Eingabefeld, nicht auf den ersten Knopf.
          ereignis.preventDefault();
          feld.current?.focus();
        }}
      >
        <div className="border-border flex items-center gap-2 border-b py-1.5 pr-1.5 pl-3">
          <h2 id={titelId} className="font-semibold">
            {t.titel}
          </h2>
          {/* F9: Unter 768 px deckt die Chatbox die Zeile mit dem Mandanten ab. */}
          <span
            data-teil="mandant"
            className="text-muted-foreground text-beiwerk flex min-w-0 items-center gap-1"
          >
            <Building2 aria-hidden="true" className="size-3.5 shrink-0" />
            <span className="sr-only">{t.mandant}: </span>
            <span className="truncate font-mono">{mandant.id}</span>
          </span>
          <div className="ml-auto flex shrink-0 items-center gap-1">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="min-h-bedienelement aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
              aria-disabled={frisch}
              onClick={() => {
                if (!frisch) {
                  steuerung.neu();
                  feld.current?.focus();
                }
              }}
            >
              <MessageSquarePlus aria-hidden="true" data-icon="inline-start" />
              {t.neuesGespraech}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="min-h-bedienelement min-w-bedienelement"
              aria-label={t.schliessen}
              title={t.schliessen}
              onClick={() => setzeOffen(false)}
            >
              <X aria-hidden="true" />
            </Button>
          </div>
        </div>

        <Verlauf steuerung={steuerung} typen={stand.typen} mandant={mandant} />
        <Eingabe steuerung={steuerung} feldRef={feld} />

        <div aria-live="polite" data-teil="ansage" className="sr-only">
          {ansagetext(zustand, texte)}
        </div>
      </PopoverContent>
    </Popover>
  );
}
