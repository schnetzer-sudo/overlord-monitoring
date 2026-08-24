"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  BENUTZER_SCHLUESSEL,
  holeNutzer,
  setzeAktiv,
  setzeMandanten,
  setzePasswort,
  setzeRolle,
  setzeSperre,
  type Nutzerzeile,
} from "./api";
import type { Vorgang } from "./selbstschutz";
import { mitAktualisierterZeile } from "./zeilen";

/**
 * Die Kontenliste. **Ein Fetch, keine Paginierung** (E16).
 *
 * **Ohne eigenes `staleTime`**, also mit den 30 Sekunden aus
 * `lib/query-client.ts`. Das ist hier die richtige Voreinstellung und nicht die
 * bequeme: Die Liste ändert sich fast nur durch die fünf Vorgänge dieser Seite
 * selbst, und deren Antwort **ist** die geänderte Zeile — sie wird gesetzt und
 * nicht nachgeholt.
 *
 * **Kein Mandant im Schlüssel** (E2): Die Liste ist mandantenfrei. Der
 * Mandantenwechsel leert den Zwischenspeicher trotzdem vollständig
 * (`lib/zwischenspeicher.ts`) — das kostet hier einen Fetch und ist der Preis
 * dafür, dass die Regel ohne Ausnahme gilt.
 */
export function useNutzer() {
  return useQuery<Nutzerzeile[]>({
    queryKey: BENUTZER_SCHLUESSEL.liste,
    queryFn: holeNutzer,
  });
}

/**
 * **Ein Vorgang, ein Aufruf, eine Mutation** — und bewusst *eine* für alle fünf.
 *
 * ## Warum nicht fünf Hooks
 *
 * Weil die Zeile genau einen Zustand hat. Fünf Mutationen nebeneinander hätten
 * fünf `isPending` und fünf `error`, und das Formular müsste sie
 * zusammenrechnen, um zu wissen, ob gerade etwas läuft. Mit einer ist die
 * Antwort auf „läuft etwas an dieser Zeile" eine Variable — und was lief, steht
 * in {@link Vorgang} und lässt sich neben der richtigen Schaltfläche melden.
 *
 * Der zweite Grund wiegt schwerer und ist derselbe wie im Backend: **E19 ist
 * eine Regel und keine Aufzählung.** Läuft jeder Vorgang durch dieselbe Stelle,
 * kann die Vorwarnung nicht an einem von fünf Aufrufwegen vergessen werden.
 *
 * ## Die Antwort wird gesetzt, die Liste nicht neu geholt
 *
 * Jeder der fünf antwortet mit der geänderten Zeile — genau der Zeile, deren
 * neuen Stand wir dann in der Hand halten. `invalidateQueries` wäre hier falsch:
 * Es markierte die Daten nur als veraltet und zeigte sie weiter an, bis die
 * neue Antwort da ist.
 *
 * **Und es ginge auch nicht ohne Weiteres.** Trifft der Vorgang das eigene
 * Konto, verwirft er dabei die eigene Sitzung (E5) — eine nachgeschobene
 * Abfrage liefe in ein `401` und schickte den Nutzer auf die Anmeldung, bevor er
 * die Antwort gesehen hat, die er gerade ausgelöst hat.
 *
 * `retry` steht auf `false`, so ist es für alle Mutationen konfiguriert
 * (`lib/query-client.ts`). Ein zweites `PUT` wäre nur dann harmlos, wenn das
 * erste wirklich nicht angekommen ist, und das weiß der Browser nicht.
 */
export function useVorgang() {
  const speicher = useQueryClient();

  return useMutation({
    mutationFn: ({ id, vorgang }: { id: number; vorgang: Vorgang }) => {
      switch (vorgang.art) {
        case "sperre":
          return setzeSperre(id, vorgang.gesperrt);
        case "aktiv":
          return setzeAktiv(id, vorgang.aktiv);
        case "rolle":
          return setzeRolle(id, vorgang.rolle);
        case "mandanten":
          return setzeMandanten(id, vorgang.mandanten);
        case "passwort":
          return setzePasswort(id, vorgang.passwort);
      }
    },
    onSuccess: (zeile) => {
      speicher.setQueryData<Nutzerzeile[]>(BENUTZER_SCHLUESSEL.liste, (alt) =>
        alt === undefined ? alt : mitAktualisierterZeile(alt, zeile),
      );
    },
  });
}
