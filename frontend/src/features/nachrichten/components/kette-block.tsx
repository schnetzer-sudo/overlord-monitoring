"use client";

import { useId, useState, type ReactNode } from "react";

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
  abwaertsAbschnitt,
  gezeigteAbwaertsglieder,
  hatInhalt,
  kettenabschnitte,
  nachladenMoeglich,
  type Kettenabschnitt,
} from "../kette";
import { useKette, useKettenAbwaerts } from "../hooks";
import { StatusPlakette } from "./status-plakette";

/**
 * **Was hängt an dieser Nachricht** — die Kette, zwischen Kopf und Zeitleiste.
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

  return (
    <div className="flex flex-col gap-3">
      {abschnitte.map((abschnitt) =>
        abschnitt.glieder.length === 0 ? null : (
          <Abschnitt
            key={abschnitt.art}
            abschnitt={abschnitt}
            aufOeffnen={aufOeffnen}
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

      {nachladenIn === null ? nachladen : null}

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
}: {
  abschnitt: Kettenabschnitt;
  aufOeffnen: (messageId: string) => void;
  nachladen: ReactNode;
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

  return (
    <section aria-labelledby={titelId} className="flex flex-col gap-1">
      <h3 id={titelId} className="text-muted-foreground text-beiwerk font-medium">
        {ueberschrift}
      </h3>
      <ul className="flex flex-col">
        {abschnitt.glieder.map((glied) => (
          <GliedZeile key={glied.messageId} glied={glied} aufOeffnen={aufOeffnen} />
        ))}
      </ul>
      {nachladen}
    </section>
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
