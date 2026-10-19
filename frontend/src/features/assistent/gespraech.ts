import { einsetzen, type Sprache, type Texte } from "@/i18n";
import { fehleranzeige } from "@/lib/fehlertext";
import { formatiereZahl } from "@/lib/format";
import { ProblemFehler } from "@/lib/http";

import type {
  AngelegterAuftrag,
  NachrichtAntwort,
  Pruefanfrage,
  Prueffehler,
  Vorschlag,
  Werte,
} from "./api";

/**
 * **Das Gespräch im Arbeitsspeicher** (F3, `docs/assistent-frontend.md` §3).
 *
 * Kein Endpunkt liefert den Verlauf zurück; das Backend hält ihn nur für die
 * Gesprächsführung (E‑1038). Was der Kunde in der Chatbox sieht — Begrüßung,
 * Nachrichten, Antworten, Karten samt halb geänderter Felder —, steht deshalb
 * hier: ein Zustand und eine **reine** Übergangsfunktion für `useReducer`. Die
 * Kopfzeile hält ihn, solange sie steht, also über jede Navigation hinweg und
 * nicht über ein Neuladen (E‑1076). Kein lokaler Speicher, keine URL.
 *
 * Die Karte selbst hält keinen Zustand: Schließt der Kunde die Chatbox, hängt
 * ihr Inhalt aus, und eine Karte mit eigenem Zustand verlöre ihre `anfrageId`
 * — ein zweiter Klick nach dem Wiederöffnen legte dann doppelt an (E‑1072).
 */

/** Was nach einem gescheiterten Aufruf in der Chatbox steht (§6). */
export type Hinweis =
  | { art: "zuVieleNachrichten" }
  | { art: "ausgeschoepft"; hoechstens: number | undefined }
  | { art: "nachrichtUngueltig"; hoechstensZeichen: number | undefined }
  | { art: "nichtErreichbar" }
  | { art: "abgelaufen" }
  | { art: "sonstiges"; fehler: ProblemFehler };

/** Wo die Prüfung des Entwurfs auf der Karte steht (E‑1071). */
export type Pruefstand =
  | { stand: "frei" }
  | { stand: "laeuft" }
  | { stand: "fehler"; fehler: Prueffehler[] }
  | { stand: "typUnbekannt" }
  | { stand: "gestoert" };

/** Wo das Anlegen der Karte steht (E‑1072, E‑1073). */
export type Anlegestand =
  | { stand: "bereit" }
  | { stand: "laeuft" }
  | { stand: "fehler"; fehler: Prueffehler[] }
  | { stand: "typUnbekannt" }
  | { stand: "gestoert" }
  | { stand: "angelegt"; einordnung: string };

export type Karte = {
  vorschlag: Vorschlag;
  /** Einmal je Karte, beim Erscheinen erzeugt; jeder Versuch schickt dieselbe (E‑1072). */
  anfrageId: string;
  /** Die übernommenen Werte je Eintrag — das, was „Auftrag anlegen“ schickt. */
  werte: Werte[];
  modus: "ansicht" | "aendern";
  /** Der Stand der Eingabefelder, nur im Änderungsmodus. */
  entwurf: Werte[] | null;
  pruefung: Pruefstand;
  anlegen: Anlegestand;
  /** „Alle n anzeigen“ gewählt (F6). */
  aufgeklappt: boolean;
};

export type Beitrag =
  | { art: "begruessung"; id: string }
  | { art: "kunde"; id: string; text: string }
  | { art: "assistent"; id: string; text: string; karte: Karte | null };

/** Was die verborgene Ansage gerade sagt (`docs/assistent-frontend.md` §7). */
export type Ansage = { art: "wartet" } | { art: "antwort"; beitragId: string } | null;

export type Gespraech = {
  /** Erst nach dem ersten Senden gesetzt (E‑1082). */
  gespraechId: string | null;
  beitraege: Beitrag[];
  /** Der Text im Eingabefeld. Bleibt beim Schließen und bei „Neues Gespräch“ stehen. */
  entwurf: string;
  /** Eine Nachricht ist unterwegs (E‑1075). */
  wartet: boolean;
  /** `404` auf das Gespräch: Senden gesperrt bis „Neues Gespräch“ (E‑1076). */
  abgelaufen: boolean;
  hinweis: Hinweis | null;
  ansage: Ansage;
  /**
   * Zählt jede Änderung am Ende des Verlaufs. Sie ist der Schlüssel, unter dem
   * das Neue ins Bild kommt — auch ein zweiter, gleicher Hinweis.
   */
  stand: number;
  /** Laufende Nummer für Kennungen; zählt über „Neues Gespräch“ hinweg weiter. */
  naechste: number;
};

export type Aktion =
  | { art: "entwurf"; text: string }
  | { art: "senden"; text: string }
  | { art: "eroeffnet"; gespraechId: string }
  | { art: "beantwortet"; antwort: NachrichtAntwort; anfrageId: string }
  | { art: "gescheitert"; text: string; hinweis: Hinweis | null }
  | { art: "neu" }
  | { art: "aufklappen"; karteId: string; offen: boolean }
  | { art: "aendern"; karteId: string }
  | { art: "feld"; karteId: string; entwurf: Werte[] }
  | { art: "pruefungBeginnt"; karteId: string; entwurf: Werte[] }
  | { art: "geprueft"; karteId: string; entwurf: Werte[]; ergebnis: Pruefstand }
  | { art: "uebernehmen"; karteId: string }
  | { art: "verwerfen"; karteId: string }
  | { art: "anlegenBeginnt"; karteId: string }
  | { art: "angelegt"; karteId: string; auftrag: AngelegterAuftrag }
  | { art: "anlegenGescheitert"; karteId: string; ergebnis: Anlegestand };

/** Bis zu so vielen Einträgen ist die Karte offen (F6). */
export const OFFENE_EINTRAEGE = 5;

export function neuesGespraech(): Gespraech {
  return {
    gespraechId: null,
    beitraege: [{ art: "begruessung", id: "b0" }],
    entwurf: "",
    wartet: false,
    abgelaufen: false,
    hinweis: null,
    ansage: null,
    stand: 0,
    naechste: 1,
  };
}

/** Leer heißt `null` oder leerer Text — dieselbe Lesart wie im Backend (`docs/tasks.md` §5.3). */
export function istLeer(wert: string | null | undefined): boolean {
  return wert === null || wert === undefined || wert === "";
}

/**
 * Ob „Senden“ etwas tut. Eine Länge prüft das Frontend nicht — die Grenze kennt
 * das Backend und meldet sie (`docs/assistent-frontend.md` §4).
 */
export function kannSenden(zustand: Gespraech): boolean {
  return !zustand.wartet && !zustand.abgelaufen && zustand.entwurf.trim() !== "";
}

export function gespraechSchritt(zustand: Gespraech, aktion: Aktion): Gespraech {
  switch (aktion.art) {
    case "entwurf":
      return zustand.wartet ? zustand : { ...zustand, entwurf: aktion.text };

    case "senden": {
      if (zustand.wartet || zustand.abgelaufen || aktion.text.trim() === "") {
        return zustand;
      }
      const beitrag: Beitrag = { art: "kunde", id: `b${zustand.naechste}`, text: aktion.text };
      return {
        ...zustand,
        beitraege: [...zustand.beitraege, beitrag],
        entwurf: "",
        wartet: true,
        hinweis: null,
        ansage: { art: "wartet" },
        stand: zustand.stand + 1,
        naechste: zustand.naechste + 1,
      };
    }

    case "eroeffnet":
      return { ...zustand, gespraechId: aktion.gespraechId };

    case "beantwortet": {
      if (!zustand.wartet) {
        return zustand;
      }
      const id = `b${zustand.naechste}`;
      const { vorschlag } = aktion.antwort;
      const beitrag: Beitrag = {
        art: "assistent",
        id,
        text: aktion.antwort.text,
        karte: vorschlag === null ? null : neueKarte(vorschlag, aktion.anfrageId),
      };
      return {
        ...zustand,
        beitraege: [...zustand.beitraege, beitrag],
        wartet: false,
        ansage: { art: "antwort", beitragId: id },
        stand: zustand.stand + 1,
        naechste: zustand.naechste + 1,
      };
    }

    case "gescheitert": {
      if (!zustand.wartet) {
        return zustand;
      }
      // F4: Die Nachricht hat das Backend nicht in seinen Verlauf genommen
      // (E‑1052) — also auch nicht in diesen. Sie steht wieder im Feld.
      const letzter = zustand.beitraege.at(-1);
      const beitraege =
        letzter?.art === "kunde" && letzter.text === aktion.text
          ? zustand.beitraege.slice(0, -1)
          : zustand.beitraege;
      return {
        ...zustand,
        beitraege,
        entwurf: aktion.text,
        wartet: false,
        abgelaufen: zustand.abgelaufen || aktion.hinweis?.art === "abgelaufen",
        hinweis: aktion.hinweis,
        ansage: null,
        stand: zustand.stand + 1,
      };
    }

    case "neu": {
      const frisch = neuesGespraech();
      return {
        ...frisch,
        beitraege: [{ art: "begruessung", id: `b${zustand.naechste}` }],
        entwurf: zustand.entwurf,
        stand: zustand.stand + 1,
        naechste: zustand.naechste + 1,
      };
    }

    default:
      return mitKarte(zustand, aktion.karteId, (karte) => kartenSchritt(karte, aktion));
  }
}

function neueKarte(vorschlag: Vorschlag, anfrageId: string): Karte {
  return {
    vorschlag,
    anfrageId,
    werte: vorschlag.eintraege.map((eintrag) => ({ ...eintrag.werte })),
    modus: "ansicht",
    entwurf: null,
    pruefung: { stand: "frei" },
    anlegen: { stand: "bereit" },
    aufgeklappt: false,
  };
}

function mitKarte(
  zustand: Gespraech,
  karteId: string,
  schritt: (karte: Karte) => Karte,
): Gespraech {
  let geaendert = false;
  const beitraege = zustand.beitraege.map((beitrag) => {
    if (beitrag.art !== "assistent" || beitrag.id !== karteId || beitrag.karte === null) {
      return beitrag;
    }
    const karte = schritt(beitrag.karte);
    if (karte === beitrag.karte) {
      return beitrag;
    }
    geaendert = true;
    return { ...beitrag, karte };
  });
  // Eine Karte, die es nicht mehr gibt — nach „Neues Gespräch“ kommt die Antwort
  // einer Prüfung ins Leere —, ändert nichts.
  return geaendert ? { ...zustand, beitraege } : zustand;
}

type Kartenaktion = Exclude<
  Aktion,
  { art: "entwurf" | "senden" | "eroeffnet" | "beantwortet" | "gescheitert" | "neu" }
>;

function kartenSchritt(karte: Karte, aktion: Kartenaktion): Karte {
  const angelegt = karte.anlegen.stand === "angelegt";
  switch (aktion.art) {
    case "aufklappen":
      return { ...karte, aufgeklappt: aktion.offen };

    case "aendern":
      if (angelegt || karte.modus === "aendern" || karte.anlegen.stand === "laeuft") {
        return karte;
      }
      return {
        ...karte,
        modus: "aendern",
        entwurf: karte.werte.map((werte) => ({ ...werte })),
        // Was beim Anlegen am Feld stand, steht dort weiter, bis eine neue
        // Prüfung etwas anderes sagt.
        pruefung:
          karte.anlegen.stand === "fehler"
            ? { stand: "fehler", fehler: karte.anlegen.fehler }
            : { stand: "frei" },
      };

    case "feld":
      if (karte.modus !== "aendern") {
        return karte;
      }
      return { ...karte, entwurf: aktion.entwurf, pruefung: { stand: "laeuft" } };

    case "pruefungBeginnt":
      if (karte.modus !== "aendern" || karte.entwurf !== aktion.entwurf) {
        return karte;
      }
      return { ...karte, pruefung: { stand: "laeuft" } };

    case "geprueft":
      // Nur die Antwort auf den **aktuellen** Stand zählt; eine ältere
      // verwirft sich selbst (F5).
      if (karte.modus !== "aendern" || karte.entwurf !== aktion.entwurf) {
        return karte;
      }
      return { ...karte, pruefung: aktion.ergebnis };

    case "uebernehmen":
      if (!darfUebernehmen(karte) || karte.entwurf === null) {
        return karte;
      }
      return {
        ...karte,
        werte: karte.entwurf,
        modus: "ansicht",
        entwurf: null,
        pruefung: { stand: "frei" },
        anlegen: { stand: "bereit" },
      };

    case "verwerfen":
      if (karte.modus !== "aendern") {
        return karte;
      }
      return { ...karte, modus: "ansicht", entwurf: null, pruefung: { stand: "frei" } };

    case "anlegenBeginnt":
      if (!darfAnlegen(karte)) {
        return karte;
      }
      return { ...karte, anlegen: { stand: "laeuft" } };

    case "angelegt":
      if (karte.anlegen.stand !== "laeuft") {
        return karte;
      }
      return {
        ...karte,
        werte: werteAusAuftrag(karte, aktion.auftrag),
        anlegen: { stand: "angelegt", einordnung: aktion.auftrag.einordnung },
      };

    case "anlegenGescheitert":
      if (karte.anlegen.stand !== "laeuft") {
        return karte;
      }
      return { ...karte, anlegen: aktion.ergebnis };
  }
}

/**
 * Die Werte, die **angelegt sind** (F7). Mit derselben `anfrageId` antwortet das
 * Backend nach einer Zeitüberschreitung mit dem vorhandenen Auftrag, auch wenn
 * die Karte inzwischen andere Werte trägt (`docs/tasks.md` §6.2). Die Antwort
 * trägt nur Felder mit Wert; die übrigen sind leer.
 */
function werteAusAuftrag(karte: Karte, auftrag: AngelegterAuftrag): Werte[] {
  if (!Array.isArray(auftrag.eintraege) || auftrag.eintraege.length === 0) {
    return karte.werte;
  }
  return auftrag.eintraege.map((eintrag) =>
    Object.fromEntries(
      karte.vorschlag.felder.map((feld) => [feld.name, eintrag.werte?.[feld.name] ?? null]),
    ),
  );
}

/** Der Rumpf für Prüfung und Anlegen: `typ`, `aktion` und die Werte, sonst nichts. */
export function pruefanfrage(vorschlag: Vorschlag, werte: Werte[]): Pruefanfrage {
  return { typ: vorschlag.typ, aktion: vorschlag.aktion, eintraege: werte };
}

/** „Übernehmen“ ist frei, wenn die letzte Prüfung zum aktuellen Stand nichts fand (E‑1071). */
export function darfUebernehmen(karte: Karte): boolean {
  return karte.modus === "aendern" && karte.pruefung.stand === "frei";
}

/** „Auftrag anlegen“ (E‑1073, F8). */
export function darfAnlegen(karte: Karte): boolean {
  return (
    karte.modus === "ansicht" &&
    (karte.anlegen.stand === "bereit" || karte.anlegen.stand === "gestoert")
  );
}

/** Die Fehler, die gerade auf der Karte stehen: im Änderungsmodus die der Prüfung, sonst die des Anlegens. */
export function aktuelleFehler(karte: Karte): Prueffehler[] {
  if (karte.modus === "aendern") {
    return karte.pruefung.stand === "fehler" ? karte.pruefung.fehler : [];
  }
  return karte.anlegen.stand === "fehler" ? karte.anlegen.fehler : [];
}

/** Die Werte, die die Karte gerade zeigt. */
export function angezeigteWerte(karte: Karte): Werte[] {
  return karte.modus === "aendern" && karte.entwurf !== null ? karte.entwurf : karte.werte;
}

/**
 * Wohin ein Fehler gehört (`docs/assistent-frontend.md` §5): an ein Feld, an
 * einen Eintrag oder an die Karte. Ein Feld, das die Karte nicht zeigt, gehört
 * an seinen Eintrag — sonst stünde die Meldung nirgends.
 */
export function ortVon(
  fehler: Prueffehler,
  karte: Karte,
):
  | { art: "feld"; index: number; feld: string }
  | { art: "eintrag"; index: number }
  | { art: "karte" } {
  const anzahl = angezeigteWerte(karte).length;
  if (fehler.index === null || fehler.index < 0 || fehler.index >= anzahl) {
    return { art: "karte" };
  }
  if (fehler.feld !== null && karte.vorschlag.felder.some((feld) => feld.name === fehler.feld)) {
    return { art: "feld", index: fehler.index, feld: fehler.feld };
  }
  return { art: "eintrag", index: fehler.index };
}

/**
 * Die Überschrift eines Eintrags (E‑1080): der Schlüssel aus dem Vorschlag,
 * solange die übernommenen Werte des Eintrags unverändert sind; sonst `null`,
 * und die Karte schreibt „Eintrag n“. Die Karte bildet keinen Schlüssel selbst
 * — das hieße, die Regel der Typdefinition nachzubauen (Punkt 1017).
 */
export function ueberschrift(karte: Karte, index: number): string | null {
  const ursprung = karte.vorschlag.eintraege[index];
  const werte = karte.werte[index];
  if (ursprung === undefined || werte === undefined || ursprung.schluessel === null) {
    return null;
  }
  const unveraendert = karte.vorschlag.felder.every((feld) =>
    gleich(ursprung.werte[feld.name], werte[feld.name]),
  );
  return unveraendert ? ursprung.schluessel : null;
}

function gleich(a: string | null | undefined, b: string | null | undefined): boolean {
  return istLeer(a) ? istLeer(b) : a === b;
}

/**
 * Wie viele Einträge offen stehen (F6): bis fünf alle; darüber die ersten fünf,
 * außer „Alle n anzeigen“ ist gewählt, die Karte wird geändert oder ein
 * verborgener Eintrag trägt einen Fehler.
 */
export function sichtbareEintraege(karte: Karte): number {
  const anzahl = angezeigteWerte(karte).length;
  if (anzahl <= OFFENE_EINTRAEGE || karte.aufgeklappt || karte.modus === "aendern") {
    return anzahl;
  }
  const verborgenerFehler = aktuelleFehler(karte).some((fehler) => {
    const ort = ortVon(fehler, karte);
    return ort.art !== "karte" && ort.index >= OFFENE_EINTRAEGE;
  });
  return verborgenerFehler ? anzahl : OFFENE_EINTRAEGE;
}

/**
 * Was ein gescheiterter Aufruf der Chatbox sagt (§6), **nach Problemtyp und
 * nicht nach Statuscode** (E‑1081): `429` heißt zweierlei, und nur bei einem
 * davon hilft Warten.
 *
 * `null` heißt: nichts anzeigen — beim `409`, den die Sperre ausschließen soll
 * (F12), und beim `401`, auf den der Zwischenspeicher schon umleitet.
 */
export function hinweisAus(fehler: unknown): Hinweis | null {
  if (!(fehler instanceof ProblemFehler)) {
    return { art: "nichtErreichbar" };
  }
  if (fehler.status === 401 || fehler.typ === "nachricht-laeuft") {
    return null;
  }
  if (fehler.status === 404) {
    return { art: "abgelaufen" };
  }
  if (fehler.typ === "gespraech-ausgeschoepft") {
    return { art: "ausgeschoepft", hoechstens: fehler.zahl("hoechstensNachrichten") };
  }
  if (fehler.status === 429) {
    return { art: "zuVieleNachrichten" };
  }
  if (fehler.typ === "nachricht-ungueltig") {
    return { art: "nachrichtUngueltig", hoechstensZeichen: fehler.zahl("hoechstensZeichen") };
  }
  if (fehler.status === 0 || fehler.status >= 500) {
    return { art: "nichtErreichbar" };
  }
  return { art: "sonstiges", fehler };
}

/** Ob ein Hinweis „Neues Gespräch“ anbietet. */
export function bietetNeuesGespraech(hinweis: Hinweis | null): boolean {
  return hinweis?.art === "abgelaufen" || hinweis?.art === "ausgeschoepft";
}

/** Die Karte zu einer Kennung, oder `null`. */
export function karteVon(zustand: Gespraech, karteId: string): Karte | null {
  for (const beitrag of zustand.beitraege) {
    if (beitrag.art === "assistent" && beitrag.id === karteId) {
      return beitrag.karte;
    }
  }
  return null;
}

/** Der Satz zu einem Hinweis (§6). Fehlt eine Zahl im Rumpf, steht derselbe Satz ohne sie. */
export function hinweistext(hinweis: Hinweis, texte: Texte, sprache: Sprache): string {
  const t = texte.assistent.hinweis;
  switch (hinweis.art) {
    case "zuVieleNachrichten":
      return t.zuVieleNachrichten;
    case "ausgeschoepft":
      return hinweis.hoechstens === undefined
        ? t.ausgeschoepftOhneZahl
        : einsetzen(t.ausgeschoepft, { anzahl: formatiereZahl(hinweis.hoechstens, sprache) });
    case "nachrichtUngueltig":
      return hinweis.hoechstensZeichen === undefined
        ? t.nachrichtUngueltigOhneZahl
        : einsetzen(t.nachrichtUngueltig, {
            anzahl: formatiereZahl(hinweis.hoechstensZeichen, sprache),
          });
    case "nichtErreichbar":
      return t.nichtErreichbar;
    case "abgelaufen":
      return t.abgelaufen;
    case "sonstiges":
      return fehleranzeige(hinweis.fehler, texte).text;
  }
}

/**
 * Was die verborgene `aria-live`-Region sagt (`docs/assistent-frontend.md` §7):
 * die Warteanzeige, danach die Antwort — bei einer Karte nur Typ und Zahl der
 * Einträge, nicht die ganze Karte. Hinweise sagen sich als `Alert` selbst an.
 */
export function ansagetext(zustand: Gespraech, texte: Texte): string {
  const t = texte.assistent;
  const ansage = zustand.ansage;
  if (ansage === null) {
    return "";
  }
  if (ansage.art === "wartet") {
    return t.wartet;
  }
  const beitrag = zustand.beitraege.find((kandidat) => kandidat.id === ansage.beitragId);
  if (beitrag?.art !== "assistent") {
    return "";
  }
  const teile = [`${t.sprecher.assistent} ${beitrag.text}`];
  if (beitrag.karte !== null) {
    const { name, eintraege } = beitrag.karte.vorschlag;
    teile.push(
      eintraege.length === 1
        ? einsetzen(t.ansage.vorschlagEins, { name })
        : einsetzen(t.ansage.vorschlagMehr, { name, anzahl: eintraege.length }),
    );
  }
  return teile.join(" ");
}
