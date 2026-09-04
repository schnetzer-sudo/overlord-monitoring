"use client";

import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { useAnzeigezone } from "@/components/zeitzone";
import { useSprache, useTexte } from "@/i18n/provider";
import { formatiereAchsenzeit, formatiereZahl } from "@/lib/format";
import type { Rollupzeitraum } from "@/lib/rollupzeitraum";
import { rollenfuellung, type Statusrolle } from "@/lib/status-farbe";
import {
  STAPELREIHENFOLGE,
  VERLAUFSDECKUNG_OBEN,
  VERLAUFSDECKUNG_UNTEN,
  VERLAUFSFLAECHE,
  VERLAUFSKONTUR,
  achsenaufloesung,
  achsenbreite,
  einordnungenDerRolle,
  rolleKommtVor,
  verlaufszeilen,
  type Verlaufszeile,
} from "../verlauf";

/**
 * Der Verlauf — **eine Fläche und ein Streifen darunter** (Entscheidungen
 * E‑83 bis E‑86, 04.09.2026; davor E‑l und E‑t).
 *
 * ## Was sich am 04.09.2026 geändert hat, und was nicht
 *
 * | | vorher | jetzt |
 * |---|---|---|
 * | Der Verlauf | vier gestapelte Balkenreihen, eine je Farbrolle | **eine Fläche mit der Gesamtsumme** je Eimer |
 * | Die Legende | vier Einträge | **entfällt** — es gibt nur noch eine Reihe |
 * | Der Tooltip | Rollen mit ihren Einordnungen | **unverändert**, Zeile für Zeile |
 * | Der Fehlerstreifen | Balken, eigene Skala | **unverändert** |
 * | Bewegung | keine | **nur beim Aufbau**, und mit `prefers-reduced-motion` gar keine |
 *
 * **E‑l fällt zur Hälfte.** Die Zusammenfassung acht Einordnungen → vier Rollen
 * lebt weiter, aber nur noch im **Tooltip**; im Bild steht sie nicht mehr. Der
 * Auftraggeber wiegt die Lesbarkeit der **Gesamtmenge** höher als die
 * Aufteilung im Bild: Wer den Verlauf ansieht, fragt zuerst *wie viel*, und
 * ein Stapel aus vier Farben beantwortet das schlechter als eine Linie.
 * Ausgeschrieben in
 * [`docs/dashboard-frontend.md`](../../../../docs/dashboard-frontend.md) §5.2.
 *
 * ## Was Recharts hier bekommt und was nicht
 *
 * Farbe kommt **ausschließlich** als `var(--token)` in ein Prop.
 * [`docs/frontend-grundlagen.md`](../../../../docs/frontend-grundlagen.md) §8a
 * hat das für `<Bar fill>`, Achse, Gitter und Tooltip-Zeiger gemessen und
 * **Farbverläufe ausdrücklich als nicht angesehen** ausgewiesen; §8b holt sie
 * nach — mit einem Befund, der ohne die Messung mitgeliefert worden wäre
 * (`fillOpacity`, siehe unten).
 *
 * | Lage | Wie sie hier steht |
 * |---|---|
 * | `<Bar fill>` | gemessen in §8a. Trägt den Fehlerstreifen |
 * | `<linearGradient><stop stop-color>` | gemessen in **§8b**, beide Kanten, beide Farbschemata |
 * | `<Area stroke>` | gemessen in §8b — dieselbe Zeichenkette im SVG-Attribut, dasselbe Pixel |
 * | Legende | **gibt es nicht mehr** |
 * | Tooltip | **eigener `content`** — eigenes JSX mit Tailwind-Klassen, also gar kein Recharts-Farbweg |
 * | Tooltip-Zeiger | `var()` in einem Prop, und er **muss** gesetzt werden: Recharts' Voreinstellung für eine Nicht-Balken-Fläche ist ein fest eingebauter grauer Hex-Wert (`component/Cursor.js`) |
 * | `Cell` | kommt nicht vor |
 *
 * ## Die Farbe der Fläche sagt nichts über die Daten — das ist ihre Aufgabe
 *
 * `VERLAUFSFLAECHE` und `VERLAUFSKONTUR` stehen in `features/dashboard/verlauf.ts`
 * mit der ganzen Begründung. Kurz: Die Fläche trägt eine **Summe**, und eine
 * Summe hat keinen Status. Seit dem 04.09.2026 trägt sie dafür eine **eigene
 * Rolle** in Ton 230 (E‑87) und nicht mehr den Akzent — der sagt über die Daten
 * zwar nichts, über die **Anwendung** aber sehr wohl etwas.
 *
 * **Die Aussage trägt die Kontur, die Fläche trägt Gewicht.** Deshalb gilt die
 * 3 : 1 aus WCAG 1.4.11 an `VERLAUFSKONTUR` (7,29 : 1 hell, 9,06 : 1 dunkel)
 * und ausdrücklich **nicht** an der Fläche: Die ist eine Tönung, und ihr
 * Kontrast wird berichtet statt gefordert.
 *
 * ## Zwei Diagramme und nicht zwei Achsen in einem
 *
 * Über den Gesamtbestand ist die Einordnung `FEHLER` **0,03 %** — in 260 Pixeln
 * Höhe unter einem Pixel. Deshalb ein zweiter, schmaler Streifen mit **eigener
 * Skala**. Er bleibt ein **Balken**diagramm, und die verschiedene Form ist
 * gewollt: Sie hält auseinander, was verschiedene Größen sind.
 *
 * **Die Zeitachse steht einmal, unter dem Streifen.** Beide Diagramme haben
 * dieselben Eimer und dieselbe Achsenbreite (`achsenbreite`).
 *
 * **Überfällig läuft nicht mit**, in keiner Variante: Die Kategorie entsteht
 * live über `Message` und steht nicht je Eimer im Rollup.
 */

const HOEHE_VERLAUF = 260;
const HOEHE_STREIFEN = 88;

/**
 * **Der Deckel für die Balkenbreite.**
 *
 * Bei zwölf Eimern über die volle Fensterbreite gibt Recharts jedem Balken rund
 * hundert Pixel und füllt sie bis auf `barCategoryGap` aus: Der Streifen liest
 * sich dann als **eine zusammenhängende Farbfläche** und nicht als Reihe
 * einzelner Zählungen. Recharts rechnet `min(Slotbreite, maxBarSize)` und rückt
 * den schmaleren Balken in die Mitte seines Slots; wo die Slotbreite ohnehin
 * darunter liegt — 48 Eimer am schmalen Fenster —, bewirkt die Zahl **nichts**.
 *
 * **Der Wert ist gemessen und nicht gewählt** (E‑v, 01.09.2026); die
 * Balkenbreiten, aus denen er folgt, stehen in
 * [`docs/dashboard-frontend.md`](../../../../docs/dashboard-frontend.md) §5.2.
 *
 * > **Seit dem 04.09.2026 trägt ihn nur noch ein Diagramm.** Bis dahin stand
 * > hier der Satz, beide bekämen denselben Wert, weil sonst die Balken des
 * > Streifens bei kleiner Eimerzahl nicht mehr unter denen des Verlaufs
 * > stünden. Der Verlauf hat keine Balken mehr; der **Wert bleibt unverändert**,
 * > und der Grund, aus dem er im Streifen steht, gilt dort unverändert weiter.
 */
const MAX_BALKENBREITE = 28;

const ACHSENSCHRIFT = { fill: "var(--muted-foreground)", fontSize: 11 };
const ACHSENLINIE = { stroke: "var(--border)" };
/** Der Balkenzeiger — eine Fläche über dem ganzen Slot. */
const ZEIGER = { fill: "var(--muted)" };
/**
 * Der Flächenzeiger — eine **Linie**, denn über einer Fläche gibt es keinen
 * Slot, den man einfärben könnte. `fill` bliebe hier wirkungslos, und Recharts
 * malte seinen eingebauten Grauwert — einen festen Hex-Wert außerhalb des
 * Konzepts, der hier nicht einmal als Zitat stehen darf
 * (`tests/farbwerte.test.ts` liest Text, und das ist richtig so).
 * Nachzulesen in `recharts/es6/component/Cursor.js`.
 */
const ZEIGERLINIE = { stroke: "var(--border)" };

/**
 * **Der Farbverlauf braucht eine `id`, und eine feste ist hier die richtige.**
 *
 * `useId()` erzeugte je Einbau eine andere und machte den DOM unzitierbar; die
 * Landingpage rendert genau einen Verlauf, und zwei Diagramme mit derselben
 * `id` gäbe es nur, wenn jemand einen zweiten daneben stellte. Dann fiele es
 * sofort auf, weil beide dieselbe Füllung zeigten.
 */
const FUELLUNGS_ID = "verlauf-flaeche";

/**
 * **Die Deckung der beiden Stopps steht nicht mehr hier** — sie sind seit dem
 * 04.09.2026 Token in `app/globals.css` und je Block verschieden (hell
 * 0,35 / 0,03, dunkel 0,28 / 0,04). Die Begründung steht an
 * `VERLAUFSDECKUNG_OBEN` in `features/dashboard/verlauf.ts`; hier stünde sie
 * ein zweites Mal.
 *
 * ## Wo die Gitterlinie steht, und es ist der Verlauf und nicht die Farbe
 *
 * Das Gitter liegt **unter** der Fläche — nachgesehen an den Recharts-Lagen im
 * DOM (`recharts-zIndex-layer_-100` gegen `_100`), nicht angenommen. Was von
 * einer Linie übrig bleibt, hängt damit allein an der Deckung an ihrer Höhe.
 * Gemessen als OKLab-Abstand zwischen „Fläche über Linie" und „Fläche über
 * Karte", gegen die Sichtprobe von 0,025 aus `visuelles-konzept.md` §7a:
 *
 * | Deckung | hell | dunkel |
 * |---|---|---|
 * | oberer Stopp | 0,0669 | 0,0741 |
 * | Fuß | 0,0973 | 0,1086 |
 *
 * **Die Linie verschwindet an keiner Stelle mehr.** Mit dem alten oberen Stopp
 * von `1` war sie unter dem Scheitel vollständig gedeckt (0,0000) und kam erst
 * unterhalb von rund 87 % Deckung wieder durch; das war eine Eigenschaft der
 * vollen Deckung und keine der Farbe. Der niedrigste gemessene Wert liegt jetzt
 * bei **0,0669** und damit zweieinhalbfach über der Schwelle.
 */

/**
 * **Die Dauer des Aufbaus — an beiden Diagrammen dieselbe.**
 *
 * Recharts' Voreinstellungen sind **verschieden**: `<Area>` 1500 ms, `<Bar>`
 * 400 ms (`cartesian/Area.js`, `cartesian/Bar.js`). Ohne diese Konstante liefe
 * der Streifen fertig, während die Fläche noch wächst — und die gemeinsame
 * Zeitachse verspräche für die Dauer des Aufbaus eine Zuordnung, die das Bild
 * nicht zeigt. `animationBegin` steht aus demselben Grund an beiden auf `0`.
 *
 * **600 ms sind gewählt und nicht gemessen.** Lang genug, dass der Aufbau als
 * Aufbau lesbar ist, kurz genug, dass niemand darauf wartet.
 */
const AUFBAUDAUER = 600;
const AUFBAUBEGINN = 0;

export function VerlaufDiagramm({
  punkte,
  zeitraum,
}: {
  punkte: Parameters<typeof verlaufszeilen>[0];
  zeitraum: Rollupzeitraum;
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
   * dieselbe Breite, sonst stünden ihre Eimer nicht mehr übereinander.
   */
  const groesster = zeilen.reduce((hoechst, zeile) => Math.max(hoechst, zeile.gesamt), 0);
  const achse = achsenbreite(zahl(groesster).length);

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h2 className="text-basis font-semibold">{texte.dashboard.verlauf.titel}</h2>
        {/*
         * **Was die Fläche zählt, stand bis zum 04.09.2026 in der Legende.**
         * Mit ihr ist das letzte Wort gefallen, das den Verlauf beschriftet
         * hat — die y-Achse trägt nur Zahlen. Der Satz ist keine neue
         * Zeichenkette: `achseAnzahl` steht seit dem 01.09.2026 in beiden
         * Sprachdateien.
         */}
        <p className="text-muted-foreground text-beiwerk">{texte.dashboard.verlauf.achseAnzahl}</p>
      </div>

      {/*
       * **Der `key` trägt das Zeitraumpaar — ein Wechsel baut neu auf**
       * (Entscheidung E‑86).
       *
       * Die Zahl der Eimer ändert sich mit dem Paar (48 · 30 · 12). Ohne den
       * `key` bliebe die Komponente dieselbe, und Recharts interpolierte
       * zwischen zwei Pfaden mit **unterschiedlich vielen** Stützstellen: Der
       * Zwischenzustand zeigte für den Bruchteil einer Sekunde eine Kurve, die
       * es in keinem der beiden Zeiträume gibt. Ein Neuaufbau kennt diesen
       * Zustand nicht.
       *
       * Er steht an **beiden** Containern: Baute nur einer neu auf, liefen die
       * beiden Aufbauten auseinander.
       */}
      <ResponsiveContainer key={`verlauf-${zeitraum}`} width="100%" height={HOEHE_VERLAUF}>
        {/*
         * `bottom: 14` ist kein Abstand, sondern Platz für die **Null** der
         * y-Achse. Recharts lässt eine Beschriftung weg, deren Textkasten über
         * den Zeichenbereich hinausragte, und die Null sitzt mittig auf der
         * untersten Linie: Mit acht Pixeln fiel sie weg, mit vierzehn steht sie
         * da (nachgesehen am 01.09.2026 an den `<text>`-Knoten, nicht geschätzt).
         * Ohne sie beginnt die Skala sichtbar bei 55, und die Fläche sähe
         * niedriger aus, als sie ist.
         */}
        <AreaChart data={zeilen} margin={{ top: 4, right: 4, bottom: 14, left: 0 }}>
          <defs>
            {/*
             * **Zwei Stopps, beide auf demselben Token.** Der Verlauf läuft
             * nicht durch den Farbraum, sondern nur durch die Deckung — eine
             * zweite Farbe wäre eine zweite Aussage, und die Fläche macht nur
             * eine.
             *
             * `objectBoundingBox` (die Voreinstellung) heißt: Versatz 0 liegt
             * am **höchsten Punkt der Fläche** und Versatz 1 an der Nulllinie.
             * Der Farbverlauf hängt damit an der Fläche und nicht am
             * Zeichenbereich — bei einem flachen Verlauf ist der Scheitel
             * genauso satt wie bei einem hohen.
             */}
            <linearGradient id={FUELLUNGS_ID} x1="0" y1="0" x2="0" y2="1">
              {/*
               * **`style` und nicht `stopOpacity`.** Beide schreiben dieselbe
               * Eigenschaft, aber nur die Stildeklaration löst `var()` auf:
               * Ein Präsentationsattribut trüge die Zeichenkette unverändert
               * ins SVG, und der Stopp fiele auf seine Voreinstellung `1`
               * zurück — sichtbar als Fläche ohne jede Durchsicht. Am
               * laufenden System nachgesehen, nicht angenommen.
               */}
              <stop
                offset="0%"
                stopColor={VERLAUFSFLAECHE}
                style={{ stopOpacity: VERLAUFSDECKUNG_OBEN }}
              />
              <stop
                offset="100%"
                stopColor={VERLAUFSFLAECHE}
                style={{ stopOpacity: VERLAUFSDECKUNG_UNTEN }}
              />
            </linearGradient>
          </defs>
          <CartesianGrid vertical={false} {...ACHSENLINIE} />
          {/*
           * **`scale="band"` und nicht die Voreinstellung** — sonst stünde die
           * Fläche nicht über dem Streifen.
           *
           * Recharts wählt für eine Kategorieachse ohne Balken `scalePoint`
           * (`combineRealScaleType`): Der erste Punkt säße dann am **linken
           * Rand** des Zeichenbereichs, der Balken desselben Eimers im
           * Streifen darunter aber in der Mitte seines Bandes. Ein Eimer ist
           * ein **Zeitraum** und kein Zeitpunkt; das Band ist die richtige
           * Form, und es ist dieselbe, die der Streifen benutzt.
           */}
          <XAxis dataKey="eimer" scale="band" hide />
          <YAxis
            width={achse}
            allowDecimals={false}
            tickLine={false}
            axisLine={false}
            tick={ACHSENSCHRIFT}
            tickFormatter={zahl}
          />
          <Tooltip
            cursor={ZEIGERLINIE}
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
          {/*
           * **`type="monotone"` und nicht `natural`.** Beide sind weiche
           * Kurven; die natürliche Spline schwingt zwischen zwei Stützstellen
           * über sie hinaus und zöge zwischen einem gefüllten und einem leeren
           * Eimer eine Kurve **unter null**. Das wäre eine negative Anzahl
           * Nachrichten, gemalt. `monotone` kann das nicht.
           *
           * **Seit dem 04.09.2026 steht eine Zahl daneben statt einer
           * Behauptung.** Der gezeichnete Pfad ist aus dem `d`-Attribut
           * ausgelesen und dicht abgetastet worden, in allen drei Zeiträumen:
           * Er verlässt das Intervall zwischen niedrigstem und höchstem Eimer
           * um **0,0000 px**. Über dieselben 45 Stützstellen läge die
           * natürliche Spline bei **−395,6 Nachrichten** — 30,5 px unter der
           * Nulllinie. Zahlen und Gegenprobe in
           * [`docs/dashboard-frontend.md`](../../../../docs/dashboard-frontend.md)
           * §5.2.
           *
           * **`fillOpacity={1}` ist kein Beiwerk.** Recharts' Voreinstellung
           * für `<Area>` ist **0,6** (`cartesian/Area.js`), und sie liegt
           * *über* dem Farbverlauf: Ohne diese Zeile malte die Oberkante nicht
           * das Token, sondern das Token zu **60 %** über dem Untergrund — und
           * damit in Hell und Dunkel zwei verschiedene Farben. Gefunden in der
           * Messung vor dem Einbau (§8b), nicht hinterher in der Ansicht.
           *
           * **`isAnimationActive` steht bewusst nicht da.** Die Voreinstellung
           * ist `'auto'`, und `'auto'` heißt in Recharts 3.10.1: kein Aufbau
           * bei `prefers-reduced-motion: reduce` und keiner beim
           * Serverrendern (`util/usePrefersReducedMotion.js`). Ein eigener
           * Schalter wäre ein zweiter Weg zu derselben Entscheidung.
           */}
          <Area
            type="monotone"
            dataKey="gesamt"
            name={texte.dashboard.verlauf.gesamt}
            fill={`url(#${FUELLUNGS_ID})`}
            fillOpacity={1}
            stroke={VERLAUFSKONTUR}
            strokeWidth={2}
            dot={false}
            activeDot={false}
            animationBegin={AUFBAUBEGINN}
            animationDuration={AUFBAUDAUER}
          />
        </AreaChart>
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

      <ResponsiveContainer key={`streifen-${zeitraum}`} width="100%" height={HOEHE_STREIFEN}>
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
            activeBar={false}
            animationBegin={AUFBAUBEGINN}
            animationDuration={AUFBAUDAUER}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/**
 * Die Farbquadrate des Tooltips tragen den **Vordergrund** als Fläche und nicht
 * die `-flaeche`-Stufe. Vollständige Klassennamen, weil Tailwind den Quelltext
 * durchsucht.
 *
 * **Sie sind seit dem 04.09.2026 die einzige Stelle, an der die vier Farbrollen
 * des Verlaufs noch zu sehen sind** — die Legende ist mit dem Stapel entfallen.
 * Genau deshalb bleiben sie: Ohne sie stünde im Tooltip eine Liste ohne jede
 * Verbindung zu den Plaketten der Nachrichtenliste.
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
 * **Seit dem 04.09.2026 ist er die ganze Aufteilung und nicht mehr ihre
 * Ergänzung.** Im Bild steht eine Fläche mit einer Summe; hier steht, woraus
 * sie besteht — vier Rollen, und unter jeder mehrgliedrigen die Einordnungen.
 * Der Unterschied zwischen *aufgeteilt* und *zusammengeführt* geht damit nicht
 * verloren, er hat nur keinen Ort mehr im Bild.
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
