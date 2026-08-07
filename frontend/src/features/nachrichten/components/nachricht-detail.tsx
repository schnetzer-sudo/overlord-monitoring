"use client";

import { useCallback, useId, useState, type ReactNode } from "react";
import { Check, Copy, X } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Fehler } from "@/components/zustand";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereRelativ, formatiereZeitpunktGenau } from "@/lib/format";
import { ProblemFehler } from "@/lib/http";
import type { Texte } from "@/i18n";

import type { KuratierteEigenschaft, Nachrichtendetail } from "../api";
import { bedeutungNichtVerifiziert } from "../detail";
import { useNachrichtendetail } from "../hooks";
import { EigenschaftenBlock } from "./eigenschaften-block";
import { StatusPlakette } from "./status-plakette";
import { Zeitleiste } from "./zeitleiste";

/**
 * Was mit **einer** Nachricht passiert ist — in verständlicher Sprache.
 *
 * ## Eine Komponente, zwei Einhängepunkte
 *
 * | Weg | Verhalten |
 * |---|---|
 * | `/nachrichten?nachricht=<id>` | Panel neben der Liste, die Liste bleibt im Blick |
 * | `/nachrichten/<id>` | dieselbe Komponente als eigene Seite |
 *
 * **Keine abfangenden Routen** (`(.)`-Konvention des App Routers). Das ist der
 * komplexeste Teil des Routings für einen Gewinn, den wir nicht brauchen: Zwei
 * schlichte Einhängepunkte und eine Komponente leisten dasselbe und sind zu
 * lesen, ohne die Konvention zu kennen.
 *
 * Die eigene Route existiert, weil ein Link auf einen Beleg die eigentliche
 * Anwendung ist — „schick mir mal den Link" — und weil die BAM-Suche in
 * Schritt 7 einen Einstieg ohne Liste braucht.
 *
 * ## Sie hängt nicht am Ergebnis der Liste
 *
 * Geladen wird über die eigene Kennung. Ein tiefer Link auf eine Nachricht
 * außerhalb des aktuellen Zeitfensters zeigt die Nachricht, auch wenn die Liste
 * dahinter leer ist. Das ist gewollt und darf nicht „repariert" werden, indem
 * die Ansicht auf die Listendaten zugreift.
 *
 * @param aufSchliessen entfernt im Panel den Parameter aus der URL, auf der
 *   eigenen Route führt es zurück zur Liste.
 * @param schliessenText die Beschriftung dafür — „Schließen" gegen „Zurück zur
 *   Liste". Es ist derselbe Vorgang mit zwei Bedeutungen.
 */
export function NachrichtDetail({
  messageId,
  aufSchliessen,
  schliessenText,
}: {
  messageId: string;
  aufSchliessen: () => void;
  schliessenText: string;
}) {
  const texte = useTexte();
  const anfrage = useNachrichtendetail(messageId);
  const titelId = useId();

  return (
    <section
      aria-labelledby={titelId}
      className="border-border bg-card flex flex-col gap-4 rounded-lg border p-3"
    >
      <div className="flex items-start justify-between gap-2">
        <h2 id={titelId} className="text-ueberschrift min-w-0 flex-1 font-semibold">
          {anfrage.data ? (
            <span className="block truncate" title={anfrage.data.sosName ?? undefined}>
              {anfrage.data.sosName ?? (
                <span className="text-muted-foreground italic">
                  {texte.nachrichten.nichtZugeordnet}
                </span>
              )}
            </span>
          ) : (
            texte.nachrichten.detail.titel
          )}
        </h2>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="min-h-bedienelement shrink-0"
          onClick={aufSchliessen}
          title={schliessenText}
        >
          <X aria-hidden="true" />
          <span className="sr-only">{schliessenText}</span>
        </Button>
      </div>

      {anfrage.isPending ? (
        <DetailSkelett />
      ) : anfrage.error ? (
        <DetailFehler fehler={anfrage.error} aufWiederholen={() => void anfrage.refetch()} />
      ) : anfrage.data ? (
        <>
          <Kopf detail={anfrage.data} />
          <Zeitleiste detail={anfrage.data} />
          <EigenschaftenBlock
            // Beim Blättern zwischen Nachrichten beginnt der Block wieder
            // eingeklappt — und lädt damit auch nichts nach.
            key={anfrage.data.messageId}
            messageId={anfrage.data.messageId}
            anzahl={anfrage.data.eigenschaftenAnzahl}
          />
        </>
      ) : null}
    </section>
  );
}

/**
 * Der Kopf: Status, Zeiten, Zuordnung, kuratierte Felder, Kennung.
 *
 * **Er muss null kuratierte Felder aushalten.** Die Auswahl ist faktisch
 * mandantenabhängig — bei `ZAST` und `SYSTEM` ist keines der beiden Felder je
 * befüllt, und leere Werte liefert das Backend gar nicht erst. Der Kopf darf
 * dann nicht zu einem leeren Kasten mit Rahmen werden; er zeigt schlicht das,
 * was da ist.
 */
function Kopf({ detail }: { detail: Nachrichtendetail }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const zeit = (wert: string | null) => {
    if (wert === null) {
      return { text: texte.nachrichten.ohneWert, hinweis: undefined };
    }
    return {
      text: formatiereZeitpunktGenau(wert, sprache, zone),
      hinweis: formatiereRelativ(wert, sprache),
    };
  };

  const zeitpunkt = zeit(detail.zeitpunkt);
  const start = zeit(detail.start);

  return (
    <div className="flex flex-col gap-3">
      <StatusPlakette
        statusKind={detail.statusKind}
        rohwert={detail.status}
        // Das Detail führt `bedeutungNichtVerifiziert` nicht — es ist genau die
        // Einordnung `UNGEKLAERT` und wird deshalb an einer Stelle abgeleitet
        // (`../detail.ts`) statt hier nachgebaut.
        bedeutungNichtVerifiziert={bedeutungNichtVerifiziert(detail.statusKind)}
      />

      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
        <Feld beschriftung={texte.nachrichten.detail.zeitpunkt} hinweis={zeitpunkt.hinweis}>
          {zeitpunkt.text}
        </Feld>
        <Feld beschriftung={texte.nachrichten.detail.start} hinweis={start.hinweis}>
          {start.text}
        </Feld>
        <Feld beschriftung={texte.nachrichten.detail.projekt}>
          <Zuordnung wert={detail.projectName} />
        </Feld>
        <Feld beschriftung={texte.nachrichten.detail.prozess}>
          <Zuordnung wert={detail.processName} />
        </Feld>
        {detail.kuratierteEigenschaften
          .slice()
          .sort((a, b) => a.rang - b.rang)
          .map((eigenschaft) => (
            <Feld
              key={`${eigenschaft.name}-${eigenschaft.rang}-${eigenschaft.wert}`}
              beschriftung={kuratierteBeschriftung(eigenschaft, texte)}
            >
              {eigenschaft.wert}
            </Feld>
          ))}
      </dl>

      <Kennung messageId={detail.messageId} />
    </div>
  );
}

function Feld({
  beschriftung,
  hinweis,
  children,
}: {
  beschriftung: string;
  hinweis?: string;
  children: ReactNode;
}) {
  return (
    <>
      <dt className="text-muted-foreground text-beiwerk self-center">{beschriftung}</dt>
      {/* Eine Zeile hoch, gekürzt, Vollwert im `title` — dieselbe Regel wie in
          der Liste (`nachrichtenliste.md` §8.1). */}
      <dd className="min-w-0 truncate" title={hinweis}>
        {children}
      </dd>
    </>
  );
}

/**
 * Die deutsche Beschriftung eines kuratierten Felds.
 *
 * **Ohne Übersetzung erscheint der Rohname**, sichtbar unfertig. Das ist besser
 * als ihn zu verstecken: Ein neuer Name aus dem Altsystem fällt beim ersten
 * Blick auf, statt lautlos zu fehlen.
 */
function kuratierteBeschriftung(eigenschaft: KuratierteEigenschaft, texte: Texte): string {
  const katalog = texte.nachrichten.detail.kuratiert as Record<string, string | undefined>;
  return katalog[eigenschaft.name] ?? eigenschaft.name;
}

/** „Nicht zugeordnet heißt nicht zugeordnet" (Regel Q4) — und sieht auch so aus. */
function Zuordnung({ wert }: { wert: string | null }) {
  const texte = useTexte();
  if (wert === null || wert === "") {
    return (
      <span className="text-muted-foreground italic">{texte.nachrichten.nichtZugeordnet}</span>
    );
  }
  return <span title={wert}>{wert}</span>;
}

/**
 * Die `MessageID` kehrt hier zurück, nachdem sie aus der Liste geflogen ist.
 *
 * **Klein, unauffällig, mit Kopierfunktion.** Sie ist Beiwerk nach dem Leitsatz
 * — aber sie ist das, was jemand in eine E-Mail an die EDI-Betreuung schreibt,
 * und ohne Kopierfunktion trägt eine `varchar(36)`-UUID nichts.
 */
function Kennung({ messageId }: { messageId: string }) {
  const texte = useTexte();
  const [kopiert, setKopiert] = useState(false);

  const kopiere = useCallback(() => {
    // Scheitert es — kein sicherer Kontext, kein Recht —, bleibt der Wert
    // sichtbar und markierbar stehen. Eine Fehlermeldung für einen Knopf, der
    // Beiwerk kopiert, wäre lauter als die Sache.
    void navigator.clipboard
      ?.writeText(messageId)
      .then(() => setKopiert(true))
      .catch(() => undefined);
  }, [messageId]);

  return (
    <div className="flex min-w-0 items-center gap-1">
      <span className="text-muted-foreground text-beiwerk shrink-0">
        {texte.nachrichten.detail.kennung}
      </span>
      <code className="text-beiwerk min-w-0 truncate font-mono" title={messageId}>
        {messageId}
      </code>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        className="min-h-bedienelement shrink-0"
        onClick={kopiere}
        title={
          kopiert
            ? texte.nachrichten.detail.kennungKopiert
            : texte.nachrichten.detail.kennungKopieren
        }
      >
        {kopiert ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
        <span className="sr-only">
          {kopiert
            ? texte.nachrichten.detail.kennungKopiert
            : texte.nachrichten.detail.kennungKopieren}
        </span>
      </Button>
    </div>
  );
}

/**
 * **Ein Platzhalter in der Gestalt der späteren Ansicht**, kein Kreisel über
 * einem leeren Kasten: Kopfzeilen, ein paar Schrittzeilen, der Block darunter.
 * Die Liste dahinter bleibt bedienbar.
 */
function DetailSkelett() {
  const texte = useTexte();
  return (
    <div className="flex flex-col gap-3" aria-busy="true">
      <span className="sr-only">{texte.zustand.laedt}</span>
      <Skeleton className="h-zeile w-32" />
      <div className="flex flex-col gap-1">
        {[0, 1, 2, 3].map((nummer) => (
          <Skeleton key={nummer} className="h-5 w-full" />
        ))}
      </div>
      <div className="flex flex-col gap-1">
        {[0, 1, 2].map((nummer) => (
          <Skeleton key={nummer} className="h-zeile w-full" />
        ))}
      </div>
    </div>
  );
}

/**
 * Der Fehlerzustand — **inline, und das Panel schließt sich nicht.**
 *
 * Ein Link, der nichts tut, ist schlechter als einer, der sagt warum. Deshalb
 * keine eigene Fehlerseite: Panel und Route zeigen denselben Zustand an
 * derselben Stelle.
 *
 * **Der Text ist für „gibt es nicht" und „gehört einem anderen Mandanten"
 * identisch.** Das Backend macht die beiden Fälle absichtlich ununterscheidbar
 * (404 statt 403); eine Oberfläche, die „keine Berechtigung" schriebe, gäbe
 * genau das preis, was diese Regel schützt. Genannt wird stattdessen der Mandant
 * in der Kopfzeile als das, was die Sichtbarkeit bestimmt — für beide Fälle
 * wahr, und für einen Admin die eigentliche Handlungsanweisung: Er sieht immer
 * nur einen Mandanten gleichzeitig, und ein geteilter Link zeigt erst nach dem
 * Wechsel etwas.
 *
 * Wiederholt wird ein `404` nicht (`lib/query-client.ts`) — deshalb steht bei
 * ihm auch keine Schaltfläche dafür.
 */
function DetailFehler({ fehler, aufWiederholen }: { fehler: unknown; aufWiederholen: () => void }) {
  const texte = useTexte();

  if (fehler instanceof ProblemFehler && fehler.status === 404) {
    return (
      <div className="border-border text-muted-foreground max-w-prose rounded-md border border-dashed p-3">
        <p className="text-foreground font-medium">{texte.fehler["nicht-gefunden"]}</p>
        <p className="text-beiwerk mt-1">{texte.nachrichten.detail.nichtGefunden}</p>
      </div>
    );
  }

  // Für alles andere der gewöhnliche Baustein: Er übersetzt über den `type` der
  // `problem+json`-Antwort und zeigt die Fehler-Kennung nur dort, wo sie hilft.
  // Vier Zustände je Ansicht, und die Bausteine dafür werden nicht je Ansicht
  // nachgebaut (`components/zustand.tsx`).
  return <Fehler fehler={fehler} aufWiederholen={aufWiederholen} />;
}
