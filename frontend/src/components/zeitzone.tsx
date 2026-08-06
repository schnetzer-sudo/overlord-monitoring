"use client";

import { createContext, useContext, type ReactNode } from "react";

import { ZEITZONE_RUECKFALL } from "@/lib/format";

/**
 * Die Zone, in der die Oberfläche Zeitstempel anzeigt.
 *
 * **Sie kommt vom Backend** (`anzeigezone` in der Selbstauskunft) und ist
 * dieselbe, mit der `common/Zeitpunkte` die Wanduhrzeit der Quelle nach UTC
 * umrechnet. Warum überhaupt eine feste Zone und nicht die des Browsers, steht
 * in `lib/format.ts`.
 *
 * **Warum ein Kontext und keine Prop-Kette.** Den Wert hat der Anwendungsrahmen,
 * gebraucht wird er in jeder Tabellenzelle. Ihn durchzureichen hieße, jede
 * Zwischenkomponente um einen Parameter zu erweitern, den sie selbst nicht
 * braucht — und beim ersten Vergessen fiele die Anzeige lautlos auf UTC zurück.
 *
 * **Warum in `components/` und nicht in einem Feature.** `features/nachrichten`
 * darf nicht aus `features/sitzung` importieren, und dort liegt die
 * Selbstauskunft. Der Kontext ist genau die Naht dazwischen: Der Rahmen füllt
 * ihn, jedes Feature liest ihn.
 */
const Kontext = createContext<string | undefined>(undefined);

export function ZeitzoneProvider({
  zone,
  children,
}: {
  zone: string | undefined;
  children: ReactNode;
}) {
  return <Kontext.Provider value={zone}>{children}</Kontext.Provider>;
}

/**
 * Die Anzeigezone, oder {@link ZEITZONE_RUECKFALL}, solange keine vorliegt.
 *
 * Bewusst **ohne** Ausnahme bei fehlendem Provider — anders als `useTexte`. Ein
 * fehlender Text ist ein Fehler in der Oberfläche; eine fehlende Zone ist der
 * normale Zustand, solange die Selbstauskunft lädt. Gezeigt wird dann UTC: für
 * alle gleich und sichtbar abweichend, statt je nach Standort verschoben.
 */
export function useAnzeigezone(): string {
  return useContext(Kontext) ?? ZEITZONE_RUECKFALL;
}
