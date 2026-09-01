import { ROUTEN } from "@/lib/routen";

import type { Fenster } from "./api";

/**
 * Wohin eine Kachel führt (Entscheidung E‑m).
 *
 * | Kachel | Ziel |
 * |---|---|
 * | **Fehler** | `/nachrichten?status=FEHLER&von=…&bis=…` |
 * | **Überfällig, im Fenster** | `/nachrichten?ueberfaellig=true&von=…&bis=…` |
 * | **Überfällig, insgesamt** | **kein Verweis** |
 * | **Nachrichten** | kein Verweis |
 *
 * ## Warum „insgesamt" nicht klickt
 *
 * Die Zahl hat bewusst kein Zeitfenster (`docs/dashboard.md` §5, Regel L9), die
 * Liste hat ein **Pflicht**-Zeitfenster (Regel L1). Jedes Ziel zeigte damit eine
 * **andere Zahl** als die Kachel — und eine Kachel, die auf eine andere Zahl
 * führt als sie nennt, ist schlechter als eine, die nicht klickt. Ein Satz an
 * der Kachel sagt das.
 *
 * ## Die beiden Filter werden nie kombiniert
 *
 * `status=FEHLER` und `ueberfaellig=true` sind am Listen-Endpunkt ausdrücklich
 * **unvereinbar** und ergeben `400` `ueberfaellig-und-status-unvereinbar`:
 * Überfällig setzt `WARTEND` oder `LAEUFT` voraus, Fehler ist ein Endstatus. Die
 * beiden Adressen entstehen deshalb in **zwei getrennten Funktionen**, und keine
 * von beiden nimmt den anderen Parameter entgegen — eine gemeinsame Funktion mit
 * zwei Schaltern wäre die Stelle, an der jemand beide setzt.
 *
 * ## Das Zeitfenster wird **unverändert** durchgereicht
 *
 * `fenster.von` und `fenster.bis` stehen so in der Adresse, wie die Antwort sie
 * nennt. Nichts wird gerundet, verkürzt oder umgerechnet: Ein Ziel, das ein
 * anderes Fenster zeigt als die Kachel, ist derselbe Fehler wie eine Zahl, die
 * nicht stimmt.
 *
 * **Auch bei `12M` nicht.** Der Listen-Endpunkt weist eine Spanne über einem
 * Jahr mit `zeitfenster-zu-gross` ab — und misst dabei ein **Kalenderjahr**
 * (`Zeitfenster.absolutes`: `von.isBefore(bis.minusYears(1))`), ausdrücklich
 * nicht 365 Tage. Das `12M`-Fenster des Dashboards ist genau zwölf
 * Kalendermonate von Monatsanfang zu Monatsanfang und damit genau ein
 * Kalenderjahr; die Prüfung greift nicht, auch nicht im Schaltjahr. Nachgesehen
 * am 01.09.2026 (`docs/dashboard-frontend.md`, Probe C.2).
 *
 * ## Ein `<a href>` und keine Schaltfläche
 *
 * Beide Adressen sind echte Verweise. „Schick mir mal den Link" ist bei diesem
 * Werkzeug die eigentliche Anwendung; ein Ziel, das man weder mit der mittleren
 * Maustaste öffnen noch kopieren kann, verfehlt genau das.
 */

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
 * Die Fehlerkachel → die Liste, auf **die Einordnung** gefiltert.
 *
 * `status=FEHLER` ist die fachliche Einordnung und kein Rohwert; das Backend
 * weist einen Rohwert an dieser Stelle mit `400` `status-unbekannt` ab.
 *
 * **Genau ein Wert, und deshalb ist die Trennzeichenfrage hier keine.** `nuqs`
 * liest den Parameter über `searchParams.get` und trennt an Kommata; bei einem
 * einzigen Wert sind die wiederholte und die kommagetrennte Form identisch.
 */
export function fehlerZiel(fenster: Fenster): string {
  const parameter = new URLSearchParams();
  parameter.set("status", "FEHLER");
  return ziel(mitFenster(parameter, fenster));
}

/**
 * Die Kachel „Überfällig, im Fenster" → die Liste in ihrer zweiten Abfrageform.
 *
 * **`ueberfaellig` ist kein Filter, sondern eine zweite Abfrageform**
 * (`docs/nachrichtenliste.md` §5b) — und ausdrücklich **ohne** `status`.
 */
export function ueberfaelligZiel(fenster: Fenster): string {
  const parameter = new URLSearchParams();
  parameter.set("ueberfaellig", "true");
  return ziel(mitFenster(parameter, fenster));
}

/**
 * Eine einzelne Nachricht aus „Zuletzt aufgefallen" → das Detail auf seiner
 * **eigenen Route**.
 *
 * Nicht `/nachrichten?nachricht=…`: Das öffnete das Panel *neben der Liste* und
 * brächte damit eine Liste mit, die niemand angefragt hat — samt ihrem
 * Standardfenster, das mit dem des Dashboards nichts zu tun hat.
 */
export { nachrichtAnsicht as nachrichtZiel } from "@/lib/routen";
