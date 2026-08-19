"use client";

import { useInfiniteQuery, useQuery, type InfiniteData } from "@tanstack/react-query";
import { useQueryStates } from "nuqs";
import { useCallback, useEffect, useState, useSyncExternalStore } from "react";

import {
  mitFreiemFenster,
  mitVorwahl,
  ohneZeitfenster,
  type Zeitfensterzustand,
  type Zeitraum,
} from "@/lib/filter";

import {
  NACHRICHTEN_SCHLUESSEL,
  holeArtefakte,
  holeArtefaktinhalt,
  holeBamSuche,
  holeBamTypen,
  holeBamWerte,
  holeEigenschaften,
  holeKette,
  holeKettenAbwaerts,
  holeNachrichten,
  holeNachrichtendetail,
  holeProzesse,
  type Artefaktanzeige,
  type Artefaktliste,
  type BamSuchergebnis,
  type BamTyp,
  type BamWerte,
  type Eigenschaft,
  type Kette,
  type Kettenglied,
  type Nachricht,
  type Nachrichtendetail,
  type Prozess,
  type Seite,
} from "./api";
import {
  NACHRICHTEN_PARAMETER,
  alsAbfrage,
  type Nachrichtenfilter,
  type Sortierung,
  type Statusart,
} from "./filter";
import {
  SUCHE_PARAMETER,
  begriffeAus,
  modusAus,
  type Suchbegriff,
  type Suchmodus,
  type Suchzustand,
} from "./suche";

/**
 * **`Escape` schließt die Detailansicht** — an genau einer Stelle, für beide
 * Einhängepunkte.
 *
 * Im Panel entfernt es den Parameter aus der URL, auf der eigenen Route führt es
 * zurück zur Liste. Es ist derselbe Vorgang mit zwei Bedeutungen, und es ist
 * derselbe wie beim Schließen-Knopf — die Taste tut nichts, was der Knopf nicht
 * täte.
 *
 * ## Warum das ein Hook ist und keine zwei `useEffect`
 *
 * Nachgetragen am 10.08.2026, als die eigene Route dieselbe Taste bekam. Die
 * beiden Ausnahmen unten sind der Grund: Ein zweiter Abzug derselben Bedingungen
 * wäre die Stelle, an der eine davon irgendwann fehlt — und dann räumt `Escape`
 * in einem Suchfeld nicht mehr die Eingabe, sondern schließt die Ansicht.
 *
 * ## Warum überhaupt `Escape`
 *
 * Aus der Sichtprüfung am 07.08.2026: Öffnen mit der Tastatur ging von Anfang an
 * — die Zeile ist ein Tabstopp, `Enter` und `Leertaste` öffnen sie. Schließen
 * ging *theoretisch* auch, der Schließen-Knopf steht im DOM hinter der Tabelle;
 * man muss also durch bis zu fünfzig Zeilen tabben. Das erfüllt „mit der Tastatur
 * erreichbar" und verfehlt „mit der Tastatur bedienbar".
 *
 * `Escape` statt eines Fokussprungs ins Panel: Ein Sprung nähme dem Nutzer die
 * Stelle in der Liste, an der er gerade war — und einem Mausnutzer, der nichts
 * davon wollte, ebenso.
 *
 * ## Zwei Ausnahmen, damit die Taste nicht zweierlei tut
 *
 * In einem Eingabefeld räumt `Escape` die Eingabe (ein `type="search"` leert sich
 * nativ), und ein offenes Radix-Auswahlfeld schließt sich damit. Beides bleibt
 * das, was es ist; die Ansicht bleibt dann stehen.
 *
 * @param aktiv ob die Ansicht überhaupt offen ist. Auf der eigenen Route immer,
 *   im Panel nur bei gesetztem Parameter — sonst hinge ein Zuhörer am Dokument,
 *   der nichts zu tun hat.
 */
export function useEscapeSchliesst(aktiv: boolean, aufSchliessen: () => void) {
  useEffect(() => {
    if (!aktiv) {
      return;
    }
    function beiTaste(ereignis: KeyboardEvent) {
      if (ereignis.key !== "Escape" || ereignis.defaultPrevented) {
        return;
      }
      const ziel = ereignis.target as HTMLElement | null;
      if (ziel?.closest("input, textarea, select, [contenteditable='true']")) {
        return;
      }
      if (document.querySelector("[data-radix-popper-content-wrapper]") !== null) {
        return;
      }
      aufSchliessen();
    }
    document.addEventListener("keydown", beiTaste);
    return () => document.removeEventListener("keydown", beiTaste);
  }, [aktiv, aufSchliessen]);
}

/**
 * Der Filterzustand, gebunden an die URL.
 *
 * Die beiden Zeitfenstermodi schließen einander aus, und **das Frontend lässt den
 * verbotenen Zustand gar nicht erst entstehen**: Wer eine Vorwahl wählt, verliert
 * `von`/`bis`; wer ein freies Fenster setzt, verliert `zeitraum`. Beides zugleich
 * wäre `400` `zeitfenster-mehrdeutig` — und ein Nutzer, der über eine Schaltfläche
 * in einen Fehlerzustand gerät, hat keine Möglichkeit, ihn zu verstehen.
 */
export function useNachrichtenfilter() {
  const [filter, setzeFilter] = useQueryStates(NACHRICHTEN_PARAMETER, { history: "replace" });

  /*
   * Hier stand bis zum 11.08.2026 ein Effekt, der `zwischenschritte` beim ersten
   * Rendern in die URL nachtrug — was ausgeblendet ist, muss man teilen können.
   * Die Liste blendet nichts mehr aus; damit ist auch nichts mehr nachzutragen,
   * und die URL bleibt leer, solange der Nutzer nichts eingestellt hat.
   */

  const setzeZeitfenster = useCallback(
    (zustand: Zeitfensterzustand) => void setzeFilter(zustand),
    [setzeFilter],
  );

  return {
    filter: filter as Nachrichtenfilter,
    setzeZeitraum: useCallback(
      (zeitraum: Zeitraum) => setzeZeitfenster(mitVorwahl(zeitraum)),
      [setzeZeitfenster],
    ),
    setzeFreiesFenster: useCallback(
      (von: Date | null, bis: Date | null) => setzeZeitfenster(mitFreiemFenster(von, bis)),
      [setzeZeitfenster],
    ),
    setzeZeitfensterZurueck: useCallback(
      () => setzeZeitfenster(ohneZeitfenster()),
      [setzeZeitfenster],
    ),
    setzeStatus: useCallback(
      (status: Statusart[]) => void setzeFilter({ status: status.length === 0 ? null : status }),
      [setzeFilter],
    ),
    setzeProzesse: useCallback(
      (prozess: string[]) => void setzeFilter({ prozess: prozess.length === 0 ? null : prozess }),
      [setzeFilter],
    ),
    /**
     * Ein neuer Begriff setzt `langeSuche` zurück. Die Grenze wurde für *diese*
     * Suche bewusst aufgehoben; sie stillschweigend über den nächsten Begriff
     * mitzunehmen hieße, eine einmalige Entscheidung dauerhaft zu machen —
     * ausgerechnet bei der Entscheidung, die eine mehrsekündige Abfrage erlaubt.
     */
    setzeSuche: useCallback(
      (suche: string) =>
        void setzeFilter({ suche: suche === "" ? null : suche, langeSuche: false }),
      [setzeFilter],
    ),
    setzeLangeSuche: useCallback(
      (langeSuche: boolean) => void setzeFilter({ langeSuche }),
      [setzeFilter],
    ),
    setzeSortierung: useCallback(
      (sortierung: Sortierung) => void setzeFilter({ sortierung }),
      [setzeFilter],
    ),
    /**
     * Öffnet oder schließt die Detailansicht neben der Liste.
     *
     * **`null` schließt und lässt den übrigen Filterzustand unberührt** — es
     * wird genau ein Parameter entfernt und nicht die URL neu gebaut. Wer das
     * Panel schließt, will seine Liste behalten, wie sie war.
     *
     * Der Verlaufseintrag entsteht am Parser (`history: "push"` in
     * `filter.ts`), nicht hier: Er gehört zum Parameter und nicht zum Aufrufer.
     */
    setzeNachricht: useCallback(
      (messageId: string | null) => void setzeFilter({ nachricht: messageId }),
      [setzeFilter],
    ),
  };
}

/**
 * Die Prozessauswahl. **Länger gehalten als die Liste** — reine Stammdaten, die
 * sich selten ändern, neben einer Liste, die sich unter Umständen jede Minute
 * aktualisiert.
 *
 * Beim Mandantenwechsel wird der gesamte Zwischenspeicher geleert, nicht
 * invalidiert (`lib/zwischenspeicher.ts`); eine eigene Invalidierung braucht es
 * hier deshalb nicht.
 */
const PROZESSE_HALTBARKEIT = 15 * 60 * 1000;

export function useProzesse() {
  return useQuery<Prozess[]>({
    queryKey: NACHRICHTEN_SCHLUESSEL.prozesse,
    queryFn: holeProzesse,
    staleTime: PROZESSE_HALTBARKEIT,
    gcTime: PROZESSE_HALTBARKEIT,
  });
}

/*
 * Hier stand bis zum 11.08.2026 `useMerkmale` — die Abfrage auf
 * `/api/nachrichten/merkmale` samt ihrem eigenen, besonders langen
 * Zwischenspeicher. Sie beantwortete genau eine Frage: ob der Ausblende-Schalter
 * erscheinen soll. Endpunkt und Schalter sind gemeinsam entfallen
 * (`docs/nachrichtenliste.md` §5).
 */

/**
 * Das Detail **einer** Nachricht.
 *
 * **Es hängt nicht am Ergebnis der Liste.** Geladen wird über die Kennung, und
 * zwar auch dann, wenn die Nachricht außerhalb des gewählten Zeitfensters liegt
 * und die Liste dahinter leer ist. Das ist gewollt: Ein tiefer Link auf einen
 * Beleg ist die eigentliche Anwendung dieser Ansicht — „schick mir mal den
 * Link" —, und der Empfänger hat das Zeitfenster des Absenders nicht.
 *
 * Ein `404` wird nicht wiederholt (`lib/query-client.ts`): Fremd und nicht
 * vorhanden sind ununterscheidbar, und beides steht schon beim ersten Aufruf
 * fest.
 *
 * @param messageId `null`, solange nichts gewählt ist — dann läuft keine Abfrage.
 */
export function useNachrichtendetail(messageId: string | null) {
  return useQuery<Nachrichtendetail>({
    // Der Schlüssel trägt die leere Kennung nie: Bei `null` ist die Abfrage aus,
    // und ein Eintrag dafür entstünde gar nicht erst.
    queryKey: NACHRICHTEN_SCHLUESSEL.detail(messageId ?? ""),
    queryFn: () => holeNachrichtendetail(messageId as string),
    enabled: messageId !== null && messageId !== "",
  });
}

/**
 * Die technischen Eigenschaften — **erst beim Aufklappen.**
 *
 * Der Kopf trägt die Anzahl, der Block ist damit beschriftbar, ohne ihn zu
 * laden. Genau dafür gibt es den zweiten Endpunkt; ihn mitzuladen nähme ihm
 * seinen Zweck.
 *
 * Länger gehalten als die Liste: Die Eigenschaften einer abgeschlossenen
 * Nachricht ändern sich nicht mehr, und wer zwischen zwei Nachrichten hin und
 * her springt, soll nicht zweimal dieselbe Antwort holen.
 *
 * @param aktiv der Schalter des Blocks. Bewusst ein Parameter: Der Zustand
 *   gehört der Komponente, nicht der Abfrage — und **nicht der URL**, denn er
 *   ist keine Ansicht, die jemand teilt.
 */
export function useEigenschaften(messageId: string | null, aktiv: boolean) {
  return useQuery<Eigenschaft[]>({
    queryKey: NACHRICHTEN_SCHLUESSEL.eigenschaften(messageId ?? ""),
    queryFn: () => holeEigenschaften(messageId as string),
    enabled: aktiv && messageId !== null && messageId !== "",
    staleTime: EIGENSCHAFTEN_HALTBARKEIT,
    gcTime: EIGENSCHAFTEN_HALTBARKEIT,
  });
}

const EIGENSCHAFTEN_HALTBARKEIT = 15 * 60 * 1000;

/**
 * Die Belegdaten einer Nachricht — **erst beim Aufklappen.**
 *
 * Dieselbe Bauform wie bei den technischen Eigenschaften, aus demselben Grund:
 * Der Kopf trägt mit `bamAnzahl` die Zahl, der Block ist damit beschriftbar,
 * ohne ihn zu laden.
 *
 * **Und dieselbe Bauform wie beim Kettenblock, aus einem zweiten Grund:** Ist
 * `bamAnzahl` null, gibt es den Block gar nicht — dann steht `aktiv` nie auf
 * wahr, und es entsteht **keine Anfrage**. Bei 80,6 Prozent der Nachrichten ist
 * das der Fall, bei Merge-Eingängen bei allen (M41).
 *
 * Länger gehalten als die Liste: Die Belegnummern einer abgeschlossenen
 * Nachricht ändern sich nicht mehr, und wer zwischen zwei Nachrichten hin und
 * her springt, soll nicht zweimal dieselbe Antwort holen.
 *
 * @param aktiv der Schalter des Blocks. Bewusst ein Parameter: Der Zustand
 *   gehört der Komponente, nicht der Abfrage — und **nicht der URL**, denn er
 *   ist keine Ansicht, die jemand teilt.
 */
export function useBamWerte(messageId: string | null, aktiv: boolean) {
  return useQuery<BamWerte>({
    queryKey: NACHRICHTEN_SCHLUESSEL.bam(messageId ?? ""),
    queryFn: () => holeBamWerte(messageId as string),
    enabled: aktiv && messageId !== null && messageId !== "",
    staleTime: BAM_HALTBARKEIT,
    gcTime: BAM_HALTBARKEIT,
  });
}

const BAM_HALTBARKEIT = 15 * 60 * 1000;

/**
 * Die Belegarten zur Auswahl neben dem Suchfeld.
 *
 * **Länger gehalten als jede Liste** — reine Stammdaten des Mandanten, die sich
 * ohne Zutun des Altsystems nicht ändern. Beim Mandantenwechsel wird der gesamte
 * Zwischenspeicher geleert, nicht invalidiert (`lib/zwischenspeicher.ts`); eine
 * eigene Invalidierung braucht es deshalb nicht.
 *
 * **Sie lädt auf jeder Seite**, weil das Feld in der Kopfzeile steht — einmal je
 * Sitzung und Mandant. Der Aufruf liest zwei Stammdatentabellen mit zusammen 131
 * Zeilen und kostet gemessen 0,53 Millisekunden (M48).
 */
export function useBamTypen() {
  return useQuery<BamTyp[]>({
    queryKey: NACHRICHTEN_SCHLUESSEL.bamTypen,
    queryFn: holeBamTypen,
    staleTime: BAM_HALTBARKEIT,
    gcTime: BAM_HALTBARKEIT,
  });
}

/**
 * Der Zustand der Belegsuche, gebunden an die URL.
 *
 * **`history: "replace"`**, wie bei jeder Filterleiste: Eine Marke, die man
 * hinzufügt oder wegnimmt, ist keine Station, zu der man zurückgeht. Der
 * Parameter `nachricht` bringt sein `push` am Parser mit (`filter.ts`) und wird
 * davon nicht berührt — am schmalen Fenster ist das Zurück des Browsers der Weg
 * aus der geöffneten Nachricht heraus.
 */
export function useSuchzustand() {
  const [zustand, setzeZustand] = useQueryStates(SUCHE_PARAMETER, { history: "replace" });

  return {
    zustand: zustand as Suchzustand,
    begriffe: begriffeAus(zustand as Suchzustand),
    /** Der Modus **der URL** — nicht der der Antwort. Die beiden fallen auseinander, solange geladen wird. */
    modus: modusAus(zustand as Suchzustand),
    /**
     * **Jede Änderung an den Begriffen setzt auf `exakt` zurück.**
     *
     * Wer eine Marke hinzufügt oder wegnimmt, stellt eine **neue Frage**, und die
     * wird zuerst genau beantwortet — sonst liefe die teuerste Zugriffsform
     * dieses Projekts (M50) unbemerkt weiter, obwohl der Anlass für sie, das
     * leere Ergebnis, gar nicht mehr gilt. **Das Zeitfenster bleibt dabei, wie es
     * ist**; es beschreibt den Ausschnitt und nicht die Frage.
     */
    setzeBegriffe: useCallback(
      (begriffe: Suchbegriff[]) =>
        void setzeZustand({ begriff: begriffe.length === 0 ? null : begriffe, modus: null }),
      [setzeZustand],
    ),
    /** `null`/`null` heißt „Vorgabe des Servers" — und nicht „30 Tage" (Regel L1). */
    setzeFenster: useCallback(
      (von: Date | null, bis: Date | null) => void setzeZustand({ von, bis }),
      [setzeZustand],
    ),
    /**
     * Der Vergleichsmodus, samt dem Fenster, über das er laufen soll.
     *
     * **`null` als Modus ist der Rückweg auf „genau suchen"** — der Parameter
     * verschwindet aus der URL, weil `exakt` die Vorgabe ist.
     *
     * **`fenster` ist `null`, wenn sich am Zeitfenster nichts ändert**, und dann
     * wird auch nichts geschrieben: Es bleibt beim gewählten Ausschnitt, und war
     * keiner gewählt, bleibt die Vorgabe des Backends die Vorgabe des Backends.
     * Das ist der Unterschied zwischen „ändert einen Wert" und „schreibt
     * denselben Wert noch einmal hin" — der zweite Fall machte aus einer
     * Servervorgabe stillschweigend einen eigenen Zeitpunkt in der URL.
     *
     * **Beim Rückweg wird das Fenster nicht zurückgesetzt.** Wer aus einem
     * Jahresfenster in den Präfixmodus gegangen ist, kommt mit dreißig Tagen
     * zurück und sieht sie. Das ist ein bewusst in Kauf genommener Nachteil und
     * kein Versehen: Ein Wert, der sich beim Moduswechsel von selbst änderte,
     * wäre versteckter Zustand — und der Zustand steht hier vollständig in der
     * URL (`docs/bam-suche.md` §23).
     */
    setzeModus: useCallback(
      (modus: Suchmodus | null, fenster: { von: Date; bis: Date } | null) =>
        void setzeZustand(fenster === null ? { modus } : { modus, ...fenster }),
      [setzeZustand],
    ),
    /**
     * Öffnet oder schließt die Nachricht neben der Trefferliste.
     *
     * **`null` schließt und lässt den übrigen Zustand unberührt** — es wird genau
     * ein Parameter entfernt und nicht die URL neu gebaut. Der Verlaufseintrag
     * entsteht am Parser (`history: "push"` in `filter.ts`) und nicht hier.
     */
    setzeNachricht: useCallback(
      (messageId: string | null) => void setzeZustand({ nachricht: messageId }),
      [setzeZustand],
    ),
  };
}

/**
 * Eine Belegsuche.
 *
 * **Ohne Begriff läuft keine Abfrage.** Den Endpunkt gibt es ohne Suchbegriff
 * nicht; ein Aufruf ohne wäre ein garantiertes `400`, und die Ansicht zeigt
 * stattdessen ihren Leerzustand.
 *
 * **Kein zweiter Versuch bei `suche-abgebrochen`** — das entscheidet
 * `lib/query-client.ts` an einer Stelle für die ganze Anwendung: Dieselbe Abfrage
 * liefe noch einmal in dieselbe Zeitgrenze und kostete weitere Sekunden auf der
 * Produktionsdatenbank.
 *
 * **Nicht länger gehalten als die Vorgabe.** Anders als Belegdaten und Kette
 * beschreibt eine Suche keinen festen Gegenstand, sondern ein Zeitfenster — und
 * das wandert mit der Anwendungsuhr weiter, sobald es die Vorgabe ist.
 */
export function useBamSuche(abfrage: string, aktiv: boolean) {
  return useQuery<BamSuchergebnis>({
    queryKey: NACHRICHTEN_SCHLUESSEL.bamSuche(abfrage),
    queryFn: () => holeBamSuche(abfrage),
    enabled: aktiv,
  });
}

/**
 * Die Kette einer Nachricht — **nur, wenn sie eine hat.**
 *
 * `aktiv` kommt aus `detail.rollen`: Ist die Liste leer, steht die Nachricht in
 * keiner Kette, und es entsteht **keine Anfrage**. Rund 60 Prozent aller Zeilen
 * sind das. Die Auskunft kostet nichts — die vier Verkettungsspalten stehen auf
 * der `Message`-Zeile, die der Detail-Endpunkt ohnehin liest (E4).
 *
 * Länger gehalten als die Liste, aus demselben Grund wie die Eigenschaften: Die
 * Kette einer abgeschlossenen Nachricht ändert sich nicht mehr, und wer
 * zwischen zwei Gliedern hin und her springt, soll nicht zweimal dieselbe
 * Antwort holen.
 */
export function useKette(messageId: string | null, aktiv: boolean) {
  return useQuery<Kette>({
    queryKey: NACHRICHTEN_SCHLUESSEL.kette(messageId ?? ""),
    queryFn: () => holeKette(messageId as string),
    enabled: aktiv && messageId !== null && messageId !== "",
    staleTime: KETTE_HALTBARKEIT,
    gcTime: KETTE_HALTBARKEIT,
  });
}

const KETTE_HALTBARKEIT = 15 * 60 * 1000;

/**
 * Die Artefakte einer Nachricht — **seit dem 18.08.2026 mit dem Detail, nicht
 * mehr erst beim Aufklappen.**
 *
 * Sie hing bis dahin am Schalter eines eigenen Blocks, dieselbe Bauform wie bei
 * Belegdaten und Eigenschaften. **Den Block gibt es nicht mehr:** Die Artefakte
 * hängen als Ziele an den Zeilen der Zeitleiste, und die steht immer da
 * (`docs/rohdaten-frontend.md` §3). Ein Schalter, der erst geladen hätte,
 * bliebe ohne Bedienelement.
 *
 * **Der Preis ist eine Anfrage je Detailaufruf, und er ist gemessen klein:**
 * 0,867 ms als reine Datenbankabfrage (`docs/rohdaten-backend.md` §9). Vor allem
 * aber **spricht sie keine Ablage an** — sie liest `MessageProperty`. Der
 * SOAP-Aufruf gegen den Filestore steckt allein im *Inhalt* eines Artefakts
 * (38 bis 244 ms je Datei, M66/M60), und der wird weiterhin erst beim Öffnen der
 * Ansicht geholt. Genau diese Trennung ist der Grund, warum das hier vertretbar
 * ist und dort nicht.
 *
 * **Derselbe Schlüssel wie in der Ansicht.** Wer aus dem Detail heraus eine
 * Datei öffnet, holt die Liste kein zweites Mal — und die Beschriftung dort ist
 * dieselbe Zeichenkette wie am Ziel, aus dem er kam.
 *
 * Länger gehalten als die Liste: Welche Artefakte an einer abgeschlossenen
 * Nachricht hängen, ändert sich nicht mehr.
 *
 * @param aktiv bleibt ein Parameter. Das Detail reicht `true` durch; die Ansicht
 *   auf der eigenen Route tut dasselbe. Er steht weiterhin hier, weil die
 *   Entscheidung *ob geladen wird* dem Aufrufer gehört und nicht der Abfrage.
 */
export function useArtefakte(messageId: string | null, aktiv: boolean) {
  return useQuery<Artefaktliste>({
    queryKey: NACHRICHTEN_SCHLUESSEL.dateien(messageId ?? ""),
    queryFn: () => holeArtefakte(messageId as string),
    enabled: aktiv && messageId !== null && messageId !== "",
    staleTime: ARTEFAKTE_HALTBARKEIT,
    gcTime: ARTEFAKTE_HALTBARKEIT,
  });
}

const ARTEFAKTE_HALTBARKEIT = 15 * 60 * 1000;

/**
 * Der Inhalt **eines** Artefakts.
 *
 * **Er wird erst beim Öffnen der Ansicht geholt, nicht mit der Liste.** Hinter
 * jedem Abruf steht ein SOAP-Aufruf gegen die Ablage — 38 bis 244 ms je Datei
 * (M66, M60) —, und eine Nachricht trägt bis zu fünfzehn Artefakte. Die Liste
 * vorzuladen hieße, fünfzehn fremde Anlagen zu befragen, um drei Zeilen
 * anzuzeigen.
 *
 * **Nicht länger gehalten als die Vorgabe.** Anders als die Artefaktliste
 * beschreibt der Inhalt keinen Datenbankstand, sondern das Ergebnis eines
 * Abrufs bei einer fremden Anlage: *Ablage nicht erreichbar* ist ein
 * Betriebszustand, der sich in einer Minute geändert haben kann. Ihn zu halten
 * hieße, einen vorübergehenden Ausfall für eine Viertelstunde festzuschreiben.
 *
 * Ein `404` wird nicht wiederholt (`lib/query-client.ts`): Eine unbekannte
 * Nachricht, ein fremder Mandant und eine unbrauchbare Kennung sind
 * ununterscheidbar, und alle drei stehen beim ersten Aufruf fest.
 */
export function useArtefaktinhalt(messageId: string, artefaktId: string) {
  return useQuery<Artefaktanzeige>({
    queryKey: NACHRICHTEN_SCHLUESSEL.dateiInhalt(messageId, artefaktId),
    queryFn: () => holeArtefaktinhalt(messageId, artefaktId),
    enabled: messageId !== "" && artefaktId !== "",
  });
}

/**
 * Die nachgeladenen Seiten der Abwärtsglieder — **cursor-basiert, und erst auf
 * Verlangen.**
 *
 * ## Es beginnt hinter dem, was schon dasteht
 *
 * `/kette` liefert die ersten fünfzig Abwärtsglieder **und mit
 * `abwaertsCursor` die Position dahinter**. Die erste hier geholte Seite ist
 * deshalb die *zweite* — ein Klick bringt fünfzig neue Zeilen, in **einer**
 * Anfrage.
 *
 * Bis zum 11.08.2026 war das ein Umweg: `/kette` lieferte keinen Cursor, der
 * Block holte die erste Seite ein zweites Mal, nur um eine Position zu
 * bekommen, und zog die zweite sofort nach — zwei Anfragen für einen Klick.
 * Der Nachzug in `components/kette-block.tsx` ist mit dem Feld ersatzlos
 * entfallen.
 *
 * @param aktiv der Schalter des Blocks. Bewusst ein Parameter: Der Zustand
 *   gehört der Komponente, nicht der Abfrage — und **nicht der URL**, denn er
 *   beschreibt keine Ansicht, die jemand teilt.
 * @param abCursor `kette.abwaertsCursor`. Er wird gelesen, wenn die erste
 *   Anfrage läuft — und die läuft erst, wenn `aktiv` wahr ist, also nachdem die
 *   Kette da war.
 */
export function useKettenAbwaerts(
  messageId: string | null,
  aktiv: boolean,
  abCursor: string | null,
) {
  return useInfiniteQuery<
    Seite<Kettenglied>,
    unknown,
    InfiniteData<Seite<Kettenglied>>,
    ReturnType<typeof NACHRICHTEN_SCHLUESSEL.kettenAbwaerts>,
    string | null
  >({
    queryKey: NACHRICHTEN_SCHLUESSEL.kettenAbwaerts(messageId ?? ""),
    queryFn: ({ pageParam }) => holeKettenAbwaerts(messageId as string, pageParam),
    initialPageParam: abCursor,
    // `nextCursor` ist undurchsichtig und wird nicht auseinandergenommen — er
    // geht zurück, wie er kam.
    getNextPageParam: (letzte) => letzte.nextCursor,
    enabled: aktiv && messageId !== null && messageId !== "",
    staleTime: KETTE_HALTBARKEIT,
    gcTime: KETTE_HALTBARKEIT,
  });
}

/** Intervall der automatischen Aktualisierung. */
export const AKTUALISIERUNG_INTERVALL_MS = 60_000;

/**
 * Ob der Tab gerade sichtbar ist.
 *
 * **Ohne diese Bedingung vervielfacht ein offengelassener Browser die Last auf der
 * Produktionsdatenbank.** Ein Fenster, das über Nacht auf der Liste steht, stellt
 * sonst 480 Abfragen — für niemanden, der hinsieht.
 */
function useSichtbar(): boolean {
  return useSyncExternalStore(
    (melde) => {
      document.addEventListener("visibilitychange", melde);
      return () => document.removeEventListener("visibilitychange", melde);
    },
    () => !document.hidden,
    // Auf dem Server gibt es kein `document`; dort gilt „sichtbar", damit der
    // erste Rendervorgang nicht davon abhängt.
    () => true,
  );
}

export type Listenzustand = {
  seite: Seite<Nachricht> | undefined;
  /**
   * Die letzte Seite, die tatsächlich geliefert wurde — auch dann noch, wenn die
   * aktuelle Anfrage gescheitert ist.
   *
   * Sie existiert für genau einen Fall: eine Rückmeldung, die dem **Suchfeld**
   * gilt und nicht der Ansicht. Wer bei stehender Liste einen Suchbegriff tippt,
   * der über der Fenstergrenze liegt, bekommt einen Hinweis am Feld — und soll
   * dabei sehen, was er vorher gesehen hat. Ohne diesen Rückgriff verschwände die
   * Liste, weil die neue Abfrage einen eigenen Schlüssel hat und für den nie
   * Daten ankamen.
   */
  letzteSeite: Seite<Nachricht> | undefined;
  /** Ab wann die Zeilen stehen — sichtbar, auch bei manueller Bedienung. */
  standVon: number;
  /** Erster Aufbau: noch keine Zeilen da. Der Zustand „Laden". */
  laedt: boolean;
  /** Es läuft eine Abfrage, aber es stehen schon Zeilen — kein Skelett, nur ein Hinweis. */
  laeuft: boolean;
  fehler: unknown;
  aufSeiteEins: boolean;
  kannVor: boolean;
  kannZurueck: boolean;
  vor: () => void;
  zurueck: () => void;
  aktualisiere: () => void;
};

/**
 * Eine Seite der Liste, samt Blättern und automatischer Aktualisierung.
 *
 * ## Blättern ohne Seitenzahlen
 *
 * Vorwärts über `nextCursor`, rückwärts über einen **Stapel im
 * Komponentenzustand** — nicht über einen zweiten Cursor vom Server. Der Server
 * müsste dafür die Gegenrichtung mitrechnen, und der Aufrufer weiß ohnehin, wo er
 * herkam.
 *
 * **Keine Seitenzahlen.** Es gibt keine Gesamtzahl (Regel L2 verbietet den
 * `COUNT`), und eine erfundene wäre schlimmer als keine.
 *
 * ## Die automatische Aktualisierung ist eng gefasst
 *
 * - **Standardmäßig aus.** Das Werkzeug wird geöffnet, wenn etwas nicht stimmt;
 *   eine Liste, die unter den Händen springt, hilft dabei nicht.
 * - **Nur auf Seite eins.** Sobald geblättert wurde, pausiert sie — sonst zeigt
 *   die Ansicht plötzlich einen anderen Ausschnitt, oder der Cursor liegt außerhalb
 *   des Fensters und die Antwort ist `400`.
 * - **Nur bei sichtbarem Tab.**
 *
 * @param aktualisierungAn Schalterzustand. Bewusst ein Parameter und nicht in
 *   diesem Hook gehalten: Er gehört der Oberfläche, nicht der Abfrage — und er
 *   steht nicht in der URL, weil er die Arbeitsweise des Betrachters betrifft und
 *   nicht den gezeigten Ausschnitt.
 */
export function useNachrichtenSeite(
  filter: Nachrichtenfilter,
  aktualisierungAn: boolean,
): Listenzustand {
  const [stapel, setStapel] = useState<string[]>([]);
  const sichtbar = useSichtbar();

  const grundabfrage = alsAbfrage(filter);

  /*
   * Ein geänderter Filter beginnt wieder auf Seite eins: Der Cursor der alten
   * Abfrage trägt einen Zeitpunkt, der im neuen Fenster nichts zu suchen hat —
   * das Backend antwortet darauf mit `cursor-ungueltig`.
   *
   * Angepasst **während des Renderns** und nicht in einem Effekt. Ein Effekt
   * liefe erst nach dem Malen: Die Ansicht stellte für einen Durchgang die alte
   * Seitenposition im neuen Filter, also garantiert eine Fehlerantwort. Hier
   * verwirft React den begonnenen Durchlauf und rendert sofort neu — der falsche
   * Zustand erscheint nie auf dem Bildschirm.
   *
   * `aktuellerStapel` statt `stapel`, damit auch der verworfene Durchlauf schon
   * mit der zurückgesetzten Position rechnet und keine Abfrage mit dem alten
   * Cursor anstößt.
   */
  const [vorherigeAbfrage, setVorherigeAbfrage] = useState(grundabfrage);
  let aktuellerStapel = stapel;
  if (vorherigeAbfrage !== grundabfrage) {
    setVorherigeAbfrage(grundabfrage);
    setStapel([]);
    aktuellerStapel = [];
  }

  const abfrage = alsAbfrage(filter, aktuellerStapel.at(-1) ?? null);
  const aufSeiteEins = aktuellerStapel.length === 0;

  const anfrage = useQuery<Seite<Nachricht>>({
    queryKey: NACHRICHTEN_SCHLUESSEL.liste(abfrage),
    queryFn: () => holeNachrichten(abfrage),
    refetchInterval:
      aktualisierungAn && aufSeiteEins && sichtbar ? AKTUALISIERUNG_INTERVALL_MS : false,
    // Zweite Sicherung gegen den offengelassenen Browser: Auch wenn oben etwas
    // durchrutschte, läuft im Hintergrund kein Intervall.
    refetchIntervalInBackground: false,
  });

  const seite = anfrage.data;

  /*
   * Kein `placeholderData`: Das hielte die alte Seite bei *jedem* Filterwechsel
   * stehen und nähme dem Nutzer die Rückmeldung, dass gerade neu geladen wird.
   * Hier geht es um einen einzigen Fall, und wer ihn braucht, holt sich den
   * Rückgriff ausdrücklich.
   *
   * Angepasst **während des Renderns** und nicht in einem Effekt — dasselbe
   * Muster wie beim Zurücksetzen des Seitenstapels weiter oben: React verwirft
   * den begonnenen Durchlauf und rendert sofort neu, der Zwischenstand erscheint
   * nie auf dem Bildschirm.
   */
  const [letzteSeite, setLetzteSeite] = useState<Seite<Nachricht> | undefined>(undefined);
  if (seite !== undefined && seite !== letzteSeite) {
    setLetzteSeite(seite);
  }

  const nachladen = anfrage.refetch;

  return {
    seite,
    letzteSeite: seite ?? letzteSeite,
    standVon: anfrage.dataUpdatedAt,
    laedt: anfrage.isPending,
    laeuft: anfrage.isFetching && !anfrage.isPending,
    fehler: anfrage.error,
    aufSeiteEins,
    kannVor: seite?.hasMore === true && seite.nextCursor !== null,
    kannZurueck: !aufSeiteEins,
    vor: useCallback(() => {
      const naechster = seite?.nextCursor;
      if (naechster) {
        setStapel((bisher) => [...bisher, naechster]);
      }
    }, [seite?.nextCursor]),
    zurueck: useCallback(() => setStapel((bisher) => bisher.slice(0, -1)), []),
    aktualisiere: useCallback(() => void nachladen(), [nachladen]),
  };
}
