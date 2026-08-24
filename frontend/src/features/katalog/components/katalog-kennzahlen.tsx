"use client";

import { Info } from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereAnteil, formatiereZahl } from "@/lib/format";

import type { Katalogzeile } from "../api";
import { fortschritt, hinweisNoetig } from "../kennzahlen";

/**
 * Die zwei Aussagen über der Liste: **der Fortschritt** (E18) und **der Hinweis**
 * (E17).
 *
 * Beide lesen die **volle** Liste und nicht die gefilterte — die Begründung
 * steht im Kopf von `kennzahlen.ts`.
 *
 * ## Der Fortschritt ist eine Zahl und kein Balken
 *
 * Ein Balken ist hier nicht verboten — anders als beim Lauf gibt es sehr wohl
 * bekannten Fortschritt. Er wäre trotzdem falsch: Bei `VOTG` stünde er lange bei
 * einem Zehntel, und eine breite, fast leere Fläche liest sich als Alarm. Die
 * Zahl sagt dasselbe nüchtern, und nüchtern ist hier richtig — sie ist die
 * Wahrheit über eine Arbeit, die eben lange dauert.
 *
 * Der Satz daneben — *„tote Prozesse und der Auffangprozess zählen mit"* — ist
 * nicht Beiwerk: Ohne ihn liest jemand die schlechte Zahl bei `VOTG` als Fehler,
 * wo 350 von 390 Prozessen schlicht keine Nachricht tragen.
 *
 * ## Der Hinweis hängt am Partner allein
 *
 * Er erscheint, wenn **keine** Zeile eine `vorschlagHerkunft` außer `KEINE`
 * trägt — gerechnet aus den Daten, niemals aus einer Mandantenliste im Code.
 * Er ist eine Auskunft und kein Fehler und trägt deshalb die neutrale Fassung
 * von `Alert`; die rote gehört dem Fehlerzustand.
 */
export function KatalogKennzahlen({ zeilen }: { zeilen: readonly Katalogzeile[] }) {
  const texte = useTexte();
  const sprache = useSprache();
  const stand = fortschritt(zeilen);

  return (
    <div className="flex flex-col gap-3">
      <p className="text-beiwerk flex flex-wrap items-baseline gap-x-2 gap-y-1">
        <span className="tabular-nums">
          {einsetzen(texte.katalog.fortschritt.satz, {
            gepflegt: formatiereZahl(stand.gepflegt, sprache),
            gesamt: formatiereZahl(stand.gesamt, sprache),
          })}
        </span>
        <span className="text-muted-foreground tabular-nums">
          · {formatiereAnteil(stand.anteil, sprache)}
        </span>
        <span className="text-muted-foreground">{texte.katalog.fortschritt.alleZaehlenMit}</span>
      </p>

      {hinweisNoetig(zeilen) ? (
        <Alert>
          <Info aria-hidden="true" />
          <AlertTitle>{texte.katalog.hinweis.ohnePartnervorschlag}</AlertTitle>
          <AlertDescription>{texte.katalog.hinweis.ohnePartnervorschlagFolge}</AlertDescription>
        </Alert>
      ) : null}
    </div>
  );
}
