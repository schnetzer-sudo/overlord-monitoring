"use client";

import { useState } from "react";
import { Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Fehler, Laden } from "@/components/zustand";
import { useSprache, useTexte } from "@/i18n/provider";

import type { Massenmodus, Vorschlagsuebernahme } from "../api";
import { useVorschlagsuebernahme } from "../hooks";
import { darfUebernehmen, uebernahmesaetze } from "../uebernahme";

/**
 * **„Vorschläge übernehmen"** — ein Knopf, ein Dialog, eine Statusänderung
 * (E22 bis E24).
 *
 * ## Was hier passiert, und was ausdrücklich nicht
 *
 * Partner und Richtung stehen **bereits in der Zeile**: Die Heuristik hat sie
 * beim Lauf geschrieben, nur mit Status `OFFEN`. Übernehmen ist deshalb **kein
 * Kopieren von Werten, sondern ausschließlich eine Statusänderung**. Über die
 * Leitung reist keine Liste von `ProcessID`, sondern nur ein Modus (E23).
 *
 * ## Der Knopf trägt keine Bedingung „gibt es überhaupt welche"
 *
 * Er ist immer bedienbar. Diese Frage beantwortet die Vorschau, und zwar dort,
 * wo die Regel aus E22 lebt: im Backend. Stünde sie zusätzlich im Browser,
 * stünde sie zweimal — derselbe Gedanke wie bei E23.
 *
 * Gesperrt ist er in genau zwei Lagen, beide bereits im Bestand vorhanden:
 * solange eine Zeile bearbeitet wird (§11, Punkt 7 — er holt die Liste neu, und
 * die offene Zeile könnte dabei aus der Antwort fallen), und solange der Lauf
 * fährt. Beides trägt der Aufrufer zusammen als `gesperrt` heran.
 *
 * ## Die Vorschau wird beim Öffnen geholt, nicht beim Rendern der Seite
 *
 * **Im Ereignis und nicht in einem Effekt.** Das Öffnen ist etwas, das
 * passiert, und kein Zustand, mit dem sich etwas abgleichen ließe — dieselbe
 * Wahl wie beim Halt der Laufzeituhr in `lauf-knopf.tsx`.
 *
 * **Schließen und erneutes Öffnen holt sie neu**; eine alte Zahl wird nicht
 * wiederverwendet. Sonst bestätigte der Nutzer eine Zahl, die zu einer anderen
 * Anfrage gehört — genau der Fehler, den das gemeinsame Statement im Backend
 * auf seiner Seite ausschließt.
 *
 * ## Ohne die Schließen-Schaltfläche des Generators
 *
 * `showCloseButton={false}`: Sie trägt eine feste englische Zeichenkette —
 * dieselbe Klasse, die bei der Massenzuordnung weggelassen wurde. Das Formular
 * hat seinen eigenen Abbrechen-Knopf, und `Escape` schließt weiterhin.
 */
export function VorschlaegeUebernehmen({
  gesperrt,
  nurMitNachrichten,
}: {
  gesperrt: boolean;
  /**
   * Nur für den vierten Satz. **Er ändert an der Übernahme nichts** (E23) — und
   * genau das sagt er.
   */
  nurMitNachrichten: boolean;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const [offen, setOffen] = useState(false);
  const [vorschau, setVorschau] = useState<Vorschlagsuebernahme | null>(null);
  const uebernahme = useVorschlagsuebernahme();

  function frage(modus: Massenmodus) {
    uebernahme.mutate(
      { modus },
      {
        onSuccess: (antwort) => {
          if (antwort.modus === "AUSFUEHREN") {
            setOffen(false);
            return;
          }
          setVorschau(antwort);
        },
      },
    );
  }

  /**
   * Öffnen **holt** die Vorschau, Schließen **verwirft** sie. Beides im selben
   * Ereignis, damit keine Zahl aus einem früheren Öffnen überlebt.
   */
  function aufOffen(neu: boolean) {
    setOffen(neu);
    setVorschau(null);
    uebernahme.reset();
    if (neu) {
      frage("VORSCHAU");
    }
  }

  const bereit = darfUebernehmen(vorschau, uebernahme.isPending);

  return (
    <Dialog open={offen} onOpenChange={aufOffen}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline" disabled={gesperrt} className="min-h-beruehrung">
          <Check aria-hidden="true" />
          {texte.katalog.uebernahme.oeffnen}
        </Button>
      </DialogTrigger>
      <DialogContent showCloseButton={false} className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{texte.katalog.uebernahme.titel}</DialogTitle>
          <DialogDescription>{texte.katalog.uebernahme.einleitung}</DialogDescription>
        </DialogHeader>

        <form
          onSubmit={(ereignis) => {
            ereignis.preventDefault();
            if (bereit) {
              frage("AUSFUEHREN");
            }
          }}
          className="flex flex-col gap-4"
        >
          {/*
           * Die vier Zustände dieser Fläche. Der Fehler steht **im** Dialog und
           * nicht über der Liste: Die Liste ist richtig, nur diese eine Frage
           * nicht.
           */}
          {uebernahme.error ? (
            <Fehler fehler={uebernahme.error} />
          ) : vorschau === null ? (
            <Laden zeilen={2} />
          ) : (
            <div role="status" className="text-beiwerk flex flex-col gap-2">
              {uebernahmesaetze(vorschau, nurMitNachrichten, texte, sprache).map((satz) => (
                <p key={satz} className="max-w-prose">
                  {satz}
                </p>
              ))}
            </div>
          )}

          <div className="flex flex-wrap items-center gap-2">
            <Button type="submit" disabled={!bereit} className="min-h-beruehrung">
              {uebernahme.isPending && vorschau !== null
                ? texte.katalog.uebernahme.uebernehmenLaeuft
                : texte.katalog.uebernahme.uebernehmen}
            </Button>
            <Button
              type="button"
              variant="ghost"
              disabled={uebernahme.isPending}
              onClick={() => aufOffen(false)}
              className="min-h-beruehrung"
            >
              {texte.katalog.uebernahme.abbrechen}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
