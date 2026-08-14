"use client";

import { useCallback, useId, useState } from "react";

import { Marke } from "@/components/marke";
import { useAnzeigezone } from "@/components/zeitzone";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import {
  formatiereZahl,
  formatiereZeitpunkt,
  wanduhrzeitFuerEingabe,
  zeitpunktAusWanduhrzeit,
} from "@/lib/format";
import { ProblemFehler, istPraefixfensterZuGross } from "@/lib/http";

import type { BamSuchergebnis } from "../api";
import { useBamSuche, useBamTypen, useEscapeSchliesst, useSuchzustand } from "../hooks";
import {
  HOECHSTENS_BEGRIFFE,
  PRAEFIX_FENSTER_TAGE,
  abweichendeVarianten,
  alsAbfrage,
  fensterZurueck,
  jahresfensterAb,
  modusAusAntwort,
  nulltrefferHinweis,
  praefixfenster,
  spanneInTagen,
  zeigtPraefixAngebot,
  type Suchmodus,
  type VorigeRunde,
} from "../suche";
import { MarkenLeiste } from "./marken-leiste";
import { NachrichtDetail } from "./nachricht-detail";
import { TrefferTabelle } from "./treffer-tabelle";

/** Der Problemtyp, den die Suche mit der Nachrichtenliste teilt — es entsteht kein zweiter. */
const ABGEBROCHEN = "suche-abgebrochen";

/**
 * **Die Belegsuche — „wo ist mein Lieferschein?"**
 *
 * Der Einstiegspunkt des MVP, und die eine Ansicht, die den Leitsatz unmittelbar
 * beantwortet: Der typische Nutzer ist kein EDI-Spezialist, er hat eine Nummer
 * und will wissen, wo der Beleg steht.
 *
 * ## Eine eigene Route, und nicht die Liste mit anderen Parametern
 *
 * Verlockend wäre `/nachrichten` gewesen — das Ergebnis *ist* eine
 * Nachrichtenliste. Dagegen stehen zwei Dinge: Die Liste hat ein
 * Pflicht-Zeitfenster als Wesenszug (Vorgabe 24 Stunden), die Suche eines von 30
 * Tagen (`docs/bam-suche.md` §2) — die Suche dorthin zu legen hieße, die
 * Fensterentscheidung stillschweigend gegen den Nutzer zu treffen, der eine
 * Nummer hat und kein Datum. Und es stünden zwei Suchparameter in derselben URL,
 * die verschiedene Dinge tun.
 *
 * **Kein Navigationseintrag.** Das Feld steht in der Kopfzeile und damit auf
 * jeder Seite; ein Menüpunkt daneben wäre eine zweite Tür in denselben Raum.
 * Genau deshalb muss diese Seite ohne Begriffe eine tragfähige Seite sein — wer
 * das Feld leert, hat sonst keine Wegweisung mehr.
 *
 * ## Der Zustand steht vollständig in der URL
 *
 * Begriffe, Zeitfenster und die geöffnete Nachricht; eine Suche ist damit
 * teilbar. Was die URL nicht ausdrücken kann — die begonnene Eingabe im Feld, die
 * gewählte Belegart, der kurze Hinweis auf eine doppelte Marke —, ist kein
 * Filterzustand und liegt im Komponentenzustand
 * (`docs/frontend-grundlagen.md` §8).
 *
 * ## Das Detail-Panel ist dasselbe wie neben der Liste
 *
 * Nicht nachgebaut: Es lädt über seine **eigene** Kennung und hängt nicht am
 * Ergebnis der Suche (`docs/nachrichtendetail.md` §10). Was es zeigt, ist
 * dieselbe Ansicht mit demselben Belegdaten-Block — dessen Marken hier so wenig
 * anklickbar sind wie dort.
 *
 * **Ohne Umschalter auf die eigene Route.** Er führt von dort zurück an die
 * *Liste* und nicht an die Suche (`lib/routen.ts`); ein Umschalter, dessen
 * Rückweg woanders endet, ist keiner.
 */
export function SucheAnsicht() {
  const texte = useTexte();
  const { zustand, begriffe, modus, setzeBegriffe, setzeFenster, setzeModus, setzeNachricht } =
    useSuchzustand();
  const abfrage = alsAbfrage(zustand);
  const anfrage = useBamSuche(abfrage, begriffe.length > 0);
  const ergebnis = anfrage.data;

  const gewaehlt = zustand.nachricht;
  const schliesse = useCallback(() => setzeNachricht(null), [setzeNachricht]);
  useEscapeSchliesst(gewaehlt !== null, schliesse);

  /*
   * Die vorige Trefferzahl — für die Nulltreffer-Zeile.
   *
   * Sie kostet **keine** zusätzliche Abfrage: Vor jedem `+` wurde schon gesucht,
   * die Zahl stand also ohnehin auf dem Schirm. Gehalten wird sie samt der
   * Abfrage, zu der sie gehört; ohne diesen Schlüssel verglichen die Folgerender
   * die Runde mit sich selbst.
   *
   * Angepasst **während des Renderns** und nicht in einem Effekt — dasselbe
   * Muster wie bei `letzteSeite` in `hooks.ts`: React verwirft den begonnenen
   * Durchlauf und rendert sofort neu, der Zwischenstand erscheint nie auf dem
   * Bildschirm.
   */
  const [stand, setStand] = useState<{
    fuer: string;
    vorige: VorigeRunde | null;
    hinweis: VorigeRunde | null;
  }>({ fuer: "", vorige: null, hinweis: null });

  if (ergebnis !== undefined && stand.fuer !== abfrage) {
    const runde: VorigeRunde = {
      begriffe: ergebnis.begriffe.length,
      treffer: ergebnis.nachrichten.length,
      abgeschnitten: ergebnis.abgeschnitten,
    };
    setStand({
      fuer: abfrage,
      vorige: runde,
      hinweis: nulltrefferHinweis(stand.vorige, runde.begriffe, runde.treffer),
    });
  }

  const abgebrochen =
    anfrage.error instanceof ProblemFehler && anfrage.error.typ === ABGEBROCHEN
      ? anfrage.error
      : undefined;

  /*
   * **Die Oberfläche löst diesen Fehler nie selbst aus** — der Knopf verkleinert
   * das Fenster vorher (`praefixfenster`), und „Auf ein Jahr erweitern" gibt es
   * im Präfixmodus nicht. Er kommt aus einer von Hand gebauten URL oder aus von
   * Hand eingetragenen Zeitpunkten, und dafür gibt es einen Fehlerzustand mit
   * Ausweg statt einer leeren Seite (`docs/bam-suche.md` §23).
   */
  const fensterZuGross =
    anfrage.error instanceof ProblemFehler && istPraefixfensterZuGross(anfrage.error)
      ? anfrage.error
      : undefined;

  /*
   * **Das Angebot hängt an vier Bedingungen, und sie stehen als reine Funktion
   * daneben** (`suche.ts` `zeigtPraefixAngebot`) — nicht als Kette von `&&` in
   * diesem Baum. Die vierte, „nicht abgebrochen", ist die, die am leichtesten
   * durchrutscht: Sie ergäbe sich heute schon aus der Reihenfolge der Zweige
   * unten, aber das ist eine Eigenschaft des Markups und keine Zusage.
   *
   * **Gefragt wird der Modus der Antwort und nicht der der URL.** Sie fallen
   * auseinander, solange geladen wird — und angeboten wird nur, was
   * nachweislich noch nicht gelaufen ist.
   */
  const angebot = zeigtPraefixAngebot({
    modus: modusAusAntwort(ergebnis?.modus),
    treffer: ergebnis?.nachrichten.length ?? 0,
    begriffe: begriffe.length,
    abgebrochen: abgebrochen !== undefined,
  });

  /*
   * Der Knopf verkleinert das Fenster **selbst**, wenn es über dem Deckel liegt
   * — und er rechnet dabei vom `bis` **aus der Antwort** zurück, nicht von der
   * Browseruhr (Regel Z1). Liegt das Fenster schon darunter, ändert
   * `praefixfenster` nichts und gibt `null`; dann wird auch kein Zeitpunkt in
   * die URL geschrieben.
   */
  const aufPraefix = useCallback(() => {
    if (ergebnis === undefined) {
      return;
    }
    setzeModus(
      "praefix",
      praefixfenster({ von: new Date(ergebnis.von), bis: new Date(ergebnis.bis) }),
    );
  }, [ergebnis, setzeModus]);

  /** Der Rückweg. **Das Fenster bleibt stehen** — siehe `hooks.ts` `setzeModus`. */
  const aufExakt = useCallback(() => setzeModus(null, null), [setzeModus]);

  return (
    /*
     * Ab `xl` steht das Panel **neben** der Trefferliste, darunter an ihrer
     * Stelle — dieselbe Aufteilung wie in der Nachrichtenliste. Beides sitzt im
     * **einen** Scrollbereich des Anwendungsrahmens; es entsteht keine zweite
     * Bildlaufleiste (`docs/frontend-grundlagen.md` §7).
     */
    <div className="flex flex-col gap-4 xl:flex-row xl:items-start">
      <div
        className={
          gewaehlt !== null
            ? "hidden min-w-0 flex-1 flex-col gap-4 xl:flex"
            : "flex min-w-0 flex-1 flex-col gap-4"
        }
      >
        <h1 className="text-ueberschrift font-semibold">{texte.suche.titel}</h1>

        {begriffe.length === 0 ? (
          <Leerzustand />
        ) : (
          <>
            <MarkenLeiste begriffe={begriffe} aufBegriffe={setzeBegriffe} />

            {/* Marken und Zeitfenster bleiben in **jedem** Zustand stehen — sie
                sind der Weg aus einem leeren Ergebnis heraus. Sie mit den Daten
                zu verstecken hieße, dem Nutzer das Werkzeug wegzunehmen, wenn er
                es braucht (dieselbe Regel wie bei der Filterleiste der Liste). */}
            <Kopfzeilen
              ergebnis={ergebnis}
              modus={modus}
              aufExakt={aufExakt}
              von={zustand.von}
              bis={zustand.bis}
              aufFenster={setzeFenster}
              nulltrefferVorher={stand.hinweis}
              voll={begriffe.length >= HOECHSTENS_BEGRIFFE}
            />

            {abgebrochen !== undefined ? (
              /* Der Abbruch an der Zeitgrenze ist ein absehbarer Fall und kein
                 Systemfehler. **Kein „Erneut versuchen"**: Dieselbe Abfrage liefe
                 in dieselbe Grenze. Was hilft, steht im Satz — und die beiden
                 Handlungen stehen unmittelbar darüber.

                 **Und hier steht ausdrücklich kein Angebot.** Wer gerade an der
                 Zeitgrenze gescheitert ist, bekommt keine teurere Suche
                 angeboten (`docs/bam-suche.md` §23). */
              <p className="text-muted-foreground text-beiwerk max-w-prose" role="status">
                {texte.suche.abgebrochen}
              </p>
            ) : fensterZuGross !== undefined ? (
              <FensterZuGross fehler={fensterZuGross} bis={zustand.bis} aufFenster={setzeFenster} />
            ) : anfrage.error ? (
              <Fehler fehler={anfrage.error} aufWiederholen={() => void anfrage.refetch()} />
            ) : anfrage.isPending ? (
              <Laden zeilen={8} />
            ) : (ergebnis?.nachrichten.length ?? 0) === 0 ? (
              <Leer titel={texte.suche.ergebnis.keine} hinweis={texte.suche.ergebnis.keineHinweis}>
                {angebot ? <PraefixAngebot aufPraefix={aufPraefix} /> : null}
              </Leer>
            ) : (
              <div className="border-border bg-card overflow-x-auto rounded-lg border">
                <TrefferTabelle
                  zeilen={ergebnis?.nachrichten ?? []}
                  gewaehlt={gewaehlt}
                  aufAuswahl={setzeNachricht}
                />
              </div>
            )}
          </>
        )}
      </div>

      {gewaehlt === null ? null : (
        <div className="min-w-0 xl:w-[26rem] xl:shrink-0 2xl:w-[30rem]">
          <NachrichtDetail
            // Ein Wechsel der Nachricht ist eine neue Ansicht und kein neuer
            // Zustand derselben: Der Kopierknopf und die Blöcke beginnen von vorn.
            key={gewaehlt}
            messageId={gewaehlt}
            aufSchliessen={schliesse}
            schliessenText={texte.nachrichten.detail.schliessen}
            // Ein Glied der Kette öffnet sich über denselben Parameter wie eine
            // Trefferzeile — kein neuer Mechanismus, und die Ansicht bleibt
            // teilbar.
            aufOeffnen={setzeNachricht}
          />
        </div>
      )}
    </div>
  );
}

/**
 * Was über der Liste steht — **vier Angaben, und jede hat einen gemessenen
 * Grund.**
 *
 * Seit Teil 4 gehört der Präfixhinweis dazu, und er steht **hier und nur hier**:
 * Der Modus gilt für die **ganze** Suche und nicht je Begriff — an jede Marke
 * geschrieben behauptete er das Gegenteil (`docs/bam-suche.md` §23). Aus
 * demselben Grund steht der Zeitraumhinweis in derselben Zeile und nicht an
 * einer zweiten Stelle.
 */
function Kopfzeilen({
  ergebnis,
  modus,
  aufExakt,
  von,
  bis,
  aufFenster,
  nulltrefferVorher,
  voll,
}: {
  ergebnis: BamSuchergebnis | undefined;
  modus: Suchmodus;
  aufExakt: () => void;
  von: Date | null;
  bis: Date | null;
  aufFenster: (von: Date | null, bis: Date | null) => void;
  nulltrefferVorher: VorigeRunde | null;
  voll: boolean;
}) {
  const texte = useTexte();
  const sprache = useSprache();

  return (
    <div className="flex flex-col gap-1.5">
      {modus === "praefix" ? <Praefixzeile ergebnis={ergebnis} aufExakt={aufExakt} /> : null}
      <Trefferzeile ergebnis={ergebnis} />
      <Zeitfensterzeile
        ergebnis={ergebnis}
        modus={modus}
        von={von}
        bis={bis}
        aufFenster={aufFenster}
      />
      <Variantenzeile ergebnis={ergebnis} />

      {nulltrefferVorher === null ? null : (
        /*
         * **Die Nulltreffer-Falle, und sie ist umsonst zu vermeiden.** Jede Marke
         * verengt. Landet die dritte bei null, sieht der Nutzer nicht, welche es
         * war — und mit einer nicht mitgetippten führenden Null passiert genau
         * das. Die vorige Trefferzahl stand ohnehin schon auf dem Schirm.
         *
         * **War die vorige Runde abgeschnitten, sagt die Zeile „mehr als".** Die
         * gelieferte Zahl ist dort die Seitengröße und nicht die Trefferzahl; sie
         * als solche auszugeben wäre ein falscher Schluss in genau der Zeile, die
         * vor einem falschen Schluss bewahren soll.
         */
        <p className="text-muted-foreground text-beiwerk max-w-prose" role="status">
          {einsetzen(
            nulltrefferVorher.abgeschnitten
              ? texte.suche.ergebnis.nulltrefferAbgeschnitten
              : texte.suche.ergebnis.nulltreffer,
            { anzahl: formatiereZahl(nulltrefferVorher.treffer, sprache) },
          )}
        </p>
      )}

      {voll ? (
        <p className="text-muted-foreground text-beiwerk max-w-prose">
          {einsetzen(texte.suche.marken.grenzeErreicht, { anzahl: HOECHSTENS_BEGRIFFE })}
        </p>
      ) : null}
    </div>
  );
}

/**
 * Die Trefferzahl — **und die Abschneidung immer zusammen mit dem Fenster.**
 *
 * Greift das harte Limit von 50, muss die Meldung **beides** sagen: dass
 * abgeschnitten wurde und dass ein Fenster galt. Nur eines von beidem ist
 * irreführend — „mehr als 50" ohne Fenster liest sich wie eine Aussage über den
 * ganzen Bestand, und die wäre falsch.
 *
 * ## Im Präfixmodus rät sie zu **mehr Zeichen**
 *
 * Derselbe Befund, ein anderer Rat — und er stimmt: **Die Trefferzahl ist die
 * Kostengröße** (E6), und eine längere Eingabe senkt sie. Der Rat zum Zeitraum
 * bleibt daneben stehen, aber nachgeordnet: Dort ist bei dreißig Tagen weniger
 * zu holen als bei einem Jahr.
 *
 * **Entschieden wird das am Modus der Antwort und nicht an dem der URL.** Der
 * Rat gehört zu dem Vergleich, der die Zahl erzeugt hat.
 */
function Trefferzeile({ ergebnis }: { ergebnis: BamSuchergebnis | undefined }) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  if (ergebnis === undefined) {
    return null;
  }

  const werte = {
    anzahl: formatiereZahl(ergebnis.nachrichten.length, sprache),
    von: formatiereZeitpunkt(ergebnis.von, sprache, zone),
    bis: formatiereZeitpunkt(ergebnis.bis, sprache, zone),
  };

  const abgeschnitten =
    modusAusAntwort(ergebnis.modus) === "praefix"
      ? texte.suche.ergebnis.abgeschnittenPraefix
      : texte.suche.ergebnis.abgeschnitten;

  return (
    <p className="text-basis" role="status">
      {einsetzen(ergebnis.abgeschnitten ? abgeschnitten : texte.suche.ergebnis.anzahl, werte)}
    </p>
  );
}

/**
 * **Der Hinweis, dass präfixweise gesucht wird — samt dem Weg zurück.**
 *
 * Ein Modus, aus dem man nur durch Neuladen herauskommt, ist eine Falle. Der
 * Rückweg steht deshalb neben dem Hinweis und nicht in einem Menü, und er ist
 * **sofort** da: auch während geladen wird, wo es noch kein Ergebnis gibt.
 *
 * ## Der Zeitraum steht dabei, und das ist der Preis der Verkleinerung
 *
 * Der Präfixmodus läuft über höchstens dreißig Tage
 * ({@link PRAEFIX_FENSTER_TAGE}); wer aus einem Jahresfenster hierher gekommen
 * ist, bekommt Ergebnisse aus einem Ausschnitt davon. **Ohne die beiden
 * Datumsangaben liest er „nicht gefunden" als „nicht vorhanden"**
 * (`docs/bam-suche.md` §23).
 *
 * **Genannt wird das Fenster aus der Antwort**, also das tatsächlich verwendete —
 * und nur, wenn die Antwort auch präfixweise gelaufen ist. Solange geladen wird,
 * gibt es keinen durchsuchten Zeitraum, und einer zu behaupten wäre schlechter
 * als keiner.
 */
function Praefixzeile({
  ergebnis,
  aufExakt,
}: {
  ergebnis: BamSuchergebnis | undefined;
  aufExakt: () => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const gelaufen = ergebnis !== undefined && modusAusAntwort(ergebnis.modus) === "praefix";

  return (
    <div className="border-border bg-muted flex flex-col gap-1 rounded-md border px-3 py-2">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span className="text-basis" role="status">
          {texte.suche.praefix.laeuft}
        </span>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="min-h-beruehrung"
          onClick={aufExakt}
        >
          {texte.suche.praefix.zurueck}
        </Button>
      </div>

      {gelaufen ? (
        <p className="text-muted-foreground text-beiwerk max-w-prose">
          {einsetzen(texte.suche.praefix.zeitraum, {
            von: formatiereZeitpunkt(ergebnis.von, sprache, zone),
            bis: formatiereZeitpunkt(ergebnis.bis, sprache, zone),
          })}
        </p>
      ) : null}
    </div>
  );
}

/**
 * **Das Angebot — und es läuft erst auf Zutun.**
 *
 * Es steht im Leerzustand der Trefferliste und an keiner zweiten Stelle: kein
 * Hinweis am Suchfeld, kein Vorschlag beim Tippen. Wann es erscheint, entscheidet
 * `zeigtPraefixAngebot`; **dass** es erst auf einen Klick läuft, entscheidet
 * M50 — der Präfixmodus ist die teuerste Zugriffsform dieses Projekts (3,851 s
 * im schlimmsten bekannten Fall über dreißig Tage). Ein Knopf macht diesen Preis
 * zu einer Entscheidung des Nutzers statt zu einer Nebenwirkung.
 *
 * **Der Text nennt, was passiert, und nicht wie es heißt.** „Präfixsuche" hilft
 * dem Nutzer nicht, der kein EDI-Spezialist ist. Dazu ein Satz Erwartung, weil
 * das Ergebnis anders aussehen wird: Es erscheinen auch längere Nummern.
 *
 * **Der Knopf trägt eine Beschriftung und kein bloßes Symbol** — er ist selten
 * sichtbar und muss beim ersten Mal verständlich sein.
 */
function PraefixAngebot({ aufPraefix }: { aufPraefix: () => void }) {
  const texte = useTexte();

  return (
    <div className="mt-2 flex max-w-prose flex-col items-center gap-2">
      <p className="text-foreground text-basis">{texte.suche.praefix.angebot}</p>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="min-h-beruehrung"
        onClick={aufPraefix}
      >
        {texte.suche.praefix.angebotKnopf}
      </Button>
      <p className="text-beiwerk">{texte.suche.praefix.angebotErwartung}</p>
    </div>
  );
}

/**
 * **Der Deckel, von außen erwischt.**
 *
 * Die Oberfläche löst `praefixsuche-fenster-zu-gross` nie selbst aus — der Knopf
 * verkleinert das Fenster vorher. Wer sich eine URL von Hand baut, landet
 * trotzdem hier, und dann ist es ein gewöhnlicher Fehlerzustand **mit Ausweg**
 * und keine leere Seite.
 *
 * **Zwei Auswege, und beide stehen da:** das Fenster auf den Deckel verkleinern
 * (hier), oder wieder genau suchen (im Präfixhinweis darüber). Dazu bleiben die
 * Zeitraum-Bedienelemente stehen wie in jedem anderen Zustand.
 *
 * ## Die Zahlen kommen aus der Antwort
 *
 * `grenzeTage` und `angefragtTage` stehen im Rumpf (`docs/frontend-grundlagen.md`
 * §6) — dieselbe Bauform wie bei `suche-fenster-zu-gross` in der
 * Nachrichtenliste. **Fehlt eine von beiden**, greift der allgemeine Satz aus dem
 * Fehlerkatalog statt einer Meldung mit einer Lücke darin.
 *
 * ## Verkleinert wird gegen das `bis` der **URL**
 *
 * Eine Antwort gibt es hier nicht, und die Browseruhr ist keine Quelle (Regel
 * Z1). Das `bis` steht in der URL — ohne es wäre das Fenster gar nicht über den
 * Deckel gekommen. Fehlt es wider Erwarten doch, entfällt der Knopf, statt einen
 * Zeitpunkt zu erfinden.
 */
function FensterZuGross({
  fehler,
  bis,
  aufFenster,
}: {
  fehler: ProblemFehler;
  bis: Date | null;
  aufFenster: (von: Date | null, bis: Date | null) => void;
}) {
  const texte = useTexte();

  const grenzeTage = fehler.zahl("grenzeTage");
  const angefragtTage = fehler.zahl("angefragtTage");
  const grenze = grenzeTage ?? PRAEFIX_FENSTER_TAGE;

  const text =
    grenzeTage !== undefined && angefragtTage !== undefined
      ? einsetzen(texte.suche.praefix.fensterZuGross, {
          grenze: grenzeTage,
          angefragt: angefragtTage,
        })
      : undefined;

  return (
    <Fehler fehler={fehler} text={text}>
      {bis === null ? null : (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="min-h-beruehrung mt-3"
          onClick={() => {
            const enger = fensterZurueck(bis, grenze);
            aufFenster(enger.von, enger.bis);
          }}
        >
          {einsetzen(texte.suche.praefix.fensterVerkleinern, { grenze })}
        </Button>
      )}
    </Fehler>
  );
}

/**
 * **Die Varianten der Normalisierung, sobald sie vom Getippten abweichen.**
 *
 * Keine stille Korrektur: Wer `4711815` tippt und `004711815` findet, muss
 * erfahren, warum — sonst sähe die Trefferliste aus, als hätte die Datenbank
 * etwas anderes enthalten als sie enthält, und wer die Nummer anschließend im
 * Altwerkzeug nachschlägt, fände sie dort nicht.
 *
 * **Die Leerzeichen-Fassung steht nicht darin**, und das entscheidet der
 * Endpunkt: Ein führendes Leerzeichen sieht der Nutzer weder in seiner Eingabe
 * noch im angezeigten Wert; eine Zeile *„gesucht nach 4711 und ␣4711"* läse sich
 * wie ein Anzeigefehler (`docs/bam-suche.md` §3). Wohin es gehört, steht in der
 * Hilfe im Leerzustand.
 */
function Variantenzeile({ ergebnis }: { ergebnis: BamSuchergebnis | undefined }) {
  const texte = useTexte();

  const zeilen = (ergebnis?.begriffe ?? [])
    .map((begriff) => ({
      eingabe: begriff.eingabe,
      weitere: abweichendeVarianten(begriff.eingabe, begriff.varianten),
    }))
    .filter((zeile) => zeile.weitere.length > 0);

  if (zeilen.length === 0) {
    return null;
  }

  return (
    <ul className="text-muted-foreground text-beiwerk flex flex-col gap-0.5">
      {zeilen.map((zeile) => (
        <li key={zeile.eingabe}>
          {einsetzen(texte.suche.varianten, {
            eingabe: zeile.eingabe,
            fassungen: zeile.weitere.join(", "),
          })}
        </li>
      ))}
    </ul>
  );
}

/**
 * **Das Zeitfenster, sichtbar und verstellbar.**
 *
 * Es verändert nicht nur die Laufzeit, sondern die **Antwort**: M35 misst 279 von
 * 234.159 Treffern bei einem Tagesfenster. Wer nicht weiß, dass er durch ein
 * Fenster schaut, hält das Gefundene für alles, was es gibt — deshalb steht es
 * neben der Trefferzahl und nicht in den Voreinstellungen.
 *
 * ## Drei Bedienelemente, und keines rechnet gegen die Browseruhr
 *
 * Das ist der Punkt, an dem Regel Z1 hängt. Die Anwendungsuhr steht im Profil
 * `dev` Monate hinter der realen Zeit; ein aus `Date.now()` gerechnetes Fenster
 * liefe an den Daten vorbei. Deshalb:
 *
 * - **„Auf ein Jahr erweitern"** rechnet vom `bis` **aus der Antwort** zurück —
 *   also von dem Zeitpunkt, den das Backend selbst verwendet hat.
 * - **„Vorgabe wiederherstellen"** entfernt beide Parameter und überlässt dem
 *   Backend seine 30 Tage. Ein zweiter Standardwert hier liefe dem ersten
 *   irgendwann hinterher.
 * - **Die beiden Felder** nehmen Wanduhrzeit entgegen und rechnen in der
 *   **Anzeigezone** um, nicht in der des Browsers (`lib/format.ts`).
 *
 * ## Ein halb getipptes Feld meldet sich nicht von selbst
 *
 * Ein `datetime-local` liefert seinen Wert erst, wenn **alle** Segmente stehen —
 * und feuert bis dahin kein `input`. Wer nur das Datum einträgt, sieht es im Feld
 * und die Anwendung täte nichts und sagte nichts. Herausgegeben wird der Zustand
 * allein über `validity.badInput`, gelesen an `keyup` und `blur`. Derselbe Befund
 * wie am freien Fenster der Liste (`docs/nachrichtenliste.md` §8.2).
 */
function Zeitfensterzeile({
  ergebnis,
  modus,
  von,
  bis,
  aufFenster,
}: {
  ergebnis: BamSuchergebnis | undefined;
  modus: Suchmodus;
  von: Date | null;
  bis: Date | null;
  aufFenster: (von: Date | null, bis: Date | null) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();
  const vonId = useId();
  const bisId = useId();
  const hinweisId = useId();

  // „Ändern gedrückt, aber noch nichts eingetragen" ist kein Filterzustand: Der
  // Ausschnitt ist derselbe wie vorher. Ohne diesen Komponentenzustand wären die
  // Felder aber gar nicht erreichbar, solange die Vorgabe gilt.
  const [offen, setOffen] = useState(false);
  const [angefangen, setAngefangen] = useState({ von: false, bis: false });

  function merkeEingabestand(feld: "von" | "bis", ziel: HTMLInputElement) {
    const halb = ziel.validity.badInput;
    setAngefangen((bisher) => (bisher[feld] === halb ? bisher : { ...bisher, [feld]: halb }));
  }

  const eigenesFenster = von !== null || bis !== null;
  const felderSichtbar = offen || eigenesFenster;

  // Das Jahresfenster hängt am Fenster der Antwort. Solange keine da ist, gibt es
  // keinen Anker — und der Knopf erscheint nicht, statt gegen die Browseruhr zu
  // rechnen.
  const anker = ergebnis === undefined ? null : new Date(ergebnis.bis);
  const spanne =
    ergebnis === undefined ? null : spanneInTagen(new Date(ergebnis.von), new Date(ergebnis.bis));

  const hinweis =
    angefangen.von || angefangen.bis
      ? texte.nachrichten.zeitfenster.unvollstaendig
      : (von === null) !== (bis === null)
        ? texte.nachrichten.zeitfenster.beideNoetig
        : undefined;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="min-h-beruehrung"
          aria-expanded={felderSichtbar}
          onClick={() => setOffen((bisher) => !bisher)}
        >
          {texte.suche.fenster.aendern}
        </Button>

        {/*
         * **Im Präfixmodus gibt es diesen Knopf nicht**, und das ist die eine
         * Stelle, an der Teil 4 ein vorhandenes Bedienelement anfasst.
         *
         * Er führte dort in **ein Klick** in ein garantiertes `400
         * praefixsuche-fenster-zu-gross` — der Modus ist auf dreißig Tage
         * gedeckelt (`docs/bam-suche.md` §18). Die Zusage lautet, dass die
         * Oberfläche diesen Fehler **nie selbst auslöst**; ein Knopf, dessen
         * einziges Ergebnis eine Fehlermeldung ist, bricht sie.
         *
         * **Die Zeitraum-Bedienelemente wirken deshalb trotzdem unverändert**,
         * und zwar die, um die es geht: Die beiden Felder verschieben das
         * Dreißig-Tage-Fenster weiter zurück, „Vorgabe wiederherstellen" bleibt.
         * Weggenommen ist nur der eine Weg, der in diesem Modus keinen
         * erreichbaren Zustand herstellen kann. Wer ein Jahr will, sucht wieder
         * genau — der Knopf dafür steht unmittelbar darüber.
         */}
        {modus === "exakt" && anker !== null && spanne !== null && spanne < 360 ? (
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="min-h-beruehrung"
            onClick={() => {
              const jahr = jahresfensterAb(anker);
              aufFenster(jahr.von, jahr.bis);
            }}
          >
            {texte.suche.fenster.einJahr}
          </Button>
        ) : null}

        {eigenesFenster ? (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="min-h-beruehrung"
            onClick={() => {
              setAngefangen({ von: false, bis: false });
              aufFenster(null, null);
            }}
          >
            {texte.suche.fenster.vorgabe}
          </Button>
        ) : null}
      </div>

      {felderSichtbar ? (
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-0.5">
            <Label htmlFor={vonId} className="text-beiwerk">
              {texte.nachrichten.zeitfenster.von}
            </Label>
            <Input
              id={vonId}
              type="datetime-local"
              className="h-bedienelement w-auto"
              aria-describedby={hinweis === undefined ? undefined : hinweisId}
              value={wanduhrzeitFuerEingabe(von, zone)}
              onKeyUp={(ereignis) => merkeEingabestand("von", ereignis.currentTarget)}
              onBlur={(ereignis) => merkeEingabestand("von", ereignis.currentTarget)}
              onChange={(ereignis) => {
                merkeEingabestand("von", ereignis.currentTarget);
                aufFenster(zeitpunktAusWanduhrzeit(ereignis.target.value, zone), bis);
              }}
            />
          </div>
          <div className="flex flex-col gap-0.5">
            <Label htmlFor={bisId} className="text-beiwerk">
              {texte.nachrichten.zeitfenster.bis}
            </Label>
            <Input
              id={bisId}
              type="datetime-local"
              className="h-bedienelement w-auto"
              aria-describedby={hinweis === undefined ? undefined : hinweisId}
              value={wanduhrzeitFuerEingabe(bis, zone)}
              onKeyUp={(ereignis) => merkeEingabestand("bis", ereignis.currentTarget)}
              onBlur={(ereignis) => merkeEingabestand("bis", ereignis.currentTarget)}
              onChange={(ereignis) => {
                merkeEingabestand("bis", ereignis.currentTarget);
                aufFenster(von, zeitpunktAusWanduhrzeit(ereignis.target.value, zone));
              }}
            />
          </div>
        </div>
      ) : null}

      {hinweis === undefined ? null : (
        // Dieselbe ruhige Farbrolle wie am Suchfeld der Liste: Der Nutzer hat
        // nichts falsch gemacht, er ist nur noch nicht fertig.
        <p id={hinweisId} className="text-muted-foreground text-beiwerk max-w-prose">
          {hinweis}
        </p>
      )}

      <span className="sr-only">
        {ergebnis === undefined
          ? null
          : einsetzen(texte.suche.fenster.gilt, {
              von: formatiereZeitpunkt(ergebnis.von, sprache, zone),
              bis: formatiereZeitpunkt(ergebnis.bis, sprache, zone),
            })}
      </span>
    </div>
  );
}

/**
 * **Die Seite ohne Begriffe — eine Einladung und keine leere Fläche.**
 *
 * Es gibt keinen Navigationseintrag für die Suche; wer das Feld leert, hat sonst
 * keine Wegweisung mehr und die Seite wäre eine Sackgasse. Sie erklärt deshalb,
 * **was** gesucht werden kann, und verweist auf das Feld in der Kopfzeile.
 *
 * **Kein zweites Suchfeld auf der Seite.** Zwei Felder für dieselbe Sache sind so
 * verwirrend wie zwei für verschiedene.
 *
 * **Die Belegarten des eigenen Mandanten stehen dabei, wenn es welche gibt.** Das
 * ist die einzige Aufzählung auf dieser Seite, die für *diesen* Mandanten
 * nachweislich gilt — alles andere wäre ein Beispiel. Hat er keine konfiguriert
 * ({@code EDITIONLINGERI}, {@code SYSTEM}, {@code WOC}), erscheint auch keine
 * Liste: kein Platzhalter, keine leere Aufzählung.
 *
 * ## Und hier steht der einzige Satz über die Belegart im Präfixmodus
 *
 * Über führende Nullen hinweg findet die Suche nach dem **Anfang** einer Nummer
 * nur mit gewählter Belegart — Auffüllen und Präfixmuster ankern gegeneinander,
 * und der Ausweg zieht die Nullen ins Muster, was eine Sollänge braucht (M49‑1,
 * `docs/bam-suche.md` §17).
 *
 * **Daraus wird kein Zwang und keine Belehrung.** Das Angebot erscheint mit und
 * ohne gewählte Belegart; erwähnt wird sie an dieser einen Stelle, wo ohnehin die
 * Hilfe zur Suche steht — und **nur, wenn dieser Mandant überhaupt Belegarten
 * hat**. Sonst nennte der Satz ein Bedienelement, das es hier nicht gibt.
 *
 * **Kein Sonderfall für Typ 9006.** Dass die Lieferschein-Nr. nicht in
 * `bam_sollaenge` steht, ist eine offene Entscheidung des Auftraggebers
 * ([`bam-sollaengen.md`](../../../../docs/bam-sollaengen.md) §8) und wird nicht
 * durch einen zweiten Kuratierungsort geheilt — eine zweite Stelle liefe der
 * ersten hinterher.
 */
function Leerzustand() {
  const texte = useTexte();
  const typen = useBamTypen().data ?? [];

  return (
    <div className="border-border bg-card flex max-w-prose flex-col gap-3 rounded-lg border p-4">
      <h2 className="font-medium">{texte.suche.leer.titel}</h2>
      <p className="text-muted-foreground">{texte.suche.leer.was}</p>

      {typen.length === 0 ? null : (
        <div className="flex flex-col gap-1.5">
          <p className="text-muted-foreground text-beiwerk">{texte.suche.leer.belegarten}</p>
          <ul className="flex flex-wrap gap-1.5">
            {typen.map((eintrag) => (
              <li key={eintrag.typ}>
                <Marke>{eintrag.bezeichnung}</Marke>
              </li>
            ))}
          </ul>
        </div>
      )}

      <p className="text-muted-foreground text-beiwerk">{texte.suche.leer.hilfe}</p>
      {typen.length === 0 ? null : (
        <p className="text-muted-foreground text-beiwerk">{texte.suche.leer.belegartHilfe}</p>
      )}
      <p className="text-muted-foreground text-beiwerk">{texte.suche.leer.zeitraumHilfe}</p>
    </div>
  );
}
