"use client";

import {
  AlertTriangle,
  CheckCheck,
  CircleCheck,
  CircleHelp,
  Clock,
  Merge,
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
  // Die beiden teilen sich die Farbrolle „offen" und tragen deshalb den ganzen
  // Unterschied im Zeichen und in der Beschriftung: aus eins wurde viel, aus
  // viel wurde eins.
  AUFGETEILT: Split,
  ZUSAMMENGEFUEHRT: Merge,
  UNGEKLAERT: CircleHelp,
};

export function StatusPlakette({
  statusKind,
  rohwert,
  bedeutungNichtVerifiziert,
  schritt,
  kompakt = false,
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
  /**
   * Nur das Zeichen, ohne Beschriftung — für eine Zeile, in der der Status
   * neben Ablaufname und Zeitpunkt steht und die 26 rem breit ist
   * (Kettenblock).
   *
   * **Das ist keine Ausnahme von „nie allein über Farbe".** Die Regel verlangt
   * „zusätzlich eine Beschriftung **oder** ein Zeichen"
   * (`visuelles-konzept.md` §3), und die acht Zeichen unterscheiden sich in
   * ihrer Form, nicht in ihrer Farbe. Die Beschriftung geht dabei nicht
   * verloren: Sie steht im `title` und für Vorleseprogramme im Markup.
   *
   * **Ein zweites Zeichen- oder Farbverzeichnis entsteht dafür nicht.** Genau
   * deshalb ist das eine Eigenschaft dieser Komponente und keine zweite.
   */
  kompakt?: boolean;
}) {
  const texte = useTexte();
  // Ein Wert, den diese Fassung nicht kennt, ist derselbe Fall wie ein
  // unbekannter Rohwert: ungeklärt, nicht geraten.
  const art: Statusart = statusKind in ZEICHEN ? (statusKind as Statusart) : "UNGEKLAERT";
  const Zeichen = ZEICHEN[art];

  const beschriftung = bedeutungNichtVerifiziert
    ? (rohwert ?? texte.einordnung.UNGEKLAERT)
    : texte.einordnung[art];

  const hinweis = bedeutungNichtVerifiziert
    ? texte.nachrichten.bedeutungNichtVerifiziert
    : rohwert === null
      ? undefined
      : `${texte.nachrichten.rohwert}: ${rohwert}`;

  if (kompakt) {
    return (
      <Badge
        variant="outline"
        className={cn("size-5 shrink-0 justify-center p-0", statusKlassen(art))}
        title={hinweis === undefined ? beschriftung : `${beschriftung} — ${hinweis}`}
      >
        <Zeichen aria-hidden="true" />
        <span className="sr-only">
          {beschriftung}
          {hinweis === undefined ? null : ` — ${hinweis}`}
        </span>
      </Badge>
    );
  }

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
      {schritt ? <SchrittZusatz schritt={schritt} /> : null}
    </span>
  );
}

/**
 * Der Schritt neben dem Status — **ohne Präposition.**
 *
 * **Nur bei offenen Nachrichten** — bei allen anderen liefert das Backend `null`,
 * weil `SOSActionID` dort den *letzten* Schritt benennt und nicht den aktuellen.
 * Als eigene Spalte wäre er auf 99 Prozent der Zeilen belanglos; hier steht er
 * genau dort, wo die Frage entsteht, die er beantwortet: „wartend — worauf?"
 *
 * ## Warum die Zelle keine Präposition mehr nennt (11.08.2026)
 *
 * Seit Schritt 5 stand hier „wartet vor: {Schritt}" beziehungsweise „läuft auf:
 * {Schritt}". **Messung M29 hat das erste widerlegt:** Über alle 538 wartenden
 * Nachrichten zeigt `Message.SOSActionID` auf den Schritt, der **zuletzt gelaufen**
 * ist — die Nachricht wartet *in* ihm, nicht davor. Das Detail sagt das seither
 * richtig; die Liste sagte an derselben Nachricht eine Zeile daneben das
 * Gegenteil.
 *
 * **Die Zelle nennt jetzt Status und Schritt und sonst nichts.** Nicht als
 * Kompromiss: Der Unterschied zwischen *in* und *vor* entsteht aus dem Vergleich
 * von `Message.SOSActionID` mit dem zuletzt ausgeführten Schritt — und der steht
 * in `MessageAction`, einer Tabelle mit 10,3 Millionen Zeilen, die die Liste nach
 * L2 und L3 nicht je Seite joinen soll. Das Detail kann es, weil es genau eine
 * Nachricht lädt. Die Zelle sagt damit genau das, was ihre Datenquelle hergibt —
 * und nicht mehr (`docs/nachrichtenliste.md` §8.1).
 *
 * **Die Beschriftung steht sichtbar da und nicht nur im Tooltip.** Auf einem
 * Touchgerät gibt es keinen Hover; ein Name ohne jede Einordnung wäre dort ein
 * Wort neben einer Plakette. Der `title` trägt denselben Text ungekürzt — die
 * Zelle ist eine Zeile hoch und kürzt.
 *
 * **Ohne eigene Farbrolle.** Er ist Beiwerk im Sinne des Leitsatzes und trägt die
 * gedämpfte Textfarbe; eine eigene Farbe wäre eine Statusaussage, die er nicht
 * macht (`visuelles-konzept.md` §3).
 */
function SchrittZusatz({ schritt }: { schritt: string }) {
  const texte = useTexte();
  const beschriftung = einsetzen(texte.nachrichten.schrittZusatz, { schritt });

  return (
    <span className="text-muted-foreground text-beiwerk min-w-0 truncate" title={beschriftung}>
      {beschriftung}
    </span>
  );
}
