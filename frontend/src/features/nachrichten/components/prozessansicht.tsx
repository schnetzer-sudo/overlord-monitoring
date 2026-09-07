"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { ArrowLeft, X } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { ZeitraumUmschalter } from "@/components/zeitraum-umschalter";
import { Fehler, Laden, Leer } from "@/components/zustand";
import { einsetzen } from "@/i18n";
import { useSprache, useTexte } from "@/i18n/provider";
import { useAnzeigezone } from "@/components/zeitzone";
import { formatiereZahl, formatiereZeitpunkt } from "@/lib/format";
import { angezeigterBaumfenstermodus, hervorgehobenerBaumzeitraum } from "@/lib/rollupzeitraum";
import { ROUTEN } from "@/lib/routen";
import { cn } from "@/lib/utils";

import type { Fenster, Partnerknoten, Prozessbaum, Prozessknoten } from "../api";
import type { Nachrichtenfilter, Sortierung } from "../filter";
import {
  useEscapeSchliesst,
  useNachrichtenSeite,
  useProzessansichtzustand,
  useProzessbaum,
} from "../hooks";
import { baumabfrage, baumfensterFehler, listenfilter } from "../prozessansicht";
import {
  eingegrenzterBaum,
  ohnePfad,
  partnertext,
  partnerVon,
  pfadZuProzess,
  prozessAus,
  richtungstext,
  sichtbareProzesse,
} from "../prozessbaum";
import { BaumfensterFelder } from "./baumfenster-felder";
import { Blaettern } from "./blaettern";
import { NachrichtDetail } from "./nachricht-detail";
import { NachrichtenTabelle } from "./nachrichten-tabelle";
import { ProzessBaum } from "./prozess-baum";

/**
 * Die Prozessansicht — **Baum links, Übertragungen rechts** (`/prozesse`,
 * `docs/process-view.md` §15).
 *
 * ## Drei Breitenzustände, an den vorhandenen Schwellen
 *
 * | Breite | Aufteilung |
 * |---|---|
 * | ab `xl` (1280 px) | Baum in fester Breite links, Liste rechts |
 * | `md` bis `xl` | beide nebeneinander, der Baum mit 40 % Anteil |
 * | unter `md` (768 px) | eine Spalte: Baum → Liste → Panel, mit Weg zurück |
 *
 * **Keine neuen Umbruchpunkte.** `md` ist der des Projekts
 * (`docs/visuelles-konzept.md` §6), `xl` der, an dem das Nachrichtenpanel neben
 * die Liste tritt (`docs/nachrichtendetail.md` §10.7). Umgesetzt über Klassen
 * und **nicht** über eine Abfrage der Fensterbreite in JavaScript — die wäre ein
 * zweiter Umbruchpunkt neben dem der Ansicht, und zwei laufen auseinander.
 *
 * **Unter `md` wird ausgeblendet, nicht ausgehängt** (`display: none`). Der Baum
 * behält damit seinen Aufklappzustand und die Liste ihre Seitenposition; wer
 * zurückgeht, findet beides wieder, ohne dass eine zweite Abfrage auf die
 * Produktionsdatenbank geht — dieselbe Bauform wie in der Nachrichtenliste.
 *
 * ## E‑57 — Beim Öffnen des Panels weicht der **Baum** *(02.09.2026, dreht E‑53 um)*
 *
 * Drei Spalten sind bei 1280 px unmöglich: Baum (26 rem) und Panel (26 rem) sind
 * zusammen 832 px von rund 1.016 px nutzbarer Inhaltsbreite. Eine der drei muss
 * weichen — bis heute war es die Liste, weil *der Baum der Kontext sei, den der
 * Nutzer behalten will*.
 *
 * **Wer mehrere Nachrichten desselben Prozesses durchsieht, braucht die Liste
 * und nicht den Baum.** Der Baum wird einmal am Anfang benutzt; die Liste ist
 * der Ort, an dem weitergeklickt wird. Der Kontext geht dabei nicht verloren, er
 * wechselt die Form: Die Überschrift der rechten Spalte trägt den Prozessnamen
 * ohnehin.
 *
 * | Zustand | Aufteilung |
 * |---|---|
 * | ab `xl`, Panel offen | **Baum weicht**, Liste und Panel stehen nebeneinander |
 * | `md` bis `xl`, Panel offen | Baum weicht, Panel steht an der Stelle der Liste |
 * | unter `md` | eine Spalte: Baum → Liste → Panel |
 * | Panel zu | Baum und Liste nebeneinander |
 *
 * **Ausgeblendet, nicht ausgehängt** (`display: none`) — dieselbe Bauform, die
 * E‑53 für die Liste gewählt hatte. Der Aufklappzustand des Baums bleibt, und
 * beim Schließen geht keine zweite Abfrage hinaus.
 *
 * ## Ein Scrollbereich, wie überall
 *
 * Beide Spalten sitzen im **einen** Scrollbereich des Anwendungsrahmens
 * (`docs/frontend-grundlagen.md` §7). Kein `overflow-y-auto`, kein `h-full`,
 * kein `h-dvh` — die bekannte Folge ist, dass ein weit aufgeklappter Baum die
 * Liste daneben nach oben aus dem Bild schiebt. Der Kopf der Baumspalte klebt
 * dafür (`sticky`), so wie die Tabellenkopfzeilen von Katalog und Benutzern.
 */
export function ProzessAnsicht() {
  const texte = useTexte();
  const {
    zustand,
    setzeZeitraum,
    setzeFreiesFenster,
    setzeProzess,
    setzeNachricht,
    setzeNurMitVerkehr,
    setzeSortierung,
  } = useProzessansichtzustand();
  const antwort = useProzessbaum(baumabfrage(zustand));

  /*
   * „Frei gedrückt, aber noch nichts eingetragen" steht bewusst nicht in der
   * URL — es ist derselbe Baum wie gar keine Auswahl. Ohne diesen
   * Komponentenzustand wäre der freie Modus über die Oberfläche gar nicht
   * erreichbar (`lib/rollupzeitraum.ts`, `angezeigterBaumfenstermodus`).
   */
  const [freiGewaehlt, setFreiGewaehlt] = useState(false);
  const fenstermodus = angezeigterBaumfenstermodus(zustand, freiGewaehlt);
  const fehlerAnDenFeldern = baumfensterFehler(antwort.error);

  /*
   * **Der letzte gelieferte Baum bleibt stehen, solange die Antwort an die
   * Datumsfelder gehört.** Wer zwischen „Von" und „Bis" tippt, bekommt vom
   * Backend `zeitfenster-unvollstaendig`; ihm dafür den Baum wegzunehmen hieße,
   * die Ansicht zu leeren, weil er noch nicht fertig ist. Dieselbe Bauform wie
   * `letzteSeite` in `useNachrichtenSeite`: ausdrücklich gehalten und nicht über
   * `placeholderData`, damit ein Fensterwechsel weiterhin als Laden sichtbar ist.
   * Angepasst während des Renderns, nicht in einem Effekt.
   */
  const [letzterBaum, setLetzterBaum] = useState<Prozessbaum | undefined>(undefined);
  if (antwort.data !== undefined && antwort.data !== letzterBaum) {
    setLetzterBaum(antwort.data);
  }
  const baum = antwort.data ?? (fehlerAnDenFeldern === undefined ? undefined : letzterBaum);

  const [eingrenzung, setEingrenzung] = useState("");

  /**
   * Der Aufklappzustand — **abgeleitet, nicht nachgeführt.**
   *
   * Offen ist ein Knoten, wenn der Nutzer ihn umgeschaltet hat; hat er das
   * nicht, entscheidet der Pfad zum gewählten Prozess. Damit öffnet ein tiefer
   * Link den Baum an der richtigen Stelle, **ohne** dass ein Effekt Zustand
   * nachträgt — ein `setState` im Effekt ist im Projekt verboten
   * (`react-hooks/set-state-in-effect`), und ein Effekt liefe hier ohnehin erst
   * nach dem ersten Malen.
   */
  const [umgeschaltet, setUmgeschaltet] = useState<ReadonlyMap<string, boolean>>(() => new Map());
  const pfad = useMemo(
    () => pfadZuProzess(baum?.partner ?? [], zustand.prozess),
    [baum?.partner, zustand.prozess],
  );
  const imPfad = useMemo(() => new Set(pfad), [pfad]);

  /*
   * **Ein Wechsel des gewählten Prozesses vergisst, was entlang seines Pfades
   * umgeschaltet war** (`prozessbaum.ts` {@link ohnePfad}).
   *
   * Ohne das bliebe ein einmal zugeklappter Partner zu, auch wenn ein später
   * gewählter Prozess unter ihm hängt — `??` fällt bei `false` nicht durch. Der
   * Fall ist der dokumentierte Weg und kein Sonderfall: zuklappen, woanders
   * wählen, **Zurück** drücken.
   *
   * Angepasst **während des Renderns** und nicht in einem Effekt — dasselbe
   * Muster wie beim Zurücksetzen des Seitenstapels in `useNachrichtenSeite`:
   * React verwirft den begonnenen Durchlauf und rendert sofort neu, der
   * Zwischenstand erscheint nie auf dem Bildschirm.
   */
  const [vorherigerProzess, setVorherigerProzess] = useState(zustand.prozess);
  let aktuellUmgeschaltet = umgeschaltet;
  if (vorherigerProzess !== zustand.prozess) {
    setVorherigerProzess(zustand.prozess);
    aktuellUmgeschaltet = ohnePfad(umgeschaltet, pfad);
    setUmgeschaltet(aktuellUmgeschaltet);
  }

  const istOffen = useCallback(
    (schluessel: string) => aktuellUmgeschaltet.get(schluessel) ?? imPfad.has(schluessel),
    [aktuellUmgeschaltet, imPfad],
  );
  const aufUmschalten = useCallback(
    (schluessel: string) =>
      setUmgeschaltet((bisher) => {
        const neu = new Map(bisher);
        neu.set(schluessel, !(bisher.get(schluessel) ?? imPfad.has(schluessel)));
        return neu;
      }),
    [imPfad],
  );

  const gefiltert = useMemo(
    () => eingegrenzterBaum(baum?.partner ?? [], eingrenzung, zustand.nurMitVerkehr),
    [baum?.partner, eingrenzung, zustand.nurMitVerkehr],
  );

  const filter = listenfilter(zustand, baum?.fenster);
  const gewaehlterProzess = prozessAus(baum, zustand.prozess);
  const zuordnung = partnerVon(baum, zustand.prozess);

  const schliesse = useCallback(() => setzeNachricht(null), [setzeNachricht]);
  useEscapeSchliesst(zustand.nachricht !== null, schliesse);

  /**
   * **Unter `md` weicht der Baum — und mit ihm die Zeile, die gerade den Fokus
   * trägt.**
   *
   * Wer den Baum mit der Tastatur bedient und `Eingabe` drückt, verliert den
   * Fokus sonst an `document.body`: Der nächste Tabulator beginnt wieder oben
   * am Anwendungsrahmen, und der Zusammenhang zwischen Auswahl und Ergebnis
   * bricht ab. Der Fokus geht deshalb auf „Zurück zum Baum" — die
   * Schaltfläche, die genau dort steht, wo der Baum eben war.
   *
   * **Ohne Abfrage der Fensterbreite in JavaScript**, und das ist der Kniff:
   * Die Schaltfläche trägt `md:hidden`. Ab `md` ist sie `display: none`, und
   * `focus()` tut auf einem solchen Element nichts — der Fokus bleibt dort, wo
   * der Nutzer ihn hatte, genau wie in der Nachrichtenliste. Die Regel wirkt
   * damit über dieselbe Klasse, die den Umbruch macht, und nicht über einen
   * zweiten Umbruchpunkt daneben.
   *
   * **Nicht beim ersten Rendern.** Ein tiefer Link ist keine Handlung des
   * Nutzers; ihm den Fokus zu verschieben, während er die Seite noch liest,
   * wäre genau der Sprung, den das Nachrichtendetail bewusst vermeidet.
   */
  const zurueckKnopf = useRef<HTMLButtonElement>(null);
  const zuletztGewaehlt = useRef(zustand.prozess);
  useEffect(() => {
    const vorher = zuletztGewaehlt.current;
    zuletztGewaehlt.current = zustand.prozess;
    if (vorher === zustand.prozess || zustand.prozess === null) {
      return;
    }
    zurueckKnopf.current?.focus();
  }, [zustand.prozess]);

  /**
   * **Weicht der Baum, geht der Fokus mit** — die zweite Hälfte der Regel aus
   * §15, seit E‑57 *(02.09.2026)*.
   *
   * ## Der Weg, auf dem der Fokus im Baum liegt, wenn das Panel öffnet
   *
   * Er ist am laufenden System nachgefahren (M129) und nicht erdacht: Prozess
   * wählen, eine Nachricht öffnen, `Escape`, **eine Zeile im Baum fokussieren**,
   * dann mit dem Zurück des Browsers in den Zustand mit `nachricht` — der Baum
   * bekommt `display: none`, und `document.activeElement` fällt auf
   * `document.body`. Der nächste Tabulator begänne wieder oben am
   * Anwendungsrahmen. **Über den Baum selbst ist der Fall nicht erreichbar:** Er
   * setzt nur `prozess`, nie `nachricht`.
   *
   * ## Warum die Bedingung `document.body` heißt und nicht „war im Baum"
   *
   * Gefragt wird nach dem **Ergebnis** und nicht nach der Vorgeschichte, und die
   * Abfrage trennt die Wege sauber: Eine Zeile der Liste trägt `tabIndex={0}`,
   * ein Klick und die Eingabetaste lassen den Fokus also **auf ihr** — dort wird
   * nichts verschoben (nachgefahren, M129). Ein Kettenglied im Panel lässt ihn
   * im Panel. Nur der gefallene Fokus wird aufgefangen.
   *
   * ⚠️ **Zwei Fassungen davor waren zu einfach, und beide sind am laufenden
   * System durchgefallen** (M129):
   *
   * 1. *Die Abfrage im Effekt selbst.* Wenn der Effekt läuft, steht
   *    `document.activeElement` **noch auf der Baumzeile** — der Browser setzt
   *    den Fokus erst zurück, wenn er das Rendern das nächste Mal auffrischt.
   *    Deshalb das `requestAnimationFrame`.
   * 2. *Nur `document.body` abfragen.* Der Fokus fällt **nicht zuverlässig**
   *    dorthin: In einem Teil der Läufe bleibt `document.activeElement` die
   *    Baumzeile, obwohl ihr Vorfahr `display: none` trägt. Beides ist derselbe
   *    Bruch — der nächste Tabulator beginnt oben am Anwendungsrahmen —, und
   *    deshalb fängt die Bedingung **beide** Fälle: Fokus gefallen **oder**
   *    Fokus im weggeblendeten Baum.
   *
   * **Nicht beim ersten Rendern**, und dafür braucht es keine eigene Angabe: Ein
   * tiefer Link ist ein Zustandswechsel, den {@link zuletztGeoeffnet} gar nicht
   * erst sieht — dieselbe Bauform wie beim Rückweg unter `md` darüber. Er landet
   * ohnehin auf `document.body`; abgefangen wird er von dieser Reihenfolge und
   * nicht von der Fokusabfrage.
   *
   * **Der Bereich fängt ihn und nicht die Schaltfläche „Schließen".** Die wäre
   * das genauere Gegenstück zu „Zurück zum Baum", steht aber in
   * `nachricht-detail.tsx` — einem Baustein, den diese Ansicht als vierter
   * Einhängepunkt benutzt und in dieser Runde nicht anfasst. Der Bereich mit
   * `tabIndex={-1}` setzt die Tabulatorstelle an seinen Anfang; der nächste
   * Tabulator führt in das Panel und nicht an den Rahmen.
   */
  const baumSpalte = useRef<HTMLDivElement>(null);
  const panelBereich = useRef<HTMLDivElement>(null);
  const zuletztGeoeffnet = useRef(zustand.nachricht);
  useEffect(() => {
    const vorher = zuletztGeoeffnet.current;
    zuletztGeoeffnet.current = zustand.nachricht;
    if (vorher !== null || zustand.nachricht === null) {
      return;
    }
    const bild = requestAnimationFrame(() => {
      const aktiv = document.activeElement;
      const gefallen = aktiv === null || aktiv === document.body;
      if (!gefallen && baumSpalte.current?.contains(aktiv) !== true) {
        return;
      }
      panelBereich.current?.focus();
    });
    return () => cancelAnimationFrame(bild);
  }, [zustand.nachricht]);

  const etwasGewaehlt = zustand.prozess !== null;
  const panelOffen = zustand.nachricht !== null;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <h1 className="text-ueberschrift font-semibold">{texte.navigation.eintraege.prozesse}</h1>
        {/*
         * **Außerhalb der Zustandskette**, wie im Dashboard — aber **nicht aus
         * dessen Grund.** Dort ist der Umschalter der Weg herauszufinden, ob es
         * am Zeitraum liegt (E‑p); hier kann er das gar nicht sein: Der Umfang
         * des Baums ist **fensterunabhängig** (E‑35), ein leerer Baum bleibt
         * über alle drei Paare leer, und die Ursache liegt dann am Katalog.
         *
         * Er steht trotzdem hier oben und bleibt bedienbar, weil er die
         * **Kennzahlen** an jedem Knoten ändert — und weil ein Bedienelement,
         * das mit dem Inhalt verschwindet, dem Nutzer genau dann fehlt, wenn er
         * etwas ausprobieren will. Im Ladezustand gesperrt.
         */}
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
          <ZeitraumUmschalter
            gewaehlt={hervorgehobenerBaumzeitraum(zustand, freiGewaehlt, baum?.zeitraum)}
            aufAuswahl={(zeitraum) => {
              setFreiGewaehlt(false);
              setzeZeitraum(zeitraum);
            }}
            aufFrei={() => {
              // Der freie Modus beginnt leer: Ein vorbelegtes Fenster wäre ein
              // zweiter Standardwert. Sichtbar wird die Wahl über `freiGewaehlt`.
              setFreiGewaehlt(true);
              setzeFreiesFenster(null, null);
            }}
            gesperrt={antwort.isPending}
          />
          {/* Die Felder stehen **neben** dem Umschalter, nicht darin — nur so
              bleibt das Dashboard zeichengleich. Sie sind Kinder derselben
              Zeile in derselben Höhe: Die Zeile wird breiter, nicht höher, und
              der Umschalter springt nicht. */}
          {fenstermodus === "frei" ? (
            <BaumfensterFelder
              von={zustand.von}
              bis={zustand.bis}
              aufAenderung={setzeFreiesFenster}
              fehler={fehlerAnDenFeldern}
            />
          ) : null}
        </div>
      </div>

      {antwort.isPending ? (
        <Laden zeilen={8} />
      ) : antwort.isError && fehlerAnDenFeldern === undefined ? (
        <Fehler fehler={antwort.error} aufWiederholen={() => void antwort.refetch()} />
      ) : baum === undefined ? null : baum.gesamt.anzahlProzesse === 0 ? (
        <Leer titel={texte.prozesse.baum.leerTitel} hinweis={texte.prozesse.baum.leerHinweis} />
      ) : (
        <div className="flex flex-col gap-4 md:flex-row md:items-start">
          <div
            ref={baumSpalte}
            className={cn(
              "xl:w-baumspalte min-w-0 md:w-2/5 md:shrink-0",
              /*
               * **Zwei Gründe zu weichen, und sie überlagern sich nicht** — die
               * eine Fassung darf die andere nicht überschreiben. Bei offenem
               * Panel weicht der Baum in **jeder** Breite (E‑57); ohne Panel
               * weicht er nur unter `md`, wo die Liste an seine Stelle tritt
               * (§15). Ein `hidden` neben einem `hidden md:block` gewänne ab
               * `md` nicht, deshalb steht hier eine Auswahl und keine Kette.
               */
              panelOffen ? "hidden" : etwasGewaehlt && "hidden md:block",
            )}
          >
            <Baumspalte
              baum={baum}
              gefiltert={gefiltert}
              eingrenzung={eingrenzung}
              aufEingrenzung={setEingrenzung}
              nurMitVerkehr={zustand.nurMitVerkehr}
              aufNurMitVerkehr={setzeNurMitVerkehr}
              gewaehlt={zustand.prozess}
              springeZurAuswahl={zustand.nachricht === null}
              istOffen={istOffen}
              aufUmschalten={aufUmschalten}
              aufAuswahl={setzeProzess}
            />
          </div>

          <div className={cn("min-w-0 flex-1", !etwasGewaehlt && "hidden md:block")}>
            {filter === null ? (
              <Leer
                titel={texte.prozesse.liste.leerTitel}
                hinweis={texte.prozesse.liste.leerHinweis}
              >
                <Link
                  href={ROUTEN.nachrichten}
                  className="text-accent-foreground min-h-beruehrung focus-visible:ring-ring mt-1 inline-flex items-center rounded-sm underline underline-offset-2 focus-visible:ring-2 focus-visible:outline-none"
                >
                  {texte.prozesse.liste.zurNachrichtenliste}
                </Link>
              </Leer>
            ) : (
              <div className="flex flex-col gap-3">
                <Kopf
                  prozess={gewaehlterProzess}
                  processId={zustand.prozess as string}
                  zuordnung={zuordnung}
                  fenster={baum.fenster}
                  zurueckKnopf={zurueckKnopf}
                  aufZurueck={() => setzeProzess(null)}
                />

                {/*
                 * **Ab `xl` steht das Panel neben der Liste, darunter an ihrer
                 * Stelle** — Klasse für Klasse dieselbe Hülle wie in der
                 * Nachrichtenliste (`nachrichten-ansicht.tsx`). Seit E‑57 ist es
                 * dieselbe Aufteilung und nicht mehr eine eigene: Was hier
                 * weicht, ist der Baum links.
                 *
                 * **Ausgeblendet statt ausgehängt** (`display: none`): Die
                 * Seitenposition der Liste bleibt stehen, und beim Schließen
                 * geht keine zweite Abfrage hinaus.
                 */}
                <div className="flex flex-col gap-4 xl:flex-row xl:items-start">
                  <div className={cn("min-w-0 flex-1", panelOffen && "hidden xl:block")}>
                    <Uebertragungen
                      filter={filter}
                      gewaehlteNachricht={zustand.nachricht}
                      aufNachricht={setzeNachricht}
                      aufSortierung={setzeSortierung}
                    />
                  </div>

                  {/* `zustand.nachricht` und nicht `panelOffen`: Nur die
                      Abfrage am Feld selbst engt den Typ auf `string` ein. */}
                  {zustand.nachricht === null ? null : (
                    /*
                     * **Dieselben Breiten wie in der Nachrichtenliste** — 26 rem ab
                     * `xl`, 30 rem ab `2xl` (`docs/nachrichtendetail.md` §10.7).
                     * Das Panel ist für diese Breite entworfen: Die Zeitleiste
                     * steht senkrecht, „weil das Panel schmal ist" (§10.4).
                     *
                     * **Hier standen bis zum 02.09.2026 `max-w-inhalt` und
                     * `beschriftung-breit`**, weil das Panel unter E‑53 an die
                     * Stelle der Liste trat und deren volle Breite bekam — bei
                     * 1920 px gemessene 1.223 px. Mit E‑57 tut es das ab `xl`
                     * nicht mehr, und beide wären dort **falsch**: Ein Deckel von
                     * 16 rem ließe der Beschriftungsspalte 256 px von 416 px
                     * Panelbreite. Die 10 rem sind genau für dieses Panel gemessen
                     * (`docs/bam-werte.md` §11a).
                     */
                    <div
                      ref={panelBereich}
                      tabIndex={-1}
                      className="min-w-0 focus-visible:outline-none xl:w-[26rem] xl:shrink-0 2xl:w-[30rem]"
                    >
                      <NachrichtDetail
                        // Ein Wechsel der Nachricht ist eine neue Ansicht und kein
                        // neuer Zustand derselben.
                        key={zustand.nachricht}
                        messageId={zustand.nachricht}
                        aufSchliessen={schliesse}
                        schliessenText={texte.nachrichten.detail.schliessen}
                        aufOeffnen={setzeNachricht}
                        /*
                         * **Ohne Umschalter.** Sein Rückweg führt an die
                         * Nachrichtenliste (`lib/routen.ts` `ansichtNebenListe`)
                         * und damit woanders hin, als er herkam — genau die
                         * Überlegung, mit der die Belegsuche ihn seit dem
                         * 13.08.2026 weglässt (`docs/nachrichtendetail.md` §10.7).
                         */
                      />
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Die linke Spalte: Eingrenzung, Schalter, Kopfzahlen und der Baum.
 *
 * **Der Kopf klebt.** Bei 154 Partnern — und aufgeklappt 1.158 Zeilen — wäre ein
 * Eingrenzungsfeld, das mit dem Baum nach oben wandert, nach zwei Bildschirmen
 * nicht mehr erreichbar. `sticky` braucht dafür **keinen** eigenen
 * Scrollcontainer; es hängt sich an den des Anwendungsrahmens, genau wie die
 * Tabellenkopfzeilen von Katalog und Benutzerverwaltung. `-top-4` gleicht dessen
 * senkrechten Innenabstand aus.
 */
function Baumspalte({
  baum,
  gefiltert,
  eingrenzung,
  aufEingrenzung,
  nurMitVerkehr,
  aufNurMitVerkehr,
  gewaehlt,
  springeZurAuswahl,
  istOffen,
  aufUmschalten,
  aufAuswahl,
}: {
  baum: Prozessbaum;
  /** Die **eingegrenzten** Knoten — die Ansicht filtert, die Spalte zeichnet. */
  gefiltert: readonly Partnerknoten[];
  eingrenzung: string;
  aufEingrenzung: (wert: string) => void;
  nurMitVerkehr: boolean;
  aufNurMitVerkehr: (wert: boolean) => void;
  gewaehlt: string | null;
  /** Durchgereicht an den Baum — siehe dort. */
  springeZurAuswahl: boolean;
  istOffen: (schluessel: string) => boolean;
  aufUmschalten: (schluessel: string) => void;
  aufAuswahl: (processId: string) => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zahl = (wert: number) => formatiereZahl(wert, sprache);

  const sichtbar = sichtbareProzesse(gefiltert);
  const eingeschraenkt = eingrenzung.trim() !== "" || nurMitVerkehr;

  return (
    <div className="flex flex-col gap-2">
      <div className="bg-background sticky -top-4 z-10 flex flex-col gap-2 pt-1 pb-2">
        <div className="flex items-center gap-1">
          <Label htmlFor="baum-eingrenzung" className="sr-only">
            {texte.prozesse.baum.eingrenzung}
          </Label>
          <Input
            id="baum-eingrenzung"
            type="search"
            value={eingrenzung}
            onChange={(ereignis) => aufEingrenzung(ereignis.target.value)}
            placeholder={texte.prozesse.baum.eingrenzung}
            /*
             * **Örtlich und nicht serverseitig.** Die Antwort liegt vollständig
             * vor (E‑33); ein Serverparameter brächte genau die Fallstricke mit,
             * die den Freitextfilter der Liste teuer machen. Deshalb auch keine
             * Entprellung: Es geht keine Anfrage hinaus.
             */
            className="h-bedienelement min-w-0 flex-1"
          />
          {eingrenzung === "" ? null : (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="min-h-bedienelement shrink-0"
              onClick={() => aufEingrenzung("")}
              title={texte.prozesse.baum.eingrenzungLeeren}
            >
              <X aria-hidden="true" />
              <span className="sr-only">{texte.prozesse.baum.eingrenzungLeeren}</span>
            </Button>
          )}
        </div>

        <div className="flex items-center gap-2">
          <Switch
            id="nur-mit-verkehr"
            checked={nurMitVerkehr}
            onCheckedChange={aufNurMitVerkehr}
            aria-describedby="nur-mit-verkehr-hinweis"
          />
          {/*
           * **Die Beschriftung trägt die Berührungsfläche, nicht der Schalter.**
           * Gemessen bei `pointer: coarse`: Der Baustein aus dem Generator ist
           * `h-[18.4px]` hoch und kommt mit seiner `::after`-Vergrößerung auf 32
           * bis 36 px — unter den 44 px, die `docs/visuelles-konzept.md` §5
           * „nirgends unterschritten" nennt. Am Generatorbereich wird nichts
           * geändert (`components/ui` ist seiner).
           *
           * Die Beschriftung schaltet über `htmlFor` denselben Schalter. Mit
           * `min-h-beruehrung` ist damit ein Ziel von voller Zeilenhöhe da, und
           * die Regel hält an der Stelle, an der ein Finger sie braucht.
           */}
          <Label
            htmlFor="nur-mit-verkehr"
            className="min-h-beruehrung flex cursor-pointer items-center font-normal"
          >
            {texte.prozesse.baum.nurMitVerkehr}
          </Label>
        </div>
        <p id="nur-mit-verkehr-hinweis" className="sr-only">
          {texte.prozesse.baum.nurMitVerkehrHinweis}
        </p>

        {/*
         * Die drei Zustände nebeneinander — sie sind disjunkt und vollständig,
         * und ihre Summe ist die Prozesszahl. Die Zahlen kommen aus `gesamt` und
         * beschreiben den **ganzen** Mandanten; was die Eingrenzung übrig lässt,
         * steht in der Zeile darunter und wird nicht damit vermischt.
         */}
        <p className="text-muted-foreground text-beiwerk">
          {einsetzen(texte.prozesse.baum.verteilung, {
            prozesse: zahl(baum.gesamt.anzahlProzesse),
            bewegt: zahl(baum.gesamt.bewegt),
            still: zahl(baum.gesamt.still),
            nie: zahl(baum.gesamt.nie),
          })}
          {eingeschraenkt ? (
            <>
              {" · "}
              {einsetzen(texte.prozesse.baum.gezeigt, {
                sichtbar: zahl(sichtbar),
                gesamt: zahl(baum.gesamt.anzahlProzesse),
              })}
            </>
          ) : null}
        </p>
      </div>

      {gefiltert.length === 0 ? (
        <p className="text-muted-foreground px-1 py-2">{texte.prozesse.baum.keineTreffer}</p>
      ) : (
        <ProzessBaum
          partner={gefiltert}
          stilleSchwelleMonate={baum.stilleSchwelleMonate}
          gewaehlt={gewaehlt}
          springeZurAuswahl={springeZurAuswahl}
          istOffen={istOffen}
          aufUmschalten={aufUmschalten}
          aufAuswahl={aufAuswahl}
        />
      )}
    </div>
  );
}

/**
 * Der Kopf der rechten Spalte.
 *
 * **Er nennt den Prozess, nicht die Nachrichtenzahl.** Die steht links im Baum,
 * und zweimal dieselbe Zahl an zwei Orten ist der Anfang zweier Zahlen.
 *
 * **„Zurück zum Baum" erscheint nur unter `md`.** Darüber steht der Baum
 * daneben, und eine Schaltfläche, die nichts Sichtbares ändert, verspricht
 * etwas, das sie nicht hält — dieselbe Überlegung wie beim Ansichtsumschalter
 * des Nachrichtendetails.
 */
function Kopf({
  prozess,
  processId,
  zuordnung,
  fenster,
  zurueckKnopf,
  aufZurueck,
}: {
  /**
   * `undefined` heißt **nicht gefunden**, `processName: null` heißt **gefunden,
   * trägt aber keinen Namen**. Die beiden fallen ausdrücklich nicht zusammen
   * (Regel Q4) — deshalb bekommt der Kopf den Knoten und keinen flachgeklopften
   * Namen.
   */
  prozess: Prozessknoten | undefined;
  processId: string;
  zuordnung: { partner: string | null; richtung: string | null } | undefined;
  fenster: Fenster;
  zurueckKnopf: React.RefObject<HTMLButtonElement | null>;
  aufZurueck: () => void;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  return (
    <div className="flex flex-col gap-1">
      <Button
        ref={zurueckKnopf}
        type="button"
        variant="ghost"
        onClick={aufZurueck}
        className="min-h-beruehrung -ms-2 w-fit md:hidden"
      >
        <ArrowLeft aria-hidden="true" />
        {texte.prozesse.liste.zurueckZumBaum}
      </Button>

      <h2 className="font-medium break-words">
        {/*
         * **Drei Fälle, und die Ansicht behauptet in keinem mehr, als sie
         * weiß.** Ein Prozess ohne Namen bekommt keinen erfundenen; eine
         * Kennung, die im Baum nicht vorkommt — ein geteilter Link aus einem
         * anderen Mandanten —, bekommt **nicht** den Satz „ohne Namen", denn
         * über ihren Namen ist gar nichts bekannt. Dort steht die Kennung
         * selbst.
         */}
        {prozess === undefined ? (
          <span className="font-mono break-all">{processId}</span>
        ) : (
          (prozess.processName ?? texte.prozesse.ohneNamen)
        )}
      </h2>

      <p className="text-muted-foreground text-beiwerk break-words">
        {zuordnung === undefined ? (
          texte.prozesse.liste.nichtGefunden
        ) : (
          <>
            {partnertext(zuordnung.partner, texte)}
            {" · "}
            {richtungstext(zuordnung.richtung, texte)}
          </>
        )}
        {" · "}
        {/*
         * **Der Zeitraum steht da, und zwar als Zeitpunkte.** Die Liste
         * darunter zeigt nicht „30 Tage", sondern genau das Fenster, das der
         * Baum gelesen hat (E‑50) — und die beiden Zahlen links und rechts
         * meinen dann nachweislich denselben Ausschnitt.
         */}
        {einsetzen(texte.prozesse.liste.fenster, {
          von: formatiereZeitpunkt(fenster.von, sprache, zone),
          bis: formatiereZeitpunkt(fenster.bis, sprache, zone),
        })}
      </p>
    </div>
  );
}

/**
 * Die Übertragungsliste rechts — **die Nachrichtenliste, verdrahtet und nicht
 * nachgebaut.**
 *
 * Dieselbe Tabelle, dasselbe Blättern, derselbe Prozessfilter (§1 von
 * `docs/nachrichtenliste.md`). Was fehlt, fehlt mit Grund: **keine
 * Filterleiste** — Zeitraum und Prozess sind hier gesetzt, und ein zweiter
 * Zeitraumschalter neben dem im Kopf wäre ein zweiter Standardwert.
 *
 * **Die automatische Aktualisierung bleibt** — sie gehört zum Blätterblock, und
 * sie hier wegzunehmen hieße, dieselbe Liste an zwei Orten verschieden zu bauen.
 * Ihr Schalter liegt im Komponentenzustand und nicht in der URL: Er betrifft die
 * Arbeitsweise des Betrachters und nicht den gezeigten Ausschnitt. Dass der
 * Baum daneben seine Zahlen nicht mitzieht, ist die bekannte Folge und steht in
 * `docs/process-view.md` §18.
 *
 * ## Die Sonderregel für die verdeckte Liste ist mit E‑57 entfallen *(02.09.2026)*
 *
 * **Was hier stand und warum:** Unter E‑53 wich die *Liste*, sobald das Panel
 * öffnete — in jeder Breite. Sie blieb montiert, damit ihre Seitenposition
 * stehenbleibt, und lief damit unsichtbar weiter: `useNachrichtenSeite` prüft
 * von sich aus nur `document.hidden`, also die Registerkarte. Alle sechzig
 * Sekunden ging eine Abfrage für eine Liste hinaus, die niemand sah — **und
 * abschalten konnte der Nutzer sie nicht, denn der Schalter steckte in
 * demselben verdeckten Bereich.** Dagegen stand eine zusätzliche Angabe
 * `sichtbar` am Aufruf.
 *
 * **Mit E‑57 weicht der Baum und nicht die Liste.** Ab `xl` steht sie neben dem
 * Panel und ist bedienbar; die Angabe hätte dort nichts mehr zu verhindern. Eine
 * Bedingung stehen zu lassen, deren Grund entfallen ist, wäre schlechter als sie
 * zu entfernen — sie sähe wie eine Regel aus und wäre keine.
 *
 * ⚠️ **Was dabei zurückbleibt, und es ist benannt:** Unter `xl` weicht die Liste
 * weiterhin, und dort kehrt der alte Fall zurück. Er ist damit **derselbe**, den
 * die Nachrichtenliste seit Schritt 5 trägt — dort steht `useNachrichtenSeite`
 * ohne diese Angabe, und die Liste weicht unter `xl` genauso. Ihn hier allein zu
 * behandeln hieße wieder, dieselbe Liste an zwei Orten verschieden zu bauen; ihn
 * über die Fensterbreite zu behandeln hieße, einen zweiten Umbruchpunkt in
 * JavaScript zu führen. Geführt als offener Punkt **121**
 * (`docs/process-view.md` §13).
 *
 * **Eigene Komponente, damit ohne gewählten Prozess keine Abfrage entsteht.**
 * Hooks laufen nicht bedingt; also läuft die Komponente bedingt.
 */
function Uebertragungen({
  filter,
  gewaehlteNachricht,
  aufNachricht,
  aufSortierung,
}: {
  filter: Nachrichtenfilter;
  gewaehlteNachricht: string | null;
  aufNachricht: (messageId: string | null) => void;
  aufSortierung: (sortierung: Sortierung) => void;
}) {
  const texte = useTexte();
  /*
   * **Nicht in der URL** — der Schalter betrifft die Arbeitsweise des
   * Betrachters, nicht den gezeigten Ausschnitt (`docs/nachrichtenliste.md`
   * §8.2). Dieselbe Festlegung wie in der Nachrichtenliste, und derselbe
   * Komponentenzustand.
   */
  const [aktualisierungAn, setAktualisierungAn] = useState(false);
  const liste = useNachrichtenSeite(filter, aktualisierungAn);

  if (liste.fehler !== null && liste.fehler !== undefined) {
    return <Fehler fehler={liste.fehler} aufWiederholen={liste.aktualisiere} />;
  }

  if (liste.laedt && liste.seite === undefined) {
    return <Laden zeilen={8} />;
  }

  const zeilen = liste.seite?.items ?? [];

  if (zeilen.length === 0) {
    return (
      <Leer
        titel={texte.prozesse.liste.leerImZeitraumTitel}
        hinweis={texte.prozesse.liste.leerImZeitraum}
      />
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {/*
       * Dieselbe Hülle wie in der Nachrichtenliste und in der Belegsuche, Klasse
       * für Klasse. **Ohne `relative`, und das ist geprüft und nicht übersehen:**
       * `components/ui/table.tsx` bringt seinen eigenen `relative`-Container mit,
       * und die `sr-only`-Spannen der Tabelle hängen darin. Ein zweites
       * `relative` hier wäre eine Abweichung von den beiden Nachbarn ohne
       * Wirkung (`docs/frontend-grundlagen.md` §7, dritte Bedingung).
       */}
      <div className="border-border bg-card overflow-x-auto rounded-lg border">
        <NachrichtenTabelle
          zeilen={zeilen}
          sortierung={filter.sortierung ?? "neueste"}
          aufSortierung={aufSortierung}
          gewaehlt={gewaehlteNachricht}
          aufAuswahl={aufNachricht}
        />
      </div>
      <Blaettern
        kannZurueck={liste.kannZurueck}
        kannVor={liste.kannVor}
        aufZurueck={liste.zurueck}
        aufVor={liste.vor}
        aufSeiteEins={liste.aufSeiteEins}
        standVon={liste.standVon}
        laeuft={liste.laeuft}
        aktualisierungAn={aktualisierungAn}
        aufAktualisierung={setAktualisierungAn}
        aufAktualisieren={liste.aktualisiere}
      />
    </div>
  );
}
