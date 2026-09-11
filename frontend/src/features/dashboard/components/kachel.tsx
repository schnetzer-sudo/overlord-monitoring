"use client";

import type { LucideIcon } from "lucide-react";

import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

/**
 * Der **gemeinsame Rahmen** der Kachelreihe und ihr Kopf.
 *
 * **Beides stand bis zum 10.09.2026 in `kacheln.tsx`** und ist von dort
 * unverändert hierher gewandert — keine Zeile Gestaltung hat sich dabei
 * geändert. Der Anlass ist die fünfte Kachel: Sie wohnt in
 * `plattform-block.tsx`, weil sie die Auskunft über die **Anlage** trägt und
 * nicht über den Mandanten, und `kacheln.tsx` setzt sie in die Reihe. Blieben
 * Rahmen und Kopf dort, importierten die beiden Dateien einander im Kreis.
 *
 * **Es ist kein neuer Baustein, sondern ein umgezogener.** Wer die Gestalt einer
 * Kachel ändern will, ändert sie hier — und zwar für alle fünf zugleich.
 */

/** Der gemeinsame Rahmen. Die Farbe kommt als Klassenkette von außen. */
export function Kachel({ klassen, children }: { klassen?: string; children: React.ReactNode }) {
  return (
    <Card size="sm" className={cn("gap-2 px-4", klassen)}>
      {children}
    </Card>
  );
}

/**
 * Der Kopf einer Kachel: **Zeichen und Wort**.
 *
 * Ihn tragen *Fehler*, *Nachrichten* und seit dem 10.09.2026 *Plattform*. Die
 * beiden Zustandskacheln haben an seiner Stelle ihre Plakette — sie trägt
 * Zeichen *und* Wort, und ein Kopf darüber sagte dasselbe Wort ein zweites Mal
 * (`docs/dashboard-frontend.md` §5.4).
 */
export function Kopf({ zeichen: Zeichen, titel }: { zeichen: LucideIcon; titel: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <Zeichen aria-hidden="true" className="size-4 shrink-0" />
      <span className="text-basis font-medium">{titel}</span>
    </div>
  );
}
