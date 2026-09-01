"use client";

import { useId } from "react";
import { Check } from "lucide-react";

import {
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
} from "@/components/ui/dropdown-menu";
import { dichteSetzen } from "@/dichte/aktion";
import { DICHTESTUFEN, DICHTE_FELD } from "@/dichte";
import { useDichte } from "@/dichte/provider";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

/**
 * Der Dichteumschalter — **vier Stufen, ein Formular**.
 *
 * Er steht im Nutzermenü der Kopfzeile, neben der Sprachwahl: Dort erwartet man
 * Einstellungen, die den **Nutzer** betreffen und nicht die Ansicht. Die
 * gewählte Stufe liegt im Cookie `overlord_dichte`, das Wurzel-Layout liest sie
 * serverseitig und setzt `data-dichte` an `<html>` — Begründung und Bauform in
 * `src/dichte/index.ts`.
 *
 * ## Warum ein Formular und keine Radiogruppe
 *
 * `components/ui/dropdown-menu.tsx` liefert `DropdownMenuRadioGroup` und
 * `DropdownMenuRadioItem` mit; **beide sind da, und keiner ist hier
 * verwendbar.** Sie schalten über `onValueChange` im Browser, und dieser
 * Umschalter muss über eine **Server-Aktion** schalten: Das Wurzel-Layout liest
 * die Stufe auf dem Server, also muss der neue Wert dort ankommen. Ein
 * `asChild` um eine Absende-Schaltfläche scheidet aus, weil `RadioItem` seine
 * Kinder selbst zusammensetzt (Anzeigehäkchen plus `children`).
 *
 * **Nachgeholt wird davon genau ein Attributpaar** — `role="menuitemradio"` und
 * `aria-checked` —, und das ist dieselbe Auszeichnung, die Radix an einem
 * `RadioItem` selbst erzeugt.
 *
 * > **Dass das Attribut gewinnt, hängt an Radix und nicht an uns.** `Slot`
 * > mischt mit `{ ...slotProps, ...childProps }`, das Kind steht hinten und
 * > setzt sich durch (`@radix-ui/react-slot`, nachgesehen im Paket). Kehrte
 * > sich die Reihenfolge je um, stünde hier wieder `role="menuitem"` — und
 * > **kein Test hielte das fest**, weil das Projekt gerenderte Bäume zählt.
 * > Nachgemessen ist es am laufenden System (`docs/dichte-umschalter.md` §4).
 *
 * Die **Gestalt** kommt unverändert aus dem Generatorbereich:
 * `DropdownMenuItem` bringt Abstände, Fokusfläche und die Größe des Zeichens
 * mit, `DropdownMenuGroup` die Gruppe. In `components/ui` ist dafür nichts
 * geändert und nichts nachinstalliert worden.
 *
 * ## Das Menü bleibt offen
 *
 * `onSelect` wird abgefangen. Das ist keine Bequemlichkeit, sondern der Zweck
 * des Bedienelements: Wer die Anzeigegröße sucht, will sie **sehen** und dann
 * entscheiden. Bliebe das Menü nicht stehen, müsste er es für jeden Vergleich
 * viermal neu öffnen — und die Wirkung tritt hinter dem Menü ein, nicht darin.
 *
 * ## Die Beschriftung sagt „Größe" und der Code sagt „Dichte"
 *
 * Absicht. Im Code heißen die Stufen `xs`, `s`, `m`, `l`, und das Ding heißt
 * Dichte, weil es das ist: `--dichte-zeile` und die Abstände gehen mit. Der
 * Nutzer ist kein EDI-Spezialist und erst recht kein Gestalter; er liest
 * „Anzeigegröße". Die Übersetzung ist die Stelle, an der solche Namen
 * auseinandergehen dürfen — die Texte stehen in `i18n/de.ts` und `en.ts`,
 * **keine Zeichenkette in dieser Datei**.
 */
export function DichteUmschaltung() {
  const aktiv = useDichte();
  const texte = useTexte();
  const titelId = useId();

  return (
    // `role="none"` nimmt das Formular aus dem Baum der Menürollen. Ohne das
    // stünde zwischen `role="menu"` und `role="group"` ein Element, das dort
    // nicht vorgesehen ist.
    <form action={dichteSetzen} role="none">
      <DropdownMenuLabel id={titelId} className="text-muted-foreground text-beiwerk font-normal">
        {texte.dichte.bezeichnung}
      </DropdownMenuLabel>
      <DropdownMenuGroup aria-labelledby={titelId}>
        {DICHTESTUFEN.map((stufe) => (
          <DropdownMenuItem
            key={stufe}
            asChild
            // Am Berührungsgerät fällt `--dichte-bedienelement` auf
            // `--dichte-beruehrung` zurück, und das ist in jeder Stufe
            // mindestens 44 px (`globals.css`). Dieselbe Zusicherung wie an
            // jedem anderen Bedienelement der Kopfzeile.
            className="min-h-bedienelement"
            onSelect={(ereignis) => ereignis.preventDefault()}
          >
            <button
              type="submit"
              name={DICHTE_FELD}
              value={stufe}
              role="menuitemradio"
              aria-checked={stufe === aktiv}
              className="w-full text-left"
            >
              {/* Das Häkchen steht auch bei den inaktiven Stufen im Fluss —
                  nur unsichtbar. Sonst rückten die Beschriftungen beim
                  Umschalten um seine Breite hin und her, und das ist die
                  Bewegung, die §1 des visuellen Konzepts ausschließt. */}
              <Check aria-hidden="true" className={cn(stufe !== aktiv && "invisible")} />
              {texte.dichte.stufen[stufe]}
            </button>
          </DropdownMenuItem>
        ))}
      </DropdownMenuGroup>
    </form>
  );
}
