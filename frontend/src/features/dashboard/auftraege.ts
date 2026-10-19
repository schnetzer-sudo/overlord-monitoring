import { ProblemFehler } from "@/lib/http";
import {
  AUFTRAGSEINORDNUNGEN,
  ZEILENERGEBNISSE,
  type Auftragseinordnung,
  type Zeilenergebnis,
} from "@/lib/status-farbe";

import type { Auftrag, Auftragseintrag } from "./api";

/**
 * Die Regeln des Blocks „Ihre Aufträge“ (Linie `assistent`, A4,
 * `docs/assistent-frontend.md` §12) — **frei von React**, wie `filter.ts` und
 * `verlauf.ts` daneben, und deshalb als reine Funktionen geprüft
 * (`tests/auftraege.test.ts`).
 *
 * **Hier wird kein Stand abgeleitet.** Die Einordnung kommt aus dem Backend
 * (`TaskStatusClassifier`, `docs/tasks.md` §5.5); gezählt wird nur, was die
 * Zeile ausschreibt — „<x> von <n> nicht erfolgreich“.
 */

/** Sichtbare Aufträge, bevor „weitere anzeigen“ steht (E‑1101). */
export const SICHTBARE_AUFTRAEGE = 5;

/** Sichtbare Einträge eines aufgeklappten Auftrags (E‑1104). */
export const SICHTBARE_EINTRAEGE = 5;

/** Schlüssel in der zugeklappten Zeile (E‑1102). */
export const SCHLUESSEL_ZUGEKLAPPT = 2;

/**
 * Die Einordnung, **und ein Wert, den dieses Frontend nicht kennt, ist
 * `UNBEKANNT`**. Der Vertrag nennt sieben (`docs/tasks.md` §6.5); kommt je ein
 * achter, steht er wie ein unbekannter Status da — mit dem Rohwert, nicht mit
 * einem geratenen Wort (Regel Q4).
 */
export function einordnungVon(auftrag: Pick<Auftrag, "einordnung">): Auftragseinordnung {
  return (AUFTRAGSEINORDNUNGEN as readonly string[]).includes(auftrag.einordnung)
    ? auftrag.einordnung
    : "UNBEKANNT";
}

/** Das Ergebnis einer Zeile; ein unbekannter Wert zählt wie `UNBEKANNT` als nicht Ok. */
export function ergebnisVon(eintrag: Pick<Auftragseintrag, "ergebnis">): Zeilenergebnis | null {
  if (eintrag.ergebnis === null) {
    return null;
  }
  return (ZEILENERGEBNISSE as readonly string[]).includes(eintrag.ergebnis)
    ? eintrag.ergebnis
    : "UNBEKANNT";
}

/** Was von einer Liste zu sehen ist: die ersten `anzahl`, oder alle. */
export function sichtbare<T>(liste: readonly T[], alle: boolean, anzahl: number): readonly T[] {
  return alle ? liste : liste.slice(0, anzahl);
}

/** Wie viele hinter „weitere anzeigen“ stehen. */
export function weitere(liste: readonly unknown[], anzahl: number): number {
  return Math.max(0, liste.length - anzahl);
}

/**
 * Stehen Aufträge **mehrerer Typen** in der Liste? Dann steht der Name des Typs
 * zusätzlich oben in der Zeile (E‑1103). Gezählt über die ganze Liste, nicht
 * nur über die sichtbaren fünf — sonst wechselte die Zeile ihr Aussehen mit
 * „weitere anzeigen“.
 */
export function mehrereTypen(liste: readonly Pick<Auftrag, "typ">[]): boolean {
  return new Set(liste.map((auftrag) => auftrag.typ)).size > 1;
}

/**
 * Die Schlüssel der zugeklappten Zeile: die ersten zwei, und wie viele danach
 * kommen (E‑1102). **Ohne Schlüssel** steht „Eintrag <n>“ (F23) — gebildet
 * über `rueckfall`, damit hier kein Text steht.
 */
export function schluesselZugeklappt(
  eintraege: readonly Pick<Auftragseintrag, "schluessel">[],
  rueckfall: (nummer: number) => string,
): { schluessel: string[]; weitere: number } {
  return {
    schluessel: eintraege
      .slice(0, SCHLUESSEL_ZUGEKLAPPT)
      .map((eintrag, index) => eintrag.schluessel ?? rueckfall(index + 1)),
    weitere: weitere(eintraege, SCHLUESSEL_ZUGEKLAPPT),
  };
}

/**
 * Wie viele Einträge **nicht erfolgreich** sind: jeder, dessen Ergebnis nicht
 * `OK` ist. `UNBEKANNT` zählt mit, wie im Backend (`docs/tasks.md` §5.4).
 * Gebraucht für „<x> von <n> nicht erfolgreich“ bei „Teilweise erledigt“.
 */
export function nichtErfolgreich(eintraege: readonly Pick<Auftragseintrag, "ergebnis">[]): number {
  return eintraege.filter((eintrag) => ergebnisVon(eintrag) !== "OK").length;
}

/**
 * Darf „Auftrag löschen“ stehen? **Nur bei `OFFEN`** (E‑1110), für jede Rolle
 * gleich. Der Knopf ist Bequemlichkeit, keine Berechtigung: Das Backend prüft
 * im Statement selbst.
 */
export function loeschbar(auftrag: Pick<Auftrag, "einordnung">): boolean {
  return einordnungVon(auftrag) === "OFFEN";
}

/** Ein Wert eines Eintrags mit seinem `label`. */
export type BeschrifteterWert = { name: string; label: string; wert: string };

/**
 * Die Werte eines Eintrags mit ihrem `label` aus der Kopie (E‑1104, E‑1120),
 * **in der Reihenfolge der Definition** — und nur die mit Wert (F21).
 *
 * Ein Feldname, den `felder` nicht kennt, steht mit seinem Namen da und wird
 * nicht weggelassen: Was der Auftrag trägt, wird gezeigt (Regel Q4). Nach dem
 * Vertrag kommt das nicht vor.
 */
export function beschrifteteWerte(
  felder: readonly { name: string; label: string }[],
  werte: Readonly<Record<string, string>>,
): BeschrifteterWert[] {
  const bekannt = felder
    .filter((feld) => Object.hasOwn(werte, feld.name))
    .map((feld) => ({ name: feld.name, label: feld.label, wert: werte[feld.name] }));
  const namen = new Set(felder.map((feld) => feld.name));
  const fremd = Object.keys(werte)
    .filter((name) => !namen.has(name))
    .map((name) => ({ name, label: name, wert: werte[name] }));
  return [...bekannt, ...fremd];
}

/**
 * Wie ein Löschen ausgegangen ist (E‑1112, E‑1116, F22).
 *
 * - `geloescht` — `204`
 * - `uebernommen` — `409 auftrag-nicht-offen`: Die Plattform hat ihn abgeholt
 * - `verschwunden` — `404`: Es gibt ihn hier nicht mehr
 * - `gestoert` — alles andere: Netz, `5xx`, ein unerwarteter Problemtyp
 *
 * **`409` wird am Problemtyp erkannt und nicht am Status** — dieselbe Regel wie
 * überall (`docs/frontend-grundlagen.md` §6). `404` heißt hier immer „nicht
 * vorhanden“, gleich warum; ein Wort über Berechtigung gibt es nicht.
 */
export type Loeschergebnis = "geloescht" | "uebernommen" | "verschwunden" | "gestoert";

export const AUFTRAG_NICHT_OFFEN = "auftrag-nicht-offen";

export function loeschergebnis(fehler: unknown): Exclude<Loeschergebnis, "geloescht"> {
  if (fehler instanceof ProblemFehler && fehler.typ === AUFTRAG_NICHT_OFFEN) {
    return "uebernommen";
  }
  if (fehler instanceof ProblemFehler && fehler.status === 404) {
    return "verschwunden";
  }
  return "gestoert";
}

/**
 * Lädt die Liste nach diesem Ausgang neu? Nach `204`, `409` und `404` ja
 * (E‑1109, E‑1116) — der Stand hat sich geändert. Nach einer Störung nicht:
 * Es ist nichts passiert, und der Kunde versucht es erneut.
 */
export function laedtNeuNach(ergebnis: Loeschergebnis): boolean {
  return ergebnis !== "gestoert";
}

// ─── Der Zustand des Blocks ─────────────────────────────────────────────────

/** Ein Satz oben im Block (F22). */
export type Obenmeldung = "geloescht" | "verschwunden";

/** Ein Satz in einer Zeile (F22). */
export type Zeilenmeldung = "uebernommen" | "gestoert";

/**
 * Was der Block sich merkt — **im Arbeitsspeicher, nicht in der URL** (F20):
 * aufgeklappte Zeilen, ob alle Aufträge oder alle Einträge eines Auftrags zu
 * sehen sind, bei welchem Auftrag die Rückfrage zum Löschen offen ist, und die
 * Sätze nach einem Löschen.
 *
 * `obenNummer` zählt jeden neuen Satz oben. Der Block setzt den Fokus darauf,
 * wenn sie sich ändert — auch dann, wenn zweimal hintereinander derselbe Satz
 * kommt.
 */
export type Blockzustand = {
  offen: readonly string[];
  alleAuftraege: boolean;
  alleEintraege: readonly string[];
  /** Der Auftrag, bei dem die Rückfrage „Auftrag löschen?“ steht (E‑1112). */
  frage: string | null;
  oben: Obenmeldung | null;
  obenNummer: number;
  zeilen: Readonly<Record<string, Zeilenmeldung>>;
};

export const BLOCK_ANFANG: Blockzustand = {
  offen: [],
  alleAuftraege: false,
  alleEintraege: [],
  frage: null,
  oben: null,
  obenNummer: 0,
  zeilen: {},
};

export type Blockschritt =
  | { art: "umschalten"; taskId: string }
  | { art: "alleAuftraege"; an: boolean }
  | { art: "alleEintraege"; taskId: string; an: boolean }
  | { art: "frage"; taskId: string | null }
  | { art: "loeschenBegonnen"; taskId: string }
  | { art: "loeschenBeendet"; taskId: string; ergebnis: Loeschergebnis }
  | { art: "neuLaden" };

function ohne(liste: readonly string[], wert: string): readonly string[] {
  return liste.filter((eintrag) => eintrag !== wert);
}

function ohneZeile(
  zeilen: Readonly<Record<string, Zeilenmeldung>>,
  taskId: string,
): Readonly<Record<string, Zeilenmeldung>> {
  return Object.fromEntries(Object.entries(zeilen).filter(([schluessel]) => schluessel !== taskId));
}

/**
 * Die Übergänge. **„Neu laden“ nimmt alle Sätze weg** (F22) und lässt Auf- und
 * Zugeklapptes stehen (F20). Ein neuer Versuch zu löschen nimmt die Sätze weg,
 * die zu einem früheren gehören. **Das Ende eines Löschens schließt die
 * Rückfrage**, gleich wie es ausging: Bei einer Störung steht danach wieder
 * „Auftrag löschen“ (F22).
 */
export function blockschritt(zustand: Blockzustand, schritt: Blockschritt): Blockzustand {
  switch (schritt.art) {
    case "umschalten":
      return {
        ...zustand,
        offen: zustand.offen.includes(schritt.taskId)
          ? ohne(zustand.offen, schritt.taskId)
          : [...zustand.offen, schritt.taskId],
      };
    case "alleAuftraege":
      return { ...zustand, alleAuftraege: schritt.an };
    case "alleEintraege":
      return {
        ...zustand,
        alleEintraege: schritt.an
          ? [...ohne(zustand.alleEintraege, schritt.taskId), schritt.taskId]
          : ohne(zustand.alleEintraege, schritt.taskId),
      };
    case "frage":
      return { ...zustand, frage: schritt.taskId };
    case "loeschenBegonnen":
      return { ...zustand, oben: null, zeilen: ohneZeile(zustand.zeilen, schritt.taskId) };
    case "loeschenBeendet":
      if (schritt.ergebnis === "geloescht" || schritt.ergebnis === "verschwunden") {
        return {
          ...zustand,
          offen: ohne(zustand.offen, schritt.taskId),
          frage: null,
          oben: schritt.ergebnis,
          obenNummer: zustand.obenNummer + 1,
          zeilen: ohneZeile(zustand.zeilen, schritt.taskId),
        };
      }
      return {
        ...zustand,
        frage: null,
        zeilen: { ...zustand.zeilen, [schritt.taskId]: schritt.ergebnis },
      };
    case "neuLaden":
      return { ...zustand, oben: null, zeilen: {} };
  }
}

/**
 * Steht der Block? (E‑1098, F18, F22)
 *
 * | Abfrage | Block |
 * |---|---|
 * | lädt zum ersten Mal | nein — kein Platzhalter |
 * | Fehler | ja, mit Satz und „Erneut versuchen“ |
 * | mindestens ein Auftrag | ja |
 * | leer | nur, solange oben ein Satz steht — sonst nein |
 */
export function blockSichtbar(
  status: "pending" | "error" | "success",
  anzahl: number,
  oben: Obenmeldung | null,
): boolean {
  if (status === "pending") {
    return false;
  }
  if (status === "error") {
    return true;
  }
  return anzahl > 0 || oben !== null;
}
