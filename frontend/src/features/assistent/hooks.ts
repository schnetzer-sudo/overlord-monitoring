"use client";

import { useEffect, useReducer, useRef } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { ProblemFehler } from "@/lib/http";

import {
  ASSISTENT_SCHLUESSEL,
  beendeGespraech,
  eroeffneGespraech,
  holeStand,
  legeAuftragAn,
  neueAnfrageId,
  prueffehlerAus,
  pruefeAuftrag,
  sendeNachricht,
  type Vorschlag,
  type Werte,
} from "./api";
import {
  darfAnlegen,
  gespraechSchritt,
  hinweisAus,
  karteVon,
  kannSenden,
  neuesGespraech,
  pruefanfrage,
  type Gespraech,
  type Pruefstand,
} from "./gespraech";

/**
 * Ob der Assistent da ist und mit welchen Typen (E‑1068). Ohne eigene Regel:
 * Ein Fehler heißt hier dasselbe wie `verfuegbar: false` — das Symbol fehlt.
 */
export function useAssistentStand() {
  return useQuery({ queryKey: ASSISTENT_SCHLUESSEL.stand, queryFn: holeStand });
}

/** Geprüft wird so lange nach der letzten Änderung auf der Karte (F5). Gewählt, nicht gemessen. */
export const PRUEFVERZOEGERUNG_MS = 400;

export type Kartensteuerung = {
  aufklappen: (karteId: string, offen: boolean) => void;
  aendern: (karteId: string) => void;
  feld: (karteId: string, entwurf: Werte[]) => void;
  erneutPruefen: (karteId: string) => void;
  uebernehmen: (karteId: string) => void;
  verwerfen: (karteId: string) => void;
  anlegen: (karteId: string) => void;
};

export type Gespraechssteuerung = {
  zustand: Gespraech;
  setzeEntwurf: (text: string) => void;
  senden: () => void;
  neu: () => void;
  karte: Kartensteuerung;
};

/**
 * **Das Gespräch samt Karten**, gehalten in der Kopfzeile (F3, E‑1076).
 *
 * Die Mutationen stehen hier und nicht in der Chatbox: Deren Inhalt hängt beim
 * Schließen aus, und eine Antwort, die dann ankommt, fände sonst niemanden mehr.
 * Dasselbe gilt für die verzögerte Prüfung. Mutationen wiederholen nicht
 * (`lib/query-client.ts`), und ein `401` leitet dort um, auch für diese.
 *
 * **Die Generation** zählt „Neues Gespräch“: Eine Antwort, die zu einem schon
 * beendeten Gespräch gehört, kommt nicht in das neue. Ein Gespräch, das erst
 * eröffnet wurde, als der Kunde schon neu begonnen hatte, wird gleich wieder
 * beendet.
 */
export function useGespraech(): Gespraechssteuerung {
  const [zustand, schritt] = useReducer(gespraechSchritt, undefined, neuesGespraech);

  const eroeffnen = useMutation({ mutationFn: eroeffneGespraech });
  const nachricht = useMutation({
    mutationFn: ({ gespraechId, text }: { gespraechId: string; text: string }) =>
      sendeNachricht(gespraechId, text),
  });
  const beenden = useMutation({ mutationFn: beendeGespraech });
  const pruefung = useMutation({ mutationFn: pruefeAuftrag });
  const anlage = useMutation({ mutationFn: legeAuftragAn });

  const generation = useRef(0);
  const zeitgeber = useRef(new Map<string, ReturnType<typeof setTimeout>>());

  useEffect(() => {
    const laufend = zeitgeber.current;
    return () => {
      for (const kennung of laufend.values()) {
        clearTimeout(kennung);
      }
      laufend.clear();
    };
  }, []);

  function stoppePruefung(karteId: string) {
    const alt = zeitgeber.current.get(karteId);
    if (alt !== undefined) {
      clearTimeout(alt);
      zeitgeber.current.delete(karteId);
    }
  }

  async function pruefe(karteId: string, vorschlag: Vorschlag, entwurf: Werte[]) {
    let ergebnis: Pruefstand;
    try {
      const fehler = await pruefung.mutateAsync(pruefanfrage(vorschlag, entwurf));
      ergebnis = fehler.length === 0 ? { stand: "frei" } : { stand: "fehler", fehler };
    } catch (fehler) {
      ergebnis =
        fehler instanceof ProblemFehler && fehler.status === 404
          ? { stand: "typUnbekannt" }
          : { stand: "gestoert" };
    }
    schritt({ art: "geprueft", karteId, entwurf, ergebnis });
  }

  async function sendenAsync() {
    if (!kannSenden(zustand)) {
      return;
    }
    const text = zustand.entwurf;
    const meine = generation.current;
    schritt({ art: "senden", text });
    try {
      let gespraechId = zustand.gespraechId;
      if (gespraechId === null) {
        gespraechId = (await eroeffnen.mutateAsync()).gespraechId;
        if (meine !== generation.current) {
          beenden.mutate(gespraechId);
          return;
        }
        schritt({ art: "eroeffnet", gespraechId });
      }
      const antwort = await nachricht.mutateAsync({ gespraechId, text });
      if (meine === generation.current) {
        schritt({ art: "beantwortet", antwort, anfrageId: neueAnfrageId() });
      }
    } catch (fehler) {
      if (meine !== generation.current) {
        return;
      }
      if (fehler instanceof ProblemFehler && fehler.typ === "nachricht-laeuft") {
        // F12: Die Sperre während der Antwort soll das ausschließen.
        console.warn("Assistent: 409 nachricht-laeuft trotz Sperre", fehler.traceId ?? "");
      }
      schritt({ art: "gescheitert", text, hinweis: hinweisAus(fehler) });
    }
  }

  async function anlegenAsync(karteId: string) {
    const karte = karteVon(zustand, karteId);
    // Ein zweiter Klick während des Aufrufs schickt nichts; derselbe Klick nach
    // einem Fehler schickt dieselbe `anfrageId` (E‑1072).
    if (karte === null || !darfAnlegen(karte)) {
      return;
    }
    schritt({ art: "anlegenBeginnt", karteId });
    try {
      const auftrag = await anlage.mutateAsync({
        anfrageId: karte.anfrageId,
        ...pruefanfrage(karte.vorschlag, karte.werte),
      });
      schritt({ art: "angelegt", karteId, auftrag });
    } catch (fehler) {
      const prueffehler =
        fehler instanceof ProblemFehler && fehler.status === 400
          ? prueffehlerAus(fehler.fehlerliste)
          : [];
      schritt({
        art: "anlegenGescheitert",
        karteId,
        ergebnis:
          prueffehler.length > 0
            ? { stand: "fehler", fehler: prueffehler }
            : fehler instanceof ProblemFehler && fehler.status === 404
              ? { stand: "typUnbekannt" }
              : { stand: "gestoert" },
      });
    }
  }

  return {
    zustand,
    setzeEntwurf: (text) => schritt({ art: "entwurf", text }),
    senden: () => void sendenAsync(),
    neu: () => {
      generation.current += 1;
      for (const karteId of [...zeitgeber.current.keys()]) {
        stoppePruefung(karteId);
      }
      // Ein abgelaufenes Gespräch gibt es nicht mehr; ein Fehler beim Beenden
      // ändert für den Kunden nichts und wird nicht angezeigt.
      if (zustand.gespraechId !== null && !zustand.abgelaufen) {
        beenden.mutate(zustand.gespraechId);
      }
      schritt({ art: "neu" });
    },
    karte: {
      aufklappen: (karteId, offen) => schritt({ art: "aufklappen", karteId, offen }),
      aendern: (karteId) => schritt({ art: "aendern", karteId }),
      feld: (karteId, entwurf) => {
        const karte = karteVon(zustand, karteId);
        if (karte === null) {
          return;
        }
        schritt({ art: "feld", karteId, entwurf });
        stoppePruefung(karteId);
        zeitgeber.current.set(
          karteId,
          setTimeout(() => {
            zeitgeber.current.delete(karteId);
            void pruefe(karteId, karte.vorschlag, entwurf);
          }, PRUEFVERZOEGERUNG_MS),
        );
      },
      erneutPruefen: (karteId) => {
        const karte = karteVon(zustand, karteId);
        if (karte === null || karte.entwurf === null) {
          return;
        }
        const entwurf = karte.entwurf;
        stoppePruefung(karteId);
        schritt({ art: "pruefungBeginnt", karteId, entwurf });
        void pruefe(karteId, karte.vorschlag, entwurf);
      },
      uebernehmen: (karteId) => {
        stoppePruefung(karteId);
        schritt({ art: "uebernehmen", karteId });
      },
      verwerfen: (karteId) => {
        stoppePruefung(karteId);
        schritt({ art: "verwerfen", karteId });
      },
      anlegen: (karteId) => void anlegenAsync(karteId),
    },
  };
}
