"use client";

import { AlertCircle, Inbox } from "lucide-react";

import { useTexte } from "@/i18n/provider";
import { fehleranzeige } from "@/lib/fehlertext";
import { cn } from "@/lib/utils";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * Die drei sichtbaren Zustände jeder Ansicht: **Laden, Leer, Fehler**.
 *
 * „Leer" ist kein Fehler und sieht auch nicht so aus — das ist der Grund, warum
 * es hier zwei getrennte Bausteine gibt und nicht einen mit einem Schalter. Wer
 * bei jedem leeren Zeitfenster eine rote Meldung sieht, hört auf, rote Meldungen
 * ernst zu nehmen.
 */

export function Laden({ zeilen = 3, className }: { zeilen?: number; className?: string }) {
  const texte = useTexte();
  return (
    <div className={cn("space-y-3", className)} aria-busy="true" aria-live="polite">
      <span className="sr-only">{texte.zustand.laedt}</span>
      {Array.from({ length: zeilen }, (_, nummer) => (
        <Skeleton key={nummer} className="h-zeile w-full" />
      ))}
    </div>
  );
}

/**
 * @param hinweis Woran es liegen kann. Bei einer leeren Liste ist das meist das
 *   Zeitfenster — dem Nutzer nur „nichts gefunden" hinzuwerfen, hilft ihm nicht.
 * @param children Ein **Ausweg**, wenn es einen gibt: die Belegsuche bietet hier
 *   seit Schritt 7, Teil 4 an, über den Anfang der Nummer zu suchen
 *   (`docs/bam-suche.md` §23). Er steht **im** Leerzustand und nicht darunter,
 *   weil er zu ihm gehört — ein Angebot neben der Meldung sähe aus wie eine
 *   zweite Sache. Bleibt der Platz leer, ändert sich nichts an der Bauform.
 */
export function Leer({
  titel,
  hinweis,
  children,
}: {
  titel?: string;
  hinweis?: string;
  children?: React.ReactNode;
}) {
  const texte = useTexte();
  return (
    <div className="border-border bg-card text-muted-foreground flex flex-col items-center gap-2 rounded-lg border border-dashed px-4 py-10 text-center">
      <Inbox aria-hidden="true" className="size-6 opacity-60" />
      <p className="text-foreground font-medium">{titel ?? texte.zustand.leerTitel}</p>
      {hinweis ? <p className="text-beiwerk max-w-prose">{hinweis}</p> : null}
      {children}
    </div>
  );
}

/**
 * @param text Ein **genauerer** Satz als der aus dem Fehlerkatalog, wenn die
 *   Antwort ihn hergibt. Gebraucht von einem Fall: `praefixsuche-fenster-zu-gross`
 *   bringt `grenzeTage` und `angefragtTage` mit, und eine Meldung mit beiden
 *   Zahlen ist besser als eine ohne (`docs/frontend-grundlagen.md` §6).
 *   **`undefined` heißt „nimm den Katalog"** — genau das ist der Rückfall, wenn
 *   eine der Zahlen fehlt.
 * @param children Ein Ausweg neben „Erneut versuchen", wenn es einen gibt, der
 *   mehr hilft: bei einem zu großen Zeitfenster das Verkleinern selbst.
 */
export function Fehler({
  fehler,
  aufWiederholen,
  text,
  children,
}: {
  fehler: unknown;
  aufWiederholen?: () => void;
  text?: string;
  children?: React.ReactNode;
}) {
  const texte = useTexte();
  const anzeige = fehleranzeige(fehler, texte);

  return (
    <Alert variant="destructive">
      <AlertCircle aria-hidden="true" />
      <AlertTitle>{texte.zustand.fehlerTitel}</AlertTitle>
      <AlertDescription>
        <p>{text ?? anzeige.text}</p>
        {anzeige.felder.length > 0 ? (
          <ul className="mt-1 list-disc pl-4">
            {anzeige.felder.map((feld) => (
              <li key={feld.feld}>{feld.meldung}</li>
            ))}
          </ul>
        ) : null}
        {anzeige.kennung ? (
          <p className="text-beiwerk mt-2">
            {texte.zustand.kennung}: <code className="font-mono">{anzeige.kennung}</code>
            <br />
            {texte.zustand.kennungHinweis}
          </p>
        ) : null}
        {aufWiederholen ? (
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="min-h-beruehrung mt-3"
            onClick={aufWiederholen}
          >
            {texte.zustand.erneutVersuchen}
          </Button>
        ) : null}
        {children}
      </AlertDescription>
    </Alert>
  );
}
