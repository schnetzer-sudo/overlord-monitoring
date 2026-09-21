"use client";

import { useState, type ReactNode } from "react";

import { useTexte } from "@/i18n/provider";
import { cn } from "@/lib/utils";

import { AUFKLAPP_UEBERGANG, AufklappInhalt, AufklappSchalter, Aufklappen } from "./aufklappen";

/**
 * **Eine Zeile der Zeitleiste, die sich aufklappen lässt** — Schritt, *Eingang*
 * und *Ohne Schritt in der Zeitleiste* stehen alle drei darauf *(21.09.2026,
 * `docs/nachrichtendetail.md` §10.16)*.
 *
 * ## Eingeklappt sieht sie aus wie vorher
 *
 * Dieselbe Zeilenhöhe, dieselben Spalten, dieselbe Linie links. **Kein Pfeil,
 * keine Zahl, keine Marke** — die Regel des Panels lautet: Blöcke klappen mit
 * Pfeil, Zeitleistenzeilen mit der Linie. Neu ist allein die Anfassbarkeit:
 * Zeigehand, Hover-Fläche und Fokusring wie an der Kettenzeile, und die
 * Hover-Fläche lässt die Linie frei.
 *
 * ## Aufklappbar ist nur, was Inhalt hat (E‑221)
 *
 * Ohne Inhalt ist die Zeile ein `div` mit **denselben** Klassen wie die
 * Schaltfläche, ohne die der Bedienung. Beim Wechsel — die Eigenschaften treffen
 * nach dem Detail ein — ändert sie deshalb weder Höhe noch Breite.
 *
 * ## Die Ziele liegen über der Schaltfläche, nicht in ihr
 *
 * Ein Verweis in einer Schaltfläche wäre verschachtelte Bedienung. Die
 * Schaltfläche spannt die ganze Zeile; die Ziele liegen als **Geschwister** in
 * einer zweiten, deckungsgleichen Ebene darüber, und in der Schaltfläche hält
 * ein Platzhalter ihre Stelle frei — je Zeile so breit wie die Ziele. Die obere
 * Ebene lässt Zeigerereignisse durch und nimmt sie nur an den Zielen an: **Ein
 * Klick auf die Zeile schaltet, ein Klick auf ein Ziel nicht.**
 *
 * Beide Ebenen tragen dieselbe Anordnung (`EBENE`), damit die Ziele stehen, wo
 * sie vorher standen — auch bei der laufenden Zeile, deren rechter Teil keine
 * feste Breite hat.
 *
 * ## Aufgeklappt trägt der Abschnitt die Akzentlinie
 *
 * Die Linie ist der linke Rand **des ganzen Eintrags**, Zeile samt Inhalt: Sie
 * läuft mit demselben Mittel hindurch, in derselben Breite und Art, ohne Lücke,
 * und wächst mit der Höhe des Inhalts — eine eigene Animation hat sie nicht.
 * Offen steht sie in `--akzent-schrift`, der Name ebenso. Das ist
 * Anwendungszustand wie die Akzenttönung der geöffneten Listenzeile und **keine
 * Statusaussage**; den Zustand tragen zusätzlich der sichtbare Inhalt und
 * `aria-expanded` (`docs/visuelles-konzept.md` §3). Strichbreite und
 * Schriftstärke bleiben, sonst verschöbe sich die Zeile.
 *
 * ## Der Zustand
 *
 * Je Zeile unabhängig, mehrere gleichzeitig offen, anfangs zu; nicht in URL,
 * Cookie oder Storage. Zurückgesetzt wird beim Nachrichtenwechsel über den Baum
 * (`key` am Aufrufer), nicht über einen Effekt (E‑226).
 */

/** Die Anordnung einer Zeile — **für beide Ebenen dieselbe**, sonst wandern die Ziele. */
const EBENE = "flex items-center gap-2 px-1";

export function AufklappZeile({
  als,
  gestrichelt = false,
  kontur,
  zeilenklasse,
  namensklasse,
  name,
  hinweis,
  zieleAnzahl,
  zieleKnoten,
  rechts,
  rechtsSpiegel,
  children,
}: {
  als: "li" | "div";
  /** Gestrichelt ist, was kein ausgeführter Schritt ist — Eingang und Rest. */
  gestrichelt?: boolean;
  /** Eine Statuskontur geht der Akzentlinie vor: Sie sagt etwas über die Daten. */
  kontur?: string;
  zeilenklasse: string;
  namensklasse?: string;
  name: string;
  /** Der `title` — bei einem Schritt die Herkunft seines Namens (`schrittHinweis`). */
  hinweis?: string;
  /** Wie viele Ziele die Zeile trägt — daran bemisst sich der Platzhalter. */
  zieleAnzahl: number;
  /**
   * Die Ziele selbst (`artefakt-ziele.tsx` `Ziele`, mit `pointer-events-auto`).
   * Als Knoten gereicht und nicht hier gebaut: `Zielzeile` dort steht auf dieser
   * Datei, und ein Import zurück wäre ein Kreis.
   */
  zieleKnoten: ReactNode;
  rechts?: ReactNode;
  /** Der rechte Teil noch einmal, unsichtbar — er hält die Ziele an ihrer Stelle. */
  rechtsSpiegel?: ReactNode;
  /** Der Inhalt. `null` oder `undefined`: Die Zeile ist nicht aufklappbar. */
  children?: ReactNode;
}) {
  const texte = useTexte();
  const [offen, setOffen] = useState(false);
  const aufklappbar = children !== null && children !== undefined && children !== false;
  const Rahmen = als;

  const kopf = (
    <>
      <span
        className={cn(
          "min-w-0 flex-1 truncate transition-[color]",
          AUFKLAPP_UEBERGANG,
          namensklasse,
          offen && "text-akzent-schrift",
        )}
      >
        {name}
        {aufklappbar ? (
          // Der zugängliche Name beginnt mit dem sichtbaren (WCAG 2.5.3); was
          // sich öffnet, steht dahinter. Der Zustand liegt allein in
          // `aria-expanded`.
          <span className="sr-only">, {texte.nachrichten.detail.eigenschaften.anZeile}</span>
        ) : null}
      </span>
      {zieleAnzahl === 0 ? null : (
        <span
          aria-hidden="true"
          className="shrink-0"
          // So breit wie die Ziele darüber: je Ziel 2 rem, dazwischen 0,125 rem.
          style={{ width: `${zieleAnzahl * 2 + (zieleAnzahl - 1) * 0.125}rem` }}
        />
      )}
      {rechts}
    </>
  );

  const zeile = (
    // `-mr-1`: Die Zeile reicht rechts 0,25 rem über die Leiste hinaus, in den
    // Innenabstand des Panels. So behält die Dauer ihre Stelle, und die
    // Hover-Fläche endet nicht bündig an ihrer letzten Ziffer.
    <div className="relative -mr-1 pl-1">
      {aufklappbar ? (
        <AufklappSchalter
          title={hinweis}
          className={cn(
            EBENE,
            zeilenklasse,
            "hover:bg-muted focus-visible:ring-ring w-full cursor-pointer rounded-sm text-left",
            "focus-visible:ring-2 focus-visible:outline-none",
          )}
        >
          {kopf}
        </AufklappSchalter>
      ) : (
        <div title={hinweis} className={cn(EBENE, zeilenklasse)}>
          {kopf}
        </div>
      )}

      {zieleAnzahl === 0 ? null : (
        <div className={cn(EBENE, "pointer-events-none absolute inset-y-0 right-0 left-1")}>
          <span className="min-w-0 flex-1" />
          {zieleKnoten}
          {rechtsSpiegel}
        </div>
      )}
    </div>
  );

  const linie = cn(
    "flex flex-col border-l-2 transition-[border-color]",
    AUFKLAPP_UEBERGANG,
    gestrichelt && "border-dashed",
    kontur ?? (offen ? "border-akzent-schrift" : "border-border"),
  );

  if (!aufklappbar) {
    return <Rahmen className={linie}>{zeile}</Rahmen>;
  }

  return (
    <Aufklappen asChild offen={offen} aufWechsel={setOffen}>
      <Rahmen className={linie}>
        {zeile}
        {/* Auf der Flucht des Namens, mit etwas Luft innerhalb des
            Linienabschnitts. Eigene Anordnung über die volle Breite: Der Inhalt
            beeinflusst die Spalten der Leiste nicht. */}
        <AufklappInhalt className="min-w-0 pt-0.5 pb-2 pl-2">{children}</AufklappInhalt>
      </Rahmen>
    </Aufklappen>
  );
}
