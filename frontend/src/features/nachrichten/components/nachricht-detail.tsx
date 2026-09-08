"use client";

import { useCallback, useId, useState, type ReactNode } from "react";
import Link from "next/link";
import { Check, Copy, ListTree, X } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { Fehler } from "@/components/zustand";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereDauer, formatiereRelativ, formatiereZeitpunktGenau } from "@/lib/format";
import { ProblemFehler } from "@/lib/http";
import type { Texte } from "@/i18n";

import type { KuratierteEigenschaft, Nachrichtendetail } from "../api";
import { bedeutungNichtVerifiziert, METADATEN_POSITION } from "../detail";
import { useArtefakte, useNachrichtendetail } from "../hooks";
import { absprungZiel, absprungfenster } from "../prozessansicht";
import { zieleJeSchritt, zieleOhneZeile } from "../rohdaten";
import { AnsichtUmschalter, type Umschaltziel } from "./ansicht-umschalter";
import { Zielzeile } from "./artefakt-ziele";
import { BamBlock } from "./bam-block";
import { EigenschaftenBlock, type Sprungziel } from "./eigenschaften-block";
import { KettenBlock } from "./kette-block";
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
 * @param aufOeffnen öffnet ein Glied der Kette. Wie beim Schließen entscheidet
 *   der Einhängepunkt, was das heißt: im Panel der Parameter `nachricht` in der
 *   URL, auf der eigenen Route dieselbe Route mit der neuen Kennung. Beides ist
 *   der **bestehende** Weg, und beides bleibt teilbar.
 * @param umschaltenZu wohin der Umschalter im Kopf führt — aus dem Panel auf die
 *   eigene Route, von dort zurück ans Panel (`ansicht-umschalter.tsx`). Er tritt
 *   **neben** den Schließen-Knopf und nicht an seine Stelle.
 *
 *   <p><b>Beide Angaben sind seit Schritt 7, Teil 3 freiwillig.</b> Es gibt einen
 *   dritten Einhängepunkt: die Trefferliste der Belegsuche. Dort entfällt der
 *   Umschalter, und zwar nicht aus Platzgründen — der Rückweg von der eigenen
 *   Route führt an die **Liste** und nicht an die Suche (`lib/routen.ts`). Ein
 *   Umschalter, der woanders endet als dort, wo er herkam, ist keiner. Fehlen sie,
 *   erscheint er nicht; alles Übrige bleibt unverändert.
 * @param aufUmschalten der Weg dorthin, wieder vom Einhängepunkt gestellt.
 * @param prozessbaumFenster das Fenster, **aus dem gesprungen wird** — für den
 *   Absprung „Im Prozessbaum anzeigen" (E‑103, `docs/property-suche.md` §12).
 *   Der Einhängepunkt stellt es, wenn er ein **absolutes** kennt: die Belegsuche
 *   aus ihrer Antwort, die Nachrichtenliste aus `von`/`bis` der URL. Fehlt es
 *   — relativer Modus der Liste, eigene Route, Prozessansicht —, gibt es keinen
 *   Link: Ein Link, der in einem anderen Fenster landet als versprochen, ist
 *   schlechter als kein Link (E‑104).
 */
export function NachrichtDetail({
  messageId,
  aufSchliessen,
  schliessenText,
  aufOeffnen,
  umschaltenZu,
  aufUmschalten,
  prozessbaumFenster,
}: {
  messageId: string;
  aufSchliessen: () => void;
  schliessenText: string;
  aufOeffnen: (messageId: string) => void;
  umschaltenZu?: Umschaltziel;
  aufUmschalten?: () => void;
  prozessbaumFenster?: { von: Date; bis: Date };
}) {
  const texte = useTexte();
  const anfrage = useNachrichtendetail(messageId);
  const titelId = useId();

  // **Die Artefaktliste wird seit dem 18.08.2026 mit dem Detail geholt und nicht
  // mehr erst beim Aufklappen.** Der Block, der sie aufklappte, gibt es nicht
  // mehr; die Ziele hängen an der Zeitleiste, und die steht immer da.
  //
  // Der Preis ist eine Anfrage je Detailaufruf, und er ist gemessen klein: Die
  // Liste ist eine reine Datenbankabfrage von 0,867 ms
  // (`docs/rohdaten-backend.md` §9) und **spricht keine Ablage an** — der
  // SOAP-Aufruf steckt allein im Inhalt, und der wird weiterhin erst beim
  // Öffnen der Ansicht geholt.
  const artefakte = useArtefakte(messageId, true);

  // Der angesprungene Schritt trägt seine Nachricht mit sich, und beim Wechsel
  // fällt er weg. **Diese Komponente wird beim Blättern nicht neu aufgebaut** —
  // nur ihre Kinder tragen ein `key`. Ohne das Zurücksetzen klappte der
  // Eigenschaftenblock der nächsten Nachricht von selbst auf, und wer später
  // zur ersten zurückkehrt, spränge dort ein zweites Mal.
  //
  // Zurückgesetzt wird beim Rendern und nicht in einem Effekt: Es ist ein
  // Zustand, der sich aus einer Eigenschaft ergibt (`react.dev`, *adjusting
  // state when a prop changes*), und ein Effekt dafür löste eine zweite
  // Renderrunde aus.
  const [sprung, setSprung] = useState<(Sprungziel & { messageId: string }) | null>(null);
  if (sprung !== null && sprung.messageId !== messageId) {
    setSprung(null);
  }

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
        {/* Erst umschalten, dann schließen — die Reihenfolge im DOM ist die
            Reihenfolge unter `Tab`, und „anders zeigen" steht vor „weg damit".
            Ohne Ziel gibt es ihn nicht: Die Suche hat keinen zweiten
            Einhängepunkt, an den er zurückführen könnte. */}
        {umschaltenZu === undefined || aufUmschalten === undefined ? null : (
          <AnsichtUmschalter zu={umschaltenZu} aufUmschalten={aufUmschalten} />
        )}
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
          <Kopf detail={anfrage.data} prozessbaumFenster={prozessbaumFenster} />
          {/*
            Die Kette sitzt zwischen Kopf und Zeitleiste: Sie beantwortet „was
            hängt daran" und steht damit näher an der Nachricht selbst als der
            Ablauf ihrer Schritte. `key` baut sie beim Blättern zwischen
            Nachrichten neu auf — sonst überlebte der Nachladezustand einer
            Kette die Nachricht, zu der er gehört.

            Der Schlüssel trägt einen Namen davor, weil er sich sonst mit dem
            des Eigenschaftenblocks deckte: Zwei Geschwister mit demselben
            `key` sind für React derselbe Platz im Baum. Aufgefallen in der
            Sichtprüfung am 11.08.2026 als Konsolenmeldung.
          */}
          <KettenBlock
            key={`kette-${anfrage.data.messageId}`}
            detail={anfrage.data}
            aufOeffnen={aufOeffnen}
          />
          {/*
            Die Belegdaten sitzen zwischen Kettenblock und Zeitleiste: Sie
            beantworten „welcher Beleg ist das", die Zeitleiste „was ist damit
            passiert". Nach dem Leitsatz kommt die erste Frage zuerst — der
            typische Nutzer sucht einen Beleg.

            `key` mit eigenem Präfix, wie bei den Nachbarn: Drei Geschwister mit
            demselben Schlüssel wären für React derselbe Platz im Baum. Der
            Befund dazu stammt vom 11.08.2026 (`verkettung.md` §8.12), und
            `tests/detail-baum.test.tsx` hält ihn fest.
          */}
          <BamBlock
            key={`bam-${anfrage.data.messageId}`}
            messageId={anfrage.data.messageId}
            anzahl={anfrage.data.bamAnzahl}
          />
          <Ablauf
            detail={anfrage.data}
            artefakte={artefakte}
            sprung={sprung}
            aufSprung={(position) =>
              setSprung((bisher) => ({
                messageId,
                position,
                // Die laufende Nummer, damit derselbe Schritt zweimal
                // hintereinander zweimal wirkt.
                nummer: (bisher?.nummer ?? 0) + 1,
              }))
            }
          />
        </>
      ) : null}
    </section>
  );
}

/**
 * **Der Ablauf einer Nachricht: die Zeitleiste, ihre Ziele und die technischen
 * Eigenschaften darunter** — seit dem 18.08.2026 eine Einheit statt dreier
 * gleichrangiger Listen derselben Sache.
 *
 * ## Was hier steht, und in welcher Reihenfolge
 *
 * | | |
 * |---|---|
 * | **Eingang** | alles auf Schritt `0` — seit dem 19.08.2026 das Paar des Lesedienstes, Datei und Protokoll (M73). Schritt `0` hängt an keinem Ablaufschritt und steht deshalb **über** der Leiste |
 * | **Zeitleiste** | je Schritt Name, Balken, Dauer — und die Artefakte, die auf ihm liegen |
 * | **Ohne Schritt in der Zeitleiste** | der Rest. Gemessen leer (M57, Befund 1), gebaut, damit kein Artefakt lautlos verschwindet |
 * | **Technische Eigenschaften** | das Technischste zuletzt, nach Schritt gruppiert und aus der Leiste anspringbar |
 *
 * ## Warum die Dateien keinen eigenen Block mehr haben
 *
 * Sie hatten einen, vom 18.08.2026 bis zum selben Tag. Im gebauten Zustand
 * standen darin **neun Zeilen mit vier sich wiederholenden Schrittnamen** —
 * denselben, die drei Zeilen darüber in der Zeitleiste schon standen, dort mit
 * Dauer und Balken. Der Fehler lag in der Entscheidung und nicht in der
 * Umsetzung: Entscheidung 6 entstand, bevor M57 zeigte, dass Artefakte **am
 * Schritt** hängen (`docs/rohdaten.md` §3).
 *
 * **Die technischen Eigenschaften bleiben ein eigener Block**, und die
 * Trennlinie ist nicht „gehört zum Schritt oder nicht", sondern **„ein Ziel oder
 * ein Textblock"**: Dateien sind null bis zwei Verweise je Schritt,
 * Eigenschaften rund 23 Schlüssel-Wert-Paare je Nachricht (M44). Zwei Ziele
 * passen in eine Schrittzeile, zehn Wertepaare sprengen sie.
 */
function Ablauf({
  detail,
  artefakte,
  sprung,
  aufSprung,
}: {
  detail: Nachrichtendetail;
  artefakte: ReturnType<typeof useArtefakte>;
  sprung: Sprungziel | null;
  aufSprung: (position: number) => void;
}) {
  const texte = useTexte();
  const bausteine = texte.nachrichten.detail.dateien;

  // Beschriftet wird mit der Schrittfolge, die ohnehin im Baum liegt — dieselbe
  // Verbindung, aus der die Gruppenköpfe des Eigenschaftenblocks entstehen
  // (17.08.2026). **Keine zweite Anfrage.**
  const ziele = zieleJeSchritt(artefakte.data, detail.schritte, texte);

  return (
    <>
      {/*
        **Eingang, Leiste und Rest stehen bündig aufeinander, ohne Abstand.**
        Sie tragen dieselbe senkrechte Kontur links — die Schiene der Zeitleiste
        —, und ein Abstand von 16 rem/4 dazwischen zerschnitte sie in drei
        Stücke. Der Rahmen darum hält seinen Abstand zu den Nachbarblöcken; nach
        innen gibt es keinen.
      */}
      <div className="flex flex-col">
        <Zielzeile
          messageId={detail.messageId}
          beschriftung={bausteine.eingang}
          hinweis={bausteine.eingangHinweis}
          ziele={ziele.get(METADATEN_POSITION) ?? []}
        />

        <Zeitleiste
          detail={detail}
          ziele={ziele}
          // Ohne Eigenschaften gibt es unten keinen Block, und dann bleiben die
          // Schrittnamen Text. Ein Weg, der ins Leere führte, wäre schlechter
          // als keiner — dieselbe Regel, aus der der Block dort einen Satz statt
          // eines Schalters zeigt.
          aufSchritt={detail.eigenschaftenAnzahl === 0 ? undefined : aufSprung}
        />

        <Zielzeile
          messageId={detail.messageId}
          beschriftung={bausteine.ohneZeile}
          ziele={zieleOhneZeile(ziele, detail.schritte)}
        />
      </div>

      {/*
        Die Zeitleiste hängt an einem anderen Endpunkt als die Artefakte: Fällt
        deren Abfrage aus, steht die Leiste weiterhin und es fehlen allein die
        Ziele. Gesagt wird das trotzdem — sonst sähe eine Nachricht ohne
        erreichbare Dateien aus wie eine ohne Dateien, und die gibt es gemessen
        nicht (M55: 3 bis 15, bei jedem Mandanten).

        Der gewöhnliche Baustein und kein eigener: Vier Zustände je Ansicht, und
        die werden nicht je Ansicht nachgebaut (`components/zustand.tsx`).
      */}
      {artefakte.error ? (
        <Fehler
          fehler={artefakte.error}
          text={bausteine.zieleFehlgeschlagen}
          aufWiederholen={() => void artefakte.refetch()}
        />
      ) : null}

      <EigenschaftenBlock
        // Beim Blättern zwischen Nachrichten beginnt der Block wieder
        // eingeklappt — und lädt damit auch nichts nach. `key` mit eigenem
        // Präfix, wie bei den Nachbarn: Geschwister mit demselben Schlüssel
        // wären für React derselbe Platz im Baum (`verkettung.md` §8.12).
        key={`eigenschaften-${detail.messageId}`}
        messageId={detail.messageId}
        anzahl={detail.eigenschaftenAnzahl}
        // Nur zum Beschriften der Gruppen (17.08.2026). Es ist dieselbe Liste,
        // aus der die Zeitleiste darüber entsteht — genau deshalb stehen die
        // Gruppen in derselben Reihenfolge und tragen wortgleich dieselben
        // Namen.
        schritte={detail.schritte}
        sprung={sprung}
      />
    </>
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
function Kopf({
  detail,
  prozessbaumFenster,
}: {
  detail: Nachrichtendetail;
  prozessbaumFenster?: { von: Date; bis: Date };
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  /*
   * **Der Absprung in den Prozessbaum (E‑103) — ein Link und kein Bau.** Er
   * führt auf die bestehende Route `/prozesse` mit dem Fenster, aus dem man
   * kommt, **absolut** aufgelöst (E‑104), dem Prozess dieser Nachricht und der
   * Nachricht selbst; der Baum klappt den Partner über `pfadZuProzess` selbst
   * auf. Gerundet wird auf volle Stunden nach außen und am Jahr gedeckelt
   * (`prozessansicht.ts` `absprungfenster`); liegt die Nachricht danach
   * außerhalb, gibt es keinen Link. **Hier im Panel und nicht als Kontextmenü
   * oder Spalte** — Tastatur- und Berührungserreichbarkeit, dieselbe Disziplin
   * wie bei den Berührungsflächen des Baums.
   */
  const baumfenster =
    prozessbaumFenster === undefined
      ? null
      : absprungfenster(prozessbaumFenster, new Date(detail.zeitpunkt));

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
        {/*
          Die Gesamtdauer steht neben Beginn und Zeitpunkt, weil sie genau die
          Spanne zwischen den beiden ist. Sie ist die Abdeckung für Zeit, die
          zwischen zwei Schritten steckt und in keiner Schrittdauer auftaucht:
          Passt die Summe der Schrittdauern nicht dazu, lag die Nachricht
          dazwischen. Genau dafür war die gestrichene Lückenzeile gedacht.
        */}
        <Feld beschriftung={texte.nachrichten.detail.gesamtdauer}>
          {detail.gesamtdauerSekunden === null ? (
            texte.nachrichten.ohneWert
          ) : (
            <span data-ziffern>
              {formatiereDauer(detail.gesamtdauerSekunden, texte.nachrichten.detail.dauer)}
            </span>
          )}
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

      {baumfenster === null ? null : (
        /* Ein echter Verweis: mit der mittleren Maustaste zu öffnen, zu
           kopieren, per Tab erreichbar. Die Mindestfläche am Finger bringt
           `min-h-beruehrung` mit — dieselbe Regel wie bei den Baumzeilen. */
        <Link
          href={absprungZiel(detail.processId, detail.messageId, baumfenster)}
          className="text-beiwerk min-h-beruehrung focus-visible:ring-ring inline-flex w-fit items-center gap-1 rounded-sm underline-offset-4 hover:underline focus-visible:ring-2 focus-visible:outline-none"
          data-absprung="prozessbaum"
        >
          <ListTree aria-hidden="true" className="size-4 shrink-0 opacity-70" />
          {texte.nachrichten.detail.imProzessbaum}
        </Link>
      )}

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
