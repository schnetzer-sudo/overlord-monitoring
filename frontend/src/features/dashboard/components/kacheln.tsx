"use client";

import Link from "next/link";
import { useState } from "react";
import { AlertTriangle, ChevronDown, ChevronUp, CircleOff, Clock, Mails } from "lucide-react";

import { Card } from "@/components/ui/card";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { problemKlassenOhneKontur, statusKlassenOhneKontur } from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

import type { Fehlerart, Fenster, Kacheln as Kachelwerte } from "../api";
import { fehlerartText } from "../beschriftung";
import { fehlerZiel, ueberfaelligZiel } from "../verweise";

/**
 * Die drei Kacheln — **Fehler, Überfällig, Nachrichten, in dieser Reihenfolge**.
 *
 * Die Reihenfolge folgt dem Leitsatz: *Das Werkzeug wird geöffnet, wenn etwas
 * nicht stimmt.* Die Zählkachel steht hinten, weil sie keine Frage beantwortet,
 * mit der jemand herkommt.
 *
 * ## Die Gestalt kommt aus der Sichtprobe A.2 (Entscheidung E‑u)
 *
 * Beide Problemkategorien tragen **Fläche und Vordergrund, keine Kontur** —
 * `statusKlassenOhneKontur` und `problemKlassenOhneKontur`. Die Begründung steht
 * in [`docs/dashboard-frontend.md`](../../../../docs/dashboard-frontend.md) §3
 * und kurz: `--ueberfaellig-kontur` liegt bei L 0.65 und erfüllt als einzige der
 * fünf die 3 : 1 aus WCAG 1.4.11; neben der Fehlerkontur bei L 0.86 liest sich
 * das als Rangfolge, und Regel Q3 führt beide gleichrangig.
 *
 * **Nie allein über Farbe.** Jede Kachel trägt zusätzlich das Wort und ein
 * Zeichen, und die Zeichen unterscheiden sich in der Form: Warndreieck gegen Uhr.
 *
 * ## Was klickt und was nicht (Entscheidung E‑m)
 *
 * | Kachel | Ziel |
 * |---|---|
 * | **Fehler** | die Liste, auf `status=FEHLER` und das Fenster der Antwort |
 * | **Überfällig, im Fenster** | die Liste, auf `ueberfaellig=true` und dasselbe Fenster |
 * | **Überfällig, insgesamt** | **nichts** — die Zahl hat kein Zeitfenster, die Liste braucht eines |
 * | **Nachrichten** | nichts |
 *
 * **Die beiden Filter erscheinen nie zusammen.** Sie sind am Listen-Endpunkt
 * unvereinbar und ergäben `400`; die Adressen entstehen deshalb in zwei
 * getrennten Funktionen (`verweise.ts`).
 *
 * **Verlinkt ist ein Bereich der Kachel und nicht die ganze Kachel.** In der
 * Fehlerkachel steht darunter die Schaltfläche für die Aufschlüsselung, und ein
 * `<button>` in einem `<a>` ist kein gültiges Markup — es ist auch keine Frage
 * der Form: Der Verweis führt in die Liste, der Schalter tut hier etwas.
 */
export function Kacheln({ kacheln, fenster }: { kacheln: Kachelwerte; fenster: Fenster }) {
  const texte = useTexte();

  return (
    <div className="flex flex-col gap-2">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <FehlerKachel fehler={kacheln.fehler} fenster={fenster} />
        <UeberfaelligKachel ueberfaellig={kacheln.ueberfaellig} fenster={fenster} />
        <NachrichtenKachel anzahl={kacheln.nachrichten} />
      </div>
      {/*
       * Bekannte Grenze 3 aus `docs/dashboard.md` §2, und sie steht sichtbar da
       * und nicht in einem `title`: Auf einem Berührungsgerät gibt es kein
       * Überfahren, und dort erführe es niemand.
       */}
      <p className="text-muted-foreground text-beiwerk">
        {texte.dashboard.kacheln.nachrichtenHinweis}
      </p>
    </div>
  );
}

/** Der gemeinsame Rahmen. Die Farbe kommt als Klassenkette von außen. */
function Kachel({ klassen, children }: { klassen?: string; children: React.ReactNode }) {
  return (
    <Card size="sm" className={cn("gap-2 px-4", klassen)}>
      {children}
    </Card>
  );
}

function Kopf({ zeichen: Zeichen, titel }: { zeichen: typeof AlertTriangle; titel: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <Zeichen aria-hidden="true" className="size-4 shrink-0" />
      <span className="text-basis font-medium">{titel}</span>
    </div>
  );
}

function Zahl({ wert }: { wert: number }) {
  const sprache = useSprache();
  return (
    <span className="text-3xl leading-none font-semibold tabular-nums">
      {formatiereZahl(wert, sprache)}
    </span>
  );
}

/**
 * Der anklickbare Teil einer Kachel.
 *
 * **Ein echter Verweis und keine Schaltfläche.** „Schick mir mal den Link" ist
 * bei diesem Werkzeug die eigentliche Anwendung; ein Ziel, das man weder mit der
 * mittleren Maustaste öffnen noch kopieren kann, verfehlt das.
 */
function Verweis({
  ziel,
  bezeichnung,
  children,
}: {
  ziel: string;
  bezeichnung: string;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={ziel}
      aria-label={bezeichnung}
      title={bezeichnung}
      className="focus-visible:ring-ring -mx-1 rounded-md px-1 hover:underline focus-visible:ring-2 focus-visible:outline-none"
    >
      {children}
    </Link>
  );
}

function FehlerKachel({ fehler, fenster }: { fehler: Kachelwerte["fehler"]; fenster: Fenster }) {
  const texte = useTexte();
  const [offen, setOffen] = useState(false);
  const hatArten = fehler.arten.length > 0;

  return (
    <Kachel klassen={statusKlassenOhneKontur("FEHLER")}>
      <Kopf zeichen={AlertTriangle} titel={texte.dashboard.kacheln.fehler} />
      <Verweis ziel={fehlerZiel(fenster)} bezeichnung={texte.dashboard.kacheln.fehlerVerweis}>
        <Zahl wert={fehler.anzahl} />
      </Verweis>

      {hatArten ? (
        <div className="flex flex-col gap-1">
          <button
            type="button"
            onClick={() => setOffen((bisher) => !bisher)}
            aria-expanded={offen}
            className="focus-visible:ring-ring text-beiwerk -mx-1 flex w-fit items-center gap-1 rounded-md px-1 opacity-90 hover:underline focus-visible:ring-2 focus-visible:outline-none"
          >
            {offen
              ? texte.dashboard.kacheln.artenZuklappen
              : texte.dashboard.kacheln.artenAufklappen}
            {offen ? (
              <ChevronUp aria-hidden="true" className="size-3.5" />
            ) : (
              <ChevronDown aria-hidden="true" className="size-3.5" />
            )}
          </button>
          {offen ? <Fehlerarten arten={fehler.arten} /> : null}
        </div>
      ) : null}
    </Kachel>
  );
}

/**
 * Die Aufschlüsselung nach Art — **inline in der Kachel, aufklappbar**.
 *
 * **Beschriftet wird über den Rohwert und nicht über die gelieferte `art`.** Der
 * Endpunkt setzt für `COMMIT_REJECTED` einen deutschen Festtext
 * (`MessageStatusClassifier.ABGELEHNT_VOM_PARTNER`); stünde der hier
 * unverändert, läse ihn auch ein englischer Nutzer. Für jeden anderen Rohwert
 * ist `art` kein Anzeigetext, sondern ein **Wert** — der Namensteil hinter
 * `ERROR_` oder der Rohwert selbst — und der wird nicht übersetzt (Regel Q4).
 *
 * **Der Rohwert steht daneben**, als `title`: Ohne ihn wäre *„Vom Partner
 * abgelehnt"* eine Zeichenkette, an der sich nichts mehr festmachen ließe.
 */
function Fehlerarten({ arten }: { arten: readonly Fehlerart[] }) {
  const texte = useTexte();
  const sprache = useSprache();

  return (
    <dl className="text-beiwerk grid grid-cols-[1fr_auto] gap-x-3 tabular-nums">
      {arten.map((art) => (
        <ArtZeile
          key={art.rohwert}
          rohwert={art.rohwert}
          name={fehlerartText(art, texte)}
          anzahl={formatiereZahl(art.anzahl, sprache)}
        />
      ))}
    </dl>
  );
}

function ArtZeile({ rohwert, name, anzahl }: { rohwert: string; name: string; anzahl: string }) {
  return (
    <>
      <dt className="truncate opacity-90" title={rohwert}>
        {name}
      </dt>
      <dd className="text-right font-medium">{anzahl}</dd>
    </>
  );
}

function UeberfaelligKachel({
  ueberfaellig,
  fenster,
}: {
  ueberfaellig: Kachelwerte["ueberfaellig"];
  fenster: Fenster;
}) {
  const texte = useTexte();
  const sprache = useSprache();

  return (
    <Kachel klassen={problemKlassenOhneKontur("UEBERFAELLIG")}>
      <Kopf zeichen={Clock} titel={texte.dashboard.kacheln.ueberfaellig} />

      {ueberfaellig.ermittelbar && ueberfaellig.imFenster !== null ? (
        <>
          <div className="flex flex-wrap items-baseline gap-x-2">
            <Verweis
              ziel={ueberfaelligZiel(fenster)}
              bezeichnung={texte.dashboard.kacheln.ueberfaelligVerweis}
            >
              <Zahl wert={ueberfaellig.imFenster} />
            </Verweis>
            <span className="text-beiwerk opacity-90">
              {texte.dashboard.kacheln.ueberfaelligImFenster}
            </span>
          </div>
          {/*
           * **„Insgesamt" trägt keinen Verweis** (E‑m). Die Zahl hat bewusst kein
           * Zeitfenster, die Liste hat ein Pflicht-Zeitfenster — jedes Ziel
           * zeigte eine andere Zahl als die Kachel. Der Satz daneben sagt das;
           * eine Kachel, die auf eine andere Zahl führt als sie nennt, ist
           * schlechter als eine, die nicht klickt.
           */}
          <p className="text-beiwerk tabular-nums opacity-90">
            {formatiereZahl(ueberfaellig.insgesamt ?? 0, sprache)}{" "}
            {texte.dashboard.kacheln.ueberfaelligInsgesamt}
          </p>
          <p className="text-beiwerk opacity-75">{texte.dashboard.kacheln.insgesamtOhneVerweis}</p>
        </>
      ) : (
        <NichtErmittelbar />
      )}
    </Kachel>
  );
}

/**
 * „Nicht ermittelbar" (Entscheidung E‑q) — **keine `0`, kein Rot, kein
 * Fehlerzustand.**
 *
 * Null hieße „es hängt nichts", und das ist in einem Überwachungswerkzeug die
 * schlimmste falsche Antwort. Der Kacheltitel bleibt stehen, an der Stelle der
 * Zahl steht gedämpfter Text mit einem Zeichen, die Kachel ist **nicht
 * klickbar**, und ein Satz nennt den Grund. Für den Nutzer ist das eine
 * Auskunft und kein technischer Fehler — deshalb keine Fehler-Kennung und keine
 * Schaltfläche „Erneut versuchen": Die übrigen Blöcke stehen ja.
 *
 * **Beide Zahlen fallen zusammen**; das gibt der Vertrag vor
 * (`docs/dashboard.md` §5). Hier wird deshalb nicht die eine gezeigt und die
 * andere weggelassen, sondern der ganze Zahlenteil ersetzt.
 */
function NichtErmittelbar() {
  const texte = useTexte();
  return (
    <>
      <p className="flex items-center gap-1.5 opacity-75">
        <CircleOff aria-hidden="true" className="size-5 shrink-0" />
        <span className="text-3xl leading-none font-semibold">
          {texte.dashboard.kacheln.nichtErmittelbar}
        </span>
      </p>
      <p className="text-beiwerk opacity-90">{texte.dashboard.kacheln.nichtErmittelbarHinweis}</p>
    </>
  );
}

/**
 * Die Zählkachel — **neutral und ohne Verweis.**
 *
 * Sie trägt keine Farbrolle: Eine Zahl über den Verkehr ist kein Zustand, und
 * eine Fläche daneben machte aus einer Auskunft eine Meldung
 * (`docs/visuelles-konzept.md` §3).
 */
function NachrichtenKachel({ anzahl }: { anzahl: number }) {
  const texte = useTexte();
  return (
    <Kachel>
      <Kopf zeichen={Mails} titel={texte.dashboard.kacheln.nachrichten} />
      <Zahl wert={anzahl} />
    </Kachel>
  );
}
