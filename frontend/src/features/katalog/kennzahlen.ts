import type { Katalogzeile } from "./api";

/**
 * Was sich aus der **vollen** Liste ablesen lässt — der Fortschritt, der
 * Hinweis über der Liste und die Projekte der Massenzuordnung.
 *
 * **Frei von React**, damit die drei Aussagen prüfbar sind, ohne einen Baum zu
 * rendern.
 *
 * > **Alle drei brauchen die volle Liste und nicht die gefilterte.** Das ist die
 * > eine Stelle, an der der Filter `nurOffene` teuer wird: Er ist ein
 * > Anfrageparameter, die Antwort enthält also nur offene Zeilen — und über
 * > ihnen wäre jede der drei Aussagen falsch. Der Fortschritt könnte den
 * > Zähler nicht kennen, der Hinweis übersähe eine gepflegte Zeile mit
 * > Regelherkunft, und die Projektauswahl verlöre ausgerechnet die Projekte,
 * > die vollständig gepflegt sind — also genau die, für die E12 die
 * > Massenzuordnung als Korrekturwerkzeug vorsieht.
 * >
 * > Die Ansicht hält deshalb **zusätzlich** die ungefilterte Liste. Solange der
 * > Haken nicht gesetzt ist, ist das dieselbe Abfrage und kostet nichts;
 * > gesetzt kostet es eine zweite über denselben Endpunkt (L12: 8,0 ms bei
 * > `NEXANS`). Eine eigene Zähl-Abfrage gibt es weiterhin nicht (E8).
 */

export type Fortschritt = {
  gepflegt: number;
  gesamt: number;
  /** Zwischen 0 und 1. Bei leerer Liste `0` — nicht `NaN`. */
  anteil: number;
};

/**
 * **Eine Zahl, über alle Prozesse** (E18).
 *
 * Der Nenner sind **alle** — tote Prozesse und der Auffangprozess zählen mit.
 * E14 teilt ihn ausdrücklich nicht: Eine zweite Quote „gepflegt unter denen mit
 * Nachrichten" stünde als bequemere Zahl neben der richtigen und wäre binnen
 * einer Woche die berichtete. Bei `VOTG` sähe die Kuratierung dann zu 89,74 %
 * fertig aus, während 350 Zeilen unbearbeitet stehen.
 *
 * Ungewichtet: nicht nach Aufkommen. Eine Gewichtung behauptete eine
 * Genauigkeit, die die Datenlage nicht hergibt.
 */
export function fortschritt(zeilen: readonly Katalogzeile[]): Fortschritt {
  const gesamt = zeilen.length;
  const gepflegt = zeilen.filter((zeile) => zeile.pflegestatus === "GEPFLEGT").length;
  return { gepflegt, gesamt, anteil: gesamt === 0 ? 0 : gepflegt / gesamt };
}

/**
 * Konnte für **keinen** Prozess dieses Mandanten ein Partner vorgeschlagen
 * werden? (E17)
 *
 * **Die Bedingung wird aus den Daten gerechnet, niemals aus einer
 * Mandantenliste im Code.** Zur Kontrolle — und ausdrücklich nur zur Kontrolle —
 * sind es im Bestand **fünf** Mandanten: `SUTTONS`, `ZAST`, `WOC`, `SYSTEM` und
 * `NXHBE`. Die früheren Listen mit vier zählen etwas anderes, nämlich Mandanten
 * ohne *jede* Ableitung; `NXHBE` hat eine Richtung, aber für keinen seiner 17
 * Prozesse einen Partner. **Diese fünf sind Erwartung zum Nachprüfen und kein
 * Datenbestand zum Einbauen.**
 *
 * **Sie hängt am Partner allein und nicht an der Richtung.** Die Richtung ist
 * der leichtere Teil der Kuratierung — bei `NEXANS` und `NXHBE` kommt sie
 * vollständig aus dem Projektnamen —, und ein Hinweis, der wegen einer
 * gefüllten Richtung verschwindet, verschwände genau dort, wo die eigentliche
 * Arbeit noch aussteht.
 *
 * **`vorschlagHerkunft` beschreibt den Partner und altert nicht.** Sie bleibt
 * beim Kuratieren stehen — sie sagt, welche Regelfassung diese Zeile einmal
 * vorgeschlagen hat, und wird durch eine Kuratierung nicht falsch, sondern
 * historisch. Der Hinweis bleibt deshalb stehen, auch wenn ein Mensch inzwischen
 * jeden Partner von Hand eingetragen hat: Vorgeschlagen hat sie trotzdem keiner.
 *
 * Auf einer **leeren** Liste ist die Aussage keine — `every` wäre dort wahr, und
 * ein Hinweis über null Zeilen sagt nichts über einen Mandanten.
 */
export function ohneJedenPartnervorschlag(zeilen: readonly Katalogzeile[]): boolean {
  return zeilen.length > 0 && zeilen.every((zeile) => zeile.vorschlagHerkunft === "KEINE");
}

export type Projekt = {
  projectId: string;
  projectName: string | null;
  /** Wie viele Prozesse dieses Projekts in der Liste stehen. */
  anzahl: number;
};

/**
 * Die Projekte des Mandanten, abgeleitet aus der Liste — **ohne eigene
 * Abfrage.**
 *
 * Die Reihenfolge ist die der Liste, und die kommt nach `ProjectID` sortiert vom
 * Backend (E6). Hier wird nicht umsortiert; die Auswahl steht damit in derselben
 * Ordnung wie die Tabelle darunter.
 *
 * Ein Projekt ohne Namen behält seine Kennung — sie ist der Wert, den die
 * Massenzuordnung entgegennimmt, und der Name ist nur die Lesehilfe daneben.
 */
export function projekteAus(zeilen: readonly Katalogzeile[]): Projekt[] {
  const projekte = new Map<string, Projekt>();
  for (const zeile of zeilen) {
    const bisher = projekte.get(zeile.projectId);
    if (bisher === undefined) {
      projekte.set(zeile.projectId, {
        projectId: zeile.projectId,
        projectName: zeile.projectName,
        anzahl: 1,
      });
    } else {
      bisher.anzahl += 1;
    }
  }
  return [...projekte.values()];
}
