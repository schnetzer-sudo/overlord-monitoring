import type { Automatikzustand } from "@/components/neu-laden";

import { alsAbfrage, type Nachrichtenfilter } from "./filter";

/**
 * Wann die Liste von selbst neu fragt, und wohin „Neu laden" führt — **rein,
 * ohne React** (`docs/neu-laden.md`, `docs/nachrichtenliste.md` §8.3).
 *
 * Die Regeln stehen hier und nicht als Bedingung in einem Haken, damit sie als
 * Funktionen geprüft werden können (`tests/neu-laden.test.ts`).
 */

/** Das Intervall der automatischen Aktualisierung. Keine Wahl (E‑165). */
export const AKTUALISIERUNG_INTERVALL_MS = 60_000;

/**
 * **Die automatische Aktualisierung fragt nur, wenn alle drei gelten** —
 * unverändert seit Schritt 4 (`docs/nachrichtenliste.md` §8.3):
 *
 * - **der Schalter ist an** — die Vorgabe ist aus; wer das Werkzeug öffnet, ist
 *   angespannt, und eine Liste, die unter den Händen springt, hilft nicht;
 * - **Seite eins** — sonst zeigte die Ansicht plötzlich einen anderen
 *   Ausschnitt, oder der Cursor läge außerhalb des Fensters;
 * - **der Tab ist sichtbar** — sonst stellte ein über Nacht offenes Fenster
 *   480 Abfragen auf der Produktionsdatenbank.
 *
 * Zurück kommt der Wert für `refetchInterval`: die Millisekunden oder `false`.
 */
export function aktualisierungsintervall(lage: {
  an: boolean;
  aufSeiteEins: boolean;
  sichtbar: boolean;
}): number | false {
  return lage.an && lage.aufSeiteEins && lage.sichtbar ? AKTUALISIERUNG_INTERVALL_MS : false;
}

/**
 * **Was der Schalter zeigt** (E‑171). Pausiert ist er, wenn er an ist und die
 * Liste nicht auf Seite eins steht — und zwar **am Schalter selbst**, denn ein
 * Schalter, der an ist und nichts tut, ist schlimmer als einer, der aus ist.
 *
 * **Der verdeckte Tab pausiert ihn nicht.** Wer ihn sieht, hat den Tab ohnehin
 * im Vordergrund; die Bedingung wirkt allein auf das Intervall.
 */
export function automatikzustand(an: boolean, aufSeiteEins: boolean): Automatikzustand {
  if (!an) {
    return "aus";
  }
  return aufSeiteEins ? "an" : "pausiert";
}

/**
 * **Wohin „Neu laden" führt: Seite eins desselben Filters** (E‑168).
 *
 * Kein Cursor — auch dann nicht, wenn gerade Seite sieben zu sehen ist. Zwei
 * Gründe, und jeder reicht allein: Bei der Vorgabesortierung über
 * `MessageLastUpdate` wandert eine geänderte Nachricht nach oben, die Seiten
 * dahinter verschieben sich also; und der Cursor eines relativen Fensters kann
 * inzwischen aus dem Fenster gefallen sein (`cursor-ungueltig`).
 *
 * **Die geöffnete Nachricht gehört nicht dazu** (E‑169) — `alsAbfrage` kennt sie
 * ohnehin nicht (`docs/frontend-grundlagen.md` §8, dritte Regel). Neu geladen
 * werden die Hauptdaten der Ansicht, nie das Panel.
 */
export function abfrageNachNeuLaden(filter: Nachrichtenfilter): string {
  return alsAbfrage(filter, null);
}

/**
 * **Der Blätterstapel, nachdem Seite eins angekommen ist.**
 *
 * „Neu laden" holt auf einer späteren Seite zuerst Seite eins und wechselt erst
 * danach — so bleibt die vorhandene Seite stehen, bis die neue da ist. In der
 * Zwischenzeit kann der Nutzer weitergeblättert oder den Filter geändert haben;
 * dann hat er eine neue Stelle gewählt, und die Rückkehr auf Seite eins
 * überginge sie. Verglichen wird deshalb der **Stapel selbst**, nicht seine
 * Länge: Jedes Blättern und jede Filteränderung setzt einen neuen.
 */
export function stapelNachNeuLaden(beimKlick: string[], jetzt: string[]): string[] {
  return jetzt === beimKlick ? [] : jetzt;
}
