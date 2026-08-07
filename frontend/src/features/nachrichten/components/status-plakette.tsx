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
import { einsetzen } from "@/i18n";
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
  schritt,
}: {
  statusKind: string;
  rohwert: string | null;
  bedeutungNichtVerifiziert: boolean;
  /**
   * Der Schritt, auf dem eine **offene** Nachricht gerade steht. Das Backend
   * liefert ihn nur bei `WARTEND` und `LAEUFT`; diese Komponente entscheidet das
   * nicht nach und prüft es nicht nach — sie zeigt, was da ist.
   */
  schritt?: string | null;
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
    // Eine Zeile, nicht zwei: Die Zeilenhöhe der Liste ist `--dichte-zeile` und
    // gilt für alle Zeilen gleich. Sie auf zwei Zeilen auszulegen kostete jede
    // Zeile ein Drittel Höhe — für einen Zusatz, den in der Testkopie 538 von
    // 3,3 Millionen Zeilen tragen.
    <span className="flex min-w-0 items-center gap-1.5">
      <Badge
        variant="outline"
        className={cn(
          "h-auto max-w-full gap-1.5 px-2 py-0.5",
          statusKlassen(art),
          /*
           * **Steht ein Schritt daneben, weicht der Status nicht.** Ohne diese
           * Unterscheidung kürzte die Zeile zuerst die Plakette — aus „Wartend"
           * wurde „Warte…", während der Schritt daneben Platz behielt. Genau
           * verkehrt herum: Der Status ist die Hauptinformation, der Schritt ist
           * Beiwerk nach dem Leitsatz. Aufgefallen in der Sichtprüfung am
           * 07.08.2026.
           *
           * Ohne Schritt darf sie weiter weichen: Dort ist sie das einzige
           * Element, und bei `bedeutungNichtVerifiziert` steht ein Rohwert
           * beliebiger Länge darin.
           */
          schritt ? "shrink-0" : "shrink",
        )}
        title={hinweis}
      >
        <Zeichen aria-hidden="true" />
        <span className={cn("truncate", bedeutungNichtVerifiziert && "font-mono")}>
          {beschriftung}
        </span>
        {hinweis === undefined ? null : <span className="sr-only">— {hinweis}</span>}
      </Badge>
      {schritt ? <SchrittZusatz schritt={schritt} art={art} /> : null}
    </span>
  );
}

/**
 * Der Schritt neben dem Status — **und was die Zelle über ihn behauptet.**
 *
 * **Nur bei offenen Nachrichten** — bei allen anderen liefert das Backend `null`,
 * weil `SOSActionID` dort den *letzten* Schritt benennt und nicht den aktuellen.
 * Als eigene Spalte wäre er auf 99 Prozent der Zeilen belanglos; hier steht er
 * genau dort, wo die Frage entsteht, die er beantwortet: „wartend — worauf?"
 *
 * ## Zwei Beschriftungen, seit Schritt 5
 *
 * Bis dahin stand hier der nackte Name mit dem Tooltip „Aktueller Schritt", und
 * das führte in die Irre: Wer „Send Message to Pool" neben `Wartend` liest,
 * nimmt an, dieser Schritt laufe gerade. **Messung M16 (3) sagt das Gegenteil** —
 * bei allen 538 `SUSPENDED`-Nachrichten der Testkopie ist *jede* Aktion beendet.
 * Eine wartende Nachricht steht **zwischen** zwei Schritten, nicht auf einem.
 *
 * **Für `LAEUFT` gilt das nicht als belegt.** `RUNNING` kommt in der Testkopie
 * null Mal vor; gerade dort wäre ein tatsächlich laufender Schritt der zu
 * erwartende Fall. Die Beschriftung trägt deshalb **beide** Lagen und stellt
 * nicht einfach alles auf „wartet vor" um — das wäre dieselbe ungeprüfte
 * Behauptung mit umgekehrtem Vorzeichen.
 *
 * **Ohne eigene Farbrolle.** Er ist Beiwerk im Sinne des Leitsatzes und trägt die
 * gedämpfte Textfarbe; eine eigene Farbe wäre eine Statusaussage, die er nicht
 * macht (`visuelles-konzept.md` §3).
 */
function SchrittZusatz({ schritt, art }: { schritt: string; art: Statusart }) {
  const texte = useTexte();
  const wartet = art === "WARTEND";
  const beschriftung = einsetzen(
    wartet ? texte.nachrichten.schrittWartetVor : texte.nachrichten.schrittLaeuftAuf,
    { schritt },
  );
  const hinweis = einsetzen(
    wartet ? texte.nachrichten.schrittWartetVorHinweis : texte.nachrichten.schrittLaeuftAufHinweis,
    { schritt },
  );

  return (
    <span className="text-muted-foreground text-beiwerk min-w-0 truncate" title={hinweis}>
      <span aria-hidden="true">{beschriftung}</span>
      <span className="sr-only">{hinweis}</span>
    </span>
  );
}
