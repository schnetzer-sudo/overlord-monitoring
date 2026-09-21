"use client";

import {
  createContext,
  useContext,
  useId,
  useMemo,
  type ComponentProps,
  type ReactNode,
} from "react";

import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";

/**
 * **Der eine Aufklappbaustein des Nachrichtendetails** *(21.09.2026, E‑228,
 * `docs/nachrichtendetail.md` §10.16)*.
 *
 * Alles, was im Detail auf- und zuklappt, steht darauf und bewegt sich deshalb
 * gleich: die Zeilen der Zeitleiste (Eingang, Schritte, Rest), die Belegdaten,
 * die technischen Eigenschaften und die Kettenabschnitte. Bis dahin trug jeder
 * Block seinen eigenen `useState` samt Schaltfläche, und der Inhalt wurde
 * ein- und ausgehängt.
 *
 * ## Zustand und Auszeichnung kommen aus `components/ui/collapsible`
 *
 * Der Generatorbaustein (Radix) liefert Schaltfläche, `aria-expanded` und
 * `data-state`. **Zwei Dinge setzt diese Datei selbst**, weil Radix sie an den
 * ausgehängten Inhalt bindet und der Inhalt hier eingehängt bleibt:
 *
 * - `aria-controls` steht **immer** an der Schaltfläche — Radix setzte es nur im
 *   offenen Zustand, weil es den zugeklappten Inhalt sonst nicht gäbe.
 * - Der zugeklappte Inhalt ist **`inert`**: eingehängt, aber weder mit der
 *   Tastatur noch für ein Vorleseprogramm erreichbar.
 *
 * ## Die Bewegung — dritte benannte Ausnahme von `visuelles-konzept.md` §7
 *
 * | Was | Wert |
 * |---|---|
 * | Höhe | `grid-template-rows` von `0fr` auf `1fr`, 300 ms, `cubic-bezier(0.2, 0, 0, 1)`, zu wie auf |
 * | Inhalt beim Öffnen | blendet über 150 ms nach 55 ms Verzögerung ein und rückt 4 px nach unten |
 * | Inhalt beim Schließen | blendet über 110 ms ohne Verzögerung aus |
 *
 * Nur CSS, keine Abhängigkeit, keine Keyframes. **Ein Übergang läuft beim
 * Einhängen nicht** — damit bewegt sich nichts beim ersten Aufbau und nichts
 * beim Nachrichtenwechsel (der Baum wird über `key` neu aufgebaut). Wächst der
 * offene Inhalt nach — die Kette lädt weiter —, bleibt die Spur bei `1fr`, und
 * es bewegt sich ebenfalls nichts.
 *
 * **Der Ausschalter ist Teil der Ausnahme:** Bei `prefers-reduced-motion:
 * reduce` steht alles sofort (`motion-reduce:transition-none`).
 *
 * ## Warum der Übergang nicht am Radix-Knoten hängt
 *
 * Radix misst seinen Inhaltsknoten bei jedem Wechsel und setzt dafür
 * `transition-duration: 0s` an ihn — ein Übergang an diesem Knoten spränge. Die
 * Rasterspur liegt deshalb eine Ebene darüber, das Einblenden eine darunter.
 */

type Zustand = { offen: boolean; inhaltId: string };

const AufklappKontext = createContext<Zustand | null>(null);

function useAufklappen(): Zustand {
  const zustand = useContext(AufklappKontext);
  if (zustand === null) {
    throw new Error("AufklappSchalter und AufklappInhalt gehören in <Aufklappen>.");
  }
  return zustand;
}

/**
 * Die Dauer der Höhenbewegung — dieselbe für Pfeil, Linie und Name, auf wie zu.
 *
 * **300 ms** *(E‑229, 21.09.2026 — bis dahin 220 ms)*. Vorgabe des
 * Auftraggebers, in beide Richtungen. Das Ein- und Ausblenden des Inhalts
 * (150 ms nach 55 ms, 110 ms) ist davon unberührt.
 */
export const AUFKLAPP_UEBERGANG =
  "duration-[300ms] ease-[cubic-bezier(0.2,0,0,1)] motion-reduce:transition-none";

/**
 * Der Rahmen um Schalter und Inhalt. **Der Zustand gehört dem Aufrufer** — er
 * braucht ihn für die Akzentlinie und den Pfeil —, und er liegt weder in URL,
 * Cookie noch Storage.
 *
 * `asChild` reicht den Rahmen an das eigene Element durch: Die Zeile der
 * Zeitleiste bleibt ein `li`.
 */
export function Aufklappen({
  offen,
  aufWechsel,
  ...rest
}: {
  offen: boolean;
  aufWechsel: (offen: boolean) => void;
} & Omit<ComponentProps<typeof Collapsible>, "open" | "onOpenChange" | "defaultOpen">) {
  const inhaltId = useId();
  const zustand = useMemo(() => ({ offen, inhaltId }), [offen, inhaltId]);

  return (
    <AufklappKontext.Provider value={zustand}>
      <Collapsible open={offen} onOpenChange={aufWechsel} {...rest} />
    </AufklappKontext.Provider>
  );
}

/** Die Schaltfläche. Ein `button`, `aria-expanded` von Radix, `aria-controls` immer. */
export function AufklappSchalter(props: ComponentProps<typeof CollapsibleTrigger>) {
  const { inhaltId } = useAufklappen();
  return <CollapsibleTrigger aria-controls={inhaltId} {...props} />;
}

/**
 * Der Inhalt — **immer eingehängt**, zugeklappt `inert` und auf Höhe null.
 *
 * @param className die Anordnung des Inhalts selbst. Sie liegt auf der innersten
 *   Ebene; die beiden darüber gehören der Bewegung.
 * @param randFuerFokus hält an allen vier Seiten 0,25 rem frei, damit der
 *   Fokusring eines Bedienelements im Inhalt nicht an der Schnittkante der
 *   Höhenbewegung abgeschnitten wird. **Seitlich gleicht ein negativer Rand es
 *   aus; oben und unten nicht** — ein negativer Rand an einer Spur der Höhe
 *   null zöge die Nachbarn zusammen. Der Aufrufer rechnet die 0,25 rem als
 *   seinen Abstand zum Schalter.
 */
export function AufklappInhalt({
  children,
  className,
  randFuerFokus = false,
}: {
  children: ReactNode;
  className?: string;
  randFuerFokus?: boolean;
}) {
  const { offen, inhaltId } = useAufklappen();

  return (
    <div
      data-state={offen ? "open" : "closed"}
      data-aufklappen="spur"
      className={cn(
        "grid grid-rows-[0fr] transition-[grid-template-rows] data-[state=open]:grid-rows-[1fr]",
        AUFKLAPP_UEBERGANG,
        randFuerFokus && "-mx-1",
      )}
    >
      <CollapsibleContent
        forceMount
        id={inhaltId}
        inert={!offen}
        className={cn("min-h-0 overflow-hidden", randFuerFokus && "px-1")}
      >
        <div
          data-state={offen ? "open" : "closed"}
          data-aufklappen="inhalt"
          className={cn(
            "-translate-y-1 opacity-0 transition-[opacity,translate] duration-[110ms] ease-out",
            "data-[state=open]:translate-y-0 data-[state=open]:opacity-100",
            "data-[state=open]:delay-[55ms] data-[state=open]:duration-150",
            "motion-reduce:transition-none",
            randFuerFokus && "py-1",
            className,
          )}
        >
          {children}
        </div>
      </CollapsibleContent>
    </div>
  );
}
