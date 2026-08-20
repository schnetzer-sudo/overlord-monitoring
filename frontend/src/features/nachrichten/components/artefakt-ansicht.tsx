"use client";

import { useId } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  Binary,
  CloudOff,
  Download,
  FileX,
  Info,
  ScrollText,
  type LucideIcon,
} from "lucide-react";

import { Fehler } from "@/components/zustand";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereZahl } from "@/lib/format";
import { ProblemFehler } from "@/lib/http";
import { nachrichtAnsicht } from "@/lib/routen";

import type { Artefaktanzeige } from "../api";
import { useArtefakte, useArtefaktinhalt, useNachrichtendetail } from "../hooks";
import {
  anzeigevermerke,
  artefaktBeschriftung,
  downloadMoeglich,
  downloadPfad,
  erneutVersuchenSinnvoll,
  findeArtefakt,
} from "../rohdaten";

/**
 * **Ein Artefakt, im Browser gelesen.**
 *
 * Die Anzeige ist der Regelfall, nicht der Download — das ist die tragende
 * Änderung gegenüber dem Implementierungsplan (`docs/rohdaten.md` §1).
 *
 * ## Die eine Regel, die nicht verhandelbar ist
 *
 * **Der Dateiinhalt wird als Textknoten gerendert. Niemals als HTML.** Kein
 * `dangerouslySetInnerHTML`, kein `iframe`, kein `srcDoc`, keine Blob-URL, kein
 * `window.open` auf einen Inhalt. Eine EDI-Datei kann gültiges HTML oder SVG
 * enthalten; der Inhalt kommt vom Partner und ist von außen befüllbar. Ein
 * `{text}` in JSX ist genau das Richtige — React erzeugt daraus einen Textknoten
 * und nichts sonst. `tests/artefakt-ansicht.test.tsx` hält es mit einem Inhalt
 * fest, der gültiges HTML ist.
 *
 * ## Ein Textknoten, kein Element je Zeile
 *
 * Das größte gemessene Artefakt hat **609.995 Byte** (M60). In ein Element je
 * Zeile zerlegt wären das Zehntausende Knoten, und die Seite wäre unbenutzbar.
 * Der Text steht deshalb als **ein** Kind in einem `<pre>`.
 *
 * Daraus folgt unmittelbar: **keine Zeilennummern im MVP.** Sie erzwingen die
 * Zerlegung. Als offener Punkt notiert, nicht heimlich eingebaut.
 *
 * ## Keine Aufbereitung
 *
 * Festbreitenschrift, keine Umformatierung, keine Syntaxhervorhebung. Die vier
 * abgeschalteten Formatumwandlungen des Altsystems bleiben abgeschaltet
 * (`PROJEKTBESCHREIBUNG.md` §9).
 *
 * ## Drei Abfragen, und jede hat ihren Grund
 *
 * | Abfrage | wofür |
 * |---|---|
 * | `…/dateien/{id}/inhalt` | der Inhalt. **Erst hier**, nie mit der Liste — dahinter steht ein SOAP-Abruf gegen die Ablage |
 * | `…/dateien` | der Eintrag zu dieser Kennung: Art, Schritt, Familie, Ausschnitt |
 * | `…/{messageId}` | die Schrittfolge, **allein zum Beschriften** (`../rohdaten.ts`) |
 *
 * Die beiden letzten liegen im Zwischenspeicher, sobald jemand aus dem
 * Nachrichtendetail hierher gekommen ist — dann kostet die Ansicht **eine**
 * Anfrage. Bei einem geteilten Verweis sind es drei, und beide zusätzlichen sind
 * reine Datenbankabfragen unter einer Millisekunde (`docs/rohdaten-backend.md`
 * §9). **Ohne sie stünde über der Datei eine Kennung statt eines Namens** — und
 * dass beide Stellen denselben Namen sagen, ist der Grund, warum sie aus
 * derselben Funktion kommen.
 */
export function ArtefaktAnsicht({
  messageId,
  artefaktId,
}: {
  messageId: string;
  artefaktId: string;
}) {
  const texte = useTexte();
  const titelId = useId();
  const bausteine = texte.nachrichten.detail.dateien;

  const inhalt = useArtefaktinhalt(messageId, artefaktId);
  const liste = useArtefakte(messageId, true);
  const detail = useNachrichtendetail(messageId);

  const artefakt = findeArtefakt(liste.data, artefaktId);
  const beschriftung =
    artefakt === null ? null : artefaktBeschriftung(artefakt, detail.data?.schritte ?? [], texte);

  return (
    <section aria-labelledby={titelId} className="flex flex-col gap-4">
      {/* Der Weg zurück steht **über** dem Titel und ist ein echter Verweis:
          Die eigene Route ist verlinkbar (Entscheidung 7), und ihr Rückweg soll
          es auch sein. Er führt an die Nachricht, nicht an die Liste — das ist
          die Station, aus der die Datei stammt. */}
      <Link
        href={nachrichtAnsicht(messageId)}
        className="text-muted-foreground hover:text-foreground focus-visible:ring-ring min-h-bedienelement text-beiwerk -mx-1 flex w-fit items-center gap-1.5 rounded-md px-1 focus-visible:ring-2 focus-visible:outline-none"
      >
        <ArrowLeft aria-hidden="true" className="size-3.5 shrink-0" />
        {bausteine.zurueck}
      </Link>

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="flex min-w-0 flex-col gap-0.5">
            <h1 id={titelId} className="text-ueberschrift font-semibold break-words">
              {/* Solange die Liste lädt, steht hier der allgemeine Titel statt
                  einer Kennung. Eine erfundene Beschriftung gäbe es nicht, und
                  ein leerer Titel wäre schlechter als ein allgemeiner. */}
              {beschriftung ?? bausteine.ansichtTitel}
            </h1>
            <Herkunftszeile anzeige={inhalt.data} />
          </div>

          {inhalt.data !== undefined && downloadMoeglich(inhalt.data) ? (
            <DownloadKnopf messageId={messageId} artefaktId={artefaktId} />
          ) : null}
        </div>

        {inhalt.isPending ? (
          <InhaltSkelett />
        ) : inhalt.error ? (
          <InhaltFehler fehler={inhalt.error} aufWiederholen={() => void inhalt.refetch()} />
        ) : inhalt.data === undefined ? null : (
          <>
            <Vermerke anzeige={inhalt.data} />
            <Inhalt anzeige={inhalt.data} aufWiederholen={() => void inhalt.refetch()} />
          </>
        )}
      </div>
    </section>
  );
}

/**
 * **Die Herkunftszeile** — Art, Größe und Kodierung in fester Laufweite.
 *
 * Sie ist die Auskunft, die das Altsystem nie gibt: Dort steht über dem Feld
 * nichts, und wer eine leere Anzeige sieht, weiß nicht, ob er ein Protokoll ohne
 * freigegebenen Abschnitt vor sich hat oder eine Binärdatei oder einen Ausfall.
 *
 * **Nichts steht hier, was nicht gemessen wäre.** Die Kodierung ist
 * `ISO-8859-1`, weil 8 von 8 Protokollen und 9 von 16 Nutzdateien **kein**
 * gültiges UTF-8 sind (M61) — sie wird nicht zur Laufzeit erraten, und deshalb
 * darf sie dastehen.
 *
 * **Die Größe erscheint nur, wenn es eine gibt.** In den Zuständen ohne Inhalt
 * liefert das Backend `0`, und „0 Bytes" wäre eine Aussage über eine Datei, die
 * gar nicht abgerufen werden konnte. Die Kodierung erscheint nur bei tatsächlich
 * angezeigtem Text: Sie beschreibt, wie *dieser* Text entstanden ist.
 */
function Herkunftszeile({ anzeige }: { anzeige: Artefaktanzeige | undefined }) {
  const texte = useTexte();
  const sprache = useSprache();
  const bausteine = texte.nachrichten.detail.dateien;

  if (anzeige === undefined) {
    return null;
  }

  const angaben = [
    anzeige.name,
    bausteine.art[anzeige.art],
    anzeige.groesseBytes > 0
      ? einsetzen(bausteine.groesse, { bytes: formatiereZahl(anzeige.groesseBytes, sprache) })
      : null,
    anzeige.zustand === "ANZEIGBAR"
      ? einsetzen(bausteine.kodierung, { name: anzeige.kodierung })
      : null,
  ].filter((angabe): angabe is string => angabe !== null);

  return (
    <ul
      className="text-muted-foreground text-beiwerk flex flex-wrap items-center gap-x-3 font-mono"
      data-ziffern
    >
      {/* Der Schlüssel ist die Position: Die Zeile ist eine feste Reihenfolge
          heterogener Angaben und wird nie umsortiert — hier ist der Index die
          Identität und nicht ein Notbehelf. */}
      {angaben.map((angabe, stelle) => (
        <li key={stelle}>{angabe}</li>
      ))}
    </ul>
  );
}

/**
 * **Der Download — und nur dort, wo er etwas liefern kann.**
 *
 * Ob er erscheint, entscheidet `downloadMoeglich` in `../rohdaten.ts` aus dem
 * Zustand der Anzeige. Das ist Entscheidung 9 in der Oberfläche: *Die Oberfläche
 * darf keinen Knopf anbieten, der etwas anderes verspricht als die Anzeige.*
 *
 * **Ein gewöhnlicher Verweis auf den Backend-Endpunkt**, niemals auf den
 * Filestore (`docs/rohdaten.md` §9) und niemals über eine Blob-URL. Der Browser
 * sieht `Content-Disposition: attachment` und legt die Datei ab, ohne die Seite
 * zu verlassen; der Dateiname kommt aus derselben Kopfzeile und wird hier nicht
 * nachgebaut.
 *
 * **Kein `download`-Attribut.** Der Name gehört dem Backend — es baut ihn aus
 * `FileReader.FileProperty.OriginalFilename`, wo es ihn gibt (69,6 %, M17), und
 * bereinigt ihn von Steuerzeichen, Pfadangaben und Unicode-Formatzeichen. Ein
 * zweiter Name hier liefe dem ersten hinterher, und der Browser zöge ihn vor.
 */
function DownloadKnopf({ messageId, artefaktId }: { messageId: string; artefaktId: string }) {
  const texte = useTexte();

  return (
    <Button asChild variant="outline" size="sm" className="min-h-bedienelement shrink-0">
      <a href={downloadPfad(messageId, artefaktId)}>
        <Download data-icon="inline-start" aria-hidden="true" />
        {texte.nachrichten.detail.dateien.herunterladen}
      </a>
    </Button>
  );
}

/**
 * Die Vermerke über der Anzeige — **jeder sagt, dass hier nicht die ganze Datei
 * steht.**
 *
 * Sie stehen zwischen Kopf und Inhalt, weil sie gelesen sein müssen, bevor
 * jemand den Text deutet. Keine Farbe: Es ist keiner ein Fehler, und Rot hat in
 * diesem Farbsystem genau eine Bedeutung (`docs/visuelles-konzept.md` §3).
 */
function Vermerke({ anzeige }: { anzeige: Artefaktanzeige }) {
  const texte = useTexte();
  const sprache = useSprache();
  const bausteine = texte.nachrichten.detail.dateien;

  const vermerke = anzeigevermerke(anzeige);
  if (vermerke.length === 0) {
    return null;
  }

  const text = {
    AUSSCHNITT: bausteine.vermerkAusschnitt,
    GEKAPPT: bausteine.vermerkGekappt,
    MEHRERE_EINTRAEGE: einsetzen(bausteine.vermerkMehrereEintraege, {
      anzahl: formatiereZahl(anzeige.zipEintraege, sprache),
    }),
  };

  return (
    <ul className="flex flex-col gap-1">
      {vermerke.map((vermerk) => (
        <li
          key={vermerk}
          data-vermerk={vermerk}
          className="border-border text-muted-foreground text-beiwerk flex items-start gap-1.5 border-l-2 py-0.5 pl-2"
        >
          <Info aria-hidden="true" className="mt-0.5 size-3.5 shrink-0" />
          <span className="min-w-0">{text[vermerk]}</span>
        </li>
      ))}
    </ul>
  );
}

/**
 * Der Inhalt — **oder einer der benannten Zustände. Niemals ein leeres Feld.**
 *
 * Das ist der Unterschied zum Altsystem, und er ist der ganze Punkt von
 * `docs/rohdaten.md` §8: Dort steht bei jedem dieser Fälle dieselbe leere
 * Fläche, und der Nutzer kann nicht unterscheiden, ob nichts da ist, nichts für
 * ihn da ist oder gerade nichts geht.
 *
 * **„Datei nicht vorhanden" und „Ablage nicht erreichbar" verschmelzen nicht zu
 * einem „Fehler beim Laden".** Für den Betrieb ist genau diese Unterscheidung
 * die wichtigere: Die eine Datei gibt es nicht mehr, die andere ist gerade nicht
 * zu erreichen. Sichtbar wird der Unterschied nicht über Farbe, sondern über das
 * Angebot — nur der zweite Zustand bekommt „Erneut versuchen"
 * (`erneutVersuchenSinnvoll`).
 */
function Inhalt({
  anzeige,
  aufWiederholen,
}: {
  anzeige: Artefaktanzeige;
  aufWiederholen: () => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const bausteine = texte.nachrichten.detail.dateien;

  // **Die Regel steht an einer Stelle und wird an allen angewandt.** Genau ein
  // Zustand lohnt einen zweiten Versuch; die übrigen bekommen `undefined` und
  // damit keinen Knopf. Eine Bedingung im Zweig „Ablage nicht erreichbar" wäre
  // immer wahr und sagte nichts — hier trägt sie die Unterscheidung.
  const wiederholen = erneutVersuchenSinnvoll(anzeige.zustand) ? aufWiederholen : undefined;

  switch (anzeige.zustand) {
    case "ANZEIGBAR":
      // Eine Datei mit null Byte ist nicht gemessen — das kleinste beobachtete
      // Artefakt hat 2 Byte (M60) —, aber möglich. Ohne diesen Zweig stünde
      // dort ein leerer Kasten, und genau den schließt §8 aus. Abgeleitet, keine
      // eigene Entscheidung: dieselbe Bauform wie der fünfte Beschnittfall des
      // Backends.
      return anzeige.text === "" ? (
        <Zustandsfeld
          symbol={ScrollText}
          titel={bausteine.leerTitel}
          text={bausteine.leerText}
          kennung="LEER"
          aufWiederholen={wiederholen}
        />
      ) : (
        <Textfeld text={anzeige.text} />
      );

    case "BINAERDATEI":
      return (
        <Zustandsfeld
          symbol={Binary}
          titel={bausteine.binaerTitel}
          text={einsetzen(bausteine.binaerText, {
            bytes: formatiereZahl(anzeige.groesseBytes, sprache),
          })}
          kennung={anzeige.zustand}
          aufWiederholen={wiederholen}
        />
      );

    case "KEIN_ANZEIGBARER_PROTOKOLLTEIL":
      return (
        <Zustandsfeld
          symbol={ScrollText}
          titel={bausteine.keinProtokollteilTitel}
          text={bausteine.keinProtokollteilText}
          kennung={anzeige.zustand}
          aufWiederholen={wiederholen}
        />
      );

    case "DATEI_NICHT_VORHANDEN":
      return (
        <Zustandsfeld
          symbol={FileX}
          titel={bausteine.nichtVorhandenTitel}
          text={bausteine.nichtVorhandenText}
          kennung={anzeige.zustand}
          aufWiederholen={wiederholen}
        />
      );

    case "ABLAGE_NICHT_ERREICHBAR":
      return (
        <Zustandsfeld
          symbol={CloudOff}
          titel={bausteine.ablageTitel}
          text={bausteine.ablageText}
          kennung={anzeige.zustand}
          aufWiederholen={wiederholen}
        />
      );
  }
}

/**
 * **Der Dateiinhalt — ein Textknoten in einem `<pre>`, und sonst nichts.**
 *
 * ## Umbruch statt waagerechtem Bildlauf
 *
 * Das ist die bewusste Entscheidung für schmale Fenster, und sie ist gemessen
 * begründet: **36 von 206 Artefakten tragen überhaupt kein Zeilenende** (M61) —
 * eine EDIFACT-Übertragung ist regelmäßig *eine* Zeile über die ganze Datei. Mit
 * waagerechtem Bildlauf sähe der Nutzer bei einer 600-KB-Zeile die ersten achtzig
 * Zeichen und danach eine Bildlaufleiste, deren Griff ein Pixel breit ist. Der
 * Umbruch verbirgt nichts; der waagerechte Bildlauf verbirgt fast alles.
 *
 * **Der Preis, ausdrücklich getragen:** Eine Zeile, die im Original bis Spalte
 * 300 lief, belegt hier drei sichtbare Reihen. Der Text bleibt Zeichen für
 * Zeichen derselbe — nur der Zeilenfall der *Darstellung* ist ein anderer. Da es
 * ohnehin keine Zeilennummern gibt (sie erzwängen ein Element je Zeile), geht
 * dabei keine Angabe verloren, auf die sich jemand beziehen könnte.
 *
 * ## Eine Bildlaufleiste je Seite
 *
 * Der einzige senkrechte Scrollbereich bleibt der Inhaltsbereich des Rahmens
 * (`docs/frontend-grundlagen.md` §7). Dieses Feld bekommt **keinen eigenen** —
 * dafür hat die Ansicht ihre eigene Route und damit die ganze Fläche. Und weil
 * umgebrochen statt geschoben wird, entsteht auch waagerecht keine.
 *
 * `break-words` und nicht `break-all`: Gebrochen wird erst, wenn ein Stück sonst
 * überliefe — Segmente bleiben zusammen, solange sie passen.
 */
function Textfeld({ text }: { text: string }) {
  const texte = useTexte();
  const titelId = useId();
  const bausteine = texte.nachrichten.detail.dateien;

  return (
    <section aria-labelledby={titelId}>
      <h2 id={titelId} className="sr-only">
        {bausteine.inhalt}
      </h2>
      <pre
        data-inhalt
        className="border-border bg-muted text-beiwerk rounded-md border p-3 font-mono break-words whitespace-pre-wrap"
      >
        {/* Ein einziges Kind, und es ist ein Textknoten. Kein
            `dangerouslySetInnerHTML`, kein Element je Zeile, keine Zerlegung.
            Beides zugleich: die Sicherheitsregel und die Bauvorgabe aus M60. */}
        {text}
      </pre>
    </section>
  );
}

/**
 * Ein benannter Zustand: Zeichen, Überschrift, ein Satz — und wo er hilft, ein
 * Ausweg.
 *
 * **Gestrichelte Kontur und gedämpfter Ton, wie beim Leerzustand der Liste**
 * (`components/zustand.tsx`). Keine Statusfarbe: Keiner dieser Zustände sagt
 * etwas über die *Nachricht* aus, und Rot hat in diesem Farbsystem genau eine
 * Bedeutung.
 */
function Zustandsfeld({
  symbol: Symbol,
  titel,
  text,
  kennung,
  aufWiederholen,
}: {
  symbol: LucideIcon;
  titel: string;
  text: string;
  kennung: string;
  /**
   * **Nur gesetzt, wo ein zweiter Versuch etwas ändern kann** — der Aufrufer
   * entscheidet das über `erneutVersuchenSinnvoll`. Ein Knopf, der nichts
   * bewirkt, wäre eine Falschauskunft, und ein ausgegrauter wäre eine leisere.
   */
  aufWiederholen?: () => void;
}) {
  const texte = useTexte();

  return (
    <div
      data-zustand={kennung}
      className="border-border bg-card text-muted-foreground flex flex-col items-start gap-2 rounded-md border border-dashed p-4"
    >
      <Symbol aria-hidden="true" className="size-5 opacity-60" />
      <p className="text-foreground font-medium">{titel}</p>
      <p className="text-beiwerk max-w-prose">{text}</p>
      {aufWiederholen === undefined ? null : (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="min-h-beruehrung mt-1"
          onClick={aufWiederholen}
        >
          {texte.zustand.erneutVersuchen}
        </Button>
      )}
    </div>
  );
}

/** Ein Platzhalter in der Gestalt der späteren Anzeige, kein Kreisel über einer leeren Fläche. */
function InhaltSkelett() {
  const texte = useTexte();
  return (
    <div className="flex flex-col gap-2" aria-busy="true">
      <span className="sr-only">{texte.zustand.laedt}</span>
      {[0, 1, 2, 3, 4, 5].map((nummer) => (
        <Skeleton key={nummer} className="h-4 w-full" />
      ))}
    </div>
  );
}

/**
 * Der Fehlerzustand des Abrufs.
 *
 * **Nicht zu verwechseln mit den vier Zuständen aus §8** — die kommen mit `200`
 * und stehen im Inhalt. Hier landet, was schon vor dem Abruf schiefging: eine
 * unbekannte Nachricht, eine fremde, eine unbrauchbare Kennung. Alle drei
 * beantwortet das Backend absichtlich gleich, und der Text sagt deshalb nichts
 * über Berechtigungen (`docs/frontend-grundlagen.md` §6).
 */
function InhaltFehler({ fehler, aufWiederholen }: { fehler: unknown; aufWiederholen: () => void }) {
  const texte = useTexte();

  if (fehler instanceof ProblemFehler && fehler.status === 404) {
    return (
      <div className="border-border text-muted-foreground max-w-prose rounded-md border border-dashed p-3">
        <p className="text-foreground font-medium">{texte.fehler["nicht-gefunden"]}</p>
        <p className="text-beiwerk mt-1">{texte.nachrichten.detail.nichtGefunden}</p>
      </div>
    );
  }

  return <Fehler fehler={fehler} aufWiederholen={aufWiederholen} />;
}
