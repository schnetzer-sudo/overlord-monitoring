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
 * eines leeren. Der Fall tritt an genau einer Stelle ein: bei einer Gruppe ohne
 * passenden Schritt (`gruppiereEigenschaften`, Regel 5). Die Zeitleiste liefert
 * Name und Herkunft immer.
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

/** Eine Gruppe des Eigenschaftenblocks: ein ausgeführter Schritt und was an ihm hängt. */
export type EigenschaftenGruppe = {
  position: number;
  /** Name des Schritts aus `schritte[]`; `null`, wenn es dazu keinen Schritt gibt. */
  beschriftung: string | null;
  namensherkunft: Namensherkunft | null;
  rohwert: string | null;
  /** true genau für position === 0 */
  istNachricht: boolean;
  eintraege: Eigenschaft[];
};

/**
 * Die technischen Eigenschaften einer Nachricht, **nach dem ausgeführten Schritt
 * gruppiert**.
 *
 * ## Warum überhaupt gruppiert wird
 *
 * Flach untereinander steht `Converter.Log.GUID` zweimal und `Service.Type`
 * dreimal und sieht aus wie eine Dublette. Es sind Einträge **verschiedener
 * Prozessschritte**: 31 der 101 gemessenen Namen kommen auf mehr als einem
 * Schritt vor, und `Converter.Payload.GUID` steht mit 7.862 Zeilen auf 6.149
 * Nachrichten (M17 3). Mit dem Schrittnamen darüber sieht der Nutzer, **wo** ein
 * Wert entstanden ist — dass etwa per OFTP empfangen und später per FTP versendet
 * wurde.
 *
 * ## Gruppiert wird über `MessageActionID` — nicht über `SOSActionID`
 *
 * Drei Gründe, und der dritte ist der tragende:
 *
 * 1. **`MessageProperty` hat gar keine `SOSActionID`** (M14) — nur
 *    `MessageActionID`. Über die Ablaufkennung zu gruppieren verlangte einen
 *    Join, den niemand braucht.
 * 2. **Die beiden Kennungen sind nicht deckungsgleich.** M15 (1) misst
 *    `MessageActionID = 2` mal neben `SOSActionID` 1, mal neben 2, und
 *    `MessageActionID = 3` neben 2, 3 oder 10 — in **614 von 20.352** Aktionen
 *    des Tagesfensters weichen sie voneinander ab.
 * 3. **`MessageActionID` ist die Ausführung** und über den Primärschlüssel
 *    `(MessageID, MessageActionID)` je Nachricht eindeutig. `SOSActionID` ist der
 *    Schlüssel in die *Ablaufdefinition*; führte ein Ablauf denselben
 *    Definitionsschritt zweimal aus, verschmölzen beide Ausführungen zu einer
 *    Gruppe.
 *
 * Im Frontend heißt das Feld auf beiden Seiten `position` — an `Eigenschaft` wie
 * an `Schritt`, und beide Male steht die Spalte `MessageActionID` dahinter
 * (`nachrichtendetail.md` §4).
 *
 * ## Die Reihenfolge der Gruppen kommt von außen und wird hier nicht erfunden
 *
 * `position === 0` zuerst (die Nachricht selbst), danach **in der Reihenfolge von
 * `schritte[]`**, so wie sie ankommt, zuletzt die übrigen Positionen aufsteigend.
 *
 * **Es wird nicht numerisch nachsortiert.** Die Ordnung der Schritte
 * (`MessageActionStart`, bei Gleichstand `MessageActionID`) steht an **genau
 * einer** Stelle, nämlich im `ORDER BY` von `findeAktionen`
 * (`nachrichtendetail.md` §4). Eine zweite Sortierung hier wäre die Drift, gegen
 * die diese Regel gerichtet ist — und die Gruppen stünden dann in einer anderen
 * Reihenfolge als die Zeilen der Zeitleiste darüber.
 *
 * ## Was die Funktion ausdrücklich nicht tut
 *
 * - **Sie sortiert innerhalb einer Gruppe nicht um.** Die Eingabereihenfolge
 *   bleibt erhalten; sie ist die des Backends (`MessagePropertyName`, dann
 *   `MessageActionID`) und keine Zusage dieser Datei.
 * - **Sie fasst nichts zusammen und lässt nichts weg.** Die Summe der
 *   Gruppengrößen ist die Länge der Eingabe; derselbe Name in zwei Gruppen bleibt
 *   zweimal stehen — genau das ist der Punkt.
 * - **Sie erfindet keinen Namen.** Eine Position ohne passenden Schritt bekommt
 *   `beschriftung = null` und landet am Ende.
 * - **Sie erzeugt keine leere Gruppe.** Ein Schritt ohne Eigenschaften ist
 *   gemessen: `MessageActionID = 502` existiert in `MessageAction`, kommt in
 *   `MessageProperty` aber nicht vor (M17 3).
 */
export function gruppiereEigenschaften(
  eigenschaften: Eigenschaft[],
  schritte: Schritt[],
): EigenschaftenGruppe[] {
  // Eine Map hält die Einfügereihenfolge je Eimer — damit bleibt die
  // Reihenfolge innerhalb einer Gruppe die der Eingabe, ohne einen Sortierlauf.
  const nachPosition = new Map<number, Eigenschaft[]>();
  for (const eigenschaft of eigenschaften) {
    const eimer = nachPosition.get(eigenschaft.position);
    if (eimer === undefined) {
      nachPosition.set(eigenschaft.position, [eigenschaft]);
    } else {
      eimer.push(eigenschaft);
    }
  }

  const gruppen: EigenschaftenGruppe[] = [];
  const vergeben = new Set<number>();

  function nimm(position: number, schritt: Schritt | undefined): void {
    const eintraege = nachPosition.get(position);
    // Ohne Einträge keine Gruppe: Ein Schritt ohne Eigenschaften bekommt keine
    // leere Überschrift.
    if (eintraege === undefined || vergeben.has(position)) {
      return;
    }
    vergeben.add(position);
    gruppen.push({
      position,
      beschriftung: schritt?.name ?? null,
      namensherkunft: schritt?.namensherkunft ?? null,
      rohwert: schritt?.rohwert ?? null,
      istNachricht: position === METADATEN_POSITION,
      eintraege,
    });
  }

  // Die Nachricht selbst zuerst. Sie steht in `schritte[]` nicht drin — der
  // Metadaten-Schritt ist kein Prozessschritt und wird schon im Backend
  // ausgenommen (`nachrichtendetail.md` §4). Nachgesehen wird trotzdem, damit
  // die Gruppe ihren Namen bekäme, falls die Antwort ihn eines Tages mitschickt.
  nimm(
    METADATEN_POSITION,
    schritte.find((schritt) => schritt.position === METADATEN_POSITION),
  );

  // Danach in der Reihenfolge der Zeitleiste — und in keiner anderen.
  for (const schritt of schritte) {
    if (schritt.position === METADATEN_POSITION) {
      continue;
    }
    nimm(schritt.position, schritt);
  }

  // Zuletzt, was zu keinem gelieferten Schritt gehört: aufsteigend nach Zahl,
  // ohne Beschriftung. Hier ist die Zahl das einzige, was es an Ordnung gibt.
  const uebrige = [...nachPosition.keys()]
    .filter((position) => !vergeben.has(position))
    .sort((eine, andere) => eine - andere);
  for (const position of uebrige) {
    nimm(position, undefined);
  }

  return gruppen;
}
