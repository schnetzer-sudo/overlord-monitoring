"use client";

import { useId, useState } from "react";
import { X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import { passendeVorschlaege } from "../zuordnung";

/**
 * Der Partner: **ein Auswahlfeld mit Vorschlägen, und frei tippbar** (E21).
 *
 * ## Warum es ein echtes Eingabefeld ist und keine Auswahl
 *
 * Die Vorschlagsliste speist sich aus den bereits gepflegten Zeilen desselben
 * Mandanten (E2). Eine geschlossene Auswahl wäre deshalb ein Kreis ohne
 * Eingang: **Der erste Partner eines Mandanten stünde nie darin**, und die
 * Kuratierung käme nie in Gang. Die Vorschläge sind ein Geländer gegen
 * Schreibvarianten — `BAYER` neben `Bayer` neben `BAYER AG` —, keine Schranke.
 *
 * ## Warum von Hand und nicht über einen Generator-Baustein
 *
 * Der Bestand kennt genau ein Muster für „aus einer Liste wählen": Eingabefeld
 * plus gefilterte Liste, von Hand gebaut (`features/nachrichten/components/
 * prozess-filter.tsx`). Ein Kommandopalette-Baustein brächte eine neue
 * Abhängigkeit für einen einzigen Fall — und sein Eingabefeld trägt den
 * *Suchbegriff*, nicht den Wert. Genau das ist hier verkehrt herum: **Das Feld
 * ist der Wert**, die Liste hilft nur beim Treffen.
 *
 * Ein `Popover` scheidet aus demselben Grund aus: Er nimmt dem Feld den Fokus,
 * und ein Auswahlfeld, in dem man nicht tippen kann, während die Liste offen
 * ist, ist keins.
 *
 * ## Tastatur
 *
 * Pfeil ab öffnet und wandert, Pfeil auf zurück — bis **über** den ersten
 * Eintrag hinaus, und dann ist wieder der getippte Text der gewählte Wert. Enter
 * übernimmt den hervorgehobenen Vorschlag, ohne das Formular abzuschicken; ohne
 * Hervorhebung schickt es ab, denn dann steht der gemeinte Wert schon da.
 * Escape schließt die Liste und **hält an** — das zweite Escape bricht die
 * Bearbeitung ab. Wer eine Liste offen hat, meint mit „weg damit" die Liste.
 */
export function PartnerFeld({
  wert,
  aufAenderung,
  vorschlaege,
  kennung,
}: {
  wert: string;
  aufAenderung: (wert: string) => void;
  vorschlaege: readonly string[];
  kennung: string;
}) {
  const texte = useTexte();
  const listenId = useId();
  const [offen, setOffen] = useState(false);
  const [aktiv, setAktiv] = useState(-1);

  const treffer = passendeVorschlaege(vorschlaege, wert);
  const listeSichtbar = offen && treffer.length > 0;
  const eintragId = (nummer: number) => `${listenId}-${nummer}`;

  function uebernimm(name: string) {
    aufAenderung(name);
    setOffen(false);
    setAktiv(-1);
  }

  function beiTaste(ereignis: React.KeyboardEvent<HTMLInputElement>) {
    if (ereignis.key === "ArrowDown") {
      ereignis.preventDefault();
      if (!listeSichtbar) {
        setOffen(true);
        setAktiv(0);
        return;
      }
      setAktiv((bisher) => Math.min(bisher + 1, treffer.length - 1));
      return;
    }
    if (ereignis.key === "ArrowUp") {
      ereignis.preventDefault();
      setAktiv((bisher) => Math.max(bisher - 1, -1));
      return;
    }
    if (ereignis.key === "Enter" && listeSichtbar && aktiv >= 0) {
      // Kein Abschicken: Der Nutzer hat einen Vorschlag gewählt, nicht das
      // Formular beendet.
      ereignis.preventDefault();
      uebernimm(treffer[aktiv]);
      return;
    }
    if (ereignis.key === "Escape" && offen) {
      // `stopPropagation`, damit das Formular darüber die Bearbeitung nicht
      // gleich mit abbricht — siehe den Kopf dieser Datei.
      ereignis.preventDefault();
      ereignis.stopPropagation();
      setOffen(false);
      setAktiv(-1);
    }
  }

  return (
    <div className="relative flex flex-col gap-1">
      <div className="flex items-start gap-1">
        <Input
          id={kennung}
          value={wert}
          role="combobox"
          aria-expanded={listeSichtbar}
          aria-controls={listenId}
          aria-autocomplete="list"
          aria-activedescendant={listeSichtbar && aktiv >= 0 ? eintragId(aktiv) : undefined}
          autoComplete="off"
          placeholder={texte.katalog.bearbeiten.partnerPlatzhalter}
          onChange={(ereignis) => {
            aufAenderung(ereignis.target.value);
            setOffen(true);
            setAktiv(-1);
          }}
          onFocus={() => setOffen(true)}
          onBlur={() => {
            setOffen(false);
            setAktiv(-1);
          }}
          onKeyDown={beiTaste}
          className="h-feld min-w-0 flex-1"
        />
        {/*
         * **Der Weg zum leeren Feld, und er ist der Punkt.** Ein gepflegter
         * leerer Partner heißt „hingesehen, es gibt keinen" (E4) und ist die
         * einzige Pflege, die ein toter Prozess je bekommt. Ohne diese
         * Schaltfläche müsste man den Vorschlag markieren und löschen — und
         * niemand käme auf die Idee, dass das erlaubt ist.
         */}
        <Button
          type="button"
          variant="ghost"
          size="icon"
          disabled={wert === ""}
          onClick={() => uebernimm("")}
          title={texte.katalog.bearbeiten.partnerLeeren}
          className="min-h-feld shrink-0"
        >
          <X aria-hidden="true" />
          <span className="sr-only">{texte.katalog.bearbeiten.partnerLeeren}</span>
        </Button>
      </div>

      {listeSichtbar ? (
        <ul
          id={listenId}
          role="listbox"
          aria-label={texte.katalog.bearbeiten.vorschlaege}
          className="border-border bg-popover absolute top-full right-0 left-0 z-20 mt-1 max-h-56 overflow-y-auto rounded-md border p-1 shadow-md"
        >
          {treffer.map((name, nummer) => (
            <li
              key={name}
              id={eintragId(nummer)}
              role="option"
              aria-selected={nummer === aktiv}
              // `onMouseDown` statt `onClick`: Der Klick auf einen Eintrag darf
              // dem Feld nicht vorher den Fokus nehmen — sonst schließt `onBlur`
              // die Liste, und der Klick geht ins Leere.
              onMouseDown={(ereignis) => {
                ereignis.preventDefault();
                uebernimm(name);
              }}
              className={cn(
                "min-h-beruehrung flex cursor-pointer items-center rounded-sm px-2 break-all",
                nummer === aktiv ? "bg-accent text-accent-foreground" : "hover:bg-muted",
              )}
            >
              {name}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
