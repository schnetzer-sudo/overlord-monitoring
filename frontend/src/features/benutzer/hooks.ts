"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { MANDANTEN_SCHLUESSEL, holeMandanten, type Mandant } from "@/lib/mandanten";

import {
  BENUTZER_SCHLUESSEL,
  holeNutzer,
  legeKontoAn,
  setzeAktiv,
  setzeMandanten,
  setzePasswort,
  setzeRolle,
  setzeSperre,
  type Anlegedaten,
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

/**
 * Wie lange die Mandantenliste als frisch gilt — fünfzehn Minuten.
 *
 * Dieselbe Zahl und derselbe Grund wie bei den Partnervorschlägen der
 * Katalogpflege: Stammdaten, die sich während einer Pflegesitzung nicht ändern.
 */
const MANDANTEN_HALTBARKEIT = 15 * 60 * 1000;

/**
 * Die wählbaren Mandanten — **eine Abfrage für beide Verwender im Feature.**
 *
 * `GET /api/mandanten` steht seit Schritt 3 und wird aus `lib/mandanten.ts`
 * wiederverwendet, nicht nachgebaut. Gelesen wird sie hier zweimal: von der
 * Mengenpflege an der Zeile (E4) und seit 9c von der Anlegemaske. Beide
 * brauchen dieselbe Antwort unter demselben Schlüssel — und damit auch dieselbe
 * Haltbarkeit.
 *
 * ## Warum sie länger gehalten wird als die Voreinstellung
 *
 * **Der Grund ist gemessen und nicht vorgesorgt** (Sichtprüfung 26.08.2026):
 * Nach jedem Speichern wechselt der `key` der Mandantenauswahl — so setzt sich
 * ihr Entwurf zurück —, React hängt sie neu ein, und `useQuery` holt beim
 * Einhängen nach, sobald die Antwort älter als `staleTime` ist. Mit den dreißig
 * Sekunden aus `lib/query-client.ts` ging deshalb **nach jeder Mengenersetzung**
 * ein zusätzliches `GET /api/mandanten` hinaus — auf einer Seite, deren ganzer
 * Punkt ist, dass die Antwort den Zwischenspeicher setzt, statt nachzuholen.
 *
 * Es sind Stammdaten aus `GlassfishDB.Mandant`, zehn Zeilen, und sie ändern sich
 * nicht, während jemand ein Konto pflegt — dieselbe Zahl und derselbe Grund wie
 * bei den Partnervorschlägen der Katalogpflege (`features/katalog/hooks.ts`).
 *
 * *Seit 9c steht die Abfrage hier statt in `mandanten-auswahl.tsx`: Zwei
 * Bausteine mit je einer eigenen Fassung derselben Haltbarkeit liefen
 * auseinander, und zwar an der Stelle, an der es niemandem auffiele.*
 */
export function useMandanten() {
  return useQuery<Mandant[]>({
    queryKey: MANDANTEN_SCHLUESSEL,
    queryFn: holeMandanten,
    staleTime: MANDANTEN_HALTBARKEIT,
    gcTime: MANDANTEN_HALTBARKEIT,
  });
}

/**
 * **Anlegen — eine eigene Mutation und nicht die der fünf** (9c).
 *
 * Sie sieht daneben aus wie eine sechste, und sie ist es an drei Stellen nicht:
 *
 * 1. **Eine andere Antwort.** `POST /api/admin/users` liefert vier Felder, die
 *    Zeile hat neun. Sie taugt nicht als Zeile.
 * 2. **Ein anderes Zwischenspeicherverhalten** (E24, siehe unten).
 * 3. **Kein Selbstschutz und keine Vorwarnung.** Anlegen trifft nie das eigene
 *    Konto und verwirft keine Sitzung; E19 hier mitzubenutzen wäre eine Regel
 *    ohne Fall — und dieselbe Strecke für zwei Dinge, von denen eines sie nicht
 *    braucht, ist der Weg, auf dem die Regel für das andere später verloren geht.
 *
 * ## Die Liste wird neu geholt, der Zwischenspeicher wird nicht gesetzt (E24)
 *
 * **Das ist die ausdrückliche Ausnahme vom Muster der Seite.** Die fünf
 * schreibenden Vorgänge setzen die Zeile aus ihrer Antwort; hier ginge das nur,
 * indem `active`, `mustChangePassword`, `locked`, `lastLogin` und `tenants` aus
 * dem *dokumentierten Verhalten* ergänzt würden — also indem in den
 * Zwischenspeicher geschrieben wird, was der Server nicht gesagt hat. Das wäre
 * die Art Vermutung, die genau dann falsch ist, wenn sich das Backend einmal
 * ändert, und sie stünde dann in der Liste, ohne dass jemand nachsieht.
 *
 * Die Ausnahme kostet **einen zweiten Aufruf über rund dreißig Konten** — 17,87
 * ms (M82) — und ist damit bezahlt.
 *
 * `invalidateQueries` und nicht `refetchQueries`: Die Liste ist eingehängt und
 * wird dadurch sofort nachgeholt; bis die Antwort da ist, steht die bisherige
 * da. Das ist hier richtig — anders als bei den fünf gibt es keine Zeile, die
 * währenddessen falsch aussähe.
 *
 * **Verworfen ist, den Vertrag zu ändern**, damit `POST` die Zeile liefert (E4).
 */
export function useAnlegen() {
  const speicher = useQueryClient();

  return useMutation({
    mutationFn: (daten: Anlegedaten) => legeKontoAn(daten),
    onSuccess: () => {
      void speicher.invalidateQueries({ queryKey: BENUTZER_SCHLUESSEL.liste });
    },
  });
}
