"use client";

import { useId, useState } from "react";
import { ChevronRight } from "lucide-react";

import { MARKE_GESTALT } from "@/components/marke";
import { Fehler, Laden } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { zerlegeBeschriftung } from "@/lib/bam-beschriftung";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { BamGruppe } from "../api";
import { useBamWerte } from "../hooks";

/**
 * **Welcher Beleg ist das** — die Belegnummern der Nachricht, nach Art
 * gruppiert.
 *
 * ## Er sitzt zwischen Kettenblock und Zeitleiste
 *
 * Er beantwortet *welcher Beleg ist das*, die Zeitleiste beantwortet *was ist
 * damit passiert*. Nach dem Leitsatz kommt die erste Frage zuerst: Der typische
 * Nutzer ist kein EDI-Spezialist, er sucht einen Beleg. Die Belegnummern sind
 * damit die Hauptinformation dieser Ansicht und nicht Beiwerk.
 *
 * ## Ohne Belegdaten gibt es keinen Block
 *
 * Ist `bamAnzahl` null, erscheint nichts: keine Überschrift, kein Schalter, kein
 * leerer Rahmen — und **keine Anfrage auf `/bam`**. Bei **80,6 Prozent** aller
 * Nachrichten ist das der Fall, bei Merge-Eingängen bei **allen** (M41). Genau
 * dafür trägt der Detail-Endpunkt die Zahl im Kopf: Ohne sie müsste die
 * Oberfläche einen Block zeichnen, um festzustellen, dass er leer ist.
 *
 * Dieselbe Regel wie beim Kettenblock bei leeren `rollen` und beim
 * Eigenschaftenblock bei `eigenschaftenAnzahl === 0`.
 *
 * ## Eingeklappt, lädt beim Aufklappen
 *
 * Dieselbe Bauform wie der Eigenschaften-Block, aus demselben Grund — und die
 * Überschrift trägt die Zahl aus dem Kopf, damit erkennbar ist, ob sich das
 * Aufklappen lohnt. **Der Ladezustand liegt im Block, nicht im Panel:** Wer die
 * Belegdaten aufklappt, will die Zeitleiste nicht verlieren.
 *
 * ## Die Deckelung ist ehrlich
 *
 * Je Gruppe liefert das Backend höchstens zwanzig Werte und dazu die **wahre**
 * Gesamtzahl. Steht `weitereVorhanden`, erscheint die Restangabe hinter den
 * Werten. **Kein „mehr laden":** Das bräuchte einen Cursor und kommt erst, wenn
 * jemand es braucht.
 *
 * ## Kein Verweis in die Suche
 *
 * Ein Klick auf einen Wert tut nichts. Der Suchendpunkt entsteht erst in Teil 2,
 * und ein toter Verweis ist schlechter als keiner.
 */
export function BamBlock({ messageId, anzahl }: { messageId: string; anzahl: number }) {
  const texte = useTexte();
  const sprache = useSprache();
  const bereichId = useId();
  const [offen, setOffen] = useState(false);
  const anfrage = useBamWerte(messageId, offen);

  // Kein leerer Rahmen und keine Anfrage. Anders als beim Eigenschaftenblock
  // bleibt hier auch kein Satz stehen: Dass eine Nachricht keine Belegnummer
  // trägt, ist der Normalfall und keine Auskunft, für die jemand Platz opfern
  // würde.
  if (anzahl === 0) {
    return null;
  }

  const beschriftung = einsetzen(texte.nachrichten.detail.bam.titel, {
    anzahl: formatiereZahl(anzahl, sprache),
  });

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={() => setOffen((bisher) => !bisher)}
        aria-expanded={offen}
        aria-controls={bereichId}
        title={
          offen ? texte.nachrichten.detail.bam.zuklappen : texte.nachrichten.detail.bam.aufklappen
        }
        className="hover:bg-muted focus-visible:ring-ring min-h-bedienelement text-beiwerk -mx-1 flex w-fit max-w-full items-center gap-1.5 rounded-md px-1 text-left font-medium focus-visible:ring-2 focus-visible:outline-none"
      >
        {/* Eine Drehung, keine Bewegung: Das visuelle Konzept lässt außer
            Schublade und Menü keine Animation zu. */}
        <ChevronRight
          aria-hidden="true"
          className={cn("size-3.5 shrink-0 opacity-70", offen && "rotate-90")}
        />
        {beschriftung}
      </button>

      {offen ? (
        <div id={bereichId} className="flex flex-col gap-2">
          {anfrage.isPending ? (
            <Laden zeilen={3} />
          ) : anfrage.error ? (
            <Fehler fehler={anfrage.error} aufWiederholen={() => void anfrage.refetch()} />
          ) : (anfrage.data?.gruppen.length ?? 0) === 0 ? (
            <p className="text-muted-foreground text-beiwerk">
              {texte.nachrichten.detail.bam.leer}
            </p>
          ) : (
            anfrage.data?.gruppen.map((gruppe) => (
              // Der Typ ist der Schlüssel — innerhalb einer Nachricht kommt er
              // genau einmal vor, weil die Zählung darüber gruppiert.
              <Gruppe key={gruppe.typ} gruppe={gruppe} />
            ))
          )}
        </div>
      ) : null}
    </div>
  );
}

/**
 * Eine Typgruppe: **Beschriftung links, Werte rechts** — und wo nötig die
 * Restangabe hinter den Werten.
 *
 * ## Die Werte stehen nebeneinander, jeder als Marke
 *
 * **Die Marke ist kein Schmuck, sondern die Wortgrenze** — und das ist
 * gemessen, nicht angenommen: **2,97 Prozent** der BAM-Werte tragen ein
 * Leerzeichen *innen* (E7, 27.792 von 936.529 über einen Monat), angeführt von
 * 9018 — dem Typ, den `NEXANS` auf 92,26 Prozent seiner Wurzeln trägt (M39).
 * `0Z3 915 902 D` ist *ein* Wert und nicht vier.
 *
 * **Ein Trennzeichen taugt deshalb nicht.** Das naheliegende wäre das
 * Leerzeichen, und genau das steht in den Daten; für jedes andere — Komma,
 * Semikolon, Pipe — ist ungemessen, ob ein Wert es enthält, und eine Annahme
 * darüber wäre nach Regel Q4 geraten. Die Marke macht die Grenze zu einer
 * Eigenschaft der Darstellung und muss die Frage gar nicht beantworten.
 *
 * **Keine Statusfarbe.** Sie sagt nichts über einen Zustand und nutzt deshalb
 * den vorhandenen gedämpften Flächenton, nicht eine neue Farbrolle
 * (`docs/visuelles-konzept.md` §3).
 *
 * ## Die Beschriftungsspalte ist gedeckelt, nicht inhaltsbreit
 *
 * `--dichte-beschriftung`, und der Deckel ist der Punkt:
 * `Lieferantennummer beim Kunden_K_SAP` misst gegen die echte Schrift 249 px
 * und setzte inhaltsbreit die Breite für **alle** — vier kurzen Beschriftungen
 * (124 bis 166 px) nähme sie rund die Hälfte des Panels weg. So kostet eine
 * lange Beschriftung zwei Zeilen in ihrer *eigenen* Zelle und nimmt niemandem
 * Platz — schlimmstenfalls ist eine Gruppe damit so hoch wie in der gestapelten
 * Form, nie höher.
 *
 * **Warum nicht breiter:** Im Panel ist die Zeile 454 px breit, bei 10 rem
 * bleiben 282 px für die Werte — und die längste gemessene Belegnummer braucht
 * als Marke rund 285 px (35 Zeichen, M38). Eine breitere Beschriftungsspalte
 * spart der *Beschriftung* eine Zeile und zwingt dafür den **Wert** in den
 * Umbruch. Das wäre die Regel *„in einer Zelle weicht die Hauptinformation
 * nicht"* verkehrt herum: Die Belegnummer ist die Hauptinformation, die
 * Beschriftung ist Beiwerk.
 *
 * **Der Deckel gehört zum Einhängepunkt, nicht zum Block** *(13.08.2026)*. Der
 * Block liest `--dichte-beschriftung` und erfährt nicht, wo er hängt; die eigene
 * Route setzt den Wert auf ihrem Wrapper auf 16 rem herauf
 * (`.beschriftung-breit` in `globals.css`, `nachricht-seite.tsx`). Dort ist die
 * Gruppenzeile gemessene 1.126 px breit statt 454 — die Begründung für die
 * 10 rem ist die Rechnung *„was bleibt dem Wert übrig"*, und dort bleibt ihm
 * reichlich. Dieselben Beschriftungen brachen um, ohne dass jemand dadurch
 * Platz gewann.
 *
 * Die Zeilen richten sich **oben** aus. Mittig schwömme eine einzeilige
 * Beschriftung neben drei Zeilen Werten in der Mitte.
 *
 * ## Am schmalen Fenster fällt sie auf die gestapelte Form zurück
 *
 * Unter dem vorhandenen Umbruchpunkt des Projekts (768 px,
 * `docs/visuelles-konzept.md` §6) steht die Beschriftung wieder über den
 * Werten; zwei Spalten tragen dort nicht. **Kein neuer Umbruchpunkt.**
 *
 * ## Die Bezeichnung bleibt vollständig
 *
 * Sie kommt unverändert aus dem Altsystem, samt ihrer Endung (`_K_SAP`,
 * `_L_SAP`, `_FORS`). **M45 misst, dass die Endung unterscheidet:** Ohne sie
 * fallen 62 Beschreibungen auf 57, und `Abladestelle_L_SAP` und
 * `Abladestelle_K_SAP` stehen auf 3.405 Nachrichten eines Monats gemeinsam —
 * gekürzt stünden dort zwei Gruppen mit identischer Überschrift untereinander.
 *
 * **Sie bricht deshalb nicht *in* der Endung** *(13.08.2026)*. Gezeigt wird
 * dieselbe Zeichenkette, nur in zwei Teilen: Der Name darf weiterhin umbrechen,
 * die Endung ist eine Einheit (`lib/bam-beschriftung.ts`). Ohne das fiel der
 * Bruch dorthin, wo die Breite ausging — `Kundenmaterialnummer_` / `K_SAP`.
 *
 * **Ein Typ wird nicht als „nicht konfiguriert" markiert.** Ob er in der
 * Konfiguration des Mandanten steht, ist eine interne Angabe; der Nutzer sieht
 * ihn schlicht weiter unten. Das Backend liefert die Angabe gar nicht erst.
 */
function Gruppe({ gruppe }: { gruppe: BamGruppe }) {
  const texte = useTexte();
  const sprache = useSprache();
  const titelId = useId();

  const rest = gruppe.gesamt - gruppe.werte.length;
  const { name, endung } = zerlegeBeschriftung(gruppe.bezeichnung);

  return (
    <section
      aria-labelledby={titelId}
      className="md:grid-cols-beschriftung grid grid-cols-1 items-start gap-x-3 gap-y-1"
    >
      {/* Umbruch statt Kürzung: Die Spalte ist gedeckelt, also bricht eine lange
          Beschriftung in ihr um — und ein `title` mit dem Vollwert wäre ein
          Versprechen auf etwas, das ohnehin dasteht.

          Der Name behält dabei sein `break-words` vom `h3`: Eine Beschriftung,
          deren Namensteil allein breiter ist als die Spalte, muss weiterhin
          umbrechen dürfen. Nur die Endung ist eine Einheit.

          Zwischen den beiden Teilen darf **kein Leerzeichen** entstehen, sonst
          stünde dort „Kundenmaterialnummer _K_SAP". Sie stehen deshalb ohne
          jeden Text dazwischen; ein `{" "}` wäre hier genau der Fehler. */}
      <h3 id={titelId} className="text-muted-foreground text-beiwerk font-medium break-words">
        <span>{name}</span>
        {endung === null ? null : (
          <span data-endung className="whitespace-nowrap">
            {endung}
          </span>
        )}
      </h3>
      <ul className="flex flex-wrap items-center gap-1">
        {gruppe.werte.map((wert) => (
          // Der Schlüssel ist `(typ, wert)` und niemals der Wert allein: Bei
          // 4,17 Prozent der Paare steht derselbe Wert unter mehreren Typen
          // (M37). Innerhalb einer Nachricht ist das Paar eindeutig — der
          // Primärschlüssel ist (MessageID, MessageBAMType, MessageBAMValue).
          // Die Gestalt ist seit Teil 3 geteilt (`components/marke.tsx`) — es
          // soll keine zweite im Projekt geben. Was hier **nicht** dazukommt,
          // ist die Bedienbarkeit: Diese Marke bleibt kein Knopf.
          <li key={`${gruppe.typ}-${wert}`} data-wert className={cn(MARKE_GESTALT, "font-mono")}>
            {wert}
          </li>
        ))}
        {gruppe.weitereVorhanden ? (
          // Die ehrliche Restangabe steht **hinter** den Marken und in
          // derselben Zelle — aber ohne Fläche: Als Marke gesetzt sähe sie aus
          // wie ein weiterer Wert.
          <li data-rest className="text-muted-foreground text-beiwerk">
            {einsetzen(texte.nachrichten.detail.bam.weitere, {
              anzahl: formatiereZahl(rest, sprache),
            })}
          </li>
        ) : null}
      </ul>
    </section>
  );
}
