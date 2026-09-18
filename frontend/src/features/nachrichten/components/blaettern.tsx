"use client";

import { ChevronLeft, ChevronRight, Sigma } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Button } from "@/components/ui/button";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl, formatiereZeitpunktGenau } from "@/lib/format";

import type { Treffer } from "../aktualisierung";

/**
 * Blättern und der Stand der Liste.
 *
 * ## Keine Seitenzahlen
 *
 * Es gibt keine Gesamtzahl — ein `COUNT` über `Message` wäre genau die
 * Live-Aggregation, die Regel L2 verbietet, und über ein Jahresfenster kostete er
 * mehr als die Seite selbst. Eine erfundene Zahl wäre schlimmer als keine, also
 * gibt es „vorwärts" und „rückwärts" und sonst nichts.
 *
 * ## Der Stand bleibt hier — Schalter und Knopf sind nach oben gewandert *(16.09.2026)*
 *
 * **Der Zeitpunkt der letzten Aktualisierung ist immer sichtbar**, auch bei
 * manueller Bedienung. Ohne ihn weiß niemand, ob er auf Daten von vor drei
 * Sekunden oder von vor drei Stunden schaut — und genau das ist die Frage, wegen
 * der jemand dieses Werkzeug öffnet. Er steht weiterhin unter der Liste (E‑171).
 *
 * **Bis zum 16.09.2026 standen hier auch der Schalter der automatischen
 * Aktualisierung, der Hinweis, dass sie beim Blättern pausiert, und ein Knopf
 * „Jetzt aktualisieren".** Der Schalter steht seither samt Pausenanzeige im Kopf
 * der Nachrichtenliste, direkt vor „Neu laden" (`components/neu-laden.tsx`,
 * E‑163, E‑171) — so ist er auch dann erreichbar, wenn die Liste lang ist. Der
 * Knopf ist entfallen (E‑172): Er holte die *aktuelle* Seite neu, auch Seite
 * sieben, und damit genau den Cursor, den „Neu laden" vermeidet (E‑168). Zwei
 * Knöpfe, die dasselbe versprechen und Verschiedenes tun, wären schlechter als
 * einer.
 *
 * ## Der Σ — bis hier gezählt *(18.09.2026, E‑216)*
 *
 * **Am linken Rand steht seither, wie viele Zeilen bis zur angezeigten Seite
 * angekommen sind.** Das ist keine Gesamtzahl, und der Satz oben bleibt wahr:
 * „Sonst nichts" gilt dem Blättern, und Seitenzahlen gibt es weiterhin nicht.
 * Solange weitere Seiten folgen, heißt die Zahl „mehr als"; genau ist sie erst
 * auf der letzten. Gerechnet wird sie nicht hier, sondern in `aktualisierung.ts`
 * (`trefferBisHier`) — dieser Block zeigt nur an.
 *
 * Das Zeichen ist ein Symbol und für Vorleseprogramme verborgen; vorgelesen wird
 * „Treffer:" samt Zahl. **Kein `aria-live`:** Die automatische Aktualisierung
 * rechnet die Zahl alle sechzig Sekunden neu, und jede Änderung vorzulesen hieße,
 * dem Nutzer ins Wort zu fallen. Der Stand daneben behält seines.
 */
export function Blaettern({
  kannZurueck,
  kannVor,
  aufZurueck,
  aufVor,
  standVon,
  laeuft,
  treffer,
}: {
  kannZurueck: boolean;
  kannVor: boolean;
  aufZurueck: () => void;
  aufVor: () => void;
  /** Zeitstempel der letzten erfolgreichen Antwort, `0` solange es keine gibt. */
  standVon: number;
  laeuft: boolean;
  /**
   * **Die Treffer bis hier** (Σ, E‑216). **Nur im Datenzustand**; ohne Angabe
   * steht kein Σ. Welcher Zustand gilt, weiß die Ansicht und nicht dieser Block —
   * deshalb ist die Angabe Pflicht, und jeder Verwender entscheidet sie
   * ausdrücklich.
   */
  treffer: Treffer | undefined;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();
  const t = texte.nachrichten.blaettern;

  const stand =
    standVon === 0
      ? texte.nachrichten.aktualisierung.standUnbekannt
      : einsetzen(texte.nachrichten.aktualisierung.stand, {
          zeit: formatiereZeitpunktGenau(new Date(standVon).toISOString(), sprache, zone),
        });

  return (
    <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
      <div className="text-muted-foreground text-beiwerk flex flex-wrap items-center gap-x-4 gap-y-1">
        {treffer === undefined ? null : (
          <span
            className="inline-flex items-center gap-1 tabular-nums"
            title={treffer.genau ? undefined : t.trefferMehrAlsHinweis}
          >
            <Sigma aria-hidden="true" className="size-3.5 shrink-0" />
            <span className="sr-only">{t.treffer}</span>{" "}
            {treffer.genau
              ? formatiereZahl(treffer.anzahl, sprache)
              : einsetzen(t.trefferMehrAls, { zahl: formatiereZahl(treffer.anzahl, sprache) })}
          </span>
        )}
        <span aria-live="polite">{laeuft ? texte.nachrichten.aktualisierung.laeuft : stand}</span>
      </div>

      <div className="flex items-center gap-1">
        <Button
          type="button"
          variant="outline"
          className="min-h-bedienelement"
          disabled={!kannZurueck}
          onClick={aufZurueck}
        >
          <ChevronLeft aria-hidden="true" />
          <span className="hidden sm:inline">{texte.nachrichten.blaettern.zurueck}</span>
          <span className="sr-only sm:hidden">{texte.nachrichten.blaettern.zurueck}</span>
        </Button>
        <Button
          type="button"
          variant="outline"
          className="min-h-bedienelement"
          disabled={!kannVor}
          onClick={aufVor}
        >
          <span className="hidden sm:inline">{texte.nachrichten.blaettern.vor}</span>
          <span className="sr-only sm:hidden">{texte.nachrichten.blaettern.vor}</span>
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </div>
  );
}
