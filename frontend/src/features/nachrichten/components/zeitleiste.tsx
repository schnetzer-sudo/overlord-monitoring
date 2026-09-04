"use client";

import { Clock, PlayCircle } from "lucide-react";

import { useAnzeigezone } from "@/components/zeitzone";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereDauer, formatiereZeitpunktGenau } from "@/lib/format";
import { cn } from "@/lib/utils";

import type { Nachrichtendetail, Schritt } from "../api";
import { schrittHinweis, wartezeile, zeitleiste, type Zeitleistenzeile } from "../detail";
import type { Artefaktziel } from "../rohdaten";
import { Ziele } from "./artefakt-ziele";

/**
 * Die Zeitleiste — der Kern der Detailansicht.
 *
 * **Senkrecht, ein Schritt je Zeile**, weil das Panel schmal ist. Je Schritt der
 * Name, ein schmaler Balken und die Dauer als Text.
 *
 * Warum der Balken auf **diese** Nachricht normiert ist und ab wann eine Lücke
 * eine eigene Zeile bekommt, steht in `../detail.ts` — dort, wo es gerechnet und
 * geprüft wird. Diese Datei stellt nur dar.
 *
 * **Feste Zeilenhöhe je Schritt**, nach der Regel aus `nachrichtenliste.md` §8.1:
 * Was nicht hineinpasst, wird gekürzt, der Vollwert steht im `title`. Die
 * gemessene Namenslänge geht bis 61 Zeichen — in einem Panel von 26 rem passt
 * das nicht immer, und eine Leiste mit springenden Zeilenhöhen lässt sich nicht
 * überfliegen.
 *
 * ## Was am 18.08.2026 dazugekommen ist — und was nicht
 *
 * Zwei Dinge, beide **an** der Zeile und keines *in* der Rechnung:
 *
 * 1. **Die Ziele.** Je Schritt hängen die Artefakte daran, die auf ihm liegen —
 *    in aller Regel zwei, Datei und Protokoll. Wo nichts liegt, hängt nichts.
 * 2. **Der Weg zu den technischen Eigenschaften.** Der Name wird zur
 *    Schaltfläche und führt an die Gruppe desselben Schritts im Block darunter.
 *
 * **An der Zeitleiste selbst ändert das nichts:** keine andere Sortierung, keine
 * zweite Datenquelle für die Zeilen, keine neue Zeile. `zeitleiste(detail)`
 * rechnet unverändert (`../detail.ts`), und `schritte[]` bleibt die einzige
 * Quelle der Zeilen — der Metadaten-Schritt steht auch jetzt in keiner
 * (`docs/nachrichtendetail.md` §4). Was auf ihm liegt, steht **über** der Leiste
 * (`artefakt-ziele.tsx` `Zielzeile`).
 *
 * @param ziele die Artefakte je `position`, aus `../rohdaten.ts`
 *   `zieleJeSchritt`. Fehlt die Liste — sie lädt noch oder ihre Abfrage ist
 *   fehlgeschlagen —, ist die Abbildung leer und keine Zeile trägt ein Ziel.
 *   Die Leiste hängt nicht daran.
 * @param aufSchritt der Weg zur Eigenschaftengruppe dieses Schritts. **Ohne ihn
 *   bleibt der Name ein Text** und keine Schaltfläche: Der Aufrufer reicht ihn
 *   nur durch, wenn es unter der Leiste überhaupt einen Block gibt
 *   (`eigenschaftenAnzahl > 0`).
 */
export function Zeitleiste({
  detail,
  ziele,
  aufSchritt,
}: {
  detail: Nachrichtendetail;
  ziele?: Map<number, Artefaktziel[]>;
  aufSchritt?: (position: number) => void;
}) {
  const zeilen = zeitleiste(detail);
  const warten = wartezeile(detail);

  if (zeilen.length === 0) {
    return (
      <div className="flex flex-col gap-2">
        <LeereLeiste detail={detail} />
        {warten ? <WarteZeile warten={warten} /> : null}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <ol className="flex flex-col">
        {zeilen.map((zeile) => (
          <Zeile
            key={zeile.id}
            zeile={zeile}
            messageId={detail.messageId}
            ziele={ziele}
            aufSchritt={aufSchritt}
          />
        ))}
      </ol>
      {warten ? <WarteZeile warten={warten} /> : null}
    </div>
  );
}

/**
 * Der Satz über der leeren Leiste. **Er unterscheidet drei Lagen, die alle drei
 * dieselbe leere Zeilenliste ergeben** — was der Nutzer liest, entscheidet der
 * gelieferte Zustand und nicht die Länge der Liste.
 *
 * | Zustand | Satz |
 * |---|---|
 * | `EMPFANGEN` | *Empfangen am … — seitdem ist kein Schritt ausgeführt worden.* |
 * | `OHNE_AKTION` | *Zu dieser Nachricht ist kein Ablauf protokolliert.* |
 * | `KEINER` | *Für diese Nachricht ist kein Prozessschritt aufgezeichnet.* |
 *
 * Bis zum 10.08.2026 trugen die ersten beiden **einen** Zustand (`OHNE_SCHRITT`)
 * und damit einen Satz. Der eine ist eine Auskunft über die **Plattform** — die
 * Nachricht ist angekommen und hängt seitdem —, der andere über die
 * **Datenlage**. Ein gemeinsamer Text müsste so vage sein, dass er beides
 * abdeckt, und wäre dann für keinen der beiden brauchbar.
 *
 * **Der Zeitpunkt ist der fachliche Start**, und bei `EMPFANGEN` ist das genau
 * der Metadaten-Schritt: die einzige Aktion, die es dort gibt. Formatiert wird
 * er mit derselben Zone und derselben Funktion wie im Kopf. Fehlt er — die
 * Spalte lässt `NULL` zu, gemessen ist er auf keiner der 10,3 Millionen Zeilen
 * leer (M22) —, steht der Satz ohne Datum da statt mit einem Platzhalter.
 */
function LeereLeiste({ detail }: { detail: Nachrichtendetail }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  if (detail.offenerZustand === "OHNE_AKTION") {
    return (
      <p className="text-muted-foreground text-beiwerk">{texte.nachrichten.detail.ohneAktion}</p>
    );
  }

  if (detail.offenerZustand === "EMPFANGEN") {
    return (
      <p className="text-muted-foreground text-beiwerk">
        {detail.start === null
          ? texte.nachrichten.detail.empfangenOhneZeitpunkt
          : einsetzen(texte.nachrichten.detail.empfangen, {
              zeitpunkt: formatiereZeitpunktGenau(detail.start, sprache, zone),
            })}
      </p>
    );
  }

  return (
    <p className="text-muted-foreground text-beiwerk">{texte.nachrichten.detail.keineSchritte}</p>
  );
}

function Zeile({
  zeile,
  messageId,
  ziele,
  aufSchritt,
}: {
  zeile: Zeitleistenzeile;
  messageId: string;
  ziele?: Map<number, Artefaktziel[]>;
  aufSchritt?: (position: number) => void;
}) {
  if (zeile.art === "erwartet") {
    return <ErwarteteZeile name={zeile.name} bereitsGelaufen={zeile.bereitsGelaufen} />;
  }
  return (
    <SchrittZeile
      schritt={zeile.schritt}
      anteil={zeile.anteil}
      messageId={messageId}
      // Kein Eimer heißt keine Ziele — und das ist der Regelfall: Solange die
      // Artefaktliste lädt, ist die Abbildung leer, und ein Schritt ohne
      // Artefakte kommt darin gar nicht vor.
      ziele={ziele?.get(zeile.schritt.position) ?? []}
      aufSchritt={aufSchritt}
    />
  );
}

/**
 * Ein ausgeführter Schritt.
 *
 * **Die Herkunft des Namens steht im Tooltip**, dezent und zusammen mit dem
 * Rohwert — kein Symbol, kein Warnzeichen, keine eigene Spalte. Ein Nutzer, der
 * „Send File by FTP" liest, soll nicht mit der Frage belastet werden, wie wir
 * darauf gekommen sind; wer nachsehen will, findet es. Bei `ROHWERT` steht
 * ohnehin der Rohwert als Name — auch dort gehört die Herkunft in den Tooltip,
 * damit die Erklärung an **einer** Stelle liegt.
 *
 * **Zusammengesetzt wird er seit dem 17.08.2026 in `../detail.ts`** und nicht
 * mehr hier: Der Eigenschaftenblock gruppiert seine Werte nach Schritten und
 * beschriftet jede Gruppe mit demselben Tooltip. Zwei Stellen, die denselben
 * Schritt verschieden benennen, wären genau der Fehler, den jene Gruppierung
 * beseitigen soll.
 *
 * ## Der Name führt seit dem 18.08.2026 an die Eigenschaften dieses Schritts
 *
 * Die Gruppierung des Eigenschaftenblocks besteht seit dem 17.08.2026; es fehlte
 * nur der Weg dorthin. **Kein neuer Block, keine Duplizierung** — ein Klick auf
 * den Schritt klappt den Block auf und setzt den Fokus auf seine Gruppe.
 *
 * Das schließt zugleich den offenen Punkt, dass zwei Bausteine dieselbe Sache
 * nach verschiedenen Spalten ordneten: Es gibt jetzt **eine** Ordnung, und das
 * ist die dieser Leiste.
 */
function SchrittZeile({
  schritt,
  anteil,
  messageId,
  ziele,
  aufSchritt,
}: {
  schritt: Schritt;
  anteil: number | null;
  messageId: string;
  ziele: Artefaktziel[];
  aufSchritt?: (position: number) => void;
}) {
  const texte = useTexte();
  const dauer =
    schritt.dauerSekunden === null
      ? texte.nachrichten.detail.ohneDauer
      : formatiereDauer(schritt.dauerSekunden, texte.nachrichten.detail.dauer);

  const hinweis = schrittHinweis(schritt, texte);

  return (
    <li
      className={cn(
        "h-zeile flex items-center gap-2 border-l-2 pl-2",
        // Der laufende Schritt trägt die Farbrolle „offen" — dieselbe, die die
        // Statusplakette für „wartend, laufend" hat. Der Akzent der Anwendung
        // wäre hier falsch: Er sagt etwas über die Anwendung, nie über die
        // Daten (`visuelles-konzept.md` §3).
        schritt.laeuftAuf ? "border-status-offen-kontur" : "border-border",
      )}
    >
      {/* Ohne Namen keine Schaltfläche: Ein Bedienelement ohne sichtbare
          Beschriftung wäre für jeden, der es nicht ohnehin kennt, eine leere
          Fläche — und sein zugänglicher Name hieße „Technische Eigenschaften zu
          … anzeigen" mit einer Lücke darin. */}
      {aufSchritt === undefined || schritt.name === "" ? (
        <span className="min-w-0 flex-1 truncate" title={hinweis}>
          {schritt.name}
        </span>
      ) : (
        // Der sichtbare Name steht im zugänglichen Namen (WCAG 2.5.3): Wer
        // „Datei gelesen" sagt, muss die Schaltfläche damit erreichen. Der
        // Tooltip bleibt die Herkunft — er beantwortet eine andere Frage.
        <button
          type="button"
          onClick={() => aufSchritt(schritt.position)}
          title={hinweis}
          aria-label={einsetzen(texte.nachrichten.detail.dateien.zuEigenschaften, {
            name: schritt.name,
          })}
          // `self-stretch`: **Die Schaltfläche ist so hoch wie ihre Zeile.**
          // Ohne sie wäre ihre Trefferfläche die Zeilenhöhe der Schrift und
          // damit ein schmales Band in der Mitte einer 2.25-rem-Zeile — am
          // Finger nicht zu treffen, und der Rest der Zeile täte nichts. Die
          // Zeile selbst bleibt `h-zeile`; gedehnt wird nur dieses Kind.
          className="hover:bg-muted focus-visible:ring-ring -mx-1 flex min-w-0 flex-1 items-center self-stretch rounded-md px-1 text-left focus-visible:ring-2 focus-visible:outline-none"
        >
          {/* Das Kürzen gehört auf dieses `span` und nicht auf die
              Schaltfläche: Auf einem Flex-Behälter greift `text-overflow`
              nicht, der Name bräche dann ab statt mit Auslassungspunkten zu
              enden. */}
          <span className="min-w-0 truncate">{schritt.name}</span>
        </button>
      )}

      <Ziele messageId={messageId} ziele={ziele} />

      {schritt.laeuftAuf ? (
        // Nie allein über Farbe: Zeichen **und** Text.
        <span className="text-status-offen text-beiwerk flex shrink-0 items-center gap-1">
          <PlayCircle aria-hidden="true" className="size-3.5" />
          {texte.nachrichten.detail.laeuftGerade}
        </span>
      ) : (
        <Balken anteil={anteil} />
      )}

      <span className="text-muted-foreground text-beiwerk w-20 shrink-0 text-right" data-ziffern>
        {schritt.laeuftAuf && schritt.dauerSekunden === null ? "" : dauer}
      </span>
    </li>
  );
}

/**
 * Der Balken. **Ohne Zahl wäre er ein Gefühl** — die Dauer steht immer daneben,
 * und diese Fläche ist nur die Beziehung der Schritte untereinander.
 *
 * Fehlt die Dauer, fehlt der Balken: Eine Fläche der Breite null sähe aus wie
 * „praktisch nichts", und das ist etwas anderes als „nicht aufgezeichnet".
 */
function Balken({ anteil }: { anteil: number | null }) {
  if (anteil === null) {
    return <span className="w-20 shrink-0 sm:w-28" />;
  }
  return (
    <span
      aria-hidden="true"
      className="bg-muted block h-1.5 w-20 shrink-0 overflow-hidden rounded-full sm:w-28"
    >
      <span
        className="bg-muted-foreground block h-full rounded-full"
        style={{ width: `${(anteil * 100).toFixed(2)}%` }}
      />
    </span>
  );
}

/**
 * *wartet seit 4 h 12 min · Frist 30 min* — die Zeile am offenen Zustand.
 *
 * ## Sie ist der Ersatz für die Lückenzeile, und zwar aus einem gemessenen Grund
 *
 * Die Lückenzeile stand *zwischen* zwei Schritten und ist über rund 700 geprüfte
 * Nachrichten nie erschienen: Die größte Lücke beträgt eine Sekunde. **Die
 * Wartezeit steckt in der Dauer des `WAITUNTIL`-Schritts**, nicht im Zwischenraum
 * — und die eigentliche Frage eines Nutzers vor einer hängenden Nachricht lautet
 * ohnehin nicht „wie lange lag sie zwischen zwei Schritten", sondern „wie lange
 * steht sie schon".
 *
 * ## Beide Zahlen sind gerechnet, bevor sie hier ankommen
 *
 * `wartetSeitSekunden` entsteht im Backend gegen die **Anwendungsuhr**. Im Profil
 * `dev` steht die Monate zurück; `Date.now()` gegen einen gelieferten Zeitstempel
 * ergäbe dort „vor 7 Monaten" statt „vor 4 Stunden".
 *
 * ## ~~Überfällig wird hervorgehoben~~
 *
 * ⚠️ **Die Hervorhebung ist am 03.09.2026 entfallen** *(E‑71)*. Hier stand ein
 * dritter Zweig: Bei `ueberfaellig` wurde die Zeile achromatisch hervorgehoben —
 * über Zeichen, Wort und Schriftstärke, ausdrücklich ohne Farbe, weil Rot allein
 * der Kategorie *Fehler* gehört. **Die Problemkategorie ist widerlegt**, das Feld
 * gibt es nicht mehr, und ein Zweig, dessen Bedingung nie zutrifft, ist eine
 * Behauptung über einen Zustand, den es nicht gibt.
 *
 * **Was von dem Zweig bleibt, steht anderswo:** Der Grundsatz — Rot ist der
 * Kategorie *Fehler* vorbehalten, eine Hervorhebung kommt ohne Farbe aus — gilt
 * unverändert (`docs/visuelles-konzept.md` §3). Die Farbrolle `--ueberfaellig`
 * bleibt ohne Verbraucher bestehen (E‑77).
 *
 * ## Die Frist erscheint jetzt nur noch, wo sie durchgesetzt wird (E‑76)
 *
 * **Bei `LAEUFT` steht beides** — *läuft seit X* und *Frist Y* —, und zusammen
 * sagen sie, wann die Nachricht in `ERROR_TIMEOUT` kippt. **Bei `WARTEND` liefert
 * das Backend `fristSekunden = null`**, und dann steht nur *wartet seit X*. Diese
 * Komponente prüft das nicht nach: Sie zeigt die Hälften, die da sind.
 */
function WarteZeile({ warten }: { warten: NonNullable<ReturnType<typeof wartezeile>> }) {
  const texte = useTexte();
  const dauer = formatiereDauer(warten.sekunden, texte.nachrichten.detail.dauer);
  const satz = einsetzen(
    warten.laeuft ? texte.nachrichten.detail.laeuftSeit : texte.nachrichten.detail.wartetSeit,
    { dauer },
  );

  return (
    <p className="text-beiwerk text-muted-foreground flex flex-wrap items-center gap-x-2 gap-y-1">
      <span className="flex items-center gap-1.5">
        <Clock aria-hidden="true" className="size-3.5 shrink-0" />
        {satz}
      </span>

      {warten.fristSekunden === null ? null : (
        <span data-ziffern>
          {einsetzen(texte.nachrichten.detail.frist, {
            dauer: formatiereDauer(warten.fristSekunden, texte.nachrichten.detail.dauer),
          })}
        </span>
      )}
    </p>
  );
}

/**
 * Das Ende der Leiste bei `WARTET_IN` und `WARTET_VOR`. **Die Nachricht wartet —
 * und das steht da**, erkennbar anders als die ausgeführten Schritte.
 *
 * Drei Fälle, und alle drei kommen aus **gelieferten Feldern**; hier wird nichts
 * verglichen und nichts abgeleitet:
 *
 * 1. **Kein benannter Schritt** (`naechsterSchritt === null`, über den
 *    Gesamtbestand 43,9 Prozent der Verweise): Das wird benannt und nicht
 *    weggelassen. Die Nachricht wartet, wir wissen nur nicht worauf; sie
 *    stillschweigend wie eine abgeschlossene aussehen zu lassen wäre die
 *    schlechtere Auskunft.
 * 2. **`WARTET_IN`** — der gemessene Normalfall, 538 von 538 (M29). Der Verweis
 *    zeigt auf den Schritt, der sie schlafen gelegt hat. Sein Name wird **nicht
 *    wiederholt**: Er steht eine Zeile darüber, mit seiner Dauer. Die Zeile sagt
 *    nur noch, dass es hier nicht von selbst weitergeht; der Verweis bleibt im
 *    Tooltip nachlesbar.
 * 3. **`WARTET_VOR`** — der Verweis zeigt auf einen anderen Schritt, und der
 *    steht als noch nicht begonnen da. In der Testkopie null Mal beobachtet.
 */
function ErwarteteZeile({
  name,
  bereitsGelaufen,
}: {
  name: string | null;
  bereitsGelaufen: boolean;
}) {
  const texte = useTexte();

  if (name === null) {
    return (
      <li className="border-border text-muted-foreground text-beiwerk flex items-center gap-1.5 border-l-2 border-dashed py-1 pl-2">
        <Clock aria-hidden="true" className="size-3.5 shrink-0" />
        {texte.nachrichten.detail.wartetVorUnbekannt}
      </li>
    );
  }

  if (bereitsGelaufen) {
    return (
      <li
        className="border-border text-muted-foreground text-beiwerk flex items-center gap-1.5 border-l-2 border-dashed py-1 pl-2"
        title={einsetzen(texte.nachrichten.detail.verweistAuf, { schritt: name })}
      >
        <Clock aria-hidden="true" className="size-3.5 shrink-0" />
        {texte.nachrichten.detail.wartetWeiterhin}
      </li>
    );
  }

  return (
    <li className="border-border h-zeile text-muted-foreground flex items-center gap-2 border-l-2 border-dashed pl-2">
      <Clock aria-hidden="true" className="size-3.5 shrink-0" />
      <span className="min-w-0 flex-1 truncate italic" title={name}>
        {name}
      </span>
      <span className="text-beiwerk shrink-0">{texte.nachrichten.detail.nochNichtBegonnen}</span>
    </li>
  );
}
