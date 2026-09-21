"use client";

import { useId, useState, type ReactNode } from "react";
import { ChevronRight } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Fehler } from "@/components/zustand";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl, formatiereZeitpunkt, formatiereZeitpunktGenau } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Kettenglied, Nachrichtendetail } from "../api";
import {
  abschnittAufklappbar,
  abwaertsAbschnitt,
  gezeigteAbwaertsglieder,
  hatInhalt,
  kettenabschnitte,
  nachladenMoeglich,
  type Abschnittsart,
  type Kettenabschnitt,
} from "../kette";
import { useKette, useKettenAbwaerts } from "../hooks";
import { AUFKLAPP_UEBERGANG, AufklappInhalt, AufklappSchalter, Aufklappen } from "./aufklappen";
import { StatusPlakette } from "./status-plakette";

/**
 * **Was hängt an dieser Nachricht** — die Kette, seit dem 21.09.2026 unter der
 * ganzen Zeitleiste (E‑218; bis dahin zwischen Kopf und Zeitleiste).
 *
 * ## Ohne Kette gibt es keinen Block
 *
 * Ist `detail.rollen` leer, erscheint nichts: keine Überschrift, kein leerer
 * Kasten, kein Platzhalter — und **keine Anfrage auf `/kette`**. Rund 60 Prozent
 * aller Zeilen tragen keine Kette; für sie entsteht damit keine zweite Anfrage.
 * Das kostet auch nichts an Auskunft: Die vier Verkettungsspalten stehen auf der
 * `Message`-Zeile, die der Detail-Endpunkt ohnehin liest (E4).
 *
 * Dieselbe Regel wie beim Eigenschaftenblock bei `eigenschaftenAnzahl === 0` und
 * in der Liste bei einem Mandanten ohne BAM-Konfiguration: **Eine Fläche ohne
 * Inhalt behauptet, es gäbe dort etwas zu sehen.**
 *
 * ## Und deshalb darf er dauerhaft sichtbar sein
 *
 * > **Eingeschränkt am 21.09.2026 (E‑227, `docs/verkettung.md` §8.15).** Der
 * > **Block** bleibt dauerhaft sichtbar — Überschriften, Zahl und Abbruchsätze
 * > stehen immer da. **Ein Abschnitt mit mehr als einem Glied beginnt aber zu**,
 * > mit Pfeil in der Bauform der Belegdaten; ein Abschnitt mit genau einem Glied
 * > steht ohne Schalter offen. *„Darf offen stehen"* gilt nur noch bei einem
 * > Glied.
 *
 * Der Kettenblock ist **nicht eingeklappt**. Sein Hauptnachteil wäre gewesen,
 * dass er bei der Mehrheit der Nachrichten Platz ohne Inhalt kostet — und genau
 * den trägt er mit der Bedingung oben nicht mehr. Die Kette ist nach dem
 * Leitsatz die Antwort auf „wo ist mein Lieferschein" und damit kein Beiwerk,
 * das hinter einen Klick gehört.
 *
 * ## Eingeteilt wird nach der Flussrichtung
 *
 * Welches Glied in welchen Abschnitt gehört, entscheidet `../kette.ts` — dort,
 * wo es gerechnet und geprüft wird. Diese Datei stellt nur dar. **Der Block
 * trägt keine eigene Farbe:** Die Statusplakette je Glied nutzt die bestehenden
 * Statusfarben und sonst nichts. Die Kettenrolle ist eine Aussage über die
 * Struktur, nicht über den Zustand der Daten.
 *
 * ## Der Ladezustand liegt im Block, nicht im Panel
 *
 * Wer die Kette lädt, will die Zeitleiste nicht verlieren — dieselbe Regel wie
 * bei den technischen Eigenschaften. Ein Fehler beim Auflösen der Kette darf das
 * Detail nicht mitreißen und erscheint deshalb inline.
 *
 * **Kein eigener Scrollbereich.** Bei 3.048 Kindern wird das Panel lang; das ist
 * in Ordnung, es scrollt mit `main`, dem einzigen senkrechten Scroller
 * (`frontend-grundlagen.md` §7).
 *
 * @param aufOeffnen öffnet ein Glied. Im Panel setzt das den Parameter
 *   `nachricht` in der URL, auf der eigenen Route führt es auf dieselbe Route
 *   mit der neuen Kennung — in beiden Fällen der **bestehende** Weg, und in
 *   beiden bleibt die Ansicht teilbar.
 */
export function KettenBlock({
  detail,
  aufOeffnen,
}: {
  detail: Nachrichtendetail;
  aufOeffnen: (messageId: string) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const hatRollen = detail.rollen.length > 0;

  const kette = useKette(detail.messageId, hatRollen);

  /**
   * Ob der Nutzer nachgeladen hat. **Kein Filterzustand** — er beschreibt keine
   * Ansicht, die jemand teilt, und gehört deshalb nicht in die URL. Beim
   * Blättern zwischen Nachrichten beginnt er wieder bei `false`, weil der
   * Aufrufer den Block über `key={messageId}` neu aufbaut.
   */
  const [nachgeladen, setNachgeladen] = useState(false);

  /**
   * Welche Abschnitte offen sind. **Hier und nicht im Abschnitt**, weil die
   * Nachladen-Schaltfläche im Sonderfall verteilter Abwärtsglieder für sich
   * steht und erscheint, sobald **einer** der beiden offen ist (§8.15). Anfangs
   * alle zu; nicht in URL, Cookie oder Storage; zurückgesetzt beim
   * Nachrichtenwechsel über `key` am Aufrufer, wie der Nachladezustand.
   */
  const [offene, setOffene] = useState<ReadonlySet<Abschnittsart>>(new Set());

  /**
   * **Das Blättern setzt hinter der Seite an, die schon dasteht.**
   * `kette.abwaertsCursor` ist die Position der letzten gezeigten Zeile, also
   * bringt die erste hier geholte Seite fünfzig **neue** Glieder — eine Anfrage
   * für einen Klick.
   *
   * Der Wert ist zum Zeitpunkt der ersten Anfrage da: `nachgeladen` wird nur
   * über die Schaltfläche wahr, und die gibt es erst, wenn die Kette geladen
   * ist.
   */
  const seiten = useKettenAbwaerts(
    detail.messageId,
    nachgeladen,
    kette.data?.abwaertsCursor ?? null,
  );

  const { data: seitenDaten, hasNextPage, isFetching, fetchNextPage } = seiten;

  if (!hatRollen) {
    return null;
  }

  if (kette.isPending) {
    return <KettenSkelett />;
  }

  if (kette.error) {
    return <Fehler fehler={kette.error} aufWiederholen={() => void kette.refetch()} />;
  }

  // Die Flags sagen, dass es eine Kette gibt; die Zeilen dazu holt der Endpunkt
  // trotzdem selbst (`verkettung.md` §4). Kommt dabei nichts heraus, gibt es
  // auch hier nichts zu zeigen.
  if (!hatInhalt(kette.data)) {
    return null;
  }

  // Die erste Seite bleibt stehen und wird ergänzt — nicht ersetzt. Genau das
  // ist der Unterschied, den `abwaertsCursor` macht.
  const abwaerts = gezeigteAbwaertsglieder(
    kette.data,
    seitenDaten?.pages.map((seite) => seite.items) ?? [],
  );
  const abschnitte = kettenabschnitte(kette.data, abwaerts);
  const weitere = seitenDaten ? hasNextPage : nachladenMoeglich(kette.data);
  const nachladenIn = abwaertsAbschnitt(abwaerts);

  const nachladen = weitere ? (
    <NachladenKnopf
      laeuft={isFetching}
      aufNachladen={() => {
        if (!nachgeladen) {
          setNachgeladen(true);
          return;
        }
        void fetchNextPage();
      }}
    />
  ) : null;

  // Ein Abschnitt mit genau einem Glied steht ohne Schalter offen — und zählt
  // deshalb als offen.
  const sichtbar = abschnitte.filter((abschnitt) => abschnitt.glieder.length > 0);
  const einerOffen = sichtbar.some(
    (abschnitt) => !abschnittAufklappbar(abschnitt) || offene.has(abschnitt.art),
  );

  return (
    <div className="flex flex-col gap-3">
      {abschnitte.map((abschnitt) =>
        abschnitt.glieder.length === 0 ? null : (
          <Abschnitt
            key={abschnitt.art}
            abschnitt={abschnitt}
            aufOeffnen={aufOeffnen}
            offen={offene.has(abschnitt.art)}
            aufWechsel={(offen) =>
              setOffene((bisher) => {
                const neu = new Set(bisher);
                if (offen) {
                  neu.add(abschnitt.art);
                } else {
                  neu.delete(abschnitt.art);
                }
                return neu;
              })
            }
            // Der Knopf steht unter dem Abschnitt, in dem die Abwärtsglieder
            // stehen. Verteilen sie sich auf beide — eine Zeile kann zugleich
            // Split-Wurzel und Merge-Ergebnis sein (M30‑4) —, ließe er sich
            // keinem zuordnen und steht darunter für sich.
            nachladen={nachladenIn === abschnitt.art ? nachladen : null}
          />
        ),
      )}

      {/*
        Eine Kette, die stillschweigend abbricht, ist schlimmer als eine, die
        sagt, dass sie abbricht. Beide Sätze stehen unter beiden Abschnitten:
        Der Aufstieg kann über sie hinweg verlaufen — ein Merge-Ergebnis, das
        selbst ein Split-Kind ist, hat Glieder in beiden —, und ein Satz an
        einem Abschnitt hinge dann am falschen.
      */}
      {kette.data.tiefeErreicht ? <Hinweis text={texte.nachrichten.kette.tiefeErreicht} /> : null}
      {kette.data.zyklusErkannt ? <Hinweis text={texte.nachrichten.kette.zyklusErkannt} /> : null}

      {/* Der Sonderfall verteilter Abwärtsglieder (§8.7): Die Schaltfläche lässt
          sich keinem Abschnitt zuordnen und steht für sich — **sobald einer der
          beiden offen ist.** Unter zwei zugeklappten Abschnitten lüde sie Zeilen
          nach, die niemand sieht. */}
      {nachladenIn === null && einerOffen ? nachladen : null}

      {seiten.error ? (
        <Fehler fehler={seiten.error} aufWiederholen={() => void fetchNextPage()} />
      ) : null}

      <span className="sr-only" aria-live="polite">
        {seitenDaten
          ? einsetzen(texte.nachrichten.kette.geladen, {
              anzahl: formatiereZahl(abwaerts.length, sprache),
            })
          : ""}
      </span>
    </div>
  );
}

/**
 * Ein Abschnitt: Überschrift und seine Glieder.
 *
 * **Die Überschrift nennt die Zahl, wo es eine gibt** — und nur dort. Für den
 * Abstieg liefert der Endpunkt sie gemessen genau (`abwaertsGesamt`, M30‑1);
 * für den Aufstieg gibt es keine, und die Länge der Liste ist bei
 * `tiefeErreicht` gerade nicht die Gesamtzahl. Eine Zahl, die der Endpunkt
 * nicht liefert, wird hier nicht behauptet.
 */
function Abschnitt({
  abschnitt,
  aufOeffnen,
  nachladen,
  offen,
  aufWechsel,
}: {
  abschnitt: Kettenabschnitt;
  aufOeffnen: (messageId: string) => void;
  nachladen: ReactNode;
  offen: boolean;
  aufWechsel: (offen: boolean) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const titelId = useId();
  const wortschatz = texte.nachrichten.kette[abschnitt.art];

  const ueberschrift =
    abschnitt.gesamt === null
      ? wortschatz.titel
      : abschnitt.gesamt === 1
        ? wortschatz.titelEins
        : einsetzen(wortschatz.titelZahl, { anzahl: formatiereZahl(abschnitt.gesamt, sprache) });

  const glieder = (
    <ul className="flex flex-col">
      {abschnitt.glieder.map((glied) => (
        <GliedZeile key={glied.messageId} glied={glied} aufOeffnen={aufOeffnen} />
      ))}
    </ul>
  );

  // **Genau ein Glied: ohne Schalter, offen** (E‑227). Die Überschrift hält die
  // Einrückung des Pfeils frei — 0,875 rem Pfeil und 0,375 rem Abstand —, damit
  // sie mit der eines aufklappbaren Abschnitts fluchtet, und ist so hoch wie
  // dessen Schaltfläche.
  if (!abschnittAufklappbar(abschnitt)) {
    return (
      <section aria-labelledby={titelId} className="flex flex-col gap-1">
        <h3
          id={titelId}
          className="text-muted-foreground text-beiwerk min-h-bedienelement flex items-center pl-5 font-medium"
        >
          {ueberschrift}
        </h3>
        {glieder}
        {nachladen}
      </section>
    );
  }

  return (
    // **Mehr als ein Glied: aufklappbar, anfangs zu, mit Pfeil in der Bauform
    // der Belegdaten** (E‑227). Die Regel des Panels: Blöcke klappen mit Pfeil,
    // Zeitleistenzeilen mit der Linie. Die Bewegung kommt aus `aufklappen.tsx`
    // (E‑228) — beim Nachladen bewegt sich nichts, der offene Inhalt wächst
    // einfach (§8.8).
    <Aufklappen asChild offen={offen} aufWechsel={aufWechsel}>
      <section aria-labelledby={titelId} className="flex flex-col">
        <h3 id={titelId} className="text-muted-foreground text-beiwerk font-medium">
          <AufklappSchalter
            title={offen ? texte.nachrichten.kette.zuklappen : texte.nachrichten.kette.aufklappen}
            className="hover:bg-muted focus-visible:ring-ring min-h-bedienelement -mx-1 flex w-fit max-w-full items-center gap-1.5 rounded-md px-1 text-left font-medium focus-visible:ring-2 focus-visible:outline-none"
          >
            <ChevronRight
              aria-hidden="true"
              className={cn(
                "size-3.5 shrink-0 opacity-70 transition-[rotate]",
                AUFKLAPP_UEBERGANG,
                offen && "rotate-90",
              )}
            />
            {ueberschrift}
          </AufklappSchalter>
        </h3>
        {/* Die Nachladen-Schaltfläche gehört zum Inhalt ihres Abschnitts: Unter
            einem zugeklappten lüde sie Zeilen nach, die niemand sieht.
            `randFuerFokus`: Die Glieder sind Schaltflächen, und ihr Fokusring
            darf an der Schnittkante der Höhenbewegung nicht abgeschnitten
            werden; die 0,25 rem oben sind der Abstand zur Überschrift. */}
        <AufklappInhalt randFuerFokus className="flex flex-col gap-1">
          {glieder}
          {nachladen}
        </AufklappInhalt>
      </section>
    </Aufklappen>
  );
}

/**
 * Ein Glied — **eine Zeile, und sie ist anfassbar wie eine Listenzeile.**
 *
 * Zeigehand, Hover-Fläche, Fokusring, `Tab`/`Enter`/`Leertaste`: Ein `button`
 * bringt das alles von sich aus mit, und ein Klick öffnet das Detail dieses
 * Glieds.
 *
 * **Feste Zeilenhöhe** (`--dichte-zeile`), gekürzt, Vollwert im `title` — nach
 * der Regel aus `nachrichtenliste.md` §8.1. Die gemessene Namenslänge geht bis
 * 61 Zeichen, und das Panel ist 26 rem breit.
 *
 * **Nicht eingerückt.** `aufwaerts` kann mehrere Ebenen tragen (E2: mindestens
 * vier), aber eine Einrückung je Ebene fräße genau die Breite, die die
 * Ablaufnamen brauchen. Die Reihenfolge trägt die Ebene; wo sie nicht genügt —
 * ab der zweiten Stufe —, steht sie als Beiwerk daneben.
 *
 * **Die aktuelle Nachricht wird nicht wiederholt.** Sie steht darüber im Kopf;
 * eine zweite Zeile für dieselbe Nachricht wäre eine Aussage, die keine ist.
 */
function GliedZeile({
  glied,
  aufOeffnen,
}: {
  glied: Kettenglied;
  aufOeffnen: (messageId: string) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const stufe = Math.abs(glied.ebene);
  const stufenText = einsetzen(texte.nachrichten.kette.stufe, { stufe });
  const name = glied.sosName ?? texte.nachrichten.nichtZugeordnet;

  const hinweis = [
    name,
    formatiereZeitpunktGenau(glied.zeitpunkt, sprache, zone),
    `${texte.nachrichten.kette.beziehung[glied.beziehung]} · ${stufenText}`,
  ].join("\n");

  return (
    <li>
      <button
        type="button"
        onClick={() => aufOeffnen(glied.messageId)}
        title={hinweis}
        aria-label={einsetzen(texte.nachrichten.kette.gliedOeffnen, { ablauf: name })}
        className={cn(
          "h-zeile hover:bg-muted focus-visible:ring-ring flex w-full cursor-pointer items-center",
          "gap-2 rounded-sm px-1 text-left focus-visible:ring-2 focus-visible:outline-none",
        )}
      >
        <StatusPlakette
          statusKind={glied.statusKind}
          rohwert={glied.status}
          bedeutungNichtVerifiziert={glied.statusKind === "UNGEKLAERT"}
          kompakt
        />
        <span className={cn("min-w-0 flex-1 truncate", glied.sosName === null && "italic")}>
          {name}
        </span>
        {stufe > 1 ? (
          <span className="text-muted-foreground text-beiwerk shrink-0">{stufenText}</span>
        ) : null}
        <time
          dateTime={glied.zeitpunkt}
          className="text-muted-foreground text-beiwerk shrink-0"
          data-ziffern
        >
          {formatiereZeitpunkt(glied.zeitpunkt, sprache, zone)}
        </time>
      </button>
    </li>
  );
}

/** Ein Satz in der ruhigen Farbrolle — keine Warnfarbe, keine Plakette. */
function Hinweis({ text }: { text: string }) {
  return <p className="text-muted-foreground text-beiwerk">{text}</p>;
}

/**
 * **Nachgeladen wird im Block, nicht gesprungen.**
 *
 * Naheliegend wäre ein Filter `?wurzel=…` an der Nachrichtenliste. Das bräche
 * Regel L1: Die Liste verlangt ein Pflicht-Zeitfenster, und die Kinder einer
 * drei Monate alten Wurzel lägen außerhalb jedes vernünftigen Fensters. Der
 * Cursor-Endpunkt aus Teil 1 ist ohnehin der billigere Zugriff — er steigt über
 * `SourceMessageIDIDX` ein und nicht über `MessageLastUpdateIDX` (E5).
 *
 * **Die Seite wird angehängt, nicht ersetzt.**
 */
function NachladenKnopf({ laeuft, aufNachladen }: { laeuft: boolean; aufNachladen: () => void }) {
  const texte = useTexte();
  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      className="min-h-beruehrung w-fit"
      onClick={aufNachladen}
      disabled={laeuft}
    >
      {laeuft ? texte.nachrichten.kette.laedtWeitere : texte.nachrichten.kette.weitereLaden}
    </Button>
  );
}

/**
 * **Ein Platzhalter in der Gestalt des späteren Blocks** — Überschrift und zwei
 * Zeilen —, kein Kreisel über einem leeren Kasten. Die Zeitleiste darunter
 * bleibt stehen.
 */
function KettenSkelett() {
  const texte = useTexte();
  return (
    <div className="flex flex-col gap-1" aria-busy="true">
      <span className="sr-only">{texte.zustand.laedt}</span>
      <Skeleton className="h-4 w-28" />
      {[0, 1].map((nummer) => (
        <Skeleton key={nummer} className="h-zeile w-full" />
      ))}
    </div>
  );
}
