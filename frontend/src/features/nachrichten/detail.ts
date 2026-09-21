import type { Texte } from "@/i18n";

import type { Eigenschaft, Nachrichtendetail, Namensherkunft, Schritt } from "./api";

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
 * Der Tooltip zu einem Schritt — **an genau einer Stelle für beide Aufrufer.**
 *
 * Name, `Baustein: <Rohwert>` und der Herkunftstext, durch Zeilenumbrüche
 * getrennt. Die Zusammensetzung lag bis zum 17.08.2026 inline in
 * `components/zeitleiste.tsx`; seit der Eigenschaftenblock nach Schritten
 * gruppiert, braucht sie ein zweiter Aufrufer. **Zwei Stellen, die denselben
 * Schritt verschieden benennen, sind genau der Fehler, den die Gruppierung
 * beseitigen soll** — ein Gruppenkopf muss wortgleich dasselbe sagen wie die
 * Zeile der Zeitleiste darüber.
 *
 * **Alle drei Angaben dürfen fehlen**, und dann gibt es keinen Tooltip statt
 * eines leeren. Der Fall trat bis zum 21.09.2026 bei einer Gruppe ohne
 * passenden Schritt ein; seit die Eigenschaften unter ihrer Schrittzeile stehen
 * (`verteileEigenschaften`), ruft nur noch die Zeitleiste — und die liefert Name
 * und Herkunft immer.
 *
 * `undefined` statt `null`, weil das Ergebnis unverändert in ein `title`-Attribut
 * geht — dort heißt `undefined` „kein Attribut".
 */
export function schrittHinweis(
  schritt: {
    name: string | null;
    rohwert: string | null;
    namensherkunft: Namensherkunft | null;
  },
  texte: Texte,
): string | undefined {
  const teile = [
    schritt.name,
    schritt.rohwert ? `${texte.nachrichten.detail.baustein}: ${schritt.rohwert}` : null,
    schritt.namensherkunft === null
      ? null
      : texte.nachrichten.detail.herkunft[schritt.namensherkunft],
  ];

  // `typeof` und nicht `!== null`: Eine fehlende Angabe soll auch dann
  // herausfallen, wenn sie als `undefined` ankommt — sonst stünde eine leere
  // Zeile im Tooltip, und ein `title` aus einer leeren Zeile ist etwas anderes
  // als gar keines.
  const vorhandene = teile.filter((teil): teil is string => typeof teil === "string");

  return vorhandene.length === 0 ? undefined : vorhandene.join("\n");
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
  /**
   * ⚠️ **Bei `WARTEND` immer `null`** *(seit dem 03.09.2026, E‑76)*. Eine
   * wartende Nachricht wird vom Wächter des Altsystems nie beendet, die Frist
   * wird auf sie also nicht angewendet — und ein Feld, das eine Frist nennt, die
   * niemand durchsetzt, ist eine falsche Auskunft. **Bei `LAEUFT` bleibt sie
   * und wird erst dadurch richtig:** zusammen mit `sekunden` sagt sie, wann die
   * Nachricht in `ERROR_TIMEOUT` kippt.
   */
  fristSekunden: number | null;
};

export function wartezeile(detail: Nachrichtendetail): Wartezeile | null {
  if (detail.wartetSeitSekunden === null) {
    return null;
  }
  return {
    laeuft: detail.offenerZustand === "LAEUFT_AUF",
    sekunden: detail.wartetSeitSekunden,
    fristSekunden: detail.fristSekunden,
  };
}

/* ─────────────────────────────────────────────────────────────────────────────
   Die technischen Eigenschaften, nach ausgeführtem Schritt gruppiert
   (Nacharbeit zu Schritt 5, Teil 2 — 17.08.2026)
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Die Position des Metadaten-Schritts.
 *
 * > **Belegvermerk** (Regel L10).
 * > *Gemessen (M15 1, Fenster A; S1 über 704.427 Aktionen beider Fenster, null
 * > Abweichungen):* `SOSActionID = 0` und `MessageActionID = 0` treffen dieselbe
 * > Menge.
 * > *Behauptet wird:* dass `position === 0` in der Oberfläche der
 * > Metadaten-Schritt ist.
 * > **Die Lücke:** Das Frontend sieht die `SOSActionID` gar nicht und verlässt
 * > sich auf diese Deckung. Sie ist belegt, aber sie ist eine Messung und keine
 * > Zusage des Schemas.
 */
export const METADATEN_POSITION = 0;

/**
 * Das Präfix der allgemeinen Angaben zur Nachricht — **exakt, mit Punkt und in
 * dieser Schreibung.** `Message` ohne Punkt, `MessageX.…` und `message.…`
 * gehören nicht dazu; sie fallen dadurch nicht heraus, sondern stehen sichtbar
 * im Eingang (`verteileEigenschaften`).
 */
export const ALLGEMEIN_PRAEFIX = "Message.";

/** Die Eigenschaften einer Position, für die die Zeitleiste keine Zeile führt. */
export type EigenschaftenOhneZeile = { position: number; eintraege: Eigenschaft[] };

/** Die vier disjunkten Teile, in die `verteileEigenschaften` die Antwort zerlegt. */
export type Eigenschaftenverteilung = {
  /** `position === 0` **und** Name beginnt mit `Message.` — der Block *Technische Eigenschaften*. */
  allgemein: Eigenschaft[];
  /** die übrigen mit `position === 0` — die Zeile *Eingang*. */
  eingang: Eigenschaft[];
  /** Position mit Zeile in `schritte[]`, in **deren** Reihenfolge; ohne leere Einträge. */
  jeSchritt: Map<number, Eigenschaft[]>;
  /** Position ungleich `0` ohne Zeile, aufsteigend nach Zahl — die Zeile *Ohne Schritt in der Zeitleiste*. */
  ohneZeile: EigenschaftenOhneZeile[];
};

/**
 * **Wohin jede technische Eigenschaft gehört — entschieden an genau dieser
 * Stelle** *(21.09.2026, E‑219, `docs/nachrichtendetail.md` §10.16)*.
 *
 * Sie ersetzt `gruppiereEigenschaften` (17.08.2026). Die Eigenschaften stehen
 * nicht mehr gruppiert in einem Block, sondern **unter ihrem Schritt in der
 * Zeitleiste**; im Block bleiben allein die allgemeinen Angaben zur Nachricht.
 *
 * | Teil | Regel | Wo es steht |
 * |---|---|---|
 * | `allgemein` | `position === 0` und `name.startsWith("Message.")` | Block *Technische Eigenschaften* |
 * | `eingang` | die übrigen mit `position === 0` | Zeile *Eingang* |
 * | `jeSchritt` | Position mit Zeile in `schritte[]` | unter der Schrittzeile |
 * | `ohneZeile` | Position ungleich `0` ohne Zeile | Zeile *Ohne Schritt in der Zeitleiste* |
 *
 * ## Die Invariante
 *
 * **Die Summe der vier Teile ist die Länge der Eingabe.** Nichts wird
 * zusammengefasst, nichts fällt heraus; derselbe Name in mehreren Teilen bleibt
 * mehrfach stehen (31 der 101 gemessenen Namen kommen auf mehr als einem Schritt
 * vor, M17 3). `gekappt` und `originalLaengeBytes` gehen unverändert durch.
 *
 * ## Position `0` geht immer in die ersten beiden Teile
 *
 * Auch dann, wenn `schritte[]` eines Tages eine Zeile mit Position `0` führte:
 * Die Leiste bekommt für den Metadaten-Schritt keine Zeile
 * (`docs/nachrichtendetail.md` §4), und seine Eigenschaften hätten sonst zwei
 * mögliche Orte. Umgekehrt landet ein `Message.*` auf einer Position ungleich
 * `0` **am Schritt** und nicht im Block — gemessen kommt das nicht vor (die
 * Familie steht ausnahmslos auf Schritt `0`, M17 3), aber die Regel hängt an
 * beiden Bedingungen und nicht am Namen allein.
 *
 * ## Gruppiert wird über `MessageActionID` — nicht über `SOSActionID`
 *
 * Unverändert seit dem 17.08.2026: `MessageProperty` hat gar keine `SOSActionID`
 * (M14), die beiden Kennungen sind nicht deckungsgleich (614 von 20.352
 * Aktionen weichen ab, M15 1), und `MessageActionID` ist die *Ausführung* und
 * über den Primärschlüssel je Nachricht eindeutig. Im Frontend heißt das Feld
 * auf beiden Seiten `position`.
 *
 * ## Die Reihenfolge kommt von außen und wird hier nicht erfunden
 *
 * - **In jedem Teil bleibt die Reihenfolge der Antwort.** Sie ist die des
 *   Backends (`MessagePropertyName`, dann `MessageActionID`); ein
 *   Ersatzsortierer ist ausdrücklich nicht gebaut.
 * - **`jeSchritt` folgt `schritte[]` und nicht der Zahl.** Die Ordnung der
 *   Schritte steht an genau einer Stelle, im `ORDER BY` von `findeAktionen`.
 * - **`ohneZeile` ist aufsteigend nach Zahl** — dort ist die Zahl das einzige,
 *   was es an Ordnung gibt.
 * - **Ein Schritt ohne Eigenschaften kommt in `jeSchritt` nicht vor.** Der Fall
 *   ist gemessen: `MessageActionID = 502` steht in `MessageAction` und fehlt in
 *   `MessageProperty` (M17 3). Seine Zeile bleibt Text.
 */
export function verteileEigenschaften(
  eigenschaften: readonly Eigenschaft[],
  schritte: readonly Schritt[],
): Eigenschaftenverteilung {
  const allgemein: Eigenschaft[] = [];
  const eingang: Eigenschaft[] = [];

  // Eine Map hält die Einfügereihenfolge je Eimer — damit bleibt die
  // Reihenfolge innerhalb eines Teils die der Eingabe, ohne einen Sortierlauf.
  const nachPosition = new Map<number, Eigenschaft[]>();

  for (const eigenschaft of eigenschaften) {
    if (eigenschaft.position === METADATEN_POSITION) {
      (eigenschaft.name.startsWith(ALLGEMEIN_PRAEFIX) ? allgemein : eingang).push(eigenschaft);
      continue;
    }
    const eimer = nachPosition.get(eigenschaft.position);
    if (eimer === undefined) {
      nachPosition.set(eigenschaft.position, [eigenschaft]);
    } else {
      eimer.push(eigenschaft);
    }
  }

  // In der Reihenfolge der Zeitleiste — und in keiner anderen.
  const jeSchritt = new Map<number, Eigenschaft[]>();
  for (const schritt of schritte) {
    const eintraege = nachPosition.get(schritt.position);
    if (eintraege !== undefined && !jeSchritt.has(schritt.position)) {
      jeSchritt.set(schritt.position, eintraege);
    }
  }

  const ohneZeile = [...nachPosition.entries()]
    .filter(([position]) => !jeSchritt.has(position))
    .sort(([eine], [andere]) => eine - andere)
    .map(([position, eintraege]) => ({ position, eintraege }));

  return { allgemein, eingang, jeSchritt, ohneZeile };
}

/**
 * Ob eine Schrittzeile aufklappbar ist — **nur, was Inhalt hat** (E‑221).
 *
 * `undefined` heißt: Die Eigenschaften laden noch oder ihre Abfrage ist
 * gescheitert. **Dann ist nichts aufklappbar**, und die Zeile bleibt Text; eine
 * Schaltfläche, die einen leeren Bereich öffnet, ist schlimmer als keine.
 */
export function schrittAufklappbar(
  verteilung: Eigenschaftenverteilung | undefined,
  position: number,
): boolean {
  return (verteilung?.jeSchritt.get(position)?.length ?? 0) > 0;
}

/**
 * Die beiden gestrichelten Zeilen um die Leiste — *Eingang* darüber, *Ohne
 * Schritt in der Zeitleiste* darunter (E‑222, E‑223).
 *
 * **Es gibt die Zeile, wenn dort ein Artefakt oder eine Eigenschaft liegt;
 * aufklappbar ist sie nur mit Eigenschaften.** Bis zum 21.09.2026 hing ihre
 * Existenz allein an den Artefakten (`docs/rohdaten-frontend.md` §3).
 */
export function zusatzzeile(
  ziele: number,
  eigenschaften: number,
): { vorhanden: boolean; aufklappbar: boolean } {
  return { vorhanden: ziele > 0 || eigenschaften > 0, aufklappbar: eigenschaften > 0 };
}
