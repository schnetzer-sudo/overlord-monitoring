"use client";

import { useId } from "react";

import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { useTexte } from "@/i18n/provider";

import type { Katalogfilter } from "../filter";

/**
 * Die zwei Filter der Pflegeliste — **beide in der URL**, damit man zeigen kann,
 * was man sieht.
 *
 * **Kontrollkästchen und kein Schalter.** Im Bestand trägt der `Switch` genau
 * einen Zustand, und der steht ausdrücklich *nicht* in der URL (die automatische
 * Aktualisierung der Nachrichtenliste). Was den gezeigten Ausschnitt bestimmt,
 * ist im Bestand ein Kontrollkästchen — im Status- und im Prozessfilter. Zwei
 * unabhängige Wahrheitswerte sind genau das, und keine sich ausschließende
 * Vorwahl, für die es die `ToggleGroup` gäbe.
 *
 * **Beide Hinweise stehen sichtbar da und nicht in einem `title`.** Der zweite
 * ist der wichtigere und wäre versteckt am gefährlichsten: „nur mit
 * Nachrichten" lässt die **ungeprüften** Zeilen stehen (E20), und wer das nicht
 * weiß, hält die Liste für falsch gefiltert. Der erste steht daneben, weil ein
 * einzelner erklärter und ein einzelner unerklärter Filter schlechter aussehen
 * als zwei gleich behandelte.
 */
export function KatalogFilterleiste({
  filter,
  aufNurOffene,
  aufNurMitNachrichten,
}: {
  filter: Katalogfilter;
  aufNurOffene: (wert: boolean) => void;
  aufNurMitNachrichten: (wert: boolean) => void;
}) {
  const texte = useTexte();
  const offeneId = useId();
  const nachrichtenId = useId();

  return (
    <div
      role="group"
      aria-label={texte.katalog.filter.bezeichnung}
      className="flex flex-wrap gap-x-6 gap-y-3"
    >
      <Schalter
        kennung={offeneId}
        beschriftung={texte.katalog.filter.nurOffene}
        hinweis={texte.katalog.filter.nurOffeneHinweis}
        gesetzt={filter.nurOffene}
        aufAenderung={aufNurOffene}
      />
      <Schalter
        kennung={nachrichtenId}
        beschriftung={texte.katalog.filter.nurMitNachrichten}
        hinweis={texte.katalog.filter.nurMitNachrichtenHinweis}
        gesetzt={filter.nurMitNachrichten}
        aufAenderung={aufNurMitNachrichten}
      />
    </div>
  );
}

function Schalter({
  kennung,
  beschriftung,
  hinweis,
  gesetzt,
  aufAenderung,
}: {
  kennung: string;
  beschriftung: string;
  hinweis: string;
  gesetzt: boolean;
  aufAenderung: (wert: boolean) => void;
}) {
  return (
    <div className="min-h-beruehrung flex items-start gap-2">
      <Checkbox
        id={kennung}
        checked={gesetzt}
        onCheckedChange={(wert) => aufAenderung(wert === true)}
        className="mt-1 shrink-0"
      />
      <Label htmlFor={kennung} className="cursor-pointer flex-col items-start gap-0.5 font-normal">
        <span>{beschriftung}</span>
        <span className="text-muted-foreground text-beiwerk">{hinweis}</span>
      </Label>
    </div>
  );
}
