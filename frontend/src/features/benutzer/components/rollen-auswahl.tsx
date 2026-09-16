"use client";

import { AuswahlFeld, type Auswahleintrag } from "@/components/auswahl-feld";
import { useTexte } from "@/i18n/provider";

import { ROLLEN, type Rolle } from "../api";

/**
 * Die Rollenauswahl der Benutzerverwaltung — **eine Liste, die der Anwendung
 * gehört**, und kein natives `<select>` mehr (Punkt 180,
 * `docs/benutzerverwaltung-frontend.md` §18).
 *
 * ## Was hier steht und was im Baustein
 *
 * **Die Bauform steht in `components/auswahl-feld.tsx`** — Popover, Liste,
 * Tastatur, Farben, und die Begründung dafür. Seit dem 16.09.2026 trägt sie
 * auch die Baumgliederung; sie ist deshalb dort und nicht mehr hier.
 *
 * **Hier steht, was die Rolle davon unterscheidet:**
 *
 * - **Ein unbekannter Rollenwert steht im Feld, wie er ist** — er fällt nicht
 *   still auf einen der beiden bekannten (Regel Q4). Das erledigt der Baustein,
 *   indem er keinen Eintrag findet und den Rohwert zeigt.
 * - **„Mandant" steht da, ist aber nicht wählbar**, solange das Konto keinen
 *   Mandanten trägt (E11) — `nichtWaehlbar`.
 * - **Eine wählbare leere Zeile** („Bitte wählen") gibt es nur in der Maske
 *   „Konto anlegen", wie vorher die leere Option.
 */
export function RollenAuswahl({
  id,
  beschriftung,
  wert,
  aufWahl,
  gesperrt = false,
  leer,
  nichtWaehlbar = [],
}: {
  /** Für `<Label htmlFor>` — das Feld ist der Auslöser. */
  id: string;
  /** Der zugängliche Name der aufgeklappten Liste; derselbe Text wie die Beschriftung. */
  beschriftung: string;
  /** Der gespeicherte Rohwert. Ein unbekannter steht da, wie er ist (Regel Q4). */
  wert: string;
  aufWahl: (rolle: Rolle | "") => void;
  gesperrt?: boolean;
  /** Eine wählbare leere Zeile oben, etwa „Bitte wählen" — wie die leere Option vorher. */
  leer?: string;
  /** Rollen, die angezeigt, aber nicht gewählt werden können. */
  nichtWaehlbar?: readonly Rolle[];
}) {
  const texte = useTexte();

  const eintraege: Auswahleintrag<Rolle | "">[] = [
    ...(leer === undefined ? [] : [{ wert: "" as const, text: leer }]),
    ...ROLLEN.map((rolle) => ({
      wert: rolle,
      text: texte.rolle[rolle],
      waehlbar: !nichtWaehlbar.includes(rolle),
    })),
  ];

  return (
    <AuswahlFeld
      id={id}
      beschriftung={beschriftung}
      wert={wert}
      eintraege={eintraege}
      aufWahl={aufWahl}
      gesperrt={gesperrt}
    />
  );
}
