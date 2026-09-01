"use client";

import { createContext, useContext, type ReactNode } from "react";

import { STANDARDDICHTE, type Dichtestufe } from "./index";

/**
 * Reicht die **aktive** Dichtestufe in den Client-Baum. Sie kommt als Prop vom
 * Wurzel-Layout, wird also serverseitig aus dem Cookie gelesen — dieselbe Naht
 * wie beim Sprachkontext.
 *
 * **Wozu überhaupt ein Kontext, wo die Stufe doch am `<html>` steht.** Weil der
 * Umschalter sie *lesen* muss, um die aktive Stufe zu markieren, und weil der
 * einzige andere Weg dorthin `getComputedStyle` bzw. `document.documentElement`
 * wäre — beides erst nach der Hydratation zu haben und damit ein
 * Nachladeeffekt in einem Menü. `docs/frontend-grundlagen.md` schließt den
 * Laufzeitweg für Gestaltungswerte ausdrücklich aus, und der Grund trägt hier
 * genauso: Es wäre ein zweiter Weg, auf dem derselbe Wert in die Anwendung
 * kommt.
 *
 * Eine Prop-Kette scheidet aus demselben Grund aus wie bei `zeitzone.tsx`: Der
 * Wert entsteht im Wurzel-Layout, gebraucht wird er im Nutzermenü tief in der
 * Kopfzeile.
 */
const Kontext = createContext<Dichtestufe>(STANDARDDICHTE);

export function DichteProvider({ dichte, children }: { dichte: Dichtestufe; children: ReactNode }) {
  return <Kontext.Provider value={dichte}>{children}</Kontext.Provider>;
}

/**
 * Die aktive Dichtestufe.
 *
 * Bewusst **ohne** Ausnahme bei fehlendem Provider — anders als `useTexte` und
 * aus demselben Grund wie bei `useAnzeigezone`: Der Vorgabewert ist hier keine
 * Notlösung, sondern **genau derselbe definierte Rückfall**, den auch ein
 * fehlendes oder unbekanntes Cookie ergibt. Ein Test, der nur den Umschalter
 * rendert, bekommt damit dieselbe Antwort wie die Anwendung ohne Cookie.
 */
export function useDichte(): Dichtestufe {
  return useContext(Kontext);
}
