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
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereRelativ, formatiereZeitpunktGenau } from "@/lib/format";

import type { Nachricht } from "../api";
import type { Sortierung } from "../filter";
import { StatusPlakette } from "./status-plakette";

/**
 * Die Nachrichtenliste.
 *
 * ## Die Spalten
 *
 * `Zeitpunkt · Status · Ablauf · Projekt`
 *
 * **Die BAM-Spalten sind weg.** Sie sollten das sein, woran ein Sachbearbeiter
 * seinen Beleg wiedererkennt, und waren im Betrieb leer: Messung M11 weist die
 * beiden für `NEXANS` kuratierten Typen auf 98,93 und 96,97 Prozent der Zeilen
 * als leer aus, und **kein einziger** der 40 konfigurierten Typen erreicht ein
 * Fünftel der Nachrichten. Eine Spalte, die fast immer leer ist, behauptet, es
 * gäbe dort etwas zu sehen — und kostet den Platz, den die übrigen brauchen.
 *
 * **„Ablauf" zeigt `sosName`, nicht `processName`.** Die Projektbeschreibung
 * §3.2 legt `SOSName` als den Anzeigenamen fest, und er ist durchgängig in
 * Klartext gepflegt (Messung L14: 1.818 Zeilen, kein leerer Wert, auf allen
 * 180.251 Nachrichten des dichten Monats auflösbar). `ProcessName` ist nur
 * zufällig lesbar — „Kunde A Lieferschein (VDA)" steht dort neben
 * „KUNDE_B_MX_000000_LAB". Er geht nicht verloren: Er steht im Tooltip und
 * für Vorleseprogramme verborgen im Markup, und der Freitextfilter durchsucht
 * ihn weiterhin.
 *
 * **Keine `MessageID`-Spalte.** Eine `varchar(36)`-UUID widerspricht dem Leitsatz
 * „interne IDs sind Beiwerk", und ohne Kopierfunktion trägt sie nichts. Sie kommt
 * in Schritt 5 wieder, wenn es ein Detail gibt, auf das sie zeigt.
 *
 * ## Der Zeitpunkt trägt Sekunden
 *
 * `29.12.2025, 23:39:14`. Auf dem Bild des Auftraggebers standen mehrfach zwei
 * Zeilen mit identischem Zeitpunkt, Prozess und Projekt nebeneinander — für den
 * Nutzer nicht unterscheidbar. Für ein Werkzeug, dessen Leitfrage „wo ist mein
 * Beleg" lautet, sind zwei ununterscheidbare Zeilen so schädlich wie eine leere
 * Spalte. Der relative Abstand steht weiterhin im Tooltip; er ist die Ergänzung,
 * nie der Ersatz.
 *
 * ## Feste Zeilenhöhe — die Regel für jede Spalte
 *
 * Die Tabelle liegt in `table-fixed`, jede Zeile ist `h-zeile` hoch
 * (`--dichte-zeile`, `docs/visuelles-konzept.md` §5), und **jeder** Zellinhalt
 * wird auf eine Zeile gekürzt; der Vollwert steht im `title`. Das gilt
 * ausdrücklich nicht nur für die heutigen vier Spalten: Ohne diese Regel zerreißt
 * der nächste lange Wert die Liste wieder, so wie es die mehrwertigen BAM-Zellen
 * getan haben. Der längste gemessene Ablaufname hat 55 Zeichen (L14).
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
 * Zuerst und einzig fällt **Projekt** weg (unter `md`). Übrig bleiben Zeitpunkt,
 * Status und Ablauf — die drei, mit denen sich ein Beleg wiederfinden lässt. Der
 * aktive Mandant bleibt bei jeder Breite in der Kopfzeile sichtbar; das
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

  return (
    // `table-fixed` ist die Voraussetzung der festen Zeilenhöhe: Nur mit festen
    // Spaltenbreiten hat eine Zelle eine Breite, auf die sich kürzen lässt.
    <Table className="text-basis table-fixed">
      <TableHeader>
        <TableRow className="hover:bg-transparent">
          <TableHead className="h-8 w-[11.5rem]">
            <SortierUmschalter sortierung={sortierung} aufSortierung={aufSortierung} />
          </TableHead>
          <TableHead className="h-8 w-[10.5rem]">{texte.nachrichten.spalten.status}</TableHead>
          {/* Ohne Breitenangabe: Der Ablaufname bekommt, was übrig bleibt. */}
          <TableHead className="h-8">{texte.nachrichten.spalten.ablauf}</TableHead>
          <TableHead className="hidden h-8 w-[18rem] md:table-cell">
            {texte.nachrichten.spalten.projekt}
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {zeilen.map((zeile) => (
          // Kein onClick, kein cursor-pointer, keine Hover-Fläche.
          <TableRow key={zeile.messageId} className="h-zeile hover:bg-transparent">
            <TableCell className="px-2 py-0 align-middle">
              <ZeitpunktZelle wert={zeile.zeitpunkt} />
            </TableCell>
            <TableCell className="px-2 py-0 align-middle">
              <StatusPlakette
                statusKind={zeile.statusKind}
                rohwert={zeile.status}
                bedeutungNichtVerifiziert={zeile.bedeutungNichtVerifiziert}
              />
            </TableCell>
            <TableCell className="px-2 py-0 align-middle">
              <AblaufZelle sosName={zeile.sosName} processName={zeile.processName} />
            </TableCell>
            <TableCell className="text-muted-foreground hidden px-2 py-0 align-middle md:table-cell">
              <span className="block truncate" title={zeile.projectName ?? undefined}>
                <Name wert={zeile.projectName} />
              </span>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

/**
 * Sortiert wird ausschließlich über den Zeitpunkt, Richtung wählbar — deshalb
 * sitzt die Umschaltung in dieser einen Spaltenüberschrift und nicht in einem
 * eigenen Auswahlfeld. Für Status oder Ablaufname gibt es weder einen Index noch
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
 * Zeit **absolut mit Sekunden** in der Zelle, **relativ** im Tooltip.
 *
 * Der absolute Wert ist der, den man gegen das Altwerkzeug hält und in eine
 * Störungsmeldung schreibt; der relative sagt auf einen Blick, ob etwas gerade
 * eben passiert ist. Umgekehrt wäre es falsch herum: „vor 3 Stunden" lässt sich
 * nicht weitergeben.
 *
 * **Die Sekunden stehen in der Zelle und nicht im Tooltip**, seit dem
 * Durchklicken des Auftraggebers: Zwei Zeilen, die sich bis zur Minute gleichen,
 * sind ohne sie nicht auseinanderzuhalten — und ein Tooltip, den man je Zeile
 * aufrufen muss, um zwei Zeilen zu unterscheiden, beantwortet die Frage nicht.
 * Auf einem Touchgerät gibt es ihn ohnehin nicht.
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

  return (
    <time dateTime={wert} title={relativ} className="block truncate">
      {formatiereZeitpunktGenau(wert, sprache, zone)}
      <span className="sr-only">
        {" "}
        — {texte.nachrichten.zeitRelativ}: {relativ}
      </span>
    </time>
  );
}

/**
 * Der Ablaufname in der Zelle, der Prozessname im Tooltip.
 *
 * Beide beschreiben, wo eine Nachricht durchläuft, und nur einer ist verlässlich
 * lesbar. Der andere geht trotzdem nicht verloren — er ist der Name, unter dem
 * ein Kollege dieselbe Zeile im Altwerkzeug wiederfindet, und der Freitextfilter
 * durchsucht ihn weiterhin. Was man sucht, sieht man damit auch.
 */
function AblaufZelle({
  sosName,
  processName,
}: {
  sosName: string | null;
  processName: string | null;
}) {
  const texte = useTexte();
  const prozess = processName === null || processName === "" ? null : processName;
  const hinweis = `${texte.nachrichten.prozessName}: ${prozess ?? texte.nachrichten.nichtZugeordnet}`;

  return (
    <span className="block truncate" title={`${sosName ?? ""}\n${hinweis}`.trim()}>
      <Name wert={sosName} />
      <span className="sr-only"> — {hinweis}</span>
    </span>
  );
}

/**
 * „Nicht zugeordnet heißt nicht zugeordnet" (Regel Q4) — und sieht auch so aus.
 *
 * **Ohne eigenen Tooltip.** Den setzt die Zelle, die diesen Namen einbettet: Ein
 * `title` hier gewönne gegen den der Zelle, und der Ablauf verlöre damit genau
 * den Prozessnamen, für den sein Tooltip da ist.
 */
function Name({ wert }: { wert: string | null }) {
  const texte = useTexte();
  if (wert === null || wert === "") {
    return (
      <span className="text-muted-foreground italic">{texte.nachrichten.nichtZugeordnet}</span>
    );
  }
  return <>{wert}</>;
}
