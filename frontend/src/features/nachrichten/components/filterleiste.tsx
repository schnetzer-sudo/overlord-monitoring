"use client";

import { useEffect, useId, useState } from "react";
import { ChevronsUpDown, Eye, EyeOff, Search, X } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { zeitfenstermodus, ZEITRAEUME, type Zeitraum } from "@/lib/filter";
import { wanduhrzeitFuerEingabe, zeitpunktAusWanduhrzeit } from "@/lib/format";

import {
  STATUSARTEN,
  SUCHE_MINDESTLAENGE,
  type Nachrichtenfilter,
  type Statusart,
} from "../filter";
import { ProzessFilter } from "./prozess-filter";

/** Wie lange nach dem letzten Tastendruck gewartet wird, bevor gesucht wird. */
const SUCHE_ENTPRELLUNG_MS = 400;

type Steuerung = {
  setzeZeitraum: (zeitraum: Zeitraum) => void;
  setzeFreiesFenster: (von: Date | null, bis: Date | null) => void;
  setzeZeitfensterZurueck: () => void;
  setzeStatus: (status: Statusart[]) => void;
  setzeProzesse: (prozesse: string[]) => void;
  setzeSuche: (suche: string) => void;
  setzeZwischenschritte: (zwischenschritte: boolean) => void;
};

/**
 * Die Filterleiste. Jeder Wert hier steht in der URL — was der Nutzer einstellt,
 * soll ein Kollege im Link sehen.
 *
 * **Nicht in der URL:** die Seitenposition (ein Link auf Seite sieben zeigte beim
 * Empfänger auf andere Zeilen) und der Schalter für die automatische
 * Aktualisierung (er betrifft die Arbeitsweise des Betrachters, nicht den
 * gezeigten Ausschnitt).
 */
export function Filterleiste({
  filter,
  steuerung,
  suchfehler,
}: {
  filter: Nachrichtenfilter;
  steuerung: Steuerung;
  /** Ein `suchbegriff-zu-unscharf` gehört an das Suchfeld, nicht über die Ansicht. */
  suchfehler?: string;
}) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-start gap-2">
        <Zeitfensterwahl filter={filter} steuerung={steuerung} />
        <StatusFilter gewaehlt={filter.status ?? []} aufAuswahl={steuerung.setzeStatus} />
        <ProzessFilter gewaehlt={filter.prozess ?? []} aufAuswahl={steuerung.setzeProzesse} />
        <Suchfeld wert={filter.suche ?? ""} aufSuche={steuerung.setzeSuche} fehler={suchfehler} />
      </div>
      <ZwischenschritteChip
        eingeblendet={filter.zwischenschritte}
        aufUmschalten={steuerung.setzeZwischenschritte}
      />
    </div>
  );
}

/**
 * Zeitfenster als sichtbare Vorwahlen, dazu der freie Modus.
 *
 * **Beide Modi zugleich lässt die Oberfläche gar nicht erst zu.** Das Backend
 * lehnte das mit `zeitfenster-mehrdeutig` ab — bewusst statt einer stillen
 * Vorrangregel —, und ein Nutzer, der über eine Schaltfläche in einen
 * Fehlerzustand gerät, hat keine Möglichkeit, ihn zu verstehen. Eine Vorwahl
 * löscht deshalb `von`/`bis`, ein freies Fenster löscht `zeitraum`.
 *
 * **Ohne Auswahl ist keine Vorwahl gedrückt.** Es gilt dann der Standard des
 * Backends aus Regel L1. Eine Vorwahl hier vorzumarkieren hieße, denselben Wert
 * ein zweites Mal zu pflegen — der Hinweis daneben sagt stattdessen, was gerade
 * gilt.
 */
function Zeitfensterwahl({
  filter,
  steuerung,
}: {
  filter: Nachrichtenfilter;
  steuerung: Steuerung;
}) {
  const texte = useTexte();
  const zone = useAnzeigezone();
  const modus = zeitfenstermodus(filter);
  const vonId = useId();
  const bisId = useId();

  const beschriftung: Record<Zeitraum, string> = {
    "24h": texte.nachrichten.zeitfenster.h24,
    "7d": texte.nachrichten.zeitfenster.d7,
    "30d": texte.nachrichten.zeitfenster.d30,
  };

  return (
    <div className="flex flex-col gap-1">
      <ToggleGroup
        type="single"
        variant="outline"
        aria-label={texte.nachrichten.zeitfenster.bezeichnung}
        value={modus === "frei" ? "frei" : (filter.zeitraum ?? "")}
        onValueChange={(wert) => {
          if (wert === "") {
            steuerung.setzeZeitfensterZurueck();
          } else if (wert === "frei") {
            // Der freie Modus beginnt leer: Ein vorbelegtes Fenster wäre wieder
            // ein zweiter Standardwert.
            steuerung.setzeFreiesFenster(null, null);
          } else {
            steuerung.setzeZeitraum(wert as Zeitraum);
          }
        }}
      >
        {ZEITRAEUME.map((zeitraum) => (
          <ToggleGroupItem key={zeitraum} value={zeitraum} className="min-h-bedienelement px-2.5">
            {beschriftung[zeitraum]}
          </ToggleGroupItem>
        ))}
        <ToggleGroupItem value="frei" className="min-h-bedienelement px-2.5">
          {texte.nachrichten.zeitfenster.frei}
        </ToggleGroupItem>
      </ToggleGroup>

      {modus === "offen" ? (
        <p className="text-muted-foreground text-beiwerk">
          {texte.nachrichten.zeitfenster.standardHinweis}
        </p>
      ) : null}

      {modus === "frei" ? (
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-0.5">
            <Label htmlFor={vonId} className="text-beiwerk">
              {texte.nachrichten.zeitfenster.von}
            </Label>
            {/* Die Eingabe ist Wanduhrzeit ohne Zone. Umgerechnet wird in der
                Anzeigezone — sonst wäre das freie Fenster gegen die Daten
                verschoben, sobald jemand nicht in der Zone des Servers sitzt. */}
            <Input
              id={vonId}
              type="datetime-local"
              className="h-bedienelement w-auto"
              value={wanduhrzeitFuerEingabe(filter.von, zone)}
              onChange={(ereignis) =>
                steuerung.setzeFreiesFenster(
                  zeitpunktAusWanduhrzeit(ereignis.target.value, zone),
                  filter.bis,
                )
              }
            />
          </div>
          <div className="flex flex-col gap-0.5">
            <Label htmlFor={bisId} className="text-beiwerk">
              {texte.nachrichten.zeitfenster.bis}
            </Label>
            <Input
              id={bisId}
              type="datetime-local"
              className="h-bedienelement w-auto"
              value={wanduhrzeitFuerEingabe(filter.bis, zone)}
              onChange={(ereignis) =>
                steuerung.setzeFreiesFenster(
                  filter.von,
                  zeitpunktAusWanduhrzeit(ereignis.target.value, zone),
                )
              }
            />
          </div>
        </div>
      ) : null}
    </div>
  );
}

/**
 * Der Statusfilter arbeitet über die **Einordnungen** aus `MessageStatusKind`,
 * nicht über Rohwerte: Ein Nutzer sucht „Fehler", nicht `ERROR_DUPLICATE`. Die
 * Beschriftungen kommen aus den Sprachdateien, die Menge aus dem Backend.
 */
function StatusFilter({
  gewaehlt,
  aufAuswahl,
}: {
  gewaehlt: Statusart[];
  aufAuswahl: (status: Statusart[]) => void;
}) {
  const texte = useTexte();

  const beschriftung =
    gewaehlt.length === 0
      ? texte.nachrichten.statusfilter.alle
      : einsetzen(texte.nachrichten.statusfilter.gewaehlt, { anzahl: gewaehlt.length });

  return (
    <div className="flex items-center gap-1">
      <Popover>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            className="min-h-bedienelement w-full justify-between gap-2 sm:w-52"
          >
            <span className="truncate">
              <span className="text-muted-foreground">
                {texte.nachrichten.statusfilter.bezeichnung}:{" "}
              </span>
              {beschriftung}
            </span>
            <ChevronsUpDown aria-hidden="true" className="size-3.5 shrink-0 opacity-60" />
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-64">
          <ul className="flex flex-col">
            {STATUSARTEN.map((art) => {
              const kennung = `status-${art}`;
              const an = gewaehlt.includes(art);
              return (
                <li key={art}>
                  <div className="hover:bg-muted min-h-beruehrung flex items-center gap-2 rounded-sm px-1">
                    <Checkbox
                      id={kennung}
                      checked={an}
                      onCheckedChange={() =>
                        aufAuswahl(
                          an ? gewaehlt.filter((wert) => wert !== art) : [...gewaehlt, art],
                        )
                      }
                    />
                    <Label htmlFor={kennung} className="flex-1 cursor-pointer py-1 font-normal">
                      {texte.nachrichten.status[art]}
                    </Label>
                  </div>
                </li>
              );
            })}
          </ul>
        </PopoverContent>
      </Popover>

      {gewaehlt.length > 0 ? (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="min-h-bedienelement shrink-0"
          onClick={() => aufAuswahl([])}
          title={texte.nachrichten.statusfilter.zuruecksetzen}
        >
          <X aria-hidden="true" />
          <span className="sr-only">{texte.nachrichten.statusfilter.zuruecksetzen}</span>
        </Button>
      ) : null}
    </div>
  );
}

/**
 * Das Suchfeld — **entprellt und ab drei Zeichen**.
 *
 * Der Freitextfilter ist der teuerste Fall des Endpunkts (Messungen L7c und L11),
 * und seine Kosten wachsen mit dem Zeitfenster. Bei jedem Tastendruck zu suchen
 * hieße, dieselbe teure Abfrage fünfmal für einen Begriff zu stellen, den der
 * Nutzer noch gar nicht fertig getippt hat.
 *
 * **Zu kurz ist kein Fehler, sondern ein Zwischenzustand.** Der Nutzer läuft beim
 * Tippen zwangsläufig hindurch; ihm nach dem zweiten Zeichen eine Fehlermeldung
 * hinzustellen, wäre eine Belehrung für etwas, das er gerade tut. Der Hinweis
 * sagt stattdessen, was noch fehlt.
 */
function Suchfeld({
  wert,
  aufSuche,
  fehler,
}: {
  wert: string;
  aufSuche: (suche: string) => void;
  fehler?: string;
}) {
  const texte = useTexte();
  const kennung = useId();
  const [eingabe, setEingabe] = useState(wert);

  // Der äußere Wert gewinnt, wenn er sich ohne Zutun dieses Feldes ändert — etwa
  // weil jemand einen Link geöffnet oder den Zurück-Knopf benutzt hat. Angepasst
  // während des Renderns: Ein Effekt zeigte für einen Durchgang den alten Text.
  const [letzterWert, setLetzterWert] = useState(wert);
  if (letzterWert !== wert) {
    setLetzterWert(wert);
    setEingabe(wert);
  }

  useEffect(() => {
    if (eingabe === wert) {
      return;
    }
    const uhr = setTimeout(() => aufSuche(eingabe), SUCHE_ENTPRELLUNG_MS);
    return () => clearTimeout(uhr);
  }, [eingabe, wert, aufSuche]);

  const fehlendeZeichen = SUCHE_MINDESTLAENGE - eingabe.trim().length;
  const hinweis =
    fehler ??
    (eingabe.trim().length > 0 && fehlendeZeichen > 0
      ? einsetzen(texte.nachrichten.suche.zuKurz, { anzahl: fehlendeZeichen })
      : undefined);

  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <div className="flex items-center gap-1">
        <Label htmlFor={kennung} className="sr-only">
          {texte.nachrichten.suche.bezeichnung}
        </Label>
        <div className="relative">
          <Search
            aria-hidden="true"
            className="text-muted-foreground pointer-events-none absolute top-1/2 left-2 size-3.5 -translate-y-1/2"
          />
          <Input
            id={kennung}
            type="search"
            value={eingabe}
            onChange={(ereignis) => setEingabe(ereignis.target.value)}
            placeholder={texte.nachrichten.suche.platzhalter}
            aria-describedby={hinweis === undefined ? undefined : `${kennung}-hinweis`}
            className="h-bedienelement w-full pl-7 sm:w-64"
          />
        </div>
        {eingabe !== "" ? (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="min-h-bedienelement shrink-0"
            onClick={() => {
              setEingabe("");
              aufSuche("");
            }}
            title={texte.nachrichten.suche.leeren}
          >
            <X aria-hidden="true" />
            <span className="sr-only">{texte.nachrichten.suche.leeren}</span>
          </Button>
        ) : null}
      </div>
      {hinweis === undefined ? null : (
        <p id={`${kennung}-hinweis`} className="text-muted-foreground text-beiwerk">
          {hinweis}
        </p>
      )}
    </div>
  );
}

/**
 * Zwischenschritte sind ausgeblendet — **und das steht sichtbar da.**
 *
 * 34,38 Prozent aller Zeilen sind `SPLITTED` oder `MERGED`. Eine Liste, die zu
 * einem Drittel aus Begriffen besteht, die der Zielnutzer nicht kennt, kostet
 * beim ersten Kontakt Vertrauen — deshalb bleiben sie draußen. Aber wer ein
 * Drittel weglässt, muss es sagen: Sonst sucht jemand eine Nachricht, die es gibt
 * und die er nie zu sehen bekommt.
 *
 * Der Chip erklärt in einem Halbsatz, was fehlt, und schaltet es ein.
 */
function ZwischenschritteChip({
  eingeblendet,
  aufUmschalten,
}: {
  eingeblendet: boolean;
  aufUmschalten: (eingeblendet: boolean) => void;
}) {
  const texte = useTexte();
  const Zeichen = eingeblendet ? Eye : EyeOff;

  return (
    <button
      type="button"
      onClick={() => aufUmschalten(!eingeblendet)}
      title={
        eingeblendet
          ? texte.nachrichten.zwischenschritte.ausblenden
          : texte.nachrichten.zwischenschritte.einblenden
      }
      className="border-border bg-card hover:bg-muted focus-visible:ring-ring text-beiwerk min-h-bedienelement flex w-fit max-w-full items-center gap-2 rounded-md border px-2.5 text-left focus-visible:ring-2 focus-visible:outline-none"
    >
      <Zeichen aria-hidden="true" className="size-3.5 shrink-0 opacity-70" />
      <span className="font-medium">
        {eingeblendet
          ? texte.nachrichten.zwischenschritte.chipAn
          : texte.nachrichten.zwischenschritte.chipAus}
      </span>
      <span className="text-muted-foreground hidden truncate sm:inline">
        {texte.nachrichten.zwischenschritte.erklaerung}
      </span>
    </button>
  );
}
