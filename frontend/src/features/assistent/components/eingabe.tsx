"use client";

import { useId, type RefObject } from "react";
import { SendHorizontal } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import { kannSenden } from "../gespraech";
import type { Gespraechssteuerung } from "../hooks";

/**
 * Die Klassen des Eingabefelds aus `components/ui/input.tsx`, für ein
 * `textarea` (F1): Der Generator brächte für sein `textarea` am 19.10.2026 eine
 * neue Abhängigkeit mit (`cn`), für ein einziges Feld.
 */
const FELD =
  "border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 dark:bg-input/30 aria-disabled:bg-input/50 max-h-40 min-h-16 w-full min-w-0 resize-none rounded-lg border bg-transparent px-2.5 py-1.5 transition-colors outline-none field-sizing-content focus-visible:ring-3 aria-disabled:cursor-not-allowed";

/**
 * **Die Eingabe** (`docs/assistent-frontend.md` §4). Enter sendet,
 * Umschalt+Enter bricht um; während einer Zeichenfolge im Eingabesystem
 * (`isComposing`) sendet Enter nicht.
 *
 * **Während der Antwort ist das Feld `readOnly`, nicht `disabled`** (F11): Ein
 * gesperrtes Feld verlöre den Fokus, und der Kunde schreibt nach der Antwort
 * dort weiter, wo er war. Senden ist gesperrt — `aria-disabled`, aus demselben
 * Grund, und der Klick prüft die Sperre selbst.
 *
 * **Eine Länge prüft das Feld nicht.** Die Grenze kennt das Backend und meldet
 * sie mit `hoechstensZeichen`.
 */
export function Eingabe({
  steuerung,
  feldRef,
}: {
  steuerung: Gespraechssteuerung;
  feldRef: RefObject<HTMLTextAreaElement | null>;
}) {
  const texte = useTexte();
  const t = texte.assistent.eingabe;
  const kennung = useId();
  const { zustand } = steuerung;
  const gesperrt = zustand.wartet;
  const sendenGesperrt = !kannSenden(zustand);

  function senden() {
    // Der Fokus gehört ins Feld — auch nach einem Klick auf „Senden“.
    feldRef.current?.focus();
    steuerung.senden();
  }

  return (
    <form
      className="border-border flex flex-col gap-1 border-t px-3 py-2"
      onSubmit={(ereignis) => {
        ereignis.preventDefault();
        senden();
      }}
    >
      <Label htmlFor={kennung} className="sr-only">
        {t.beschriftung}
      </Label>
      <div className="flex items-end gap-2">
        <textarea
          ref={feldRef}
          id={kennung}
          rows={2}
          value={zustand.entwurf}
          readOnly={gesperrt}
          aria-disabled={gesperrt || undefined}
          aria-describedby={`${kennung}-tasten`}
          placeholder={t.platzhalter}
          className={FELD}
          onChange={(ereignis) => steuerung.setzeEntwurf(ereignis.target.value)}
          onKeyDown={(ereignis) => {
            if (ereignis.key !== "Enter" || ereignis.shiftKey || ereignis.nativeEvent.isComposing) {
              return;
            }
            ereignis.preventDefault();
            senden();
          }}
        />
        <Button
          type="submit"
          className={cn(
            "min-h-bedienelement shrink-0",
            "aria-disabled:cursor-not-allowed aria-disabled:opacity-50",
          )}
          aria-disabled={sendenGesperrt}
        >
          <SendHorizontal aria-hidden="true" data-icon="inline-start" />
          {t.senden}
        </Button>
      </div>
      <p id={`${kennung}-tasten`} className="text-muted-foreground text-beiwerk">
        {t.tasten}
      </p>
    </form>
  );
}
