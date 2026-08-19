"use client";

import { useEffect, useId, useRef, useState } from "react";
import { ChevronRight } from "lucide-react";

import { Fehler, Laden } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Eigenschaft, Schritt } from "../api";
import { gruppiereEigenschaften, schrittHinweis, type EigenschaftenGruppe } from "../detail";
import { useEigenschaften } from "../hooks";

/**
 * Die technischen Eigenschaften — **eingeklappt, erst beim Aufklappen geladen
 * und nach ausgeführtem Schritt gruppiert.**
 *
 * Beschriftet wird der Block mit der Anzahl **aus dem Kopf**, also ohne ihn zu
 * laden. Genau dafür trägt der Detail-Endpunkt `eigenschaftenAnzahl`; lüde die
 * Oberfläche zum Beschriften, hätte der zweite Endpunkt keinen Zweck.
 *
 * **Der Ladezustand liegt im Block, nicht im ganzen Panel.** Wer die
 * Eigenschaften aufklappt, will die Zeitleiste nicht verlieren.
 *
 * **Der Zustand gehört nicht in die URL.** Er ist keine Ansicht, die jemand
 * teilt — und beim Blättern zwischen Nachrichten beginnt der Block wieder
 * eingeklappt (der Aufrufer setzt dafür `key={messageId}`).
 *
 * ## Die Gruppierung (17.08.2026)
 *
 * Flach untereinander stand `Converter.Log.GUID` zweimal und `Service.Type`
 * dreimal und sah aus wie eine Dublette — es sind Einträge **verschiedener
 * Prozessschritte** (M17 3). Gruppiert wird über `position`, beschriftet mit dem
 * Schrittnamen aus der Zeitleiste; **gerechnet wird das in `../detail.ts`**, weil
 * es eine Entscheidung ist und Entscheidungen geprüft werden, Markup nicht.
 *
 * **Eine Ebene, alle Gruppen offen, Überschriften dazwischen.** Keine klappbaren
 * Untergruppen: Gemessen sind 22,6 Eigenschaften je Nachricht, Minimum 14,
 * Maximum 38 (M17 1) — das wäre Mechanik für zwanzig Zeilen.
 *
 * ## Der Weg hierher kommt aus der Zeitleiste (18.08.2026)
 *
 * Ein Klick auf einen Schritt dort klappt den Block auf und setzt den Fokus auf
 * **seine** Gruppe. Der Fokus und nicht ein Bildlauf: Er bewegt die Ansicht
 * ebenso, nimmt aber die Tastatur mit — und er ist die einzige Bewegung, die
 * `visuelles-konzept.md` §7 ohnehin zulässt, weil sie keine ist.
 *
 * **Kein neuer Block, keine Duplizierung.** Die Gruppierung besteht seit dem
 * 17.08.2026; es fehlte nur der Weg dorthin.
 *
 * @param schritte die Schrittfolge aus dem Detail, allein zum Beschriften.
 *   Fehlen sie, tragen alle Gruppen den Rückfall; die Einteilung selbst hängt
 *   nicht an ihnen.
 * @param sprung die Gruppe, die angesprungen werden soll — `null`, solange
 *   niemand gesprungen ist. **`nummer` zählt die Klicks**, damit derselbe
 *   Schritt zweimal hintereinander zweimal wirkt; ohne sie wäre der zweite Klick
 *   auf dieselbe Zeile wirkungslos.
 */
export type Sprungziel = { position: number; nummer: number };

export function EigenschaftenBlock({
  messageId,
  anzahl,
  schritte = [],
  sprung = null,
}: {
  messageId: string;
  anzahl: number;
  schritte?: Schritt[];
  sprung?: Sprungziel | null;
}) {
  const texte = useTexte();
  const bereichId = useId();
  const [offen, setOffen] = useState(false);
  const anfrage = useEigenschaften(messageId, offen);
  const daten = anfrage.data;

  // **Aufgeklappt wird beim Rendern, nicht in einem Effekt.** Ein Sprung ist
  // eine Änderung an einer Eigenschaft, aus der sich der eigene Zustand ergibt —
  // React nennt das „adjusting state when a prop changes", und es ist der
  // ausdrückliche Gegenentwurf zu einem Effekt, der `setState` ruft und dabei
  // eine zweite Renderrunde auslöst.
  const [gesehen, setGesehen] = useState(0);
  if (sprung !== null && sprung.nummer !== gesehen) {
    setGesehen(sprung.nummer);
    setOffen(true);
  }

  // Der Fokus dagegen **ist** ein Effekt: Er ändert das Dokument und nicht den
  // Zustand — und er kann erst laufen, wenn die Gruppen im Baum stehen. Zwischen
  // Klick und Baum liegt die Abfrage, die es erst beim Aufklappen gibt.
  const gesprungen = useRef(0);
  const herkunft = useRef<{ nummer: number; element: Element | null } | null>(null);

  useEffect(() => {
    if (sprung === null || !offen || gesprungen.current === sprung.nummer) {
      return;
    }

    // **Wo stand der Fokus, als geklickt wurde?** Beim ersten Durchlauf zu
    // diesem Sprung festgehalten — später ist es zu spät, dann steht er
    // womöglich schon woanders.
    if (herkunft.current?.nummer !== sprung.nummer) {
      herkunft.current = { nummer: sprung.nummer, element: document.activeElement };
    }
    const stand = herkunft.current;

    // Die Antwort steht noch aus. Bei einem kalten Zwischenspeicher können das
    // in dieser Umgebung Sekunden sein — gesprungen wird erst, wenn es etwas
    // anzuspringen gibt.
    if (daten === undefined) {
      return;
    }

    // **Ein Klick, ein Sprung**, und danach ist er verbraucht — auch wenn er
    // gleich verworfen wird. Ohne diese Marke sprünge die Ansicht ein zweites
    // Mal, sobald jemand den Block von Hand zu- und wieder aufklappt.
    gesprungen.current = sprung.nummer;

    // **Wer weitergegangen ist, wird nicht zurückgerissen.** Hat der Nutzer den
    // Fokus in der Wartezeit selbst bewegt — weitergetabbt, den Block von Hand
    // zugeklappt —, ist der Sprung überholt. Ein Fokuswechsel Sekunden nach der
    // Betätigung reißt ihn aus dem heraus, was er inzwischen tut, und
    // unterbricht mitten in der Ansage eines Vorleseprogramms.
    if (stand.element !== document.activeElement) {
      return;
    }

    // Gibt es zu dem Schritt keine Gruppe, bekommt der Bereich selbst den Fokus.
    // Der Fall ist gemessen: `MessageActionID = 502` steht in `MessageAction`,
    // kommt in `MessageProperty` aber nicht vor (M17 3) — dann gibt es dort
    // nichts anzuspringen, und ins Leere zu springen wäre schlechter als an den
    // Anfang des Blocks.
    const ziel =
      document.getElementById(gruppenId(bereichId, sprung.position)) ??
      document.getElementById(bereichId);
    ziel?.focus();
  }, [sprung, offen, daten, bereichId]);

  /**
   * Auf- und Zuklappen von Hand.
   *
   * **Zuklappen erledigt einen ausstehenden Sprung.** Wer während des Ladens
   * zuklappt, hat den Sprung aufgegeben; ohne diese Zeile käme er beim nächsten
   * Aufklappen nach — Minuten später und ohne Anlass. Der Fokusvergleich im
   * Effekt fängt denselben Fall im Browser mit ab; hier steht er als Regel und
   * nicht als Nebenwirkung.
   */
  const umschalten = () => {
    if (offen && sprung !== null) {
      gesprungen.current = sprung.nummer;
    }
    setOffen(!offen);
  };

  const beschriftung = einsetzen(texte.nachrichten.detail.eigenschaften.titel, { anzahl });

  // Ohne Eigenschaften gibt es nichts aufzuklappen. Ein Schalter, der einen
  // leeren Bereich öffnet, ist schlimmer als keiner.
  if (anzahl === 0) {
    return (
      <p className="text-muted-foreground text-beiwerk">
        {texte.nachrichten.detail.eigenschaften.keine}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={umschalten}
        aria-expanded={offen}
        aria-controls={bereichId}
        title={
          offen
            ? texte.nachrichten.detail.eigenschaften.zuklappen
            : texte.nachrichten.detail.eigenschaften.aufklappen
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
        // `tabIndex={-1}`: Der Bereich ist das Rückfallziel eines Sprungs aus
        // der Zeitleiste. Er ist damit programmatisch fokussierbar, kommt aber
        // in keiner Tabreihenfolge vor.
        //
        // **Und er zeigt, dass er den Fokus hat.** Ein Sprungziel ohne sichtbare
        // Kontur lässt den Tastaturnutzer nicht wissen, wo er gelandet ist —
        // und das nächste `Tab` setzt dann von einer unsichtbaren Stelle aus
        // fort. Dieselbe Rolle und dieselbe Stärke wie überall sonst.
        <div
          id={bereichId}
          tabIndex={-1}
          className="focus-visible:ring-ring rounded-md focus-visible:ring-2 focus-visible:outline-none"
        >
          {anfrage.isPending ? (
            <Laden zeilen={3} />
          ) : anfrage.error ? (
            <Fehler fehler={anfrage.error} aufWiederholen={() => void anfrage.refetch()} />
          ) : (anfrage.data?.length ?? 0) === 0 ? (
            <p className="text-muted-foreground text-beiwerk">
              {texte.nachrichten.detail.eigenschaften.leer}
            </p>
          ) : (
            <div className="flex flex-col gap-3">
              {gruppiereEigenschaften(anfrage.data ?? [], schritte).map((gruppe) => (
                // Die Position ist der Schlüssel: Sie kommt je Nachricht genau
                // einmal vor, und die Einteilung gruppiert genau darüber.
                <Gruppe
                  key={gruppe.position}
                  gruppe={gruppe}
                  kopfId={gruppenId(bereichId, gruppe.position)}
                />
              ))}
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
}

/**
 * Die Kennung einer Gruppe im DOM — **an einer Stelle gebildet**, weil sie an
 * zwei gebraucht wird: Der Effekt sucht sie, der Abschnitt trägt sie. Der
 * Bereichsschlüssel aus `useId` steht davor, damit zwei Blöcke im selben
 * Dokument — Panel und eigene Route existieren nicht gleichzeitig, aber die
 * Regel hängt nicht daran — sich nicht dieselbe Kennung teilen.
 */
function gruppenId(bereichId: string, position: number): string {
  return `${bereichId}-gruppe-${position}`;
}

/**
 * Eine Gruppe: die Überschrift des Schritts und darunter seine Einträge.
 *
 * ## Der Kopf sagt, in welchem Schritt der Wert entstanden ist
 *
 * `{Beschriftung} ({Anzahl})` — bei der Nachricht selbst die übersetzte
 * Beschriftung, bei einer Position ohne gelieferten Schritt der Rückfall
 * *Schritt N*. **Kein erfundener Name.**
 *
 * **Der Tooltip ist derselbe wie in der Zeitleiste** und wird nicht nachgebaut:
 * Er kommt aus `../detail.ts`, damit ein Gruppenkopf wortgleich dasselbe sagt wie
 * die Zeile darüber. Bei der Nachricht selbst gibt es keinen Schritt und deshalb
 * auch keinen Tooltip.
 *
 * ## Sie ist eine Beschriftung und keine Statusaussage
 *
 * **Keine eigene Farbe, keine Animation, kein Übergang**
 * (`visuelles-konzept.md` §7): der vorhandene gedämpfte Ton, wie bei der
 * Beschriftung im BAM-Block. Und **kein eigener Scrollbereich** — es bleibt beim
 * einen senkrechten Scroller (`frontend-grundlagen.md` §7).
 *
 * **Umbruch statt Kürzung.** Die gemessene Namenslänge geht bis 61 Zeichen; die
 * Anzahl steht am Ende derselben Zeichenkette und darf nicht als Erstes
 * wegfallen. Ein Schrittname, der im Panel nicht in eine Zeile passt, bricht
 * deshalb um — dieselbe Entscheidung wie bei den Beschriftungen des BAM-Blocks.
 */
function Gruppe({ gruppe, kopfId }: { gruppe: EigenschaftenGruppe; kopfId: string }) {
  const texte = useTexte();
  const sprache = useSprache();
  const titelId = useId();

  const bausteine = texte.nachrichten.detail.eigenschaften;
  const name = gruppe.istNachricht
    ? bausteine.gruppeNachricht
    : (gruppe.beschriftung ?? einsetzen(bausteine.gruppeSchritt, { nummer: gruppe.position }));

  return (
    // `id` und `tabIndex={-1}`: das Ziel eines Sprungs aus der Zeitleiste. Der
    // Fokus landet auf dem **Abschnitt** und nicht auf der Überschrift — über
    // `aria-labelledby` liest ein Vorleseprogramm damit den Gruppennamen und
    // weiß zugleich, dass darunter noch etwas kommt.
    //
    // **Der Fokus ist sichtbar**, mit derselben Rolle und derselben Stärke wie
    // an jedem Bedienelement dieses Projekts. Wer per Tastatur aus der
    // Zeitleiste hierher springt, muss sehen, wo er gelandet ist — sonst führt
    // das nächste `Tab` aus einer unsichtbaren Stelle weiter. Der Rahmen liegt
    // etwas außerhalb, damit er die Gruppe umfasst und nicht ihre erste Zeile
    // überdeckt.
    <section
      id={kopfId}
      tabIndex={-1}
      aria-labelledby={titelId}
      className="focus-visible:ring-ring flex flex-col gap-1 rounded-md focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none"
    >
      <h3
        id={titelId}
        className="text-muted-foreground text-beiwerk font-medium break-words"
        // `beschriftung` ist der Name des Schritts — dieselbe Zeichenkette, die
        // die Zeitleiste als Zeile führt, und deshalb derselbe Tooltip.
        title={schrittHinweis(
          {
            name: gruppe.beschriftung,
            rohwert: gruppe.rohwert,
            namensherkunft: gruppe.namensherkunft,
          },
          texte,
        )}
      >
        {einsetzen(bausteine.gruppe, {
          name,
          anzahl: formatiereZahl(gruppe.eintraege.length, sprache),
        })}
      </h3>
      <ul className="flex flex-col">
        {gruppe.eintraege.map((eigenschaft) => (
          <EigenschaftZeile
            // Der Primärschlüssel ist `(MessageID, MessagePropertyName,
            // MessageActionID)` — innerhalb einer Gruppe ist der Name damit
            // eindeutig. Über Gruppen hinweg ist er es **nicht** (M17 3), und
            // deshalb steht die Position davor.
            key={`${gruppe.position}:${eigenschaft.name}`}
            eigenschaft={eigenschaft}
          />
        ))}
      </ul>
    </section>
  );
}

/**
 * Name und Wert als **Rohwerte**, in fester Zeilenhöhe.
 *
 * **Ein gekappter Wert wird als gekappt gekennzeichnet**, mit seiner
 * ursprünglichen Länge — das Backend liefert beides. Ein stillschweigend
 * abgeschnittener Wert ist schlimmer als ein sichtbar abgeschnittener: Ohne das
 * Kennzeichen läse jemand eine halbe Belegnummer als ganze.
 */
function EigenschaftZeile({ eigenschaft }: { eigenschaft: Eigenschaft }) {
  const texte = useTexte();
  const sprache = useSprache();

  const gekapptHinweis =
    eigenschaft.gekappt && eigenschaft.originalLaengeBytes !== null
      ? einsetzen(texte.nachrichten.detail.eigenschaften.gekapptHinweis, {
          bytes: formatiereZahl(eigenschaft.originalLaengeBytes, sprache),
        })
      : undefined;

  return (
    <li className="h-zeile text-beiwerk flex items-center gap-2">
      <span
        className="text-muted-foreground w-2/5 shrink-0 truncate font-mono"
        title={eigenschaft.name}
      >
        {eigenschaft.name}
      </span>
      <span className="min-w-0 flex-1 truncate font-mono" title={eigenschaft.wert}>
        {eigenschaft.wert}
      </span>
      {eigenschaft.gekappt ? (
        <span
          className="border-border text-muted-foreground shrink-0 rounded-sm border px-1"
          title={gekapptHinweis}
        >
          {texte.nachrichten.detail.eigenschaften.gekappt}
          {gekapptHinweis === undefined ? null : (
            <span className="sr-only"> — {gekapptHinweis}</span>
          )}
        </span>
      ) : null}
    </li>
  );
}
