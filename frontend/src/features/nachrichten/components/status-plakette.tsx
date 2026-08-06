"use client";

import {
  AlertTriangle,
  CheckCheck,
  CircleCheck,
  CircleHelp,
  Clock,
  PlayCircle,
  Split,
  type LucideIcon,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { useTexte } from "@/i18n/provider";
import { statusKlassen, type Statusart } from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

/**
 * Der Status einer Zeile.
 *
 * **Nie allein über Farbe.** Jede Plakette trägt zusätzlich eine Beschriftung und
 * ein Zeichen; die Farbrolle ist die halbe Aussage, nie die ganze. Zwei Gründe,
 * beide für sich ausreichend: Rot-Grün-Schwäche betrifft rund acht Prozent der
 * Männer, und dieses Werkzeug ist ein Fehlermelder — ein Status, den man nicht
 * unterscheiden kann, ist keiner. Dazu liegt der Akzent der Anwendung als
 * Gelbgrün zwischen den beiden fachlich belegten Farbzonen und darf nie als
 * Statusaussage lesbar sein (`docs/visuelles-konzept.md` §3).
 *
 * **Die Zuordnung Status → Farbe steht nicht hier**, sondern an genau einer
 * Stelle in `lib/status-farbe.ts`. Diese Komponente kennt keine Farbe.
 *
 * **Der Rohwert geht nicht verloren.** Bei einer gesicherten Einordnung steht er
 * im Tooltip — damit ein Anwender ihn gegen die alte Oberfläche halten kann. Bei
 * `bedeutungNichtVerifiziert` wird er zur Beschriftung: Der Wert kommt so aus dem
 * Altsystem, seine fachliche Bedeutung ist nicht belegt, und die Oberfläche
 * kennzeichnet das, statt einen plausiblen Text zu erfinden.
 */
const ZEICHEN: Record<Statusart, LucideIcon> = {
  FEHLER: AlertTriangle,
  ABGESCHLOSSEN: CircleCheck,
  QUITTIERT: CheckCheck,
  WARTEND: Clock,
  LAEUFT: PlayCircle,
  ZWISCHENSCHRITT: Split,
  UNGEKLAERT: CircleHelp,
};

export function StatusPlakette({
  statusKind,
  rohwert,
  bedeutungNichtVerifiziert,
}: {
  statusKind: string;
  rohwert: string | null;
  bedeutungNichtVerifiziert: boolean;
}) {
  const texte = useTexte();
  // Ein Wert, den diese Fassung nicht kennt, ist derselbe Fall wie ein
  // unbekannter Rohwert: ungeklärt, nicht geraten.
  const art: Statusart = statusKind in ZEICHEN ? (statusKind as Statusart) : "UNGEKLAERT";
  const Zeichen = ZEICHEN[art];

  const beschriftung = bedeutungNichtVerifiziert
    ? (rohwert ?? texte.nachrichten.status.UNGEKLAERT)
    : texte.nachrichten.status[art];

  const hinweis = bedeutungNichtVerifiziert
    ? texte.nachrichten.bedeutungNichtVerifiziert
    : rohwert === null
      ? undefined
      : `${texte.nachrichten.rohwert}: ${rohwert}`;

  return (
    <Badge
      variant="outline"
      className={cn("h-auto max-w-full gap-1.5 px-2 py-0.5", statusKlassen(art))}
      title={hinweis}
    >
      <Zeichen aria-hidden="true" />
      <span className={cn("truncate", bedeutungNichtVerifiziert && "font-mono")}>
        {beschriftung}
      </span>
      {hinweis === undefined ? null : <span className="sr-only">— {hinweis}</span>}
    </Badge>
  );
}
