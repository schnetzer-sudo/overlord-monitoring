"use client";

import { useQuery } from "@tanstack/react-query";
import { useQueryStates } from "nuqs";
import { useCallback } from "react";

import { KATALOG_SCHLUESSEL, holeKatalogzeilen, holePartner, type Katalogzeile } from "./api";
import { KATALOG_PARAMETER, type Katalogfilter } from "./filter";

/**
 * Der Filterzustand der Pflegeliste, an die URL gebunden.
 *
 * **`history: "replace"`** wie bei jeder Filterleiste: Ein Filter, den man
 * verstellt, ist keine Station, zu der man zurückgeht. Ein Verlaufseintrag je
 * Häkchen machte den Zurück-Knopf unbrauchbar.
 *
 * Die Parser stehen in `filter.ts` und sind frei von React; hier steht nur die
 * Bindung.
 */
export function useKatalogfilter() {
  const [filter, setzeFilter] = useQueryStates(KATALOG_PARAMETER, { history: "replace" });

  return {
    filter: filter as Katalogfilter,
    setzeNurOffene: useCallback(
      (nurOffene: boolean) => void setzeFilter({ nurOffene }),
      [setzeFilter],
    ),
    setzeNurMitNachrichten: useCallback(
      (nurMitNachrichten: boolean) => void setzeFilter({ nurMitNachrichten }),
      [setzeFilter],
    ),
  };
}

/**
 * Die Pflegeliste. **Ein Fetch, keine Paginierung** (E8).
 *
 * Der Schlüssel trägt `nurOffene`, weil das ein Anfrageparameter ist. Wer den
 * Haken setzt und wieder herausnimmt, bekommt die vorherige Antwort aus dem
 * Zwischenspeicher zurück, statt sie noch einmal zu holen.
 *
 * **Beim Mandantenwechsel wird der gesamte Zwischenspeicher geleert**, nicht
 * invalidiert (`lib/zwischenspeicher.ts`); eine eigene Invalidierung braucht es
 * hier deshalb nicht — und der Mandant steht aus demselben Grund in keinem
 * Schlüssel.
 */
export function useKatalogzeilen(nurOffene: boolean) {
  return useQuery<Katalogzeile[]>({
    queryKey: KATALOG_SCHLUESSEL.prozesse(nurOffene),
    queryFn: () => holeKatalogzeilen(nurOffene),
  });
}

/**
 * Die Partnervorschläge. **Länger gehalten als die Liste** — sie sind
 * Stammdaten des Mandanten und ändern sich nur, wenn jemand kuratiert.
 *
 * Genau dann werden sie auch aufgefrischt: Die Zuordnung einer Zeile setzt sie
 * neu (siehe `useZuordnen`). Ein neuer Partnername steht damit sofort in der
 * Auswahl, ohne dass die Liste ein zweites Mal geholt würde.
 */
const PARTNER_HALTBARKEIT = 15 * 60 * 1000;

export function usePartner() {
  return useQuery<string[]>({
    queryKey: KATALOG_SCHLUESSEL.partner,
    queryFn: holePartner,
    staleTime: PARTNER_HALTBARKEIT,
    gcTime: PARTNER_HALTBARKEIT,
  });
}
