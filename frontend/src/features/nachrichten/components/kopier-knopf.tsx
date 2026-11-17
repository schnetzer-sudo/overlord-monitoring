"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Check, Copy } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

import { KOPIERT_ANZEIGE_MS } from "../kopieren";

/**
 * **Kopieren mit Rückmeldung** — der Zustand hinter dem Kopierknopf
 * (`docs/nachrichtendetail.md` §10.3 und §10.21, E‑1207).
 *
 * Herausgelöst am 17.11.2026 aus der `MessageID` im Kopf, wo er inline stand;
 * die Eigenschaftszeile ist sein zweiter Verbraucher. Der Zustand liegt beim
 * Aufrufer und nicht im Knopf, weil die Zeile ihn **auch ohne Knopf** auslöst —
 * ein Tippen auf den Wert zeigt das Häkchen an derselben Stelle.
 *
 * **Scheitert das Kopieren** — kein sicherer Kontext, kein Recht —, bleibt der
 * Wert sichtbar und markierbar stehen, und das Zeichen wechselt nicht. Eine
 * Fehlermeldung für einen Knopf, der Beiwerk kopiert, wäre lauter als die
 * Sache. `navigator.clipboard?.` bricht die ganze Kette ab, wenn es die
 * Zwischenablage nicht gibt.
 *
 * **Kopiert wird der übergebene Wert, nie Text aus dem DOM.** Die Anzeige ist
 * gekürzt; der Wert aus den Daten ist es nicht.
 */
export function useKopieren(wert: string): { kopiert: boolean; kopiere: () => void } {
  const [kopiert, setKopiert] = useState(false);
  const zeitgeber = useRef<ReturnType<typeof setTimeout> | null>(null);
  const abgebaut = useRef(false);

  // Nur das Aufräumen: Ein Zeitgeber, der nach dem Abbau noch liefe, setzte den
  // Zustand eines Baums, den es nicht mehr gibt. Kein setState im Effekt.
  useEffect(() => {
    abgebaut.current = false;
    return () => {
      abgebaut.current = true;
      if (zeitgeber.current !== null) clearTimeout(zeitgeber.current);
    };
  }, []);

  const kopiere = useCallback(() => {
    void navigator.clipboard
      ?.writeText(wert)
      .then(() => {
        if (abgebaut.current) return;
        setKopiert(true);
        if (zeitgeber.current !== null) clearTimeout(zeitgeber.current);
        zeitgeber.current = setTimeout(() => {
          zeitgeber.current = null;
          setKopiert(false);
        }, KOPIERT_ANZEIGE_MS);
      })
      .catch(() => undefined);
  }, [wert]);

  return { kopiert, kopiere };
}

/**
 * **Der Kopierknopf** — ein Zeichen, `Copy` und nach dem Kopieren für
 * {@link KOPIERT_ANZEIGE_MS} `Check` aus `lucide-react`.
 *
 * `button` mit `aria-label` **und** `title`: das eine für das
 * Vorleseprogramm, das andere für den Zeiger. Beide wechseln mit dem Zustand —
 * das ist die Ansage, dieselbe wie seit Schritt 5 an der `MessageID`; kein
 * Toast, keine Live-Region. Fokusring, `Enter` und `Leertaste` bringt der
 * `Button` mit.
 *
 * `data-kopiert` steht am Knopf, solange das Häkchen steht. Die Zeile liest es,
 * um den sonst verborgenen Knopf für diese Zeit sichtbar zu machen.
 */
export function KopierKnopf({
  kopiert,
  aufKopieren,
  beschriftung,
  beschriftungKopiert,
  className,
}: {
  kopiert: boolean;
  aufKopieren: () => void;
  beschriftung: string;
  beschriftungKopiert: string;
  className?: string;
}) {
  const text = kopiert ? beschriftungKopiert : beschriftung;

  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      className={cn("shrink-0", className)}
      onClick={aufKopieren}
      aria-label={text}
      title={text}
      data-kopieren="knopf"
      data-kopiert={kopiert ? "true" : undefined}
    >
      {kopiert ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
    </Button>
  );
}
