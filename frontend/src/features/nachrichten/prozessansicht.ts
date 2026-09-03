import { createParser, parseAsBoolean } from "nuqs";

import { NACHRICHT_PARAMETER } from "@/lib/routen";
import {
  istRollupzeitraum,
  parseAsRollupzeitraum,
  type Rollupzeitraum,
} from "@/lib/rollupzeitraum";

import type { Fenster } from "./api";
import {
  LANGE_SUCHE_VORGABE,
  UEBERFAELLIG_VORGABE,
  istSortierung,
  parseAsSortierung,
  type Nachrichtenfilter,
  type Sortierung,
} from "./filter";

/**
 * Der URL-Zustand der **Prozessansicht** — `/prozesse`.
 *
 * ```
 * /prozesse?zeitraum=30T&prozess=<ProcessID>&nachricht=<MessageID>&nurMitVerkehr=true
 * ```
 *
 * **Frei von React**, wie `filter.ts` und `suche.ts`: Die Umrechnung Zustand →
 * URL und Zustand → Anfrage ist eine reine Funktion und wird als solche geprüft
 * (`tests/prozessansicht.test.ts`).
 *
 * ## Was hier **nicht** steht, und warum
 *
 * **Der aufgeklappte Partner.** Er ergibt sich aus dem gewählten Prozess
 * (`prozessbaum.ts` {@link pfadZuProzess}). Zwei Zustände für dieselbe Sache
 * liefen auseinander, und ein Link mit `prozess=…` und einem widersprechenden
 * Aufklappzustand wäre nicht mehr zu deuten.
 *
 * **Die Eingrenzung des Baums.** Sie ist eine **Eingabe** und kein Ausschnitt —
 * dieselbe Bauform wie das Eingrenzungsfeld der Prozessauswahl
 * (`components/prozess-filter.tsx`), das seit Schritt 4 im Komponentenzustand
 * liegt. Der Zweischritt aus `docs/frontend-grundlagen.md` §8 endet hier bei
 * Punkt 2: Sie beschreibt, was jemand gerade tippt, und der Baum darunter zeigt
 * dieselbe Antwort. **Der Schalter darunter ist der Gegenfall** — siehe
 * {@link PROZESSANSICHT_PARAMETER}.
 *
 * **Die Seitenposition der Liste rechts.** Kein Cursor in der URL, aus
 * demselben Grund wie in der Nachrichtenliste: Ein Link auf Seite sieben eines
 * Fensters zeigte beim Empfänger auf andere Zeilen.
 */

/**
 * `nurMitVerkehr` blendet Prozesse ohne Nachricht im Zeitraum aus. **Vorgabe
 * aus.**
 *
 * Bei `VOTG` sind 89,7 % der Prozesse „nie" (M111) — ohne den Schalter ist der
 * Baum dort fast vollständig gedämpft. **Mit Vorgabe *an* wäre dagegen genau der
 * Prozess unauffindbar, den jemand sucht, *weil* er nichts trägt**
 * (`docs/prozessauswahl.md` §3), und das ist der Fall, für den es diese Ansicht
 * gibt.
 */
export const NUR_MIT_VERKEHR_VORGABE = false;

/**
 * Eine Kennung aus der URL — **eine leere ist keine Auswahl.**
 *
 * ## ⚠️ Warum das ein eigener Parser ist und keine Prüfung daneben
 *
 * `parseAsString` reicht den Wert durch, und `nuqs` hält einen **leeren String
 * nicht für abwesend**: `/prozesse?prozess=` ergibt `""` und nicht `null`. Eine
 * von Hand gebaute oder abgeschnittene URL brächte die Ansicht damit in einen
 * Zustand, den es nicht geben soll — `etwasGewaehlt` wäre wahr, der Baum wiche
 * unter `md`, und {@link listenfilter} baute einen Filter mit `prozess: [""]`.
 * **Das Backend wirft leere Werte weg**, und ein leerer Prozessfilter heißt dort
 * „alle": Rechts stünde unter der Überschrift eines Prozesses der **gesamte
 * Verkehr des Mandanten**.
 *
 * Die Regel stand zuerst nur in {@link ausSuchparametern} — also an der
 * Funktion, die der **Test** ruft, und nicht an der, die die **Anwendung**
 * ruft. Sie steht deshalb jetzt im Parser, und `ausSuchparametern` benutzt
 * denselben: Test und Laufzeit prüfen damit dieselbe Regel.
 */
export const parseAsKennung = createParser<string>({
  parse: (wert) => (wert === "" ? null : wert),
  serialize: (wert) => wert,
});

/**
 * Die Parameter, so wie sie in der URL stehen.
 *
 * **`prozess` und `nachricht` tragen `history: "push"`**, der Rest nicht. Beide
 * *öffnen* etwas: Unter 768 px tritt die Liste an die Stelle des Baums und das
 * Panel an die Stelle der Liste — das Zurück des Browsers ist dort der Weg
 * heraus, und ohne eigenen Verlaufseintrag spränge es an beidem vorbei
 * (`docs/frontend-grundlagen.md` §8, *„replace für Filter, push für Ansichten"*).
 *
 * **`nurMitVerkehr` steht in der URL, das Eingrenzungsfeld nicht.** Der
 * Unterschied ist die Regel aus §8 und keine Geschmacksfrage: *Was ausgeblendet
 * ist, muss man teilen können.* Der Schalter **lässt weg** — wer einen Link
 * weitergibt, in dem 350 von 390 Prozessen fehlen, muss das mitgeben. Die
 * Eingrenzung dagegen ist eine Eingabe, durch die man beim Tippen hindurchläuft.
 *
 * **Ohne `clearOnDefault: false`, und das ist geprüft, nicht übersehen.** Die
 * Vorgabe `false` lässt nichts weg — sie zeigt alles. Ein Standardwert, der
 * etwas *weglässt*, gehört in die URL; einer, der etwas *zulässt*, nicht. Genau
 * wie bei `langeSuche` und `ueberfaellig` in `filter.ts`.
 */
export const PROZESSANSICHT_PARAMETER = {
  zeitraum: parseAsRollupzeitraum,
  prozess: parseAsKennung.withOptions({ history: "push" }),
  [NACHRICHT_PARAMETER]: parseAsKennung.withOptions({ history: "push" }),
  nurMitVerkehr: parseAsBoolean.withDefault(NUR_MIT_VERKEHR_VORGABE),
  /**
   * **Die Übertragungsliste behält ihren Sortierumschalter**, und damit gehört
   * der Wert in die URL: Er beschreibt den gezeigten Ausschnitt, und die Regel
   * dafür steht in `docs/frontend-grundlagen.md` §8.
   *
   * **Derselbe Parser wie in `filter.ts`** und nicht ein zweiter mit denselben
   * zwei Werten — zwei liefen bei einem dritten Wert auseinander.
   */
  sortierung: parseAsSortierung,
};

export type Prozessansichtzustand = {
  /**
   * Das **gewählte** Paar — `null` heißt „nicht gewählt", nicht „48H". Welches
   * dann wirkt, sagt allein die Antwort (Entscheidung E‑38 im Backend, E‑n in
   * der Oberfläche).
   */
  zeitraum: Rollupzeitraum | null;
  /** Die gewählte `ProcessID`. `null` heißt: rechts steht der Leerzustand. */
  prozess: string | null;
  /** Die geöffnete Nachricht. `null` heißt: rechts steht die Übertragungsliste. */
  nachricht: string | null;
  nurMitVerkehr: boolean;
  /** `null` heißt „nicht gewählt" — die Vorgabe `neueste` setzt der Endpunkt. */
  sortierung: Sortierung | null;
};

export const LEERE_PROZESSANSICHT: Prozessansichtzustand = {
  zeitraum: null,
  prozess: null,
  nachricht: null,
  nurMitVerkehr: NUR_MIT_VERKEHR_VORGABE,
  sortierung: null,
};

/**
 * Der Zustand als **URL** — die Gegenrichtung zu dem, was `nuqs` beim Rendern
 * tut, als reine Funktion für den Test.
 *
 * Die Reihenfolge ist fest, damit zwei gleiche Zustände dieselbe URL ergeben.
 * `nachricht` steht zuletzt: dort, wo man beim Weitergeben hinsieht — dieselbe
 * Anordnung wie in `filter.ts`.
 */
export function alsSuchparameter(zustand: Prozessansichtzustand): URLSearchParams {
  const parameter = new URLSearchParams();
  if (zustand.zeitraum !== null) {
    parameter.set("zeitraum", zustand.zeitraum);
  }
  if (zustand.nurMitVerkehr) {
    parameter.set("nurMitVerkehr", "true");
  }
  if (zustand.sortierung !== null) {
    parameter.set("sortierung", zustand.sortierung);
  }
  if (zustand.prozess !== null && zustand.prozess !== "") {
    parameter.set("prozess", zustand.prozess);
  }
  if (zustand.nachricht !== null && zustand.nachricht !== "") {
    parameter.set(NACHRICHT_PARAMETER, zustand.nachricht);
  }
  return parameter;
}

/**
 * Liest den Zustand aus einer URL. Ein unbrauchbarer Wert wird übergangen —
 * nicht abgewiesen und nicht stillschweigend auf eine Vorgabe gezogen.
 *
 * **Eine leere Kennung ist keine Auswahl.** Sie entstünde nur aus einer von Hand
 * gebauten URL (`?prozess=`) und öffnete sonst eine Liste, die garantiert nichts
 * findet.
 */
export function ausSuchparametern(suchparameter: URLSearchParams): Prozessansichtzustand {
  const zeitraum = suchparameter.get("zeitraum");
  const prozess = suchparameter.get("prozess");
  const nachricht = suchparameter.get(NACHRICHT_PARAMETER);
  const nurMitVerkehr = suchparameter.get("nurMitVerkehr");
  const sortierung = suchparameter.get("sortierung");

  // **Derselbe Parser wie zur Laufzeit** ({@link parseAsKennung}) und nicht
  // eine zweite Prüfung daneben: Sonst prüft der Test eine Regel, die die
  // Anwendung nicht anwendet — genau der Fall, der hier einmal vorlag.
  const kennung = (wert: string | null) => (wert === null ? null : parseAsKennung.parse(wert));

  return {
    zeitraum: istRollupzeitraum(zeitraum) ? zeitraum : null,
    prozess: kennung(prozess),
    nachricht: kennung(nachricht),
    nurMitVerkehr: nurMitVerkehr === null ? NUR_MIT_VERKEHR_VORGABE : nurMitVerkehr === "true",
    sortierung: istSortierung(sortierung) ? sortierung : null,
  };
}

/**
 * Der Filter der **Übertragungsliste rechts** — aus dem gewählten Prozess und
 * dem Fenster, das der Baum-Endpunkt gelesen hat.
 *
 * ## Warum das Fenster aus der Antwort kommt und nicht aus einem `zeitraum`
 *
 * Die Liste kennt die drei Paare `48H`, `30T`, `12M` nicht — ihre relativen
 * Zeiträume heißen `24h`, `7d`, `30d` (`lib/filter.ts`), und keines der sechs
 * fällt mit einem der anderen zusammen. Es bleibt der zweite Modus: `von`/`bis`.
 *
 * **Und das ist nicht der Notausgang, sondern die richtige Antwort.** Beide
 * Seiten lesen dieselbe Spalte — der Rollup gruppiert nach `MessageLastUpdate`,
 * und die Liste filtert über dieselbe (`NachrichtenRepository`). Mit demselben
 * Fenster zeigen Baum und Liste denselben Ausschnitt; mit einem eigenen
 * Zeitraum stünden links und rechts zwei verschiedene Zahlen, und keine wäre
 * falsch.
 *
 * **Gerechnet wird hier nichts.** Das Fenster kommt aus der Antwort, die es
 * gegen die *Anwendungsuhr* aufgelöst hat (Regel Z1) — im Browser gerechnet
 * wäre es gegen die Browseruhr gerechnet, und die Testkopie liegt Monate hinter
 * der realen Uhrzeit.
 *
 * ## Die eine benannte Ungenauigkeit
 *
 * Die obere Grenze des Baums ist **ausschließend** und liegt auf einer
 * Eimergrenze; die Liste vergleicht mit `<=`. Eine Nachricht, die exakt auf der
 * oberen Grenze liegt, erscheint deshalb **rechts sehr wohl und links nicht**:
 * Die Liste liefert sie, der Baum zählt sie nicht mit. Das betrifft
 * genau den Zeitpunkt `bis` und wird nicht ausgeglichen: Eine Sekunde
 * abzuziehen wäre eine Rechnung in der Oberfläche, und die ist teurer als die
 * Ungenauigkeit.
 *
 * @returns `null`, solange kein Prozess gewählt ist oder der Baum noch kein
 *   Fenster genannt hat. Dann läuft **keine** Abfrage — die Ansicht zeigt ihren
 *   Leerzustand.
 */
export function listenfilter(
  zustand: Prozessansichtzustand,
  fenster: Fenster | undefined,
): Nachrichtenfilter | null {
  if (zustand.prozess === null || fenster === undefined) {
    return null;
  }
  const von = new Date(fenster.von);
  const bis = new Date(fenster.bis);
  if (Number.isNaN(von.getTime()) || Number.isNaN(bis.getTime())) {
    return null;
  }

  return {
    zeitraum: null,
    von,
    bis,
    status: null,
    prozess: [zustand.prozess],
    suche: null,
    langeSuche: LANGE_SUCHE_VORGABE,
    ueberfaellig: UEBERFAELLIG_VORGABE,
    sortierung: zustand.sortierung,
    /**
     * **Nicht `zustand.nachricht`.** Die geöffnete Nachricht ist Zustand der
     * Ansicht und kein Filter der Liste; `alsAbfrage` ließe sie ohnehin weg,
     * aber sie hier zu setzen hieße, sich auf diese Auslassung zu verlassen.
     */
    nachricht: null,
  };
}
