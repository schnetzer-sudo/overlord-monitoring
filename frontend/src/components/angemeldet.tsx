"use client";

import { createContext, useContext, type ReactNode } from "react";

/**
 * **Wer gerade angemeldet ist** — genauer: unter welchem Benutzernamen.
 *
 * Die dritte Naht dieser Art nach `components/zeitzone.tsx` und
 * `components/suchsignal.tsx`, und aus demselben Grund: Den Wert hat der
 * Anwendungsrahmen aus der Selbstauskunft, gebraucht wird er in einem Feature,
 * und **ein Feature importiert nicht aus `features/sitzung`**
 * (`docs/frontend-grundlagen.md` §8).
 *
 * ## Warum nur der Name und keine `id`
 *
 * Weil es keine gibt. **`GET /api/auth/me` liefert keine `id`** — die
 * Selbstauskunft trägt `username`, `role`, `mandant`, `mustChangePassword` und
 * `anzeigezone`, mehr nicht. Die Kontenliste der Benutzerverwaltung führt
 * daneben eine `id`, aber es gibt keinen Wert, über den sich die beiden
 * verknüpfen ließen außer dem Namen (`docs/benutzerverwaltung.md` E19).
 *
 * **Die Selbstauskunft um eine `id` zu erweitern ist ausdrücklich verworfen:**
 * Sie ist seit Schritt 3 ein Vertrag, der in 9a schon einmal gebrochen worden
 * ist (`downloadAllowed`, E18), und ein zweiter Bruch in derselben Runde für
 * eine Bequemlichkeit der Oberfläche wäre schlecht bezahlt.
 *
 * ## Der Name ist eine Auskunft und keine Berechtigung
 *
 * Was hier steht, entscheidet **nichts**. Es steuert einen Hinweis, den der
 * Nutzer vor einem Vorgang zu sehen bekommt — mehr nicht. Wer ihn im Browser
 * verstellt, bekommt einen falschen Hinweis und sonst gar nichts: Verbindlich
 * prüft das Backend, und der Selbstschutz aus E12 hängt dort an der Konto-ID
 * und nicht an dieser Zeichenkette.
 */
const Kontext = createContext<string | undefined>(undefined);

export function AngemeldetProvider({
  username,
  children,
}: {
  username: string | undefined;
  children: ReactNode;
}) {
  return <Kontext.Provider value={username}>{children}</Kontext.Provider>;
}

/**
 * Der Benutzername der laufenden Sitzung, oder `undefined`, solange keiner
 * vorliegt.
 *
 * Bewusst **ohne** Ausnahme bei fehlendem Provider — wie {@link
 * useAnzeigezone} und anders als `useTexte`. Ein fehlender Text ist ein Fehler
 * in der Oberfläche; ein fehlender Name ist der normale Zustand, solange die
 * Selbstauskunft lädt. Was daran hängt, ist ein Hinweis: Er bleibt dann aus,
 * statt falsch zu erscheinen.
 */
export function useAngemeldeterName(): string | undefined {
  return useContext(Kontext);
}
