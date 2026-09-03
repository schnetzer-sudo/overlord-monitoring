import { einsetzen, type Texte } from "@/i18n";

import type { Partnerknoten, Prozessbaum, Prozessknoten, Richtungsknoten } from "./api";

/**
 * Der Baum als **Zeilen** — die Entscheidungen der Prozessansicht, ohne React.
 *
 * Hier steht, was die Ansicht aus der Antwort macht: welche Ebene erscheint,
 * welche übersprungen wird, was eine Eingrenzung übrig lässt und in welcher
 * Reihenfolge die sichtbaren Zeilen stehen. **Die Reihenfolge selbst kommt aus
 * der Antwort** (`docs/process-view.md` E‑39) und wird hier nirgends geändert —
 * Partner alphabetisch, „nicht zugeordnet" am Ende, Richtungen in der
 * Reihenfolge der Aufzählung, Blätter nach `ProcessName`.
 *
 * **Frei von React**, wie `filter.ts` und aus demselben Grund: Das sind
 * Entscheidungen, und Entscheidungen werden geprüft, Markup nicht
 * (`docs/frontend-grundlagen.md` §9).
 */

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
 * **Der Preis ist gezählt** (M130, §29): Bei `NEXANS` bekommen 19 Partner ihre
 * Ebene zurück, bei `IBIS` 19; einer bei `NEXANS` behält sie nicht
 * (`SONDERPROZESS`, elf Prozesse ohne kuratierte Richtung). Der Baum ist damit
 * **innerhalb dessen, was er zeigt, gleichförmig**: Wo eine Richtung steht,
 * steht sie als Ebene — nirgends als Vorsatz.
 *
 * ## E‑46 — Das Überspringen geschieht in der Oberfläche, nicht im Endpunkt
 *
 * Der ist abgenommen und gemessen; seine Antwort bleibt vollständig und sagt
 * weiter, was der Katalog weiß. Die Darstellung entscheidet, ob sie dafür eine
 * Ebene aufmacht — dieselbe Trennung wie überall: das Backend stellt fest, die
 * Oberfläche beschriftet.
 */
export function richtungsebeneFaelltWeg(partner: Partnerknoten): boolean {
  if (partner.richtungen.length > 1) {
    return false;
  }
  const einzige = partner.richtungen[0];
  // Ein gepflegter, aber unbekannter vierter Katalogwert ist **bekannt** und
  // bekommt seine Ebene: Er steht dort, wie er im Katalog steht (Regel Q4).
  return einzige === undefined || einzige.richtung === null;
}

/**
 * Der Schlüssel eines Partnerknotens.
 *
 * **Aus dem Wert und nicht aus dem Index**: Der Zustand „dieser Partner ist
 * aufgeklappt" muss eine Eingrenzung überleben, und die verschiebt jeden Index.
 *
 * `\u0000` steht für „nicht zugeordnet". Ein Steuerzeichen und kein leerer
 * String, damit ein Partner, der tatsächlich `""` hieße, nicht denselben
 * Schlüssel bekäme.
 */
export function partnerSchluessel(partner: string | null): string {
  return `partner:${partner ?? "\u0000"}`;
}

export function richtungsSchluessel(partner: string | null, richtung: string | null): string {
  return `${partnerSchluessel(partner)}/richtung:${richtung ?? "\u0000"}`;
}

/** Die `ProcessID` ist über den ganzen Baum eindeutig — mehr braucht es nicht. */
export function prozessSchluessel(processId: string): string {
  return `prozess:${processId}`;
}

/**
 * Die Knoten, die offen stehen müssen, damit ein Prozess sichtbar ist.
 *
 * **Der aufgeklappte Partner ergibt sich aus dem gewählten Prozess und steht
 * nicht eigens in der URL.** Ein tiefer Link auf `?prozess=…` zeigt den Baum
 * damit an der richtigen Stelle geöffnet, ohne dass die Adresse einen zweiten
 * Zustand tragen müsste, der mit dem ersten auseinanderlaufen könnte.
 *
 * Leer, solange der Prozess in diesem Baum nicht vorkommt — etwa bei einem Link
 * aus einem anderen Mandanten. Die Ansicht behauptet dann nichts.
 */
export function pfadZuProzess(
  partner: readonly Partnerknoten[],
  processId: string | null,
): string[] {
  if (processId === null) {
    return [];
  }
  for (const knoten of partner) {
    for (const richtung of knoten.richtungen) {
      if (!richtung.prozesse.some((prozess) => prozess.processId === processId)) {
        continue;
      }
      const pfad = [partnerSchluessel(knoten.partner)];
      if (!richtungsebeneFaelltWeg(knoten)) {
        pfad.push(richtungsSchluessel(knoten.partner, richtung.richtung));
      }
      return pfad;
    }
  }
  return [];
}

/** Die drei Zustände aus `docs/process-view.md` §4 — nie im Frontend gerechnet. */
export type Prozesszustand = Prozessknoten["zustand"];

type Zeilenrumpf = {
  /** Stabil über Eingrenzungen hinweg; zugleich der React-Schlüssel. */
  readonly schluessel: string;
  /** `aria-level`, 1‑basiert. Bei übersprungener Richtungsebene ist ein Blatt Ebene 2. */
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
      readonly art: "PARTNER";
      readonly partner: string | null;
      readonly anzahlProzesse: number;
      readonly offen: boolean;
    })
  | (Zeilenrumpf & {
      readonly art: "RICHTUNG";
      readonly richtung: string | null;
      readonly anzahlProzesse: number;
      readonly offen: boolean;
    })
  | (Zeilenrumpf & {
      readonly art: "PROZESS";
      readonly prozess: Prozessknoten;
    });

/**
 * Die sichtbaren Zeilen des Baums, von oben nach unten.
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
  partner: readonly Partnerknoten[],
  istOffen: (schluessel: string) => boolean,
): Baumzeile[] {
  const zeilen: Baumzeile[] = [];

  partner.forEach((knoten, index) => {
    const schluessel = partnerSchluessel(knoten.partner);
    const offen = istOffen(schluessel);
    const ohneEbene = richtungsebeneFaelltWeg(knoten);
    const einzige = knoten.richtungen[0];

    zeilen.push({
      art: "PARTNER",
      schluessel,
      ebene: 1,
      position: index + 1,
      geschwister: partner.length,
      offen,
      partner: knoten.partner,
      anzahlProzesse: knoten.anzahlProzesse,
      nachrichten: knoten.nachrichten,
      fehler: knoten.fehler,
    });

    if (!offen) {
      return;
    }

    if (ohneEbene) {
      /*
       * Die Blätter rücken eine Ebene herauf. **Sie tragen die Richtung nicht**
       * (E‑58): Weggefallen ist die Ebene nur dort, wo die Richtung `null` ist,
       * und dafür gibt es kein Wort. Vor dem 03.09.2026 stand hier ein Zeichen
       * und danach ein Vorsatz vor dem Namen; beides ist entfallen.
       */
      blattzeilen(einzige, 2).forEach((zeile) => zeilen.push(zeile));
      return;
    }

    knoten.richtungen.forEach((richtung, richtungsindex) => {
      const richtungsschluessel = richtungsSchluessel(knoten.partner, richtung.richtung);
      const richtungOffen = istOffen(richtungsschluessel);

      zeilen.push({
        art: "RICHTUNG",
        schluessel: richtungsschluessel,
        ebene: 2,
        position: richtungsindex + 1,
        geschwister: knoten.richtungen.length,
        offen: richtungOffen,
        richtung: richtung.richtung,
        anzahlProzesse: richtung.anzahlProzesse,
        nachrichten: richtung.nachrichten,
        fehler: richtung.fehler,
      });

      if (richtungOffen) {
        blattzeilen(richtung, 3).forEach((zeile) => zeilen.push(zeile));
      }
    });
  });

  return zeilen;
}

function blattzeilen(richtung: Richtungsknoten | undefined, ebene: number): Baumzeile[] {
  if (richtung === undefined) {
    return [];
  }
  return richtung.prozesse.map((prozess, index) => ({
    art: "PROZESS" as const,
    schluessel: prozessSchluessel(prozess.processId),
    ebene,
    position: index + 1,
    geschwister: richtung.prozesse.length,
    nachrichten: prozess.nachrichten,
    fehler: prozess.fehler,
    prozess,
  }));
}

/**
 * Die Eingrenzung — **örtlich und nicht serverseitig.**
 *
 * Die Antwort liegt vollständig vor (E‑33); ein Serverparameter brächte genau
 * die Fallstricke mit, die den Freitextfilter der Liste teuer machen
 * (`docs/prozessauswahl.md` §9). Bei 733 Blättern ist eine Eingrenzung im
 * Speicher nicht messbar teuer.
 *
 * **Sie filtert Partner *und* Prozessnamen, und ein Partner bleibt stehen,
 * dessen Kind trifft.** Trifft der **Partner** selbst, bleiben alle seine
 * Prozesse stehen — wer nach einem Partner sucht, will dessen Prozesse sehen und
 * nicht die Teilmenge, deren Namen zufällig denselben Text tragen.
 *
 * **Sie greift nicht auf die Richtung zu** und nicht auf die Wörter „nicht
 * zugeordnet" oder „nicht ermittelt": Beides sind Texte der *Oberfläche* und
 * stünden in zwei Sprachen verschieden da. Gefiltert wird über die Werte, die
 * die Antwort trägt (Regel Q4).
 *
 * @param nurMitVerkehr blendet Blätter ohne Nachricht **im gewählten Zeitraum**
 *   aus. Bei `VOTG` sind 89,7 % der Prozesse „nie" (M111) — ohne diesen
 *   Schalter ist der Baum dort fast vollständig gedämpft. **Vorgabe aus**, denn
 *   mit Vorgabe *an* wäre genau der Prozess unauffindbar, den jemand sucht,
 *   *weil* er nichts trägt (`docs/prozessauswahl.md` §3).
 */
export function eingegrenzterBaum(
  partner: readonly Partnerknoten[],
  begriff: string,
  nurMitVerkehr: boolean,
): Partnerknoten[] {
  const gesucht = begriff.trim().toLocaleLowerCase();

  const gefiltert: Partnerknoten[] = [];
  for (const knoten of partner) {
    const partnerTrifft = enthaelt(knoten.partner, gesucht);

    const richtungen: Richtungsknoten[] = [];
    for (const richtung of knoten.richtungen) {
      const prozesse = richtung.prozesse.filter(
        (prozess) =>
          (partnerTrifft || enthaelt(prozess.processName, gesucht)) &&
          (!nurMitVerkehr || prozess.nachrichten > 0),
      );
      if (prozesse.length > 0) {
        richtungen.push({ ...richtung, prozesse, ...summe(prozesse) });
      }
    }

    if (richtungen.length > 0) {
      gefiltert.push({ ...knoten, richtungen, ...summeDerGruppen(richtungen) });
    }
  }
  return gefiltert;
}

/**
 * **Die Zahlen eines eingegrenzten Knotens sind die Summe seiner sichtbaren
 * Blätter** — und nicht die Zahl, die die Antwort für den ganzen Knoten nennt.
 *
 * Sonst stünde über zwei Zeilen „12 Prozesse". Die Rechnung ist **exakt und
 * keine Näherung**: Alle drei Kennzahlen sind über die Blätter additiv, und der
 * Dienst bildet sie genauso (`ProzessbaumService`). Ohne Eingrenzung fällt sie
 * deshalb mit den gelieferten Werten zusammen — `tests/prozessbaum.test.ts`
 * hält genau das fest.
 */
function summe(prozesse: readonly Prozessknoten[]): {
  anzahlProzesse: number;
  nachrichten: number;
  fehler: number;
} {
  return {
    anzahlProzesse: prozesse.length,
    nachrichten: prozesse.reduce((wert, prozess) => wert + prozess.nachrichten, 0),
    fehler: prozesse.reduce((wert, prozess) => wert + prozess.fehler, 0),
  };
}

function summeDerGruppen(richtungen: readonly Richtungsknoten[]): {
  anzahlProzesse: number;
  nachrichten: number;
  fehler: number;
} {
  return {
    anzahlProzesse: richtungen.reduce((wert, gruppe) => wert + gruppe.anzahlProzesse, 0),
    nachrichten: richtungen.reduce((wert, gruppe) => wert + gruppe.nachrichten, 0),
    fehler: richtungen.reduce((wert, gruppe) => wert + gruppe.fehler, 0),
  };
}

function enthaelt(wert: string | null, gesucht: string): boolean {
  if (gesucht === "") {
    return true;
  }
  return wert !== null && wert.toLocaleLowerCase().includes(gesucht);
}

/** Wie viele Prozesse nach der Eingrenzung übrig sind — die Zahl neben dem Feld. */
export function sichtbareProzesse(partner: readonly Partnerknoten[]): number {
  return partner.reduce((wert, knoten) => wert + knoten.anzahlProzesse, 0);
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
  for (const knoten of baum.partner) {
    for (const richtung of knoten.richtungen) {
      const treffer = richtung.prozesse.find((prozess) => prozess.processId === processId);
      if (treffer !== undefined) {
        return treffer;
      }
    }
  }
  return undefined;
}

/**
 * Der Partner eines Prozesses — für die Überschrift der rechten Spalte.
 *
 * `undefined` heißt „nicht gefunden", `null` heißt „nicht zugeordnet". Die
 * beiden fallen ausdrücklich nicht zusammen (Regel Q4).
 */
export function partnerVon(
  baum: Prozessbaum | undefined,
  processId: string | null,
): { partner: string | null; richtung: string | null } | undefined {
  if (baum === undefined || processId === null) {
    return undefined;
  }
  for (const knoten of baum.partner) {
    for (const richtung of knoten.richtungen) {
      if (richtung.prozesse.some((prozess) => prozess.processId === processId)) {
        return { partner: knoten.partner, richtung: richtung.richtung };
      }
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

  if (zeile.art === "PARTNER") {
    teile.push(partnertext(zeile.partner, texte), texte.prozesse.baum.ebenePartner);
    teile.push(
      einsetzen(texte.prozesse.baum.anzahlProzesse, { anzahl: zahl(zeile.anzahlProzesse) }),
    );
  } else if (zeile.art === "RICHTUNG") {
    teile.push(richtungstext(zeile.richtung, texte), texte.prozesse.baum.ebeneRichtung);
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
    teile.push(
      zeile.prozess.processName ?? texte.prozesse.ohneNamen,
      texte.prozesse.baum.ebeneProzess,
    );
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
 *    Ebene; das trägt auch dann, wenn die Richtungsebene weggefallen ist (E‑45)
 *    und ein Blatt auf Ebene 2 steht.
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
  const gruppe = zeile.art !== "PROZESS";
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
      if (!gruppe) {
        return null;
      }
      return zeile.offen ? fokus(index + 1) : { art: "UMSCHALTEN", schluessel: zeile.schluessel };
    case "ArrowLeft": {
      if (gruppe && zeile.offen) {
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
