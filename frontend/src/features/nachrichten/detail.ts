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
 * ## Die Lücke zwischen zwei Schritten ist eine eigene Zeile
 *
 * Ohne sie steht die Wartezeit in keiner Schrittdauer, und genau sie ist bei
 * einer hängenden Nachricht oft die ganze Antwort.
 */

/**
 * Ab wann eine Lücke zwischen zwei Schritten eine eigene Zeile bekommt.
 *
 * Eine Minute: Darunter ist der Abstand die normale Übergabe zwischen zwei
 * Diensten und keine Auskunft — eine Zeile je Schrittwechsel machte die Leiste
 * doppelt so lang und sagte nichts.
 */
export const LUECKE_SCHWELLE_SEKUNDEN = 60;

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
  | { art: "luecke"; id: string; sekunden: number }
  /**
   * Die Zeile, die bei `WARTET_VOR` ans Ende kommt.
   *
   * `name` ist nullbar: Die Nachricht wartet, wir wissen nur nicht worauf — und
   * das wird benannt und nicht weggelassen.
   *
   * **`bereitsGelaufen` ist der Befund aus der Sichtprüfung vom 07.08.2026** und
   * kein Feinschliff. Siehe {@link zeitleiste}.
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
 * Die Lücke zwischen zwei aufeinanderfolgenden Schritten in ganzen Sekunden —
 * oder `null`, wenn keine Zeile daraus wird.
 *
 * Keine Zeile gibt es, wenn der vorige Schritt gar kein Ende trägt (dann gibt es
 * keinen Zwischenraum, sondern einen offenen Schritt), wenn einer der beiden
 * Zeitpunkte unlesbar ist, und wenn die Lücke negativ, null oder unter der
 * Schwelle liegt.
 */
export function luecke(vorher: Schritt, nachher: Schritt): number | null {
  if (vorher.ende === null) {
    return null;
  }
  const ende = Date.parse(vorher.ende);
  const start = Date.parse(nachher.start);
  if (Number.isNaN(ende) || Number.isNaN(start)) {
    return null;
  }
  const sekunden = Math.floor((start - ende) / 1000);
  return sekunden >= LUECKE_SCHWELLE_SEKUNDEN ? sekunden : null;
}

/**
 * Die Zeilen der Zeitleiste, in Anzeigereihenfolge.
 *
 * **Es wird nichts abgeschnitten.** Die Beschreibung „bei `LAEUFT_AUF` endet die
 * Leiste dort" trifft in den Daten von selbst zu — der laufende Schritt ist der
 * letzte, er trägt kein Ende und bekommt deshalb weder eine Lückenzeile noch
 * eine Fortsetzung. Schritte hinter ihm wegzulassen wäre etwas anderes: das
 * stillschweigende Verschweigen gemessener Zeilen.
 *
 * **Der offene Zustand wird gezeigt, nicht errechnet.** `WARTET_VOR` hängt eine
 * eigene Zeile an, `OHNE_SCHRITT` und `KEINER` hängen nichts an — was bei leerer
 * Schrittfolge zu sehen ist, entscheidet die Komponente über den Zustand und
 * nicht über die Länge dieser Liste.
 *
 * ## `bereitsGelaufen` — der Befund aus der Sichtprüfung vom 07.08.2026
 *
 * **Bei den wartenden Nachrichten der Testkopie benennt `naechsterSchritt`
 * denselben Schritt, der gerade gelaufen ist.** Nachgesehen an einer
 * `SUSPENDED`-Nachricht: Schritt 2 heißt „Send Message to Pool" und trägt den
 * Rohwert `NXS_MERGE|BMW|WAITUNTIL|…|SUSPEND` — er ist der Schritt, der die
 * Nachricht *schlafen legt*. Und `Message.SOSActionID` zeigt auf genau ihn.
 *
 * Ohne diese Unterscheidung stünde „Send Message to Pool · noch nicht begonnen"
 * unmittelbar unter „Send Message to Pool · 2 min" — für den Nutzer, der laut
 * Leitsatz kein EDI-Spezialist ist, schlicht ein Widerspruch.
 *
 * **Abgeleitet wird hier nichts über den Zustand.** Der kommt weiter aus dem
 * Backend; verglichen werden zwei gelieferte Felder, und das Ergebnis
 * entscheidet nur über die **Wortwahl** einer Zeile. Verglichen wird über den
 * *Namen* und nicht über eine Kennung: Zwei gleich benannte Zeilen
 * untereinander sind für den Leser dieselbe Zeile, gleich welche Kennung
 * dahintersteht.
 */
export function zeitleiste(detail: Nachrichtendetail): Zeitleistenzeile[] {
  const laengste = laengsteDauer(detail.schritte);
  const zeilen: Zeitleistenzeile[] = [];

  detail.schritte.forEach((schritt, nummer) => {
    const vorher = detail.schritte[nummer - 1];
    if (vorher !== undefined) {
      const sekunden = luecke(vorher, schritt);
      if (sekunden !== null) {
        zeilen.push({ art: "luecke", id: `luecke-${schritt.position}`, sekunden });
      }
    }
    zeilen.push({
      art: "schritt",
      id: `schritt-${schritt.position}`,
      schritt,
      anteil: balkenanteil(schritt.dauerSekunden, laengste),
    });
  });

  if (detail.offenerZustand === "WARTET_VOR") {
    zeilen.push({
      art: "erwartet",
      id: "erwartet",
      name: detail.naechsterSchritt,
      bereitsGelaufen:
        detail.naechsterSchritt !== null &&
        detail.schritte.some((schritt) => schritt.name === detail.naechsterSchritt),
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
