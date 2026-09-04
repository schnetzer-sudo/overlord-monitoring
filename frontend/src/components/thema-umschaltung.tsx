"use client";

import { Check, SunMoon } from "lucide-react";

import {
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
} from "@/components/ui/dropdown-menu";
import { themaSetzen } from "@/thema/aktion";
import { THEMAWERTE, THEMA_FELD } from "@/thema";
import { useThema } from "@/thema/provider";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

/**
 * Der Themaumschalter — **drei Werte, ein Formular**, hinter einer Zeile im
 * Nutzermenü.
 *
 * **Er ist Zeile für Zeile derselbe Baustein wie `dichte-umschaltung.tsx`**,
 * und das ist keine Kopie aus Bequemlichkeit, sondern die Aussage: Sprache,
 * Dichte und Thema sind dieselbe Art Sache — eine Eigenschaft des Nutzers, im
 * Cookie, serverseitig gelesen, über eine Server-Aktion umgeschaltet. Zwei
 * benachbarte Menüeinträge, die sich verschieden bedienen ließen, wären die
 * teurere Lösung.
 *
 * Der gewählte Wert liegt im Cookie `overlord_thema`, das Wurzel-Layout liest
 * ihn serverseitig und setzt `data-thema` an `<html>` — Begründung und Bauform
 * in `src/thema/index.ts`.
 *
 * ## Warum ein Untermenü
 *
 * Aus demselben Grund wie bei der Dichte: Das Nutzermenü zählt sonst zwei
 * Einträge, und drei weitere plus Überschrift machten es dauerhaft länger —
 * auch für den Nutzer, der das Erscheinungsbild einmal einstellt und nie wieder
 * anfasst. Der Haken im Untermenü sagt an der Stelle, an der man ohnehin
 * hinsieht, was gilt; am Auslöser steht der aktuelle Wert deshalb bewusst
 * nicht.
 *
 * ## Warum ein Formular und keine Radiogruppe
 *
 * `components/ui/dropdown-menu.tsx` liefert `DropdownMenuRadioGroup` und
 * `DropdownMenuRadioItem` mit; **beide sind da, und keiner ist hier
 * verwendbar.** Sie schalten über `onValueChange` im Browser, und dieser
 * Umschalter muss über eine **Server-Aktion** schalten: Das Wurzel-Layout liest
 * den Wert auf dem Server, also muss der neue dort ankommen. Ein `asChild` um
 * eine Absende-Schaltfläche scheidet aus, weil `RadioItem` seine Kinder selbst
 * zusammensetzt (Anzeigehäkchen plus `children`).
 *
 * **Nachgeholt wird davon genau ein Attributpaar** — `role="menuitemradio"` und
 * `aria-checked` —, und das ist dieselbe Auszeichnung, die Radix an einem
 * `RadioItem` selbst erzeugt. Die **Gestalt** kommt unverändert aus dem
 * Generatorbereich; in `components/ui` ist nichts geändert und nichts
 * nachinstalliert worden.
 *
 * ## Kein zweites Zeichen je Zeile
 *
 * Naheliegend wären Sonne, Mond und Bildschirm an den drei Einträgen. Sie
 * stehen bewusst nicht da: Der Haken ist in diesem Menü das Zeichen für *„das
 * gilt"*, und ein zweites Zeichen daneben machte aus einer Auskunft zwei. Der
 * Auslöser trägt eines, so wie der Dichteauslöser eines trägt.
 *
 * ## Das Menü bleibt offen
 *
 * `onSelect` wird abgefangen — derselbe Grund wie bei der Dichte, und hier noch
 * deutlicher: Die Wirkung tritt **hinter** dem Menü ein. Wer hell und dunkel
 * vergleichen will, müsste es sonst für jeden Vergleich neu öffnen.
 *
 * ## Die Beschriftung sagt „Erscheinungsbild"
 *
 * Nicht „Thema" und nicht „Dunkelmodus": Der Eintrag hat drei Werte, und einer
 * davon ist *hell*. „Dunkelmodus" wäre der Name eines der drei Zustände als
 * Name für die Wahl. Die Texte stehen in `i18n/de.ts` und `en.ts`, **keine
 * Zeichenkette in dieser Datei**.
 */
export function ThemaUmschaltung() {
  const aktiv = useThema();
  const texte = useTexte();

  return (
    <DropdownMenuSub>
      {/* Am Berührungsgerät fällt `--dichte-bedienelement` auf
          `--dichte-beruehrung` zurück, und das ist in jeder Stufe mindestens
          44 px (`globals.css`). Dieselbe Zusicherung wie an jedem anderen
          Eintrag dieses Menüs — der Auslöser eingeschlossen. */}
      <DropdownMenuSubTrigger className="min-h-bedienelement">
        <SunMoon aria-hidden="true" />
        {texte.thema.bezeichnung}
      </DropdownMenuSubTrigger>
      <DropdownMenuSubContent>
        {/* `role="none"` nimmt das Formular aus dem Baum der Menürollen. Ohne
            das stünde zwischen `role="menu"` und `role="group"` ein Element,
            das dort nicht vorgesehen ist. */}
        <form action={themaSetzen} role="none">
          <DropdownMenuGroup>
            {THEMAWERTE.map((wahl) => (
              <DropdownMenuItem
                key={wahl}
                asChild
                className="min-h-bedienelement"
                onSelect={(ereignis) => ereignis.preventDefault()}
              >
                <button
                  type="submit"
                  name={THEMA_FELD}
                  value={wahl}
                  role="menuitemradio"
                  aria-checked={wahl === aktiv}
                  className="w-full text-left"
                >
                  {/* Das Häkchen steht auch bei den inaktiven Werten im Fluss —
                      nur unsichtbar. Sonst rückten die Beschriftungen beim
                      Umschalten um seine Breite hin und her, und das ist die
                      Bewegung, die §1 des visuellen Konzepts ausschließt. */}
                  <Check aria-hidden="true" className={cn(wahl !== aktiv && "invisible")} />
                  {texte.thema.werte[wahl]}
                </button>
              </DropdownMenuItem>
            ))}
          </DropdownMenuGroup>
        </form>
      </DropdownMenuSubContent>
    </DropdownMenuSub>
  );
}
