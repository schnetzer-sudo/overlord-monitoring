"use client";

import { ALargeSmall, Check } from "lucide-react";

import {
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
} from "@/components/ui/dropdown-menu";
import { dichteSetzen } from "@/dichte/aktion";
import { DICHTESTUFEN, DICHTE_FELD } from "@/dichte";
import { useDichte } from "@/dichte/provider";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

/**
 * Der Dichteumschalter — **vier Stufen, ein Formular**, seit dem 01.09.2026
 * hinter **einer** Zeile im Nutzermenü.
 *
 * Er steht im Nutzermenü der Kopfzeile, neben der Sprachwahl: Dort erwartet man
 * Einstellungen, die den **Nutzer** betreffen und nicht die Ansicht. Die
 * gewählte Stufe liegt im Cookie `overlord_dichte`, das Wurzel-Layout liest sie
 * serverseitig und setzt `data-dichte` an `<html>` — Begründung und Bauform in
 * `src/dichte/index.ts`.
 *
 * ## Warum ein Untermenü *(01.09.2026)*
 *
 * Die vier Stufen lagen zuvor **flach** im Nutzermenü, unter einer Überschrift.
 * Das Menü zählt sonst zwei Einträge; vier weitere plus Überschrift machten es
 * mehr als doppelt so lang, und zwar dauerhaft — auch für den Nutzer, der die
 * Größe einmal einstellt und nie wieder anfasst. Der Umschalter ist damit die
 * längste Sache in einem Menü, in dem er die seltenste ist.
 *
 * **Am Auslöser steht der aktuelle Wert bewusst nicht.** Kein
 * „Anzeigegröße · Sehr klein" und keine gedämpfte Zweitzeile: Beides machte die
 * eine Zeile wieder so hoch wie die vier, die sie ersetzt, und der Haken im
 * Untermenü sagt dasselbe an der Stelle, an der man ohnehin hinsieht.
 *
 * **Die Einträge selbst sind unverändert** — dieselben vier Absende-Knöpfe,
 * dasselbe Formular, dasselbe Attributpaar. Umgezogen ist nur ihr Ort.
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
 * mit, `DropdownMenuGroup` die Gruppe, `DropdownMenuSub…` das Untermenü samt
 * Pfeil. In `components/ui` ist dafür nichts geändert und nichts
 * nachinstalliert worden.
 *
 * ## Die Gruppe trägt keinen eigenen Namen mehr
 *
 * Vorher benannte eine `DropdownMenuLabel` die Gruppe über `aria-labelledby`.
 * Diese Überschrift ist der Auslöser geworden, und **Radix hängt den Namen von
 * dort an das Untermenü**: `MenuSubContent` setzt
 * `aria-labelledby={triggerId}` (nachgesehen in `@radix-ui/react-menu`). Der
 * Name ist also nicht weg, er steht eine Ebene höher — an dem Element, das ein
 * Vorleseprogramm beim Betreten ansagt. Ein zusätzliches `aria-label` an der
 * Gruppe ließe „Anzeigegröße" **zweimal** hintereinander vorlesen.
 *
 * > Aus demselben Grund bekommt der `SubTrigger` **kein eigenes `id`**: Radix
 * > setzt seines vor dem Durchreichen der Props (`id: subContext.triggerId,
 * > ...props`), ein eigenes gewänne — und zerschnitte damit genau die
 * > Verbindung, die den Namen trägt.
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

  return (
    <DropdownMenuSub>
      {/* Am Berührungsgerät fällt `--dichte-bedienelement` auf
          `--dichte-beruehrung` zurück, und das ist in jeder Stufe mindestens
          44 px (`globals.css`). Dieselbe Zusicherung wie an jedem anderen
          Eintrag dieses Menüs — der Auslöser eingeschlossen. */}
      <DropdownMenuSubTrigger className="min-h-bedienelement">
        <ALargeSmall aria-hidden="true" />
        {texte.dichte.bezeichnung}
      </DropdownMenuSubTrigger>
      <DropdownMenuSubContent>
        {/* `role="none"` nimmt das Formular aus dem Baum der Menürollen. Ohne
            das stünde zwischen `role="menu"` und `role="group"` ein Element,
            das dort nicht vorgesehen ist. */}
        <form action={dichteSetzen} role="none">
          <DropdownMenuGroup>
            {DICHTESTUFEN.map((stufe) => (
              <DropdownMenuItem
                key={stufe}
                asChild
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
      </DropdownMenuSubContent>
    </DropdownMenuSub>
  );
}
