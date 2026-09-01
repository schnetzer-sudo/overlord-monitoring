"use client";

import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { useAnzeigezone } from "@/components/zeitzone";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereAchsenzeit, formatiereZahl } from "@/lib/format";
import { rollenfuellung, type Statusrolle } from "@/lib/status-farbe";

import type { Dashboardzeitraum } from "../api";
import {
  STAPELREIHENFOLGE,
  achsenaufloesung,
  achsenbreite,
  einordnungenDerRolle,
  rolleKommtVor,
  verlaufszeilen,
  type Verlaufszeile,
} from "../verlauf";

/**
 * Der Verlauf — **vier Reihen und ein zweiter Streifen darunter**
 * (Entscheidungen E‑l und E‑t).
 *
 * ## Was Recharts hier bekommt und was nicht
 *
 * Farbe kommt **ausschließlich** als `fill="var(--token)"` in ein Prop, und der
 * Wert kommt aus `lib/status-farbe.ts` — nie aus dieser Datei.
 * [`docs/frontend-grundlagen.md`](../../../../docs/frontend-grundlagen.md) §8a
 * hat das gemessen und dabei drei Lagen ausdrücklich **nicht** angesehen. Zwei
 * davon sind hier umgangen und eine ist nachgemessen:
 *
 * | Lage | Wie sie hier steht |
 * |---|---|
 * | `<Bar fill>` | gemessen in §8a. Benutzt |
 * | Legende | **nicht Recharts'** — eigenes Markup unter dem Diagramm, siehe unten |
 * | Tooltip | **eigener `content`** — eigenes JSX mit Tailwind-Klassen, also gar kein Recharts-Farbweg |
 * | `activeBar` | ausdrücklich `false`. Sie ist in §8a als „nicht angesehen" ausgewiesen, und sie wird hier nicht gebraucht |
 * | `Cell`, Farbverläufe | kommen nicht vor |
 * | Achsen, Gitter, Tooltip-Zeiger | `var()` in einem Prop, und **das war nicht gemessen** — nachgeholt am 01.09.2026, §8a ist ergänzt |
 *
 * **Keine Animation** (`isAnimationActive={false}`). `docs/visuelles-konzept.md`
 * §1: keine Bewegung, die keine Auskunft gibt — und ein Balken, der bei jedem
 * Zeitraumwechsel von unten hochwächst, gibt keine.
 *
 * ## Die eigene Legende
 *
 * Recharts brächte eine mit, und §8a hat sogar sie gemessen. Sie kann trotzdem
 * nicht, was hier gebraucht wird: **eine Rolle weglassen, die über alle Eimer
 * null ist.** Ein Legendeneintrag ohne Segment im Bild verspricht eine
 * Unterscheidung, die es nicht gibt. Vier Zeilen eigenes Markup sind billiger
 * als ein Umweg über `payload`-Filter — und sie sind der ohnehin belegte Weg,
 * auf dem Farbe in dieses Projekt kommt: eine Tailwind-Klasse aus
 * `lib/status-farbe.ts`.
 *
 * ## Zwei Diagramme und nicht zwei Achsen in einem
 *
 * Über den Gesamtbestand ist die Einordnung `FEHLER` **0,03 %**. In einem
 * gestapelten Balken von 260 Pixeln Höhe ist das **unter einem Pixel** — die
 * wichtigste Kategorie des Werkzeugs wäre unsichtbar, bei jedem Mandanten und in
 * jedem Fenster. Deshalb ein zweiter, schmaler Streifen mit **eigener Skala**.
 *
 * **Und deshalb keine zweite y-Achse in einem Diagramm:** Ohne getrennte Flächen
 * liest jemand die Höhe gegen den Hauptbalken und hält fünf Fehler für ein
 * Drittel des Verkehrs. Der Satz unter der Überschrift des Streifens sagt es
 * zusätzlich in Worten.
 *
 * **Die Zeitachse steht einmal, unter dem Streifen.** Beide Diagramme haben
 * dieselben Eimer und dieselbe Achsenbreite (`ACHSENBREITE`), ihre Balken stehen
 * damit übereinander. Zweimal dieselbe Beschriftung wäre doppelt gelesener Platz.
 *
 * **Kein zusätzlicher Lesevorgang.** Es sind dieselben Daten aus Block 1,
 * zweimal dargestellt.
 *
 * **Überfällig läuft nicht mit**, in keiner Variante: Die Kategorie entsteht live
 * über `Message` und steht nicht je Eimer im Rollup.
 */

const HOEHE_VERLAUF = 260;
const HOEHE_STREIFEN = 88;

/**
 * **Der Deckel für die Balkenbreite — an beiden Diagrammen derselbe.**
 *
 * Bei zwölf Eimern über die volle Fensterbreite gibt Recharts jedem Balken
 * rund hundert Pixel und füllt sie bis auf `barCategoryGap` aus: Der Verlauf
 * liest sich dann als **eine zusammenhängende Farbfläche** und nicht als Reihe
 * einzelner Zählungen. Bei 48 Eimern tritt das nicht auf — das Bild hängt an
 * der Zahl der Eimer und nicht an der Bauform.
 *
 * **Ein Deckel und keine feste Breite.** Recharts rechnet
 * `min(Slotbreite, maxBarSize)` und rückt den schmaleren Balken in die Mitte
 * seines Slots. Wo die Slotbreite ohnehin darunter liegt — 48 Eimer am
 * schmalen Fenster —, bewirkt die Zahl **nichts**, und genau das ist der
 * Unterschied zur gerechneten Konstante, die hier schon einmal gescheitert ist
 * (`achsenabstand`, §5.3).
 *
 * **Beide Diagramme bekommen denselben Wert**, aus demselben Grund wie die
 * gemeinsame Achsenbreite: Sonst stünden die Balken des Fehlerstreifens bei
 * kleiner Eimerzahl nicht mehr unter denen des Verlaufs, und die gemeinsame
 * Zeitachse verspräche eine Zuordnung, die es nicht gäbe.
 *
 * Die Zahl ist **gemessen und nicht gewählt**; die Balkenbreiten, aus denen sie
 * folgt, stehen in
 * [`docs/dashboard-frontend.md`](../../../../docs/dashboard-frontend.md) §5.2.
 */
const MAX_BALKENBREITE = 28;

const ACHSENSCHRIFT = { fill: "var(--muted-foreground)", fontSize: 11 };
const ACHSENLINIE = { stroke: "var(--border)" };
const ZEIGER = { fill: "var(--muted)" };

export function VerlaufDiagramm({
  punkte,
  zeitraum,
}: {
  punkte: Parameters<typeof verlaufszeilen>[0];
  zeitraum: Dashboardzeitraum;
}) {
  const texte = useTexte();
  const sprache = useSprache();
  const zone = useAnzeigezone();

  const zeilen = verlaufszeilen(punkte);
  const aufloesung = achsenaufloesung(zeitraum);
  const sichtbareRollen = STAPELREIHENFOLGE.filter((rolle) => rolleKommtVor(zeilen, rolle));
  const fehlerKommtVor = rolleKommtVor(zeilen, "fehler");

  const achsenzeit = (wert: string) => formatiereAchsenzeit(wert, aufloesung, sprache, zone);
  const zahl = (wert: number) => formatiereZahl(wert, sprache);

  /*
   * **Die Achsenbreite folgt der längsten Zahl, die vorkommen kann.** Mit einer
   * festen war bei `NEXANS` über zwölf Monate `:20.000` statt `220.000` zu
   * lesen — Recharts beschneidet an der Achsenbreite und meldet nichts.
   * Gerechnet wird über die größte Eimersumme; **beide** Diagramme bekommen
   * dieselbe Breite, sonst stünden ihre Balken nicht mehr übereinander.
   */
  const groesster = zeilen.reduce((hoechst, zeile) => Math.max(hoechst, zeile.gesamt), 0);
  const achse = achsenbreite(zahl(groesster).length);

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h2 className="text-basis font-semibold">{texte.dashboard.verlauf.titel}</h2>
        <Legende rollen={sichtbareRollen} />
      </div>

      <ResponsiveContainer width="100%" height={HOEHE_VERLAUF}>
        {/*
         * `bottom: 14` ist kein Abstand, sondern Platz für die **Null** der
         * y-Achse. Recharts lässt eine Beschriftung weg, deren Textkasten über
         * den Zeichenbereich hinausragte, und die Null sitzt mittig auf der
         * untersten Linie: Mit acht Pixeln fiel sie weg, mit vierzehn steht sie
         * da (nachgesehen am 01.09.2026 an den `<text>`-Knoten, nicht geschätzt).
         * Ohne sie beginnt die Skala sichtbar bei 55, und der Balken sähe kürzer
         * aus, als er ist.
         */}
        <BarChart
          data={zeilen}
          margin={{ top: 4, right: 4, bottom: 14, left: 0 }}
          barCategoryGap={1}
        >
          <CartesianGrid vertical={false} {...ACHSENLINIE} />
          <XAxis dataKey="eimer" hide />
          <YAxis
            width={achse}
            allowDecimals={false}
            tickLine={false}
            axisLine={false}
            tick={ACHSENSCHRIFT}
            tickFormatter={zahl}
          />
          <Tooltip
            cursor={ZEIGER}
            isAnimationActive={false}
            wrapperStyle={{ outline: "none" }}
            content={(eigenschaften) => (
              <VerlaufTooltip
                zeile={zeileAus(eigenschaften)}
                rollen={sichtbareRollen}
                achsenzeit={achsenzeit}
                zahl={zahl}
              />
            )}
          />
          {sichtbareRollen.map((rolle) => (
            <Bar
              key={rolle}
              dataKey={rolle}
              stackId="verlauf"
              name={texte.dashboard.verlauf.rollen[rolle]}
              fill={rollenfuellung(rolle)}
              maxBarSize={MAX_BALKENBREITE}
              isAnimationActive={false}
              activeBar={false}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>

      <div className="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h3 className="text-basis font-semibold">{texte.dashboard.verlauf.streifenTitel}</h3>
        <p className="text-muted-foreground text-beiwerk">
          {texte.dashboard.verlauf.streifenHinweis}
        </p>
      </div>

      {fehlerKommtVor ? null : (
        <p className="text-muted-foreground text-beiwerk">{texte.dashboard.verlauf.streifenLeer}</p>
      )}

      <ResponsiveContainer width="100%" height={HOEHE_STREIFEN}>
        <BarChart
          data={zeilen}
          margin={{ top: 4, right: 4, bottom: 0, left: 0 }}
          barCategoryGap={1}
        >
          <CartesianGrid vertical={false} {...ACHSENLINIE} />
          <XAxis
            dataKey="eimer"
            /*
             * **Wie viele Eimer beschriftet werden, entscheidet die Breite** und
             * keine feste Zahl.
             *
             * Hier stand `interval={achsenabstand(zeilen.length)}` — gerechnet
             * aus der Zahl der Eimer und einem Deckel von zwölf Beschriftungen.
             * Am 1500 px breiten Fenster trug das; **bei 360 px überlappten die
             * Beschriftungen um 12 Pixel** (gemessen am laufenden System über
             * die Kästen der `<text>`-Knoten). Eine Zahl, die von der Breite
             * nicht weiß, kann bei beiden nicht richtig sein.
             *
             * `equidistantPreserveStart` wählt einen **gleichabständigen**
             * Ausschnitt, der in die vorhandene Breite passt, und `minTickGap`
             * sagt, wie eng „passt" gemeint ist. Damit hängt die Dichte am
             * Augenschein und nicht an einer geratenen Konstante — und sie
             * stimmt an jedem Umbruchpunkt, auch an denen, die es noch nicht
             * gibt.
             */
            interval="equidistantPreserveStart"
            minTickGap={12}
            tickFormatter={achsenzeit}
            tickLine={false}
            axisLine={ACHSENLINIE}
            tick={ACHSENSCHRIFT}
          />
          <YAxis
            width={achse}
            allowDecimals={false}
            tickLine={false}
            axisLine={false}
            tick={ACHSENSCHRIFT}
            tickFormatter={zahl}
            /*
             * **Die eigene Skala beginnt bei null und endet am eigenen
             * Höchstwert.** Genau das ist ihr Zweck: Fünf Fehler sollen als fünf
             * Fehler zu sehen sein und nicht als Anteil am Verkehr darüber.
             *
             * **Zwei Beschriftungen und nicht fünf.** Bei einem Höchstwert von
             * zwei rundete Recharts die Skala auf vier auf und beschriftete
             * mittendrin — der Streifen behauptete damit einen Kopfraum, den es
             * nicht gibt. Null und der tatsächliche Höchstwert sagen genau das,
             * worauf es hier ankommt: **es sind einzelne Zeilen.**
             *
             * `Math.max(…, 1)` fängt den fehlerfreien Zeitraum: Eine Skala von
             * null bis null hätte keine Höhe. Daneben steht dann ohnehin der
             * Satz, dass nichts als Fehler eingeordnet ist.
             */
            domain={[0, (hoechster: number) => Math.max(hoechster, 1)]}
            tickCount={2}
          />
          <Tooltip
            cursor={ZEIGER}
            isAnimationActive={false}
            wrapperStyle={{ outline: "none" }}
            content={(eigenschaften) => (
              <VerlaufTooltip
                zeile={zeileAus(eigenschaften)}
                rollen={["fehler"]}
                achsenzeit={achsenzeit}
                zahl={zahl}
                nurRollensumme
              />
            )}
          />
          <Bar
            dataKey="fehler"
            name={texte.dashboard.verlauf.rollen.fehler}
            fill={rollenfuellung("fehler")}
            maxBarSize={MAX_BALKENBREITE}
            isAnimationActive={false}
            activeBar={false}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/**
 * Die Legende — **vier Einträge, nie acht** (Entscheidung E‑l).
 *
 * Die Farbe kommt als Tailwind-Klasse aus `lib/status-farbe.ts`, auf demselben
 * Weg wie jede Plakette des Projekts. Ein Quadrat allein wäre die halbe Aussage;
 * daneben steht das Wort.
 */
function Legende({ rollen }: { rollen: readonly Statusrolle[] }) {
  const texte = useTexte();
  return (
    <ul className="text-beiwerk flex flex-wrap items-center gap-x-3 gap-y-1">
      {rollen.map((rolle) => (
        <li key={rolle} className="flex items-center gap-1.5">
          <span aria-hidden="true" className={`size-2.5 rounded-xs ${FLAECHENKLASSE[rolle]}`} />
          <span className="text-muted-foreground">{texte.dashboard.verlauf.rollen[rolle]}</span>
        </li>
      ))}
    </ul>
  );
}

/**
 * Das Legendenquadrat trägt den **Vordergrund** als Fläche und nicht die
 * `-flaeche`-Stufe: Es soll dieselbe Farbe zeigen wie das Segment im Balken, und
 * das ist der Vordergrund. Vollständige Klassennamen, weil Tailwind den
 * Quelltext durchsucht.
 */
const FLAECHENKLASSE: Record<Statusrolle, string> = {
  fehler: "bg-status-fehler",
  offen: "bg-status-offen",
  abgeschlossen: "bg-status-abgeschlossen",
  ungeklaert: "bg-status-ungeklaert",
};

/** Was Recharts dem `content` mitgibt — nur das, was hier gebraucht wird. */
type Tooltipeigenschaften = {
  active?: boolean;
  payload?: readonly { payload?: unknown }[];
};

function zeileAus(eigenschaften: Tooltipeigenschaften): Verlaufszeile | null {
  if (eigenschaften.active !== true) {
    return null;
  }
  const erster = eigenschaften.payload?.[0]?.payload;
  return erster === undefined || erster === null ? null : (erster as Verlaufszeile);
}

/**
 * Der Tooltip nennt die enthaltenen Einordnungen **einzeln mit ihren Zahlen**.
 *
 * Das ist die Gegenleistung für die Zusammenfassung auf vier Reihen: Im Bild
 * stehen vier Farben, hier steht wieder, woraus sie bestehen — und damit geht der
 * Unterschied zwischen *aufgeteilt* und *zusammengeführt* nicht verloren.
 *
 * **Eigener `content` statt des mitgelieferten Tooltips.** Der Standard nennt je
 * Reihe eine Zahl; gebraucht wird je Reihe eine **Liste**. Das ist eigenes JSX
 * mit Tailwind-Klassen und damit kein Recharts-Farbweg.
 *
 * @param nurRollensumme Für den Fehlerstreifen: Dort ist die Rolle die
 *   Aufschlüsselung schon, eine Liste darunter wiederholte sie.
 */
function VerlaufTooltip({
  zeile,
  rollen,
  achsenzeit,
  zahl,
  nurRollensumme = false,
}: {
  zeile: Verlaufszeile | null;
  rollen: readonly Statusrolle[];
  achsenzeit: (wert: string) => string;
  zahl: (wert: number) => string;
  nurRollensumme?: boolean;
}) {
  const texte = useTexte();
  if (zeile === null) {
    return null;
  }

  return (
    <div className="bg-card text-card-foreground text-beiwerk ring-foreground/10 rounded-md px-3 py-2 shadow-md ring-1">
      <p className="font-medium tabular-nums">{achsenzeit(zeile.eimer)}</p>
      <dl className="mt-1 grid grid-cols-[auto_auto] gap-x-3 tabular-nums">
        {rollen.map((rolle) => (
          <RollenZeile
            key={rolle}
            zeile={zeile}
            rolle={rolle}
            zahl={zahl}
            nurRollensumme={nurRollensumme}
          />
        ))}
        {nurRollensumme ? null : (
          <>
            <dt className="text-muted-foreground border-border mt-1 border-t pt-1">
              {texte.dashboard.verlauf.gesamt}
            </dt>
            <dd className="border-border mt-1 border-t pt-1 text-right font-medium">
              {zahl(zeile.gesamt)}
            </dd>
          </>
        )}
      </dl>
    </div>
  );
}

function RollenZeile({
  zeile,
  rolle,
  zahl,
  nurRollensumme,
}: {
  zeile: Verlaufszeile;
  rolle: Statusrolle;
  zahl: (wert: number) => string;
  nurRollensumme: boolean;
}) {
  const texte = useTexte();
  const enthaltene = einordnungenDerRolle(zeile, rolle);

  return (
    <>
      <dt className="flex items-center gap-1.5">
        <span aria-hidden="true" className={`size-2.5 rounded-xs ${FLAECHENKLASSE[rolle]}`} />
        {texte.dashboard.verlauf.rollen[rolle]}
      </dt>
      <dd className="text-right font-medium">{zahl(zeile[rolle])}</dd>
      {nurRollensumme
        ? null
        : enthaltene
            /*
             * **Nur mehrgliedrige Rollen werden aufgeschlüsselt.** Bei `fehler`
             * steht unter „Fehler 12" sonst noch einmal „Fehler 12" — eine
             * Wiederholung, die aussieht wie eine zweite Zahl.
             */
            .filter(() => enthaltene.length > 1)
            .map((eintrag) => (
              <Fragmentzeile
                key={eintrag.einordnung}
                name={texte.einordnung[eintrag.einordnung]}
                wert={zahl(eintrag.anzahl)}
              />
            ))}
    </>
  );
}

function Fragmentzeile({ name, wert }: { name: string; wert: string }) {
  return (
    <>
      <dt className="text-muted-foreground pl-4">{name}</dt>
      <dd className="text-muted-foreground text-right">{wert}</dd>
    </>
  );
}
