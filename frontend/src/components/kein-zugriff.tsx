"use client";

import { Lock } from "lucide-react";

import { useTexte } from "@/i18n/provider";

/**
 * **Kein Zugriff** — der Zustand, den eine Ansicht braucht, deren Endpunkt die
 * Rolle verlangt (`docs/frontend-grundlagen.md` §2).
 *
 * Er steht neben Laden, Leer und Fehler aus `components/zustand.tsx` und ist
 * bewusst keiner von ihnen:
 *
 * - **Kein Fehler.** Rot heißt in diesem Werkzeug „da ist etwas kaputt". Hier
 *   ist nichts kaputt; der Nutzer steht nur vor einer Grenze, die für ihn gilt.
 *   Eine rote Meldung lädt zum Melden ein, und es gibt nichts zu melden.
 * - **Kein Leerzustand.** „Nichts anzuzeigen" wäre die Unwahrheit: Es gibt
 *   etwas, er darf es nur nicht sehen.
 * - **Kein zweiter Versuch.** `403` steht schon beim ersten Aufruf fest
 *   (`lib/query-client.ts`), und ein Knopf „Erneut versuchen" verspräche, dass
 *   sich daran etwas ändern ließe.
 *
 * **Der Satz kommt aus dem Fehlerkatalog, nicht von hier.**
 * `fehler["zugriff-verweigert"]` ist der Problemtyp, den das Backend liefert;
 * er wird an dieser Stelle ohne Umweg über `fehleranzeige` gelesen, weil dieser
 * Baustein auch dort steht, wo gar keine Antwort vorliegt. Ein zweiter eigener
 * Wortlaut daneben wäre genau die Doppelpflege, vor der
 * `docs/frontend-grundlagen.md` §6 warnt.
 *
 * **Für den 404-Text gilt das Gegenteil und es bleibt dabei:** Dort darf das
 * Wort „Zugriff" nicht vorkommen, und `tests/sprachdateien.test.ts` erzwingt
 * das. Hier *ist* fehlende Berechtigung die Wahrheit und darf benannt werden —
 * es sind zwei Schlüssel, und das ist der ganze Grund.
 */
export function KeinZugriff() {
  const texte = useTexte();

  return (
    <div className="border-border bg-card text-muted-foreground flex flex-col items-center gap-2 rounded-lg border px-4 py-10 text-center">
      <Lock aria-hidden="true" className="size-6 opacity-60" />
      <p className="text-foreground font-medium">{texte.zustand.keinZugriffTitel}</p>
      <p className="text-beiwerk max-w-prose">{texte.fehler["zugriff-verweigert"]}</p>
    </div>
  );
}
