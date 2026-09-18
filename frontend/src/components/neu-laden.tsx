"use client";

import { CirclePause, Hourglass, RefreshCw, Timer, TimerOff } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Toggle } from "@/components/ui/toggle";
import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

/**
 * Die drei Lagen des Schalters für die automatische Aktualisierung.
 *
 * **Pausiert ist eine eigene Lage und nicht „an" mit einem Hinweis daneben**
 * (E‑171): Der Schalter ist an, und trotzdem fragt nichts ab — ein Schalter, der
 * an ist und nichts tut, ist schlimmer als einer, der aus ist. Die Regel, wann
 * pausiert ist, gehört der Liste und nicht diesem Baustein
 * (`features/nachrichten/aktualisierung.ts`).
 */
export type Automatikzustand = "aus" | "an" | "pausiert";

/**
 * **Eingeschaltet trägt der Schalter die Akzentfläche** (E‑217,
 * `docs/neu-laden.md` §2) — an wie pausiert, denn pausiert ist eingeschaltet;
 * die Pause zeigt das Symbol.
 *
 * **Beide Selektoren, weil der Generator beide setzt.** `components/ui/toggle.tsx`
 * zeichnet den gedrückten Zustand zweimal aus, mit `aria-pressed:bg-muted` und
 * `data-[state=on]:bg-muted`, und Radix setzt beide Attribute
 * (`aria-pressed="true"`, `data-state="on"`). Überschrieben wird deshalb die
 * Fläche unter beiden; `cn` im Generator wirft die gleichnamigen `bg-muted`
 * hinaus. Schrift und Kontur hängen an `aria-pressed` allein, der Generator
 * setzt dort nichts.
 *
 * Fläche und Schrift sind die der gefüllten Schaltfläche (`Button`, Variante
 * `default`), die Kontur ist die aus `docs/visuelles-konzept.md` §3, *„Was diese
 * Farbe nicht kann“*. **Überfahren wie dort** (`/80`) und mit gleicher Schrift —
 * die beiden `hover`-Klassen sind spezifischer als `hover:bg-muted` und
 * `hover:text-foreground` des Generators und gewinnen unabhängig von der
 * Reihenfolge im Stylesheet. Der Fokusring bleibt der des Generators
 * (`--ring`, also `--akzent-schrift`). Keine neue Farbrolle, kein neuer Wert.
 *
 * `bg-akzent` ist **nicht** shadcns `bg-accent`: jenes ist in diesem Projekt die
 * blasse `--akzent-flaeche`.
 */
const EINGESCHALTET = [
  "aria-pressed:bg-akzent data-[state=on]:bg-akzent",
  "aria-pressed:text-akzent-vordergrund aria-pressed:border-akzent-schrift",
  "aria-pressed:hover:bg-akzent/80 aria-pressed:hover:text-akzent-vordergrund",
].join(" ");

/**
 * **„Neu laden" — und auf Wunsch der Schalter der automatischen Aktualisierung
 * davor** (`docs/neu-laden.md`).
 *
 * ## Warum er in `components/` liegt
 *
 * Drei Ansichten tragen ihn — Übersicht, Nachrichten, Prozesse —, und ein
 * Feature importiert nicht aus einem Nachbarfeature
 * (`docs/frontend-grundlagen.md` §8). Er hat die Höhe der
 * Zeitraum-Schaltflächen: `min-h-bedienelement`, dieselbe Klasse wie ein Knopf
 * des Zeitraumumschalters, und ist ebenso breit wie hoch.
 *
 * **Wo er steht, entscheidet die Ansicht** (E‑173, korrigiert E‑163): Auf
 * Übersicht und Prozessansicht unmittelbar **rechts** neben dem letzten
 * Zeitraum-Knopf, in den Nachrichten zusammen mit dem Schalter am **rechten
 * Rand** der Filterleiste.
 *
 * ## Der Schalter ist freiwillig
 *
 * Dasselbe Muster wie `aufFrei` am Zeitraumumschalter: **Ohne `automatik`
 * erscheint kein Schalter.** Nur die Nachrichtenliste gibt die Angabe mit
 * (E‑164); Übersicht und Prozessansicht laden ausschließlich von Hand.
 *
 * ## Sichtbar ist beim Knopf nur das Symbol, beim Schalter „Auto"
 *
 * Die Pfeile im Kreis sagen „neu laden" ohne Wort (E‑174, korrigiert E‑166);
 * „Auto" bleibt, weil eine Uhr allein nicht sagt, was sie schaltet. **Der
 * vollständige Name steht als zugänglicher Name und als Tooltip** — beim Knopf
 * sagt er, was die Ansicht dabei tut (in der Liste: zurück auf Seite eins),
 * beim Schalter nennt der Tooltip die Lage samt Grund.
 *
 * ## Keine Lage hängt an der Farbe, und nichts bewegt sich
 *
 * - **Lädt:** Das Symbol wechselt von den Pfeilen auf die Sanduhr, dazu
 *   `aria-busy`. **Kein Drehen und kein Übergang** — Bewegung zieht
 *   Aufmerksamkeit, und die gehört den Daten (`docs/visuelles-konzept.md` §7,
 *   E‑167). Ein Klick in dieser Lage tut nichts; der Knopf bleibt trotzdem
 *   fokussierbar und wird nicht `disabled` — ein gesperrter Knopf verlöre unter
 *   der Tastatur den Fokus.
 * - **Schalter aus, an, pausiert:** drei Symbole (Uhr durchgestrichen, Uhr,
 *   Pause), dazu `aria-pressed` über den Generatorbaustein `Toggle`. Die Fläche
 *   des gedrückten Zustands ist die des Zeitraumumschalters und keine neue
 *   Farbrolle; sie ist die halbe Aussage, nie die ganze
 *   (`docs/visuelles-konzept.md` §3).
 *
 *   *Korrigiert am 18.09.2026 (E‑217):* Eingeschaltet trägt der Schalter
 *   **nicht mehr** die Fläche des Zeitraumumschalters (`bg-muted`, 1,07 : 1,
 *   Punkt 92), sondern die Akzentfläche der gefüllten Schaltfläche — siehe
 *   {@link EINGESCHALTET}. Weiterhin keine neue Farbrolle, und die Fläche bleibt
 *   die halbe Aussage: Die Lage steht im Symbol.
 */
export function NeuLaden({
  name,
  laedt,
  aufNeuLaden,
  automatik,
}: {
  /** Der vollständige Name: zugänglicher Name und Tooltip des Knopfes. */
  name: string;
  /** Es läuft ein Abruf der Hauptdaten dieser Ansicht. */
  laedt: boolean;
  aufNeuLaden: () => void;
  /** **Freiwillig.** Ohne die Angabe gibt es keinen Schalter. */
  automatik?: {
    zustand: Automatikzustand;
    aufUmschalten: (an: boolean) => void;
  };
}) {
  const texte = useTexte();
  const t = texte.neuLaden;

  return (
    <div className="flex items-center gap-2">
      {automatik === undefined ? null : (
        <Toggle
          variant="outline"
          className={cn("min-h-bedienelement px-2.5", EINGESCHALTET)}
          pressed={automatik.zustand !== "aus"}
          onPressedChange={automatik.aufUmschalten}
          aria-label={t.automatik.name}
          title={t.automatik[automatik.zustand]}
        >
          {automatik.zustand === "aus" ? (
            <TimerOff aria-hidden="true" />
          ) : automatik.zustand === "an" ? (
            <Timer aria-hidden="true" />
          ) : (
            <CirclePause aria-hidden="true" />
          )}
          {t.automatik.knopf}
        </Toggle>
      )}

      {/* `size="icon"` macht ihn quadratisch; `min-w` hält das auch am
          Berührungsgerät, wo `min-h-bedienelement` auf 44 px springt. */}
      <Button
        type="button"
        variant="outline"
        size="icon"
        className="min-h-bedienelement min-w-bedienelement"
        aria-label={name}
        aria-busy={laedt}
        title={name}
        onClick={() => {
          // Ein zweiter Klick während des Abrufs stellt keine zweite Anfrage.
          // Die Ansicht prüft das ebenfalls — hier steht es, damit kein
          // Verbraucher es vergessen kann.
          if (!laedt) {
            aufNeuLaden();
          }
        }}
      >
        {laedt ? <Hourglass aria-hidden="true" /> : <RefreshCw aria-hidden="true" />}
      </Button>
    </div>
  );
}
