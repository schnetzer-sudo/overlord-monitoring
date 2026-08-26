import type { Sprache, Texte } from "@/i18n";
import { einsetzen } from "@/i18n";
import { formatiereZahl } from "@/lib/format";

import type { Vorschlagsuebernahme } from "./api";

/**
 * Was der Dialog „Vorschläge übernehmen" sagt — **als reine Funktion, damit die
 * Sätze prüfbar sind, ohne einen Baum zu rendern.**
 *
 * Die Ansicht daneben ist Verdrahtung; hier steht die einzige Entscheidung, die
 * falsch zu treffen wehtut: **welcher Satz wann dasteht**.
 *
 * ## Der dritte Teilsatz ist Pflicht und kein Beiwerk
 *
 * Ohne ihn liest ein Administrator bei `NEXANS` „509" über einer Liste von 733
 * Zeilen und sucht die fehlenden 224 in einem Fehler statt in E22. Die 224
 * tragen eine **Richtung** aus dem Projektnamen, aber nie einen
 * **Partner**vorschlag — und „gepflegt mit leerem Partner" hieße in diesem
 * Katalog „hingesehen, es gibt keinen" (E4). Ein Knopf, der 224 Behauptungen
 * erfindet, wäre schlimmer als einer, der 224 Zeilen liegenlässt.
 *
 * ## Und ein vierter Satz, sobald der Filter gesetzt ist
 *
 * `nurMitNachrichten` wird im Browser gerechnet (E20); die Übernahme läuft im
 * Backend über **alle** Prozesse des Mandanten (E23). Wer den Haken gesetzt hat,
 * sieht eine Teilmenge und übernimmt trotzdem alles. Das ist genau die Stelle,
 * an der E23 sonst überrascht.
 */

/**
 * Wie eine Regelzahl im Satz erscheint: `keinen`, `einen` oder die Zahl selbst.
 *
 * **Die Null bekommt ein Wort und keine Ziffer.** „509 aus Regel A, keinen aus
 * Regel B" liest sich als Aussage; „509 aus Regel A, 0 aus Regel B" liest sich
 * als Tabelle, die in einen Satz gerutscht ist.
 */
export function regelzahl(anzahl: number, texte: Texte, sprache: Sprache): string {
  if (anzahl === 0) {
    return texte.katalog.uebernahme.keinen;
  }
  if (anzahl === 1) {
    return texte.katalog.uebernahme.einen;
  }
  return formatiereZahl(anzahl, sprache);
}

/**
 * Die Sätze des Dialogs, in der Reihenfolge, in der sie dastehen.
 *
 * | Lage | Was dasteht |
 * |---|---|
 * | `betroffen === 0` | ein Satz: es gibt nichts zu übernehmen |
 * | `betroffen > 0` | drei Sätze: die Zahl mit ihrer Aufschlüsselung, was mit den Zeilen geschieht, und was **nicht** mitkommt |
 * | dazu, wenn `nurMitNachrichten` | ein vierter: der Filter wirkt hier nicht |
 *
 * Der Filtersatz erscheint **auch bei `betroffen === 0`**: Wer den Haken gesetzt
 * hat und „keine Vorschläge" liest, soll nicht vermuten, der Filter habe sie
 * weggenommen.
 */
export function uebernahmesaetze(
  vorschau: Vorschlagsuebernahme,
  nurMitNachrichten: boolean,
  texte: Texte,
  sprache: Sprache,
): string[] {
  const worte = texte.katalog.uebernahme;
  const saetze: string[] = [];

  if (vorschau.betroffen === 0) {
    saetze.push(worte.keine);
  } else {
    const zahl =
      vorschau.betroffen === 1
        ? worte.uebernimmtEins
        : einsetzen(worte.uebernimmtViele, {
            betroffen: formatiereZahl(vorschau.betroffen, sprache),
          });
    const aufteilung = einsetzen(worte.aufteilung, {
      regelA: regelzahl(vorschau.regelA, texte, sprache),
      regelB: regelzahl(vorschau.regelB, texte, sprache),
    });
    saetze.push(`${zahl} ${aufteilung}`, worte.folge, worte.ohneVorschlagBleibtOffen);
  }

  if (nurMitNachrichten) {
    saetze.push(worte.filterWirktNicht);
  }
  return saetze;
}

/**
 * Darf „Übernehmen" gedrückt werden?
 *
 * **Gesperrt, bis eine Vorschau vorliegt** — dieselbe Zusage wie bei der
 * Massenzuordnung: Der Nutzer bestätigt eine Zahl, und ohne Vorschau gibt es
 * keine. Gesperrt auch bei `betroffen === 0`: Ein Knopf, der nichts tut, sollte
 * nicht bedienbar aussehen.
 *
 * **Die Frage „gibt es überhaupt welche" wird hier und nirgends sonst
 * beantwortet** — nämlich an einer Antwort des Backends. Der Knopf über der
 * Liste trägt diese Bedingung ausdrücklich **nicht** (E23): Stünde die Regel aus
 * E22 zusätzlich im Browser, stünde sie zweimal.
 */
export function darfUebernehmen(vorschau: Vorschlagsuebernahme | null, laeuft: boolean): boolean {
  return vorschau !== null && vorschau.betroffen > 0 && !laeuft;
}
