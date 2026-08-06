"use client";

import { ArrowDown, ArrowUp } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereRelativ, formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { BamWerte, Nachricht } from "../api";
import type { Sortierung } from "../filter";
import { StatusPlakette } from "./status-plakette";

/**
 * Die Nachrichtenliste.
 *
 * ## Die Spalten
 *
 * `Zeitpunkt · Status · Prozess · Projekt · BAM 1 · BAM 2`
 *
 * **Keine `MessageID`-Spalte.** Eine `varchar(36)`-UUID widerspricht dem Leitsatz
 * „interne IDs sind Beiwerk", und ohne Kopierfunktion trägt sie nichts. Sie kommt
 * in Schritt 5 wieder, wenn es ein Detail gibt, auf das sie zeigt.
 *
 * **Die BAM-Überschriften kommen aus der Antwort**, nicht aus einer festen Liste:
 * Welche zwei BAM-Typen ein Mandant sieht, entscheidet seine Konfiguration. Hat
 * er keine (`EDITIONLINGERI`, `SYSTEM`, `WOC`), fehlen beide Spalten und die
 * Tabelle hat vier — eine leere Spalte behauptete, es gäbe dort etwas zu sehen.
 *
 * ## Der Zeilenklick hat keine Funktion
 *
 * Kein Panel, kein Kopieren, **kein Hover-Zustand, der eine Interaktion
 * verspricht** — deshalb ist die Hover-Färbung, die `components/ui/table`
 * mitbringt, hier ausdrücklich abgeschaltet. Schritt 5 belegt den Klick; bis
 * dahin wäre ein Anfassgefühl ohne Wirkung schlimmer als gar keins.
 *
 * ## Am schmalen Fenster
 *
 * Zuerst fällt **BAM 2** weg, dann **Projekt**. Übrig bleiben Zeitpunkt, Status,
 * Prozess und BAM 1 — die vier, mit denen sich ein Beleg wiedererkennen lässt.
 * Der aktive Mandant bleibt bei jeder Breite in der Kopfzeile sichtbar; das
 * entscheidet der Anwendungsrahmen, nicht diese Tabelle.
 */
export function NachrichtenTabelle({
  zeilen,
  sortierung,
  aufSortierung,
}: {
  zeilen: Nachricht[];
  sortierung: Sortierung;
  aufSortierung: (sortierung: Sortierung) => void;
}) {
  const texte = useTexte();

  // Jede Zeile trägt so viele BAM-Einträge, wie der Mandant Spalten hat — auch
  // die Zeile, die dazu nichts zu sagen hat. Die erste genügt deshalb, um die
  // Überschriften zu bestimmen.
  const bamSpalten = zeilen[0]?.bamWerte ?? [];

  return (
    <Table className="text-basis">
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead className="w-[11.5rem]">
            <SortierUmschalter sortierung={sortierung} aufSortierung={aufSortierung} />
          </TableHead>
          <TableHead className="w-[10.5rem]">{texte.nachrichten.spalten.status}</TableHead>
          <TableHead>{texte.nachrichten.spalten.prozess}</TableHead>
          <TableHead className="hidden md:table-cell">
            {texte.nachrichten.spalten.projekt}
          </TableHead>
          {bamSpalten.map((spalte, stelle) => (
            <TableHead
              key={spalte.typ}
              // Zuerst fällt BAM 2 weg, dann Projekt: Die zweite BAM-Spalte
              // erscheint deshalb erst später als das Projekt.
              className={cn(stelle === 1 && "hidden lg:table-cell")}
            >
              {spalte.beschreibung}
            </TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {zeilen.map((zeile) => (
          // Kein onClick, kein cursor-pointer, keine Hover-Fläche.
          <TableRow key={zeile.messageId} className="h-zeile hover:bg-transparent">
            <TableCell className="align-middle">
              <ZeitpunktZelle wert={zeile.zeitpunkt} />
            </TableCell>
            <TableCell className="align-middle">
              <StatusPlakette
                statusKind={zeile.statusKind}
                rohwert={zeile.status}
                bedeutungNichtVerifiziert={zeile.bedeutungNichtVerifiziert}
              />
            </TableCell>
            <TableCell className="align-middle">
              <Name wert={zeile.processName} />
            </TableCell>
            <TableCell className="text-muted-foreground hidden align-middle md:table-cell">
              <Name wert={zeile.projectName} />
            </TableCell>
            {zeile.bamWerte.map((spalte, stelle) => (
              <TableCell
                key={spalte.typ}
                className={cn("align-middle", stelle === 1 && "hidden lg:table-cell")}
              >
                <BamZelle spalte={spalte} />
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

/**
 * Sortiert wird ausschließlich über den Zeitpunkt, Richtung wählbar — deshalb
 * sitzt die Umschaltung in dieser einen Spaltenüberschrift und nicht in einem
 * eigenen Auswahlfeld. Für Status oder Prozessname gibt es weder einen Index noch
 * einen eindeutigen Tiebreaker; ein Cursor darauf würde Zeilen überspringen oder
 * doppelt liefern.
 */
function SortierUmschalter({
  sortierung,
  aufSortierung,
}: {
  sortierung: Sortierung;
  aufSortierung: (sortierung: Sortierung) => void;
}) {
  const texte = useTexte();
  const neueste = sortierung === "neueste";
  const Pfeil = neueste ? ArrowDown : ArrowUp;

  return (
    <button
      type="button"
      onClick={() => aufSortierung(neueste ? "aelteste" : "neueste")}
      title={texte.nachrichten.sortierungUmschalten}
      aria-label={`${texte.nachrichten.spalten.zeitpunkt} — ${
        neueste ? texte.nachrichten.sortierungNeueste : texte.nachrichten.sortierungAelteste
      }`}
      className="focus-visible:ring-ring -mx-1 inline-flex items-center gap-1 rounded-sm px-1 focus-visible:ring-2 focus-visible:outline-none"
    >
      {texte.nachrichten.spalten.zeitpunkt}
      <Pfeil aria-hidden="true" className="size-3.5 opacity-70" />
    </button>
  );
}

/**
 * Zeit **absolut** in der Zelle, **relativ** im Tooltip.
 *
 * Der absolute Wert ist der, den man gegen das Altwerkzeug hält und in eine
 * Störungsmeldung schreibt; der relative sagt auf einen Blick, ob etwas gerade
 * eben passiert ist. Umgekehrt wäre es falsch herum: „vor 3 Stunden" lässt sich
 * nicht weitergeben.
 *
 * **Der Tooltip ist ein `title`-Attribut und keine Bibliothekskomponente.** Das
 * visuelle Konzept lässt außer Schublade und Menü keine Bewegung zu, und ein
 * eingeblendeter Tooltip brächte genau die mit. Für Vorleseprogramme steht
 * derselbe Text zusätzlich verborgen im Markup.
 */
function ZeitpunktZelle({ wert }: { wert: string }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const relativ = formatiereRelativ(wert, sprache);
  const genau = formatiereZeitpunktGenau(wert, sprache, zone);

  return (
    <time dateTime={wert} title={`${genau} · ${relativ}`} className="whitespace-nowrap">
      {formatiereZeitpunkt(wert, sprache, zone)}
      <span className="sr-only">
        {" "}
        — {texte.nachrichten.zeitRelativ}: {relativ}
      </span>
    </time>
  );
}

/** „Nicht zugeordnet heißt nicht zugeordnet" (Regel Q4) — und sieht auch so aus. */
function Name({ wert }: { wert: string | null }) {
  const texte = useTexte();
  if (wert === null || wert === "") {
    return (
      <span className="text-muted-foreground italic">{texte.nachrichten.nichtZugeordnet}</span>
    );
  }
  return <span className="break-words">{wert}</span>;
}

/**
 * Die Werte einer BAM-Spalte.
 *
 * Mehrere Werte eines Typs werden zusammengefasst — eine gesplittete
 * Sammelrechnung trägt so mehrere Lieferscheinnummern. **Was nicht mitgekommen
 * ist, wird gezählt und nicht verschwiegen:** Eine stumm gekürzte Liste sieht aus
 * wie eine vollständige, und der Nutzer suchte dann nach einer Nummer, die es
 * gibt und die er nie zu sehen bekommt.
 */
function BamZelle({ spalte }: { spalte: BamWerte }) {
  const texte = useTexte();
  const sprache = useSprache();

  if (spalte.werte.length === 0) {
    return <span className="text-muted-foreground">{texte.nachrichten.ohneWert}</span>;
  }

  return (
    <div className="flex flex-col gap-0.5">
      {spalte.werte.map((wert) => (
        <span key={wert} className="text-beiwerk font-mono break-all">
          {wert}
        </span>
      ))}
      {spalte.weitere > 0 ? (
        <span className="text-muted-foreground text-beiwerk">
          {einsetzen(texte.nachrichten.bamWeitere, {
            anzahl: new Intl.NumberFormat(sprache).format(spalte.weitere),
          })}
        </span>
      ) : null}
    </div>
  );
}
