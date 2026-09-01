"use client";

import { useQuery } from "@tanstack/react-query";
import { useQueryStates } from "nuqs";
import { useCallback } from "react";

import {
  DASHBOARD_SCHLUESSEL,
  holeDashboard,
  type Dashboard,
  type Dashboardzeitraum,
  type Verteilungssicht,
} from "./api";
import { DASHBOARD_PARAMETER, mitSicht, type Dashboardzustand } from "./filter";

/**
 * Der URL-Zustand der Landingpage, an `nuqs` gebunden.
 *
 * **`history: "replace"`** wie bei jeder Filterleiste: Ein Zeitraum, den man
 * verstellt, ist keine Station, zu der man zurückgeht — und drei Klicks durch
 * die Zeiträume machten den Zurück-Knopf sonst zu einem Zeitraum-Knopf.
 *
 * Die Parser stehen in `filter.ts` und sind frei von React; hier steht nur die
 * Bindung.
 */
export function useDashboardzustand() {
  const [zustand, setzeZustand] = useQueryStates(DASHBOARD_PARAMETER, { history: "replace" });

  return {
    zustand: zustand as Dashboardzustand,
    /**
     * **Der gewählte Wert wird immer geschrieben, auch wenn er dem entspricht,
     * was der Endpunkt ohnehin genommen hätte.** Ein Klick ist eine Absicht, und
     * eine geteilte URL soll sie tragen — beim Empfänger könnte der Endpunkt ein
     * anderes Paar wählen, denn er wählt nach dem *Mandanten*
     * (`docs/dashboard.md` §3).
     */
    setzeZeitraum: useCallback(
      (zeitraum: Dashboardzeitraum) => void setzeZustand({ zeitraum }),
      [setzeZustand],
    ),
    /**
     * **Die Vorgabe verschwindet aus der URL**, die andere Sicht steht darin —
     * die Regel steht als reine Funktion daneben ({@link mitSicht}) und nicht
     * als Bedingung hier.
     */
    setzeSicht: useCallback(
      (sicht: Verteilungssicht) => void setzeZustand({ verteilung: mitSicht(sicht) }),
      [setzeZustand],
    ),
  };
}

/**
 * Die ganze Landingpage in **einem** Aufruf.
 *
 * **Kein Block lädt nach**, auch die Verteilung nicht: Ein Sichtwechsel ändert
 * den Abfrageschlüssel, und damit ist es ein neuer Aufruf derselben Adresse mit
 * anderem Parameter — kein Teilnachladen. Wer zurückschaltet, bekommt die
 * vorherige Antwort aus dem Zwischenspeicher, ohne dass eine zweite Anfrage
 * hinausgeht.
 *
 * **Der Schlüssel trägt genau das, was in der Anfrage steht** — `null` und
 * nicht das vom Endpunkt gewählte Paar. Schriebe die Ansicht das gewählte Paar
 * zurück, entstünde beim ersten Rendern ein zweiter Schlüssel und damit eine
 * zweite Anfrage für dieselbe Antwort.
 *
 * **Beim Mandantenwechsel wird der gesamte Zwischenspeicher geleert**, nicht
 * invalidiert (`lib/zwischenspeicher.ts`); eine eigene Invalidierung braucht es
 * hier nicht, und der Mandant steht aus demselben Grund in keinem Schlüssel.
 */
export function useDashboard(zustand: Dashboardzustand) {
  return useQuery<Dashboard>({
    queryKey: DASHBOARD_SCHLUESSEL.landingpage(zustand.zeitraum, zustand.verteilung),
    queryFn: () => holeDashboard(zustand.zeitraum, zustand.verteilung),
  });
}
