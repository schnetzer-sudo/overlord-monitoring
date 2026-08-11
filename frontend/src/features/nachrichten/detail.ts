import type { Nachrichtendetail, Schritt } from "./api";

/**
 * Die Zeitleiste der Detailansicht — **als reine Funktion, ohne React.**
 *
 * Sie beantwortet eine einzige Frage: **welcher Schritt hat die Zeit
 * gefressen.** Alles hier folgt daraus, und alles hier ist deshalb prüfbar
 * (`tests/nachrichtendetail.test.ts`) statt in einer Komponente verstreut.
 *
 * ## Der Balken ist auf die Nachricht normiert, nicht auf eine absolute Skala
 *
 * Der längste Schritt **dieser** Nachricht bekommt die volle Breite, alle
 * anderen anteilig davon. Auf einer absoluten Zeitachse ergäbe ein Wartschritt
 * von 30 Minuten neben vier Schritten von Sekundenbruchteilen einen vollen
 * Balken und vier unsichtbare Striche — korrekt und nutzlos. Normiert
 * beantwortet die Leiste die Frage, für die sie da ist.
 *
 * Der Preis ist bewusst in Kauf genommen: Zwei Nachrichten sind über ihre
 * Balken **nicht** vergleichbar. Deshalb steht die Dauer immer auch als Text
 * daneben — ein Balken ohne Zahl ist ein Gefühl.
 *
 * ## Es gibt keine Lückenzeile mehr
 *
 * Bis zum 10.08.2026 stand hier eine `luecke`-Funktion: Lag zwischen dem Ende
 * eines Schritts und dem Beginn des nächsten mehr als eine Minute, bekam der
 * Zwischenraum eine eigene Zeile. **Über rund 700 geprüfte Nachrichten ist sie
 * nie erschienen** — die größte gemessene Lücke beträgt *eine Sekunde*.
 *
 * Das war kein knapper Fehlschlag, sondern strukturell: **Die Wartezeit steckt
 * in der Dauer des `WAITUNTIL`-Schritts, nicht zwischen zwei Schritten.** An
 * ihre Stelle ist die Wartezeile am offenen Zustand getreten
 * (`wartetSeitSekunden`), und die Zeit, die doch einmal zwischen zwei Schritten
 * steckt, wird über `gesamtdauerSekunden` im Kopf sichtbar. Vollständig
 * begründet in `nachrichtendetail.md` §10.12.
 */

/**
 * Die Mindestbreite eines Balkens, als Anteil der vollen Breite.
 *
 * Sehr kurze Schritte sollen sich als **„praktisch nichts"** lesen, nicht als
 * „nicht vorhanden". Ein Balken der Breite null wäre von einem fehlenden Balken
 * nicht zu unterscheiden — und ein fehlender Balken bedeutet hier etwas anderes:
 * dass es gar keine Dauer gibt.
 */
export const BALKEN_MINDESTANTEIL = 0.02;

/** Eine Zeile der Zeitleiste. */
export type Zeitleistenzeile =
  | {
      art: "schritt";
      id: string;
      schritt: Schritt;
      /** Anteil an der vollen Breite, `null` bei fehlender Dauer. */
      anteil: number | null;
    }
  /**
   * Die Zeile, die bei den beiden Wartezuständen ans Ende kommt.
   *
   * `name` ist nullbar: Die Nachricht wartet, wir wissen nur nicht worauf — und
   * das wird benannt und nicht weggelassen.
   *
   * **`bereitsGelaufen` kommt aus dem gelieferten Zustand und nicht aus einem
   * Vergleich hier.** Bis zum 10.08.2026 verglich diese Datei `naechsterSchritt`
   * mit den Namen der Schritte; seit M29 entscheidet das Backend über die
   * Kennungen, und `WARTET_IN` ist genau dieser Fall.
   */
  | { art: "erwartet"; id: string; name: string | null; bereitsGelaufen: boolean };

/** Die längste gemessene Dauer dieser Nachricht. `0`, wenn keine vorliegt. */
export function laengsteDauer(schritte: readonly Schritt[]): number {
  let laengste = 0;
  for (const schritt of schritte) {
    if (schritt.dauerSekunden !== null && schritt.dauerSekunden > laengste) {
      laengste = schritt.dauerSekunden;
    }
  }
  return laengste;
}

/**
 * Der Anteil eines Balkens an der vollen Breite.
 *
 * `null` heißt **kein Balken**: Es gibt keine Dauer, und eine erfundene Null
 * wäre eine Aussage, die die Daten nicht hergeben.
 *
 * Ist die längste Dauer `0` — jeder Schritt lief unter einer Sekunde —, bekommt
 * jeder Balken die Mindestbreite. Das ist die richtige Auskunft: Es gibt hier
 * nichts zu vergleichen.
 */
export function balkenanteil(dauerSekunden: number | null, laengste: number): number | null {
  if (dauerSekunden === null) {
    return null;
  }
  if (laengste <= 0) {
    return BALKEN_MINDESTANTEIL;
  }
  return Math.min(1, Math.max(BALKEN_MINDESTANTEIL, dauerSekunden / laengste));
}

/**
 * Die Zeilen der Zeitleiste, in Anzeigereihenfolge.
 *
 * **Es wird nichts abgeschnitten.** Die Beschreibung „bei `LAEUFT_AUF` endet die
 * Leiste dort" trifft in den Daten von selbst zu — der laufende Schritt ist der
 * letzte, er trägt kein Ende und bekommt deshalb keine Fortsetzung. Schritte
 * hinter ihm wegzulassen wäre etwas anderes: das stillschweigende Verschweigen
 * gemessener Zeilen.
 *
 * **Der offene Zustand wird gezeigt, nicht errechnet.** `WARTET_IN` und
 * `WARTET_VOR` hängen eine eigene Zeile an, `EMPFANGEN`, `OHNE_AKTION` und
 * `KEINER` hängen nichts an — was bei leerer Schrittfolge zu sehen ist,
 * entscheidet die Komponente über den Zustand und nicht über die Länge dieser
 * Liste. **Die Leiste selbst ändert sich durch die Aufteilung von `OHNE_SCHRITT`
 * nicht**: Der Metadaten-Schritt war nie eine Zeile und wird auch keine (S1).
 *
 * ## Die Wortwahl der letzten Zeile kommt aus dem Zustand
 *
 * Der Unterschied ist die Präposition, und sie ist ehrlich: Die Nachricht wartet
 * *in* einem Schritt, der sie schlafen gelegt hat (`WARTET_IN`), oder *vor*
 * einem, der noch nicht begonnen hat (`WARTET_VOR`). Bei `WARTET_IN` wird der
 * Name nicht wiederholt — er steht eine Zeile darüber, mit seiner Dauer; sonst
 * stünde „Send Message to Pool · noch nicht begonnen" unmittelbar unter
 * „Send Message to Pool · 2 min", und das ist für jeden, der laut Leitsatz kein
 * EDI-Spezialist ist, schlicht ein Widerspruch.
 */
export function zeitleiste(detail: Nachrichtendetail): Zeitleistenzeile[] {
  const laengste = laengsteDauer(detail.schritte);
  const zeilen: Zeitleistenzeile[] = detail.schritte.map((schritt) => ({
    art: "schritt",
    id: `schritt-${schritt.position}`,
    schritt,
    anteil: balkenanteil(schritt.dauerSekunden, laengste),
  }));

  if (detail.offenerZustand === "WARTET_IN" || detail.offenerZustand === "WARTET_VOR") {
    zeilen.push({
      art: "erwartet",
      id: "erwartet",
      name: detail.naechsterSchritt,
      bereitsGelaufen: detail.offenerZustand === "WARTET_IN",
    });
  }

  return zeilen;
}

/**
 * Ob die Antwort neben dem Rohwert für `bedeutungNichtVerifiziert` steht.
 *
 * **Das Detail führt dieses Feld nicht** — es ist genau die Einordnung
 * `UNGEKLAERT` und damit aus der Antwort ableitbar; ein zweites Feld dafür wäre
 * eine zweite Wahrheit. Die Liste führt es, weil sie es je Zeile braucht.
 * Abgeleitet wird es deshalb hier, an einer Stelle, und nicht in der Komponente.
 */
export function bedeutungNichtVerifiziert(statusKind: string): boolean {
  return statusKind === "UNGEKLAERT";
}

/**
 * Die Wartezeile am offenen Zustand — *wartet seit 4 h 12 min · Frist 30 min*.
 *
 * **Beide Zahlen kommen fertig aus dem Backend.** Hier wird nur entschieden, ob
 * die Zeile überhaupt erscheint und welches Verb sie trägt; gerechnet wird
 * nichts. Das ist keine Förmlichkeit: Die Anwendungsuhr steht im Profil `dev`
 * Monate zurück, und eine Dauer aus `Date.now()` wäre dort um Monate falsch.
 *
 * `null` heißt **keine Zeile**: Ohne Wartedauer gibt es nichts zu sagen. Die
 * Frist darf dabei fehlen — eine Nachricht ohne gesetzten Timeout wartet
 * trotzdem, und dann steht eben nur die eine Hälfte da.
 *
 * **Bei `EMPFANGEN` erscheint sie, bei `OHNE_AKTION` nicht** — und zwar ohne
 * eine Bedingung auf den Zustand: Das Backend liefert dort eine Wartedauer und
 * hier keine. Eine Zeile mit Platzhalter wäre schlechter als keine.
 */
export type Wartezeile = {
  /** `true` bei `LAEUFT_AUF`: Sie läuft, sie wartet nicht. */
  laeuft: boolean;
  sekunden: number;
  fristSekunden: number | null;
  ueberfaellig: boolean;
};

export function wartezeile(detail: Nachrichtendetail): Wartezeile | null {
  if (detail.wartetSeitSekunden === null) {
    return null;
  }
  return {
    laeuft: detail.offenerZustand === "LAEUFT_AUF",
    sekunden: detail.wartetSeitSekunden,
    fristSekunden: detail.fristSekunden,
    ueberfaellig: detail.ueberfaellig,
  };
}
