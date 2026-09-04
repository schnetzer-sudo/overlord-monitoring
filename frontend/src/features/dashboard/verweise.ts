import { ROUTEN } from "@/lib/routen";
import type { Rollupzeitraum } from "@/lib/rollupzeitraum";

import type { Fenster } from "./api";

/**
 * Wohin eine Kachel führt (Entscheidungen E‑m und **E‑80**).
 *
 * | Kachel | Ziel | Zeitraum |
 * |---|---|---|
 * | **Fehler** | `status=FEHLER` | **erbt** das Fenster der Antwort |
 * | **Läuft** | `status=LAEUFT` | **erbt** |
 * | **Wartend** | `status=WARTEND` | **bringt seinen mit** |
 * | **Nachrichten** | kein Verweis | — |
 *
 * ## Die Asymmetrie ist der ganze Inhalt von E‑80
 *
 * **Läuft darf erben.** Eine laufende Nachricht ist höchstens so alt wie die
 * Wächterfrist des Altsystems — rund 30 Minuten — und liegt damit in jedem
 * Zeitraum, den der Umschalter anbietet.
 *
 * **Wartend darf nicht.** Auf der Testkopie sind es 579.934 Sekunden, also 6,71
 * Tage; bei 48 Stunden zeigte das Ziel einen Bruchteil der genannten Zahl.
 * **E‑m hat für genau diesen Fall die Klickbarkeit abgeschaltet** („Überfällig
 * insgesamt"); hier wird er gelöst statt vermieden — der Link bringt sein
 * Fenster mit, und weil das eine ausdrückliche Wahl ist, steht es nach E‑n in
 * der URL.
 *
 * ## Zu weit zu greifen kostet nichts, zu kurz kostet die Zeile
 *
 * `aeltesteSekunden` ist ein **Alter** und kein Zeitpunkt; der Bezug ist die
 * Anwendungsuhr des Backends, und die darf im Browser nicht nachgerechnet werden
 * (im Profil `dev` steht sie Monate zurück). Der einzige Anker in der Antwort
 * ist `fenster.bis` — und der liegt **hinter** `jetzt`, weil er der Anfang des
 * nächsten Eimers ist. `bis - aeltesteSekunden` läge damit *nach* der ältesten
 * Zeile und schnitte sie weg.
 *
 * Deshalb wird zweimal Luft nach hinten gelassen:
 *
 * 1. **eine Eimerbreite**, als feste Obergrenze je Paar. `bis` minus eine
 *    Eimerbreite ist höchstens der Anfang des Eimers, in dem `jetzt` liegt, und
 *    liegt damit garantiert **nicht später** als `jetzt`.
 * 2. **die Abrundung auf den Tagesanfang** — bis zu 24 Stunden mehr, und
 *    nebenbei eine Adresse, die man in einem geteilten Link lesen kann.
 *
 * **Das darf großzügig sein, und zwar beweisbar:** `aeltesteSekunden` gehört zur
 * **ältesten** wartenden Zeile, gemessen über dieselbe Spalte, nach der die
 * Liste filtert (`MessageLastUpdate`). Ein früheres `von` kann deshalb **keine
 * einzige Zeile hinzufügen** — es gibt keine ältere. Ein zu spätes `von` ließe
 * dagegen genau die Zeile weg, um derentwillen jemand klickt.
 */

/**
 * Die Eimerbreite je Paar als **feste Obergrenze in Millisekunden**.
 *
 * Fest und nicht kalendarisch gerechnet: Ein Kalendermonat über `Date`
 * abzuziehen hinge an der Zone, in der man ihn abzieht — das Backend richtet die
 * Eimer in der **Anwendungszone** aus, die Antwort nennt sie in UTC. Der
 * Unterschied wäre höchstens der Zonenversatz, aber er ist unnötig: Nach oben
 * abzurunden kostet nichts (siehe oben), also steht für `12M` schlicht der
 * längste Monat.
 */
const EIMER_HOECHSTENS_MS: Record<Rollupzeitraum, number> = {
  "48H": 60 * 60 * 1000,
  "30T": 24 * 60 * 60 * 1000,
  "12M": 31 * 24 * 60 * 60 * 1000,
};

/**
 * Der Spielraum, mit dem die Notbremse die Zone des Backends abdeckt.
 *
 * Der Listen-Endpunkt misst das Jahr als **Kalenderjahr in der Anwendungszone**
 * (`common/Zeitfenster.absolutes`: `von.isBefore(bis.minusYears(1))`), hier wird
 * in UTC gerechnet. Die beiden Ergebnisse können sich um den Unterschied der
 * Zonenversätze an den beiden Enden unterscheiden — höchstens eine Stunde.
 * **Eine Stunde zu früh zu bremsen ist die richtige Richtung:** Ein Link, der
 * weniger zeigt als die Kachel nennt, darf nicht entstehen; eine Kachel, die
 * eine Stunde zu früh nicht mehr klickt, sagt in einem Satz warum.
 */
const ZONENSPIELRAUM_MS = 60 * 60 * 1000;

const TAG_MS = 24 * 60 * 60 * 1000;

/**
 * Das Zeitfenster als Parameterpaar — **ISO 8601 in UTC**, wie die Antwort es
 * liefert und wie Richtlinie §5.3 es verlangt.
 *
 * Der Wert wird nicht durch `Date` geschickt und wieder herausgeschrieben: Das
 * änderte die Schreibweise (`…:00Z` würde zu `…:00.000Z`), ohne den Zeitpunkt zu
 * ändern — und in einer geteilten URL sieht man den Unterschied.
 */
function mitFenster(parameter: URLSearchParams, fenster: Fenster): URLSearchParams {
  parameter.set("von", fenster.von);
  parameter.set("bis", fenster.bis);
  return parameter;
}

function ziel(parameter: URLSearchParams): string {
  return `${ROUTEN.nachrichten}?${parameter.toString()}`;
}

/**
 * Eine Kachel, die auf **die Einordnung** filtert.
 *
 * `status=…` ist die fachliche Einordnung und kein Rohwert; das Backend weist
 * einen Rohwert an dieser Stelle mit `400` `status-unbekannt` ab.
 *
 * **Genau ein Wert, und deshalb ist die Trennzeichenfrage hier keine.** `nuqs`
 * liest den Parameter über `searchParams.get` und trennt an Kommata; bei einem
 * einzigen Wert sind die wiederholte und die kommagetrennte Form identisch.
 */
function statusZiel(einordnung: string, fenster: Fenster): string {
  const parameter = new URLSearchParams();
  parameter.set("status", einordnung);
  return ziel(mitFenster(parameter, fenster));
}

/**
 * Die Fehlerkachel → die Liste, gefiltert auf `FEHLER`, **mit dem Fenster der
 * Antwort**.
 *
 * Es wird nichts gerundet, verkürzt oder umgerechnet — **auch bei `12M`
 * nicht.** Der Listen-Endpunkt weist eine Spanne über einem Jahr mit
 * `zeitfenster-zu-gross` ab und misst dabei ein **Kalenderjahr**; das
 * `12M`-Fenster ist genau zwölf Kalendermonate von Monatsanfang zu Monatsanfang.
 * Die Prüfung greift nicht, auch nicht im Schaltjahr. Nachgesehen am 01.09.2026
 * (`docs/dashboard-frontend.md`, Probe C.2).
 */
export function fehlerZiel(fenster: Fenster): string {
  return statusZiel("FEHLER", fenster);
}

/**
 * Die Kachel *Läuft* → die Liste auf `status=LAEUFT`, **mit dem geerbten
 * Fenster**.
 *
 * Sie erbt, weil eine laufende Nachricht höchstens so alt wie die Wächterfrist
 * ist. Ein eigenes Fenster wäre hier kein Gewinn, sondern eine zweite Regel für
 * dieselbe Frage.
 */
export function laeuftZiel(fenster: Fenster): string {
  return statusZiel("LAEUFT", fenster);
}

/**
 * Das Fenster, das die Kachel *Wartend* **mitbringt**.
 *
 * `null` heißt **Notbremse**: Die Spanne läge über der Höchstspanne der Liste
 * (ein Jahr, Regel L1), der Endpunkt wiese sie mit `zeitfenster-zu-gross` ab.
 * Dann klickt die Kachel nicht und sagt in einem Satz warum — ein Link, der
 * weniger zeigt als die Kachel nennt, entsteht nicht, auch nicht still.
 *
 * **Ohne `aeltesteSekunden` wird geerbt.** Das ist der Fall `anzahl = 0`: Es gibt
 * keine älteste Zeile, also nichts, wofür das Fenster geweitet werden müsste.
 * Das Ziel ist dann eine leere Liste — und die ist die richtige Antwort auf eine
 * Kachel, die `0` zeigt.
 *
 * `bis` bleibt in jedem Fall das Ende des gewählten Zeitraums, die übrigen
 * Filter bleiben unberührt.
 */
export function wartendFenster(
  fenster: Fenster,
  zeitraum: Rollupzeitraum,
  aeltesteSekunden: number | null,
): Fenster | null {
  if (aeltesteSekunden === null) {
    return fenster;
  }

  const bis = new Date(fenster.bis);
  const eimeranfang = bis.getTime() - EIMER_HOECHSTENS_MS[zeitraum];
  const aelteste = eimeranfang - aeltesteSekunden * 1000;
  const von = new Date(Math.floor(aelteste / TAG_MS) * TAG_MS);

  const jahresgrenze = new Date(bis);
  jahresgrenze.setUTCFullYear(jahresgrenze.getUTCFullYear() - 1);
  if (von.getTime() < jahresgrenze.getTime() + ZONENSPIELRAUM_MS) {
    return null;
  }

  // `toISOString` schreibt immer Millisekunden; der Tagesanfang hat keine, und
  // eine geteilte URL soll aussehen wie die, die aus `fenster` entsteht.
  return { von: von.toISOString().replace(".000Z", "Z"), bis: fenster.bis };
}

/**
 * Die Kachel *Wartend* → die Liste auf `status=WARTEND`, **mit dem eigenen
 * Fenster**. `null` heißt: Die Kachel klickt nicht (siehe {@link
 * wartendFenster}).
 */
export function wartendZiel(
  fenster: Fenster,
  zeitraum: Rollupzeitraum,
  aeltesteSekunden: number | null,
): string | null {
  const eigenes = wartendFenster(fenster, zeitraum, aeltesteSekunden);
  return eigenes === null ? null : statusZiel("WARTEND", eigenes);
}

/**
 * Eine Zeile aus „Zuletzt aufgefallen" → **die Liste, gefiltert auf diesen
 * Prozess und auf `FEHLER`**, mit dem Fenster der Antwort.
 *
 * ## Warum nicht mehr ins Nachrichtendetail
 *
 * Bis zum 04.09.2026 trug jede Zeile eine **Nachricht** und führte auf
 * `/nachrichten/<id>`. Seit **E‑90** trägt sie einen **Prozess** mit `n`
 * Nachrichten darunter — eine davon herauszugreifen wäre eine Behauptung, die
 * die Zeile nicht macht. Das Ziel ist deshalb die Liste, und zwar **genau die
 * Menge, die die Zahl daneben nennt**: derselbe Zeitraum, dieselbe Bedingung,
 * dieser eine Prozess.
 *
 * **Damit kommt zurück, was der Verdichtung zum Opfer fiel.** Die Kennung der
 * einzelnen Nachricht und ihr Rohstatus stehen nicht mehr in der Zeile; sie
 * stehen einen Klick entfernt und dort vollständig, mit Cursor, Filter und
 * Sortierung. Offener Punkt 136 verlangte den Rohstatus je Zeile zurück — eine
 * Zeile, die einen ganzen Prozess zusammenfasst, hat keinen.
 *
 * `prozess=` ist derselbe Parameter, den die Liste ohnehin kennt; das Backend
 * filtert darüber auf `Message.ProcessID`.
 */
export function prozessFehlerZiel(fenster: Fenster, processId: string): string {
  const parameter = new URLSearchParams();
  parameter.set("status", "FEHLER");
  parameter.set("prozess", processId);
  return ziel(mitFenster(parameter, fenster));
}
