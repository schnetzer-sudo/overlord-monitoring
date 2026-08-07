"use client";

import { useMemo, useState } from "react";
import { Check, ChevronsUpDown, TriangleAlert, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import { useProzesse } from "../hooks";
import type { Prozess } from "../api";

/**
 * Der Prozessfilter — eine Mehrfachauswahl aus `GET /api/prozesse`.
 *
 * **Warum die Auswahl aus einem eigenen Endpunkt kommt und nicht aus den
 * angezeigten Zeilen:** Sonst enthielte sie nur, was ohnehin schon zu sehen ist —
 * und genau der Prozess, der *nichts* geliefert hat, fehlte. Der ist aber der
 * interessante, wenn jemand wissen will, warum nichts ankommt.
 *
 * **Unbekannte Kennungen werden gezeigt, nicht verschluckt.** Ein geteilter Link
 * kann `ProcessID`s eines anderen Mandanten tragen; für den Empfänger sind sie
 * bedeutungslos und die Liste bleibt garantiert leer. Sie stillschweigend zu
 * entfernen hieße, dem Empfänger einen anderen Ausschnitt zu zeigen, als der
 * Absender geteilt hat — sie stehen deshalb als eigener, entfernbarer Eintrag da.
 * Beim Mandantenwechsel selbst stellt sich die Frage nicht: Dort wird der
 * Zwischenspeicher geleert und auf die Startseite gewechselt, die Filter der URL
 * fallen also ohnehin weg (`lib/zwischenspeicher.ts`).
 */
export function ProzessFilter({
  gewaehlt,
  aufAuswahl,
}: {
  gewaehlt: string[];
  aufAuswahl: (prozesse: string[]) => void;
}) {
  const texte = useTexte();
  const [offen, setOffen] = useState(false);
  const [eingrenzung, setEingrenzung] = useState("");
  const { data: prozesse, isPending } = useProzesse();

  const bekannt = useMemo(() => new Set((prozesse ?? []).map((p) => p.processId)), [prozesse]);
  const unbekannt = gewaehlt.filter((id) => !isPending && !bekannt.has(id));

  const sichtbar = useMemo(() => {
    const begriff = eingrenzung.trim().toLowerCase();
    if (begriff === "") {
      return prozesse ?? [];
    }
    return (prozesse ?? []).filter((prozess) =>
      `${prozess.processName ?? ""} ${prozess.projectName ?? ""}`.toLowerCase().includes(begriff),
    );
  }, [prozesse, eingrenzung]);

  function umschalten(id: string) {
    aufAuswahl(gewaehlt.includes(id) ? gewaehlt.filter((wert) => wert !== id) : [...gewaehlt, id]);
  }

  const beschriftung =
    gewaehlt.length === 0
      ? texte.nachrichten.prozessfilter.alle
      : einsetzen(texte.nachrichten.prozessfilter.gewaehlt, { anzahl: gewaehlt.length });

  return (
    <div className="flex items-center gap-1">
      <Popover open={offen} onOpenChange={setOffen}>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            className="min-h-bedienelement w-full justify-between gap-2 sm:w-56"
          >
            <span className="truncate">
              <span className="text-muted-foreground">
                {texte.nachrichten.prozessfilter.bezeichnung}:{" "}
              </span>
              {beschriftung}
            </span>
            <ChevronsUpDown aria-hidden="true" className="size-3.5 shrink-0 opacity-60" />
          </Button>
        </PopoverTrigger>
        {/*
         * **Die aufgeklappte Liste ist breiter als das geschlossene Feld** — und
         * das ist keine Kosmetik. Bei 733 Einträgen für `NEXANS` ist die Auswahl
         * ohne vollständige Namen nicht bedienbar: Der längste Prozessname hat 58
         * Zeichen, der längste Projektname 44, und im Vorgängerstand schnitt die
         * Liste bei 20 rem ab. Wer zwischen „…_LAB_VDA" und „…_LAB_VDA_2" wählen
         * soll, sieht dann beide Male dasselbe.
         *
         * 34 rem tragen den Namen im Regelfall ganz; was darüber liegt, bricht um
         * (siehe `ProzessZeile`) statt zu kürzen. Nach oben begrenzt das Fenster:
         * `calc(100vw-2rem)` hält die Schublade auch bei 360 px im Bild.
         */}
        <PopoverContent
          align="start"
          className="w-[min(34rem,calc(100vw-2rem))] max-w-[calc(100vw-2rem)]"
        >
          <Label htmlFor="prozess-eingrenzung" className="sr-only">
            {texte.nachrichten.prozessfilter.suchen}
          </Label>
          <Input
            id="prozess-eingrenzung"
            value={eingrenzung}
            onChange={(ereignis) => setEingrenzung(ereignis.target.value)}
            placeholder={texte.nachrichten.prozessfilter.suchen}
            // Rein örtlich: Diese Eingabe schränkt die bereits geladene Liste
            // ein und stellt keine Anfrage. Bei ein paar hundert Stammdaten ist
            // ein Serverparameter nicht nötig — und er brächte genau die
            // Fallstricke mit, die den Freitextfilter der Liste teuer machen.
            className="h-bedienelement"
          />

          <div className="max-h-72 overflow-y-auto">
            {isPending ? (
              <p className="text-muted-foreground px-1 py-2">{texte.zustand.laedt}</p>
            ) : sichtbar.length === 0 ? (
              <p className="text-muted-foreground px-1 py-2">
                {(prozesse ?? []).length === 0
                  ? texte.nachrichten.prozessfilter.leer
                  : texte.nachrichten.prozessfilter.keineTreffer}
              </p>
            ) : (
              <ul className="flex flex-col">
                {sichtbar.map((prozess) => (
                  <ProzessZeile
                    key={prozess.processId}
                    prozess={prozess}
                    gewaehlt={gewaehlt.includes(prozess.processId)}
                    aufUmschalten={() => umschalten(prozess.processId)}
                  />
                ))}
              </ul>
            )}

            {unbekannt.length > 0 ? (
              <ul className="border-border mt-1 flex flex-col border-t pt-1">
                {unbekannt.map((id) => (
                  <li key={id}>
                    <button
                      type="button"
                      onClick={() => umschalten(id)}
                      className="hover:bg-muted focus-visible:ring-ring min-h-beruehrung flex w-full items-center gap-2 rounded-sm px-1 text-left focus-visible:ring-2 focus-visible:outline-none"
                    >
                      <TriangleAlert aria-hidden="true" className="size-3.5 shrink-0 opacity-70" />
                      <span className="min-w-0 flex-1">
                        <span className="block">{texte.nachrichten.prozessfilter.unbekannt}</span>
                        <span className="text-muted-foreground text-beiwerk block font-mono break-all">
                          {id}
                        </span>
                      </span>
                      <Check aria-hidden="true" className="size-3.5 shrink-0 opacity-70" />
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
        </PopoverContent>
      </Popover>

      {gewaehlt.length > 0 ? (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="min-h-bedienelement shrink-0"
          onClick={() => aufAuswahl([])}
          title={texte.nachrichten.prozessfilter.zuruecksetzen}
        >
          <X aria-hidden="true" />
          <span className="sr-only">{texte.nachrichten.prozessfilter.zuruecksetzen}</span>
        </Button>
      ) : null}
    </div>
  );
}

/**
 * Ein Prozess mit seinem Projekt darunter. Das Projekt steht dabei, weil
 * Prozessnamen sich zwischen Projekten wiederholen — ohne die Gruppe wäre die
 * Auswahl mehrdeutig.
 */
function ProzessZeile({
  prozess,
  gewaehlt,
  aufUmschalten,
}: {
  prozess: Prozess;
  gewaehlt: boolean;
  aufUmschalten: () => void;
}) {
  const texte = useTexte();
  const kennung = `prozess-${prozess.processId}`;

  return (
    <li>
      <div
        className={cn(
          "hover:bg-muted min-h-beruehrung flex items-center gap-2 rounded-sm px-1",
          gewaehlt && "bg-accent",
        )}
      >
        <Checkbox
          id={kennung}
          checked={gewaehlt}
          onCheckedChange={aufUmschalten}
          className="shrink-0"
        />
        <Label htmlFor={kennung} className="min-w-0 flex-1 cursor-pointer py-1 font-normal">
          {/*
           * **Umbrechen statt kürzen.** In der Tabelle gilt die feste Zeilenhöhe,
           * weil dort 50 Zeilen überflogen werden; hier wird *ausgewählt*, und
           * ein gekürzter Name macht die Auswahl mehrdeutig. Ein Prozessname, der
           * nicht in eine Zeile passt, bekommt deshalb eine zweite — die
           * Zeilenhöhe der Auswahl darf schwanken, die der Liste nicht.
           */}
          <span className="block break-words">
            {prozess.processName ?? texte.nachrichten.nichtZugeordnet}
          </span>
          <span className="text-muted-foreground text-beiwerk block break-words">
            {prozess.projectName ?? texte.nachrichten.nichtZugeordnet}
          </span>
        </Label>
      </div>
    </li>
  );
}
