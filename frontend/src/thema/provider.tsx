"use client";

import { createContext, useContext, type ReactNode } from "react";

import { STANDARDTHEMA, type Themawert } from "./index";

/**
 * Reicht das **aktive** Erscheinungsbild in den Client-Baum. Es kommt als Prop
 * vom Wurzel-Layout, wird also serverseitig aus dem Cookie gelesen — dieselbe
 * Naht wie beim Sprach- und Dichtekontext.
 *
 * **Wozu überhaupt ein Kontext, wo der Wert doch am `<html>` steht.** Weil der
 * Umschalter ihn *lesen* muss, um die aktive Wahl zu markieren, und weil der
 * einzige andere Weg dorthin `document.documentElement` bzw.
 * `getComputedStyle` wäre — beides erst nach der Hydratation zu haben und damit
 * ein Nachladeeffekt in einem Menü. `docs/frontend-grundlagen.md` schließt den
 * Laufzeitweg für Gestaltungswerte ausdrücklich aus, und der Grund trägt hier
 * genauso: Es wäre ein zweiter Weg, auf dem derselbe Wert in die Anwendung
 * kommt.
 *
 * > **Was der Kontext ausdrücklich nicht sagt: ob es gerade dunkel *ist*.** Bei
 * > `system` entscheidet das die Medienabfrage im Stilblatt, und diese Datei
 * > erfährt davon nichts. Das ist Absicht — wer hier `matchMedia` befragte,
 * > baute genau den zweiten Weg, den E‑60 vermeidet: Der Dunkelzustand käme
 * > dann aus dem Browser statt aus dem CSS und flackerte beim Laden.
 */
const Kontext = createContext<Themawert>(STANDARDTHEMA);

export function ThemaProvider({ thema, children }: { thema: Themawert; children: ReactNode }) {
  return <Kontext.Provider value={thema}>{children}</Kontext.Provider>;
}

/**
 * Das aktive Erscheinungsbild — `hell`, `dunkel` oder `system`.
 *
 * Bewusst **ohne** Ausnahme bei fehlendem Provider — anders als `useTexte` und
 * aus demselben Grund wie bei `useDichte`: Der Vorgabewert ist hier keine
 * Notlösung, sondern **genau derselbe definierte Rückfall**, den auch ein
 * fehlendes oder unbekanntes Cookie ergibt. Ein Test, der nur den Umschalter
 * rendert, bekommt damit dieselbe Antwort wie die Anwendung ohne Cookie.
 */
export function useThema(): Themawert {
  return useContext(Kontext);
}
