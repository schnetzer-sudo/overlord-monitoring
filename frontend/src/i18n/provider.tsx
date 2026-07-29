"use client";

import { createContext, useContext, type ReactNode } from "react";

import type { Sprache, Texte } from "./index";

type Sprachkontext = { sprache: Sprache; texte: Texte };

const Kontext = createContext<Sprachkontext | null>(null);

/**
 * Reicht die **aktive** Sprachdatei in den Client-Baum. Sie kommt als Prop vom
 * Wurzel-Layout, wird also serverseitig ausgewählt — deshalb liegt nie die
 * zweite Sprachdatei im ausgelieferten Zustand.
 */
export function SpracheProvider({
  sprache,
  texte,
  children,
}: Sprachkontext & { children: ReactNode }) {
  return <Kontext.Provider value={{ sprache, texte }}>{children}</Kontext.Provider>;
}

function useSprachkontext(): Sprachkontext {
  const wert = useContext(Kontext);
  if (wert === null) {
    throw new Error("useTexte ausserhalb von SpracheProvider verwendet");
  }
  return wert;
}

/** Alle Zeichenketten der aktiven Sprache. */
export function useTexte(): Texte {
  return useSprachkontext().texte;
}

/** Die aktive Sprache — für `Intl`-Formatierung und die Umschaltung. */
export function useSprache(): Sprache {
  return useSprachkontext().sprache;
}
