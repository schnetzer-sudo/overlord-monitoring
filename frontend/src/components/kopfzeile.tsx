"use client";

import { useState } from "react";
import { Menu } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Suchfeld } from "@/features/nachrichten/components/suchfeld";
import type { Selbstauskunft } from "@/features/sitzung/api";
import { Nutzermenue } from "@/features/sitzung/components/nutzermenue";
import { useTexte } from "@/i18n/provider";

import { MandantAnzeige } from "./mandant-anzeige";
import { NavigationsListe } from "./navigations-liste";
import { Sprachumschaltung } from "./sprachumschaltung";

/**
 * Kopfzeile: Produktname, reservierter Suchplatz, aktiver Mandant,
 * Sprachumschaltung, Nutzermenü.
 *
 * Sie spannt über die volle Fensterbreite und steht fest — gescrollt wird
 * ausschließlich der Inhaltsbereich darunter.
 *
 * Unter 768 Pixel bricht die Zeile um: Produktname und Nutzermenü oben, Mandant
 * und Sprache darunter. Das kostet ein paar Pixel Höhe und ist der Preis dafür,
 * dass **nichts** ausgeblendet und **nichts** horizontal gescrollt wird — bei
 * 360 Pixel Breite passt beides nicht in eine Zeile, ohne dass etwas
 * unleserlich wird.
 *
 * Die Navigation wird darunter zur Schublade. Der aktive Mandant nicht.
 */
export function Kopfzeile({
  auskunft,
  navigationSichtbar,
  mandantenwechselErlaubt,
}: {
  auskunft: Selbstauskunft;
  navigationSichtbar: boolean;
  mandantenwechselErlaubt: boolean;
}) {
  const texte = useTexte();
  const [schubladeOffen, setzeSchublade] = useState(false);

  return (
    <header className="bg-card border-border shrink-0 border-b">
      {/* Der linke Innenabstand ist derselbe wie in der Navigationsspalte
          darunter, damit Produktname und Navigationseinträge auf einer Kante
          stehen. */}
      <div className="md:h-kopfzeile flex flex-wrap items-center gap-2 px-3 py-2 md:flex-nowrap md:px-5 md:py-0">
        {navigationSichtbar ? (
          <Sheet open={schubladeOffen} onOpenChange={setzeSchublade}>
            <SheetTrigger asChild>
              <Button
                type="button"
                variant="ghost"
                className="min-h-bedienelement order-1 h-auto shrink-0 px-2 md:hidden"
                aria-label={texte.navigation.oeffnen}
              >
                <Menu aria-hidden="true" />
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-72 p-4">
              <SheetHeader className="p-0">
                <SheetTitle>{texte.navigation.bezeichnung}</SheetTitle>
                <SheetDescription className="sr-only">
                  {texte.anwendung.beschreibung}
                </SheetDescription>
              </SheetHeader>
              <NavigationsListe
                rolle={auskunft.role}
                aufAuswahl={() => setzeSchublade(false)}
                className="mt-4"
              />
            </SheetContent>
          </Sheet>
        ) : null}

        <span className="text-ueberschrift order-2 min-w-0 flex-1 truncate font-semibold">
          {texte.anwendung.name}
        </span>

        {/*
         * Der seit Schritt 3 reservierte Platz für die Belegsuche — **seit
         * Schritt 7, Teil 3 gefüllt**.
         *
         * Die Suche ist laut Leitsatz der Haupteinstieg; ihr Platz stand deshalb
         * von Anfang an fest, damit sie sich später nicht zwischen Mandant,
         * Sprache und Nutzermenü drängt. Bis dahin blieb er ausdrücklich leer —
         * ein Feld, das nichts tut, ist schlechter als keins.
         *
         * **Unterhalb von 768 px ist es eine eigene, volle Zeile.** Am
         * Zeigergerät sitzt es in den reservierten 18 rem zwischen Produktname
         * und Mandant; darunter trägt diese Breite nicht mehr. Es *entfällt*
         * dort aber nicht: Ein Haupteinstieg, den es am schmalen Fenster nicht
         * gibt, ist keiner. Das kostet eine Zeile Höhe — dieselbe Abwägung wie
         * beim Umbruch der Kopfzeile selbst (`docs/visuelles-konzept.md` §6).
         *
         * **Kein neuer Umbruchpunkt**: `md` ist der des Projekts.
         */}
        {navigationSichtbar ? (
          <div data-bereich="suche" className="md:w-suchbereich order-6 w-full shrink-0 md:order-3">
            <Suchfeld />
          </div>
        ) : null}

        {/* Auf dem Handy die zweite Zeile, am Rechner rechts neben dem Suchplatz. */}
        <div className="order-5 flex w-full min-w-0 items-center gap-2 md:order-4 md:w-auto">
          <MandantAnzeige
            mandant={auskunft.mandant}
            wechselErlaubt={mandantenwechselErlaubt}
            className="min-w-0 flex-1 md:flex-none"
          />
          <Sprachumschaltung />
        </div>

        <div className="order-4 shrink-0 md:order-5">
          <Nutzermenue auskunft={auskunft} />
        </div>
      </div>
    </header>
  );
}
