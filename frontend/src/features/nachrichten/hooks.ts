"use client";

import { useQuery } from "@tanstack/react-query";
import { useQueryStates } from "nuqs";
import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";

import {
  mitFreiemFenster,
  mitVorwahl,
  ohneZeitfenster,
  type Zeitfensterzustand,
  type Zeitraum,
} from "@/lib/filter";

import {
  NACHRICHTEN_SCHLUESSEL,
  holeMerkmale,
  holeNachrichten,
  holeProzesse,
  type Merkmale,
  type Nachricht,
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

  /**
   * `zwischenschritte` steht ausdrücklich in der URL, **ab dem ersten Rendern**.
   * Was ausgeblendet ist, muss man auch teilen können: Bekäme der Empfänger eines
   * Links den Parameter nicht mit, sähe er dieselbe Ansicht mit anderen Zeilen.
   *
   * Nur beim ersten Rendern und nur, wenn er fehlt — `history: "replace"` legt
   * dafür keinen Eintrag im Verlauf an.
   */
  const nachgetragen = useRef(false);
  useEffect(() => {
    if (nachgetragen.current) {
      return;
    }
    nachgetragen.current = true;
    if (!new URLSearchParams(window.location.search).has("zwischenschritte")) {
      void setzeFilter({ zwischenschritte: filter.zwischenschritte });
    }
  }, [filter.zwischenschritte, setzeFilter]);

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
    setzeZwischenschritte: useCallback(
      (zwischenschritte: boolean) => void setzeFilter({ zwischenschritte }),
      [setzeFilter],
    ),
    setzeSortierung: useCallback(
      (sortierung: Sortierung) => void setzeFilter({ sortierung }),
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

/**
 * Die Merkmale des Bestands. **Noch länger gehalten als die Prozessauswahl** — sie
 * beschreiben den Gesamtbestand eines Mandanten und ändern sich, wenn überhaupt,
 * genau einmal.
 *
 * Das Backend hält den Wert seinerseits eine Stunde; hier zu kurz zu halten hieße
 * nur, dieselbe Antwort öfter über die Leitung zu schicken.
 *
 * Beim Mandantenwechsel wird der gesamte Zwischenspeicher geleert, nicht
 * invalidiert (`lib/zwischenspeicher.ts`) — eine eigene Invalidierung braucht es
 * deshalb auch hier nicht.
 */
const MERKMALE_HALTBARKEIT = 60 * 60 * 1000;

export function useMerkmale() {
  return useQuery<Merkmale>({
    queryKey: NACHRICHTEN_SCHLUESSEL.merkmale,
    queryFn: holeMerkmale,
    staleTime: MERKMALE_HALTBARKEIT,
    gcTime: MERKMALE_HALTBARKEIT,
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
