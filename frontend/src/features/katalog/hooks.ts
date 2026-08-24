"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useQueryStates } from "nuqs";
import { useCallback } from "react";

import {
  KATALOG_SCHLUESSEL,
  holeKatalogzeilen,
  holePartner,
  massenzuordnung,
  starteVorschlagslauf,
  zuordne,
  type Katalogzeile,
  type MassenzuordnungAnfrage,
  type ZuordnenAnfrage,
} from "./api";
import { KATALOG_PARAMETER, type Katalogfilter } from "./filter";
import { mitAktualisierterZeile, mitPartner } from "./zuordnung";

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

/**
 * Setzt Partner und Richtung **einer** Zeile (E19).
 *
 * **Die Antwort aktualisiert den Zwischenspeicher, die Liste wird nicht neu
 * geholt.** 733 Zeilen wegen einer einzigen Änderung noch einmal zu ziehen wäre
 * teuer und nähme dem Nutzer obendrein seine Seitenposition — mitten in einer
 * Arbeit, die genau aus vielen solchen Änderungen besteht.
 *
 * Gesetzt wird mit `setQueriesData` über den **Präfix**, also über beide
 * Filterstellungen zugleich. `invalidateQueries` wäre hier falsch: Es markierte
 * die Daten nur als veraltet und zeigte sie weiter an, bis die neue Antwort da
 * ist — für eine Zeile, deren neuen Stand wir bereits in der Hand halten.
 *
 * **Die Partnerliste wächst mit.** Ein neu getippter Name steht ab der nächsten
 * Zeile in der Auswahl, ohne dass die Liste ein zweites Mal geholt würde.
 *
 * `retry` steht auf `false` — so ist es für alle Mutationen konfiguriert
 * (`lib/query-client.ts`). Ein zweites `PUT` wäre nur dann harmlos, wenn das
 * erste wirklich nicht angekommen ist, und das weiß der Browser nicht.
 */
export function useZuordnen() {
  const speicher = useQueryClient();

  return useMutation({
    mutationFn: ({ processId, anfrage }: { processId: string; anfrage: ZuordnenAnfrage }) =>
      zuordne(processId, anfrage),
    onSuccess: (zeile) => {
      speicher.setQueriesData<Katalogzeile[]>(
        { queryKey: KATALOG_SCHLUESSEL.prozesseBereich },
        (alt) => (alt === undefined ? alt : mitAktualisierterZeile(alt, zeile)),
      );
      speicher.setQueryData<string[]>(KATALOG_SCHLUESSEL.partner, (alt) =>
        alt === undefined ? alt : mitPartner(alt, zeile.partner),
      );
    },
  });
}

/**
 * Der Lauf: Heuristik **und** Bestandserhebung in einem Knopfdruck (E13, E14).
 *
 * **Danach wird die Liste neu geholt und nicht gesetzt** — anders als bei
 * {@link useZuordnen}, und aus dem umgekehrten Grund: Dort halten wir die
 * geänderte Zeile in der Hand, hier hat sich die Liste **in der Breite**
 * geändert. Der Lauf legt fehlende Zeilen an, frischt Vorschläge auf und
 * schreibt `traegt_nachrichten` auf jede Zeile des Mandanten; welche das im
 * Einzelnen sind, sagt die Antwort nicht. Aus acht Zahlen lässt sich keine
 * Liste rekonstruieren.
 *
 * Die Partnervorschläge werden mit invalidiert: Ein Lauf kann neue Zeilen mit
 * Vorschlägen angelegt haben, und die speisen die Auswahl (E2).
 */
export function useVorschlagslauf() {
  const speicher = useQueryClient();

  return useMutation({
    mutationFn: starteVorschlagslauf,
    onSuccess: () => {
      void speicher.invalidateQueries({ queryKey: KATALOG_SCHLUESSEL.prozesseBereich });
      void speicher.invalidateQueries({ queryKey: KATALOG_SCHLUESSEL.partner });
    },
  });
}

/**
 * Die Massenzuordnung — **zwei Modi, ein Aufruf** (E11, E12).
 *
 * **Nur `AUSFUEHREN` rührt den Zwischenspeicher an.** Eine Vorschau hat nichts
 * geschrieben; sie zu invalidieren hieße, 733 Zeilen für eine Frage neu zu
 * holen, die niemand beantwortet bekommen wollte.
 *
 * Nach dem Ausführen wird **invalidiert und nicht gesetzt** — aus demselben
 * Grund wie beim Lauf: Die Antwort nennt Zahlen, keine Zeilen. Bis zu 226
 * Prozesse haben sich geändert, und welche das im Einzelnen sind, sagt sie
 * nicht.
 */
export function useMassenzuordnung() {
  const speicher = useQueryClient();

  return useMutation({
    mutationFn: (anfrage: MassenzuordnungAnfrage) => massenzuordnung(anfrage),
    onSuccess: (antwort) => {
      if (antwort.modus !== "AUSFUEHREN") {
        return;
      }
      void speicher.invalidateQueries({ queryKey: KATALOG_SCHLUESSEL.prozesseBereich });
      void speicher.invalidateQueries({ queryKey: KATALOG_SCHLUESSEL.partner });
    },
  });
}
