"use client";

import { Clock, Hourglass, PlayCircle } from "lucide-react";

import { einsetzen } from "@/i18n";
import { useTexte } from "@/i18n/provider";
import { formatiereDauer } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Nachrichtendetail, Schritt } from "../api";
import { zeitleiste, type Zeitleistenzeile } from "../detail";

/**
 * Die Zeitleiste — der Kern der Detailansicht.
 *
 * **Senkrecht, ein Schritt je Zeile**, weil das Panel schmal ist. Je Schritt der
 * Name, ein schmaler Balken und die Dauer als Text.
 *
 * Warum der Balken auf **diese** Nachricht normiert ist und ab wann eine Lücke
 * eine eigene Zeile bekommt, steht in `../detail.ts` — dort, wo es gerechnet und
 * geprüft wird. Diese Datei stellt nur dar.
 *
 * **Feste Zeilenhöhe je Schritt**, nach der Regel aus `nachrichtenliste.md` §8.1:
 * Was nicht hineinpasst, wird gekürzt, der Vollwert steht im `title`. Die
 * gemessene Namenslänge geht bis 61 Zeichen — in einem Panel von 26 rem passt
 * das nicht immer, und eine Leiste mit springenden Zeilenhöhen lässt sich nicht
 * überfliegen.
 */
export function Zeitleiste({ detail }: { detail: Nachrichtendetail }) {
  const texte = useTexte();
  const zeilen = zeitleiste(detail);

  if (zeilen.length === 0) {
    return (
      <p className="text-muted-foreground text-beiwerk">
        {detail.offenerZustand === "OHNE_SCHRITT"
          ? texte.nachrichten.detail.ohneSchritt
          : texte.nachrichten.detail.keineSchritte}
      </p>
    );
  }

  return (
    <ol className="flex flex-col">
      {zeilen.map((zeile) => (
        <Zeile key={zeile.id} zeile={zeile} />
      ))}
    </ol>
  );
}

function Zeile({ zeile }: { zeile: Zeitleistenzeile }) {
  if (zeile.art === "luecke") {
    return <LueckenZeile sekunden={zeile.sekunden} />;
  }
  if (zeile.art === "erwartet") {
    return <ErwarteteZeile name={zeile.name} bereitsGelaufen={zeile.bereitsGelaufen} />;
  }
  return <SchrittZeile schritt={zeile.schritt} anteil={zeile.anteil} />;
}

/**
 * Ein ausgeführter Schritt.
 *
 * **Die Herkunft des Namens steht im Tooltip**, dezent und zusammen mit dem
 * Rohwert — kein Symbol, kein Warnzeichen, keine eigene Spalte. Ein Nutzer, der
 * „Send File by FTP" liest, soll nicht mit der Frage belastet werden, wie wir
 * darauf gekommen sind; wer nachsehen will, findet es. Bei `ROHWERT` steht
 * ohnehin der Rohwert als Name — auch dort gehört die Herkunft in den Tooltip,
 * damit die Erklärung an **einer** Stelle liegt.
 */
function SchrittZeile({ schritt, anteil }: { schritt: Schritt; anteil: number | null }) {
  const texte = useTexte();
  const dauer =
    schritt.dauerSekunden === null
      ? texte.nachrichten.detail.ohneDauer
      : formatiereDauer(schritt.dauerSekunden, texte.nachrichten.detail.dauer);

  const hinweis = [
    schritt.name,
    schritt.rohwert ? `${texte.nachrichten.detail.baustein}: ${schritt.rohwert}` : undefined,
    texte.nachrichten.detail.herkunft[schritt.namensherkunft],
  ]
    .filter((teil): teil is string => teil !== undefined)
    .join("\n");

  return (
    <li
      className={cn(
        "h-zeile flex items-center gap-2 border-l-2 pl-2",
        // Der laufende Schritt trägt die Farbrolle „offen" — dieselbe, die die
        // Statusplakette für „wartend, laufend" hat. Der Akzent der Anwendung
        // wäre hier falsch: Er sagt etwas über die Anwendung, nie über die
        // Daten (`visuelles-konzept.md` §3).
        schritt.laeuftAuf ? "border-status-offen-kontur" : "border-border",
      )}
    >
      <span className="min-w-0 flex-1 truncate" title={hinweis}>
        {schritt.name}
      </span>

      {schritt.laeuftAuf ? (
        // Nie allein über Farbe: Zeichen **und** Text.
        <span className="text-status-offen text-beiwerk flex shrink-0 items-center gap-1">
          <PlayCircle aria-hidden="true" className="size-3.5" />
          {texte.nachrichten.detail.laeuftGerade}
        </span>
      ) : (
        <Balken anteil={anteil} />
      )}

      <span className="text-muted-foreground text-beiwerk w-20 shrink-0 text-right" data-ziffern>
        {schritt.laeuftAuf && schritt.dauerSekunden === null ? "" : dauer}
      </span>
    </li>
  );
}

/**
 * Der Balken. **Ohne Zahl wäre er ein Gefühl** — die Dauer steht immer daneben,
 * und diese Fläche ist nur die Beziehung der Schritte untereinander.
 *
 * Fehlt die Dauer, fehlt der Balken: Eine Fläche der Breite null sähe aus wie
 * „praktisch nichts", und das ist etwas anderes als „nicht aufgezeichnet".
 */
function Balken({ anteil }: { anteil: number | null }) {
  if (anteil === null) {
    return <span className="w-20 shrink-0 sm:w-28" />;
  }
  return (
    <span
      aria-hidden="true"
      className="bg-muted block h-1.5 w-20 shrink-0 overflow-hidden rounded-full sm:w-28"
    >
      <span
        className="bg-muted-foreground block h-full rounded-full"
        style={{ width: `${(anteil * 100).toFixed(2)}%` }}
      />
    </span>
  );
}

/**
 * Die Wartezeit **zwischen** zwei Schritten.
 *
 * Ohne diese Zeile steht sie in keiner Schrittdauer — und genau sie ist bei
 * einer hängenden Nachricht oft die ganze Antwort.
 */
function LueckenZeile({ sekunden }: { sekunden: number }) {
  const texte = useTexte();
  return (
    <li className="border-border text-muted-foreground text-beiwerk flex items-center gap-1.5 border-l-2 py-1 pl-2">
      <Hourglass aria-hidden="true" className="size-3.5 shrink-0 opacity-70" />
      {einsetzen(texte.nachrichten.detail.gewartet, {
        dauer: formatiereDauer(sekunden, texte.nachrichten.detail.dauer),
      })}
    </li>
  );
}

/**
 * Das Ende der Leiste bei `WARTET_VOR`. **Die Nachricht wartet — und das steht
 * da**, erkennbar anders als die ausgeführten Schritte.
 *
 * Drei Fälle, und alle drei kommen aus zwei gelieferten Feldern:
 *
 * 1. **Kein nächster Schritt** (`naechsterSchritt === null`, über den
 *    Gesamtbestand 43,9 Prozent der Verweise): Das wird benannt und nicht
 *    weggelassen. Die Nachricht wartet, wir wissen nur nicht worauf; sie
 *    stillschweigend wie eine abgeschlossene aussehen zu lassen wäre die
 *    schlechtere Auskunft.
 * 2. **Der benannte Schritt ist bereits gelaufen** — der Fall der Testkopie
 *    (Sichtprüfung 07.08.2026, siehe `../detail.ts`). Dann wird sein Name
 *    **nicht wiederholt**: Er steht eine Zeile darüber, mit seiner Dauer. Die
 *    Zeile sagt nur noch, dass es hier nicht von selbst weitergeht; der Verweis
 *    bleibt im Tooltip nachlesbar.
 * 3. **Ein anderer Schritt**: Er steht als noch nicht begonnen da.
 */
function ErwarteteZeile({
  name,
  bereitsGelaufen,
}: {
  name: string | null;
  bereitsGelaufen: boolean;
}) {
  const texte = useTexte();

  if (name === null) {
    return (
      <li className="border-border text-muted-foreground text-beiwerk flex items-center gap-1.5 border-l-2 border-dashed py-1 pl-2">
        <Clock aria-hidden="true" className="size-3.5 shrink-0" />
        {texte.nachrichten.detail.wartetVorUnbekannt}
      </li>
    );
  }

  if (bereitsGelaufen) {
    return (
      <li
        className="border-border text-muted-foreground text-beiwerk flex items-center gap-1.5 border-l-2 border-dashed py-1 pl-2"
        title={einsetzen(texte.nachrichten.detail.verweistAuf, { schritt: name })}
      >
        <Clock aria-hidden="true" className="size-3.5 shrink-0" />
        {texte.nachrichten.detail.wartetWeiterhin}
      </li>
    );
  }

  return (
    <li className="border-border h-zeile text-muted-foreground flex items-center gap-2 border-l-2 border-dashed pl-2">
      <Clock aria-hidden="true" className="size-3.5 shrink-0" />
      <span className="min-w-0 flex-1 truncate italic" title={name}>
        {name}
      </span>
      <span className="text-beiwerk shrink-0">{texte.nachrichten.detail.nochNichtBegonnen}</span>
    </li>
  );
}
