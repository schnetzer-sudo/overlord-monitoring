"use client";

import { useQuery } from "@tanstack/react-query";
import { useQueryStates } from "nuqs";
import { useCallback } from "react";

import type { Rollupzeitraum } from "@/lib/rollupzeitraum";

import { DASHBOARD_SCHLUESSEL, holeDashboard, type Dashboard, type Verteilungssicht } from "./api";
import { DASHBOARD_PARAMETER, mitSicht, type Dashboardzustand } from "./filter";

/**
 * Der URL-Zustand der Landingpage, an `nuqs` gebunden.
 *
 * **`history: "replace"`** wie bei jeder Filterleiste: Ein Zeitraum, den man
 * verstellt, ist keine Station, zu der man zurückgeht — und drei Klicks durch
 * die Zeiträume machten den Zurück-Knopf sonst zu einem Zeitraum-Knopf.
 *
 * **`shallow: true` steht ausdrücklich da**, obwohl es die Voreinstellung von
 * `nuqs` ist (16.09.2026). Ein Sichtwechsel schreibt die URL und darf dabei
 * keinen Server-Roundtrip auslösen — die Antwort mit beiden Sichten liegt schon
 * im Zwischenspeicher (E‑161). Mit `shallow: false` ließe der App-Router die
 * Seite beim Server neu rendern; die Zeile sagt, dass das hier eine Anforderung
 * ist und kein Zufall der Voreinstellung.
 *
 * Die Parser stehen in `filter.ts` und sind frei von React; hier steht nur die
 * Bindung.
 */
export function useDashboardzustand() {
  const [zustand, setzeZustand] = useQueryStates(DASHBOARD_PARAMETER, {
    history: "replace",
    shallow: true,
  });

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
      (zeitraum: Rollupzeitraum) => void setzeZustand({ zeitraum }),
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
 * Die ganze Landingpage in **einem** Aufruf — **mit beiden Sichten der
 * Verteilung**.
 *
 * **Der Schlüssel trägt nur den Zeitraum** (seit dem 16.09.2026, E‑161). Ein
 * Sichtwechsel ändert die URL, aber nicht den Schlüssel: Die Abfrage behält ihre
 * Daten, steht nie auf `isPending`, und kein Block fällt in den Ladezustand. Bis
 * dahin trug der Schlüssel auch die Sicht — der Wechsel war ein neuer Schlüssel
 * ohne Daten, und die Ansicht ersetzte deshalb **jeden** Block durch den
 * Ladezustand und baute ihn nach der Antwort neu auf, samt der Aufbaubewegung
 * des Verlaufs (`docs/dashboard-frontend.md` §2).
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
    queryKey: DASHBOARD_SCHLUESSEL.landingpage(zustand.zeitraum),
    queryFn: () => holeDashboard(zustand.zeitraum),
  });
}
