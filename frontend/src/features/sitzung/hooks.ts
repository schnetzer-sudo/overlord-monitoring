"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { ROUTEN } from "@/lib/routen";
import {
  nachAbmeldung,
  nachMandantenwechsel,
  zielNachMandantenwechsel,
} from "@/lib/zwischenspeicher";

import {
  SITZUNG_SCHLUESSEL,
  aenderePasswort,
  holeMandanten,
  holeSelbstauskunft,
  meldeAb,
  meldeAn,
  wechsleMandant,
  type Selbstauskunft,
} from "./api";

/**
 * Der Zustand der Sitzung. Bewusst ohne `staleTime`: Alles andere darf kurz
 * veraltet sein, die Frage „wer bin ich und welchen Mandanten sehe ich gerade"
 * nicht.
 *
 * Kein zweiter Versuch: Ein `401` heißt „nicht angemeldet" und ändert sich beim
 * Wiederholen nicht. Die Umleitung auf die Anmeldung übernimmt der QueryClient.
 */
export function useSelbstauskunft() {
  return useQuery({
    queryKey: SITZUNG_SCHLUESSEL.selbstauskunft,
    queryFn: holeSelbstauskunft,
    staleTime: 0,
    retry: false,
  });
}

export function useAnmelden() {
  const speicher = useQueryClient();
  return useMutation({
    mutationFn: meldeAn,
    onSuccess: (auskunft: Selbstauskunft) => {
      // Vor dem Setzen leeren: Am selben Gerät kann vorher jemand anders
      // angemeldet gewesen sein.
      speicher.clear();
      speicher.setQueryData(SITZUNG_SCHLUESSEL.selbstauskunft, auskunft);
    },
  });
}

/**
 * Abmelden. Der Zwischenspeicher wird geleert, **auch wenn der Aufruf
 * fehlschlägt** — der Nutzer wollte gehen, und die Daten des vorherigen Nutzers
 * dürfen dem nächsten am selben Gerät nicht kurz aufblitzen.
 *
 * Danach eine harte Navigation statt `router.replace`: Sie wirft auch den
 * Zustand im Speicher weg, den ein Router-Wechsel stehen ließe.
 */
export function useAbmelden() {
  const speicher = useQueryClient();
  return useMutation({
    mutationFn: meldeAb,
    onSettled: () => {
      nachAbmeldung(speicher, () => window.location.replace(ROUTEN.anmeldung));
    },
  });
}

export function usePasswortAendern() {
  const speicher = useQueryClient();
  return useMutation({
    mutationFn: aenderePasswort,
    onSuccess: (auskunft: Selbstauskunft) => {
      speicher.setQueryData(SITZUNG_SCHLUESSEL.selbstauskunft, auskunft);
    },
  });
}

export function useMandanten() {
  return useQuery({
    queryKey: SITZUNG_SCHLUESSEL.mandanten,
    queryFn: holeMandanten,
  });
}

/**
 * Mandantenwechsel. Der Zwischenspeicher wird **geleert, nicht invalidiert** —
 * Begründung in `lib/zwischenspeicher.ts`.
 *
 * Und es wird auf ein Ziel **ohne Filter in der URL** gewechselt: Der
 * Prozessfilter der Nachrichtenliste trägt `ProcessID`s, die dem alten Mandanten
 * gehören und für den neuen bedeutungslos sind. Bliebe er stehen, sähe der Nutzer
 * eine dauerhaft leere Liste, deren Ursache niemand sieht.
 */
export function useMandantWechseln() {
  const speicher = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: wechsleMandant,
    onSuccess: (auskunft: Selbstauskunft) => {
      nachMandantenwechsel(speicher, () => {
        speicher.setQueryData(SITZUNG_SCHLUESSEL.selbstauskunft, auskunft);
        router.replace(zielNachMandantenwechsel(ROUTEN.startseite));
      });
    },
  });
}
