"use client";

import { Marke } from "@/components/marke";
import { useSuchsignal } from "@/components/suchsignal";
import { useTexte } from "@/i18n/provider";

import type { BamTyp } from "../api";
import { useBamTypen } from "../hooks";
import { alsParameter, type Suchbegriff } from "../suche";

/**
 * Die Begriffe als **Marken**, mit `UND` verknüpft.
 *
 * ## Warum die Marken hier stehen und nicht am Feld
 *
 * Das Feld steht in der Kopfzeile und ist 18 rem breit; acht Marken passen dort
 * nicht hin. Vor allem aber gehören sie neben das Ergebnis, das sie erzeugen —
 * die Trefferspalte zeigt bewusst **keinen** Wert, weil er hier steht
 * (`treffer-tabelle.tsx`).
 *
 * ## Ein doppelter Begriff wird nicht abgelegt
 *
 * Gleicher Typ **und** gleicher Wert: Die vorhandene Marke hebt sich kurz hervor,
 * es entsteht keine zweite. **Der Schlüssel ist deshalb `(typ, wert)` und niemals
 * der Wert allein** — bei 4,17 Prozent der Paare steht derselbe Wert unter
 * mehreren Typen (M37). Zwei Marken mit demselben React-Schlüssel wären eine
 * Meldung in der Konsole und sonst nichts Sichtbares; genau dafür gibt es
 * `tests/suche-marken.test.tsx`.
 *
 * ## Die Marke ist hier ein Bedienelement — die im Detail ist es nicht
 *
 * Dieselbe Gestalt (`components/marke.tsx`), aber mit Schließen-Schaltfläche. Die
 * Bedienbarkeit ist ein Schalter und springt nicht von selbst an: Der
 * Belegdaten-Block gibt keine mit und bleibt damit kein Knopf
 * (`docs/bam-werte.md` §11).
 */
export function MarkenLeiste({
  begriffe,
  aufBegriffe,
}: {
  begriffe: Suchbegriff[];
  aufBegriffe: (begriffe: Suchbegriff[]) => void;
}) {
  const texte = useTexte();
  const { doppelt } = useSuchsignal();
  const typen = useBamTypen().data ?? [];

  return (
    <ul aria-label={texte.suche.marken.bezeichnung} className="flex flex-wrap items-center gap-1.5">
      {begriffe.map((begriff) => {
        const schluessel = alsParameter(begriff);
        return (
          <li key={schluessel} data-begriff={schluessel}>
            <Marke
              hervorgehoben={doppelt === schluessel}
              entfernenText={texte.suche.marken.entfernen}
              aufEntfernen={() =>
                aufBegriffe(begriffe.filter((eintrag) => alsParameter(eintrag) !== schluessel))
              }
            >
              <Begriffstext begriff={begriff} typen={typen} />
            </Marke>
          </li>
        );
      })}
    </ul>
  );
}

/**
 * Was in einer Marke steht: die Belegart, wenn eine gewählt war, und der Wert.
 *
 * **Der Wert läuft in fester Laufweite** — er ist eine Nummer und keine Prosa,
 * und steht damit in derselben Schrift wie die Belegnummern im Detail und die
 * Zeitpunkte in der Liste.
 *
 * **Eine Belegart ohne Beschriftung erscheint als Typnummer.** Das kommt vor: Ein
 * geteilter Link kann einen Typ tragen, den dieser Mandant nicht konfiguriert
 * hat. Sichtbar unfertig statt lautlos weggelassen — dieselbe Regel wie bei
 * `Typbezeichnung` im Backend.
 */
function Begriffstext({ begriff, typen }: { begriff: Suchbegriff; typen: BamTyp[] }) {
  if (begriff.typ === null) {
    return <span className="font-mono">{begriff.wert}</span>;
  }
  const bezeichnung =
    typen.find((eintrag) => eintrag.typ === begriff.typ)?.bezeichnung ?? String(begriff.typ);
  return (
    <>
      <span className="text-muted-foreground">{bezeichnung}: </span>
      <span className="font-mono">{begriff.wert}</span>
    </>
  );
}
