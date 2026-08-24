"use client";

import { Marke } from "@/components/marke";
import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZeitpunkt } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Katalogzeile } from "../api";
import { istAuffangprozess } from "../filter";

/**
 * Eine Zeile der Pflegeliste in ihrer **Anzeigeform**.
 *
 * ## Was hier keine Farbe trägt, und warum
 *
 * Weder Pflegestatus noch Bestand noch Richtung bekommen eine Farbrolle. Die
 * vier Statusfarben des Projekts sind fachlich an den **Nachrichtenstatus**
 * vergeben (`docs/visuelles-konzept.md` §3), und *„es gibt keine zweite
 * Bedeutung von Grün oder Rot"*. Ein grünes „gepflegt" neben einem grünen
 * „abgeschlossen" hieße zweierlei mit demselben Zeichen.
 *
 * Die Unterscheidung liegt deshalb vollständig im **Wort**. Das erfüllt die
 * Regel „nie allein über Farbe" nicht knapp, sondern trivial: Es ist gar keine
 * Farbe im Spiel.
 *
 * ## Die drei Zustände von `traegtNachrichten` (E14)
 *
 * | Wert | Anzeige |
 * |---|---|
 * | `true` | trägt Nachrichten |
 * | `false` | trägt keine Nachrichten |
 * | `null` | **nicht geprüft** — und ausdrücklich nicht dasselbe wie `false` |
 *
 * `false` und `null` stehen beide gedämpft, sagen aber Verschiedenes. Der
 * Unterschied ist der ganze Grund für die Spalte: Bei `VOTG` tragen 350 von 390
 * Prozessen keine einzige Nachricht (M83‑5) — der Kurator schreibt dort 350-mal
 * eine Zuordnung zu einem Vertrag, der nichts produziert, und das ist eine
 * andere Aussage als dieselbe Zeile bei einem Prozess mit Verkehr.
 *
 * ## Der Auffangprozess (E16)
 *
 * Erkannt über die `ProcessID` und **niemals über `^0+_`** — die Begründung und
 * die Messung stehen an {@link istAuffangprozess}. Er wird gekennzeichnet und
 * zählt ganz normal mit; er ist kein Sonderfall der Menge, sondern einer der
 * Benennung.
 */
export function KatalogZeile({
  zeile,
  aktionen,
}: {
  zeile: Katalogzeile;
  aktionen?: React.ReactNode;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  return (
    <>
      <td className="px-2 py-1.5 align-top">
        <span className="block font-mono break-all">{zeile.processId}</span>
        <span className="text-muted-foreground text-beiwerk block break-words">
          {zeile.processName ?? texte.katalog.ohneNamen}
        </span>
        {istAuffangprozess(zeile.processId) ? (
          <Marke className="mt-1 inline-block">
            <span title={texte.katalog.auffangprozessHinweis}>{texte.katalog.auffangprozess}</span>
          </Marke>
        ) : null}
      </td>

      <td className="text-muted-foreground hidden px-2 py-1.5 align-top lg:table-cell">
        <span className="block font-mono break-all">{zeile.projectId}</span>
        <span className="text-beiwerk block break-words">
          {zeile.projectName ?? texte.katalog.ohneNamen}
        </span>
      </td>

      <td className="px-2 py-1.5 align-top">
        <PartnerAnzeige zeile={zeile} />
      </td>

      <td className="hidden px-2 py-1.5 align-top md:table-cell">
        {zeile.richtung === null ? (
          <span className="text-muted-foreground">{texte.katalog.nichtZugeordnet}</span>
        ) : (
          texte.katalog.richtungen[zeile.richtung]
        )}
      </td>

      <td className="hidden px-2 py-1.5 align-top md:table-cell">
        <BestandAnzeige zeile={zeile} sprache={sprache} zone={zone} />
      </td>

      <td className="px-2 py-1.5 align-top">
        <span className="flex flex-col items-start gap-1">
          <span className={cn(zeile.pflegestatus === "OFFEN" && "text-muted-foreground")}>
            {texte.katalog.pflegestatus[zeile.pflegestatus]}
          </span>
          {aktionen}
        </span>
      </td>
    </>
  );
}

/**
 * Partner samt Vermerk — die **drei Bedeutungen aus zwei Feldern** (E4).
 *
 * | Pflegestatus | Feld | Vermerk |
 * |---|---|---|
 * | `OFFEN` | leer, Herkunft `KEINE` | „kein Vorschlag ableitbar" (E9) |
 * | `OFFEN` | leer, Herkunft gesetzt | keiner — der Fall kommt nicht vor |
 * | `OFFEN` | gefüllt | „Vorschlag" — unbestätigt |
 * | `GEPFLEGT` | gefüllt | keiner — das ist der Normalfall |
 * | `GEPFLEGT` | leer | „hingesehen, es gibt keinen" |
 *
 * **Der leere gepflegte Partner braucht seinen Vermerk am dringendsten.** Ohne
 * ihn sähe die Zeile aus wie eine unbearbeitete — und genau dieser Zustand ist
 * die einzige Pflege, die ein toter Prozess je bekommt.
 */
function PartnerAnzeige({ zeile }: { zeile: Katalogzeile }) {
  const texte = useTexte();
  const vermerk =
    zeile.partner === null
      ? zeile.pflegestatus === "GEPFLEGT"
        ? texte.katalog.vermerk.ohnePartner
        : zeile.vorschlagHerkunft === "KEINE"
          ? texte.katalog.vermerk.keinVorschlag
          : null
      : zeile.pflegestatus === "OFFEN"
        ? texte.katalog.vermerk.vorschlag
        : null;

  return (
    <>
      <span className={cn("block break-words", zeile.partner === null && "text-muted-foreground")}>
        {zeile.partner ?? texte.katalog.nichtZugeordnet}
      </span>
      {vermerk === null ? null : (
        <span className="text-muted-foreground text-beiwerk block break-words">{vermerk}</span>
      )}
    </>
  );
}

/**
 * Der Bestand einer Zeile — das Wort, und darunter das Alter der Erhebung.
 *
 * **Das Alter steht an der Zeile und nicht über der Liste.** Das Backend
 * liefert dafür ausdrücklich kein Feld im Umschlag: Die Angabe gehört an die
 * Zeile, die sie beschreibt (`docs/prozess-katalog-backend.md` §4). Nach einem
 * Lauf tragen alle Zeilen denselben Zeitpunkt; davor können sie sich
 * unterscheiden, und dann ist der Unterschied die Auskunft.
 */
function BestandAnzeige({
  zeile,
  sprache,
  zone,
}: {
  zeile: Katalogzeile;
  sprache: ReturnType<typeof useSprache>;
  zone: string;
}) {
  const texte = useTexte();

  if (zeile.traegtNachrichten === null) {
    return (
      <span className="text-muted-foreground block" title={texte.katalog.bestand.nieGeprueft}>
        {texte.katalog.bestand.ungeprueft}
      </span>
    );
  }

  const geprueft =
    zeile.bestandGeprueftAm === null
      ? null
      : einsetzen(texte.katalog.bestand.geprueftAm, {
          zeitpunkt: formatiereZeitpunkt(zeile.bestandGeprueftAm, sprache, zone),
        });

  return (
    <>
      <span className={cn("block", !zeile.traegtNachrichten && "text-muted-foreground")}>
        {zeile.traegtNachrichten ? texte.katalog.bestand.traegt : texte.katalog.bestand.traegtNicht}
      </span>
      {geprueft === null ? null : (
        <span className="text-muted-foreground text-beiwerk block">{geprueft}</span>
      )}
    </>
  );
}
