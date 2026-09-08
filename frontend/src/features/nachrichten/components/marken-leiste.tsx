"use client";

import { Marke } from "@/components/marke";
import { useSuchsignal } from "@/components/suchsignal";
import { useTexte } from "@/i18n/provider";

import type { SuchfeldBam } from "../api";
import { useSuchfelder } from "../hooks";
import { markenschluessel, type Suchbegriff, type Suchmarke } from "../suche";

/**
 * Die Begriffe als **Marken**, mit `UND` verknüpft — seit Teil 2 der
 * Property-Suche **beider Arten**: Belegnummern und Feldbegriffe.
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
 * Gleiche Art, gleicher Typ beziehungsweise Name **und** gleicher Wert: Die
 * vorhandene Marke hebt sich kurz hervor, es entsteht keine zweite. **Der
 * Schlüssel ist deshalb die Art voran, dann `(typ, wert)` oder `(name, wert)`**
 * (`suche.ts` `markenschluessel`) — bei 4,17 Prozent der Paare steht derselbe
 * Wert unter mehreren Typen (M37), und eine BAM-Marke und eine Feld-Marke sind
 * nie Dubletten voneinander. Zwei Marken mit demselben React-Schlüssel wären
 * eine Meldung in der Konsole und sonst nichts Sichtbares; genau dafür gibt es
 * `tests/suche-marken.test.tsx`.
 *
 * ## Eine Gestalt, zwei Beschriftungen
 *
 * `components/marke.tsx` bleibt unverändert — es gibt keine zweite
 * Markengestalt im Projekt. Eine BAM-Marke trägt die Belegart, falls eine
 * gewählt war, und den Wert; eine Feld-Marke den **technischen Namen
 * unverändert** (E‑105) und dahinter den Wert. Beide in derselben Anordnung:
 * gedämpft, was der Nutzer gewählt hat; in fester Laufweite, was er getippt hat.
 *
 * ## Die Marke ist hier ein Bedienelement — die im Detail ist es nicht
 *
 * Dieselbe Gestalt (`components/marke.tsx`), aber mit Schließen-Schaltfläche. Die
 * Bedienbarkeit ist ein Schalter und springt nicht von selbst an: Der
 * Belegdaten-Block gibt keine mit und bleibt damit kein Knopf
 * (`docs/bam-werte.md` §11).
 */
export function MarkenLeiste({
  marken,
  aufMarken,
}: {
  marken: Suchmarke[];
  aufMarken: (marken: Suchmarke[]) => void;
}) {
  const texte = useTexte();
  const { doppelt } = useSuchsignal();
  const belegarten = useSuchfelder().data?.bam ?? [];

  return (
    <ul aria-label={texte.suche.marken.bezeichnung} className="flex flex-wrap items-center gap-1.5">
      {marken.map((marke) => {
        const schluessel = markenschluessel(marke);
        return (
          <li key={schluessel} data-marke={schluessel}>
            <Marke
              hervorgehoben={doppelt === schluessel}
              entfernenText={texte.suche.marken.entfernen}
              aufEntfernen={() =>
                aufMarken(marken.filter((eintrag) => markenschluessel(eintrag) !== schluessel))
              }
            >
              {marke.art === "bam" ? (
                <Begriffstext begriff={marke.begriff} belegarten={belegarten} />
              ) : (
                <>
                  <span className="text-muted-foreground">{marke.feld.name}: </span>
                  <span className="font-mono">{marke.feld.wert}</span>
                </>
              )}
            </Marke>
          </li>
        );
      })}
    </ul>
  );
}

/**
 * Was in einer BAM-Marke steht: die Belegart, wenn eine gewählt war, und der Wert.
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
function Begriffstext({
  begriff,
  belegarten,
}: {
  begriff: Suchbegriff;
  belegarten: SuchfeldBam[];
}) {
  if (begriff.typ === null) {
    return <span className="font-mono">{begriff.wert}</span>;
  }
  const bezeichnung =
    belegarten.find((eintrag) => eintrag.typ === begriff.typ)?.bezeichnung ?? String(begriff.typ);
  return (
    <>
      <span className="text-muted-foreground">{bezeichnung}: </span>
      <span className="font-mono">{begriff.wert}</span>
    </>
  );
}
