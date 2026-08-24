"use client";

import { useId, useState } from "react";

import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Fehler } from "@/components/zustand";
import { useTexte } from "@/i18n/provider";

import { RICHTUNGEN, type Katalogzeile } from "../api";
import { useZuordnen } from "../hooks";
import {
  OHNE_RICHTUNG,
  alsAnfrage,
  entwurfAus,
  speichertOhnePartner,
  type Richtungswahl,
} from "../zuordnung";
import { PartnerFeld } from "./partner-feld";

/**
 * Die Bearbeitung **einer** Zeile (E19) — ein `PUT`, kein Sammelspeichern.
 *
 * ## Warum das Formular unter der Zeile steht und nicht in ihr
 *
 * Drei Bauformen kamen in Frage, und zwei scheitern an derselben Stelle:
 *
 * | Bauform | Woran sie scheitert |
 * |---|---|
 * | Eingabefelder **in** den Zellen | Unter `md` sind Richtung und Bestand ausgeblendet — die Richtung wäre am schmalen Fenster gar nicht erreichbar. Und 96 px Spaltenbreite tragen kein Auswahlfeld |
 * | Ein **Dialog** über der Tabelle | Nimmt die Nachbarzeilen weg, an denen man sich beim Kuratieren orientiert. Für zwei Felder außerdem zu schwer |
 * | Die Zeile **ersetzen** | Der bisherige Stand verschwindet genau in dem Moment, in dem man ihn ändern will |
 *
 * Geblieben ist die vierte: **Die Zeile bleibt stehen, das Formular klappt
 * darunter auf.** Der alte Stand ist beim Tippen sichtbar, die Zuordnung
 * funktioniert bei jeder Breite, und die Tastaturreihenfolge ist die natürliche.
 *
 * ## „Nichts" ist an beiden Feldern eine Wahl und kein Zustand, in den man fällt
 *
 * Das ist der eine Gedanke, aus dem der Rest folgt:
 *
 * - Der Partner hat eine **Leeren-Schaltfläche**, und wenn das Feld leer ist,
 *   sagt ein Satz darunter, was das Speichern bedeutet: *„hingesehen, es gibt
 *   keinen"* (E4). Er erscheint erst dann — neben einem gefüllten Feld wäre er
 *   Rauschen.
 * - Die Richtung hat **drei** Knöpfe statt zwei. Der dritte heißt „keine".
 *   Zwei Knöpfe mit Abwahl beim zweiten Druck sagen dasselbe und sagen es
 *   niemandem.
 *
 * Ein dritter Pflegestatus entsteht dadurch ausdrücklich nicht (E4, §9 der
 * Festlegung): „gepflegt mit leerem Partner" sagt es mit den Feldern, die
 * ohnehin da sind.
 *
 * ## Abbrechen verwirft, aber nie stillschweigend
 *
 * `Abbrechen` und `Escape` sind ausdrückliche Anweisungen des Nutzers und
 * brauchen keine Rückfrage. Was **nicht** passieren darf, ist das Verwerfen
 * ohne Anlass: Deshalb lässt sich keine zweite Zeile öffnen, solange diese
 * offen ist ({@link darfOeffnen}), und deshalb schließt sich das Formular erst
 * nach einer erfolgreichen Antwort.
 */
export function ZeilenFormular({
  zeile,
  aufSchliessen,
  vorschlaege,
}: {
  zeile: Katalogzeile;
  aufSchliessen: () => void;
  vorschlaege: readonly string[];
}) {
  const texte = useTexte();
  const partnerId = useId();
  const richtungId = useId();
  const [entwurf, setEntwurf] = useState(() => entwurfAus(zeile));
  const zuordnen = useZuordnen();

  function beiAbsenden(ereignis: React.FormEvent) {
    ereignis.preventDefault();
    zuordnen.mutate(
      { processId: zeile.processId, anfrage: alsAnfrage(entwurf) },
      { onSuccess: aufSchliessen },
    );
  }

  return (
    <form
      onSubmit={beiAbsenden}
      onKeyDown={(ereignis) => {
        if (ereignis.key === "Escape" && !zuordnen.isPending) {
          aufSchliessen();
        }
      }}
      className="flex flex-col gap-3 px-2 py-3"
    >
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:gap-6">
        <div className="flex min-w-0 flex-1 flex-col gap-1 md:max-w-80">
          <Label htmlFor={partnerId}>{texte.katalog.bearbeiten.partner}</Label>
          <PartnerFeld
            kennung={partnerId}
            wert={entwurf.partner}
            vorschlaege={vorschlaege}
            aufAenderung={(partner) => setEntwurf((bisher) => ({ ...bisher, partner }))}
          />
        </div>

        <div className="flex flex-col gap-1">
          <span id={richtungId} className="text-basis font-medium">
            {texte.katalog.bearbeiten.richtung}
          </span>
          <ToggleGroup
            type="single"
            variant="outline"
            aria-labelledby={richtungId}
            value={entwurf.richtung}
            onValueChange={(wert) =>
              setEntwurf((bisher) => ({
                ...bisher,
                // Die leere Zeichenkette meldet die `ToggleGroup` beim Abwählen
                // des aktiven Knopfes. Sie bedeutet dasselbe wie der dritte
                // Knopf und wird auf ihn abgebildet, statt einen vierten
                // Zustand entstehen zu lassen.
                richtung: (wert === "" ? OHNE_RICHTUNG : wert) as Richtungswahl,
              }))
            }
          >
            {RICHTUNGEN.map((richtung) => (
              <ToggleGroupItem
                key={richtung}
                value={richtung}
                className="min-h-bedienelement px-2.5"
              >
                {texte.katalog.richtungen[richtung]}
              </ToggleGroupItem>
            ))}
            <ToggleGroupItem value={OHNE_RICHTUNG} className="min-h-bedienelement px-2.5">
              {texte.katalog.bearbeiten.ohneRichtung}
            </ToggleGroupItem>
          </ToggleGroup>
        </div>
      </div>

      {speichertOhnePartner(entwurf) ? (
        <p className="text-muted-foreground text-beiwerk">
          {texte.katalog.bearbeiten.ohnePartnerHinweis}
        </p>
      ) : null}

      {/*
       * Der Fehler steht **am Formular** und nicht über der Ansicht: Die Liste
       * ist richtig, nur diese eine Eingabe nicht. Ohne `aufWiederholen` — der
       * Nutzer schickt selbst noch einmal ab, und für `partner-zu-lang` ändert
       * ein zweiter Versuch mit demselben Wert ohnehin nichts.
       */}
      {zuordnen.error ? <Fehler fehler={zuordnen.error} /> : null}

      <div className="flex flex-wrap items-center gap-2">
        <Button type="submit" disabled={zuordnen.isPending} className="min-h-beruehrung">
          {zuordnen.isPending
            ? texte.katalog.bearbeiten.speichernLaeuft
            : texte.katalog.bearbeiten.speichern}
        </Button>
        <Button
          type="button"
          variant="outline"
          disabled={zuordnen.isPending}
          onClick={aufSchliessen}
          className="min-h-beruehrung"
        >
          {texte.katalog.bearbeiten.abbrechen}
        </Button>
        {/*
         * Der Satz erklärt alle ausgegrauten „Bearbeiten"-Schaltflächen der
         * Liste auf einmal. Als `title` an jeder einzelnen stünde er dort, wo
         * ihn niemand sucht — und an einer deaktivierten Schaltfläche zeigen
         * ihn manche Browser gar nicht erst.
         */}
        <p className="text-muted-foreground text-beiwerk">{texte.katalog.bearbeiten.gesperrt}</p>
      </div>
    </form>
  );
}
