"use client";

import { useRef, useState, type KeyboardEvent } from "react";
import { Check, ChevronsUpDown } from "lucide-react";

import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

/**
 * Ein Eintrag der Liste. `waehlbar` ist voreingestellt wahr; eine nicht wählbare
 * Zeile **steht da** und ist nicht anklickbar — sichtbar bleiben ist die halbe
 * Auskunft (die andere steht als Satz unter dem Feld).
 */
export type Auswahleintrag<W extends string> = {
  wert: W;
  text: string;
  waehlbar?: boolean;
};

/**
 * **Ein Auswahlfeld, das der Anwendung gehört** — die Bauform, die an die Stelle
 * der nativen `<select>` tritt (Punkt 180, `docs/benutzerverwaltung-frontend.md`
 * §18).
 *
 * ## Warum es diesen Baustein gibt
 *
 * Die aufgeklappte Liste eines nativen Auswahlfelds zeichnet der Browser. Im
 * Dunkelmodus ist sie in Chrome hell bei dunklem Feld, in Firefox dunkel in
 * einem anderen Grau als die Anwendung — gemeldet vom Auftraggeber am
 * 15.09.2026 für die Rolle und am **16.09.2026 für die Baumgliederung**. **Der
 * Grund für den Umbau ist nicht der Grauton, sondern die Prüfbarkeit:** Die
 * Farbe einer nativen Liste steht in keiner Zeile von `globals.css`, und keine
 * Prüfung des Projekts erreicht sie.
 *
 * **Er steht in `components/` und nicht im Feature**, seit die zweite Stelle
 * dazugekommen ist: Die Rolle liegt in `features/benutzer`, und der nächste
 * Umbau (Mandant, Massenzuordnung) liegt woanders. Zwei Nachbauten derselben
 * Liste wären genau die Drift, gegen die der Umbau gerichtet ist.
 *
 * ## Die Bauform ist die vorhandene
 *
 * Dieselbe wie bei den Filtern der Nachrichtenliste und der Prozessauswahl:
 * `Popover` aus `components/ui`, darin eine Liste von Zeilen. **Kein neuer
 * Farbwert und kein neues Token** — die Fläche ist `--popover`, die überfahrene
 * und die fokussierte Zeile `--muted`, die gewählte `--accent`, die Zeilenhöhe
 * `--dichte-bedienzeile` (`docs/prozessauswahl.md` §7a). Das geschlossene Feld
 * trägt dieselben Klassen wie vorher das native — es sieht aus wie die Felder
 * daneben.
 *
 * ## Tastatur — das Muster „Auswahl in einer Aufklappliste" (WAI-ARIA)
 *
 * | Taste | am geschlossenen Feld | in der Liste |
 * |---|---|---|
 * | `Enter`, `Leertaste` | öffnet | wählt die Zeile und schließt |
 * | `↓`, `↑` | öffnet | nächste, vorige wählbare Zeile |
 * | `Pos1`, `Ende` | — | erste, letzte wählbare Zeile |
 * | `Escape` | — | schließt ohne Wahl, der Fokus kehrt ans Feld zurück |
 *
 * ⚠️ **Eine Abweichung vom nativen Feld, und sie ist gewollt:** In Chrome unter
 * Windows ändert `↓` am *geschlossenen* nativen Feld sofort den Wert. Hier
 * öffnet es. Beide Felder des Zeilenformulars laufen beim Umlegen los, und eine
 * Rollenänderung verwirft alle Sitzungen des Kontos (`benutzerverwaltung.md`
 * E5) — eine Pfeiltaste, die nebenbei abmeldet, ist kein Verhalten, das zu
 * erhalten wäre.
 *
 * **Die Beschriftung bleibt, wo sie war:** `<Label htmlFor>` zeigt auf das Feld,
 * und ein `<button>` ist beschriftbar wie ein `<select>`. Ein Klick auf die
 * Beschriftung öffnet die Liste.
 *
 * **Ein unbekannter Wert steht im Feld, wie er ist** (Regel Q4) — er fällt nicht
 * still auf einen der bekannten. **Eine Wahl, die nichts ändert, meldet nichts**
 * — wie das native `change`, das beim selben Wert nicht feuert.
 */
export function AuswahlFeld<W extends string>({
  id,
  beschriftung,
  wert,
  eintraege,
  aufWahl,
  gesperrt = false,
}: {
  /** Für `<Label htmlFor>` — das Feld ist der Auslöser. */
  id: string;
  /** Der zugängliche Name der aufgeklappten Liste; derselbe Text wie die Beschriftung. */
  beschriftung: string;
  /** Der gespeicherte Rohwert. Ein unbekannter steht da, wie er ist. */
  wert: string;
  eintraege: readonly Auswahleintrag<W>[];
  aufWahl: (wert: W) => void;
  gesperrt?: boolean;
}) {
  const [offen, setOffen] = useState(false);
  const liste = useRef<HTMLUListElement>(null);

  const anzeige = eintraege.find((eintrag) => eintrag.wert === wert)?.text ?? wert;

  function zeilen(): HTMLElement[] {
    return Array.from(liste.current?.querySelectorAll<HTMLElement>('[role="option"]') ?? []).filter(
      (zeile) => zeile.getAttribute("aria-disabled") !== "true",
    );
  }

  function waehle(neu: W) {
    setOffen(false);
    if (neu !== wert) {
      aufWahl(neu);
    }
  }

  function amFeld(ereignis: KeyboardEvent<HTMLButtonElement>) {
    if (!offen && (ereignis.key === "ArrowDown" || ereignis.key === "ArrowUp")) {
      ereignis.preventDefault();
      setOffen(true);
    }
  }

  function inDerListe(ereignis: KeyboardEvent<HTMLUListElement>) {
    const alle = zeilen();
    const jetzt = alle.indexOf(document.activeElement as HTMLElement);
    const ziel =
      ereignis.key === "ArrowDown"
        ? Math.min(alle.length - 1, jetzt + 1)
        : ereignis.key === "ArrowUp"
          ? Math.max(0, jetzt - 1)
          : ereignis.key === "Home"
            ? 0
            : ereignis.key === "End"
              ? alle.length - 1
              : null;
    if (ziel !== null) {
      ereignis.preventDefault();
      alle[ziel]?.focus();
    }
  }

  return (
    <Popover open={offen && !gesperrt} onOpenChange={setOffen}>
      <PopoverTrigger asChild>
        <button
          id={id}
          type="button"
          disabled={gesperrt}
          aria-haspopup="listbox"
          onKeyDown={amFeld}
          className="border-input focus-visible:border-ring focus-visible:ring-ring/50 h-feld flex w-full min-w-0 items-center justify-between gap-2 rounded-lg border bg-transparent px-2.5 py-1 text-left outline-none focus-visible:ring-3 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <span className={cn("truncate", wert === "" && "text-muted-foreground")}>{anzeige}</span>
          <ChevronsUpDown aria-hidden="true" className="size-3.5 shrink-0 opacity-60" />
        </button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) min-w-48 p-1"
        // Der Fokus geht auf die gewählte Zeile, sonst auf die erste wählbare —
        // wie beim nativen Feld, das beim Öffnen auf seinem Wert steht.
        onOpenAutoFocus={(ereignis) => {
          ereignis.preventDefault();
          const alle = zeilen();
          (
            alle.find((zeile) => zeile.getAttribute("aria-selected") === "true") ?? alle[0]
          )?.focus();
        }}
      >
        <ul ref={liste} role="listbox" aria-label={beschriftung} onKeyDown={inDerListe}>
          {eintraege.map((eintrag) => {
            const gewaehlt = eintrag.wert === wert;
            const waehlbar = eintrag.waehlbar ?? true;
            return (
              <li
                key={eintrag.wert === "" ? "leer" : eintrag.wert}
                role="option"
                data-wert={eintrag.wert}
                aria-selected={gewaehlt}
                aria-disabled={waehlbar ? undefined : true}
                tabIndex={-1}
                onClick={() => (waehlbar ? waehle(eintrag.wert) : undefined)}
                onKeyDown={(ereignis) => {
                  if (ereignis.key === "Enter" || ereignis.key === " ") {
                    ereignis.preventDefault();
                    if (waehlbar) {
                      waehle(eintrag.wert);
                    }
                  }
                }}
                className={cn(
                  "min-h-bedienzeile hover:bg-muted focus:bg-muted focus-visible:ring-ring flex cursor-pointer items-center gap-2 rounded-sm px-2 outline-none focus-visible:ring-2",
                  gewaehlt && "bg-accent",
                  !waehlbar && "cursor-not-allowed opacity-50",
                  eintrag.wert === "" && "text-muted-foreground",
                )}
              >
                {/* Das Häkchen steht bei allen im Fluss und nur beim gewählten sichtbar —
                    sonst rückte die Beschriftung beim Wählen. Dieselbe Regel wie im
                    Nutzermenü (`docs/dunkelmodus.md` §12.6). */}
                <Check
                  aria-hidden="true"
                  className={cn("size-3.5 shrink-0", gewaehlt ? "opacity-100" : "opacity-0")}
                />
                <span className="min-w-0 flex-1 py-1 break-words">{eintrag.text}</span>
              </li>
            );
          })}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
