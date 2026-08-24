"use client";

import { TriangleAlert } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";

import type { Vorgang } from "../selbstschutz";

/**
 * **E19 — der Entzug trifft den Handelnden.**
 *
 * Nach E5 verwirft *jeder* der fünf schreibenden Vorgänge **alle** Sitzungen des
 * betroffenen Kontos, ohne Fallunterscheidung. Trifft es das eigene Konto,
 * meldet der Admin sich mit dem Klick selbst ab — und landet nach
 * `docs/frontend-grundlagen.md` §5 **wortlos** auf der Anmeldung: Bei `401` wird
 * umgeleitet und nicht gemeldet.
 *
 * **Ohne diesen Dialog sähe das aus wie ein Absturz.** Der Nutzer drückt einen
 * Knopf, und die Anwendung ist weg; nichts in der Oberfläche verbindet das eine
 * mit dem anderen. Deshalb steht die Auskunft *vor* dem Aufruf und nicht danach
 * — danach gibt es keine Oberfläche mehr, die sie zeigen könnte.
 *
 * ## Warum ein Dialog und nicht ein Satz im Formular
 *
 * Ein Satz erklärt; ein Dialog **unterbricht und verlangt eine Antwort**. Das
 * ist hier der Unterschied: Die Folge ist nicht schlimm, aber sie ist
 * unumkehrbar in dem Sinn, der zählt — der Nutzer muss sich neu anmelden, und
 * das mitten in einer Arbeit, die er gerade nicht zu Ende gebracht hat. Ein
 * Hinweis, den man überliest, wäre für genau diesen Fall gebaut und würde ihn
 * verfehlen.
 *
 * ## Er erscheint nur für die Vorgänge, die auch wirklich laufen
 *
 * Sich selbst zu sperren, zu deaktivieren oder herabzustufen verbietet E12
 * bereits im Backend mit `409`. Dafür gibt es **keinen** Dialog: Er verspräche,
 * dass es nach dem Bestätigen passiert, und es passiert nicht. Dort zeigt die
 * Oberfläche die Übersetzung des Problemtyps. Die Auswahl trifft
 * {@link brauchtVorwarnung}.
 *
 * **Der Text nennt den Vorgang beim Namen** und nicht nur die Folge. „Du wirst
 * abgemeldet" allein ließe offen, ob die Änderung überhaupt stattfindet — sie
 * findet statt, und danach ist man abgemeldet.
 */
export function Vorwarnung({
  vorgang,
  laeuft,
  aufAbbrechen,
  aufBestaetigen,
}: {
  /** Der wartende Vorgang, oder `null` — dann ist der Dialog zu. */
  vorgang: Vorgang | null;
  laeuft: boolean;
  aufAbbrechen: () => void;
  aufBestaetigen: () => void;
}) {
  const texte = useTexte();

  return (
    <Dialog
      open={vorgang !== null}
      onOpenChange={(offen) => {
        if (!offen && !laeuft) {
          aufAbbrechen();
        }
      }}
    >
      {/*
       * Ohne die Schließen-Schaltfläche des Generators: Sie trägt eine feste
       * englische Zeichenkette (`docs/frontend-grundlagen.md` §10). Statt den
       * Generatorbereich von Hand zu ändern, wird sie weggelassen — der Dialog
       * hat seinen eigenen Abbrechen-Knopf, und `Escape` schließt weiterhin.
       */}
      <DialogContent showCloseButton={false} className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <TriangleAlert aria-hidden="true" className="size-4 shrink-0" />
            {texte.benutzer.vorwarnung.titel}
          </DialogTitle>
          <DialogDescription>
            {vorgang === null
              ? null
              : einsetzen(texte.benutzer.vorwarnung.text, {
                  vorgang: texte.benutzer.vorwarnung.vorgaenge[vorgang.art],
                })}
          </DialogDescription>
        </DialogHeader>

        <p className="text-beiwerk">{texte.benutzer.vorwarnung.folge}</p>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            disabled={laeuft}
            onClick={aufBestaetigen}
            className="min-h-beruehrung"
          >
            {laeuft ? texte.benutzer.vorwarnung.laeuft : texte.benutzer.vorwarnung.bestaetigen}
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={laeuft}
            onClick={aufAbbrechen}
            className="min-h-beruehrung"
          >
            {texte.benutzer.vorwarnung.abbrechen}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
