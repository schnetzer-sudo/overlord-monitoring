import { einsetzen, type Texte } from "@/i18n";

import type { Baumebene, Baumknoten, Gruppenknoten, Prozessbaum, Prozessknoten } from "./api";

/**
 * Der Baum als **Zeilen** — die Entscheidungen der Prozessansicht, ohne React.
 *
 * Hier steht, was die Ansicht aus der Antwort macht: welche Ebene erscheint,
 * welche übersprungen wird, was eine Eingrenzung übrig lässt und in welcher
 * Reihenfolge die sichtbaren Zeilen stehen. **Die Reihenfolge selbst kommt aus
 * der Antwort** (`docs/process-view.md` E‑39, E‑142) und wird hier nirgends
 * geändert.
 *
 * ## Ein Renderpfad für beide Gliederungen *(seit 15.09.2026, E‑140)*
 *
 * Die Antwort ist rekursiv: `knoten` an der Wurzel, `kinder` an jeder Gruppe,
 * und die Ebenennamen stehen als `ebenen` daneben. Jede Funktion hier läuft über
 * diese Form und über nichts anderes — **es gibt keinen zweiten Pfad für den
 * Projektbaum**. Zwei Pfade liefen beim nächsten Feld auseinander, und der
 * Unterschied fiele erst im Bild auf.
 *
 * **Frei von React**, wie `filter.ts` und aus demselben Grund: Das sind
 * Entscheidungen, und Entscheidungen werden geprüft, Markup nicht
 * (`docs/frontend-grundlagen.md` §9).
 */

/** Eine Gruppe trägt `kinder`, ein Prozess `processId` — mehr braucht die Unterscheidung nicht. */
export function istGruppe(knoten: Baumknoten): knoten is Gruppenknoten {
  return "kinder" in knoten;
}

/** Der Name der Ebene in dieser Tiefe; jenseits der Liste stehen Prozesse. */
function ebeneBei(ebenen: readonly Baumebene[], tiefe: number): Baumebene {
  return ebenen[tiefe] ?? "PROZESS";
}

/**
 * ## E‑58 — Die Ebene fällt nur weg, wo es **nichts zu schreiben** gibt
 *
 * *Entschieden am 03.09.2026, `docs/process-view.md` §29. Engt E‑45 ein und
 * ersetzt E‑55.*
 *
 * **Eine bekannte Richtung steht immer als eigene Ebene**, auch wenn der Partner
 * nur eine hat. Der Grund ist die **Einheitlichkeit**, und sie ist am Bild
 * entschieden worden: Bei `ACOME` stand „Eingehend" als Zeile, bei `ADIENT`
 * dasselbe Wort als Vorsatz in der Prozesszeile — zwei Schreibweisen für
 * denselben Sachverhalt, direkt untereinander.
 * Das Bild danebengestellt steht in `docs/process-view.md` §29 und bewusst nicht
 * hier: Der Prozessname darin endet auf ein Kürzel, das `tests/farbwerte.test.ts`
 * samt folgender Klammer als CSS-Farbfunktion liest — ein Fehlalarm, den ein
 * Beispiel nicht wert ist.
 *
 * ## Was von E‑45 bleibt, und es ist der Fall, für den sie gebaut war
 *
 * **Ist die Richtung `null`, fällt die Ebene weiter weg.** Ein Knoten „nicht
 * ermittelt" über einem einzigen Kind ordnet nichts und schreibt nichts hin —
 * bei `VOTG` wären es **133** solche Knoten, einer je Partner, weil dort kein
 * einziger Prozess eine kuratierte Richtung trägt (M110). Offener Punkt 108
 * bleibt damit geschlossen.
 *
 * ## E‑145 — Die Regel gilt für die Richtungsebene und für keine andere *(15.09.2026)*
 *
 * Seit es die Projektgliederung gibt, stünde die Verallgemeinerung nahe: „eine
 * Ebene mit nur einem Knoten überspringen". **Sie ist nicht gebaut.** E‑45 ist für
 * die Richtung entschieden — einen Knoten, der nichts hinschreibt —, und für die
 * Projektebene ist sie weder entschieden noch gewünscht: Ein Mandant mit einem
 * einzigen Projekt (`SUTTONS`, `WOC`) sieht es als Zeile. Deshalb fragt die
 * Funktion ausdrücklich nach der **Ebene der Kinder** und nicht nach ihrer Zahl
 * allein.
 *
 * ## E‑46 — Das Überspringen geschieht in der Oberfläche, nicht im Endpunkt
 *
 * Der ist abgenommen und gemessen; seine Antwort bleibt vollständig und sagt
 * weiter, was der Katalog weiß. Die Darstellung entscheidet, ob sie dafür eine
 * Ebene aufmacht — dieselbe Trennung wie überall: das Backend stellt fest, die
 * Oberfläche beschriftet.
 *
 * @param kinderebene der Name der Ebene, auf der die Kinder von `gruppe` stehen
 */
export function ebeneFaelltWeg(gruppe: Gruppenknoten, kinderebene: Baumebene): boolean {
  if (kinderebene !== "RICHTUNG" || gruppe.kinder.length > 1) {
    return false;
  }
  const einzige = gruppe.kinder[0];
  // Ein gepflegter, aber unbekannter vierter Katalogwert ist **bekannt** und
  // bekommt seine Ebene: Er steht dort, wie er im Katalog steht (Regel Q4).
  return einzige === undefined || (istGruppe(einzige) && einzige.name === null);
}

/**
 * Der Schlüssel einer Gruppe — **aus dem Pfad der Werte und nicht aus dem
 * Index**: Der Zustand „dieser Knoten ist aufgeklappt" muss eine Eingrenzung
 * überleben, und die verschiebt jeden Index.
 *
 * **Der Ebenenname steht darin**, und das ist seit dem 15.09.2026 mehr als
 * Lesbarkeit: Beide Gliederungen teilen sich den Aufklappzustand der Ansicht,
 * und ein Partner und ein Projekt mit demselben Wert bekämen sonst denselben
 * Schlüssel.
 *
 * `\u0000` steht für die Gruppe ohne Wert. Ein Steuerzeichen und kein leerer
 * String, damit ein Wert, der tatsächlich `""` hieße, nicht denselben Schlüssel
 * bekäme.
 *
 * @param eltern der Schlüssel der Gruppe darüber, `null` an der Wurzel
 * @param schluessel der Gruppenschlüssel aus der Antwort (hochgestellt, E‑41)
 */
export function gruppenSchluessel(
  eltern: string | null,
  ebene: Baumebene,
  schluessel: string | null,
): string {
  const eigener = `${ebene.toLowerCase()}:${schluessel ?? "\u0000"}`;
  return eltern === null ? eigener : `${eltern}/${eigener}`;
}

/** Die `ProcessID` ist über den ganzen Baum eindeutig — mehr braucht es nicht. */
export function prozessSchluessel(processId: string): string {
  return `prozess:${processId}`;
}

/**
 * Die Gruppen, die offen stehen müssen, damit ein Prozess sichtbar ist.
 *
 * **Der aufgeklappte Knoten ergibt sich aus dem gewählten Prozess und steht
 * nicht eigens in der URL.** Ein tiefer Link auf `?prozess=…` zeigt den Baum
 * damit an der richtigen Stelle geöffnet, ohne dass die Adresse einen zweiten
 * Zustand tragen müsste, der mit dem ersten auseinanderlaufen könnte — und das
 * in beiden Gliederungen, weil beide dieselben Blätter tragen.
 *
 * Leer, solange der Prozess in diesem Baum nicht vorkommt — etwa bei einem Link
 * aus einem anderen Mandanten. Die Ansicht behauptet dann nichts.
 */
export function pfadZuProzess(
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  processId: string | null,
): string[] {
  if (processId === null) {
    return [];
  }
  return suchePfad(knoten, ebenen, 0, null, processId) ?? [];
}

function suchePfad(
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  tiefe: number,
  eltern: string | null,
  processId: string,
): string[] | null {
  for (const einzeln of knoten) {
    if (!istGruppe(einzeln)) {
      if (einzeln.processId === processId) {
        return [];
      }
      continue;
    }
    const schluessel = gruppenSchluessel(eltern, ebeneBei(ebenen, tiefe), einzeln.schluessel);
    const kinderebene = ebeneBei(ebenen, tiefe + 1);
    if (ebeneFaelltWeg(einzeln, kinderebene)) {
      const einzige = einzeln.kinder[0];
      if (einzige !== undefined && istGruppe(einzige)) {
        const darunter = suchePfad(
          einzige.kinder,
          ebenen,
          tiefe + 2,
          gruppenSchluessel(schluessel, kinderebene, einzige.schluessel),
          processId,
        );
        // Die übersprungene Gruppe hat keine Zeile und steht deshalb nicht im Pfad.
        if (darunter !== null) {
          return [schluessel, ...darunter];
        }
      }
      continue;
    }
    const darunter = suchePfad(einzeln.kinder, ebenen, tiefe + 1, schluessel, processId);
    if (darunter !== null) {
      return [schluessel, ...darunter];
    }
  }
  return null;
}

/** Die drei Zustände aus `docs/process-view.md` §4 — nie im Frontend gerechnet. */
export type Prozesszustand = Prozessknoten["zustand"];

type Zeilenrumpf = {
  /** Stabil über Eingrenzungen hinweg; zugleich der React-Schlüssel. */
  readonly schluessel: string;
  /** `aria-level`, 1‑basiert. Bei übersprungener Richtungsebene steht ein Blatt eine Ebene höher. */
  readonly ebene: number;
  /** `aria-posinset`, 1‑basiert. */
  readonly position: number;
  /** `aria-setsize`. */
  readonly geschwister: number;
  readonly nachrichten: number;
  readonly fehler: number;
};

export type Baumzeile =
  | (Zeilenrumpf & {
      readonly art: "GRUPPE";
      /** Welche Ebene die Gruppe ist — sie entscheidet über Beschriftung und vorgelesenen Namen. */
      readonly ebenenname: Baumebene;
      readonly name: string | null;
      readonly anzahlProzesse: number;
      readonly offen: boolean;
    })
  | (Zeilenrumpf & {
      readonly art: "PROZESS";
      readonly prozess: Prozessknoten;
    });

/**
 * Die sichtbaren Zeilen des Baums, von oben nach unten — **für zwei wie für drei
 * Ebenen dieselbe Funktion**.
 *
 * **Flach und nicht geschachtelt.** Die Tastaturbedienung des WAI‑ARIA-Musters
 * bewegt sich über die *sichtbaren* Knoten — auf einer flachen Liste ist das
 * „eins weiter", auf einem geschachtelten Baum ein Durchlauf. Die Schachtelung
 * steht in `aria-level`, `aria-posinset` und `aria-setsize`, wo ein
 * Vorleseprogramm sie erwartet.
 *
 * @param istOffen entscheidet je Knotenschlüssel. Bewusst ein Prädikat und keine
 *   Menge: Der Zustand der Ansicht ist „umgeschaltet gegenüber dem, was der
 *   gewählte Prozess ohnehin öffnet", und das ist keine Menge, die sich vorab
 *   aufzählen ließe.
 */
export function baumzeilen(
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  istOffen: (schluessel: string) => boolean,
): Baumzeile[] {
  const zeilen: Baumzeile[] = [];
  fuegeZeilenAn(zeilen, knoten, ebenen, 0, 1, null, istOffen);
  return zeilen;
}

function fuegeZeilenAn(
  zeilen: Baumzeile[],
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  tiefe: number,
  ariaEbene: number,
  eltern: string | null,
  istOffen: (schluessel: string) => boolean,
): void {
  knoten.forEach((einzeln, index) => {
    if (!istGruppe(einzeln)) {
      zeilen.push({
        art: "PROZESS",
        schluessel: prozessSchluessel(einzeln.processId),
        ebene: ariaEbene,
        position: index + 1,
        geschwister: knoten.length,
        nachrichten: einzeln.nachrichten,
        fehler: einzeln.fehler,
        prozess: einzeln,
      });
      return;
    }

    const ebenenname = ebeneBei(ebenen, tiefe);
    const schluessel = gruppenSchluessel(eltern, ebenenname, einzeln.schluessel);
    const offen = istOffen(schluessel);

    zeilen.push({
      art: "GRUPPE",
      ebenenname,
      schluessel,
      ebene: ariaEbene,
      position: index + 1,
      geschwister: knoten.length,
      offen,
      name: einzeln.name,
      anzahlProzesse: einzeln.anzahlProzesse,
      nachrichten: einzeln.nachrichten,
      fehler: einzeln.fehler,
    });

    if (!offen) {
      return;
    }

    const kinderebene = ebeneBei(ebenen, tiefe + 1);
    if (ebeneFaelltWeg(einzeln, kinderebene)) {
      /*
       * Die Kinder der einzigen Richtung rücken eine Ebene herauf. **Sie tragen
       * die Richtung nicht** (E‑58): Weggefallen ist die Ebene nur dort, wo die
       * Richtung `null` ist, und dafür gibt es kein Wort. Vor dem 03.09.2026
       * stand hier ein Zeichen und danach ein Vorsatz vor dem Namen; beides ist
       * entfallen.
       */
      const einzige = einzeln.kinder[0];
      if (einzige !== undefined && istGruppe(einzige)) {
        fuegeZeilenAn(
          zeilen,
          einzige.kinder,
          ebenen,
          tiefe + 2,
          ariaEbene + 1,
          gruppenSchluessel(schluessel, kinderebene, einzige.schluessel),
          istOffen,
        );
      }
      return;
    }

    fuegeZeilenAn(zeilen, einzeln.kinder, ebenen, tiefe + 1, ariaEbene + 1, schluessel, istOffen);
  });
}

/**
 * Die Eingrenzung — **örtlich und nicht serverseitig.**
 *
 * Die Antwort liegt vollständig vor (E‑33); ein Serverparameter brächte genau
 * die Fallstricke mit, die den Freitextfilter der Liste teuer machen
 * (`docs/prozessauswahl.md` §9). Bei 733 Blättern ist eine Eingrenzung im
 * Speicher nicht messbar teuer.
 *
 * **Sie filtert Gruppen *und* Prozessnamen, und eine Gruppe bleibt stehen,
 * deren Kind trifft.** Trifft die **Gruppe** selbst, bleiben **alle** ihre
 * Prozesse stehen — wer nach einem Partner oder einem Projekt sucht, will dessen
 * Prozesse sehen und nicht die Teilmenge, deren Namen zufällig denselben Text
 * tragen.
 *
 * **Sie greift nicht auf die Richtung zu** und nicht auf die Wörter „nicht
 * zugeordnet" oder „nicht ermittelt": Beides sind Texte der *Oberfläche* und
 * stünden in zwei Sprachen verschieden da. Gefiltert wird über die Werte, die
 * die Antwort trägt (Regel Q4) — seit dem 15.09.2026 auch über die
 * Projektbeschreibung, denn die ist ein solcher Wert.
 *
 * @param nurMitVerkehr blendet Blätter ohne Nachricht **im gewählten Zeitraum**
 *   aus. Bei `VOTG` sind 89,7 % der Prozesse „nie" (M111) — ohne diesen
 *   Schalter ist der Baum dort fast vollständig gedämpft. **Vorgabe aus**, denn
 *   mit Vorgabe *an* wäre genau der Prozess unauffindbar, den jemand sucht,
 *   *weil* er nichts trägt (`docs/prozessauswahl.md` §3).
 */
export function eingegrenzterBaum(
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  begriff: string,
  nurMitVerkehr: boolean,
): Baumknoten[] {
  return grenzeEin(knoten, ebenen, 0, begriff.trim().toLocaleLowerCase(), nurMitVerkehr, false);
}

function grenzeEin(
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  tiefe: number,
  gesucht: string,
  nurMitVerkehr: boolean,
  vorfahrTrifft: boolean,
): Baumknoten[] {
  const gefiltert: Baumknoten[] = [];
  for (const einzeln of knoten) {
    if (!istGruppe(einzeln)) {
      if (
        (vorfahrTrifft || enthaelt(einzeln.name, gesucht)) &&
        (!nurMitVerkehr || einzeln.nachrichten > 0)
      ) {
        gefiltert.push(einzeln);
      }
      continue;
    }
    const trifft =
      vorfahrTrifft || (ebeneBei(ebenen, tiefe) !== "RICHTUNG" && enthaelt(einzeln.name, gesucht));
    const kinder = grenzeEin(einzeln.kinder, ebenen, tiefe + 1, gesucht, nurMitVerkehr, trifft);
    if (kinder.length > 0) {
      gefiltert.push({ ...einzeln, kinder, ...summe(kinder) });
    }
  }
  return gefiltert;
}

/**
 * **Die Zahlen eines eingegrenzten Knotens sind die Summe seiner sichtbaren
 * Kinder** — und nicht die Zahl, die die Antwort für den ganzen Knoten nennt.
 *
 * Sonst stünde über zwei Zeilen „12 Prozesse". Die Rechnung ist **exakt und
 * keine Näherung**: Alle drei Kennzahlen sind über die Blätter additiv, und der
 * Dienst bildet sie genauso (`ProzessbaumService`). Ohne Eingrenzung fällt sie
 * deshalb mit den gelieferten Werten zusammen — `tests/prozessbaum.test.ts`
 * hält genau das fest.
 */
function summe(kinder: readonly Baumknoten[]): {
  anzahlProzesse: number;
  nachrichten: number;
  fehler: number;
} {
  return {
    anzahlProzesse: sichtbareProzesse(kinder),
    nachrichten: kinder.reduce((wert, kind) => wert + kind.nachrichten, 0),
    fehler: kinder.reduce((wert, kind) => wert + kind.fehler, 0),
  };
}

function enthaelt(wert: string | null, gesucht: string): boolean {
  if (gesucht === "") {
    return true;
  }
  return wert !== null && wert.toLocaleLowerCase().includes(gesucht);
}

/** Wie viele Prozesse unter diesen Knoten hängen — die Zahl neben dem Feld. */
export function sichtbareProzesse(knoten: readonly Baumknoten[]): number {
  return knoten.reduce(
    (wert, einzeln) => wert + (istGruppe(einzeln) ? einzeln.anzahlProzesse : 1),
    0,
  );
}

/**
 * Der Prozessknoten zu einer `ProcessID` — für die Überschrift der rechten
 * Spalte.
 *
 * **Gesucht wird im ungefilterten Baum.** Eine Eingrenzung blendet Zeilen aus;
 * sie darf nicht dazu führen, dass die geöffnete Liste ihren Namen verliert.
 *
 * `undefined`, wenn die Kennung in diesem Baum nicht vorkommt — ein geteilter
 * Link kann eine `ProcessID` eines anderen Mandanten tragen. Die Ansicht zeigt
 * dann die Kennung und behauptet keinen Namen.
 */
export function prozessAus(
  baum: Prozessbaum | undefined,
  processId: string | null,
): Prozessknoten | undefined {
  if (baum === undefined || processId === null) {
    return undefined;
  }
  return findeBlatt(baum.knoten, processId);
}

function findeBlatt(knoten: readonly Baumknoten[], processId: string): Prozessknoten | undefined {
  for (const einzeln of knoten) {
    if (!istGruppe(einzeln)) {
      if (einzeln.processId === processId) {
        return einzeln;
      }
      continue;
    }
    const treffer = findeBlatt(einzeln.kinder, processId);
    if (treffer !== undefined) {
      return treffer;
    }
  }
  return undefined;
}

/** Eine Gruppe über einem Prozess — ihre Ebene und ihr Name. */
export type Zuordnungsglied = { readonly ebene: Baumebene; readonly name: string | null };

/**
 * Die Gruppen über einem Prozess, von außen nach innen — für die Überschrift der
 * rechten Spalte. Im Partnerbaum Partner und Richtung, im Projektbaum das
 * Projekt.
 *
 * **Auch eine übersprungene Richtung steht darin** („nicht ermittelt"): In der
 * Überschrift gibt es keine Einrückung, die sie ersetzen könnte.
 *
 * `undefined` heißt „nicht gefunden", ein Glied mit `name: null` heißt „ohne
 * Wert". Die beiden fallen ausdrücklich nicht zusammen (Regel Q4).
 */
export function zuordnungVon(
  baum: Prozessbaum | undefined,
  processId: string | null,
): readonly Zuordnungsglied[] | undefined {
  if (baum === undefined || processId === null) {
    return undefined;
  }
  return sucheZuordnung(baum.knoten, baum.ebenen, 0, processId);
}

function sucheZuordnung(
  knoten: readonly Baumknoten[],
  ebenen: readonly Baumebene[],
  tiefe: number,
  processId: string,
): Zuordnungsglied[] | undefined {
  for (const einzeln of knoten) {
    if (!istGruppe(einzeln)) {
      if (einzeln.processId === processId) {
        return [];
      }
      continue;
    }
    const darunter = sucheZuordnung(einzeln.kinder, ebenen, tiefe + 1, processId);
    if (darunter !== undefined) {
      return [{ ebene: ebeneBei(ebenen, tiefe), name: einzeln.name }, ...darunter];
    }
  }
  return undefined;
}

/* ─────────────────────────────────────────────────────────────────────────────
   Beschriftung — die Regeln, nicht das Markup
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Der Anzeigetext einer Richtung.
 *
 * Drei Fälle, und der dritte ist der Grund, warum das eine Funktion ist:
 * `EINGEHEND` und `AUSGEHEND` werden übersetzt, `null` heißt **nicht
 * ermittelt** — und ein **gepflegter, aber unbekannter Wert bleibt stehen, wie
 * er ist** (Regel Q4). Der Katalog führt `richtung` als `varchar(20)` mit einer
 * Whitelist im Code; ein vierter Wert soll keine Migration kosten und hier keine
 * geratene Übersetzung bekommen.
 */
export function richtungstext(richtung: string | null, texte: Texte): string {
  if (richtung === null) {
    return texte.prozesse.richtung.nichtErmittelt;
  }
  if (richtung === "EINGEHEND" || richtung === "AUSGEHEND") {
    return texte.prozesse.richtung[richtung];
  }
  return richtung;
}

/** `null` heißt „nicht zugeordnet" und nie „leer" (Regel Q4). */
export function partnertext(partner: string | null, texte: Texte): string {
  return partner ?? texte.prozesse.nichtZugeordnet;
}

/**
 * Der Anzeigetext eines Knotens — **je Ebene eine Regel, für beide
 * Gliederungen eine Funktion**.
 *
 * | Ebene | ohne Wert |
 * |---|---|
 * | `PARTNER` | „nicht zugeordnet“ |
 * | `RICHTUNG` | „nicht ermittelt“, bekannte Werte übersetzt |
 * | `PROJEKT` | „Projekt ohne Beschreibung“ |
 * | `PROZESS` | „Prozess ohne Namen“ |
 *
 * **Die Zeile für `PROJEKT` ist keine Rückfallregel** (E‑144). Der Knoten bleibt
 * mit seinem Wert, wo er ist; ohne Beschreibung bekäme er nur keine leere
 * Beschriftung. Am Bestand kommt der Fall nicht vor.
 */
export function knotentext(ebene: Baumebene, name: string | null, texte: Texte): string {
  switch (ebene) {
    case "PARTNER":
      return partnertext(name, texte);
    case "RICHTUNG":
      return richtungstext(name, texte);
    case "PROJEKT":
      return name ?? texte.prozesse.ohneBeschreibung;
    case "PROZESS":
      return name ?? texte.prozesse.ohneNamen;
  }
}

/** Die Ebene als Wort — nur im vorgelesenen Namen einer Zeile, auf dem Bildschirm sagt es die Einrückung. */
export function ebenenbezeichnung(ebene: Baumebene, texte: Texte): string {
  switch (ebene) {
    case "PARTNER":
      return texte.prozesse.baum.ebenePartner;
    case "RICHTUNG":
      return texte.prozesse.baum.ebeneRichtung;
    case "PROJEKT":
      return texte.prozesse.baum.ebeneProjekt;
    case "PROZESS":
      return texte.prozesse.baum.ebeneProzess;
  }
}

/**
 * Der Zusatz, der den Zustand eines Prozesses **in Worten** trägt.
 *
 * ## E‑56 — Von drei Zuständen sind in der Zeile **zwei** zu sehen *(02.09.2026)*
 *
 * | Zustand | in der Zeile |
 * |---|---|
 * | `BEWEGT` | nichts — der unmarkierte Normalfall (E‑35) |
 * | `STILL` | die Marke „seit über {monate} Monaten nichts" |
 * | `NIE` | **nichts** — weder Wort noch Dämpfung |
 *
 * `STILL` bleibt, weil es ein **Vorfall** ist: Es gab eine Beziehung, und sie
 * ist verstummt — 0 bis 28 je Mandant (M111), eine Menge, die jemand durchsieht.
 * `NIE` ist eine **Katalogfrage** und kein Vorfall: Der Katalog führt denselben
 * Sachverhalt (E‑36), und ein neu angelegter Partner ohne Verkehr sähe in der
 * Zeile sonst aus wie ein Fehlerfall.
 *
 * **Wort und Dämpfung fallen zusammen, und das ist der Kern.** Bliebe die
 * Dämpfung ohne das Wort stehen, wäre der Zustand allein über Helligkeit
 * ausgedrückt — genau der Fall, den `docs/visuelles-konzept.md` §3 verbietet.
 * Die frühere Begründung („Warum ‚nie' trotzdem ein Wort bekommt") steht mitsamt
 * ihrer Umkehrung in `docs/process-view.md` §17.
 *
 * **Was bleibt:** die Zählung in der Kopfzeile („… — 23 bewegt, 1 still, 11 noch
 * nie"), der Schalter „Nur mit Verkehr im Zeitraum" (E‑48) und das Backend
 * vollständig — `zustand` wird weiter geliefert (E‑35, E‑36).
 *
 * **Die Schwelle steht nicht im Code.** Sie kommt als `stilleSchwelleMonate` aus
 * der Antwort (Entscheidung E‑37); die Oberfläche beschriftet damit und rechnet
 * nichts nach.
 */
export function zustandstext(
  zustand: Prozesszustand,
  stilleSchwelleMonate: number,
  texte: Texte,
): string | null {
  if (zustand === "STILL") {
    return einsetzen(texte.prozesse.baum.still, { monate: stilleSchwelleMonate });
  }
  return null;
}

/**
 * Der zugängliche Name einer Zeile — **ein `aria-label` statt vieler
 * `sr-only`-Spannen.**
 *
 * Eine Zeile trägt bis zu vier Zahlen ohne Beschriftung; vorgelesen wäre das
 * „ACME 12 8608 2". Die Beschriftungen als `sr-only` danebenzustellen kostete
 * bei 1.158 Zeilen über zweitausend zusätzliche Knoten — und jeder davon ist
 * `position: absolute` und damit ein Kandidat für den Befund aus
 * `docs/frontend-grundlagen.md` §7.
 *
 * **Als reine Funktion, weil es eine Entscheidung ist**: welche Angaben in
 * welcher Reihenfolge vorgelesen werden.
 */
export function zeilenbeschriftung(
  zeile: Baumzeile,
  stilleSchwelleMonate: number,
  texte: Texte,
  zahl: (wert: number) => string,
): string {
  const teile: string[] = [];

  if (zeile.art === "GRUPPE") {
    teile.push(
      knotentext(zeile.ebenenname, zeile.name, texte),
      ebenenbezeichnung(zeile.ebenenname, texte),
    );
    teile.push(
      einsetzen(texte.prozesse.baum.anzahlProzesse, { anzahl: zahl(zeile.anzahlProzesse) }),
    );
  } else {
    /*
     * **Die Richtung steht hier nicht**, und seit E‑58 in keinem Fall mehr: Wo
     * sie bekannt ist, trägt sie eine eigene Ebene — die ein Vorleseprogramm
     * über `aria-level` als Elternknoten findet —, und wo sie das nicht ist,
     * gibt es sie nicht. Bis zum 03.09.2026 stand hier „nicht ermittelt" für
     * genau die Blätter, die sichtbar nichts trugen; sichtbare und vorgelesene
     * Fassung liefen damit auseinander.
     */
    teile.push(knotentext("PROZESS", zeile.prozess.name, texte), texte.prozesse.baum.ebeneProzess);
  }

  teile.push(einsetzen(texte.prozesse.baum.anzahlNachrichten, { anzahl: zahl(zeile.nachrichten) }));
  if (zeile.fehler > 0) {
    teile.push(einsetzen(texte.prozesse.baum.anzahlFehler, { anzahl: zahl(zeile.fehler) }));
  }
  if (zeile.art === "PROZESS") {
    const zusatz = zustandstext(zeile.prozess.zustand, stilleSchwelleMonate, texte);
    if (zusatz !== null) {
      teile.push(zusatz);
    }
  }

  return teile.join(", ");
}

/* ─────────────────────────────────────────────────────────────────────────────
   Tastatur — das WAI‑ARIA-Muster als reine Funktion
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * Die Tasten, die der Baum an sich zieht.
 *
 * Sie stehen hier, weil das Unterdrücken der Voreinstellung **nicht** an der
 * Frage hängt, ob die Taste gerade etwas bewirkt: `ArrowDown` auf der letzten
 * Zeile bewirkt nichts und darf trotzdem die Seite nicht scrollen.
 */
export const BAUMTASTEN = [
  "ArrowDown",
  "ArrowUp",
  "ArrowLeft",
  "ArrowRight",
  "Home",
  "End",
  "Enter",
  " ",
] as const;

export type Baumbefehl =
  | { art: "FOKUS"; schluessel: string }
  | { art: "UMSCHALTEN"; schluessel: string }
  | { art: "WAEHLEN"; processId: string };

/**
 * Was ein Tastendruck im Baum bedeutet — **die Entscheidung, ohne DOM.**
 *
 * Das WAI‑ARIA-Muster für Bäume in einer reinen Funktion: Pfeil auf und ab
 * bewegen über die *sichtbaren* Knoten, rechts klappt auf, links zu. Sie steht
 * hier und nicht in der Komponente, weil sie eine Entscheidung ist und
 * Entscheidungen geprüft werden (`docs/frontend-grundlagen.md` §9) — ein
 * gerenderter Baum würde denselben Satz teurer belegen.
 *
 * **Zwei Feinheiten des Musters, die leicht verlorengehen:**
 *
 * 1. **Rechts klappt auf und springt nicht zugleich.** Auf einem zugeklappten
 *    Knoten öffnet die Taste ihn; erst der zweite Druck geht zum ersten Kind.
 *    Ein Sprung in einem Zug überspringt die Rückmeldung, dass überhaupt
 *    etwas aufgegangen ist.
 * 2. **Links klappt zu — oder geht zum Elternknoten.** Auf einem Blatt und auf
 *    einem zugeklappten Knoten ist das der Weg nach oben. Auf einer flachen
 *    Liste ist der Elternknoten die nächste Zeile darüber mit **kleinerer**
 *    Ebene; das trägt auch dann, wenn die Richtungsebene weggefallen ist (E‑45),
 *    und für zwei Ebenen genauso wie für drei.
 *
 * @returns `null`, wenn die Taste an dieser Stelle nichts bewirkt — am Anfang,
 *   am Ende, auf einem Blatt mit Pfeil rechts. Die Voreinstellung des Browsers
 *   wird trotzdem unterdrückt, siehe {@link BAUMTASTEN}.
 */
export function tastenbefehl(
  zeilen: readonly Baumzeile[],
  index: number,
  taste: string,
): Baumbefehl | null {
  const zeile = zeilen[index];
  if (zeile === undefined) {
    return null;
  }
  const fokus = (ziel: number): Baumbefehl | null =>
    zeilen[ziel] === undefined ? null : { art: "FOKUS", schluessel: zeilen[ziel].schluessel };

  switch (taste) {
    case "ArrowDown":
      return fokus(index + 1);
    case "ArrowUp":
      return fokus(index - 1);
    case "Home":
      return fokus(0);
    case "End":
      return fokus(zeilen.length - 1);
    case "ArrowRight":
      if (zeile.art !== "GRUPPE") {
        return null;
      }
      return zeile.offen ? fokus(index + 1) : { art: "UMSCHALTEN", schluessel: zeile.schluessel };
    case "ArrowLeft": {
      if (zeile.art === "GRUPPE" && zeile.offen) {
        return { art: "UMSCHALTEN", schluessel: zeile.schluessel };
      }
      for (let lauf = index - 1; lauf >= 0; lauf -= 1) {
        if (zeilen[lauf].ebene < zeile.ebene) {
          return fokus(lauf);
        }
      }
      return null;
    }
    case "Enter":
    case " ":
      return zeile.art === "PROZESS"
        ? { art: "WAEHLEN", processId: zeile.prozess.processId }
        : { art: "UMSCHALTEN", schluessel: zeile.schluessel };
    default:
      return null;
  }
}

/**
 * Die Umschaltungen **entlang eines Pfades** verwerfen.
 *
 * ## Warum es das braucht — der Fall, der es gefunden hat
 *
 * Offen ist ein Knoten, wenn der Nutzer ihn umgeschaltet hat; hat er das nicht,
 * entscheidet der Pfad zum gewählten Prozess. Beides überlagert sich mit
 * `??`, und **`??` fällt bei `false` nicht durch**: Ein Partner, den der
 * Nutzer einmal zugeklappt hat, bleibt zu — auch dann, wenn ein später
 * gewählter Prozess unter ihm hängt.
 *
 * Die Klickfolge, an der das auffällt, ist der dokumentierte Weg und kein
 * Sonderfall: Prozess P unter Partner A öffnen, A zuklappen, unter Partner B
 * den Prozess Q wählen, **Zurück** drücken. Die Adresse steht wieder auf P,
 * die Liste rechts zeigt P — und links behauptet der Baum, es gebe P nicht.
 *
 * **Deshalb wird beim Wechsel des gewählten Prozesses vergessen, was entlang
 * seines Pfades umgeschaltet war.** Alles andere bleibt stehen: Wer sich zehn
 * Partner aufgeklappt hat, verliert sie nicht, weil er einen Prozess wählt.
 */
export function ohnePfad(
  umgeschaltet: ReadonlyMap<string, boolean>,
  pfad: readonly string[],
): ReadonlyMap<string, boolean> {
  if (pfad.length === 0 || pfad.every((schluessel) => !umgeschaltet.has(schluessel))) {
    return umgeschaltet;
  }
  const neu = new Map(umgeschaltet);
  for (const schluessel of pfad) {
    neu.delete(schluessel);
  }
  return neu;
}
