"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ChevronDown, ChevronRight, TriangleAlert } from "lucide-react";

import { MARKE_GESTALT } from "@/components/marke";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { statusVordergrund } from "@/lib/status-farbe";
import { cn } from "@/lib/utils";

import type { Partnerknoten } from "../api";
import {
  BAUMTASTEN,
  baumzeilen,
  partnertext,
  prozessSchluessel,
  richtungstext,
  tastenbefehl,
  zeilenbeschriftung,
  zustandstext,
  type Baumzeile,
} from "../prozessbaum";

/**
 * Der Prozessbaum — `role="tree"` nach dem WAI‑ARIA-Muster.
 *
 * ## Warum kein Accordion-Baustein
 *
 * Der Auftrag verlangt die Prüfung, und sie fällt aus zwei Gründen gegen ihn
 * aus. Erstens **die Bauform**: Ein Accordion ist eine Folge unabhängiger
 * Abschnitte, ein Baum eine Hierarchie mit `aria-level`; die Tastaturbedienung
 * ist eine andere (Pfeiltasten bewegen den Fokus, statt zwischen Kopfzeilen zu
 * tabben). Zweitens **die Menge**: Bei `NEXANS` sind es 155 Partnerknoten (154
 * kuratierte Partner plus die Gruppe „nicht zugeordnet", M117), 270
 * Richtungsknoten nach E‑45 und 733 Blätter — vollständig aufgeklappt **1.158
 * Zeilen**, am gerenderten Baum ausgezählt. Jeder Abschnitt eines Accordions
 * brächte eine eigene Zustandsverwaltung mit.
 *
 * ## Flach im DOM, geschachtelt in ARIA
 *
 * Gerendert wird die **Liste der sichtbaren Zeilen** (`prozessbaum.ts`
 * {@link baumzeilen}), nicht ein geschachtelter Baum. Die Tiefe steht in
 * `aria-level`, die Geschwisterzahl in `aria-posinset`/`aria-setsize` — genau
 * die Form, die die ARIA-Spezifikation für einen „flattened tree" vorsieht. Die
 * Tastaturbedienung wird dadurch zu „eine Zeile weiter" statt zu einem
 * Baumdurchlauf, und der DOM-Baum bleibt flach.
 *
 * ## Ein Tabstopp, nicht 1.158
 *
 * Roving `tabindex`: Genau eine Zeile trägt `tabIndex={0}`, alle anderen `-1`.
 * Wer aus dem Eingrenzungsfeld heraus tabbt, landet im Baum und nicht in seiner
 * ersten von tausend Zeilen; die Pfeiltasten bewegen den Fokus darin weiter.
 *
 * ## Kein eigener Scrollbereich
 *
 * Der Baum sitzt im **einen** Scrollbereich des Anwendungsrahmens
 * (`docs/frontend-grundlagen.md` §7). Ein zweiter wäre der erste Verstoß gegen
 * genau die Regeln, die dort gemessen worden sind — dieselbe Festlegung wie beim
 * Nachrichtenpanel (`docs/nachrichtendetail.md` §10.7). **`relative` trägt der
 * Rahmen darum**: `sr-only` ist `position: absolute`, und ein solches Element
 * ohne positionierten Vorfahren macht in einer langen Liste die ganze Seite
 * scrollbar.
 */
export function ProzessBaum({
  partner,
  stilleSchwelleMonate,
  gewaehlt,
  springeZurAuswahl,
  istOffen,
  aufUmschalten,
  aufAuswahl,
}: {
  /** Die **eingegrenzten** Partnerknoten — die Ansicht filtert, der Baum zeichnet. */
  partner: readonly Partnerknoten[];
  /** Kommt aus der Antwort (E‑37). Der Baum rechnet keine Monate nach. */
  stilleSchwelleMonate: number;
  gewaehlt: string | null;
  /**
   * Ob die gewählte Zeile ins Bild geholt werden soll.
   *
   * **Falsch, solange das Panel offen steht.** Baum und Panel sitzen im *einen*
   * Scrollbereich; ein Sprung an eine Zeile weit unten im Baum schöbe das Panel
   * daneben nach oben aus dem Bild. Wer einen Link auf eine **Nachricht**
   * öffnet, will zuerst den Beleg sehen — der Baum ist der Kontext, den er
   * danach sucht.
   */
  springeZurAuswahl: boolean;
  istOffen: (schluessel: string) => boolean;
  aufUmschalten: (schluessel: string) => void;
  aufAuswahl: (processId: string) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zahl = useCallback((wert: number) => formatiereZahl(wert, sprache), [sprache]);

  const zeilen = useMemo(() => baumzeilen(partner, istOffen), [partner, istOffen]);

  /**
   * Welche Zeile den Tabstopp trägt.
   *
   * **Kein Effekt hält ihn nach**: Verschwindet die gemerkte Zeile — durch eine
   * Eingrenzung, durch Zuklappen —, greift beim nächsten Rendern der Rückfall.
   * Ein Effekt, der den Zustand nachzöge, wäre ein `setState` im Effekt und
   * damit genau das, was `react-hooks/set-state-in-effect` verbietet.
   */
  const [fokus, setFokus] = useState<string | null>(null);
  const sichtbar = useMemo(() => zeilen.map((zeile) => zeile.schluessel), [zeilen]);
  const aktiv =
    fokus !== null && sichtbar.includes(fokus)
      ? fokus
      : gewaehlt !== null && sichtbar.includes(prozessSchluessel(gewaehlt))
        ? prozessSchluessel(gewaehlt)
        : (sichtbar[0] ?? null);

  const knoten = useRef(new Map<string, HTMLDivElement>());

  /**
   * **Ein tiefer Link zeigt die Stelle, nicht nur den offenen Ast.**
   *
   * Der Aufklappzustand ergibt sich aus dem gewählten Prozess; sichtbar ist er
   * damit noch nicht — bei `NEXANS` ist der Baum mehrere Bildschirme hoch, und
   * der Empfänger eines Links landete am Anfang. `block: "nearest"` scrollt
   * **nur, wenn nötig**: Wer im Baum weiterklickt, sieht seine Zeile ohnehin,
   * und ein Sprung bei jedem Klick wäre eine Bewegung, die niemand angefordert
   * hat (`docs/visuelles-konzept.md` §7).
   *
   * **Und gar nicht, solange das Panel offen steht** ({@link springeZurAuswahl}):
   * Beide Spalten sitzen im einen Scrollbereich, ein Sprung tief in den Baum
   * schöbe das Panel daneben aus dem Bild.
   *
   * **Kein `focus()`.** Ein Fokussprung beim Öffnen nähme dem Nutzer die Stelle,
   * an der er gerade war — dieselbe Überlegung, mit der das Nachrichtendetail
   * `Escape` statt eines Fokussprungs bekommen hat.
   */
  useEffect(() => {
    if (gewaehlt === null || !springeZurAuswahl) {
      return;
    }
    // `?.scrollIntoView?.(…)`: `jsdom` rechnet kein Layout und bringt die
    // Methode nicht mit. Ein Aufruf ohne Absicherung wäre dort ein
    // `TypeError` — und damit ein roter Test für etwas, das im Browser stimmt.
    knoten.current.get(prozessSchluessel(gewaehlt))?.scrollIntoView?.({ block: "nearest" });
  }, [gewaehlt, springeZurAuswahl]);

  const springe = useCallback((schluessel: string | undefined) => {
    if (schluessel === undefined) {
      return;
    }
    setFokus(schluessel);
    knoten.current.get(schluessel)?.focus();
  }, []);

  /**
   * **Die Entscheidung steht in `prozessbaum.ts`, hier steht ihre Ausführung.**
   *
   * Welche Taste an welcher Stelle was bedeutet, ist das WAI‑ARIA-Muster und
   * damit eine Festlegung — sie wird als reine Funktion geprüft
   * ({@link tastenbefehl}), nicht über einen gerenderten Baum.
   *
   * **Unterdrückt wird die Voreinstellung für jede Taste, die der Baum an sich
   * zieht** — auch dort, wo sie nichts bewirkt: `ArrowDown` auf der letzten
   * Zeile darf die Seite nicht scrollen.
   */
  function beiTaste(ereignis: React.KeyboardEvent, index: number) {
    /*
     * **Eine Modifiertaste gehört dem Browser, nicht dem Baum.** `Alt+←` ist
     * das Zurück des Browsers — und genau der Weg, den `prozess` und
     * `nachricht` mit `history: "push"` überhaupt erst anlegen. Ohne diesen
     * Ausstieg verschluckte der Baum die Rücknavigation, weil er nur
     * `ereignis.key` ansieht; dasselbe gilt für `Strg+Pos1` und `Strg+Ende`.
     * Das WAI‑ARIA-Beispiel für `treeview` steigt an derselben Stelle aus.
     */
    if (ereignis.altKey || ereignis.ctrlKey || ereignis.metaKey || ereignis.shiftKey) {
      return;
    }
    if (!(BAUMTASTEN as readonly string[]).includes(ereignis.key)) {
      return;
    }
    ereignis.preventDefault();

    const befehl = tastenbefehl(zeilen, index, ereignis.key);
    if (befehl === null) {
      return;
    }
    if (befehl.art === "FOKUS") {
      springe(befehl.schluessel);
    } else if (befehl.art === "UMSCHALTEN") {
      aufUmschalten(befehl.schluessel);
    } else {
      aufAuswahl(befehl.processId);
    }
  }

  return (
    <div
      role="tree"
      aria-label={texte.prozesse.baum.bezeichnung}
      className="relative flex flex-col"
    >
      {zeilen.map((zeile, index) => (
        <BaumZeile
          key={zeile.schluessel}
          zeile={zeile}
          beschriftung={zeilenbeschriftung(zeile, stilleSchwelleMonate, texte, zahl)}
          stilleSchwelleMonate={stilleSchwelleMonate}
          zahl={zahl}
          gewaehlt={zeile.art === "PROZESS" && zeile.prozess.processId === gewaehlt}
          tabbar={zeile.schluessel === aktiv}
          merke={(element) => {
            if (element === null) {
              knoten.current.delete(zeile.schluessel);
            } else {
              knoten.current.set(zeile.schluessel, element);
            }
          }}
          aufKlick={() =>
            zeile.art === "PROZESS"
              ? aufAuswahl(zeile.prozess.processId)
              : aufUmschalten(zeile.schluessel)
          }
          aufTaste={(ereignis) => beiTaste(ereignis, index)}
          aufFokus={() => setFokus(zeile.schluessel)}
        />
      ))}
    </div>
  );
}

/**
 * Die Einrückung je Ebene, als **vollständige Klassennamen**.
 *
 * Tailwind durchsucht den Quelltext; ein zusammengesetzter Name entstünde nie im
 * erzeugten CSS. Die Werte liegen in `rem` und skalieren damit mit der
 * Dichtestufe — `ps-5` ist eine Zeichenbreite des Aufklapp-Zeichens.
 *
 * **Ebene 2 kommt zweimal vor, und das ist E‑45**: als Richtungsknoten *und* als
 * Blatt eines Partners, dessen Richtungsebene weggefallen ist. Beide stehen
 * gleich weit eingerückt, weil beide gleich tief hängen.
 */
const EINRUECKUNG: Record<number, string> = {
  1: "ps-0",
  2: "ps-5",
  3: "ps-10",
};

function BaumZeile({
  zeile,
  beschriftung,
  stilleSchwelleMonate,
  zahl,
  gewaehlt,
  tabbar,
  merke,
  aufKlick,
  aufTaste,
  aufFokus,
}: {
  zeile: Baumzeile;
  beschriftung: string;
  stilleSchwelleMonate: number;
  zahl: (wert: number) => string;
  gewaehlt: boolean;
  tabbar: boolean;
  merke: (element: HTMLDivElement | null) => void;
  aufKlick: () => void;
  aufTaste: (ereignis: React.KeyboardEvent) => void;
  aufFokus: () => void;
}) {
  const texte = useTexte();
  const zusatz =
    zeile.art === "PROZESS"
      ? zustandstext(zeile.prozess.zustand, stilleSchwelleMonate, texte)
      : null;

  return (
    <div
      ref={merke}
      role="treeitem"
      aria-level={zeile.ebene}
      aria-posinset={zeile.position}
      aria-setsize={zeile.geschwister}
      /*
       * **`aria-selected` steht auf jeder Zeile, `aria-expanded` nur auf
       * Gruppen.** Auswählbar ist allein ein Prozess — eine Gruppe trägt
       * deshalb `false` und nie `true`. Das ist keine Nachgiebigkeit gegenüber
       * der Lint-Regel, sondern die Aussage selbst: Der Baum kennt genau eine
       * Auswahl, und sie liegt nie auf einem Partner.
       */
      aria-selected={gewaehlt}
      {...(zeile.art === "PROZESS" ? {} : { "aria-expanded": zeile.offen })}
      /*
       * **Ein `aria-label` statt vieler `sr-only`-Spannen.** Die Regel, welche
       * Angabe in welcher Reihenfolge vorgelesen wird, steht als reine Funktion
       * in `prozessbaum.ts` und wird dort geprüft.
       */
      aria-label={beschriftung}
      tabIndex={tabbar ? 0 : -1}
      onClick={aufKlick}
      onKeyDown={aufTaste}
      onFocus={aufFokus}
      className={cn(
        /*
         * **`--dichte-bedienzeile`: die Zeilenhöhe am Zeiger, die Berührungsfläche
         * am Finger** (E‑54, `docs/process-view.md` §24).
         *
         * Bis zum 02.09.2026 stand hier `min-h-beruehrung`, und das Token ist aus
         * der Dichteskalierung **heraus** (`docs/visuelles-konzept.md` §5): In
         * `xs`, `s` und `m` griff überall derselbe Boden von 44 px, und der
         * Dichteumschalter bewegte im Baum drei Zeilen über die ganze Skala —
         * 19/19/18/16 gegen 28/26/24/19 in der Nachrichtenliste (M121).
         *
         * Die Umschaltung steht in `globals.css` und nicht hier: `@media (pointer:
         * coarse)` setzt das Token auf `--dichte-beruehrung` zurück. **`pointer`
         * und nicht `any-pointer`** — der Preis ist benannt und dokumentiert.
         */
        "min-h-bedienzeile focus-visible:ring-ring flex cursor-pointer items-start gap-1.5 rounded-sm py-1 pe-1 focus-visible:ring-2 focus-visible:outline-none",
        "hover:bg-muted",
        // Die Auswahl sagt etwas über die **Anwendung** — welche Zeile offen ist
        // —, nicht über die Daten. Dieselbe blasse Akzenttönung wie die geöffnete
        // Zeile der Nachrichtenliste und der aktive Navigationseintrag.
        gewaehlt && "bg-accent",
        /*
         * **Hier stand bis zum 02.09.2026 eine Dämpfung für „nie"** (E‑56). Sie
         * ist zusammen mit dem Wort gefallen und **nur** zusammen mit ihm:
         * Bliebe sie allein stehen, wäre der Zustand ausschließlich über
         * Helligkeit ausgedrückt — genau der Fall, den
         * `docs/visuelles-konzept.md` §3 verbietet.
         */
        EINRUECKUNG[zeile.ebene] ?? EINRUECKUNG[3],
      )}
    >
      <Zeichen zeile={zeile} />

      <span className="min-w-0 flex-1 py-0.5">
        {/*
         * **Umbrechen statt kürzen.** Hier wird *ausgewählt*, nicht überflogen;
         * ein gekürzter Name macht die Auswahl mehrdeutig, sobald sich zwei
         * Prozesse erst hinter dem Schnitt unterscheiden — bei Namen wie
         * `… Lieferschein (VDA)` neben `… Lieferschein (EDIFACT)` der Regelfall
         * (`docs/prozessauswahl.md` §7a). Die feste Zeilenhöhe gilt der Tabelle
         * rechts und nicht dem Baum.
         */}
        <span className={cn("block break-words", zeile.art === "PARTNER" && "font-medium")}>
          {zeile.art === "PARTNER"
            ? partnertext(zeile.partner, texte)
            : zeile.art === "RICHTUNG"
              ? richtungstext(zeile.richtung, texte)
              : (zeile.prozess.processName ?? texte.prozesse.ohneNamen)}
        </span>

        {zusatz === null ? null : (
          /*
           * **Nur `STILL` kommt hier an** ({@link zustandstext}): Es ist ein
           * Vorfall und bekommt die Marken-Gestalt. Seit E‑56 ist es der einzige
           * Zustand mit einem Zusatz — die Fallunterscheidung, die hier stand,
           * hätte keinen zweiten Zweig mehr zu unterscheiden.
           *
           * **Keine eigene Farbrolle:** `--ueberfaellig` gehört der Kategorie
           * *Überfällig* und darf nicht für einen zweiten Sachverhalt stehen.
           */
          <span className={cn("mt-0.5 inline-block", MARKE_GESTALT)}>{zusatz}</span>
        )}
      </span>

      <Zahlen zeile={zeile} zahl={zahl} />
    </div>
  );
}

/**
 * Das Zeichen am Zeilenanfang — **eine Stelle, zwei Bedeutungen**.
 *
 * | Zeile | Zeichen |
 * |---|---|
 * | Gruppe (Partner, Richtung) | das Aufklappzeichen |
 * | Blatt | ein Platzhalter der Zeichenbreite, damit der Name auf einer Höhe mit
 *   der Gruppe darüber beginnt |
 *
 * **Die Richtung steht hier nicht mehr, und zwar in keinem Fall** *(E‑58,
 * 03.09.2026)*. Sie hat zwei Fassungen gehabt: bis zum 02.09.2026 drei Zeichen
 * (`↙`, `↗`, gestrichelter Kreis), danach einen Tag lang das **Wort** vor dem
 * Prozessnamen. Beides stand nur an den Blättern, deren Richtungsebene
 * weggefallen war — und war damit eine zweite Schreibweise für denselben
 * Sachverhalt, den die Ebene daneben als Zeile führt.
 *
 * **Seit E‑58 trägt die Ebene sie überall, wo sie bekannt ist**
 * ({@link richtungsebeneFaelltWeg}); weg fällt die Ebene nur noch dort, wo die
 * Richtung `null` ist — und dort gäbe es nichts zu schreiben.
 */
function Zeichen({ zeile }: { zeile: Baumzeile }) {
  if (zeile.art !== "PROZESS") {
    const Symbol = zeile.offen ? ChevronDown : ChevronRight;
    return <Symbol aria-hidden="true" className="mt-1.5 size-3.5 shrink-0 opacity-70" />;
  }

  return <span aria-hidden="true" className="mt-1.5 size-3.5 shrink-0" />;
}

/**
 * Nachrichten und Fehler — **nicht Überfällig.**
 *
 * Die Kategorie *Überfällig* steht nicht im Baum (Entscheidung E‑43): Je Prozess
 * wäre sie eine Live-Aggregation über `Message`, und die Zahl steht in der
 * Übertragungsliste rechts ohnehin.
 *
 * **Die Nachrichtenzahl steht immer da, auch als `0`** — Null ist eine Aussage
 * und kein fehlender Wert. **Die Fehlerzahl nur, wenn es welche gibt:** Eine
 * rote `0` auf 1.158 Zeilen wäre ein Flächenteppich, und die Farbe verlöre
 * genau das, wofür sie da ist.
 */
function Zahlen({ zeile, zahl }: { zeile: Baumzeile; zahl: (wert: number) => string }) {
  return (
    <span aria-hidden="true" className="text-beiwerk flex shrink-0 items-start gap-2 py-1">
      {zeile.fehler > 0 ? (
        <span className={cn("inline-flex items-center gap-0.5", statusVordergrund("FEHLER"))}>
          {/* Nie allein über Farbe (§3 des Konzepts): das Zeichen gehört dazu. */}
          <TriangleAlert aria-hidden="true" className="size-3" />
          <span className="tabular-nums">{zahl(zeile.fehler)}</span>
        </span>
      ) : null}
      <span className="text-muted-foreground w-12 text-right tabular-nums">
        {zahl(zeile.nachrichten)}
      </span>
    </span>
  );
}
