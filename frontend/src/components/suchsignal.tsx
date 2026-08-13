"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

/**
 * Die eine Meldung, die vom **Suchfeld in der Kopfzeile** zur **Trefferansicht**
 * laufen muss: *diesen Begriff gibt es schon*.
 *
 * ## Warum es dafür überhaupt eine Naht braucht
 *
 * Ein doppelter Begriff wird nicht abgelegt — stattdessen meldet sich die
 * **vorhandene** Marke. Nur stehen die beiden Seiten an verschiedenen Orten: Das
 * Feld sitzt in der Kopfzeile und ist auf jeder Seite da, die Marken stehen über
 * der Trefferliste auf `/suche`. Ohne diese Naht müsste der eine dem anderen über
 * die URL zurufen — und was in der URL steht, steht in jedem geteilten Link
 * (`docs/frontend-grundlagen.md` §8). Ein Hinweis, der einmal aufblitzt, ist kein
 * Filterzustand.
 *
 * **Warum in `components/` und nicht in einem Feature:** dieselbe Lage wie bei
 * `zeitzone.tsx`. Der Anwendungsrahmen spannt ihn über Kopfzeile *und* Inhalt;
 * beide Seiten lesen ihn, keine importiert die andere.
 *
 * ## Ohne Provider passiert nichts
 *
 * Der Vorgabewert ist ein Signal, das niemand hört. Das ist hier richtig: Fehlt
 * der Rahmen — etwa in einem Test, der nur das Feld rendert —, soll das Feld
 * trotzdem funktionieren. Es ist ein Hinweis und keine Funktion.
 */
type Suchsignal = {
  /** Der Schlüssel `<typ>:<wert>` der Marke, die sich melden soll — oder `null`. */
  doppelt: string | null;
  meldeDoppelt: (schluessel: string) => void;
};

/**
 * Wie lange die vorhandene Marke hervorgehoben bleibt.
 *
 * Lang genug, um den Blick zu erreichen, kurz genug, um nicht als dauerhafte
 * Kennzeichnung gelesen zu werden. **Kein Übergang und keine Bewegung** — das
 * visuelle Konzept lässt außer Schublade und Menü keine Animation zu; der Ring
 * ist da oder er ist es nicht.
 */
const SICHTBAR_MS = 1500;

const Kontext = createContext<Suchsignal>({ doppelt: null, meldeDoppelt: () => {} });

export function SuchsignalProvider({ children }: { children: ReactNode }) {
  const [doppelt, setDoppelt] = useState<string | null>(null);
  const uhr = useRef<ReturnType<typeof setTimeout> | null>(null);

  const meldeDoppelt = useCallback((schluessel: string) => {
    // Ein zweiter Klick auf denselben Doppelten stellt die Uhr neu, statt einen
    // zweiten Zeitgeber danebenzustellen.
    if (uhr.current !== null) {
      clearTimeout(uhr.current);
    }
    setDoppelt(schluessel);
    uhr.current = setTimeout(() => {
      setDoppelt(null);
      uhr.current = null;
    }, SICHTBAR_MS);
  }, []);

  // Sonst überlebte der Zeitgeber die Abmeldung und schriebe in einen Baum, den
  // es nicht mehr gibt.
  useEffect(
    () => () => {
      if (uhr.current !== null) {
        clearTimeout(uhr.current);
      }
    },
    [],
  );

  const wert = useMemo(() => ({ doppelt, meldeDoppelt }), [doppelt, meldeDoppelt]);

  return <Kontext.Provider value={wert}>{children}</Kontext.Provider>;
}

export function useSuchsignal(): Suchsignal {
  return useContext(Kontext);
}
